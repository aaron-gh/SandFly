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

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.RangeInfoCompat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Report #358: Chrome's indeterminate page-load bar gave the progress tone a NaN rate. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressBarEarconRateTest {
  private fun progressBar(range: RangeInfoCompat? = null): AccessibilityNodeInfoCompat =
    AccessibilityNodeInfoCompat.wrap(AccessibilityNodeInfo()).apply {
      className = "android.widget.ProgressBar"
      if (range != null) rangeInfo = range
    }

  private fun contentChanged(itemCount: Int, index: Int): AccessibilityEvent =
    AccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED).apply {
      this.itemCount = itemCount
      currentItemIndex = index
    }

  @Test
  fun indeterminateBarWithNoItems_playsAtTheLowestRate() {
    assertEquals(
      0.5,
      EarconFeedbackUtils.getProgressBarChangeEarconRate(contentChanged(0, 0), progressBar()),
      1e-9,
    )
  }

  @Test
  fun eventWithoutItemCount_playsAtTheLowestRate() {
    assertEquals(
      0.5,
      EarconFeedbackUtils.getProgressBarChangeEarconRate(contentChanged(-1, -1), progressBar()),
      1e-9,
    )
  }

  @Test
  fun halfway_playsAtItsOwnRate() {
    assertEquals(
      1.0,
      EarconFeedbackUtils.getProgressBarChangeEarconRate(contentChanged(100, 50), progressBar()),
      1e-9,
    )
  }

  @Test
  fun percentOutOfRange_isClamped() {
    val over = RangeInfoCompat.obtain(RangeInfoCompat.RANGE_TYPE_PERCENT, 0f, 100f, 250f)
    assertEquals(
      2.0,
      EarconFeedbackUtils.getProgressBarChangeEarconRate(contentChanged(0, 0), progressBar(over)),
      1e-9,
    )
  }

  @Test
  fun percentNaN_playsAtNormalRate() {
    val nan = RangeInfoCompat.obtain(RangeInfoCompat.RANGE_TYPE_PERCENT, 0f, 100f, Float.NaN)
    assertEquals(
      1.0,
      EarconFeedbackUtils.getProgressBarChangeEarconRate(contentChanged(0, 0), progressBar(nan)),
      1e-9,
    )
  }
}
