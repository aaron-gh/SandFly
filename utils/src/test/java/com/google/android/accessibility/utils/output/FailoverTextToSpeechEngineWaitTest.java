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
}
