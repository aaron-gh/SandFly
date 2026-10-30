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

package com.google.android.accessibility.talkback.screenelements

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScreenElementsTest {
  private val toolbar = Rect(0, 30, 1080, 160)
  private val back = Rect(0, 30, 120, 160)
  private val page = Rect(0, 160, 1080, 2300)
  private val title = Rect(40, 200, 1040, 300)

  @Test
  fun anElementBelongsToTheSmallestNodeHoldingIt() {
    val nodes = listOf(toolbar, back, page, title)
    assertEquals(3, ScreenElements.anchorIndex(Rect(100, 220, 300, 280), nodes))
    assertEquals(2, ScreenElements.anchorIndex(Rect(100, 1000, 500, 1100), nodes))
  }

  @Test
  fun anElementOutsideEveryNodeFollowsTheLastNodeBeforeIt() {
    val left = Rect(0, 500, 300, 600)
    val below = Rect(0, 800, 1080, 900)
    // In the row of the left node, to its right.
    assertEquals(0, ScreenElements.anchorIndex(Rect(500, 520, 700, 580), listOf(left, below)))
    // Below both, after the lower one, whatever their order in the list.
    assertEquals(0, ScreenElements.anchorIndex(Rect(0, 1000, 200, 1100), listOf(below, left)))
  }

  @Test
  fun anElementAboveEveryNodeHasNoAnchor() {
    assertEquals(-1, ScreenElements.anchorIndex(Rect(0, 0, 100, 20), listOf(page)))
  }

  @Test
  fun elementsAreReadInRowsFromTopToBottomAndLeftToRight() {
    fun element(label: String, bounds: Rect) = ScreenElement(label, ScreenElement.Role.BUTTON, bounds)
    val title = element("Title", Rect(300, 550, 800, 650))
    val help = element("Help", Rect(930, 270, 1020, 360))
    val coins = element("Coins", Rect(400, 740, 680, 800))
    val left = element("Left", Rect(100, 1000, 400, 1100))
    val right = element("Right", Rect(500, 1010, 900, 1090))
    assertEquals(
      listOf("Help", "Title", "Coins", "Left", "Right"),
      ScreenElements.inReadingOrder(listOf(right, title, coins, help, left)).map { it.label },
    )
  }

  @Test
  fun elementsWithNoClearRowsAreStillSorted() {
    // A tall element with items to its upper right and lower left, many times over: pairwise row
    // comparison isn't transitive here.
    val elements =
      (0 until 40).flatMap { i ->
        val y = i * 10
        listOf(
          ScreenElement("A$i", ScreenElement.Role.BUTTON, Rect(200, y, 260, y + 50)),
          ScreenElement("B$i", ScreenElement.Role.BUTTON, Rect(100, y, 160, y + 400)),
          ScreenElement("C$i", ScreenElement.Role.BUTTON, Rect(0, y + 300, 60, y + 350)),
        )
      }
    assertEquals(elements.size, ScreenElements.inReadingOrder(elements.shuffled(java.util.Random(1))).size)
  }
}
