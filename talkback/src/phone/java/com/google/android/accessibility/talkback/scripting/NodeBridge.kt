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

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.json.JSONArray
import org.json.JSONObject

class NodeBridge(private val showPasswords: Boolean) {
  private val nodes = LinkedHashMap<Int, AccessibilityNodeInfoCompat>(64, 0.75f, true)
  private val handles = HashMap<AccessibilityNodeInfoCompat, Int>()
  private var nextHandle = 1

  fun node(handle: Int): AccessibilityNodeInfoCompat =
    nodes[handle] ?: apiError("That item is no longer available")

  fun snapshot(node: AccessibilityNodeInfoCompat?): Any {
    node ?: return JSONObject.NULL
    val bounds = Rect().also(node::getBoundsInScreen)
    return jsonObject(
      "handle" to handleFor(node),
      "text" to textOf(node),
      "contentDescription" to node.contentDescription?.toString(),
      "hint" to node.hintText?.toString(),
      "stateDescription" to node.stateDescription?.toString(),
      "id" to node.viewIdResourceName,
      "className" to node.className?.toString(),
      "role" to ScriptRoles.appRole(node),
      "packageName" to node.packageName?.toString(),
      "windowId" to node.windowId,
      "bounds" to
        jsonObject(
          "left" to bounds.left,
          "top" to bounds.top,
          "right" to bounds.right,
          "bottom" to bounds.bottom,
        ),
      "childCount" to node.childCount,
      "selectionStart" to node.textSelectionStart,
      "selectionEnd" to node.textSelectionEnd,
      "checkable" to node.isCheckable,
      "checked" to node.isChecked,
      "clickable" to node.isClickable,
      "longClickable" to node.isLongClickable,
      "focusable" to node.isFocusable,
      "focused" to node.isFocused,
      "accessibilityFocused" to node.isAccessibilityFocused,
      "selected" to node.isSelected,
      "enabled" to node.isEnabled,
      "editable" to node.isEditable,
      "password" to node.isPassword,
      "scrollable" to node.isScrollable,
      "visible" to node.isVisibleToUser,
      "heading" to node.isHeading,
      "actions" to actionsOf(node),
    )
  }

  fun children(handle: Int): JSONArray = JSONArray(node(handle).children().map(::snapshot))

  fun find(
    root: AccessibilityNodeInfoCompat,
    query: JSONObject,
    limit: Int,
    deadline: Long,
  ): JSONArray = JSONArray(NodeQuery.of(query).findIn(root, limit, ::textOf, deadline).map(::snapshot))

  fun textOf(node: AccessibilityNodeInfoCompat): String? =
    node.text?.toString().takeUnless { node.isPassword && !showPasswords }

  private fun actionsOf(node: AccessibilityNodeInfoCompat): JSONArray =
    JSONArray(
      node.actionList.mapNotNull { action ->
        (action.label?.toString() ?: NAMES_BY_ID[action.id])?.let {
          jsonObject("id" to action.id, "label" to it)
        }
      }
    )

  private fun handleFor(node: AccessibilityNodeInfoCompat): Int {
    handles[node]?.takeIf { it in nodes }?.let { existing ->
      nodes[existing] = node
      return existing
    }
    val handle = nextHandle++
    nodes[handle] = node
    handles[node] = handle
    while (nodes.size > MAX_HANDLES) {
      val (eldestHandle, eldest) = nodes.entries.first()
      nodes.remove(eldestHandle)
      handles.remove(eldest)
    }
    return handle
  }

  companion object {
    private const val MAX_HANDLES = 500

    val ACTIONS: Map<String, Int> =
      mapOf(
        "click" to AccessibilityNodeInfo.ACTION_CLICK,
        "longClick" to AccessibilityNodeInfo.ACTION_LONG_CLICK,
        "scrollForward" to AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,
        "scrollBackward" to AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,
        "expand" to AccessibilityNodeInfo.ACTION_EXPAND,
        "collapse" to AccessibilityNodeInfo.ACTION_COLLAPSE,
        "dismiss" to AccessibilityNodeInfo.ACTION_DISMISS,
        "copy" to AccessibilityNodeInfo.ACTION_COPY,
        "cut" to AccessibilityNodeInfo.ACTION_CUT,
        "paste" to AccessibilityNodeInfo.ACTION_PASTE,
        "select" to AccessibilityNodeInfo.ACTION_SELECT,
      )
    private val NAMES_BY_ID = ACTIONS.entries.associate { (name, id) -> id to name }

    fun actionId(node: AccessibilityNodeInfoCompat, action: Any?): Int =
      when (action) {
        is Number -> action.toInt()
        is String ->
          ACTIONS[action]
            ?: node.actionList.firstOrNull { it.label?.toString().equals(action, true) }?.id
        else -> null
      } ?: apiError("Unknown action $action")
  }
}
