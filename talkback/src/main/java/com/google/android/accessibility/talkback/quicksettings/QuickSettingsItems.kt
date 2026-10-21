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

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.core.content.edit
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils

/** A control of SandFly's own, which the quick settings panel can show. */
enum class QuickControl(@StringRes val title: Int, val kind: Kind) {
  WIFI(R.string.quick_settings_wifi, Kind.SWITCH),
  BLUETOOTH(R.string.quick_settings_bluetooth, Kind.SWITCH),
  MOBILE_DATA(R.string.quick_settings_mobile_data, Kind.SWITCH),
  AIRPLANE_MODE(R.string.quick_settings_airplane_mode, Kind.SWITCH),
  DO_NOT_DISTURB(R.string.quick_settings_do_not_disturb, Kind.SWITCH),
  SOUND_MODE(R.string.quick_settings_sound_mode, Kind.CHOICE),
  FLASHLIGHT(R.string.quick_settings_flashlight, Kind.SWITCH),
  FLASHLIGHT_BRIGHTNESS(R.string.quick_settings_flashlight_brightness, Kind.SLIDER),
  LOCATION(R.string.quick_settings_location, Kind.SWITCH),
  AUTO_ROTATE(R.string.quick_settings_auto_rotate, Kind.SWITCH),
  BATTERY_SAVER(R.string.quick_settings_battery_saver, Kind.SWITCH),
  DARK_THEME(R.string.quick_settings_dark_theme, Kind.SWITCH),
  NFC(R.string.quick_settings_nfc, Kind.SWITCH),
  BRIGHTNESS(R.string.quick_settings_brightness, Kind.SLIDER),
  ADAPTIVE_BRIGHTNESS(R.string.quick_settings_adaptive_brightness, Kind.SWITCH),
  MEDIA_VOLUME(R.string.quick_settings_media_volume, Kind.SLIDER),
  CALL_VOLUME(R.string.quick_settings_call_volume, Kind.SLIDER),
  RING_VOLUME(R.string.quick_settings_ring_volume, Kind.SLIDER),
  NOTIFICATION_VOLUME(R.string.quick_settings_notification_volume, Kind.SLIDER),
  ALARM_VOLUME(R.string.quick_settings_alarm_volume, Kind.SLIDER),
  ACCESSIBILITY_VOLUME(R.string.quick_settings_accessibility_volume, Kind.SLIDER),
  SYSTEM_VOLUME(R.string.quick_settings_system_volume, Kind.SLIDER),
  BATTERY(R.string.quick_settings_battery, Kind.INFO);

  enum class Kind {
    SWITCH,
    SLIDER,
    /** A double tap moves to the next of a few values, which are also actions. */
    CHOICE,
    INFO,
  }
}

/** Something in the quick settings panel: one of SandFly's controls, or an app's tile. */
sealed class QuickItem {
  abstract val id: String

  data class Control(val control: QuickControl) : QuickItem() {
    override val id: String
      get() = control.name
  }

  data class Tile(val component: ComponentName) : QuickItem() {
    override val id: String
      get() = TILE_PREFIX + component.flattenToString()
  }

  companion object {
    private const val TILE_PREFIX = "tile:"

    fun parse(id: String): QuickItem? {
      if (id.startsWith(TILE_PREFIX)) {
        return ComponentName.unflattenFromString(id.removePrefix(TILE_PREFIX))?.let { Tile(it) }
      }
      return QuickControl.entries.firstOrNull { it.name == id }?.let { Control(it) }
    }
  }
}

/** What the quick settings panel shows, in order, and whether it opens instead of the system's. */
object QuickSettingsItems {
  private const val PREF_ITEMS = "pref_quick_settings_items"
  private const val SEPARATOR = "\n"

  private val DEFAULT_CONTROLS =
    listOf(
      QuickControl.WIFI,
      QuickControl.BLUETOOTH,
      QuickControl.MOBILE_DATA,
      QuickControl.AIRPLANE_MODE,
      QuickControl.DO_NOT_DISTURB,
      QuickControl.SOUND_MODE,
      QuickControl.FLASHLIGHT,
      QuickControl.LOCATION,
      QuickControl.AUTO_ROTATE,
      QuickControl.BATTERY_SAVER,
      QuickControl.BRIGHTNESS,
      QuickControl.MEDIA_VOLUME,
      QuickControl.RING_VOLUME,
      QuickControl.ACCESSIBILITY_VOLUME,
      QuickControl.BATTERY,
    )

  @JvmStatic
  fun isEnabled(context: Context): Boolean =
    SharedPreferencesUtils.getSharedPreferences(context)
      .getBoolean(
        context.getString(R.string.pref_quick_settings_key),
        context.resources.getBoolean(R.bool.pref_quick_settings_default),
      )

  /**
   * The items the user chose, including controls this device doesn't have and tiles of apps that
   * are gone, so that they keep their place.
   */
  fun saved(prefs: SharedPreferences): List<QuickItem> {
    val value = prefs.getString(PREF_ITEMS, null) ?: return DEFAULT_CONTROLS.map { QuickItem.Control(it) }
    return value.split(SEPARATOR).filter { it.isNotEmpty() }.mapNotNull { QuickItem.parse(it) }.distinct()
  }

  fun save(prefs: SharedPreferences, items: List<QuickItem>) {
    prefs.edit { putString(PREF_ITEMS, items.distinct().joinToString(SEPARATOR) { it.id }) }
  }

  /** The items the panel shows: those this device has, with the [installed] app tiles. */
  fun shown(context: Context, controls: QuickControls, installed: Set<ComponentName>): List<QuickItem> =
    saved(SharedPreferencesUtils.getSharedPreferences(context)).filter { item ->
      when (item) {
        is QuickItem.Control -> controls.isAvailable(item.control)
        is QuickItem.Tile -> item.component in installed
      }
    }
}
