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
 * License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.talkback.actor.gemini.screenqa

import android.graphics.Rect
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.gemineye.api.AccessibilityTree
import com.google.android.accessibility.gemineye.api.NodeId
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.protobuf.ByteString

/**
 * Numbers the visible nodes of the window that holds a node, so that Gemini can refer to screen
 * elements by number and TalkBack can find the live nodes again.
 */
class ScreenTree private constructor(private val nodes: List<AccessibilityNodeInfoCompat>) :
  AccessibilityTree {

  /** One line per node that has content or can be acted on, for the Gemini prompt. */
  val description: String by lazy {
    nodes
      .withIndex()
      .mapNotNull { (id, node) -> describe(id, node, compact = false) }
      .joinToString(separator = NEWLINE)
  }

  /**
   * A shorter list for a model on the phone, where every token costs time: only nodes that have a
   * label, no screen coordinates, shorter labels, and a cap on the number of lines.
   */
  val compactDescription: String by lazy {
    nodes
      .withIndex()
      .mapNotNull { (id, node) -> describe(id, node, compact = true) }
      .take(COMPACT_MAX_LINES)
      .joinToString(separator = NEWLINE)
  }

  /**
   * The text of the nodes, each once, shortened like [compactDescription], for a small model to
   * copy names from.
   */
  val texts: List<String> by lazy {
    nodes
      .mapNotNull { (it.contentDescription ?: it.text)?.toString()?.trim() }
      .filter { it.isNotEmpty() }
      .map { it.take(COMPACT_MAX_LABEL_LENGTH) }
      .distinct()
      .take(COMPACT_MAX_LINES)
  }

  // Node numbers are unique across the whole tree, so the window ID is not needed to find a node.
  override fun findNodeById(nodeId: NodeId): AccessibilityNodeInfoCompat? =
    nodes.getOrNull(nodeId.uniqueId)

  override fun serialize(): ByteString = ByteString.copyFromUtf8(description)

  private fun describe(id: Int, node: AccessibilityNodeInfoCompat, compact: Boolean): String? {
    val bounds = Rect().also { node.getBoundsInScreen(it) }
    return formatLine(
      id = id,
      className = node.className?.toString()?.substringAfterLast('.') ?: "View",
      rawLabel = (node.contentDescription ?: node.text)?.toString(),
      traits =
        listOfNotNull(
          "clickable".takeIf { node.isClickable },
          "editable".takeIf { node.isEditable },
          "checked".takeIf { node.isCheckable && node.isChecked },
          "not checked".takeIf { node.isCheckable && !node.isChecked },
          "scrollable".takeIf { node.isScrollable },
          "heading".takeIf { node.isHeading },
        ),
      bounds = listOf(bounds.left, bounds.top, bounds.right, bounds.bottom),
      compact = compact,
    )
  }

  companion object {
    private const val MAX_NODES = 400
    private const val MAX_LABEL_LENGTH = 120
    private const val NEWLINE = "\n"
    private const val COMPACT_MAX_LINES = 120
    private const val COMPACT_MAX_LABEL_LENGTH = 60

    /** Formats one node as a line of the prompt, or null when the node adds nothing. */
    @JvmStatic
    internal fun formatLine(
      id: Int,
      className: String,
      rawLabel: String?,
      traits: List<String>,
      bounds: List<Int>,
      compact: Boolean,
    ): String? {
      val maxLabel = if (compact) COMPACT_MAX_LABEL_LENGTH else MAX_LABEL_LENGTH
      val label = rawLabel?.trim()?.take(maxLabel)
      val usefulTraits = if (compact) traits.filter { it != "scrollable" } else traits
      if (label.isNullOrEmpty() && (compact || usefulTraits.isEmpty())) {
        return null
      }
      return buildString {
        append("$id: $className")
        if (!label.isNullOrEmpty()) append(" \"$label\"")
        if (usefulTraits.isNotEmpty()) append(" (${usefulTraits.joinToString()})")
        if (!compact) append(" [${bounds.joinToString(",")}]")
      }
    }

    /** Collects the visible nodes of the window that holds [node], in breadth-first order. */
    @JvmStatic
    fun fromNode(node: AccessibilityNodeInfoCompat): ScreenTree {
      val root = AccessibilityNodeInfoUtils.getRoot(node) ?: node
      val nodes = mutableListOf<AccessibilityNodeInfoCompat>()
      val queue = ArrayDeque(listOf(root))
      while (queue.isNotEmpty() && nodes.size < MAX_NODES) {
        val current = queue.removeFirst()
        if (!AccessibilityNodeInfoUtils.isVisible(current)) {
          continue
        }
        nodes.add(current)
        for (index in 0 until current.childCount) {
          current.getChild(index)?.let { queue.addLast(it) }
        }
      }
      return ScreenTree(nodes)
    }
  }
}
