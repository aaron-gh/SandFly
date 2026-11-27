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
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Checks when a swipe jumps straight to the next row of a list. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JumpToNextRowTest {

  @Test
  fun recognizesOnlyTheMissingPartOfTheFocusedRow() {
    val list = Rect(46, 434, 1175, 2400)
    val full = Rect(0, 0, 1129, 239)
    val end = Rect(46, 2346, 1175, 2400)
    val start = Rect(46, 434, 1175, 488)
    assertEquals(185, FocusProcessorForLogicalNavigation.clippedFocusedRowDelta(list, end, full))
    assertEquals(-185, FocusProcessorForLogicalNavigation.clippedFocusedRowDelta(list, start, full))
    assertEquals(
      0,
      FocusProcessorForLogicalNavigation.clippedFocusedRowDelta(
        list,
        Rect(46, 2161, 1175, 2400),
        full,
      ),
    )
    assertEquals(
      0,
      FocusProcessorForLogicalNavigation.clippedFocusedRowDelta(
        list,
        Rect(46, 2346, 800, 2400),
        full,
      ),
    )
    assertEquals(
      0,
      FocusProcessorForLogicalNavigation.clippedFocusedRowDelta(
        list,
        end,
        Rect(0, 0, 1129, 1966),
      ),
    )
  }

  @Test
  fun anchorsThePreviousViewportUsingMeasuredRows() {
    val known = (9..17).associateWith { 239 }
    assertEquals(10, PreviousListRows.previousViewportAnchor(17, 1966, known))
    assertEquals(9, PreviousListRows.previousViewportAnchor(9, 1966, known))
  }

  @Test
  fun keepsUnknownAndOversizedRowsOutOfThePreviousViewport() {
    assertEquals(-1, PreviousListRows.previousViewportAnchor(17, 1966, mapOf(16 to 239)))
    assertEquals(-1, PreviousListRows.previousViewportAnchor(17, 1966, mapOf(17 to 1966)))
    assertEquals(16, PreviousListRows.previousViewportAnchor(17, 1966, mapOf(16 to 239, 17 to 239)))
    assertEquals(17, PreviousListRows.previousViewportAnchor(17, 1966, mapOf(16 to 2000, 17 to 239)))
  }


  @Test
  fun usesRowAfterSpan() {
    assertEquals(3, FocusProcessorForLogicalNavigation.rowAfter(2, 1, 10))
    assertEquals(5, FocusProcessorForLogicalNavigation.rowAfter(2, 3, 10))
    assertEquals(3, FocusProcessorForLogicalNavigation.rowAfter(2, 0, 10))
  }

  @Test
  fun noRowPastTheEnd() {
    assertEquals(-1, FocusProcessorForLogicalNavigation.rowAfter(9, 1, 10))
    assertEquals(-1, FocusProcessorForLogicalNavigation.rowAfter(8, 3, 10))
  }

  @Test
  fun noRowForIndexOutOfBounds() {
    assertEquals(-1, FocusProcessorForLogicalNavigation.rowAfter(-1, 1, 10))
    assertEquals(-1, FocusProcessorForLogicalNavigation.rowAfter(12, 1, 10))
  }

  @Test
  fun itemMustReachListBottom() {
    val list = Rect(0, 200, 1000, 2000)
    assertTrue(FocusProcessorForLogicalNavigation.reachesListBottom(Rect(0, 900, 1000, 2000), list, 0))
    assertFalse(
      FocusProcessorForLogicalNavigation.reachesListBottom(Rect(0, 900, 1000, 1990), list, 0)
    )
  }

  @Test
  fun itemEndingWithinSlackReachesListBottom() {
    // Nothing Phone (1) Developer options: the last row on screen ends 5 px above the list's bottom.
    val list = Rect(0, 556, 1080, 2400)
    assertTrue(
      FocusProcessorForLogicalNavigation.reachesListBottom(Rect(0, 2198, 1080, 2395), list, 32)
    )
    assertFalse(
      FocusProcessorForLogicalNavigation.reachesListBottom(Rect(0, 2000, 1080, 2300), list, 32)
    )
  }

  @Test
  fun emptyBoundsDoNotReachListBottom() {
    val list = Rect(0, 200, 1000, 2000)
    assertFalse(FocusProcessorForLogicalNavigation.reachesListBottom(Rect(), list, 32))
    assertFalse(
      FocusProcessorForLogicalNavigation.reachesListBottom(Rect(0, 900, 1000, 2000), Rect(), 32)
    )
  }

  @Test
  fun plainItemHasNoPartsTakingFocus() {
    val item = node()
    node(item)
    node(item)
    assertFalse(FocusProcessorForLogicalNavigation.hasPartsTakingFocus(item, item))
  }

  @Test
  fun clickableDescendantTakesFocus() {
    val item = node()
    val layout = node(item)
    node(layout).unwrap().isClickable = true
    assertTrue(FocusProcessorForLogicalNavigation.hasPartsTakingFocus(item, item))
  }

  @Test
  fun adjustableSliderTakesFocus() {
    val item = node()
    node(item).unwrap().className = "android.widget.SeekBar"
    assertTrue(FocusProcessorForLogicalNavigation.hasPartsTakingFocus(item, item))
  }

  @Test
  fun pivotInsideItemDoesNotCount() {
    val item = node()
    val pivot = node(item)
    pivot.unwrap().isFocusable = true
    assertFalse(FocusProcessorForLogicalNavigation.hasPartsTakingFocus(item, pivot))
  }

  @Test
  fun onlyTheGroupDescribesTheCompleteRow() {
    val item = textRow()
    assertEquals(item.getChild(0), FocusProcessorForLogicalNavigation.singleTextRowFocus(item))
  }

  @Test
  fun rowWithoutDescriptionKeepsPaging() {
    assertNull(FocusProcessorForLogicalNavigation.singleTextRowFocus(textRow(label = null)))
  }

  @Test
  fun anotherControlInTheRowKeepsPaging() {
    assertNull(FocusProcessorForLogicalNavigation.singleTextRowFocus(textRow(hasAnotherControl = true)))
  }

  @Test
  fun independentlyFocusableRowWrapperKeepsPaging() {
    val item = textRow()
    item.unwrap().isScreenReaderFocusable = true
    assertNull(FocusProcessorForLogicalNavigation.singleTextRowFocus(item))
  }

  private fun textRow(
    label: String? = "Full message",
    hasAnotherControl: Boolean = false,
  ): AccessibilityNodeInfoCompat {
    val item = node().apply { unwrap().className = "android.view.ViewGroup" }
    node(item).unwrap().apply {
      className = "android.view.ViewGroup"
      contentDescription = label
      isClickable = true
    }
    if (hasAnotherControl) {
      node(item).unwrap().isClickable = true
    }
    return item
  }

  @Test
  fun tooManyNodesCountAsPartsTakingFocus() {
    val item = node()
    repeat(101) { node(item) }
    assertTrue(FocusProcessorForLogicalNavigation.hasPartsTakingFocus(item, item))
  }

  @Test
  fun listWithoutScrollEventsWaitsBriefly() {
    assertTrue(FocusProcessorForLogicalNavigation.listStillMoving(1179, 1000, 0, 0, 0, 0))
    assertFalse(FocusProcessorForLogicalNavigation.listStillMoving(1180, 1000, 0, 0, 0, 0))
  }

  @Test
  fun listMovingAtSpeedKeepsWaiting() {
    // 20 px just after the scroll, then a full step: speeding up.
    assertTrue(FocusProcessorForLogicalNavigation.listStillMoving(1110, 1000, 2, 1100, 600, 20))
    // A step of at least half the one before.
    assertTrue(FocusProcessorForLogicalNavigation.listStillMoving(1210, 1000, 3, 1200, 300, 600))
    // One scroll event: nothing to compare yet.
    assertTrue(FocusProcessorForLogicalNavigation.listStillMoving(1110, 1000, 1, 1100, 5, 900))
  }

  @Test
  fun listSlowingDownCountsAsStopped() {
    assertFalse(FocusProcessorForLogicalNavigation.listStillMoving(1210, 1000, 3, 1200, 250, 600))
  }

  @Test
  fun listWithoutRecentScrollEventCountsAsStopped() {
    assertFalse(FocusProcessorForLogicalNavigation.listStillMoving(1250, 1000, 2, 1100, 600, 600))
  }

  @Test
  fun waitEndsAfterMaximum() {
    assertFalse(FocusProcessorForLogicalNavigation.listStillMoving(1270, 1000, 3, 1260, 600, 600))
  }

  @Test
  fun trackerIsTheSmallPartThatLeavesFirst() {
    val list = Rect(0, 200, 1000, 2200)
    val parts =
      listOf(
        Rect(0, 200, 1000, 2200), // as tall as the list
        Rect(125, 387, 1026, 472),
        Rect(125, 1500, 1026, 1600),
        Rect(0, 0, 0, 0), // empty
        Rect(125, 2400, 1026, 2500), // off screen
      )
    assertEquals(1, FocusProcessorForLogicalNavigation.pickTracker(list, parts, true))
    assertEquals(2, FocusProcessorForLogicalNavigation.pickTracker(list, parts, false))
    assertEquals(-1, FocusProcessorForLogicalNavigation.pickTracker(list, listOf(Rect(list)), true))
  }

  @Test
  fun trackerStepsFromCsrLogSlowDownOnlyAtTheEnd() {
    // Steps of a Nekogram message scrolled forward, every 30 ms.
    val steps = intArrayOf(542, 501, 301, 261, 165, 89, 56, 35)
    var moved = false
    var previous = 0
    for (step in steps) {
      assertFalse(FocusProcessorForLogicalNavigation.trackerSlowed(step, previous, moved))
      moved = true
      previous = step
    }
    assertTrue(FocusProcessorForLogicalNavigation.trackerSlowed(3, 35, true))
    assertTrue(FocusProcessorForLogicalNavigation.trackerSlowed(100, 542, true))
  }

  @Test
  fun trackerThatHasNotMovedHasNotSlowed() {
    assertFalse(FocusProcessorForLogicalNavigation.trackerSlowed(0, 0, false))
  }

  @Test
  fun rowAtTheListsBottomEndsItGoingForward() {
    val list = Rect(0, 200, 1080, 2520)
    assertTrue(FocusProcessorForLogicalNavigation.endsListOnScreen(Rect(0, 1932, 1080, 2520), list, true))
    assertFalse(FocusProcessorForLogicalNavigation.endsListOnScreen(Rect(0, 1932, 1080, 2400), list, true))
    assertFalse(FocusProcessorForLogicalNavigation.endsListOnScreen(Rect(0, 1932, 1080, 2520), list, false))
    assertTrue(FocusProcessorForLogicalNavigation.endsListOnScreen(Rect(0, 200, 1080, 900), list, false))
  }

  @Test
  fun narrowItemDoesntEndTheList() {
    val list = Rect(0, 200, 1080, 2520)
    // An item of a grid may have another beside it.
    assertFalse(FocusProcessorForLogicalNavigation.endsListOnScreen(Rect(0, 1932, 540, 2520), list, true))
    assertFalse(FocusProcessorForLogicalNavigation.endsListOnScreen(Rect(), list, true))
  }

  @Test
  fun itemStaysAfterAMeasuredPage() {
    val list = Rect(0, 0, 1080, 2520)
    // Cut off at the bottom: 2520 - 2200 = 320 px stay.
    assertTrue(FocusProcessorForLogicalNavigation.staysAfterPage(Rect(0, 361, 1080, 2520), list, 2200, 130, true))
    // A page as tall as the list leaves nothing.
    assertFalse(FocusProcessorForLogicalNavigation.staysAfterPage(Rect(0, 361, 1080, 2520), list, 2500, 130, true))
    // An item ending high up leaves with the page.
    assertFalse(FocusProcessorForLogicalNavigation.staysAfterPage(Rect(0, 1800, 1080, 2300), list, 2200, 130, true))
  }

  @Test
  fun itemStaysAfterAPageBack() {
    val list = Rect(0, 0, 1080, 2520)
    // Cut off at the top: its top moves down by the page and stays above the bottom.
    assertTrue(FocusProcessorForLogicalNavigation.staysAfterPage(Rect(0, 0, 1080, 2000), list, 2200, 130, false))
    assertFalse(FocusProcessorForLogicalNavigation.staysAfterPage(Rect(0, 400, 1080, 2000), list, 2200, 130, false))
  }

  @Test
  fun itemAtTheEdgeLeavesWithAPageNotMeasured() {
    // SandFly#72: a settings row ending at the bottom of a list 2112 px tall, whose page scrolled
    // 2120 px.
    val list = Rect(0, 364, 1220, 2476)
    assertFalse(FocusProcessorForLogicalNavigation.staysAfterPage(Rect(0, 2280, 1220, 2476), list, 0, 183, true))
    assertFalse(FocusProcessorForLogicalNavigation.staysAfterPage(Rect(0, 364, 1220, 560), list, 0, 183, false))
    // Reported past the edge by more than a page and the margin.
    assertTrue(FocusProcessorForLogicalNavigation.staysAfterPage(Rect(0, 1000, 1220, 2900), list, 0, 183, true))
    assertTrue(FocusProcessorForLogicalNavigation.staysAfterPage(Rect(0, -60, 1220, 1000), list, 0, 183, false))
  }

  @Test
  fun nextItemFollowsTheScrolledAwayItem() {
    // The row at 2280-2476 scrolled 2120 px up: the next row starts at 356.
    assertEquals(356, TraversalTreeCache.placeAfterScroll(2280, 2476, 2120, true))
    // A row at the top scrolled 2000 px down: the previous row ends at 2364.
    assertEquals(2364, TraversalTreeCache.placeAfterScroll(364, 560, -2000, false))
  }

  @Test
  fun scrolledAwayItemLeftTheList() {
    // FastSM's Compose list 2030 px tall: its scroll deltas added up to 2 for a page that moved the
    // item at 2198-2486 off the top, so the next item starts at the list's top.
    val list = Rect(0, 456, 1080, 2486)
    val shift = TraversalTreeCache.shiftOffList(2, Rect(0, 2198, 1080, 2486), list, true)
    assertEquals(2030, shift)
    assertEquals(456, TraversalTreeCache.placeAfterScroll(2198, 2486, shift, true))
    // Back: the item at the top went below the list's bottom.
    assertEquals(-2028, TraversalTreeCache.shiftOffList(-1, Rect(0, 458, 1080, 697), list, false))
    // Deltas in pixels already move the item off.
    val rows = Rect(0, 364, 1220, 2476)
    assertEquals(2120, TraversalTreeCache.shiftOffList(2120, Rect(0, 2280, 1220, 2476), rows, true))
    assertEquals(-2120, TraversalTreeCache.shiftOffList(-2120, Rect(0, 364, 1220, 560), rows, false))
  }

  @Test
  fun clippedTrackerTouchesTheListsEdge() {
    val list = Rect(0, 0, 1080, 2520)
    assertTrue(FocusProcessorForLogicalNavigation.touchesEdge(Rect(0, 0, 1080, 2000), list))
    assertTrue(FocusProcessorForLogicalNavigation.touchesEdge(Rect(0, 361, 1080, 2520), list))
    // A part reported where it is, off screen.
    assertFalse(FocusProcessorForLogicalNavigation.touchesEdge(Rect(125, -500, 1026, -400), list))
    assertFalse(FocusProcessorForLogicalNavigation.touchesEdge(Rect(125, 900, 1026, 1000), list))
  }

  @Test
  fun watchedListStopsOnlyAfterItsScrollEvents() {
    val moving = FocusProcessorForLogicalNavigation.LIST_MOVING
    val stopped = FocusProcessorForLogicalNavigation.LIST_STOPPED
    val notScrolled = FocusProcessorForLogicalNavigation.LIST_NOT_SCROLLED
    assertEquals(moving, FocusProcessorForLogicalNavigation.watchedListState(100, 0, 100))
    assertEquals(notScrolled, FocusProcessorForLogicalNavigation.watchedListState(180, 0, 180))
    assertEquals(moving, FocusProcessorForLogicalNavigation.watchedListState(300, 3, 60))
    assertEquals(stopped, FocusProcessorForLogicalNavigation.watchedListState(500, 5, 150))
    assertEquals(stopped, FocusProcessorForLogicalNavigation.watchedListState(1500, 9, 10))
  }

  @Test
  fun nextItemMeetsThePivotsItem() {
    val item = Rect(0, 1000, 1080, 1300)
    assertTrue(FocusProcessorForLogicalNavigation.meetsItem(Rect(0, 1300, 1080, 1600), item, true, 2000))
    // A gap of a whole row: another item may lie between.
    assertFalse(FocusProcessorForLogicalNavigation.meetsItem(Rect(0, 1600, 1080, 1900), item, true, 2000))
    assertTrue(FocusProcessorForLogicalNavigation.meetsItem(Rect(0, 700, 1080, 1000), item, false, 2000))
    assertFalse(FocusProcessorForLogicalNavigation.meetsItem(Rect(0, 400, 1080, 700), item, false, 2000))
  }

  @Test
  fun gridTargetAtOrPastTheSuggestedCellIsTheNextItem() {
    // File picker: suggested row 11 holds a section break; the next file is row 12.
    assertTrue(FocusProcessorForLogicalNavigation.isAtOrPastCell(12, 0, 11, 0, true))
    assertTrue(FocusProcessorForLogicalNavigation.isAtOrPastCell(11, 0, 11, 0, true))
    assertFalse(FocusProcessorForLogicalNavigation.isAtOrPastCell(10, 2, 11, 0, true))
    // Going back, the cell before the suggested one is past it.
    assertTrue(FocusProcessorForLogicalNavigation.isAtOrPastCell(9, 3, 10, 3, false))
    assertFalse(FocusProcessorForLogicalNavigation.isAtOrPastCell(11, 0, 10, 3, false))
  }

  @Test
  fun cardHoldsItsButton() {
    val remove = Rect(666, 684, 1053, 765)
    assertTrue(FocusProcessorForLogicalNavigation.holdsPivot(Rect(0, 538, 1080, 783), remove))
    assertFalse(FocusProcessorForLogicalNavigation.holdsPivot(Rect(remove), remove))
    assertFalse(FocusProcessorForLogicalNavigation.holdsPivot(Rect(0, 783, 1080, 1028), remove))
  }

  @Test
  fun otherWindowsScrollEventsAreLeftOutWhileAListIsFollowed() {
    assertTrue(FocusProcessorForLogicalNavigation.isOtherWindowScroll(7, 3, 1000, 1500))
    assertFalse(FocusProcessorForLogicalNavigation.isOtherWindowScroll(3, 3, 1000, 1500))
    // Long after the scroll, or with no list followed, every event counts.
    assertFalse(FocusProcessorForLogicalNavigation.isOtherWindowScroll(7, 3, 1000, 3000))
    assertFalse(FocusProcessorForLogicalNavigation.isOtherWindowScroll(7, -1, 1000, 1500))
  }

  @Test
  fun learnedPageNeedsTheSameListHeight() {
    assertTrue(FocusProcessorForLogicalNavigation.sameHeight(2000, 2000))
    assertTrue(FocusProcessorForLogicalNavigation.sameHeight(2000, 1950))
    // The keyboard came up or went away.
    assertFalse(FocusProcessorForLogicalNavigation.sameHeight(1100, 2000))
    assertFalse(FocusProcessorForLogicalNavigation.sameHeight(null, 2000))
  }

  @Test
  fun rememberedPlaceFollowsPixelScrolls() {
    assertEquals(500, TraversalTreeCache.placeAfterScrolls(2000, 1500))
    // Compose deltas of 1 per page aren't pixels.
    assertEquals(2000, TraversalTreeCache.placeAfterScrolls(2000, 16))
    assertEquals(Int.MIN_VALUE, TraversalTreeCache.placeAfterScrolls(Int.MIN_VALUE, 1500))
  }

  @Test
  fun itemLeftOnScreenByAPageWasDrawnAgain() {
    val list = Rect(0, 300, 1080, 2100)
    // Moved up 1300 px to 400..700: still in the list.
    assertTrue(ScrollAhead.stillOnScreenAfter(Rect(0, 1700, 1080, 2000), 1300, list))
    // Moved up 1500 px to 200..500: partly above the list, so it may have gone on further.
    assertFalse(ScrollAhead.stillOnScreenAfter(Rect(0, 1700, 1080, 2000), 1500, list))
    // Moved up 2000 px: gone above the list.
    assertFalse(ScrollAhead.stillOnScreenAfter(Rect(0, 1700, 1080, 2000), 2000, list))
    // Compose deltas tell nothing.
    assertFalse(ScrollAhead.stillOnScreenAfter(Rect(0, 1700, 1080, 2000), 1, list))
    assertFalse(ScrollAhead.stillOnScreenAfter(Rect(0, 1700, 1080, 2000), 1500, null))
  }

  @Test
  fun targetBeforeOrHoldingThePivotGoesBack() {
    // Facebook: the Remove button of a person, inside that person's card, after a page scroll.
    val remove = Rect(666, 684, 1053, 765)
    val card = Rect(0, 538, 1080, 783)
    val nextCard = Rect(0, 783, 1080, 1028)
    assertTrue(FocusProcessorForLogicalNavigation.goesBackFromPivot(card, remove, true))
    assertTrue(
      FocusProcessorForLogicalNavigation.goesBackFromPivot(Rect(0, 293, 1080, 538), remove, true)
    )
    assertFalse(FocusProcessorForLogicalNavigation.goesBackFromPivot(nextCard, remove, true))
    // The Add Friend button left of Remove on its row comes before it.
    assertTrue(
      FocusProcessorForLogicalNavigation.goesBackFromPivot(Rect(23, 684, 640, 765), remove, true)
    )
    // In a right-to-left layout the button right of the pivot comes before it.
    assertFalse(
      FocusProcessorForLogicalNavigation.goesBackFromPivot(
        Rect(23, 684, 640, 765), remove, true, true
      )
    )
    assertTrue(
      FocusProcessorForLogicalNavigation.goesBackFromPivot(
        Rect(1060, 684, 1080, 765), remove, true, true
      )
    )
    // Going up, the card before the button is the way on.
    assertFalse(FocusProcessorForLogicalNavigation.goesBackFromPivot(card, remove, false))
    assertTrue(FocusProcessorForLogicalNavigation.goesBackFromPivot(nextCard, remove, false))
    assertFalse(FocusProcessorForLogicalNavigation.goesBackFromPivot(Rect(), remove, true))
  }

  @Test
  fun moveFromTheLastRowGoesPastTheList() {
    assertTrue(FocusProcessorForLogicalNavigation.isPastLastRow(11, 11))
    assertTrue(FocusProcessorForLogicalNavigation.isPastLastRow(-1, 11))
    assertFalse(FocusProcessorForLogicalNavigation.isPastLastRow(10, 11))
    // Row count not known.
    assertFalse(FocusProcessorForLogicalNavigation.isPastLastRow(11, -1))
    assertFalse(FocusProcessorForLogicalNavigation.isPastLastRow(-1, 0))
  }

  @Test
  fun itemShownWholeOnlyWhenNotCutOff() {
    val list = Rect(0, 320, 1080, 2378)
    // CodeRelay's last message, 293 px, ending at the list's bottom.
    assertTrue(
      FocusProcessorForLogicalNavigation.isShownWhole(
        Rect(0, 0, 1080, 293), Rect(0, 2085, 1080, 2378), list
      )
    )
    // Taller than shown: cut off at the bottom.
    assertFalse(
      FocusProcessorForLogicalNavigation.isShownWhole(
        Rect(0, 0, 1080, 900), Rect(0, 2085, 1080, 2378), list
      )
    )
    assertFalse(
      FocusProcessorForLogicalNavigation.isShownWhole(
        Rect(0, 0, 1080, 293), Rect(0, 0, 0, 0), list
      )
    )
  }

  @Test
  fun oneEventListStopsAFrameAfterItsEvent() {
    val moving = FocusProcessorForLogicalNavigation.LIST_MOVING
    val stopped = FocusProcessorForLogicalNavigation.LIST_STOPPED
    val notScrolled = FocusProcessorForLogicalNavigation.LIST_NOT_SCROLLED
    assertEquals(moving, FocusProcessorForLogicalNavigation.watchedListState(30, 1, 10, true))
    assertEquals(stopped, FocusProcessorForLogicalNavigation.watchedListState(40, 1, 16, true))
    assertEquals(moving, FocusProcessorForLogicalNavigation.watchedListState(40, 1, 16, false))
    // No event yet: waits for it as any list does.
    assertEquals(moving, FocusProcessorForLogicalNavigation.watchedListState(100, 0, 100, true))
    assertEquals(notScrolled, FocusProcessorForLogicalNavigation.watchedListState(180, 0, 180, true))
    assertFalse(
      FocusProcessorForLogicalNavigation.listStillMoving(1040, 1000, 1, 1020, 1, 1, true)
    )
    assertTrue(
      FocusProcessorForLogicalNavigation.listStillMoving(1040, 1000, 1, 1020, 1, 1, false)
    )
    assertTrue(
      FocusProcessorForLogicalNavigation.listStillMoving(1030, 1000, 1, 1020, 1, 1, true)
    )
    assertTrue(FocusProcessorForLogicalNavigation.listStillMoving(1100, 1000, 0, 0, 0, 0, true))
  }

  @Test
  fun oneEventListNeedsTwoOneEventScrollsInARow() {
    assertEquals(1, FocusProcessorForLogicalNavigation.nextOneEventScrolls(0, true))
    assertEquals(2, FocusProcessorForLogicalNavigation.nextOneEventScrolls(1, true))
    assertEquals(2, FocusProcessorForLogicalNavigation.nextOneEventScrolls(2, true))
    assertEquals(0, FocusProcessorForLogicalNavigation.nextOneEventScrolls(2, false))
    assertEquals(0, FocusProcessorForLogicalNavigation.nextOneEventScrolls(1, false))
  }

  @Test
  fun oneEventScrollIsToldAfterTheQuietTime() {
    assertEquals(true, FocusProcessorForLogicalNavigation.oneEventScroll(1, 150))
    assertNull(FocusProcessorForLogicalNavigation.oneEventScroll(1, 16))
    assertNull(FocusProcessorForLogicalNavigation.oneEventScroll(0, 200))
    assertEquals(false, FocusProcessorForLogicalNavigation.oneEventScroll(2, 16))
  }

  @Test
  fun itemPastThePivotsItem() {
    val pivotItem = Rect(0, 200, 1080, 1500)
    assertTrue(FocusProcessorForLogicalNavigation.isPastItem(Rect(0, 1500, 1080, 2520), pivotItem, true))
    assertFalse(FocusProcessorForLogicalNavigation.isPastItem(Rect(0, 0, 1080, 200), pivotItem, true))
    assertTrue(FocusProcessorForLogicalNavigation.isPastItem(Rect(0, 0, 1080, 200), pivotItem, false))
    // The pivot's item has left the screen: every item on screen is past it.
    assertTrue(FocusProcessorForLogicalNavigation.isPastItem(Rect(0, 300, 1080, 600), null, true))
    assertFalse(FocusProcessorForLogicalNavigation.isPastItem(Rect(), null, true))
  }

  @Test
  fun pivotsItemLeavesRoomForTheNextItem() {
    val list = Rect(0, 0, 1080, 2520)
    // Going back, the item above appears once the pivot's item moves down from the top.
    assertFalse(FocusProcessorForLogicalNavigation.leavesRoomPast(Rect(0, 0, 1080, 2280), list, false))
    assertTrue(FocusProcessorForLogicalNavigation.leavesRoomPast(Rect(0, 400, 1080, 2520), list, false))
    assertFalse(FocusProcessorForLogicalNavigation.leavesRoomPast(Rect(0, 400, 1080, 2520), list, true))
    assertTrue(FocusProcessorForLogicalNavigation.leavesRoomPast(Rect(0, 0, 1080, 2000), list, true))
    assertTrue(FocusProcessorForLogicalNavigation.leavesRoomPast(null, list, true))
  }

  @Test
  fun cutOffItemIsShownAfterTheScroll() {
    val list = Rect(0, 200, 1080, 2520)
    assertTrue(FocusProcessorForLogicalNavigation.isCutOff(Rect(0, 2300, 1080, 2520), list))
    assertTrue(FocusProcessorForLogicalNavigation.isCutOff(Rect(0, 200, 1080, 400), list))
    assertFalse(FocusProcessorForLogicalNavigation.isCutOff(Rect(0, 600, 1080, 900), list))
    // An item as tall as the list can't be shown whole.
    assertFalse(FocusProcessorForLogicalNavigation.isCutOff(Rect(0, 200, 1080, 2520), list))
  }

  @Test
  fun onlyASingleQuietPageNoTallerThanTheListIsLearned() {
    assertTrue(FocusProcessorForLogicalNavigation.isReliablePage(2200, 2520, true, false))
    // Overlapping scrolls added up past the list's height.
    assertFalse(FocusProcessorForLogicalNavigation.isReliablePage(2884, 2520, true, false))
    assertFalse(FocusProcessorForLogicalNavigation.isReliablePage(2200, 2520, false, false))
    assertFalse(FocusProcessorForLogicalNavigation.isReliablePage(2200, 2520, true, true))
    assertFalse(FocusProcessorForLogicalNavigation.isReliablePage(0, 2520, true, false))
  }

  @Test
  fun swipeGoesOnInTheListOfTheScrolledAwayItem() {
    // FastSM's order: the window, the list, its items, then the title above the list.
    val list = Rect(0, 456, 1080, 2486)
    val bounds =
      listOf(
        Rect(0, 0, 1080, 2520),
        list,
        Rect(0, 456, 1080, 700),
        Rect(0, 700, 1080, 1900),
        Rect(0, 1900, 1080, 2486),
        Rect(36, 145, 185, 202),
      )
    val focusable = listOf(false, false, true, true, true, true)
    // Forward: the item at the list's top, not the title before it on screen.
    assertEquals(2, TraversalTreeCache.itemAfterScrolledAway(bounds, focusable, list, 456, true))
    // Back: the item at the list's bottom, though the title comes after it in the order.
    assertEquals(4, TraversalTreeCache.itemAfterScrolledAway(bounds, focusable, list, 2486, false))
    // A place between items: the next one starts below it, the previous one ends above it.
    assertEquals(3, TraversalTreeCache.itemAfterScrolledAway(bounds, focusable, list, 650, true))
    assertEquals(3, TraversalTreeCache.itemAfterScrolledAway(bounds, focusable, list, 2000, false))
    // No list known, or nothing of the list past the place.
    assertEquals(-1, TraversalTreeCache.itemAfterScrolledAway(bounds, focusable, Rect(), 456, true))
    assertEquals(-1, TraversalTreeCache.itemAfterScrolledAway(bounds, focusable, list, 2500, true))
  }

  @Test
  fun lastOfItemsEndingTogetherIsReachedFirstGoingBack() {
    val list = Rect(0, 0, 1080, 2000)
    val bounds = listOf(Rect(0, 1500, 1080, 2000), Rect(900, 1800, 1080, 2000))
    val focusable = listOf(true, true)
    assertEquals(1, TraversalTreeCache.itemAfterScrolledAway(bounds, focusable, list, 2000, false))
    assertEquals(0, TraversalTreeCache.itemAfterScrolledAway(bounds, focusable, list, 1500, true))
  }

  @Test
  fun pageShorterThanHalfTheListIsNotLearned() {
    // FastSM's Compose list: deltas of 1 and 2 for pages of a 2030 px list.
    assertFalse(FocusProcessorForLogicalNavigation.isReliablePage(1, 2030, true, false))
    assertFalse(FocusProcessorForLogicalNavigation.isReliablePage(2, 2030, true, false))
    assertFalse(FocusProcessorForLogicalNavigation.isReliablePage(1014, 2030, true, false))
    assertTrue(FocusProcessorForLogicalNavigation.isReliablePage(1015, 2030, true, false))
  }

  @Test
  fun itemClearOfTheListEdgesNeedsNoScroll() {
    val list = Rect(0, 0, 1080, 2520)
    assertTrue(FocusProcessorForLogicalNavigation.isClearOfEdges(Rect(0, 900, 1080, 1400), list))
    assertFalse(FocusProcessorForLogicalNavigation.isClearOfEdges(Rect(0, 2000, 1080, 2520), list))
    assertFalse(FocusProcessorForLogicalNavigation.isClearOfEdges(Rect(0, 0, 1080, 400), list))
    assertFalse(FocusProcessorForLogicalNavigation.isClearOfEdges(Rect(), list))
  }

  @Test
  fun lastWholeRowAboveAPartlyShownRowNeedsTheEdgeCheck() {
    // #76: Xiaomi Control Panel app list. The next row shows 24 px below the focused row, and
    // a show-on-screen scroll of the focused row reveals it.
    val list = Rect(0, 713, 1220, 2656)
    assertFalse(
      FocusProcessorForLogicalNavigation.isClearOfEdges(Rect(44, 2374, 1176, 2632), list)
    )
    // The row above it has a whole row's room below it.
    assertTrue(
      FocusProcessorForLogicalNavigation.isClearOfEdges(Rect(44, 2116, 1176, 2374), list)
    )
  }

  @Test
  fun cardBesideAPartlyShownCardNeedsTheEdgeCheck() {
    val carousel = Rect(0, 500, 1080, 900)
    // A card at the start of a horizontal list, the next card partly shown after it.
    assertFalse(
      FocusProcessorForLogicalNavigation.isClearOfEdges(Rect(40, 520, 860, 880), carousel)
    )
    // A tile with a whole tile's room on each side.
    assertTrue(
      FocusProcessorForLogicalNavigation.isClearOfEdges(Rect(400, 520, 680, 880), carousel)
    )
  }

  @Test
  fun scrollAheadTakesOverFromItsOwnPage() {
    // The list moves from a page scrolled ahead 100 ms ago, after the swipe's own scroll.
    assertTrue(ScrollAhead.isOwnPage(1900, 1000, 2000))
    // A swipe's scroll after the page: wait for it.
    assertFalse(ScrollAhead.isOwnPage(1000, 1900, 2000))
    assertFalse(ScrollAhead.isOwnPage(0, 0, 2000))
  }

  @Test
  fun scrollAheadWaitsForTheListToStop() {
    assertFalse(ScrollAhead.isQuiet(1149, 1000))
    assertTrue(ScrollAhead.isQuiet(1150, 1000))
  }

  @Test
  fun scrollViewItemsAreInItsOnlyChild() {
    // Nothing Phone (1) Settings home: a ScrollView whose one child lays out the rows.
    val scrollView = node()
    val layout = node(scrollView)
    val row = node(layout)
    node(layout)
    assertEquals(layout, FocusProcessorForLogicalNavigation.itemParentOf(scrollView, row))
  }

  @Test
  fun listWithManyChildrenHoldsItsItems() {
    val list = node()
    val row = node(list)
    node(list)
    assertEquals(list, FocusProcessorForLogicalNavigation.itemParentOf(list, row))
  }

  @Test
  fun onlyChildThatTakesFocusIsAnItem() {
    val scrollView = node()
    val row = node(scrollView)
    node(row)
    node(row)
    assertEquals(scrollView, FocusProcessorForLogicalNavigation.itemParentOf(scrollView, row))
    val other = node()
    val clickable = node(other)
    clickable.unwrap().isClickable = true
    node(clickable)
    node(clickable)
    assertEquals(other, FocusProcessorForLogicalNavigation.itemParentOf(other, node()))
  }

  private fun node(parent: AccessibilityNodeInfoCompat? = null): AccessibilityNodeInfoCompat {
    val raw = AccessibilityNodeInfo.obtain()
    // Distinct sources keep the nodes unequal.
    raw.setSource(View(RuntimeEnvironment.getApplication()))
    raw.className = "android.view.View"
    raw.isVisibleToUser = true
    raw.setBoundsInScreen(Rect(0, 0, 100, 100))
    val node = AccessibilityNodeInfoCompat.wrap(raw)
    if (parent != null) {
      shadowOf(parent.unwrap()).addChild(raw)
    }
    return node
  }
}
