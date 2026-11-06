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
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import com.google.android.accessibility.utils.monitor.ScreenMonitor

/**
 * Swipes up with two fingers on the lock screen, which unlocks it or shows the PIN, pattern or
 * password, for the Home gesture of the VoiceOver layout, as Home does on an iPhone. Android's lock
 * screen takes that from a two-finger swipe up while a screen reader explores touch.
 */
class LockScreenSwipe(private val service: AccessibilityService) {
  private val handler = Handler(Looper.getMainLooper())
  private var pending: Runnable? = null

  /** Returns whether the lock screen is in front: the device is locked and the system UI shows. */
  fun isLockScreenShown(): Boolean {
    if (!ScreenMonitor.isDeviceLocked(service)) return false
    val root = service.rootInActiveWindow ?: return false
    return root.packageName?.toString() == SYSTEM_UI
  }

  /** Swipes up with two fingers in the middle of the screen. Returns whether it was sent. */
  fun swipeUp(): Boolean {
    if (!SyntheticTouches.isAvailable() || pending != null) return false
    val metrics = service.resources.displayMetrics
    val width = metrics.widthPixels.toFloat()
    val height = metrics.heightPixels.toFloat()
    if (width <= 0f || height <= 0f) return false
    val startY = height * START_FRACTION
    val endY = height * END_FRACTION
    val builder = GestureDescription.Builder()
    for (x in listOf(width * LEFT_FINGER_FRACTION, width * RIGHT_FINGER_FRACTION)) {
      val path =
        Path().apply {
          moveTo(x, startY)
          lineTo(x, endY)
        }
      builder.addStroke(GestureDescription.StrokeDescription(path, 0, SWIPE_DURATION_MS))
    }
    val gesture = builder.build()
    // The swipe goes to the lock screen as it is; see SyntheticTouches. The Home gesture may still
    // be ending.
    val send = Runnable {
      pending = null
      SyntheticTouches.dispatch(service, gesture, SWIPE_DURATION_MS)
    }
    pending = send
    handler.postDelayed(send, GESTURE_END_WAIT_MS)
    return true
  }

  fun cancel() {
    pending?.let {
      handler.removeCallbacks(it)
      pending = null
    }
  }

  private companion object {
    const val SYSTEM_UI = "com.android.systemui"
    const val START_FRACTION = 0.75f
    const val END_FRACTION = 0.3f
    const val LEFT_FINGER_FRACTION = 0.4f
    const val RIGHT_FINGER_FRACTION = 0.6f
    const val SWIPE_DURATION_MS = 300L
    const val GESTURE_END_WAIT_MS = 150L
  }
}
