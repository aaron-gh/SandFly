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

package com.google.android.accessibility.talkback.imagecaption

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The bounded search for overlapping views, and the bitmap work, of screenshot preparation. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScreenshotPreparationTest {

  private class Node(
    val bounds: Rect,
    val focusable: Boolean = false,
    val children: MutableList<Node> = mutableListOf(),
  )

  /** Reads [Node]s and counts the children read. */
  private class Tree : ScreenshotPreparation.NodeTree<Node> {
    var childrenRead = 0

    override fun getChildCount(node: Node) = node.children.size

    override fun getChild(node: Node, index: Int): Node {
      childrenRead++
      return node.children[index]
    }

    override fun getBoundsInScreen(node: Node, outBounds: Rect) {
      outBounds.set(node.bounds)
    }

    override fun isFocusable(node: Node) = node.focusable
  }

  private fun find(
    root: Node,
    target: Rect,
    tree: Tree = Tree(),
    excluded: Set<Node> = emptySet(),
    maxVisitedNodes: Int = ScreenshotPreparation.MAX_VISITED_NODES,
    maxOverlaps: Int = ScreenshotPreparation.MAX_OVERLAPS,
    stop: () -> Boolean = { false },
  ): Pair<Boolean, List<Rect>> {
    val overlaps = mutableListOf<Rect>()
    val complete =
      ScreenshotPreparation.findOverlaps(
        root,
        target,
        excluded,
        tree,
        maxVisitedNodes,
        maxOverlaps,
        stop,
        overlaps,
      )
    return complete to overlaps
  }

  @Test
  fun aFocusableViewInsideTheTarget_isAnOverlap() {
    val button = Node(Rect(10, 10, 40, 40), focusable = true)
    val target = Node(Rect(0, 0, 100, 100), focusable = true, children = mutableListOf(button))
    val root = Node(Rect(0, 0, 1000, 2000), children = mutableListOf(target))

    val (complete, overlaps) = find(root, target.bounds, excluded = setOf(target))

    assertTrue(complete)
    assertEquals(listOf(Rect(10, 10, 40, 40)), overlaps)
  }

  @Test
  fun aViewThatCoversNearlyAllOfTheTarget_isNotAnOverlap() {
    val cover = Node(Rect(0, 0, 95, 95), focusable = true)
    val unfocusable = Node(Rect(10, 10, 40, 40))
    val target = Node(Rect(0, 0, 100, 100), children = mutableListOf(cover, unfocusable))

    val (_, overlaps) = find(target, target.bounds)

    assertTrue(overlaps.isEmpty())
  }

  @Test
  fun anExcludedAncestor_isNotAnOverlap() {
    val target = Node(Rect(20, 20, 40, 40))
    val ancestor = Node(Rect(20, 20, 30, 30), focusable = true, children = mutableListOf(target))

    val (_, overlaps) = find(ancestor, target.bounds, excluded = setOf(ancestor, target))

    assertTrue(overlaps.isEmpty())
  }

  @Test
  fun branchesOutsideTheTarget_areNotRead() {
    // A long web page: 500 rows of 10 items each, and the target is in the first row.
    val rows =
      (0 until 500).map { row ->
        val top = row * 100
        Node(
          Rect(0, top, 1000, top + 100),
          children =
            (0 until 10)
              .map { column -> Node(Rect(column * 100, top, column * 100 + 100, top + 100)) }
              .toMutableList(),
        )
      }
    val page = Node(Rect(0, 0, 1000, 50_000), children = rows.toMutableList())
    val tree = Tree()

    val (complete, _) = find(page, Rect(0, 0, 100, 100), tree)

    assertTrue(complete)
    // The rows, then the items of the first row only.
    assertEquals(500 + 10, tree.childrenRead)
  }

  @Test
  fun aNodeWithoutBounds_isSearched() {
    val button = Node(Rect(10, 10, 40, 40), focusable = true)
    val group = Node(Rect(), children = mutableListOf(button))
    val root = Node(Rect(0, 0, 1000, 2000), children = mutableListOf(group))

    val (_, overlaps) = find(root, Rect(0, 0, 100, 100))

    assertEquals(listOf(Rect(10, 10, 40, 40)), overlaps)
  }

  @Test
  fun theChildrenOfAnOverlap_areNotRead() {
    val inner = Node(Rect(12, 12, 20, 20), focusable = true)
    val card = Node(Rect(10, 10, 40, 40), focusable = true, children = mutableListOf(inner))
    val root = Node(Rect(0, 0, 100, 100), children = mutableListOf(card))
    val tree = Tree()

    val (_, overlaps) = find(root, root.bounds, tree, excluded = setOf(root))

    assertEquals(listOf(Rect(10, 10, 40, 40)), overlaps)
    assertEquals(1, tree.childrenRead)
  }

  @Test
  fun theSearchStopsAfterTheMostNodes() {
    val root = Node(Rect(0, 0, 1000, 1000))
    repeat(5000) { root.children += Node(Rect(0, 0, 1000, 1000)) }
    val tree = Tree()

    val (complete, _) = find(root, Rect(0, 0, 100, 100), tree, maxVisitedNodes = 100)

    assertFalse(complete)
    // The root's children are listed when it is visited; 99 of them are visited after it.
    assertEquals(5000, tree.childrenRead)
  }

  @Test
  fun theSearchStopsAfterTheMostOverlaps() {
    val root = Node(Rect(0, 0, 1000, 1000))
    repeat(50) { i -> root.children += Node(Rect(i, i, i + 10, i + 10), focusable = true) }

    val (complete, overlaps) = find(root, root.bounds, excluded = setOf(root), maxOverlaps = 5)

    assertFalse(complete)
    assertEquals(5, overlaps.size)
  }

  @Test
  fun theSearchStopsWhenAsked() {
    val root = Node(Rect(0, 0, 1000, 1000))
    repeat(50) { i -> root.children += Node(Rect(i, i, i + 10, i + 10), focusable = true) }
    var checks = 0

    val (complete, overlaps) =
      find(root, root.bounds, excluded = setOf(root), stop = { ++checks > 3 })

    assertFalse(complete)
    assertEquals(2, overlaps.size)
  }

  @Test
  fun overlapsOfAnAccessibilityNode_areFoundFromItsRoot() {
    val root = AccessibilityNodeInfo.obtain()
    root.setBoundsInScreen(Rect(0, 0, 1000, 2000))
    val target = AccessibilityNodeInfo.obtain()
    target.setBoundsInScreen(Rect(0, 0, 500, 500))
    shadowOf(root).addChild(target)
    val button = AccessibilityNodeInfo.obtain()
    button.setBoundsInScreen(Rect(100, 100, 200, 200))
    button.isFocusable = true
    shadowOf(target).addChild(button)
    val elsewhere = AccessibilityNodeInfo.obtain()
    elsewhere.setBoundsInScreen(Rect(0, 600, 100, 700))
    elsewhere.isFocusable = true
    shadowOf(root).addChild(elsewhere)
    val overlaps = mutableListOf<Rect>()

    val complete =
      ScreenshotPreparation.findOverlapsOfNode(
        AccessibilityNodeInfoCompat.wrap(target),
        Rect(0, 0, 500, 500),
        { false },
        overlaps,
      )

    assertTrue(complete)
    assertEquals(listOf(Rect(100, 100, 200, 200)), overlaps)
  }

  @Test
  @GraphicsMode(GraphicsMode.Mode.NATIVE)
  fun blackOut_blacksOutTheBlocksInACopy() {
    val screenshot = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    screenshot.eraseColor(Color.WHITE)

    val blocked = ScreenshotPreparation.blackOut(screenshot, listOf(Rect(10, 10, 20, 20)))!!

    assertEquals(Color.BLACK, blocked.getPixel(15, 15))
    assertEquals(Color.WHITE, blocked.getPixel(25, 25))
    assertEquals(Color.WHITE, screenshot.getPixel(15, 15))
  }

  @Test
  fun blackOut_withNothingInTheImage_makesNoCopy() {
    val screenshot = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

    assertNull(ScreenshotPreparation.blackOut(screenshot, emptyList()))
    assertNull(ScreenshotPreparation.blackOut(screenshot, listOf(Rect(200, 200, 300, 300))))
  }

  @Test
  fun crop_withoutBounds_copiesTheImage() {
    val image = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

    val cropped = ScreenshotPreparation.crop(image, null)

    assertFalse(cropped === image)
    assertEquals(100, cropped.width)
  }

  @Test
  fun crop_cutsTheBounds() {
    val image = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

    val cropped = ScreenshotPreparation.crop(image, Rect(10, 20, 40, 60))

    assertEquals(30, cropped.width)
    assertEquals(40, cropped.height)
  }
}
