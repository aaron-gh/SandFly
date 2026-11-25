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

package com.google.android.accessibility.talkback.keyboard

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

/**
 * The text field that SandFly focus moved to, with a SandFly command or by touch, and that hasn't
 * been activated or typed into since. Arrow key navigation moves past such a field even where the
 * app gave it input focus: moving onto a text field reads it, and editing starts when the user
 * activates it or types into it.
 */
class TextFieldActivation {
  /** The text field SandFly focus moved to, until it is activated or typed into. */
  private var movedTo: AccessibilityNodeInfoCompat? = null

  /** The item SandFly focus was last on. */
  private var lastFocus: AccessibilityNodeInfoCompat? = null

  /**
   * Whether an item was activated since the last key press or touch, so that a text field the
   * activation moves SandFly focus to, such as a search field that a Search button opens, is
   * edited.
   */
  private var activated = false

  /**
   * SandFly focus moved to [node]. [moving] is whether the user moved it, with a SandFly command or
   * by touch, rather than typed or opened a new screen since.
   */
  fun onAccessibilityFocused(node: AccessibilityNodeInfoCompat, moving: Boolean) {
    // SandFly focus put back on the same item, such as after the app changed the screen, isn't a
    // move.
    if (node == lastFocus) {
      return
    }
    lastFocus = node
    movedTo = node.takeIf { moving && !activated && it.isEditable }
  }

  /** A key other than a modifier was pressed, or the screen touched. */
  fun onInput() {
    activated = false
  }

  /** The user ran Click, as with Enter or Up Arrow + Down Arrow. */
  fun onActivated() {
    activated = true
    movedTo = null
  }

  /** [node] was clicked, as by a double tap. */
  fun onClicked(node: AccessibilityNodeInfoCompat) {
    if (node == movedTo) {
      movedTo = null
    }
  }

  /** A key that types or edits went to the app. */
  fun onTyped() {
    movedTo = null
  }

  /** Whether [node] is the text field SandFly focus moved to and that is not being edited. */
  fun isMovedTo(node: AccessibilityNodeInfoCompat?): Boolean = node != null && node == movedTo

  fun reset() {
    movedTo = null
    lastFocus = null
    activated = false
  }
}
