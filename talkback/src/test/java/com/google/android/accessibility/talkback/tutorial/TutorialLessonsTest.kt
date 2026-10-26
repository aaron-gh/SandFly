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

package com.google.android.accessibility.talkback.tutorial

import android.content.Context
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping
import com.google.android.accessibility.talkback.selector.SelectorController.Setting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** The tutorial's lessons: their texts, gestures and what each step waits for. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TutorialLessonsTest {
  private val context: Context
    get() = RuntimeEnvironment.getApplication()

  private class FakeContext(
    private val context: Context,
    private val recent: Int = 0,
    override val readingControl: Setting = Setting.GRANULARITY_CHARACTERS,
  ) : StepContext {
    override fun isAction(action: String, key: Int) = action == context.getString(key)

    override fun recentAction(key: Int) = key == recent

    override fun string(id: Int) = context.getString(id)
  }

  private fun step(lesson: String, index: Int) =
    TutorialLessons.all[TutorialLessons.indexOf(lesson)].steps[index]

  /** The highest %n$ placeholder in the string, or 0. */
  private fun placeholders(id: Int): Int =
    Regex("""%(\d+)\$""").findAll(context.getString(id)).maxOfOrNull { it.groupValues[1].toInt() }
      ?: 0

  @Test
  fun lessonIdsAreUnique() {
    val ids = TutorialLessons.all.map { it.id }
    assertEquals(ids.size, ids.toSet().size)
    assertTrue(TutorialLessons.indexOf(TutorialLessons.PRACTICE_LESSON) >= 0)
  }

  @Test
  fun everyInstructionHasAnArgumentForEachPlaceholder() {
    for (lesson in TutorialLessons.all) {
      for (step in lesson.steps) {
        val others = step.names.size + if (step.listItem > 0) 1 else 0
        assertTrue(
          context.getResources().getResourceEntryName(step.text),
          placeholders(step.text) <= step.actions.size + others,
        )
        if (step.altText != 0) {
          assertTrue(placeholders(step.altText) <= step.altActions.size + others)
        }
      }
    }
  }

  @Test
  fun everyActionHasADefaultGestureOrAnAlternative() {
    val mapping = GestureShortcutMapping(context)
    val assigned = mapping.gestureActions.values.toSet()
    mapping.onUnbind()
    for (lesson in TutorialLessons.all) {
      for (step in lesson.steps) {
        val actions =
          if (step.actions.all { context.getString(it) in assigned }) step.actions
          else step.altActions
        for (action in actions) {
          assertTrue(context.getString(action), context.getString(action) in assigned)
        }
      }
    }
  }

  @Test
  fun movingToEggsCountsOnlyAfterASwipe() {
    val step = step("move", 0)
    val eggs = TutorialInput.Focused(Targets.EGGS)
    assertNull(step.check(eggs, FakeContext(context)))
    assertNotNull(step.check(eggs, FakeContext(context, recent = R.string.shortcut_value_next)))
    assertNull(
      step.check(
        TutorialInput.Focused(Targets.DATES),
        FakeContext(context, recent = R.string.shortcut_value_next),
      )
    )
  }

  @Test
  fun exploringCountsOnlyTouchedSquares() {
    val step = step("explore", 0)
    assertEquals(4, step.count)
    assertTrue(step.distinct)
    val sun = TutorialInput.Focused(Targets.SUN)
    assertEquals(Targets.SUN, step.check(sun, FakeContext(context)))
    assertNull(step.check(sun, FakeContext(context, recent = R.string.shortcut_value_next)))
    assertNull(step.check(TutorialInput.Focused(Targets.APPLES), FakeContext(context)))
  }

  @Test
  fun choosingWordsNeedsTheWordsReadingControl() {
    val step = step("reading_controls", 1)
    val gesture = TutorialInput.Gesture(0, context.getString(R.string.shortcut_value_select_next_setting))
    assertNull(step.check(gesture, FakeContext(context)))
    assertNotNull(
      step.check(gesture, FakeContext(context, readingControl = Setting.GRANULARITY_WORDS))
    )
  }

  @Test
  fun typingAcceptsTheWordInAnyCase() {
    val step = step("typing", 1)
    val word = context.getString(R.string.sandfly_tutorial_typing_word)
    assertNotNull(
      step.check(TutorialInput.TextChanged(Targets.NAME, word.uppercase() + " "), FakeContext(context))
    )
    assertNull(step.check(TutorialInput.TextChanged(Targets.NAME, word + "s"), FakeContext(context)))
  }

  @Test
  fun stepsThatTeachLeavingGesturesTakeThem() {
    val kept = TutorialLessons.LEAVING_ACTIONS + TutorialLessons.SILENCING_ACTIONS
    for (lesson in TutorialLessons.all) {
      for (step in lesson.steps) {
        for (action in step.actions.filter { it in kept }) {
          assertTrue(context.getString(action), action in step.taken)
        }
      }
    }
  }

  @Test
  fun systemGesturesAreTakenInTheirSteps() {
    val lesson = TutorialLessons.all[TutorialLessons.indexOf("system")]
    for (step in lesson.steps) {
      assertEquals(step.actions, step.taken)
    }
  }
}
