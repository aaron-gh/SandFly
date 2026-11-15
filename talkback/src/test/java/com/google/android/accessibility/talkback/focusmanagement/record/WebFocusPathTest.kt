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

import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * The focus path of an item of web content is read with the item's siblings, but without the
 * siblings of its ancestors until they are needed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WebFocusPathTest {
  private val ancestorsAndUninterruptible =
    AccessibilityNodeInfoCompat.FLAG_PREFETCH_ANCESTORS or
      AccessibilityNodeInfoCompat.FLAG_PREFETCH_UNINTERRUPTIBLE

  /** A node in a tree that counts what is read from it. */
  private class Node(label: String, web: Boolean = false) :
    AccessibilityNodeInfoCompat(
      AccessibilityNodeInfo.obtain(View(RuntimeEnvironment.getApplication()))
    ) {
    var owner: Node? = null
    val children = mutableListOf<Node>()
    var childReads = 0
    val parentReads = mutableListOf<Int?>()
    var failChildReads = false

    init {
      text = label
      className = "android.view.View"
      packageName = "example.web"
      if (web) {
        addAction(ACTION_NEXT_HTML_ELEMENT)
        addAction(ACTION_PREVIOUS_HTML_ELEMENT)
      }
    }

    fun add(vararg nodes: Node) = nodes.forEach {
      it.owner = this
      children.add(it)
    }

    override fun getParent(): AccessibilityNodeInfoCompat? {
      parentReads.add(null)
      return owner
    }

    override fun getParent(prefetchingStrategy: Int): AccessibilityNodeInfoCompat? {
      parentReads.add(prefetchingStrategy)
      return owner
    }

    override fun getChildCount() = children.size

    override fun getChild(index: Int): AccessibilityNodeInfoCompat? {
      childReads++
      if (failChildReads) {
        throw IllegalStateException("app went away")
      }
      return children.getOrNull(index)
    }

    override fun getChild(index: Int, prefetchingStrategy: Int) = getChild(index)
  }

  /** root > group > (first, leaf, last), all in web content. */
  private class Tree {
    val root = Node("root", web = true)
    val group = Node("group", web = true)
    val first = Node("first", web = true)
    val leaf = Node("leaf", web = true)
    val last = Node("last", web = true)

    init {
      root.add(group)
      group.add(first, leaf, last)
    }

    val childReads
      get() = root.childReads + group.childReads

    fun failChildReads() {
      root.failChildReads = true
      group.failChildReads = true
    }
  }

  private fun descriptions(path: NodePathDescription): List<NodeDescription> =
    ReflectionHelpers.getField(path, "nodeDescriptions")

  private fun record(node: AccessibilityNodeInfoCompat) =
    FocusActionRecord(node, FocusActionInfo.builder().build(), /* actionTime= */ 1000)

  @Test
  fun recordingWebFocusReadsOnlyTheSiblingsOfTheFocusedItem() {
    val tree = Tree()

    val record = record(tree.leaf)
    val copy = FocusActionRecord.copy(record)!!
    assertEquals(record, copy)
    assertEquals(record.hashCode(), copy.hashCode())
    assertTrue(record.ancestorIdentityPath.containsNodeByHashAndIdentity(tree.group))
    assertTrue(record.ancestorIdentityPath.containsNodeByHashAndIdentity(tree.root))

    assertEquals(0, tree.root.childReads)
    val path = descriptions(record.ancestorIdentityPath)
    assertEquals(listOf("leaf", "group", "root"), path.map { it.text.toString() })
    assertEquals(1, path[0].rawIndexInParent)
    assertEquals("first", path[0].previousSiblingText.toString())
    assertEquals("last", path[0].nextSiblingText.toString())
    assertEquals(NodeDescription.UNDEFINED_INDEX, path[1].rawIndexInParent)
  }

  @Test
  fun theFocusedItemKeepsItsPositionAfterItLeavesThePage() {
    val tree = Tree()
    val record = record(tree.leaf)
    // The page removes the item before the path is needed to find what replaced it.
    tree.group.children.remove(tree.leaf)

    val leaf = descriptions(record.nodePathDescription)[0]
    assertEquals(1, leaf.rawIndexInParent)
    assertEquals("first", leaf.previousSiblingText.toString())
    assertEquals("last", leaf.nextSiblingText.toString())
  }

  @Test
  fun theFocusedItemsPositionIsSoughtAmongALimitedNumberOfSiblings() {
    val root = Node("root", web = true)
    val siblings =
      List(NodePathDescription.MAX_END_SIBLINGS_READ + 10) { Node("item-$it", web = true) }
    root.add(*siblings.toTypedArray())

    val near = descriptions(record(siblings[2]).ancestorIdentityPath)[0]
    assertEquals(2, near.rawIndexInParent)

    root.childReads = 0
    val far = descriptions(record(siblings.last()).ancestorIdentityPath)[0]
    assertEquals(NodeDescription.UNDEFINED_INDEX, far.rawIndexInParent)
    assertNull(far.previousSiblingText)
    assertEquals(NodePathDescription.MAX_END_SIBLINGS_READ, root.childReads)
  }

  @Test
  fun aFailedSiblingReadWhenFocusMovesLeavesThePositionUndefined() {
    val tree = Tree()
    tree.failChildReads()

    val record = record(tree.leaf)

    val leaf = descriptions(record.ancestorIdentityPath)[0]
    assertEquals(NodeDescription.UNDEFINED_INDEX, leaf.rawIndexInParent)
    assertNull(leaf.previousSiblingText)
    assertEquals("leaf", leaf.text.toString())
  }

  @Test
  fun ancestorsAreReadInOneRequestEach() {
    val tree = Tree()
    record(tree.leaf)
    for (node in listOf(tree.leaf, tree.group, tree.root)) {
      assertEquals(listOf<Int?>(ancestorsAndUninterruptible), node.parentReads)
    }
  }

  @Test
  fun siblingsAreReadOnceWhenThePathIsNeededAndSharedWithCopies() {
    val tree = Tree()
    val record = record(tree.leaf)
    val copy = FocusActionRecord.copy(record)!!

    val path = record.nodePathDescription
    val leaf = descriptions(path)[0]
    assertEquals(1, leaf.rawIndexInParent)
    assertEquals("first", leaf.previousSiblingText.toString())
    assertEquals("last", leaf.nextSiblingText.toString())
    assertEquals(0, descriptions(path)[1].rawIndexInParent)

    val reads = tree.childReads
    assertTrue(tree.root.childReads > 0)
    assertSame(path, record.nodePathDescription)
    assertSame(path, copy.nodePathDescription)
    assertEquals(reads, tree.childReads)
  }

  @Test
  fun theCompletedPathMatchesOneReadAllAtOnce() {
    val tree = Tree()
    val deferred = record(tree.leaf).nodePathDescription
    val direct = NodePathDescription.obtain(tree.leaf)
    assertEquals(direct, deferred)
  }

  @Test
  fun theFocusedItemKeepsItsTextAndPositionFromTheTimeOfFocus() {
    val tree = Tree()
    val record = record(tree.leaf)
    // The page changes before the path is needed.
    tree.leaf.text = "changed"
    tree.group.children.add(0, Node("inserted"))
    tree.root.children.add(0, Node("inserted"))

    val path = descriptions(record.nodePathDescription)
    assertEquals("leaf", path[0].text.toString())
    assertEquals(1, path[0].rawIndexInParent)
    assertEquals("first", path[0].previousSiblingText.toString())
    // The ancestors' positions are read when the path is needed.
    assertEquals(1, path[1].rawIndexInParent)
  }

  @Test
  fun aFailedSiblingReadKeepsThePathWithoutSiblingsAndIsNotRetried() {
    val tree = Tree()
    val record = record(tree.leaf)
    tree.failChildReads()

    val path = record.nodePathDescription
    assertEquals(1, descriptions(path)[0].rawIndexInParent)
    assertEquals(NodeDescription.UNDEFINED_INDEX, descriptions(path)[1].rawIndexInParent)
    assertNull(descriptions(path)[1].previousSiblingText)
    val reads = tree.childReads

    assertSame(path, record.nodePathDescription)
    assertEquals(reads, tree.childReads)
  }

  @Test
  fun recordsOfDifferentFocusActionsDiffer() {
    val tree = Tree()
    val record = record(tree.leaf)
    val later = FocusActionRecord(tree.leaf, FocusActionInfo.builder().build(), 2000)
    assertNotEquals(record, later)
    assertEquals(0, tree.root.childReads)
  }

  @Test
  fun nativeFocusReadsEachParentOnceWithSiblings() {
    val root = Node("root")
    val group = Node("group")
    val first = Node("first")
    val leaf = Node("leaf")
    root.add(group)
    group.add(first, leaf)

    val record = record(leaf)

    for (node in listOf(leaf, group, root)) {
      assertEquals(listOf<Int?>(null), node.parentReads)
    }
    val description = descriptions(record.ancestorIdentityPath)[0]
    assertEquals(1, description.rawIndexInParent)
    assertEquals("first", description.previousSiblingText.toString())
    assertSame(record.ancestorIdentityPath, record.nodePathDescription)
  }

  @Test
  fun aLoopInTheTreeEndsThePath() {
    val a = Node("a", web = true)
    val b = Node("b", web = true)
    a.add(b)
    b.add(a)
    assertEquals(2, descriptions(record(a).ancestorIdentityPath).size)
    assertEquals(2, descriptions(NodePathDescription.obtain(a)).size)
  }
  @Test fun aNestedFrameRecordsTheOutermostDocumentIdentity() {
    val leaf = Node("leaf", web = true)
    val inner = Node("inner", web = true).apply {
      uniqueId = "inner-document"
      extras.putString("AccessibilityNodeInfo.chromeRole", "rootWebArea")
      add(leaf)
    }
    val outer = Node("outer", web = true).apply {
      uniqueId = "outer-document"
      extras.putString("AccessibilityNodeInfo.chromeRole", "rootWebArea")
      add(inner)
    }
    Node("native host").apply { add(outer) }
    assertEquals("outer-document", record(leaf).ancestorIdentityPath.savedChromiumDocument!!.uniqueId)
  }

  @Test fun aRemovedProviderIsNotSearchedAmongItsParentsReplacementChildren() {
    val root = Node("root")
    val frame = Node("frame")
    val host = Node("host").apply { className = "android.webkit.WebView" }
    val document = Node("document", web = true)
    val leaf = Node("focused item", web = true)
    root.add(Node("before frame"), frame)
    frame.add(host)
    host.add(document)
    document.add(leaf)
    val record = record(leaf)
    shadowOf(host.unwrap()).setRefreshReturnValue(false)
    host.failChildReads = true
    document.failChildReads = true
    frame.children.clear()
    repeat(40) { frame.add(Node("replacement $it")) }

    val path = descriptions(record.nodePathDescription)

    assertEquals(0, frame.childReads)
    assertEquals(-1, path[2].rawIndexInParent) // Missing native host, never in the new children.
    assertEquals(1, path[3].rawIndexInParent) // Its live frame still gets normal context.
    assertEquals("focused item", path[0].text.toString())
  }

  @Test fun aReplacedDocumentIsNotSearchedWhileItsNativeHostSurvives() {
    val frame = Node("frame")
    val host = Node("host").apply { className = "android.webkit.WebView" }
    val document = Node("old document", web = true).apply {
      extras.putString("AccessibilityNodeInfo.chromeRole", "rootWebArea")
    }
    val leaf = Node("focused item", web = true)
    frame.add(host)
    host.add(document)
    document.add(leaf)
    val record = record(leaf)
    shadowOf(document.unwrap()).setRefreshReturnValue(false)
    shadowOf(host.unwrap()).setRefreshReturnValue(true)
    document.failChildReads = true
    host.children.clear()
    repeat(40) { host.add(Node("new document $it", web = true)) }

    val path = descriptions(record.nodePathDescription)

    assertEquals(0, host.childReads)
    assertEquals(-1, path[1].rawIndexInParent)
    assertEquals(0, path[2].rawIndexInParent)
    assertEquals("focused item", path[0].text.toString())
  }

  @Test @Config(sdk = [32]) fun aRemovedProviderIsStillScopedWithoutParallelNodeQueries() {
    aRemovedProviderIsNotSearchedAmongItsParentsReplacementChildren()
  }

  @Test fun aRemovedMiddleAncestorDoesNotScanItsParentsReplacementChildren() {
    val tree = Tree()
    val record = record(tree.leaf)
    tree.root.children.clear()
    repeat(40) { tree.root.add(Node("replacement $it", web = true)) }
    val path = record.ancestorIdentityPath

    path.addSiblingContext(1, NodePathDescription.Deadline.none(),
      mapOf<AccessibilityNodeInfoCompat, AccessibilityNodeInfoCompat?>(tree.group to null))

    assertEquals(0, tree.root.childReads)
    assertEquals(-1, descriptions(path)[1].rawIndexInParent)
    assertEquals(1, descriptions(path)[0].rawIndexInParent)
    assertEquals("leaf", descriptions(path)[0].text.toString())
  }
}
