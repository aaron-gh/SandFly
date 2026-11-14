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

package com.google.android.accessibility.talkback.focusmanagement;

import android.graphics.Rect;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.utils.Role;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Finds content that is in the node tree but off screen, so a swipe can scroll just far enough to
 * show it. Scroll views lay out all of their content, so the next item exists before it is shown.
 * Lists and grids recycle their rows, and pagers hold whole pages off to the side, so those are
 * left to the usual page scroll. So are Compose scrolling columns, which animate to show an item,
 * so the search right after would still find it off screen.
 */
final class OffscreenContent {
  // Large scroll views are rare, and walking a huge tree on every edge swipe would cost more than
  // the page scroll it saves.
  private static final int MAX_NODES = 1500;

  private OffscreenContent() {}

  /**
   * Whether {@code scrollable} is a scroll view, which keeps all of its content in the tree and
   * scrolls to show an item before the action returns. AndroidX nested scroll views say they are
   * scroll views too.
   */
  static boolean keepsAllContent(AccessibilityNodeInfoCompat scrollable) {
    int role = Role.getRole(scrollable);
    return scrollable.isVisibleToUser()
        && scrollable.getCollectionInfo() == null
        && (role == Role.ROLE_SCROLL_VIEW || role == Role.ROLE_HORIZONTAL_SCROLL_VIEW);
  }

  /**
   * The off-screen content to show next: the first item after the pivot, and an item about a screen
   * further on.
   */
  record Ahead(AccessibilityNodeInfoCompat next, AccessibilityNodeInfoCompat pageEnd) {}

  /**
   * Finds the first node after {@code pivot} in {@code scrollable}, in tree order, that is off
   * screen or cut off at the edge and has something to say or do, and the node about a screen further on, judged by how many such
   * nodes are on screen now. Showing the screen's worth at once keeps the next swipes on screen.
   *
   * <p>Nodes that are hidden rather than scrolled away are skipped: collapsed nodes with no size,
   * everything inside a collapsed container, and everything inside a nested pager or scrolling
   * container, whose off-screen content is off to the side or not laid out. A nested list that
   * can't scroll, such as the rows of Android Settings' home page, lays out all of its items.
   */
  static @Nullable Ahead findAhead(
      AccessibilityNodeInfoCompat scrollable, AccessibilityNodeInfoCompat pivot, boolean forward) {
    List<AccessibilityNodeInfoCompat> nodes = new ArrayList<>();
    List<Integer> subtreeEnds = new ArrayList<>();
    List<Boolean> hidden = new ArrayList<>();
    if (!collect(scrollable, nodes, subtreeEnds, hidden)) {
      return null;
    }
    int pivotIndex = nodes.indexOf(pivot);
    if (pivotIndex < 0) {
      return null;
    }
    int onScreen = 0;
    for (int i = 0; i < nodes.size(); i++) {
      if (!hidden.get(i) && nodes.get(i).isVisibleToUser() && saysOrDoes(nodes.get(i))) {
        onScreen++;
      }
    }
    List<AccessibilityNodeInfoCompat> ahead = new ArrayList<>();
    int wanted = Math.max(1, onScreen);
    if (forward) {
      // Skip the pivot's own descendants, which the pivot already speaks.
      for (int i = subtreeEnds.get(pivotIndex); i < nodes.size() && ahead.size() < wanted; i++) {
        if (!hidden.get(i) && isCandidate(nodes.get(i))) {
          ahead.add(nodes.get(i));
        }
      }
    } else {
      for (int i = pivotIndex - 1; i >= 0 && ahead.size() < wanted; i--) {
        // Ancestors of the pivot come before it in tree order, but they are on screen around it.
        if (subtreeEnds.get(i) > pivotIndex) {
          continue;
        }
        if (!hidden.get(i) && isCandidate(nodes.get(i))) {
          ahead.add(nodes.get(i));
        }
      }
    }
    return ahead.isEmpty() ? null : new Ahead(ahead.get(0), ahead.get(ahead.size() - 1));
  }

  /**
   * Lists the subtree in pre-order, with the index just past each node's subtree, and whether each
   * node is hidden inside a collapsed or nested scrolling container. Returns {@code false} if the
   * subtree is too large.
   */
  private static boolean collect(
      AccessibilityNodeInfoCompat root,
      List<AccessibilityNodeInfoCompat> nodes,
      List<Integer> subtreeEnds,
      List<Boolean> hidden) {
    // Each entry is a node to visit, or the index of a visited node whose subtree ends here.
    Deque<Object> stack = new ArrayDeque<>();
    stack.push(new Visit(root, /* hidden= */ false));
    while (!stack.isEmpty()) {
      Object top = stack.pop();
      if (top instanceof Integer start) {
        subtreeEnds.set(start, nodes.size());
        continue;
      }
      Visit visit = (Visit) top;
      AccessibilityNodeInfoCompat node = visit.node;
      if (nodes.size() >= MAX_NODES) {
        return false;
      }
      int index = nodes.size();
      nodes.add(node);
      subtreeEnds.add(index + 1);
      hidden.add(visit.hidden);
      stack.push(index);
      boolean childrenHidden = visit.hidden || (node != root && hidesChildren(node));
      for (int i = node.getChildCount() - 1; i >= 0; i--) {
        AccessibilityNodeInfoCompat child = node.getChild(i);
        if (child != null) {
          stack.push(new Visit(child, childrenHidden));
        }
      }
    }
    return true;
  }

  /**
   * Whether the children of a node inside the scroll view can be off screen without being scrolled
   * away: in a collapsed container, a pager, or a nested container that scrolls on its own.
   */
  @SuppressWarnings("deprecation") // getBoundsInParent is the only unclipped size a node reports.
  private static boolean hidesChildren(AccessibilityNodeInfoCompat node) {
    Rect size = new Rect();
    node.getBoundsInParent(size);
    return size.width() <= 0
        || size.height() <= 0
        || node.isScrollable()
        || Role.getRole(node) == Role.ROLE_PAGER;
  }

  /** A node to visit, and whether it is hidden inside a container. */
  private record Visit(AccessibilityNodeInfoCompat node, boolean hidden) {}

  /**
   * Whether {@code node} is off screen or cut off at the edge, has a size, and has something to say
   * or do. A node cut off at the edge has not been read yet, and would go past the other edge.
   */
  @SuppressWarnings("deprecation") // getBoundsInParent is the only unclipped size a node reports.
  private static boolean isCandidate(AccessibilityNodeInfoCompat node) {
    Rect size = new Rect();
    node.getBoundsInParent(size);
    if (size.width() <= 0 || size.height() <= 0) {
      return false;
    }
    if (node.isVisibleToUser()) {
      Rect shown = new Rect();
      node.getBoundsInScreen(shown);
      if (shown.width() >= size.width() && shown.height() >= size.height()) {
        return false;
      }
    }
    return saysOrDoes(node);
  }

  private static boolean saysOrDoes(AccessibilityNodeInfoCompat node) {
    return !TextUtils.isEmpty(node.getText())
        || !TextUtils.isEmpty(node.getContentDescription())
        || node.isFocusable()
        || node.isClickable()
        || node.isScreenReaderFocusable();
  }
}
