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

import android.view.accessibility.AccessibilityEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A window is read ahead of its order at most once every 2 seconds, unless an event has cleared
 * part of it from Android's node cache since.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WindowNodePrefetcherReadAgainTest {
  private val window = 41

  @Before
  fun setUp() {
    // Forget the window read by an earlier test.
    WindowNodePrefetcher.onEvent(-1, AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, 0)
    assertTrue(WindowNodePrefetcher.shouldReadWindow(window, START))
  }

  @Test
  fun unchangedWindowIsNotReadAgainWithinTwoSeconds() {
    assertFalse(WindowNodePrefetcher.shouldReadWindow(window, START + 1_000))
    assertTrue(WindowNodePrefetcher.shouldReadWindow(window, START + 2_000))
  }

  @Test
  fun subtreeChangeHasWindowReadAgain() {
    WindowNodePrefetcher.onEvent(
      window,
      AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
      AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE or AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT,
    )
    assertTrue(WindowNodePrefetcher.shouldReadWindow(window, START + 100))
    assertFalse(WindowNodePrefetcher.shouldReadWindow(window, START + 200))
  }

  @Test
  fun scrollHasWindowReadAgain() {
    WindowNodePrefetcher.onEvent(window, AccessibilityEvent.TYPE_VIEW_SCROLLED, 0)
    assertTrue(WindowNodePrefetcher.shouldReadWindow(window, START + 100))
  }

  @Test
  fun textChangeKeepsWindowRead() {
    WindowNodePrefetcher.onEvent(
      window,
      AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
      AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT,
    )
    WindowNodePrefetcher.onEvent(window, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED, 0)
    assertFalse(WindowNodePrefetcher.shouldReadWindow(window, START + 100))
  }

  @Test
  fun changeInAnotherWindowKeepsWindowRead() {
    WindowNodePrefetcher.onEvent(
      window + 1,
      AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
      AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE,
    )
    assertFalse(WindowNodePrefetcher.shouldReadWindow(window, START + 100))
  }

  @Test
  fun anotherWindowIsRead() {
    assertTrue(WindowNodePrefetcher.shouldReadWindow(window + 1, START + 100))
  }

  private companion object {
    const val START = 1_000_000L
  }
}
