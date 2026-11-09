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

  private val handler = Handler(Looper.getMainLooper())

  /** Whether the second Recents action is still to be performed. */
  private var secondRecentsPending = false

  /** Returns whether the switch started: false on the lock screen, or if Recents failed. */
  @JvmStatic
  fun switchTo(service: AccessibilityService): Boolean {
    if (service.getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
      return false
    }
    if (!RecentTasks.isAvailable(service)) {
      return performRecentsTwice(service)
    }
    val front = OverviewActivity.openedFrom() ?: RecentTasks.frontPackages(service)
    RecentTasks.load(service) { tasks ->
      if (tasks == null) {
        performRecentsTwice(service)
        return@load
      }
      val previous =
        RecentTasks.previousTask(tasks, front, setOfNotNull(RecentTasks.homePackage(service)))
      if (previous == null) {
        say(R.string.overview_no_previous_app)
        return@load
      }
      RecentTasks.open(service, previous) { worked ->
        if (!worked) {
          say(R.string.overview_failed)
        }
      }
    }
    return true
  }

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
