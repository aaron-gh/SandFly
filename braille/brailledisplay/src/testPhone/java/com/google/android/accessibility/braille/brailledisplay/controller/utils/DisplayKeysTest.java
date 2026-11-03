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

package com.google.android.accessibility.braille.brailledisplay.controller.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.android.accessibility.braille.brltty.BrailleInputEvent;
import com.google.android.accessibility.braille.common.DisplayKeyOverrides;
import org.junit.Test;

public class DisplayKeysTest {

  @Test
  public void routingKeysPassCaptureAndCantBeAssigned() {
    int[] routing = {
      BrailleInputEvent.CMD_ROUTE,
      BrailleInputEvent.CMD_LONG_PRESS_ROUTE,
      BrailleInputEvent.CMD_ACTIVATE_CURRENT,
      BrailleInputEvent.CMD_LONG_PRESS_ROUTE_CURRENT,
    };
    for (int command : routing) {
      assertTrue(DisplayKeys.passesCapture(command));
      assertFalse(DisplayKeys.isAssignable(command));
      assertNull(DisplayKeys.keyOf(command, /* argument= */ 0));
    }
  }

  @Test
  public void touchAndHoldCurrentCanBeAssigned() {
    int command = BrailleInputEvent.CMD_LONG_PRESS_CURRENT;
    assertFalse(DisplayKeys.passesCapture(command));
    assertTrue(DisplayKeys.isAssignable(command));
    assertEquals(DisplayKeyOverrides.commandKey(command), DisplayKeys.keyOf(command, 0));
  }

  @Test
  public void spaceChordsAreKeysOnlyWithDots() {
    int withSpace = BrailleInputEvent.DOTS_WITH_SPACE;
    assertEquals(
        DisplayKeyOverrides.chordKey(0b101),
        DisplayKeys.keyOf(BrailleInputEvent.CMD_BRAILLE_KEY, withSpace | 0b101));
    assertNull(DisplayKeys.keyOf(BrailleInputEvent.CMD_BRAILLE_KEY, withSpace));
    assertNull(DisplayKeys.keyOf(BrailleInputEvent.CMD_BRAILLE_KEY, 0b101));
  }
}
