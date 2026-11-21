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

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.core.content.edit
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.EmojiSearch
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.Locale

/**
 * The SandFly keyboard, an input method for physical keyboards with no keys on the screen. The app
 * types the keys as it does without an input method; the keyboard adds to that:
 * - It tells SandFly whether a text field takes what is typed, so that SandFly knows when the user
 *   is editing without asking the app.
 * - A printing key held down types its Alt character, as on BlackBerry keyboards, or its capital,
 *   instead of repeating.
 * - Emoji search and the symbol chooser, which SandFly's commands open: typed words list the
 *   characters whose names they start, the arrow keys or the trackpad move through them, Enter
 *   types the one spoken, and Back or Escape closes the list.
 */
class SandFlyKeyboard : InputMethodService(), SandFlyKeyboardHost.Keyboard {
  private val prefs by lazy { SharedPreferencesUtils.getSharedPreferences(this) }

  /** Emoji with their names in the user's language, read when the keyboard starts. */
  @Volatile private var emojiSearch: EmojiSearch? = null

  private var editing = false
  private var chooser: CharacterChooser? = null

  /** Keys whose press the keyboard took, so that their repeats and release are taken too. */
  private val takenKeys = mutableSetOf<Int>()

  /** The printing key held down, the character it typed, and whether its hold was handled. */
  private var heldKey = KeyEvent.KEYCODE_UNKNOWN
  private var heldTyped = 0
  private var heldDone = false

  override val isEditing: Boolean
    get() = editing

  override val isChoosing: Boolean
    get() = chooser != null

  override fun onCreate() {
    super.onCreate()
    SandFlyKeyboardHost.keyboard = this
    val context = applicationContext
    Thread(
        {
          emojiSearch =
            try {
              EmojiSearch.read(context, Locale.getDefault())
            } catch (e: Throwable) {
              LogUtils.e(TAG, "Can't read emoji: %s", e)
              null
            }
        },
        "SandFlyKeyboardEmoji",
      )
      .apply { isDaemon = true }
      .start()
  }

  override fun onDestroy() {
    if (SandFlyKeyboardHost.keyboard === this) {
      SandFlyKeyboardHost.keyboard = null
    }
    super.onDestroy()
  }

  /** The keys are on the physical keyboard. */
  override fun onEvaluateInputViewShown(): Boolean {
    super.onEvaluateInputViewShown()
    return false
  }

  override fun onEvaluateFullscreenMode(): Boolean = false

  override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
    super.onStartInput(attribute, restarting)
    editing = attribute.inputType and InputType.TYPE_MASK_CLASS != InputType.TYPE_NULL
    if (!restarting) {
      chooser = null
    }
    heldKey = KeyEvent.KEYCODE_UNKNOWN
  }

  override fun onFinishInput() {
    editing = false
    chooser = null
    heldKey = KeyEvent.KEYCODE_UNKNOWN
    takenKeys.clear()
    super.onFinishInput()
  }

  override fun openChooser(kind: CharacterChooser.Kind): Boolean {
    if (!editing || currentInputConnection == null) {
      return false
    }
    val opened =
      when (kind) {
        CharacterChooser.Kind.EMOJI ->
          CharacterChooser(kind, emptyList(), recent(kind)) { query ->
            emojiSearch?.find(query, MAX_EMOJI)?.map { CharacterChooser.Item(it.emoji(), it.name()) }
              ?: emptyList()
          }
        CharacterChooser.Kind.SYMBOL -> CharacterChooser(kind, CharacterChooser.symbols(), recent(kind))
      }
    chooser = opened
    val title =
      getString(
        if (kind == CharacterChooser.Kind.EMOJI) R.string.sandfly_keyboard_emoji_search
        else R.string.sandfly_keyboard_symbols
      )
    // Emoji search starts with the recent emoji, if any.
    SandFlyKeyboardHost.speak(if (opened.current == null) title else title + ", " + describe(opened))
    return true
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    if (keyCode in takenKeys) {
      return true
    }
    val open = chooser
    if (open != null) {
      val taken = onChooserKey(open, keyCode, event)
      if (taken) takenKeys += keyCode
      return taken || super.onKeyDown(keyCode, event)
    }
    // Alt + Space on the Zinwa Q25 types this private character for the symbol list.
    if (event.unicodeChar == Q25_SYMBOL_CHARACTER) {
      takenKeys += keyCode
      openChooser(CharacterChooser.Kind.SYMBOL)
      return true
    }
    if (onHeldKey(keyCode, event)) {
      return true
    }
    return super.onKeyDown(keyCode, event)
  }

  override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
    if (takenKeys.remove(keyCode)) {
      return true
    }
    if (keyCode == heldKey) {
      heldKey = KeyEvent.KEYCODE_UNKNOWN
    }
    return super.onKeyUp(keyCode, event)
  }

  /** Handles a key while [open] is open. Returns false for keys it leaves, such as Shift. */
  private fun onChooserKey(open: CharacterChooser, keyCode: Int, event: KeyEvent): Boolean {
    when (keyCode) {
      KeyEvent.KEYCODE_BACK,
      KeyEvent.KEYCODE_ESCAPE -> {
        chooser = null
        SandFlyKeyboardHost.speak(getString(R.string.sandfly_keyboard_chooser_closed))
      }
      KeyEvent.KEYCODE_ENTER,
      KeyEvent.KEYCODE_NUMPAD_ENTER,
      KeyEvent.KEYCODE_DPAD_CENTER -> choose(open)
      KeyEvent.KEYCODE_DPAD_DOWN,
      KeyEvent.KEYCODE_DPAD_RIGHT -> {
        open.next()
        SandFlyKeyboardHost.speak(describe(open))
      }
      KeyEvent.KEYCODE_DPAD_UP,
      KeyEvent.KEYCODE_DPAD_LEFT -> {
        open.previous()
        SandFlyKeyboardHost.speak(describe(open))
      }
      KeyEvent.KEYCODE_TAB -> {
        if (event.isShiftPressed) open.previous() else open.next()
        SandFlyKeyboardHost.speak(describe(open))
      }
      KeyEvent.KEYCODE_DEL -> {
        if (open.deleteLast()) {
          SandFlyKeyboardHost.speak(describe(open))
        } else {
          chooser = null
          SandFlyKeyboardHost.speak(getString(R.string.sandfly_keyboard_chooser_closed))
        }
      }
      else -> {
        val character = event.unicodeChar
        if (character <= 0 || Character.isISOControl(character) || character > Char.MAX_VALUE.code) {
          return false
        }
        open.type(character.toChar())
        SandFlyKeyboardHost.speak(describe(open))
      }
    }
    return true
  }

  /** Types the character chosen in [open] and closes it. */
  private fun choose(open: CharacterChooser) {
    val item = open.current
    chooser = null
    if (item == null) {
      SandFlyKeyboardHost.speak(getString(R.string.sandfly_keyboard_chooser_closed))
      return
    }
    currentInputConnection?.commitText(item.text, 1)
    remember(open.kind, item)
  }

  /** What is chosen now in [open], with its place among the matches. */
  private fun describe(open: CharacterChooser): String {
    val item = open.current ?: return getString(R.string.sandfly_keyboard_no_matches)
    return getString(R.string.sandfly_keyboard_chooser_item, item.name, open.index + 1, open.matches.size)
  }

  /**
   * Follows a printing key held down: the app types it when it goes down; once Android repeats it,
   * the keyboard puts the key's Alt character or capital in its place, and takes the repeats.
   */
  private fun onHeldKey(keyCode: Int, event: KeyEvent): Boolean {
    val mode = prefs.getString(getString(R.string.pref_sandfly_keyboard_held_keys_key), HELD_ALT)
    if (mode == HELD_REPEAT) {
      return false
    }
    if (event.repeatCount == 0) {
      heldKey = if (isHoldable(event)) keyCode else KeyEvent.KEYCODE_UNKNOWN
      heldTyped = event.unicodeChar
      heldDone = false
      return false
    }
    if (keyCode != heldKey) {
      return false
    }
    if (!heldDone) {
      heldDone = true
      val typed = heldTyped.toChar()
      val capital = typed.uppercaseChar().takeIf { it != typed }
      val replacement = if (mode == HELD_CAPITAL) capital else altCharacter(event, typed) ?: capital
      if (replacement != null) {
        replaceTyped(typed, replacement)
      }
    }
    return true
  }

  /** Replaces [typed], just before the cursor, with [replacement] in one edit. */
  private fun replaceTyped(typed: Char, replacement: Char) {
    val connection = currentInputConnection ?: return
    val before = connection.getTextBeforeCursor(1, 0)
    if (before == null || before.length != 1 || before[0] != typed) {
      return
    }
    connection.beginBatchEdit()
    connection.deleteSurroundingText(1, 0)
    connection.commitText(replacement.toString(), 1)
    connection.endBatchEdit()
  }

  /** The characters chosen last, each saved as a line of the character, a tab and its name. */
  private fun recent(kind: CharacterChooser.Kind): List<CharacterChooser.Item> =
    prefs.getString(recentKey(kind), null)?.split('\n').orEmpty().mapNotNull { line ->
      val tab = line.indexOf('\t')
      if (tab <= 0) null else CharacterChooser.Item(line.substring(0, tab), line.substring(tab + 1))
    }

  private fun remember(kind: CharacterChooser.Kind, item: CharacterChooser.Item) {
    val list = (listOf(item) + recent(kind).filter { it.text != item.text }).take(MAX_RECENT)
    prefs.edit { putString(recentKey(kind), list.joinToString("\n") { it.text + "\t" + it.name }) }
  }

  private fun recentKey(kind: CharacterChooser.Kind) = PREF_RECENT + kind.name.lowercase()

  private companion object {
    const val TAG = "SandFlyKeyboard"
    const val MAX_EMOJI = 200
    const val PREF_RECENT = "pref_sandfly_keyboard_recent_"
    const val MAX_RECENT = 20

    /** The values of the held keys setting. */
    const val HELD_ALT = "alt"
    const val HELD_CAPITAL = "capital"
    const val HELD_REPEAT = "repeat"

    /** What Alt + Space types on the Zinwa Q25, for its keyboard's symbol list. */
    const val Q25_SYMBOL_CHARACTER = 0xEF01

    private const val TYPING_MODIFIERS =
      KeyEvent.META_CTRL_MASK or
        KeyEvent.META_ALT_MASK or
        KeyEvent.META_META_MASK or
        KeyEvent.META_SYM_ON or
        KeyEvent.META_FUNCTION_ON

    /** A printing key other than Space, pressed alone or with Shift. */
    fun isHoldable(event: KeyEvent): Boolean =
      event.isPrintingKey &&
        event.keyCode != KeyEvent.KEYCODE_SPACE &&
        event.unicodeChar > 0 &&
        event.metaState and TYPING_MODIFIERS == 0

    /** The key's Alt character, where it has one other than [typed]. */
    fun altCharacter(event: KeyEvent, typed: Char): Char? {
      val alt = event.keyCharacterMap.get(event.keyCode, KeyEvent.META_ALT_ON)
      if (alt <= 0 || alt > Char.MAX_VALUE.code || Character.isISOControl(alt)) return null
      val character = alt.toChar()
      // Private characters, such as the Q25's for its symbol list, aren't typed.
      if (character in ''..'' || character == typed) return null
      return character
    }
  }
}
