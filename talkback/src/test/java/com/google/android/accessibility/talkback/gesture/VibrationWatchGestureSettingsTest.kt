/* Copyright 2026 Backtalk contributors. Licensed under the Apache License, Version 2.0. */

package com.google.android.accessibility.talkback.gesture

import android.accessibilityservice.AccessibilityService.GESTURE_2_FINGER_DOUBLE_TAP
import android.accessibilityservice.AccessibilityService.GESTURE_2_FINGER_SINGLE_TAP
import android.accessibilityservice.AccessibilityService.GESTURE_3_FINGER_SINGLE_TAP_AND_HOLD
import com.google.android.accessibility.talkback.directtouch.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VibrationWatchGestureSettingsTest {
  private val prefs = FakeSharedPreferences()

  @Test
  fun defaultDoesNotReserveEitherTap() {
    assertFalse(VibrationWatchGestureSettings.isEnabled(prefs))
    for (gesture in listOf(GESTURE_2_FINGER_SINGLE_TAP, GESTURE_2_FINGER_DOUBLE_TAP)) {
      assertFalse(VibrationWatchGestureSettings.shouldReserve(prefs, true, gesture))
    }
  }

  @Test
  fun reservesOnlySingleAndDoubleTwoFingerTapsOnWear() {
    prefs.edit().putBoolean(VibrationWatchGestureSettings.PREF_RESERVED, true).apply()
    // Exercise all framework gesture IDs, including swipe, hold and triple-tap variants.
    for (gesture in -1..GESTURE_3_FINGER_SINGLE_TAP_AND_HOLD) {
      val reserved = VibrationWatchGestureSettings.shouldReserve(prefs, true, gesture)
      if (gesture == GESTURE_2_FINGER_SINGLE_TAP || gesture == GESTURE_2_FINGER_DOUBLE_TAP) {
        assertTrue("Gesture $gesture", reserved)
      } else {
        assertFalse("Gesture $gesture", reserved)
      }
      assertFalse(VibrationWatchGestureSettings.shouldReserve(prefs, false, gesture))
    }
  }

  @Test
  fun switchingTakesEffectImmediatelyAndKeepsAssignments() {
    val assignmentKey = "pref_shortcut_2finger_1tap"
    prefs.edit().putString(assignmentKey, "custom_action").apply()
    for (enabled in listOf(true, false, true)) {
      prefs.edit().putBoolean(VibrationWatchGestureSettings.PREF_RESERVED, enabled).apply()
      val reserved =
        VibrationWatchGestureSettings.shouldReserve(prefs, true, GESTURE_2_FINGER_SINGLE_TAP)
      assertEquals(enabled, reserved)
      assertEquals("custom_action", prefs.getString(assignmentKey, null))
    }
  }
}
