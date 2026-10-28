/*
 * Copyright 2026 SandFly contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.accessibility.talkback.translation

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.actor.gemini.AiTextRequests
import com.google.android.accessibility.talkback.translation.TranslationFailure.Kind
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechController
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.IOException
import java.util.Locale
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import org.json.JSONException

/**
 * Translates text with the service chosen in Translation settings, from the chosen language or the
 * one detected, into the target language, or the second target language when the text is already
 * in the target language. Main thread only; results come on the main thread.
 */
class Translator(context: Context) {
  private val appContext = context.applicationContext
  private val prefs: SharedPreferences = SharedPreferencesUtils.getSharedPreferences(appContext)
  private val handler = Handler(Looper.getMainLooper())
  // One thread, which ends when it has been idle for a while.
  private val worker =
    ThreadPoolExecutor(1, 1, IDLE_SECONDS, TimeUnit.SECONDS, LinkedBlockingQueue()) { runnable ->
        Thread(runnable, "translation").apply { isDaemon = true }
      }
      .apply { allowCoreThreadTimeOut(true) }
  /** Counts requests, so that the result of one that was replaced is dropped. */
  private var request = 0
  private var usedOnDevice = false

  sealed interface Result {
    data class Done(val translation: Translation) : Result

    data class Failed(val failure: TranslationFailure, val service: TranslationService) : Result

    /** On-device models that are downloading before the text is translated. */
    data class Downloading(val languages: List<String>) : Result
  }

  /** The service that is used now. */
  fun service(): TranslationService = TranslationSettings.service(prefs, OnDeviceTranslations.get() != null)

  /** Whether [service] can translate now, as far as can be told without asking it. */
  fun isSetUp(service: TranslationService): Boolean =
    when (service) {
      TranslationService.ON_DEVICE -> OnDeviceTranslations.get() != null
      TranslationService.AI -> AiTextRequests.isSetUp(appContext)
      else -> TranslationSettings.missing(prefs, service) == null
    }

  /**
   * Translates [text]. [onResult] gets [Result.Downloading] first when on-device models are
   * downloaded, and then the translation or the failure.
   */
  fun translate(text: String, onResult: (Result) -> Unit) {
    val id = ++request
    val source = TranslationSettings.source(prefs)
    run(
      text,
      service(),
      source,
      TranslationSettings.target(prefs),
      if (source == null) TranslationSettings.secondTarget(prefs) else null,
      Downloads.WAIT,
    ) { result ->
      if (id == request || result is Result.Downloading) onResult(result)
    }
  }

  /**
   * Translates [text] from [from] into [to] with [service], for Automatic translation, alongside
   * [translate]. [downloads] says what happens when a language model isn't downloaded;
   * [onDownloaded] is told when models that [Downloads.START] started have all downloaded.
   */
  fun translateNow(
    text: String,
    service: TranslationService,
    from: String,
    to: String,
    downloads: Downloads,
    onDownloaded: () -> Unit,
    onResult: (Result) -> Unit,
  ) = run(text, service, from, to, second = null, downloads, onDownloaded, onResult)

  /** What happens when on-device language models aren't downloaded. */
  enum class Downloads {
    /** They are downloaded, and then the text is translated. */
    WAIT,
    /** They start downloading, and the translation fails. */
    START,
    /** The translation fails. */
    NONE,
  }

  private fun run(
    text: String,
    service: TranslationService,
    source: String?,
    target: String,
    second: String?,
    downloads: Downloads,
    onDownloaded: () -> Unit = {},
    report: (Result) -> Unit,
  ) {
    val fail: (Kind, String) -> Unit = { kind, detail -> report(Result.Failed(TranslationFailure(kind, detail), service)) }
    if (!isSetUp(service)) return fail(Kind.NOT_SET_UP, "")
    val input = text.take(TranslationApis.MAX_TEXT)
    when (service) {
      TranslationService.ON_DEVICE -> onDevice(input, source, target, second, downloads, onDownloaded, report, fail)
      TranslationService.AI -> {
        val ai = ai(appContext)
        ai.cancel()
        ai.ask(AiTranslationPrompt.prompt(input, source, target, second), json = true) { answer, failure ->
          when {
            answer != null -> report(Result.Done(AiTranslationPrompt.parse(answer, target)))
            failure != null -> fail(Kind.OTHER, failure.userMessage(appContext))
          }
        }
      }
      else -> web(service, input, source, target, second, report)
    }
  }

  fun cancel() {
    request++
    ai?.cancel()
  }

  fun shutdown() {
    cancel()
    worker.shutdownNow()
    if (usedOnDevice) OnDeviceTranslations.get()?.close()
  }

  private fun web(
    service: TranslationService,
    text: String,
    source: String?,
    target: String,
    second: String?,
    report: (Result) -> Unit,
  ) {
    val key = TranslationSettings.apiKey(prefs, service)
    val region = TranslationSettings.microsoftRegion(prefs)
    val address = TranslationSettings.libreTranslateAddress(prefs)
    val locale = Locale.getDefault()
    worker.execute {
      val result =
        try {
          var translation = ask(service, key, region, address, locale, text, source, target)
          if (second != null && TranslationLanguages.same(translation.from, target)) {
            translation = ask(service, key, region, address, locale, text, translation.from, second)
          }
          Result.Done(translation)
        } catch (e: FailureException) {
          Result.Failed(e.failure, service)
        } catch (e: IOException) {
          LogUtils.w(TAG, "%s not reached: %s", service.id, e)
          Result.Failed(TranslationFailure(Kind.CONNECTION), service)
        } catch (e: Throwable) {
          // Such as a header the connection refuses, or no memory for the answer.
          LogUtils.w(TAG, "%s failed: %s", service.id, e)
          Result.Failed(TranslationFailure(Kind.OTHER), service)
        }
      handler.post { report(result) }
    }
  }

  @Throws(IOException::class, FailureException::class)
  private fun ask(
    service: TranslationService,
    key: String,
    region: String,
    address: String,
    locale: Locale,
    text: String,
    from: String?,
    to: String,
  ): Translation {
    val request =
      when (service) {
        TranslationService.GOOGLE_CLOUD -> TranslationApis.googleCloud(key, text, from, to)
        TranslationService.DEEPL -> TranslationApis.deepL(key, text, from, to, locale)
        TranslationService.MICROSOFT -> TranslationApis.microsoft(key, region, text, from, to)
        else -> TranslationApis.libreTranslate(address, key, text, from, to)
      }
    val (status, body) = TranslationApis.send(request)
    if (status != 200) {
      val failure = TranslationApis.failure(status, body)
      LogUtils.w(TAG, "%s: HTTP %d %s", service.id, status, failure.detail)
      throw FailureException(failure)
    }
    return try {
      when (service) {
        TranslationService.GOOGLE_CLOUD -> TranslationApis.parseGoogleCloud(body, to)
        TranslationService.DEEPL -> TranslationApis.parseDeepL(body, to)
        TranslationService.MICROSOFT -> TranslationApis.parseMicrosoft(body, to)
        else -> TranslationApis.parseLibreTranslate(body, to)
      }
    } catch (e: JSONException) {
      LogUtils.w(TAG, "%s answer not read: %s", service.id, e)
      throw FailureException(TranslationFailure(Kind.OTHER, "The answer could not be read"))
    }
  }

  private fun onDevice(
    text: String,
    source: String?,
    target: String,
    second: String?,
    downloads: Downloads,
    onDownloaded: () -> Unit,
    report: (Result) -> Unit,
    fail: (Kind, String) -> Unit,
  ) {
    val models = OnDeviceTranslations.get() ?: return fail(Kind.NOT_SET_UP, "")
    usedOnDevice = true
    val withSource: (String) -> Unit = { from ->
      val to = if (second != null && from == target) second else target
      val unsupported = listOf(from, to).firstOrNull { it !in models.languages }
      when {
        from == to -> report(Result.Done(Translation(text, from, to)))
        unsupported != null -> fail(Kind.LANGUAGE, unsupported)
        else -> withModels(models, from, to, downloads, onDownloaded, report, fail) {
          models.translate(text, from, to) { translated ->
            if (translated == null) fail(Kind.OTHER, "") else report(Result.Done(Translation(translated, from, to)))
          }
        }
      }
    }
    if (source != null) {
      withSource(source)
    } else {
      models.detect(text) { detected, unavailable ->
        when {
          detected != null -> withSource(detected)
          unavailable -> fail(Kind.DETECTION_DOWNLOADING, "")
          else -> fail(Kind.NOT_DETECTED, "")
        }
      }
    }
  }

  /** Runs [translate] once the models of [from] and [to], and English between them, are downloaded. */
  private fun withModels(
    models: OnDeviceTranslation,
    from: String,
    to: String,
    downloads: Downloads,
    onDownloaded: () -> Unit,
    report: (Result) -> Unit,
    fail: (Kind, String) -> Unit,
    translate: () -> Unit,
  ) {
    val needed = listOf(from, to, ENGLISH).distinct()
    if (needed.any { models.isDownloading(it) }) return fail(Kind.DOWNLOADING, "")
    models.downloaded { downloaded ->
      val missing = needed.filter { it !in downloaded }
      if (missing.isEmpty()) return@downloaded translate()
      if (downloads == Downloads.NONE) return@downloaded fail(Kind.DOWNLOADING, "")
      report(Result.Downloading(missing))
      val wifiOnly = TranslationSettings.wifiOnly(prefs)
      var left = missing.size
      var failed = false
      for (language in missing) {
        models.download(language, wifiOnly) { ok ->
          failed = failed || !ok
          if (--left > 0) return@download
          when {
            downloads == Downloads.START -> if (!failed) onDownloaded()
            failed -> fail(Kind.CONNECTION, "")
            else -> translate()
          }
        }
      }
      if (downloads == Downloads.START) fail(Kind.DOWNLOADING, "")
    }
  }

  private class FailureException(val failure: TranslationFailure) : Exception(failure.detail)

  private companion object {
    const val TAG = "Translator"
    const val ENGLISH = "en"
    const val IDLE_SECONDS = 30L

    /**
     * Asks the AI service. One for the process, made when first used, since it starts threads that
     * it never stops; it speaks its messages through the service running at the time.
     */
    private var ai: AiTextRequests? = null

    fun ai(context: Context): AiTextRequests =
      ai ?: AiTextRequests(context) { speak(it) }.also { ai = it }

    /** Speaks why the on-device model couldn't answer, such as low memory. */
    private fun speak(message: String) {
      val options =
        SpeechController.SpeakOptions.create()
          .setQueueMode(SpeechController.QUEUE_MODE_INTERRUPT)
          .setFlags(FeedbackItem.FLAG_FORCE_FEEDBACK_ALL or FeedbackItem.FLAG_NO_HISTORY)
      TalkBackService.getInstance()?.speechController?.speak(message, EVENT_ID_UNTRACKED, options)
    }
  }
}
