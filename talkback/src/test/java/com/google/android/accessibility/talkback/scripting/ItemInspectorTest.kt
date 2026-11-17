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

package com.google.android.accessibility.talkback.scripting

import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.ViewCompat
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ItemInspectorTest {
  private val context: Context = RuntimeEnvironment.getApplication()

  private fun node(build: AccessibilityNodeInfo.() -> Unit): AccessibilityNodeInfo =
    AccessibilityNodeInfo.obtain().apply {
      isVisibleToUser = true
      isEnabled = true
      build()
    }

  @Test
  fun buttonGivesMacroTextsAndTypesUnderHeadings() {
    val button = node {
      contentDescription = "New post"
      isClickable = true
    }
    val icon = node { text = "Plus" }
    shadowOf(button).addChild(icon)

    val text = ItemInspector.plainText(context, ItemInspector.report(context, AccessibilityNodeInfoCompat.wrap(button)))

    assertTrue(text, text.startsWith("Texts\ndescription: New post\n\nItem types\nClickable\nEnabled\n\nItems inside\nlevel 1: text: Plus\n\nScript details\n"))
    assertTrue(text, text.contains("kind: clickable, enabled"))
    assertFalse(text, text.contains("\ntext: New post"))
  }

  @Test
  fun headingGivesHeadingTypeHintAndState() {
    val heading = node {
      text = "New post"
      isHeading = true
      hintText = "Title"
      stateDescription = "Draft"
      viewIdResourceName = "com.example:id/title"
    }

    val text = ItemInspector.plainText(context, ItemInspector.report(context, AccessibilityNodeInfoCompat.wrap(heading)))

    assertTrue(text, text.startsWith("Texts\ntext: New post\nhint: Title\nstate: Draft\n\nItem types\nHeading\nEnabled\n\nScript details\nid: com.example:id/title"))
    assertFalse(text, text.contains("Items inside"))
  }

  @Test
  fun showOpensWindowWithHeadingsAndCopyCopiesAll() {
    val button = node {
      contentDescription = "New post"
      isClickable = true
    }
    val compat = AccessibilityNodeInfoCompat.wrap(button)

    assertNull(ItemInspector.show(context, compat))
    val intent = shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity
    val activity = Robolectric.buildActivity(ItemDetailsActivity::class.java, intent).setup().get()
    val headings = mutableListOf<String>()
    val lines = mutableListOf<String>()
    fun walk(view: View) {
      if (view is ViewGroup) for (i in 0 until view.childCount) walk(view.getChildAt(i))
      else if (view is TextView && view !is Button) {
        if (ViewCompat.isAccessibilityHeading(view)) headings.add(view.text.toString()) else lines.add(view.text.toString())
      }
    }
    walk(activity.window.decorView)
    assertEquals(listOf("Item details", "Texts", "Item types", "Script details"), headings.take(4))
    assertTrue(lines.toString(), lines.contains("description: New post"))

    assertEquals("Copied item details", ItemInspector.copy(context, compat))
    val clip = context.getSystemService(ClipboardManager::class.java).primaryClip!!.getItemAt(0).text.toString()
    assertTrue(clip, clip.startsWith("Texts\ndescription: New post"))
    assertEquals("No item has focus", ItemInspector.copy(context, null))
  }
}
