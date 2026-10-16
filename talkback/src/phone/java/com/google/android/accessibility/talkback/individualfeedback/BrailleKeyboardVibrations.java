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

package com.google.android.accessibility.talkback.individualfeedback;

import android.content.Context;
import com.google.android.accessibility.brailleime.BrailleImeVibrator;

/** The braille keyboard's vibrations, for Individual sounds and vibrations. */
final class BrailleKeyboardVibrations {

  private BrailleKeyboardVibrations() {}

  /** Whether there is a braille keyboard whose vibrations can be turned off. */
  static boolean isAvailable() {
    return true;
  }

  /** Plays the braille keyboard's own vibration named {@code name}, as themes name it. */
  static boolean preview(Context context, String name) {
    return BrailleImeVibrator.getInstance(context).preview(name);
  }
}
