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

package com.google.android.accessibility.talkback.selector;

import android.content.Context;
import com.google.android.accessibility.braille.common.BrailleUserPreferences;

/** Braille keyboard settings that reading controls can change. */
final class BrailleKeyboardSettings {

  private BrailleKeyboardSettings() {}

  /** Turns "Tablet held up faces away" on or off, and returns its new value. */
  static boolean toggleTabletHeldUpFacesAway(Context context) {
    boolean facesAway = !BrailleUserPreferences.readTabletHeldUpFacesAway(context);
    BrailleUserPreferences.writeTabletHeldUpFacesAway(context, facesAway);
    return facesAway;
  }
}
