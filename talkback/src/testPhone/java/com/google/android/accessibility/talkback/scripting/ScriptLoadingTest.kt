/*
 * Copyright 2026 Backtalk contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.talkback.scripting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScriptLoadingTest {
  private fun script(
    id: String,
    extra: String = "",
    enabled: Boolean = true,
    keepRunning: Boolean = false,
  ): InstalledScript =
    InstalledScript(
      ScriptManifest.parse(
        """{"id":"$id","name":"$id","author":"Me","minApiVersion":1,"testedApiVersion":1$extra}"""
      ),
      enabled = enabled,
      granted = emptySet(),
      revision = 1,
      keepRunning = keepRunning,
    )

  private val clock = script("clock", ""","keepLoaded":true""", keepRunning = true)
  private val clockSwitchOff = script("clock-off", ""","keepLoaded":true""")
  private val clockTurnedOff =
    script("clock-disabled", ""","keepLoaded":true""", enabled = false, keepRunning = true)
  // The switch was on while an earlier version of the script had keepLoaded.
  private val noLongerKept = script("plain", keepRunning = true)
  private val appScript = script("mail", ""","apps":"com.example.mail"""")
  private val onDemand =
    script(
      "lookup",
      ""","onDemand":true,"permissions":["input"],"commands":[{"id":"go","title":"Go"}]""",
    )
  private val all =
    listOf(clock, clockSwitchOff, clockTurnedOff, noLongerKept, appScript, onDemand)

  @Test
  fun onlyScriptsTheUserKeepsStayLoadedWithTheScreenOff() {
    assertTrue(ScriptLoading.keptWithScreenOff(clock))
    assertFalse(ScriptLoading.keptWithScreenOff(clockSwitchOff))
    assertFalse(ScriptLoading.keptWithScreenOff(clockTurnedOff))
    assertFalse(ScriptLoading.keptWithScreenOff(noLongerKept))
    val wanted = ScriptLoading.wanted(all, screenOn = false, demanded = setOf("lookup")) { true }
    assertEquals(listOf(clock), wanted)
  }

  @Test
  fun withTheScreenOnScriptsLoadAsBefore() {
    val wanted =
      ScriptLoading.wanted(all, screenOn = true, demanded = setOf("lookup")) {
        it.enabled && it.manifest.isGlobal
      }
    assertEquals(listOf(clock, clockSwitchOff, noLongerKept, onDemand), wanted)
    assertEquals(
      listOf(clock, clockSwitchOff, noLongerKept),
      ScriptLoading.wanted(all, screenOn = true, demanded = emptySet()) {
        it.enabled && it.manifest.isGlobal
      },
    )
  }

  @Test
  fun theEngineRunsWithTheScreenOffOnlyForKeptScripts() {
    assertFalse(ScriptLoading.engineNeeded(false, true, true, false))
    assertTrue(ScriptLoading.engineNeeded(false, false, false, true))
    assertTrue(ScriptLoading.engineNeeded(true, true, false, false))
    assertTrue(ScriptLoading.engineNeeded(true, false, true, false))
    assertFalse(ScriptLoading.engineNeeded(true, false, false, true))
  }
}
