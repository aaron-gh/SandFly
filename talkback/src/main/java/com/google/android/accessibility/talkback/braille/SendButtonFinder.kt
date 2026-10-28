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

package com.google.android.accessibility.talkback.braille

import android.graphics.Rect
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import java.util.ArrayDeque
import java.util.Locale

/**
 * Finds the button that sends what was typed in a field, for chat apps such as Messages, WhatsApp
 * and Telegram. Their message fields have no editor action, or one the app ignores, so the braille
 * keyboard's submit gesture has to press the send button next to the field instead.
 */
object SendButtonFinder {
  /** Stops a search through a huge window from holding up the keyboard. */
  private const val MAX_NODES = 3000

  /** How many levels above a labeled view to look for the view that takes the click. */
  private const val MAX_CLICKABLE_ANCESTOR_DEPTH = 3

  /** Labels longer than this are text, such as a message that starts with "Send", not a button. */
  private const val MAX_LABEL_LENGTH = 50

  /**
   * Returns the view to click to send what was typed in [field], or null if its window has none.
   *
   * [sendWords] are the words a send button can be labeled with, such as "Send" in the user's
   * language. A button in the same row as the field wins over one elsewhere in the window, so a
   * message in the conversation that is labeled like a button can't be pressed instead.
   */
  @JvmStatic
  fun find(
    field: AccessibilityNodeInfoCompat,
    sendWords: Collection<String>,
  ): AccessibilityNodeInfoCompat? {
    val root = AccessibilityNodeInfoUtils.getRoot(field) ?: return null
    val fieldBounds = Rect().also { field.getBoundsInScreen(it) }
    var best: Candidate? = null
    val queue = ArrayDeque<AccessibilityNodeInfoCompat>().apply { add(root) }
    var visited = 0
    while (queue.isNotEmpty() && visited < MAX_NODES) {
      val node = queue.removeFirst()
      visited++
      for (i in 0 until node.childCount) {
        node.getChild(i)?.let { queue.add(it) }
      }
      val strength = matchStrength(node, sendWords)
      if (strength == Strength.NONE || node.isEditable || !node.isVisibleToUser) {
        continue
      }
      val target = clickTarget(node) ?: continue
      if (!target.isEnabled || target == field) {
        continue
      }
      val bounds = Rect().also { target.getBoundsInScreen(it) }
      // A container around the field, such as the whole compose bar, would click the wrong thing.
      if (bounds.contains(fieldBounds)) {
        continue
      }
      val sameRow = bounds.top < fieldBounds.bottom && bounds.bottom > fieldBounds.top
      // Away from the field, only a view the app calls a send button will do, not a message that
      // just says "Send".
      if (!sameRow && !isSendViewId(node.viewIdResourceName)) {
        continue
      }
      val candidate = Candidate(target, sameRow, strength, distance(bounds, fieldBounds))
      if (best == null || candidate.isBetterThan(best)) {
        best = candidate
      }
    }
    return best?.node
  }

  /** Returns whether [viewId], such as `com.whatsapp:id/send`, names a send button. */
  @JvmStatic
  fun isSendViewId(viewId: String?): Boolean {
    if (viewId.isNullOrEmpty()) {
      return false
    }
    // Ids look like "send", "send_message_button_icon", "sendButton" or "Compose:Draft:Send".
    val name = viewId.substringAfter(":id/")
    return name
      .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
      .lowercase(Locale.ROOT)
      .split(Regex("[^a-z0-9]+"))
      .contains("send")
  }

  /**
   * Returns how surely [label] names a send button: [Strength.STRONG] when it is just one of
   * [sendWords], [Strength.WEAK] when it starts with one, as "Send SMS" or "Send message" do.
   */
  @JvmStatic
  fun labelStrength(label: CharSequence?, sendWords: Collection<String>): Strength {
    if (label.isNullOrBlank() || label.length > MAX_LABEL_LENGTH) {
      return Strength.NONE
    }
    val text = label.toString().trim().lowercase()
    var strength = Strength.NONE
    for (word in sendWords) {
      val sendWord = word.trim().lowercase()
      if (sendWord.isEmpty()) {
        continue
      }
      if (text == sendWord) {
        return Strength.STRONG
      }
      if (text.startsWith("$sendWord ")) {
        strength = Strength.WEAK
      }
    }
    return strength
  }

  private fun matchStrength(
    node: AccessibilityNodeInfoCompat,
    sendWords: Collection<String>,
  ): Strength {
    if (isSendViewId(node.viewIdResourceName)) {
      return Strength.STRONG
    }
    return maxOf(
      labelStrength(node.contentDescription, sendWords),
      labelStrength(node.text, sendWords),
      labelStrength(node.tooltipText, sendWords),
    )
  }

  /** Returns [node] if it takes clicks, otherwise the nearest ancestor that does. */
  private fun clickTarget(node: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat? {
    var current: AccessibilityNodeInfoCompat? = node
    for (depth in 0..MAX_CLICKABLE_ANCESTOR_DEPTH) {
      val candidate = current ?: return null
      if (candidate.isClickable) {
        return candidate
      }
      current = candidate.parent
    }
    return null
  }

  private fun distance(a: Rect, b: Rect): Long {
    val dx = maxOf(0, maxOf(a.left, b.left) - minOf(a.right, b.right)).toLong()
    val dy = maxOf(0, maxOf(a.top, b.top) - minOf(a.bottom, b.bottom)).toLong()
    return dx * dx + dy * dy
  }

  /** How surely a view is a send button. */
  enum class Strength {
    NONE,
    WEAK,
    STRONG,
  }

  private class Candidate(
    val node: AccessibilityNodeInfoCompat,
    val sameRow: Boolean,
    val strength: Strength,
    val distance: Long,
  ) {
    fun isBetterThan(other: Candidate): Boolean {
      if (sameRow != other.sameRow) {
        return sameRow
      }
      if (distance != other.distance) {
        return distance < other.distance
      }
      return strength > other.strength
    }
  }
}
