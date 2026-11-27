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

import android.content.Context
import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeyNamesTest {
  private val context: Context = RuntimeEnvironment.getApplication()

  @Test
  fun namesKeysFromStrings() {
    assertEquals("Space", KeyNames.of(context, KeyEvent.KEYCODE_SPACE))
    assertEquals("Period", KeyNames.of(context, KeyEvent.KEYCODE_PERIOD))
    assertEquals("Page Up", KeyNames.of(context, KeyEvent.KEYCODE_PAGE_UP))
    assertEquals("Insert", KeyNames.of(context, KeyEvent.KEYCODE_INSERT))
  }

  @Test
  fun namesNumpadDigits() {
    assertEquals("Numpad 0", KeyNames.of(context, KeyEvent.KEYCODE_NUMPAD_0))
    assertEquals("Numpad 5", KeyNames.of(context, KeyEvent.KEYCODE_NUMPAD_5))
    assertEquals("Numpad 9", KeyNames.of(context, KeyEvent.KEYCODE_NUMPAD_9))
  }

  @Test
  fun lettersDigitsAndFunctionKeysHaveNoName() {
    assertNull(KeyNames.of(context, KeyEvent.KEYCODE_A))
    assertNull(KeyNames.of(context, KeyEvent.KEYCODE_5))
    assertNull(KeyNames.of(context, KeyEvent.KEYCODE_F5))
  }

  @Test
  fun keysWithoutANameKeepAndroidsName() {
    assertEquals("Comma", KeyNames.orAndroidName(context, KeyEvent.KEYCODE_COMMA))
    assertEquals(
      KeyEvent.keyCodeToString(KeyEvent.KEYCODE_A).removePrefix("KEYCODE_").replace('_', ' '),
      KeyNames.orAndroidName(context, KeyEvent.KEYCODE_A),
    )
  }
}
