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
    assertEquals("^Remove.+", query.getJSONObject("has").getString("matches"))
    assertEquals(1, query.getInt("hasDepth"))
    assertEquals(3, ItemPattern.count(query, friends))
    for (button in listOf(3, 4)) {
      assertEquals(false, ItemPattern.matches(query, friendRow("A B", "2").getJSONArray("children").getJSONObject(button)))
    }
  }

  /** A row of Facebook's Friends tab as Inspect focused item listed it, with the levels. */
  private fun facebookRow(name: String, mutual: String, add: String = "Add Friend", addText: String = "Add friend"): JSONObject =
    node(
      "android.widget.Button",
      description = "$name, $mutual",
      clickable = true,
      children =
        arrayOf(
          node("android.widget.ImageView", description = name),
          node(
            "android.view.ViewGroup",
            children =
              arrayOf(
                node("android.view.ViewGroup", text = name, description = name),
                node("android.view.ViewGroup", text = mutual, description = mutual),
                node("android.widget.Button", description = add, clickable = true, children = arrayOf(node("android.view.ViewGroup", text = addText, description = addText))),
                node("android.widget.Button", description = "Remove $name", clickable = true, children = arrayOf(node("android.view.ViewGroup", text = "Remove", description = "Remove"))),
              ),
          ),
        ),
    )

  @Test
  fun facebookFriendsTab_namesRowsButNotTheirButtons() {
    val heading = node("android.widget.Button", description = "See all", clickable = true, children = arrayOf(node("android.view.ViewGroup", text = "See all")))
    val sample =
      JSONObject()
        .put("item", facebookRow("Chloe McCarthy", "1 mutual friend"))
        .put(
          "similar",
          JSONArray()
            .put(facebookRow("Allison Broderick", "3 mutual friends"))
            .put(facebookRow("Sam Lee", "12 mutual friends", add = "Cancel request", addText = "Cancel"))
            .put(heading),
        )
    val query = ItemPattern.likeQuery(sample)!!
    assertEquals("^Remove.+", query.getJSONObject("has").getString("matches"))
    // The heading row is built differently, so it isn't counted.
    assertEquals(3, ItemPattern.count(query, sample))
    val buttons = ItemPattern.buttons(sample)
    assertEquals(listOf("Add Friend", "Remove"), buttons.map { it.title })
    // Neither button, nor the texts in them, is a row.
    val row = facebookRow("Bo Ng", "2 mutual friends")
    val inside = row.getJSONArray("children").getJSONObject(1).getJSONArray("children")
    for (i in 2..3) {
      val button = inside.getJSONObject(i)
      assertEquals(false, ItemPattern.matches(query, button))
      assertEquals(false, ItemPattern.matches(query, button.getJSONArray("children").getJSONObject(0)))
    }
    assertEquals(true, ItemPattern.matches(query, row))
    // Each action finds its own button in a row.
    assertEquals(true, ItemPattern.matches(buttons[1].target, inside.getJSONObject(3)))
    assertEquals(false, ItemPattern.matches(buttons[1].target, inside.getJSONObject(3).getJSONArray("children").getJSONObject(0)))
  }

  @Test
  fun buttons_areTitledByWhatEveryRowHas() {
    val buttons = ItemPattern.buttons(friends)
    assertEquals(listOf("Add Friend", "Remove"), buttons.map { it.title })
    assertEquals("^Remove.+", buttons[1].target.getString("matches"))
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
    assertEquals(".+'s profile picture$", edit.getString("find"))
    val result = SpeechEdits.apply("Anna Lee, Anna Lee's profile picture, Add friend", JSONArray().put(edit)) as SpeechEdits.Result.Speak
    assertEquals("Anna Lee, Add friend", result.text)
  }

  @Test
  fun aloneOnScreen_buttonWithTheNameIsFoundByTheRest() {
    val row = JSONObject().put("item", friendRow("Natalie Anderson", "3 mutual friends"))
    val buttons = ItemPattern.buttons(row)
    assertEquals(listOf("Add Friend", "Remove"), buttons.map { it.title })
    assertEquals("^Remove.+", buttons[1].target.getString("matches"))
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
  fun sample_withRowsOfFewerItemsBeside_doesNotFail() {
    // The inspected item is the third in its row; the row beside it has one item. Following the
    // inspected item's place into that row asked for a child past its last one, which Android 16
    // answers with an exception.
    val list = node("androidx.recyclerview.widget.RecyclerView")
    val full = node("android.view.ViewGroup")
    for (text in listOf("A", "B", "C")) org.robolectric.Shadows.shadowOf(full).addChild(node("android.widget.TextView", text = text))
    val short = node("android.view.ViewGroup")
    org.robolectric.Shadows.shadowOf(short).addChild(node("android.widget.TextView", text = "D"))
    org.robolectric.Shadows.shadowOf(list).addChild(full)
    org.robolectric.Shadows.shadowOf(list).addChild(short)
    val inspected = androidx.core.view.accessibility.AccessibilityNodeInfoCompat.wrap(list.getChild(0).getChild(2))
    val sample = ItemInspector.sample(inspected)
    assertEquals("C", sample.getJSONObject("item").getString("text"))
  }

  @Test
  fun sample_ofAnItemInAListRow_hasTheRowItIsIn() {
    val list = node("androidx.recyclerview.widget.RecyclerView")
    val title = node("android.view.ViewGroup", text = "Post title")
    org.robolectric.Shadows.shadowOf(list).addChild(title)
    for (name in listOf("Andria Jones", "Bob Smith")) {
      val wrapper = node("android.view.ViewGroup")
      val card = node("android.view.ViewGroup", clickable = true)
      org.robolectric.Shadows.shadowOf(card).addChild(node("android.widget.Button", description = name, clickable = true))
      org.robolectric.Shadows.shadowOf(card).addChild(node("android.widget.Button", description = "Reply to ${name.substringBefore(' ')}'s comment", clickable = true))
      org.robolectric.Shadows.shadowOf(wrapper).addChild(card)
      org.robolectric.Shadows.shadowOf(list).addChild(wrapper)
    }
    // The author's name in the first comment.
    val inspected = androidx.core.view.accessibility.AccessibilityNodeInfoCompat.wrap(list.getChild(1).getChild(0).getChild(0))
    val row = ItemPattern.rowSample(ItemInspector.sample(inspected))!!
    assertEquals(true, row.getJSONObject("item").getBoolean("clickable"))
    // The post's title row is of the same class, but built differently: only the other comment is like it.
    assertEquals(1, ItemPattern.similarCount(row))
    assertEquals(listOf("Reply"), ItemPattern.buttons(row).map { it.title })
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

/** Comments on a Facebook post, from the screen tree the user sent. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FacebookCommentsTest {
  private fun n(cls: String, text: String? = null, description: String? = null, clickable: Boolean = false, longClickable: Boolean = false, actions: List<Pair<Int, String>> = emptyList(), vararg children: JSONObject): JSONObject =
    JSONObject()
      .put("class", "android.view.$cls")
      .putOpt("text", text)
      .putOpt("description", description)
      .apply { if (clickable) put("clickable", true) }
      .apply { if (longClickable) put("longClickable", true) }
      .apply { if (actions.isNotEmpty()) put("actions", JSONArray(actions.map { JSONObject().put("id", it.first).put("label", it.second) })) }
      .apply { if (children.isNotEmpty()) put("children", JSONArray(children.toList())) }

  // The comment itself, inside the list row that holds only it, as the sample keeps it.
  private fun comment(first: String, last: String, body: String, time: String, short: String): JSONObject =
      n("ViewGroup", clickable = true, longClickable = true, children = arrayOf(
        n("ViewGroup", children = arrayOf(n("ViewGroup", children = arrayOf(
          n("Button", text = "$first $last", description = "$first $last", clickable = true),
          n("ViewGroup", clickable = true, longClickable = true, children = arrayOf(n("Button", text = body, description = body))),
        )))),
        n("ViewGroup", children = arrayOf(
          n("ViewGroup", text = time, description = time, children = arrayOf(n("Button", text = short, description = time, clickable = true))),
          n("Button", description = "Like $first's comment button. Double-tap and hold to show reaction tray.", clickable = true, longClickable = true, actions = listOf(32 to "Long-press for more reactions")),
          n("Button", text = "Reply to $first's comment", description = "Reply to $first's comment", clickable = true),
        )),
      ))

  // The rows a post is split into, beside its comments.
  private val postRows =
    JSONArray()
      .put(n("ViewGroup", text = "Great & Little Broughton Crack", description = "Great & Little Broughton Crack", children = arrayOf(n("Button", text = "Great & Little Broughton Crack", clickable = true))))
      .put(n("Button", description = "More options for Amanda McKendrey's post", clickable = true))
      .put(n("ViewGroup", children = arrayOf(n("ViewGroup", children = arrayOf(n("ViewGroup", children = arrayOf(
        n("Button", description = "Like button. Double-tap and hold to react to the comment.", clickable = true, longClickable = true),
        n("Button", text = "Comment, 1 comment", description = "Comment, 1 comment", clickable = true),
        n("Button", description = "Share button. Double-tap to share the post.", clickable = true),
        n("Button", description = "Save post", clickable = true),
      )))))))

  private val sample =
    JSONObject()
      .put("item", comment("Andria", "Jones", "It’s a three way light system, so you can get in, and out of the village 🤩", "6 hours", "6h"))
      .put("similar", JSONArray().put(comment("Bob", "Smith", "Thanks", "2 hours", "2h")).put(comment("Cara", "Lee", "Same here", "1 day", "1d")))
      .put("others", postRows)

  @Test
  fun commentRows_areNamedByWhatOnlyCommentsHave() {
    val query = ItemPattern.likeQuery(sample)!!
    // Comments can be clicked and the rows of the post can't, and a post's Like button starts with
    // "Like" too: neither those rows nor the groups inside a comment are named.
    assertEquals(true, query.getBoolean("clickable"))
    assertEquals(3, ItemPattern.count(query, sample))
    for (i in 0 until postRows.length()) assertEquals(false, ItemPattern.matches(query, postRows.getJSONObject(i)))
    val inside = sample.getJSONObject("item").getJSONArray("children")
    for (i in 0 until inside.length()) assertEquals(false, ItemPattern.matches(query, inside.getJSONObject(i)))
  }

  @Test
  fun commentButtons_areTitledByWhatTheyDo() {
    val buttons = ItemPattern.buttons(sample)
    assertEquals(listOf("Like", "Reply"), buttons.map { it.title })
    assertEquals("Long-press for more reactions", buttons[0].longPressTitle)
  }

  @Test
  fun aGroupedComment_readsItsAuthorTextAndTime() {
    val item = sample.getJSONObject("item")
    val edits = JSONArray()
    for (button in ItemPattern.buttons(sample)) {
      edits.put(JSONObject().put("op", "removeParts").put("find", button.target.getString("matches")).put("regex", true))
    }
    val text = ItemPattern.parts(item, grouped = true).joinToString(", ")
    val read = SpeechEdits.apply(text, edits) as SpeechEdits.Result.Speak
    assertEquals("Andria Jones, It’s a three way light system, so you can get in, and out of the village 🤩, 6 hours", read.text)
  }

  @Test
  fun titles_dropHintsNamesAndButton() {
    assertEquals("Like", ItemPattern.titleOf("Like"))
    assertEquals("Share", ItemPattern.titleOf("Share button. Double-tap to share the post."))
    assertEquals("Reply", ItemPattern.titleOf("Reply to"))
    assertEquals("Save post", ItemPattern.titleOf("Save post"))
  }

  @Test
  fun aloneOnScreen_theNameAndTimeAreNotActions() {
    val alone = JSONObject().put("item", sample.getJSONObject("item"))
    assertEquals(listOf("Like", "Reply"), ItemPattern.buttons(alone).map { it.title })
  }
}
