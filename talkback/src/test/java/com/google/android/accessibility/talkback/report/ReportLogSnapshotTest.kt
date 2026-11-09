package com.google.android.accessibility.talkback.report

import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportLogSnapshotTest {
  private val reads = mutableListOf<FutureTask<LogcatSnapshot?>>()
  private val posted = mutableListOf<Runnable>()
  private var postedDelay = 0L
  private var now = 0L

  private fun newSnapshots(waitMs: Long) =
    ReportLogSnapshot(
      submit = { task -> FutureTask(task).also { reads += it } },
      elapsedMs = { now },
      postDelayed = { task, delayMs ->
        posted += task
        postedDelay = delayMs
      },
      removeCallbacks = { task -> posted -= task },
      waitMs = waitMs,
    )

  private val snapshots = newSnapshots(waitMs = 1)
  private val logcat = LogcatSnapshot("log", "others")
  private val read = Callable<LogcatSnapshot?> { logcat }

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
    assertEquals(logcat, snapshots.await(snapshots.current()))
  }

  @Test
  fun aSendWaitsForARunningRead() {
    val slow = newSnapshots(waitMs = 5_000)
    slow.screenShown(1, read)
    val waiting = slow.current()
    val task = reads.last()
    Thread {
        Thread.sleep(50)
        task.run()
      }
      .start()
    assertEquals(logcat, slow.await(waiting))
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
  fun aReadWithoutAMainLogFallsBackToSend() {
    snapshots.screenShown(1, Callable { LogcatSnapshot("", "others") })
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
  fun aNewInstancePastTheLimitReplacesTheSnapshot() {
    finishedVisit(activity = 1)
    val stale = snapshots.current()
    snapshots.activityStopped(1, screenOn = false)
    now += ReportLogSnapshot.SCREEN_OFF_LIMIT_MS + 1
    snapshots.screenShown(2, read)
    assertEquals(2, reads.size)
    assertTrue(stale !== snapshots.current())
  }

  @Test
  fun aNewInstanceWithinTheLimitKeepsTheSnapshot() {
    finishedVisit(activity = 1)
    val before = snapshots.current()
    snapshots.activityStopped(1, screenOn = false)
    now += ReportLogSnapshot.SCREEN_OFF_LIMIT_MS
    snapshots.screenShown(2, read)
    assertEquals(1, reads.size)
    assertTrue(before === snapshots.current())
    assertTrue(posted.isEmpty())
  }

  @Test
  fun theScreenOffLimitDropsTheSnapshotWithoutARestart() {
    finishedVisit()
    snapshots.activityStopped(1, screenOn = false)
    assertEquals(ReportLogSnapshot.SCREEN_OFF_LIMIT_MS, postedDelay)
    posted.single().run()
    assertNull(snapshots.current())
    snapshots.screenShown(1, read)
    assertEquals(2, reads.size)
  }

  @Test
  fun startingAgainWithinTheLimitRemovesTheDelayedDrop() {
    finishedVisit()
    snapshots.activityStopped(1, screenOn = false)
    snapshots.activityStarted(1)
    assertTrue(posted.isEmpty())
  }

  @Test
  fun endingTheVisitRemovesTheDelayedDrop() {
    finishedVisit()
    snapshots.activityStopped(1, screenOn = false)
    snapshots.endVisit(1)
    assertTrue(posted.isEmpty())
  }

  @Test
  fun aVisitInMultiWindowModeReadsNothing() {
    snapshots.screenShown(1, null)
    snapshots.readIfMissing(null)
    snapshots.logTurnedOn(null)
    assertTrue(reads.isEmpty())
    assertNull(snapshots.current())
  }

  @Test
  fun afterTheLogIsTurnedOffALaterFormDoesNotRead() {
    finishedVisit()
    snapshots.logTurnedOff()
    assertNull(snapshots.current())
    snapshots.readIfMissing(read)
    assertEquals(1, reads.size)
  }

  @Test
  fun turningTheLogOnAgainReads() {
    finishedVisit()
    snapshots.logTurnedOff()
    snapshots.logTurnedOn(read)
    assertEquals(2, reads.size)
  }

  @Test
  fun aNewVisitReadsAfterTheLogWasTurnedOff() {
    finishedVisit()
    snapshots.logTurnedOff()
    snapshots.activityStopped(1, screenOn = true)
    snapshots.screenShown(1, read)
    assertEquals(2, reads.size)
    snapshots.clearSent(snapshots.current())
    snapshots.readIfMissing(read)
    assertEquals(3, reads.size)
  }

  @Test
  fun aReportWithTheLogOffDoesNotStopALaterFormReading() {
    finishedVisit()
    snapshots.discard()
    snapshots.readIfMissing(read)
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
