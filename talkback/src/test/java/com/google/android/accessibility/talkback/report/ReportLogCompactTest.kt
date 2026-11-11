package com.google.android.accessibility.talkback.report

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReportLogCompactTest {

  @Test
  fun dropsActorsLinesAndShortensPipelineLines() {
    val long = "x".repeat(500)
    val log =
      "11-11 12:24:53.416  2407  2407 D talkback: Pipeline: execute() feedback=$long\n" +
        "11-11 12:24:53.417  2407  2407 D talkback: Actors: act() eventId=$long\n" +
        "11-11 12:24:53.418  2407  2407 D talkback: Other: kept\n"
    val lines = ProblemReportSender.compact(log).lines()
    assertEquals(3, lines.size)
    assertEquals(301, lines[0].length)
    assertEquals("11-11 12:24:53.418  2407  2407 D talkback: Other: kept", lines[1])
    assertEquals("", lines[2])
  }

  @Test
  fun leavesOtherLogsAlone() {
    val log = "a\nb"
    assertEquals(log, ProblemReportSender.compact(log))
  }
}
