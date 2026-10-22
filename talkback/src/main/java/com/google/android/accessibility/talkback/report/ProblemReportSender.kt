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
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import com.google.android.accessibility.talkback.update.UpdateChecker
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.IOException
import java.net.HttpURLConnection
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Talks to the report server, which creates the GitHub issues: sends problem reports, reads the
 * status and replies of sent ones, and sends the reporter's replies.
 */
object ProblemReportSender {
  enum class Result {
    SENT,
    RATE_LIMITED,
    NO_CONNECTION,
    NOT_SENT,
  }

  private const val TAG = "ProblemReportSender"
  // The Worker in tools/report-worker.
  private const val SERVER = "https://sandfly-reports.aaron-graham-hewitt.workers.dev"
  private const val REPORT_URL = "$SERVER/report"
  private const val THREADS_URL = "$SERVER/threads"
  private const val REPLY_URL = "$SERVER/reply"
  /** The most tokens the server takes in one request for threads. */
  private const val MAX_TOKENS = 20
  private const val HTTP_OK = 200
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

  /** The status and messages of a sent report, as the report server has them. */
  data class ReportThread(val token: String, val status: String, val messages: List<ReportMessage>)

  /**
   * Sends the [report], with SandFly's log if [withLog], and calls [done] on the main thread with
   * the result and, once sent, the token the server knows the report by.
   */
  @MainThread
  fun send(
    context: Context,
    report: ProblemReport,
    withLog: Boolean,
    done: (Result, String?) -> Unit,
  ) {
    isSending = true
    val appContext = context.applicationContext
    executor.execute {
      val (result, answer) =
        try {
          post(appContext, REPORT_URL, (if (withLog) report.copy(log = readLog()) else report).toJson())
        } catch (e: RuntimeException) {
          LogUtils.e(TAG, "Report not sent: %s", e)
          Result.NOT_SENT to null
        }
      val token = answer?.optString("token")?.takeIf { it.isNotEmpty() }
      mainHandler.post {
        isSending = false
        done(result, token)
      }
    }
  }

  /** Sends a reply to the report with [token], and calls [done] on the main thread. */
  @MainThread
  fun reply(context: Context, token: String, body: String, done: (Result, ReportMessage?) -> Unit) {
    val appContext = context.applicationContext
    executor.execute {
      val (result, answer) =
        try {
          post(appContext, REPLY_URL, JSONObject().put("token", token).put("body", body))
        } catch (e: RuntimeException) {
          LogUtils.e(TAG, "Reply not sent: %s", e)
          Result.NOT_SENT to null
        }
      val message =
        answer?.let {
          ReportMessage(it.optLong("id"), fromDeveloper = false, body = body, at = it.optString("at"))
        }
      mainHandler.post { done(result, message) }
    }
  }

  /** Reads the threads of the reports with [tokens], or returns null if they can't be read. */
  @WorkerThread
  fun threads(context: Context, tokens: List<String>): List<ReportThread>? {
    val threads = mutableListOf<ReportThread>()
    for (chunk in tokens.chunked(MAX_TOKENS)) {
      val (result, answer) =
        post(context, THREADS_URL, JSONObject().put("tokens", JSONArray(chunk)), HTTP_OK)
      if (result != Result.SENT || answer == null) return null
      try {
        threads += parseThreads(answer)
      } catch (e: JSONException) {
        LogUtils.w(TAG, "Cannot read the threads: %s", e)
        return null
      }
    }
    return threads
  }

  @VisibleForTesting
  fun parseThreads(answer: JSONObject): List<ReportThread> {
    val array = answer.getJSONArray("threads")
    return (0 until array.length()).map { index ->
      val thread = array.getJSONObject(index)
      val messages = thread.optJSONArray("messages") ?: JSONArray()
      ReportThread(
        token = thread.getString("token"),
        status = thread.optString("status", SentReport.STATUS_OPEN),
        messages =
          (0 until messages.length()).map {
            val message = messages.getJSONObject(it)
            ReportMessage(
              id = message.getLong("id"),
              fromDeveloper = message.optString("from") == "developer",
              body = message.optString("body"),
              at = message.optString("at"),
            )
          },
      )
    }
  }

  /** Posts [json] to [url], and returns the result and the server's answer. */
  @WorkerThread
  private fun post(
    context: Context,
    url: String,
    json: JSONObject,
    success: Int = HTTP_CREATED,
  ): Pair<Result, JSONObject?> {
    val connection: HttpURLConnection =
      try {
        UpdateChecker.openConnection(context, url)
      } catch (e: IOException) {
        return Result.NO_CONNECTION to null
      }
    return try {
      connection.requestMethod = "POST"
      connection.doOutput = true
      connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
      val body = json.toString().toByteArray(Charsets.UTF_8)
      connection.setFixedLengthStreamingMode(body.size)
      connection.outputStream.use { it.write(body) }
      when (val code = connection.responseCode) {
        success -> Result.SENT to readAnswer(connection)
        HTTP_TOO_MANY_REQUESTS -> Result.RATE_LIMITED to null
        else -> {
          LogUtils.w(TAG, "Report server returned %d", code)
          Result.NOT_SENT to null
        }
      }
    } catch (e: IOException) {
      LogUtils.w(TAG, "Report server not reached: %s", e)
      Result.NO_CONNECTION to null
    } finally {
      connection.disconnect()
    }
  }

  /** The server's JSON answer, or null if it can't be read or is too long. */
  private fun readAnswer(connection: HttpURLConnection): JSONObject? =
    try {
      val text =
        connection.inputStream.use { input ->
          val output = java.io.ByteArrayOutputStream()
          val buffer = ByteArray(BUFFER_BYTES)
          while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            output.write(buffer, 0, count)
            if (output.size() > MAX_ANSWER_BYTES) throw IOException("The answer is too long")
          }
          output.toByteArray().decodeToString()
        }
      JSONObject(text)
    } catch (e: IOException) {
      LogUtils.w(TAG, "Cannot read the report server's answer: %s", e)
      null
    } catch (e: JSONException) {
      LogUtils.w(TAG, "Cannot read the report server's answer: %s", e)
      null
    }

  private const val BUFFER_BYTES = 8192

  /** The most of an answer that is read: 20 threads of up to 100 messages. */
  private const val MAX_ANSWER_BYTES = 4 * 1024 * 1024

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
      if (log.length <= ProblemReport.MAX_LOG) log
      else log.takeLast(ProblemReport.MAX_LOG).substringAfter('\n')
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
