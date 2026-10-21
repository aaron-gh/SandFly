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

package com.google.android.accessibility.talkback.quicksettings

import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.ContentObserver
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.Vibrator
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.shizuku.ShizukuCommand
import com.google.android.accessibility.talkback.shizuku.ShizukuGrants
import com.google.android.accessibility.talkback.shizuku.ShizukuShell
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.text.NumberFormat

/** The ringer modes that the Sound mode control moves between. */
enum class SoundMode(val ringerMode: Int, @StringRes val title: Int) {
  SOUND(AudioManager.RINGER_MODE_NORMAL, R.string.quick_settings_sound),
  VIBRATE(AudioManager.RINGER_MODE_VIBRATE, R.string.quick_settings_vibrate),
  MUTE(AudioManager.RINGER_MODE_SILENT, R.string.quick_settings_mute),
}

/** A slider's range and value. A step is what one swipe up or down changes. */
data class SliderState(val min: Int, val max: Int, val value: Int, val step: Int)

/**
 * Reads and changes SandFly's own quick settings controls. Android lets apps change volumes,
 * brightness, the flashlight, the ringer and Do Not Disturb themselves. Wi-Fi, Bluetooth and the
 * other radios, battery saver and dark theme can only be changed by the system and the shell, so
 * SandFly changes them through Shizuku, and without it opens the system's settings for them.
 *
 * Everything here runs on the main thread.
 */
class QuickControls(private val context: Context) {

  /** How a change was made. */
  enum class Change {
    /** The setting has changed. */
    DONE,
    /** A command is changing it. */
    STARTED,
    /** The system's settings for it opened instead. */
    OPENED,
    FAILED,
  }

  private val audio = context.getSystemService(AudioManager::class.java)
  private val notifications = context.getSystemService(NotificationManager::class.java)
  private val power = context.getSystemService(PowerManager::class.java)
  private val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
  private val camera = context.getSystemService(CameraManager::class.java)
  private val handler = Handler(Looper.getMainLooper())

  /** The camera with the flashlight, found once. */
  private val torchCamera: String? by lazy {
    try {
      camera
        ?.cameraIdList
        ?.map { it to camera.getCameraCharacteristics(it) }
        ?.filter { it.second.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
        ?.sortedBy {
          if (it.second.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) 0 else 1
        }
        ?.firstOrNull()
        ?.first
    } catch (e: Exception) {
      LogUtils.w(TAG, "Cannot find the flashlight: %s", e)
      null
    }
  }

  private val torchMaxLevel: Int by lazy {
    val id = torchCamera
    if (id == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      1
    } else {
      runCatching {
          camera.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL)
        }
        .getOrNull() ?: 1
    }
  }

  /** The flashlight's state, which Android reports only to a torch callback. */
  private var torchOn = false

  private var onChange: (() -> Unit)? = null

  private val receiver =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        onChange?.invoke()
      }
    }

  private val observer =
    object : ContentObserver(handler) {
      override fun onChange(selfChange: Boolean, uri: Uri?) {
        onChange?.invoke()
      }
    }

  private val torchCallback =
    object : CameraManager.TorchCallback() {
      override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
        if (cameraId == torchCamera) {
          torchOn = enabled
          onChange?.invoke()
        }
      }

      override fun onTorchStrengthLevelChanged(cameraId: String, newStrengthLevel: Int) {
        if (cameraId == torchCamera) {
          onChange?.invoke()
        }
      }
    }

  /** Bluetooth audio devices connecting and disconnecting. */
  private val audioDevices =
    object : AudioDeviceCallback() {
      override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
        onChange?.invoke()
      }

      override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
        onChange?.invoke()
      }
    }

  /** Calls [onChange] whenever one of the controls may have changed, until [stop]. */
  fun start(onChange: () -> Unit) {
    this.onChange = onChange
    val filter =
      IntentFilter().apply {
        VOLUME_ACTIONS.forEach { addAction(it) }
        addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
        addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        addAction(LocationManager.MODE_CHANGED_ACTION)
        addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        addAction(Intent.ACTION_BATTERY_CHANGED)
        addAction(NFC_STATE_CHANGED)
      }
    ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
    val resolver = context.contentResolver
    resolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
    resolver.registerContentObserver(Settings.Global.CONTENT_URI, true, observer)
    if (torchCamera != null) {
      camera.registerTorchCallback(torchCallback, handler)
    }
    audio.registerAudioDeviceCallback(audioDevices, handler)
  }

  fun stop() {
    onChange = null
    runCatching { context.unregisterReceiver(receiver) }
    context.contentResolver.unregisterContentObserver(observer)
    if (torchCamera != null) {
      camera.unregisterTorchCallback(torchCallback)
    }
    audio.unregisterAudioDeviceCallback(audioDevices)
  }

  fun isAvailable(control: QuickControl): Boolean {
    val packageManager = context.packageManager
    return when (control) {
      QuickControl.MOBILE_DATA,
      QuickControl.CALL_VOLUME ->
        packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
      QuickControl.BLUETOOTH -> bluetooth() != null
      QuickControl.FLASHLIGHT -> torchCamera != null
      QuickControl.FLASHLIGHT_BRIGHTNESS -> torchMaxLevel > 1
      QuickControl.NFC -> nfc() != null
      QuickControl.WIFI -> wifi != null
      else -> true
    }
  }

  /** Whether a switch is on, or null if it can't be read. */
  fun isOn(control: QuickControl): Boolean? =
    try {
      when (control) {
        QuickControl.WIFI -> wifi?.isWifiEnabled
        QuickControl.BLUETOOTH -> bluetooth()?.isEnabled
        QuickControl.MOBILE_DATA ->
          try {
            context.getSystemService(TelephonyManager::class.java)?.isDataEnabled
          } catch (e: SecurityException) {
            // Before Android 9, isDataEnabled needs the phone permission.
            global(MOBILE_DATA) == 1
          }
        QuickControl.AIRPLANE_MODE -> global(Settings.Global.AIRPLANE_MODE_ON) == 1
        QuickControl.DO_NOT_DISTURB ->
          notifications.currentInterruptionFilter.let {
            it != NotificationManager.INTERRUPTION_FILTER_ALL &&
              it != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
          }
        QuickControl.FLASHLIGHT -> torchOn
        QuickControl.LOCATION ->
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.getSystemService(LocationManager::class.java)?.isLocationEnabled
          } else {
            @Suppress("DEPRECATION") // LocationManager.isLocationEnabled needs Android 9.
            Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE, 0) != 0
          }
        QuickControl.AUTO_ROTATE -> system(Settings.System.ACCELEROMETER_ROTATION) == 1
        QuickControl.BATTERY_SAVER -> power.isPowerSaveMode
        QuickControl.DARK_THEME ->
          (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        QuickControl.NFC -> nfc()?.isEnabled
        QuickControl.ADAPTIVE_BRIGHTNESS ->
          system(Settings.System.SCREEN_BRIGHTNESS_MODE) ==
            Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        else -> null
      }
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Cannot read %s: %s", control, e)
      null
    }

  /**
   * Turns a switch on or off. [onFailed] runs if a command that was started fails, after the
   * system's settings for the switch have opened instead.
   */
  fun setSwitch(control: QuickControl, on: Boolean, onFailed: () -> Unit): Change {
    when (control) {
      QuickControl.FLASHLIGHT -> {
        val id = torchCamera ?: return Change.FAILED
        return try {
          camera.setTorchMode(id, on)
          Change.DONE
        } catch (e: Exception) {
          LogUtils.w(TAG, "Cannot turn the flashlight on or off: %s", e)
          Change.FAILED
        }
      }
      QuickControl.DO_NOT_DISTURB -> {
        if (!notifications.isNotificationPolicyAccessGranted) {
          return open(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
        notifications.setInterruptionFilter(
          if (on) NotificationManager.INTERRUPTION_FILTER_PRIORITY
          else NotificationManager.INTERRUPTION_FILTER_ALL
        )
        return Change.DONE
      }
      QuickControl.AUTO_ROTATE ->
        return writeSystem(Settings.System.ACCELEROMETER_ROTATION, if (on) 1 else 0, onFailed)
      QuickControl.ADAPTIVE_BRIGHTNESS ->
        return writeSystem(
          Settings.System.SCREEN_BRIGHTNESS_MODE,
          if (on) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
          else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
          onFailed,
        )
      else -> {}
    }
    val command = COMMANDS[control] ?: return Change.FAILED
    if (!ShizukuGrants.isReady()) {
      return openSettings(control)
    }
    ShizukuShell.run(context, command, ShizukuCommand.argument(on)) { worked ->
      if (!worked) {
        openSettings(control)
        onFailed()
      }
    }
    return Change.STARTED
  }

  /**
   * What a switch is connected to: the Wi-Fi network, which Android names only with location
   * permission, the mobile carrier, or the Bluetooth audio devices.
   */
  @Suppress("DEPRECATION") // connectionInfo is the only way to get the name without a callback.
  fun subtitle(control: QuickControl): String? =
    try {
      when (control) {
        QuickControl.WIFI -> {
          val info = wifi?.takeIf { it.isWifiEnabled }?.connectionInfo
          if (info == null || info.supplicantState != SupplicantState.COMPLETED) {
            null
          } else {
            info.ssid?.removeSurrounding("\"")?.takeUnless { it == WifiManager.UNKNOWN_SSID }
              ?: context.getString(R.string.quick_settings_connected)
          }
        }
        QuickControl.MOBILE_DATA ->
          context
            .getSystemService(TelephonyManager::class.java)
            ?.takeIf { isOn(control) == true && it.simState == TelephonyManager.SIM_STATE_READY }
            ?.networkOperatorName
            ?.trim()
        QuickControl.BLUETOOTH ->
          audio
            .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .filter { it.type in BLUETOOTH_OUTPUTS }
            .map { it.productName.toString().trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(", ")
        else -> null
      }?.ifEmpty { null }
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Cannot read what %s is connected to: %s", control, e)
      null
    }

  fun slider(control: QuickControl): SliderState? {
    STREAMS[control]?.let { stream ->
      val min =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) audio.getStreamMinVolume(stream) else 0
      return SliderState(min, audio.getStreamMaxVolume(stream), audio.getStreamVolume(stream), 1)
    }
    return when (control) {
      QuickControl.BRIGHTNESS ->
        SliderState(0, MAX_BRIGHTNESS, system(Settings.System.SCREEN_BRIGHTNESS) ?: 0, BRIGHTNESS_STEP)
      QuickControl.FLASHLIGHT_BRIGHTNESS -> {
        val id = torchCamera
        if (id == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val level = runCatching { camera.getTorchStrengthLevel(id) }.getOrDefault(1)
        SliderState(1, torchMaxLevel, level, 1)
      }
      else -> null
    }
  }

  fun setSlider(control: QuickControl, value: Int, onFailed: () -> Unit) {
    STREAMS[control]?.let { stream ->
      try {
        if (audio.getStreamVolume(stream) != value) {
          // The slider says the new volume, so the screen reader doesn't announce it again.
          TalkBackService.getInstance()?.ignoreSelfVolumeChange(stream)
        }
        audio.setStreamVolume(stream, value, 0)
      } catch (e: SecurityException) {
        // Ring and notification volumes can't turn Do Not Disturb on or off without access.
        LogUtils.w(TAG, "Cannot change %s: %s", control, e)
        onFailed()
      }
      return
    }
    when (control) {
      QuickControl.BRIGHTNESS ->
        writeSystem(Settings.System.SCREEN_BRIGHTNESS, value.coerceIn(1, MAX_BRIGHTNESS), onFailed)
      QuickControl.FLASHLIGHT_BRIGHTNESS -> {
        val id = torchCamera ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          try {
            camera.turnOnTorchWithStrengthLevel(id, value.coerceIn(1, torchMaxLevel))
          } catch (e: Exception) {
            LogUtils.w(TAG, "Cannot change the flashlight brightness: %s", e)
            onFailed()
          }
        }
      }
      else -> {}
    }
  }

  /** A slider's value as spoken: a percentage of its range. */
  fun sliderText(state: SliderState): String {
    val range = (state.max - state.min).coerceAtLeast(1)
    return NumberFormat.getPercentInstance().format((state.value - state.min).toDouble() / range)
  }

  fun soundMode(): SoundMode =
    SoundMode.entries.firstOrNull { it.ringerMode == audio.ringerMode } ?: SoundMode.SOUND

  /**
   * The modes SandFly can set: Vibrate needs a vibrator, and Mute needs Do Not Disturb access,
   * which notification access includes.
   */
  fun soundModes(): List<SoundMode> =
    SoundMode.entries.filter { mode ->
      when (mode) {
        SoundMode.SOUND -> true
        SoundMode.VIBRATE -> context.getSystemService(Vibrator::class.java)?.hasVibrator() == true
        SoundMode.MUTE -> notifications.isNotificationPolicyAccessGranted
      }
    }

  fun setSoundMode(mode: SoundMode): Change {
    return try {
      audio.ringerMode = mode.ringerMode
      Change.DONE
    } catch (e: SecurityException) {
      LogUtils.w(TAG, "Cannot change the sound mode: %s", e)
      Change.FAILED
    }
  }

  /** The battery level, and whether it is charging. */
  fun batteryText(): String {
    val battery =
      ContextCompat.registerReceiver(
        context,
        null,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        ContextCompat.RECEIVER_EXPORTED,
      )
    val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    if (level < 0 || scale <= 0) {
      return ""
    }
    val percent = NumberFormat.getPercentInstance().format(level.toDouble() / scale)
    return when (status) {
      BatteryManager.BATTERY_STATUS_CHARGING ->
        context.getString(R.string.quick_settings_battery_charging, percent)
      BatteryManager.BATTERY_STATUS_FULL ->
        context.getString(R.string.quick_settings_battery_full, percent)
      else -> percent
    }
  }

  /** The system's settings for a control, which a long press opens. */
  fun settingsIntent(control: QuickControl): Intent? =
    when (control) {
      QuickControl.WIFI -> Intent(Settings.ACTION_WIFI_SETTINGS)
      QuickControl.BLUETOOTH -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
      QuickControl.MOBILE_DATA -> Intent(Settings.ACTION_WIRELESS_SETTINGS)
      QuickControl.AIRPLANE_MODE -> Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
      QuickControl.DO_NOT_DISTURB -> Intent(ACTION_ZEN_MODE_SETTINGS)
      QuickControl.LOCATION -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
      QuickControl.BATTERY_SAVER -> Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
      QuickControl.BATTERY -> Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
      QuickControl.NFC -> Intent(Settings.ACTION_NFC_SETTINGS)
      QuickControl.AUTO_ROTATE,
      QuickControl.DARK_THEME,
      QuickControl.BRIGHTNESS,
      QuickControl.ADAPTIVE_BRIGHTNESS -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
      QuickControl.SOUND_MODE,
      QuickControl.MEDIA_VOLUME,
      QuickControl.CALL_VOLUME,
      QuickControl.RING_VOLUME,
      QuickControl.NOTIFICATION_VOLUME,
      QuickControl.ALARM_VOLUME,
      QuickControl.SYSTEM_VOLUME -> Intent(Settings.ACTION_SOUND_SETTINGS)
      QuickControl.ACCESSIBILITY_VOLUME -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
      QuickControl.FLASHLIGHT,
      QuickControl.FLASHLIGHT_BRIGHTNESS -> null
    }

  /**
   * Opens what changes a switch without Shizuku: the system's settings panel for it where
   * Android has one, which opens over the quick settings panel, and its settings otherwise.
   */
  private fun openSettings(control: QuickControl): Change {
    val panel =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        when (control) {
          QuickControl.WIFI -> Settings.Panel.ACTION_WIFI
          QuickControl.MOBILE_DATA -> Settings.Panel.ACTION_INTERNET_CONNECTIVITY
          QuickControl.NFC -> Settings.Panel.ACTION_NFC
          else -> null
        }
      } else {
        null
      }
    val intent = panel?.let { Intent(it) } ?: settingsIntent(control) ?: return Change.FAILED
    return open(intent)
  }

  /**
   * Writes a system setting, which needs the user to let SandFly modify system settings. SandFly
   * gives itself that through Shizuku when it can, and otherwise opens the system's screen for it.
   */
  private fun writeSystem(name: String, value: Int, onFailed: () -> Unit): Change {
    if (Settings.System.canWrite(context)) {
      return if (putSystem(name, value)) Change.DONE else Change.FAILED
    }
    if (!ShizukuGrants.isReady()) {
      return open(
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + context.packageName))
      )
    }
    ShizukuGrants.request(context, ShizukuGrants.Grant.WRITE_SETTINGS) { granted ->
      if (!granted || !putSystem(name, value)) {
        onFailed()
      }
    }
    return Change.STARTED
  }

  private fun putSystem(name: String, value: Int): Boolean =
    try {
      Settings.System.putInt(context.contentResolver, name, value)
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Cannot write %s: %s", name, e)
      false
    }

  private fun open(intent: Intent): Change =
    try {
      context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
      Change.OPENED
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Cannot open %s: %s", intent, e)
      Change.FAILED
    }

  private fun bluetooth(): BluetoothAdapter? =
    context.getSystemService(BluetoothManager::class.java)?.adapter

  private fun nfc(): NfcAdapter? =
    if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)) {
      NfcAdapter.getDefaultAdapter(context)
    } else {
      null
    }

  private fun global(name: String): Int? =
    runCatching { Settings.Global.getInt(context.contentResolver, name) }.getOrNull()

  private fun system(name: String): Int? =
    runCatching { Settings.System.getInt(context.contentResolver, name) }.getOrNull()

  companion object {
    private const val TAG = "QuickControls"
    private const val MAX_BRIGHTNESS = 255
    /** About 5% of the brightness range. */
    private const val BRIGHTNESS_STEP = 13

    /** Not public in Android, which sends them all the same. */
    private val VOLUME_ACTIONS =
      listOf("android.media.VOLUME_CHANGED_ACTION", "android.media.STREAM_MUTE_CHANGED_ACTION")
    private const val NFC_STATE_CHANGED = "android.nfc.action.ADAPTER_STATE_CHANGED"
    private const val MOBILE_DATA = "mobile_data"
    private const val ACTION_ZEN_MODE_SETTINGS = "android.settings.ZEN_MODE_SETTINGS"

    private val BLUETOOTH_OUTPUTS =
      setOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_HEARING_AID,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER,
        AudioDeviceInfo.TYPE_BLE_BROADCAST,
      )

    private val STREAMS =
      mapOf(
        QuickControl.MEDIA_VOLUME to AudioManager.STREAM_MUSIC,
        QuickControl.CALL_VOLUME to AudioManager.STREAM_VOICE_CALL,
        QuickControl.RING_VOLUME to AudioManager.STREAM_RING,
        QuickControl.NOTIFICATION_VOLUME to AudioManager.STREAM_NOTIFICATION,
        QuickControl.ALARM_VOLUME to AudioManager.STREAM_ALARM,
        QuickControl.ACCESSIBILITY_VOLUME to AudioManager.STREAM_ACCESSIBILITY,
        QuickControl.SYSTEM_VOLUME to AudioManager.STREAM_SYSTEM,
      )

    private val COMMANDS =
      mapOf(
        QuickControl.WIFI to ShizukuCommand.WIFI,
        QuickControl.BLUETOOTH to ShizukuCommand.BLUETOOTH,
        QuickControl.MOBILE_DATA to ShizukuCommand.MOBILE_DATA,
        QuickControl.AIRPLANE_MODE to ShizukuCommand.AIRPLANE_MODE,
        QuickControl.LOCATION to ShizukuCommand.LOCATION,
        QuickControl.BATTERY_SAVER to ShizukuCommand.BATTERY_SAVER,
        QuickControl.DARK_THEME to ShizukuCommand.DARK_THEME,
        QuickControl.NFC to ShizukuCommand.NFC,
      )
  }
}
