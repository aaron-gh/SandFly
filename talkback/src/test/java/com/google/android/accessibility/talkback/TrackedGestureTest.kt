/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.talkback

import android.accessibilityservice.AccessibilityGestureEvent
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_RIGHT
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_UP
import android.accessibilityservice.TouchInteractionController
import android.accessibilityservice.TouchInteractionController.STATE_DRAGGING
import android.accessibilityservice.TouchInteractionController.STATE_TOUCH_INTERACTING
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Display
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_2_FINGER_PINCH
import com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_2_FINGER_SCRUB
import com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_2_FINGER_SPREAD
import com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_3_FINGER_SPREAD
import com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_4_FINGER_PINCH
import com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_EDGE_LONG_SWIPE_DOWN_FROM_TOP_RIGHT
import com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_EDGE_LONG_SWIPE_UP_FROM_BOTTOM
import com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_EDGE_LONG_SWIPE_RIGHT_FROM_LEFT_BOTTOM
import com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_EDGE_SWIPE_DOWN_FROM_TOP_LEFT
import com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_EDGE_SWIPE_DOWN_FROM_TOP_RIGHT
import com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_EDGE_SWIPE_LEFT_FROM_RIGHT_TOP
import com.google.android.accessibility.utils.gestures.GestureManifold.GESTURE_EDGE_SWIPE_UP_FROM_BOTTOM
import java.time.Duration
import kotlin.math.cos
import kotlin.math.sin
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * Runs the gestures that Backtalk recognizes while the fingers are down, the edge gestures, the
 * two-finger scrub and pinches and spreads, through the monitor, with Android's own
 * TouchInteractionController passing it the touch, as TouchInteractionMonitorTest does. The screen
 * is 320 by 470 pixels at 160 dpi, so a centimeter is 63 pixels: edge gestures start within 31
 * pixels of the edge, and reach their stages 63 and 315 pixels in.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w320dp-h470dp-mdpi")
class TrackedGestureTest {
  /** Records the gestures that reach the service. */
  class RecordingService : TalkBackService() {
    val gestures = mutableListOf<Int>()
    var mapping: GestureShortcutMapping? = null

    override fun onGesture(accessibilityGestureEvent: AccessibilityGestureEvent): Boolean {
      gestures.add(accessibilityGestureEvent.gestureId)
      return true
    }

    override fun onGestureDetectionStarted() {}

    override fun getGestureShortcutMapping(): GestureShortcutMapping? = mapping
  }

  private val main = Handler(Looper.getMainLooper())
  private val mainLooper: ShadowLooper = shadowOf(Looper.getMainLooper())
  private lateinit var service: RecordingService
  private lateinit var controller: TouchInteractionController
  private lateinit var monitor: TouchInteractionMonitor
  private var start = 0L
  private var downTime = 0L

  @Before
  fun setUp() {
    service = Robolectric.buildService(RecordingService::class.java).get()
    service.mapping = GestureShortcutMapping(service)
    controller =
      TouchInteractionController::class
        .java
        .getDeclaredConstructor(
          AccessibilityService::class.java,
          Any::class.java,
          Int::class.javaPrimitiveType,
        )
        .apply { isAccessible = true }
        .newInstance(service, Any(), Display.DEFAULT_DISPLAY)
    val display =
      service.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
    monitor =
      TouchInteractionMonitor(
        display,
        SharedPreferencesUtils.getSharedPreferences(service),
        controller,
        service,
        PrimesController(),
        TouchExplorationModeFailureReporter(object : TalkBackAnalytics {}),
        {},
        {},
      )
    monitor.setMultiFingerGesturesEnabled(true)
    monitor.setTwoFingerPassthroughEnabled(true)
    monitor.setServiceHandlesDoubleTap(true)
    controller.registerCallback(/* executor= */ null, monitor)
    TouchInteractionController::class.java.getDeclaredField("mServiceDetectsGestures").apply {
      isAccessible = true
      setBoolean(controller, true)
    }
    start = SystemClock.uptimeMillis()
  }

  @After
  fun tearDown() {
    monitor.stop()
    controller.unregisterCallback(monitor)
  }

  private fun callController(name: String, type: Class<*>, argument: Any) {
    TouchInteractionController::class
      .java
      .getDeclaredMethod(name, type)
      .apply { isAccessible = true }
      .invoke(controller, argument)
  }

  private fun stateChangeAt(state: Int, timeMs: Long) {
    main.postAtTime(
      { callController("onStateChanged", Int::class.javaPrimitiveType!!, state) },
      start + timeMs,
    )
  }

  /** The fingers are at [points], as x and y pairs, [timeMs] after the start. */
  private fun touchAt(action: Int, timeMs: Long, vararg points: Float, actionIndex: Int = 0) {
    val eventTime = start + timeMs
    if (action == ACTION_DOWN) {
      downTime = eventTime
    }
    val count = points.size / 2
    val properties =
      Array(count) { i ->
        MotionEvent.PointerProperties().apply {
          id = i
          toolType = MotionEvent.TOOL_TYPE_FINGER
        }
      }
    val coords =
      Array(count) { i ->
        MotionEvent.PointerCoords().apply {
          x = points[i * 2]
          y = points[i * 2 + 1]
          pressure = 1f
          size = 1f
        }
      }
    val fullAction =
      if (action == ACTION_POINTER_DOWN || action == ACTION_POINTER_UP) {
        action or (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
      } else {
        action
      }
    val event =
      MotionEvent.obtain(
        downTime,
        eventTime,
        fullAction,
        count,
        properties,
        coords,
        0,
        0,
        1f,
        1f,
        0,
        0,
        0,
        0,
      )
    main.postAtTime({ callController("onMotionEvent", MotionEvent::class.java, event) }, eventTime)
  }

  private fun runUntil(timeMs: Long) {
    mainLooper.idleFor(Duration.ofMillis(start + timeMs - SystemClock.uptimeMillis()))
  }

  /**
   * One finger rests at ([x], [fromY]) for [restMs], moving [driftX] pixels sideways meanwhile,
   * then slides to each y in [toY] in turn, in 8 steps of [stepMs] each, and lifts.
   */
  private fun slide(
    x: Float,
    fromY: Float,
    vararg toY: Float,
    stepMs: Long = 10,
    restMs: Long = 0,
    driftX: Float = 0f,
  ) {
    stateChangeAt(STATE_TOUCH_INTERACTING, 0)
    touchAt(ACTION_DOWN, 0, x, fromY)
    var time = 0L
    var y = fromY
    var currentX = x
    val restSteps = restMs / 10
    while (time + 10 <= restMs) {
      time += 10
      currentX += driftX / restSteps
      touchAt(ACTION_MOVE, time, currentX, y)
    }
    for (target in toY) {
      val steps = 8
      val stepY = (target - y) / steps
      repeat(steps) {
        time += stepMs
        y += stepY
        touchAt(ACTION_MOVE, time, currentX, y)
      }
    }
    time += 10
    touchAt(ACTION_UP, time, currentX, y)
    runUntil(time + 500)
  }

  /**
   * One finger lands at ([fromX], [y]), slides to each x in [toX] in turn, in 8 steps of 10 ms
   * each, and lifts.
   */
  private fun slideSideways(y: Float, fromX: Float, vararg toX: Float) {
    stateChangeAt(STATE_TOUCH_INTERACTING, 0)
    touchAt(ACTION_DOWN, 0, fromX, y)
    var time = 0L
    var x = fromX
    for (target in toX) {
      val stepX = (target - x) / 8
      repeat(8) {
        time += 10
        x += stepX
        touchAt(ACTION_MOVE, time, x, y)
      }
    }
    time += 10
    touchAt(ACTION_UP, time, x, y)
    runUntil(time + 500)
  }

  @Test
  fun swipeUpFromTheBottomEdgeIsTheFirstStage() {
    slide(160f, 465f, 380f)
    assertEquals(listOf(GESTURE_EDGE_SWIPE_UP_FROM_BOTTOM), service.gestures)
  }

  @Test
  fun slowSwipeUpFromTheBottomEdgeKeepsTouchExplorationWaiting() {
    // Reaches the first stage 180 ms after the touch, after touch exploration would start.
    slide(160f, 465f, 380f, stepMs = 30)
    assertEquals(listOf(GESTURE_EDGE_SWIPE_UP_FROM_BOTTOM), service.gestures)
  }

  @Test
  fun driftingSidewaysAtTheEdgeBeforeSlidingIsAnEdgeGesture() {
    // A finger that lands partly off the screen is reported at the edge until enough of it is on.
    slide(160f, 465f, 380f, restMs = 120, driftX = -15f)
    assertEquals(listOf(GESTURE_EDGE_SWIPE_UP_FROM_BOTTOM), service.gestures)
  }

  @Test
  fun restingAtTheEdgeBeforeSlidingIsNotAnEdgeGesture() {
    slide(160f, 465f, 380f, restMs = 250)
    assertEquals(false, service.gestures.contains(GESTURE_EDGE_SWIPE_UP_FROM_BOTTOM))
  }

  @Test
  fun longSwipeUpFromTheBottomEdgeIsTheSecondStage() {
    slide(160f, 465f, 130f)
    assertEquals(listOf(GESTURE_EDGE_LONG_SWIPE_UP_FROM_BOTTOM), service.gestures)
  }

  @Test
  fun slidingBackToTheSecondStageAndLiftingIsTheFirstStage() {
    slide(160f, 465f, 130f, 380f)
    assertEquals(listOf(GESTURE_EDGE_SWIPE_UP_FROM_BOTTOM), service.gestures)
  }

  @Test
  fun slidingBackToTheEdgeCancels() {
    slide(160f, 465f, 380f, 462f)
    assertEquals(listOf<Int>(), service.gestures)
  }

  @Test
  fun swipeDownFromTheTopIsBySide() {
    slide(60f, 5f, 90f)
    slide(260f, 5f, 90f)
    assertEquals(
      listOf(GESTURE_EDGE_SWIPE_DOWN_FROM_TOP_LEFT, GESTURE_EDGE_SWIPE_DOWN_FROM_TOP_RIGHT),
      service.gestures,
    )
  }

  @Test
  fun longSwipeDownFromTheTopIsTheSecondStage() {
    assign(R.string.shortcut_value_home, R.string.pref_shortcut_edge_top_right_long_down_key)
    slide(260f, 5f, 90f, 360f)
    assertEquals(listOf(GESTURE_EDGE_LONG_SWIPE_DOWN_FROM_TOP_RIGHT), service.gestures)
  }

  @Test
  fun swipeInFromASideEdgeIsByHalf() {
    assign(
      R.string.shortcut_value_home,
      R.string.pref_shortcut_edge_right_top_left_key,
      R.string.pref_shortcut_edge_left_bottom_long_right_key,
    )
    slideSideways(100f, 315f, 230f)
    slideSideways(400f, 5f, 90f, 330f)
    assertEquals(
      listOf(GESTURE_EDGE_SWIPE_LEFT_FROM_RIGHT_TOP, GESTURE_EDGE_LONG_SWIPE_RIGHT_FROM_LEFT_BOTTOM),
      service.gestures,
    )
  }

  @Test
  fun swipeFromAnEdgeWhoseSwipeDoesNothingIsAnOrdinarySwipe() {
    assign(R.string.shortcut_value_home, R.string.pref_shortcut_edge_left_bottom_long_right_key)
    slideSideways(400f, 5f, 90f, 200f)
    slideSideways(400f, 5f, 90f, 330f)
    assertEquals(
      listOf(GESTURE_SWIPE_RIGHT, GESTURE_EDGE_LONG_SWIPE_RIGHT_FROM_LEFT_BOTTOM),
      service.gestures,
    )
  }

  @Test
  fun swipeRightFromAnUnassignedSideEdgeIsAnOrdinarySwipe() {
    slideSideways(400f, 5f, 90f, 200f)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT), service.gestures)
  }

  @Test
  fun longSwipeDownFromTheTopIsTheFirstStageWhenTheLongSwipeDoesNothing() {
    slide(60f, 5f, 90f, 360f)
    assertEquals(listOf(GESTURE_EDGE_SWIPE_DOWN_FROM_TOP_LEFT), service.gestures)
  }

  @Test
  fun swipeUpAwayFromTheEdgeIsAnOrdinarySwipe() {
    slide(160f, 400f, 300f)
    assertEquals(listOf(GESTURE_SWIPE_UP), service.gestures)
  }

  @Test
  fun unassignedEdgeLeavesAnOrdinarySwipe() {
    val unassigned = service.getString(R.string.shortcut_value_unassigned)
    SharedPreferencesUtils.getSharedPreferences(service)
      .edit()
      .putString(service.getString(R.string.pref_shortcut_edge_bottom_up_key), unassigned)
      .putString(service.getString(R.string.pref_shortcut_edge_bottom_long_up_key), unassigned)
      .commit()
    service.mapping = GestureShortcutMapping(service)
    slide(160f, 465f, 380f)
    assertEquals(listOf(GESTURE_SWIPE_UP), service.gestures)
  }

  /**
   * Two fingers 60 pixels apart move to each x in [xs] in turn, 8 steps of 10 ms each. If
   * [draggingAtMs] is set, the system passes the touch to the app as a drag then, as it does soon
   * after Backtalk asks it to.
   */
  private fun twoFingerMove(vararg xs: Float, draggingAtMs: Long? = null) {
    stateChangeAt(STATE_TOUCH_INTERACTING, 0)
    draggingAtMs?.let { stateChangeAt(STATE_DRAGGING, it) }
    var x = xs[0]
    val y = 200f
    touchAt(ACTION_DOWN, 0, x, y)
    touchAt(ACTION_POINTER_DOWN, 5, x, y, x, y + 60f, actionIndex = 1)
    var time = 5L
    for (target in xs.drop(1)) {
      val stepX = (target - x) / 8
      repeat(8) {
        time += 10
        x += stepX
        touchAt(ACTION_MOVE, time, x, y, x, y + 60f)
      }
    }
    time += 10
    touchAt(ACTION_POINTER_UP, time, x, y, x, y + 60f, actionIndex = 1)
    touchAt(ACTION_UP, time + 5, x, y)
    runUntil(time + 500)
  }

  @Test
  fun oneWayAndBackIsAScrub() {
    twoFingerMove(100f, 200f, 100f, draggingAtMs = 40)
    assertEquals(listOf(GESTURE_2_FINGER_SCRUB), service.gestures)
  }

  @Test
  fun sidewaysTwoFingerSwipeScrollsTheApp() {
    twoFingerMove(100f, 220f)
    assertEquals(true, requestedDragging())
    assertEquals(false, service.gestures.contains(GESTURE_2_FINGER_SCRUB))
  }

  /** Whether Backtalk asked the system to pass the touch to the app as a drag. */
  private fun requestedDragging(): Boolean {
    val requests =
      TouchInteractionMonitor::class
        .java
        .getDeclaredField("callerInfos")
        .apply { isAccessible = true }
        .get(monitor) as Collection<*>
    return requests.any { request ->
      request!!.javaClass.getDeclaredField("state").apply { isAccessible = true }.getInt(request) ==
        STATE_DRAGGING
    }
  }

  @Test
  fun moreStrokesAreStillOneScrub() {
    twoFingerMove(100f, 200f, 100f, 200f, draggingAtMs = 40)
    assertEquals(listOf(GESTURE_2_FINGER_SCRUB), service.gestures)
  }

  /** Assigns [action] to the gestures with the setting keys [keyIds]. */
  private fun assign(action: Int, vararg keyIds: Int) {
    val editor = SharedPreferencesUtils.getSharedPreferences(service).edit()
    for (keyId in keyIds) {
      editor.putString(service.getString(keyId), service.getString(action))
    }
    editor.commit()
    service.mapping = GestureShortcutMapping(service)
  }

  /**
   * [count] fingers land one by one around the middle of the screen, [fromRadius] pixels from it,
   * move to [toRadius] in 8 steps of 10 ms, turning by [turnDegrees] and all moving [shiftX]
   * pixels, and lift.
   */
  private fun fingersMove(
    count: Int,
    fromRadius: Float,
    toRadius: Float,
    shiftX: Float = 0f,
    turnDegrees: Float = 0f,
  ) {
    fun points(radius: Float, shift: Float, turn: Float, fingers: Int): FloatArray {
      val result = FloatArray(fingers * 2)
      for (i in 0 until fingers) {
        val angle = Math.toRadians((360.0 * i / count) + 45.0 + turn)
        result[i * 2] = 160f + shift + radius * cos(angle).toFloat()
        result[i * 2 + 1] = 235f + radius * sin(angle).toFloat()
      }
      return result
    }
    stateChangeAt(STATE_TOUCH_INTERACTING, 0)
    var time = 0L
    touchAt(ACTION_DOWN, time, *points(fromRadius, 0f, 0f, 1))
    for (fingers in 2..count) {
      time += 5
      touchAt(ACTION_POINTER_DOWN, time, *points(fromRadius, 0f, 0f, fingers), actionIndex = fingers - 1)
    }
    var last = points(fromRadius, 0f, 0f, count)
    for (step in 1..8) {
      time += 10
      last =
        points(
          fromRadius + (toRadius - fromRadius) * step / 8,
          shiftX * step / 8,
          turnDegrees * step / 8,
          count,
        )
      touchAt(ACTION_MOVE, time, *last)
    }
    for (fingers in count downTo 2) {
      time += 5
      touchAt(ACTION_POINTER_UP, time, *last.copyOf(fingers * 2), actionIndex = fingers - 1)
    }
    touchAt(ACTION_UP, time + 5, *last.copyOf(2))
    runUntil(time + 500)
  }

  @Test
  fun unassignedPinchIsNotAGesture() {
    fingersMove(2, 100f, 50f)
    assertEquals(false, service.gestures.contains(GESTURE_2_FINGER_PINCH))
  }

  @Test
  fun twoFingerPinchAndSpread() {
    assign(
      R.string.shortcut_value_home,
      R.string.pref_shortcut_2finger_pinch_key,
      R.string.pref_shortcut_2finger_spread_key,
    )
    fingersMove(2, 100f, 50f)
    fingersMove(2, 60f, 110f)
    assertEquals(listOf(GESTURE_2_FINGER_PINCH, GESTURE_2_FINGER_SPREAD), service.gestures)
  }

  @Test
  fun threeFingerSpread() {
    assign(R.string.shortcut_value_home, R.string.pref_shortcut_3finger_spread_key)
    fingersMove(3, 60f, 110f)
    assertEquals(listOf(GESTURE_3_FINGER_SPREAD), service.gestures)
  }

  @Test
  fun fourFingerPinch() {
    assign(R.string.shortcut_value_home, R.string.pref_shortcut_4finger_pinch_key)
    fingersMove(4, 110f, 55f)
    assertEquals(listOf(GESTURE_4_FINGER_PINCH), service.gestures)
  }

  @Test
  fun twoFingersMovingTogetherAreNotAPinch() {
    assign(R.string.shortcut_value_home, R.string.pref_shortcut_2finger_pinch_key)
    fingersMove(2, 80f, 80f, shiftX = 120f)
    assertEquals(false, service.gestures.contains(GESTURE_2_FINGER_PINCH))
  }

  @Test
  fun twoFingersTurningAreNotAPinch() {
    assign(R.string.shortcut_value_home, R.string.pref_shortcut_2finger_spread_key)
    fingersMove(2, 70f, 75f, turnDegrees = 90f)
    assertEquals(false, service.gestures.contains(GESTURE_2_FINGER_SPREAD))
  }
}
