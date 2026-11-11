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
import com.google.android.accessibility.utils.WebInterfaceUtils
import com.google.android.accessibility.utils.traversal.OrderedTraversalStrategy
import com.google.android.accessibility.utils.traversal.TraversalStrategy
import com.google.android.accessibility.utils.traversal.TraversalStrategyUtils
import java.util.function.Supplier
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/** The order worked out around a node is the same as the whole window's order, or not used. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalTraversalOrderTest {
  private var nextId = 1L

  /** A node of a made-up window, laid out in [bounds]. */
  private inner class Node(
    val name: String,
    bounds: Rect,
    text: String? = null,
    clickable: Boolean = false,
    visible: Boolean = true,
    web: Boolean = false,
  ) : AccessibilityNodeInfoCompat(info(bounds, text, clickable, visible, web)) {
    val kids = ArrayList<Node>()
    var up: Node? = null
    var before: Node? = null
    var after: Node? = null

    fun add(vararg children: Node): Node {
      for (child in children) {
        kids.add(child)
        child.up = this
      }
      return this
    }

    override fun getChildCount(): Int = kids.size

    override fun getChild(index: Int): AccessibilityNodeInfoCompat? = kids[index]

    override fun getParent(): AccessibilityNodeInfoCompat? = up

    override fun getTraversalBefore(): AccessibilityNodeInfoCompat? = before

    override fun getTraversalAfter(): AccessibilityNodeInfoCompat? = after

    override fun toString() = name
  }

  private fun info(
    bounds: Rect,
    text: String?,
    clickable: Boolean,
    visible: Boolean,
    web: Boolean,
  ): AccessibilityNodeInfo {
    val info = AccessibilityNodeInfo.obtain()
    ReflectionHelpers.setField(info, "mSourceNodeId", nextId++)
    info.setBoundsInScreen(bounds)
    info.isVisibleToUser = visible
    info.isClickable = clickable
    info.isFocusable = clickable
    info.text = text
    info.className = if (text != null) "android.widget.TextView" else "android.view.ViewGroup"
    if (web) {
      info.addAction(AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT)
      info.addAction(AccessibilityNodeInfo.ACTION_PREVIOUS_HTML_ELEMENT)
    }
    return info
  }

  private var builds = 0

  @Before
  fun setUp() {
    TraversalTreeCache.clear("test")
    builds = 0
  }

  private fun all(root: Node): List<Node> = listOf(root) + root.kids.flatMap { all(it) }

  private fun wholeOrder(root: Node) = OrderedTraversalStrategy(root)

  /**
   * A local order around [pivot], carrying on with the nodes the last one read, as navigation does
   * while nothing changes, unless [fresh].
   */
  private fun localOrder(pivot: Node, root: Node, fresh: Boolean = false): LocalTraversalOrder? {
    if (fresh) {
      TraversalTreeCache.clear("test")
    }
    return LocalTraversalOrder.start(
      pivot,
      root,
      Supplier {
        builds++
        wholeOrder(root)
      },
    )
  }

  private fun search(order: TraversalStrategy, pivot: Node, direction: Int) =
    TraversalStrategyUtils.searchFocus(
      order,
      pivot,
      direction,
      NavigationTarget.createNodeFilter(NavigationTarget.TARGET_DEFAULT, order.speakingNodesCache),
    )

  /**
   * Counts of local orders that answered by themselves, that built the whole order, that found
   * their steps differ from the whole order's, and that gave another result because a node that
   * sets its place in the order was never read.
   */
  private data class Outcome(
    var answered: Int = 0,
    var fellBack: Int = 0,
    var notStarted: Int = 0,
    var mismatched: Int = 0,
    var unread: Int = 0,
  ) {
    fun add(other: Outcome) {
      answered += other.answered
      fellBack += other.fellBack
      notStarted += other.notStarted
      mismatched += other.mismatched
      unread += other.unread
    }
  }

  /**
   * Checks that, from every node of [root] and in both directions, each step and each search of a
   * local order, read afresh and carrying on with the nodes read before, give what the whole order
   * gives, or that the local order throws
   * [LocalTraversalOrder.MismatchException], for the navigation to be done again. Results may
   * differ only when one of [hints], the nodes that set their place in the order, was never read.
   */
  private fun assertSameAsWholeOrder(root: Node, hints: List<Node> = emptyList()): Outcome {
    val outcome = Outcome()
    val whole = wholeOrder(root)
    for (pivot in all(root)) for (fresh in listOf(true, false)) {
      for (direction in
        listOf(TraversalStrategy.SEARCH_FOCUS_FORWARD, TraversalStrategy.SEARCH_FOCUS_BACKWARD)) {
        val expectedSearch = search(whole, pivot, direction)
        val expectedStep = whole.findFocus(pivot, direction)
        val buildsBefore = builds
        val local = localOrder(pivot, root, fresh)
        if (local == null) {
          outcome.notStarted++
          continue
        }
        try {
          val found = search(local, pivot, direction)
          val step = local.findFocus(pivot, direction)
          if (found != expectedSearch || step != expectedStep) {
            assertTrue(
              "from $pivot in $direction: search $found, not $expectedSearch; " +
                "step $step, not $expectedStep",
              hints.any { !local.holds(it) },
            )
            outcome.unread++
          } else if (builds == buildsBefore) {
            outcome.answered++
          } else {
            outcome.fellBack++
          }
        } catch (e: LocalTraversalOrder.MismatchException) {
          assertTrue("mismatch without hints from $pivot", hints.isNotEmpty())
          outcome.mismatched++
        }
        local.finish()
      }
    }
    return outcome
  }

  private fun rect(left: Int, top: Int, right: Int, bottom: Int) = Rect(left, top, right, bottom)

  /** A screen with a title, a list of [rows] clickable rows with two lines each, and a button. */
  private fun settingsScreen(rows: Int): Node {
    val root = Node("root", rect(0, 0, 1000, 2000))
    val title = Node("title", rect(0, 0, 1000, 100), text = "Settings")
    val list = Node("list", rect(0, 100, 1000, 1900))
    for (i in 0 until rows) {
      val top = 100 + i * 100
      val row = Node("row$i", rect(0, top, 1000, top + 100), clickable = true)
      row.add(
        Node("row$i.title", rect(10, top, 990, top + 50), text = "Item $i"),
        Node("row$i.summary", rect(10, top + 50, 990, top + 100), text = "Summary $i"),
      )
      list.add(row)
    }
    val button = Node("button", rect(0, 1900, 1000, 2000), text = "OK", clickable = true)
    return root.add(title, list, button)
  }

  @Test
  fun nextAndPreviousRowsAreFoundAroundTheFocusedRow() {
    val root = settingsScreen(rows = 10)
    val row = root.kids[1].kids[4]
    val forward = localOrder(row, root)!!
    assertEquals("row5", search(forward, row, TraversalStrategy.SEARCH_FOCUS_FORWARD).toString())
    val back = localOrder(row, root)!!
    assertEquals("row3", search(back, row, TraversalStrategy.SEARCH_FOCUS_BACKWARD).toString())
    assertEquals(0, builds)
  }

  @Test
  fun theLastRowGoesOnToTheButtonAndTheFirstBackToTheTitle() {
    val root = settingsScreen(rows = 5)
    val list = root.kids[1]
    val last = list.kids.last()
    assertEquals(
      "button",
      search(localOrder(last, root)!!, last, TraversalStrategy.SEARCH_FOCUS_FORWARD).toString(),
    )
    val first = list.kids.first()
    assertEquals(
      "title",
      search(localOrder(first, root)!!, first, TraversalStrategy.SEARCH_FOCUS_BACKWARD).toString(),
    )
    assertEquals(0, builds)
  }

  @Test
  fun theEndOfTheWindowBuildsTheWholeOrder() {
    val root = settingsScreen(rows = 3)
    val button = root.kids.last()
    assertNull(search(localOrder(button, root)!!, button, TraversalStrategy.SEARCH_FOCUS_FORWARD))
    assertEquals(1, builds)
    val title = root.kids.first()
    assertNull(search(localOrder(title, root)!!, title, TraversalStrategy.SEARCH_FOCUS_BACKWARD))
    assertEquals(2, builds)
  }

  @Test
  fun aSettingsScreenIsAnsweredLocallyExceptAtItsEdges() {
    val outcome = assertSameAsWholeOrder(settingsScreen(rows = 12))
    println("Settings screen: $outcome")
    assertTrue("$outcome", outcome.answered > outcome.fellBack * 4)
  }

  @Test
  fun nestedContainersAndInvisibleNodes() {
    val root = Node("root", rect(0, 0, 1000, 2000))
    val header = Node("header", rect(0, 0, 1000, 200))
    header.add(
      Node("back", rect(0, 0, 100, 100), clickable = true, text = "Back"),
      Node("hidden", rect(100, 0, 200, 100), text = "Hidden", visible = false),
      Node("heading", rect(200, 0, 1000, 100), text = "Heading"),
    )
    val body = Node("body", rect(0, 200, 1000, 1800))
    val group = Node("group", rect(0, 200, 1000, 800))
    group.add(
      Node("a", rect(0, 200, 1000, 400), text = "A"),
      Node("empty", rect(0, 400, 1000, 600)),
      Node("b", rect(0, 600, 1000, 800), text = "B", clickable = true)
        .add(Node("b.text", rect(0, 600, 1000, 800), text = "B text")),
    )
    body.add(group, Node("c", rect(0, 800, 1000, 1000), text = "C"))
    root.add(header, body, Node("footer", rect(0, 1800, 1000, 2000), text = "Footer"))
    val outcome = assertSameAsWholeOrder(root)
    assertTrue("$outcome", outcome.answered > 0)
  }

  @Test
  fun childrenOrderedByWhereTheyAreOnScreen() {
    // The second container comes first in the node tree, but its item is below the first's item,
    // and its padding covers the first: the whole order moves it after the first.
    val root = Node("root", rect(0, 0, 1000, 2000))
    val row = Node("row", rect(0, 0, 1000, 1000))
    val second = Node("second", rect(0, 0, 1000, 1000))
    second.add(Node("second.item", rect(0, 600, 1000, 700), text = "Second", clickable = true))
    val first = Node("first", rect(0, 100, 1000, 500))
    first.add(Node("first.item", rect(0, 100, 1000, 200), text = "First", clickable = true))
    row.add(second, first)
    root.add(Node("top", rect(0, 0, 1000, 50), text = "Top"), row, Node("end", rect(0, 1900, 1000, 2000), text = "End"))
    val whole = wholeOrder(root)
    assertEquals(listOf("root", "top", "row", "first", "first.item", "second", "second.item", "end"),
      whole.dumpTree().map { it.toString() })
    val outcome = assertSameAsWholeOrder(root)
    assertTrue("$outcome", outcome.answered > 0)
  }

  @Test
  fun traversalOrderAmongSiblingsIsWorkedOutHere() {
    for (useBefore in listOf(true, false)) {
      val root = settingsScreen(rows = 6)
      val list = root.kids[1]
      val moved = list.kids[1]
      if (useBefore) moved.before = list.kids[4] else moved.after = list.kids[4]
      assertTrue(wholeOrder(root).movesOnlyAmongSiblings())
      val outcome = assertSameAsWholeOrder(root)
      assertTrue("$outcome", outcome.answered > outcome.fellBack * 4)
    }
  }

  @Test
  fun anActionBarBeforeTheContentIsWorkedOutHere() {
    // AppCompat's action bar container sets its place before the content, its sibling, which comes
    // first in the node tree.
    val root = Node("root", rect(0, 0, 1000, 2000))
    val decor = Node("decor", rect(0, 0, 1000, 2000))
    val content = Node("content", rect(0, 0, 1000, 2000))
    val list = Node("list", rect(0, 200, 1000, 2000))
    for (i in 0 until 8) {
      val top = 200 + i * 200
      list.add(Node("row$i", rect(0, top, 1000, top + 200), text = "Row $i", clickable = true))
    }
    content.add(list)
    val actionBar = Node("actionBar", rect(0, 0, 1000, 200))
    actionBar.add(
      Node("up", rect(0, 0, 200, 200), text = "Navigate up", clickable = true),
      Node("title", rect(200, 0, 1000, 200), text = "Title"),
    )
    actionBar.before = content
    decor.add(content, actionBar)
    root.add(decor)
    val whole = wholeOrder(root)
    assertTrue(whole.movesOnlyAmongSiblings())
    assertEquals(
      listOf("root", "decor", "actionBar", "up", "title", "content", "list") +
        (0 until 8).map { "row$it" },
      whole.dumpTree().map { it.toString() },
    )
    val outcome = assertSameAsWholeOrder(root)
    assertTrue("$outcome", outcome.answered > outcome.fellBack * 3)
    // The first row goes back to the title, which is before the content.
    val first = list.kids.first()
    assertEquals(
      "title",
      search(localOrder(first, root)!!, first, TraversalStrategy.SEARCH_FOCUS_BACKWARD).toString(),
    )
  }

  @Test
  fun traversalOrderSetAcrossContainersBuildsTheWholeOrder() {
    val root = settingsScreen(rows = 4)
    val list = root.kids[1]
    val moved = list.kids[2]
    moved.after = root.kids.last()
    assertFalse(wholeOrder(root).movesOnlyAmongSiblings())
    val outcome = assertSameAsWholeOrder(root, hints = listOf(moved))
    assertTrue("$outcome", outcome.fellBack + outcome.mismatched > 0)
  }

  @Test
  fun aNodeInTwoMovesIsNotOnlyAmongSiblings() {
    val root = settingsScreen(rows = 4)
    val list = root.kids[1]
    list.kids[0].before = list.kids[2]
    list.kids[1].after = list.kids[2]
    assertFalse(wholeOrder(root).movesOnlyAmongSiblings())
    val outcome = assertSameAsWholeOrder(root, hints = listOf(list.kids[0], list.kids[1]))
    assertTrue("$outcome", outcome.fellBack > 0)
  }

  @Test
  fun webContentIsLeftToTheWebOrder() {
    val root = Node("root", rect(0, 0, 1000, 2000))
    val webView = Node("web", rect(0, 100, 1000, 1900), web = true)
    val paragraph = Node("p", rect(0, 100, 1000, 200), text = "Paragraph", web = true)
    webView.add(paragraph)
    root.add(Node("top", rect(0, 0, 1000, 100), text = "Top"), webView, Node("end", rect(0, 1900, 1000, 2000), text = "End"))
    assertNull(localOrder(paragraph, root))
    assertSameAsWholeOrder(root)
  }

  @Test
  fun aWideParentIsLeftToTheWholeOrder() {
    val root = settingsScreen(rows = LocalTraversalOrder.MAX_CHILDREN + 1)
    val row = root.kids[1].kids[3]
    assertNull(localOrder(row, root))
  }

  @Test
  fun aNodeOutsideTheWindowIsNotStarted() {
    val root = settingsScreen(rows = 3)
    val other = Node("other", rect(0, 0, 10, 10), text = "Other")
    assertNull(localOrder(other, root))
  }

  @Test
  fun randomTreesGiveTheWholeOrder() {
    val total = Outcome()
    var reordered = 0
    for (seed in 0 until 150) {
      val random = Random(seed)
      val root = Node("root", rect(0, 0, 1000, 4000))
      fill(root, Rect(0, 0, 1000, 4000), random, depth = 0)
      val whole = wholeOrder(root)
      assertTrue(whole.movesOnlyAmongSiblings())
      if (whole.dumpTree() != preorder(root)) reordered++
      total.add(assertSameAsWholeOrder(root))
    }
    println("Random trees: $total")
    assertEquals("$total", 0, total.mismatched + total.unread)
    assertTrue("$total", total.answered > total.fellBack)
    // Enough of the trees have children that the whole order moves by where they are on screen.
    assertTrue("$reordered", reordered > 10)
  }

  /** The nodes of [node] in tree order, leaving out web content, as the whole order does. */
  private fun preorder(node: Node): List<Node> =
    if (WebInterfaceUtils.supportsWebActions(node)) listOf(node)
    else listOf(node) + node.kids.flatMap { preorder(it) }

  /**
   * Adds random children to [parent], laid out in [area], sometimes out of tree order, and
   * sometimes with a container that covers all of [area] while its own children are in one part
   * of it, as an app bar over a list is.
   */
  private fun fill(parent: Node, area: Rect, random: Random, depth: Int) {
    val count = if (depth >= 4) 0 else random.nextInt(if (depth == 0) 3 else 0, 6)
    if (count == 0) return
    // Padding makes a container that can't be focused take its children's bounds for ordering.
    val pad = if (random.nextBoolean()) 0 else 5
    val inner = Rect(area.left + pad, area.top + pad, area.right - pad, area.bottom - pad)
    val horizontal = random.nextInt(4) == 0
    val slots = (0 until count).toMutableList()
    if (random.nextInt(4) == 0) slots.shuffle(random)
    for (index in 0 until count) {
      val slot = slots[index]
      val bounds =
        if (horizontal) {
          val width = inner.width() / count
          Rect(inner.left + slot * width, inner.top, inner.left + (slot + 1) * width, inner.bottom)
        } else {
          val height = inner.height() / count
          Rect(inner.left, inner.top + slot * height, inner.right, inner.top + (slot + 1) * height)
        }
      val covers = random.nextInt(5) == 0
      val kind = random.nextInt(10)
      val child =
        Node(
          "${parent.name}.$index",
          if (covers) Rect(inner) else bounds,
          text = if (kind < 6) "Text ${parent.name}.$index" else null,
          clickable = kind in 3..5 || kind == 9,
          visible = random.nextInt(12) != 0,
          web = random.nextInt(40) == 0,
        )
      parent.add(child)
      fill(child, bounds, random, depth + 1)
    }
  }

  @Test
  fun aWindowWithoutTraversalOrderHasNoHints() {
    assertTrue(wholeOrder(settingsScreen(rows = 4)).movesOnlyAmongSiblings())
  }

  @Test
  fun predictionWithoutWholeOrderAnswersNullWhenUnsure() {
    val root = settingsScreen(rows = 3)
    val button = root.kids.last()
    val local = LocalTraversalOrder.start(button, root, /* buildFull= */ null)
    assertNotNull(local)
    assertNull(search(local!!, button, TraversalStrategy.SEARCH_FOCUS_FORWARD))
    assertFalse(local.containsNode(button))
  }

  @Test
  fun aNodeReadLaterThatMovesNextToAnEarlierStepThrows() {
    // The text in the container sets its place after the pivot, so the whole order goes from the
    // pivot into it. The local order finds the container first, which can't be focused, and reads
    // the text only then.
    val root = Node("root", rect(0, 0, 1000, 2000))
    val pivot = Node("pivot", rect(0, 100, 1000, 200), text = "Pivot")
    val container = Node("container", rect(0, 200, 1000, 400))
    val moved = Node("moved", rect(0, 200, 1000, 300), text = "Moved")
    container.add(moved)
    moved.after = pivot
    root.add(
      Node("top", rect(0, 0, 1000, 100), text = "Top"),
      pivot,
      container,
      Node("end", rect(0, 1900, 1000, 2000), text = "End"),
    )
    assertEquals(
      "moved",
      search(wholeOrder(root), pivot, TraversalStrategy.SEARCH_FOCUS_FORWARD).toString(),
    )
    val local = localOrder(pivot, root)!!
    assertThrows(LocalTraversalOrder.MismatchException::class.java) {
      search(local, pivot, TraversalStrategy.SEARCH_FOCUS_FORWARD)
    }
  }

  @Test
  fun randomTreesWithTraversalOrderGiveTheWholeOrderOrThrow() {
    val total = Outcome()
    for (seed in 0 until 150) {
      val random = Random(1000 + seed)
      val root = Node("root", rect(0, 0, 1000, 4000))
      fill(root, Rect(0, 0, 1000, 4000), random, depth = 0)
      val nodes = all(root)
      if (nodes.size < 3) continue
      val hints = ArrayList<Node>()
      repeat(random.nextInt(1, 3)) {
        val moving = nodes[random.nextInt(1, nodes.size)]
        val target = nodes[random.nextInt(1, nodes.size)]
        if (moving == target || moving.before != null || moving.after != null) return@repeat
        if (random.nextBoolean()) moving.before = target else moving.after = target
        hints.add(moving)
      }
      total.add(assertSameAsWholeOrder(root, hints))
    }
    println("Random trees with traversal order: $total")
    // The checks against the whole order catch some of the steps that differ.
    assertTrue("$total", total.mismatched > 0)
  }

  @Test
  fun randomTreesWithTraversalOrderAmongSiblingsGiveTheWholeOrder() {
    val total = Outcome()
    var used = 0
    for (seed in 0 until 200) {
      val random = Random(5000 + seed)
      val root = Node("root", rect(0, 0, 1000, 4000))
      fill(root, Rect(0, 0, 1000, 4000), random, depth = 0)
      val parents = all(root).filter { it.kids.size >= 2 }
      if (parents.isEmpty()) continue
      val moved = HashSet<Node>()
      repeat(random.nextInt(1, 4)) {
        val parent = parents[random.nextInt(parents.size)]
        val node = parent.kids[random.nextInt(parent.kids.size)]
        val sibling = parent.kids[random.nextInt(parent.kids.size)]
        if (node == sibling || node in moved || sibling in moved) return@repeat
        if (random.nextBoolean()) node.before = sibling else node.after = sibling
        moved.add(node)
        moved.add(sibling)
      }
      if (moved.isEmpty() || !wholeOrder(root).movesOnlyAmongSiblings()) continue
      used++
      try {
        total.add(assertSameAsWholeOrder(root))
      } catch (e: AssertionError) {
        val moves =
          all(root)
            .filter { it.before != null || it.after != null }
            .joinToString { if (it.before != null) "$it before ${it.before}" else "$it after ${it.after}" }
        throw AssertionError("Seed $seed, $moves: ${e.message}", e)
      }
    }
    println("Random trees with traversal order among siblings: $used trees, $total")
    assertTrue("$used", used > 100)
    assertTrue("$total", total.answered > total.fellBack)
  }
}
