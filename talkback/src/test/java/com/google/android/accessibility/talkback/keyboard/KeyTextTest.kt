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

package com.google.android.accessibility.talkback.keyboard

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeyTextTest {

  @Test
  fun writesAndReadsShortcuts() {
    val shortcuts =
      listOf(
        KeyCombo(KeyEvent.META_SHIFT_ON, KeyEvent.KEYCODE_DPAD_RIGHT, true) to "SandFly+Shift+Right",
        KeyCombo(KeyEvent.META_CTRL_ON or KeyEvent.META_ALT_ON, KeyEvent.KEYCODE_H, false) to "Ctrl+Alt+H",
        KeyCombo(0, KeyEvent.KEYCODE_O, KeyEvent.KEYCODE_H, true) to "SandFly+O, H",
        KeyCombo(0, KeyEvent.KEYCODE_F5, false) to "F5",
        KeyCombo(0, KeyEvent.KEYCODE_FORWARD_DEL, true) to "SandFly+Delete",
        KeyCombo.sandFlyKeyPressedTwice() to "SandFly twice",
      )
    for ((keyCombo, text) in shortcuts) {
      assertEquals(text, KeyText.of(keyCombo))
      assertEquals(text, keyCombo, KeyText.parse(text))
    }
  }

  @Test
  fun readsOtherSpellings() {
    assertEquals(
      KeyCombo(KeyEvent.META_CTRL_ON, KeyEvent.KEYCODE_ESCAPE, true),
      KeyText.parse(" sandfly + control + esc "),
    )
    assertEquals(KeyCombo(0, KeyEvent.KEYCODE_NUMPAD_5, false), KeyText.parse("NUMPAD_5"))
    assertEquals(KeyCombo(KeyEvent.META_META_ON, KeyEvent.KEYCODE_A, false), KeyText.parse("Meta+a"))
  }

  @Test
  fun refusesWhatIsNoShortcut() {
    for (text in listOf("", "SandFly+", "SandFly+Foo", "Hyper+H", "Shift", "SandFly+H, ", "Ctrl+Shift")) {
      assertNull(text, KeyText.parse(text))
    }
    assertNull(KeyText.of(KeyCombo()))
  }
}
