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

import com.google.android.accessibility.talkback.focusmanagement.WebTreeOrder.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Moving through web content by the order of the page's node tree. */
@RunWith(JUnit4::class)
class WebTreeOrderTest {

  private class Node(
    val text: String? = null,
    val actionable: Boolean = false,
    val frame: Boolean = false,
    val web: Boolean = true,
    val control: Boolean = false,
    vararg kids: Node,
  ) {
    var parent: Node? = null
    val children = kids.toList()

    init {
      children.forEach { it.parent = this }
    }

    override fun toString() = text ?: "node"
  }

  private var reads = 0

  private val tree =
    object : WebTreeOrder.Tree<Node> {
      override fun parent(node: Node): Node? {
        reads++
        return node.parent
      }

      override fun childCount(node: Node) = node.children.size

      override fun child(node: Node, index: Int): Node? {
        reads++
        return node.children.getOrNull(index)
      }

      override fun isWeb(node: Node) = node.web || node.frame

      override fun isFrame(node: Node) = node.frame

      override fun kind(node: Node, childCount: Int) =
        WebTreeOrder.kindOf(
          node.actionable,
          WebTreeOrder.hasText(node.text),
          node.control,
          childCount,
        )
    }

  private fun text(text: String) = Node(text)

  private fun container(vararg kids: Node) = Node(null, false, false, true, false, *kids)

  private fun page(vararg kids: Node) = Node("Claude", true, true, true, false, *kids)

  /** A link, which has its text again in its children. */
  private fun link(vararg kids: Node) =
    Node(kids.joinToString(" ") { it.text ?: "" }, true, false, true, true, *kids)

  /** A section that can be clicked, which is not a control. */
  private fun clickable(vararg kids: Node) = Node(null, true, false, true, false, *kids)

  private fun next(pivot: Node) = WebTreeOrder.find(tree, pivot, /* forward= */ true)

  private fun previous(pivot: Node) = WebTreeOrder.find(tree, pivot, /* forward= */ false)

  // The message that Chromium skipped parts of, as the WebView gives it.
  private val heading = text("Claude")
  private val whatLabel = text("What the log shows:")
  private val whatText = text(" a Telegram post was read in 8 pieces")
  private val likelyLabel = text("Likely cause (not confirmed):")
  private val likelyText = text(" realme's Android pausing the RHVoice app")
  private val bullet = text("• ")
  private val bulletText = text("With Low-latency audio on")
  private val draftLabel = text("Draft reply")
  private val notPosted = text(" (not posted):")
  private val quoted = text("The log shows RHVoice making no audio")
  private val decisions = text("Decisions needed:")
  private val root =
    page(
      heading,
      container(whatLabel, whatText),
      container(likelyLabel, likelyText),
      container(container(bullet, bulletText)),
      container(draftLabel, notPosted),
      container(quoted, container(container(text("1. "), text("Turn off")))),
      container(decisions),
    )

  @Before
  fun setUp() {
    WebTreeOrder.forgetPositions()
  }

  @Test
  fun forwardFromTheEndOfAParagraphGoesToTheStartOfTheNext() {
    assertSame(likelyLabel, next(whatText))
  }

  @Test
  fun forwardFromBoldTextGoesToTheTextAfterIt() {
    assertSame(notPosted, next(draftLabel))
  }

  @Test
  fun forwardFromAParagraphGoesIntoAQuote() {
    assertSame(quoted, next(notPosted))
  }

  @Test
  fun forwardGoesIntoAList() {
    assertSame(bullet, next(likelyText))
    assertSame(bulletText, next(bullet))
  }

  @Test
  fun backwardGoesToTheBoldTextBeforeIt() {
    assertSame(draftLabel, previous(notPosted))
  }

  @Test
  fun backwardFromTheStartOfAParagraphGoesToTheEndOfTheOneBefore() {
    assertSame(bulletText, previous(draftLabel))
    assertSame(whatText, previous(likelyLabel))
  }

  @Test
  fun backwardOutOfAQuoteGoesToItsLastItem() {
    val turnOff = (root.children[5].children[1].children[0].children[1])
    assertSame(turnOff, previous(decisions))
  }

  @Test
  fun thePageItselfIsEnteredAtItsFirstItem() {
    assertSame(heading, next(root))
  }

  @Test
  fun backwardFromThePageItselfAsksThePage() {
    assertNull(previous(root))
  }

  @Test
  fun theEndsOfThePageAskThePage() {
    assertNull(next(decisions))
    assertNull(previous(heading))
  }

  @Test
  fun whiteSpaceAndEmptyNodesArePassedOver() {
    val after = text("after")
    val start = text("start")
    page(container(start, text("  "), Node()), container(Node(" \n")), after)
    assertSame(after, next(start))
    assertSame(start, previous(after))
  }

  @Test
  fun aNodeWithoutChildrenThatCanBeActedOnIsAnItem() {
    val start = text("start")
    val image = Node(actionable = true)
    page(start, container(image))
    assertSame(image, next(start))
  }

  @Test
  fun aLinkIsOneItemWithoutItsText() {
    val before = text("Start ")
    val linkText = text("inline link")
    val inline = link(linkText)
    val after = text(" after link.")
    page(container(before, inline, after))
    assertSame(inline, next(before))
    assertSame(after, next(inline))
    assertSame(inline, previous(after))
    assertSame(before, previous(inline))
  }

  @Test
  fun aLinkAroundSeveralLinesIsOneItem() {
    val before = text("before")
    val block = link(container(text("line one")), container(text("line two")))
    val after = text("after")
    page(before, block, after)
    assertSame(block, next(before))
    assertSame(after, next(block))
    assertSame(block, previous(after))
  }

  @Test
  fun fromTheTextOfALinkFocusGoesOnPastTheLinkOrBackToIt() {
    val linkText = text("inline link")
    val inline = link(linkText, text("more"))
    val after = text("after")
    page(text("before"), inline, after)
    assertSame(after, next(linkText))
    assertSame(inline, previous(linkText))
  }

  @Test
  fun aControlInsideALinkIsAnItem() {
    val button = Node("Close", actionable = true)
    val card = link(text("Title"), button)
    val after = text("after")
    page(card, after)
    assertSame(button, next(card))
    assertSame(button, previous(after))
    assertSame(card, previous(button))
  }

  @Test
  fun aClickableSectionAsksThePage() {
    val start = text("start")
    val end = text("end")
    page(start, clickable(text("inside")), end)
    assertNull(next(start))
    assertNull(previous(end))
  }

  @Test
  fun aPivotInsideAClickableSectionAsksThePage() {
    val inside = text("inside")
    page(clickable(inside, text("more")), text("after"))
    assertNull(next(inside))
  }

  @Test
  fun ambiguousPivotDoesNotReadAncestorsInEitherDirection() {
    val pivot = clickable(text("inside"))
    var outer = pivot
    repeat(30) { outer = container(outer) }
    page(outer)
    for (forward in listOf(true, false)) {
      reads = 0
      assertNull(WebTreeOrder.find(tree, pivot, forward))
      assertEquals(0, reads)
    }
  }

  @Test
  fun ambiguousAncestorStopsBeforeTheRestOfTheChainInEitherDirection() {
    val pivot = text("inside")
    var outer = clickable(pivot, text("another item"))
    repeat(30) { outer = container(outer) }
    page(outer)
    for (forward in listOf(true, false)) {
      reads = 0
      assertNull(WebTreeOrder.find(tree, pivot, forward))
      assertEquals(1, reads)
    }
  }

  @Test
  fun framesAreEnteredAndEmptyFramesPassedOver() {
    val start = text("start")
    val inFrame = text("in frame")
    page(start, Node("empty frame", true, true), Node("frame", true, true, true, false, inFrame))
    assertSame(inFrame, next(start))
    assertSame(start, previous(inFrame))
  }

  @Test
  fun anItemOutsideWebContentAsksThePage() {
    val native = Node("native", web = false)
    assertNull(next(native))
  }

  @Test
  fun tooManyNodesAskThePage() {
    val start = text("start")
    val end = text("end")
    val empty = Array(WebTreeOrder.MAX_NODES_READ) { Node() }
    page(start, container(*empty), end)
    assertNull(next(start))
  }

  @Test
  fun goingOnFromAFoundItemReadsFewNodes() {
    val items = Array(WebTreeOrder.MAX_NODES_READ * 2) { text("item $it") }
    page(container(*items))
    var item = items[0]
    for (index in 1 until items.size) {
      reads = 0
      val found = next(item)
      assertSame(items[index], found)
      assertTrue("$reads reads", reads < 10)
      item = found!!
    }
  }

  @Test
  fun kinds() {
    assertEquals(Kind.TEXT, WebTreeOrder.kindOf(false, true, false, 0))
    assertEquals(Kind.ACTION, WebTreeOrder.kindOf(true, false, false, 0))
    assertEquals(Kind.ACTION, WebTreeOrder.kindOf(true, true, true, 0))
    assertEquals(Kind.SKIP, WebTreeOrder.kindOf(false, false, false, 0))
    assertEquals(Kind.CONTAINER, WebTreeOrder.kindOf(false, true, false, 2))
    assertEquals(Kind.CONTAINER, WebTreeOrder.kindOf(false, true, true, 2))
    assertEquals(Kind.CONTROL, WebTreeOrder.kindOf(true, true, true, 2))
    assertEquals(Kind.UNSURE, WebTreeOrder.kindOf(true, false, false, 2))
    assertFalse(WebTreeOrder.hasText(" \n\t"))
    assertFalse(WebTreeOrder.hasText(null))
    assertTrue(WebTreeOrder.hasText(" a "))
  }
}
