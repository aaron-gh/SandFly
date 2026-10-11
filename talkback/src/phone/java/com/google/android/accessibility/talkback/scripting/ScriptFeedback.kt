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

package com.google.android.accessibility.talkback.scripting

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.SpannableString
import android.text.Spanned
import android.text.style.LocaleSpan
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.SpeechController.QUEUE_MODE_INTERRUPT
import com.google.android.accessibility.utils.output.SpeechController.QUEUE_MODE_QUEUE
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions
import com.google.android.accessibility.utils.output.FailoverTextToSpeech.SpeechParam
import com.google.android.accessibility.utils.output.FeedbackItem
import com.google.android.accessibility.utils.output.SpeechControllerImpl
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

class ScriptFeedback(
  private val service: AccessibilityService,
  private val pipeline: Pipeline.FeedbackReturner,
  private val resumeBacktalk: Runnable,
) {
  private val mainHandler = Handler(Looper.getMainLooper())
  // Sounds scripts are playing now. Android gives an app only so many audio tracks, and Backtalk's
  // own sounds come out of the same allowance.
  private val playing = AtomicInteger()

  @Volatile var paused = false

  class Voice(val rate: Float = 1f, val pitch: Float = 1f, val language: String? = null)

  fun speak(text: String, interrupt: Boolean, voice: Voice = Voice()) {
    val params =
      Bundle().apply {
        putBoolean(SpeechControllerImpl.SPEECH_PARAM_FROM_SCRIPT, true)
        putFloat(SpeechParam.RATE, voice.rate)
        putFloat(SpeechParam.PITCH, voice.pitch)
      }
    val options =
      SpeakOptions.create()
        .setQueueMode(if (interrupt) QUEUE_MODE_INTERRUPT else QUEUE_MODE_QUEUE)
        .setFlags(FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_AUDIO_PLAYBACK_ACTIVE)
        .setSpeechParams(params)
    give(Feedback.speech(inLanguage(text, voice.language), options))
  }

  fun playAudio(samples: FloatArray, sampleRate: Int, volume: Float) {
    if (!soundOn() || !startPlaying()) return
    val pcm =
      ShortArray(samples.size) {
        (samples[it].coerceIn(-1f, 1f) * volume * Short.MAX_VALUE).roundToInt().toShort()
      }
    val track =
      try {
        AudioTrack.Builder()
          .setAudioAttributes(SONIFICATION)
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(sampleRate)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setTransferMode(AudioTrack.MODE_STATIC)
          .setBufferSizeInBytes(pcm.size * 2)
          .build()
      } catch (e: RuntimeException) {
        LogUtils.e(TAG, "Could not play script audio: %s", e)
        playing.decrementAndGet()
        return
      }
    val release = {
      track.release()
      playing.decrementAndGet()
    }
    try {
      track.write(pcm, 0, pcm.size)
      track.play()
      mainHandler.postDelayed(
        { release() },
        pcm.size * 1000L / sampleRate + RELEASE_DELAY_MS,
      )
    } catch (e: IllegalStateException) {
      LogUtils.e(TAG, "Could not play script audio: %s", e)
      release()
    }
  }

  private fun startPlaying(): Boolean {
    if (playing.incrementAndGet() <= MAX_PLAYING) return true
    playing.decrementAndGet()
    LogUtils.w(TAG, "Skipped a script sound, %d are already playing", MAX_PLAYING)
    return false
  }

  fun playSound(resId: Int): Boolean {
    give(Feedback.sound(resId))
    return soundOn()
  }

  fun playFile(file: File, done: (Boolean) -> Unit) {
    if (!soundOn() || !startPlaying()) return done(false)
    onMain {
      val player = MediaPlayer()
      var finished = false
      val finish = { played: Boolean ->
        if (!finished) {
          finished = true
          player.release()
          playing.decrementAndGet()
          done(played)
        }
      }
      player.setAudioAttributes(SONIFICATION)
      player.setOnCompletionListener { finish(true) }
      player.setOnErrorListener { _, _, _ -> true.also { finish(false) } }
      try {
        player.setDataSource(file.path)
        player.prepare()
        player.start()
      } catch (e: IOException) {
        LogUtils.e(TAG, "Could not play %s: %s", file.name, e)
        finish(false)
      } catch (e: RuntimeException) {
        // MediaPlayer also throws IllegalStateException and IllegalArgumentException, and this is
        // the main thread.
        LogUtils.e(TAG, "Could not play %s: %s", file.name, e)
        finish(false)
      }
    }
  }

  fun resume() {
    mainHandler.post(resumeBacktalk)
  }

  fun focus(node: AccessibilityNodeInfoCompat, done: (Boolean) -> Unit) = onMain {
    val info = FocusActionInfo.builder().setSourceAction(FocusActionInfo.UNKNOWN).build()
    done(pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.focus(node, info)))
  }

  fun openApp(packageName: String, done: (Boolean) -> Unit) = onMain { done(launch(packageName)) }

  fun vibrate(pattern: LongArray) {
    if (paused || !preference(R.string.pref_vibration_key, R.bool.pref_vibration_default)) return
    val vibrator =
      service.getSystemService(Vibrator::class.java)?.takeIf { it.hasVibrator() } ?: return
    @Suppress("DEPRECATION")
    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0) + pattern, -1), SONIFICATION)
  }

  private fun soundOn(): Boolean =
    !paused && preference(R.string.pref_soundback_key, R.bool.pref_soundback_default)

  private fun preference(key: Int, default: Int): Boolean =
    SharedPreferencesUtils.getBooleanPref(
      SharedPreferencesUtils.getSharedPreferences(service),
      service.resources,
      key,
      default,
    )

  private fun inLanguage(text: String, language: String?): CharSequence =
    language?.let {
      SpannableString(text).apply {
        setSpan(LocaleSpan(Locale.forLanguageTag(it)), 0, length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
      }
    } ?: text

  private fun launch(packageName: String): Boolean {
    val intent =
      service.packageManager.getLaunchIntentForPackage(packageName)?.addFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK
      ) ?: return false
    return runCatching { service.startActivity(intent) }
      .onFailure { LogUtils.e(TAG, "Could not open %s: %s", packageName, it) }
      .isSuccess
  }

  private fun give(feedback: Feedback.Part.Builder) = onMain {
    pipeline.returnFeedback(EVENT_ID_UNTRACKED, feedback)
  }

  private fun onMain(action: () -> Unit) {
    mainHandler.post(action)
  }

  private companion object {
    const val TAG = "ScriptFeedback"
    const val RELEASE_DELAY_MS = 200L
    const val MAX_PLAYING = 4
    val SONIFICATION: AudioAttributes =
      AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
        .build()
  }
}
