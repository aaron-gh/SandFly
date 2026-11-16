/*
 * Copyright 2026 SandFly contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.talkback.monitor;

import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

import android.os.Handler;
import android.os.Looper;
import com.google.android.accessibility.utils.monitor.RecentUserInput;
import com.google.android.accessibility.utils.output.SpeechController;
import com.google.android.accessibility.utils.output.SpeechController.UtteranceCompleteRunnable;
import java.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowSystemClock;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@LooperMode(LooperMode.Mode.PAUSED)
public final class WakeAnnouncementRecoveryTest {
  private int retries;
  private boolean eligible;
  private WakeAnnouncementRecovery recovery;

  @Before
  public void setUp() {
    eligible = true;
    RecentUserInput.mark();
    ShadowSystemClock.advanceBy(Duration.ofMillis(1));
    recovery =
        new WakeAnnouncementRecovery(
            new Handler(Looper.getMainLooper()), () -> eligible, () -> retries++);
  }

  @Test
  public void systemInterruptRetriesAfterTheClearOperationReturns() {
    UtteranceCompleteRunnable completed = recovery.track();
    recovery.onSystemInterrupt(() -> completed.run(SpeechController.STATUS_INTERRUPTED));
    assertEquals(0, retries);
    shadowOf(Looper.getMainLooper()).idle();
    assertEquals(1, retries);
  }

  @Test
  public void completedAnnouncementIsNotRepeatedByALaterSystemInterrupt() {
    UtteranceCompleteRunnable completed = recovery.track();
    completed.run(SpeechController.STATUS_SPOKEN);
    recovery.onSystemInterrupt(() -> {});
    shadowOf(Looper.getMainLooper()).idle();
    assertEquals(0, retries);
  }

  @Test
  public void interruptionWithoutTheSystemCallbackDoesNotRetry() {
    recovery.track().run(SpeechController.STATUS_INTERRUPTED);
    shadowOf(Looper.getMainLooper()).idle();
    assertEquals(0, retries);
  }

  @Test
  public void userInputBeforeTheSystemInterruptPreventsReplay() {
    UtteranceCompleteRunnable completed = recovery.track();
    ShadowSystemClock.advanceBy(Duration.ofMillis(1));
    RecentUserInput.mark();
    recovery.onSystemInterrupt(() -> completed.run(SpeechController.STATUS_INTERRUPTED));
    shadowOf(Looper.getMainLooper()).idle();
    assertEquals(0, retries);
  }

  @Test
  public void userInputWhileTheCompletionIsQueuedPreventsReplay() {
    UtteranceCompleteRunnable completed = recovery.track();
    recovery.onSystemInterrupt(() -> completed.run(SpeechController.STATUS_INTERRUPTED));
    ShadowSystemClock.advanceBy(Duration.ofMillis(1));
    RecentUserInput.mark();
    shadowOf(Looper.getMainLooper()).idle();
    assertEquals(0, retries);
  }

  @Test
  public void aStopInTheSameMillisecondAsTheRequestStillPreventsReplay() {
    UtteranceCompleteRunnable completed = recovery.track();
    recovery.onSystemInterrupt(() -> completed.run(SpeechController.STATUS_INTERRUPTED));
    RecentUserInput.mark();
    shadowOf(Looper.getMainLooper()).idle();
    assertEquals(0, retries);
  }

  @Test
  public void aTtsErrorIsNotTreatedAsASystemCancellation() {
    UtteranceCompleteRunnable completed = recovery.track();
    completed.run(SpeechController.STATUS_ERROR);
    shadowOf(Looper.getMainLooper()).idle();
    assertEquals(0, retries);
  }

  @Test
  public void screenOffWhileTheCompletionIsQueuedPreventsReplay() {
    UtteranceCompleteRunnable completed = recovery.track();
    recovery.onSystemInterrupt(() -> completed.run(SpeechController.STATUS_INTERRUPTED));
    recovery.cancel();
    shadowOf(Looper.getMainLooper()).idle();
    assertEquals(0, retries);
  }

  @Test
  public void callOrDisabledTimeBeforeTheCompletionPreventsReplay() {
    UtteranceCompleteRunnable completed = recovery.track();
    recovery.onSystemInterrupt(() -> completed.run(SpeechController.STATUS_INTERRUPTED));
    eligible = false;
    shadowOf(Looper.getMainLooper()).idle();
    assertEquals(0, retries);
  }

  @Test
  public void callbacksFromAnEarlierWakeCannotRetryInTheNextWake() {
    UtteranceCompleteRunnable previous = recovery.track();
    recovery.onSystemInterrupt(() -> {});
    recovery.cancel();
    recovery.track();
    previous.run(SpeechController.STATUS_INTERRUPTED);
    shadowOf(Looper.getMainLooper()).idle();
    assertEquals(0, retries);
  }

  @Test
  public void lateSystemInterruptDoesNotResurrectOldSpeech() {
    UtteranceCompleteRunnable completed = recovery.track();
    ShadowSystemClock.advanceBy(Duration.ofMillis(2001));
    recovery.onSystemInterrupt(() -> completed.run(SpeechController.STATUS_INTERRUPTED));
    shadowOf(Looper.getMainLooper()).idle();
    assertEquals(0, retries);
  }

  @Test
  public void repeatedInterruptsAndDuplicateCompletionsCannotLoop() {
    UtteranceCompleteRunnable completed = recovery.track();
    recovery.onSystemInterrupt(() -> {});
    recovery.onSystemInterrupt(() -> {});
    completed.run(SpeechController.STATUS_INTERRUPTED);
    completed.run(SpeechController.STATUS_INTERRUPTED);
    shadowOf(Looper.getMainLooper()).idle();
    recovery.onSystemInterrupt(() -> {});
    completed.run(SpeechController.STATUS_INTERRUPTED);
    shadowOf(Looper.getMainLooper()).idle();
    assertEquals(1, retries);
  }

  @Test
  public void hardStopReportedAsErrorStillRecoversTheWakeAnnouncement() {
    UtteranceCompleteRunnable completed = recovery.track();
    recovery.onSystemInterrupt(() -> completed.run(SpeechController.STATUS_ERROR));
    shadowOf(Looper.getMainLooper()).idle();
    assertEquals(1, retries);
  }

  @Test
  public void queuedAnnouncementRemovedWithoutACompletionCallbackIsRecovered() {
    recovery.track();
    recovery.onSystemInterrupt(() -> {});
    shadowOf(Looper.getMainLooper()).idle();
    assertEquals(1, retries);
  }

  @Test
  public void platformInterruptIsStillExecutedWhenNoTimeIsPending() {
    int[] interrupts = {0};
    recovery.onSystemInterrupt(() -> interrupts[0]++);
    shadowOf(Looper.getMainLooper()).idle();
    assertEquals(1, interrupts[0]);
    assertEquals(0, retries);
  }
}
