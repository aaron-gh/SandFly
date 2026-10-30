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

import android.os.SystemClock
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.json.JSONArray
import org.json.JSONObject

typealias TextOf = (AccessibilityNodeInfoCompat) -> String?

data class NodeQuery(
  val id: String? = null,
  val text: String? = null,
  val textContains: String? = null,
  val contentDescription: String? = null,
  val className: String? = null,
  val role: String? = null,
  val clickable: Boolean? = null,
  val parentId: String? = null,
  /** Queries that each match one of the items around this one. */
  val inside: List<NodeQuery> = emptyList(),
  val has: NodeQuery? = null,
  /** Wanted values of the item's states, such as checked = true. */
  val states: Map<String, Boolean> = emptyMap(),
) {
  fun matches(node: AccessibilityNodeInfoCompat, textOf: TextOf = PLAIN_TEXT): Boolean =
    (id == null || idMatches(id, node.viewIdResourceName)) &&
      (clickable == null || node.isClickable == clickable) &&
      states.all { (state, wanted) -> STATES.getValue(state)(node) == wanted } &&
      (className == null || node.className?.toString()?.endsWith(className) == true) &&
      (contentDescription == null || node.contentDescription?.toString() == contentDescription) &&
      (text == null || textOf(node) == text) &&
      (textContains == null || contains(node, textOf, textContains)) &&
      (role == null || ScriptRoles.appRole(node) == role) &&
      (parentId == null || idMatches(parentId, node.parent?.viewIdResourceName)) &&
      // Last, because every ancestor is a call to the app.
      (inside.isEmpty() || isInside(node, textOf)) &&
      // After that, because it can look at many more items.
      (has == null || hasInside(node, has, textOf))

  /**
   * Finds items under [root], and stops early at [deadline], a [SystemClock.uptimeMillis] time.
   * Every item visited is a call to the app, so a large screen can take longer than a script is
   * given.
   */
  fun findIn(
    root: AccessibilityNodeInfoCompat,
    limit: Int = 1,
    textOf: TextOf = PLAIN_TEXT,
    deadline: Long = Long.MAX_VALUE,
  ): List<AccessibilityNodeInfoCompat> = buildList {
    val stack = ArrayDeque(listOf(root))
    var visited = 0
    while (stack.isNotEmpty() && visited++ < MAX_VISITED && size < limit) {
      if (SystemClock.uptimeMillis() > deadline) break
      val node = stack.removeLast()
      if (matches(node, textOf)) add(node)
      stack += node.children().asReversed()
    }
  }

  fun firstIn(
    root: AccessibilityNodeInfoCompat,
    deadline: Long = Long.MAX_VALUE,
  ): AccessibilityNodeInfoCompat? = findIn(root, deadline = deadline).firstOrNull()

  /** Whether each query in [inside] matches one of the items around [node], walking them once. */
  private fun isInside(node: AccessibilityNodeInfoCompat, textOf: TextOf): Boolean {
    val left = inside.toMutableList()
    for (ancestor in node.ancestors()) {
      left.removeAll { it.matches(ancestor, textOf) }
      if (left.isEmpty()) return true
    }
    return false
  }

  /**
   * Whether an item inside [node], up to [MAX_HAS_DEPTH] levels down, matches [query]. It looks at
   * no more than [MAX_HAS_VISITED] items, nearest first, since each is a call to the app.
   */
  private fun hasInside(node: AccessibilityNodeInfoCompat, query: NodeQuery, textOf: TextOf): Boolean {
    var level = node.children()
    var visited = 0
    repeat(MAX_HAS_DEPTH) {
      val next = mutableListOf<AccessibilityNodeInfoCompat>()
      for (child in level) {
        if (++visited > MAX_HAS_VISITED) return false
        if (query.matches(child, textOf)) return true
        next += child.children()
      }
      if (next.isEmpty()) return false
      level = next
    }
    return false
  }

  private fun contains(node: AccessibilityNodeInfoCompat, textOf: TextOf, part: String): Boolean =
    listOf(textOf(node), node.contentDescription?.toString()).any {
      it?.contains(part, ignoreCase = true) == true
    }

  companion object {
    private const val MAX_VISITED = 3000
    private const val MAX_HAS_DEPTH = 6
    private const val MAX_HAS_VISITED = 60
    private val PLAIN_TEXT: TextOf = { it.text?.toString() }

    private val STATES: Map<String, (AccessibilityNodeInfoCompat) -> Boolean> =
      mapOf(
        "checkable" to { it.isCheckable },
        "checked" to { it.isChecked },
        "longClickable" to { it.isLongClickable },
        "focusable" to { it.isFocusable },
        "focused" to { it.isFocused },
        "selected" to { it.isSelected },
        "enabled" to { it.isEnabled },
        "editable" to { it.isEditable },
        "scrollable" to { it.isScrollable },
        "visible" to { it.isVisibleToUser },
        "heading" to { it.isHeading },
        "password" to { it.isPassword },
      )

    val KEYS =
      STATES.keys +
      setOf(
        "id",
        "text",
        "textContains",
        "contentDescription",
        "className",
        "role",
        "clickable",
        "parentId",
        "inside",
        "has",
      )

    fun of(json: JSONObject, nested: Boolean = false): NodeQuery {
      fun text(key: String) = json.optString(key).ifEmpty { null }
      return NodeQuery(
        id = text("id"),
        text = text("text"),
        textContains = text("textContains"),
        contentDescription = text("contentDescription"),
        className = text("className"),
        role = text("role"),
        clickable = if (json.has("clickable")) json.getBoolean("clickable") else null,
        parentId = text("parentId"),
        // One level: each further level would walk every ancestor again for every ancestor.
        inside = if (nested) emptyList() else insideOf(json.opt("inside")),
        // One level too: a nested one would search everything inside every item it looks at.
        has = if (nested) null else json.optJSONObject("has")?.let { of(it, nested = true) },
        states = STATES.keys.filter(json::has).associateWith(json::getBoolean),
      )
    }

    /** inside is one query, or a list of them. */
    private fun insideOf(value: Any?): List<NodeQuery> =
      when (value) {
        is JSONObject -> listOf(of(value, nested = true))
        is JSONArray ->
          (0 until value.length()).mapNotNull { value.optJSONObject(it)?.let { of(it, nested = true) } }
        else -> emptyList()
      }

    fun idMatches(wanted: String, actual: String?): Boolean =
      actual != null &&
        if (':' in wanted) wanted == actual else actual.substringAfter(":id/", actual) == wanted
  }
}
