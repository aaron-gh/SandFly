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

import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.widget.EditText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * What Android's own text fields give of a password, with the Android 14 framework: accessibility
 * gets bullets, while the input connection, which the SandFly keyboard reads, gets the text.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PasswordFieldTextTest {
  private val password = "Pa@1"

  private fun field(inputType: Int): EditText {
    val activity = Robolectric.buildActivity(android.app.Activity::class.java).setup().get()
    val field = EditText(activity)
    activity.setContentView(field)
    field.inputType = inputType
    field.setText(password)
    field.setSelection(password.length)
    return field
  }

  private fun connection(field: EditText, info: EditorInfo = EditorInfo()): InputConnection =
    field.onCreateInputConnection(info)!!

  @Test
  fun inputConnectionGivesThePasswordText() {
    for (inputType in
      listOf(
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
        InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD,
      )) {
      val field = field(inputType)
      val connection = connection(field)
      assertEquals(password, connection.getTextBeforeCursor(100, 0).toString())
      assertEquals(password, connection.getExtractedText(ExtractedTextRequest(), 0)!!.text.toString())
      field.setSelection(1)
      assertEquals("a@1", connection.getTextAfterCursor(100, 0).toString())
      field.setSelection(0, 2)
      assertEquals("Pa", connection.getSelectedText(0).toString())
    }
  }

  @Test
  fun accessibilityGetsNoPasswordText() {
    val field = field(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
    val node = field.createAccessibilityNodeInfo()
    assertTrue(node.isPassword)
    assertNotEquals(password, node.text?.toString())
  }

  @Test
  fun editorInfoLeavesOutTheInitialTextOfAPasswordField() {
    val field = field(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
    val info = EditorInfo()
    connection(field, info)
    assertNull(info.getInitialTextBeforeCursor(100, 0))
  }
}
