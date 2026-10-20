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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProblemReportTest {
  private val bug =
    ProblemReport(
      type = "Incorrect behavior",
      area = "Speech",
      title = " Focus skips a row ",
      app = " ",
      description = "It skips.\n",
      steps = "1. Open\n2. Swipe",
      email = "",
      version = "SandFly 0.1.2, build 790",
      device = listOf("Android 16 (API 36)"),
      log = null,
    )

  @Test
  fun theJsonHasTheFilledFieldsTrimmed() {
    val json = bug.toJson()
    assertEquals(
      setOf("type", "area", "title", "description", "steps", "version", "device"),
      json.keys().asSequence().toSet(),
    )
    assertEquals("Focus skips a row", json.getString("title"))
    assertEquals("It skips.", json.getString("description"))
    assertEquals("Android 16 (API 36)", json.getJSONArray("device").getString(0))
  }

  @Test
  fun aSuggestionHasNoSteps() {
    assertFalse(bug.copy(type = ProblemReport.SUGGESTION).toJson().has("steps"))
  }

  @Test
  fun aLongLogKeepsItsEnd() {
    val log = "a".repeat(ProblemReport.MAX_LOG) + "end"
    val sent = bug.copy(log = log).toJson().getString("log")
    assertEquals(ProblemReport.MAX_LOG, sent.length)
    assertEquals("end", sent.takeLast(3))
  }
}
