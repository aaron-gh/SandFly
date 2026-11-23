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

package com.google.android.accessibility.talkback.stucktouch

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityManager
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Sets up Android's touch explorer again when it keeps fingers that have lifted.
 *
 * When the screen turns off during a touch, Android cancels the touch with an event that its
 * accessibility input filter passes on without giving it to the touch explorer. The touch explorer
 * keeps those fingers as down, and from then on every touch ends in touch exploration instead of
 * the clear state: SandFly gets no motion events, so no gesture, double tap or edge swipe is
 * detected, and only exploring works. Only a new touch explorer clears it.
 *
 * Turning explore by touch off removes the touch explorer, and turning it on makes a new one.
 * Android updates its input filter before it tells apps that touch exploration is off, so explore
 * by touch is turned on again when that state change arrives, or after [TIMEOUT_MS].
 */
class TouchExplorerReset(
  context: Context,
  private val touchExploration: StuckTouchController.TouchExploration,
) {

  private val handler = Handler(Looper.getMainLooper())
  private val accessibilityManager = context.getSystemService(AccessibilityManager::class.java)
  private var resetting = false
  private val listener =
    AccessibilityManager.TouchExplorationStateChangeListener { enabled ->
      if (!enabled) {
        finish()
      }
    }
  private val timeout = Runnable { finish() }

  /** Turns explore by touch off and on again, unless it is off or a reset is in progress. */
  fun reset(reason: String) {
    if (resetting || !touchExploration.isOn()) {
      return
    }
    LogUtils.w(TAG, "Setting touch exploration up again: %s", reason)
    resetting = true
    accessibilityManager?.addTouchExplorationStateChangeListener(listener)
    touchExploration.turnOff()
    handler.postDelayed(timeout, TIMEOUT_MS)
  }

  fun shutdown() {
    // Explore by touch stays as it is: the service is going away, and turns it off itself.
    stop()
  }

  private fun finish() {
    if (!resetting) {
      return
    }
    stop()
    touchExploration.restore()
  }

  private fun stop() {
    resetting = false
    handler.removeCallbacks(timeout)
    accessibilityManager?.removeTouchExplorationStateChangeListener(listener)
  }

  companion object {
    private const val TAG = "TouchExplorerReset"

    /** How long explore by touch stays off if Android doesn't report the change. */
    const val TIMEOUT_MS = 1_000L
  }
}
