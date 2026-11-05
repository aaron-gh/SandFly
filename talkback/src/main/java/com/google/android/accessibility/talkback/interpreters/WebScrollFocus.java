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
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.Role;
import com.google.android.accessibility.utils.WebInterfaceUtils;
import java.util.ArrayDeque;
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

  /**
   * Children of one node above which a search finds where the items in view start by a binary
   * search over their bounds, instead of reading them from the first.
   */
  @VisibleForTesting static final int MIN_CHILDREN_FOR_BINARY_SEARCH = 32;

  /** How long after the fingers leave the screen the page can still be moving from a fling. */
  @VisibleForTesting static final long FLING_MS = 3000;

  private WebScrollFocus() {}

  /** Whether a scroll from {@code node} can concern focus in web content. Asks the app nothing. */
  static boolean isWebContent(@Nullable AccessibilityNodeInfoCompat node) {
    return WebInterfaceUtils.supportsWebActions(node) || Role.getRole(node) == Role.ROLE_WEB_VIEW;
  }

  /**
   * Whether {@code node} scrolls only sideways, as a strip of category tabs does. Chrome gives a
   * scrollable node the directions it can scroll in as actions. Asks the app nothing.
   */
  static boolean scrollsOnlySideways(@Nullable AccessibilityNodeInfoCompat node) {
    if (node == null) {
      return false;
    }
    boolean sideways = false;
    for (AccessibilityActionCompat action : node.getActionList()) {
      int id = action.getId();
      if (id == AccessibilityActionCompat.ACTION_SCROLL_UP.getId()
          || id == AccessibilityActionCompat.ACTION_SCROLL_DOWN.getId()) {
        return false;
      }
      sideways |=
          id == AccessibilityActionCompat.ACTION_SCROLL_LEFT.getId()
              || id == AccessibilityActionCompat.ACTION_SCROLL_RIGHT.getId();
    }
    return sideways;
  }

  /**
   * Returns the position in {@code focusAndAncestors} of the outermost node below {@code root}
   * that scrolls only sideways, or -1. The page scrolls such a strip of category tabs itself to
   * follow the content that the user scrolls, so the focus in it is no guide to where that content
   * is.
   */
  static int outermostSidewaysStrip(
      List<AccessibilityNodeInfoCompat> focusAndAncestors, AccessibilityNodeInfoCompat root) {
    for (int i = focusAndAncestors.indexOf(root) - 1; i >= 0; i--) {
      if (scrollsOnlySideways(focusAndAncestors.get(i))) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Whether a scroll at {@code scrollTime} came from the user's fingers: a touch interaction that
   * started before it was still going on, or ended less than {@link #FLING_MS} before it. A swipe
   * is a touch interaction too, so the scroll does not count if focus moved after the touch
   * started: the page then scrolls to show the item that a swipe or continuous reading focused.
   * All times are {@link android.os.SystemClock#uptimeMillis} times, or -1 when not known.
   */
  static boolean isUserScroll(
      long scrollTime,
      long touchStartTime,
      long touchEndTime,
      boolean touching,
      long previousTouchStartTime,
      long previousTouchEndTime,
      long focusTime) {
    if (touchStartTime > scrollTime) {
      // A new touch started after the scroll, such as a swipe before the scroll was handled.
      return isUserScroll(
          scrollTime, previousTouchStartTime, previousTouchEndTime, /* touching= */ false, focusTime);
    }
    return isUserScroll(scrollTime, touchStartTime, touchEndTime, touching, focusTime);
  }

  /** {@link #isUserScroll} for a scroll during or after the touch interaction given. */
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
   * last one if not {@code forward}. Runs a {@link Search} to its end.
   *
   * @param focusAndAncestors the focused node followed by its ancestors, nearest first, or {@code
   *     null} when {@code root} does not hold the focus
   */
  static @Nullable AccessibilityNodeInfoCompat findItemInView(
      AccessibilityNodeInfoCompat root,
      @Nullable List<AccessibilityNodeInfoCompat> focusAndAncestors,
      Rect viewport,
      boolean forward) {
    Search search = new Search(root, focusAndAncestors, viewport, forward);
    while (!search.step()) {}
    return search.result();
  }

  /**
   * A search for the first item of web content in a root that lies in a viewport, or the last one
   * when searching backward. An item that lies wholly in the viewport is preferred, because
   * focusing an item across an edge makes the page scroll to show all of it.
   *
   * <p>The search starts next to the item that has focus, so that a heading or tab bar that stays
   * at the top of the page is not taken in place of the content that scrolled into view. It falls
   * back to the start of the root, or its end when searching backward.
   *
   * <p>Each {@link #step} reads at most one node from the app, so that the caller can let the main
   * thread handle touch input between steps, and stop the search at a time limit.
   */
  static final class Search {
    private final AccessibilityNodeInfoCompat root;
    private final @Nullable List<AccessibilityNodeInfoCompat> focusAndAncestors;
    private final Rect viewport;
    private final boolean forward;
    private final Rect bounds = new Rect();

    private int nodesLeft = MAX_NODES_READ;
    private int indexReadsLeft = MAX_INDEX_READS;

    /** The first item found that lies across an edge of the viewport. */
    private @Nullable AccessibilityNodeInfoCompat partial;

    private @Nullable AccessibilityNodeInfoCompat result;
    private boolean done;

    /**
     * Position in {@link #focusAndAncestors} of the node whose siblings are searched, or past the
     * ancestors, when the root is searched from its start.
     */
    private int level = 0;

    private final int rootIndex;

    /** Whether the position of the node at {@link #level} among its siblings is being read. */
    private boolean findingIndex;

    private int indexScan;
    private int indexReadsAtLevelStart;

    /** Whether the root has been searched from its start. */
    private boolean rootSearched;

    /** The nodes whose children are being searched, innermost last. */
    private final ArrayDeque<Frame> frames = new ArrayDeque<>();

    /** The children of one node that are still to be searched. */
    private static final class Frame {
      final AccessibilityNodeInfoCompat node;
      final int count;

      /** The next child to read, once the binary search for where to start has ended. */
      int next;

      /** The range of a binary search for the first child to read, while {@code low < high}. */
      int low;

      int high;

      Frame(AccessibilityNodeInfoCompat node, int count, int next) {
        this.node = node;
        this.count = count;
        this.next = next;
      }
    }

    Search(
        AccessibilityNodeInfoCompat root,
        @Nullable List<AccessibilityNodeInfoCompat> focusAndAncestors,
        Rect viewport,
        boolean forward) {
      this.root = root;
      this.focusAndAncestors = focusAndAncestors;
      this.viewport = new Rect(viewport);
      this.forward = forward;
      rootIndex = (focusAndAncestors == null) ? -1 : focusAndAncestors.indexOf(root);
      startLevel();
    }

    /** Returns the item found once the search has ended, or null. */
    @Nullable AccessibilityNodeInfoCompat result() {
      return result;
    }

    /** Ends the search, with an item across an edge of the viewport if one was found. */
    void stop() {
      finish(partial);
    }

    /** Does one step of the search, reading at most one node. Returns whether the search ended. */
    boolean step() {
      while (!done) {
        if (findingIndex) {
          stepIndex();
          return done;
        }
        if (!frames.isEmpty()) {
          if (stepFrames()) {
            return done;
          }
          continue;
        }
        // The children searched from the current level held nothing.
        if (nodesLeft <= 0) {
          finish(partial);
        } else if (level < rootIndex) {
          level++;
          startLevel();
        } else if (!rootSearched) {
          // An item next to the focus, even across an edge, before one at the start of the page.
          if (partial != null) {
            finish(partial);
          } else {
            rootSearched = true;
            pushStart(root);
          }
        } else {
          finish(partial);
        }
      }
      return true;
    }

    private void finish(@Nullable AccessibilityNodeInfoCompat item) {
      result = item;
      done = true;
      frames.clear();
      findingIndex = false;
    }

    private void startLevel() {
      if (level < rootIndex) {
        findingIndex = true;
        indexScan = 0;
        indexReadsAtLevelStart = indexReadsLeft;
      } else {
        level = Math.max(level, rootIndex);
      }
    }

    /** Reads the next sibling of the node at {@link #level} to find its position among them. */
    private void stepIndex() {
      AccessibilityNodeInfoCompat node = focusAndAncestors.get(level);
      AccessibilityNodeInfoCompat parent = focusAndAncestors.get(level + 1);
      int count = parent.getChildCount();
      if (indexScan < count && indexReadsLeft > 0) {
        indexReadsLeft--;
        int i = indexScan++;
        if (node.equals(parent.getChild(i))) {
          findingIndex = false;
          frames.push(new Frame(parent, count, forward ? i + 1 : i - 1));
        }
        return;
      }
      findingIndex = false;
      int read = indexReadsAtLevelStart - indexReadsLeft;
      if (read >= count) {
        // The node is no longer among the children, such as an item that the page replaced or
        // removed as it scrolled. Search the children from the first, or the last.
        pushStart(parent);
        return;
      }
      // Too many children to find its position by reading them. Items in view after it lie past
      // the children read, and before it, before the first child below the viewport.
      Frame frame = new Frame(parent, count, 0);
      frame.low = forward ? read : 0;
      frame.high = count;
      frames.push(frame);
    }

    /** Pushes {@code node} to be searched from its first child, or its last one. */
    private void pushStart(AccessibilityNodeInfoCompat node) {
      int count = node.getChildCount();
      Frame frame = new Frame(node, count, forward ? 0 : count - 1);
      if (count > MIN_CHILDREN_FOR_BINARY_SEARCH) {
        // A long list: find where the items in view start without reading those before them.
        frame.low = 0;
        frame.high = count;
      }
      frames.push(frame);
    }

    /**
     * Reads one node from the innermost frame. Returns whether a node was read, or the search
     * ended; false when a frame ended without reading one.
     */
    private boolean stepFrames() {
      Frame frame = frames.peek();
      if (frame.low < frame.high) {
        stepBinarySearch(frame);
        return true;
      }
      int i = frame.next;
      if (i < 0 || i >= frame.count) {
        frames.pop();
        return false;
      }
      if (nodesLeft-- <= 0) {
        finish(partial);
        return true;
      }
      frame.next += forward ? 1 : -1;
      AccessibilityNodeInfoCompat child = frame.node.getChild(i);
      if (child == null) {
        return true;
      }
      child.getBoundsInScreen(bounds);
      // Items outside a container are usually outside the viewport too.
      if (isOutside(bounds, viewport)) {
        return true;
      }
      if (isItem(child, bounds, viewport)) {
        if (isWhollyInside(bounds, viewport)) {
          finish(child);
          return true;
        }
        if (partial == null) {
          partial = child;
        }
      }
      // An item across an edge can hold one that is wholly in view.
      pushStart(child);
      return true;
    }

    /**
     * One step of a binary search over the children of the frame's node, which lie in document
     * order, for where to start: forward, the first child that is not above the viewport;
     * backward, the last child that is not below it.
     */
    private void stepBinarySearch(Frame frame) {
      if (nodesLeft <= 0) {
        finish(partial);
        return;
      }
      int middle = (frame.low + frame.high) >>> 1;
      nodesLeft--;
      AccessibilityNodeInfoCompat child = frame.node.getChild(middle);
      boolean before = false;
      if (child != null) {
        child.getBoundsInScreen(bounds);
        before = forward ? isAbove(bounds, viewport) : !isBelow(bounds, viewport);
      }
      if (before) {
        frame.low = middle + 1;
      } else {
        frame.high = middle;
      }
      if (frame.low >= frame.high) {
        frame.next = forward ? frame.low : frame.high - 1;
      }
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
