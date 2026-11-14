/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.google.android.accessibility.utils.input;

import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.utils.AccessibilityEventListener;
import com.google.android.accessibility.utils.AccessibilityEventUtils;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.BuildVersionUtils;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.Supplier;
import com.google.android.accessibility.utils.monitor.AudioPlaybackMonitor;
import com.google.android.accessibility.utils.monitor.TouchMonitor;
import com.google.android.accessibility.utils.output.ScrollActionRecord;
import com.google.android.accessibility.utils.output.ScrollActionRecord.UserAction;
import com.google.android.accessibility.utils.traversal.TraversalStrategy;
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SearchDirectionOrUnknown;
import com.google.android.accessibility.utils.traversal.TraversalStrategyUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Interprets {@link AccessibilityEvent#TYPE_VIEW_SCROLLED} and {@link
 * AccessibilityEvent#TYPE_WINDOW_CONTENT_CHANGED} with scroll position information.
 *
 * <p><b>Usage: Register as a {@link ScrollEventHandler} to listen to interpreted scroll events.</b>
 */
public class ScrollEventInterpreter implements AccessibilityEventListener {

  private static final String TAG = "ScrollEventInterpreter";

  private static final int SCROLL_NOISE_RANGE = 15;

  /**
   * A scroll this soon after moving to the next or previous item, in the same window and with no
   * touch since, comes from the app showing the newly focused item, as web views do.
   */
  @VisibleForTesting static final long NAVIGATION_SCROLL_MS = 750;

  /** When the last move to the next or previous item started, and in which window. Main thread. */
  private static long navigationUptime = -1;

  private static int navigationWindowId = -1;
  private static boolean webNavigation;
  private static long lastDirectTouchUptime = -1;
  private static long pendingWebActionTime = -1;
  private static long pendingDirectTouchTime = -1;
  private record WebNavigation(int window, long start, long end) {}
  private static final ArrayDeque<WebNavigation> completedWebNavigation = new ArrayDeque<>();

  /** Raw motion can arrive while the synchronous browser action is waiting for the app. */
  public static synchronized void startWebNavigation(long actionTime) {
    finishWebNavigation(actionTime);
    webNavigation = false;
    navigationUptime = -1;
    pendingWebActionTime = actionTime;
    pendingDirectTouchTime = -1;
  }

  public static synchronized void finishWebNavigationAction(
      int windowId, long actionTime, boolean succeeded) {
    if (pendingWebActionTime != actionTime) return;
    if (succeeded) onWebNavigation(windowId, actionTime);
    pendingWebActionTime = -1;
    pendingDirectTouchTime = -1;
  }

  /** The browser has synchronously moved focus; its automatic scroll events carry this time. */
  public static synchronized void onWebNavigation(int windowId, long actionTime) {
    long touchBoundary = pendingWebActionTime == actionTime ? pendingDirectTouchTime
        : lastDirectTouchUptime >= actionTime ? lastDirectTouchUptime : -1;
    boolean directTouchSinceAction = touchBoundary >= actionTime;
    navigationUptime = directTouchSinceAction ? -1 : actionTime;
    navigationWindowId = windowId;
    webNavigation = !directTouchSinceAction;
    completedWebNavigation.addLast(new WebNavigation(
        windowId, actionTime, directTouchSinceAction ? touchBoundary
            : android.os.SystemClock.uptimeMillis()));
    if (completedWebNavigation.size() > 32) completedWebNavigation.removeFirst();
  }

  /**
   * A delayed event created during a completed browser action still belongs to it, even if a
   * newer touch or command has already cleared the current navigation marker. Do not extend
   * them past a direct touch: manual scrolling must retain manual recovery.
   */
  @VisibleForTesting
  static synchronized boolean wasCompletedWebNavigation(int window, long eventTime) {
    for (WebNavigation navigation : completedWebNavigation) {
      if (navigation.window == window && eventTime >= navigation.start
          && eventTime <= navigation.end) return true;
    }
    return false;
  }

  @VisibleForTesting
  static synchronized void clearCompletedWebNavigation() { completedWebNavigation.clear(); }

  /** Matches actual browser navigation, not the absence of a recent touch. */
  public static synchronized boolean isWebNavigationEvent(AccessibilityEvent event) {
    return followsCurrentWebNavigation(event.getWindowId(), event.getEventTime())
        || wasCompletedWebNavigation(event.getWindowId(), event.getEventTime());
  }

  private static boolean followsCurrentWebNavigation(int window, long time) {
    WebNavigation current = completedWebNavigation.peekLast();
    return webNavigation && current != null && current.window == window && time >= current.start
        && time <= current.end + NAVIGATION_SCROLL_MS;
  }

  /** No navigation attribution may survive service shutdown or a window replacement. */
  public static synchronized void resetNavigation() {
    navigationUptime = -1;
    navigationWindowId = -1;
    webNavigation = false;
    lastDirectTouchUptime = -1;
    pendingWebActionTime = -1;
    pendingDirectTouchTime = -1;
    completedWebNavigation.clear();
  }

  /**
   * Forgets browser navigation when a window changes state. A native app's scroll to show the next
   * item can follow such a change, so its navigation marker stays.
   */
  private static synchronized void resetWebNavigation() {
    if (webNavigation) {
      navigationUptime = -1;
      navigationWindowId = -1;
    }
    webNavigation = false;
    pendingWebActionTime = -1;
    pendingDirectTouchTime = -1;
    completedWebNavigation.clear();
  }

  @VisibleForTesting
  static synchronized boolean followsCurrentNavigation(int window, long time) {
    return webNavigation ? followsCurrentWebNavigation(window, time)
        : followsNavigation(window, time, navigationWindowId, navigationUptime);
  }

  /** Records that a move to the next or previous item in window {@code windowId} starts now. */
  public static synchronized void onNavigation(int windowId) {
    finishWebNavigation(android.os.SystemClock.uptimeMillis());
    webNavigation = false;
    navigationUptime = android.os.SystemClock.uptimeMillis();
    navigationWindowId = windowId;
  }

  /** Forgets the last move to the next or previous item, such as when a touch starts. */
  public static synchronized void clearNavigation() {
    clearMarkersAt(android.os.SystemClock.uptimeMillis());
  }

  /** A new one-finger touch may be the next flick, not direct scrolling of the app. */
  public static synchronized void onTouchInteractionStart() {
    if (!webNavigation) clearNavigation();
  }

  /** Called before multi-finger or delegated motion can scroll the app directly. */
  public static synchronized void clearNavigationAt(long touchTime) {
    lastDirectTouchUptime = Math.max(lastDirectTouchUptime, touchTime);
    if (pendingWebActionTime >= 0 && touchTime >= pendingWebActionTime) {
      pendingDirectTouchTime = pendingDirectTouchTime < 0 ? touchTime
          : Math.min(pendingDirectTouchTime, touchTime);
    }
    // A delayed motion may precede the current command but still cap an older span.
    ArrayDeque<WebNavigation> capped = new ArrayDeque<>();
    for (WebNavigation span : completedWebNavigation) {
      capped.addLast(span.start <= touchTime && touchTime < span.end
          ? new WebNavigation(span.window, span.start, touchTime) : span);
    }
    completedWebNavigation.clear();
    completedWebNavigation.addAll(capped);
    clearMarkersAt(touchTime);
  }

  private static void clearMarkersAt(long touchTime) {
    // A queued old motion cannot clear a command that began after that motion.
    if (navigationUptime > touchTime) return;
    finishWebNavigation(touchTime);
    navigationUptime = -1;
    webNavigation = false;
  }

  private static void finishWebNavigation(long end) {
    if (!webNavigation || completedWebNavigation.isEmpty()) return;
    WebNavigation previous = completedWebNavigation.removeLast();
    completedWebNavigation.addLast(new WebNavigation(previous.window, previous.start,
        Math.max(previous.start, Math.min(end, previous.end + NAVIGATION_SCROLL_MS))));
  }

  /**
   * Whether a scroll event of window {@code windowId} at {@code eventUptime} follows a move to the
   * next or previous item that started at {@code navigationUptime} in window {@code
   * navigationWindowId}.
   */
  @VisibleForTesting
  static boolean followsNavigation(
      int windowId, long eventUptime, int navigationWindowId, long navigationUptime) {
    return navigationUptime >= 0
        && windowId == navigationWindowId
        && eventUptime >= navigationUptime
        && eventUptime - navigationUptime <= NAVIGATION_SCROLL_MS;
  }

  // The record for the current scroll instance Id which we already have handled.
  private int handledScrollInstanceId = -1;

  /** Contains interpreted information in addition to {@link AccessibilityEvent}. */
  public static class ScrollEventInterpretation {

    public static final ScrollEventInterpretation DEFAULT_INTERPRETATION =
        new ScrollEventInterpretation(
            ScrollActionRecord.ACTION_UNKNOWN,
            TraversalStrategy.SEARCH_FOCUS_UNKNOWN,
            /* hasValidIndex= */ false,
            /* isDuplicateEvent= */ false,
            /* isFromScrollable= */ false,
            /* isMediaPlayerAutoScroll= */ false,
            SCROLL_INSTANCE_ID_UNDEFINED);

    /** Source {@link UserAction} that leads to the scroll event. */
    @UserAction public final int userAction;

    @SearchDirectionOrUnknown public final int scrollDirection;

    /**
     * Sets to {@code true} if the event has valid AdapterView index(fromIndex, toIndex) or valid
     * ScrollView index(scrollX, scrollY).
     */
    public final boolean hasValidIndex;

    /** Sets to {@code true} if the event reports the same scroll position as the previous event. */
    public final boolean isDuplicateEvent;

    public final boolean isFromScrollable;
    public final boolean isMediaPlayerAutoScroll;

    /**
     * Created at {@link #onScrollActionCompat(int, AccessibilityNodeInfoCompat, long)} to identify
     * auto scroll action.
     */
    public final int scrollInstanceId;

    /**
     * Whether the event comes from a scroll that moving to the next or previous item made, rather
     * than one that the user asked for.
     */
    public final boolean isFromNavigation;

    /** Whether the event comes from SandFly scrolling a list ahead of the focused item. */
    public final boolean isFromScrollAhead;

    @VisibleForTesting
    public ScrollEventInterpretation(
        @UserAction int userAction,
        @SearchDirectionOrUnknown int scrollDirection,
        boolean hasValidIndex,
        boolean isDuplicateEvent,
        boolean isFromScrollable,
        boolean isMediaPlayerAutoScroll,
        int scrollInstanceId) {
      this(
          userAction,
          scrollDirection,
          hasValidIndex,
          isDuplicateEvent,
          isFromScrollable,
          isMediaPlayerAutoScroll,
          scrollInstanceId,
          /* isFromNavigation= */ false,
          /* isFromScrollAhead= */ false);
    }

    public ScrollEventInterpretation(
        @UserAction int userAction,
        @SearchDirectionOrUnknown int scrollDirection,
        boolean hasValidIndex,
        boolean isDuplicateEvent,
        boolean isFromScrollable,
        boolean isMediaPlayerAutoScroll,
        int scrollInstanceId,
        boolean isFromNavigation,
        boolean isFromScrollAhead) {
      this.isFromNavigation = isFromNavigation;
      this.isFromScrollAhead = isFromScrollAhead;
      this.userAction = userAction;
      this.scrollDirection = scrollDirection;
      this.hasValidIndex = hasValidIndex;
      this.isDuplicateEvent = isDuplicateEvent;
      this.isFromScrollable = isFromScrollable;
      this.isMediaPlayerAutoScroll = isMediaPlayerAutoScroll;
      this.scrollInstanceId = scrollInstanceId;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("ScrollEventInterpretation{");
      sb.append("userAction=").append(ScrollActionRecord.userActionToString(userAction));
      sb.append(", scrollDirection=")
          .append(TraversalStrategyUtils.directionToString(scrollDirection));
      sb.append(", hasValidIndex=").append(hasValidIndex);
      sb.append(", isDuplicateEvent=").append(isDuplicateEvent);
      sb.append(", isFromScrollable=").append(isFromScrollable);
      sb.append(", isMediaPlayerAutoScroll=").append(isMediaPlayerAutoScroll);
      sb.append(", scrollInstanceId=").append(scrollInstanceId);
      sb.append(", isFromNavigation=").append(isFromNavigation);
      sb.append(", isFromScrollAhead=").append(isFromScrollAhead);
      sb.append('}');
      return sb.toString();
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof ScrollEventInterpretation)) {
        return false;
      }

      ScrollEventInterpretation that = (ScrollEventInterpretation) o;

      return (userAction == that.userAction)
          && (scrollDirection == that.scrollDirection)
          && (hasValidIndex == that.hasValidIndex)
          && (isDuplicateEvent == that.isDuplicateEvent)
          && (scrollInstanceId == that.scrollInstanceId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          userAction, scrollDirection, hasValidIndex, isDuplicateEvent, scrollInstanceId);
    }
  }

  /**
   * Listens to interpreted {@link AccessibilityEvent#TYPE_VIEW_SCROLLED} and {@link
   * AccessibilityEvent#TYPE_WINDOW_CONTENT_CHANGED} with scroll position information.
   */
  public interface ScrollEventHandler {
    void onScrollEvent(
        AccessibilityEvent event, ScrollEventInterpretation interpretation, EventId eventId);
  }

  /**
   * Timeout to determine whether a scroll event could be resulted from the last scroll action.
   * REFERTO Extends the timeout due to late TYPE_VIEW_SCROLLED event.
   */
  public enum ScrollTimeout {
    SCROLL_TIMEOUT_LONG(1000),
    SCROLL_TIMEOUT_SHORT(500);

    private final int timeoutMillis;

    ScrollTimeout(int timeoutMillis) {
      this.timeoutMillis = timeoutMillis;
    }

    public int getTimeoutMillis() {
      return this.timeoutMillis;
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////
  // Constants

  /** Undefined scroll position index. */
  private static final int INDEX_UNDEFINED = -1;

  /** Undefined auto-scroll action scrollInstanceId. */
  private static final int SCROLL_INSTANCE_ID_UNDEFINED = -1;

  /** Event types that are handled by ScrollEventInterpreter. */
  private static final int MASK_EVENTS =
      AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
          | AccessibilityEvent.TYPE_VIEW_SCROLLED
          | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;

  //////////////////////////////////////////////////////////////////////////////////////////
  // Member data

  // Inputs
  private Supplier<ScrollActionRecord> scrollActorState;
  private Supplier<ScrollActionRecord> finishedScrollRecord = () -> null;
  private final @Nullable AudioPlaybackMonitor audioPlaybackMonitor;
  private final @NonNull TouchMonitor touchMonitor;

  /** Maps from scrollable node id to scroll position information. */
  private final HashMap<NodeIdentifier, PositionInfo> cachedPositionInfo = new HashMap<>();

  // Outputs
  private final List<ScrollEventHandler> listeners = new ArrayList<>();

  private final boolean supportMultipleAutoScroll;

  //////////////////////////////////////////////////////////////////////////////////////////
  // Construction

  public ScrollEventInterpreter(
      @Nullable AudioPlaybackMonitor audioPlaybackMonitor,
      @NonNull TouchMonitor touchMonitor,
      boolean supportMultipleAutoScroll) {
    this.audioPlaybackMonitor = audioPlaybackMonitor;
    this.touchMonitor = touchMonitor;
    this.supportMultipleAutoScroll = supportMultipleAutoScroll;
  }

  /**
   * Sets where to find the last auto-scroll that was reset as complete. Its scroll events that
   * come after, as an animated scroll finishes, are not mistaken for a manual scroll.
   */
  public void setFinishedScrollActionRecord(Supplier<ScrollActionRecord> finishedScrollRecord) {
    this.finishedScrollRecord = finishedScrollRecord;
  }

  public void setScrollActorState(Supplier<ScrollActionRecord> scrollActorState) {
    this.scrollActorState = scrollActorState;
  }

  public void addListener(ScrollEventHandler listener) {
    listeners.add(listener);
  }

  //////////////////////////////////////////////////////////////////////////////////////////
  // Methods

  @Override
  public int getEventTypes() {
    return MASK_EVENTS;
  }

  @SuppressLint("SwitchIntDef") // Only a subset of event-types are handled.
  @Override
  public void onAccessibilityEvent(AccessibilityEvent event, EventId eventId) {
    switch (event.getEventType()) {
      case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
        // Window state changes clear the cache.
        cachedPositionInfo.clear();
        resetWebNavigation();
        break;
      case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
        if (((event.getContentChangeTypes() & AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) == 0)
            || !hasValidIndex(event)) {
          break;
        }
        // fall through
      case AccessibilityEvent.TYPE_VIEW_SCROLLED:
        if (Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU && isWebNavigationEvent(event)
            && scrollActorState != null && scrollActorState.get() == null
            && finishedScrollRecord.get() == null) {
          // Keep the scroll cache current for a later manual reverse scroll. Delivery stays
          // inline; only navigation position feedback is suppressed, never the cache update.
          processScrollEvent(event, eventId, readSource(event, true), true,
              isAutomaticMediaScrollEvent(event));
        } else {
          processScrollEvent(event, eventId, readSource(event, false), false,
              isAutomaticMediaScrollEvent(event));
        }
        break;
      default:
        break;
    }
  }

  @VisibleForTesting
  void notifyListenersWithInterpretation(
      AccessibilityEvent event, ScrollEventInterpretation interpretation, EventId eventId) {
    for (ScrollEventHandler listener : listeners) {
      listener.onScrollEvent(event, interpretation, eventId);
    }
  }

  /** A single source read, with no unrelated descendant prefetch for browser navigation. */
  @VisibleForTesting
  @Nullable AccessibilityNodeInfo readSource(AccessibilityEvent event, boolean fromWebNavigation) {
    if (Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU && fromWebNavigation) return event.getSource(0);
    return event.getSource();
  }

  private void processScrollEvent(AccessibilityEvent event, EventId eventId,
      AccessibilityNodeInfo source, boolean fromWebNavigation, boolean mediaScroll) {
    ScrollEventInterpretation interpretation = interpret(event, source, fromWebNavigation, mediaScroll);
    if (interpretation == null) return;
    if (source != null && interpretation.hasValidIndex) {
      NodeIdentifier key = new NodeIdentifier(source);
      PositionInfo previous = cachedPositionInfo.get(key);
      // Framework events can arrive late too. An old navigation event must not rewind a
      // position already updated by a newer manual scroll. Native/manual updates stay as before.
      if (!fromWebNavigation || previous == null || previous.eventTime <= event.getEventTime()) {
        if (interpretation.isDuplicateEvent && previous != null) {
          previous.eventTime = Math.max(previous.eventTime, event.getEventTime());
        } else {
          cachedPositionInfo.put(key, new PositionInfo(event));
        }
      }
    }
    if (fromWebNavigation) {
      interpretation = new ScrollEventInterpretation(ScrollActionRecord.ACTION_UNKNOWN,
          interpretation.scrollDirection, false, interpretation.isDuplicateEvent,
          interpretation.isFromScrollable, interpretation.isMediaPlayerAutoScroll,
          interpretation.scrollInstanceId, true, false);
    }
    notifyListenersWithInterpretation(event, interpretation, eventId);
  }

  @Nullable
  private ScrollEventInterpretation interpret(AccessibilityEvent event,
      AccessibilityNodeInfo sourceNode, boolean fromWebNavigation, boolean mediaScroll) {
    if (sourceNode == null) {
      return ScrollEventInterpretation.DEFAULT_INTERPRETATION;
    }

    // REFERTO. Get scroll position WINDOW_CONTENT_CHANGED events for more fine-grained
    // scroll action detection. We need to carefully handle these events. Event if the position
    // changes, it might not result from scroll action. We need a solid strategy to protect against
    // this issue. It's difficult. The least effort we can do is to filter out non-scrollable node.
    if ((event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
        && !AccessibilityNodeInfoUtils.isScrollable(AccessibilityNodeInfoCompat.wrap(sourceNode))) {
      return ScrollEventInterpretation.DEFAULT_INTERPRETATION;
    }

    final NodeIdentifier sourceNodeIdentifier = new NodeIdentifier(sourceNode);

    @SearchDirectionOrUnknown
    final int scrollDirection = getScrollDirection(sourceNodeIdentifier, event);
    @UserAction final int userAction;
    final int scrollInstanceId;
    boolean isFromNavigation = false;
    boolean isFromScrollAhead = false;

    @Nullable ScrollActionRecord autoScrollRecord = fromWebNavigation ? null : getAutoScrollRecordIfAvailable(event);
    if (autoScrollRecord == null) {
      scrollInstanceId = SCROLL_INSTANCE_ID_UNDEFINED;
      // Note that TYPE_WINDOW_CONTENT_CHANGED events can also be interpreted as manual scroll
      // action. TYPE_VIEW_SCROLLED events are filed at very coarse granularity. If we rely only on
      // TYPE_VIEW_SCROLLED events to detect manual scroll action, it happens when the user slightly
      // scroll a list and accessibility focus goes off screen, we don't receive
      // TYPE_VIEW_SCROLLED event thus we don't update accessibility focus. If user performs linear
      // navigation after that, accessibility focus might go to the beginning of screen.
      // We take into account TYPE_WINDOW_CONTENT_CHANGED events to provide more
      // fine-grained manual scroll callback.
      if (fromWebNavigation) {
        userAction = ScrollActionRecord.ACTION_UNKNOWN;
        isFromNavigation = true;
      } else if (isFromFinishedAutoScroll(event)) {
        // TalkBack's own scroll, still moving after it went far enough to count as complete. As a
        // manual scroll, it would move the focus that the scroll has just placed.
        userAction = ScrollActionRecord.ACTION_UNKNOWN;
        isFromNavigation = isNavigationScroll(finishedScrollRecord.get());
        isFromScrollAhead = isScrollAhead(finishedScrollRecord.get());
        LogUtils.i(TAG, "End of an auto scroll");
      } else if (followsCurrentNavigation(event.getWindowId(), event.getEventTime())) {
        // The app scrolled to show the item that moving to the next or previous item focused,
        // such as a web view showing a web element.
        userAction = ScrollActionRecord.ACTION_UNKNOWN;
        isFromNavigation = true;
        LogUtils.i(TAG, "Scroll that shows the next or previous item");
      } else {
        userAction =
            scrollDirection == TraversalStrategy.SEARCH_FOCUS_UNKNOWN
                ? ScrollActionRecord.ACTION_UNKNOWN
                : ScrollActionRecord.ACTION_MANUAL_SCROLL;
        LogUtils.i(TAG, "Manual scrolling");
      }
    } else {
      scrollInstanceId = autoScrollRecord.scrollInstanceId;
      userAction = autoScrollRecord.userAction;
      isFromNavigation = isNavigationScroll(autoScrollRecord);
      isFromScrollAhead = isScrollAhead(autoScrollRecord);
      if (handledScrollInstanceId < scrollInstanceId) {
        // We record the current scroll instance Id so if we handle this Id again, it would mean
        // that it is from the multiple scroll attempts trigger from the same scroll action.
        handledScrollInstanceId = scrollInstanceId;
      }
      LogUtils.i(TAG, "Auto scrolling: autoScrollRecord=%s", autoScrollRecord);
    }

    final boolean hasValidIndex = hasValidIndex(event);
    final boolean isDuplicateEvent = hasValidIndex && isDuplicateEvent(sourceNodeIdentifier, event);
    final boolean isMediaPlayerAutoScroll = mediaScroll;
    final boolean isFromScrollable = event.getEventType() == TYPE_VIEW_SCROLLED && sourceNode.isScrollable();

    ScrollEventInterpretation interpretation =
        new ScrollEventInterpretation(
            userAction,
            scrollDirection,
            hasValidIndex,
            isDuplicateEvent,
            isFromScrollable,
            isMediaPlayerAutoScroll,
            scrollInstanceId,
            isFromNavigation,
            isFromScrollAhead);
    LogUtils.d(TAG, "interpret ScrollEventInterpretation=%s", interpretation);
    return interpretation;
  }

  /** Returns AutoScrollRecord if the {@code event} is resulted from cached auto-scroll action. */
  private @Nullable ScrollActionRecord getAutoScrollRecordIfAvailable(AccessibilityEvent event) {
    @Nullable ScrollActionRecord autoScrollRecord = scrollActorState.get();
    if (autoScrollRecord == null) {
      return null;
    }

    if (isFromAutoScrollAction(event, autoScrollRecord)) {
      AccessibilityNodeInfoCompat node = AccessibilityEventUtils.sourceCompat(event);
      return autoScrollRecord.scrolledNodeMatches(node) ? autoScrollRecord : null;
    } else {
      return null;
    }
  }

  /**
   * Whether the {@code event} comes from the last auto-scroll that was reset as complete, from the
   * same node and within the time an auto-scroll record lasts.
   */
  private boolean isFromFinishedAutoScroll(AccessibilityEvent event) {
    @Nullable ScrollActionRecord finished = finishedScrollRecord.get();
    return finished != null
        && isFromAutoScrollAction(event, finished)
        && finished.scrolledNodeMatches(AccessibilityEventUtils.sourceCompat(event));
  }

  /** Whether {@code record} is of a scroll that moving to the next or previous item made. */
  private static boolean isNavigationScroll(@Nullable ScrollActionRecord record) {
    return record != null
        && record.userAction == ScrollActionRecord.ACTION_AUTO_SCROLL
        && (ScrollActionRecord.FOCUS.equals(record.scrollSource) || isScrollAhead(record));
  }

  /** Whether {@code record} is of SandFly scrolling a list ahead of the focused item. */
  private static boolean isScrollAhead(@Nullable ScrollActionRecord record) {
    return record != null && ScrollActionRecord.SCROLL_AHEAD.equals(record.scrollSource);
  }

  /** Checks whether the {@code event} is resulted from cached auto-scroll action. */
  private boolean isFromAutoScrollAction(
      AccessibilityEvent event, ScrollActionRecord autoScrollRecord) {

    long timeDiff = event.getEventTime() - autoScrollRecord.autoScrolledTime;
    LogUtils.v(TAG, "timeDiff=%d", timeDiff);

    if (supportMultipleAutoScroll
        && handledScrollInstanceId == autoScrollRecord.scrollInstanceId
        && (timeDiff <= ScrollTimeout.SCROLL_TIMEOUT_LONG.getTimeoutMillis())) {
      // For Wear, we may perform auto-scroll multiple times to find a specific node (i.e. a
      // heading). Sometimes, the timing of an a11y event from previous auto-scrolling action would
      // be delayed after the timing of current auto-scroll action. So, we mitigate it by checking
      // the same scroll instance Id given that it doesn't exceed time out.
      // auto_scroll_attempt_1 - > event_scroll_1_1 ->  auto_scroll_attempt_2 -> event_scroll_1_2
      // => event_scroll_1_2.getEventTime() < auto_scroll_attempt_2.autoScrolledTime
      LogUtils.i(
          TAG,
          "This event is from the multiple scroll attempts triggered from the same scroll action."
              + " (scrollInstanceId=%d)",
          handledScrollInstanceId);
      return true;
    }

    // Note that both TYPE_VIEW_SCROLLED and TYPE_WINDOW_CONTENT_CHANGED events could result from
    // auto-scroll action. And a single scroll action can trigger multiple scroll events, we'll keep
    // the autoScrollRecord until next auto scroll happened and use the time diff to distinguish if
    // the current scroll is from the same scroll action.
    return (timeDiff >= 0L) && (timeDiff <= ScrollTimeout.SCROLL_TIMEOUT_LONG.getTimeoutMillis());
  }

  /**
   * Calculates the scroll direction by current AccessibilityEvent and previous one, also filters
   * small noises to prevent false alarms.
   */
  @SearchDirectionOrUnknown
  private int getScrollDirection(NodeIdentifier sourceNodeIdentifier, AccessibilityEvent event) {
    PositionInfo previousPosition = cachedPositionInfo.get(sourceNodeIdentifier);
    if (previousPosition == null) {
      if (BuildVersionUtils.isAtLeastR()) {
        return getScrollDirectionFromDeltas(event);
      }
      return TraversalStrategy.SEARCH_FOCUS_UNKNOWN;
    }

    // Checks scroll of AdapterViews and doesn't care toIndex because changing of toIndex might be
    // expanding list-item only.
    if (event.getFromIndex() != INDEX_UNDEFINED && previousPosition.fromIndex != INDEX_UNDEFINED) {
      if (event.getFromIndex() > previousPosition.fromIndex) {
        return TraversalStrategy.SEARCH_FOCUS_FORWARD;
      } else if (event.getFromIndex() < previousPosition.fromIndex) {
        return TraversalStrategy.SEARCH_FOCUS_BACKWARD;
      }
    }

    // Checks scroll of ScrollViews and ignores small noises.
    if (event.getScrollX() > (previousPosition.scrollX + SCROLL_NOISE_RANGE)
        || event.getScrollY() > (previousPosition.scrollY + SCROLL_NOISE_RANGE)) {
      return TraversalStrategy.SEARCH_FOCUS_FORWARD;
    } else if (event.getScrollX() < (previousPosition.scrollX - SCROLL_NOISE_RANGE)
        || event.getScrollY() < (previousPosition.scrollY - SCROLL_NOISE_RANGE)) {
      return TraversalStrategy.SEARCH_FOCUS_BACKWARD;
    }

    if (BuildVersionUtils.isAtLeastR()) {
      return getScrollDirectionFromDeltas(event);
    }

    return TraversalStrategy.SEARCH_FOCUS_UNKNOWN;
  }

  @TargetApi(VERSION_CODES.R)
  @SearchDirectionOrUnknown
  private int getScrollDirectionFromDeltas(AccessibilityEvent event) {
    int scrollDeltaX = event.getScrollDeltaX();
    int scrollDeltaY = event.getScrollDeltaY();
    if (Math.abs(scrollDeltaX) < SCROLL_NOISE_RANGE
        && Math.abs(scrollDeltaY) < SCROLL_NOISE_RANGE) {
      return TraversalStrategy.SEARCH_FOCUS_UNKNOWN;
    }
    // Gets the scroll delta value but ignores the small noises.
    if (scrollDeltaX - SCROLL_NOISE_RANGE > 0 || scrollDeltaY - SCROLL_NOISE_RANGE > 0) {
      return TraversalStrategy.SEARCH_FOCUS_FORWARD;
    } else if (scrollDeltaX + SCROLL_NOISE_RANGE < 0 || scrollDeltaY + SCROLL_NOISE_RANGE < 0) {
      return TraversalStrategy.SEARCH_FOCUS_BACKWARD;
    }
    return TraversalStrategy.SEARCH_FOCUS_UNKNOWN;
  }

  private boolean isDuplicateEvent(NodeIdentifier sourceNodeIdentifier, AccessibilityEvent event) {
    final PositionInfo positionInfo = cachedPositionInfo.get(sourceNodeIdentifier);
    return (positionInfo != null) && positionInfo.equals(new PositionInfo(event));
  }

  private static boolean hasValidIndex(AccessibilityEvent event) {
    return hasValidAdapterViewIndex(event)
        || hasValidScrollViewIndex(event)
        || AccessibilityEventUtils.hasValidScrollDelta(event);
  }

  private static boolean hasValidAdapterViewIndex(AccessibilityEvent event) {
    return (event.getFromIndex() != INDEX_UNDEFINED)
        && (event.getToIndex() != INDEX_UNDEFINED)
        && (event.getItemCount() > 0);
  }

  private static boolean hasValidScrollViewIndex(AccessibilityEvent event) {
    return (event.getScrollX() > INDEX_UNDEFINED || event.getScrollY() > INDEX_UNDEFINED)
        && (event.getMaxScrollX() > 0 || event.getMaxScrollY() > 0);
  }

  /**
   * Whether the scroll event was probably generated automatically by playing media.
   *
   * <p>Particularly for videos with captions enabled, new scroll events fire every second or so.
   * Earcons should not play for these events because they don't provide useful information and are
   * distracting.
   */
  private boolean isAutomaticMediaScrollEvent(@NonNull AccessibilityEvent event) {
    if (event.getEventType() != TYPE_VIEW_SCROLLED) {
      return false;
    }
    // TODO: Investigate a more accurate way to determine whether a scroll event was
    // user initiated or initiated by playing media.
    return audioPlaybackMonitor != null
        && audioPlaybackMonitor.isPlaybackSourceActive(
            AudioPlaybackMonitor.PlaybackSource.USAGE_MEDIA)
        && !touchMonitor.isUserTouchingScreen();
  }

  /** Caches scroll position from {@link AccessibilityEvent}. */
  private static class PositionInfo {
    private long eventTime;
    private final int fromIndex;
    private final int toIndex;
    private final int scrollX;
    private final int scrollY;
    private final int itemCount;
    private final int scrollDeltaX;
    private final int scrollDeltaY;

    PositionInfo(AccessibilityEvent event) {
      eventTime = event.getEventTime();
      fromIndex = event.getFromIndex();
      toIndex = event.getToIndex();
      scrollX = event.getScrollX();
      scrollY = event.getScrollY();
      itemCount = event.getItemCount();
      scrollDeltaX = AccessibilityEventUtils.getScrollDeltaX(event);
      scrollDeltaY = AccessibilityEventUtils.getScrollDeltaY(event);
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof PositionInfo)) {
        return false;
      }

      PositionInfo that = (PositionInfo) o;

      return (fromIndex == that.fromIndex)
          && (toIndex == that.toIndex)
          && (scrollX == that.scrollX)
          && (scrollY == that.scrollY)
          && (itemCount == that.itemCount)
          && (scrollDeltaY == that.scrollDeltaY)
          && (scrollDeltaX == that.scrollDeltaX);
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          fromIndex, toIndex, scrollX, scrollY, itemCount, scrollDeltaX, scrollDeltaY);
    }
  }

  /** Light-weight identifier for {@link AccessibilityNodeInfo}. */
  private static class NodeIdentifier {
    private final int nodeHashCode;
    private final int windowId;

    NodeIdentifier(AccessibilityNodeInfo node) {
      nodeHashCode = node.hashCode();
      windowId = node.getWindowId();
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof NodeIdentifier)) {
        return false;
      }

      NodeIdentifier that = (NodeIdentifier) o;

      return (nodeHashCode == that.nodeHashCode) && (windowId == that.windowId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(nodeHashCode, windowId);
    }
  }
}
