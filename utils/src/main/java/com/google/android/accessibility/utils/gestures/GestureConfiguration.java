/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.view.ViewConfiguration;

/** This class contains timeout values that control the behavior of gesture detection. */
public final class GestureConfiguration {

  /**
   * The maximum number of milliseconds that can be between two taps for them to count as a
   * multi-tap gesture.
   */
  public static final int DOUBLE_TAP_TIMEOUT_MS = ViewConfiguration.getDoubleTapTimeout() - 50;

  // A gesture waits a bit longer than the tap gap before it completes, so a late tap still counts.
  private static final int MULTI_TAP_COMPLETION_MARGIN_MS =
      ViewConfiguration.getDoubleTapTimeout() - DOUBLE_TAP_TIMEOUT_MS;

  private static volatile int multiTapTimeoutMs = DOUBLE_TAP_TIMEOUT_MS;

  private GestureConfiguration() {}

  /** Returns the user-set maximum number of milliseconds between two taps of a multi-tap gesture. */
  public static int getMultiTapTimeoutMs() {
    return multiTapTimeoutMs;
  }

  /** Returns how long a tap gesture waits for another tap before it completes. */
  public static int getMultiTapCompletionTimeoutMs() {
    return multiTapTimeoutMs + MULTI_TAP_COMPLETION_MARGIN_MS;
  }

  /**
   * Returns the maximum number of milliseconds between two taps of a multi-finger multi-tap gesture:
   * the user-set time, but never shorter than the default, as lifting and placing several fingers
   * takes longer than one.
   */
  public static int getMultiFingerTapTimeoutMs() {
    return Math.max(multiTapTimeoutMs, DOUBLE_TAP_TIMEOUT_MS);
  }

  /** Returns how long a multi-finger tap gesture waits for another tap before it completes. */
  public static int getMultiFingerTapCompletionTimeoutMs() {
    return getMultiFingerTapTimeoutMs() + MULTI_TAP_COMPLETION_MARGIN_MS;
  }

  public static void setMultiTapTimeoutMs(int timeoutMs) {
    multiTapTimeoutMs = timeoutMs;
  }
}
