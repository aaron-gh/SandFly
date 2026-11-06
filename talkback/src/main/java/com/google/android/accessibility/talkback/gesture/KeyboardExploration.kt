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

package com.google.android.accessibility.talkback.gesture

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.graphics.Region
import android.os.Build
import android.text.TextUtils
import android.view.accessibility.AccessibilityWindowInfo
import androidx.annotation.RequiresApi
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.AccessibilityServiceCompatUtils.Constants.GBOARD_PACKAGE_NAME
import com.google.android.accessibility.utils.AccessibilityServiceCompatUtils.Constants.GBOARD_PACKAGE_NAME_DEV

/**
 * Finds what a finger touches on Gboard, or Pixelboard, which is built from it, without touch
 * exploration.
 *
 * In touch exploration Android moves the finger as hover events to the app, and the screen reader
 * learns what is under it only from the app's hover enter events. Gboard's popup of alternative
 * characters posts its hover enter events, and on some phones, such as a Galaxy Z Fold7 and a Xiaomi
 * with Android 16, they never arrive, so nothing in the popup can be found. A touch on Gboard is
 * kept from touch exploration instead, its motion events stay with SandFly, and the node under the
 * finger is found here from the window's nodes.
 */
object KeyboardExploration {
  /** Pixelboard, a keyboard built from Gboard, with the same popup of alternative characters. */
  private const val PIXELBOARD_PACKAGE_NAME = "com.akshaykadam.pixelboard"

  /** Returns whether [packageName] is Gboard's, or that of a keyboard built from it. */
  @JvmStatic
  fun isGboardPackage(packageName: CharSequence?): Boolean =
    TextUtils.equals(packageName, GBOARD_PACKAGE_NAME) ||
      TextUtils.equals(packageName, GBOARD_PACKAGE_NAME_DEV) ||
      TextUtils.equals(packageName, PIXELBOARD_PACKAGE_NAME)

  /**
   * Returns the root of Gboard's window if it is the topmost window at [x], [y] on [displayId], or
   * null. Android doesn't cache a window's root, so this waits for the keyboard's app to send it: a
   * touch fetches it once and finds the nodes under the finger with [nodeAt].
   */
  @JvmStatic
  @RequiresApi(Build.VERSION_CODES.R)
  fun gboardRootAt(
    service: AccessibilityService,
    displayId: Int,
    x: Float,
    y: Float,
  ): AccessibilityNodeInfoCompat? {
    val window = topmostWindowAt(service, displayId, x, y) ?: return null
    if (window.type != AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
      return null
    }
    val root = window.root ?: return null
    return if (isGboardPackage(root.packageName)) AccessibilityNodeInfoCompat.wrap(root) else null
  }

  /** Returns the topmost window at [x], [y] on [displayId], from the cached window list. */
  @RequiresApi(Build.VERSION_CODES.R)
  private fun topmostWindowAt(
    service: AccessibilityService,
    displayId: Int,
    x: Float,
    y: Float,
  ): AccessibilityWindowInfo? {
    val region = Region()
    val windows = service.windowsOnAllDisplays.get(displayId) ?: return null
    for (window in windows) {
      if (window.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY) {
        continue
      }
      window.getRegionInScreen(region)
      if (region.contains(x.toInt(), y.toInt())) {
        return window
      }
    }
    return null
  }

  /**
   * Returns the node at [x], [y] under [root], the root of Gboard's window from [gboardRootAt], that
   * touch exploration would focus, or null if there is none, or the point isn't on that window.
   *
   * Of overlapping children the later one, drawn on top, is tried first. A child with nothing to
   * focus is passed over for the next.
   */
  @JvmStatic
  @RequiresApi(Build.VERSION_CODES.R)
  fun nodeAt(
    service: AccessibilityService,
    displayId: Int,
    root: AccessibilityNodeInfoCompat,
    x: Float,
    y: Float,
  ): AccessibilityNodeInfoCompat? {
    if (!isOnWindow(service, displayId, root, x, y)) {
      return null
    }
    return nodeUnder(root, x, y)
  }

  /**
   * Returns whether the window of [root], the root of Gboard's window from [gboardRootAt], is the
   * topmost window at [x], [y] on [displayId].
   */
  @JvmStatic
  @RequiresApi(Build.VERSION_CODES.R)
  fun isOnWindow(
    service: AccessibilityService,
    displayId: Int,
    root: AccessibilityNodeInfoCompat,
    x: Float,
    y: Float,
  ): Boolean = topmostWindowAt(service, displayId, x, y)?.id == root.windowId

  /**
   * Returns the node at [x], [y] under [root] that touch exploration would focus, or null, without
   * checking that [root]'s window is the topmost one there; see [isOnWindow].
   */
  @JvmStatic
  fun nodeUnder(root: AccessibilityNodeInfoCompat, x: Float, y: Float): AccessibilityNodeInfoCompat? =
    focusableAt(root, x.toInt(), y.toInt(), Rect(), depth = 0)

  private const val MAX_DEPTH = 30

  /** How deep under the popup its characters are looked for. */
  private const val POPUP_DEPTH = 4

  /**
   * Returns the characters of Gboard's [popup], fetched again from Gboard, which waits for Gboard: a
   * touch fetches them once and selects among them with [popupKeyAt].
   */
  @JvmStatic
  fun popupKeys(popup: AccessibilityNodeInfoCompat): List<AccessibilityNodeInfoCompat> {
    popup.refresh()
    val keys = ArrayList<AccessibilityNodeInfoCompat>()
    collectFocusable(popup, POPUP_DEPTH, keys)
    return keys
  }

  /**
   * Returns the character of Gboard's popup, among its [keys] from [popupKeys], that a finger at
   * [x], [y] selects: the one under it, or else the nearest, as Gboard selects one for a finger
   * anywhere on the screen while the popup is open. Returns null if there are no characters.
   */
  @JvmStatic
  fun popupKeyAt(
    keys: List<AccessibilityNodeInfoCompat>,
    x: Float,
    y: Float,
  ): AccessibilityNodeInfoCompat? {
    val bounds = Rect()
    var nearest: AccessibilityNodeInfoCompat? = null
    var nearestDistance = Float.MAX_VALUE
    for (key in keys) {
      key.getBoundsInScreen(bounds)
      if (bounds.contains(x.toInt(), y.toInt())) {
        return key
      }
      val distance = Math.hypot(
        (bounds.exactCenterX() - x).toDouble(),
        (bounds.exactCenterY() - y).toDouble(),
      ).toFloat()
      if (distance < nearestDistance) {
        nearestDistance = distance
        nearest = key
      }
    }
    return nearest
  }

  private fun collectFocusable(
    node: AccessibilityNodeInfoCompat,
    depth: Int,
    keys: MutableList<AccessibilityNodeInfoCompat>,
  ) {
    for (i in 0 until node.childCount) {
      val child = node.getChild(i) ?: continue
      if (!child.isVisibleToUser) {
        continue
      }
      if (AccessibilityNodeInfoUtils.FILTER_SHOULD_FOCUS_EXCEPT_WEB_VIEW.accept(child)) {
        keys += child
      } else if (depth > 1) {
        collectFocusable(child, depth - 1, keys)
      }
    }
  }

  private fun focusableAt(
    node: AccessibilityNodeInfoCompat,
    x: Int,
    y: Int,
    bounds: Rect,
    depth: Int,
  ): AccessibilityNodeInfoCompat? {
    if (depth > MAX_DEPTH || !node.isVisibleToUser) {
      return null
    }
    node.getBoundsInScreen(bounds)
    if (!bounds.contains(x, y)) {
      return null
    }
    for (i in node.childCount - 1 downTo 0) {
      val child = node.getChild(i) ?: continue
      val found = focusableAt(child, x, y, bounds, depth + 1)
      if (found != null) {
        return found
      }
    }
    return node.takeIf { AccessibilityNodeInfoUtils.FILTER_SHOULD_FOCUS_EXCEPT_WEB_VIEW.accept(it) }
  }
}
