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

package com.google.android.accessibility.talkback.compositor

import android.graphics.Rect
import android.text.Spanned
import android.text.style.LocaleSpan
import android.text.style.StyleSpan
import android.text.style.TtsSpan
import android.text.style.URLSpan
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

/**
 * What an announcement of focusing a node is worked out from: the node and its descendants, their
 * texts, states, actions and places inside the node, and the node's ancestors, which give its
 * list, container and pane. Two fingerprints of the same node are equal only if all of these are,
 * so an announcement worked out when one was taken still holds when the other is.
 *
 * Places on screen are left out, so that a scroll that only moves the node keeps its fingerprint.
 * The places of descendants relative to the node are kept, as they set the order they are read in.
 */
object NodeFingerprint {
  /** Most nodes of the node's own tree that are read; a bigger tree has no fingerprint. */
  const val MAX_NODES = 64

  /** Most ancestors that are read; a deeper node has no fingerprint. */
  const val MAX_ANCESTORS = 64

  /**
   * Returns the fingerprint of [node], read from the nodes Android holds for it, or null if its
   * tree or its ancestors are too big to read, see [MAX_NODES] and [MAX_ANCESTORS].
   */
  @JvmStatic
  fun of(node: AccessibilityNodeInfoCompat): List<Any?>? {
    val values = ArrayList<Any?>()
    val origin = Rect()
    node.getBoundsInScreen(origin)
    val count = intArrayOf(0)
    if (!addTree(values, node, origin, count)) {
      return null
    }
    values.add(ANCESTORS)
    var ancestor = node.parent
    var depth = 0
    while (ancestor != null) {
      if (++depth > MAX_ANCESTORS) {
        return null
      }
      addAncestor(values, ancestor)
      ancestor = ancestor.parent
    }
    return values
  }

  /** Marks where the ancestors start, so that a tree can't read as a tree and its ancestors. */
  private const val ANCESTORS = "ancestors"

  private fun addTree(
    values: MutableList<Any?>,
    node: AccessibilityNodeInfoCompat,
    origin: Rect,
    count: IntArray,
  ): Boolean {
    if (++count[0] > MAX_NODES) {
      return false
    }
    addNode(values, node, origin)
    val children = node.childCount
    for (i in 0 until children) {
      val child = node.getChild(i)
      if (child == null) {
        values.add(null)
        continue
      }
      if (!addTree(values, child, origin, count)) {
        return false
      }
    }
    return true
  }

  private fun addNode(values: MutableList<Any?>, node: AccessibilityNodeInfoCompat, origin: Rect) {
    values.add(node.className?.toString())
    values.add(node.viewIdResourceName)
    addText(values, node.text)
    addText(values, node.contentDescription)
    addText(values, node.stateDescription)
    addText(values, node.hintText)
    addText(values, node.error)
    addText(values, node.tooltipText)
    addText(values, node.paneTitle)
    addText(values, node.roleDescription)
    addText(values, node.containerTitle)
    values.add(flags(node))
    values.add(node.inputType)
    values.add(node.childCount)
    val bounds = Rect()
    node.getBoundsInScreen(bounds)
    values.add(bounds.left - origin.left)
    values.add(bounds.top - origin.top)
    values.add(bounds.width())
    values.add(bounds.height())
    addCollectionInfo(values, node)
    node.rangeInfo.let { range ->
      if (range == null) {
        values.add(null)
      } else {
        values.add(range.type)
        values.add(range.min)
        values.add(range.max)
        values.add(range.current)
      }
    }
    val actions = node.unwrap().actionList
    values.add(actions.size)
    for (action in actions) {
      values.add(action.id)
      addText(values, action.label)
    }
    val label = node.labeledBy
    if (label == null) {
      values.add(null)
    } else {
      addText(values, label.text)
      addText(values, label.contentDescription)
    }
  }

  private fun addAncestor(values: MutableList<Any?>, node: AccessibilityNodeInfoCompat) {
    // The ancestor itself: the same texts in another list are said with that list's details.
    values.add(node)
    values.add(node.className?.toString())
    addText(values, node.text)
    addText(values, node.contentDescription)
    addText(values, node.paneTitle)
    addText(values, node.roleDescription)
    addText(values, node.containerTitle)
    values.add(node.isHeading)
    addCollectionInfo(values, node)
  }

  private fun addCollectionInfo(values: MutableList<Any?>, node: AccessibilityNodeInfoCompat) {
    val collection = node.collectionInfo
    if (collection == null) {
      values.add(null)
    } else {
      values.add(collection.rowCount)
      values.add(collection.columnCount)
      values.add(collection.isHierarchical)
      values.add(collection.selectionMode)
    }
    val item = node.collectionItemInfo
    if (item == null) {
      values.add(null)
    } else {
      values.add(item.rowIndex)
      values.add(item.rowSpan)
      values.add(item.columnIndex)
      values.add(item.columnSpan)
      @Suppress("DEPRECATION") values.add(item.isHeading)
      values.add(item.isSelected)
    }
  }

  private fun flags(node: AccessibilityNodeInfoCompat): Int {
    var flags = 0
    val states =
      booleanArrayOf(
        node.isCheckable,
        node.isChecked,
        node.isSelected,
        node.isEnabled,
        node.isVisibleToUser,
        node.isFocusable,
        node.isFocused,
        node.isClickable,
        node.isLongClickable,
        node.isScreenReaderFocusable,
        node.isHeading,
        node.isPassword,
        node.isEditable,
        node.isScrollable,
        node.isContentInvalid,
        node.isImportantForAccessibility,
        node.isShowingHintText,
        node.isMultiLine,
        node.isDismissable,
        node.isTextEntryKey,
      )
    for (i in states.indices) {
      if (states[i]) {
        flags = flags or (1 shl i)
      }
    }
    return flags
  }

  /**
   * Adds [text] with the spans that change how it is spoken: links, languages, styles and spoken
   * forms, with where they are.
   */
  private fun addText(values: MutableList<Any?>, text: CharSequence?) {
    if (text == null) {
      values.add(null)
      return
    }
    values.add(text.toString())
    if (text !is Spanned) {
      values.add(0)
      return
    }
    val spans = text.getSpans(0, text.length, Any::class.java)
    // The number of spans first, so that a span can't read as the next value.
    values.add(spans.size)
    for (span in spans) {
      values.add(span.javaClass.name)
      values.add(text.getSpanStart(span))
      values.add(text.getSpanEnd(span))
      when (span) {
        is URLSpan -> values.add(span.url)
        is LocaleSpan -> values.add(span.locales.toLanguageTags())
        is StyleSpan -> values.add(span.style)
        is TtsSpan -> {
          values.add(span.type)
          // Reading the size first unparcels the arguments, so that they are printed.
          span.args.size()
          values.add(span.args.toString())
        }
        else -> values.add(null)
      }
    }
  }
}
