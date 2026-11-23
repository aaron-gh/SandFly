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

package com.google.android.accessibility.talkback.dialog

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenTextTest {

  @Test
  fun mergeLeavesOutItemsKeptOnScreen() {
    val texts = mutableListOf("A", "B", "C", "D")
    val added = ScreenText.merge(texts, listOf("C", "D", "E", "F"))
    assertEquals(listOf("A", "B", "C", "D", "E", "F"), texts)
    assertEquals(2, added)
  }

  @Test
  fun mergeKeepsRepeatedTextsPastTheOverlap() {
    val texts = mutableListOf("Post 1", "Like", "Post 2", "Like")
    ScreenText.merge(texts, listOf("Post 2", "Like", "Post 3", "Like"))
    assertEquals(listOf("Post 1", "Like", "Post 2", "Like", "Post 3", "Like"), texts)
  }

  @Test
  fun mergeAddsAPageWithoutOverlap() {
    val texts = mutableListOf("A", "B")
    ScreenText.merge(texts, listOf("C", "D"))
    assertEquals(listOf("A", "B", "C", "D"), texts)
  }

  @Test
  fun mergeLeavesOutRecentTextsWhenAnEdgeItemChanged() {
    // "D" was cut off at the bottom edge and reads "D more" once fully on screen.
    val texts = mutableListOf("A", "B", "C", "D")
    ScreenText.merge(texts, listOf("C", "D more", "E"))
    assertEquals(listOf("A", "B", "C", "D", "D more", "E"), texts)
  }

  @Test
  fun mergeIntoEmptyAddsEverything() {
    val texts = mutableListOf<String>()
    ScreenText.merge(texts, listOf("A", "B"))
    assertEquals(listOf("A", "B"), texts)
  }
}
