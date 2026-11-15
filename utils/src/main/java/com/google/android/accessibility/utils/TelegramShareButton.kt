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

package com.google.android.accessibility.utils

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

/**
 * The button beside a Telegram message, Share in channels, or Go to message where Telegram opens
 * the original chat instead. Telegram and its forks expose it as an image button child of the
 * message, so every message in a channel took two swipes. It isn't focused on its own; the
 * message's actions have it, under the label Telegram gives it.
 */
object TelegramShareButton {
  private const val IMAGE_BUTTON = "android.widget.ImageButton"
  private const val MAX_CHILDREN = 50

  /** Whether [node] is the share button of a message. */
  @JvmStatic
  fun isShareButton(node: AccessibilityNodeInfoCompat): Boolean =
    looksLikeShareButton(node) && node.parent?.let { TelegramReplyQuote.isMessage(it) } == true

  /** The share button among [message]'s children, or null when it has none. */
  @JvmStatic
  fun shareButtonOf(message: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat? {
    if (!TelegramReplyQuote.isTelegram(message.packageName) ||
      !TelegramReplyQuote.isMessage(message)
    ) {
      return null
    }
    for (i in 0 until minOf(message.childCount, MAX_CHILDREN)) {
      val child = message.getChild(i) ?: continue
      if (looksLikeShareButton(child)) return child
    }
    return null
  }

  private fun looksLikeShareButton(node: AccessibilityNodeInfoCompat): Boolean =
    looksLikeShareButton(node.packageName, node.className, node.contentDescription, node.isClickable)

  @JvmStatic
  fun looksLikeShareButton(
    packageName: CharSequence?,
    className: CharSequence?,
    contentDescription: CharSequence?,
    clickable: Boolean,
  ): Boolean =
    TelegramReplyQuote.isTelegram(packageName) &&
      className?.toString() == IMAGE_BUTTON &&
      clickable &&
      !contentDescription.isNullOrBlank()
}
