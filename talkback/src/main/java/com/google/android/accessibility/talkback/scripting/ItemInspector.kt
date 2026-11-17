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
        Section(R.string.item_details_inside, innerTexts(node)),
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
    val activeRoot = (context as? AccessibilityService)?.rootInActiveWindow
    val root =
      node?.let(AccessibilityNodeInfoUtils::getRoot)
        ?: activeRoot?.let(AccessibilityNodeInfoCompat::wrap)
        ?: return context.getString(R.string.script_tree_none)
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
    context.copyToClipboard(context.getString(R.string.script_tree_clip_label), tree.toString(1))
    return context.getString(R.string.script_tree_copied, count)
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
  private const val MAX_DEPTH = 60
  private const val MAX_ITEMS = 2000
  /** The most items in the inspected item whose texts Inspect focused item reads. */
  private const val MAX_INNER_ITEMS = 30
  /** The most texts of items in the inspected item that Inspect focused item gives. */
  private const val MAX_INNER_TEXTS = 10

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
