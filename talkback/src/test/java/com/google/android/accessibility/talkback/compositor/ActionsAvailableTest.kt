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

package com.google.android.accessibility.talkback.compositor

import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import com.google.android.accessibility.talkback.directtouch.FakeSharedPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ActionsAvailableTest {
  // An ID an app gives a custom action, outside Android's IDs.
  private val replyId = 0x7f0a0001

  private fun node(
    className: String = "android.view.ViewGroup",
    configure: AccessibilityNodeInfoCompat.() -> Unit = {},
  ) =
    AccessibilityNodeInfoCompat.wrap(AccessibilityNodeInfo.obtain()).apply {
      this.className = className
      packageName = "example.chat"
      isVisibleToUser = true
      configure()
    }

  @Test
  fun aChatMessageWithReplyHasActions() {
    val message = node {
      isClickable = true
      addAction(AccessibilityActionCompat.ACTION_CLICK)
      addAction(AccessibilityActionCompat(replyId, "Reply"))
    }
    assertTrue(ActionsAvailable.has(message))
  }

  @Test
  fun aPlainButtonHasNone() {
    val button = node("android.widget.Button") {
      isClickable = true
      isLongClickable = true
      addAction(AccessibilityActionCompat.ACTION_CLICK)
      addAction(AccessibilityActionCompat.ACTION_LONG_CLICK)
      addAction(AccessibilityActionCompat.ACTION_FOCUS)
      addAction(AccessibilityActionCompat.ACTION_SELECT)
      addAction(AccessibilityActionCompat.ACTION_ACCESSIBILITY_FOCUS)
    }
    assertFalse(ActionsAvailable.has(button))
  }

  @Test
  fun androidsActionsDoNotCountEvenWithALabelOfTheApp() {
    val item = node {
      addAction(AccessibilityActionCompat(AccessibilityNodeInfoCompat.ACTION_CLICK, "Play"))
      addAction(AccessibilityActionCompat.ACTION_SCROLL_FORWARD)
      addAction(AccessibilityActionCompat.ACTION_EXPAND)
      addAction(AccessibilityActionCompat.ACTION_DISMISS)
    }
    assertFalse(ActionsAvailable.has(item))
  }

  @Test
  fun aCustomActionWithoutALabelDoesNotCount() {
    assertFalse(ActionsAvailable.has(node { addAction(AccessibilityActionCompat(replyId, null)) }))
  }

  @Test
  fun keyboardKeysDoNotCount() {
    val key = node("android.widget.TextView") {
      isTextEntryKey = true
      addAction(AccessibilityActionCompat(replyId, "Insert accent"))
    }
    assertFalse(ActionsAvailable.has(key))
    assertFalse(ActionsAvailable.has(null))
  }

  @Test
  fun bothSettingsKeepTheDefaultBehaviour() {
    val prefs = FakeSharedPreferences()
    assertFalse(ActionsAvailable.isSpoken(prefs))
    assertFalse(ActionsAvailable.isSoundAfterSpeech(prefs))
    prefs
      .edit()
      .putString(ActionsAvailable.PREF_SOUND_TIMING, ActionsAvailable.TIMING_AFTER_SPEECH)
      .putBoolean(ActionsAvailable.PREF_SPEAK, true)
      .apply()
    assertTrue(ActionsAvailable.isSoundAfterSpeech(prefs))
    assertTrue(ActionsAvailable.isSpoken(prefs))
  }
}
