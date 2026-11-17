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
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * Which sideways scrollables count as carousels showing one page, which swipes pass instead of
 * turning (report 383), and which hold several items that swipes scroll to (report 449).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ShowsOnePageTest {
  private var nextId = 1L

  private inner class Node(className: String, bounds: Rect) :
    AccessibilityNodeInfoCompat(info(className, bounds)) {
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
  private fun info(className: String, bounds: Rect) =
    AccessibilityNodeInfo.obtain().apply {
      ReflectionHelpers.setField(this, "mSourceNodeId", nextId++)
      this.className = className
      isVisibleToUser = true
      setBoundsInParent(bounds)
      setBoundsInScreen(bounds)
    }

  private fun bar() = Rect(0, 2148, 1080, 2274)

  private fun scrollView() =
    Node("android.widget.HorizontalScrollView", bar()).apply { isScrollable = true }

  /** Tasker's project bar: projects 157 px wide in the scroll view's content layout. */
  @Test
  fun scrollViewWithNarrowItemsIsNotOnePage() {
    val content = Node("android.widget.LinearLayout", bar())
    val projects =
      (0 until 7).map { Node("android.widget.LinearLayout", Rect(it * 157, 2148, it * 157 + 157, 2274)) }
    content.add(*projects.toTypedArray())
    val list = scrollView().add(content)

    assertFalse(FocusProcessorForLogicalNavigation.showsOnePage(list, projects[6]))
  }

  @Test
  fun scrollViewItemPartIsNotOnePage() {
    val content = Node("android.widget.LinearLayout", bar())
    val project = Node("android.widget.LinearLayout", Rect(942, 2148, 1080, 2274))
    val label = Node("android.widget.TextView", Rect(950, 2160, 1070, 2260))
    project.add(label)
    content.add(Node("android.widget.LinearLayout", Rect(0, 2148, 942, 2274)), project)
    val list = scrollView().add(content)

    assertFalse(FocusProcessorForLogicalNavigation.showsOnePage(list, label))
  }

  @Test
  fun scrollViewWithPageWideItemsIsOnePage() {
    val content = Node("android.widget.LinearLayout", bar())
    val page = Node("android.widget.FrameLayout", bar())
    val button = Node("android.widget.Button", Rect(100, 2160, 400, 2260))
    page.add(button)
    content.add(page)
    val list = scrollView().add(content)

    assertTrue(FocusProcessorForLogicalNavigation.showsOnePage(list, button))
  }

  @Test
  fun listWithPageWideItemIsOnePage() {
    val page = Node("android.widget.FrameLayout", bar())
    val button = Node("android.widget.Button", Rect(100, 2160, 400, 2260))
    page.add(button)
    val list =
      Node("androidx.recyclerview.widget.RecyclerView", bar()).apply { isScrollable = true }
    list.add(page)

    assertTrue(FocusProcessorForLogicalNavigation.showsOnePage(list, button))
  }

  @Test
  fun listWithNarrowItemsIsNotOnePage() {
    val list =
      Node("androidx.recyclerview.widget.RecyclerView", bar()).apply { isScrollable = true }
    val item = Node("android.widget.LinearLayout", Rect(0, 2148, 300, 2274))
    list.add(item, Node("android.widget.LinearLayout", Rect(300, 2148, 600, 2274)))

    assertFalse(FocusProcessorForLogicalNavigation.showsOnePage(list, item))
  }
}
