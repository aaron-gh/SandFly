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
import com.google.android.accessibility.braille.common.BrailleUserPreferences
import com.google.android.accessibility.braille.common.R
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED

/**
 * Plays the braille keyboard's typing sounds, when the user turns on typing sounds: one for a
 * character, and one each for space, delete and new line. They play like SandFly's other sounds,
 * the sound theme's if it has them.
 */
object BrailleImeTypingSounds {
  /** The kind of key to sound like. */
  enum class Key(internal val resId: Int) {
    CHARACTER(R.raw.braille_keyboard_character),
    SPACE(R.raw.braille_keyboard_space),
    DELETE(R.raw.braille_keyboard_delete),
    NEW_LINE(R.raw.braille_keyboard_new_line),
  }

  @JvmStatic
  fun play(context: Context, key: Key) {
    if (!BrailleUserPreferences.readTypingSounds(context)) {
      return
    }
    // The braille keyboard vibrates on its own.
    BrailleIme.feedbackController()?.playAuditoryWithoutHaptic(key.resId, EVENT_ID_UNTRACKED)
  }
}
