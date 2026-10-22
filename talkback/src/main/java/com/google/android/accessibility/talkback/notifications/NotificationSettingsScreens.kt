/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.talkback.notifications

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.annotation.XmlRes
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.notificationlist.NotificationList
import com.google.android.accessibility.talkback.notificationlist.NotificationListService
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.VoiceProfiles
import java.util.Calendar

/** Number choices of a list setting, and how each is said. */
internal object NotificationChoices {
  fun percent(context: Context, value: Int) = context.getString(R.string.notification_percent, value)

  fun duration(context: Context, seconds: Int): String {
    val r = context.resources
    return when {
      seconds < 0 -> context.getString(R.string.notification_never_again)
      seconds == 0 -> context.getString(R.string.notification_off)
      seconds < 60 -> r.getQuantityString(R.plurals.notification_seconds, seconds, seconds)
      seconds < 3600 -> r.getQuantityString(R.plurals.notification_minutes, seconds / 60, seconds / 60)
      else -> r.getQuantityString(R.plurals.notification_hours, seconds / 3600, seconds / 3600)
    }
  }

  fun count(context: Context, value: Int, plural: Int): String =
    if (value == 0) context.getString(R.string.notification_off)
    else context.resources.getQuantityString(plural, value, value)

  /** The choices of each list setting whose choices are numbers, with how each is said. */
  fun of(context: Context, key: String): List<Pair<Int, String>>? {
    fun seconds(vararg values: Int) = values.map { it to duration(context, it) }
    fun minutes(vararg values: Int) = values.map { it to duration(context, it * 60) }
    fun counts(plural: Int, vararg values: Int) = values.map { it to count(context, it, plural) }
    val k = NotificationSettings.Companion.Keys
    return when (key) {
      k.MIN_DIGITS -> (3..10).map { it to context.resources.getQuantityString(R.plurals.notification_digits, it, it) }
      k.MAX_WORDS -> counts(R.plurals.notification_words, 0, 5, 10, 15, 20, 30, 50, 100)
      k.MAX_SENTENCES -> counts(R.plurals.notification_sentences, 0, 1, 2, 3, 5)
      k.MAX_CHARACTERS -> counts(R.plurals.notification_characters, 0, 50, 100, 200, 300, 500, 1000)
      k.MAX_SECONDS, k.DELAY -> seconds(0, 1, 2, 3, 5, 10, 15, 30, 60).let { list ->
        if (key == k.DELAY) list.filter { it.first <= 10 } else list.filter { it.first == 0 || it.first >= 5 }
      }
      k.REPEAT_MINUTES -> minutes(0, 1, 2, 5, 10, 15, 30)
      k.REPEAT_SECONDS -> seconds(0, 10, 30, 60, 300, 900, 3600, -1)
      k.COOLDOWN -> seconds(0, 5, 10, 30, 60, 300, 900)
      k.PREFIX_MEMORY -> seconds(30, 60, 300, 900)
      SpokenSchedules.KEY_SUMMARY_GAP -> seconds(0, 1, 2, 3, 5)
      SpokenSchedules.KEY_CLOCK_INTERVAL ->
        listOf(0 to context.getString(R.string.notification_off)) + minutes(15, 30, 60, 180).drop(0)
      else -> null
    }
  }
}

/**
 * A notification settings screen made from [xml]. It fills in the choices of number settings,
 * shows formats and times, and connects the notification listener when a setting needs it.
 */
abstract class NotificationXmlFragment(@XmlRes xml: Int, @StringRes private val title: Int) :
  TalkbackBaseFragment(xml) {
  protected lateinit var prefs: SharedPreferences

  private val listener =
    SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      when (key) {
        NotificationSettings.Companion.Keys.DISMISS,
        NotificationSettings.Companion.Keys.APP_CANCEL,
        NotificationSettings.Companion.Keys.DND,
        SpokenSchedules.KEY_SUMMARY_DAILY -> {
          NotificationListService.rebind(requireContext())
          updateAccess()
        }
        SpokenSchedules.KEY_CLOCK_EXACT -> if (prefs.getBoolean(key, false)) askForExactAlarms()
      }
    }

  override fun getTitle(): CharSequence = getText(title)

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    super.onCreatePreferences(savedInstanceState, rootKey)
    val context = requireContext()
    prefs = SharedPreferencesUtils.getSharedPreferences(context)
    for (i in 0 until preferenceScreen.preferenceCount) setUp(preferenceScreen.getPreference(i))
  }

  override fun onResume() {
    super.onResume()
    prefs.registerOnSharedPreferenceChangeListener(listener)
    updateAccess()
  }

  override fun onPause() {
    prefs.unregisterOnSharedPreferenceChangeListener(listener)
    super.onPause()
  }

  private fun setUp(preference: Preference) {
    if (preference is androidx.preference.PreferenceGroup) {
      for (i in 0 until preference.preferenceCount) setUp(preference.getPreference(i))
      return
    }
    val context = requireContext()
    val key = preference.key ?: return
    when {
      key == NotificationSettings.Companion.Keys.VOICE_PROFILE && preference is ListPreference ->
        setUpVoices(preference)
      preference is ListPreference && preference.entries == null -> {
        val choices = NotificationChoices.of(context, key) ?: return
        preference.entries = choices.map { it.second }.toTypedArray()
        preference.entryValues = choices.map { it.first.toString() }.toTypedArray()
        if (preference.value == null || preference.findIndexOfValue(preference.value) < 0) {
          // A value saved as a number, or not saved yet, shows as the choice closest to it.
          val current = NotificationSettings.int(prefs, key, choices.first().first)
          preference.value = (choices.firstOrNull { it.first == current } ?: choices.first()).first.toString()
        }
      }
      preference is EditTextPreference -> setUpText(preference, key)
      key == NotificationSettings.Companion.Keys.QUIET_START ||
        key == NotificationSettings.Companion.Keys.QUIET_END ||
        key == SpokenSchedules.KEY_SUMMARY_TIME -> setUpTime(preference, key)
      key == KEY_ACCESS -> preference.setOnPreferenceClickListener {
        openAccessSettings(context)
        true
      }
      key == KEY_SUMMARY_NOW -> preference.setOnPreferenceClickListener {
        if (!NotificationReader.readSummary()) {
          android.widget.Toast.makeText(context, R.string.notification_summary_needs_sandfly, android.widget.Toast.LENGTH_SHORT).show()
        }
        true
      }
    }
  }

  /** Lists SandFly's voice and the voice profiles, by name, to read notifications with. */
  private fun setUpVoices(preference: ListPreference) {
    val names =
      VoiceProfiles.names(prefs, getString(R.string.voice_profile_default)) {
        getString(R.string.voice_profile_new_name, it)
      }
    preference.entries =
      (listOf(getString(R.string.notification_voice_sandfly)) + names.values).toTypedArray()
    preference.entryValues = (listOf("") + names.keys).toTypedArray()
    if (preference.findIndexOfValue(preference.value.orEmpty()) < 0) preference.value = ""
  }

  override fun onStart() {
    super.onStart()
    // Profiles may have been added or renamed on their own screen.
    findPreference<ListPreference>(NotificationSettings.Companion.Keys.VOICE_PROFILE)?.let { setUpVoices(it) }
  }

  private fun setUpText(preference: EditTextPreference, key: String) {
    val defaultText =
      when (key) {
        NotificationSettings.Companion.Keys.FORMAT -> NotificationSettings.DEFAULT_FORMAT
        NotificationSettings.Companion.Keys.PRIVATE_FORMAT -> NotificationSettings.DEFAULT_PRIVATE_FORMAT
        SpokenSchedules.KEY_CLOCK_FORMAT -> getString(R.string.notification_clock_format_default)
        else -> return
      }
    preference.setSummaryProvider { (it as EditTextPreference).text?.takeIf { text -> text.isNotBlank() } ?: defaultText }
    preference.setOnBindEditTextListener { edit ->
      if (edit.text.isNullOrBlank()) edit.setText(defaultText)
    }
  }

  private fun setUpTime(preference: Preference, key: String) {
    val default =
      when (key) {
        NotificationSettings.Companion.Keys.QUIET_START -> NotificationSettings().quietStartMinutes
        NotificationSettings.Companion.Keys.QUIET_END -> NotificationSettings().quietEndMinutes
        else -> 8 * 60
      }
    fun current() = NotificationSettings.int(prefs, key, default)
    preference.summary = timeText(requireContext(), current())
    preference.setOnPreferenceClickListener {
      val context = requireContext()
      val minutes = current()
      TimePickerDialog(
          context,
          { _, hour, minute ->
            prefs.edit().putInt(key, hour * 60 + minute).apply()
            preference.summary = timeText(context, hour * 60 + minute)
          },
          minutes / 60,
          minutes % 60,
          DateFormat.is24HourFormat(context),
        )
        .show()
      true
    }
  }

  private fun updateAccess() {
    val access = findPreference<Preference>(KEY_ACCESS) ?: return
    access.isVisible = !NotificationList.isAccessGranted(requireContext())
    access.summary = getString(R.string.notification_access_summary)
  }

  private fun askForExactAlarms() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val alarms = requireContext().getSystemService(AlarmManager::class.java) ?: return
    if (alarms.canScheduleExactAlarms()) return
    try {
      startActivity(
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + requireContext().packageName))
      )
    } catch (e: ActivityNotFoundException) {
      // The setting falls back to times within a minute.
    }
  }

  companion object {
    const val KEY_ACCESS = "pref_notification_access"
    const val KEY_SUMMARY_NOW = "pref_notification_summary_now"

    fun timeText(context: Context, minutes: Int): String {
      val calendar =
        Calendar.getInstance().apply {
          set(Calendar.HOUR_OF_DAY, minutes / 60)
          set(Calendar.MINUTE, minutes % 60)
        }
      return DateFormat.getTimeFormat(context).format(calendar.time)
    }

    /** Opens the system setting where the user gives SandFly notification access. */
    fun openAccessSettings(context: Context) {
      val component = ComponentName(context, NotificationListService::class.java)
      val intents =
        listOfNotNull(
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
              .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
          } else {
            null
          },
          Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
        )
      for (intent in intents) {
        try {
          context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
          return
        } catch (e: ActivityNotFoundException) {
          // Try the next.
        }
      }
    }
  }
}

class NotificationReadingFragment :
  NotificationXmlFragment(R.xml.notification_reading_preferences, R.string.notification_reading_title)

class NotificationKindsFragment :
  NotificationXmlFragment(R.xml.notification_kinds_preferences, R.string.notification_kinds_title)

class NotificationWhenFragment :
  NotificationXmlFragment(R.xml.notification_when_preferences, R.string.notification_when_title)

class NotificationRepeatsFragment :
  NotificationXmlFragment(R.xml.notification_repeats_preferences, R.string.notification_repeats_title)

class NotificationStopFragment :
  NotificationXmlFragment(R.xml.notification_stop_preferences, R.string.notification_stop_title)

class NotificationSchedulesFragment :
  NotificationXmlFragment(R.xml.notification_schedules_preferences, R.string.notification_schedules_title)
