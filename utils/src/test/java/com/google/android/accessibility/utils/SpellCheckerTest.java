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

package com.google.android.accessibility.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.text.Spanned;
import android.text.style.SuggestionSpan;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SuggestionsInfo;
import com.google.common.collect.ImmutableList;
import java.time.Duration;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSystemClock;

/** Spell checker results for the text of an edit field. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SpellCheckerTest {

  private static SuggestionsInfo typo() {
    return new SuggestionsInfo(
        SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO, new String[] {"word"});
  }

  private static SuggestionSpan[] spans(CharSequence text) {
    Spanned spanned = (Spanned) text;
    return spanned.getSpans(0, spanned.length(), SuggestionSpan.class);
  }

  @Test
  public void resultsPastTheEndOfTheTextAreSkipped() {
    Context context = RuntimeEnvironment.getApplication();
    String text = "one wrd and a shorter end";
    SentenceSuggestionsInfo info =
        new SentenceSuggestionsInfo(
            new SuggestionsInfo[] {typo(), typo()}, new int[] {4, 25}, new int[] {3, 5});

    CharSequence result =
        SpellChecker.wrapSuggestionSpansForAllMisspelledWords(
            context, text, ImmutableList.of(info));

    SuggestionSpan[] spans = spans(result);
    assertEquals(1, spans.length);
    assertEquals(4, ((Spanned) result).getSpanStart(spans[0]));
    assertEquals(7, ((Spanned) result).getSpanEnd(spans[0]));
  }

  @After
  public void tearDown() {
    SpellChecker.resetSessionUnavailable();
    SpellChecker.clearCache();
  }

  @Test
  public void noNewSessionIsTriedForAWhileAfterOneFails() {
    Context context = RuntimeEnvironment.getApplication();
    SpellChecker.resetSessionUnavailable();
    SpellChecker first = new SpellChecker(context, "wrd", new SpellChecker.SpellCheckingListener());

    // Robolectric has no spell checker service, so the session can't be created.
    assertFalse(first.perform());
    assertTrue(SpellChecker.isSessionUnavailable());
    assertEquals("wrd", SpellChecker.getTextWithSuggestionSpans(context, "wrd").toString());

    ShadowSystemClock.advanceBy(Duration.ofMillis(SpellChecker.NO_SESSION_RETRY_DELAY_MS));
    assertFalse(SpellChecker.isSessionUnavailable());
  }

  @Test
  public void cachedTextReturnsInputOnMissAndSpansOnCacheHit() {
    Context context = RuntimeEnvironment.getApplication();
    String text = "wrd";

    CharSequence miss = SpellChecker.getCachedTextWithSuggestionSpans(context, text);
    assertEquals(text, miss.toString());
    assertTrue(SpellChecker.isSessionUnavailable());

    SentenceSuggestionsInfo info =
        new SentenceSuggestionsInfo(new SuggestionsInfo[] {typo()}, new int[] {0}, new int[] {3});
    CharSequence checked =
        SpellChecker.wrapSuggestionSpansForAllMisspelledWords(
            context, text, ImmutableList.of(info));
    SpellChecker.updateCachedResult(checked);

    CharSequence hit = SpellChecker.getCachedTextWithSuggestionSpans(context, text);
    assertEquals(1, spans(hit).length);
  }

  @Test
  public void timeoutBackoffSkipsNewSessionsUntilDelayExpires() {
    SpellChecker.markSessionUnavailable();
    assertTrue(SpellChecker.isSessionUnavailable());

    ShadowSystemClock.advanceBy(Duration.ofMillis(SpellChecker.NO_SESSION_RETRY_DELAY_MS));
    assertFalse(SpellChecker.isSessionUnavailable());
  }
}
