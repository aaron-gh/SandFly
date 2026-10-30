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
import android.accessibilityservice.AccessibilityService
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.res.Resources
import android.hardware.SensorPrivacyManager
import android.media.MediaMetadata
import android.media.MediaRouter2
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.ConnectivityManager
import android.os.Process
import android.provider.AlarmClock
import android.text.format.DateFormat
import com.google.android.accessibility.talkback.notificationlist.NotificationList
import com.google.android.accessibility.talkback.notificationlist.NotificationListService
import com.google.android.accessibility.talkback.notificationlist.NotificationReplyActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
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

/** One of the values of a choice control, such as Vibrate or 30 seconds. */
data class Choice(val label: String, val value: Int)

/**
 * A choice control's value and the values a double tap moves through. [current] can be a value
 * that isn't one of [options], such as a screen timeout set elsewhere.
 */
data class ChoiceState(val current: Choice, val options: List<Choice>)

/** What media plays: its title and app, whether it plays, and the actions it allows. */
data class MediaState(
  val title: String?,
  val app: String?,
  val playing: Boolean,
  val actions: List<Pair<String, () -> Unit>>,
  val playPause: (() -> Unit)?,
)

/**
 * Reads and changes SandFly's own quick settings controls. Android lets apps change volumes,
 * brightness, the flashlight, the ringer and Do Not Disturb themselves, and with permissions that
 * SandFly gives itself through Shizuku, system and secure settings such as auto-rotate, night
 * light and color inversion. Wi-Fi, Bluetooth and the other radios, battery saver, dark theme,
 * data saver and the microphone and camera switches can only be changed by the system and the
 * shell, so SandFly changes them through Shizuku, and without it opens the system's settings.
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
  private val brightnessCurve by lazy { BrightnessCurve.forDevice() }

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

  /**
   * Whether the microphone and camera are blocked, by sensor, which only a dump shows, so the
   * panel reads it in the background and sets it here.
   */
  var sensorPrivacy: Map<Int, Boolean> = emptyMap()

  /**
   * Whether the hotspot is on, which only system apps and the shell can read: from Shizuku when
   * the panel opens, and from Android's broadcasts while it is open.
   */
  private var hotspotOn: Boolean? = null

  private val mediaSessions = context.getSystemService(MediaSessionManager::class.java)
  private val listenerComponent = ComponentName(context, NotificationListService::class.java)
  private var mediaController: MediaController? = null

  private var onChange: (() -> Unit)? = null

  private val receiver =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WIFI_AP_STATE_CHANGED) {
          when (intent.getIntExtra(EXTRA_WIFI_AP_STATE, -1)) {
            WIFI_AP_STATE_ENABLED -> hotspotOn = true
            WIFI_AP_STATE_DISABLED,
            WIFI_AP_STATE_FAILED -> hotspotOn = false
          }
        }
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

  private val sessionsListener =
    MediaSessionManager.OnActiveSessionsChangedListener { onChange?.invoke() }

  private val mediaCallback =
    object : MediaController.Callback() {
      override fun onPlaybackStateChanged(state: PlaybackState?) {
        onChange?.invoke()
      }

      override fun onMetadataChanged(metadata: MediaMetadata?) {
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
        addAction(ConnectivityManager.ACTION_RESTRICT_BACKGROUND_CHANGED)
        addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
        addAction(WIFI_AP_STATE_CHANGED)
      }
    ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
    val resolver = context.contentResolver
    resolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
    resolver.registerContentObserver(Settings.Global.CONTENT_URI, true, observer)
    resolver.registerContentObserver(Settings.Secure.CONTENT_URI, true, observer)
    if (torchCamera != null) {
      camera.registerTorchCallback(torchCallback, handler)
    }
    audio.registerAudioDeviceCallback(audioDevices, handler)
    readShellState { onChange() }
    try {
      mediaSessions?.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, handler)
    } catch (e: SecurityException) {
      // Without notification access, SandFly can't see media sessions.
    }
  }

  fun stop() {
    onChange = null
    runCatching { context.unregisterReceiver(receiver) }
    context.contentResolver.unregisterContentObserver(observer)
    if (torchCamera != null) {
      camera.unregisterTorchCallback(torchCallback)
    }
    audio.unregisterAudioDeviceCallback(audioDevices)
    mediaSessions?.removeOnActiveSessionsChangedListener(sessionsListener)
    mediaController?.unregisterCallback(mediaCallback)
    mediaController = null
  }

  /** Whether SandFly changes a switch through Shizuku. */
  fun usesShizuku(control: QuickControl): Boolean = control in COMMANDS

  /**
   * Reads the switches that only the shell can read, which needs Shizuku to allow SandFly
   * already, then calls [onDone] if the panel is still open.
   */
  fun readShellState(onDone: () -> Unit) {
    if (wifi == null) return
    ShizukuShell.state(context, ShizukuCommand.HOTSPOT) { on ->
      if (onChange == null) return@state
      if (on != null) hotspotOn = on
      onDone()
    }
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
      QuickControl.WIFI,
      QuickControl.HOTSPOT -> wifi != null
      QuickControl.NIGHT_LIGHT -> systemBool("config_nightDisplayAvailable", false)
      QuickControl.EXTRA_DIM ->
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
          systemBool("config_reduceBrightColorsAvailable", false)
      QuickControl.MICROPHONE_ACCESS -> supportsSensorToggle(SensorPrivacy.MICROPHONE)
      QuickControl.CAMERA_ACCESS -> supportsSensorToggle(SensorPrivacy.CAMERA)
      QuickControl.MEDIA_OUTPUT -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
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
        QuickControl.DATA_SAVER ->
          context.getSystemService(ConnectivityManager::class.java)?.restrictBackgroundStatus?.let {
            it != ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED
          }
        QuickControl.HOTSPOT -> hotspotOn
        QuickControl.MICROPHONE_ACCESS -> sensorPrivacy[SensorPrivacy.MICROPHONE]?.not()
        QuickControl.CAMERA_ACCESS -> sensorPrivacy[SensorPrivacy.CAMERA]?.not()
        else -> SECURE_SWITCHES[control]?.let { secure(it)?.let { value -> value != 0 } }
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
    SECURE_SWITCHES[control]?.let { name ->
      return writeSecure(name, if (on) 1 else 0, control, onFailed)
    }
    val command = COMMANDS[control] ?: return Change.FAILED
    if (!ShizukuGrants.isReady()) {
      return openSettings(control)
    }
    val argument =
      if (control == QuickControl.MICROPHONE_ACCESS || control == QuickControl.CAMERA_ACCESS) {
        // The switch says whether the sensor works, and the command whether it is blocked.
        ShizukuCommand.argument(!on, Process.myUserHandle().hashCode())
      } else {
        ShizukuCommand.argument(on)
      }
    ShizukuShell.run(context, command, argument) { worked ->
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
      QuickControl.BRIGHTNESS -> {
        val setting = system(Settings.System.SCREEN_BRIGHTNESS) ?: brightnessCurve.min
        val position = (brightnessCurve.toSlider(setting) * BRIGHTNESS_POSITIONS).roundToInt()
        SliderState(0, BRIGHTNESS_POSITIONS, position, BRIGHTNESS_STEP)
      }
      QuickControl.FLASHLIGHT_BRIGHTNESS -> {
        val id = torchCamera
        if (id == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val level = runCatching { camera.getTorchStrengthLevel(id) }.getOrDefault(1)
        SliderState(1, torchMaxLevel, level, 1)
      }
      else -> null
    }
  }

  /**
   * Sets [control]'s slider to [value]. Returns [Change.OPENED] when a settings screen opened to
   * allow it instead, such as Modify system settings for brightness.
   */
  fun setSlider(control: QuickControl, value: Int, onFailed: () -> Unit): Change {
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
        return Change.FAILED
      }
      return Change.DONE
    }
    return when (control) {
      QuickControl.BRIGHTNESS ->
        writeSystem(
          Settings.System.SCREEN_BRIGHTNESS,
          brightnessCurve.toSetting(value.toDouble() / BRIGHTNESS_POSITIONS),
          onFailed,
        )
      QuickControl.FLASHLIGHT_BRIGHTNESS -> {
        val id = torchCamera ?: return Change.FAILED
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return Change.FAILED
        try {
          camera.turnOnTorchWithStrengthLevel(id, value.coerceIn(1, torchMaxLevel))
          Change.DONE
        } catch (e: Exception) {
          LogUtils.w(TAG, "Cannot change the flashlight brightness: %s", e)
          onFailed()
          Change.FAILED
        }
      }
      else -> Change.FAILED
    }
  }

  /** A slider's value as spoken: a percentage of its range. */
  fun sliderText(state: SliderState): String {
    val range = (state.max - state.min).coerceAtLeast(1)
    return NumberFormat.getPercentInstance().format((state.value - state.min).toDouble() / range)
  }

  /** A choice control's value and the values a double tap moves through. */
  fun choice(control: QuickControl): ChoiceState? =
    when (control) {
      QuickControl.SOUND_MODE -> {
        val current =
          SoundMode.entries.firstOrNull { it.ringerMode == audio.ringerMode } ?: SoundMode.SOUND
        ChoiceState(soundChoice(current), soundModes().map { soundChoice(it) })
      }
      QuickControl.SCREEN_TIMEOUT -> {
        val current = system(Settings.System.SCREEN_OFF_TIMEOUT) ?: return null
        ChoiceState(timeoutChoice(current), SCREEN_TIMEOUTS_MS.map { timeoutChoice(it) })
      }
      QuickControl.FONT_SIZE -> {
        val scale =
          runCatching { Settings.System.getFloat(context.contentResolver, Settings.System.FONT_SCALE) }
            .getOrDefault(1f)
        val sizes =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) FONT_SIZES
          else FONT_SIZES.filter { it <= MAX_FONT_SIZE_BEFORE_14 }
        ChoiceState(fontChoice((scale * 100).roundToInt()), sizes.map { fontChoice(it) })
      }
      else -> null
    }

  fun setChoice(control: QuickControl, value: Int, onFailed: () -> Unit): Change =
    when (control) {
      QuickControl.SOUND_MODE ->
        try {
          audio.ringerMode = value
          Change.DONE
        } catch (e: SecurityException) {
          LogUtils.w(TAG, "Cannot change the sound mode: %s", e)
          Change.FAILED
        }
      QuickControl.SCREEN_TIMEOUT -> writeSystem(Settings.System.SCREEN_OFF_TIMEOUT, value, onFailed)
      QuickControl.FONT_SIZE -> writeSystem(Settings.System.FONT_SCALE, value / 100f, onFailed)
      else -> Change.FAILED
    }

  /**
   * The modes SandFly can set: Vibrate needs a vibrator, and Mute needs Do Not Disturb access,
   * which notification access includes.
   */
  private fun soundModes(): List<SoundMode> =
    SoundMode.entries.filter { mode ->
      when (mode) {
        SoundMode.SOUND -> true
        SoundMode.VIBRATE -> context.getSystemService(Vibrator::class.java)?.hasVibrator() == true
        SoundMode.MUTE -> notifications.isNotificationPolicyAccessGranted
      }
    }

  private fun soundChoice(mode: SoundMode) = Choice(context.getString(mode.title), mode.ringerMode)

  private fun timeoutChoice(ms: Int): Choice {
    val seconds = ms / 1000
    val label =
      if (ms >= NEVER_TIMEOUT_MS) {
        context.getString(R.string.quick_settings_never)
      } else if (seconds < 60) {
        context.resources.getQuantityString(R.plurals.quick_settings_seconds, seconds, seconds)
      } else {
        val minutes = seconds / 60
        context.resources.getQuantityString(R.plurals.quick_settings_minutes, minutes, minutes)
      }
    return Choice(label, ms)
  }

  private fun fontChoice(percent: Int) =
    Choice(NumberFormat.getPercentInstance().format(percent / 100.0), percent)

  /** What a button shows under its name. */
  fun buttonText(control: QuickControl): String? =
    when (control) {
      QuickControl.BATTERY -> batteryText()
      QuickControl.NEXT_ALARM -> {
        val next = context.getSystemService(AlarmManager::class.java)?.nextAlarmClock
        if (next == null) {
          context.getString(R.string.quick_settings_no_alarm)
        } else {
          val pattern =
            DateFormat.getBestDateTimePattern(
              Locale.getDefault(),
              if (DateFormat.is24HourFormat(context)) "EHm" else "Ehma",
            )
          SimpleDateFormat(pattern, Locale.getDefault()).format(Date(next.triggerTime))
        }
      }
      else -> null
    }

  /** What a button does: opens something, which closes the panel, or does something. */
  fun press(control: QuickControl): Change =
    when (control) {
      QuickControl.BATTERY -> open(Intent(Intent.ACTION_POWER_USAGE_SUMMARY))
      QuickControl.SETTINGS -> open(Intent(Settings.ACTION_SETTINGS))
      QuickControl.POWER_MENU ->
        if (
          TalkBackService.getInstance()
            ?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG) == true
        ) {
          Change.DONE
        } else {
          Change.FAILED
        }
      QuickControl.NEXT_ALARM -> {
        val show = context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.showIntent
        try {
          if (show != null) {
            show.send(context, 0, null, null, null, null, NotificationReplyActivity.startOptions())
            Change.OPENED
          } else {
            open(Intent(AlarmClock.ACTION_SHOW_ALARMS))
          }
        } catch (e: PendingIntent.CanceledException) {
          open(Intent(AlarmClock.ACTION_SHOW_ALARMS))
        }
      }
      QuickControl.MEDIA_OUTPUT ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
          MediaRouter2.getInstance(context).showSystemOutputSwitcher()
        ) {
          Change.DONE
        } else {
          open(Intent(Settings.ACTION_SOUND_SETTINGS))
        }
      else -> Change.FAILED
    }

  /**
   * The media that plays, or played last: the playing session if there is one, and otherwise the
   * one Android ranks first. Null when nothing has a session, and when SandFly doesn't have
   * notification access, without which Android doesn't show sessions.
   */
  fun media(): MediaState? {
    val sessions =
      try {
        mediaSessions?.getActiveSessions(listenerComponent).orEmpty()
      } catch (e: SecurityException) {
        return null
      }
    val controller =
      sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        ?: sessions.firstOrNull()
    if (controller?.sessionToken != mediaController?.sessionToken) {
      mediaController?.unregisterCallback(mediaCallback)
      mediaController = controller
      controller?.registerCallback(mediaCallback, handler)
    }
    controller ?: return null
    val state = controller.playbackState
    val supported = state?.actions ?: 0L
    fun supports(action: Long) = supported and action != 0L
    val controls = controller.transportControls
    val playing = state?.state == PlaybackState.STATE_PLAYING
    val actions = ArrayList<Pair<String, () -> Unit>>()
    if (supports(PlaybackState.ACTION_SKIP_TO_PREVIOUS)) {
      actions += context.getString(R.string.notification_list_previous) to { controls.skipToPrevious() }
    }
    if (supports(PlaybackState.ACTION_SKIP_TO_NEXT)) {
      actions += context.getString(R.string.notification_list_next) to { controls.skipToNext() }
    }
    state?.customActions?.forEach { custom ->
      actions += custom.name.toString() to { controls.sendCustomAction(custom, custom.extras) }
    }
    val playPause: (() -> Unit)? =
      when {
        playing && supports(PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE) -> {
          { controls.pause() }
        }
        !playing && supports(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PLAY_PAUSE) -> {
          { controls.play() }
        }
        else -> null
      }
    val app =
      runCatching {
          context.packageManager.getApplicationLabel(
            context.packageManager.getApplicationInfo(controller.packageName, 0)
          )
        }
        .getOrNull()
        ?.toString()
    val title = controller.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim()
    return MediaState(title?.ifEmpty { null }, app, playing, actions, playPause)
  }

  /** Whether SandFly has notification access, which media needs. */
  fun hasNotificationAccess(): Boolean = NotificationList.isAccessGranted(context)

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
      QuickControl.DATA_SAVER -> Intent(Settings.ACTION_DATA_USAGE_SETTINGS)
      QuickControl.HOTSPOT -> Intent(ACTION_TETHER_SETTINGS)
      QuickControl.NIGHT_LIGHT,
      QuickControl.SCREEN_TIMEOUT,
      QuickControl.FONT_SIZE -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
      QuickControl.EXTRA_DIM,
      QuickControl.COLOR_INVERSION,
      QuickControl.COLOR_CORRECTION -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
      QuickControl.MICROPHONE_ACCESS,
      QuickControl.CAMERA_ACCESS ->
        Intent(
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Settings.ACTION_PRIVACY_SETTINGS
          else Settings.ACTION_SECURITY_SETTINGS
        )
      QuickControl.MEDIA,
      QuickControl.MEDIA_OUTPUT -> Intent(Settings.ACTION_SOUND_SETTINGS)
      QuickControl.NEXT_ALARM -> Intent(AlarmClock.ACTION_SHOW_ALARMS)
      QuickControl.FLASHLIGHT,
      QuickControl.FLASHLIGHT_BRIGHTNESS,
      QuickControl.SETTINGS,
      QuickControl.POWER_MENU -> null
    }

  /**
   * Opens what changes a switch without Shizuku: the system's settings panel for it where
   * Android has one, which opens over the quick settings panel, and its settings otherwise.
   */
  fun openSettings(control: QuickControl): Change {
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

  private fun writeSystem(name: String, value: Float, onFailed: () -> Unit): Change {
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

  private fun putSystem(name: String, value: Float): Boolean =
    try {
      Settings.System.putFloat(context.contentResolver, name, value)
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Cannot write %s: %s", name, e)
      false
    }

  /**
   * Writes a secure setting, which needs WRITE_SECURE_SETTINGS. SandFly grants itself that
   * through Shizuku when it can, and otherwise opens the system's settings for [control].
   */
  private fun writeSecure(name: String, value: Int, control: QuickControl, onFailed: () -> Unit): Change {
    if (ShizukuGrants.Grant.WRITE_SECURE_SETTINGS.isGranted(context)) {
      return if (putSecure(name, value)) Change.DONE else Change.FAILED
    }
    if (!ShizukuGrants.isReady()) {
      return openSettings(control)
    }
    ShizukuGrants.request(context, ShizukuGrants.Grant.WRITE_SECURE_SETTINGS) { granted ->
      if (!granted || !putSecure(name, value)) {
        onFailed()
      }
    }
    return Change.STARTED
  }

  private fun putSecure(name: String, value: Int): Boolean =
    try {
      Settings.Secure.putInt(context.contentResolver, name, value)
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Cannot write %s: %s", name, e)
      false
    }

  /** A secure setting, or null if Android doesn't let apps read it. */
  private fun secure(name: String): Int? =
    try {
      Settings.Secure.getInt(context.contentResolver, name, 0)
    } catch (e: RuntimeException) {
      null
    }

  /** A boolean from Android's own configuration, which apps can read but not name directly. */
  private fun systemBool(name: String, default: Boolean): Boolean {
    val resources = Resources.getSystem()
    val id = resources.getIdentifier(name, "bool", "android")
    return if (id == 0) default else runCatching { resources.getBoolean(id) }.getOrDefault(default)
  }

  private fun supportsSensorToggle(sensor: Int): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
      runCatching {
          context.getSystemService(SensorPrivacyManager::class.java)?.supportsSensorToggle(sensor)
        }
        .getOrNull() == true

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
    /** The brightness slider's positions, as percentages of the system's slider. */
    private const val BRIGHTNESS_POSITIONS = 100
    private const val BRIGHTNESS_STEP = 5

    private val SCREEN_TIMEOUTS_MS = listOf(15_000, 30_000, 60_000, 120_000, 300_000, 600_000, 1_800_000)
    /** Font sizes as percentages, as the system's font size slider offers them. */
    private val FONT_SIZES = listOf(85, 100, 115, 130, 150, 180, 200)
    private const val MAX_FONT_SIZE_BEFORE_14 = 130
    /** Some phones offer a screen timeout of Integer.MAX_VALUE, which means never. */
    private const val NEVER_TIMEOUT_MS = Int.MAX_VALUE / 2

    /** Switches that are secure settings, which only apps with WRITE_SECURE_SETTINGS change. */
    private val SECURE_SWITCHES =
      mapOf(
        QuickControl.NIGHT_LIGHT to "night_display_activated",
        QuickControl.EXTRA_DIM to "reduce_bright_colors_activated",
        QuickControl.COLOR_INVERSION to "accessibility_display_inversion_enabled",
        QuickControl.COLOR_CORRECTION to "accessibility_display_daltonizer_enabled",
      )

    /** Not public in Android, which sends them all the same. */
    private val VOLUME_ACTIONS =
      listOf("android.media.VOLUME_CHANGED_ACTION", "android.media.STREAM_MUTE_CHANGED_ACTION")
    private const val NFC_STATE_CHANGED = "android.nfc.action.ADAPTER_STATE_CHANGED"
    private const val MOBILE_DATA = "mobile_data"
    private const val ACTION_ZEN_MODE_SETTINGS = "android.settings.ZEN_MODE_SETTINGS"
    private const val ACTION_TETHER_SETTINGS = "android.settings.TETHER_SETTINGS"
    /** WifiManager's hotspot broadcast and states, which aren't public. */
    private const val WIFI_AP_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED"
    private const val EXTRA_WIFI_AP_STATE = "wifi_state"
    private const val WIFI_AP_STATE_DISABLED = 11
    private const val WIFI_AP_STATE_ENABLED = 13
    private const val WIFI_AP_STATE_FAILED = 14

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
        QuickControl.DATA_SAVER to ShizukuCommand.DATA_SAVER,
        QuickControl.HOTSPOT to ShizukuCommand.HOTSPOT,
        QuickControl.MICROPHONE_ACCESS to ShizukuCommand.MICROPHONE_BLOCKED,
        QuickControl.CAMERA_ACCESS to ShizukuCommand.CAMERA_BLOCKED,
      )
  }
}
