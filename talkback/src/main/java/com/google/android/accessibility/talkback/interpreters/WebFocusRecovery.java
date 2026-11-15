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
import android.os.SystemClock;
import androidx.annotation.IntDef;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.utils.ConcurrentNodeQueries;
import com.google.android.accessibility.utils.ConcurrentNodeQueries.ParentRead;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.Role;
import com.google.android.accessibility.utils.WebInterfaceUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Finds the item of web content to focus after the user scrolled the page. Node reads can run on
 * the existing bounded query pool; the serial fallback yields between individual reads.
 *
 * <p>The steps read the ancestors of the focus to find the WebView that holds it and whether the
 * scrolled view holds it, then run a {@link WebScrollFocus.Search}.
 */
final class WebFocusRecovery {

  /** Most ancestors of a node read to find its WebView. */
  @VisibleForTesting static final int MAX_ANCESTORS_READ = 128;

  /** The optional parallel check is limited to a short, already captured path. */
  private static final int MAX_READY_PATH_PARENTS = 16;
  private static final long READY_PATH_WAIT_MS = 80;

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
  private AccessibilityNodeInfoCompat scrolled;
  private final boolean leftBehind;
  private final boolean forward;

  /** A strip that the page scrolled sideways, which may be fixed across the top of the view. */
  private @Nullable AccessibilityNodeInfoCompat strip;

  private boolean stripRead;
  private final Rect stripBounds = new Rect();

  /** Whether the focused item moved since it was focused, so the page did not fix it in place. */
  private final boolean focusMoved;
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
  private List<AccessibilityNodeInfoCompat> readyPath = Collections.emptyList();
  private int readySourceIndex;
  private boolean readyPathChecked;
  private @Outcome int outcome = RUNNING;
  private @Nullable AccessibilityNodeInfoCompat item;
  private final AtomicBoolean backgroundCancelled = new AtomicBoolean();
  private long readDeadline = Long.MAX_VALUE;

  /** Only node reads run on a worker; the caller still validates and applies the result. */
  @Nullable Future<Boolean> startInBackground(long deadline) {
    AccessibilityNodeInfoCompat anchor = focus != null ? focus
        : lastFocused != null ? lastFocused : scrolled;
    return startInBackground(deadline,
        read -> ConcurrentNodeQueries.readAlongsidePath(anchor, read));
  }

  @VisibleForTesting
  @Nullable Future<Boolean> startInBackground(
      long deadline, Function<Supplier<Boolean>, Future<Boolean>> submit) {
    // The strip is also kept by the interpreter. Copy it here, on the caller's thread, so that the
    // worker never reads a node that the main thread can refresh.
    if (strip != null) strip = AccessibilityNodeInfoCompat.obtain(strip);
    Future<Boolean> work = submit.apply(() -> {
      readDeadline = deadline;
      if (backgroundCancelled.get() || Thread.currentThread().isInterrupted()) return false;
      while (!backgroundCancelled.get() && !Thread.currentThread().isInterrupted()
          && SystemClock.uptimeMillis() < deadline) {
        if (step()) return !backgroundCancelled.get() && !Thread.currentThread().isInterrupted();
      }
      if (backgroundCancelled.get() || Thread.currentThread().isInterrupted()) return false;
      LogUtils.w("WebFocusRecovery", "Stopped the search for a web item in view at its time limit");
      stop();
      return true;
    });
    // A rejected worker keeps the original one-read-at-a-time fallback. The optional
    // parent batch must not introduce an 80 ms join on the main thread.
    if (work == null) readyPath = Collections.emptyList();
    return work;
  }

  void cancelBackground() {
    backgroundCancelled.set(true);
  }

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
   * @param strip a strip in the scrolled window that the page scrolled sideways while the user
   *     scrolled other content, or null. When it lies across the top of the view, items under it
   *     count as out of view.
   * @param focusMoved whether the focused item moved since it was focused. Focus on an item that
   *     the page keeps in place, such as in a header above the strip, stays.
   */
  WebFocusRecovery(
      @Nullable AccessibilityNodeInfoCompat focus,
      @Nullable AccessibilityNodeInfoCompat lastFocused,
      AccessibilityNodeInfoCompat scrolled,
      boolean leftBehind,
      boolean forward,
      @Nullable AccessibilityNodeInfoCompat strip,
      boolean focusMoved) {
    this(focus, lastFocused, scrolled, leftBehind, forward, strip, focusMoved,
        Collections.emptyList());
  }

  WebFocusRecovery(
      @Nullable AccessibilityNodeInfoCompat focus,
      @Nullable AccessibilityNodeInfoCompat lastFocused,
      AccessibilityNodeInfoCompat scrolled,
      boolean leftBehind,
      boolean forward,
      @Nullable AccessibilityNodeInfoCompat strip,
      boolean focusMoved,
      List<AccessibilityNodeInfoCompat> readyPath) {
    this.strip = strip;
    this.focusMoved = focusMoved;
    this.focus = focus;
    this.lastFocused = (focus == null) ? lastFocused : null;
    this.scrolled = scrolled;
    this.leftBehind = leftBehind;
    this.forward = forward;
    scrolled.getBoundsInScreen(scrolledBounds);
    if (focus != null) {
      focus.getBoundsInScreen(focusBounds);
      startWalk(WALK_FROM_FOCUS, focus);
      selectReadyPath(readyPath);
    } else if (this.lastFocused != null) {
      startWalk(WALK_FROM_FOCUS, this.lastFocused);
    } else {
      startWalk(WALK_FROM_SCROLLED, scrolled);
    }
  }

  private void selectReadyPath(List<AccessibilityNodeInfoCompat> path) {
    if (path.isEmpty() || focus == null || !sameIdentity(path.get(0), focus)) return;
    int source = -1;
    for (int i = 0; i < path.size() && i <= MAX_READY_PATH_PARENTS; i++) {
      AccessibilityNodeInfoCompat node = path.get(i);
      if (node.getWindowId() != focus.getWindowId()) return;
      if (sameIdentity(node, scrolled)) source = i;
      if (isNativeWebView(node)) {
        if (i > 0 && source >= 0) {
          readyPath = new ArrayList<>(path.subList(0, i + 1));
          readyPath.set(0, focus);
          readySourceIndex = source;
        }
        return;
      }
    }
  }

  private static boolean isNativeWebView(AccessibilityNodeInfoCompat node) {
    return Role.getRole(node) == Role.ROLE_WEB_VIEW
        && !WebInterfaceUtils.supportsWebActions(node);
  }

  static boolean sameIdentity(
      AccessibilityNodeInfoCompat saved, AccessibilityNodeInfoCompat current) {
    if (!saved.equals(current) || saved.getWindowId() != current.getWindowId()) return false;
    String id = saved.getUniqueId();
    return id != null ? id.equals(current.getUniqueId())
        : current.getUniqueId() == null && isNativeWebView(saved) && isNativeWebView(current);
  }

  /** A native WebView and its own root document report the same physical viewport. */
  @VisibleForTesting
  static boolean sameNativeViewport(AccessibilityNodeInfoCompat first,
      AccessibilityNodeInfoCompat second, List<AccessibilityNodeInfoCompat> path) {
    for (int i = 0; i + 1 < path.size(); i++) {
      AccessibilityNodeInfoCompat document = path.get(i);
      AccessibilityNodeInfoCompat host = path.get(i + 1);
      if (isNativeWebView(host) && Role.getRole(document) == Role.ROLE_WEB_VIEW
          && "rootWebArea".contentEquals(String.valueOf(
              AccessibilityNodeInfoUtils.getChromeRole(document)))
          && ((sameIdentity(first, host) && sameIdentity(second, document))
              || (sameIdentity(second, host) && sameIdentity(first, document)))) return true;
    }
    return false;
  }

  @VisibleForTesting
  static List<AccessibilityNodeInfoCompat> verifiedAncestorNodes(
      List<AccessibilityNodeInfoCompat> saved,
      Map<AccessibilityNodeInfoCompat, ParentRead> reads) {
    List<AccessibilityNodeInfoCompat> current = new ArrayList<>();
    for (int i = 0; i + 1 < saved.size(); i++) {
      ParentRead read = reads.get(saved.get(i));
      if (read == null || !sameIdentity(saved.get(i), read.getChild())
          || !sameIdentity(saved.get(i + 1), read.getParent())
          || (i == 0 && !read.getChild().isAccessibilityFocused())) {
        return Collections.emptyList();
      }
      current.add(read.getChild());
      if (i + 2 == saved.size()) current.add(read.getParent());
    }
    return !current.isEmpty() && isNativeWebView(current.get(current.size() - 1))
        ? current : Collections.emptyList();
  }

  private void tryCurrentAncestors() {
    int last = readyPath.size() - 1;
    Map<AccessibilityNodeInfoCompat, ParentRead> reads =
        ConcurrentNodeQueries.refreshParentsForRecovery(
            readyPath.subList(0, last), readyPathWaitMs(readDeadline, SystemClock.uptimeMillis()));
    List<AccessibilityNodeInfoCompat> current = verifiedAncestorNodes(readyPath, reads);
    if (current.isEmpty()) return;
    focusAndAncestors.addAll(current);
    current.get(0).getBoundsInScreen(focusBounds);
    scrolled = current.get(readySourceIndex);
    scrolled.getBoundsInScreen(scrolledBounds);
    scrolledHoldsFocus = true;
    webView = current.get(current.size() - 1);
    walking = null;
  }

  @VisibleForTesting
  static long readyPathWaitMs(long deadline, long now) {
    return Math.min(READY_PATH_WAIT_MS, Math.max(0, deadline - now));
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

  /** Advances the bounded path check or one serial read. Returns whether recovery ended. */
  boolean step() {
    if (outcome != RUNNING) {
      return true;
    }
    if (!readyPathChecked && !readyPath.isEmpty()) {
      readyPathChecked = true;
      tryCurrentAncestors();
    } else if (search != null) {
      if (search.step()) {
        end(search.result());
      }
    } else if (strip != null && !stripRead) {
      // Where the strip is now: one read.
      stripRead = true;
      if (strip.refresh() && strip.isVisibleToUser()) {
        strip.getBoundsInScreen(stripBounds);
      } else {
        strip = null;
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
    // Read the ancestry without unrelated sibling or descendant branches.
    AccessibilityNodeInfoCompat parent = node.getParent(
        AccessibilityNodeInfoCompat.FLAG_PREFETCH_ANCESTORS
            | AccessibilityNodeInfoCompat.FLAG_PREFETCH_UNINTERRUPTIBLE);
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
      if (scrolledHoldsFocus
          && WebScrollFocus.isWebContent(scrolled)
          && !WebInterfaceUtils.supportsWebActions(node)) {
        // The web source is already on this path. Native ancestors above its WebView
        // cannot change the search; keep reading them only for a native scroll source.
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
      startSearch(scrolled, belowStrip(viewport));
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
    // Focus in a header or strip that the page fixes at the top stays, and so does focus that
    // only part of the strip covers.
    boolean underStrip =
        strip != null
            && focusMoved
            && !focusAndAncestors.contains(strip)
            && WebScrollFocus.isUnderFixedStrip(focusBounds, viewport, stripBounds);
    if (!leftBehind && !underStrip && !WebScrollFocus.isOutside(focusBounds, viewport)) {
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
    startSearch(searchRoot, belowStrip(viewport));
  }

  /** Returns the part of {@code viewport} below {@link #strip} when it is fixed across its top. */
  private Rect belowStrip(Rect viewport) {
    return (strip == null) ? viewport : WebScrollFocus.belowFixedStrip(viewport, stripBounds);
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
