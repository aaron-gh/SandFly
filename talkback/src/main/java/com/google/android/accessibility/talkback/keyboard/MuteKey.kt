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

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.dictation.SandflyDictation
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.KeyboardUtils
import com.google.android.accessibility.utils.Role
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * The mute key of phones that have one, such as the Zinwa Q25. In a text field being edited, with
 * Hold mute key to dictate in text fields on, holding it dictates and releasing it stops
 * recording; pressed during a dictation into a field, it stops that dictation. Elsewhere it runs
 * the Mute key press command. A press it doesn't take goes on to the keymap's shortcuts, then to
 * Android.
 */
class MuteKey(
  private val service: AccessibilityService,
  /** Runs a command by its value, or does nothing when commands can't run yet. */
  private val perform: (String) -> Unit,
) {
  private enum class Press {
    /** Not taken: the press goes on to the keymap and Android. */
    NONE,
    /** Taken: started a dictation, which the release stops. */
    DICTATING,
    /** Taken for anything else. */
    TAKEN,
  }

  private var press = Press.NONE

  /** Takes [event] when it is a mute key press, or its release, that this handles. */
  fun onKeyEvent(event: KeyEvent, active: Boolean): Boolean {
    if (event.keyCode != KeyEvent.KEYCODE_VOLUME_MUTE) return false
    return when (event.action) {
      KeyEvent.ACTION_DOWN -> {
        if (event.repeatCount == 0) {
          press = if (active) onPress() else Press.NONE
        }
        press != Press.NONE
      }
      KeyEvent.ACTION_UP -> {
        val released = press
        press = Press.NONE
        if (released == Press.DICTATING) {
          SandflyDictation.stopFieldRecording()
        }
        released != Press.NONE
      }
      else -> press != Press.NONE
    }
  }

  /**
   * Forgets a press, as when SandFly is suspended, so that its release isn't taken; a dictation it
   * started stops recording.
   */
  fun reset() {
    if (press == Press.DICTATING) SandflyDictation.stopFieldRecording()
    press = Press.NONE
  }

  private fun onPress(): Press {
    if (dictatesInTextFields(service) && !inCall()) {
      val field = editingTextField()
      if (field != null) {
        if (SandflyDictation.isActive && !SandflyDictation.isListening) {
          // A dictation into a field stops, as Dictate pressed again does.
          SandflyDictation.dictate(null)
          return Press.TAKEN
        }
        if (!SandflyDictation.isActive) {
          return if (SandflyDictation.dictate(field)) Press.DICTATING else Press.TAKEN
        }
      }
    }
    val command = pressCommand(service) ?: return Press.NONE
    perform(command)
    return Press.TAKEN
  }

  private fun inCall(): Boolean = (service as? TalkBackService)?.isPhoneCallActive() == true

  /** The text field with input focus while a keyboard types into it, or null. */
  private fun editingTextField(): AccessibilityNodeInfoCompat? {
    if (!KeyboardUtils.isKeyboardActive(service)) return null
    val focus =
      try {
        AccessibilityNodeInfoUtils.toCompat(service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT))
      } catch (e: RuntimeException) {
        // The window may go away while it is asked.
        LogUtils.w(TAG, "No input focus: %s", e)
        null
      } ?: return null
    return focus.takeIf {
      Role.getRole(it) == Role.ROLE_EDIT_TEXT && it.isFocused && it.isVisibleToUser
    }
  }

  companion object {
    private const val TAG = "MuteKey"

    /** Whether the phone has a mute key. */
    @JvmStatic
    fun isAvailable(): Boolean = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_VOLUME_MUTE)

    /** Whether Hold mute key to dictate in text fields is on. */
    @JvmStatic
    fun dictatesInTextFields(context: Context): Boolean =
      SharedPreferencesUtils.getBooleanPref(
        SharedPreferencesUtils.getSharedPreferences(context),
        context.resources,
        R.string.pref_mute_key_dictates_key,
        R.bool.pref_mute_key_dictates_default,
      )

    /** The command of Mute key press, or null when it has none. */
    private fun pressCommand(context: Context): String? {
      val unassigned = context.getString(R.string.shortcut_value_unassigned)
      return SharedPreferencesUtils.getSharedPreferences(context)
        .getString(context.getString(R.string.pref_mute_key_press_key), unassigned)
        ?.takeIf { it.isNotEmpty() && it != unassigned }
    }
  }
}
