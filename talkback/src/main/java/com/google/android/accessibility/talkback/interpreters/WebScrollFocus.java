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

package com.google.android.accessibility.talkback.interpreters;

import android.graphics.Rect;
import android.text.TextUtils;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.Role;
import com.google.android.accessibility.utils.WebInterfaceUtils;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Finds an item of web content in view after the user scrolls the page.
 *
 * <p>Web content keeps accessibility focus on an item that scrolls out of view. A swipe from there
 * asks the page for the item next to it, and the page scrolls back to that item. So after a manual
 * scroll that leaves focus out of view, focus moves to the first item in view, or the last when
 * scrolling back.
 *
 * <p>Chrome clips the bounds of web nodes at the top and bottom of its viewport. A node above or
 * below the viewport has an inverted or zero-height rectangle at that edge, and a node that lies
 * across an edge ends at that edge.
 */
final class WebScrollFocus {

  /** Most nodes that one search reads from the app. The search runs on the main thread. */
  @VisibleForTesting static final int MAX_NODES_READ = 400;

  /**
   * Most children that one search reads to find the positions of the focus and its ancestors among
   * their siblings, apart from {@link #MAX_NODES_READ}.
   */
  @VisibleForTesting static final int MAX_INDEX_READS = 100;

  /** How long after the fingers leave the screen the page can still be moving from a fling. */
  @VisibleForTesting static final long FLING_MS = 3000;

  private WebScrollFocus() {}

  /** Whether a scroll from {@code node} can concern focus in web content. Asks the app nothing. */
  static boolean isWebContent(@Nullable AccessibilityNodeInfoCompat node) {
    return WebInterfaceUtils.supportsWebActions(node) || Role.getRole(node) == Role.ROLE_WEB_VIEW;
  }

  /**
   * Whether a scroll at {@code scrollTime} came from the user's fingers: a touch interaction that
   * started before it was still going on, or ended less than {@link #FLING_MS} before it. A swipe
   * is a touch interaction too, so the scroll does not count if focus moved after the touch
   * started: the page then scrolls to show the item that a swipe or continuous reading focused.
   * All times are {@link android.os.SystemClock#uptimeMillis} times, or -1 when not known.
   */
  static boolean isUserScroll(
      long scrollTime, long touchStartTime, long touchEndTime, boolean touching, long focusTime) {
    if (touchStartTime < 0 || scrollTime < touchStartTime || focusTime >= touchStartTime) {
      return false;
    }
    return touching || (touchEndTime >= touchStartTime && scrollTime - touchEndTime < FLING_MS);
  }

  /**
   * Whether {@code bounds} lie outside {@code viewport}. Bounds that are all zero are unknown, and
   * not outside.
   */
  static boolean isOutside(Rect bounds, Rect viewport) {
    if (bounds.left == 0 && bounds.top == 0 && bounds.right == 0 && bounds.bottom == 0) {
      return false;
    }
    return !(Math.min(bounds.left, bounds.right) < viewport.right
        && Math.max(bounds.left, bounds.right) > viewport.left
        && Math.min(bounds.top, bounds.bottom) < viewport.bottom
        && Math.max(bounds.top, bounds.bottom) > viewport.top);
  }

  /**
   * Returns the first item of web content in {@code root} that lies in {@code viewport}, or the
   * last one if not {@code forward}. An item that lies wholly in the viewport is preferred, because
   * focusing an item across an edge makes the page scroll to show all of it.
   *
   * <p>The search starts next to the item that has focus, so that a heading or tab bar that stays
   * at the top of the page is not taken in place of the content that scrolled into view. It falls
   * back to the start of {@code root}.
   *
   * @param focusAndAncestors the focused node followed by its ancestors, nearest first, or {@code
   *     null} when {@code root} does not hold the focus
   */
  static @Nullable AccessibilityNodeInfoCompat findItemInView(
      AccessibilityNodeInfoCompat root,
      @Nullable List<AccessibilityNodeInfoCompat> focusAndAncestors,
      Rect viewport,
      boolean forward) {
    Search search = new Search(viewport, forward);
    int rootIndex = (focusAndAncestors == null) ? -1 : focusAndAncestors.indexOf(root);
    for (int i = 0; i < rootIndex; i++) {
      AccessibilityNodeInfoCompat node = focusAndAncestors.get(i);
      AccessibilityNodeInfoCompat parent = focusAndAncestors.get(i + 1);
      int count = parent.getChildCount();
      int indexReadsLeft = search.indexReadsLeft;
      int index = search.indexInParent(parent, node, count);
      int read = indexReadsLeft - search.indexReadsLeft;
      int start;
      if (index >= 0) {
        start = forward ? index + 1 : index - 1;
      } else if (read >= count) {
        // The node is no longer among the children.
        break;
      } else {
        // Too many children to find its position by reading them. Items in view after it lie past
        // the children read, and before it, before the first child below the viewport.
        start = search.firstPastViewport(parent, forward ? read : 0, count);
      }
      AccessibilityNodeInfoCompat item = search.children(parent, start);
      if (item != null) {
        return item;
      }
      if (search.nodesLeft <= 0) {
        return search.partial;
      }
    }
    // An item next to the focus, even across an edge, before one at the start of the page.
    if (search.partial != null) {
      return search.partial;
    }
    AccessibilityNodeInfoCompat item =
        search.children(root, forward ? 0 : root.getChildCount() - 1);
    return (item != null) ? item : search.partial;
  }

  /** One search, with the nodes it may still read. */
  private static final class Search {
    final Rect viewport;
    final boolean forward;
    int nodesLeft = MAX_NODES_READ;
    int indexReadsLeft = MAX_INDEX_READS;
    /** The first item found that lies across an edge of the viewport. */
    @Nullable AccessibilityNodeInfoCompat partial;
    private final Rect bounds = new Rect();

    Search(Rect viewport, boolean forward) {
      this.viewport = viewport;
      this.forward = forward;
    }

    int indexInParent(
        AccessibilityNodeInfoCompat parent, AccessibilityNodeInfoCompat node, int count) {
      for (int i = 0; i < count && indexReadsLeft > 0; i++) {
        indexReadsLeft--;
        if (node.equals(parent.getChild(i))) {
          return i;
        }
      }
      return -1;
    }

    /**
     * Returns where to search the children of {@code parent} in {@code [from, count)}, which lie in
     * document order: forward, the first child that is not above the viewport; backward, the last
     * child that is not below it. A binary search.
     */
    int firstPastViewport(AccessibilityNodeInfoCompat parent, int from, int count) {
      int low = from;
      int high = count;
      while (low < high && nodesLeft > 0) {
        int middle = (low + high) >>> 1;
        nodesLeft--;
        AccessibilityNodeInfoCompat child = parent.getChild(middle);
        boolean before = false;
        if (child != null) {
          child.getBoundsInScreen(bounds);
          before = forward ? isAbove(bounds, viewport) : !isBelow(bounds, viewport);
        }
        if (before) {
          low = middle + 1;
        } else {
          high = middle;
        }
      }
      return forward ? low : high - 1;
    }

    /** Searches the children of {@code node} from {@code start} onwards, or backwards. */
    @Nullable AccessibilityNodeInfoCompat children(AccessibilityNodeInfoCompat node, int start) {
      int count = node.getChildCount();
      for (int i = start; i >= 0 && i < count; i += forward ? 1 : -1) {
        if (nodesLeft-- <= 0) {
          return null;
        }
        AccessibilityNodeInfoCompat child = node.getChild(i);
        if (child == null) {
          continue;
        }
        child.getBoundsInScreen(bounds);
        // Items outside a container are usually outside the viewport too.
        if (isOutside(bounds, viewport)) {
          continue;
        }
        if (isItem(child, bounds, viewport)) {
          if (isWhollyInside(bounds, viewport)) {
            return child;
          }
          if (partial == null) {
            partial = child;
          }
        }
        // An item across an edge can hold one that is wholly in view.
        AccessibilityNodeInfoCompat item = children(child, forward ? 0 : child.getChildCount() - 1);
        if (item != null || nodesLeft <= 0) {
          return item;
        }
      }
      return null;
    }
  }

  /** Whether {@code bounds} lie above {@code viewport}. Bounds that are all zero are unknown. */
  private static boolean isAbove(Rect bounds, Rect viewport) {
    return isOutside(bounds, viewport) && Math.max(bounds.top, bounds.bottom) <= viewport.top;
  }

  /** Whether {@code bounds} lie below {@code viewport}. Bounds that are all zero are unknown. */
  private static boolean isBelow(Rect bounds, Rect viewport) {
    return isOutside(bounds, viewport) && Math.min(bounds.top, bounds.bottom) >= viewport.bottom;
  }

  /**
   * Whether {@code bounds} lie wholly inside {@code viewport}. A node that ends exactly at the top
   * or bottom edge may be clipped there.
   */
  @VisibleForTesting
  static boolean isWhollyInside(Rect bounds, Rect viewport) {
    return bounds.top > viewport.top
        && bounds.bottom < viewport.bottom
        && bounds.left >= viewport.left
        && bounds.right <= viewport.right;
  }

  private static boolean isItem(AccessibilityNodeInfoCompat node, Rect bounds, Rect viewport) {
    // Documents and frames.
    if (Role.getRole(node) == Role.ROLE_WEB_VIEW) {
      return false;
    }
    Rect visible = new Rect(bounds);
    visible.sort();
    if (!visible.intersect(viewport)) {
      return false;
    }
    // A container that fills most of the viewport, such as a clickable page wrapper or section.
    if (node.getChildCount() > 0 && visible.height() * 4 > viewport.height() * 3) {
      return false;
    }
    return node.isScreenReaderFocusable()
        || AccessibilityNodeInfoUtils.isActionableForAccessibility(node)
        || !TextUtils.isEmpty(AccessibilityNodeInfoUtils.getNodeText(node));
  }
}
