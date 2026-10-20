/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.google.android.accessibility.talkback;

import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOWS_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
import static android.view.accessibility.AccessibilityEvent.WINDOWS_CHANGE_ADDED;
import static android.view.accessibility.AccessibilityEvent.WINDOWS_CHANGE_REMOVED;
import static android.view.accessibility.AccessibilityEvent.WINDOWS_CHANGE_TITLE;
import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD;
import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD;
import static androidx.core.view.accessibility.AccessibilityEventCompat.CONTENT_CHANGE_TYPE_PANE_APPEARED;
import static androidx.core.view.accessibility.AccessibilityEventCompat.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED;
import static com.google.android.accessibility.utils.Performance.MOTION_EVENT_DIRECTION_BACKWARD;
import static com.google.android.accessibility.utils.Performance.MOTION_EVENT_DIRECTION_FORWARD;
import static com.google.android.accessibility.utils.Performance.MOTION_EVENT_DIRECTION_UNDEFINED;
import static com.google.android.accessibility.utils.input.CursorGranularity.DEFAULT;
import static com.google.android.accessibility.utils.monitor.InputModeTracker.INPUT_MODE_TOUCH;
import static com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_BACKWARD;
import static com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_FORWARD;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.core.view.ViewConfigurationCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.Pipeline.FeedbackReturner;
import com.google.android.accessibility.utils.AccessibilityEventListener;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.DisplayUtils;
import com.google.android.accessibility.utils.Filter;
import com.google.android.accessibility.utils.Logger;
import com.google.android.accessibility.utils.Performance;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.Role;
import com.google.android.accessibility.utils.Role.RoleName;
import com.google.android.accessibility.utils.WeakReferenceHandler;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.time.Duration;

/**
 * A controller to decide whether to intercept rotary encoder events and handle it if needed.
 *
 * <p>The methods annotated as {@code @WorkerThread} should be run in the {@link
 * MotionEventController#backgroundHandler}.
 */
@RequiresApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
public class MotionEventController implements AccessibilityEventListener {

  private static final String TAG = "MotionEventController";

  private static final int TYPE_WINDOWS_CHANGE_FOR_RESET =
      WINDOWS_CHANGE_ADDED | WINDOWS_CHANGE_REMOVED | WINDOWS_CHANGE_TITLE;

  private static final int TYPE_WINDOW_STATE_CHANGE_FOR_RESET =
      CONTENT_CHANGE_TYPE_PANE_APPEARED | CONTENT_CHANGE_TYPE_PANE_DISAPPEARED;

  private static final int THROTTLE_MS = 50;
  // This is an experience value which is approximately equal to a default height for a
  // WearChipButton.
  private static final int THRESHOLD_SCROLL_DP = 52;

  private final int thresholdScrollPx;
  private final float scaledVerticalScrollFactor;

  private final AccessibilityService service;
  private final FeedbackReturner feedbackReturner;

  private final MotionEventHandler backgroundHandler;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private final Runnable disableInterceptRotaryEncoderRunnable =
      () -> interceptRotaryEncoderInternal(/* shouldHandleMotionEvent= */ false);
  private final Runnable recheckInterceptRotaryEncoderRunnable =
      () -> {
        LogUtils.v(TAG, "Recheck whether we should intercept rotary encoder again");
        interceptRotaryEncoderIfNeeded();
      };
  private static final Duration RECHECK_DELAY_DURATION = Duration.ofSeconds(1);

  private boolean isHandlingMotionEvent;

  private float axisSum = 0;

  // We update these current nodes from TYPE_VIEW_ACCESSIBILITY_FOCUSED and TYPE_VIEW_SCROLLED.
  // Besides, we will make sure that the currentScrollableNodeForFocus is the descendant of the
  // currentFocusedNodeInScrollable.
  private AccessibilityNodeInfoCompat currentScrollableNodeForFocus;
  private boolean isReachToEdgeScrolledEvent = false;
  private AccessibilityNodeInfoCompat currentFocusedNodeInScrollable;
  // We should intercept RSB for the out of scrollable a11y focused node when the input-focused node
  // is a scrollable node.
  private boolean isInterceptForNodeOutOfScrollableNode;
  // These 2 variables are used for dump logger.
  private boolean useForLogIsFocusedNodeOutOfScrollableContainer;
  private AccessibilityNodeInfoCompat inputFocusedNode;

  public MotionEventController(AccessibilityService service, FeedbackReturner feedbackReturner) {
    this.service = service;
    this.feedbackReturner = feedbackReturner;

    scaledVerticalScrollFactor =
        ViewConfigurationCompat.getScaledVerticalScrollFactor(
            ViewConfiguration.get(service), service);
    thresholdScrollPx = DisplayUtils.dpToPx(service, THRESHOLD_SCROLL_DP);

    AccessibilityServiceInfo info = service.getServiceInfo();
    if (info != null) {
      isHandlingMotionEvent =
          (info.getMotionEventSources() & InputDevice.SOURCE_ROTARY_ENCODER) != 0;
    } else {
      LogUtils.w(TAG, "AccessibilityServiceInfo is null while initializing isHandlingMotionEvent.");
    }

    HandlerThread handlerThread = new HandlerThread(TAG);
    handlerThread.start();
    backgroundHandler = new MotionEventHandler(this, handlerThread.getLooper());
  }

  @Override
  public int getEventTypes() {
    return TYPE_VIEW_ACCESSIBILITY_FOCUSED
        | TYPE_VIEW_SCROLLED
        | TYPE_WINDOWS_CHANGED
        | TYPE_WINDOW_STATE_CHANGED;
  }

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event, EventId eventId) {
    backgroundHandler.post(
        () -> {
          updateCurrentNodes(event);
          interceptRotaryEncoderIfNeeded();
        });
  }

  public void onMotionEvent(@NonNull MotionEvent event) {
    if (event.getSource() != InputDevice.SOURCE_ROTARY_ENCODER) {
      // Currently, skipping other sources, we only handle SOURCE_ROTARY_ENCODER in this class.
      return;
    }

    float axisValue = event.getAxisValue(MotionEvent.AXIS_SCROLL);
    backgroundHandler.post(() -> updateAxisSum(axisValue));
    backgroundHandler.postDelayPerformActionIfEmptyMessage();
  }

  public void shutdown() {
    LogUtils.v(TAG, "shutdown: MotionEventController is going to shutdown.");
    backgroundHandler.getLooper().quit();
  }

  @WorkerThread
  private void updateAxisSum(float axisValue) {
    float scrollPx = axisValue * scaledVerticalScrollFactor;
    if (axisSum * scrollPx > 0) {
      axisSum += scrollPx;
    } else {
      // We reset axisSum as the 1st opposite direction value if a users scroll in a different
      // direction to the previous scrolling.
      axisSum = scrollPx;
    }
  }

  @WorkerThread
  private void performActionIfNeeded(EventId eventId) {
    Feedback.FocusDirection.Builder feedback = null;
    int direction = MOTION_EVENT_DIRECTION_UNDEFINED;

    if (axisSum > thresholdScrollPx) {
      feedback = Feedback.focusDirection(SEARCH_FOCUS_BACKWARD);
      direction = MOTION_EVENT_DIRECTION_BACKWARD;

      axisSum -= thresholdScrollPx;
    } else if (axisSum < -thresholdScrollPx) {
      feedback = Feedback.focusDirection(SEARCH_FOCUS_FORWARD);
      direction = MOTION_EVENT_DIRECTION_FORWARD;

      axisSum += thresholdScrollPx;
    }

    if (direction == MOTION_EVENT_DIRECTION_UNDEFINED) {
      return;
    }

    // Sets granularity to default because previous/next action always moves at default granularity.
    feedback
        .setGranularity(DEFAULT)
        .setInputMode(INPUT_MODE_TOUCH)
        .setDefaultToInputFocus(true)
        .setScroll(true)
        .setWrap(false);

    final Feedback.FocusDirection.Builder currentFeedback = feedback;

    mainHandler.post(
        () -> {
          boolean success = feedbackReturner.returnFeedback(eventId, currentFeedback);
          if (!success) {
            // If we reach an edge, we don't need to intercept motion events and we can just
            // fallback to original scrolling. It is helpful to avoid missing animation like the
            // dismiss of quick settings and notification tray by scrolling RSB.
            LogUtils.v(TAG, "performActionIfNeeded: returnFeedback failed");
            backgroundHandler.post(disableInterceptRotaryEncoderRunnable);
            backgroundHandler.postDelayed(
                recheckInterceptRotaryEncoderRunnable, RECHECK_DELAY_DURATION.toMillis());
          }
        });
  }

  @WorkerThread
  private void updateCurrentNodes(AccessibilityEvent event) {
    LogUtils.v(TAG, "updateCurrentNodes on event: " + event);
    int eventType = event.getEventType();
    switch (eventType) {
      case TYPE_VIEW_ACCESSIBILITY_FOCUSED -> updateFocusedNode(event);
      case TYPE_VIEW_SCROLLED -> updateScrollableNode(event);
      case TYPE_WINDOWS_CHANGED -> {
        if ((event.getWindowChanges() & TYPE_WINDOWS_CHANGE_FOR_RESET) != 0) {
          resetCurrentNodes();
        }
      }
      case TYPE_WINDOW_STATE_CHANGED -> {
        if ((event.getContentChangeTypes() & TYPE_WINDOW_STATE_CHANGE_FOR_RESET) != 0) {
          resetCurrentNodes();
        }
      }
      default ->
          LogUtils.w(TAG, "updateCurrentNodes: Shouldn't process this event. (event=%s)", event);
    }
  }

  private void resetCurrentNodes() {
    currentScrollableNodeForFocus = null;
    clearCurrentFocusedNodeInScrollable();
    axisSum = 0;
  }

  private void updateFocusedNode(AccessibilityEvent event) {
    AccessibilityNodeInfoCompat focusedNode =
        AccessibilityNodeInfoUtils.toCompat(event.getSource());

    AccessibilityNodeInfoCompat focusedNodeContainer = getScrollableRoleContainer(focusedNode);
    boolean focusedNodeInScrollableContainer = focusedNodeContainer != null;

    isInterceptForNodeOutOfScrollableNode =
        shouldInterceptForNodeOutOfScrollableNode(focusedNode, focusedNodeContainer);

    if (focusedNodeInScrollableContainer) {
      currentFocusedNodeInScrollable = focusedNode;
      if (currentScrollableNodeForFocus == null && focusedNode != null) {
        // There are 2 situations that currentScrollableNodeForFocus is null so we use
        // focusedNodeContainer as a backup suggestion.
        //
        // 1. After we enter a page, we might haven't received TYPE_VIEW_SCROLLED and the
        // currentScrollableNodeForFocus is null. So, we set it from the focused node's ancestor if
        // the ancestor is a scrollable container.
        // 2. The last source node from TYPE_VIEW_SCROLLED is not the ancestor of the that time
        // focused node.
        updateCurrentScrollableNodeForFocus(focusedNodeContainer, /* scrolledEvent= */ null);
      }
    } else {
      clearCurrentFocusedNodeInScrollable();
    }
  }

  private void updateCurrentScrollableNodeForFocus(
      AccessibilityNodeInfoCompat currentScrollableNodeForFocus, AccessibilityEvent scrolledEvent) {
    this.currentScrollableNodeForFocus = currentScrollableNodeForFocus;
    isReachToEdgeScrolledEvent = calculateReachToVerticalEdgeEvent(scrolledEvent);
  }

  private boolean calculateReachToVerticalEdgeEvent(AccessibilityEvent scrolledEvent) {
    if (scrolledEvent == null) {
      return false;
    }

    int maxScrollY = scrolledEvent.getMaxScrollY();
    int scrollY = scrolledEvent.getScrollY();
    LogUtils.v(
        TAG, "calculateReachToVerticalEdgeEvent: scrollY=%d, maxScrollY=%d", scrollY, maxScrollY);

    if (maxScrollY == 0) {
      LogUtils.w(TAG, "invalid maxScrollY");
      return false;
    }

    return scrollY == 0 || scrollY == maxScrollY;
  }

  private void clearCurrentFocusedNodeInScrollable() {
    currentFocusedNodeInScrollable = null;
    isReachToEdgeScrolledEvent = false;
  }

  // Returns true when the a11y focused node is out of a scrollable container and the input-focused
  // node is scrollable node.
  private boolean shouldInterceptForNodeOutOfScrollableNode(
      @Nullable AccessibilityNodeInfoCompat focusedNode,
      @Nullable AccessibilityNodeInfoCompat focusedNodeContainer) {

    boolean isFocusedNodeOutOfScrollableContainer =
        AccessibilityNodeInfoUtils.isVisible(focusedNode) && focusedNodeContainer == null;

    AccessibilityNodeInfoCompat inputFocusedNode =
        AccessibilityNodeInfoUtils.toCompat(service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT));
    LogUtils.v(
        TAG,
        "shouldInterceptForNodeOutOfScrollableNode: focusedNode=%s, focusedNodeContainer=%s,"
            + " isFocusedNodeOutOfScrollableContainer=%b, inputFocusedNode=%s",
        focusedNode,
        focusedNodeContainer,
        isFocusedNodeOutOfScrollableContainer,
        inputFocusedNode);
    useForLogIsFocusedNodeOutOfScrollableContainer = isFocusedNodeOutOfScrollableContainer;
    this.inputFocusedNode = inputFocusedNode;
    return isFocusedNodeOutOfScrollableContainer
        && (inputFocusedNode == null || isScrollableRoleContainer(inputFocusedNode));
  }

  private void updateScrollableNode(AccessibilityEvent event) {
    AccessibilityNodeInfoCompat scrollableNode =
        AccessibilityNodeInfoUtils.toCompat(event.getSource());

    if (AccessibilityNodeInfoUtils.hasDescendant(scrollableNode, currentFocusedNodeInScrollable)) {
      updateCurrentScrollableNodeForFocus(scrollableNode, event);
    } else {
      currentScrollableNodeForFocus = null;
    }
  }

  @Nullable
  private AccessibilityNodeInfoCompat getScrollableRoleContainer(AccessibilityNodeInfoCompat node) {
    if (!AccessibilityNodeInfoUtils.isVisible(node)) {
      LogUtils.v(TAG, "getScrollableContainer: node is invisible");
      return null;
    }

    return AccessibilityNodeInfoUtils.getMatchingAncestor(
        node,
        new Filter<>() {
          @Override
          public boolean accept(AccessibilityNodeInfoCompat ancestor) {
            if (ancestor == null) {
              return false;
            }

            return isScrollableRoleContainer(ancestor);
          }
        });
  }

  private boolean isScrollableRoleContainer(@Nullable AccessibilityNodeInfoCompat node) {
    if (node == null) {
      return false;
    }
    @RoleName int role = Role.getRole(node);
    return role == Role.ROLE_LIST
        || role == Role.ROLE_GRID
        || role == Role.ROLE_SCROLL_VIEW
        || node.getCollectionInfo() != null;
  }

  @WorkerThread
  private void interceptRotaryEncoderIfNeeded() {
    if (service == null) {
      LogUtils.w(
          TAG,
          "interceptRotaryEncoderIfNeeded: AccessibilityService is null while checking condition.");
      return;
    }

    boolean shouldHandleMotionEvent = shouldHandleMotionEvent();
    LogUtils.d(
        TAG,
        "interceptRotaryEncoderIfNeeded: shouldHandleMotionEvent=%b, isHandlingMotionEvent=%b",
        shouldHandleMotionEvent,
        isHandlingMotionEvent);
    if (isHandlingMotionEvent == shouldHandleMotionEvent) {
      return;
    }
    LogUtils.i(
        TAG,
        "interceptRotaryEncoderIfNeeded: change to shouldHandleMotionEvent=%b",
        shouldHandleMotionEvent);

    interceptRotaryEncoderInternal(shouldHandleMotionEvent);
  }

  @WorkerThread
  private void interceptRotaryEncoderInternal(boolean shouldHandleMotionEvent) {
    if (isHandlingMotionEvent == shouldHandleMotionEvent) {
      return;
    }

    AccessibilityServiceInfo info = service.getServiceInfo();
    if (info == null) {
      LogUtils.w(
          TAG,
          "interceptRotaryEncoderInternal: AccessibilityServiceInfo is null while checking"
              + " condition.");
      return;
    }

    if (shouldHandleMotionEvent) {
      info.setMotionEventSources(InputDevice.SOURCE_ROTARY_ENCODER);
      axisSum = 0;
    } else {
      info.setMotionEventSources(/* motionEventSources= */ 0);
    }

    service.setServiceInfo(info);
    isHandlingMotionEvent = shouldHandleMotionEvent;
  }

  // Make it visible for testing since we cannot mock AccessibilityServiceInfo in Robolectric test.
  @VisibleForTesting
  boolean shouldHandleMotionEvent() {

    LogUtils.i(
        TAG,
        """
        shouldHandleMotionEvent: isInterceptForNodeOutOfScrollableNode=%b
        currentFocusedNodeInScrollable=%s
        currentScrollableNodeForFocus=%s\
        """,
        isInterceptForNodeOutOfScrollableNode,
        currentFocusedNodeInScrollable,
        currentScrollableNodeForFocus);

    if (isInterceptForNodeOutOfScrollableNode) {
      // We should handle it when the a11y focused node is out of any scrollable container and the
      // input-focused node is a scrollable node, so intercepting it make users can traverse all
      // node on the screen by RSB.
      return true;
    }

    return currentFocusedNodeInScrollable != null && isUnsupportedDoubleSideScrollable();
  }

  private boolean isUnsupportedDoubleSideScrollable() {
    AccessibilityNodeInfoCompat scrollableNode = currentScrollableNodeForFocus;
    return scrollableNode != null
        // We only intercept MotionEvent from the scrollable which has one of these conditions.
        // 1.) It has input focus.
        // The reason is that we don't want to intercept MotionEvent from the scrollable which is
        // not the input-focused node. For example, the scrollable is a scrollable list and the
        // input-focused node is a button in the list. In this case, we don't want to intercept
        // MotionEvent from the scrollable list because users can use some input methods, like RSB,
        // to control the button.
        // 2.) It is from the TYPE_VIEW_SCROLLED events.
        // The reason is the we already observe that the scrollable node has been scrolled. It has
        // a large change that users are using RSB to scroll it. Otherwise, it violates Wear policy.
        && (scrollableNode.isFocused()
            || isReachToEdgeAndScrollableNodeRelevantToFocusedNode(scrollableNode))
        && (!AccessibilityNodeInfoUtils.supportsAction(
                scrollableNode, ACTION_SCROLL_BACKWARD.getId())
            || !AccessibilityNodeInfoUtils.supportsAction(
                scrollableNode, ACTION_SCROLL_FORWARD.getId()));
  }

  private boolean isReachToEdgeAndScrollableNodeRelevantToFocusedNode(
      AccessibilityNodeInfoCompat scrollableNode) {
    return isReachToEdgeScrolledEvent
        && (AccessibilityNodeInfoUtils.hasDescendant(scrollableNode, inputFocusedNode)
            || AccessibilityNodeInfoUtils.hasAncestor(scrollableNode, inputFocusedNode));
  }

  @VisibleForTesting
  Looper getBackgroundLooper() {
    return backgroundHandler.getLooper();
  }

  @VisibleForTesting
  void setInputFocusedNode(AccessibilityNodeInfoCompat inputFocusedNode) {
    this.inputFocusedNode = inputFocusedNode;
  }

  public void dump(Logger dumpLogger) {
    dumpLogger.log(
        """
        TalkBackService MotionEventController's state:
        shouldHandleMotionEvent=%b
          isInterceptForNodeOutOfScrollableNode=%b
            isFocusedNodeOutOfScrollableContainer=%s
            inputFocusedNode=%s
          isUnsupportedDoubleSideScrollable=%b
            currentFocusedNodeInScrollable=%s
            currentScrollableNodeForFocus=%s
            isReachToEdgeScrolledEvent=%b
        """,
        shouldHandleMotionEvent(),
        isInterceptForNodeOutOfScrollableNode,
        useForLogIsFocusedNodeOutOfScrollableContainer,
        inputFocusedNode,
        isUnsupportedDoubleSideScrollable(),
        currentFocusedNodeInScrollable,
        currentScrollableNodeForFocus,
        isReachToEdgeScrolledEvent);
  }

  private static final class MotionEventHandler
      extends WeakReferenceHandler<MotionEventController> {

    static final int WHAT_PERFORM_ACTION = 1;

    public MotionEventHandler(MotionEventController controller, Looper looper) {
      super(controller, looper);
    }

    @Override
    protected void handleMessage(Message msg, MotionEventController parent) {
      parent.performActionIfNeeded((EventId) msg.obj);
    }

    public void postDelayPerformActionIfEmptyMessage() {
      if (!hasMessages(WHAT_PERFORM_ACTION)) {
        Message message =
            obtainMessage(
                /* what= */ WHAT_PERFORM_ACTION,
                /* obj= */ Performance.getInstance()
                    .onEventReceived(InputDevice.SOURCE_ROTARY_ENCODER));
        sendMessageDelayed(message, THROTTLE_MS);
      }
    }
  }
}
