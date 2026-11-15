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
import java.util.concurrent.atomic.AtomicLong;

/**
 * When the user last did something: a touch, a gesture, a key, input on the braille keyboard or a
 * braille display, or a voice command. Speech that soon after answers what the user did, such as the home screen
 * after the gesture that goes home, even when the app reports it rather than Backtalk.
 */
public final class RecentUserInput {
  /**
   * How long speech counts as answering the user. Long enough for a new screen to open and be
   * announced, short enough that a notification a while later doesn't count.
   */
  private static final long WINDOW_MS = 2000;

  private static volatile long lastInputUptimeMs = -WINDOW_MS;
  private static final AtomicLong inputSequence = new AtomicLong();

  private RecentUserInput() {}

  /** Records that the user just did something. */
  public static void mark() {
    inputSequence.incrementAndGet();
    lastInputUptimeMs = SystemClock.uptimeMillis();
  }

  /** Changes for every user action, including actions in the same uptime millisecond. */
  public static long inputSequence() {
    return inputSequence.get();
  }

  /**
   * Whether the user did something within the last couple of seconds, after {@code uptimeMs}, a
   * {@link SystemClock#uptimeMillis()} time.
   */
  public static boolean isRecentAfter(long uptimeMs) {
    return isRecent() && uptimeMs < lastInputUptimeMs;
  }

  /** When the user last did something, in {@link SystemClock#uptimeMillis()} time. */
  public static long lastInputUptimeMs() {
    return lastInputUptimeMs;
  }

  /** Whether the user did something within the last couple of seconds. */
  public static boolean isRecent() {
    return SystemClock.uptimeMillis() - lastInputUptimeMs < WINDOW_MS;
  }
}
