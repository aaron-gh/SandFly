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

import com.google.android.accessibility.utils.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScriptRulesMergeTest {
  private fun merge(vararg rules: ScriptRule): NodeRule? = ScriptRules.mergeRules(rules.toList())

  @Test
  fun noRulesChangeNothing() {
    assertNull(merge())
  }

  @Test
  fun theFirstRuleThatSetsSomethingWins() {
    val merged =
      merge(ScriptRule(id = "a", label = "First"), ScriptRule(id = "a", label = "Second"))!!

    assertEquals("First", merged.label)
  }

  @Test
  fun laterRulesFillInWhatEarlierOnesLeave() {
    val merged =
      merge(
        ScriptRule(id = "a", label = "Play"),
        ScriptRule(id = "a", hint = "Double-tap to play", role = "button", heading = true),
      )!!

    assertEquals("Play", merged.label)
    assertEquals("Double-tap to play", merged.hint)
    assertEquals(Role.ROLE_BUTTON, merged.role)
    assertEquals(true, merged.heading)
    assertNull(merged.speak)
    assertNull(merged.state)
  }

  @Test
  fun hidingAnItemKeepsWhatIsInsideIt() {
    val merged = merge(ScriptRule(id = "a", hide = Hide.SELF))!!

    assertTrue(merged.hide)
    assertFalse(merged.hideInside)
    assertFalse(merged.hidesDescendants)
  }

  @Test
  fun hidingAllHidesWhatIsInsideToo() {
    val merged = merge(ScriptRule(id = "a", hide = Hide.ALL))!!

    assertTrue(merged.hide)
    assertTrue(merged.hidesDescendants)
  }

  @Test
  fun aRuleThatDoesNotHideLeavesALaterOneToDecide() {
    val merged = merge(ScriptRule(id = "a", label = "A"), ScriptRule(id = "a", hide = Hide.ALL))!!

    assertTrue(merged.hide)
    assertTrue(merged.hideInside)
  }

  @Test
  fun anyRuleCanGroupTheItem() {
    val merged = merge(ScriptRule(id = "a", label = "Row"), ScriptRule(id = "a", group = true))!!

    assertTrue(merged.group)
    assertTrue(merged.hidesDescendants)
    assertFalse(merged.hide)
  }

  @Test
  fun readingOrderComesFromTheFirstRuleThatGivesOne() {
    val merged =
      merge(
        ScriptRule(id = "a", readAfter = NodeQuery(id = "title")),
        ScriptRule(id = "a", readBefore = NodeQuery(id = "other")),
      )!!

    assertFalse(merged.order!!.isBefore)
  }

  @Test
  fun theRulesThatMatchedAreListedForTheInspector() {
    val merged =
      ScriptRules.mergeRules(listOf(ScriptRule(id = "a", label = "A")), listOf("My script: a"))!!

    assertEquals(listOf("My script: a"), merged.sources)
  }
}
