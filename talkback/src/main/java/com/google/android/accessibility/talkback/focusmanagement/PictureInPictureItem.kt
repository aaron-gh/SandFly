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

package com.google.android.accessibility.talkback.focusmanagement

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.view.accessibility.AccessibilityWindowInfo
import androidx.annotation.VisibleForTesting
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.AccessibilityServiceCompatUtils
import com.google.android.accessibility.utils.AccessibilityWindowInfoUtils

/**
 * A picture-in-picture window, such as a video playing over the home screen, is one item for
 * swipes: its root, named "<app> picture in picture". The controls in the window and in the
 * system's picture-in-picture menu over it are actions of that item. Touch still reaches each
 * control.
 */
object PictureInPictureItem {
  private const val MAX_CONTROLS = 40
  private const val MAX_NODES = 400
  private const val MIN_SLACK_PX = 8
  private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
  // The title of System UI's picture-in-picture menu window, from Android 12.
  private const val PIP_MENU_TITLE = "PipMenuView"

  /** A control of the picture-in-picture window, offered as an action of its item. */
  class Control(val label: CharSequence, val node: AccessibilityNodeInfoCompat)

  /**
   * The picture-in-picture window among [windows] that the window [windowId] is, or lies over as
   * its menu does; null when it is neither.
   */
  @JvmStatic
  fun pipWindowFor(windows: List<AccessibilityWindowInfo>, windowId: Int): AccessibilityWindowInfo? {
    val pip = windows.firstOrNull { AccessibilityWindowInfoUtils.isPictureInPicture(it) } ?: return null
    if (pip.id == windowId) return pip
    val window = windows.firstOrNull { it.id == windowId } ?: return null
    return if (liesOver(window, pip)) pip else null
  }

  /** The ids of the windows among [windows] that lie over the picture-in-picture window. */
  @JvmStatic
  fun overlayWindowIds(windows: List<AccessibilityWindowInfo>): Set<Int> {
    val pip =
      windows.firstOrNull { AccessibilityWindowInfoUtils.isPictureInPicture(it) }
        ?: return emptySet()
    return windows.filter { it.id != pip.id && liesOver(it, pip) }.map { it.id }.toSet()
  }

  /**
   * Whether [window] is a system window over the picture-in-picture window [pip], such as its menu:
   * it has about the bounds of [pip], or it is System UI's picture-in-picture menu with its centre
   * in [pip]. Other system windows over it, such as the volume dialog or another app's floating
   * button, are their own.
   */
  @VisibleForTesting
  @JvmStatic
  fun liesOver(window: AccessibilityWindowInfo, pip: AccessibilityWindowInfo): Boolean {
    if (window.id == pip.id || window.type != AccessibilityWindowInfo.TYPE_SYSTEM) return false
    val pipBounds = Rect().also { pip.getBoundsInScreen(it) }
    val bounds = Rect().also { window.getBoundsInScreen(it) }
    if (bounds.isEmpty || pipBounds.isEmpty) return false
    if (roughlyEqual(bounds, pipBounds)) return true
    if (!pipBounds.contains(bounds.centerX(), bounds.centerY())) return false
    if (!isMenuTitle(window.title)) return false
    // Only now, for a window over the picture-in-picture window, ask for its root.
    return isMenuPackage(AccessibilityWindowInfoUtils.getRoot(window)?.packageName)
  }

  /** Whether [bounds] differ from [pipBounds] by at most a tenth of its shorter side at each edge. */
  @VisibleForTesting
  @JvmStatic
  fun roughlyEqual(bounds: Rect, pipBounds: Rect): Boolean {
    val slack = maxOf(MIN_SLACK_PX, minOf(pipBounds.width(), pipBounds.height()) / 10)
    return Math.abs(bounds.left - pipBounds.left) <= slack &&
      Math.abs(bounds.top - pipBounds.top) <= slack &&
      Math.abs(bounds.right - pipBounds.right) <= slack &&
      Math.abs(bounds.bottom - pipBounds.bottom) <= slack
  }

  /** Whether a window titled [title] can be the menu: titled as System UI's menu, or untitled. */
  @VisibleForTesting
  @JvmStatic
  fun isMenuTitle(title: CharSequence?): Boolean =
    title.isNullOrEmpty() || title.toString() == PIP_MENU_TITLE

  /** Whether a window whose root has [packageName] can be the menu: System UI's. */
  @VisibleForTesting
  @JvmStatic
  fun isMenuPackage(packageName: CharSequence?): Boolean =
    packageName != null && packageName.toString() == SYSTEM_UI_PACKAGE

  /** Whether [node] is the root of a picture-in-picture window, which swipes focus as one item. */
  @JvmStatic
  fun isItem(node: AccessibilityNodeInfoCompat?): Boolean =
    // The window check comes first: it is usually answered from the window cache, and most nodes
    // fail it, so their parents aren't fetched.
    node != null && AccessibilityNodeInfoUtils.isPictureInPicture(node) && node.parent == null

  /** "<app> picture in picture" for the item, or null when [node] isn't one. */
  @JvmStatic
  fun label(context: Context, node: AccessibilityNodeInfoCompat): CharSequence? {
    if (!isItem(node)) return null
    val name =
      appName(context, node.packageName)
        ?: AccessibilityNodeInfoUtils.getWindow(node)?.title?.takeIf { it.isNotBlank() }
        ?: return null
    return context.getString(R.string.template_overlay_window, name)
  }

  private fun appName(context: Context, packageName: CharSequence?): CharSequence? {
    if (packageName.isNullOrEmpty()) return null
    return try {
      val pm = context.packageManager
      pm.getApplicationLabel(pm.getApplicationInfo(packageName.toString(), 0)).takeIf {
        it.isNotBlank()
      }
    } catch (e: PackageManager.NameNotFoundException) {
      null
    }
  }

  /**
   * The labelled controls in the picture-in-picture window of [item] and in the windows over it,
   * such as play, pause, close and full screen.
   */
  @JvmStatic
  fun controls(service: AccessibilityService, item: AccessibilityNodeInfoCompat): List<Control> {
    val windows = AccessibilityServiceCompatUtils.getWindows(service)
    val pip = pipWindowFor(windows, item.windowId) ?: return emptyList()
    val roots = mutableListOf(item)
    for (window in windows) {
      if (liesOver(window, pip)) {
        AccessibilityWindowInfoUtils.getRootCompat(window)?.let { roots.add(it) }
      }
    }
    val controls = mutableListOf<Control>()
    val labels = HashSet<String>()
    var visited = 0
    for (root in roots) {
      val queue = ArrayDeque<AccessibilityNodeInfoCompat>()
      queue.add(root)
      while (queue.isNotEmpty() && visited < MAX_NODES && controls.size < MAX_CONTROLS) {
        val node = queue.removeFirst()
        visited++
        if (node != item && node.isVisibleToUser && isClickable(node)) {
          val label = labelOf(node)
          if (label != null && labels.add(label.toString())) {
            controls.add(Control(label, node))
          }
        }
        for (i in 0 until node.childCount) {
          node.getChild(i)?.let { queue.add(it) }
        }
      }
    }
    return controls
  }

  private fun isClickable(node: AccessibilityNodeInfoCompat): Boolean =
    node.isEnabled &&
      (node.isClickable ||
        AccessibilityNodeInfoUtils.supportsAction(node, AccessibilityNodeInfoCompat.ACTION_CLICK))

  /** The control's description or text, or that of its first child with either. */
  private fun labelOf(node: AccessibilityNodeInfoCompat): CharSequence? {
    ownLabel(node)?.let {
      return it
    }
    for (i in 0 until node.childCount) {
      node.getChild(i)?.let { child -> ownLabel(child)?.let { return it } }
    }
    return null
  }

  private fun ownLabel(node: AccessibilityNodeInfoCompat): CharSequence? =
    node.contentDescription?.takeIf { it.isNotBlank() } ?: node.text?.takeIf { it.isNotBlank() }
}
