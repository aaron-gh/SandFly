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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
  fun keysAreTheSandFlyModifierOtherModifiersAndAKey() {
    assertEquals(ScriptInput.Keys(0, KeyEvent.KEYCODE_N), ScriptInput.parseKeys("sf+n"))
    assertEquals(
      ScriptInput.Keys(KeyEvent.META_SHIFT_ON, KeyEvent.KEYCODE_N),
      ScriptInput.parseKeys("SF + Shift + N"),
    )
  }

  @Test
  fun keysHaveNamesForArrowsAndPunctuation() {
    assertEquals(KeyEvent.KEYCODE_DPAD_UP, ScriptInput.parseKeys("sf+up").keyCode)
    assertEquals(KeyEvent.KEYCODE_DEL, ScriptInput.parseKeys("sf+backspace").keyCode)
    assertEquals(KeyEvent.KEYCODE_SLASH, ScriptInput.parseKeys("sf+/").keyCode)
    assertEquals(KeyEvent.KEYCODE_F5, ScriptInput.parseKeys("sf+f5").keyCode)
  }

  @Test
  fun aSecondKeyFollowsAComma() {
    val keys = ScriptInput.parseKeys("sf+t, s")

    assertEquals(KeyEvent.KEYCODE_T, keys.keyCode)
    assertEquals(ScriptInput.Keys(0, KeyEvent.KEYCODE_S), keys.next)
    assertEquals(ScriptInput.Keys(0, KeyEvent.KEYCODE_T), keys.first)
  }

  @Test
  fun onlyTheFirstKeysIncludeTheSandFlyModifier() {
    rejects("sf+t, sf+s")
    rejects("t, sf+s")
  }

  @Test
  fun keysCanLeaveOutTheSandFlyModifier() {
    assertEquals(ScriptInput.Keys(0, KeyEvent.KEYCODE_K, plain = true), ScriptInput.parseKeys("k"))
    assertEquals(
      ScriptInput.Keys(KeyEvent.META_SHIFT_ON, KeyEvent.KEYCODE_N, plain = true),
      ScriptInput.parseKeys("shift+n"),
    )
    assertEquals(
      ScriptInput.Keys(KeyEvent.META_SHIFT_ON, KeyEvent.KEYCODE_PERIOD, plain = true),
      ScriptInput.parseKeys("shift+."),
    )
    assertEquals(KeyEvent.KEYCODE_SPACE, ScriptInput.parseKeys("space").keyCode)
    assertEquals(KeyEvent.KEYCODE_DPAD_LEFT, ScriptInput.parseKeys("left").keyCode)
    assertEquals(KeyEvent.KEYCODE_1, ScriptInput.parseKeys("1").keyCode)
    assertFalse(ScriptInput.parseKeys("sf+k").plain)
  }

  @Test
  fun aSecondKeyFollowsKeysWithoutTheSandFlyModifier() {
    val keys = ScriptInput.parseKeys("t, s")

    assertEquals(ScriptInput.Keys(0, KeyEvent.KEYCODE_T, plain = true), keys.first)
    assertEquals(ScriptInput.Keys(0, KeyEvent.KEYCODE_S), keys.next)
  }

  @Test
  fun keysWithAndWithoutTheSandFlyModifierDiffer() {
    val plain = ScriptInput.parseKeys("shift+n")
    val withModifier = ScriptInput.parseKeys("sf+shift+n")

    assertNotEquals(withModifier, plain)
    assertNotEquals(withModifier.id, plain.id)
    assertNotEquals(ScriptInput.keysBinding(withModifier), ScriptInput.keysBinding(plain))
    assertNotEquals(ScriptInput.parseKeys("sf+t, s").id, ScriptInput.parseKeys("t, s").id)
  }

  @Test
  fun aModifierAloneIsNotKeys() {
    rejects("shift")
    rejects("ctrl+shift")
    rejects("shift_left")
    rejects("sf")
  }

  @Test
  fun badKeysAreRejected() {
    rejects("sf+")
    rejects("sf+hyper+n")
    rejects("sf+notakey")
    rejects("sf+shift")
    rejects("sf+a, b, c")
  }

  @Test
  fun keysSurviveBeingStoredByTheirId() {
    for (text in listOf("sf+n", "sf+shift+n", "sf+t, shift+s", "k", "shift+n", "t, shift+s")) {
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
