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

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.KeyguardManager
import android.content.Context
import android.graphics.Path
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telecom.TelecomManager
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.gesture.AppGestures
import com.google.android.accessibility.talkback.gesture.SyntheticTouches
import com.google.android.accessibility.talkback.speakerphone.SpeakerphoneInCallService
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.function.BooleanSupplier

/**
 * Letter keys press keypad buttons on keyboards whose letters carry numbers, as on BlackBerry, Zinwa
 * and Unihertz phones, where W, E and R are 1, 2 and 3: on the lock screen PIN pad, in calls, where
 * they play the keypad tones, and in calculators, where the letters' Alt characters, such as + and
 * -, press the operator buttons. A letter on the lock screen before the PIN pad shows opens it, or
 * unlocks the device where no PIN is needed.
 *
 * Keys with a modifier, digit keys and keys typed into a text field being edited aren't taken.
 */
class KeypadKeys(private val service: AccessibilityService) {
  private val handler = Handler(Looper.getMainLooper())

  /** Keys whose press was taken, so that their release is taken too. */
  private val takenKeys = mutableSetOf<Int>()

  /** The key whose call keypad tone is playing. */
  private var toneKey = KeyEvent.KEYCODE_UNKNOWN

  /** Digits typed on the lock screen while its PIN pad opens. */
  private val pendingDigits = StringBuilder()
  private var pendingChecks = 0

  private val typePendingDigits =
    object : Runnable {
      override fun run() {
        if (pendingDigits.isEmpty()) {
          return
        }
        try {
          val pad = lockScreenRoots()
          if (pad.isNotEmpty() && findButton(pad, pendingDigits[0], Screen.LOCK_SCREEN) != null) {
            for (digit in pendingDigits) {
              findButton(pad, digit, Screen.LOCK_SCREEN)?.let(::click)
            }
            pendingDigits.clear()
            return
          }
        } catch (e: RuntimeException) {
          // The lock screen's window may go away while it is read.
          LogUtils.w(TAG, "PIN not typed: %s", e)
          pendingDigits.clear()
          return
        }
        if (++pendingChecks >= PENDING_CHECKS) {
          LogUtils.i(TAG, "The PIN pad did not open")
          pendingDigits.clear()
          return
        }
        handler.postDelayed(this, PENDING_CHECK_MS)
      }
    }

  /**
   * Handles [event] when it presses a keypad button. [editingText] tells whether the user is typing
   * in a text field, which gets its keys; it is asked only where a keypad may be shown.
   */
  fun onKeyEvent(event: KeyEvent, editingText: BooleanSupplier): Boolean {
    val keyCode = event.keyCode
    if (event.action == KeyEvent.ACTION_UP) {
      if (!takenKeys.remove(keyCode)) {
        return false
      }
      if (toneKey == keyCode) {
        toneKey = KeyEvent.KEYCODE_UNKNOWN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          SpeakerphoneInCallService.stopDtmfTone()
        }
      }
      return true
    }
    if (event.action != KeyEvent.ACTION_DOWN) {
      return takenKeys.contains(keyCode)
    }
    if (takenKeys.contains(keyCode)) {
      // Repeats of a taken key.
      return true
    }
    // Digit keys type for themselves, except on the lock screen, where one before the PIN pad shows
    // opens it, as a letter does.
    if (
      event.repeatCount > 0 ||
        !event.hasNoModifiers() ||
        (isDigitKey(keyCode) && !isKeyguardLocked()) ||
        !isOn(service)
    ) {
      if (event.repeatCount == 0 && isKeyguardLocked()) {
        LogUtils.e(
          TAG,
          "Lock screen key %s left alone: modifiers %s, setting on %s",
          KeyEvent.keyCodeToString(keyCode),
          !event.hasNoModifiers(),
          isOn(service),
        )
      }
      return false
    }
    val taken =
      try {
        press(event, editingText)
      } catch (e: RuntimeException) {
        // The app's window may go away while it is read.
        LogUtils.w(TAG, "Keypad key not pressed: %s", e)
        false
      }
    if (taken) {
      takenKeys.add(keyCode)
    }
    return taken
  }

  private fun isKeyguardLocked(): Boolean =
    service.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true

  /** Clears what is waiting: keys held and digits for a PIN pad that is opening. */
  fun reset() {
    takenKeys.clear()
    if (toneKey != KeyEvent.KEYCODE_UNKNOWN && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      SpeakerphoneInCallService.stopDtmfTone()
    }
    toneKey = KeyEvent.KEYCODE_UNKNOWN
    pendingDigits.clear()
    handler.removeCallbacks(typePendingDigits)
  }

  private fun press(event: KeyEvent, editingText: BooleanSupplier): Boolean {
    val keyCode = event.keyCode
    val keyguard = service.getSystemService(KeyguardManager::class.java)
    if (keyguard != null && keyguard.isKeyguardLocked) {
      val onLockScreen = pressOnLockScreen(event, keyguard)
      // Logged at error level so that it shows in the published builds, which log only errors: a
      // key on the lock screen that types nothing is otherwise impossible to trace. No digits.
      LogUtils.e(
        TAG,
        "Lock screen key %s: %s; window in front %s, PIN pad %s, digits waiting %d",
        KeyEvent.keyCodeToString(event.keyCode),
        when (onLockScreen) {
          true -> "taken"
          false -> "left to the lock screen"
          null -> "no lock screen window in front"
        },
        service.rootInActiveWindow?.packageName,
        lockScreenRoots().let { it.isNotEmpty() && hasPinPad(it) },
        pendingDigits.length,
      )
      if (onLockScreen != null) {
        return onLockScreen
      }
    }
    val character = keypadCharacter(event.keyCharacterMap, keyCode) ?: return false
    // Only where a keypad may be shown are the app's window and focus looked up, so that typing
    // elsewhere waits for nothing.
    val inCall = character in CALL_CHARACTERS && isInCall()
    val calculator =
      character in CALCULATOR_CHARACTERS && AppGestures.appShown().let { it.isEmpty() || isCalculator(it) }
    if ((!inCall && !calculator) || editingText.asBoolean) {
      return false
    }
    val root = service.rootInActiveWindow ?: return false
    val packageName = root.packageName?.toString() ?: return false
    if (inCall && isPhoneApp(packageName)) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeakerphoneInCallService.playDtmfTone(character)) {
        toneKey = keyCode
        return true
      }
      return findButton(listOf(root), character, Screen.CALL)?.let(::click) ?: false
    }
    if (calculator && isCalculator(packageName)) {
      return findButton(listOf(root), character, Screen.CALCULATOR)?.let(::click) ?: false
    }
    return false
  }

  /**
   * Presses a PIN pad button for [event] on the lock screen, or opens the PIN pad for a digit, or
   * returns null where no lock screen window is in front, as while a call is shown over it.
   */
  private fun pressOnLockScreen(event: KeyEvent, keyguard: KeyguardManager): Boolean? {
    val roots = lockScreenRoots()
    if (roots.isEmpty()) {
      return null
    }
    // A password field gets the letters. A PIN pad's own entry can be an editable field with input
    // focus too, as on the Zinwa Q25, so the field is the password's only where no PIN pad shows.
    if (
      roots.any { it.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.isEditable == true } &&
        !hasPinPad(roots)
    ) {
      return false
    }
    val keyCode = event.keyCode
    if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
      return findById(roots, SYSTEM_UI, "key_enter")?.let(::click) ?: false
    }
    if (keyCode == KeyEvent.KEYCODE_DEL) {
      return findById(roots, SYSTEM_UI, "delete_button")?.let(::click) ?: false
    }
    // A digit key's digit is its key code's: the Zinwa Q25 sends digit key codes for the letter keys
    // on the lock screen, and its character map has no keys 1 to 9.
    val digit =
      digitOf(keyCode)
        ?: keypadCharacter(event.keyCharacterMap, keyCode)?.takeIf { it.isDigit() }
        ?: return false
    // Where no PIN is needed, as while Extend Unlock keeps the device unlocked or with a swipe lock,
    // a digit unlocks instead.
    if (!keyguard.isDeviceLocked && pendingDigits.isEmpty() && !hasPinPad(roots)) {
      return unlock(roots)
    }
    if (pendingDigits.isNotEmpty()) {
      if (pendingDigits.length < MAX_PENDING_DIGITS) {
        pendingDigits.append(digit)
      }
      return true
    }
    findButton(roots, digit, Screen.LOCK_SCREEN)?.let {
      return click(it)
    }
    // Before the PIN pad shows: open it with a swipe up, then type.
    if (!keyguard.isDeviceSecure || !openPinPad()) {
      return false
    }
    pendingDigits.append(digit)
    pendingChecks = 0
    handler.postDelayed(typePendingDigits, PENDING_CHECK_MS)
    return true
  }

  /** Whether the lock screen shows its PIN pad: its button for 1 is on screen. */
  private fun hasPinPad(roots: List<AccessibilityNodeInfo>): Boolean =
    findById(roots, SYSTEM_UI, buttonIds('1', Screen.LOCK_SCREEN).first()) != null

  /** The roots of the lock screen's windows in front. */
  private fun lockScreenRoots(): List<AccessibilityNodeInfo> {
    val active = service.rootInActiveWindow
    if (active != null && active.packageName?.toString() == SYSTEM_UI) {
      return listOf(active)
    }
    if (active != null) {
      // An app over the lock screen, such as a call, has the keys.
      return emptyList()
    }
    return service.windows.mapNotNull { it.root }.filter { it.packageName?.toString() == SYSTEM_UI }
  }

  /**
   * Dismisses a lock screen that needs no PIN: presses its lock icon, which says "Unlocked", or
   * swipes up across it where no lock icon shows.
   */
  private fun unlock(roots: List<AccessibilityNodeInfo>): Boolean {
    val icon = findById(roots, SYSTEM_UI, "lock_icon_view")
    // The icon may take the click action without being marked clickable, as on a double tap.
    val clicked =
      icon != null &&
        if (icon.actionList.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)) {
          icon.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
          click(icon)
        }
    LogUtils.e(TAG, "Lock screen needs no PIN: lock icon %s, clicked %s", icon != null, clicked)
    return clicked || swipeUp()
  }

  /** Swipes up across the lock screen, which shows its PIN pad. */
  private fun openPinPad(): Boolean = swipeUp()

  private fun swipeUp(): Boolean {
    val metrics = service.resources.displayMetrics
    val x = metrics.widthPixels / 2f
    val path =
      Path().apply {
        moveTo(x, metrics.heightPixels * 0.8f)
        lineTo(x, metrics.heightPixels * 0.3f)
      }
    val gesture =
      GestureDescription.Builder()
        .addStroke(GestureDescription.StrokeDescription(path, 0, SWIPE_MS))
        .build()
    // Where SandFly recognizes gestures itself, a plain dispatched swipe goes through its gesture
    // detection, which takes it as a SandFly gesture, and touch lock drops it; as SandFly's own
    // touch it goes to the lock screen as it is.
    val sent =
      if (SyntheticTouches.isAvailable()) {
        SyntheticTouches.dispatch(service, gesture, SWIPE_MS)
      } else {
        service.dispatchGesture(gesture, null, null)
      }
    LogUtils.e(TAG, "Lock screen swipe sent: %s, as SandFly's own touch: %s", sent, SyntheticTouches.isAvailable())
    return sent
  }

  private fun isInCall(): Boolean =
    service.getSystemService(AudioManager::class.java)?.mode == AudioManager.MODE_IN_CALL

  private fun isPhoneApp(packageName: String): Boolean {
    if (packageName in PHONE_APPS) {
      return true
    }
    val telecom = service.getSystemService(TelecomManager::class.java) ?: return false
    return try {
      telecom.defaultDialerPackage == packageName
    } catch (e: SecurityException) {
      false
    }
  }

  private fun click(node: AccessibilityNodeInfo): Boolean {
    var target: AccessibilityNodeInfo? = node
    var depth = 0
    while (target != null && !target.isClickable && depth < CLICKABLE_PARENT_DEPTH) {
      target = target.parent
      depth++
    }
    return target != null && target.isClickable && target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
  }

  /** Finds the button for [character] on [screen], by its ID, then by its text. */
  private fun findButton(
    roots: List<AccessibilityNodeInfo>,
    character: Char,
    screen: Screen,
  ): AccessibilityNodeInfo? {
    for (root in roots) {
      val packageName = root.packageName?.toString() ?: continue
      for (id in buttonIds(character, screen)) {
        findById(listOf(root), packageName, id)?.let {
          return it
        }
      }
    }
    return findByLabel(roots, character)
  }

  private fun findById(
    roots: List<AccessibilityNodeInfo>,
    packageName: String,
    id: String,
  ): AccessibilityNodeInfo? {
    for (root in roots) {
      root.findAccessibilityNodeInfosByViewId("$packageName:id/$id").firstOrNull { it.isVisibleToUser }?.let {
        return it
      }
    }
    return null
  }

  /** Finds a visible button labelled [character], such as "2" or "2 ABC", among a few hundred items. */
  private fun findByLabel(roots: List<AccessibilityNodeInfo>, character: Char): AccessibilityNodeInfo? {
    val queue = ArrayDeque<AccessibilityNodeInfo>(roots)
    var seen = 0
    while (queue.isNotEmpty() && seen < MAX_NODES) {
      val node = queue.removeFirst()
      seen++
      if (node.isVisibleToUser && !node.isEditable) {
        val label = node.text?.toString()?.takeIf { it.isNotBlank() } ?: node.contentDescription?.toString()
        if (label != null && isLabelFor(label, character)) {
          return node
        }
      }
      for (i in 0 until node.childCount) {
        node.getChild(i)?.let(queue::addLast)
      }
    }
    return null
  }

  private enum class Screen {
    LOCK_SCREEN,
    CALL,
    CALCULATOR,
  }

  companion object {
    private const val TAG = "KeypadKeys"
    private const val SYSTEM_UI = "com.android.systemui"

    /** How long to wait for the PIN pad to open: this many checks, this far apart. */
    private const val PENDING_CHECKS = 20
    private const val PENDING_CHECK_MS = 100L
    private const val MAX_PENDING_DIGITS = 16
    private const val SWIPE_MS = 150L
    private const val MAX_NODES = 400
    private const val CLICKABLE_PARENT_DEPTH = 3

    private const val CALL_CHARACTERS = "0123456789*#"
    private const val CALCULATOR_CHARACTERS = "0123456789+-*/.%()!="

    /** Phone apps whose call screens have a keypad, besides the default phone app. */
    private val PHONE_APPS =
      setOf(
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.incallui",
        "com.samsung.android.incallui",
        "com.samsung.android.dialer",
      )

    /** Calculator apps, besides any whose package name says calculator. */
    private val CALCULATORS = setOf("com.android.calculator2", "com.google.android.calculator")

    private val DIALPAD_IDS =
      listOf("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine")

    /** Labels of a keypad button for a digit: the digit, maybe with its letters, as "2 ABC". */
    private val DIGIT_LABEL = Regex("""^\d[\s,.]*[\p{L}+]{0,5}$""")

    /** Characters that calculators show for the operators typed. */
    private val OPERATOR_LABELS =
      mapOf(
        '*' to setOf("*", "×", "✕", "x"),
        '/' to setOf("/", "÷"),
        '-' to setOf("-", "−", "–"),
        '+' to setOf("+"),
        '.' to setOf(".", ","),
        '%' to setOf("%"),
        '(' to setOf("(", "( )", "()"),
        ')' to setOf(")", "( )", "()"),
        '!' to setOf("!"),
        '=' to setOf("="),
      )

    /** Whether the setting is on. */
    @JvmStatic
    fun isOn(context: Context): Boolean =
      SharedPreferencesUtils.getBooleanPref(
        SharedPreferencesUtils.getSharedPreferences(context),
        context.resources,
        R.string.pref_keypad_letter_keys_key,
        R.bool.pref_keypad_letter_keys_default,
      )

    /** Whether a keyboard built into the phone has letters that carry numbers. */
    @JvmStatic
    fun isAvailable(): Boolean =
      InputDevice.getDeviceIds().any { id ->
        val device = InputDevice.getDevice(id) ?: return@any false
        !device.isVirtual &&
          device.keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC &&
          (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || !device.isExternal) &&
          lettersCarryNumbers(device.keyCharacterMap)
      }

    private fun lettersCarryNumbers(map: KeyCharacterMap): Boolean =
      (KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z).any {
        keypadCharacter(map, it)?.isDigit() == true
      }

    /**
     * The number or symbol that the keyboard's [keyCode] carries: its number, as W is 1 on
     * BlackBerry keyboards, else its Alt character where that is a keypad character.
     */
    @JvmStatic
    fun keypadCharacter(map: KeyCharacterMap?, keyCode: Int): Char? {
      if (map == null) {
        return null
      }
      val number = map.getNumber(keyCode)
      if (number != 0.toChar() && isKeypadCharacter(number)) {
        return number
      }
      val alt = map.get(keyCode, KeyEvent.META_ALT_ON)
      if (alt > 0 && isKeypadCharacter(alt.toChar())) {
        return alt.toChar()
      }
      return null
    }

    internal fun isKeypadCharacter(character: Char): Boolean =
      character in CALCULATOR_CHARACTERS || character == '#'

    private fun isDigitKey(keyCode: Int): Boolean =
      keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 ||
        keyCode in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN

    /** The digit of a digit key, from the number row or the numeric keypad. */
    internal fun digitOf(keyCode: Int): Char? =
      when (keyCode) {
        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> '0' + (keyCode - KeyEvent.KEYCODE_0)
        in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 ->
          '0' + (keyCode - KeyEvent.KEYCODE_NUMPAD_0)
        else -> null
      }

    internal fun isCalculator(packageName: String): Boolean =
      packageName in CALCULATORS || packageName.lowercase().contains("calculator")

    /** Whether [label] is the label of the keypad button for [character]. */
    internal fun isLabelFor(label: String, character: Char): Boolean {
      val text = label.trim()
      if (character.isDigit()) {
        return text.firstOrNull() == character && DIGIT_LABEL.matches(text)
      }
      if (character == '#' || character == '*') {
        if (text == character.toString()) return true
      }
      return OPERATOR_LABELS[character]?.contains(text) == true
    }

    /** IDs of the button for [character] on [screen], in the app that shows it. */
    private fun buttonIds(character: Char, screen: Screen): List<String> =
      when (screen) {
        Screen.LOCK_SCREEN -> if (character.isDigit()) listOf("key$character") else emptyList()
        Screen.CALL ->
          when (character) {
            '*' -> listOf("star")
            '#' -> listOf("pound")
            else -> listOf(DIALPAD_IDS[character - '0'])
          }
        Screen.CALCULATOR ->
          when (character) {
            in '0'..'9' -> listOf("digit_$character")
            '+' -> listOf("op_add")
            '-' -> listOf("op_sub")
            '*' -> listOf("op_mul")
            '/' -> listOf("op_div")
            '.' -> listOf("dec_point")
            '%' -> listOf("op_pct")
            '!' -> listOf("op_fact")
            '(' -> listOf("lparen", "parens")
            ')' -> listOf("rparen", "parens")
            '=' -> listOf("eq")
            else -> emptyList()
          }
      }
  }
}
