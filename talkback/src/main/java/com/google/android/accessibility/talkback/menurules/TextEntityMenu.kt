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

package com.google.android.accessibility.talkback.menurules

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.style.URLSpan
import android.text.util.Linkify
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.contextmenu.ContextMenu
import com.google.android.accessibility.talkback.contextmenu.ContextMenuItem
import com.google.android.accessibility.talkback.contextmenu.OnContextMenuItemClickListener
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Links, email addresses, phone numbers and codes, such as one-time codes in a text message, that
 * are written in an item's text without being links, offered with the item's links in the
 * Actions menu: links and addresses open, numbers open in the phone app, and codes are copied.
 */
class TextEntityMenu(private val pipeline: Pipeline.FeedbackReturner) : NodeMenu {
  /** Something found in the text, and what choosing it does. */
  sealed interface Entity {
    val text: String

    data class Web(override val text: String, val url: String) : Entity

    data class Email(override val text: String, val address: String) : Entity

    data class Phone(override val text: String, val number: String) : Entity

    data class Code(override val text: String) : Entity
  }

  /**
   * Whether the text of [node] or its children may hold something to offer. It runs as each item
   * is focused, for the hint that actions are available, so it only looks for digits or the marks
   * of an address.
   */
  override fun accept(context: Context, node: AccessibilityNodeInfoCompat): Boolean {
    if (mayHold(node.text) || mayHold(node.contentDescription)) return true
    for (i in 0 until minOf(node.childCount, MAX_CHILDREN_CHECKED)) {
      val child = node.getChild(i) ?: continue
      if (mayHold(child.text) || mayHold(child.contentDescription)) return true
    }
    return false
  }

  /**
   * The items for the Actions menu. None for the hint that actions are available
   * ([includeAncestors] false), which a date or an amount in the text would otherwise give to many
   * items.
   */
  override fun getMenuItemsForNode(
    context: Context,
    node: AccessibilityNodeInfoCompat,
    includeAncestors: Boolean,
  ): List<ContextMenuItem> {
    if (!includeAncestors) return emptyList()
    return find(textOf(node), linkedUrls(node)).mapIndexed { index, entity ->
      val label =
        when (entity) {
          is Entity.Web,
          is Entity.Email -> entity.text
          is Entity.Phone -> context.getString(R.string.menu_item_call_number, entity.text)
          is Entity.Code -> context.getString(R.string.menu_item_copy_code, entity.text)
        }
      ContextMenu.createMenuItem(context, R.id.group_links, FIRST_ITEM_ID + index, Menu.NONE, label)
        .apply {
          setOnMenuItemClickListener(
            object : OnContextMenuItemClickListener {
              override fun onMenuItemClick(item: MenuItem): Boolean = choose(context, entity)
            }
          )
        }
    }
  }

  private fun choose(context: Context, entity: Entity): Boolean {
    val intent =
      when (entity) {
        is Entity.Web -> Intent(Intent.ACTION_VIEW, Uri.parse(entity.url))
        is Entity.Email -> Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + entity.address))
        is Entity.Phone -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(entity.number)))
        is Entity.Code -> {
          val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
          clipboard.setPrimaryClip(ClipData.newPlainText(entity.text, entity.text))
          pipeline.returnFeedback(
            EVENT_ID_UNTRACKED,
            Feedback.speech(context.getString(R.string.template_text_copied, entity.text)),
          )
          return true
        }
      }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
      context.startActivity(intent)
      true
    } catch (e: ActivityNotFoundException) {
      LogUtils.w(TAG, "Nothing opens %s", entity)
      false
    }
  }

  companion object {
    private const val TAG = "TextEntityMenu"
    // After the link menu's items, which are numbered from 0 in the same group.
    private const val FIRST_ITEM_ID = 1000
    private const val MAX_NODES = 40
    private const val MAX_CHARACTERS = 4_000
    private const val MAX_ENTITIES = 20
    private const val MAX_CHILDREN_CHECKED = 10
    private val MAY_HOLD = Regex("\\p{Nd}{4}|@|://|[a-zA-Z0-9]\\.[a-zA-Z]{2,}")

    private fun mayHold(text: CharSequence?): Boolean =
      text != null && text.length >= 4 && MAY_HOLD.containsMatchIn(text)

    private val CODE = Regex("(?<!\\p{Nd})\\p{Nd}{4,8}(?!\\p{Nd})")

    // Digit groups with at most one space, dot or hyphen between them, and a group in parentheses
    // such as an area code. Possessive, so that text without a number takes linear time.
    private val PHONE =
      Regex("\\+?+(?:\\(\\d{1,5}\\)|\\d++)(?:[ .\\-]?+(?:\\(\\d{1,5}\\)|\\d++))*+")
    private val PHONE_GROUP = Regex("\\d+|\\(\\d+\\)")

    private fun isCurrency(c: Char?): Boolean =
      c != null && Character.getType(c) == Character.CURRENCY_SYMBOL.toInt()

    /**
     * Whether the number starting at [start] is part of something else: a word, a user name, a
     * link, a date, an amount or a larger number. A hyphen after a single letter, as in G-482916,
     * leaves the number on its own.
     */
    private fun joinedBefore(text: String, start: Int): Boolean {
      val c = text.getOrNull(start - 1) ?: return false
      if (c.isLetterOrDigit() || c in "_@/\\#&=%+.,'" || isCurrency(c)) return true
      // Part of a time, such as 10:45, but not after a word, as in Code:482916.
      if (c == ':') return text.getOrNull(start - 2)?.isDigit() == true
      if (c == '-') {
        val p = text.getOrNull(start - 2) ?: return false
        if (!p.isLetterOrDigit()) return false
        return p.isDigit() || text.getOrNull(start - 3)?.isLetterOrDigit() == true
      }
      return c.isWhitespace() && isCurrency(text.getOrNull(start - 2))
    }

    /** Whether the number ending before [end] continues into something else. */
    private fun joinedAfter(text: String, end: Int): Boolean {
      val c = text.getOrNull(end) ?: return false
      if (c.isLetterOrDigit() || c in "_@/\\%" || isCurrency(c)) return true
      if (c in ".,:-'") return text.getOrNull(end + 1)?.isLetterOrDigit() == true
      return c.isWhitespace() && isCurrency(text.getOrNull(end + 1))
    }

    private fun isYear(digits: String): Boolean =
      digits.length == 4 && digits.toIntOrNull()?.let { it in 1900..2099 } == true

    /**
     * The number to dial for [shown], a match of [PHONE], or null when it reads as a date, a time
     * span, an amount, an address or a plain number rather than a phone number.
     */
    private fun phoneNumber(shown: String): String? {
      val digits = shown.filter { it in '0'..'9' }
      if (digits.length !in 7..15) return null
      val international = shown.startsWith('+')
      val groups = PHONE_GROUP.findAll(shown).map { it.value }.toList()
      val plain = groups.filter { !it.startsWith('(') }
      val hasParentheses = plain.size < groups.size
      val dots = '.' in shown
      if (groups.size == 1) {
        // Without separators, only a full national number.
        if (!international && digits.length !in 10..12) return null
      } else {
        // After the country code, a single digit may start the national number, as in +33 1.
        if (plain.drop(if (international) 2 else 1).any { it.length < 2 }) return null
        if (dots && '-' in shown) return null
        // A decimal, or two numbers in a row, such as a code and a number of minutes.
        if (groups.size == 2 && !international && !hasParentheses) {
          if (dots || plain.any { it.length < 3 }) return null
          if (plain.all(::isYear)) return null
        }
        if (dots && !international) {
          // An IP address or an amount with dots between thousands.
          if (groups.size == 4 && groups.all { it.length <= 3 }) return null
          if (plain.drop(1).all { it.length == 3 }) return null
        }
        // A date, such as 2026-11-07 or 07.11.2026, also followed by a time.
        if (groups.size == 3 && digits.length <= 8 && plain.count { it.length <= 2 } >= 2) {
          return null
        }
        if (!international && groups.size > 3) {
          val (a, b, c) = groups
          if (isYear(a) && b.length <= 2 && c.length <= 2) return null
          if (a.length <= 2 && b.length <= 2 && isYear(c)) return null
        }
      }
      // +44 (0)20 is dialled as +4420.
      val dialled = if (international) shown.replace("(0)", "") else shown
      return dialled.filter { it in '0'..'9' || it == '+' }
    }

    /** The text of [node] and its descendants, as much as is looked through. */
    @JvmStatic
    fun textOf(node: AccessibilityNodeInfoCompat): String {
      val text = StringBuilder()
      val queue = ArrayDeque<AccessibilityNodeInfoCompat>()
      queue.add(node)
      var visited = 0
      while (queue.isNotEmpty() && visited < MAX_NODES && text.length < MAX_CHARACTERS) {
        val current = queue.removeFirst()
        visited++
        current.text?.let { text.append(it).append('\n') }
        current.contentDescription?.let { text.append(it).append('\n') }
        for (i in 0 until current.childCount) {
          current.getChild(i)?.let(queue::add)
        }
      }
      return text.take(MAX_CHARACTERS).toString()
    }

    /** The URLs that [node]'s own text already links, which the link menu offers. */
    private fun linkedUrls(node: AccessibilityNodeInfoCompat): Set<String> {
      val text = node.text as? Spanned ?: return emptySet()
      return text.getSpans(0, text.length, URLSpan::class.java).mapNotNull { it.url }.toSet()
    }

    /** Finds the entities in [text], leaving out links in [linked]. */
    @JvmStatic
    fun find(text: String, linked: Set<String> = emptySet()): List<Entity> {
      if (text.isBlank()) return emptyList()
      val spannable = SpannableString(text)
      // Not Linkify.PHONE_NUMBERS, which takes years, parts of user names and other short digit
      // runs for numbers.
      Linkify.addLinks(spannable, Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES)
      val found = LinkedHashMap<String, Entity>()
      val linkRanges = mutableListOf<IntRange>()
      val spans =
        spannable.getSpans(0, spannable.length, URLSpan::class.java).sortedBy {
          spannable.getSpanStart(it)
        }
      for (span in spans) {
        val start = spannable.getSpanStart(span)
        val end = spannable.getSpanEnd(span)
        val shown = text.substring(start, end)
        val url = span.url ?: continue
        linkRanges += start until end
        val entity =
          when {
            url.startsWith("mailto:") -> Entity.Email(shown, url.removePrefix("mailto:"))
            url in linked || shown in linked -> null
            else -> Entity.Web(shown, url)
          }
        if (entity != null) found.putIfAbsent(entity.text, entity)
      }
      fun inLink(range: IntRange) = linkRanges.any { range.first in it || range.last in it }
      val phoneRanges = mutableListOf<IntRange>()
      for (match in PHONE.findAll(text)) {
        val range = match.range
        if (inLink(range)) continue
        if (joinedBefore(text, range.first) || joinedAfter(text, range.last + 1)) continue
        val number = phoneNumber(match.value) ?: continue
        phoneRanges += range
        found.putIfAbsent(match.value, Entity.Phone(match.value, number))
      }
      for (match in CODE.findAll(text)) {
        val range = match.range
        if (inLink(range) || phoneRanges.any { range.first in it }) continue
        if (joinedBefore(text, range.first) || joinedAfter(text, range.last + 1)) continue
        if (isYear(match.value)) continue
        found.putIfAbsent(match.value, Entity.Code(match.value))
      }
      return found.values.take(MAX_ENTITIES)
    }
  }
}
