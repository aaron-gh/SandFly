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
   * its centre lies in [pip] and it is at most twice as big.
   */
  @VisibleForTesting
  @JvmStatic
  fun liesOver(window: AccessibilityWindowInfo, pip: AccessibilityWindowInfo): Boolean {
    if (window.id == pip.id || window.type != AccessibilityWindowInfo.TYPE_SYSTEM) return false
    val pipBounds = Rect().also { pip.getBoundsInScreen(it) }
    val bounds = Rect().also { window.getBoundsInScreen(it) }
    if (bounds.isEmpty || pipBounds.isEmpty) return false
    return pipBounds.contains(bounds.centerX(), bounds.centerY()) &&
      bounds.width().toLong() * bounds.height() <=
        2L * pipBounds.width().toLong() * pipBounds.height()
  }

  /** Whether [node] is the root of a picture-in-picture window, which swipes focus as one item. */
  @JvmStatic
  fun isItem(node: AccessibilityNodeInfoCompat?): Boolean =
    node != null && node.parent == null && AccessibilityNodeInfoUtils.isPictureInPicture(node)

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
