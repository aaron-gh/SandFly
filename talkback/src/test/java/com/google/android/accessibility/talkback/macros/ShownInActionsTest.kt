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

import com.google.android.accessibility.talkback.macros.Macros.Clause
import com.google.android.accessibility.talkback.macros.Macros.Condition
import com.google.android.accessibility.talkback.macros.Macros.ItemOnScreen
import com.google.android.accessibility.talkback.macros.Macros.Macro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** The macros shown in Actions: conditions checked once per item and test, not once per call. */
class ShownInActionsTest {
  private fun macro(id: String, vararg labels: String) =
    Macro(
      id,
      id,
      emptyList(),
      inActions = true,
      actionsCondition = Condition(labels.map { Clause(ItemOnScreen(it)) }),
    )

  private val copy = macro("copy", "Copy")
  private val move = macro("move", "Copy", "Move")
  private val always = Macro("always", "always", emptyList(), inActions = true)

  private val checked = ArrayList<Macros.Test>()

  private fun check(onScreen: Set<String>): (Macros.Test) -> Boolean = { test ->
    checked.add(test)
    (test as ItemOnScreen).label in onScreen
  }

  @Test
  fun macrosWithoutConditionsAreNotChecked() {
    val cache = ShownInActions(1_000)
    val macros = listOf(always)
    assertSame(macros, cache.get("item", macros, 0, check(emptySet())))
    assertEquals(0, checked.size)
  }

  @Test
  fun sameTestIsCheckedOnceForAllMacros() {
    val shown = ShownInActions.shownBy(listOf(copy, move, always), check(setOf("Copy")))
    assertEquals(listOf(copy, always), shown)
    assertEquals(listOf<Macros.Test>(ItemOnScreen("Copy"), ItemOnScreen("Move")), checked)
  }

  @Test
  fun sameItemReusesResultUntilItExpires() {
    val cache = ShownInActions(1_000)
    val macros = listOf(copy, move, always)
    repeat(10) { cache.get("item", macros, 100L + it, check(setOf("Copy", "Move"))) }
    assertEquals(2, checked.size)
    assertEquals(listOf(copy, move, always), cache.get("item", macros, 1_099, check(emptySet())))
    assertEquals(2, checked.size)
    assertEquals(listOf(always), cache.get("item", macros, 1_100, check(emptySet())))
    assertEquals(3, checked.size)
  }

  @Test
  fun otherItemOrChangedMacrosAreCheckedAgain() {
    val cache = ShownInActions(1_000)
    val macros = listOf(copy, always)
    cache.get("item", macros, 0, check(setOf("Copy")))
    assertEquals(listOf(always), cache.get("other", macros, 1, check(emptySet())))
    val changed = listOf(copy.copy(actionsCondition = Condition(listOf(Clause(ItemOnScreen("Move"))))), always)
    assertEquals(changed, cache.get("other", changed, 2, check(setOf("Move"))))
    assertEquals(3, checked.size)
  }

  @Test
  fun conditionTiming_addsTimeByKindOfTest() {
    var now = 0L
    val timing = ConditionTiming { now }
    assertTrue(timing.isEmpty)
    assertTrue(timing.time(ItemOnScreen("Copy")) { now += 30_000_000; true })
    assertFalse(timing.time(ItemOnScreen("Move")) { now += 10_000_000; false })
    timing.time(Macros.KeyboardShown) { now += 500_000; false }
    assertFalse(timing.isEmpty)
    assertEquals(
      "3 tests in 40.5 ms: ItemOnScreen 2 in 40.0 ms, KeyboardShown 1 in 0.5 ms",
      timing.describe(),
    )
  }
}
