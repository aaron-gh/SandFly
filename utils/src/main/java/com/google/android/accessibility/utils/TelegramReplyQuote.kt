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

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ClickableSpan
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

/**
 * The quote of the message a Telegram message replies to. Telegram and its forks expose it as a
 * child of the message, "Reply, name, text", with no class name, which a swipe lands on after the
 * message, or not when the quote is scrolled off the screen. It is read as part of the message
 * instead, after the sender's name, and isn't focused on its own.
 */
object TelegramReplyQuote {
  /** The sender and text of a quote. */
  data class Quote(val name: CharSequence, val text: CharSequence)

  private val PACKAGE_PREFIXES =
    listOf(
      "org.telegram.",
      "tw.nekomimi.",
      "xyz.nextalone.nagram",
      "nekox.messenger",
      "com.exteragram.",
      "uz.unnarsx.cherrygram",
      "it.octogram.",
      "app.nicegram",
      "org.forkgram.",
    )

  @JvmStatic
  fun isTelegram(packageName: CharSequence?): Boolean {
    val name = packageName?.toString() ?: return false
    return PACKAGE_PREFIXES.any { name.startsWith(it) }
  }

  /** Whether [node] is a message's reply quote. */
  @JvmStatic
  fun isReplyQuote(node: AccessibilityNodeInfoCompat): Boolean =
    looksLikeQuote(node) && node.parent?.let { isMessage(it) } == true

  /** The reply quote among [message]'s children, or null when it has none. */
  @JvmStatic
  fun quoteOf(message: AccessibilityNodeInfoCompat): Quote? {
    if (!isTelegram(message.packageName) || !isMessage(message)) return null
    for (i in 0 until minOf(message.childCount, MAX_CHILDREN)) {
      val child = message.getChild(i) ?: continue
      if (looksLikeQuote(child)) return parse(child.contentDescription)
    }
    return null
  }

  /**
   * [description] of [message] with [quote] after the sender's name, or first when the message
   * has no name line. [format] puts the name and the quoted text together, such as "replying to
   * %1$s: %2$s".
   */
  @JvmStatic
  fun withQuote(
    message: AccessibilityNodeInfoCompat,
    description: CharSequence,
    quote: Quote,
    format: String,
    separator: CharSequence,
  ): CharSequence {
    val said = String.format(format, quote.name, quote.text)
    val nameLine = nameLine(message.text)
    val result = SpannableStringBuilder()
    if (nameLine != null && description.toString().startsWith(nameLine)) {
      result.append(description, 0, nameLine.length).append(separator).append(said)
      val rest = description.subSequence(nameLine.length, description.length).trimStart('\n', ' ', ',')
      if (rest.isNotEmpty()) result.append(separator).append(rest)
    } else {
      result.append(said)
      if (description.isNotEmpty()) result.append(separator).append(description)
    }
    return result
  }

  /** "Reply, name, text" split into the name and the text; the first word is Telegram's "Reply". */
  @JvmStatic
  fun parse(description: CharSequence?): Quote? {
    val text = description?.toString() ?: return null
    val afterLabel = text.indexOf(", ")
    if (afterLabel < 0) return null
    val afterName = text.indexOf(", ", afterLabel + 2)
    if (afterName < 0) return null
    val name = text.substring(afterLabel + 2, afterName).trim()
    val quoted = text.substring(afterName + 2).trim()
    if (name.isEmpty() || quoted.isEmpty()) return null
    return Quote(name, quoted)
  }

  /**
   * The sender's name, when it is the message's first line: Telegram makes it a link to the
   * sender's profile in groups, and leaves it out in private chats and the user's own messages.
   */
  private fun nameLine(text: CharSequence?): String? {
    if (text !is Spanned) return null
    val lineEnd = TextUtils.indexOf(text, '\n').takeIf { it > 0 } ?: return null
    val spans = text.getSpans(0, lineEnd, ClickableSpan::class.java)
    val name = spans.firstOrNull { text.getSpanStart(it) == 0 } ?: return null
    if (text.getSpanEnd(name) < minOf(lineEnd, text.length) - NAME_TAIL) return null
    return text.subSequence(0, lineEnd).toString()
  }

  private fun looksLikeQuote(node: AccessibilityNodeInfoCompat): Boolean =
    isTelegram(node.packageName) &&
      node.className.isNullOrEmpty() &&
      node.isClickable &&
      node.contentDescription?.let { parse(it) } != null

  /** A message in the chat: a focusable view group with the message as its text. */
  private fun isMessage(node: AccessibilityNodeInfoCompat): Boolean =
    node.className?.toString() == "android.view.ViewGroup" &&
      node.isFocusable &&
      !node.text.isNullOrEmpty()

  private const val MAX_CHILDREN = 50

  /** Characters after the name link, such as an admin title, still on the name line. */
  private const val NAME_TAIL = 64
}
