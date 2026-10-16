/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.talkback.compositor

import android.view.accessibility.AccessibilityEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreparedFocusSpeechTest {
  private val window = 7
  private val otherWindow = 8

  @Test
  fun anyContentChangeInTheWindowCounts_evenTextOrStateAlone() {
    assertTrue(
      PreparedFocusSpeech.canChange(
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
        window,
        window,
      )
    )
    assertTrue(PreparedFocusSpeech.canChange(AccessibilityEvent.TYPE_VIEW_SCROLLED, window, window))
    assertTrue(
      PreparedFocusSpeech.canChange(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED, window, window)
    )
  }

  @Test
  fun changesInAnotherWindowDoNotCount() {
    assertFalse(
      PreparedFocusSpeech.canChange(
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
        otherWindow,
        window,
      )
    )
  }

  @Test
  fun windowChangesAndEventsWithoutAWindowCount() {
    assertTrue(
      PreparedFocusSpeech.canChange(AccessibilityEvent.TYPE_WINDOWS_CHANGED, otherWindow, window)
    )
    assertTrue(
      PreparedFocusSpeech.canChange(
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
        otherWindow,
        window,
      )
    )
    assertTrue(PreparedFocusSpeech.canChange(AccessibilityEvent.TYPE_VIEW_CLICKED, -1, window))
  }

  @Test
  fun focusAndTouchEventsDoNotCount() {
    for (type in
      listOf(
        AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED,
        AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED,
        AccessibilityEvent.TYPE_VIEW_HOVER_ENTER,
        AccessibilityEvent.TYPE_TOUCH_INTERACTION_START,
        AccessibilityEvent.TYPE_TOUCH_INTERACTION_END,
        AccessibilityEvent.TYPE_ANNOUNCEMENT,
      )) {
      assertFalse(PreparedFocusSpeech.canChange(type, window, window))
    }
  }

  @Test
  fun announcementPreparedJustBeforeTheSwipeIsNotUsed() {
    val prepared = 1_000L
    assertFalse(PreparedFocusSpeech.isOldEnough(prepared, prepared))
    assertFalse(
      PreparedFocusSpeech.isOldEnough(prepared, prepared + PreparedFocusSpeech.MIN_AGE_MS - 1)
    )
    assertTrue(PreparedFocusSpeech.isOldEnough(prepared, prepared + PreparedFocusSpeech.MIN_AGE_MS))
    assertTrue(PreparedFocusSpeech.isOldEnough(prepared, prepared + 2_000L))
  }
}
