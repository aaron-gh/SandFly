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

import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_DOWN
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_LEFT
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_RIGHT
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_UP
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.gestures.GestureConfiguration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * Checks which one-finger swipes run before the finger lifts, with Recognize swipes before lifting
 * off, from the commands of each gesture layout, app and change.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SwipesBeforeLiftDefaultsTest {

  private val context = RuntimeEnvironment.getApplication()
  private val prefs = SharedPreferencesUtils.getSharedPreferences(context)
  private val unassigned = context.getString(R.string.shortcut_value_unassigned)
  private val swipes = listOf(GESTURE_SWIPE_UP, GESTURE_SWIPE_DOWN, GESTURE_SWIPE_LEFT, GESTURE_SWIPE_RIGHT)

  @After
  fun forget() {
    prefs.edit().clear().commit()
    AppGestures.load(prefs)
    AppGestures.onWindowShown(null)
    GestureProfiles.load(prefs)
  }

  /** The swipes that run before the lift with the commands [mapping] has now. */
  private fun beforeLift(mapping: GestureShortcutMapping): List<Int> =
    swipes.filter {
      GestureConfiguration.runsBeforeLift(
        it,
        /* twoDirectionSwipesOff= */ false,
        /* whenNothingFollows= */ true,
        mapping::isAssigned,
      )
    }

  @Test
  fun sandFlyLayout_everySwipeWaitsForTheLift() {
    // Every back-and-forth and angle gesture has a command in SandFly's layout.
    assertEquals(emptyList<Int>(), beforeLift(GestureShortcutMapping(context)))
  }

  @Test
  fun voiceOverLayout_everySwipeRunsBeforeTheLift() {
    GestureLayout.select(prefs, GestureLayout.VOICEOVER)
    assertEquals(swipes, beforeLift(GestureShortcutMapping(context)))
  }

  @Test
  fun unassigningTheGesturesThatStartUp_onlyUpRunsBeforeTheLift() {
    val mapping = GestureShortcutMapping(context)
    prefs
      .edit()
      .putString(context.getString(R.string.pref_shortcut_up_and_down_key), unassigned)
      .putString(context.getString(R.string.pref_shortcut_up_and_left_key), unassigned)
      .putString(context.getString(R.string.pref_shortcut_up_and_right_key), unassigned)
      .commit()
    ShadowLooper.idleMainLooper()
    assertEquals(listOf(GESTURE_SWIPE_UP), beforeLift(mapping))
  }

  @Test
  fun assigningAnAngle_onlyItsDirectionWaits() {
    GestureLayout.select(prefs, GestureLayout.VOICEOVER)
    val mapping = GestureShortcutMapping(context)
    prefs
      .edit()
      .putString(
        GestureLayout.key(prefs, context.getString(R.string.pref_shortcut_up_and_right_key)),
        context.getString(R.string.shortcut_value_home),
      )
      .commit()
    ShadowLooper.idleMainLooper()
    assertEquals(listOf(GESTURE_SWIPE_DOWN, GESTURE_SWIPE_LEFT, GESTURE_SWIPE_RIGHT), beforeLift(mapping))
  }

  @Test
  fun appGesture_waitsOnlyInThatApp() {
    GestureLayout.select(prefs, GestureLayout.VOICEOVER)
    val mapping = GestureShortcutMapping(context)
    AppGestures.write(
      prefs,
      mapOf(
        "com.example.player" to
          AppGestures.App(
            "Player",
            mapOf(context.getString(R.string.pref_shortcut_up_and_right_key) to "FAST_FORWARD"),
          )
      ),
    )
    AppGestures.load(prefs)
    AppGestures.onWindowShown("com.example.player")
    assertEquals(listOf(GESTURE_SWIPE_DOWN, GESTURE_SWIPE_LEFT, GESTURE_SWIPE_RIGHT), beforeLift(mapping))
    AppGestures.onWindowShown("com.example.mail")
    assertEquals(swipes, beforeLift(mapping))
  }

  @Test
  fun profileThatUnassignsTheGesturesThatStartLeft_leftRunsBeforeTheLift() {
    val mapping = GestureShortcutMapping(context)
    val left =
      GestureProfiles.Profile(
        "a",
        "One hand",
        mapOf(
          context.getString(R.string.pref_shortcut_left_and_right_key) to unassigned,
          context.getString(R.string.pref_shortcut_left_and_up_key) to unassigned,
          context.getString(R.string.pref_shortcut_left_and_down_key) to unassigned,
        ),
      )
    GestureProfiles.write(prefs, listOf(left))
    GestureProfiles.select(context, prefs, "a")
    assertEquals(listOf(GESTURE_SWIPE_LEFT), beforeLift(mapping))
    GestureProfiles.select(context, prefs, "")
    assertEquals(emptyList<Int>(), beforeLift(mapping))
  }
}
