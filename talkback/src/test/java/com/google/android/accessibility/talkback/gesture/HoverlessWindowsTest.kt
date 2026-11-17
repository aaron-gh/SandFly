/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.gesture

import android.graphics.Rect
import android.view.Display
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * Threads (SandFly#440): on its main tabs, touch exploration's hover reaches only an empty
 * container as large as the window, over Compose items with correct bounds.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HoverlessWindowsTest {

  private lateinit var window: AccessibilityWindowInfo
  private lateinit var root: AccessibilityNodeInfo
  private lateinit var post: AccessibilityNodeInfo
  private lateinit var overlay: AccessibilityNodeInfo

  @Before
  fun setUp() {
    HoverlessWindows.clear()
    window = AccessibilityWindowInfo()
    shadowOf(window).apply {
      setId(APP_ID)
      setType(AccessibilityWindowInfo.TYPE_APPLICATION)
      setBoundsInScreen(SCREEN)
      setDisplayId(Display.DEFAULT_DISPLAY)
    }
    root = node(SCREEN)
    shadowOf(window).setRoot(root)
    val feed = node(Rect(0, 200, 1080, 2200))
    post =
      node(Rect(0, 1000, 1080, 1400)).apply {
        text = "Post by someone"
        isClickable = true
        isFocusable = true
        addAction(AccessibilityNodeInfo.ACTION_CLICK)
      }
    shadowOf(feed).addChild(post)
    shadowOf(root).addChild(feed)
    // Drawn over the feed, it takes the hover and reports it for itself.
    overlay = node(SCREEN).apply { className = "android.view.ViewGroup" }
    shadowOf(root).addChild(overlay)
  }

  private fun node(bounds: Rect) =
    AccessibilityNodeInfo().apply {
      // Distinct sources keep the nodes unequal.
      setSource(View(RuntimeEnvironment.getApplication()))
      packageName = "com.instagram.barcelona"
      className = "android.view.View"
      setBoundsInScreen(bounds)
      isVisibleToUser = true
      ReflectionHelpers.setField(this, "mWindowId", APP_ID)
      shadowOf(this).setAccessibilityWindowInfo(window)
    }

  private fun itemFor(touched: AccessibilityNodeInfo) =
    HoverlessWindows.itemUnderFinger(AccessibilityNodeInfoCompat.wrap(touched))

  @Test
  fun hoverOnTheEmptyContainerFocusesThePostUnderTheFinger() {
    HoverlessWindows.onTouchExplorationRequested(500f, 1200f)
    assertEquals(AccessibilityNodeInfoCompat.wrap(post), itemFor(overlay))
  }

  @Test
  fun withoutAFingerPositionNothingIsFound() {
    assertNull(itemFor(overlay))
    HoverlessWindows.onTouchExplorationRequested(500f, 1200f)
    HoverlessWindows.clearStart()
    assertNull(itemFor(overlay))
  }

  @Test
  fun emptySpaceFindsNothing() {
    HoverlessWindows.onTouchExplorationRequested(500f, 600f)
    assertNull(itemFor(overlay))
  }

  @Test
  fun aLabelledContainerIsLeftAlone() {
    HoverlessWindows.onTouchExplorationRequested(500f, 1200f)
    overlay.contentDescription = "Home"
    assertNull(itemFor(overlay))
  }

  @Test
  fun aSmallContainerIsLeftAlone() {
    HoverlessWindows.onTouchExplorationRequested(500f, 1200f)
    overlay.setBoundsInScreen(Rect(0, 900, 1080, 1500))
    assertNull(itemFor(overlay))
  }

  @Test
  fun otherWindowTypesAreLeftAlone() {
    HoverlessWindows.onTouchExplorationRequested(500f, 1200f)
    shadowOf(window).setType(AccessibilityWindowInfo.TYPE_SYSTEM)
    assertNull(itemFor(overlay))
  }

  @Test
  fun mostOfTheWindowIsThreeQuartersOfItsArea() {
    assertTrue(HoverlessWindows.coversMostOf(Rect(0, 0, 1080, 1800), SCREEN))
    assertFalse(HoverlessWindows.coversMostOf(Rect(0, 0, 1080, 1700), SCREEN))
    assertFalse(HoverlessWindows.coversMostOf(Rect(0, 0, 1080, 2400), Rect()))
  }

  private companion object {
    const val APP_ID = 3
    val SCREEN = Rect(0, 0, 1080, 2400)
  }
}
