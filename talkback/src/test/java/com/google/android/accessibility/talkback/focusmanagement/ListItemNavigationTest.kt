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
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionInfoCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * Moving by list item goes from item to item of the list that holds focus, passing over the
 * controls inside items, such as the Share button of a post in a feed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ListItemNavigationTest {
  private var nextId = 1L

  /** A node of a made-up window. */
  private inner class Node(
    val name: String,
    top: Int,
    text: String? = null,
    clickable: Boolean = false,
    className: String? = null,
  ) : AccessibilityNodeInfoCompat(info(top, text, clickable, className)) {
    val kids = ArrayList<Node>()
    var up: Node? = null

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

    override fun toString() = name
  }

  private fun info(
    top: Int,
    text: String?,
    clickable: Boolean,
    className: String?,
  ): AccessibilityNodeInfo {
    val info = AccessibilityNodeInfo.obtain()
    ReflectionHelpers.setField(info, "mSourceNodeId", nextId++)
    info.setBoundsInScreen(Rect(0, top, 1000, top + 90))
    info.isVisibleToUser = true
    info.isClickable = clickable
    info.isFocusable = clickable
    info.text = text
    info.className =
      className ?: if (text != null) "android.widget.TextView" else "android.view.ViewGroup"
    return info
  }

  /** A post that can be focused, with a Share button and a row of pictures in it. */
  private fun post(name: String, top: Int): Node {
    val pictures =
      Node("$name pictures", top + 30).apply {
        setCollectionInfo(CollectionInfoCompat.obtain(1, 2, false, 0))
        add(
          Node("$name picture 1", top + 30, text = "Picture", clickable = true),
          Node("$name picture 2", top + 30, text = "Picture", clickable = true),
        )
      }
    return Node(name, top, text = "Post", clickable = true)
      .add(pictures, Node("$name share", top + 60, text = "Share", clickable = true))
  }

  private val first = post("first", 100)
  private val second = post("second", 200)
  private val third = post("third", 300)
  private val feed =
    Node("feed", 100).apply {
      setCollectionInfo(CollectionInfoCompat.obtain(3, 1, false, 0))
      add(first, second, third)
    }
  private val title = Node("title", 0, text = "Channel")

  init {
    Node("window", 0).add(title, feed)
  }

  @Test
  fun aListHoldsItsItemsAndTheirParts() {
    assertEquals(feed, ListItemNavigation.listOf(second))
    assertEquals(feed, ListItemNavigation.listOf(second.kids[1]))
    assertNull(ListItemNavigation.listOf(title))
  }

  @Test
  fun nextItemPassesOverTheControlsInsideTheItem() {
    assertEquals(second, ListItemNavigation.target(first, feed, forward = true))
    assertEquals(third, ListItemNavigation.target(second, feed, forward = true))
  }

  @Test
  fun fromAControlInsideAnItemNextAndPreviousGoToTheItemsAround() {
    val share = second.kids[1]
    assertEquals(third, ListItemNavigation.target(share, feed, forward = true))
    assertEquals(first, ListItemNavigation.target(share, feed, forward = false))
  }

  @Test
  fun previousItemIsTheItemItselfNotItsLastControl() {
    assertEquals(second, ListItemNavigation.target(third, feed, forward = false))
  }

  @Test
  fun inANestedListTheNearestListIsUsed() {
    val pictures = second.kids[0] as Node
    assertEquals(pictures, ListItemNavigation.listOf(pictures.kids[0]))
    assertEquals(
      pictures.kids[1],
      ListItemNavigation.target(pictures.kids[0], pictures, forward = true),
    )
  }

  @Test
  fun noItemPastTheEnds() {
    assertNull(ListItemNavigation.target(third, feed, forward = true))
    assertNull(ListItemNavigation.target(first.kids[1], feed, forward = false))
  }

  @Test
  fun anItemThatCannotBeFocusedIsEnteredAtItsFirstNode() {
    val label = Node("label", 500, text = "Wi-Fi", clickable = true)
    val toggle = Node("toggle", 500, text = "On", clickable = true)
    val row = Node("row", 500).add(label, toggle)
    val other = Node("other", 400, text = "Bluetooth", clickable = true)
    val last = Node("last", 600, text = "Location", clickable = true)
    val list = Node("list", 400, className = "android.widget.ListView").add(other, row, last)
    Node("window", 0).add(list)
    assertEquals(label, ListItemNavigation.target(other, list, forward = true))
    assertEquals(label, ListItemNavigation.target(last, list, forward = false))
    assertEquals(label, ListItemNavigation.itemTarget(toggle, list))
  }

  @Test
  fun aScrollViewIsEnteredThroughTheLayoutItWraps() {
    val one = Node("one", 0, text = "One", clickable = true)
    val two = Node("two", 100, text = "Two", clickable = true)
    val layout = Node("layout", 0).add(one, two)
    val scroll = Node("scroll", 0, className = "android.widget.ScrollView").add(layout)
    Node("window", 0).add(scroll)
    assertEquals(scroll, ListItemNavigation.listOf(one))
    assertEquals(two, ListItemNavigation.target(one, scroll, forward = true))
    assertEquals(one, ListItemNavigation.target(two, scroll, forward = false))
  }
}
