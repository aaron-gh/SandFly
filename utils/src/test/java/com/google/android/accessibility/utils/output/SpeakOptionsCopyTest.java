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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions;
import java.util.Set;
import org.junit.Test;

public class SpeakOptionsCopyTest {

  private static SpeakOptions everyFieldSet() {
    return SpeakOptions.create()
        .setEarcons(Set.of(1, 2))
        .setHaptics(Set.of(3))
        .setQueueMode(SpeechController.QUEUE_MODE_QUEUE)
        .setFlags(FeedbackItem.FLAG_COMMAND_FEEDBACK | FeedbackItem.FLAG_FORCE_FEEDBACK)
        .setUtteranceGroup(SpeechController.UTTERANCE_GROUP_NOTIFICATIONS)
        .setStartingAction(() -> {})
        .setRangeStartCallback((start, end) -> {})
        .setCompletedAction(status -> {});
  }

  @Test
  public void copy_hasEveryFieldOfTheOriginal() {
    SpeakOptions original = everyFieldSet();

    SpeakOptions copy = original.copy();

    assertNotSame(original, copy);
    assertEquals(original, copy);
    assertSame(original.mStartingAction, copy.mStartingAction);
    assertSame(original.mRangeStartCallback, copy.mRangeStartCallback);
    assertSame(original.mCompletedAction, copy.mCompletedAction);
  }

  @Test
  public void changingTheCopy_leavesTheOriginalAsItWas() {
    SpeakOptions original = everyFieldSet();
    int flags = original.mFlags;

    SpeakOptions copy =
        original
            .copy()
            .setUtteranceGroup(SpeechController.UTTERANCE_GROUP_COMMAND_FEEDBACK)
            .setFlags(flags | FeedbackItem.FLAG_INTERRUPT_CURRENT_UTTERANCE_WITH_SAME_UTTERANCE_GROUP);

    assertEquals(SpeechController.UTTERANCE_GROUP_NOTIFICATIONS, original.mUtteranceGroup);
    assertEquals(flags, original.mFlags);
    assertEquals(SpeechController.UTTERANCE_GROUP_COMMAND_FEEDBACK, copy.mUtteranceGroup);
  }
}
