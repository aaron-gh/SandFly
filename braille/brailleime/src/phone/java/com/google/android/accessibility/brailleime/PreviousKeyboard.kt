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

package com.google.android.accessibility.brailleime

/** Decides when the braille keyboard changes back to the keyboard used before it. */
object PreviousKeyboard {
  /**
   * Whether to change back now: [settingOn] for the Return to the previous keyboard setting,
   * [brailleDisplayConnected] while a braille display types through the braille keyboard,
   * [otherKeyboardEnabled] when there is another keyboard to change to, and [stillCurrent] while
   * the braille keyboard is still the current keyboard, since Android also ends the input as it
   * changes to another keyboard.
   */
  fun shouldReturn(
    settingOn: Boolean,
    brailleDisplayConnected: Boolean,
    otherKeyboardEnabled: Boolean,
    stillCurrent: Boolean,
  ): Boolean = settingOn && !brailleDisplayConnected && otherKeyboardEnabled && stillCurrent

  /**
   * Whether [currentImeId], the current keyboard as Android stores it, such as
   * "com.example/.Keyboard", is the keyboard with [packageName] and [className].
   */
  fun isCurrent(currentImeId: String?, packageName: String, className: String): Boolean {
    val parts = currentImeId?.split('/') ?: return false
    if (parts.size != 2) {
      return false
    }
    val (imePackage, imeClass) = parts
    val fullClass = if (imeClass.startsWith('.')) imePackage + imeClass else imeClass
    return imePackage == packageName && fullClass == className
  }
}
