/*
 * Copyright (C) 2021s The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.utils.gestures;

import static android.accessibilityservice.AccessibilityService.GESTURE_2_FINGER_SWIPE_DOWN;
import static android.accessibilityservice.AccessibilityService.GESTURE_2_FINGER_SWIPE_LEFT;
import static android.accessibilityservice.AccessibilityService.GESTURE_2_FINGER_SWIPE_RIGHT;
import static android.accessibilityservice.AccessibilityService.GESTURE_2_FINGER_SWIPE_UP;
import static com.google.android.accessibility.utils.gestures.TwoFingerSecondFingerMultiTap.ROTATE_DIRECTION_BACKWARD;
import static com.google.android.accessibility.utils.gestures.TwoFingerSecondFingerMultiTap.ROTATE_DIRECTION_FORWARD;

import android.accessibilityservice.AccessibilityGestureEvent;
import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.os.Build;
import android.view.MotionEvent;
import androidx.annotation.RequiresApi;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.gestures.GestureMatcher.AnalyticsEventLogger;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayList;
import java.util.List;

/**
 * This class coordinates a series of individual gesture matchers to serve as a unified gesture
 * detector. Gesture matchers are tied to a single gesture. It calls listener callback functions
 * when a gesture starts or completes.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public class GestureManifold implements GestureMatcher.StateChangeListener {
  public static final int GESTURE_FAKED_SPLIT_TYPING = -3;
  public static final int GESTURE_TAP_HOLD_AND_2ND_FINGER_FORWARD_DOUBLE_TAP = -4;
  public static final int GESTURE_TAP_HOLD_AND_2ND_FINGER_BACKWARD_DOUBLE_TAP = -5;
  public static final int GESTURE_TOUCH_EXPLORE = -6;
  public static final int GESTURE_TAP_UP_TOUCH_EXPLORE = -7;
  public static final int GESTURE_FAKED_SPLIT_TYPING_AND_HOLD = -8;
  // Two fingers turning around each other. Reported once per step of rotation, while the fingers
  // are still down, by TalkBack's TouchInteractionMonitor rather than by a gesture matcher.
  public static final int GESTURE_2_FINGER_ROTATE_CLOCKWISE = -9;
  public static final int GESTURE_2_FINGER_ROTATE_COUNTERCLOCKWISE = -10;
  // Android has no gestures beyond triple taps, so Backtalk's own detection adds this one.
  public static final int GESTURE_3_FINGER_QUADRUPLE_TAP = -11;
  // One finger sliding in from an edge of the screen, as on iOS, and the two-finger scrub. Reported
  // by TalkBack's TouchInteractionMonitor rather than by a gesture matcher.
  public static final int GESTURE_EDGE_SWIPE_UP_FROM_BOTTOM = -12;
  public static final int GESTURE_EDGE_LONG_SWIPE_UP_FROM_BOTTOM = -13;
  public static final int GESTURE_EDGE_SWIPE_DOWN_FROM_TOP_LEFT = -14;
  public static final int GESTURE_EDGE_SWIPE_DOWN_FROM_TOP_RIGHT = -15;
  public static final int GESTURE_2_FINGER_SCRUB = -16;
  // Fingers moving toward their middle or away from it. Reported by TalkBack's
  // TouchInteractionMonitor rather than by a gesture matcher.
  public static final int GESTURE_2_FINGER_PINCH = -17;
  public static final int GESTURE_2_FINGER_SPREAD = -18;
  public static final int GESTURE_3_FINGER_PINCH = -19;
  public static final int GESTURE_3_FINGER_SPREAD = -20;
  public static final int GESTURE_4_FINGER_PINCH = -21;
  public static final int GESTURE_4_FINGER_SPREAD = -22;
  // More edge gestures: the second stage from the top edge, and both stages from each half of the
  // left and right edges.
  public static final int GESTURE_EDGE_LONG_SWIPE_DOWN_FROM_TOP_LEFT = -23;
  public static final int GESTURE_EDGE_LONG_SWIPE_DOWN_FROM_TOP_RIGHT = -24;
  public static final int GESTURE_EDGE_SWIPE_RIGHT_FROM_LEFT_TOP = -25;
  public static final int GESTURE_EDGE_LONG_SWIPE_RIGHT_FROM_LEFT_TOP = -26;
  public static final int GESTURE_EDGE_SWIPE_RIGHT_FROM_LEFT_BOTTOM = -27;
  public static final int GESTURE_EDGE_LONG_SWIPE_RIGHT_FROM_LEFT_BOTTOM = -28;
  public static final int GESTURE_EDGE_SWIPE_LEFT_FROM_RIGHT_TOP = -29;
  public static final int GESTURE_EDGE_LONG_SWIPE_LEFT_FROM_RIGHT_TOP = -30;
  public static final int GESTURE_EDGE_SWIPE_LEFT_FROM_RIGHT_BOTTOM = -31;
  public static final int GESTURE_EDGE_LONG_SWIPE_LEFT_FROM_RIGHT_BOTTOM = -32;
  // One-finger swipes whose finger rests at the end before lifting, which SandFly's own detection
  // reports in place of the swipe when they have an action.
  public static final int GESTURE_SWIPE_UP_AND_HOLD = -40;
  public static final int GESTURE_SWIPE_DOWN_AND_HOLD = -41;
  public static final int GESTURE_SWIPE_LEFT_AND_HOLD = -42;
  public static final int GESTURE_SWIPE_RIGHT_AND_HOLD = -43;
  public static final int GESTURE_SWIPE_UP_AND_DOWN_AND_HOLD = -44;
  public static final int GESTURE_SWIPE_DOWN_AND_UP_AND_HOLD = -45;
  public static final int GESTURE_SWIPE_LEFT_AND_RIGHT_AND_HOLD = -46;
  public static final int GESTURE_SWIPE_RIGHT_AND_LEFT_AND_HOLD = -47;
  public static final int GESTURE_SWIPE_UP_AND_LEFT_AND_HOLD = -48;
  public static final int GESTURE_SWIPE_UP_AND_RIGHT_AND_HOLD = -49;
  public static final int GESTURE_SWIPE_DOWN_AND_LEFT_AND_HOLD = -50;
  public static final int GESTURE_SWIPE_DOWN_AND_RIGHT_AND_HOLD = -51;
  public static final int GESTURE_SWIPE_LEFT_AND_UP_AND_HOLD = -52;
  public static final int GESTURE_SWIPE_LEFT_AND_DOWN_AND_HOLD = -53;
  public static final int GESTURE_SWIPE_RIGHT_AND_UP_AND_HOLD = -54;
  public static final int GESTURE_SWIPE_RIGHT_AND_DOWN_AND_HOLD = -55;
  // One finger sliding along an edge of the screen. Reported by TalkBack's TouchInteractionMonitor
  // rather than by a gesture matcher.
  public static final int GESTURE_EDGE_SWIPE_UP_ALONG_LEFT = -56;
  public static final int GESTURE_EDGE_SWIPE_DOWN_ALONG_LEFT = -57;
  public static final int GESTURE_EDGE_SWIPE_UP_ALONG_RIGHT = -58;
  public static final int GESTURE_EDGE_SWIPE_DOWN_ALONG_RIGHT = -59;
  public static final int GESTURE_EDGE_SWIPE_LEFT_ALONG_TOP = -60;
  public static final int GESTURE_EDGE_SWIPE_RIGHT_ALONG_TOP = -61;
  public static final int GESTURE_EDGE_SWIPE_LEFT_ALONG_BOTTOM = -62;
  public static final int GESTURE_EDGE_SWIPE_RIGHT_ALONG_BOTTOM = -63;
  // A four-finger tap on the top or bottom half of the screen. Reported by TalkBack's
  // TouchInteractionMonitor in place of the four-finger tap when one of them has an action.
  public static final int GESTURE_4_FINGER_SINGLE_TAP_TOP = -64;
  public static final int GESTURE_4_FINGER_SINGLE_TAP_BOTTOM = -65;
  // One-finger taps in a corner of the screen. Reported by TalkBack's TouchInteractionMonitor in
  // place of the taps when the corner gesture has an action.
  public static final int GESTURE_CORNER_TOP_LEFT_TAP = -66;
  public static final int GESTURE_CORNER_TOP_LEFT_DOUBLE_TAP = -67;
  public static final int GESTURE_CORNER_TOP_LEFT_TRIPLE_TAP = -68;
  public static final int GESTURE_CORNER_TOP_LEFT_DOUBLE_TAP_AND_HOLD = -69;
  public static final int GESTURE_CORNER_TOP_LEFT_TRIPLE_TAP_AND_HOLD = -70;
  public static final int GESTURE_CORNER_TOP_RIGHT_TAP = -71;
  public static final int GESTURE_CORNER_TOP_RIGHT_DOUBLE_TAP = -72;
  public static final int GESTURE_CORNER_TOP_RIGHT_TRIPLE_TAP = -73;
  public static final int GESTURE_CORNER_TOP_RIGHT_DOUBLE_TAP_AND_HOLD = -74;
  public static final int GESTURE_CORNER_TOP_RIGHT_TRIPLE_TAP_AND_HOLD = -75;
  public static final int GESTURE_CORNER_BOTTOM_LEFT_TAP = -76;
  public static final int GESTURE_CORNER_BOTTOM_LEFT_DOUBLE_TAP = -77;
  public static final int GESTURE_CORNER_BOTTOM_LEFT_TRIPLE_TAP = -78;
  public static final int GESTURE_CORNER_BOTTOM_LEFT_DOUBLE_TAP_AND_HOLD = -79;
  public static final int GESTURE_CORNER_BOTTOM_LEFT_TRIPLE_TAP_AND_HOLD = -80;
  public static final int GESTURE_CORNER_BOTTOM_RIGHT_TAP = -81;
  public static final int GESTURE_CORNER_BOTTOM_RIGHT_DOUBLE_TAP = -82;
  public static final int GESTURE_CORNER_BOTTOM_RIGHT_TRIPLE_TAP = -83;
  public static final int GESTURE_CORNER_BOTTOM_RIGHT_DOUBLE_TAP_AND_HOLD = -84;
  public static final int GESTURE_CORNER_BOTTOM_RIGHT_TRIPLE_TAP_AND_HOLD = -85;

  // Match the value of GESTURE_ID_2FINGER_1TAP_HOLD in TalkBack.
  public static final int GESTURE_2_FINGER_SINGLE_TAP_AND_HOLD = 63;

  private static final String LOG_TAG = "GestureManifold";

  private final List<GestureMatcher> gestures = new ArrayList<>();
  private final int displayId;
  // Listener to be notified of gesture start and end.
  private final Listener listener;
  // Whether multi-finger gestures are enabled.
  boolean multiFingerGesturesEnabled;
  // Whether the two-finger passthrough is enabled when multi-finger gestures are enabled.
  private boolean twoFingerPassthroughEnabled;
  // A list of all the multi-finger gestures, for easy adding and removal.
  private final List<GestureMatcher> multiFingerGestures = new ArrayList<>();
  // A list of two-finger swipes, for easy adding and removal when turning on or off two-finger
  // passthrough.
  private final List<GestureMatcher> twoFingerSwipes = new ArrayList<>();
  private boolean logMotionEvent = false;
  // Whether a gesture completed on the move event being processed.
  private boolean completedInMove = false;

  /** Define the interface to get project base feature settings. */
  /** Returns the swipe and hold version of the one-finger swipe {@code swipeGestureId}, or 0. */
  public static int swipeAndHoldOf(int swipeGestureId) {
    return switch (swipeGestureId) {
      case AccessibilityService.GESTURE_SWIPE_UP -> GESTURE_SWIPE_UP_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_DOWN -> GESTURE_SWIPE_DOWN_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_LEFT -> GESTURE_SWIPE_LEFT_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_RIGHT -> GESTURE_SWIPE_RIGHT_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_UP_AND_DOWN -> GESTURE_SWIPE_UP_AND_DOWN_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_DOWN_AND_UP -> GESTURE_SWIPE_DOWN_AND_UP_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_LEFT_AND_RIGHT -> GESTURE_SWIPE_LEFT_AND_RIGHT_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_RIGHT_AND_LEFT -> GESTURE_SWIPE_RIGHT_AND_LEFT_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_UP_AND_LEFT -> GESTURE_SWIPE_UP_AND_LEFT_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_UP_AND_RIGHT -> GESTURE_SWIPE_UP_AND_RIGHT_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_DOWN_AND_LEFT -> GESTURE_SWIPE_DOWN_AND_LEFT_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_DOWN_AND_RIGHT -> GESTURE_SWIPE_DOWN_AND_RIGHT_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_LEFT_AND_UP -> GESTURE_SWIPE_LEFT_AND_UP_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_LEFT_AND_DOWN -> GESTURE_SWIPE_LEFT_AND_DOWN_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_RIGHT_AND_UP -> GESTURE_SWIPE_RIGHT_AND_UP_AND_HOLD;
      case AccessibilityService.GESTURE_SWIPE_RIGHT_AND_DOWN -> GESTURE_SWIPE_RIGHT_AND_DOWN_AND_HOLD;
      default -> 0;
    };
  }

  public interface GestureConfigProvider {
    default float getDoubleTapSlopMultiplier() {
      return (float) 1.0;
    }

    default boolean getSpeedUpTouchExploreState() {
      return false;
    }

    default boolean invalidSwipeGestureEarlyDetection() {
      return false;
    }

    default boolean useMultipleGestureSet() {
      return false;
    }

    default boolean enableSplitTapAndHold() {
      return false;
    }

    /** Returns whether {@code gestureId} has an action, so that it is worth matching. */
    default boolean isGestureAssigned(int gestureId) {
      return false;
    }

    /**
     * Returns whether the touch may still become another kind of gesture, such as an edge swipe,
     * so that a swipe must not run before the finger lifts.
     */
    default boolean holdsSwipeUntilLift() {
      return false;
    }
  }

  public GestureManifold(
      Context context,
      Listener listener,
      GestureConfigProvider configResolver,
      AnalyticsEventLogger logger,
      int displayId,
      ImmutableList<String> supportGestureList) {
    this.listener = listener;
    this.displayId = displayId;
    multiFingerGesturesEnabled = false;
    twoFingerPassthroughEnabled = false;

    // Set up gestures.
    List<GestureMatcher> gestureMatcherList =
        GestureMatcherFactory.getGestureMatcherList(
            context, supportGestureList, this, configResolver, logger);

    for (GestureMatcher gestureMatcher : gestureMatcherList) {
      if (gestureMatcher != null) {
        if ((gestureMatcher instanceof Swipe)
            || (gestureMatcher instanceof MultiTap)
            || (gestureMatcher instanceof MultiTapAndHold)
            || (gestureMatcher instanceof SecondFingerTap)) {
          gestures.add(gestureMatcher);
        } else {
          multiFingerGestures.add(gestureMatcher);
          int gestureId = gestureMatcher.getGestureId();
          if ((gestureId == GESTURE_2_FINGER_SWIPE_DOWN)
              || (gestureId == GESTURE_2_FINGER_SWIPE_LEFT)
              || (gestureId == GESTURE_2_FINGER_SWIPE_RIGHT)
              || (gestureId == GESTURE_2_FINGER_SWIPE_UP)) {
            twoFingerSwipes.add(gestureMatcher);
          }
        }
      }
    }
    if (configResolver.useMultipleGestureSet()) {
      gestures.add(
          new TwoFingerSecondFingerMultiTap(
              context,
              2,
              ROTATE_DIRECTION_FORWARD,
              GESTURE_TAP_HOLD_AND_2ND_FINGER_FORWARD_DOUBLE_TAP,
              this,
              logger));
      gestures.add(
          new TwoFingerSecondFingerMultiTap(
              context,
              2,
              ROTATE_DIRECTION_BACKWARD,
              GESTURE_TAP_HOLD_AND_2ND_FINGER_BACKWARD_DOUBLE_TAP,
              this,
              logger));
    }
  }

  public void onConfigurationChanged(Context context) {
    for (GestureMatcher gestureDetector : gestures) {
      gestureDetector.onConfigurationChanged(context);
    }
  }

  public void enableLogMotionEvent() {
    logMotionEvent = true;
    for (GestureMatcher gestureDetector : gestures) {
      gestureDetector.enableLogMotionEvent();
    }
  }

  /**
   * Processes a motion event.
   *
   * @param event The event as received from the previous entry in the event stream.
   * @return True if the event has been appropriately handled by the gesture manifold and related
   *     callback functions, false if it should be handled further by the calling function.
   */
  @CanIgnoreReturnValue
  public boolean onMotionEvent(EventId eventId, MotionEvent event) {
    completedInMove = false;
    for (GestureMatcher matcher : gestures) {
      if (matcher.getState() != GestureMatcher.STATE_GESTURE_CANCELED) {
        if (logMotionEvent) {
          LogUtils.v(LOG_TAG, matcher.toString());
        }
        matcher.onMotionEvent(eventId, event);
        if (logMotionEvent) {
          LogUtils.v(LOG_TAG, matcher.toString());
        }

        // A gesture completed before the lift, whose listener may have cleared every matcher: the
        // rest of the touch is not a gesture.
        if (matcher.getState() == GestureMatcher.STATE_GESTURE_COMPLETED || completedInMove) {
          // Here we just return. The actual gesture dispatch is done in
          // onStateChanged().
          // No need to process this event any further.
          return true;
        }
      }
    }
    return false;
  }

  public void clear() {
    for (GestureMatcher matcher : gestures) {
      matcher.clear();
    }
  }

  /**
   * Listener that receives notifications of the state of the gesture detector. Listener functions
   * are called as a result of onMotionEvent(). The current MotionEvent in the context of these
   * functions is the event passed into onMotionEvent.
   */
  public interface Listener {

    /**
     * Called when the system has decided the event stream is a potential gesture.
     *
     * @param gestureId the gesture which is start matching.
     */
    void onGestureStarted(int gestureId);

    /**
     * Called when an event stream is recognized as a gesture.
     *
     * @param gestureEvent Information about the gesture.
     */
    void onGestureCompleted(AccessibilityGestureEvent gestureEvent);

    /**
     * Called when the system has decided an event stream doesn't match any known gesture.
     *
     * @param gestureId the gesture which is fail to match.
     */
    void onGestureCancelled(int gestureId);

    /**
     * Called when the gesture is processing and should be avoided to be interrupted. It's mainly be
     * used to extend the multi-tap timeout even the user sets the touch focus delay with a shorter
     * time.
     *
     * @param gestureId the gesture which is fail to match.
     */
    default void onGestureProcessing(int gestureId) {}
  }

  @Override
  public void onStateChanged(int gestureId, int state, MotionEvent event) {
    if (state == GestureMatcher.STATE_GESTURE_STARTED) {
      listener.onGestureStarted(gestureId);
    } else if (state == GestureMatcher.STATE_GESTURE_COMPLETED) {
      onGestureCompleted(gestureId, event);
    } else if (state == GestureMatcher.STATE_GESTURE_CANCELED) {
      listener.onGestureCancelled(gestureId);
    } else if (state == GestureMatcher.STATE_GESTURE_PROCESSING) {
      listener.onGestureProcessing(gestureId);
    }
  }

  /**
   * Called when the gesture detector has successfully identified the gesture by a series of
   * MotionEvent.
   *
   * @param gestureId the gesture which is fail to match.
   * @param event the last MotionEvent to match the identified gesture.
   */
  private void onGestureCompleted(int gestureId, MotionEvent event) {
    if (event != null && event.getActionMasked() == MotionEvent.ACTION_MOVE) {
      completedInMove = true;
    }
    // Note that gestures that complete immediately call clear() from onMotionEvent.
    // Gestures that complete on a delay call clear() here.
    ArrayList<MotionEvent> eventList = new ArrayList<>();
    eventList.add(event);
    AccessibilityGestureEvent gestureEvent =
        new AccessibilityGestureEvent(gestureId, displayId, eventList);
    for (GestureMatcher matcher : gestures) {
      if (gestureId == GESTURE_TAP_UP_TOUCH_EXPLORE
          && matcher.bypassCancelByTapUpToTouchExplore()) {
        // Skip to cancel the gesture which claims itself to bypass cancel for this event.
        continue;
      } else if (gestureId == GESTURE_FAKED_SPLIT_TYPING
          && matcher.getGestureId() == GESTURE_FAKED_SPLIT_TYPING_AND_HOLD) {
        matcher.restart(true);
        continue;
      } else if (gestureId == GESTURE_FAKED_SPLIT_TYPING_AND_HOLD
          && matcher.getGestureId() == GESTURE_FAKED_SPLIT_TYPING) {
        matcher.restart(true);
        continue;
      }
      if (matcher.getGestureId() != gestureId) {
        matcher.cancelGesture(event, false);
      }
    }
    listener.onGestureCompleted(gestureEvent);
  }

  public boolean isMultiFingerGesturesEnabled() {
    return multiFingerGesturesEnabled;
  }

  public void setMultiFingerGesturesEnabled(boolean mode) {
    if (multiFingerGesturesEnabled != mode) {
      multiFingerGesturesEnabled = mode;
      if (mode) {
        gestures.addAll(multiFingerGestures);
      } else {
        gestures.removeAll(multiFingerGestures);
      }
    }
  }

  public boolean isTwoFingerPassthroughEnabled() {
    return twoFingerPassthroughEnabled;
  }

  public void setTwoFingerPassthroughEnabled(boolean mode) {
    if (twoFingerPassthroughEnabled != mode) {
      twoFingerPassthroughEnabled = mode;
      if (!mode) {
        multiFingerGestures.addAll(twoFingerSwipes);
        if (multiFingerGesturesEnabled) {
          gestures.addAll(twoFingerSwipes);
        }
      } else {
        multiFingerGestures.removeAll(twoFingerSwipes);
        gestures.removeAll(twoFingerSwipes);
      }
    }
  }

  /**
   * This class helps to collect data (saved time to enter Touch Explore), in addition to the
   * fundamental Gesture analytic event.
   */
  public static class TapToTouchExploreAnalyticsEvent extends GestureAnalyticsEvent {
    public int savedTimeMs;

    public TapToTouchExploreAnalyticsEvent(int event, int gestureId, int savedTimeMs) {
      super(event, gestureId);
      this.savedTimeMs = savedTimeMs;
    }
  }
}
