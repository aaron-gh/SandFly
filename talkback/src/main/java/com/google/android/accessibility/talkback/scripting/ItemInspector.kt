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
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import java.util.concurrent.Executors
import java.util.function.Consumer
import org.json.JSONArray
import org.json.JSONObject

object ItemInspector {
  @JvmStatic
  fun inspect(context: Context, node: AccessibilityNodeInfoCompat?): String {
    node ?: return context.getString(R.string.script_inspect_no_item)
    val details =
      listOf(
          "id" to node.viewIdResourceName,
          "className" to node.className,
          "role" to ScriptRoles.role(node),
          "appRole" to ScriptRoles.appRole(node).takeIf { it != ScriptRoles.role(node) },
          "app" to node.packageName,
          "activity" to Scripts.currentActivity(),
          "window" to Scripts.currentWindowTitle(),
          "text" to AccessibilityNodeInfoUtils.getText(node),
          "contentDescription" to node.contentDescription,
          "rules" to Scripts.rule(node)?.sources?.joinToString("; "),
        )
        .filterNot { it.second.isNullOrEmpty() }
        .joinToString("\n") { (name, value) -> "$name: $value" }
    context.copyToClipboard(context.getString(R.string.script_inspect_clip_label), details)
    val message = node.viewIdResourceName?.let { R.string.script_inspect_copied }
    return context.getString(message ?: R.string.script_inspect_no_id, details)
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

  private val treeReader by lazy { Executors.newSingleThreadExecutor() }
  private const val MAX_DEPTH = 60
  private const val MAX_ITEMS = 2000

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

fun Context.copyToClipboard(label: CharSequence, text: CharSequence): Boolean =
  getSystemService(ClipboardManager::class.java)
    ?.setPrimaryClip(ClipData.newPlainText(label, text)) != null
