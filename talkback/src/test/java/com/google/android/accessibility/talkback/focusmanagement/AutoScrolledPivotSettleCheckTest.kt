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
import com.google.android.accessibility.talkback.focusmanagement.action.NavigationAction
import com.google.android.accessibility.utils.Filter
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.Performance.EventId
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_FORWARD
import java.time.Duration
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
 * Regression test for the fix that keeps the resume point {@link
 * FocusProcessorForLogicalNavigation#findNodeNearPivotPlace} hands back distinct from the pivot
 * that's actually accessibility-focused. The deferred settle-check callbacks
 * (`searchWhenListSlows`, `searchAgainWhenListMoves`) compare the real accessibility focus
 * against whichever node was passed in as `focusBeforeScroll` to decide whether to keep waiting
 * -- that node must stay the real pivot, never the resume point, since the resume point is never
 * actually focused.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AutoScrolledPivotSettleCheckTest {
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
    val service = Robolectric.buildService(TalkBackService::class.java).get()
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
  fun deferredSettleCheck_comparesTheRealFocusAgainstItselfInsteadOfTheResumePoint() {
    val itemAbove = item(Rect(0, 100, 200, 200))
    val itemBelow = item(Rect(0, 400, 200, 500))
    // Bounds chosen to equal itemAbove's own bounds further down: once the recovery below
    // anchors on itemAbove, that makes it look (to the settle-check) like a pivot that fills the
    // whole list and is therefore worth waiting on, exactly as a real auto-scrolled list would.
    val scrolledNode = Node("android.widget.FrameLayout").apply { setBoundsInScreen(Rect(0, 100, 200, 200)) }
    scrolledNode.add(itemAbove, itemBelow)

    // The real pivot: still refreshes fine and reports a valid on-screen place -- between
    // itemAbove and itemBelow -- but, as if its underlying view had just been recycled by a
    // large auto-scroll, was never added to scrolledNode's subtree, so it isn't a key in the
    // freshly-built local tree.
    val stalePivot = item(Rect(0, 200, 200, 300))
    focusedNode = stalePivot

    ReflectionHelpers.setField(processor, "listSettled", false)
    ReflectionHelpers.setField(processor, "lastAutoScrollUptime", SystemClock.uptimeMillis())

    handle(scrolledNode, stalePivot, settleChecksLeft = 1)

    // The recovery found itemAbove as the resume point, which happens to fill the whole list and
    // the list still counts as moving, so resolution is deferred to a settle-check tick instead
    // of resolving immediately.
    assertTrue(focusFeedback().isEmpty())

    // The list has now settled; let the deferred tick run.
    ReflectionHelpers.setField(processor, "listSettled", true)
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))

    // With the real pivot kept distinct from the resume point used to search, the deferred tick
    // recognizes that accessibility focus hasn't moved since the scroll and resumes the search,
    // landing on itemBelow -- the node right after the resume point. Before the fix, the tick
    // compared the real focus (stalePivot) against the resume point it had been handed instead
    // (itemAbove), found them unequal, and gave up silently: no feedback is ever produced here,
    // leaving the swipe without any effect.
    assertEquals(itemBelow, focusFeedback().single().target())
  }
}
