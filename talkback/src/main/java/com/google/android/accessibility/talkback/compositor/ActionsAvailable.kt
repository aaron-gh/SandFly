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

package com.google.android.accessibility.talkback.compositor

import android.content.SharedPreferences
import android.text.TextUtils
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.Role

/**
 * The Actions available sound and speech, for items with actions of their app beyond the standard
 * ones, such as Reply and Forward on a chat message.
 */
object ActionsAvailable {
  /** When the sound plays: [TIMING_FOCUS] or [TIMING_AFTER_SPEECH]. */
  const val PREF_SOUND_TIMING = "pref_actions_available_sound_timing"
  const val TIMING_FOCUS = "focus"
  const val TIMING_AFTER_SPEECH = "after_speech"

  /** Whether "Actions available" is spoken after the item. Off by default. */
  const val PREF_SPEAK = "pref_speak_actions_available"

  @JvmStatic
  fun isSoundAfterSpeech(prefs: SharedPreferences): Boolean =
    prefs.getString(PREF_SOUND_TIMING, TIMING_FOCUS) == TIMING_AFTER_SPEECH

  @JvmStatic fun isSpoken(prefs: SharedPreferences): Boolean = prefs.getBoolean(PREF_SPEAK, false)

  /**
   * Whether [node] has actions of its app: custom actions, whose IDs are not Android's, with a
   * label. Android's actions, such as click, long click, scroll, focus, select, expand, collapse,
   * dismiss and drag, don't count, even with a label of the app's, so a plain button has none. Keys
   * of on-screen keyboards don't count.
   */
  @JvmStatic
  fun has(node: AccessibilityNodeInfoCompat?): Boolean {
    if (node == null || !hasCustomAction(node.actionList)) {
      return false
    }
    val role = Role.getRole(node)
    return role != Role.ROLE_TEXT_ENTRY_KEY && role != Role.ROLE_KEYBOARD_KEY
  }

  /** Whether [actions] has a custom action with a label. */
  @JvmStatic
  fun hasCustomAction(actions: List<AccessibilityActionCompat>?): Boolean =
    actions != null &&
      actions.any { AccessibilityNodeInfoUtils.isCustomAction(it) && !TextUtils.isEmpty(it.label) }
}
