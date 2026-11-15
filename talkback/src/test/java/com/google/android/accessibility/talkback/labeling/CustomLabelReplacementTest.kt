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

import android.view.View
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CustomLabelReplacementTest {
  /** Labels each view in [labels], by resource name. */
  private class Labels(private val labels: Map<String, String>) : StoragelessLabelManager() {
    override fun getLabelForViewIdFromCache(resourceName: String?): Label? =
      labels[resourceName]?.let {
        Label("com.example", null, resourceName!!.substringAfter(":id/"), it, "en", 1, null, 0)
      }
  }

  private val send = "com.example:id/send"
  private val message = "com.example:id/message"
  private val title = "android:id/title"
  private val row = "com.example:id/row"
  private val labels =
    Labels(mapOf(send to "Send now", message to "Message", title to "Name", row to "Chat"))

  private fun info(
    id: String?,
    text: String? = null,
    description: String? = null,
    editable: Boolean = false,
  ): AccessibilityNodeInfo =
    AccessibilityNodeInfo().apply {
      // A source, so that nodes can be told apart.
      setSource(View(RuntimeEnvironment.getApplication()))
      className = if (editable) "android.widget.EditText" else "android.widget.Button"
      viewIdResourceName = id
      this.text = text
      contentDescription = description
      isEditable = editable
      isClickable = true
      isEnabled = true
    }

  /** Sealed, as nodes from apps are, so that the items with its resource ID can be asked for. */
  private fun sealed(info: AccessibilityNodeInfo): AccessibilityNodeInfoCompat {
    ReflectionHelpers.callInstanceMethod<Any>(
      info,
      "setSealed",
      ReflectionHelpers.ClassParameter.from(Boolean::class.javaPrimitiveType, true),
    )
    return AccessibilityNodeInfoCompat.wrap(info)
  }

  private fun node(
    id: String?,
    text: String? = null,
    description: String? = null,
    editable: Boolean = false,
  ): AccessibilityNodeInfoCompat = sealed(info(id, text, description, editable))

  /** A node whose window has [count] items with its resource ID. */
  @Suppress("DEPRECATION")
  private fun repeated(id: String, text: String, count: Int): AccessibilityNodeInfoCompat {
    val info = info(id, text)
    return object : AccessibilityNodeInfoCompat(info) {
      override fun findAccessibilityNodeInfosByViewId(viewId: String) =
        List(count) { AccessibilityNodeInfoCompat.wrap(info) }
    }
  }

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
    assertTrue(TalkBackLabelManager.canReplaceText(node(send, text = "Send")))
    assertFalse(TalkBackLabelManager.canReplaceText(node(message, text = "Hello", editable = true)))
    assertFalse(TalkBackLabelManager.canReplaceText(node(title, text = "Wi-Fi")))
    assertFalse(TalkBackLabelManager.canReplaceText(node(null, text = "Send")))
  }

  @Test
  fun aLabelOnAnAndroidIdOnlyFillsIn() {
    assertEquals("Wi-Fi", TalkBackLabelManager.getNodeText(node(title, text = "Wi-Fi"), labels).toString())
    assertEquals("", AccessibilityNodeFeedbackUtils.getReplacingLabelText(node(title, text = "Wi-Fi"), ImageContents(labels, null)))
    assertEquals("Name", TalkBackLabelManager.getNodeText(node(title), labels))
  }

  @Test
  fun aLabelOfAListItemOrOfAnItemInOneOnlyFillsIn() {
    val item =
      info(row, text = "Anna").apply {
        collectionItemInfo = AccessibilityNodeInfo.CollectionItemInfo(0, 1, 0, 1, false)
      }
    assertFalse(TalkBackLabelManager.canReplaceText(sealed(item)))
    assertEquals("Anna", TalkBackLabelManager.getNodeText(AccessibilityNodeInfoCompat.wrap(item), labels).toString())

    val parent = AccessibilityNodeInfo().apply {
      setSource(View(RuntimeEnvironment.getApplication()))
      collectionItemInfo = AccessibilityNodeInfo.CollectionItemInfo(0, 1, 0, 1, false)
    }
    val child = info(send, text = "Send")
    shadowOf(parent).addChild(child)
    assertFalse(TalkBackLabelManager.canReplaceText(sealed(child)))
    assertEquals("Send", TalkBackLabelManager.getNodeText(AccessibilityNodeInfoCompat.wrap(child), labels).toString())
  }

  @Test
  fun aLabelOfAResourceIdSharedInTheWindowOnlyFillsIn() {
    assertFalse(TalkBackLabelManager.canReplaceText(repeated(row, "Anna", 2)))
    assertEquals("Anna", TalkBackLabelManager.getNodeText(repeated(row, "Anna", 2), labels).toString())
    assertEquals(
      "",
      AccessibilityNodeFeedbackUtils.getReplacingLabelText(repeated(row, "Anna", 2), ImageContents(labels, null)),
    )
    assertTrue(TalkBackLabelManager.canReplaceText(repeated(row, "Anna", 1)))
    assertEquals("Chat", TalkBackLabelManager.getNodeText(repeated(row, "Anna", 1), labels))
  }

  @Test
  fun aLabelledControlIsNotUnlabelled() {
    assertFalse(labels.stateReader().needsLabel(node(send, text = "Send")))
    assertTrue(labels.stateReader().needsLabel(node(send)))
  }
}
