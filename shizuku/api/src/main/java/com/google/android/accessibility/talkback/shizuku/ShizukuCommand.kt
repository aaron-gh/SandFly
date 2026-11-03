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

package com.google.android.accessibility.talkback.shizuku

/**
 * A command that SandFly runs as the shell user, through Shizuku.
 *
 * The service gets a command by its ordinal, and the SandFly add-on may run an older or newer
 * service than SandFly, so add new commands only at the end.
 */
enum class ShizukuCommand {
  WIFI,
  BLUETOOTH,
  MOBILE_DATA,
  AIRPLANE_MODE,
  LOCATION,
  BATTERY_SAVER,
  DARK_THEME,
  NFC,
  DATA_SAVER,
  /**
   * Turns the Wi-Fi hotspot on or off. There is no shell command for it, so the service calls
   * Android's TetheringManager as the shell user, which may tether.
   */
  HOTSPOT,
  /** Blocks the microphone for a user, as the system's Microphone access switch does. */
  MICROPHONE_BLOCKED,
  CAMERA_BLOCKED,
  /** Adds an app's tile to the system's quick settings, which SystemUI needs to run it. */
  ADD_TILE,
  /** Clicks an app's tile in the system's quick settings, without opening them. */
  CLICK_TILE,
  /** Types text as key events, into apps that have no text field to set text in. */
  INPUT_TEXT,
  /** Sends key events: up to 100 key code names, separated by spaces. */
  INPUT_KEYEVENT,
  /** Starts an accessibility shortcut's activity that other apps may not start. */
  START_SHORTCUT_ACTIVITY,
  /**
   * Runs an accessibility service's accessibility shortcut, as Android's own shortcuts do, without
   * the service being one of them. The argument is the service's component, with " button" after
   * it for a service that the shortcut opens rather than turns on or off.
   */
  ACCESSIBILITY_SHORTCUT,
  /** Clears the output that Android keeps for accessibility audio. It takes no argument. */
  RESET_ACCESSIBILITY_AUDIO;

  /**
   * The command, as adb would run it after `adb shell`, or null if [argument] isn't one it takes.
   * Switches take [ON] or [OFF], the sensor switches with `@` and a user ID after it, and tiles a
   * flattened component name.
   */
  fun shellCommand(argument: String): List<String>? {
    val match = SWITCH.matchEntire(argument)
    val on = match?.let { it.groupValues[1] == ON }
    val user = match?.groupValues?.get(2)?.ifEmpty { null }
    val component = argument.takeIf { COMPONENT.matches(it) }
    return when (this) {
      WIFI -> on?.let { listOf("cmd", "wifi", "set-wifi-enabled", if (it) "enabled" else "disabled") }
      BLUETOOTH -> on?.let { listOf("cmd", "bluetooth_manager", if (it) "enable" else "disable") }
      MOBILE_DATA -> on?.let { listOf("svc", "data", if (it) "enable" else "disable") }
      AIRPLANE_MODE ->
        on?.let { listOf("cmd", "connectivity", "airplane-mode", if (it) "enable" else "disable") }
      LOCATION -> on?.let { listOf("cmd", "location", "set-location-enabled", it.toString()) }
      BATTERY_SAVER -> on?.let { listOf("cmd", "power", "set-mode", if (it) "1" else "0") }
      DARK_THEME -> on?.let { listOf("cmd", "uimode", "night", if (it) "yes" else "no") }
      NFC -> on?.let { listOf("svc", "nfc", if (it) "enable" else "disable") }
      DATA_SAVER -> on?.let { listOf("cmd", "netpolicy", "set", "restrict-background", it.toString()) }
      HOTSPOT -> null
      MICROPHONE_BLOCKED -> sensorCommand(on, user, "microphone")
      CAMERA_BLOCKED -> sensorCommand(on, user, "camera")
      ADD_TILE -> component?.let { listOf("cmd", "statusbar", "add-tile", it) }
      CLICK_TILE -> component?.let { listOf("cmd", "statusbar", "click-tile", it) }
      // input text reads %s as a space.
      INPUT_TEXT ->
        argument
          .takeIf { it.isNotEmpty() && it.length <= MAX_TEXT && it.none { c -> c.isISOControl() } }
          ?.let { listOf("input", "text", it.replace(" ", "%s")) }
      INPUT_KEYEVENT -> KEY_CODES.matchEntire(argument)?.let { listOf("input", "keyevent") + argument.split(' ') }
      START_SHORTCUT_ACTIVITY ->
        component?.let {
          listOf(
            "am",
            "start",
            "-a",
            "android.intent.action.MAIN",
            "-c",
            "android.intent.category.ACCESSIBILITY_SHORTCUT_TARGET",
            "-n",
            it,
          )
        }
      ACCESSIBILITY_SHORTCUT -> null
      RESET_ACCESSIBILITY_AUDIO -> null
    }
  }

  private fun sensorCommand(on: Boolean?, user: String?, sensor: String): List<String>? {
    if (on == null || user == null) return null
    return listOf("cmd", "sensor_privacy", if (on) "enable" else "disable", user, sensor)
  }

  companion object {
    const val ON = "on"
    const val OFF = "off"

    fun argument(on: Boolean): String = if (on) ON else OFF

    fun argument(on: Boolean, userId: Int): String = argument(on) + "@" + userId

    private val SWITCH = Regex("""(on|off)(?:@(\d{1,6}))?""")

    /** The most characters [INPUT_TEXT] types at once. */
    const val MAX_TEXT = 1000

    private val KEY_CODES = Regex("""KEYCODE_[A-Z0-9_]{1,30}( KEYCODE_[A-Z0-9_]{1,30}){0,99}""")

    /** The suffix of an [ACCESSIBILITY_SHORTCUT] argument for a service the shortcut opens. */
    const val BUTTON = " button"

    /** Returns whether [argument] is a flattened component name. */
    fun isComponent(argument: String): Boolean = COMPONENT.matches(argument)

    private val COMPONENT =
      Regex(
        """[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+/\.?[A-Za-z_$][A-Za-z0-9_$]*(\.[A-Za-z_$][A-Za-z0-9_$]*)*"""
      )
  }
}
