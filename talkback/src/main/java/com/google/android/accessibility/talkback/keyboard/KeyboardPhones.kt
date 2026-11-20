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

import android.os.Build
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
      else -> false
    }
  }

  /** Whether this phone is a Zinwa Q25, whose Sym key is Right Alt. */
  private fun isZinwaQ25(manufacturer: String?, model: String?): Boolean =
    manufacturer.orEmpty().lowercase(Locale.ROOT).contains("zinwa") &&
      model.orEmpty().uppercase(Locale.ROOT).contains("Q25")

  /**
   * The SandFly key for this phone's keyboard: Right Alt on the Zinwa Q25, whose Sym key it is, Sym
   * where the keyboard has one, otherwise Alt, as on the first Unihertz Titan.
   */
  @JvmStatic
  fun sandFlyKey(): SandFlyKey =
    sandFlyKey(
      Build.MANUFACTURER,
      Build.MODEL,
      hasSym = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_SYM),
    )

  internal fun sandFlyKey(manufacturer: String?, model: String?, hasSym: Boolean): SandFlyKey =
    when {
      isZinwaQ25(manufacturer, model) -> SandFlyKey.RIGHT_ALT
      hasSym -> SandFlyKey.SYM
      else -> SandFlyKey.ALT
    }
}
