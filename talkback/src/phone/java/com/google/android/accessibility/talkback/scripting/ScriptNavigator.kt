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
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils

class ScriptNavigator(private val manager: ScriptManager) {
  private class Match(val node: AccessibilityNodeInfoCompat, val start: Int, var end: Int = start)

  fun move(runtime: ScriptRuntime, query: NodeQuery, forward: Boolean) {
    val focus = manager.focusedNode()
    val root =
      focus?.let(AccessibilityNodeInfoUtils::getRoot) ?: manager.activeRoot() ?: return edge()
    val (matches, focusAt) = scan(root, focus) { query.matches(it, runtime::textOf) }
    val target =
      if (forward) matches.firstOrNull { it.start > focusAt }
      else matches.lastOrNull { it.end < focusAt }
    target?.let { manager.feedback.focus(it.node) { moved -> if (!moved) edge() } } ?: edge()
  }

  private fun edge() {
    manager.feedback.playSound(R.raw.complete)
  }

  private fun scan(
    root: AccessibilityNodeInfoCompat,
    focus: AccessibilityNodeInfoCompat?,
    matches: (AccessibilityNodeInfoCompat) -> Boolean,
  ): Pair<List<Match>, Int> {
    val found = mutableListOf<Match>()
    val deadline = SystemClock.uptimeMillis() + ScriptRuntime.CALL_LIMIT_MS
    var index = 0
    var focusAt = -1
    fun visit(node: AccessibilityNodeInfoCompat, depth: Int) {
      val start = index++
      if (node == focus) focusAt = start
      val match = Match(node, start).takeIf { node.isVisibleToUser && matches(node) }
      match?.let(found::add)
      if (depth < MAX_DEPTH) {
        for (child in node.children()) {
          if (index >= MAX_VISITED || SystemClock.uptimeMillis() > deadline) break
          visit(child, depth + 1)
        }
      }
      match?.end = index - 1
    }
    visit(root, 0)
    return found to focusAt
  }

  private companion object {
    const val MAX_DEPTH = 60
    const val MAX_VISITED = 3000
  }
}
