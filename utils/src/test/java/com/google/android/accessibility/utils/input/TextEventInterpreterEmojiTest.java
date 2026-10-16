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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;
import com.google.android.accessibility.utils.output.EmojiSpeech;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Moving by character over emoji. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TextEventInterpreterEmojiTest {
  private static final String TEXT = "a👍🏽b👨‍👩‍👧";

  private Context context;

  @Before
  public void setUp() {
    context = RuntimeEnvironment.getApplication();
  }

  @After
  public void tearDown() {
    EmojiSpeech.setMode(context, EmojiSpeech.MODE_ENGINE);
  }

  /** Returns what moving by character over {@code start} to {@code end} says, or null. */
  private static String spoken(Context context, int start, int end, boolean forward) {
    int[] range = TextEventInterpreter.characterRange(context, TEXT, start, end, forward);
    return range == null ? null : TEXT.substring(range[0], range[1]);
  }

  @Test
  public void wholeEmojiWhenBacktalkSpeaksThem() {
    for (String mode : new String[] {EmojiSpeech.MODE_NAMES, EmojiSpeech.MODE_NONE}) {
      EmojiSpeech.setMode(context, mode);
      // An app that moves by whole emoji, as text views do.
      assertEquals("a", spoken(context, 0, 1, true));
      assertEquals("👍🏽", spoken(context, 1, 5, true));
      assertEquals("👍🏽", spoken(context, 1, 5, false));
      assertEquals("b", spoken(context, 5, 6, true));
      assertEquals("👨‍👩‍👧", spoken(context, 6, 14, false));
    }
  }

  @Test
  public void appsMovingByPartsHearTheEmojiOnce() {
    EmojiSpeech.setMode(context, EmojiSpeech.MODE_NAMES);
    // Chrome moves by code point: forward into the thumb, then its skin tone.
    assertEquals("👍🏽", spoken(context, 1, 3, true));
    assertNull(spoken(context, 3, 5, true));
    // Backward into the family's last person, then the joiner and the rest.
    assertEquals("👨‍👩‍👧", spoken(context, 12, 14, false));
    assertNull(spoken(context, 11, 12, false));
    assertNull(spoken(context, 6, 8, false));
  }

  @Test
  public void oneCharacterWhenTheEngineSpeaksThem() {
    EmojiSpeech.setMode(context, EmojiSpeech.MODE_ENGINE);
    assertEquals("a", spoken(context, 0, 1, true));
    assertEquals("\uD83D", spoken(context, 1, 5, true));
  }
}
