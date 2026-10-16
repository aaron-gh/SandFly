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

package com.google.android.accessibility.utils.output

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.nio.ByteBuffer
import java.util.ArrayDeque
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Plays Backtalk's sounds and speech on one low-latency audio track at the device's own sample
 * rate, so that Android can give it its fast path, which reaches the speaker tens of milliseconds
 * sooner than ordinary tracks. Short sounds are mixed together, and speech streams play one after
 * another, as they arrive from the engine.
 *
 * The track keeps running while there is something to play and for a moment after, so that the
 * next sound starts at once, then pauses. Its thread ends then, and nothing runs until the next
 * sound or speech. While an app records from the microphone, the track pauses as soon as what it
 * played has been heard: some apps, such as Gemini, don't take the user's voice while accessibility
 * audio plays, even silence. Speech and sounds that start while an app records play the usual
 * way instead, for the same reason.
 */
class LowLatencyAudio private constructor(context: Context, private val attributes: AudioAttributes) {
  /** Output frames per second. */
  val sampleRate: Int

  private val burstFrames: Int
  private val mainHandler = Handler(Looper.getMainLooper())
  private val lock = java.lang.Object()
  private val clips = ArrayList<PlayingClip>()
  private val streams = ArrayDeque<SpeechStream>()
  // Listener callbacks run here, in order, so that they never hold up the audio thread. Its thread
  // ends when there is nothing to report.
  private val callbacks = singleThreadExecutor("LowLatencyAudio callbacks")
  @Volatile private var track: AudioTrack?
  // Set when no track can be made, or the track stops taking audio and cannot be remade. Then audio
  // plays the usual way.
  @Volatile private var broken = false
  // Set by shutdown. Nothing plays again, and the track is released.
  @Volatile private var released = false
  // When the track last took audio, to find a track that has stopped.
  @Volatile private var lastWrite = 0L
  // Set by the watchdog when the track has taken no audio for WRITE_STUCK_MS.
  @Volatile private var remakeTrack = false
  // Set when a paused track without the fast path starts, until its first write completes. An
  // output such as Bluetooth can take a second or more to wake from standby, which is not a stuck
  // track, so the watchdog gives it WAKE_STUCK_MS longer.
  @Volatile private var waking = false
  // Whether the track has the fast path, and its underrun count when its buffer was last sized.
  @Volatile private var fastPath = false
  @Volatile private var underruns = 0
  // The fast path's buffer size, once it has had to grow, kept for new tracks and outputs.
  @Volatile private var fastFrames = 0
  // Set when the track starts, until its first write completes. Running short before that is only
  // the track starting, not a buffer too small.
  @Volatile private var starting = false
  // The watchdog's checks, scheduled only while the audio thread runs.
  private var watchdog: ScheduledFuture<*>? = null
  private var thread: Thread? = null
  private var idleFrames = 0
  // Whether the track has audio queued since it last paused. Without the fast path, it pauses as
  // soon as its audio has been heard, after silentFrames of silence.
  private var primed = false
  private var silentFrames = 0
  // While held, speech streams keep what they have and keep receiving audio, but do not play.
  private var held = false
  // Whether this hold has filled up already, so it is reported once.
  private var holdFull = false

  init {
    val audioManager = context.getSystemService(AudioManager::class.java)
    sampleRate =
      audioManager?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull()
        ?: DEFAULT_RATE
    burstFrames =
      audioManager?.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)?.toIntOrNull()
        ?: DEFAULT_BURST
    track = createTrack()
  }

  /** A short sound, as stereo frames at [rate]. */
  class Clip internal constructor(internal val frames: FloatArray, internal val rate: Int) {
    /** How many samples the clip holds. */
    val size: Int
      get() = frames.size
  }

  private class PlayingClip(val clip: Clip, val left: Float, val right: Float, val step: Double) {
    var position = 0.0
  }

  /** Converts decoded audio to a clip for this output. */
  fun prepare(decoded: AudioDecoder.Decoded): Clip {
    val resampled =
      SincResampler(decoded.sampleRate, sampleRate, decoded.channels).let {
        val out = it.process(decoded.samples)
        out + it.flush()
      }
    return Clip(toStereo(resampled, decoded.channels), sampleRate)
  }

  /** Plays [clip] at [left] and [right] volumes from 0 to 1, and [rate], 1 for its own speed. */
  fun play(clip: Clip, left: Float, right: Float, rate: Float) {
    // A clip made for a player at another rate, before this one replaced it, still plays in tune.
    val step = rate.toDouble().coerceIn(0.25, 4.0) * clip.rate / sampleRate
    synchronized(lock) {
      if (released || broken) return
      clips += PlayingClip(clip, left, right, step)
      wake()
    }
  }

  /** Called on a callback thread as a speech stream plays. */
  interface StreamListener {
    /** The stream's first sound reached the output. */
    fun onStarted(id: String)

    /** Speech reached the word range [start] to [end] of the text. */
    fun onRange(id: String, start: Int, end: Int)

    /** The stream played to its end, or was stopped before it, if not [completed]. */
    fun onFinished(id: String, completed: Boolean)

    /** The engine finished without giving any audio, so the stream played nothing. */
    fun onNoAudio(id: String)

    /**
     * The stream waited at the front of the queue for [STALL_MS] without the engine giving it any
     * audio or finishing, so it was dropped, to let the speech after it play.
     */
    fun onStalled(id: String)

    /**
     * The player stopped working, such as when its track could not be made again, so the stream
     * was dropped to play the usual way. Unlike [onStalled], the engine is not at fault.
     */
    fun onPlayerFailed(id: String)

    /**
     * The engine's audio has been digital silence for [SILENT_MS], as if it applied the zero volume
     * Backtalk gives it itself, so the stream plays nothing.
     */
    fun onSilent(id: String)

    /**
     * Held speech reached [MAX_HELD_MS] of audio, so the stream takes no more of it. The held
     * speech should be dropped and said again the usual way when it resumes.
     */
    fun onHoldFull(id: String)
  }

  /**
   * Speech from an engine, played after the streams opened before it. Audio is written to it as
   * the engine makes it.
   */
  inner class SpeechStream internal constructor(
    val id: String,
    private val volume: Float,
    private val pan: Float,
    private val listener: StreamListener,
  ) {
    private var resampler: SincResampler? = null
    private var inChannels = 1
    private var inRate = 0
    private var inFrames = 0L
    private var heardSound = false
    private var reportedSilent = false
    private var inEncoding = AudioFormat.ENCODING_PCM_16BIT
    private val pending = ArrayDeque<FloatArray>()
    private var headOffset = 0
    private var framesWritten = 0L
    private var framesPlayed = 0L
    private var started = false
    private var ranDry = false
    private var ended = false
    private var finished = false
    // When the engine last gave this stream anything, or it reached the front of the queue.
    private var lastActivity = SystemClock.uptimeMillis()
    // When this stream reached the front of the queue, or 0 before.
    private var headSince = 0L
    // Word ranges by the output frame they start at.
    private val ranges = ArrayDeque<LongArray>()

    /** The engine's format, from the start of synthesis. */
    fun begin(rate: Int, encoding: Int, channels: Int) {
      synchronized(lock) {
        lastActivity = SystemClock.uptimeMillis()
        if (
          !AudioDecoder.isSupportedEncoding(encoding) ||
            !SpatialSoundPlayer.isSupportedFormat(rate, channels)
        ) {
          // An engine can claim any format. One Backtalk cannot play is said the usual way.
          LogUtils.w(TAG, "Speech %s has unsupported format %d Hz %d ch %d", id, rate, channels, encoding)
          if (streams.remove(this)) abandon(playerFailed = false)
          return
        }
        inChannels = channels
        inRate = rate
        inEncoding = encoding
        resampler = SincResampler(rate, sampleRate, inChannels)
      }
    }

    /** A piece of the engine's audio. */
    fun write(audio: ByteArray) {
      if (resampler == null || finished) return
      val samples = AudioDecoder.FloatList()
      AudioDecoder.toFloats(ByteBuffer.wrap(audio), inEncoding, samples)
      val floats = samples.toArray()
      synchronized(lock) {
        lastActivity = SystemClock.uptimeMillis()
        val resampler = resampler ?: return
        if (finished) return
        if (held && heldFrames() >= sampleRate * MAX_HELD_MS / 1000) {
          // A long pause would otherwise keep all the speech the engine goes on making.
          if (!holdFull) {
            holdFull = true
            LogUtils.w(TAG, "Held speech %s is full", id)
            callbacks.execute { listener.onHoldFull(id) }
          }
          return
        }
        if (framesWritten - framesPlayed >= sampleRate * MAX_QUEUED_MS / 1000) {
          // The engine makes speech as fast as it plays, so this much waiting means the track or
          // the engine is not behaving. Say it the usual way rather than keep it all.
          LogUtils.e(TAG, "Speech %s has too much audio waiting", id)
          if (streams.remove(this)) abandon(playerFailed = false)
          return
        }
        inFrames += floats.size / inChannels
        if (!heardSound && floats.any { it != 0f }) heardSound = true
        if (!heardSound && inRate > 0 && inFrames * 1000 / inRate >= SILENT_MS) reportSilent()
        add(toStereo(resampler.process(floats), inChannels))
      }
    }

    /**
     * The engine reached a word range, at [frame] of its audio, or at the end of the audio written
     * so far if [frame] is unknown.
     */
    fun range(start: Int, end: Int, frame: Int) {
      synchronized(lock) {
        if (finished) return
        val at =
          if (frame > 0 && inRate > 0) frame.toLong() * sampleRate / inRate else framesWritten
        ranges += longArrayOf(at, start.toLong(), end.toLong())
      }
    }

    private fun reportSilent() {
      if (reportedSilent) return
      reportedSilent = true
      callbacks.execute { listener.onSilent(id) }
    }

    /** The engine finished making the audio. */
    fun end() {
      synchronized(lock) {
        if (finished) return
        // A short utterance that is all silence, such as a pause or punctuation, says nothing
        // about whether the engine silenced its audio. Only SILENT_MS of silence does.
        resampler?.let { add(toStereo(it.flush(), inChannels)) }
        ended = true
        wake()
      }
    }

    private fun add(frames: FloatArray) {
      if (frames.isEmpty()) return
      pending += frames
      framesWritten += frames.size / 2
      wake()
    }

    /**
     * Whether this stream has finished, been stopped or been dropped, and has reported it. The
     * engine's callbacks for it after that are to be ignored.
     */
    val isFinished: Boolean
      get() = synchronized(lock) { finished }

    /** Frames written and not yet played. */
    internal fun pendingFrames(): Long = framesWritten - framesPlayed

    /** Whether the engine finished making the audio. */
    internal val isEnded: Boolean
      get() = ended

    /** How long since the engine last gave this stream anything, at [now]. */
    internal fun quietFor(now: Long): Long = now - lastActivity

    /** Mixes this stream's next frames into [out], and returns whether it has more to play. */
    internal fun mixInto(out: FloatArray, frames: Int): Boolean {
      var written = 0
      while (written < frames && pending.isNotEmpty()) {
        val head = pending.peekFirst()!!
        val available = head.size / 2 - headOffset
        val count = minOf(available, frames - written)
        val left = volume * minOf(1f, 1 - pan)
        val right = volume * minOf(1f, 1 + pan)
        for (i in 0 until count) {
          out[(written + i) * 2] += head[(headOffset + i) * 2] * left
          out[(written + i) * 2 + 1] += head[(headOffset + i) * 2 + 1] * right
        }
        written += count
        headOffset += count
        if (headOffset * 2 >= head.size) {
          pending.removeFirst()
          headOffset = 0
        }
      }
      if (started && !ended && written < frames && !ranDry) {
        // The engine has not kept up, so the speech has a gap.
        ranDry = true
        LogUtils.d(TAG, "Speech %s ran dry after %d frames", id, framesPlayed + written)
      }
      if (written > 0 && !started) {
        started = true
        callbacks.execute { listener.onStarted(id) }
      }
      framesPlayed += written
      while (ranges.isNotEmpty() && ranges.peekFirst()!![0] <= framesPlayed) {
        val range = ranges.removeFirst()
        callbacks.execute { listener.onRange(id, range[1].toInt(), range[2].toInt()) }
      }
      if (ended && pending.isEmpty()) {
        finish(completed = true)
        return false
      }
      return true
    }

    /**
     * Whether this stream, at the front of the queue since [now] or before, has waited [STALL_MS]
     * without the engine giving it audio or finishing, such as after the engine was shut down or
     * frozen.
     */
    internal fun isStalled(now: Long): Boolean {
      if (headSince == 0L) headSince = now
      return !ended && pending.isEmpty() && now - maxOf(lastActivity, headSince) > STALL_MS
    }

    /**
     * Ends this stream for another way to play it, without reporting that it finished.
     * [playerFailed] tells whether the player, rather than the engine, is why.
     */
    internal fun abandon(playerFailed: Boolean) {
      finished = true
      pending.clear()
      callbacks.execute {
        if (playerFailed) listener.onPlayerFailed(id) else listener.onStalled(id)
      }
    }

    internal fun finish(completed: Boolean) {
      if (finished) return
      finished = true
      pending.clear()
      val noAudio = completed && framesWritten == 0L
      callbacks.execute {
        if (noAudio) listener.onNoAudio(id) else listener.onFinished(id, completed)
      }
    }

    /** Stops this stream, whether or not it has started playing. */
    fun stop() {
      synchronized(lock) {
        if (streams.remove(this)) finish(completed = false)
      }
    }

    /** Removes this stream without reporting anything, as if it had never been opened. */
    fun discard() {
      synchronized(lock) {
        streams.remove(this)
        finished = true
        pending.clear()
      }
    }
  }

  /**
   * Opens a speech stream at [volume] from 0 to 1 and [pan] from -1, left, to 1, right. It plays
   * once the streams before it have finished.
   */
  fun openStream(id: String, volume: Float, pan: Float, listener: StreamListener): SpeechStream {
    synchronized(lock) {
      val stream = SpeechStream(id, volume.coerceIn(0f, 1f), pan.coerceIn(-1f, 1f), listener)
      if (released || broken) {
        // Shut down or given up on since it was got: said the usual way instead.
        stream.abandon(playerFailed = true)
        return stream
      }
      streams += stream
      wake()
      return stream
    }
  }

  /** Stops every speech stream, which each report that they were stopped. */
  fun stopStreams() {
    synchronized(lock) {
      held = false
      while (streams.isNotEmpty()) streams.removeFirst().finish(completed = false)
    }
  }

  /** The frames held speech has waiting, while held. */
  private fun heldFrames(): Long = streams.sumOf { it.pendingFrames() }

  /**
   * Holds speech where it is, mid-word if need be, so that [resume] carries on from exactly there,
   * and returns the ID of the speech stream held, or null if there is no speech to hold. The streams
   * keep receiving the engine's audio meanwhile, up to [MAX_HELD_MS]. Sounds still play.
   */
  fun hold(): String? =
    synchronized(lock) {
      val head = streams.peekFirst() ?: return null
      held = true
      holdFull = false
      head.id
    }

  /** Carries on playing held speech. */
  fun resume() {
    synchronized(lock) {
      held = false
      holdFull = false
      wake()
    }
  }

  /**
   * Stops everything and releases the track. Speech streams report that they were stopped. The
   * player cannot be used again: [get] makes a new one.
   */
  fun shutdown() {
    synchronized(lock) {
      if (released) return
      released = true
      held = false
      clips.clear()
      while (streams.isNotEmpty()) streams.removeFirst().finish(completed = false)
      if (thread == null) {
        track?.release()
        track = null
      } else {
        // The audio thread releases the track as it ends.
        interrupt(track)
        lock.notifyAll()
      }
    }
  }

  /** Starts the audio thread and its watchdog if they are not running. Called with [lock] held. */
  private fun wake() {
    idleFrames = 0
    // A thread waiting for speech carries on with it.
    lock.notifyAll()
    if (thread != null || released || broken) return
    lastWrite = SystemClock.uptimeMillis()
    remakeTrack = false
    waking = false
    thread = Thread(::run, "LowLatencyAudio").apply { start() }
    // Separate from the audio thread, so that a stuck track cannot stop it.
    watchdog =
      WATCHDOG.scheduleWithFixedDelay(
        ::checkForStalls,
        WATCHDOG_MS,
        WATCHDOG_MS,
        TimeUnit.MILLISECONDS,
      )
  }

  /** Makes a write blocked on [output] return, so the audio thread can go on. */
  private fun interrupt(output: AudioTrack?) {
    try {
      output?.pause()
    } catch (e: IllegalStateException) {
      // Released already.
    }
  }

  /**
   * Drops a speech stream the engine stopped feeding, and gives up on the track if it stopped taking
   * audio, so that speech never waits in silence. Abandoned speech is said the usual way.
   */
  private fun checkForStalls() {
    synchronized(lock) {
      if (thread == null) return
      val now = SystemClock.uptimeMillis()
      val head = if (held) null else streams.peekFirst()
      if (head != null && head.isStalled(now)) {
        streams.removeFirst()
        head.abandon(playerFailed = false)
        LogUtils.w(TAG, "Speech %s got no audio in time", head.id)
      }
      val stuckFor = now - lastWrite
      val extra = if (waking) WAKE_STUCK_MS else 0L
      if (stuckFor > TRACK_STUCK_MS + extra) {
        LogUtils.e(TAG, "Track took no audio for %d ms", stuckFor)
        giveUp()
      } else if (stuckFor > WRITE_STUCK_MS + extra && !remakeTrack) {
        // Android stopped taking audio on this track, such as after its output changed.
        LogUtils.w(TAG, "Track took no audio for %d ms, making a new one", stuckFor)
        remakeTrack = true
        interrupt(track)
      }
    }
  }

  /** Stops using the low-latency track: everything waiting plays the usual way. */
  private fun giveUp() {
    synchronized(lock) {
      broken = true
      clips.clear()
      while (streams.isNotEmpty()) streams.removeFirst().abandon(playerFailed = true)
      if (thread == null) {
        track?.release()
        track = null
      } else {
        interrupt(track)
        lock.notifyAll()
      }
    }
  }

  private fun run() {
    Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
    val buffer = FloatArray(burstFrames * 2)
    while (true) {
      val output: AudioTrack
      synchronized(lock) {
        val current = track
        val idle = clips.isEmpty() && (streams.isEmpty() || held)
        if (released || broken || current == null || (idle && idleFrames >= idleLimit())) {
          end(current)
          return
        }
        output = current
        if (!fastPath) {
          // Silence queued without the fast path would delay the next sound by the whole buffer, so
          // silence plays only until the audio before it has been heard. Then the track pauses.
          val audible = hasAudio()
          if (primed && !audible && silentFrames >= output.bufferSizeInFrames) pauseAndFlush(output)
          if (!primed && !readyToStart(output)) {
            if (idle) {
              end(current)
              return
            }
            // Speech waiting for the engine. The thread stays, so the watchdog can drop a stall.
            lastWrite = SystemClock.uptimeMillis()
            lock.wait(REST_MS)
            continue
          }
          buffer.fill(0f)
          mix(buffer)
          // A stream that ended with nothing left finishes without starting the track.
          if (!primed && !audible) continue
          silentFrames = if (audible) 0 else silentFrames + burstFrames
        } else {
          buffer.fill(0f)
          mix(buffer)
        }
        primed = true
        if (idle) idleFrames += burstFrames
      }
      if (!remakeTrack && play(output) && write(output, buffer)) continue
      if (released || broken) continue
      // Android stopped taking audio on this track, such as after its output changed. Make a new
      // one, or play the usual way from now on.
      synchronized(lock) {
        track = null
        primed = false
      }
      output.release()
      val fresh = createTrack()
      synchronized(lock) {
        remakeTrack = false
        track = fresh
        if (fresh == null) giveUp()
      }
    }
  }

  /**
   * Ends the audio thread and its watchdog. The track is paused for the next sound, or released if
   * it will not be used again. Called with [lock] held.
   */
  private fun end(output: AudioTrack?) {
    thread = null
    watchdog?.cancel(false)
    watchdog = null
    if (released || broken) {
      track = null
      primed = false
      output?.release()
      return
    }
    if (output != null) pauseAndFlush(output)
  }

  /** Pauses the track and drops what it has queued. Called with [lock] held. */
  private fun pauseAndFlush(output: AudioTrack) {
    primed = false
    try {
      if (output.playState == AudioTrack.PLAYSTATE_PLAYING) {
        output.pause()
        output.flush()
      }
    } catch (e: IllegalStateException) {
      // Released already.
    }
  }

  /** Whether the next burst has any sound or speech in it. Called with [lock] held. */
  private fun hasAudio(): Boolean {
    if (clips.isNotEmpty()) return true
    val head = if (held) null else streams.peekFirst()
    return head != null && head.pendingFrames() > 0
  }

  /**
   * Whether there is enough to start the paused track on without the fast path, which waits for a
   * full buffer before it plays: a sound, which is all there at once, or speech that fills the
   * buffer, has ended, or has stopped coming for REST_MS. Called with [lock] held.
   */
  private fun readyToStart(output: AudioTrack): Boolean {
    if (clips.isNotEmpty()) return true
    if (held) return false
    val head = streams.peekFirst() ?: return false
    val pending = head.pendingFrames()
    return head.isEnded ||
      pending >= output.bufferSizeInFrames ||
      (pending > 0 && head.quietFor(SystemClock.uptimeMillis()) >= REST_MS)
  }

  private fun play(output: AudioTrack): Boolean =
    try {
      if (output.playState != AudioTrack.PLAYSTATE_PLAYING) {
        output.play()
        starting = true
        if (!fastPath) waking = true
      }
      true
    } catch (e: IllegalStateException) {
      false
    }

  /**
   * Writes [buffer], waiting until the track has room, and returns whether the track took it all.
   * The thread sleeps while it waits, and wakes once for each burst played. A track that stops
   * taking audio is paused by the watchdog, which ends the wait.
   */
  private fun write(output: AudioTrack, buffer: FloatArray): Boolean {
    val count =
      try {
        output.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
      } catch (e: IllegalStateException) {
        return false
      }
    if (count != buffer.size) return false
    lastWrite = SystemClock.uptimeMillis()
    waking = false
    try {
      keepUp(output)
    } catch (e: IllegalStateException) {
      return false
    }
    return true
  }

  private fun idleLimit(): Int =
    sampleRate * (if (microphoneInUse) MICROPHONE_IDLE_MS else IDLE_MS) / 1000

  /** Mixes the next burst of every sound and the speech stream at the head of the queue. */
  private fun mix(out: FloatArray) {
    val iterator = clips.iterator()
    while (iterator.hasNext()) {
      val playing = iterator.next()
      val frames = playing.clip.frames
      val total = frames.size / 2
      for (i in 0 until burstFrames) {
        val position = playing.position
        val index = position.toInt()
        if (index >= total) break
        val next = minOf(index + 1, total - 1)
        val fraction = (position - index).toFloat()
        out[i * 2] +=
          (frames[index * 2] * (1 - fraction) + frames[next * 2] * fraction) * playing.left
        out[i * 2 + 1] +=
          (frames[index * 2 + 1] * (1 - fraction) + frames[next * 2 + 1] * fraction) * playing.right
        playing.position += playing.step
      }
      if (playing.position >= total) iterator.remove()
    }
    val head = if (held) null else streams.peekFirst()
    if (head != null && !head.mixInto(out, burstFrames)) streams.removeFirst()
    for (i in out.indices) out[i] = out[i].coerceIn(-1f, 1f)
  }

  private fun createTrack(): AudioTrack? =
    try {
      val minBytes =
        AudioTrack.getMinBufferSize(
          sampleRate,
          AudioFormat.CHANNEL_OUT_STEREO,
          AudioFormat.ENCODING_PCM_FLOAT,
        )
      AudioTrack.Builder()
        .setAudioAttributes(attributes)
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()
        )
        .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .setBufferSizeInBytes(minBytes)
        .build()
        .also {
          fitBuffer(it)
          // Moving to another output, such as Bluetooth, can lose or gain the fast path.
          it.addOnRoutingChangedListener({ routed -> fitBuffer(routed as AudioTrack) }, mainHandler)
        }
    } catch (e: RuntimeException) {
      LogUtils.e(TAG, "Cannot create a low-latency track: %s", e)
      null
    }

  /**
   * Queues two bursts on the fast path, so new sound waits no longer than that, or more if it ran
   * short before. The burst Android reports can be smaller than the fast mixer's period, such as 144
   * frames for a 192 frame period. Other outputs, such as Bluetooth without the fast path, mix a
   * much larger period at a time, so the buffer starts at SLOW_BUFFER_MS there. Both grow in
   * [keepUp] until the mixer stops running short.
   */
  private fun fitBuffer(output: AudioTrack) {
    try {
      val fast = output.performanceMode == AudioTrack.PERFORMANCE_MODE_LOW_LATENCY
      output.setBufferSizeInFrames(
        if (fast) maxOf(burstFrames * QUEUED_BURSTS, fastFrames)
        else sampleRate * SLOW_BUFFER_MS / 1000
      )
      underruns = output.underrunCount
      fastPath = fast
      LogUtils.d(
        TAG,
        "Track at %d Hz, %d frames, low latency=%b",
        sampleRate,
        output.bufferSizeInFrames,
        fast,
      )
    } catch (e: IllegalStateException) {
      // Released already.
    }
  }

  /**
   * Grows the buffer after the mixer ran short, which otherwise pops, or crackles and slows audio
   * down. A full buffer delays new sound by its length, so it grows only as far as it has to: a
   * burst at a time on the fast path, and double without it.
   */
  private fun keepUp(output: AudioTrack) {
    val count = output.underrunCount
    if (starting) {
      starting = false
      underruns = count
      return
    }
    if (count <= underruns) return
    underruns = count
    val size = output.bufferSizeInFrames
    if (size >= output.bufferCapacityInFrames) return
    val grown = if (fastPath) size + burstFrames else size * 2
    output.setBufferSizeInFrames(minOf(grown, output.bufferCapacityInFrames))
    if (fastPath) fastFrames = output.bufferSizeInFrames
    LogUtils.d(TAG, "Track ran short, now %d frames", output.bufferSizeInFrames)
  }

  companion object {
    private const val TAG = "LowLatencyAudio"
    private const val DEFAULT_RATE = 48_000
    private const val DEFAULT_BURST = 192
    private const val QUEUED_BURSTS = 2
    // Two periods of a typical mixer without the fast path.
    private const val SLOW_BUFFER_MS = 40
    // How long the audio thread waits for the engine's speech at a time, well within WRITE_STUCK_MS.
    private const val REST_MS = 100L
    private const val IDLE_MS = 2_000
    // While an app records, long enough for the audio already queued to be heard.
    private const val MICROPHONE_IDLE_MS = 100
    // How long the callback thread is kept with nothing to do.
    private const val THREAD_KEEP_ALIVE_MS = 5_000L

    /** The most audio held speech keeps, at about 384 KB a second. */
    const val MAX_HELD_MS = 10_000L

    /** The most audio a speech stream may have waiting to play while not held. */
    private const val MAX_QUEUED_MS = 30_000L

    /** How long a stream at the front of the queue waits for audio before it is dropped. */
    const val STALL_MS = 4_000L

    /** How much digital silence from the engine means it silenced the audio itself. */
    const val SILENT_MS = 500L

    /** How often the watchdog looks for stalled speech and a stuck track. */
    private const val WATCHDOG_MS = 250L

    /** How long the track may take no audio before it is remade. */
    private const val WRITE_STUCK_MS = 300L

    /** How long the track may take no audio before the watchdog gives up on it. */
    private const val TRACK_STUCK_MS = 1_500L

    /** How much longer a track without the fast path may take to wake from standby. */
    private const val WAKE_STUCK_MS = 2_000L

    private val instances = HashMap<Int, LowLatencyAudio>()

    // Runs every player's watchdog while its audio thread runs. Its thread ends when none is.
    private val WATCHDOG =
      ScheduledThreadPoolExecutor(1) { Thread(it, "LowLatencyAudio watchdog") }
        .apply {
          setKeepAliveTime(THREAD_KEEP_ALIVE_MS, TimeUnit.MILLISECONDS)
          allowCoreThreadTimeOut(true)
          removeOnCancelPolicy = true
        }

    /** An executor running tasks in order on one thread, which ends when it has nothing to do. */
    private fun singleThreadExecutor(name: String): Executor =
      ThreadPoolExecutor(
          1,
          1,
          THREAD_KEEP_ALIVE_MS,
          TimeUnit.MILLISECONDS,
          LinkedBlockingQueue(),
        ) {
          Thread(it, name)
        }
        .apply { allowCoreThreadTimeOut(true) }

    /** The players made so far, without making new ones. */
    /** Whether an app is recording from the microphone, set by [setMicrophoneInUse]. */
    @Volatile private var microphoneInUse = false

    /**
     * Whether an app is recording from the microphone. Speech and sounds then play the usual way:
     * some apps, such as Gemini, don't hear the user while the accessibility service itself plays
     * audio, but do while the speech engine plays it.
     */
    @JvmStatic fun isMicrophoneInUse(): Boolean = microphoneInUse

    /**
     * Tells the players whether an app is recording from the microphone, so that while one is,
     * their tracks pause soon after each sound rather than running on in silence.
     */
    @JvmStatic
    fun setMicrophoneInUse(inUse: Boolean) {
      microphoneInUse = inUse
    }

    @JvmStatic
    fun players(): List<LowLatencyAudio> = synchronized(instances) { instances.values.toList() }

    /** Stops every speech stream of every player made so far, without making new ones. */
    @JvmStatic
    fun stopAllStreams() {
      val players = synchronized(instances) { instances.values.toList() }
      for (player in players) player.stopStreams()
    }

    /**
     * Shuts down every player, releasing their tracks, for when the setting is turned off or the
     * service stops. Players are made again when next needed.
     */
    @JvmStatic
    fun shutdownAll() {
      val players = synchronized(instances) { instances.values.toList().also { instances.clear() } }
      for (player in players) player.shutdown()
    }

    /**
     * The player for [attributes]' usage, one for each kind of audio, or null if the device cannot
     * make a low-latency track, and audio should play the usual way.
     */
    @JvmStatic
    fun get(context: Context, attributes: AudioAttributes): LowLatencyAudio? =
      synchronized(instances) {
        instances
          .getOrPut(attributes.usage) { LowLatencyAudio(context.applicationContext, attributes) }
          .takeIf { it.track != null && !it.broken }
      }

    /** Turns interleaved audio with [channels] channels into stereo frames. */
    @JvmStatic
    fun toStereo(samples: FloatArray, channels: Int): FloatArray =
      when (channels) {
        2 -> samples
        1 -> FloatArray(samples.size * 2) { samples[it / 2] }
        else -> {
          val frames = samples.size / channels
          FloatArray(frames * 2) { i -> samples[(i / 2) * channels + (i % 2)] }
        }
      }
  }
}
