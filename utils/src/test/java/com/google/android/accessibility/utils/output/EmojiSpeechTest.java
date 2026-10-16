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

package com.google.android.accessibility.utils.output;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.LocaleSpan;
import android.text.style.StyleSpan;
import android.text.style.TtsSpan;
import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class EmojiSpeechTest {
  private Context context;

  @Before
  public void setUp() {
    context = RuntimeEnvironment.getApplication();
    LanguageSwitch.setVoiceLanguage(Locale.US);
  }

  @After
  public void tearDown() {
    EmojiSpeech.setMode(context, EmojiSpeech.MODE_ENGINE);
    EmojiSpeech.setRepeatCount(0);
  }

  private String spoken(String mode, CharSequence text) {
    EmojiSpeech.setMode(context, mode);
    return EmojiSpeech.process(context, text).toString();
  }

  @Test
  public void emojiAroundFindsWholeEmojiInALongRun() {
    EmojiSpeech.setMode(context, EmojiSpeech.MODE_NAMES);
    String family = "👨‍👩‍👧‍👦";
    String text = "a" + family.repeat(10) + "b";
    // Inside the eighth family, more than 64 characters from the start of the run.
    int eighth = 1 + 7 * family.length();
    int[] around = EmojiSpeech.emojiAround(context, text, eighth + 3);
    assertEquals(eighth, around[0]);
    assertEquals(eighth + family.length(), around[1]);
  }

  @Test
  public void emojiAroundPairsFlagsFromTheStartOfTheRun() {
    EmojiSpeech.setMode(context, EmojiSpeech.MODE_NAMES);
    String flags = "🇨🇦🇫🇷".repeat(20);
    // Each flag is two regional indicators, each two UTF-16 units.
    int[] around = EmojiSpeech.emojiAround(context, flags, 4 * 33 + 2);
    assertEquals(4 * 33, around[0]);
    assertEquals(4 * 34, around[1]);
  }

  @Test
  public void namesInSeveralLanguagesAtOnce() {
    SpannableStringBuilder text = new SpannableStringBuilder("👍 👍 👍 👍");
    // Each thumbs up is two UTF-16 units, with a space after it.
    text.setSpan(new LocaleSpan(Locale.GERMAN), 3, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    text.setSpan(new LocaleSpan(Locale.FRENCH), 6, 8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    text.setSpan(
        new LocaleSpan(Locale.forLanguageTag("es")), 9, 11, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    String spoken = spoken(EmojiSpeech.MODE_NAMES, text);
    assertEquals("thumbs up Daumen hoch pouce vers le haut pulgar hacia arriba", spoken);
  }

  @Test
  public void engineGetsTheTextItself() {
    EmojiSpeech.setMode(context, EmojiSpeech.MODE_ENGINE);
    CharSequence text = "Hi 😀";
    assertSame(text, EmojiSpeech.process(context, text));
  }

  @Test
  public void textWithoutEmojiIsUntouched() {
    EmojiSpeech.setMode(context, EmojiSpeech.MODE_NAMES);
    CharSequence text = "Hello © 2026";
    assertSame(text, EmojiSpeech.process(context, text));
  }

  @Test
  public void namesReplaceEmoji() {
    assertEquals("Hi grinning face there", spoken(EmojiSpeech.MODE_NAMES, "Hi 😀 there"));
    assertEquals(
        "ok thumbs up: medium skin tone thanks",
        spoken(EmojiSpeech.MODE_NAMES, "ok👍🏽thanks"));
    assertEquals(
        "family: man, woman, girl grinning face",
        spoken(EmojiSpeech.MODE_NAMES, "👨‍👩‍👧😀"));
  }

  @Test
  public void noneLeavesEmojiOut() {
    assertEquals("I love it   so much", spoken(EmojiSpeech.MODE_NONE, "I love it 👍🏽 so much"));
    assertEquals("well done", spoken(EmojiSpeech.MODE_NONE, "well👍🏽done"));
  }

  @Test
  public void noneStillNamesTextThatIsOnlyEmoji() {
    assertEquals("thumbs up: medium skin tone", spoken(EmojiSpeech.MODE_NONE, "👍🏽"));
    assertEquals("grinning face grinning face", spoken(EmojiSpeech.MODE_NONE, " 😀😀").trim());
  }

  @Test
  public void namesFollowTheLanguageOfTheText() {
    SpannableStringBuilder text = new SpannableStringBuilder("Hi 👍 und 👍");
    text.setSpan(new LocaleSpan(Locale.GERMANY), 6, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    assertEquals("Hi thumbs up und Daumen hoch", spoken(EmojiSpeech.MODE_NAMES, text));
  }

  @Test
  public void otherSpansAreKept() {
    SpannableStringBuilder text = new SpannableStringBuilder("Bold 😀 text");
    StyleSpan bold = new StyleSpan(android.graphics.Typeface.BOLD);
    text.setSpan(bold, 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    EmojiSpeech.setMode(context, EmojiSpeech.MODE_NAMES);
    Spanned result = (Spanned) EmojiSpeech.process(context, text);
    assertEquals(0, result.getSpanStart(bold));
    assertEquals(result.length(), result.getSpanEnd(bold));
  }

  @Test
  public void emojiCountedAsSymbolsFollowTheEmojiCount() {
    // As SpeechCleanupUtils counts repeated symbols.
    SpannableStringBuilder text = new SpannableStringBuilder("lol 😂😂😂😂");
    text.setSpan(
        new TtsSpan.TextBuilder("4 😂").build(), 4, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    assertEquals(
        "lol face with tears of joy face with tears of joy face with tears of joy"
            + " face with tears of joy",
        spoken(EmojiSpeech.MODE_NAMES, text));
    EmojiSpeech.setRepeatCount(3);
    assertEquals("lol 4 face with tears of joy", spoken(EmojiSpeech.MODE_NAMES, text));
    assertEquals("lol  ", spoken(EmojiSpeech.MODE_NONE, text));
    assertEquals("4 face with tears of joy", spoken(EmojiSpeech.MODE_NONE, text.subSequence(4, 12)));
  }

  @Test
  public void repeatedEmojiAreCountedFromTheSetting() {
    String text = "yes 😀😀 great 👍🏽👍🏽👍🏽 👍🏽👍 ok";
    assertEquals(
        "yes grinning face grinning face great thumbs up: medium skin tone thumbs up: medium skin"
            + " tone thumbs up: medium skin tone thumbs up: medium skin tone thumbs up ok",
        spoken(EmojiSpeech.MODE_NAMES, text));
    EmojiSpeech.setRepeatCount(2);
    // Only the same emoji, skin tone and all, right after itself is counted.
    assertEquals(
        "yes 2 grinning face great 3 thumbs up: medium skin tone thumbs up: medium skin tone"
            + " thumbs up ok",
        spoken(EmojiSpeech.MODE_NAMES, text));
    EmojiSpeech.setRepeatCount(3);
    assertEquals(
        "yes grinning face grinning face great 3 thumbs up: medium skin tone thumbs up: medium"
            + " skin tone thumbs up ok",
        spoken(EmojiSpeech.MODE_NAMES, text));
    EmojiSpeech.setRepeatCount(4);
    assertEquals("4 grinning face", spoken(EmojiSpeech.MODE_NAMES, "😀😀😀😀"));
    assertEquals("grinning face grinning face grinning face", spoken(EmojiSpeech.MODE_NAMES, "😀😀😀"));
  }

  @Test
  public void countingIsOnlyForNames() {
    EmojiSpeech.setRepeatCount(2);
    assertEquals("yes    ok", spoken(EmojiSpeech.MODE_NONE, "yes 😀😀 ok"));
    CharSequence text = "yes 😀😀 ok";
    EmojiSpeech.setMode(context, EmojiSpeech.MODE_ENGINE);
    assertSame(text, EmojiSpeech.process(context, text));
  }

  @Test
  public void copyingGetsRepeatedEmojiBack() {
    EmojiSpeech.setRepeatCount(2);
    EmojiSpeech.setMode(context, EmojiSpeech.MODE_NAMES);
    String text = "yes 😀😀 great 👍🏽👍🏽👍🏽 ok";
    assertEquals(text, EmojiSpeech.restoreOriginalText(EmojiSpeech.process(context, text)).toString());
  }

  @Test
  public void copyingGetsTheEmojiBack() {
    String[] texts = {"Hi 😀 there", "ok👍🏽thanks", "👨‍👩‍👧😀", "I ❤️ it, © 2026", "👍🏽"};
    for (String mode : new String[] {EmojiSpeech.MODE_NAMES, EmojiSpeech.MODE_NONE}) {
      EmojiSpeech.setMode(context, mode);
      for (String text : texts) {
        CharSequence spoken = EmojiSpeech.process(context, text);
        assertEquals(mode, text, EmojiSpeech.restoreOriginalText(spoken).toString());
      }
    }
  }

  @Test
  public void copyingGetsCountedEmojiBack() {
    SpannableStringBuilder text = new SpannableStringBuilder("lol 😂😂😂😂 ok 😀");
    text.setSpan(new TtsSpan.TextBuilder("4 😂").build(), 4, 12, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    for (String mode : new String[] {EmojiSpeech.MODE_NAMES, EmojiSpeech.MODE_NONE}) {
      EmojiSpeech.setMode(context, mode);
      CharSequence spoken = EmojiSpeech.process(context, text);
      assertEquals(mode, text.toString(), EmojiSpeech.restoreOriginalText(spoken).toString());
    }
  }

  @Test
  public void copyingSurvivesSplittingIntoFragments() {
    EmojiSpeech.setMode(context, EmojiSpeech.MODE_NAMES);
    CharSequence spoken = EmojiSpeech.process(context, "One 😀. Two 👍🏽.");
    // Speech is split into fragments, then joined again for copying.
    SpannableStringBuilder joined = new SpannableStringBuilder();
    joined.append(spoken.subSequence(0, 18)).append(spoken.subSequence(18, spoken.length()));
    assertEquals("One 😀. Two 👍🏽.", EmojiSpeech.restoreOriginalText(joined).toString());
  }

  @Test
  public void emojiEndForSpelling() {
    String text = "a👍🏽b";
    EmojiSpeech.setMode(context, EmojiSpeech.MODE_ENGINE);
    assertEquals(-1, EmojiSpeech.emojiEnd(context, text, 1));
    EmojiSpeech.setMode(context, EmojiSpeech.MODE_NAMES);
    assertEquals(-1, EmojiSpeech.emojiEnd(context, text, 0));
    assertEquals(5, EmojiSpeech.emojiEnd(context, text, 1));
  }
}
