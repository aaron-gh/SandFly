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

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.Callable
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
  private const val LOG_LINES = 10_000

  /** The most lines and characters of the log of each of SandFly's other processes. */
  private const val OTHER_LOG_LINES = 3_000
  private const val MAX_OTHER_LOG = 100_000

  /**
   * The most lines and characters of the gesture log: this process's last lines with the gesture
   * tag, which the main log reaches back only a few seconds for at Debug level.
   */
  internal const val GESTURE_LOG_LINES = 2_000
  internal const val MAX_GESTURE_LOG = 60_000
  private const val GESTURE_TAG = "BacktalkGesture"
  private const val MAX_GESTURE_LINE = 1_000

  /**
   * The lowest priority of this process's log lines that a report holds: debug. Verbose lines come
   * many a second while SandFly reads, and would leave room for only the last second or two.
   */
  internal const val MAIN_LOG_PRIORITY = "*:D"
  private const val HTTP_BAD_REQUEST = 400
  private const val HTTP_TOO_LARGE = 413

  /** The folder of the IDs of SandFly's other processes, by process name. */
  private const val PROCESS_IDS_DIR = "process-ids"
  private const val LOG_TIMEOUT_SECONDS = 5L

  // One thread at most, which ends when idle.
  private val executor = ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS, LinkedBlockingQueue())
  private val mainHandler = Handler(Looper.getMainLooper())

  // One thread at most, which ends when idle. Not [executor], whose sends can take a minute and
  // would delay a read that has to come before the report form's own output.
  private val snapshotExecutor =
    ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS, LinkedBlockingQueue())
  private val logSnapshot =
    ReportLogSnapshot(
      submit = { task -> snapshotExecutor.submit(task) },
      elapsedMs = { SystemClock.elapsedRealtime() },
      postDelayed = { task, delayMs -> mainHandler.postDelayed(task, delayMs) },
      removeCallbacks = { task -> mainHandler.removeCallbacks(task) },
    )

  // Holds no activity, so it can stay in each activity's lifecycle.
  private val reportActivityObserver =
    object : DefaultLifecycleObserver {
      override fun onStart(owner: LifecycleOwner) {
        logSnapshot.activityStarted(System.identityHashCode(owner))
      }

      override fun onStop(owner: LifecycleOwner) {
        val activity = owner as? Activity ?: return
        if (activity.isChangingConfigurations) return
        val screenOn = activity.getSystemService(PowerManager::class.java)?.isInteractive != false
        logSnapshot.activityStopped(System.identityHashCode(owner), screenOn)
      }

      override fun onDestroy(owner: LifecycleOwner) {
        val activity = owner as? Activity ?: return
        if (!activity.isChangingConfigurations) logSnapshot.endVisit(System.identityHashCode(owner))
      }
    }

  /** True from [send] until its result is delivered. */
  @Volatile
  var isSending = false
    private set

  /** The status and messages of a sent report, as the report server has them. */
  data class ReportThread(val token: String, val status: String, val messages: List<ReportMessage>)

  /**
   * Call when a report screen is shown in [activity]. Starts a visit to the report screens, with a
   * snapshot of the logcat text read off the main thread, if none is open. See [ReportLogSnapshot].
   */
  @MainThread
  fun reportScreenShown(activity: FragmentActivity) {
    activity.lifecycle.addObserver(reportActivityObserver)
    logSnapshot.screenShown(System.identityHashCode(activity), snapshotRead(activity))
  }

  /** Call when a report form is opened: reads a snapshot if the visit has none. */
  @MainThread
  fun reportFormOpened(activity: FragmentActivity) {
    logSnapshot.readIfMissing(snapshotRead(activity))
  }

  /**
   * Call when Include SandFly log changes. Turning it off drops the snapshot, and no later form of
   * the visit reads one.
   */
  @MainThread
  fun includeLogChanged(activity: FragmentActivity, include: Boolean) {
    if (include) logSnapshot.logTurnedOn(snapshotRead(activity)) else logSnapshot.logTurnedOff()
  }

  /** Call when the list of reports closes, which ends the visit. */
  @MainThread
  fun reportListClosed() {
    logSnapshot.endVisit()
  }

  /**
   * Sends the [report], with SandFly's log if [withLog], and calls [done] on the main thread with
   * the result and, once sent, the token the server knows the report by. The logcat text is the
   * snapshot read when the report screens opened, or the log at the time of sending if there is
   * none or [inMultiWindow], where the reporter can reproduce a problem in the other pane. The
   * head sections are always read at the time of sending.
   */
  @MainThread
  fun send(
    context: Context,
    report: ProblemReport,
    withLog: Boolean,
    inMultiWindow: Boolean,
    done: (Result, String?) -> Unit,
  ) {
    isSending = true
    val appContext = context.applicationContext
    if (!withLog || inMultiWindow) logSnapshot.discard()
    val snapshot = logSnapshot.current()
    executor.execute {
      val (result, answer) =
        try {
          sendReport(
            appContext,
            if (withLog) report.copy(log = logWithCrash(appContext, logSnapshot.await(snapshot)))
            else report,
          )
        } catch (e: RuntimeException) {
          LogUtils.e(TAG, "Report not sent: %s", e)
          Result.NOT_SENT to null
        }
      if (withLog && result == Result.SENT) {
        CrashLog.clear(appContext)
        TurnOffLog.clear(appContext)
        StallLog.clear(appContext)
      }
      val token = answer?.optString("token")?.takeIf { it.isNotEmpty() }
      mainHandler.post {
        isSending = false
        // A report that was not sent can be sent again, with the same log.
        if (result == Result.SENT) logSnapshot.clearSent(snapshot)
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

  /**
   * Sends [report]. A server from before longer logs turns a long one down, so then it is sent
   * again with the end of the log that such a server takes.
   */
  @WorkerThread
  private fun sendReport(context: Context, report: ProblemReport): Pair<Result, JSONObject?> {
    val log = report.log
    if (log == null || log.length <= ProblemReport.OLD_MAX_LOG) {
      return post(context, REPORT_URL, report.toJson())
    }
    var turnedDown = false
    val sent = post(context, REPORT_URL, report.toJson(), onBadRequest = { turnedDown = true })
    if (!turnedDown) return sent
    LogUtils.w(TAG, "The report server turned the long log down; sending a shorter one")
    val shorter = log.takeLast(ProblemReport.OLD_MAX_LOG).substringAfter('\n')
    return post(context, REPORT_URL, report.copy(log = shorter).toJson())
  }

  /**
   * Posts [json] to [url], and returns the result and the server's answer. [onBadRequest] runs when
   * the server turns the request down as wrong or too large.
   */
  @WorkerThread
  private fun post(
    context: Context,
    url: String,
    json: JSONObject,
    success: Int = HTTP_CREATED,
    onBadRequest: () -> Unit = {},
  ): Pair<Result, JSONObject?> {
    val connection: HttpURLConnection =
      try {
        openConnection(context, url)
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
          if (code == HTTP_BAD_REQUEST || code == HTTP_TOO_LARGE) onBadRequest()
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

  /**
   * The log, after SandFly's last crash, last turn-off and main thread stalls if there were any,
   * which the log doesn't reach back to, and how its last processes ended, then the last gesture
   * lines, and then the ends of the logs of SandFly's other processes, such as the one that runs
   * on-device AI.
   */
  @WorkerThread
  private fun logWithCrash(context: Context, snapshot: LogcatSnapshot?): String? {
    val crash = CrashLog.read(context)
    val turnOff = TurnOffLog.read(context)
    val stalls = StallLog.read(context)
    val exits = ExitReasons.describe(context)
    val others = snapshot?.others ?: otherProcessLogs(context)
    val log = snapshot?.main ?: readLog(Process.myPid(), LOG_LINES, MAIN_LOG_PRIORITY)
    val gestures = snapshot?.gestures ?: readGestureLog(Process.myPid()).orEmpty()
    if (
      crash == null &&
        turnOff == null &&
        stalls == null &&
        exits == null &&
        others.isEmpty() &&
        gestures.isEmpty()
    ) {
      return log
    }
    val head =
      (if (crash == null) "" else "Last crash:\n$crash\n\n") +
        (if (turnOff == null) "" else "Last turn-off:\n$turnOff\n\n") +
        (if (stalls == null) "" else "Main thread stalls:\n$stalls\n\n") +
        (if (exits == null) "" else "Process exits:\n$exits\n\n") +
        (if (crash == null && turnOff == null && stalls == null && exits == null) "" else "Log:\n")
    return joinLog(head, log.orEmpty(), gestures, others)
  }

  /**
   * [head], as much of the end of [log] as fits in [ProblemReport.MAX_LOG], the gesture lines
   * under their heading, and the other processes' logs.
   */
  @VisibleForTesting
  internal fun joinLog(head: String, log: String, gestures: String, others: String): String {
    val gestureSection = gestureSection(gestures)
    val room =
      (ProblemReport.MAX_LOG - head.length - gestureSection.length - others.length).coerceAtLeast(0)
    return head + log.takeLast(room) + gestureSection + others
  }

  private fun gestureSection(gestures: String): String =
    if (gestures.isEmpty()) "" else "\n\nGesture log:\n$gestures"


  /**
   * Reads the logcat text for a snapshot, null when [activity] is in multi-window mode. The read
   * gives null if logcat gives no main log, so that Send reads the log itself.
   */
  private fun snapshotRead(activity: FragmentActivity): Callable<LogcatSnapshot?>? {
    if (activity.isInMultiWindowMode) return null
    val context = activity.applicationContext
    return Callable<LogcatSnapshot?> {
      val main =
        readLog(Process.myPid(), LOG_LINES, MAIN_LOG_PRIORITY)?.takeIf { it.isNotBlank() }
          ?: return@Callable null
      val gestures = readGestureLog(Process.myPid()).orEmpty()
      val others = otherProcessLogs(context)
      val room =
        (ProblemReport.MAX_LOG - gestureSection(gestures).length - others.length)
          .coerceAtLeast(0)
      LogcatSnapshot(main.takeLast(room), others, gestures)
    }
  }

  /**
   * Remembers this process's ID under its name, so that a report includes its log even after it
   * has ended. Call it when one of SandFly's processes starts: the screen reader's, and others such
   * as on-device AI's.
   */
  @JvmStatic
  fun rememberProcess(context: Context, name: String) {
    try {
      val dir = File(context.noBackupFilesDir, PROCESS_IDS_DIR).apply { mkdirs() }
      File(dir, name).writeText(Process.myPid().toString())
    } catch (e: IOException) {
      LogUtils.w(TAG, "Cannot remember the process %s: %s", name, e)
    }
  }

  /** The ends of the logs of SandFly's processes other than this one, each under its name. */
  @WorkerThread
  private fun otherProcessLogs(context: Context): String {
    val pids = LinkedHashMap<Int, String>()
    // Processes that ran since, whose logs are still in the log buffer if they have ended.
    File(context.noBackupFilesDir, PROCESS_IDS_DIR).listFiles()?.forEach { file ->
      val text =
        try {
          file.readText()
        } catch (e: IOException) {
          LogUtils.w(TAG, "Cannot read the process ID %s: %s", file.name, e)
          return@forEach
        }
      text.trim().toIntOrNull()?.let { pids[it] = file.name }
    }
    try {
      context.getSystemService(ActivityManager::class.java)?.runningAppProcesses?.forEach {
        if (it.processName.startsWith(context.packageName)) pids[it.pid] = it.processName
      }
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Cannot list SandFly's processes: %s", e)
    }
    pids.remove(Process.myPid())
    val logs = StringBuilder()
    for ((pid, name) in pids) {
      val log =
        readLog(pid, OTHER_LOG_LINES, MAIN_LOG_PRIORITY)?.takeIf { it.isNotBlank() } ?: continue
      logs.append("\n\nLog of ").append(name).append(":\n")
      logs.append(log.takeLast(MAX_OTHER_LOG))
    }
    return logs.toString()
  }

  /**
   * The last [lines] lines of the log of process [pid], or null if logcat cannot be run, with only
   * the lines of [priority] or higher, in logcat's filter format, if it isn't null.
   */
  @WorkerThread
  internal fun readLog(pid: Int, lines: Int, priority: String? = null): String? {
    val process =
      try {
        ProcessBuilder(
            "logcat",
            "-d",
            "-v",
            "threadtime",
            "-t",
            lines.toString(),
            "--pid",
            pid.toString(),
            *listOfNotNull(priority).toTypedArray(),
          )
          .redirectErrorStream(true)
          .start()
      } catch (e: IOException) {
        LogUtils.w(TAG, "Cannot run logcat: %s", e)
        return null
      }
    return try {
      val log = compact(process.inputStream.bufferedReader().use { it.readText() })
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

  /**
   * The last [GESTURE_LOG_LINES] lines of process [pid] with the gesture tag, at most
   * [MAX_GESTURE_LOG] characters, or null if logcat cannot be run.
   */
  @WorkerThread
  private fun readGestureLog(pid: Int): String? {
    // No -t: logcat counts those lines before it filters by tag, which would leave only the
    // gesture lines among the last lines of the whole log.
    val process =
      try {
        ProcessBuilder(
            "logcat",
            "-d",
            "-v",
            "threadtime",
            "--pid",
            pid.toString(),
            "-s",
            "$GESTURE_TAG:V",
          )
          .redirectErrorStream(true)
          .start()
      } catch (e: IOException) {
        LogUtils.w(TAG, "Cannot run logcat: %s", e)
        return null
      }
    return try {
      val log =
        process.inputStream.bufferedReader().use {
          lastLines(it, GESTURE_LOG_LINES, MAX_GESTURE_LOG)
        }
      process.waitFor(LOG_TIMEOUT_SECONDS, TimeUnit.SECONDS)
      log
    } catch (e: IOException) {
      LogUtils.w(TAG, "Cannot read the gesture log: %s", e)
      null
    } catch (e: InterruptedException) {
      Thread.currentThread().interrupt()
      null
    } finally {
      process.destroy()
    }
  }

  /**
   * The last [maxLines] lines of [reader], at most [maxChars] characters in all, without logcat's
   * buffer headings. Holds no more than that while it reads; longer lines are cut.
   */
  @VisibleForTesting
  internal fun lastLines(reader: BufferedReader, maxLines: Int, maxChars: Int): String {
    val lines = ArrayDeque<String>()
    var chars = 0
    while (true) {
      val line = reader.readLine() ?: break
      if (line.isEmpty() || line.startsWith(LOGCAT_HEADING)) continue
      val kept =
        if (line.length > MAX_GESTURE_LINE) line.substring(0, MAX_GESTURE_LINE) + "…" else line
      lines.addLast(kept)
      chars += kept.length + 1
      while (lines.isNotEmpty() && (lines.size > maxLines || chars - 1 > maxChars)) {
        chars -= lines.removeFirst().length + 1
      }
    }
    return lines.joinToString("\n")
  }

  private const val LOGCAT_HEADING = "--------- "

  /**
   * Leaves out the Debug lines that repeat each feedback part ("Actors: act()" logs what
   * "Pipeline: execute()" logged just before) and shortens the "Pipeline: execute()" lines, which
   * otherwise fill most of a report's log in a few seconds of moving around.
   */
  @VisibleForTesting
  internal fun compact(log: String): String {
    if (!log.contains(PIPELINE_MARKER) && !log.contains(ACTORS_MARKER)) return log
    return log
      .split('\n')
      .filterNot { it.contains(ACTORS_MARKER) }
      .joinToString("\n") {
        if (it.length > MAX_PIPELINE_LINE && it.contains(PIPELINE_MARKER)) {
          it.substring(0, MAX_PIPELINE_LINE) + "…"
        } else {
          it
        }
      }
  }

  private const val PIPELINE_MARKER = " Pipeline: execute() "
  private const val ACTORS_MARKER = " Actors: act() "
  private const val MAX_PIPELINE_LINE = 300

  private const val CONNECTION_TIMEOUT_MS = 30_000

  /** Opens a connection to [url] with SandFly's timeouts and user agent. */
  private fun openConnection(context: Context, url: String): HttpURLConnection {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = CONNECTION_TIMEOUT_MS
    connection.readTimeout = CONNECTION_TIMEOUT_MS
    val version =
      try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
      } catch (e: Exception) {
        ""
      }
    connection.setRequestProperty("User-Agent", "SandFly/$version")
    return connection
  }
}
