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

package com.google.android.accessibility.utils.output;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.R;
import java.util.List;
import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** The emoji sound after emoji, with After emoji names set to Play a sound. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class EmojiSoundFragmentsTest {
  private Context context;

  @Before
  public void setUp() {
    context = RuntimeEnvironment.getApplication();
    LanguageSwitch.setVoiceLanguage(Locale.US);
  }

  @After
  public void tearDown() {
    EmojiSpeech.setAfterName(EmojiSpeech.AFTER_NAME_NOTHING);
    EmojiSpeech.setMode(context, EmojiSpeech.MODE_ENGINE);
  }

  private FeedbackItem split(String mode, String text) {
    EmojiSpeech.setMode(context, mode);
    FeedbackItem item = new FeedbackItem((EventId) null);
    item.addFragment(new FeedbackFragment(EmojiSpeech.process(context, text), null));
    FeedbackProcessingUtils.addFormattingCharacteristics(
        context,
        item,
        /* usePunctuation= */ false,
        new SpeechControllerImpl.InlineFormattingHistory(""),
        TextFormattingUtils.OPTION_NONE,
        TextFormattingUtils.FEEDBACK_MODE_NONE,
        /* removeUnnecessarySpans= */ false);
    return item;
  }

  private static boolean isEmoji(FeedbackFragment fragment) {
    return fragment.getSpeechParams().getBoolean(EmojiSpeech.SPEECH_PARAM_EMOJI, false);
  }

  private static boolean isSound(FeedbackFragment fragment) {
    return TextUtils.isEmpty(fragment.getText())
        && fragment.getEarcons().size() == 1
        && fragment.getEarcons().contains(R.raw.emoji);
  }

  /** Checks that each emoji fragment, and nothing else, is followed by a sound of its own. */
  private static void assertSoundAfterEachEmoji(FeedbackItem item, int emojiCount) {
    List<FeedbackFragment> fragments = item.getFragments();
    int emoji = 0;
    int sounds = 0;
    for (int i = 0; i < fragments.size(); i++) {
      FeedbackFragment fragment = fragments.get(i);
      if (isEmoji(fragment)) {
        emoji++;
        assertTrue("no sound after " + fragment.getText(), i + 1 < fragments.size());
        assertTrue(isSound(fragments.get(i + 1)));
      } else if (isSound(fragment)) {
        sounds++;
        assertTrue(i > 0 && isEmoji(fragments.get(i - 1)));
      } else {
        assertFalse(fragment.getEarcons().contains(R.raw.emoji));
      }
    }
    assertEquals(emojiCount, emoji);
    assertEquals(emojiCount, sounds);
  }

  private static String joined(FeedbackItem item) {
    StringBuilder text = new StringBuilder();
    for (FeedbackFragment fragment : item.getFragments()) {
      text.append(fragment.getText());
    }
    return text.toString();
  }

  @Test
  public void aSoundFollowsEachNamedEmojiAlsoAtTheEnd() {
    EmojiSpeech.setAfterName(EmojiSpeech.AFTER_NAME_SOUND);
    FeedbackItem item = split(EmojiSpeech.MODE_NAMES, "yes 😀 great 👍");
    assertSoundAfterEachEmoji(item, 2);
    assertEquals("yes grinning face great thumbs up", joined(item));
    assertTrue(isSound(item.getFragments().get(item.getFragments().size() - 1)));
  }

  @Test
  public void aSoundFollowsEmojiTheEngineReads() {
    EmojiSpeech.setAfterName(EmojiSpeech.AFTER_NAME_SOUND);
    FeedbackItem item = split(EmojiSpeech.MODE_ENGINE, "😀 hi");
    assertSoundAfterEachEmoji(item, 1);
    assertEquals("😀 hi", joined(item));
  }

  @Test
  public void emojiNextToEachOtherHaveOneSound() {
    EmojiSpeech.setAfterName(EmojiSpeech.AFTER_NAME_SOUND);
    FeedbackItem item = split(EmojiSpeech.MODE_NAMES, "ok 😀😀 👍 done");
    assertSoundAfterEachEmoji(item, 1);
  }

  @Test
  public void noSoundWithTheWordOrNothing() {
    for (String after :
        new String[] {EmojiSpeech.AFTER_NAME_WORD, EmojiSpeech.AFTER_NAME_NOTHING}) {
      EmojiSpeech.setAfterName(after);
      FeedbackItem item = split(EmojiSpeech.MODE_NAMES, "yes 😀 great");
      for (FeedbackFragment fragment : item.getFragments()) {
        assertFalse(after, fragment.getEarcons().contains(R.raw.emoji));
        assertFalse(after, TextUtils.isEmpty(fragment.getText()));
      }
    }
  }

  @Test
  public void soundFragmentsAreAddedAfterFragmentsMarkedAsEmoji() {
    FeedbackItem item = new FeedbackItem((EventId) null);
    Bundle emoji = new Bundle();
    emoji.putBoolean(EmojiSpeech.SPEECH_PARAM_EMOJI, true);
    item.addFragment(new FeedbackFragment("grinning face", emoji));
    item.addFragment(new FeedbackFragment(" and ", null));
    item.addFragment(new FeedbackFragment("thumbs up", emoji));
    FeedbackProcessingUtils.addEmojiSounds(item);
    assertEquals(5, item.getFragments().size());
    assertSoundAfterEachEmoji(item, 2);
  }
}
