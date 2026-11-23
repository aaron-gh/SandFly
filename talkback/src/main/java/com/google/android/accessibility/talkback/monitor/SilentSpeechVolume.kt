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

import android.media.AudioManager

/**
 * Volume keys > Change speech volume silently while speaking: a change of the speech volume that
 * follows a volume key press made while SandFly speaks or has speech queued gets no spoken volume.
 * A press within [SERIES_MS] of the press before it keeps that press's decision when it was silent,
 * so a series of presses, or a held key, stays silent after speech ends. Presses while a finger is
 * on the screen, changes with no press before them and changes of other streams are announced.
 */
class SilentSpeechVolume {
  /** The switch. */
  var enabled = false

  private var pressedAt = NEVER
  private var silent = false

  /**
   * Records a volume key press, or a repeat of a held key, at [now]: [speaking] whether SandFly
   * speaks or has speech queued, not counting its spoken volume, and [touching] whether a finger
   * is on the screen.
   */
  fun onVolumeKeyDown(now: Long, speaking: Boolean, touching: Boolean) {
    val inSeries = pressedAt != NEVER && now - pressedAt < SERIES_MS
    silent = enabled && !touching && (speaking || (inSeries && silent))
    pressedAt = now
  }

  /**
   * Whether a change of [stream] reported at [now] gets no spoken volume: it is the speech volume
   * [speechStream] and follows a silent press.
   */
  fun isSilent(stream: Int, speechStream: Int, now: Long): Boolean =
    enabled && silent && stream == speechStream && pressedAt != NEVER && now - pressedAt < SERIES_MS

  companion object {
    /** As ProcessorVolumeStream: a press this soon after the one before it belongs to its series. */
    const val SERIES_MS = 1_000L

    private const val NEVER = Long.MIN_VALUE

    /**
     * The stream whose volume speech plays at: [speechStream], or the media stream for the
     * accessibility stream on devices without one, [hasAccessibilityStream].
     */
    @JvmStatic
    fun speechVolumeStream(speechStream: Int, hasAccessibilityStream: Boolean): Int =
      if (speechStream == AudioManager.STREAM_ACCESSIBILITY && !hasAccessibilityStream) {
        AudioManager.STREAM_MUSIC
      } else {
        speechStream
      }
  }
}
