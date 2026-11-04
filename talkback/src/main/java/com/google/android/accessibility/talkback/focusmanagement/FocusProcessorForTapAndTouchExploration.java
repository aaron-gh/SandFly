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

import static androidx.core.view.accessibility.AccessibilityWindowInfoCompat.TYPE_INPUT_METHOD;
import static com.google.android.accessibility.talkback.Interpretation.Touch.Action.LIFT;
import static com.google.android.accessibility.talkback.Interpretation.Touch.Action.LONG_PRESS;
import static com.google.android.accessibility.talkback.Interpretation.Touch.Action.TAP;
import static com.google.android.accessibility.talkback.Interpretation.Touch.Action.TOUCH_ENTERED_UNFOCUSED_NODE;
import static com.google.android.accessibility.talkback.Interpretation.Touch.Action.TOUCH_FOCUSED_NODE;
import static com.google.android.accessibility.talkback.Interpretation.Touch.Action.TOUCH_NOTHING;
import static com.google.android.accessibility.talkback.Interpretation.Touch.Action.TOUCH_START;
import static com.google.android.accessibility.talkback.Interpretation.Touch.Action.TOUCH_UNFOCUSED_NODE;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.GESTURE_LIFT_TO_TYPE;
import static com.google.android.accessibility.talkback.compositor.Compositor.EVENT_INPUT_DESCRIBE_NODE;
import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Message;
import android.os.SystemClock;
import android.view.ViewConfiguration;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityWindowInfoCompat;
import com.google.android.accessibility.talkback.ActorState;
import com.google.android.accessibility.talkback.Interpretation;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics;
import com.google.android.accessibility.talkback.directtouch.DirectTouchRegions;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.focusmanagement.action.TouchExplorationAction;
import com.google.android.accessibility.talkback.scripting.Scripts;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.AccessibilityWindowInfoUtils;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.Role;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.WeakReferenceHandler;
import com.google.android.libraries.accessibility.utils.device.ScreenUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;

/** Event interpreter to handle accessibility focus during touch interaction. */
public class FocusProcessorForTapAndTouchExploration {
  private static final String TAG = "FocusProcessorForTap";

  /** This specifies TalkBack supported type confirmation */
  public static final int DOUBLE_TAP = 0;
  public static final int LIFT_TO_TYPE = 1;
  // When set to this mode, all keys(including the function keys) from the input type window are
  // interpreted as text entry keys.
  public static final int FORCE_LIFT_TO_TYPE_ON_IME = 2;
  // Like FORCE_LIFT_TO_TYPE_ON_IME, but the key that sends or submits, such as Enter, Done, or
  // Send, still needs a double-tap.
  public static final int LIFT_TO_TYPE_EXCEPT_ACTION_KEY = 3;
  // Like FORCE_LIFT_TO_TYPE_ON_IME, and a tap on the keyboard types the key under it without
  // focusing or speaking it first.
  public static final int TAP_TO_TYPE = 4;

  /**
   * Typing method options: can be DOUBLE_TAP, LIFT_TO_TYPE, FORCE_LIFT_TO_TYPE_ON_IME,
   * LIFT_TO_TYPE_EXCEPT_ACTION_KEY, or TAP_TO_TYPE.
   */
  @IntDef({
    DOUBLE_TAP,
    LIFT_TO_TYPE,
    FORCE_LIFT_TO_TYPE_ON_IME,
    LIFT_TO_TYPE_EXCEPT_ACTION_KEY,
    TAP_TO_TYPE
  })
  public @interface TypingMethod {}

  /**
   * The end of the view ID of Gboard's action key, which is Enter, Done, Send, Search, or Go,
   * depending on the text field.
   */
  private static final String GBOARD_ACTION_KEY_ID_SUFFIX = ":id/key_pos_ime_action";

  /** Returns whether {@code typingMethod} lifts to type every key in the keyboard. */
  public static boolean liftsToTypeAnyKey(@TypingMethod int typingMethod) {
    return typingMethod == FORCE_LIFT_TO_TYPE_ON_IME
        || typingMethod == LIFT_TO_TYPE_EXCEPT_ACTION_KEY
        || typingMethod == TAP_TO_TYPE;
  }

  /**
   * Returns whether lifting the finger on {@code key} in the keyboard types it with {@code
   * typingMethod}. The keyboard's action key needs a double-tap with {@link
   * #LIFT_TO_TYPE_EXCEPT_ACTION_KEY}. Returns false if there's no key, such as when the finger
   * lifts where nothing can take focus.
   */
  public static boolean liftsToType(
      @TypingMethod int typingMethod, @Nullable AccessibilityNodeInfoCompat key) {
    if (key == null) {
      return false;
    }
    if (typingMethod == LIFT_TO_TYPE_EXCEPT_ACTION_KEY) {
      String id = key.getViewIdResourceName();
      return id == null || !id.endsWith(GBOARD_ACTION_KEY_ID_SUFFIX);
    }
    return typingMethod == FORCE_LIFT_TO_TYPE_ON_IME
        || typingMethod == TAP_TO_TYPE
        || (typingMethod == LIFT_TO_TYPE && Role.getRole(key) == Role.ROLE_TEXT_ENTRY_KEY);
  }

  /** The timeout after which an event is no longer considered a tap. */
  private static final long TAP_TIMEOUT_MS = ViewConfiguration.getJumpTapTimeout();

  // It is copied from  Gboard's code.
  private static final long LIFT_TO_TYPE_LONG_PRESS_DELAY_MS = 3000;

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Member variables

  private Pipeline.InterpretationReceiver interpretationReceiver;
  private ActorState actorState;
  private long longPressDuration = LIFT_TO_TYPE_LONG_PRESS_DELAY_MS;
  private final PostDelayHandler postDelayHandler;

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Settings

  private boolean isSingleTapEnabled = false;

  /** Indicates the current input type. */
  @TypingMethod private int typingMethod = LIFT_TO_TYPE;

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Boolean values representing states in the state machine.

  /**
   * Whether the interaction might lead to refocus action.
   *
   * <p>It's initially set to {@code true}. It's set to {@code false} if:
   *
   * <ul>
   *   <li>{@link SpeechController} is speaking on {@link
   *       TouchExplorationAction#TOUCH_INTERACTION_START} action
   *   <li>When the first non-null focusable node is touched, the node is not accessibility focused.
   * </ul>
   */
  private boolean mayBeRefocusAction = true;

  /**
   * Whether the interaction might lead to single tap(click) action.
   *
   * <p>It's initially set to {@code true}. It's set to {@code false} if:
   *
   * <ul>
   *   <li>The first focusable node being touched is not already accessibilityy focused.
   *   <li>The user touches on more than on focusable node during the interaction.
   * </ul>
   */
  private boolean mayBeSingleTap = true;

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Important nodes during the touch interaction.

  // Whether the user hovers enter any node during touch exploration.
  private boolean hasHoveredEnterNode = false;

  // The last focusable node being touched.
  @Nullable private AccessibilityNodeInfoCompat lastFocusableNodeBeingTouched;

  /**
   * Whether the interaction might lead to a click action.
   *
   * <p>It's initially set to {@code true}. It's set to {@code false} if the last touched node
   * performs a long click action:
   */
  private boolean mayBeLiftToType = true;

  // When Split-tapping triggered, we record & skip the Lift-to-type decision, to avoid the double
  // input phenomenon, until the touch interaction end.
  private boolean isSplitTap = false;

  // Whether the touch interaction ends with a tap on the keyboard with TAP_TO_TYPE, so the key
  // under the finger is typed without being focused.
  private boolean isTapToType = false;

  // The key a tap to type went down on, which it types even if the finger moved onto another key
  // before it lifted.
  @Nullable private AccessibilityNodeInfoCompat tapToTypeKey;

  private long touchInteractionStartTime;

  // Whether the finger of the touch in progress was last at the edge of the screen, where lifting
  // it, such as by sliding it off the screen, activates nothing.
  private boolean fingerAtScreenEdge = false;
  // When the finger last left the strip at the bottom of the screen. An app item touched from then
  // on means the finger moved back up; a finger that lifted in the strip touches none.
  private long leftScreenEdgeTimeMs = Long.MAX_VALUE;

  // How long the finger stays on an item before lifting it activates nothing, with Lift to
  // activate, or 0 for never. The setting is Hold to cancel.
  private long holdToCancelMs = 0;
  // Whether the finger stayed on the item it touches for holdToCancelMs.
  private boolean heldToCancel = false;
  // Plays the feedback of an activation cancelled by holding or by the edge of the screen.
  @Nullable private Runnable liftCancelledFeedback;

  // Whether a tap that lifts before the item under it is focused activates the item. The setting is
  // Tap to activate.
  private boolean tapToActivateEnabled = false;
  // Whether touch exploration of the touch in progress started after its finger lifted: a tap.
  private boolean isTapExploration = false;
  // The item a tap activates when the touch ends, found by that touch exploration.
  @Nullable private AccessibilityNodeInfoCompat tapToActivateNode;
  private final Context context;
  private final TalkBackAnalytics analytics;
  private final boolean splitTapEverywhere;

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Contstructor methods

  public FocusProcessorForTapAndTouchExploration(Context context, TalkBackAnalytics analytics) {
    this.context = context;
    this.analytics = analytics;
    this.splitTapEverywhere =
        FeatureSupport.supportSplitTapEverywhere() && FeatureFlagReader.splitTapEverywhere(context);
    postDelayHandler = new PostDelayHandler(this, longPressDuration);
  }

  /**
   * Called when the touch interaction in progress is a tap on the keyboard with {@link
   * #TAP_TO_TYPE}, before touch exploration of where the finger lifted starts.
   */
  public void onTapToType() {
    isTapToType = true;
  }

  public boolean isEnableLiftToType() {
    return typingMethod != DOUBLE_TAP;
  }

  public void setInterpretationReceiver(Pipeline.InterpretationReceiver interpretationReceiver) {
    this.interpretationReceiver = interpretationReceiver;
  }

  public void setActorState(ActorState actorState) {
    this.actorState = actorState;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Setters and getters for Setting values.

  public void setSingleTapEnabled(boolean enabled) {
    isSingleTapEnabled = enabled;
  }

  public boolean getSingleTapEnabled() {
    return isSingleTapEnabled;
  }

  public boolean onTouchExplorationAction(
      TouchExplorationAction touchExplorationAction, EventId eventId) {

    return switch (touchExplorationAction.type) {
      case TouchExplorationAction.TOUCH_INTERACTION_START -> handleTouchInteractionStart(eventId);
      case TouchExplorationAction.HOVER_ENTER ->
          handleHoverEnterNode(touchExplorationAction.touchedFocusableNode, eventId);
      case TouchExplorationAction.TOUCH_INTERACTION_END -> handleTouchInteractionEnd(eventId);
      default ->
          // Do nothing.
          false;
    };
  }

  /**
   * Handles the beginning of a new touch interaction event. It cancels the ongoing speech and reset
   * variables.
   *
   * @return {@code true} if successfully performs an accessibility action.
   */
  private boolean handleTouchInteractionStart(EventId eventId) {
    // Reset cached information at the beginning of a new touch interaction cycle.
    reset();
    fingerAtScreenEdge = false;
    leftScreenEdgeTimeMs = Long.MAX_VALUE;
    isTapExploration = false;

    touchInteractionStartTime = SystemClock.uptimeMillis();

    interpretationReceiver.input(eventId, null, Interpretation.TouchInteraction.create(true));
    if (actorState.getSpeechState().isSpeaking()) {
      // We refocus and re-announce a node if TalkBack is currently speaking.
      mayBeRefocusAction = true;
      interpretationReceiver.input(
          eventId, /* event= */ null, Interpretation.Touch.create(TOUCH_START));
    }
    // Always return false because no accessibility action is performed.
    return false;
  }

  /** Resets the cached information of the current touch interaction. */
  private void reset() {
    lastFocusableNodeBeingTouched = null;

    hasHoveredEnterNode = false;
    tapToTypeKey = null;
    tapToActivateNode = null;

    // Everything is possible at the beginning.
    mayBeRefocusAction = true;
    mayBeSingleTap = true;
    mayBeLiftToType = true;
    isSplitTap = false;
    heldToCancel = false;
  }

  /**
   * Handles hover enter events.
   *
   * @return {@code true} if successfully performs an accessibility action.
   */
  private boolean handleHoverEnterNode(
      @Nullable AccessibilityNodeInfoCompat touchedFocusableNode, EventId eventId) {
    if (isTapToType && tapToTypeKey != null) {
      return false;
    }
    postDelayHandler.cancelLongPress();
    postDelayHandler.cancelRefocusTimeout();
    if (fingerAtScreenEdge && eventId != null && eventId.getEventTimeMs() >= leftScreenEdgeTimeMs) {
      // The finger has moved back up from the strip at the bottom of the screen to an app's item.
      fingerAtScreenEdge = false;
    }
    if (touchedFocusableNode != null && !touchedFocusableNode.isAccessibilityFocused()) {
      // TalkBack do not enter the focused view with Split-typing & Lift-to-type the same time. When
      // focused moved into a new node, by default, TalkBack assumes the lift-to-type until a
      // split-typing gesture detected.
      isSplitTap = false;
      interpretationReceiver.input(
          eventId, /* event= */ null, Interpretation.Touch.create(TOUCH_ENTERED_UNFOCUSED_NODE));
    } else if (!hasHoveredEnterNode && actorState.getSpeechState().isSpeaking()) {
      // Speech that started after the finger went down, such as an app's announcement, isn't
      // stopped at the start of the touch, and an item that is already focused is spoken after
      // speech that new speech can't interrupt.
      interpretationReceiver.input(
          eventId, /* event= */ null, Interpretation.Touch.create(TOUCH_START));
    }
    if (tapToActivateEnabled
        && !hasHoveredEnterNode
        && touchedFocusableNode != null
        && isTapExploration
        && !(isEnableLiftToType() && supportsLiftToType(touchedFocusableNode))) {
      // Touch exploration started after the finger lifted, so the touch was a tap: the item is
      // activated when the touch ends, which follows at once, without being focused first.
      hasHoveredEnterNode = true;
      tapToActivateNode = touchedFocusableNode;
      lastFocusableNodeBeingTouched = touchedFocusableNode;
      return false;
    }
    tapToActivateNode = null;
    boolean result;
    if (isTapToType
        && !hasHoveredEnterNode
        && touchedFocusableNode != null
        && supportsLiftToType(touchedFocusableNode)) {
      // The key is typed when the touch ends, which follows at once.
      hasHoveredEnterNode = true;
      tapToTypeKey = touchedFocusableNode;
      result = false;
    } else if (!hasHoveredEnterNode) {
      hasHoveredEnterNode = true;
      // Handle the first node being touched.
      result = onHoverEnterFirstNode(touchedFocusableNode, eventId);
    } else {
      result = onHoverEnterGeneralNode(touchedFocusableNode, eventId);
    }

    // Reset it when last touched node is changed.
    mayBeLiftToType = true;
    lastFocusableNodeBeingTouched = touchedFocusableNode;

    int scriptLongPressMs = Scripts.longPressAfterMs(touchedFocusableNode);
    if (scriptLongPressMs > 0 && AccessibilityNodeInfoUtils.isLongClickable(touchedFocusableNode)) {
      postDelayHandler.longPressAfterTimeout(scriptLongPressMs);
    }
    // Each item the finger moves to can be activated again, and held again to cancel.
    heldToCancel = false;
    postDelayHandler.cancelHoldToCancel();
    if (holdToCancelMs > 0 && touchedFocusableNode != null && scriptLongPressMs <= 0) {
      postDelayHandler.holdToCancelAfter(holdToCancelMs);
    }
    return result;
  }

  /**
   * Handles the first node being touched during touch exploration.
   *
   * @return {@code true} if successfully performs an accessibility action.
   */
  private boolean onHoverEnterFirstNode(
      @Nullable AccessibilityNodeInfoCompat touchedFocusableNode, EventId eventId) {
    if (touchedFocusableNode == null || !touchedFocusableNode.isAccessibilityFocused()) {
      return onHoverEnterGeneralNode(touchedFocusableNode, eventId);
    }

    if (isEnableLiftToType() && supportsLiftToType(touchedFocusableNode)) {
      if (touchedFocusableNode.isAccessibilityFocused()) {
        mayBeRefocusAction = false;
        if (AccessibilityNodeInfoUtils.isLongClickable(touchedFocusableNode)) {
          postDelayHandler.longPressAfterTimeout();
        }

        interpretationReceiver.input(
            EVENT_ID_UNTRACKED,
            /* event= */ null,
            new Interpretation.CompositorID(EVENT_INPUT_DESCRIBE_NODE),
            touchedFocusableNode);

      } else {
        mayBeRefocusAction = true;
        if (AccessibilityNodeInfoUtils.isLongClickable(touchedFocusableNode)) {
          postDelayHandler.longPressAfterTimeout();
        }
      }
      return false;
    }

    if (isSingleTapEnabled) {
      // Post delay to refocus on it.
      // If user hovers enter another node before timeout, cancel refocus action.
      // If user lifts finger before timeout, cancel refocus action and click(single-tap) on the
      // node.
      postDelayHandler.refocusAfterTimeout();
    } else {
      return touchFocusedNode(touchedFocusableNode, eventId);
    }
    return false;
  }

  /**
   * @return {@code true} if the user's lift-to-activate setting says that lifting the finger on
   *     {@code node} should activate it.
   */
  private boolean shouldLiftToActivate(@Nullable AccessibilityNodeInfoCompat node) {
    if (node == null) {
      return false;
    }
    if (Scripts.liftsToActivate(node)) {
      return true;
    }
    LiftToActivateMode mode =
        LiftToActivateMode.fromPrefValue(
            SharedPreferencesUtils.getSharedPreferences(context)
                .getString(context.getString(R.string.pref_lift_to_activate_key), null));
    return LiftToActivate.shouldActivate(
        mode, mode == LiftToActivateMode.NAVIGATION_BAR && isNavigationBarButton(node));
  }

  /**
   * Returns whether {@code node} is a button of the system navigation bar: a System UI button with
   * a navigation bar view id, or any node in a system window shaped like a navigation bar, since
   * not every phone draws the bar in System UI.
   */
  private boolean isNavigationBarButton(AccessibilityNodeInfoCompat node) {
    if (LiftToActivate.isNavigationBarButton(node.getPackageName(), node.getViewIdResourceName())) {
      return true;
    }
    AccessibilityWindowInfoCompat window = AccessibilityNodeInfoUtils.getWindow(node);
    if (window == null || window.getType() != AccessibilityWindowInfoCompat.TYPE_SYSTEM) {
      return false;
    }
    Rect bounds = new Rect();
    window.getBoundsInScreen(bounds);
    Point display = ScreenUtils.getRealScreenSize(context);
    return DirectTouchRegions.INSTANCE.isNavigationBar(
        bounds.left, bounds.top, bounds.right, bounds.bottom, display.x, display.y);
  }

  /** @return {@code true} if the role of node support lift-to-type functionality. */
  private boolean supportsLiftToType(AccessibilityNodeInfoCompat accessibilityNodeInfoCompat) {
    if (typingMethod == LIFT_TO_TYPE_EXCEPT_ACTION_KEY
        && !liftsToType(typingMethod, accessibilityNodeInfoCompat)) {
      return false;
    }
    if (Role.getRole(accessibilityNodeInfoCompat) == Role.ROLE_TEXT_ENTRY_KEY) {
      return true;
    } else if (!liftsToTypeAnyKey(typingMethod)) {
      return false;
    }

    AccessibilityWindowInfoCompat window =
        AccessibilityNodeInfoUtils.getWindow(accessibilityNodeInfoCompat);
    return (AccessibilityWindowInfoUtils.getType(window) == TYPE_INPUT_METHOD);
  }

  /** @return {@code true} if successfully performs an accessibility action. */
  private boolean onHoverEnterGeneralNode(
      AccessibilityNodeInfoCompat touchedFocusableNode, EventId eventId) {
    // We'll try to focus on some node, it must not be a single tap action, nor a refocus action.
    mayBeSingleTap = false;
    mayBeRefocusAction = false;
    if (touchedFocusableNode == null) {
      interpretationReceiver.input(
          eventId, /* event= */ null, Interpretation.Touch.create(TOUCH_NOTHING));
      return false;
    } else {
      return touchNewNode(touchedFocusableNode, eventId);
    }
  }

  /**
   * Handles the end of an ongoing touch interaction event. Tries to perform click action in
   * single-tap mode or lift-to-type mode.
   *
   * @return {@code true} if successfully performs an accessibility action.
   */
  private boolean handleTouchInteractionEnd(EventId eventId) {
    // Touch interaction end, clear all the post-delayed actions.
    // TODO: Shall we also cancel empty touch feedback?
    postDelayHandler.cancelRefocusTimeout();
    postDelayHandler.cancelLongPress();
    postDelayHandler.cancelHoldToCancel();

    long currentTime = SystemClock.uptimeMillis();

    boolean result = false;
    boolean liftActivates = liftActivates();
    if (liftActivates && fingerAtScreenEdge) {
      LogUtils.v(TAG, "Not activating: the finger lifted at the edge of the screen");
    } else if (liftActivates && heldToCancel) {
      LogUtils.v(TAG, "Not activating: the finger was held on the item to cancel");
    } else if (tapToActivateNode != null) {
      // Tap to activate.
      result =
          interpretationReceiver.input(
              eventId, /* event= */ null, Interpretation.Touch.create(LIFT, tapToActivateNode));
    } else if (liftActivates) {
      // Perform click action for lift-to-type mode.
      analytics.onGesture(GESTURE_LIFT_TO_TYPE);
      result =
          interpretationReceiver.input(
              eventId,
              /* event= */ null,
              Interpretation.Touch.create(LIFT, lastFocusableNodeBeingTouched));
    } else if (isSingleTapEnabled
        && mayBeSingleTap
        && (currentTime - touchInteractionStartTime < TAP_TIMEOUT_MS)) {
      // Perform click for single-tap mode.
      result =
          interpretationReceiver.input(
              eventId,
              /* event= */ null,
              Interpretation.Touch.create(TAP, lastFocusableNodeBeingTouched));
    }
    // Perform Custom Action menu switching in reading control
    interpretationReceiver.input(eventId, null, Interpretation.TouchInteraction.create(false));

    reset();
    isTapToType = false;
    isTapExploration = false;
    return result;
  }

  /**
   * Holding the finger on an item with Lift to activate makes lifting it activate nothing. Keys
   * that lift to type are left to their own long press.
   */
  private void holdToCancel() {
    AccessibilityNodeInfoCompat node = lastFocusableNodeBeingTouched;
    if (node == null
        || !mayBeLiftToType
        || isSplitTap
        || (isEnableLiftToType() && supportsLiftToType(node))
        || !shouldLiftToActivate(node)) {
      return;
    }
    heldToCancel = true;
    LogUtils.v(TAG, "Held on the item: lifting activates nothing");
    if (liftCancelledFeedback != null) {
      liftCancelledFeedback.run();
    }
  }

  /**
   * Sets whether the finger of the touch in progress is at the edge of the screen, where lifting it
   * activates nothing.
   */
  public void setFingerAtScreenEdge(boolean atEdge) {
    fingerAtScreenEdge = atEdge;
    leftScreenEdgeTimeMs = Long.MAX_VALUE;
  }

  /**
   * Notes that the finger left the strip at the bottom of the screen at {@code timeMs}, by moving
   * back up or by lifting.
   */
  public void onFingerLeftScreenEdge(long timeMs) {
    leftScreenEdgeTimeMs = timeMs;
  }

  /** Sets how long the finger stays on an item for lifting it to activate nothing; 0 for never. */
  public void setHoldToCancelMs(long ms) {
    holdToCancelMs = Math.max(0, ms);
  }

  /** Sets what plays when holding or the edge of the screen cancels an activation. */
  public void setLiftCancelledFeedback(@Nullable Runnable feedback) {
    liftCancelledFeedback = feedback;
  }

  /** Sets whether a tap that lifts before the item under it is focused activates the item. */
  public void setTapToActivateEnabled(boolean enabled) {
    tapToActivateEnabled = enabled;
  }

  /**
   * Called when touch exploration of the touch in progress starts after its finger lifted, so the
   * touch was a tap, before touch exploration finds the item under it.
   */
  public void onTapExploration() {
    isTapExploration = true;
  }

  /**
   * Returns whether lifting the finger now activates the item it last touched, with Lift to
   * activate, or types the key, with lift to type.
   */
  public boolean liftActivates() {
    return mayBeLiftToType
        && !isSplitTap
        && ((isEnableLiftToType() && supportsLiftToType(lastFocusableNodeBeingTouched))
            || shouldLiftToActivate(lastFocusableNodeBeingTouched));
  }

  /**
   * If the focused node exists, then 1. When splitTapEverywhere is configured and either it set
   * text entry key or the window type is IME, perform click action. 2. Otherwise, perform click
   * action for all clickable nodes.
   *
   * @return {@code true} if successfully performs click action.
   */
  public boolean performSplitTap(EventId eventId) {
    if (lastFocusableNodeBeingTouched != null) {
      if (splitTapEverywhere
          || (Role.getRole(lastFocusableNodeBeingTouched) == Role.ROLE_TEXT_ENTRY_KEY
              || AccessibilityWindowInfoUtils.getType(
                      AccessibilityNodeInfoUtils.getWindow(lastFocusableNodeBeingTouched))
                  == TYPE_INPUT_METHOD)) {
        // Perform click action for lift-to-type mode.
        interpretationReceiver.input(
            eventId,
            /* event= */ null,
            Interpretation.Touch.create(LIFT, lastFocusableNodeBeingTouched));
        isSplitTap = true;
        return true;
      }
    }
    return false;
  }

  private boolean touchFocusedNode(AccessibilityNodeInfoCompat node, @Nullable EventId eventId) {
    return mayBeRefocusAction
        && (node != null)
        && interpretationReceiver.input(
            eventId, /* event= */ null, Interpretation.Touch.create(TOUCH_FOCUSED_NODE, node));
  }

  private boolean touchNewNode(AccessibilityNodeInfoCompat node, EventId eventId) {

    boolean result =
        interpretationReceiver.input(
            eventId, /* event= */ null, Interpretation.Touch.create(TOUCH_UNFOCUSED_NODE, node));

    if (result
        && isEnableLiftToType()
        && supportsLiftToType(node)
        && AccessibilityNodeInfoUtils.isLongClickable(node)) {
      postDelayHandler.longPressAfterTimeout();
    }
    return result;
  }

  /** Delays for detecting long-presses and unique touches */
  private static class PostDelayHandler
      extends WeakReferenceHandler<FocusProcessorForTapAndTouchExploration> {
    private static final int MSG_REFOCUS = 1;
    private static final int MSG_LONG_CLICK_LAST_NODE = 2;
    private static final int MSG_HOLD_TO_CANCEL = 3;

    private long longPressDelayMs;

    private PostDelayHandler(
        FocusProcessorForTapAndTouchExploration parent, long longPressDelayMs) {
      super(parent);
      this.longPressDelayMs = longPressDelayMs;
    }

    @Override
    protected void handleMessage(Message msg, FocusProcessorForTapAndTouchExploration parent) {
      if (parent == null) {
        return;
      }
      switch (msg.what) {
        case MSG_REFOCUS ->
            parent.touchFocusedNode(parent.lastFocusableNodeBeingTouched, /* eventId= */ null);
        case MSG_LONG_CLICK_LAST_NODE -> {
          if (parent.supportsLiftToType(parent.lastFocusableNodeBeingTouched)
              || Scripts.longPressAfterMs(parent.lastFocusableNodeBeingTouched) > 0) {
            parent.interpretationReceiver.input(
                /* eventId= */ null,
                /* event= */ null,
                Interpretation.Touch.create(LONG_PRESS, parent.lastFocusableNodeBeingTouched));
            parent.mayBeLiftToType = false;
          }
        }
        case MSG_HOLD_TO_CANCEL -> parent.holdToCancel();
        default -> {}
      }
    }

    private void holdToCancelAfter(long delayMs) {
      removeMessages(MSG_HOLD_TO_CANCEL);
      sendEmptyMessageDelayed(MSG_HOLD_TO_CANCEL, delayMs);
    }

    private void cancelHoldToCancel() {
      removeMessages(MSG_HOLD_TO_CANCEL);
    }

    private void longPressAfterTimeout() {
      longPressAfterTimeout(longPressDelayMs);
    }

    private void longPressAfterTimeout(long delayMs) {
      removeMessages(MSG_LONG_CLICK_LAST_NODE);
      sendEmptyMessageDelayed(MSG_LONG_CLICK_LAST_NODE, delayMs);
    }

    private void cancelLongPress() {
      removeMessages(MSG_LONG_CLICK_LAST_NODE);
    }

    private void refocusAfterTimeout() {
      removeMessages(MSG_REFOCUS);

      final Message msg = obtainMessage(MSG_REFOCUS);
      sendMessageDelayed(msg, TAP_TIMEOUT_MS);
    }

    private void cancelRefocusTimeout() {
      removeMessages(MSG_REFOCUS);
    }
  }

  public void setTypingMethod(@TypingMethod int type) {
    if (!splitTapEverywhere || typingMethod != type) {
      reset();
      typingMethod = type;
    }
  }

  @TypingMethod
  public int getTypingMethod() {
    return typingMethod;
  }

  public void setTypingLongPressDurationMs(int duration) {
    longPressDuration = (long) duration;
    if (postDelayHandler != null) {
      postDelayHandler.longPressDelayMs = longPressDuration;
    }
  }
}
