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
import android.content.Context
import android.os.Build
import android.view.ViewConfiguration
import androidx.annotation.RequiresApi
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.flags.FeatureFlagReader
import com.google.android.accessibility.talkback.gesture.CornerTapTracker
import com.google.android.accessibility.talkback.gesture.CornerTapTracker.Corner
import com.google.android.accessibility.talkback.gesture.EdgeMenus
import com.google.android.accessibility.talkback.gesture.EdgeSwipeTracker.Edge
import com.google.android.accessibility.talkback.gesture.EdgeSwipeTracker.MenuEdge
import com.google.android.accessibility.talkback.gesture.GestureLayout
import com.google.android.accessibility.talkback.gesture.SwipesBeforeLift
import com.google.android.accessibility.talkback.gesture.TwoStrokeSwipes
import com.google.android.accessibility.talkback.trainer.Direction.DOWN
import com.google.android.accessibility.talkback.trainer.Direction.LEFT
import com.google.android.accessibility.talkback.trainer.Direction.RIGHT
import com.google.android.accessibility.talkback.trainer.Direction.UP
import com.google.android.accessibility.utils.BuildVersionUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.gestures.GestureConfiguration
import com.google.android.accessibility.utils.gestures.GestureManifold
import com.google.android.accessibility.utils.gestures.GestureUtils
import com.google.android.accessibility.utils.gestures.TwoStrokeSwipeIds

/** A direction on the screen, where y grows downward. */
enum class Direction(val dx: Float, val dy: Float) {
  UP(0f, -1f),
  DOWN(0f, 1f),
  LEFT(-1f, 0f),
  RIGHT(1f, 0f);

  companion object {
    /** The direction a movement goes, as the gesture matchers decide it: the larger axis wins. */
    fun of(dx: Float, dy: Float): Direction =
      if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
        if (dx < 0) LEFT else RIGHT
      } else {
        if (dy < 0) UP else DOWN
      }
  }
}

/** What the fingers do in a gesture. */
sealed interface Shape {
  /**
   * A swipe of [fingers] in one direction, or in two that meet at a turn. With [hold], the finger
   * then stays still before it lifts.
   */
  data class Swipe(val fingers: Int, val directions: List<Direction>, val hold: Boolean = false) :
    Shape

  /** Taps; with [half], in that half of the screen. */
  data class Taps(
    val fingers: Int,
    val taps: Int,
    val hold: Boolean,
    val half: ScreenHalf? = null,
  ) : Shape

  /** Taps of [fingers], then at once a swipe of [swipeFingers] toward [direction]. */
  data class TapSwipe(
    val fingers: Int,
    val taps: Int,
    val swipeFingers: Int,
    val direction: Direction,
  ) : Shape

  /** Taps on the back of the device. */
  data class BackTaps(val taps: Int) : Shape

  /** Shaking the device back and forth. */
  data object Shake : Shape

  /** A one-finger swipe toward [first], the finger lifted, and a second one toward [second]. */
  data class TwoStroke(val first: Direction, val second: Direction) : Shape

  data class Pinch(val fingers: Int, val spread: Boolean) : Shape

  data class Rotate(val clockwise: Boolean) : Shape

  data object Scrub : Shape

  data class EdgeSlide(val edge: Edge, val long: Boolean) : Shape

  data class CornerTaps(val corner: Corner, val taps: Int, val hold: Boolean = false) : Shape

  /**
   * Choosing [item] of the menu along [edge], whose items are [names]: the user's menu, or sample
   * items when [sample] is true, as the edge has no menu.
   */
  data class EdgeMenu(
    val edge: MenuEdge,
    val item: Int,
    val names: List<String>,
    val sample: Boolean,
  ) : Shape
}

enum class ScreenHalf {
  TOP,
  BOTTOM,
  /** One finger in the top third of the screen and the other in the bottom third. */
  TOP_AND_BOTTOM,
}

class Exercise(val gestureId: Int, val shape: Shape)

/** The groups of exercises the trainer offers. */
enum class TrainerGroup(val title: Int) {
  ONE_FINGER(R.string.gesture_trainer_group_one_finger),
  TWO_FINGERS(R.string.gesture_trainer_group_two_fingers),
  THREE_FINGERS(R.string.gesture_trainer_group_three_fingers),
  FOUR_FINGERS(R.string.gesture_trainer_group_four_fingers),
  PINCH_ROTATE_SCRUB(R.string.gesture_trainer_group_pinch_rotate_scrub),
  TAP_SWIPE(R.string.gesture_trainer_group_tap_swipe),
  THREE_PART_SWIPES(R.string.gesture_trainer_group_three_part),
  TWO_STROKE_SWIPES(R.string.gesture_trainer_group_two_stroke),
  EDGE_SWIPES(R.string.gesture_trainer_group_edge_swipes),
  ALONG_EDGES(R.string.gesture_trainer_group_along_edges),
  EDGE_MENUS(R.string.gesture_trainer_group_edge_menus),
  CORNERS(R.string.gesture_trainer_group_corners),
  BACK_TAP(R.string.gesture_trainer_group_back_tap),
  SHAKE(R.string.gesture_trainer_group_shake),
}

/** A group's exercises, easy ones first, and why some gestures are left out. */
class ExerciseList(val exercises: List<Exercise>, val leftOut: List<String>)

object TrainerExercises {
  /** The exercises of [group] that SandFly recognizes with the settings in use. */
  fun of(context: Context, group: TrainerGroup): ExerciseList {
    // SandFly recognizes gestures itself from Android 13.
    if (!BuildVersionUtils.isAtLeastT()) return ExerciseList(emptyList(), emptyList())
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val twoDirectionSwipes = !SwipesBeforeLift.isOn(context)
    val twoFingerSwipes = GestureLayout.handlesTwoFingerSwipes(prefs)
    val leftOut = ArrayList<String>()
    val exercises = ArrayList<Exercise>()
    when (group) {
      TrainerGroup.ONE_FINGER -> {
        exercises += swipes(1)
        exercises += taps(1, AccessibilityService.GESTURE_DOUBLE_TAP, 2, hold = false)
        exercises += taps(1, AccessibilityService.GESTURE_DOUBLE_TAP_AND_HOLD, 2, hold = true)
        exercises += swipes(1).map(::andHold)
        if (twoDirectionSwipes) {
          exercises += backAndForth()
          exercises += backAndForth().map(::andHold)
          exercises += oneFingerAngles()
          exercises += oneFingerAngles().map(::andHold)
        } else {
          leftOut +=
            context.getString(
              R.string.gesture_trainer_left_out_swipes_before_lift,
              context.getString(R.string.title_pref_swipes_before_lift),
            )
        }
      }
      TrainerGroup.TWO_FINGERS -> {
        exercises += taps(2, AccessibilityService.GESTURE_2_FINGER_SINGLE_TAP, 1, hold = false)
        exercises += taps(2, AccessibilityService.GESTURE_2_FINGER_DOUBLE_TAP, 2, hold = false)
        exercises += taps(2, AccessibilityService.GESTURE_2_FINGER_TRIPLE_TAP, 3, hold = false)
        exercises += taps(2, GestureManifold.GESTURE_2_FINGER_SINGLE_TAP_AND_HOLD, 1, hold = true)
        exercises +=
          taps(2, AccessibilityService.GESTURE_2_FINGER_DOUBLE_TAP_AND_HOLD, 2, hold = true)
        exercises +=
          taps(2, AccessibilityService.GESTURE_2_FINGER_TRIPLE_TAP_AND_HOLD, 3, hold = true)
        exercises +=
          Exercise(
            GestureManifold.GESTURE_2_FINGER_DOUBLE_TAP_TOP_AND_BOTTOM,
            Shape.Taps(2, 2, hold = false, half = ScreenHalf.TOP_AND_BOTTOM),
          )
        if (twoFingerSwipes) {
          exercises += swipes(2)
          if (twoDirectionSwipes) exercises += twoFingerAngles()
        } else {
          leftOut +=
            context.getString(
              R.string.gesture_trainer_left_out_two_finger_swipes,
              context.getString(R.string.title_pref_handle_two_finger_swipes),
            )
        }
      }
      TrainerGroup.THREE_FINGERS -> {
        exercises += taps(3, AccessibilityService.GESTURE_3_FINGER_SINGLE_TAP, 1, hold = false)
        exercises += taps(3, AccessibilityService.GESTURE_3_FINGER_DOUBLE_TAP, 2, hold = false)
        exercises += taps(3, AccessibilityService.GESTURE_3_FINGER_TRIPLE_TAP, 3, hold = false)
        exercises += taps(3, GestureManifold.GESTURE_3_FINGER_QUADRUPLE_TAP, 4, hold = false)
        exercises +=
          taps(3, AccessibilityService.GESTURE_3_FINGER_SINGLE_TAP_AND_HOLD, 1, hold = true)
        exercises +=
          taps(3, AccessibilityService.GESTURE_3_FINGER_DOUBLE_TAP_AND_HOLD, 2, hold = true)
        exercises +=
          taps(3, AccessibilityService.GESTURE_3_FINGER_TRIPLE_TAP_AND_HOLD, 3, hold = true)
        exercises += swipes(3)
      }
      TrainerGroup.FOUR_FINGERS -> {
        exercises += taps(4, AccessibilityService.GESTURE_4_FINGER_SINGLE_TAP, 1, hold = false)
        exercises +=
          Exercise(
            GestureManifold.GESTURE_4_FINGER_SINGLE_TAP_TOP,
            Shape.Taps(4, 1, hold = false, half = ScreenHalf.TOP),
          )
        exercises +=
          Exercise(
            GestureManifold.GESTURE_4_FINGER_SINGLE_TAP_BOTTOM,
            Shape.Taps(4, 1, hold = false, half = ScreenHalf.BOTTOM),
          )
        exercises += taps(4, AccessibilityService.GESTURE_4_FINGER_DOUBLE_TAP, 2, hold = false)
        exercises += taps(4, AccessibilityService.GESTURE_4_FINGER_TRIPLE_TAP, 3, hold = false)
        exercises +=
          taps(4, AccessibilityService.GESTURE_4_FINGER_DOUBLE_TAP_AND_HOLD, 2, hold = true)
        exercises += swipes(4)
      }
      TrainerGroup.PINCH_ROTATE_SCRUB -> {
        exercises += Exercise(GestureManifold.GESTURE_2_FINGER_PINCH, Shape.Pinch(2, false))
        exercises += Exercise(GestureManifold.GESTURE_2_FINGER_SPREAD, Shape.Pinch(2, true))
        exercises += Exercise(GestureManifold.GESTURE_3_FINGER_PINCH, Shape.Pinch(3, false))
        exercises += Exercise(GestureManifold.GESTURE_3_FINGER_SPREAD, Shape.Pinch(3, true))
        exercises += Exercise(GestureManifold.GESTURE_4_FINGER_PINCH, Shape.Pinch(4, false))
        exercises += Exercise(GestureManifold.GESTURE_4_FINGER_SPREAD, Shape.Pinch(4, true))
        exercises +=
          Exercise(GestureManifold.GESTURE_2_FINGER_ROTATE_CLOCKWISE, Shape.Rotate(true))
        exercises +=
          Exercise(GestureManifold.GESTURE_2_FINGER_ROTATE_COUNTERCLOCKWISE, Shape.Rotate(false))
        // Shown also while two-finger swipes scroll the app; its results then say how to turn
        // them on, as the scrub needs them.
        exercises += Exercise(GestureManifold.GESTURE_2_FINGER_SCRUB, Shape.Scrub)
      }
      TrainerGroup.TAP_SWIPE -> {
        for (fingers in 2..4) {
          if (fingers == 2 && !twoFingerSwipes) {
            leftOut +=
              context.getString(
                R.string.gesture_trainer_left_out_two_finger_tap_swipes,
                context.getString(R.string.title_pref_handle_two_finger_swipes),
              )
            continue
          }
          exercises += tapSwipes(fingers, swipeFingers = fingers)
        }
        exercises += tapSwipes(2, swipeFingers = 1)
      }
      TrainerGroup.THREE_PART_SWIPES ->
        if (twoDirectionSwipes) {
          exercises += threePartSwipes()
        } else {
          leftOut +=
            context.getString(
              R.string.gesture_trainer_left_out_three_part_before_lift,
              context.getString(R.string.title_pref_swipes_before_lift),
            )
        }
      TrainerGroup.TWO_STROKE_SWIPES ->
        if (twoStrokeSwipeTimeMs(context) > 0) {
          exercises += twoStrokeSwipes()
        } else {
          leftOut +=
            context.getString(
              R.string.gesture_trainer_left_out_two_stroke_off,
              context.getString(R.string.title_pref_two_stroke_swipe_time),
              context.getString(R.string.value_two_stroke_swipe_time_off),
            )
        }
      TrainerGroup.EDGE_SWIPES -> {
        exercises += edgeSlides(long = false)
        exercises += edgeSlides(long = true)
      }
      TrainerGroup.ALONG_EDGES -> {
        val taken = takenEdges(context, prefs)
        exercises += alongSlides(taken)
        leftOut += taken.map { (side, reason) -> context.getString(reason, alongEdgeName(context, side)) }
      }
      TrainerGroup.EDGE_MENUS -> {
        exercises += edgeMenu(context, prefs, MenuEdge.LEFT)
        exercises += edgeMenu(context, prefs, MenuEdge.RIGHT)
      }
      TrainerGroup.CORNERS -> exercises += cornerTaps()
      TrainerGroup.BACK_TAP -> {
        exercises += Exercise(BACK_DOUBLE_TAP_ID, Shape.BackTaps(2))
        exercises += Exercise(BACK_TRIPLE_TAP_ID, Shape.BackTaps(3))
        exercises += Exercise(BACK_QUADRUPLE_TAP_ID, Shape.BackTaps(4))
      }
      TrainerGroup.SHAKE -> exercises += Exercise(SHAKE_ID, Shape.Shake)
    }
    return ExerciseList(exercises, leftOut)
  }

  private fun swipes(fingers: Int): List<Exercise> =
    listOf(RIGHT, LEFT, DOWN, UP).map { Exercise(swipeId(fingers, it), Shape.Swipe(fingers, listOf(it))) }

  private fun swipeId(fingers: Int, direction: Direction): Int =
    when (fingers) {
      1 ->
        when (direction) {
          UP -> AccessibilityService.GESTURE_SWIPE_UP
          DOWN -> AccessibilityService.GESTURE_SWIPE_DOWN
          LEFT -> AccessibilityService.GESTURE_SWIPE_LEFT
          RIGHT -> AccessibilityService.GESTURE_SWIPE_RIGHT
        }
      2 ->
        when (direction) {
          UP -> AccessibilityService.GESTURE_2_FINGER_SWIPE_UP
          DOWN -> AccessibilityService.GESTURE_2_FINGER_SWIPE_DOWN
          LEFT -> AccessibilityService.GESTURE_2_FINGER_SWIPE_LEFT
          RIGHT -> AccessibilityService.GESTURE_2_FINGER_SWIPE_RIGHT
        }
      3 ->
        when (direction) {
          UP -> AccessibilityService.GESTURE_3_FINGER_SWIPE_UP
          DOWN -> AccessibilityService.GESTURE_3_FINGER_SWIPE_DOWN
          LEFT -> AccessibilityService.GESTURE_3_FINGER_SWIPE_LEFT
          RIGHT -> AccessibilityService.GESTURE_3_FINGER_SWIPE_RIGHT
        }
      else ->
        when (direction) {
          UP -> AccessibilityService.GESTURE_4_FINGER_SWIPE_UP
          DOWN -> AccessibilityService.GESTURE_4_FINGER_SWIPE_DOWN
          LEFT -> AccessibilityService.GESTURE_4_FINGER_SWIPE_LEFT
          RIGHT -> AccessibilityService.GESTURE_4_FINGER_SWIPE_RIGHT
        }
    }

  private fun taps(fingers: Int, gestureId: Int, taps: Int, hold: Boolean) =
    Exercise(gestureId, Shape.Taps(fingers, taps, hold))

  private fun backAndForth(): List<Exercise> =
    listOf(
      Exercise(AccessibilityService.GESTURE_SWIPE_UP_AND_DOWN, Shape.Swipe(1, listOf(UP, DOWN))),
      Exercise(AccessibilityService.GESTURE_SWIPE_DOWN_AND_UP, Shape.Swipe(1, listOf(DOWN, UP))),
      Exercise(
        AccessibilityService.GESTURE_SWIPE_LEFT_AND_RIGHT,
        Shape.Swipe(1, listOf(LEFT, RIGHT)),
      ),
      Exercise(
        AccessibilityService.GESTURE_SWIPE_RIGHT_AND_LEFT,
        Shape.Swipe(1, listOf(RIGHT, LEFT)),
      ),
    )

  private fun oneFingerAngles(): List<Exercise> =
    listOf(
      AccessibilityService.GESTURE_SWIPE_UP_AND_LEFT to listOf(UP, LEFT),
      AccessibilityService.GESTURE_SWIPE_UP_AND_RIGHT to listOf(UP, RIGHT),
      AccessibilityService.GESTURE_SWIPE_DOWN_AND_LEFT to listOf(DOWN, LEFT),
      AccessibilityService.GESTURE_SWIPE_DOWN_AND_RIGHT to listOf(DOWN, RIGHT),
      AccessibilityService.GESTURE_SWIPE_LEFT_AND_UP to listOf(LEFT, UP),
      AccessibilityService.GESTURE_SWIPE_LEFT_AND_DOWN to listOf(LEFT, DOWN),
      AccessibilityService.GESTURE_SWIPE_RIGHT_AND_UP to listOf(RIGHT, UP),
      AccessibilityService.GESTURE_SWIPE_RIGHT_AND_DOWN to listOf(RIGHT, DOWN),
    )
      .map { (id, directions) -> Exercise(id, Shape.Swipe(1, directions)) }

  private fun twoFingerAngles(): List<Exercise> =
    listOf(
      GestureManifold.GESTURE_2_FINGER_SWIPE_UP_AND_LEFT to listOf(UP, LEFT),
      GestureManifold.GESTURE_2_FINGER_SWIPE_UP_AND_RIGHT to listOf(UP, RIGHT),
      GestureManifold.GESTURE_2_FINGER_SWIPE_DOWN_AND_LEFT to listOf(DOWN, LEFT),
      GestureManifold.GESTURE_2_FINGER_SWIPE_DOWN_AND_RIGHT to listOf(DOWN, RIGHT),
      GestureManifold.GESTURE_2_FINGER_SWIPE_LEFT_AND_UP to listOf(LEFT, UP),
      GestureManifold.GESTURE_2_FINGER_SWIPE_LEFT_AND_DOWN to listOf(LEFT, DOWN),
      GestureManifold.GESTURE_2_FINGER_SWIPE_RIGHT_AND_UP to listOf(RIGHT, UP),
      GestureManifold.GESTURE_2_FINGER_SWIPE_RIGHT_AND_DOWN to listOf(RIGHT, DOWN),
    )
      .map { (id, directions) -> Exercise(id, Shape.Swipe(2, directions)) }

  /**
   * The one-finger swipes in three directions, each turning from the one before, numbered as the
   * gesture matchers number them: by first direction, then second, then third, each up, down, left,
   * right.
   */
  private fun threePartSwipes(): List<Exercise> {
    val ways = listOf(UP, DOWN, LEFT, RIGHT)
    var id = GestureManifold.GESTURE_SWIPE_UP_AND_DOWN_AND_UP
    val exercises = ArrayList<Exercise>()
    for (first in ways) {
      for (second in ways - first) {
        for (third in ways - second) {
          exercises += Exercise(id--, Shape.Swipe(1, listOf(first, second, third)))
        }
      }
    }
    return exercises
  }

  /** The two-stroke swipes of two one-direction swipes, numbered as TwoStrokeSwipeIds numbers them. */
  private fun twoStrokeSwipes(): List<Exercise> {
    val ways = listOf(UP, DOWN, LEFT, RIGHT)
    return ways.flatMap { first ->
      ways.map { second ->
        Exercise(
          TwoStrokeSwipeIds.twoStrokeSwipeOf(swipeId(1, first), swipeId(1, second)),
          Shape.TwoStroke(first, second),
        )
      }
    }
  }

  /** Time between two-stroke swipes, in ms, or 0 while they are off, as TwoStrokeSwipes takes it. */
  fun twoStrokeSwipeTimeMs(context: Context): Int {
    if (!BuildVersionUtils.isAtLeastT()) return 0
    return SharedPreferencesUtils.getIntFromStringPref(
        SharedPreferencesUtils.getSharedPreferences(context),
        context.resources,
        R.string.pref_two_stroke_swipe_time_key,
        R.string.pref_two_stroke_swipe_time_default,
      )
      .takeIf { it in TwoStrokeSwipes.TIMES_MS } ?: 0
  }

  /** Corner taps, the simplest first: every corner with one tap, then with two, and so on. */
  private fun cornerTaps(): List<Exercise> =
    CornerTapTracker.Kind.entries.flatMap { kind ->
      CORNERS.map { corner ->
        Exercise(cornerGestureId(corner, kind), Shape.CornerTaps(corner, kind.taps, kind.hold))
      }
    }

  private val CORNERS = listOf(Corner.TOP_LEFT, Corner.TOP_RIGHT, Corner.BOTTOM_LEFT, Corner.BOTTOM_RIGHT)

  private fun cornerGestureId(corner: Corner, kind: CornerTapTracker.Kind): Int =
    CORNER_GESTURE_IDS.getValue(corner).getValue(kind)

  private val CORNER_GESTURE_IDS =
    mapOf(
      Corner.TOP_LEFT to
        mapOf(
          CornerTapTracker.Kind.TAP to GestureManifold.GESTURE_CORNER_TOP_LEFT_TAP,
          CornerTapTracker.Kind.DOUBLE_TAP to GestureManifold.GESTURE_CORNER_TOP_LEFT_DOUBLE_TAP,
          CornerTapTracker.Kind.TRIPLE_TAP to GestureManifold.GESTURE_CORNER_TOP_LEFT_TRIPLE_TAP,
          CornerTapTracker.Kind.DOUBLE_TAP_AND_HOLD to
            GestureManifold.GESTURE_CORNER_TOP_LEFT_DOUBLE_TAP_AND_HOLD,
          CornerTapTracker.Kind.TRIPLE_TAP_AND_HOLD to
            GestureManifold.GESTURE_CORNER_TOP_LEFT_TRIPLE_TAP_AND_HOLD,
        ),
      Corner.TOP_RIGHT to
        mapOf(
          CornerTapTracker.Kind.TAP to GestureManifold.GESTURE_CORNER_TOP_RIGHT_TAP,
          CornerTapTracker.Kind.DOUBLE_TAP to GestureManifold.GESTURE_CORNER_TOP_RIGHT_DOUBLE_TAP,
          CornerTapTracker.Kind.TRIPLE_TAP to GestureManifold.GESTURE_CORNER_TOP_RIGHT_TRIPLE_TAP,
          CornerTapTracker.Kind.DOUBLE_TAP_AND_HOLD to
            GestureManifold.GESTURE_CORNER_TOP_RIGHT_DOUBLE_TAP_AND_HOLD,
          CornerTapTracker.Kind.TRIPLE_TAP_AND_HOLD to
            GestureManifold.GESTURE_CORNER_TOP_RIGHT_TRIPLE_TAP_AND_HOLD,
        ),
      Corner.BOTTOM_LEFT to
        mapOf(
          CornerTapTracker.Kind.TAP to GestureManifold.GESTURE_CORNER_BOTTOM_LEFT_TAP,
          CornerTapTracker.Kind.DOUBLE_TAP to GestureManifold.GESTURE_CORNER_BOTTOM_LEFT_DOUBLE_TAP,
          CornerTapTracker.Kind.TRIPLE_TAP to GestureManifold.GESTURE_CORNER_BOTTOM_LEFT_TRIPLE_TAP,
          CornerTapTracker.Kind.DOUBLE_TAP_AND_HOLD to
            GestureManifold.GESTURE_CORNER_BOTTOM_LEFT_DOUBLE_TAP_AND_HOLD,
          CornerTapTracker.Kind.TRIPLE_TAP_AND_HOLD to
            GestureManifold.GESTURE_CORNER_BOTTOM_LEFT_TRIPLE_TAP_AND_HOLD,
        ),
      Corner.BOTTOM_RIGHT to
        mapOf(
          CornerTapTracker.Kind.TAP to GestureManifold.GESTURE_CORNER_BOTTOM_RIGHT_TAP,
          CornerTapTracker.Kind.DOUBLE_TAP to GestureManifold.GESTURE_CORNER_BOTTOM_RIGHT_DOUBLE_TAP,
          CornerTapTracker.Kind.TRIPLE_TAP to GestureManifold.GESTURE_CORNER_BOTTOM_RIGHT_TRIPLE_TAP,
          CornerTapTracker.Kind.DOUBLE_TAP_AND_HOLD to
            GestureManifold.GESTURE_CORNER_BOTTOM_RIGHT_DOUBLE_TAP_AND_HOLD,
          CornerTapTracker.Kind.TRIPLE_TAP_AND_HOLD to
            GestureManifold.GESTURE_CORNER_BOTTOM_RIGHT_TRIPLE_TAP_AND_HOLD,
        ),
    )

  /** The swipe and hold version of a one-finger [swipe] exercise. */
  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  private fun andHold(swipe: Exercise): Exercise =
    Exercise(
      GestureManifold.swipeAndHoldOf(swipe.gestureId),
      (swipe.shape as Shape.Swipe).copy(hold = true),
    )

  private val TAP_SWIPE_DIRECTIONS = listOf(UP, DOWN, LEFT, RIGHT)

  /** One, two and three taps of [fingers], each then with a swipe of [swipeFingers] four ways. */
  private fun tapSwipes(fingers: Int, swipeFingers: Int): List<Exercise> =
    (1..3).flatMap { taps ->
      TAP_SWIPE_DIRECTIONS.map { direction ->
        Exercise(
          tapSwipeId(fingers, taps, swipeFingers, direction),
          Shape.TapSwipe(fingers, taps, swipeFingers, direction),
        )
      }
    }

  /**
   * The gesture of [taps] with [fingers] and then a swipe of [swipeFingers]. Each kind's ids count
   * down from its single tap and swipe up: up, down, left and right, for one tap, then two, then
   * three.
   */
  private fun tapSwipeId(fingers: Int, taps: Int, swipeFingers: Int, direction: Direction): Int {
    val singleTapSwipeUp =
      if (swipeFingers == 1) GestureManifold.GESTURE_2_FINGER_SINGLE_TAP_1_FINGER_SWIPE_UP
      else
        when (fingers) {
          2 -> GestureManifold.GESTURE_2_FINGER_SINGLE_TAP_SWIPE_UP
          3 -> GestureManifold.GESTURE_3_FINGER_SINGLE_TAP_SWIPE_UP
          else -> GestureManifold.GESTURE_4_FINGER_SINGLE_TAP_SWIPE_UP
        }
    return singleTapSwipeUp - (taps - 1) * TAP_SWIPE_DIRECTIONS.size -
      TAP_SWIPE_DIRECTIONS.indexOf(direction)
  }

  private fun edgeSlides(long: Boolean): List<Exercise> =
    listOf(
      Edge.BOTTOM to
        (if (long) GestureManifold.GESTURE_EDGE_LONG_SWIPE_UP_FROM_BOTTOM
        else GestureManifold.GESTURE_EDGE_SWIPE_UP_FROM_BOTTOM),
      Edge.TOP_LEFT to
        (if (long) GestureManifold.GESTURE_EDGE_LONG_SWIPE_DOWN_FROM_TOP_LEFT
        else GestureManifold.GESTURE_EDGE_SWIPE_DOWN_FROM_TOP_LEFT),
      Edge.TOP_RIGHT to
        (if (long) GestureManifold.GESTURE_EDGE_LONG_SWIPE_DOWN_FROM_TOP_RIGHT
        else GestureManifold.GESTURE_EDGE_SWIPE_DOWN_FROM_TOP_RIGHT),
      Edge.LEFT_TOP to
        (if (long) GestureManifold.GESTURE_EDGE_LONG_SWIPE_RIGHT_FROM_LEFT_TOP
        else GestureManifold.GESTURE_EDGE_SWIPE_RIGHT_FROM_LEFT_TOP),
      Edge.LEFT_BOTTOM to
        (if (long) GestureManifold.GESTURE_EDGE_LONG_SWIPE_RIGHT_FROM_LEFT_BOTTOM
        else GestureManifold.GESTURE_EDGE_SWIPE_RIGHT_FROM_LEFT_BOTTOM),
      Edge.RIGHT_TOP to
        (if (long) GestureManifold.GESTURE_EDGE_LONG_SWIPE_LEFT_FROM_RIGHT_TOP
        else GestureManifold.GESTURE_EDGE_SWIPE_LEFT_FROM_RIGHT_TOP),
      Edge.RIGHT_BOTTOM to
        (if (long) GestureManifold.GESTURE_EDGE_LONG_SWIPE_LEFT_FROM_RIGHT_BOTTOM
        else GestureManifold.GESTURE_EDGE_SWIPE_LEFT_FROM_RIGHT_BOTTOM),
    )
      .map { (edge, id) -> Exercise(id, Shape.EdgeSlide(edge, long)) }

  /** The number of sample items in the practice menu of an edge that has no menu. */
  const val SAMPLE_MENU_ITEMS = 4

  /**
   * Exercises in the menu along [edge]: items drawn at random from all of them, a new draw each
   * time the group opens, so that the user aims at a different item each time. An edge with no menu is
   * practised with sample items, Option 1 to Option [SAMPLE_MENU_ITEMS].
   */
  private fun edgeMenu(
    context: Context,
    prefs: android.content.SharedPreferences,
    edge: MenuEdge,
  ): List<Exercise> {
    val menu = EdgeMenus.read(prefs, edge, context.getString(R.string.shortcut_value_unassigned))
    val sample = menu.size == 0
    val names =
      if (sample) sampleMenuItems(context)
      else (0 until menu.size).map { menu.name(context, it) }
    return names.indices
      .shuffled()
      .take(MENU_EXERCISES_PER_EDGE)
      .map { Exercise(MENU_GESTURE_ID, Shape.EdgeMenu(edge, it, names, sample)) }
  }

  fun sampleMenuItems(context: Context): List<String> =
    (1..SAMPLE_MENU_ITEMS).map { context.getString(R.string.gesture_trainer_menu_option, it) }

  private const val MENU_EXERCISES_PER_EDGE = 3

  /** An edge menu choice is no gesture. */
  const val MENU_GESTURE_ID = 0

  /**
   * Back taps and the shake are no gestures of the gesture matchers, so the trainer gives them ids
   * of its own, far from the matchers' ids, which go down to two-stroke swipes near -1300.
   */
  const val BACK_DOUBLE_TAP_ID = -90002

  const val BACK_TRIPLE_TAP_ID = -90003

  const val BACK_QUADRUPLE_TAP_ID = -90004

  const val SHAKE_ID = -90010

  /** The trainer's id of [taps] on the back, or null for a number of taps that is no gesture. */
  fun backTapId(taps: Int): Int? =
    when (taps) {
      2 -> BACK_DOUBLE_TAP_ID
      3 -> BACK_TRIPLE_TAP_ID
      4 -> BACK_QUADRUPLE_TAP_ID
      else -> null
    }

  private val ALONG_SLIDES =
    listOf(
      Edge.ALONG_LEFT_UP to GestureManifold.GESTURE_EDGE_SWIPE_UP_ALONG_LEFT,
      Edge.ALONG_LEFT_DOWN to GestureManifold.GESTURE_EDGE_SWIPE_DOWN_ALONG_LEFT,
      Edge.ALONG_RIGHT_UP to GestureManifold.GESTURE_EDGE_SWIPE_UP_ALONG_RIGHT,
      Edge.ALONG_RIGHT_DOWN to GestureManifold.GESTURE_EDGE_SWIPE_DOWN_ALONG_RIGHT,
      Edge.ALONG_TOP_LEFT to GestureManifold.GESTURE_EDGE_SWIPE_LEFT_ALONG_TOP,
      Edge.ALONG_TOP_RIGHT to GestureManifold.GESTURE_EDGE_SWIPE_RIGHT_ALONG_TOP,
      Edge.ALONG_BOTTOM_LEFT to GestureManifold.GESTURE_EDGE_SWIPE_LEFT_ALONG_BOTTOM,
      Edge.ALONG_BOTTOM_RIGHT to GestureManifold.GESTURE_EDGE_SWIPE_RIGHT_ALONG_BOTTOM,
    )

  /**
   * The edges whose slides along them do something else than a gesture, each with the text that
   * says what: it has an edge menu, or it scrolls the app.
   */
  private fun takenEdges(
    context: Context,
    prefs: android.content.SharedPreferences,
  ): Map<ScreenSide, Int> {
    fun scrolls(key: Int) =
      prefs.getBoolean(
        context.getString(key),
        context.resources.getBoolean(R.bool.pref_edge_scroll_default),
      )
    val unassigned = context.getString(R.string.shortcut_value_unassigned)
    fun isMenu(edge: MenuEdge) = EdgeMenus.read(prefs, edge, unassigned).size > 0
    val taken = LinkedHashMap<ScreenSide, Int>()
    fun take(side: ScreenSide, menu: Boolean, scrollKey: Int) {
      when {
        menu -> taken[side] = R.string.gesture_trainer_left_out_along_menu
        scrolls(scrollKey) -> taken[side] = R.string.gesture_trainer_left_out_along_scroll
      }
    }
    take(ScreenSide.LEFT, isMenu(MenuEdge.LEFT), R.string.pref_edge_scroll_left_key)
    take(ScreenSide.RIGHT, isMenu(MenuEdge.RIGHT), R.string.pref_edge_scroll_right_key)
    take(ScreenSide.TOP, false, R.string.pref_edge_scroll_top_key)
    take(ScreenSide.BOTTOM, false, R.string.pref_edge_scroll_bottom_key)
    return taken
  }

  /** "the left edge" and so on, as in "along the left edge". */
  fun alongEdgeName(context: Context, side: ScreenSide): String =
    context.getString(
      when (side) {
        ScreenSide.LEFT -> R.string.gesture_trainer_along_left
        ScreenSide.RIGHT -> R.string.gesture_trainer_along_right
        ScreenSide.TOP -> R.string.gesture_trainer_along_top
        ScreenSide.BOTTOM -> R.string.gesture_trainer_along_bottom
      }
    )

  /** Slides along the edges that are not [taken]. */
  private fun alongSlides(taken: Map<ScreenSide, Int>): List<Exercise> =
    ALONG_SLIDES.filter { (edge, _) -> edge.side !in taken }
      .map { (edge, id) -> Exercise(id, Shape.EdgeSlide(edge, long = false)) }
}

/** The limits the gesture matchers and trackers use, with the user's settings, in pixels and ms. */
class TrainerLimits(
  val pxPerCm: Float,
  val touchSlopPx: Float,
  val doubleTapSlopPx: Float,
  val tapTimeoutMs: Long,
  val longPressMs: Long,
  val multiTapGapMs: Long,
  val multiFingerTapGapMs: Long,
  /** The time a one-finger swipe has to travel its first centimetre. */
  val swipeStartMs: Long,
  /** The time a one-finger swipe has to travel each further centimetre. */
  val swipeContinueMs: Long,
  val edgeZonePx: Float,
  val edgeFirstStagePx: Float,
  val edgeSecondStagePx: Float,
  val cornerSizePx: Float,
  val cornerTapMs: Long,
  val screenWidth: Int,
  val screenHeight: Int,
  /** How long the finger must stay still after a swipe for the swipe and hold. */
  val swipeHoldMs: Long = 500,
  /** How soon after the first swipe of a two-stroke swipe lifts the second must start. */
  val twoStrokeMs: Long = 1000,
) {
  fun toMm(px: Float): Float = px / pxPerCm * 10f

  /**
   * How long the last tap of [shape] must be held. Two fingers held once is TalkBack's recovery
   * gesture, which waits much longer than a long press.
   */
  fun holdMs(shape: Shape.Taps): Long =
    if (shape.fingers == 2 && shape.taps == 1) GestureConfiguration.TWO_FINGER_LONG_HOLD_MS.toLong()
    else longPressMs

  companion object {
    private const val SWIPE_START_MS_PER_CM = 150
    private const val SWIPE_CONTINUE_MS_PER_CM = 350

    fun read(context: Context, screenWidth: Int, screenHeight: Int): TrainerLimits {
      val prefs = SharedPreferencesUtils.getSharedPreferences(context)
      val res = context.resources
      val config = ViewConfiguration.get(context)
      val pxPerCm = res.displayMetrics.ydpi / GestureUtils.CM_PER_INCH
      fun mm(key: Int, default: Int) =
        SharedPreferencesUtils.getIntFromStringPref(prefs, res, key, default)
      val edgeZoneMm = mm(R.string.pref_edge_zone_mm_key, R.string.pref_edge_zone_mm_default)
      val cornerMm =
        mm(R.string.pref_corner_size_mm_key, R.string.pref_corner_size_mm_default).takeIf {
          it > 0
        } ?: edgeZoneMm
      val firstClickMm =
        maxOf(mm(R.string.pref_edge_first_click_mm_key, R.string.pref_edge_first_click_mm_default), 1)
      val secondClickMm =
        maxOf(mm(R.string.pref_edge_second_click_mm_key, R.string.pref_edge_second_click_mm_default), 1)
      val cornerTapMs =
        mm(R.string.pref_corner_tap_time_ms_key, R.string.pref_corner_tap_time_ms_default)
      val continueMs =
        if (FeatureFlagReader.invalidSwipeGestureEarlyDetection(context)) {
          SWIPE_CONTINUE_MS_PER_CM * 3
        } else {
          SWIPE_CONTINUE_MS_PER_CM
        }
      return TrainerLimits(
        pxPerCm = pxPerCm,
        touchSlopPx = config.scaledTouchSlop.toFloat(),
        doubleTapSlopPx =
          config.scaledDoubleTapSlop * FeatureFlagReader.gestureDoubleTapSlopMultiplier(context),
        tapTimeoutMs = ViewConfiguration.getTapTimeout().toLong(),
        longPressMs = ViewConfiguration.getLongPressTimeout().toLong(),
        multiTapGapMs = GestureConfiguration.getMultiTapTimeoutMs().toLong(),
        multiFingerTapGapMs = GestureConfiguration.getMultiFingerTapTimeoutMs().toLong(),
        swipeStartMs =
          GestureConfiguration.scaleSwipeStartThresholdMs(SWIPE_START_MS_PER_CM).toLong(),
        swipeContinueMs = continueMs.toLong(),
        edgeZonePx = pxPerCm * maxOf(edgeZoneMm, 1) / 10f,
        edgeFirstStagePx = pxPerCm * firstClickMm / 10f,
        // The second click setting is the way on from the first click, as EdgeSwipeTracker takes it.
        edgeSecondStagePx = pxPerCm * (firstClickMm + secondClickMm) / 10f,
        cornerSizePx = pxPerCm * maxOf(cornerMm, 1) / 10f,
        cornerTapMs = if (cornerTapMs > 0) cornerTapMs.toLong() else ViewConfiguration.getTapTimeout().toLong(),
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        swipeHoldMs = GestureConfiguration.getSwipeHoldTimeMs().toLong(),
        twoStrokeMs = TrainerExercises.twoStrokeSwipeTimeMs(context).toLong(),
      )
    }
  }
}
