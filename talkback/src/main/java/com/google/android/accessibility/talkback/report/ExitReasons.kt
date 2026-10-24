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

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * How SandFly's last processes ended, as Android recorded it, for problem reports: a process that
 * the phone stops, such as when a phone maker's power or security manager kills it, leaves nothing
 * in SandFly's own log.
 */
object ExitReasons {
  private const val MAX_EXITS = 10

  /** The last process exits, newest first, or null if there are none or before Android 11. */
  @JvmStatic
  fun describe(context: Context): String? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
    val exits =
      runCatching {
          context
            .getSystemService(ActivityManager::class.java)
            ?.getHistoricalProcessExitReasons(context.packageName, 0, MAX_EXITS)
        }
        .getOrNull()
    if (exits.isNullOrEmpty()) return null
    return exits.joinToString("\n") { describe(it) }
  }

  @RequiresApi(Build.VERSION_CODES.R)
  private fun describe(exit: ApplicationExitInfo): String {
    val time = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.ROOT).format(Date(exit.timestamp))
    return "$time ${exit.processName} pid ${exit.pid}: ${reasonName(exit.reason)}, " +
      "status ${exit.status}, importance ${exit.importance}, " +
      (exit.description ?: "no description")
  }

  /** The name of an exit reason. Numbers, since some of the constants are only on newer Android. */
  private fun reasonName(reason: Int): String =
    when (reason) {
      0 -> "unknown"
      1 -> "exited itself"
      2 -> "signaled"
      3 -> "low memory"
      4 -> "crash"
      5 -> "native crash"
      6 -> "not responding"
      7 -> "initialization failure"
      8 -> "permission change"
      9 -> "excessive resource use"
      10 -> "stopped by the user"
      11 -> "user stopped"
      12 -> "dependency died"
      13 -> "other"
      14 -> "freezer"
      15 -> "package state change"
      16 -> "package updated"
      else -> "reason $reason"
    }
}
