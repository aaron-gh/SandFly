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

import android.Manifest
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import java.util.Calendar

/**
 * Reads what the device is doing, for the notification settings and rules. Each part that can't
 * be read, such as a Wi-Fi name without location access, is left at its default.
 */
class DeviceStateReader(private val context: Context) {
  /** The app in front, which the service tells as windows change. */
  @Volatile var foregroundApp: String = ""

  /**
   * Reads the parts of the state that [settings] and their rules use, or every part when
   * [settings] is null. The rest is left at its default.
   */
  fun read(settings: NotificationSettings? = null): DeviceState {
    val used =
      settings?.rules?.filter { it.enabled }?.flatMap { rule -> rule.conditions.map { it.type } }?.toSet()
    fun needs(type: RuleCondition.Type, bySettings: Boolean = false) =
      used == null || bySettings || type in used
    val headphones = settings == null || settings.headphones != HeadphoneMode.ALWAYS
    val audio = context.getSystemService(AudioManager::class.java)
    val calendar = Calendar.getInstance()
    val day = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1
    val battery =
      if (needs(RuleCondition.Type.BATTERY_BELOW) || needs(RuleCondition.Type.CHARGING)) {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
      } else {
        null
      }
    val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val ringer =
      if (needs(RuleCondition.Type.RINGER_SILENT, settings?.let { it.honourSilent || it.honourVibrate } == true)) {
        audio?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
      } else {
        AudioManager.RINGER_MODE_NORMAL
      }
    return DeviceState(
      screenOn = context.getSystemService(PowerManager::class.java)?.isInteractive != false,
      locked =
        needs(RuleCondition.Type.LOCKED, settings?.whenLocked == false) &&
          context.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true,
      bluetoothDevices =
        if (needs(RuleCondition.Type.BLUETOOTH_DEVICE, headphones)) bluetoothDevices() else emptySet(),
      wiredHeadphones = needs(RuleCondition.Type.WIRED_HEADPHONES, headphones) && wiredHeadphones(audio),
      batteryPercent = if (level >= 0 && scale > 0) level * 100 / scale else 100,
      charging =
        status == BatteryManager.BATTERY_STATUS_CHARGING ||
          status == BatteryManager.BATTERY_STATUS_FULL,
      landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE,
      minuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE),
      dayOfWeek = day,
      wifiName = if (needs(RuleCondition.Type.WIFI_NETWORK)) wifiName() else "",
      foregroundApp = foregroundApp,
      ringerSilent = ringer == AudioManager.RINGER_MODE_SILENT,
      ringerVibrate = ringer == AudioManager.RINGER_MODE_VIBRATE,
      mediaPlaying =
        needs(RuleCondition.Type.MEDIA_PLAYING, settings?.media == MediaBehavior.SKIP) &&
          audio?.isMusicActive == true,
      carMode =
        (settings == null || settings.skipInCar) &&
          context.getSystemService(UiModeManager::class.java)?.currentModeType ==
            Configuration.UI_MODE_TYPE_CAR,
    )
  }

  private fun wiredHeadphones(audio: AudioManager?): Boolean =
    audio?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.any {
      it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
        it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
        it.type == AudioDeviceInfo.TYPE_USB_HEADSET
    } == true

  /** The Bluetooth audio devices connected, by address, when SandFly may see them. */
  @SuppressLint("MissingPermission")
  private fun bluetoothDevices(): Set<String> {
    if (!canUseBluetooth(context)) return emptySet()
    // BluetoothManager.getConnectedDevices takes only GATT profiles and throws for HEADSET and A2DP.
    // AudioDeviceInfo has addresses from Android 9.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return emptySet()
    val audio = context.getSystemService(AudioManager::class.java) ?: return emptySet()
    return try {
      audio
        .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        .filter { it.type in BLUETOOTH_OUTPUTS && it.address.isNotEmpty() }
        .map { it.address.uppercase() }
        .toSet()
    } catch (e: RuntimeException) {
      emptySet()
    }
  }

  @Suppress("DEPRECATION") // The network's name still comes from WifiInfo.
  private fun wifiName(): String {
    if (!canReadWifiName(context)) return ""
    val wifi = context.applicationContext.getSystemService(WifiManager::class.java) ?: return ""
    val ssid =
      try {
        wifi.connectionInfo?.ssid
      } catch (e: RuntimeException) {
        null
      } ?: return ""
    return ssid.removeSurrounding("\"").takeIf { it != UNKNOWN_SSID }
      .orEmpty()
  }

  companion object {
    /** What Android gives as the name of a network it won't name. */
    private const val UNKNOWN_SSID = "<unknown ssid>"
    private val BLUETOOTH_OUTPUTS =
      setOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER,
      )

    /** Whether SandFly may list Bluetooth devices, which needs a permission on Android 12. */
    fun canUseBluetooth(context: Context): Boolean =
      Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
          PackageManager.PERMISSION_GRANTED

    /** Whether SandFly may read the Wi-Fi network's name, which needs location access. */
    fun canReadWifiName(context: Context): Boolean =
      context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
  }
}
