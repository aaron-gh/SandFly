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
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.Region
import android.os.Build
import android.os.SystemClock
import android.text.TextUtils
import android.view.accessibility.AccessibilityWindowInfo
import androidx.annotation.RequiresApi
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.clipboard.ClipboardHistoryOverlay
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
   * The first Gboard version that selects the characters of its popup from touch exploration's
   * hover and reports each one: 18.8. Gboard 18.4 (Pixelboard) and 18.5 report none, so SandFly
   * explores those itself.
   */
  private const val POPUP_REPORTING_MAJOR = 18
  private const val POPUP_REPORTING_MINOR = 8

  /** How long a keyboard's version is kept before it is read again, as after an update. */
  private const val VERSION_CHECK_MS = 60_000L

  private class VersionCheck(val reportsPopup: Boolean, val checkedAtMs: Long)

  private val versionChecks = HashMap<String, VersionCheck>()

  /**
   * Returns whether the Gboard-based keyboard [packageName] takes touch exploration as other
   * keyboards do: it selects the characters of its popup from the hover, reports each one, and
   * types the selected one when the hover exits. Older versions are explored by SandFly itself.
   */
  @JvmStatic
  fun usesTouchExploration(context: Context, packageName: CharSequence?): Boolean {
    if (packageName == null || !isGboardPackage(packageName)) {
      return false
    }
    val name = packageName.toString()
    val now = SystemClock.uptimeMillis()
    versionChecks[name]
      ?.takeIf { now - it.checkedAtMs < VERSION_CHECK_MS }
      ?.let {
        return it.reportsPopup
      }
    val versionName =
      try {
        context.packageManager.getPackageInfo(name, 0).versionName
      } catch (e: PackageManager.NameNotFoundException) {
        null
      }
    val reportsPopup = reportsPopupSelection(versionName)
    versionChecks[name] = VersionCheck(reportsPopup, now)
    return reportsPopup
  }

  /**
   * Returns whether a Gboard [versionName], such as "18.8.0.1022711984-beta-arm64-v8a", reports the
   * characters its popup selects.
   */
  @JvmStatic
  fun reportsPopupSelection(versionName: String?): Boolean {
    val parts = versionName?.split('.') ?: return false
    val major = parts.getOrNull(0)?.toIntOrNull() ?: return false
    val minor = parts.getOrNull(1)?.toIntOrNull() ?: return false
    return major > POPUP_REPORTING_MAJOR ||
      (major == POPUP_REPORTING_MAJOR && minor >= POPUP_REPORTING_MINOR)
  }

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
  ): AccessibilityNodeInfoCompat? =
    gboardRootAt(service, service.windowsOnAllDisplays.get(displayId) ?: emptyList(), x, y)

  /** [gboardRootAt] with the window list of the display already read. */
  @JvmStatic
  @RequiresApi(Build.VERSION_CODES.R)
  fun gboardRootAt(
    service: AccessibilityService,
    windows: List<AccessibilityWindowInfo>,
    x: Float,
    y: Float,
  ): AccessibilityNodeInfoCompat? {
    val window = topmostWindowIn(service, windows, x, y, withOverlays = false) ?: return null
    if (window.type != AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
      return null
    }
    val root = window.root ?: return null
    return if (isGboardPackage(root.packageName)) AccessibilityNodeInfoCompat.wrap(root) else null
  }

  /**
   * Returns the topmost window at [x], [y] on [displayId], from the cached window list. Accessibility
   * overlays other than the clipboard history are skipped unless [withOverlays].
   */
  @RequiresApi(Build.VERSION_CODES.R)
  private fun topmostWindowAt(
    service: AccessibilityService,
    displayId: Int,
    x: Float,
    y: Float,
    withOverlays: Boolean = false,
  ): AccessibilityWindowInfo? {
    val windows = service.windowsOnAllDisplays.get(displayId) ?: return null
    return topmostWindowIn(service, windows, x, y, withOverlays)
  }

  private fun topmostWindowIn(
    service: AccessibilityService,
    windows: List<AccessibilityWindowInfo>,
    x: Float,
    y: Float,
    withOverlays: Boolean,
  ): AccessibilityWindowInfo? {
    val region = Region()
    for (window in windows) {
      // The clipboard history covers the keyboard.
      if (
        !withOverlays &&
          window.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY &&
          !ClipboardHistoryOverlay.isOwnWindow(service, window)
      ) {
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

  /** A single bounded hit test for a continued touch whose first move has not arrived. */
  @JvmStatic
  @RequiresApi(Build.VERSION_CODES.R)
  fun stationaryTouchTargetAt(
    service: AccessibilityService,
    displayId: Int,
    x: Float,
    y: Float,
  ): AccessibilityNodeInfoCompat? {
    val budget = StationaryTouchLookup()
    val window = topmostWindowAt(service, displayId, x, y, withOverlays = true) ?: return null
    // SandFly's overlays, such as Screen search, cover the app: Android's touch exploration finds
    // their items.
    if (
      window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD ||
        window.type == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY
    ) {
      return null
    }
    val root = window.root ?: return null
    return focusableAt(AccessibilityNodeInfoCompat.wrap(root), x.toInt(), y.toInt(), Rect(), 0, budget)
  }

  // A node read may still take longer on its own. Do not start further reads then.
  private class StationaryTouchLookup {
    private val deadline = SystemClock.uptimeMillis() + 16
    private var childrenLeft = 48

    fun hasTime() = SystemClock.uptimeMillis() < deadline

    fun takeChild() = hasTime() && childrenLeft-- > 0

    /** Whether the lookup stopped before it read every child it needed. */
    fun exhausted() = !hasTime() || childrenLeft < 0
  }

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
   * Returns the popup of alternative characters that Gboard's [key] is in, for Gboard versions that
   * send no pane event when it opens: the highest of [key]'s ancestors that is not part of the
   * keyboard with [heldKey], the key the finger at [x], [y] holds, and is either narrower than
   * Gboard's window, [windowWidth] wide, or over [heldKey]. Gboard 18.8 beta puts the popup's
   * characters in a view over the whole window. Without [heldKey], the popup is the widest
   * ancestor narrower than the window and not under the finger. Ancestors are looked for as far up
   * as [popupKeys] looks down for the characters. Returns null if [key] is under the finger, or
   * none of its ancestors is such a popup.
   */
  @JvmStatic
  fun popupOfKey(
    key: AccessibilityNodeInfoCompat,
    heldKey: AccessibilityNodeInfoCompat?,
    windowWidth: Int,
    x: Float,
    y: Float,
  ): AccessibilityNodeInfoCompat? {
    val bounds = Rect()
    key.getBoundsInScreen(bounds)
    if (bounds.contains(x.toInt(), y.toInt()) || key == heldKey) {
      return null
    }
    val keyboard = heldKey?.let { ancestorsOf(it, MAX_DEPTH) }
    val heldBounds = Rect()
    heldKey?.getBoundsInScreen(heldBounds)
    var popup: AccessibilityNodeInfoCompat? = null
    var ancestor = key.parent
    var level = 1
    while (ancestor != null && level <= POPUP_DEPTH) {
      ancestor.getBoundsInScreen(bounds)
      // The keyboard itself, or a part of it with the held key, isn't the popup; nor is another
      // part of the keyboard as wide as the window, such as a row of keys.
      val isPopup =
        if (keyboard != null) {
          ancestor !in keyboard &&
            (bounds.width() < windowWidth || bounds.contains(heldBounds))
        } else {
          bounds.width() < windowWidth && !bounds.contains(x.toInt(), y.toInt())
        }
      if (!isPopup) {
        break
      }
      popup = ancestor
      ancestor = ancestor.parent
      level++
    }
    return popup
  }

  /** Returns [node]'s ancestors, up to [depth] of them. */
  private fun ancestorsOf(node: AccessibilityNodeInfoCompat, depth: Int): Set<AccessibilityNodeInfoCompat> {
    val ancestors = HashSet<AccessibilityNodeInfoCompat>()
    var ancestor = node.parent
    while (ancestor != null && ancestors.size < depth && ancestors.add(ancestor)) {
      ancestor = ancestor.parent
    }
    return ancestors
  }

  /**
   * Describes [key]'s ancestors, as far up as [popupOfKey] looks, for the log when no popup is
   * found.
   */
  @JvmStatic
  fun describeAncestors(key: AccessibilityNodeInfoCompat): String {
    val bounds = Rect()
    val description = StringBuilder()
    var ancestor = key.parent
    var level = 1
    while (ancestor != null && level <= POPUP_DEPTH) {
      ancestor.getBoundsInScreen(bounds)
      description
        .append(if (level > 1) " < " else "")
        .append(ancestor.className?.toString()?.substringAfterLast('.'))
        .append(' ')
        .append(bounds.toShortString())
        .append(" children ")
        .append(ancestor.childCount)
      ancestor = ancestor.parent
      level++
    }
    return description.toString()
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
    stationaryTouch: StationaryTouchLookup? = null,
  ): AccessibilityNodeInfoCompat? {
    if (depth > MAX_DEPTH || stationaryTouch?.hasTime() == false || !node.isVisibleToUser) {
      return null
    }
    node.getBoundsInScreen(bounds)
    if (!bounds.contains(x, y)) {
      return null
    }
    for (i in node.childCount - 1 downTo 0) {
      if (stationaryTouch?.takeChild() == false) return null
      val child = node.getChild(i) ?: continue
      val found = focusableAt(child, x, y, bounds, depth + 1, stationaryTouch)
      if (found != null) {
        return found
      }
    }
    // The item under the finger may be in a child left unread: not this container.
    if (stationaryTouch?.exhausted() == true) return null
    return node.takeIf { AccessibilityNodeInfoUtils.FILTER_SHOULD_FOCUS_EXCEPT_WEB_VIEW.accept(it) }
  }
}
