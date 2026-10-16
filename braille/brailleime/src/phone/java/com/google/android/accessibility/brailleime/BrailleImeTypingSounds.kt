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

package com.google.android.accessibility.brailleime

import android.content.Context
import android.media.AudioManager
import com.google.android.accessibility.braille.common.BrailleUserPreferences
import com.google.android.accessibility.utils.output.ThemeSounds

/**
 * Plays Android's keyboard sounds as the braille keyboard types, when the user turns on typing
 * sounds: a key click for a character, and the space, delete and return sounds for those actions.
 * A sound theme can replace each by its theme name.
 */
object BrailleImeTypingSounds {
  /** The kind of key to sound like. */
  enum class Key(internal val effect: Int, internal val themeName: String) {
    CHARACTER(AudioManager.FX_KEYPRESS_STANDARD, "braille_keyboard_character"),
    SPACE(AudioManager.FX_KEYPRESS_SPACEBAR, "braille_keyboard_space"),
    DELETE(AudioManager.FX_KEYPRESS_DELETE, "braille_keyboard_delete"),
    NEW_LINE(AudioManager.FX_KEYPRESS_RETURN, "braille_keyboard_new_line"),
  }

  /**
   * The sound effect volume. Given a volume, Android plays the sound even when touch sounds are
   * off in its settings, since the user turned typing sounds on here.
   */
  private const val VOLUME = 0.5f

  @JvmStatic
  fun play(context: Context, key: Key) {
    if (!BrailleUserPreferences.readTypingSounds(context)) {
      return
    }
    if (ThemeSounds.play(key.themeName, VOLUME)) {
      return
    }
    context.getSystemService(AudioManager::class.java)?.playSoundEffect(key.effect, VOLUME)
  }
}
