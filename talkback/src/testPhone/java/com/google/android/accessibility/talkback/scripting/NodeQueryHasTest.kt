/*
 * Copyright 2026 Backtalk contributors
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NodeQueryHasTest {
  @Test
  fun has_matchesAnItemInsideIt() {
    val post = node("ViewGroup")
    val header = node("ViewGroup")
    val subscribe = node("Button", description = "Subscribe to asteria.")
    add(post, header)
    add(header, subscribe)

    val query = NodeQuery.of(JSONObject("""{"className":"ViewGroup","has":{"textContains":"Subscribe to"}}"""))

    assertTrue(query.matches(AccessibilityNodeInfoCompat.wrap(post)))
    assertFalse(query.matches(AccessibilityNodeInfoCompat.wrap(subscribe)))
  }

  @Test
  fun has_doesNotMatchItself() {
    val button = node("Button", description = "Subscribe to asteria.")
    val query = NodeQuery.of(JSONObject("""{"has":{"textContains":"Subscribe to"}}"""))
    assertFalse(query.matches(AccessibilityNodeInfoCompat.wrap(button)))
  }

  @Test
  fun has_stopsAfterSixLevels() {
    val root = node("ViewGroup")
    var parent = root
    repeat(7) {
      val child = node("ViewGroup")
      add(parent, child)
      parent = child
    }
    add(parent, node("Button", description = "Like"))
    val query = NodeQuery.of(JSONObject("""{"has":{"contentDescription":"Like"}}"""))
    assertFalse(query.matches(AccessibilityNodeInfoCompat.wrap(root)))
  }

  @Test
  fun has_cannotNest() {
    val query =
      NodeQuery.of(JSONObject("""{"has":{"has":{"text":"x"},"inside":{"text":"y"},"text":"z"}}"""))
    assertNull(query.has?.has)
    assertNull(query.has?.inside)
  }

  private fun node(className: String, description: String? = null): AccessibilityNodeInfo =
    AccessibilityNodeInfo.obtain().apply {
      this.className = className
      contentDescription = description
      isVisibleToUser = true
    }

  private fun add(parent: AccessibilityNodeInfo, child: AccessibilityNodeInfo) {
    shadowOf(parent).addChild(child)
  }
}
