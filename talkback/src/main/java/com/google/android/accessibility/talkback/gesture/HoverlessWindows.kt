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

package com.google.android.accessibility.talkback.gesture

import android.graphics.Rect
import android.os.SystemClock
import android.text.TextUtils
import android.view.accessibility.AccessibilityWindowInfo
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Items under the finger in app windows whose hover events find nothing, found from the window's
 * nodes.
 *
 * Some apps, such as Threads on its main tabs, report touch exploration's hover only for an empty
 * container as large as the window, while their items are in the node tree with correct bounds.
 * When SandFly detects gestures itself, it knows where the finger was when it requested touch
 * exploration. A hover entering such a container then focuses the item under that point instead.
 *
 * Everything here runs on the main thread.
 */
@MainThread
object HoverlessWindows {
  private const val TAG = "HoverlessWindows"

  /** The share of its window's area a hovered container covers for its hover to count as empty. */
  @VisibleForTesting const val MOST_OF_WINDOW = 0.75f

  /** How long after touch exploration starts its finger position is used for a hover. */
  private const val START_POINT_MS = 1_000L

  private var startX = 0f
  private var startY = 0f
  private var startTimeMs = 0L
  private var hasStart = false

  /**
   * Notes where the finger is as SandFly requests touch exploration for it, from the touch's motion
   * events, which stop once touch exploration starts.
   */
  @JvmStatic
  fun onTouchExplorationRequested(x: Float, y: Float) {
    startX = x
    startY = y
    startTimeMs = SystemClock.uptimeMillis()
    hasStart = true
    coverStartPending = true
    coverItem = null
  }

  /** Forgets the finger position, as a new touch starts or SandFly stops detecting gestures. */
  @JvmStatic
  fun clearStart() {
    hasStart = false
    coverStartPending = false
    coverItem = null
  }

  /**
   * Returns the item to focus for a hover entering [touched], which touch exploration found nothing
   * to focus for, or null. The item is the one at the finger's position as touch exploration
   * started, when [touched] is an empty container covering most of an app window.
   */
  @JvmStatic
  fun itemUnderFinger(touched: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat? {
    if (!hasStart || SystemClock.uptimeMillis() - startTimeMs > START_POINT_MS) {
      return null
    }
    if (
      !TextUtils.isEmpty(touched.text) ||
        !TextUtils.isEmpty(touched.contentDescription) ||
        AccessibilityNodeInfoUtils.isActionableForAccessibility(touched) ||
        touched.isScreenReaderFocusable
    ) {
      return null
    }
    val window = AccessibilityNodeInfoUtils.getWindow(touched) ?: return null
    if (window.type != AccessibilityWindowInfo.TYPE_APPLICATION) {
      return null
    }
    val windowBounds = Rect().also(window::getBoundsInScreen)
    val touchedBounds = Rect().also(touched::getBoundsInScreen)
    val x = startX.toInt()
    val y = startY.toInt()
    if (!windowBounds.contains(x, y) || !coversMostOf(touchedBounds, windowBounds)) {
      return null
    }
    val root = window.root ?: return null
    val item = KeyboardExploration.nodeUnder(root, startX, startY)
    if (item == null || item == touched) {
      return null
    }
    LogUtils.d(TAG, "Hover entered an empty container; the item at %d, %d is focused", x, y)
    return item
  }

  /**
   * Returns the item to focus in place of [focusable], which touch exploration found under the
   * finger, or null to focus [focusable]. That is when [focusable] has nothing to speak or scrolls,
   * covers most of an app window, and an item with something to speak is at the finger's position
   * as touch exploration started: the smallest such item, so that a button wins over the view
   * behind it.
   *
   * TikTok's video page is a clickable frame with no label as large as the page, and its hover
   * reaches only that frame and the feed's pager before it, wherever the finger is, though the Like
   * and Comment buttons are drawn over them. The page's children are not listed in the order they
   * are drawn, so the item can't be found from the last child under the finger.
   */
  @JvmStatic
  fun itemOverSilentCover(focusable: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat? {
    val now = SystemClock.uptimeMillis()
    // TikTok reports one touch as hovers entering the pager and then the frame within a few
    // milliseconds. Both get the item found for the first.
    val found = coverItem
    if (found != null && now - coverItemTimeMs <= HOVER_BURST_MS && isSilentCover(focusable) != null) {
      return found
    }
    coverItem = null
    // Only the touch's first hover is at the finger's start position. A finger that starts on a
    // labelled button and slides onto a video surface must not take the button with it.
    if (!coverStartPending) {
      return null
    }
    coverStartPending = false
    if (!hasStart || now - startTimeMs > START_POINT_MS) {
      return null
    }
    val root = isSilentCover(focusable) ?: return null
    val x = startX.toInt()
    val y = startY.toInt()
    val item = smallestSpeakingItemAt(root, x, y, focusable)
    if (item == null || item == focusable) {
      return null
    }
    LogUtils.d(TAG, "Hover entered a silent cover; the item at %d, %d is focused", x, y)
    coverItem = item
    coverItemTimeMs = now
    return item
  }

  /** How soon after the first hover of a touch the app's further hovers of it come. */
  private const val HOVER_BURST_MS = 150L

  /** Whether the next hover is the first of the touch, at the finger's start position. */
  private var coverStartPending = false

  /** The item found for the touch's first hover, for the app's further hovers of the same touch. */
  private var coverItem: AccessibilityNodeInfoCompat? = null
  private var coverItemTimeMs = 0L

  /**
   * Returns the root of [focusable]'s window if [focusable] is a silent cover: a view with nothing
   * to speak, or a pager or list read only as its role, such as "Page", covering most of an app
   * window that holds the finger's start position. Otherwise null.
   */
  private fun isSilentCover(focusable: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat? {
    if (!focusable.isScrollable && AccessibilityNodeInfoUtils.isSpeakingNode(focusable, null)) {
      return null
    }
    val window = AccessibilityNodeInfoUtils.getWindow(focusable) ?: return null
    if (window.type != AccessibilityWindowInfo.TYPE_APPLICATION) {
      return null
    }
    val windowBounds = Rect().also(window::getBoundsInScreen)
    val focusableBounds = Rect().also(focusable::getBoundsInScreen)
    if (
      !windowBounds.contains(startX.toInt(), startY.toInt()) ||
        !coversMostOf(focusableBounds, windowBounds)
    ) {
      return null
    }
    return window.root
  }

  /** Most nodes read to find the item under the finger. */
  private const val MAX_NODES = 1500

  /**
   * Returns the smallest node under [root] at [x], [y] that would be focused, has something to
   * speak, does not scroll and is not drawn under [cover], or null. A text field that scrolls
   * counts. A scrim or overlay drawn over the page keeps its own hover.
   */
  @VisibleForTesting
  @JvmStatic
  fun smallestSpeakingItemAt(
    root: AccessibilityNodeInfoCompat,
    x: Int,
    y: Int,
    cover: AccessibilityNodeInfoCompat? = null,
  ): AccessibilityNodeInfoCompat? {
    val coverPath = cover?.let(::pathToRoot)
    val cache = HashMap<AccessibilityNodeInfoCompat, Boolean>()
    val bounds = Rect()
    var best: AccessibilityNodeInfoCompat? = null
    var bestArea = Long.MAX_VALUE
    val queue = ArrayDeque<AccessibilityNodeInfoCompat>()
    queue.add(root)
    var read = 0
    while (queue.isNotEmpty() && read < MAX_NODES) {
      val node = queue.removeFirst()
      read++
      if (!node.isVisibleToUser) {
        continue
      }
      node.getBoundsInScreen(bounds)
      if (!bounds.contains(x, y)) {
        continue
      }
      val area = bounds.width().toLong() * bounds.height()
      // A pager or list is no item of its own: TikTok's feed pager is as large as the "Video" view
      // on its page, and is read only as "Page".
      if (
        area < bestArea &&
          !(node.isScrollable && !node.isEditable) &&
          AccessibilityNodeInfoUtils.shouldFocusNode(node, cache) &&
          AccessibilityNodeInfoUtils.isSpeakingNode(node, cache) &&
          (coverPath == null || !isDrawnUnder(node, coverPath))
      ) {
        best = node
        bestArea = area
      }
      for (i in 0 until node.childCount) {
        node.getChild(i)?.let(queue::add)
      }
    }
    return best
  }

  /** [node] and its ancestors, from [node] up to the root. */
  private fun pathToRoot(node: AccessibilityNodeInfoCompat): List<AccessibilityNodeInfoCompat> {
    val path = ArrayList<AccessibilityNodeInfoCompat>()
    var current: AccessibilityNodeInfoCompat? = node
    while (current != null && path.size <= MAX_DEPTH) {
      path.add(current)
      current = current.parent
    }
    return path
  }

  private const val MAX_DEPTH = 60

  /**
   * Whether [node] is drawn under the cover whose path to the root is [coverPath]: [node] holds the
   * cover, or below their nearest common ancestor the branch with [node] is drawn before the branch
   * with the cover. The drawing order of the two branches decides; if Android reports the same for
   * both, the later child is taken as drawn over the earlier one. An item inside the cover is drawn
   * over it.
   */
  @VisibleForTesting
  @JvmStatic
  fun isDrawnUnder(
    node: AccessibilityNodeInfoCompat,
    coverPath: List<AccessibilityNodeInfoCompat>,
  ): Boolean {
    val cover = coverPath.firstOrNull() ?: return false
    if (coverPath.indexOf(node) > 0) {
      return true
    }
    var child: AccessibilityNodeInfoCompat = node
    var parent: AccessibilityNodeInfoCompat? = node.parent
    var depth = 0
    while (parent != null && depth <= MAX_DEPTH) {
      if (parent == cover) {
        return false
      }
      val index = coverPath.indexOf(parent)
      if (index > 0) {
        val coverBranch = coverPath[index - 1]
        val nodeOrder = child.drawingOrder
        val coverOrder = coverBranch.drawingOrder
        if (nodeOrder != coverOrder) {
          return nodeOrder < coverOrder
        }
        return childIndex(parent, child) < childIndex(parent, coverBranch)
      }
      child = parent
      parent = parent.parent
      depth++
    }
    return false
  }

  private fun childIndex(parent: AccessibilityNodeInfoCompat, child: AccessibilityNodeInfoCompat): Int {
    for (i in 0 until parent.childCount) {
      if (parent.getChild(i) == child) {
        return i
      }
    }
    return -1
  }

  /** Whether [node] covers at least [MOST_OF_WINDOW] of [window]'s area. */
  @JvmStatic
  fun coversMostOf(node: Rect, window: Rect): Boolean {
    val overlap = Rect()
    if (window.isEmpty || !overlap.setIntersect(node, window)) {
      return false
    }
    val overlapArea = overlap.width().toLong() * overlap.height()
    val windowArea = window.width().toLong() * window.height()
    return overlapArea >= MOST_OF_WINDOW * windowArea
  }

  @VisibleForTesting
  fun clear() {
    clearStart()
  }
}
