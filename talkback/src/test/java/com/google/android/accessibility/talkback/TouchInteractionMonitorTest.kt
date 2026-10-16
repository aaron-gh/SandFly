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
import android.accessibilityservice.AccessibilityService.GESTURE_DOUBLE_TAP
import android.accessibilityservice.AccessibilityService.GESTURE_SWIPE_RIGHT
import android.accessibilityservice.TouchInteractionController
import android.accessibilityservice.TouchInteractionController.STATE_CLEAR
import android.accessibilityservice.TouchInteractionController.STATE_TOUCH_INTERACTING
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Display
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.ViewConfiguration
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.time.Duration
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
import org.robolectric.shadows.ShadowSystemClock

/**
 * Runs the monitor with Android's own TouchInteractionController passing it touch state changes and
 * motion events on the main thread, as Android does. The main looper is paused, so each test
 * decides when it runs, and can keep it busy while the user's finger moves on.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TouchInteractionMonitorTest {
  /** Records the gestures and other results that reach the service, and their threads. */
  class RecordingService : TalkBackService() {
    val gestures = mutableListOf<Int>()
    val threads = mutableSetOf<Thread>()

    override fun onGesture(accessibilityGestureEvent: AccessibilityGestureEvent): Boolean {
      threads.add(Thread.currentThread())
      gestures.add(accessibilityGestureEvent.gestureId)
      return true
    }

    override fun onGestureDetectionStarted() {
      threads.add(Thread.currentThread())
    }
  }

  /** Records the threads that ask the controller for touch exploration. */
  private class RecordingReporter :
    TouchExplorationModeFailureReporter(object : TalkBackAnalytics {}) {
    val requests = mutableListOf<Thread>()

    override fun onRequestTouchExploration() {
      requests.add(Thread.currentThread())
      super.onRequestTouchExploration()
    }
  }

  private val mainThread: Thread = Looper.getMainLooper().thread
  private val main = Handler(Looper.getMainLooper())
  private val mainLooper: ShadowLooper = shadowOf(Looper.getMainLooper())
  private val reporter = RecordingReporter()
  private val provisioned = mutableListOf<Boolean>()
  private val provisionThreads = mutableListOf<Thread>()
  private lateinit var service: RecordingService
  private lateinit var controller: TouchInteractionController
  private lateinit var monitor: TouchInteractionMonitor
  private var start = 0L
  private var downTime = 0L

  @Before
  fun setUp() {
    service = Robolectric.buildService(RecordingService::class.java).get()
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
    // As TalkBackService.registerGestureDetection() sets the monitor up and registers it: with no
    // executor, the controller calls the monitor on the main thread as each event comes in.
    monitor =
      TouchInteractionMonitor(
        display,
        SharedPreferencesUtils.getSharedPreferences(service),
        controller,
        service,
        PrimesController(),
        reporter,
        { enabled ->
          provisionThreads.add(Thread.currentThread())
          provisioned.add(enabled)
        },
        {},
      )
    monitor.setMultiFingerGesturesEnabled(true)
    monitor.setTwoFingerPassthroughEnabled(true)
    monitor.setServiceHandlesDoubleTap(true)
    controller.registerCallback(/* executor= */ null, monitor)
    // Without a connection to Android, the controller would refuse every state change.
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

  /** Android changes the touch state [timeMs] after the start, through the main thread. */
  private fun stateChangeAt(state: Int, timeMs: Long) {
    main.postAtTime(
      { callController("onStateChanged", Int::class.javaPrimitiveType!!, state) },
      start + timeMs,
    )
  }

  /** The finger touches at ([x], [y]) [timeMs] after the start; Android passes it on through the main thread. */
  private fun touchAt(action: Int, x: Float, y: Float, timeMs: Long) {
    val eventTime = start + timeMs
    if (action == ACTION_DOWN) {
      downTime = eventTime
    }
    val event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0)
    main.postAtTime({ callController("onMotionEvent", MotionEvent::class.java, event) }, eventTime)
  }

  /** Runs the main thread until [timeMs] after the start. */
  private fun runUntil(timeMs: Long) {
    mainLooper.idleFor(Duration.ofMillis(start + timeMs - SystemClock.uptimeMillis()))
  }

  /** Lets time pass until [timeMs] after the start while the main thread is busy with other work. */
  private fun mainThreadBusyUntil(timeMs: Long) {
    ShadowSystemClock.advanceBy(Duration.ofMillis(start + timeMs - SystemClock.uptimeMillis()))
  }

  @Test
  fun fingerHeldStillAsksForTouchExplorationOnTheMainThreadAfterTheDelay() {
    stateChangeAt(STATE_TOUCH_INTERACTING, 0)
    touchAt(ACTION_DOWN, 160f, 200f, 0)

    runUntil(140)
    assertEquals(listOf<Thread>(), reporter.requests)
    runUntil(200)
    assertEquals(listOf(mainThread), reporter.requests)
  }

  @Test
  fun swipeThatWaitedOnTheBusyMainThreadIsAGestureRatherThanTouchExploration() {
    stateChangeAt(STATE_TOUCH_INTERACTING, 0)
    touchAt(ACTION_DOWN, 40f, 200f, 0)
    runUntil(10)
    // The finger swipes right while the main thread is busy for longer than the touch exploration
    // delay. The delay ends after the moves came in, so the moves come first.
    for (step in 1..6) {
      touchAt(ACTION_MOVE, 40f + 40f * step, 200f, 10L + 15 * step)
    }
    touchAt(ACTION_UP, 280f, 200f, 110)
    mainThreadBusyUntil(400)
    runUntil(450)

    assertEquals(listOf<Thread>(), reporter.requests)
    assertEquals(listOf(GESTURE_SWIPE_RIGHT), service.gestures)
    assertEquals(setOf(mainThread), service.threads)
  }

  @Test
  fun doubleTapThatWaitedOnTheBusyMainThreadIsNotTakenForAHold() {
    stateChangeAt(STATE_TOUCH_INTERACTING, 0)
    touchAt(ACTION_DOWN, 160f, 200f, 0)
    touchAt(ACTION_UP, 160f, 200f, 50)
    touchAt(ACTION_DOWN, 160f, 200f, 150)
    runUntil(160)
    // The finger lifts 50 ms after it went down again, while the main thread is busy until after
    // the hold would have completed. A hold goes to the controller, not to the service.
    touchAt(ACTION_UP, 160f, 200f, 200)
    val holdEnds = 150 + ViewConfiguration.getLongPressTimeout().toLong()
    mainThreadBusyUntil(holdEnds + 100)
    runUntil(holdEnds + 110)

    assertEquals(listOf(GESTURE_DOUBLE_TAP), service.gestures)
    assertEquals(setOf(mainThread), service.threads)
  }

  @Test
  fun touchExplorationTurnedOffDuringATouchChangesWhenTheTouchEnds() {
    stateChangeAt(STATE_TOUCH_INTERACTING, 0)
    touchAt(ACTION_DOWN, 160f, 200f, 0)
    runUntil(10)

    monitor.requestA11yTouchExploreState(false)
    runUntil(20)
    assertEquals(listOf<Boolean>(), provisioned)

    touchAt(ACTION_UP, 160f, 200f, 50)
    stateChangeAt(STATE_CLEAR, 50)
    runUntil(60)
    assertEquals(listOf(false), provisioned)
    assertEquals(listOf(mainThread), provisionThreads)
  }
}
