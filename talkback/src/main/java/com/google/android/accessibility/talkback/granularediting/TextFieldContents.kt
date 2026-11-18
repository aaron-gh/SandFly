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

import android.content.Context
import android.text.TextUtils
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.compositor.CompositorUtils
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.Role

/** Read text field: the whole text of a text field, as the command says it. */
object TextFieldContents {

  /**
   * The text field the command reads: the text field with input focus, else the focused item when
   * it is a text field, or null when there is none.
   */
  @JvmStatic
  fun field(
    inputFocus: AccessibilityNodeInfoCompat?,
    accessibilityFocus: AccessibilityNodeInfoCompat?,
  ): AccessibilityNodeInfoCompat? =
    when {
      inputFocus != null &&
        inputFocus.isVisibleToUser &&
        Role.getRole(inputFocus) == Role.ROLE_EDIT_TEXT -> inputFocus
      Role.getRole(accessibilityFocus) == Role.ROLE_EDIT_TEXT -> accessibilityFocus
      else -> null
    }

  /**
   * What the command says for [node]: its whole text, or "Empty" and the hint when it has no text.
   * The text of a password field is said as the app gives it, without symbol names.
   */
  @JvmStatic
  fun describe(context: Context, node: AccessibilityNodeInfoCompat): CharSequence {
    val text: CharSequence? =
      if (node.isShowingHintText) null else AccessibilityNodeInfoUtils.getText(node)
    if (TextUtils.isEmpty(text)) {
      val hint = AccessibilityNodeInfoUtils.getHintText(node)
      val empty = context.getString(R.string.text_field_empty)
      return if (TextUtils.isEmpty(hint)) empty else CompositorUtils.joinCharSequences(empty, hint)
    }
    return if (node.isPassword) text!! else CompositorUtils.refineInputText(context, text)
  }
}
