/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.utils.monitor;

import android.os.SystemClock;

/**
 * When the user last did something: a touch, a gesture, a key, or input on the braille keyboard or
 * a braille display. Speech that soon after answers what the user did, such as the home screen
 * after the gesture that goes home, even when the app reports it rather than Backtalk.
 */
public final class RecentUserInput {
  /**
   * How long speech counts as answering the user. Long enough for a new screen to open and be
   * announced, short enough that a notification a while later doesn't count.
   */
  private static final long WINDOW_MS = 2000;

  private static volatile long lastInputUptimeMs = -WINDOW_MS;

  private RecentUserInput() {}

  /** Records that the user just did something. */
  public static void mark() {
    lastInputUptimeMs = SystemClock.uptimeMillis();
  }

  /** Whether the user did something within the last couple of seconds. */
  public static boolean isRecent() {
    return SystemClock.uptimeMillis() - lastInputUptimeMs < WINDOW_MS;
  }
}
