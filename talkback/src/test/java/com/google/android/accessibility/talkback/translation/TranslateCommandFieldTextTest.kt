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

package com.google.android.accessibility.talkback.translation

import android.view.accessibility.AccessibilityNodeInfo
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TranslateCommandFieldTextTest {
  private val context = RuntimeEnvironment.getApplication()

  private fun field(typed: String): AccessibilityNodeInfoCompat =
    AccessibilityNodeInfoCompat.wrap(EditText(context).apply { setText(typed) }.createAccessibilityNodeInfo())

  @Test
  fun textFieldGivesItsWholeText() {
    assertEquals(
      "First sentence. Second sentence.",
      TranslateCommand.fieldTextToTranslate(field("  First sentence. Second sentence.\n")),
    )
  }

  @Test
  fun commaWithoutMatchingDescriptionKeepsTheWholeText() {
    assertEquals("Hello, world", TranslateCommand.fieldTextToTranslate(field("Hello, world")))
  }

  @Test
  fun textThatMayIncludeItsLabelGivesNothing() {
    val node = field("Hello, Message")
    node.contentDescription = "Hello, Message"
    assertNull(TranslateCommand.fieldTextToTranslate(node))
  }

  @Test
  fun noNodeOrOtherItemsGiveNothing() {
    assertNull(TranslateCommand.fieldTextToTranslate(null))
    val label = TextView(context).apply { text = "A label" }.createAccessibilityNodeInfo()
    assertNull(TranslateCommand.fieldTextToTranslate(AccessibilityNodeInfoCompat.wrap(label)))
  }

  @Test
  fun passwordFieldGivesNothing() {
    val node = field("secret")
    node.isPassword = true
    assertNull(TranslateCommand.fieldTextToTranslate(node))
  }

  @Test
  fun hintOnlyFieldGivesNothing() {
    val node = field("Message")
    node.isShowingHintText = true
    assertNull(TranslateCommand.fieldTextToTranslate(node))
  }

  @Test
  fun emptyAndBlankFieldsGiveNothing() {
    assertNull(TranslateCommand.fieldTextToTranslate(field("")))
    assertNull(TranslateCommand.fieldTextToTranslate(field("   ")))
  }

  @Test
  fun placeholderWithoutHintFlagGivesNothing() {
    val placeholder =
      object : EditText(context) {
        override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
          super.onInitializeAccessibilityNodeInfo(info)
          if (getText().isEmpty()) info.text = "Message"
        }
      }
    assertNull(
      TranslateCommand.fieldTextToTranslate(
        AccessibilityNodeInfoCompat.wrap(placeholder.createAccessibilityNodeInfo())
      )
    )
  }
}
