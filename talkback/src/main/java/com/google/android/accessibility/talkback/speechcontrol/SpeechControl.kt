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

import android.app.KeyguardManager
import android.content.Context
import android.os.SystemClock
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.pause.PauseController
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.FeedbackItem.FLAG_CLEAR_QUEUED_UTTERANCES_WITH_SAME_UTTERANCE_GROUP
import com.google.android.accessibility.utils.output.FeedbackItem.FLAG_INTERRUPT_CURRENT_UTTERANCE_WITH_SAME_UTTERANCE_GROUP
import com.google.android.accessibility.utils.output.SpeechController.QUEUE_MODE_QUEUE
import com.google.android.accessibility.utils.output.SpeechController.UTTERANCE_GROUP_OTHER_APPS
import com.google.android.accessibility.utils.output.SpeechControllerImpl
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Lets apps the user chose speak through Backtalk, in the user's voice and settings, from
 * [SpeechControlReceiver] and [SpeechControlService]. See docs/speech-api.md.
 *
 * Speech from other apps can't be turned against the user:
 * - Nothing is said unless Let other apps speak is on and the user allowed that app, which
 *   Backtalk asks about the first time the app tries.
 * - A sound can play before each thing an app says, so it can't pass for Backtalk.
 * - Nothing is said while the device is locked, or while Backtalk is off or paused.
 * - Each app can speak a few times at once, then about once a second.
 * - Apps can replace or stop only speech from other apps, never Backtalk's own.
 *
 * Everything runs on the main thread.
 */
object SpeechControl {
  private const val TAG = "SpeechControl"

  const val PERMISSION = "fyi.quin.backtalk.permission.SPEAK"

  const val ACTION_SPEAK = "fyi.quin.backtalk.action.SPEAK"
  const val ACTION_STOP = "fyi.quin.backtalk.action.STOP_SPEECH"

  const val EXTRA_TEXT = "text"
  const val EXTRA_INTERRUPT = "interrupt"
  const val EXTRA_RATE = "rate"
  const val EXTRA_PITCH = "pitch"
  const val EXTRA_VOLUME = "volume"

  /** Must match pref_speech_control_key. */
  const val PREF_ENABLED = "pref_speech_control"
  const val PREF_SOUND = "pref_speech_control_sound"

  private var context: Context? = null
  private var feedback: Pipeline.FeedbackReturner? = null
  private var stopOtherAppsSpeech: Runnable? = null
  private val rateLimiter = SpeechControlPolicy.RateLimiter()

  /** Called by Backtalk when it starts, so that apps can speak through it. */
  @JvmStatic
  @MainThread
  fun attach(
    context: Context,
    feedback: Pipeline.FeedbackReturner,
    speechController: SpeechControllerImpl,
  ) {
    attach(context, feedback) { speechController.stopUtteranceGroup(UTTERANCE_GROUP_OTHER_APPS) }
  }

  /** As [attach], with how to stop what other apps are saying, which tests replace. */
  @VisibleForTesting
  fun attach(context: Context, feedback: Pipeline.FeedbackReturner, stopOtherAppsSpeech: Runnable) {
    this.context = context.applicationContext
    this.feedback = feedback
    this.stopOtherAppsSpeech = stopOtherAppsSpeech
  }

  /** Called by Backtalk when it stops. */
  @JvmStatic
  @MainThread
  fun detach() {
    context = null
    feedback = null
    stopOtherAppsSpeech = null
    rateLimiter.clear()
  }

  fun isEnabled(context: Context): Boolean =
    SharedPreferencesUtils.getSharedPreferences(context).getBoolean(PREF_ENABLED, false)

  /** Whether a sound plays before each thing another app says. Off unless the user turns it on. */
  fun playsSound(context: Context): Boolean =
    SharedPreferencesUtils.getSharedPreferences(context).getBoolean(PREF_SOUND, false)

  /**
   * Says [text] for the first of [apps], the caller's packages, that the user allowed, and asks
   * the user about the caller if they haven't answered. Returns whether the text was passed on to
   * be said.
   */
  @MainThread
  fun speak(
    apps: List<String>,
    text: CharSequence?,
    interrupt: Boolean,
    rate: Float = 1f,
    pitch: Float = 1f,
    volume: Float = 1f,
  ): Boolean {
    val app = allowedApp(apps) ?: return false
    val feedback = feedback ?: return false
    val context = context ?: return false
    if (context.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true) {
      LogUtils.i(TAG, "Ignored speech from %s, since the device is locked", app)
      return false
    }
    val say = SpeechControlPolicy.textToSay(text) ?: return false
    if (!rateLimiter.tryAcquire(app, SystemClock.uptimeMillis())) {
      LogUtils.i(TAG, "Ignored speech from %s, since it is speaking too often", app)
      return false
    }
    val options =
      ExternalSpeech.options(
        QUEUE_MODE_QUEUE,
        ExternalSpeech.params(
          SpeechControlPolicy.clampScale(rate),
          SpeechControlPolicy.clampScale(pitch),
          SpeechControlPolicy.clampVolume(volume),
        ),
        // Replaces only what other apps were saying, never Backtalk's own speech.
        if (interrupt) {
          FLAG_CLEAR_QUEUED_UTTERANCES_WITH_SAME_UTTERANCE_GROUP or
            FLAG_INTERRUPT_CURRENT_UTTERANCE_WITH_SAME_UTTERANCE_GROUP
        } else {
          0
        },
      )
    options.setUtteranceGroup(UTTERANCE_GROUP_OTHER_APPS)
    val speech = Feedback.speech(say, options)
    // The sound marks the speech as another app's.
    if (playsSound(context)) speech.sound(R.raw.chime_up)
    return feedback.returnFeedback(EVENT_ID_UNTRACKED, speech)
  }

  /** Stops what other apps are saying, for the first of [apps] that the user allowed. */
  @MainThread
  fun stop(apps: List<String>): Boolean {
    allowedApp(apps) ?: return false
    val stop = stopOtherAppsSpeech ?: return false
    stop.run()
    return true
  }

  /**
   * The first of [apps] that may speak now, or null. Asks the user about the first one they
   * haven't answered for.
   */
  private fun allowedApp(apps: List<String>): String? {
    val context = context ?: return null
    if (!isEnabled(context)) {
      LogUtils.i(TAG, "Ignored a request, since other apps may not speak")
      return null
    }
    if (PauseController.isPaused()) return null
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val allowed =
      apps.firstOrNull { SpeechControlApps.state(prefs, it) == SpeechControlApps.State.ALLOWED }
    if (allowed != null) return allowed
    apps
      .firstOrNull { SpeechControlApps.state(prefs, it) == SpeechControlApps.State.UNKNOWN }
      ?.let { SpeechControlApproval.ask(context, it) }
    return null
  }
}
