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

package com.google.android.accessibility.talkback.scripting

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScriptKeysTest {
  private fun rejects(text: String) {
    assertThrows(IllegalArgumentException::class.java) { ScriptInput.parseKeys(text) }
  }

  @Test
  fun keysAreTheBacktalkModifierOtherModifiersAndAKey() {
    assertEquals(ScriptInput.Keys(0, KeyEvent.KEYCODE_N), ScriptInput.parseKeys("backtalk+n"))
    assertEquals(
      ScriptInput.Keys(KeyEvent.META_SHIFT_ON, KeyEvent.KEYCODE_N),
      ScriptInput.parseKeys("Backtalk + Shift + N"),
    )
  }

  @Test
  fun keysHaveNamesForArrowsAndPunctuation() {
    assertEquals(KeyEvent.KEYCODE_DPAD_UP, ScriptInput.parseKeys("backtalk+up").keyCode)
    assertEquals(KeyEvent.KEYCODE_DEL, ScriptInput.parseKeys("backtalk+backspace").keyCode)
    assertEquals(KeyEvent.KEYCODE_SLASH, ScriptInput.parseKeys("backtalk+/").keyCode)
    assertEquals(KeyEvent.KEYCODE_F5, ScriptInput.parseKeys("backtalk+f5").keyCode)
  }

  @Test
  fun aSecondKeyFollowsAComma() {
    val keys = ScriptInput.parseKeys("backtalk+t, s")

    assertEquals(KeyEvent.KEYCODE_T, keys.keyCode)
    assertEquals(ScriptInput.Keys(0, KeyEvent.KEYCODE_S), keys.next)
    assertEquals(ScriptInput.Keys(0, KeyEvent.KEYCODE_T), keys.first)
  }

  @Test
  fun onlyTheFirstKeysIncludeTheBacktalkModifier() {
    rejects("shift+n")
    rejects("backtalk+t, backtalk+s")
  }

  @Test
  fun badKeysAreRejected() {
    rejects("backtalk+")
    rejects("backtalk+hyper+n")
    rejects("backtalk+notakey")
    rejects("backtalk+shift")
    rejects("backtalk+a, b, c")
  }

  @Test
  fun keysSurviveBeingStoredByTheirId() {
    for (text in listOf("backtalk+n", "backtalk+shift+n", "backtalk+t, shift+s")) {
      val keys = ScriptInput.parseKeys(text)

      assertEquals(keys, ScriptInput.keysFromId(keys.id))
    }
  }

  @Test
  fun aDamagedIdIsNotKeys() {
    assertNull(ScriptInput.keysFromId("nonsense"))
    assertNull(ScriptInput.keysFromId("1:2>x"))
  }
}
