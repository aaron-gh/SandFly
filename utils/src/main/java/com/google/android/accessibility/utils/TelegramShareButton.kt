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
 * message, so every message in a channel took two swipes. Such buttons aren't focused on their
 * own; the message's actions have each of them, under the label Telegram gives it.
 */
object TelegramShareButton {
  private const val IMAGE_BUTTON = "android.widget.ImageButton"

  /**
   * Whether [node] is a share button of a message. Every such child of a message is hidden, and
   * each is an action of the message, so none is out of reach.
   */
  @JvmStatic
  fun isShareButton(node: AccessibilityNodeInfoCompat): Boolean =
    looksLikeShareButton(node) && node.parent?.let { TelegramReplyQuote.isMessage(it) } == true

  /** The share buttons among [message]'s children, in order; empty when it has none. */
  @JvmStatic
  fun shareButtonsOf(message: AccessibilityNodeInfoCompat): List<AccessibilityNodeInfoCompat> {
    if (!TelegramReplyQuote.isTelegram(message.packageName) ||
      !TelegramReplyQuote.isMessage(message)
    ) {
      return emptyList()
    }
    // All children, with no limit, so that every button hidden by isShareButton is offered, even
    // after the many links of a long message.
    return shareButtonsAmong((0 until message.childCount).map(message::getChild))
  }

  /** The children in [children] that are share buttons, in order. */
  @JvmStatic
  fun shareButtonsAmong(
    children: List<AccessibilityNodeInfoCompat?>
  ): List<AccessibilityNodeInfoCompat> = children.filterNotNull().filter(::looksLikeShareButton)

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
