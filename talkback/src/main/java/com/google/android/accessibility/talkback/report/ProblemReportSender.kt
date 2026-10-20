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
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import com.google.android.accessibility.talkback.update.UpdateChecker
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Sends problem reports to the report server, which creates the GitHub issues. */
object ProblemReportSender {
  enum class Result {
    SENT,
    RATE_LIMITED,
    NO_CONNECTION,
    NOT_SENT,
  }

  private const val TAG = "ProblemReportSender"
  // The Worker in tools/report-worker.
  private const val REPORT_URL = "https://sandfly-reports.aaron-gh.workers.dev/report"
  private const val HTTP_CREATED = 201
  private const val HTTP_TOO_MANY_REQUESTS = 429
  private const val LOG_LINES = 1000
  private const val LOG_TIMEOUT_SECONDS = 5L

  // One thread at most, which ends when idle.
  private val executor = ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS, LinkedBlockingQueue())
  private val mainHandler = Handler(Looper.getMainLooper())

  /** True from [send] until its result is delivered. */
  @Volatile
  var isSending = false
    private set

  /** Sends the [report], with SandFly's log if [withLog], and calls [done] on the main thread. */
  @MainThread
  fun send(context: Context, report: ProblemReport, withLog: Boolean, done: (Result) -> Unit) {
    isSending = true
    val appContext = context.applicationContext
    executor.execute {
      val result =
        try {
          post(appContext, if (withLog) report.copy(log = readLog()) else report)
        } catch (e: RuntimeException) {
          LogUtils.e(TAG, "Report not sent: %s", e)
          Result.NOT_SENT
        }
      mainHandler.post {
        isSending = false
        done(result)
      }
    }
  }

  @WorkerThread
  private fun post(context: Context, report: ProblemReport): Result {
    val connection =
      try {
        UpdateChecker.openConnection(context, REPORT_URL)
      } catch (e: IOException) {
        return Result.NO_CONNECTION
      }
    return try {
      connection.requestMethod = "POST"
      connection.doOutput = true
      connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
      val body = report.toJson().toString().toByteArray(Charsets.UTF_8)
      connection.setFixedLengthStreamingMode(body.size)
      connection.outputStream.use { it.write(body) }
      when (val code = connection.responseCode) {
        HTTP_CREATED -> Result.SENT
        HTTP_TOO_MANY_REQUESTS -> Result.RATE_LIMITED
        else -> {
          LogUtils.w(TAG, "Report server returned %d", code)
          Result.NOT_SENT
        }
      }
    } catch (e: IOException) {
      LogUtils.w(TAG, "Report not sent: %s", e)
      Result.NO_CONNECTION
    } finally {
      connection.disconnect()
    }
  }

  /** The last lines of this process's log, or null if logcat cannot be run. */
  @WorkerThread
  private fun readLog(): String? {
    val process =
      try {
        ProcessBuilder(
            "logcat",
            "-d",
            "-v",
            "threadtime",
            "-t",
            LOG_LINES.toString(),
            "--pid",
            Process.myPid().toString(),
          )
          .redirectErrorStream(true)
          .start()
      } catch (e: IOException) {
        LogUtils.w(TAG, "Cannot run logcat: %s", e)
        return null
      }
    return try {
      val log = process.inputStream.bufferedReader().use { it.readText() }
      process.waitFor(LOG_TIMEOUT_SECONDS, TimeUnit.SECONDS)
      log.takeLast(ProblemReport.MAX_LOG)
    } catch (e: IOException) {
      LogUtils.w(TAG, "Cannot read the log: %s", e)
      null
    } catch (e: InterruptedException) {
      Thread.currentThread().interrupt()
      null
    } finally {
      process.destroy()
    }
  }
}
