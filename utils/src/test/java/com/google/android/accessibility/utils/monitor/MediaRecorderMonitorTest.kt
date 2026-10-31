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

package com.google.android.accessibility.utils.monitor

import android.media.MediaRecorder.AudioSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaRecorderMonitorTest {
  @Test
  fun everySourceThatRecordsTheMicrophoneCounts() {
    for (source in
      listOf(
        AudioSource.DEFAULT,
        AudioSource.MIC,
        AudioSource.CAMCORDER,
        AudioSource.VOICE_RECOGNITION,
        AudioSource.VOICE_COMMUNICATION,
        AudioSource.UNPROCESSED,
        AudioSource.VOICE_PERFORMANCE,
      )) {
      assertTrue("source $source", MediaRecorderMonitor.recordsMicrophone(source, false))
    }
  }

  @Test
  fun aSilencedRecordingDoesNotCount() {
    assertFalse(MediaRecorderMonitor.recordsMicrophone(AudioSource.MIC, true))
  }

  @Test
  fun recordingWhatThePhonePlaysDoesNotCount() {
    assertFalse(MediaRecorderMonitor.recordsMicrophone(AudioSource.REMOTE_SUBMIX, false))
  }
}
