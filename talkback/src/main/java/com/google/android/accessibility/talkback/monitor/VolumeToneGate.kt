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
 * The audio usage of the volume step tone: [speechUsage], except on One UI when that is
 * accessibility. One UI adjusts the accessibility stream with the volume keys while that stream is
 * active and for 1 s after, so a tone at accessibility usage would change which stream the next
 * step adjusts.
 */
fun volumeToneUsage(speechUsage: Int, oneUi: Boolean): Int =
  if (oneUi && speechUsage == AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY) {
    AudioAttributes.USAGE_MEDIA
  } else {
    speechUsage
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
