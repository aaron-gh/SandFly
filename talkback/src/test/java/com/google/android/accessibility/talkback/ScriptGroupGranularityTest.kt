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

package com.google.android.accessibility.talkback

import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.input.CursorGranularity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScriptGroupGranularityTest {
  private fun node(
    className: String,
    nodeText: String? = null,
    configure: AccessibilityNodeInfoCompat.() -> Unit = {},
  ) =
    AccessibilityNodeInfoCompat.wrap(AccessibilityNodeInfo.obtain()).apply {
      this.className = className
      packageName = "example.app"
      text = nodeText
      isVisibleToUser = true
      configure()
    }

  private val row = node("android.view.ViewGroup") { isFocusable = true }
  private val title = node("android.widget.TextView", "Video title") { isClickable = true }
  private val duration = node("android.widget.TextView", "6 minutes")
  private val channel = node("android.widget.Button", "Go to channel") { isClickable = true }

  private fun extract(
    root: AccessibilityNodeInfoCompat,
    items: List<AccessibilityNodeInfoCompat>?,
  ): Pair<List<AccessibilityNodeInfoCompat>, Int> {
    val nodes = mutableListOf<AccessibilityNodeInfoCompat>()
    val mask =
      CursorGranularityManager.extractNavigableNodes(root, nodes, HashSet()) {
        if (it == root) items else null
      }
    return nodes to mask
  }

  @Test
  fun aGroupedItemIsMovedThroughFromTheFirstItemItReads() {
    val (nodes, mask) = extract(row, listOf(title, duration, channel))

    assertEquals(listOf(title, duration, channel), nodes)
    assertTrue(mask and CursorGranularity.WORD.value != 0)
    assertTrue(mask and CursorGranularity.CHARACTER.value != 0)
  }

  @Test
  fun aGroupedItemWithItsOwnTextIsMovedThroughByItAlone() {
    val named = node("android.view.ViewGroup", "Named row") { isFocusable = true }

    val (nodes, _) = extract(named, listOf(title, duration))

    assertEquals(listOf(named), nodes)
  }

  @Test
  fun anItemInsideTheGroupWithoutTextIsSkipped() {
    val image = node("android.widget.ImageView") { isClickable = true }

    val (nodes, _) = extract(row, listOf(image, title))

    assertEquals(listOf(title), nodes)
  }
}
