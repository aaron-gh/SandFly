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
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.commands.Commands
import com.google.android.accessibility.talkback.gesture.AppGestures

/**
 * Keyboard practice, as NVDA's input help: while it is on, each key pressed is spoken with the
 * command its keys run, and nothing runs. Keys don't reach apps or SandFly's other key handling.
 *
 * Keys are named as the keyboard shortcuts list names them, and the SandFly key pressed alone is
 * also named as the SandFly key. A modifier key is spoken when it goes down, with the command of
 * the modifier keys down pressed alone. Keys pressed twice, keys held down and the second key of
 * two in a row are spoken with the command of that press, as [KeyPresses] and [KeyComboManager]
 * match them.
 * Arrow keys are spoken with their arrow key navigation command while it is on. The keys of Turn
 * keyboard practice on or off, Back and Escape turn it off.
 *
 * A key pressed before keyboard practice turned on is released where it was pressed. A key pressed
 * while it was on is released here, after it turned off too.
 */
class KeyPractice(
  private val context: Context,
  private val manager: KeyComboManager,
  private val speaker: (String) -> Unit,
) {
  /** Whether keyboard practice is on. */
  var isOn = false
    private set

  /** Keys pressed while keyboard practice was on, whose repeats and release are taken too. */
  private val taken = mutableSetOf<Int>()

  /** Keys down now that this took, with the modifier bit each presses, 0 for other keys. */
  private val down = mutableMapOf<Int, Int>()

  private var sessionStart = 0L
  /** Whether the keys pressed since all keys were up started soon after the last press. */
  private var sessionMaybeTwice = false
  /** How many keys other than modifier keys were pressed since all keys were up. */
  private var sessionKeyCount = 0
  /** The one press since all keys were up: modifier keys alone, or one key with its modifiers. */
  private var sessionPress: KeyCombo? = null
  /** Whether a press since all keys were up was the second of two, so not the first of the next. */
  private var sessionTwice = false

  private var lastPress: KeyCombo? = null
  private var lastPressTime = 0L

  /** The first key of two in a row, SandFly key + R or O, while the second may follow. */
  private var sequenceStart: KeyCombo? = null
  private var sequenceStartTime = 0L

  private val handler = Handler(Looper.getMainLooper())
  private var heldCheck: Runnable? = null

  /** Names of commands by value, found once while keyboard practice is on. */
  private val labels = HashMap<String, String?>()

  /** What keys run: the keys as spoken, and the command's name, or null for none. */
  private class Found(val keys: KeyCombo, val name: String?, val turnsOff: Boolean)

  /** Turns keyboard practice on or off, and says which. */
  fun toggle() = setOn(!isOn)

  /** Turns keyboard practice on or off, and says which. */
  fun setOn(on: Boolean) {
    isOn = on
    resetPresses()
    labels.clear()
    speaker(context.getString(if (on) R.string.key_practice_on else R.string.key_practice_off))
  }

  /** Turns keyboard practice off without a word and forgets the keys down, as SandFly stops. */
  fun reset() {
    isOn = false
    taken.clear()
    down.clear()
    resetPresses()
    labels.clear()
  }

  private fun resetPresses() {
    cancelHeld()
    lastPress = null
    sequenceStart = null
    sessionPress = null
    sessionKeyCount = 0
    sessionTwice = false
  }

  /** Returns whether [event] is keyboard practice's, so that nothing else gets it. */
  fun onKeyEvent(event: KeyEvent): Boolean {
    val keyCode = event.keyCode
    if (keyCode == KeyEvent.KEYCODE_UNKNOWN || keyCode == KeyEvent.KEYCODE_POWER) return false
    return when (event.action) {
      KeyEvent.ACTION_DOWN -> {
        if (event.repeatCount > 0) return keyCode in taken
        if (!isOn) {
          taken -= keyCode
          return false
        }
        taken += keyCode
        onPress(event)
        true
      }
      KeyEvent.ACTION_UP -> {
        if (!taken.remove(keyCode)) return false
        onRelease(event)
        true
      }
      else -> keyCode in taken
    }
  }

  private fun onPress(event: KeyEvent) {
    cancelHeld()
    val now = event.eventTime
    if (down.isEmpty()) {
      sessionStart = now
      sessionMaybeTwice = lastPress != null && now - lastPressTime < KeyPresses.TWICE_MS
      sessionKeyCount = 0
      sessionPress = null
      sessionTwice = false
    }
    val heldKey = manager.sandFlyKey?.takeIf { it.isHeldKey }
    val bit = KeyPresses.modifierBitOf(event, heldKey)
    down[event.keyCode] = bit
    val modifiers = manager.modifiersOf(event)
    if (bit != 0) {
      onModifierPress(event, modifiers or bit, bit)
    } else {
      onKeyPress(event, modifiers)
    }
  }

  private fun onRelease(event: KeyEvent) {
    down.remove(event.keyCode)
    cancelHeld()
    if (down.isNotEmpty()) return
    val held = event.eventTime - sessionStart >= KeyPresses.HOLD_MS
    lastPress = if (sessionTwice || held) null else sessionPress
    lastPressTime = event.eventTime
  }

  /** A modifier key, or the SandFly key, went down: [bits] are the modifiers down with it. */
  private fun onModifierPress(event: KeyEvent, bits: Int, bit: Int) {
    val trigger = manager.keyComboModel.triggerModifier
    val combo = KeyPresses.comboOf(bits, trigger)
    val sandFlyAlone = combo == KeyCombo.modifierKeys(KeyComboModel.NO_MODIFIER, true)
    val keys = if (bits == bit) modifierKeyName(event) else keysText(combo)
    val sandFlyName =
      if (sandFlyAlone) context.getString(R.string.key_practice_sandfly_key) else null
    // A modifier key pressed with another key down is part of that key's shortcut, not alone.
    if (sessionKeyCount > 0) {
      speak(keys, sandFlyName, null)
      return
    }
    if (sessionMaybeTwice && combo == lastPress) {
      val twice =
        find(combo.withPressType(KeyCombo.PRESSED_TWICE))
          ?: if (sandFlyAlone) find(KeyCombo.sandFlyKeyPressedTwice()) else null
      if (twice != null) {
        sessionTwice = true
        sessionPress = null
        announce(keysText(twice.keys), twice)
        return
      }
    }
    sessionPress = combo
    val found = find(combo)
    if (found?.turnsOff == true) {
      setOn(false)
      return
    }
    speak(keys, sandFlyName, found?.name)
    scheduleHeld(combo.withPressType(KeyCombo.HELD_DOWN), event.keyCode)
  }

  /** A key other than a modifier key went down, with [modifiers] down. */
  private fun onKeyPress(event: KeyEvent, modifiers: Int) {
    val rawKeyCode = event.keyCode
    if (
      (rawKeyCode == KeyEvent.KEYCODE_BACK || rawKeyCode == KeyEvent.KEYCODE_ESCAPE) &&
        modifiers == KeyComboModel.NO_MODIFIER
    ) {
      setOn(false)
      return
    }
    sessionKeyCount++
    val trigger = manager.keyComboModel.triggerModifier
    val usesTrigger = trigger != KeyComboModel.NO_MODIFIER && modifiers and trigger == trigger
    val keyCode = KeyComboManager.getConvertedKeyCode(event)
    val combo =
      KeyCombo(
        if (usesTrigger) modifiers and trigger.inv() else modifiers,
        KeyComboModel.NO_PREFIX_KEY_CODE,
        keyCode,
        usesTrigger,
      )
    sessionPress = if (sessionKeyCount == 1) combo else null

    // The second key of two in a row, such as SandFly key + O, then H.
    val start = sequenceStart
    sequenceStart = null
    if (
      start != null &&
        event.eventTime - sequenceStartTime <= SEQUENCE_TIMEOUT_MS &&
        manager.isSequenceKeyInfraSupported
    ) {
      val sequence =
        KeyCombo(start.modifiers, start.keyCode, keyCode, start.isTriggerModifierUsed)
      val found = find(sequence)
      if (found != null) {
        announce(keysText(sequence), found)
      } else {
        speak(keysText(sequence), null, null)
      }
      return
    }
    if (manager.isSequenceKeyInfraSupported && combo.isSequenceKeyPrefixCombo) {
      sequenceStart = combo
      sequenceStartTime = event.eventTime
      speak(keysText(combo), null, null)
      return
    }

    if (sessionMaybeTwice && combo == lastPress) {
      val twice = findKeys(combo.withPressType(KeyCombo.PRESSED_TWICE))
      if (twice != null) {
        sessionTwice = true
        sessionPress = null
        announce(keysText(twice.keys), twice)
        return
      }
    }

    arrowKeys(rawKeyCode, combo)?.let { (keys, found) ->
      announce(keys, found)
      return
    }

    val found = findKeys(combo)
    // A shortcut that works only while editing is spoken with its keys as the list shows them.
    val keys = if (found != null && found.keys.isWhileEditing) found.keys else combo
    announce(keysText(keys), found)
    scheduleHeld(combo.withPressType(KeyCombo.HELD_DOWN), rawKeyCode)
  }

  /**
   * The arrow keys of arrow key navigation that [keyCode] presses with the other arrow key down, as
   * spoken, and their command; null when arrow key navigation doesn't take them.
   */
  private fun arrowKeys(keyCode: Int, combo: KeyCombo): Pair<String, Found>? {
    if (combo.isTriggerModifierUsed || keyCode !in ARROWS || !ArrowKeyNav.isOn(context)) {
      return null
    }
    val other = down.keys.firstOrNull { it != keyCode && it in ARROWS } ?: KeyEvent.KEYCODE_UNKNOWN
    val keys = ArrowKeyNav.keys(combo.modifiers, keyCode, other)
    val arrowKeys = ArrowKeyNav.arrowKeysFor(keys) ?: return null
    val value = ArrowKeyNav.commandFor(context, arrowKeys)
    val found =
      if (value == ArrowKeyNav.UNASSIGNED) {
        Found(combo, null, false)
      } else {
        Found(combo, label(value), turnsOff(value))
      }
    return manager.getArrowKeysStringRepresentation(keys) to found
  }

  /**
   * Returns what [combo] runs, pressed as a key: as it is, then as the same keys while editing, and
   * where the keymap's single keys work with the SandFly key too, as the key without it.
   */
  private fun findKeys(combo: KeyCombo): Found? {
    // Numpad Enter and the centre of a D-pad are Enter in the keyboard phone keymaps.
    val keys =
      if (plainKeys() && combo.keyCode in ENTER_KEYS) {
        KeyCombo(
          combo.modifiers,
          combo.prefixKeyCode,
          KeyEvent.KEYCODE_ENTER,
          combo.isTriggerModifierUsed,
        )
      } else {
        combo
      }
    // In browse mode, keys without the SandFly key run their browse mode shortcut first.
    if (
      plainKeys() &&
        manager.isBrowseModeEnabled &&
        !keys.isTriggerModifierUsed &&
        keys.modifiers and KeyCombo.PRESS_TYPES == 0
    ) {
      find(keys.withInBrowseMode(true))?.let {
        return it
      }
    }
    find(keys)?.let {
      return it
    }
    if (!keys.isTriggerModifierUsed && keys.modifiers and KeyCombo.PRESS_TYPES == 0) {
      find(keys.withWhileEditing(true))?.let {
        return it
      }
    }
    if (keys.isTriggerModifierUsed && plainKeys()) {
      val plain = KeyCombo(keys.modifiers, keys.prefixKeyCode, keys.keyCode, false)
      find(plain)?.let {
        return Found(combo, it.name, it.turnsOff)
      }
    }
    return null
  }

  /** Returns the command [combo] runs: the app's shortcut, then the keymap's, or null for none. */
  private fun find(combo: KeyCombo): Found? {
    for ((keys, value) in AppGestures.keys()) {
      if (keys == combo) {
        // Unassigned in the app: the keys go to the app.
        if (value == context.getString(R.string.shortcut_value_unassigned)) return null
        return Found(combo, label(value), turnsOff(value))
      }
    }
    val model = manager.keyComboModel ?: return null
    for ((key, keys) in model.keyComboMap) {
      if (keys != combo) continue
      val shortcutValue = Commands.valueOfShortcutKey(key)
      val action =
        if (shortcutValue == null) {
          TalkBackPhysicalKeyboardShortcut.getActionFromKey(context.resources, key)
        } else {
          null
        }
      val value = shortcutValue ?: Commands.valueOfKeyboardAction(context, action!!)
      val name = label(value) ?: action?.getDescription(context.resources) ?: return null
      val shown =
        if (key in manager.browseModeOnlyCommands) {
          context.getString(R.string.template_keycombo_menu_browse_mode_command, name)
        } else {
          name
        }
      val toggle = action == TalkBackPhysicalKeyboardShortcut.TOGGLE_KEY_PRACTICE
      return Found(combo, shown, toggle || turnsOff(value))
    }
    return null
  }

  /** Whether the command saved as [value] is Turn keyboard practice on or off. */
  private fun turnsOff(value: String): Boolean =
    Commands.keyboardActionOf(context, value) ==
      TalkBackPhysicalKeyboardShortcut.TOGGLE_KEY_PRACTICE

  private fun label(value: String): String? =
    labels.getOrPut(value) { Commands.label(context, value) }

  private fun plainKeys(): Boolean =
    (manager.keyComboModel as? NewKeyComboModel)?.plainKeys() == true

  /** Says [keys] and what they run, or turns keyboard practice off where they turn it off. */
  private fun announce(keys: String, found: Found?) {
    if (found?.turnsOff == true) {
      setOn(false)
      return
    }
    speak(keys, null, found?.name)
  }

  private fun speak(keys: String, what: String?, command: String?) {
    speaker(listOfNotNull(keys, what, command).filter { it.isNotEmpty() }.joinToString(", "))
  }

  /**
   * After [KeyPresses.HOLD_MS], while [keyCode] is still down and no other key went down, says the
   * keys of [held] and their command, where they have one.
   */
  private fun scheduleHeld(held: KeyCombo, keyCode: Int) {
    val found = find(held) ?: return
    val check = Runnable {
      heldCheck = null
      if (isOn && keyCode in down) announce(keysText(held), found)
    }
    heldCheck = check
    handler.postDelayed(check, KeyPresses.HOLD_MS)
  }

  private fun cancelHeld() {
    heldCheck?.let { handler.removeCallbacks(it) }
    heldCheck = null
  }

  /** [combo] as the keyboard shortcuts list names it. */
  private fun keysText(combo: KeyCombo): String {
    val text = manager.getKeyComboStringRepresentation(combo)
    if (text.isNotEmpty()) return text
    // A key the list doesn't name, such as Num.
    return KeyEvent.keyCodeToString(combo.keyCode).removePrefix("KEYCODE_").replace('_', ' ')
  }

  /** The name of the modifier key, or held SandFly key, that [event] presses. */
  private fun modifierKeyName(event: KeyEvent): String {
    val sandFlyKey = manager.sandFlyKey
    if (sandFlyKey != null && sandFlyKey.isKey(event)) return context.getString(sandFlyKey.nameRes)
    val res =
      when (event.keyCode) {
        KeyEvent.KEYCODE_SHIFT_LEFT -> R.string.keycombo_key_modifier_shift
        KeyEvent.KEYCODE_SHIFT_RIGHT -> R.string.keycombo_key_modifier_right_shift
        KeyEvent.KEYCODE_ALT_LEFT -> R.string.keycombo_key_modifier_alt
        KeyEvent.KEYCODE_ALT_RIGHT -> R.string.keycombo_key_modifier_right_alt
        KeyEvent.KEYCODE_CTRL_LEFT,
        KeyEvent.KEYCODE_CTRL_RIGHT -> R.string.keycombo_key_modifier_ctrl
        KeyEvent.KEYCODE_META_LEFT,
        KeyEvent.KEYCODE_META_RIGHT -> R.string.keycombo_key_modifier_meta
        KeyEvent.KEYCODE_SYM -> R.string.keycombo_key_modifier_sym
        KeyEvent.KEYCODE_FUNCTION -> R.string.keycombo_key_modifier_function
        else -> 0
      }
    return if (res != 0) {
      context.getString(res)
    } else {
      KeyEvent.keyCodeToString(event.keyCode).removePrefix("KEYCODE_").replace('_', ' ')
    }
  }

  private companion object {
    /** How long after the first of two keys in a row the second can follow, as KeyComboManager. */
    const val SEQUENCE_TIMEOUT_MS = 3000L

    val ARROWS =
      setOf(
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
      )

    val ENTER_KEYS = setOf(KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_DPAD_CENTER)
  }
}
