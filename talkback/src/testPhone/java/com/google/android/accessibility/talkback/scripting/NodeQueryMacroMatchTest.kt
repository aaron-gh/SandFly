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

import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Queries match items by the same texts and item types as macro tests. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NodeQueryMacroMatchTest {
  private fun node(build: AccessibilityNodeInfo.() -> Unit = {}): AccessibilityNodeInfo =
    AccessibilityNodeInfo.obtain().apply {
      isVisibleToUser = true
      isEnabled = true
      build()
    }

  private fun query(json: String) = NodeQuery.of(JSONObject(json))

  private fun NodeQuery.matches(node: AccessibilityNodeInfo) =
    matches(AccessibilityNodeInfoCompat.wrap(node))

  @Test
  fun contains_looksInAnyTextOrOneField() {
    val field = node {
      hintText = "Search posts"
      stateDescription = "Collapsed"
    }

    assertTrue(query("""{"contains":"search"}""").matches(field))
    assertTrue(query("""{"contains":"collapsed"}""").matches(field))
    assertTrue(query("""{"contains":"posts","field":"hint"}""").matches(field))
    assertFalse(query("""{"contains":"posts","field":"text"}""").matches(field))
    assertTrue(query("""{"contains":"collapsed","field":"state"}""").matches(field))
  }

  @Test
  fun matches_findsARegularExpressionInAField() {
    val button = node { contentDescription = "New post" }
    val heading = node { contentDescription = "New post draft" }

    val exact = query("""{"matches":"^new post$","field":"contentDescription"}""")

    assertTrue(exact.matches(button))
    assertFalse(exact.matches(heading))
    assertTrue(query("""{"matches":"post\\b","field":"description"}""").matches(heading))
  }

  @Test
  fun idField_matchesWithAndWithoutPackage() {
    val send = node { viewIdResourceName = "com.example:id/send_button" }

    assertTrue(query("""{"matches":"^send_button$","field":"id"}""").matches(send))
    assertTrue(query("""{"contains":"example:id/send","field":"id"}""").matches(send))
    assertFalse(query("""{"contains":"send"}""").matches(send))
  }

  @Test
  fun hintAndStateDescription_matchExactly() {
    val item = node {
      hintText = "Title"
      stateDescription = "Draft"
    }

    assertTrue(query("""{"hint":"Title","stateDescription":"Draft"}""").matches(item))
    assertFalse(query("""{"hint":"Tit"}""").matches(item))
  }

  @Test
  fun kind_matchesAsMacroItemTypes() {
    val button = node { isClickable = true }
    val label = node { text = "New post" }
    shadowOf(button).addChild(label)
    val heading = node {
      text = "New post"
      isHeading = true
    }
    val slider = node {
      rangeInfo = AccessibilityNodeInfo.RangeInfo.obtain(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT, 0f, 10f, 5f)
    }

    // A text in a button counts as clickable, as in a macro test.
    val clickable = query("""{"contains":"new post","kind":"clickable"}""")
    assertTrue(clickable.matches(label))
    assertFalse(clickable.matches(heading))
    assertTrue(query("""{"contains":"new post","kind":["heading","enabled"]}""").matches(heading))
    assertTrue(query("""{"kind":"adjustable"}""").matches(slider))
    assertFalse(query("""{"kind":"longClickable"}""").matches(label))
  }

  @Test
  fun problems_areReported() {
    assertEquals(
      "field needs contains or matches",
      assertThrows(ScriptApiException::class.java) { query("""{"field":"text"}""") }.message,
    )
    assertTrue(
      assertThrows(ScriptApiException::class.java) { query("""{"contains":"a","field":"label"}""") }
        .message!!
        .startsWith("Unknown field label")
    )
    assertTrue(
      assertThrows(ScriptApiException::class.java) { query("""{"kind":"button"}""") }
        .message!!
        .startsWith("Unknown kind button")
    )
    assertTrue(
      assertThrows(ScriptApiException::class.java) { query("""{"matches":"(a+)+"}""") }
        .message!!
        .startsWith("matches isn't a usable regular expression")
    )
  }

  @Test
  fun manifest_reportsBadMatchFields() {
    val error =
      assertThrows(ManifestException::class.java) {
        ManifestParser(strict = true).rule(JSONObject("""{"match":{"matches":"("},"label":"x"}"""))
      }
    assertTrue(error.message, error.message!!.startsWith("matches isn't a usable regular expression"))
  }
}
