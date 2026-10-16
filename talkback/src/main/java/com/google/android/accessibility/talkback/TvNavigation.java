/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.google.android.accessibility.talkback;

import android.content.Context;
import android.os.Process;
import java.util.HashSet;
import java.util.Set;

/** Specifics to control TV navigation. */
public final class TvNavigation {
  private static final long KEY_EVENT_TIMEOUT_MILLIS = 1500;

  private TvNavigation() {}

  // When the focused item can't be clicked, select goes to the app instead of doing nothing. YouTube
  // on Android TV shows only placeholder views and moves its own input focus among them, so
  // accessibility focus and input focus rarely match there.
  public static boolean letSystemHandleDpadCenterWhenFocusNotInSync(Context context) {
    return true;
  }

  public static Set<String> packagesDpadAllowlist(Context context) {
    return new HashSet<>();
  }

  // The handler thread that would call TelevisionNavigationController.shouldHandleEvent() isn't in
  // the open-source code, so with it on, the select key was always consumed and never reached apps
  // such as YouTube.
  public static boolean useHandlerThread(Context context) {
    return false;
  }

  public static int handlerThreadPriority(Context context) {
    return Process.THREAD_PRIORITY_VIDEO;
  }

  public static long keyEventTimeoutMillis(Context context) {
    return KEY_EVENT_TIMEOUT_MILLIS;
  }
}
