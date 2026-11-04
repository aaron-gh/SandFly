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

import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED;
import static com.google.android.accessibility.talkback.Interpretation.Touch.Action.LIFT;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
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
import java.util.ArrayList;
import java.util.List;
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
  private @Nullable ManualScrollInterpretation pendingManualScroll;
  private final Runnable handlePendingManualScroll =
      () -> {
        ManualScrollInterpretation interpretation = pendingManualScroll;
        pendingManualScroll = null;
        if (interpretation != null) {
          handleManualScroll(interpretation);
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
      handleManualScroll(interpretation);
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
    handler.removeCallbacks(handlePendingManualScroll);
    handler.postDelayed(handlePendingManualScroll, MANUAL_SCROLL_SETTLE_MS);
  }

  private void handleManualScroll(ManualScrollInterpretation interpretation) {
    if (!screenState.areMainWindowsStable()) {
      LogUtils.w(
          TAG,
          "onScrollEvent return due to windows are not stable and the focus will"
              + " be handled by onScreenStateChanged after main windows are stable.");
      return;
    }

    AccessibilityNodeInfoCompat currentA11yFocusedNode =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    if (shouldMoveFocus(currentA11yFocusedNode, interpretation.direction())) {
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

      pipelineInterpretations.input(
          interpretation.eventId(), interpretation.event(), builder.build());
    } else if (!FormFactorUtils.isAndroidWear()) {
      moveWebFocusIntoView(interpretation, currentA11yFocusedNode);
    }
  }

  /** Most ancestors of the focused node read to find its WebView. */
  private static final int MAX_ANCESTORS_READ = 128;

  /**
   * Moves accessibility focus that the user scrolled out of view in web content to an item in view.
   * Web content counts as focusable while its WebView is visible, so focus out of view is kept, and
   * the next swipe from it would scroll the page back.
   */
  private void moveWebFocusIntoView(
      ManualScrollInterpretation interpretation, @Nullable AccessibilityNodeInfoCompat focus) {
    if (focus == null
        || !WebInterfaceUtils.supportsWebActions(focus)
        || !isUserScroll(interpretation)) {
      return;
    }
    AccessibilityNodeInfoCompat scrolled =
        AccessibilityEventUtils.sourceCompat(interpretation.event());
    if (scrolled == null) {
      return;
    }
    Rect focusBounds = new Rect();
    focus.getBoundsInScreen(focusBounds);
    Rect scrolledBounds = new Rect();
    scrolled.getBoundsInScreen(scrolledBounds);
    // Focus in the scrolled view is in view. Most scrolls end here, without asking the app.
    if (!WebScrollFocus.isOutside(focusBounds, scrolledBounds)) {
      return;
    }

    // The focused node and its ancestors, the WebView that holds it, and whether the scrolled view
    // is one of them.
    List<AccessibilityNodeInfoCompat> focusAndAncestors = new ArrayList<>();
    AccessibilityNodeInfoCompat webView = null;
    boolean scrolledHoldsFocus = false;
    AccessibilityNodeInfoCompat node = focus;
    while (node != null
        && focusAndAncestors.size() < MAX_ANCESTORS_READ
        && !focusAndAncestors.contains(node)) {
      focusAndAncestors.add(node);
      scrolledHoldsFocus |= node.equals(scrolled);
      AccessibilityNodeInfoCompat parent = node.getParent();
      if (webView == null
          && Role.getRole(node) == Role.ROLE_WEB_VIEW
          && Role.getRole(parent) != Role.ROLE_WEB_VIEW) {
        webView = node;
      }
      node = parent;
    }
    if (webView == null) {
      return;
    }
    Rect viewport = new Rect();
    webView.getBoundsInScreen(viewport);
    if (viewport.isEmpty()) {
      return;
    }
    if (scrolledHoldsFocus) {
      // Focus can also scroll out of a list or frame in the page.
      if (!scrolledBounds.isEmpty() && !viewport.intersect(scrolledBounds)) {
        return;
      }
    } else if (!WebScrollFocus.isWebContent(scrolled)
        || !(scrolled.equals(webView)
            || AccessibilityNodeInfoUtils.hasAncestor(scrolled, webView))) {
      // The scroll is outside the WebView that holds the focus.
      return;
    }
    if (!WebScrollFocus.isOutside(focusBounds, viewport)) {
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
      return;
    }
    AccessibilityNodeInfoCompat item =
        WebScrollFocus.findItemInView(
            searchRoot,
            focusAndAncestors,
            viewport,
            interpretation.direction() != TraversalStrategy.SEARCH_FOCUS_BACKWARD);
    if (item == null) {
      LogUtils.d(TAG, "Web focus is out of view, and no item is in view");
      return;
    }
    pipelineInterpretations.input(
        interpretation.eventId(),
        interpretation.event(),
        ManualScroll.builder()
            .setDirection(interpretation.direction())
            .setScreenState(screenState.getStableScreenState())
            .setWebItemInView(item)
            .build());
  }

  /**
   * Whether the user's fingers scrolled the page. The page also scrolls itself to show an item that
   * a swipe or continuous reading focused, and that scroll is not one of SandFly's scroll actions.
   */
  private boolean isUserScroll(ManualScrollInterpretation interpretation) {
    FocusActionRecord record = actorState.getFocusHistory().getLastFocusActionRecord();
    return WebScrollFocus.isUserScroll(
        interpretation.event().getEventTime(),
        touchMonitor.getTouchStartTime(),
        touchMonitor.getTouchEndTime(),
        touchMonitor.isUserTouchingScreen(),
        (record == null) ? -1 : record.getActionTime());
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
    return interpretation;
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

  /** Returns whether lifting the finger now activates the item or types the key it last touched. */
  public boolean liftActivates() {
    return focusProcessorForTapAndTouchExploration.liftActivates();
  }
}
