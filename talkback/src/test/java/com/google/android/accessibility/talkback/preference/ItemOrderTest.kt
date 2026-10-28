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

package com.google.android.accessibility.talkback.preference

import org.junit.Assert.assertEquals
import org.junit.Test

class ItemOrderTest {
  private val defaults = listOf("a", "b", "c", "d")

  @Test
  fun noSavedOrderKeepsTheDefaults() {
    assertEquals(defaults, ItemOrder.ordered(emptyList(), defaults))
  }

  @Test
  fun savedOrderComesFirst() {
    assertEquals(listOf("d", "c", "b", "a"), ItemOrder.ordered(listOf("d", "c", "b", "a"), defaults))
  }

  @Test
  fun newKeysGoAfterTheKeyTheyFollowByDefault() {
    assertEquals(listOf("c", "d", "a", "b"), ItemOrder.ordered(listOf("c", "a", "b"), defaults))
    assertEquals(listOf("a", "c", "b", "d"), ItemOrder.ordered(listOf("c", "b", "d"), defaults))
  }

  @Test
  fun unknownAndRepeatedKeysAreDropped() {
    assertEquals(defaults, ItemOrder.ordered(listOf("a", "x", "b", "a", "c", "d"), defaults))
  }

  @Test
  fun storedSplitsOnCommas() {
    assertEquals(listOf("a", "b"), ItemOrder.stored("a,,b"))
    assertEquals(emptyList<String>(), ItemOrder.stored(null))
  }
}
