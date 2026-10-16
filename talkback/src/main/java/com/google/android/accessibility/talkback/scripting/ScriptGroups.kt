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

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils

object ScriptGroups {
  private const val MAX_DEPTH = 50

  fun items(
    group: AccessibilityNodeInfoCompat,
    ruleOf: (AccessibilityNodeInfoCompat) -> NodeRule?,
  ): List<AccessibilityNodeInfoCompat> = buildList { collect(group, ruleOf, 0) }

  fun actionTarget(
    group: AccessibilityNodeInfoCompat,
    longClick: Boolean,
  ): AccessibilityNodeInfoCompat {
    val acts = { node: AccessibilityNodeInfoCompat ->
      AccessibilityNodeInfoUtils.isVisible(node) &&
        if (longClick) AccessibilityNodeInfoUtils.isLongClickable(node)
        else AccessibilityNodeInfoUtils.isClickable(node)
    }
    return if (acts(group)) group else group.descendantsInReadingOrder().firstOrNull(acts) ?: group
  }

  private fun MutableList<AccessibilityNodeInfoCompat>.collect(
    node: AccessibilityNodeInfoCompat,
    ruleOf: (AccessibilityNodeInfoCompat) -> NodeRule?,
    depth: Int,
  ) {
    if (depth >= MAX_DEPTH) return
    for (child in node.childrenInReadingOrder()) {
      val rule = ruleOf(child)
      if (!AccessibilityNodeInfoUtils.isVisible(child) || rule?.hideInside == true) continue
      if (rule?.hide != true) add(child)
      if (rule?.label == null && child.contentDescription.isNullOrEmpty()) {
        collect(child, ruleOf, depth + 1)
      }
    }
  }
}
