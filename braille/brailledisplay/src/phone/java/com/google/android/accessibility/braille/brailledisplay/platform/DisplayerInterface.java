/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.braille.brailledisplay.platform;

import com.google.android.accessibility.braille.brltty.BrailleDisplayProperties;

/**
 * The display that a {@link Controller} drives. {@link Displayer} implements it for displays
 * connected through BRLTTY. A display that delivers its input already decoded, through {@link
 * Controller#onBrailleInputEvent}, can implement it without BRLTTY.
 */
public interface DisplayerInterface {

  /** The display's properties, including its number of cells. */
  BrailleDisplayProperties getDeviceProperties();

  /** Whether the display is ready to show braille. */
  boolean isDisplayReady();

  /** Shows the given dot patterns on the display's cells. */
  void writeBrailleDots(byte[] brailleDotBytes);
}
