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

import android.graphics.Rect
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.compositor.Compositor
import com.google.android.accessibility.talkback.compositor.GlobalVariables
import com.google.android.accessibility.talkback.focusmanagement.action.NavigationAction
import com.google.android.accessibility.utils.Filter
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.monitor.CollectionState
import com.google.android.accessibility.utils.monitor.InputModeTracker
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_BACKWARD
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_FORWARD
import java.time.Duration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * Moves that waited for a list to scroll run together: the items before the last move's item are
 * passed without being focused, and only that item is focused. A move that would scroll or stop at
 * an edge runs on its own from the focused item, as before.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WaitingMovesBatchTest {
  private lateinit var processor: FocusProcessorForLogicalNavigation
  private val feedback = ArrayList<Feedback>()
  private var nextId = 1L
  private var focusedNode: AccessibilityNodeInfoCompat? = null

  /** A node whose parent and children are set by the test. */
  private inner class Node(className: String, vararg actions: AccessibilityAction) :
    AccessibilityNodeInfoCompat(info(className, actions)) {
    var parentNode: Node? = null
    val children = mutableListOf<Node>()

    override fun getParent(): AccessibilityNodeInfoCompat? = parentNode

    override fun getParent(prefetchingStrategy: Int): AccessibilityNodeInfoCompat? = parentNode

    override fun refresh(): Boolean = true

    override fun getChildCount(): Int = children.size

    override fun getChild(index: Int): AccessibilityNodeInfoCompat? = children[index]

    fun add(vararg nodes: Node): Node {
      nodes.forEach {
        it.parentNode = this
        children.add(it)
      }
      return this
    }
  }

  private fun info(className: String, actions: Array<out AccessibilityAction>) =
    AccessibilityNodeInfo.obtain().apply {
      ReflectionHelpers.setField(this, "mSourceNodeId", nextId++)
      this.className = className
      isVisibleToUser = true
      actions.forEach { addAction(it) }
      isScrollable = actions.any { it.id != AccessibilityNodeInfo.ACTION_CLICK }
    }

  /** Rows 100 high from 300 down, each a row clear of the edges of a list from 0 to 1000. */
  private fun rows(count: Int): List<Node> =
    List(count) { index ->
      Node("android.widget.TextView", AccessibilityAction.ACTION_CLICK).apply {
        setBoundsInScreen(Rect(0, 300 + index * 100, 200, 400 + index * 100))
        isFocusable = true
        isClickable = true
        text = "Row $index"
      }
    }

  private fun window(rows: List<Node>): Node {
    val list =
      Node(
          "android.widget.ListView",
          AccessibilityAction.ACTION_SCROLL_FORWARD,
          AccessibilityAction.ACTION_SCROLL_BACKWARD,
        )
        .apply { setBoundsInScreen(Rect(0, 0, 200, 1000)) }
        .add(*rows.toTypedArray())
    return Node("android.widget.FrameLayout").apply { setBoundsInScreen(Rect(0, 0, 200, 1000)) }
      .add(list)
  }

  @Before
  fun setUp() {
    val service = Robolectric.buildService(TalkBackService::class.java).get()
    FormFactorUtils.initialize(service)
    TraversalTreeCache.clear("test")
    val monitor =
      object : AccessibilityFocusMonitor(service, null, null) {
        override fun getAccessibilityFocus(
          useInputFocusIfEmpty: Boolean,
          requireEditable: Boolean,
          isCurrent: Filter<AccessibilityNodeInfoCompat>?,
        ): AccessibilityNodeInfoCompat? = focusedNode
      }
    processor =
      FocusProcessorForLogicalNavigation(
          service,
          null,
          monitor,
          null,
          null,
          Compositor.TextComposer { _, _, _ -> null },
          GlobalVariables(service, InputModeTracker(), CollectionState(), null),
        )
        .apply {
          setPipeline(
            Pipeline.FeedbackReturner {
              feedback.add(it)
              it.failovers()
                .firstNotNullOfOrNull { part -> part.focus()?.target() }
                ?.let { target -> focusedNode = target }
              true
            }
          )
        }
  }

  @After
  fun tearDown() {
    TraversalTreeCache.clear("test")
  }

  private fun action(
    direction: Int = SEARCH_FOCUS_FORWARD,
    target: Int = NavigationTarget.TARGET_DEFAULT,
  ) =
    NavigationAction.Builder()
      .setAction(NavigationAction.DIRECTIONAL_NAVIGATION)
      .setDirection(direction)
      .setTarget(target)
      .setShouldScroll(true)
      .build()

  private fun focusTargets(): List<AccessibilityNodeInfoCompat> =
    feedback.flatMap { it.failovers() }.mapNotNull { it.focus()?.target() }

  /** The scrolls of a node, leaving out resets of the scroll record. */
  private fun scrolls(): List<Feedback.Scroll> =
    feedback
      .flatMap { it.failovers() }
      .mapNotNull { it.scroll() }
      .filter { it.nodeCompat() != null }

  private fun waiting(): Int =
    ReflectionHelpers.getField<java.util.ArrayDeque<*>>(processor, "deferredNavigations").size

  /** Makes moves wait, as while an earlier swipe's scroll is still to focus the next item. */
  private fun startScrollWait() {
    ReflectionHelpers.setField(processor, "lastAutoScrollUptime", SystemClock.uptimeMillis())
    ReflectionHelpers.setField(
      processor,
      "pendingScrollSearchSerial",
      ReflectionHelpers.getField<Int>(processor, "navigationSerial"),
    )
  }

  private fun endScrollWait() {
    ReflectionHelpers.setField(processor, "pendingScrollSearchSerial", -1)
  }

  private fun runWaitingMoves() {
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
  }

  @Test
  fun threeWaitingMovesFocusOnlyTheThirdItem() {
    val rows = rows(6)
    window(rows)
    focusedNode = rows[0]
    startScrollWait()
    repeat(3) { assertTrue(processor.onNavigationAction(action(), EVENT_ID_UNTRACKED)) }
    assertEquals(3, waiting())

    endScrollWait()
    runWaitingMoves()

    assertEquals(listOf<AccessibilityNodeInfoCompat>(rows[3]), focusTargets())
    assertTrue(scrolls().isEmpty())
    assertEquals(0, waiting())
  }

  @Test
  fun waitingMovesBackFocusOnlyTheLastItem() {
    val rows = rows(6)
    window(rows)
    focusedNode = rows[4]
    startScrollWait()
    repeat(2) {
      assertTrue(processor.onNavigationAction(action(SEARCH_FOCUS_BACKWARD), EVENT_ID_UNTRACKED))
    }

    endScrollWait()
    runWaitingMoves()

    assertEquals(listOf<AccessibilityNodeInfoCompat>(rows[2]), focusTargets())
  }

  @Test
  fun aMoveThatScrollsRunsFromTheFocusedItem() {
    val rows = rows(4)
    window(rows)
    focusedNode = rows[2]
    startScrollWait()
    repeat(3) { assertTrue(processor.onNavigationAction(action(), EVENT_ID_UNTRACKED)) }

    endScrollWait()
    runWaitingMoves()

    // The second move would scroll the list from the last row, so the first focuses that row and
    // the second runs from it; the third waits for the scroll.
    assertEquals(listOf<AccessibilityNodeInfoCompat>(rows[3]), focusTargets())
    assertEquals(1, scrolls().size)
    assertEquals(1, waiting())
  }

  @Test
  fun theBatchStopsBeforeAMoveThatScrolls() {
    val rows = rows(4)
    window(rows)
    focusedNode = rows[0]
    startScrollWait()
    repeat(4) { assertTrue(processor.onNavigationAction(action(), EVENT_ID_UNTRACKED)) }

    endScrollWait()
    runWaitingMoves()

    // Rows 1 and 2 are passed and row 3 is focused; the fourth move then scrolls from row 3.
    assertEquals(listOf<AccessibilityNodeInfoCompat>(rows[3]), focusTargets())
    assertEquals(1, scrolls().size)
    assertEquals(0, waiting())
  }

  @Test
  fun movesOfAnotherKindRunOneByOne() {
    val rows = rows(6)
    rows.forEach { it.isHeading = true }
    window(rows)
    focusedNode = rows[0]
    startScrollWait()
    assertTrue(processor.onNavigationAction(action(), EVENT_ID_UNTRACKED))
    assertTrue(
      processor.onNavigationAction(
        action(target = NavigationTarget.TARGET_HEADING),
        EVENT_ID_UNTRACKED,
      )
    )

    endScrollWait()
    runWaitingMoves()

    assertEquals(listOf<AccessibilityNodeInfoCompat>(rows[1], rows[2]), focusTargets())
  }

  @Test
  fun oneWaitingMoveRunsAsBefore() {
    val rows = rows(6)
    window(rows)
    focusedNode = rows[0]
    startScrollWait()
    assertTrue(processor.onNavigationAction(action(), EVENT_ID_UNTRACKED))

    endScrollWait()
    runWaitingMoves()

    assertEquals(listOf<AccessibilityNodeInfoCompat>(rows[1]), focusTargets())
  }
}
