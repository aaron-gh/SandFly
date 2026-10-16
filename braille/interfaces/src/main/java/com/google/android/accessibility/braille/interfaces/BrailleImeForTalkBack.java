/*
 * Copyright 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.braille.interfaces;

import com.google.android.accessibility.utils.input.CursorGranularity;
import java.util.OptionalInt;

/** Allows TalkBack to signal to BrailleIme. */
public interface BrailleImeForTalkBack {
  /** Callbacks when TalkBack state changes from active to suspended or to inactive. */
  void onTalkBackSuspended();

  /** Callbacks when TalkBack state changes from inactive or suspended to active. */
  void onTalkBackResumed();

  /** Returns whether braille keyboard is in full screen and touch interacting. */
  boolean isTouchInteracting();

  /** Returns the callback of BrailleIme to BrailleDisplay. */
  BrailleImeForBrailleDisplay getBrailleImeForBrailleDisplay();

  /** Callbacks when screen dimmed. */
  void onScreenDim();

  /** Callback when screen undimmed. */
  void onScreenBright();

  /** Returns whether the granularity is valid for editing. */
  boolean isGranularityValid(CursorGranularity cursorGranularity);

  /** Returns whether braille keyboard is activated. */
  boolean isBrailleKeyboardActivated();

  /**
   * Returns the braille keyboard's echo setting, a {@code TextEventFilter.KeyboardEchoType}, while
   * the user types on the on-screen braille keyboard rather than a braille display; otherwise
   * empty.
   */
  OptionalInt getOnScreenKeyboardEcho();
}
