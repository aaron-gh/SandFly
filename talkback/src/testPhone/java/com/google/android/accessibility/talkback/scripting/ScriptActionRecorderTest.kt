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
  fun touchHold_isAnActionAndAStep() {
    val rule =
      ManifestParser(strict = true).rule(
        JSONObject()
          .put("match", JSONObject().put("id", "comment"))
          .put(
            "actions",
            JSONArray()
              .put(JSONObject().put("title", "React").put("touchHold", JSONObject().put("matches", "^Like.+")))
              .put(JSONObject().put("title", "Menu").put("steps", JSONArray().put(JSONObject().put("touchHold", JSONArray())))),
          )
      )
    assertEquals(RuleItemAction.Verb.TOUCH_HOLD, rule.actions[0].verb)
    assertEquals(RuleItemAction.Verb.TOUCH_HOLD, rule.actions[1].steps.single().verb)
  }

  @Test
  fun buttonInTheItem_isFoundWithoutTheEpisodesName() {
    val row = JSONObject().put("description", "Episode 12: The tide, 41 minutes")
    val target = ItemPattern.target(row, JSONObject().put("description", "Download Episode 12: The tide"))!!
    assertEquals("^Download.+", target.getString("matches"))
  }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RuleButtonsTest {
  private fun node(description: String? = null, text: String? = null, clickable: Boolean = false) =
    AccessibilityNodeInfo.obtain().apply {
      contentDescription = description
      this.text = text
      isClickable = clickable
      isVisibleToUser = true
    }

  @Test
  fun buttonsIn_areTheOutermostButtonsByTheirNames() {
    val row = node(description = "Episode 12, 41 minutes", clickable = true)
    val download = node(description = "Download", clickable = true)
    shadowOf(download).addChild(node(text = "Download"))
    val more = node(clickable = true)
    shadowOf(more).addChild(node(text = "More options"))
    val group = node()
    shadowOf(group).addChild(node(description = "Like", clickable = true))
    shadowOf(group).addChild(node(description = "Share", clickable = true))
    shadowOf(row).addChild(download)
    shadowOf(row).addChild(more)
    shadowOf(row).addChild(group)
    val buttons = buttonsIn(AccessibilityNodeInfoCompat.wrap(row), listOf("share"))
    assertEquals(listOf("Download", "More options", "Like"), buttons.map { it.first })
  }

  @Test
  fun buttonActions_makeARuleWithoutOtherChanges() {
    val rule = ManifestParser(strict = true).rule(JSONObject().put("match", JSONObject().put("id", "row")).put("buttonActions", true).put("skipButtons", JSONArray().put("Share")))
    assertEquals(true, rule.buttonActions)
    assertEquals(listOf("Share"), rule.skipButtons)
  }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReadPartsTest {
  private fun node(description: String? = null, text: String? = null) =
    AccessibilityNodeInfo.obtain().apply {
      contentDescription = description
      this.text = text
      isVisibleToUser = true
    }

  private fun message(sender: String?, body: String, quote: String?): AccessibilityNodeInfo {
    val row = node()
    shadowOf(row).addChild(node(text = sender ?: ""))
    shadowOf(row).addChild(node(text = body))
    shadowOf(row).addChild(node(text = quote ?: ""))
    return row
  }

  private val parts =
    listOf(
      ReadPart(path = listOf(1), fromAbove = true),
      ReadPart(path = listOf(2)),
      ReadPart(path = listOf(3), before = "in reply to"),
    )

  @Test
  fun compose_takesPartsInOrder_withTextBefore_andTheSenderFromAbove() {
    val list = node()
    shadowOf(list).addChild(message("Alex", "Are you coming?", null))
    shadowOf(list).addChild(message(null, "Running late", "Are you coming?"))
    val second = AccessibilityNodeInfoCompat.wrap(list.getChild(1))
    assertEquals("Alex, Running late, in reply to Are you coming?", ScriptReadParts.compose(second, parts))
  }

  @Test
  fun readParts_parseFromAManifest_andPreviewOnASample() {
    val json =
      JSONArray()
        .put(JSONObject().put("path", JSONArray().put(1)).put("fromAbove", true))
        .put(JSONObject().put("path", JSONArray().put(2)))
        .put(JSONObject().put("from", JSONArray()).put("before", "row"))
    val rule = ManifestParser(strict = true).rule(JSONObject().put("match", JSONObject().put("id", "row")).put("read", json))
    assertEquals(3, rule.read.size)
    assertEquals(listOf(1), rule.read[0].path)
    val item =
      JSONObject()
        .put("description", "Message")
        .put("children", JSONArray().put(JSONObject().put("text", "Alex")).put(JSONObject().put("text", "Hi")))
    json.getJSONObject(2).put("from", JSONObject())
    assertEquals("Alex, Hi, row Message", ItemPattern.readText(item, json))
    assertEquals(listOf(listOf(1) to "Alex", listOf(2) to "Hi"), ItemPattern.readChoices(item))
  }
}
