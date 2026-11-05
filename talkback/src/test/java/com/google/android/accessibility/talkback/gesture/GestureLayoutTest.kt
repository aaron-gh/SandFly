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

package com.google.android.accessibility.talkback.gesture

import android.content.Context
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.commands.Commands
import com.google.android.accessibility.utils.FormFactorUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Checks that each gesture layout keeps its own commands and VoiceOver's defaults. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GestureLayoutTest {

  private val context = RuntimeEnvironment.getApplication()
  private val prefs = context.getSharedPreferences("gesture_layout_test", Context.MODE_PRIVATE)
  private val up = context.getString(R.string.pref_shortcut_up_key)
  private val upDefault = context.getString(R.string.pref_shortcut_up_default)
  private val unassigned = context.getString(R.string.shortcut_value_unassigned)

  @After
  fun forget() {
    prefs.edit().clear().commit()
  }

  @Test
  fun sandFlyGesturesUntilChosen() {
    assertFalse(GestureLayout.isChosen(prefs))
    assertEquals(GestureLayout.SANDFLY, GestureLayout.current(prefs))
    assertEquals(up, GestureLayout.key(prefs, up))
    assertEquals(upDefault, GestureLayout.read(context, prefs, up, upDefault))
  }

  @Test
  fun savingTheListValueAloneIsNoChoice() {
    // The Gestures screen's list saves its value when the screen opens.
    prefs.edit().putString(GestureLayout.KEY, GestureLayout.SANDFLY).commit()
    assertFalse(GestureLayout.isChosen(prefs))
    GestureLayout.markAskPending(prefs)
    assertTrue(GestureLayout.isAskPending(prefs))
    GestureLayout.select(prefs, GestureLayout.SANDFLY)
    assertTrue(GestureLayout.isChosen(prefs))
    assertFalse(GestureLayout.isAskPending(prefs))
  }

  @Test
  fun voiceOverGesturesHaveVoiceOverDefaults() {
    GestureLayout.select(prefs, GestureLayout.VOICEOVER)
    assertTrue(GestureLayout.isChosen(prefs))
    val swipeUp = context.getString(R.string.pref_shortcut_2finger_swipe_up_key)
    assertEquals(
      context.getString(R.string.shortcut_value_read_from_top),
      GestureLayout.read(context, prefs, swipeUp, "SCROLL_UP"),
    )
    val tripleTap = context.getString(R.string.pref_shortcut_3finger_3tap_key)
    assertEquals(
      context.getString(R.string.shortcut_value_show_hide_screen),
      GestureLayout.read(context, prefs, tripleTap, "COPY"),
    )
  }

  @Test
  fun gesturesVoiceOverLacksAreUnassigned() {
    GestureLayout.select(prefs, GestureLayout.VOICEOVER)
    for (keyRes in
      listOf(
        R.string.pref_shortcut_up_and_left_key,
        R.string.pref_shortcut_4finger_1tap_key,
        R.string.pref_shortcut_4finger_swipe_up_key,
        R.string.pref_shortcut_2finger_swipe_left_key,
        R.string.pref_shortcut_back_tap_double_key,
      )) {
      val key = context.getString(keyRes)
      assertEquals(key, unassigned, GestureLayout.read(context, prefs, key, "HOME"))
    }
  }

  @Test
  fun eachLayoutKeepsItsOwnChanges() {
    GestureLayout.select(prefs, GestureLayout.VOICEOVER)
    prefs.edit().putString(GestureLayout.key(prefs, up), "HOME").commit()
    assertEquals("HOME", GestureLayout.read(context, prefs, up, upDefault))
    GestureLayout.select(prefs, GestureLayout.SANDFLY)
    assertEquals(upDefault, GestureLayout.read(context, prefs, up, upDefault))
    GestureLayout.select(prefs, GestureLayout.VOICEOVER)
    assertEquals("HOME", GestureLayout.read(context, prefs, up, upDefault))
  }

  @Test
  fun choosingALayoutSetsTwoFingerSwipes() {
    GestureLayout.select(prefs, GestureLayout.VOICEOVER)
    assertTrue(prefs.getBoolean(GestureLayout.TWO_FINGER_SWIPES_KEY, false))
    GestureLayout.select(prefs, GestureLayout.SANDFLY)
    assertFalse(prefs.getBoolean(GestureLayout.TWO_FINGER_SWIPES_KEY, true))
  }

  @Test
  fun voiceOverDefaultsAreGesturesAndCommands() {
    FormFactorUtils.initialize(context)
    val gestures = context.resources.getStringArray(R.array.pref_shortcut_keys).toSet()
    // The one-finger double tap and double tap and hold have no row; they keep SandFly's commands.
    val doubleTaps =
      mapOf(
        R.string.pref_shortcut_1finger_2tap_key to R.string.pref_shortcut_1finger_2tap_default,
        R.string.pref_shortcut_1finger_2tap_hold_key to
          R.string.pref_shortcut_1finger_2tap_hold_default,
      )
    val defaults = VoiceOverGestures.defaults(context)
    for ((key, value) in doubleTaps) {
      assertEquals(context.getString(value), defaults[context.getString(key)])
    }
    val doubleTapKeys = doubleTaps.keys.map { context.getString(it) }.toSet()
    for ((key, value) in defaults - doubleTapKeys) {
      assertTrue(key, key in gestures)
      assertNotNull(value, Commands.label(context, value))
    }
  }

  @Test
  fun positionInListAndTable() {
    assertEquals("Item 3 of 10", ItemPosition.text(context, 2, 0, 10, 1))
    assertEquals("Item 2 of 5", ItemPosition.text(context, 0, 1, 1, 5))
    assertEquals("Row 2 of 4, column 3 of 5", ItemPosition.text(context, 1, 2, 4, 5))
    assertEquals("Item 1 of 1", ItemPosition.text(context, 0, 0, 1, 1))
    assertNull(ItemPosition.text(context, 0, 0, -1, -1))
    assertNull(ItemPosition.text(context, 12, 0, 10, 1))
  }
}
