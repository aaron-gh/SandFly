/*
 * Copyright 2026 SandFly contributors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0
 */
package com.google.android.accessibility.talkback.focusmanagement.record

import com.google.android.accessibility.talkback.focusmanagement.record.AccessibilityFocusActionHistory.ActivatedItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** List items activated in a window, kept for focus to return to them after Back. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ActivatedItemsAfterBackTest {
  private val history = AccessibilityFocusActionHistory(RuntimeEnvironment.getApplication())

  private fun item(label: String, window: Int = 1) =
    ActivatedItem(window, "android.widget.LinearLayout", "app:id/row", label, 100)

  private fun back(time: Long, window: Int = 1, windows: List<Int>? = null) =
    history.onBack(time, window, null, null, windows)

  @Test
  fun backOffersTheNewestItemsOfItsWindowFirst() {
    val a = item("A")
    val other = item("Other", window = 2)
    val b = item("B")
    history.onItemActivated(a)
    history.onItemActivated(other)
    history.onItemActivated(b)
    back(1000)
    assertEquals(listOf(b, a), history.getActivatedItemsAfterBack(1100))
  }

  @Test
  fun restoringAnItemDropsItAndTheNewerItemsOfItsWindow() {
    val a = item("A")
    val toggle = item("Toggle")
    val other = item("Other", window = 2)
    history.onItemActivated(a)
    history.onItemActivated(toggle)
    history.onItemActivated(other)
    back(1000)
    history.onActivatedItemRestored(a)
    assertFalse(history.isBackPending(1100))
    assertEquals(1, history.activatedItemCount)
    back(2000, window = 2)
    assertEquals(listOf(other), history.getActivatedItemsAfterBack(2100))
  }

  @Test
  fun backWithoutRestoreDropsTheNewestItemAfterTheLimit() {
    val a = item("A")
    val b = item("B")
    history.onItemActivated(a)
    history.onItemActivated(b)
    back(1000)
    assertTrue(history.isBackPending(1000 + AccessibilityFocusActionHistory.RESTORE_AFTER_BACK_MS))
    assertTrue(
      history.getActivatedItemsAfterBack(
        1001 + AccessibilityFocusActionHistory.RESTORE_AFTER_BACK_MS).isEmpty())
    back(5000)
    assertEquals(listOf(a), history.getActivatedItemsAfterBack(5100))
  }

  @Test
  fun keyAndActionOfOneBackCountOnce() {
    history.onItemActivated(item("A"))
    history.onItemActivated(item("B"))
    back(1000)
    back(1000 + AccessibilityFocusActionHistory.SAME_BACK_MS - 1)
    assertEquals(2, history.activatedItemCount)
    // A second Back before the list came back leaves the list below.
    back(1000 + AccessibilityFocusActionHistory.SAME_BACK_MS)
    assertEquals(listOf("A"), history.getActivatedItemsAfterBack(1400).map { it.label })
  }

  @Test
  fun userMoveEndsTheBack() {
    history.onItemActivated(item("A"))
    history.onItemActivated(item("B"))
    back(1000)
    history.endBack()
    assertFalse(history.isBackPending(1100))
    assertEquals(1, history.activatedItemCount)
  }

  @Test
  fun cancelledBackKeepsTheItems() {
    history.onItemActivated(item("A"))
    history.onItemActivated(item("B"))
    back(1000)
    history.cancelBack()
    assertFalse(history.isBackPending(1100))
    assertEquals(2, history.activatedItemCount)
    back(2000)
    assertEquals(listOf("B", "A"), history.getActivatedItemsAfterBack(2100).map { it.label })
  }

  @Test
  fun itemsOfClosedWindowsAreDropped() {
    history.onItemActivated(item("Gone", window = 3))
    history.onItemActivated(item("A"))
    back(1000, windows = listOf(1, 2))
    assertEquals(1, history.activatedItemCount)
  }

  @Test
  fun backIsNotPendingWithoutItems() {
    back(1000)
    assertFalse(history.isBackPending(1100))
  }

  @Test
  fun activatingTheSameItemAgainKeepsOneAndTheStackIsCapped() {
    val a = item("A")
    history.onItemActivated(a)
    history.onItemActivated(item("A"))
    assertEquals(1, history.activatedItemCount)
    for (i in 0 until AccessibilityFocusActionHistory.MAXIMUM_ACTIVATED_ITEMS + 4) {
      history.onItemActivated(item("Item $i"))
    }
    assertEquals(
      AccessibilityFocusActionHistory.MAXIMUM_ACTIVATED_ITEMS, history.activatedItemCount)
    back(1000)
    assertEquals(
      "Item ${AccessibilityFocusActionHistory.MAXIMUM_ACTIVATED_ITEMS + 3}",
      history.getActivatedItemsAfterBack(1100)[0].label)
  }

  @Test
  fun clearDropsEverything() {
    history.onItemActivated(item("A"))
    back(1000)
    history.clear()
    assertFalse(history.isBackPending(1100))
    assertEquals(0, history.activatedItemCount)
  }
}
