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

import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Which focused nodes are left to the app's focus event rather than spoken early. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NamedOnlyByFocusEventTest {
  private fun card(): AccessibilityNodeInfo =
    AccessibilityNodeInfo.obtain().apply {
      className = "android.widget.FrameLayout"
      isClickable = true
      isFocusable = true
      isVisibleToUser = true
      isEnabled = true
    }

  private fun named(node: AccessibilityNodeInfo) =
    EventFilter.isNamedOnlyByFocusEvent(AccessibilityNodeInfoCompat.wrap(node))

  @Test
  fun childlessNodeWithoutText_isNamedOnlyByEvent() {
    assertTrue(named(card()))
  }

  @Test
  fun blankDescription_isNamedOnlyByEvent() {
    assertTrue(named(card().apply { contentDescription = "  " }))
  }

  @Test
  fun description_isSpokenEarly() {
    assertFalse(named(card().apply { contentDescription = "Settings" }))
  }

  @Test
  fun text_isSpokenEarly() {
    assertFalse(named(card().apply { text = "Settings" }))
  }

  @Test
  fun stateDescription_isSpokenEarly() {
    assertFalse(named(card().apply { stateDescription = "On" }))
  }

  @Test
  fun hint_isSpokenEarly() {
    assertFalse(named(card().apply { hintText = "Search" }))
  }

  @Test
  fun nodeWithChildren_isSpokenEarly() {
    val card = card()
    val title = AccessibilityNodeInfo.obtain().apply { text = "Settings" }
    shadowOf(card).addChild(title)
    assertFalse(named(card))
  }

  /** A HiOS recent apps card (report 473): long-clickable, not clickable, only custom actions. */
  @Test
  fun recentAppsCardWithOnlyActions_isNamedOnlyByEvent() {
    val card =
      AccessibilityNodeInfo.obtain().apply {
        className = "android.widget.FrameLayout"
        isLongClickable = true
        isFocusable = true
        isVisibleToUser = true
        isEnabled = true
        addAction(AccessibilityNodeInfo.AccessibilityAction(1001, "Close"))
        addAction(AccessibilityNodeInfo.AccessibilityAction(1002, "Info aplikasi"))
      }
    assertTrue(named(card))
  }
}
