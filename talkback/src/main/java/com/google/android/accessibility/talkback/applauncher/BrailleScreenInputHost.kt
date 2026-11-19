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
package com.google.android.accessibility.talkback.applauncher

/** What braille commands reach in SandFly. */
interface BrailleScreenInputHost {
  /** Runs the command saved as [value], as a gesture assigned to it would. */
  fun runCommand(value: String)

  /** Whether the braille keyboard is shown, or a text field is being edited. */
  fun canType(): Boolean

  /** Whether the braille keyboard is shown. */
  fun isBrailleKeyboardShown(): Boolean

  /**
   * Changes the input method to the braille keyboard, or says why it can't, and returns whether it
   * changed.
   */
  fun switchToBrailleKeyboard(): Boolean
}
