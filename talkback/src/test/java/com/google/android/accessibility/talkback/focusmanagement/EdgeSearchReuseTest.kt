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
import com.google.android.accessibility.utils.traversal.OrderedTraversalStrategy
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_FORWARD
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * Moving to the next item away from a list's edge takes the item that the edge check found,
 * rather than searching the order a second time, when that search was the same as the move's.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EdgeSearchReuseTest {
  private lateinit var processor: FocusProcessorForLogicalNavigation
  private val feedback = ArrayList<Feedback>()
  private var nextId = 1L

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

  /** An order that counts the steps searched through it. */
  private class CountingOrder(root: AccessibilityNodeInfoCompat) : OrderedTraversalStrategy(root) {
    var steps = 0

    override fun findFocus(
      startNode: AccessibilityNodeInfoCompat?,
      direction: Int,
    ): AccessibilityNodeInfoCompat? {
      steps++
      return super.findFocus(startNode, direction)
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

  /** A row of a list from 0 to 1000, at least a row clear of its edges at [top] 300 to 600. */
  private fun row(top: Int, heading: Boolean = false) =
    Node("android.widget.TextView", AccessibilityAction.ACTION_CLICK).apply {
      setBoundsInScreen(Rect(0, top, 200, top + 100))
      isFocusable = true
      isClickable = true
      isHeading = heading
      text = "Row"
    }

  private fun list(vararg rows: Node): Node =
    Node(
        "android.widget.ListView",
        AccessibilityAction.ACTION_SCROLL_FORWARD,
        AccessibilityAction.ACTION_SCROLL_BACKWARD,
      )
      .apply { setBoundsInScreen(Rect(0, 0, 200, 1000)) }
      .add(*rows)

  private fun window(list: Node): Node =
    Node("android.widget.FrameLayout").apply { setBoundsInScreen(Rect(0, 0, 200, 1000)) }.add(list)

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
        ): AccessibilityNodeInfoCompat? = null
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
              true
            }
          )
        }
  }

  @After
  fun tearDown() {
    TraversalTreeCache.clear("test")
  }

  private fun action(target: Int = NavigationTarget.TARGET_DEFAULT) =
    NavigationAction.Builder()
      .setAction(NavigationAction.DIRECTIONAL_NAVIGATION)
      .setDirection(SEARCH_FOCUS_FORWARD)
      .setTarget(target)
      .setShouldScroll(true)
      .build()

  private fun focusTargets(): List<AccessibilityNodeInfoCompat> =
    feedback.flatMap { it.failovers() }.mapNotNull { it.focus()?.target() }

  private fun navigate(
    pivot: AccessibilityNodeInfoCompat,
    navigationAction: NavigationAction = action(),
  ): Boolean =
    processor.onDirectionalNavigationAction(
      pivot,
      /* ignoreDescendantsOfPivot= */ false,
      navigationAction,
      EVENT_ID_UNTRACKED,
    )

  @Test
  fun theNextRowIsSearchedForOnce() {
    val first = row(300)
    val second = row(400)
    val root = window(list(first, second, row(500)))
    val order = CountingOrder(root)
    TraversalTreeCache.put(root, order)

    assertTrue(navigate(first))

    assertEquals(listOf<AccessibilityNodeInfoCompat>(second), focusTargets())
    // The edge check's one step; the move took the row it found.
    assertEquals(1, order.steps)
  }

  @Test
  fun aWebViewAfterTheRowIsEntered() {
    val first = row(300)
    val webView =
      Node("android.webkit.WebView").apply {
        setBoundsInScreen(Rect(0, 400, 200, 500))
        addAction(AccessibilityNodeInfoCompat.ACTION_NEXT_HTML_ELEMENT)
        addAction(AccessibilityNodeInfoCompat.ACTION_PREVIOUS_HTML_ELEMENT)
      }
    val root = window(list(first, webView, row(500)))
    TraversalTreeCache.put(root, CountingOrder(root))

    assertTrue(navigate(first))

    assertEquals(listOf<AccessibilityNodeInfoCompat>(webView), focusTargets())
  }

  @Test
  fun movingByHeadingSearchesPastTheRowTheEdgeCheckFound() {
    val first = row(300)
    val heading = row(500, heading = true)
    val root = window(list(first, row(400), heading, row(600)))
    TraversalTreeCache.put(root, CountingOrder(root))

    assertTrue(navigate(first, action(NavigationTarget.TARGET_HEADING)))

    assertEquals(listOf<AccessibilityNodeInfoCompat>(heading), focusTargets())
  }
}
