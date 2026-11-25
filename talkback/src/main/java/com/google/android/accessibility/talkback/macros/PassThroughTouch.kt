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
import android.os.SystemClock
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
 * which takes it as a touch of its own. Used by macro touches, touch-and-hold rules, Activate
 * with a tap and Hold a touch on the focused item. Called on the main thread.
 *
 * Touches running at the same time share the pass-through lock, which is released when the last of
 * them ends. A lock held by another feature, such as the braille keyboard, is left alone and no
 * touch is made.
 */
object PassThroughTouch {
  private const val TAG = "PassThroughTouch"

  /** How long Android takes to apply the pass-through region. */
  private const val PASS_THROUGH_WAIT_MS = 350L

  /** The length of each part of a touch held by [hold]. */
  const val HOLD_STEP_MS = 250L

  /**
   * How far a part of a touch that stays in place moves while the touch continues: to a point this
   * far beside it, then back. Far less than the touch slop, so apps take it as a touch held still.
   */
  const val NUDGE_PX = 1f

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
    if (service.feedbackReturner == null) return false
    if (segments.isEmpty()) {
      done(true)
      return true
    }
    return performParts(service, segments::getOrNull, stopped, done)
  }

  /**
   * Touches [point] and holds the touch down until [stopped] turns true, then lifts it and calls
   * [done]. The touch is dispatched in parts of [HOLD_STEP_MS], each moving [NUDGE_PX] aside or
   * back, so it lifts at most that long after [stopped] turns true. Returns false, without calling [done], when the touch can't start, as
   * [perform] does.
   */
  internal fun hold(
    service: TalkBackService,
    point: PointF,
    stopped: () -> Boolean,
    done: (Boolean) -> Unit,
  ): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || isLockedElsewhere(service)) return false
    if (service.feedbackReturner == null) return false
    val part = TouchSegment(point.x, point.y, point.x, point.y, HOLD_STEP_MS, true)
    return performParts(service, { part }, stopped, done)
  }

  /** How a touch ended, as logged. */
  private enum class Ending {
    /** Every part was performed and the touch lifted. */
    COMPLETED,
    /** Stopped by the caller; the touch lifted where it was. */
    STOPPED,
    /** Android cancelled a part: another gesture, a real touch or touch handling changing. */
    CANCELLED,
    /** Android didn't take a part, or there was none. */
    NOT_DISPATCHED,
  }

  /**
   * The part [segment] as it is dispatched, after [previous], the part dispatched before it, or
   * first when null. It starts where [previous] ended, as a continued stroke has to. A part that
   * continues without moving goes [NUDGE_PX] to a point beside it, or back from there: Android sends
   * no event for a part of a continued touch that doesn't move. The first such part would then be
   * reported done as soon as the touch goes down, and the next, with no event at all, cancelled,
   * while the app keeps the touch down. A part that lifts without moving lifts where the touch is.
   * [maxX] is the largest x on the screen.
   */
  internal fun dispatchedPart(segment: TouchSegment, previous: TouchSegment?, maxX: Float): TouchSegment {
    val fromX = previous?.toX ?: segment.fromX
    val fromY = previous?.toY ?: segment.fromY
    return when {
      segment.moves -> segment.copy(fromX = fromX, fromY = fromY)
      !segment.willContinue -> segment.copy(fromX = fromX, fromY = fromY, toX = fromX, toY = fromY)
      fromX == segment.fromX && fromY == segment.fromY -> {
        val besideX =
          if (segment.fromX + NUDGE_PX <= maxX) segment.fromX + NUDGE_PX
          else segment.fromX - NUDGE_PX
        segment.copy(toX = besideX)
      }
      else -> segment.copy(fromX = fromX, fromY = fromY)
    }
  }

  /**
   * Performs the touch whose parts [part] gives by index, each dispatched when the one before ends,
   * until a part doesn't continue, [part] gives null or [stopped] turns true; then lifts the touch
   * where it is and calls [done]. Needs Android 11 and the pipeline, which the callers check.
   */
  private fun performParts(
    service: TalkBackService,
    part: (Int) -> TouchSegment?,
    stopped: () -> Boolean,
    done: (Boolean) -> Unit,
  ): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
    val pipeline = service.feedbackReturner ?: return false
    val size = ScreenUtils.getRealScreenSize(service)
    val maxX = (size.x - 1).toFloat()
    if (holds == 0) {
      pipeline.returnFeedback(
        EVENT_ID_UNTRACKED,
        Feedback.passThroughMode(LOCK_PASS_THROUGH, Region(0, 0, size.x, size.y)),
      )
    }
    holds++
    var ended = false
    // When the touch went down, in uptime, or 0 before it did.
    var downTime = 0L
    // The parts dispatched so far.
    var parts = 0
    val end = { ending: Ending ->
      if (!ended) {
        ended = true
        holds--
        if (holds == 0) {
          pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.passThroughMode(LOCK_PASS_THROUGH, null))
        }
        LogUtils.w(
          TAG,
          "Touch ended: %s, after %d parts, %d ms after it went down",
          ending,
          parts,
          if (downTime == 0L) 0L else SystemClock.uptimeMillis() - downTime,
        )
        done(ending == Ending.COMPLETED)
      }
    }
    // The stroke and part, as dispatched, that ended last, which the next one continues.
    var previous: GestureDescription.StrokeDescription? = null
    var previousPart: TouchSegment? = null
    lateinit var dispatch: (Int) -> Unit
    dispatch = dispatch@{ i ->
      val stop = stopped()
      val last = previousPart
      val segment =
        when {
          // No part left: the touch lifts where it is.
          !stop -> part(i) ?: last?.lift()
          // Stopped before the touch went down.
          last == null -> null
          else -> last.lift()
        }
      if (segment == null) {
        // Nothing was performed.
        end(if (stop) Ending.STOPPED else Ending.NOT_DISPATCHED)
        return@dispatch
      }
      val continues = segment.willContinue && !stop
      val dispatched = dispatchedPart(segment.copy(willContinue = continues), last, maxX)
      val path =
        Path().apply {
          moveTo(dispatched.fromX, dispatched.fromY)
          if (dispatched.moves) lineTo(dispatched.toX, dispatched.toY)
        }
      try {
        val stroke =
          previous?.continueStroke(path, 0, dispatched.durationMs, continues)
            ?: GestureDescription.StrokeDescription(path, 0, dispatched.durationMs, continues)
        val callback =
          object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(description: GestureDescription) {
              previous = stroke
              previousPart = dispatched
              when {
                stop -> end(Ending.STOPPED)
                continues -> dispatch(i + 1)
                else -> end(Ending.COMPLETED)
              }
            }

            override fun onCancelled(description: GestureDescription) {
              LogUtils.w(TAG, "Touch part %d cancelled", i)
              end(Ending.CANCELLED)
            }
          }
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        if (!service.dispatchGesture(gesture, callback, main)) {
          end(Ending.NOT_DISPATCHED)
          return@dispatch
        }
        parts++
        if (i == 0) {
          downTime = SystemClock.uptimeMillis()
          LogUtils.w(TAG, "Touch going down at %.1f, %.1f", dispatched.fromX, dispatched.fromY)
        }
        if (!continues) {
          LogUtils.w(
            TAG,
            "Touch lifting at %.1f, %.1f in %d ms: %s",
            dispatched.toX,
            dispatched.toY,
            dispatched.durationMs,
            if (stop) "stopped" else "last part",
          )
        }
      } catch (e: RuntimeException) {
        LogUtils.w(TAG, "Cannot touch: %s", e)
        end(Ending.NOT_DISPATCHED)
      }
    }
    // Android applies the pass-through region a moment later.
    main.postDelayed({ dispatch(0) }, PASS_THROUGH_WAIT_MS)
    return true
  }
}
