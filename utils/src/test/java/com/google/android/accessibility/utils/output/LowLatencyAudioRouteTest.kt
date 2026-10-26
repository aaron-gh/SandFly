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

package com.google.android.accessibility.utils.output

import android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
import android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO
import android.media.AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
import android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
import android.media.AudioManager.MODE_IN_CALL
import android.media.AudioManager.MODE_IN_COMMUNICATION
import android.media.AudioManager.MODE_NORMAL
import com.google.android.accessibility.utils.output.LowLatencyAudio.Companion.sendToMedia
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LowLatencyAudioRouteTest {
  @Test
  fun earpieceAfterCallWithEarbuds_goesToEarbuds() {
    assertTrue(sendToMedia(MODE_NORMAL, TYPE_BUILTIN_EARPIECE, null, TYPE_BLUETOOTH_A2DP, false))
  }

  @Test
  fun earpieceAfterCallWithoutEarbuds_goesToSpeaker() {
    assertTrue(sendToMedia(MODE_NORMAL, TYPE_BUILTIN_EARPIECE, null, TYPE_BUILTIN_SPEAKER, false))
  }

  @Test
  fun trackOnEarpieceWhileAndroidReportsEarbuds_goesToEarbuds() {
    assertTrue(sendToMedia(MODE_NORMAL, TYPE_BLUETOOTH_A2DP, TYPE_BUILTIN_EARPIECE, TYPE_BLUETOOTH_A2DP, false))
  }

  @Test
  fun keptAfterTrackLeftTheEarpiece_whileSticky() {
    assertTrue(sendToMedia(MODE_NORMAL, TYPE_BLUETOOTH_A2DP, TYPE_BLUETOOTH_A2DP, TYPE_BLUETOOTH_A2DP, true))
  }

  @Test
  fun androidRoutesToMedia_leftAlone() {
    assertFalse(sendToMedia(MODE_NORMAL, TYPE_BLUETOOTH_A2DP, TYPE_BLUETOOTH_A2DP, TYPE_BLUETOOTH_A2DP, false))
    assertFalse(sendToMedia(MODE_NORMAL, TYPE_BUILTIN_SPEAKER, null, TYPE_BUILTIN_SPEAKER, false))
  }

  @Test
  fun duringCall_leftAlone() {
    assertFalse(sendToMedia(MODE_IN_CALL, TYPE_BUILTIN_EARPIECE, TYPE_BUILTIN_EARPIECE, TYPE_BLUETOOTH_A2DP, true))
    assertFalse(sendToMedia(MODE_IN_COMMUNICATION, TYPE_BLUETOOTH_SCO, null, TYPE_BLUETOOTH_A2DP, false))
  }

  @Test
  fun mediaOnEarpieceOrUnknown_leftAlone() {
    assertFalse(sendToMedia(MODE_NORMAL, TYPE_BUILTIN_EARPIECE, null, TYPE_BUILTIN_EARPIECE, false))
    assertFalse(sendToMedia(MODE_NORMAL, TYPE_BUILTIN_EARPIECE, null, null, true))
  }
}
