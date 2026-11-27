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
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * A swipe out of a scroll view doesn't scroll it when nothing after the focused item is off
 * screen, as with a dialog message of one long text.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OffscreenNothingAheadTest {
  private var nextId = 1L

  private inner class Node(className: String) : AccessibilityNodeInfoCompat(info(className)) {
    var parentNode: Node? = null
    val children = mutableListOf<Node>()

    override fun getParent(): AccessibilityNodeInfoCompat? = parentNode

    override fun getChildCount(): Int = children.size

    override fun getChild(index: Int): AccessibilityNodeInfoCompat? = children[index]

    fun add(vararg nodes: Node): Node {
      nodes.forEach {
        it.parentNode = this
        children.add(it)
      }
      return this
    }
  }

  @Suppress("DEPRECATION")
  private fun info(className: String) =
    AccessibilityNodeInfo.obtain().apply {
      ReflectionHelpers.setField(this, "mSourceNodeId", nextId++)
      this.className = className
      isVisibleToUser = true
      setBoundsInParent(Rect(0, 0, 900, 100))
      setBoundsInScreen(Rect(0, 0, 900, 100))
    }

  /** A scroll view 1000 px tall on screen. */
  @Suppress("DEPRECATION")
  private fun scrollView() =
    Node("android.widget.ScrollView").apply {
      isScrollable = true
      setBoundsInParent(Rect(0, 0, 900, 1000))
      setBoundsInScreen(Rect(0, 0, 900, 1000))
    }

  /** A text 3000 px tall, of which the top 1000 px are on screen. */
  @Suppress("DEPRECATION")
  private fun longText() =
    Node("android.widget.TextView").apply {
      text = "Changes"
      setBoundsInParent(Rect(0, 0, 900, 3000))
      setBoundsInScreen(Rect(0, 0, 900, 1000))
    }

  /** A button scrolled out of view. */
  private fun offscreenButton() =
    Node("android.widget.Button").apply {
      text = "OK"
      isFocusable = true
      isClickable = true
      isVisibleToUser = false
      setBoundsInScreen(Rect())
    }

  @Test
  fun oneLongText_hasNothingAheadEitherWay() {
    val text = longText()
    val scroll = scrollView().add(Node("android.view.View"), text)
    assertTrue(OffscreenContent.hasNothingAhead(scroll, text, /* forward= */ true, /* rtl= */ false))
    assertTrue(OffscreenContent.hasNothingAhead(scroll, text, /* forward= */ false, /* rtl= */ false))
  }

  @Test
  fun indexedScrollView_keepsScrollingAtBothEdgesOfTheLoadedItems() {
    val first = Node("android.widget.Button").apply { text = "First loaded item" }
    val last = Node("android.widget.Button").apply { text = "Last loaded item" }
    val scroll =
      scrollView().add(first, last).apply {
        addAction(AccessibilityActionCompat.ACTION_SCROLL_TO_POSITION)
      }

    // A collection can report a ScrollView role without CollectionInfo and load more items later.
    assertTrue(OffscreenContent.keepsAllContent(scroll))
    assertFalse(OffscreenContent.hasNothingAhead(scroll, last, /* forward= */ true, /* rtl= */ false))
    assertFalse(OffscreenContent.hasNothingAhead(scroll, first, /* forward= */ false, /* rtl= */ false))
  }

  @Test
  fun offscreenButtonAfterText_isAheadForwardOnly() {
    val text = longText()
    val scroll = scrollView().add(text, offscreenButton())
    assertFalse(OffscreenContent.hasNothingAhead(scroll, text, /* forward= */ true, /* rtl= */ false))
    assertTrue(OffscreenContent.hasNothingAhead(scroll, text, /* forward= */ false, /* rtl= */ false))
  }

  @Test
  fun offscreenButtonBeforeText_isAheadBackwardOnly() {
    val text = longText()
    val scroll = scrollView().add(offscreenButton(), text)
    assertTrue(OffscreenContent.hasNothingAhead(scroll, text, /* forward= */ true, /* rtl= */ false))
    assertFalse(OffscreenContent.hasNothingAhead(scroll, text, /* forward= */ false, /* rtl= */ false))
  }

  @Test
  fun nestedScrollingContainerAfterText_mayHoldMore() {
    val message = longText()
    val nested =
      Node("android.widget.HorizontalScrollView")
        .apply { isScrollable = true }
        .add(Node("android.widget.Button").apply { text = "Next" })
    val scroll = scrollView().add(message, nested)
    assertFalse(OffscreenContent.hasNothingAhead(scroll, message, /* forward= */ true, /* rtl= */ false))
  }

  /** A notification row of the shade, fully on screen from [top] to [bottom]. */
  @Suppress("DEPRECATION")
  private fun row(title: String, top: Int, bottom: Int) =
    Node("android.widget.FrameLayout").apply {
      text = title
      isFocusable = true
      setBoundsInParent(Rect(0, 0, 900, bottom - top))
      setBoundsInScreen(Rect(0, top, 900, bottom))
    }

  @Test
  fun shadeLeavesRowsPastTheEdgeOutOfTheTree_scrollsOnToThem() {
    // The notification shade's list says it is a scroll view, but rows past its edge aren't in
    // the tree, so nothing in the tree reaches past the edge.
    val first = row("Message 1", 0, 450)
    val last = row("Message 2", 450, 900)
    val scroll = scrollView().add(first, last)
    assertTrue(OffscreenContent.keepsAllContent(scroll))
    assertFalse(OffscreenContent.hasNothingAhead(scroll, last, /* forward= */ true, /* rtl= */ false))
    assertFalse(OffscreenContent.hasNothingAhead(scroll, first, /* forward= */ false, /* rtl= */ false))
  }

  @Test
  @Suppress("DEPRECATION")
  fun shadeRowCutOffAtTheEdge_scrollsOnToTheRowsPastIt() {
    // The last row shown is cut off by the shade's bottom edge; the rows past it aren't in the tree.
    val first = row("Message 1", 0, 600)
    val last =
      row("Message 2", 600, 1000).apply { setBoundsInParent(Rect(0, 0, 900, 450)) }
    val scroll = scrollView().add(first, last).apply { packageName = "com.android.systemui" }
    assertFalse(OffscreenContent.hasNothingAhead(scroll, last, /* forward= */ true, /* rtl= */ false))
  }

  @Test
  @Suppress("DEPRECATION")
  fun groupCutOffAtTheOtherEdge_scrollsOnToTheRowsAfterIt() {
    // An expanded group scrolled up past the top, with its last row fully on screen.
    val last = row("Message 8", 700, 900)
    val group =
      Node("android.widget.FrameLayout")
        .apply {
          setBoundsInParent(Rect(0, 0, 900, 1500))
          setBoundsInScreen(Rect(0, 0, 900, 900))
        }
        .add(row("Message 7", 500, 700), last)
    val scroll = scrollView().add(group)
    assertFalse(OffscreenContent.hasNothingAhead(scroll, last, /* forward= */ true, /* rtl= */ false))
  }

  @Test
  @Suppress("DEPRECATION")
  fun silentContentPastTheEdge_hasNothingAhead() {
    val text = row("Changes", 0, 900)
    val spacer =
      Node("android.view.View").apply {
        isVisibleToUser = false
        setBoundsInScreen(Rect())
      }
    val scroll = scrollView().add(text, spacer)
    assertTrue(OffscreenContent.hasNothingAhead(scroll, text, /* forward= */ true, /* rtl= */ false))
  }

  @Test
  fun pivotIsTheScrollView_scrollsAsBefore() {
    val scroll = scrollView().add(longText())
    assertFalse(OffscreenContent.hasNothingAhead(scroll, scroll, /* forward= */ true, /* rtl= */ false))
  }

  @Test
  fun exactVerticalLimit_requiresAValidRangeAndTheRequestedDirection() {
    assertTrue(FocusProcessorForLogicalNavigation.isVerticalScrollLimit(100, 100, true))
    assertTrue(FocusProcessorForLogicalNavigation.isVerticalScrollLimit(0, 100, false))
    assertFalse(FocusProcessorForLogicalNavigation.isVerticalScrollLimit(99, 100, true))
    assertFalse(FocusProcessorForLogicalNavigation.isVerticalScrollLimit(1, 100, false))
    assertFalse(FocusProcessorForLogicalNavigation.isVerticalScrollLimit(0, 0, false))
    assertFalse(FocusProcessorForLogicalNavigation.isVerticalScrollLimit(-1, -1, true))
    assertFalse(FocusProcessorForLogicalNavigation.isVerticalScrollLimit(100, 100, false))
  }

  @Test
  fun exactVerticalLimit_mustLoseBothActionsForThatDirection() {
    val bottom = scrollView().apply { addAction(AccessibilityActionCompat.ACTION_SCROLL_UP) }
    assertTrue(FocusProcessorForLogicalNavigation.confirmsVerticalScrollLimit(bottom, true))
    assertFalse(FocusProcessorForLogicalNavigation.confirmsVerticalScrollLimit(bottom, false))
    bottom.addAction(AccessibilityActionCompat.ACTION_SCROLL_DOWN)
    assertFalse(FocusProcessorForLogicalNavigation.confirmsVerticalScrollLimit(bottom, true))

    val top = scrollView().apply { addAction(AccessibilityActionCompat.ACTION_SCROLL_FORWARD) }
    assertTrue(FocusProcessorForLogicalNavigation.confirmsVerticalScrollLimit(top, false))
    top.addAction(AccessibilityActionCompat.ACTION_SCROLL_BACKWARD)
    assertFalse(FocusProcessorForLogicalNavigation.confirmsVerticalScrollLimit(top, false))

    assertFalse(FocusProcessorForLogicalNavigation.confirmsVerticalScrollLimit(scrollView(), true))
  }

  @Test
  fun exactVerticalLimit_isOnlyUsedForPlainVerticalScrollViews() {
    assertTrue(FocusProcessorForLogicalNavigation.isPlainVerticalScrollView(scrollView()))
    val indexed = scrollView().apply {
      addAction(AccessibilityActionCompat.ACTION_SCROLL_TO_POSITION)
    }
    assertFalse(FocusProcessorForLogicalNavigation.isPlainVerticalScrollView(indexed))
    assertFalse(FocusProcessorForLogicalNavigation.isPlainVerticalScrollView(
      Node("android.widget.HorizontalScrollView")
    ))
    assertFalse(FocusProcessorForLogicalNavigation.isPlainVerticalScrollView(
      scrollView().apply { packageName = "com.android.systemui" }
    ))
  }

  @Test
  fun pivotOutsideTheScrollView_scrollsAsBefore() {
    val scroll = scrollView().add(longText())
    assertFalse(OffscreenContent.hasNothingAhead(scroll, longText(), /* forward= */ true, /* rtl= */ false))
  }
}
