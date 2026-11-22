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

package com.google.android.accessibility.talkback.commands

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.R

/**
 * Activate confirm button and Activate cancel button: find a button on the screen whose label is
 * one of a list of labels, such as OK or Cancel.
 */
object ButtonCommands {

  /** The two commands, with the setting that holds their labels and the built-in labels. */
  enum class Kind(
    @StringRes val valueRes: Int,
    @StringRes val labelRes: Int,
    @StringRes val wordsKeyRes: Int,
    @StringRes val defaultsRes: Int,
    @StringRes val noneRes: Int,
  ) {
    CONFIRM(
      R.string.shortcut_value_activate_confirm_button,
      R.string.shortcut_activate_confirm_button,
      R.string.pref_confirm_button_words_key,
      R.string.confirm_button_words_default,
      R.string.no_confirm_button,
    ),
    CANCEL(
      R.string.shortcut_value_activate_cancel_button,
      R.string.shortcut_activate_cancel_button,
      R.string.pref_cancel_button_words_key,
      R.string.cancel_button_words_default,
      R.string.no_cancel_button,
    ),
  }

  /** Most characters kept in a label list. */
  const val MAX_TEXT = 4_000

  private const val MAX_WORDS = 200
  private const val MAX_LABEL_LENGTH = 100
  private const val MAX_NODES = 3_000
  private const val MAX_INNER_NODES = 20

  private val WHITESPACE = Regex("\\s+")
  private val TRAILING_PUNCTUATION = Regex("[\\p{P}\\p{S}\\s]+$")

  /** Returns the command saved as [value], or null when it is neither of the two. */
  @JvmStatic
  fun kindOf(context: Context, value: String?): Kind? =
    value?.let { v -> Kind.entries.firstOrNull { context.getString(it.valueRes) == v } }

  /** Returns the label list of [kind], one label per line: the user's, or the built-in one. */
  @JvmStatic
  fun wordsText(context: Context, prefs: SharedPreferences, kind: Kind): String =
    prefs.getString(context.getString(kind.wordsKeyRes), null)?.takeIf { it.isNotBlank() }
      ?: context.getString(kind.defaultsRes)

  /**
   * Saves [text] as the label list of [kind]. An empty list, or the built-in one, removes the
   * setting, so the built-in list applies.
   */
  @JvmStatic
  fun saveWords(context: Context, prefs: SharedPreferences, kind: Kind, text: String) {
    val lines =
      text.take(MAX_TEXT).lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
    val key = context.getString(kind.wordsKeyRes)
    val defaults = context.getString(kind.defaultsRes).lines().map { it.trim() }
    if (lines.isEmpty() || lines == defaults) {
      prefs.edit().remove(key).apply()
    } else {
      prefs.edit().putString(key, lines.joinToString("\n")).apply()
    }
  }

  /** Returns the labels of [kind], normalized as [normalize] does. */
  @JvmStatic
  fun words(context: Context, prefs: SharedPreferences, kind: Kind): Set<String> =
    parse(wordsText(context, prefs, kind))

  /** Returns the labels in [text], one per line, normalized. */
  @VisibleForTesting
  fun parse(text: String): Set<String> =
    text.take(MAX_TEXT).lineSequence().map { normalize(it) }.filter { it.isNotEmpty() }
      .take(MAX_WORDS).toSet()

  /**
   * Returns [label] trimmed, in lower case, with spaces collapsed, a typographic apostrophe as a
   * plain one and punctuation and symbols at the end removed.
   */
  @VisibleForTesting
  fun normalize(label: CharSequence): String =
    label
      .toString()
      .replace('’', '\'')
      .replace(WHITESPACE, " ")
      .replace(TRAILING_PUNCTUATION, "")
      .trim()
      .lowercase()

  /** Whether [label] is one of [words]. */
  @VisibleForTesting
  fun matches(label: CharSequence?, words: Set<String>): Boolean =
    !label.isNullOrBlank() && label.length <= MAX_LABEL_LENGTH && normalize(label) in words

  /**
   * Returns the index in [bounds] of the button to activate: the one whose centre is nearest to
   * the centre of [focus], or without focus the lowest on the screen.
   */
  @VisibleForTesting
  fun choose(bounds: List<Rect>, focus: Rect?): Int {
    if (bounds.isEmpty()) return -1
    if (focus != null) {
      val x = focus.exactCenterX()
      val y = focus.exactCenterY()
      return bounds.indices.minByOrNull { i ->
        val dx = bounds[i].exactCenterX() - x
        val dy = bounds[i].exactCenterY() - y
        dx * dx + dy * dy
      } ?: -1
    }
    return bounds.indices.maxWithOrNull(
      compareBy<Int> { bounds[it].exactCenterY() }.thenBy { bounds[it].bottom }
    ) ?: -1
  }

  /**
   * Returns the button of [kind] to activate, or null when there is none. The active window is
   * searched first, then the other app windows from the top; the first window with a matching
   * button is used.
   */
  @JvmStatic
  fun find(
    service: AccessibilityService,
    prefs: SharedPreferences,
    kind: Kind,
  ): AccessibilityNodeInfoCompat? {
    val words = words(service, prefs, kind)
    if (words.isEmpty()) return null
    val windows =
      service.windows
        .filter { isSearched(it) }
        .sortedWith(
          compareByDescending<AccessibilityWindowInfo> { it.isActive }.thenByDescending { it.layer }
        )
    val focus =
      service.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)?.let { node ->
        Rect().also { node.getBoundsInScreen(it) }
      }
    var count = 0
    for (window in windows) {
      val root = window.root ?: continue
      val found = mutableListOf<AccessibilityNodeInfo>()
      val queue = ArrayDeque<AccessibilityNodeInfo>()
      queue.add(root)
      while (queue.isNotEmpty() && count < MAX_NODES) {
        val node = queue.removeFirst()
        count++
        if (node.isVisibleToUser && isButton(node) && labelMatches(node, words)) {
          found += node
          continue
        }
        for (i in 0 until node.childCount) {
          node.getChild(i)?.let { queue.add(it) }
        }
      }
      if (found.isNotEmpty()) {
        val bounds = found.map { node -> Rect().also { node.getBoundsInScreen(it) } }
        return AccessibilityNodeInfoCompat.wrap(found[choose(bounds, focus)])
      }
      if (count >= MAX_NODES) break
    }
    return null
  }

  /** App windows, and system windows such as permission dialogs while they are active. */
  private fun isSearched(window: AccessibilityWindowInfo): Boolean =
    when (window.type) {
      // A picture-in-picture window has its own Next and Close buttons.
      AccessibilityWindowInfo.TYPE_APPLICATION -> window.isActive || !window.isInPictureInPictureMode
      // The status bar and navigation bar, with their Back button, are never active.
      AccessibilityWindowInfo.TYPE_SYSTEM -> window.isActive || window.isFocused
      else -> false
    }

  private fun isButton(node: AccessibilityNodeInfo): Boolean =
    node.isEnabled &&
      node.actionList.any { it.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK.id }

  /**
   * Whether the text or content description of [node] is one of [words], or, when it has neither,
   * the only text inside it.
   */
  private fun labelMatches(node: AccessibilityNodeInfo, words: Set<String>): Boolean {
    val text = node.text
    val description = node.contentDescription
    if (matches(text, words) || matches(description, words)) return true
    if (!text.isNullOrBlank() || !description.isNullOrBlank()) return false
    return matches(innerLabel(node), words)
  }

  /** Returns the only label of the items inside [node] that can't be activated, or null. */
  private fun innerLabel(node: AccessibilityNodeInfo): CharSequence? {
    var label: CharSequence? = null
    var count = 0
    val queue = ArrayDeque<AccessibilityNodeInfo>()
    for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
    while (queue.isNotEmpty() && count < MAX_INNER_NODES) {
      val inner = queue.removeFirst()
      count++
      if (inner.isClickable) return null
      val own = inner.text?.takeIf { it.isNotBlank() } ?: inner.contentDescription
      if (!own.isNullOrBlank()) {
        if (label != null) return null
        label = own
      }
      for (i in 0 until inner.childCount) inner.getChild(i)?.let { queue.add(it) }
    }
    return if (queue.isEmpty()) label else null
  }
}
