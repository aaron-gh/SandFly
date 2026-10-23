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

package com.google.android.accessibility.talkback.dictation

import android.os.Handler
import android.os.Looper
import android.speech.SpeechRecognizer
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Feedback.EditText.Action.INSERT
import com.google.android.accessibility.talkback.Feedback.Speech.Action.SILENCE
import com.google.android.accessibility.talkback.Feedback.Speech.Action.UNSILENCE
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.actor.gemini.AiProvider
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.ErrorReason
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.FinishReason
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.GeminiResponseListener
import com.google.android.accessibility.talkback.actor.gemini.local.LocalModelManager
import com.google.android.accessibility.talkback.utils.SpeechRecognizerPerformer
import com.google.android.accessibility.talkback.utils.SpeechRecognizerPerformer.SpeechRecognizerRequester
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.Role
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Dictate: types what the user says into the focused text field. Android's speech recognition
 * turns the speech into text, then, when Correct dictation with AI is on and an AI service is in
 * use, the service corrects its punctuation, capital letters and misrecognized words before it is
 * typed.
 */
class AiDictation(
  private val service: TalkBackService,
  private val pipeline: Pipeline.FeedbackReturner,
) : SpeechRecognizerRequester {
  private val recognizer =
    SpeechRecognizerPerformer(service, MAX_LISTENING_MS, /* partialResultFastFeedback= */ false)
      .also { it.setListener(this) }
  private val handler = Handler(Looper.getMainLooper())
  private var field: AccessibilityNodeInfoCompat? = null
  private var listening = false
  /** Counts corrections, so that an answer to one that was given up on is ignored. */
  private var generation = 0
  /** Types the recognized text when the correction takes too long, or null when none waits. */
  private var giveUp: Runnable? = null

  /**
   * Starts listening for text to type into [node], or stops when it is already listening. While a
   * correction is awaited, types the recognized text at once instead.
   */
  fun start(node: AccessibilityNodeInfoCompat?): Boolean {
    if (listening) {
      recognizer.stopListening()
      return true
    }
    giveUp?.let {
      handler.removeCallbacks(it)
      it.run()
      return true
    }
    if (node == null || Role.getRole(node) != Role.ROLE_EDIT_TEXT) {
      speak(service.getString(R.string.not_editable))
      return false
    }
    field = node
    recognizer.doListening()
    return true
  }

  override fun onResult(command: String, isPartialResult: Boolean): Boolean {
    val target = field ?: return true
    field = null
    val heard = command.trim()
    if (heard.isEmpty()) return true
    // A dictated password stays on the phone.
    val actor = service.geminiActor
    if (target.isPassword || actor == null || !correctsWithAi()) {
      type(target, heard)
      return true
    }
    val request = ++generation
    val fallback = Runnable {
      if (request != generation) return@Runnable
      generation++
      giveUp = null
      LogUtils.w(TAG, "No correction in time, typing the recognized text")
      speak(service.getString(R.string.dictation_not_corrected))
      type(target, heard)
    }
    giveUp = fallback
    handler.postDelayed(fallback, CORRECTION_TIMEOUT_MS)
    actor.requestText(
      prompt(textBeforeCursor(target), heard),
      object : GeminiResponseListener {
        override fun onResponse(finishReason: FinishReason, response: String?) {
          if (!takeAnswer(request, fallback)) return
          val corrected = if (finishReason == FinishReason.STOP) response?.let(::cleanUp) else null
          if (corrected != null && isCorrection(heard, corrected)) {
            type(target, corrected)
          } else {
            LogUtils.w(TAG, "No correction (%s), typing the recognized text", finishReason)
            speak(
              response?.takeIf { finishReason != FinishReason.STOP }
                ?: service.getString(R.string.dictation_not_corrected)
            )
            type(target, heard)
          }
        }

        override fun onError(errorReason: ErrorReason) {
          if (!takeAnswer(request, fallback)) return
          LogUtils.w(TAG, "No correction (%s), typing the recognized text", errorReason)
          if (errorReason != ErrorReason.JOB_CANCELLED) {
            speak(service.getString(R.string.dictation_not_corrected))
          }
          type(target, heard)
        }
      },
    )
    return true
  }

  /** Whether the answer to [request] is still awaited; if so, it is the one used. */
  private fun takeAnswer(request: Int, fallback: Runnable): Boolean {
    if (request != generation) return false
    generation++
    handler.removeCallbacks(fallback)
    giveUp = null
    return true
  }

  override fun onFeedbackEvent(eventId: Int) {
    when (eventId) {
      SpeechRecognizerRequester.EVENT_START_LISTENING -> {
        listening = true
        pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.sound(R.raw.chime_up))
        pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(SILENCE))
      }
      SpeechRecognizerRequester.EVENT_STOP_LISTENING -> {
        listening = false
        pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(UNSILENCE))
        pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.sound(R.raw.chime_down))
      }
      SpeechRecognizerRequester.EVENT_MIC_PERMISSION_NOT_GRANTED ->
        speak(service.getString(R.string.voice_commands_no_mic_permissions))
      SpeechRecognizerRequester.EVENT_PLATFORM_NOT_SUPPORT,
      SpeechRecognizerRequester.EVENT_FEATURE_UNAVAILABLE ->
        speak(service.getString(R.string.dictation_unavailable))
      else -> {}
    }
  }

  override fun onError(error: Int) {
    field = null
    speak(
      service.getString(
        when (error) {
          SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> R.string.voice_commands_no_mic_permissions
          SpeechRecognizer.ERROR_NO_MATCH,
          SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> R.string.dictation_nothing_recognized
          else -> R.string.dictation_unavailable
        }
      )
    )
  }

  /** Stops listening, as when SandFly turns off. */
  fun shutdown() {
    field = null
    generation++
    giveUp?.let { handler.removeCallbacks(it) }
    giveUp = null
    recognizer.reset()
  }

  private fun correctsWithAi(): Boolean {
    val prefs = SharedPreferencesUtils.getSharedPreferences(service)
    if (
      !prefs.getBoolean(
        service.getString(R.string.pref_ai_dictation_key),
        service.resources.getBoolean(R.bool.pref_ai_dictation_default),
      )
    ) {
      return false
    }
    val provider = AiProvider.active(prefs)
    return (provider != AiProvider.NONE && provider.isReady(prefs)) ||
      LocalModelManager.get(service).answersRequests()
  }

  /** Types [text] at the cursor, with the spaces it needs beside the text around it. */
  private fun type(target: AccessibilityNodeInfoCompat, text: String) {
    if (text.isEmpty()) return
    if (
      !target.refresh() ||
        !pipeline.returnFeedback(
          EVENT_ID_UNTRACKED,
          Feedback.edit(target, INSERT)
            .setText(spaced(textBeforeCursor(target), text, textAfterCursor(target))),
        )
    ) {
      speak(service.getString(R.string.dictation_field_gone))
    }
  }

  private fun speak(text: String) {
    pipeline.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.speech(
        text,
        SpeakOptions.create()
          .setFlags(FeedbackItem.FLAG_NO_HISTORY or FeedbackItem.FLAG_FORCE_FEEDBACK_ALL),
      )
        // After speech is no longer silenced for listening, as voice commands do.
        .setDelayMs(SPEECH_DELAY_MS),
    )
  }

  internal companion object {
    private const val TAG = "AiDictation"
    /** Listening stops after this long without speech recognition results. */
    private const val MAX_LISTENING_MS = 30_000
    /** How much of the text before the cursor the AI service gets, for context. */
    private const val CONTEXT_CHARACTERS = 300
    /** The recognized text is typed when the correction takes longer than this. */
    private const val CORRECTION_TIMEOUT_MS = 20_000L
    private const val SPEECH_DELAY_MS = 100

    fun textBeforeCursor(node: AccessibilityNodeInfoCompat): String {
      if (node.isShowingHintText || AccessibilityNodeInfoUtils.isEmptyEditTextRegardlessOfHint(node)) return ""
      val text = node.text?.toString() ?: return ""
      val cursor = node.textSelectionStart.takeIf { it in 0..text.length } ?: text.length
      return text.substring(0, cursor)
    }

    fun textAfterCursor(node: AccessibilityNodeInfoCompat): String {
      if (node.isShowingHintText || AccessibilityNodeInfoUtils.isEmptyEditTextRegardlessOfHint(node)) return ""
      val text = node.text?.toString() ?: return ""
      val cursor = node.textSelectionEnd.takeIf { it in 0..text.length } ?: text.length
      return text.substring(cursor)
    }

    /** [text] with a space before it and after it where the text around it needs one. */
    fun spaced(before: String, text: String, after: String): String {
      val leading =
        before.isNotEmpty() &&
          !before.last().isWhitespace() &&
          before.last() !in OPENING &&
          text.first() !in CLOSING
      val trailing = after.isNotEmpty() && after.first().isLetterOrDigit()
      return (if (leading) " " else "") + text + (if (trailing) " " else "")
    }

    /**
     * Whether [answer] is a correction of [heard], not something else, such as an answer to it or
     * an explanation, which would be much longer.
     */
    fun isCorrection(heard: String, answer: String): Boolean =
      answer.isNotEmpty() &&
        answer.length <= heard.length * 2 + 40 &&
        answer.lines().size <= heard.lines().size * 2 + 1

    fun prompt(before: String, heard: String): String =
      "The text below came from speech recognition and will be typed into a text field. " +
        "Correct its punctuation and capital letters, and words that speech recognition " +
        "probably misheard. Turn spoken punctuation, such as \"comma\" or \"new line\", into the " +
        "marks. Keep the speaker's words, wording and language otherwise. Don't answer, add to " +
        "or explain it. Reply with the corrected text only.\n\n" +
        "Text already in the field before the cursor, for context only:\n" +
        "\"\"\"${quoted(before.takeLast(CONTEXT_CHARACTERS))}\"\"\"\n\n" +
        "Recognized speech:\n\"\"\"${quoted(heard)}\"\"\""

    /** Text that can't end the triple quotes around it. */
    private fun quoted(text: String): String = text.replace("\"\"\"", "'''")

    /** Removes the triple quotes or code fence that a model put around its answer. */
    fun cleanUp(answer: String): String {
      var text = answer.trim()
      if (text.startsWith("```")) {
        text = text.removePrefix("```").substringAfter('\n', text.removePrefix("```"))
        text = text.removeSuffix("```").trim()
      }
      if (text.startsWith("\"\"\"") && text.endsWith("\"\"\"") && text.length >= 6) {
        text = text.substring(3, text.length - 3).trim()
      }
      return text
    }

    private const val OPENING = "([{\"“‘«¿¡"
    private const val CLOSING = ".,;:!?)]}…'’”»\n"
  }
}
