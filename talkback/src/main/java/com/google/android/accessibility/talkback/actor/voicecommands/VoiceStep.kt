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

package com.google.android.accessibility.talkback.actor.voicecommands

/** One step of a spoken voice command, which can chain several. */
sealed class VoiceStep {
  /** Runs the command saved as [value], as a gesture assigned to it would. */
  data class Command(val value: String, val label: String) : VoiceStep()

  /** Activates the item on the screen whose text or description is [label]. */
  data class Tap(val label: String) : VoiceStep()

  /** Opens the app [name], whose command value is [value] once it is known. */
  data class OpenApp(val name: String, val value: String? = null) : VoiceStep()

  /** Types [text] into the edit field with input focus. */
  data class TypeText(val text: String) : VoiceStep()

  data class Wait(val millis: Long) : VoiceStep()

  /** Calls the contact or phone number [who]. */
  data class Call(val who: String) : VoiceStep()

  /**
   * Writes a message. The recipient is [who], or else the contact whose name starts [whoAndText],
   * the rest of which is the text. The text is [text], or else the result of the step before when
   * [usePrevious], or else it is asked for. [app] names the messaging app; null is SMS.
   */
  data class Message(
    val who: String? = null,
    val whoAndText: String? = null,
    val text: String? = null,
    val app: String? = null,
    val usePrevious: Boolean = false,
  ) : VoiceStep()

  /** Speaks a description of the screen, or of the focused image, and keeps it as the result. */
  data class Describe(val screen: Boolean) : VoiceStep()

  /** Copies the result of the step before, or else the last spoken phrase. */
  data object Copy : VoiceStep()

  data class Say(val text: String) : VoiceStep()

  /** Runs [text] as one of the voice commands that came with TalkBack. */
  data class Legacy(val text: String) : VoiceStep()
}
