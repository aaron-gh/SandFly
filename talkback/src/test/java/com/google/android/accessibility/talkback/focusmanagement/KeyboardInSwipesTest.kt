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

import android.view.accessibility.AccessibilityWindowInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Swipes skip the keyboard, and only double-tap typing swipes through its keys. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class KeyboardInSwipesTest {
  @Test
  fun swipesFromOtherWindowsSkipTheKeyboard() {
    assertFalse(
      FocusProcessorForLogicalNavigation.swipesIncludeKeyboard(
        window(AccessibilityWindowInfo.TYPE_APPLICATION)
      )
    )
    assertFalse(
      FocusProcessorForLogicalNavigation.swipesIncludeKeyboard(
        window(AccessibilityWindowInfo.TYPE_SYSTEM)
      )
    )
  }

  @Test
  fun swipesFromInsideTheKeyboardStillLeaveIt() {
    assertTrue(
      FocusProcessorForLogicalNavigation.swipesIncludeKeyboard(
        window(AccessibilityWindowInfo.TYPE_INPUT_METHOD)
      )
    )
  }

  @Test
  fun onlyDoubleTapTypingSwipesThroughKeys() {
    assertFalse(
      FocusProcessorForLogicalNavigation.swipesFromKeysStayInApp(
        FocusProcessorForTapAndTouchExploration.DOUBLE_TAP,
        /* throughKeys= */ false,
      )
    )
    for (method in
      listOf(
        FocusProcessorForTapAndTouchExploration.LIFT_TO_TYPE,
        FocusProcessorForTapAndTouchExploration.FORCE_LIFT_TO_TYPE_ON_IME,
        FocusProcessorForTapAndTouchExploration.LIFT_TO_TYPE_EXCEPT_ACTION_KEY,
        FocusProcessorForTapAndTouchExploration.TAP_TO_TYPE,
      )) {
      assertTrue(FocusProcessorForLogicalNavigation.swipesFromKeysStayInApp(method, false))
      assertFalse(FocusProcessorForLogicalNavigation.swipesFromKeysStayInApp(method, true))
    }
  }

  private fun window(type: Int): AccessibilityWindowInfo {
    val window = AccessibilityWindowInfo.obtain()
    AccessibilityWindowInfo::class
      .java
      .getDeclaredMethod("setType", Int::class.javaPrimitiveType)
      .invoke(window, type)
    return window
  }
}
