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

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Bundle
import android.view.View
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
      "roleDescription" to node.roleDescription?.toString(),
      "error" to node.error?.toString(),
      "tooltip" to node.tooltipText?.toString(),
      "paneTitle" to node.paneTitle?.toString(),
      "containerTitle" to node.containerTitle?.toString(),
      "uniqueId" to node.uniqueId,
      "multiLine" to node.isMultiLine,
      "maxTextLength" to node.maxTextLength.takeIf { it >= 0 },
      "inputType" to node.inputType,
      "dismissable" to node.isDismissable,
      "contentInvalid" to node.isContentInvalid,
      "textSelectable" to node.isTextSelectable,
      "importantForAccessibility" to node.isImportantForAccessibility,
      "screenReaderFocusable" to node.isScreenReaderFocusable,
      "liveRegion" to LIVE_REGIONS[node.liveRegion],
      "drawingOrder" to node.drawingOrder,
      "range" to rangeOf(node),
      "collection" to collectionOf(node),
      "collectionItem" to collectionItemOf(node),
    )
  }

  private fun rangeOf(node: AccessibilityNodeInfoCompat): Any? =
    node.rangeInfo?.let {
      jsonObject(
        "min" to it.min.toDouble(),
        "max" to it.max.toDouble(),
        "current" to it.current.toDouble(),
        "type" to RANGE_TYPES[it.type],
      )
    }

  private fun collectionOf(node: AccessibilityNodeInfoCompat): Any? =
    node.collectionInfo?.let {
      jsonObject(
        "rows" to it.rowCount.takeIf { n -> n >= 0 },
        "columns" to it.columnCount.takeIf { n -> n >= 0 },
        "hierarchical" to it.isHierarchical,
      )
    }

  private fun collectionItemOf(node: AccessibilityNodeInfoCompat): Any? =
    node.collectionItemInfo?.let {
      jsonObject(
        "row" to it.rowIndex + 1,
        "column" to it.columnIndex + 1,
        "rowSpan" to it.rowSpan,
        "columnSpan" to it.columnSpan,
        "heading" to it.isHeading,
        "rowTitle" to it.rowTitle,
        "columnTitle" to it.columnTitle,
      )
    }

  fun labeledBy(handle: Int): Any = snapshot(node(handle).labeledBy)

  fun labelFor(handle: Int): Any = snapshot(node(handle).labelFor)

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

  @SuppressLint("InlinedApi")
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
        "clearSelection" to AccessibilityNodeInfo.ACTION_CLEAR_SELECTION,
        "focusInput" to AccessibilityNodeInfo.ACTION_FOCUS,
        "clearFocus" to AccessibilityNodeInfo.ACTION_CLEAR_FOCUS,
        "showOnScreen" to android.R.id.accessibilityActionShowOnScreen,
        "scrollUp" to android.R.id.accessibilityActionScrollUp,
        "scrollDown" to android.R.id.accessibilityActionScrollDown,
        "scrollLeft" to android.R.id.accessibilityActionScrollLeft,
        "scrollRight" to android.R.id.accessibilityActionScrollRight,
        "pageUp" to android.R.id.accessibilityActionPageUp,
        "pageDown" to android.R.id.accessibilityActionPageDown,
        "pageLeft" to android.R.id.accessibilityActionPageLeft,
        "pageRight" to android.R.id.accessibilityActionPageRight,
        "contextClick" to android.R.id.accessibilityActionContextClick,
        "imeEnter" to android.R.id.accessibilityActionImeEnter,
        "pressAndHold" to android.R.id.accessibilityActionPressAndHold,
        "showTooltip" to android.R.id.accessibilityActionShowTooltip,
        "hideTooltip" to android.R.id.accessibilityActionHideTooltip,
        "showTextSuggestions" to android.R.id.accessibilityActionShowTextSuggestions,
      )

    private val LIVE_REGIONS =
      mapOf(
        View.ACCESSIBILITY_LIVE_REGION_POLITE to "polite",
        View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE to "assertive",
      )

    private val RANGE_TYPES =
      mapOf(
        AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_INT to "integer",
        AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_FLOAT to "float",
        AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_PERCENT to "percent",
        AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INDETERMINATE to "indeterminate",
      )
    private val NAMES_BY_ID = ACTIONS.entries.associate { (name, id) -> id to name }

    /** Actions that take arguments, with the names scripts give them. */
    val ARGUMENT_ACTIONS: Map<String, Int> =
      mapOf(
        "setText" to AccessibilityNodeInfo.ACTION_SET_TEXT,
        "setSelection" to AccessibilityNodeInfo.ACTION_SET_SELECTION,
        "setProgress" to android.R.id.accessibilityActionSetProgress,
        "scrollToPosition" to android.R.id.accessibilityActionScrollToPosition,
        "nextAtGranularity" to AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY,
        "previousAtGranularity" to AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY,
        "nextHtmlElement" to AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT,
        "previousHtmlElement" to AccessibilityNodeInfo.ACTION_PREVIOUS_HTML_ELEMENT,
      )

    val GRANULARITIES: Map<String, Int> =
      mapOf(
        "character" to AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER,
        "word" to AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD,
        "line" to AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE,
        "paragraph" to AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH,
        "page" to AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE,
      )

    /** The arguments of [action], from what the script gave as [args]. */
    fun arguments(action: String, args: JSONObject?): Bundle {
      val a = args ?: JSONObject()
      val bundle = Bundle()
      when (action) {
        "setText" ->
          bundle.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            a.optString("text"),
          )
        "setSelection" -> {
          val start = a.optInt("start", 0)
          bundle.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, start)
          bundle.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, a.optInt("end", start))
        }
        "setProgress" ->
          bundle.putFloat(
            AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,
            a.optDouble("value", 0.0).toFloat(),
          )
        "scrollToPosition" -> {
          bundle.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_ROW_INT, a.optInt("row", 1) - 1)
          bundle.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_COLUMN_INT, a.optInt("column", 1) - 1)
        }
        "nextAtGranularity",
        "previousAtGranularity" -> {
          val unit = a.optString("unit", "character")
          bundle.putInt(
            AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
            GRANULARITIES[unit]
              ?: apiError("Unknown unit $unit. Units: ${GRANULARITIES.keys.joinToString()}"),
          )
          bundle.putBoolean(
            AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN,
            a.optBoolean("extend"),
          )
        }
        "nextHtmlElement",
        "previousHtmlElement" ->
          bundle.putString(
            AccessibilityNodeInfo.ACTION_ARGUMENT_HTML_ELEMENT_STRING,
            a.optString("element").uppercase(),
          )
      }
      return bundle
    }

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
