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

package com.google.android.accessibility.braille.brailledisplay.platform.external;

import android.os.RemoteException;
import com.google.android.accessibility.braille.brailledisplay.BrailleDisplayLog;
import com.google.android.accessibility.braille.brailledisplay.platform.DisplayerInterface;
import com.google.android.accessibility.braille.brltty.BrailleDisplayProperties;
import com.google.android.accessibility.braille.brltty.BrailleKeyBinding;
import fyi.quin.backtalk.braille.IBrailleDisplay;
import java.util.Collections;

/**
 * A braille display that another app provides. Its app decodes its keys, so there are no key
 * bindings to report, and the cells it shows go back to the app.
 */
final class ExternalDisplayer implements DisplayerInterface {
  private static final String TAG = "ExternalDisplayer";

  private final BrailleDisplayProperties properties;
  private final IBrailleDisplay display;

  ExternalDisplayer(String name, int cellCount, IBrailleDisplay display) {
    this.properties =
        new BrailleDisplayProperties(
            name,
            cellCount,
            /* numStatusCells= */ 0,
            new BrailleKeyBinding[0],
            Collections.emptyMap());
    this.display = display;
  }

  @Override
  public BrailleDisplayProperties getDeviceProperties() {
    return properties;
  }

  @Override
  public boolean isDisplayReady() {
    return true;
  }

  @Override
  public void writeBrailleDots(byte[] brailleDotBytes) {
    if (properties.getNumTextCells() == 0) {
      return;
    }
    try {
      display.showCells(brailleDotBytes);
    } catch (RemoteException e) {
      BrailleDisplayLog.w(TAG, "couldn't show cells", e);
    }
  }
}
