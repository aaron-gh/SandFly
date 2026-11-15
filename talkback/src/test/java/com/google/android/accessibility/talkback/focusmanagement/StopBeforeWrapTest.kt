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

package com.google.android.accessibility.talkback.focusmanagement

import android.view.accessibility.AccessibilityWindowInfo
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.compositor.Compositor
import com.google.android.accessibility.utils.Filter
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_FORWARD
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * At the last window, a swipe stops once before wrapping around with Stop at the last item before
 * wrapping on, and goes on around at once with it off.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StopBeforeWrapTest {
  private lateinit var service: TalkBackService
  private lateinit var processor: FocusProcessorForLogicalNavigation
  private val window = AccessibilityWindowInfo.obtain()

  /** One window, the last; counts the searches for the window after it. */
  private inner class LastWindow : WindowTraversal(service) {
    var nextWindowSearches = 0

    override fun isLastWindow(
      baseWindow: AccessibilityWindowInfo,
      windowFilter: Filter<AccessibilityWindowInfo>,
    ) = true

    override fun getNextWindow(currentWindow: AccessibilityWindowInfo): AccessibilityWindowInfo? {
      nextWindowSearches++
      return null
    }
  }

  @Before
  fun setUp() {
    service = Robolectric.buildService(TalkBackService::class.java).get()
    FormFactorUtils.initialize(service)
    processor =
      FocusProcessorForLogicalNavigation(
        service,
        null,
        AccessibilityFocusMonitor(service, null, null),
        null,
        null,
        Compositor.TextComposer { _, _, _ -> null },
        null,
      )
    FocusProcessorForLogicalNavigation.setWrapAround(true)
  }

  @After
  fun tearDown() {
    FocusProcessorForLogicalNavigation.setWrapAround(true)
    FocusProcessorForLogicalNavigation.setStopBeforeWrap(true)
  }

  private fun search(traversal: WindowTraversal) =
    processor.searchTargetInNextOrPreviousWindow(
      null,
      traversal,
      /* isScreenRtl= */ false,
      null,
      window,
      SEARCH_FOCUS_FORWARD,
      null,
      /* isInitialFocus= */ false,
      all(),
      all(),
    )

  private fun <T> all(): Filter<T> =
    object : Filter<T>() {
      override fun accept(obj: T) = true
    }

  private fun reachEdge(): Boolean = ReflectionHelpers.getField(processor, "reachEdge")

  private fun stoppedAtEdge(): Boolean = ReflectionHelpers.getField(processor, "stoppedAtEdge")

  @Test
  fun stopBeforeWrapOn_theFirstSwipeStopsAndTheNextGoesOn() {
    val traversal = LastWindow()
    assertNull(search(traversal))
    assertTrue(reachEdge())
    assertEquals(0, traversal.nextWindowSearches)

    search(traversal)
    assertEquals(1, traversal.nextWindowSearches)
  }

  @Test
  fun stopBeforeWrapOff_theFirstSwipeGoesOn() {
    FocusProcessorForLogicalNavigation.setStopBeforeWrap(false)
    val traversal = LastWindow()
    search(traversal)
    assertTrue(reachEdge())
    assertFalse(stoppedAtEdge())
    assertEquals(1, traversal.nextWindowSearches)
  }

  @Test
  fun wrapAroundOff_stopsWhateverStopBeforeWrapIs() {
    FocusProcessorForLogicalNavigation.setWrapAround(false)
    FocusProcessorForLogicalNavigation.setStopBeforeWrap(false)
    val traversal = LastWindow()
    assertNull(search(traversal))
    assertTrue(stoppedAtEdge())
    assertEquals(0, traversal.nextWindowSearches)

    search(traversal)
    assertEquals(0, traversal.nextWindowSearches)
  }
}
