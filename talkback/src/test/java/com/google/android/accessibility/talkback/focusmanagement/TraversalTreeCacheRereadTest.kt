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

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.traversal.OrderedTraversalStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/** An incomplete order is read again in bulk at most once until its window changes. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TraversalTreeCacheRereadTest {
  private val window = 7

  @Before
  fun setUp() {
    TraversalTreeCache.clear("test")
  }

  private fun contentChange(changes: Int): AccessibilityEvent {
    val event = AccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
    event.contentChangeTypes = changes
    return event
  }

  @Test
  fun aWindowIsReadAgainOnce() {
    assertTrue(TraversalTreeCache.startReread(window))
    assertFalse(TraversalTreeCache.startReread(window))
  }

  @Test
  fun aStructuralChangeAllowsAnotherRead() {
    TraversalTreeCache.startReread(window)
    TraversalTreeCache.onAccessibilityEvent(
      contentChange(AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE)
    )
    assertTrue(TraversalTreeCache.startReread(window))
  }

  @Test
  fun aTextChangeOrFocusEventDoesNotAllowAnotherRead() {
    TraversalTreeCache.startReread(window)
    TraversalTreeCache.onAccessibilityEvent(
      contentChange(AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT)
    )
    TraversalTreeCache.onAccessibilityEvent(
      AccessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED)
    )
    assertFalse(TraversalTreeCache.startReread(window))
  }

  @Test
  fun aNodeActionAllowsAnotherRead() {
    TraversalTreeCache.startReread(window)
    TraversalTreeCache.onNodeAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD)
    assertTrue(TraversalTreeCache.startReread(window))
  }

  /** A root that lists children none of which can be read. */
  private class IncompleteRoot : AccessibilityNodeInfoCompat(rootInfo()) {
    override fun getChildCount(): Int = 2

    override fun getChild(index: Int): AccessibilityNodeInfoCompat? = null
  }

  @Test
  fun anIncompleteOrderIsSavedWithoutAllowingAnotherRead() {
    val root = IncompleteRoot()
    val order = OrderedTraversalStrategy(root)
    assertTrue(order.missesChildren())
    assertTrue(TraversalTreeCache.startReread(window))
    TraversalTreeCache.put(root, order)
    assertEquals(root, TraversalTreeCache.rootFor(root))
    assertFalse(TraversalTreeCache.startReread(window))
  }
}

private fun rootInfo(): AccessibilityNodeInfo {
  val info = AccessibilityNodeInfo.obtain()
  ReflectionHelpers.setField(info, "mSourceNodeId", 1L)
  info.isVisibleToUser = true
  return info
}
