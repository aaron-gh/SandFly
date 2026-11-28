/*
 * Copyright (C) 2020 Google Inc.
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

package com.google.android.accessibility.talkback.actor;

import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD;
import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD;
import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS;
import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor;
import com.google.android.accessibility.utils.AccessibilityEventListener;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.Role;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This class supports to notify user while manipulating slider(SeekBar). When the current value is
 * already maximum and user tries to increase it, or the value is minimum and user tries to decrease
 * it, user will get notify about the status.
 *
 * <p>With Slider step set to one of SandFly's steps, a slider that takes ACTION_SET_PROGRESS is set
 * to the next value of that step (see {@link SliderSteps}). With Skip steps that don't change the
 * value on, a step after which the spoken value is the same is followed by another, once the app
 * reports the step: on an event from the slider, or when {@link #STEP_WAIT_MS} has passed. This
 * stops when the value changes, at the end of the range, after {@link SliderSteps#skipLimit} steps
 * or {@link #MAX_SKIP_MS}, when the app doesn't take a step, and on a touch, a gesture, a focus
 * change, a new window, the next adjustment and when the service stops.
 */
public class NumberAdjustor implements AccessibilityEventListener {
  private static final String TAG = "NumberAdjustor";

  /** How long the app has to report a step before SandFly reads the slider again. */
  private static final long STEP_WAIT_MS = 300;

  /** The longest that skipping steps goes on after the adjustment. */
  private static final long MAX_SKIP_MS = 10_000;

  private static final int SKIP_EVENT_TYPES =
      AccessibilityEvent.TYPE_VIEW_SELECTED
          | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
          | AccessibilityEvent.TYPE_VIEW_SCROLLED
          | AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
          | AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
          | AccessibilityEvent.TYPE_TOUCH_INTERACTION_START
          | AccessibilityEvent.TYPE_GESTURE_DETECTION_START;

  private final Context context;
  private Pipeline.FeedbackReturner pipeline;
  private final AccessibilityFocusMonitor accessibilityFocusMonitor;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable stepWait = () -> checkSkip(/* waited= */ true);

  /** The adjustment whose steps are being skipped, or null. Used on the main thread only. */
  private @Nullable Skip skip;

  /** An adjustment that goes on while its steps don't change the spoken value. */
  private static final class Skip {
    final AccessibilityNodeInfoCompat node;
    final boolean increase;
    final String mode;
    final String valueBefore;
    final long until;
    float currentBeforeStep;
    int stepsLeft;

    Skip(
        AccessibilityNodeInfoCompat node,
        boolean increase,
        String mode,
        String valueBefore,
        float currentBeforeStep,
        int stepsLeft,
        long until) {
      this.node = node;
      this.increase = increase;
      this.mode = mode;
      this.valueBefore = valueBefore;
      this.currentBeforeStep = currentBeforeStep;
      this.stepsLeft = stepsLeft;
      this.until = until;
    }
  }

  public NumberAdjustor(Context context, AccessibilityFocusMonitor accessibilityFocusMonitor) {
    this.context = context;
    this.accessibilityFocusMonitor = accessibilityFocusMonitor;
  }

  public void setPipeline(Pipeline.FeedbackReturner pipeline) {
    this.pipeline = pipeline;
  }

  /**
   * Check whether the focused node is Role.ROLE_SEEK_CONTROL, and the current value has reached its
   * upper/lower bound.
   *
   * @return true when the value adjusting succeeds. Return false otherwise.
   */
  public boolean adjustValue(boolean decrease) {
    stopSkipping();
    @Nullable AccessibilityNodeInfoCompat node =
        accessibilityFocusMonitor.getSupportedAdjustableNode();
    if (node == null) {
      return false;
    }
    if (Role.getRole(node) == Role.ROLE_SEEK_CONTROL) {
      final AccessibilityNodeInfoCompat.RangeInfoCompat rangeInfo = node.getRangeInfo();
      if (rangeInfo != null) {
        CharSequence stateDescription = node.getStateDescription();
        if (decrease && (rangeInfo.getCurrent() <= rangeInfo.getMin())) {
          // Notify user it reaches lower bound: 0%
          pipeline.returnFeedback(
              EVENT_ID_UNTRACKED,
              Feedback.speech(
                  TextUtils.isEmpty(stateDescription)
                      ? context.getString(R.string.template_seekbar_range, 0)
                      : stateDescription,
                  SpeakOptions.create()));
          return false;
        } else if (!decrease && (rangeInfo.getCurrent() >= rangeInfo.getMax())) {
          // Notify user it reaches upper bound: 100%
          pipeline.returnFeedback(
              EVENT_ID_UNTRACKED,
              Feedback.speech(
                  TextUtils.isEmpty(stateDescription)
                      ? context.getString(R.string.template_seekbar_range, 100)
                      : stateDescription,
                  SpeakOptions.create()));
          return false;
        }
      }
    }
    if (Role.getRole(node) == Role.ROLE_SEEK_CONTROL) {
      SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
      String mode = stepMode(context, prefs);
      boolean skipUnchanged = skipsUnchangedSteps(context, prefs);
      if (SliderSteps.isOwnStep(mode) || skipUnchanged) {
        return adjustSlider(node, /* increase= */ !decrease, mode, skipUnchanged);
      }
    }
    if (decrease) {
      if (AccessibilityNodeInfoUtils.supportsAction(node, ACTION_SCROLL_BACKWARD.getId())) {
        pipeline.returnFeedback(
            EVENT_ID_UNTRACKED, Feedback.nodeAction(node, ACTION_SCROLL_BACKWARD.getId()));
        return true;
      }
    } else {
      if (AccessibilityNodeInfoUtils.supportsAction(node, ACTION_SCROLL_FORWARD.getId())) {
        pipeline.returnFeedback(
            EVENT_ID_UNTRACKED, Feedback.nodeAction(node, ACTION_SCROLL_FORWARD.getId()));
        return true;
      }
    }
    LogUtils.d(TAG, "adjustValue does not happen");
    return false;
  }

  /** Slider step: one of the {@link SliderSteps} modes. */
  private static String stepMode(Context context, SharedPreferences prefs) {
    return prefs.getString(
        context.getString(R.string.pref_slider_step_key),
        context.getString(R.string.pref_slider_step_default));
  }

  private static boolean skipsUnchangedSteps(Context context, SharedPreferences prefs) {
    return SharedPreferencesUtils.getBooleanPref(
        prefs,
        context.getResources(),
        R.string.pref_slider_skip_unchanged_key,
        R.bool.pref_slider_skip_unchanged_default);
  }

  /**
   * Moves the slider one step with Slider step, and with Skip steps that don't change the value on,
   * starts skipping. Returns whether a step was taken.
   */
  private boolean adjustSlider(
      AccessibilityNodeInfoCompat node, boolean increase, String mode, boolean skipUnchanged) {
    AccessibilityNodeInfoCompat.@Nullable RangeInfoCompat range = node.getRangeInfo();
    String valueBefore = spokenValue(node);
    float currentBefore = range == null ? Float.NaN : range.getCurrent();
    if (!step(node, increase, mode)) {
      LogUtils.d(TAG, "adjustValue does not happen");
      return false;
    }
    if (skipUnchanged
        && range != null
        && SliderSteps.isValidRange(
            range.getType(), range.getMin(), range.getMax(), range.getCurrent())) {
      int limit = SliderSteps.skipLimit(mode, range.getType(), range.getMin(), range.getMax());
      skip =
          new Skip(
              node,
              increase,
              mode,
              valueBefore,
              currentBefore,
              limit - 1,
              SystemClock.uptimeMillis() + MAX_SKIP_MS);
      handler.postDelayed(stepWait, STEP_WAIT_MS);
    }
    return true;
  }

  /**
   * Moves the slider one step: to the value of SandFly's step with ACTION_SET_PROGRESS where {@code
   * mode} has one and the slider takes it, otherwise with the app's step. Returns whether an action
   * was sent.
   */
  private boolean step(AccessibilityNodeInfoCompat node, boolean increase, String mode) {
    AccessibilityNodeInfoCompat.@Nullable RangeInfoCompat range = node.getRangeInfo();
    if (range != null
        && SliderSteps.isOwnStep(mode)
        && AccessibilityNodeInfoUtils.supportsAction(node, ACTION_SET_PROGRESS.getId())) {
      @Nullable Float target =
          SliderSteps.next(
              mode, range.getType(), range.getMin(), range.getMax(), range.getCurrent(), increase);
      if (target != null) {
        Bundle args = new Bundle();
        args.putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, target);
        if (pipeline.returnFeedback(
            EVENT_ID_UNTRACKED, Feedback.nodeAction(node, ACTION_SET_PROGRESS.getId(), args))) {
          return true;
        }
        // Such as a slider that keeps to steps of its own and stays on the nearest one.
        LogUtils.d(TAG, "The slider didn't take %s; using its own step", target);
      }
    }
    int action = increase ? ACTION_SCROLL_FORWARD.getId() : ACTION_SCROLL_BACKWARD.getId();
    if (!AccessibilityNodeInfoUtils.supportsAction(node, action)) {
      return false;
    }
    pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.nodeAction(node, action));
    return true;
  }

  private static String spokenValue(AccessibilityNodeInfoCompat node) {
    AccessibilityNodeInfoCompat.@Nullable RangeInfoCompat range = node.getRangeInfo();
    return SliderSteps.spokenValue(
        node.getStateDescription(), node.getText(), range == null ? Float.NaN : range.getCurrent());
  }

  @Override
  public int getEventTypes() {
    return SKIP_EVENT_TYPES;
  }

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event, EventId eventId) {
    @Nullable Skip current = skip;
    if (current == null) {
      return;
    }
    switch (event.getEventType()) {
      case AccessibilityEvent.TYPE_TOUCH_INTERACTION_START,
          AccessibilityEvent.TYPE_GESTURE_DETECTION_START,
          AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> stopSkipping();
      case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED -> {
        if (!isFromNode(event, current.node)) {
          stopSkipping();
        }
      }
      default -> {
        if (isFromNode(event, current.node)) {
          checkSkip(/* waited= */ false);
        }
      }
    }
  }

  private static boolean isFromNode(AccessibilityEvent event, AccessibilityNodeInfoCompat node) {
    if (event.getWindowId() != node.getWindowId()
        || !TextUtils.equals(event.getPackageName(), node.getPackageName())) {
      return false;
    }
    @Nullable AccessibilityNodeInfo source = event.getSource();
    return source != null && source.equals(node.unwrap());
  }

  /**
   * Reads the slider again after a step. Once the app has taken the step, which changes the current
   * value of the range, skipping stops if the spoken value changed and otherwise takes the next step.
   * Until then it waits, for {@link #STEP_WAIT_MS} at most: {@code waited} is true when that time
   * has passed, and skipping stops if the app hasn't taken the step.
   */
  private void checkSkip(boolean waited) {
    @Nullable Skip current = skip;
    if (current == null) {
      return;
    }
    AccessibilityNodeInfoCompat node = current.node;
    if (!node.refresh() || SystemClock.uptimeMillis() > current.until) {
      stopSkipping();
      return;
    }
    AccessibilityNodeInfoCompat.@Nullable RangeInfoCompat range = node.getRangeInfo();
    if (range == null
        || !SliderSteps.isValidRange(
            range.getType(), range.getMin(), range.getMax(), range.getCurrent())) {
      stopSkipping();
      return;
    }
    if (range.getCurrent() == current.currentBeforeStep) {
      // The app hasn't taken the step yet, or didn't take it.
      if (waited) {
        stopSkipping();
      }
      return;
    }
    if (!current.valueBefore.equals(spokenValue(node))
        || current.stepsLeft <= 0
        || SliderSteps.isAtEnd(
            range.getMin(), range.getMax(), range.getCurrent(), current.increase)) {
      stopSkipping();
      return;
    }
    handler.removeCallbacks(stepWait);
    current.stepsLeft--;
    current.currentBeforeStep = range.getCurrent();
    if (!step(node, current.increase, current.mode)) {
      stopSkipping();
      return;
    }
    LogUtils.v(TAG, "The value didn't change; another step, %d left", current.stepsLeft);
    handler.postDelayed(stepWait, STEP_WAIT_MS);
  }

  /** Stops skipping steps that don't change the value. */
  public void stopSkipping() {
    handler.removeCallbacks(stepWait);
    skip = null;
  }
}
