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

package com.google.android.accessibility.utils.output

import android.media.SoundPool
import android.os.Vibrator
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.R
import java.time.Duration
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OutputMuteTest {

  @After
  fun unmute() {
    OutputMute.releaseOwner(OWNER, "test")
    OutputMute.releaseOwner(OTHER, "test")
  }

  @Test
  fun onlyWhatIsNamedIsMuted() {
    OutputMute.start(OWNER, APP, speech = true, sounds = true, vibration = false, ms = 3_000)
    assertTrue(OutputMute.isSpeechMuted())
    assertTrue(OutputMute.areSoundsMuted())
    assertFalse(OutputMute.isVibrationMuted())
  }

  @Test
  fun aMuteEndsAfterItsTimeAndAtMostTenSeconds() {
    OutputMute.start(OWNER, APP, speech = true, sounds = false, vibration = false, ms = 3_000)
    ShadowSystemClock.advanceBy(Duration.ofMillis(2_999))
    assertTrue(OutputMute.isSpeechMuted())
    ShadowSystemClock.advanceBy(Duration.ofMillis(1))
    assertFalse(OutputMute.isSpeechMuted())

    OutputMute.start(OWNER, APP, speech = true, sounds = false, vibration = false, ms = 60_000)
    ShadowSystemClock.advanceBy(Duration.ofMillis(OutputMute.MAX_MS))
    assertFalse(OutputMute.isSpeechMuted())
  }

  @Test
  fun aHandleReleasesOnlyItsOwnMute() {
    val first = OutputMute.start(OWNER, APP, speech = true, sounds = false, vibration = false, ms = 3_000)
    OutputMute.start(OTHER, APP, speech = true, sounds = false, vibration = false, ms = 3_000)
    OutputMute.release(first)
    assertTrue(OutputMute.isSpeechMuted())
    OutputMute.releaseOwner(OWNER, "test")
    assertTrue(OutputMute.isSpeechMuted())
    OutputMute.releaseOwner(OTHER, "test")
    assertFalse(OutputMute.isSpeechMuted())
  }

  @Test
  fun userInputEndsItButAScriptsOwnInputDoesNot() {
    OutputMute.start(OWNER, APP, speech = true, sounds = true, vibration = true, ms = 3_000)
    OutputMute.scriptInputStarted()
    OutputMute.onUserInput("touch")
    assertTrue(OutputMute.isSpeechMuted())
    OutputMute.scriptInputEnded()
    OutputMute.onUserInput("key")
    assertFalse(OutputMute.isSpeechMuted())
    assertFalse(OutputMute.areSoundsMuted())
    assertFalse(OutputMute.isVibrationMuted())
  }

  @Test
  fun anotherAppInFrontEndsIt() {
    OutputMute.start(OWNER, APP, speech = true, sounds = false, vibration = false, ms = 3_000)
    OutputMute.onAppChanged(APP)
    assertTrue(OutputMute.isSpeechMuted())
    OutputMute.onAppChanged("com.android.launcher")
    assertFalse(OutputMute.isSpeechMuted())
  }

  @Test
  fun mutedVibrationIsDropped() {
    val context = RuntimeEnvironment.getApplication()
    val controller =
      FeedbackController(context, SoundPool.Builder().build(), context.getSystemService(Vibrator::class.java))
    controller.setHapticEnabled(true)
    assertTrue(controller.playHaptic(R.array.window_state_pattern, EVENT_ID_UNTRACKED))
    OutputMute.start(OWNER, APP, speech = false, sounds = false, vibration = true, ms = 3_000)
    assertFalse(controller.playHaptic(R.array.window_state_pattern, EVENT_ID_UNTRACKED))
    OutputMute.releaseOwner(OWNER, "test")
    assertTrue(controller.playHaptic(R.array.window_state_pattern, EVENT_ID_UNTRACKED))
  }

  private companion object {
    const val OWNER = "script.one"
    const val OTHER = "script.two"
    const val APP = "com.whatsapp"
  }
}
