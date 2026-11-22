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
import android.graphics.Rect
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ButtonCommandsTest {
  private val context: Context = RuntimeEnvironment.getApplication()

  @Test
  fun builtInLabelsMatchIgnoringCaseSpacesAndTrailingPunctuation() {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val confirm = ButtonCommands.words(context, prefs, ButtonCommands.Kind.CONFIRM)
    val cancel = ButtonCommands.words(context, prefs, ButtonCommands.Kind.CANCEL)
    assertTrue(ButtonCommands.matches("OK", confirm))
    assertTrue(ButtonCommands.matches(" ok ", confirm))
    assertTrue(ButtonCommands.matches("Continue…", confirm))
    assertTrue(ButtonCommands.matches("Next >", confirm))
    assertTrue(ButtonCommands.matches("Don’t allow", cancel))
    assertTrue(ButtonCommands.matches("NOT  NOW", cancel))
    assertFalse(ButtonCommands.matches("Send money", confirm))
    assertFalse(ButtonCommands.matches("OK", cancel))
    assertFalse(ButtonCommands.matches("", confirm))
    assertFalse(ButtonCommands.matches(null, confirm))
  }

  @Test
  fun savedLabelsReplaceTheBuiltInOnes() {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val kind = ButtonCommands.Kind.CONFIRM
    ButtonCommands.saveWords(context, prefs, kind, " Go \n\nApply.\n")
    assertEquals("Go\nApply.", ButtonCommands.wordsText(context, prefs, kind))
    assertEquals(setOf("go", "apply"), ButtonCommands.words(context, prefs, kind))
    // Saving nothing or the built-in list removes the setting.
    ButtonCommands.saveWords(context, prefs, kind, "")
    assertNull(prefs.getString(context.getString(kind.wordsKeyRes), null))
    ButtonCommands.saveWords(context, prefs, kind, ButtonCommands.wordsText(context, prefs, kind))
    assertNull(prefs.getString(context.getString(kind.wordsKeyRes), null))
    assertTrue("ok" in ButtonCommands.words(context, prefs, kind))
  }

  @Test
  fun choosesTheButtonNearestToFocusOrTheLowest() {
    val top = Rect(0, 0, 100, 50)
    val bottomLeft = Rect(0, 900, 100, 950)
    val bottomRight = Rect(500, 900, 600, 950)
    val buttons = listOf(top, bottomLeft, bottomRight)
    assertEquals(0, ButtonCommands.choose(buttons, Rect(0, 60, 100, 110)))
    assertEquals(2, ButtonCommands.choose(buttons, Rect(450, 800, 550, 850)))
    assertEquals(1, ButtonCommands.choose(listOf(top, bottomLeft), null))
    assertEquals(-1, ButtonCommands.choose(emptyList(), null))
  }

  @Test
  fun commandValuesAreRecognized() {
    for (kind in ButtonCommands.Kind.entries) {
      assertEquals(kind, ButtonCommands.kindOf(context, context.getString(kind.valueRes)))
    }
    assertNull(ButtonCommands.kindOf(context, "TOUCH_CLICK_ACTION"))
    assertNull(ButtonCommands.kindOf(context, null))
  }
}
