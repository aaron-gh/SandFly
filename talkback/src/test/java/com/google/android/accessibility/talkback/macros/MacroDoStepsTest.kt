/*
 * Copyright 2026 SandFly contributors
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

package com.google.android.accessibility.talkback.macros

import android.content.Context
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MacroDoStepsTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private val prefs = SharedPreferencesUtils.getSharedPreferences(context)

  @Test
  fun doStepsAndValueTests_surviveJson() {
    val steps =
      listOf(
        Macros.Do(Macros.DoKind.PLAY_SOUND, "click"),
        Macros.Do(Macros.DoKind.VIBRATE, number = 300),
        Macros.Do(Macros.DoKind.SET_VOLUME, name = "media", number = 40),
        Macros.Do(Macros.DoKind.FETCH, "https://example.com/{page}", "body"),
        Macros.If(Macros.Condition(listOf(Macros.Clause(Macros.ValueIs("body", "^ok", regex = true)), Macros.Clause(Macros.ValueIs("mode", ""), not = true)))),
        Macros.EndIf,
      )
    val macro = Macros.Macro("m", "M", steps)

    assertEquals(macro, Macros.macroFromJson(Macros.macroToJson(macro)))
  }

  @Test
  fun badValueNames_areDropped() {
    val json = """[{"id":"m","name":"M","steps":[{"type":"do","do":"set_value","name":"bad name","text":"x"},{"type":"do","do":"nope"}]}]"""
    assertEquals(emptyList<Macros.Step>(), Macros.macroFromJson(json)!!.steps)
  }

  @Test
  fun fill_putsValuesInAndLeavesUnknownNames() {
    assertEquals(
      "Hello Ana, {missing} \$1",
      Macros.fill("Hello {name}, {missing} {cost}", mapOf("name" to "Ana", "cost" to "\$1")),
    )
  }

  @Test
  fun values_areKeptPerScope() {
    Macros.setValue(prefs, "script:a", "count", "3")
    Macros.setValue(prefs, Macros.USER_SCOPE, "count", "9")

    assertEquals(mapOf("count" to "3"), Macros.values(prefs, Macros.valueScope("script:a:macros/c1.json")))
    assertEquals(mapOf("count" to "9"), Macros.values(prefs, Macros.valueScope("plain-id")))
    Macros.setValue(prefs, "script:a", "count", null)
    assertTrue(Macros.values(prefs, "script:a").isEmpty())
    Macros.clearValues(prefs, Macros.USER_SCOPE)
    assertNull(Macros.values(prefs, Macros.USER_SCOPE)["count"])
  }

  @Test
  fun describe_saysWhatDoStepsDo() {
    assertEquals("Vibrate 300 ms", Macros.describe(context, Macros.Do(Macros.DoKind.VIBRATE, number = 300)))
    assertEquals("Set value: {item}, into last", Macros.describe(context, Macros.Do(Macros.DoKind.SET_VALUE, "{item}", "last")))
  }
}
