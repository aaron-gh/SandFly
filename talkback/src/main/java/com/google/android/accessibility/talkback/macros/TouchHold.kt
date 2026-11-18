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

package com.google.android.accessibility.talkback.macros

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Region
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Feedback.PassThroughMode.Action.LOCK_PASS_THROUGH
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.libraries.accessibility.utils.device.ScreenUtils

/**
 * A touch held on an item, passed through to the app as a double-tap and hold is: for buttons that
 * act on a touch held down, such as a Like button that shows reactions, which the accessibility
 * long click doesn't open.
 */
object TouchHold {
  const val DEFAULT_HOLD_MS = 1000L
  private const val MAX_HOLD_MS = 10_000L
  private const val PASS_THROUGH_WAIT_MS = 350L

  /**
   * Touches the middle of the part of [bounds] on screen for [holdMs], then calls [done] on the
   * main thread with whether the touch was performed. Needs Android 11. Called on the main thread.
   */
  @JvmStatic
  fun perform(service: TalkBackService, bounds: Rect, holdMs: Long, done: (Boolean) -> Unit) {
    val pipeline = service.feedbackReturner
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || pipeline == null) return done(false)
    val size = ScreenUtils.getRealScreenSize(service)
    val visible = Rect(bounds)
    if (!visible.intersect(0, 0, size.x, size.y)) return done(false)
    val x = visible.exactCenterX().coerceIn(0f, (size.x - 1).toFloat())
    val y = visible.exactCenterY().coerceIn(0f, (size.y - 1).toFloat())
    val main = Handler(Looper.getMainLooper())
    // Touch exploration would take the touch as the person's own, so it goes to the app instead.
    pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.passThroughMode(LOCK_PASS_THROUGH, Region(0, 0, size.x, size.y)))
    var ended = false
    val end = { performed: Boolean ->
      if (!ended) {
        ended = true
        pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.passThroughMode(LOCK_PASS_THROUGH, null))
        done(performed)
      }
    }
    // Android applies the pass-through region a moment later.
    main.postDelayed(
      {
        try {
          val path = Path().apply { moveTo(x, y) }
          val stroke = GestureDescription.StrokeDescription(path, 0, holdMs.coerceIn(1L, MAX_HOLD_MS))
          val gesture = GestureDescription.Builder().addStroke(stroke).build()
          val callback =
            object : AccessibilityService.GestureResultCallback() {
              override fun onCompleted(description: GestureDescription) = end(true)

              override fun onCancelled(description: GestureDescription) = end(false)
            }
          if (!service.dispatchGesture(gesture, callback, main)) end(false)
        } catch (e: RuntimeException) {
          end(false)
        }
      },
      PASS_THROUGH_WAIT_MS,
    )
  }
}
