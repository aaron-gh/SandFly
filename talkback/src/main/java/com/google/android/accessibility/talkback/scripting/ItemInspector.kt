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

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.StringRes
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.macros.MacroRunner
import com.google.android.accessibility.talkback.macros.Macros
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.ClipboardHistory
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import org.json.JSONArray
import org.json.JSONObject

object ItemInspector {
  /** A part of an item's details: a heading and its lines, each a name and a value. */
  data class Section(@StringRes val title: Int, val lines: List<Pair<String, String>>)

  /**
   * Opens a window with the details of [node], under a heading for each part. Returns what to say
   * instead when there is no item.
   */
  @JvmStatic
  fun show(context: Context, node: AccessibilityNodeInfoCompat?): String? {
    node ?: return context.getString(R.string.script_inspect_no_item)
    context.startActivity(ItemDetailsActivity.intent(context, report(context, node)))
    return null
  }

  /** Copies the details of [node] as text, and returns what to say. */
  @JvmStatic
  fun copy(context: Context, node: AccessibilityNodeInfoCompat?): String {
    node ?: return context.getString(R.string.script_inspect_no_item)
    context.copyToClipboard(
      context.getString(R.string.script_inspect_clip_label),
      plainText(context, report(context, node)),
    )
    return context.getString(R.string.item_details_copied_all)
  }

  /**
   * The details of [node]: the texts and item types macro tests match, the texts of the items in
   * it, the id and role scripts match, and the app it is in. Parts with nothing in them are left
   * out.
   */
  fun report(context: Context, node: AccessibilityNodeInfoCompat): List<Section> {
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putString(PREF_LAST_QUERY, scriptQuery(node).toString())
      .putString(PREF_LAST_APP, node.packageName?.toString())
      .putString(PREF_LAST_TEXT, spokenText(node))
      .putString(PREF_LAST_SAMPLE, sample(node).toString())
      .apply()
    val role = ScriptRoles.role(node)
    val script =
      lines(
        "id" to node.viewIdResourceName,
        "role" to role,
        "appRole" to ScriptRoles.appRole(node).takeIf { it != role },
        "className" to node.className,
        "kind" to kinds(node).joinToString(", ") { it.scriptName },
        "rules" to Scripts.rule(node)?.sources?.joinToString("; "),
      )
    return listOf(
        Section(R.string.item_details_texts, lines(*texts(node).toTypedArray())),
        Section(R.string.item_details_types, itemTypes(context, node).map { "" to it }),
        Section(R.string.item_details_inside, innerTree(node)),
        Section(R.string.item_details_script, script),
        Section(
          R.string.item_details_app,
          lines(
            "app" to node.packageName,
            "activity" to Scripts.currentActivity(),
            "window" to Scripts.currentWindowTitle(),
          ),
        ),
      )
      .filter { it.lines.isNotEmpty() }
  }

  /**
   * A script query for [node]: its id when it has one, or else its description or text, and
   * Clickable or Heading when it is one.
   */
  fun scriptQuery(node: AccessibilityNodeInfoCompat): JSONObject {
    val query = JSONObject()
    val id = node.viewIdResourceName?.substringAfter(":id/")
    val description = node.contentDescription?.toString()?.trim().orEmpty()
    val text = node.text?.toString()?.trim().orEmpty()
    when {
      !id.isNullOrEmpty() -> query.put("id", id)
      description.isNotEmpty() -> query.put("contains", description).put("field", "contentDescription")
      text.isNotEmpty() && !node.isPassword -> query.put("contains", text).put("field", "text")
      // A row whose texts are items in it, such as a settings row: the first of them names it.
      else ->
        innerTexts(node).firstOrNull { (name, _) -> name == "text" || name == "description" }?.let { (name, value) ->
          val field = if (name == "text") "text" else "contentDescription"
          query.put("has", JSONObject().put("contains", value).put("field", field))
        }
    }
    val kinds = listOfNotNull("clickable".takeIf { node.isClickable }, "heading".takeIf { node.isHeading })
    when (kinds.size) {
      0 -> {}
      1 -> query.put("kind", kinds[0])
      else -> query.put("kind", JSONArray(kinds))
    }
    return query
  }

  /** The script query of the item last inspected, or null. */
  @JvmStatic
  fun lastQuery(context: Context): JSONObject? =
    SharedPreferencesUtils.getSharedPreferences(context).getString(PREF_LAST_QUERY, null)?.let {
      try {
        JSONObject(it).takeIf { query -> query.length() > 0 }
      } catch (e: org.json.JSONException) {
        null
      }
    }

  /**
   * The texts of [node] as SandFly joins them: its description or text, or else the texts of the
   * items in it, separated by commas. For trying speech edits on.
   */
  private fun spokenText(node: AccessibilityNodeInfoCompat): String? {
    val own = (node.contentDescription?.takeIf { it.isNotBlank() } ?: node.text)?.toString()?.trim()
    if (!own.isNullOrEmpty()) return own
    return innerTexts(node).map { it.second }.distinct().joinToString(", ").ifEmpty { null }
  }

  /**
   * [node] and the items like it around it, such as the other rows of its list, with the items in
   * each. The script builder finds what they have in common, to name them all in one rule.
   */
  fun sample(node: AccessibilityNodeInfoCompat): JSONObject {
    val sample = JSONObject().put("item", sampleItem(node)).put("similar", JSONArray(similarItems(node).map(::sampleItem)))
    // The row of a list the item is in, such as a comment holding its author, text and buttons:
    // with the rows beside it, those of its class to compare with and the others, such as the rows
    // a post is split into, which a rule for the row mustn't name.
    rowOf(node)?.let { wrapper ->
      val parent = wrapper.parent
      // A row is often wrapped in items holding only it; the innermost of them is the row.
      val row = unwrapped(wrapper)
      if (row == node) return@let
      val siblings = parent?.childrenInReadingOrder()?.filter { it.isVisibleToUser && it != wrapper }?.map(::unwrapped)?.toList().orEmpty()
      val (alike, others) = siblings.partition { it.className == row.className }
      sample.put(
        "row",
        JSONObject()
          .put("item", sampleItem(row))
          .put("similar", JSONArray(alike.take(MAX_SIMILAR).map(::sampleItem)))
          .put("others", JSONArray(others.take(MAX_SIMILAR).map { sampleItem(it, OTHERS_DEPTH) })),
      )
    }
    return sample
  }

  /** [node], or the innermost item in it that holds only the next, as long as none has a text. */
  private fun unwrapped(node: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat {
    var item = node
    repeat(MAX_ROW_LEVELS) {
      if (!item.text.isNullOrEmpty() || !item.contentDescription.isNullOrEmpty()) return item
      val shown = item.childrenInReadingOrder().filter { it.isVisibleToUser }.take(2).toList()
      item = shown.singleOrNull() ?: return item
    }
    return item
  }

  /** The item around [node], or [node], that is a row of a list, or null when it isn't in one. */
  private fun rowOf(node: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat? =
    (sequenceOf(node) + node.ancestors().take(MAX_ROW_LEVELS)).firstOrNull { item -> item.parent?.let(::isList) == true }

  private fun isList(node: AccessibilityNodeInfoCompat): Boolean {
    val className = node.className?.toString().orEmpty()
    return node.collectionInfo != null || LIST_CLASSES.any { className.endsWith(it) }
  }

  /**
   * The items in the same place as [node] under its parent, or else under an item further up: the
   * other rows of a list, even when each row is wrapped in an item of its own.
   */
  private fun similarItems(node: AccessibilityNodeInfoCompat): List<AccessibilityNodeInfoCompat> {
    val className = node.className ?: return emptyList()
    val path = mutableListOf<Int>()
    var child = node
    for (level in 1..MAX_SIMILAR_LEVELS) {
      val parent = child.parent ?: return emptyList()
      val index = (0 until parent.childCount).firstOrNull { parent.getChild(it) == child } ?: return emptyList()
      path.add(0, index)
      val found =
        (0 until parent.childCount)
          .asSequence()
          .filter { it != index }
          .mapNotNull { start ->
            path.drop(1).fold(parent.getChild(start) as AccessibilityNodeInfoCompat?) { item, i -> item?.getChild(i) }
          }
          .filter { it.className == className && it.isVisibleToUser }
          .take(MAX_SIMILAR)
          .toList()
      if (found.isNotEmpty()) return found
      child = parent
    }
    return emptyList()
  }

  /** [node] as JSON: its class, id, texts and whether it can be clicked, and the items in it. */
  @JvmStatic
  fun snapshot(node: AccessibilityNodeInfoCompat, maxDepth: Int = MAX_SAMPLE_DEPTH): JSONObject = sampleItem(node, maxDepth)

  private fun sampleItem(node: AccessibilityNodeInfoCompat, maxDepth: Int = MAX_SAMPLE_DEPTH): JSONObject {
    var count = 0
    fun describe(item: AccessibilityNodeInfoCompat, depth: Int): JSONObject =
      JSONObject().apply {
        count++
        putOpt("class", item.className?.toString())
        putOpt("id", item.viewIdResourceName?.substringAfter(":id/"))
        putOpt("text", item.text?.toString()?.trim()?.takeIf { it.isNotEmpty() && !item.isPassword })
        putOpt("description", item.contentDescription?.toString()?.trim()?.ifEmpty { null })
        if (item.isClickable) put("clickable", true)
        if (item.isLongClickable) put("longClickable", true)
        if (AccessibilityNodeInfoUtils.isHeading(item)) put("heading", true)
        // Actions the app named, such as "Long-press for more reactions" on a Like button.
        putList("actions", item.actionList.mapNotNull { action -> action.label?.toString()?.let { JSONObject().put("id", action.id).put("label", it) } })
        if (depth < maxDepth) {
          val children =
            item.childrenInReadingOrder().filter { it.isVisibleToUser }.takeWhile { count < MAX_SAMPLE_ITEMS }
          putList("children", children.map { describe(it, depth + 1) }.toList())
        }
      }
    return describe(node, 0)
  }

  /** What [sample] saved for the item last inspected, or null. */
  @JvmStatic
  fun lastSample(context: Context): JSONObject? =
    SharedPreferencesUtils.getSharedPreferences(context).getString(PREF_LAST_SAMPLE, null)?.let {
      try {
        JSONObject(it)
      } catch (e: org.json.JSONException) {
        null
      }
    }

  /** The text of the item last inspected, as SandFly joins its texts, or null. */
  @JvmStatic
  fun lastText(context: Context): String? =
    SharedPreferencesUtils.getSharedPreferences(context).getString(PREF_LAST_TEXT, null)

  /** The package of the app of the item last inspected, or null. */
  @JvmStatic
  fun lastApp(context: Context): String? =
    SharedPreferencesUtils.getSharedPreferences(context).getString(PREF_LAST_APP, null)

  /** [sections] as text: each heading, then its lines, with a blank line between parts. */
  fun plainText(context: Context, sections: List<Section>): String =
    sections.joinToString("\n\n") { section ->
      (listOf(context.getString(section.title)) + section.lines.map(::lineText)).joinToString("\n")
    }

  /** A line of a part: "name: value", or the value alone when it has no name. */
  fun lineText(line: Pair<String, String>): String =
    if (line.first.isEmpty()) line.second else "${line.first}: ${line.second}"

  private fun lines(vararg values: Pair<String, CharSequence?>): List<Pair<String, String>> =
    values.mapNotNull { (name, value) -> value?.toString()?.takeIf { it.isNotEmpty() }?.let { name to it } }

  /** The texts of [node] a macro's Match in can name, by the names in its list. */
  private fun texts(node: AccessibilityNodeInfoCompat): List<Pair<String, CharSequence?>> =
    listOf(
      "text" to node.text,
      "description" to node.contentDescription,
      "hint" to node.hintText,
      "state" to node.stateDescription,
    )

  /** The macro item types [node] is, by their titles in a test's Item type list. */
  private fun itemTypes(context: Context, node: AccessibilityNodeInfoCompat): List<String> =
    kinds(node).map { context.getString(it.title) }

  private fun kinds(node: AccessibilityNodeInfoCompat): List<Macros.Kind> =
    Macros.Kind.entries.filter { MacroRunner.isKind(node.unwrap(), it) }

  /**
   * The texts of the items in [node], up to [MAX_INNER_TEXTS]. A Focused item test matches these
   * too; an Item on screen test matches the inner item itself.
   */
  private fun innerTexts(node: AccessibilityNodeInfoCompat): List<Pair<String, String>> {
    val found = mutableListOf<Pair<String, String>>()
    val queue = ArrayDeque(node.children())
    var count = 0
    while (queue.isNotEmpty() && count < MAX_INNER_ITEMS && found.size < MAX_INNER_TEXTS) {
      val item = queue.removeFirst()
      count++
      if (!item.isVisibleToUser) continue
      found.addAll(lines(*texts(item).toTypedArray()))
      queue.addAll(item.children())
    }
    return found.take(MAX_INNER_TEXTS)
  }

  /**
   * The items in [node] in reading order, each under the item it is in: its level (1 for the items
   * directly in [node]), class, whether it can be clicked, and its texts. Items with no text that
   * can't be clicked are listed when items are in them, so the levels show what holds what.
   */
  private fun innerTree(node: AccessibilityNodeInfoCompat): List<Pair<String, String>> {
    val lines = mutableListOf<Pair<String, String>>()
    var count = 0
    fun visit(item: AccessibilityNodeInfoCompat, level: Int) {
      for (child in item.childrenInReadingOrder()) {
        if (count >= MAX_TREE_ITEMS || lines.size >= MAX_TREE_LINES) return
        count++
        if (!child.isVisibleToUser) continue
        val texts = texts(child).mapNotNull { (name, value) -> value?.toString()?.takeIf { it.isNotEmpty() }?.let { "$name: $it" } }
        val clickable = listOfNotNull("clickable".takeIf { child.isClickable }, "longClickable".takeIf { child.isLongClickable })
        if (texts.isNotEmpty() || clickable.isNotEmpty() || child.childCount > 0) {
          val parts = listOfNotNull(child.className?.toString()?.substringAfterLast('.')) + clickable + texts
          lines += "level $level" to parts.joinToString("; ")
        }
        if (level < MAX_TREE_LEVEL) visit(child, level + 1)
      }
    }
    visit(node, 1)
    return lines
  }

  /**
   * Copies every item on screen as JSON, then gives [done] what to say. Reading the screen asks the
   * app about each item in turn, so it happens off the main thread.
   */
  @JvmStatic
  fun copyScreenTree(context: Context, node: AccessibilityNodeInfoCompat?, done: Consumer<String>) {
    val main = Handler(Looper.getMainLooper())
    treeReader.execute {
      val message =
        try {
          readScreenTree(context, node)
        } catch (e: RuntimeException) {
          context.getString(R.string.script_tree_none)
        }
      main.post { done.accept(message) }
    }
  }

  private fun readScreenTree(context: Context, node: AccessibilityNodeInfoCompat?): String {
    val (tree, count) = screenTree(context, node) ?: return context.getString(R.string.script_tree_none)
    context.copyToClipboard(context.getString(R.string.script_tree_clip_label), tree.toString(1))
    return context.getString(R.string.script_tree_copied, count)
  }

  /**
   * Every item of the window of [node], or of the active window, as JSON, with how many items it
   * has; or null when there is no window. Asks the app about each item in turn, so it is called off
   * the main thread.
   */
  @JvmStatic
  fun screenTree(context: Context, node: AccessibilityNodeInfoCompat?): Pair<JSONObject, Int>? {
    val activeRoot = (context as? AccessibilityService)?.rootInActiveWindow
    val root =
      node?.let(AccessibilityNodeInfoUtils::getRoot)
        ?: activeRoot?.let(AccessibilityNodeInfoCompat::wrap)
        ?: return null
    var count = 0
    fun describe(item: AccessibilityNodeInfoCompat, depth: Int): JSONObject =
      JSONObject().apply {
        count++
        putOpt("id", item.viewIdResourceName)
        putOpt("class", item.className?.toString()?.substringAfterLast('.'))
        putOpt("role", ScriptRoles.role(item).takeIf { it != "none" })
        putOpt("text", AccessibilityNodeInfoUtils.getText(item)?.toString())
        putOpt("description", item.contentDescription?.toString())
        putOpt("hint", item.hintText?.toString())
        putOpt("state", item.stateDescription?.toString())
        putList("flags", FLAGS.filter { (_, test) -> test(item) }.map { it.first })
        putList("actions", item.actionList.mapNotNull { it.label?.toString() })
        putList("rules", Scripts.rule(item)?.sources.orEmpty())
        if (depth < MAX_DEPTH) {
          val children = item.children().asSequence().takeWhile { count < MAX_ITEMS }
          putList("children", children.map { describe(it, depth + 1) }.toList())
        }
      }
    val tree =
      JSONObject()
        .putOpt("app", root.packageName?.toString())
        .putOpt("activity", Scripts.currentActivity())
        .putOpt("window", Scripts.currentWindowTitle())
        .put("root", describe(root, 0))
    return tree to count
  }

  private fun JSONObject.putList(key: String, values: List<Any>) {
    if (values.isNotEmpty()) put(key, JSONArray(values))
  }

  private val treeReader by lazy {
    ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>())
  }
  private const val PREF_LAST_QUERY = "item_inspector_last_query"
  private const val PREF_LAST_APP = "item_inspector_last_app"
  private const val PREF_LAST_TEXT = "item_inspector_last_text"
  private const val PREF_LAST_SAMPLE = "item_inspector_last_sample"
  /** How far up from the inspected item items like it are looked for. */
  private const val MAX_SIMILAR_LEVELS = 3
  /** The most items like the inspected item kept. */
  private const val MAX_SIMILAR = 8
  /** How deep, and how many items, of each item are kept. */
  private const val MAX_SAMPLE_DEPTH = 5
  private const val MAX_SAMPLE_ITEMS = 40
  /** How far up from the inspected item a list row is looked for. */
  private const val MAX_ROW_LEVELS = 10
  /** How deep the rows beside a row that aren't like it are kept. */
  private const val OTHERS_DEPTH = 3
  private val LIST_CLASSES = listOf("RecyclerView", "ListView", "GridView")
  private const val MAX_DEPTH = 60
  private const val MAX_ITEMS = 2000
  /** The most items in the inspected item whose texts Inspect focused item reads. */
  private const val MAX_INNER_ITEMS = 30
  /** The most texts of items in the inspected item that Inspect focused item gives. */
  private const val MAX_INNER_TEXTS = 10
  /** How many items in the inspected item Items inside looks at and lists, and how deep. */
  private const val MAX_TREE_ITEMS = 120
  private const val MAX_TREE_LINES = 60
  private const val MAX_TREE_LEVEL = 8

  private val FLAGS: List<Pair<String, (AccessibilityNodeInfoCompat) -> Boolean>> =
    listOf(
      "clickable" to { it.isClickable },
      "longClickable" to { it.isLongClickable },
      "focusable" to { it.isFocusable },
      "checkable" to { it.isCheckable },
      "checked" to { it.isChecked },
      "selected" to { it.isSelected },
      "editable" to { it.isEditable },
      "scrollable" to { it.isScrollable },
      "heading" to AccessibilityNodeInfoUtils::isHeading,
      "password" to { it.isPassword },
      "focused" to { it.isAccessibilityFocused },
      "offscreen" to { !it.isVisibleToUser },
      "disabled" to { !it.isEnabled },
    )
}

@Suppress("UNUSED_PARAMETER")
fun Context.copyToClipboard(label: CharSequence, text: CharSequence): Boolean =
  ClipboardHistory.copy(this, text)
