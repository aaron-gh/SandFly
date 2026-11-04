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

package com.google.android.accessibility.talkback.focusmanagement.record

import android.os.SystemClock
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.FocusFinder
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import org.robolectric.util.ReflectionHelpers

/**
 * Finding the node to refocus after the screen changed asks the app for nodes on the main thread,
 * so it is done once per change, each ancestor is asked for once, and the search after a content
 * change has a time limit.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RefocusSearchTest {

  /** A node in a tree that counts what is asked of it. */
  private class Node(label: String, web: Boolean = false) :
    AccessibilityNodeInfoCompat(
      AccessibilityNodeInfo.obtain(View(RuntimeEnvironment.getApplication()))
    ) {
    var owner: Node? = null
    val children = mutableListOf<Node>()
    /** Whether the node is still in the app's tree. */
    var alive = true
    var refreshes = 0
    var childReads = 0
    /** How long the app takes to answer each request for this node. */
    var answerMs = 0L

    init {
      text = label
      className = "android.view.View"
      packageName = "example.app"
      isVisibleToUser = true
      if (web) {
        addAction(ACTION_NEXT_HTML_ELEMENT)
        addAction(ACTION_PREVIOUS_HTML_ELEMENT)
      }
    }

    fun add(vararg nodes: Node) = nodes.forEach {
      it.owner = this
      children.add(it)
    }

    private fun answer() {
      if (answerMs > 0) {
        ShadowSystemClock.advanceBy(Duration.ofMillis(answerMs))
      }
    }

    override fun refresh(): Boolean {
      refreshes++
      answer()
      return alive
    }

    override fun getParent(): AccessibilityNodeInfoCompat? = owner

    override fun getParent(prefetchingStrategy: Int): AccessibilityNodeInfoCompat? = owner

    override fun getChildCount() = children.size

    override fun getChild(index: Int): AccessibilityNodeInfoCompat? {
      childReads++
      answer()
      return children.getOrNull(index)
    }

    override fun getChild(index: Int, prefetchingStrategy: Int) = getChild(index)
  }

  private val focusFinder = FocusFinder(null)

  private fun newHistory() = AccessibilityFocusActionHistory(RuntimeEnvironment.getApplication())

  /** Seeks [record] in [root] between startRefocus and endRefocus, as after a content change. */
  private fun seekDuringRefocus(
    history: AccessibilityFocusActionHistory,
    root: AccessibilityNodeInfoCompat,
    record: FocusActionRecord,
  ): AccessibilityNodeInfoCompat? {
    history.startRefocus()
    try {
      return history.reader.getFocusableNodeFromFocusRecord(root, focusFinder, record)
    } finally {
      history.endRefocus()
    }
  }

  /** Returns [path] with the ancestors' positions read until [deadline]. */
  private fun withSiblingContext(
    path: NodePathDescription,
    deadline: NodePathDescription.Deadline,
  ): List<NodeDescription> =
    descriptions(NodePathDescription(path).apply { addSiblingContext(1, deadline) })

  private fun record(node: AccessibilityNodeInfoCompat) =
    FocusActionRecord(node, FocusActionInfo.builder().build(), /* actionTime= */ 1000)

  private fun descriptions(path: NodePathDescription): List<NodeDescription> =
    ReflectionHelpers.getField(path, "nodeDescriptions")

  /** Returns the root of a rebuilt page, which has nothing to focus yet. */
  private fun newRoot() =
    Node("").apply {
      text = null
      isVisibleToUser = false
    }

  /** Returns a chain of [depth] nodes below a root, the last of them the focused leaf. */
  private fun chain(depth: Int, web: Boolean = false): List<Node> {
    val nodes = List(depth + 1) { Node("node-$it", web) }
    for (i in 0 until depth) {
      nodes[i].add(nodes[i + 1])
    }
    return nodes
  }

  @Test
  fun aRecordNotFoundIsNotSoughtAgainInTheSameWindowDuringOneRefocus() {
    val nodes = chain(depth = 6, web = true)
    val record = record(nodes.last())
    // The page is rebuilt: none of the recorded nodes is left.
    nodes.forEach { it.alive = false }
    val root = newRoot()
    val history = AccessibilityFocusActionHistory(RuntimeEnvironment.getApplication())

    history.startRefocus()
    assertNull(history.reader.getFocusableNodeFromFocusRecord(root, focusFinder, record))
    val requests = nodes.sumOf { it.refreshes + it.childReads }
    assertTrue(requests > 0)
    assertNull(history.reader.getFocusableNodeFromFocusRecord(root, focusFinder, record))
    assertEquals(requests, nodes.sumOf { it.refreshes + it.childReads })
    history.endRefocus()

    // The next change on screen searches again.
    assertNull(history.reader.getFocusableNodeFromFocusRecord(root, focusFinder, record))
    assertTrue(nodes.sumOf { it.refreshes + it.childReads } > requests)
  }

  @Test
  fun outsideARefocusEverySearchAsksTheApp() {
    val nodes = chain(depth = 3)
    val record = record(nodes.last())
    nodes.forEach { it.alive = false }
    val root = newRoot()
    val history = AccessibilityFocusActionHistory(RuntimeEnvironment.getApplication())

    history.reader.getFocusableNodeFromFocusRecord(root, focusFinder, record)
    val requests = nodes.sumOf { it.refreshes }
    history.reader.getFocusableNodeFromFocusRecord(root, focusFinder, record)
    assertEquals(2 * requests, nodes.sumOf { it.refreshes })
  }

  @Test
  fun eachAncestorWithoutAUniqueIdIsRefreshedOnce() {
    val nodes = chain(depth = 5)
    val record = record(nodes.last())
    nodes.forEach { it.alive = false }

    newHistory().reader.getFocusableNodeFromFocusRecord(newRoot(), focusFinder, record)

    for (ancestor in nodes.dropLast(1)) {
      assertEquals(ancestor.text.toString(), 1, ancestor.refreshes)
    }
  }

  /** Removes [nodes] from the app, makes each of them answer in 100 ms, and resets the counts. */
  private fun removeAndSlowDown(nodes: List<Node>) =
    nodes.forEach {
      it.alive = false
      it.answerMs = 100
      it.refreshes = 0
      it.childReads = 0
    }

  @Test
  fun theSearchStopsAtItsTimeLimitDuringARefocus() {
    val nodes = chain(depth = 30)
    val record = record(nodes.last())
    removeAndSlowDown(nodes)
    val start = SystemClock.uptimeMillis()

    assertNull(seekDuringRefocus(newHistory(), newRoot(), record))

    // One refresh of the focused node before the search, then the requests that fit in the limit.
    val requests = nodes.sumOf { it.refreshes + it.childReads }
    assertTrue(
      "$requests requests",
      requests <= 1 + NodePathDescription.SEARCH_TIME_LIMIT_MS / 100 + 1,
    )
    assertTrue(SystemClock.uptimeMillis() - start <= NodePathDescription.SEARCH_TIME_LIMIT_MS + 200)
  }

  @Test
  fun aSearchOutsideARefocusHasNoTimeLimit() {
    val nodes = chain(depth = 30)
    val record = record(nodes.last())
    removeAndSlowDown(nodes)

    newHistory().reader.getFocusableNodeFromFocusRecord(newRoot(), focusFinder, record)

    // The upward search reaches the root.
    for (ancestor in nodes.dropLast(1)) {
      assertEquals(ancestor.text.toString(), 1, ancestor.refreshes)
    }
  }

  @Test
  fun aSearchStoppedAtItsTimeLimitIsSoughtAgainDuringTheSameRefocus() {
    val nodes = chain(depth = 30)
    val record = record(nodes.last())
    removeAndSlowDown(nodes)
    val root = newRoot()
    val history = newHistory()

    history.startRefocus()
    assertNull(history.reader.getFocusableNodeFromFocusRecord(root, focusFinder, record))
    val requests = nodes.sumOf { it.refreshes + it.childReads }
    assertNull(history.reader.getFocusableNodeFromFocusRecord(root, focusFinder, record))
    history.endRefocus()

    assertTrue(nodes.sumOf { it.refreshes + it.childReads } > requests)
  }

  @Test
  fun theAncestorsNearestTheRootAreLeftWithoutPositionsAtTheTimeLimit() {
    // root > outer > group > (first, leaf, last), in web content.
    val root = Node("root", web = true)
    val outer = Node("outer", web = true)
    val group = Node("group", web = true)
    val first = Node("first", web = true)
    val leaf = Node("leaf", web = true)
    val last = Node("last", web = true)
    root.add(outer)
    outer.add(group)
    group.add(first, leaf, last)
    val record = record(leaf)
    // Finding the group among the children of outer takes the whole time limit.
    outer.answerMs = NodePathDescription.SEARCH_TIME_LIMIT_MS

    val path =
      withSiblingContext(
        record.ancestorIdentityPath,
        NodePathDescription.Deadline.afterSearchTimeLimit(),
      )

    assertEquals(1, path[0].rawIndexInParent)
    assertEquals(0, path[1].rawIndexInParent)
    assertEquals(NodeDescription.UNDEFINED_INDEX, path[2].rawIndexInParent)
    assertEquals(0, root.childReads)
  }

  @Test
  fun aPassedDeadlineReadsNoSiblings() {
    val nodes = chain(depth = 3, web = true)
    val record = record(nodes.last())
    nodes.forEach { it.childReads = 0 }

    val path =
      withSiblingContext(
        record.ancestorIdentityPath,
        NodePathDescription.Deadline(SystemClock.uptimeMillis()),
      )

    assertEquals(0, nodes.sumOf { it.childReads })
    assertTrue(path.drop(1).all { it.rawIndexInParent == NodeDescription.UNDEFINED_INDEX })
  }

  @Test
  fun siblingsNotReadByTheTimeLimitAreReadByTheNextSearch() {
    // root > outer > group > (first, leaf, last), in web content.
    val root = Node("root", web = true)
    val outer = Node("outer", web = true)
    val group = Node("group", web = true)
    val first = Node("first", web = true)
    val leaf = Node("leaf", web = true)
    val last = Node("last", web = true)
    root.add(outer)
    outer.add(group)
    group.add(first, leaf, last)
    val record = record(leaf)
    leaf.alive = false
    // Finding the group among the children of outer takes the whole time limit.
    outer.answerMs = NodePathDescription.SEARCH_TIME_LIMIT_MS
    val history = newHistory()

    seekDuringRefocus(history, newRoot(), record)

    // The search had its own time limit after the sibling reading reached its own.
    assertTrue(group.refreshes > 0)
    // The position of outer was not read, and is read the next time, without reading the group's
    // position again.
    assertEquals(0, root.childReads)
    outer.answerMs = 0
    val outerChildReads = outer.childReads
    val path = descriptions(record.nodePathDescription)
    assertTrue(root.childReads > 0)
    assertEquals(0, path[2].rawIndexInParent)
    assertEquals(0, path[1].rawIndexInParent)
    assertEquals(outerChildReads, outer.childReads)
  }

  @Test
  fun aReplacedListItemIsFoundAtItsPlace() {
    // root > list > (a, b, c): the list is refreshed, and b is replaced by a new node.
    val root = Node("root")
    val list = Node("list")
    val a = Node("a")
    val b = Node("b")
    val c = Node("c")
    root.add(list)
    list.add(a, b, c)
    val record = record(b)

    b.alive = false
    val newB = Node("b")
    newB.owner = list
    list.children[1] = newB

    val found =
      record.nodePathDescription.findNodeToRefocus(
        root,
        focusFinder,
        NodePathDescription.Deadline.afterSearchTimeLimit(),
      )

    assertEquals(newB, found)
  }
}
