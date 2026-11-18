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

package com.google.android.accessibility.talkback.granularediting

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TextFieldContentsTest {
  private val context = RuntimeEnvironment.getApplication()

  @Suppress("DEPRECATION") // obtain(): a node to give text to.
  private fun editBox(
    text: String?,
    hintText: String? = null,
    showingHint: Boolean = false,
    password: Boolean = false,
  ) =
    AccessibilityNodeInfoCompat.obtain().apply {
      className = "android.widget.EditText"
      isEditable = true
      isVisibleToUser = true
      this.text = text
      this.hintText = hintText
      isShowingHintText = showingHint
      isPassword = password
    }

  @Suppress("DEPRECATION")
  private fun button() =
    AccessibilityNodeInfoCompat.obtain().apply {
      className = "android.widget.Button"
      isVisibleToUser = true
      text = "Send"
    }

  @Test
  fun saysTheWholeText() {
    assertEquals(
      "Hello world. Second line",
      TextFieldContents.describe(context, editBox("Hello world. Second line")).toString(),
    )
  }

  @Test
  fun saysEmptyAndTheHintWithoutText() {
    assertEquals("Empty", TextFieldContents.describe(context, editBox(null)).toString())
    assertEquals(
      "Empty, Message",
      TextFieldContents.describe(context, editBox(null, hintText = "Message")).toString(),
    )
    // A hint shown as the text is no text.
    assertEquals(
      "Empty, Message",
      TextFieldContents.describe(
          context,
          editBox("Message", hintText = "Message", showingHint = true),
        )
        .toString(),
    )
  }

  @Test
  fun passwordTextIsSaidAsGiven() {
    assertEquals(
      "•••",
      TextFieldContents.describe(context, editBox("•••", password = true))
        .toString(),
    )
  }

  @Test
  fun inputFocusComesFirstThenTheFocusedTextField() {
    val typedIn = editBox("a")
    val focused = editBox("b")
    assertSame(typedIn, TextFieldContents.field(typedIn, focused))
    assertSame(focused, TextFieldContents.field(null, focused))
    assertSame(focused, TextFieldContents.field(button(), focused))
    assertNull(TextFieldContents.field(null, button()))
    assertNull(TextFieldContents.field(button(), null))
  }
}
