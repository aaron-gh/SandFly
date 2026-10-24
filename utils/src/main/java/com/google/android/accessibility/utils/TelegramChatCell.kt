/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.utils

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

/**
 * A conversation in the chat list of Telegram and its forks. The app draws the row itself, with no
 * text or children, and only sets its content description while filling in the row's own
 * accessibility events, such as its focus event. The list reuses rows for other conversations as
 * it scrolls or updates, so until that event a row still describes the conversation it last
 * showed. Its focus is spoken from the app's event, not early from the node.
 */
object TelegramChatCell {
  /** Whether [node] may be a chat list row, whose own description is out of date until focused. */
  @JvmStatic
  fun isDescribedOnFocus(node: AccessibilityNodeInfoCompat): Boolean =
    isDescribedOnFocus(node.packageName, node.className, node.text, node.childCount)

  @JvmStatic
  fun isDescribedOnFocus(
    packageName: CharSequence?,
    className: CharSequence?,
    text: CharSequence?,
    childCount: Int,
  ): Boolean =
    TelegramReplyQuote.isTelegram(packageName) &&
      className?.toString() in CLASS_NAMES &&
      text.isNullOrEmpty() &&
      childCount == 0

  /** A row is a view group, or a check box while chats are being selected. */
  private val CLASS_NAMES = setOf("android.view.ViewGroup", "android.widget.CheckBox")
}
