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

package com.google.android.accessibility.talkback.macros

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.SystemClock
import android.provider.AlarmClock
import android.provider.MediaStore
import android.view.KeyEvent
import com.google.android.accessibility.talkback.quicksettings.ChoiceState
import com.google.android.accessibility.talkback.quicksettings.QuickControl
import com.google.android.accessibility.talkback.quicksettings.QuickControls
import com.google.android.accessibility.talkback.shizuku.ShizukuCommand
import com.google.android.accessibility.talkback.shizuku.ShizukuShell
import kotlin.math.roundToInt

/**
 * What macros and scripts do to the device outside the screen: the controls of SandFly's quick
 * settings panel, such as Wi-Fi, the flashlight, Do Not Disturb, brightness and volumes, by the
 * names scripts give them; media keys; and timers, alarms, calls, messages and the camera, through
 * the apps that handle them.
 */
object DeviceActions {
  /** The controls, by their names in scripts and steps. */
  val CONTROLS: Map<String, QuickControl> =
    linkedMapOf(
      "wifi" to QuickControl.WIFI,
      "bluetooth" to QuickControl.BLUETOOTH,
      "mobileData" to QuickControl.MOBILE_DATA,
      "hotspot" to QuickControl.HOTSPOT,
      "airplaneMode" to QuickControl.AIRPLANE_MODE,
      "dataSaver" to QuickControl.DATA_SAVER,
      "doNotDisturb" to QuickControl.DO_NOT_DISTURB,
      "soundMode" to QuickControl.SOUND_MODE,
      "flashlight" to QuickControl.FLASHLIGHT,
      "flashlightBrightness" to QuickControl.FLASHLIGHT_BRIGHTNESS,
      "location" to QuickControl.LOCATION,
      "autoRotate" to QuickControl.AUTO_ROTATE,
      "batterySaver" to QuickControl.BATTERY_SAVER,
      "darkTheme" to QuickControl.DARK_THEME,
      "nightLight" to QuickControl.NIGHT_LIGHT,
      "extraDim" to QuickControl.EXTRA_DIM,
      "colorInversion" to QuickControl.COLOR_INVERSION,
      "colorCorrection" to QuickControl.COLOR_CORRECTION,
      "nfc" to QuickControl.NFC,
      "microphoneAccess" to QuickControl.MICROPHONE_ACCESS,
      "cameraAccess" to QuickControl.CAMERA_ACCESS,
      "brightness" to QuickControl.BRIGHTNESS,
      "adaptiveBrightness" to QuickControl.ADAPTIVE_BRIGHTNESS,
      "screenTimeout" to QuickControl.SCREEN_TIMEOUT,
      "fontSize" to QuickControl.FONT_SIZE,
      "displaySize" to QuickControl.DISPLAY_SIZE,
      "mediaVolume" to QuickControl.MEDIA_VOLUME,
      "callVolume" to QuickControl.CALL_VOLUME,
      "ringVolume" to QuickControl.RING_VOLUME,
      "notificationVolume" to QuickControl.NOTIFICATION_VOLUME,
      "alarmVolume" to QuickControl.ALARM_VOLUME,
      "accessibilityVolume" to QuickControl.ACCESSIBILITY_VOLUME,
      "systemVolume" to QuickControl.SYSTEM_VOLUME,
    )

  /** The media keys, by their names in scripts and steps. */
  val MEDIA_KEYS: Map<String, Int> =
    linkedMapOf(
      "playPause" to KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
      "play" to KeyEvent.KEYCODE_MEDIA_PLAY,
      "pause" to KeyEvent.KEYCODE_MEDIA_PAUSE,
      "next" to KeyEvent.KEYCODE_MEDIA_NEXT,
      "previous" to KeyEvent.KEYCODE_MEDIA_PREVIOUS,
      "stop" to KeyEvent.KEYCODE_MEDIA_STOP,
      "rewind" to KeyEvent.KEYCODE_MEDIA_REWIND,
      "fastForward" to KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
    )

  /** The names of the controls this device has. */
  fun availableControls(context: Context): List<String> {
    val controls = QuickControls(context)
    return CONTROLS.filterValues { controls.isAvailable(it) }.keys.toList()
  }

  /** The kind of the control [name]: switch, slider or choice, or null for no such control. */
  fun kindOf(name: String): QuickControl.Kind? = CONTROLS[name]?.kind

  /**
   * The state of the control [name]: a switch's true or false, a slider's value, or a choice's
   * value and label. Null when there is no such control here, or it can't be read.
   */
  fun state(context: Context, name: String): State? {
    val control = CONTROLS[name] ?: return null
    val controls = QuickControls(context)
    if (!controls.isAvailable(control)) return null
    return when (control.kind) {
      QuickControl.Kind.SWITCH ->
        (if (control == QuickControl.FLASHLIGHT) Flashlight.isOn(context) else controls.isOn(control))?.let { State.Switch(it) }
      QuickControl.Kind.SLIDER -> controls.slider(control)?.let { State.Slider(it.value, it.min, it.max) }
      QuickControl.Kind.CHOICE ->
        controls.choice(control)?.let { choice ->
          State.Choice(
            choice.current.value,
            choice.current.label,
            choice.options.map { it.value to it.label },
            controls.freeChoiceValues(control),
          )
        }
      else -> null
    }
  }

  /** The state of a control. */
  sealed class State {
    abstract val text: String

    data class Switch(val on: Boolean) : State() {
      override val text: String
        get() = if (on) "on" else "off"
    }

    data class Slider(val value: Int, val min: Int, val max: Int) : State() {
      override val text: String
        get() = value.toString()
    }

    /** [range] is the numbers it also takes besides its options, if any. */
    data class Choice(
      val value: Int,
      val label: String,
      val options: List<Pair<Int, String>>,
      val range: IntRange? = null,
    ) : State() {
      override val text: String
        get() = label
    }
  }

  /**
   * Sets the control [name] to [value]: a switch to on, off or toggle; a slider to a number, a
   * percent of its range such as 50%, or by +N or -N; a choice to an option's value or label, or
   * next. [done] gets whether it worked, or why not, on the main thread for changes made through
   * Shizuku and at once for the others.
   */
  fun set(context: Context, name: String, value: String, done: (Boolean, String?) -> Unit) {
    val control = CONTROLS[name] ?: return done(false, "Unknown control $name")
    val controls = QuickControls(context)
    if (!controls.isAvailable(control)) return done(false, "$name isn't on this device")
    val text = value.trim()
    fun finish(change: QuickControls.Change) =
      when (change) {
        QuickControls.Change.DONE, QuickControls.Change.STARTED -> done(true, null)
        QuickControls.Change.OPENED -> done(false, "$name needs a permission; its settings opened")
        QuickControls.Change.FAILED -> done(false, "$name wasn't changed")
      }
    when (control.kind) {
      QuickControl.Kind.SWITCH -> {
        val wanted =
          when (text.lowercase()) {
            "on", "true", "1" -> true
            "off", "false", "0" -> false
            "toggle" -> null
            else -> return done(false, "A switch is on, off or toggle")
          }
        fun apply(on: Boolean) {
          var failed = false
          val change = controls.setSwitch(control, on, onDone = {}, onFailed = { failed = true })
          if (control == QuickControl.FLASHLIGHT && change == QuickControls.Change.DONE) Flashlight.saw(on)
          if (failed) done(false, "$name wasn't changed") else finish(change)
        }
        when {
          wanted != null -> apply(wanted)
          control == QuickControl.FLASHLIGHT -> apply(!Flashlight.isOn(context))
          control == QuickControl.HOTSPOT ->
            ShizukuShell.state(context, ShizukuCommand.HOTSPOT) { on ->
              if (on == null) done(false, "The hotspot can't be read without Shizuku") else apply(!on)
            }
          else -> controls.isOn(control)?.let { apply(!it) } ?: done(false, "$name can't be read, so it can't be toggled")
        }
      }
      QuickControl.Kind.SLIDER -> {
        val slider = controls.slider(control) ?: return done(false, "$name can't be read")
        val target =
          when {
            text.endsWith("%") ->
              text.dropLast(1).trim().toDoubleOrNull()?.let { slider.min + ((slider.max - slider.min) * it / 100).toInt() }
            text.startsWith("+") || (text.startsWith("-") && text.length > 1) ->
              text.toIntOrNull()?.let { slider.value + it * slider.step.coerceAtLeast(1) }
            else -> text.toIntOrNull()
          } ?: return done(false, "A slider takes a number, a percent such as 50%, or +N or -N")
        var failed = false
        val change = controls.setSlider(control, target.coerceIn(slider.min, slider.max)) { failed = true }
        if (failed) done(false, "$name wasn't changed") else finish(change)
      }
      QuickControl.Kind.CHOICE -> {
        val choice = controls.choice(control) ?: return done(false, "$name can't be read")
        val free = controls.freeChoiceValues(control)
        val value =
          choiceValue(choice, text, free)
            ?: return done(
              false,
              "$name has ${choice.options.joinToString { it.label }}" +
                free?.let { ", or any number from ${it.first} to ${it.last}" }.orEmpty(),
            )
        var failed = false
        val change = controls.setChoice(control, value) { failed = true }
        if (failed) done(false, "$name wasn't changed") else finish(change)
      }
      else -> done(false, "$name can't be set")
    }
  }

  /**
   * The value [text] picks for a choice: `next`, an option's value or label, or part of a label.
   * For a choice that also takes any number in [free], a number such as 50, 50.0 or 50% is that
   * number, so that 50 isn't taken as part of the label 150%; null when it is outside [free].
   */
  internal fun choiceValue(choice: ChoiceState, text: String, free: IntRange?): Int? {
    val options = choice.options
    if (text.equals("next", ignoreCase = true)) {
      return options.getOrNull((options.indexOfFirst { it.value == choice.current.value } + 1) % options.size)?.value
    }
    if (free != null) {
      text.removeSuffix("%").trim().toDoubleOrNull()?.let { number ->
        return number.takeIf { it.isFinite() }?.roundToInt()?.takeIf { it in free }
      }
    }
    return (options.firstOrNull { it.value.toString() == text }
        ?: options.firstOrNull { it.label.equals(text, ignoreCase = true) }
        ?: options.firstOrNull { it.label.contains(text, ignoreCase = true) })
      ?.value
  }

  /** Presses the media key [name] for the app that plays media. Returns false for no such key. */
  fun media(context: Context, name: String): Boolean {
    val code = MEDIA_KEYS[name] ?: return false
    val audio = context.getSystemService(AudioManager::class.java) ?: return false
    val time = SystemClock.uptimeMillis()
    audio.dispatchMediaKeyEvent(KeyEvent(time, time, KeyEvent.ACTION_DOWN, code, 0))
    audio.dispatchMediaKeyEvent(KeyEvent(time, time, KeyEvent.ACTION_UP, code, 0))
    return true
  }

  /** The battery level in percent, or null when it can't be read. */
  fun batteryLevel(context: Context): Int? =
    context.getSystemService(BatteryManager::class.java)
      ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
      ?.takeIf { it in 0..100 }

  /** Starts a timer of [seconds] in the clock app, without opening it where the app allows. */
  fun setTimer(context: Context, seconds: Int, label: String?): Boolean =
    start(
      context,
      Intent(AlarmClock.ACTION_SET_TIMER)
        .putExtra(AlarmClock.EXTRA_LENGTH, seconds.coerceIn(1, 86_400))
        .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        .apply { if (!label.isNullOrBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, label) },
    )

  /** Sets an alarm at [hour]:[minute] in the clock app, without opening it where the app allows. */
  fun setAlarm(context: Context, hour: Int, minute: Int, label: String?): Boolean =
    start(
      context,
      Intent(AlarmClock.ACTION_SET_ALARM)
        .putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
        .putExtra(AlarmClock.EXTRA_MINUTES, minute.coerceIn(0, 59))
        .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        .apply { if (!label.isNullOrBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, label) },
    )

  /** Calls [number], or opens the dialler with it when SandFly may not call. */
  fun call(context: Context, number: String): Boolean {
    val canCall = context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
    return start(context, Intent(if (canCall) Intent.ACTION_CALL else Intent.ACTION_DIAL, Uri.fromParts("tel", number.trim(), null)))
  }

  /** Opens the messaging app with a message of [text] to [number], to send. */
  fun message(context: Context, number: String, text: String?): Boolean =
    start(
      context,
      Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number.trim(), null)).apply {
        if (!text.isNullOrEmpty()) putExtra("sms_body", text)
      },
    )

  /** Opens the camera app, for a photo or, for [video], a video. */
  fun camera(context: Context, video: Boolean = false): Boolean =
    start(context, Intent(if (video) MediaStore.INTENT_ACTION_VIDEO_CAMERA else MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))

  private fun start(context: Context, intent: Intent): Boolean =
    try {
      context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
      true
    } catch (e: ActivityNotFoundException) {
      false
    } catch (e: SecurityException) {
      false
    }
}
