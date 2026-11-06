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

package com.google.android.accessibility.talkback.gesture

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.google.android.accessibility.utils.FeatureSupport

/**
 * Touches that SandFly makes itself, such as the swipe of pull to refresh, sent to the app as they
 * are. Android passes them through touch exploration like the user's own, which would take a swipe
 * as a SandFly gesture; so the touch they start is handed straight to the app, which gets the
 * swipe. Android cancels the user's own touches while they run, so no real touch passes with them.
 */
object SyntheticTouches {
  /** Until when, in uptime, the next touch to go down is SandFly's own. */
  @Volatile private var expectedUntil = 0L

  private val handler = Handler(Looper.getMainLooper())

  /**
   * Returns whether SandFly can send touches that reach the app as they are: only where it
   * recognizes gestures itself, and so sees the touches and can hand them on.
   */
  @JvmStatic fun isAvailable(): Boolean = FeatureSupport.supportGestureDetection()

  /**
   * Sends [gesture], which lasts [durationMs], to the app under it, and runs [onEnd] when it ends
   * or is cancelled. Returns false when it couldn't be sent.
   */
  @JvmStatic
  fun dispatch(
    service: AccessibilityService,
    gesture: GestureDescription,
    durationMs: Long,
    onEnd: Runnable? = null,
  ): Boolean {
    if (!isAvailable()) return false
    expectedUntil = SystemClock.uptimeMillis() + durationMs + START_MARGIN_MS
    val callback =
      object : AccessibilityService.GestureResultCallback() {
        override fun onCompleted(description: GestureDescription) {
          expectedUntil = 0L
          onEnd?.run()
        }

        override fun onCancelled(description: GestureDescription) {
          expectedUntil = 0L
          onEnd?.run()
        }
      }
    if (!service.dispatchGesture(gesture, callback, handler)) {
      expectedUntil = 0L
      return false
    }
    return true
  }

  /**
   * Returns whether the touch going down now is one SandFly sent, which is then no longer
   * expected. Called by touch handling for each touch that starts.
   */
  @JvmStatic
  fun takeIfExpected(): Boolean {
    if (SystemClock.uptimeMillis() > expectedUntil) return false
    expectedUntil = 0L
    return true
  }

  /** How long after the gesture is sent its touch may still go down. */
  private const val START_MARGIN_MS = 500L
}
