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
 * The one copy of SandFly's log for the report being written, read when the report screens open so
 * that the report's form does not push the reporter's problem out of the log.
 *
 * A snapshot belongs to one visit to the report screens, and is kept in memory only. A visit starts
 * when a report screen is shown and none is open. It ends, and its snapshot is dropped as stale,
 * when:
 * - the activity of the report screens stops while the screen is on: the reporter went to another
 *   app or screen;
 * - that activity has been stopped with the screen off for longer than [SCREEN_OFF_LIMIT_MS]
 *   when it starts again;
 * - that activity is destroyed, or the list of reports is closed.
 *
 * A screen shown afterwards starts a new visit and reads a new snapshot, so that a draft opened in a
 * later visit does not get the log of an earlier one. Moving between the report screens and a
 * configuration change keep the visit. A sent report drops the snapshot and the visit goes on; the
 * next form opened in it reads a new one.
 *
 * All methods except [await] run on the main thread.
 */
internal class ReportLogSnapshot(
  private val submit: (Callable<String?>) -> Future<String?>,
  private val elapsedMs: () -> Long,
  private val waitMs: Long = WAIT_MS,
) {
  private var visiting = false
  private var activity = 0
  private var snapshot: Future<String?>? = null
  private var stoppedAt: Long? = null

  /** A report screen of [activity] is on screen. Starts a visit if none is open. */
  @MainThread
  fun screenShown(activity: Int, read: Callable<String?>) {
    this.activity = activity
    stoppedAt = null
    if (visiting) return
    visiting = true
    snapshot = submit(read)
  }

  /** Reads a snapshot for the open visit if it has none, as after a report was sent. */
  @MainThread
  fun readIfMissing(read: Callable<String?>) {
    if (visiting && snapshot == null) snapshot = submit(read)
  }

  /** The snapshot of this visit, or null. Pass it to [await]. */
  @MainThread fun current(): Future<String?>? = snapshot

  /** Drops the snapshot, which a report without the log has no use for. */
  @MainThread
  fun discard() {
    snapshot?.cancel(true)
    snapshot = null
  }

  /** Drops [sent] after its report was sent, if it is still the snapshot of this visit. */
  @MainThread
  fun clearSent(sent: Future<String?>?) {
    if (sent != null && sent === snapshot) snapshot = null
  }

  @MainThread
  fun activityStopped(activity: Int, screenOn: Boolean) {
    if (!visiting || activity != this.activity) return
    if (screenOn) endVisit() else stoppedAt = elapsedMs()
  }

  @MainThread
  fun activityStarted(activity: Int) {
    if (activity != this.activity) return
    val stopped = stoppedAt ?: return
    if (elapsedMs() - stopped > SCREEN_OFF_LIMIT_MS) endVisit() else stoppedAt = null
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
    discard()
  }

  /**
   * The text of [snapshot], or null if the log has to be read at Send instead: there is no
   * snapshot, its read failed or was empty, or it did not finish within [waitMs]. An empty log is
   * never returned.
   */
  @WorkerThread
  fun await(snapshot: Future<String?>?): String? {
    if (snapshot == null) return null
    return try {
      snapshot.get(waitMs, TimeUnit.MILLISECONDS)?.takeIf { it.isNotEmpty() }
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
