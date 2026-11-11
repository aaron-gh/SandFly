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

package com.google.android.accessibility.talkback.gesture

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Checks where SandFly drags a list to reach the next item, and which way. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DragScrollTest {

  private val forward = AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD
  private val backward = AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD
  private val recyclerView = "androidx.recyclerview.widget.RecyclerView"

  @Test
  fun viewListsScrollForwardByMovingContentUp() {
    assertEquals(true, DragScroll.contentMovesUp(forward, recyclerView, null))
    assertEquals(false, DragScroll.contentMovesUp(backward, recyclerView, null))
    assertEquals(true, DragScroll.contentMovesUp(forward, "android.widget.ListView", null))
  }

  @Test
  fun unknownListsNeedALearnedDirection() {
    // Compose reports its lists as ScrollView, and a reversed one scrolls forward the other way.
    assertNull(DragScroll.contentMovesUp(forward, "android.widget.ScrollView", null))
    assertNull(DragScroll.contentMovesUp(backward, "android.view.View", null))
    assertNull(DragScroll.contentMovesUp(forward, null, null))
    assertEquals(false, DragScroll.contentMovesUp(forward, "android.widget.ScrollView", false))
    assertEquals(true, DragScroll.contentMovesUp(backward, "android.widget.ScrollView", false))
  }

  @Test
  fun learnedDirectionWinsOverTheKindOfList() {
    assertEquals(false, DragScroll.contentMovesUp(forward, recyclerView, false))
  }

  @Test
  fun scrollDownAndUpSayTheDirection() {
    assertEquals(true, DragScroll.contentMovesUp(AccessibilityAction.ACTION_SCROLL_DOWN.id, null, null))
    assertEquals(false, DragScroll.contentMovesUp(AccessibilityAction.ACTION_SCROLL_UP.id, null, false))
    assertNull(DragScroll.contentMovesUp(AccessibilityAction.ACTION_SCROLL_LEFT.id, recyclerView, true))
  }

  @Test
  fun dragsUpTheUncoveredListToMoveContentUp() {
    val path = path(Rect(0, 300, 1000, 2100), blockers = emptyList(), contentUp = true)!!
    assertTrue(path.movesContentUp)
    assertEquals(500f, path.x, 0.5f)
    // 8 % of the list's 1800 px is kept from each end.
    assertEquals(2100 - 144f, path.startY, 0.5f)
    assertEquals(300 + 144f, path.endY, 0.5f)
  }

  @Test
  fun dragsDownToMoveContentDown() {
    val path = path(Rect(0, 300, 1000, 2100), blockers = emptyList(), contentUp = false)!!
    assertFalse(path.movesContentUp)
    assertTrue(path.startY < path.endY)
  }

  @Test
  fun keepsClearOfTheScreensEdges() {
    val path = path(Rect(0, 0, 1000, 2400), blockers = emptyList(), contentUp = true)!!
    // The list runs under the status bar and the navigation bar.
    assertTrue(path.endY >= 100f)
    assertTrue(path.startY <= 2400 - 150f)
    assertTrue(path.x in 100f..900f)
  }

  @Test
  fun stopsShortOfWhatCoversTheList() {
    val list = Rect(0, 200, 1000, 2200)
    val toolbar = Rect(0, 200, 1000, 400)
    val messageField = Rect(100, 2000, 800, 2200)
    val path = path(list, blockers = listOf(toolbar, messageField), contentUp = true)!!
    assertTrue(path.startY < 2000f)
    assertTrue(path.endY > 400f)
  }

  @Test
  fun ignoresWhatIsBesideTheList() {
    val list = Rect(0, 200, 500, 2200)
    val otherPane = Rect(500, 200, 1000, 2200)
    val path = path(list, blockers = listOf(otherPane), contentUp = true)
    assertNotNull(path)
  }

  @Test
  fun noDragWhenTooLittleIsUncovered() {
    val list = Rect(0, 200, 1000, 2200)
    val keyboard = Rect(0, 400, 1000, 2400)
    assertNull(path(list, blockers = listOf(keyboard), contentUp = true))
    assertNull(path(Rect(0, 0, 0, 0), blockers = emptyList(), contentUp = true))
  }

  @Test
  fun goesAroundTheFocusedItemsControls() {
    val list = Rect(0, 200, 1000, 2200)
    val button = Rect(400, 1000, 600, 1100)
    val path = path(list, blockers = emptyList(), avoid = listOf(button), contentUp = true)!!
    assertTrue(path.x < 400f || path.x >= 600f)
  }

  @Test
  fun takesTheMiddleWhenControlsAreEverywhere() {
    val list = Rect(0, 200, 1000, 2200)
    val wide = Rect(0, 1000, 1000, 1100)
    val path = path(list, blockers = emptyList(), avoid = listOf(wide), contentUp = true)!!
    assertEquals(500f, path.x, 0.5f)
  }

  @Test
  fun fullWidthItemsMeanAVerticalList() {
    val list = Rect(0, 200, 1000, 2200)
    assertTrue(DragScroll.isFullWidth(Rect(0, 200, 1000, 3000), list))
    assertTrue(DragScroll.isFullWidth(Rect(50, 900, 950, 1000), list))
    assertFalse(DragScroll.isFullWidth(Rect(0, 200, 400, 2200), list))
    assertFalse(DragScroll.isFullWidth(Rect(0, 200, 1000, 2200), Rect()))
  }

  private fun path(
    list: Rect,
    blockers: List<Rect>,
    contentUp: Boolean,
    avoid: List<Rect> = emptyList(),
  ): DragScroll.DragPath? =
    DragScroll.dragPath(
      list,
      screenWidth = 1000,
      screenHeight = 2400,
      blockers = blockers,
      avoid = avoid,
      contentUp = contentUp,
      sideMarginPx = 100,
      topMarginPx = 100,
      bottomMarginPx = 150,
      minDistancePx = 250,
    )
}
