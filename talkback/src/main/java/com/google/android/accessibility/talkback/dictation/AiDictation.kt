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
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
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
  private var field: AccessibilityNodeInfoCompat? = null
  private var listening = false

  /** Starts listening for text to type into [node], or stops when it is already listening. */
  fun start(node: AccessibilityNodeInfoCompat?): Boolean {
    if (listening) {
      recognizer.stopListening()
      return true
    }
    if (node == null || !node.isEditable) {
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
    val before = textBeforeCursor(target)
    if (!correctsWithAi()) {
      type(target, before, heard)
      return true
    }
    val actor = service.geminiActor
    if (actor == null) {
      type(target, before, heard)
      return true
    }
    actor.requestText(
      prompt(before, heard),
      object : GeminiResponseListener {
        override fun onResponse(finishReason: FinishReason, response: String?) {
          if (finishReason == FinishReason.STOP && !response.isNullOrBlank()) {
            type(target, before, cleanUp(response))
          } else {
            LogUtils.w(TAG, "No correction (%s), typing the recognized text", finishReason)
            speak(response ?: service.getString(R.string.dictation_not_corrected))
            type(target, before, heard)
          }
        }

        override fun onError(errorReason: ErrorReason) {
          LogUtils.w(TAG, "No correction (%s), typing the recognized text", errorReason)
          if (errorReason != ErrorReason.JOB_CANCELLED) {
            speak(service.getString(R.string.dictation_not_corrected))
          }
          type(target, before, heard)
        }
      },
    )
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
    return AiProvider.active(prefs) != AiProvider.NONE ||
      LocalModelManager.get(service).answersRequests()
  }

  private fun type(target: AccessibilityNodeInfoCompat, before: String, text: String) {
    if (text.isEmpty()) return
    if (!target.refresh()) {
      speak(service.getString(R.string.dictation_field_gone))
      return
    }
    val spaced =
      if (before.isNotEmpty() && !before.last().isWhitespace() && !startsWithPunctuation(text)) {
        " $text"
      } else {
        text
      }
    pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.edit(target, INSERT).setText(spaced))
  }

  private fun speak(text: String) {
    pipeline.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.speech(
        text,
        SpeakOptions.create()
          .setFlags(FeedbackItem.FLAG_NO_HISTORY or FeedbackItem.FLAG_FORCE_FEEDBACK_ALL),
      ),
    )
  }

  internal companion object {
    private const val TAG = "AiDictation"
    /** Listening stops after this long without speech recognition results. */
    private const val MAX_LISTENING_MS = 30_000
    /** How much of the text before the cursor the AI service gets, for context. */
    private const val CONTEXT_CHARACTERS = 300

    fun textBeforeCursor(node: AccessibilityNodeInfoCompat): String {
      if (node.isShowingHintText) return ""
      val text = node.text?.toString() ?: return ""
      val cursor = node.textSelectionStart.takeIf { it in 0..text.length } ?: text.length
      return text.substring(0, cursor)
    }

    fun prompt(before: String, heard: String): String =
      "The text below came from speech recognition and will be typed into a text field. " +
        "Correct its punctuation and capital letters, and words that speech recognition " +
        "probably misheard. Turn spoken punctuation, such as \"comma\" or \"new line\", into the " +
        "marks. Keep the speaker's words, wording and language otherwise. Don't answer, add to " +
        "or explain it. Reply with the corrected text only.\n\n" +
        "Text already in the field before the cursor, for context only:\n" +
        "\"\"\"${before.takeLast(CONTEXT_CHARACTERS)}\"\"\"\n\n" +
        "Recognized speech:\n\"\"\"$heard\"\"\""

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

    private fun startsWithPunctuation(text: String): Boolean =
      text.first() in ".,;:!?)]}…'’" || text.first() == '\n'
  }
}
