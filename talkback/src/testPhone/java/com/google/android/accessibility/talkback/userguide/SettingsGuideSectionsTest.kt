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

package com.google.android.accessibility.talkback.userguide

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.accessibility.talkback.commands.AppGesturesFragment
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The settings screens named for the Help action exist in the phone build. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsGuideSectionsTest {

  @Test
  fun everyScreenIsAFragmentOrActivityThatCanBeShown() {
    val wrong =
      SettingsGuideSections.SECTIONS.keys.filter { name ->
        val type =
          try {
            Class.forName(name, false, javaClass.classLoader)
          } catch (e: ClassNotFoundException) {
            return@filter true
          }
        Modifier.isAbstract(type.modifiers) ||
          !(Fragment::class.java.isAssignableFrom(type) || Activity::class.java.isAssignableFrom(type))
      }
    assertEquals(emptyList<String>(), wrong)
  }

  @Test
  fun appGesturesFromKeyboardShortcutsOpenAppKeyboardShortcuts() {
    val name = AppGesturesFragment::class.java.name
    val fromKeyboard =
      Bundle().apply { putString(AppGesturesFragment.ARG_SHOWN_FROM, AppGesturesFragment.FROM_KEYBOARD_SHORTCUTS) }
    assertEquals(SettingsGuideSections.APP_KEYBOARD_SHORTCUTS, SettingsGuideSections.section(name, fromKeyboard))
    assertEquals("gestures-in-apps", SettingsGuideSections.section(name, Bundle()))
  }
}
