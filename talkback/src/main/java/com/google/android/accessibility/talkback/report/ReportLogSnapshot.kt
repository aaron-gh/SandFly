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

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * The logcat text of SandFly's processes at the time the report screens opened: the end of this
 * process's log, and the ends of the logs of the other processes.
 */
internal data class LogcatSnapshot(val main: String, val others: String)

/**
 * The one copy of SandFly's logcat text for the report being written, read when the report screens
 * open so that the report's form does not push the reporter's problem out of the log. The head
 * sections of a report are read at Send and are not part of it.
 *
 * A snapshot belongs to one visit to the report screens, and is kept in memory only. A visit starts
 * when a report screen is shown and none is open. It ends, and its snapshot is dropped as stale,
 * when:
 * - the activity of the report screens stops while the screen is on: the reporter went to another
 *   app or screen;
 * - that activity has been stopped with the screen off for longer than [SCREEN_OFF_LIMIT_MS];
 * - that activity is destroyed, or the list of reports is closed.
 *
 * A screen shown afterwards starts a new visit and reads a new snapshot, so that a draft opened in a
 * later visit does not get the log of an earlier one. Moving between the report screens and a
 * configuration change keep the visit. A sent report drops the snapshot and the visit goes on; the
 * next form opened in it reads a new one, unless the reporter turned the log off in this visit.
 *
 * A null read means that the log is read at Send, as when the activity is in multi-window mode: the
 * reporter can reproduce a problem in the other pane while the activity stays resumed.
 *
 * All methods except [await] run on the main thread.
 */
internal class ReportLogSnapshot(
  private val submit: (Callable<LogcatSnapshot?>) -> Future<LogcatSnapshot?>,
  private val elapsedMs: () -> Long,
  private val postDelayed: (Runnable, Long) -> Unit,
  private val removeCallbacks: (Runnable) -> Unit,
  private val waitMs: Long = WAIT_MS,
) {
  private var visiting = false
  private var activity = 0
  private var snapshot: Future<LogcatSnapshot?>? = null
  private var stoppedAt: Long? = null
  private var logTurnedOff = false
  private val endAfterScreenOffLimit = Runnable { endVisit() }

  /** A report screen of [activity] is on screen. Starts a visit if none is open. */
  @MainThread
  fun screenShown(activity: Int, read: Callable<LogcatSnapshot?>?) {
    leaveStoppedState()
    this.activity = activity
    if (visiting) return
    visiting = true
    snapshot = read?.let { submit(it) }
  }

  /** Reads a snapshot for the open visit if it has none, as after a report was sent. */
  @MainThread
  fun readIfMissing(read: Callable<LogcatSnapshot?>?) {
    if (read != null && visiting && snapshot == null && !logTurnedOff) snapshot = submit(read)
  }

  /** The reporter turned the log off: drops the snapshot, and no form of this visit reads one. */
  @MainThread
  fun logTurnedOff() {
    logTurnedOff = true
    discard()
  }

  /** The reporter turned the log on: reads a snapshot if the visit has none. */
  @MainThread
  fun logTurnedOn(read: Callable<LogcatSnapshot?>?) {
    logTurnedOff = false
    readIfMissing(read)
  }

  /** The snapshot of this visit, or null. Pass it to [await]. */
  @MainThread fun current(): Future<LogcatSnapshot?>? = snapshot

  /** Drops the snapshot, which a report without the log has no use for. */
  @MainThread
  fun discard() {
    snapshot?.cancel(true)
    snapshot = null
  }

  /** Drops [sent] after its report was sent, if it is still the snapshot of this visit. */
  @MainThread
  fun clearSent(sent: Future<LogcatSnapshot?>?) {
    if (sent != null && sent === snapshot) snapshot = null
  }

  @MainThread
  fun activityStopped(activity: Int, screenOn: Boolean) {
    if (!visiting || activity != this.activity) return
    if (screenOn) {
      endVisit()
    } else {
      stoppedAt = elapsedMs()
      postDelayed(endAfterScreenOffLimit, SCREEN_OFF_LIMIT_MS)
    }
  }

  @MainThread
  fun activityStarted(activity: Int) {
    if (activity == this.activity) leaveStoppedState()
  }

  /** Ends the visit if the activity was stopped for too long, and otherwise resumes it. */
  private fun leaveStoppedState() {
    val stopped = stoppedAt ?: return
    if (elapsedMs() - stopped > SCREEN_OFF_LIMIT_MS) {
      endVisit()
    } else {
      stoppedAt = null
      removeCallbacks(endAfterScreenOffLimit)
    }
  }

  /** Ends the visit of [activity], if it is the activity of the open visit. */
  @MainThread
  fun endVisit(activity: Int) {
    if (activity == this.activity) endVisit()
  }

  @MainThread
  fun endVisit() {
    visiting = false
    stoppedAt = null
    logTurnedOff = false
    removeCallbacks(endAfterScreenOffLimit)
    discard()
  }

  /**
   * The contents of [snapshot], or null if the log has to be read at Send instead: there is no
   * snapshot, its read failed or held no main log, or it did not finish within [waitMs].
   */
  @WorkerThread
  fun await(snapshot: Future<LogcatSnapshot?>?): LogcatSnapshot? {
    if (snapshot == null) return null
    return try {
      snapshot.get(waitMs, TimeUnit.MILLISECONDS)?.takeIf { it.main.isNotEmpty() }
    } catch (e: ExecutionException) {
      null
    } catch (e: TimeoutException) {
      null
    } catch (e: CancellationException) {
      null
    } catch (e: InterruptedException) {
      Thread.currentThread().interrupt()
      null
    }
  }

  companion object {
    /** How long a stopped activity with the screen off keeps its visit. */
    const val SCREEN_OFF_LIMIT_MS = 10L * 60 * 1000

    /** How long Send waits for a read still running. */
    const val WAIT_MS = 15_000L
  }
}
