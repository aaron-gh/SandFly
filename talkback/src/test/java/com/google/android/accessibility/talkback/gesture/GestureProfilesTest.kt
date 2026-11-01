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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Checks that the gesture profile in use gives gestures its own commands. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GestureProfilesTest {

  private val context = RuntimeEnvironment.getApplication()
  private val prefs = context.getSharedPreferences("gesture_profiles_test", Context.MODE_PRIVATE)

  private val driving =
    GestureProfiles.Profile(
      "a",
      "Driving",
      mapOf("pref_shortcut_up" to "FAST_FORWARD", "pref_shortcut_down" to "UNASSIGNED"),
    )
  private val reading = GestureProfiles.Profile("b", "Reading", emptyMap())

  @After
  fun forget() {
    prefs.edit().clear().apply()
    GestureProfiles.load(prefs)
  }

  @Test
  fun defaultGivesNoGestureACommand() {
    GestureProfiles.write(prefs, listOf(driving))
    GestureProfiles.load(prefs)
    assertNull(GestureProfiles.action("pref_shortcut_up"))
  }

  @Test
  fun profileGivesOnlyItsOwnGesturesACommand() {
    GestureProfiles.write(prefs, listOf(driving))
    GestureProfiles.select(context, prefs, "a")
    assertEquals("FAST_FORWARD", GestureProfiles.action("pref_shortcut_up"))
    assertEquals("UNASSIGNED", GestureProfiles.action("pref_shortcut_down"))
    assertNull(GestureProfiles.action("pref_shortcut_left"))
    assertNull(GestureProfiles.action(null))
  }

  @Test
  fun selectSaysTheNameAndDefaultIsNamed() {
    GestureProfiles.write(prefs, listOf(driving))
    assertEquals("Driving", GestureProfiles.select(context, prefs, "a"))
    assertEquals("a", GestureProfiles.activeId(prefs))
    assertEquals(
      context.getString(R.string.gesture_profile_default),
      GestureProfiles.select(context, prefs, ""),
    )
    assertNull(GestureProfiles.action("pref_shortcut_up"))
  }

  @Test
  fun deletedProfileCannotBeSelectedAndCountsAsDefault() {
    GestureProfiles.write(prefs, listOf(driving))
    GestureProfiles.select(context, prefs, "a")
    GestureProfiles.write(prefs, emptyList())
    GestureProfiles.load(prefs)
    assertEquals("", GestureProfiles.activeId(prefs))
    assertNull(GestureProfiles.action("pref_shortcut_up"))
    assertNull(GestureProfiles.select(context, prefs, "a"))
  }

  @Test
  fun adjacentGoesThroughDefaultAndTheProfilesInOrderAndWraps() {
    GestureProfiles.write(prefs, listOf(driving, reading))
    GestureProfiles.load(prefs)
    assertEquals("a", GestureProfiles.adjacentId(prefs, next = true))
    assertEquals("b", GestureProfiles.adjacentId(prefs, next = false))
    GestureProfiles.select(context, prefs, "b")
    assertEquals("", GestureProfiles.adjacentId(prefs, next = true))
    assertEquals("a", GestureProfiles.adjacentId(prefs, next = false))
  }

  @Test
  fun commandValuesNameTheirProfile() {
    GestureProfiles.write(prefs, listOf(driving))
    val value = GestureProfiles.valueOf("a")
    assertTrue(GestureProfiles.isAction(value))
    assertFalse(GestureProfiles.isAction("OPEN_APP:x/y"))
    assertEquals("a", GestureProfiles.idOf(value))
    assertEquals(
      context.getString(R.string.shortcut_use_gesture_profile, "Driving"),
      GestureProfiles.label(context, prefs, value),
    )
    assertEquals(
      context.getString(
        R.string.shortcut_use_gesture_profile,
        context.getString(R.string.gesture_profile_default),
      ),
      GestureProfiles.label(context, prefs, GestureProfiles.valueOf("")),
    )
    assertNull(GestureProfiles.label(context, prefs, GestureProfiles.valueOf("gone")))
  }

  @Test
  fun rowsReadAndChangeTheirProfilesGesture() {
    GestureProfiles.write(prefs, listOf(driving, reading))
    val up = GestureProfiles.rowKey("a", "pref_shortcut_up")
    val left = GestureProfiles.rowKey("a", "pref_shortcut_left")
    assertTrue(GestureProfiles.isRowKey(up))
    assertEquals("FAST_FORWARD", GestureProfiles.rowValue(prefs, up))
    assertEquals(GestureProfiles.DEFAULT_VALUE, GestureProfiles.rowValue(prefs, left))

    assertTrue(GestureProfiles.setRowValue(prefs, left, "READ_FROM_TOP"))
    assertTrue(GestureProfiles.setRowValue(prefs, up, GestureProfiles.DEFAULT_VALUE))
    val changed = GestureProfiles.read(prefs)
    assertEquals(
      mapOf("pref_shortcut_down" to "UNASSIGNED", "pref_shortcut_left" to "READ_FROM_TOP"),
      changed[0].gestures,
    )
    assertEquals(reading, changed[1])
  }

  @Test
  fun otherKeysAndDeletedProfilesHaveNoRowValue() {
    GestureProfiles.write(prefs, listOf(driving))
    assertFalse(GestureProfiles.isRowKey("pref_shortcut_up"))
    assertNull(GestureProfiles.rowValue(prefs, "pref_shortcut_up"))
    assertFalse(GestureProfiles.setRowValue(prefs, "pref_shortcut_up", "READ_FROM_TOP"))
    assertNull(GestureProfiles.rowValue(prefs, GestureProfiles.rowKey("gone", "pref_shortcut_up")))
    assertEquals(listOf(driving), GestureProfiles.read(prefs))
  }

  @Test
  fun jsonKeepsOrderNamesAndGestures() {
    val json = GestureProfiles.toJson(listOf(driving, reading))
    assertEquals(listOf(driving, reading), GestureProfiles.fromJson(json))
  }

  @Test
  fun brokenJsonReadsAsNoProfiles() {
    assertEquals(emptyList<GestureProfiles.Profile>(), GestureProfiles.fromJson("{"))
    assertEquals(emptyList<GestureProfiles.Profile>(), GestureProfiles.fromJson("[{\"name\":\"x\"}]"))
  }
}
