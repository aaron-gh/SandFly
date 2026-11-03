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

package com.google.android.accessibility.talkback.preference.search

import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import com.google.android.accessibility.talkback.preference.search.SettingsSearch.Candidate
import com.google.android.accessibility.talkback.preference.search.SettingsSearch.EditorAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchTest {

  @Test
  fun normalizeRemovesCaseAccentsAndExtraSpaces() {
    assertEquals("acao rapida", SettingsSearch.normalize("  Ação \t RÁPIDA "))
    assertEquals("istanbul", SettingsSearch.normalize("İstanbul"))
    assertEquals("fi", SettingsSearch.normalize("ﬁ"))
    assertEquals("", SettingsSearch.normalize(null))
  }

  @Test
  fun wordsAreNormalizedAndSplitOnSpaces() {
    assertEquals(listOf("velocidade", "da", "fala"), SettingsSearch.words("  Velocidade da   FALA "))
    assertEquals(emptyList<String>(), SettingsSearch.words("   "))
  }

  @Test
  fun everyWordMustBeSomewhereInAnyOrder() {
    val texts = listOf<CharSequence?>("Velocidade da fala", null, "Voz")
    assertTrue(SettingsSearch.matches(SettingsSearch.words("fala VELOC"), texts))
    assertTrue(SettingsSearch.matches(SettingsSearch.words("vóz"), texts))
    assertTrue(SettingsSearch.matches(SettingsSearch.words("cidade"), texts))
    assertFalse(SettingsSearch.matches(SettingsSearch.words("fala volume"), texts))
    assertFalse(SettingsSearch.matches(emptyList(), texts))
  }

  @Test
  fun matchesInNamesComeFirst() {
    val candidates =
      listOf(
        Candidate("Swipe up then right", listOf("Read from top")),
        Candidate("Read from next item", listOf("Reading")),
        Candidate("Read from top", listOf("Reading")),
        Candidate("Speech rate"),
      )
    assertEquals(listOf(2, 0), SettingsSearch.find(candidates, SettingsSearch.words("top read")))
    assertEquals(listOf(1, 2), SettingsSearch.find(candidates, SettingsSearch.words("read from")).take(2))
    assertEquals(emptyList<Int>(), SettingsSearch.find(candidates, SettingsSearch.words("volume")))
  }

  @Test
  fun labelShowsDetailsOnlyWhenTheNameDoesNotMatch() {
    val candidate = Candidate("Swipe up then right", listOf("Read from top"), "Gestures > One finger")
    assertEquals(
      "Swipe up then right\nGestures > One finger",
      SettingsSearch.label(candidate, SettingsSearch.words("swipe up")),
    )
    assertEquals(
      "Swipe up then right\nRead from top\nGestures > One finger",
      SettingsSearch.label(candidate, SettingsSearch.words("top")),
    )
    val shown = Candidate("Read from top", listOf("Alt + Enter"), alwaysShowDetails = true)
    assertEquals("Read from top\nAlt + Enter", SettingsSearch.label(shown, SettingsSearch.words("top")))
  }

  @Test
  fun longDetailsAreShortened() {
    val candidate = Candidate("Name", listOf("x".repeat(300)))
    val label = SettingsSearch.label(candidate, SettingsSearch.words("xxx")).toString()
    assertTrue(label.length < 140)
    assertTrue(label.endsWith("…"))
  }

  @Test
  fun pathJoinsTitles() {
    assertEquals("Speech > Stopping speech", SettingsSearch.path(listOf("Speech", "Stopping speech")))
  }

  @Test
  fun keyboardSearchDoneAndGoActionsSearch() {
    for (action in listOf(EditorInfo.IME_ACTION_SEARCH, EditorInfo.IME_ACTION_DONE, EditorInfo.IME_ACTION_GO)) {
      assertEquals(EditorAction.SEARCH, SettingsSearch.editorAction(action, null, 0))
    }
    assertEquals(EditorAction.IGNORE, SettingsSearch.editorAction(EditorInfo.IME_ACTION_NEXT, null, 0))
  }

  @Test
  fun physicalEnterSearchesOnPressAndIsConsumedOnRelease() {
    assertEquals(
      EditorAction.SEARCH,
      SettingsSearch.editorAction(EditorInfo.IME_NULL, KeyEvent.ACTION_DOWN, 0),
    )
    assertEquals(
      EditorAction.CONSUME,
      SettingsSearch.editorAction(EditorInfo.IME_NULL, KeyEvent.ACTION_DOWN, 1),
    )
    assertEquals(
      EditorAction.CONSUME,
      SettingsSearch.editorAction(EditorInfo.IME_NULL, KeyEvent.ACTION_UP, 0),
    )
    assertEquals(EditorAction.IGNORE, SettingsSearch.editorAction(EditorInfo.IME_NULL, null, 0))
  }

  @Test
  fun emptyQueryFindsNothing() {
    assertEquals(emptyList<Int>(), SettingsSearch.find(listOf(Candidate("Speech")), SettingsSearch.words("  ")))
  }
}
