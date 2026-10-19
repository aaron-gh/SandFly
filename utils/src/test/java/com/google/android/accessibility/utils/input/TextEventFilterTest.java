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

package com.google.android.accessibility.utils.input;

import static com.google.android.accessibility.utils.input.TextEventFilter.PREF_ECHO_CHARACTERS;
import static com.google.android.accessibility.utils.input.TextEventFilter.PREF_ECHO_CHARACTERS_AND_WORDS;
import static com.google.android.accessibility.utils.input.TextEventFilter.PREF_ECHO_NONE;
import static com.google.android.accessibility.utils.input.TextEventFilter.PREF_ECHO_WORDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.android.accessibility.utils.input.TextEventFilter.TypingInput;
import org.junit.Test;

public class TextEventFilterTest {
  private static final long EVENT_TIME = 10_000;

  private final TextEventFilter filter =
      new TextEventFilter(/* context= */ null, /* textCursorTracker= */ null, new TextEventHistory());

  @Test
  public void onScreenKeyboardEchoAppliesWithoutBrailleInput() {
    filter.setKeyboardEcho(TypingInput.ON_SCREEN_KEYBOARD, PREF_ECHO_WORDS);

    assertEquals(TypingInput.ON_SCREEN_KEYBOARD, filter.getTypingInput(EVENT_TIME));
    assertFalse(filter.shouldEchoAddedText(EVENT_TIME));
    assertTrue(filter.shouldEchoInitialWords(EVENT_TIME));
  }

  @Test
  public void onScreenKeyboardEchoAppliesWhenNoBrailleInputIsTyping() {
    filter.setKeyboardEcho(TypingInput.ON_SCREEN_KEYBOARD, PREF_ECHO_CHARACTERS);
    filter.setBrailleInputReader(() -> null);

    assertTrue(filter.shouldEchoAddedText(EVENT_TIME));
    assertFalse(filter.shouldEchoInitialWords(EVENT_TIME));
  }

  @Test
  public void brailleKeyboardHasItsOwnEcho() {
    filter.setKeyboardEcho(TypingInput.ON_SCREEN_KEYBOARD, PREF_ECHO_NONE);
    filter.setKeyboardEcho(TypingInput.BRAILLE_KEYBOARD, PREF_ECHO_WORDS);
    filter.setBrailleInputReader(() -> TypingInput.BRAILLE_KEYBOARD);

    assertFalse(filter.shouldEchoAddedText(EVENT_TIME));
    assertTrue(filter.shouldEchoInitialWords(EVENT_TIME));
  }

  @Test
  public void brailleDisplayHasItsOwnEcho() {
    filter.setKeyboardEcho(TypingInput.BRAILLE_KEYBOARD, PREF_ECHO_NONE);
    filter.setKeyboardEcho(TypingInput.BRAILLE_DISPLAY, PREF_ECHO_CHARACTERS_AND_WORDS);
    filter.setBrailleInputReader(() -> TypingInput.BRAILLE_DISPLAY);

    assertTrue(filter.shouldEchoAddedText(EVENT_TIME));
    assertTrue(filter.shouldEchoInitialWords(EVENT_TIME));
  }

  @Test
  public void physicalKeyboardEchoAppliesToKeyPresses() {
    filter.setKeyboardEcho(TypingInput.PHYSICAL_KEYBOARD, PREF_ECHO_NONE);
    filter.setKeyboardEcho(TypingInput.BRAILLE_KEYBOARD, PREF_ECHO_CHARACTERS_AND_WORDS);
    filter.setBrailleInputReader(() -> TypingInput.BRAILLE_KEYBOARD);
    filter.setLastKeyEventTime(EVENT_TIME - 10);

    assertEquals(TypingInput.PHYSICAL_KEYBOARD, filter.getTypingInput(EVENT_TIME));
    assertFalse(filter.shouldEchoAddedText(EVENT_TIME));
    assertFalse(filter.shouldEchoInitialWords(EVENT_TIME));
  }
}
