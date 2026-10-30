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
package com.google.android.accessibility.talkback.handwriting

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Region
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.google.android.accessibility.talkback.ActorState
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Feedback.PassThroughMode.Action.LOCK_PASS_THROUGH
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * A writing surface on the whole screen, over everything. While it shows, touches on the screen go
 * straight to [onTouch] instead of through touch exploration, as the circle menu takes touches.
 */
class HandwritingOverlay(
  private val service: AccessibilityService,
  private val pipeline: Pipeline.FeedbackReturner,
  private val actorState: ActorState,
  /** The window's title. */
  private val title: String,
  private val onTouch: (MotionEvent) -> Unit,
) {
  private val windowManager = service.getSystemService(WindowManager::class.java)
  private var view: View? = null
  private var passthroughRegionSet = false

  val isShowing: Boolean
    get() = view != null

  val width: Float
    get() = view?.width?.toFloat() ?: 0f

  val height: Float
    get() = view?.height?.toFloat() ?: 0f

  /** Shows the surface, and returns false when it can't. */
  @SuppressLint("ClickableViewAccessibility")
  fun show(): Boolean {
    if (isShowing) return true
    val surface =
      object : View(service as Context) {
        override fun onTouchEvent(event: MotionEvent): Boolean {
          onTouch(event)
          return true
        }
      }
    surface.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    val params =
      WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        format = PixelFormat.TRANSLUCENT
        flags =
          WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        width = WindowManager.LayoutParams.MATCH_PARENT
        height = WindowManager.LayoutParams.MATCH_PARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        title = this@HandwritingOverlay.title
      }
    try {
      windowManager.addView(surface, params)
    } catch (e: RuntimeException) {
      LogUtils.e(TAG, "Could not show the handwriting surface: %s", e)
      return false
    }
    view = surface
    setPassthroughRegion()
    return true
  }

  /** Removes the surface. */
  fun remove() {
    val current = view ?: return
    view = null
    clearPassthroughRegion()
    try {
      windowManager.removeViewImmediate(current)
    } catch (e: IllegalArgumentException) {
      LogUtils.w(TAG, "Handwriting surface was not attached: %s", e)
    }
  }

  /** Sends touches on the whole screen straight to the surface, instead of through touch exploration. */
  private fun setPassthroughRegion() {
    // Pass-through mode and the braille keyboard use the same region, so leave it alone for them.
    if (
      Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
        actorState.passThroughModeState.isPassThroughModeActive
    ) {
      return
    }
    val display =
      service.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY) ?: return
    val metrics = DisplayMetrics()
    @Suppress("DEPRECATION") display.getRealMetrics(metrics)
    // Through pass-through mode, which puts back the region it had, such as direct touch's.
    pipeline.returnFeedback(
      EVENT_ID_UNTRACKED,
      Feedback.passThroughMode(LOCK_PASS_THROUGH, Region(0, 0, metrics.widthPixels, metrics.heightPixels)),
    )
    passthroughRegionSet = true
  }

  private fun clearPassthroughRegion() {
    if (passthroughRegionSet) {
      passthroughRegionSet = false
      pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.passThroughMode(LOCK_PASS_THROUGH, null))
    }
  }

  private companion object {
    const val TAG = "HandwritingOverlay"
  }
}
