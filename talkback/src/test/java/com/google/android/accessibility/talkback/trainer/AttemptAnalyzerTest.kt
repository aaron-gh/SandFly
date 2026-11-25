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

import android.accessibilityservice.AccessibilityService
import com.google.android.accessibility.talkback.gesture.BackTapDetector
import com.google.android.accessibility.talkback.gesture.CornerTapTracker.Corner
import com.google.android.accessibility.talkback.gesture.EdgeSwipeTracker.Edge
import com.google.android.accessibility.talkback.gesture.EdgeSwipeTracker.MenuEdge
import com.google.android.accessibility.utils.gestures.GestureManifold
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttemptAnalyzerTest {
  // 100 px per cm, so 1 px is 0.1 mm.
  private val limits =
    TrainerLimits(
      pxPerCm = 100f,
      touchSlopPx = 20f,
      doubleTapSlopPx = 100f,
      tapTimeoutMs = 100,
      longPressMs = 500,
      multiTapGapMs = 250,
      multiFingerTapGapMs = 300,
      swipeStartMs = 150,
      swipeContinueMs = 350,
      edgeZonePx = 50f,
      edgeFirstStagePx = 100f,
      edgeSecondStagePx = 400f,
      cornerSizePx = 50f,
      cornerTapMs = 100,
      screenWidth = 1000,
      screenHeight = 2000,
    )
  private val analyzer = AttemptAnalyzer(limits)

  private val swipeRight =
    Exercise(AccessibilityService.GESTURE_SWIPE_RIGHT, Shape.Swipe(1, listOf(Direction.RIGHT)))

  @Test
  fun fastStraightSwipe_isPrecise() {
    val touch = touch(line(0, 300f, 500f, 600f, 500f, durationMs = 120))
    val report = analyzer.analyze(swipeRight, listOf(touch), listOf(swipeRight.gestureId))
    assertTrue(report.success)
    assertEquals(Rating.PRECISE, report.rating)
    assertTrue(report.findings.isEmpty())
  }

  @Test
  fun swipeThatRestsFirst_isASlowStart() {
    val rest = FingerPath(0)
    for (t in 0L..300L step 20) rest.points += TouchPoint(t, 300f, 500f)
    val moving = line(300, 300f, 500f, 600f, 500f, durationMs = 120)
    rest.points += moving.points
    rest.upMs = moving.upMs
    val report = analyzer.analyze(swipeRight, listOf(touch(rest)), emptyList())
    assertFalse(report.success)
    assertEquals(FindingKind.SLOW_START, report.problems.single().kind)
    assertTrue(report.problems.single().value > 300f)
  }

  @Test
  fun swipeThatBecameExploration_saysSoAndNotTooShort() {
    // The finger rests, touch exploration takes over, and the rest of its way is never seen.
    val rest = FingerPath(0)
    for (t in 0L..700L step 20) rest.points += TouchPoint(t, 300f, 500f)
    rest.points += TouchPoint(720, 340f, 500f)
    rest.upMs = 900
    val touch = touch(rest)
    touch.becameExploration = true
    val report = analyzer.analyze(swipeRight, listOf(touch), emptyList())
    assertFalse(report.success)
    assertEquals(FindingKind.BECAME_EXPLORATION, report.problems.single().kind)
  }

  @Test
  fun swipeThatHooksAtTheEnd_isCurved() {
    val path = line(0, 300f, 500f, 600f, 500f, durationMs = 100)
    val hook = line(100, 600f, 500f, 600f, 300f, durationMs = 60)
    path.points += hook.points.drop(1)
    path.upMs = hook.upMs
    val report = analyzer.analyze(swipeRight, listOf(touch(path)), emptyList())
    assertTrue(report.problems.any { it.kind == FindingKind.CURVED })
  }

  @Test
  fun angleGesture_findsBothParts() {
    val exercise =
      Exercise(
        AccessibilityService.GESTURE_SWIPE_UP_AND_RIGHT,
        Shape.Swipe(1, listOf(Direction.UP, Direction.RIGHT)),
      )
    val path = line(0, 500f, 1000f, 500f, 750f, durationMs = 100)
    val second = line(100, 500f, 750f, 750f, 750f, durationMs = 100)
    path.points += second.points.drop(1)
    path.upMs = second.upMs
    val report = analyzer.analyze(exercise, listOf(touch(path)), listOf(exercise.gestureId))
    assertTrue(report.success)
    assertTrue(report.findings.none { it.kind == FindingKind.TURN_MISSING })
  }

  @Test
  fun twoFingersLandingFarApart_isReported() {
    val exercise =
      Exercise(AccessibilityService.GESTURE_2_FINGER_SWIPE_UP, Shape.Swipe(2, listOf(Direction.UP)))
    val first = line(0, 400f, 1500f, 400f, 1200f, durationMs = 500, pointerId = 0)
    val second = line(300, 500f, 1500f, 500f, 1200f, durationMs = 200, pointerId = 1)
    val touch = touch(first, second)
    val report = analyzer.analyze(exercise, listOf(touch), emptyList())
    val finding = report.problems.first { it.kind == FindingKind.FINGERS_LANDED_APART }
    assertEquals(300f, finding.value)
    assertEquals(200f, finding.limit)
  }

  @Test
  fun slowDoubleTap_isReported() {
    val exercise =
      Exercise(AccessibilityService.GESTURE_DOUBLE_TAP, Shape.Taps(1, 2, hold = false))
    val taps = listOf(touch(tap(0, 500f, 500f, 60)), touch(tap(460, 505f, 500f, 60)))
    val report = analyzer.analyze(exercise, taps, emptyList())
    val finding = report.problems.single()
    assertEquals(FindingKind.TAPS_TOO_SLOW, finding.kind)
    assertEquals(400f, finding.value)
  }

  @Test
  fun tapOutsideTheCorner_isAMiss() {
    val exercise =
      Exercise(GestureManifold.GESTURE_CORNER_TOP_LEFT_TAP, Shape.CornerTaps(Corner.TOP_LEFT, 1))
    val report = analyzer.analyze(exercise, listOf(touch(tap(0, 30f, 80f, 60))), emptyList())
    val finding = report.problems.single()
    assertEquals(FindingKind.CORNER_MISSED, finding.kind)
    assertEquals(8f, finding.value, 0.01f)
    assertEquals(5f, finding.limit, 0.01f)
  }

  @Test
  fun smallSpread_isTooSmall() {
    val exercise = Exercise(GestureManifold.GESTURE_2_FINGER_SPREAD, Shape.Pinch(2, spread = true))
    val a = line(0, 480f, 1000f, 470f, 1000f, durationMs = 200, pointerId = 0)
    val b = line(0, 520f, 1000f, 530f, 1000f, durationMs = 200, pointerId = 1)
    val report = analyzer.analyze(exercise, listOf(touch(a, b)), emptyList())
    assertEquals(FindingKind.PINCH_TOO_SMALL, report.problems.single().kind)
  }

  @Test
  fun clockwiseTurn_isClockwise() {
    val exercise =
      Exercise(GestureManifold.GESTURE_2_FINGER_ROTATE_CLOCKWISE, Shape.Rotate(clockwise = true))
    val a = FingerPath(0)
    val b = FingerPath(1)
    // Seen on the screen, where y grows downward, the fingers turn clockwise by 90 degrees.
    for (step in 0..9) {
      val angle = Math.toRadians(180.0 + step * 10.0)
      val t = step * 20L
      a.points += TouchPoint(t, 500f + 150f * cos(angle).toFloat(), 1000f + 150f * sin(angle).toFloat())
      b.points += TouchPoint(t, 500f - 150f * cos(angle).toFloat(), 1000f - 150f * sin(angle).toFloat())
    }
    a.upMs = 180
    b.upMs = 180
    val report = analyzer.analyze(exercise, listOf(touch(a, b)), listOf(exercise.gestureId))
    assertTrue(report.success)
    assertTrue(report.findings.isEmpty())
    assertEquals(90f, report.measures.first { it.kind == MeasureKind.ROTATION_DEGREES }.value, 1f)
  }

  @Test
  fun swipeAndHold_measuresTheRestNotAsAPause() {
    val exercise =
      Exercise(
        GestureManifold.GESTURE_SWIPE_RIGHT_AND_HOLD,
        Shape.Swipe(1, listOf(Direction.RIGHT), hold = true),
      )
    val path = line(0, 300f, 500f, 600f, 500f, durationMs = 120)
    for (t in 130L..520L step 10) path.points += TouchPoint(t, 600f, 500f)
    path.upMs = 520
    val report = analyzer.analyze(exercise, listOf(touch(path)), emptyList())
    assertTrue(report.problems.none { it.kind == FindingKind.PAUSED })
    val hold = report.problems.single { it.kind == FindingKind.SWIPE_HOLD_TOO_SHORT }
    assertEquals(400f, hold.value)
    assertEquals(500f, hold.limit)
  }

  @Test
  fun tapThenSwipe_findsTheTapsAndTheLateSwipe() {
    val exercise =
      Exercise(
        GestureManifold.GESTURE_2_FINGER_DOUBLE_TAP_SWIPE_UP,
        Shape.TapSwipe(fingers = 2, taps = 2, swipeFingers = 2, direction = Direction.UP),
      )
    val touches =
      listOf(
        touch(tap(0, 400f, 1000f, 60), tap(0, 500f, 1000f, 60).withId(1)),
        touch(tap(200, 400f, 1000f, 60), tap(200, 500f, 1000f, 60).withId(1)),
        touch(
          line(660, 400f, 1000f, 400f, 700f, durationMs = 150, pointerId = 0),
          line(660, 500f, 1000f, 500f, 700f, durationMs = 150, pointerId = 1),
        ),
      )
    val report = analyzer.analyze(exercise, touches, emptyList())
    assertTrue(report.problems.none { it.kind == FindingKind.TOO_FEW_TAPS })
    val late = report.problems.single { it.kind == FindingKind.SWIPE_LATE }
    assertEquals(400f, late.value)
  }

  @Test
  fun tapsWithoutSwipe_missTheSwipe() {
    val exercise =
      Exercise(
        GestureManifold.GESTURE_2_FINGER_SINGLE_TAP_1_FINGER_SWIPE_LEFT,
        Shape.TapSwipe(fingers = 2, taps = 1, swipeFingers = 1, direction = Direction.LEFT),
      )
    val touches = listOf(touch(tap(0, 400f, 1000f, 60), tap(0, 500f, 1000f, 60).withId(1)))
    val report = analyzer.analyze(exercise, touches, emptyList())
    assertTrue(report.problems.any { it.kind == FindingKind.SWIPE_MISSING })
  }

  @Test
  fun fourFingerTapInTheOtherHalf_isReported() {
    val exercise =
      Exercise(
        GestureManifold.GESTURE_4_FINGER_SINGLE_TAP_TOP,
        Shape.Taps(4, 1, hold = false, half = ScreenHalf.TOP),
      )
    val fingers = (0..3).map { tap(0, 200f + it * 150f, 1500f, 60).withId(it) }
    val report = analyzer.analyze(exercise, listOf(touch(*fingers.toTypedArray())), emptyList())
    assertEquals(FindingKind.TAP_WRONG_HALF, report.problems.single().kind)
  }

  @Test
  fun cornerDoubleTapAndHold_checksTheHoldNotTheTapTime() {
    val exercise =
      Exercise(
        GestureManifold.GESTURE_CORNER_TOP_LEFT_DOUBLE_TAP_AND_HOLD,
        Shape.CornerTaps(Corner.TOP_LEFT, 2, hold = true),
      )
    val taps = listOf(touch(tap(0, 20f, 20f, 60)), touch(tap(150, 20f, 20f, 300)))
    val report = analyzer.analyze(exercise, taps, emptyList())
    val hold = report.problems.single()
    assertEquals(FindingKind.HOLD_TOO_SHORT, hold.kind)
    assertEquals(300f, hold.value)
  }

  @Test
  fun lightBackTaps_areTooLight() {
    val exercise = Exercise(TrainerExercises.BACK_DOUBLE_TAP_ID, Shape.BackTaps(2))
    val jolts =
      listOf(
        BackTapDetector.Jolt(0, 10f, 9f, rejection = null),
        BackTapDetector.Jolt(300_000_000, 5f, 6.75f, BackTapDetector.Rejection.TOO_LIGHT),
      )
    val report = analyzer.analyzeBackTaps(exercise, jolts, count = 1)
    assertFalse(report.success)
    assertTrue(report.problems.any { it.kind == FindingKind.TOO_FEW_TAPS })
    assertTrue(report.problems.any { it.kind == FindingKind.BACK_TAP_TOO_LIGHT })
  }

  @Test
  fun backTripleTap_isRecognizedAsTripleNotDouble() {
    val exercise = Exercise(TrainerExercises.BACK_DOUBLE_TAP_ID, Shape.BackTaps(2))
    val jolts = (0..2).map { BackTapDetector.Jolt(it * 200_000_000L, 12f, 9f, rejection = null) }
    val report = analyzer.analyzeBackTaps(exercise, jolts, count = 3)
    assertFalse(report.success)
    assertEquals(listOf(TrainerExercises.BACK_TRIPLE_TAP_ID), report.recognized)
    assertTrue(report.problems.any { it.kind == FindingKind.TOO_MANY_TAPS })
  }

  @Test
  fun backTapsWithATouchOnTheScreen_fail() {
    val exercise = Exercise(TrainerExercises.BACK_DOUBLE_TAP_ID, Shape.BackTaps(2))
    val jolts = (0..1).map { BackTapDetector.Jolt(it * 200_000_000L, 12f, 9f, rejection = null) }
    val report = analyzer.analyzeBackTaps(exercise, jolts, count = 2, screenTouched = true)
    assertFalse(report.success)
    assertEquals(FindingKind.BACK_TAP_SCREEN_TOUCHED, report.problems.single().kind)
  }

  @Test
  fun recognizedGesture_isRatedOnlyByLimitsItKept() {
    // The trainer's estimate says too short, but SandFly recognized the spread.
    val exercise = Exercise(GestureManifold.GESTURE_2_FINGER_SPREAD, Shape.Pinch(2, spread = true))
    val a = line(0, 480f, 1000f, 470f, 1000f, durationMs = 200, pointerId = 0)
    val b = line(0, 520f, 1000f, 530f, 1000f, durationMs = 200, pointerId = 1)
    val report = analyzer.analyze(exercise, listOf(touch(a, b)), listOf(exercise.gestureId))
    assertTrue(report.success)
    assertEquals(Rating.PRECISE, report.rating)
  }

  @Test
  fun spreadThatTurns_isTakenAsARotation() {
    val exercise = Exercise(GestureManifold.GESTURE_2_FINGER_SPREAD, Shape.Pinch(2, spread = true))
    val a = FingerPath(0)
    val b = FingerPath(1)
    // The fingers move apart from 100 to 200 px from their middle while turning by 40 degrees.
    for (step in 0..10) {
      val angle = Math.toRadians(180.0 + step * 4.0)
      val r = 100f + step * 10f
      val t = step * 20L
      a.points += TouchPoint(t, 500f + r * cos(angle).toFloat(), 1000f + r * sin(angle).toFloat())
      b.points += TouchPoint(t, 500f - r * cos(angle).toFloat(), 1000f - r * sin(angle).toFloat())
    }
    a.upMs = 200
    b.upMs = 200
    val rotate = GestureManifold.GESTURE_2_FINGER_ROTATE_CLOCKWISE
    val report = analyzer.analyze(exercise, listOf(touch(a, b)), listOf(rotate))
    val turned = report.problems.single { it.kind == FindingKind.TURNED_IN_PINCH }
    assertEquals(40f, turned.value, 1f)
  }

  @Test
  fun slowEdgeSlideThatBecameExploration_saysSoAndNotTooShort() {
    val exercise =
      Exercise(
        GestureManifold.GESTURE_EDGE_SWIPE_UP_FROM_BOTTOM,
        Shape.EdgeSlide(Edge.BOTTOM, long = false),
      )
    // Only its slow start is seen before touch exploration takes over.
    val touch = touch(line(0, 540f, 1995f, 540f, 1960f, durationMs = 560))
    touch.becameExploration = true
    val report = analyzer.analyze(exercise, listOf(touch), emptyList())
    assertEquals(FindingKind.BECAME_EXPLORATION, report.problems.single().kind)
  }

  @Test
  fun slideAlongTheLeftEdgeForTheTopEdge_isTheWrongEdge() {
    val exercise =
      Exercise(
        GestureManifold.GESTURE_EDGE_SWIPE_LEFT_ALONG_TOP,
        Shape.EdgeSlide(Edge.ALONG_TOP_LEFT, long = false),
      )
    // Down the left edge, 8 cm from the top, as a slide through the left edge's menu.
    val path = line(0, 10f, 800f, 10f, 1100f, durationMs = 200)
    val report = analyzer.analyze(exercise, listOf(touch(path)), emptyList(), menuChoice = 3)
    val wrongEdge = report.problems.single { it.kind == FindingKind.WRONG_EDGE }
    assertEquals(ScreenSide.LEFT, wrongEdge.side)
    assertEquals(ScreenSide.TOP, wrongEdge.expectedSide)
    assertTrue(report.problems.any { it.kind == FindingKind.MENU_OPENED_INSTEAD })
    assertTrue(report.problems.none { it.kind == FindingKind.EDGE_START_TOO_FAR })
  }

  private val swipeRightLiftSwipeDown =
    Exercise(
      GestureManifold.GESTURE_SWIPE_RIGHT_LIFT_SWIPE_DOWN,
      Shape.TwoStroke(Direction.RIGHT, Direction.DOWN),
    )

  @Test
  fun twoStrokeWithOneSwipe_missesTheSecondStroke() {
    val first = touch(line(0, 300f, 500f, 600f, 500f, durationMs = 120))
    val report = analyzer.analyze(swipeRightLiftSwipeDown, listOf(first), emptyList())
    assertFalse(report.success)
    assertEquals(FindingKind.SECOND_STROKE_MISSING, report.problems.single().kind)
  }

  @Test
  fun twoStrokeWithALateSecondSwipe_isLate() {
    val first = touch(line(0, 300f, 500f, 600f, 500f, durationMs = 120))
    val second = touch(line(1620, 500f, 800f, 500f, 1100f, durationMs = 120))
    val report = analyzer.analyze(swipeRightLiftSwipeDown, listOf(first, second), emptyList())
    val late = report.problems.single()
    assertEquals(FindingKind.SECOND_STROKE_LATE, late.kind)
    assertEquals(1500f, late.value)
    assertEquals(1000f, late.limit)
    assertEquals(1500f, report.measures.single { it.kind == MeasureKind.STROKE_GAP_MS }.value)
  }

  @Test
  fun twoStrokeWithTheSecondSwipeTheWrongWay_namesTheSecondStroke() {
    val first = touch(line(0, 300f, 500f, 600f, 500f, durationMs = 120))
    val second = touch(line(400, 500f, 1100f, 500f, 800f, durationMs = 120))
    val report = analyzer.analyze(swipeRightLiftSwipeDown, listOf(first, second), emptyList())
    val wrong = report.problems.single()
    assertEquals(FindingKind.STROKE_WRONG_DIRECTION, wrong.kind)
    assertEquals(2f, wrong.value)
    assertEquals(Direction.UP, wrong.direction)
  }

  @Test
  fun twoStrokeRecognized_succeeds() {
    val first = touch(line(0, 300f, 500f, 600f, 500f, durationMs = 120))
    val second = touch(line(400, 500f, 800f, 500f, 1100f, durationMs = 120))
    val report =
      analyzer.analyze(
        swipeRightLiftSwipeDown,
        listOf(first, second),
        listOf(swipeRightLiftSwipeDown.gestureId),
      )
    assertTrue(report.success)
    assertTrue(report.problems.isEmpty())
  }

  // The left edge menu with four items, each 5 cm of the 20 cm screen; the third is wanted.
  private val leftMenuThirdItem =
    Exercise(
      TrainerExercises.MENU_GESTURE_ID,
      Shape.EdgeMenu(
        MenuEdge.LEFT,
        item = 2,
        names = listOf("Option 1", "Option 2", "Option 3", "Option 4"),
        sample = true,
      ),
    )

  @Test
  fun edgeMenuLiftedAtTheWantedItem_succeeds() {
    val path = line(0, 10f, 300f, 10f, 1200f, durationMs = 300)
    val report = analyzer.analyze(leftMenuThirdItem, listOf(touch(path)), emptyList(), menuChoice = 2)
    assertTrue(report.success)
    assertEquals(3f, report.measures.single { it.kind == MeasureKind.MENU_ITEM }.value)
  }

  @Test
  fun edgeMenuLiftedAtAnotherItem_namesBothItems() {
    val path = line(0, 10f, 300f, 10f, 1700f, durationMs = 400)
    val report = analyzer.analyze(leftMenuThirdItem, listOf(touch(path)), emptyList(), menuChoice = 3)
    assertFalse(report.success)
    val wrong = report.problems.single { it.kind == FindingKind.MENU_WRONG_ITEM }
    assertEquals(4f, wrong.value)
    assertEquals(3f, wrong.limit)
  }

  @Test
  fun edgeMenuLeftFarFromTheEdge_isCancelled() {
    val path = line(0, 10f, 300f, 400f, 1200f, durationMs = 300)
    val report =
      analyzer.analyze(
        leftMenuThirdItem,
        listOf(touch(path)),
        emptyList(),
        menuChoice = AttemptAnalyzer.NO_MENU_ITEM,
      )
    assertFalse(report.success)
    assertTrue(report.problems.any { it.kind == FindingKind.MENU_LEFT_EDGE })
  }

  @Test
  fun edgeMenuStartedAtTheTopEnd_isInTheCorner() {
    val path = line(0, 10f, 20f, 10f, 1200f, durationMs = 300)
    val report = analyzer.analyze(leftMenuThirdItem, listOf(touch(path)), emptyList(), menuChoice = 2)
    assertTrue(report.success)
    val failed = analyzer.analyze(leftMenuThirdItem, listOf(touch(path)), emptyList(), menuChoice = 1)
    assertTrue(failed.problems.any { it.kind == FindingKind.MENU_IN_CORNER })
  }

  @Test
  fun lightShake_isTooLight() {
    val shake = Exercise(TrainerExercises.SHAKE_ID, Shape.Shake)
    val report = analyzer.analyzeShake(shake, strongest = 6f, needed = 12f, mostSwings = 2, shaken = false)
    assertFalse(report.success)
    val light = report.problems.single()
    assertEquals(FindingKind.SHAKE_TOO_LIGHT, light.kind)
    assertEquals(6f, light.value)
    assertEquals(12f, light.limit)
  }

  @Test
  fun segments_splitsAtARightAngle() {
    val points =
      (0..8).map { TouchPoint(it * 10L, 0f, -30f * it) } +
        (1..8).map { TouchPoint(80L + it * 10L, 30f * it, -240f) }
    assertEquals(3, AttemptAnalyzer.segments(points).size)
  }

  // A finger moving evenly from (x1, y1) to (x2, y2), sampled every 10 ms.
  private fun line(
    startMs: Long,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    durationMs: Long,
    pointerId: Int = 0,
  ): FingerPath {
    val path = FingerPath(pointerId)
    val steps = (durationMs / 10).toInt()
    for (i in 0..steps) {
      val f = i.toFloat() / steps
      path.points += TouchPoint(startMs + i * 10L, x1 + (x2 - x1) * f, y1 + (y2 - y1) * f)
    }
    path.upMs = startMs + durationMs
    return path
  }

  private fun tap(startMs: Long, x: Float, y: Float, durationMs: Long): FingerPath {
    val path = FingerPath(0)
    path.points += TouchPoint(startMs, x, y)
    path.points += TouchPoint(startMs + durationMs, x, y)
    path.upMs = startMs + durationMs
    return path
  }

  /** The same path for the finger with pointer id [id]. */
  private fun FingerPath.withId(id: Int): FingerPath =
    FingerPath(id).also {
      it.points += points
      it.upMs = upMs
    }

  private fun touch(vararg fingers: FingerPath): Touch =
    Touch().apply {
      this.fingers += fingers
      mostFingersAtOnce = fingers.size
    }
}
