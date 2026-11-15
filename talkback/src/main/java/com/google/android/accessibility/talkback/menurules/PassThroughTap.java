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

package com.google.android.accessibility.talkback.menurules;

import static com.google.android.accessibility.talkback.Feedback.PassThroughMode.Action.LOCK_PASS_THROUGH;
import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Region;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.ViewConfiguration;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.TalkBackService;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Taps the screen as a finger would, through the pass-through region, for app controls. */
final class PassThroughTap {

  /** How long Android takes to apply the pass-through region. */
  private static final long PASS_THROUGH_WAIT_MS = 350;

  private final Pipeline.FeedbackReturner pipeline;
  private final Handler handler;

  PassThroughTap(Pipeline.FeedbackReturner pipeline, Handler handler) {
    this.pipeline = pipeline;
    this.handler = handler;
  }

  /** Taps the screen at ([x], [y]). Returns whether the tap was sent. */
  boolean tap(int x, int y) {
    @Nullable AccessibilityService service = TalkBackService.getInstance();
    if (service == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
      return false;
    }
    Path path = new Path();
    path.moveTo(x, y);
    GestureDescription gesture =
        new GestureDescription.Builder()
            .addStroke(
                new GestureDescription.StrokeDescription(
                    path, /* startTime= */ 0, ViewConfiguration.getTapTimeout()))
            .build();
    DisplayMetrics metrics = service.getResources().getDisplayMetrics();
    pipeline.returnFeedback(
        EVENT_ID_UNTRACKED,
        Feedback.passThroughMode(
            LOCK_PASS_THROUGH, new Region(0, 0, metrics.widthPixels, metrics.heightPixels)));
    AccessibilityService.GestureResultCallback ended =
        new AccessibilityService.GestureResultCallback() {
          @Override
          public void onCompleted(GestureDescription description) {
            release();
          }

          @Override
          public void onCancelled(GestureDescription description) {
            release();
          }
        };
    handler.postDelayed(
        () -> {
          if (!service.dispatchGesture(gesture, ended, handler)) {
            release();
          }
        },
        PASS_THROUGH_WAIT_MS);
    return true;
  }

  private void release() {
    pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.passThroughMode(LOCK_PASS_THROUGH, null));
  }
}
