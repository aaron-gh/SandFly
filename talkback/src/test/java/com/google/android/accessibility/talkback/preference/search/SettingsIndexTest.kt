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

package com.google.android.accessibility.talkback.preference.search

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.view.ContextThemeWrapper
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.commands.CommandFragment
import com.google.android.accessibility.talkback.commands.Commands
import com.google.android.accessibility.talkback.gesture.GestureLayout
import com.google.android.accessibility.talkback.notificationlist.NotificationListService
import com.google.android.accessibility.talkback.notifications.NotificationXmlFragment
import com.google.android.accessibility.talkback.preference.base.TalkBackGestureShortcutPreferenceFragment
import com.google.android.accessibility.talkback.preference.base.TalkBackPreferenceFragment
import com.google.android.accessibility.talkback.preference.base.TextToSpeechSettingsFragment
import com.google.android.accessibility.talkback.translation.TranslationFragment
import com.google.android.accessibility.talkback.translation.TranslationSettings
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.CapturePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsIndexTest {
  private lateinit var context: Context
  private lateinit var entries: List<SettingsIndex.Entry>

  @Before
  fun setUp() {
    val app = RuntimeEnvironment.getApplication()
    FormFactorUtils.initialize(app)
    context = ContextThemeWrapper(app, R.style.TalkBackSettings)
    entries = SettingsIndex.build(context)
  }

  @Test
  fun readingTheScreensSavesNoSettings() {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    prefs.edit().clear().commit()
    SettingsIndex.build(context)
    assertEquals(emptyMap<String, Any?>(), prefs.all)
  }

  @Test
  fun screensWithMultiSelectSettingsAreRead() {
    val keys = entries.map { it.key }.toSet()
    assertTrue(context.getString(R.string.pref_speaker_devices_key) in keys)
    assertTrue(context.getString(R.string.pref_selector_actions_key) in keys)
  }

  @Test
  fun anOptionOfAnItemIsFoundOnlyWhileTheItemIsInTheList() {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val option = context.getString(R.string.pref_text_entities_key)
    val readingControls = context.getString(R.string.title_pref_category_selector_menu)
    val sandflyMenu = context.getString(R.string.title_pref_category_context_menu)
    fun screensWithOption() =
      SettingsIndex.build(context).filter { it.key == option }.map { it.path.first() }.toSet()

    // Actions is in the reading controls and not in the SandFly menu by default.
    assertEquals(setOf(readingControls), screensWithOption())
    try {
      prefs
        .edit()
        .putBoolean(context.getString(R.string.pref_selector_actions_key), false)
        .putBoolean(context.getString(R.string.pref_show_context_menu_custom_action_setting_key), true)
        .commit()
      assertEquals(setOf(sandflyMenu), screensWithOption())
    } finally {
      prefs.edit().clear().commit()
    }
  }

  @Test
  fun settingIsFoundWithItsScreenAndCategory() {
    val entry = entries.firstOrNull { it.key == context.getString(R.string.pref_touch_stops_speech_key) }
    assertNotNull(entry)
    assertEquals(context.getString(R.string.title_pref_touch_stops_speech), entry!!.title)
    assertEquals(
      listOf(context.getString(R.string.title_pref_speech), context.getString(R.string.speech_category_stopping)),
      entry.path,
    )
    assertEquals(TextToSpeechSettingsFragment::class.java.name, entry.screen)
  }

  @Test
  fun mainScreenEntriesHaveNoPath() {
    val speech = entries.first { it.key == context.getString(R.string.pref_tts_settings_key) }
    assertEquals(emptyList<String>(), speech.path)
    assertEquals(TalkBackPreferenceFragment::class.java.name, speech.screen)
  }

  @Test
  fun gesturesAreFoundWithoutTheCommandsTheyRun() {
    val gestures =
      entries.filter { it.screen == TalkBackGestureShortcutPreferenceFragment::class.java.name }
    assertTrue(gestures.size > 20)
    val gestureKeys = context.resources.getStringArray(R.array.pref_shortcut_keys).toSet()
    val found = gestures.filter { it.key in gestureKeys }
    assertTrue(found.size > 20)
    assertTrue(found.all { it.summary == null })
  }

  @Test
  fun buildInBackgroundDeliversTheEntriesOnTheMainThread() {
    var built: List<SettingsIndex.Entry>? = null
    var thread: Thread? = null
    SettingsIndex.buildInBackground(context) {
      built = it
      thread = Thread.currentThread()
    }
    val deadline = System.currentTimeMillis() + 60_000
    while (built == null && System.currentTimeMillis() < deadline) {
      shadowOf(Looper.getMainLooper()).idle()
      Thread.sleep(5)
    }
    assertEquals(entries.map { it.screen to it.key }, built!!.map { it.screen to it.key })
    assertSame(Looper.getMainLooper().thread, thread)
  }

  @Test
  fun stoppedBuildDeliversNothing() {
    var delivered = false
    val stop = SettingsIndex.buildInBackground(context) { delivered = true }
    stop.run()
    repeat(20) {
      Thread.sleep(10)
      shadowOf(Looper.getMainLooper()).idle()
    }
    assertFalse(delivered)
  }

  @Test
  fun everyCommandIsFound() {
    val commands = entries.filter { it.screen == CommandFragment::class.java.name }
    val expected = Commands.categories(context).sumOf { it.commands.size }
    assertEquals(expected, commands.size)
    assertTrue(commands.all { it.path.first() == context.getString(R.string.title_pref_commands) })
    assertTrue(commands.all { it.args.getString(CommandFragment.ARG_COMMAND) != null })
  }

  @Test
  fun hiddenPreferencesAreNotFound() {
    val keys = entries.mapNotNull { it.key }.toSet()
    assertFalse(context.getString(R.string.pref_survey_setting_entry_point_key) in keys)
    assertFalse(SettingsSearch.SEARCH_KEY in keys)
  }

  @Test
  fun manageLabelsIsFoundOnlyAfterSetup() {
    val labels = context.getString(R.string.pref_manage_labels_key)
    Settings.Secure.putInt(context.contentResolver, "user_setup_complete", 0)
    assertFalse(labels in keys())
    Settings.Secure.putInt(context.contentResolver, "user_setup_complete", 1)
    assertTrue(labels in keys())
  }

  @Test
  fun notificationAccessIsFoundUntilGranted() {
    val notifications = context.getSystemService(NotificationManager::class.java)
    val listener = ComponentName(context, NotificationListService::class.java)
    shadowOf(notifications).setNotificationListenerAccessGranted(listener, false)
    assertTrue(NotificationXmlFragment.KEY_ACCESS in keys())
    shadowOf(notifications).setNotificationListenerAccessGranted(listener, true)
    assertFalse(NotificationXmlFragment.KEY_ACCESS in keys())
  }

  @Test
  fun translationSecondLanguageOrSwapIsFoundByTheSourceLanguage() {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    prefs.edit().remove(TranslationSettings.PREF_SOURCE).commit()
    var keys = keys()
    assertTrue(TranslationFragment.KEY_SECOND_TARGET in keys)
    assertFalse(TranslationFragment.KEY_SWAP in keys)
    prefs.edit().putString(TranslationSettings.PREF_SOURCE, "fr").commit()
    keys = keys()
    assertFalse(TranslationFragment.KEY_SECOND_TARGET in keys)
    assertTrue(TranslationFragment.KEY_SWAP in keys)
  }

  private fun keys(): Set<String> = SettingsIndex.build(context).mapNotNull { it.key }.toSet()

  /** Returns [keys] as found while the device reports Android 9. */
  private fun keysOnAndroid9(): Set<String> {
    val sdk = Build.VERSION.SDK_INT
    ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.P)
    try {
      return keys()
    } finally {
      ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
    }
  }

  @Test
  fun twoFingerSwipesSwitchIsFoundOnlyWithMultiFingerGestures() {
    assertTrue(GestureLayout.TWO_FINGER_SWIPES_KEY in keys())
    assertFalse(GestureLayout.TWO_FINGER_SWIPES_KEY in keysOnAndroid9())
  }

  @Test
  fun keepSpeechOffCastsIsFoundFromAndroid10() {
    assertTrue(CapturePolicy.PREF_KEEP_OFF_CASTS in keys())
    assertFalse(CapturePolicy.PREF_KEEP_OFF_CASTS in keysOnAndroid9())
  }

  @Test
  fun everyScreenIsReadOnce() {
    assertTrue(entries.all { it.title.isNotBlank() })
    // Titles that the screens fill in, such as "Download %1$s", aren't listed as they are.
    assertTrue(entries.none { it.title.contains("%s") || it.title.contains("%1\$s") })
    val keyed = entries.filter { it.key != null }
    assertEquals(keyed.size, keyed.map { it.screen to it.key }.distinct().size)
    assertTrue(entries.map { it.screen }.distinct().size > 20)
  }
}
