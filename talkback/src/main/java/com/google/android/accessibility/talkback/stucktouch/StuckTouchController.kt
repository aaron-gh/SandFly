/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.talkback.stucktouch

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.Performance
import com.google.android.accessibility.utils.output.FeedbackItem.FLAG_FORCE_FEEDBACK_ALL
import com.google.android.accessibility.utils.output.FeedbackItem.FLAG_NO_HISTORY
import com.google.android.accessibility.utils.output.SpeechController.QUEUE_MODE_QUEUE
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Notices when touch exploration has stopped because of an Android bug, and fixes it.
 *
 * After some touches with many fingers, Android's touch exploration can leave pointers it injected
 * down. The input dispatcher then rejects every hover event touch exploration injects, for every
 * screen reader, until the phone restarts. Swipes and taps still work, but dragging a finger says
 * nothing.
 *
 * A touch that Android reports as touch exploration, that lasts at least [MIN_EXPLORE_MS] and
 * hovers over nothing, may be the bug. With the DUMP permission, granted with ADB, Backtalk then
 * reads the pointers the dispatcher holds from `dumpsys input`. If the same ones are held twice in
 * a row, it turns explore by touch off, lifts them with [StuckPointerGesture], turns explore by
 * touch back on, and reads them again to check. Without the permission, or on phones whose
 * `dumpsys input` doesn't show those pointers, Backtalk can't tell for sure or fix it, so after
 * [MISSES_TO_ANNOUNCE] such touches in a row it says that touch exploration may be stuck.
 *
 * Everything but running `dumpsys input` happens on the main thread.
 */
class StuckTouchController(
  private val service: AccessibilityService,
  private val prefs: SharedPreferences,
  private val feedback: Pipeline.FeedbackReturner,
  private val touchExploration: TouchExploration,
) {

  /** Explore by touch, as Backtalk turns it on and off. */
  interface TouchExploration {
    /** Whether explore by touch is on. */
    fun isOn(): Boolean

    /** Turns explore by touch off. */
    fun turnOff()

    /** Turns explore by touch on again, if the settings and state allow it. */
    fun restore()
  }

  private enum class Phase {
    /** Watching touches. */
    IDLE,
    /** Reading the held pointers to see whether they are stuck. */
    CHECKING,
    /** Explore by touch is off and the gesture that lifts the pointers runs. */
    LIFTING,
    /** Explore by touch is back on, and the held pointers are read again. */
    VERIFYING,
  }

  private val handler = Handler(Looper.getMainLooper())
  private var executor: ExecutorService? = null
  private val key = service.getString(R.string.pref_fix_stuck_touch_exploration_key)
  private val default = service.resources.getBoolean(R.bool.pref_fix_stuck_touch_exploration_default)

  @Volatile private var stopped = false
  private var phase = Phase.IDLE

  // The current touch.
  private var touching = false
  private var touchStart = 0L
  private var explored = false
  private var hovered = false

  private var misses = 0
  private var lastCheck = 0L
  private var lastWarning = 0L
  private var retryAfter = 0L
  // Whether this phone's dumpsys input has shown the held pointers.
  private var dumpShowsPointers = false

  // Stuck pointers found while a touch was in progress, lifted when it ends.
  private var pendingDisplay = 0
  private var pendingPointers: Set<Int>? = null

  // The lift in progress.
  private var liftDisplay = 0
  private var liftPointers: Set<Int> = emptySet()
  private var liftAttempt = 0
  private var liftCompleted = false
  private val dispatch = Runnable { dispatchGesture() }
  private val giveUp = Runnable {
    LogUtils.w(TAG, "No result for the gesture")
    finishLift(completed = false)
  }

  // Ends a dumpsys input that takes too long, from the main thread.
  @Volatile private var dumpProcess: Process? = null
  private val killDump = Runnable { dumpProcess?.destroyForcibly() }

  fun onAccessibilityEvent(event: AccessibilityEvent) {
    when (event.eventType) {
      AccessibilityEvent.TYPE_TOUCH_INTERACTION_START -> {
        touching = true
        touchStart = SystemClock.uptimeMillis()
        explored = false
        hovered = false
      }
      AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START -> explored = true
      AccessibilityEvent.TYPE_VIEW_HOVER_ENTER -> {
        hovered = true
        misses = 0
      }
      AccessibilityEvent.TYPE_TOUCH_INTERACTION_END -> {
        touching = false
        onTouchEnd()
      }
    }
  }

  fun shutdown() {
    stopped = true
    // Gesture results arrive as new messages, so the phase must stop them too.
    phase = Phase.IDLE
    handler.removeCallbacksAndMessages(null)
    // Explore by touch stays as it is: the service is going away, and turns it off itself.
    dumpProcess?.destroyForcibly()
    executor?.shutdownNow()
    executor = null
  }

  private fun onTouchEnd() {
    if (stopped) {
      return
    }
    val pending = pendingPointers
    if (pending != null) {
      pendingPointers = null
      // A touch that hovered over something shows that touch exploration works again.
      if (!hovered) {
        startLift(pendingDisplay, pending)
      }
      return
    }
    val now = SystemClock.uptimeMillis()
    if (
      phase != Phase.IDLE ||
        !explored ||
        hovered ||
        now - touchStart < MIN_EXPLORE_MS ||
        now < retryAfter ||
        !prefs.getBoolean(key, default) ||
        !touchExploration.isOn()
    ) {
      return
    }
    if (!hasDumpPermission()) {
      onMiss(dumpGranted = false)
      return
    }
    if (now - lastCheck < MIN_CHECK_INTERVAL_MS) {
      return
    }
    lastCheck = now
    phase = Phase.CHECKING
    background().execute {
      var first: Map<Int, Set<Int>>? = null
      var second: Map<Int, Set<Int>>? = null
      try {
        first = readStuckPointers()
        second =
          if (first.isNullOrEmpty()) {
            first
          } else {
            // A pointer an app injects through the shell is also device -1, and is held only
            // while that touch lasts. Pointers that this bug leaves down stay.
            SystemClock.sleep(CONFIRM_DELAY_MS)
            readStuckPointers()
          }
      } catch (e: RuntimeException) {
        LogUtils.e(TAG, "Checking for stuck pointers failed: %s", e)
      } finally {
        // Always return to the main thread, so that the phase goes back to idle.
        post { onChecked(first, second) }
      }
    }
  }

  private fun onChecked(first: Map<Int, Set<Int>>?, second: Map<Int, Set<Int>>?) {
    if (phase != Phase.CHECKING) {
      return
    }
    phase = Phase.IDLE
    if (first == null || second == null) {
      // A phone whose dumpsys input never shows the pointers can only warn. A failed read on a
      // phone that does show them says nothing either way.
      if (!dumpShowsPointers) {
        onMiss(dumpGranted = true)
      }
      return
    }
    dumpShowsPointers = true
    val display = second.keys.firstOrNull { first[it] == second[it] } ?: return
    val pointers = second.getValue(display)
    if (touching) {
      // Lifting injects a gesture, which would cancel the touch in progress.
      pendingDisplay = display
      pendingPointers = pointers
    } else {
      startLift(display, pointers)
    }
  }

  /** Counts a touch that hovered over nothing, and warns after [MISSES_TO_ANNOUNCE] in a row. */
  private fun onMiss(dumpGranted: Boolean) {
    misses++
    if (misses < MISSES_TO_ANNOUNCE) {
      return
    }
    misses = 0
    val now = SystemClock.uptimeMillis()
    if (lastWarning != 0L && now - lastWarning < WARNING_INTERVAL_MS) {
      return
    }
    lastWarning = now
    LogUtils.w(TAG, "Touch exploration may be stuck")
    speak(
      if (dumpGranted) R.string.stuck_touch_exploration_restart
      else R.string.stuck_touch_exploration_grant_dump
    )
  }

  private fun startLift(display: Int, pointers: Set<Int>) {
    if (phase != Phase.IDLE || !prefs.getBoolean(key, default) || !touchExploration.isOn()) {
      return
    }
    LogUtils.w(TAG, "Injected pointers %s held down on display %d", pointers, display)
    if (StuckPointerGesture.build(pointers, display) == null) {
      // A gesture can't reach these pointer ids.
      retryAfter = SystemClock.uptimeMillis() + WARNING_INTERVAL_MS
      speak(R.string.stuck_touch_exploration_restart)
      return
    }
    phase = Phase.LIFTING
    liftDisplay = display
    liftPointers = pointers
    liftAttempt = 0
    speak(R.string.stuck_touch_exploration_fixing)
    touchExploration.turnOff()
    // Turning explore by touch off rebuilds Android's input filter, and a gesture sent before that
    // goes to the old one and is cancelled.
    handler.postDelayed(dispatch, FILTER_SETTLE_MS)
    handler.postDelayed(giveUp, GESTURE_TIMEOUT_MS)
  }

  private fun dispatchGesture() {
    if (phase != Phase.LIFTING) {
      return
    }
    val attempt = ++liftAttempt
    val gesture = StuckPointerGesture.build(liftPointers, liftDisplay)
    val dispatched =
      gesture != null &&
        service.dispatchGesture(
          gesture,
          object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
              if (phase == Phase.LIFTING && attempt == liftAttempt) {
                finishLift(completed = true)
              }
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
              if (phase != Phase.LIFTING || attempt != liftAttempt) {
                return
              }
              if (liftAttempt < MAX_DISPATCH_ATTEMPTS) {
                handler.postDelayed(dispatch, FILTER_SETTLE_MS)
              } else {
                finishLift(completed = false)
              }
            }
          },
          handler,
        )
    if (!dispatched) {
      finishLift(completed = false)
    }
  }

  /** Turns explore by touch back on and checks the held pointers again. */
  private fun finishLift(completed: Boolean) {
    if (phase != Phase.LIFTING) {
      return
    }
    phase = Phase.VERIFYING
    liftCompleted = completed
    handler.removeCallbacks(dispatch)
    handler.removeCallbacks(giveUp)
    touchExploration.restore()
    val display = liftDisplay
    background().execute {
      var left: Set<Int>? = null
      try {
        left = readStuckPointers()?.let { it[display].orEmpty() }
      } catch (e: RuntimeException) {
        LogUtils.e(TAG, "Checking for stuck pointers failed: %s", e)
      } finally {
        post { onVerified(left) }
      }
    }
  }

  private fun onVerified(left: Set<Int>?) {
    if (phase != Phase.VERIFYING) {
      return
    }
    phase = Phase.IDLE
    if (left == null) {
      // The next exploring touch checks again.
      LogUtils.w(TAG, "Couldn't read the held pointers after the gesture")
    } else if (left.isEmpty()) {
      LogUtils.w(TAG, "Lifted the stuck pointers")
      speak(R.string.stuck_touch_exploration_fixed)
    } else if (!liftCompleted) {
      // A touch or a slow filter rebuild cancelled the gesture. The next exploring touch tries
      // again.
      LogUtils.w(TAG, "Gesture cancelled, pointers %s still held down", left)
    } else {
      LogUtils.w(TAG, "Pointers %s still held down", left)
      retryAfter = SystemClock.uptimeMillis() + WARNING_INTERVAL_MS
      speak(R.string.stuck_touch_exploration_not_fixed)
    }
  }

  /** Runs [action] on the main thread, unless Backtalk has shut down by then. */
  private fun post(action: () -> Unit) {
    handler.post {
      if (!stopped) {
        action()
      }
    }
  }

  private fun hasDumpPermission() =
    service.checkSelfPermission(Manifest.permission.DUMP) == PackageManager.PERMISSION_GRANTED

  private fun background(): ExecutorService =
    executor ?: Executors.newSingleThreadExecutor { Thread(it, TAG) }.also { executor = it }

  /** Runs `dumpsys input`. Returns null if it fails or doesn't show the pointers. */
  private fun readStuckPointers(): Map<Int, Set<Int>>? {
    val start = SystemClock.uptimeMillis()
    val result = runDumpsys()
    LogUtils.d(TAG, "dumpsys input: %s in %d ms", result, SystemClock.uptimeMillis() - start)
    return result
  }

  private fun runDumpsys(): Map<Int, Set<Int>>? {
    if (stopped) {
      return null
    }
    val process =
      try {
        ProcessBuilder("dumpsys", "input").redirectErrorStream(true).start()
      } catch (e: IOException) {
        LogUtils.w(TAG, "dumpsys input failed: %s", e)
        return null
      }
    dumpProcess = process
    // Reading a pipe can't be interrupted, so a dumpsys that hangs is killed instead, which ends
    // the read.
    handler.postDelayed(killDump, DUMP_TIMEOUT_MS)
    return try {
      process.inputStream.bufferedReader().useLines { lines ->
        InjectedPointers.parse(lines.take(MAX_DUMP_LINES))
      }
    } catch (e: IOException) {
      LogUtils.w(TAG, "dumpsys input failed: %s", e)
      null
    } finally {
      handler.removeCallbacks(killDump)
      dumpProcess = null
      process.destroyForcibly()
    }
  }

  private fun speak(text: Int) {
    val options =
      SpeakOptions.create()
        .setQueueMode(QUEUE_MODE_QUEUE)
        .setFlags(FLAG_NO_HISTORY or FLAG_FORCE_FEEDBACK_ALL)
    feedback.returnFeedback(
      Performance.EVENT_ID_UNTRACKED,
      Feedback.speech(service.getString(text), options),
    )
  }

  companion object {
    private const val TAG = "StuckTouch"

    /** An exploring touch at least this long, with no hover, may be the bug. */
    const val MIN_EXPLORE_MS = 300L

    /** Exploring touches in a row that hover over nothing before Backtalk warns, when it can't check. */
    const val MISSES_TO_ANNOUNCE = 3

    private const val MIN_CHECK_INTERVAL_MS = 2_000L
    private const val CONFIRM_DELAY_MS = 300L
    private const val WARNING_INTERVAL_MS = 10 * 60_000L
    private const val FILTER_SETTLE_MS = 150L
    private const val GESTURE_TIMEOUT_MS = 3_000L
    private const val MAX_DISPATCH_ATTEMPTS = 2
    private const val MAX_DUMP_LINES = 20_000
    private const val DUMP_TIMEOUT_MS = 3_000L
  }
}
