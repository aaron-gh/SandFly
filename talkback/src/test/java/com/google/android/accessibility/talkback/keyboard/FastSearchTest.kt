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
import android.view.KeyEvent
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.ServiceStateListener
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/** Browse mode's fast search: the fast search key with a letter or digit focuses an item. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FastSearchTest {
  private lateinit var context: Context
  private lateinit var manager: KeyComboManager

  /** The searches run: the character, whether forward, whether wrapping. */
  private val searches = mutableListOf<Triple<Char, Boolean, Boolean>>()
  private val spoken = mutableListOf<String>()
  private var found = true
  private var time = 1_000L

  private val rightAlt = KeyEvent.META_ALT_ON or KeyEvent.META_ALT_RIGHT_ON
  private val leftAlt = KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
  private val shift = KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
  private val ctrl = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    FormFactorUtils.initialize(context)
    SharedPreferencesUtils.getSharedPreferences(context).edit().clear().commit()
    manager =
      KeyComboManager.create(context).apply {
        onServiceStateChanged(ServiceStateListener.SERVICE_STATE_ACTIVE)
        setFastSearch(
          { codePoint, forward, wrap ->
            searches += Triple(codePoint.toChar(), forward, wrap)
            found
          },
          { spoken += it },
        )
      }
    ReflectionHelpers.setField(manager, "browseModeEnabled", true)
  }

  private fun choose(key: String) {
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putString(context.getString(R.string.pref_fast_search_key), key)
      .commit()
  }

  private fun key(action: Int, keyCode: Int, metaState: Int, repeat: Int = 0): Boolean {
    time += 10
    return manager.onKeyEvent(KeyEvent(time, time, action, keyCode, repeat, metaState), null)
  }

  /** Presses [keyCode] with [metaState] held, returning whether its press and release were taken. */
  private fun press(keyCode: Int, metaState: Int): Pair<Boolean, Boolean> {
    val down = key(KeyEvent.ACTION_DOWN, keyCode, metaState)
    val up = key(KeyEvent.ACTION_UP, keyCode, metaState)
    time += 1_000
    return down to up
  }

  @Test
  fun offByDefault() {
    assertEquals(false to false, press(KeyEvent.KEYCODE_A, rightAlt))
    assertTrue(searches.isEmpty())
  }

  @Test
  fun keyWithALetterFocusesTheNextItem() {
    choose(FastSearchKey.RIGHT_ALT.value)
    assertEquals(true to true, press(KeyEvent.KEYCODE_A, rightAlt))
    assertEquals(listOf(Triple('a', true, true)), searches)
  }

  @Test
  fun shiftFocusesThePreviousItem() {
    choose(FastSearchKey.RIGHT_ALT.value)
    press(KeyEvent.KEYCODE_M, rightAlt or shift)
    assertEquals(listOf(Triple('m', false, true)), searches)
  }

  @Test
  fun digitsSearchToo() {
    choose(FastSearchKey.RIGHT_ALT.value)
    press(KeyEvent.KEYCODE_5, rightAlt)
    assertEquals(listOf(Triple('5', true, true)), searches)
  }

  @Test
  fun repeatsAreTakenWithoutSearchingAgain() {
    choose(FastSearchKey.RIGHT_ALT.value)
    assertTrue(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A, rightAlt))
    assertTrue(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A, rightAlt, repeat = 1))
    assertTrue(key(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_A, rightAlt))
    assertEquals(1, searches.size)
  }

  @Test
  fun otherKeysAndModifiersAreLeftAlone() {
    choose(FastSearchKey.RIGHT_ALT.value)
    press(KeyEvent.KEYCODE_A, leftAlt)
    press(KeyEvent.KEYCODE_A, rightAlt or ctrl)
    press(KeyEvent.KEYCODE_TAB, rightAlt)
    assertTrue(searches.isEmpty())
  }

  @Test
  fun eitherKeyOfAKindWorksForTheKind() {
    choose(FastSearchKey.ALT.value)
    press(KeyEvent.KEYCODE_A, leftAlt)
    press(KeyEvent.KEYCODE_B, rightAlt)
    assertEquals(listOf('a', 'b'), searches.map { it.first })
  }

  @Test
  fun onlyInBrowseMode() {
    choose(FastSearchKey.RIGHT_ALT.value)
    ReflectionHelpers.setField(manager, "browseModeEnabled", false)
    assertEquals(false to false, press(KeyEvent.KEYCODE_A, rightAlt))
    assertTrue(searches.isEmpty())
  }

  @Test
  fun saysNotFoundWithoutWrappingWhenWrapAroundIsOff() {
    choose(FastSearchKey.RIGHT_CTRL.value)
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putBoolean(context.getString(R.string.pref_fast_search_wrap_key), false)
      .commit()
    found = false
    press(KeyEvent.KEYCODE_Z, KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_RIGHT_ON)
    assertEquals(listOf(Triple('z', true, false)), searches)
    assertEquals(listOf(context.getString(R.string.granular_editing_not_found)), spoken)
  }

  @Test
  fun theSandFlyKeyCantBeTheFastSearchKey() {
    // The SandFly keymap's SandFly key is Meta.
    choose(FastSearchKey.LEFT_META.value)
    press(KeyEvent.KEYCODE_A, KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON)
    assertTrue(searches.isEmpty())
  }

  @Test
  fun availableKeysLeaveOutTheSandFlyKey() {
    val withMeta = FastSearchKey.available(SandFlyKey.META)
    assertFalse(FastSearchKey.META in withMeta)
    assertFalse(FastSearchKey.RIGHT_META in withMeta)
    assertTrue(FastSearchKey.RIGHT_ALT in withMeta)
    val withRightAlt = FastSearchKey.available(SandFlyKey.RIGHT_ALT)
    assertFalse(FastSearchKey.ALT in withRightAlt)
    assertFalse(FastSearchKey.RIGHT_ALT in withRightAlt)
    assertTrue(FastSearchKey.LEFT_ALT in withRightAlt)
    val withCtrlAlt = FastSearchKey.available(SandFlyKey.CTRL_ALT)
    assertEquals(
      listOf(
        FastSearchKey.META,
        FastSearchKey.LEFT_META,
        FastSearchKey.RIGHT_META,
        FastSearchKey.SYM,
        FastSearchKey.FUNCTION,
      ),
      withCtrlAlt,
    )
    assertEquals(FastSearchKey.entries, FastSearchKey.available(SandFlyKey.INSERT))
    assertNull(FastSearchKey.of(FastSearchKey.ALT.value, SandFlyKey.ALT))
  }

  @Test
  fun startsWithIgnoresCaseAccentsAndLeadingSymbols() {
    assertTrue(FastSearch.startsWith("Écran", 'e'.code))
    assertTrue(FastSearch.startsWith("email", 'E'.code))
    assertTrue(FastSearch.startsWith("  (555) Call", '5'.code))
    assertTrue(FastSearch.startsWith("• Item", 'i'.code))
    assertFalse(FastSearch.startsWith("Bob", 'a'.code))
    assertFalse(FastSearch.startsWith("", 'a'.code))
    assertFalse(FastSearch.startsWith(null, 'a'.code))
  }
}
