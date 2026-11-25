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
package com.google.android.accessibility.talkback.keyboard

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.KeyCharacterMap
import android.view.KeyEvent
import java.util.Locale

/**
 * Phones with a built-in keyboard that has no Ctrl or Meta key and no arrow keys, which the
 * keyboard phone keymap is for: the Unihertz Titan phones, BlackBerry KEYone and KEY2, and Zinwa's
 * phones built on BlackBerry hardware.
 */
object KeyboardPhones {
  /** BlackBerry KEYone, KEY2 and KEY2 LE. */
  private val BLACKBERRY_KEYBOARD_MODELS = listOf("BBB100", "BBF100", "BBE100")

  /** Whether this phone is a keyboard phone, read from its manufacturer and model. */
  @JvmStatic
  fun isKeyboardPhone(): Boolean = isKeyboardPhone(Build.MANUFACTURER, Build.MODEL)

  internal fun isKeyboardPhone(manufacturer: String?, model: String?): Boolean {
    val maker = manufacturer.orEmpty().lowercase(Locale.ROOT)
    val name = model.orEmpty().uppercase(Locale.ROOT)
    return when {
      maker.contains("unihertz") -> name.contains("TITAN")
      maker.contains("blackberry") || maker.contains("tcl") ->
        BLACKBERRY_KEYBOARD_MODELS.any { name.startsWith(it) }
      maker.contains("zinwa") -> true
      isZinwaQ25(manufacturer, model) -> true
      else -> false
    }
  }

  /**
   * Whether this phone is a Zinwa Q25, whose Sym key is Right Alt. Its firmware names Xelex as the
   * manufacturer.
   */
  private fun isZinwaQ25(manufacturer: String?, model: String?): Boolean {
    val maker = manufacturer.orEmpty().lowercase(Locale.ROOT)
    return (maker.contains("zinwa") || maker.contains("xelex")) &&
      model.orEmpty().uppercase(Locale.ROOT).contains("Q25")
  }

  private fun isUnihertz(manufacturer: String?): Boolean =
    manufacturer.orEmpty().lowercase(Locale.ROOT).contains("unihertz")

  /** The Unihertz Titan 2 Elite, whose Fn key sends no press SandFly can follow, even as Ctrl. */
  private fun isTitanElite(manufacturer: String?, model: String?): Boolean =
    isUnihertz(manufacturer) && model.orEmpty().uppercase(Locale.ROOT).contains("ELITE")

  /**
   * Whether the Unihertz keyboard settings make the Fn key work as Ctrl, read from the system
   * settings the phone keeps them in.
   */
  private fun isFnSetToCtrl(context: Context): Boolean =
    try {
      val resolver = context.contentResolver
      Settings.System.getInt(resolver, "fn_programmable_key_enable", 0) == 1 &&
        Settings.System.getInt(resolver, "fn_programmable_key_function", 0) == FN_AS_CTRL
    } catch (e: RuntimeException) {
      false
    }

  /**
   * The SandFly key for this phone's keyboard, a key typing doesn't need where it has one, so that
   * Sym stays the keyboard's: Ctrl where the Unihertz Fn key is set to Ctrl, Right Shift on the
   * Unihertz Titan 2 Elite, the Zinwa Q25 and BlackBerry phones with a right Shift, otherwise Sym,
   * or Alt on a keyboard without Sym, as on the first Unihertz Titan.
   */
  @JvmStatic
  fun sandFlyKey(context: Context): SandFlyKey =
    sandFlyKey(
      Build.MANUFACTURER,
      Build.MODEL,
      hasSym = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_SYM),
      hasRightShift = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_SHIFT_RIGHT),
      fnIsCtrl = isUnihertz(Build.MANUFACTURER) && isFnSetToCtrl(context),
    )

  internal fun sandFlyKey(
    manufacturer: String?,
    model: String?,
    hasSym: Boolean,
    hasRightShift: Boolean = false,
    fnIsCtrl: Boolean = false,
  ): SandFlyKey {
    val maker = manufacturer.orEmpty().lowercase(Locale.ROOT)
    return when {
      fnIsCtrl && !isTitanElite(manufacturer, model) -> SandFlyKey.CTRL
      isTitanElite(manufacturer, model) || isZinwaQ25(manufacturer, model) -> SandFlyKey.RIGHT_SHIFT
      (maker.contains("blackberry") || maker.contains("tcl")) && hasRightShift ->
        SandFlyKey.RIGHT_SHIFT
      hasSym -> SandFlyKey.SYM
      else -> SandFlyKey.ALT
    }
  }

  /** The value of the Unihertz Fn key setting for Ctrl. */
  private const val FN_AS_CTRL = 1
}
