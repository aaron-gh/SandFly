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

package com.google.android.accessibility.talkback.focusmanagement;

import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.focusmanagement.FocusProcessorForLogicalNavigation.ScrollAheadPlan;
import com.google.android.libraries.accessibility.utils.log.LogUtils;

/**
 * Scrolls the list of the focused item ahead, while the item is read, when the item ends the list
 * on screen, so that the next item is on screen before the next swipe. A part of the focused item
 * always stays on screen, and focus stays where it is. The scrolls are SandFly's own and make no
 * sound. Any touch, key, window change or new focus stops it.
 */
public final class ScrollAhead {

  private static final String TAG = "ScrollAhead";


  /** Checks this often whether the list has stopped. */
  private static final long CHECK_MS = 30;

  /** A list counts as stopped once it has sent no scroll event for this long. */
  @VisibleForTesting static final long QUIET_MS = 150;

  /** The longest wait for a list to stop before scrolling ahead gives up. */
  private static final long MAX_WAIT_MS = 1000;

  /** Most pages scrolled ahead of one focused item. */
  @VisibleForTesting static final int MAX_PAGES = 10;

  /** Focus that a move to the previous item placed this recently is scrolled ahead backward. */
  private static final long BACKWARD_MOVE_MS = 5000;

  /** Least height of the focused item that must stay on screen after a page, in dp. */
  private static final int MIN_REMAIN_DP = 48;

  private static volatile boolean enabled = true;

  /** The scroll ahead of the service running now, for its events. */
  private static @Nullable ScrollAhead current;

  private final FocusProcessorForLogicalNavigation navigation;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private @Nullable AccessibilityNodeInfoCompat focused;
  private int pages;
  private long waitStart;

  /** Whether this scrolls toward the next item, or back toward the previous one. */
  private boolean forward = true;

  /** When this last scrolled a page, in {@link SystemClock#uptimeMillis()}. */
  private long lastPageUptime;

  /** Run when scrolling ahead has brought the next item on screen and read the window's order. */
  private static @Nullable Runnable onNextItemShown;
  private @Nullable AccessibilityNodeInfoCompat scrolledList;
  private int deltaSumAtScroll;

  /** Where the focused item was before the last page. */
  private @Nullable Rect pageItemBounds;

  /** The list that the last page scrolled, and where it was on screen. */
  private @Nullable AccessibilityNodeInfoCompat pageList;

  private final Rect pageListBounds = new Rect();

  /** Whether the list was still when the last page started, so that its distance can be learned. */
  private boolean quietAtPage;

  /** The scroll action count once the last page started; another scroll since spoils its distance. */
  private int actionCountAtPage;

  ScrollAhead(FocusProcessorForLogicalNavigation navigation) {
    this.navigation = navigation;
    current = this;
  }

  /**
   * Sets what runs once scrolling ahead has brought the next item on screen, such as preparing
   * what the next swipe says.
   */
  public static void setOnNextItemShown(@Nullable Runnable runnable) {
    onNextItemShown = runnable;
  }

  /** Sets whether lists are scrolled ahead of the focused item. */
  public static void setEnabled(boolean on) {
    enabled = on;
    if (!on) {
      cancelAll("turned off");
    }
  }

  /**
   * Starts scrolling ahead of a newly focused item, and stops on window changes and touches.
   * Called on the main thread for each accessibility event.
   */
  public static void onAccessibilityEvent(AccessibilityEvent event) {
    ScrollAhead scrollAhead = current;
    if (scrollAhead == null) {
      return;
    }
    switch (event.getEventType()) {
      case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED -> {
        TraversalTreeCache.forgetScrolledAway();
        // HTML navigation cannot produce a native scroll-ahead plan. Cancel the old plan
        // without asking the app for a node that onFocused/planScrollAhead would discard.
        boolean htmlNavigation = event.getAction() == AccessibilityNodeInfoCompat.ACTION_NEXT_HTML_ELEMENT
            || event.getAction() == AccessibilityNodeInfoCompat.ACTION_PREVIOUS_HTML_ELEMENT;
        if (!enabled || htmlNavigation) {
          scrollAhead.onFocused(null);
          break;
        }
        android.view.accessibility.AccessibilityNodeInfo raw = event.getSource();
        scrollAhead.onFocused(raw == null ? null : AccessibilityNodeInfoCompat.wrap(raw));
      }
      case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED -> {
        scrollAhead.onFocusCleared();
        scrollAhead.cancel(AccessibilityEvent.eventTypeToString(event.getEventType()));
      }
      case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
          AccessibilityEvent.TYPE_TOUCH_INTERACTION_START ->
          scrollAhead.cancel(AccessibilityEvent.eventTypeToString(event.getEventType()));
      default -> {}
    }
  }

  /** Stops scrolling ahead, such as when the user presses a key. */
  public static void cancelAll(String reason) {
    ScrollAhead scrollAhead = current;
    if (scrollAhead != null) {
      scrollAhead.cancel(reason);
    }
  }

  private void onFocused(@Nullable AccessibilityNodeInfoCompat node) {
    cancel("new focus");
    if (!enabled || node == null) {
      return;
    }
    focused = node;
    pages = 0;
    pageItemBounds = null;
    pageList = null;
    waitStart = SystemClock.uptimeMillis();
    // At once, so that the page has scrolled before the next swipe.
    handler.post(this::step);
  }

  /**
   * Lets the next swipe go on from the focused item when its focus is cleared after a page: the
   * page has moved it off screen. Swipes and keys stop scrolling ahead before they move focus.
   */
  private void onFocusCleared() {
    if (focused != null && pages > 0) {
      noteScrolledAway(focused);
    }
  }

  private void noteScrolledAway(AccessibilityNodeInfoCompat node) {
    Rect bounds = pageItemBounds;
    AccessibilityNodeInfoCompat list = pageList;
    if (bounds == null || list == null) {
      return;
    }
    LogUtils.d(TAG, "The page moved the focused item at %s off screen.", bounds);
    // The page was planned to leave a part of the item on screen, so it was longer than learned.
    FocusProcessorForLogicalNavigation.distrustPage(list);
    TraversalTreeCache.rememberScrolledAway(
        node.getWindowId(), bounds, pageListBounds, deltaSumAtScroll, forward);
  }

  private void cancel(String reason) {
    if (focused != null && pages > 0) {
      LogUtils.d(TAG, "Stopped scrolling ahead after %d pages: %s.", pages, reason);
    }
    handler.removeCallbacksAndMessages(null);
    focused = null;
    scrolledList = null;
  }

  /** Scrolls a page once the list has stopped, until the next item is on screen. */
  private void step() {
    AccessibilityNodeInfoCompat node = focused;
    if (node == null) {
      return;
    }
    long now = SystemClock.uptimeMillis();
    if (!isQuiet(now, FocusProcessorForLogicalNavigation.lastScrollEventUptime())
        && !(pages == 0 && isOwnPageMoving(now))) {
      if (now - waitStart > MAX_WAIT_MS) {
        LogUtils.d(TAG, "Not scrolling ahead: the list doesn't stop.");
        focused = null;
        return;
      }
      handler.postDelayed(this::step, CHECK_MS);
      return;
    }
    if (navigation.isReadingContinuously()) {
      focused = null;
      return;
    }
    // The way of the last move to the next or previous item: the next move most likely goes the
    // same way.
    if (pages == 0) {
      forward = !navigation.movedBackwardWithin(BACKWARD_MOVE_MS);
    }
    if (scrolledList != null) {
      FocusProcessorForLogicalNavigation.learnPage(
          scrolledList,
          Math.abs(FocusProcessorForLogicalNavigation.scrollDeltaSumY() - deltaSumAtScroll),
          quietAtPage,
          FocusProcessorForLogicalNavigation.scrollActionCount() != actionCountAtPage);
      scrolledList = null;
    }
    if (!node.refresh() || (pages > 0 && !node.isVisibleToUser())) {
      if (pages > 0) {
        LogUtils.d(TAG, "Stopped scrolling ahead after %d pages: the focused item left.", pages);
        noteScrolledAway(node);
      }
      focused = null;
      return;
    }
    boolean[] nextShown = new boolean[1];
    int minRemainPx =
        Math.round(MIN_REMAIN_DP * navigationDensity());
    @Nullable ScrollAheadPlan plan =
        navigation.planScrollAhead(node, minRemainPx, forward, nextShown);
    if (plan == null) {
      if (nextShown[0] && pages > 0) {
        LogUtils.d(
            TAG,
            "%s item on screen after scrolling ahead %d pages.",
            forward ? "Next" : "Previous",
            pages);
        // The scroll cleared the saved order; read it now rather than on the next swipe.
        waitStart = SystemClock.uptimeMillis();
        handler.postDelayed(() -> prepareOrder(node), CHECK_MS);
      } else {
        focused = null;
      }
      return;
    }
    if (pages >= MAX_PAGES) {
      LogUtils.d(TAG, "Stopped scrolling ahead after %d pages.", pages);
      focused = null;
      return;
    }
    deltaSumAtScroll = FocusProcessorForLogicalNavigation.scrollDeltaSumY();
    quietAtPage = isQuiet(now, FocusProcessorForLogicalNavigation.lastScrollEventUptime());
    if (!navigation.scrollListAhead(plan.list, plan.scrollAction)) {
      focused = null;
      return;
    }
    actionCountAtPage = FocusProcessorForLogicalNavigation.scrollActionCount();
    scrolledList = plan.list;
    pageItemBounds = plan.itemBounds;
    pageList = plan.list;
    plan.list.getBoundsInScreen(pageListBounds);
    lastPageUptime = SystemClock.uptimeMillis();
    pages++;
    LogUtils.d(
        TAG,
        "Scrolling ahead of the focused item %s: page %d, last page %s px.",
        forward ? "forward" : "back",
        pages,
        plan.pagePx > 0 ? String.valueOf(plan.pagePx) : "not known");
    waitStart = SystemClock.uptimeMillis();
    // The scroll event of the page comes after the scroll; wait for it to start.
    handler.postDelayed(this::step, CHECK_MS + QUIET_MS);
  }

  private void prepareOrder(AccessibilityNodeInfoCompat node) {
    if (focused != node) {
      return;
    }
    long now = SystemClock.uptimeMillis();
    if (!isQuiet(now, FocusProcessorForLogicalNavigation.lastScrollEventUptime())
        && now - waitStart < MAX_WAIT_MS) {
      handler.postDelayed(() -> prepareOrder(node), CHECK_MS);
      return;
    }
    focused = null;
    long start = SystemClock.uptimeMillis();
    navigation.prepareWindowOrder(node);
    LogUtils.d(TAG, "Window order read in %d ms.", SystemClock.uptimeMillis() - start);
    Runnable shown = onNextItemShown;
    if (shown != null) {
      shown.run();
    }
  }

  /**
   * Whether the list moves only from a page this scrolled for an earlier item, which a new page
   * may take over from, rather than from a swipe's scroll, which the next item is waited for in.
   */
  private boolean isOwnPageMoving(long now) {
    return isOwnPage(lastPageUptime, navigation.lastAutoScrollUptime(), now);
  }

  /**
   * Returns whether the latest scroll at {@code now} is a page scrolled ahead at {@code
   * pageUptime} rather than a swipe's scroll at {@code navigationScrollUptime}.
   */
  @VisibleForTesting
  static boolean isOwnPage(long pageUptime, long navigationScrollUptime, long now) {
    return pageUptime > 0 && pageUptime > navigationScrollUptime && now - pageUptime < MAX_WAIT_MS;
  }

  private float navigationDensity() {
    return navigation.density();
  }

  /** Whether a list that sent its last scroll event at {@code lastScrollEvent} has stopped. */
  @VisibleForTesting
  static boolean isQuiet(long now, long lastScrollEvent) {
    return now - lastScrollEvent >= QUIET_MS;
  }
}
