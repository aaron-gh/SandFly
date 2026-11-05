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
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Checks which scrollable items Scroll left and Scroll right may scroll. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScrollsSidewaysTest {

  private fun node(
    className: String,
    vararg actions: AccessibilityAction,
    collection: AccessibilityNodeInfo.CollectionInfo? = null,
  ): AccessibilityNodeInfoCompat {
    val node = AccessibilityNodeInfo()
    node.className = className
    node.isScrollable = true
    actions.forEach { node.addAction(it) }
    node.collectionInfo = collection
    return AccessibilityNodeInfoCompat.wrap(node)
  }

  @Test
  fun verticalScrollViewDoesNotScrollSideways() {
    assertFalse(
      FocusProcessorForLogicalNavigation.scrollsSideways(
        node(
          "android.widget.ScrollView",
          AccessibilityAction.ACTION_SCROLL_FORWARD,
          AccessibilityAction.ACTION_SCROLL_DOWN,
        )
      )
    )
  }

  @Test
  fun verticalListDoesNotScrollSideways() {
    assertFalse(
      FocusProcessorForLogicalNavigation.scrollsSideways(
        node(
          "androidx.recyclerview.widget.RecyclerView",
          AccessibilityAction.ACTION_SCROLL_FORWARD,
          collection = AccessibilityNodeInfo.CollectionInfo(20, 1, false),
        )
      )
    )
  }

  @Test
  fun pagerWithPageActionsScrollsSideways() {
    assertTrue(
      FocusProcessorForLogicalNavigation.scrollsSideways(
        node(
          "androidx.viewpager.widget.ViewPager",
          AccessibilityAction.ACTION_SCROLL_FORWARD,
          AccessibilityAction.ACTION_PAGE_RIGHT,
        )
      )
    )
  }

  @Test
  fun horizontalScrollViewScrollsSideways() {
    assertTrue(
      FocusProcessorForLogicalNavigation.scrollsSideways(
        node("android.widget.HorizontalScrollView", AccessibilityAction.ACTION_SCROLL_FORWARD)
      )
    )
  }

  @Test
  fun oneRowListScrollsSideways() {
    assertTrue(
      FocusProcessorForLogicalNavigation.scrollsSideways(
        node(
          "androidx.recyclerview.widget.RecyclerView",
          AccessibilityAction.ACTION_SCROLL_FORWARD,
          collection = AccessibilityNodeInfo.CollectionInfo(1, 8, false),
        )
      )
    )
  }
}
