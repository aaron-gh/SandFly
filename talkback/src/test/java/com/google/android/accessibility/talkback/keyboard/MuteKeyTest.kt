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
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The mute key outside text fields: its Mute key press command, or the keymap and Android. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MuteKeyTest {
  class Service : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}
  }

  private lateinit var service: Service
  private val performed = mutableListOf<String>()
  private lateinit var muteKey: MuteKey

  @Before
  fun setUp() {
    service = Robolectric.buildService(Service::class.java).create().get()
    muteKey = MuteKey(service) { performed += it }
  }

  private fun setCommand(value: String) {
    SharedPreferencesUtils.getSharedPreferences(service)
      .edit()
      .putString(service.getString(R.string.pref_mute_key_press_key), value)
      .commit()
  }

  private fun key(action: Int, keyCode: Int = KeyEvent.KEYCODE_VOLUME_MUTE, repeat: Int = 0) =
    KeyEvent(0, 0, action, keyCode, repeat)

  @Test
  fun pressRunsItsCommandOnceAndTakesTheWholePress() {
    setCommand("read_from_top")
    assertTrue(muteKey.onKeyEvent(key(KeyEvent.ACTION_DOWN), active = true))
    assertTrue(muteKey.onKeyEvent(key(KeyEvent.ACTION_DOWN, repeat = 1), active = true))
    assertTrue(muteKey.onKeyEvent(key(KeyEvent.ACTION_UP), active = true))
    assertEquals(listOf("read_from_top"), performed)
  }

  @Test
  fun pressWithoutCommandGoesOn() {
    assertFalse(muteKey.onKeyEvent(key(KeyEvent.ACTION_DOWN), active = true))
    assertFalse(muteKey.onKeyEvent(key(KeyEvent.ACTION_UP), active = true))
    setCommand(service.getString(R.string.shortcut_value_unassigned))
    assertFalse(muteKey.onKeyEvent(key(KeyEvent.ACTION_DOWN), active = true))
    assertFalse(muteKey.onKeyEvent(key(KeyEvent.ACTION_UP), active = true))
    assertTrue(performed.isEmpty())
  }

  @Test
  fun pressWhileSuspendedGoesOn() {
    setCommand("read_from_top")
    assertFalse(muteKey.onKeyEvent(key(KeyEvent.ACTION_DOWN), active = false))
    assertFalse(muteKey.onKeyEvent(key(KeyEvent.ACTION_UP), active = true))
    assertTrue(performed.isEmpty())
  }

  @Test
  fun releaseAfterResetGoesOn() {
    setCommand("read_from_top")
    assertTrue(muteKey.onKeyEvent(key(KeyEvent.ACTION_DOWN), active = true))
    muteKey.reset()
    assertFalse(muteKey.onKeyEvent(key(KeyEvent.ACTION_UP), active = true))
  }

  @Test
  fun otherKeysGoOn() {
    setCommand("read_from_top")
    for (keyCode in listOf(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_MUTE, KeyEvent.KEYCODE_A)) {
      assertFalse(muteKey.onKeyEvent(key(KeyEvent.ACTION_DOWN, keyCode), active = true))
      assertFalse(muteKey.onKeyEvent(key(KeyEvent.ACTION_UP, keyCode), active = true))
    }
    assertTrue(performed.isEmpty())
  }

  @Test
  fun dictatesInTextFieldsByDefault() {
    assertTrue(MuteKey.dictatesInTextFields(service))
  }
}
