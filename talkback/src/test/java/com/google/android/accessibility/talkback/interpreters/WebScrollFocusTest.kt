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
import org.junit.Assert.assertNotNull
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
  fun anItemAcrossAnEdgeIsPassedOverForOneWhollyInView() {
    val focus = above("Burger")
    val acrossTop = Node(Rect(0, 100, 1000, 300), text = "Salad")
    val whollyInView = item(300, 500, "Fries")
    val page = Node(viewport, null, false, true, focus, acrossTop, whollyInView)

    assertSame(
      whollyInView,
      WebScrollFocus.findItemInView(page, listOf(focus, page), viewport, true),
    )
  }

  @Test
  fun scrollingBackPassesOverAnItemAcrossTheBottomEdge() {
    val whollyInView = item(1500, 1900, "Fries")
    val acrossBottom = Node(Rect(0, 1900, 1000, 2000), text = "Salad")
    val focus = below("Burger")
    val page = Node(viewport, null, false, true, whollyInView, acrossBottom, focus)

    assertSame(
      whollyInView,
      WebScrollFocus.findItemInView(page, listOf(focus, page), viewport, false),
    )
  }

  @Test
  fun aClickableSectionAcrossAnEdgeIsLookedIntoForAnItemWhollyInView() {
    val focus = above("Burger")
    val link = Node(Rect(0, 400, 1000, 500), text = "Order", clickable = true)
    val section =
      Node(Rect(0, 100, 1000, 600), "Fries", true, false, Node(Rect(0, 100, 1000, 300)), link)
    val page = Node(viewport, null, false, true, focus, section)

    assertSame(link, WebScrollFocus.findItemInView(page, listOf(focus, page), viewport, true))
  }

  @Test
  fun anItemAcrossAnEdgeIsTakenWhenNothingIsWhollyInView() {
    val tabs = Node(Rect(0, 150, 1000, 250), text = "Menu", clickable = true)
    val focus = above("Burger")
    val acrossBoth = Node(Rect(0, 100, 1000, 2000), text = "Long article")
    val section = Node(Rect(0, 100, 1000, 2000), null, false, false, focus, acrossBoth)
    val page = Node(viewport, null, false, true, tabs, section)

    assertSame(
      acrossBoth,
      WebScrollFocus.findItemInView(page, listOf(focus, section, page), viewport, true),
    )
  }

  @Test
  fun aContainerThatFillsMostOfTheViewportIsLookedInto() {
    val text = item(300, 400, "Menu")
    val section = Node(Rect(0, 200, 1000, 1900), null, true, false, text)
    val page = Node(viewport, null, false, true, section)

    assertSame(text, WebScrollFocus.findItemInView(page, null, viewport, true))
  }

  @Test
  fun boundsThatEndAtTheTopOrBottomEdgeAreNotWhollyInside() {
    assertTrue(WebScrollFocus.isWhollyInside(Rect(0, 101, 1000, 1999), viewport))
    assertFalse(WebScrollFocus.isWhollyInside(Rect(0, 100, 1000, 300), viewport))
    assertFalse(WebScrollFocus.isWhollyInside(Rect(0, 1800, 1000, 2000), viewport))
    assertFalse(WebScrollFocus.isWhollyInside(Rect(-300, 600, 300, 900), viewport))
  }

  @Test
  fun focusFarDownALongListIsFoundWithoutReadingTheListFromItsStart() {
    val focusIndex = 2 * WebScrollFocus.MAX_NODES_READ
    val items =
      Array(4 * WebScrollFocus.MAX_NODES_READ) {
        when {
          it <= focusIndex -> above("Item $it")
          it == focusIndex + 1 -> item(300, 500, "Item $it")
          else -> below("Item $it")
        }
      }
    val page = Node(viewport, null, false, true, *items)

    val found =
      WebScrollFocus.findItemInView(page, listOf(items[focusIndex], page), viewport, true)

    assertSame(items[focusIndex + 1], found)
    assertTrue(reads <= WebScrollFocus.MAX_INDEX_READS + WebScrollFocus.MAX_NODES_READ)
  }

  @Test
  fun scrollingBackInALongListFindsTheLastItemInView() {
    val focusIndex = 2 * WebScrollFocus.MAX_NODES_READ
    val items =
      Array(4 * WebScrollFocus.MAX_NODES_READ) {
        when {
          it < focusIndex - 1 -> above("Item $it")
          it == focusIndex - 1 -> item(1500, 1900, "Item $it")
          else -> below("Item $it")
        }
      }
    val page = Node(viewport, null, false, true, *items)

    val found =
      WebScrollFocus.findItemInView(page, listOf(items[focusIndex], page), viewport, false)

    assertSame(items[focusIndex - 1], found)
    assertTrue(reads <= WebScrollFocus.MAX_INDEX_READS + WebScrollFocus.MAX_NODES_READ)
  }

  @Test
  fun findingTheFocusPositionDoesNotUseTheSearchBudget() {
    val focusIndex = WebScrollFocus.MAX_INDEX_READS - 1
    val items =
      Array(focusIndex + WebScrollFocus.MAX_NODES_READ) {
        if (it == focusIndex + WebScrollFocus.MAX_NODES_READ - 1) item(300, 500, "Item $it")
        else above("Item $it")
      }
    val page = Node(viewport, null, false, true, *items)

    assertNotNull(
      WebScrollFocus.findItemInView(page, listOf(items[focusIndex], page), viewport, true)
    )
  }

  @Test
  fun aScrollWhileTouchingIsTheUsers() {
    assertTrue(WebScrollFocus.isUserScroll(1500, 1000, 900, true, 500))
  }

  @Test
  fun aScrollShortlyAfterTheTouchEndedIsTheUsersFling() {
    val end = 1200L
    assertTrue(WebScrollFocus.isUserScroll(end + WebScrollFocus.FLING_MS - 1, 1000, end, false, 500))
    assertFalse(WebScrollFocus.isUserScroll(end + WebScrollFocus.FLING_MS, 1000, end, false, 500))
  }

  @Test
  fun aScrollAfterFocusMovedSinceTheTouchStartedIsThePagesOwn() {
    // A swipe: the touch starts, focus moves, the page scrolls to the focused item.
    assertFalse(WebScrollFocus.isUserScroll(1300, 1000, 1100, false, 1150))
  }

  @Test
  fun aScrollWithoutATouchIsNotTheUsers() {
    assertFalse(WebScrollFocus.isUserScroll(1300, -1, -1, false, 500))
    assertFalse(WebScrollFocus.isUserScroll(900, 1000, 1100, false, 500))
  }

  @Test
  fun aSearchReadsALimitedNumberOfNodes() {
    val items = Array(2 * WebScrollFocus.MAX_NODES_READ) { above("Item $it") }
    val page = Node(viewport, null, false, true, *items)

    assertNull(WebScrollFocus.findItemInView(page, null, viewport, true))
    assertEquals(WebScrollFocus.MAX_NODES_READ, reads)
  }
}
