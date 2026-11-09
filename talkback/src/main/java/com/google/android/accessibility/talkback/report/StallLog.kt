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

package com.google.android.accessibility.talkback.report

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import com.google.android.accessibility.talkback.gesture.AppGestures
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Records when the screen reader's main thread stops responding, so that a problem report can
 * include it: a hang writes no turn-off or crash record, and a restart clears the log.
 *
 * While the screen is on, a thread of its own posts to the main thread every few seconds. If the
 * post has not run after [StallDetector.STAGES_MS], it saves the stack of the main thread and of
 * the threads running SandFly's code, and the end of the log.
 */
object StallLog {
  private const val TAG = "StallLog"
  private const val DIR_NAME = "stalls"
  private const val CHECK_MS = 5_000L
  private const val LOG_LINES = 200
  private const val MAX_RECORD_CHARS = 30_000
  private const val MAX_STACK_FRAMES = 40
  private const val MAX_OTHER_THREADS = 12
  private const val MAX_OTHER_FRAMES = 12
  @VisibleForTesting internal const val MAX_RECORDS = 3
  /** A stall older than this is left out of reports. */
  private const val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000
  private const val APP_PACKAGE_PREFIX = "com.google.android.accessibility"

  private var thread: HandlerThread? = null
  private var watcher: Handler? = null
  private val main = Handler(Looper.getMainLooper())

  // Used only on the watcher thread.
  private var detector = StallDetector()
  private var currentRecord: File? = null

  private val ping = Runnable {
    val answered = SystemClock.uptimeMillis()
    watcher?.post { onAnswered(answered) }
  }

  /** Starts watching the main thread, when the screen reader is turned on. */
  @JvmStatic
  @Synchronized
  fun start(context: Context) {
    if (thread != null) return
    val app = context.applicationContext
    detector = StallDetector()
    currentRecord = null
    val started = HandlerThread(TAG).apply { start() }
    val handler = Handler(started.looper)
    thread = started
    watcher = handler
    val power = app.getSystemService(PowerManager::class.java)
    handler.post(
      object : Runnable {
        override fun run() {
          try {
            check(app, power)
          } catch (t: Throwable) {
            LogUtils.w(TAG, "Cannot check the main thread: %s", t)
          }
          handler.postDelayed(this, CHECK_MS)
        }
      }
    )
  }

  /** Stops watching, when the screen reader is turned off. */
  @JvmStatic
  @Synchronized
  fun stop() {
    main.removeCallbacks(ping)
    thread?.quit()
    thread = null
    watcher = null
  }

  private fun check(app: Context, power: PowerManager?) {
    val now = SystemClock.uptimeMillis()
    when (val decision = detector.tick(now, power?.isInteractive != false)) {
      StallDetector.PING -> main.post(ping)
      StallDetector.NOTHING -> {}
      else -> record(app, decision)
    }
  }

  private fun onAnswered(answered: Long) {
    val stalled = detector.answered(answered) ?: return
    currentRecord?.let { file ->
      runCatching { file.appendText("\nMain thread responded after ${stalled / 1000} s.\n") }
    }
    currentRecord = null
  }

  private fun record(app: Context, stalledMs: Long) {
    val first = currentRecord == null
    val file = currentRecord ?: newRecord(app).also { currentRecord = it }
    val time = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.ROOT).format(Date())
    val text = buildString {
      append(time).append(" pid ").append(Process.myPid())
      append(": main thread not responding for ").append(stalledMs / 1000).append(" s\n")
      if (first) append("App in front: ").append(AppGestures.appShown().ifEmpty { "unknown" }).append('\n')
      append(stacks())
      append('\n')
    }
    // The stacks go first: the process may not live until the log has been read.
    file.appendText(text.take(MAX_RECORD_CHARS))
    if (first) {
      val log =
        ProblemReportSender.readLog(
          Process.myPid(),
          LOG_LINES,
          ProblemReportSender.MAIN_LOG_PRIORITY,
        )
      if (log != null) file.appendText("Log:\n" + log.takeLast(MAX_RECORD_CHARS) + "\n")
    }
  }

  private fun newRecord(app: Context): File {
    val dir = dir(app).apply { mkdirs() }
    prune(dir, MAX_RECORDS - 1)
    return File(dir, "${System.currentTimeMillis()}.txt")
  }

  /** Deletes all but the [keep] newest records in [dir]. */
  @VisibleForTesting
  internal fun prune(dir: File, keep: Int) {
    val records = dir.listFiles()?.sortedBy { it.name.substringBefore('.').toLongOrNull() ?: 0L }
    records?.dropLast(keep)?.forEach { it.delete() }
  }

  private fun stacks(): String = buildString {
    val mainThread = Looper.getMainLooper().thread
    append("Main thread, ").append(mainThread.state).append(":\n")
    appendFrames(mainThread.stackTrace, MAX_STACK_FRAMES)
    var others = 0
    for ((other, frames) in Thread.getAllStackTraces()) {
      if (other === mainThread || others >= MAX_OTHER_THREADS) continue
      if (frames.none { it.className.startsWith(APP_PACKAGE_PREFIX) }) continue
      others++
      append(other.name).append(", ").append(other.state).append(":\n")
      appendFrames(frames, MAX_OTHER_FRAMES)
    }
  }

  private fun StringBuilder.appendFrames(frames: Array<StackTraceElement>, max: Int) {
    frames.take(max).forEach { append("\tat ").append(it).append('\n') }
  }

  /** The stalls recorded in the last week, oldest first, or null if there are none. */
  @JvmStatic
  fun read(context: Context): String? {
    val cutoff = System.currentTimeMillis() - MAX_AGE_MS
    val records =
      dir(context.applicationContext).listFiles()?.filter { it.lastModified() >= cutoff }
        ?.sortedBy { it.name.substringBefore('.').toLongOrNull() ?: 0L }
        ?: return null
    return records
      .mapNotNull { runCatching { it.readText() }.getOrNull() }
      .filter { it.isNotBlank() }
      .takeIf { it.isNotEmpty() }
      ?.joinToString("\n")
  }

  /** Forgets the stalls, once a report has carried them. */
  @JvmStatic
  fun clear(context: Context) {
    dir(context.applicationContext).listFiles()?.forEach { it.delete() }
  }

  private fun dir(app: Context) = File(app.createDeviceProtectedStorageContext().filesDir, DIR_NAME)
}

/**
 * Decides when a post to the main thread has taken long enough to record. It is called on one
 * thread only.
 */
internal class StallDetector {
  private var pingedAt: Long? = null
  private var stagesRecorded = 0

  /**
   * Returns [PING] to post to the main thread, the time in milliseconds it has not responded for
   * when that should be recorded, or [NOTHING]. [canPing] is false while the screen is off.
   */
  fun tick(now: Long, canPing: Boolean): Long {
    val pinged = pingedAt
    if (pinged == null) {
      if (!canPing) return NOTHING
      pingedAt = now
      return PING
    }
    val stalled = now - pinged
    if (stagesRecorded < STAGES_MS.size && stalled >= STAGES_MS[stagesRecorded]) {
      stagesRecorded++
      return stalled
    }
    return NOTHING
  }

  /** The main thread ran the post. Returns how long it took if that was recorded, or null. */
  fun answered(now: Long): Long? {
    val pinged = pingedAt ?: return null
    val recorded = stagesRecorded > 0
    pingedAt = null
    stagesRecorded = 0
    return if (recorded) now - pinged else null
  }

  companion object {
    const val PING = -1L
    const val NOTHING = -2L
    /** How long the main thread has not responded for at each record of its stacks. */
    val STAGES_MS = longArrayOf(10_000, 30_000, 60_000)
  }
}
