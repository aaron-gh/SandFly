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

package com.google.android.accessibility.talkback.scripting

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** The options that a script sets for a list setting, and what becomes of them in an update. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScriptSettingOptionsTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private val store = ScriptStore.get(context)

  private fun install(version: String, setting: String) {
    val source =
      """
      local manifest = {
        id = 'options-test',
        name = 'Options test',
        version = '$version',
        author = 'Me',
        minApiVersion = 7,
        testedApiVersion = 7,
        settings = { $setting },
      }
      """
        .trimIndent()
    store.install(ScriptPackage.withMain(emptyMap(), source), granted = emptySet())
  }

  private fun options(): List<Pair<String, String>> {
    val script = store.find("options-test")!!
    return store.settingOptions(script, script.manifest.settings.single())
  }

  private val list =
    "{ key = 'pick', type = 'list', title = 'Pick', options = { { value = 'a', label = 'A' } } }"

  private val command =
    "{ key = 'pick', type = 'command', title = 'Pick', " +
      "options = { { value = 'time', label = 'Time' } } }"

  @After
  fun removeScript() {
    store.remove("options-test")
  }

  @Test
  fun aListShowsTheOptionsTheScriptSet() {
    install("1.0", list)
    store.setSettingOptions("options-test", "pick", listOf("b" to "B"))
    assertEquals(listOf("b" to "B"), options())
  }

  @Test
  fun optionsSetForAListDontApplyOnceItIsACommandSetting() {
    install("1.0", list)
    store.setSettingOptions("options-test", "pick", listOf("b" to "B"))
    install("2.0", command)
    assertEquals(listOf("time" to "Time"), options())
  }

  @Test
  fun optionsSetForAListDontComeBackWhenItIsAListAgain() {
    install("1.0", list)
    store.setSettingOptions("options-test", "pick", listOf("b" to "B"))
    install("2.0", command)
    install("3.0", list)
    assertEquals(listOf("a" to "A"), options())
  }
}
