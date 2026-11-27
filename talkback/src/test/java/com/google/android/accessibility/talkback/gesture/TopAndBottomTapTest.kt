/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.gesture

import android.view.Surface
import org.junit.Assert.assertEquals
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

  @Test
  fun inLandscapeTheEndsOfThePhoneAreLeftAndRight() {
    // A phone 900 high turned sideways: the screen as displayed is 900 wide and 400 high.
    for (rotation in listOf(Surface.ROTATION_90, Surface.ROTATION_270)) {
      assertEquals(900, TopAndBottomTap.deviceLength(900, 400, rotation))
      assertTrue(
        TopAndBottomTap.isTopAndBottom(
          TopAndBottomTap.alongDevice(20f, 200f, rotation),
          TopAndBottomTap.alongDevice(880f, 200f, rotation),
          TopAndBottomTap.deviceLength(900, 400, rotation),
        )
      )
      // The long edges, the top and bottom of the screen as displayed, are no longer the ends.
      assertFalse(
        TopAndBottomTap.isTopAndBottom(
          TopAndBottomTap.alongDevice(450f, 10f, rotation),
          TopAndBottomTap.alongDevice(450f, 390f, rotation),
          TopAndBottomTap.deviceLength(900, 400, rotation),
        )
      )
    }
  }

  @Test
  fun inPortraitTheEndsAreTopAndBottom() {
    for (rotation in listOf(Surface.ROTATION_0, Surface.ROTATION_180)) {
      assertEquals(900, TopAndBottomTap.deviceLength(400, 900, rotation))
      assertEquals(30f, TopAndBottomTap.alongDevice(200f, 30f, rotation))
    }
  }
}
