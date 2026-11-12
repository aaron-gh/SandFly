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
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.text.format.DateFormat
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.compositor.rule.EventTypeNotificationStateChangedFeedbackRule
import com.google.android.accessibility.talkback.notificationlist.NotificationListService
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * Speech at set times: the time, every so often, and a summary of the notifications once a day.
 * The alarms are allowed while the device is idle (Doze), exact when SandFly has Alarms and
 * reminders, and set again after each one and when the settings, time or time zone change.
 */
class SpokenSchedules(
  private val context: Context,
  /** Says a text, interrupting what notifications are saying when the second value is true. */
  private val say: (CharSequence, Boolean) -> Unit,
  /** Says the time, in the voice its setting chooses. */
  private val sayTime: (CharSequence) -> Unit,
) {
  private val alarms = context.getSystemService(AlarmManager::class.java)
  private val prefs = SharedPreferencesUtils.getSharedPreferences(context)
  private val clockAction = context.packageName + ACTION_CLOCK_SUFFIX
  private val summaryAction = context.packageName + ACTION_SUMMARY_SUFFIX
  private val clockAlarm = alarmIntent(clockAction)
  private val summaryAlarm = alarmIntent(summaryAction)
  /** Keeps the CPU on until the speech starts, as the alarm's own wake lock ends on return. */
  private val wakeLock =
    context.getSystemService(PowerManager::class.java)
      ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
      ?.apply { setReferenceCounted(false) }
  private var started = false
  private var lastClockAt = 0L
  /** When the clock alarm is set for, to log how late it went off. */
  private var clockAlarmAt = 0L

  private val settingsListener =
    SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      if (key == null || key.startsWith(CLOCK_PREFIX) || key.startsWith(SUMMARY_PREFIX)) schedule()
    }

  private val receiver =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        if (!started) return
        when (intent.action) {
          clockAction -> {
            wakeLock?.acquire(CLOCK_WAKE_MS)
            onClock()
          }
          summaryAction -> {
            wakeLock?.acquire(SUMMARY_WAKE_MS)
            onSummary()
          }
          else -> schedule()
        }
      }
    }

  @MainThread
  fun start() {
    if (started) return
    started = true
    prefs.registerOnSharedPreferenceChangeListener(settingsListener)
    val filter =
      IntentFilter().apply {
        addAction(clockAction)
        addAction(summaryAction)
        addAction(Intent.ACTION_TIME_CHANGED)
        addAction(Intent.ACTION_TIMEZONE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          addAction(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
        }
      }
    ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    schedule()
  }

  @MainThread
  fun stop() {
    if (!started) return
    started = false
    prefs.unregisterOnSharedPreferenceChangeListener(settingsListener)
    context.unregisterReceiver(receiver)
    alarms?.cancel(clockAlarm)
    alarms?.cancel(summaryAlarm)
    if (wakeLock?.isHeld == true) wakeLock.release()
  }

  private fun alarmIntent(action: String): PendingIntent =
    PendingIntent.getBroadcast(
      context,
      0,
      Intent(action).setPackage(context.packageName),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

  private fun schedule() {
    val alarms = alarms ?: return
    alarms.cancel(clockAlarm)
    alarms.cancel(summaryAlarm)
    val interval = NotificationSettings.int(prefs, KEY_CLOCK_INTERVAL, 0)
    val now = System.currentTimeMillis()
    if (interval > 0) {
      clockAlarmAt = nextClockTime(now, interval)
      set(alarms, clockAlarmAt, clockAlarm)
    }
    if (prefs.getBoolean(KEY_SUMMARY_DAILY, false)) {
      val minutes = NotificationSettings.int(prefs, KEY_SUMMARY_TIME, 8 * 60)
      set(alarms, nextDailyTime(now, minutes), summaryAlarm)
    }
  }

  /**
   * Sets an alarm that goes off in Doze too: exact with Alarms and reminders, otherwise when
   * Android chooses, which can be several minutes late.
   */
  private fun set(alarms: AlarmManager, at: Long, operation: PendingIntent) {
    if (canScheduleExact(alarms)) {
      try {
        alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        LogUtils.d(TAG, "Exact alarm at %tT", at)
        return
      } catch (e: SecurityException) {
        // Alarms and reminders was turned off just now.
      }
    }
    alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
    LogUtils.d(TAG, "Inexact alarm at %tT, Alarms and reminders is off", at)
  }

  private fun onClock() {
    LogUtils.d(TAG, "Clock alarm %d ms after its time", System.currentTimeMillis() - clockAlarmAt)
    schedule()
    val elapsed = SystemClock.elapsedRealtime()
    if (lastClockAt != 0L && elapsed - lastClockAt < MIN_CLOCK_GAP_MS) return
    lastClockAt = elapsed
    val text = clockText(context, prefs.getString(KEY_CLOCK_FORMAT, null).orEmpty())
    if (text.isNotBlank()) sayTime(text)
  }

  private fun onSummary() {
    schedule()
    readSummary(context, say)
  }

  companion object {
    private const val TAG = "SpokenSchedules"
    private const val WAKE_LOCK_TAG = "SandFly:SpokenSchedules"
    private const val ACTION_CLOCK_SUFFIX = ".SPOKEN_SCHEDULE_CLOCK"
    private const val ACTION_SUMMARY_SUFFIX = ".SPOKEN_SCHEDULE_SUMMARY"
    private const val CLOCK_WAKE_MS = 15_000L
    private const val SUMMARY_WAKE_MS = 60_000L
    private const val MINUTE_MS = 60_000L
    private const val MINUTES_PER_DAY = 24 * 60
    /** The clock is never said twice within this time, whatever the alarms do. */
    private const val MIN_CLOCK_GAP_MS = 60_000L
    private const val KEEP_FOR_SUMMARY_MS = 60_000L
    private const val RETRY_MS = 1_500L
    private const val CLOCK_PREFIX = "pref_notification_clock"
    private const val SUMMARY_PREFIX = "pref_notification_summary"
    /** Runs the summary's later lines, so that stopping notification speech drops them. */
    private val summaryHandler = Handler(Looper.getMainLooper())
    const val KEY_CLOCK_INTERVAL = "pref_notification_clock_interval"
    const val KEY_CLOCK_FORMAT = "pref_notification_clock_format"
    const val KEY_SUMMARY_DAILY = "pref_notification_summary_daily"
    const val KEY_SUMMARY_TIME = "pref_notification_summary_time"
    const val KEY_SUMMARY_NEWEST_FIRST = "pref_notification_summary_newest_first"
    const val KEY_SUMMARY_NAME = "pref_notification_summary_name"
    const val KEY_SUMMARY_GAP = "pref_notification_summary_gap"

    /**
     * The first minute after [now] at which the clock in [zone] shows a multiple of
     * [intervalMinutes] from midnight. Walks real minutes, so a clock change for daylight saving
     * time never gives a time at or before [now].
     */
    fun nextClockTime(now: Long, intervalMinutes: Int, zone: TimeZone = TimeZone.getDefault()): Long {
      val interval = intervalMinutes.coerceAtLeast(1)
      var at = Math.floorDiv(now, MINUTE_MS) * MINUTE_MS + MINUTE_MS
      repeat(2 * MINUTES_PER_DAY) {
        val minuteOfDay = Math.floorMod(Math.floorDiv(at + zone.getOffset(at), MINUTE_MS), MINUTES_PER_DAY.toLong())
        if (minuteOfDay % interval == 0L) return at
        at += MINUTE_MS
      }
      return now + interval * MINUTE_MS
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

    private fun canScheduleExact(alarms: AlarmManager): Boolean =
      Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    /**
     * Whether the time is said at intervals but Alarms and reminders is off, so the alarms are
     * inexact and the time can be said minutes late.
     */
    fun needsExactAlarms(context: Context): Boolean {
      val alarms = context.getSystemService(AlarmManager::class.java) ?: return false
      val prefs = SharedPreferencesUtils.getSharedPreferences(context)
      return NotificationSettings.int(prefs, KEY_CLOCK_INTERVAL, 0) > 0 && !canScheduleExact(alarms)
    }

    /**
     * The battery level for {battery}, such as "76 percent", or empty when it is unknown. The word,
     * not the % sign, which some text-to-speech engines don't say.
     */
    fun batteryText(context: Context, level: Int, scale: Int): String {
      if (level < 0 || scale <= 0) return ""
      val percent = level * 100 / scale
      return context.resources.getQuantityString(R.plurals.notification_clock_battery, percent, percent)
    }

    /** The time as [format] says, with {time}, {date} and {battery} filled in. */
    fun clockText(context: Context, format: String): String {
      val now = Date()
      val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
      val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
      val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
      val percent = batteryText(context, level, scale)
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
      cancelSummary()
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
          summaryHandler.postDelayed({ readSummary(context, speak, retry = false) }, RETRY_MS)
          return
        }
        say(context.getString(R.string.notification_summary_no_access), interrupt = true)
        return
      }
      val settings = NotificationSettings.read(prefs)
      val decider = NotificationDecider()
      val notifications =
        runCatching { listener.activeNotifications }.getOrNull().orEmpty()
          .filter { NotificationListService.isCounted(context, it) }
          .sortedBy { it.postTime }
          .let { if (prefs.getBoolean(KEY_SUMMARY_NEWEST_FIRST, false)) it.reversed() else it }
      val format = context.getString(R.string.notification_summary_item_format)
      val state = DeviceStateReader(context).read(settings)
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
      lines.forEachIndexed { index, line ->
        summaryHandler.postDelayed({ say(line) }, (index + 1) * gap * 1000L)
      }
    }

    /** Drops the summary's lines that are still to be said. */
    @JvmStatic
    @MainThread
    fun cancelSummary() = summaryHandler.removeCallbacksAndMessages(null)
  }
}
