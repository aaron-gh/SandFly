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
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.focusmanagement.action.NavigationAction
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * Scroll left and Scroll right swipe over a pager that has no scroll actions, such as the folder
 * tabs of Solid Explorer (report 540), and only where nothing takes a scroll or page action.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SidewaysSwipeTest {
  private var nextId = 1L
  private val screen = Rect(0, 0, 1080, 2340)

  private inner class Node(className: String, bounds: Rect, visible: Boolean = true) :
    AccessibilityNodeInfoCompat(info(className, bounds, visible)) {
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

  @Suppress("DEPRECATION")
  private fun info(className: String, bounds: Rect, visible: Boolean) =
    AccessibilityNodeInfo.obtain().apply {
      ReflectionHelpers.setField(this, "mSourceNodeId", nextId++)
      this.className = className
      isVisibleToUser = visible
      setBoundsInParent(bounds)
      setBoundsInScreen(bounds)
    }

  private fun action(type: Int) = NavigationAction.Builder().setAction(type).build()

  private class Screen(val root: AccessibilityNodeInfoCompat, val file: AccessibilityNodeInfoCompat)

  /**
   * Solid Explorer as in the report: the pager is a plain view group with no actions, one tab page
   * on the screen and the other off it, a breadcrumb bar and a file grid on the page.
   */
  private fun solidExplorer(): Screen {
    val pagerBounds = Rect(0, 100, 1080, 2300)
    val file = Node("android.widget.RelativeLayout", Rect(0, 400, 540, 800))
    val grid = Node("android.widget.GridView", Rect(0, 300, 1080, 2300)).add(file)
    val breadcrumb = Node("android.widget.HorizontalScrollView", Rect(0, 100, 1080, 300))
    val page = Node("android.view.ViewGroup", pagerBounds).add(breadcrumb, grid)
    val otherPage = Node("android.view.ViewGroup", Rect(), visible = false)
    val pager = Node("android.view.ViewGroup", pagerBounds).add(page, otherPage)
    val root = Node("android.widget.FrameLayout", screen).add(pager)
    return Screen(root, file)
  }

  @Test
  fun scrollLeftSwipesLeftOverThePager() {
    val tabs = solidExplorer()
    val line =
      FocusProcessorForLogicalNavigation.sidewaysSwipeLine(
        tabs.file,
        action(NavigationAction.SCROLL_LEFT),
        screen,
      )
    // The screen less 86 px on each side, 908 px wide: from 3/4 to 1/4 of it, at the pager's
    // middle.
    assertArrayEquals(floatArrayOf(1200f, 767f, 313f), line, 0.01f)
  }

  @Test
  fun scrollRightSwipesRight() {
    val tabs = solidExplorer()
    val line =
      FocusProcessorForLogicalNavigation.sidewaysSwipeLine(
        tabs.file,
        action(NavigationAction.SCROLL_RIGHT),
        screen,
      )
    assertArrayEquals(floatArrayOf(1200f, 313f, 767f), line, 0.01f)
  }

  @Test
  fun aNodeWithAPageActionGetsNoSwipe() {
    val tabs = solidExplorer()
    val breadcrumb = (tabs.root as Node).children[0].children[0].children[0]
    breadcrumb.isScrollable = true
    breadcrumb.addAction(AccessibilityActionCompat.ACTION_SCROLL_FORWARD)
    assertNull(
      FocusProcessorForLogicalNavigation.sidewaysSwipeLine(
        tabs.file,
        action(NavigationAction.SCROLL_LEFT),
        screen,
      )
    )
  }

  @Test
  fun aPagerWithScrollActionsGetsNoSwipe() {
    val tabs = solidExplorer()
    val pager = (tabs.root as Node).children[0]
    pager.isScrollable = true
    pager.addAction(AccessibilityActionCompat.ACTION_SCROLL_LEFT)
    assertNull(
      FocusProcessorForLogicalNavigation.sidewaysSwipeLine(
        tabs.file,
        action(NavigationAction.SCROLL_RIGHT),
        screen,
      )
    )
  }

  /** A sideways swipe over a list could dismiss or archive the item under it. */
  @Test
  fun aListWithoutAPagerGetsNoSwipe() {
    val message = Node("android.widget.LinearLayout", Rect(0, 300, 1080, 500))
    val list =
      Node("androidx.recyclerview.widget.RecyclerView", Rect(0, 300, 1080, 2300))
        .add(message, Node("android.widget.LinearLayout", Rect(), visible = false))
    val root = Node("android.widget.FrameLayout", screen).add(list)
    assertNull(
      FocusProcessorForLogicalNavigation.sidewaysSwipeLine(
        message,
        action(NavigationAction.SCROLL_LEFT),
        screen,
      )
    )
    assertNull(
      FocusProcessorForLogicalNavigation.sidewaysSwipeLine(
        root,
        action(NavigationAction.SCROLL_LEFT),
        screen,
      )
    )
  }

  @Test
  fun scrollUpGetsNoSwipe() {
    val tabs = solidExplorer()
    assertNull(
      FocusProcessorForLogicalNavigation.sidewaysSwipeLine(
        tabs.file,
        action(NavigationAction.SCROLL_UP),
        screen,
      )
    )
  }
}
