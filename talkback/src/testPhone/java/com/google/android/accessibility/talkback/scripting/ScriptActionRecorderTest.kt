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

import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.PerformActionUtils
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScriptActionRecorderTest {
  private val context = RuntimeEnvironment.getApplication()

  private fun node(className: String, text: String? = null, description: String? = null) =
    AccessibilityNodeInfo.obtain().apply {
      this.className = className
      this.text = text
      contentDescription = description
      packageName = "com.example.podcasts"
      isVisibleToUser = true
      isClickable = true
      isLongClickable = true
      addAction(AccessibilityNodeInfo.ACTION_CLICK)
      addAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
    }

  @Test
  fun longPressAndMenuEntry_becomeStepsForAnyEpisode() {
    val list = node("androidx.recyclerview.widget.RecyclerView")
    val row = node("android.view.ViewGroup", description = "Episode 12: The tide, 41 minutes")
    val download = node("android.widget.ImageButton", description = "Download Episode 12: The tide")
    shadowOf(row).addChild(download)
    shadowOf(list).addChild(row)
    val menu = node("android.widget.TextView", text = "Add to queue")

    ScriptActionRecorder.start(context, "podcasts", 0, "Add to queue", JSONObject().put("contains", "minutes"))
    PerformActionUtils.performAction(AccessibilityNodeInfoCompat.wrap(list.getChild(0)), AccessibilityNodeInfo.ACTION_LONG_CLICK, null)
    PerformActionUtils.performAction(AccessibilityNodeInfoCompat.wrap(menu), AccessibilityNodeInfo.ACTION_CLICK, null)
    val steps = ScriptActionRecorder.stop(context)

    assertEquals(2, steps.size)
    assertEquals("{}", steps[0].getJSONObject("longClick").toString())
    assertEquals("item", steps[0].getString("where"))
    assertEquals("Add to queue", steps[1].getJSONObject("click").getString("text"))
    assertEquals("screen", steps[1].getString("where"))

    // The steps make an action SandFly takes.
    val rule =
      ManifestParser(strict = true).rule(
        JSONObject()
          .put("match", JSONObject().put("contains", "minutes"))
          .put("actions", JSONArray().put(JSONObject().put("title", "Add to queue").put("steps", JSONArray(steps))))
      )
    val action = rule.actions.single()
    assertEquals(2, action.steps.size)
    assertEquals(RuleItemAction.Verb.LONG_CLICK, action.steps[0].verb)
    assertEquals(true, action.steps[1].onScreen)

    // In main.lua an empty table reads as an empty list, which is the item too.
    val fromLua =
      ManifestParser(strict = true).rule(
        JSONObject()
          .put("match", JSONObject().put("contains", "minutes"))
          .put("actions", JSONArray().put(JSONObject().put("title", "Queue").put("steps", JSONArray().put(JSONObject().put("longClick", JSONArray())))))
      )
    assertEquals(RuleItemAction.Verb.LONG_CLICK, fromLua.actions.single().steps.single().verb)
  }

  @Test
  fun buttonInTheItem_isFoundWithoutTheEpisodesName() {
    val row = JSONObject().put("description", "Episode 12: The tide, 41 minutes")
    val target = ItemPattern.target(row, JSONObject().put("description", "Download Episode 12: The tide"))!!
    assertEquals("^Download.+", target.getString("matches"))
  }
}
