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
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import androidx.annotation.MainThread
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.compositor.rule.EventTypeNotificationStateChangedFeedbackRule
import com.google.android.accessibility.talkback.notificationlist.NotificationListService
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.Calendar
import java.util.Date

/**
 * Speech at set times: the time, every so often, and a summary of the notifications once a day.
 * Alarms with listeners in SandFly's process wake the phone for them, so they need no extra
 * permission, and they are set again after each one and when the settings change.
 */
class SpokenSchedules(
  private val context: Context,
  /** Says a text, interrupting what notifications are saying when the second value is true. */
  private val say: (CharSequence, Boolean) -> Unit,
) {
  private val handler = Handler(Looper.getMainLooper())
  private val alarms = context.getSystemService(AlarmManager::class.java)
  private val prefs = SharedPreferencesUtils.getSharedPreferences(context)
  private val clockAlarm = AlarmManager.OnAlarmListener { onClock() }
  private val summaryAlarm = AlarmManager.OnAlarmListener { onSummary() }
  private var started = false

  private val settingsListener =
    SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      if (key == null || key.startsWith(CLOCK_PREFIX) || key.startsWith(SUMMARY_PREFIX)) schedule()
    }

  @MainThread
  fun start() {
    if (started) return
    started = true
    prefs.registerOnSharedPreferenceChangeListener(settingsListener)
    schedule()
  }

  @MainThread
  fun stop() {
    if (!started) return
    started = false
    prefs.unregisterOnSharedPreferenceChangeListener(settingsListener)
    alarms?.cancel(clockAlarm)
    alarms?.cancel(summaryAlarm)
  }

  private fun schedule() {
    val alarms = alarms ?: return
    alarms.cancel(clockAlarm)
    alarms.cancel(summaryAlarm)
    val interval = NotificationSettings.int(prefs, KEY_CLOCK_INTERVAL, 0)
    val now = System.currentTimeMillis()
    if (interval > 0) {
      set(alarms, nextClockTime(now, interval), prefs.getBoolean(KEY_CLOCK_EXACT, false), clockAlarm)
    }
    if (prefs.getBoolean(KEY_SUMMARY_DAILY, false)) {
      val minutes = NotificationSettings.int(prefs, KEY_SUMMARY_TIME, 8 * 60)
      set(alarms, nextDailyTime(now, minutes), exact = true, summaryAlarm)
    }
  }

  private fun set(alarms: AlarmManager, at: Long, exact: Boolean, listener: AlarmManager.OnAlarmListener) {
    try {
      if (exact && canScheduleExact(alarms)) {
        alarms.setExact(AlarmManager.RTC_WAKEUP, at, TAG, listener, handler)
      } else {
        alarms.setWindow(AlarmManager.RTC_WAKEUP, at, WINDOW_MS, TAG, listener, handler)
      }
    } catch (e: SecurityException) {
      alarms.setWindow(AlarmManager.RTC_WAKEUP, at, WINDOW_MS, TAG, listener, handler)
    }
  }

  private fun canScheduleExact(alarms: AlarmManager): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

  private fun onClock() {
    schedule()
    val text = clockText(context, prefs.getString(KEY_CLOCK_FORMAT, null).orEmpty())
    speak(text)
  }

  private fun onSummary() {
    schedule()
    readSummary(context, say)
  }

  private fun speak(text: CharSequence) {
    if (text.isNotBlank()) say(text, false)
  }

  companion object {
    private const val TAG = "SpokenSchedules"
    private const val WINDOW_MS = 60_000L
    private const val KEEP_FOR_SUMMARY_MS = 60_000L
    private const val RETRY_MS = 1_500L
    private const val CLOCK_PREFIX = "pref_notification_clock"
    private const val SUMMARY_PREFIX = "pref_notification_summary"
    const val KEY_CLOCK_INTERVAL = "pref_notification_clock_interval"
    const val KEY_CLOCK_FORMAT = "pref_notification_clock_format"
    const val KEY_CLOCK_EXACT = "pref_notification_clock_exact"
    const val KEY_SUMMARY_DAILY = "pref_notification_summary_daily"
    const val KEY_SUMMARY_TIME = "pref_notification_summary_time"
    const val KEY_SUMMARY_NEWEST_FIRST = "pref_notification_summary_newest_first"
    const val KEY_SUMMARY_NAME = "pref_notification_summary_name"
    const val KEY_SUMMARY_GAP = "pref_notification_summary_gap"

    /** The next time on a boundary of [intervalMinutes] from midnight, after [now]. */
    fun nextClockTime(now: Long, intervalMinutes: Int): Long {
      val calendar = Calendar.getInstance().apply { timeInMillis = now }
      val minute = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
      val next = (minute / intervalMinutes + 1) * intervalMinutes
      calendar.set(Calendar.HOUR_OF_DAY, 0)
      calendar.set(Calendar.MINUTE, 0)
      calendar.set(Calendar.SECOND, 0)
      calendar.set(Calendar.MILLISECOND, 0)
      calendar.add(Calendar.MINUTE, next)
      return calendar.timeInMillis
    }

    /** The next time it is [minuteOfDay] minutes past midnight, after [now]. */
    fun nextDailyTime(now: Long, minuteOfDay: Int): Long {
      val calendar = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
        set(Calendar.MINUTE, minuteOfDay % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
      if (calendar.timeInMillis <= now) calendar.add(Calendar.DAY_OF_YEAR, 1)
      return calendar.timeInMillis
    }

    /** The time as [format] says, with {time}, {date} and {battery} filled in. */
    fun clockText(context: Context, format: String): String {
      val now = Date()
      val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
      val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
      val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
      val percent = if (level >= 0 && scale > 0) (level * 100 / scale).toString() else ""
      val template = format.ifBlank { context.getString(R.string.notification_clock_format_default) }
      return NotificationText.tidySeparators(
        template
          .replace("{time}", DateFormat.getTimeFormat(context).format(now))
          .replace("{date}", DateFormat.getLongDateFormat(context).format(now))
          .replace("{battery}", percent)
      )
    }

    /**
     * Reads every notification in the shade, as the notification settings say how, with a
     * greeting and how many there are. Needs notification access.
     */
    @JvmStatic
    @MainThread
    fun readSummary(context: Context, speak: (CharSequence, Boolean) -> Unit) {
      readSummary(context, speak, retry = true)
    }

    private fun readSummary(context: Context, speak: (CharSequence, Boolean) -> Unit, retry: Boolean) {
      val prefs = SharedPreferencesUtils.getSharedPreferences(context)
      val listener = NotificationListService.instance
      fun say(text: CharSequence, interrupt: Boolean = false) = speak(text, interrupt)
      if (listener == null) {
        // The listener connects only while something needs it; it is needed now for a minute.
        if (retry && NotificationListService.isAccessGranted(context)) {
          NotificationListService.keepConnected(context, KEEP_FOR_SUMMARY_MS)
          Handler(Looper.getMainLooper())
            .postDelayed({ readSummary(context, speak, retry = false) }, RETRY_MS)
          return
        }
        say(context.getString(R.string.notification_summary_no_access), interrupt = true)
        return
      }
      val settings = NotificationSettings.read(prefs)
      val decider = NotificationDecider()
      val notifications =
        runCatching { listener.activeNotifications }.getOrNull().orEmpty()
          .filter { it.packageName != context.packageName || it.notification.channelId != null }
          .sortedBy { it.postTime }
          .let { if (prefs.getBoolean(KEY_SUMMARY_NEWEST_FIRST, false)) it.reversed() else it }
      val format = context.getString(R.string.notification_summary_item_format)
      val state = DeviceStateReader(context).read()
      val lines =
        notifications.mapNotNull { sbn ->
          val info =
            NotificationInfo.from(sbn.packageName, NotificationReader.appName(context, sbn.packageName), sbn.notification)
          if (info.groupSummary || info.ongoing) return@mapNotNull null
          val category =
            EventTypeNotificationStateChangedFeedbackRule.getNotificationCategoryStateText(context, sbn.notification)
              .toString()
          decider.describe(info, settings, format, state, category)
        }
      val name = prefs.getString(KEY_SUMMARY_NAME, null).orEmpty().trim()
      val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
      val greeting =
        context.getString(
          when {
            hour < 12 -> R.string.notification_summary_morning
            hour < 18 -> R.string.notification_summary_afternoon
            else -> R.string.notification_summary_evening
          }
        )
      val intro =
        listOfNotNull(
            if (name.isEmpty()) greeting else context.getString(R.string.notification_summary_greeting_name, greeting, name),
            context.resources.getQuantityString(R.plurals.notification_summary_count, lines.size, lines.size),
          )
          .joinToString(" ")
      LogUtils.d(TAG, "Reading a summary of %d notifications", lines.size)
      say(intro, interrupt = true)
      val gap = NotificationSettings.int(prefs, KEY_SUMMARY_GAP, 0).coerceIn(0, 10)
      if (gap == 0) {
        lines.forEach { say(it) }
        return
      }
      val handler = Handler(Looper.getMainLooper())
      lines.forEachIndexed { index, line ->
        handler.postDelayed({ say(line) }, (index + 1) * gap * 1000L)
      }
    }
  }
}
