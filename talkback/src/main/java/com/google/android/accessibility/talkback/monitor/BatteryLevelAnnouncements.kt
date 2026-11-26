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

package com.google.android.accessibility.talkback.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * The Battery level setting: says the battery level each time it reaches a multiple of a step, such
 * as every 10%, or at a time interval, charging or not. Nothing is said while the screen is off: a
 * level reached or an interval ended while it was off is said once when it turns on. Nothing runs
 * while the setting is Off. The time interval runs on a handler while the screen is on, so it never
 * wakes the device.
 */
class BatteryLevelAnnouncements(
  private val context: Context,
  /** Says the battery level, in the voice its setting chooses. */
  private val say: (CharSequence) -> Unit,
) {
  /** How often the level is said: at every [percent] or every [minutes]. */
  data class Interval(val percent: Int = 0, val minutes: Int = 0)

  private val prefs = SharedPreferencesUtils.getSharedPreferences(context)
  private val power = context.getSystemService(PowerManager::class.java)
  private val handler = Handler(Looper.getMainLooper())
  private var started = false
  private var registered = false
  private var interval: Interval? = null
  private var level = UNKNOWN_LEVEL
  /** When the time interval started, in [SystemClock.elapsedRealtime]. */
  private var since = 0L
  /** When the screen turned off, in [SystemClock.elapsedRealtime], or [SCREEN_IS_ON]. */
  private var screenOffAt = SCREEN_IS_ON
  /** Whether a level step was reached while the screen was off. */
  private var stepReachedWhileOff = false

  private val tick = Runnable { onTick() }

  private val settingsListener =
    SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      if (key == null || key == KEY_INTERVAL) update()
    }

  private val receiver =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
          Intent.ACTION_BATTERY_CHANGED -> onLevel(intent)
          Intent.ACTION_SCREEN_ON -> onScreenOn()
          Intent.ACTION_SCREEN_OFF -> onScreenOff()
        }
      }
    }

  @MainThread
  fun start() {
    if (started) return
    started = true
    prefs.registerOnSharedPreferenceChangeListener(settingsListener)
    update()
  }

  @MainThread
  fun stop() {
    if (!started) return
    started = false
    prefs.unregisterOnSharedPreferenceChangeListener(settingsListener)
    interval = null
    release()
  }

  private fun update() {
    if (!started) return
    val now = parse(prefs.getString(KEY_INTERVAL, null))
    if (now == interval) return
    interval = now
    release()
    if (now == null) return
    since = SystemClock.elapsedRealtime()
    screenOffAt = if (isScreenOn()) SCREEN_IS_ON else since
    stepReachedWhileOff = false
    val filter =
      IntentFilter().apply {
        addAction(Intent.ACTION_BATTERY_CHANGED)
        addAction(Intent.ACTION_SCREEN_ON)
        addAction(Intent.ACTION_SCREEN_OFF)
      }
    // The battery change is sticky: the current level arrives at once and isn't said.
    ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    registered = true
    scheduleTick()
  }

  private fun release() {
    handler.removeCallbacks(tick)
    if (registered) {
      registered = false
      context.unregisterReceiver(receiver)
    }
    level = UNKNOWN_LEVEL
    stepReachedWhileOff = false
  }

  private fun onScreenOff() {
    handler.removeCallbacks(tick)
    if (screenOffAt == SCREEN_IS_ON) screenOffAt = SystemClock.elapsedRealtime()
  }

  /** Says the level once if a step was reached or an interval ended while the screen was off. */
  private fun onScreenOn() {
    val offAt = screenOffAt
    screenOffAt = SCREEN_IS_ON
    val minutes = interval?.minutes ?: 0
    val missed =
      stepReachedWhileOff ||
        (minutes > 0 &&
          offAt != SCREEN_IS_ON &&
          intervalEndedBetween(since, offAt, SystemClock.elapsedRealtime(), minutes * MINUTE_MS))
    stepReachedWhileOff = false
    if (missed && level != UNKNOWN_LEVEL) {
      LogUtils.i(TAG, "Battery level %d%% reached while the screen was off", level)
      sayLevel()
    }
    scheduleTick()
  }

  private fun onLevel(intent: Intent) {
    val raw = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
    if (raw < 0 || scale <= 0) return
    val now = BatteryMonitor.getBatteryLevel(scale, raw)
    val last = level
    level = now
    val step = interval?.percent ?: 0
    if (step > 0 && last != UNKNOWN_LEVEL && reachesStep(last, now, step)) {
      if (isScreenOn()) {
        LogUtils.i(TAG, "Battery level %d%% reached", now)
        sayLevel()
      } else {
        stepReachedWhileOff = true
      }
    }
  }

  private fun scheduleTick() {
    handler.removeCallbacks(tick)
    val minutes = interval?.minutes ?: 0
    if (minutes <= 0 || !isScreenOn()) return
    handler.postDelayed(
      tick,
      nextTickDelay(SystemClock.elapsedRealtime(), since, minutes * MINUTE_MS),
    )
  }

  private fun onTick() {
    if (isScreenOn() && level != UNKNOWN_LEVEL) {
      LogUtils.i(TAG, "Battery level %d%% at the interval", level)
      sayLevel()
    }
    scheduleTick()
  }

  private fun sayLevel() {
    say(context.getString(R.string.template_battery_level, level.toString()))
  }

  private fun isScreenOn(): Boolean = power?.isInteractive ?: true

  companion object {
    /** The Battery level setting. */
    const val KEY_INTERVAL = "pref_battery_level_interval"

    /** The voice the battery level is said with, as the time announcements choose theirs. */
    const val KEY_VOICE = "pref_battery_level_voice_profile"

    private const val TAG = "BatteryLevelAnnouncements"
    private const val UNKNOWN_LEVEL = -1
    private const val SCREEN_IS_ON = -1L
    private const val MINUTE_MS = 60_000L

    /** Returns the interval that a value of the Battery level setting names, null for Off. */
    @JvmStatic
    fun parse(value: String?): Interval? =
      when (value) {
        "percent_5" -> Interval(percent = 5)
        "percent_10" -> Interval(percent = 10)
        "minutes_15" -> Interval(minutes = 15)
        "minutes_30" -> Interval(minutes = 30)
        "minutes_60" -> Interval(minutes = 60)
        else -> null
      }

    /**
     * Returns whether the level, going from [last] to [now] percent, reached a multiple of [step]:
     * 21 to 20 and 19 to 20 do for a step of 10, 20 to 19 and 20 to 21 don't, and a jump over a
     * multiple, such as 21 to 19, does.
     */
    @VisibleForTesting
    fun reachesStep(last: Int, now: Int, step: Int): Boolean {
      if (step <= 0 || now == last) return false
      return if (now < last) {
        // A multiple in [now, last).
        Math.floorDiv(last - 1, step) >= Math.floorDiv(now + step - 1, step)
      } else {
        // A multiple in (last, now].
        Math.floorDiv(now, step) >= Math.floorDiv(last, step) + 1
      }
    }

    /**
     * Returns how long after [now] the next time interval of [intervalMs] since [since] ends, so
     * that the level is said every interval from when the setting started, whenever the screen
     * turns on.
     */
    @VisibleForTesting
    fun nextTickDelay(now: Long, since: Long, intervalMs: Long): Long {
      if (intervalMs <= 0) return 0
      val elapsed = (now - since).coerceAtLeast(0)
      return intervalMs - elapsed % intervalMs
    }

    /**
     * Returns whether a time interval of [intervalMs] counted from [since] ended after [from] and
     * no later than [to], such as while the screen was off.
     */
    @VisibleForTesting
    fun intervalEndedBetween(since: Long, from: Long, to: Long, intervalMs: Long): Boolean {
      if (intervalMs <= 0 || to <= from) return false
      val start = (from - since).coerceAtLeast(0)
      val end = (to - since).coerceAtLeast(0)
      return end / intervalMs > start / intervalMs
    }
  }
}
