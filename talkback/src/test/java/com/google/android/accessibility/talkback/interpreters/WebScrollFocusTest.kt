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

package com.google.android.accessibility.talkback.interpreters

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** After a manual scroll, focus out of view in web content moves to an item in view. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WebScrollFocusTest {
  /** The WebView on screen. */
  private val viewport = Rect(0, 100, 1000, 2000)
  private var reads = 0

  private inner class Node(
    bounds: Rect,
    text: String? = null,
    clickable: Boolean = false,
    webView: Boolean = false,
    vararg children: Node,
  ) : AccessibilityNodeInfoCompat(AccessibilityNodeInfo.obtain()) {
    val nodes = children.toList()

    init {
      setBoundsInScreen(bounds)
      className = if (webView) "android.webkit.WebView" else "android.view.View"
      this.text = text
      isClickable = clickable
      isVisibleToUser = true
      addAction(ACTION_NEXT_HTML_ELEMENT)
      addAction(ACTION_PREVIOUS_HTML_ELEMENT)
    }

    override fun getChildCount() = nodes.size

    override fun getChild(index: Int): AccessibilityNodeInfoCompat {
      reads++
      return nodes[index]
    }

    override fun equals(other: Any?) = this === other

    override fun hashCode() = System.identityHashCode(this)
  }

  private fun item(top: Int, bottom: Int, text: String) =
    Node(Rect(0, top, 1000, bottom), text = text)

  /** Chrome clips a node above the viewport to its top edge. */
  private fun above(text: String) = Node(Rect(0, 100, 1000, 40), text = text)

  /** Chrome clips a node below the viewport to its bottom edge. */
  private fun below(text: String) = Node(Rect(0, 2300, 1000, 2000), text = text)

  @Test
  fun boundsClippedAtAnEdgeOfTheViewportAreOutsideIt() {
    assertTrue(WebScrollFocus.isOutside(Rect(0, 100, 1000, 40), viewport))
    assertTrue(WebScrollFocus.isOutside(Rect(0, 2300, 1000, 2000), viewport))
    assertTrue(WebScrollFocus.isOutside(Rect(0, 100, 1000, 100), viewport))
    assertTrue(WebScrollFocus.isOutside(Rect(1000, 500, 1400, 600), viewport))
  }

  @Test
  fun boundsInTheViewportAreNotOutsideIt() {
    assertFalse(WebScrollFocus.isOutside(Rect(0, 100, 1000, 300), viewport))
    assertFalse(WebScrollFocus.isOutside(Rect(900, 500, 1400, 600), viewport))
    assertFalse(WebScrollFocus.isOutside(Rect(0, 500, 1000, 500), viewport))
  }

  @Test
  fun boundsThatAreAllZeroAreUnknownAndNotOutside() {
    assertFalse(WebScrollFocus.isOutside(Rect(), viewport))
  }

  @Test
  fun scrollingOnFindsTheFirstItemAfterTheFocusNotATabBarAtTheTop() {
    val tabs = Node(Rect(0, 100, 1000, 200), clickable = true)
    val focus = above("Burger")
    val firstInView = item(300, 500, "Fries")
    val section =
      Node(Rect(0, 100, 1000, 2000), null, false, false, above("Salad"), focus, firstInView)
    val page = Node(viewport, null, false, true, tabs, section)

    val found =
      WebScrollFocus.findItemInView(page, listOf(focus, section, page), viewport, true)

    assertSame(firstInView, found)
  }

  @Test
  fun scrollingBackFindsTheLastItemBeforeTheFocus() {
    val lastInView = item(1500, 1900, "Fries")
    val focus = below("Burger")
    val bar = Node(Rect(0, 1900, 1000, 2000), text = "View cart")
    val section =
      Node(Rect(0, 100, 1000, 2000), null, false, false, item(300, 500, "Salad"), lastInView, focus)
    val page = Node(viewport, null, false, true, section, bar)

    val found =
      WebScrollFocus.findItemInView(page, listOf(focus, section, page), viewport, false)

    assertSame(lastInView, found)
  }

  @Test
  fun aScrolledListWithoutTheFocusIsSearchedFromItsStart() {
    val card = Node(Rect(100, 600, 400, 900), text = "Drinks", clickable = true)
    val carousel =
      Node(
        Rect(0, 600, 1000, 900),
        null,
        false,
        false,
        Node(Rect(-300, 600, 0, 900), text = "Pizza"),
        card,
      )

    assertSame(card, WebScrollFocus.findItemInView(carousel, null, viewport, true))
  }

  @Test
  fun documentsAndContainersThatFillTheViewportAreLookedInto() {
    val text = item(300, 400, "Menu")
    val wrapper = Node(viewport, null, true, false, text)
    val frame = Node(viewport, "Frame", false, true, wrapper)
    val page = Node(viewport, null, false, true, frame)

    assertSame(text, WebScrollFocus.findItemInView(page, null, viewport, true))
  }

  @Test
  fun aContainerWithoutBoundsIsLookedInto() {
    val text = item(300, 400, "Menu")
    val page = Node(viewport, null, false, true, Node(Rect(), null, false, false, text))

    assertSame(text, WebScrollFocus.findItemInView(page, null, viewport, true))
  }

  @Test
  fun aSearchReadsALimitedNumberOfNodes() {
    val items = Array(2 * WebScrollFocus.MAX_NODES_READ) { above("Item $it") }
    val page = Node(viewport, null, false, true, *items)

    assertNull(WebScrollFocus.findItemInView(page, null, viewport, true))
    assertEquals(WebScrollFocus.MAX_NODES_READ, reads)
  }
}
