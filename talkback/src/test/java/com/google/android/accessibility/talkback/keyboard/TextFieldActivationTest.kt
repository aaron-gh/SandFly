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

import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TextFieldActivationTest {
  private fun node(editable: Boolean): AccessibilityNodeInfoCompat =
    AccessibilityNodeInfoCompat.wrap(
      AccessibilityNodeInfo.obtain(View(RuntimeEnvironment.getApplication())).apply {
        isEditable = editable
      }
    )

  private val field = node(editable = true)
  private val button = node(editable = false)

  @Test
  fun aTextFieldMovedToIsReadUntilActivated() {
    val activation = TextFieldActivation()
    activation.onAccessibilityFocused(field, moving = true)
    assertTrue(activation.isMovedTo(field))
    activation.onInput()
    activation.onActivated()
    assertFalse(activation.isMovedTo(field))
  }

  @Test
  fun typingIntoItOrClickingItStartsEditing() {
    val activation = TextFieldActivation()
    activation.onAccessibilityFocused(field, moving = true)
    activation.onTyped()
    assertFalse(activation.isMovedTo(field))

    activation.onAccessibilityFocused(button, moving = true)
    activation.onAccessibilityFocused(field, moving = true)
    assertTrue(activation.isMovedTo(field))
    activation.onClicked(button)
    assertTrue(activation.isMovedTo(field))
    activation.onClicked(field)
    assertFalse(activation.isMovedTo(field))
  }

  @Test
  fun focusOnANewScreenOrWhileTypingIsNotAMove() {
    val activation = TextFieldActivation()
    activation.onAccessibilityFocused(field, moving = false)
    assertFalse(activation.isMovedTo(field))
    activation.onAccessibilityFocused(button, moving = true)
    assertFalse(activation.isMovedTo(button))
  }

  @Test
  fun focusPutBackOnTheFieldBeingEditedIsNotAMove() {
    val activation = TextFieldActivation()
    activation.onAccessibilityFocused(field, moving = false)
    // A SandFly command, then SandFly focus put back on the same field.
    activation.onInput()
    activation.onAccessibilityFocused(field, moving = true)
    assertFalse(activation.isMovedTo(field))
  }

  @Test
  fun aTextFieldThatAnActivationMovesToIsEdited() {
    val activation = TextFieldActivation()
    activation.onAccessibilityFocused(button, moving = true)
    // Enter on a Search button, which opens a search field.
    activation.onInput()
    activation.onActivated()
    activation.onAccessibilityFocused(field, moving = true)
    assertFalse(activation.isMovedTo(field))
    // The next key moves again.
    activation.onInput()
    activation.onAccessibilityFocused(button, moving = true)
    activation.onAccessibilityFocused(field, moving = true)
    assertTrue(activation.isMovedTo(field))
  }
}
