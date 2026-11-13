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

import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.gestures.GestureConfiguration
import org.junit.After
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

/** Checks that a gesture profile follows Default's Recognize swipes before lifting unless set. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SwipesBeforeLiftProfilesTest {

  private val context = RuntimeEnvironment.getApplication()
  private val prefs = SharedPreferencesUtils.getSharedPreferences(context)

  @Before
  fun setUp() {
    FormFactorUtils.initialize(context)
    prefs.edit().clear().commit()
  }

  @After
  fun forget() {
    prefs.edit().clear().commit()
    GestureProfiles.load(prefs)
    GestureConfiguration.setSwipesBeforeLift(false)
  }

  private fun setDefault(on: Boolean) {
    prefs.edit().putBoolean(context.getString(R.string.pref_swipes_before_lift_key), on).commit()
  }

  private fun useProfile(swipesBeforeLift: Boolean?): GestureProfiles.Profile {
    val profile = GestureProfiles.Profile("p", "Walking", emptyMap(), swipesBeforeLift)
    GestureProfiles.write(prefs, listOf(profile))
    GestureProfiles.select(context, prefs, "p")
    return profile
  }

  @Test
  fun profileFollowsDefaultWhenOn() {
    setDefault(true)
    val profile = useProfile(null)
    assertTrue(SwipesBeforeLift.isOnInProfile(context, profile))
    assertTrue(SwipesBeforeLift.isOn(context))
    assertTrue(GestureConfiguration.swipesBeforeLift())
  }

  @Test
  fun profileFollowsDefaultWhenOff() {
    setDefault(false)
    val profile = useProfile(null)
    assertFalse(SwipesBeforeLift.isOnInProfile(context, profile))
    assertFalse(SwipesBeforeLift.isOn(context))
  }

  @Test
  fun profileTurnedOffWhileDefaultIsOn() {
    setDefault(true)
    useProfile(false)
    assertFalse(SwipesBeforeLift.isOn(context))
    assertFalse(GestureConfiguration.swipesBeforeLift())
    assertTrue(SwipesBeforeLift.isOnByDefault(context))
    GestureProfiles.select(context, prefs, "")
    assertTrue(SwipesBeforeLift.isOn(context))
    assertTrue(GestureConfiguration.swipesBeforeLift())
  }

  @Test
  fun profileTurnedOnWhileDefaultIsOff() {
    setDefault(false)
    useProfile(true)
    assertTrue(SwipesBeforeLift.isOn(context))
    assertFalse(SwipesBeforeLift.isOnByDefault(context))
  }

  @Test
  fun valueSameAsDefaultIsSavedAsFollowingDefault() {
    setDefault(true)
    assertNull(SwipesBeforeLift.profileValue(context, true))
    assertEquals(false, SwipesBeforeLift.profileValue(context, false))
    setDefault(false)
    assertNull(SwipesBeforeLift.profileValue(context, false))
    assertEquals(true, SwipesBeforeLift.profileValue(context, true))
  }

  @Test
  fun valueIsKeptInTheSavedProfiles() {
    val profiles =
      listOf(
        GestureProfiles.Profile("a", "On", emptyMap(), true),
        GestureProfiles.Profile("b", "Off", emptyMap(), false),
        GestureProfiles.Profile("c", "Default", emptyMap(), null),
      )
    val json = GestureProfiles.toJson(profiles)
    assertEquals(profiles, GestureProfiles.fromJson(json))
    assertFalse(json.substringAfter("\"c\"").contains("swipesBeforeLift"))
  }
}
