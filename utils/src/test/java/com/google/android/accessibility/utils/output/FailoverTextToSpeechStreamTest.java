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

import android.media.AudioAttributes;
import android.media.AudioManager;
import org.junit.Test;

public class FailoverTextToSpeechStreamTest {

  @Test
  public void audioStreamOf_mapsTheVolumes() {
    assertEquals(
        AudioManager.STREAM_ACCESSIBILITY,
        FailoverTextToSpeech.audioStreamOf(FailoverTextToSpeech.STREAM_VALUE_ACCESSIBILITY));
    assertEquals(
        AudioManager.STREAM_MUSIC,
        FailoverTextToSpeech.audioStreamOf(FailoverTextToSpeech.STREAM_VALUE_MEDIA));
    assertEquals(
        AudioManager.STREAM_RING,
        FailoverTextToSpeech.audioStreamOf(FailoverTextToSpeech.STREAM_VALUE_RING));
    assertEquals(
        AudioManager.STREAM_VOICE_CALL,
        FailoverTextToSpeech.audioStreamOf(FailoverTextToSpeech.STREAM_VALUE_CALL));
  }

  @Test
  public void audioStreamOf_anythingElse_isAccessibility() {
    assertEquals(AudioManager.STREAM_ACCESSIBILITY, FailoverTextToSpeech.audioStreamOf(null));
    assertEquals(AudioManager.STREAM_ACCESSIBILITY, FailoverTextToSpeech.audioStreamOf("speech"));
  }

  @Test
  public void usageForStream_ringPlaysLikeARingtone() {
    assertEquals(
        AudioAttributes.USAGE_NOTIFICATION_RINGTONE,
        FailoverTextToSpeech.usageForStream(FailoverTextToSpeech.STREAM_VALUE_RING));
    assertEquals(
        AudioAttributes.USAGE_MEDIA,
        FailoverTextToSpeech.usageForStream(FailoverTextToSpeech.STREAM_VALUE_MEDIA));
    assertEquals(
        AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY,
        FailoverTextToSpeech.usageForStream(null));
  }
}
