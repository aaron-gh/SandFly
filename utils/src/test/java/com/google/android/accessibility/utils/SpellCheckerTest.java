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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
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

  private static SentenceSuggestionsInfo singleTypoResult(int length) {
    return new SentenceSuggestionsInfo(
        new SuggestionsInfo[] {typo()}, new int[] {0}, new int[] {length});
  }

  @After
  public void tearDown() {
    SpellChecker.setSessionStarterForTesting(null);
    SpellChecker.resetSessionUnavailable();
    SpellChecker.clearCache();
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
    AtomicReference<SpellChecker.SpellCheckingListener> activeListener = new AtomicReference<>();
    SpellChecker.setSessionStarterForTesting(
        (input, listener) -> {
          activeListener.set(listener);
          return () -> {};
        });

    CharSequence miss = SpellChecker.getCachedTextWithSuggestionSpans(context, text);
    assertEquals(text, miss.toString());
    assertFalse(SpellChecker.hasCachedResult(text));

    activeListener.get().onFinished(ImmutableList.of(singleTypoResult(text.length())));

    assertTrue(SpellChecker.hasCachedResult(text));
    CharSequence hit = SpellChecker.getCachedTextWithSuggestionSpans(context, text);
    assertEquals(1, spans(hit).length);
  }

  @Test
  public void backgroundTimeoutBacksOffWarmUpsWithoutBlockingSyncCheck() {
    Context context = RuntimeEnvironment.getApplication();
    AtomicInteger startedSessions = new AtomicInteger();
    AtomicInteger closedSessions = new AtomicInteger();
    SpellChecker.setSessionStarterForTesting(
        (input, listener) -> {
          startedSessions.incrementAndGet();
          if ("sync".contentEquals(input)) {
            listener.onFinished(ImmutableList.of(singleTypoResult(input.length())));
          }
          return closedSessions::incrementAndGet;
        });

    SpellChecker.getCachedTextWithSuggestionSpans(context, "slow");
    assertEquals(1, startedSessions.get());

    ShadowLooper.idleMainLooper(
        SpellChecker.BACKGROUND_CHECK_MAX_WAITING_TIME_MS, TimeUnit.MILLISECONDS);
    assertEquals(1, closedSessions.get());
    assertTrue(SpellChecker.isBackgroundTimeoutActive());
    assertFalse(SpellChecker.isSessionUnavailable());

    // Automatic warm-ups back off after a background timeout.
    SpellChecker.getCachedTextWithSuggestionSpans(context, "skipped");
    assertEquals(1, startedSessions.get());

    // Explicit synchronous checks still run during background timeout backoff.
    CharSequence syncResult = SpellChecker.getTextWithSuggestionSpans(context, "sync");
    assertEquals(2, startedSessions.get());
    assertEquals(1, spans(syncResult).length);

    ShadowSystemClock.advanceBy(Duration.ofMillis(SpellChecker.NO_SESSION_RETRY_DELAY_MS));
    assertFalse(SpellChecker.isBackgroundTimeoutActive());
    SpellChecker.getCachedTextWithSuggestionSpans(context, "after");
    assertEquals(3, startedSessions.get());
  }

  @Test
  public void syncTimeoutDoesNotMarkSessionUnavailable() {
    Context context = RuntimeEnvironment.getApplication();
    SpellChecker.setSessionStarterForTesting((input, listener) -> () -> {});

    CharSequence result = SpellChecker.getTextWithSuggestionSpans(context, "hang");
    assertEquals("hang", result.toString());
    assertFalse(SpellChecker.isSessionUnavailable());
    assertFalse(SpellChecker.isBackgroundTimeoutActive());
  }

  @Test
  public void inFlightWarmUpReusesSessionForCallback() {
    Context context = RuntimeEnvironment.getApplication();
    AtomicInteger startedSessions = new AtomicInteger();
    AtomicBoolean callbackRan = new AtomicBoolean();
    AtomicReference<SpellChecker.SpellCheckingListener> activeListener = new AtomicReference<>();
    SpellChecker.setSessionStarterForTesting(
        (input, listener) -> {
          startedSessions.incrementAndGet();
          activeListener.set(listener);
          return () -> {};
        });

    SpellChecker.getCachedTextWithSuggestionSpans(context, "helo");
    SpellChecker.checkInBackground(context, "helo", () -> callbackRan.set(true));
    assertEquals(1, startedSessions.get());

    activeListener.get().onFinished(ImmutableList.of(singleTypoResult(4)));
    assertTrue(callbackRan.get());
    assertEquals(1, spans(SpellChecker.getCachedTextWithSuggestionSpans(context, "helo")).length);
  }

  @Test
  public void cacheRetainsMultipleFieldsAndEvictsOldestBeyondLimit() {
    Context context = RuntimeEnvironment.getApplication();
    for (int i = 0; i < SpellChecker.MAX_CACHE_ENTRIES; i++) {
      String text = "wrd" + i;
      SpellChecker.updateCachedResult(
          SpellChecker.wrapSuggestionSpansForAllMisspelledWords(
              context, text, ImmutableList.of(singleTypoResult(text.length()))));
    }

    // Accessing field 0 keeps it fresh, so field 1 is evicted when the limit is exceeded.
    assertEquals(1, spans(SpellChecker.getCachedTextWithSuggestionSpans(context, "wrd0")).length);
    String extra = "extra";
    SpellChecker.updateCachedResult(
        SpellChecker.wrapSuggestionSpansForAllMisspelledWords(
            context, extra, ImmutableList.of(singleTypoResult(extra.length()))));

    assertTrue(SpellChecker.hasCachedResult("wrd0"));
    assertFalse(SpellChecker.hasCachedResult("wrd1"));
    assertTrue(SpellChecker.hasCachedResult(extra));
  }
}
