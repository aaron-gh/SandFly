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

package com.google.android.accessibility.utils.gestures;

import android.content.Context;
import android.os.Build;
import android.view.MotionEvent;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.gestures.GestureManifold.GestureConfigProvider;

/**
 * Matches one, two or three taps with several fingers followed by a swipe in one direction with as
 * many fingers or with one finger, such as a double-tap with two fingers and then a swipe up with
 * two fingers. The swipe is the next touch after the last tap, which starts within the time allowed
 * between taps.
 *
 * <p>The matcher goes on to the swipe only while its gesture has an action, and the taps it starts
 * with wait for the swipe only then.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MultiFingerTapThenSwipe extends MultiFingerMultiTap {

  private final int direction;
  // The number of fingers of the swipe.
  private final int swipeFingers;
  private final GestureConfigProvider gestureConfig;
  // Matches the swipe after the taps. Null only while the superclass constructor runs.
  @Nullable private MultiFingerSwipe swipe;
  // Whether the taps are done and the next touch is matched as the swipe.
  private boolean swipePhase;
  // When the fingers lifted from the last tap.
  private long tapsEndTime;
  @Nullable private EventId swipeEventId;

  MultiFingerTapThenSwipe(
      Context context,
      int fingers,
      int taps,
      int swipeFingers,
      int direction,
      int gestureId,
      GestureMatcher.StateChangeListener listener,
      GestureConfigProvider configProvider,
      GestureMatcher.AnalyticsEventLogger logger) {
    super(context, fingers, taps, gestureId, listener, logger);
    this.direction = direction;
    this.swipeFingers = swipeFingers;
    this.gestureConfig = configProvider;
    swipe =
        new MultiFingerSwipe(
            context, swipeFingers, direction, gestureId, this::onSwipeStateChanged, logger);
  }

  @Override
  public void clear() {
    swipePhase = false;
    swipeEventId = null;
    if (swipe != null) {
      swipe.clear();
    }
    super.clear();
  }

  @Override
  public void onConfigurationChanged(Context context) {
    super.onConfigurationChanged(context);
    if (swipe != null) {
      swipe.onConfigurationChanged(context);
    }
  }

  /** The gesture goes on after all of its taps, with the swipe. */
  @Override
  boolean continuesAfterTaps(int fingers, int taps) {
    return targetFingerCount == fingers && mTargetTapCount >= taps;
  }

  @Override
  protected void onTapsDone(EventId eventId, MotionEvent event) {
    // Checked once the taps are done rather than at each touch, as the check can read the window
    // list for app gestures.
    if (!gestureConfig.isGestureAssigned(getGestureId())) {
      // Nothing to run, so nothing to wait for.
      cancelGesture(event, /* notify= */ false);
      return;
    }
    swipePhase = true;
    tapsEndTime = event.getEventTime();
    if (swipe != null) {
      swipe.clear();
    }
    // The swipe must start within the time allowed between taps.
    cancelAfterDoubleTapTimeout(event);
  }

  @Override
  protected void onDown(EventId eventId, MotionEvent event) {
    if (!swipePhase) {
      super.onDown(eventId, event);
      return;
    }
    if (event.getEventTime() - tapsEndTime > GestureConfiguration.getMultiFingerTapTimeoutMs()) {
      cancelGesture(event);
      return;
    }
    cancelPendingTransitions();
    forwardToSwipe(eventId, event);
  }

  @Override
  protected void onPointerDown(EventId eventId, MotionEvent event) {
    if (swipePhase) {
      forwardToSwipe(eventId, event);
    } else {
      super.onPointerDown(eventId, event);
    }
  }

  @Override
  protected void onMove(EventId eventId, MotionEvent event) {
    if (swipePhase) {
      forwardToSwipe(eventId, event);
    } else {
      super.onMove(eventId, event);
    }
  }

  @Override
  protected void onPointerUp(EventId eventId, MotionEvent event) {
    if (swipePhase) {
      forwardToSwipe(eventId, event);
    } else {
      super.onPointerUp(eventId, event);
    }
  }

  @Override
  protected void onUp(EventId eventId, MotionEvent event) {
    if (!swipePhase) {
      super.onUp(eventId, event);
      return;
    }
    forwardToSwipe(eventId, event);
    if (getState() != STATE_GESTURE_COMPLETED && getState() != STATE_GESTURE_CANCELED) {
      // The fingers lifted without swiping: another tap.
      cancelGesture(event);
    }
  }

  private void forwardToSwipe(EventId eventId, MotionEvent event) {
    if (swipe == null) {
      cancelGesture(event);
      return;
    }
    swipeEventId = eventId;
    swipe.onMotionEvent(eventId, event);
  }

  /** Follows the swipe matcher, which reports to this matcher only. */
  private void onSwipeStateChanged(int gestureId, int state, MotionEvent event) {
    if (!swipePhase) {
      return;
    }
    if (state == STATE_GESTURE_COMPLETED) {
      completeGesture(swipeEventId, event);
    } else if (state == STATE_GESTURE_CANCELED) {
      cancelGesture(event);
    }
  }

  @Override
  public String getGestureName() {
    StringBuilder builder = new StringBuilder();
    builder.append(targetFingerCount).append("-Finger ");
    if (mTargetTapCount == 1) {
      builder.append("Single");
    } else if (mTargetTapCount == 2) {
      builder.append("Double");
    } else if (mTargetTapCount == 3) {
      builder.append("Triple");
    } else {
      builder.append(mTargetTapCount);
    }
    builder.append(" Tap then ");
    if (swipeFingers != targetFingerCount) {
      builder.append(swipeFingers).append("-Finger ");
    }
    return builder.append("Swipe ").append(MultiFingerSwipe.directionToString(direction)).toString();
  }
}
