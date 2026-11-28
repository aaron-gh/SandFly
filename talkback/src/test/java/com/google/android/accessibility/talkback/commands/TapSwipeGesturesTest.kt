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

package com.google.android.accessibility.talkback.commands

import android.annotation.SuppressLint
import android.content.Context
import android.view.ContextThemeWrapper
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import com.google.android.accessibility.material.preference.AccessibilitySuitePreferenceCategory
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.base.GestureListPreference
import com.google.android.accessibility.talkback.preference.search.SettingsIndex
import com.google.android.accessibility.talkback.preference.search.SettingsSearch
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Checks the four categories inside Tap, then swipe on the Gestures screen. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TapSwipeGesturesTest {
  private lateinit var context: Context

  @Before
  fun setUp() {
    val app = RuntimeEnvironment.getApplication()
    FormFactorUtils.initialize(app)
    context = ContextThemeWrapper(app, R.style.TalkBackSettings)
    SharedPreferencesUtils.getSharedPreferences(context).edit().clear().commit()
  }

  @After
  fun forget() {
    SharedPreferencesUtils.getSharedPreferences(context).edit().clear().commit()
    context.getSharedPreferences("collapsible_categories", Context.MODE_PRIVATE).edit().clear().commit()
  }

  @SuppressLint("RestrictedApi")
  private fun gesturesScreen(): PreferenceScreen =
    PreferenceManager(context).inflateFromResource(context, R.xml.gesture_preferences, null)

  private fun s(id: Int): String = context.getString(id)

  @Test
  fun tapSwipeHasFourCategoriesOfTwelveGestures() {
    val categories = CommandBindings.gestureCategories(context, gesturesScreen(), false)
    val tapSwipe = categories.single { it.key == s(R.string.pref_category_tap_swipe_shortcuts_key) }
    assertEquals(emptyList<CommandBindings.GestureSlot>(), tapSwipe.gestures)
    assertEquals(
      listOf(
        s(R.string.two_finger_gestures_title),
        s(R.string.three_finger_gestures_title),
        s(R.string.four_finger_gestures_title),
        s(R.string.tap_swipe_2finger_tap_1finger_swipe_gestures_title),
      ),
      tapSwipe.subcategories.map { it.title.toString() },
    )
    for (category in tapSwipe.subcategories) {
      assertEquals(12, category.gestures.size)
    }
    assertTrue(
      tapSwipe.subcategories[3].gestures.all { it.baseKey.contains("_one_finger_swipe_") }
    )
    // The other categories have no categories inside them.
    assertTrue(categories.filter { it !== tapSwipe }.all { it.subcategories.isEmpty() })
  }

  @Test
  fun everyTapSwipeGestureCanBeAssignedInAnApp() {
    val keys = CommandBindings.gestures(context, gesturesScreen()).map { it.baseKey }.toSet()
    for (key in
      listOf(
        R.string.pref_shortcut_2finger_1tap_swipe_up_key,
        R.string.pref_shortcut_3finger_2tap_swipe_left_key,
        R.string.pref_shortcut_4finger_3tap_swipe_right_key,
        R.string.pref_shortcut_2finger_3tap_1finger_swipe_down_key,
      )) {
      assertTrue(s(key), s(key) in keys)
    }
  }

  @Test
  fun settingsSearchFindsTheGesturesWithTheirCategories() {
    val entries = SettingsIndex.build(context)
    val entry = entries.firstOrNull { it.key == s(R.string.pref_shortcut_2finger_2tap_1finger_swipe_up_key) }
    assertNotNull(entry)
    assertEquals(
      listOf(
        s(R.string.tap_swipe_gestures_title),
        s(R.string.tap_swipe_2finger_tap_1finger_swipe_gestures_title),
      ),
      entry!!.path.takeLast(2),
    )
    val threeFingers = entries.first { it.key == s(R.string.pref_shortcut_3finger_1tap_swipe_up_key) }
    assertEquals(
      listOf(s(R.string.tap_swipe_gestures_title), s(R.string.three_finger_gestures_title)),
      threeFingers.path.takeLast(2),
    )
  }

  @Test
  fun searchOnTheScreenFindsRowsOfCollapsedCategories() {
    val screen = gesturesScreen()
    val tapSwipe =
      screen.findPreference<AccessibilitySuitePreferenceCategory>(
        s(R.string.pref_category_tap_swipe_shortcuts_key)
      )!!
    val twoFingers =
      screen.findPreference<AccessibilitySuitePreferenceCategory>(
        s(R.string.pref_category_tap_swipe_2finger_shortcuts_key)
      )!!
    twoFingers.setCollapsible("Collapsed", "Expanded")
    tapSwipe.setCollapsible("Collapsed", "Expanded")
    val row = screen.findPreference<Preference>(s(R.string.pref_shortcut_2finger_1tap_swipe_up_key))!!
    assertFalse(twoFingers.isVisible)
    assertFalse(row.isVisible)
    assertTrue(tapSwipe.isHiddenByCollapse(twoFingers))
    assertTrue(twoFingers.isHiddenByCollapse(row))

    val rows = mutableListOf<Preference>()
    SettingsSearch.collectRows(screen, { it is GestureListPreference }, rows)
    assertTrue(row in rows)

    // A row hidden for another reason is still left out.
    twoFingers.setCollapsed(false)
    row.isVisible = false
    twoFingers.setCollapsed(true)
    assertFalse(twoFingers.isHiddenByCollapse(row))
    rows.clear()
    SettingsSearch.collectRows(screen, { it is GestureListPreference }, rows)
    assertFalse(row in rows)
  }
}
