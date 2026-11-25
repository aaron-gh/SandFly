/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_PAGE_DOWN;
import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_PAGE_LEFT;
import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_PAGE_RIGHT;
import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_PAGE_UP;
import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD;
import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN;
import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD;
import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT;
import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT;
import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP;
import static com.google.android.accessibility.talkback.Constants.CLANK_PACKAGE_NAME;
import static com.google.android.accessibility.talkback.flags.FeatureFlagReader.getSuccessAutoScrollPercentageThreshold;
import static com.google.android.accessibility.talkback.focusmanagement.NavigationTarget.TARGET_CONTAINER;
import static com.google.android.accessibility.talkback.focusmanagement.NavigationTarget.TARGET_SEARCH;
import static com.google.android.accessibility.utils.AccessibilityEventUtils.DELTA_UNDEFINED;
import static com.google.android.accessibility.utils.AccessibilityNodeInfoUtils.FILTER_AUTO_SCROLL;
import static com.google.android.accessibility.utils.AccessibilityNodeInfoUtils.FILTER_CONTAINER;
import static com.google.android.accessibility.utils.AccessibilityNodeInfoUtils.FILTER_SCROLLABLE_GRID;
import static com.google.android.accessibility.utils.AccessibilityNodeInfoUtils.FILTER_SHOULD_FOCUS;
import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;
import static com.google.android.accessibility.utils.Role.ROLE_FLOATING_ACTION_BUTTON;
import static com.google.android.accessibility.utils.monitor.InputModeTracker.INPUT_MODE_KEYBOARD;
import static com.google.android.accessibility.utils.output.ScrollActionRecord.AutoScrollSuccessChecker;
import static com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_BACKWARD;
import static com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_DOWN;
import static com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_FORWARD;
import static com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_LEFT;
import static com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_RIGHT;
import static com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_UP;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Trace;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import androidx.annotation.IntDef;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionItemInfoCompat;
import androidx.core.view.accessibility.AccessibilityWindowInfoCompat;
import com.google.android.accessibility.talkback.ActorState;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.actor.FocusManagerInternal;
import com.google.android.accessibility.talkback.actor.search.StringMatcher;
import com.google.android.accessibility.talkback.actor.search.UniversalSearchActor;
import com.google.android.accessibility.talkback.compositor.Compositor.TextComposer;
import com.google.android.accessibility.talkback.compositor.GlobalVariables;
import com.google.android.accessibility.talkback.eventprocessor.ProcessorAccessibilityHints;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.focusmanagement.NavigationTarget.TargetType;
import com.google.android.accessibility.talkback.focusmanagement.action.NavigationAction;
import com.google.android.accessibility.talkback.gesture.SidewaysSwipe;
import com.google.android.accessibility.talkback.screenelements.ScreenElements;
import com.google.android.accessibility.talkback.selector.VerticalNavigation;
import com.google.android.accessibility.talkback.focusmanagement.interpreter.ScreenState;
import com.google.android.accessibility.talkback.focusmanagement.interpreter.ScreenStateMonitor;
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo;
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionRecord;
import com.google.android.accessibility.talkback.utils.LinkUtils;
import com.google.android.accessibility.talkback.utils.TalkbackFeatureSupport;
import com.google.android.accessibility.utils.AccessibilityEventUtils;
import com.google.android.accessibility.utils.AccessibilityNode;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.AccessibilityServiceCompatUtils;
import com.google.android.accessibility.utils.AccessibilityWindowInfoUtils;
import com.google.android.accessibility.utils.BuildVersionUtils;
import com.google.android.accessibility.utils.DisplayUtils;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.Filter;
import com.google.android.accessibility.utils.FocusFinder;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.accessibility.utils.NodeActionFilter;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.Role;
import com.google.android.accessibility.utils.Role.RoleName;
import com.google.android.accessibility.utils.ScrollableNodeInfo;
import com.google.android.accessibility.utils.SettingsUtils;
import com.google.android.accessibility.utils.WebInterfaceUtils;
import com.google.android.accessibility.utils.WindowUtils;
import com.google.android.accessibility.utils.input.CursorGranularity;
import com.google.android.accessibility.utils.input.ScrollEventInterpreter;
import com.google.android.accessibility.utils.input.ScrollEventInterpreter.ScrollTimeout;
import com.google.android.accessibility.utils.monitor.CollectionState;
import com.google.android.accessibility.utils.output.FeedbackItem;
import com.google.android.accessibility.utils.output.ScrollActionRecord;
import com.google.android.accessibility.utils.output.ScrollActionRecord.UserAction;
import com.google.android.accessibility.utils.output.SpeechController;
import com.google.android.accessibility.utils.traversal.GridTraversalManager;
import com.google.android.accessibility.utils.traversal.OrderedTraversalStrategy;
import com.google.android.accessibility.utils.traversal.OrderedTraversalStrategyConfig;
import com.google.android.accessibility.utils.traversal.TraversalStrategy;
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SearchDirection;
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SearchDirectionOrUnknown;
import com.google.android.accessibility.utils.traversal.TraversalStrategyUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Handles the use case of logical navigation actions. */
public class FocusProcessorForLogicalNavigation {
  // The direction of the last move to the next or previous item, which the next move most likely
  // takes too.
  private @SearchDirection int lastLogicalDirection = TraversalStrategy.SEARCH_FOCUS_FORWARD;

  /** When {@link #lastLogicalDirection} was set, in {@link SystemClock#uptimeMillis()}. */
  private long lastLogicalDirectionUptime;

  /** Whether a move to the previous item happened less than {@code withinMs} ago. */
  boolean movedBackwardWithin(long withinMs) {
    return lastLogicalDirection == TraversalStrategy.SEARCH_FOCUS_BACKWARD
        && SystemClock.uptimeMillis() - lastLogicalDirectionUptime < withinMs;
  }


  ///////////////////////////////////////////////////////////////////////////////////////////////
  // Constants

  private static final String TAG = "FocusProcessor-LogicalNav";

  private static final Filter<AccessibilityNodeInfoCompat>
      SCROLLABLE_ROLE_FILTER_FOR_DIRECTION_NAVIGATION = FILTER_AUTO_SCROLL;

  private static final Filter<AccessibilityNodeInfoCompat>
      SCROLLABLE_ROLE_FILTER_FOR_SCROLL_GESTURE =
          AccessibilityNodeInfoUtils.FILTER_SCROLLABLE.and(
              new Filter<AccessibilityNodeInfoCompat>() {
                @Override
                public boolean accept(AccessibilityNodeInfoCompat node) {
                  @RoleName int role = Role.getRole(node);
                  return (role != Role.ROLE_SEEK_CONTROL)
                      && (role != Role.ROLE_DATE_PICKER)
                      && (role != Role.ROLE_TIME_PICKER);
                }
              });

  ///////////////////////////////////////////////////////////////////////////////////////////////
  // Member variables

  /** Filters target window when performing window navigation with keyboard shortcuts. */
  @VisibleForTesting public final Filter<AccessibilityWindowInfo> filterWindowForWindowNavigation;

  private final AccessibilityService service;
  private final FocusFinder focusFinder;
  private ActorState actorState;
  private final ScreenStateMonitor.State screenState;
  private final UniversalSearchActor.State searchState;
  private Pipeline.FeedbackReturner pipeline;
  private final boolean isWindowNavigationSupported;

  // Whether the previous navigation action reaches the edge of the window, reset the value after
  // successfully finding the focus.
  private boolean reachEdge = false;

  /** What auto-scrolling at the edge did in the last search, for {@link #logEdge}. */
  private String edgeScrollNote = "notTried";

  private static boolean wrapAround = true;

  // With wrapping on, whether the first swipe past the last or first item stops there, and only
  // the next one wraps around.
  private static boolean stopBeforeWrap = true;

  // Whether a swipe past the first or last item of the navigation bar or the status bar moves to
  // the next or previous window, instead of around the bar.
  private static boolean leaveSystemBars = false;

  // With any typing method but double-tap, swipes from the keyboard go on in the app from the text
  // field rather than through the keys.
  @FocusProcessorForTapAndTouchExploration.TypingMethod
  private static int typingMethod = FocusProcessorForTapAndTouchExploration.LIFT_TO_TYPE;

  // Whether the current navigation action stopped at the first or last item because wrapping is
  // off, where it would otherwise have wrapped around.
  private boolean stoppedAtEdge = false;

  /** The last node that was scrolled while navigating with native macro granularity. */
  private @Nullable AccessibilityNodeInfoCompat lastScrolledNodeForNativeMacroGranularity;

  /** Callback to handle scroll success or failure. */
  private @Nullable AutoScrollCallback scrollCallback;

  /** Set while searching again after showing an off-screen item, so it is tried only once. */
  private boolean revealingOffscreen;

  // Checks once a frame for about 100 ms, when Android would send the scroll event anyway.
  private static final long EARLY_SCROLL_RETRY_MS = 16;
  private static final int EARLY_SCROLL_RETRIES = 6;
  // A jump lays the list out anew, which can take longer. Giving up too soon on a jump that later
  // takes effect would add a page scroll on top of it, so it gets about 300 ms.
  private static final int EARLY_JUMP_RETRIES = 18;

  /** Most items that previous container steps back over to reach the start of a container. */
  private static final int MAX_CONTAINER_START_STEPS = 500;

  // Checks whether a page scroll has already moved the list, before its scroll event arrives.
  private final Handler earlyScrollCheckHandler = new Handler(Looper.getMainLooper());

  /** The order worked out around the pivot for the navigation in progress, if any. */
  private @Nullable LocalTraversalOrder localOrder;

  /** Whether the navigation in progress is done again with the whole order. */
  private boolean localOrderOff;

  // While a list that a swipe scrolled is still moving, its next item may not be on screen yet.
  // The search is repeated once a frame until the list stops, for at most about 300 ms, rather
  // than navigating again, which would scroll a second page and skip the items in between.
  private static final long SETTLE_CHECK_MS = 16;
  private static final int SETTLE_CHECKS = 20;

  // A focused item that fills its list doesn't seem to move while the list scrolls, so the list's
  // scroll events tell whether it still moves. Android sends them at most every 100 ms while it
  // does. The search waits while the list moves at speed, and starts once a scroll event shows it
  // slowing down: a step less than half the one before. It waits at most MAX_MOVING_WAIT_MS from
  // the scroll, NO_MOVE_WAIT_MS if the list sends no scroll event, and stops waiting when none has
  // come for STOPPED_MS. The checks read nothing from the app.
  private static final long MOVING_CHECK_MS = 30;
  private static final long MAX_MOVING_WAIT_MS = 270;
  private static final long NO_MOVE_WAIT_MS = 180;
  private static final long STOPPED_MS = 150;

  // The scroll events of any window, counted on the main thread: when the last arrived, how many
  // have, how far the last two moved, and their vertical deltas added up.
  private static long lastScrollEventUptime;
  private static int scrollEventCount;
  private static int lastScrollStep;
  private static int previousScrollStep;
  private static int scrollDeltaSumY;

  /** When the last auto scroll for a navigation started, in {@link SystemClock#uptimeMillis()}. */
  private long lastAutoScrollUptime;

  /** The scroll whose list a node is being followed for, which decides when to search. */
  private @Nullable AutoScrollCallback trackedCallback;

  /** Whether the list had sent no scroll event for a while when the last auto scroll started. */
  private boolean scrollStartedQuiet;

  /** Whether following a node has shown that the list of the last auto scroll has slowed down. */
  private boolean listSettled;

  /** {@link #scrollEventCount} and {@link #scrollDeltaSumY} when that scroll started. */
  private int scrollStartEventCount;

  private int scrollStartDeltaSumY;

  /** Whether the list of the last auto scroll is known to scroll a page in one scroll event. */
  private boolean scrollOfOneEventList;

  /** Whether the last auto scroll was a jump to a row, which can leave the pivot in place. */
  private boolean lastScrollWasJump;

  /** When the action of the last auto scroll returned, in {@link SystemClock#uptimeMillis()}. */
  private long scrollActionReturnedUptime;

  /** Where the pivot of the last auto scroll was when that scroll started. */
  private final Rect pivotBoundsAtScrollStart = new Rect();

  /** The list of the last auto scroll, when it is known to scroll a page in one scroll event. */
  private @Nullable AccessibilityNodeInfoCompat oneEventScrollList;

  // While a scroll made for navigation goes on, scroll events of other windows, such as a ticker in
  // the notification shade or another app on a split screen, don't tell whether its list moves.
  private static final long OWN_WINDOW_MS = 2000;
  private static int scrollWindowId = -1;
  private static long scrollWindowSetUptime;

  /** Notes the window of a list that navigation has just scrolled. */
  static void noteScrollWindow(int windowId) {
    scrollWindowId = windowId;
    scrollWindowSetUptime = SystemClock.uptimeMillis();
  }

  /**
   * Whether a scroll event of window {@code eventWindowId}, at {@code now}, is left out while the
   * list of window {@code scrollWindow}, scrolled at {@code scrollWindowSet}, is followed.
   */
  @VisibleForTesting
  static boolean isOtherWindowScroll(
      int eventWindowId, int scrollWindow, long scrollWindowSet, long now) {
    return scrollWindow >= 0
        && eventWindowId >= 0
        && eventWindowId != scrollWindow
        && now - scrollWindowSet < OWN_WINDOW_MS;
  }

  /** Records a scroll event, which tells whether a list scrolled by navigation still moves. */
  public static void onScrollEventReceived(AccessibilityEvent event) {
    if (isOtherWindowScroll(
        event.getWindowId(), scrollWindowId, scrollWindowSetUptime, SystemClock.uptimeMillis())) {
      return;
    }
    lastScrollEventUptime = SystemClock.uptimeMillis();
    scrollEventCount++;
    int deltaX = AccessibilityEventUtils.getScrollDeltaX(event);
    int deltaY = AccessibilityEventUtils.getScrollDeltaY(event);
    int step = 0;
    if (deltaY != DELTA_UNDEFINED && deltaY != 0) {
      step = Math.abs(deltaY);
      scrollDeltaSumY += deltaY;
    } else if (deltaX != DELTA_UNDEFINED) {
      step = Math.abs(deltaX);
    }
    previousScrollStep = lastScrollStep;
    lastScrollStep = step;
  }

  // Counts navigation actions, so that a search waiting for a list to stop gives way to a newer one.
  private int navigationSerial;
  // A consumed scroll callback can still have its focus search posted for the next frame.
  private int pendingScrollSearchSerial = -1;

  /** Scrolls lists ahead of the focused item. */
  private final ScrollAhead scrollAhead;

  /** Whether the item after a list's last one on screen is focused as soon as a scroll shows it. */
  private static volatile boolean speakWhileScrolling = true;

  /** Sets whether the next item is focused as soon as a scroll shows it. */
  public static void setSpeakWhileScrolling(boolean on) {
    speakWhileScrolling = on;
  }

  /** The vertical deltas of all scroll events so far, added up. */
  static int scrollDeltaSumY() {
    return scrollDeltaSumY;
  }

  /** When the last scroll for a navigation started, in {@link SystemClock#uptimeMillis()}. */
  long lastAutoScrollUptime() {
    return lastAutoScrollUptime;
  }

  /** When the last scroll event of any window arrived, in {@link SystemClock#uptimeMillis()}. */
  static long lastScrollEventUptime() {
    return lastScrollEventUptime;
  }

  /** The number of scroll events of any window that have arrived. */
  public static int scrollEventCount() {
    return scrollEventCount;
  }

  /** The screen's density, pixels per dp. */
  float density() {
    return service.getResources().getDisplayMetrics().density;
  }

  /** Whether continuous reading is going on. */
  boolean isReadingContinuously() {
    return actorState != null && actorState.getContinuousRead().isActive();
  }

  /** What scrolling ahead of a focused item scrolls: a list, with a scroll action. */
  static final class ScrollAheadPlan {
    final AccessibilityNodeInfoCompat list;
    final int scrollAction;
    final int pagePx;

    /** Where the focused item was on screen before the page. */
    final Rect itemBounds;

    ScrollAheadPlan(
        AccessibilityNodeInfoCompat list, int scrollAction, int pagePx, Rect itemBounds) {
      this.list = list;
      this.scrollAction = scrollAction;
      this.pagePx = pagePx;
      this.itemBounds = itemBounds;
    }
  }

  /**
   * Returns how to scroll the list of {@code focused} forward so that the next item comes on
   * screen while a part of the focused item stays, or null if it shouldn't scroll. {@code
   * nextShown} gets whether the reason is that the next item is on screen already.
   */
  @Nullable ScrollAheadPlan planScrollAhead(
      AccessibilityNodeInfoCompat focused,
      int minRemainPx,
      boolean forward,
      boolean[] nextShown) {
    nextShown[0] = false;
    @SearchDirection
    int searchDirection =
        forward ? TraversalStrategy.SEARCH_FOCUS_FORWARD : TraversalStrategy.SEARCH_FOCUS_BACKWARD;
    if (!AccessibilityNodeInfoUtils.isVisible(focused)
        || focused.isEditable()
        || Role.getRole(focused) == Role.ROLE_EDIT_TEXT
        || WebInterfaceUtils.supportsWebActions(focused)
        || AccessibilityNodeInfoUtils.isScrollable(focused)) {
      return null;
    }
    ScrollableNodeInfo scrollableNodeInfo =
        ScrollableNodeInfo.findScrollableNodeForDirection(
            searchDirection,
            focused,
            /* includeSelf= */ false,
            WindowUtils.isScreenLayoutRTL(service));
    if (scrollableNodeInfo == null) {
      return null;
    }
    AccessibilityNodeInfoCompat list = scrollableNodeInfo.getNode();
    if (!isItemList(list)) {
      return null;
    }
    AccessibilityNodeInfoCompat item = itemOf(focused, list);
    if (item != null && !item.equals(focused)) {
      // The list may still be moving from an earlier page, so read where the item is now.
      item.refresh();
    }
    Rect itemBounds = new Rect();
    (item != null ? item : focused).getBoundsInScreen(itemBounds);
    // Another item on screen past this one: the next swipe needn't scroll. Checked first, as it
    // reads no more than the list's children.
    if (item != null && hasItemBeyond(list, item, itemBounds, forward)) {
      nextShown[0] = true;
      return null;
    }
    // The next item as a swipe would find it in the list: if it is on screen, a swipe needn't
    // scroll.
    TraversalStrategy localTraversalStrategy =
        TraversalStrategyUtils.getTraversalStrategy(list, focusFinder, searchDirection);
    Filter<AccessibilityNodeInfoCompat> nodeFilter =
        NavigationTarget.createNodeFilter(
            NavigationTarget.TARGET_DEFAULT, localTraversalStrategy.getSpeakingNodesCache());
    AccessibilityNodeInfoCompat next =
        TraversalStrategyUtils.searchFocus(
            localTraversalStrategy, focused, searchDirection, nodeFilter);
    if (next != null && AccessibilityNodeInfoUtils.isVisible(next)) {
      nextShown[0] = true;
      return null;
    }
    Integer direction =
        scrollableNodeInfo.getSupportedScrollDirection(searchDirection);
    if (direction == null) {
      LogUtils.d(
          TAG, "Not scrolling ahead: the list can't scroll %s.", forward ? "forward" : "back");
      return null;
    }
    int scrollAction = TraversalStrategyUtils.convertSearchDirectionToScrollAction(direction);
    if (forward
        ? scrollAction != AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD
            && scrollAction != AccessibilityActionCompat.ACTION_SCROLL_DOWN.getId()
        : scrollAction != AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD
            && scrollAction != AccessibilityActionCompat.ACTION_SCROLL_UP.getId()) {
      return null;
    }
    Rect listBounds = new Rect();
    list.getBoundsInScreen(listBounds);
    int page = learnedPage(list);
    if (page > listBounds.height()) {
      LogUtils.d(TAG, "Forgetting a page of %d px, taller than the list.", page);
      forgetPage(list);
      page = 0;
    }
    if (!staysAfterPage(itemBounds, listBounds, page, minRemainPx, forward)) {
      LogUtils.d(
          TAG,
          "Not scrolling ahead: a page (%s) could scroll the item at %s out of the list at %s.",
          page > 0 ? page + " px" : "size not known",
          itemBounds,
          listBounds);
      return null;
    }
    return new ScrollAheadPlan(list, scrollAction, page, itemBounds);
  }

  /**
   * Returns whether at least {@code minRemainPx} of an item with {@code item} bounds stays in a
   * list with {@code list} bounds after a page scroll {@code forward}, or back: the item's bottom
   * must stay that far below the list's top, or its top that far above the list's bottom. A page of
   * {@code pagePx} measured earlier must also leave that much of the list. Without a measured page,
   * a page is taken to be the list's height and {@code minRemainPx} more: some lists scroll a page
   * slightly taller than they show, and an item that ends exactly at the list's edge leaves the
   * screen with such a page. Items that Android cuts off at the list's edge never pass then.
   */
  @VisibleForTesting
  static boolean staysAfterPage(
      Rect item, Rect list, int pagePx, int minRemainPx, boolean forward) {
    if (item.isEmpty() || list.isEmpty()) {
      return false;
    }
    if (pagePx > 0 && pagePx > list.height() - minRemainPx) {
      return false;
    }
    int page = pagePx > 0 ? pagePx : list.height() + minRemainPx;
    return forward
        ? item.bottom - page >= list.top + minRemainPx
        : item.top + page <= list.bottom - minRemainPx;
  }

  /** Returns the item of {@code list} that holds {@code node}, or null if it isn't near one. */
  private static @Nullable AccessibilityNodeInfoCompat itemOf(
      AccessibilityNodeInfoCompat node, AccessibilityNodeInfoCompat list) {
    AccessibilityNodeInfoCompat item = node;
    AccessibilityNodeInfoCompat parent = node.getParent();
    for (int depth = 0; parent != null && !parent.equals(list); depth++) {
      if (depth >= MAX_ITEM_DEPTH) {
        return null;
      }
      item = parent;
      parent = parent.getParent();
    }
    return parent == null ? null : item;
  }

  /** Scrolls {@code list} ahead of the focused item, as SandFly's own silent scroll. */
  boolean scrollListAhead(AccessibilityNodeInfoCompat list, int scrollAction) {
    countScrollAction();
    noteScrollWindow(list.getWindowId());
    return pipeline.returnFeedback(
        EVENT_ID_UNTRACKED,
        Feedback.scroll(
            list,
            ScrollActionRecord.ACTION_AUTO_SCROLL,
            scrollAction,
            ScrollActionRecord.SCROLL_AHEAD,
            ScrollTimeout.SCROLL_TIMEOUT_SHORT,
            /* autoScrollAttempt= */ 0));
  }

  // The last list whose items showed which way their indexes run, and which way they run.
  private @Nullable AccessibilityNodeInfoCompat listWithKnownOrder;
  private boolean listWithKnownOrderRunsUpward;

  // Target to put focus on on the next window navigation action
  private @Nullable AccessibilityNodeInfoCompat stealWindowNavigationTarget = null;
  private @SearchDirectionOrUnknown int stealWindowNavigationTargetDirection =
      TraversalStrategy.SEARCH_FOCUS_UNKNOWN;

  // Object-wrapper around static-method getAccessibilityFocus(), for test-mocking.
  // Where the first item of the list was at the last check, while the pivot has left the list.
  private @Nullable Rect firstItemBoundsAfterPivotLeft;

  private final AccessibilityFocusMonitor accessibilityFocusMonitor;

  @NonNull private final TextComposer compositor;
  private final GlobalVariables globalVariables;

  ///////////////////////////////////////////////////////////////////////////////////////////////
  // Construction

  public FocusProcessorForLogicalNavigation(
      @NonNull AccessibilityService service,
      FocusFinder focusFinder,
      AccessibilityFocusMonitor accessibilityFocusMonitor,
      ScreenStateMonitor.State screenState,
      UniversalSearchActor.State searchState,
      @NonNull TextComposer compositor,
      GlobalVariables globalVariables) {
    current = this;
    this.service = service;
    this.focusFinder = focusFinder;
    this.accessibilityFocusMonitor = accessibilityFocusMonitor;
    this.screenState = screenState;
    this.searchState = searchState;
    this.compositor = compositor;
    this.globalVariables = globalVariables;
    scrollAhead = new ScrollAhead(this);
    isWindowNavigationSupported = !FormFactorUtils.isAndroidTv();
    filterWindowForWindowNavigation = new WindowNavigationFilter(service, searchState);
  }

  public void setPipeline(Pipeline.FeedbackReturner pipeline) {
    this.pipeline = pipeline;
  }

  public void setActorState(ActorState actorState) {
    this.actorState = actorState;
  }

  public static void setWrapAround(boolean wrap) {
    wrapAround = wrap;
  }

  public static void setStopBeforeWrap(boolean stop) {
    stopBeforeWrap = stop;
  }

  public static void setLeaveSystemBars(boolean leave) {
    leaveSystemBars = leave;
  }

  public static void setTypingMethod(
      @FocusProcessorForTapAndTouchExploration.TypingMethod int method) {
    typingMethod = method;
  }

  /**
   * Whether swipes from {@code fromWindow} can go to the keyboard's window. Swipes from other
   * windows never go into the keyboard; swipes from inside it, such as with double-tap typing,
   * still leave it.
   */
  @VisibleForTesting
  static boolean swipesIncludeKeyboard(@Nullable AccessibilityWindowInfo fromWindow) {
    return AccessibilityWindowInfoUtils.isImeWindow(fromWindow);
  }

  /** Whether swipes from a key go on in the app from the text field, rather than through keys. */
  @VisibleForTesting
  static boolean swipesFromKeysStayInApp(
      @FocusProcessorForTapAndTouchExploration.TypingMethod int method) {
    return method != FocusProcessorForTapAndTouchExploration.DOUBLE_TAP;
  }

  /** Whether a swipe past the last or first item stops there, rather than wrapping around. */
  private static boolean stopsAtEdge() {
    return !wrapAround || stopBeforeWrap;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////
  // Methods
  public boolean onNavigationAction(NavigationAction navigationAction, EventId eventId) {
    if (deferWhileSwipeScrolls(navigationAction, eventId)) {
      return true;
    }
    if (!deferredNavigations.isEmpty()) {
      // Another kind of navigation came first: the swipes waiting for the list would go on from
      // where it leaves focus.
      LogUtils.d(TAG, "Dropping %d waiting swipes for another navigation.", deferredNavigations.size());
      deferredNavigations.clear();
    }
    navigationSerial++;
    return runNavigationAction(navigationAction, eventId);
  }

  private boolean runNavigationAction(NavigationAction navigationAction, EventId eventId) {
    if (navigationAction == null || navigationAction.actionType == NavigationAction.UNKNOWN) {
      LogUtils.w(TAG, "Cannot perform navigation action: action type undefined.");
      return false;
    }
    AccessibilityNodeInfoCompat pivot = getPivotNodeForNavigationAction(navigationAction);
    if (pivot == null) {
      // Ideally the pivot should never be null. Do the null check in case of exception.
      LogUtils.w(TAG, "Cannot find pivot node for %s", navigationAction);
      return false;
    }
    pivot = textFieldForKeyAfterTyping(pivot, navigationAction);
    if (navigationAction.actionType == NavigationAction.DIRECTIONAL_NAVIGATION) {
      ScrollEventInterpreter.onNavigation(pivot.getWindowId());
    }
    stoppedAtEdge = false;
    return switch (navigationAction.actionType) {
      case NavigationAction.DIRECTIONAL_NAVIGATION ->
          onDirectionalNavigationAction(
                  pivot, /* ignoreDescendantsOfPivot= */ false, navigationAction, eventId)
              || repeatAtEdge(navigationAction, eventId);
      case NavigationAction.JUMP_TO_TOP, NavigationAction.JUMP_TO_BOTTOM ->
          onJumpAction(pivot, navigationAction, eventId);
      case NavigationAction.SCROLL_FORWARD, NavigationAction.SCROLL_BACKWARD ->
          onScrollAction(pivot, navigationAction, eventId);
      case NavigationAction.SCROLL_UP,
          NavigationAction.SCROLL_DOWN,
          NavigationAction.SCROLL_LEFT,
          NavigationAction.SCROLL_RIGHT ->
          onScrollOrPageAction(pivot, navigationAction, eventId);
      default -> false;
    };
  }

  // A swipe that comes while an earlier swipe's scroll hasn't yet focused the next item waits for
  // it, and then moves on from that item. Otherwise it would start from the item the scroll moved
  // off screen, or from the window, and skip items. Waits at most MAX_DEFER_MS after the scroll.
  private static final long MAX_DEFER_MS = 1500;
  private static final int MAX_DEFERRED_NAVIGATIONS = 4;
  private final ArrayDeque<Pair<NavigationAction, EventId>> deferredNavigations =
      new ArrayDeque<>();

  /** Queues {@code navigationAction} while a swipe's scroll is pending. Returns whether it did. */
  private boolean deferWhileSwipeScrolls(
      @Nullable NavigationAction navigationAction, EventId eventId) {
    if (navigationAction == null
        || navigationAction.actionType != NavigationAction.DIRECTIONAL_NAVIGATION
        || (deferredNavigations.isEmpty() && !isSwipeScrollPending())) {
      return false;
    }
    if (deferredNavigations.size() >= MAX_DEFERRED_NAVIGATIONS) {
      LogUtils.d(TAG, "Dropping a swipe: %d swipes already wait.", MAX_DEFERRED_NAVIGATIONS);
      return true;
    }
    Pair<NavigationAction, EventId> lastQueued = deferredNavigations.peekLast();
    if (lastQueued != null && goesForward(lastQueued.first) != goesForward(navigationAction)) {
      // A swipe back after a waiting swipe takes that swipe back.
      deferredNavigations.pollLast();
      LogUtils.d(TAG, "A swipe the other way takes back the last waiting swipe.");
      return true;
    }
    LogUtils.d(TAG, "Swipe waits for the list to show the item of the swipe before it.");
    if (deferredNavigations.isEmpty()) {
      firstDeferredUptime = SystemClock.uptimeMillis();
    }
    deferredNavigations.add(Pair.create(navigationAction, eventId));
    if (deferredNavigations.size() == 1) {
      earlyScrollCheckHandler.postDelayed(this::runDeferredNavigations, FRAME_MS);
    }
    return true;
  }

  /** When the first of {@link #deferredNavigations} was queued. */
  private long firstDeferredUptime;

  /**
   * Whether focus was last moved after {@code sinceUptime} by touch exploration or a new screen,
   * rather than by navigation.
   */
  private boolean focusMovedElsewhereSince(long sinceUptime) {
    if (actorState == null || actorState.getFocusHistory() == null) {
      return false;
    }
    FocusActionRecord record = actorState.getFocusHistory().getLastFocusActionRecord();
    if (record == null || record.getActionTime() < sinceUptime) {
      return false;
    }
    int source = record.getExtraInfo().sourceAction;
    return source == FocusActionInfo.TOUCH_EXPLORATION
        || source == FocusActionInfo.SCREEN_STATE_CHANGE;
  }

  /** The instance of the running service, for {@link #swipeScrollPending()}. Main thread. */
  private static @Nullable FocusProcessorForLogicalNavigation current;

  /**
   * Whether a swipe's scroll is still to focus the next item. Restoring lost focus meanwhile would
   * take the swipe over: the scroll's handler then sees focus moved elsewhere and stops.
   */
  public static boolean swipeScrollPending() {
    FocusProcessorForLogicalNavigation processor = current;
    return processor != null && processor.isSwipeScrollPending();
  }

  /** Whether a swipe's scroll is still to focus the next item. */
  private boolean isSwipeScrollPending() {
    AutoScrollCallback callback = scrollCallback;
    return SystemClock.uptimeMillis() - lastAutoScrollUptime < MAX_DEFER_MS
        && ((callback != null
                && callback.sourceAction.actionType == NavigationAction.DIRECTIONAL_NAVIGATION)
            || pendingScrollSearchSerial == navigationSerial);
  }

  private void runDeferredNavigations() {
    if (deferredNavigations.isEmpty()) {
      return;
    }
    if (isSwipeScrollPending()) {
      earlyScrollCheckHandler.postDelayed(this::runDeferredNavigations, FRAME_MS);
      return;
    }
    if (focusMovedElsewhereSince(firstDeferredUptime)) {
      LogUtils.d(
          TAG, "Dropping %d waiting swipes: focus moved by touch or a new screen.",
          deferredNavigations.size());
      deferredNavigations.clear();
      return;
    }
    Pair<NavigationAction, EventId> next = deferredNavigations.poll();
    LogUtils.d(TAG, "Running a swipe that waited for the list.");
    navigationSerial++;
    runNavigationAction(next.first, next.second);
    if (!deferredNavigations.isEmpty()) {
      earlyScrollCheckHandler.postDelayed(this::runDeferredNavigations, FRAME_MS);
    }
  }

  /**
   * With wrapping off, moving to the next item from the last one, or to the previous item from the
   * first, focuses the item again, so that it is spoken again with the end sound in place of the
   * focus sound. Returns whether it did.
   */
  @VisibleForTesting
  boolean repeatAtEdge(NavigationAction navigationAction, @Nullable EventId eventId) {
    if (!stoppedAtEdge
        || wrapAround
        || !navigationAction.shouldWrap
        || !isMoveToNextOrPreviousItem(navigationAction)) {
      return false;
    }
    stoppedAtEdge = false;
    @Nullable AccessibilityNodeInfoCompat focused =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    if (focused == null) {
      return false;
    }
    return setAccessibilityFocusInternal(
        focused, navigationAction, /* linkIndex= */ -1, ColumnWrap.EDGE, eventId);
  }

  @VisibleForTesting
  AccessibilityNodeInfoCompat getStealNextWindowNavigationTarget() {
    return stealWindowNavigationTarget;
  }

  @VisibleForTesting
  @SearchDirectionOrUnknown
  int getStealNextWindowNavigationTargeDirection() {
    return stealWindowNavigationTargetDirection;
  }

  /** Updates the target that should be focused on performing the next window navigation action. */
  public void updateStealNextWindowNavigation(
      @Nullable AccessibilityNodeInfoCompat target, @SearchDirectionOrUnknown int direction) {
    // If the target argument is ever null, reset the tracked node.
    if (target == null) {
      stealWindowNavigationTarget = null;
      stealWindowNavigationTargetDirection = TraversalStrategy.SEARCH_FOCUS_UNKNOWN;
      return;
    }
    if (target.equals(stealWindowNavigationTarget)) {
      return;
    }
    stealWindowNavigationTarget = target;
    stealWindowNavigationTargetDirection = direction;
  }

  /**
   * Moves focus to next node after current focused-node, which matches search-filter. Returns
   * success flag.
   */
  public boolean searchAndFocus(boolean startAtRoot, Filter<AccessibilityNodeInfoCompat> filter) {
    return search(
            startAtRoot,
            /* focus= */ true,
            TraversalStrategy.SEARCH_FOCUS_FORWARD,
            /* beforeFocus= */ null,
            filter)
        != null;
  }

  /**
   * Moves focus to the next node after the focused node, or the previous one when {@code forward}
   * is false, that matches {@code filter}. {@code beforeFocus} runs once a node is found, before it
   * is focused. Returns whether a node was found and focused.
   */
  public boolean searchAndFocus(
      boolean startAtRoot,
      boolean forward,
      @Nullable Runnable beforeFocus,
      Filter<AccessibilityNodeInfoCompat> filter) {
    return search(
            startAtRoot,
            /* focus= */ true,
            forward ? TraversalStrategy.SEARCH_FOCUS_FORWARD : TraversalStrategy.SEARCH_FOCUS_BACKWARD,
            beforeFocus,
            filter)
        != null;
  }

  /**
   * Finds next node which matches search-filter. Optionally focuses matching node. Returns matching
   * node.
   */
  private @Nullable AccessibilityNode search(
      boolean startAtRoot,
      boolean focus,
      @SearchDirection int direction,
      @Nullable Runnable beforeFocus,
      Filter<AccessibilityNodeInfoCompat> filter) {

    AccessibilityNodeInfoCompat start = null;
    AccessibilityNodeInfoCompat rootNode = null;
    // Try to find current accessibility-focused node.
    if (!startAtRoot) {
      start = accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    }
    // Find root node, or return failure.
    if (start == null || !start.refresh()) {
      // Start from root node of active window.
      rootNode = AccessibilityServiceCompatUtils.getRootInActiveWindow(service);
      if (rootNode == null) {
        return null;
      }
      start = rootNode;
    } else {
      // Derive root node from start node.
      rootNode = AccessibilityNodeInfoUtils.getRoot(start);
      if (rootNode == null) {
        return null;
      }
    }

    // Search for node satisfying filter. Swipes move through web content with its own
    // navigation actions, so the traversal order leaves out its children unless asked; a search
    // has to look inside web pages and WebViews too.
    TraversalStrategy traversalStrategy =
        TraversalStrategyUtils.getTraversalStrategy(
            rootNode,
            focusFinder,
            direction,
            /* includeChildrenOfNodesWithWebActions= */ true,
            /* makeFabFirst= */ false);
    // From the root, a backward search starts at the last node.
    AccessibilityNodeInfoCompat target =
        start == rootNode && direction == TraversalStrategy.SEARCH_FOCUS_BACKWARD
            ? TraversalStrategyUtils.findFirstFocusInNodeTree(
                traversalStrategy, rootNode, direction, filter)
            : TraversalStrategyUtils.searchFocus(traversalStrategy, start, direction, filter);
    if (target == null) {
      return null;
    }
    if (beforeFocus != null) {
      beforeFocus.run();
    }

    // Focus first matching node.
    // Focus is implemented in the same function as searching, because they use the same rootNode
    // and traversalStrategy.
    if (focus) {
      EventId eventId = EVENT_ID_UNTRACKED;
      NavigationAction navigationAction =
          new NavigationAction.Builder()
              .setAction(NavigationAction.DIRECTIONAL_NAVIGATION)
              .setDirection(direction)
              .build();
      boolean focused = setAccessibilityFocusInternal(target, navigationAction, eventId);
      if (!focused) {
        return null;
      }
    }
    return AccessibilityNode.takeOwnership(target);
  }

  /**
   * Returns the pivot node for the given navigation action.
   *
   * <p>Pivot node is the {@link AccessibilityNodeInfoCompat} based on which we search for target
   * focusable node. We apply the following strategy to find pivot node:
   *
   * <ol>
   *   <li>Use the accessibility focused node if it's non-null and refreshable.
   *   <li>Otherwise use input focused node if {@link NavigationAction#useInputFocusAsPivotIfEmpty}
   *       is set to {@code true} and it's refreshable.
   *   <li>Otherwise return the root node of active window.
   * </ol>
   */
  private AccessibilityNodeInfoCompat getPivotNodeForNavigationAction(
      NavigationAction navigationAction) {
    // While the saved reading order holds the focused node, Android's cached copy of the node is
    // current: nothing that could change the window has happened since the order was read from the
    // app. So the focus monitor need not ask the app for the node again then.
    AccessibilityNodeInfoCompat pivot =
        accessibilityFocusMonitor.getAccessibilityFocus(
            /* useInputFocusIfEmpty= */ false,
            /* requireEditable= */ false,
            /* isCurrent= */ Filter.node(TraversalTreeCache::holds));
    if (pivot != null
        && pivot.isScrollable()
        && isMoveToNextOrPreviousItem(navigationAction)
        && focusFellToListAfterNavigation(pivot)) {
      // The list itself has focus because the item focused in it went away: go on from the item's
      // place rather than from the list, which a swipe leaves at once.
      AccessibilityNodeInfoCompat recovered = recoverLostListPivot(navigationAction);
      if (recovered != null) {
        pivot = recovered;
      }
    }
    if (pivot == null && navigationAction.useInputFocusAsPivotIfEmpty) {
      // Focus lost in a list that was just navigated, such as an item drawn again or scrolled away,
      // goes on from its place: a text field with input focus, such as a chat's message field,
      // would otherwise take over and every item between would be skipped.
      pivot =
          isMoveToNextOrPreviousItem(navigationAction)
              ? recoverLostListPivot(navigationAction)
              : null;
      if (pivot == null) {
        pivot =
            accessibilityFocusMonitor.getAccessibilityFocus(
                /* useInputFocusIfEmpty= */ true,
                /* requireEditable= */ false,
                /* isCurrent= */ Filter.node(TraversalTreeCache::holds));
      }
    }

    // The focus monitor can return Android's cached copy of a node that was just removed, so check
    // with the app that the pivot is still there. Skip that round trip when the saved reading order
    // holds the pivot: nothing that could remove a node has happened since it was saved. The focus
    // monitor has already checked an accessibility-focused node, so only the input focus it falls
    // back to needs the check.
    if (pivot != null
        && !pivot.isAccessibilityFocused()
        && !TraversalTreeCache.holds(pivot)
        && !pivot.refresh()) {
      pivot = null;
    }

    AccessibilityNodeInfoCompat rootForMissingPivot =
        pivot == null ? AccessibilityServiceCompatUtils.getRootInActiveWindow(service) : null;
    // A scroll focus can have succeeded while Android's cached focus is missing or still on the
    // old node. Check our last successful native move only when it disagrees with that cache.
    if (isMoveToNextOrPreviousItem(navigationAction)
        && lastAutoScrollUptime != 0
        && SystemClock.uptimeMillis() - lastAutoScrollUptime < MAX_DEFER_MS
        && (pivot == null || !WebInterfaceUtils.supportsWebActions(pivot))
        && actorState != null
        && actorState.getFocusHistory() != null) {
      FocusActionRecord record = actorState.getFocusHistory().getLastFocusActionRecord();
      if (record != null && record.getExtraInfo().sourceAction == FocusActionInfo.LOGICAL_NAVIGATION) {
        AccessibilityNodeInfoCompat latest = record.getFocusedNode();
        AccessibilityNodeInfoCompat windowNode = pivot == null ? rootForMissingPivot : pivot;
        if (!latest.equals(pivot)
            && windowNode != null
            && latest.getWindowId() == windowNode.getWindowId()
            && !WebInterfaceUtils.supportsWebActions(latest)
            && latest.refresh()
            && latest.isAccessibilityFocused()
            && AccessibilityNodeInfoUtils.isVisible(latest)) {
          LogUtils.d(TAG, "Continuing from the native focus set after the scroll.");
          pivot = latest;
        }
      }
    }

    if (pivot != null && !pivot.isAccessibilityFocused()
        && isMoveToNextOrPreviousItem(navigationAction)
        && Role.getRole(pivot) == Role.ROLE_WEB_VIEW
        && actorState != null && actorState.getFocusHistory() != null) {
      FocusActionRecord record = actorState.getFocusHistory().getLastFocusActionRecord();
      if (record != null && record.getExtraInfo().sourceAction == FocusActionInfo.LOGICAL_NAVIGATION) {
        AccessibilityNodeInfoCompat surviving = record.getSurvivingWebPivot(pivot);
        if (surviving != null) {
          LogUtils.d(TAG, "Continue in the same web document from surviving pivot %s", surviving);
          pivot = surviving;
        }
      }
    }

    // If we cannot find a pivot, or the pivot is not accessible, choose the root node of the active
    // window.
    if (pivot == null) {
      // TODO: We might need to define our own "active window" in TalkBack side.
      AccessibilityNodeInfoCompat root = rootForMissingPivot;
      // The focused node was removed from the window — an app re-laying-out its content, such as a
      // blocked ad slot collapsing, does this. Starting from the window root would send the user
      // back to the top of the page, so carry on from the node nearest where the last navigation
      // started instead.
      AccessibilityNodeInfoCompat nearest =
          (root == null)
              ? null
              : TraversalTreeCache.pivotAfterRemoval(
                  root, goesForward(navigationAction), lastFocusedNode());
      if (nearest != null && nearest.refresh()) {
        pivot = nearest;
      } else {
        pivot = root;
      }
      if (LogUtils.shouldLog(Log.INFO)) {
        Rect bounds = new Rect();
        if (pivot != null) {
          pivot.getBoundsInScreen(bounds);
        }
        LogUtils.i(
            TAG,
            "No focus to move from; starting from %s at %s, %s.",
            pivot == root ? "the window" : "the node near the last pivot",
            bounds,
            rowDescription(pivot));
      }
    }
    return pivot;
  }

  /**
   * Whether {@code list}, which has focus, holds the node that the last navigation focused shortly
   * before, rather than having been focused itself.
   */
  private boolean focusFellToListAfterNavigation(AccessibilityNodeInfoCompat list) {
    if (actorState == null || actorState.getFocusHistory() == null) {
      return false;
    }
    FocusActionRecord record = actorState.getFocusHistory().getLastFocusActionRecord();
    if (record == null
        || record.getExtraInfo().sourceAction != FocusActionInfo.LOGICAL_NAVIGATION
        || SystemClock.uptimeMillis() - record.getActionTime() >= LOST_PIVOT_MS) {
      return false;
    }
    AccessibilityNodeInfoCompat last = record.getFocusedNode();
    return !last.equals(list) && AccessibilityNodeInfoUtils.hasAncestor(last, list);
  }

  /** How recent the last navigation in a window must be for its place to stand in for lost focus. */
  private static final long LOST_PIVOT_MS = 10_000;

  /**
   * Returns the node to go on from when focus was lost in the active window just after navigation
   * there, from the place the last navigation started, or null.
   */
  /** Whether {@code navigationAction} moves forward in reading order. */
  private boolean goesForward(NavigationAction navigationAction) {
    boolean isRtl = WindowUtils.isScreenLayoutRTL(service);
    if (navigationAction.searchDirection != TraversalStrategy.SEARCH_FOCUS_UNKNOWN) {
      return TraversalStrategyUtils.getLogicalDirection(navigationAction.searchDirection, isRtl)
          != SEARCH_FOCUS_BACKWARD;
    }
    // Scroll and jump actions carry no search direction.
    return switch (navigationAction.actionType) {
      case NavigationAction.SCROLL_BACKWARD,
          NavigationAction.SCROLL_UP,
          NavigationAction.JUMP_TO_BOTTOM -> false;
      case NavigationAction.SCROLL_LEFT -> isRtl;
      case NavigationAction.SCROLL_RIGHT -> !isRtl;
      default -> true;
    };
  }

  /** The node focus was last put on, from the focus history, or null. */
  private @Nullable AccessibilityNodeInfoCompat lastFocusedNode() {
    if (actorState == null || actorState.getFocusHistory() == null) {
      return null;
    }
    FocusActionRecord record = actorState.getFocusHistory().getLastFocusActionRecord();
    return record == null ? null : record.getFocusedNode();
  }

  private @Nullable AccessibilityNodeInfoCompat recoverLostListPivot(
      NavigationAction navigationAction) {
    AccessibilityNodeInfoCompat root = AccessibilityServiceCompatUtils.getRootInActiveWindow(service);
    if (root == null) {
      return null;
    }
    int windowId = root.getWindowId();
    boolean recent = TraversalTreeCache.continuesAfterScrolledAway(windowId);
    if (!recent && actorState != null && actorState.getFocusHistory() != null) {
      FocusActionRecord record = actorState.getFocusHistory().getLastFocusActionRecord();
      recent =
          record != null
              && record.getExtraInfo().sourceAction == FocusActionInfo.LOGICAL_NAVIGATION
              && record.getFocusedNode().getWindowId() == windowId
              && SystemClock.uptimeMillis() - record.getActionTime() < LOST_PIVOT_MS;
    }
    if (!recent) {
      return null;
    }
    AccessibilityNodeInfoCompat nearest =
        TraversalTreeCache.pivotAfterRemoval(
            root, goesForward(navigationAction), lastFocusedNode());
    if (nearest == null || !nearest.refresh()) {
      return null;
    }
    LogUtils.i(TAG, "No focus to move from; going on from the place of the last navigation.");
    return nearest;
  }

  /**
   * Navigates to a web or native table. Throws exception if the target type is not related to
   * table.
   */
  private boolean navigateToTableTarget(
      @NonNull AccessibilityNodeInfoCompat pivot,
      @NonNull NavigationAction navigationAction,
      @Nullable EventId eventId) {
    if (WebInterfaceUtils.supportsWebActions(pivot)
        && NavigationTarget.supportsTableNavigation(navigationAction.targetType)) {
      return pipeline.returnFeedback(eventId, Feedback.webDirectionHtml(pivot, navigationAction));
    } else if (TableNavigation.isNativeTableGranularity(navigationAction.targetType)) {
      return navigateToNativeTable(pivot, navigationAction, eventId);
    } else {
      throw new IllegalArgumentException(
          "Not supported table navigation targetType: " + navigationAction.targetType);
    }
  }

  /** Navigates to a native table. Show error messages if navigation fails. */
  private boolean navigateToNativeTable(
      @NonNull AccessibilityNodeInfoCompat pivot,
      @NonNull NavigationAction navigationAction,
      @Nullable EventId eventId) {
    if (SheetsGrid.isMoving() && SheetsGrid.isInSheets(pivot)) {
      // A quick swipe while a Sheets move waits or the grid slides over: Sheets may have moved its
      // focus off the cells meanwhile, so don't call that the table's end; the move focuses the
      // cell it finds.
      return true;
    }
    if (SheetsGrid.touchGridIfLost(pivot)) {
      // Google Sheets lists no cells, and the focus is on the grid's range label: a touch on
      // the grid brings the cells back, and SandFly speaks the one touched.
      return true;
    }
    if (SheetsGrid.isCell(pivot)) {
      // Google Sheets: its grid sets no table information, see SheetsGrid.
      AccessibilityNodeInfoCompat cell =
          SheetsGrid.navigate(
              pivot,
              navigationAction.targetType,
              navigationAction.searchDirection,
              region ->
                  pipeline.returnFeedback(
                      com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED,
                      Feedback.passThroughMode(
                          Feedback.PassThroughMode.Action.LOCK_PASS_THROUGH, region)),
              found -> {
                if (found == null
                    || !setAccessibilityFocusInternal(found, navigationAction, eventId)) {
                  showFailureMessageForTableNavigationAction(navigationAction, eventId);
                }
              });
      if (cell != null && setAccessibilityFocusInternal(cell, navigationAction, eventId)) {
        return true;
      }
      if (cell == null && SheetsGrid.isMoving()) {
        // The move waits for Sheets or scrolls the grid; the cell is focused once it's found.
        return true;
      }
      showFailureMessageForTableNavigationAction(navigationAction, eventId);
      return false;
    }
    AccessibilityNodeInfoCompat tableRoot = AccessibilityNodeInfoUtils.getTableRoot(pivot);
    if (tableRoot == null) {
      LogUtils.w(TAG, "Cannot perform table navigation: invalid tableRoot.");
      showFailureMessageForInvalidTable(eventId);
      return false;
    }
    AccessibilityNodeInfoCompat pivotCell =
        AccessibilityNodeInfoUtils.getTableCellUnderTable(pivot);
    if (pivotCell == null) {
      LogUtils.w(TAG, "Cannot perform table navigation: invalid pivotCell.");
      showFailureMessageForInvalidTable(eventId);
      return false;
    }
    NavigationAction tableNavigationAction =
        NavigationAction.Builder.copy(navigationAction)
            .setPivotIndex(
                Pair.create(
                    pivotCell.getCollectionItemInfo().getRowIndex(),
                    pivotCell.getCollectionItemInfo().getColumnIndex()))
            .build();
    TableNavigation tableNavigation =
        new TableNavigation(tableNavigationAction, tableRoot, focusFinder, eventId);
    boolean result = navigateToTableCell(tableNavigation, pivot);
    if (!result) {
      showFailureMessageForTableNavigationAction(navigationAction, eventId);
    }
    return result;
  }

  /**
   * Navigates to a table cell. Performs auto scroll if the cell is out of the screen.
   *
   * @param tableNavigation The table navigation action.
   * @param pivot The pivot node.
   * @return {@code true} if the cell is successfully navigated to or scrolled to.
   */
  private boolean navigateToTableCell(
      TableNavigation tableNavigation, AccessibilityNodeInfoCompat pivot) {
    boolean result = false;
    AccessibilityNodeInfoCompat focusableNode = tableNavigation.navigateToTableCell();
    if (focusableNode != null) {
      result =
          setAccessibilityFocusInternal(
              focusableNode, tableNavigation.getNavigationAction(), tableNavigation.getEventId());
    }
    // Try to auto scroll to find the cell out of the screen. It scrolls by {@link
    // AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD} or {@link
    // AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD}
    // instead of {@link AccessibilityNodeInfoCompat.ACTION_SCROLL_TO_POSITION} because scroll to
    // position is unreliable in native GridView.
    int scrollAction =
        TraversalStrategyUtils.convertSearchDirectionToScrollAction(
            tableNavigation.getScrollDirection());
    if (!result && tableNavigation.canScroll(scrollAction)) {
      NavigationAction actionForScroll =
          NavigationAction.Builder.copy(tableNavigation.getNavigationAction())
              // Sets to false to avoid scrolling again, except for the far end of a table, which
              // can be more than one scroll away.
              .setShouldScroll(
                  tableNavigation.movesToBoundary()
                      && shouldKeepSearch(tableNavigation.getNavigationAction()))
              .setAutoScrollAttempt(tableNavigation.getNavigationAction().autoScrollAttempt + 1)
              .build();
      AutoScrollSuccessChecker autoScrollChecker =
          createAutoScrollCheckerIfNeeded(
              tableNavigation.getTableRoot(), actionForScroll, scrollAction);
      result =
          performScrollActionInternal(
              ScrollActionRecord.ACTION_AUTO_SCROLL,
              tableNavigation.getTableRoot(),
              pivot,
              scrollAction,
              actionForScroll,
              ScrollTimeout.SCROLL_TIMEOUT_LONG,
              autoScrollChecker,
              tableNavigation.getEventId());
    }
    return result;
  }

  private void handleViewScrolledForTableNavigationAction(
      TableNavigation tableNavigation, AccessibilityNodeInfoCompat pivot) {
    if (navigateToTableCell(tableNavigation, pivot)) {
      return;
    }
    handleViewAutoScrollFailedForTableNavigationAction(tableNavigation);
  }

  private void handleViewAutoScrollFailedForTableNavigationAction(TableNavigation tableNavigation) {
    showFailureMessageForTableNavigationAction(
        tableNavigation.getNavigationAction(), tableNavigation.getEventId());
    focusInitialTable(tableNavigation);
  }

  private void showFailureMessageForTableNavigationAction(
      NavigationAction sourceAction, EventId eventId) {
    announceNativeElement(sourceAction.searchDirection, sourceAction.targetType, eventId);
  }

  private void showFailureMessageForInvalidTable(EventId eventId) {
    announce(service.getString(R.string.table_navigation_not_supported), eventId);
  }

  /**
   * Focus on the first focusable node in the table.
   *
   * <p>This is a fallback method when the navigation action fails.
   */
  private void focusInitialTable(TableNavigation tableNavigation) {
    AccessibilityNodeInfoCompat curFocusNode =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    if (AccessibilityNodeInfoUtils.isVisible(curFocusNode)) {
      return;
    }
    AccessibilityNodeInfoCompat nodeToFocus =
        tableNavigation.getFirstFocusableByDirection(
            tableNavigation.getTableRoot(), TraversalStrategy.SEARCH_FOCUS_FORWARD);
    setAccessibilityFocusInternal(
        nodeToFocus, tableNavigation.getNavigationAction(), tableNavigation.getEventId());
  }

  /**
   * Handles {@link NavigationAction#DIRECTIONAL_NAVIGATION} actions.
   *
   * @return {@code true} if any accessibility action is successfully performed.
   */
  @VisibleForTesting
  boolean onDirectionalNavigationAction(
      @NonNull AccessibilityNodeInfoCompat pivot,
      boolean ignoreDescendantsOfPivot,
      @NonNull NavigationAction navigationAction,
      @Nullable EventId eventId) {
    if (navigationAction.targetType == NavigationTarget.TARGET_WINDOW) {
      return navigateToWindowTarget(pivot, navigationAction, eventId);
    } else if (isWindowNavigationSupported
        && navigationAction.targetType != NavigationTarget.TARGET_SEARCH
        && TraversalStrategyUtils.isLogicalDirection(navigationAction.searchDirection)
        && pictureInPictureOf(pivot) != null) {
      return leavePictureInPicture(pivot, navigationAction, eventId);
    } else if (NavigationTarget.supportsTableNavigation(navigationAction.targetType)) {
      return navigateToTableTarget(pivot, navigationAction, eventId);
    } else if (NavigationTarget.isWebOnlyTarget(navigationAction.targetType)
        && WebInterfaceUtils.supportsWebActions(pivot)) {
      return navigateToHtmlTarget(pivot, navigationAction, eventId);
    } else if (navigationAction.targetType == NavigationTarget.TARGET_SEARCH) {
      return navigateToScreenSearchTarget(pivot, navigationAction, eventId);
    } else if (navigationAction.targetType == NavigationTarget.TARGET_HTML_ELEMENT_VISITED_LINK) {
      // Apps have no visited links, so scrolling could not bring one on screen.
      return navigateToDefaultOrMacroGranularityTarget(
          pivot,
          ignoreDescendantsOfPivot,
          NavigationAction.Builder.copy(navigationAction).setShouldScroll(false).build(),
          eventId);
    } else {
      return navigateToDefaultOrMacroGranularityTarget(
          pivot, ignoreDescendantsOfPivot, navigationAction, eventId);
    }
  }

  /**
   * Handles {@link NavigationAction#JUMP_TO_TOP} and {@link NavigationAction#JUMP_TO_BOTTOM}
   * actions.
   *
   * @return {@code true} if any accessibility action is successfully performed.
   */
  private boolean onJumpAction(
      AccessibilityNodeInfoCompat pivot, NavigationAction navigationAction, EventId eventId) {
    boolean isNodeFromClank =
        FeatureFlagReader.enableNewJumpNavigationForClank(service)
            && pivot != null
            && pivot.getPackageName() != null
            && pivot.getPackageName().toString().equals(CLANK_PACKAGE_NAME);

    AccessibilityNodeInfoCompat rootNode;
    if (isNodeFromClank) {
      // Finds the first child of the Clank root node that is an ancestor of the pivot node.
      // Restricts the potential jump targets to the same Clank UI section as the pivot node.
      rootNode = AccessibilityNodeInfoUtils.getNthAncestorFromRoot(pivot, 1);
    } else {
      rootNode = AccessibilityNodeInfoUtils.getRoot(pivot);
    }

    if (rootNode == null) {
      LogUtils.w(TAG, "Cannot perform jump action: unable to find root node.");
      return false;
    }

    @SearchDirection
    int searchDirection =
        navigationAction.actionType == NavigationAction.JUMP_TO_TOP
            ? TraversalStrategy.SEARCH_FOCUS_FORWARD
            : TraversalStrategy.SEARCH_FOCUS_BACKWARD;

    TraversalStrategy traversalStrategy =
        TraversalStrategyUtils.getTraversalStrategy(rootNode, focusFinder, searchDirection);

    // Always use default granularity when jumping to the beginning/end of the window.
    AccessibilityNodeInfoCompat target =
        TraversalStrategyUtils.findFirstFocusInNodeTree(
            traversalStrategy,
            rootNode,
            searchDirection,
            NavigationTarget.createNodeFilter(
                NavigationTarget.TARGET_DEFAULT, traversalStrategy.getSpeakingNodesCache()));
    if (target != null) {
      // Navigate to the next focusable node if this node is Clank's WebView.
      if (isNodeFromClank && Role.getRole(target) == Role.ROLE_WEB_VIEW) {
        NavigationAction nextNavigationAction =
            new NavigationAction.Builder()
                .setAction(NavigationAction.JUMP_TO_BOTTOM)
                .setDirection(searchDirection)
                .setTarget(NavigationTarget.TARGET_DEFAULT)
                .setInputMode(INPUT_MODE_KEYBOARD)
                .setOriginalNavigationGranularity(CursorGranularity.DEFAULT)
                .setShouldWrap(true)
                .setShouldScroll(true)
                .build();

        return onDirectionalNavigationAction(
            target, /* ignoreDescendantsOfPivot= */ false, nextNavigationAction, eventId);
      }

      // JUMP_TO_TOP makes the focus on the top, so the top-half of the target node may be covered
      // if it's inside a scrollable container. In this case the checking direction should be
      // SEARCH_FOCUS_BACKWARD(and vice versa).
      @SearchDirection
      int ensureOnScreenDirection =
          navigationAction.actionType == NavigationAction.JUMP_TO_TOP
              ? TraversalStrategy.SEARCH_FOCUS_BACKWARD
              : TraversalStrategy.SEARCH_FOCUS_FORWARD;
      ensureOnScreen(target, ensureOnScreenDirection, eventId);
      return setAccessibilityFocusInternal(target, navigationAction, eventId);
    }
    return false;
  }

  /**
   * Handles {@link NavigationAction#SCROLL_FORWARD} and {@link NavigationAction#SCROLL_BACKWARD}
   * actions.
   *
   * @return {@code true} if any accessibility action is successfully performed.
   */
  private boolean onScrollAction(
      @NonNull AccessibilityNodeInfoCompat pivot,
      NavigationAction navigationAction,
      EventId eventId) {
    AccessibilityNodeInfoCompat scrollableNode = null;
    AccessibilityNodeInfoCompat rootNode = null;
    final int scrollAction;
    if (navigationAction.actionType == NavigationAction.SCROLL_FORWARD) {
      scrollAction = AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD;
    } else if (navigationAction.actionType == NavigationAction.SCROLL_BACKWARD) {
      scrollAction = AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD;
    } else {
      throw new IllegalArgumentException("Unknown scroll action.");
    }

    final Filter<AccessibilityNodeInfoCompat> nodeFilter = getScrollFilter(navigationAction);
    if (nodeFilter == null) {
      return false;
    }

    if (AccessibilityNodeInfoUtils.supportsAction(pivot, scrollAction)) {
      // Try to scroll the node itself first. It's useful when focusing on a SeekBar.
      scrollableNode = pivot;
    } else if (pivot.isAccessibilityFocused()) {
      scrollableNode = AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(pivot, nodeFilter);
    }

    if (scrollableNode == null) {
      rootNode = AccessibilityNodeInfoUtils.getRoot(pivot);
      if (rootNode != null) {
        scrollableNode = AccessibilityNodeInfoUtils.searchFromBfs(rootNode, nodeFilter);
      }
    }
    return (scrollableNode != null)
        && performScrollActionInternal(
            ScrollActionRecord.ACTION_SCROLL_COMMAND,
            scrollableNode,
            pivot,
            scrollAction,
            navigationAction,
            eventId);
  }

  /**
   * Handles {@link NavigationAction#SCROLL_UP}, {@link NavigationAction#SCROLL_DOWN}, {@link
   * NavigationAction#SCROLL_LEFT} and {@link NavigationAction#SCROLL_RIGHT} actions.
   *
   * <p>Checks if the pivot node or its ancestors by return value of
   * {@link getScrollOrPageActionFilter), handles pivot node's supported action accordingly.
   *
   * @return {@code true} if any accessibility action is successfully performed.
   */
  private boolean onScrollOrPageAction(
      AccessibilityNodeInfoCompat pivot, NavigationAction navigationAction, EventId eventId) {
    AccessibilityNodeInfoCompat pageOrScrollNode =
        AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(
            pivot, getScrollOrPageActionFilter(navigationAction));
    if (pageOrScrollNode != null) {
      if (Role.getRole(pageOrScrollNode) == Role.ROLE_PAGER) {
        // Scroll the pager node only if the swiping direction is left or right. For swiping up or
        // down, we only scroll the pager node if it's the only scrollable node on the screen. In
        // that case the node will be found via BFS traversal.
        if (navigationAction.actionType == NavigationAction.SCROLL_LEFT
            || navigationAction.actionType == NavigationAction.SCROLL_RIGHT) {
          return performPageOrScrollAction(navigationAction, pageOrScrollNode, eventId);
        }
      } else {
        return performPageOrScrollAction(navigationAction, pageOrScrollNode, eventId);
      }
    }

    AccessibilityNodeInfoCompat rootNode = AccessibilityNodeInfoUtils.getRoot(pivot);
    AccessibilityNodeInfoCompat nodeFromBfs = null;

    if (navigationAction.actionType == NavigationAction.SCROLL_UP
        || navigationAction.actionType == NavigationAction.SCROLL_DOWN) {
      nodeFromBfs = searchScrollableNodeFromBfs(rootNode, navigationAction, true);
    } else if (navigationAction.actionType == NavigationAction.SCROLL_LEFT
        || navigationAction.actionType == NavigationAction.SCROLL_RIGHT) {
      nodeFromBfs = searchScrollableNodeFromBfs(rootNode, navigationAction, false);
    }

    if (nodeFromBfs != null) {
      return performPageOrScrollAction(navigationAction, nodeFromBfs, eventId);
    }

    return swipeSideways(pivot, navigationAction);
  }

  /**
   * Swipes over the pager around {@code pivot} for Scroll left or Scroll right where no node takes
   * a scroll or page action, for pagers that move only for touches. Returns whether it did.
   */
  private boolean swipeSideways(
      AccessibilityNodeInfoCompat pivot, NavigationAction navigationAction) {
    if (navigationAction.actionType != NavigationAction.SCROLL_LEFT
        && navigationAction.actionType != NavigationAction.SCROLL_RIGHT) {
      return false;
    }
    DisplayMetrics metrics = service.getResources().getDisplayMetrics();
    float[] line =
        sidewaysSwipeLine(
            pivot, navigationAction, new Rect(0, 0, metrics.widthPixels, metrics.heightPixels));
    if (line == null || !SidewaysSwipe.send(service, line)) {
      return false;
    }
    LogUtils.d(TAG, "No sideways scroller for %s, swiping the pager.", navigationAction);
    return true;
  }

  /**
   * The swipe that Scroll left or Scroll right sends where no node takes a scroll or page action
   * for it: over the closest pager around {@code pivot}, as {@link SidewaysSwipe#swipeLine} gives
   * it. Null for other actions, where a node takes the action, or where no pager holds {@code
   * pivot}.
   */
  @VisibleForTesting
  static float @Nullable [] sidewaysSwipeLine(
      AccessibilityNodeInfoCompat pivot, NavigationAction navigationAction, Rect screen) {
    if (navigationAction.actionType != NavigationAction.SCROLL_LEFT
        && navigationAction.actionType != NavigationAction.SCROLL_RIGHT) {
      return null;
    }
    Filter<AccessibilityNodeInfoCompat> filter = getScrollOrPageActionFilter(navigationAction);
    if (filter == null
        || AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(pivot, filter) != null) {
      return null;
    }
    AccessibilityNodeInfoCompat root = AccessibilityNodeInfoUtils.getRoot(pivot);
    if (root != null && AccessibilityNodeInfoUtils.searchFromBfs(root, filter) != null) {
      return null;
    }
    AccessibilityNodeInfoCompat pager = SidewaysSwipe.pagerHolding(pivot);
    if (pager == null) {
      return null;
    }
    Rect bounds = new Rect();
    pager.getBoundsInScreen(bounds);
    return SidewaysSwipe.swipeLine(
        bounds, screen, navigationAction.actionType == NavigationAction.SCROLL_LEFT);
  }

  private boolean performPageOrScrollAction(
      NavigationAction navigationAction,
      @Nullable AccessibilityNodeInfoCompat scrollableNode,
      EventId eventId) {
    if (scrollableNode == null) {
      return false;
    }
    // SCROLL_UP/SCROLL_LEFT action means the content should move "UP/LEFT" for this action and
    // rest actions follow the same rule
    switch (navigationAction.actionType) {
      case NavigationAction.SCROLL_UP -> {
        if (AccessibilityNodeInfoUtils.supportsAction(scrollableNode, ACTION_PAGE_DOWN.getId())) {
          return pipeline.returnFeedback(
              eventId, Feedback.nodeAction(scrollableNode, ACTION_PAGE_DOWN.getId()));
        } else if (AccessibilityNodeInfoUtils.supportsAction(
            scrollableNode, ACTION_SCROLL_DOWN.getId())) {
          return pipeline.returnFeedback(
              eventId, Feedback.nodeAction(scrollableNode, ACTION_SCROLL_DOWN.getId()));
        } else if (AccessibilityNodeInfoUtils.supportsAction(
            scrollableNode, ACTION_SCROLL_FORWARD.getId())) {
          return pipeline.returnFeedback(
              eventId, Feedback.nodeAction(scrollableNode, ACTION_SCROLL_FORWARD.getId()));
        }
      }
      case NavigationAction.SCROLL_DOWN -> {
        if (AccessibilityNodeInfoUtils.supportsAction(scrollableNode, ACTION_PAGE_UP.getId())) {
          return pipeline.returnFeedback(
              eventId, Feedback.nodeAction(scrollableNode, ACTION_PAGE_UP.getId()));
        } else if (AccessibilityNodeInfoUtils.supportsAction(
            scrollableNode, ACTION_SCROLL_UP.getId())) {
          return pipeline.returnFeedback(
              eventId, Feedback.nodeAction(scrollableNode, ACTION_SCROLL_UP.getId()));
        } else if (AccessibilityNodeInfoUtils.supportsAction(
            scrollableNode, ACTION_SCROLL_BACKWARD.getId())) {
          return pipeline.returnFeedback(
              eventId, Feedback.nodeAction(scrollableNode, ACTION_SCROLL_BACKWARD.getId()));
        }
      }
      case NavigationAction.SCROLL_LEFT -> {
        if (AccessibilityNodeInfoUtils.supportsAction(scrollableNode, ACTION_PAGE_RIGHT.getId())) {
          return pipeline.returnFeedback(
              eventId, Feedback.nodeAction(scrollableNode, ACTION_PAGE_RIGHT.getId()));
        } else if (AccessibilityNodeInfoUtils.supportsAction(
            scrollableNode, ACTION_SCROLL_RIGHT.getId())) {
          return pipeline.returnFeedback(
              eventId, Feedback.nodeAction(scrollableNode, ACTION_SCROLL_RIGHT.getId()));
        } else if (AccessibilityNodeInfoUtils.supportsAction(
            scrollableNode, ACTION_SCROLL_FORWARD.getId())) {
          return pipeline.returnFeedback(
              eventId, Feedback.nodeAction(scrollableNode, ACTION_SCROLL_FORWARD.getId()));
        }
      }
      case NavigationAction.SCROLL_RIGHT -> {
        if (AccessibilityNodeInfoUtils.supportsAction(scrollableNode, ACTION_PAGE_LEFT.getId())) {
          return pipeline.returnFeedback(
              eventId, Feedback.nodeAction(scrollableNode, ACTION_PAGE_LEFT.getId()));
        } else if (AccessibilityNodeInfoUtils.supportsAction(
            scrollableNode, ACTION_SCROLL_LEFT.getId())) {
          return pipeline.returnFeedback(
              eventId, Feedback.nodeAction(scrollableNode, ACTION_SCROLL_LEFT.getId()));
        } else if (AccessibilityNodeInfoUtils.supportsAction(
            scrollableNode, ACTION_SCROLL_BACKWARD.getId())) {
          return pipeline.returnFeedback(
              eventId, Feedback.nodeAction(scrollableNode, ACTION_SCROLL_BACKWARD.getId()));
        }
      }
      default -> {
        return false;
      }
    }
    return false;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Internal directional navigation methods based on target types.

  /**
   * Navigates to html target from a web pivot.
   *
   * @return {@code true} if any accessibility action is successfully performed.
   */
  private boolean navigateToHtmlTarget(
      AccessibilityNodeInfoCompat pivot, NavigationAction navigationAction, EventId eventId) {
    return pipeline.returnFeedback(eventId, Feedback.webDirectionHtml(pivot, navigationAction));
  }

  /**
   * Finds next node in a different container (list/grid/pager/recycler/scrollable/containerTitle).
   */
  public AccessibilityNodeInfoCompat findContainerTarget(
      @Nullable AccessibilityNodeInfoCompat start, @NonNull NavigationAction navigationAction) {
    if (start == null) {
      return null;
    }

    @SearchDirection int direction = navigationAction.searchDirection;
    @Nullable AccessibilityNodeInfoCompat root = AccessibilityNodeInfoUtils.getRoot(start);

    // Find current container, searching up from current-focus.
    Filter<AccessibilityNodeInfoCompat> containerFilter =
        NavigationTarget.createContainerFilter(navigationAction.targetType);
    // Moving by list, table or landmark skips items outside any of them.
    boolean containerRequired = NavigationTarget.isWebOnlyTarget(navigationAction.targetType);
    @Nullable AccessibilityNodeInfoCompat containerOld = currentContainer(start, containerFilter);
    LogUtils.v(
        TAG,
        "FocusProcessorForLogicalNavigation.findContainerTarget() containerOld=%s",
        containerOld);

    // Find next node in traversal order that has a different container.
    TraversalStrategy traversalStrategy =
        TraversalStrategyUtils.getTraversalStrategy(root, focusFinder, direction);
    @Nullable AccessibilityNodeInfoCompat target =
        TraversalStrategyUtils.searchFocus(
            traversalStrategy,
            start,
            direction,
            Filter.node(
                n -> {
                  @Nullable
                  AccessibilityNodeInfoCompat container = currentContainer(n, containerFilter);
                  return (container != null || !containerRequired)
                      && !Objects.equals(containerOld, container)
                      && AccessibilityNodeInfoUtils.shouldFocusNode(
                          n, traversalStrategy.getSpeakingNodesCache());
                }));
    // SandFly: the previous container is entered at its start, as the next one is, rather than at
    // its last item.
    // Web content keeps the old landing; stepping through a whole page would take long.
    if (target != null
        && direction == TraversalStrategy.SEARCH_FOCUS_BACKWARD
        && NavigationTarget.isContainerTarget(navigationAction.targetType)
        && !WebInterfaceUtils.supportsWebActions(target)) {
      @Nullable
      AccessibilityNodeInfoCompat containerNew = currentContainer(target, containerFilter);
      for (int step = 0; step < MAX_CONTAINER_START_STEPS; step++) {
        @Nullable AccessibilityNodeInfoCompat previous =
            TraversalStrategyUtils.searchFocus(
                traversalStrategy,
                target,
                direction,
                Filter.node(
                    n ->
                        AccessibilityNodeInfoUtils.shouldFocusNode(
                            n, traversalStrategy.getSpeakingNodesCache())));
        if (previous == null
            || !Objects.equals(containerNew, currentContainer(previous, containerFilter))) {
          break;
        }
        target = previous;
      }
    }
    LogUtils.v(
        TAG,
        "FocusProcessorForLogicalNavigation.findContainerTarget() target container=%s",
        currentContainer(target, containerFilter));
    return target;
  }

  /**
   * Returns the list that moving by list item from {@code pivot} goes through item by item, or null
   * when the move isn't by list item or {@code pivot} isn't in a list outside web content.
   */
  private static @Nullable AccessibilityNodeInfoCompat listForListItemTarget(
      AccessibilityNodeInfoCompat pivot,
      boolean ignoreDescendantsOfPivot,
      NavigationAction navigationAction) {
    if (navigationAction.targetType != NavigationTarget.TARGET_HTML_ELEMENT_LIST_ITEM
        || ignoreDescendantsOfPivot
        || !TraversalStrategyUtils.isLogicalDirection(navigationAction.searchDirection)) {
      return null;
    }
    return ListItemNavigation.listOf(pivot);
  }

  /**
   * Finds the item of {@code list} after or before the one holding {@code pivot}, passing over the
   * controls inside items. Past the last item on screen, finds the next list item after the list,
   * which the list is scrolled through first, as for other elements.
   */
  private @NonNull NavigationResult findListItemTarget(
      AccessibilityNodeInfoCompat pivot,
      AccessibilityNodeInfoCompat list,
      NavigationAction navigationAction,
      @SearchDirection int logicalDirection,
      Filter<AccessibilityNodeInfoCompat> nodeFilter,
      TraversalStrategy traversalStrategy,
      EventId eventId) {
    @Nullable
    AccessibilityNodeInfoCompat target =
        ListItemNavigation.target(
            pivot, list, /* forward= */ logicalDirection == TraversalStrategy.SEARCH_FOCUS_FORWARD);
    if (target != null) {
      return NavigationResult.create(target);
    }
    Filter<AccessibilityNodeInfoCompat> outsideList =
        Filter.node(n -> !AccessibilityNodeInfoUtils.hasAncestor(n, list)).and(nodeFilter);
    return findTargetFromNativeElement(
        list, navigationAction, outsideList, traversalStrategy, eventId);
  }

  /** Finds current container (list/grid/pager/recycler/scrollable/containerTitle...) */
  private @Nullable AccessibilityNodeInfoCompat currentContainer(
      @Nullable AccessibilityNodeInfoCompat start,
      Filter<AccessibilityNodeInfoCompat> containerFilter) {
    if (start == null) {
      return null;
    }

    // Search up from start-node.
    @Nullable AccessibilityNodeInfoCompat container =
        AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(start, containerFilter);
    // SandFly: a bar inside a bar, such as a search bar in a toolbar, is one stop: the outer bar.
    if (container != null && AccessibilityNodeInfoUtils.FILTER_BAR.accept(container)) {
      @Nullable AccessibilityNodeInfoCompat outer =
          AccessibilityNodeInfoUtils.getMatchingAncestor(
              container, AccessibilityNodeInfoUtils.FILTER_BAR);
      while (outer != null) {
        container = outer;
        outer =
            AccessibilityNodeInfoUtils.getMatchingAncestor(
                outer, AccessibilityNodeInfoUtils.FILTER_BAR);
      }
    }
    return container;
  }

  /**
   * Navigates into the next or previous window.
   *
   * <p>Called when the user performs window navigation with keyboard shortcuts.
   *
   * @return {@code true} if any accessibility action is successfully performed.
   */
  private boolean navigateToWindowTarget(
      AccessibilityNodeInfoCompat pivot, NavigationAction navigationAction, EventId eventId) {
    AccessibilityWindowInfo currentWindow = AccessibilityNodeInfoUtils.getWindow(pivot.unwrap());

    // When stealing the next window gesture - typically for a heads-up notification - try to
    // put focus on the target first. If this fails, use the default window navigation logic.
    if (stealWindowNavigationTarget != null && stealWindowNavigationTarget.refresh()) {
      stealWindowNavigationTarget =
          AccessibilityNodeInfoUtils.getSelfOrMatchingDescendant(
              stealWindowNavigationTarget, FILTER_SHOULD_FOCUS);
      if (stealWindowNavigationTarget != null) {
        boolean stoleFocus = false;
        boolean isScreenRtl = WindowUtils.isScreenLayoutRTL(service);
        int logicalDirection =
            TraversalStrategyUtils.getLogicalDirection(
                navigationAction.searchDirection, isScreenRtl);
        int stealLogicalDirection =
            TraversalStrategyUtils.getLogicalDirection(
                stealWindowNavigationTargetDirection, isScreenRtl);
        if (logicalDirection == stealLogicalDirection) {
          stoleFocus =
              setAccessibilityFocusInternal(stealWindowNavigationTarget, navigationAction, eventId);
        }
        LogUtils.d(
            TAG,
            "Try to steal focus with target=%s, steal direction=%s, NavigationAction direction=%s,"
                + " stoleFocus=%b ",
            stealWindowNavigationTarget,
            TraversalStrategyUtils.directionToString(stealWindowNavigationTargetDirection),
            TraversalStrategyUtils.directionToString(navigationAction.searchDirection),
            stoleFocus);

        stealWindowNavigationTarget = null;
        stealWindowNavigationTargetDirection = TraversalStrategy.SEARCH_FOCUS_UNKNOWN;
        if (stoleFocus) {
          return true;
        }
        // Stealing focus didn't work, continue with default logic.
      }
    }

    if (!filterWindowForWindowNavigation.accept(currentWindow)) {
      return false;
    }

    // Navigate to pane window target if it is available.
    if (navigateToWindowPaneTarget(pivot, navigationAction, eventId)) {
      return true;
    }

    Map<AccessibilityNodeInfoCompat, Boolean> speakingNodesCache = new HashMap<>();
    WindowTraversal windowTraversal = new WindowTraversal(service);
    boolean isScreenRtl = WindowUtils.isScreenLayoutRTL(service);
    AccessibilityNodeInfoCompat target =
        searchTargetInNextOrPreviousWindow(
            screenState.getStableScreenState(),
            windowTraversal,
            isScreenRtl,
            /* currentFocus= */ null,
            currentWindow,
            navigationAction.searchDirection,
            focusFinder,
            /* isInitialFocus= */ true,
            filterWindowForWindowNavigation,
            NavigationTarget.createNodeFilter(NavigationTarget.TARGET_DEFAULT, speakingNodesCache));
    return (target != null) && setAccessibilityFocusInternal(target, navigationAction, eventId);
  }

  /**
   * Navigates into the next or previous window pane.
   *
   * <p>Called when the user performs window navigation with keyboard shortcuts.
   *
   * @return {@code true} if any accessibility action is successfully performed.
   */
  private boolean navigateToWindowPaneTarget(
      @Nullable AccessibilityNodeInfoCompat pivot,
      NavigationAction navigationAction,
      EventId eventId) {
    if (pivot == null) {
      return false;
    }
    if (screenState.getStableScreenState() == null
        || !screenState.getStableScreenState().hasAccessibilityPane(pivot.getWindowId())) {
      // Return if the active window doesn't contain window pane.
      return false;
    }

    @SearchDirection int direction = navigationAction.searchDirection;
    @Nullable AccessibilityNodeInfoCompat root = AccessibilityNodeInfoUtils.getRoot(pivot);
    TraversalStrategy traversalStrategy =
        TraversalStrategyUtils.getTraversalStrategy(root, focusFinder, direction);
    @Nullable AccessibilityNodeInfoCompat pivotPaneContainer = currentPaneContainer(pivot);
    // If starting pivot is inside a pane...
    if (pivotPaneContainer != null) {
      // Move focus to the next/previous node outside of this pane container.
      AccessibilityNodeInfoCompat target =
          TraversalStrategyUtils.searchFocus(
              traversalStrategy,
              pivot,
              direction,
              Filter.node(
                  n ->
                      !Objects.equals(pivotPaneContainer, currentPaneContainer(n))
                          && AccessibilityNodeInfoUtils.shouldFocusNode(
                              n, traversalStrategy.getSpeakingNodesCache())));
      if (target != null) {
        return setAccessibilityFocusInternal(target, navigationAction, eventId);
      }
    } else {
      @Nullable AccessibilityNodeInfoCompat targetInWindowPane =
          TraversalStrategyUtils.searchFocus(
              traversalStrategy,
              pivot,
              direction,
              Filter.node(n -> !TextUtils.isEmpty(n.getPaneTitle())));
      if (targetInWindowPane == null) {
        return false;
      }
      // Navigates to the target in window pane.
      AccessibilityNodeInfoCompat target =
          TraversalStrategyUtils.findFirstFocusInNodeTree(
              traversalStrategy,
              targetInWindowPane,
              direction,
              NavigationTarget.createNodeFilter(
                  NavigationTarget.TARGET_DEFAULT, traversalStrategy.getSpeakingNodesCache()));
      if (target != null) {
        return setAccessibilityFocusInternal(target, navigationAction, eventId);
      }
    }

    return false;
  }

  /** Finds current window-pane container. */
  private @Nullable AccessibilityNodeInfoCompat currentPaneContainer(
      @Nullable AccessibilityNodeInfoCompat start) {
    return (start == null)
        ? null
        : AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(
            start, Filter.node(n -> !TextUtils.isEmpty(n.getPaneTitle())));
  }

  /**
   * Navigates to screen-search target.
   *
   * @return {@code true} if any accessibility action is successfully performed.
   */
  private boolean navigateToScreenSearchTarget(
      @NonNull AccessibilityNodeInfoCompat pivot,
      NavigationAction navigationAction,
      EventId eventId) {
    CharSequence keyword = searchState.getLastKeyword();
    if (TextUtils.isEmpty(keyword)) {
      LogUtils.d(TAG, "navigateToScreenSearchTarget  keyword empty");
      announce(globalVariables.getScreenSearchNoKeywordHint(), eventId);
      return false;
    }

    AccessibilityNodeInfoCompat target = findSearchTarget(pivot, navigationAction, keyword);
    ScrollableNodeInfo scrollableNodeInfo =
        ScrollableNodeInfo.findScrollableNodeForDirection(
            navigationAction.searchDirection,
            pivot,
            /* includeSelf= */ true,
            WindowUtils.isScreenLayoutRTL(service));
    if (scrollableNodeInfo != null
        && (target == null
            || (!Objects.equals(
                findContainerTarget(pivot, navigationAction),
                findContainerTarget(target, navigationAction))))) {
      // Auto-scroll then search again if no target found on current page or container changed.
      if (autoScroll(scrollableNodeInfo, pivot, navigationAction, eventId)) {
        return true;
      }
    }

    if (target == null) {
      announceNoSearchResult(navigationAction.searchDirection, eventId);
      return false;
    }
    return setAccessibilityFocusInternal(target, navigationAction, eventId);
  }

  /** Finds next or previous screen search target. */
  private @Nullable AccessibilityNodeInfoCompat findSearchTarget(
      @Nullable AccessibilityNodeInfoCompat pivot,
      @NonNull NavigationAction navigationAction,
      CharSequence keyword) {
    if (pivot == null || TextUtils.isEmpty(keyword)) {
      return null;
    }

    @SearchDirection int direction = navigationAction.searchDirection;
    TraversalStrategy traversalStrategy =
        TraversalStrategyUtils.getTraversalStrategy(
            AccessibilityNodeInfoUtils.getRoot(pivot),
            focusFinder,
            OrderedTraversalStrategyConfig.builder()
                .setSearchDirection(direction)
                .setIncludeChildrenOfNodesWithWebActions(true)
                .build());
    AccessibilityNodeInfoCompat focusablePivot =
        AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(
            pivot, AccessibilityNodeInfoUtils.FILTER_SHOULD_FOCUS);
    @Nullable AccessibilityNodeInfoCompat target =
        TraversalStrategyUtils.searchFocus(
            traversalStrategy,
            pivot,
            direction,
            Filter.node(
                node -> {
                  if (node == null) {
                    return false;
                  }
                  if (!AccessibilityNodeInfoUtils.isVisible(node)) {
                    return false;
                  }
                  // Skip the same focusable parent. The search target that should not focus may
                  // have the same focusable parent with the nodes inside the focusable parent. It
                  // will help navigation across different focusable screen-search target.
                  if (Objects.equals(
                      focusablePivot,
                      AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(
                          node, AccessibilityNodeInfoUtils.FILTER_SHOULD_FOCUS))) {
                    return false;
                  }
                  CharSequence nodeText = AccessibilityNodeInfoUtils.getNodeText(node);
                  if (TextUtils.isEmpty(nodeText)) {
                    return false;
                  }
                  return !StringMatcher.findMatches(nodeText.toString(), keyword.toString())
                      .isEmpty();
                }));

    return AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(
        target, AccessibilityNodeInfoUtils.FILTER_SHOULD_FOCUS);
  }

  /**
   * Navigates to default target or macro granularity target.
   *
   * <p>This navigation action happens in four use cases:
   *
   * <ul>
   *   <li>The user is navigating with default granularity
   *   <li>The use is navigating with native macro granularity.
   *   <li>The use is navigating with html macro granularity.
   *   <li>The user is navigating with micro granularity, but reaches edge of current node, and
   *       needs to move focus to another node. In this case {@link
   *       NavigationAction#originalNavigationGranularity} is not {@code null} and {link
   *       CursorGranularity#isMicroGranularity} is {@code true}.
   * </ul>
   *
   * @return {@code true} if any accessibility action is successfully performed.
   */
  /**
   * Returns the node that a plain swipe in {@code searchDirection} from {@code pivot} would most
   * likely focus, without scrolling, focusing or changing anything, or null if there is none on
   * screen in the same window, or if there is no saved traversal order to search. It finds the
   * nodes through the same saved order and node checks as a swipe, so that a swipe that follows
   * finds them already fetched from the app.
   */
  /** The direction of the last move to the next or previous item: forward or backward. */
  public @SearchDirection int lastLogicalDirection() {
    return lastLogicalDirection;
  }

  public @Nullable AccessibilityNodeInfoCompat predictTarget(
      @NonNull AccessibilityNodeInfoCompat pivot, @SearchDirection int searchDirection) {
    if (!TraversalStrategyUtils.isLogicalDirection(searchDirection)
        || WebInterfaceUtils.supportsWebActions(pivot)) {
      return null;
    }
    // Only with the saved reading order or an order worked out around the pivot, never a new
    // whole order: building one takes from about 10 ms to, in a big app, a few hundred, on the
    // main thread, where a swipe that starts meanwhile would wait for it. A swipe saves the order
    // it builds, so the next prediction has one. The root comes from the saved order too, as
    // asking the window for it would wait for the app.
    AccessibilityNodeInfoCompat rootNode = TraversalTreeCache.rootFor(pivot);
    TraversalStrategy traversalStrategy = null;
    @Nullable LocalTraversalOrder predictionOrder = null;
    if (rootNode == null) {
      // Remember where the next swipe starts all the same, as a search of the saved order does, so
      // that a swipe after the focused node is removed carries on from here.
      TraversalTreeCache.rememberPivot(pivot);
      // Without a saved order, work out the next item around the pivot, as a swipe would, but
      // never build the whole order here.
      @Nullable AccessibilityNodeInfoCompat localRoot = TraversalTreeCache.localRootFor(pivot);
      if (localRoot == null || ScreenElements.isShowing()) {
        return null;
      }
      predictionOrder = LocalTraversalOrder.start(pivot, localRoot, /* buildFull= */ null);
      traversalStrategy = predictionOrder;
    } else {
      traversalStrategy = TraversalTreeCache.get(rootNode, pivot);
    }
    if (traversalStrategy == null) {
      return null;
    }
    Filter<AccessibilityNodeInfoCompat> nodeFilter =
        NavigationTarget.createNodeFilter(
            NavigationTarget.TARGET_DEFAULT, traversalStrategy.getSpeakingNodesCache());
    AccessibilityNodeInfoCompat target =
        TraversalStrategyUtils.searchFocus(traversalStrategy, pivot, searchDirection, nodeFilter);
    if (predictionOrder != null) {
      predictionOrder.finish();
    }
    if (target == null
        || target.getWindowId() != pivot.getWindowId()
        || !target.isVisibleToUser()
        || WebInterfaceUtils.supportsWebActions(target)) {
      return null;
    }
    return target;
  }

  /**
   * Builds the order of {@code root} again after reading the active window with one request that
   * also returns its descendants, when a node in the order of {@code root} lists children none of
   * which could be read. Some apps, such as Xiaomi's on HyperOS, list containers among a window's
   * children that Android does not return when asked for one node at a time, which left only the
   * root in the order: both swipe directions then stopped at a false edge. The descendants of the
   * window's root land in Android's node cache, from which the build then reads them. Done once
   * until the window changes, and only when {@code root} is the active window's root. Returns null
   * when nothing was read again.
   */
  private @Nullable OrderedTraversalStrategy rereadIncompleteOrder(
      AccessibilityNodeInfoCompat root) {
    // Android usually answers this from its window cache, filled by the window list.
    @Nullable AccessibilityWindowInfoCompat window = AccessibilityNodeInfoUtils.getWindow(root);
    if (window == null
        || !window.isActive()
        || !TraversalTreeCache.startReread(root.getWindowId())) {
      return null;
    }
    @Nullable AccessibilityNodeInfo activeRoot = readActiveWindowRoot();
    if (activeRoot == null || activeRoot.getWindowId() != root.getWindowId()) {
      return null;
    }
    WindowNodePrefetcher.prefetch(root, service.getPackageName());
    OrderedTraversalStrategy reread = new OrderedTraversalStrategy(root);
    LogUtils.i(
        TAG,
        "Order of window %d missed children; read again, %s",
        root.getWindowId(),
        reread.missesChildren() ? "still incomplete" : "complete");
    return reread;
  }

  /**
   * Reads the root of the active window from the app with its descendants. The root that
   * TalkBackService returns is a copy saved until the next window event, so it is marked out of
   * date first on versions without the prefetching overload.
   */
  private @Nullable AccessibilityNodeInfo readActiveWindowRoot() {
    if (BuildVersionUtils.isAtLeastT()) {
      return service.getRootInActiveWindow(AccessibilityNodeInfo.FLAG_PREFETCH_DESCENDANTS_HYBRID);
    }
    if (service instanceof TalkBackService talkBackService) {
      talkBackService.setRootDirty(true);
    }
    return service.getRootInActiveWindow();
  }

  /** The app node whose recognised screen elements a swipe has just left, going forward. */
  private @Nullable AccessibilityNodeInfoCompat leavingScreenElementsOf;

  /**
   * Moves through recognised screen elements, which sit in the reading order after the app node
   * each is anchored to. Returns null when the swipe is the app's own.
   */
  private @Nullable Boolean navigateAroundScreenElements(
      AccessibilityNodeInfoCompat pivot,
      boolean ignoreDescendantsOfPivot,
      NavigationAction navigationAction,
      @SearchDirection int logicalDirection,
      EventId eventId) {
    if (ScreenElements.getNeedsOrder()) {
      orderScreenElements();
    }
    boolean forward = logicalDirection == TraversalStrategy.SEARCH_FOCUS_FORWARD;
    int index = ScreenElements.indexOf(pivot);
    if (index >= 0) {
      @Nullable AccessibilityNodeInfoCompat next = ScreenElements.neighbour(index, forward);
      if (next != null) {
        return setAccessibilityFocusInternal(next, navigationAction, eventId);
      }
      @Nullable AccessibilityNodeInfoCompat anchor = ScreenElements.anchor(index);
      if (anchor != null && ScreenElements.isCoveredHost(anchor)) {
        // A view the elements cover is passed over, also a WebView, which would otherwise take the
        // swipe into its own web navigation.
        @Nullable AccessibilityNodeInfoCompat past = ScreenElements.pastCoveredHost(anchor, forward);
        if (past != null) {
          return setAccessibilityFocusInternal(past, navigationAction, eventId);
        }
        // At the window's edge, the swipe goes on from the covered view as SandFly's own: to the
        // next window, or around the app's window.
        leavingScreenElementsOf = anchor;
        try {
          return navigateToDefaultOrMacroGranularityTarget(
              anchor, ignoreDescendantsOfPivot, navigationAction, eventId);
        } finally {
          leavingScreenElementsOf = null;
        }
      }
      if (!forward) {
        if (anchor != null) {
          return setAccessibilityFocusInternal(anchor, navigationAction, eventId);
        }
        return wrapAroundScreenElements(
            ignoreDescendantsOfPivot, navigationAction, forward, eventId);
      }
      if (anchor == null) {
        @Nullable AccessibilityNodeInfoCompat first = ScreenElements.firstAppNodeOrNull();
        if (first != null) {
          return setAccessibilityFocusInternal(first, navigationAction, eventId);
        }
        return wrapAroundScreenElements(
            ignoreDescendantsOfPivot, navigationAction, forward, eventId);
      }
      // Carry on from the anchor as if the swipe had started there.
      leavingScreenElementsOf = anchor;
      try {
        return navigateToDefaultOrMacroGranularityTarget(
            anchor, ignoreDescendantsOfPivot, navigationAction, eventId);
      } finally {
        leavingScreenElementsOf = null;
      }
    }
    if (pivot.getWindowId() != ScreenElements.getHostWindow()) {
      return null;
    }
    if (pivot.equals(leavingScreenElementsOf)) {
      return null;
    }
    @Nullable AccessibilityNodeInfoCompat outside = ScreenElements.fromOutsideOrder(pivot, forward);
    if (outside != null) {
      return setAccessibilityFocusInternal(outside, navigationAction, eventId);
    }
    if (forward) {
      @Nullable AccessibilityNodeInfoCompat element = ScreenElements.firstAfter(pivot);
      return element == null ? null : setAccessibilityFocusInternal(element, navigationAction, eventId);
    }
    if (ScreenElements.isFirstAppNode(pivot)) {
      @Nullable AccessibilityNodeInfoCompat element = ScreenElements.lastBefore(null);
      if (element != null) {
        return setAccessibilityFocusInternal(element, navigationAction, eventId);
      }
    }
    return null;
  }

  /**
   * Goes on from the first or last recognised element of a window when the swipe leaves the
   * elements at the window's start or end: around the elements of a window without nodes of its
   * own, such as a game, if wrapping is on; otherwise on from the window's first node.
   */
  private boolean wrapAroundScreenElements(
      boolean ignoreDescendantsOfPivot,
      NavigationAction navigationAction,
      boolean forward,
      EventId eventId) {
    @Nullable AccessibilityNodeInfoCompat first = ScreenElements.firstAppNodeOrNull();
    if (first == null) {
      if (!navigationAction.shouldWrap || !wrapAround) {
        stoppedAtEdge = !wrapAround;
        return false;
      }
      @Nullable AccessibilityNodeInfoCompat element = ScreenElements.beforeFirstNode(forward);
      return element != null && setAccessibilityFocusInternal(element, navigationAction, eventId);
    }
    // Before the window's first node, a backward swipe carries on from there, around the window.
    leavingScreenElementsOf = first;
    try {
      return navigateToDefaultOrMacroGranularityTarget(
          first, ignoreDescendantsOfPivot, navigationAction, eventId);
    } finally {
      leavingScreenElementsOf = null;
    }
  }

  /** Gives each recognised screen element its anchor among the host window's nodes. */
  private void orderScreenElements() {
    List<AccessibilityNodeInfoCompat> nodes = new ArrayList<>();
    @Nullable AccessibilityWindowInfo host = null;
    for (AccessibilityWindowInfo window : AccessibilityServiceCompatUtils.getWindows(service)) {
      if (window.getId() == ScreenElements.getHostWindow()) {
        host = window;
        break;
      }
    }
    @Nullable AccessibilityNodeInfoCompat root =
        host == null ? null : AccessibilityWindowInfoUtils.getRootCompat(host);
    if (root != null) {
      TraversalStrategy strategy =
          TraversalStrategyUtils.getTraversalStrategy(
              root, focusFinder, TraversalStrategy.SEARCH_FOCUS_FORWARD);
      Filter<AccessibilityNodeInfoCompat> filter =
          NavigationTarget.createNodeFilter(
              NavigationTarget.TARGET_DEFAULT, strategy.getSpeakingNodesCache());
      @Nullable AccessibilityNodeInfoCompat node =
          filter.accept(root)
              ? root
              : TraversalStrategyUtils.searchFocus(
                  strategy, root, TraversalStrategy.SEARCH_FOCUS_FORWARD, filter);
      while (node != null && nodes.size() < MAX_ORDERED_NODES) {
        if (nodes.contains(node)) {
          break;
        }
        nodes.add(node);
        node =
            TraversalStrategyUtils.searchFocus(
                strategy, node, TraversalStrategy.SEARCH_FOCUS_FORWARD, filter);
      }
    }
    ScreenElements.setOrder(nodes);
  }

  private static final int MAX_ORDERED_NODES = 2000;

  private boolean navigateToDefaultOrMacroGranularityTarget(
      @NonNull AccessibilityNodeInfoCompat pivot,
      boolean ignoreDescendantsOfPivot,
      NavigationAction navigationAction,
      EventId eventId) {
    return navigateToDefaultOrMacroGranularityTarget(
        pivot, ignoreDescendantsOfPivot, navigationAction, /* htmlActionFailed= */ false, eventId);
  }

  /**
   * Same as {@link #navigateToDefaultOrMacroGranularityTarget(AccessibilityNodeInfoCompat, boolean,
   * NavigationAction, EventId)}, except that {@code htmlActionFailed} says that the web page has
   * already been asked to move from {@code pivot} for this navigation, and had nothing to move to.
   */
  private boolean navigateToDefaultOrMacroGranularityTarget(
      @NonNull AccessibilityNodeInfoCompat pivot,
      boolean ignoreDescendantsOfPivot,
      NavigationAction navigationAction,
      boolean htmlActionFailed,
      EventId eventId) {
    @Nullable LocalTraversalOrder outerLocalOrder = localOrder;
    localOrder = null;
    try {
      return navigateToDefaultOrMacroGranularityTargetInternal(
          pivot, ignoreDescendantsOfPivot, navigationAction, htmlActionFailed, eventId);
    } catch (LocalTraversalOrder.MismatchException e) {
      // Thrown by a search, before anything was done with its result.
      if (localOrder != null) {
        localOrder.finish();
        localOrder = null;
      }
      boolean outerOff = localOrderOff;
      localOrderOff = true;
      try {
        return navigateToDefaultOrMacroGranularityTargetInternal(
            pivot, ignoreDescendantsOfPivot, navigationAction, htmlActionFailed, eventId);
      } finally {
        localOrderOff = outerOff;
      }
    } finally {
      if (localOrder != null) {
        localOrder.finish();
      }
      localOrder = outerLocalOrder;
    }
  }

  /**
   * Whether the next or previous item from {@code pivot} may be worked out around it, with {@link
   * LocalTraversalOrder}, when the saved reading order doesn't hold it: a swipe or key to the next
   * or previous item in a native view.
   */
  private static boolean mayUseLocalOrder(
      AccessibilityNodeInfoCompat pivot, NavigationAction navigationAction) {
    return isMoveToNextOrPreviousItem(navigationAction)
        && !ScreenElements.isShowing()
        && !WebInterfaceUtils.supportsWebActions(pivot);
  }

  /**
   * Builds the reading order of the window of {@code rootNode}, reading its nodes from the app
   * first, and saves it for later swipes.
   */
  private TraversalStrategy buildWindowOrder(
      AccessibilityNodeInfoCompat rootNode, @SearchDirection int searchDirection) {
    boolean trace = LogUtils.shouldLog(Log.DEBUG);
    long buildStart = trace ? SystemClock.uptimeMillis() : 0;
    if (trace) {
      Trace.beginSection("BuildTraversalStrategy");
    }
    WindowNodePrefetcher.prefetchWindow(rootNode, service.getPackageName());
    TraversalStrategy traversalStrategy =
        TraversalStrategyUtils.getTraversalStrategy(rootNode, focusFinder, searchDirection);
    if (trace) {
      Trace.endSection();
      Log.d(
          "BacktalkTreeCache",
          "Built order in " + (SystemClock.uptimeMillis() - buildStart) + " ms");
    }
    if (traversalStrategy instanceof OrderedTraversalStrategy orderedStrategy
        && orderedStrategy.missesChildren()) {
      @Nullable OrderedTraversalStrategy reread = rereadIncompleteOrder(rootNode);
      if (reread != null) {
        traversalStrategy = reread;
      }
    }
    if (traversalStrategy instanceof OrderedTraversalStrategy orderedStrategy) {
      TraversalTreeCache.put(rootNode, orderedStrategy);
    }
    return traversalStrategy;
  }

  private boolean navigateToDefaultOrMacroGranularityTargetInternal(
      @NonNull AccessibilityNodeInfoCompat pivot,
      boolean ignoreDescendantsOfPivot,
      NavigationAction navigationAction,
      boolean htmlActionFailed,
      EventId eventId) {
    int searchDirection = navigationAction.searchDirection;
    int logicalDirection =
        TraversalStrategyUtils.getLogicalDirection(
            searchDirection, WindowUtils.isScreenLayoutRTL(service));
    if (logicalDirection == TraversalStrategy.SEARCH_FOCUS_FORWARD
        || logicalDirection == TraversalStrategy.SEARCH_FOCUS_BACKWARD) {
      lastLogicalDirection = logicalDirection;
      lastLogicalDirectionUptime = SystemClock.uptimeMillis();
    }

    if (ScreenElements.isShowing()
        && navigationAction.targetType == NavigationTarget.TARGET_DEFAULT
        && TraversalStrategyUtils.isLogicalDirection(searchDirection)) {
      @Nullable Boolean handled =
          navigateAroundScreenElements(
              pivot, ignoreDescendantsOfPivot, navigationAction, logicalDirection, eventId);
      if (handled != null) {
        return handled;
      }
    }

    // In web content, the page finds the next item itself, and the window's reading order, which
    // leaves out the page's items, is only needed to leave the page. Building that order took up
    // to a few hundred milliseconds on every swipe in a big app, so ask the page first.
    if (!htmlActionFailed
        && !ignoreDescendantsOfPivot
        && navigationAction.actionType == NavigationAction.DIRECTIONAL_NAVIGATION
        && TraversalStrategyUtils.isLogicalDirection(searchDirection)
        && WebInterfaceUtils.supportsWebActions(pivot)
        && Role.getRole(pivot) != Role.ROLE_WEB_VIEW) {
      // A search of the saved order would remember where this swipe started.
      TraversalTreeCache.rememberPivot(pivot);
      if (navigateToHtmlTargetExpectingFocus(pivot, navigationAction, eventId)) {
        reachEdge = false;
        return true;
      }
      htmlActionFailed = true;
      // The page's last item is focused, and the page is a row of a native list ending at its edge:
      // scroll the list, so the rows past it come on screen.
      if (navigationAction.shouldScroll
          && scrollListHoldingWebView(pivot, navigationAction, logicalDirection, eventId)) {
        return true;
      }
    }

    // At the end of a list on screen, scroll at once rather than build the window's order first,
    // which the scroll makes out of date anyway.
    if (!revealingOffscreen
        && !ignoreDescendantsOfPivot
        && !htmlActionFailed
        && scrollFromListEndAtOnce(pivot, navigationAction, logicalDirection, eventId)) {
      return true;
    }

    // Asking the window for its root waits for the app on every swipe, so take the root of the
    // saved reading order when that holds the pivot.
    AccessibilityNodeInfoCompat rootNode = TraversalTreeCache.rootFor(pivot);
    // Without a saved order, work out the next item around the pivot, as long as the answer is the
    // same as the whole order's. The root kept for that is checked to be the pivot's.
    if (rootNode == null && !localOrderOff && mayUseLocalOrder(pivot, navigationAction)) {
      @Nullable AccessibilityNodeInfoCompat localRoot = TraversalTreeCache.localRootFor(pivot);
      if (localRoot != null) {
        TraversalTreeCache.rememberPivot(pivot);
        localOrder =
            LocalTraversalOrder.start(
                pivot,
                localRoot,
                // A logical direction, which always gives an ordered strategy.
                () -> (OrderedTraversalStrategy) buildWindowOrder(localRoot, searchDirection));
        if (localOrder != null) {
          rootNode = localRoot;
        }
      }
    }
    if (rootNode == null) {
      rootNode = AccessibilityNodeInfoUtils.getRoot(pivot);
    }
    if (rootNode == null) {
      LogUtils.w(TAG, "Cannot perform navigation action: unable to find root node.");
      return false;
    }

    // Build the window's traversal tree once and share it with the edge checks below, which would
    // otherwise each build their own copy of the same tree. Reuse the tree of the last swipe if
    // nothing in the window has changed since.
    TraversalStrategy traversalStrategy = localOrder;
    if (traversalStrategy == null && TraversalStrategyUtils.isLogicalDirection(searchDirection)) {
      traversalStrategy = TraversalTreeCache.get(rootNode, pivot);
    }
    boolean usedSavedTree = traversalStrategy != null && traversalStrategy != localOrder;
    if (traversalStrategy == null) {
      traversalStrategy = buildWindowOrder(rootNode, searchDirection);
    }

    // In a scroll view the next item is already in the tree, just off screen. Show the next screen
    // at once and search again, rather than scrolling a page and waiting for the scroll event.
    if (!revealingOffscreen) {
      @RevealResult
      int reveal =
          revealNextOffscreenItem(
              pivot,
              ignoreDescendantsOfPivot,
              navigationAction,
              logicalDirection,
              reusableOrderedStrategy(traversalStrategy, rootNode, pivot),
              eventId);
      if (reveal == REVEAL_SHOWN) {
        // The saved order was read before the scroll, so it still has the next item off screen,
        // and the search would stop at the edge.
        TraversalTreeCache.clear("revealed next item");
        revealingOffscreen = true;
        try {
          return navigateToDefaultOrMacroGranularityTarget(
              pivot, ignoreDescendantsOfPivot, navigationAction, htmlActionFailed, eventId);
        } finally {
          revealingOffscreen = false;
        }
      }
      if (reveal == REVEAL_MOVED) {
        // The view moved, but the item is still not on screen. A page scroll now would pass what
        // the move brought on screen, so stop here, and let the next swipe carry on.
        return true;
      }
    }

    // Perform auto-scroll action if necessary. Not after showing the next item, which has already
    // scrolled the view: a page scroll on top would pass content that never came on screen.
    edgeScrollNote = revealingOffscreen ? "afterReveal" : "notTried";
    if (!revealingOffscreen
        && autoScrollAtEdge(
            pivot,
            ignoreDescendantsOfPivot,
            navigationAction,
            reusableOrderedStrategy(traversalStrategy, rootNode, pivot),
            eventId)) {
      return true;
    }

    Filter<AccessibilityNodeInfoCompat> nodeFilter =
        NavigationTarget.createNodeFilter(
            navigationAction.targetType, traversalStrategy.getSpeakingNodesCache());
    if (ignoreDescendantsOfPivot) {
      final AccessibilityNodeInfoCompat pivotCopy = pivot;
      nodeFilter =
          new Filter<AccessibilityNodeInfoCompat>() {
            @Override
            public boolean accept(AccessibilityNodeInfoCompat node) {
              return !AccessibilityNodeInfoUtils.hasAncestor(node, pivotCopy);
            }
          }.and(nodeFilter);
    }

    NavigationResult navigationResult = NavigationResult.create(NavigationResult.Type.EMPTY);
    // Moving by list item in a list goes from item to item of that list.
    @Nullable
    AccessibilityNodeInfoCompat list =
        listForListItemTarget(pivot, ignoreDescendantsOfPivot, navigationAction);

    // Search for target node within current window.
    // Consider the following linear order when navigating between web elements and native elements
    // assuming that a WebView container is positioned between native elements:
    // - Forward: Native elements -> WebView container -> web elements -> native elements.
    // - Backward: Native elements -> web elements -> WebView container -> native elements.
    // Note: the design assumes there is only one WebView and multiple WebView should be a corner
    // case which is out of the scope.
    if (WebInterfaceUtils.supportsWebActions(pivot)) {
      navigationResult =
          htmlActionFailed
              ? findTargetOutsideWebView(pivot, navigationAction, nodeFilter, traversalStrategy)
              : findTargetFromWebElement(
                  pivot, navigationAction, nodeFilter, traversalStrategy, eventId);
    } else if (NavigationTarget.isContainerTarget(navigationAction.targetType)) {
      navigationResult = NavigationResult.create(findContainerTarget(pivot, navigationAction));
    } else if (list != null) {
      navigationResult =
          findListItemTarget(
              pivot,
              list,
              navigationAction,
              logicalDirection,
              nodeFilter,
              traversalStrategy,
              eventId);
    } else {
      int linkIndex = getLinkIndexFromPivot(pivot, navigationAction);
      if (linkIndex >= 0) {
        navigationResult = NavigationResult.create(pivot);
        navigationResult.setLinkIndex(linkIndex);
      } else {
        navigationResult =
            findTargetFromNativeElement(
                pivot, navigationAction, nodeFilter, traversalStrategy, eventId);
      }
    }
    if (navigationResult.shouldSkipNavigation()) {
      if (navigationResult.isWebElement()) {
        reachEdge = false;
        LogUtils.d(
            TAG, "Return and reset reachEdge, web element focus will be handled by the framework.");
      }
      return navigationResult.isFocusAvailable();
    }

    if (scrollAfterFindTarget(
        pivot, navigationResult.getNode(), ignoreDescendantsOfPivot, navigationAction, eventId)) {
      return true;
    }

    // Special rule to optimize the traversal order of AutoCompleteTextView suggestions.
    NavigationResult suggestedResult =
        findTargetForEditTextSuggestions(
            pivot, navigationAction, nodeFilter, navigationResult.isEmpty());
    if (!suggestedResult.isEmpty()) {
      navigationResult = suggestedResult;
      LogUtils.d(TAG, "Target is overrided by findSuggestedTargetForEditText");
      if (navigationResult.shouldSkipNavigation()) {
        return navigationResult.isFocusAvailable();
      }
    }

    // No target available in the current window, so navigate across windows.
    if (isWindowNavigationSupported && navigationResult.isEmpty()) {
      navigationResult = findTargetAcrossWindows(pivot, navigationAction, eventId);
      if (navigationResult.shouldSkipNavigation()) {
        return navigationResult.isFocusAvailable();
      }
    }

    // Try to wrap around inside current window if reaching the edge.
    if (reachEdge && navigationAction.shouldWrap && wrapAround && navigationResult.isEmpty()) {
      navigationResult =
          findTargetForWrapAround(rootNode, navigationAction, traversalStrategy, eventId);
      if (navigationResult.shouldSkipNavigation()) {
        if (navigationResult.isWebElement()) {
          reachEdge = false;
          LogUtils.d(
              TAG,
              "Return and reset reachEdge, web element focus will be handled by the framework.");
        }
        return navigationResult.isFocusAvailable();
      }
    }

    // Special rule to search focus for directional traversal.
    if ((navigationAction.targetType == NavigationTarget.TARGET_DEFAULT)
        && TraversalStrategyUtils.isSpatialDirection(navigationAction.searchDirection)) {
      suggestedResult =
          findTargetForDirectionalTraveral(
              pivot, navigationResult.getNode(), navigationAction.searchDirection);
      if (!suggestedResult.isEmpty()) {
        navigationResult = suggestedResult;
        LogUtils.d(TAG, "Target is overrided by findTargetForDirectionalTraveral");
        if (navigationResult.shouldSkipNavigation()) {
          return navigationResult.isFocusAvailable();
        }
      }
    }

    AccessibilityNodeInfoCompat target = navigationResult.getNode();

    // Recognised screen elements anchored to the target come between it and the pivot, and a view
    // they cover is passed over for its elements.
    if (target != null
        && navigationAction.targetType == NavigationTarget.TARGET_DEFAULT
        && ScreenElements.isShowing()
        && target.getWindowId() == ScreenElements.getHostWindow()) {
      @Nullable AccessibilityNodeInfoCompat element = null;
      if (logicalDirection == TraversalStrategy.SEARCH_FOCUS_BACKWARD) {
        element = ScreenElements.lastBefore(target);
      } else if (logicalDirection == TraversalStrategy.SEARCH_FOCUS_FORWARD) {
        if (ScreenElements.getNeedsOrder()) {
          orderScreenElements();
        }
        if (ScreenElements.isFirstAppNode(target)) {
          // Wrapping around or coming from another window: the elements before the first node
          // come first.
          element = ScreenElements.beforeFirstNode(/* forward= */ true);
        }
        if (element == null && ScreenElements.isCoveredHost(target)) {
          element = ScreenElements.firstAfter(target);
        }
      }
      if (element != null) {
        return setAccessibilityFocusInternal(element, navigationAction, eventId);
      }
    }

    // A plain swipe to an item that is at least partly on screen speaks it first and brings the
    // rest of it on screen after, as reading what may need scrolling waits for the app. An item
    // with text off screen, such as a row whose title is cut off at the list's edge, is described
    // without that text until the scroll, so it is scrolled first and spoken from the app's focus
    // event, which then comes after the scroll.
    boolean revealAfterFocus =
        target != null
            && navigationAction.shouldScroll
            && navigationAction.actionType == NavigationAction.DIRECTIONAL_NAVIGATION
            && navigationAction.targetType == NavigationTarget.TARGET_DEFAULT
            && target.isVisibleToUser();
    if (revealAfterFocus
        && hasTextOffScreen(
            target, scrollingAncestorBounds(target), SystemBarCover.systemBarBounds(service))) {
      LogUtils.d(TAG, "Scrolling before focusing: text in the target is off screen.");
      revealAfterFocus = false;
    }
    if ((target != null) && navigationAction.shouldScroll && !revealAfterFocus) {
      boolean scrolled =
          ensureOnScreen(
              target,
              navigationAction.searchDirection,
              reusableOrderedStrategy(traversalStrategy, rootNode, target),
              eventId);
      // REFERTO If ensureOnScreen caused scrolling, we need use the scroll callback
      // to set focus on the next node (from the pivot) inside scrollable parent. This is helpful
      // to find focus that was invisible before scrolling.
      // A plain swipe already knows its target, which is in the tree because it is at least partly
      // on screen. Focus it now and let the scroll finish behind the speech, instead of waiting
      // about 200 ms for the scroll event and searching again. Other targets, such as headings and
      // table cells, still search again after the scroll.
      boolean focusBeforeScrollEnds =
          scrolled
              && navigationAction.actionType == NavigationAction.DIRECTIONAL_NAVIGATION
              && navigationAction.targetType == NavigationTarget.TARGET_DEFAULT
              && target.isVisibleToUser();
      if (focusBeforeScrollEnds) {
        scrollCallback = null;
      } else if (scrolled
          && (scrollCallback == null || !scrollCallback.assumeScrollSuccess())) {
        // REFERTO Framework might not send TYPE_VIEW_SCROLLED event back after
        // ensureOnScreen. Register a scrollCallBack may make the scroll timeout and thus
        // searching nodes outside the scrollable and fail. So we ignore scroll fail in this case
        // by setting assumeScrollSuccess to true. To prevent recursively searching focus after
        // scroll fail, we also check whether the caller comes from assumeScrollSuccess to ensure
        // it only bypass once.
        // TODO: remove the workaround after fixing the bug in framework and a11y
        // event is ready.
        scrollCallback =
            new AutoScrollCallback(this, navigationAction, pivot, /* assumeScrollSuccess= */ true);
        return true;
      }
    }

    if (target != null) {
      int linkIndexToFocus = getLinkIndexForFocus(pivot, navigationResult, navigationAction);
      if (linkIndexToFocus >= 0) {
        // Announce the single link feedback instead of a11y focus feedback.
        announceSingleLinkFocused(LinkUtils.getLinkText(target, linkIndexToFocus), eventId);
        boolean focused =
            setAccessibilityFocusInternal(target, navigationAction, linkIndexToFocus, eventId);
        if (focused && revealAfterFocus) {
          revealAfterFocus(target, navigationAction, traversalStrategy, rootNode, eventId);
        }
        return focused;
      } else {
        boolean focused =
            setAccessibilityFocusInternal(
                target,
                navigationAction,
                /* linkIndex= */ -1,
                columnWrap(pivot, target, navigationAction, logicalDirection),
                eventId);
        if (focused && revealAfterFocus) {
          revealAfterFocus(target, navigationAction, traversalStrategy, rootNode, eventId);
        }
        if (!focused && usedSavedTree) {
          // The app might have removed the target before its change event reached us, so try
          // again with a new tree.
          TraversalTreeCache.clear("focus failed");
          return navigateToDefaultOrMacroGranularityTarget(
              pivot, ignoreDescendantsOfPivot, navigationAction, htmlActionFailed, eventId);
        }
        return focused;
      }
    }

    // No target found.
    logEdge(pivot, navigationAction);
    announceNativeElement(logicalDirection, navigationAction.targetType, eventId);
    return false;
  }

  /** Most descendants of a swipe's target that {@link #hasTextOffScreen} looks at. */
  private static final int MAX_OFF_SCREEN_TEXT_NODES = 32;

  /**
   * Whether {@code target}, which has no description of its own and is read by its contents, has
   * a descendant with text that isn't visible to the user, such as the title of a list row cut off
   * at the list's edge or behind its padding. Only the first {@link #MAX_OFF_SCREEN_TEXT_NODES}
   * descendants are looked at.
   */
  @VisibleForTesting
  static boolean hasTextOffScreen(AccessibilityNodeInfoCompat target) {
    return hasTextOffScreen(target, /* listBounds= */ null, Collections.emptyList());
  }

  /** Bounds of the list that scrolls {@code target}, or null if there is none. */
  private static @Nullable Rect scrollingAncestorBounds(AccessibilityNodeInfoCompat target) {
    AccessibilityNodeInfoCompat list =
        AccessibilityNodeInfoUtils.getMatchingAncestor(target, FILTER_AUTO_SCROLL);
    if (list == null) {
      list =
          AccessibilityNodeInfoUtils.getMatchingAncestor(
              target, AccessibilityNodeInfoUtils.FILTER_AUTO_SCROLL_ANY_ROLE);
    }
    if (list == null) {
      return null;
    }
    Rect bounds = new Rect();
    list.getBoundsInScreen(bounds);
    return bounds;
  }

  /**
   * Like {@link #hasTextOffScreen(AccessibilityNodeInfoCompat)}, but text that lies inside {@code
   * listBounds} and under one of {@code barBounds} is covered by a system bar rather than hidden by
   * the app, so scrolling can't reveal it and it doesn't count.
   */
  @VisibleForTesting
  static boolean hasTextOffScreen(
      AccessibilityNodeInfoCompat target, @Nullable Rect listBounds, List<Rect> barBounds) {
    if (hasText(target.getContentDescription())) {
      return false;
    }
    ArrayDeque<AccessibilityNodeInfoCompat> queue = new ArrayDeque<>();
    queue.add(target);
    int checked = 0;
    while (!queue.isEmpty()) {
      AccessibilityNodeInfoCompat node = queue.poll();
      for (int i = 0; i < node.getChildCount(); i++) {
        if (++checked > MAX_OFF_SCREEN_TEXT_NODES) {
          return false;
        }
        AccessibilityNodeInfoCompat child = node.getChild(i);
        if (child == null) {
          continue;
        }
        if (!child.isVisibleToUser()
            && (hasText(child.getText()) || hasText(child.getContentDescription()))
            && !SystemBarCover.isCovered(child, listBounds, barBounds)) {
          return true;
        }
        queue.add(child);
      }
    }
    return false;
  }

  private static boolean hasText(@Nullable CharSequence text) {
    return text != null && TextUtils.getTrimmedLength(text) > 0;
  }

  /**
   * Brings the rest of {@code target}, which focus has just moved to and which is being spoken,
   * on screen. The scroll leaves focus where it is and says nothing.
   */
  private void revealAfterFocus(
      AccessibilityNodeInfoCompat target,
      NavigationAction navigationAction,
      TraversalStrategy traversalStrategy,
      AccessibilityNodeInfoCompat rootNode,
      EventId eventId) {
    long start = SystemClock.uptimeMillis();
    boolean scrolled =
        ensureOnScreen(
            target,
            navigationAction.searchDirection,
            reusableOrderedStrategy(traversalStrategy, rootNode, target),
            eventId);
    // Focus is already on the target; the scroll's events have nothing to search for.
    scrollCallback = null;
    if (scrolled) {
      LogUtils.d(
          TAG,
          "Showed the rest of the focused item after focusing it, in %d ms.",
          SystemClock.uptimeMillis() - start);
    }
  }

  /** Generates the speech and hint for the current focused single link. */
  private void announceSingleLinkFocused(String text, EventId eventId) {
    if (TextUtils.isEmpty(text)) {
      LogUtils.w(TAG, "Empty link text");
      return;
    }
    SpeechController.SpeakOptions speakOptions =
        SpeechController.SpeakOptions.create()
            .setQueueMode(SpeechController.QUEUE_MODE_FLUSH_ALL)
            .setFlags(FeedbackItem.FLAG_FORCE_FEEDBACK);
    pipeline.returnFeedback(
        eventId, ProcessorAccessibilityHints.singleLinkToHint(service, compositor));
    pipeline.returnFeedback(eventId, Feedback.speech(text, speakOptions));
  }

  /**
   * Perform auto-scroll action after finding the target. Sometimes the default target might not be
   * the appropriate target so that it needs to do the extra scroll to find the appropriate target
   * later.
   *
   * <p>Note: For Grid, position-to-scroll action helps to scroll grid page while TalkBack needs
   * handling the focus target additionally.
   */
  private boolean scrollAfterFindTarget(
      AccessibilityNodeInfoCompat pivot,
      AccessibilityNodeInfoCompat target,
      boolean ignoreDescendantsOfPivot,
      NavigationAction navigationAction,
      EventId eventId) {
    if (WebInterfaceUtils.supportsWebActions(pivot)) {
      // Do not scrollAfterFindTarget for web element due to web already handle this.
      LogUtils.v(TAG, "scrollAfterFindTarget returns due to web element");
      return false;
    }
    // Scrolls for GRID traversal if necessary.
    int searchDirection = navigationAction.searchDirection;
    if (navigationAction.targetType == NavigationTarget.TARGET_DEFAULT
        && TraversalStrategyUtils.isLogicalDirection(searchDirection)) {
      AccessibilityNodeInfoCompat grid =
          AccessibilityNodeInfoUtils.getMatchingAncestor(pivot, FILTER_SCROLLABLE_GRID);
      if (grid != null && target != null) {
        int logicalDirection =
            TraversalStrategyUtils.getLogicalDirection(
                searchDirection, WindowUtils.isScreenLayoutRTL(service));
        // For horizontal grids, the logical target may lie outside the current screen and the
        // target provided by the framework may be incorrect. We use GridTraversalManager, which
        // uses the grid's CollectionInfo and the pivot node and target node's CollectionItemInfo to
        // suggest an alternate target. If performing the scroll action failed, fallback to the
        // original searching focus strategy.
        Pair<Integer, Integer> targetPositionForScroll =
            GridTraversalManager.suggestOffScreenTarget(grid, pivot, target, logicalDirection);
        if (targetPositionForScroll != null
            && reachesSuggestedCell(grid, target, targetPositionForScroll, logicalDirection)) {
          // Report 424: Android's file picker reports its one-column file list as a grid. The row
          // after the focused one was a section break with nothing to focus, so the next file was
          // two rows on; scrolling to the empty row ended in silence. A target on screen at or
          // past the suggested cell is the next item.
          LogUtils.d(TAG, "Grid: the target on screen is at or past the suggested cell; not scrolling.");
          targetPositionForScroll = null;
        }
        if (performActionScrollToPosition(
            grid,
            pivot,
            targetPositionForScroll,
            NavigationAction.Builder.copy(navigationAction)
                .setPositionForScroll(targetPositionForScroll)
                .build(),
            eventId)) {
          return true;
        }
      }
    }

    // Scrolls for macro granularity navigation if necessary. Attempts to scroll is made only
    // if ignoreDescendantsOfPivot is set to false to prevent an infinite loop.
    if ((NavigationTarget.isMacroGranularity(navigationAction.targetType))
        && !ignoreDescendantsOfPivot) {
      boolean scrolled =
          scrollForNativeMacroGranularity(
              target, navigationAction, getScrollFilter(navigationAction), eventId);
      if (scrolled) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns whether {@code node} is a WebView that navigation goes into. Moving by element first
   * finds what comes before a WebView that is still off screen.
   */
  private static boolean isWebViewToEnter(
      AccessibilityNodeInfoCompat node, NavigationAction navigationAction) {
    return Role.getRole(node) == Role.ROLE_WEB_VIEW
        && WebInterfaceUtils.supportsWebActions(node)
        && (navigationAction.targetType == NavigationTarget.TARGET_DEFAULT
            || AccessibilityNodeInfoUtils.isVisible(node));
  }

  /**
   * If the pivot is native view, searches for the middlePivot node first and then determines to
   * navigate to native or web element.
   */
  private @NonNull NavigationResult findTargetFromNativeElement(
      AccessibilityNodeInfoCompat nativePivot,
      NavigationAction navigationAction,
      Filter<AccessibilityNodeInfoCompat> nodeFilter,
      TraversalStrategy traversalStrategy,
      EventId eventId) {
    // Filter to find the native node with target type or WebView, returns WebView if find it first.
    Filter<AccessibilityNodeInfoCompat> nodeFilterOrWebView =
        new Filter<AccessibilityNodeInfoCompat>() {
          @Override
          public boolean accept(AccessibilityNodeInfoCompat node) {
            return isWebViewToEnter(node, navigationAction);
          }
        }.or(nodeFilter);

    AccessibilityNodeInfoCompat middlePivot =
        TraversalStrategyUtils.searchFocus(
            traversalStrategy, nativePivot, navigationAction.searchDirection, nodeFilterOrWebView);
    // A view that recognised screen elements cover, such as a game's view or a WebView with only a
    // canvas, is passed over for its elements, before a WebView would take over navigation.
    if (middlePivot != null
        && navigationAction.targetType == NavigationTarget.TARGET_DEFAULT
        && ScreenElements.isShowing()
        && ScreenElements.isCoveredHost(middlePivot)) {
      if (ScreenElements.getNeedsOrder()) {
        orderScreenElements();
      }
      boolean forward =
          TraversalStrategyUtils.getLogicalDirection(
                  navigationAction.searchDirection, WindowUtils.isScreenLayoutRTL(service))
              == TraversalStrategy.SEARCH_FOCUS_FORWARD;
      @Nullable AccessibilityNodeInfoCompat element =
          forward ? ScreenElements.firstAfter(middlePivot) : ScreenElements.lastBefore(middlePivot);
      if (element != null) {
        return NavigationResult.create(element);
      }
    }
    return findTargetFromMiddlePivot(
        middlePivot, navigationAction, nodeFilter, traversalStrategy, eventId);
  }

  /**
   * Returns the valid next/previous link index from the pivot if the pivot is the current focused
   * node containing the focused link. Otherwise returns -1.
   */
  private int getLinkIndexFromPivot(
      AccessibilityNodeInfoCompat nativePivot, NavigationAction navigationAction) {
    if (!supportClickableLinks(navigationAction.targetType) || reachEdge) {
      return FocusActionInfo.LINK_INDEX_NOT_SET;
    }
    int linkSpanCount = LinkUtils.getLinkSpansInNodeGroup(nativePivot).size();
    if (linkSpanCount == 0) {
      return FocusActionInfo.LINK_INDEX_NOT_SET;
    }
    int logicalDirection =
        TraversalStrategyUtils.getLogicalDirection(
            navigationAction.searchDirection, WindowUtils.isScreenLayoutRTL(service));
    FocusActionRecord record = actorState.getFocusHistory().getLastFocusActionRecord();
    int oldIndex = record.getExtraInfo().getFocusedLinkIndex();
    int newIndex = -1;
    if (oldIndex >= 0 && nativePivot.equals(record.getFocusedNode())) {
      if (logicalDirection == TraversalStrategy.SEARCH_FOCUS_FORWARD) {
        newIndex = oldIndex + 1;
      } else {
        newIndex = oldIndex - 1;
      }
    }
    if (newIndex >= 0 && newIndex < linkSpanCount) {
      LogUtils.v(TAG, "getLinkIndexFromPivot=" + newIndex);
      return newIndex;
    }
    return FocusActionInfo.LINK_INDEX_NOT_SET;
  }

  /**
   * Returns the valid previous/next initial link index from the target which is going to gain the
   * focus. Otherwise returns -1.
   */
  private int getLinkIndexFromTarget(
      AccessibilityNodeInfoCompat nativeTarget, NavigationAction navigationAction) {
    if (!supportClickableLinks(navigationAction.targetType)) {
      return FocusActionInfo.LINK_INDEX_NOT_SET;
    }
    int linkSpanCount = LinkUtils.getLinkSpansInNodeGroup(nativeTarget).size();
    if (linkSpanCount == 0) {
      return FocusActionInfo.LINK_INDEX_NOT_SET;
    }
    int logicalDirection =
        TraversalStrategyUtils.getLogicalDirection(
            navigationAction.searchDirection, WindowUtils.isScreenLayoutRTL(service));
    int newIndex;
    if (logicalDirection == TraversalStrategy.SEARCH_FOCUS_FORWARD) {
      newIndex = 0;
    } else {
      newIndex = linkSpanCount - 1;
    }
    LogUtils.v(TAG, "getLinkIndexFromTarget=" + newIndex);
    return newIndex;
  }

  /**
   * Returns the next/previous link index for focus from the pivot or the target. Otherwise returns
   * -1.
   */
  private int getLinkIndexForFocus(
      AccessibilityNodeInfoCompat pivot,
      NavigationResult navigationResult,
      NavigationAction navigationAction) {
    int linkIndex;
    if (navigationResult.getLinkIndex() >= 0 && pivot.equals(navigationResult.getNode())) {
      linkIndex = navigationResult.getLinkIndex();
    } else {
      linkIndex = getLinkIndexFromTarget(navigationResult.getNode(), navigationAction);
    }
    return linkIndex;
  }

  private boolean supportClickableLinks(int targetType) {
    return globalVariables.supportClickableLinks()
        && (targetType == NavigationTarget.TARGET_LINK
            || targetType == NavigationTarget.TARGET_HTML_ELEMENT_UNVISITED_LINK);
  }

  /**
   * The middle-pivot must be WebView container or native element. If it is WebView container,
   * navigates to WebView container inside. Otherwise returns the native element.
   */
  private @NonNull NavigationResult findTargetFromMiddlePivot(
      AccessibilityNodeInfoCompat middlePivot,
      NavigationAction navigationAction,
      Filter<AccessibilityNodeInfoCompat> nodeFilter,
      TraversalStrategy traversalStrategy,
      EventId eventId) {
    if (middlePivot == null) {
      return NavigationResult.create(NavigationResult.Type.EMPTY);
    }
    if (!WebInterfaceUtils.supportsWebActions(middlePivot)) {
      // Middle-pivot is the native element and should be the target.
      return NavigationResult.create(middlePivot);
    }

    // Middle-pivot must be WebView container
    if (Role.getRole(middlePivot) != Role.ROLE_WEB_VIEW) {
      throw new IllegalArgumentException(
          "Middle-pivot must be either native or WebView container!");
    }
    if (navigationAction.targetType == NavigationTarget.TARGET_DEFAULT) {
      int searchDirection = navigationAction.searchDirection;
      int logicalDirection =
          TraversalStrategyUtils.getLogicalDirection(
              searchDirection, WindowUtils.isScreenLayoutRTL(service));
      if (logicalDirection == TraversalStrategy.SEARCH_FOCUS_BACKWARD) {
        // If the default previous native element is the WebView container,
        // navigate to html directly to find the last element in the WebView.
        return navigateToHtmlTargetWithFallBack(
            middlePivot, navigationAction, nodeFilter, traversalStrategy, eventId);
      } else {
        // If the default next native element is the WebView container,
        // returns the WebView container directly.
        return NavigationResult.create(middlePivot);
      }
    } else {
      return findTargetFromWebElement(
          middlePivot, navigationAction, nodeFilter, traversalStrategy, eventId);
    }
  }

  /**
   * If the pivot is WebView container to find the default previous node, it should looks for the
   * previous native element. For other web pivot, navigate to web elements.
   */
  private @NonNull NavigationResult findTargetFromWebElement(
      AccessibilityNodeInfoCompat webPivot,
      NavigationAction navigationAction,
      Filter<AccessibilityNodeInfoCompat> nodeFilter,
      TraversalStrategy traversalStrategy,
      EventId eventId) {
    boolean isNodeFromClank =
        FeatureFlagReader.enableNewJumpNavigationForClank(service)
            && webPivot != null
            && webPivot.getPackageName() != null
            && webPivot.getPackageName().toString().equals(CLANK_PACKAGE_NAME);
    // If a JUMP_TO_BOTTOM action is being performed inside Clank container, let it traverse the
    // WebView so it can focus the last HTML item.
    boolean allowClankWebViewTraversal =
        isNodeFromClank && navigationAction.actionType == NavigationAction.JUMP_TO_BOTTOM;

    int searchDirection = navigationAction.searchDirection;
    int logicalDirection =
        TraversalStrategyUtils.getLogicalDirection(
            searchDirection, WindowUtils.isScreenLayoutRTL(service));
    // Prevent navigating to html target if the pivot is WebView container to find the default
    // previous node because it will find the wrong last item in the WebView.
    if (!allowClankWebViewTraversal
        && (Role.getRole(webPivot) == Role.ROLE_WEB_VIEW)
        && (navigationAction.targetType == NavigationTarget.TARGET_DEFAULT)
        && (logicalDirection == TraversalStrategy.SEARCH_FOCUS_BACKWARD)) {
      AccessibilityNodeInfoCompat target =
          TraversalStrategyUtils.searchFocus(
              traversalStrategy, webPivot, searchDirection, nodeFilter);
      return NavigationResult.create(target);
    }
    return navigateToHtmlTargetWithFallBack(
        webPivot, navigationAction, nodeFilter, traversalStrategy, eventId);
  }

  /**
   * Finds the next or previous web element. If it fails, jump out of the WebView container to find
   * the next or previous native element as the fallback.
   */
  private NavigationResult navigateToHtmlTargetWithFallBack(
      AccessibilityNodeInfoCompat webPivot,
      NavigationAction navigationAction,
      Filter<AccessibilityNodeInfoCompat> nodeFilter,
      TraversalStrategy traversalStrategy,
      EventId eventId) {
    if (navigateToHtmlTargetExpectingFocus(webPivot, navigationAction, eventId)) {
      return NavigationResult.create(NavigationResult.Type.WEB_ELEMENT);
    }
    return findTargetOutsideWebView(webPivot, navigationAction, nodeFilter, traversalStrategy);
  }

  /**
   * Asks the web page to move focus from {@code webPivot}, as {@link #navigateToHtmlTarget} does,
   * and when it moves to the next or previous item, has its focus event checked for leaving a row.
   * The next or previous item is taken from the page's node tree instead where that is clear:
   * Chromium's own search can skip items, or find none, in a page that was just loaded.
   */
  private boolean navigateToHtmlTargetExpectingFocus(
      AccessibilityNodeInfoCompat webPivot, NavigationAction navigationAction, EventId eventId) {
    if (isMoveToNextOrPreviousItem(navigationAction)) {
      int logicalDirection =
          TraversalStrategyUtils.getLogicalDirection(
              navigationAction.searchDirection, WindowUtils.isScreenLayoutRTL(service));
      long searchStart = SystemClock.uptimeMillis();
      @Nullable AccessibilityNodeInfoCompat target =
          WebTreeOrder.find(
              webPivot, logicalDirection == TraversalStrategy.SEARCH_FOCUS_FORWARD);
      LogUtils.d(
          TAG,
          "Web node tree searched in %d ms, %s",
          SystemClock.uptimeMillis() - searchStart,
          (target == null) ? "asking the page" : "found");
      if (target != null
          && setAccessibilityFocusInternal(
              target,
              navigationAction,
              /* linkIndex= */ -1,
              columnWrap(webPivot, target, navigationAction, logicalDirection),
              eventId)) {
        LogUtils.d(TAG, "Web item from the node tree: %s", target);
        return true;
      }
    }
    if (!navigateToHtmlTarget(webPivot, navigationAction, eventId)) {
      return false;
    }
    if (isMoveToNextOrPreviousItem(navigationAction)) {
      // The page moves the focus, so its focus event shows where it went.
      boolean rtl = WindowUtils.isScreenLayoutRTL(service);
      ColumnWrap.expectWebFocus(
          webPivot,
          TraversalStrategyUtils.getLogicalDirection(navigationAction.searchDirection, rtl)
              == TraversalStrategy.SEARCH_FOCUS_FORWARD,
          rtl);
    }
    return true;
  }

  /** Finds the node next to the WebView that holds {@code webPivot}, in the window's order. */
  private NavigationResult findTargetOutsideWebView(
      AccessibilityNodeInfoCompat webPivot,
      NavigationAction navigationAction,
      Filter<AccessibilityNodeInfoCompat> nodeFilter,
      TraversalStrategy traversalStrategy) {
    AccessibilityNodeInfoCompat webContainer = webPivot;
    if (Role.getRole(webPivot) != Role.ROLE_WEB_VIEW) {
      webContainer = WebInterfaceUtils.ascendToWebView(webPivot);
    }
    if (webContainer == null) {
      return NavigationResult.create(NavigationResult.Type.EMPTY);
    }
    AccessibilityNodeInfoCompat target =
        TraversalStrategyUtils.searchFocus(
            traversalStrategy, webContainer, navigationAction.searchDirection, nodeFilter);
    return NavigationResult.create(target);
  }

  /**
   * Optimize the traversal order of AutoCompleteTextView suggestions to make the suggestions
   * navigable immediately after AutoCompleteTextView.
   */
  private @NonNull NavigationResult findTargetForEditTextSuggestions(
      AccessibilityNodeInfoCompat pivot,
      NavigationAction navigationAction,
      Filter<AccessibilityNodeInfoCompat> nodeFilter,
      boolean isNavigationResultEmpty) {
    TraversalStrategy traversalStrategy = null;
    AccessibilityNodeInfoCompat rootNode = null;
    AccessibilityNodeInfoCompat target = null;
    int searchDirection = navigationAction.searchDirection;
    int logicalDirection =
        TraversalStrategyUtils.getLogicalDirection(
            searchDirection, WindowUtils.isScreenLayoutRTL(service));
    AccessibilityWindowInfo currentWindow = AccessibilityNodeInfoUtils.getWindow(pivot.unwrap());
    AccessibilityNodeInfoCompat anchorNode = AccessibilityWindowInfoUtils.getAnchor(currentWindow);
    if (navigationAction.targetType == NavigationTarget.TARGET_DEFAULT
        && logicalDirection == TraversalStrategy.SEARCH_FOCUS_FORWARD
        && Role.getRole(pivot) == Role.ROLE_EDIT_TEXT) {
      // Set the target to the initial focus node of the anchored window.
      AccessibilityWindowInfo anchoredWindow =
          AccessibilityWindowInfoUtils.getAnchoredWindow(pivot.unwrap());
      if (anchoredWindow != null) {
        rootNode = AccessibilityWindowInfoUtils.getRootCompat(anchoredWindow);
        traversalStrategy = createTraversal(rootNode, searchDirection);
        target =
            TraversalStrategyUtils.findFirstFocusInNodeTree(
                traversalStrategy, rootNode, searchDirection, nodeFilter);
      }
    } else if (navigationAction.targetType == NavigationTarget.TARGET_DEFAULT
        && isNavigationResultEmpty
        && anchorNode != null
        && Role.getRole(anchorNode) == Role.ROLE_EDIT_TEXT) {
      // Return talkback-focus to the parent-window.
      if (logicalDirection == TraversalStrategy.SEARCH_FOCUS_BACKWARD) {
        // Set the target back to the anchor node.
        target = AccessibilityWindowInfoUtils.getAnchor(currentWindow);
      } else if (logicalDirection == TraversalStrategy.SEARCH_FOCUS_FORWARD) {
        // Set the target to the next traversal node of the anchor node.
        rootNode = AccessibilityNodeInfoUtils.getRoot(anchorNode);
        if (rootNode != null) {
          traversalStrategy = createTraversal(rootNode, searchDirection);
          target =
              TraversalStrategyUtils.searchFocus(
                  traversalStrategy, anchorNode, searchDirection, nodeFilter);
        }
      }
    }
    return NavigationResult.create(target);
  }

  /**
   * The picture-in-picture window that {@code node} is in, or whose menu it is in; null when
   * neither.
   */
  private @Nullable AccessibilityWindowInfo pictureInPictureOf(AccessibilityNodeInfoCompat node) {
    @Nullable AccessibilityWindowInfo window = AccessibilityNodeInfoUtils.getWindow(node.unwrap());
    if (window == null) {
      return null;
    }
    if (AccessibilityWindowInfoUtils.isPictureInPicture(window)) {
      return window;
    }
    if (AccessibilityWindowInfoUtils.getType(window) != AccessibilityWindowInfoCompat.TYPE_SYSTEM) {
      return null;
    }
    return PictureInPictureItem.pipWindowFor(
        AccessibilityServiceCompatUtils.getWindows(service), window.getId());
  }

  /**
   * Moves from the picture-in-picture item, or from a control in it that touch focused, to the
   * window before or after it: the window and its menu are one item.
   */
  private boolean leavePictureInPicture(
      AccessibilityNodeInfoCompat pivot, NavigationAction navigationAction, EventId eventId) {
    NavigationResult result = findTargetAcrossWindows(pivot, navigationAction, eventId);
    if (result.shouldSkipNavigation()) {
      return result.isFocusAvailable();
    }
    @Nullable AccessibilityNodeInfoCompat target = result.getNode();
    if (target == null) {
      LogUtils.d(TAG, "No window to move to from picture-in-picture");
      return false;
    }
    return setAccessibilityFocusInternal(target, navigationAction, eventId);
  }

  /**
   * Finds the target node in the next or previous window, and the target window is determined by
   * {@code DirectionalNavigationWindowFilter}.
   *
   * <p>It assumes there is no WebView across windows and always find the native elements first.
   */
  private @NonNull NavigationResult findTargetAcrossWindows(
      AccessibilityNodeInfoCompat pivot, NavigationAction navigationAction, EventId eventId) {
    WindowTraversal windowTraversal = new WindowTraversal(service);
    boolean isScreenRtl = WindowUtils.isScreenLayoutRTL(service);
    List<AccessibilityWindowInfo> windows = AccessibilityServiceCompatUtils.getWindows(service);
    // The picture-in-picture menu and other windows over a picture-in-picture window are part of
    // its one item.
    int searchDirection = navigationAction.searchDirection;
    int logicalDirection = TraversalStrategyUtils.getLogicalDirection(searchDirection, isScreenRtl);
    @Nullable AccessibilityWindowInfo currentWindow =
        AccessibilityNodeInfoUtils.getWindow(pivot.unwrap());
    DirectionalNavigationWindowFilter windowFilter =
        new DirectionalNavigationWindowFilter(
            service,
            searchState,
            PictureInPictureItem.overlayWindowIds(windows),
            swipesIncludeKeyboard(currentWindow));
    if (currentWindow != null
        && AccessibilityWindowInfoUtils.getType(currentWindow)
            == AccessibilityWindowInfoCompat.TYPE_SYSTEM) {
      @Nullable AccessibilityWindowInfo pip =
          PictureInPictureItem.pipWindowFor(windows, currentWindow.getId());
      if (pip != null) {
        currentWindow = pip;
      }
    }

    if (currentWindow == null) {
      // Ideally currentWindow should never be null. Do the null check in case of exception.
      LogUtils.w(TAG, "Cannot navigate across window: unable to identify current window");
      return NavigationResult.create(NavigationResult.Type.EXCEPTION);
    }

    // Swipes don't go into the navigation bar or the status bar, so focus there, after Back or a
    // touch, goes on into the app at the bar's end instead of around the bar, when set to.
    if (leaveSystemBars
        && (logicalDirection == TraversalStrategy.SEARCH_FOCUS_FORWARD
            || logicalDirection == TraversalStrategy.SEARCH_FOCUS_BACKWARD)
        && !windowFilter.accept(currentWindow)
        && WindowUtils.isSystemBar(service, currentWindow)) {
      return findTargetLeavingSystemBar(
          pivot,
          currentWindow,
          windowTraversal,
          windowFilter,
          navigationAction,
          logicalDirection,
          eventId);
    }

    // Skip one swipe if it's the last element in the last window. Without wrapping, stop there on every swipe, rather than going on to the first window.
    if ((!reachEdge || !wrapAround)
        && (!windowFilter.accept(currentWindow)
            || needPauseWhenTraverseAcrossWindow(
                windowTraversal, isScreenRtl, currentWindow, searchDirection, windowFilter))) {
      reachEdge = true;
      // Otherwise this swipe goes on as the next one would, around to the first window.
      if (stopsAtEdge()) {
        stoppedAtEdge = !wrapAround;
        logEdge(pivot, navigationAction);
        announceNativeElement(logicalDirection, navigationAction.targetType, eventId);
        LogUtils.v(TAG, "Reach edge before searchTargetInNextOrPreviousWindow in:" + currentWindow);
        return NavigationResult.create(NavigationResult.Type.REACH_EDGE);
      }
      // This swipe wraps around at once. Log the edge as a stop does, with what auto-scroll did.
      logEdge(pivot, navigationAction);
    }

    AccessibilityNodeInfoCompat target = null;
    if (windowFilter.accept(currentWindow)) {
      // By default, when navigating across windows, the focus is placed on the first/last element
      // of the new window. However, if transitioning from an IME window, it may cause many
      // exceptions if there are many items between the editing node and the last element in the
      // window. So we'll prefer to use the editing node when performing backward navigation cross
      // windows.
      if (logicalDirection == TraversalStrategy.SEARCH_FOCUS_BACKWARD
          && AccessibilityWindowInfoUtils.isImeWindow(currentWindow)) {
        target = accessibilityFocusMonitor.getInputFocus();
      }

      if (!AccessibilityNodeInfoUtils.shouldFocusNode(target)) {
        boolean reachEdgeBeforeSearch = reachEdge;
        Map<AccessibilityNodeInfoCompat, Boolean> speakingNodesCache = new HashMap<>();
        target =
            searchTargetInNextOrPreviousWindow(
                screenState.getStableScreenState(),
                windowTraversal,
                isScreenRtl,
                pivot,
                currentWindow,
                searchDirection,
                focusFinder,
                /* isInitialFocus= */ false,
                windowFilter,
                NavigationTarget.createNodeFilter(navigationAction.targetType, speakingNodesCache));
        if (reachEdgeBeforeSearch != reachEdge && stopsAtEdge()) {
          // Skip one swipe if reaching edge while searching windows in loop.
          stoppedAtEdge = !wrapAround;
          logEdge(pivot, navigationAction);
          announceNativeElement(logicalDirection, navigationAction.targetType, eventId);
          return NavigationResult.create(NavigationResult.Type.REACH_EDGE);
        }
      }
    }
    return NavigationResult.create(target);
  }

  /**
   * Moves on from the first or last item of the navigation bar or the status bar into the app. The
   * status bar comes before the app and the navigation bar after it, so going forward from the
   * navigation bar or back from the status bar passes the end of the screen, where Wrap around and
   * Stop at the last item before wrapping apply as at the last window.
   */
  private @NonNull NavigationResult findTargetLeavingSystemBar(
      AccessibilityNodeInfoCompat pivot,
      AccessibilityWindowInfo barWindow,
      WindowTraversal windowTraversal,
      Filter<AccessibilityWindowInfo> windowFilter,
      NavigationAction navigationAction,
      @SearchDirection int logicalDirection,
      EventId eventId) {
    boolean forward = logicalDirection == TraversalStrategy.SEARCH_FOCUS_FORWARD;
    boolean pastScreenEnd = forward != WindowUtils.isStatusBar(service, barWindow);
    if (stopsLeavingSystemBar(pastScreenEnd, navigationAction.shouldWrap)) {
      logEdge(pivot, navigationAction);
      announceNativeElement(logicalDirection, navigationAction.targetType, eventId);
      return NavigationResult.create(NavigationResult.Type.REACH_EDGE);
    }
    List<AccessibilityWindowInfo> appWindows =
        appWindowsNextToSystemBar(
            AccessibilityServiceCompatUtils.getWindows(service),
            windowTraversal,
            windowFilter,
            barWindow,
            forward);
    for (AccessibilityWindowInfo window : appWindows) {
      @Nullable AccessibilityNodeInfoCompat root = AccessibilityWindowInfoUtils.getRootCompat(window);
      if (root == null) {
        continue;
      }
      TraversalStrategy strategy =
          TraversalStrategyUtils.getTraversalStrategy(
              root, focusFinder, navigationAction.searchDirection);
      NavigationResult result =
          findTargetForWrapAround(root, navigationAction, strategy, eventId);
      if (result.isEmpty()) {
        continue;
      }
      if (result.isWebElement()) {
        // The page moves focus itself; the edge was passed.
        reachEdge = false;
      }
      LogUtils.d(TAG, "Leaving a system bar into window %d", window.getId());
      return result;
    }
    // Nothing to focus outside the bar: stop at its end, or wrap around inside it, as at the last
    // window.
    if (!pastScreenEnd
        && stopsLeavingSystemBar(/* pastScreenEnd= */ true, navigationAction.shouldWrap)) {
      logEdge(pivot, navigationAction);
      announceNativeElement(logicalDirection, navigationAction.targetType, eventId);
      return NavigationResult.create(NavigationResult.Type.REACH_EDGE);
    }
    return NavigationResult.create(NavigationResult.Type.EMPTY);
  }

  /**
   * Whether a swipe leaving a system bar stops at the bar's last or first item. Only a swipe that
   * passes the end of the screen ({@code pastScreenEnd}) stops: always with Wrap around off or
   * for a navigation that doesn't wrap, and once before going on with Stop at the last item before
   * wrapping on.
   */
  @VisibleForTesting
  boolean stopsLeavingSystemBar(boolean pastScreenEnd, boolean shouldWrap) {
    if (!pastScreenEnd) {
      return false;
    }
    if (reachEdge && wrapAround && shouldWrap) {
      // The swipe after the one that stopped.
      return false;
    }
    reachEdge = true;
    if (stopsAtEdge() || !shouldWrap) {
      stoppedAtEdge = !wrapAround;
      return true;
    }
    return false;
  }

  /**
   * The windows a swipe leaving the system bar {@code barWindow} tries, in order: the next window
   * swipes go to in the direction of the swipe, such as the keyboard above the navigation bar, then
   * the app window with input focus and the active app window.
   */
  @VisibleForTesting
  static List<AccessibilityWindowInfo> appWindowsNextToSystemBar(
      List<AccessibilityWindowInfo> windows,
      WindowTraversal windowTraversal,
      Filter<AccessibilityWindowInfo> windowFilter,
      AccessibilityWindowInfo barWindow,
      boolean forward) {
    List<AccessibilityWindowInfo> result = new ArrayList<>();
    @Nullable AccessibilityWindowInfo next = barWindow;
    for (int i = 0; i < windows.size(); i++) {
      next =
          forward ? windowTraversal.getNextWindow(next) : windowTraversal.getPreviousWindow(next);
      if (next == null || next.equals(barWindow)) {
        break;
      }
      if (windowFilter.accept(next)) {
        result.add(next);
        break;
      }
    }
    for (AccessibilityWindowInfo window : windows) {
      if (window.isFocused()
          && !result.contains(window)
          && isAppWindowOutsideBar(window, windowFilter, barWindow)) {
        result.add(window);
        break;
      }
    }
    for (AccessibilityWindowInfo window : windows) {
      if (window.isActive()
          && !result.contains(window)
          && isAppWindowOutsideBar(window, windowFilter, barWindow)) {
        result.add(window);
        break;
      }
    }
    return result;
  }

  private static boolean isAppWindowOutsideBar(
      AccessibilityWindowInfo window,
      Filter<AccessibilityWindowInfo> windowFilter,
      AccessibilityWindowInfo barWindow) {
    return !window.equals(barWindow)
        && AccessibilityWindowInfoUtils.getType(window)
            == AccessibilityWindowInfoCompat.TYPE_APPLICATION
        && windowFilter.accept(window);
  }

  /**
   * Try to wrap around inside current window, which is equivalent to find the first native or web
   * element in the current window with the given search direction.
   */
  private NavigationResult findTargetForWrapAround(
      AccessibilityNodeInfoCompat root,
      NavigationAction navigationAction,
      TraversalStrategy traversalStrategy,
      EventId eventId) {
    Filter<AccessibilityNodeInfoCompat> nodeFilter =
        NavigationTarget.createNodeFilter(
            navigationAction.targetType, traversalStrategy.getSpeakingNodesCache());
    // Filter to find the native node with target type or WebView, returns WebView if find it first.
    Filter<AccessibilityNodeInfoCompat> nodeFilterOrWebView =
        new Filter<AccessibilityNodeInfoCompat>() {
          @Override
          public boolean accept(AccessibilityNodeInfoCompat node) {
            return isWebViewToEnter(node, navigationAction);
          }
        }.or(nodeFilter);

    AccessibilityNodeInfoCompat middlePivot =
        TraversalStrategyUtils.findFirstFocusInNodeTree(
            traversalStrategy, root, navigationAction.searchDirection, nodeFilterOrWebView);
    return findTargetFromMiddlePivot(
        middlePivot, navigationAction, nodeFilter, traversalStrategy, eventId);
  }

  /**
   * When searching the next focus, potentially consider the result of View.focusSearch() which
   * defines the next input focus in the given direction in the absence of an accessibility service.
   * If only one of the TalkBack target and the focusSearch target is accessibility-focusable, or
   * exists in the first place, prefer that one. If only one of them is input-focusable (or
   * enabled), prefer the one that is not. If both are input-focusable (and enabled), prefer the
   * focusSearch target.
   */
  private NavigationResult findTargetForDirectionalTraveral(
      AccessibilityNodeInfoCompat pivot, AccessibilityNodeInfoCompat target, int searchDirection) {
    int focusDirection =
        TraversalStrategyUtils.nodeSearchDirectionToViewSearchDirection(searchDirection);
    AccessibilityNodeInfoCompat focusSearchTarget = pivot.focusSearch(focusDirection);
    // Potentially allow the currently focused node to keep focus even if not
    // accessibility-focusable.
    // The reason is that per default it is marked as not accessibility-focusable if it has focus.
    if ((focusSearchTarget != null)
        && focusSearchTarget.isAccessibilityFocused()
        && allowFocusResting(target)) {
      LogUtils.d(TAG, "Using focusSearch() target which is the already focused node.");
      return NavigationResult.create(NavigationResult.Type.EXISTING_FOCUS);
    }
    if ((focusSearchTarget != null)
        && !focusSearchTarget.equals(target)
        && (!focusSearchTarget.isFocusable() || (target == null) || target.isFocusable())
        && (!focusSearchTarget.isEnabled() || (target == null) || target.isEnabled())
        && AccessibilityNodeInfoUtils.shouldFocusNode(focusSearchTarget)
        && AccessibilityNodeInfoUtils.supportsAction(
            focusSearchTarget, AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS)) {
      LogUtils.d(
          TAG,
          (target == null)
              ? "Using focusSearch() target because TalkBack navigation target was null."
              : "Using focusSearch() target instead of TalkBack navigation target.");
      target = focusSearchTarget;
    }
    return NavigationResult.create(target);
  }

  private boolean allowFocusResting(@Nullable AccessibilityNodeInfoCompat talkBackTarget) {
    if (!FeatureFlagReader.allowFocusResting(/* context= */ service)) {
      return false;
    }
    if (talkBackTarget == null) {
      return true;
    }
    return talkBackTarget.isFocusable() && talkBackTarget.isEnabled();
  }

  /**
   * Whether {@code target}, found by the search, is on screen in {@code grid} and in a one-column
   * grid, or at or past the {@code suggested} cell in {@code logicalDirection}: then it is the next
   * item, and scrolling to the suggested cell would only show an empty or passed one.
   */
  private static boolean reachesSuggestedCell(
      AccessibilityNodeInfoCompat grid,
      AccessibilityNodeInfoCompat target,
      Pair<Integer, Integer> suggested,
      @SearchDirection int logicalDirection) {
    CollectionItemInfoCompat item = target.getCollectionItemInfo();
    CollectionInfoCompat collection = grid.getCollectionInfo();
    if (item == null || collection == null || !target.isVisibleToUser()) {
      return false;
    }
    if (collection.getColumnCount() == 1) {
      return true;
    }
    return isAtOrPastCell(
        item.getRowIndex(),
        item.getColumnIndex(),
        suggested.first,
        suggested.second,
        logicalDirection == SEARCH_FOCUS_FORWARD);
  }

  /** Whether the cell at {@code row}, {@code column} is at or past the suggested one going {@code forward} or back. */
  @VisibleForTesting
  static boolean isAtOrPastCell(
      int row, int column, int suggestedRow, int suggestedColumn, boolean forward) {
    if (row != suggestedRow) {
      return forward ? row > suggestedRow : row < suggestedRow;
    }
    return forward ? column >= suggestedColumn : column <= suggestedColumn;
  }

  private boolean performActionScrollToPosition(
      AccessibilityNodeInfoCompat nodeInfo,
      AccessibilityNodeInfoCompat pivot,
      @Nullable Pair<Integer, Integer> targetPosition,
      NavigationAction navigationAction,
      EventId eventId) {
    if (targetPosition == null) {
      return false;
    }
    long autoScrollUptimeBefore = lastAutoScrollUptime;
    scrollCallback = new AutoScrollCallback(this, navigationAction, pivot);
    Bundle arguments = new Bundle();
    arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_ROW_INT, targetPosition.first);
    arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_COLUMN_INT, targetPosition.second);
    boolean scrolled =
        pipeline.returnFeedback(
            eventId,
            Feedback.scrollToPosition(
                nodeInfo,
                ScrollActionRecord.ACTION_AUTO_SCROLL,
                AccessibilityActionCompat.ACTION_SCROLL_TO_POSITION.getId(),
                arguments,
                ScrollActionRecord.FOCUS,
                ScrollTimeout.SCROLL_TIMEOUT_LONG,
                navigationAction.autoScrollAttempt));
    if (!scrolled) {
      dropFailedScrollCallback(autoScrollUptimeBefore);
    }
    return scrolled;
  }

  /**
   * Forgets the callback of a scroll action that the app refused. No scroll event or timeout
   * comes for it, so it would hold the next swipes back for {@link #MAX_DEFER_MS}.
   */
  private void dropFailedScrollCallback(long autoScrollUptimeBefore) {
    scrollCallback = null;
    trackedCallback = null;
    lastAutoScrollUptime = autoScrollUptimeBefore;
    scrollWindowId = -1;
  }

  /** Returns {@code true} if current window is the last window on screen in traversal order. */
  private boolean needPauseWhenTraverseAcrossWindow(
      WindowTraversal windowTraversal,
      boolean isScreenRtl,
      AccessibilityWindowInfo currentWindow,
      @SearchDirection int searchDirection,
      Filter<AccessibilityWindowInfo> windowFilter) {
    @TraversalStrategy.SearchDirection
    int logicalDirection = TraversalStrategyUtils.getLogicalDirection(searchDirection, isScreenRtl);
    if (logicalDirection == TraversalStrategy.SEARCH_FOCUS_FORWARD) {
      return windowTraversal.isLastWindow(currentWindow, windowFilter);
    } else if (logicalDirection == TraversalStrategy.SEARCH_FOCUS_BACKWARD) {
      return windowTraversal.isFirstWindow(currentWindow, windowFilter);
    } else {
      throw new IllegalStateException("Unknown logical direction");
    }
  }

  private boolean scrollForNativeMacroGranularity(
      AccessibilityNodeInfoCompat target,
      NavigationAction navigationAction,
      Filter<AccessibilityNodeInfoCompat> filterScrollable,
      EventId eventId) {

    AccessibilityNodeInfoCompat referenceNode = null;
    AccessibilityNodeInfoCompat focusedOrReferenceNodeParent = null;
    AccessibilityNodeInfoCompat a11yFocusedNode =
        accessibilityFocusMonitor.getAccessibilityFocus(
            navigationAction.useInputFocusAsPivotIfEmpty);
    boolean hasValidAccessibilityFocus =
        (a11yFocusedNode != null) && AccessibilityNodeInfoUtils.isVisible(a11yFocusedNode);

    if (hasValidAccessibilityFocus) {
      referenceNode = a11yFocusedNode;
      focusedOrReferenceNodeParent =
          AccessibilityNodeInfoUtils.getMatchingAncestor(a11yFocusedNode, filterScrollable);
    } else {
      // If a11y focus is not valid, we try to get the first child of the last scrolled container
      // to keep as a reference for scrolling. A visibility check is not required as it is just a
      // reference to start the scroll.
      referenceNode =
          refreshAndGetFirstOrLastChild(
              lastScrolledNodeForNativeMacroGranularity, /* firstChild= */ true);
      focusedOrReferenceNodeParent = lastScrolledNodeForNativeMacroGranularity;
    }

    // If we are navigating within a scrollable container with native macro granularity, we want
    // to make sure we have traversed the scrollable list at least once by auto-scroll before
    // jumping to an element that is on screen but out of the scrollable container. If the target
    // inside the scrollable view not found, it would fallback to use the target outside of the
    // scrollable container.
    if ((focusedOrReferenceNodeParent != null)
        && (target != null)
        && !AccessibilityNodeInfoUtils.hasAncestor(target, focusedOrReferenceNodeParent)) {
      navigationAction =
          NavigationAction.Builder.copy(navigationAction).setFallbackTarget(target).build();
      target = null;
    }

    // If we find no target on screen for native macro granularity, we do our best attempt to
    // scroll to the next screen and place the focus on the new screen if it exists.
    if (target == null && referenceNode != null) {
      ScrollableNodeInfo scrollableNodeInfo =
          ScrollableNodeInfo.findScrollableNodeForDirection(
              navigationAction.searchDirection,
              referenceNode,
              /* includeSelf= */ true,
              /* rtl= */ WindowUtils.isScreenLayoutRTL(service));
      if (scrollableNodeInfo != null
          && autoScroll(scrollableNodeInfo, referenceNode, navigationAction, eventId)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the first or the last child of a refreshed scrollable node.
   *
   * @param node The parent node whose first or last child is returned.
   * @param firstChild If {@code true} indicates first child, else last child.
   * @return First or last child of the {@code node}
   */
  private static @Nullable AccessibilityNodeInfoCompat refreshAndGetFirstOrLastChild(
      @Nullable AccessibilityNodeInfoCompat node, boolean firstChild) {
    // In this condition, we should successfully refresh the scrollable node to ensure that it
    // doesn't contain stale children.
    if (node != null && node.refresh() && node.getChildCount() > 0) {
      int childNumber = 0;
      if (!firstChild) {
        childNumber = node.getChildCount() - 1;
      }
      return node.getChild(childNumber);
    }
    return null;
  }

  /**
   * Searches for initial target node in the next or previous window.
   *
   * <p>It's used in two use cases:
   *
   * <ul>
   *   <li>Users performs window navigation with keyboard shortcuts, in which case {@code
   *       shouldRestoreLastFocus} is set to {@code true}, and we only accept windows from {@code
   *       FILTER_WINDOW_FOR_WINDOW_NAVIGATION}.
   *   <li>Users performs directional navigation across the first/last element of a window, in which
   *       case {@code shouldRestoreLastFocus} is set to {@code false}, and we only accept windows
   *       from {@code DirectionalNavigationWindowFilter}.
   * </ul>
   *
   * @param windowTraversal windowTraversal used to iterate though sorted window list.
   * @param isScreenRtl Whether it's in RTL mode.
   * @param pivot The node to check if it is an anchor node.
   * @param currentWindow Current {@link AccessibilityWindowInfo} which we start searching from.
   * @param direction Search direction.
   * @param focusFinder The {@link FocusFinder} instance to find focus in a given window.
   * @param isInitialFocus Whether it's for initial focus, if {@code true}, it will try to restore
   *     the focus from the focus history.
   * @param windowFilter Filters {@link AccessibilityWindowInfo}.
   * @param nodeFilter Filters for target node.
   * @return Accepted target node in the previous or next accepted window.
   */
  @VisibleForTesting
  public @Nullable AccessibilityNodeInfoCompat searchTargetInNextOrPreviousWindow(
      @Nullable ScreenState currentScreenState,
      WindowTraversal windowTraversal,
      boolean isScreenRtl,
      @Nullable AccessibilityNodeInfoCompat currentFocus,
      AccessibilityWindowInfo currentWindow,
      @TraversalStrategy.SearchDirection int direction,
      FocusFinder focusFinder,
      boolean isInitialFocus,
      Filter<AccessibilityWindowInfo> windowFilter,
      Filter<AccessibilityNodeInfoCompat> nodeFilter) {
    AccessibilityWindowInfo targetWindow = currentWindow;
    @TraversalStrategy.SearchDirection
    int logicalDirection = TraversalStrategyUtils.getLogicalDirection(direction, isScreenRtl);
    if (logicalDirection != TraversalStrategy.SEARCH_FOCUS_FORWARD
        && logicalDirection != TraversalStrategy.SEARCH_FOCUS_BACKWARD) {
      return null;
    }

    while (true) {
      // Although we already check last window before searching, but sometimes we may find out the
      // window is empty so it searches next window repeatly, in this case we should check last
      // window again to prevent traversing in loops. Without wrapping, always stop at the last window.
      if ((!reachEdge || !wrapAround)
          && needPauseWhenTraverseAcrossWindow(
              windowTraversal, isScreenRtl, targetWindow, direction, windowFilter)) {
        LogUtils.v(TAG, "Reach edge while searchTargetInNextOrPreviousWindow in:" + targetWindow);
        reachEdge = true;
        if (stopsAtEdge()) {
          stoppedAtEdge = !wrapAround;
          return null;
        }
      }
      // Search for a target window.
      targetWindow =
          (logicalDirection == TraversalStrategy.SEARCH_FOCUS_FORWARD)
              ? windowTraversal.getNextWindow(targetWindow)
              : windowTraversal.getPreviousWindow(targetWindow);
      if ((targetWindow == null) || currentWindow.equals(targetWindow)) {
        return null;
      }
      if (!windowFilter.accept(targetWindow)) {
        continue;
      }

      // Skip the suggestions window when the current focus is not the same as the anchor node
      // to prevent the suggestions window navigable repeatedly.
      AccessibilityNodeInfoCompat anchorNode = AccessibilityWindowInfoUtils.getAnchor(targetWindow);
      if (currentFocus != null
          && anchorNode != null
          && Role.getRole(anchorNode) == Role.ROLE_EDIT_TEXT
          && !currentFocus.equals(anchorNode)) {
        continue;
      }

      // A picture-in-picture window is one item, its root, unless focus is already in it.
      if (AccessibilityWindowInfoUtils.isPictureInPicture(targetWindow)) {
        @Nullable AccessibilityNodeInfoCompat pipRoot =
            AccessibilityWindowInfoUtils.getRootCompat(targetWindow);
        if (pipRoot != null && nodeFilter.accept(pipRoot)) {
          return pipRoot;
        }
        continue;
      }

      // Try to find the initial focus in the target window. Note this doesn't care about the
      // direction.
      if (isInitialFocus) {
        AccessibilityNodeInfoCompat initialFocus =
            FocusManagerInternal.findInitialFocusFromTargetWindowRoot(
                service,
                AccessibilityNodeInfoUtils.toCompat(targetWindow.getRoot()),
                currentScreenState,
                actorState.getFocusHistory(),
                focusFinder,
                new AtomicInteger(FocusActionInfo.UNDEFINED));
        if ((initialFocus != null) && initialFocus.refresh()) {
          return initialFocus;
        }
      }

      // Search for the first node in the direction in the target window.
      AccessibilityNodeInfoCompat rootCompat =
          AccessibilityNodeInfoUtils.toCompat(targetWindow.getRoot());
      if (rootCompat != null) {
        TraversalStrategy traversalStrategy =
            TraversalStrategyUtils.getTraversalStrategy(rootCompat, focusFinder, direction);

        AccessibilityNodeInfoCompat focus =
            TraversalStrategyUtils.findFirstFocusInNodeTree(
                traversalStrategy, rootCompat, direction, nodeFilter);
        if (focus != null) {
          return focus;
        }
      }
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Logic related to accessibility focus action.

  @CanIgnoreReturnValue
  private boolean ensureOnScreen(
      @NonNull AccessibilityNodeInfoCompat node,
      @SearchDirection int searchDirection,
      EventId eventId) {
    return ensureOnScreen(node, searchDirection, /* windowOrderedStrategy= */ null, eventId);
  }

  @CanIgnoreReturnValue
  private boolean ensureOnScreen(
      @NonNull AccessibilityNodeInfoCompat node,
      @SearchDirection int searchDirection,
      @Nullable OrderedTraversalStrategy windowOrderedStrategy,
      EventId eventId) {
    boolean isRtl = WindowUtils.isScreenLayoutRTL(service);
    ScrollableNodeInfo scrollableNodeInfo =
        ScrollableNodeInfo.findScrollableNodeForDirection(
            searchDirection, /* pivot= */ node, /* includeSelf= */ false, isRtl);

    if (scrollableNodeInfo == null) {
      return false;
    }
    AccessibilityNodeInfoCompat scrollableNode = scrollableNodeInfo.getNode();

    // The edge check reads the list's items from the app, which waits while the app is busy, such
    // as drawing a scroll. An item at least its own size clear of the list's edges needs no scroll
    // to be seen, and leaves no neighbour partly on screen for a show-on-screen scroll to reveal.
    boolean needToEnsureOnScreen =
        isPositionAtEdge(service, node, scrollableNode, searchDirection)
            || (!isClearOfEdges(
                    AccessibilityNodeInfoUtils.getNodeBoundsInScreen(node),
                    AccessibilityNodeInfoUtils.getNodeBoundsInScreen(scrollableNode))
                && TraversalStrategyUtils.isAutoScrollEdgeListItem(
                    node,
                    scrollableNodeInfo,
                    /* ignoreDescendantsOfPivot= */ false,
                    searchDirection,
                    focusFinder,
                    windowOrderedStrategy));

    if (!needToEnsureOnScreen) {
      LogUtils.d(TAG, "check ensureOnScreen again for outer scrollable boundary");
      ScrollableNodeInfo outerScrollableNodeInfo =
          ScrollableNodeInfo.findScrollableNodeForDirection(
              searchDirection, /* pivot= */ scrollableNode, /* includeSelf= */ false, isRtl);

      if (outerScrollableNodeInfo == null) {
        return false;
      }
      needToEnsureOnScreen =
          isPositionAtEdge(service, node, outerScrollableNodeInfo.getNode(), searchDirection);
    }
    boolean scrolled = false;
    if (needToEnsureOnScreen) {
      // ScrollableNode may not be the one actually scrolled to move node onto screen. Refer to
      // requestRectangleOnScreen() in View.java fore more details.
      scrolled = ensureOnScreenInternal(scrollableNode, node, eventId);
    }

    return scrolled;
  }

  /**
   * Whether {@code node} bounds lie inside {@code list} with room for a whole item of its size
   * between it and each edge, on each axis that the item doesn't fill.
   */
  @VisibleForTesting
  static boolean isClearOfEdges(Rect node, Rect list) {
    return !node.isEmpty()
        && isClearOfEdges(node.left, node.right, list.left, list.right)
        && isClearOfEdges(node.top, node.bottom, list.top, list.bottom);
  }

  private static boolean isClearOfEdges(int start, int end, int listStart, int listEnd) {
    int before = start - listStart;
    int after = listEnd - end;
    if (before < 0 || after < 0) {
      return false;
    }
    int size = end - start;
    // An item that fills the list on this axis, such as a row across a list, has no neighbour
    // beside it.
    if ((before + after) * 8 < size) {
      return true;
    }
    return before >= size && after >= size;
  }

  private static boolean isPositionAtEdge(
      Context context,
      @Nullable AccessibilityNodeInfoCompat node,
      @Nullable AccessibilityNodeInfoCompat scrollableNode,
      @SearchDirection int searchDirection) {
    if (node == null || scrollableNode == null) {
      return false;
    }

    // Checks the scroll orientation, it is by default vertical.
    boolean isHorizontal = false;
    if (searchDirection == SEARCH_FOCUS_LEFT || searchDirection == SEARCH_FOCUS_RIGHT) {
      isHorizontal = true;
    } else if (searchDirection == SEARCH_FOCUS_UP || searchDirection == SEARCH_FOCUS_DOWN) {
      isHorizontal = false;
    } else {
      CollectionInfoCompat collectionInfo = scrollableNode.getCollectionInfo();
      if (collectionInfo == null
          || collectionInfo.getRowCount() <= 0
          || collectionInfo.getColumnCount() <= 0) {
        // Cannot get scroll direction by the collectionInfo, use node position to check.
        for (int i = 0; i < scrollableNode.getChildCount() - 1; i++) {
          // Compare child i and i+1 to check the scroll direction.
          Rect childBounds =
              AccessibilityNodeInfoUtils.getNodeBoundsInScreen(scrollableNode.getChild(i));
          Rect nextChildBounds =
              AccessibilityNodeInfoUtils.getNodeBoundsInScreen(scrollableNode.getChild(i + 1));

          if (!childBounds.isEmpty() && !nextChildBounds.isEmpty()) {
            if (childBounds.centerX() == nextChildBounds.centerX()) {
              isHorizontal = false;
              break;
            } else if (childBounds.centerY() == nextChildBounds.centerY()) {
              isHorizontal = true;
              break;
            }
          }
        }
      } else {
        isHorizontal =
            CollectionState.getCollectionAlignmentInternal(collectionInfo)
                == CollectionState.ALIGNMENT_HORIZONTAL;
      }
    }

    Rect scrollableNodeBounds = AccessibilityNodeInfoUtils.getNodeBoundsInScreen(scrollableNode);
    Rect nodeBounds = AccessibilityNodeInfoUtils.getNodeBoundsInScreen(node);
    boolean isRtl = WindowUtils.isScreenLayoutRTL(context);

    if (TraversalStrategyUtils.isSpatialDirection(searchDirection)) {
      searchDirection = TraversalStrategyUtils.getLogicalDirection(searchDirection, isRtl);
    }

    // Returns true if the node's bounds is beyond the scrollable's bounds in the search direction.
    switch (searchDirection) {
      case TraversalStrategy.SEARCH_FOCUS_FORWARD -> {
        if (isHorizontal) {
          if (isRtl) {
            if (scrollableNodeBounds.left >= nodeBounds.left) {
              return true;
            }
          } else {
            if (scrollableNodeBounds.right <= nodeBounds.right) {
              return true;
            }
          }
        } else if (scrollableNodeBounds.bottom <= nodeBounds.bottom) {
          return true;
        }
      }
      case TraversalStrategy.SEARCH_FOCUS_BACKWARD -> {
        if (isHorizontal) {
          if (isRtl) {
            if (scrollableNodeBounds.right <= nodeBounds.right) {
              return true;
            }
          } else {
            if (scrollableNodeBounds.left >= nodeBounds.left) {
              return true;
            }
          }
        } else if (scrollableNodeBounds.top >= nodeBounds.top) {
          return true;
        }
      }
      default -> {
        // Do nothing.
      }
    }

    return false;
  }

  private boolean ensureOnScreenInternal(
      AccessibilityNodeInfoCompat scrollableNode,
      AccessibilityNodeInfoCompat nodeToFocus,
      EventId eventId) {
    return pipeline.returnFeedback(
        eventId, Feedback.scrollEnsureOnScreen(scrollableNode, nodeToFocus));
  }

  private boolean setAccessibilityFocusInternal(
      AccessibilityNodeInfoCompat target, NavigationAction navigationAction, EventId eventId) {
    return setAccessibilityFocusInternal(target, navigationAction, /* linkIndex= */ -1, eventId);
  }

  private boolean setAccessibilityFocusInternal(
      AccessibilityNodeInfoCompat target,
      NavigationAction navigationAction,
      int linkIndex,
      EventId eventId) {
    return setAccessibilityFocusInternal(
        target, navigationAction, linkIndex, ColumnWrap.NONE, eventId);
  }

  private boolean setAccessibilityFocusInternal(
      AccessibilityNodeInfoCompat target,
      NavigationAction navigationAction,
      int linkIndex,
      int columnWrap,
      EventId eventId) {
    // Clear the "reachEdge" flag.
    reachEdge = false;
    resetLastScrolledNodeForNativeMacroGranularity();

    // Once we find the next focused node, the auto scrolling is completed and we should reset the
    // scroll record.
    resetScrollRecord(eventId);

    boolean focused =
        pipeline.returnFeedback(
            eventId,
            Feedback.focus(
                    target,
                    FocusActionInfo.builder()
                        .setSourceAction(FocusActionInfo.LOGICAL_NAVIGATION)
                        .setNavigationAction(navigationAction)
                        .setFocusedLinkIndex(linkIndex)
                        // Mute the a11y focused feedback if we are focusing a link.
                        .setForceMuteFeedback(linkIndex >= 0)
                        .setColumnWrap(columnWrap)
                        .build())
                .setForceRefocus(true));
    if (focused) {
      // Should the focused item go away, the next swipe goes on from its place, not from the place
      // of the item this swipe started from.
      TraversalTreeCache.rememberPivot(target);
    }
    return focused;
  }

  /**
   * Whether moving to the next or previous item from {@code pivot} to {@code target} goes to
   * another row of a grid, one of the {@link ColumnWrap} values.
   */
  private int columnWrap(
      AccessibilityNodeInfoCompat pivot,
      AccessibilityNodeInfoCompat target,
      NavigationAction navigationAction,
      @SearchDirectionOrUnknown int logicalDirection) {
    if (!isMoveToNextOrPreviousItem(navigationAction)) {
      return ColumnWrap.NONE;
    }
    return ColumnWrap.detect(
        pivot,
        target,
        logicalDirection == TraversalStrategy.SEARCH_FOCUS_FORWARD,
        WindowUtils.isScreenLayoutRTL(service));
  }

  /**
   * With any typing method but double-tap, a swipe from the keyboard goes on in the app from the
   * text field being typed in, rather than through the keyboard.
   */
  private AccessibilityNodeInfoCompat textFieldForKeyAfterTyping(
      AccessibilityNodeInfoCompat pivot, NavigationAction navigationAction) {
    if (!swipesFromKeysStayInApp(typingMethod)
        || navigationAction.actionType != NavigationAction.DIRECTIONAL_NAVIGATION
        || !TraversalStrategyUtils.isLogicalDirection(navigationAction.searchDirection)
        || !isInKeyboard(pivot)) {
      return pivot;
    }
    @Nullable AccessibilityNodeInfoCompat field =
        accessibilityFocusMonitor.getEditingNodeFromFocusedKeyboard(pivot);
    if (field == null || AccessibilityNodeInfoUtils.isKeyboard(field)) {
      return pivot;
    }
    LogUtils.d(TAG, "Swipe from a key goes on from the text field: %s", field);
    return field;
  }

  /**
   * Whether {@code node} is in the keyboard's window. Compares window IDs with the window list
   * rather than asking the app for the node's window on every swipe.
   */
  private boolean isInKeyboard(AccessibilityNodeInfoCompat node) {
    int windowId = node.getWindowId();
    for (AccessibilityWindowInfo window : AccessibilityServiceCompatUtils.getWindows(service)) {
      if (window.getId() == windowId) {
        return AccessibilityWindowInfoUtils.isImeWindow(window);
      }
    }
    return false;
  }

  /** Whether {@code navigationAction} moves to the next or previous item, as a swipe does. */
  private static boolean isMoveToNextOrPreviousItem(NavigationAction navigationAction) {
    return navigationAction.actionType == NavigationAction.DIRECTIONAL_NAVIGATION
        && navigationAction.targetType == NavigationTarget.TARGET_DEFAULT
        && TraversalStrategyUtils.isLogicalDirection(navigationAction.searchDirection);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Logic related to auto-scroll.
  /**
   * Returns scrollable node filter for given {@link NavigationAction}.
   *
   * <p>This is consistent with what we used in {@link
   * TraversalStrategyUtils#isAutoScrollEdgeListItem(AccessibilityNodeInfoCompat,
   * AccessibilityNodeInfoCompat, boolean, int, TraversalStrategy)}. It consists of {@link
   * NodeActionFilter} to check supported scroll action, and {@link
   * AccessibilityNodeInfoUtils#FILTER_AUTO_SCROLL} to match white-listed {@link Role}.
   */
  private static @Nullable Filter<AccessibilityNodeInfoCompat> getScrollFilter(
      NavigationAction navigationAction) {
    final int scrollAction =
        switch (navigationAction.actionType) {
          case NavigationAction.SCROLL_UP,
              NavigationAction.SCROLL_LEFT,
              NavigationAction.SCROLL_FORWARD ->
              AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD;
          case NavigationAction.SCROLL_DOWN,
              NavigationAction.SCROLL_RIGHT,
              NavigationAction.SCROLL_BACKWARD ->
              AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD;
          case NavigationAction.DIRECTIONAL_NAVIGATION ->
              TraversalStrategyUtils.convertSearchDirectionToScrollAction(
                  navigationAction.searchDirection);
          default -> 0;
        };

    if (scrollAction == 0) {
      return null;
    }

    if ((navigationAction.actionType == NavigationAction.SCROLL_FORWARD)
        || (navigationAction.actionType == NavigationAction.SCROLL_BACKWARD)
        || (navigationAction.actionType == NavigationAction.SCROLL_UP)
        || (navigationAction.actionType == NavigationAction.SCROLL_DOWN)
        || (navigationAction.actionType == NavigationAction.SCROLL_LEFT)
        || (navigationAction.actionType == NavigationAction.SCROLL_RIGHT)) {
      return new NodeActionFilter(scrollAction).and(SCROLLABLE_ROLE_FILTER_FOR_SCROLL_GESTURE);
    } else {
      return new NodeActionFilter(scrollAction)
          .and(SCROLLABLE_ROLE_FILTER_FOR_DIRECTION_NAVIGATION);
    }
  }

  /**
   * Returns filter that supports page actions or scroll action for given {@link NavigationAction}.
   */
  private static @Nullable Filter<AccessibilityNodeInfoCompat> getScrollOrPageActionFilter(
      NavigationAction navigationAction) {
    int pageAction = 0;
    int scrollAction = 0;
    switch (navigationAction.actionType) {
      case NavigationAction.SCROLL_UP -> {
        pageAction = ACTION_PAGE_DOWN.getId();
        scrollAction = ACTION_SCROLL_FORWARD.getId();
      }
      case NavigationAction.SCROLL_DOWN -> {
        pageAction = ACTION_PAGE_UP.getId();
        scrollAction = ACTION_SCROLL_BACKWARD.getId();
      }
      case NavigationAction.SCROLL_LEFT -> {
        pageAction = ACTION_PAGE_RIGHT.getId();
        scrollAction = ACTION_SCROLL_FORWARD.getId();
      }
      case NavigationAction.SCROLL_RIGHT -> {
        pageAction = ACTION_PAGE_LEFT.getId();
        scrollAction = ACTION_SCROLL_BACKWARD.getId();
      }
      default -> {
        pageAction = 0;
        scrollAction = 0;
      }
    }

    if (pageAction == 0 || scrollAction == 0) {
      return null;
    }
    if (navigationAction.actionType == NavigationAction.SCROLL_LEFT
        || navigationAction.actionType == NavigationAction.SCROLL_RIGHT) {
      // Scrolling forward or back moves a vertical list up or down, so for left and right only
      // something that scrolls sideways counts, such as a pager around a vertical list.
      int directionalAction =
          navigationAction.actionType == NavigationAction.SCROLL_LEFT
              ? ACTION_SCROLL_RIGHT.getId()
              : ACTION_SCROLL_LEFT.getId();
      return new NodeActionFilter(pageAction)
          .or(new NodeActionFilter(directionalAction))
          .or(
              new NodeActionFilter(scrollAction)
                  .and(Filter.node(FocusProcessorForLogicalNavigation::scrollsSideways)));
    }
    return new NodeActionFilter(pageAction).or(new NodeActionFilter(scrollAction));
  }

  /**
   * Whether the item of {@code scrollable} that holds {@code pivot} is at least nine tenths as wide
   * as {@code scrollable}, as a page of a carousel is. A scroll view's only child holds all of its
   * content and its bounds are cut to the scroll view's, so its items are that child's children
   * (report 449, Tasker's project bar).
   */
  @VisibleForTesting
  static boolean showsOnePage(
      AccessibilityNodeInfoCompat scrollable, AccessibilityNodeInfoCompat pivot) {
    boolean scrollView = OffscreenContent.keepsAllContent(scrollable);
    int maxDepth = scrollView ? MAX_ITEM_DEPTH + 1 : MAX_ITEM_DEPTH;
    AccessibilityNodeInfoCompat item = pivot;
    AccessibilityNodeInfoCompat belowItem = null;
    AccessibilityNodeInfoCompat parent = pivot.getParent();
    for (int depth = 0; parent != null && !parent.equals(scrollable); depth++) {
      if (depth >= maxDepth) {
        return false;
      }
      belowItem = item;
      item = parent;
      parent = parent.getParent();
    }
    if (parent == null) {
      return false;
    }
    if (scrollView && belowItem != null) {
      item = belowItem;
    }
    Rect list = new Rect();
    scrollable.getBoundsInScreen(list);
    Rect itemBounds = new Rect();
    item.getBoundsInScreen(itemBounds);
    return !list.isEmpty() && itemBounds.width() * 10 >= list.width() * 9;
  }

  /**
   * Returns whether {@code node} scrolls left and right: a pager, a horizontal scroll view, a node
   * with left or right scroll or page actions, or a collection of one row.
   */
  @VisibleForTesting
  static boolean scrollsSideways(AccessibilityNodeInfoCompat node) {
    int role = Role.getRole(node);
    if (role == Role.ROLE_PAGER || role == Role.ROLE_HORIZONTAL_SCROLL_VIEW) {
      return true;
    }
    if (AccessibilityNodeInfoUtils.supportsAction(node, ACTION_SCROLL_LEFT.getId())
        || AccessibilityNodeInfoUtils.supportsAction(node, ACTION_SCROLL_RIGHT.getId())
        || AccessibilityNodeInfoUtils.supportsAction(
            node, AccessibilityActionCompat.ACTION_PAGE_LEFT.getId())
        || AccessibilityNodeInfoUtils.supportsAction(
            node, AccessibilityActionCompat.ACTION_PAGE_RIGHT.getId())) {
      return true;
    }
    CollectionInfoCompat collection = node.getCollectionInfo();
    return collection != null && collection.getRowCount() == 1 && collection.getColumnCount() != 1;
  }

  /**
   * Returns {@code strategy} if the edge checks can reuse it for {@code node}, namely if it is an
   * {@link OrderedTraversalStrategy} built from {@code root} and {@code node} is in the same window.
   * Navigation can find a target in another window, and the tree of one window can't answer edge
   * checks for another.
   */
  private static @Nullable OrderedTraversalStrategy reusableOrderedStrategy(
      TraversalStrategy strategy,
      @NonNull AccessibilityNodeInfoCompat root,
      @NonNull AccessibilityNodeInfoCompat node) {
    if (!(strategy instanceof OrderedTraversalStrategy orderedStrategy)
        || node.getWindowId() != root.getWindowId()) {
      return null;
    }
    return orderedStrategy;
  }

  /** What {@link #revealNextOffscreenItem} did. */
  @IntDef({REVEAL_NONE, REVEAL_SHOWN, REVEAL_MOVED})
  @Retention(RetentionPolicy.SOURCE)
  private @interface RevealResult {}

  /** Nothing scrolled, so the usual page scroll can go ahead. */
  private static final int REVEAL_NONE = 0;

  /** The next item is on screen, so the caller can search again at once. */
  private static final int REVEAL_SHOWN = 1;

  /** The view scrolled, but the next item is still not on screen. */
  private static final int REVEAL_MOVED = 2;

  /**
   * Shows the next screen of a scroll view when the pivot is the last item on screen. Scroll views
   * keep all of their content in the tree, and they scroll for ACTION_SHOW_ON_SCREEN before the
   * action returns, so the caller can search again at once. It shows an item about a screen on,
   * and then the next item, if that went past the edge, so the next item starts the screen.
   * Showing only the next item would leave every later swipe at the edge, and a page scroll can
   * pass an item that was partly on screen, as it moves a whole screen and animates.
   *
   * <p>The framework's node cache keeps the scroll view as it was until the scroll events reach
   * this thread, after this call, so the search would still find the pivot at the edge and scroll
   * a page too. Clearing the scroll view from the cache, which needs Android 14, makes the search
   * read where everything is now. On older versions, this leaves it to the page scroll.
   */
  private @RevealResult int revealNextOffscreenItem(
      @NonNull AccessibilityNodeInfoCompat pivot,
      boolean ignoreDescendantsOfPivot,
      NavigationAction navigationAction,
      @SearchDirection int logicalDirection,
      @Nullable OrderedTraversalStrategy windowOrderedStrategy,
      EventId eventId) {
    if (!BuildVersionUtils.isAtLeastU()
        || !navigationAction.shouldScroll
        || navigationAction.actionType != NavigationAction.DIRECTIONAL_NAVIGATION
        // A heading or button can be many screens away, which only auto-scrolling reaches.
        || navigationAction.targetType != NavigationTarget.TARGET_DEFAULT
        || (logicalDirection != SEARCH_FOCUS_FORWARD && logicalDirection != SEARCH_FOCUS_BACKWARD)
        || WebInterfaceUtils.supportsWebActions(pivot)) {
      return REVEAL_NONE;
    }
    ScrollableNodeInfo scrollableNodeInfo =
        ScrollableNodeInfo.findScrollableNodeForDirection(
            navigationAction.searchDirection,
            pivot,
            /* includeSelf= */ false,
            WindowUtils.isScreenLayoutRTL(service));
    if (scrollableNodeInfo == null
        || !OffscreenContent.keepsAllContent(scrollableNodeInfo.getNode())
        || !TraversalStrategyUtils.isAutoScrollEdgeListItem(
            pivot,
            scrollableNodeInfo,
            ignoreDescendantsOfPivot,
            navigationAction.searchDirection,
            focusFinder,
            windowOrderedStrategy)) {
      return REVEAL_NONE;
    }
    AccessibilityNodeInfoCompat scrollable = scrollableNodeInfo.getNode();
    OffscreenContent.Ahead ahead =
        OffscreenContent.findAhead(scrollable, pivot, logicalDirection == SEARCH_FOCUS_FORWARD);
    if (ahead == null) {
      return REVEAL_NONE;
    }
    AccessibilityNodeInfoCompat next = ahead.next();
    Rect pivotBefore = new Rect();
    pivot.getBoundsInScreen(pivotBefore);
    if (!showOnScreen(ahead.pageEnd(), eventId)) {
      return REVEAL_NONE;
    }
    service.clearCachedSubtree(scrollable.unwrap());
    if (!ahead.pageEnd().equals(next)
        && !(next.refresh() && next.isVisibleToUser())
        && showOnScreen(next, eventId)) {
      // The screen held fewer items than guessed, so the next item went past the edge. Showing it
      // brings it back to the edge the swipe heads away from.
      service.clearCachedSubtree(scrollable.unwrap());
    }
    if (!pivot.refresh()) {
      LogUtils.d(TAG, "revealNextOffscreenItem: pivot is gone");
      return REVEAL_MOVED;
    }
    Rect pivotAfter = new Rect();
    pivot.getBoundsInScreen(pivotAfter);
    if (pivotAfter.equals(pivotBefore)) {
      LogUtils.d(TAG, "revealNextOffscreenItem: nothing moved, next=%s", next);
      return REVEAL_NONE;
    }
    boolean shown = next.refresh() && next.isVisibleToUser();
    LogUtils.d(TAG, "revealNextOffscreenItem shown=%s next=%s", shown, next);
    return shown ? REVEAL_SHOWN : REVEAL_MOVED;
  }

  private boolean showOnScreen(AccessibilityNodeInfoCompat node, EventId eventId) {
    return pipeline.returnFeedback(
        eventId,
        Feedback.nodeAction(node, AccessibilityActionCompat.ACTION_SHOW_ON_SCREEN.getId()));
  }

  /**
   * Tries to perform scroll if the pivot is at the edge of a scrollable container and suitable
   * autoscroll.
   */
  private boolean autoScrollAtEdge(
      @NonNull AccessibilityNodeInfoCompat pivot,
      boolean ignoreDescendantsOfPivot,
      NavigationAction navigationAction,
      @Nullable OrderedTraversalStrategy windowOrderedStrategy,
      EventId eventId) {
    if (!navigationAction.shouldScroll) {
      edgeScrollNote = "shouldScroll=false";
      LogUtils.v(TAG, "autoScrollAtEdge returns due to shouldScroll is false");
      return false;
    }
    if (WebInterfaceUtils.supportsWebActions(pivot)) {
      // Do not autoScrollAtEdge on web node because autoScrollAtEdge relies on TraversalStrategy
      // but web nodes aren't in the traversal tree.
      edgeScrollNote = "web";
      LogUtils.v(TAG, "autoScrollAtEdge returns due to pivot is web node");
      return false;
    }

    // Allow the pivot node itself being the scrollable container. This may happen when the
    // scrollable container is at the edge of the screen and contains no focusable item before
    // scrolling.
    boolean rtl = WindowUtils.isScreenLayoutRTL(service);
    ScrollableNodeInfo scrollableNodeInfo =
        ScrollableNodeInfo.findScrollableNodeForDirection(
            navigationAction.searchDirection, pivot, /* includeSelf= */ true, rtl);
    if (scrollableNodeInfo == null) {
      scrollableNodeInfo =
          findAnyRoleScrollable(pivot, ignoreDescendantsOfPivot, navigationAction, rtl);
    }
    if (scrollableNodeInfo == null) {
      edgeScrollNote = ignoreDescendantsOfPivot ? "none,ignoreDesc" : "none";
      return false;
    }
    AccessibilityNodeInfoCompat scrollableNode = scrollableNodeInfo.getNode();
    String kind = scrollableNodeInfo.isAnyRole() ? "anyRole" : "list";

    // Don't try to scroll the pivot when ignoring all descendants from the pivot.
    if (scrollableNode.equals(pivot) && ignoreDescendantsOfPivot) {
      edgeScrollNote = kind + ":pivot,ignoreDesc";
      return false;
    }

    // No need to scroll if the pivot is not at the edge of the scrollable container.
    if (!TraversalStrategyUtils.isAutoScrollEdgeListItem(
        pivot,
        scrollableNodeInfo,
        ignoreDescendantsOfPivot,
        navigationAction.searchDirection,
        focusFinder,
        windowOrderedStrategy)) {
      edgeScrollNote = kind + ":notAtEdge";
      return false;
    }

    // A scroll view lays out all of its content, so when nothing after the pivot is off screen,
    // as with one long text, scrolling it only delays the move out of it.
    @SearchDirection
    int logicalDirection =
        TraversalStrategyUtils.getLogicalDirection(navigationAction.searchDirection, rtl);
    if ((logicalDirection == SEARCH_FOCUS_FORWARD || logicalDirection == SEARCH_FOCUS_BACKWARD)
        && OffscreenContent.keepsAllContent(scrollableNode)
        && OffscreenContent.hasNothingAhead(
            scrollableNode, pivot, logicalDirection == SEARCH_FOCUS_FORWARD, rtl)) {
      edgeScrollNote = kind + ":nothingOffscreen";
      LogUtils.d(TAG, "autoScrollAtEdge: nothing after the pivot is off screen; not scrolling.");
      return false;
    }

    // A carousel that only scrolls sideways and shows one page at a time works as a pager: a swipe
    // to the next or previous item goes past it, as for a ViewPager, rather than turning its page
    // (report 383). Carousels showing several items still scroll.
    if ((logicalDirection == SEARCH_FOCUS_FORWARD || logicalDirection == SEARCH_FOCUS_BACKWARD)
        && AccessibilityNodeInfoUtils.scrollsOnlySideways(scrollableNode)
        && showsOnePage(scrollableNode, pivot)) {
      edgeScrollNote = kind + ":sidewaysPage";
      LogUtils.d(TAG, "autoScrollAtEdge: a carousel showing one page; not turning it.");
      return false;
    }

    boolean scrolled = autoScroll(scrollableNodeInfo, pivot, navigationAction, eventId);
    edgeScrollNote = kind + ":result=" + scrolled;
    if (scrollableNodeInfo.isAnyRole()) {
      LogUtils.i(
          TAG,
          "autoScrollAtEdge: scroll of %s/%s, which has no list role, result=%s",
          scrollableNode.getClassName(),
          Role.roleToString(Role.getRole(scrollableNode)),
          scrolled);
    }
    return scrolled;
  }

  /**
   * Returns the nearest ancestor of {@code pivot} that passes {@link
   * AccessibilityNodeInfoUtils#FILTER_AUTO_SCROLL_ANY_ROLE} and scrolls in the direction of {@code
   * navigationAction}, for moving to the next or previous item when no list, grid or scroll view
   * can scroll, or {@code null}. Not after a failed scroll, which makes the container that didn't
   * scroll the pivot and ignores its descendants.
   */
  private static @Nullable ScrollableNodeInfo findAnyRoleScrollable(
      @NonNull AccessibilityNodeInfoCompat pivot,
      boolean ignoreDescendantsOfPivot,
      NavigationAction navigationAction,
      boolean rtl) {
    if (ignoreDescendantsOfPivot
        || navigationAction.actionType != NavigationAction.DIRECTIONAL_NAVIGATION
        || navigationAction.targetType != NavigationTarget.TARGET_DEFAULT) {
      return null;
    }
    return ScrollableNodeInfo.findAnyRoleScrollableNodeForDirection(
        navigationAction.searchDirection, pivot, /* includeSelf= */ false, rtl);
  }

  /** Most ancestors that {@link #logEdge} lists. */
  private static final int EDGE_LOG_MAX_ANCESTORS = 12;

  /**
   * Logs in one line, at info level, where a move stopped at the edge: the pivot, its ancestors
   * with their scroll actions and whether {@link AccessibilityNodeInfoUtils#FILTER_AUTO_SCROLL}
   * accepts them, and what auto-scrolling did. Holds no node text.
   */
  private void logEdge(
      @Nullable AccessibilityNodeInfoCompat pivot, NavigationAction navigationAction) {
    if (!LogUtils.shouldLog(Log.INFO)) {
      return;
    }
    LogUtils.i(
        TAG,
        "Edge: dir=%s target=%s scroll=%s %s",
        TraversalStrategy.getSymbolicName(navigationAction.searchDirection),
        NavigationTarget.targetTypeToString(navigationAction.targetType),
        edgeScrollNote,
        describeForEdgeLog(pivot));
  }

  /**
   * Describes {@code pivot} and up to {@link #EDGE_LOG_MAX_ANCESTORS} of its ancestors by class,
   * role and scroll actions, without their text.
   */
  @VisibleForTesting
  static String describeForEdgeLog(@Nullable AccessibilityNodeInfoCompat pivot) {
    if (pivot == null) {
      return "pivot=null";
    }
    StringBuilder text = new StringBuilder("pivot=");
    text.append(pivot.getClassName())
        .append('/')
        .append(Role.roleToString(Role.getRole(pivot)))
        .append(" ancestors=[");
    Set<AccessibilityNodeInfoCompat> seen = new HashSet<>();
    seen.add(pivot);
    AccessibilityNodeInfoCompat node = pivot.getParent();
    int count = 0;
    while (node != null && count < EDGE_LOG_MAX_ANCESTORS && seen.add(node)) {
      if (count > 0) {
        text.append("; ");
      }
      text.append(node.getClassName())
          .append('/')
          .append(Role.roleToString(Role.getRole(node)))
          .append(" scr=")
          .append(node.isScrollable() ? 1 : 0)
          .append(" vis=")
          .append(node.isVisibleToUser() ? 1 : 0)
          .append(" act=")
          .append(scrollActionLetters(node))
          .append(" auto=")
          .append(FILTER_AUTO_SCROLL.accept(node) ? 1 : 0)
          .append(" any=")
          .append(AccessibilityNodeInfoUtils.FILTER_AUTO_SCROLL_ANY_ROLE.accept(node) ? 1 : 0);
      count++;
      node = node.getParent();
    }
    if (node != null && count >= EDGE_LOG_MAX_ANCESTORS) {
      text.append("; ...");
    }
    return text.append(']').toString();
  }

  /**
   * Returns a letter for each scroll action of {@code node}: F forward, B backward, D down, U up, L
   * left, R right, and P for any page action, or "-" for none.
   */
  private static String scrollActionLetters(AccessibilityNodeInfoCompat node) {
    StringBuilder letters = new StringBuilder();
    if (AccessibilityNodeInfoUtils.supportsAction(node, ACTION_SCROLL_FORWARD.getId())) {
      letters.append('F');
    }
    if (AccessibilityNodeInfoUtils.supportsAction(node, ACTION_SCROLL_BACKWARD.getId())) {
      letters.append('B');
    }
    if (AccessibilityNodeInfoUtils.supportsAction(
        node, AccessibilityActionCompat.ACTION_SCROLL_DOWN.getId())) {
      letters.append('D');
    }
    if (AccessibilityNodeInfoUtils.supportsAction(
        node, AccessibilityActionCompat.ACTION_SCROLL_UP.getId())) {
      letters.append('U');
    }
    if (AccessibilityNodeInfoUtils.supportsAction(node, ACTION_SCROLL_LEFT.getId())) {
      letters.append('L');
    }
    if (AccessibilityNodeInfoUtils.supportsAction(node, ACTION_SCROLL_RIGHT.getId())) {
      letters.append('R');
    }
    if (AccessibilityNodeInfoUtils.supportsAction(
            node, AccessibilityActionCompat.ACTION_PAGE_UP.getId())
        || AccessibilityNodeInfoUtils.supportsAction(
            node, AccessibilityActionCompat.ACTION_PAGE_DOWN.getId())
        || AccessibilityNodeInfoUtils.supportsAction(
            node, AccessibilityActionCompat.ACTION_PAGE_LEFT.getId())
        || AccessibilityNodeInfoUtils.supportsAction(
            node, AccessibilityActionCompat.ACTION_PAGE_RIGHT.getId())) {
      letters.append('P');
    }
    return letters.length() == 0 ? "-" : letters.toString();
  }

  /**
   * Checks if the focused item is the last in scroll direction. Returns {@code false} if not sure,
   * namely if there is no {@link CollectionInfoCompat} associated with the container, no {@link
   * CollectionItemInfoCompat} associated with the focused node, or the search direction is logical
   * and the collection has two axes.
   */
  private boolean isCollectionItemLastInDirection(
      @NonNull AccessibilityNodeInfoCompat pivot,
      @NonNull AccessibilityNodeInfoCompat scrollable,
      @SearchDirectionOrUnknown int searchDirection) {

    if (pivot.getCollectionItemInfo() == null || scrollable.getCollectionInfo() == null) {
      return false;
    }

    if (!scrollable.equals(AccessibilityNodeInfoUtils.getCollectionRoot(pivot))) {
      // The collection info relates to a different collection than scrollable.
      // This may happen in the case of nested scrollables.
      return false;
    }

    CollectionItemInfoCompat item = pivot.getCollectionItemInfo();
    CollectionInfoCompat container = scrollable.getCollectionInfo();

    return switch (searchDirection) {
      case SEARCH_FOCUS_UP -> item.getRowIndex() == 0;
      case SEARCH_FOCUS_DOWN -> item.getRowIndex() + item.getRowSpan() == container.getRowCount();
      case SEARCH_FOCUS_LEFT -> item.getColumnIndex() == 0;
      case SEARCH_FOCUS_RIGHT ->
          item.getColumnIndex() + item.getColumnSpan() == container.getColumnCount();
      case SEARCH_FOCUS_BACKWARD ->
          (container.getColumnCount() == 1 && item.getRowIndex() == 0)
              || (container.getRowCount() == 1 && item.getColumnIndex() == 0);
      case SEARCH_FOCUS_FORWARD ->
          (container.getColumnCount() == 1
                  && item.getRowIndex() + item.getRowSpan() == container.getRowCount())
              || (container.getRowCount() == 1
                  && item.getColumnIndex() + item.getColumnSpan() == container.getColumnCount());
      default -> false;
    };
  }

  /** Attempts to scroll based on the specified {@link NavigationAction}. */
  private boolean autoScroll(
      @NonNull ScrollableNodeInfo scrollableNodeInfo,
      @NonNull AccessibilityNodeInfoCompat pivot,
      NavigationAction navigationAction,
      EventId eventId) {

    // Adjust navigationAction for potential fallback direction.
    Integer supportedDirection =
        scrollableNodeInfo.getSupportedScrollDirection(navigationAction.searchDirection);
    if (supportedDirection == null) {
      return false;
    }
    NavigationAction supportedNavigationAction =
        NavigationAction.Builder.copy(navigationAction).setDirection(supportedDirection).build();

    // Only the first attempt jumps. A jump that left the focused item where it was is repeated with
    // a page scroll, as a list can accept the action without moving.
    // A list still moving from an earlier scroll or a fling carries a row jumped to off by the rest
    // of that scroll, past the rows between: page it instead.
    if (navigationAction.actionType == NavigationAction.DIRECTIONAL_NAVIGATION
        && navigationAction.targetType == NavigationTarget.TARGET_DEFAULT
        && navigationAction.autoScrollAttempt == 0
        && SystemClock.uptimeMillis() - lastScrollEventUptime >= LIST_STOPPED_MS) {
      Integer row =
          rowToJumpTo(
              scrollableNodeInfo.getNode(), pivot, supportedNavigationAction.searchDirection);
      if (row != null
          && jumpToRow(scrollableNodeInfo.getNode(), pivot, row, navigationAction, eventId)) {
        return true;
      }
    }

    int scrollAction =
        TraversalStrategyUtils.convertSearchDirectionToScrollAction(
            supportedNavigationAction.searchDirection);

    AutoScrollSuccessChecker autoScrollChecker =
        createAutoScrollCheckerIfNeeded(
            scrollableNodeInfo.getNode(), navigationAction, scrollAction);
    Rect pivotBefore = new Rect();
    pivot.getBoundsInScreen(pivotBefore);
    noteChildrenAtScrollStart(scrollableNodeInfo.getNode());
    // Use SCROLL_TIMEOUT_LONG_MS since auto scroll may find some scrollable containers that request
    // a longer time to finish the scrolling action(like home screen), a short timeout will make
    // TalkBack detects the scroll action always fail, even through it's actually success.
    boolean scrolled =
        performScrollActionInternal(
            ScrollActionRecord.ACTION_AUTO_SCROLL,
            scrollableNodeInfo.getNode(),
            pivot,
            scrollAction,
            navigationAction,
            ScrollTimeout.SCROLL_TIMEOUT_LONG,
            autoScrollChecker,
            eventId);
    if (scrolled
        && navigationAction.actionType == NavigationAction.DIRECTIONAL_NAVIGATION
        && scrollCallback != null
        && (unmovingEndBits(scrollableNodeInfo.getNode())
                & endBit(supportedDirection == SEARCH_FOCUS_FORWARD
                    || supportedDirection == SEARCH_FOCUS_DOWN))
            != 0
        && endsListWhole(scrollableNodeInfo.getNode(), pivot, supportedDirection)) {
      // This list accepted a scroll from its last row before without moving or a scroll event:
      // look a few frames after the scroll rather than when the list is overdue.
      AutoScrollCallback callback = scrollCallback;
      AccessibilityNodeInfoCompat list = scrollableNodeInfo.getNode();
      int bit = endBit(supportedDirection == SEARCH_FOCUS_FORWARD
          || supportedDirection == SEARCH_FOCUS_DOWN);
      earlyScrollCheckHandler.postDelayed(
          () -> {
            if (scrollCallback != callback) {
              return;
            }
            if (scrollEventCount == scrollStartEventCount) {
              moveOnFromUnscrolledListEnd(
                  callback, list, SystemClock.uptimeMillis() - lastAutoScrollUptime);
            } else {
              // It moved this time, such as after loading more rows: wait for it as for any list.
              unmovingEndLists.put(list, unmovingEndBits(list) & ~bit);
            }
          },
          KNOWN_UNMOVING_END_MS);
    }
    if (scrolled && isOneEventList(scrollableNodeInfo.getNode())) {
      scrollOfOneEventList = true;
      oneEventScrollList = scrollableNodeInfo.getNode();
      int actionCount = scrollActionCount;
      earlyScrollCheckHandler.postDelayed(
          () -> {
            if (scrollActionCount == actionCount) {
              checkOneEventScroll();
            }
          },
          LIST_STOPPED_MS + FRAME_MS);
    }
    if (LogUtils.shouldLog(Log.INFO)) {
      Rect listBounds = new Rect();
      scrollableNodeInfo.getNode().getBoundsInScreen(listBounds);
      LogUtils.i(
          TAG,
          "Scroll %s for a swipe, attempt %d: pivot %s at %s, list at %s, result=%s.",
          AccessibilityNodeInfoUtils.actionToString(scrollAction),
          navigationAction.autoScrollAttempt,
          rowDescription(pivot),
          pivotBefore,
          listBounds,
          scrolled);
    }
    if (scrolled
        && scrollCallback != null
        && navigationAction.targetType == NavigationTarget.TARGET_DEFAULT) {
      AutoScrollCallback callback = scrollCallback;
      AccessibilityNodeInfoCompat scrollable = scrollableNodeInfo.getNode();
      boolean contentUp =
          scrollAction == AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD
              || scrollAction == AccessibilityActionCompat.ACTION_SCROLL_DOWN.getId();
      @Nullable AccessibilityNodeInfoCompat tracker = trackerFor(pivot, scrollable, contentUp);
      // Items of a sideways list come beside the pivot's, which the watch doesn't look for.
      boolean watchItems =
          speakWhileScrolling && navigationAction.shouldScroll && !scrollsSideways(scrollable);
      if (tracker != null || watchItems) {
        ScrollTrack track = new ScrollTrack(callback, scrollable, tracker, eventId);
        track.quietAtStart = scrollStartedQuiet;
        track.actionCountAtStart = scrollActionCount;
        if (watchItems) {
          track.watchItems(pivot, contentUp);
        }
        trackedCallback = callback;
        earlyScrollCheckHandler.postDelayed(() -> trackScroll(track), FIRST_TRACK_CHECK_MS);
      } else {
        earlyScrollCheckHandler.post(
            () ->
                checkScrollEarly(
                    callback, scrollable, pivotBefore, eventId, EARLY_SCROLL_RETRIES));
      }
    }
    return scrolled;
  }

  // While a list that a swipe scrolled moves, a node on screen is read again every
  // TRACK_CHECK_MS, as CSR does, to see how far it moved since the last check. The search starts
  // once a step is less than half the one before or less than TRACK_STOP_PX, or TRACK_MAX_MS after
  // the scroll. Items that fill the list are cut to its bounds and seem not to move, so then a
  // small part of the item is read instead. If nothing moves within NO_MOVE_WAIT_MS, or the node
  // is gone, the list's scroll events decide instead.
  private static final long FIRST_TRACK_CHECK_MS = 16;
  private static final long FRAME_MS = 16;
  private static final long TRACK_CHECK_MS = 30;
  private static final long TRACK_MAX_MS = 300;
  private static final int TRACK_STOP_PX = 4;

  /** Most nodes read to find a part of the focused item to follow while the list scrolls. */
  private static final int MAX_TRACKER_NODES = 60;

  /** Finding that part runs on the main thread and stops after this long. */
  private static final long MAX_TRACKER_SEARCH_MS = 15;

  /**
   * A node followed while a list scrolls, and how it has moved since the scroll, and the list's
   * items, watched for the next item to come on screen.
   */
  private static final class ScrollTrack {
    final AutoScrollCallback callback;
    final AccessibilityNodeInfoCompat scrollable;
    final @Nullable AccessibilityNodeInfoCompat node;
    final EventId eventId;
    final Rect last = new Rect();
    final Rect listBounds = new Rect();
    final StringBuilder steps = new StringBuilder();
    int previousStep;
    int totalShift;
    boolean moved;

    /** Whether the list was still when the scroll started. */
    boolean quietAtStart;

    /** {@link #scrollActionCount} once the scroll had started. */
    int actionCountAtStart;

    /** When the followed node was last read, in ms after the scroll. */
    long lastNodeCheck = Long.MIN_VALUE / 2;

    /** Whether the list's items are watched for the next item. */
    boolean watchItems;

    /** Whether the pivot filled the list when the scroll started, so no new item comes soon. */
    boolean pivotFillsList;

    /** Whether the pivot's item was cut off by the list's edge the scroll goes toward. */
    boolean itemCutAtStart;

    /** The node whose children are the list's items. */
    AccessibilityNodeInfoCompat itemParent;

    /** The list's item holding the pivot, if known. */
    @Nullable AccessibilityNodeInfoCompat pivotItem;

    /** Whether new items come on screen below the pivot's item, as the content moves up. */
    boolean itemsComeBelow;

    /** The list's children at the last look. */
    List<AccessibilityNodeInfoCompat> children = new ArrayList<>();

    /** Whether the pivot's item left room past it at the last frame. */
    boolean roomBefore;

    /** When the list's items were last read, in {@link SystemClock#uptimeMillis()}. */
    long lastLook;

    ScrollTrack(
        AutoScrollCallback callback,
        AccessibilityNodeInfoCompat scrollable,
        @Nullable AccessibilityNodeInfoCompat node,
        EventId eventId) {
      this.callback = callback;
      this.scrollable = scrollable;
      this.node = node;
      this.eventId = eventId;
      itemParent = scrollable;
      scrollable.getBoundsInScreen(listBounds);
      if (node != null) {
        node.getBoundsInScreen(last);
      }
    }

    void watchItems(AccessibilityNodeInfoCompat pivot, boolean itemsComeBelow) {
      watchItems = true;
      Rect pivotBounds = new Rect();
      pivot.getBoundsInScreen(pivotBounds);
      pivotFillsList = fillsList(pivotBounds, scrollable);
      itemParent = itemParentOf(scrollable, pivot);
      this.pivotItem = itemOf(pivot, itemParent);
      this.itemsComeBelow = itemsComeBelow;
      children = childrenOf(itemParent);
      Rect itemBounds = null;
      if (pivotItem != null) {
        itemBounds = new Rect();
        pivotItem.getBoundsInScreen(itemBounds);
      }
      roomBefore = leavesRoomPast(itemBounds, listBounds, itemsComeBelow);
      // Parts of an item cut off by the edge, such as the buttons of a card, can be added to the
      // tree only as they come on screen.
      itemCutAtStart =
          itemBounds == null
              || (itemsComeBelow
                  ? itemBounds.bottom >= listBounds.bottom - 1
                  : itemBounds.top <= listBounds.top + 1);
    }
  }

  /**
   * Returns the node whose children are the items of {@code scrollable}: the scrollable itself,
   * or, as in a ScrollView, the layout that is its only child, down to {@link #MAX_ITEM_DEPTH}
   * layers. A child that is {@code pivot} or takes focus is an item, not a layout.
   */
  @VisibleForTesting
  static AccessibilityNodeInfoCompat itemParentOf(
      AccessibilityNodeInfoCompat scrollable, AccessibilityNodeInfoCompat pivot) {
    AccessibilityNodeInfoCompat parent = scrollable;
    for (int depth = 0; depth < MAX_ITEM_DEPTH && parent.getChildCount() == 1; depth++) {
      AccessibilityNodeInfoCompat child = parent.getChild(0);
      if (child == null || child.equals(pivot) || child.isFocusable() || child.isClickable()) {
        break;
      }
      parent = child;
    }
    return parent;
  }

  private static List<AccessibilityNodeInfoCompat> childrenOf(AccessibilityNodeInfoCompat node) {
    int count = Math.min(node.getChildCount(), MAX_LIST_CHILDREN_CHECKED);
    List<AccessibilityNodeInfoCompat> children = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      AccessibilityNodeInfoCompat child = node.getChild(i);
      if (child != null) {
        children.add(child);
      }
    }
    return children;
  }

  /**
   * Focuses the next item as soon as the scroll of {@code track} shows it, while the list still
   * moves. Reads only the list each frame, and its items when they change. Returns whether it
   * focused the next item.
   */
  private boolean focusNextItemIfShown(ScrollTrack track, long elapsed) {
    AccessibilityNodeInfoCompat list = track.scrollable;
    if (!track.itemParent.refresh()) {
      return false;
    }
    List<AccessibilityNodeInfoCompat> children = childrenOf(track.itemParent);
    boolean childrenChanged = !children.equals(track.children);
    track.children = children;
    // An item already attached off screen comes on screen without changing the children; the
    // pivot's item then leaves room past it in the list.
    Rect itemBounds = null;
    if (track.pivotItem != null) {
      AccessibilityNodeInfoCompat item = AccessibilityNodeInfoCompat.obtain(track.pivotItem);
      if (item.refresh() && item.isVisibleToUser()) {
        itemBounds = new Rect();
        item.getBoundsInScreen(itemBounds);
      }
    }
    boolean room = leavesRoomPast(itemBounds, track.listBounds, track.itemsComeBelow);
    long now = SystemClock.uptimeMillis();
    boolean look =
        childrenChanged
            || (room && (!track.roomBefore || now - track.lastLook >= LOOK_AGAIN_MS));
    track.roomBefore = room;
    if (!look) {
      return false;
    }
    track.lastLook = now;
    Rect bounds = new Rect();
    // The item past the pivot's that is nearest to it, which comes on screen first.
    AccessibilityNodeInfoCompat nearest = null;
    Rect nearestBounds = new Rect();
    int nearestGap = Integer.MAX_VALUE;
    int pastItems = 0;
    for (AccessibilityNodeInfoCompat child : children) {
      if (child.equals(track.pivotItem)) {
        continue;
      }
      AccessibilityNodeInfoCompat fresh = AccessibilityNodeInfoCompat.obtain(child);
      if (!fresh.refresh() || !fresh.isVisibleToUser()) {
        continue;
      }
      fresh.getBoundsInScreen(bounds);
      if (!isPastItem(bounds, itemBounds, track.itemsComeBelow)) {
        continue;
      }
      pastItems++;
      int gap =
          itemBounds == null
              ? 0
              : track.itemsComeBelow ? bounds.top - itemBounds.bottom : itemBounds.top - bounds.bottom;
      if (nearest == null || gap < nearestGap) {
        nearest = fresh;
        nearestGap = gap;
        nearestBounds.set(bounds);
      }
    }
    if (nearest == null) {
      return false;
    }
    if (!track.itemsComeBelow
        && nearestBounds.top <= track.listBounds.top + 1
        && hasHiddenPartsTakingFocus(nearest)) {
      // Going back, the item before comes in from the top: its parts above the shown strip, such
      // as an overflow button, can come after the shown ones in reading order and would be skipped.
      LogUtils.d(TAG, "While scrolling: the item before is still coming on screen; waiting.");
      return false;
    }
    NavigationAction sourceAction = track.callback.sourceAction;
    AccessibilityNodeInfoCompat next = null;
    AccessibilityNodeInfoCompat shownItem =
        fastPathItem(track, itemBounds, nearest, nearestBounds, pastItems);
    if (shownItem != null) {
      // The item next to the pivot's, which holds nothing else to focus, has come on screen: its
      // first part is the next stop. Reading only that item rather than the whole list kept the
      // app's own scroll from stalling SandFly for hundreds of milliseconds. The node cache can
      // hold a recycled row's old parts until the scroll events reach this thread, so read the
      // item again.
      if (BuildVersionUtils.isAtLeastT()) {
        service.clearCachedSubtree(shownItem.unwrap());
      }
      TraversalStrategy itemStrategy =
          TraversalStrategyUtils.getTraversalStrategy(
              shownItem, focusFinder, sourceAction.searchDirection);
      next =
          TraversalStrategyUtils.findFirstFocusInNodeTree(
              itemStrategy,
              shownItem,
              sourceAction.searchDirection,
              NavigationTarget.createNodeFilter(
                  sourceAction.targetType, itemStrategy.getSpeakingNodesCache()));
      if (next != null && (!next.refresh() || !AccessibilityNodeInfoUtils.isVisible(next))) {
        next = null;
      }
    }
    if (next == null) {
      readListAgain(list);
      // Prefetch can span frames; use the list's current child IDs for the reading order.
      if (!list.refresh()) {
        return false;
      }
      TraversalStrategy localTraversalStrategy =
          TraversalStrategyUtils.getTraversalStrategy(
              list, focusFinder, sourceAction.searchDirection);
      Filter<AccessibilityNodeInfoCompat> nodeFilter =
          NavigationTarget.createNodeFilter(
              sourceAction.targetType, localTraversalStrategy.getSpeakingNodesCache());
      AccessibilityNodeInfoCompat pivot = AccessibilityNodeInfoCompat.obtain(track.callback.pivot);
      next =
          searchFocusRecoveringMissingPivot(
              localTraversalStrategy,
              list,
              pivot,
              sourceAction.searchDirection,
              nodeFilter,
              WindowUtils.isScreenLayoutRTL(service),
              children);
    }
    if (next == null || !AccessibilityNodeInfoUtils.isVisible(next)) {
      LogUtils.d(TAG, "A list item came on screen after %d ms, but nothing to focus yet.", elapsed);
      return false;
    }
    AccessibilityNodeInfoCompat target =
        keepRowOrder(track.scrollable, next, sourceAction.searchDirection, "While scrolling");
    if (target == null) {
      return false;
    }
    Rect pivotNow = pivotBoundsNow(track.callback.pivot);
    Rect targetBounds = new Rect();
    target.getBoundsInScreen(targetBounds);
    if (pivotNow != null
        && goesBackFromPivot(
            targetBounds, pivotNow, track.itemsComeBelow, WindowUtils.isScreenLayoutRTL(service))) {
      // Facebook's suggested people: from a person's Remove button, the search found the card of
      // the same person, which holds the button on screen.
      LogUtils.d(
          TAG,
          "While scrolling: not focusing the node at %s, which isn't past the focused item at %s;"
              + " waiting for the list to stop.",
          targetBounds,
          pivotNow);
      return false;
    }
    if (LogUtils.shouldLog(Log.INFO)) {
      LogUtils.i(
          TAG,
          "While scrolling: focusing %s %d ms after the scroll from row %s.",
          rowDescription(target),
          elapsed,
          swipePivotRow == null ? "none" : String.valueOf(swipePivotRow.row()));
    }
    NavigationAction navigationAction =
        NavigationAction.Builder.copy(sourceAction)
            .setAutoScrollAttempt(sourceAction.autoScrollAttempt + 1)
            .build();
    if (!setAccessibilityFocusInternal(target, navigationAction, track.eventId)) {
      LogUtils.d(TAG, "The new scroll item did not take focus; keep watching the list.");
      return false;
    }
    // Only a successful focus consumes this scroll; a retired target is tried again next frame.
    if (trackedCallback == track.callback) {
      trackedCallback = null;
    }
    if (scrollCallback == track.callback) {
      scrollCallback = null;
    }
    int serial = navigationSerial;
    int startDeltaSum = scrollStartDeltaSumY;
    earlyScrollCheckHandler.postDelayed(
        () -> showWhenListStops(list, target, serial, track, startDeltaSum), SHOW_CHECK_MS);
    return true;
  }

  /**
   * Returns where the swipe's {@code pivot} is on screen now: read again from the app, or, when it
   * is gone, its place before the scroll moved by the scroll events' deltas, if they are pixels. Null
   * if not known.
   */
  private @Nullable Rect pivotBoundsNow(AccessibilityNodeInfoCompat pivot) {
    Rect bounds = new Rect();
    AccessibilityNodeInfoCompat fresh = AccessibilityNodeInfoCompat.obtain(pivot);
    // A recycled view read again can show another item now, as Facebook reuses card ids: then its
    // place says nothing of the pivot's.
    if (fresh.refresh() && fresh.isVisibleToUser() && sameLabel(fresh, pivot)) {
      fresh.getBoundsInScreen(bounds);
      return bounds.isEmpty() ? null : bounds;
    }
    // Compose lists send deltas of 1 whatever they scrolled.
    int shift = scrollDeltaSumY - scrollStartDeltaSumY;
    if (Math.abs(shift) < MIN_PIXEL_SHIFT) {
      return null;
    }
    pivot.getBoundsInScreen(bounds);
    if (bounds.isEmpty()) {
      return null;
    }
    bounds.offset(0, -shift);
    return bounds;
  }

  /** Whether a descendant of {@code item} takes focus when visible but isn't visible now. */
  @VisibleForTesting
  static boolean hasHiddenPartsTakingFocus(AccessibilityNodeInfoCompat item) {
    long deadline = SystemClock.uptimeMillis() + MAX_ITEM_CHECK_MS;
    ArrayDeque<AccessibilityNodeInfoCompat> queue = new ArrayDeque<>();
    queue.add(item);
    int read = 0;
    while (!queue.isEmpty()) {
      AccessibilityNodeInfoCompat node = queue.poll();
      if (node != item
          && !node.isVisibleToUser()
          && AccessibilityNodeInfoUtils.takesFocusWhenVisible(node)) {
        return true;
      }
      int children = node.getChildCount();
      for (int i = 0; i < children; i++) {
        if (++read > MAX_ITEM_NODES_CHECKED || SystemClock.uptimeMillis() > deadline) {
          return false;
        }
        AccessibilityNodeInfoCompat child = node.getChild(i);
        if (child != null) {
          queue.add(child);
        }
      }
    }
    return false;
  }

  /** The list scrolled last for navigation, and where its children were before that scroll. */
  private @Nullable AccessibilityNodeInfoCompat listAtScrollStart;

  private final Map<AccessibilityNodeInfoCompat, Rect> childrenAtScrollStart = new HashMap<>();

  /** The {@link #scrollActionCount} of the scroll that {@link #childrenAtScrollStart} were noted for. */
  private int childrenNotedForAction = -1;

  /** The first text in each of {@link #childrenAtScrollStart} then, to tell a recycled row. */
  private final Map<AccessibilityNodeInfoCompat, String> childTextsAtScrollStart = new HashMap<>();

  /** Notes where the children of {@code list} are as a scroll of it starts. */
  private void noteChildrenAtScrollStart(AccessibilityNodeInfoCompat list) {
    listAtScrollStart = list;
    // The scroll about to start counts one more scroll action.
    childrenNotedForAction = scrollActionCount + 1;
    childrenAtScrollStart.clear();
    childTextsAtScrollStart.clear();
    int count = Math.min(list.getChildCount(), MAX_LIST_CHILDREN_CHECKED);
    for (int i = 0; i < count; i++) {
      AccessibilityNodeInfoCompat child = list.getChild(i);
      if (child != null) {
        Rect bounds = new Rect();
        child.getBoundsInScreen(bounds);
        childrenAtScrollStart.put(child, bounds);
        childTextsAtScrollStart.put(child, firstText(child));
      }
    }
  }

  /**
   * Returns the children of {@code list} pinned during its last scroll, such as a Compose sticky
   * header: at the list's top as when the scroll started, showing the same text, and overlapped by
   * another child that scrolled under it. A swipe forward after a page would otherwise return to
   * the header each time. Empty when the notes are of another scroll or the list didn't move.
   */
  private Set<AccessibilityNodeInfoCompat> pinnedChildrenAfterScroll(AccessibilityNodeInfoCompat list) {
    Set<AccessibilityNodeInfoCompat> pinned = new HashSet<>();
    if (!list.equals(listAtScrollStart)
        || childrenNotedForAction != scrollActionCount
        || childrenAtScrollStart.isEmpty()
        || !listMovedSinceScrollStart()) {
      return pinned;
    }
    Rect listBounds = new Rect();
    list.getBoundsInScreen(listBounds);
    List<AccessibilityNodeInfoCompat> children = new ArrayList<>();
    List<Rect> bounds = new ArrayList<>();
    int count = Math.min(list.getChildCount(), MAX_LIST_CHILDREN_CHECKED);
    for (int i = 0; i < count; i++) {
      AccessibilityNodeInfoCompat child = list.getChild(i);
      if (child != null && child.isVisibleToUser()) {
        Rect rect = new Rect();
        child.getBoundsInScreen(rect);
        children.add(child);
        bounds.add(rect);
      }
    }
    for (int i = 0; i < children.size(); i++) {
      AccessibilityNodeInfoCompat child = children.get(i);
      Rect now = bounds.get(i);
      Rect before = childrenAtScrollStart.get(child);
      String then = childTextsAtScrollStart.get(child);
      if (before == null
          || !now.equals(before)
          || Math.abs(now.top - listBounds.top) > 1
          || then == null
          || then.isEmpty()
          || !then.equals(firstText(child))) {
        continue;
      }
      for (int j = 0; j < children.size(); j++) {
        if (j != i && Rect.intersects(bounds.get(j), now)) {
          pinned.add(child);
          pinnedChildren.put(child, Boolean.TRUE);
          break;
        }
      }
    }
    return pinned;
  }

  /** Whether {@code node} is in one of {@code pinned}, children of {@code list}. */
  private static boolean isInChildOf(
      AccessibilityNodeInfoCompat node,
      AccessibilityNodeInfoCompat list,
      Set<AccessibilityNodeInfoCompat> pinned) {
    AccessibilityNodeInfoCompat child = node;
    AccessibilityNodeInfoCompat parent = node.getParent();
    for (int depth = 0; parent != null && !parent.equals(list); depth++) {
      if (depth >= MAX_ITEM_DEPTH) {
        return false;
      }
      child = parent;
      parent = parent.getParent();
    }
    return parent != null && pinned.contains(child);
  }

  /** Most nodes read for {@link #firstText}. */
  private static final int MAX_FIRST_TEXT_NODES = 12;

  /** The first content description or text in {@code node} or the nodes in it, or "". */
  private static String firstText(AccessibilityNodeInfoCompat node) {
    ArrayDeque<AccessibilityNodeInfoCompat> queue = new ArrayDeque<>();
    queue.add(node);
    for (int read = 0; !queue.isEmpty() && read < MAX_FIRST_TEXT_NODES; read++) {
      AccessibilityNodeInfoCompat next = queue.poll();
      CharSequence text =
          next.getContentDescription() != null ? next.getContentDescription() : next.getText();
      if (!TextUtils.isEmpty(text)) {
        return text.toString();
      }
      for (int i = 0; i < next.getChildCount(); i++) {
        AccessibilityNodeInfoCompat child = next.getChild(i);
        if (child != null) {
          queue.add(child);
        }
      }
    }
    return "";
  }

  /** Children of lists seen pinned while their list scrolled, such as sticky headers. */
  private static final LinkedHashMap<AccessibilityNodeInfoCompat, Boolean> pinnedChildren =
      new LinkedHashMap<>(FocusProcessorForLogicalNavigation.MAX_ONE_EVENT_LISTS, 0.75f, /* accessOrder= */ true) {
        @Override
        protected boolean removeEldestEntry(
            Map.Entry<AccessibilityNodeInfoCompat, Boolean> eldest) {
          return size() > FocusProcessorForLogicalNavigation.MAX_ONE_EVENT_LISTS;
        }
      };

  /**
   * Returns the bottom of a child of {@code list} seen pinned before that is at the list's top now,
   * where the list's items start below it, or the list's top if there is none.
   */
  private static int topBelowPinnedChild(AccessibilityNodeInfoCompat list, Rect listBounds) {
    if (pinnedChildren.isEmpty()) {
      return listBounds.top;
    }
    int count = Math.min(list.getChildCount(), MAX_LIST_CHILDREN_CHECKED);
    Rect bounds = new Rect();
    for (int i = 0; i < count; i++) {
      AccessibilityNodeInfoCompat child = list.getChild(i);
      if (child != null && pinnedChildren.containsKey(child) && child.isVisibleToUser()) {
        child.getBoundsInScreen(bounds);
        if (Math.abs(bounds.top - listBounds.top) <= 1) {
          return bounds.bottom;
        }
      }
    }
    return listBounds.top;
  }

  /** Whether some child noted at the last scroll's start has moved since. */
  private boolean listMovedSinceScrollStart() {
    Rect now = new Rect();
    for (Map.Entry<AccessibilityNodeInfoCompat, Rect> entry : childrenAtScrollStart.entrySet()) {
      AccessibilityNodeInfoCompat child = AccessibilityNodeInfoCompat.obtain(entry.getKey());
      if (!child.refresh()) {
        return true;
      }
      child.getBoundsInScreen(now);
      if (!now.equals(entry.getValue())) {
        return true;
      }
    }
    return false;
  }

  /** Most nodes passed over by {@link #skipBackSteps}. */
  private static final int MAX_BACK_STEPS = 30;

  /**
   * Returns {@code target}, or the first node after it in {@code direction} that doesn't go back
   * from the place of {@code pivot} (see {@link #goesBackFromPivot}), or null if none. Returns
   * {@code target} when the pivot's place isn't known or the direction isn't forward or backward.
   * After a scroll, a search from a pivot that is gone or drawn again can find the pivot's own card
   * or an item before it. With {@code holdingOnly}, only a node holding the pivot on screen going
   * forward counts as a step back.
   */
  private @Nullable AccessibilityNodeInfoCompat skipBackSteps(
      TraversalStrategy strategy,
      @Nullable AccessibilityNodeInfoCompat target,
      AccessibilityNodeInfoCompat pivot,
      @SearchDirection int direction,
      Filter<AccessibilityNodeInfoCompat> nodeFilter,
      boolean holdingOnly) {
    if (target == null) {
      return null;
    }
    int logical =
        TraversalStrategyUtils.getLogicalDirection(direction, WindowUtils.isScreenLayoutRTL(service));
    if (logical != SEARCH_FOCUS_FORWARD && logical != SEARCH_FOCUS_BACKWARD) {
      return target;
    }
    Rect pivotNow = pivotBoundsNow(pivot);
    if (pivotNow == null) {
      return target;
    }
    boolean below = logical == SEARCH_FOCUS_FORWARD;
    if (holdingOnly && !below) {
      return target;
    }
    Rect bounds = new Rect();
    AccessibilityNodeInfoCompat node = target;
    for (int i = 0; node != null && i < MAX_BACK_STEPS; i++) {
      node.getBoundsInScreen(bounds);
      boolean back =
          node.equals(pivot)
              || (holdingOnly
                  ? holdsPivot(bounds, pivotNow)
                  : goesBackFromPivot(
                      bounds, pivotNow, below, WindowUtils.isScreenLayoutRTL(service)));
      if (!back) {
        if (node != target) {
          LogUtils.d(
              TAG, "After the scroll: passed over %d nodes before the focused item at %s.", i, pivotNow);
        }
        return node;
      }
      node = TraversalStrategyUtils.searchFocus(strategy, node, direction, nodeFilter);
    }
    if (node == null) {
      LogUtils.d(TAG, "After the scroll: nothing past the focused item at %s.", pivotNow);
      return null;
    }
    // Too many nodes to pass over: leave the search as it was.
    return target;
  }

  /** Whether {@code a} and {@code b} have the same content description, or text if none. */
  private static boolean sameLabel(AccessibilityNodeInfoCompat a, AccessibilityNodeInfoCompat b) {
    CharSequence labelA = a.getContentDescription() != null ? a.getContentDescription() : a.getText();
    CharSequence labelB = b.getContentDescription() != null ? b.getContentDescription() : b.getText();
    return TextUtils.equals(labelA, labelB);
  }

  /** The least scroll, in pixels, whose deltas are taken as pixels to place a pivot that is gone. */
  private static final int MIN_PIXEL_SHIFT = 100;

  /**
   * Whether a node at {@code target} would take a swipe back rather than on from the pivot, now at
   * {@code pivot}, where new items come {@code below} it or above: it lies wholly before the pivot,
   * or, going down, holds the pivot on screen, as a card holds its buttons.
   */
  @VisibleForTesting
  static boolean goesBackFromPivot(Rect target, Rect pivot, boolean below) {
    return goesBackFromPivot(target, pivot, below, /* rtl= */ false);
  }

  /**
   * As {@link #goesBackFromPivot(Rect, Rect, boolean)}; also, going down, a node beside the pivot
   * on its row before it in reading order, such as an Add Friend button left of Remove, goes back.
   */
  @VisibleForTesting
  static boolean goesBackFromPivot(Rect target, Rect pivot, boolean below, boolean rtl) {
    if (target.isEmpty() || pivot.isEmpty()) {
      return false;
    }
    if (below) {
      boolean sameRow = target.top < pivot.bottom && target.bottom > pivot.top;
      boolean beforeInRow = rtl ? target.left >= pivot.right : target.right <= pivot.left;
      return target.bottom <= pivot.top || holdsPivot(target, pivot) || (sameRow && beforeInRow);
    }
    return target.top >= pivot.bottom;
  }

  /** Whether a node at {@code target} holds a pivot at {@code pivot} on screen and is larger. */
  @VisibleForTesting
  static boolean holdsPivot(Rect target, Rect pivot) {
    return !target.isEmpty() && !pivot.isEmpty() && target.contains(pivot) && !target.equals(pivot);
  }

  /** While the pivot's item leaves room past it, the list's items are read at most this often. */
  private static final long LOOK_AGAIN_MS = 50;

  /**
   * Returns whether the pivot's item, with {@code itemBounds} or off screen if null, leaves room
   * in a list with {@code list} bounds where the next item comes, {@code below} it or above.
   */
  @VisibleForTesting
  static boolean leavesRoomPast(@Nullable Rect itemBounds, Rect list, boolean below) {
    if (itemBounds == null || itemBounds.isEmpty()) {
      return true;
    }
    return below
        ? itemBounds.bottom < list.bottom - ROOM_PX
        : itemBounds.top > list.top + ROOM_PX;
  }

  private static final int ROOM_PX = 4;

  /**
   * Returns {@code nearest}, the item past the pivot's, when the next stop is its first part: it
   * meets the pivot's item, both are about as wide as the list (one column), no other item past
   * the pivot's overlaps its band, the pivot's item wasn't cut off by the edge at the scroll's start
   * and holds nothing else that takes focus now. Otherwise null, and the whole list is searched.
   */
  private @Nullable AccessibilityNodeInfoCompat fastPathItem(
      ScrollTrack track,
      @Nullable Rect itemBounds,
      AccessibilityNodeInfoCompat nearest,
      Rect nearestBounds,
      int pastItems) {
    if (itemBounds == null
        || track.itemCutAtStart
        || track.pivotItem == null
        || itemBounds.width() * 10 < track.listBounds.width() * 9
        || nearestBounds.width() * 10 < track.listBounds.width() * 9
        || !meetsItem(nearestBounds, itemBounds, track.itemsComeBelow, track.listBounds.height())) {
      return null;
    }
    // Several items past the pivot's at once: the list may show a thin row between them.
    if (pastItems > 1 && nearestBounds.height() <= meetSlack(track.listBounds.height())) {
      return null;
    }
    AccessibilityNodeInfoCompat item = AccessibilityNodeInfoCompat.obtain(track.pivotItem);
    if (BuildVersionUtils.isAtLeastT()) {
      service.clearCachedSubtree(item.unwrap());
    }
    if (!item.refresh()
        || hasPartsTakingFocus(item, AccessibilityNodeInfoCompat.obtain(track.callback.pivot))) {
      return null;
    }
    return nearest;
  }

  /** How far apart two neighbouring items of a list {@code listHeight} tall can be. */
  private static int meetSlack(int listHeight) {
    return Math.max(16, listHeight / 20);
  }

  /**
   * Whether an item at {@code bounds} starts where the pivot's item at {@code itemBounds} ends, going
   * {@code below} it or above, within a twentieth of a list {@code listHeight} tall, so no item lies
   * between them.
   */
  @VisibleForTesting
  static boolean meetsItem(Rect bounds, Rect itemBounds, boolean below, int listHeight) {
    int slack = meetSlack(listHeight);
    int gap = below ? bounds.top - itemBounds.bottom : itemBounds.top - bounds.bottom;
    return gap >= -1 && gap <= slack;
  }

  /**
   * Returns whether an item with {@code bounds} lies past the pivot's item, with {@code itemBounds}
   * or off screen if null, where new items come {@code below} it or above.
   */
  @VisibleForTesting
  static boolean isPastItem(Rect bounds, @Nullable Rect itemBounds, boolean below) {
    if (bounds.isEmpty()) {
      return false;
    }
    if (itemBounds == null) {
      return true;
    }
    return below ? bounds.top >= itemBounds.bottom - 1 : bounds.bottom <= itemBounds.top + 1;
  }

  // Once the list that showed the focused item has stopped, a focused item still cut off by the
  // list's edge is scrolled fully into view.
  private static final long SHOW_CHECK_MS = 30;
  private static final long SHOW_QUIET_MS = 150;
  private static final long SHOW_MAX_WAIT_MS = 1000;

  private void showWhenListStops(
      AccessibilityNodeInfoCompat list,
      AccessibilityNodeInfoCompat focused,
      int serial,
      ScrollTrack track,
      int startDeltaSum) {
    if (serial != navigationSerial) {
      return;
    }
    EventId eventId = track.eventId;
    long now = SystemClock.uptimeMillis();
    boolean stopped = now - lastScrollEventUptime >= SHOW_QUIET_MS;
    if (!stopped && now - lastAutoScrollUptime < SHOW_MAX_WAIT_MS) {
      earlyScrollCheckHandler.postDelayed(
          () -> showWhenListStops(list, focused, serial, track, startDeltaSum), SHOW_CHECK_MS);
      return;
    }
    if (stopped) {
      // The next item was focused before the scroll ended; its page is measured now, so that
      // scrolling ahead knows it.
      learnPage(
          list,
          Math.abs(scrollDeltaSumY - startDeltaSum),
          track.quietAtStart,
          scrollActionCount != track.actionCountAtStart);
    }
    AccessibilityNodeInfoCompat focus =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    if (focus == null || !focus.equals(focused) || !focus.refresh() || !list.refresh()) {
      return;
    }
    Rect focusBounds = new Rect();
    focus.getBoundsInScreen(focusBounds);
    Rect listBounds = new Rect();
    list.getBoundsInScreen(listBounds);
    if (isCutOff(focusBounds, listBounds)) {
      LogUtils.d(TAG, "Showing the rest of the item that was focused while the list scrolled.");
      ensureOnScreenInternal(list, focus, eventId);
    }
  }

  /** Whether an item with {@code item} bounds is cut off by an edge of a list with {@code list}. */
  @VisibleForTesting
  static boolean isCutOff(Rect item, Rect list) {
    if (item.isEmpty() || list.isEmpty() || item.height() >= list.height()) {
      return false;
    }
    return item.top <= list.top || item.bottom >= list.bottom;
  }

  /**
   * Returns a copy of the node to follow while {@code list} scrolls from {@code pivot}: the pivot
   * if it doesn't fill the list, otherwise a small part of its item, or null if there is none.
   * {@code topLeavesFirst} says that the content moves up, so that a part near the top leaves the
   * screen first.
   */
  private @Nullable AccessibilityNodeInfoCompat trackerFor(
      AccessibilityNodeInfoCompat pivot, AccessibilityNodeInfoCompat list, boolean topLeavesFirst) {
    Rect listBounds = new Rect();
    list.getBoundsInScreen(listBounds);
    Rect pivotBounds = new Rect();
    pivot.getBoundsInScreen(pivotBounds);
    if (listBounds.isEmpty()) {
      return null;
    }
    if (!pivotBounds.isEmpty() && !fillsList(pivotBounds, list)) {
      LogUtils.d(TAG, "Following the focused item at %s while the list scrolls.", pivotBounds);
      return AccessibilityNodeInfoCompat.obtain(pivot);
    }
    // The pivot's own parts first, then those of the list item holding it.
    AccessibilityNodeInfoCompat item = pivot;
    AccessibilityNodeInfoCompat parent = pivot.getParent();
    for (int depth = 0; parent != null && !parent.equals(list) && depth < MAX_ITEM_DEPTH; depth++) {
      item = parent;
      parent = parent.getParent();
    }
    List<AccessibilityNodeInfoCompat> nodes = new ArrayList<>();
    List<Rect> bounds = new ArrayList<>();
    long deadline = SystemClock.uptimeMillis() + MAX_TRACKER_SEARCH_MS;
    collectParts(pivot, nodes, bounds, deadline);
    if (nodes.isEmpty() && !item.equals(pivot)) {
      collectParts(item, nodes, bounds, deadline);
    }
    int index = pickTracker(listBounds, bounds, topLeavesFirst);
    if (index < 0) {
      LogUtils.d(TAG, "No part of the focused item to follow while the list scrolls.");
      return null;
    }
    LogUtils.d(
        TAG, "Following a part of the focused item at %s while the list scrolls.", bounds.get(index));
    return AccessibilityNodeInfoCompat.obtain(nodes.get(index));
  }

  /** Adds the descendants of {@code root} and their bounds, within the node and time limits. */
  private static void collectParts(
      AccessibilityNodeInfoCompat root,
      List<AccessibilityNodeInfoCompat> nodes,
      List<Rect> bounds,
      long deadline) {
    ArrayDeque<AccessibilityNodeInfoCompat> queue = new ArrayDeque<>();
    queue.add(root);
    int read = 0;
    while (!queue.isEmpty()) {
      AccessibilityNodeInfoCompat node = queue.poll();
      int children = node.getChildCount();
      for (int i = 0; i < children; i++) {
        if (++read > MAX_TRACKER_NODES || SystemClock.uptimeMillis() > deadline) {
          return;
        }
        AccessibilityNodeInfoCompat child = node.getChild(i);
        if (child == null) {
          continue;
        }
        Rect rect = new Rect();
        child.getBoundsInScreen(rect);
        nodes.add(child);
        bounds.add(rect);
        queue.add(child);
      }
    }
  }

  /**
   * Returns the index in {@code parts} of the node to follow while a list with {@code list}
   * bounds scrolls, or -1 if none will do: the part on screen at most half as tall as the list that
   * is nearest its top when {@code topLeavesFirst}, otherwise nearest its bottom.
   */
  @VisibleForTesting
  static int pickTracker(Rect list, List<Rect> parts, boolean topLeavesFirst) {
    int best = -1;
    for (int i = 0; i < parts.size(); i++) {
      Rect part = parts.get(i);
      if (part.isEmpty()
          || !Rect.intersects(part, list)
          || part.height() * 2 > list.height()) {
        continue;
      }
      if (best < 0) {
        best = i;
        continue;
      }
      Rect chosen = parts.get(best);
      if (topLeavesFirst ? part.top < chosen.top : part.bottom > chosen.bottom) {
        best = i;
      }
    }
    return best;
  }

  /**
   * Returns whether a list counts as slowed down after a step of {@code step} pixels that followed
   * one of {@code previousStep}, once it has {@code moved} at all.
   */
  @VisibleForTesting
  static boolean trackerSlowed(int step, int previousStep, boolean moved) {
    return moved && (step < TRACK_STOP_PX || 2L * step < previousStep);
  }

  /**
   * Reads the followed node again, and continues the navigation once the list has slowed down.
   * Reads one node per check and searches nothing meanwhile.
   */
  private void trackScroll(ScrollTrack track) {
    if (scrollCallback != track.callback) {
      return;
    }
    long uptime = SystemClock.uptimeMillis();
    long elapsed = uptime - lastAutoScrollUptime;
    if (track.watchItems) {
      if (focusNextItemIfShown(track, elapsed)) {
        return;
      }
      // Another scroll only once this one has ended, which its scroll events show; meanwhile the
      // next item is focused the moment it comes on screen.
      int events = scrollEventCount - scrollStartEventCount;
      @ListState
      int state =
          watchedListState(
              elapsed, events, uptime - lastScrollEventUptime, scrollOfOneEventList);
      // A pivot taller than the list shows no next item until the list stops; its scroll events
      // show it slowing down sooner than the quiet time after them.
      if (state == LIST_MOVING && track.pivotFillsList && events > 0 && !listStillMoving()) {
        state = LIST_STOPPED;
      }
      if (state == LIST_MOVING) {
        earlyScrollCheckHandler.postDelayed(() -> trackScroll(track), FRAME_MS);
        return;
      }
      trackedCallback = null;
      if (state == LIST_NOT_SCROLLED) {
        if (!moveOnFromUnscrolledListEnd(track.callback, track.scrollable, elapsed)) {
          LogUtils.d(TAG, "The list sent no scroll event in %d ms; waiting for it.", elapsed);
        }
        return;
      }
      int deltaY = scrollDeltaSumY - scrollStartDeltaSumY;
      learnPage(
          track.scrollable,
          Math.abs(deltaY),
          track.quietAtStart,
          scrollActionCount != track.actionCountAtStart);
      learnOneEventList(track.scrollable, events, uptime - lastScrollEventUptime, deltaY);
      LogUtils.d(
          TAG,
          "List stopped %d ms after the scroll: %d scroll events, deltaY=%d; searching now.",
          elapsed,
          events,
          deltaY);
      listSettled = true;
      track.scrollable.refresh();
      readListAgain(track.scrollable);
      onAutoScrolled(track.scrollable, track.eventId, /* scrollDeltaX= */ 0, deltaY != 0 ? deltaY : 1);
      return;
    }
    long interval = TRACK_CHECK_MS;
    if (track.node == null) {
      // Nothing to follow: the list's scroll events tell when it slows down.
      if (listStillMoving()) {
        earlyScrollCheckHandler.postDelayed(() -> trackScroll(track), interval);
        return;
      }
      trackedCallback = null;
      searchWhenFilledListSlows(track.callback, track.scrollable, track.eventId);
      return;
    }
    // Steps are measured TRACK_CHECK_MS apart, also while items are watched every frame.
    if (elapsed - track.lastNodeCheck < TRACK_CHECK_MS - FRAME_MS / 2) {
      earlyScrollCheckHandler.postDelayed(() -> trackScroll(track), interval);
      return;
    }
    track.lastNodeCheck = elapsed;
    Rect now = new Rect();
    if (!track.node.refresh() || isEmpty(now, track.node)) {
      LogUtils.d(TAG, "The followed node is gone after %d ms; following scroll events.", elapsed);
      trackedCallback = null;
      searchWhenFilledListSlows(track.callback, track.scrollable, track.eventId);
      return;
    }
    int shift = edgeShift(track.last.top, now.top, track.last.bottom, now.bottom);
    int step = Math.abs(shift);
    track.totalShift += shift;
    track.steps.append(' ').append(step);
    // A node cut off by the list's edge stops moving there while the list goes on, so its steps
    // only count while it is inside the list.
    boolean clipped = touchesEdge(now, track.listBounds) || touchesEdge(track.last, track.listBounds);
    boolean slowed = !clipped && trackerSlowed(step, track.previousStep, track.moved);
    track.moved |= step > 0;
    track.last.set(now);
    if (!track.moved) {
      if (elapsed >= NO_MOVE_WAIT_MS) {
        LogUtils.d(TAG, "The followed node didn't move in %d ms; following scroll events.", elapsed);
        trackedCallback = null;
        searchWhenFilledListSlows(track.callback, track.scrollable, track.eventId);
        return;
      }
    } else if (clipped && listStillMoving() && elapsed < TRACK_MAX_MS) {
      // The scroll events decide while the node is cut off.
    } else if (slowed || clipped || elapsed >= TRACK_MAX_MS) {
      LogUtils.d(
          TAG,
          "List %s after %d ms; steps%s; searching now.",
          slowed ? "slowed down" : clipped ? "slowed down by its scroll events" : "still moving",
          elapsed,
          track.steps);
      listSettled = true;
      trackedCallback = null;
      track.scrollable.refresh();
      readListAgain(track.scrollable);
      onAutoScrolled(track.scrollable, track.eventId, /* scrollDeltaX= */ 0, track.totalShift);
      return;
    }
    track.previousStep = step;
    earlyScrollCheckHandler.postDelayed(() -> trackScroll(track), interval);
  }

  /**
   * Whether {@code bounds} end at an edge of {@code list}, as when the list cuts them off. Bounds
   * past the edge are a part that the app reports where it is, off screen.
   */
  @VisibleForTesting
  static boolean touchesEdge(Rect bounds, Rect list) {
    return Math.abs(bounds.top - list.top) <= 1 || Math.abs(bounds.bottom - list.bottom) <= 1;
  }

  @IntDef({LIST_MOVING, LIST_STOPPED, LIST_NOT_SCROLLED})
  @Retention(RetentionPolicy.SOURCE)
  @interface ListState {}

  static final int LIST_MOVING = 0;
  static final int LIST_STOPPED = 1;
  static final int LIST_NOT_SCROLLED = 2;

  // While the next item is watched for, a list counts as stopped once it has sent no scroll event
  // for LIST_STOPPED_MS, or WATCH_MAX_MS after the scroll.
  private static final long LIST_STOPPED_MS = 150;
  private static final long WATCH_MAX_MS = 1500;

  /**
   * Returns whether a list scrolled {@code elapsed} ms ago, which has sent {@code events} scroll
   * events since, the last {@code sinceLastEvent} ms ago, still moves, has stopped, or sent none.
   */
  @VisibleForTesting
  static @ListState int watchedListState(long elapsed, int events, long sinceLastEvent) {
    return watchedListState(elapsed, events, sinceLastEvent, /* oneEventList= */ false);
  }

  /**
   * As {@link #watchedListState(long, int, long)}, for a list that scrolls a page in one scroll
   * event when {@code oneEventList}: such a list has stopped a frame after its first event.
   */
  @VisibleForTesting
  static @ListState int watchedListState(
      long elapsed, int events, long sinceLastEvent, boolean oneEventList) {
    if (events <= 0) {
      return elapsed >= NO_MOVE_WAIT_MS ? LIST_NOT_SCROLLED : LIST_MOVING;
    }
    if (sinceLastEvent >= LIST_STOPPED_MS
        || elapsed >= WATCH_MAX_MS
        || (oneEventList && sinceLastEvent >= FRAME_MS)) {
      return LIST_STOPPED;
    }
    return LIST_MOVING;
  }

  // Lists such as Compose lazy lists jump a whole page at once and send one scroll event for it.
  // Once ONE_EVENT_SCROLLS page scrolls in a row of a list have each sent one event and then none
  // for LIST_STOPPED_MS, the list counts as stopped a frame after the first event of its later
  // scrolls, rather than after LIST_STOPPED_MS. Paging through an item taller than the list then
  // takes about a frame per page. An animated list sends its first scroll event at once and the
  // next one 100 ms later, but a scroll that ends within a frame, as near the end of the list, sends
  // one: hence more than one such scroll in a row. A scroll that sends more than one event, also
  // after one counted as stopped early, takes the list off.
  private static final int MAX_ONE_EVENT_LISTS = 8;
  private static final int ONE_EVENT_SCROLLS = 2;

  /** Page scrolls in a row that sent one scroll event, of the lists measured last. Main thread. */
  private static final LinkedHashMap<AccessibilityNodeInfoCompat, Integer> oneEventLists =
      new LinkedHashMap<>(MAX_ONE_EVENT_LISTS, 0.75f, /* accessOrder= */ true) {
        @Override
        protected boolean removeEldestEntry(
            Map.Entry<AccessibilityNodeInfoCompat, Integer> eldest) {
          return size() > MAX_ONE_EVENT_LISTS;
        }
      };

  /** Whether {@code list} is known to scroll a page in one scroll event. */
  static boolean isOneEventList(AccessibilityNodeInfoCompat list) {
    Integer scrolls = oneEventLists.get(list);
    return scrolls != null && scrolls >= ONE_EVENT_SCROLLS;
  }

  /**
   * Records whether {@code list}, whose page scroll has sent {@code events} scroll events, the last
   * {@code sinceLastEvent} ms ago, scrolled a page in one event.
   */
  static void learnOneEventList(AccessibilityNodeInfoCompat list, int events, long sinceLastEvent) {
    learnOneEventList(list, events, sinceLastEvent, /* deltaY= */ 0);
  }

  /**
   * As {@link #learnOneEventList(AccessibilityNodeInfoCompat, int, long)}. {@code deltaY}, the
   * page's sum of scroll deltas, doesn't tell: a delta of -1 is Android's mark for none.
   */
  static void learnOneEventList(
      AccessibilityNodeInfoCompat list, int events, long sinceLastEvent, int deltaY) {
    @Nullable Boolean oneEvent = oneEventScroll(events, sinceLastEvent);
    if (oneEvent == null) {
      return;
    }
    boolean known = isOneEventList(list);
    Integer scrolls = oneEventLists.get(list);
    oneEventLists.put(list, nextOneEventScrolls(scrolls == null ? 0 : scrolls, oneEvent));
    if (known != isOneEventList(list)) {
      LogUtils.d(
          TAG,
          known
              ? "The list sends several scroll events for a page."
              : "The list scrolls a page in one scroll event; later scrolls wait a frame.");
    }
  }

  /**
   * Returns the count of page scrolls in a row that sent one scroll event, after a scroll that did
   * if {@code oneEvent}, given {@code scrolls} before it.
   */
  @VisibleForTesting
  static int nextOneEventScrolls(int scrolls, boolean oneEvent) {
    return oneEvent ? Math.min(scrolls + 1, ONE_EVENT_SCROLLS) : 0;
  }

  /**
   * Takes the list of the last auto scroll off the lists known to scroll in one event if that
   * scroll, counted as stopped after its first event, has sent more than one.
   */
  private void checkOneEventScroll() {
    AccessibilityNodeInfoCompat list = oneEventScrollList;
    if (!scrollOfOneEventList || list == null) {
      return;
    }
    int events = scrollEventCount - scrollStartEventCount;
    if (events > 1) {
      oneEventScrollList = null;
      learnOneEventList(list, events, /* sinceLastEvent= */ 0);
    }
  }

  /**
   * Returns whether a page scroll that has sent {@code events} scroll events, the last {@code
   * sinceLastEvent} ms ago, was a scroll in one event, or null if that can't be told.
   */
  @VisibleForTesting
  static @Nullable Boolean oneEventScroll(int events, long sinceLastEvent) {
    if (events > 1) {
      return false;
    }
    if (events == 1 && sinceLastEvent >= LIST_STOPPED_MS) {
      return true;
    }
    return null;
  }

  /** Most lists whose page scroll is remembered. */
  private static final int MAX_PAGE_LISTS = 8;

  /** How far a page scroll moved each of the lists measured last. Main thread. */
  private static final LinkedHashMap<AccessibilityNodeInfoCompat, Integer> pages =
      new LinkedHashMap<>(MAX_PAGE_LISTS, 0.75f, /* accessOrder= */ true) {
        @Override
        protected boolean removeEldestEntry(
            Map.Entry<AccessibilityNodeInfoCompat, Integer> eldest) {
          return size() > MAX_PAGE_LISTS;
        }
      };

  /** Counts the scroll actions performed for navigation and scrolling ahead. Main thread. */
  private static int scrollActionCount;

  static int scrollActionCount() {
    return scrollActionCount;
  }

  static void countScrollAction() {
    scrollActionCount++;
  }

  /**
   * Records how far one page scroll of {@code list} moved it, {@code px} from its scroll events,
   * when the measurement can be trusted: the list was still when the scroll started ({@code
   * quietAtStart}), no other scroll started before its events ended ({@code otherScrollSince}), the
   * page is between half the list's height and its height, and the list can still scroll both ways.
   * The largest such page of the list is kept, as a page measured too short lets scrolling ahead
   * move the focused item off screen. A list whose page moved the focused item off screen is never
   * learned again (see {@link #distrustPage}).
   */
  static void learnPage(
      AccessibilityNodeInfoCompat list, int px, boolean quietAtStart, boolean otherScrollSince) {
    Rect bounds = new Rect();
    list.getBoundsInScreen(bounds);
    if (!isReliablePage(px, bounds.height(), quietAtStart, otherScrollSince)) {
      LogUtils.d(
          TAG,
          "Page of %d px not learned: list %d px tall, still at start %b, other scroll %b.",
          px,
          bounds.height(),
          quietAtStart,
          otherScrollSince);
      return;
    }
    Integer known = pages.get(list);
    if (known != null
        && (known == PAGE_NOT_MEASURABLE
            || (known >= px && sameHeight(pageHeights.get(list), bounds.height())))) {
      return;
    }
    // A page that reached the list's start or end can be shorter than a full one.
    if (!list.refresh()
        || !(AccessibilityNodeInfoUtils.supportsAction(
                list, AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD)
            || AccessibilityNodeInfoUtils.supportsAction(
                list, AccessibilityActionCompat.ACTION_SCROLL_DOWN.getId()))
        || !(AccessibilityNodeInfoUtils.supportsAction(
                list, AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD)
            || AccessibilityNodeInfoUtils.supportsAction(
                list, AccessibilityActionCompat.ACTION_SCROLL_UP.getId()))) {
      LogUtils.d(TAG, "Page of %d px not learned: the list is at its start or end.", px);
      return;
    }
    pages.put(list, px);
    pageHeights.put(list, bounds.height());
    LogUtils.d(TAG, "Learned a page of %d px for the list %d px tall.", px, bounds.height());
  }

  /**
   * Returns whether a measured page of {@code px} for a list {@code listHeight} tall is usable. A
   * page under half the list's height is taken to be measured in other units than pixels: the scroll
   * deltas of a Compose lazy list in FastSM added up to 1 or 2 for a page of a 2030 px list.
   */
  @VisibleForTesting
  static boolean isReliablePage(
      int px, int listHeight, boolean quietAtStart, boolean otherScrollSince) {
    return px > 0
        && px <= listHeight
        && px * 2 >= listHeight
        && quietAtStart
        && !otherScrollSince;
  }

  /** Marks a list in {@link #pages} whose page the scroll deltas don't measure. */
  private static final int PAGE_NOT_MEASURABLE = -1;

  /** Returns how far a page scroll moves {@code list}, or 0 if that isn't known. */
  static int learnedPage(AccessibilityNodeInfoCompat list) {
    Integer page = pages.get(list);
    if (page == null || page < 0) {
      return 0;
    }
    // A page learned while the list had another height, such as with the keyboard up, is wrong now.
    Rect bounds = new Rect();
    list.getBoundsInScreen(bounds);
    return sameHeight(pageHeights.get(list), bounds.height()) ? page : 0;
  }

  /** The heights of the lists in {@link #pages} when their pages were learned. Main thread. */
  private static final LinkedHashMap<AccessibilityNodeInfoCompat, Integer> pageHeights =
      new LinkedHashMap<>(MAX_PAGE_LISTS, 0.75f, /* accessOrder= */ true) {
        @Override
        protected boolean removeEldestEntry(
            Map.Entry<AccessibilityNodeInfoCompat, Integer> eldest) {
          return size() > MAX_PAGE_LISTS;
        }
      };

  /** Whether a list {@code height} px tall has the {@code learned} height, within 5%. */
  @VisibleForTesting
  static boolean sameHeight(@Nullable Integer learned, int height) {
    return learned != null && Math.abs(learned - height) * 20 <= Math.max(learned, height);
  }

  /**
   * Stops learning pages for {@code list}, after a page that scrolling ahead expected to leave the
   * focused item on screen moved it off: the list's scroll deltas don't measure its page.
   */
  static void distrustPage(AccessibilityNodeInfoCompat list) {
    Integer known = pages.put(list, PAGE_NOT_MEASURABLE);
    Rect bounds = new Rect();
    list.getBoundsInScreen(bounds);
    pageHeights.put(list, bounds.height());
    if (known == null || known != PAGE_NOT_MEASURABLE) {
      LogUtils.d(TAG, "Not learning pages of this list again: a page moved the focused item off.");
    }
  }

  /** Forgets the page learned for {@code list}. */
  static void forgetPage(AccessibilityNodeInfoCompat list) {
    pages.remove(list);
    pageHeights.remove(list);
  }

  /** Puts the bounds of {@code node} in {@code bounds} and returns whether they are empty. */
  private static boolean isEmpty(Rect bounds, AccessibilityNodeInfoCompat node) {
    node.getBoundsInScreen(bounds);
    return bounds.isEmpty();
  }

  /**
   * Scrolls the list at once, without building the window's order, when the focused item's list
   * item is as wide as the list, reaches its edge in {@code logicalDirection}, and has no other part
   * that takes focus, so the next item can only come from scrolling. Returns false to search as
   * usual, such as when the list can't scroll that way any more.
   */
  private boolean scrollFromListEndAtOnce(
      AccessibilityNodeInfoCompat pivot,
      NavigationAction navigationAction,
      @SearchDirection int logicalDirection,
      EventId eventId) {
    if (navigationAction.actionType != NavigationAction.DIRECTIONAL_NAVIGATION
        || navigationAction.targetType != NavigationTarget.TARGET_DEFAULT
        || !navigationAction.shouldScroll
        || !TraversalStrategyUtils.isLogicalDirection(navigationAction.searchDirection)
        || (logicalDirection != TraversalStrategy.SEARCH_FOCUS_FORWARD
            && logicalDirection != TraversalStrategy.SEARCH_FOCUS_BACKWARD)
        || ScreenElements.isShowing()
        || WebInterfaceUtils.supportsWebActions(pivot)) {
      return false;
    }
    ScrollableNodeInfo scrollableNodeInfo =
        ScrollableNodeInfo.findScrollableNodeForDirection(
            navigationAction.searchDirection,
            pivot,
            /* includeSelf= */ false,
            WindowUtils.isScreenLayoutRTL(service));
    if (scrollableNodeInfo == null) {
      return false;
    }
    AccessibilityNodeInfoCompat list = scrollableNodeInfo.getNode();
    if (!isItemList(list)) {
      return false;
    }
    AccessibilityNodeInfoCompat item = pivot;
    AccessibilityNodeInfoCompat parent = pivot.getParent();
    for (int depth = 0; parent != null && !parent.equals(list); depth++) {
      if (depth >= MAX_ITEM_DEPTH) {
        return false;
      }
      item = parent;
      parent = parent.getParent();
    }
    if (parent == null) {
      return false;
    }
    Rect listBounds = new Rect();
    list.getBoundsInScreen(listBounds);
    Rect itemBounds = new Rect();
    item.getBoundsInScreen(itemBounds);
    boolean forward = logicalDirection == TraversalStrategy.SEARCH_FOCUS_FORWARD;
    if (!forward) {
      // Going back from an item partly under a sticky header, the list's items start below the
      // header. An item starting right at the header's bottom has the header before it.
      int headerBottom = topBelowPinnedChild(list, listBounds);
      if (itemBounds.top < headerBottom) {
        listBounds.top = headerBottom;
      }
    }
    if (!endsListOnScreen(itemBounds, listBounds, forward)
        || hasItemBeyond(list, item, itemBounds, forward)
        || hasPartsTakingFocus(item, pivot)
        // Going back from a button inside a row that takes focus, the row comes before it.
        || (!forward
            && !item.equals(pivot)
            && AccessibilityNodeInfoUtils.takesFocusWhenVisible(item))) {
      return false;
    }
    if (!autoScroll(scrollableNodeInfo, pivot, navigationAction, eventId)) {
      return false;
    }
    LogUtils.d(TAG, "The focused item ends the list on screen; scrolled without building the order.");
    return true;
  }

  /**
   * Scrolls the native list holding the WebView of {@code webPivot} when that WebView's row ends the
   * list on screen in {@code logicalDirection} and no other row is past it, as at a list end with
   * the pivot itself. The search after the scroll goes on from the WebView. Returns whether it
   * scrolled.
   */
  private boolean scrollListHoldingWebView(
      AccessibilityNodeInfoCompat webPivot,
      NavigationAction navigationAction,
      @SearchDirection int logicalDirection,
      EventId eventId) {
    if (logicalDirection != SEARCH_FOCUS_FORWARD && logicalDirection != SEARCH_FOCUS_BACKWARD) {
      return false;
    }
    AccessibilityNodeInfoCompat webView = WebInterfaceUtils.ascendToWebView(webPivot);
    if (webView == null) {
      return false;
    }
    ScrollableNodeInfo scrollableNodeInfo =
        ScrollableNodeInfo.findScrollableNodeForDirection(
            navigationAction.searchDirection,
            webView,
            /* includeSelf= */ false,
            WindowUtils.isScreenLayoutRTL(service));
    if (scrollableNodeInfo == null || !isItemList(scrollableNodeInfo.getNode())) {
      return false;
    }
    AccessibilityNodeInfoCompat list = scrollableNodeInfo.getNode();
    AccessibilityNodeInfoCompat item = webView;
    AccessibilityNodeInfoCompat parent = webView.getParent();
    for (int depth = 0; parent != null && !parent.equals(list); depth++) {
      if (depth >= MAX_ITEM_DEPTH) {
        return false;
      }
      item = parent;
      parent = parent.getParent();
    }
    if (parent == null) {
      return false;
    }
    Rect listBounds = new Rect();
    list.getBoundsInScreen(listBounds);
    Rect itemBounds = new Rect();
    item.getBoundsInScreen(itemBounds);
    boolean forward = logicalDirection == SEARCH_FOCUS_FORWARD;
    if (!endsListOnScreen(itemBounds, listBounds, forward)
        || hasItemBeyond(list, item, itemBounds, forward)) {
      return false;
    }
    LogUtils.d(TAG, "The page in a list row has no item past this one; scrolling the list.");
    return autoScroll(scrollableNodeInfo, webView, navigationAction, eventId);
  }

  /** Whether {@code list} shows items of an adapter, which creates only the items on screen. */
  static boolean isItemList(AccessibilityNodeInfoCompat list) {
    if (!VerticalNavigation.scrollsVertically(list)
        || WebInterfaceUtils.supportsWebActions(list)
        || Role.getRole(list) == Role.ROLE_WEB_VIEW) {
      return false;
    }
    @RoleName int role = Role.getRole(list);
    if (list.getCollectionInfo() != null || role == Role.ROLE_LIST || role == Role.ROLE_GRID) {
      return true;
    }
    CharSequence className = list.getClassName();
    return className != null
        && (className.toString().endsWith("RecyclerView")
            || className.toString().endsWith("ListView"));
  }

  /**
   * Returns whether an item with {@code item} bounds is a row as wide as a list with {@code list}
   * bounds that reaches its bottom, going {@code forward}, or its top.
   */
  @VisibleForTesting
  static boolean endsListOnScreen(Rect item, Rect list, boolean forward) {
    if (item.isEmpty() || list.isEmpty() || item.width() * 10 < list.width() * 9) {
      return false;
    }
    return forward ? item.bottom >= list.bottom : item.top <= list.top;
  }

  /** Whether another item of {@code list} on screen lies past {@code item} going that way. */
  private static boolean hasItemBeyond(
      AccessibilityNodeInfoCompat list,
      AccessibilityNodeInfoCompat item,
      Rect itemBounds,
      boolean forward) {
    int children = Math.min(list.getChildCount(), MAX_LIST_CHILDREN_CHECKED);
    Rect bounds = new Rect();
    for (int i = 0; i < children; i++) {
      AccessibilityNodeInfoCompat child = list.getChild(i);
      if (child == null
          || child.equals(item)
          || !child.isVisibleToUser()
          || (!forward && pinnedChildren.containsKey(child))) {
        continue;
      }
      child.getBoundsInScreen(bounds);
      if (forward ? bounds.bottom > itemBounds.bottom : bounds.top < itemBounds.top) {
        return true;
      }
    }
    return false;
  }

  private static final int MAX_LIST_CHILDREN_CHECKED = 30;

  /**
   * Reads the order of the items of the window of {@code focused} now, unless it is saved already,
   * so that the next swipe needn't. Called while nothing else happens, such as after scrolling
   * ahead.
   */
  void prepareWindowOrder(AccessibilityNodeInfoCompat focused) {
    if (TraversalTreeCache.holdsCurrent(focused)) {
      return;
    }
    AccessibilityNodeInfoCompat root = AccessibilityNodeInfoUtils.getRoot(focused);
    if (root == null) {
      return;
    }
    LogUtils.d(TAG, "Reading the window's order before the next swipe.");
    TraversalTreeCache.rememberPivot(focused);
    buildWindowOrder(root, TraversalStrategy.SEARCH_FOCUS_FORWARD);
  }

  /** Most layers between a list and the focused node of one of its items. */
  private static final int MAX_ITEM_DEPTH = 3;

  /** At most this many nodes of a list item are read to look for parts that take focus. */
  private static final int MAX_ITEM_NODES_CHECKED = 100;

  /** The check for parts that take focus runs on the main thread and stops after this long. */
  private static final long MAX_ITEM_CHECK_MS = 30;

  /**
   * Returns the row after the focused item when the list can jump straight to it, or null to scroll
   * a page as usual. An item taller than the list took one scroll per screen, each waited for,
   * before the next item came into view. This is only done going forward, in a one-column list
   * running down the screen whose item holds the pivot and reaches the list's bottom, or ends
   * within {@link #JUMP_BOTTOM_SLACK_DP} of it, so that the next item isn't on screen, and has no
   * other part that takes focus on its own, which a jump would skip. Going backward a jump would
   * land on the top of an item taller than the list and skip its parts further down, which a page
   * scroll back reaches first.
   */
  private @Nullable Integer rowToJumpTo(
      AccessibilityNodeInfoCompat list,
      AccessibilityNodeInfoCompat pivot,
      @SearchDirection int direction) {
    if (direction != SEARCH_FOCUS_FORWARD && direction != SEARCH_FOCUS_DOWN) {
      return null;
    }
    if (!AccessibilityNodeInfoUtils.supportsAction(
        list, AccessibilityActionCompat.ACTION_SCROLL_TO_POSITION.getId())) {
      return null;
    }
    CollectionInfoCompat collection = list.getCollectionInfo();
    if (collection == null || collection.getColumnCount() != 1 || collection.getRowCount() < 2) {
      return null;
    }
    // In a list laid out in reverse, such as a chat, the next row is the one above.
    Boolean runUpward = indexesRunUpward(list);
    if (runUpward == null || runUpward) {
      LogUtils.d(TAG, "Not jumping: the list's order is reversed or not known yet.");
      return null;
    }
    // The list's item may be the pivot, or a node holding it in layouts that aren't focusable.
    AccessibilityNodeInfoCompat itemNode = pivot;
    AccessibilityNodeInfoCompat parent = pivot.getParent();
    for (int depth = 0; parent != null && !parent.equals(list); depth++) {
      if (depth >= MAX_ITEM_DEPTH) {
        LogUtils.d(TAG, "Not jumping: the focused node is too deep in the list's item.");
        return null;
      }
      itemNode = parent;
      parent = parent.getParent();
    }
    if (parent == null) {
      LogUtils.d(TAG, "Not jumping: the focused node isn't in the list.");
      return null;
    }
    CollectionItemInfoCompat item = itemNode.getCollectionItemInfo();
    if (item == null
        && itemNode != pivot
        && list.equals(AccessibilityNodeInfoUtils.getCollectionRoot(pivot))) {
      item = pivot.getCollectionItemInfo();
    }
    if (item == null) {
      LogUtils.d(TAG, "Not jumping: the list's item has no row.");
      return null;
    }
    int row = rowAfter(item.getRowIndex(), item.getRowSpan(), collection.getRowCount());
    if (row < 0) {
      return null;
    }
    Rect itemBounds = new Rect();
    itemNode.getBoundsInScreen(itemBounds);
    Rect listBounds = new Rect();
    list.getBoundsInScreen(listBounds);
    int slackPx = Math.round(JUMP_BOTTOM_SLACK_DP * density());
    if (!reachesListBottom(itemBounds, listBounds, slackPx)) {
      LogUtils.v(TAG, "Not jumping: the focused item doesn't reach the list's bottom.");
      return null;
    }
    if (hasPartsTakingFocus(itemNode, pivot)) {
      LogUtils.d(TAG, "Not jumping to row %d: the item has parts that take focus.", row);
      return null;
    }
    return row;
  }

  /**
   * Ends the swipe's scroll as failed, so that focus moves on past the list, when {@code
   * scrollable} has sent no scroll event in {@code elapsed} ms since the scroll and the focused item
   * is still its last row in the swipe's direction, shown whole. Some lists, such as a RecyclerView
   * stacked from its end, offer a scroll forward there, accept it, don't move and send no scroll
   * event, and the swipe then waited for the scroll's timeout. The scroll is still made, as some
   * apps load more items when their list is scrolled at its end; a list that has added rows since
   * keeps the usual wait. Returns whether it ended the scroll.
   */
  private boolean moveOnFromUnscrolledListEnd(
      AutoScrollCallback callback, AccessibilityNodeInfoCompat scrollable, long elapsed) {
    if (scrollCallback != callback
        || callback.sourceAction.actionType != NavigationAction.DIRECTIONAL_NAVIGATION) {
      return false;
    }
    AccessibilityNodeInfoCompat list = AccessibilityNodeInfoCompat.obtain(scrollable);
    AccessibilityNodeInfoCompat pivot = AccessibilityNodeInfoCompat.obtain(callback.pivot);
    if (!list.refresh() || !pivot.refresh()) {
      return false;
    }
    if (!endsListWhole(list, pivot, callback.sourceAction.searchDirection)) {
      // Lists without row numbers, such as Compose lists or Facebook's, can't tell their last row.
      // Give a busy app longer to start the scroll, then move on if nothing moved.
      // Timed from when the app answered the action, which a busy app takes long to do.
      long now = SystemClock.uptimeMillis();
      long start = Math.min(scrollActionReturnedUptime, now);
      long wait = Math.max(MIN_UNSCROLLED_CHECK_MS, start + UNSCROLLED_LIST_MS - now);
      earlyScrollCheckHandler.postDelayed(
          () -> moveOnIfListStillUnscrolled(callback, scrollable), wait);
      return false;
    }
    LogUtils.d(
        TAG,
        "The list sent no scroll event in %d ms and the focused item is its last row, fully"
            + " shown; moving on.",
        elapsed);
    int direction = callback.sourceAction.searchDirection;
    boolean forwardEnd =
        TraversalStrategyUtils.getLogicalDirection(direction, WindowUtils.isScreenLayoutRTL(service))
                == SEARCH_FOCUS_FORWARD
            || direction == SEARCH_FOCUS_DOWN;
    unmovingEndLists.put(list, unmovingEndBits(list) | endBit(forwardEnd));
    trackedCallback = null;
    onAutoScrollFailed(scrollable);
    return true;
  }

  /** How long after a scroll a list known not to move at its end is checked. */
  private static final long KNOWN_UNMOVING_END_MS = 3 * FRAME_MS;

  /**
   * Lists that accepted a scroll from their last row shown whole without moving, with {@link
   * #endBit} of the ends where they did. Main thread.
   */
  private static final LinkedHashMap<AccessibilityNodeInfoCompat, Integer> unmovingEndLists =
      new LinkedHashMap<>(8, 0.75f, /* accessOrder= */ true) {
        @Override
        protected boolean removeEldestEntry(
            Map.Entry<AccessibilityNodeInfoCompat, Integer> eldest) {
          return size() > 8;
        }
      };

  private static int endBit(boolean forward) {
    return forward ? 1 : 2;
  }

  private static int unmovingEndBits(AccessibilityNodeInfoCompat list) {
    Integer bits = unmovingEndLists.get(list);
    return bits == null ? 0 : bits;
  }

  /** How long after a scroll a list without row numbers counts as not scrolled by it. */
  private static final long UNSCROLLED_LIST_MS = 450;

  /** The least wait before that check, so that the scroll's first frame has come. */
  private static final long MIN_UNSCROLLED_CHECK_MS = 150;

  /**
   * Ends the scroll of {@code callback} as failed when {@code scrollable} has still sent no scroll
   * event and the pivot is where it was when the scroll started, without filling the list.
   */
  private void moveOnIfListStillUnscrolled(
      AutoScrollCallback callback, AccessibilityNodeInfoCompat scrollable) {
    if (scrollCallback != callback || scrollEventCount != scrollStartEventCount) {
      return;
    }
    AccessibilityNodeInfoCompat pivot = AccessibilityNodeInfoCompat.obtain(callback.pivot);
    AccessibilityNodeInfoCompat list = AccessibilityNodeInfoCompat.obtain(scrollable);
    if (!pivot.refresh() || !list.refresh() || !AccessibilityNodeInfoUtils.isVisible(pivot)) {
      return;
    }
    Rect now = new Rect();
    pivot.getBoundsInScreen(now);
    if (!now.equals(pivotBoundsAtScrollStart) || fillsList(now, list)) {
      return;
    }
    LogUtils.d(
        TAG,
        "The list sent no scroll event in %d ms and the focused item didn't move; moving on.",
        SystemClock.uptimeMillis() - lastAutoScrollUptime);
    trackedCallback = null;
    onAutoScrollFailed(scrollable);
  }

  /**
   * Whether the list item holding {@code pivot} is the last row of the one-column {@code list} in
   * {@code direction} and is shown whole inside the list, so that a scroll that way didn't show
   * anything new.
   */
  @SuppressWarnings("deprecation") // getBoundsInParent is the only unclipped size a node reports.
  private boolean endsListWhole(
      AccessibilityNodeInfoCompat list,
      AccessibilityNodeInfoCompat pivot,
      @SearchDirection int direction) {
    boolean forward;
    if (direction == SEARCH_FOCUS_FORWARD || direction == SEARCH_FOCUS_DOWN) {
      forward = true;
    } else if (direction == SEARCH_FOCUS_BACKWARD || direction == SEARCH_FOCUS_UP) {
      forward = false;
    } else {
      return false;
    }
    ListRow from = rowOf(pivot);
    CollectionInfoCompat collection = list.getCollectionInfo();
    if (from == null || !list.equals(from.list()) || collection == null) {
      return false;
    }
    Boolean upward = indexesRunUpward(list);
    if (upward == null) {
      return false;
    }
    if (!isPastLastRow(rowAfterMove(from, forward, upward), collection.getRowCount())) {
      return false;
    }
    AccessibilityNodeInfoCompat item = pivot;
    AccessibilityNodeInfoCompat parent = pivot.getParent();
    for (int depth = 0; parent != null && !parent.equals(list); depth++) {
      if (depth >= MAX_ITEM_DEPTH) {
        return false;
      }
      item = parent;
      parent = parent.getParent();
    }
    if (parent == null || !item.isVisibleToUser()) {
      return false;
    }
    Rect size = new Rect();
    item.getBoundsInParent(size);
    Rect shown = new Rect();
    item.getBoundsInScreen(shown);
    Rect listBounds = new Rect();
    list.getBoundsInScreen(listBounds);
    return isShownWhole(size, shown, listBounds);
  }

  /** Whether {@code row}, the row a move goes to, is outside a collection of {@code rowCount}. */
  @VisibleForTesting
  static boolean isPastLastRow(int row, int rowCount) {
    return rowCount > 0 && (row < 0 || row >= rowCount);
  }

  /**
   * Whether an item of unclipped {@code size}, shown at {@code shown}, is shown whole inside a
   * list at {@code list}.
   */
  @VisibleForTesting
  static boolean isShownWhole(Rect size, Rect shown, Rect list) {
    return !shown.isEmpty()
        && shown.height() >= size.height()
        && shown.top >= list.top
        && shown.bottom <= list.bottom;
  }

  /**
   * Returns whether the indexes of {@code list} run up the screen, or null if that isn't known. A
   * list showing one item can't show it, so the last list that did is remembered.
   */
  private @Nullable Boolean indexesRunUpward(AccessibilityNodeInfoCompat list) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
      return null;
    }
    Boolean onScreen = AutoScrollSuccessCheckerImpl.indexesRunUpwardIfKnown(list);
    if (onScreen != null) {
      listWithKnownOrder = list;
      listWithKnownOrderRunsUpward = onScreen;
      return onScreen;
    }
    return list.equals(listWithKnownOrder) ? listWithKnownOrderRunsUpward : null;
  }

  /**
   * Returns the row after an item at {@code rowIndex} spanning {@code rowSpan} rows, or -1 if the
   * collection has no such row.
   */
  @VisibleForTesting
  static int rowAfter(int rowIndex, int rowSpan, int rowCount) {
    if (rowIndex < 0 || rowIndex >= rowCount) {
      return -1;
    }
    int row = rowIndex + Math.max(1, rowSpan);
    return row < rowCount ? row : -1;
  }

  /** A row of a one-column list: the list, the row's index and how many rows it spans. */
  @VisibleForTesting
  record ListRow(AccessibilityNodeInfoCompat list, int row, int span) {}

  /** The row that the swipe in progress started from, read when its first scroll started. */
  private @Nullable ListRow swipePivotRow;

  /**
   * Returns the row of the one-column list item that is {@code node} or holds it within {@link
   * #MAX_ITEM_DEPTH} layers, or null if it isn't in such a list.
   */
  @VisibleForTesting
  static @Nullable ListRow rowOf(@Nullable AccessibilityNodeInfoCompat node) {
    AccessibilityNodeInfoCompat item = node;
    for (int depth = 0; item != null && depth <= MAX_ITEM_DEPTH; depth++) {
      CollectionItemInfoCompat info = item.getCollectionItemInfo();
      if (info != null) {
        AccessibilityNodeInfoCompat list = item.getParent();
        CollectionInfoCompat collection = (list == null) ? null : list.getCollectionInfo();
        if (collection == null || collection.getColumnCount() != 1 || info.getRowIndex() < 0) {
          return null;
        }
        return new ListRow(list, info.getRowIndex(), Math.max(1, info.getRowSpan()));
      }
      item = item.getParent();
    }
    return null;
  }

  /**
   * Returns the row that a move {@code forward}, or back, goes to from {@code from}, in a list
   * whose indexes run up the screen if {@code indexesRunUpward}.
   */
  @VisibleForTesting
  static int rowAfterMove(ListRow from, boolean forward, boolean indexesRunUpward) {
    return (forward == indexesRunUpward) ? from.row() - 1 : from.row() + from.span();
  }

  /** Returns the child of {@code list} on screen that is its item at {@code row}, if any. */
  @VisibleForTesting
  static @Nullable AccessibilityNodeInfoCompat itemAtRow(
      AccessibilityNodeInfoCompat list, int row) {
    int count = Math.min(list.getChildCount(), MAX_LIST_CHILDREN_CHECKED);
    for (int i = 0; i < count; i++) {
      AccessibilityNodeInfoCompat child = list.getChild(i);
      if (child == null || !child.isVisibleToUser()) {
        continue;
      }
      CollectionItemInfoCompat info = child.getCollectionItemInfo();
      if (info != null && info.getRowIndex() == row) {
        return child;
      }
    }
    return null;
  }

  /**
   * Returns whether {@code found}, a row of {@code list} or null if the found node is in none, lies
   * past {@code nextRow}, the row after {@code fromRow} going the swipe's way.
   */
  @VisibleForTesting
  static boolean liesPastRow(
      @Nullable ListRow found, AccessibilityNodeInfoCompat list, int fromRow, int nextRow) {
    if (found == null || !list.equals(found.list())) {
      return false;
    }
    return (nextRow > fromRow) ? found.row() > nextRow : found.row() < nextRow;
  }

  /**
   * Returns the node to focus in {@code item}, as a swipe in the logical {@code direction} would
   * reach it: going forward, the item or its first part that takes focus; going back, its last
   * part that takes focus, or the item. Null if nothing in it does.
   */
  @VisibleForTesting
  static @Nullable AccessibilityNodeInfoCompat focusIn(
      AccessibilityNodeInfoCompat item,
      TraversalStrategy strategy,
      @SearchDirection int direction,
      Filter<AccessibilityNodeInfoCompat> filter) {
    AccessibilityNodeInfoCompat target =
        TraversalStrategyUtils.findFirstFocusInNodeTree(strategy, item, direction, filter);
    if (target == null
        || !(target.equals(item) || AccessibilityNodeInfoUtils.hasAncestor(target, item))
        || !AccessibilityNodeInfoUtils.isVisible(target)) {
      return null;
    }
    return target;
  }

  /**
   * Returns the node to focus after a swipe's scroll of {@code list}: {@code found}, or, in a
   * one-column list, the row after the one the swipe started from, when that row is on screen and
   * {@code found} lies past it or is null. A search from a starting item whose view the list has
   * reused for a later row, or from the first item on screen after a scroll that went too far, would
   * otherwise skip the rows in between, and finding nothing would scroll another page.
   */
  private @Nullable AccessibilityNodeInfoCompat keepRowOrder(
      AccessibilityNodeInfoCompat list,
      @Nullable AccessibilityNodeInfoCompat found,
      @SearchDirection int searchDirection,
      String when) {
    ListRow from = swipePivotRow;
    if (from == null || !from.list().equals(list)) {
      return found;
    }
    int logicalDirection =
        TraversalStrategyUtils.getLogicalDirection(
            searchDirection, WindowUtils.isScreenLayoutRTL(service));
    if (logicalDirection != SEARCH_FOCUS_FORWARD && logicalDirection != SEARCH_FOCUS_BACKWARD) {
      return found;
    }
    // Only a node more than a row away, or none, is checked further.
    ListRow foundRow = (found == null) ? null : rowOf(found);
    if (found != null
        && (foundRow == null || Math.abs(foundRow.row() - from.row()) <= from.span())) {
      return found;
    }
    Boolean upward = indexesRunUpward(list);
    if (upward == null) {
      return found;
    }
    int nextRow = rowAfterMove(from, logicalDirection == SEARCH_FOCUS_FORWARD, upward);
    if (found != null && !liesPastRow(foundRow, list, from.row(), nextRow)) {
      return found;
    }
    AccessibilityNodeInfoCompat item = itemAtRow(list, nextRow);
    AccessibilityNodeInfoCompat target = null;
    if (item != null) {
      TraversalStrategy strategy =
          TraversalStrategyUtils.getTraversalStrategy(list, focusFinder, logicalDirection);
      target =
          focusIn(
              item,
              strategy,
              logicalDirection,
              NavigationTarget.createNodeFilter(
                  NavigationTarget.TARGET_DEFAULT, strategy.getSpeakingNodesCache()));
    }
    if (target == null) {
      if (found != null) {
        LogUtils.i(
            TAG,
            "%s: row %d after row %d; row %d is not on screen or takes no focus.",
            when,
            foundRow.row(),
            from.row(),
            nextRow);
      }
      return found;
    }
    LogUtils.i(
        TAG,
        "%s: focusing row %d after row %d in place of %s.",
        when,
        nextRow,
        from.row(),
        found == null ? "another scroll" : "row " + foundRow.row());
    return target;
  }

  /** Describes the list row of {@code node} for the log. */
  private static String rowDescription(@Nullable AccessibilityNodeInfoCompat node) {
    ListRow row = rowOf(node);
    return (row == null) ? "a node in no list row" : "row " + row.row();
  }

  /**
   * A focused item ending this close to the list's bottom counts as reaching it. Android Settings
   * can leave a few pixels below its last row on screen.
   */
  private static final float JUMP_BOTTOM_SLACK_DP = 12;

  /** Returns whether {@code itemBounds} end within {@code slackPx} of {@code listBounds}' bottom. */
  @VisibleForTesting
  static boolean reachesListBottom(Rect itemBounds, Rect listBounds, int slackPx) {
    return !listBounds.isEmpty()
        && !itemBounds.isEmpty()
        && itemBounds.bottom >= listBounds.bottom - slackPx;
  }

  /**
   * Returns whether a descendant of {@code item} other than {@code pivot}, on screen or not, can
   * take focus on its own, or whether there are too many nodes to tell.
   */
  @VisibleForTesting
  static boolean hasPartsTakingFocus(
      AccessibilityNodeInfoCompat item, AccessibilityNodeInfoCompat pivot) {
    long deadline = SystemClock.uptimeMillis() + MAX_ITEM_CHECK_MS;
    ArrayDeque<AccessibilityNodeInfoCompat> queue = new ArrayDeque<>();
    queue.add(item);
    int read = 0;
    while (!queue.isEmpty()) {
      AccessibilityNodeInfoCompat node = queue.poll();
      if (node != item
          && !node.equals(pivot)
          && AccessibilityNodeInfoUtils.takesFocusWhenVisible(node)) {
        return true;
      }
      int children = node.getChildCount();
      for (int i = 0; i < children; i++) {
        if (++read > MAX_ITEM_NODES_CHECKED || SystemClock.uptimeMillis() > deadline) {
          return true;
        }
        AccessibilityNodeInfoCompat child = node.getChild(i);
        if (child != null) {
          queue.add(child);
        }
      }
    }
    return false;
  }

  /** Shows {@code row} of {@code list} at once, then moves focus as after an auto scroll. */
  private boolean jumpToRow(
      AccessibilityNodeInfoCompat list,
      AccessibilityNodeInfoCompat pivot,
      int row,
      NavigationAction navigationAction,
      EventId eventId) {
    LogUtils.i(TAG, "Jumping to row %d of the list.", row);
    Rect pivotBefore = new Rect();
    pivot.getBoundsInScreen(pivotBefore);
    boolean scrolled =
        performActionScrollToPosition(
            list, pivot, Pair.create(row, 0), navigationAction, eventId);
    if (scrolled) {
      AutoScrollCallback callback = scrollCallback;
      callback.isJump = true;
      lastScrollWasJump = true;
      earlyScrollCheckHandler.post(
          () -> checkScrollEarly(callback, list, pivotBefore, eventId, EARLY_JUMP_RETRIES));
    }
    return scrolled;
  }

  /**
   * Continues the navigation as soon as the pivot has moved, instead of waiting for the scroll
   * event. Android sends scroll events at most every 100 ms, but lists usually scroll before the
   * action returns or within a few frames. If nothing has moved yet, checks again every {@link
   * #EARLY_SCROLL_RETRY_MS}, up to {@code retries} times, and then leaves it to the scroll event,
   * which is ignored if this check already handled the scroll.
   */
  private void checkScrollEarly(
      AutoScrollCallback callback,
      AccessibilityNodeInfoCompat scrollable,
      Rect pivotBefore,
      EventId eventId,
      int retries) {
    if (scrollCallback != callback) {
      return;
    }
    AccessibilityNodeInfoCompat pivotNow = AccessibilityNodeInfoCompat.obtain(callback.pivot);
    Rect pivotAfter = new Rect();
    boolean pivotPresent = pivotNow.refresh();
    if (pivotPresent) {
      pivotNow.getBoundsInScreen(pivotAfter);
      if (pivotAfter.equals(pivotBefore)) {
        // A focused item that fills the list looks still while the list moves; its scroll events
        // tell when the list slows down.
        if (!callback.isJump && fillsList(pivotAfter, scrollable)) {
          searchWhenFilledListSlows(callback, scrollable, eventId);
          return;
        }
        if (retries > 0) {
          earlyScrollCheckHandler.postDelayed(
              () -> checkScrollEarly(callback, scrollable, pivotBefore, eventId, retries - 1),
              EARLY_SCROLL_RETRY_MS);
          return;
        }
        // A jump that changed nothing may send no scroll event at all, and waiting for the failure
        // timeout would move focus out of the list. Continue, so that a page scroll follows.
        if (!callback.isJump) {
          return;
        }
        LogUtils.d(TAG, "checkScrollEarly: the jump left the focused item where it was.");
      }
    }
    int deltaX =
        pivotPresent
            ? edgeShift(pivotBefore.left, pivotAfter.left, pivotBefore.right, pivotAfter.right)
            : DELTA_UNDEFINED;
    int deltaY =
        pivotPresent
            ? edgeShift(pivotBefore.top, pivotAfter.top, pivotBefore.bottom, pivotAfter.bottom)
            : DELTA_UNDEFINED;
    LogUtils.d(TAG, "checkScrollEarly: scrolled, deltaX=%s deltaY=%s", deltaX, deltaY);
    scrollable.refresh();
    readListAgain(scrollable);
    onAutoScrolled(scrollable, eventId, deltaX, deltaY);
  }

  /**
   * Continues the navigation once the list scrolled for {@code callback}, whose focused item fills
   * it, has slowed down, rather than when its scroll events stop. If the list has sent no scroll
   * event by then, leaves it to the scroll event or its timeout.
   */
  private void searchWhenFilledListSlows(
      AutoScrollCallback callback, AccessibilityNodeInfoCompat scrollable, EventId eventId) {
    if (scrollCallback != callback) {
      return;
    }
    if (listStillMoving()) {
      earlyScrollCheckHandler.postDelayed(
          () -> searchWhenFilledListSlows(callback, scrollable, eventId), MOVING_CHECK_MS);
      return;
    }
    int events = scrollEventCount - scrollStartEventCount;
    if (events == 0) {
      if (!moveOnFromUnscrolledListEnd(
          callback, scrollable, SystemClock.uptimeMillis() - lastAutoScrollUptime)) {
        LogUtils.d(TAG, "The list sent no scroll event; waiting for it.");
      }
      return;
    }
    learnOneEventList(scrollable, events, SystemClock.uptimeMillis() - lastScrollEventUptime);
    logListSlowed();
    int deltaY = scrollDeltaSumY - scrollStartDeltaSumY;
    scrollable.refresh();
    readListAgain(scrollable);
    onAutoScrolled(scrollable, eventId, /* scrollDeltaX= */ 0, deltaY != 0 ? deltaY : 1);
  }

  /**
   * Returns how far a node moved between two bounds along one axis, as a scroll delta: positive
   * when it moved up or left. A node cut off at the list's start edge keeps that edge at the list's
   * edge while it moves, so the end edge gives the distance then.
   */
  @VisibleForTesting
  static int edgeShift(int startBefore, int startAfter, int endBefore, int endAfter) {
    int shift = startBefore - startAfter;
    return shift != 0 ? shift : endBefore - endAfter;
  }

  /**
   * Reads the items of {@code scrollable}, which has just scrolled, from the app again. Android's
   * node cache keeps them as they were until the list's scroll events, which clear it, reach this
   * thread, so a search before then would find rows at their old places, or a reused row with the
   * text it had. The items are read on several threads at once, so this costs about one frame of
   * the app's. Android 12 and older can't clear part of the cache, so this does nothing there.
   */
  private void readListAgain(AccessibilityNodeInfoCompat scrollable) {
    if (!BuildVersionUtils.isAtLeastT()) {
      return;
    }
    service.clearCachedSubtree(scrollable.unwrap());
    WindowNodePrefetcher.prefetch(scrollable, service.getPackageName());
  }

  /**
   * Delivers an {@link AutoScrollSuccessChecker} with given navigation action and scroll action if
   * necessary.
   *
   * <p>To simplify the logic, we do it for phone only. For non-default granularity, it might need
   * multiple auto-scrolls to find the target, so we don't create the checker for it.
   */
  @VisibleForTesting
  @Nullable AutoScrollSuccessChecker createAutoScrollCheckerIfNeeded(
      AccessibilityNodeInfoCompat scrollableNode,
      NavigationAction navigationAction,
      int scrollAction) {
    if (FormFactorUtils.isAndroidWear() || FormFactorUtils.isAndroidTv()) {
      return null;
    }

    if (navigationAction.targetType != NavigationTarget.TARGET_DEFAULT) {
      return null;
    }
    if (getSuccessAutoScrollPercentageThreshold(service) == 0f || !BuildVersionUtils.isAtLeastP()) {
      return null;
    }

    if (scrollAction != AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD
        && scrollAction != AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD) {
      return null;
    }
    final Rect nodeBounds = new Rect();
    scrollableNode.getBoundsInScreen(nodeBounds);
    return new AutoScrollSuccessCheckerImpl(
        scrollAction,
        nodeBounds,
        getSuccessAutoScrollPercentageThreshold(service),
        AutoScrollSuccessCheckerImpl.indexesRunUpward(scrollableNode));
  }

  private boolean performScrollActionInternal(
      @UserAction int userAction,
      @NonNull AccessibilityNodeInfoCompat scrollableNode,
      @NonNull AccessibilityNodeInfoCompat pivotNode,
      int scrollAction,
      NavigationAction sourceAction,
      EventId eventId) {
    return performScrollActionInternal(
        userAction,
        scrollableNode,
        pivotNode,
        scrollAction,
        sourceAction,
        ScrollTimeout.SCROLL_TIMEOUT_SHORT,
        /* autoScrollSuccessChecker= */ null,
        eventId);
  }

  private boolean performScrollActionInternal(
      @UserAction int userAction,
      @NonNull AccessibilityNodeInfoCompat scrollableNode,
      @NonNull AccessibilityNodeInfoCompat pivotNode,
      int scrollAction,
      NavigationAction sourceAction,
      ScrollTimeout scrollTimeout,
      @Nullable AutoScrollSuccessChecker autoScrollSuccessChecker,
      EventId eventId) {
    long autoScrollUptimeBefore = lastAutoScrollUptime;
    if ((sourceAction.actionType == NavigationAction.SCROLL_BACKWARD
            || sourceAction.actionType == NavigationAction.SCROLL_FORWARD)
        && !AccessibilityNodeInfoUtils.hasAncestor(pivotNode, scrollableNode)) {
      // Don't update a11y focus in callback if pivot is not a descendant of scrollable node.
      scrollCallback = null;
    } else {
      scrollCallback = new AutoScrollCallback(this, sourceAction, pivotNode);
    }
    boolean scrolled =
        pipeline.returnFeedback(
            eventId,
            Feedback.scroll(
                scrollableNode,
                userAction,
                scrollAction,
                ScrollActionRecord.FOCUS,
                scrollTimeout,
                sourceAction.autoScrollAttempt,
                autoScrollSuccessChecker));
    scrollActionReturnedUptime = SystemClock.uptimeMillis();
    if (!scrolled) {
      dropFailedScrollCallback(autoScrollUptimeBefore);
    }
    return scrolled;
  }

  /** Determines feedback for auto-scroll success after directional-navigation action. */
  public void onAutoScrolled(
      @NonNull AccessibilityNodeInfoCompat scrolledNode,
      EventId eventId,
      int scrollDeltaX,
      int scrollDeltaY) {
    if (scrollCallback != null && scrollCallback == trackedCallback) {
      // The followed node decides when the list has slowed down, not the scroll events.
      LogUtils.v(TAG, "onAutoScrolled ignored while a node is followed.");
      return;
    }
    if (scrollCallback != null) {
      AccessibilityNodeInfoCompat currentFocus =
          accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
      final AutoScrollCallback autoScrollCallback = scrollCallback;
      scrollCallback = null;
      if (currentFocus == null
          || currentFocus.equals(autoScrollCallback.pivot)
          || fellToContainer(currentFocus, scrolledNode, autoScrollCallback.pivot)) {
        // Prevent changing the focus if it has already been changed, e.g., by another actor.
        // In the onAutoScrolled method, we will assign a new scrollCallback.
        autoScrollCallback.onAutoScrolled(scrolledNode, eventId, scrollDeltaX, scrollDeltaY);
      } else {
        LogUtils.w(TAG, "Skip onAutoScrolled because currentFocus is assigned by another actor.");
      }
    }
  }

  /**
   * Whether {@code focus} is the scrolled list, or a container holding the pivot, which focus falls
   * to when the scroll takes the pivot's view away. Android's file picker moved focus to its file
   * list when a jump to the next row recycled the focused row; that is not focus set elsewhere.
   */
  private static boolean fellToContainer(
      AccessibilityNodeInfoCompat focus,
      AccessibilityNodeInfoCompat scrolledNode,
      AccessibilityNodeInfoCompat pivot) {
    if (focus.equals(scrolledNode)) {
      LogUtils.d(TAG, "Focus fell to the scrolled list; going on from the scroll.");
      return true;
    }
    // The scroll went from a WebView row while focus is on the page's last item inside it.
    if (Role.getRole(pivot) == Role.ROLE_WEB_VIEW
        && AccessibilityNodeInfoUtils.hasAncestor(focus, pivot)) {
      return true;
    }
    if (AccessibilityNodeInfoUtils.hasAncestor(pivot, focus)
        && AccessibilityNodeInfoUtils.hasAncestor(scrolledNode, focus)) {
      LogUtils.d(TAG, "Focus fell to a container of the list; going on from the scroll.");
      return true;
    }
    return false;
  }

  /** Determines feedback for auto-scroll failure after directional-navigation action. */
  public void onAutoScrollFailed(@NonNull AccessibilityNodeInfoCompat scrolledNode) {
    if (scrollCallback != null) {
      scrollCallback.onAutoScrollFailed(scrolledNode);
      scrollCallback = null;
    }
  }

  private void handleViewScrolledForScrollNavigationAction(
      @NonNull AccessibilityNodeInfoCompat scrolledNode,
      NavigationAction sourceAction,
      EventId eventId) {
    AccessibilityNodeInfoCompat currentFocus =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    boolean hasValidA11yFocus = AccessibilityNodeInfoUtils.isVisible(currentFocus);
    if (hasValidA11yFocus && !AccessibilityNodeInfoUtils.hasAncestor(currentFocus, scrolledNode)) {
      // Do nothing if there is a valid focus outside of the scrolled container.
      return;
    }
    // 1. Visible, inside scrolledNode
    // 2. Invisible, no focus.
    TraversalStrategy traversalStrategy =
        TraversalStrategyUtils.getTraversalStrategy(
            scrolledNode, focusFinder, TraversalStrategy.SEARCH_FOCUS_FORWARD);
    // Use TARGET_DEFAULT regardless of the current granularity.
    Filter<AccessibilityNodeInfoCompat> nodeFilter =
        NavigationTarget.createNodeFilter(
            NavigationTarget.TARGET_DEFAULT, traversalStrategy.getSpeakingNodesCache());
    @SearchDirection
    int direction =
        (sourceAction.actionType == NavigationAction.SCROLL_FORWARD)
            ? TraversalStrategy.SEARCH_FOCUS_FORWARD
            : TraversalStrategy.SEARCH_FOCUS_BACKWARD;

    AccessibilityNodeInfoCompat nodeToFocus;
    if (hasValidA11yFocus) {
      nodeToFocus =
          TraversalStrategyUtils.searchFocus(
              traversalStrategy, currentFocus, direction, nodeFilter);
    } else {
      nodeToFocus =
          TraversalStrategyUtils.findFirstFocusInNodeTree(
              traversalStrategy, scrolledNode, direction, nodeFilter);
    }
    if (nodeToFocus != null) {
      setAccessibilityFocusInternal(nodeToFocus, sourceAction, eventId);
    }
  }

  /**
   * Called when we receive result event for auto-scroll action with macro granularity target.
   *
   * <p><b>Warning:</b> Do not rely too much logic on {@code focusBeforeScroll}. It is possible in
   * RecyclerView when {@code focusBeforeScroll} goes off screen, and after calling {@link
   * AccessibilityNodeInfoCompat#refresh()}, the node is reused and pointed to another emerging list
   * item. This is how RecyclerView "recycles" views and we cannot get rid of it. If using this
   * field, please refresh and validate the node to ensure it is identical with what it was before
   * scroll action.
   */
  private void handleViewAutoScrolledForDirectionalNavigationWithMacroGranularityTarget(
      @NonNull AccessibilityNodeInfoCompat scrolledNode,
      NavigationAction sourceAction,
      EventId eventId) {

    NavigationAction navigationAction =
        NavigationAction.Builder.copy(sourceAction)
            .setAutoScrollAttempt(sourceAction.autoScrollAttempt + 1)
            .build();

    int logicalDirection =
        TraversalStrategyUtils.getLogicalDirection(
            navigationAction.searchDirection, WindowUtils.isScreenLayoutRTL(service));

    // Try to find the next focusable node based on current focus.
    // For native macro granularity, we try to find the reference node to start the search from.
    // This workaround is required due to REFERTO, else we can use focusBeforeScroll
    // as the start node even for macro granularity.
    // TODO: Remove this workaround after REFERTO is fixed.
    AccessibilityNodeInfoCompat refNode =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    if (!AccessibilityNodeInfoUtils.isVisible(refNode)) {
      refNode = null;
    }
    // Local TraversalStrategy generated in sub-tree of a refreshed scrolledNode.
    TraversalStrategy localTraversalStrategy =
        TraversalStrategyUtils.getTraversalStrategy(
            scrolledNode, focusFinder, sourceAction.searchDirection);
    if (refNode == null) {
      // Start from the first focusable node of the Scrollable by the traversal strategy direction.
      refNode =
          TraversalStrategyUtils.findFirstFocusInNodeTree(
              localTraversalStrategy,
              scrolledNode,
              sourceAction.searchDirection,
              FILTER_SHOULD_FOCUS.and(
                  new Filter<AccessibilityNodeInfoCompat>() {
                    @Override
                    public boolean accept(AccessibilityNodeInfoCompat node) {
                      return !scrolledNode.equals(node);
                    }
                  }));
    }
    if (refNode == null) {
      LogUtils.w(
          TAG,
          "handleViewAutoScrolledForDirectionalNavigationWithMacroGranularityTarget returns due to"
              + " refNode is null");
      return;
    }
    if (sourceAction.targetType == NavigationTarget.TARGET_HTML_ELEMENT_LIST_ITEM
        && handleViewAutoScrolledForListItem(
            scrolledNode, refNode, navigationAction, logicalDirection, eventId)) {
      return;
    }
    Filter<AccessibilityNodeInfoCompat> nodeFilter =
        NavigationTarget.createNodeFilter(
            sourceAction.targetType, localTraversalStrategy.getSpeakingNodesCache());

    // Only if the refNode does not satisfy the desired macro granularity target type or is
    // default granularity, we look for the next target starting from
    // refNode. Else, just set the focus to refNode.
    AccessibilityNodeInfoCompat nodeToFocus;
    if (nodeFilter.accept(refNode) && !refNode.isAccessibilityFocused()) {
      nodeToFocus = refNode;
    } else if (NavigationTarget.isContainerTarget(sourceAction.targetType)) {
      nodeToFocus = findContainerTarget(refNode, sourceAction);

      if (currentContainer(
              nodeToFocus, NavigationTarget.createContainerFilter(sourceAction.targetType))
          == null) {
        // Lists, tables and landmarks can be further than one scroll away, as other elements can.
        if (NavigationTarget.isWebOnlyTarget(sourceAction.targetType)
            && shouldKeepSearch(navigationAction)
            && performScrollActionInternal(
                ScrollActionRecord.ACTION_AUTO_SCROLL,
                scrolledNode,
                refNode,
                TraversalStrategyUtils.convertSearchDirectionToScrollAction(
                    navigationAction.searchDirection),
                navigationAction,
                ScrollTimeout.SCROLL_TIMEOUT_LONG,
                /* autoScrollSuccessChecker= */ null,
                eventId)) {
          return;
        }
        announceNativeElement(logicalDirection, navigationAction.targetType, eventId);
        // The scroll may have moved the focused item off screen. Keep focus in the scrolled view.
        if (nodeToFocus == null) {
          nodeToFocus = findInitialFocusAfterScrolled(refNode, sourceAction, localTraversalStrategy);
        }
      }
    } else {
      nodeToFocus =
          TraversalStrategyUtils.searchFocus(
              localTraversalStrategy, refNode, sourceAction.searchDirection, nodeFilter);
      setLastScrolledNodeForNativeMacroGranularity(scrolledNode);
      if (nodeToFocus == null) {
        boolean scrollSuccess = false;
        if (shouldKeepSearch(navigationAction)) {
          scrollSuccess =
              performScrollActionInternal(
                  ScrollActionRecord.ACTION_AUTO_SCROLL,
                  scrolledNode,
                  refNode,
                  TraversalStrategyUtils.convertSearchDirectionToScrollAction(
                      navigationAction.searchDirection),
                  navigationAction,
                  ScrollTimeout.SCROLL_TIMEOUT_LONG,
                  /* autoScrollSuccessChecker= */ null,
                  eventId);
        }

        if (scrollSuccess) {
          // Early return if next scroll action performed.
          return;
        }

        // Fallback to focus on the target outside of scrollable node.
        if (navigationAction.fallbackTarget != null) {
          navigationAction.fallbackTarget.refresh();
          if (FILTER_SHOULD_FOCUS.accept(navigationAction.fallbackTarget)) {
            setAccessibilityFocusInternal(
                navigationAction.fallbackTarget, navigationAction, eventId);
            return;
          }
        }

        nodeToFocus = findInitialFocusAfterScrolled(refNode, sourceAction, localTraversalStrategy);

        announceNativeElement(logicalDirection, navigationAction.targetType, eventId);
      }
    }

    if (nodeToFocus != null) {
      setAccessibilityFocusInternal(nodeToFocus, navigationAction, eventId);
    }
  }

  /**
   * Moves by list item after a scroll made for it, when {@code refNode} is in a list: to the item
   * after or before the focused one, or, when the scroll took the focused item off screen, to the
   * item of {@code refNode}, the first one on screen in the direction of the move. Scrolls again
   * when no item has come on screen yet. Returns false when {@code refNode} isn't in a list.
   */
  private boolean handleViewAutoScrolledForListItem(
      @NonNull AccessibilityNodeInfoCompat scrolledNode,
      @NonNull AccessibilityNodeInfoCompat refNode,
      NavigationAction navigationAction,
      @SearchDirection int logicalDirection,
      EventId eventId) {
    boolean focused = refNode.isAccessibilityFocused();
    // Without focus, the first node on screen can be in a list inside an item, such as a row of
    // pictures in a post, so the scrolled list is the one to move in.
    @Nullable
    AccessibilityNodeInfoCompat list =
        !focused
                && ListItemNavigation.isList(scrolledNode)
                && AccessibilityNodeInfoUtils.hasAncestor(refNode, scrolledNode)
            ? scrolledNode
            : ListItemNavigation.listOf(refNode);
    if (list == null) {
      return false;
    }
    @Nullable
    AccessibilityNodeInfoCompat nodeToFocus =
        focused
            ? ListItemNavigation.target(
                refNode, list, /* forward= */ logicalDirection == SEARCH_FOCUS_FORWARD)
            : ListItemNavigation.itemTarget(refNode, list);
    if (nodeToFocus != null) {
      setAccessibilityFocusInternal(nodeToFocus, navigationAction, eventId);
      return true;
    }
    // An item taller than the list can need more than one scroll before the next one shows.
    if (shouldKeepSearch(navigationAction)
        && performScrollActionInternal(
            ScrollActionRecord.ACTION_AUTO_SCROLL,
            scrolledNode,
            refNode,
            TraversalStrategyUtils.convertSearchDirectionToScrollAction(
                navigationAction.searchDirection),
            navigationAction,
            ScrollTimeout.SCROLL_TIMEOUT_LONG,
            /* autoScrollSuccessChecker= */ null,
            eventId)) {
      return true;
    }
    // The end of the list: the list item found after it before scrolling, if any.
    if (navigationAction.fallbackTarget != null) {
      navigationAction.fallbackTarget.refresh();
      if (FILTER_SHOULD_FOCUS.accept(navigationAction.fallbackTarget)) {
        setAccessibilityFocusInternal(navigationAction.fallbackTarget, navigationAction, eventId);
        return true;
      }
    }
    announceNativeElement(logicalDirection, navigationAction.targetType, eventId);
    return true;
  }

  /** Called when we receive result event for auto-scroll action with screen search target. */
  private void handleViewAutoScrolledForDirectionalNavigationWithScreenSearchTarget(
      @NonNull AccessibilityNodeInfoCompat scrolledNode,
      NavigationAction sourceAction,
      EventId eventId) {
    AccessibilityNodeInfoCompat refNode =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    if (!AccessibilityNodeInfoUtils.isVisible(refNode)) {
      refNode = null;
    }
    if (refNode == null) {
      // Local TraversalStrategy generated in sub-tree of a refreshed scrolledNode.
      TraversalStrategy localTraversalStrategy =
          TraversalStrategyUtils.getTraversalStrategy(
              scrolledNode, focusFinder, sourceAction.searchDirection);
      // Start from the first focusable node of the Scrollable by the traversal strategy direction.
      refNode =
          TraversalStrategyUtils.findFirstFocusInNodeTree(
              localTraversalStrategy,
              scrolledNode,
              sourceAction.searchDirection,
              FILTER_SHOULD_FOCUS.and(
                  new Filter<AccessibilityNodeInfoCompat>() {
                    @Override
                    public boolean accept(AccessibilityNodeInfoCompat node) {
                      return !scrolledNode.equals(node);
                    }
                  }));
    }
    AccessibilityNodeInfoCompat nodeToFocus =
        findSearchTarget(refNode, sourceAction, searchState.getLastKeyword());
    if (nodeToFocus == null) {
      announceNoSearchResult(sourceAction.searchDirection, eventId);
    } else {
      setAccessibilityFocusInternal(nodeToFocus, sourceAction, eventId);
    }
  }

  /**
   * Find initial focus if target not found after auto-scroll performed.
   *
   * <p>Note: If no target is found and the scrolled screen doesn't have the accessibility focus(
   * {@code refNode} is not from {@link AccessibilityFocusMonitor#getAccessibilityFocus}), then
   * search for the new initial focus in the scrollable container.
   */
  private @Nullable AccessibilityNodeInfoCompat findInitialFocusAfterScrolled(
      AccessibilityNodeInfoCompat refNode,
      NavigationAction navigationAction,
      TraversalStrategy localTraversalStrategy) {
    if (refNode != null && !refNode.isAccessibilityFocused()) {
      // Focus on an edit field or slider gives it input focus, which turns browse mode off.
      Filter<AccessibilityNodeInfoCompat> filter =
          FILTER_SHOULD_FOCUS.and(
              Filter.node(
                  n ->
                      Role.getRole(n) != Role.ROLE_EDIT_TEXT
                          && !AccessibilityNodeInfoUtils.isInteractableWithArrowKeys(n)));
      return filter.accept(refNode)
          ? refNode
          : TraversalStrategyUtils.searchFocus(
              localTraversalStrategy, refNode, navigationAction.searchDirection, filter);
    }
    return null;
  }

  // TODO: Provides an overall experience of focusing on small nodes on both watch and
  //  phone devices.
  // It’s okay that the restrictions are not strict since a user can perform a gesture to stop
  // keeping search. We will add a test to cover this scenario.
  // TODO: Create a test to cover the case of stopping multiple auto scroll.
  public static final int MAX_MULTIPLE_AUTO_SCROLL_ATTEMPT = 100;
  private static final int MAX_ELEMENT_AUTO_SCROLL_ATTEMPT = 10;
  public static final int MAX_MULTIPLE_SCROLL_SCREEN_MULTIPLIER = 100;

  private boolean shouldKeepSearch(NavigationAction navigationAction) {
    // Off watches, a limit keeps a search in an endless feed short. Only element moves get here.
    int maxAttempts =
        TalkbackFeatureSupport.supportMultipleAutoScroll()
            ? MAX_MULTIPLE_AUTO_SCROLL_ATTEMPT
            : MAX_ELEMENT_AUTO_SCROLL_ATTEMPT;
    final Point screenPxSize = DisplayUtils.getScreenPixelSizeWithoutWindowDecor(service);
    return navigationAction.autoScrollAttempt <= maxAttempts
        && (BuildVersionUtils.isAtLeastP() // Only build version at least P supports scroll delta.
            && Math.abs(navigationAction.prevScrollDeltaSumX)
                < MAX_MULTIPLE_SCROLL_SCREEN_MULTIPLIER * screenPxSize.x
            && Math.abs(navigationAction.prevScrollDeltaSumY)
                < MAX_MULTIPLE_SCROLL_SCREEN_MULTIPLIER * screenPxSize.y);
  }

  /**
   * Called when we receive result event for auto-scroll action with default target.
   *
   * <p><b>Warning:</b> Do not rely too much logic on {@code focusBeforeScroll}. It is possible in
   * RecyclerView when {@code focusBeforeScroll} goes off screen, and after calling {@link
   * AccessibilityNodeInfoCompat#refresh()}, the node is reused and pointed to another emerging list
   * item. This is how RecyclerView "recycles" views and we cannot get rid of it. If using this
   * field, please refresh and validate the node to ensure it is identical with what it was before
   * scroll action.
   */
  private void handleViewAutoScrolledForDirectionalNavigationWithDefaultTarget(
      @NonNull AccessibilityNodeInfoCompat scrolledNode,
      @NonNull AccessibilityNodeInfoCompat focusBeforeScroll,
      NavigationAction sourceAction,
      EventId eventId) {
    handleViewAutoScrolledForDirectionalNavigationWithDefaultTarget(
        scrolledNode, focusBeforeScroll, sourceAction, eventId, SETTLE_CHECKS);
  }

  /**
   * Searches again {@code SETTLE_CHECK_MS} from now, after reading the list again, while it may
   * still be moving. Gives up if the user navigates or focus moves elsewhere in the meantime.
   */
  private void searchAgainWhenListMoves(
      @NonNull AccessibilityNodeInfoCompat scrolledNode,
      @NonNull AccessibilityNodeInfoCompat focusBeforeScroll,
      NavigationAction sourceAction,
      EventId eventId,
      int settleChecksLeft) {
    int serial = navigationSerial;
    pendingScrollSearchSerial = serial;
    earlyScrollCheckHandler.postDelayed(
        () -> {
          if (pendingScrollSearchSerial == serial) {
            pendingScrollSearchSerial = -1;
          }
          if (serial != navigationSerial) {
            return;
          }
          AccessibilityNodeInfoCompat focus =
              accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
          if (focus != null
              && !focus.equals(focusBeforeScroll)
              && !fellToContainer(focus, scrolledNode, focusBeforeScroll)) {
            return;
          }
          scrolledNode.refresh();
          readListAgain(scrolledNode);
          handleViewAutoScrolledForDirectionalNavigationWithDefaultTarget(
              scrolledNode, focusBeforeScroll, sourceAction, eventId, settleChecksLeft);
        },
        SETTLE_CHECK_MS);
  }

  private void handleViewAutoScrolledForDirectionalNavigationWithDefaultTarget(
      @NonNull AccessibilityNodeInfoCompat scrolledNode,
      @NonNull AccessibilityNodeInfoCompat focusBeforeScroll,
      NavigationAction sourceAction,
      EventId eventId,
      int settleChecksLeft) {
    // Following a node has shown that the list has slowed down.
    boolean settled = listSettled;
    // Prior to focus on position for scroll in grid.
    if (sourceAction.positionForScroll != null) {
      AccessibilityNodeInfoCompat grid =
          AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(
              scrolledNode, FILTER_SCROLLABLE_GRID);
      LogUtils.v(TAG, "Focus on position for scroll in grid " + sourceAction.positionForScroll);
      if (grid != null) {
        AccessibilityNodeInfoCompat nodeToFocus =
            TableNavigation.getCellFromTargetIndex(grid, sourceAction.positionForScroll);
        if (nodeToFocus != null) {
          setAccessibilityFocusInternal(nodeToFocus, sourceAction, eventId);
          return;
        }
        LogUtils.v(TAG, "Failed to focus on position for scroll");
      }
    }

    // Local TraversalStrategy generated in sub-tree of scrolledNode.
    TraversalStrategy localTraversalStrategy =
        TraversalStrategyUtils.getTraversalStrategy(
            scrolledNode, focusFinder, sourceAction.searchDirection);
    Filter<AccessibilityNodeInfoCompat> nodeFilter =
        NavigationTarget.createNodeFilter(
            sourceAction.targetType, localTraversalStrategy.getSpeakingNodesCache());
    Rect previousRect = new Rect();
    focusBeforeScroll.getBoundsInScreen(previousRect);
    boolean validAccessibilityFocus =
        focusBeforeScroll.refresh() && AccessibilityNodeInfoUtils.isVisible(focusBeforeScroll);
    // A missing pivot's edge can still leave the screen after a slowdown. Keep its existing
    // stability checks; a valid pivot can continue as soon as the list has slowed down.
    if (settled && validAccessibilityFocus) {
      settleChecksLeft = 0;
    }

    NavigationAction navigationAction =
        NavigationAction.Builder.copy(sourceAction)
            .setAutoScrollAttempt(sourceAction.autoScrollAttempt + 1)
            .build();

    // A focused item that fills the list looks still while the list moves. Searching while it
    // moves at speed finds nothing and reads the list each time, so wait until it slows down.
    if (validAccessibilityFocus && settleChecksLeft > 0 && sourceAction.shouldScroll) {
      Rect boundsNow = getBoundsAfterScroll(focusBeforeScroll);
      if (previousRect.equals(boundsNow)
          && fillsList(boundsNow, scrolledNode)
          && listStillMoving()) {
        LogUtils.d(TAG, "List still moving at speed; checking its scroll events again.");
        searchWhenListSlows(
            scrolledNode, focusBeforeScroll, sourceAction, eventId, settleChecksLeft - 1);
        return;
      }
    }

    AccessibilityNodeInfoCompat nodeToFocus;
    if (validAccessibilityFocus) {
      // A valid pivot can be missing from the list's order after the scroll, as in some
      // RecyclerViews, and a search from it finds nothing. Search from the node at its place
      // instead. The pivot keeps the focus, and the checks and retries below go on from it.
      AccessibilityNodeInfoCompat searchPivot = focusBeforeScroll;
      if (isMissingFromListOrder(localTraversalStrategy, scrolledNode, focusBeforeScroll)) {
        AccessibilityNodeInfoCompat nearest =
            findNodeNearPivotPlace(
                (OrderedTraversalStrategy) localTraversalStrategy,
                focusBeforeScroll,
                sourceAction.searchDirection,
                WindowUtils.isScreenLayoutRTL(service));
        if (nearest == null) {
          // Nothing in the list is on the near side of the pivot's place: focus the list's first
          // or last item, as for a pivot that left the list.
          LogUtils.d(TAG, "Pivot missing from the list's order at its edge; focusing the edge.");
          focusEdgeNodeWhenSettled(
              localTraversalStrategy,
              scrolledNode,
              focusBeforeScroll,
              sourceAction,
              navigationAction,
              nodeFilter,
              eventId,
              settleChecksLeft);
          return;
        }
        LogUtils.d(TAG, "Pivot missing from the list's order; searching from its place.");
        searchPivot = nearest;
      }
      // Try to find the next focusable node based on current focus.
      nodeToFocus =
          TraversalStrategyUtils.searchFocus(
              localTraversalStrategy, searchPivot, sourceAction.searchDirection, nodeFilter);
      // A RecyclerView can show another row in the pivot's view by now, so only a node holding the
      // pivot on screen, such as its own card, counts as a step back here.
      nodeToFocus =
          skipBackSteps(
              localTraversalStrategy,
              nodeToFocus,
              focusBeforeScroll,
              sourceAction.searchDirection,
              nodeFilter,
              /* holdingOnly= */ true);
      if (nodeToFocus == null) {
        Rect newRect = getBoundsAfterScroll(focusBeforeScroll);
        // The pivot moved since the scroll or the last look, so the list may still be moving and
        // its next item not on screen yet. Navigating again now would scroll a second page. A pivot
        // that fills the list, such as a post taller than the screen, is cut to the list's bounds
        // and doesn't seem to move, so the list's scroll events decide instead.
        if ((!previousRect.equals(newRect)
                || (fillsList(newRect, scrolledNode) && listStillMoving()))
            && settleChecksLeft > 0
            && sourceAction.shouldScroll) {
          LogUtils.d(TAG, "List still moving; searching again on the next frame.");
          searchAgainWhenListMoves(
              scrolledNode, focusBeforeScroll, sourceAction, eventId, settleChecksLeft - 1);
          return;
        }
        // The row after the swipe's starting row may be on screen though the pivot shows another
        // row now; scrolling again would pass it.
        AccessibilityNodeInfoCompat inNextRow =
            keepRowOrder(scrolledNode, null, sourceAction.searchDirection, "After the scroll");
        if (inNextRow != null) {
          setAccessibilityFocusInternal(inNextRow, navigationAction, eventId);
          return;
        }
        if (LogUtils.shouldLog(Log.INFO)) {
          LogUtils.i(
              TAG,
              "After the scroll: nothing after the pivot (%s); searching again, attempt %d.",
              rowDescription(focusBeforeScroll),
              navigationAction.autoScrollAttempt);
        }
        // A list that accepted the scroll without moving, such as one at its end, sends no deltas,
        // but scroll events from other views count as scrolls here. Its pivot, inside it and not
        // filling it, then hasn't moved: scrolling again would only repeat that.
        boolean listDidNotMove =
            !lastScrollWasJump
                && scrollDeltaSumY == scrollStartDeltaSumY
                && newRect.equals(pivotBoundsAtScrollStart)
                && previousRect.equals(newRect)
                && !fillsList(newRect, scrolledNode)
                && AccessibilityNodeInfoUtils.hasAncestor(focusBeforeScroll, scrolledNode);
        if (listDidNotMove && sourceAction.shouldScroll) {
          LogUtils.d(TAG, "After the scroll: the pivot inside the list didn't move; not scrolling it again.");
          navigationAction =
              NavigationAction.Builder.copy(navigationAction).setShouldScroll(false).build();
        } else if (settled
            && sourceAction.shouldScroll
            && sourceAction.isPrevScrolled
            && scrollAgainAtOnce(scrolledNode, focusBeforeScroll, navigationAction, eventId)) {
          return;
        }
        if (!listDidNotMove && previousRect.equals(newRect) && sourceAction.shouldScroll) {
          if (sourceAction.isPrevScrolled) {
            LogUtils.d(TAG, "Pivot didn't move but has scrolls. Repeat until to find the next.");
          } else {
            LogUtils.d(TAG, "Pivot didn't move and no scrolls, do not repeat.");
            navigationAction =
                NavigationAction.Builder.copy(sourceAction)
                    .setAutoScrollAttempt(sourceAction.autoScrollAttempt + 1)
                    .setShouldScroll(false)
                    .build();
          }
        }
        // Repeat navigation action in hope that eventually a new item will be exposed. From a
        // WebView row the page's own items stay out of it: a search into the page from the
        // WebView would focus its first item again.
        onDirectionalNavigationAction(
            /* pivot= */ focusBeforeScroll,
            /* ignoreDescendantsOfPivot= */ Role.getRole(focusBeforeScroll) == Role.ROLE_WEB_VIEW,
            navigationAction,
            eventId);
        return;
      }
    } else {
      // RecyclerView may recycle focusBeforeScroll after it leaves the viewport. Do not search
      // from that stale node: searching from it can skip the first newly visible item.
      focusEdgeNodeWhenSettled(
          localTraversalStrategy,
          scrolledNode,
          focusBeforeScroll,
          sourceAction,
          navigationAction,
          nodeFilter,
          eventId,
          settleChecksLeft);
      return;
    }

    finishAutoScrolledNavigation(
        focusBeforeScroll, nodeToFocus, scrolledNode, navigationAction, eventId);
  }

  /**
   * Focuses the first/last focusable node in {@code scrolledNode}'s subtree (per {@link
   * #findFocusAfterInvalidPivot}), for when there's no usable pivot to search onward from --
   * either because it's invalid/not visible, or because it's missing from the local tree and was
   * itself at the relevant edge of what that tree currently has. Waits for the list to settle
   * first via {@link #searchAgainWhenListMoves} when it might still be moving, since the edge
   * item can still scroll away and lose focus if focused too early. Does nothing if there's no
   * edge node to focus.
   */
  private void focusEdgeNodeWhenSettled(
      TraversalStrategy localTraversalStrategy,
      AccessibilityNodeInfoCompat scrolledNode,
      AccessibilityNodeInfoCompat focusBeforeScroll,
      NavigationAction sourceAction,
      NavigationAction navigationAction,
      Filter<AccessibilityNodeInfoCompat> nodeFilter,
      EventId eventId,
      int settleChecksLeft) {
    if (goesForward(sourceAction)) {
      Set<AccessibilityNodeInfoCompat> pinned = pinnedChildrenAfterScroll(scrolledNode);
      if (!pinned.isEmpty()) {
        nodeFilter = nodeFilter.and(Filter.node(node -> !isInChildOf(node, scrolledNode, pinned)));
      }
    }
    AccessibilityNodeInfoCompat edgeNode =
        findFocusAfterInvalidPivot(
            localTraversalStrategy,
            scrolledNode,
            sourceAction.searchDirection,
            nodeFilter,
            focusBeforeScroll);
    edgeNode =
        skipBackSteps(
            localTraversalStrategy,
            edgeNode,
            focusBeforeScroll,
            sourceAction.searchDirection,
            nodeFilter,
            /* holdingOnly= */ false);
    if (edgeNode == null) {
      // Only the old pivot's row (or its children) may be on screen while the list still moves.
      // Search again when it stops, rather than dropping the swipe.
      if (settleChecksLeft > 0) {
        searchAgainWhenListMoves(
            scrolledNode, focusBeforeScroll, sourceAction, eventId, settleChecksLeft - 1);
      }
      // Without a valid pivot node, don't repeat the navigation action.
      return;
    }

    // The pivot left the list while it still moves, so the first item may scroll away too and
    // focus would be lost. Wait until that item stops moving.
    Rect edgeBounds = new Rect();
    edgeNode.getBoundsInScreen(edgeBounds);
    if (settleChecksLeft > 0 && !edgeBounds.equals(firstItemBoundsAfterPivotLeft)) {
      firstItemBoundsAfterPivotLeft = edgeBounds;
      LogUtils.d(TAG, "Pivot left the list; searching again when the list stops.");
      searchAgainWhenListMoves(
          scrolledNode, focusBeforeScroll, sourceAction, eventId, settleChecksLeft - 1);
      return;
    }
    firstItemBoundsAfterPivotLeft = null;

    finishAutoScrolledNavigation(
        focusBeforeScroll, edgeNode, scrolledNode, navigationAction, eventId);
  }

  /**
   * Common tail of {@link #handleViewAutoScrolledForDirectionalNavigationWithDefaultTarget}: if
   * moving backward with default target lands on a WebView container node, descends into its
   * last element instead; otherwise ensures {@code nodeToFocus} is on screen and focuses it.
   */
  private void finishAutoScrolledNavigation(
      AccessibilityNodeInfoCompat focusBeforeScroll,
      AccessibilityNodeInfoCompat nodeToFocus,
      AccessibilityNodeInfoCompat scrolledNode,
      NavigationAction navigationAction,
      EventId eventId) {
    nodeToFocus =
        keepRowOrder(
            scrolledNode, nodeToFocus, navigationAction.searchDirection, "After the scroll");
    if (LogUtils.shouldLog(Log.INFO)) {
      LogUtils.i(
          TAG,
          "After the scroll: swipe from row %s, focusing %s.",
          swipePivotRow == null ? "none" : String.valueOf(swipePivotRow.row()),
          rowDescription(nodeToFocus));
    }

    // If we're moving backward with default target from native views to WebView container node,
    // automatically descend to the last element in the WebView.
    int logicalDirection =
        TraversalStrategyUtils.getLogicalDirection(
            navigationAction.searchDirection, WindowUtils.isScreenLayoutRTL(service));
    if (logicalDirection == TraversalStrategy.SEARCH_FOCUS_BACKWARD) {
      // We don't need to check role of the last focused node, because auto-scroll always
      // happens on native pivot.
      if (Role.getRole(nodeToFocus) == Role.ROLE_WEB_VIEW) {
        if (navigateToHtmlTarget(/* pivot= */ nodeToFocus, navigationAction, eventId)) {
          return;
        }
      }
    }

    if (shouldReEnsureSmallNodeOnScreen(focusBeforeScroll, nodeToFocus, navigationAction)) {
      ensureOnScreenInternal(scrolledNode, nodeToFocus, eventId);
    }

    setAccessibilityFocusInternal(nodeToFocus, navigationAction, eventId);
  }

  /**
   * Searches {@code strategy} for the node after/before {@code pivot}, recovering first if
   * {@code pivot} -- though still valid and visible -- isn't a key in that tree (e.g. a
   * RecyclerView recycled its view during the auto-scroll this caller is tracking), the same way
   * {@link #handleViewAutoScrolledForDirectionalNavigationWithDefaultTarget} does. Falls back to
   * {@link #findFocusAfterInvalidPivot} -- the first/last focusable in {@code scrolledNode}'s
   * subtree -- when {@code pivot} is outright invalid, or was itself at the relevant edge of
   * what the tree currently has. Used while a scroll is moving: returns null for a known
   * incomplete order or an empty indexed item, so the existing watcher can try again. {@code
   * items} are the scrolled container's children already read by that watcher.
   */
  @VisibleForTesting
  static @Nullable AccessibilityNodeInfoCompat searchFocusRecoveringMissingPivot(
      TraversalStrategy strategy,
      AccessibilityNodeInfoCompat scrolledNode,
      AccessibilityNodeInfoCompat pivot,
      @SearchDirection int direction,
      Filter<AccessibilityNodeInfoCompat> nodeFilter,
      boolean rtl,
      List<AccessibilityNodeInfoCompat> items) {
    // An unread child can be the adjacent item. The existing scroll watcher will try again.
    if (strategy instanceof OrderedTraversalStrategy ordered && ordered.missesChildren()) {
      LogUtils.d(TAG, "Early scroll order missed children; waiting for a complete order.");
      return null;
    }
    Set<AccessibilityNodeInfoCompat> emptyItems = new HashSet<>();
    if (scrolledNode.getCollectionInfo() == null
        && AccessibilityNodeInfoUtils.supportsAction(
            scrolledNode, AccessibilityActionCompat.ACTION_SCROLL_TO_POSITION.getId())) {
      Filter<AccessibilityNodeInfoCompat> focusable = nodeFilter;
      nodeFilter =
          Filter.node(
              node -> {
                if (focusable.accept(node)) {
                  return true;
                }
                // An indexed item may appear before the icon inside it is attached.
                if (node.getChildCount() == 0 && items.contains(node)) {
                  emptyItems.add(node);
                  return true;
                }
                return false;
              });
    }
    AccessibilityNodeInfoCompat next;
    if (!pivot.refresh() || !AccessibilityNodeInfoUtils.isVisible(pivot)) {
      LogUtils.d(TAG, "While scrolling: the pivot is gone; searching from the list's edge.");
      next = findFocusAfterInvalidPivot(strategy, scrolledNode, direction, nodeFilter, pivot);
    } else if (isMissingFromListOrder(strategy, scrolledNode, pivot)) {
      LogUtils.d(TAG, "While scrolling: the pivot is missing from the order; searching near it.");
      AccessibilityNodeInfoCompat nearest =
          findNodeNearPivotPlace((OrderedTraversalStrategy) strategy, pivot, direction, rtl);
      next =
          (nearest != null)
              ? TraversalStrategyUtils.searchFocus(strategy, nearest, direction, nodeFilter)
              : findFocusAfterInvalidPivot(strategy, scrolledNode, direction, nodeFilter, pivot);
    } else {
      next = TraversalStrategyUtils.searchFocus(strategy, pivot, direction, nodeFilter);
    }
    if (emptyItems.contains(next)) {
      LogUtils.d(
          TAG, "Adjacent indexed item %d is empty; waiting for its content.", next.hashCode());
      return null;
    }
    return next;
  }

  /**
   * Returns whether {@code pivot}, a node inside {@code scrolledNode}, is missing from {@code
   * strategy}, the order of {@code scrolledNode}. Web content is left out of that order on
   * purpose, so a pivot in it doesn't count.
   */
  private static boolean isMissingFromListOrder(
      TraversalStrategy strategy,
      AccessibilityNodeInfoCompat scrolledNode,
      AccessibilityNodeInfoCompat pivot) {
    return strategy instanceof OrderedTraversalStrategy orderedStrategy
        && !orderedStrategy.containsNode(pivot)
        && !WebInterfaceUtils.supportsWebActions(pivot)
        && AccessibilityNodeInfoUtils.hasAncestor(pivot, scrolledNode);
  }

  /**
   * Finds the node in {@code strategy}'s own tree nearest {@code pivot}'s current on-screen
   * position, for when {@code pivot} -- though still valid and visible -- isn't a key in that
   * tree (e.g. a RecyclerView recycled its view during a multi-step auto-scroll, so the
   * freshly-built local tree never included it). The caller always does a further search from
   * the direction edge of whatever is returned, so this returns the node whose position is
   * nearest {@code pivot}'s on the near side: for a forward search, the last node not yet past
   * {@code pivot}'s own bottom edge; for a backward search, the first node at or past {@code
   * pivot}'s own top edge. Returns {@code null} when {@code pivot} was itself at the relevant
   * edge of what this tree currently has (e.g. the auto-scroll revealed exactly the top/bottom
   * of the content) -- there is nothing on the near side to anchor a further search from, so the
   * caller resolves that case directly instead, the same way an already-invalid pivot is.
   */
  @VisibleForTesting
  static @Nullable AccessibilityNodeInfoCompat findNodeNearPivotPlace(
      OrderedTraversalStrategy strategy,
      AccessibilityNodeInfoCompat pivot,
      @SearchDirection int direction,
      boolean rtl) {
    Rect pivotBounds = new Rect();
    pivot.getBoundsInScreen(pivotBounds);
    // dumpTree() is in traversal order, so an ancestor of where the pivot sits -- starting with
    // dumpTree()'s first entry, the strategy's own root (here, scrolledNode) -- always comes
    // before it, and its bounds span its entire subtree, so they always contain the pivot's.
    // Such a node is never a real anchor: in the backward direction especially, it would be the
    // very first node whose bounds reach the pivot's place, even though its own children above
    // that place -- real candidates -- haven't been considered yet. Skip any node whose bounds
    // contain the pivot's, not just the root.
    List<AccessibilityNodeInfoCompat> nodes = strategy.dumpTree();
    Rect bounds = new Rect();
    if (direction == TraversalStrategy.SEARCH_FOCUS_FORWARD) {
      AccessibilityNodeInfoCompat above = null;
      for (AccessibilityNodeInfoCompat node : nodes) {
        node.getBoundsInScreen(bounds);
        // A node at the pivot's very place is the pivot drawn again as a new node: search on
        // from it, not from the node before it, which would find it and focus the same item.
        if (bounds.equals(pivotBounds) && !AccessibilityNodeInfoUtils.hasAncestor(pivot, node)) {
          return node;
        }
        if (bounds.contains(pivotBounds)) {
          continue;
        }
        // A node sharing the pivot's row/band (e.g. a grid cell or a carousel item) can match
        // on top/bottom alone regardless of which side of the pivot it's actually on. Tell them
        // apart by reading-order position within the row instead -- but only for an actual
        // row-mate beside the pivot; a node in the pivot's own column just happens to overlap it
        // vertically (e.g. mid-scroll), and falls through to the ordinary top/bottom check below.
        if (isRowMateOfPivot(bounds, pivotBounds)) {
          if (isBeforePivotInRow(bounds, pivotBounds, rtl)) {
            above = node;
            continue;
          }
          return above;
        }
        if (bounds.top >= pivotBounds.bottom) {
          return above;
        }
        above = node;
      }
      return above;
    } else {
      for (AccessibilityNodeInfoCompat node : nodes) {
        node.getBoundsInScreen(bounds);
        if (bounds.equals(pivotBounds) && !AccessibilityNodeInfoUtils.hasAncestor(pivot, node)) {
          return node;
        }
        if (bounds.contains(pivotBounds)) {
          continue;
        }
        if (isRowMateOfPivot(bounds, pivotBounds)) {
          if (isBeforePivotInRow(bounds, pivotBounds, rtl)) {
            continue;
          }
          return node;
        }
        if (bounds.bottom > pivotBounds.top) {
          return node;
        }
      }
      return null;
    }
  }

  /**
   * Returns whether {@code bounds} shares {@code pivotBounds}'s row/band (vertical overlap)
   * while sitting beside it rather than above/below it (no horizontal overlap) -- the shape of a
   * grid cell or carousel item next to the pivot, as opposed to an ordinary single-column sibling
   * that happens to vertically overlap the pivot's place (e.g. mid-scroll).
   */
  private static boolean isRowMateOfPivot(Rect bounds, Rect pivotBounds) {
    boolean verticalOverlap = bounds.top < pivotBounds.bottom && bounds.bottom > pivotBounds.top;
    boolean horizontalOverlap = bounds.left < pivotBounds.right && bounds.right > pivotBounds.left;
    return verticalOverlap && !horizontalOverlap;
  }

  /**
   * Returns whether {@code bounds} sits before {@code pivotBounds} in reading order within a
   * shared row, going right-to-left when {@code rtl}.
   */
  private static boolean isBeforePivotInRow(Rect bounds, Rect pivotBounds, boolean rtl) {
    return rtl ? bounds.left >= pivotBounds.right : bounds.right <= pivotBounds.left;
  }

  /**
   * Scrolls {@code list} again from {@code pivot} at once, after a scroll that has visibly moved it
   * showed no next item, without building the window's order. Returns false if the list can't
   * scroll that way any more, so that the search goes on as usual.
   */
  private boolean scrollAgainAtOnce(
      AccessibilityNodeInfoCompat list,
      AccessibilityNodeInfoCompat pivot,
      NavigationAction navigationAction,
      EventId eventId) {
    list.refresh();
    boolean rtl = WindowUtils.isScreenLayoutRTL(service);
    ScrollableNodeInfo scrollableNodeInfo =
        ScrollableNodeInfo.findScrollableNodeForDirection(
            navigationAction.searchDirection, pivot, /* includeSelf= */ false, rtl);
    if (scrollableNodeInfo == null) {
      scrollableNodeInfo =
          findAnyRoleScrollable(pivot, /* ignoreDescendantsOfPivot= */ false, navigationAction, rtl);
    }
    if (scrollableNodeInfo == null || !scrollableNodeInfo.getNode().equals(list)) {
      return false;
    }
    if (!autoScroll(scrollableNodeInfo, pivot, navigationAction, eventId)) {
      return false;
    }
    LogUtils.i(TAG, "No next item yet; scrolled again at once.");
    return true;
  }

  /** Finds the first valid target after the previous focus node was recycled during scrolling. */
  @VisibleForTesting
  static @Nullable AccessibilityNodeInfoCompat findFocusAfterInvalidPivot(
      TraversalStrategy traversalStrategy,
      AccessibilityNodeInfoCompat scrolledNode,
      @SearchDirection int searchDirection,
      Filter<AccessibilityNodeInfoCompat> nodeFilter,
      @Nullable AccessibilityNodeInfoCompat previousPivot) {
    // A removed child can still claim to be visible in the cached order. Do not focus it again.
    // Going forward must also not return to its parent, which precedes it in reading order.
    if (previousPivot != null
        && !previousPivot.equals(scrolledNode)
        && !WebInterfaceUtils.supportsWebActions(scrolledNode)) {
      Set<AccessibilityNodeInfoCompat> excluded = new HashSet<>();
      excluded.add(previousPivot);
      if (searchDirection == SEARCH_FOCUS_FORWARD) {
        AccessibilityNodeInfoCompat parent = previousPivot.getParent();
        for (int depth = 0;
            parent != null && !parent.equals(scrolledNode) && depth < MAX_ITEM_DEPTH;
            depth++) {
          excluded.add(parent);
          parent = parent.getParent();
        }
      }
      nodeFilter = nodeFilter.and(Filter.node(node -> !excluded.contains(node)));
      if (searchDirection == SEARCH_FOCUS_BACKWARD) {
        // A row precedes its children in reading order. Moving backward from a removed row
        // must not choose one of its cached children, which would move forward instead.
        nodeFilter =
            nodeFilter.and(
                Filter.node(
                    node -> {
                      AccessibilityNodeInfoCompat parent = node.getParent();
                      for (int depth = 0; parent != null && depth < MAX_ITEM_DEPTH; depth++) {
                        if (parent.equals(previousPivot)) {
                          return false;
                        }
                        if (parent.equals(scrolledNode)) {
                          break;
                        }
                        parent = parent.getParent();
                      }
                      return true;
                    }));
      }
    }
    return TraversalStrategyUtils.findFirstFocusInNodeTree(
        traversalStrategy, scrolledNode, searchDirection, nodeFilter);
  }

  /** Returns whether the list of the last auto scroll still moves at speed. */
  private boolean listStillMoving() {
    return !listSettled
        && listStillMoving(
            SystemClock.uptimeMillis(),
            lastAutoScrollUptime,
            scrollEventCount - scrollStartEventCount,
            lastScrollEventUptime,
            lastScrollStep,
            previousScrollStep,
            scrollOfOneEventList);
  }

  /**
   * Returns whether a list scrolled at {@code scrollStart} counts as still moving at speed at
   * {@code now}, after {@code events} scroll events since the scroll, the last at {@code
   * lastEvent}, which moved {@code lastStep} pixels after a step of {@code previousStep}.
   */
  @VisibleForTesting
  static boolean listStillMoving(
      long now, long scrollStart, int events, long lastEvent, int lastStep, int previousStep) {
    return listStillMoving(
        now, scrollStart, events, lastEvent, lastStep, previousStep, /* oneEventList= */ false);
  }

  /**
   * As {@link #listStillMoving(long, long, int, long, int, int)}, for a list that scrolls a page
   * in one scroll event when {@code oneEventList}: such a list has stopped a frame after its first
   * event.
   */
  @VisibleForTesting
  static boolean listStillMoving(
      long now,
      long scrollStart,
      int events,
      long lastEvent,
      int lastStep,
      int previousStep,
      boolean oneEventList) {
    long sinceStart = now - scrollStart;
    if (oneEventList && events > 0 && now - lastEvent >= FRAME_MS) {
      return false;
    }
    if (sinceStart >= MAX_MOVING_WAIT_MS) {
      return false;
    }
    if (events <= 0) {
      return sinceStart < NO_MOVE_WAIT_MS;
    }
    if (now - lastEvent >= STOPPED_MS) {
      return false;
    }
    // Slowing down: the list is nearly where it stops.
    return events < 2 || 2L * lastStep >= previousStep;
  }

  /**
   * Searches again once the list, whose focused item fills it, has slowed down, checking its scroll
   * events every {@link #MOVING_CHECK_MS} meanwhile. Gives up if the user navigates or focus moves
   * elsewhere.
   */
  private void searchWhenListSlows(
      @NonNull AccessibilityNodeInfoCompat scrolledNode,
      @NonNull AccessibilityNodeInfoCompat focusBeforeScroll,
      NavigationAction sourceAction,
      EventId eventId,
      int settleChecksLeft) {
    int serial = navigationSerial;
    pendingScrollSearchSerial = serial;
    earlyScrollCheckHandler.postDelayed(
        () -> {
          if (pendingScrollSearchSerial == serial) {
            pendingScrollSearchSerial = -1;
          }
          if (serial != navigationSerial) {
            return;
          }
          AccessibilityNodeInfoCompat focus =
              accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
          if (focus != null
              && !focus.equals(focusBeforeScroll)
              && !fellToContainer(focus, scrolledNode, focusBeforeScroll)) {
            return;
          }
          if (listStillMoving()) {
            searchWhenListSlows(
                scrolledNode, focusBeforeScroll, sourceAction, eventId, settleChecksLeft);
            return;
          }
          logListSlowed();
          scrolledNode.refresh();
          readListAgain(scrolledNode);
          handleViewAutoScrolledForDirectionalNavigationWithDefaultTarget(
              scrolledNode, focusBeforeScroll, sourceAction, eventId, settleChecksLeft);
        },
        MOVING_CHECK_MS);
  }

  private void logListSlowed() {
    LogUtils.d(
        TAG,
        "List slowed down %d ms after the scroll: %d scroll events, last steps %d and %d.",
        SystemClock.uptimeMillis() - lastAutoScrollUptime,
        scrollEventCount - scrollStartEventCount,
        previousScrollStep,
        lastScrollStep);
  }

  /** Returns whether {@code pivotBounds} cover all of {@code list} that is on screen. */
  private static boolean fillsList(Rect pivotBounds, AccessibilityNodeInfoCompat list) {
    Rect listBounds = new Rect();
    list.getBoundsInScreen(listBounds);
    return !listBounds.isEmpty()
        && pivotBounds.top <= listBounds.top
        && pivotBounds.bottom >= listBounds.bottom;
  }

  @VisibleForTesting
  @NonNull Rect getBoundsAfterScroll(@NonNull AccessibilityNodeInfoCompat node) {
    Rect newBounds = new Rect();
    node.getBoundsInScreen(newBounds);
    return newBounds;
  }

  private boolean shouldReEnsureSmallNodeOnScreen(
      AccessibilityNodeInfoCompat beforeNode,
      AccessibilityNodeInfoCompat nodeToFocus,
      NavigationAction action) {
    final Point screenPxSize = DisplayUtils.getScreenPixelSizeWithoutWindowDecor(service);
    return nodeToFocus != null
        && !beforeNode.equals(nodeToFocus)
        && action != null
        && action.autoScrollAttempt > 0
        && AccessibilityNodeInfoUtils.isSmallNodeInHeight(service, nodeToFocus)
        && AccessibilityNodeInfoUtils.isTopOrBottomBorderNode(screenPxSize, nodeToFocus);
  }

  private void setLastScrolledNodeForNativeMacroGranularity(
      AccessibilityNodeInfoCompat scrolledNode) {
    lastScrolledNodeForNativeMacroGranularity = scrolledNode;
  }

  public void resetLastScrolledNodeForNativeMacroGranularity() {
    lastScrolledNodeForNativeMacroGranularity = null;
  }

  private void resetScrollRecord(EventId eventId) {
    pipeline.returnFeedback(eventId, Feedback.scrollResetScrollActionRecords());
  }

  /**
   * Handles auto-scroll callback when the scroll action is successfully performed but no result
   * {@link android.view.accessibility.AccessibilityEvent#TYPE_VIEW_SCROLLED} event is received.
   *
   * <p>This is a very corner case when "scroll" related metric is not correctly configured for some
   * customized containers. In this case, we should jump out of the container and continue searching
   * for the next target.
   */
  private void handleViewAutoScrollFailedForDirectionalNavigationAction(
      @NonNull AccessibilityNodeInfoCompat nodeToScroll, NavigationAction sourceAction) {
    // When auto-scroll fails, we don't search down the scrolled container, instead, we jump out of
    // it searching for the next target. Thus we use 'nodeToScroll' as the pivot and
    // 'ignoreDescendantsOfPivot' is set to TRUE.
    onDirectionalNavigationAction(
        /* pivot= */ nodeToScroll,
        /* ignoreDescendantsOfPivot= */ true,
        sourceAction,
        /* eventId= */ null);
  }

  private TraversalStrategy createTraversal(
      @NonNull AccessibilityNodeInfoCompat node, @TraversalStrategy.SearchDirection int direction) {
    return TraversalStrategyUtils.getTraversalStrategy(node, focusFinder, direction);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Methods to make announcement.
  // TODO: Think about moving this into Compositor.

  /** Announces if there are no more elements while using native granularity. */
  private void announceNativeElement(int direction, @TargetType int targetType, EventId eventId) {
    boolean forward = (direction == TraversalStrategy.SEARCH_FOCUS_FORWARD);
    int resId = forward ? R.string.end_of_page : R.string.start_of_page;

    String target = NavigationTarget.nativeTargetToDisplayName(/* context= */ service, targetType);
    if (TextUtils.isEmpty(target)) {
      return;
    }

    String text = service.getString(resId, target);
    announce(text, eventId);
  }

  private void announceNoSearchResult(int direction, EventId eventId) {
    boolean forward = (direction == TraversalStrategy.SEARCH_FOCUS_FORWARD);
    int resId = forward ? R.string.search_end_of_page : R.string.search_start_of_page;
    announce(service.getString(resId), eventId);
  }

  private void announce(CharSequence text, EventId eventId) {
    SpeechController.SpeakOptions speakOptions =
        SpeechController.SpeakOptions.create()
            .setQueueMode(SpeechController.QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH)
            .setFlags(FeedbackItem.FLAG_FORCE_FEEDBACK);
    pipeline.returnFeedback(eventId, Feedback.speech(text, speakOptions));
  }

  /**
   * Returns the biggest scroll view or pager view on the screen, it will traverse the node tree by
   * BFS. If the view hierarchy is complicated, for example, a pager view contains a long scroll
   * view, even though the pager is bigger than the scroll view, we may prefer to select the scroll
   * view since the content user interested should be inside it. So this method also supports to
   * give the pager view the lower priority. In that case the pager will be selected only if there
   * is no other scrollable view on the screen.
   *
   * @param node The root node to traverse from.
   * @param navigationAction The direction of the scrolling action.
   * @param pagerWithLowPriority Give the pager view a lower priority or not.
   * @return The scrollable node reached via BFS traversal.
   */
  public @Nullable AccessibilityNodeInfoCompat searchScrollableNodeFromBfs(
      @Nullable AccessibilityNodeInfoCompat node,
      NavigationAction navigationAction,
      boolean pagerWithLowPriority) {
    if (node == null) {
      return null;
    }

    final Filter<AccessibilityNodeInfoCompat> scrollableFilter =
        getScrollOrPageActionFilter(navigationAction);

    // Do the first BFS search without considering Pager role, no matter if we want to give pager a
    // lower priority or not. For the case pagerWithLowPriority==true, it only allows to get a pager
    // by the first search. The result will be used if we cannot find other scrollable nodes by
    // following steps, that also means the result we find here is the only scrollable node on the
    // screen.
    AccessibilityNodeInfoCompat result =
        AccessibilityNodeInfoUtils.searchFromBfs(node, scrollableFilter);
    if (result == null) {
      // No scrollable view on the screen.
      return null;
    }

    MaxSizeNodeAccumulator maxSizeNodeAccumulator;
    if (pagerWithLowPriority) {
      if (Role.getRole(result) == Role.ROLE_PAGER) {
        // Since we prefer to not select a pager, give null for the node area filter as a initial
        // value.
        maxSizeNodeAccumulator =
            new MaxSizeNodeAccumulator(
                null,
                scrollableFilter.and(Filter.node((item) -> Role.getRole(item) != Role.ROLE_PAGER)));
      } else {
        maxSizeNodeAccumulator =
            new MaxSizeNodeAccumulator(
                result,
                scrollableFilter.and(Filter.node((item) -> Role.getRole(item) != Role.ROLE_PAGER)));
      }
    } else {
      maxSizeNodeAccumulator = new MaxSizeNodeAccumulator(result, scrollableFilter);
    }

    AccessibilityNodeInfoUtils.searchFromBfs(
        node, Filter.node((item) -> false), maxSizeNodeAccumulator);
    if (maxSizeNodeAccumulator.maximumScrollableNode == null) {
      return result;
    }

    return maxSizeNodeAccumulator.maximumScrollableNode;
  }

  /**
   * Callback to be invoked after scroll action is performed in {@link
   * FocusProcessorForLogicalNavigation}. It caches some information to be used when handling the
   * result scroll event.
   */
  private static final class AutoScrollCallback {

    private final FocusProcessorForLogicalNavigation parent;
    private final NavigationAction sourceAction;
    private final AccessibilityNodeInfoCompat pivot;

    private boolean assumeScrollSuccess;

    /** Whether the scroll was a jump to a row, which keeps searching with a page scroll. */
    private boolean isJump;

    AutoScrollCallback(
        FocusProcessorForLogicalNavigation parent,
        NavigationAction sourceAction,
        @NonNull AccessibilityNodeInfoCompat pivot) {
      this(parent, sourceAction, pivot, false);
    }

    AutoScrollCallback(
        FocusProcessorForLogicalNavigation parent,
        NavigationAction sourceAction,
        @NonNull AccessibilityNodeInfoCompat pivot,
        boolean assumeScrollSuccess) {
      this.parent = parent;
      this.sourceAction = sourceAction;
      this.pivot = pivot;
      this.assumeScrollSuccess = assumeScrollSuccess;
      // The scroll before this one has sent all the events it will send before this one's.
      parent.checkOneEventScroll();
      // Each callback is created as its scroll starts. See listStillMoving().
      parent.lastAutoScrollUptime = SystemClock.uptimeMillis();
      parent.scrollStartEventCount = scrollEventCount;
      parent.scrollStartDeltaSumY = scrollDeltaSumY;
      parent.listSettled = false;
      parent.lastScrollWasJump = false;
      parent.scrollActionReturnedUptime = Long.MAX_VALUE;
      noteScrollWindow(pivot.getWindowId());
      pivot.getBoundsInScreen(parent.pivotBoundsAtScrollStart);
      parent.firstItemBoundsAfterPivotLeft = null;
      parent.scrollOfOneEventList = false;
      parent.oneEventScrollList = null;
      parent.scrollStartedQuiet =
          SystemClock.uptimeMillis() - lastScrollEventUptime >= LIST_STOPPED_MS;
      // The swipe's first scroll reads the starting row; later scrolls of the swipe keep it, as
      // their pivot may since show another row.
      if (sourceAction.autoScrollAttempt == 0) {
        parent.swipePivotRow =
            (sourceAction.actionType == NavigationAction.DIRECTIONAL_NAVIGATION
                    && sourceAction.targetType == NavigationTarget.TARGET_DEFAULT)
                ? rowOf(pivot)
                : null;
      }
      countScrollAction();
    }

    public void onAutoScrolled(
        @NonNull AccessibilityNodeInfoCompat scrolledNode,
        EventId eventId,
        int scrollDeltaX,
        int scrollDeltaY) {

      final NavigationAction navigationAction =
          provideNavigationActionByScrollDelta(scrollDeltaX, scrollDeltaY);

      LogUtils.d(
          TAG,
          "AutoScrollCallback onAutoScrolled, eventId="
              + eventId
              + ",navigationAction="
              + navigationAction);

      switch (sourceAction.actionType) {
        case NavigationAction.DIRECTIONAL_NAVIGATION -> {
          if (sourceAction.targetType == NavigationTarget.TARGET_DEFAULT) {
            parent.handleViewAutoScrolledForDirectionalNavigationWithDefaultTarget(
                scrolledNode, pivot, navigationAction, eventId);
          } else if (NavigationTarget.isMacroGranularity(sourceAction.targetType)) {
            parent.handleViewAutoScrolledForDirectionalNavigationWithMacroGranularityTarget(
                scrolledNode, navigationAction, eventId);
          } else if (sourceAction.targetType == TARGET_SEARCH) {
            parent.handleViewAutoScrolledForDirectionalNavigationWithScreenSearchTarget(
                scrolledNode, navigationAction, eventId);
          } else if (TableNavigation.isNativeTableGranularity(sourceAction.targetType)) {
            TableNavigation tableNavigation =
                new TableNavigation(navigationAction, scrolledNode, parent.focusFinder, eventId);
            parent.handleViewScrolledForTableNavigationAction(tableNavigation, pivot);
          }
        }
        case NavigationAction.SCROLL_FORWARD, NavigationAction.SCROLL_BACKWARD ->
            parent.handleViewScrolledForScrollNavigationAction(
                scrolledNode, navigationAction, eventId);
        default -> {}
      }
      clear();
    }

    private NavigationAction provideNavigationActionByScrollDelta(
        int scrollDeltaX, int scrollDeltaY) {
      final NavigationAction.Builder builder = NavigationAction.Builder.copy(sourceAction);
      builder.setIsPrevScrolled(false);
      if (scrollDeltaX != DELTA_UNDEFINED && scrollDeltaX != 0) {
        builder.setPrevScrollDeltaSumX(sourceAction.prevScrollDeltaSumX + scrollDeltaX);
        builder.setIsPrevScrolled(true);
      }
      if (scrollDeltaY != DELTA_UNDEFINED && scrollDeltaY != 0) {
        builder.setPrevScrollDeltaSumY(sourceAction.prevScrollDeltaSumY + scrollDeltaY);
        builder.setIsPrevScrolled(true);
      }
      if (isJump) {
        // A jump that left the pivot in place reports no delta. It must not end the scrolling.
        builder.setIsPrevScrolled(true);
      }
      return builder.build();
    }

    public void onAutoScrollFailed(@NonNull AccessibilityNodeInfoCompat nodeToScroll) {
      LogUtils.d(
          TAG,
          "AutoScrollCallback onAutoScrollFailed, assumeScrollSuccess="
              + assumeScrollSuccess
              + ",actionType="
              + NavigationAction.actionTypeToString(sourceAction.actionType));

      if (assumeScrollSuccess) {
        onAutoScrolled(
            nodeToScroll, EVENT_ID_UNTRACKED, /* scrollDeltaX= */ 0, /* scrollDeltaY*/ 0);
        return;
      }
      switch (sourceAction.actionType) {
        case NavigationAction.DIRECTIONAL_NAVIGATION -> {
          if (TableNavigation.isNativeTableGranularity(sourceAction.targetType)) {
            TableNavigation tableNavigation =
                new TableNavigation(
                    sourceAction, nodeToScroll, parent.focusFinder, EVENT_ID_UNTRACKED);
            parent.handleViewAutoScrollFailedForTableNavigationAction(tableNavigation);
          } else {
            parent.handleViewAutoScrollFailedForDirectionalNavigationAction(
                nodeToScroll, sourceAction);
          }
        }
        default -> {}
      }
      clear();
    }

    public boolean assumeScrollSuccess() {
      return assumeScrollSuccess;
    }

    /** Clears assumeScrollSuccess */
    private void clear() {
      assumeScrollSuccess = false;
    }
  }

  /** A data class to represent the navigation result of logical navigation. */
  private static class NavigationResult {
    /** The type of the navigation result. */
    public enum Type {
      EMPTY, // No element found.
      NATIVE_ELEMENT, // A native element found, calls #getNode to return the native element node.
      WEB_ELEMENT, // A web element found, it should skip the navigation and return success.
      EXISTING_FOCUS, // An existing focus found, it should skip the navigation and return success.
      REACH_EDGE, // An edge found, it should skip the navigation and return failure.
      EXCEPTION, // General exception cases, it should skip the navigation and return failure.
    }

    private Type type;
    private AccessibilityNodeInfoCompat node;
    private int linkIndex;

    private NavigationResult(Type type, AccessibilityNodeInfoCompat node) {
      this.type = type;
      this.node = node;
      this.linkIndex = -1;
    }

    /** Convenient method to create an instance for the certain type without the node. */
    public static NavigationResult create(Type type) {
      if (type == Type.NATIVE_ELEMENT) {
        throw new IllegalArgumentException("create native type without node");
      }
      return new NavigationResult(type, null);
    }

    /**
     * Convenient method to create an instance for the native element, return empty if null node.
     */
    public static NavigationResult create(AccessibilityNodeInfoCompat node) {
      return new NavigationResult((node == null ? Type.EMPTY : Type.NATIVE_ELEMENT), node);
    }

    /** Gets the native element node. */
    public @Nullable AccessibilityNodeInfoCompat getNode() {
      if (shouldSkipNavigation()) {
        throw new IllegalStateException("getNode for skipped type: " + type);
      }
      return node;
    }

    public boolean isEmpty() {
      return Type.EMPTY.equals(type);
    }

    public boolean isWebElement() {
      return Type.WEB_ELEMENT.equals(type);
    }

    public boolean isFocusAvailable() {
      return Type.NATIVE_ELEMENT.equals(type) || isWebElement() || Type.EXISTING_FOCUS.equals(type);
    }

    /** Returns true if it should skip the navigation immediately. */
    public boolean shouldSkipNavigation() {
      return Type.EXISTING_FOCUS.equals(type)
          || isWebElement()
          || Type.REACH_EDGE.equals(type)
          || Type.EXCEPTION.equals(type);
    }

    public void setLinkIndex(int linkIndex) {
      this.linkIndex = linkIndex;
    }

    public int getLinkIndex() {
      return linkIndex;
    }
  }

  /**
   * Filters nodes which are smaller than the temporary scrollable node. The accumulator will update
   * the temporary scrollable node once it finds a bigger scrollable node. Finally it can get the
   * node with maximum area in the node tree.
   */
  private static class MaxSizeNodeAccumulator extends Filter<AccessibilityNodeInfoCompat> {
    final Filter<AccessibilityNodeInfoCompat> scrollableFilter;
    AccessibilityNodeInfoCompat maximumScrollableNode;
    int maximumSize;

    /**
     * @param node Initial node of the max size check.
     */
    MaxSizeNodeAccumulator(
        @Nullable AccessibilityNodeInfoCompat node,
        Filter<AccessibilityNodeInfoCompat> scrollableFilter) {
      this.scrollableFilter = scrollableFilter;
      if (node == null) {
        maximumSize = 0;
      } else {
        maximumScrollableNode = node;
        Rect nodeBounds = new Rect();
        maximumScrollableNode.getBoundsInScreen(nodeBounds);
        maximumSize = nodeBounds.width() * nodeBounds.height();
      }
    }

    @Override
    public boolean accept(AccessibilityNodeInfoCompat node) {
      if (node == null) {
        return true;
      }

      Rect nodeBounds = new Rect();
      node.getBoundsInScreen(nodeBounds);
      int nodeSize = nodeBounds.width() * nodeBounds.height();
      if (nodeSize <= maximumSize) {
        return true;
      } else {
        // Update maximum scrollable node if the node is scrollable.
        if (scrollableFilter.accept(node)) {
          maximumScrollableNode = node;
          maximumSize = nodeSize;
        }
      }

      return false;
    }
  }

  /** Filters target window when performing directional navigation across windows. */
  private static class DirectionalNavigationWindowFilter extends Filter<AccessibilityWindowInfo> {
    final Context context;
    final UniversalSearchActor.State searchState;
    final Set<Integer> pictureInPictureOverlayIds;
    final boolean includeKeyboard;

    DirectionalNavigationWindowFilter(
        Context context,
        UniversalSearchActor.State searchState,
        Set<Integer> pictureInPictureOverlayIds,
        boolean includeKeyboard) {
      this.context = context;
      this.searchState = searchState;
      this.pictureInPictureOverlayIds = pictureInPictureOverlayIds;
      this.includeKeyboard = includeKeyboard;
    }

    @Override
    public boolean accept(AccessibilityWindowInfo window) {
      if (window == null || pictureInPictureOverlayIds.contains(window.getId())) {
        return false;
      }
      int type = AccessibilityWindowInfoUtils.getType(window);
      if (searchState.isUiVisible()) {
        return (isSearchOverlay(context, window)
            || (type == AccessibilityWindowInfo.TYPE_SYSTEM
                && (!WindowUtils.isSystemBar(context, window)
                    || !SettingsUtils.allowLinksOutOfSettings(
                        context))) // System bar in SUW is navigable.
            || (type == AccessibilityWindowInfo.TYPE_MAGNIFICATION_OVERLAY)
            || (type == AccessibilityWindowInfo.TYPE_INPUT_METHOD)
            || (Role.getRole(window.getRoot()) == ROLE_FLOATING_ACTION_BUTTON));
      } else {
        boolean focusContainedInApplicationWindow =
            FormFactorUtils.isAndroidPc()
                && FeatureFlagReader.focusContainedInApplicationWindow(context);
        // When the focus should be contained in the application window,
        // application window type is filtered out.
        return ((!focusContainedInApplicationWindow
                && type == AccessibilityWindowInfo.TYPE_APPLICATION)
            || (type == AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER)
            || (type == AccessibilityWindowInfo.TYPE_SYSTEM
                && (!WindowUtils.isSystemBar(context, window)
                    || !SettingsUtils.allowLinksOutOfSettings(
                        context))) // System bar in SUW is navigable.
            || (type == AccessibilityWindowInfo.TYPE_MAGNIFICATION_OVERLAY)
            || (includeKeyboard && type == AccessibilityWindowInfo.TYPE_INPUT_METHOD)
            || (Role.getRole(window.getRoot()) == ROLE_FLOATING_ACTION_BUTTON));
      }
    }
  }

  private static class WindowNavigationFilter extends Filter<AccessibilityWindowInfo> {
    final Context context;
    final UniversalSearchActor.State searchState;

    WindowNavigationFilter(Context context, UniversalSearchActor.State searchState) {
      this.context = context;
      this.searchState = searchState;
    }

    @Override
    public boolean accept(AccessibilityWindowInfo window) {
      if (window == null) {
        return false;
      }

      int type = AccessibilityWindowInfoUtils.getType(window);
      if (searchState.isUiVisible()) {
        return isSearchOverlay(context, window)
            || (type == AccessibilityWindowInfo.TYPE_INPUT_METHOD)
            || (type == AccessibilityWindowInfo.TYPE_SYSTEM)
            || (type == AccessibilityWindowInfo.TYPE_MAGNIFICATION_OVERLAY)
            || (Role.getRole(window.getRoot()) == ROLE_FLOATING_ACTION_BUTTON);
      } else {
        return (type == AccessibilityWindowInfo.TYPE_APPLICATION)
            || (type == AccessibilityWindowInfo.TYPE_INPUT_METHOD)
            || (type == AccessibilityWindowInfo.TYPE_SYSTEM)
            || (type == AccessibilityWindowInfo.TYPE_MAGNIFICATION_OVERLAY)
            || (Role.getRole(window.getRoot()) == ROLE_FLOATING_ACTION_BUTTON);
      }
    }
  }

  private static boolean isSearchOverlay(Context context, AccessibilityWindowInfo window) {
    return (AccessibilityWindowInfoUtils.getType(window)
            == AccessibilityWindowInfoCompat.TYPE_ACCESSIBILITY_OVERLAY)
        && TextUtils.equals(window.getTitle(), context.getString(R.string.title_screen_search));
  }
}
