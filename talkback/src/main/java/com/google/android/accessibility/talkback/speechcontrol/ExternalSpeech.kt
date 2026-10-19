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

package com.google.android.accessibility.talkback.speechcontrol

import android.os.Bundle
import com.google.android.accessibility.utils.output.FailoverTextToSpeech.SpeechParam
import com.google.android.accessibility.utils.output.FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_AUDIO_PLAYBACK_ACTIVE
import com.google.android.accessibility.utils.output.SpeechController.QueueMode
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions
import com.google.android.accessibility.utils.output.SpeechControllerImpl

/**
 * Speech options for text that a script or another app asks Backtalk to say. It keeps its own rate
 * and pitch, is said as given, without Backtalk's speech filter, and is said over media.
 */
object ExternalSpeech {
  @JvmStatic
  fun params(rate: Float, pitch: Float, volume: Float? = null): Bundle =
    Bundle().apply {
      putBoolean(SpeechControllerImpl.SPEECH_PARAM_FROM_SCRIPT, true)
      putFloat(SpeechParam.RATE, rate)
      putFloat(SpeechParam.PITCH, pitch)
      volume?.let { putFloat(SpeechParam.VOLUME, it) }
    }

  @JvmStatic
  fun options(@QueueMode queueMode: Int, params: Bundle, extraFlags: Int = 0): SpeakOptions =
    SpeakOptions.create()
      .setQueueMode(queueMode)
      .setFlags(FLAG_FORCE_FEEDBACK_EVEN_IF_AUDIO_PLAYBACK_ACTIVE or extraFlags)
      .setSpeechParams(params)
}
