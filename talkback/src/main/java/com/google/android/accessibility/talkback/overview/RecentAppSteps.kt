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
import android.app.KeyguardManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.StringRes
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.shizuku.ShizukuShell
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechController
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions

/**
 * The Older app and Newer app commands: Older app opens the next older app in the recent tasks, and
 * repeated, goes further back; Newer app goes back toward the most recent one. Each opened app's
 * name is spoken.
 *
 * A sequence of steps keeps the order of the recent tasks as read at its start, since opening a
 * task moves it to the front of the list. It ends [SEQUENCE_MS] after the last step, or when an
 * app that the sequence hasn't opened comes to the front, and the next step starts a new one from
 * the app on screen.
 *
 * The tasks are read and opened through Shizuku, as for SandFly's overview. Without it, Older app
 * does what Previous app does, and Newer app says that the recent apps are unavailable.
 *
 * Runs on the main thread.
 */
object RecentAppSteps {
  /** The time without a step after which the next step starts a new sequence. */
  internal const val SEQUENCE_MS = 5_000L

  private val handler = Handler(Looper.getMainLooper())

  /** Counts sequences, so that a late read for an earlier one is ignored. */
  private var sequence = 0
  /** Whether a sequence is under way: its tasks are being read or have been read. */
  private var active = false
  /** The recent tasks of the sequence, without the launcher, once read. */
  private var tasks: List<RecentTask>? = null
  /** Steps asked for while the tasks were read: positive is older, negative newer. */
  private var pendingSteps = 0
  /** The position of the task opened last, or of the app on screen at the start; -1 for none. */
  private var index = -1
  /** The position Newer app goes back to at most: the app on screen at the start. */
  private var newest = 0
  /** Packages of the app windows the sequence started from and of the tasks it opened. */
  private val visited = HashSet<String>()
  private var lastStepAt = 0L
  /** When the read of the sequence's tasks started. */
  private var readSince = 0L
  /** Whether the Shizuku service is held for the sequence's steps. */
  private var holding = false

  private val endSequence = Runnable { end() }

  /** Takes a step: [older] toward older apps, otherwise toward newer ones. */
  @JvmStatic
  fun step(service: AccessibilityService, older: Boolean): Boolean {
    if (isLocked(service)) {
      return false
    }
    if (!RecentTasks.isAvailable(service)) {
      end()
      if (older) {
        return PreviousApp.switchTo(service)
      }
      say(R.string.overview_unavailable)
      return true
    }
    val step = if (older) 1 else -1
    val front = OverviewActivity.openedFrom() ?: RecentTasks.frontPackages(service)
    val now = SystemClock.uptimeMillis()
    if (tasks == null && active && now - readSince <= PreviousApp.READ_LIMIT_MS) {
      // Still reading: the steps are taken once the tasks are read.
      pendingSteps += step
      return true
    }
    if (
      tasks != null &&
        now - lastStepAt <= SEQUENCE_MS &&
        (front.isEmpty() || front.any { it in visited })
    ) {
      lastStepAt = now
      scheduleEnd()
      move(service, step)
      return true
    }
    start(service, step, front)
    return true
  }

  private fun start(service: AccessibilityService, step: Int, front: List<String>) {
    end()
    active = true
    val current = ++sequence
    pendingSteps = step
    visited += front
    holding = true
    ShizukuShell.hold(service)
    readSince = SystemClock.uptimeMillis()
    RecentTasks.load(service) { result ->
      if (current != sequence) return@load
      when {
        // SandFly was turned off or suspended while the tasks were read.
        TalkBackService.getInstance() == null || !TalkBackService.isServiceActive() -> end()
        isLocked(service) -> end()
        SystemClock.uptimeMillis() - readSince > PreviousApp.READ_LIMIT_MS -> {
          end()
          say(R.string.overview_failed)
        }
        result == null -> {
          end()
          say(R.string.overview_unavailable)
        }
        else -> {
          val home = RecentTasks.homePackage(service)
          val list = result.filter { it.packageName != home }
          if (list.isEmpty()) {
            end()
            say(R.string.overview_empty)
            return@load
          }
          tasks = list
          index = RecentTasks.frontIndex(list, front)
          newest = maxOf(index, 0)
          lastStepAt = SystemClock.uptimeMillis()
          scheduleEnd()
          val steps = pendingSteps
          pendingSteps = 0
          move(service, steps)
        }
      }
    }
  }

  private fun move(service: AccessibilityService, steps: Int) {
    val list = tasks ?: return
    if (steps == 0) return
    val target = target(index, steps, newest, list.size)
    if (target == index) {
      say(if (steps > 0) R.string.overview_no_older_app else R.string.overview_no_newer_app)
      return
    }
    index = target
    val task = list[target]
    visited += task.packageName
    task.topPackage?.let { visited += it }
    speak(task.name(service))
    RecentTasks.open(service, task) { worked ->
      if (!worked) say(R.string.overview_failed)
    }
  }

  /**
   * The position [steps] away from [current] in a list of [size] tasks, positive toward older
   * tasks, going no further than the oldest task or than [newest] toward newer ones. Returns
   * [current] when there is no task that way.
   */
  internal fun target(current: Int, steps: Int, newest: Int, size: Int): Int =
    when {
      steps > 0 -> minOf(current + steps, size - 1).takeIf { it > current } ?: current
      steps < 0 -> maxOf(current + steps, newest).takeIf { it < current } ?: current
      else -> current
    }

  private fun scheduleEnd() {
    handler.removeCallbacks(endSequence)
    handler.postDelayed(endSequence, SEQUENCE_MS)
  }

  /** Ends the sequence, as when SandFly is turned off, so that a read under way opens no task. */
  @JvmStatic
  fun stop() {
    end()
  }

  /** Ends the sequence, so that the next step starts from the app on screen. */
  private fun end() {
    handler.removeCallbacks(endSequence)
    sequence++
    active = false
    tasks = null
    pendingSteps = 0
    index = -1
    newest = 0
    visited.clear()
    if (holding) {
      holding = false
      ShizukuShell.release()
    }
  }

  private fun isLocked(service: AccessibilityService): Boolean =
    service.getSystemService(KeyguardManager::class.java).isKeyguardLocked

  private fun speak(text: CharSequence) {
    val talkBack = TalkBackService.getInstance() ?: return
    talkBack.feedbackReturner?.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.speech(
        text,
        SpeakOptions.create()
          // The app's window and focus are spoken after the name, not over it.
          .setQueueMode(SpeechController.QUEUE_MODE_INTERRUPT_AND_UNINTERRUPTIBLE_BY_NEW_SPEECH)
          .setFlags(FeedbackItem.FLAG_NO_HISTORY or FeedbackItem.FLAG_FORCE_FEEDBACK_ALL),
      ),
    )
  }

  private fun say(@StringRes text: Int) {
    val talkBack = TalkBackService.getInstance() ?: return
    talkBack.feedbackReturner?.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.requestedSpeech(talkBack.getString(text)),
    )
  }
}
