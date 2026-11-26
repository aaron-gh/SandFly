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

package com.google.android.accessibility.talkback.macros

import android.app.KeyguardManager
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo
import com.google.android.accessibility.talkback.gesture.OpenAppActions
import com.google.android.accessibility.talkback.labeling.TalkBackLabelManager
import com.google.android.accessibility.talkback.report.ReportNotes
import com.google.android.accessibility.talkback.screenelements.ScreenElements
import com.google.android.accessibility.talkback.scripting.Scripts
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechController
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions
import com.google.android.libraries.accessibility.utils.device.ScreenUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils
import com.google.android.accessibility.utils.output.OutputMute

/**
 * Runs macros, one at a time: starting a macro stops the one running.
 *
 * Steps run on the main thread, one after another through a handler, so gestures and the screen
 * reader go on between them. Looking for an item to tap and checking conditions read the windows on
 * a thread of the run's own, since each node they read asks the app. A step that fails says so and
 * stops the macro.
 *
 * Called on the main thread.
 */
object MacroRunner {
  private const val TAG = "MacroRunner"

  /** The time after a step that changes the screen, such as a command, for the change to start. */
  private const val STEP_GAP_MS = 300L

  private const val POLL_MS = 250L

  /** The time between two runs of a repeat's steps, so that a repeat of quick steps doesn't spin. */
  private const val REPEAT_GAP_MS = 100L

  /** The most items in the focused item whose text a test reads. */
  private const val MAX_FOCUS_NODES = 50

  /** How long a Type text step waits for an edit field to have input focus. */
  private const val FIELD_TIMEOUT_MS = 3_000L

  private const val MAX_NODES = 5_000
  private const val MAX_CLICK_PARENTS = 6
  /** The most items of a row looked through for a checked switch. */
  private const val MAX_ROW_NODES = 30

  /** How long the macros shown in the Actions menu of an item are reused for the same item. */
  private const val SHOWN_IN_ACTIONS_REUSE_MS = 1_000L

  private val shownInActionsCache = ShownInActions(SHOWN_IN_ACTIONS_REUSE_MS)

  private val mainHandler = Handler(Looper.getMainLooper())

  private var current: Run? = null

  /** Returns the ID of the macro running, or null. */
  @JvmStatic fun runningId(): String? = current?.macro?.id?.takeIf { it != UNSAVED_ID }

  /**
   * Runs the command [value]: the macro whose command it is, or [Macros.STOP_COMMAND]. Returns
   * false when there is no such macro or SandFly isn't running.
   */
  @JvmStatic
  fun runCommand(value: String): Boolean {
    if (value == Macros.STOP_COMMAND) {
      // While steps are recorded, it ends the recording on the macro's screen.
      TalkBackService.getInstance()?.let { if (MacroRecorder.openMacro(it)) return true }
      stop()
      return true
    }
    if (value == Macros.RECORD_COMMAND) {
      return TalkBackService.getInstance()?.let { MacroRecorder.recordOrStop(it) } ?: false
    }
    val id = Macros.idOf(value) ?: return false
    return run(id)
  }

  /**
   * Runs the macro [id], stopping the one running, when its condition is met. [sayNotMet] says so
   * when it isn't. Returns false when it can't run.
   */
  @JvmStatic
  @JvmOverloads
  fun run(id: String, sayNotMet: Boolean = false, onDone: ((Boolean) -> Unit)? = null): Boolean {
    val service = TalkBackService.getInstance() ?: return false
    val macro = Macros.find(SharedPreferencesUtils.getSharedPreferences(service), id) ?: return false
    start(service, macro, sayNotMet, onDone)
    return true
  }

  /**
   * Runs [steps] as a macro that isn't saved, as a voice command does, stopping the macro running.
   * With [quiet], a step that fails says nothing. [onDone] gets whether every step ran, unless the
   * run is stopped. Returns false when SandFly isn't running.
   */
  @JvmStatic
  fun runSteps(steps: List<Macros.Step>, quiet: Boolean, onDone: (Boolean) -> Unit): Boolean {
    val service = TalkBackService.getInstance() ?: return false
    start(service, Macros.Macro(UNSAVED_ID, "", steps), sayNotMet = false, onDone, quiet)
    return true
  }

  /**
   * Runs [macro], which isn't one of the user's own, such as one a script gives, when its
   * condition is met, stopping the macro running. Returns false when SandFly isn't running.
   */
  @JvmStatic
  @JvmOverloads
  fun runMacro(macro: Macros.Macro, values: Map<String, String> = emptyMap()): Boolean {
    val service = TalkBackService.getInstance() ?: return false
    start(service, macro, sayNotMet = false, onDone = null, values = values)
    return true
  }

  private const val UNSAVED_ID = ""

  private const val TONE_MS = 200

  /** The kinds of address an Open link step opens. */
  private val LINK_SCHEMES = setOf("http", "https", "mailto", "tel", "sms", "smsto", "geo", "market")

  /** SandFly's sounds a Play sound step plays, by [Macros.SOUNDS] names. */
  private val SOUNDS: Map<String, Int> =
    mapOf(
      "focus" to R.raw.focus,
      "actionable" to R.raw.focus_actionable,
      "click" to R.raw.tick,
      "longClick" to R.raw.long_clicked,
      "scroll" to R.raw.scroll_tone,
      "listEnter" to R.raw.chime_up,
      "listExit" to R.raw.chime_down,
      "end" to R.raw.complete,
      "enter" to R.raw.view_entered,
      "typo" to R.raw.typo,
      "beep" to R.raw.volume_beep,
    )

  private fun start(
    service: TalkBackService,
    macro: Macros.Macro,
    sayNotMet: Boolean,
    onDone: ((Boolean) -> Unit)?,
    quiet: Boolean = false,
    values: Map<String, String> = emptyMap(),
  ) {
    stop()
    val run = Run(service, macro, onDone, quiet, values)
    current = run
    run.later(0) { run.start(sayNotMet) }
  }

  /** Stops the macro running, if one is. */
  @JvmStatic
  fun stop() {
    current?.finish()
    current = null
  }

  /** One run of [macro]. Its callbacks on the main handler are posted with the run as token. */
  private class Run(
    val service: TalkBackService,
    val macro: Macros.Macro,
    private var onDone: ((Boolean) -> Unit)?,
    private val quiet: Boolean = false,
    given: Map<String, String> = emptyMap(),
  ) {
    @Volatile var finished = false

    private val prefs = SharedPreferencesUtils.getSharedPreferences(service)
    private val scope = Macros.valueScope(macro.id)

    // The values of the run: those kept for its macros, then those it was given, such as the text
    // of the notification that started it. Read on the search thread too.
    private val values: MutableMap<String, String> =
      java.util.concurrent.ConcurrentHashMap(Macros.values(prefs, scope) + given)

    /** [text] with its values filled in, and the values every run has, such as {item}. */
    private fun fill(text: String): String {
      if ('{' !in text) return text
      val all = HashMap(values)
      if ("{item}" in text) all["item"] = focusedText()
      if ("{clipboard}" in text) {
        all["clipboard"] =
          service.getSystemService(android.content.ClipboardManager::class.java)
            ?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(service)?.toString().orEmpty()
      }
      if ("{battery}" in text) DeviceActions.batteryLevel(service)?.let { all["battery"] = it.toString() }
      // {device.wifi} and the like: the state of a device control.
      Regex("\\{device\\.([A-Za-z]+)\\}").findAll(text).forEach { match ->
        val name = match.groupValues[1]
        DeviceActions.state(service, name)?.let { all["device.$name"] = it.text }
      }
      val now = java.util.Date()
      all.putIfAbsent("time", android.text.format.DateFormat.getTimeFormat(service).format(now))
      all.putIfAbsent("date", android.text.format.DateFormat.getLongDateFormat(service).format(now))
      return Macros.fill(text, all)
    }

    private fun focusedText(): String =
      try {
        service.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)?.let { node ->
          (node.contentDescription?.takeIf { it.isNotBlank() } ?: node.text)?.toString() ?: innerText(node)
        }.orEmpty()
      } catch (e: RuntimeException) {
        ""
      }

    private fun keep(name: String, value: String) {
      values[name] = value.take(Macros.MAX_VALUE_CHARS)
      Macros.setValue(prefs, scope, name, value)
    }

    /** Runs the [Macros.Do] step [step] at [index]. */
    private fun runDo(index: Int, step: Macros.Do) {
      val text = fill(step.text)
      val next = { later(STEP_GAP_MS) { runStep(index + 1) } }
      when (step.kind) {
        Macros.DoKind.SET_VALUE -> {
          keep(step.name, text)
          runStep(index + 1)
        }
        Macros.DoKind.PLAY_SOUND -> {
          SOUNDS[step.text]?.let { service.feedbackReturner?.returnFeedback(EVENT_ID_UNTRACKED, Feedback.sound(it)) }
            ?: return fail(index, step, "no sound ${step.text}")
          runStep(index + 1)
        }
        Macros.DoKind.VIBRATE -> {
          val vibrator =
            service.getSystemService(android.os.Vibrator::class.java)?.takeUnless { OutputMute.isVibrationMuted() }
          vibrator?.vibrate(
            android.os.VibrationEffect.createOneShot(step.number.coerceIn(1, 5_000).toLong(), android.os.VibrationEffect.DEFAULT_AMPLITUDE)
          )
          runStep(index + 1)
        }
        Macros.DoKind.PLAY_TONE -> {
          if (!OutputMute.areSoundsMuted()) playTone(step.number.coerceIn(20, 20_000))
          runStep(index + 1)
        }
        Macros.DoKind.DEVICE_CONTROL ->
          DeviceActions.set(service, step.name, text) { worked, why ->
            mainHandler.post { ifActive { if (worked) next() else fail(index, step, why ?: "not changed") } }
          }
        Macros.DoKind.MEDIA -> {
          if (!DeviceActions.media(service, step.text)) return fail(index, step, "no media key ${step.text}")
          next()
        }
        Macros.DoKind.SET_TIMER -> {
          if (!DeviceActions.setTimer(service, step.number, text)) return fail(index, step, "no clock app")
          next()
        }
        Macros.DoKind.SET_ALARM -> {
          if (!DeviceActions.setAlarm(service, step.number / 60, step.number % 60, text)) return fail(index, step, "no clock app")
          next()
        }
        Macros.DoKind.CALL -> {
          if (!DeviceActions.call(service, text)) return fail(index, step, "no phone app")
          next()
        }
        Macros.DoKind.SEND_MESSAGE -> {
          if (!DeviceActions.message(service, fill(step.name), text)) return fail(index, step, "no messaging app")
          next()
        }
        Macros.DoKind.OPEN_CAMERA -> {
          if (!DeviceActions.camera(service, step.text == "video")) return fail(index, step, "no camera app")
          next()
        }
        Macros.DoKind.COPY_TEXT -> {
          service.getSystemService(android.content.ClipboardManager::class.java)
            ?.setPrimaryClip(android.content.ClipData.newPlainText(macro.name, text))
          next()
        }
        Macros.DoKind.OPEN_LINK -> {
          val uri = android.net.Uri.parse(text.trim())
          if (uri.scheme?.lowercase() !in LINK_SCHEMES) return fail(index, step, "not a link: $text")
          try {
            service.startActivity(
              android.content.Intent(android.content.Intent.ACTION_VIEW, uri).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
          } catch (e: android.content.ActivityNotFoundException) {
            return fail(index, step, "no app opens $text")
          }
          next()
        }
        Macros.DoKind.SHARE_TEXT -> {
          val send =
            android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, text)
          service.startActivity(
            android.content.Intent.createChooser(send, null).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
          )
          next()
        }
        else -> {
          val device = Macros.deviceSteps() ?: return fail(index, step, "not in this build")
          device.run(service, step, text) { worked, value ->
            mainHandler.post {
              ifActive {
                if (!worked) {
                  fail(index, step, value ?: "failed")
                } else {
                  if (step.kind.setsValue) keep(step.name, value.orEmpty())
                  next()
                }
              }
            }
          }
        }
      }
    }

    private fun playTone(frequency: Int) {
      val rate = 22_050
      val samples = ShortArray(rate * TONE_MS / 1000) { i ->
        val fade = minOf(1.0, minOf(i, rate * TONE_MS / 1000 - i) / 200.0)
        (Math.sin(2 * Math.PI * frequency * i / rate) * 0.4 * fade * Short.MAX_VALUE).toInt().toShort()
      }
      val track =
        android.media.AudioTrack.Builder()
          .setAudioAttributes(
            android.media.AudioAttributes.Builder()
              .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
              .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()
          )
          .setAudioFormat(
            android.media.AudioFormat.Builder()
              .setSampleRate(rate)
              .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
              .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setTransferMode(android.media.AudioTrack.MODE_STATIC)
          .setBufferSizeInBytes(samples.size * 2)
          .build()
      track.write(samples, 0, samples.size)
      track.play()
      mainHandler.postDelayed({ track.release() }, TONE_MS + 200L)
    }

    // Whether a step failed or the condition wasn't met.
    private var failed = false

    private val flow = Macros.analyze(macro.steps)

    // How many times each repeat has run its steps since the macro reached it.
    private val repeats = IntArray(macro.steps.size)

    // The thread that looks for items and checks conditions, made when the run first needs it.
    private var searchThread: HandlerThread? = null
    @Volatile private var searchHandler: Handler? = null

    private val active: Boolean
      get() = !finished && current === this && TalkBackService.getInstance() === service

    fun later(delayMs: Long, block: () -> Unit) {
      mainHandler.postAtTime({ ifActive(block) }, this, SystemClock.uptimeMillis() + delayMs)
    }

    /** Runs [block] while the run is active, or else ends it, as when SandFly has been turned off. */
    private fun ifActive(block: () -> Unit) {
      if (active) block() else done()
    }

    /** Runs the steps when the macro's condition is met and its blocks match. */
    fun start(sayNotMet: Boolean) {
      flow.problem?.let {
        Log.w(TAG, "Macro not run: blocks don't match at step ${it.index + 1}")
        say(
          service.getString(
            R.string.macro_stopped_problem,
            it.index + 1,
            service.getString(it.message),
          )
        )
        failed = true
        done()
        return
      }
      val app = macro.app
      if (app == null) {
        startIfMet(sayNotMet)
        return
      }
      evaluate(Macros.Condition(listOf(Macros.Clause(Macros.AppInFront(app))))) { inApp ->
        if (inApp) {
          startIfMet(sayNotMet)
        } else {
          if (sayNotMet) say(service.getString(R.string.macro_not_in_app, Macros.appName(service, app)))
          failed = true
          done()
        }
      }
    }

    /** Runs the steps when the macro's condition is met. */
    private fun startIfMet(sayNotMet: Boolean) {
      evaluate(macro.condition) { met ->
        if (met) {
          runStep(0)
        } else {
          if (sayNotMet) say(service.getString(R.string.macro_not_met))
          failed = true
          done()
        }
      }
    }

    fun runStep(index: Int) {
      if (!active) return
      val step = macro.steps.getOrNull(index)
      if (step == null) {
        done()
        return
      }
      try {
        when (step) {
          is Macros.If -> evaluate(step.condition) { if (it) runStep(index + 1) else branch(flow.next[index]) }
          // The branch before it has run.
          is Macros.ElseIf, Macros.Else -> runStep(flow.end[index] + 1)
          Macros.EndIf -> runStep(index + 1)
          is Macros.RepeatTimes, is Macros.RepeatWhile -> {
            repeats[index] = 0
            repeatAgain(index)
          }
          Macros.EndRepeat -> later(REPEAT_GAP_MS) { repeatAgain(flow.start[index]) }
          Macros.ExitLoop -> runStep(flow.end[flow.start[index]] + 1)
          Macros.StopMacro -> done()
          is Macros.WaitUntil -> waitUntil(index, step, SystemClock.uptimeMillis() + step.timeoutMillis)
          is Macros.RunCommand -> {
            if (step.value == Macros.STOP_COMMAND) {
              done()
              return
            }
            // A macro started from a macro would stop it, and could start itself again and again.
            if (Macros.isCommand(step.value)) {
              fail(index, step, "macro run from a macro")
              return
            }
            if (!service.performCommand(step.value)) {
              fail(index, step, "command not run")
              return
            }
            later(STEP_GAP_MS) { runStep(index + 1) }
          }
          is Macros.Wait -> later(step.millis) { runStep(index + 1) }
          is Macros.OpenApp -> {
            if (!OpenAppActions.open(service, step.value)) {
              fail(index, step, "app not opened")
              return
            }
            later(STEP_GAP_MS) { runStep(index + 1) }
          }
          is Macros.TypeText ->
            typeText(index, step.copy(text = fill(step.text)), SystemClock.uptimeMillis() + FIELD_TIMEOUT_MS)
          is Macros.Tap -> findItem(index, step, step.label, step.regex, step.timeoutMillis)
          is Macros.TapPosition -> tapAt(index, step)
          is Macros.TouchHold -> findItem(index, step, step.label, step.regex, step.timeoutMillis)
          is Macros.FocusItem -> findItem(index, step, step.label, step.regex, step.timeoutMillis)
          is Macros.ActOnItem ->
            if (step.label.isEmpty()) findList(index, step) else findItem(index, step, step.label, step.regex, step.timeoutMillis)
          is Macros.SpeakItem -> findItem(index, step, step.label, step.regex, step.timeoutMillis)
          is Macros.SpeakText -> {
            speak(fill(step.text))
            runStep(index + 1)
          }
          is Macros.Do -> runDo(index, step)
        }
      } catch (e: RuntimeException) {
        fail(index, step, e.toString())
      }
    }

    /** Goes on at [index], an Else if, Else or End if reached when the branches before it didn't run. */
    private fun branch(index: Int) {
      when (val step = macro.steps[index]) {
        is Macros.ElseIf -> evaluate(step.condition) { if (it) runStep(index + 1) else branch(flow.next[index]) }
        else -> runStep(index + 1)
      }
    }

    /** Runs the steps of the repeat at [index] once more, or goes on after it. */
    private fun repeatAgain(index: Int) {
      val step = macro.steps[index]
      val again: (Boolean) -> Unit = { again ->
        when {
          !again -> runStep(flow.end[index] + 1)
          repeats[index] >= Macros.MAX_REPEATS -> {
            Log.w(TAG, "Macro stopped at step ${index + 1}: ${Macros.MAX_REPEATS} repeats")
            say(
              service.getString(
                R.string.macro_stopped_repeats,
                Macros.MAX_REPEATS,
                Macros.describe(service, step),
              )
            )
            failed = true
            done()
          }
          else -> {
            repeats[index]++
            runStep(index + 1)
          }
        }
      }
      when (step) {
        is Macros.RepeatTimes -> again(repeats[index] < step.count)
        is Macros.RepeatWhile -> evaluate(step.condition, again)
        else -> runStep(index + 1)
      }
    }

    private fun waitUntil(index: Int, step: Macros.WaitUntil, deadline: Long) {
      evaluate(step.condition) { met ->
        when {
          met -> runStep(index + 1)
          SystemClock.uptimeMillis() + POLL_MS <= deadline -> later(POLL_MS) { waitUntil(index, step, deadline) }
          else -> {
            Log.w(TAG, "Macro stopped at step ${index + 1}: Wait until timed out")
            say(service.getString(R.string.macro_stopped_timeout, Macros.describe(service, step)))
            failed = true
            done()
          }
        }
      }
    }

    /**
     * Checks [condition] on the search thread, and calls [then] on the main thread with whether it
     * is met, while the run is active. A test that can't read the windows isn't met.
     */
    private fun evaluate(condition: Macros.Condition, then: (Boolean) -> Unit) {
      if (condition.clauses.isEmpty()) {
        then(true)
        return
      }
      searchHandler().post {
        if (finished) return@post
        val met =
          try {
            condition.isMet { check(service, it, values) }
          } catch (e: RuntimeException) {
            LogUtils.w(TAG, "Cannot check a macro condition: %s", e)
            false
          }
        mainHandler.post { ifActive { then(met) } }
      }
    }

    private fun searchHandler(): Handler =
      searchHandler
        ?: Handler(HandlerThread(TAG).also { it.start(); searchThread = it }.looper).also {
          searchHandler = it
        }

    private fun typeText(index: Int, step: Macros.TypeText, deadline: Long) {
      val field =
        try {
          service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.takeIf { it.isEditable }
        } catch (e: RuntimeException) {
          LogUtils.w(TAG, "Cannot find the edit field: %s", e)
          null
        }
      if (field == null) {
        if (SystemClock.uptimeMillis() < deadline) {
          later(POLL_MS) { typeText(index, step, deadline) }
        } else {
          fail(index, step, "no edit field with input focus")
        }
        return
      }
      val typed =
        if (step.replace) {
          replaceText(field, step.text)
        } else {
          service.feedbackReturner?.returnFeedback(
            EVENT_ID_UNTRACKED,
            Feedback.edit(
                AccessibilityNodeInfoCompat.wrap(field),
                Feedback.EditText.Action.INSERT,
              )
              .setText(step.text)
              // The typed text is said, not the field's whole text.
              .setSpeakText(true),
          ) == true
        }
      if (!typed) {
        fail(index, step, "text not set")
        return
      }
      later(STEP_GAP_MS) { runStep(index + 1) }
    }

    private fun replaceText(field: AccessibilityNodeInfo, text: String): Boolean {
      val arguments =
        Bundle().apply {
          putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
      if (!field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) return false
      val cursor =
        Bundle().apply {
          putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, text.length)
          putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, text.length)
        }
      // The text is set even when the field can't move its cursor.
      field.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, cursor)
      return true
    }

    /** Taps the screen at the position of [step]. Fails when the position is outside the screen. */
    private fun tapAt(index: Int, step: Macros.TapPosition) {
      val size = ScreenUtils.getRealScreenSize(service)
      if (step.x !in 0 until size.x || step.y !in 0 until size.y) {
        fail(index, step, "position outside the screen of ${size.x} x ${size.y}")
        return
      }
      val x = step.x.toFloat()
      val y = step.y.toFloat()
      touch(index, step, listOf(TouchSegment(x, y, x, y, ViewConfiguration.getTapTimeout().toLong(), false)))
    }

    /**
     * Touches the centre of the part of [bounds], those of the item of [step], on the screen, holds,
     * slides when the step does, and lifts. The end of a slide is kept on the screen.
     */
    private fun touchHold(index: Int, step: Macros.TouchHold, bounds: Rect) {
      val size = ScreenUtils.getRealScreenSize(service)
      val visible = Rect(bounds)
      if (!visible.intersect(0, 0, size.x, size.y)) {
        fail(index, step, "item not on the screen", found = true)
        return
      }
      val x = visible.exactCenterX().coerceIn(0f, (size.x - 1).toFloat())
      val y = visible.exactCenterY().coerceIn(0f, (size.y - 1).toFloat())
      var toX = x
      var toY = y
      step.slide?.let {
        val distance = step.slideDp * service.resources.displayMetrics.density
        toX = (x + it.dx * distance).coerceIn(0f, (size.x - 1).toFloat())
        toY = (y + it.dy * distance).coerceIn(0f, (size.y - 1).toFloat())
      }
      touch(index, step, TouchSegment.plan(x, y, toX, toY, step.holdMillis))
    }

    /**
     * Performs [segments] as one touch through [PassThroughTouch]. Goes on to the next step when the
     * touch ends. When the run is stopped while the touch is down, the touch is lifted where it is.
     */
    private fun touch(index: Int, step: Macros.Step, segments: List<TouchSegment>) {
      if (!PassThroughTouch.isSupported()) {
        fail(index, step, "touch needs Android 11", found = true)
        return
      }
      if (service.feedbackReturner == null) {
        fail(index, step, "no feedback pipeline", found = true)
        return
      }
      if (PassThroughTouch.isLockedElsewhere(service)) {
        fail(index, step, "pass-through held by another feature", found = true)
        return
      }
      if (segments.isEmpty()) {
        runStep(index + 1)
        return
      }
      val started =
        PassThroughTouch.perform(service, segments, stopped = { !active }) { performed ->
          ifActive {
            if (performed) later(STEP_GAP_MS) { runStep(index + 1) }
            else fail(index, step, "touch not performed", found = true)
          }
        }
      if (!started) fail(index, step, "touch not performed", found = true)
    }

    /**
     * Runs the Tap item, Focus item or Speak item [step], whose item has [label], waiting up to
     * [timeoutMillis].
     */
    private fun findItem(index: Int, step: Macros.Step, label: String, regex: Boolean, timeoutMillis: Long) {
      val match = LabelMatch(label, regex)
      if (!match.isUsable) {
        fail(index, step, "regular expression not usable", found = true)
        return
      }
      val deadline = SystemClock.uptimeMillis() + timeoutMillis
      searchHandler().post { search(index, step, match, deadline) }
    }

    /** Runs the scroll [step] that has no item on the largest list on screen, waiting for one. */
    private fun findList(index: Int, step: Macros.ActOnItem) {
      val deadline = SystemClock.uptimeMillis() + step.timeoutMillis
      searchHandler().post { searchList(index, step, deadline) }
    }

    private fun searchList(index: Int, step: Macros.ActOnItem, deadline: Long) {
      if (finished) return
      val result =
        try {
          largestList(service, step.act.action)?.let { act(it, step) }
        } catch (e: RuntimeException) {
          LogUtils.w(TAG, "Cannot read the windows: %s", e)
          null
        }
      when {
        result == true -> mainHandler.post { ifActive { later(STEP_GAP_MS) { runStep(index + 1) } } }
        result == false -> mainHandler.post { ifActive { fail(index, step, "scroll not performed", found = true) } }
        SystemClock.uptimeMillis() + POLL_MS <= deadline ->
          searchHandler?.postDelayed({ searchList(index, step, deadline) }, POLL_MS)
        else -> mainHandler.post { ifActive { fail(index, step, "no list", found = true) } }
      }
    }

    /**
     * Looks for the item of [step], a Tap item, Focus item or Speak item step, by [label] and the
     * step's item type on the search thread until [deadline], and clicks it, moves accessibility
     * focus to it or speaks it.
     */
    private fun search(index: Int, step: Macros.Step, label: LabelMatch, deadline: Long) {
      if (finished) return
      val field =
        when (step) {
          is Macros.Tap -> step.field
          is Macros.FocusItem -> step.field
          is Macros.SpeakItem -> step.field
          is Macros.TouchHold -> step.field
          is Macros.ActOnItem -> step.field
          else -> null
        }
      val kind =
        when (step) {
          is Macros.Tap -> step.kind
          is Macros.FocusItem -> step.kind
          is Macros.SpeakItem -> step.kind
          is Macros.TouchHold -> step.kind
          is Macros.ActOnItem -> step.kind
          else -> null
        }
      // The item to focus or speak, the point to touch, whether the click was performed, or null
      // when the item isn't there.
      val result: Any? =
        try {
          val node = findItem(service, label, kind, field = field)
          // The run can be stopped while the windows are read.
          if (finished) return
          node?.let {
            when (step) {
              is Macros.FocusItem -> focusTarget(it)
              is Macros.ActOnItem -> act(it, step)
              is Macros.SpeakItem -> ItemToSpeak(AccessibilityNodeInfoCompat.wrap(it), innerText(it))
              is Macros.TouchHold -> {
                val bounds = Rect()
                it.getBoundsInScreen(bounds)
                // Only the part in the item's window, so that the touch doesn't land on a window
                // over it, such as the keyboard or the navigation bar.
                val windowBounds = Rect()
                it.window?.getBoundsInScreen(windowBounds)
                if (!windowBounds.isEmpty && !bounds.intersect(windowBounds)) bounds.setEmpty()
                if (bounds.isEmpty) NO_POSITION else bounds
              }
              else -> click(it)
            }
          }
        } catch (e: RuntimeException) {
          LogUtils.w(TAG, "Cannot read the windows: %s", e)
          null
        }
      when {
        result is AccessibilityNodeInfoCompat -> mainHandler.post { ifActive { focus(index, step, result) } }
        result is ItemToSpeak -> mainHandler.post { ifActive { speakItem(index, step, result) } }
        result is Rect && step is Macros.TouchHold ->
          mainHandler.post { ifActive { touchHold(index, step, result) } }
        result === NO_POSITION ->
          mainHandler.post { ifActive { fail(index, step, "item not on the screen", found = true) } }
        result == true -> mainHandler.post { ifActive { later(STEP_GAP_MS) { runStep(index + 1) } } }
        result == false -> mainHandler.post { ifActive { fail(index, step, "click not performed", found = true) } }
        SystemClock.uptimeMillis() + POLL_MS <= deadline ->
          searchHandler?.postDelayed({ search(index, step, label, deadline) }, POLL_MS)
        else -> mainHandler.post { ifActive { fail(index, step, "item not found") } }
      }
    }

    /** Moves accessibility focus to [node] through the pipeline, which speaks it, and goes on. */
    private fun focus(index: Int, step: Macros.Step, node: AccessibilityNodeInfoCompat) {
      val info = FocusActionInfo.builder().setSourceAction(FocusActionInfo.UNKNOWN).build()
      val moved =
        service.feedbackReturner?.returnFeedback(
          EVENT_ID_UNTRACKED,
          Feedback.focus(node, info).setForceRefocus(true),
        ) == true
      if (moved) {
        later(STEP_GAP_MS) { runStep(index + 1) }
      } else {
        fail(index, step, "focus not moved", found = true)
      }
    }

    /**
     * Speaks the name of the item [item] found for [step], as SandFly speaks it, without moving
     * accessibility focus, and goes on.
     */
    private fun speakItem(index: Int, step: Macros.Step, item: ItemToSpeak) {
      val name =
        try {
          itemName(item.node) ?: item.innerText
        } catch (e: RuntimeException) {
          LogUtils.w(TAG, "Cannot read the item: %s", e)
          null
        }
      if (name.isNullOrBlank()) {
        fail(index, step, "item has no text", found = true)
        return
      }
      speak(name.take(Macros.MAX_TEXT))
      runStep(index + 1)
    }

    /**
     * The name SandFly speaks for [node]: its label from a script, screen recognition or a custom
     * label, its description or text, or its hint. Null when it has none.
     */
    private fun itemName(node: AccessibilityNodeInfoCompat): CharSequence? {
      val labelManager =
        try {
          service.labelManager
        } catch (e: IllegalStateException) {
          null
        }
      return sequenceOf<() -> CharSequence?>(
          { Scripts.ruleLabel(node) },
          { ScreenElements.label(node) },
          { TalkBackLabelManager.getNodeText(node, labelManager) },
          { node.hintText },
        )
        .map { it() }
        .firstOrNull { !it.isNullOrBlank() }
    }

    /**
     * Says that [step], at [index], failed, or for a step that finds an item not [found], that its
     * item isn't there, and stops. Logs [reason] at warning level, which problem reports include by
     * default.
     */
    private fun fail(index: Int, step: Macros.Step, reason: String, found: Boolean = false) {
      Log.w(TAG, "Macro stopped at step ${index + 1} of ${macro.steps.size}: ${step::class.simpleName}, $reason")
      val itemLabel =
        when (step) {
          is Macros.Tap -> step.label
          is Macros.FocusItem -> step.label
          is Macros.SpeakItem -> step.label
          is Macros.TouchHold -> step.label
          is Macros.ActOnItem -> step.label
          else -> null
        }
      val text =
        if (itemLabel != null && !found) {
          service.getString(R.string.macro_item_not_found, itemLabel)
        } else {
          service.getString(R.string.macro_step_failed, Macros.describe(service, step))
        }
      say(text)
      failed = true
      done()
    }

    private fun say(text: String) {
      if (quiet) return
      service.feedbackReturner?.returnFeedback(EVENT_ID_UNTRACKED, Feedback.requestedSpeech(text))
    }

    /**
     * Speaks [text] for a Speak text or Speak item step, after the speech before it, such as that
     * of a Focus item step, also in a run that says nothing about failed steps.
     */
    private fun speak(text: CharSequence) {
      service.feedbackReturner?.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(text, speakStepOptions()))
    }

    private fun done() {
      val wasActive = !finished
      finish()
      if (current === this) current = null
      // A run that was stopped, as by another, says nothing.
      if (wasActive && TalkBackService.getInstance() === service) {
        onDone?.let { callback ->
          onDone = null
          callback(!failed)
        }
      }
    }

    fun finish() {
      finished = true
      mainHandler.removeCallbacksAndMessages(this)
      searchThread?.quitSafely()
      searchThread = null
      searchHandler = null
    }
  }

  /**
   * The item a Speak item step found, read on the search thread, with [innerText], the texts of the
   * items in it, for when it has no name of its own.
   */
  private class ItemToSpeak(val node: AccessibilityNodeInfoCompat, val innerText: String?)

  /** Options for the speech of Speak text and Speak item steps: queued, and heard during calls and media. */
  private fun speakStepOptions(): SpeakOptions =
    SpeakOptions.create()
      .setQueueMode(SpeechController.QUEUE_MODE_QUEUE)
      .setFlags(FeedbackItem.FLAG_FORCE_FEEDBACK_ALL)

  /**
   * Looks for the text of a macro step or test in the texts of items: as a text they contain,
   * ignoring case, or for [regex] as a regular expression found in them. A regular expression that
   * [Macros.compilePattern] refuses matches nothing.
   */
  private class LabelMatch(label: String, private val regex: Boolean) {
    private val target = label.trim()
    private val pattern = if (regex) Macros.compilePattern(target) else null

    val isEmpty: Boolean = target.isEmpty()

    /** Whether there is something to look for. */
    val isUsable: Boolean = !isEmpty && (!regex || pattern != null)

    /** Whether [text] is the text looked for. A regular expression is only ever found in a text. */
    fun isExactly(text: String): Boolean = !regex && text.equals(target, ignoreCase = true)

    fun isIn(text: CharSequence): Boolean =
      if (regex) pattern?.matcher(text.take(Macros.MAX_PATTERN_INPUT))?.find() == true
      else text.contains(target, ignoreCase = true)
  }

  /**
   * The item a step that finds items by [label] in [field], a regular expression for [regex],
   * finds on screen now, or null. For [MacroRecorder], which checks that it finds the item pressed.
   */
  internal fun firstItem(service: TalkBackService, label: String, regex: Boolean, field: Macros.Field?): AccessibilityNodeInfo? =
    findItem(service, LabelMatch(label, regex), field = field)

  /**
   * The item whose text or description is [label], ignoring case, or else the first one whose text
   * or description contains it or, for a regular expression, matches it, in the active window
   * first, then from the top window down. SandFly's own overlays are left out.
   */
  private fun findItem(
    service: TalkBackService,
    label: LabelMatch,
    kind: Macros.Kind? = null,
    anyMatch: Boolean = false,
    field: Macros.Field? = null,
  ): AccessibilityNodeInfo? {
    if (!label.isUsable) return null
    val roots =
      service.windows
        .filter { it.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY }
        .sortedWith(compareByDescending<AccessibilityWindowInfo> { it.isActive }.thenByDescending { it.layer })
        .mapNotNull { it.root }
    return findItemIn(roots, label, kind, anyMatch, field)
  }

  /**
   * The item a step that finds items by [label] in [field], a regular expression for [regex], of
   * [kind] when it isn't null, finds in the windows of [roots], or null. For tests.
   */
  internal fun stepItemIn(
    roots: List<AccessibilityNodeInfo>,
    label: String,
    regex: Boolean = false,
    field: Macros.Field? = null,
    kind: Macros.Kind? = null,
  ): AccessibilityNodeInfo? = findItemIn(roots, LabelMatch(label, regex), kind, anyMatch = false, field = field)

  /** [findItem] in the windows of [roots], in that order. */
  private fun findItemIn(
    roots: List<AccessibilityNodeInfo>,
    label: LabelMatch,
    kind: Macros.Kind?,
    anyMatch: Boolean,
    field: Macros.Field?,
  ): AccessibilityNodeInfo? {
    if (!label.isUsable) return null
    var partial: AccessibilityNodeInfo? = null
    var count = 0
    for (root in roots) {
      val queue = ArrayDeque<AccessibilityNodeInfo>()
      queue.add(root)
      while (queue.isNotEmpty() && count < MAX_NODES) {
        val node = queue.removeFirst()
        count++
        if (node.isVisibleToUser) {
          val texts =
            when {
              field != null -> texts(node, field)
              // A step looks in the same texts with an item type as without one.
              !anyMatch -> listOfNotNull(node.text, node.contentDescription, AccessibilityNodeInfoCompat.wrap(node).tooltipText)
              else -> texts(node)
            }
          var exact = false
          var contains = false
          for (text in texts) {
            val trimmed = text.toString().trim()
            if (label.isExactly(trimmed)) {
              exact = true
              break
            }
            if (!contains && label.isIn(trimmed)) contains = true
          }
          // The kind is checked only for an item with the text, as it can read the items around it.
          if ((exact || (contains && (anyMatch || partial == null))) && (kind == null || isKind(node, kind))) {
            if (exact || anyMatch) return node
            partial = node
          }
        }
        for (i in 0 until node.childCount) {
          node.getChild(i)?.let { queue.add(it) }
        }
      }
    }
    return partial
  }

  /**
   * Returns the [macros] listed in the Actions menu of [item], an item of their app, that are shown
   * there now: those whose [Macros.Macro.actionsCondition] is met. A test that fails to run counts as
   * met, so the macro stays listed. The result for the same item and macros is reused for
   * [SHOWN_IN_ACTIONS_REUSE_MS]. The time the tests take in other apps is noted for problem reports.
   */
  @JvmStatic
  fun shownInActions(service: TalkBackService, item: Any?, macros: List<Macros.Macro>): List<Macros.Macro> {
    val timing = ConditionTiming()
    val shown =
      shownInActionsCache.get(item, macros, SystemClock.uptimeMillis()) { test ->
        timing.time(test) {
          try {
            check(service, test, emptyMap())
          } catch (e: RuntimeException) {
            LogUtils.w(TAG, "Actions condition of a macro not checked: %s", e)
            true
          }
        }
      }
    val app = (item as? AccessibilityNodeInfoCompat)?.packageName?.toString()
    if (!timing.isEmpty && app != service.packageName) {
      val note =
        "Actions conditions of ${macros.count { it.actionsCondition.clauses.isNotEmpty() }} macros " +
          "in $app: ${timing.describe()}"
      LogUtils.d(TAG, "%s", note)
      ReportNotes.macroConditions.add(note)
    }
    return shown
  }

  /** Returns whether the macro condition test [test] is met. Called on the search thread. */
  private fun check(service: TalkBackService, test: Macros.Test, values: Map<String, String>): Boolean =
    when (test) {
      is Macros.ValueIs -> {
        val value = values[test.name].orEmpty()
        if (test.label.isEmpty()) value.isNotEmpty() else LabelMatch(test.label, test.regex).isIn(value)
      }
      is Macros.ItemOnScreen ->
        findItem(service, LabelMatch(test.label, test.regex), test.kind, anyMatch = true, field = test.field) != null
      is Macros.FocusedItem -> {
        val focused = service.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
        val label = LabelMatch(test.label, test.regex)
        focused != null &&
          (test.kind == null || isKind(focused, test.kind)) &&
          (label.isEmpty || containsText(focused, label, test.field))
      }
      is Macros.FocusInside -> {
        val label = LabelMatch(test.label, test.regex)
        generateSequence(service.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)) { it.parent }
          .take(MAX_CLICK_PARENTS * 5)
          .any { node -> (if (test.field != null) texts(node, test.field) else texts(node)).any { label.isIn(it) } }
      }
      is Macros.AppInFront -> activePackage(service) == test.packageName
      Macros.KeyboardShown -> service.windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
      Macros.ScreenLocked -> service.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true
    }

  /** The package of the app in the active window, or of the top app window when none is active. */
  private fun activePackage(service: TalkBackService): CharSequence? {
    val windows = service.windows
    val window =
      windows.firstOrNull { it.isActive && it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
        ?: windows
          .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
          .maxByOrNull { it.layer }
    return window?.root?.packageName
  }

  /**
   * The [field] of [node], or nothing when it has none. A resource ID is also given without its
   * package, so that "send" is the whole of com.example:id/send.
   */
  private fun texts(node: AccessibilityNodeInfo, field: Macros.Field): List<CharSequence> =
    when (field) {
      Macros.Field.TEXT -> listOfNotNull(node.text)
      Macros.Field.DESCRIPTION -> listOfNotNull(node.contentDescription)
      Macros.Field.HINT -> listOfNotNull(node.hintText)
      Macros.Field.STATE -> listOfNotNull(AccessibilityNodeInfoCompat.wrap(node).stateDescription)
      Macros.Field.TOOLTIP -> listOfNotNull(AccessibilityNodeInfoCompat.wrap(node).tooltipText)
      Macros.Field.RESOURCE_ID ->
        node.viewIdResourceName?.let { id -> listOf(id, id.substringAfter(":id/")).distinct() }.orEmpty()
    }

  private fun texts(node: AccessibilityNodeInfo): List<CharSequence> =
    listOfNotNull(
      node.text,
      node.contentDescription,
      node.hintText,
      AccessibilityNodeInfoCompat.wrap(node).stateDescription,
      AccessibilityNodeInfoCompat.wrap(node).tooltipText,
    )

  /**
   * Returns whether the texts of [node] or of the items in it, or only their [field] when that
   * isn't null, have [label] in them.
   */
  private fun containsText(node: AccessibilityNodeInfo, label: LabelMatch, field: Macros.Field?): Boolean {
    val queue = ArrayDeque<AccessibilityNodeInfo>()
    queue.add(node)
    var count = 0
    while (queue.isNotEmpty() && count < MAX_FOCUS_NODES) {
      val item = queue.removeFirst()
      count++
      val texts = if (field != null) texts(item, field) else texts(item)
      if (texts.any { label.isIn(it) }) return true
      for (i in 0 until item.childCount) {
        item.getChild(i)?.let { queue.add(it) }
      }
    }
    return false
  }

  /**
   * Returns whether [node] is of [kind]. For Clickable and Long-clickable, an item above it within
   * a few levels counts too, as the text of a button is often an item in it. Inspect focused item
   * lists the kinds of an item with this.
   */
  internal fun isKind(node: AccessibilityNodeInfo, kind: Macros.Kind): Boolean =
    when (kind) {
      Macros.Kind.CLICKABLE -> nodeOrParent(node) { it.isClickable }
      Macros.Kind.LONG_CLICKABLE -> nodeOrParent(node) { it.isLongClickable }
      Macros.Kind.EDITABLE -> node.isEditable
      Macros.Kind.SCROLLABLE ->
        node.isScrollable ||
          node.actionList.any {
            it.id == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD ||
              it.id == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
          }
      Macros.Kind.ADJUSTABLE ->
        node.rangeInfo != null ||
          node.actionList.any { it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id }
      Macros.Kind.CHECKABLE -> node.isCheckable
      @Suppress("DEPRECATION") Macros.Kind.CHECKED -> nodeOrParent(node) { it.isChecked } || checkedInRow(node)
      Macros.Kind.SELECTED -> node.isSelected
      Macros.Kind.HEADING -> AccessibilityNodeInfoCompat.wrap(node).isHeading
      Macros.Kind.ENABLED -> node.isEnabled
    }

  /**
   * Whether the nearest clickable item around [node], such as a settings row, has a switch or check
   * box in it that is checked.
   */
  @Suppress("DEPRECATION")
  private fun checkedInRow(node: AccessibilityNodeInfo): Boolean {
    var row: AccessibilityNodeInfo? = node
    repeat(MAX_CLICK_PARENTS + 1) {
      val item = row ?: return false
      if (item.isClickable) {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(item)
        var count = 0
        while (queue.isNotEmpty() && count < MAX_ROW_NODES) {
          val inner = queue.removeFirst()
          count++
          if (inner.isCheckable && inner.isChecked) return true
          for (i in 0 until inner.childCount) inner.getChild(i)?.let { queue.add(it) }
        }
        return false
      }
      row = item.parent
    }
    return false
  }

  /**
   * The largest visible item that can do [action], a scroll, in the active window, or else in the
   * top window that has one. SandFly's own overlays and the keyboard are left out.
   */
  private fun largestList(service: TalkBackService, action: Int): AccessibilityNodeInfo? {
    val roots =
      service.windows
        .filter { it.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY && it.type != AccessibilityWindowInfo.TYPE_INPUT_METHOD }
        .sortedWith(compareByDescending<AccessibilityWindowInfo> { it.isActive }.thenByDescending { it.layer })
        .mapNotNull { it.root }
    var count = 0
    val bounds = Rect()
    for (root in roots) {
      var best: AccessibilityNodeInfo? = null
      var bestArea = 0L
      val queue = ArrayDeque<AccessibilityNodeInfo>()
      queue.add(root)
      while (queue.isNotEmpty() && count < MAX_NODES) {
        val node = queue.removeFirst()
        count++
        if (node.isVisibleToUser && node.actionList.any { it.id == action }) {
          node.getBoundsInScreen(bounds)
          val area = bounds.width().toLong() * bounds.height()
          if (best == null || area > bestArea) {
            best = node
            bestArea = area
          }
        }
        for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
      }
      if (best != null) return best
    }
    return null
  }

  private fun nodeOrParent(node: AccessibilityNodeInfo, test: (AccessibilityNodeInfo) -> Boolean): Boolean {
    var candidate: AccessibilityNodeInfo? = node
    repeat(MAX_CLICK_PARENTS + 1) {
      val item = candidate ?: return false
      if (test(item)) return true
      candidate = item.parent
    }
    return false
  }

  /**
   * The descriptions or texts of the items in [node], in order, joined with commas, when [node] has
   * neither itself, or null. Called on the search thread.
   */
  private fun innerText(node: AccessibilityNodeInfo): String? {
    if (node.contentDescription?.isNotBlank() == true || node.text?.isNotBlank() == true) return null
    val texts = mutableListOf<String>()
    val stack = ArrayDeque<AccessibilityNodeInfo>()
    for (i in node.childCount - 1 downTo 0) node.getChild(i)?.let { stack.addLast(it) }
    var count = 0
    var length = 0
    while (stack.isNotEmpty() && count < MAX_FOCUS_NODES && length < Macros.MAX_TEXT) {
      val item = stack.removeLast()
      count++
      if (!item.isVisibleToUser) continue
      val text = AccessibilityNodeInfoUtils.getNodeText(AccessibilityNodeInfoCompat.wrap(item))?.toString()?.trim()
      if (!text.isNullOrEmpty()) {
        texts.add(text)
        length += text.length
        // An item's description stands for the items in it.
        if (!item.contentDescription.isNullOrBlank()) continue
      }
      for (i in item.childCount - 1 downTo 0) item.getChild(i)?.let { stack.addLast(it) }
    }
    return texts.joinToString(", ").takeIf { it.isNotEmpty() }
  }

  /** Found by a Touch and hold item step: an item with no part on the screen. */
  private val NO_POSITION = Any()

  /**
   * [node], or the nearest item above it that SandFly moves accessibility focus to, or [node] when
   * there is none. Called on the search thread.
   */
  private fun focusTarget(node: AccessibilityNodeInfo): AccessibilityNodeInfoCompat {
    val compat = AccessibilityNodeInfoCompat.wrap(node)
    return AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(
      compat,
      AccessibilityNodeInfoUtils.FILTER_SHOULD_FOCUS,
    ) ?: compat
  }

  /** Clicks [node], or the nearest item above it that can be clicked. Returns whether it did. */
  /**
   * Long-presses or scrolls [node], or the nearest item around it that can be, [ActOnItem.times]
   * times.
   */
  private fun act(node: AccessibilityNodeInfo, step: Macros.ActOnItem): Boolean {
    var candidate: AccessibilityNodeInfo? = node
    repeat(MAX_CLICK_PARENTS + 1) {
      val item = candidate ?: return@repeat
      if (item.actionList.any { it.id == step.act.action }) {
        return (1..step.times).map { item.performAction(step.act.action) }.all { it }
      }
      candidate = item.parent
    }
    return node.performAction(step.act.action)
  }

  private fun click(node: AccessibilityNodeInfo): Boolean {
    var candidate: AccessibilityNodeInfo? = node
    repeat(MAX_CLICK_PARENTS + 1) {
      val item = candidate ?: return@repeat
      if (item.isClickable && item.isEnabled) {
        return item.performAction(AccessibilityNodeInfo.ACTION_CLICK).also {
          if (!it) Log.w(TAG, "Click returned false: ${item.className} in ${item.packageName}")
        }
      }
      candidate = item.parent
    }
    return node.performAction(AccessibilityNodeInfo.ACTION_CLICK).also {
      if (!it) Log.w(TAG, "No clickable item: ${node.className} in ${node.packageName}")
    }
  }
}

/**
 * A part of a touch that a macro step performs: from one point to another, or staying at one, for
 * [durationMs]. With [willContinue], the touch stays down for the next part.
 */
internal data class TouchSegment(
  val fromX: Float,
  val fromY: Float,
  val toX: Float,
  val toY: Float,
  val durationMs: Long,
  val willContinue: Boolean,
) {
  val moves: Boolean
    get() = fromX != toX || fromY != toY

  /** A short part, where this one ends, that lifts the touch. */
  fun lift(): TouchSegment = TouchSegment(toX, toY, toX, toY, LIFT_MS, false)

  companion object {
    /**
     * The longest part of a hold. A macro stopped while a touch is held lifts it when the part ends,
     * and a gesture can't be longer than a minute.
     */
    const val HOLD_PART_MS = 1_000L

    /** How long a slide takes. */
    const val SLIDE_MS = 300L

    const val LIFT_MS = 10L

    /**
     * The parts of a touch at [x], [y] held for [holdMs], then slid to [toX], [toY] when that is
     * another point, and lifted.
     */
    fun plan(x: Float, y: Float, toX: Float, toY: Float, holdMs: Long): List<TouchSegment> {
      val slides = x != toX || y != toY
      val parts = ArrayList<TouchSegment>()
      var left = holdMs.coerceAtLeast(1L)
      while (left > 0) {
        val part = minOf(left, HOLD_PART_MS)
        left -= part
        parts += TouchSegment(x, y, x, y, part, left > 0 || slides)
      }
      if (slides) parts += TouchSegment(x, y, toX, toY, SLIDE_MS, false)
      return parts
    }
  }
}
