package com.google.android.accessibility.talkback.report

import java.io.BufferedReader
import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReportGestureLogTest {

  private fun reader(text: String) = BufferedReader(StringReader(text))

  @Test
  fun lastLines_keepsTheLastLinesWithoutHeadings() {
    val text =
      "--------- beginning of main\n" + (1..10).joinToString("\n") { "D BacktalkGesture: $it" }
    val kept = ProblemReportSender.lastLines(reader(text), 3, 10_000)
    assertEquals("D BacktalkGesture: 8\nD BacktalkGesture: 9\nD BacktalkGesture: 10", kept)
  }

  @Test
  fun lastLines_keepsTheMostRecentCharacters() {
    val text = (1..100).joinToString("\n") { "line %03d".format(it) }
    val kept = ProblemReportSender.lastLines(reader(text), 1_000, 30)
    assertTrue(kept.length <= 30)
    assertEquals("line 098\nline 099\nline 100", kept)
  }

  @Test
  fun lastLines_cutsLongLines() {
    val kept = ProblemReportSender.lastLines(reader("x".repeat(5_000)), 10, 10_000)
    assertEquals(1_001, kept.length)
  }

  @Test
  fun joinLog_putsGesturesAfterTheLogAndShortensTheLog() {
    val log = "m".repeat(ProblemReport.MAX_LOG)
    val gestures = "g".repeat(1_000)
    val others = "\n\nLog of other:\nother"
    val joined = ProblemReportSender.joinLog("Log:\n", log, gestures, others)
    assertEquals(ProblemReport.MAX_LOG, joined.length)
    assertTrue(joined.startsWith("Log:\nmmm"))
    assertTrue(joined.endsWith("m\n\nGesture log:\n$gestures$others"))
  }

  @Test
  fun joinLog_leavesOutAnEmptyGestureSection() {
    assertEquals("Log:\nmain", ProblemReportSender.joinLog("Log:\n", "main", "", ""))
  }
}
