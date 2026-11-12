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

import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.compositor.Compositor
import com.google.android.accessibility.talkback.focusmanagement.action.NavigationAction
import com.google.android.accessibility.utils.Filter
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_BACKWARD
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_FORWARD
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * With Wrap around off, moving past the first or last item focuses that item again, which speaks
 * it with the end sound in place of the focus sound.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RepeatAtEdgeTest {
  private lateinit var service: TalkBackService
  private lateinit var processor: FocusProcessorForLogicalNavigation
  private val focused =
    AccessibilityNodeInfoCompat(AccessibilityNodeInfo.obtain()).apply { isVisibleToUser = true }
  private val feedback = ArrayList<Feedback>()

  @Before
  fun setUp() {
    service = Robolectric.buildService(TalkBackService::class.java).get()
    FormFactorUtils.initialize(service)
    val monitor =
      object : AccessibilityFocusMonitor(service, null, null) {
        override fun getAccessibilityFocus(
          useInputFocusIfEmpty: Boolean,
          requireEditable: Boolean,
          isCurrent: Filter<AccessibilityNodeInfoCompat>?,
        ): AccessibilityNodeInfoCompat = focused
      }
    processor =
      FocusProcessorForLogicalNavigation(
          service,
          null,
          monitor,
          null,
          null,
          Compositor.TextComposer { _, _, _ -> null },
          null,
        )
        .apply {
          setPipeline(
            Pipeline.FeedbackReturner {
              feedback.add(it)
              true
            }
          )
        }
    FocusProcessorForLogicalNavigation.setWrapAround(false)
  }

  @After
  fun tearDown() {
    FocusProcessorForLogicalNavigation.setWrapAround(true)
  }

  private fun action(
    direction: Int = SEARCH_FOCUS_FORWARD,
    target: Int = NavigationTarget.TARGET_DEFAULT,
    wraps: Boolean = true,
  ) =
    NavigationAction.Builder()
      .setAction(NavigationAction.DIRECTIONAL_NAVIGATION)
      .setDirection(direction)
      .setTarget(target)
      .setShouldWrap(wraps)
      .build()

  private fun focusFeedback(): List<Feedback.Focus> =
    feedback.flatMap { it.failovers() }.mapNotNull { it.focus() }

  private fun stopAtEdge() {
    ReflectionHelpers.setField(processor, "stoppedAtEdge", true)
  }

  @Test
  fun atTheLastItem_theItemIsFocusedAgainWithTheEdgeSound() {
    stopAtEdge()
    assertTrue(processor.repeatAtEdge(action(), EVENT_ID_UNTRACKED))

    val focus = focusFeedback().single()
    assertSame(focused, focus.target())
    assertTrue(focus.forceRefocus())
    assertEquals(ColumnWrap.EDGE, focus.focusActionInfo()!!.columnWrap)
    assertEquals(R.raw.complete, ColumnWrap.sound(ColumnWrap.EDGE))
  }

  @Test
  fun atTheFirstItem_theItemIsFocusedAgain() {
    stopAtEdge()
    assertTrue(processor.repeatAtEdge(action(SEARCH_FOCUS_BACKWARD), EVENT_ID_UNTRACKED))
    assertSame(focused, focusFeedback().single().target())
  }

  @Test
  fun withWrapAroundOn_nothingIsRepeated() {
    FocusProcessorForLogicalNavigation.setWrapAround(true)
    stopAtEdge()
    assertFalse(processor.repeatAtEdge(action(), EVENT_ID_UNTRACKED))
    assertTrue(feedback.isEmpty())
  }

  @Test
  fun anActionThatNeverWraps_isNotRepeated() {
    stopAtEdge()
    assertFalse(processor.repeatAtEdge(action(wraps = false), EVENT_ID_UNTRACKED))
    assertTrue(feedback.isEmpty())
  }

  @Test
  fun movingByHeading_isNotRepeated() {
    stopAtEdge()
    assertFalse(
      processor.repeatAtEdge(action(target = NavigationTarget.TARGET_HEADING), EVENT_ID_UNTRACKED)
    )
    assertTrue(feedback.isEmpty())
  }

  @Test
  fun aFailureAwayFromTheEdge_isNotRepeated() {
    assertFalse(processor.repeatAtEdge(action(), EVENT_ID_UNTRACKED))
    assertTrue(feedback.isEmpty())
  }
}
