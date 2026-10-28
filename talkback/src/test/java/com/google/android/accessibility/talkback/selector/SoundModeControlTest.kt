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

package com.google.android.accessibility.talkback.selector

import android.app.NotificationManager
import android.media.AudioManager
import android.os.Vibrator
import android.provider.Settings
import com.google.android.accessibility.talkback.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SoundModeControlTest {
  private val context = RuntimeEnvironment.getApplication()
  private val audio = context.getSystemService(AudioManager::class.java)

  @Before
  fun setUp() {
    shadowOf(context.getSystemService(Vibrator::class.java)).setHasVibrator(true)
    setAccess(true)
    audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
  }

  @Test
  fun next_goesFromSoundToVibrateToMuteAndWraps() {
    assertEquals(context.getString(R.string.quick_settings_vibrate), change(next = true))
    assertEquals(AudioManager.RINGER_MODE_VIBRATE, audio.ringerMode)
    assertEquals(context.getString(R.string.quick_settings_mute), change(next = true))
    assertEquals(AudioManager.RINGER_MODE_SILENT, audio.ringerMode)
    assertEquals(context.getString(R.string.quick_settings_sound), change(next = true))
    assertEquals(AudioManager.RINGER_MODE_NORMAL, audio.ringerMode)
  }

  @Test
  fun previous_wrapsFromSoundToMute() {
    change(next = false)
    assertEquals(AudioManager.RINGER_MODE_SILENT, audio.ringerMode)
  }

  @Test
  fun withoutAVibrator_vibrateIsSkipped() {
    shadowOf(context.getSystemService(Vibrator::class.java)).setHasVibrator(false)
    change(next = true)
    assertEquals(AudioManager.RINGER_MODE_SILENT, audio.ringerMode)
  }

  @Test
  fun withoutAccess_muteIsNotSetAndTheAccessSettingOpens() {
    setAccess(false)
    audio.ringerMode = AudioManager.RINGER_MODE_VIBRATE
    assertEquals(context.getString(R.string.sound_mode_needs_access), change(next = true))
    assertEquals(AudioManager.RINGER_MODE_VIBRATE, audio.ringerMode)
    assertEquals(
      Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS,
      shadowOf(context).nextStartedActivity.action,
    )
  }

  @Test
  fun withoutAccess_soundAndVibrateStillChange() {
    setAccess(false)
    change(next = true)
    assertEquals(AudioManager.RINGER_MODE_VIBRATE, audio.ringerMode)
    change(next = false)
    assertEquals(AudioManager.RINGER_MODE_NORMAL, audio.ringerMode)
    assertNull(shadowOf(context).nextStartedActivity)
  }

  @Test
  fun withoutAccess_muteIsKept() {
    audio.ringerMode = AudioManager.RINGER_MODE_SILENT
    setAccess(false)
    assertEquals(context.getString(R.string.sound_mode_needs_access), change(next = true))
    assertEquals(AudioManager.RINGER_MODE_SILENT, audio.ringerMode)
  }

  private fun change(next: Boolean) = SoundModeControl.change(context, next)

  private fun setAccess(granted: Boolean) {
    shadowOf(context.getSystemService(NotificationManager::class.java))
      .setNotificationPolicyAccessGranted(granted)
  }
}
