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

package com.google.android.accessibility.talkback.actor

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo
import com.google.android.accessibility.utils.Performance.EventId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The order of speaking focus before its action and of the action, with a focus action that
 * fails. A failed action leaves the focus history alone, so none is needed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FocusManagerInternalSpeakFirstTest {
  private val steps = mutableListOf<String>()

  private val pipeline =
    object : Pipeline.FeedbackReturner {
      override fun returnFeedback(feedback: Feedback): Boolean {
        val part = feedback.failovers()[0]
        val nodeAction = part.nodeAction()
        return when {
          nodeAction != null -> {
            steps += "action ${nodeAction.actionId()}"
            false
          }
          part.interruptAllFeedback() -> {
            steps += "interrupt"
            true
          }
          else -> true
        }
      }
    }

  private inner class Listener(private val speaks: Boolean) :
    FocusManagerInternal.FocusSetListener {
    override fun beforeAccessibilityFocusSet(
      node: AccessibilityNodeInfoCompat,
      info: FocusActionInfo,
      eventId: EventId?,
      actionTime: Long,
    ): Boolean {
      steps += "before"
      return speaks
    }

    override fun onAccessibilityFocusSet(
      node: AccessibilityNodeInfoCompat,
      info: FocusActionInfo,
      eventId: EventId?,
      actionTime: Long,
    ) {
      steps += "set"
    }

    override fun onAccessibilityFocusNotSet(node: AccessibilityNodeInfoCompat) {
      steps += "not set"
    }
  }

  private fun manager(listener: FocusManagerInternal.FocusSetListener): FocusManagerInternal =
    FocusManagerInternal(null, null, null, null, null).apply {
      setPipeline(pipeline)
      setFocusSetListener(listener)
    }

  @Suppress("DEPRECATION") // obtain(): a node for the action.
  private val node = AccessibilityNodeInfoCompat.obtain()

  private val info =
    FocusActionInfo.builder().setSourceAction(FocusActionInfo.LOGICAL_NAVIGATION).build()

  private val focusAction = "action ${AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS}"

  @Test
  fun spokenFirst_failedAction_stopsSpeechAndTellsListener() {
    val result =
      manager(Listener(speaks = true))
        .performAccessibilityFocusActionInternal(node, info, null, /* maySpeakFirst= */ true)

    assertFalse(result)
    assertEquals(listOf("before", focusAction, "interrupt", "not set"), steps)
  }

  @Test
  fun notSpokenFirst_failedAction_leavesSpeech() {
    manager(Listener(speaks = false))
      .performAccessibilityFocusActionInternal(node, info, null, /* maySpeakFirst= */ true)

    assertEquals(listOf("before", focusAction), steps)
  }

  @Test
  fun inputFocusSynced_notSpokenFirst() {
    manager(Listener(speaks = true))
      .performAccessibilityFocusActionInternal(node, info, null, /* maySpeakFirst= */ false)

    assertEquals(listOf(focusAction), steps)
  }

  @Test
  fun mutedNextFocus_notSpokenFirst() {
    val manager = manager(Listener(speaks = true))
    manager.setMuteNextFocus()

    manager.performAccessibilityFocusActionInternal(node, info, null, /* maySpeakFirst= */ true)

    assertEquals(listOf(focusAction), steps)
  }
}
