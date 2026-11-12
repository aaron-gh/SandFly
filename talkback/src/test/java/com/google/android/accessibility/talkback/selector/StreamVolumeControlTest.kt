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

import android.media.AudioManager
import com.google.android.accessibility.talkback.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StreamVolumeControlTest {
  private val context = RuntimeEnvironment.getApplication()
  private val audio = context.getSystemService(AudioManager::class.java)

  @Before
  fun setUp() {
    shadowOf(audio).setStreamMinVolume(0)
    shadowOf(audio).setStreamMaxVolume(10)
    audio.setStreamVolume(AudioManager.STREAM_ALARM, 4, 0)
    audio.setStreamVolume(AudioManager.STREAM_MUSIC, 4, 0)
  }

  @Test
  fun raise_changesOnlyThatStreamOneStepAndSaysThePercentage() {
    assertEquals(
      setText(R.string.value_stream_alarm, 50),
      StreamVolumeControl.change(context, AudioManager.STREAM_ALARM, /* lower= */ false),
    )
    assertEquals(5, audio.getStreamVolume(AudioManager.STREAM_ALARM))
    assertEquals(4, audio.getStreamVolume(AudioManager.STREAM_MUSIC))
  }

  @Test
  fun lower_changesTheStreamOneStep() {
    assertEquals(
      setText(R.string.value_stream_music, 30),
      StreamVolumeControl.change(context, AudioManager.STREAM_MUSIC, /* lower= */ true),
    )
    assertEquals(3, audio.getStreamVolume(AudioManager.STREAM_MUSIC))
  }

  @Test
  fun atTheLowest_saysMinimumAndTheVolume() {
    audio.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0)
    assertEquals(
      limitText(R.string.template_volume_change_minimum, 0),
      StreamVolumeControl.change(context, AudioManager.STREAM_ALARM, /* lower= */ true),
    )
    assertEquals(0, audio.getStreamVolume(AudioManager.STREAM_ALARM))
  }

  @Test
  fun atTheHighest_saysMaximumAndTheVolume() {
    audio.setStreamVolume(AudioManager.STREAM_ALARM, 10, 0)
    assertEquals(
      limitText(R.string.template_volume_change_maximum, 100),
      StreamVolumeControl.change(context, AudioManager.STREAM_ALARM, /* lower= */ false),
    )
    assertEquals(10, audio.getStreamVolume(AudioManager.STREAM_ALARM))
  }

  @Test
  fun percentText_isTheVolumeAsAPercentageOfItsRange() {
    assertEquals(
      context.getString(R.string.tb_template_percent, "40"),
      StreamVolumeControl.percentText(context, AudioManager.STREAM_ALARM),
    )
  }

  private fun setText(streamName: Int, percent: Int) =
    context.getString(R.string.template_stream_volume_set, context.getString(streamName), percent)

  private fun limitText(limit: Int, percent: Int) =
    context.getString(
      R.string.template_setting_limit,
      context.getString(limit),
      context.getString(R.string.tb_template_percent, percent.toString()),
    )
}
