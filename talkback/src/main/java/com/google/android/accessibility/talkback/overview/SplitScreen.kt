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

package com.google.android.accessibility.talkback.overview

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.AccessibilityServiceCompatUtils
import com.google.android.accessibility.utils.KeyboardUtils

/**
 * Keeps window focus in split screen on the app the user is in. Back and key presses go to the
 * window with window focus, which touch exploration and swipes don't move. Android moves it for any
 * accessibility action on a node of another window, other than setting accessibility focus.
 */
class SplitScreen(private val service: AccessibilityService) {
  private val handler = Handler(Looper.getMainLooper())
  private val focusTextField = Runnable { moveWindowFocusToTextField() }
  private var touching = false
  private var lastAppWindowId = NO_WINDOW

  fun onAccessibilityEvent(event: AccessibilityEvent) {
    when (event.eventType) {
      AccessibilityEvent.TYPE_TOUCH_INTERACTION_START -> {
        touching = true
        handler.removeCallbacks(focusTextField)
      }
      AccessibilityEvent.TYPE_TOUCH_INTERACTION_END -> {
        touching = false
        scheduleFocusTextField()
      }
      AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED -> onAccessibilityFocused(event)
    }
  }

  private fun onAccessibilityFocused(event: AccessibilityEvent) {
    val windows = windows()
    if (!isSplit(windows)) {
      lastAppWindowId = NO_WINDOW
      return
    }
    if (windows.firstOrNull { it.id == event.windowId }?.type == TYPE_APP) {
      lastAppWindowId = event.windowId
    }
    scheduleFocusTextField()
  }

  private fun scheduleFocusTextField() {
    handler.removeCallbacks(focusTextField)
    handler.postDelayed(focusTextField, FOCUS_TEXT_FIELD_DELAY_MS)
  }

  /** Gives a text field with accessibility focus window and input focus, for a hardware keyboard. */
  private fun moveWindowFocusToTextField() {
    if (touching || !KeyboardUtils.isHardKeyboardActive(service)) return
    val node = service.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY) ?: return
    if (!node.isEditable) return
    val windows = windows()
    if (!isSplit(windows)) return
    val window = windows.firstOrNull { it.id == node.windowId } ?: return
    if (canTakeWindowFocus(window, windows) || (window.isFocused && !node.isFocused)) {
      node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
    }
  }

  /**
   * In split screen, moves window focus to the app that last had accessibility focus, then runs
   * [action]. Returns false, without running it, if window focus doesn't have to move.
   */
  fun runInLastFocusedApp(action: Runnable): Boolean {
    val windows = windows()
    if (!isSplit(windows)) return false
    val window = windows.firstOrNull { it.id == lastAppWindowId } ?: return false
    if (!canTakeWindowFocus(window, windows)) return false
    val root = window.root ?: return false
    root.performAction(NO_NODE_ACTION)
    waitForWindowFocus(window.id, SystemClock.uptimeMillis() + WINDOW_FOCUS_TIMEOUT_MS, action)
    return true
  }

  private fun waitForWindowFocus(windowId: Int, deadline: Long, action: Runnable) {
    val focused = windows().firstOrNull { it.id == windowId }?.isFocused ?: false
    if (focused || SystemClock.uptimeMillis() >= deadline) {
      // The window list changes before input dispatch does.
      handler.postDelayed(action, INPUT_FOCUS_SETTLE_MS)
    } else {
      handler.postDelayed(
        { waitForWindowFocus(windowId, deadline, action) },
        WINDOW_FOCUS_POLL_MS,
      )
    }
  }

  fun shutdown() {
    handler.removeCallbacksAndMessages(null)
  }

  private fun windows(): List<AccessibilityWindowInfo> =
    AccessibilityServiceCompatUtils.getWindows(service).orEmpty()

  private fun isSplit(windows: List<AccessibilityWindowInfo>): Boolean =
    windows.any { it.type == TYPE_DIVIDER }

  /** Only another app's window gives up window focus, not a dialog overlay or the system's. */
  private fun canTakeWindowFocus(
    window: AccessibilityWindowInfo,
    windows: List<AccessibilityWindowInfo>,
  ): Boolean {
    if (window.type != TYPE_APP || window.isFocused || window.isInPictureInPictureMode) return false
    val focused = windows.firstOrNull { it.isFocused } ?: return true
    return focused.type == TYPE_APP
  }

  companion object {
    private const val TYPE_APP = AccessibilityWindowInfo.TYPE_APPLICATION
    private const val TYPE_DIVIDER = AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER
    private const val NO_WINDOW = -1

    /** An action no view handles: Android moves window focus before the app receives it. */
    private const val NO_NODE_ACTION = 0x5cff0000

    private const val FOCUS_TEXT_FIELD_DELAY_MS = 150L
    private const val WINDOW_FOCUS_TIMEOUT_MS = 400L
    private const val WINDOW_FOCUS_POLL_MS = 20L
    private const val INPUT_FOCUS_SETTLE_MS = 60L
    private const val MAX_DIVIDER_DEPTH = 6

    /** Whether the split screen divider is on screen. */
    @JvmStatic
    fun isShown(service: AccessibilityService): Boolean =
      AccessibilityServiceCompatUtils.getWindows(service).orEmpty().any { it.type == TYPE_DIVIDER }

    /**
     * Whether [node] is in the split screen app without window focus. Pressing a key takes every
     * window out of touch mode, which gives a view in that app input focus too.
     */
    @JvmStatic
    fun isInUnfocusedApp(node: AccessibilityNodeInfoCompat): Boolean {
      val service = TalkBackService.getInstance() ?: return false
      val window = node.window ?: return false
      return window.type == TYPE_APP &&
        !window.isFocused &&
        AccessibilityServiceCompatUtils.getWindows(service).orEmpty().any { it.type == TYPE_DIVIDER }
    }

    /** The divider's node with the system's sizing actions, or its root without them. */
    @JvmStatic
    fun dividerHandle(service: AccessibilityService): AccessibilityNodeInfoCompat? {
      val window =
        AccessibilityServiceCompatUtils.getWindows(service).orEmpty().firstOrNull {
          it.type == TYPE_DIVIDER
        } ?: return null
      val root = window.root?.let { AccessibilityNodeInfoCompat.wrap(it) } ?: return null
      return findWithActions(root, 0) ?: root
    }

    private fun findWithActions(
      node: AccessibilityNodeInfoCompat,
      depth: Int,
    ): AccessibilityNodeInfoCompat? {
      if (dividerActions(node).isNotEmpty()) return node
      if (depth >= MAX_DIVIDER_DEPTH) return null
      for (index in 0 until node.childCount) {
        val child = node.getChild(index) ?: continue
        findWithActions(child, depth + 1)?.let { return it }
      }
      return null
    }

    /** The divider's own actions, such as its sizes and full screen for either app. */
    @JvmStatic
    fun dividerActions(
      node: AccessibilityNodeInfoCompat
    ): List<AccessibilityNodeInfoCompat.AccessibilityActionCompat> =
      node.actionList.filter { AccessibilityNodeInfoUtils.isCustomAction(it) && it.label != null }
  }
}
