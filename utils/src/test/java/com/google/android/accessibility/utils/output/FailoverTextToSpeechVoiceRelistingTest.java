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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.google.android.accessibility.utils.output.FailoverTextToSpeech.VoiceRelisting;
import org.junit.Test;

/** Checks when the voices kept for menus are listed again in the background, and kept. */
public class FailoverTextToSpeechVoiceRelistingTest {
  private static final long LISTED = 100_000;
  private static final long RELIST_AFTER_MS = VoiceRelisting.RELIST_AFTER_MS;
  private static final long OLD = LISTED + RELIST_AFTER_MS + 1;
  private static final int NOT_STARTED = VoiceRelisting.NOT_STARTED;

  private static VoiceRelisting listedAt(long time) {
    VoiceRelisting relisting = new VoiceRelisting();
    relisting.onListed(time);
    return relisting;
  }

  @Test
  public void keptVoices_areListedAgain_onlyOnceOld() {
    VoiceRelisting relisting = listedAt(LISTED);

    assertEquals(NOT_STARTED, relisting.start(LISTED + 1));
    assertEquals(NOT_STARTED, relisting.start(LISTED + RELIST_AFTER_MS));
    assertNotEquals(NOT_STARTED, relisting.start(OLD));
  }

  @Test
  public void oneListingRunsAtATime() {
    VoiceRelisting relisting = listedAt(LISTED);

    assertNotEquals(NOT_STARTED, relisting.start(OLD));
    assertEquals(NOT_STARTED, relisting.start(OLD + 1000));
  }

  @Test
  public void listedVoices_areKept_andNewForAMinute() {
    VoiceRelisting relisting = listedAt(LISTED);
    int started = relisting.start(OLD);

    assertTrue(relisting.onEnded(started, /* listed= */ true, OLD + 500));

    assertEquals(NOT_STARTED, relisting.start(OLD + 1000));
    assertNotEquals(NOT_STARTED, relisting.start(OLD + 500 + RELIST_AFTER_MS + 1));
  }

  @Test
  public void failedListing_isTriedAgainOnlyAMinuteLater() {
    VoiceRelisting relisting = listedAt(LISTED);
    int started = relisting.start(OLD);

    assertFalse(relisting.onEnded(started, /* listed= */ false, OLD + 500));

    assertEquals(NOT_STARTED, relisting.start(OLD + 1000));
    assertNotEquals(NOT_STARTED, relisting.start(OLD + 500 + RELIST_AFTER_MS + 1));
  }

  @Test
  public void voicesReplacedWhileListing_areNotOverwritten() {
    // Another engine, or a fuller list, replaces the voices while the old listing runs.
    VoiceRelisting relisting = listedAt(LISTED);
    int started = relisting.start(OLD);

    relisting.onReplaced();

    assertFalse(relisting.onEnded(started, /* listed= */ true, OLD + 500));
  }

  @Test
  public void voicesListedWhileListing_areNotOverwritten_andStayNew() {
    VoiceRelisting relisting = listedAt(LISTED);
    int started = relisting.start(OLD);

    // Speech listed the voices again itself, as when it looked for a voice.
    relisting.onListed(OLD + 200);

    assertFalse(relisting.onEnded(started, /* listed= */ true, OLD + 500));
    assertEquals(NOT_STARTED, relisting.start(OLD + 1000));
  }

  @Test
  public void replacedVoices_areListedAgainAtOnce_evenRightAfterTheDeviceStarts() {
    VoiceRelisting relisting = listedAt(/* time= */ 10);

    relisting.onReplaced();

    assertNotEquals(NOT_STARTED, relisting.start(/* now= */ 20));
  }

  @Test
  public void aListingIsCurrent_onlyWhileItRunsAndTheVoicesStay() {
    VoiceRelisting relisting = listedAt(LISTED);
    int listing = relisting.start(OLD);
    assertTrue(relisting.isCurrent(listing));

    relisting.onListed(OLD + 100);

    assertFalse(relisting.isCurrent(listing));
  }

  @Test
  public void cancelledListing_letsTheNextAskListAtOnce() {
    // The engine changed between asking for the listing and starting it.
    VoiceRelisting relisting = listedAt(LISTED);
    int listing = relisting.start(OLD);

    relisting.cancel(listing);

    assertNotEquals(NOT_STARTED, relisting.start(OLD + 1));
  }

  @Test
  public void aListingThatHangs_doesNotStopListingsAfterTheVoicesAreReplaced() {
    VoiceRelisting relisting = listedAt(LISTED);
    int hung = relisting.start(OLD);

    // Another engine is used while the listing still waits for the old one.
    relisting.onReplaced();
    int next = relisting.start(OLD + 1000);
    assertNotEquals(NOT_STARTED, next);

    // The old listing ending late neither counts nor ends the one that runs now.
    assertFalse(relisting.onEnded(hung, /* listed= */ true, OLD + 2000));
    assertEquals(NOT_STARTED, relisting.start(OLD + 3000));
    assertTrue(relisting.onEnded(next, /* listed= */ true, OLD + 4000));
  }
}
