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

package com.google.android.accessibility.talkback.compositor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentItemsTest {
  private var now = 0L
  private val items = RecentItems<String>(maxItems = 3, maxAgeMs = 2_000L, clock = { now })

  @Test
  fun anItemIsForgottenAfterItsFirstRemoval() {
    items.add("a")
    assertTrue(items.contains("a"))
    assertTrue(items.remove("a"))
    assertFalse(items.remove("a"))
    assertTrue(items.isEmpty())
  }

  @Test
  fun containsDoesNotForget() {
    items.add("a")
    assertTrue(items.contains("a"))
    assertTrue(items.contains("a"))
    assertTrue(items.remove("a"))
  }

  @Test
  fun itemsExpire() {
    items.add("a")
    now = 1_500L
    items.add("b")
    now = 2_001L
    assertFalse(items.contains("a"))
    assertTrue(items.contains("b"))
    now = 3_501L
    assertTrue(items.isEmpty())
  }

  @Test
  fun theOldestIsForgottenWhenFull() {
    items.add("a")
    items.add("b")
    items.add("c")
    items.add("d")
    assertFalse(items.contains("a"))
    assertTrue(items.contains("b"))
    assertTrue(items.contains("d"))
  }

  @Test
  fun eachRemovalTakesOneEntry() {
    items.add("a")
    items.add("a")
    assertTrue(items.remove("a"))
    assertTrue(items.remove("a"))
    assertFalse(items.remove("a"))
  }

  @Test
  fun removeFirstTakesTheOldestMatch() {
    items.add("a1")
    items.add("b")
    items.add("a2")
    assertEquals("a1", items.removeFirst { it.startsWith("a") })
    assertEquals("a2", items.find { it.startsWith("a") })
    assertNull(items.removeFirst { it == "c" })
  }

  @Test
  fun removeAllAndClearForget() {
    items.add("a")
    items.add("b")
    items.add("a")
    items.removeAll { it == "a" }
    assertFalse(items.contains("a"))
    assertTrue(items.contains("b"))
    items.clear()
    assertTrue(items.isEmpty())
  }
}
