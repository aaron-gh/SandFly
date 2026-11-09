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

import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.traversal.OrderedTraversalController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Traversal attributes that name each other must not put a loop in the reading order. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OrderedTraversalLoopTest {

  @Test
  fun parentAndChildBeforeEachOther_keepTreeOrder() {
    val first = FakeNode("first")
    val second = FakeNode("second")
    val child = FakeNode("child", first, second)
    val parent = FakeNode("parent", child)
    val root = FakeNode("root", parent)
    parent.before = child
    child.before = parent
    first.before = second

    assertEquals(listOf("root", "parent", "child", "first", "second"), order(root))
  }

  @Test
  fun nodeOutsidePairMovesBeforeItsChild() {
    val child = FakeNode("child")
    val parent = FakeNode("parent", child)
    val other = FakeNode("other")
    val root = FakeNode("root", parent, other)
    parent.before = child
    child.before = parent
    other.before = child

    assertEquals(listOf("root", "parent", "other", "child"), order(root))
  }

  @Test
  fun rootAndChildBeforeEachOther_keepTreeOrder() {
    val child = FakeNode("child")
    val root = FakeNode("root", child)
    root.before = child
    child.before = root

    assertEquals(listOf("root", "child"), order(root))
  }

  /** Runs on a daemon thread because a loop in the tree never returns. */
  private fun order(root: FakeNode): List<String> {
    val names = java.util.Collections.synchronizedList(mutableListOf<String>())
    val worker = Thread {
      val controller = OrderedTraversalController()
      controller.setSpeakingNodesCache(HashMap())
      controller.initOrder(root, false)
      var node = controller.findFirst()
      while (node != null) {
        names += (node as FakeNode).name
        node = controller.findNext(node)
      }
    }
    worker.isDaemon = true
    worker.start()
    worker.join(10_000)
    assertFalse(
      "Traversal order did not finish: " + worker.stackTrace.take(3).joinToString(),
      worker.isAlive,
    )
    return names.toList()
  }

  @Suppress("DEPRECATION")
  private class FakeNode(val name: String, vararg children: FakeNode) :
    AccessibilityNodeInfoCompat(AccessibilityNodeInfo.obtain()) {
    private val children = children.toList()
    var before: FakeNode? = null

    override fun getChildCount() = children.size

    override fun getChild(index: Int): AccessibilityNodeInfoCompat = children[index]

    override fun getTraversalBefore(): AccessibilityNodeInfoCompat? = before

    override fun getTraversalAfter(): AccessibilityNodeInfoCompat? = null

    override fun equals(other: Any?) = this === other

    override fun hashCode() = System.identityHashCode(this)
  }
}
