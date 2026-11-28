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
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor
import com.google.android.accessibility.utils.input.CursorGranularity
import com.google.android.accessibility.utils.output.CopySelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Lines in text SandFly moves through itself, such as a Telegram message (#682). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LineGranularityTest {
  private fun node(nodeText: String?) =
    AccessibilityNodeInfoCompat.wrap(AccessibilityNodeInfo.obtain()).apply {
      className = "android.view.ViewGroup"
      packageName = "org.telegram.messenger"
      text = nodeText
      // Focused, so that locking navigation to it clears no selection.
      isFocused = true
    }

  private val message = "Title of the link\nhttps://example.com/page\nSent 11:15"

  @Test
  fun appDrawnTextIsOfferedLines() {
    assertTrue(
      CursorGranularityManager.getSupportedGranularities(node(message))
        .contains(CursorGranularity.LINE)
    )
  }

  @Test
  fun movingByLineStopsAtEachLineBreak() {
    val item = node(message)
    val traversal = GranularityTraversal(null, CopySelection(), null)
    val line = CursorGranularity.LINE.value
    assertTrue(traversal.traverseAtGranularity(item, line, true, null))
    assertEquals(17, traversal.getCursorPosition(item))
    assertTrue(traversal.traverseAtGranularity(item, line, true, null))
    assertEquals(42, traversal.getCursorPosition(item))
    assertTrue(traversal.traverseAtGranularity(item, line, true, null))
    assertEquals(message.length, traversal.getCursorPosition(item))
    assertFalse(traversal.traverseAtGranularity(item, line, true, null))
    assertTrue(traversal.traverseAtGranularity(item, line, false, null))
    assertEquals(43, traversal.getCursorPosition(item))
  }

  @Test
  fun aGranularityTheItemDoesNotSupportIsStillSaved() {
    val noKeyboard =
      object : AccessibilityFocusMonitor(null, null, null) {
        override fun getEditingNodeFromFocusedKeyboard(node: AccessibilityNodeInfoCompat?): AccessibilityNodeInfoCompat? = null
      }
    val manager = CursorGranularityManager(null, noKeyboard, null, RuntimeEnvironment.getApplication())
    assertFalse(manager.setGranularityAt(node(null), CursorGranularity.LINE, null))
    assertEquals(CursorGranularity.LINE, manager.savedGranularity)
  }
}
