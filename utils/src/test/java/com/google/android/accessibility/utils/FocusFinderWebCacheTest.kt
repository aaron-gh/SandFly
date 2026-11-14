/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.utils

import android.accessibilityservice.AccessibilityService
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FocusFinderWebCacheTest {
  private class Service : AccessibilityService() {
    var cached: AccessibilityNodeInfo? = null
    var actual: AccessibilityNodeInfo? = null
    var root: AccessibilityNodeInfo? = null
    var queries = 0
    var clears = 0
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun findFocus(type: Int): AccessibilityNodeInfo? {
      queries++
      return cached
    }
    override fun clearCachedSubtree(node: AccessibilityNodeInfo): Boolean {
      clears++
      cached = actual
      return true
    }
    override fun getRootInActiveWindow(): AccessibilityNodeInfo? = root
  }

  private fun node(web: Boolean, focused: Boolean, visible: Boolean = true) =
    AccessibilityNodeInfo.obtain(View(RuntimeEnvironment.getApplication())).apply {
      isVisibleToUser = visible
      isAccessibilityFocused = focused
      if (web) addAction(AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT)
      shadowOf(this).setRefreshReturnValue(true)
      ReflectionHelpers.callInstanceMethod<Any>(this, "setSealed",
        ReflectionHelpers.ClassParameter.from(Boolean::class.javaPrimitiveType!!, true))
    }

  @Test fun currentWebFocusNeedsNoSecondQuery() {
    val current = node(web = true, focused = true)
    val service = Service().apply { cached = current; actual = current }
    assertSame(current, FocusFinder.getAccessibilityFocusNode(service, false)?.unwrap())
    assertEquals(1, service.queries)
    assertEquals(0, service.clears)
  }

  @Test fun anExistingWebNodeThatLostFocusIsReplacedByTheActualFocus() {
    val old = node(web = true, focused = false)
    val next = node(web = true, focused = true)
    val service = Service().apply { cached = old; actual = next }
    assertSame(next, FocusFinder.getAccessibilityFocusNode(service, false)?.unwrap())
    assertEquals(2, service.queries)
    assertEquals(1, service.clears)
  }

  @Test fun aWebFocusThatWasClearedKeepsTheStillExistingPivot() {
    val previous = node(web = true, focused = false)
    val service = Service().apply { cached = previous }
    assertSame(previous, FocusFinder.getAccessibilityFocusNode(service, false)?.unwrap())
    assertEquals(1, service.clears)
  }

  @Test fun replacingStaleWebFocusDoesNotExposeAnInvisibleNativeView() {
    val service = Service().apply {
      cached = node(web = true, focused = false)
      actual = node(web = false, focused = true, visible = false)
    }
    assertNull(FocusFinder.getAccessibilityFocusNode(service, false))
    assertEquals(1, service.clears)
  }

  @Test fun replacingStaleWebFocusPreservesTheExistingWebVisibilityExemption() {
    val current = node(web = true, focused = true, visible = false)
    val service = Service().apply {
      cached = node(web = true, focused = false)
      actual = current
    }
    assertSame(current, FocusFinder.getAccessibilityFocusNode(service, false)?.unwrap())
    assertEquals(1, service.clears)
  }

  @Test fun aRemovedCachedWebNodeIsReplacedByTheActualFocus() {
    val old = node(web = true, focused = true).apply {
      shadowOf(this).setRefreshReturnValue(false)
    }
    val current = node(web = true, focused = true)
    val service = Service().apply { cached = old; actual = current }
    assertSame(current, FocusFinder.getAccessibilityFocusNode(service, false)?.unwrap())
    assertEquals(2, service.queries)
    assertEquals(1, service.clears)
  }

  @Test fun aRemovedWebNodeWithoutAnyActualFocusStillReturnsNull() {
    val old = node(web = true, focused = true).apply {
      shadowOf(this).setRefreshReturnValue(false)
    }
    val service = Service().apply { cached = old }
    assertNull(FocusFinder.getAccessibilityFocusNode(service, false))
    assertEquals(1, service.clears)
  }

  @Test fun aRemovedNativeNodeKeepsTheExistingFailureBehavior() {
    val old = node(web = false, focused = true).apply {
      shadowOf(this).setRefreshReturnValue(false)
    }
    val service = Service().apply { cached = old; actual = node(web = false, focused = true) }
    assertNull(FocusFinder.getAccessibilityFocusNode(service, false))
    assertEquals(1, service.queries)
    assertEquals(0, service.clears)
  }

  @Test fun nativeFocusKeepsTheExistingBehavior() {
    val current = node(web = false, focused = false)
    val service = Service().apply { cached = current }
    assertSame(current, FocusFinder.getAccessibilityFocusNode(service, false)?.unwrap())
    assertEquals(1, service.queries)
    assertEquals(0, service.clears)
  }

  @Test fun aFallbackRootDoesNotHaveToBeFocused() {
    val root = node(web = true, focused = false)
    val service = Service().apply { this.root = root }
    assertSame(root, FocusFinder.getAccessibilityFocusNode(service, true)?.unwrap())
    assertEquals(0, service.clears)
  }
}
