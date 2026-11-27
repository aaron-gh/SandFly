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
import com.google.android.accessibility.talkback.ActorState
import com.google.android.accessibility.talkback.ActorStateWritable
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.compositor.Compositor
import com.google.android.accessibility.talkback.focusmanagement.action.NavigationAction
import com.google.android.accessibility.talkback.focusmanagement.record.AccessibilityFocusActionHistory
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo
import com.google.android.accessibility.utils.Filter
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.Performance.EventId
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_FORWARD
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
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
 * [FocusProcessorForLogicalNavigation] after an auto-scroll with the default target: the settle
 * checks follow the focused pivot, also when the pivot is missing from the list's order and the
 * search starts from the node at its place.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AutoScrolledPivotSettleCheckTest {
  private lateinit var processor: FocusProcessorForLogicalNavigation
  private lateinit var service: TalkBackService
  private val feedback = ArrayList<Feedback>()
  private var nextId = 1L
  private var focusedNode: AccessibilityNodeInfoCompat? = null

  /** A node whose parent and children are set by the test. */
  private inner class Node(className: String, vararg actions: AccessibilityAction) :
    AccessibilityNodeInfoCompat(info(className, actions)) {
    var parentNode: Node? = null
    val children = mutableListOf<Node>()

    /** Bounds that [refresh] reads, as the list moves; unchanged if null. */
    var boundsAfterRefresh: Rect? = null
    var refreshReads = 0

    override fun getParent(): AccessibilityNodeInfoCompat? = parentNode

    override fun refresh(): Boolean {
      refreshReads++
      boundsAfterRefresh?.let { setBoundsInScreen(it) }
      return true
    }

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
    }

  private fun item(bounds: Rect): Node =
    Node("android.widget.TextView", AccessibilityAction.ACTION_CLICK).apply {
      setBoundsInScreen(bounds)
      isFocusable = true
      isClickable = true
      text = "Item"
    }

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
  }

  private fun action() =
    NavigationAction.Builder()
      .setAction(NavigationAction.DIRECTIONAL_NAVIGATION)
      .setDirection(SEARCH_FOCUS_FORWARD)
      .setTarget(NavigationTarget.TARGET_DEFAULT)
      .setShouldScroll(true)
      .build()

  private fun focusFeedback(): List<Feedback.Focus> =
    feedback.flatMap { it.failovers() }.mapNotNull { it.focus() }

  private fun handle(scrolledNode: Node, pivot: Node, settleChecksLeft: Int) {
    ReflectionHelpers.callInstanceMethod<Any?>(
      processor,
      "handleViewAutoScrolledForDirectionalNavigationWithDefaultTarget",
      ReflectionHelpers.ClassParameter.from(AccessibilityNodeInfoCompat::class.java, scrolledNode),
      ReflectionHelpers.ClassParameter.from(AccessibilityNodeInfoCompat::class.java, pivot),
      ReflectionHelpers.ClassParameter.from(NavigationAction::class.java, action()),
      ReflectionHelpers.ClassParameter.from(EventId::class.java, EVENT_ID_UNTRACKED),
      ReflectionHelpers.ClassParameter.from(Int::class.javaPrimitiveType, settleChecksLeft),
    )
  }

  @Test
  fun missingPivot_waitsOnTheFocusedPivotAndSearchesFromItsPlace() {
    val itemAbove = item(Rect(0, 100, 200, 200))
    val itemBelow = item(Rect(0, 300, 200, 400))
    val scrolledNode =
      Node("android.widget.FrameLayout").apply { setBoundsInScreen(Rect(0, 200, 200, 300)) }
    scrolledNode.add(itemAbove, itemBelow)
    // The focused pivot fills the list and lists the list as its parent, but the list's children
    // leave it out, so it is missing from the list's order.
    val pivot = item(Rect(0, 200, 200, 300)).apply { parentNode = scrolledNode }
    focusedNode = pivot

    ReflectionHelpers.setField(processor, "listSettled", false)
    ReflectionHelpers.setField(processor, "lastAutoScrollUptime", SystemClock.uptimeMillis())

    handle(scrolledNode, pivot, settleChecksLeft = 1)

    // The pivot fills the list, which still moves: the search waits.
    assertTrue(focusFeedback().isEmpty())

    ReflectionHelpers.setField(processor, "listSettled", true)
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))

    // The wait checked the focus against the pivot, not against the node the search starts from,
    // and the search from the pivot's place found the next item.
    assertEquals(itemBelow, focusFeedback().single().target())
  }

  @Test
  fun pivotInOrder_movedSinceLastRead_waitsForTheList() {
    val itemAbove = item(Rect(0, 0, 200, 100))
    val pivot = item(Rect(0, 300, 200, 400)).apply { boundsAfterRefresh = Rect(0, 250, 200, 350) }
    val scrolledNode =
      Node("android.widget.FrameLayout").apply { setBoundsInScreen(Rect(0, 0, 200, 400)) }
    scrolledNode.add(itemAbove, pivot)
    focusedNode = pivot

    ReflectionHelpers.setField(processor, "listSettled", false)
    ReflectionHelpers.setField(processor, "lastAutoScrollUptime", SystemClock.uptimeMillis())

    handle(scrolledNode, pivot, settleChecksLeft = 1)

    // Nothing follows the pivot yet, and it moved since it was last read: the list still moves,
    // so the search waits instead of navigating again.
    assertTrue(feedback.isEmpty())
    assertTrue(shadowOf(Looper.getMainLooper()).nextScheduledTaskTime > Duration.ZERO)
  }

  @Test
  fun nextSwipeWaitsWhileTheRemovedPivotsEdgeIsSettling() {
    val next = item(Rect(0, 0, 200, 100))
    val list = Node("android.widget.FrameLayout").apply {
      setBoundsInScreen(Rect(0, 0, 200, 400))
      add(next)
    }
    val pivot = item(Rect(0, 0, 200, 0)).apply {
      isVisibleToUser = false
      parentNode = list
    }
    focusedNode = null
    ReflectionHelpers.setField(processor, "lastAutoScrollUptime", SystemClock.uptimeMillis())
    ReflectionHelpers.setField(processor, "navigationSerial", 7)

    // The scroll callback has already been consumed, but its edge search is still posted.
    handle(list, pivot, settleChecksLeft = 1)
    assertTrue(focusFeedback().isEmpty())

    assertTrue(processor.onNavigationAction(action(), EVENT_ID_UNTRACKED))
    assertEquals(7, ReflectionHelpers.getField<Int>(processor, "navigationSerial"))
    assertEquals(
      1,
      ReflectionHelpers.getField<java.util.ArrayDeque<*>>(processor, "deferredNavigations").size,
    )
    assertTrue(focusFeedback().isEmpty())
  }

  @Test
  fun focusMovedElsewhereCancelsThePostedScrollSearchWait() {
    val next = item(Rect(0, 0, 200, 100))
    val list = Node("android.widget.FrameLayout").apply {
      setBoundsInScreen(Rect(0, 0, 200, 400))
      add(next)
    }
    val pivot = item(Rect(0, 0, 200, 0)).apply {
      isVisibleToUser = false
      parentNode = list
    }
    focusedNode = null
    ReflectionHelpers.setField(processor, "lastAutoScrollUptime", SystemClock.uptimeMillis())
    handle(list, pivot, settleChecksLeft = 1)

    focusedNode = item(Rect(0, 200, 200, 300))
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))

    assertTrue(focusFeedback().isEmpty())
    assertEquals(
      false,
      ReflectionHelpers.callInstanceMethod<Boolean>(processor, "isSwipeScrollPending"),
    )
  }

  @Test
  fun aSlowedListStillWaitsForItsEdgeWhenThePivotHasLeft() {
    val departingEdge = item(Rect(0, 0, 200, 5))
    val next = item(Rect(0, 5, 200, 100))
    val list = Node("android.widget.FrameLayout").apply {
      setBoundsInScreen(Rect(0, 0, 200, 400))
      add(departingEdge, next)
    }
    val pivot = item(Rect(0, 0, 200, 0)).apply {
      isVisibleToUser = false
      parentNode = list
    }
    ReflectionHelpers.setField(processor, "listSettled", true)

    // "Slowed" still moves a partly visible edge off screen. Do not focus it and release the
    // next swipe just because the tracked node or scroll deltas have slowed.
    handle(list, pivot, settleChecksLeft = 1)
    assertTrue(focusFeedback().isEmpty())

    list.children.remove(departingEdge)
    next.setBoundsInScreen(Rect(0, 0, 200, 95))
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
    assertEquals(next, focusFeedback().single().target())
  }

  private fun recordNativeFocus(node: Node) {
    val history = AccessibilityFocusActionHistory(org.robolectric.RuntimeEnvironment.getApplication())
    history.onAccessibilityFocusAction(
      node, FocusActionInfo.builder().setSourceAction(FocusActionInfo.LOGICAL_NAVIGATION).build(),
      SystemClock.uptimeMillis(), null,
    )
    processor.setActorState(ActorState(ActorStateWritable(
      null, null, null, null, history.reader, null, null, null, null, null, null, null, null, null,
    )))
  }

  private fun navigationPivot(): AccessibilityNodeInfoCompat? =
    ReflectionHelpers.callInstanceMethod(
      processor, "getPivotNodeForNavigationAction",
      ReflectionHelpers.ClassParameter.from(NavigationAction::class.java, action()),
      ReflectionHelpers.ClassParameter.from(AccessibilityNodeInfoCompat::class.java, focusedNode),
    )

  @Test
  fun aSwipeAfterEarlyFocusUsesTheNodeThatStillActuallyHasFocus() {
    val previous = item(Rect(0, 0, 200, 100)).apply { isAccessibilityFocused = true }
    val current = item(Rect(0, 100, 200, 200)).apply { isAccessibilityFocused = true }
    focusedNode = previous // Android's focus event is still behind the next swipe.
    recordNativeFocus(current)
    ReflectionHelpers.setField(processor, "lastAutoScrollUptime", SystemClock.uptimeMillis())

    assertSame(current, navigationPivot())
  }

  @Test
  fun anAppMovingFocusElsewhereDoesNotRestoreTheLastNavigationFocus() {
    val current = item(Rect(0, 0, 200, 100)).apply { isAccessibilityFocused = true }
    val previous = item(Rect(0, 100, 200, 200)).apply { isAccessibilityFocused = false }
    focusedNode = current
    recordNativeFocus(previous)
    ReflectionHelpers.setField(processor, "lastAutoScrollUptime", SystemClock.uptimeMillis())

    assertSame(current, navigationPivot())
  }

  @Test
  fun normalFocusThatMatchesTheLastNavigationNeedsNoAdditionalRefresh() {
    val current = item(Rect(0, 0, 200, 100)).apply { isAccessibilityFocused = true }
    focusedNode = current
    recordNativeFocus(current)
    ReflectionHelpers.setField(processor, "lastAutoScrollUptime", SystemClock.uptimeMillis())
    val before = current.refreshReads

    assertSame(current, navigationPivot())
    assertEquals(before, current.refreshReads)
  }

  @Test
  fun navigationWithoutARecentAutomaticScrollKeepsTheCurrentFocus() {
    val previous = item(Rect(0, 0, 200, 100)).apply { isAccessibilityFocused = true }
    val current = item(Rect(0, 100, 200, 200)).apply { isAccessibilityFocused = true }
    focusedNode = previous
    recordNativeFocus(current)
    ReflectionHelpers.setField(processor, "lastAutoScrollUptime", 0L)

    assertSame(previous, navigationPivot())
  }

  @Test
  fun aNavigationRecordInAnotherWindowCannotReplaceTheCurrentFocus() {
    val current = item(Rect(0, 0, 200, 100)).apply { isAccessibilityFocused = true }
    val otherWindow = item(Rect(0, 100, 200, 200)).apply {
      isAccessibilityFocused = true
      ReflectionHelpers.setField(unwrap(), "mWindowId", 1)
    }
    focusedNode = current
    recordNativeFocus(otherWindow)
    ReflectionHelpers.setField(processor, "lastAutoScrollUptime", SystemClock.uptimeMillis())
    val before = otherWindow.refreshReads

    assertSame(current, navigationPivot())
    assertEquals(before, otherWindow.refreshReads)
  }

  @Test
  fun aMissingCachedFocusAfterTheScrollUsesTheStillFocusedNativeNode() {
    val root = Node("android.widget.FrameLayout")
    val current = item(Rect(0, 0, 200, 100)).apply { isAccessibilityFocused = true }
    focusedNode = null
    shadowOf(service).setRootInActiveWindow(root.unwrap())
    recordNativeFocus(current)
    ReflectionHelpers.setField(processor, "lastAutoScrollUptime", SystemClock.uptimeMillis())

    assertSame(current, navigationPivot())
  }

  @Test
  fun aMissingFocusCannotRestoreANodeInADifferentWindow() {
    val root = Node("android.widget.FrameLayout")
    val otherWindow = item(Rect(0, 0, 200, 100)).apply {
      isAccessibilityFocused = true
      ReflectionHelpers.setField(unwrap(), "mWindowId", 1)
    }
    focusedNode = null
    shadowOf(service).setRootInActiveWindow(root.unwrap())
    recordNativeFocus(otherWindow)
    ReflectionHelpers.setField(processor, "lastAutoScrollUptime", SystemClock.uptimeMillis())
    val before = otherWindow.refreshReads

    assertEquals(root, navigationPivot())
    assertEquals(before, otherWindow.refreshReads)
  }

  @Test
  fun aMissingFocusDoesNotRestoreARecordThatNoLongerHasFocus() {
    val root = Node("android.widget.FrameLayout")
    val previous = item(Rect(0, 0, 200, 100)).apply { isAccessibilityFocused = false }
    focusedNode = null
    shadowOf(service).setRootInActiveWindow(root.unwrap())
    recordNativeFocus(previous)
    ReflectionHelpers.setField(processor, "lastAutoScrollUptime", SystemClock.uptimeMillis())

    assertEquals(root, navigationPivot())
  }
}
