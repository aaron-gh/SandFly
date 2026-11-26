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
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.VisibleForTesting
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.output.FailoverTextToSpeech.SpeechParam
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechController
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions

/**
 * What SandFly and the SandFly keyboard, the input method for physical keyboards, which runs in
 * SandFly's process, know of each other: whether a text field is being edited, the keyboard's
 * character choosers, the characters of password fields, and SandFly's speech.
 */
object SandFlyKeyboardHost {
  /** The SandFly keyboard, while it is the keyboard in use. */
  interface Keyboard {
    /** Whether a text field takes what is typed. */
    val isEditing: Boolean

    /** Whether a character chooser is open, which takes the keys. */
    val isChoosing: Boolean

    /** Opens the chooser of [kind] in the text field being edited. Returns false if none is. */
    fun openChooser(kind: CharacterChooser.Kind): Boolean

    /** Whether editing mode is on, where keys without the SandFly key edit. */
    val isEditingModeOn: Boolean

    /** Turns editing mode on in the text field being edited, or off. False if it can't be on. */
    fun toggleEditingMode(): Boolean

    /** SandFly took a key press, such as a shortcut's, which the keyboard doesn't get. */
    fun onKeyTaken() {}

    /**
     * Whether the keyboard says the characters typed, deleted and moved over in the password field
     * being edited, with Speak password characters on.
     */
    val speaksPasswordCharacters: Boolean
      get() = false

    /**
     * Spells the text of the password field being edited, or says that the field isn't one.
     * Returns false if no text field is being edited.
     */
    fun spellPasswordField(): Boolean = false

    /** Whether SandFly should leave [keyCode] to the keyboard, for its word suggestions. */
    fun takesKey(keyCode: Int): Boolean = false

    /** Whether a word is being typed with Word suggestions on, for the reading control. */
    val hasWordSuggestions: Boolean
      get() = false

    /**
     * Moves to the next or previous word suggestion, opening the list, and returns what to say, or
     * null without a text field being edited.
     */
    fun stepSuggestion(next: Boolean): String? = null

    /**
     * Types the word suggestion chosen, or the first, and says it. Returns false without a text
     * field being edited.
     */
    fun typeSuggestion(): Boolean = false
  }

  @JvmStatic @Volatile var keyboard: Keyboard? = null

  /** SandFly's feedback, or null while SandFly is off. */
  @JvmStatic @Volatile var feedback: Pipeline.FeedbackReturner? = null

  /**
   * Whether a text field takes what is typed, as the SandFly keyboard knows, or null while another
   * keyboard is in use.
   */
  @JvmStatic fun isEditing(): Boolean? = keyboard?.isEditing

  /** Whether a chooser of the SandFly keyboard is open, so that keys go to it. */
  @JvmStatic fun isChoosing(): Boolean = keyboard?.isChoosing == true

  /** Whether the SandFly keyboard's editing mode is on, so that keys without the SandFly key go to it. */
  @JvmStatic fun isEditingModeOn(): Boolean = keyboard?.isEditingModeOn == true

  /** Turns editing mode on or off; false without the SandFly keyboard in a text field. */
  @JvmStatic fun toggleEditingMode(): Boolean = keyboard?.toggleEditingMode() == true

  /** Tells the SandFly keyboard that SandFly took a key press. */
  @JvmStatic
  fun onKeyTaken() {
    keyboard?.onKeyTaken()
  }

  /** Opens a chooser in the text field being edited; false without the SandFly keyboard. */
  @JvmStatic
  fun openChooser(kind: CharacterChooser.Kind): Boolean = keyboard?.openChooser(kind) == true

  /**
   * Whether the SandFly keyboard says the characters of the password field being edited, so that
   * SandFly doesn't say them as bullets.
   */
  @JvmStatic fun speaksPasswordCharacters(): Boolean = keyboard?.speaksPasswordCharacters == true

  /** Spells the password field being edited; false without the SandFly keyboard in a text field. */
  @JvmStatic fun spellPasswordField(): Boolean = keyboard?.spellPasswordField() == true

  /** Whether SandFly should leave [keyCode] to the SandFly keyboard's word suggestions. */
  @JvmStatic fun takesKey(keyCode: Int): Boolean = keyboard?.takesKey(keyCode) == true

  /** Whether a word with suggestions is being typed with the SandFly keyboard. */
  @JvmStatic fun hasWordSuggestions(): Boolean = keyboard?.hasWordSuggestions == true

  /** What the next or previous word suggestion says, or null without the keyboard in a field. */
  @JvmStatic fun stepSuggestion(next: Boolean): String? = keyboard?.stepSuggestion(next)

  /** Types the word suggestion chosen; false without the SandFly keyboard in a text field. */
  @JvmStatic fun typeSuggestion(): Boolean = keyboard?.typeSuggestion() == true

  /**
   * Deletes the learned words of the keyboard's word list in memory and in files, which the word
   * list sets once read; null until then.
   */
  @JvmStatic @Volatile var learnedWordsDeleter: ((Context) -> Unit)? = null

  /** Deletes the words that auto-correct and word suggestions learned, of every language. */
  @JvmStatic
  fun deleteLearnedWords(context: Context) {
    val deleter = learnedWordsDeleter
    if (deleter != null) {
      deleter(context)
      return
    }
    context.filesDir.listFiles { _, name -> name.startsWith(LEARNED_WORDS_PREFIX) }?.forEach {
      it.delete()
    }
  }

  /** The start of the names of the files of learned words. */
  const val LEARNED_WORDS_PREFIX = "sandfly_keyboard_words_"

  /** The text change the keyboard's last own edit makes: removed and added lengths, and until when. */
  @Volatile private var expectedEdit: LongArray? = null

  /**
   * Tells SandFly that the keyboard replaces [removed] characters with [added], and says what it
   * did, so that the text change isn't spoken too.
   */
  @JvmStatic
  fun expectEdit(removed: Int, added: Int) {
    expectedEdit =
      longArrayOf(removed.toLong(), added.toLong(), SystemClock.uptimeMillis() + EDIT_EVENT_MS)
  }

  /** Whether [event] is the text change of the keyboard's last own edit, which it then forgets. */
  @JvmStatic
  fun isKeyboardEdit(event: AccessibilityEvent): Boolean {
    val edit = expectedEdit ?: return false
    if (SystemClock.uptimeMillis() > edit[2]) {
      expectedEdit = null
      return false
    }
    if (event.removedCount.toLong() != edit[0] || event.addedCount.toLong() != edit[1]) {
      return false
    }
    expectedEdit = null
    return true
  }

  /** How long after the keyboard's own edit its text change is waited for. */
  private const val EDIT_EVENT_MS = 1_000L

  /**
   * Speaks characters of a password field at once, at [pitch] relative to the speech pitch, in
   * [queueMode]. They are kept out of the speech history, so that Repeat, Spell and Copy last spoken
   * phrase and the Speech history list don't have them.
   */
  @JvmStatic
  fun speakPassword(
    text: CharSequence,
    pitch: Float = 1f,
    queueMode: Int = SpeechController.QUEUE_MODE_INTERRUPT,
  ) {
    feedback?.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.speech(
        text,
        SpeakOptions.create()
          .setQueueMode(queueMode)
          .setUtteranceGroup(SpeechController.UTTERANCE_GROUP_TEXT_SELECTION)
          .setSpeechParams(Bundle().apply { putFloat(SpeechParam.PITCH, pitch) })
          .setFlags(FeedbackItem.FLAG_FORCE_FEEDBACK_ALL or FeedbackItem.FLAG_NO_HISTORY),
      ),
    )
  }

  /**
   * Speaks a correction or word suggestion said while typing, at a lower pitch than typing: at
   * once, interrupting other speech, or with [queued], after the speech queued before it, such as
   * the keyboard echo of the key just typed. The next key stops it ([stopSuggestion]).
   */
  @JvmStatic
  fun speakSuggestion(text: CharSequence, queued: Boolean = false) {
    val feedback = feedback ?: return
    suggestionSpoken = true
    feedback.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.speech(
        text,
        SpeakOptions.create()
          .setQueueMode(
            if (queued) SpeechController.QUEUE_MODE_QUEUE else SpeechController.QUEUE_MODE_INTERRUPT
          )
          .setUtteranceGroup(SpeechController.UTTERANCE_GROUP_KEYBOARD_SUGGESTIONS)
          .setSpeechParams(Bundle().apply { putFloat(SpeechParam.PITCH, SUGGESTION_PITCH) })
          .setFlags(
            FeedbackItem.FLAG_FORCE_FEEDBACK_ALL or
              FeedbackItem.FLAG_CLEAR_QUEUED_UTTERANCES_WITH_SAME_UTTERANCE_GROUP or
              FeedbackItem.FLAG_INTERRUPT_CURRENT_UTTERANCE_WITH_SAME_UTTERANCE_GROUP
          ),
      ),
    )
  }

  /**
   * Speaks [text] right after the keyboard echo of the key pressed at [keyTime], in uptime
   * milliseconds: once SandFly has handled the text change that key made, whether it spoke it or
   * not, or at once if it already has. Where no text change comes within [ECHO_WAIT_MS], as with
   * fields that send none, [text] is spoken then.
   */
  @JvmStatic
  fun speakSuggestionAfterEcho(text: CharSequence, keyTime: Long) {
    main.removeCallbacks(pendingTimeout)
    if (lastTextChangeHandled >= keyTime) {
      pendingSuggestion = null
      speakSuggestion(text, queued = true)
      return
    }
    pendingSuggestion = text
    main.postDelayed(pendingTimeout, ECHO_WAIT_MS)
  }

  /**
   * SandFly handled a text change, after any keyboard echo of it was queued: speaks the suggestion
   * waiting for it.
   */
  @JvmStatic
  fun onTextChangeHandled() {
    lastTextChangeHandled = SystemClock.uptimeMillis()
    val text = pendingSuggestion ?: return
    pendingSuggestion = null
    main.removeCallbacks(pendingTimeout)
    speakSuggestion(text, queued = true)
  }

  /** Forgets a suggestion waiting for the echo, and stops one spoken or queued. */
  @JvmStatic
  fun stopSuggestion() {
    pendingSuggestion = null
    main.removeCallbacks(pendingTimeout)
    if (suggestionSpoken) {
      suggestionSpoken = false
      suggestionStopper?.invoke()
    }
  }

  /** Forgets the suggestion speech state, for tests, whose clock starts again for each. */
  @VisibleForTesting
  fun resetSuggestionSpeech() {
    stopSuggestion()
    lastTextChangeHandled = 0L
    suggestionSpoken = false
  }

  /** Stops the speech of [SpeechController.UTTERANCE_GROUP_KEYBOARD_SUGGESTIONS]; set by SandFly. */
  @JvmStatic @Volatile var suggestionStopper: (() -> Unit)? = null

  private val main = Handler(Looper.getMainLooper())
  private var pendingSuggestion: CharSequence? = null
  private var lastTextChangeHandled = 0L
  private var suggestionSpoken = false
  private val pendingTimeout = Runnable {
    val text = pendingSuggestion
    pendingSuggestion = null
    if (text != null) speakSuggestion(text, queued = true)
  }

  /** How long a suggestion waits for the text change of its key before it is spoken anyway. */
  private const val ECHO_WAIT_MS = 300L

  /** The pitch of corrections and suggestions said while typing, relative to speech's. */
  private const val SUGGESTION_PITCH = 0.8f

  /** Speaks [text] at once, as SandFly's own feedback. */
  @JvmStatic
  fun speak(text: CharSequence) {
    feedback?.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.speech(
        text,
        SpeakOptions.create()
          .setQueueMode(SpeechController.QUEUE_MODE_INTERRUPT)
          .setFlags(FeedbackItem.FLAG_FORCE_FEEDBACK_ALL),
      ),
    )
  }
}
