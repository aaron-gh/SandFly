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

package com.google.android.accessibility.talkback.selector

import android.content.Context
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor
import com.google.android.accessibility.talkback.selector.SelectorController.ContextualSetting
import com.google.android.accessibility.talkback.selector.SelectorController.Setting
import com.google.android.accessibility.utils.ValueSteps

/**
 * The Clipboard history reading control, in edit boxes: swipes go through the texts of the
 * clipboard history, newest first, and double-tap pastes the one said last into the edit box.
 * Never selected automatically; it is left when focus leaves the edit box.
 */
class ClipboardHistorySetting(private val accessibilityFocusMonitor: AccessibilityFocusMonitor) :
  ContextualSetting {

  /** The text said last, which double-tap pastes, until focus moves. */
  var chosen: String? = null
    private set

  override fun getSetting(): Setting = Setting.CLIPBOARD_HISTORY

  override fun isNodeSupportSetting(context: Context, node: AccessibilityNodeInfoCompat?): Boolean =
    editBox(node) != null

  override fun shouldActivateSetting(context: Context, node: AccessibilityNodeInfoCompat?): Boolean =
    false

  /**
   * The edit box that [node], or else the item with accessibility focus, is or types into, such as
   * a key of the keyboard; null when there is none.
   */
  fun editBox(node: AccessibilityNodeInfoCompat?): AccessibilityNodeInfoCompat? {
    val focused =
      node ?: accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false)
    return focused
      ?.let { accessibilityFocusMonitor.getNodeForEditingActions(it) }
      ?.takeIf { it.isEditable }
  }

  /**
   * Chooses the text after or before the one said last among [entries], newest first, and returns
   * it; null when [entries] is empty.
   */
  fun step(entries: List<String>, isNext: Boolean): String? {
    chosen = next(entries, chosen, isNext)
    return chosen
  }

  /** Forgets the text said last, such as when focus moves. */
  fun reset() {
    chosen = null
  }

  companion object {
    /**
     * The text after or before [chosen] among [entries]: the first, or the last going back, when
     * [chosen] isn't among them. Null when [entries] is empty.
     */
    @JvmStatic
    fun next(entries: List<String>, chosen: String?, isNext: Boolean): String? {
      if (entries.isEmpty()) return null
      val index = chosen?.let { entries.indexOf(it) } ?: -1
      val newIndex =
        if (index < 0) {
          if (isNext) 0 else entries.size - 1
        } else {
          ValueSteps.step(index, entries.size, isNext)
        }
      return entries[newIndex]
    }
  }
}
