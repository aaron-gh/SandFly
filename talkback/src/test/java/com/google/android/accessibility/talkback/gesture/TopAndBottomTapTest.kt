/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.gesture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopAndBottomTapTest {
  private val height = 900

  @Test
  fun oneFingerInTheTopThirdAndOneInTheBottomThird() {
    assertTrue(TopAndBottomTap.isTopAndBottom(100f, 800f, height))
    assertTrue(TopAndBottomTap.isTopAndBottom(800f, 100f, height))
    assertTrue(TopAndBottomTap.isTopAndBottom(0f, 899f, height))
    assertTrue(TopAndBottomTap.isTopAndBottom(299f, 600f, height))
  }

  @Test
  fun aFingerInTheMiddleThirdIsNotTopAndBottom() {
    assertFalse(TopAndBottomTap.isTopAndBottom(300f, 800f, height))
    assertFalse(TopAndBottomTap.isTopAndBottom(100f, 599f, height))
    assertFalse(TopAndBottomTap.isTopAndBottom(450f, 450f, height))
  }

  @Test
  fun bothFingersInTheSameThirdIsNotTopAndBottom() {
    assertFalse(TopAndBottomTap.isTopAndBottom(50f, 250f, height))
    assertFalse(TopAndBottomTap.isTopAndBottom(650f, 880f, height))
  }

  @Test
  fun landscapeUsesTheHeightAsDisplayed() {
    // A 2400 by 1080 screen turned to landscape is 1080 pixels high.
    assertTrue(TopAndBottomTap.isTopAndBottom(200f, 900f, 1080))
    assertFalse(TopAndBottomTap.isTopAndBottom(200f, 700f, 1080))
  }

  @Test
  fun noScreenHeightIsNotTopAndBottom() {
    assertFalse(TopAndBottomTap.isTopAndBottom(0f, 0f, 0))
  }
}
