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

package com.google.android.accessibility.utils.traversal

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
 * An order is incomplete when a node lists children and none of them can be read, as with the
 * containers of Xiaomi's action bar layout on HyperOS.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OrderedTraversalMissingChildrenTest {
  private var nextId = 1L

  private fun info(): AccessibilityNodeInfo {
    val info = AccessibilityNodeInfo.obtain()
    ReflectionHelpers.setField(info, "mSourceNodeId", nextId++)
    info.isVisibleToUser = true
    return info
  }

  /** A node with [children], of which the null ones cannot be read. */
  private inner class Node(private val children: List<AccessibilityNodeInfoCompat?>) :
    AccessibilityNodeInfoCompat(info()) {
    override fun getChildCount(): Int = children.size

    override fun getChild(index: Int): AccessibilityNodeInfoCompat? = children[index]
  }

  private fun leaf() = Node(emptyList())

  @Test
  fun aRootWhoseChildrenCannotBeReadMissesChildren() {
    assertTrue(OrderedTraversalStrategy(Node(listOf(null, null, null))).missesChildren())
  }

  @Test
  fun aContainerWhoseChildrenCannotBeReadMissesChildren() {
    val root = Node(listOf(leaf(), Node(listOf(null, null))))
    assertTrue(OrderedTraversalStrategy(root).missesChildren())
  }

  @Test
  fun aReadableTreeMissesNothing() {
    val root = Node(listOf(leaf(), Node(listOf(leaf(), leaf()))))
    assertFalse(OrderedTraversalStrategy(root).missesChildren())
  }

  @Test
  fun someChildrenThatCannotBeReadAreNotAMissingPart() {
    assertFalse(OrderedTraversalStrategy(Node(listOf(leaf(), null))).missesChildren())
  }

  @Test
  fun theChildrenOfWebContentAreNotRead() {
    val page = Node(listOf(null, null))
    page.addAction(AccessibilityNodeInfoCompat.ACTION_NEXT_HTML_ELEMENT)
    page.addAction(AccessibilityNodeInfoCompat.ACTION_PREVIOUS_HTML_ELEMENT)
    assertFalse(OrderedTraversalStrategy(Node(listOf(page))).missesChildren())
  }
}
