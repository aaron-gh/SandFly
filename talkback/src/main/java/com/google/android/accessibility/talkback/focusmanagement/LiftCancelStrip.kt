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
 * so the strip reports when the finger moves in and out of it. The strip sends no accessibility
 * events, so the item the finger touched before stays the touched item.
 */
class LiftCancelStrip(private val context: Context, private val listener: Listener) {
  /** Receives where the finger is. */
  interface Listener {
    /** Called when the finger moves into the strip, with the time of the hover. */
    fun onFingerEnteredStrip(timeMs: Long)

    /**
     * Called when the finger leaves the strip, with the time of the hover. Moving back up and
     * lifting the finger in the strip send the same exit; only moving up is followed by an app
     * reporting the item under the finger, from this time on.
     */
    fun onFingerLeftStrip(timeMs: Long)
  }

  private val windowManager = context.getSystemService(WindowManager::class.java)
  private var view: View? = null
  // Whether the hovers of the touch in progress are in the strip.
  private var hovering = false

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
            MotionEvent.ACTION_HOVER_MOVE ->
              if (!hovering) {
                hovering = true
                listener.onFingerEnteredStrip(event.eventTime)
              }
            MotionEvent.ACTION_HOVER_EXIT -> {
              hovering = false
              listener.onFingerLeftStrip(event.eventTime)
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
    try {
      windowManager.removeView(strip)
    } catch (e: IllegalArgumentException) {
      LogUtils.w(TAG, "Lift cancel strip was not shown: %s", e)
    }
    if (hovering) {
      hovering = false
      listener.onFingerLeftStrip(SystemClock.uptimeMillis())
    }
  }

  private companion object {
    const val TAG = "LiftCancelStrip"

    /**
     * The height of the strip. On the Fold's inner screen, a finger sliding off the bottom was
     * reported at most 1.4 to 3.2 mm from the edge.
     */
    const val STRIP_HEIGHT_MM = 3.5f
  }
}
