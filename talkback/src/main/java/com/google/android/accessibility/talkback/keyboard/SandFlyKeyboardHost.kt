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

import android.os.Bundle
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
