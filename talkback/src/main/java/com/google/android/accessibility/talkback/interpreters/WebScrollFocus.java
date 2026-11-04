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
 * below the viewport has an inverted or zero-height rectangle at that edge.
 */
final class WebScrollFocus {

  /** Most nodes that one search reads from the app. The search runs on the main thread. */
  @VisibleForTesting static final int MAX_NODES_READ = 400;

  private WebScrollFocus() {}

  /** Whether a scroll from {@code node} can concern focus in web content. Asks the app nothing. */
  static boolean isWebContent(@Nullable AccessibilityNodeInfoCompat node) {
    return WebInterfaceUtils.supportsWebActions(node) || Role.getRole(node) == Role.ROLE_WEB_VIEW;
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
   * last one if not {@code forward}.
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
    int[] budget = {MAX_NODES_READ};
    int rootIndex = (focusAndAncestors == null) ? -1 : focusAndAncestors.indexOf(root);
    for (int i = 0; i < rootIndex; i++) {
      AccessibilityNodeInfoCompat node = focusAndAncestors.get(i);
      AccessibilityNodeInfoCompat parent = focusAndAncestors.get(i + 1);
      int index = indexInParent(parent, node, budget);
      if (index < 0) {
        break;
      }
      AccessibilityNodeInfoCompat item =
          searchChildren(parent, forward ? index + 1 : index - 1, viewport, forward, budget);
      if (item != null || budget[0] <= 0) {
        return item;
      }
    }
    return searchChildren(
        root, forward ? 0 : root.getChildCount() - 1, viewport, forward, budget);
  }

  private static int indexInParent(
      AccessibilityNodeInfoCompat parent, AccessibilityNodeInfoCompat node, int[] budget) {
    int count = parent.getChildCount();
    for (int i = 0; i < count && budget[0]-- > 0; i++) {
      if (node.equals(parent.getChild(i))) {
        return i;
      }
    }
    return -1;
  }

  /** Searches the children of {@code node} from {@code start} onwards, or backwards. */
  private static @Nullable AccessibilityNodeInfoCompat searchChildren(
      AccessibilityNodeInfoCompat node, int start, Rect viewport, boolean forward, int[] budget) {
    int count = node.getChildCount();
    Rect bounds = new Rect();
    for (int i = start; i >= 0 && i < count; i += forward ? 1 : -1) {
      if (budget[0]-- <= 0) {
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
        return child;
      }
      AccessibilityNodeInfoCompat item =
          searchChildren(
              child, forward ? 0 : child.getChildCount() - 1, viewport, forward, budget);
      if (item != null || budget[0] <= 0) {
        return item;
      }
    }
    return null;
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
    // A container that fills the whole viewport, such as a clickable page wrapper.
    if (node.getChildCount() > 0 && visible.equals(viewport)) {
      return false;
    }
    return node.isScreenReaderFocusable()
        || AccessibilityNodeInfoUtils.isActionableForAccessibility(node)
        || !TextUtils.isEmpty(AccessibilityNodeInfoUtils.getNodeText(node));
  }
}
