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

import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.method.MetaKeyKeyListener
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** The modifier keys whose meta key states are cleared when a chooser closes. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChooserMetaKeysTest {
  private val alt = KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
  private val shift = KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON

  @Test
  fun altOfAltSpaceIsClearedWhenTheChooserCloses() {
    val keys = ChooserMetaKeys()
    // Alt goes down and reaches the keyboard; SandFly takes Space and opens the chooser.
    keys.onKeyEvent(alt, choosing = false)
    keys.onOpen()
    // Alt is released, a name is typed and Enter chooses.
    keys.onKeyEvent(0, choosing = true)
    keys.onKeyEvent(0, choosing = true)
    assertEquals(KeyEvent.META_ALT_ON, keys.onClose())
    // Nothing is left for the next chooser.
    keys.onOpen()
    assertEquals(0, keys.onClose())
  }

  @Test
  fun altHeldUntilTheChooserClosesIsCleared() {
    val keys = ChooserMetaKeys()
    keys.onKeyEvent(alt, choosing = false)
    keys.onOpen()
    // Alt + Enter.
    keys.onKeyEvent(alt, choosing = true)
    assertEquals(KeyEvent.META_ALT_ON, keys.onClose())
  }

  @Test
  fun modifiersPressedWhileOpenAreCleared() {
    val keys = ChooserMetaKeys()
    keys.onOpen()
    // Shift pressed for a capital in the query, then Escape.
    keys.onKeyEvent(shift, choosing = true)
    keys.onKeyEvent(0, choosing = true)
    assertEquals(KeyEvent.META_SHIFT_ON, keys.onClose())
  }

  @Test
  fun shiftAltSpaceOpenedByTheKeyboardClearsBoth() {
    val keys = ChooserMetaKeys()
    keys.onKeyEvent(alt or shift, choosing = false)
    keys.onOpen()
    assertEquals(KeyEvent.META_ALT_ON or KeyEvent.META_SHIFT_ON, keys.onClose())
  }

  @Test
  fun noModifiersNothingToClear() {
    val keys = ChooserMetaKeys()
    // Alt + W typed before: Alt was released before the chooser opened.
    keys.onKeyEvent(alt, choosing = false)
    keys.onKeyEvent(0, choosing = false)
    keys.onOpen()
    keys.onKeyEvent(0, choosing = true)
    assertEquals(0, keys.onClose())
  }

  @Test
  fun keysWithoutAChooserAreNotCleared() {
    val keys = ChooserMetaKeys()
    keys.onKeyEvent(alt, choosing = false)
    keys.onKeyEvent(shift, choosing = false)
    assertEquals(0, keys.onClose())
  }

  @Test
  fun resetForgetsHeldModifiers() {
    val keys = ChooserMetaKeys()
    keys.onKeyEvent(alt, choosing = false)
    keys.reset()
    keys.onOpen()
    assertEquals(0, keys.onClose())
  }

  @Test
  fun statesAndNames() {
    assertEquals(
      KeyEvent.META_ALT_ON,
      ChooserMetaKeys.statesOf(KeyEvent.META_ALT_RIGHT_ON or KeyEvent.META_NUM_LOCK_ON),
    )
    assertEquals(KeyEvent.META_SYM_ON, ChooserMetaKeys.statesOf(KeyEvent.META_SYM_ON))
    assertEquals(0, ChooserMetaKeys.statesOf(KeyEvent.META_CTRL_ON or KeyEvent.META_CAPS_LOCK_ON))
    assertEquals("Shift, Alt", ChooserMetaKeys.describe(KeyEvent.META_ALT_ON or KeyEvent.META_SHIFT_ON))
    assertEquals("none", ChooserMetaKeys.describe(0))
  }

  /**
   * The platform side: a text field's key listener keeps Alt pressed in its text after Alt goes
   * down, and clearMetaKeyStates of the input connection, as the SandFly keyboard calls it when the
   * chooser closes, removes it.
   */
  @Test
  fun clearMetaKeyStatesRemovesAltFromTheTextField() {
    val text = SpannableStringBuilder("")
    val listener = object : MetaKeyKeyListener() {}
    val altDown = KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ALT_LEFT, 0, alt)
    assertTrue(listener.onKeyDown(null, text, KeyEvent.KEYCODE_ALT_LEFT, altDown))
    assertNotEquals(0, MetaKeyKeyListener.getMetaState(text, KeyEvent.META_ALT_ON))

    val connection =
      object : BaseInputConnection(View(RuntimeEnvironment.getApplication()), true) {
        override fun getEditable(): Editable = text
      }
    val keys = ChooserMetaKeys()
    keys.onKeyEvent(alt, choosing = false)
    keys.onOpen()
    keys.onKeyEvent(0, choosing = true)
    assertTrue(connection.clearMetaKeyStates(keys.onClose()))
    assertEquals(0, MetaKeyKeyListener.getMetaState(text, KeyEvent.META_ALT_ON))
  }
}
