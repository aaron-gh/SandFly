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

package com.google.android.accessibility.talkback.macros

import android.content.Context
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.macros.Macros.AppInFront
import com.google.android.accessibility.talkback.macros.Macros.Clause
import com.google.android.accessibility.talkback.macros.Macros.Condition
import com.google.android.accessibility.talkback.macros.Macros.Else
import com.google.android.accessibility.talkback.macros.Macros.ElseIf
import com.google.android.accessibility.talkback.macros.Macros.EndIf
import com.google.android.accessibility.talkback.macros.Macros.EndRepeat
import com.google.android.accessibility.talkback.macros.Macros.ExitLoop
import com.google.android.accessibility.talkback.macros.Macros.FocusedItem
import com.google.android.accessibility.talkback.macros.Macros.If
import com.google.android.accessibility.talkback.macros.Macros.ItemOnScreen
import com.google.android.accessibility.talkback.macros.Macros.KeyboardShown
import com.google.android.accessibility.talkback.macros.Macros.Kind
import com.google.android.accessibility.talkback.macros.Macros.Macro
import com.google.android.accessibility.talkback.macros.Macros.RepeatTimes
import com.google.android.accessibility.talkback.macros.Macros.RepeatWhile
import com.google.android.accessibility.talkback.macros.Macros.ScreenLocked
import com.google.android.accessibility.talkback.macros.Macros.StopMacro
import com.google.android.accessibility.talkback.macros.Macros.Test as MacroTest
import com.google.android.accessibility.talkback.macros.Macros.Wait
import com.google.android.accessibility.talkback.macros.Macros.WaitUntil
import com.google.android.accessibility.utils.FormFactorUtils
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MacroFlowTest {
  private lateinit var context: Context

  private val keyboard = Condition(listOf(Clause(KeyboardShown)))
  private val locked = Condition(listOf(Clause(ScreenLocked)))

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    FormFactorUtils.initialize(context)
  }

  private fun met(condition: Condition, vararg true_: MacroTest): Boolean {
    val checked = ArrayList<MacroTest>()
    return condition.isMet {
      checked += it
      it in true_
    }
  }

  @Test
  fun andGoesBeforeOr() {
    val a = ItemOnScreen("a")
    val b = ItemOnScreen("b")
    val c = ItemOnScreen("c")
    // a and b, or c
    val condition = Condition(listOf(Clause(a), Clause(b), Clause(c, or = true)))
    assertTrue(met(condition, a, b))
    assertTrue(met(condition, c))
    assertFalse(met(condition, a))
    assertFalse(met(condition, b))
    assertFalse(met(condition))
    // a, or b and c
    val other = Condition(listOf(Clause(a), Clause(b, or = true), Clause(c)))
    assertTrue(met(other, a))
    assertTrue(met(other, b, c))
    assertFalse(met(other, b))
  }

  @Test
  fun notReversesATest() {
    val condition = Condition(listOf(Clause(KeyboardShown, not = true), Clause(ScreenLocked)))
    assertTrue(met(condition, ScreenLocked))
    assertFalse(met(condition, ScreenLocked, KeyboardShown))
  }

  @Test
  fun noTestsIsMetAndTestsAfterAFailedAndAreNotChecked() {
    assertTrue(Condition().isMet { throw AssertionError() })
    val checked = ArrayList<MacroTest>()
    val condition = Condition(listOf(Clause(KeyboardShown), Clause(ScreenLocked)))
    assertFalse(condition.isMet { checked += it; false })
    assertEquals(listOf<MacroTest>(KeyboardShown), checked)
  }

  @Test
  fun ifElseIfElseLeadToTheNextBranchAndTheEnd() {
    val steps = listOf(If(keyboard), Wait(1), ElseIf(locked), Wait(2), Else, Wait(3), EndIf, Wait(4))
    val flow = Macros.analyze(steps)
    assertNull(flow.problem)
    assertEquals(2, flow.next[0])
    assertEquals(4, flow.next[2])
    assertEquals(6, flow.next[4])
    assertEquals(6, flow.end[0])
    assertEquals(6, flow.end[2])
    assertEquals(6, flow.end[4])
    assertArrayEquals(intArrayOf(0, 1, 0, 1, 0, 1, 0, 0), flow.level)
  }

  @Test
  fun repeatsLeadToTheirEndAndBack() {
    val steps =
      listOf(RepeatTimes(3), RepeatWhile(keyboard), If(locked), ExitLoop, EndIf, EndRepeat, ExitLoop, EndRepeat)
    val flow = Macros.analyze(steps)
    assertNull(flow.problem)
    assertEquals(7, flow.end[0])
    assertEquals(5, flow.end[1])
    assertEquals(1, flow.start[5])
    assertEquals(0, flow.start[7])
    assertEquals(1, flow.start[3])
    assertEquals(0, flow.start[6])
    assertArrayEquals(intArrayOf(0, 1, 2, 3, 2, 1, 1, 0), flow.level)
  }

  @Test
  fun mismatchedBlocksAreProblems() {
    fun problem(vararg steps: Macros.Step) = Macros.analyze(steps.toList()).problem
    assertEquals(Macros.Problem(0, R.string.macro_problem_no_end_if), problem(If(keyboard), Wait(1)))
    assertEquals(Macros.Problem(0, R.string.macro_problem_no_end_repeat), problem(RepeatTimes(2)))
    assertEquals(Macros.Problem(0, R.string.macro_problem_no_if), problem(Else, EndIf))
    assertEquals(Macros.Problem(0, R.string.macro_problem_no_if), problem(EndIf))
    assertEquals(Macros.Problem(0, R.string.macro_problem_no_repeat), problem(EndRepeat))
    assertEquals(Macros.Problem(0, R.string.macro_problem_exit), problem(ExitLoop))
    assertEquals(Macros.Problem(2, R.string.macro_problem_after_else), problem(If(keyboard), Else, Else, EndIf))
    assertEquals(
      Macros.Problem(2, R.string.macro_problem_after_else),
      problem(If(keyboard), Else, ElseIf(locked), EndIf),
    )
    // An If closed inside a repeat that it isn't in: the repeat has no End repeat.
    assertEquals(
      Macros.Problem(1, R.string.macro_problem_no_end_repeat),
      problem(If(keyboard), RepeatTimes(2), EndIf, EndRepeat),
    )
    assertEquals(Macros.Problem(1, R.string.macro_problem_exit), problem(If(keyboard), ExitLoop, EndIf))
    assertNull(problem())
    assertNull(problem(Wait(1), StopMacro))
  }

  @Test
  fun deletingABlockStartDeletesItsOwnElseAndEnd() {
    val steps =
      listOf(If(keyboard), Wait(1), If(locked), Wait(2), Else, EndIf, Else, Wait(3), EndIf, Wait(4))
    assertEquals(
      listOf(Wait(1), If(locked), Wait(2), Else, EndIf, Wait(3), Wait(4)),
      Macros.removed(steps, 0),
    )
    assertEquals(
      listOf(If(keyboard), Wait(1), Wait(2), Else, Wait(3), EndIf, Wait(4)),
      Macros.removed(steps, 2),
    )
    val loop = listOf(RepeatTimes(2), If(keyboard), EndIf, EndRepeat, Wait(1))
    assertEquals(listOf(If(keyboard), EndIf, Wait(1)), Macros.removed(loop, 0))
    assertEquals(listOf(Wait(1)), Macros.removed(listOf(Wait(1), EndIf), 1))
    assertNull(Macros.removed(loop, 9))
  }

  @Test
  fun conditionsAndBlocksSurviveJson() {
    val condition =
      Condition(
        listOf(
          Clause(ItemOnScreen("Send", Kind.CLICKABLE)),
          Clause(FocusedItem("", Kind.EDITABLE), not = true),
          Clause(AppInFront("com.whatsapp"), or = true),
          Clause(KeyboardShown),
          Clause(ScreenLocked, not = true, or = true),
          Clause(FocusedItem("Message")),
        )
      )
    val macro =
      Macro(
        "a",
        "Flow",
        listOf(
          If(condition),
          ElseIf(keyboard),
          Else,
          EndIf,
          RepeatTimes(5),
          RepeatWhile(locked),
          ExitLoop,
          EndRepeat,
          EndRepeat,
          WaitUntil(keyboard, 4_000),
          StopMacro,
        ),
        condition = locked,
      )
    assertEquals(listOf(macro), Macros.fromJson(Macros.toJson(listOf(macro))))
  }

  @Test
  fun conditionValuesOutOfRangeAreKeptWithinLimitsOrLeftOut() {
    val tests =
      JSONArray()
        .put(JSONObject().put("test", "item").put("label", " "))
        .put(JSONObject().put("test", "focused"))
        .put(JSONObject().put("test", "focused").put("kind", "unknown"))
        .put(JSONObject().put("test", "app"))
        .put(JSONObject().put("test", "weather"))
        .put(JSONObject().put("test", "item").put("label", "OK").put("kind", "unknown"))
        .put(JSONObject().put("test", "focused").put("kind", "heading").put("or", true))
    val steps =
      JSONArray()
        .put(JSONObject().put("type", "repeat").put("count", 0))
        .put(JSONObject().put("type", "repeat").put("count", 1_000_000))
        .put(JSONObject().put("type", "wait_until").put("timeout", 1))
        .put(JSONObject().put("type", "if").put("condition", tests))
    val json =
      JSONArray().put(JSONObject().put("id", "a").put("name", "A").put("steps", steps)).toString()
    assertEquals(
      listOf(
        RepeatTimes(1),
        RepeatTimes(Macros.MAX_REPEATS),
        WaitUntil(Condition(), Macros.MIN_TAP_TIMEOUT_MS),
        If(Condition(listOf(Clause(ItemOnScreen("OK")), Clause(FocusedItem("", Kind.HEADING), or = true)))),
      ),
      Macros.fromJson(json).single().steps,
    )
  }

  @Test
  fun describeJoinsTests() {
    val condition =
      Condition(
        listOf(
          Clause(ItemOnScreen("Send", Kind.CLICKABLE)),
          Clause(KeyboardShown, not = true),
          Clause(FocusedItem("", Kind.EDITABLE), or = true),
        )
      )
    assertEquals(
      "Item on screen: Send, Clickable and Keyboard hidden, or Focused item: Editable",
      Macros.describe(context, condition),
    )
    assertEquals("If No tests", Macros.describe(context, If(Condition())))
    assertEquals("Repeat 3 times", Macros.describe(context, RepeatTimes(3)))
  }
}
