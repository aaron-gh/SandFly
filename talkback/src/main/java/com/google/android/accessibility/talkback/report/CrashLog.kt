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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Keeps the stack trace of SandFly's last crash, so that a problem report sent after it can include
 * it: the report's log is only of the process that sends it, which starts after the crash.
 */
object CrashLog {
  private const val FILE_NAME = "last_crash.txt"
  private const val MAX_CHARS = 20_000
  /** A crash older than this is left out of reports. */
  private const val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000

  /** Records uncaught exceptions of this process, then lets the earlier handler run. */
  @JvmStatic
  fun install(context: Context) {
    val storage = context.applicationContext.createDeviceProtectedStorageContext()
    val earlier = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      try {
        val time = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.ROOT).format(Date())
        val text =
          "$time pid ${Process.myPid()} thread ${thread.name}\n" + throwable.stackTraceToString()
        file(storage).writeText(text.take(MAX_CHARS))
      } catch (t: Throwable) {
        // Nothing more can be done while the process is crashing.
      }
      earlier?.uncaughtException(thread, throwable)
    }
  }

  /** The last crash, if it was in the last week, or null. */
  @JvmStatic
  fun read(context: Context): String? {
    val file = file(context.applicationContext.createDeviceProtectedStorageContext())
    if (!file.isFile || System.currentTimeMillis() - file.lastModified() > MAX_AGE_MS) return null
    return runCatching { file.readText() }.getOrNull()?.takeIf { it.isNotBlank() }
  }

  /** Forgets the last crash, once a report has carried it. */
  @JvmStatic
  fun clear(context: Context) {
    file(context.applicationContext.createDeviceProtectedStorageContext()).delete()
  }

  private fun file(storage: Context) = File(storage.filesDir, FILE_NAME)
}
