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

package com.google.android.accessibility.talkback.preference.base

import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import com.google.android.accessibility.talkback.R

fun setMoveActions(row: View, index: Int, count: Int, move: (offset: Int) -> Boolean) {
  setMoveAction(row, R.id.accessibility_custom_action_0, R.string.action_move_up, index > 0) {
    move(-1)
  }
  setMoveAction(
    row,
    R.id.accessibility_custom_action_1,
    R.string.action_move_down,
    index in 0 until count - 1,
  ) {
    move(1)
  }
}

private fun setMoveAction(
  row: View,
  id: Int,
  @StringRes label: Int,
  canMove: Boolean,
  move: () -> Boolean,
) {
  if (!canMove) {
    ViewCompat.removeAccessibilityAction(row, id)
    return
  }
  val text = row.context.getString(label)
  ViewCompat.replaceAccessibilityAction(row, AccessibilityActionCompat(id, text), text) { _, _ ->
    move()
  }
}

fun announce(view: View, text: CharSequence) {
  @Suppress("DEPRECATION") // There is no replacement for a one-off message.
  view.announceForAccessibility(text)
}

fun positionText(view: View, index: Int, count: Int): String =
  view.context.getString(R.string.template_list_position, index + 1, count)
