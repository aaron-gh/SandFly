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
import android.view.MotionEvent
import com.google.android.accessibility.talkback.gesture.EdgeMenus
import com.google.android.accessibility.talkback.gesture.EdgeSwipeTracker.MenuEdge
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureTrainerHooksTest {
  @After
  fun clearListener() {
    GestureTrainerHooks.listener = null
  }

  @Test
  fun withNoListener_takesNothing() {
    assertFalse(GestureTrainerHooks.isWaitingForAttempts())
    assertFalse(GestureTrainerHooks.isPractised(AccessibilityService.GESTURE_DOUBLE_TAP))
    assertFalse(GestureTrainerHooks.isPractised(0))
    assertFalse(GestureTrainerHooks.onGesture(AccessibilityService.GESTURE_SWIPE_RIGHT))
    assertNull(GestureTrainerHooks.practiceMenuItems(MenuEdge.LEFT))
    // Without a listener these return without effect.
    GestureTrainerHooks.onTouchInteractionEnded(timeMs = 100, explored = true)
    GestureTrainerHooks.onEdgeMenuChoice(MenuEdge.RIGHT, 1)
  }

  @Test
  fun withAListener_takesThePractisedGesture() {
    val taken = ArrayList<Int>()
    GestureTrainerHooks.listener =
      object : GestureTrainerHooks.Listener {
        override val practisedGestureId = AccessibilityService.GESTURE_SWIPE_UP

        override fun onTouch(event: MotionEvent) {}

        override fun onGesture(gestureId: Int): Boolean {
          taken += gestureId
          return true
        }
      }
    assertTrue(GestureTrainerHooks.isWaitingForAttempts())
    assertTrue(GestureTrainerHooks.isPractised(AccessibilityService.GESTURE_SWIPE_UP))
    assertFalse(GestureTrainerHooks.isPractised(AccessibilityService.GESTURE_SWIPE_DOWN))
    assertTrue(GestureTrainerHooks.onGesture(AccessibilityService.GESTURE_SWIPE_DOWN))
    assertEquals(listOf(AccessibilityService.GESTURE_SWIPE_DOWN), taken)
  }

  @Test
  fun sampleEdgeMenu_runsNoCommands() {
    val menu = EdgeMenus.Menu.sample(listOf("Option 1", "Option 2"))
    assertEquals(2, menu.size)
    assertNull(menu.command(0))
    assertNull(menu.command(1))
    assertEquals("Option 1", EdgeMenus.Menu(listOf("Option 1")).command(0))
  }
}
