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

package com.google.android.accessibility.talkback.notifications

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.annotation.MainThread
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.output.FailoverTextToSpeech
import com.google.android.accessibility.utils.output.SpeechChunker
import com.google.android.accessibility.utils.output.VoiceProfiles.VoiceProfile
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.util.Locale

/**
 * A speech engine of its own for notifications, set up from one of the user's voice profiles, so
 * that notifications are read in another engine or voice than SandFly's. It plays on the
 * accessibility stream, as SandFly's speech does.
 */
class NotificationVoice(private val context: Context) {
  /** Told when an utterance starts and when it ends, on the main thread. */
  interface Listener {
    fun onStart()

    /** [spoken] is false when it was stopped or failed. */
    fun onDone(spoken: Boolean)
  }

  private class Request(
    val id: String,
    val text: CharSequence,
    val flush: Boolean,
    val locale: Locale?,
    val listener: Listener?,
  )

  private class Piece(val request: String, val first: Boolean, val last: Boolean)

  private val handler = Handler(Looper.getMainLooper())
  private var tts: TextToSpeech? = null
  private var profile: VoiceProfile? = null
  private var ready = false
  private val pending = mutableListOf<Request>()
  private val listeners = HashMap<String, Listener>()
  /** The piece of a request each utterance the engine speaks is. */
  private val pieces = HashMap<String, Piece>()
  private var nextId = 0
  /** The engine's voice named in the profile, found once, since listing voices is slow. */
  private var foundVoice: Voice? = null
  private var foundVoiceName: String? = null

  /** Whether the engine couldn't start, so that notifications go back to SandFly's voice. */
  var failed = false
    private set

  /** The profile the voice is set up from. */
  val currentProfile: VoiceProfile?
    get() = profile

  /** Sets up the voice from [profile], starting its engine again when the engine changes. */
  @MainThread
  fun use(profile: VoiceProfile) {
    val previous = this.profile
    this.profile = profile
    if (tts != null && previous?.engine == profile.engine && !failed) {
      if (ready) applyVoice()
      return
    }
    shutdown()
    this.profile = profile
    failed = false
    val engine = profile.engine.ifEmpty { null }
    tts =
      TextToSpeech(
        context,
        { status -> handler.post { onInit(status) } },
        engine,
      )
  }

  private fun onInit(status: Int) {
    val tts = tts ?: return
    if (status != TextToSpeech.SUCCESS) {
      LogUtils.w(TAG, "The notification voice's engine didn't start: %d", status)
      failed = true
      pending.toList().forEach { it.listener?.onDone(false) }
      pending.clear()
      return
    }
    ready = true
    tts.setAudioAttributes(
      AudioAttributes.Builder()
        .setUsage(FailoverTextToSpeech.speechUsage(context))
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    )
    tts.setOnUtteranceProgressListener(
      object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {
          handler.post {
            val piece = pieces[utteranceId] ?: return@post
            if (piece.first) listeners[piece.request]?.onStart()
          }
        }

        override fun onDone(utteranceId: String) {
          handler.post {
            val piece = pieces.remove(utteranceId) ?: return@post
            if (piece.last) listeners.remove(piece.request)?.onDone(true)
          }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String) {
          handler.post { endRequest(utteranceId) }
        }

        override fun onError(utteranceId: String, errorCode: Int) {
          handler.post { endRequest(utteranceId) }
        }

        override fun onStop(utteranceId: String, interrupted: Boolean) {
          handler.post { endRequest(utteranceId) }
        }
      }
    )
    applyVoice()
    val waiting = pending.toList()
    pending.clear()
    waiting.forEach { speakNow(it) }
  }

  /** Ends the request of a piece that stopped or failed, with the pieces after it. */
  private fun endRequest(utteranceId: String) {
    val request = pieces.remove(utteranceId)?.request ?: return
    pieces.values.removeAll { it.request == request }
    listeners.remove(request)?.onDone(false)
  }

  /**
   * Sets the profile's voice as SandFly's own speech does: its voice, else its language, else the
   * engine's default voice. Each is tried in turn, since a listed voice can still fail to be set.
   */
  private fun applyVoice() {
    val tts = tts ?: return
    val profile = profile ?: return
    runCatching {
      val voice = findVoice(tts, profile.voice)
      if (voice != null && tts.setVoice(voice) == TextToSpeech.SUCCESS) return
      if (
        profile.language.isNotEmpty() &&
          tts.setLanguage(Locale.forLanguageTag(profile.language)) >= TextToSpeech.LANG_AVAILABLE
      ) {
        return
      }
      tts.defaultVoice?.let { tts.setVoice(it) }
    }
  }

  /** Returns the engine's voice called [name], unless it needs downloading. */
  private fun findVoice(tts: TextToSpeech, name: String): Voice? {
    if (name.isEmpty()) return null
    if (foundVoiceName != name) {
      foundVoiceName = name
      foundVoice = runCatching { tts.voices }.getOrNull()?.firstOrNull { it?.name == name }
    }
    val installed =
      foundVoice?.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true
    return foundVoice?.takeIf { installed }
  }

  /**
   * Says [text], after what this voice is saying unless [flush]. [locale] is the language the
   * text is written in, when it was detected. The profile's rate, pitch, volume and phrases apply.
   */
  @MainThread
  fun speak(
    text: CharSequence,
    flush: Boolean,
    locale: Locale?,
    listener: Listener?,
  ) {
    val request = Request("notification_${nextId++}", text, flush, locale, listener)
    when {
      failed -> listener?.onDone(false)
      ready -> speakNow(request)
      else -> {
        if (flush) pending.clear()
        pending += request
      }
    }
  }

  private fun speakNow(request: Request) {
    val tts = tts
    val profile = profile
    if (tts == null || profile == null) {
      request.listener?.onDone(false)
      return
    }
    // A detected language replaces the profile's for this text only.
    if (request.locale != null && tts.isLanguageAvailable(request.locale) >= TextToSpeech.LANG_AVAILABLE) {
      tts.setLanguage(request.locale)
    } else if (request.locale != null) {
      applyVoice()
    }
    // As SandFly's own speech does, the profile's rate and pitch are multiples of Android's
    // default text-to-speech rate and pitch, which an engine connection of its own doesn't apply.
    val resolver = context.contentResolver
    val defaultRate = Settings.Secure.getInt(resolver, Settings.Secure.TTS_DEFAULT_RATE, 100) / 100f
    val defaultPitch = Settings.Secure.getInt(resolver, Settings.Secure.TTS_DEFAULT_PITCH, 100) / 100f
    tts.setSpeechRate((profile.rate * defaultRate).coerceIn(0.1f, 6f))
    tts.setPitch((profile.pitch * defaultPitch).coerceIn(0.2f, 2f))
    val minVolume = context.resources.getInteger(R.integer.pref_speech_volume_min)
    val volume = maxOf(profile.volume, minVolume) / 100f
    val params =
      Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f)) }
    request.listener?.let { listeners[request.id] = it }
    val text = request.text.let { if (it.length > MAX_LENGTH) it.subSequence(0, MAX_LENGTH) else it }
    // With phrases on, long text is sent a sentence at a time, as SandFly's own speech sends it.
    val starts =
      if (profile.phrases) SpeechChunker.chunkStarts(text, request.locale ?: Locale.getDefault())
      else listOf(0)
    if (starts.size > 1) LogUtils.d(TAG, "speak: %s in %d pieces", request.id, starts.size)
    for (i in starts.indices) {
      val last = i == starts.lastIndex
      val pieceId = if (starts.size == 1) request.id else "${request.id}_$i"
      pieces[pieceId] = Piece(request.id, first = i == 0, last = last)
      val piece = text.subSequence(starts[i], if (last) text.length else starts[i + 1])
      val queueMode =
        if (i == 0 && request.flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
      if (tts.speak(piece, queueMode, params, pieceId) != TextToSpeech.SUCCESS) {
        if (i == 0) {
          endRequest(pieceId)
        } else {
          // Finish the request with the pieces that were queued.
          pieces.remove(pieceId)
          val previousId = "${request.id}_${i - 1}"
          pieces[previousId] = Piece(request.id, first = i == 1, last = true)
        }
        break
      }
    }
    // The detected language applies to this text only.
    if (request.locale != null) applyVoice()
  }

  /** Whether the voice is saying something. */
  val isSpeaking: Boolean
    get() = ready && runCatching { tts?.isSpeaking == true }.getOrDefault(false)

  /** Stops what the voice is saying and what waits to be said. */
  @MainThread
  fun stop() {
    pending.toList().forEach { it.listener?.onDone(false) }
    pending.clear()
    if (ready) runCatching { tts?.stop() }
  }

  @MainThread
  fun shutdown() {
    stop()
    listeners.values.toList().forEach { it.onDone(false) }
    listeners.clear()
    pieces.clear()
    runCatching { tts?.shutdown() }
    tts = null
    ready = false
    profile = null
    foundVoice = null
    foundVoiceName = null
  }

  private companion object {
    const val TAG = "NotificationVoice"
    /** Android's limit on the text of one utterance. */
    val MAX_LENGTH = TextToSpeech.getMaxSpeechInputLength()
  }
}
