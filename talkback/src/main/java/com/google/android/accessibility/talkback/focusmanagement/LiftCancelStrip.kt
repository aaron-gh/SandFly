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

package com.google.android.accessibility.talkback.focusmanagement

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * A transparent strip along the bottom edge of the screen, over every app, where lifting the
 * finger, such as by sliding it off the bottom of the screen, activates nothing with Lift to
 * activate.
 *
 * Once touch exploration starts, SandFly gets no motion events, only the items the finger touches,
 * and an item can reach the bottom of the screen. Hovers over SandFly's own window still reach it,
 * so the strip reports when the finger is in it. The strip sends no accessibility events, so the
 * item the finger touched before stays the touched item.
 */
class LiftCancelStrip(private val context: Context, private val listener: Listener) {
  /** Receives where the finger is. */
  fun interface Listener {
    /**
     * Called with true and the time of the hover when the finger moves into the strip, and with
     * false [EXIT_DELAY_MS] after it leaves the strip. Lifting the finger also leaves the strip, but
     * the touch has ended by the time false is reported.
     */
    fun onFingerInStrip(inStrip: Boolean, timeMs: Long)
  }

  private val windowManager = context.getSystemService(WindowManager::class.java)
  private var view: View? = null
  // Whether the hovers of the touch in progress are in the strip.
  private var hovering = false
  private val reportExit = Runnable {
    if (hovering) {
      hovering = false
      listener.onFingerInStrip(false, SystemClock.uptimeMillis())
    }
  }

  /** Shows the strip, or removes it. */
  fun setShown(shown: Boolean) {
    if (shown == (view != null)) {
      return
    }
    if (shown) show() else hide()
  }

  private fun show() {
    val strip =
      object : View(context) {
        override fun onHoverEvent(event: MotionEvent): Boolean {
          when (event.actionMasked) {
            MotionEvent.ACTION_HOVER_ENTER,
            MotionEvent.ACTION_HOVER_MOVE -> {
              removeCallbacks(reportExit)
              if (!hovering) {
                hovering = true
                listener.onFingerInStrip(true, event.eventTime)
              }
            }
            MotionEvent.ACTION_HOVER_EXIT -> {
              LogUtils.v(TAG, "Finger left the strip at y=%f", event.y)
              // The same exit comes when the finger lifts in the strip and when it moves back up.
              // A lift ends the touch within milliseconds, before the exit is reported.
              postDelayed(reportExit, EXIT_DELAY_MS)
            }
            else -> {}
          }
          return true
        }

        override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent): Boolean = true

        override fun sendAccessibilityEvent(eventType: Int) {}

        override fun sendAccessibilityEventUnchecked(event: AccessibilityEvent) {}
      }
    strip.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    val heightPx =
      TypedValue.applyDimension(
          TypedValue.COMPLEX_UNIT_MM,
          STRIP_HEIGHT_MM,
          context.resources.displayMetrics,
        )
        .toInt()
        .coerceAtLeast(1)
    val params =
      WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        heightPx,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
          WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
          WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
          WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
      )
    params.gravity = Gravity.BOTTOM
    params.title = "SandFly lift cancel strip"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      params.layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      params.fitInsetsTypes = 0
      params.fitInsetsSides = 0
    }
    try {
      windowManager.addView(strip, params)
      view = strip
    } catch (e: RuntimeException) {
      // BadTokenException or IllegalStateException, when the service has no window token.
      LogUtils.w(TAG, "Couldn't show the lift cancel strip: %s", e)
    }
  }

  private fun hide() {
    val strip = view ?: return
    view = null
    strip.removeCallbacks(reportExit)
    try {
      windowManager.removeView(strip)
    } catch (e: IllegalArgumentException) {
      LogUtils.w(TAG, "Lift cancel strip was not shown: %s", e)
    }
    if (hovering) {
      hovering = false
      listener.onFingerInStrip(false, SystemClock.uptimeMillis())
    }
  }

  private companion object {
    const val TAG = "LiftCancelStrip"

    /** The height of the strip. A finger sliding off the screen is last reported within it. */
    const val STRIP_HEIGHT_MM = 2f

    /** How long after the finger leaves the strip it is reported out of it. */
    const val EXIT_DELAY_MS = 150L
  }
}
