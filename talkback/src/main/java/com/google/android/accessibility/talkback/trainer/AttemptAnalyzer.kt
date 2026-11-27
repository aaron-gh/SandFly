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

package com.google.android.accessibility.talkback.trainer

import com.google.android.accessibility.talkback.gesture.BackTapDetector
import com.google.android.accessibility.talkback.gesture.CornerTapTracker.Corner
import com.google.android.accessibility.talkback.gesture.EdgeSwipeTracker
import com.google.android.accessibility.talkback.gesture.EdgeSwipeTracker.Edge
import com.google.android.accessibility.talkback.gesture.EdgeSwipeTracker.MenuEdge
import com.google.android.accessibility.talkback.gesture.TopAndBottomTap
import com.google.android.accessibility.talkback.gesture.ShakeDetector
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.hypot

enum class Rating {
  PRECISE,
  GOOD,
  BORDERLINE,
  FAILED,
}

enum class Severity {
  /** Past a limit: the likely reason the gesture wasn't recognized. */
  PROBLEM,
  /** Within the limit, but close to it. */
  CLOSE,
}

/** Something the fingers did, compared with what SandFly allows. */
enum class FindingKind {
  WRONG_FINGER_COUNT,
  FINGERS_LANDED_APART,
  FINGER_LIFTED_EARLY,
  FINGERS_LIFTED_APART,
  SLOW_START,
  /** The touch became touch exploration, so its motion events stopped before it lifted. */
  BECAME_EXPLORATION,
  PAUSED,
  TOO_SHORT,
  WRONG_DIRECTION,
  OFF_LINE,
  CURVED,
  TURN_MISSING,
  SECOND_PART_WRONG,
  TAP_MOVED,
  TAP_HELD_LONG,
  HOLD_TOO_SHORT,
  TOO_FEW_TAPS,
  TOO_MANY_TAPS,
  TAPS_TOO_SLOW,
  TAPS_APART,
  PINCH_TOO_SMALL,
  PINCH_WRONG_WAY,
  MOVED_TOGETHER,
  ROTATE_TOO_SMALL,
  ROTATE_WRONG_WAY,
  SCRUB_TOO_SHORT,
  SCRUB_NO_RETURN,
  SCRUB_TOO_SLOW,
  EDGE_START_TOO_FAR,
  EDGE_WRONG_HALF,
  EDGE_TOO_SHORT,
  EDGE_TOO_LONG,
  /** A slide along an edge that went away from it. */
  ALONG_OFF_EDGE,
  EDGE_TOO_SLOW,
  EDGE_SIDEWAYS,
  CORNER_MISSED,
  CORNER_TAP_SLOW,
  MENU_IN_CORNER,
  MENU_LEFT_EDGE,
  /** A menu across the screen that the finger left by moving up or down from where it opened. */
  MENU_LEFT_LINE,
  MENU_WRONG_ITEM,
  SWIPE_HOLD_TOO_SHORT,
  TAP_WRONG_HALF,
  SWIPE_MISSING,
  SWIPE_LATE,
  BACK_TAP_TOO_LIGHT,
  BACK_TAP_SIDEWAYS,
  BACK_TAP_MOVING,
  BACK_TAP_SCREEN_TOUCHED,
  BACK_TAPS_TOO_SLOW,
  TURNED_IN_PINCH,
  WRONG_EDGE,
  MENU_OPENED_INSTEAD,
  /** The fingers of a top and bottom tap were not one in the top third and one in the bottom. */
  TAP_NOT_TOP_AND_BOTTOM,
  /** The second swipe of a two-stroke swipe never came. */
  SECOND_STROKE_MISSING,
  /** The second swipe of a two-stroke swipe started too long after the first lifted. */
  SECOND_STROKE_LATE,
  /** The swipe of one stroke of a two-stroke swipe went the wrong way; value is the stroke, 1 or 2. */
  STROKE_WRONG_DIRECTION,
  SHAKE_TOO_LIGHT,
  SHAKE_TOO_FEW_SWINGS,
}

/** A side of the screen. */
enum class ScreenSide {
  LEFT,
  RIGHT,
  TOP,
  BOTTOM,
}

/** The side of the screen the slides of this edge start at. */
val Edge.side: ScreenSide
  get() =
    when (this) {
      Edge.BOTTOM, Edge.ALONG_BOTTOM_LEFT, Edge.ALONG_BOTTOM_RIGHT -> ScreenSide.BOTTOM
      Edge.TOP_LEFT, Edge.TOP_RIGHT, Edge.ALONG_TOP_LEFT, Edge.ALONG_TOP_RIGHT -> ScreenSide.TOP
      Edge.LEFT_TOP, Edge.LEFT_BOTTOM, Edge.ALONG_LEFT_UP, Edge.ALONG_LEFT_DOWN -> ScreenSide.LEFT
      Edge.RIGHT_TOP, Edge.RIGHT_BOTTOM, Edge.ALONG_RIGHT_UP, Edge.ALONG_RIGHT_DOWN -> ScreenSide.RIGHT
    }

/**
 * A finding with its measured [value] and the [limit] it is compared with, in ms, mm, degrees or a
 * count, as [kind] has it. [direction] is the direction the fingers went, for wrong directions.
 */
class Finding(
  val kind: FindingKind,
  val severity: Severity,
  val value: Float = 0f,
  val limit: Float = 0f,
  val direction: Direction? = null,
  /** The side the finger started at, for a wrong edge; [expectedSide] is the exercise's. */
  val side: ScreenSide? = null,
  val expectedSide: ScreenSide? = null,
)

enum class MeasureKind {
  DURATION_MS,
  LENGTH_MM,
  SPEED_CM_PER_S,
  ANGLE_OFF_LINE_DEGREES,
  START_MS,
  LANDING_SPREAD_MS,
  TAP_MS,
  GAP_MS,
  SPREAD_CHANGE_MM,
  ROTATION_DEGREES,
  CORNER_DISTANCE_MM,
  EDGE_DISTANCE_MM,
  MENU_ITEM,
  HOLD_MS,
  BACK_TAP_STRENGTH,
  SHAKE_STRENGTH,
  FIRST_STROKE_MS,
  /** From the first swipe of a two-stroke swipe lifting to the second starting. */
  STROKE_GAP_MS,
  SECOND_STROKE_MS,
  /** From the first finger landing to the last lifting, over all touches of the gesture. */
  WHOLE_GESTURE_MS,
}

class Measure(val kind: MeasureKind, val value: Float)

class AttemptReport(
  val success: Boolean,
  /** The gestures SandFly recognized, in order. */
  val recognized: List<Int>,
  /** The edge menu item the finger lifted at, [NO_MENU_ITEM], or null if no menu opened. */
  val menuChoice: Int?,
  val rating: Rating,
  val findings: List<Finding>,
  val measures: List<Measure>,
) {
  val problems: List<Finding>
    get() = findings.filter { it.severity == Severity.PROBLEM }
}

/**
 * Measures an attempt at an exercise against the limits that SandFly's gesture matchers and
 * trackers use, to say how close it came and what to change.
 */
class AttemptAnalyzer(private val limits: TrainerLimits) {

  fun analyze(
    exercise: Exercise,
    touches: List<Touch>,
    recognized: List<Int>,
    menuChoice: Int? = null,
  ): AttemptReport {
    val check = Checks(limits)
    val usable = touches.filter { touch -> touch.fingers.isNotEmpty() && touch.fingers.all { it.upMs >= 0 } }
    if (usable.isNotEmpty()) {
      when (val shape = exercise.shape) {
        is Shape.Swipe -> swipe(check, shape, usable)
        is Shape.Taps -> taps(check, shape, usable)
        is Shape.Pinch -> pinch(check, shape, usable)
        is Shape.Rotate -> rotate(check, shape, usable)
        Shape.Scrub -> scrub(check, usable)
        is Shape.EdgeSlide -> edgeSlide(check, shape, usable)
        is Shape.CornerTaps -> cornerTaps(check, shape, usable)
        is Shape.EdgeMenu -> edgeMenu(check, shape, usable)
        is Shape.TapSwipe -> tapSwipe(check, shape, usable)
        // Back taps are measured from the accelerometer, in analyzeBackTaps.
        is Shape.BackTaps -> {}
        Shape.Shake -> {}
        is Shape.TwoStroke -> twoStroke(check, shape, usable)
      }
    }
    val shape = exercise.shape
    if (shape !is Shape.EdgeMenu && menuChoice != null) {
      check.problem(FindingKind.MENU_OPENED_INSTEAD)
    }
    val success =
      if (shape is Shape.EdgeMenu) menuChoice == shape.item else exercise.gestureId in recognized
    val rating = check.rating(success)
    // A recognized gesture passed every limit that matters, whatever the estimate says.
    val findings =
      if (success) check.findings.filter { it.severity == Severity.CLOSE } else check.findings
    return AttemptReport(success, recognized, menuChoice, rating, findings, check.measures)
  }

  // Swipes

  /**
   * Checks a swipe. [oneFingerMatcher] says whether the one-finger swipe matcher recognizes it, with
   * its time limits and its turns; otherwise the multi-finger one does, as after taps.
   */
  private fun swipe(
    check: Checks,
    shape: Shape.Swipe,
    touches: List<Touch>,
    oneFingerMatcher: Boolean = shape.fingers == 1,
  ) {
    val touch = touches.maxBy { touch -> touch.fingers.maxOf { it.farthestFromStart() } }
    checkFingers(check, touch, shape.fingers)
    val cm = limits.pxPerCm
    val main = if (shape.hold) movingPart(check, touch.fingers.first()) else touch.fingers.first()
    // A swipe that turns is as long as the way the finger went, not the gap between its ends.
    val lengthPx =
      touch.fingers
        .map { if (shape.directions.size == 1) it.start.distanceTo(it.end) else pathLength(it) }
        .average()
        .toFloat()
    if (touch.becameExploration) {
      // The rest of its way, and when exploration took over, were never seen.
      check.problem(FindingKind.BECAME_EXPLORATION)
      return
    }
    check.measure(MeasureKind.DURATION_MS, touch.durationMs.toFloat())
    check.measure(MeasureKind.LENGTH_MM, limits.toMm(lengthPx))
    // The hold at the end is no part of the speed.
    val movingMs = if (shape.hold) main.end.timeMs - main.downMs else touch.durationMs
    if (movingMs > 0) {
      check.measure(MeasureKind.SPEED_CM_PER_S, lengthPx / cm / (movingMs / 1000f))
    }
    val firstCm = timeToTravel(main, cm)
    if (firstCm == null) {
      check.atLeast(FindingKind.TOO_SHORT, limits.toMm(main.farthestFromStart()), 10f)
      return
    }
    check.measure(MeasureKind.START_MS, firstCm.toFloat())
    if (oneFingerMatcher) {
      check.atMost(FindingKind.SLOW_START, firstCm.toFloat(), limits.swipeStartMs.toFloat())
      check.atMost(FindingKind.PAUSED, longestPause(main, cm).toFloat(), limits.swipeContinueMs.toFloat())
    }
    val slop = limits.touchSlopPx * shape.fingers
    for (finger in touch.fingers) {
      val started = finger.points.firstOrNull { it.distanceTo(finger.start) >= slop } ?: continue
      val startDirection =
        Direction.of(started.x - finger.start.x, started.y - finger.start.y)
      if (startDirection != shape.directions[0]) {
        check.problem(FindingKind.WRONG_DIRECTION, direction = startDirection)
        return
      }
    }
    if (shape.directions.size == 1) {
      check.measure(
        MeasureKind.ANGLE_OFF_LINE_DEGREES,
        angleOff(main.end.x - main.start.x, main.end.y - main.start.y, shape.directions[0]),
      )
    }
    val samples = (listOf(main) + touch.fingers.drop(1)).map { resample(it) }
    if (!oneFingerMatcher && shape.directions.size == 1) {
      // Each finger's every 2.5 mm must go the swipe's way.
      val worst =
        samples.maxOf { points ->
          points.zipWithNext().maxOfOrNull { (a, b) -> angleOff(b.x - a.x, b.y - a.y, shape.directions[0]) } ?: 0f
        }
      check.atMost(FindingKind.OFF_LINE, worst, MAX_ANGLE_DEGREES)
      return
    }
    for (points in samples) {
      if (points.size < 2) continue
      val parts = segments(points)
      val expected = shape.directions.size
      when {
        parts.size - 1 < expected -> check.problem(FindingKind.TURN_MISSING)
        parts.size - 1 > expected ->
          check.problem(FindingKind.CURVED, (parts.size - 1).toFloat(), expected.toFloat())
        else ->
          for (i in 0 until expected) {
            val a = parts[i]
            val b = parts[i + 1]
            val went = Direction.of(b.x - a.x, b.y - a.y)
            if (went != shape.directions[i]) {
              check.problem(
                if (i == 0) FindingKind.WRONG_DIRECTION else FindingKind.SECOND_PART_WRONG,
                direction = went,
              )
            }
          }
      }
      if (shape.directions.size == 1) {
        val off = angleOff(points.last().x - points.first().x, points.last().y - points.first().y, shape.directions[0])
        check.atMost(FindingKind.OFF_LINE, off, MAX_ANGLE_DEGREES)
      }
    }
  }

  private fun pathLength(finger: FingerPath): Float =
    finger.points.zipWithNext().sumOf { (a, b) -> a.distanceTo(b).toDouble() }.toFloat()

  /**
   * The part of [finger]'s path before it came to rest for the hold, which is checked against the
   * hold time. The rest starts where the finger last moved beyond the touch slop of where it lifts.
   */
  private fun movingPart(check: Checks, finger: FingerPath): FingerPath {
    val points = finger.points
    val end = finger.end
    val lastMove = points.indexOfLast { it.distanceTo(end) >= limits.touchSlopPx }
    val restStart = points[(lastMove + 1).coerceAtMost(points.size - 1)]
    val heldMs = (finger.upMs - restStart.timeMs).toFloat()
    check.measure(MeasureKind.HOLD_MS, heldMs)
    check.atLeast(FindingKind.SWIPE_HOLD_TOO_SHORT, heldMs, limits.swipeHoldMs.toFloat())
    val moving = FingerPath(finger.pointerId)
    moving.points += points.subList(0, (lastMove + 2).coerceIn(1, points.size))
    moving.upMs = moving.end.timeMs
    return moving
  }

  private fun checkFingers(check: Checks, touch: Touch, fingers: Int) {
    if (touch.mostFingersAtOnce != fingers) {
      check.problem(FindingKind.WRONG_FINGER_COUNT, touch.mostFingersAtOnce.toFloat(), fingers.toFloat())
    }
    if (fingers > 1) {
      val downs = touch.fingers.map { it.downMs }.sorted()
      val gap = downs.zipWithNext().maxOfOrNull { (a, b) -> b - a } ?: 0L
      check.measure(MeasureKind.LANDING_SPREAD_MS, touch.landingSpreadMs.toFloat())
      check.atMost(FindingKind.FINGERS_LANDED_APART, gap.toFloat(), (fingers * limits.tapTimeoutMs).toFloat())
      if (touch.liftedBeforeAllLanded) check.problem(FindingKind.FINGER_LIFTED_EARLY)
    }
  }

  /** The time [finger] took to get [distancePx] from where it landed, or null if it never did. */
  private fun timeToTravel(finger: FingerPath, distancePx: Float): Long? =
    finger.points.firstOrNull { it.distanceTo(finger.start) > distancePx }?.let { it.timeMs - finger.downMs }

  /** The longest time the swipe took to go a further centimetre, after its first, before lifting. */
  private fun longestPause(finger: FingerPath, cmPx: Float): Long {
    val points = finger.points
    val startIndex = points.indexOfFirst { it.distanceTo(finger.start) > cmPx }
    if (startIndex < 0) return 0
    var base = points[startIndex]
    var longest = 0L
    // The lift ends the swipe, so the time before it is no pause.
    for (point in points.subList(startIndex + 1, maxOf(startIndex + 1, points.size - 1))) {
      if (point.distanceTo(base) > cmPx) {
        base = point
      } else {
        longest = maxOf(longest, point.timeMs - base.timeMs)
      }
    }
    return longest
  }

  /** The finger's points, at least 2.5 mm apart along one axis, as the swipe matchers sample them. */
  private fun resample(finger: FingerPath): List<TouchPoint> {
    val min = limits.pxPerCm * SAMPLE_CM
    val kept = arrayListOf(finger.start)
    for (point in finger.points.drop(1)) {
      val last = kept.last()
      if (abs(point.x - last.x) >= min || abs(point.y - last.y) >= min) kept += point
    }
    return kept
  }

  // Taps

  private fun taps(check: Checks, shape: Shape.Taps, touches: List<Touch>) {
    tapCount(check, touches.size, shape.taps)
    val fingers = shape.fingers
    touches.forEachIndexed { index, touch ->
      checkFingers(check, touch, fingers)
      if (fingers > 1) {
        val ups = touch.fingers.map { it.upMs }
        check.atMost(
          FindingKind.FINGERS_LIFTED_APART,
          (ups.max() - ups.min()).toFloat(),
          (fingers * limits.tapTimeoutMs).toFloat(),
        )
      }
      val moved = touch.fingers.maxOf { it.farthestFromStart() }
      check.atMost(FindingKind.TAP_MOVED, limits.toMm(moved), limits.toMm(limits.touchSlopPx * fingers))
      val held = touch.durationMs.toFloat()
      if (shape.hold && index == shape.taps - 1) {
        check.atLeast(FindingKind.HOLD_TOO_SHORT, held, limits.holdMs(shape).toFloat())
      } else {
        check.measure(MeasureKind.TAP_MS, held)
        check.atMost(FindingKind.TAP_HELD_LONG, held, limits.longPressMs.toFloat())
      }
    }
    val gapLimit = if (fingers == 1) limits.multiTapGapMs else limits.multiFingerTapGapMs
    tapGaps(check, touches, gapLimit, limits.doubleTapSlopPx * fingers)
    shape.half?.let { half -> checkHalf(check, touches.last(), half) }
  }

  /** Checks that the fingers lifted in [half] of the screen, as SandFly takes their middle there. */
  private fun checkHalf(check: Checks, touch: Touch, half: ScreenHalf) {
    if (half == ScreenHalf.TOP_AND_BOTTOM) {
      // SandFly takes where the fingers of the last tap went down, along the device held upright.
      val ys = touch.fingers.map { TopAndBottomTap.alongDevice(it.start.x, it.start.y, limits.rotation) }
      val length = TopAndBottomTap.deviceLength(limits.screenWidth, limits.screenHeight, limits.rotation)
      val topAndBottom = ys.size == 2 && TopAndBottomTap.isTopAndBottom(ys[0], ys[1], length)
      if (!topAndBottom) check.problem(FindingKind.TAP_NOT_TOP_AND_BOTTOM)
      return
    }
    val middleY = touch.fingers.map { it.end.y }.average().toFloat()
    val inTop = middleY < limits.screenHeight / 2f
    if (inTop != (half == ScreenHalf.TOP)) check.problem(FindingKind.TAP_WRONG_HALF)
  }

  // Two-stroke swipes

  /** Checks each swipe of a two-stroke swipe, and the time between them. */
  private fun twoStroke(check: Checks, shape: Shape.TwoStroke, touches: List<Touch>) {
    val strokes = touches.take(2)
    strokes.forEachIndexed { i, touch ->
      val wanted = if (i == 0) shape.first else shape.second
      val strokeCheck = Checks(limits)
      swipe(strokeCheck, Shape.Swipe(1, listOf(wanted)), listOf(touch))
      // Each stroke's own measures would read as the whole gesture's: the trainer shows one of a kind.
      check.absorb(strokeCheck, withMeasures = false) { finding ->
        if (finding.kind == FindingKind.WRONG_DIRECTION) {
          Finding(FindingKind.STROKE_WRONG_DIRECTION, finding.severity, (i + 1).toFloat(), direction = finding.direction)
        } else {
          finding
        }
      }
    }
    check.measure(MeasureKind.FIRST_STROKE_MS, strokes[0].durationMs.toFloat())
    if (strokes.size < 2) {
      check.problem(FindingKind.SECOND_STROKE_MISSING)
      return
    }
    val gap = (strokes[1].downMs - strokes[0].upMs).toFloat()
    check.measure(MeasureKind.STROKE_GAP_MS, gap)
    check.measure(MeasureKind.SECOND_STROKE_MS, strokes[1].durationMs.toFloat())
    check.measure(MeasureKind.WHOLE_GESTURE_MS, (strokes[1].upMs - strokes[0].downMs).toFloat())
    check.atMost(FindingKind.SECOND_STROKE_LATE, gap, limits.twoStrokeMs.toFloat())
  }

  // Taps, then a swipe

  private fun tapSwipe(check: Checks, shape: Shape.TapSwipe, touches: List<Touch>) {
    val swipeAt =
      touches.indexOfFirst { touch ->
        limits.toMm(touch.fingers.maxOf { it.farthestFromStart() }) >= TAP_SWIPE_MOVED_MM
      }
    if (swipeAt < 0) {
      taps(check, Shape.Taps(shape.fingers, shape.taps, hold = false), touches)
      check.problem(FindingKind.SWIPE_MISSING)
      return
    }
    val taps = touches.subList(0, swipeAt)
    val swipe = touches[swipeAt]
    taps(check, Shape.Taps(shape.fingers, shape.taps, hold = false), taps)
    taps.lastOrNull()?.let { lastTap ->
      check.atMost(
        FindingKind.SWIPE_LATE,
        (swipe.downMs - lastTap.upMs).toFloat(),
        limits.multiFingerTapGapMs.toFloat(),
      )
    }
    swipe(
      check,
      Shape.Swipe(shape.swipeFingers, listOf(shape.direction)),
      listOf(swipe),
      oneFingerMatcher = false,
    )
  }

  private fun tapCount(check: Checks, made: Int, wanted: Int) {
    if (made < wanted) check.problem(FindingKind.TOO_FEW_TAPS, made.toFloat(), wanted.toFloat())
    if (made > wanted) check.problem(FindingKind.TOO_MANY_TAPS, made.toFloat(), wanted.toFloat())
  }

  private fun tapGaps(check: Checks, touches: List<Touch>, gapLimitMs: Long, slopPx: Float) {
    for ((before, after) in touches.zipWithNext()) {
      val gap = (after.downMs - before.upMs).toFloat()
      check.measure(MeasureKind.GAP_MS, gap)
      check.atMost(FindingKind.TAPS_TOO_SLOW, gap, gapLimitMs.toFloat())
      val apart = after.fingers.first().start.distanceTo(before.fingers.first().start)
      check.atMost(FindingKind.TAPS_APART, limits.toMm(apart), limits.toMm(slopPx))
    }
  }

  // Pinch, rotation and scrub

  private fun pinch(check: Checks, shape: Shape.Pinch, touches: List<Touch>) {
    val touch = touches.first()
    checkFingers(check, touch, shape.fingers)
    val (start, end) = fingersAtStartAndEnd(touch) ?: return
    val startSpread = spread(start)
    val endSpread = spread(end)
    val change = endSpread - startSpread
    check.measure(MeasureKind.SPREAD_CHANGE_MM, limits.toMm(change))
    check.measure(MeasureKind.DURATION_MS, touch.durationMs.toFloat())
    val wanted = if (shape.spread) change else -change
    if (wanted < 0 && limits.toMm(-wanted) >= MIN_PINCH_MM) {
      check.problem(FindingKind.PINCH_WRONG_WAY)
      return
    }
    check.atLeast(FindingKind.PINCH_TOO_SMALL, limits.toMm(maxOf(wanted, 0f)), MIN_PINCH_MM)
    val moved = middle(start).distanceTo(middle(end))
    if (moved > abs(change)) {
      check.problem(FindingKind.MOVED_TOGETHER, limits.toMm(moved), limits.toMm(abs(change)))
    }
    // Two fingers that turn around each other first make a rotation.
    if (shape.fingers == 2 && touch.fingers.size >= 2) {
      val turned = abs(turnDegrees(touch.fingers[0], touch.fingers[1]))
      check.measure(MeasureKind.ROTATION_DEGREES, turned)
      check.atMost(FindingKind.TURNED_IN_PINCH, turned, MIN_ROTATION_DEGREES)
    }
  }

  private fun rotate(check: Checks, shape: Shape.Rotate, touches: List<Touch>) {
    val touch = touches.first()
    checkFingers(check, touch, 2)
    if (touch.fingers.size < 2) return
    val degrees = turnDegrees(touch.fingers[0], touch.fingers[1])
    check.measure(MeasureKind.ROTATION_DEGREES, abs(degrees))
    check.measure(MeasureKind.DURATION_MS, touch.durationMs.toFloat())
    val wanted = if (shape.clockwise) degrees else -degrees
    if (wanted < 0 && -wanted >= MIN_ROTATION_DEGREES) {
      check.problem(FindingKind.ROTATE_WRONG_WAY)
      return
    }
    check.atLeast(FindingKind.ROTATE_TOO_SMALL, maxOf(wanted, 0f), MIN_ROTATION_DEGREES)
  }

  /** How far the line from [a] to [b] turned, in degrees; clockwise on the screen is positive. */
  private fun turnDegrees(a: FingerPath, b: FingerPath): Float {
    var total = 0.0
    var last: Double? = null
    var j = 0
    for (pa in a.points) {
      while (j + 1 < b.points.size && b.points[j + 1].timeMs <= pa.timeMs) j++
      val pb = b.points[j]
      if (pb.timeMs < b.downMs || pa.timeMs < b.downMs) continue
      val angle = Math.toDegrees(atan2((pb.y - pa.y).toDouble(), (pb.x - pa.x).toDouble()))
      last?.let {
        var delta = angle - it
        while (delta > 180) delta -= 360
        while (delta < -180) delta += 360
        total += delta
      }
      last = angle
    }
    return total.toFloat()
  }

  private fun scrub(check: Checks, touches: List<Touch>) {
    val touch = touches.first()
    checkFingers(check, touch, 2)
    if (touch.fingers.size < 2) return
    val a = touch.fingers[0]
    val b = touch.fingers[1]
    val startMs = b.downMs
    val xs =
      a.points.filter { it.timeMs >= startMs }.map { pa ->
        val pb = b.points.lastOrNull { it.timeMs <= pa.timeMs } ?: b.start
        TouchPoint(pa.timeMs, (pa.x + pb.x) / 2f, (pa.y + pb.y) / 2f)
      }
    if (xs.size < 2) return
    val x0 = xs.first().x
    val peak = xs.maxBy { abs(it.x - x0) }
    val stroke = abs(peak.x - x0)
    val afterPeak = xs.subList(xs.indexOf(peak), xs.size)
    val back = afterPeak.maxOf { abs(peak.x - it.x) }
    check.measure(MeasureKind.LENGTH_MM, limits.toMm(stroke))
    check.measure(MeasureKind.DURATION_MS, touch.durationMs.toFloat())
    check.atLeast(FindingKind.SCRUB_TOO_SHORT, limits.toMm(stroke), SCRUB_STROKE_MM)
    check.atLeast(FindingKind.SCRUB_NO_RETURN, limits.toMm(back), SCRUB_STROKE_MM * SCRUB_RETURN_FRACTION)
    val moving = xs.firstOrNull { abs(it.x - x0) >= limits.pxPerCm * SCRUB_STROKE_MM / 10f * 0.25f }
    val returned = afterPeak.firstOrNull { abs(peak.x - it.x) >= limits.pxPerCm * SCRUB_STROKE_MM / 10f * SCRUB_RETURN_FRACTION }
    if (moving != null && returned != null) {
      check.atMost(FindingKind.SCRUB_TOO_SLOW, (returned.timeMs - moving.timeMs).toFloat(), SCRUB_MAX_MS)
    }
  }

  /** Where the fingers were once all had landed, and just before the first lifted. */
  private fun fingersAtStartAndEnd(touch: Touch): Pair<List<TouchPoint>, List<TouchPoint>>? {
    if (touch.fingers.size < 2) return null
    val allDown = touch.fingers.maxOf { it.downMs }
    val firstUp = touch.fingers.minOf { it.upMs }
    val start = touch.fingers.map { f -> f.points.lastOrNull { it.timeMs <= allDown } ?: f.start }
    val end = touch.fingers.map { f -> f.points.lastOrNull { it.timeMs <= firstUp } ?: f.end }
    return start to end
  }

  private fun middle(points: List<TouchPoint>) =
    TouchPoint(0, points.map { it.x }.average().toFloat(), points.map { it.y }.average().toFloat())

  private fun spread(points: List<TouchPoint>): Float {
    val m = middle(points)
    return points.map { it.distanceTo(m) }.average().toFloat()
  }

  // Edges and corners

  private fun edgeSlide(check: Checks, shape: Shape.EdgeSlide, touches: List<Touch>) {
    val touch = touches.maxBy { touch -> touch.fingers.first().farthestFromStart() }
    checkFingers(check, touch, 1)
    if (touch.becameExploration) {
      // The rest of its way was never seen, so its length would be wrong.
      check.problem(FindingKind.BECAME_EXPLORATION)
      return
    }
    val finger = touch.fingers.first()
    val start = finger.start
    val w = limits.screenWidth.toFloat()
    val h = limits.screenHeight.toFloat()
    check.measure(MeasureKind.DURATION_MS, touch.durationMs.toFloat())
    if (startedAtOtherSide(check, start, shape.edge.side)) return
    val fromEdge =
      when (shape.edge) {
        Edge.BOTTOM, Edge.ALONG_BOTTOM_LEFT, Edge.ALONG_BOTTOM_RIGHT -> h - start.y
        Edge.TOP_LEFT, Edge.TOP_RIGHT, Edge.ALONG_TOP_LEFT, Edge.ALONG_TOP_RIGHT -> start.y
        Edge.LEFT_TOP, Edge.LEFT_BOTTOM, Edge.ALONG_LEFT_UP, Edge.ALONG_LEFT_DOWN -> start.x
        Edge.RIGHT_TOP, Edge.RIGHT_BOTTOM, Edge.ALONG_RIGHT_UP, Edge.ALONG_RIGHT_DOWN -> w - start.x
      }
    check.measure(MeasureKind.EDGE_DISTANCE_MM, limits.toMm(maxOf(fromEdge, 0f)))
    check.atMost(FindingKind.EDGE_START_TOO_FAR, limits.toMm(maxOf(fromEdge, 0f)), limits.toMm(limits.edgeZonePx))
    val rightHalf =
      when (shape.edge) {
        Edge.TOP_LEFT -> start.x < w / 2
        Edge.TOP_RIGHT -> start.x >= w / 2
        Edge.LEFT_TOP, Edge.RIGHT_TOP -> start.y < h / 2
        Edge.LEFT_BOTTOM, Edge.RIGHT_BOTTOM -> start.y >= h / 2
        else -> true
      }
    if (!rightHalf) check.problem(FindingKind.EDGE_WRONG_HALF)
    val direction = edgeDirection(shape.edge)
    fun forward(p: TouchPoint) = (p.x - start.x) * direction.dx + (p.y - start.y) * direction.dy
    fun sideways(p: TouchPoint) = abs((p.x - start.x) * direction.dy - (p.y - start.y) * direction.dx)
    val endForward = forward(finger.end)
    check.measure(MeasureKind.LENGTH_MM, limits.toMm(maxOf(endForward, 0f)))
    val stage = if (shape.long) limits.edgeSecondStagePx else limits.edgeFirstStagePx
    check.atLeast(FindingKind.EDGE_TOO_SHORT, limits.toMm(maxOf(endForward, 0f)), limits.toMm(stage))
    if (!shape.long && !shape.edge.along && endForward >= limits.edgeSecondStagePx) {
      check.problem(FindingKind.EDGE_TOO_LONG, limits.toMm(endForward), limits.toMm(limits.edgeSecondStagePx))
    }
    val reached = finger.points.firstOrNull { forward(it) >= stage }
    if (reached != null) {
      val maxMs = if (shape.long) EDGE_LONG_START_MS else EDGE_START_MS
      check.atMost(FindingKind.EDGE_TOO_SLOW, (reached.timeMs - finger.downMs).toFloat(), maxMs)
      val angle =
        Math.toDegrees(atan2(sideways(reached).toDouble(), forward(reached).toDouble())).toFloat()
      if (shape.edge.along) {
        check.atMost(FindingKind.ALONG_OFF_EDGE, angle, EDGE_ALONG_MAX_DEGREES)
      } else {
        check.atMost(FindingKind.EDGE_SIDEWAYS, angle, MAX_ANGLE_DEGREES)
      }
    }
  }

  private fun distanceTo(side: ScreenSide, p: TouchPoint): Float =
    when (side) {
      ScreenSide.LEFT -> p.x
      ScreenSide.RIGHT -> limits.screenWidth - p.x
      ScreenSide.TOP -> p.y
      ScreenSide.BOTTOM -> limits.screenHeight - p.y
    }.coerceAtLeast(0f)

  /**
   * Reports a finger that started at another edge than [expected], beyond the edge zone of
   * [expected] and nearer to the other edge: then its distance from [expected] means nothing.
   */
  private fun startedAtOtherSide(check: Checks, start: TouchPoint, expected: ScreenSide): Boolean {
    val nearest = ScreenSide.entries.minBy { distanceTo(it, start) }
    if (nearest == expected || distanceTo(expected, start) <= limits.edgeZonePx) return false
    check.problem(FindingKind.WRONG_EDGE, side = nearest, expectedSide = expected)
    return true
  }

  private fun edgeDirection(edge: Edge): Direction =
    when (edge) {
      Edge.BOTTOM, Edge.ALONG_LEFT_UP, Edge.ALONG_RIGHT_UP -> Direction.UP
      Edge.TOP_LEFT, Edge.TOP_RIGHT, Edge.ALONG_LEFT_DOWN, Edge.ALONG_RIGHT_DOWN -> Direction.DOWN
      Edge.LEFT_TOP, Edge.LEFT_BOTTOM, Edge.ALONG_TOP_RIGHT, Edge.ALONG_BOTTOM_RIGHT -> Direction.RIGHT
      Edge.RIGHT_TOP, Edge.RIGHT_BOTTOM, Edge.ALONG_TOP_LEFT, Edge.ALONG_BOTTOM_LEFT -> Direction.LEFT
    }

  private fun cornerTaps(check: Checks, shape: Shape.CornerTaps, touches: List<Touch>) {
    tapCount(check, touches.size, shape.taps)
    val w = limits.screenWidth.toFloat()
    val h = limits.screenHeight.toFloat()
    touches.forEachIndexed { index, touch ->
      checkFingers(check, touch, 1)
      val p = touch.fingers.first().start
      val dx = if (shape.corner == Corner.TOP_LEFT || shape.corner == Corner.BOTTOM_LEFT) p.x else w - p.x
      val dy = if (shape.corner == Corner.TOP_LEFT || shape.corner == Corner.TOP_RIGHT) p.y else h - p.y
      val outside = limits.toMm(maxOf(dx, dy, 0f))
      check.measure(MeasureKind.CORNER_DISTANCE_MM, outside)
      check.atMost(FindingKind.CORNER_MISSED, outside, limits.toMm(limits.cornerSizePx))
      if (shape.hold && index == shape.taps - 1) {
        check.atLeast(FindingKind.HOLD_TOO_SHORT, touch.durationMs.toFloat(), limits.longPressMs.toFloat())
      } else {
        check.measure(MeasureKind.TAP_MS, touch.durationMs.toFloat())
        check.atMost(FindingKind.CORNER_TAP_SLOW, touch.durationMs.toFloat(), limits.cornerTapMs.toFloat())
      }
      check.atMost(
        FindingKind.TAP_MOVED,
        limits.toMm(touch.fingers.first().farthestFromStart()),
        limits.toMm(limits.touchSlopPx),
      )
    }
    tapGaps(check, touches, limits.multiTapGapMs, limits.doubleTapSlopPx)
  }

  private fun edgeMenu(check: Checks, shape: Shape.EdgeMenu, touches: List<Touch>) {
    val touch = touches.maxBy { touch -> touch.fingers.first().farthestFromStart() }
    checkFingers(check, touch, 1)
    val finger = touch.fingers.first()
    val start = finger.start
    val w = limits.screenWidth.toFloat()
    val h = limits.screenHeight.toFloat()
    val left = shape.edge == MenuEdge.LEFT
    if (startedAtOtherSide(check, start, if (left) ScreenSide.LEFT else ScreenSide.RIGHT)) return
    fun fromEdge(p: TouchPoint) = maxOf(if (left) p.x else w - p.x, 0f)
    check.measure(MeasureKind.EDGE_DISTANCE_MM, limits.toMm(fromEdge(start)))
    check.atMost(FindingKind.EDGE_START_TOO_FAR, limits.toMm(fromEdge(start)), limits.toMm(limits.edgeZonePx))
    if (shape.horizontal) {
      menuAcross(check, shape, finger, ::fromEdge)
      return
    }
    // The ends of a side edge belong to the top and bottom edges.
    if (start.y <= limits.edgeZonePx || start.y >= h - limits.edgeZonePx) {
      check.problem(FindingKind.MENU_IN_CORNER)
    }
    val opened = finger.points.firstOrNull { abs(it.y - start.y) >= limits.edgeFirstStagePx }
    if (opened == null) {
      val along = finger.points.maxOf { abs(it.y - start.y) }
      check.atLeast(FindingKind.EDGE_TOO_SHORT, limits.toMm(along), limits.toMm(limits.edgeFirstStagePx))
      return
    }
    check.atMost(FindingKind.EDGE_TOO_SLOW, (opened.timeMs - finger.downMs).toFloat(), EDGE_START_MS)
    val angle =
      Math.toDegrees(atan2(abs(opened.x - start.x).toDouble(), abs(opened.y - start.y).toDouble())).toFloat()
    check.atMost(FindingKind.EDGE_SIDEWAYS, angle, EDGE_ALONG_MAX_DEGREES)
    val end = finger.end
    check.atMost(
      FindingKind.MENU_LEFT_EDGE,
      limits.toMm(fromEdge(end)),
      limits.toMm(limits.edgeZonePx * MENU_ZONE_FACTOR),
    )
    val items = shape.names.size
    val liftedAt = (end.y * items / h).toInt().coerceIn(0, items - 1)
    check.measure(MeasureKind.MENU_ITEM, (liftedAt + 1).toFloat())
    if (liftedAt != shape.item) {
      check.problem(FindingKind.MENU_WRONG_ITEM, (liftedAt + 1).toFloat(), (shape.item + 1).toFloat())
    }
  }

  /**
   * The rest of [edgeMenu] for a menu across the screen, as EdgeSwipeTracker takes it: the slide in
   * from the edge that it replaces, from the corners too, then the item level with the finger across
   * the screen, while the finger stays level with where the menu opened.
   */
  private fun menuAcross(
    check: Checks,
    shape: Shape.EdgeMenu,
    finger: FingerPath,
    fromEdge: (TouchPoint) -> Float,
  ) {
    val start = finger.start
    val left = shape.edge == MenuEdge.LEFT
    fun inward(p: TouchPoint) = if (left) p.x - start.x else start.x - p.x
    val opened = finger.points.firstOrNull { inward(it) >= limits.edgeFirstStagePx }
    if (opened == null) {
      val farthest = maxOf(finger.points.maxOf { inward(it) }, 0f)
      check.atLeast(FindingKind.EDGE_TOO_SHORT, limits.toMm(farthest), limits.toMm(limits.edgeFirstStagePx))
      return
    }
    check.atMost(FindingKind.EDGE_TOO_SLOW, (opened.timeMs - finger.downMs).toFloat(), EDGE_START_MS)
    val angle =
      Math.toDegrees(atan2(abs(opened.y - start.y).toDouble(), inward(opened).toDouble())).toFloat()
    check.atMost(FindingKind.EDGE_SIDEWAYS, angle, MAX_ANGLE_DEGREES)
    val end = finger.end
    check.atMost(
      FindingKind.MENU_LEFT_LINE,
      limits.toMm(abs(end.y - opened.y)),
      limits.toMm(limits.edgeZonePx * MENU_ZONE_FACTOR),
    )
    val items = shape.names.size
    val liftedAt =
      EdgeSwipeTracker.horizontalMenuItem(
          fromEdge(end),
          items,
          limits.screenWidth.toFloat(),
          limits.edgeFirstStagePx,
        )
        .coerceAtLeast(0)
    check.measure(MeasureKind.MENU_ITEM, (liftedAt + 1).toFloat())
    if (liftedAt != shape.item) {
      check.problem(FindingKind.MENU_WRONG_ITEM, (liftedAt + 1).toFloat(), (shape.item + 1).toFloat())
    }
  }

  // Shake

  /**
   * Judges a shake from the [strongest] movement the accelerometer felt in the try, in m/s² beyond
   * gravity, where [needed] was needed for a swing, and the most swings, each the other way, that
   * counted toward a shake at once. [shaken] says whether the shake was recognized.
   */
  fun analyzeShake(
    exercise: Exercise,
    strongest: Float,
    needed: Float,
    mostSwings: Int,
    shaken: Boolean,
  ): AttemptReport {
    val check = Checks(limits)
    check.measure(MeasureKind.SHAKE_STRENGTH, strongest)
    if (!shaken) {
      check.atLeast(FindingKind.SHAKE_TOO_LIGHT, strongest, needed)
      if (strongest >= needed) {
        check.atLeast(FindingKind.SHAKE_TOO_FEW_SWINGS, mostSwings.toFloat(), ShakeDetector.SWINGS.toFloat())
      }
    }
    val recognized = if (shaken) listOf(TrainerExercises.SHAKE_ID) else emptyList()
    val success = exercise.gestureId in recognized
    val findings =
      if (success) check.findings.filter { it.severity == Severity.CLOSE } else check.findings
    return AttemptReport(success, recognized, null, check.rating(success), findings, check.measures)
  }

  // Back taps

  /**
   * Judges taps on the back from the [jolts] the accelerometer felt in the try, and the number of
   * taps the detector counted in the run that ended, or null when no run ended. A try in which
   * [screenTouched] fails, as taps while the screen is touched don't count.
   */
  fun analyzeBackTaps(
    exercise: Exercise,
    jolts: List<BackTapDetector.Jolt>,
    count: Int?,
    screenTouched: Boolean = false,
  ): AttemptReport {
    val shape = exercise.shape as Shape.BackTaps
    val check = Checks(limits)
    val taps = jolts.filter { it.rejection == null }
    tapCount(check, count ?: taps.size, shape.taps)
    if (screenTouched) check.problem(FindingKind.BACK_TAP_SCREEN_TOUCHED)
    for (jolt in jolts) {
      check.measure(MeasureKind.BACK_TAP_STRENGTH, jolt.strength)
      when (jolt.rejection) {
        null -> check.atLeast(FindingKind.BACK_TAP_TOO_LIGHT, jolt.strength, jolt.needed)
        BackTapDetector.Rejection.TOO_LIGHT ->
          check.problem(FindingKind.BACK_TAP_TOO_LIGHT, jolt.strength, jolt.needed)
        BackTapDetector.Rejection.SIDEWAYS -> check.problem(FindingKind.BACK_TAP_SIDEWAYS)
        BackTapDetector.Rejection.MOVING -> check.problem(FindingKind.BACK_TAP_MOVING)
        BackTapDetector.Rejection.SCREEN_TOUCHED ->
          if (!screenTouched) check.problem(FindingKind.BACK_TAP_SCREEN_TOUCHED)
        // Shaking after a run of taps, which belongs to it.
        BackTapDetector.Rejection.JUST_AFTER_RUN -> {}
      }
    }
    for ((before, after) in taps.zipWithNext()) {
      val gapMs = (after.timeNs - before.timeNs) / 1_000_000f
      check.measure(MeasureKind.GAP_MS, gapMs)
      check.atMost(
        FindingKind.BACK_TAPS_TOO_SLOW,
        gapMs,
        BackTapDetector.MAX_TAP_INTERVAL_NS / 1_000_000f,
      )
    }
    val recognized =
      if (screenTouched) emptyList() else listOfNotNull(count?.let { TrainerExercises.backTapId(it) })
    val success = exercise.gestureId in recognized
    val rating = check.rating(success)
    val findings =
      if (success) check.findings.filter { it.severity == Severity.CLOSE } else check.findings
    return AttemptReport(success, recognized, null, rating, findings, check.measures)
  }

  /** Collects findings and measures, and how close the attempt came to any limit. */
  private class Checks(val limits: TrainerLimits) {
    val findings = ArrayList<Finding>()
    val measures = ArrayList<Measure>()

    /**
     * The largest share of a limit used by a check within its limit: 1 is at the limit. A
     * recognized gesture is rated by it alone, as the trainer's estimate of a limit SandFly passed
     * was wrong.
     */
    private var closeness = 0f

    fun rating(success: Boolean): Rating =
      when {
        !success -> Rating.FAILED
        closeness < PRECISE_BELOW -> Rating.PRECISE
        closeness < GOOD_BELOW -> Rating.GOOD
        else -> Rating.BORDERLINE
      }

    fun measure(kind: MeasureKind, value: Float) {
      measures += Measure(kind, value)
    }

    /** Takes the findings, as [map] turns them, measures and closeness of [other] checks. */
    fun absorb(other: Checks, withMeasures: Boolean = true, map: (Finding) -> Finding) {
      findings += other.findings.map(map)
      if (withMeasures) measures += other.measures
      closeness = maxOf(closeness, other.closeness)
    }

    fun problem(
      kind: FindingKind,
      value: Float = 0f,
      limit: Float = 0f,
      direction: Direction? = null,
      side: ScreenSide? = null,
      expectedSide: ScreenSide? = null,
    ) {
      findings += Finding(kind, Severity.PROBLEM, value, limit, direction, side, expectedSide)
    }

    fun atMost(kind: FindingKind, value: Float, limit: Float) {
      if (limit <= 0f) return
      judge(kind, value / limit, value, limit)
    }

    fun atLeast(kind: FindingKind, value: Float, minimum: Float) {
      if (minimum <= 0f) return
      judge(kind, if (value <= 0f) 2f else minimum / value, value, minimum)
    }

    private fun judge(kind: FindingKind, share: Float, value: Float, limit: Float) {
      if (share <= 1f) closeness = maxOf(closeness, share)
      when {
        share > 1f -> findings += Finding(kind, Severity.PROBLEM, value, limit)
        share >= GOOD_BELOW -> findings += Finding(kind, Severity.CLOSE, value, limit)
      }
    }
  }

  companion object {
    private const val PRECISE_BELOW = 0.6f
    private const val GOOD_BELOW = 0.85f
    private const val SAMPLE_CM = 0.25f
    private const val MAX_ANGLE_DEGREES = 45f
    private const val EDGE_ALONG_MAX_DEGREES = 26.5f
    private const val EDGE_START_MS = 500f
    private const val EDGE_LONG_START_MS = 1000f
    private const val MIN_PINCH_MM = 5f
    private const val MIN_ROTATION_DEGREES = 15f
    private const val SCRUB_STROKE_MM = 10f
    private const val SCRUB_RETURN_FRACTION = 0.6f
    private const val SCRUB_MAX_MS = 1200f
    private const val MENU_ZONE_FACTOR = 3f
    /** How far a touch must move to be taken as the swipe after the taps, not as a tap. */
    private const val TAP_SWIPE_MOVED_MM = 5f

    /** The edge menu choice when the finger lifted away from the edge, choosing nothing. */
    const val NO_MENU_ITEM = -1

    /** How far a movement of ([dx], [dy]) turns away from [direction], in degrees. */
    fun angleOff(dx: Float, dy: Float, direction: Direction): Float {
      val length = hypot(dx, dy)
      if (length == 0f) return 0f
      val cos = ((dx * direction.dx + dy * direction.dy) / length).coerceIn(-1f, 1f)
      return Math.toDegrees(acos(cos).toDouble()).toFloat()
    }

    /**
     * Splits [stroke] where its direction changes by about 90 degrees, as the one-finger swipe
     * matcher does. Returns the points where the parts start, and the last point.
     */
    fun segments(stroke: List<TouchPoint>): List<TouchPoint> {
      val path = ArrayList<TouchPoint>()
      var lastDelimiter = stroke[0]
      path += lastDelimiter
      var dX = 0f
      var dY = 0f
      var count = 0
      var length = 0f
      var next = stroke[0]
      for (i in 1 until stroke.size) {
        next = stroke[i]
        if (count > 0) {
          val currentDX = dX / count
          val currentDY = dY / count
          val newDelimiter =
            TouchPoint(next.timeMs, length * currentDX + lastDelimiter.x, length * currentDY + lastDelimiter.y)
          var nextDX = next.x - newDelimiter.x
          var nextDY = next.y - newDelimiter.y
          val nextLength = hypot(nextDX, nextDY)
          nextDX /= nextLength
          nextDY /= nextLength
          if (currentDX * nextDX + currentDY * nextDY < 0f) {
            path += newDelimiter
            lastDelimiter = newDelimiter
            dX = 0f
            dY = 0f
            count = 0
          }
        }
        val currentDX = next.x - lastDelimiter.x
        val currentDY = next.y - lastDelimiter.y
        length = hypot(currentDX, currentDY)
        count++
        dX += currentDX / length
        dY += currentDY / length
      }
      path += next
      return path
    }
  }
}
