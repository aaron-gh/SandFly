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
import androidx.annotation.IntDef;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.utils.Role;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Finds the item of web content to focus after the user scrolled the page, in steps that each read
 * at most one node from the app, so that the main thread can handle touch input between them.
 *
 * <p>The steps read the ancestors of the focus to find the WebView that holds it and whether the
 * scrolled view holds it, then run a {@link WebScrollFocus.Search}.
 */
final class WebFocusRecovery {

  /** Most ancestors of a node read to find its WebView. */
  @VisibleForTesting static final int MAX_ANCESTORS_READ = 128;

  /** What the recovery found. */
  @IntDef({RUNNING, FOUND, IN_VIEW, NOTHING_IN_VIEW, NOT_WEB_SCROLL})
  @Retention(RetentionPolicy.SOURCE)
  @interface Outcome {}

  /** Not ended yet. */
  static final int RUNNING = 0;

  /** An item in view to focus: {@link #item}. */
  static final int FOUND = 1;

  /** The focus is in view and stays. */
  static final int IN_VIEW = 2;

  /** The focus is out of view, and no item is in view, or the search stopped. */
  static final int NOTHING_IN_VIEW = 3;

  /**
   * The scroll does not concern the WebView that holds the focus, or no WebView with bounds holds
   * it.
   */
  static final int NOT_WEB_SCROLL = 4;

  private final @Nullable AccessibilityNodeInfoCompat focus;
  private final @Nullable AccessibilityNodeInfoCompat lastFocused;
  private final AccessibilityNodeInfoCompat scrolled;
  private final boolean leftBehind;
  private final boolean forward;
  private final Rect scrolledBounds = new Rect();
  private final Rect focusBounds = new Rect();

  /** The focused node, or the node that had focus last, and its ancestors, nearest first. */
  private final List<AccessibilityNodeInfoCompat> focusAndAncestors = new ArrayList<>();

  /** No walk up the ancestors has run. */
  private static final int WALK_NONE = 0;

  /** The walk up from the focus, or from the node that had focus last. */
  private static final int WALK_FROM_FOCUS = 1;

  /** The walk up from the scrolled node. */
  private static final int WALK_FROM_SCROLLED = 2;

  /** The last walk up the ancestors that started. */
  private int walk = WALK_NONE;

  /** The node whose parent the next step reads, or null when the walk up has ended. */
  private @Nullable AccessibilityNodeInfoCompat walking;

  private int ancestorsRead;
  private @Nullable AccessibilityNodeInfoCompat webView;
  private boolean scrolledHoldsFocus;

  /** Whether the walk up from the scrolled node reached {@link #webView}. */
  private boolean scrolledInWebView;

  private WebScrollFocus.@Nullable Search search;
  private @Outcome int outcome = RUNNING;
  private @Nullable AccessibilityNodeInfoCompat item;

  /**
   * @param focus the focused node, or null when focus was lost or is on a whole document, so that
   *     the item is sought in the scrolled content next to {@code lastFocused}, or from its start,
   *     or its end
   * @param lastFocused when {@code focus} is null, the node that had focus last, as it was then, or
   *     null
   * @param scrolled the node that the user scrolled
   * @param leftBehind whether the focus is known to be out of view, even if its bounds lie in the
   *     WebView
   * @param forward whether the content scrolled on, so that the first item in view is sought, not
   *     the last
   */
  WebFocusRecovery(
      @Nullable AccessibilityNodeInfoCompat focus,
      @Nullable AccessibilityNodeInfoCompat lastFocused,
      AccessibilityNodeInfoCompat scrolled,
      boolean leftBehind,
      boolean forward) {
    this.focus = focus;
    this.lastFocused = (focus == null) ? lastFocused : null;
    this.scrolled = scrolled;
    this.leftBehind = leftBehind;
    this.forward = forward;
    scrolled.getBoundsInScreen(scrolledBounds);
    if (focus != null) {
      focus.getBoundsInScreen(focusBounds);
      startWalk(WALK_FROM_FOCUS, focus);
    } else if (this.lastFocused != null) {
      startWalk(WALK_FROM_FOCUS, this.lastFocused);
    } else {
      startWalk(WALK_FROM_SCROLLED, scrolled);
    }
  }

  /** Returns what the recovery found, or {@link #RUNNING}. */
  @Outcome
  int outcome() {
    return outcome;
  }

  /** Returns the item to focus when the outcome is {@link #FOUND}. */
  @Nullable AccessibilityNodeInfoCompat item() {
    return item;
  }

  /** Ends the recovery at its time limit, with an item across an edge of the view if one was found. */
  void stop() {
    if (outcome != RUNNING) {
      return;
    }
    if (search != null) {
      search.stop();
      end(search.result());
    } else {
      outcome = NOTHING_IN_VIEW;
    }
  }

  /** Does one step, reading at most one node. Returns whether the recovery ended. */
  boolean step() {
    if (outcome != RUNNING) {
      return true;
    }
    if (search != null) {
      if (search.step()) {
        end(search.result());
      }
    } else if (walking != null) {
      stepWalk();
    } else {
      decide();
    }
    return outcome != RUNNING;
  }

  private void end(@Nullable AccessibilityNodeInfoCompat found) {
    item = found;
    outcome = (found != null) ? FOUND : NOTHING_IN_VIEW;
  }

  private void startWalk(int walk, AccessibilityNodeInfoCompat from) {
    this.walk = walk;
    walking = from;
    ancestorsRead = 0;
  }

  /** Reads the parent of {@link #walking}. */
  private void stepWalk() {
    AccessibilityNodeInfoCompat node = walking;
    if (walk == WALK_FROM_SCROLLED) {
      if (node.equals(webView)) {
        scrolledInWebView = true;
        walking = null;
        return;
      }
    } else {
      if (focusAndAncestors.size() >= MAX_ANCESTORS_READ || focusAndAncestors.contains(node)) {
        walking = null;
        return;
      }
      focusAndAncestors.add(node);
      scrolledHoldsFocus |= node.equals(scrolled);
    }
    if (ancestorsRead >= MAX_ANCESTORS_READ) {
      walking = null;
      return;
    }
    ancestorsRead++;
    AccessibilityNodeInfoCompat parent = node.getParent();
    // The outermost of the nested documents that a WebView reports: a document inside a page is a
    // web element's child, not a WebView's.
    if (webView == null
        && Role.getRole(node) == Role.ROLE_WEB_VIEW
        && Role.getRole(parent) != Role.ROLE_WEB_VIEW) {
      webView = node;
      if (walk == WALK_FROM_SCROLLED) {
        scrolledInWebView = true;
        walking = null;
        return;
      }
    }
    walking = parent;
  }

  /** Decides from the ancestors read whether to search, and where. */
  private void decide() {
    if (focus == null && walk == WALK_FROM_FOCUS && !(scrolledHoldsFocus && webView != null)) {
      // The node that had focus last is no longer in the scrolled content. Search that content
      // from its start, or its end.
      focusAndAncestors.clear();
      webView = null;
      startWalk(WALK_FROM_SCROLLED, scrolled);
      return;
    }
    if (webView == null) {
      outcome = NOT_WEB_SCROLL;
      return;
    }
    Rect viewport = new Rect();
    webView.getBoundsInScreen(viewport);
    if (viewport.isEmpty()) {
      outcome = NOT_WEB_SCROLL;
      return;
    }
    if (focus == null) {
      // Search the scrolled content next to the node that had focus last, or from its start, or
      // its end.
      if (!scrolledBounds.isEmpty() && !viewport.intersect(scrolledBounds)) {
        outcome = NOT_WEB_SCROLL;
        return;
      }
      startSearch(scrolled, viewport);
      return;
    }
    if (scrolledHoldsFocus) {
      // Focus can also scroll out of a list or frame in the page.
      if (!scrolledBounds.isEmpty() && !viewport.intersect(scrolledBounds)) {
        outcome = NOT_WEB_SCROLL;
        return;
      }
    } else if (!WebScrollFocus.isWebContent(scrolled)) {
      // The scroll is outside the WebView that holds the focus.
      outcome = NOT_WEB_SCROLL;
      return;
    } else if (!scrolled.equals(webView) && !scrolledInWebView) {
      if (walk != WALK_FROM_SCROLLED) {
        // Find whether the scrolled node lies in the WebView that holds the focus.
        startWalk(WALK_FROM_SCROLLED, scrolled);
        return;
      }
      outcome = NOT_WEB_SCROLL;
      return;
    }
    if (!leftBehind && !WebScrollFocus.isOutside(focusBounds, viewport)) {
      outcome = IN_VIEW;
      return;
    }

    // Search the part of the page that scrolled, or the whole page when a view around the WebView
    // scrolled.
    AccessibilityNodeInfoCompat searchRoot = scrolled;
    if (!WebScrollFocus.isWebContent(scrolled)) {
      searchRoot = webView;
    } else if (!scrolledHoldsFocus
        && !scrolledBounds.isEmpty()
        && !viewport.intersect(scrolledBounds)) {
      outcome = NOT_WEB_SCROLL;
      return;
    }
    startSearch(searchRoot, viewport);
  }

  private void startSearch(AccessibilityNodeInfoCompat searchRoot, Rect viewport) {
    // From a strip of category tabs, search on from the strip, or back from the end: the tabs next
    // to the focus are not the content that scrolled into view.
    @Nullable List<AccessibilityNodeInfoCompat> searchFrom =
        focusAndAncestors.isEmpty() ? null : focusAndAncestors;
    int strip = WebScrollFocus.outermostSidewaysStrip(focusAndAncestors, searchRoot);
    if (strip >= 0) {
      searchFrom = forward ? focusAndAncestors.subList(strip, focusAndAncestors.size()) : null;
    }
    search = new WebScrollFocus.Search(searchRoot, searchFrom, viewport, forward);
  }
}
