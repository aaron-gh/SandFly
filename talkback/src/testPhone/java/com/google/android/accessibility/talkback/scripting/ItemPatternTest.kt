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

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ItemPatternTest {
  companion object {
    fun node(cls: String, text: String? = null, description: String? = null, clickable: Boolean = false, vararg children: JSONObject): JSONObject =
      JSONObject()
        .put("class", cls)
        .putOpt("text", text)
        .putOpt("description", description)
        .apply { if (clickable) put("clickable", true) }
        .apply { if (children.isNotEmpty()) put("children", JSONArray(children.toList())) }

    /** A row of Facebook's Friends tab, as Inspect focused item showed it. */
    fun friendRow(name: String, mutual: String): JSONObject =
      node(
        "android.widget.Button",
        description = "$name, $mutual",
        clickable = true,
        children =
          arrayOf(
            node("android.widget.ImageView", description = name),
            node("android.widget.TextView", text = name),
            node("android.widget.TextView", text = mutual),
            // As in Facebook, the button's text is described too.
            node("android.widget.Button", description = "Add Friend", clickable = true, children = arrayOf(node("android.widget.TextView", text = "Add friend", description = "Add friend"))),
            node("android.widget.Button", description = "Remove $name", clickable = true, children = arrayOf(node("android.widget.TextView", text = "Remove"))),
          ),
      )

    val friends: JSONObject =
      JSONObject()
        .put("item", friendRow("Natalie Anderson", "3 mutual friends"))
        .put("similar", JSONArray().put(friendRow("Zoe Murrow", "1 mutual friend")).put(friendRow("Sam Lee", "12 mutual friends")))

    /** A People you may know card, alone on screen. */
    val suggestion: JSONObject =
      JSONObject().put(
        "item",
        node(
          "android.view.ViewGroup",
          clickable = true,
          children =
            arrayOf(
              node("android.widget.TextView", text = "Zoe Murrow"),
              node("android.widget.ImageView", description = "Zoe Murrow's profile picture"),
              node("android.widget.Button", description = "Add friend", clickable = true, children = arrayOf(node("android.widget.TextView", text = "Add friend"))),
              node("android.widget.Button", description = "Remove friend suggestion", clickable = true, children = arrayOf(node("android.widget.TextView", text = "Remove"))),
            ),
        ),
      )
  }

  @Test
  fun likeQuery_namesEveryRowByTheButtonTheyShare() {
    val query = ItemPattern.likeQuery(friends)!!
    assertEquals("android.widget.Button", query.getString("className"))
    // The Add Friend button holds a text described "Add friend", so a row is named by its Remove button.
    assertEquals("^Remove", query.getJSONObject("has").getString("matches"))
    assertEquals(1, query.getInt("hasDepth"))
    assertEquals(3, ItemPattern.count(query, friends))
    for (button in listOf(3, 4)) {
      assertEquals(false, ItemPattern.matches(query, friendRow("A B", "2").getJSONArray("children").getJSONObject(button)))
    }
  }

  @Test
  fun buttons_areTitledByWhatEveryRowHas() {
    val buttons = ItemPattern.buttons(friends)
    assertEquals(listOf("Add Friend", "Remove"), buttons.map { it.title })
    assertEquals("^Remove", buttons[1].target.getString("matches"))
    assertEquals("Remove Natalie Anderson", buttons[1].value)
  }

  @Test
  fun parts_ofARowWithADescriptionAreItsDescription() {
    assertEquals(listOf("Natalie Anderson", "3 mutual friends"), ItemPattern.parts(ItemPattern.item(friends)!!, grouped = true))
  }

  @Test
  fun removePart_removesThePartFromEveryRow() {
    fun row(name: String, mutual: String) = friendRow(name, mutual).apply { remove("description") }
    val sample =
      JSONObject()
        .put("item", row("Natalie Anderson", "3 mutual friends"))
        .put("similar", JSONArray().put(row("Zoe Murrow", "1 mutual friend")).put(row("Sam Lee", "12 mutual friends")))
    val parts = ItemPattern.parts(ItemPattern.item(sample)!!, grouped = true)
    assertEquals(listOf("Natalie Anderson", "Natalie Anderson", "3 mutual friends", "Add Friend", "Remove Natalie Anderson"), parts)
    val edits = JSONArray()
    for (i in listOf(1, 2, 3, 4)) edits.put(ItemPattern.removePart(sample, true, i))
    assertEquals("removeRepeats", edits.getJSONObject(0).getString("op"))
    assertEquals("mutual friend", edits.getJSONObject(1).getString("find"))
    for (other in listOf(row("Natalie Anderson", "3 mutual friends"), row("Bo Ng", "1 mutual friend"))) {
      val text = ItemPattern.parts(other, true).joinToString(", ")
      val result = SpeechEdits.apply(text, edits) as SpeechEdits.Result.Speak
      assertEquals(ItemPattern.parts(other, true)[0], result.text)
    }
  }

  @Test
  fun aloneOnScreen_namesAreLeftOut() {
    val query = ItemPattern.likeQuery(suggestion)!!
    assertEquals("Add friend", query.getJSONObject("has").getString("contentDescription"))
    assertEquals(listOf("Add friend", "Remove friend suggestion"), ItemPattern.buttons(suggestion).map { it.title })
    val edit = ItemPattern.removePart(suggestion, true, 1)!!
    assertEquals("'s profile picture$", edit.getString("find"))
    val result = SpeechEdits.apply("Anna Lee, Anna Lee's profile picture, Add friend", JSONArray().put(edit)) as SpeechEdits.Result.Speak
    assertEquals("Anna Lee, Add friend", result.text)
  }

  @Test
  fun aloneOnScreen_buttonWithTheNameIsFoundByTheRest() {
    val row = JSONObject().put("item", friendRow("Natalie Anderson", "3 mutual friends"))
    val buttons = ItemPattern.buttons(row)
    assertEquals(listOf("Add Friend", "Remove"), buttons.map { it.title })
    assertEquals("^Remove", buttons[1].target.getString("matches"))
  }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ItemInspectorSampleTest {
  private fun node(className: String, description: String? = null, text: String? = null, clickable: Boolean = false) =
    android.view.accessibility.AccessibilityNodeInfo.obtain().apply {
      this.className = className
      contentDescription = description
      this.text = text
      isClickable = clickable
      isVisibleToUser = true
    }

  private fun row(list: android.view.accessibility.AccessibilityNodeInfo, name: String): android.view.accessibility.AccessibilityNodeInfo {
    val row = node("android.widget.Button", description = "$name, 2 mutual friends", clickable = true)
    val shadow = org.robolectric.Shadows.shadowOf(row)
    shadow.addChild(node("android.widget.TextView", text = name))
    shadow.addChild(node("android.widget.Button", description = "Add Friend", clickable = true))
    shadow.addChild(node("android.widget.Button", description = "Remove $name", clickable = true))
    org.robolectric.Shadows.shadowOf(list).addChild(row)
    return row
  }

  @Test
  fun sample_hasTheRowsLikeTheInspectedOne() {
    val list = node("androidx.recyclerview.widget.RecyclerView")
    row(list, "Natalie Anderson")
    row(list, "Zoe Murrow")
    row(list, "Sam Lee")
    val inspected = androidx.core.view.accessibility.AccessibilityNodeInfoCompat.wrap(list.getChild(1))
    val sample = ItemInspector.sample(inspected)
    assertEquals("Zoe Murrow, 2 mutual friends", sample.getJSONObject("item").getString("description"))
    assertEquals(2, sample.getJSONArray("similar").length())
    assertEquals(3, sample.getJSONObject("item").getJSONArray("children").length())
    assertEquals(listOf("Add Friend", "Remove"), ItemPattern.buttons(sample).map { it.title })
  }

  @Test
  fun itemsInside_showEachItemsLevelAndClass() {
    val list = node("androidx.recyclerview.widget.RecyclerView")
    val row = androidx.core.view.accessibility.AccessibilityNodeInfoCompat.wrap(row(list, "Allison Broderick"))
    val inside =
      ItemInspector.report(RuntimeEnvironmentHolder.context, row)
        .first { it.title == com.google.android.accessibility.talkback.R.string.item_details_inside }
        .lines
    assertEquals(
      listOf(
        "level 1: TextView; text: Allison Broderick",
        "level 1: Button; clickable; description: Add Friend",
        "level 1: Button; clickable; description: Remove Allison Broderick",
      ),
      inside.map { ItemInspector.lineText(it) },
    )
  }
}

private object RuntimeEnvironmentHolder {
  val context: android.content.Context get() = org.robolectric.RuntimeEnvironment.getApplication()
}
