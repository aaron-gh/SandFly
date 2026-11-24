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

package com.google.android.accessibility.talkback.actor.helper

import android.content.Context
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.focusmanagement.action.NavigationAction
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo
import com.google.android.accessibility.talkback.keyboard.ArrowKeyNav
import com.google.android.accessibility.talkback.keyboard.Keymap
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.monitor.InputModeTracker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Whether keyboard navigation gives the item it moves to input focus, with smart browse mode on. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FocusActorHelperTest {
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    FormFactorUtils.initialize(context)
    SharedPreferencesUtils.getSharedPreferences(context).edit().clear().commit()
  }

  private fun useKeymap(keymap: Keymap) {
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putString(context.getString(R.string.pref_select_keymap_key), context.getString(keymap.valueRes))
      .commit()
  }

  private val textField: AccessibilityNodeInfoCompat
    get() =
      AccessibilityNodeInfoCompat.wrap(
        AccessibilityNodeInfo.obtain(View(context)).apply {
          isFocusable = true
          isEditable = true
        }
      )

  private fun keyboardNavigation(): FocusActionInfo =
    FocusActionInfo.builder()
      .setSourceAction(FocusActionInfo.LOGICAL_NAVIGATION)
      .setNavigationAction(
        NavigationAction.Builder()
          .setAction(NavigationAction.DIRECTIONAL_NAVIGATION)
          .setInputMode(InputModeTracker.INPUT_MODE_KEYBOARD)
          .build()
      )
      .build()

  private fun syncs(node: AccessibilityNodeInfoCompat) =
    FocusActorHelper.shouldSyncInputFocusToAccessibilityFocus(context, node, keyboardNavigation())

  @Test
  fun keyboardNavigationGivesATextFieldInputFocusWithoutArrowKeyNavigation() {
    useKeymap(Keymap.SANDFLY)
    assertTrue(syncs(textField))
    useKeymap(Keymap.NVDA)
    assertTrue(syncs(textField))
  }

  @Test
  fun keyboardNavigationLeavesInputFocusWhileArrowKeyNavigationIsOn() {
    useKeymap(Keymap.SANDFLY)
    ArrowKeyNav.setOn(context, true)
    assertFalse(syncs(textField))
  }

  @Test
  fun keyboardPhoneNavigationLeavesInputFocusWithArrowKeyNavigationOnOrOff() {
    for (keymap in listOf(Keymap.KEYBOARD_PHONE, Keymap.KEYBOARD_PHONE_SINGLE_KEYS)) {
      useKeymap(keymap)
      assertFalse(syncs(textField))
      ArrowKeyNav.setOn(context, false)
      assertFalse(syncs(textField))
    }
  }
}
