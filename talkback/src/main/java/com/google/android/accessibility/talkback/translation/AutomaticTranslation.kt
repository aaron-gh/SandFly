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
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.LocaleSpan
import android.util.LruCache
import androidx.annotation.VisibleForTesting
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechController
import com.google.android.accessibility.utils.output.SpeechControllerImpl
import com.google.android.libraries.accessibility.utils.log.LogUtils
import com.google.mlkit.common.MlKitException
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.languageid.LanguageIdentifier
import java.util.Locale
import java.util.function.Consumer

/**
 * Automatic translation: speech in another language than the target language is translated before
 * it is spoken. Each utterance's language is identified on the phone first, so speech already in
 * the target language, or in a language left untranslated, is spoken as it is and never sent to a
 * service. Utterances are spoken in their order: one that waits for a translation holds back those
 * after it, and newer speech that interrupts drops them. Main thread only.
 */
class AutomaticTranslation
@VisibleForTesting
internal constructor(context: Context, private val engine: Engine) : SpeechControllerImpl.SpeechHold {
  private val appContext = context.applicationContext
  private val prefs = SharedPreferencesUtils.getSharedPreferences(appContext)
  private val handler = Handler(Looper.getMainLooper())
  /** The language of recent text, or [NO_LANGUAGE]. */
  private val languages = LruCache<String, String>(MAX_LANGUAGES)
  /** Recent translations, by [translationKey]. */
  private val translations = LruCache<String, String>(MAX_TRANSLATIONS)
  private val pending = ArrayDeque<Held>()
  /** Languages whose download was announced, so that it is said once. */
  private val announcedDownloads = mutableSetOf<String>()
  private var announcedNotSetUp = false

  private inner class Held(
    val text: CharSequence,
    /** Speech that newer speech can't interrupt, such as notifications, which is never dropped. */
    val uninterruptible: Boolean,
    val release: Consumer<CharSequence?>,
  ) {
    var result: CharSequence? = null
    var dropped = false
    val done: Boolean
      get() = result != null || dropped

    fun ready(spoken: CharSequence) {
      if (done) return
      result = spoken
      releaseReady()
    }
  }

  override fun hold(
    text: CharSequence,
    queueMode: Int,
    flags: Int,
    release: Consumer<CharSequence?>,
  ): Boolean {
    if (queueMode and SpeechController.QUEUE_MODE_BIT_INTERRUPT != 0) dropPending()
    val plain = text.toString()
    val known = if (letters(plain) < MIN_LETTERS) NO_LANGUAGE else languages.get(plain)
    if (pending.isEmpty() && known != null && !translates(known)) return false
    val held =
      Held(text, queueMode and SpeechController.QUEUE_MODE_BIT_UNINTERRUPTIBLE_BY_NEW_SPEECH != 0, release)
    pending.addLast(held)
    process(text) { held.ready(it) }
    return true
  }

  /**
   * Gives [done] the translation of [text], or [text] when it isn't translated, isn't translated
   * in time or can't be translated, on the main thread.
   */
  private fun process(text: CharSequence, done: (CharSequence) -> Unit) {
    val plain = text.toString()
    var finished = false
    val finish = { result: CharSequence ->
      if (!finished) {
        finished = true
        done(result)
      }
    }
    if (letters(plain) < MIN_LETTERS) return finish(text)
    handler.postDelayed({ finish(text) }, MAX_WAIT_MS)
    val withLanguage = { language: String? ->
      if (language != null && translates(language)) translate(plain, language) { finish(it ?: text) }
      else finish(text)
    }
    val known = languages.get(plain)
    if (known != null) return withLanguage(known)
    engine.detect(plain) { language ->
      if (language != null) languages.put(plain, language)
      if (!finished) withLanguage(language)
    }
  }

  /** Whether text in [language] is translated: it isn't the target or a language left as it is. */
  private fun translates(language: String): Boolean {
    if (language == NO_LANGUAGE) return false
    val source = TranslationSettings.source(prefs)
    return !TranslationLanguages.same(language, TranslationSettings.target(prefs)) &&
      (source == null || TranslationLanguages.same(language, source)) &&
      TranslationSettings.untranslated(prefs).none { TranslationLanguages.same(it, language) }
  }

  /** Gives [done] the translation of [text] to speak, or null when it failed. */
  private fun translate(text: String, from: String, done: (CharSequence?) -> Unit) {
    val service = TranslationSettings.autoService(prefs, engine.onDeviceAvailable)
    val to = TranslationSettings.target(prefs)
    val key = translationKey(service, from, to, text)
    translations.get(key)?.let { return done(spoken(it, to)) }
    engine.translate(text, service, from, to) { result ->
      when (result) {
        is Translator.Result.Done -> {
          translations.put(key, result.translation.text)
          done(spoken(result.translation.text, to))
        }
        is Translator.Result.Downloading -> announceDownload(result.languages)
        is Translator.Result.Failed -> {
          if (result.failure.kind == TranslationFailure.Kind.NOT_SET_UP) announceNotSetUp(service)
          else if (result.failure.kind != TranslationFailure.Kind.DOWNLOADING) {
            LogUtils.w(TAG, "Not translated: %s %s", result.failure.kind, result.failure.detail)
          }
          done(null)
        }
      }
    }
  }

  private fun spoken(translation: String, to: String): CharSequence {
    if (!TranslationSettings.targetVoice(prefs)) return translation
    val text = SpannableString(translation)
    text.setSpan(LocaleSpan(Locale.forLanguageTag(to)), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    return text
  }

  /** Speaks the utterances at the front that are ready, in order. */
  private fun releaseReady() {
    while (pending.isNotEmpty()) {
      val first = pending.first()
      if (!first.done) return
      pending.removeFirst()
      if (!first.dropped) first.release.accept(first.result)
    }
  }

  /** Drops what newer speech interrupts: everything held that is not yet spoken, but notifications. */
  private fun dropPending() {
    val dropped = pending.filter { !it.uninterruptible }
    pending.removeAll { !it.uninterruptible }
    for (held in dropped) {
      held.dropped = true
      held.release.accept(null)
    }
    releaseReady()
  }

  private fun announceDownload(languages: List<String>) {
    if (!announcedDownloads.addAll(languages)) return
    val names = android.icu.text.ListFormatter.getInstance().format(languages.map { TranslationLanguages.name(it) })
    status(appContext.getString(R.string.translation_downloading, names))
  }

  private fun announceNotSetUp(service: TranslationService) {
    if (announcedNotSetUp) return
    announcedNotSetUp = true
    val name = appContext.getString(service.label)
    status(
      appContext.getString(
        if (service.needsKey) R.string.translation_error_no_key else R.string.translation_error_no_address,
        name,
      )
    )
  }

  private fun status(text: String) {
    TalkBackService.getInstance()
      ?.speechController
      ?.speak(
        text,
        EVENT_ID_UNTRACKED,
        SpeechController.SpeakOptions.create()
          .setQueueMode(SpeechController.QUEUE_MODE_QUEUE)
          .setFlags(FeedbackItem.FLAG_FORCE_FEEDBACK_ALL or FeedbackItem.FLAG_NO_HISTORY)
          .setSpeechParams(untranslated()),
      )
  }

  @VisibleForTesting
  internal fun close() {
    handler.removeCallbacksAndMessages(null)
    // What is held is spoken untranslated, so that nothing waiting for it is left waiting.
    val held = pending.toList()
    pending.clear()
    for (item in held) if (!item.done) item.release.accept(item.text)
    engine.close()
  }

  /** Identifies languages and translates. Callbacks come on the main thread. */
  internal interface Engine {
    val onDeviceAvailable: Boolean

    /** The language of [text], [NO_LANGUAGE] when it has none, or null when it can't be told now. */
    fun detect(text: String, done: (String?) -> Unit)

    fun translate(text: String, service: TranslationService, from: String, to: String, done: (Translator.Result) -> Unit)

    fun close()
  }

  /** ML Kit language identification on the phone, and [Translator]. */
  private class PhoneEngine(context: Context) : Engine {
    private val translator = Translator(context)
    private var identifier: LanguageIdentifier? = null
    override val onDeviceAvailable = OnDeviceTranslations.isAvailable

    override fun detect(text: String, done: (String?) -> Unit) {
      val client =
        identifier
          ?: LanguageIdentification.getClient(
              LanguageIdentificationOptions.Builder().setConfidenceThreshold(MIN_CONFIDENCE).build()
            )
            .also { identifier = it }
      client
        .identifyLanguage(text)
        .addOnSuccessListener { code ->
          // Romanized text, such as "ja-Latn", is not in its language's script.
          done((if (code.endsWith("-Latn")) null else TranslationLanguages.normalize(code)) ?: NO_LANGUAGE)
        }
        .addOnFailureListener { e ->
          // Google Play services downloads language identification when it is first asked for.
          if ((e as? MlKitException)?.errorCode != MlKitException.UNAVAILABLE) {
            LogUtils.w(TAG, "Language not identified: %s", e)
          }
          done(null)
        }
    }

    override fun translate(
      text: String,
      service: TranslationService,
      from: String,
      to: String,
      done: (Translator.Result) -> Unit,
    ) = translator.translateNow(text, service, from, to, done)

    override fun close() {
      translator.shutdown()
      identifier?.close()
      identifier = null
    }
  }

  companion object {
    private const val TAG = "AutomaticTranslation"
    internal const val NO_LANGUAGE = ""
    /** Shorter text, such as a letter typed, is spoken as it is. */
    private const val MIN_LETTERS = 3
    /** Identification has to be this sure, since translating the target language garbles it. */
    private const val MIN_CONFIDENCE = 0.7f
    /** Speech is spoken untranslated when its translation takes longer. */
    private const val MAX_WAIT_MS = 3_000L
    private const val MAX_LANGUAGES = 500
    private const val MAX_TRANSLATIONS = 300

    private var instance: AutomaticTranslation? = null

    /** Starts or stops automatic translation as its switch says. Main thread only. */
    @JvmStatic
    fun update(context: Context) {
      val on = TranslationSettings.auto(SharedPreferencesUtils.getSharedPreferences(context))
      if (on && instance == null) {
        instance =
          AutomaticTranslation(context, PhoneEngine(context.applicationContext)).also {
            SpeechControllerImpl.setSpeechHold(it)
          }
      } else if (!on && instance != null) {
        stop()
      }
    }

    /**
     * Gives [done] the translation of [text] while Automatic translation is on, else [text], on the
     * main thread. For speech that SandFly's speech doesn't say, such as notifications read in the
     * voice chosen for their app.
     */
    @JvmStatic
    fun translate(text: CharSequence, done: (CharSequence) -> Unit) {
      val auto = instance ?: return done(text)
      auto.process(text, done)
    }

    @JvmStatic
    fun stop() {
      SpeechControllerImpl.setSpeechHold(null)
      instance?.close()
      instance = null
    }

    /** Speech parameters for speech that is never translated, such as translations. */
    @JvmStatic
    fun untranslated(): Bundle = Bundle().apply { putBoolean(SpeechControllerImpl.SPEECH_PARAM_RELEASED, true) }

    fun letters(text: String): Int = text.count { it.isLetter() }

    fun translationKey(service: TranslationService, from: String, to: String, text: String): String =
      "${service.id}|$from|$to|$text"
  }
}
