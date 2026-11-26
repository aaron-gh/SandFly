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

package com.google.android.accessibility.talkback.gesture

import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.utils.input.CursorGranularity
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationGestureActionsTest {
  private val actions = NavigationGestureActions.CATEGORIES.flatMap { it.actions }

  @Test
  fun valuesAreUnique() {
    assertEquals(actions.size, actions.map { it.value }.toSet().size)
  }

  @Test
  fun valuesDoNotReuseTalkBackShortcutValues() {
    // Gesture preferences store the value, so sharing one with a TalkBack action would run both.
    val xml = File("src/main/res/values/donottranslate.xml").readText()
    val talkBackValues =
      Regex("""<string name="shortcut_value_[^"]*"[^>]*>([^<]*)</string>""")
        .findAll(xml)
        .map { it.groupValues[1] }
        .toSet()
    assertTrue(talkBackValues.contains("NEXT"))
    assertEquals(emptySet<String>(), actions.map { it.value }.toSet() intersect talkBackValues)
  }

  @Test
  fun eachTargetHasPreviousThenNext() {
    for (category in NavigationGestureActions.CATEGORIES) {
      assertTrue(category.actions.isNotEmpty())
      for ((previous, next) in category.actions.chunked(2)) {
        assertFalse(previous.forward)
        assertTrue(next.forward)
        assertEquals(previous.target, next.target)
        assertEquals(previous.value.removePrefix("PREVIOUS_"), next.value.removePrefix("NEXT_"))
      }
    }
  }

  @Test
  fun findsActionsByValue() {
    assertSame(actions.first { it.value == "NEXT_HEADING" }, NavigationGestureActions.find("NEXT_HEADING"))
    assertTrue(NavigationGestureActions.isAction("PREVIOUS_LINK"))
    assertFalse(NavigationGestureActions.isAction("NEXT"))
    assertFalse(NavigationGestureActions.isAction(null))
    assertNull(NavigationGestureActions.find("UNASSIGNED"))
  }

  @Test
  fun sentencesMoveBySentenceBetweenLinesAndParagraphs() {
    val textValues = NavigationGestureActions.CATEGORIES[0].actions.map { it.value }
    assertEquals(
      listOf("PREVIOUS_LINE", "NEXT_LINE", "PREVIOUS_SENTENCE", "NEXT_SENTENCE", "PREVIOUS_PARAGRAPH"),
      textValues.subList(textValues.indexOf("PREVIOUS_LINE"), textValues.indexOf("PREVIOUS_PARAGRAPH") + 1),
    )
    for (value in listOf("PREVIOUS_SENTENCE", "NEXT_SENTENCE")) {
      assertEquals(
        NavigationGestureActions.Target.Text(CursorGranularity.SENTENCE),
        NavigationGestureActions.find(value)!!.target,
      )
    }
  }

  @Test
  fun sentencesFailInSelectionMode() {
    val noFeedback = Pipeline.FeedbackReturner { error("No feedback expected") }
    for (value in listOf("PREVIOUS_SENTENCE", "NEXT_SENTENCE")) {
      assertFalse(
        NavigationGestureActions.perform(
          value,
          noFeedback,
          /* hasWebContent= */ false,
          /* selectionModeActive= */ true,
          /* eventId= */ null,
        )
      )
    }
  }

  @Test
  fun headingsLinksAndControlsWorkOutsideWebPages() {
    for (name in listOf("HEADING", "LINK", "CONTROL")) {
      val target = NavigationGestureActions.find("NEXT_$name")!!.target
      assertTrue(target is NavigationGestureActions.Target.NativeOrWeb)
    }
  }
}
