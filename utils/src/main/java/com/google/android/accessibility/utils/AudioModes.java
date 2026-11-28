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

package com.google.android.accessibility.utils;

import android.media.AudioManager;
import android.os.Build;

/**
 * The audio mode, kept up to date by Android's mode change callback from Android 12, so that the
 * checks made for every utterance and every change of what plays don't each ask the audio service.
 * Before Android 12, the mode is read each time.
 */
public final class AudioModes {

  private static volatile int mode = AudioManager.MODE_NORMAL;
  private static volatile boolean listening;

  private AudioModes() {}

  /** Returns the current audio mode, as {@link AudioManager#getMode()} does. */
  public static int current(AudioManager audioManager) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
      return audioManager.getMode();
    }
    if (!listening) {
      synchronized (AudioModes.class) {
        if (!listening) {
          try {
            // Registered before the mode is read, so that no change is missed in between. It stays
            // registered for the life of the process.
            audioManager.addOnModeChangedListener(Runnable::run, changed -> mode = changed);
            mode = audioManager.getMode();
            listening = true;
          } catch (RuntimeException e) {
            return audioManager.getMode();
          }
        }
      }
    }
    return mode;
  }
}
