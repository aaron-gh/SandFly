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

package com.google.android.accessibility.talkback.actor

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.graphics.Region
import android.hardware.display.DisplayManager
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import com.google.android.accessibility.talkback.Pipeline
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Runs pass-through mode with the pass-through region and the feedback pipeline replaced by
 * recordings. The main looper is paused, so each test decides when time passes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PassThroughModeActorTest {
  /** Records the pass-through regions set, and the threads that set them. */
  class RecordingService : AccessibilityService() {
    val regions = mutableListOf<Rect>()
    val threads = mutableSetOf<Thread>()

    override fun setTouchExplorationPassthroughRegion(displayId: Int, region: Region) {
      threads.add(Thread.currentThread())
      regions.add(region.bounds)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {}

    override fun onInterrupt() {}
  }

  private val mainThread: Thread = Looper.getMainLooper().thread
  private lateinit var service: RecordingService
  private lateinit var actor: PassThroughModeActor
  private val feedbackThreads = mutableSetOf<Thread>()
  private lateinit var fullScreen: Rect

  @Before
  fun setUp() {
    service = Robolectric.buildService(RecordingService::class.java).get()
    actor = PassThroughModeActor(service)
    actor.setPipeline(
      Pipeline.FeedbackReturner {
        feedbackThreads.add(Thread.currentThread())
        true
      }
    )
    val metrics = DisplayMetrics()
    service
      .getSystemService(DisplayManager::class.java)
      .getDisplay(Display.DEFAULT_DISPLAY)
      .getRealMetrics(metrics)
    fullScreen = Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
  }

  private fun pass(ms: Long) {
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
  }

  @Test
  fun theModeEndsOnTheMainThreadWhenNoTouchStartsWithin4Seconds() {
    actor.setTouchExplorePassThrough(true)
    assertEquals(listOf(fullScreen), service.regions)

    pass(3999)
    assertTrue(actor.state.isPassThroughModeActive)
    pass(1)
    assertFalse(actor.state.isPassThroughModeActive)
    assertEquals(listOf(fullScreen, Rect()), service.regions)
    assertEquals(setOf(mainThread), service.threads)
    assertEquals(setOf(mainThread), feedbackThreads)
  }

  @Test
  fun turningTheModeOnAgainStartsThe4SecondsOver() {
    actor.setTouchExplorePassThrough(true)
    pass(3000)
    actor.setTouchExplorePassThrough(true)

    pass(3999)
    assertTrue(actor.state.isPassThroughModeActive)
    pass(1)
    assertFalse(actor.state.isPassThroughModeActive)
  }

  @Test
  fun turningTheModeOffDropsTheExitStillPending() {
    actor.setTouchExplorePassThrough(true)
    pass(1000)
    actor.setTouchExplorePassThrough(false)
    assertEquals(listOf(fullScreen, Rect()), service.regions)

    // Another region, such as the circle menu's, stays as it is.
    pass(5000)
    assertEquals(listOf(fullScreen, Rect()), service.regions)
  }

  @Test
  fun aTouchThatStartsKeepsTheModeOn() {
    actor.setTouchExplorePassThrough(true)
    pass(1000)
    actor.cancelPassThroughGuardTimer()

    pass(5000)
    assertTrue(actor.state.isPassThroughModeActive)
    assertEquals(listOf(fullScreen), service.regions)
  }
}
