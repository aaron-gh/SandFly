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

import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.Region;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Display;
import androidx.annotation.Nullable;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.libraries.accessibility.utils.log.LogUtils;

/**
 * This class support the enable/disable Touch Explore Pass Through mode. Once the system enters the
 * pass-through mode {@link #setTouchExplorePassThrough(boolean)}, a guard timer is set on the main
 * thread {@link #startPassThroughGuardTimer()}. If no user touch activity before the timer expires,
 * system automatically exits the pass-through mode. On the other hand, when touch activity
 * detected, it needs to abort the timer {@link #cancelPassThroughGuardTimer()} to prevent the early
 * exit from the pass-through mode as well as the Earcon.
 */
public class PassThroughModeActor {
  private static final String TAG = "PassThroughModeActor";

  // The maximum time of Pass-Through mode held from pass-through gesture detected till the next
  // touch interaction start event.
  private static final long PASS_THROUGH_IDLE_MS = 4000; // 4 seconds
  private boolean touchExplorePassThroughActive;
  private Pipeline.FeedbackReturner pipeline;
  private AccessibilityService service;
  private boolean locked;
  // The region direct touch wants. Used whenever neither the gesture nor the braille lock holds it.
  private Region directTouchRegion = new Region();
  private PassThroughModeDialog passThroughDialog;
  // The exit runs on the main thread, like everything else here, not on a thread of its own.
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable passThroughExit =
      () -> {
        // While in locked mode, we can ignore the time out event
        if (!locked) {
          setTouchExplorePassThrough(false);
        }
      };

  ////////////////////////////////////////////////////////////////////////////////////////////////
  // Inner class for actor-state reader

  /** Read-only interface for actor-state data. */
  public class State {
    public boolean isPassThroughModeActive() {
      return PassThroughModeActor.this.touchExplorePassThroughActive;
    }
  }

  public final PassThroughModeActor.State state = new PassThroughModeActor.State();

  public PassThroughModeActor(AccessibilityService service) {
    this.service = service;
    passThroughDialog = new PassThroughModeDialog(service);
  }

  public void onDestroy() {
    cancelPassThroughGuardTimer();
  }

  public void setPipeline(Pipeline.FeedbackReturner pipeline) {
    this.pipeline = pipeline;
    passThroughDialog.setPipeline(pipeline);
  }

  /**
   * Enables/disables the touch-explore pass-through.
   *
   * <p>This is a no-op if sdk version is below R.
   *
   * @param enable {@code true} if the touch-explore pass-through should be enabled, {@code false}
   *     otherwise.
   */
  public void setTouchExplorePassThrough(boolean enable) {
    if (FeatureSupport.supportPassthrough() && !locked) {
      if (enable) {
        service.setTouchExplorationPassthroughRegion(
            Display.DEFAULT_DISPLAY, getRegionOfFullScreen(service));
        startPassThroughGuardTimer();
        LogUtils.v(TAG, "Enter touch explore pass-through mode.");
        pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.sound(R.raw.chime_up));
      } else {
        // An exit left pending would set the region again later, over whichever holds it by then,
        // such as the circle menu's.
        cancelPassThroughGuardTimer();
        service.setTouchExplorationPassthroughRegion(Display.DEFAULT_DISPLAY, directTouchRegion);
        if (touchExplorePassThroughActive) {
          LogUtils.v(TAG, "Leave touch explore pass-through mode.");
          pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.sound(R.raw.chime_down));
        }
      }
      touchExplorePassThroughActive = enable;
    }
  }

  /**
   * Sets the region direct touch wants, or clears it with null. The pass-through gesture and the
   * braille lock take priority, and direct touch's region comes back when they let go.
   */
  public void setDirectTouchRegion(@Nullable Region region) {
    directTouchRegion = region == null ? new Region() : new Region(region);
    if (FeatureSupport.supportPassthrough() && !locked && !touchExplorePassThroughActive) {
      service.setTouchExplorationPassthroughRegion(Display.DEFAULT_DISPLAY, directTouchRegion);
    }
  }

  public void showEducationDialog() {
    // The pass-through shortcut can be assigned to any gestures(including 1-finger).
    if (FeatureSupport.supportPassthrough() && !locked) {
      if (passThroughDialog.getShouldShowDialogPref()) {
        LogUtils.v(TAG, "Enter touch explore pass-through confirm dialog.");
        passThroughDialog.showDialog();
      } else {
        setTouchExplorePassThrough(/* enable= */ true);
      }
    }
  }

  /** Force enter the pass-through lock mode without the interrupt from touch-interaction. */
  public void lockTouchExplorePassThrough(Region region) {
    if (FeatureSupport.supportPassthrough()) {
      if (region != null && !region.isEmpty()) {
        cancelPassThroughGuardTimer();
        locked = true;
        touchExplorePassThroughActive = true;
        service.setTouchExplorationPassthroughRegion(Display.DEFAULT_DISPLAY, region);
        LogUtils.v(TAG, "Enter touch explore pass-through lock mode.");
      } else {
        service.setTouchExplorationPassthroughRegion(Display.DEFAULT_DISPLAY, directTouchRegion);
        locked = false;
        if (touchExplorePassThroughActive) {
          touchExplorePassThroughActive = false;
          LogUtils.v(TAG, "Leave touch explore pass-through lock mode.");
        }
      }
    }
  }

  /**
   * The 4-second guard time is used when no touch activity after system enters pass-through mode.
   * Timer expires triggering the exit of pass-through mode. On the other hand, when any touch
   * activity detected, the timer should be killed.
   */
  public void cancelPassThroughGuardTimer() {
    handler.removeCallbacks(passThroughExit);
  }

  private void startPassThroughGuardTimer() {
    // Entering the mode again starts the time over.
    handler.removeCallbacks(passThroughExit);
    handler.postDelayed(passThroughExit, PASS_THROUGH_IDLE_MS);
  }

  private Region getRegionOfFullScreen(Context context) {
    final DisplayMetrics metrics = new DisplayMetrics();
    final Display display;
    DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
    display = dm.getDisplay(Display.DEFAULT_DISPLAY);
    if (display == null) {
      return new Region();
    } else {
      display.getRealMetrics(metrics);
      return new Region(0, 0, metrics.widthPixels, metrics.heightPixels);
    }
  }
}
