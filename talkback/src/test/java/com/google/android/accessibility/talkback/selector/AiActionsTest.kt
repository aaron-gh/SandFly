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

package com.google.android.accessibility.talkback.selector

import com.google.android.accessibility.talkback.selector.AiActions.Action
import com.google.android.accessibility.utils.ValueSteps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiActionsTest {
  private val screen = Action("SCREEN_OVERVIEW", "Describe screen")
  private val image = Action("DESCRIBE_IMAGE", "Describe image")
  private val agent = Action("AI_AGENT", "Start or stop AI agent task")
  private val actions = listOf(screen, image, agent)

  @Test
  fun step_withNoChoice_startsAfterOrBeforeTheFirstAction() {
    assertEquals(image, AiActions.step(actions, null, forward = true))
    assertEquals(agent, AiActions.step(actions, null, forward = false))
  }

  @Test
  fun step_wrapsAround() {
    assertEquals(screen, AiActions.step(actions, agent.value, forward = true))
    assertEquals(agent, AiActions.step(actions, screen.value, forward = false))
  }

  @Test
  fun step_withWrapAroundOff_staysAtTheLastAndFirstAction() {
    var action: Action? = null
    assertTrue(ValueSteps.run(false) { action = AiActions.step(actions, agent.value, true) })
    assertEquals(agent, action)
    assertTrue(ValueSteps.run(false) { action = AiActions.step(actions, screen.value, false) })
    assertEquals(screen, action)
  }

  @Test
  fun step_fromAnActionNoLongerAvailable_countsFromTheFirstAction() {
    assertEquals(agent, AiActions.step(listOf(image, agent), screen.value, forward = true))
  }

  @Test
  fun step_withOneAction_staysOnIt() {
    assertEquals(agent, AiActions.step(listOf(agent), agent.value, forward = true))
  }

  @Test
  fun included_withNoChoice_includesAll() {
    assertEquals(actions, AiActions.included(actions, null))
  }

  @Test
  fun included_keepsTheReadingControlOrder() {
    assertEquals(
      listOf(screen, agent),
      AiActions.included(actions, setOf(agent.value, screen.value, "REMOVED_COMMAND")),
    )
  }

  @Test
  fun included_withNoneChosen_isEmpty() {
    assertEquals(emptyList<Action>(), AiActions.included(actions, emptySet()))
  }

  @Test
  fun step_withNoActions_returnsNull() {
    assertNull(AiActions.step(emptyList(), null, forward = true))
  }
}
