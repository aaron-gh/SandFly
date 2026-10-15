/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.talkback.directtouch

/**
 * What is on screen and how the user set things up, as far as direct touch cares. The two checks
 * that ask apps for their windows' roots are functions, so that they run only when the rest leave
 * the answer open.
 */
data class DirectTouchInput(
  val masterEnabled: Boolean,
  /** The app in front is turned on in the direct touch list. */
  val appEnabled: Boolean,
  val screenInteractive: Boolean,
  val dialogShowing: Boolean,
  val textFieldFocused: Boolean,
  /** Another app's window is on top of the game. */
  val foreignAppWindow: () -> Boolean,
  /** The notification shade or quick settings cover at least half the screen. */
  val systemUiCoversHalf: () -> Boolean,
)

object DirectTouchPolicy {
  /**
   * Whether a SystemUI window is the notification shade or quick settings. The shade is active and
   * covers half the screen or more. Only active windows count, because a landscape navigation bar
   * is more than half the display tall. That bar is still active while a finger explores it, so
   * direct touch goes off then and comes back when the active window returns to the game.
   */
  fun coversHalfScreen(active: Boolean, windowHeight: Int, displayHeight: Int): Boolean =
    active && windowHeight > displayHeight / 2

  /** Whether touches should go straight to the app in front. */
  fun shouldBeActive(input: DirectTouchInput): Boolean =
    input.masterEnabled &&
      input.appEnabled &&
      input.screenInteractive &&
      !input.dialogShowing &&
      !input.textFieldFocused &&
      !input.foreignAppWindow() &&
      !input.systemUiCoversHalf()
}
