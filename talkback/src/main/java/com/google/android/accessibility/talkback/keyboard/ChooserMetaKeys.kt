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

import android.view.KeyEvent

/**
 * The modifier keys of a character chooser of the SandFly keyboard, whose meta key states are
 * cleared in the text field when the chooser closes.
 *
 * These are the modifiers held when the chooser opened, such as Alt of Alt + Space, and those
 * pressed while it was open. The text field gets their presses and releases but not the keys
 * between them, which SandFly or the chooser take. On keyboards whose modifiers also apply to the
 * next key when pressed and released alone (KeyCharacterMap.MODIFIER_BEHAVIOR_CHORDED_OR_TOGGLED,
 * such as the Zinwa Q25's), a TextView's key listener then treats them as pressed alone and applies
 * them to the next key typed: after Alt + Space, Space types Alt + Space, U+EF01, which opens
 * Android's character picker.
 */
class ChooserMetaKeys {
  /** The meta state of the last key event the keyboard got: the modifiers held. */
  private var held = 0

  /** The meta key states to clear when the chooser closes. */
  private var states = 0

  /** Follows a key event the keyboard got, with [choosing] whether a chooser is open. */
  fun onKeyEvent(metaState: Int, choosing: Boolean) {
    held = metaState
    if (choosing) {
      states = states or statesOf(metaState)
    }
  }

  /** A chooser opened, with the modifiers held now. */
  fun onOpen() {
    states = states or statesOf(held)
  }

  /** The chooser closed. Returns the meta key states to clear, for InputConnection.clearMetaKeyStates. */
  fun onClose(): Int {
    val cleared = states
    states = 0
    return cleared
  }

  /** Forgets the modifiers held, as when input starts in another field. */
  fun reset() {
    held = 0
    states = 0
  }

  companion object {
    /** The meta key states, as InputConnection.clearMetaKeyStates takes them, of [metaState]. */
    @JvmStatic
    fun statesOf(metaState: Int): Int {
      var states = 0
      if (metaState and KeyEvent.META_SHIFT_MASK != 0) states = states or KeyEvent.META_SHIFT_ON
      if (metaState and KeyEvent.META_ALT_MASK != 0) states = states or KeyEvent.META_ALT_ON
      if (metaState and KeyEvent.META_SYM_ON != 0) states = states or KeyEvent.META_SYM_ON
      return states
    }

    /** [states] named for the log, such as "Shift, Alt", or "none". */
    @JvmStatic
    fun describe(states: Int): String {
      val names = buildList {
        if (states and KeyEvent.META_SHIFT_ON != 0) add("Shift")
        if (states and KeyEvent.META_ALT_ON != 0) add("Alt")
        if (states and KeyEvent.META_SYM_ON != 0) add("Sym")
      }
      return if (names.isEmpty()) "none" else names.joinToString(", ")
    }
  }
}
