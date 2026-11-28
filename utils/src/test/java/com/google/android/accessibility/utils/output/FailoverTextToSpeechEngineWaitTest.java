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

import static com.google.android.accessibility.utils.output.FailoverTextToSpeech.ENGINE_HANG_MS;
import static com.google.android.accessibility.utils.output.FailoverTextToSpeech.ENGINE_PROBE_WAIT_MS;
import static org.junit.Assert.assertEquals;

import com.google.android.accessibility.utils.output.FailoverTextToSpeech.UnansweredSpeech;
import org.junit.Test;

/** Checks when speech that flushes the queue starts its own wait for the engine. */
public class FailoverTextToSpeechEngineWaitTest {
  @Test
  public void flushes_startTheirOwnWait_untilSeveralGoUnanswered() {
    UnansweredSpeech speech = new UnansweredSpeech();

    assertEquals(0, speech.onSpeech(1000, /* flush= */ true));
    assertEquals(0, speech.onSpeech(2000, /* flush= */ true));
    // The third flush with no report keeps the wait from the first.
    assertEquals(1000, speech.onSpeech(3000, /* flush= */ true));
    assertEquals(1000, speech.onSpeech(4000, /* flush= */ true));
  }

  @Test
  public void aReport_startsCountingAgain() {
    UnansweredSpeech speech = new UnansweredSpeech();
    speech.onSpeech(1000, /* flush= */ true);
    speech.onSpeech(2000, /* flush= */ true);

    speech.reset();

    assertEquals(0, speech.onSpeech(3000, /* flush= */ true));
    assertEquals(0, speech.onSpeech(4000, /* flush= */ true));
    assertEquals(3000, speech.onSpeech(5000, /* flush= */ true));
  }

  @Test
  public void queuedSpeech_isNotAFlush_butCanBeTheFirstUnanswered() {
    UnansweredSpeech speech = new UnansweredSpeech();

    assertEquals(0, speech.onSpeech(500, /* flush= */ false));
    assertEquals(0, speech.onSpeech(1000, /* flush= */ true));
    assertEquals(0, speech.onSpeech(2000, /* flush= */ true));
    assertEquals(500, speech.onSpeech(3000, /* flush= */ true));
  }

  @Test
  public void keptWait_isNotHung_untilTheWaitAfterTheLastFlush() {
    UnansweredSpeech speech = new UnansweredSpeech();
    speech.onSpeech(1000, /* flush= */ true);
    speech.onSpeech(2000, /* flush= */ true);
    long keptSince = speech.onSpeech(3500, /* flush= */ true);
    assertEquals(1000, keptSince);

    // 3 s after the first flush, but only 0.5 s after the last one, as when typing fast.
    assertEquals(2500, speech.msUntilHang(4000, keptSince, ENGINE_HANG_MS));
    assertEquals(0, speech.msUntilHang(6500, keptSince, ENGINE_HANG_MS));
  }

  @Test
  public void wait_withoutFlushes_runsFromWhenTheEngineWentQuiet() {
    UnansweredSpeech speech = new UnansweredSpeech();
    speech.onSpeech(1000, /* flush= */ false);

    assertEquals(1000, speech.msUntilHang(3000, /* quietSince= */ 1000, ENGINE_HANG_MS));
    assertEquals(0, speech.msUntilHang(4000, /* quietSince= */ 1000, ENGINE_HANG_MS));
  }

  @Test
  public void aReport_forgetsTheLastFlush() {
    UnansweredSpeech speech = new UnansweredSpeech();
    speech.onSpeech(5000, /* flush= */ true);

    speech.reset();

    assertEquals(0, speech.msUntilHang(5000, /* quietSince= */ 1000, ENGINE_HANG_MS));
  }

  @Test
  public void unansweredProbe_isLookedAtAgain_beforeItCountsAsNotSpeaking() {
    // Looked at once the usual wait for an answer is over.
    assertEquals(ENGINE_PROBE_WAIT_MS, FailoverTextToSpeech.probeRecheckMs(0));
    assertEquals(200, FailoverTextToSpeech.probeRecheckMs(ENGINE_PROBE_WAIT_MS - 200));
    // Not answered by then: not known, and looked at again later.
    assertEquals(ENGINE_HANG_MS, FailoverTextToSpeech.probeRecheckMs(ENGINE_PROBE_WAIT_MS));
    // Still not answered: not speaking.
    assertEquals(
        0, FailoverTextToSpeech.probeRecheckMs(ENGINE_PROBE_WAIT_MS + ENGINE_HANG_MS));
    assertEquals(
        0, FailoverTextToSpeech.probeRecheckMs(ENGINE_PROBE_WAIT_MS + ENGINE_HANG_MS + 5000));
  }
}
