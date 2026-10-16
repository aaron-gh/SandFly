/* Copyright 2026 Backtalk contributors. Licensed under the Apache License, Version 2.0. */

package com.google.android.accessibility.talkback.gesture

import android.accessibilityservice.AccessibilityService.GESTURE_2_FINGER_DOUBLE_TAP
import android.accessibilityservice.AccessibilityService.GESTURE_2_FINGER_SINGLE_TAP
import android.content.SharedPreferences

/** Reserves only the two tap gestures used by Samsung Vibration Watch. */
object VibrationWatchGestureSettings {
  const val PREF_RESERVED = "pref_reserve_vibration_watch_gestures"

  @JvmStatic
  fun isEnabled(prefs: SharedPreferences): Boolean = prefs.getBoolean(PREF_RESERVED, false)

  /** Read the preference on each gesture so switching it takes effect immediately. */
  @JvmStatic
  fun shouldReserve(prefs: SharedPreferences, isWear: Boolean, gestureId: Int): Boolean =
    isWear &&
      isEnabled(prefs) &&
      (gestureId == GESTURE_2_FINGER_SINGLE_TAP || gestureId == GESTURE_2_FINGER_DOUBLE_TAP)
}
