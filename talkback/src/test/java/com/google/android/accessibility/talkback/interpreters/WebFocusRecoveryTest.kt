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
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The steps that find an item of web content in view after the user scrolled the page. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WebFocusRecoveryTest {
  /** The WebView on screen. */
  private val viewport = Rect(0, 100, 1000, 2000)
  private var reads = 0

  private inner class Node(
    bounds: Rect,
    text: String? = null,
    webView: Boolean = false,
    web: Boolean = true,
    vararg children: Node,
  ) : AccessibilityNodeInfoCompat(AccessibilityNodeInfo.obtain()) {
    val nodes = children.toList()
    var parentNode: Node? = null

    init {
      setBoundsInScreen(bounds)
      className = if (webView) "android.webkit.WebView" else "android.view.View"
      this.text = text
      isVisibleToUser = true
      if (web) {
        addAction(ACTION_NEXT_HTML_ELEMENT)
        addAction(ACTION_PREVIOUS_HTML_ELEMENT)
      }
      for (child in children) {
        child.parentNode = this
      }
    }

    override fun getChildCount() = nodes.size

    override fun getChild(index: Int): AccessibilityNodeInfoCompat {
      reads++
      return nodes[index]
    }

    override fun getParent(): AccessibilityNodeInfoCompat? {
      reads++
      return parentNode
    }

    override fun equals(other: Any?) = this === other

    override fun hashCode() = System.identityHashCode(this)
  }

  private fun item(top: Int, bottom: Int, text: String) =
    Node(Rect(0, top, 1000, bottom), text = text)

  /** Chrome clips a node above the viewport to its top edge. */
  private fun above(text: String) = Node(Rect(0, 100, 1000, 40), text = text)

  /** The app's views around a WebView that holds a document. */
  private fun inApp(document: Node): Node {
    val host = Node(viewport, null, true, true, document)
    return Node(Rect(0, 0, 1000, 2200), null, false, false, host)
  }

  private fun run(recovery: WebFocusRecovery): Int {
    var steps = 0
    while (true) {
      val before = reads
      val done = recovery.step()
      assertTrue(reads - before <= 1)
      steps++
      if (done) {
        return steps
      }
    }
  }

  @Test
  fun focusScrolledOutOfViewMovesToTheFirstItemInView() {
    val focus = above("Burger")
    val firstInView = item(300, 500, "Fries")
    val document = Node(viewport, null, true, true, above("Salad"), focus, firstInView)
    inApp(document)
    val recovery = WebFocusRecovery(focus, null, document, false, true)

    run(recovery)

    assertEquals(WebFocusRecovery.FOUND, recovery.outcome())
    assertSame(firstInView, recovery.item())
  }

  @Test
  fun focusNotVisibleToTheUserMovesEvenWithBoundsInTheView() {
    val focus = item(300, 500, "Order again").apply { isVisibleToUser = false }
    val firstInView = item(600, 800, "Fries")
    val document = Node(viewport, null, true, true, focus, firstInView)
    inApp(document)
    val recovery = WebFocusRecovery(focus, null, document, true, true)

    run(recovery)

    assertSame(firstInView, recovery.item())
  }

  @Test
  fun focusInViewStays() {
    val focus = item(300, 500, "Burger")
    val document = Node(viewport, null, true, true, focus, item(600, 800, "Fries"))
    inApp(document)
    // A scroll of a list in the page beside the focus.
    val list = Node(Rect(0, 900, 1000, 1200), null, false, true, item(950, 1100, "Drink"))
    list.parentNode = document
    val recovery = WebFocusRecovery(focus, null, list, false, true)

    run(recovery)

    assertEquals(WebFocusRecovery.IN_VIEW, recovery.outcome())
  }

  @Test
  fun aScrollOutsideTheWebViewThatHoldsTheFocusIsNotAWebScroll() {
    val focus = above("Burger")
    val document = Node(viewport, null, true, true, focus, item(300, 500, "Fries"))
    inApp(document)
    val otherDocument = Node(viewport, null, true, true, item(300, 500, "Ad"))
    inApp(otherDocument)
    val recovery = WebFocusRecovery(focus, null, otherDocument, false, true)

    run(recovery)

    assertEquals(WebFocusRecovery.NOT_WEB_SCROLL, recovery.outcome())
  }

  @Test
  fun lostFocusMovesToTheFirstItemInViewOfTheScrolledDocument() {
    val firstInView = item(300, 500, "Fries")
    val document =
      Node(viewport, null, true, true, above("Header"), above("Burger"), firstInView)
    inApp(document)
    val recovery = WebFocusRecovery(null, null, document, true, true)

    run(recovery)

    assertEquals(WebFocusRecovery.FOUND, recovery.outcome())
    assertSame(firstInView, recovery.item())
  }

  @Test
  fun lostFocusMovesToTheLastItemInViewAfterScrollingBack() {
    val lastInView = item(1500, 1900, "Fries")
    val document =
      Node(
        viewport,
        null,
        true,
        true,
        item(300, 500, "Salad"),
        lastInView,
        Node(Rect(0, 2300, 1000, 2000), text = "Burger"),
      )
    inApp(document)
    val recovery = WebFocusRecovery(null, null, document, true, false)

    run(recovery)

    assertSame(lastInView, recovery.item())
  }

  @Test
  fun lostFocusInAListOfThePageIsSoughtInThatList() {
    val card = item(950, 1100, "Drink")
    val list =
      Node(Rect(0, 900, 1000, 1200), null, false, true, Node(Rect(-300, 950, 0, 1100)), card)
    val document = Node(viewport, null, true, true, item(300, 500, "Burger"), list)
    inApp(document)
    val recovery = WebFocusRecovery(null, null, list, true, true)

    run(recovery)

    assertSame(card, recovery.item())
  }

  @Test
  fun lostFocusIsSoughtNextToWhereTheItemThatHadItWasNotAtTheStartOfThePage() {
    // A header that stays at the top of the page, then the section that held the focus.
    val header = item(150, 250, "McDonald's")
    val firstInView = item(600, 800, "Fries")
    val section =
      Node(Rect(0, 100, 1000, 2000), null, false, true, above("Salad"), firstInView)
    val document = Node(viewport, null, true, true, header, section)
    inApp(document)
    // The page removed the item that had focus, but it still names its parent.
    val removed = above("Burger").apply { parentNode = section }
    val recovery = WebFocusRecovery(null, removed, document, true, true)

    run(recovery)

    assertSame(firstInView, recovery.item())
  }

  @Test
  fun lostFocusOutsideTheScrolledContentIsSoughtFromItsStart() {
    val firstInView = item(300, 500, "Fries")
    val document = Node(viewport, null, true, true, above("Salad"), firstInView)
    inApp(document)
    val elsewhere = item(300, 500, "Other page")
    inApp(Node(viewport, null, true, true, elsewhere))
    val recovery = WebFocusRecovery(null, elsewhere, document, true, true)

    run(recovery)

    assertSame(firstInView, recovery.item())
  }

  @Test
  fun lostFocusWithoutAWebViewAboveTheScrolledNodeIsNotAWebScroll() {
    val list = Node(viewport, null, false, true, item(300, 500, "Fries"))
    val recovery = WebFocusRecovery(null, null, list, true, true)

    run(recovery)

    assertEquals(WebFocusRecovery.NOT_WEB_SCROLL, recovery.outcome())
  }

  @Test
  fun aStoppedRecoveryFindsNothing() {
    val focus = above("Burger")
    val document = Node(viewport, null, true, true, focus, item(300, 500, "Fries"))
    inApp(document)
    val recovery = WebFocusRecovery(focus, null, document, false, true)

    recovery.step()
    recovery.stop()

    assertEquals(WebFocusRecovery.NOTHING_IN_VIEW, recovery.outcome())
    assertTrue(recovery.step())
  }
}
