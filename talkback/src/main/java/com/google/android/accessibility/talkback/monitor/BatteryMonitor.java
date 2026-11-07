/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.google.android.accessibility.talkback.monitor;

import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.PowerManager;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.talkback.Interpretation;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.Pipeline.InterpretationReceiver;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.broadcast.SameThreadBroadcastReceiver;
import org.checkerframework.checker.nullness.qual.NonNull;

/** Monitor battery charging status changes. Start charging Stop changing */
public class BatteryMonitor extends SameThreadBroadcastReceiver {
  private final Context context;

  private Pipeline.InterpretationReceiver pipeline;

  public static final int UNKNOWN_LEVEL = -1;
  private int batteryLevel = UNKNOWN_LEVEL;
  private boolean powerConnected;
  private boolean powerSaveMode;
  // Whether this charge has been announced as full, or was full before SandFly started.
  private boolean fullAnnounced;
  // Whether a battery change has been received: the first is the state SandFly started in.
  private boolean batteryStateKnown;

  public BatteryMonitor(Context context) {
    this.context = context;
    powerSaveMode = getPowerSaveModeState();
    BatteryManager batteryManager =
        (BatteryManager) this.context.getSystemService(Context.BATTERY_SERVICE);
    powerConnected = batteryManager.isCharging();
  }

  public void setPipeline(@NonNull InterpretationReceiver pipeline) {
    if (pipeline == null) {
      throw new IllegalStateException();
    }
    this.pipeline = pipeline;
  }

  public IntentFilter getFilter() {
    final IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
    intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
    intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
    intentFilter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
    return intentFilter;
  }

  @Override
  public void onReceiveIntent(Intent intent) {
    final String action = intent.getAction();
    if (action == null) {
      return;
    }

    switch (action) {
      case Intent.ACTION_BATTERY_CHANGED -> {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        batteryLevel = getBatteryLevel(scale, level);
        boolean plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        int status =
            intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        onBatteryState(plugged, status == BatteryManager.BATTERY_STATUS_FULL || batteryLevel >= 100);
      }
      case Intent.ACTION_POWER_DISCONNECTED -> {
        powerConnected = false;
        pipeline.input(
            EVENT_ID_UNTRACKED,
            new Interpretation.Power(
                /* connected= */ powerConnected, batteryLevel, /* powerSaveMode= */ powerSaveMode));
      }
      case Intent.ACTION_POWER_CONNECTED -> {
        powerConnected = true;
        pipeline.input(
            EVENT_ID_UNTRACKED,
            new Interpretation.Power(
                /* connected= */ powerConnected, batteryLevel, /* powerSaveMode= */ powerSaveMode));
      }
      case PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> powerSaveMode = getPowerSaveModeState();
      default -> {
        // Do nothing.
      }
    }
  }

  /**
   * Says once per charge that the battery is full, with Announce full charge, while the charger is
   * connected. A charge ends when the charger is disconnected. A battery already full when SandFly
   * starts isn't announced.
   */
  private void onBatteryState(boolean plugged, boolean full) {
    boolean wasKnown = batteryStateKnown;
    batteryStateKnown = true;
    // Only unplugging re-arms it, so a top-up from 99% on the charger isn't announced again.
    if (!plugged) {
      fullAnnounced = false;
      return;
    }
    if (!full || fullAnnounced) {
      return;
    }
    fullAnnounced = true;
    if (!wasKnown
        || pipeline == null
        || !SharedPreferencesUtils.getBooleanPref(
            SharedPreferencesUtils.getSharedPreferences(context),
            context.getResources(),
            R.string.pref_announce_full_charge_key,
            R.bool.pref_announce_full_charge_default)) {
      return;
    }
    pipeline.input(
        EVENT_ID_UNTRACKED,
        new Interpretation.Power(
            /* connected= */ true, batteryLevel, powerSaveMode, /* fullyCharged= */ true));
  }

  @VisibleForTesting
  public static int getBatteryLevel(int scale, int level) {
    return (scale > 0 ? Math.round((level / (float) scale) * 100) : UNKNOWN_LEVEL);
  }

  /**
   * Returns what to say about the battery, such as "Battery 58 percent", with "Charger connected"
   * first while the device is plugged in, and "Battery saver on" last while it's on.
   */
  public String getBatteryStateDescription() {
    String level =
        batteryLevel != UNKNOWN_LEVEL
            ? context.getString(R.string.template_battery_level, String.valueOf(batteryLevel))
            : context.getString(R.string.template_battery_level_unknown);
    StringBuilder description = new StringBuilder();
    if (powerConnected) {
      description.append(context.getString(R.string.value_charger_connected)).append(", ");
    }
    description.append(level);
    if (powerSaveMode) {
      description.append(", ").append(context.getString(R.string.notification_battery_saver_mode));
    }
    return description.toString();
  }

  private boolean getPowerSaveModeState() {
    PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    return powerManager.isPowerSaveMode();
  }
}
