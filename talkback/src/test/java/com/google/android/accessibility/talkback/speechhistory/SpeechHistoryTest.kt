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

package com.google.android.accessibility.talkback.speechhistory

import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
import com.google.android.accessibility.utils.output.SpeechHistory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SpeechHistoryTest {
  @Before
  fun setUp() {
    SpeechHistory.setPaused(false)
    SpeechHistory.start()
  }

  @After
  fun tearDown() {
    SpeechHistory.setPaused(false)
    SpeechHistory.stop()
  }

  @Test
  fun newestTextComesFirst() {
    SpeechHistory.record("One")
    SpeechHistory.record("Two")
    assertEquals(listOf("Two", "One"), SpeechHistory.entries())
  }

  @Test
  fun keepsTheNewestHundred() {
    for (i in 1..SpeechHistory.MAX + 20) SpeechHistory.record("Text $i")
    val entries = SpeechHistory.entries()
    assertEquals(SpeechHistory.MAX, entries.size)
    assertEquals("Text ${SpeechHistory.MAX + 20}", entries.first())
    assertEquals("Text 21", entries.last())
  }

  @Test
  fun longTextIsCut() {
    SpeechHistory.record("a".repeat(SpeechHistory.MAX_LENGTH + 100))
    assertEquals(SpeechHistory.MAX_LENGTH, SpeechHistory.entries().single().length)
  }

  @Test
  fun cutDoesNotSplitASurrogatePair() {
    SpeechHistory.record("a".repeat(SpeechHistory.MAX_LENGTH - 1) + "😀")
    assertEquals(SpeechHistory.MAX_LENGTH - 1, SpeechHistory.entries().single().length)
  }

  @Test
  fun sameTextTwiceInARowIsListedOnce() {
    SpeechHistory.record("Again")
    SpeechHistory.record("Again")
    SpeechHistory.record("  ")
    assertEquals(listOf("Again"), SpeechHistory.entries())
  }

  @Test
  fun speechWithAccessibilityFocusOnAPasswordIsLeftOut() {
    SpeechHistory.onEvent(TYPE_VIEW_ACCESSIBILITY_FOCUSED, password = true, eventTime = 100L)
    SpeechHistory.record("Secret")
    SpeechHistory.onEvent(TYPE_VIEW_ACCESSIBILITY_FOCUSED, password = false, eventTime = 200L)
    SpeechHistory.record("Public")
    assertEquals(listOf("Public"), SpeechHistory.entries())
  }

  @Test
  fun speechWithInputFocusOnAPasswordIsLeftOut() {
    SpeechHistory.onEvent(TYPE_VIEW_FOCUSED, password = true, eventTime = 100L)
    // Keyboard keys have accessibility focus while typing.
    SpeechHistory.onEvent(TYPE_VIEW_ACCESSIBILITY_FOCUSED, password = false, eventTime = 200L)
    SpeechHistory.record("Typed")
    SpeechHistory.onEvent(TYPE_VIEW_FOCUSED, password = false, eventTime = 300L)
    SpeechHistory.record("Public")
    assertEquals(listOf("Public"), SpeechHistory.entries())
  }

  @Test
  fun typingInAPasswordFieldMeansItHasInputFocus() {
    SpeechHistory.onEvent(TYPE_VIEW_TEXT_CHANGED, password = true, eventTime = 100L)
    assertTrue(SpeechHistory.isPasswordFocused())
    SpeechHistory.onEvent(TYPE_VIEW_TEXT_CHANGED, password = false, eventTime = 200L)
    assertTrue(SpeechHistory.isPasswordFocused())
  }

  @Test
  fun aPasswordFocusedBySandFlyIsLeftOutBeforeTheAppsEvent() {
    SpeechHistory.onAccessibilityFocusSet(password = true, time = 1_000L)
    SpeechHistory.record("Secret")
    // The event of the focus before, sent before the action, arriving late.
    SpeechHistory.onEvent(TYPE_VIEW_ACCESSIBILITY_FOCUSED, password = false, eventTime = 990L)
    SpeechHistory.record("Secret again")
    assertTrue(SpeechHistory.entries().isEmpty())
    // The app's event for the next focus.
    SpeechHistory.onAccessibilityFocusSet(password = false, time = 2_000L)
    SpeechHistory.onEvent(TYPE_VIEW_ACCESSIBILITY_FOCUSED, password = false, eventTime = 2_001L)
    SpeechHistory.record("Public")
    assertEquals(listOf("Public"), SpeechHistory.entries())
  }

  @Test
  fun unknownFocusIsLeftOutUntilFocusMovesOrIsReadAgain() {
    SpeechHistory.onFocusUnknown()
    SpeechHistory.record("Unknown")
    SpeechHistory.onFocusRead(accessibilityFocusPassword = false, inputFocusPassword = false)
    assertFalse(SpeechHistory.isPasswordFocused())
    SpeechHistory.record("Public")
    assertEquals(listOf("Public"), SpeechHistory.entries())
  }

  @Test
  fun nothingIsRecordedWhilePausedOrAfterSandFlyStops() {
    SpeechHistory.setPaused(true)
    SpeechHistory.record("While shown")
    SpeechHistory.setPaused(false)
    SpeechHistory.record("Kept")
    assertEquals(listOf("Kept"), SpeechHistory.entries())

    SpeechHistory.stop()
    assertTrue(SpeechHistory.entries().isEmpty())
    SpeechHistory.record("After stop")
    assertTrue(SpeechHistory.entries().isEmpty())
  }
}
