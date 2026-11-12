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

import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.annotation.StringRes
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.libraries.accessibility.utils.log.LogUtils
import kotlin.math.roundToInt

/** Changes a volume stream one step for the stream volume reading controls. */
internal object StreamVolumeControl {
  private const val TAG = "StreamVolumeControl"

  private val STREAM_NAMES =
    mapOf(
      AudioManager.STREAM_MUSIC to R.string.value_stream_music,
      AudioManager.STREAM_VOICE_CALL to R.string.value_stream_voice_call,
      AudioManager.STREAM_RING to R.string.value_stream_ring,
      AudioManager.STREAM_NOTIFICATION to R.string.value_stream_notification,
      AudioManager.STREAM_ALARM to R.string.value_stream_alarm,
    )

  /** The volume of [stream] as a percentage of its range. */
  @JvmStatic
  fun percent(context: Context, stream: Int): Int {
    val audio = context.getSystemService(AudioManager::class.java) ?: return 0
    return percent(audio, stream)
  }

  /** The text that says the volume of [stream], such as "40 percent". */
  @JvmStatic
  fun percentText(context: Context, stream: Int): String =
    context.getString(R.string.tb_template_percent, percent(context, stream).toString())

  /**
   * Raises or lowers [stream] one step and returns the text to speak: the new volume, the limit
   * with the volume when it is at its lowest or highest, or that Do Not Disturb access is needed
   * when Android doesn't let the ring or notification volume change the ringer mode.
   */
  @JvmStatic
  fun change(context: Context, stream: Int, lower: Boolean): String {
    val audio = context.getSystemService(AudioManager::class.java) ?: return ""
    val min = minVolume(audio, stream)
    val max = audio.getStreamMaxVolume(stream)
    val volume = audio.getStreamVolume(stream)
    if (max <= min || (lower && volume <= min) || (!lower && volume >= max)) {
      return atLimit(
        context,
        if (lower) R.string.template_volume_change_minimum else R.string.template_volume_change_maximum,
        stream,
      )
    }
    try {
      // The reading control says the new volume, so the volume change isn't announced again.
      TalkBackService.getInstance()?.ignoreSelfVolumeChange(stream)
      audio.adjustStreamVolume(
        stream,
        if (lower) AudioManager.ADJUST_LOWER else AudioManager.ADJUST_RAISE,
        0,
      )
    } catch (e: SecurityException) {
      // Changing the ring or notification volume to or from silent changes Do Not Disturb.
      LogUtils.w(TAG, "Cannot change the volume of stream %d: %s", stream, e)
      return context.getString(R.string.stream_volume_needs_access)
    }
    return context.getString(
      R.string.template_stream_volume_set,
      context.getString(STREAM_NAMES[stream] ?: R.string.value_stream_music),
      percent(audio, stream),
    )
  }

  private fun atLimit(context: Context, @StringRes limit: Int, stream: Int): String =
    context.getString(
      R.string.template_setting_limit,
      context.getString(limit),
      percentText(context, stream),
    )

  private fun percent(audio: AudioManager, stream: Int): Int {
    val min = minVolume(audio, stream)
    val max = audio.getStreamMaxVolume(stream)
    val volume = audio.getStreamVolume(stream)
    return if (max > min) (100f * (volume - min) / (max - min)).roundToInt() else 100
  }

  private fun minVolume(audio: AudioManager, stream: Int): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) audio.getStreamMinVolume(stream) else 0
}
