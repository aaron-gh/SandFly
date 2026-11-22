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
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.Region
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.ViewConfiguration
import androidx.annotation.ChecksSdkIntAtLeast
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Feedback.PassThroughMode.Action.LOCK_PASS_THROUGH
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.libraries.accessibility.utils.device.ScreenUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Touches dispatched to the app as a finger's would be, with touch passed through to the app over
 * the whole screen while they run: a dispatched gesture otherwise goes through touch exploration,
 * which takes it as a touch of its own. Used by macro touches, touch-and-hold rules and Activate
 * with a tap. Called on the main thread.
 *
 * Touches running at the same time share the pass-through lock, which is released when the last of
 * them ends. A lock held by another feature, such as the braille keyboard, is left alone and no
 * touch is made.
 */
object PassThroughTouch {
  private const val TAG = "PassThroughTouch"

  /** How long Android takes to apply the pass-through region. */
  private const val PASS_THROUGH_WAIT_MS = 350L

  private val main = Handler(Looper.getMainLooper())

  /** Touches running that hold the pass-through lock. */
  private var holds = 0

  /** Whether touches can be dispatched: Android 11 and later. */
  @JvmStatic
  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
  fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

  /** Whether a feature other than these touches holds the pass-through lock. */
  @JvmStatic
  fun isLockedElsewhere(service: TalkBackService): Boolean {
    if (holds > 0 || service.feedbackReturner == null) return false
    return service.actorState?.passThroughModeState?.isLocked == true
  }

  /**
   * The centre of the part of [bounds] on the screen, or null when [bounds] are empty or off the
   * screen.
   */
  @JvmStatic
  fun visibleCentre(service: TalkBackService, bounds: Rect): PointF? {
    if (bounds.isEmpty) return null
    val size = ScreenUtils.getRealScreenSize(service)
    val visible = Rect(bounds)
    if (!visible.intersect(0, 0, size.x, size.y)) return null
    return PointF(
      visible.exactCenterX().coerceIn(0f, (size.x - 1).toFloat()),
      visible.exactCenterY().coerceIn(0f, (size.y - 1).toFloat()),
    )
  }

  /**
   * Taps the centre of the part of [bounds] on the screen. Returns whether the tap is sent; false
   * when [bounds] are off the screen, before Android 11 or when another feature holds the
   * pass-through lock.
   */
  @JvmStatic
  fun tap(service: TalkBackService, bounds: Rect): Boolean {
    if (!isSupported() || isLockedElsewhere(service)) return false
    val centre = visibleCentre(service, bounds) ?: return false
    val tap =
      TouchSegment(
        centre.x,
        centre.y,
        centre.x,
        centre.y,
        ViewConfiguration.getTapTimeout().toLong(),
        false,
      )
    return perform(service, listOf(tap), stopped = { false }) { performed ->
      if (!performed) LogUtils.w(TAG, "Tap not performed at %s", centre)
    }
  }

  /**
   * Performs [segments] as one touch, each segment dispatched when the one before ends, then calls
   * [done] with whether the touch was performed. When [stopped] turns true while the touch is down,
   * the touch is lifted where it is. Returns false, without calling [done], when the touch can't
   * start: before Android 11, with no pipeline or while another feature holds the pass-through lock.
   */
  internal fun perform(
    service: TalkBackService,
    segments: List<TouchSegment>,
    stopped: () -> Boolean,
    done: (Boolean) -> Unit,
  ): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || isLockedElsewhere(service)) return false
    val pipeline = service.feedbackReturner ?: return false
    if (segments.isEmpty()) {
      done(true)
      return true
    }
    if (holds == 0) {
      val size = ScreenUtils.getRealScreenSize(service)
      pipeline.returnFeedback(
        EVENT_ID_UNTRACKED,
        Feedback.passThroughMode(LOCK_PASS_THROUGH, Region(0, 0, size.x, size.y)),
      )
    }
    holds++
    var ended = false
    val end = { performed: Boolean ->
      if (!ended) {
        ended = true
        holds--
        if (holds == 0) {
          pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.passThroughMode(LOCK_PASS_THROUGH, null))
        }
        done(performed)
      }
    }
    // The stroke and segment that ended last, which the next one continues.
    var previous: GestureDescription.StrokeDescription? = null
    var previousSegment: TouchSegment? = null
    lateinit var dispatch: (Int) -> Unit
    dispatch = dispatch@{ i ->
      val stop = stopped()
      val last = previousSegment
      val segment =
        when {
          !stop -> segments[i]
          // Stopped before the touch went down.
          last == null -> null
          else -> last.lift()
        }
      if (segment == null) {
        end(false)
        return@dispatch
      }
      val continues = segment.willContinue && !stop
      val path =
        Path().apply {
          moveTo(segment.fromX, segment.fromY)
          if (segment.moves) lineTo(segment.toX, segment.toY)
        }
      try {
        val stroke =
          previous?.continueStroke(path, 0, segment.durationMs, continues)
            ?: GestureDescription.StrokeDescription(path, 0, segment.durationMs, continues)
        val callback =
          object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(description: GestureDescription) {
              previous = stroke
              previousSegment = segment
              when {
                stop -> end(false)
                continues -> dispatch(i + 1)
                else -> end(true)
              }
            }

            override fun onCancelled(description: GestureDescription) = end(false)
          }
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        if (!service.dispatchGesture(gesture, callback, main)) end(false)
      } catch (e: RuntimeException) {
        LogUtils.w(TAG, "Cannot touch: %s", e)
        end(false)
      }
    }
    // Android applies the pass-through region a moment later.
    main.postDelayed({ dispatch(0) }, PASS_THROUGH_WAIT_MS)
    return true
  }
}
