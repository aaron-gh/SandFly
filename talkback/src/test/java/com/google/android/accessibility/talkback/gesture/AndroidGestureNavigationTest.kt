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
import android.graphics.Rect
import android.graphics.Region
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidGestureNavigationTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private val prefs = SharedPreferencesUtils.getSharedPreferences(context)

  @Before
  fun setUp() {
    FormFactorUtils.initialize(context)
  }

  @After
  fun tearDown() {
    prefs.edit().clear().commit()
  }

  @Test
  fun theRegionIsTheSideAndBottomStrips() {
    val region = AndroidGestureNavigation.region(Rect(0, 0, 1080, 2340), left = 60, right = 50, bottom = 80)
    val expected =
      Region(0, 0, 60, 2340).apply {
        op(Rect(1030, 0, 1080, 2340), Region.Op.UNION)
        op(Rect(0, 2260, 1080, 2340), Region.Op.UNION)
      }
    assertEquals(expected, region)
    // The middle of the screen and the top edge stay with touch exploration.
    assertFalse(region.contains(540, 1170))
    assertFalse(region.contains(540, 0))
  }

  @Test
  fun sidesWithoutBackGesturesAddNothing() {
    // Gesture navigation with Back at the bottom, as Samsung's Swipe from bottom has it.
    val region = AndroidGestureNavigation.region(Rect(0, 0, 1080, 2340), left = 0, right = 0, bottom = 80)
    assertEquals(Region(0, 2260, 1080, 2340), region)
  }

  @Test
  fun edgeAndCornerGesturesAreRecognizedByTheirSettings() {
    for (key in
      listOf(
        R.string.pref_shortcut_edge_bottom_up_key,
        R.string.pref_shortcut_edge_bottom_long_up_key,
        R.string.pref_shortcut_edge_left_top_right_key,
        R.string.pref_shortcut_edge_along_right_down_key,
        R.string.pref_shortcut_corner_top_left_tap_key,
        R.string.pref_shortcut_corner_bottom_right_triple_tap_hold_key,
      )) {
      assertTrue(AndroidGestureNavigation.isEdgeOrCornerGesture(context.getString(key)))
    }
    assertFalse(
      AndroidGestureNavigation.isEdgeOrCornerGesture(
        context.getString(R.string.pref_shortcut_up_key)
      )
    )
  }

  @Test
  fun nothingIsHiddenWhileTheSwitchIsOff() {
    val edges = context.getString(R.string.pref_category_edge_shortcuts_key)
    assertFalse(AndroidGestureNavigation.isSwitchedOn(context))
    assertFalse(AndroidGestureNavigation.hidesCategory(context, edges))
  }

  @Test
  fun theSwitchAppliesOnlyWithGestureNavigation() {
    prefs
      .edit()
      .putBoolean(context.getString(R.string.pref_android_gesture_navigation_key), true)
      .commit()
    assertTrue(AndroidGestureNavigation.isSwitchedOn(context))
    val edges = context.getString(R.string.pref_category_edge_shortcuts_key)
    val gestural = AndroidGestureNavigation.isGestureNavigation(context)
    assertEquals(gestural, AndroidGestureNavigation.isOn(context))
    assertEquals(gestural, AndroidGestureNavigation.hidesCategory(context, edges))
    assertFalse(
      AndroidGestureNavigation.hidesCategory(
        context,
        context.getString(R.string.pref_category_1finger_shortcuts_key),
      )
    )
  }
}
