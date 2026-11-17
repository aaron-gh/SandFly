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
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.macros.MacroRunner
import com.google.android.accessibility.talkback.macros.Macros
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.ClipboardHistory
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import org.json.JSONArray
import org.json.JSONObject

object ItemInspector {
  /**
   * Says and copies the details of [node]: the texts and item types macro tests match, the id and
   * role scripts match, and the texts of the items in it.
   */
  @JvmStatic
  fun inspect(context: Context, node: AccessibilityNodeInfoCompat?): String {
    node ?: return context.getString(R.string.script_inspect_no_item)
    val details =
      (texts(node) +
          listOf(
            "item types" to itemTypes(context, node),
            "id" to node.viewIdResourceName,
            "role" to ScriptRoles.role(node),
            "appRole" to ScriptRoles.appRole(node).takeIf { it != ScriptRoles.role(node) },
            "className" to node.className,
            "app" to node.packageName,
            "activity" to Scripts.currentActivity(),
            "window" to Scripts.currentWindowTitle(),
            "rules" to Scripts.rule(node)?.sources?.joinToString("; "),
          ) +
          innerTexts(node))
        .filterNot { it.second.isNullOrEmpty() }
        .joinToString("\n") { (name, value) -> "$name: $value" }
    context.copyToClipboard(context.getString(R.string.script_inspect_clip_label), details)
    val message = node.viewIdResourceName?.let { R.string.script_inspect_copied }
    return context.getString(message ?: R.string.script_inspect_no_id, details)
  }

  /** The texts of [node] a macro's Match in can name, by the names in its list. */
  private fun texts(node: AccessibilityNodeInfoCompat): List<Pair<String, CharSequence?>> =
    listOf(
      "text" to node.text,
      "description" to node.contentDescription,
      "hint" to node.hintText,
      "state" to node.stateDescription,
    )

  /** The macro item types [node] is, by their titles in a test's Item type list. */
  private fun itemTypes(context: Context, node: AccessibilityNodeInfoCompat): String =
    Macros.Kind.entries
      .filter { MacroRunner.isKind(node.unwrap(), it) }
      .joinToString(", ") { context.getString(it.title) }

  /**
   * The texts of the items in [node], each as "inside text" and so on, up to [MAX_INNER_TEXTS]. A
   * Focused item test matches these too; an Item on screen test matches the inner item itself.
   */
  private fun innerTexts(node: AccessibilityNodeInfoCompat): List<Pair<String, CharSequence?>> {
    val found = mutableListOf<Pair<String, CharSequence?>>()
    val queue = ArrayDeque(node.children())
    var count = 0
    while (queue.isNotEmpty() && count < MAX_INNER_ITEMS && found.size < MAX_INNER_TEXTS) {
      val item = queue.removeFirst()
      count++
      if (!item.isVisibleToUser) continue
      texts(item)
        .filterNot { it.second.isNullOrEmpty() }
        .forEach { (name, value) -> found.add("inside $name" to value) }
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
