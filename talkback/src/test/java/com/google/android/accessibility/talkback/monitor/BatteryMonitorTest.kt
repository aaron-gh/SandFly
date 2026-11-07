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

package com.google.android.accessibility.talkback.monitor

import android.content.Intent
import android.os.BatteryManager
import com.google.android.accessibility.talkback.Interpretation
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BatteryMonitorTest {
  private val context = RuntimeEnvironment.getApplication()
  private val announced = mutableListOf<Interpretation.Power>()
  private lateinit var monitor: BatteryMonitor

  @Before
  fun setUp() {
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putBoolean(context.getString(R.string.pref_announce_full_charge_key), true)
      .commit()
    monitor = BatteryMonitor(context)
    monitor.setPipeline { _, _, interpretation, _ ->
      (interpretation as? Interpretation.Power)?.let { announced += it }
      true
    }
  }

  private fun battery(level: Int, plugged: Boolean, full: Boolean = false) {
    monitor.onReceiveIntent(
      Intent(Intent.ACTION_BATTERY_CHANGED)
        .putExtra(BatteryManager.EXTRA_LEVEL, level)
        .putExtra(BatteryManager.EXTRA_SCALE, 100)
        .putExtra(BatteryManager.EXTRA_PLUGGED, if (plugged) BatteryManager.BATTERY_PLUGGED_AC else 0)
        .putExtra(
          BatteryManager.EXTRA_STATUS,
          if (full) BatteryManager.BATTERY_STATUS_FULL else BatteryManager.BATTERY_STATUS_CHARGING,
        )
    )
  }

  @Test
  fun fullChargeIsSaidOncePerCharge() {
    battery(98, plugged = true)
    battery(99, plugged = true)
    battery(100, plugged = true, full = true)
    battery(100, plugged = true, full = true)
    assertEquals(1, announced.count { it.fullyCharged })

    battery(100, plugged = false)
    battery(97, plugged = true)
    battery(100, plugged = true, full = true)
    assertEquals(2, announced.count { it.fullyCharged })
  }

  @Test
  fun topUpOnTheChargerIsNotSaidAgain() {
    battery(99, plugged = true)
    battery(100, plugged = true, full = true)
    battery(99, plugged = true)
    battery(100, plugged = true, full = true)
    battery(99, plugged = true)
    battery(100, plugged = true)
    assertEquals(1, announced.count { it.fullyCharged })
  }

  @Test
  fun batteryFullWhenSandFlyStartsIsNotSaid() {
    battery(100, plugged = true, full = true)
    battery(100, plugged = true, full = true)
    assertEquals(0, announced.size)
  }

  @Test
  fun nothingIsSaidWithTheSettingOff() {
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putBoolean(context.getString(R.string.pref_announce_full_charge_key), false)
      .commit()
    battery(99, plugged = true)
    battery(100, plugged = true, full = true)
    assertEquals(0, announced.size)
  }
}
