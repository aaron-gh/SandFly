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
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WebLandmarkTest {
  private fun webNode(chromeRole: String?): AccessibilityNodeInfoCompat {
    val node = AccessibilityNodeInfo()
    if (chromeRole != null) {
      node.extras.putCharSequence("AccessibilityNodeInfo.chromeRole", chromeRole)
    }
    return AccessibilityNodeInfoCompat.wrap(node)
  }

  @Test
  fun landmarkRolesAreLandmarks() {
    for (role in
      listOf("banner", "complementary", "contentInfo", "form", "main", "navigation", "region", "search")) {
      assertTrue(role, AccessibilityNodeInfoUtils.isWebLandmark(webNode(role)))
    }
  }

  @Test
  fun otherNodesAreNotLandmarks() {
    assertFalse(AccessibilityNodeInfoUtils.isWebLandmark(webNode("link")))
    assertFalse(AccessibilityNodeInfoUtils.isWebLandmark(webNode("genericContainer")))
    assertFalse(AccessibilityNodeInfoUtils.isWebLandmark(webNode(null)))
    assertFalse(AccessibilityNodeInfoUtils.isWebLandmark(null))
  }
}
