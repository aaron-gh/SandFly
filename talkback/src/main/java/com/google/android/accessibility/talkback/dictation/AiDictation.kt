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

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.BuildConfig
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Feedback.EditText.Action.INSERT
import com.google.android.accessibility.talkback.Feedback.Speech.Action.SILENCE
import com.google.android.accessibility.talkback.Feedback.Speech.Action.UNSILENCE
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.actor.gemini.AiProvider
import com.google.android.accessibility.talkback.actor.gemini.AiService
import com.google.android.accessibility.talkback.actor.gemini.GeminiApiKey
import com.google.android.accessibility.talkback.actor.gemini.GeminiModel
import com.google.android.accessibility.talkback.actor.gemini.local.LocalModelManager
import com.google.android.accessibility.talkback.actor.gemini.progress.DefaultProgressToneProvider
import com.google.android.accessibility.talkback.actor.gemini.progress.ProgressTonePlayer
import com.google.android.accessibility.talkback.dictation.DictationSettings.Recognition
import com.google.android.accessibility.talkback.permission.PermissionUtils
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.Role
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.IOException
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Dictate: types what the user says into the focused text field. The gesture starts recording and
 * the same gesture stops it; nothing stops it in between. The speech is then recognized as
 * Dictation settings say: by Google on-device speech recognition, Rambler (Gemini API), Whisper on
 * the phone, or Android's speech recognition. When Correct dictation with AI is on, the AI service chosen for dictation
 * cleans up the text as Gboard's Rambler does before it is typed: filler words, false starts and
 * self-corrections removed, punctuation and numbers formatted. Rambler (Gemini API) text arrives
 * cleaned up already.
 */
class AiDictation(
  private val service: TalkBackService,
  private val pipeline: Pipeline.FeedbackReturner,
) {
  private enum class Phase {
    IDLE,
    RECORDING,
    /** Recording ended; the speech is being recognized or corrected. */
    WORKING,
  }

  private val prefs = SharedPreferencesUtils.getSharedPreferences(service)
  private val handler = Handler(Looper.getMainLooper())
  private val worker =
    Executors.newSingleThreadExecutor { Thread(it, "dictation").apply { isDaemon = true } }
  private val tones =
    ProgressTonePlayer(
      DefaultProgressToneProvider(),
      { tone -> pipeline.returnFeedback(EVENT_ID_UNTRACKED, tone.tone) },
      { WORK_TONES_MS },
      {},
    )
  private var phase = Phase.IDLE
  private var field: AccessibilityNodeInfoCompat? = null
  private var recognition = Recognition.ANDROID
  private var capture: AudioCapture? = null
  private var continuous: ContinuousRecognition? = null
  @Volatile private var transcriber: SpeechTranscriber? = null
  /** The Rambler live transcription session of the dictation being recorded, if any. */
  private var live: RamblerLive? = null
  /** Counts dictations, so that the result of one that was given up on is ignored. */
  private var generation = 0
  /** Types the recognized text when the correction takes too long, or null when none waits. */
  private var giveUp: Runnable? = null

  /**
   * Starts recording for text to type into [node], or stops recording when it is on. While a
   * correction is awaited, types the recognized text at once instead.
   */
  fun start(node: AccessibilityNodeInfoCompat?): Boolean {
    when (phase) {
      Phase.RECORDING -> {
        stopRecording()
        return true
      }
      Phase.WORKING -> {
        giveUp?.let {
          handler.removeCallbacks(it)
          it.run()
        }
        return true
      }
      Phase.IDLE -> {}
    }
    if (node == null || Role.getRole(node) != Role.ROLE_EDIT_TEXT) {
      speak(service.getString(R.string.not_editable))
      return false
    }
    if (
      ContextCompat.checkSelfPermission(service, Manifest.permission.RECORD_AUDIO) !=
        PackageManager.PERMISSION_GRANTED
    ) {
      PermissionUtils.requestPermissions(service, Manifest.permission.RECORD_AUDIO)
      return false
    }
    if (!startRecording(DictationSettings.effectiveRecognition(service, prefs))) {
      speak(service.getString(R.string.dictation_unavailable))
      return false
    }
    field = node
    phase = Phase.RECORDING
    pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.sound(R.raw.chime_up))
    // Speech would be recorded with the user's.
    pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(SILENCE))
    return true
  }

  /** Starts recording for [used]. Returns false when it can't. */
  private fun startRecording(used: Recognition): Boolean {
    recognition = used
    if (used == Recognition.GOOGLE || used == Recognition.ANDROID) {
      val request = generation
      val recognizer =
        ContinuousRecognition(
          service,
          if (used == Recognition.GOOGLE) DictationSettings.GOOGLE_SERVICE else null,
          onDone = { text -> if (request == generation) recognized(text, cleaned = false) },
          onFailed = { error -> if (request == generation) recognitionFailed(used, error) },
        )
      if (!recognizer.start()) return false
      continuous = recognizer
      return true
    }
    // Rambler live transcription transcribes while the user speaks.
    val session =
      if (used == Recognition.RAMBLER && DictationSettings.ramblerLive(prefs)) {
        RamblerLive(DictationSettings.ramblerKey(prefs)).also { it.start() }
      } else {
        null
      }
    val recorder =
      AudioCapture(
        onLimit = { handler.post { if (phase == Phase.RECORDING) stopRecording() } },
        onChunk = session?.let { live -> { pcm -> live.send(pcm) } },
      )
    if (!recorder.start()) {
      session?.cancel()
      return false
    }
    capture = recorder
    live = session
    return true
  }

  private fun stopRecording() {
    phase = Phase.WORKING
    pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(UNSILENCE))
    pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.sound(R.raw.chime_down))
    continuous?.let {
      continuous = null
      it.stop()
      return
    }
    val pcm = capture?.stop() ?: ByteArray(0)
    capture = null
    if (!AudioCapture.hasSpeechLength(pcm)) {
      live?.cancel()
      live = null
      recognized("", cleaned = false)
      return
    }
    val request = ++generation
    val used = recognition
    tones.play(/* playTone= */ true, /* delayPlayTone= */ true)
    live?.let { session ->
      live = null
      session.finish { text ->
        if (request != generation) return@finish
        if (text != null) {
          tones.stop()
          recognized(text, cleaned = true)
        } else {
          // The session failed: the whole recording goes the usual way.
          LogUtils.w(TAG, "Live transcription failed, sending the recording")
          transcribeRecording(request, used, pcm)
        }
      }
      return
    }
    transcribeRecording(request, used, pcm)
  }

  /** Recognizes [pcm] on the worker thread, then types it, unless a newer dictation started. */
  private fun transcribeRecording(request: Int, used: Recognition, pcm: ByteArray) {
    worker.execute {
      val result =
        try {
          transcribe(used, pcm)
        } catch (e: Exception) {
          LogUtils.e(TAG, "Transcription failed: %s", e)
          null
        } catch (e: OutOfMemoryError) {
          LogUtils.e(TAG, "Transcription failed: %s", e)
          null
        }
      handler.post {
        if (request != generation) return@post
        tones.stop()
        if (result == null) {
          finish()
          speak(service.getString(R.string.dictation_transcription_failed))
        } else {
          recognized(result.first, cleaned = result.second)
        }
      }
    }
  }

  /**
   * Recognizes the speech in [pcm] with [used], or with Whisper when Rambler fails. Returns the
   * text and whether it is already cleaned up, or null when it couldn't be recognized.
   */
  private fun transcribe(used: Recognition, pcm: ByteArray): Pair<String, Boolean>? {
    if (used == Recognition.RAMBLER) {
      try {
        return DictationRequests.transcribe(DictationSettings.ramblerKey(prefs), AudioCapture.toWav(pcm)) to
          true
      } catch (e: IOException) {
        LogUtils.w(TAG, "Rambler failed: %s", e)
      }
    }
    val model = WhisperModels.get(service).activeModel() ?: return null
    val whisper =
      WhisperEngines.transcriber(
        WhisperModels.get(service).file(model),
        DictationSettings.whisperOnGpu(prefs),
      ) ?: return null
    transcriber = whisper
    try {
      return whisper.transcribe(AudioCapture.toFloats(pcm), whisperLanguage())?.let { it to false }
    } finally {
      transcriber = null
    }
  }

  private fun recognitionFailed(used: Recognition, error: Int) {
    continuous = null
    // Without a language pack for the phone's language, the next way of recognizing takes over,
    // while recording is still on.
    if (
      used == Recognition.GOOGLE &&
        phase == Phase.RECORDING &&
        error in LANGUAGE_ERRORS &&
        startRecording(DictationSettings.effectiveRecognition(service, prefs, skip = Recognition.GOOGLE))
    ) {
      LogUtils.w(TAG, "Google on-device recognition has no language pack, using %s", recognition)
      return
    }
    if (phase == Phase.RECORDING) {
      pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(UNSILENCE))
      pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.sound(R.raw.chime_down))
    }
    finish()
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

  /** Types [text], recognized from the user's speech, after correcting it when that is on. */
  private fun recognized(text: String, cleaned: Boolean) {
    val target = field
    val heard = text.trim()
    if (target == null || heard.isEmpty()) {
      finish()
      speak(service.getString(R.string.dictation_nothing_recognized))
      return
    }
    // A dictated password stays on the phone.
    val corrector =
      if (cleaned || target.isPassword || !DictationSettings.correctsWithAi(service, prefs)) null
      else DictationSettings.correctingService(service, prefs)
    if (corrector == null) {
      finish()
      type(target, heard)
      return
    }
    val request = ++generation
    val fallback = Runnable {
      if (request != generation) return@Runnable
      generation++
      LogUtils.w(TAG, "No correction in time, typing the recognized text")
      finish()
      speak(service.getString(R.string.dictation_not_corrected))
      type(target, heard)
    }
    giveUp = fallback
    handler.postDelayed(fallback, correctionTimeoutMs(corrector))
    tones.play(/* playTone= */ true, /* delayPlayTone= */ false)
    val prompt = prompt(textBeforeCursor(target), heard)
    worker.execute {
      val answer =
        try {
          correct(corrector, prompt)
        } catch (e: Exception) {
          LogUtils.w(TAG, "No correction: %s", e)
          null
        } catch (e: OutOfMemoryError) {
          LogUtils.w(TAG, "No correction: %s", e)
          null
        }
      handler.post {
        if (request != generation) return@post
        generation++
        handler.removeCallbacks(fallback)
        finish()
        val corrected = answer?.let(::cleanUp)
        if (corrected != null && isCorrection(heard, corrected)) {
          type(target, corrected)
        } else {
          speak(service.getString(R.string.dictation_not_corrected))
          type(target, heard)
        }
      }
    }
  }

  /** Asks [corrector], an [AiProvider] id or [DictationSettings.AI_DEVICE], to answer [prompt]. */
  private fun correct(corrector: String, prompt: String): String? {
    if (corrector == DictationSettings.AI_DEVICE) {
      return LocalModelManager.get(service).llm()?.generate(prompt, null)
    }
    val provider = AiProvider.of(corrector) ?: return null
    if (provider == AiProvider.GEMINI) {
      val key = GeminiApiKey.effectiveKey(prefs, BuildConfig.GEMINI_API_KEY)
      var failure: IOException? = null
      // The next model is asked when one is busy, as for descriptions.
      for (model in GeminiModel.chain(prefs)) {
        try {
          return DictationRequests.askGemini(key, model, prompt)
        } catch (e: DictationRequests.HttpException) {
          if (e.code != HTTP_TOO_MANY_REQUESTS && e.code != HTTP_UNAVAILABLE) throw e
          failure = e
        }
      }
      throw failure ?: IOException("No Gemini model")
    }
    return DictationRequests.askChat(
      AiService.chatUrl(provider.addressOf(prefs)),
      provider.apiKey(prefs),
      provider.model(prefs),
      prompt,
    )
  }

  private fun correctionTimeoutMs(corrector: String): Long =
    if (corrector == DictationSettings.AI_DEVICE) DEVICE_CORRECTION_TIMEOUT_MS else CORRECTION_TIMEOUT_MS

  private fun finish() {
    // For the next dictation: a language pack downloaded since the last one is used from then on.
    if (phase != Phase.IDLE) GoogleSpeechLanguages.refresh(service)
    phase = Phase.IDLE
    giveUp = null
    tones.stop()
  }

  /** Stops recording and drops what was recorded, as when SandFly turns off. */
  fun shutdown() {
    generation++
    giveUp?.let { handler.removeCallbacks(it) }
    continuous?.cancel()
    continuous = null
    capture?.stop()
    capture = null
    live?.cancel()
    live = null
    transcriber?.cancel()
    if (phase == Phase.RECORDING) pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(UNSILENCE))
    field = null
    finish()
  }

  /** The language Whisper is told the speech is in: the phone's. */
  private fun whisperLanguage(): String =
    when (val language = Locale.getDefault().language) {
      "iw" -> "he"
      "in" -> "id"
      "ji" -> "yi"
      "fil" -> "tl"
      "nb" -> "no"
      else -> language
    }

  /** Types [text] at the cursor, with the spaces it needs beside the text around it. */
  private fun type(target: AccessibilityNodeInfoCompat, text: String) {
    field = null
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
        // After speech is no longer silenced for recording, as voice commands do.
        .setDelayMs(SPEECH_DELAY_MS),
    )
  }

  internal companion object {
    private const val TAG = "AiDictation"
    /** How long progress tones play while speech is recognized or corrected. */
    private const val WORK_TONES_MS = 10 * 60_000L
    private const val HTTP_TOO_MANY_REQUESTS = 429
    private const val HTTP_UNAVAILABLE = 503
    /** SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED and ERROR_LANGUAGE_UNAVAILABLE. */
    private val LANGUAGE_ERRORS = setOf(12, 13)
    /** How much of the text before the cursor the AI service gets, for context. */
    private const val CONTEXT_CHARACTERS = 300
    /** The recognized text is typed when the correction takes longer than this. */
    private const val CORRECTION_TIMEOUT_MS = 20_000L
    /** A model on the phone is slower than one in the cloud. */
    private const val DEVICE_CORRECTION_TIMEOUT_MS = 60_000L
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

    /**
     * Asks for the cleanup that Gboard's Rambler does on Google's servers: filler words and false
     * starts removed, self-corrections resolved, and punctuation, numbers and lists formatted.
     */
    fun prompt(before: String, heard: String): String =
      "The text below came from speech recognition and will be typed into a text field. " +
        "Clean it up as a careful typist would:\n" +
        "- Remove filler words (such as \"um\", \"uh\", \"like\", \"you know\"), stutters, " +
        "repeated words and false starts.\n" +
        "- Where the speaker corrects themselves, keep only the correction: \"meet at two, no, " +
        "three\" becomes \"meet at three\".\n" +
        "- Add punctuation and capital letters, and correct words that speech recognition " +
        "probably misheard.\n" +
        "- Turn spoken punctuation, such as \"comma\" or \"new line\", into the marks.\n" +
        "- Write numbers, times, dates and amounts as digits where that is usual, and a spoken " +
        "list as a list.\n" +
        "Keep the speaker's meaning, wording and language otherwise. Don't answer, add to or " +
        "explain it. Reply with the cleaned-up text only.\n\n" +
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
