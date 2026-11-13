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

package com.google.android.accessibility.talkback.interpreters;

import com.google.android.accessibility.utils.input.ScrollEventInterpreter;

import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED;
import static com.google.android.accessibility.talkback.Interpretation.Touch.Action.LIFT;
import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.ActorState;
import com.google.android.accessibility.talkback.Interpretation;
import com.google.android.accessibility.talkback.Interpretation.ManualScroll;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.actor.ImageCaptioner;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics;
import com.google.android.accessibility.talkback.compositor.AccessibilityFocusEventInterpretation;
import com.google.android.accessibility.talkback.compositor.AccessibilityFocusEventInterpreter;
import com.google.android.accessibility.talkback.compositor.Compositor;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor;
import com.google.android.accessibility.talkback.focusmanagement.ColumnWrap;
import com.google.android.accessibility.talkback.focusmanagement.FocusProcessorForScreenStateChange;
import com.google.android.accessibility.talkback.focusmanagement.FocusProcessorForTapAndTouchExploration;
import com.google.android.accessibility.talkback.focusmanagement.FocusProcessorForTapAndTouchExploration.TypingMethod;
import com.google.android.accessibility.talkback.focusmanagement.action.TouchExplorationAction;
import com.google.android.accessibility.talkback.focusmanagement.interpreter.ScreenState;
import com.google.android.accessibility.talkback.focusmanagement.interpreter.ScreenStateMonitor;
import com.google.android.accessibility.talkback.focusmanagement.interpreter.ScreenStateMonitor.ScreenStateChangeListener;
import com.google.android.accessibility.talkback.focusmanagement.interpreter.TouchExplorationInterpreter.TouchExplorationActionListener;
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo;
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionRecord;
import com.google.android.accessibility.talkback.focusmanagement.record.NodePathDescription;
import com.google.android.accessibility.talkback.interpreters.InputFocusInterpreter.TargetViewChangeListener;
import com.google.android.accessibility.talkback.interpreters.ManualScrollInterpreter.ManualScrollInterpretation;
import com.google.android.accessibility.talkback.interpreters.ManualScrollInterpreter.ScrolledViewChangeListener;
import com.google.android.accessibility.utils.AccessibilityEventUtils;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.BuildVersionUtils;
import com.google.android.accessibility.utils.DisplayUtils;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.accessibility.utils.Performance;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.Role;
import com.google.android.accessibility.utils.WebInterfaceUtils;
import com.google.android.accessibility.utils.labeling.Label;
import com.google.android.accessibility.utils.monitor.TouchMonitor;
import com.google.android.accessibility.utils.traversal.TraversalStrategy;
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SearchDirection;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Accessibility focus event interpreter that consumes semi-interpreted events from other
 * event-interpreters, then sends completed interpretations to pipeline.
 */
public class AccessibilityFocusInterpreter
    implements AccessibilityFocusEventInterpreter,
        ScreenStateChangeListener,
        ScrolledViewChangeListener,
        TouchExplorationActionListener,
        TargetViewChangeListener {
  public static final String TAG = "A11yFocusInterp";

  // Scroll events arrive many times per second, and finding a new focus target queries the app's
  // node tree over IPC on the main thread. Waiting for scrolling to settle keeps touch responsive.
  private static final long MANUAL_SCROLL_SETTLE_MS = 150;

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Member variables

  private final FocusProcessorForTapAndTouchExploration focusProcessorForTapAndTouchExploration;
  private final FocusProcessorForScreenStateChange focusProcessorForScreenStateChange;
  private final AccessibilityFocusMonitor accessibilityFocusMonitor;
  private final ScreenStateMonitor.State screenState;
  private final TouchMonitor touchMonitor;
  private final Context context;

  private Pipeline.InterpretationReceiver pipelineInterpretations;
  private ActorState actorState;

  private final Handler handler = new Handler(Looper.getMainLooper());

  /** Tells the user's scroll of web content from the scrolls the page makes in response. */
  private final WebScrollSource webScrollSource = new WebScrollSource();

  /** The last scroll event of the user's scroll of web content. */
  private @Nullable ManualScrollInterpretation webScroll;

  /**
   * Whether the page scrolled a strip that holds the focus and scrolls only sideways, after the
   * user scrolled other content.
   */
  private boolean pageScrolledFocusStrip;

  /**
   * The last strip of web content, scrolling only sideways, that the page scrolled while the user
   * scrolled other content, such as category tabs fixed below a page header.
   */
  private @Nullable AccessibilityNodeInfoCompat webStrip;

  private @Nullable ManualScrollInterpretation pendingManualScroll;
  private final Runnable handlePendingManualScroll =
      new Runnable() {
        @Override
        public void run() {
          ManualScrollInterpretation interpretation = pendingManualScroll;
          // A strip can still be moving from the page's last scroll when the user starts the next
          // one, so wait for the user's scroll until the fingers leave the screen.
          if (interpretation != null
              && interpretation == webScroll
              && webScrollSource.userScrollIsSideways()
              && touchMonitor.isUserTouchingScreen()) {
            handler.postDelayed(this, MANUAL_SCROLL_SETTLE_MS);
            return;
          }
          boolean stripScrolled = pageScrolledFocusStrip;
          pendingManualScroll = null;
          pageScrolledFocusStrip = false;
          if (interpretation != null) {
            handleManualScroll(interpretation, stripScrolled);
          }
        }
      };

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Construction methods

  public AccessibilityFocusInterpreter(
      Context context,
      AccessibilityFocusMonitor accessibilityFocusMonitor,
      ScreenStateMonitor.State screenState,
      TouchMonitor touchMonitor,
      TalkBackAnalytics analytics) {
    this.context = context;
    this.accessibilityFocusMonitor = accessibilityFocusMonitor;
    this.screenState = screenState;
    this.touchMonitor = touchMonitor;
    focusProcessorForTapAndTouchExploration =
        new FocusProcessorForTapAndTouchExploration(context, analytics);
    focusProcessorForScreenStateChange =
        new FocusProcessorForScreenStateChange(accessibilityFocusMonitor);
  }

  public void performSplitTap(EventId eventId) {
    if (!focusProcessorForTapAndTouchExploration.performSplitTap(eventId)) {
      // Check whether the FocusProcessorForTapAndTouchExploration#performSplitTap succeeds.
      // TODO: Split-tap should be activated everywhere, not just IME.
      // If FocusProcessorForTapAndTouchExploration#performSplitTap returns false (
      // support the Text-Entry-Key for lift-to-type, we should enable this.
      if (FeatureSupport.supportSplitTapEverywhere()
          && FeatureFlagReader.splitTapEverywhere(context)) {
        performClick(eventId);
      }
    }
  }

  private void performClick(EventId eventId) {
    AccessibilityNodeInfoCompat currentA11yFocusedNode =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    if (currentA11yFocusedNode != null) {
      pipelineInterpretations.input(
          eventId, /* event= */ null, Interpretation.Touch.create(LIFT, currentA11yFocusedNode));
    }
  }

  public void setPipeline(Pipeline.InterpretationReceiver pipeline) {
    this.pipelineInterpretations = pipeline;
    this.focusProcessorForTapAndTouchExploration.setInterpretationReceiver(pipeline);
    focusProcessorForScreenStateChange.setPipeline(pipeline);
  }

  public void setActorState(ActorState actorState) {
    this.actorState = actorState;
    focusProcessorForTapAndTouchExploration.setActorState(actorState);
    focusProcessorForScreenStateChange.setActorState(actorState);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Dispatches user actions

  /** Called by TouchExplorationInterpreter. */
  @Override
  public boolean onTouchExplorationAction(TouchExplorationAction action, EventId eventId) {
    LogUtils.d(TAG, "User action: %s", action);
    return focusProcessorForTapAndTouchExploration.onTouchExplorationAction(action, eventId);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Notifies changes from framework

  /** Called by {@link ScreenStateMonitor}. */
  @Override
  public boolean onScreenStateChanged(ScreenState screenState, EventId eventId) {
    return focusProcessorForScreenStateChange.onScreenStateChanged(screenState, eventId);
  }

  /** Event-interpreter function, called by {@link ManualScrollInterpreter}. */
  @Override
  public void onManualScroll(ManualScrollInterpretation interpretation) {
    if (FormFactorUtils.isAndroidWear()) {
      handleManualScroll(interpretation, /* stripScrolled= */ false);
      return;
    }
    if (onWebScroll(interpretation)) {
      return;
    }
    // A page can scroll a strip of tabs or a list of its own once the page itself stops, and that
    // scroll must not take the place of the scroll that moved the focus.
    if (pendingManualScroll == null
        || interpretation.scrolledHoldsFocus()
        || !pendingManualScroll.scrolledHoldsFocus()) {
      // Copy the event, because the framework may recycle it after dispatch.
      pendingManualScroll =
          ManualScrollInterpretation.create(
              interpretation.eventId(),
              AccessibilityEvent.obtain(interpretation.event()),
              interpretation.direction(),
              interpretation.scrolledHoldsFocus());
    }
    schedulePendingManualScroll();
  }

  @Override
  public boolean shouldIgnoreManualScroll(
      AccessibilityEvent event, AccessibilityNodeInfoCompat scrolledNode) {
    return !FormFactorUtils.isAndroidWear() && WebScrollFocus.isWebContent(scrolledNode)
        && ScrollEventInterpreter.isWebNavigationEvent(event);
  }

  /** Handles {@link #pendingManualScroll} once scrolling settles, in place of any earlier scroll. */
  private void schedulePendingManualScroll() {
    cancelWebFocusRecovery();
    handler.removeCallbacks(handlePendingManualScroll);
    handler.postDelayed(handlePendingManualScroll, MANUAL_SCROLL_SETTLE_MS);
  }

  /**
   * Keeps the scroll of web content that the user made with the fingers as the one to check, in
   * place of scrolls that the page makes in response, such as a strip of category tabs that
   * follows the menu. Returns whether {@code interpretation} was handled here.
   */
  private boolean onWebScroll(ManualScrollInterpretation interpretation) {
    AccessibilityNodeInfoCompat scrolled =
        AccessibilityEventUtils.sourceCompat(interpretation.event());
    if (scrolled == null || !WebScrollFocus.isWebContent(scrolled)) {
      return false;
    }
    if (ScrollEventInterpreter.isWebNavigationEvent(interpretation.event())) {
      // HTML navigation scrolls its new item into view too. It must not schedule manual-scroll
      // recovery and then run the native focusability fallback on the same web item.
      LogUtils.d(TAG, "Web scroll follows a focus action; no manual recovery needed");
      return true;
    }
    // Keyboard and page-script scrolls keep the original non-touch handling.
    if (!isUserScroll(interpretation)) return false;
    @WebScrollSource.Kind
    int kind = webScrollSource.onScroll(scrolled, touchMonitor.getTouchStartTime());
    if (kind == WebScrollSource.USER_START) {
      webScroll = null;
    } else if (kind == WebScrollSource.PAGE_SIDEWAYS && !webScrollSource.userScrollIsSideways()) {
      webStrip = scrolled;
    }
    if (kind == WebScrollSource.USER_START || kind == WebScrollSource.USER) {
      // Copy the event, because the framework may recycle it after dispatch.
      webScroll =
          ManualScrollInterpretation.create(
              interpretation.eventId(),
              AccessibilityEvent.obtain(interpretation.event()),
              interpretation.direction(),
              interpretation.scrolledHoldsFocus());
    } else if (webScroll != null
        && interpretation.scrolledHoldsFocus()
        && kind == WebScrollSource.PAGE_SIDEWAYS) {
      // The page moved the strip that holds the focus. Check the user's scroll again.
      pageScrolledFocusStrip = true;
    } else {
      // Another scroll that the page made, which leaves the focus where it was.
      return true;
    }
    pendingManualScroll = webScroll;
    schedulePendingManualScroll();
    return true;
  }

  private void handleManualScroll(
      ManualScrollInterpretation interpretation, boolean stripScrolled) {
    if (!screenState.areMainWindowsStable()) {
      LogUtils.w(
          TAG,
          "onScrollEvent return due to windows are not stable and the focus will"
              + " be handled by onScreenStateChanged after main windows are stable.");
      return;
    }

    AccessibilityNodeInfoCompat scrolled =
        AccessibilityEventUtils.sourceCompat(interpretation.event());
    if (!FormFactorUtils.isAndroidWear() && WebScrollFocus.isWebContent(scrolled)
        && ScrollEventInterpreter.isWebNavigationEvent(interpretation.event())) {
      // A swipe can move focus after this scroll was queued. Neither web recovery nor the
      // native fallback may then pull focus away from the newer navigation.
      LogUtils.d(TAG, "Discard delayed web scroll after newer focus navigation");
      return;
    }

    AccessibilityNodeInfoCompat currentA11yFocusedNode =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    if (!FormFactorUtils.isAndroidWear()
        && startWebFocusRecovery(interpretation, currentA11yFocusedNode, stripScrolled)) {
      return;
    }
    moveFocusFromScrolledOutNode(interpretation, currentA11yFocusedNode);
  }

  /** Moves focus that is no longer focusable after a manual scroll, as TalkBack does. */
  private void moveFocusFromScrolledOutNode(
      ManualScrollInterpretation interpretation,
      @Nullable AccessibilityNodeInfoCompat currentA11yFocusedNode) {
    if (!shouldMoveFocus(currentA11yFocusedNode, interpretation.direction())) {
      return;
    }
    // The scroll was matched to the focus before it was delayed. A swipe since then may have
    // moved focus into another list or document, which an old scroll must not take focus from.
    if (!FormFactorUtils.isAndroidWear()) {
      AccessibilityNodeInfoCompat scrolled =
          AccessibilityEventUtils.sourceCompat(interpretation.event());
      // Before Android 8.1 an event's source has no view ID until it is refreshed, as
      // ManualScrollInterpreter does, and the check needs the ID.
      if (scrolled != null && !BuildVersionUtils.isAtLeastOMR1()) {
        scrolled.refresh();
      }
      NodePathDescription focusPath =
          actorState.getFocusHistory().getLastFocusAncestorIdentityPath();
      if (focusPath == null || !focusPath.containsNodeByHashAndIdentity(scrolled)) {
        return;
      }
    }

    ManualScroll.Builder builder =
        ManualScroll.builder()
            .setDirection(interpretation.direction())
            .setScreenState(screenState.getStableScreenState());
    if (FormFactorUtils.isAndroidWear()) {
      builder.setCurrentFocusedNode(currentA11yFocusedNode);
    }

    pipelineInterpretations.input(interpretation.eventId(), interpretation.event(), builder.build());
  }

  /** How long one run of {@link #webFocusRecovery} may read nodes before other input is handled. */
  private static final long WEB_FOCUS_RECOVERY_SLICE_MS = 16;

  /**
   * How long {@link #webFocusRecovery} may take in all. Past it, focus moves to an item across an
   * edge of the view if one was found, or stays.
   */
  private static final long WEB_FOCUS_RECOVERY_TIME_LIMIT_MS = 500;

  /** The search for an item in view after the user scrolled web content, while it runs. */
  private @Nullable WebFocusRecovery webFocusRecovery;

  /** The scroll that {@link #webFocusRecovery} handles. */
  private @Nullable ManualScrollInterpretation webFocusRecoveryScroll;

  private long webFocusRecoveryStartTime;

  /** The time of the last focus action when {@link #webFocusRecovery} started. */
  private long webFocusRecoveryFocusTime;

  private final Runnable continueWebFocusRecovery =
      () -> runWebFocusRecovery(/* yieldToInput= */ true);

  /**
   * Starts to move accessibility focus that the user scrolled out of view in web content to an item
   * in view. Web content counts as focusable while its WebView is visible, so focus out of view is
   * kept, and the next swipe from it would scroll the page back. Returns whether the scroll is
   * handled here: focus that stays in view, or a search for an item in view, which reads the app's
   * nodes in short runs so that touch input is handled between them.
   *
   * @param stripScrolled whether the page scrolled a strip that holds the focus and scrolls only
   *     sideways, after the user scrolled other content
   */
  private boolean startWebFocusRecovery(
      ManualScrollInterpretation interpretation,
      @Nullable AccessibilityNodeInfoCompat focus,
      boolean stripScrolled) {
    boolean forward = interpretation.direction() != TraversalStrategy.SEARCH_FOCUS_BACKWARD;
    // Chrome clears the focus of an item that leaves the page's node tree as the page scrolls. A
    // WebView or document node in focus fills the view, so it counts as in view, but the next
    // swipe from it goes to the start of the page.
    boolean focusLost = focus == null || Role.getRole(focus) == Role.ROLE_WEB_VIEW;
    if (focusLost) {
      if (ScrollEventInterpreter.isWebNavigationEvent(interpretation.event())) {
        return false;
      }
      AccessibilityNodeInfoCompat scrolled =
          AccessibilityEventUtils.sourceCompat(interpretation.event());
      if (scrolled == null || !WebScrollFocus.isWebContent(scrolled)) {
        return false;
      }
      // The item that had focus may have left the tree, but its parent can still be there to
      // search from.
      FocusActionRecord record = actorState.getFocusHistory().getLastFocusActionRecord();
      AccessibilityNodeInfoCompat lastFocused =
          (record != null && WebInterfaceUtils.supportsWebActions(record.getFocusedNode()))
              ? record.getFocusedNode()
              : null;
      startWebFocusRecovery(
          interpretation,
          new WebFocusRecovery(
              null,
              lastFocused,
              scrolled,
              /* leftBehind= */ true,
              forward,
              stripIn(scrolled),
              /* focusMoved= */ true));
      return true;
    }
    if (!WebInterfaceUtils.supportsWebActions(focus) || !isUserScroll(interpretation)) {
      return false;
    }
    AccessibilityNodeInfoCompat scrolled =
        AccessibilityEventUtils.sourceCompat(interpretation.event());
    if (scrolled == null) {
      return false;
    }
    Rect focusBounds = new Rect();
    focus.getBoundsInScreen(focusBounds);
    Rect scrolledBounds = new Rect();
    scrolled.getBoundsInScreen(scrolledBounds);
    // Chrome reports an item out of view as not visible, also one that a strip in the page
    // scrolled away sideways, whose bounds can still lie in the WebView. Focus left in a strip that
    // the page scrolled to follow the content does not show where the user scrolled to either.
    boolean leftBehind = stripScrolled || !focus.isVisibleToUser();
    // An item that scrolled under a strip fixed across the top of the page, or the header above
    // it, is hidden. Where the strip was at its last scroll, without asking the app.
    AccessibilityNodeInfoCompat strip = stripIn(scrolled);
    boolean focusMoved = focusMovedSinceFocused(focus, focusBounds);
    boolean underStrip = false;
    if (strip != null && focusMoved) {
      Rect stripBounds = new Rect();
      strip.getBoundsInScreen(stripBounds);
      underStrip = WebScrollFocus.isUnderFixedStrip(focusBounds, scrolledBounds, stripBounds);
    }
    // Focus in the scrolled view is in view. Most scrolls end here, without asking the app.
    if (!leftBehind && !underStrip && !WebScrollFocus.isOutside(focusBounds, scrolledBounds)) {
      return true;
    }
    startWebFocusRecovery(
        interpretation,
        new WebFocusRecovery(focus, null, scrolled, leftBehind, forward, strip, focusMoved));
    return true;
  }

  /** Returns {@link #webStrip} if it is in the window of {@code scrolled}. */
  private @Nullable AccessibilityNodeInfoCompat stripIn(AccessibilityNodeInfoCompat scrolled) {
    return (webStrip != null && webStrip.getWindowId() == scrolled.getWindowId())
        ? webStrip
        : null;
  }

  /**
   * Whether {@code focus} lies elsewhere than when it was focused, as the focus record's copy of
   * it shows. An item that the page fixes in place, such as in a header, does not move.
   */
  private boolean focusMovedSinceFocused(AccessibilityNodeInfoCompat focus, Rect focusBounds) {
    FocusActionRecord record = actorState.getFocusHistory().getLastFocusActionRecord();
    if (record == null || !record.getFocusedNode().equals(focus)) {
      return false;
    }
    Rect then = new Rect();
    record.getFocusedNode().getBoundsInScreen(then);
    return !then.equals(focusBounds);
  }

  private void startWebFocusRecovery(
      ManualScrollInterpretation interpretation, WebFocusRecovery recovery) {
    cancelWebFocusRecovery();
    webFocusRecovery = recovery;
    webFocusRecoveryScroll = interpretation;
    webFocusRecoveryStartTime = SystemClock.uptimeMillis();
    webFocusRecoveryFocusTime = lastFocusActionTime();
    runWebFocusRecovery(/* yieldToInput= */ true);
  }

  /**
   * Runs {@link #webFocusRecovery} on. With {@code yieldToInput}, stops after {@link
   * #WEB_FOCUS_RECOVERY_SLICE_MS} and goes on once the events that arrived meanwhile, such as
   * motion events, are handled.
   */
  private void runWebFocusRecovery(boolean yieldToInput) {
    WebFocusRecovery recovery = webFocusRecovery;
    ManualScrollInterpretation interpretation = webFocusRecoveryScroll;
    if (recovery == null || interpretation == null) {
      return;
    }
    handler.removeCallbacks(continueWebFocusRecovery);
    // A swipe or touch exploration moved focus since the search started.
    if (lastFocusActionTime() != webFocusRecoveryFocusTime) {
      cancelWebFocusRecovery();
      return;
    }
    long sliceEnd = SystemClock.uptimeMillis() + WEB_FOCUS_RECOVERY_SLICE_MS;
    while (!recovery.step()) {
      long now = SystemClock.uptimeMillis();
      if (now - webFocusRecoveryStartTime >= WEB_FOCUS_RECOVERY_TIME_LIMIT_MS) {
        LogUtils.w(TAG, "Stopped the search for a web item in view at its time limit");
        recovery.stop();
        break;
      }
      if (yieldToInput && now >= sliceEnd) {
        // Posted behind the motion events and other messages that arrived meanwhile.
        handler.post(continueWebFocusRecovery);
        return;
      }
    }
    webFocusRecovery = null;
    webFocusRecoveryScroll = null;
    if (!screenState.areMainWindowsStable()
        || lastFocusActionTime() != webFocusRecoveryFocusTime) {
      return;
    }
    switch (recovery.outcome()) {
      case WebFocusRecovery.FOUND ->
          pipelineInterpretations.input(
              interpretation.eventId(),
              interpretation.event(),
              ManualScroll.builder()
                  .setDirection(interpretation.direction())
                  .setScreenState(screenState.getStableScreenState())
                  .setWebItemInView(recovery.item())
                  .build());
      case WebFocusRecovery.NOTHING_IN_VIEW -> {
        LogUtils.d(TAG, "Web focus is out of view, and no item is in view");
        AccessibilityNodeInfoCompat focus =
            accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
        // TalkBack's handling would focus the whole WebView in place of lost focus.
        if (focus != null) {
          moveFocusFromScrolledOutNode(interpretation, focus);
        }
      }
      case WebFocusRecovery.NOT_WEB_SCROLL ->
          moveFocusFromScrolledOutNode(
              interpretation,
              accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false));
      default -> {}
    }
  }

  private void cancelWebFocusRecovery() {
    handler.removeCallbacks(continueWebFocusRecovery);
    webFocusRecovery = null;
    webFocusRecoveryScroll = null;
  }

  /**
   * Handles a scroll of web content that waits for scrolling to settle, and finishes the search for
   * an item in view, at once. Called before a gesture runs its command, so that a swipe after a
   * scroll moves on from the content in view.
   */
  public void finishWebScrollFocus() {
    if (FormFactorUtils.isAndroidWear()) {
      return;
    }
    if (pendingManualScroll != null && pendingManualScroll == webScroll) {
      handler.removeCallbacks(handlePendingManualScroll);
      // Posts itself again while the fingers that scroll the page are still down.
      handlePendingManualScroll.run();
    }
    if (webFocusRecovery != null) {
      runWebFocusRecovery(/* yieldToInput= */ false);
    }
  }

  /** {@link #lastFocusActionTime} for telling whether the user scrolled the page. */
  private long focusTimeForUserScroll() {
    FocusActionRecord record = actorState.getFocusHistory().getLastFocusActionRecord();
    return (record == null)
        ? -1
        : WebScrollFocus.focusTimeForUserScroll(
            record.getExtraInfo().sourceAction, record.getActionTime());
  }

  private long lastFocusActionTime() {
    FocusActionRecord record = actorState.getFocusHistory().getLastFocusActionRecord();
    return (record == null) ? -1 : record.getActionTime();
  }

  /**
   * Whether the user's fingers scrolled the page. The page also scrolls itself to show an item that
   * a swipe or continuous reading focused, and that scroll is not one of SandFly's scroll actions.
   */
  private boolean isUserScroll(ManualScrollInterpretation interpretation) {
    return isUserScroll(interpretation.event());
  }

  private boolean isUserScroll(AccessibilityEvent event) {
    return WebScrollFocus.isUserScroll(
        event.getEventTime(),
        touchMonitor.getTouchStartTime(),
        touchMonitor.getTouchEndTime(),
        touchMonitor.isUserTouchingScreen(),
        touchMonitor.getPreviousTouchStartTime(),
        touchMonitor.getPreviousTouchEndTime(),
        focusTimeForUserScroll());
  }

  private boolean shouldMoveFocus(
      AccessibilityNodeInfoCompat currentA11yFocusedNode, @SearchDirection int direction) {
    if (FormFactorUtils.isAndroidWear()) {
      // In a Watch device, especially for a rounded screen, we have to move focus onto the next
      // node before current focused node being invisible to mitigate the fighting between TB and
      // a scrolling list view. To do so, we need to focus onto another node when the current
      // focused node is close to borders and there is enough space for the new focused node.
      return shouldFocusNextNodeForWatch(context, currentA11yFocusedNode, direction);
    } else {
      return !AccessibilityNodeInfoUtils.shouldFocusNode(currentA11yFocusedNode);
    }
  }

  private static final float PARTIAL_INVISIBLE_TOP_RATIO = 0.15f;
  private static final float PARTIAL_INVISIBLE_MID_RATIO = 0.5f;
  private static final float PARTIAL_INVISIBLE_BOTTOM_RATIO = 0.85f;

  private boolean shouldFocusNextNodeForWatch(
      Context context, AccessibilityNodeInfoCompat node, @SearchDirection int direction) {
    if (!FormFactorUtils.isAndroidWear()) {
      return false;
    }

    if (node == null) {
      return true;
    }

    Rect nodeRect = new Rect();
    node.getBoundsInScreen(nodeRect);

    Point screenPxSize = DisplayUtils.getScreenPixelSizeWithoutWindowDecor(context);
    // When we scroll up the list, the screen will go down and the top content will be disappeared,
    // and vice versa for scrolling down. Therefore, when we scroll up the list by gesture, we need
    // to search focus forward.
    return closeToBorder(direction, nodeRect, screenPxSize)
        && hasEnoughSpaceForNextNode(direction, nodeRect, screenPxSize);
  }

  private boolean closeToBorder(@SearchDirection int direction, Rect nodeRect, Point screenPxSize) {
    if (direction == TraversalStrategy.SEARCH_FOCUS_FORWARD) {
      return (float) nodeRect.top < (float) screenPxSize.y * PARTIAL_INVISIBLE_TOP_RATIO;
    } else if (direction == TraversalStrategy.SEARCH_FOCUS_BACKWARD) {
      return (float) nodeRect.bottom > (float) screenPxSize.y * PARTIAL_INVISIBLE_BOTTOM_RATIO;
    } else {
      return false;
    }
  }

  private boolean hasEnoughSpaceForNextNode(
      @SearchDirection int direction, Rect nodeRect, Point screenPxSize) {
    if (direction == TraversalStrategy.SEARCH_FOCUS_FORWARD) {
      return (float) nodeRect.bottom < (float) screenPxSize.y * PARTIAL_INVISIBLE_MID_RATIO;
    } else if (direction == TraversalStrategy.SEARCH_FOCUS_BACKWARD) {
      return (float) nodeRect.top > (float) screenPxSize.y * PARTIAL_INVISIBLE_MID_RATIO;
    } else {
      return false;
    }
  }

  /** Event-interpreter function, called by {@link InputFocusInterpreter}. */
  @Override
  public void onViewTargeted(
      @Nullable EventId eventId,
      AccessibilityEvent event,
      AccessibilityNodeInfoCompat targetedNode) {
    if (!screenState.areMainWindowsStable()) {
      LogUtils.w(
          TAG,
          "onViewTargeted return due to windows are not stable and the focus "
              + "will be handled by onScreenStateChanged after main windows are stable.");
      return;
    }
    pipelineInterpretations.input(eventId, event, new Interpretation.InputFocus(targetedNode));
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // APIs used by other TalkBack components

  /** Called by EventFilter. */
  @Override
  public @Nullable AccessibilityFocusEventInterpretation interpret(AccessibilityEvent event) {
    FocusActionInfo info = actorState.getFocusHistory().getFocusActionInfoFromEvent(event);
    boolean forceMuteFeedback = info != null && info.forceMuteFeedback;

    // For user interface interaction (such as quick menu to handle slider/number-picker) and image
    // caption.
    if (event.getEventType() == TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
      AccessibilityNodeInfoCompat node = AccessibilityNodeInfoUtils.toCompat(event.getSource());
      // Skips caption if the view has already been labeled.
      boolean needsCaption =
          !forceMuteFeedback
              && ImageCaptioner.supportsImageCaption(context)
              && ImageCaptioner.needAutomaticCaptioning(
                  context, node, actorState.getGeminiState().hasAiCore())
              && actorState.getLabelManagerState().getLabelIdForNode(node) == Label.NO_ID;
      pipelineInterpretations.input(
          Performance.getInstance().onEventReceived(event),
          event,
          Interpretation.AccessibilityFocused.create(info, needsCaption),
          node);
    }

    // Focus that a web page moved, when asked to go to the next or previous item, is compared
    // with where it was here.
    int webColumnWrap =
        event.getEventType() == TYPE_VIEW_ACCESSIBILITY_FOCUSED
            ? ColumnWrap.takeWebFocus(event)
            : ColumnWrap.NONE;

    if (info == null) {
      if (webColumnWrap == ColumnWrap.NONE) {
        return null;
      }
      AccessibilityFocusEventInterpretation interpretation =
          new AccessibilityFocusEventInterpretation(Compositor.toCompositorEvent(event));
      interpretation.setColumnWrap(webColumnWrap);
      return interpretation;
    }

    AccessibilityFocusEventInterpretation interpretation =
        new AccessibilityFocusEventInterpretation(Compositor.toCompositorEvent(event));
    interpretation.setColumnWrap(
        info.columnWrap != ColumnWrap.NONE ? info.columnWrap : webColumnWrap);
    interpretation.setForceFeedbackEvenIfAudioPlaybackActive(
        info.forceFeedbackEvenIfAudioPlaybackActive());
    interpretation.setForceFeedbackEvenIfMicrophoneActive(
        info.forceFeedbackEvenIfMicrophoneActive());
    interpretation.setForceFeedbackEvenIfSsbActive(info.forceFeedbackEvenIfSsbActive());
    interpretation.setShouldMuteFeedback(info.forceMuteFeedback);
    interpretation.setIsInitialFocusAfterScreenStateChange(
        info.sourceAction == FocusActionInfo.SCREEN_STATE_CHANGE);
    interpretation.setIsNavigateByUser(
        info.sourceAction == FocusActionInfo.TOUCH_EXPLORATION
            || info.sourceAction == FocusActionInfo.LOGICAL_NAVIGATION);
    interpretation.setIsTouchExploration(info.sourceAction == FocusActionInfo.TOUCH_EXPLORATION);
    return interpretation;
  }

  /**
   * Stops speech before an item that touch exploration focused is spoken, as touching the screen
   * does, under the same setting. An app's announcement that arrives between the touch reaching the
   * item and its focus event, such as a live region updated by the previous touch, starts after the
   * touch stopped speech. It is spoken so that new speech can't interrupt it, and the item would
   * wait for it. A keyboard key is spoken from its hover event, just before its focus event, and
   * that speech is left playing.
   */
  @Override
  public void beforeFocusSpoken(AccessibilityFocusEventInterpretation interpretation) {
    if (interpretation.getIsTouchExploration()
        && !interpretation.isEqualsToLastHoverEnterKeyboardEventNode()
        && actorState != null
        && actorState.getSpeechState().isSpeaking()) {
      pipelineInterpretations.input(
          EVENT_ID_UNTRACKED,
          /* event= */ null,
          Interpretation.Touch.create(Interpretation.Touch.Action.TOUCH_START));
    }
  }

  /**
   * Sets whether single-tap activation is enabled.
   *
   * @param enabled Whether single-tap activation is enabled.
   */
  public void setSingleTapEnabled(boolean enabled) {
    focusProcessorForTapAndTouchExploration.setSingleTapEnabled(enabled);
  }

  /**
   * Gets whether single-tap activation is enabled.
   *
   * @return Whether single-tap activation is enabled.
   */
  public boolean getSingleTapEnabled() {
    return focusProcessorForTapAndTouchExploration.getSingleTapEnabled();
  }

  /**
   * Sets type confirmation method
   *
   * @param type keyboard confirmation type
   */
  public void setTypingMethod(@TypingMethod int type) {
    focusProcessorForTapAndTouchExploration.setTypingMethod(type);
  }

  /**
   * Sets long press duration. It's only applicable when the typing method is not double-tap.
   *
   * @param duration in milliseconds
   */
  public void setTypingLongPressDurationMs(int duration) {
    focusProcessorForTapAndTouchExploration.setTypingLongPressDurationMs(duration);
  }

  /**
   * Gets whether single-tap activation is enabled.
   *
   * @return Whether single-tap activation is enabled.
   */
  @TypingMethod
  public int getTypingMethod() {
    return focusProcessorForTapAndTouchExploration.getTypingMethod();
  }

  /** Types the key that the touch interaction in progress taps, without focusing it first. */
  public void onTapToType() {
    focusProcessorForTapAndTouchExploration.onTapToType();
  }

  /** Sets whether the finger is at the edge of the screen, where lifting it activates nothing. */
  public void setFingerAtScreenEdge(boolean atEdge) {
    focusProcessorForTapAndTouchExploration.setFingerAtScreenEdge(atEdge);
  }

  /** Forgets the item touched last, so that lifting the finger now activates nothing. */
  public void forgetTouchedNode() {
    focusProcessorForTapAndTouchExploration.forgetTouchedNode();
  }

  /** Notes that the finger left the strip at the bottom of the screen at {@code timeMs}. */
  public void onFingerLeftScreenEdge(long timeMs) {
    focusProcessorForTapAndTouchExploration.onFingerLeftScreenEdge(timeMs);
  }

  /** Sets how long the finger stays on an item for lifting it to activate nothing; 0 for never. */
  public void setHoldToCancelMs(long ms) {
    focusProcessorForTapAndTouchExploration.setHoldToCancelMs(ms);
  }

  /** Sets whether a tap that lifts before the item under it is focused activates the item. */
  public void setTapToActivateEnabled(boolean enabled) {
    focusProcessorForTapAndTouchExploration.setTapToActivateEnabled(enabled);
  }

  /** Returns whether a quick tap now activates the item under it as the finger lifts. */
  public boolean quickTapActivatesAtOnce() {
    return focusProcessorForTapAndTouchExploration.quickTapActivatesAtOnce();
  }

  /** Called when touch exploration starts after the finger of the touch lifted: a tap. */
  public void onTapExploration() {
    focusProcessorForTapAndTouchExploration.onTapExploration();
  }

  /** Sets what plays when holding or the edge of the screen cancels an activation. */
  public void setLiftCancelledFeedback(@Nullable Runnable feedback) {
    focusProcessorForTapAndTouchExploration.setLiftCancelledFeedback(feedback);
  }

  /** Returns whether lifting the finger now activates the item or types the key it last touched. */
  public boolean liftActivates() {
    return focusProcessorForTapAndTouchExploration.liftActivates();
  }
}
