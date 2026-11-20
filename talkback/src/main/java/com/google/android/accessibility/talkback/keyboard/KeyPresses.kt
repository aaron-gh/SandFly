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

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent

/**
 * The presses of the keyboard phone keymap that aren't keys pressed with modifiers, as Key Mapper
 * has them: modifier keys pressed on their own, such as Alt alone or Shift + Sym, any key pressed
 * twice in a row, modifier keys held down on their own, and other keys held down. They work
 * anywhere, text fields too, except other keys held down, which are typing while the user types.
 *
 * SandFly can't send a key on to the app later, so a press that may become a double press isn't
 * held back: the first press goes on as it would, and the second runs the shortcut. A modifier key
 * pressed alone is held back from the app only where that press alone has a shortcut.
 */
class KeyPresses(private val host: Host) {
  /** What the presses need from the keymap. */
  interface Host {
    /** The modifier bits of the SandFly key, or [KeyComboModel.HELD_KEY_MODIFIER]. */
    fun triggerModifier(): Int

    /** The SandFly key, where it is a held key such as Sym. */
    fun heldSandFlyKey(): SandFlyKey?

    /** Whether [combo] is a shortcut that can run now. */
    fun isAssigned(combo: KeyCombo): Boolean

    /** Runs the shortcut [combo]. */
    fun perform(combo: KeyCombo)

    /**
     * Whether a key held down can be a shortcut now: not while the user types into a text field,
     * where holding a letter is the keyboard's.
     */
    fun canHoldKeys(): Boolean
  }

  private val handler = Handler(Looper.getMainLooper())

  /** Keys down now. */
  private val down = mutableSetOf<Int>()
  /** Keys whose press SandFly took, so their release is taken too. */
  private val taken = mutableSetOf<Int>()

  /** Modifier bits of the keys pressed since all keys were up. */
  private var sessionModifiers = 0
  /** Whether only modifier keys were pressed since all keys were up. */
  private var sessionOnlyModifiers = true
  /** Whether the press already ran its shortcut, held down or pressed twice. */
  private var sessionDone = false
  private var sessionStart = 0L
  /** Whether the press started soon after the last one, so it may be its second. */
  private var sessionMaybeTwice = false
  /** The one key, with its modifiers, that the press pressed, or null for none or more. */
  private var sessionKey: KeyCombo? = null
  /** How many keys other than modifier keys the press pressed. */
  private var sessionKeyCount = 0

  /** The last press, which a second one makes a double press. */
  private var lastPress: KeyCombo? = null
  private var lastPressTime = 0L

  private var pendingSingle: Runnable? = null
  private val holdCheck = Runnable { onHeld() }

  /** A key other than a modifier, held back while it may be held down long enough for its shortcut. */
  private var heldKey: KeyCombo? = null
  private var heldKeyCode = 0
  private var heldKeyDone = false
  private val keyHoldCheck = Runnable {
    val combo = heldKey
    if (combo != null && heldKeyCode in down && !heldKeyDone) {
      heldKeyDone = true
      host.perform(combo.withPressType(KeyCombo.HELD_DOWN))
    }
  }

  /** Returns whether [event] was used, so it doesn't go on to the app or other shortcuts. */
  fun onKeyEvent(event: KeyEvent): Boolean {
    val keyCode = event.keyCode
    if (keyCode in IGNORED_KEYS) return false
    return when (event.action) {
      KeyEvent.ACTION_DOWN -> onKeyDown(event)
      KeyEvent.ACTION_UP -> onKeyUp(event)
      else -> taken.contains(keyCode)
    }
  }

  /** Forgets the key press under way, which ran a shortcut of its own, so it isn't the first of two. */
  fun forgetPress() {
    sessionKey = null
    lastPress = null
  }

  /** Forgets the keys down, as when keyboard input is reset. */
  fun clear() {
    down.clear()
    taken.clear()
    handler.removeCallbacks(holdCheck)
    handler.removeCallbacks(keyHoldCheck)
    heldKey = null
    heldKeyCode = 0
    pendingSingle?.let { handler.removeCallbacks(it) }
    pendingSingle = null
    lastPress = null
    sessionDone = false
  }

  private fun onKeyDown(event: KeyEvent): Boolean {
    val keyCode = event.keyCode
    if (event.repeatCount > 0) return keyCode in taken
    if (down.isEmpty()) {
      sessionModifiers = 0
      sessionOnlyModifiers = true
      sessionDone = false
      sessionStart = event.eventTime
      sessionKey = null
      sessionKeyCount = 0
      sessionMaybeTwice =
        lastPress != null && event.eventTime - lastPressTime < TWICE_MS
      if (!sessionMaybeTwice) runPendingSingle()
    }
    down += keyCode
    val bit = modifierBitOf(event)
    if (bit != 0) {
      if (!sessionOnlyModifiers) return false
      sessionModifiers = sessionModifiers or bit
      handler.removeCallbacks(holdCheck)
      handler.postDelayed(holdCheck, HOLD_MS)
      // The second press of modifier keys pressed twice, or a key pressed alone that has a
      // shortcut, doesn't reach the app.
      val last = lastPress
      val secondPress =
        sessionMaybeTwice &&
          last != null &&
          last.isModifierKeys &&
          (bitsOf(last) and bit) != 0 &&
          host.isAssigned(last.withPressType(KeyCombo.PRESSED_TWICE))
      if (secondPress || host.isAssigned(comboOf(bit))) {
        taken += keyCode
        return true
      }
      return false
    }
    // Another key: not modifier keys on their own.
    runPendingSingle()
    sessionOnlyModifiers = false
    handler.removeCallbacks(holdCheck)
    val combo = keyComboOf(event)
    sessionKeyCount++
    sessionKey = if (sessionKeyCount == 1) combo else null
    val last = lastPress
    val twice = combo.withPressType(KeyCombo.PRESSED_TWICE)
    if (sessionMaybeTwice && last == combo && host.isAssigned(twice)) {
      lastPress = null
      sessionDone = true
      taken += keyCode
      host.perform(twice)
      return true
    }
    // A key with a shortcut held down waits to see how long it is held, outside typing; released
    // sooner, it runs its own shortcut, if it has one, instead of going to the app.
    if (heldKey == null &&
      host.isAssigned(combo.withPressType(KeyCombo.HELD_DOWN)) &&
      host.canHoldKeys()
    ) {
      heldKey = combo
      heldKeyCode = keyCode
      heldKeyDone = false
      taken += keyCode
      handler.postDelayed(keyHoldCheck, HOLD_MS)
      return true
    }
    return false
  }

  private fun onKeyUp(event: KeyEvent): Boolean {
    val keyCode = event.keyCode
    down -= keyCode
    val wasTaken = taken.remove(keyCode)
    if (keyCode == heldKeyCode) {
      handler.removeCallbacks(keyHoldCheck)
      val combo = heldKey
      heldKey = null
      heldKeyCode = 0
      if (combo != null && !heldKeyDone) {
        if (host.isAssigned(combo)) host.perform(combo)
      } else {
        // Held down: not the first of two presses.
        sessionKey = null
      }
    }
    if (down.isNotEmpty()) return wasTaken
    handler.removeCallbacks(holdCheck)
    if (sessionDone) {
      lastPress = null
      return wasTaken
    }
    if (!sessionOnlyModifiers) {
      lastPress = sessionKey
      lastPressTime = event.eventTime
      return wasTaken
    }
    if (sessionModifiers == 0 || event.eventTime - sessionStart >= HOLD_MS) {
      lastPress = null
      return wasTaken
    }
    val single = comboOf(sessionModifiers)
    val twice = single.withPressType(KeyCombo.PRESSED_TWICE)
    if (sessionMaybeTwice && lastPress == single && host.isAssigned(twice)) {
      pendingSingle?.let { handler.removeCallbacks(it) }
      pendingSingle = null
      lastPress = null
      host.perform(twice)
      return wasTaken
    }
    runPendingSingle()
    lastPress = single
    lastPressTime = event.eventTime
    if (host.isAssigned(single)) {
      if (host.isAssigned(twice)) {
        // Waits to see whether a second press follows.
        val run = Runnable {
          pendingSingle = null
          host.perform(single)
        }
        pendingSingle = run
        handler.postDelayed(run, TWICE_MS)
      } else {
        host.perform(single)
      }
    }
    return wasTaken
  }

  private fun onHeld() {
    if (!sessionOnlyModifiers || sessionDone || down.isEmpty() || sessionModifiers == 0) return
    val held = comboOf(sessionModifiers).withPressType(KeyCombo.HELD_DOWN)
    if (host.isAssigned(held)) {
      sessionDone = true
      host.perform(held)
    }
  }

  private fun runPendingSingle() {
    val pending = pendingSingle ?: return
    handler.removeCallbacks(pending)
    pending.run()
  }

  private fun isHeldKey(keyCode: Int): Boolean = host.heldSandFlyKey()?.keyCode == keyCode

  /** The modifier bit [event] presses: of Shift, Alt, Ctrl or Meta, or of a held SandFly key. */
  private fun modifierBitOf(event: KeyEvent): Int = modifierBitOf(event, host.heldSandFlyKey())

  /** The shortcut of the modifier keys [bits] pressed on their own. */
  private fun comboOf(bits: Int): KeyCombo = comboOf(bits, host.triggerModifier())

  /** The modifier bits of [combo], the SandFly key's among them. */
  private fun bitsOf(combo: KeyCombo): Int =
    combo.modifiers and KeyCombo.PRESS_TYPES.inv() or
      if (combo.isTriggerModifierUsed) host.triggerModifier() else 0

  /** The shortcut of the key [event] presses, with the modifiers down. */
  private fun keyComboOf(event: KeyEvent): KeyCombo =
    keyComboOf(event, host.heldSandFlyKey(), down.any { isHeldKey(it) }, host.triggerModifier())

  companion object {
    /** The modifier bit [event] presses: of Shift, Alt, Ctrl or Meta, or of [heldKey]. */
    @JvmStatic
    fun modifierBitOf(event: KeyEvent, heldKey: SandFlyKey?): Int {
      if (heldKey != null && heldKey.isHeldKey && heldKey.isKey(event)) {
        return KeyComboModel.HELD_KEY_MODIFIER
      }
      return when (event.keyCode) {
        KeyEvent.KEYCODE_SHIFT_LEFT,
        KeyEvent.KEYCODE_SHIFT_RIGHT -> KeyEvent.META_SHIFT_ON
        KeyEvent.KEYCODE_ALT_LEFT,
        KeyEvent.KEYCODE_ALT_RIGHT -> KeyEvent.META_ALT_ON
        KeyEvent.KEYCODE_CTRL_LEFT,
        KeyEvent.KEYCODE_CTRL_RIGHT -> KeyEvent.META_CTRL_ON
        KeyEvent.KEYCODE_META_LEFT,
        KeyEvent.KEYCODE_META_RIGHT -> KeyEvent.META_META_ON
        else -> 0
      }
    }

    /** The shortcut of the modifier keys [bits] pressed on their own, with [trigger] as the SandFly key. */
    @JvmStatic
    fun comboOf(bits: Int, trigger: Int): KeyCombo {
      val usesTrigger = trigger != KeyComboModel.NO_MODIFIER && bits and trigger == trigger
      return KeyCombo.modifierKeys(if (usesTrigger) bits and trigger.inv() else bits, usesTrigger)
    }

    /**
     * The shortcut of the key [event] presses, with the modifiers down, [heldKey] among them where
     * [heldDown], and [trigger] as the SandFly key.
     */
    @JvmStatic
    fun keyComboOf(event: KeyEvent, heldKey: SandFlyKey?, heldDown: Boolean, trigger: Int): KeyCombo {
      var bits = event.modifiers and KeyCombo.KEY_EVENT_MODIFIER_MASK
      if (heldDown && heldKey != null) {
        bits = heldKey.withoutOwnModifiers(bits, event.metaState) or KeyComboModel.HELD_KEY_MODIFIER
      }
      val usesTrigger = trigger != KeyComboModel.NO_MODIFIER && bits and trigger == trigger
      return KeyCombo(
        if (usesTrigger) bits and trigger.inv() else bits,
        KeyComboModel.NO_PREFIX_KEY_CODE,
        event.keyCode,
        usesTrigger,
      )
    }

    /** How soon after a press the next one makes it a double press. */
    const val TWICE_MS = 350L

    /** How long modifier keys are held down for their held shortcut. */
    const val HOLD_MS = 500L

    /** Volume and power keys have their own presses. */
    private val IGNORED_KEYS =
      setOf(
        KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_VOLUME_MUTE,
        KeyEvent.KEYCODE_POWER,
      )
  }
}
