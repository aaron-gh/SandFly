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

package com.google.android.accessibility.talkback.commands

import android.content.Context
import androidx.core.content.edit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RemovedCommandsTest {
  private val prefs =
    RuntimeEnvironment.getApplication().getSharedPreferences("removed_commands", Context.MODE_PRIVATE)

  @Test
  fun movesGestureValuesJsonAndShortcutKeys() {
    prefs.edit {
      putString("pref_shortcut_2finger_2tap", "MEDIA_CONTROL_OR_VOICE_INPUT")
      putString("pref_shortcut_3finger_2tap", "VOICE_INPUT")
      putString("pref_shortcut_up", "MEDIA_CONTROL")
      putString("app_gestures", """{"a":{"gestures":{"1":"VOICE_INPUT","2":"MEDIA_CONTROL_OR_VOICE_INPUT"}}}""")
      putString("note", "VOICE_INPUT is \"VOICE_INPUT\"")
      putLong("km|command_shortcut_VOICE_INPUT|key_combo_code", 42L)
      putBoolean("km|command_shortcut_VOICE_INPUT|trigger_modifier_used", true)
      putLong("km|command_shortcut_MEDIA_CONTROL_OR_VOICE_INPUT|key_combo_code", 7L)
      putLong("km|command_shortcut_DICTATE|key_combo_code", 1L)
    }
    RemovedCommands.migrate(prefs)

    assertEquals("DICTATE_OR_MEDIA_CONTROL", prefs.getString("pref_shortcut_2finger_2tap", null))
    assertEquals("DICTATE", prefs.getString("pref_shortcut_3finger_2tap", null))
    assertEquals("MEDIA_CONTROL", prefs.getString("pref_shortcut_up", null))
    assertEquals(
      """{"a":{"gestures":{"1":"DICTATE","2":"DICTATE_OR_MEDIA_CONTROL"}}}""",
      prefs.getString("app_gestures", null),
    )
    // Only whole values and JSON are changed.
    assertEquals("VOICE_INPUT is \"VOICE_INPUT\"", prefs.getString("note", null))
    // A shortcut already saved for the replacement is kept.
    assertEquals(1L, prefs.getLong("km|command_shortcut_DICTATE|key_combo_code", 0L))
    assertFalse(prefs.contains("km|command_shortcut_VOICE_INPUT|key_combo_code"))
    assertEquals(true, prefs.getBoolean("km|command_shortcut_DICTATE|trigger_modifier_used", false))
    assertEquals(
      7L,
      prefs.getLong("km|command_shortcut_DICTATE_OR_MEDIA_CONTROL|key_combo_code", 0L),
    )
    assertFalse(prefs.contains("km|command_shortcut_MEDIA_CONTROL_OR_VOICE_INPUT|key_combo_code"))
  }

  @Test
  fun runsOnce() {
    RemovedCommands.migrate(prefs)
    prefs.edit { putString("pref_shortcut_2finger_2tap", "VOICE_INPUT") }
    RemovedCommands.migrate(prefs)
    assertEquals("VOICE_INPUT", prefs.getString("pref_shortcut_2finger_2tap", null))
  }

  @Test
  fun runsAgainWhenForced() {
    RemovedCommands.migrate(prefs)
    prefs.edit { putString("pref_shortcut_2finger_2tap", "VOICE_INPUT") }
    RemovedCommands.migrate(prefs, force = true)
    assertEquals("DICTATE", prefs.getString("pref_shortcut_2finger_2tap", null))
  }

  @Test
  fun leavesOtherShortcutKeys() {
    assertNull(RemovedCommands.replaceShortcutKey("km|command_shortcut_VOICE_INPUTS|key_combo_code"))
    assertNull(RemovedCommands.replaceShortcutKey("km|command_shortcut_DICTATE|key_combo_code"))
  }
}
