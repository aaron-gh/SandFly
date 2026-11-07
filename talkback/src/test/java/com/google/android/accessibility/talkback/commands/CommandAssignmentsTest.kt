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

import com.google.android.accessibility.talkback.keyboard.KeyComboModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandAssignmentsTest {
  private val value = "BRAILLE_APP_NAME"

  private fun assigned(vararg saved: Pair<String, Any>) =
    CommandAssignments.isAssigned(mapOf(*saved), value)

  @Test
  fun gestureSavedAsTheValue_isAssigned() {
    assertTrue(assigned("pref_shortcut_3finger_2tap" to value))
  }

  @Test
  fun valueInsideJson_isAssigned() {
    assertTrue(assigned("pref_app_gestures" to """{"a":{"gestures":{"1":"BRAILLE_APP_NAME"}}}"""))
  }

  @Test
  fun valueInAStringSet_isAssigned() {
    assertTrue(assigned("pref_items" to setOf("HOME", value)))
  }

  @Test
  fun keyboardShortcutSavedUnderTheValue_isAssigned() {
    assertTrue(assigned("km|command_shortcut_BRAILLE_APP_NAME|key_combo_code" to 42L))
  }

  @Test
  fun unassignedKeyboardShortcut_isNotAssigned() {
    assertFalse(
      assigned(
        "km|command_shortcut_BRAILLE_APP_NAME|key_combo_code" to
          KeyComboModel.KEY_COMBO_CODE_UNASSIGNED.toLong()
      )
    )
  }

  @Test
  fun otherValuesAndLongerValues_areNotAssigned() {
    assertFalse(
      assigned(
        "pref_shortcut_3finger_2tap" to "WRITE_APP_NAME",
        "pref_app_gestures" to """{"a":{"gestures":{"1":"BRAILLE_APP_NAME_2"}}}""",
        "km|command_shortcut_BRAILLE_APP_NAMES|key_combo_code" to 42L,
        "pref_braille_app_name_seen" to true,
      )
    )
  }

  @Test
  fun nothingSaved_isNotAssigned() {
    assertFalse(assigned())
  }
}
