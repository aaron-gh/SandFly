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
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_BACKWARD
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_FORWARD
import com.google.android.accessibility.utils.traversal.TraversalStrategyUtils
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
 * Report 485 (and 383), Lost Vault, a Flutter app: on the Explore tab, a swipe to the previous
 * item from the district turned the zone pager instead of moving to the "Water Auto" button before
 * it. Flutter gives a PageView, such as the zone pager or the TabBarView around it, the class
 * android.view.View with only scroll forward and back, no role and no collection info. The tree is
 * the reporter's Copy screen tree.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FlutterPageViewTest {
  private lateinit var processor: FocusProcessorForLogicalNavigation
  private val feedback = ArrayList<Feedback>()
  private var nextId = 1L

  /** A node whose parent and children are set by the test. */
  private inner class Node(
    className: String,
    bounds: Rect,
    vararg actions: AccessibilityAction,
  ) : AccessibilityNodeInfoCompat(info(className, bounds, actions)) {
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

  private fun info(className: String, bounds: Rect, actions: Array<out AccessibilityAction>) =
    AccessibilityNodeInfo.obtain().apply {
      ReflectionHelpers.setField(this, "mSourceNodeId", nextId++)
      this.className = className
      isVisibleToUser = true
      isEnabled = true
      setBoundsInScreen(bounds)
      actions.forEach { addAction(it) }
      isScrollable =
        actions.any {
          it.id == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD ||
            it.id == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        }
    }

  private val screen = Rect(0, 0, 720, 1600)

  private fun view(bounds: Rect = screen) = Node("android.view.View", bounds)

  /** A Flutter PageView between its first and last page: it scrolls forward and back only. */
  private fun pageView(bounds: Rect) =
    Node(
      "android.view.View",
      bounds,
      AccessibilityAction.ACTION_SCROLL_FORWARD,
      AccessibilityAction.ACTION_SCROLL_BACKWARD,
    )

  private fun button(className: String, label: String, bounds: Rect) =
    Node(className, bounds, AccessibilityAction.ACTION_CLICK).apply {
      isFocusable = true
      isClickable = true
      contentDescription = label
    }

  private lateinit var root: Node
  private lateinit var tabPages: Node
  private lateinit var zones: Node
  private lateinit var waterAuto: Node
  private lateinit var district: Node

  /** Builds the Explore tab of the reporter's tree. */
  private fun buildTree() {
    waterAuto = button("android.widget.Button", "Water\nAuto", Rect(500, 300, 700, 380))
    district =
      Node("android.view.View", Rect(60, 600, 660, 1000)).apply {
        isFocusable = true
        contentDescription = "District 4\nProgress 0 / 1,334"
      }
    district.add(
      Node(
          "android.widget.Button",
          Rect(260, 900, 460, 980),
          AccessibilityAction.ACTION_CLICK,
          AccessibilityAction.ACTION_SCROLL_FORWARD,
        )
        .apply {
          isFocusable = true
          isClickable = true
          contentDescription = "Water: 10"
        }
    )
    zones =
      pageView(Rect(0, 400, 720, 1300))
        .add(
          Node("android.widget.ImageView", Rect(0, 400, 720, 1300)),
          district,
          Node("android.widget.ImageView", Rect(620, 760, 700, 840)),
        )
    val page =
      view(Rect(0, 200, 720, 1300))
        .add(
          Node("android.view.View", Rect(20, 220, 700, 290)).apply {
            isFocusable = true
            contentDescription = "Endless Desert\nThe horizon of sand stretches as far as the eye can see."
          },
          button("android.view.View", "Close", Rect(20, 300, 120, 380)),
          button("android.widget.Button", "Settings", Rect(140, 300, 300, 380)),
          waterAuto,
          view(Rect(0, 400, 720, 1300)).add(zones),
        )
    tabPages = pageView(Rect(0, 200, 720, 1300)).add(view(Rect(0, 200, 720, 1300)).add(page))
    val tabs =
      view(Rect(0, 100, 720, 200))
        .add(
          button("android.view.View", "Wastelands\nTab 1 of 3", Rect(0, 100, 240, 200)),
          button("android.view.View", "Caravan\nTab 2 of 3", Rect(240, 100, 480, 200)),
          button("android.view.View", "Vaults\n1\nTab 3 of 3", Rect(480, 100, 720, 200)),
        )
    val content =
      view()
        .add(
          button("android.widget.Button", "My resources", Rect(0, 0, 720, 100)),
          tabs,
          view(Rect(0, 200, 720, 1300)).add(tabPages),
        )
    root = Node("android.widget.FrameLayout", screen).add(view().add(content))
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
    buildTree()
  }

  private fun action(direction: Int) =
    NavigationAction.Builder()
      .setAction(NavigationAction.DIRECTIONAL_NAVIGATION)
      .setDirection(direction)
      .setTarget(NavigationTarget.TARGET_DEFAULT)
      .setShouldScroll(true)
      .build()

  private fun autoScrollAtEdge(pivot: AccessibilityNodeInfoCompat, direction: Int): Boolean =
    ReflectionHelpers.callInstanceMethod(
      processor,
      "autoScrollAtEdge",
      ReflectionHelpers.ClassParameter.from(AccessibilityNodeInfoCompat::class.java, pivot),
      ReflectionHelpers.ClassParameter.from(Boolean::class.javaPrimitiveType, false),
      ReflectionHelpers.ClassParameter.from(NavigationAction::class.java, action(direction)),
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
  fun thePreviousItemFromTheDistrictIsWaterAuto() {
    val strategy = OrderedTraversalStrategy(root)
    val previous =
      TraversalStrategyUtils.searchFocus(
        strategy,
        district,
        SEARCH_FOCUS_BACKWARD,
        NavigationTarget.createNodeFilter(
          NavigationTarget.TARGET_DEFAULT,
          strategy.speakingNodesCache,
        ),
      )
    assertEquals(waterAuto, previous)
  }

  @Test
  fun aSwipeBackFromTheDistrictDoesNotTurnTheZonePager() {
    assertFalse(autoScrollAtEdge(district, SEARCH_FOCUS_BACKWARD))
    assertTrue(scrolls().isEmpty())
    assertEquals("none", note())
  }

  @Test
  fun aSwipeOnFromTheDistrictDoesNotTurnTheZonePager() {
    // The Water: 10 button inside the district comes next, so a swipe on starts from it.
    val water = district.getChild(0)!!
    assertFalse(autoScrollAtEdge(water, SEARCH_FOCUS_FORWARD))
    assertTrue(scrolls().isEmpty())
  }

  @Test
  fun aSwipeBackFromTheFirstItemOfATabDoesNotSwitchTabs() {
    val title = (tabPages.getChild(0)!!.getChild(0)!!).getChild(0)!!
    assertFalse(autoScrollAtEdge(title, SEARCH_FOCUS_BACKWARD))
    assertTrue(scrolls().isEmpty())
    assertEquals("none", note())
  }

  @Test
  fun aComposeListOfTheSameClassStillScrolls() {
    // Compose gives its scrollables up and down actions as well, which tell their axis.
    val last = button("android.view.View", "Last", Rect(0, 1200, 720, 1300))
    val list =
      Node(
          "android.view.View",
          Rect(0, 200, 720, 1300),
          AccessibilityAction.ACTION_SCROLL_FORWARD,
          AccessibilityAction.ACTION_SCROLL_DOWN,
        )
        .add(button("android.view.View", "First", Rect(0, 200, 720, 300)), last)
    root = Node("android.widget.FrameLayout", screen).add(list)

    assertTrue(autoScrollAtEdge(last, SEARCH_FOCUS_FORWARD))
    assertEquals(list, scrolls().single().nodeCompat())
  }
}
