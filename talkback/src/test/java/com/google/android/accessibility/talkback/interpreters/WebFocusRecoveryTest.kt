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
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.ConcurrentNodeQueries.ParentRead
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/** The steps that find an item of web content in view after the user scrolled the page. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WebFocusRecoveryTest {
  /** The WebView on screen. */
  private val viewport = Rect(0, 100, 1000, 2000)
  private var reads = 0
  private val parentThreads = java.util.Collections.synchronizedSet(mutableSetOf<Thread>())

  private class IdentityNode(val identity: Int, uid: String?, native: Boolean = false,
    val testWindow: Int = 12) : AccessibilityNodeInfoCompat(AccessibilityNodeInfo.obtain()) {
    init {
      uniqueId = uid
      className = if (native) "android.webkit.WebView" else "android.view.View"
      if (!native) addAction(ACTION_NEXT_HTML_ELEMENT)
    }
    override fun getWindowId() = testWindow
    override fun equals(other: Any?) = other is IdentityNode && identity == other.identity
      && testWindow == other.testWindow
    override fun hashCode() = identity * 31 + testWindow
  }

  @Test
  fun currentAncestorLinksKeepFreshMetadataOnlyAfterEveryLinkMatches() {
    val focus = IdentityNode(1, "item")
    val parent = IdentityNode(2, "section")
    val host = IdentityNode(3, null, true)
    val nowFocus = IdentityNode(1, "item").apply { isAccessibilityFocused = true }
    val nowParent = IdentityNode(2, "section")
    val cachedParent = IdentityNode(2, "section")
    val nowHost = IdentityNode(3, null, true)
    val current = WebFocusRecovery.verifiedAncestorNodes(listOf(focus, parent, host),
      mapOf(focus to ParentRead(nowFocus, cachedParent), parent to ParentRead(nowParent, nowHost)))
    assertEquals(listOf(nowFocus, nowParent, nowHost), current)
    assertSame(nowParent, current[1])
  }

  @Test
  fun aRecycledScrollSourceCannotContinueTheOldManualFling() {
    assertTrue(WebFocusRecovery.sameIdentity(IdentityNode(1, "old"), IdentityNode(1, "old")))
    assertFalse(WebFocusRecovery.sameIdentity(IdentityNode(1, "old"), IdentityNode(1, "new")))
    assertFalse(WebFocusRecovery.sameIdentity(IdentityNode(1, "old"), IdentityNode(1, "old", testWindow = 13)))
  }

  @Test
  fun aChangedParentOrRecycledIdentityRejectsTheOptionalPath() {
    val focus = IdentityNode(1, "item")
    val parent = IdentityNode(2, "section")
    val host = IdentityNode(3, null, true)
    val nowFocus = IdentityNode(1, "item").apply { isAccessibilityFocused = true }
    for (changed in listOf(IdentityNode(4, "other"), IdentityNode(2, "replacement"),
      IdentityNode(2, "section", testWindow = 13))) {
      assertTrue(WebFocusRecovery.verifiedAncestorNodes(listOf(focus, parent, host),
        mapOf(focus to ParentRead(nowFocus, changed), parent to ParentRead(parent, host))).isEmpty())
    }
  }

  @Test
  fun aMissingAnswerOrLostFocusKeepsTheLiveWalk() {
    val focus = IdentityNode(1, "item")
    val parent = IdentityNode(2, "section")
    val host = IdentityNode(3, null, true)
    val nowFocus = IdentityNode(1, "item").apply { isAccessibilityFocused = true }
    assertTrue(WebFocusRecovery.verifiedAncestorNodes(listOf(focus, parent, host),
      mapOf(focus to ParentRead(nowFocus, parent))).isEmpty())
    nowFocus.isAccessibilityFocused = false
    assertTrue(WebFocusRecovery.verifiedAncestorNodes(listOf(focus, parent, host),
      mapOf(focus to ParentRead(nowFocus, parent), parent to ParentRead(parent, host))).isEmpty())
  }

  @Test
  fun anUnverifiedVirtualDocumentDoesNotReplaceTheNativeViewport() {
    val focus = IdentityNode(1, "item").apply { isAccessibilityFocused = true }
    val document = IdentityNode(2, "document")
    assertTrue(WebFocusRecovery.verifiedAncestorNodes(listOf(focus, document),
      mapOf(focus to ParentRead(focus, document))).isEmpty())
  }


  @Test
  fun nativeAndVirtualScrollSourcesMustBeTheSameDirectlyOwnedDocument() {
    val host = IdentityNode(3, null, true)
    fun document(id: Int, uid: String) = IdentityNode(id, uid).apply {
      className = "android.webkit.WebView"
      extras.putString("AccessibilityNodeInfo.chromeRole", "rootWebArea")
    }
    val document = document(2, "document")
    val path = listOf(IdentityNode(1, "item"), document, host)
    assertTrue(WebFocusRecovery.sameNativeViewport(host, document, path))
    assertTrue(WebFocusRecovery.sameNativeViewport(document, host, path))
    assertFalse(WebFocusRecovery.sameNativeViewport(host, document(2, "replacement"), path))
    assertFalse(WebFocusRecovery.sameNativeViewport(IdentityNode(4, null, true), document, path))
    assertFalse(WebFocusRecovery.sameNativeViewport(host, document, emptyList()))
    val frame = document(5, "inner frame")
    val nested = listOf(frame, IdentityNode(6, "frame element"), document, host)
    assertFalse(WebFocusRecovery.sameNativeViewport(host, frame, nested))
  }

  private inner class Node(
    bounds: Rect,
    text: String? = null,
    webView: Boolean = false,
    web: Boolean = true,
    vararg children: Node,
  ) : AccessibilityNodeInfoCompat(AccessibilityNodeInfo.obtain()) {
    val nodes = children.toList()
    var parentNode: Node? = null
    var parentReads = 0
    var beforeParentRead: (() -> Unit)? = null

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

    override fun getChild(index: Int, flags: Int): AccessibilityNodeInfoCompat? = getChild(index)

    override fun getParent(flags: Int): AccessibilityNodeInfoCompat? = getParent()

    override fun getChildCount() = nodes.size

    override fun getChild(index: Int): AccessibilityNodeInfoCompat {
      reads++
      return nodes[index]
    }


    override fun refresh() = true

    override fun getParent(): AccessibilityNodeInfoCompat? {
      parentThreads.add(Thread.currentThread())
      beforeParentRead?.invoke()
      reads++
      parentReads++
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
  fun aBackgroundSearchKeepsTheSameTargetAndReadsOffTheCallerThread() {
    val executor = Executors.newSingleThreadExecutor()
    try {
      val focus = above("Old item")
      val target = item(500, 700, "Visible item")
      val document = Node(viewport, null, true, true, focus, target)
      inApp(document)
      val recovery = WebFocusRecovery(focus, null, document, true, true, null, false)
      val work = recovery.startInBackground(SystemClock.uptimeMillis() + 500) { read ->
        executor.submit<Boolean> { read.get() }
      }!!
      assertTrue(work.get(2, TimeUnit.SECONDS))
      assertSame(target, recovery.item())
      assertTrue(parentThreads.isNotEmpty())
      assertFalse(parentThreads.contains(Thread.currentThread()))
    } finally { executor.shutdownNow() }
  }

  @Test
  fun aCancelledSearchStopsAfterItsOutstandingNodeRead() {
    val executor = Executors.newSingleThreadExecutor()
    val entered = CountDownLatch(1)
    val release = CountDownLatch(1)
    try {
      val focus = above("Old item").apply {
        beforeParentRead = { entered.countDown(); release.await(2, TimeUnit.SECONDS) }
      }
      val document = Node(viewport, null, true, true, focus, item(500, 700, "Visible item"))
      inApp(document)
      val recovery = WebFocusRecovery(focus, null, document, true, true, null, false)
      val work = recovery.startInBackground(SystemClock.uptimeMillis() + 500) { read ->
        executor.submit<Boolean> { read.get() }
      }!!
      assertTrue(entered.await(2, TimeUnit.SECONDS))
      recovery.cancelBackground()
      release.countDown()
      assertFalse(work.get(2, TimeUnit.SECONDS))
      assertNull(recovery.item())
      assertEquals(1, reads)
    } finally { release.countDown(); executor.shutdownNow() }
  }

  @Test
  fun theStripIsCopiedBeforeTheWorkerStarts() {
    val focus = above("Old item")
    val strip = Node(Rect(0, 100, 1000, 200), null, false, true)
    val document = Node(viewport, null, true, true, focus, strip, item(500, 700, "Visible item"))
    inApp(document)
    val recovery = WebFocusRecovery(focus, null, document, true, true, strip, true)
    var stripWhenSubmitted: Any? = null
    recovery.startInBackground(SystemClock.uptimeMillis() + 500) { _ ->
      stripWhenSubmitted = ReflectionHelpers.getField(recovery, "strip")
      null
    }
    assertNotNull(stripWhenSubmitted)
    assertNotSame(strip, stripWhenSubmitted)
  }

  @Test
  fun anExpiredBackgroundBudgetDoesNotReadTheApp() {
    val focus = above("Old item")
    val document = Node(viewport, null, true, true, focus, item(500, 700, "Visible item"))
    inApp(document)
    val recovery = WebFocusRecovery(focus, null, document, true, true, null, false)
    val work = recovery.startInBackground(SystemClock.uptimeMillis() - 1) { read ->
      java.util.concurrent.FutureTask<Boolean> { read.get() }.apply { run() }
    }!!
    assertTrue(work.get())
    assertEquals(WebFocusRecovery.NOTHING_IN_VIEW, recovery.outcome())
    assertEquals(0, reads)
  }

  @Test
  fun aParentBatchUsesOnlyTheRemainingRecoveryBudget() {
    assertEquals(80, WebFocusRecovery.readyPathWaitMs(600, 100))
    assertEquals(7, WebFocusRecovery.readyPathWaitMs(107, 100))
    assertEquals(0, WebFocusRecovery.readyPathWaitMs(100, 100))
    assertEquals(0, WebFocusRecovery.readyPathWaitMs(99, 100))
  }

  @Test
  fun anUnavailableWorkerKeepsTheOriginalStepSearch() {
    val focus = above("Old item")
    val target = item(500, 700, "Visible item")
    val document = Node(viewport, null, true, true, focus, target)
    inApp(document)
    val recovery = WebFocusRecovery(focus, null, document, true, true, null, false)
    assertNull(recovery.startInBackground(SystemClock.uptimeMillis() + 500) { null })
    run(recovery)
    assertSame(target, recovery.item())
  }

  @Test
  fun aRejectedWorkerDoesNotJoinAParentBatchOnTheSerialFallback() {
    val focus = above("Old item").apply { uniqueId = "focus" }
    val target = item(500, 700, "Visible item")
    val document = Node(viewport, null, true, true, focus, target).apply { uniqueId = "document" }
    val host = Node(viewport, null, true, false, document)
    val expectedSteps = run(WebFocusRecovery(focus, null, document, true, true, null, false))
    val recovery = WebFocusRecovery(focus, null, document, true, true, null, false,
      listOf(focus, document, host))
    assertNull(recovery.startInBackground(SystemClock.uptimeMillis() + 500) { null })
    assertEquals(expectedSteps, run(recovery))
    assertSame(target, recovery.item())
  }

  @Test
  fun focusScrolledOutOfViewMovesToTheFirstItemInView() {
    val focus = above("Burger")
    val firstInView = item(300, 500, "Fries")
    val document = Node(viewport, null, true, true, above("Salad"), focus, firstInView)
    inApp(document)
    val recovery = WebFocusRecovery(focus, null, document, false, true, null, false)

    run(recovery)

    assertEquals(WebFocusRecovery.FOUND, recovery.outcome())
    assertSame(firstInView, recovery.item())
  }

  @Test
  fun aSidewaysAncestorMustBeKnownBeforeSelectingAnInnerVisibleSibling() {
    val focus = above("Old tab")
    val inner = Node(viewport, null, false, true, focus, item(300, 500, "Other tab content"))
    val strip = Node(Rect(0, 100, 1000, 600), null, false, true, inner).apply {
      addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_LEFT)
      addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_RIGHT)
    }
    val target = item(800, 1000, "Page content after the strip")
    val document = Node(viewport, null, true, true, strip, target)
    val host = Node(viewport, null, true, false, document)
    val recovery = WebFocusRecovery(focus, null, host, false, true, null, false)
    run(recovery)
    assertSame(target, recovery.item())
  }

  @Test
  fun aKnownWebScrollNeedsNoAncestorsAboveItsNativeWebView() {
    for (scrollTheHost in listOf(false, true)) {
      val focus = above("Burger")
      val firstInView = item(300, 500, "Fries")
      val document = Node(viewport, null, true, true, focus, firstInView)
      val host = Node(viewport, null, true, false, document)
      val container = Node(viewport, null, false, false, host)
      Node(viewport, null, false, false, container)
      val scrolled = if (scrollTheHost) host else document
      val recovery = WebFocusRecovery(focus, null, scrolled, false, true, null, false)

      run(recovery)

      assertEquals(WebFocusRecovery.FOUND, recovery.outcome())
      assertSame(firstInView, recovery.item())
      assertEquals(0, container.parentReads)
    }
  }

  @Test
  fun aBackwardWebScrollNeedsNoAncestorsAboveItsNativeWebView() {
    val lastInView = item(1500, 1900, "Fries")
    val focus = Node(Rect(0, 2100, 1000, 2100), text = "Burger")
    val document = Node(viewport, null, true, true, lastInView, focus)
    val host = Node(viewport, null, true, false, document)
    val container = Node(viewport, null, false, false, host)
    val recovery = WebFocusRecovery(focus, null, document, false, false, null, false)

    run(recovery)

    assertSame(lastInView, recovery.item())
    assertEquals(0, container.parentReads)
  }

  @Test
  fun aNativeContainerScrollingTheWebViewStillChecksItsAncestors() {
    val focus = above("Burger")
    val firstInView = item(300, 500, "Fries")
    val document = Node(viewport, null, true, true, focus, firstInView)
    val host = Node(viewport, null, true, false, document)
    val container = Node(viewport, null, false, false, host)
    val recovery = WebFocusRecovery(focus, null, container, false, true, null, false)

    run(recovery)

    assertEquals(WebFocusRecovery.FOUND, recovery.outcome())
    assertSame(firstInView, recovery.item())
    assertTrue(container.parentReads > 0)
  }

  @Test
  fun focusNotVisibleToTheUserMovesEvenWithBoundsInTheView() {
    val focus = item(300, 500, "Order again").apply { isVisibleToUser = false }
    val firstInView = item(600, 800, "Fries")
    val document = Node(viewport, null, true, true, focus, firstInView)
    inApp(document)
    val recovery = WebFocusRecovery(focus, null, document, true, true, null, false)

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
    val recovery = WebFocusRecovery(focus, null, list, false, true, null, false)

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
    val recovery = WebFocusRecovery(focus, null, otherDocument, false, true, null, false)

    run(recovery)

    assertEquals(WebFocusRecovery.NOT_WEB_SCROLL, recovery.outcome())
  }

  @Test
  fun lostFocusMovesToTheFirstItemInViewOfTheScrolledDocument() {
    val firstInView = item(300, 500, "Fries")
    val document =
      Node(viewport, null, true, true, above("Header"), above("Burger"), firstInView)
    inApp(document)
    val recovery = WebFocusRecovery(null, null, document, true, true, null, false)

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
    val recovery = WebFocusRecovery(null, null, document, true, false, null, false)

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
    val recovery = WebFocusRecovery(null, null, list, true, true, null, false)

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
    val recovery = WebFocusRecovery(null, removed, document, true, true, null, false)

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
    val recovery = WebFocusRecovery(null, elsewhere, document, true, true, null, false)

    run(recovery)

    assertSame(firstInView, recovery.item())
  }

  @Test
  fun lostFocusWithoutAWebViewAboveTheScrolledNodeIsNotAWebScroll() {
    val list = Node(viewport, null, false, true, item(300, 500, "Fries"))
    val recovery = WebFocusRecovery(null, null, list, true, true, null, false)

    run(recovery)

    assertEquals(WebFocusRecovery.NOT_WEB_SCROLL, recovery.outcome())
  }

  /** A strip of category tabs fixed below a header, as in an Uber Eats restaurant menu. */
  private fun strip() =
    Node(Rect(179, 550, 1000, 740), null, false, true, Node(Rect(179, 550, 400, 740), "Deals"))

  @Test
  fun focusThatScrolledUnderAStripFixedAcrossTheTopMovesBelowTheStrip() {
    val header = item(150, 540, "McDonald's")
    val strip = strip()
    // The card is clipped at the top of the view, and lies above the bottom of the strip.
    val focus = Node(Rect(57, 150, 598, 592), text = "Big Mac")
    val acrossStrip = item(600, 900, "Fries")
    val firstBelow = item(950, 1200, "McFlurry")
    val document =
      Node(viewport, null, true, true, header, strip, focus, acrossStrip, firstBelow)
    inApp(document)
    val recovery = WebFocusRecovery(focus, null, document, false, true, strip, true)

    run(recovery)

    assertEquals(WebFocusRecovery.FOUND, recovery.outcome())
    assertSame(firstBelow, recovery.item())
  }

  @Test
  fun focusInAHeaderAboveAFixedStripStays() {
    val header = item(150, 540, "McDonald's")
    val strip = strip()
    val document = Node(viewport, null, true, true, header, strip, item(950, 1200, "Fries"))
    inApp(document)
    val recovery = WebFocusRecovery(header, null, document, false, true, strip, false)

    run(recovery)

    assertEquals(WebFocusRecovery.IN_VIEW, recovery.outcome())
  }

  @Test
  fun focusThatTheStripCoversOnlyInPartStays() {
    val strip = strip()
    val focus = item(600, 900, "Big Mac")
    val document = Node(viewport, null, true, true, strip, focus, item(950, 1200, "Fries"))
    inApp(document)
    val recovery = WebFocusRecovery(focus, null, document, false, true, strip, true)

    run(recovery)

    assertEquals(WebFocusRecovery.IN_VIEW, recovery.outcome())
  }

  @Test
  fun lostFocusMovesBelowAStripFixedAcrossTheTop() {
    val header = item(150, 540, "McDonald's")
    val strip = strip()
    val firstBelow = item(950, 1200, "Fries")
    val document = Node(viewport, null, true, true, header, strip, firstBelow)
    inApp(document)
    val recovery = WebFocusRecovery(null, null, document, true, true, strip, true)

    run(recovery)

    assertSame(firstBelow, recovery.item())
  }

  @Test
  fun aStripInTheMiddleOfTheViewHidesNothing() {
    val strip = Node(Rect(0, 1000, 1000, 1200), null, false, true)
    val focus = item(300, 500, "Big Mac")
    val document = Node(viewport, null, true, true, focus, strip)
    inApp(document)
    val recovery = WebFocusRecovery(focus, null, document, false, true, strip, true)

    run(recovery)

    assertEquals(WebFocusRecovery.IN_VIEW, recovery.outcome())
  }

  @Test
  fun aStoppedRecoveryFindsNothing() {
    val focus = above("Burger")
    val document = Node(viewport, null, true, true, focus, item(300, 500, "Fries"))
    inApp(document)
    val recovery = WebFocusRecovery(focus, null, document, false, true, null, false)

    recovery.step()
    recovery.stop()

    assertEquals(WebFocusRecovery.NOTHING_IN_VIEW, recovery.outcome())
    assertTrue(recovery.step())
  }
}
