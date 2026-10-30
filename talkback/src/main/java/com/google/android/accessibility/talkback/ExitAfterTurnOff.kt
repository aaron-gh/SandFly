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

package com.google.android.accessibility.talkback

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.Settings
import com.google.android.accessibility.talkback.report.ProblemReportSender
import com.google.android.accessibility.talkback.update.Updater
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Ends SandFly's process once its screen reader is turned off and nothing else of SandFly is in
 * use: no SandFly screen or keyboard showing, no download running, no report being sent. A process
 * left running after the turn-off kept another screen reader turned on after it silent on some
 * phones, until SandFly was force stopped.
 */
object ExitAfterTurnOff {
  private const val TAG = "ExitAfterTurnOff"
  // Leaves time for the turn-off log to be written.
  private const val DELAY_MS = 5_000L

  private val handler = Handler(Looper.getMainLooper())
  private var application: Application? = null

  private val check = Runnable { exitIfUnused() }

  private val activityCallbacks =
    object : Application.ActivityLifecycleCallbacks {
      override fun onActivityStopped(activity: Activity) = later()

      override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

      override fun onActivityStarted(activity: Activity) {}

      override fun onActivityResumed(activity: Activity) {}

      override fun onActivityPaused(activity: Activity) {}

      override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

      override fun onActivityDestroyed(activity: Activity) {}
    }

  /** Called when the screen reader is turned off. */
  @JvmStatic
  fun schedule(app: Application) {
    if (application == null) {
      application = app
      app.registerActivityLifecycleCallbacks(activityCallbacks)
    }
    later()
  }

  /** Called when the screen reader is turned on. */
  @JvmStatic
  fun cancel() {
    handler.removeCallbacks(check)
    application?.unregisterActivityLifecycleCallbacks(activityCallbacks)
    application = null
  }

  private fun later() {
    handler.removeCallbacks(check)
    handler.postDelayed(check, DELAY_MS)
  }

  private fun exitIfUnused() {
    val app = application ?: return
    if (TalkBackService.getInstance() != null) {
      cancel()
      return
    }
    if (ProblemReportSender.isSending || Updater.isBusy) {
      later()
      return
    }
    // A SandFly screen, the SandFly keyboard while it shows, or a download in the foreground. Checked
    // again when a SandFly screen closes.
    val state = ActivityManager.RunningAppProcessInfo()
    ActivityManager.getMyMemoryState(state)
    if (state.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE) return
    val keyboard =
      runCatching { Settings.Secure.getString(app.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) }
        .getOrNull()
    if (keyboard != null && keyboard.startsWith(app.packageName + "/")) return
    LogUtils.w(TAG, "SandFly is off and not in use, ending its process")
    Process.killProcess(Process.myPid())
  }
}
