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
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.Filter
import com.google.android.accessibility.utils.traversal.OrderedTraversalStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * TikTok's video feed, from a reporter's screen tree: a two-finger swipe turns the ViewPager to
 * the next video, and focus went to the page's first item, an unlabelled frame read as
 * "Unlabelled", or to the pager itself, read as "Page", on every video.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PagerTurnFocusTest {
  private var nextId = 1L

  /** A node whose parent and children are set by the test. */
  private inner class Node(
    className: String,
    bounds: Rect,
    visible: Boolean = true,
    vararg actions: AccessibilityAction,
  ) : AccessibilityNodeInfoCompat(info(className, bounds, visible, actions)) {
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

  private fun info(
    className: String,
    bounds: Rect,
    visible: Boolean,
    actions: Array<out AccessibilityAction>,
  ) =
    AccessibilityNodeInfo.obtain().apply {
      ReflectionHelpers.setField(this, "mSourceNodeId", nextId++)
      this.className = className
      isVisibleToUser = visible
      isEnabled = true
      setBoundsInScreen(bounds)
      actions.forEach { addAction(it) }
      isScrollable = actions.any { it.id == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD }
    }

  private val page = Rect(0, 0, 720, 1500)

  private fun clickable(className: String, bounds: Rect, visible: Boolean = true) =
    Node(className, bounds, visible, AccessibilityAction.ACTION_CLICK).apply {
      isFocusable = true
      isClickable = true
    }

  private fun text(text: String, bounds: Rect, visible: Boolean = true) =
    Node("android.widget.TextView", bounds, visible).apply { this.text = text }

  private fun pager() =
    Node(
        "androidx.viewpager.widget.ViewPager",
        page,
        true,
        AccessibilityAction.ACTION_SCROLL_FORWARD,
        AccessibilityAction.ACTION_SCROLL_BACKWARD,
      )
      .apply { isFocusable = true }

  private fun find(pager: Node): AccessibilityNodeInfoCompat? {
    val strategy = OrderedTraversalStrategy(pager)
    val cache = strategy.speakingNodesCache
    val filter = Filter.node { AccessibilityNodeInfoUtils.shouldFocusNode(it, cache) }
    return FocusFeedbackMapper.firstSpokenItemOfPage(pager, strategy, filter, cache)
  }

  @Test
  fun videoPage_skipsTheUnlabelledFrameAndThePager_forTheFirstItemThatSpeaks() {
    val unlabelledFrame = clickable("android.widget.FrameLayout", Rect(0, 900, 720, 1000))
    val sticker =
      clickable("android.widget.FrameLayout", Rect(100, 600, 600, 700))
        .add(text("maafkan Aku engkau", Rect(120, 620, 580, 680)))
    val pager = pager()
    pager.add(
      // The previous page, a LIVE card, out of view.
      clickable("android.view.View", Rect(0, -1500, 720, 0), visible = false).apply {
        contentDescription = "LIVE"
      },
      unlabelledFrame,
      sticker,
      clickable("android.widget.Button", Rect(620, 700, 700, 780)).apply {
        contentDescription = "Sukai video. 709 suka"
      },
    )

    assertEquals(sticker, find(pager))
  }

  @Test
  fun pageWithNothingToSpeak_focusesNothing() {
    val pager = pager()
    pager.add(clickable("android.widget.FrameLayout", Rect(0, 900, 720, 1000)))

    assertNull(find(pager))
  }
}
