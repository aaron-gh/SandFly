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

package com.google.android.accessibility.talkback.monitor

import android.media.AudioAttributes
import android.media.AudioManager
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Drops a volume step's tone while the previous tone is still sounding, so tones never queue. */
class VolumeToneGate {
  private var busyUntil = Long.MIN_VALUE

  /** Returns whether a tone may start at [now], and if so holds the gate for [busyMs]. */
  fun tryStart(now: Long, busyMs: Long): Boolean {
    if (now < busyUntil) {
      return false
    }
    busyUntil = now + busyMs
    return true
  }

  /** Lets the next tone start at once, as after a tone that failed to start. */
  fun clear() {
    busyUntil = Long.MIN_VALUE
  }
}

/**
 * Whether media plays, from the [usages] and [contentTypes] of the active players, in the same
 * order. When SandFly's own audio may have a media usage, [ownMediaAudio], players whose content is
 * speech or sonification are taken to be SandFly's, as its speech and sounds have those types.
 */
fun isMediaPlaying(usages: List<Int>, contentTypes: List<Int>, ownMediaAudio: Boolean): Boolean =
  usages.indices.any { i ->
    val usage = usages[i]
    val type = contentTypes.getOrElse(i) { AudioAttributes.CONTENT_TYPE_UNKNOWN }
    (usage == AudioAttributes.USAGE_MEDIA ||
      usage == AudioAttributes.USAGE_GAME ||
      usage == AudioAttributes.USAGE_UNKNOWN) &&
      (!ownMediaAudio ||
        (type != AudioAttributes.CONTENT_TYPE_SPEECH &&
          type != AudioAttributes.CONTENT_TYPE_SONIFICATION))
  }

/** The audio usage that makes the volume step tone play at the volume of [stream]. */
fun volumeToneUsage(stream: Int): Int =
  when (stream) {
    AudioManager.STREAM_VOICE_CALL -> AudioAttributes.USAGE_VOICE_COMMUNICATION
    AudioManager.STREAM_SYSTEM -> AudioAttributes.USAGE_ASSISTANCE_SONIFICATION
    AudioManager.STREAM_RING -> AudioAttributes.USAGE_NOTIFICATION_RINGTONE
    AudioManager.STREAM_ALARM -> AudioAttributes.USAGE_ALARM
    AudioManager.STREAM_NOTIFICATION -> AudioAttributes.USAGE_NOTIFICATION
    AudioManager.STREAM_DTMF -> AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING
    AudioManager.STREAM_ACCESSIBILITY -> AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
    else -> AudioAttributes.USAGE_MEDIA
  }

/**
 * The samples of the volume step tone, 16-bit mono: a sine wave of [frequencyHz] with [fadeMs] of
 * fade at each end, so that it starts and stops without a click.
 */
fun generateVolumeTone(
  sampleRate: Int,
  durationMs: Int,
  frequencyHz: Int,
  amplitude: Double,
  fadeMs: Int,
): ShortArray {
  val count = sampleRate * durationMs / 1000
  val fadeCount = sampleRate * fadeMs / 1000
  return ShortArray(count) { i ->
    val edge = minOf(i, count - 1 - i)
    val envelope = if (edge < fadeCount) (1 - cos(PI * edge / fadeCount)) / 2 else 1.0
    (sin(2 * PI * frequencyHz * i / sampleRate) * amplitude * envelope * Short.MAX_VALUE)
      .toInt()
      .toShort()
  }
}
