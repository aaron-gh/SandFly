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

package com.google.android.accessibility.utils.output;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SpeechProgressEstimatorTest {
  private static final String ENGINE = "engine";
  private static final String TEXT =
      "The first sentence is here. The second one, with a clause, follows it. "
          + "And the third sentence ends the paragraph.";

  @Test
  public void phraseStartIsAfterTheLastSentenceOrClauseBeforeThePosition() {
    int inThird = TEXT.indexOf("third");
    assertEquals(TEXT.indexOf("And"), SpeechProgressEstimator.phraseStartBefore(TEXT, inThird));
    int inClause = TEXT.indexOf("clause");
    assertEquals(TEXT.indexOf("with"), SpeechProgressEstimator.phraseStartBefore(TEXT, inClause));
    assertEquals(0, SpeechProgressEstimator.phraseStartBefore(TEXT, TEXT.indexOf("first")));
  }

  @Test
  public void nothingSpeakingMeansStartOver() {
    SpeechProgressEstimator estimator = new SpeechProgressEstimator();
    assertNull(estimator.currentUtteranceId());
    assertEquals(0, estimator.estimateResumeOffset(10_000));
    estimator.onQueued("a", TEXT, 1f, ENGINE);
    assertEquals(0, estimator.estimateResumeOffset(10_000));
  }

  @Test
  public void pausingSoonAfterTheStartStartsOver() {
    SpeechProgressEstimator estimator = new SpeechProgressEstimator();
    estimator.onQueued("a", TEXT, 1f, ENGINE);
    estimator.onStarted("a", 1000);
    assertEquals(0, estimator.estimateResumeOffset(1000 + SpeechProgressEstimator.SAFETY_MS));
  }

  @Test
  public void estimateNeverPassesWhereTheEngineGotTo() {
    SpeechProgressEstimator estimator = new SpeechProgressEstimator();
    estimator.onQueued("a", TEXT, 1f, ENGINE);
    estimator.onStarted("a", 0);
    for (long ms = 0; ms < 10_000; ms += 250) {
      int offset = estimator.estimateResumeOffset(ms);
      int reached = (int) (ms * SpeechProgressEstimator.DEFAULT_CHARS_PER_SECOND / 1000);
      assertTrue(offset <= reached);
      assertTrue(offset == 0 || Character.isWhitespace(TEXT.charAt(offset - 1)));
    }
  }

  @Test
  public void learnsTheEngineSpeedFromFinishedUtterances() {
    SpeechProgressEstimator estimator = new SpeechProgressEstimator();
    // 100 characters in 2.5 seconds is 40 characters a second.
    String hundred = "x".repeat(100);
    estimator.onQueued("a", hundred, 1f, ENGINE);
    estimator.onStarted("a", 0);
    estimator.onFinished("a", 2500, /* completed= */ true);

    estimator.onQueued("b", TEXT, 1f, ENGINE);
    estimator.onStarted("b", 10_000);
    // After 1.4 seconds, less the safety margin, the engine is 40 characters in, in "with a clause".
    assertEquals(TEXT.indexOf("The second"), estimator.estimateResumeOffset(11_400));
    assertEquals(TEXT.indexOf("with"), estimator.estimateResumeOffset(11_700));
  }

  @Test
  public void aPauseIsNotLearnedAsSpeakingTime() {
    SpeechProgressEstimator estimator = new SpeechProgressEstimator();
    // 100 characters in 2.5 seconds of speech, with a 7.5 second pause in the middle.
    String hundred = "x".repeat(100);
    estimator.onQueued("a", hundred, 1f, ENGINE);
    estimator.onStarted("a", 0);
    estimator.onPaused("a", 7500);
    estimator.onFinished("a", 10_000, /* completed= */ true);

    estimator.onQueued("b", TEXT, 1f, ENGINE);
    estimator.onStarted("b", 20_000);
    // Learned as 40 characters a second, as without the pause.
    assertEquals(TEXT.indexOf("with"), estimator.estimateResumeOffset(21_700));
  }

  @Test
  public void speedScalesWithTheSpeechRate() {
    SpeechProgressEstimator slow = new SpeechProgressEstimator();
    SpeechProgressEstimator fast = new SpeechProgressEstimator();
    slow.onQueued("a", TEXT, 1f, ENGINE);
    fast.onQueued("a", TEXT, 2f, ENGINE);
    slow.onStarted("a", 0);
    fast.onStarted("a", 0);
    assertTrue(fast.estimateResumeOffset(4000) > slow.estimateResumeOffset(4000));
  }

  @Test
  public void stoppedOrShortUtterancesAreNotLearned() {
    SpeechProgressEstimator estimator = new SpeechProgressEstimator();
    estimator.onQueued("stopped", "x".repeat(100), 1f, ENGINE);
    estimator.onStarted("stopped", 0);
    estimator.onFinished("stopped", 100, /* completed= */ false);
    estimator.onQueued("short", "Hi", 1f, ENGINE);
    estimator.onStarted("short", 0);
    estimator.onFinished("short", 10, /* completed= */ true);

    SpeechProgressEstimator fresh = new SpeechProgressEstimator();
    estimator.onQueued("b", TEXT, 1f, ENGINE);
    fresh.onQueued("b", TEXT, 1f, ENGINE);
    estimator.onStarted("b", 0);
    fresh.onStarted("b", 0);
    assertEquals(fresh.estimateResumeOffset(5000), estimator.estimateResumeOffset(5000));
  }

  @Test
  public void enginesThatReportWordPositionsGetNoEstimate() {
    SpeechProgressEstimator estimator = new SpeechProgressEstimator();
    estimator.onQueued("a", TEXT, 1f, ENGINE);
    estimator.onStarted("a", 0);
    estimator.onRange("a");
    assertEquals(0, estimator.estimateResumeOffset(5000));
  }

  @Test
  public void finishingTheUtteranceClearsIt() {
    SpeechProgressEstimator estimator = new SpeechProgressEstimator();
    estimator.onQueued("a", TEXT, 1f, ENGINE);
    estimator.onStarted("a", 0);
    estimator.onFinished("a", 100, /* completed= */ false);
    assertNull(estimator.currentUtteranceId());
    assertEquals(0, estimator.estimateResumeOffset(5000));
  }
}
