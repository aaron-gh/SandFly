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
import com.google.android.accessibility.utils.output.CopySelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SentenceGranularityTest {
  private fun node(
    className: String,
    nodeText: String?,
    configure: AccessibilityNodeInfoCompat.() -> Unit = {},
  ) =
    AccessibilityNodeInfoCompat.wrap(AccessibilityNodeInfo.obtain()).apply {
      this.className = className
      packageName = "example.app"
      text = nodeText
      configure()
    }

  private fun traversal() = GranularityTraversal(null, CopySelection(), null)

  private val cycle =
    listOf(
      CursorGranularity.LINE,
      CursorGranularity.SENTENCE,
      CursorGranularity.PARAGRAPH,
      CursorGranularity.DEFAULT,
    )

  private val editor: AccessibilityNodeInfoCompat.() -> Unit = {
    isEditable = true
    isFocused = true
  }

  @Test
  fun textIsOfferedSentences() {
    assertTrue(GranularityTraversal.supportsSentenceTraversal(node("android.widget.TextView", "One.")))
  }

  @Test
  fun aFocusedEditTextIsOfferedSentences() {
    assertTrue(
      GranularityTraversal.supportsSentenceTraversal(node("android.widget.EditText", "One.", editor))
    )
  }

  @Test
  fun aFocusedEditorThatIsNotAnEditTextIsNotOfferedSentences() {
    val custom = node("android.view.View", "One. Two.", editor)
    assertFalse(GranularityTraversal.supportsSentenceTraversal(custom))
    val traversal = traversal()
    assertFalse(traversal.traverseAtSentenceGranularity(custom, true, null))
    assertEquals(-1, traversal.getCursorPosition(custom))
  }

  @Test
  fun anEditorWithoutFocusIsOfferedSentences() {
    val custom = node("android.view.View", "One.") { isEditable = true }
    assertTrue(GranularityTraversal.supportsSentenceTraversal(custom))
  }

  @Test
  fun anItemWithoutTextIsNotOfferedSentences() {
    assertFalse(GranularityTraversal.supportsSentenceTraversal(node("android.widget.TextView", null)))
  }

  @Test
  fun movingBySentenceKeepsThePosition() {
    val item = node("android.widget.TextView", "One. Two.")
    val traversal = traversal()
    assertTrue(traversal.traverseAtSentenceGranularity(item, true, null))
    assertEquals(4, traversal.getCursorPosition(item))
    assertTrue(traversal.traverseAtSentenceGranularity(item, true, null))
    assertEquals(9, traversal.getCursorPosition(item))
    assertFalse(traversal.traverseAtSentenceGranularity(item, true, null))
  }

  @Test
  fun movingOnAfterATextChangeContinuesFromTheRestoredPosition() {
    val traversal = traversal()
    val changed = node("android.widget.TextView", "One. Two. Three.")
    traversal.restoreSentencePosition(changed, 4)
    assertTrue(traversal.traverseAtSentenceGranularity(changed, true, null))
    assertEquals(9, traversal.getCursorPosition(changed))
  }

  @Test
  fun aRestoredPositionIsClampedToTheNewText() {
    val traversal = traversal()
    val shorter = node("android.widget.TextView", "One.")
    traversal.restoreSentencePosition(shorter, 100)
    assertEquals(4, traversal.getCursorPosition(shorter))
    assertFalse(traversal.traverseAtSentenceGranularity(shorter, true, null))
    val empty = node("android.widget.TextView", null)
    traversal.restoreSentencePosition(empty, 100)
    assertEquals(0, traversal.getCursorPosition(empty))
  }

  @Test
  fun adjustingGranularitySkipsSentencesWhenNotOffered() {
    assertEquals(2, CursorGranularityManager.nextGranularityIndex(cycle, 0, 1, false))
    assertEquals(0, CursorGranularityManager.nextGranularityIndex(cycle, 2, -1, false))
  }

  @Test
  fun adjustingGranularityIncludesSentencesWhenOffered() {
    assertEquals(1, CursorGranularityManager.nextGranularityIndex(cycle, 0, 1, true))
    assertEquals(1, CursorGranularityManager.nextGranularityIndex(cycle, 2, -1, true))
  }

  @Test
  fun adjustingGranularityWrapsAround() {
    assertEquals(0, CursorGranularityManager.nextGranularityIndex(cycle, 3, 1, false))
    assertEquals(3, CursorGranularityManager.nextGranularityIndex(cycle, 0, -1, false))
    assertEquals(0, CursorGranularityManager.nextGranularityIndex(cycle, -1, 1, false))
  }

  @Test
  fun leavingSentencesWhenNotOfferedMovesToAnotherGranularity() {
    assertEquals(2, CursorGranularityManager.nextGranularityIndex(cycle, 1, 1, false))
    assertEquals(0, CursorGranularityManager.nextGranularityIndex(cycle, 1, -1, false))
  }
}
