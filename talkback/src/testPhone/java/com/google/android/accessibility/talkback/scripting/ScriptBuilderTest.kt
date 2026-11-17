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

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScriptBuilderTest {
  private val context: Context = RuntimeEnvironment.getApplication()

  @Test
  fun lua_readsBackAsTheSameManifest() {
    val manifest =
      JSONObject()
        .put("id", "built-x")
        .put("name", "Quotes \" and \\ and\nnew line, tab\t, ü, 😀, bell \u0007")
        .put("author", "a")
        .put("minApiVersion", 6)
        .put("testedApiVersion", 6)
        .put("permissions", JSONArray().put("speech"))
        .put(
          "rules",
          JSONArray()
            .put(
              JSONObject()
                .put("match", JSONObject().put("matches", "^new post$").put("field", "contentDescription").put("kind", JSONArray().put("clickable")))
                .put("label", "Compose")
                .put("hide", "all")
                .put("longPressAfter", 500)
                .put("heading", false)
            ),
        )

    val read = JSONObject(ManifestReader.read("local manifest = ${ScriptBuilder.lua(manifest)}\n"))

    assertEquals(manifest.toString(), read.toString())
  }

  @Test
  fun save_installsFinishedRulesAndKeepsTheDraft() {
    val id = ScriptBuilder.create(context, "Social")
    val store = ScriptStore.get(context)
    val draft = ScriptBuilder.draft(store, id)!!
    draft.getJSONArray("apps").put("com.example.social")
    draft
      .getJSONArray("rules")
      .put(
        JSONObject()
          .put("match", JSONObject().put("contains", "new post").put("field", "contentDescription").put("kind", "clickable"))
          .put("label", "Compose")
      )
      // Not finished: it changes nothing.
      .put(JSONObject().put("match", JSONObject().put("id", "toolbar")))
      .put(
        JSONObject()
          .put("match", JSONObject().put("id", "row"))
          .put("group", true)
          .put("actions", JSONArray().put(JSONObject().put("title", "Archive").put("click", JSONObject().put("id", "archive"))))
      )
    draft.getJSONArray("navigation").put(JSONObject().put("title", "Posts").put("match", JSONObject().put("id", "post")))

    ScriptBuilder.save(context, draft)

    val installed = store.find(id)!!
    assertEquals(2, installed.manifest.rules.size)
    assertEquals("Compose", installed.manifest.rules[0].label)
    assertEquals(1, installed.manifest.navigation.size)
    assertEquals(
      setOf(ScriptPermission.SPEECH, ScriptPermission.INPUT, ScriptPermission.ACTIONS),
      installed.manifest.permissions,
    )
    assertEquals(installed.manifest.permissions, installed.granted)
    assertTrue(ScriptBuilder.isBuilt(store, id))
    assertEquals(3, ScriptBuilder.draft(store, id)!!.getJSONArray("rules").length())
    assertNotNull(ScriptBuilder.ruleProblem(draft.getJSONArray("rules").getJSONObject(1)))
    assertNull(ScriptBuilder.ruleProblem(draft.getJSONArray("rules").getJSONObject(0)))
  }

  @Test
  fun permissions_followWhatActionsDo() {
    val speakItem =
      JSONObject()
        .put("match", JSONObject().put("id", "row"))
        .put("actions", JSONArray().put(JSONObject().put("title", "Time").put("speak", JSONObject().put("textOf", JSONObject().put("id", "time")))))
    val speakText =
      JSONObject()
        .put("match", JSONObject().put("id", "row"))
        .put("actions", JSONArray().put(JSONObject().put("title", "Hello").put("speak", "Hello")))

    assertEquals(
      setOf(ScriptPermission.INPUT, ScriptPermission.SCREEN),
      ScriptBuilder.permissionsOf(listOf(speakItem), emptyList()),
    )
    assertEquals(setOf(ScriptPermission.INPUT), ScriptBuilder.permissionsOf(listOf(speakText), emptyList()))
  }

  @Test
  fun inspectedItem_givesAQueryForTheBuilder() {
    val button =
      AccessibilityNodeInfo.obtain().apply {
        contentDescription = "New post"
        isClickable = true
        isVisibleToUser = true
      }
    ItemInspector.report(context, AccessibilityNodeInfoCompat.wrap(button))

    val query = ItemInspector.lastQuery(context)!!

    assertEquals("""{"contains":"New post","field":"contentDescription","kind":"clickable"}""", query.toString())
    assertTrue(NodeQuery.of(query).matches(AccessibilityNodeInfoCompat.wrap(button)))
  }

  @Test
  fun inspectedRowWithoutOwnText_isNamedByTheTextInIt() {
    val row = AccessibilityNodeInfo.obtain().apply {
      isClickable = true
      isVisibleToUser = true
    }
    val title = AccessibilityNodeInfo.obtain().apply {
      text = "Location"
      isVisibleToUser = true
    }
    org.robolectric.Shadows.shadowOf(row).addChild(title)

    ItemInspector.report(context, AccessibilityNodeInfoCompat.wrap(row))

    assertEquals(
      """{"has":{"contains":"Location","field":"text"},"kind":"clickable"}""",
      ItemInspector.lastQuery(context).toString(),
    )
  }
}
