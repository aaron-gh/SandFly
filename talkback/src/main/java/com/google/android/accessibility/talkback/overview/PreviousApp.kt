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
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED

/**
 * Switches to the app before the one on screen, for the Previous app command and for the Overview
 * action performed again while SandFly's overview is on screen. Reads the recent tasks through
 * Shizuku when it runs and allows SandFly. Otherwise performs the system's Recents action twice.
 *
 * Runs on the main thread.
 */
object PreviousApp {
  /** Time for the system's overview to open before the second Recents action. */
  private const val RECENTS_INTERVAL_MS = 500L

  /**
   * The longest a read of the recent tasks can take for its result to be acted on. It includes
   * starting the Shizuku service, which runs anew for each use from an ordinary app.
   */
  internal const val READ_LIMIT_MS = 5_000L

  /** What to do when the read of the recent tasks ends. */
  internal enum class AfterRead {
    NOTHING,
    SAY_FAILED,
    SAY_UNAVAILABLE,
    PERFORM_RECENTS_TWICE,
    SWITCH,
  }

  private val handler = Handler(Looper.getMainLooper())

  /** Whether the second Recents action is still to be performed. */
  private var secondRecentsPending = false

  /** Returns whether the switch started: false on the lock screen, or if Recents failed. */
  @JvmStatic
  fun switchTo(service: AccessibilityService): Boolean {
    if (isLocked(service)) {
      return false
    }
    val openedFrom = OverviewActivity.openedFrom()
    if (!RecentTasks.isAvailable(service)) {
      if (openedFrom != null) {
        say(R.string.overview_unavailable)
        return true
      }
      return performRecentsTwice(service)
    }
    val front = openedFrom ?: RecentTasks.frontPackages(service)
    val requestedAt = SystemClock.uptimeMillis()
    RecentTasks.load(service) { tasks ->
      when (
        afterRead(
          elapsedMs = SystemClock.uptimeMillis() - requestedAt,
          locked = isLocked(service),
          tasksRead = tasks != null,
          fromOverview = openedFrom != null,
        )
      ) {
        AfterRead.NOTHING -> {}
        AfterRead.SAY_FAILED -> say(R.string.overview_failed)
        AfterRead.SAY_UNAVAILABLE -> say(R.string.overview_unavailable)
        AfterRead.PERFORM_RECENTS_TWICE -> performRecentsTwice(service)
        AfterRead.SWITCH -> open(service, tasks.orEmpty(), front)
      }
    }
    return true
  }

  /**
   * The Recents action counts from the task in front, which is SandFly's overview when
   * [fromOverview], so it would return to the app that the overview was opened from.
   */
  internal fun afterRead(
    elapsedMs: Long,
    locked: Boolean,
    tasksRead: Boolean,
    fromOverview: Boolean,
  ): AfterRead =
    when {
      locked -> AfterRead.NOTHING
      elapsedMs > READ_LIMIT_MS -> AfterRead.SAY_FAILED
      tasksRead -> AfterRead.SWITCH
      fromOverview -> AfterRead.SAY_UNAVAILABLE
      else -> AfterRead.PERFORM_RECENTS_TWICE
    }

  private fun open(service: AccessibilityService, tasks: List<RecentTask>, front: List<String>) {
    val previous =
      RecentTasks.previousTask(tasks, front, setOfNotNull(RecentTasks.homePackage(service)))
    if (previous == null) {
      say(R.string.overview_no_previous_app)
      return
    }
    RecentTasks.open(service, previous) { worked ->
      if (!worked) {
        say(R.string.overview_failed)
      }
    }
  }

  private fun isLocked(service: AccessibilityService): Boolean =
    service.getSystemService(KeyguardManager::class.java).isKeyguardLocked

  private fun performRecentsTwice(service: AccessibilityService): Boolean {
    if (secondRecentsPending) {
      return true
    }
    if (!service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)) {
      return false
    }
    secondRecentsPending = true
    handler.postDelayed(
      {
        secondRecentsPending = false
        TalkBackService.getInstance()?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
      },
      RECENTS_INTERVAL_MS,
    )
    return true
  }

  private fun say(@StringRes text: Int) {
    val talkBack = TalkBackService.getInstance() ?: return
    talkBack.feedbackReturner?.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.requestedSpeech(talkBack.getString(text)),
    )
  }
}
