/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.utils

import android.graphics.Rect
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TextInsideItemsFocusTest {
  @After fun tearDown() = AccessibilityNodeInfoUtils.setFocusTextInsideItems(false)

  // Each node gets a view of its own, so that nodes don't compare equal to each other.
  private fun node(label: String? = null) = AccessibilityNodeInfo.obtain().apply {
    setSource(View(RuntimeEnvironment.getApplication()))
    className = "android.widget.TextView"
    packageName = "example.app"
    isVisibleToUser = true
    isImportantForAccessibility = true
    setBoundsInScreen(Rect(100, 100, 200, 200))
    text = label
  }

  private fun group(vararg children: AccessibilityNodeInfo) = node().apply {
    className = "android.widget.LinearLayout"
    children.forEach { shadowOf(this).addChild(it) }
  }

  private fun row(vararg children: AccessibilityNodeInfo) = group(*children).apply {
    isClickable = true
    isFocusable = true
    addAction(AccessibilityNodeInfo.ACTION_CLICK)
  }

  private fun accepts(node: AccessibilityNodeInfo) =
    AccessibilityNodeInfoUtils.shouldFocusNode(AccessibilityNodeInfoCompat.wrap(node))

  private fun hoverTarget(touched: AccessibilityNodeInfo) =
    AccessibilityNodeInfoUtils.findFocusFromHover(AccessibilityNodeInfoCompat.wrap(touched))

  @Test fun offTheRowIsOneItem() {
    val title = node("Battery")
    val summary = node("Last charged 2 hours ago")
    val item = row(title, summary)
    assertTrue(accepts(item))
    assertFalse(accepts(title))
    assertFalse(accepts(summary))
    assertEquals(AccessibilityNodeInfoCompat.wrap(item), hoverTarget(summary))
  }

  @Test fun onEachTextIsAnItemAndTheRowIsNot() {
    AccessibilityNodeInfoUtils.setFocusTextInsideItems(true)
    val title = node("Battery")
    val summary = node("Last charged 2 hours ago")
    val item = row(title, group(summary))
    assertFalse(accepts(item))
    assertTrue(accepts(title))
    assertTrue(accepts(summary))
    assertEquals(AccessibilityNodeInfoCompat.wrap(summary), hoverTarget(summary))
  }

  @Test fun onARowWithItsOwnLabelKeepsFocus() {
    AccessibilityNodeInfoUtils.setFocusTextInsideItems(true)
    val summary = node("Last charged 2 hours ago")
    val item = row(node("Battery"), summary).apply { contentDescription = "Battery, 80 percent" }
    assertTrue(accepts(item))
    assertTrue(accepts(summary))
  }

  @Test fun onARowWithCustomActionsKeepsFocus() {
    AccessibilityNodeInfoUtils.setFocusTextInsideItems(true)
    val item = row(node("Message")).apply { addAction(AccessibilityAction(0x7f0a0001, "Archive")) }
    assertTrue(accepts(item))
  }

  @Test fun onACheckableRowKeepsFocus() {
    AccessibilityNodeInfoUtils.setFocusTextInsideItems(true)
    val item = row(node("Wi-Fi")).apply { isCheckable = true }
    assertTrue(accepts(item))
  }

  @Test fun onASelectedTabKeepsFocus() {
    AccessibilityNodeInfoUtils.setFocusTextInsideItems(true)
    val tab = row(node("Chats")).apply { isSelected = true }
    assertTrue(accepts(tab))
  }

  @Test fun onAnItemWithARoleDescriptionKeepsFocus() {
    AccessibilityNodeInfoUtils.setFocusTextInsideItems(true)
    val tab = row(node("Calls"))
    AccessibilityNodeInfoCompat.wrap(tab).roleDescription = "Tab"
    assertTrue(accepts(tab))
  }

  @Test fun onAHeadingItemKeepsFocus() {
    AccessibilityNodeInfoUtils.setFocusTextInsideItems(true)
    val item = row(node("Network")).apply { isHeading = true }
    assertTrue(accepts(item))
  }

  @Suppress("DEPRECATION")
  @Test fun onASelectedCollectionItemKeepsFocus() {
    AccessibilityNodeInfoUtils.setFocusTextInsideItems(true)
    val item =
      row(node("Inbox")).apply {
        collectionItemInfo =
          AccessibilityNodeInfo.CollectionItemInfo.obtain(0, 1, 0, 1, false, /* selected= */ true)
      }
    assertTrue(accepts(item))
  }

  @Test fun onTextPastTheNodeLimitLeavesTheRowAsOneItem() {
    AccessibilityNodeInfoUtils.setFocusTextInsideItems(true)
    val wrappers =
      Array(AccessibilityNodeInfoUtils.MAX_TEXT_INSIDE_NODES) { node().apply { text = null } }
    val item = row(*wrappers, node("Battery"))
    assertTrue(accepts(item))
  }

  @Test fun onAStateInsideTheRowIsAnItem() {
    AccessibilityNodeInfoUtils.setFocusTextInsideItems(true)
    val toggle = node().apply {
      className = "android.widget.Switch"
      isCheckable = true
      isChecked = true
    }
    val item = row(node("Wi-Fi"), toggle)
    assertFalse(accepts(item))
    assertTrue(accepts(toggle))
  }

  @Test fun onTextOfAButtonInsideTheRowBelongsToTheButton() {
    AccessibilityNodeInfoUtils.setFocusTextInsideItems(true)
    val label = node("Details")
    val button = row(label)
    val item = row(button)
    assertFalse(accepts(item))
    assertFalse(accepts(button))
    assertTrue(accepts(label))
  }

  @Test fun onARowWithoutTextInsideIsUnchanged() {
    AccessibilityNodeInfoUtils.setFocusTextInsideItems(true)
    val item = row(node().apply { contentDescription = null }).apply { contentDescription = "Play" }
    assertTrue(accepts(item))
  }

  @Test fun onAnInvisibleTextDoesNotTakeTheRowsPlace() {
    AccessibilityNodeInfoUtils.setFocusTextInsideItems(true)
    val hidden = node("Hidden").apply { isVisibleToUser = false }
    val visible = node("Shown")
    val item = row(hidden, visible)
    assertFalse(accepts(hidden))
    assertTrue(accepts(visible))
    assertFalse(accepts(item))
  }
}
