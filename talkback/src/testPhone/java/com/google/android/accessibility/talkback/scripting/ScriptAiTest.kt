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

package com.google.android.accessibility.talkback.scripting

import com.google.android.accessibility.utils.FormFactorUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScriptAiTest {
  private val context = RuntimeEnvironment.getApplication()

  @Before
  fun setUp() {
    FormFactorUtils.initialize(context)
  }

  private val source =
    """
    local manifest = {
      name = 'Podcast rows',
      version = '1',
      minApiVersion = 6,
      testedApiVersion = 6,
      apps = { 'com.example.podcasts' },
      permissions = { 'speech', 'input', 'actions', 'network' },
      rules = {
        { match = { id = 'episode_row' }, group = true, buttonActions = true },
      },
    }

    sf.onFocus(function(item, text)
      return nil
    end)
    """.trimIndent()

  @Test
  fun save_makesABuiltScript_withTheCodeAsLua_andHoldsNetworkUntilAccepted() {
    val saved = ScriptAi.save(context, null, source)
    val id = saved.id!!
    assertEquals(emptyList<String>(), saved.problems)
    val store = ScriptStore.get(context)
    val draft = ScriptBuilder.draft(store, id)!!
    assertEquals("Podcast rows", draft.getString("name"))
    assertEquals(1, draft.getJSONArray("rules").length())
    assertTrue(draft.getString("code").contains("sf.onFocus"))
    assertTrue(ScriptPermission.NETWORK !in store.find(id)!!.granted)
    assertNotNull(ScriptAi.review(context, id))

    // Saving again changes the same script.
    assertEquals(id, ScriptAi.save(context, id, source.replace("Podcast rows", "Episodes")).id)
    assertEquals("Episodes", ScriptBuilder.draft(store, id)!!.getString("name"))

    ScriptAi.accept(context, id)
    assertTrue(ScriptPermission.NETWORK in store.find(id)!!.granted)
    assertNull(ScriptAi.review(context, id))
    // A change the agent makes is reviewed again.
    ScriptAi.save(context, id, source)
    assertTrue(ScriptPermission.NETWORK !in store.find(id)!!.granted)
    assertNotNull(ScriptAi.review(context, id))
  }

  @Test
  fun save_reportsSyntaxErrors_withoutSaving() {
    val saved = ScriptAi.save(context, null, source.replace("return nil", "return nil nil end end"))
    assertNull(saved.id)
    assertTrue(saved.problems.single(), saved.problems.single().startsWith("Lua:"))
  }
}
