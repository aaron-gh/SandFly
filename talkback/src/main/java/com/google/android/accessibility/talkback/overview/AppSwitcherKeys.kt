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

package com.google.android.accessibility.talkback.overview

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.shizuku.ShizukuShell
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechController
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED

/**
 * Alt+Tab with SandFly's overview: while Alt is held, Tab moves to the next recent task and
 * Shift+Tab to the one before, and SandFly says each task's name; releasing Alt switches to the task
 * said. The first Tab moves to the task before the current one. Escape leaves the current task in
 * front. No window is shown, so focus doesn't move until the switch.
 *
 * It takes Alt+Tab only while SandFly's overview is chosen, Shizuku allows SandFly, and Alt isn't
 * the SandFly key.
 */
class AppSwitcherKeys(
  private val context: Context,
  private val pipeline: Pipeline.FeedbackReturner,
  /** Whether Alt is the SandFly key, whose shortcuts Alt+Tab may be one of. */
  private val altIsSandFlyKey: () -> Boolean,
) {
  private val handler = Handler(Looper.getMainLooper())

  /** Whether Alt+Tab has been pressed and Alt is still held. */
  private var switching = false
  /** The tasks being moved through, once read. */
  private var tasks: List<RecentTask>? = null
  /** Tab presses before the tasks were read: positive forward, negative back. */
  private var steps = 0
  private var index = 0
  /** The position of the task in front, or -1 when none of the tasks is on screen. */
  private var frontIndex = 0
  /** Packages of the app windows on screen when Alt+Tab started. */
  private var front = emptyList<String>()
  /** Whether Alt was released before the tasks were read, so the switch waits for them. */
  private var switchWhenRead = false
  /** Counts Alt+Tab sequences, so that a late read for an earlier one is ignored. */
  private var sequence = 0
  /** Whether the shell service is held for a quick read after Alt is pressed. */
  private var holding = false

  private val releaseShell = Runnable {
    if (holding) {
      holding = false
      ShizukuShell.release()
    }
  }

  /** Takes the key if it is part of Alt+Tab. Runs on the main thread. */
  fun onKeyEvent(event: KeyEvent): Boolean {
    val keyCode = event.keyCode
    val down = event.action == KeyEvent.ACTION_DOWN
    if (switching && !event.isAltPressed && !isAlt(keyCode)) {
      // Alt was released without SandFly seeing it, such as when the keyboard disconnected.
      finish()
    }
    if (switching && switchWhenRead) {
      // Alt is up, and the switch waits only for the tasks to be read.
      return false
    }
    if (switching) {
      when {
        isAlt(keyCode) -> {
          if (!down) {
            finish()
          }
          // Alt reaches the app, which saw it pressed.
          return false
        }
        keyCode == KeyEvent.KEYCODE_TAB -> {
          if (down) move(if (event.isShiftPressed) -1 else 1)
          return true
        }
        keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_DOWN -> {
          if (down) move(1)
          return true
        }
        keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_UP -> {
          if (down) move(-1)
          return true
        }
        keyCode == KeyEvent.KEYCODE_ESCAPE -> {
          if (down) cancel()
          return true
        }
        KeyEvent.isModifierKey(keyCode) -> return false
        // Other keys do nothing while Alt+Tab is in use.
        else -> return true
      }
    }
    if (isAlt(keyCode) && down && event.repeatCount == 0 && isUsable()) {
      // Starts the shell service now, so that the tasks are read quickly when Tab follows.
      holdShell()
      return false
    }
    if (
      keyCode == KeyEvent.KEYCODE_TAB &&
        down &&
        event.isAltPressed &&
        !event.isCtrlPressed &&
        !event.isMetaPressed &&
        isUsable()
    ) {
      start(if (event.isShiftPressed) -1 else 1)
      return true
    }
    return false
  }

  /** Leaves Alt+Tab without switching, as when the screen turns off. */
  fun reset() {
    switching = false
    switchWhenRead = false
    tasks = null
    sequence++
  }

  private fun isUsable(): Boolean =
    RecentTasks.isEnabled(context) && RecentTasks.isAvailable(context) && !altIsSandFlyKey()

  private fun isAlt(keyCode: Int) =
    keyCode == KeyEvent.KEYCODE_ALT_LEFT || keyCode == KeyEvent.KEYCODE_ALT_RIGHT

  private fun holdShell() {
    handler.removeCallbacks(releaseShell)
    if (!holding) {
      holding = true
      ShizukuShell.hold(context)
    }
    handler.postDelayed(releaseShell, HOLD_MS)
  }

  private fun start(step: Int) {
    switching = true
    switchWhenRead = false
    tasks = null
    steps = step
    index = 0
    front = (context as? AccessibilityService)?.let { RecentTasks.frontPackages(it) }.orEmpty()
    val current = ++sequence
    holdShell()
    RecentTasks.load(context) { result ->
      if (current != sequence) return@load
      if (result.isNullOrEmpty()) {
        reset()
        speak(context.getString(if (result == null) R.string.overview_unavailable else R.string.overview_empty))
        return@load
      }
      tasks = result
      frontIndex = RecentTasks.frontIndex(result, front)
      index = frontIndex
      val pending = steps
      steps = 0
      move(pending)
      if (switchWhenRead) {
        finish()
      }
    }
  }

  private fun move(step: Int) {
    val list = tasks
    if (list == null) {
      steps += step
      return
    }
    // Before the first task, as on the home screen, going back starts from the last task.
    index = Math.floorMod(if (index < 0 && step < 0) step else index + step, list.size)
    speak(list[index].name(context))
  }

  /** Switches to the task said last, once Alt is released. */
  private fun finish() {
    val list = tasks
    if (list == null) {
      switchWhenRead = true
      return
    }
    val task = list.getOrNull(index)
    reset()
    if (task != null && index != frontIndex) {
      RecentTasks.open(context, task) { worked ->
        if (!worked) speak(context.getString(R.string.overview_failed))
      }
    }
  }

  private fun cancel() {
    val list = tasks
    reset()
    list?.getOrNull(frontIndex)?.let { speak(it.name(context)) }
  }

  private fun speak(text: CharSequence) {
    pipeline.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.speech(
        text,
        SpeakOptions.create()
          .setQueueMode(SpeechController.QUEUE_MODE_INTERRUPT)
          .setFlags(FeedbackItem.FLAG_NO_HISTORY or FeedbackItem.FLAG_FORCE_FEEDBACK_ALL),
      ),
    )
  }

  private companion object {
    /** How long the shell service stays started after Alt is pressed. */
    const val HOLD_MS = 30_000L
  }
}
