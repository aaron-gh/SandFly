package com.google.android.accessibility.talkback.report

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiResponseReportTest {
  @Test
  fun keepsTheLastResponseAndIgnoresEmptyOnes() {
    AiResponseReport.record("A cat on a sofa.")
    AiResponseReport.record("  A dog in a park.  ")
    AiResponseReport.record("   ")
    AiResponseReport.record(null)
    assertEquals("A dog in a park.", AiResponseReport.lastResponse())
  }

  @Test
  fun leavesRoomInTheDescriptionForThePersonsWords() {
    AiResponseReport.record("x".repeat(ProblemReport.MAX_TEXT * 2))
    assertTrue(AiResponseReport.lastResponse()!!.length < ProblemReport.MAX_TEXT)
  }
}
