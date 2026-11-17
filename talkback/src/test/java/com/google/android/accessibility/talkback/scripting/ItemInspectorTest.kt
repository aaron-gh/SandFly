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

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
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
  fun buttonGivesMacroTextsAndTypes() {
    val button = node {
      contentDescription = "New post"
      isClickable = true
    }
    val icon = node { text = "Plus" }
    shadowOf(button).addChild(icon)

    val details = ItemInspector.inspect(context, AccessibilityNodeInfoCompat.wrap(button))

    assertTrue(details, details.contains("description: New post"))
    assertTrue(details, details.contains("item types: Clickable, Enabled"))
    assertTrue(details, details.contains("inside text: Plus"))
    assertFalse(details, details.contains("\ntext: "))
  }

  @Test
  fun headingGivesHeadingTypeHintAndState() {
    val heading = node {
      text = "New post"
      isHeading = true
      hintText = "Title"
      stateDescription = "Draft"
    }

    val details = ItemInspector.inspect(context, AccessibilityNodeInfoCompat.wrap(heading))

    assertTrue(details, details.contains("text: New post"))
    assertTrue(details, details.contains("hint: Title"))
    assertTrue(details, details.contains("state: Draft"))
    assertTrue(details, details.contains("item types: Heading, Enabled"))
  }
}
