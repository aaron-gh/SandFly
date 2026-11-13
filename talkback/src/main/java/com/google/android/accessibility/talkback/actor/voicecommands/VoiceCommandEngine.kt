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

package com.google.android.accessibility.talkback.actor.voicecommands

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.actor.gemini.AiTextRequests
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.Locale
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Handles what was said as a voice command: custom voice commands, command names, the phrase
 * patterns and chains of them first, then TalkBack's own voice commands, then, when the user turned
 * it on, the AI service. Main thread.
 */
class VoiceCommandEngine(
  private val service: TalkBackService,
  private val processor: VoiceCommandProcessor,
) {
  private val mainHandler = Handler(Looper.getMainLooper())

  // Bumped for each command, so that a command's late results are dropped after another starts.
  private var generation = 0

  private var ai: AiTextRequests? = null

  private var phrases: Pair<Locale, VoiceGrammar.Phrases>? = null

  /** Handles [said], the text recognized. */
  fun handle(said: String) {
    VoicePlanRunner.stop()
    ai?.cancel()
    val id = ++generation
    val locale = locale()
    val phrases = phrases(locale)
    EXECUTOR.execute {
      val targets = VoiceTargets(service, locale)
      val result =
        try {
          VoiceGrammar(phrases, targets, locale).parse(said)
        } catch (e: Throwable) {
          LogUtils.w(TAG, "Cannot read the voice command: %s", e)
          null
        }
      mainHandler.post { if (id == generation) decide(id, said, result, targets, locale) }
    }
  }

  /** Stops the command running and drops results still to come, as when the service stops. */
  fun stop() {
    generation++
    VoicePlanRunner.stop()
    ai?.cancel()
  }

  private fun decide(id: Int, said: String, result: VoiceGrammar.Result?, targets: VoiceTargets, locale: Locale) {
    val legacyText = VoiceCommandActor.commandText(said)
    if (result == null) {
      if (!processor.handleIfRecognized(legacyText)) processor.sayNotRecognized(legacyText)
      return
    }
    if (result.cancel) {
      processor.handleIfRecognized(legacyText)
      return
    }
    if (result.complete) {
      run(result.steps, locale)
      return
    }
    val useAi = aiEnabled(service) && AiTextRequests.isSetUp(service)
    if (result.parts == 1 && !(useAi && result.joined)) {
      if (processor.handleIfRecognized(legacyText)) return
      if (useAi) askAi(id, said, targets, locale) else processor.sayNotRecognized(legacyText)
      return
    }
    if (useAi) askAi(id, said, targets, locale) else run(result.steps, locale)
  }

  private fun run(steps: List<VoiceStep>, locale: Locale) {
    VoicePlanRunner.run(service, steps, locale) { text -> processor.handleIfRecognized(text) }
  }

  private fun askAi(id: Int, said: String, targets: VoiceTargets, locale: Locale) {
    EXECUTOR.execute {
      val prompt =
        try {
          VoiceCommandAi.prompt(
            said,
            targets.categories,
            VoiceCommandAi.screenItems(service),
            locale.getDisplayLanguage(Locale.ENGLISH),
          )
        } catch (e: Throwable) {
          LogUtils.w(TAG, "Cannot make the prompt: %s", e)
          null
        }
      mainHandler.post {
        if (id != generation) return@post
        if (prompt == null) {
          say(service.getString(R.string.voice_commands_error))
          return@post
        }
        val requests = ai ?: AiTextRequests(service) { say(it) }.also { ai = it }
        requests.ask(prompt, json = true) { answer, failure ->
          if (id != generation) return@ask
          if (answer == null) {
            // A cancelled request has no failure.
            failure?.let { say(it.userMessage(service)) }
            return@ask
          }
          val steps = VoiceCommandAi.parse(answer, targets.commandsByValue)
          if (steps.isNullOrEmpty()) {
            LogUtils.w(TAG, "No steps in the AI answer")
            processor.sayNotRecognized(VoiceCommandActor.commandText(said))
          } else {
            run(steps, locale)
          }
        }
      }
    }
  }

  private fun say(text: String) {
    service.feedbackReturner?.returnFeedback(EVENT_ID_UNTRACKED, Feedback.requestedSpeech(text))
  }

  private fun locale(): Locale = service.resources.configuration.locales[0] ?: Locale.getDefault()

  private fun phrases(locale: Locale): VoiceGrammar.Phrases {
    phrases?.takeIf { it.first == locale }?.let { return it.second }
    val resources = service.resources
    fun array(id: Int): List<String> = resources.getStringArray(id).toList()
    val loaded =
      VoiceGrammar.Phrases(
        chain = array(R.array.voice_commands_chain_words),
        and = array(R.array.voice_commands_and_words),
        fillersStart = array(R.array.voice_commands_fillers_start),
        fillersEnd = array(R.array.voice_commands_fillers_end),
        cancel = array(R.array.voice_commands_cancel_words),
        sendImageDescription = array(R.array.voice_commands_send_image_description),
        sendScreenDescription = array(R.array.voice_commands_send_screen_description),
        sendPrevious = array(R.array.voice_commands_send_previous),
        message = array(R.array.voice_commands_message),
        call = array(R.array.voice_commands_call),
        describeScreen = array(R.array.voice_commands_describe_screen),
        describeImage = array(R.array.voice_commands_describe_image),
        copy = array(R.array.voice_commands_copy_result),
        wait = array(R.array.voice_commands_wait),
        type = array(R.array.voice_commands_type_text),
        run = array(R.array.voice_commands_run),
        open = array(R.array.voice_commands_open),
        tap = array(R.array.voice_commands_tap),
      )
    phrases = locale to loaded
    return loaded
  }

  companion object {
    private const val TAG = "VoiceCommandEngine"

    private val EXECUTOR = newExecutor()

    /** A thread for voice command work, which ends when idle. */
    @JvmStatic
    fun newExecutor(): ThreadPoolExecutor =
      ThreadPoolExecutor(1, 1, IDLE_SECONDS, TimeUnit.SECONDS, LinkedBlockingQueue()).apply {
        allowCoreThreadTimeOut(true)
      }

    private const val IDLE_SECONDS = 30L

    /** Whether voice commands that the phrase patterns don't understand go to the AI service. */
    @JvmStatic
    fun aiEnabled(context: Context): Boolean =
      SharedPreferencesUtils.getSharedPreferences(context)
        .getBoolean(context.getString(R.string.pref_voice_commands_ai_key), false)
  }
}
