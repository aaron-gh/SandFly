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

  /** How long after focus lands the first scroll waits, so that quick swipes don't scroll. */
  private static final long START_DELAY_MS = 150;

  /** Checks this often whether the list has stopped. */
  private static final long CHECK_MS = 30;

  /** A list counts as stopped once it has sent no scroll event for this long. */
  @VisibleForTesting static final long QUIET_MS = 150;

  /** The longest wait for a list to stop before scrolling ahead gives up. */
  private static final long MAX_WAIT_MS = 1000;

  /** Most pages scrolled ahead of one focused item. */
  @VisibleForTesting static final int MAX_PAGES = 10;

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

  ScrollAhead(FocusProcessorForLogicalNavigation navigation) {
    this.navigation = navigation;
    current = this;
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
        AccessibilityNodeInfoCompat source =
            event.getSource() == null ? null : AccessibilityNodeInfoCompat.wrap(event.getSource());
        scrollAhead.onFocused(source);
      }
      case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED,
          AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
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
    waitStart = SystemClock.uptimeMillis();
    handler.postDelayed(this::step, START_DELAY_MS);
  }

  private void cancel(String reason) {
    if (focused != null && pages > 0) {
      LogUtils.d(TAG, "Stopped scrolling ahead after %d pages: %s.", pages, reason);
    }
    handler.removeCallbacksAndMessages(null);
    focused = null;
  }

  /** Scrolls a page once the list has stopped, until the next item is on screen. */
  private void step() {
    AccessibilityNodeInfoCompat node = focused;
    if (node == null) {
      return;
    }
    long now = SystemClock.uptimeMillis();
    if (!isQuiet(now, FocusProcessorForLogicalNavigation.lastScrollEventUptime())) {
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
    if (!node.refresh()) {
      focused = null;
      return;
    }
    boolean[] nextShown = new boolean[1];
    int minRemainPx =
        Math.round(MIN_REMAIN_DP * navigationDensity());
    @Nullable ScrollAheadPlan plan = navigation.planScrollAhead(node, minRemainPx, nextShown);
    if (plan == null) {
      if (nextShown[0] && pages > 0) {
        LogUtils.d(TAG, "Next item on screen after scrolling ahead %d pages.", pages);
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
    if (!navigation.scrollListAhead(plan.list, plan.scrollAction)) {
      focused = null;
      return;
    }
    pages++;
    LogUtils.d(
        TAG, "Scrolling ahead of the focused item: page %d, %d px of it below.", pages, plan.hiddenPx);
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
