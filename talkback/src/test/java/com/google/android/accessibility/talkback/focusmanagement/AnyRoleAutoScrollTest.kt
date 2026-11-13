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
import com.google.android.accessibility.talkback.focusmanagement.action.NavigationAction
import com.google.android.accessibility.utils.Filter
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.Performance.EventId
import com.google.android.accessibility.utils.traversal.OrderedTraversalStrategy
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_FORWARD
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * At the last item on screen, moving to the next item scrolls the nearest container that scrolls
 * down, whatever its role, when no list, grid or scroll view can scroll.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AnyRoleAutoScrollTest {
  private lateinit var processor: FocusProcessorForLogicalNavigation
  private val feedback = ArrayList<Feedback>()
  private var nextId = 1L

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
      setBoundsInScreen(Rect(0, 0, 100, 100))
      actions.forEach { addAction(it) }
      isScrollable = actions.any { it.id != AccessibilityNodeInfo.ACTION_CLICK }
    }

  private fun item() =
    Node("android.widget.TextView", AccessibilityAction.ACTION_CLICK).apply {
      isFocusable = true
      isClickable = true
      text = "Private headline"
    }

  private fun verticalScroller(className: String = "android.widget.FrameLayout") =
    Node(
      className,
      AccessibilityAction.ACTION_SCROLL_FORWARD,
      AccessibilityAction.ACTION_SCROLL_DOWN,
    )

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

  private fun action(target: Int = NavigationTarget.TARGET_DEFAULT) =
    NavigationAction.Builder()
      .setAction(NavigationAction.DIRECTIONAL_NAVIGATION)
      .setDirection(SEARCH_FOCUS_FORWARD)
      .setTarget(target)
      .setShouldScroll(true)
      .build()

  private fun autoScrollAtEdge(
    pivot: AccessibilityNodeInfoCompat,
    root: AccessibilityNodeInfoCompat,
    ignoreDescendantsOfPivot: Boolean = false,
    navigationAction: NavigationAction = action(),
  ): Boolean =
    ReflectionHelpers.callInstanceMethod(
      processor,
      "autoScrollAtEdge",
      ReflectionHelpers.ClassParameter.from(AccessibilityNodeInfoCompat::class.java, pivot),
      ReflectionHelpers.ClassParameter.from(
        Boolean::class.javaPrimitiveType,
        ignoreDescendantsOfPivot,
      ),
      ReflectionHelpers.ClassParameter.from(NavigationAction::class.java, navigationAction),
      ReflectionHelpers.ClassParameter.from(
        OrderedTraversalStrategy::class.java,
        OrderedTraversalStrategy(root),
      ),
      ReflectionHelpers.ClassParameter.from(EventId::class.java, EVENT_ID_UNTRACKED),
    )

  private fun scrolls(): List<Feedback.Scroll> =
    feedback.flatMap { it.failovers() }.mapNotNull { it.scroll() }

  private fun note(): String = ReflectionHelpers.getField(processor, "edgeScrollNote")

  @Test
  fun theLastItemScrollsAViewGroupThatScrollsDown() {
    val first = item()
    val last = item()
    val scroller = verticalScroller().add(first, last)
    val root = Node("android.widget.FrameLayout").add(scroller)

    assertTrue(autoScrollAtEdge(last, root))

    val scroll = scrolls().single()
    assertEquals(scroller, scroll.nodeCompat())
    assertEquals(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, scroll.nodeAction())
    // Focus moves on after the scroll as in a list.
    assertTrue(ReflectionHelpers.getField<Any?>(processor, "scrollCallback") != null)
    assertEquals("anyRole:result=true", note())
  }

  @Test
  fun anItemBeforeTheLastDoesNotScroll() {
    val first = item()
    val last = item()
    val root = Node("android.widget.FrameLayout").add(verticalScroller().add(first, last))

    assertFalse(autoScrollAtEdge(first, root))
    assertTrue(scrolls().isEmpty())
    assertEquals("anyRole:notAtEdge", note())
  }

  @Test
  fun aListIsScrolledAsBefore() {
    val last = item()
    val list =
      Node("android.widget.ListView", AccessibilityAction.ACTION_SCROLL_FORWARD).add(item(), last)
    val root = Node("android.widget.FrameLayout").add(list)

    assertTrue(autoScrollAtEdge(last, root))
    assertEquals(list, scrolls().single().nodeCompat())
    assertEquals("list:result=true", note())
  }

  @Test
  fun aContainerThatFailedToScrollIsNotScrolledAgain() {
    // After a failed scroll, navigation goes on from the container and ignores its items.
    val scroller = verticalScroller().add(item(), item())
    val root = Node("android.widget.FrameLayout").add(scroller)

    assertFalse(autoScrollAtEdge(scroller, root, ignoreDescendantsOfPivot = true))
    assertTrue(scrolls().isEmpty())
  }

  @Test
  fun aPagerIsNotScrolled() {
    val last = item()
    val pager =
      Node(
          "android.widget.FrameLayout",
          AccessibilityAction.ACTION_SCROLL_FORWARD,
          AccessibilityAction.ACTION_PAGE_RIGHT,
        )
        .add(item(), last)
    val root = Node("android.widget.FrameLayout").add(pager)

    assertFalse(autoScrollAtEdge(last, root))
    assertTrue(scrolls().isEmpty())
    assertEquals("none", note())
  }

  @Test
  fun movingByHeadingDoesNotScrollOtherContainers() {
    val last = item()
    val root = Node("android.widget.FrameLayout").add(verticalScroller().add(item(), last))

    assertFalse(
      autoScrollAtEdge(last, root, navigationAction = action(NavigationTarget.TARGET_HEADING))
    )
    assertTrue(scrolls().isEmpty())
  }

  @Test
  fun theEdgeLogListsAncestorsWithoutText() {
    val last = item()
    val scroller = verticalScroller().add(last)
    Node("android.widget.FrameLayout").add(scroller)

    val text = FocusProcessorForLogicalNavigation.describeForEdgeLog(last)

    assertFalse(text.contains("Private headline"))
    assertTrue(text, text.startsWith("pivot=android.widget.TextView/"))
    assertTrue(
      text,
      text.contains("android.widget.FrameLayout/ROLE_VIEW_GROUP scr=1 vis=1 act=FD auto=0 any=1"),
    )
    assertTrue(text, text.endsWith("auto=0 any=0]"))
  }

  @Test
  fun theEdgeLogStopsAfterTwelveAncestors() {
    val pivot = item()
    var top: Node = Node("android.widget.FrameLayout").add(pivot)
    repeat(20) { top = Node("android.widget.FrameLayout").add(top) }

    val text = FocusProcessorForLogicalNavigation.describeForEdgeLog(pivot)

    assertEquals(12, text.split("; ").count { it.contains("FrameLayout") })
    assertTrue(text, text.endsWith("; ...]"))
  }
}
