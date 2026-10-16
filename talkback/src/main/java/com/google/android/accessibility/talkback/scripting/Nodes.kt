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
import com.google.android.accessibility.utils.traversal.ReorderedChildrenIterator

private const val MAX_DEPTH = 50

fun AccessibilityNodeInfoCompat.children(): List<AccessibilityNodeInfoCompat> =
  (0..<childCount).mapNotNull { getChild(it) }

fun AccessibilityNodeInfoCompat.childrenInReadingOrder(): Sequence<AccessibilityNodeInfoCompat> =
  ReorderedChildrenIterator.createAscendingIterator(this).asSequence().filterNotNull()

fun AccessibilityNodeInfoCompat.descendantsInReadingOrder(
  depth: Int = 0
): Sequence<AccessibilityNodeInfoCompat> =
  childrenInReadingOrder().flatMap { child ->
    sequenceOf(child) +
      if (depth < MAX_DEPTH) child.descendantsInReadingOrder(depth + 1) else emptySequence()
  }

fun AccessibilityNodeInfoCompat.ancestors(): Sequence<AccessibilityNodeInfoCompat> =
  generateSequence(parent) { it.parent }.take(MAX_DEPTH)
