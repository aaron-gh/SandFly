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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import androidx.annotation.XmlRes
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceManager
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.actor.ImageCaptioner
import com.google.android.accessibility.talkback.commands.CommandFragment
import com.google.android.accessibility.talkback.commands.Commands
import com.google.android.accessibility.talkback.commands.CommandsFragment
import com.google.android.accessibility.talkback.flags.FeatureFlagReader
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping
import com.google.android.accessibility.talkback.preference.TalkBackPreferenceFilter
import com.google.android.accessibility.talkback.preference.base.BrailleFragment
import com.google.android.accessibility.talkback.preference.base.TalkBackPreferenceFragment
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment
import com.google.android.accessibility.talkback.recognition.LiveRecognition
import com.google.android.accessibility.talkback.sandflyai.SandflyAi
import com.google.android.accessibility.talkback.utils.TalkbackFeatureSupport
import com.google.android.accessibility.utils.FeatureSupport
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.PreferenceSettingsUtils
import com.google.android.accessibility.utils.SettingsUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.monitor.InputDeviceMonitor
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * The settings that search finds: every preference of the settings screens made from XML, reached
 * from the main screen, and every command. Screens built in code, such as lists of apps or
 * profiles, are found by their entries only.
 */
object SettingsIndex {
  private const val TAG = "SettingsIndex"
  private val FORMAT = Regex("%(\\d+\\$)?[sd]")

  /**
   * A setting: its [title] and [summary], the titles of the screens and categories that lead to it,
   * and the screen that has it, opened with [args], where [key] names it.
   */
  class Entry(
    val title: String,
    val summary: String?,
    val path: List<String>,
    val screen: String,
    val args: Bundle,
    val key: String?,
  )

  private class Screen(val name: String, @XmlRes val xml: Int, val path: List<String>, val args: Bundle)

  /** Keeps nothing, so that reading a screen doesn't save its defaults as settings. */
  private object NoStorage : PreferenceDataStore() {
    override fun putString(key: String, value: String?) {}

    override fun putStringSet(key: String, values: Set<String>?) {}

    override fun putInt(key: String, value: Int) {}

    override fun putLong(key: String, value: Long) {}

    override fun putFloat(key: String, value: Float) {}

    override fun putBoolean(key: String, value: Boolean) {}
  }

  /**
   * Returns the settings of every screen, in the order of the screens from the main one. [context]
   * is a settings screen's, with its theme.
   */
  // The PreferenceManager constructor and inflateFromResource are what settings screens read their
  // XML with; they are restricted only to their library group.
  @SuppressLint("RestrictedApi")
  @JvmStatic
  fun build(context: Context): List<Entry> {
    val manager = PreferenceManager(context)
    manager.preferenceDataStore = NoStorage
    val entries = mutableListOf<Entry>()
    val unavailable = unavailableKeys(context)
    val filter = TalkBackPreferenceFilter(context)
    val root = TalkBackPreferenceFragment::class.java.name
    val visited = mutableSetOf(root)
    val queue = ArrayDeque<Screen>()
    queue += Screen(root, R.xml.preferences, emptyList(), Bundle())
    while (queue.isNotEmpty()) {
      val screen = queue.removeFirst()
      val preferences =
        try {
          manager.inflateFromResource(context, screen.xml, null)
        } catch (e: RuntimeException) {
          LogUtils.w(TAG, "Can't read the screen %s: %s", screen.name, e)
          continue
        }
      filter.filterPreferences(preferences)
      fun add(group: PreferenceGroup, path: List<String>) {
        for (i in 0 until group.preferenceCount) {
          val preference = group.getPreference(i)
          if (!preference.isVisible || preference.key in unavailable) {
            continue
          }
          val title = textOf(preference.title)
          if (preference is PreferenceCategory) {
            add(preference, if (title == null) path else path + title)
            continue
          }
          if (title == null || !preference.isSelectable) {
            continue
          }
          entries += Entry(title, summaryOf(preference), path, screen.name, screen.args, preference.key)
          if (preference is PreferenceGroup) {
            add(preference, path + title)
          }
          val fragment = preference.fragment ?: continue
          if (!visited.add(fragment)) {
            continue
          }
          val linkPath = screen.path + title
          if (fragment == CommandsFragment::class.java.name) {
            addCommands(context, linkPath, entries)
            continue
          }
          val xml = xmlOf(fragment) ?: continue
          queue += Screen(fragment, xml, linkPath, Bundle(preference.extras))
        }
      }
      add(preferences, screen.path)
      if (screen.name == root) {
        addSandflyAi(context, root, entries, visited, queue)
      }
    }
    return entries
  }

  /** The summary that says what a preference is, not its value. */
  private fun summaryOf(preference: Preference): String? =
    if (preference is ListPreference) null else textOf(preference.summary)

  /** Returns [text], or null if it is empty or a format that the screen fills in. */
  private fun textOf(text: CharSequence?): String? =
    text?.toString()?.trim()?.takeIf { it.isNotEmpty() && !FORMAT.containsMatchIn(it) }

  /** Adds the commands, each opening its own screen, as on the Commands screen. */
  private fun addCommands(context: Context, path: List<String>, entries: MutableList<Entry>) {
    for (category in Commands.categories(context)) {
      val categoryPath = path + context.getString(category.titleRes)
      for (command in category.commands) {
        val args = Bundle().apply { putString(CommandFragment.ARG_COMMAND, command.value) }
        entries += Entry(command.label, null, categoryPath, CommandFragment::class.java.name, args, null)
      }
    }
  }

  /** Adds SandFly AI's entry and screen, which the main screen adds in builds that include it. */
  private fun addSandflyAi(
    context: Context,
    root: String,
    entries: MutableList<Entry>,
    visited: MutableSet<String>,
    queue: ArrayDeque<Screen>,
  ) {
    val features = SandflyAi.features ?: return
    val title = features.settingsTitle(context)
    entries += Entry(title, null, emptyList(), root, Bundle(), "pref_sandfly_ai_screen")
    val fragment = features.settingsFragment
    if (visited.add(fragment)) {
      xmlOf(fragment)?.let { queue += Screen(fragment, it, listOf(title), Bundle()) }
    }
  }

  /**
   * Returns the XML that the settings screen [fragment] is made from, or null for screens built in
   * code. Creating a fragment doesn't attach or start it.
   */
  private fun xmlOf(fragment: String): Int? =
    try {
      val type = Class.forName(fragment, false, SettingsIndex::class.java.classLoader)
      if (TalkbackBaseFragment::class.java.isAssignableFrom(type)) {
        (type.getDeclaredConstructor().newInstance() as TalkbackBaseFragment).xmlResId.takeIf { it > 0 }
      } else {
        null
      }
    } catch (e: ReflectiveOperationException) {
      LogUtils.w(TAG, "Can't create the screen %s: %s", fragment, e)
      null
    } catch (e: LinkageError) {
      LogUtils.w(TAG, "Can't create the screen %s: %s", fragment, e)
      null
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Can't create the screen %s: %s", fragment, e)
      null
    }

  /**
   * Keys of the preferences that the screens' fragments hide on this device, besides those that
   * [TalkBackPreferenceFilter] removes. Keep in step with the fragments named.
   */
  private fun unavailableKeys(context: Context): Set<String> {
    val keys = mutableSetOf<String>()
    fun hideIf(hidden: Boolean, key: String) {
      if (hidden) {
        keys += key
      }
    }
    fun hideIf(hidden: Boolean, keyRes: Int) = hideIf(hidden, context.getString(keyRes))
    val wear = FormFactorUtils.isAndroidWear()
    val tv = FormFactorUtils.isAndroidTv()

    // TalkBackPreferenceFragment: the survey shows only while one is offered.
    hideIf(true, R.string.pref_survey_setting_entry_point_key)
    hideIf(!BrailleFragment.isSupported(context), "pref_braille_screen")
    hideIf(!ImageCaptioner.supportsImageCaption(context), R.string.pref_auto_image_captioning_key)

    // BrailleFragment.
    hideIf(!FeatureSupport.supportBrailleDisplay(context), R.string.pref_brailledisplay_key)
    hideIf(!FeatureSupport.supportBrailleKeyboard(context), R.string.pref_brailleime_key)

    // NavigationPrefFragment.
    hideIf(!FeatureFlagReader.enableSmartBrowseMode(context), R.string.pref_smart_browse_mode_key)
    hideIf(!FeatureFlagReader.enableBrowseMode(context), R.string.pref_browse_mode_per_app_key)

    // AdvancedSettingFragment and SettingsBackup.
    hideIf(
      !TalkbackFeatureSupport.supportTalkBackExitBanner(context),
      R.string.pref_show_exit_watermark_key,
    )
    hideIf(wear || tv, "pref_practice_gestures_help")
    hideIf(wear, R.string.pref_export_settings_key)
    hideIf(wear, R.string.pref_import_settings_key)

    // TextToSpeechSettingsFragment.
    hideIf(
      tv ||
        !SettingsUtils.allowLinksOutOfSettings(context) ||
        !PreferenceSettingsUtils.canHandleIntent(context, Intent(TalkBackService.INTENT_TTS_SETTINGS)),
      R.string.pref_system_tts_settings_key,
    )

    // TypingFragment: shown while a physical keyboard is connected.
    hideIf(!InputDeviceMonitor(context).hasPhysicalKeyboard(), R.string.pref_physical_keyboard_key)

    // SoundAndVibrationFragment.
    hideIf(Build.VERSION.SDK_INT < Build.VERSION_CODES.S, R.string.pref_speakerphone_away_from_ear_key)
    hideIf(
      Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU,
      R.string.pref_reset_accessibility_audio_key,
    )
    val vibrator = context.getSystemService(Vibrator::class.java)
    hideIf(
      vibrator == null || !vibrator.hasVibrator() || !vibrator.hasAmplitudeControl(),
      R.string.pref_vibration_strength_key,
    )

    // AutomaticDescriptionsFragment.
    hideIf(!LiveRecognition.isSupported(context), "pref_live_recognition_screen")
    hideIf(!ImageCaptioner.supportsIconDetection(context), R.string.pref_icon_detection_key)
    hideIf(!ImageCaptioner.supportsImageDescription(context), R.string.pref_image_description_key)

    // SelectorMenuFragment.
    hideIf(
      !LiveRecognition.isSupported(context) || !FeatureSupport.doesServiceHandleDoubleTap(),
      R.string.pref_selector_live_recognition_key,
    )

    // ContextMenuFragment.
    hideIf(
      !ImageCaptioner.supportsImageCaption(context),
      R.string.pref_show_context_menu_summarize_view_setting_key,
    )
    hideIf(
      !ImageCaptioner.supportsImageCaption(context),
      R.string.pref_show_context_menu_image_caption_setting_key,
    )
    hideIf(
      !FeatureFlagReader.enableShowKeyboardShortcutsDialog(context),
      R.string.pref_show_context_menu_show_keyboard_shortcuts_setting_key,
    )
    val proCommands = SandflyAi.features?.commands(context).orEmpty()
    hideIf(
      SandflyAi.COMMAND_RECOGNIZE_SCREEN !in proCommands,
      R.string.pref_show_context_menu_recognize_screen_setting_key,
    )
    hideIf(
      SandflyAi.COMMAND_SCREEN_RECOGNITION_IN_APP !in proCommands,
      R.string.pref_show_context_menu_screen_recognition_app_setting_key,
    )

    // DeveloperPrefFragment. The version is shown there, or as the screen's subtitle.
    hideIf(true, R.string.pref_developer_version_code_key)
    hideIf(!FeatureSupport.supportGestureDetection(), R.string.pref_talkback_gesture_detection_key)
    hideIf(
      !(FeatureSupport.supportMultipleGestureSet() && FeatureFlagReader.useMultipleGestureSet(context)),
      R.string.pref_multiple_gesture_set_key,
    )

    // TalkBackGestureShortcutPreferenceFragment.
    hideIf(
      !GestureShortcutMapping.isGestureSetEnabled(
        context,
        SharedPreferencesUtils.getSharedPreferences(context),
        R.string.pref_multiple_gesture_set_key,
        R.bool.pref_multiple_gesture_set_default,
      ),
      R.string.pref_gesture_set_key,
    )
    hideIf(!wear, R.string.pref_reserve_vibration_watch_gestures_key)
    return keys
  }
}
