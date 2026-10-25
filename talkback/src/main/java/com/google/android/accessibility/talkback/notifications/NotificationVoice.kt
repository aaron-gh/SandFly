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
import android.os.SystemClock
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
 *
 * The engine is connected only while there is something to say, and released [IDLE_MS] after, as
 * an engine kept connected while idle can be stopped by Android or the engine itself, and the
 * connection doesn't come back. Text the engine fails, or takes without starting to say it, is said
 * in SandFly's voice instead, and text it stops saying without reporting it is ended.
 */
class NotificationVoice(private val context: Context) {
  /** Told when an utterance starts and when it ends, on the main thread. */
  interface Listener {
    fun onStart()

    /** [spoken] is false when it was stopped or failed. */
    fun onDone(spoken: Boolean)

    /** The engine didn't start or didn't take the text, so nothing was said. */
    fun onUnavailable()
  }

  private class Request(
    val id: String,
    val text: CharSequence,
    val flush: Boolean,
    val locale: Locale?,
    val listener: Listener?,
  ) {
    /** Whether it was sent again on a new connection after the engine didn't take it. */
    var retried = false
  }

  private class Piece(val request: String, val first: Boolean, val last: Boolean)

  private val handler = Handler(Looper.getMainLooper())
  private var tts: TextToSpeech? = null
  private var profile: VoiceProfile? = null
  private var ready = false
  private val pending = mutableListOf<Request>()
  private val listeners = HashMap<String, Listener>()
  /** The piece of a request each utterance the engine speaks is. */
  private val pieces = HashMap<String, Piece>()
  /** Requests the engine took and hasn't started saying, in the order they were sent. */
  private val unstarted = LinkedHashMap<String, Request>()
  /** Requests the engine is saying. */
  private val speaking = HashSet<String>()
  /** Since when the engine, saying nothing, has had text to start on. */
  private var waitingSince = 0L
  /** Since when the engine has said nothing while a request it started hasn't ended, or 0. */
  private var quietSince = 0L
  private var nextId = 0
  /** The engine's voice named in the profile, found once, since listing voices is slow. */
  private var foundVoice: Voice? = null
  private var foundVoiceName: String? = null
  private val release = Runnable {
    if (listeners.isEmpty() && pending.isEmpty()) {
      LogUtils.d(TAG, "Releasing %s", profile?.engine)
      disconnect()
    }
  }
  private val initTimeout = Runnable {
    LogUtils.e(TAG, "%s didn't start within %d ms", profile?.engine, INIT_TIMEOUT_MS)
    unavailable()
  }
  private val startTimeout = Runnable { checkStarted() }
  private val speakingCheck = Runnable { checkSpeaking() }

  /** The profile the voice is set up from. */
  val currentProfile: VoiceProfile?
    get() = profile

  /** Sets up the voice from [profile]. A new engine is connected for the next text. */
  @MainThread
  fun use(profile: VoiceProfile) {
    val previous = this.profile
    this.profile = profile
    if (previous?.engine != profile.engine) {
      disconnect()
      if (pending.isNotEmpty()) connect()
    } else if (ready) {
      applyVoice()
    }
  }

  private fun connect() {
    val profile = profile ?: return
    LogUtils.d(TAG, "Connecting %s", profile.engine)
    ready = false
    var connection: TextToSpeech? = null
    connection =
      TextToSpeech(
        context,
        { status -> handler.post { onInit(connection, status) } },
        profile.engine.ifEmpty { null },
      )
    tts = connection
    handler.postDelayed(initTimeout, INIT_TIMEOUT_MS)
  }

  /** Ends the connection, and the requests sent on it. */
  private fun disconnect() {
    handler.removeCallbacks(release)
    handler.removeCallbacks(initTimeout)
    handler.removeCallbacks(startTimeout)
    handler.removeCallbacks(speakingCheck)
    val sent = listeners.values.toList()
    listeners.clear()
    pieces.clear()
    unstarted.clear()
    speaking.clear()
    runCatching { tts?.shutdown() }
    tts = null
    ready = false
    foundVoice = null
    foundVoiceName = null
    sent.forEach { it.onDone(false) }
  }

  /** Gives up on the engine for the requests waiting for it. */
  private fun unavailable() {
    val waiting = pending.toList()
    pending.clear()
    disconnect()
    waiting.forEach { it.listener?.onUnavailable() }
  }

  /** Releases the engine once nothing is said or waiting. */
  private fun releaseWhenIdle() {
    handler.removeCallbacks(release)
    if (listeners.isEmpty() && pending.isEmpty()) handler.postDelayed(release, IDLE_MS)
  }

  private fun onInit(connection: TextToSpeech?, status: Int) {
    val tts = tts
    if (tts == null || tts !== connection) return
    handler.removeCallbacks(initTimeout)
    if (status != TextToSpeech.SUCCESS) {
      LogUtils.e(TAG, "%s didn't start: %d", profile?.engine, status)
      unavailable()
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
            if (!piece.first) return@post
            unstarted.remove(piece.request)
            speaking += piece.request
            handler.removeCallbacks(startTimeout)
            watchSpeaking()
            listeners[piece.request]?.onStart()
          }
        }

        override fun onDone(utteranceId: String) {
          handler.post {
            val piece = pieces.remove(utteranceId) ?: return@post
            if (piece.last) {
              speaking.remove(piece.request)
              listeners.remove(piece.request)?.onDone(true)
              watchStart()
            }
            releaseWhenIdle()
          }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String) {
          handler.post { onFailed(utteranceId, TextToSpeech.ERROR) }
        }

        override fun onError(utteranceId: String, errorCode: Int) {
          handler.post { onFailed(utteranceId, errorCode) }
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
    releaseWhenIdle()
  }

  /** Ends the request of a piece that stopped or failed, with the pieces after it. */
  private fun endRequest(utteranceId: String) {
    val request = pieces.remove(utteranceId)?.request ?: return
    pieces.values.removeAll { it.request == request }
    unstarted.remove(request)
    speaking.remove(request)
    listeners.remove(request)?.onDone(false)
    watchStart()
    releaseWhenIdle()
  }

  /** A request the engine failed before saying any of it is said in SandFly's voice instead. */
  private fun onFailed(utteranceId: String, errorCode: Int) {
    val request = pieces[utteranceId]?.request?.let { unstarted[it] }
    if (request == null) {
      LogUtils.e(TAG, "%s failed to say %s: %d", profile?.engine, utteranceId, errorCode)
      endRequest(utteranceId)
      return
    }
    LogUtils.e(TAG, "%s failed to say %s, said in SandFly's voice: %d", profile?.engine, request.id, errorCode)
    forget(request.id)
    request.listener?.onUnavailable()
    watchStart()
    releaseWhenIdle()
  }

  /** Drops what is kept about request [id], without telling its listener. */
  private fun forget(id: String) {
    pieces.values.removeAll { it.request == id }
    unstarted.remove(id)
    speaking.remove(id)
    listeners.remove(id)
  }

  /**
   * Checks that the engine starts saying the next request it took, once it says nothing, as an
   * engine can take text and never say it or report anything.
   */
  private fun watchStart() {
    handler.removeCallbacks(startTimeout)
    if (unstarted.isEmpty() || speaking.isNotEmpty()) return
    waitingSince = SystemClock.uptimeMillis()
    handler.postDelayed(startTimeout, START_TIMEOUT_MS)
  }

  private fun checkStarted() {
    if (unstarted.isEmpty() || speaking.isNotEmpty()) return
    val waited = SystemClock.uptimeMillis() - waitingSince
    // The engine can be saying another app's text, such as SandFly's when both use it.
    val busy = runCatching { tts?.isSpeaking }.getOrNull() == true
    if (busy && waited < BUSY_START_TIMEOUT_MS) {
      handler.postDelayed(startTimeout, START_TIMEOUT_MS)
      return
    }
    LogUtils.e(TAG, "%s didn't start saying the text within %d ms", profile?.engine, waited)
    // Sent again on a new connection, or said in SandFly's voice if that was done already.
    val waiting = unstarted.values.toList()
    waiting.forEach { forget(it.id) }
    disconnect()
    val (again, given) = waiting.partition { !it.retried }
    again.forEach { it.retried = true }
    pending += again
    given.forEach { it.listener?.onUnavailable() }
    if (pending.isNotEmpty()) connect() else releaseWhenIdle()
  }

  /**
   * Checks that the requests the engine is saying end, as an engine can stop saying text without
   * reporting it, such as when it is stopped while another app listens for voice input. A
   * notification left reading would hold back the notifications for other voices for good.
   */
  private fun watchSpeaking() {
    handler.removeCallbacks(speakingCheck)
    if (speaking.isEmpty()) return
    quietSince = 0L
    handler.postDelayed(speakingCheck, SPEAKING_CHECK_MS)
  }

  private fun checkSpeaking() {
    if (speaking.isEmpty()) return
    val now = SystemClock.uptimeMillis()
    // The engine is speaking while it says the pieces of a request, and between them.
    if (runCatching { tts?.isSpeaking }.getOrNull() == true) {
      quietSince = 0L
    } else if (quietSince == 0L) {
      quietSince = now
    } else if (now - quietSince >= QUIET_MS) {
      LogUtils.e(TAG, "%s stopped saying %s without ending it", profile?.engine, speaking.joinToString())
      val ended = speaking.toList()
      speaking.clear()
      ended.forEach { id ->
        pieces.values.removeAll { it.request == id }
        listeners.remove(id)?.onDone(false)
      }
      watchStart()
      releaseWhenIdle()
      return
    }
    handler.postDelayed(speakingCheck, SPEAKING_CHECK_MS)
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
    handler.removeCallbacks(release)
    val request = Request("notification_${nextId++}", text, flush, locale, listener)
    if (ready) {
      speakNow(request)
      return
    }
    if (flush) {
      val dropped = pending.toList()
      pending.clear()
      dropped.forEach { it.listener?.onDone(false) }
    }
    pending += request
    if (tts == null) connect()
  }

  private fun speakNow(request: Request) {
    if (!ready) {
      // A new connection is starting, after the engine didn't take another request.
      pending += request
      if (tts == null) connect()
      return
    }
    val tts = tts
    val profile = profile
    LogUtils.d(TAG, "Saying %s with %s", request.id, profile?.engine)
    if (tts == null || profile == null) {
      request.listener?.onDone(false)
      releaseWhenIdle()
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
    unstarted[request.id] = request
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
        LogUtils.e(TAG, "%s didn't take %s", profile.engine, pieceId)
        if (i == 0) {
          forget(request.id)
          if (request.retried) {
            request.listener?.onUnavailable()
            releaseWhenIdle()
          } else {
            // The connection is gone, such as after the engine was stopped: send it on a new one.
            request.retried = true
            disconnect()
            pending += request
            connect()
          }
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
    // Text sent behind other text the engine hasn't started on waits for the same check.
    if (unstarted.size == 1 && request.id in unstarted) watchStart()
  }

  /** Stops what the voice is saying and what waits to be said. */
  @MainThread
  fun stop() {
    // Text the engine hasn't started on ends now, so it isn't said in SandFly's voice later.
    val dropped = pending.toList() + unstarted.values
    pending.clear()
    dropped.forEach {
      forget(it.id)
      it.listener?.onDone(false)
    }
    handler.removeCallbacks(startTimeout)
    if (ready) runCatching { tts?.stop() }
    releaseWhenIdle()
  }

  @MainThread
  fun shutdown() {
    stop()
    disconnect()
    profile = null
  }

  private companion object {
    const val TAG = "NotificationVoice"
    /** How long the engine stays connected after the last text. */
    const val IDLE_MS = 10_000L
    /** How long the engine has to start before the text is said in SandFly's voice. */
    const val INIT_TIMEOUT_MS = 10_000L
    /** How long the engine has to start saying text it took, once it says nothing else. */
    const val START_TIMEOUT_MS = 5_000L
    /** How long the engine has to start saying text while it says another app's. */
    const val BUSY_START_TIMEOUT_MS = 20_000L
    /** How often the engine is checked for still saying the text it started. */
    const val SPEAKING_CHECK_MS = 2_000L
    /** How long the engine can say nothing before the text it started is ended. */
    const val QUIET_MS = 6_000L
    /** Android's limit on the text of one utterance. */
    val MAX_LENGTH = TextToSpeech.getMaxSpeechInputLength()
  }
}
