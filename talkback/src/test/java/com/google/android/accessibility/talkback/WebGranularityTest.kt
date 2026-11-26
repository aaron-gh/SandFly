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
import android.graphics.Rect
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.WebInterfaceUtils
import com.google.android.accessibility.utils.input.CursorGranularity
import com.google.android.accessibility.utils.output.CopySelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Web elements Chrome gives no movement granularities, such as an element named with aria-label,
 * are moved through by character and word by SandFly. Report 621.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WebGranularityTest {
  private val chromeGranularities =
    AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER or
      AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD or
      AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE or
      AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH

  private fun webNode(configure: AccessibilityNodeInfoCompat.() -> Unit) =
    AccessibilityNodeInfoCompat.wrap(AccessibilityNodeInfo.obtain()).apply {
      className = "android.view.View"
      packageName = "com.android.chrome"
      isVisibleToUser = true
      addAction(AccessibilityNodeInfoCompat.ACTION_NEXT_HTML_ELEMENT)
      addAction(AccessibilityNodeInfoCompat.ACTION_PREVIOUS_HTML_ELEMENT)
      configure()
    }

  /** A link named with aria-label: the name is the content description, with no granularities. */
  private fun labelledLink() = webNode {
    isClickable = true
    isFocusable = true
    contentDescription = "Phone case, 12 dollars"
  }

  @Test
  fun aWebElementWithoutGranularitiesIsMovedThroughBySandFly() {
    assertTrue(GranularityTraversal.shouldHandleGranularityTraversalInTalkback(labelledLink()))
  }

  @Test
  fun aWebElementWithoutGranularitiesOffersCharactersAndWords() {
    val link = labelledLink()
    val nodes = mutableListOf<AccessibilityNodeInfoCompat>()
    val mask = CursorGranularityManager.extractNavigableNodes(link, nodes, HashSet()) { null }

    assertEquals(listOf(link), nodes)
    assertTrue(mask and CursorGranularity.CHARACTER.value != 0)
    assertTrue(mask and CursorGranularity.WORD.value != 0)
  }

  @Test
  fun aWebElementWithoutGranularitiesIsMovedThroughByWord() {
    val link = labelledLink()
    val traversal = GranularityTraversal(null, CopySelection(), null)
    val word = CursorGranularity.WORD.value

    assertTrue(traversal.traverseAtGranularity(link, word, true, null))
    assertEquals(5, traversal.getCursorPosition(link))
    assertTrue(traversal.traverseAtGranularity(link, word, true, null))
    assertEquals(10, traversal.getCursorPosition(link))
  }

  @Test
  fun webTextWithGranularitiesIsLeftToTheBrowser() {
    val paragraph = webNode {
      text = "Rockstar showed the map"
      unwrap().isTextSelectable = true
      movementGranularities = chromeGranularities
    }
    assertFalse(GranularityTraversal.shouldHandleGranularityTraversalInTalkback(paragraph))
  }

  @Test
  fun selectableWebTextWithoutGranularitiesIsMovedThroughBySandFly() {
    val paragraph = webNode {
      text = "Rockstar showed the map"
      unwrap().isTextSelectable = true
    }
    assertTrue(GranularityTraversal.shouldHandleGranularityTraversalInTalkback(paragraph))
  }

  @Test
  fun anEditableWebElementIsLeftToTheBrowser() {
    val field = webNode {
      className = "android.widget.EditText"
      isEditable = true
      contentDescription = "Search"
    }
    assertFalse(GranularityTraversal.shouldHandleGranularityTraversalInTalkback(field))
  }

  @Test
  fun aWebElementWithoutTextIsNotMovedThrough() {
    assertFalse(GranularityTraversal.shouldHandleGranularityTraversalInTalkback(webNode {}))
  }

  /** A paragraph Chromium clips to a viewport from 200 to 1800, unclipped from top to bottom. */
  private fun paragraphAt(top: Int, bottom: Int, offscreen: Boolean = false) = webNode {
    text = "Rockstar showed the map"
    movementGranularities = chromeGranularities
    val shown = Rect(0, top.coerceIn(200, 1800), 1000, bottom.coerceIn(200, 1800))
    setBoundsInScreen(shown)
    extras.putInt("AccessibilityNodeInfo.unclippedTop", top)
    extras.putInt("AccessibilityNodeInfo.unclippedBottom", bottom)
    if (offscreen) {
      extras.putBoolean("AccessibilityNodeInfo.offscreen", true)
    }
  }

  @Test
  fun aParagraphInsideTheViewportIsNotOutsideIt() {
    assertFalse(WebInterfaceUtils.isOutsideWebViewport(paragraphAt(400, 600)))
  }

  @Test
  fun aParagraphCutOffAtTheBottomIsOutsideTheViewport() {
    assertTrue(WebInterfaceUtils.isOutsideWebViewport(paragraphAt(1700, 1900)))
  }

  @Test
  fun aParagraphBelowTheViewportIsOutsideIt() {
    assertTrue(WebInterfaceUtils.isOutsideWebViewport(paragraphAt(2400, 2600, offscreen = true)))
  }

  @Test
  fun aNodeWithoutChromiumBoundsIsNotOutsideTheViewport() {
    assertFalse(WebInterfaceUtils.isOutsideWebViewport(webNode { text = "Map" }))
  }
}
