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

package com.google.android.accessibility.talkback.blackberry

import android.content.Context
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.backup.SettingsBackup
import com.google.android.accessibility.talkback.commands.Commands
import com.google.android.accessibility.talkback.gesture.BlackBerryGestures
import com.google.android.accessibility.talkback.gesture.GestureLayout
import com.google.android.accessibility.talkback.gesture.QuickTasks
import com.google.android.accessibility.talkback.keyboard.Keymap
import com.google.android.accessibility.talkback.soundthemes.SoundThemeManifest
import com.google.android.accessibility.talkback.soundthemes.SoundThemes
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
import org.robolectric.shadows.ShadowLooper

/** Checks BlackBerry mode's gestures, Quick Tasks and the settings of its profile. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BlackBerryModeTest {

  private val context = RuntimeEnvironment.getApplication()
  private val prefs = context.getSharedPreferences("blackberry_mode_test", Context.MODE_PRIVATE)
  private fun string(res: Int) = context.getString(res)

  @After
  fun forget() {
    prefs.edit().clear().commit()
    QuickTasks.stop()
  }

  @Test
  fun blackBerryLayoutKeepsItsOwnChanges() {
    val up = string(R.string.pref_shortcut_up_key)
    GestureLayout.select(prefs, GestureLayout.BLACKBERRY)
    assertEquals(GestureLayout.BLACKBERRY, GestureLayout.current(prefs))
    assertTrue(prefs.getBoolean(GestureLayout.TWO_FINGER_SWIPES_KEY, false))
    assertEquals(up + "_blackberry", GestureLayout.key(prefs, up))
    assertEquals(
      string(R.string.shortcut_value_next_granularity),
      GestureLayout.read(context, prefs, up, "HOME"),
    )
    prefs.edit().putString(GestureLayout.key(prefs, up), "HOME").commit()
    GestureLayout.select(prefs, GestureLayout.VOICEOVER)
    assertEquals(up + "_voiceover", GestureLayout.key(prefs, up))
    assertEquals(
      string(R.string.shortcut_value_selected_setting_previous_action),
      GestureLayout.read(context, prefs, up, "HOME"),
    )
    GestureLayout.select(prefs, GestureLayout.BLACKBERRY)
    assertEquals("HOME", GestureLayout.read(context, prefs, up, "HOME"))
  }

  @Test
  fun unknownLayoutIsSandFly() {
    prefs.edit().putString(GestureLayout.KEY, "windows phone").commit()
    assertEquals(GestureLayout.SANDFLY, GestureLayout.current(prefs))
  }

  @Test
  fun blackBerryGestures() {
    GestureLayout.select(prefs, GestureLayout.BLACKBERRY)
    for ((keyRes, valueRes) in
      listOf(
        R.string.pref_shortcut_right_key to R.string.shortcut_value_next_by_navigation_setting,
        R.string.pref_shortcut_left_key to R.string.shortcut_value_previous_by_navigation_setting,
        R.string.pref_shortcut_down_and_left_key to R.string.shortcut_value_pause_or_resume_feedback,
        R.string.pref_shortcut_down_and_right_key to R.string.shortcut_value_quick_tasks,
        R.string.pref_shortcut_2finger_swipe_up_key to R.string.shortcut_value_scroll_back,
        R.string.pref_shortcut_2finger_swipe_down_key to R.string.shortcut_value_scroll_forward,
        R.string.pref_shortcut_3finger_swipe_down_key to R.string.shortcut_value_read_from_top,
      )) {
      val key = string(keyRes)
      assertEquals(key, string(valueRes), GestureLayout.read(context, prefs, key, "HOME"))
    }
    val unassigned = string(R.string.shortcut_value_unassigned)
    val tripleTap = string(R.string.pref_shortcut_3finger_3tap_key)
    assertEquals(unassigned, GestureLayout.read(context, prefs, tripleTap, "HOME"))
  }

  @Test
  fun blackBerryDefaultsAreGesturesAndCommands() {
    FormFactorUtils.initialize(context)
    val gestures = context.resources.getStringArray(R.array.pref_shortcut_keys).toSet()
    val doubleTaps =
      setOf(
        string(R.string.pref_shortcut_1finger_2tap_key),
        string(R.string.pref_shortcut_1finger_2tap_hold_key),
      )
    for ((key, value) in BlackBerryGestures.defaults(context) - doubleTaps) {
      assertTrue(key, key in gestures)
      assertNotNull(value, Commands.label(context, value))
    }
  }

  @Test
  fun quickTasksTakeTheirGesturesForThreeSeconds() {
    val up = string(R.string.pref_shortcut_up_key)
    val twoFingerTap = string(R.string.pref_shortcut_2finger_1tap_key)
    QuickTasks.start(context) {}
    val now = android.os.SystemClock.uptimeMillis()
    assertEquals(string(R.string.shortcut_value_increase_volume), QuickTasks.action(up, now))
    // Each task gives another three seconds.
    assertEquals(
      string(R.string.shortcut_value_show_hide_screen),
      QuickTasks.action(twoFingerTap, now + 2500),
    )
    assertNotNull(QuickTasks.action(up, now + 5000))
    assertNull(QuickTasks.action(up, now + 9000))
  }

  @Test
  fun anotherGestureEndsQuickTasks() {
    val up = string(R.string.pref_shortcut_up_key)
    QuickTasks.start(context) {}
    val now = android.os.SystemClock.uptimeMillis()
    assertNull(QuickTasks.action(string(R.string.pref_shortcut_3finger_1tap_key), now))
    assertFalse(QuickTasks.isActive())
    assertNull(QuickTasks.action(up, now))
  }

  @Test
  fun runningOutOfTimeVibrates() {
    var timedOut = false
    QuickTasks.start(context) { timedOut = true }
    ShadowLooper.idleMainLooper(QuickTasks.TIMEOUT_MS + 10, java.util.concurrent.TimeUnit.MILLISECONDS)
    assertTrue(timedOut)
    assertFalse(QuickTasks.isActive())
  }

  @Test
  fun profileSettingsFitTheirSettings() {
    val settings = BlackBerrySettings.values(context, "blackberry", keyboardPhone = true)
    assertEquals(GestureLayout.BLACKBERRY, settings[GestureLayout.KEY])
    assertEquals(
      string(Keymap.BLACKBERRY.valueRes),
      settings[string(R.string.pref_select_keymap_key)],
    )
    assertEquals(false, settings[string(R.string.pref_a11y_hints_key)])
    for ((key, value) in BlackBerrySettings.profileSettings(context, "blackberry")) {
      assertTrue(key, SettingsBackup.canSetProfileValue(context, key, value))
    }
    assertFalse(
      string(R.string.pref_select_keymap_key) in
        BlackBerrySettings.values(context, null, keyboardPhone = false)
    )
  }

  @Test
  fun themeManifestHasNoWarnings() {
    val text =
      java.util.zip.ZipInputStream(context.assets.open(BlackBerryMode.THEME_ASSET)).use { zip ->
        generateSequence { zip.nextEntry }.first { it.name == "theme.json" }
        zip.readBytes().toString(Charsets.UTF_8)
      }
    val manifest = SoundThemeManifest.parse(text, SoundThemes.VIBRATION_NAMES, "BlackBerry", SoundThemes.SOUND_KEYS)
    assertEquals("BlackBerry", manifest.name)
    assertEquals(emptyList<String>(), manifest.warnings)
    assertEquals(false, manifest.controlSounds)
  }

  @Test
  fun profileIdStaysOutOfProfiles() {
    assertTrue(SettingsBackup.isKeptOutOfProfiles(BlackBerryMode.PROFILE_ID_KEY))
  }
}
