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

package com.google.android.accessibility.talkback.labeling

import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.compositor.AccessibilityNodeFeedbackUtils
import com.google.android.accessibility.talkback.imagecaption.ImageContents
import com.google.android.accessibility.utils.labeling.Label
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CustomLabelReplacementTest {
  /** Labels each view in [labels], by resource name. */
  private class Labels(private val labels: Map<String, String>) : StoragelessLabelManager() {
    override fun getLabelForViewIdFromCache(resourceName: String?): Label? =
      labels[resourceName]?.let {
        Label("com.example", null, resourceName!!.substringAfter(":id/"), it, "en", 1, null, 0)
      }

    fun replaces(node: AccessibilityNodeInfoCompat) = canReplaceText(node)
  }

  private val send = "com.example:id/send"
  private val message = "com.example:id/message"
  private val labels = Labels(mapOf(send to "Send now", message to "Message"))

  private fun node(
    id: String?,
    text: String? = null,
    description: String? = null,
    editable: Boolean = false,
  ): AccessibilityNodeInfoCompat =
    AccessibilityNodeInfoCompat.wrap(
      AccessibilityNodeInfo().apply {
        className = if (editable) "android.widget.EditText" else "android.widget.Button"
        viewIdResourceName = id
        this.text = text
        contentDescription = description
        isEditable = editable
        isClickable = true
        isEnabled = true
      }
    )

  @Test
  fun aLabelReplacesTheTextAndDescriptionOfAControl() {
    assertEquals("Send now", TalkBackLabelManager.getNodeText(node(send, text = "Send"), labels))
    assertEquals(
      "Send now",
      TalkBackLabelManager.getNodeText(node(send, description = "Send message"), labels),
    )
    assertEquals(
      "Send now",
      AccessibilityNodeFeedbackUtils.getReplacingLabelText(
        node(send, description = "Send message"),
        ImageContents(labels, null),
      ),
    )
  }

  @Test
  fun anEditFieldKeepsItsText() {
    val field = node(message, text = "Hello", editable = true)
    assertEquals("Hello", TalkBackLabelManager.getNodeText(field, labels).toString())
    assertEquals(
      "",
      AccessibilityNodeFeedbackUtils.getReplacingLabelText(field, ImageContents(labels, null)),
    )
    assertEquals("Message", TalkBackLabelManager.getNodeText(node(message, editable = true), labels))
  }

  @Test
  fun controlsWithoutALabelKeepTheirText() {
    val other = node("com.example:id/other", text = "Other")
    assertEquals("Other", TalkBackLabelManager.getNodeText(other, labels).toString())
    assertEquals(
      "",
      AccessibilityNodeFeedbackUtils.getReplacingLabelText(other, ImageContents(labels, null)),
    )
  }

  @Test
  fun labelledControlsCanBeRelabelledButNotEditFieldsOrAndroidIds() {
    assertTrue(labels.replaces(node(send, text = "Send")))
    assertFalse(labels.replaces(node(message, text = "Hello", editable = true)))
    assertFalse(labels.replaces(node("android:id/title", text = "Wi-Fi")))
    assertFalse(labels.replaces(node(null, text = "Send")))
  }

  @Test
  fun aLabelledControlIsNotUnlabelled() {
    assertFalse(labels.stateReader().needsLabel(node(send, text = "Send")))
    assertTrue(labels.stateReader().needsLabel(node(send)))
  }
}
