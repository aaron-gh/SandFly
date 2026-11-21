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

package com.google.android.accessibility.talkback.keyboard

import com.google.android.accessibility.talkback.keyboard.CharacterChooser.Item
import com.google.android.accessibility.talkback.keyboard.CharacterChooser.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Finding characters by words of their names. */
class CharacterChooserTest {
  private val items =
    listOf(
      Item("😀", "grinning face"),
      Item("😂", "face with tears of joy"),
      Item("👍", "thumbs up sign"),
      Item("❤", "heavy black heart"),
    )

  @Test
  fun everyWordStartsAWordOfTheName() {
    assertTrue(CharacterChooser.matches("face with tears of joy", "tea fa"))
    assertTrue(CharacterChooser.matches("thumbs up sign", "THUMB"))
    assertTrue(CharacterChooser.matches("anything", "  "))
    assertFalse(CharacterChooser.matches("grinning face", "ace"))
    assertFalse(CharacterChooser.matches("grinning face", "grin heart"))
  }

  @Test
  fun typingFiltersAndArrowsWrap() {
    val chooser = CharacterChooser(Kind.EMOJI, items, recent = emptyList())
    assertEquals(4, chooser.matches.size)
    "face".forEach(chooser::type)
    assertEquals(listOf("😀", "😂"), chooser.matches.map { it.text })
    chooser.previous()
    assertEquals("😂", chooser.current?.text)
    chooser.next()
    assertEquals("😀", chooser.current?.text)
    "x".forEach(chooser::type)
    assertNull(chooser.current)
    assertTrue(chooser.deleteLast())
    assertEquals(2, chooser.matches.size)
  }

  @Test
  fun recentComeFirst() {
    val chooser = CharacterChooser(Kind.EMOJI, items, recent = listOf(Item("❤", "heavy black heart")))
    assertEquals(listOf("❤", "😀", "😂", "👍"), chooser.matches.map { it.text })
  }

  @Test
  fun searchFindsOnceSomethingIsTyped() {
    val chooser =
      CharacterChooser(Kind.EMOJI, emptyList(), recent = listOf(Item("👍", "thumbs up"))) { query ->
        items.filter { CharacterChooser.matches(it.name, query) }
      }
    assertEquals(listOf("👍"), chooser.matches.map { it.text })
    "face".forEach(chooser::type)
    assertEquals(listOf("😀", "😂"), chooser.matches.map { it.text })
    "thumb".forEach { _ -> chooser.deleteLast() }
    "thu".forEach(chooser::type)
    assertEquals(listOf("👍"), chooser.matches.map { it.text })
  }

  @Test
  fun deleteWithNothingTypedClosesNothing() {
    assertFalse(CharacterChooser(Kind.SYMBOL, items, recent = emptyList()).deleteLast())
  }
}
