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
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.keyboard.words.KeyLayout
import com.google.android.accessibility.talkback.keyboard.words.WordLanguage
import com.google.android.accessibility.talkback.keyboard.words.WordList
import com.google.android.accessibility.talkback.keyboard.words.WordSuggester
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.Locale

/**
 * Auto-correct and word suggestions of the SandFly keyboard. The app types the letters, so the
 * word being typed is read from the text field: the letters just before the cursor, while the
 * cursor has moved only by typing since.
 * - Word suggestions: while a word is being typed, Up and Down, as the Zinwa Q25's trackpad sends,
 *   go through corrections and completions of it, and so do SandFly's Next and Previous word
 *   suggestion commands and the Word suggestions reading control. The one spoken is typed with a
 *   space by Enter or a trackpad click, or with the character typed by Space or punctuation. Back
 *   or Escape leave the list, and any other key leaves it and does what it does. Without a word
 *   being typed, the keys go to the app.
 * - Auto-correct: Space, punctuation or Enter after a misspelled word put its correction in its
 *   place. Delete right after a correction undoes it, and the word is never corrected again.
 * - Speak suggestion: right after the keyboard echo of each key typed in a word, or when typing
 *   pauses within it, the keyboard says, at a lower pitch, the correction that Space would put in
 *   its place ("receive, correction"), or else the first word suggestion. The next key stops it.
 *
 * Neither is used in password, email, web address and number fields, nor in fields that ask for no
 * suggestions. Words are learned, for ranking and undone corrections, only where the field allows
 * personalized learning.
 */
internal class WordInput(
  private val context: Context,
  private val prefs: SharedPreferences,
  /** The input connection of the text field being edited, for the pause announcement. */
  private val connection: () -> InputConnection?,
) {
  /**
   * Says the correction or suggestion: after the keyboard echo of the last key with [immediately],
   * else at once, after a pause. Replaced in tests.
   */
  var announce: (text: String, immediately: Boolean) -> Unit = { text, immediately ->
    if (immediately) {
      SandFlyKeyboardHost.speakSuggestionAfterEcho(text, lastTypingTime)
    } else {
      SandFlyKeyboardHost.speakSuggestion(text)
    }
  }

  /** Whether the announcement waiting to run is said right after the echo, not after a pause. */
  private var announceImmediately = false

  /**
   * The correction or suggestion said for the word typed, which Enter types and the list starts
   * with, while the cursor stays at [Offer.cursor] and no other key is pressed.
   */
  private var offered: Offer? = null

  private class Offer(val word: String, val typed: String, val cursor: Int)

  /** The last move through the list, for the second move a trackpad swipe sends. */
  private var lastMoveKey = KeyEvent.KEYCODE_UNKNOWN
  private var lastMoveTime = 0L

  private val handler = Handler(Looper.getMainLooper())
  private val pauseAnnouncement = Runnable { announcePause() }

  /** Whether the field being edited takes suggestions, and whether its words may be learned. */
  private var fieldTakesWords = false
  private var learning = false

  private var selStart = -1
  private var selEnd = -1

  /** Typing keys that went to the app since the last selection update, each moving the cursor. */
  private var pendingTyping = 0
  private var lastTypingTime = 0L

  /** Whether the cursor has moved only by typing since a word was typed. */
  private var wordActive = false

  /** The open list of suggestions, with the word they are for. */
  private var list: Suggestions? = null

  private class Suggestions(val typed: Word, val words: List<String>, var index: Int) {
    val current: String
      get() = words[index]
  }

  /** The word before the cursor: its text, where it starts, and whether it starts a sentence. */
  private class Word(val text: String, val start: Int, val sentenceStart: Boolean)

  /** The last correction, which Delete undoes while the cursor is at its end. */
  private var correction: Correction? = null

  private class Correction(val original: String, val start: Int, val typed: String) {
    val end: Int
      get() = start + typed.length
  }

  /** Where the cursor goes after the keyboard's own edit, and until when it is waited for. */
  private var ownEditEnd = -1
  private var ownEditUntil = 0L

  /** Whether the cursor is at the end of a word after the keyboard's own edit. */
  private var ownEditKeepsWord = false

  /** The character the last key taken typed, for double presses, or null. */
  var typedCharacter: Char? = null
    private set

  private var layoutDevice = Int.MIN_VALUE

  private fun autoCorrectOn(): Boolean =
    prefs.getBoolean(
      context.getString(R.string.pref_sandfly_keyboard_autocorrect_key),
      context.resources.getBoolean(R.bool.pref_sandfly_keyboard_autocorrect_default),
    )

  fun suggestionsOn(): Boolean =
    prefs.getBoolean(
      context.getString(R.string.pref_sandfly_keyboard_suggestions_key),
      context.resources.getBoolean(R.bool.pref_sandfly_keyboard_suggestions_default),
    )

  /** The language of the word list: the setting's, else the system's, or null if none has one. */
  private fun language(): WordLanguage? {
    val value =
      prefs.getString(context.getString(R.string.pref_sandfly_keyboard_word_language_key), null)
    return WordLanguage.fromCode(value) ?: WordLanguage.forLocale(Locale.getDefault())
  }

  /** The suggester, starting to read its list if needed, or null without one. */
  private fun suggester(): WordSuggester? {
    if (!fieldTakesWords || !(autoCorrectOn() || suggestionsOn())) return null
    val language = language() ?: return null
    return WordData.suggester(context, language)
  }

  fun onStartInput(info: EditorInfo, restarting: Boolean) {
    handler.removeCallbacks(pauseAnnouncement)
    offered = null
    fieldTakesWords = takesWords(info.inputType)
    learning = info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING == 0
    selStart = info.initialSelStart
    selEnd = info.initialSelEnd
    pendingTyping = 0
    wordActive = false
    ownEditEnd = -1
    if (!restarting) {
      list = null
      correction = null
    }
    // Starts reading the word list, so that it is there for the first word.
    suggester()
  }

  fun onFinishInput() {
    handler.removeCallbacks(pauseAnnouncement)
    offered = null
    SandFlyKeyboardHost.stopSuggestion()
    list = null
    correction = null
    wordActive = false
    pendingTyping = 0
    ownEditEnd = -1
    fieldTakesWords = false
    val language = language() ?: return
    WordData.loaded(language)?.let { WordData.save(context, it) }
  }

  /**
   * Follows a selection update. Returns whether it came from the keyboard's own edit, which a double
   * press after it may still replace.
   */
  fun onUpdateSelection(newStart: Int, newEnd: Int, candidatesStart: Int): Boolean {
    val unchanged = newStart == selStart && newEnd == selEnd
    selStart = newStart
    selEnd = newEnd
    val now = SystemClock.uptimeMillis()
    // Updates that leave the cursor where it was, as some fields send, change nothing here.
    if (unchanged && ownEditEnd < 0) return false
    if (ownEditEnd >= 0 && now <= ownEditUntil) {
      // The composing region set for the edit comes first, with the cursor where it was.
      if (newStart != ownEditEnd || newEnd != ownEditEnd || candidatesStart >= 0) {
        return true
      }
      ownEditEnd = -1
      wordActive = ownEditKeepsWord
      return true
    }
    ownEditEnd = -1
    offered = null
    if (pendingTyping > 0 && now - lastTypingTime <= TYPING_UPDATE_MS) {
      pendingTyping--
      wordActive = newStart == newEnd
      if (wordActive && pendingTyping == 0) schedulePause()
    } else {
      pendingTyping = 0
      wordActive = false
      list = null
    }
    return false
  }

  /** Whether SandFly should leave [keyCode] to the keyboard: the keys of the list. */
  fun takesKey(keyCode: Int): Boolean {
    if (list != null) return keyCode in LIST_KEYS
    if (keyCode in ENTER_KEYS) return validOffer() != null && enterTypesOffer()
    return (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) &&
      wordActive &&
      fieldTakesWords &&
      suggestionsOn()
  }

  /** The suggestion said for the word typed, while the cursor hasn't moved since. */
  private fun validOffer(): Offer? =
    offered?.takeIf { wordActive && fieldTakesWords && selStart == selEnd && it.cursor == selStart }

  private fun enterTypesOffer(): Boolean =
    prefs.getBoolean(
      context.getString(R.string.pref_sandfly_keyboard_enter_types_suggestion_key),
      context.resources.getBoolean(R.bool.pref_sandfly_keyboard_enter_types_suggestion_default),
    )

  /** Whether a word is being typed with word suggestions on, for the reading control. */
  fun hasWord(): Boolean = wordActive && fieldTakesWords && suggestionsOn()

  /**
   * Handles a key down that the keyboard's chooser and editing mode left. Returns whether it was
   * taken; [typedCharacter] is then the character it typed, if any.
   */
  fun onKeyDown(keyCode: Int, event: KeyEvent, connection: InputConnection?): Boolean {
    typedCharacter = null
    // A key before the pause ends, Shift too, means typing goes on; the next key stops the
    // suggestion of the last.
    handler.removeCallbacks(pauseAnnouncement)
    SandFlyKeyboardHost.stopSuggestion()
    if (connection == null || KeyEvent.isModifierKey(keyCode)) return false
    // Said for the word as it is, before this key.
    val offer = validOffer()
    offered = null
    val suggester = suggester()
    if (suggester != null) {
      useLayout(suggester, event)
      if (event.repeatCount == 0 && onWordKey(keyCode, event, connection, suggester, offer)) {
        return true
      }
    } else {
      list = null
      correction = null
    }
    followTyping(keyCode, event)
    return false
  }

  private fun onWordKey(
    keyCode: Int,
    event: KeyEvent,
    connection: InputConnection,
    suggester: WordSuggester,
    offer: Offer?,
  ): Boolean {
    val made = correction
    correction = null
    if (made != null && keyCode == KeyEvent.KEYCODE_DEL && noModifiers(event)) {
      if (selStart == made.end && selEnd == made.end) {
        undo(made, connection, suggester)
        return true
      }
    }
    val open = list
    if (open != null) {
      if (onListKey(open, keyCode, event, connection, suggester)) return true
      list = null
    }
    // Enter, or the click of the Zinwa Q25's trackpad, which sends Enter, types the suggestion
    // said.
    if (offer != null && keyCode in ENTER_KEYS && noModifiers(event) && enterTypesOffer()) {
      val word = currentWord(connection)
      if (word != null && word.text == offer.typed) {
        type(Suggestions(word, listOf(offer.word), 0), ' ', connection, suggester)
        return true
      }
    }
    val upOrDown = keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
    if (upOrDown && noModifiers(event) && wordActive && suggestionsOn()) {
      val word = currentWord(connection) ?: return false
      openList(word, suggester, next = keyCode == KeyEvent.KEYCODE_DPAD_DOWN, offer = offer)
      lastMoveKey = keyCode
      lastMoveTime = event.eventTime
      return true
    }
    val ending = endingCharacter(keyCode, event)
    if (ending != null && wordActive && selStart == selEnd) {
      val word = currentWord(connection) ?: return false
      if (autoCorrectOn()) {
        val corrected = suggester.correction(word.text, word.sentenceStart)
        if (corrected != null && corrected != word.text) {
          correct(word, corrected, ending, connection, suggester)
          return true
        }
      }
      if (learning) suggester.recordUse(word.text)
    }
    return false
  }

  /** Handles [keyCode] while [open] is open. Returns false for keys that close it and go on. */
  private fun onListKey(
    open: Suggestions,
    keyCode: Int,
    event: KeyEvent,
    connection: InputConnection,
    suggester: WordSuggester,
  ): Boolean {
    when (keyCode) {
      KeyEvent.KEYCODE_DPAD_DOWN,
      KeyEvent.KEYCODE_DPAD_UP -> {
        if (!noModifiers(event)) return false
        // A trackpad swipe sends two or more moves in a row; one moves through the list.
        val burst = keyCode == lastMoveKey && event.eventTime - lastMoveTime < TRACKPAD_BURST_MS
        lastMoveKey = keyCode
        lastMoveTime = event.eventTime
        if (burst) return true
        move(open, next = keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
        SandFlyKeyboardHost.speak(describe(open))
        return true
      }
      KeyEvent.KEYCODE_ENTER,
      KeyEvent.KEYCODE_NUMPAD_ENTER,
      KeyEvent.KEYCODE_DPAD_CENTER -> {
        if (!noModifiers(event)) return false
        type(open, ' ', connection, suggester)
        return true
      }
      KeyEvent.KEYCODE_BACK,
      KeyEvent.KEYCODE_ESCAPE -> {
        list = null
        SandFlyKeyboardHost.speak(context.getString(R.string.sandfly_keyboard_chooser_closed))
        return true
      }
    }
    val ending = endingCharacter(keyCode, event)
    if (ending == null || ending == '\n') return false
    type(open, ending, connection, suggester)
    return true
  }

  /** Moves through the list, or opens it, for SandFly's commands. Null without a text field. */
  fun step(next: Boolean, connection: InputConnection?, editing: Boolean): String? {
    if (!editing || connection == null) return null
    if (!suggestionsOn()) return context.getString(R.string.sandfly_keyboard_suggestions_off)
    val suggester = suggester() ?: return context.getString(noListMessage())
    val open = list
    if (open != null) {
      move(open, next)
      return describe(open)
    }
    val word = if (wordActive) currentWord(connection) else null
    if (word == null) return context.getString(R.string.sandfly_keyboard_no_suggestions)
    val offer = validOffer()
    offered = null
    return openList(word, suggester, next, speak = false, offer = offer)
  }

  /**
   * Types the suggestion chosen, or the first for the word typed, with a space. Returns false
   * without a text field; says why where nothing is typed.
   */
  fun typeSuggestion(connection: InputConnection?, editing: Boolean): Boolean {
    if (!editing || connection == null) return false
    if (!suggestionsOn()) {
      SandFlyKeyboardHost.speak(context.getString(R.string.sandfly_keyboard_suggestions_off))
      return true
    }
    val suggester = suggester()
    if (suggester == null) {
      SandFlyKeyboardHost.speak(context.getString(noListMessage()))
      return true
    }
    var open = list
    if (open == null) {
      val word = if (wordActive) currentWord(connection) else null
      val offer = validOffer()?.takeIf { it.typed == word?.text }
      offered = null
      val words =
        offer?.let { listOf(it.word) } ?: word?.let { suggester.suggestions(it.text) }.orEmpty()
      if (word == null || words.isEmpty()) {
        SandFlyKeyboardHost.speak(context.getString(R.string.sandfly_keyboard_no_suggestions))
        return true
      }
      open = Suggestions(word, words, 0)
    }
    type(open, ' ', connection, suggester)
    return true
  }

  /** Where the word list is still being read, or there is none for the language. */
  private fun noListMessage(): Int =
    if (language() == null) R.string.sandfly_keyboard_no_word_list
    else R.string.sandfly_keyboard_no_suggestions

  /**
   * The pause after typing in a word after which the suggestion is said, [IMMEDIATELY] for right
   * after the echo, or 0 for never.
   */
  private fun pauseMs(): Long {
    val value =
      prefs.getString(
        context.getString(R.string.pref_sandfly_keyboard_suggestion_pause_key),
        context.getString(R.string.pref_sandfly_keyboard_suggestion_pause_default),
      )
    if (value == SPEAK_IMMEDIATELY) return IMMEDIATELY
    return value?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
  }

  private fun schedulePause() {
    handler.removeCallbacks(pauseAnnouncement)
    if (!fieldTakesWords || !(autoCorrectOn() || suggestionsOn())) return
    val delay = pauseMs()
    announceImmediately = delay == IMMEDIATELY
    when {
      // After this selection update, with the text of the key in the field.
      announceImmediately -> handler.post(pauseAnnouncement)
      delay > 0 -> handler.postDelayed(pauseAnnouncement, delay)
    }
  }

  /**
   * Says the correction Space would type for the word before the cursor, or else its first
   * suggestion, if typing is still paused in it.
   */
  private fun announcePause() {
    if (!wordActive || list != null || selStart != selEnd) return
    val connection = connection() ?: return
    val suggester = suggester() ?: return
    val word = currentWord(connection) ?: return
    if (autoCorrectOn()) {
      val corrected = suggester.correction(word.text, word.sentenceStart)
      if (corrected != null && corrected != word.text) {
        offered = Offer(corrected, word.text, selStart)
        announce(
          context.getString(R.string.sandfly_keyboard_pending_correction, corrected),
          announceImmediately,
        )
        return
      }
    }
    if (suggestionsOn()) {
      suggester.suggestions(word.text, max = 1).firstOrNull()?.let {
        offered = Offer(it, word.text, selStart)
        announce(it, announceImmediately)
      }
    }
  }

  /**
   * Opens the list for [word] at its first or, going back, its last suggestion. The suggestion
   * already said, [offer], comes first; with Enter typing it, going on starts after it.
   */
  private fun openList(
    word: Word,
    suggester: WordSuggester,
    next: Boolean,
    speak: Boolean = true,
    offer: Offer? = null,
  ): String {
    handler.removeCallbacks(pauseAnnouncement)
    val said: String
    var words = suggester.suggestions(word.text)
    val saidFirst = offer?.takeIf { it.typed == word.text }
    if (saidFirst != null) words = listOf(saidFirst.word) + words.filter { it != saidFirst.word }
    if (words.isEmpty()) {
      list = null
      said = context.getString(R.string.sandfly_keyboard_no_suggestions)
    } else {
      val first = if (saidFirst != null && words.size > 1 && enterTypesOffer()) 1 else 0
      val open = Suggestions(word, words, if (next) first else words.size - 1)
      list = open
      said = describe(open)
    }
    if (speak) SandFlyKeyboardHost.speak(said)
    return said
  }

  private fun move(open: Suggestions, next: Boolean) {
    val size = open.words.size
    open.index = if (next) (open.index + 1) % size else (open.index - 1 + size) % size
  }

  private fun describe(open: Suggestions): String =
    context.getString(
      R.string.sandfly_keyboard_chooser_item,
      open.current,
      open.index + 1,
      open.words.size,
    )

  /** Types the suggestion chosen in [open] in place of its word, followed by [after]. */
  private fun type(
    open: Suggestions,
    after: Char,
    connection: InputConnection,
    suggester: WordSuggester,
  ) {
    list = null
    val chosen = open.current
    if (!replace(connection, open.typed, chosen + after, keepsWord = false)) return
    typedCharacter = after
    if (learning) suggester.recordUse(chosen)
    SandFlyKeyboardHost.speak(chosen)
  }

  private fun correct(
    word: Word,
    corrected: String,
    ending: Char,
    connection: InputConnection,
    suggester: WordSuggester,
  ) {
    // Enter goes to the app after the correction, as a key: it may send rather than type.
    val typed = if (ending == '\n') corrected else corrected + ending
    if (!replace(connection, word, typed, keepsWord = false)) return
    if (ending == '\n') {
      connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
      connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    } else {
      typedCharacter = ending
      correction = Correction(word.text, word.start, typed)
    }
    if (learning) suggester.recordUse(corrected)
    LogUtils.d(TAG, "Corrected a word of %d letters", word.text.length)
    SandFlyKeyboardHost.speak(context.getString(R.string.sandfly_keyboard_corrected, corrected))
  }

  private fun undo(made: Correction, connection: InputConnection, suggester: WordSuggester) {
    val start = made.start
    val word = Word(made.typed, start, sentenceStart = false)
    if (!replace(connection, word, made.original, keepsWord = true)) return
    if (learning) suggester.addWord(made.original)
    SandFlyKeyboardHost.speak(
      context.getString(R.string.sandfly_keyboard_correction_undone, made.original)
    )
  }

  /**
   * Replaces [word] with [text] in one edit, which SandFly doesn't speak as a text change: the
   * keyboard says what it did. Returns false if the field has moved on.
   */
  private fun replace(
    connection: InputConnection,
    word: Word,
    text: String,
    keepsWord: Boolean,
  ): Boolean {
    val end = word.start + word.text.length
    val before = connection.getTextBeforeCursor(word.text.length, 0)?.toString()
    if (before != word.text) return false
    SandFlyKeyboardHost.expectEdit(removed = word.text.length, added = text.length)
    ownEditEnd = word.start + text.length
    ownEditUntil = SystemClock.uptimeMillis() + OWN_EDIT_MS
    ownEditKeepsWord = keepsWord
    connection.beginBatchEdit()
    connection.setComposingRegion(word.start, end)
    connection.commitText(text, 1)
    connection.endBatchEdit()
    wordActive = keepsWord
    pendingTyping = 0
    return true
  }

  /** Counts a key that types or deletes; any other key ends the word. */
  private fun followTyping(keyCode: Int, event: KeyEvent) {
    val types =
      keyCode == KeyEvent.KEYCODE_DEL ||
        keyCode == KeyEvent.KEYCODE_FORWARD_DEL ||
        (event.unicodeChar != 0 && event.metaState and COMMAND_MODIFIERS == 0)
    if (types) {
      pendingTyping++
      lastTypingTime = SystemClock.uptimeMillis()
    } else {
      pendingTyping = 0
      wordActive = false
      list = null
      correction = null
    }
  }

  /** The keyboard's typing of a character or edit that moves the cursor, such as a double press. */
  fun onOwnTyping() {
    pendingTyping++
    lastTypingTime = SystemClock.uptimeMillis()
  }

  /** The word just before the cursor, or null where none is or it is part of an address. */
  private fun currentWord(connection: InputConnection): Word? {
    val after = connection.getTextAfterCursor(1, 0)
    if (after != null && after.isNotEmpty() && WordList.isWordCharacter(normal(after[0]))) {
      return null
    }
    val before = connection.getTextBeforeCursor(MAX_BEFORE, 0)?.toString() ?: return null
    var start = before.length
    while (start > 0 && WordList.isWordCharacter(normal(before[start - 1]))) start--
    // A word starts with a letter; quotes before it aren't part of it.
    while (start < before.length && !before[start].isLetter()) start++
    if (start == before.length) return null
    val text = before.substring(start)
    if (start > 0 && before[start - 1] in ADDRESS_CHARACTERS) return null
    if (start > 0 && before[start - 1].isDigit()) return null
    val cursor = cursorPosition(connection) ?: return null
    val sentenceStart =
      before.substring(0, start).trimEnd { it == ' ' || it == '\t' || it in OPENING }.let {
        if (it.isEmpty()) before.length < MAX_BEFORE else it.last() in SENTENCE_ENDS
      }
    return Word(text, cursor - text.length, sentenceStart)
  }

  /** Where the cursor is, from the selection updates or else the field's extracted text. */
  private fun cursorPosition(connection: InputConnection): Int? {
    if (selStart >= 0 && selStart == selEnd) return selStart
    val extracted = connection.getExtractedText(ExtractedTextRequest(), 0) ?: return null
    if (extracted.selectionStart < 0 || extracted.selectionStart != extracted.selectionEnd) {
      return null
    }
    return extracted.startOffset + extracted.selectionStart
  }

  /** Sets the suggester's key layout from the keyboard that typed [event]. */
  private fun useLayout(suggester: WordSuggester, event: KeyEvent) {
    if (event.deviceId == layoutDevice) return
    layoutDevice = event.deviceId
    suggester.layout = layoutOf(event.keyCharacterMap) ?: suggester.language.defaultLayout
  }

  private companion object {
    const val TAG = "SandFlyKeyboard"

    /** The setting value, and [pauseMs], of saying the suggestion right after the echo. */
    const val SPEAK_IMMEDIATELY = "immediately"
    const val IMMEDIATELY = -1L

    /** Enter, Numpad Enter and the centre of a D-pad, which type the suggestion said. */
    val ENTER_KEYS =
      setOf(KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_DPAD_CENTER)

    /**
     * Moves in the same direction closer together than this are one trackpad swipe: on the Zinwa
     * Q25, a swipe down sends two Down presses 68 ms apart.
     */
    const val TRACKPAD_BURST_MS = 100L

    /** How long after a typing key its selection update is waited for. */
    const val TYPING_UPDATE_MS = 1_000L

    /** How long after the keyboard's own edit its selection update is waited for. */
    const val OWN_EDIT_MS = 1_000L

    /** The most text read before the cursor for the word being typed. */
    const val MAX_BEFORE = 64

    val LIST_KEYS =
      setOf(
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER,
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_BACK,
        KeyEvent.KEYCODE_ESCAPE,
      )

    /** Characters before a word that make it part of an address, a tag or a number. */
    const val ADDRESS_CHARACTERS = "@#/\\_.:&=+~%$*<>|"

    /** Characters that end a sentence, and that open one before its first word. */
    const val SENTENCE_ENDS = ".!?…\n؟"
    const val OPENING = "\"'“‘«([{¿¡"

    /** Punctuation after a word that ends it, for auto-correct and typing a suggestion. */
    const val ENDING_PUNCTUATION = ".,!?;:)]}\"”…؟،"

    const val COMMAND_MODIFIERS =
      KeyEvent.META_CTRL_MASK or KeyEvent.META_META_MASK or KeyEvent.META_FUNCTION_ON

    fun noModifiers(event: KeyEvent): Boolean =
      event.metaState and
        (KeyEvent.META_SHIFT_MASK or KeyEvent.META_ALT_MASK or COMMAND_MODIFIERS or
          KeyEvent.META_SYM_ON) == 0

    fun normal(c: Char): Char = if (c == '’') '\'' else c

    /** Whether a field of [inputType] takes corrections and suggestions. */
    fun takesWords(inputType: Int): Boolean {
      if (inputType and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) return false
      if (inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS != 0) return false
      return when (inputType and InputType.TYPE_MASK_VARIATION) {
        InputType.TYPE_TEXT_VARIATION_PASSWORD,
        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
        InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
        InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
        InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
        InputType.TYPE_TEXT_VARIATION_URI -> false
        else -> true
      }
    }

    /** The character that [event] types to end a word, '\n' for Enter, or null. */
    fun endingCharacter(keyCode: Int, event: KeyEvent): Char? {
      if (event.metaState and COMMAND_MODIFIERS != 0) return null
      if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
        return if (event.metaState and (KeyEvent.META_SHIFT_MASK or KeyEvent.META_ALT_MASK) == 0) {
          '\n'
        } else {
          null
        }
      }
      if (keyCode == KeyEvent.KEYCODE_SPACE) {
        return if (event.metaState and KeyEvent.META_ALT_MASK == 0) ' ' else null
      }
      val c = event.unicodeChar
      if (c <= 0 || c > Char.MAX_VALUE.code) return null
      return c.toChar().takeIf { it in ENDING_PUNCTUATION }
    }

    /** The rows of letter keys of a physical keyboard, by key code. */
    val LETTER_ROWS =
      listOf(
        intArrayOf(
          KeyEvent.KEYCODE_Q,
          KeyEvent.KEYCODE_W,
          KeyEvent.KEYCODE_E,
          KeyEvent.KEYCODE_R,
          KeyEvent.KEYCODE_T,
          KeyEvent.KEYCODE_Y,
          KeyEvent.KEYCODE_U,
          KeyEvent.KEYCODE_I,
          KeyEvent.KEYCODE_O,
          KeyEvent.KEYCODE_P,
          KeyEvent.KEYCODE_LEFT_BRACKET,
          KeyEvent.KEYCODE_RIGHT_BRACKET,
        ),
        intArrayOf(
          KeyEvent.KEYCODE_A,
          KeyEvent.KEYCODE_S,
          KeyEvent.KEYCODE_D,
          KeyEvent.KEYCODE_F,
          KeyEvent.KEYCODE_G,
          KeyEvent.KEYCODE_H,
          KeyEvent.KEYCODE_J,
          KeyEvent.KEYCODE_K,
          KeyEvent.KEYCODE_L,
          KeyEvent.KEYCODE_SEMICOLON,
          KeyEvent.KEYCODE_APOSTROPHE,
        ),
        intArrayOf(
          KeyEvent.KEYCODE_Z,
          KeyEvent.KEYCODE_X,
          KeyEvent.KEYCODE_C,
          KeyEvent.KEYCODE_V,
          KeyEvent.KEYCODE_B,
          KeyEvent.KEYCODE_N,
          KeyEvent.KEYCODE_M,
          KeyEvent.KEYCODE_COMMA,
          KeyEvent.KEYCODE_PERIOD,
        ),
      )

    /**
     * The letters of [map]'s keys in their rows, so that AZERTY, QWERTZ and other layouts have
     * their own neighbours; null where the map types fewer than 20 letters on them.
     */
    fun layoutOf(map: KeyCharacterMap?): KeyLayout? {
      if (map == null) return null
      var letters = 0
      val rows =
        LETTER_ROWS.map { row ->
          buildString {
            for (keyCode in row) {
              val c = map.get(keyCode, 0)
              if (c > 0 && c <= Char.MAX_VALUE.code && c.toChar().isLetter()) {
                append(c.toChar().lowercaseChar())
                letters++
              } else {
                // Keeps the places of the keys after it.
                append('\u0000')
              }
            }
          }
        }
      return if (letters >= 20) KeyLayout.physical(rows) else null
    }
  }
}
