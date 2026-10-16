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

package com.google.android.accessibility.talkback.scripting

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.FingerprintGestureController
import android.annotation.SuppressLint
import android.content.Context
import android.view.KeyEvent
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping
import com.google.android.accessibility.utils.gestures.GestureManifold

@SuppressLint("InlinedApi")
object ScriptInput {
  data class Keys(val modifiers: Int, val keyCode: Int, val next: Keys? = null) {
    val id: String
      get() = "$modifiers:$keyCode" + next?.let { ">${it.id}" }.orEmpty()

    val first: Keys
      get() = copy(next = null)
  }

  private val gestures: Map<String, Int> =
    linkedMapOf(
      "swipeUp" to AccessibilityService.GESTURE_SWIPE_UP,
      "swipeDown" to AccessibilityService.GESTURE_SWIPE_DOWN,
      "swipeLeft" to AccessibilityService.GESTURE_SWIPE_LEFT,
      "swipeRight" to AccessibilityService.GESTURE_SWIPE_RIGHT,
      "swipeUpThenDown" to AccessibilityService.GESTURE_SWIPE_UP_AND_DOWN,
      "swipeDownThenUp" to AccessibilityService.GESTURE_SWIPE_DOWN_AND_UP,
      "swipeLeftThenRight" to AccessibilityService.GESTURE_SWIPE_LEFT_AND_RIGHT,
      "swipeRightThenLeft" to AccessibilityService.GESTURE_SWIPE_RIGHT_AND_LEFT,
      "swipeUpThenLeft" to AccessibilityService.GESTURE_SWIPE_UP_AND_LEFT,
      "swipeUpThenRight" to AccessibilityService.GESTURE_SWIPE_UP_AND_RIGHT,
      "swipeDownThenLeft" to AccessibilityService.GESTURE_SWIPE_DOWN_AND_LEFT,
      "swipeDownThenRight" to AccessibilityService.GESTURE_SWIPE_DOWN_AND_RIGHT,
      "swipeLeftThenUp" to AccessibilityService.GESTURE_SWIPE_LEFT_AND_UP,
      "swipeLeftThenDown" to AccessibilityService.GESTURE_SWIPE_LEFT_AND_DOWN,
      "swipeRightThenUp" to AccessibilityService.GESTURE_SWIPE_RIGHT_AND_UP,
      "swipeRightThenDown" to AccessibilityService.GESTURE_SWIPE_RIGHT_AND_DOWN,
      "doubleTap" to AccessibilityService.GESTURE_DOUBLE_TAP,
      "doubleTapAndHold" to AccessibilityService.GESTURE_DOUBLE_TAP_AND_HOLD,
      "twoFingerTap" to AccessibilityService.GESTURE_2_FINGER_SINGLE_TAP,
      "twoFingerDoubleTap" to AccessibilityService.GESTURE_2_FINGER_DOUBLE_TAP,
      "twoFingerTripleTap" to AccessibilityService.GESTURE_2_FINGER_TRIPLE_TAP,
      "twoFingerTapAndHold" to GestureManifold.GESTURE_2_FINGER_SINGLE_TAP_AND_HOLD,
      "twoFingerDoubleTapAndHold" to AccessibilityService.GESTURE_2_FINGER_DOUBLE_TAP_AND_HOLD,
      "twoFingerTripleTapAndHold" to AccessibilityService.GESTURE_2_FINGER_TRIPLE_TAP_AND_HOLD,
      "twoFingerSwipeUp" to AccessibilityService.GESTURE_2_FINGER_SWIPE_UP,
      "twoFingerSwipeDown" to AccessibilityService.GESTURE_2_FINGER_SWIPE_DOWN,
      "twoFingerSwipeLeft" to AccessibilityService.GESTURE_2_FINGER_SWIPE_LEFT,
      "twoFingerSwipeRight" to AccessibilityService.GESTURE_2_FINGER_SWIPE_RIGHT,
      "twoFingerRotateClockwise" to GestureManifold.GESTURE_2_FINGER_ROTATE_CLOCKWISE,
      "twoFingerRotateCounterclockwise" to
        GestureManifold.GESTURE_2_FINGER_ROTATE_COUNTERCLOCKWISE,
      "threeFingerTap" to AccessibilityService.GESTURE_3_FINGER_SINGLE_TAP,
      "threeFingerDoubleTap" to AccessibilityService.GESTURE_3_FINGER_DOUBLE_TAP,
      "threeFingerTripleTap" to AccessibilityService.GESTURE_3_FINGER_TRIPLE_TAP,
      "threeFingerQuadrupleTap" to GestureManifold.GESTURE_3_FINGER_QUADRUPLE_TAP,
      "threeFingerTapAndHold" to AccessibilityService.GESTURE_3_FINGER_SINGLE_TAP_AND_HOLD,
      "threeFingerDoubleTapAndHold" to AccessibilityService.GESTURE_3_FINGER_DOUBLE_TAP_AND_HOLD,
      "threeFingerTripleTapAndHold" to AccessibilityService.GESTURE_3_FINGER_TRIPLE_TAP_AND_HOLD,
      "threeFingerSwipeUp" to AccessibilityService.GESTURE_3_FINGER_SWIPE_UP,
      "threeFingerSwipeDown" to AccessibilityService.GESTURE_3_FINGER_SWIPE_DOWN,
      "threeFingerSwipeLeft" to AccessibilityService.GESTURE_3_FINGER_SWIPE_LEFT,
      "threeFingerSwipeRight" to AccessibilityService.GESTURE_3_FINGER_SWIPE_RIGHT,
      "fourFingerTap" to AccessibilityService.GESTURE_4_FINGER_SINGLE_TAP,
      "fourFingerDoubleTap" to AccessibilityService.GESTURE_4_FINGER_DOUBLE_TAP,
      "fourFingerTripleTap" to AccessibilityService.GESTURE_4_FINGER_TRIPLE_TAP,
      "fourFingerDoubleTapAndHold" to AccessibilityService.GESTURE_4_FINGER_DOUBLE_TAP_AND_HOLD,
      "fourFingerSwipeUp" to AccessibilityService.GESTURE_4_FINGER_SWIPE_UP,
      "fourFingerSwipeDown" to AccessibilityService.GESTURE_4_FINGER_SWIPE_DOWN,
      "fourFingerSwipeLeft" to AccessibilityService.GESTURE_4_FINGER_SWIPE_LEFT,
      "fourFingerSwipeRight" to AccessibilityService.GESTURE_4_FINGER_SWIPE_RIGHT,
    )

  private val fingerprintGestures: Map<String, Int> =
    linkedMapOf(
      "fingerprintSwipeUp" to FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_UP,
      "fingerprintSwipeDown" to FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_DOWN,
      "fingerprintSwipeLeft" to FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_LEFT,
      "fingerprintSwipeRight" to FingerprintGestureController.FINGERPRINT_GESTURE_SWIPE_RIGHT,
    )

  private val gesturesById = gestures.inverted()
  private val fingerprintGesturesById = fingerprintGestures.inverted()

  private val modifierFlags: Map<String, Int> =
    mapOf(
      "shift" to KeyEvent.META_SHIFT_ON,
      "ctrl" to KeyEvent.META_CTRL_ON,
      "control" to KeyEvent.META_CTRL_ON,
      "alt" to KeyEvent.META_ALT_ON,
      "meta" to KeyEvent.META_META_ON,
      "search" to KeyEvent.META_META_ON,
    )

  private val modifierTitles: Map<Int, String> =
    linkedMapOf(
      KeyEvent.META_CTRL_ON to "Ctrl",
      KeyEvent.META_ALT_ON to "Alt",
      KeyEvent.META_SHIFT_ON to "Shift",
      KeyEvent.META_META_ON to "Meta",
    )

  private val keyAliases: Map<String, Int> =
    mapOf(
      "up" to KeyEvent.KEYCODE_DPAD_UP,
      "down" to KeyEvent.KEYCODE_DPAD_DOWN,
      "left" to KeyEvent.KEYCODE_DPAD_LEFT,
      "right" to KeyEvent.KEYCODE_DPAD_RIGHT,
      "backspace" to KeyEvent.KEYCODE_DEL,
      "delete" to KeyEvent.KEYCODE_FORWARD_DEL,
      "esc" to KeyEvent.KEYCODE_ESCAPE,
      "home" to KeyEvent.KEYCODE_MOVE_HOME,
      "end" to KeyEvent.KEYCODE_MOVE_END,
      "pageup" to KeyEvent.KEYCODE_PAGE_UP,
      "pagedown" to KeyEvent.KEYCODE_PAGE_DOWN,
      "return" to KeyEvent.KEYCODE_ENTER,
      "," to KeyEvent.KEYCODE_COMMA,
      "." to KeyEvent.KEYCODE_PERIOD,
      "/" to KeyEvent.KEYCODE_SLASH,
      ";" to KeyEvent.KEYCODE_SEMICOLON,
      "'" to KeyEvent.KEYCODE_APOSTROPHE,
      "-" to KeyEvent.KEYCODE_MINUS,
      "=" to KeyEvent.KEYCODE_EQUALS,
      "[" to KeyEvent.KEYCODE_LEFT_BRACKET,
      "]" to KeyEvent.KEYCODE_RIGHT_BRACKET,
      "\\" to KeyEvent.KEYCODE_BACKSLASH,
      "`" to KeyEvent.KEYCODE_GRAVE,
    )

  const val GESTURE_BINDING = "gesture/"
  const val KEYS_BINDING = "keys/"

  fun gestureBinding(name: String): String = GESTURE_BINDING + name

  fun keysBinding(keys: Keys): String = KEYS_BINDING + keys.id

  val gestureNames: List<String> = (gestures.keys + fingerprintGestures.keys).toList()

  fun isGesture(name: String): Boolean = name in gestures || name in fingerprintGestures

  fun keysFromId(id: String): Keys? {
    val steps =
      id.split('>').map { step ->
        val numbers = step.split(':').mapNotNull { it.toIntOrNull() }
        if (numbers.size != 2) {
          return null
        }
        Keys(numbers[0], numbers[1])
      }
    return steps.reduceRight { step, next -> step.copy(next = next) }
  }

  @JvmStatic fun gestureName(gestureId: Int): String? = gesturesById[gestureId]

  @JvmStatic
  fun fingerprintGestureName(gestureId: Int): String? = fingerprintGesturesById[gestureId]

  fun gestureTitle(context: Context, name: String): String =
    gestures[name]?.let { GestureShortcutMapping.getGestureString(context, it) }
      ?: fingerprintGestures[name]?.let {
        GestureShortcutMapping.getFingerprintGestureString(context, it)
      }
      ?: name

  fun parseKeys(text: String): Keys {
    val steps = text.split(',').map { it.trim() }
    require(steps.size <= 2) { "Keys are one combination, or two after each other: $text" }
    val first = parseStep(steps.first(), text, needsBacktalk = true)
    return if (steps.size == 2) first.copy(next = parseStep(steps[1], text, needsBacktalk = false))
    else first
  }

  fun keysTitle(context: Context, keys: Keys): String {
    val first =
      (listOf(context.getString(R.string.script_keys_backtalk_modifier)) + stepTitle(keys))
        .joinToString("+")
    val next = keys.next ?: return first
    return context.getString(R.string.script_keys_then, first, stepTitle(next).joinToString("+"))
  }

  private fun parseStep(step: String, text: String, needsBacktalk: Boolean): Keys {
    val parts = step.split('+').map { it.trim() }
    require(parts.none { it.isEmpty() }) {
      "Keys look like backtalk+shift+n or backtalk+t, s: $text"
    }
    val modifiers = parts.dropLast(1).map { it.lowercase() }
    require(("backtalk" in modifiers) == needsBacktalk) {
      if (needsBacktalk) "Keys must start with backtalk, Backtalk's modifier key: $text"
      else "Only the first keys include backtalk: $text"
    }
    val flags =
      modifiers
        .filter { it != "backtalk" }
        .fold(0) { flags, name ->
          val flag = modifierFlags[name]
          require(flag != null) { "Unknown modifier $name in $text" }
          flags or flag
        }
    val keyCode =
      keyCode(parts.last())?.takeUnless { KeyEvent.isModifierKey(it) }
        ?: throw IllegalArgumentException("Unknown key ${parts.last()} in $text")
    return Keys(flags, keyCode)
  }

  private fun stepTitle(keys: Keys): List<String> =
    modifierTitles.filterKeys { keys.modifiers and it != 0 }.values + keyName(keys.keyCode)

  private fun keyCode(name: String): Int? =
    keyAliases[name.lowercase()]
      ?: KeyEvent.keyCodeFromString("KEYCODE_" + upperSnake(name)).takeIf {
        it != KeyEvent.KEYCODE_UNKNOWN
      }

  private fun keyName(keyCode: Int): String =
    when (keyCode) {
      KeyEvent.KEYCODE_DEL -> "Backspace"
      KeyEvent.KEYCODE_FORWARD_DEL -> "Delete"
      else ->
        KeyEvent.keyCodeToString(keyCode)
          .removePrefix("KEYCODE_")
          .removePrefix("DPAD_")
          .removePrefix("MOVE_")
          .split('_')
          .joinToString(" ") { part -> part.lowercase().replaceFirstChar { it.uppercase() } }
    }

  private fun upperSnake(name: String): String =
    name.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").uppercase()

  private fun <K, V> Map<K, V>.inverted(): Map<V, K> = entries.associate { (k, v) -> v to k }
}
