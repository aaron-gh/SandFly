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
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SupplementalDescriptionTest {
  @Test
  fun readsTheDescriptionChromeKeepsInExtrasBeforeAndroid16() {
    // Chrome's checkbox named by a <label for>, on Android 13: no text, the label in the extras.
    val node = AccessibilityNodeInfo()
    node.extras.putCharSequence(
      "androidx.view.accessibility.AccessibilityNodeInfoCompat.SUPPLEMENTAL_DESCRIPTION_KEY",
      "Echo cancellation",
    )
    assertEquals(
      "Echo cancellation",
      AccessibilityNodeFeedbackUtils.getSupplementalDescription(
          AccessibilityNodeInfoCompat.wrap(node)
        )
        .toString(),
    )
  }

  @Test
  fun emptyWithoutADescription() {
    assertEquals(
      "",
      AccessibilityNodeFeedbackUtils.getSupplementalDescription(
          AccessibilityNodeInfoCompat.wrap(AccessibilityNodeInfo())
        )
        .toString(),
    )
  }
}
