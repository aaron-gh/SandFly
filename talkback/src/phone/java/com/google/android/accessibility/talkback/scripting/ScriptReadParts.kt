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

import android.os.SystemClock
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

/** What a rule with read parts says for its item: each part's text, in order, joined by commas. */
object ScriptReadParts {
  /** Reading the parts asks the app for items while focus speech waits, so it gives up after this. */
  private const val LIMIT_MS = 150L
  private const val MAX_ABOVE = 4
  private const val LABEL_DEPTH = 3

  fun compose(node: AccessibilityNodeInfoCompat, parts: List<ReadPart>): String? {
    val deadline = SystemClock.uptimeMillis() + LIMIT_MS
    val texts =
      parts.mapNotNull { part ->
        val text = textOf(node, part, deadline) ?: if (part.fromAbove) above(node, part, deadline) else null
        text?.let { listOfNotNull(part.before?.trim()?.ifEmpty { null }, it).joinToString(" ") }
      }
    return texts.joinToString(", ").ifEmpty { null }
  }

  private fun textOf(node: AccessibilityNodeInfoCompat, part: ReadPart, deadline: Long): String? {
    val target =
      // Child numbers count the shown items in reading order, as Inspect focused item lists them.
      part.path?.let { path ->
        path.fold(node as AccessibilityNodeInfoCompat?) { item, number ->
          item?.childrenInReadingOrder()?.filter { it.isVisibleToUser }?.elementAtOrNull(number - 1)
        }
      }
        ?: part.from?.firstIn(node, deadline)
        ?: return null
    val text = if (target == node) ownText(node) else label(target, LABEL_DEPTH)
    return text.ifEmpty { null }
  }

  /** The part from the nearest item above [node] in its list that has it. */
  private fun above(node: AccessibilityNodeInfoCompat, part: ReadPart, deadline: Long): String? {
    val parent = node.parent ?: return null
    val index = (0 until parent.childCount).firstOrNull { parent.getChild(it) == node } ?: return null
    for (i in index - 1 downTo maxOf(0, index - MAX_ABOVE)) {
      if (SystemClock.uptimeMillis() > deadline) return null
      val sibling = parent.getChild(i) ?: continue
      textOf(sibling, part, deadline)?.let { return it }
    }
    return null
  }

  private fun ownText(node: AccessibilityNodeInfoCompat): String =
    (node.contentDescription?.takeIf { it.isNotBlank() } ?: node.text)?.toString()?.trim().orEmpty()

  private fun label(node: AccessibilityNodeInfoCompat, depth: Int): String {
    val own = ownText(node)
    if (own.isNotEmpty() || depth == 0) return own
    return node.childrenInReadingOrder().map { label(it, depth - 1) }.firstOrNull { it.isNotEmpty() }.orEmpty()
  }
}
