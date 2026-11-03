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

package com.google.android.accessibility.braille.brailledisplay.controller;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import com.google.android.accessibility.braille.brailledisplay.controller.utils.DisplayKeys;
import com.google.android.accessibility.braille.brltty.BrailleInputEvent;

/**
 * Hands the keys pressed on the braille display to the settings screen that assigns them, instead
 * of running their commands. Panning and routing keys keep working.
 */
@MainThread
public final class DisplayKeyCapture {

  /** Receives the captured keys. */
  public interface Listener {
    /**
     * Called with the {@link com.google.android.accessibility.braille.common.DisplayKeyOverrides}
     * key that was pressed, or null when those keys can't be assigned.
     */
    void onKey(@Nullable String key);
  }

  @Nullable private static Listener listener;
  private static boolean displayActive;

  private DisplayKeyCapture() {}

  /** Returns whether a braille display is connected and sends its keys to the controller. */
  public static boolean isAvailable() {
    return displayActive;
  }

  /** Sends the keys pressed from now on to {@code newListener}. */
  public static void start(Listener newListener) {
    listener = newListener;
  }

  /** Stops sending keys to {@code oldListener}. */
  public static void stop(Listener oldListener) {
    if (listener == oldListener) {
      listener = null;
    }
  }

  static void setDisplayActive(boolean active) {
    displayActive = active;
  }

  /** Returns whether the event went to the listener. */
  static boolean offer(BrailleInputEvent event) {
    Listener current = listener;
    if (current == null || DisplayKeys.passesCapture(event.getCommand())) {
      return false;
    }
    current.onKey(DisplayKeys.keyOf(event.getCommand(), event.getArgument()));
    return true;
  }
}
