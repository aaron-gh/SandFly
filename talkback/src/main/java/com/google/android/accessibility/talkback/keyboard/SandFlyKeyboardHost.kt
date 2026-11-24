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

import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechController
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions

/**
 * What SandFly and the SandFly keyboard, the input method for physical keyboards, which runs in
 * SandFly's process, know of each other: whether a text field is being edited, the keyboard's
 * character choosers, and SandFly's speech.
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
