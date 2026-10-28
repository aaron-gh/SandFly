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

package com.google.android.accessibility.talkback.compositor.roledescription

import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WebPostDescriptionTest {
  private fun node(
    chromeRole: String? = null,
    focusable: Boolean = false,
    className: String? = null,
    text: String? = null,
  ): AccessibilityNodeInfoCompat {
    val node = AccessibilityNodeInfo()
    if (chromeRole != null) {
      node.extras.putCharSequence("AccessibilityNodeInfo.chromeRole", chromeRole)
    }
    node.isFocusable = focusable
    node.className = className
    node.text = text
    return AccessibilityNodeInfoCompat.wrap(node)
  }

  @Test
  fun focusableWebPostReadsItsLinks() {
    assertTrue(
      TreeNodesDescription.isFocusableWebContainer(node("genericContainer", focusable = true))
    )
    assertTrue(TreeNodesDescription.isFocusableWebContainer(node("article", focusable = true)))
  }

  @Test
  fun otherNodesDontReadTheirLinks() {
    assertFalse(TreeNodesDescription.isFocusableWebContainer(node("genericContainer")))
    assertFalse(TreeNodesDescription.isFocusableWebContainer(node("link", focusable = true)))
    assertFalse(TreeNodesDescription.isFocusableWebContainer(node(focusable = true)))
    assertFalse(
      TreeNodesDescription.isFocusableWebContainer(
        node("button", focusable = true, className = "android.widget.Button")
      )
    )
  }

  @Test
  fun whiteSpaceTextIsSkipped() {
    assertTrue(TreeNodesDescription.isWhiteSpaceText(node(text = " ")))
    assertTrue(TreeNodesDescription.isWhiteSpaceText(node(text = "\n  ")))
    assertFalse(TreeNodesDescription.isWhiteSpaceText(node(text = " a ")))
    assertFalse(TreeNodesDescription.isWhiteSpaceText(node(text = "")))
    assertFalse(TreeNodesDescription.isWhiteSpaceText(node()))
  }
}
