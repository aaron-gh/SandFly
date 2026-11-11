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
    assertTrue(FocusProcessorForLogicalNavigation.reachesListBottom(Rect(0, 900, 1000, 2000), list))
    assertFalse(
      FocusProcessorForLogicalNavigation.reachesListBottom(Rect(0, 900, 1000, 1990), list)
    )
  }

  @Test
  fun emptyBoundsDoNotReachListBottom() {
    val list = Rect(0, 200, 1000, 2000)
    assertFalse(FocusProcessorForLogicalNavigation.reachesListBottom(Rect(), list))
    assertFalse(
      FocusProcessorForLogicalNavigation.reachesListBottom(Rect(0, 900, 1000, 2000), Rect())
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
    assertTrue(FocusProcessorForLogicalNavigation.staysAfterPage(Rect(0, 361, 1080, 2520), list, 2200, false, 130))
    // A page as tall as the list leaves nothing.
    assertFalse(FocusProcessorForLogicalNavigation.staysAfterPage(Rect(0, 361, 1080, 2520), list, 2500, true, 130))
    // An item ending high up leaves with the page.
    assertFalse(FocusProcessorForLogicalNavigation.staysAfterPage(Rect(0, 1800, 1080, 2300), list, 2200, false, 130))
  }

  @Test
  fun itemStaysInViewListsWithoutAMeasuredPage() {
    val list = Rect(0, 0, 1080, 2520)
    assertTrue(FocusProcessorForLogicalNavigation.staysAfterPage(Rect(0, 361, 1080, 2520), list, 0, true, 130))
    assertFalse(FocusProcessorForLogicalNavigation.staysAfterPage(Rect(0, 361, 1080, 2400), list, 0, true, 130))
    assertFalse(FocusProcessorForLogicalNavigation.staysAfterPage(Rect(0, 361, 1080, 2520), list, 0, false, 130))
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
  fun itemWhollyInsideTheListNeedsNoScroll() {
    val list = Rect(0, 0, 1080, 2520)
    assertTrue(FocusProcessorForLogicalNavigation.isInside(Rect(0, 900, 1080, 1400), list))
    assertFalse(FocusProcessorForLogicalNavigation.isInside(Rect(0, 2000, 1080, 2520), list))
    assertFalse(FocusProcessorForLogicalNavigation.isInside(Rect(0, 0, 1080, 400), list))
    assertFalse(FocusProcessorForLogicalNavigation.isInside(Rect(), list))
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
