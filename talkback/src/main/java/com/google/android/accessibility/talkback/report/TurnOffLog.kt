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

package com.google.android.accessibility.talkback.report

import android.content.Context
import android.os.Process
import android.provider.Settings
import com.google.android.accessibility.talkback.migration.AppIdMove
import com.google.android.accessibility.talkback.migration.ComponentLists
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Keeps SandFly's log from when its screen reader was last turned off, so that a problem report sent
 * after it can include it: on some phones, the log keeps only the last few minutes.
 */
object TurnOffLog {
  private const val TAG = "TurnOffLog"
  private const val FILE_NAME = "last_turn_off.txt"
  private const val LOG_LINES = 400
  private const val MAX_CHARS = 60_000
  /** A turn-off older than this is left out of reports. */
  private const val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000

  /**
   * Records the turn-off now: whether the service is still among Android's enabled accessibility
   * services, [activeApp], the app in front, and the end of the log. The log is read on a thread of
   * its own.
   */
  @JvmStatic
  fun record(context: Context, activeApp: CharSequence?) {
    val app = context.applicationContext
    val time = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.ROOT).format(Date())
    val enabled =
      runCatching {
          Settings.Secure.getString(app.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        }
        .getOrNull()
    val stillOn = ComponentLists.contains(enabled, AppIdMove.serviceName(app.packageName))
    val head =
      "$time pid ${Process.myPid()}\n" +
        "Still an enabled accessibility service: ${if (stillOn) "yes" else "no"}\n" +
        "App in front: ${activeApp ?: "unknown"}\n" +
        "Enabled accessibility services: $enabled\n\n"
    val pid = Process.myPid()
    Thread(
        {
          val log =
            ProblemReportSender.readLog(pid, LOG_LINES, ProblemReportSender.MAIN_LOG_PRIORITY)
              .orEmpty()
          try {
            file(app).writeText((head + log).takeLast(MAX_CHARS))
          } catch (e: IOException) {
            LogUtils.w(TAG, "Cannot keep the turn-off log: %s", e)
          }
        },
        TAG,
      )
      .start()
  }

  /** The last turn-off, if it was in the last week, or null. */
  @JvmStatic
  fun read(context: Context): String? {
    val file = file(context.applicationContext)
    if (!file.isFile || System.currentTimeMillis() - file.lastModified() > MAX_AGE_MS) return null
    return runCatching { file.readText() }.getOrNull()?.takeIf { it.isNotBlank() }
  }

  /** Forgets the last turn-off, once a report has carried it. */
  @JvmStatic
  fun clear(context: Context) {
    file(context.applicationContext).delete()
  }

  private fun file(app: Context) = File(app.createDeviceProtectedStorageContext().filesDir, FILE_NAME)
}
