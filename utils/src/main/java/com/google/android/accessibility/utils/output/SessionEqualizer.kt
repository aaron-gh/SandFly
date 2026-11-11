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

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.DynamicsProcessing
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.libraries.accessibility.utils.log.LogUtils
import kotlin.math.pow

/**
 * The speech equalizer as an audio effect on an audio session, for speech the engine plays itself
 * rather than [LowLatencyAudio]. The engine plays speech on the session when it is given as
 * `KEY_PARAM_SESSION_ID`, as Android's text-to-speech playback does. An engine that passes speech
 * on to other engines applies it only if it passes the session on as well.
 *
 * The effect's bands follow the response of [SpeechEqualizer.Filter], so that speech sounds the same
 * either way.
 */
class SessionEqualizer private constructor(val sessionId: Int, private val effect: DynamicsProcessing) {
  private var gains: IntArray? = null

  /** Sets the effect to [gains]. Returns false if the effect no longer works. */
  @RequiresApi(Build.VERSION_CODES.P)
  fun setGains(gains: IntArray): Boolean {
    if (gains.contentEquals(this.gains)) return true
    return try {
      effect.setPreEqAllChannelsTo(eq(gains))
      effect.setLimiterAllChannelsTo(limiter(gains.any { it > 0 }))
      if (!effect.enabled) effect.enabled = true
      this.gains = gains.copyOf()
      true
    } catch (e: RuntimeException) {
      // Such as after the audio server restarted, which leaves the effect dead.
      LogUtils.w(TAG, "Equalizer effect failed: %s", e)
      false
    }
  }

  fun release() {
    try {
      effect.release()
    } catch (e: RuntimeException) {
      // Already gone.
    }
  }

  companion object {
    private const val TAG = "SessionEqualizer"

    // Third-octave bands from 20 Hz to 20 kHz.
    private const val FIRST_BAND = -17
    private const val BAND_COUNT = 31
    private const val TOP_CUTOFF_HZ = 24_000f
    private const val RESPONSE_RATE = 48_000
    private const val CHANNELS = 2
    private const val FRAME_MS = 10f
    private const val LIMITER_ATTACK_MS = 1f
    private const val LIMITER_RELEASE_MS = 80f
    private const val LIMITER_RATIO = 10f
    // About the ceiling of SpeechEqualizer.Filter's limiter.
    private const val LIMITER_THRESHOLD_DB = -0.2f

    /** Whether Android has the effect, so that the equalizer works without [LowLatencyAudio]. */
    @JvmStatic fun isAvailable(): Boolean = available

    private val available by lazy {
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
        try {
          AudioEffect.queryEffects()?.any {
            it.type == AudioEffect.EFFECT_TYPE_DYNAMICS_PROCESSING
          } == true
        } catch (e: RuntimeException) {
          false
        }
    }

    /** Makes the effect on a new audio session, or returns null if Android can't. */
    @JvmStatic
    @RequiresApi(Build.VERSION_CODES.P)
    fun create(context: Context): SessionEqualizer? {
      val audioManager = context.getSystemService(AudioManager::class.java) ?: return null
      val session = audioManager.generateAudioSessionId()
      if (session <= 0) return null
      return try {
        val config =
          DynamicsProcessing.Config.Builder(
              DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
              CHANNELS,
              /* preEqInUse= */ true,
              BAND_COUNT,
              /* mbcInUse= */ false,
              0,
              /* postEqInUse= */ false,
              0,
              /* limiterInUse= */ true,
            )
            .setPreferredFrameDuration(FRAME_MS)
            .build()
        SessionEqualizer(session, DynamicsProcessing(0, session, config))
      } catch (e: RuntimeException) {
        LogUtils.w(TAG, "No equalizer effect: %s", e)
        null
      }
    }

    private fun centre(band: Int): Double = 1000.0 * 2.0.pow((FIRST_BAND + band) / 3.0)

    @RequiresApi(Build.VERSION_CODES.P)
    private fun eq(gains: IntArray): DynamicsProcessing.Eq {
      val eq = DynamicsProcessing.Eq(true, true, BAND_COUNT)
      for (band in 0 until BAND_COUNT) {
        val centre = centre(band)
        val cutoff =
          if (band == BAND_COUNT - 1) TOP_CUTOFF_HZ else (centre * 2.0.pow(1 / 6.0)).toFloat()
        val gain = SpeechEqualizer.responseDb(gains, centre, RESPONSE_RATE).toFloat()
        eq.setBand(band, DynamicsProcessing.EqBand(true, cutoff, gain))
      }
      return eq
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun limiter(enabled: Boolean) =
      DynamicsProcessing.Limiter(
        true,
        enabled,
        0,
        LIMITER_ATTACK_MS,
        LIMITER_RELEASE_MS,
        LIMITER_RATIO,
        LIMITER_THRESHOLD_DB,
        0f,
      )
  }
}
