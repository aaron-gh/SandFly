package com.google.android.accessibility.talkback.report

import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportLogSnapshotTest {
  private val reads = mutableListOf<FutureTask<String?>>()
  private var now = 0L
  private val snapshots =
    ReportLogSnapshot(
      submit = { task -> FutureTask(task).also { reads += it } },
      elapsedMs = { now },
      waitMs = 1,
    )
  private val read = Callable<String?> { "log" }

  private fun finishedVisit(activity: Int = 1) {
    snapshots.screenShown(activity, read)
    reads.last().run()
  }

  @Test
  fun aVisitReadsOnce() {
    snapshots.screenShown(1, read)
    snapshots.screenShown(1, read)
    assertEquals(1, reads.size)
  }

  @Test
  fun aFinishedSnapshotIsUsed() {
    finishedVisit()
    assertEquals("log", snapshots.await(snapshots.current()))
  }

  @Test
  fun aSendWaitsForARunningRead() {
    val slow =
      ReportLogSnapshot(
        submit = { task -> FutureTask(task).also { reads += it } },
        elapsedMs = { now },
        waitMs = 5_000,
      )
    slow.screenShown(1, read)
    val waiting = slow.current()
    val task = reads.last()
    Thread {
        Thread.sleep(50)
        task.run()
      }
      .start()
    assertEquals("log", slow.await(waiting))
  }

  @Test
  fun aSendWithoutASnapshotReadsAtSend() {
    assertNull(snapshots.await(snapshots.current()))
  }

  @Test
  fun aReadThatDoesNotFinishFallsBackToSend() {
    snapshots.screenShown(1, read)
    assertNull(snapshots.await(snapshots.current()))
  }

  @Test
  fun aFailedReadFallsBackToSend() {
    snapshots.screenShown(1, Callable { throw IllegalStateException() })
    reads.last().run()
    assertNull(snapshots.await(snapshots.current()))
  }

  @Test
  fun anEmptyReadFallsBackToSend() {
    snapshots.screenShown(1, Callable { "" })
    reads.last().run()
    assertNull(snapshots.await(snapshots.current()))
  }

  @Test
  fun aReadCancelledDuringASendFallsBackToSend() {
    snapshots.screenShown(1, read)
    val sending = snapshots.current()
    snapshots.endVisit()
    assertNull(snapshots.await(sending))
  }

  @Test
  fun leavingWithTheScreenOnEndsTheVisit() {
    finishedVisit()
    snapshots.activityStopped(1, screenOn = true)
    assertNull(snapshots.current())
    finishedVisit()
    assertEquals(2, reads.size)
  }

  @Test
  fun aStaleSnapshotIsNotTheNewVisitsSnapshot() {
    finishedVisit()
    val stale = snapshots.current()
    snapshots.activityStopped(1, screenOn = true)
    snapshots.screenShown(1, read)
    assertTrue(stale !== snapshots.current())
  }

  @Test
  fun leavingCancelsARunningRead() {
    snapshots.screenShown(1, read)
    snapshots.activityStopped(1, screenOn = true)
    assertTrue(reads[0].isCancelled)
  }

  @Test
  fun aShortTimeWithTheScreenOffKeepsTheSnapshot() {
    finishedVisit()
    val before = snapshots.current()
    snapshots.activityStopped(1, screenOn = false)
    now += ReportLogSnapshot.SCREEN_OFF_LIMIT_MS
    snapshots.activityStarted(1)
    snapshots.screenShown(1, read)
    assertTrue(before === snapshots.current())
    assertEquals(1, reads.size)
  }

  @Test
  fun aLongTimeWithTheScreenOffReplacesTheSnapshot() {
    finishedVisit()
    snapshots.activityStopped(1, screenOn = false)
    now += ReportLogSnapshot.SCREEN_OFF_LIMIT_MS + 1
    snapshots.activityStarted(1)
    assertNull(snapshots.current())
    snapshots.screenShown(1, read)
    assertEquals(2, reads.size)
  }

  @Test
  fun anEarlierActivityStoppingKeepsTheVisit() {
    finishedVisit(activity = 1)
    snapshots.screenShown(2, read)
    snapshots.activityStopped(1, screenOn = true)
    snapshots.endVisit(1)
    assertNotNull(snapshots.current())
    assertEquals(1, reads.size)
  }

  @Test
  fun theActivityOfTheVisitBeingDestroyedEndsIt() {
    finishedVisit()
    snapshots.endVisit(1)
    assertNull(snapshots.current())
  }

  @Test
  fun aSentSnapshotIsDroppedAndTheVisitGoesOn() {
    finishedVisit()
    snapshots.clearSent(snapshots.current())
    assertNull(snapshots.current())
    snapshots.screenShown(1, read)
    assertEquals(1, reads.size)
    snapshots.readIfMissing(read)
    assertEquals(2, reads.size)
  }

  @Test
  fun anUnsentReportKeepsTheSnapshotForAnotherTry() {
    finishedVisit()
    val before = snapshots.current()
    snapshots.readIfMissing(read)
    assertTrue(before === snapshots.current())
    assertEquals(1, reads.size)
  }

  @Test
  fun anOlderSentSnapshotDoesNotDropTheCurrentOne() {
    finishedVisit()
    val older = snapshots.current()
    snapshots.activityStopped(1, screenOn = true)
    finishedVisit()
    val current = snapshots.current()
    snapshots.clearSent(older)
    assertTrue(current === snapshots.current())
  }

  @Test
  fun aReportWithoutTheLogDropsTheSnapshot() {
    snapshots.screenShown(1, read)
    snapshots.discard()
    assertNull(snapshots.current())
    assertTrue(reads[0].isCancelled)
  }

  @Test
  fun noReadStartsOutsideAVisit() {
    snapshots.readIfMissing(read)
    assertTrue(reads.isEmpty())
  }
}
