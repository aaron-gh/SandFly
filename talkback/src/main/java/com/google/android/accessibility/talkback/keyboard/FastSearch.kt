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

package com.google.android.accessibility.talkback.keyboard

import android.content.Context
import android.view.KeyCharacterMap
import android.view.KeyEvent
import androidx.annotation.StringRes
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.labeling.TalkBackLabelManager
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.Filter
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.text.Normalizer

/**
 * The modifier of fast search, which in browse mode, pressed with a letter or digit, focuses the
 * next item whose text starts with it, and with Shift too, the previous one. Any modifier except
 * Shift and the keymap's SandFly key.
 */
enum class FastSearchKey(
  /** The saved value. */
  val value: String,
  @StringRes val nameRes: Int,
  /** The meta state bits that are down while this key is held. */
  val metaState: Int,
  /** The meta state bits of both keys of this kind, such as both Alt keys for Right Alt. */
  val kindMetaState: Int,
  /** The key codes of this key. */
  val keyCodes: IntArray,
) {
  ALT(
    "alt",
    R.string.keycombo_key_modifier_alt,
    KeyEvent.META_ALT_ON,
    KeyEvent.META_ALT_MASK,
    intArrayOf(KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT),
  ),
  LEFT_ALT(
    "left_alt",
    R.string.fast_search_key_left_alt,
    KeyEvent.META_ALT_LEFT_ON,
    KeyEvent.META_ALT_MASK,
    intArrayOf(KeyEvent.KEYCODE_ALT_LEFT),
  ),
  RIGHT_ALT(
    "right_alt",
    R.string.keycombo_key_modifier_right_alt,
    KeyEvent.META_ALT_RIGHT_ON,
    KeyEvent.META_ALT_MASK,
    intArrayOf(KeyEvent.KEYCODE_ALT_RIGHT),
  ),
  CTRL(
    "ctrl",
    R.string.keycombo_key_modifier_ctrl,
    KeyEvent.META_CTRL_ON,
    KeyEvent.META_CTRL_MASK,
    intArrayOf(KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT),
  ),
  LEFT_CTRL(
    "left_ctrl",
    R.string.fast_search_key_left_ctrl,
    KeyEvent.META_CTRL_LEFT_ON,
    KeyEvent.META_CTRL_MASK,
    intArrayOf(KeyEvent.KEYCODE_CTRL_LEFT),
  ),
  RIGHT_CTRL(
    "right_ctrl",
    R.string.fast_search_key_right_ctrl,
    KeyEvent.META_CTRL_RIGHT_ON,
    KeyEvent.META_CTRL_MASK,
    intArrayOf(KeyEvent.KEYCODE_CTRL_RIGHT),
  ),
  META(
    "meta",
    R.string.keycombo_key_modifier_meta,
    KeyEvent.META_META_ON,
    KeyEvent.META_META_MASK,
    intArrayOf(KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT),
  ),
  LEFT_META(
    "left_meta",
    R.string.fast_search_key_left_meta,
    KeyEvent.META_META_LEFT_ON,
    KeyEvent.META_META_MASK,
    intArrayOf(KeyEvent.KEYCODE_META_LEFT),
  ),
  RIGHT_META(
    "right_meta",
    R.string.fast_search_key_right_meta,
    KeyEvent.META_META_RIGHT_ON,
    KeyEvent.META_META_MASK,
    intArrayOf(KeyEvent.KEYCODE_META_RIGHT),
  ),
  SYM(
    "sym",
    R.string.keycombo_key_modifier_sym,
    KeyEvent.META_SYM_ON,
    KeyEvent.META_SYM_ON,
    intArrayOf(KeyEvent.KEYCODE_SYM),
  ),
  FUNCTION(
    "function",
    R.string.keycombo_key_modifier_function,
    KeyEvent.META_FUNCTION_ON,
    KeyEvent.META_FUNCTION_ON,
    intArrayOf(KeyEvent.KEYCODE_FUNCTION),
  );

  /** Whether this key is, or is part of, [sandFlyKey], so that it can't be fast search's. */
  fun isPartOf(sandFlyKey: SandFlyKey?): Boolean =
    sandFlyKey != null && keyCodes.any { sandFlyKey.hasKey(it) }

  /**
   * Whether [metaState] has this key down, and no modifier other than Shift and the other key of
   * this kind, such as left Alt for Right Alt.
   */
  fun isDownAlone(metaState: Int): Boolean =
    metaState and this.metaState == this.metaState &&
      metaState and (MODIFIERS and kindMetaState.inv()) == 0

  companion object {
    /** The modifiers that can be down with a key, other than Shift and the lock keys. */
    private const val MODIFIERS =
      KeyEvent.META_ALT_MASK or
        KeyEvent.META_CTRL_MASK or
        KeyEvent.META_META_MASK or
        KeyEvent.META_SYM_ON or
        KeyEvent.META_FUNCTION_ON

    /** The value saved while fast search is off. */
    const val OFF = "off"

    /** The keys that can be fast search's where [sandFlyKey] is the SandFly key. */
    @JvmStatic
    fun available(sandFlyKey: SandFlyKey?): List<FastSearchKey> =
      entries.filter { !it.isPartOf(sandFlyKey) }

    /** The key saved as [value], or null for none or one that is part of [sandFlyKey]. */
    @JvmStatic
    fun of(value: String?, sandFlyKey: SandFlyKey?): FastSearchKey? =
      entries.firstOrNull { it.value == value }?.takeUnless { it.isPartOf(sandFlyKey) }

    /** The fast search key chosen, or null while fast search is off or can't use the key chosen. */
    @JvmStatic
    fun chosen(context: Context, sandFlyKey: SandFlyKey?): FastSearchKey? =
      of(
        SharedPreferencesUtils.getSharedPreferences(context)
          .getString(context.getString(R.string.pref_fast_search_key), OFF),
        sandFlyKey,
      )

    /** Whether fast search wraps around from the last item to the first, and back. */
    @JvmStatic
    fun wraps(context: Context): Boolean =
      SharedPreferencesUtils.getSharedPreferences(context)
        .getBoolean(
          context.getString(R.string.pref_fast_search_wrap_key),
          context.resources.getBoolean(R.bool.pref_fast_search_wrap_default),
        )
  }
}

/**
 * Fast search in browse mode: the fast search key with a letter or digit focuses the next item
 * whose text starts with it, ignoring case and accents; with Shift too, the previous one. Takes the
 * letter key's press, repeats and release; the fast search key itself goes on as before.
 */
class FastSearch(
  private val context: Context,
  /**
   * Focuses the next item whose text starts with the given character, or the previous one, wrapping
   * around when asked. Returns whether an item was focused.
   */
  private val searcher: Searcher,
  /** Says that no item was found. */
  private val sayNotFound: Runnable,
) {
  /** Focuses an item whose text starts with a character. */
  fun interface Searcher {
    fun focusItemStartingWith(codePoint: Int, forward: Boolean, wrap: Boolean): Boolean
  }

  /** Keys whose press fast search took, so that their repeats and release are taken too. */
  private val keysTaken = mutableSetOf<Int>()

  /**
   * Takes [event] when it is a letter or digit pressed with the fast search key while [canSearch]
   * says fast search works now, or a repeat or release of such a key. [sandFlyKey] is the keymap's
   * SandFly key, which can't be fast search's.
   */
  fun onKeyEvent(event: KeyEvent, sandFlyKey: SandFlyKey?, canSearch: () -> Boolean): Boolean {
    val keyCode = event.keyCode
    return when (event.action) {
      KeyEvent.ACTION_UP -> keysTaken.remove(keyCode)
      KeyEvent.ACTION_DOWN -> {
        if (event.repeatCount > 0) return keyCode in keysTaken
        keysTaken.remove(keyCode)
        if (KeyEvent.isModifierKey(keyCode)) return false
        val key = FastSearchKey.chosen(context, sandFlyKey) ?: return false
        if (!key.isDownAlone(event.metaState)) return false
        val codePoint = typedCodePoint(event) ?: return false
        if (!canSearch()) return false
        keysTaken += keyCode
        val forward = event.metaState and KeyEvent.META_SHIFT_MASK == 0
        if (!searcher.focusItemStartingWith(codePoint, forward, FastSearchKey.wraps(context))) {
          sayNotFound.run()
        }
        true
      }
      else -> keyCode in keysTaken
    }
  }

  /** Forgets the keys taken, such as when SandFly stops. */
  fun clear() {
    keysTaken.clear()
  }

  companion object {
    /** The letter or digit [event]'s key types without modifiers, or null for other keys. */
    private fun typedCodePoint(event: KeyEvent): Int? {
      val unicode = event.getUnicodeChar(0)
      if (unicode == 0 || unicode and KeyCharacterMap.COMBINING_ACCENT != 0) return null
      return unicode.takeIf { Character.isLetterOrDigit(it) }
    }

    /** [codePoint] without its accents, in lower case, so that É, é and E are all e. */
    @JvmStatic
    fun fold(codePoint: Int): Int {
      val decomposed = Normalizer.normalize(String(Character.toChars(codePoint)), Normalizer.Form.NFD)
      return Character.toLowerCase(decomposed.codePointAt(0))
    }

    /** Whether the first letter or digit of [text] is [codePoint], ignoring case and accents. */
    @JvmStatic
    fun startsWith(text: CharSequence?, codePoint: Int): Boolean {
      if (text.isNullOrEmpty()) return false
      val string = text.toString()
      var i = 0
      while (i < string.length) {
        val c = string.codePointAt(i)
        if (Character.isLetterOrDigit(c)) return fold(c) == fold(codePoint)
        i += Character.charCount(c)
      }
      return false
    }

    /** How deep, and how many nodes, an item's text is looked for inside it. */
    private const val MAX_TEXT_DEPTH = 6
    private const val MAX_TEXT_NODES = 60

    /**
     * The text an item is read with first: its own text, description or label, or else that of the
     * first item inside it that SandFly focus doesn't go to, which the item is read with.
     */
    @JvmStatic
    fun itemText(
      node: AccessibilityNodeInfoCompat,
      labelManager: TalkBackLabelManager?,
    ): CharSequence? {
      val own = TalkBackLabelManager.getNodeText(node, labelManager)
      if (!own.isNullOrEmpty()) return own
      var visited = 0
      fun inside(parent: AccessibilityNodeInfoCompat, depth: Int): CharSequence? {
        if (depth > MAX_TEXT_DEPTH) return null
        for (i in 0 until parent.childCount) {
          if (++visited > MAX_TEXT_NODES) return null
          val child = parent.getChild(i) ?: continue
          if (!AccessibilityNodeInfoUtils.isVisible(child) ||
            AccessibilityNodeInfoUtils.shouldFocusNode(child)
          ) {
            continue
          }
          val text = TalkBackLabelManager.getNodeText(child, labelManager)
          if (!text.isNullOrEmpty()) return text
          inside(child, depth + 1)?.let {
            return it
          }
        }
        return null
      }
      return inside(node, 1)
    }

    /** Accepts the visible items SandFly focus goes to whose text starts with [codePoint]. */
    @JvmStatic
    fun itemFilter(
      codePoint: Int,
      labelManager: TalkBackLabelManager?,
    ): Filter<AccessibilityNodeInfoCompat> =
      Filter.node { node ->
        node != null &&
          AccessibilityNodeInfoUtils.isVisible(node) &&
          AccessibilityNodeInfoUtils.shouldFocusNode(node) &&
          startsWith(itemText(node, labelManager), codePoint)
      }
  }
}
