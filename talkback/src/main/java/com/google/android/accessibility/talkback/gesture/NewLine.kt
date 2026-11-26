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

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.text.InputType
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

/**
 * The New line command, which does what Enter does in a text field: types a new line in a field
 * of several lines, and performs the action of a field of one line, such as Search or Send, as the
 * handwriting keyboard does.
 */
object NewLine {
  /** Whether a field with [inputType], whose node reports [multiLine], gets a new line typed. */
  @JvmStatic
  fun typesNewLine(inputType: Int, multiLine: Boolean): Boolean =
    multiLine ||
      ((inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT &&
        (inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0)

  /** Whether New line types a new line in [field], rather than performing its action. */
  @JvmStatic
  fun typesNewLine(field: AccessibilityNodeInfoCompat): Boolean =
    typesNewLine(field.inputType, field.isMultiLine)

  /**
   * Performs the action of [field], a field of one line, or else does what Submit text does.
   * Returns whether either was done.
   */
  @JvmStatic
  fun performAction(service: AccessibilityService, field: AccessibilityNodeInfoCompat): Boolean {
    // A keyboard's full-screen editor reports the action done without doing it; Submit text
    // presses the keyboard's action key instead.
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
        !SubmitText.isKeyboardEditor(field) &&
        field.unwrap().actionList.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER) &&
        field.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)
    ) {
      return true
    }
    return SubmitText.submit(service)
  }
}
