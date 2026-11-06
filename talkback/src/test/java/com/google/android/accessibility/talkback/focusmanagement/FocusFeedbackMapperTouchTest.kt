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

package com.google.android.accessibility.talkback.focusmanagement

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Interpretation
import com.google.android.accessibility.talkback.Interpretation.Touch.Action
import com.google.android.accessibility.talkback.Mappers
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FocusFeedbackMapperTouchTest {
  private val unfocused = AccessibilityNodeInfoCompat.obtain()
  private val focused = AccessibilityNodeInfoCompat.obtain().apply { isAccessibilityFocused = true }

  @After
  fun restoreDefault() {
    FocusFeedbackMapper.setTouchStopsSpeech(true)
  }

  @Test
  fun touchStopsSpeech_stopsItOnTouch_notOnFocus() {
    FocusFeedbackMapper.setTouchStopsSpeech(true)
    assertTrue(part(Action.TOUCH_START, null)!!.interruptGentle())
    assertTrue(part(Action.TOUCH_ENTERED_UNFOCUSED_NODE, unfocused)!!.interruptGentle())
    assertFalse(part(Action.TOUCH_UNFOCUSED_NODE, unfocused)!!.interruptGentle())
    assertFalse(part(Action.TOUCH_FOCUSED_NODE, focused)!!.interruptGentle())
  }

  @Test
  fun touchDoesNotStopSpeech_stopsItWhenTouchFocusesAnItem() {
    FocusFeedbackMapper.setTouchStopsSpeech(false)
    assertNull(part(Action.TOUCH_START, null))
    assertNull(part(Action.TOUCH_ENTERED_UNFOCUSED_NODE, unfocused))
    // Speech that new speech can't interrupt, such as a notification, would otherwise keep the
    // item from being spoken.
    val focus = part(Action.TOUCH_UNFOCUSED_NODE, unfocused)!!
    assertTrue(focus.interruptGentle())
    assertNotNull(focus.focus())
    assertTrue(part(Action.TOUCH_FOCUSED_NODE, focused)!!.interruptGentle())
  }

  @Test
  fun touchDoesNotStopSpeech_keepsTheFocusedItemsSpeechWhenTheFingerStaysOnIt() {
    FocusFeedbackMapper.setTouchStopsSpeech(false)
    val focus = part(Action.TOUCH_UNFOCUSED_NODE, focused)!!
    assertFalse(focus.interruptGentle())
    assertNotNull(focus.focus())
  }

  private fun part(action: Action, node: AccessibilityNodeInfoCompat?): Feedback.Part? {
    val variables =
      Mappers.Variables(
        RuntimeEnvironment.getApplication(),
        /* event= */ null,
        Interpretation.Touch.create(action, node),
        /* monitors= */ null,
      )
    return FocusFeedbackMapper.mapTouchToFocusAction(EVENT_ID_UNTRACKED, variables, 0)
      ?.failovers()
      ?.first()
  }
}
