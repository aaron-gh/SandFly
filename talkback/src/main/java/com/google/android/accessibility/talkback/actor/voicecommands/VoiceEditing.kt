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

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.content.ContextCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.commands.Commands
import com.google.android.accessibility.talkback.dictation.SandflyDictation
import com.google.android.accessibility.talkback.macros.Macros
import com.google.android.accessibility.talkback.permission.PermissionUtils
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.Role
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.Locale

/**
 * Start voice editing: records voice commands for the focused text field one after another, with
 * SandFly's dictation, until the command runs again, a stop phrase is said, the Voice editing time
 * limit passes or focus leaves the field. Only text editing and Navigate by text commands, custom
 * voice commands for them or for macros, typing, the text editing phrases, such as "delete next 3
 * words", and the text editing voice commands of TalkBack run. Speech is silenced while recording, as for voice commands; after a command, recording
 * starts again once SandFly's speech ends. Main thread.
 */
class VoiceEditing(private val service: TalkBackService, private val processor: VoiceCommandProcessor) {
  private val handler = Handler(Looper.getMainLooper())

  private var active = false

  // Bumped when a session starts or ends, so that late results of an ended session are dropped.
  private var generation = 0

  private var field: AccessibilityNodeInfo? = null
  private var startedAt = 0L
  private var limitMs = 0L
  private var recordingStartedAt = 0L
  private var failures = 0

  // Whether recording is on for this session, and whether its commands are running.
  private var listening = false
  private var running = false

  // Whether the next recording plays the start chime: not after a recording in which nothing was
  // said, which starts again at once.
  private var chime = true

  private var locale: Locale = Locale.getDefault()
  private var phrases: VoiceGrammar.Phrases? = null
  private var stopPhrases: List<String> = emptyList()
  private var resolver: VoiceEditingRules.Resolver? = null

  private val watch =
    object : Runnable {
      override fun run() {
        if (!active) return
        val now = SystemClock.elapsedRealtime()
        if (VoiceEditingRules.expired(now, startedAt, limitMs) || !stillInField()) {
          stop(announce = true)
          return
        }
        handler.postDelayed(this, WATCH_MS)
      }
    }

  init {
    instance = this
  }

  /** Starts voice editing in [target], the focused text field, or ends it when it is on. */
  fun toggleIn(target: AccessibilityNodeInfoCompat?): Boolean {
    if (active) {
      stop(announce = true)
      return true
    }
    if (target == null || Role.getRole(target) != Role.ROLE_EDIT_TEXT) {
      say(service.getString(R.string.not_editable))
      return false
    }
    if (!SandflyDictation.isAvailable) {
      say(service.getString(R.string.dictation_unavailable))
      return false
    }
    if (
      ContextCompat.checkSelfPermission(service, Manifest.permission.RECORD_AUDIO) !=
        PackageManager.PERMISSION_GRANTED
    ) {
      PermissionUtils.requestPermissions(service, Manifest.permission.RECORD_AUDIO)
      say(service.getString(R.string.voice_commands_no_mic_permissions))
      return false
    }
    val prefs = SharedPreferencesUtils.getSharedPreferences(service)
    val minutes =
      VoiceEditingRules.timeLimitMinutes(
        prefs.getString(service.getString(R.string.pref_voice_editing_time_limit_key), null)
      )
    active = true
    val id = ++generation
    field = target.unwrap()
    startedAt = SystemClock.elapsedRealtime()
    limitMs = minutes * MINUTE_MS
    failures = 0
    chime = true
    locale = service.resources.configuration.locales[0] ?: Locale.getDefault()
    loadPhrases()
    LogUtils.i(TAG, "Voice editing on for %d minutes", minutes)
    say(service.getString(R.string.voice_editing_on))
    handler.postDelayed(watch, WATCH_MS)
    val custom = CustomVoiceCommands.read(prefs)
    val sessionLocale = locale
    EXECUTOR.execute {
      val loaded =
        try {
          loadResolver(custom, sessionLocale)
        } catch (e: RuntimeException) {
          LogUtils.w(TAG, "Cannot read the commands: %s", e)
          null
        }
      handler.post {
        if (id != generation) return@post
        if (loaded == null) {
          stop(announce = true)
          return@post
        }
        resolver = loaded
        afterSpeech(id, SPEECH_START_MS) { listen(id) }
      }
    }
    return true
  }

  /** Ends voice editing, if it is on, and says so with [announce]. */
  fun stop(announce: Boolean) {
    if (!active) return
    active = false
    generation++
    handler.removeCallbacksAndMessages(null)
    if (listening) {
      listening = false
      SandflyDictation.cancelListen()
    }
    if (running) {
      running = false
      VoicePlanRunner.stop()
    }
    field = null
    resolver = null
    LogUtils.i(TAG, "Voice editing off")
    if (announce) say(service.getString(R.string.voice_editing_off))
  }

  /** Ends voice editing without speech and lets go of the service, as when SandFly turns off. */
  fun release() {
    stop(announce = false)
    if (instance === this) instance = null
  }

  private fun loadPhrases() {
    val resources = service.resources
    fun array(id: Int): List<String> = resources.getStringArray(id).toList()
    val cancel = array(R.array.voice_commands_cancel_words)
    phrases =
      VoiceEditingRules.phrases(
        chain = array(R.array.voice_commands_chain_words),
        and = array(R.array.voice_commands_and_words),
        fillersStart = array(R.array.voice_commands_fillers_start),
        fillersEnd = array(R.array.voice_commands_fillers_end),
        cancel = cancel,
        type = array(R.array.voice_commands_type_text),
        run = array(R.array.voice_commands_run),
        text = VoiceTargets.textPhrases(service),
      )
    // The command's own name ends it too, as running the command again does.
    stopPhrases =
      array(R.array.voice_editing_stop_words) + cancel + service.getString(R.string.shortcut_voice_editing)
  }

  /** The editing commands and custom voice commands, read off the main thread. */
  private fun loadResolver(custom: List<CustomVoiceCommands.Entry>, locale: Locale): VoiceEditingRules.Resolver {
    val excluded =
      setOf(
        service.getString(R.string.shortcut_value_voice_editing),
        service.getString(R.string.shortcut_value_dictate),
        service.getString(R.string.shortcut_value_voice_commands),
      )
    val commands =
      VoiceEditingRules.editingCommands(
        Commands.categories(service),
        setOf(R.string.shortcut_title_text_editing, R.string.shortcut_title_navigate_by_text),
        excluded,
      )
    val values = commands.map { it.value }.toSet()
    return VoiceEditingRules.Resolver(
      VoiceEditingRules.commandNames(commands, locale, VoiceTargets.aliases(service)),
      custom,
      { value -> value in values || (Macros.isCommand(value) && value != Macros.RECORD_COMMAND) },
      locale,
    )
  }

  private fun listen(id: Int) {
    if (id != generation || !active) return
    if (VoiceEditingRules.expired(SystemClock.elapsedRealtime(), startedAt, limitMs) || !stillInField()) {
      stop(announce = true)
      return
    }
    val playChime = chime
    chime = false
    listening = true
    recordingStartedAt = SystemClock.elapsedRealtime()
    val started =
      SandflyDictation.listen(/* correct= */ false, /* session= */ true, playChime) { text ->
        if (id == generation) listening = false
        // Never at once: a failed start answers before listen returns.
        handler.post { heard(id, text) }
      }
    if (!started && id == generation) {
      listening = false
      LogUtils.w(TAG, "Recording didn't start")
      stop(announce = true)
    }
  }

  private fun heard(id: Int, text: String?) {
    if (id != generation || !active) return
    if (text.isNullOrBlank()) {
      val error = SandflyDictation.lastListenError
      val recordedMs = SystemClock.elapsedRealtime() - recordingStartedAt
      when (VoiceEditingRules.afterNoText(error, failures, recordedMs)) {
        VoiceEditingRules.AfterNoText.LISTEN -> {
          failures = 0
          afterSpeech(id, graceMs = 0) { listen(id) }
        }
        VoiceEditingRules.AfterNoText.RETRY -> {
          failures++
          chime = true
          LogUtils.w(TAG, "Recognition failed with %d, recording again", error)
          handler.postDelayed(
            { if (id == generation) afterSpeech(id, graceMs = 0) { listen(id) } },
            VoiceEditingRules.retryDelayMs(failures),
          )
        }
        VoiceEditingRules.AfterNoText.STOP -> {
          LogUtils.w(TAG, "Recognition failed with %d, ending voice editing", error)
          stop(announce = true)
        }
      }
      return
    }
    failures = 0
    chime = true
    if (!stillInField() || VoiceEditingRules.isStopPhrase(text, stopPhrases, locale)) {
      stop(announce = true)
      return
    }
    val grammarPhrases = phrases
    val grammarResolver = resolver
    if (grammarPhrases == null || grammarResolver == null) {
      stop(announce = true)
      return
    }
    val sessionLocale = locale
    EXECUTOR.execute {
      val result =
        try {
          VoiceGrammar(grammarPhrases, grammarResolver, sessionLocale).parse(text)
        } catch (e: RuntimeException) {
          LogUtils.w(TAG, "Cannot read the voice command: %s", e)
          null
        }
      // A text the grammar doesn't read, such as a long one, can still be a TalkBack command.
      val steps = result?.steps?.takeIf { it.isNotEmpty() } ?: listOf(VoiceStep.Legacy(text))
      handler.post { if (id == generation && active) runSteps(id, steps) }
    }
  }

  /** Runs [steps], then records again once SandFly's speech ends. */
  private fun runSteps(id: Int, steps: List<VoiceStep>) {
    running = true
    VoicePlanRunner.run(
      service,
      steps,
      locale,
      legacy = { command -> processor.handleEditingIfRecognized(command) },
      onEnd = {
        handler.post {
          if (id != generation) return@post
          running = false
          afterSpeech(id, SPEECH_START_MS) { listen(id) }
        }
      },
    )
  }

  /**
   * Runs [then] once SandFly isn't speaking, waiting up to [graceMs] for the speech of a command to
   * start, so that recording doesn't pick up speech or silence it.
   */
  private fun afterSpeech(id: Int, graceMs: Long, then: () -> Unit) {
    val start = SystemClock.uptimeMillis()
    var started = false
    fun check() {
      if (id != generation) return
      val speaking = service.speechController?.isSpeakingOrSpeechQueued == true
      started = started || speaking
      val elapsed = SystemClock.uptimeMillis() - start
      if (VoiceEditingRules.keepWaiting(speaking, started, elapsed, graceMs, SPEECH_MAX_MS)) {
        handler.postDelayed({ check() }, POLL_MS)
      } else {
        then()
      }
    }
    if (graceMs > 0) handler.postDelayed({ check() }, POLL_MS) else check()
  }

  /** Whether focus is still in the field being edited. */
  private fun stillInField(): Boolean {
    val field = field ?: return false
    return try {
      val accessibility = service.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
      val input = service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
      VoiceEditingRules.stillInField(
        accessibilityFocusInField = accessibility == field,
        accessibilityFocusNone = accessibility == null,
        accessibilityFocusOnKeyboard =
          accessibility?.window?.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD,
        inputFocusInField = input == field,
      )
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Cannot find the focus: %s", e)
      false
    }
  }

  private fun say(text: String) {
    service.feedbackReturner?.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.speech(
        text,
        SpeakOptions.create().setFlags(FeedbackItem.FLAG_NO_HISTORY or FeedbackItem.FLAG_FORCE_FEEDBACK_ALL),
      ),
    )
  }

  companion object {
    private const val TAG = "VoiceEditing"

    /** How often the time limit and the focus are checked. */
    private const val WATCH_MS = 2_000L

    private const val MINUTE_MS = 60_000L

    private const val POLL_MS = 100L

    /** How long a command's speech can take to start, and to finish, before recording again. */
    private const val SPEECH_START_MS = 800L
    private const val SPEECH_MAX_MS = 60_000L

    private val EXECUTOR = VoiceCommandEngine.newExecutor()

    /** The voice editing of the running screen reader, or null. */
    @Volatile private var instance: VoiceEditing? = null

    /** Starts voice editing in [target], or ends it when it is on. Returns false when it can't start. */
    @JvmStatic
    fun toggle(target: AccessibilityNodeInfoCompat?): Boolean = instance?.toggleIn(target) ?: false

    /** Ends voice editing without speech, as when another command records speech. */
    @JvmStatic
    fun end() {
      instance?.stop(announce = false)
    }

    /** Ends voice editing and lets go of the service, as when SandFly turns off. */
    @JvmStatic
    fun shutdown() {
      instance?.release()
    }
  }
}
