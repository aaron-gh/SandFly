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

import android.content.Context;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.utils.input.GranularityIterator.TextSegmentIterator;
import com.google.android.accessibility.utils.output.EmojiSpeech;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Moving by word over emoji. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class GranularityIteratorEmojiTest {
  private static final String TEXT = "hi 👍🏽 there, 👨‍👩‍👧 😀😀 ok";

  private Context context;

  @Before
  public void setUp() {
    context = RuntimeEnvironment.getApplication();
  }

  @After
  public void tearDown() {
    EmojiSpeech.setMode(context, EmojiSpeech.MODE_ENGINE);
  }

  private static List<String> wordsForward() {
    TextSegmentIterator words =
        GranularityIterator.getIteratorForGranularity(
            TEXT, AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_WORD);
    List<String> found = new ArrayList<>();
    int[] range;
    for (int offset = 0; (range = words.following(offset)) != null; offset = range[1]) {
      found.add(TEXT.substring(range[0], range[1]));
    }
    return found;
  }

  private static List<String> wordsBackward() {
    TextSegmentIterator words =
        GranularityIterator.getIteratorForGranularity(
            TEXT, AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_WORD);
    List<String> found = new ArrayList<>();
    int[] range;
    for (int offset = TEXT.length(); (range = words.preceding(offset)) != null; ) {
      found.add(0, TEXT.substring(range[0], range[1]));
      offset = range[0];
    }
    return found;
  }

  @Test
  public void emojiAreWordsWhenBacktalkNamesThem() {
    EmojiSpeech.setMode(context, EmojiSpeech.MODE_NAMES);
    List<String> expected = Arrays.asList("hi", "👍🏽", "there", "👨‍👩‍👧", "😀", "😀", "ok");
    assertEquals(expected, wordsForward());
    assertEquals(expected, wordsBackward());
  }

  @Test
  public void emojiAreSkippedOtherwise() {
    for (String mode : new String[] {EmojiSpeech.MODE_ENGINE, EmojiSpeech.MODE_NONE}) {
      EmojiSpeech.setMode(context, mode);
      List<String> expected = Arrays.asList("hi", "there", "ok");
      assertEquals(mode, expected, wordsForward());
      assertEquals(mode, expected, wordsBackward());
    }
  }
}
