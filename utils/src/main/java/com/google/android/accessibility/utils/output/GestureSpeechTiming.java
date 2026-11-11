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

package com.google.android.accessibility.utils.output;

import android.os.SystemClock;
import androidx.annotation.Nullable;
import com.google.android.libraries.accessibility.utils.log.LogUtils;

/**
 * Logs, in the Debug log, how long after a gesture its speech started, one line per gesture, for
 * measuring how quickly swipes are answered.
 */
public final class GestureSpeechTiming {

  private static final String TAG = "GestureSpeechTiming";

  /** Speech later than this after a gesture isn't counted as the gesture's. */
  private static final long MAX_MS = 3000;

  private static volatile long gestureUptime;
  private static volatile @Nullable String gestureAction;

  private GestureSpeechTiming() {}

  /** Records that a gesture running {@code action} was recognized now. */
  public static void onGesture(String action) {
    gestureAction = action;
    gestureUptime = SystemClock.uptimeMillis();
  }

  /** Logs the time from the last gesture, once, when its speech starts. */
  public static void onSpeechStarted() {
    long start = gestureUptime;
    if (start == 0) {
      return;
    }
    gestureUptime = 0;
    long elapsed = SystemClock.uptimeMillis() - start;
    if (elapsed <= MAX_MS && LogUtils.shouldLog(android.util.Log.DEBUG)) {
      LogUtils.d(TAG, "%s: speech %d ms after the gesture", gestureAction, elapsed);
    }
  }
}
