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

import android.content.Context
import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.os.SystemClock
import android.telephony.TelephonyManager
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechController
import java.util.Locale

/**
 * Says how long a phone call lasted when it ends, with Announce call length. The length runs from
 * when the phone goes off hook, which is when an incoming call is answered or an outgoing call is
 * dialed, to when it is idle again. Calls that ring and end without being answered are not said.
 * With a second call waiting, the length covers both calls until the last one ends.
 */
class CallDurationAnnouncer(
  private val context: Context,
  private val speak: (String) -> Unit,
  private val clock: () -> Long = SystemClock::elapsedRealtime,
) : CallStateMonitor.CallStateChangedListener {

  /** When the current call went off hook, or null without a call that went off hook. */
  private var offHookAt: Long? = null

  override fun onCallStateChanged(oldState: Int, newState: Int) {
    // A call that started and ended while call state was not followed leaves nothing behind.
    if (oldState == TelephonyManager.CALL_STATE_IDLE) {
      offHookAt = null
    }
    when (newState) {
      TelephonyManager.CALL_STATE_OFFHOOK -> if (offHookAt == null) offHookAt = clock()
      TelephonyManager.CALL_STATE_IDLE -> {
        val start = offHookAt ?: return
        offHookAt = null
        if (isOn(context)) {
          val seconds = ((clock() - start) / 1000).coerceAtLeast(0)
          speak(context.getString(R.string.template_call_ended, formatDuration(seconds)))
        }
      }
    }
  }

  companion object {
    private fun isOn(context: Context): Boolean =
      SharedPreferencesUtils.getBooleanPref(
        SharedPreferencesUtils.getSharedPreferences(context),
        context.resources,
        R.string.pref_announce_call_length_key,
        R.bool.pref_announce_call_length_default,
      )

    /** Creates one that speaks through [pipeline], so that the in-call screen closing can't cut it. */
    @JvmStatic
    fun create(context: Context, pipeline: Pipeline.FeedbackReturner): CallDurationAnnouncer =
      CallDurationAnnouncer(context, { text ->
        pipeline.returnFeedback(
          EVENT_ID_UNTRACKED,
          Feedback.speech(
            text,
            SpeechController.SpeakOptions.create()
              .setQueueMode(SpeechController.QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH)
              .setFlags(
                FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_AUDIO_PLAYBACK_ACTIVE or
                  FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_PHONE_CALL_ACTIVE
              ),
          ),
        )
      })

    /** Says [seconds] in hours, minutes and seconds, such as "3 minutes, 12 seconds". */
    @JvmStatic
    @JvmOverloads
    fun formatDuration(seconds: Long, locale: Locale = Locale.getDefault()): String {
      val hours = seconds / 3600
      val minutes = seconds % 3600 / 60
      val rest = seconds % 60
      val parts = buildList {
        if (hours > 0) add(Measure(hours, MeasureUnit.HOUR))
        if (minutes > 0) add(Measure(minutes, MeasureUnit.MINUTE))
        if (rest > 0 || isEmpty()) add(Measure(rest, MeasureUnit.SECOND))
      }
      return MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.WIDE)
        .formatMeasures(*parts.toTypedArray())
    }
  }
}
