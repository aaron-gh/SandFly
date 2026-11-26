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
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import androidx.core.content.edit
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.EmojiSearch
import com.google.android.accessibility.utils.output.SpeechController
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
 *   types the one spoken, and Back or Escape closes the list. Closing it clears the text field's
 *   meta key states of the modifiers held with it ([ChooserMetaKeys]).
 * - Editing mode, which a SandFly command turns on: single keys move the cursor and select, copy,
 *   cut and paste ([EditingMode]).
 * - Double presses ([DoublePressCharacters]): a key pressed twice puts its double-press characters
 *   in place of the character the first press typed, and Space pressed twice types a period.
 * - Shift pressed and released alone types the next letter as a capital, as on BlackBerry
 *   keyboards; pressed again, it doesn't.
 * - Auto-correct and word suggestions, each off until turned on ([WordInput]).
 * - The characters of password fields, which Android gives accessibility services only as bullets
 *   but gives the input method through its input connection: with Speak password characters on,
 *   it says a character typed, deleted or moved over, and Spell the password field spells the
 *   field ([PasswordSpeech]). Only lengths are logged.
 */
class SandFlyKeyboard : InputMethodService(), SandFlyKeyboardHost.Keyboard {
  private val prefs by lazy { SharedPreferencesUtils.getSharedPreferences(this) }

  /** Emoji with their names in the user's language, read when the keyboard starts. */
  @Volatile private var emojiSearch: EmojiSearch? = null

  private var editing = false
  private var chooser: CharacterChooser? = null

  /** The modifier keys of the open chooser, whose meta key states are cleared when it closes. */
  private val chooserMetaKeys = ChooserMetaKeys()
  private val editingMode by lazy { EditingMode(this) { password } }

  /** Auto-correct and word suggestions. */
  private val words by lazy { WordInput(this, prefs) { currentInputConnection } }

  /** Follows the password field being edited for Speak password characters. */
  private val passwordEcho by lazy { PasswordSpeech.Echo(this) }

  /** Keys whose press the keyboard took, so that their repeats and release are taken too. */
  private val takenKeys = mutableSetOf<Int>()

  /** The printing key held down, the character it typed, and whether its hold was handled. */
  private var heldKey = KeyEvent.KEYCODE_UNKNOWN
  private var heldTyped = 0
  private var heldDone = false

  /** The last press that a second press of its key can replace, with the character it typed. */
  private var lastPress: Press? = null

  private class Press(val keyCode: Int, val typed: Char, val time: Long)

  /** Whether Shift is down with no other key pressed since it went down. */
  private var shiftAlone = false

  /** Whether Shift was pressed and released alone, so that the next printing key types with Shift. */
  private var shiftNext = false

  /** Whether the field being edited is a password field, where double presses type as single ones. */
  private var password = false

  /** The double-press characters, read again when the setting changes. */
  private var doublePressText: String? = null
  private var doublePressEntries: Map<Char, String> = emptyMap()

  override val isEditing: Boolean
    get() = editing

  override val isChoosing: Boolean
    get() = chooser != null

  override val isEditingModeOn: Boolean
    get() = editingMode.isOn

  override val speaksPasswordCharacters: Boolean
    get() = editing && password && passwordEcho.isFollowing && PasswordSpeech.isOn(this, prefs)

  override fun spellPasswordField(): Boolean {
    val connection = currentInputConnection
    if (!editing || connection == null) {
      return false
    }
    if (!password) {
      LogUtils.w(TAG, "Spell the password field: not a password field")
      SandFlyKeyboardHost.speak(getString(R.string.sandfly_keyboard_not_password_field))
      return true
    }
    val field = readField(connection, monitor = false)
    LogUtils.w(
      TAG,
      "Spell the password field: %s",
      if (field == null) "text not available" else "${field.text.length} characters",
    )
    when {
      field == null ->
        SandFlyKeyboardHost.speak(getString(R.string.sandfly_keyboard_password_not_available))
      field.text.isEmpty() -> SandFlyKeyboardHost.speak(getString(R.string.text_field_empty))
      else ->
        SandFlyKeyboardHost.speakPassword(
          PasswordSpeech.spell(this, field.text),
          queueMode = SpeechController.QUEUE_MODE_INTERRUPT_AND_UNINTERRUPTIBLE_BY_NEW_SPEECH,
        )
    }
    return true
  }

  /** Whether the keyboard types, with no chooser or editing mode taking the keys. */
  private val typing: Boolean
    get() = editing && chooser == null && !editingMode.isOn

  override fun takesKey(keyCode: Int): Boolean = typing && words.takesKey(keyCode)

  override val hasWordSuggestions: Boolean
    get() = typing && words.hasWord()

  override fun stepSuggestion(next: Boolean): String? =
    words.step(next, currentInputConnection, typing)

  override fun typeSuggestion(): Boolean = words.typeSuggestion(currentInputConnection, typing)

  override fun toggleEditingMode(): Boolean {
    if (editingMode.isOn) {
      editingMode.turnOff(announce = true)
      return true
    }
    if (!editing || currentInputConnection == null) {
      return false
    }
    if (chooser != null) closeChooser("editing mode")
    lastPress = null
    shiftNext = false
    editingMode.turnOn()
    return true
  }

  /** A key SandFly took, such as Shift + Sym, wasn't Shift pressed alone, and ends a Shift press. */
  override fun onKeyTaken() {
    shiftAlone = false
    shiftNext = false
  }

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
    password = isPassword(attribute.inputType)
    lastPress = null
    shiftAlone = false
    shiftNext = false
    if (!restarting) {
      if (chooser != null) closeChooser("input started in another field")
      editingMode.turnOff(announce = false)
    }
    heldKey = KeyEvent.KEYCODE_UNKNOWN
    passwordEcho.stop()
    followPasswordField()
    words.onStartInput(attribute, restarting)
  }

  override fun onFinishInput() {
    editing = false
    words.onFinishInput()
    passwordEcho.stop()
    if (chooser != null) closeChooser("input finished")
    editingMode.turnOff(announce = false)
    heldKey = KeyEvent.KEYCODE_UNKNOWN
    lastPress = null
    shiftAlone = false
    shiftNext = false
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
    chooserMetaKeys.onOpen()
    lastPress = null
    shiftNext = false
    editingMode.turnOff(announce = false)
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
    chooserMetaKeys.onKeyEvent(event.metaState, choosing = chooser != null)
    if (keyCode in takenKeys) {
      return true
    }
    val press = lastPress
    // Another key, or the same key held down, ends the double press. A modifier key held down
    // since before the press repeats and is ignored.
    if (press != null && (keyCode != press.keyCode || event.repeatCount > 0)) {
      if (keyCode == press.keyCode || event.repeatCount == 0 || !KeyEvent.isModifierKey(keyCode)) {
        lastPress = null
      }
    }
    val open = chooser
    if (open != null) {
      val taken = onChooserKey(open, keyCode, event)
      if (taken) takenKeys += keyCode
      return taken || super.onKeyDown(keyCode, event)
    }
    if (editingMode.isOn) {
      val taken = editingMode.onKeyDown(keyCode, event, currentInputConnection)
      if (taken) takenKeys += keyCode
      return taken || super.onKeyDown(keyCode, event)
    }
    followShiftKey(keyCode, event)
    if (words.onKeyDown(keyCode, event, currentInputConnection)) {
      takenKeys += keyCode
      val typed = words.typedCharacter
      lastPress = null
      if (
        typed != null &&
          !password &&
          DoublePressCharacters.applies(typed, doublePressEntries(), doubleSpacePeriod())
      ) {
        lastPress = Press(keyCode, typed, event.eventTime)
      }
      return true
    }
    if (onShiftedKey(keyCode, event)) {
      takenKeys += keyCode
      return true
    }
    // Alt + Space on the Zinwa Q25 types this private character for the symbol list.
    if (event.unicodeChar == Q25_SYMBOL_CHARACTER) {
      takenKeys += keyCode
      openChooser(CharacterChooser.Kind.SYMBOL)
      return true
    }
    if (onDoublePress(keyCode, event)) {
      takenKeys += keyCode
      return true
    }
    if (onHeldKey(keyCode, event)) {
      return true
    }
    return super.onKeyDown(keyCode, event)
  }

  /**
   * Follows Shift pressed alone. Shift itself still goes to the app, which needs it for Shift +
   * arrow selection. Only on keyboards whose modifiers work while held ([isChorded]); on others
   * Android already applies a Shift pressed and released to the next key.
   */
  private fun followShiftKey(keyCode: Int, event: KeyEvent) {
    if (!isShiftKey(keyCode)) {
      if (event.repeatCount == 0) shiftAlone = false
      return
    }
    if (event.repeatCount == 0) {
      // Not with a modifier held down, such as Sym, whose shortcuts SandFly takes.
      shiftAlone = editing && isChorded(event) && event.metaState and TYPING_MODIFIERS == 0
    }
  }

  /**
   * Types the printing key [event] presses with Shift, after Shift was pressed and released alone.
   * Any other key ends that. Returns whether it took [event].
   */
  private fun onShiftedKey(keyCode: Int, event: KeyEvent): Boolean {
    if (!shiftNext || event.repeatCount > 0 || KeyEvent.isModifierKey(keyCode)) {
      return false
    }
    shiftNext = false
    if (
      !event.isPrintingKey ||
        keyCode == KeyEvent.KEYCODE_SPACE ||
        event.metaState and (TYPING_MODIFIERS or KeyEvent.META_SHIFT_MASK) != 0
    ) {
      return false
    }
    val connection = currentInputConnection ?: return false
    val shifted = event.keyCharacterMap.get(keyCode, event.metaState or KeyEvent.META_SHIFT_ON)
    if (shifted <= 0 || shifted > Char.MAX_VALUE.code || Character.isISOControl(shifted)) {
      return false
    }
    val typed = shifted.toChar()
    connection.commitText(typed.toString(), 1)
    lastPress = null
    if (
      !password &&
        DoublePressCharacters.applies(typed, doublePressEntries(), doubleSpacePeriod())
    ) {
      lastPress = Press(keyCode, typed, event.eventTime)
    }
    return true
  }

  override fun onUpdateSelection(
    oldSelStart: Int,
    oldSelEnd: Int,
    newSelStart: Int,
    newSelEnd: Int,
    candidatesStart: Int,
    candidatesEnd: Int,
  ) {
    super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
    if (password) followPasswordField()
    val ownEdit = words.onUpdateSelection(newSelStart, newSelEnd, candidatesStart)
    // Typing a character moves the cursor one place on; any other move ends the double press,
    // except the keyboard's own edit that typed it, such as a correction with its space.
    if (
      lastPress != null &&
        !ownEdit &&
        !(oldSelStart == oldSelEnd && newSelStart == newSelEnd && newSelStart == oldSelStart + 1)
    ) {
      lastPress = null
    }
  }

  /** A text change that leaves the selection where it was, such as Delete, comes only here. */
  override fun onUpdateExtractedText(token: Int, text: ExtractedText?) {
    super.onUpdateExtractedText(token, text)
    if (token == PASSWORD_TOKEN && password) followPasswordField()
  }

  /**
   * Reads the password field being edited again, with Speak password characters on, and says the
   * character one key typed, deleted, replaced or moved over since the last reading. The first
   * reading asks the field to report every text change, so that Delete, which doesn't move the
   * cursor, is followed too.
   */
  private fun followPasswordField() {
    val on = editing && password && PasswordSpeech.isOn(this, prefs)
    val connection = currentInputConnection
    val starting = !passwordEcho.isFollowing
    val field = if (on && connection != null) readField(connection, monitor = starting) else null
    val said =
      passwordEcho.follow(field, on, password, sayCapital = PasswordSpeech.saysCapital(this, prefs))
    if (on && starting) {
      LogUtils.w(
        TAG,
        "Speak password characters: %s",
        if (field == null) "the field's text is not available" else "following a password field",
      )
    }
    if (said != null) {
      SandFlyKeyboardHost.speakPassword(said.text, said.pitch)
    }
  }

  /**
   * The text and selection of the field of [connection], read together where the field gives its
   * extracted text, or null where it gives no text. With [monitor], the field reports its later
   * changes to [onUpdateExtractedText].
   */
  private fun readField(connection: InputConnection, monitor: Boolean): PasswordSpeech.Field? {
    val request = ExtractedTextRequest().apply { token = PASSWORD_TOKEN }
    val extracted =
      connection.getExtractedText(
        request,
        if (monitor) InputConnection.GET_EXTRACTED_TEXT_MONITOR else 0,
      )
    val text = extracted?.text
    if (text != null) {
      val start = minOf(extracted.selectionStart, extracted.selectionEnd)
      val end = maxOf(extracted.selectionStart, extracted.selectionEnd)
      if (start >= 0 && end <= text.length) {
        return PasswordSpeech.Field(text.toString(), start, end)
      }
    }
    val before = connection.getTextBeforeCursor(MAX_PASSWORD, 0) ?: return null
    val after = connection.getTextAfterCursor(MAX_PASSWORD, 0) ?: return null
    val selected = connection.getSelectedText(0) ?: ""
    return PasswordSpeech.Field(
      "$before$selected$after",
      before.length,
      before.length + selected.length,
    )
  }

  /**
   * Follows double presses: the app types the first press; a second press of the same key soon
   * after it, with nothing else between, is taken, and the keyboard puts the key's double-press
   * characters in place of the character the first press typed. Returns whether it took [event].
   */
  private fun onDoublePress(keyCode: Int, event: KeyEvent): Boolean {
    if (event.repeatCount > 0) {
      return false
    }
    val last = lastPress
    lastPress = null
    if (password || !isDoublePressable(event)) {
      return false
    }
    val typed = event.unicodeChar.toChar()
    val spacePeriod = doubleSpacePeriod()
    val entries = doublePressEntries()
    if (
      last != null &&
        last.keyCode == keyCode &&
        event.eventTime - last.time < DoublePressCharacters.TWICE_MS
    ) {
      val connection = currentInputConnection
      if (connection != null) {
        val replacement =
          DoublePressCharacters.replacement(
            last.typed,
            connection.getTextBeforeCursor(2, 0),
            entries,
            spacePeriod,
          )
        if (replacement != null) {
          connection.beginBatchEdit()
          connection.deleteSurroundingText(1, 0)
          connection.commitText(replacement, 1)
          connection.endBatchEdit()
          return true
        }
      }
    }
    if (DoublePressCharacters.applies(typed, entries, spacePeriod)) {
      lastPress = Press(keyCode, typed, event.eventTime)
    }
    return false
  }

  private fun doubleSpacePeriod(): Boolean =
    prefs.getBoolean(
      getString(R.string.pref_sandfly_keyboard_double_space_period_key),
      resources.getBoolean(R.bool.pref_sandfly_keyboard_double_space_period_default),
    )

  private fun doublePressEntries(): Map<Char, String> {
    val text = prefs.getString(DoublePressCharacters.KEY, null)
    if (text != doublePressText) {
      doublePressText = text
      doublePressEntries =
        DoublePressCharacters.parse(text).associate { it.key to it.characters }
    }
    return doublePressEntries
  }

  override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
    chooserMetaKeys.onKeyEvent(event.metaState, choosing = chooser != null)
    if (isShiftKey(keyCode)) {
      // Pressed alone, Shift applies to the next key; pressed alone again, it no longer does.
      if (shiftAlone && event.metaState and TYPING_MODIFIERS == 0) {
        shiftNext = !shiftNext
      }
      shiftAlone = false
    }
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
        closeChooser(KeyEvent.keyCodeToString(keyCode))
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
          closeChooser(KeyEvent.keyCodeToString(keyCode))
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
    closeChooser(if (item == null) "Enter with no match" else "Enter")
    if (item == null) {
      SandFlyKeyboardHost.speak(getString(R.string.sandfly_keyboard_chooser_closed))
      return
    }
    currentInputConnection?.commitText(item.text, 1)
    remember(open.kind, item)
  }

  /**
   * Closes the chooser, and clears in the text field the meta key states of the modifiers held when
   * it opened or pressed while it was open ([ChooserMetaKeys]), so that the next key typed doesn't
   * take them, such as Alt of Alt + Space.
   */
  private fun closeChooser(how: String) {
    chooser = null
    val states = chooserMetaKeys.onClose()
    val connection = currentInputConnection
    val result =
      when {
        states == 0 -> "none to clear"
        connection == null -> "not cleared, no text field"
        connection.clearMetaKeyStates(states) -> "cleared"
        else -> "not cleared, the text field refused"
      }
    LogUtils.w(
      TAG,
      "Chooser closed by %s; meta key states of its modifiers (%s): %s",
      how,
      ChooserMetaKeys.describe(states),
      result,
    )
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
    words.onOwnTyping()
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

    /** The token of the extracted text requests that follow a password field. */
    const val PASSWORD_TOKEN = 0x5046

    /** The most characters read each side of the cursor where a field gives no extracted text. */
    const val MAX_PASSWORD = 1000

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

    /** Whether the keyboard's modifiers apply only while held, with no Shift for the next key. */
    fun isChorded(event: KeyEvent): Boolean =
      event.keyCharacterMap.modifierBehavior == KeyCharacterMap.MODIFIER_BEHAVIOR_CHORDED

    fun isShiftKey(keyCode: Int): Boolean =
      keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT

    /** A printing key other than Space, pressed alone or with Shift. */
    fun isHoldable(event: KeyEvent): Boolean =
      event.isPrintingKey &&
        event.keyCode != KeyEvent.KEYCODE_SPACE &&
        event.unicodeChar > 0 &&
        event.metaState and TYPING_MODIFIERS == 0

    /** A printing key, Space too, pressed alone or with Shift, that types a character. */
    fun isDoublePressable(event: KeyEvent): Boolean =
      event.isPrintingKey &&
        event.unicodeChar > 0 &&
        event.unicodeChar <= Char.MAX_VALUE.code &&
        !Character.isISOControl(event.unicodeChar) &&
        event.metaState and TYPING_MODIFIERS == 0

    fun isPassword(inputType: Int): Boolean {
      val variation = inputType and InputType.TYPE_MASK_VARIATION
      return when (inputType and InputType.TYPE_MASK_CLASS) {
        InputType.TYPE_CLASS_TEXT ->
          variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        else -> false
      }
    }

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
