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

package com.google.android.accessibility.talkback.gesture

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks that the "Rotor turn per step" choices say the number of degrees they store. */
class RotorStepSettingTest {
  private val keys = File("src/main/res/values/donottranslate.xml").readText()
  private val strings = File("src/main/res/values/strings.xml").readText()

  private fun array(name: String): List<String> {
    val body =
      Regex("""<string-array name="$name">(.*?)</string-array>""", RegexOption.DOT_MATCHES_ALL)
        .find(keys)!!
        .groupValues[1]
    return Regex("""<item>(.*?)</item>""").findAll(body).map { it.groupValues[1] }.toList()
  }

  private fun string(file: String, name: String): String =
    Regex("""<string name="$name"[^>]*>(.*?)</string>""").find(file)!!.groupValues[1]

  private val values = array("pref_rotor_step_degrees_values")
  private val labels =
    array("pref_rotor_step_degrees_entries").map { string(strings, it.removePrefix("@string/")) }

  @Test
  fun eachLabelSaysItsValue() {
    assertEquals(values.size, labels.size)
    for ((value, label) in values.zip(labels)) {
      assertTrue("$label should start with $value degrees", label.startsWith("$value degrees"))
    }
  }

  @Test
  fun onlyTheDefaultIsMarkedDefault() {
    val default = string(keys, "pref_rotor_step_degrees_default")
    assertEquals(DEFAULT_DEGREES, default)
    for ((value, label) in values.zip(labels)) {
      assertEquals(label, value == default, label.endsWith("(default)"))
    }
  }

  @Test
  fun valuesGrowInSize() {
    val degrees = values.map { it.toInt() }
    assertEquals(degrees.sorted().distinct(), degrees)
  }

  private companion object {
    // The step the rotor used before the setting existed, in TwoFingerRotationTracker.
    const val DEFAULT_DEGREES = "30"
  }
}
