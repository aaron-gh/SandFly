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

package com.google.android.accessibility.talkback.gesture

import android.text.InputType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Which fields New line types a new line in, and which get their action. */
class NewLineTest {
  private val text = InputType.TYPE_CLASS_TEXT

  @Test
  fun multiLineFieldsGetANewLine() {
    assertTrue(NewLine.typesNewLine(text or InputType.TYPE_TEXT_FLAG_MULTI_LINE, multiLine = false))
    assertTrue(
      NewLine.typesNewLine(
        text or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
        multiLine = false,
      )
    )
    // A node that says it has several lines, as a web page's text area does, without an input type.
    assertTrue(NewLine.typesNewLine(InputType.TYPE_NULL, multiLine = true))
  }

  @Test
  fun singleLineFieldsGetTheirAction() {
    assertFalse(NewLine.typesNewLine(text, multiLine = false))
    assertFalse(NewLine.typesNewLine(text or InputType.TYPE_TEXT_VARIATION_PASSWORD, multiLine = false))
    assertFalse(NewLine.typesNewLine(InputType.TYPE_CLASS_NUMBER, multiLine = false))
    assertFalse(NewLine.typesNewLine(InputType.TYPE_NULL, multiLine = false))
  }
}
