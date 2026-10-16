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

package com.google.android.accessibility.talkback.trainingcommon.content

import android.content.Context
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.trainingcommon.TrainingIpcClient.ServiceData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Which keyboard tutorial texts show as written, for the user's keymap and shortcuts. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeyboardTutorialKeysTest {
  private val context: Context
    get() = RuntimeEnvironment.getApplication()

  private fun key(res: Int) = context.getString(res)

  private fun title(res: Int) = KeyboardTutorialKeys.title(context, key(res))!!

  private val windowKeys =
      listOf(
          R.string.keycombo_shortcut_navigate_previous_window,
          R.string.keycombo_shortcut_navigate_next_window)

  private val systemKeys =
      listOf(
          R.string.keycombo_shortcut_global_home,
          R.string.keycombo_shortcut_global_recents,
          R.string.keycombo_shortcut_global_back,
          R.string.keycombo_shortcut_global_notifications)

  /** Shortcuts as the default keymap has them: window keys set, system keys unassigned. */
  private fun defaultKeymap(): ServiceData {
    val data = ServiceData(context)
    for (res in windowKeys) {
      data.shortcuts.putKeyCombo(key(res), "Alt + Ctrl + Arrow", true, false)
    }
    for (res in systemKeys) {
      data.shortcuts.putKeyCombo(
          key(res), context.getString(R.string.keycombo_unassigned), true, false)
    }
    return data
  }

  @Test
  fun enhancedKeymapWithDefaultKeysKeepsTheText() {
    val data = ServiceData(context)
    for (res in windowKeys + systemKeys) {
      data.shortcuts.putKeyCombo(key(res), "Action + Alt + Ctrl + Arrow", true, true)
    }
    assertNull(
        KeyboardTutorialKeys.linesIfChanged(
            context, R.string.keyboard_tutorial_system_navigation_page_text, data))
  }

  @Test
  fun systemNavigationPageNamesOnlyTheWindowShortcuts() {
    val lines =
        KeyboardTutorialKeys.linesIfChanged(
            context, R.string.keyboard_tutorial_system_navigation_page_text, defaultKeymap())
    assertEquals(
        windowKeys.joinToString("\n") {
          context.getString(R.string.template_action_and_gesture, title(it), "Alt + Ctrl + Arrow")
        },
        lines)
  }

  @Test
  fun unassignedShortcutNamesTheSettingsScreen() {
    val data = ServiceData(context)
    val next = R.string.keycombo_shortcut_navigate_next_default
    data.shortcuts.putKeyCombo(
        key(next), context.getString(R.string.keycombo_unassigned), false, true)
    val path =
        context.getString(
            R.string.template_settings_path,
            context.getString(R.string.talkback_preferences_title),
            context.getString(R.string.title_pref_manage_keyboard_shortcuts))
    assertEquals(
        context.getString(R.string.template_action_and_gesture, title(next), path),
        KeyboardTutorialKeys.linesIfChanged(
            context, R.string.keyboard_tutorial_welcome_to_talkback_page_text, data))
  }

  @Test
  fun everyNamedShortcutHasATitle() {
    val missing =
        KeyboardTutorialKeys.keys()
            .map { key(it) }
            .filter { KeyboardTutorialKeys.title(context, it) == null }
    assertEquals(emptyList<String>(), missing)
  }
}
