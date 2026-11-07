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
import com.google.android.accessibility.talkback.focusmanagement.HomeScreenPages
import com.google.android.accessibility.utils.monitor.ScreenMonitor

/**
 * SandFly's own swipe up, sent to what is in front as it is; see [SyntheticTouches]. On the lock
 * screen, two fingers unlock it or show the PIN, pattern or password, for the Home gesture of the
 * VoiceOver layout, as Home does on an iPhone. On the home screen, one finger opens the apps list,
 * as a two-finger swipe passed to the launcher does, for when SandFly takes two-finger swipes.
 */
class SwipeUp(private val service: AccessibilityService) {
  private val handler = Handler(Looper.getMainLooper())
  private var pending: Runnable? = null

  /** Returns whether the lock screen is in front: the device is locked and the system UI shows. */
  fun isLockScreenShown(): Boolean {
    if (!ScreenMonitor.isDeviceLocked(service)) return false
    val root = service.rootInActiveWindow ?: return false
    return root.packageName?.toString() == SYSTEM_UI
  }

  /**
   * Returns whether the home screen is the app in front and its window is active, not covered by
   * the lock screen, the notification shade or quick settings.
   */
  fun isHomeScreenShown(): Boolean =
    HomeScreenPages.isHomePackage(service, AppGestures.appShown()) &&
      HomeScreenPages.isHomePackage(service, service.rootInActiveWindow?.packageName)

  /**
   * Swipes up with [fingers] fingers, one or two, in the middle of the screen. Returns whether it
   * was sent.
   */
  fun swipeUp(fingers: Int): Boolean {
    if (!SyntheticTouches.isAvailable() || pending != null) return false
    val metrics = service.resources.displayMetrics
    val width = metrics.widthPixels.toFloat()
    val height = metrics.heightPixels.toFloat()
    if (width <= 0f || height <= 0f) return false
    val startY = height * START_FRACTION
    val endY = height * END_FRACTION
    val builder = GestureDescription.Builder()
    val xs =
      if (fingers >= 2) listOf(width * LEFT_FINGER_FRACTION, width * RIGHT_FINGER_FRACTION)
      else listOf(width / 2)
    for (x in xs) {
      val path =
        Path().apply {
          moveTo(x, startY)
          lineTo(x, endY)
        }
      builder.addStroke(GestureDescription.StrokeDescription(path, 0, SWIPE_DURATION_MS))
    }
    val gesture = builder.build()
    // The gesture that asked for the swipe may still be ending.
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
