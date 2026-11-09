package com.google.android.accessibility.talkback.report

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StallLogTest {
  private val detector = StallDetector()

  @Test
  fun pingsAndNothingWhileTheMainThreadAnswers() {
    assertEquals(StallDetector.PING, detector.tick(0, true))
    assertEquals(StallDetector.NOTHING, detector.tick(4_000, true))
    assertNull(detector.answered(4_500))
    assertEquals(StallDetector.PING, detector.tick(5_000, true))
  }

  @Test
  fun noPingWhileTheScreenIsOff() {
    assertEquals(StallDetector.NOTHING, detector.tick(0, false))
    assertEquals(StallDetector.PING, detector.tick(5_000, true))
  }

  @Test
  fun recordsEachStageOnceAndReportsTheStallWhenAnswered() {
    detector.tick(0, true)
    assertEquals(StallDetector.NOTHING, detector.tick(9_000, true))
    assertEquals(10_000L, detector.tick(10_000, true))
    assertEquals(StallDetector.NOTHING, detector.tick(15_000, true))
    assertEquals(30_000L, detector.tick(30_000, true))
    assertEquals(60_000L, detector.tick(60_000, true))
    assertEquals(StallDetector.NOTHING, detector.tick(90_000, true))
    assertEquals(95_000L, detector.answered(95_000))
    assertEquals(StallDetector.PING, detector.tick(100_000, true))
  }

  @Test
  fun aStallGoesOnBeingRecordedAfterTheScreenTurnsOff() {
    detector.tick(0, true)
    assertEquals(10_000L, detector.tick(10_000, false))
  }

  @Test
  fun pruneKeepsTheNewestRecords() {
    val dir = Files.createTempDirectory("stalls").toFile()
    listOf(100L, 300L, 200L, 400L).forEach { File(dir, "$it.txt").writeText("x") }
    StallLog.prune(dir, 2)
    assertEquals(setOf("300.txt", "400.txt"), dir.list()!!.toSet())
    dir.deleteRecursively()
  }
}
