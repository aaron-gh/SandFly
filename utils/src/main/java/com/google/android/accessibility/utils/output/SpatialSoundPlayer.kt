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
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaDataSource
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import com.google.android.accessibility.utils.R
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Plays short sounds in 3D through headphones with [Hrtf]. Sounds are rendered and played on a
 * thread of their own, and a new sound stops the one before it, as in Unspoken, so swiping quickly
 * never piles sounds up.
 *
 * Sounds can be 16 bit WAV, or anything Android can decode, such as Ogg Vorbis. They are mixed down
 * to mono and resampled to the 44.1 kHz of the HRTFs once, the first time they play.
 */
class SpatialSoundPlayer(private val context: Context) {
  private val thread = HandlerThread("SpatialSoundPlayer").apply { start() }
  private val handler = Handler(thread.looper)

  // Everything below is used only on the player's thread.
  private var hrtf: Hrtf? = null
  // Decoded sounds, by file path for custom sounds and by resource ID otherwise, the least recently
  // played first, holding at most MAX_CACHED_SAMPLES samples in all.
  private val sounds = LinkedHashMap<String, FloatArray>(16, 0.75f, /* accessOrder= */ true)
  private var cachedSamples = 0L
  // Sounds that could not be decoded, which are not tried again until the theme changes.
  private val brokenSounds = HashSet<String>()
  // The sounds playing, with when each was asked for.
  private val tracks = ArrayList<Pair<AudioTrack, Long>>()

  /**
   * Plays [resId], or the file at [path] in its place, as if it came from [x] and [y], fractions of
   * the screen from its left and top edges, at [volume] from 0 to 1.
   */
  fun play(resId: Int, path: String?, x: Float, y: Float, volume: Float) {
    // When it was asked for, since decoding a sound the first time delays its start.
    val requested = SystemClock.uptimeMillis()
    handler.post {
      val key = path ?: "res:$resId"
      if (key in brokenSounds) return@post
      try {
        val mono = sounds[key] ?: decodeOnce(key, resId, path) ?: return@post
        val hrtf = hrtf ?: loadHrtf().also { hrtf = it }
        val stereo =
          hrtf.render(mono, Hrtf.azimuthForScreen(x), Hrtf.elevationForScreen(y))
        // A sound held up behind a slow decode belongs to a moment that has passed, and playing
        // it now would come in a burst with the others held up.
        if (SystemClock.uptimeMillis() - requested > STALE_MS) return@post
        start(stereo, volume, requested)
      } catch (e: Throwable) {
        // A broken sound, a refused audio track or running out of memory must never take the
        // screen reader down, and an OutOfMemoryError is not a RuntimeException.
        LogUtils.e(TAG, "Could not play sound %d: %s", resId, e)
      }
    }
  }

  /**
   * Decodes a sound and keeps it, or returns null and remembers that it is broken, so that a sound
   * that fails once is not decoded again on every focus.
   */
  private fun decodeOnce(key: String, resId: Int, path: String?): FloatArray? {
    val mono =
      try {
        readSound(resId, path)
      } catch (e: Throwable) {
        LogUtils.e(TAG, "Cannot decode sound %s: %s", key, e)
        null
      }
    if (mono == null) {
      brokenSounds += key
      return null
    }
    sounds[key] = mono
    cachedSamples += mono.size
    // Forget the least recently played sounds beyond the limit, but never the one just decoded.
    val oldest = sounds.entries.iterator()
    while (cachedSamples > MAX_CACHED_SAMPLES && sounds.size > 1) {
      val entry = oldest.next()
      cachedSamples -= entry.value.size
      oldest.remove()
    }
    return mono
  }

  /** Drops the decoded sounds, so that custom sounds the user has replaced free their memory. */
  fun forgetSounds() {
    handler.post {
      sounds.clear()
      cachedSamples = 0
      brokenSounds.clear()
    }
  }

  /** Stops the sounds playing now, if any, and frees the thread. */
  fun shutdown() {
    handler.post {
      stopRequestedBefore(Long.MAX_VALUE)
      thread.quitSafely()
    }
  }

  private fun start(stereo: FloatArray, volume: Float, requested: Long) {
    // Sounds of one moment, like a list sound and a control sound, play together. A sound from
    // before stops.
    stopRequestedBefore(requested - TOGETHER_MS)
    val frames = stereo.size / 2
    val newTrack =
      AudioTrack.Builder()
        .setAudioAttributes(FeedbackController.feedbackAttributes())
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setSampleRate(Hrtf.SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()
        )
        .setTransferMode(AudioTrack.MODE_STATIC)
        .setBufferSizeInBytes(stereo.size * Float.SIZE_BYTES)
        .build()
    newTrack.write(stereo, 0, stereo.size, AudioTrack.WRITE_BLOCKING)
    newTrack.setVolume(volume.coerceIn(0f, 1f))
    newTrack.play()
    tracks += newTrack to requested
    // Free the track once it has finished, unless a newer sound has stopped it already.
    val millis = frames * 1000L / Hrtf.SAMPLE_RATE
    handler.postDelayed({ stop(newTrack) }, millis + RELEASE_DELAY_MS)
  }

  /** Stops the sounds asked for before [time]. */
  private fun stopRequestedBefore(time: Long) {
    tracks.filter { (_, requested) -> requested < time }.forEach { (track, _) -> stop(track) }
  }

  private fun stop(track: AudioTrack) {
    if (!tracks.removeAll { it.first === track }) return
    try {
      track.stop()
    } catch (e: IllegalStateException) {
      // Already stopped.
    }
    track.release()
  }

  private fun loadHrtf(): Hrtf =
    context.resources.openRawResource(R.raw.hrtf_kemar).use { Hrtf.parse(it.readBytes()) }

  /**
   * Returns a sound resource, or the file at [path], as mono samples at 44.1 kHz, or null if it
   * cannot be decoded.
   */
  private fun readSound(resId: Int, path: String?): FloatArray? {
    val bytes =
      if (path != null) {
        try {
          File(path).readBytes()
        } catch (e: IOException) {
          LogUtils.w(TAG, "Cannot read sound %s: %s", path, e)
          return null
        }
      } else {
        context.resources.openRawResource(resId).use { it.readBytes() }
      }
    val samples = decodeWav(bytes) ?: decodeWithMediaCodec(bytes)
    if (samples == null || samples.isEmpty()) {
      LogUtils.w(TAG, "Cannot decode sound %d", resId)
      return null
    }
    return samples
  }

  /** Decodes any audio format Android supports, such as Ogg Vorbis, with its decoders. */
  private fun decodeWithMediaCodec(bytes: ByteArray): FloatArray? {
    val extractor = MediaExtractor()
    var codec: MediaCodec? = null
    try {
      extractor.setDataSource(ByteArrayDataSource(bytes))
      val trackIndex =
        (0 until extractor.trackCount).firstOrNull {
          extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: return null
      extractor.selectTrack(trackIndex)
      val inputFormat = extractor.getTrackFormat(trackIndex)
      var rate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
      var channels = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
      if (!isSupportedFormat(rate, channels)) return null
      var encoding = AudioFormat.ENCODING_PCM_16BIT
      val decoder = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME)!!)
      codec = decoder
      decoder.configure(inputFormat, null, null, 0)
      decoder.start()

      val samples = FloatList()
      val info = MediaCodec.BufferInfo()
      var inputDone = false
      // Earcons are short, so a stuck decoder gives up rather than holding the thread.
      var tries = 0
      while (tries++ < MAX_DECODE_STEPS) {
        if (!inputDone) {
          val inputIndex = decoder.dequeueInputBuffer(DECODE_TIMEOUT_US)
          if (inputIndex >= 0) {
            val size = extractor.readSampleData(decoder.getInputBuffer(inputIndex)!!, 0)
            if (size < 0) {
              decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
              inputDone = true
            } else {
              decoder.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
              extractor.advance()
            }
          }
        }
        val outputIndex = decoder.dequeueOutputBuffer(info, DECODE_TIMEOUT_US)
        if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
          val format = decoder.outputFormat
          rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
          channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
          if (!isSupportedFormat(rate, channels)) return null
          if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            encoding = format.getInteger(MediaFormat.KEY_PCM_ENCODING)
          }
        } else if (outputIndex >= 0) {
          val output = decoder.getOutputBuffer(outputIndex)!!.order(ByteOrder.nativeOrder())
          output.position(info.offset)
          output.limit(info.offset + info.size)
          // Each buffer is mixed down to mono as it arrives, so a sound with many channels never
          // needs memory for all of them at once.
          if (encoding == AudioFormat.ENCODING_PCM_FLOAT) {
            val floats = output.asFloatBuffer()
            val frames = floats.remaining() / channels
            if (samples.size + frames > maxInputFrames(rate)) return null
            repeat(frames) {
              var sum = 0f
              repeat(channels) { sum += floats.get() }
              samples.add(sum / channels)
            }
          } else {
            val shorts = output.asShortBuffer()
            val frames = shorts.remaining() / channels
            if (samples.size + frames > maxInputFrames(rate)) return null
            repeat(frames) {
              var sum = 0f
              repeat(channels) { sum += shorts.get() / 32768f }
              samples.add(sum / channels)
            }
          }
          decoder.releaseOutputBuffer(outputIndex, false)
          if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
            return resample(samples.toArray(), rate, Hrtf.SAMPLE_RATE)
          }
        }
      }
      return null
    } catch (e: Exception) {
      LogUtils.w(TAG, "Cannot decode sound: %s", e)
      return null
    } finally {
      codec?.let {
        try {
          it.stop()
        } catch (e: IllegalStateException) {
          // Never started.
        }
        it.release()
      }
      extractor.release()
    }
  }

  private class ByteArrayDataSource(private val bytes: ByteArray) : MediaDataSource() {
    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
      if (position >= bytes.size) return -1
      val count = minOf(size.toLong(), bytes.size - position).toInt()
      System.arraycopy(bytes, position.toInt(), buffer, offset, count)
      return count
    }

    override fun getSize(): Long = bytes.size.toLong()

    override fun close() {}
  }

  /** A growing list of floats, without boxing each sample. */
  private class FloatList {
    private var values = FloatArray(4096)
    var size = 0
      private set

    fun add(value: Float) {
      if (size == values.size) values = values.copyOf(size * 2)
      values[size++] = value
    }

    fun toArray(): FloatArray = values.copyOf(size)
  }

  companion object {
    private const val TAG = "SpatialSoundPlayer"
    private const val RELEASE_DELAY_MS = 200L
    // Sounds asked for this close together are of one moment, and play together.
    private const val TOGETHER_MS = 50L
    // Sounds that could not start within this long after they were asked for are dropped.
    private const val STALE_MS = 300L
    private const val DECODE_TIMEOUT_US = 10_000L
    private const val MAX_DECODE_STEPS = 2_000

    /** The longest sound played, in seconds. Sounds come from themes, so from anyone. */
    const val MAX_SECONDS = 10

    /** The lowest and highest sample rates of a sound, in Hz. */
    const val MIN_SAMPLE_RATE = 8_000
    const val MAX_SAMPLE_RATE = 192_000

    /** The most channels a sound may have. */
    const val MAX_CHANNELS = 8

    /** The most decoded samples kept at once, about 8 MB. */
    private const val MAX_CACHED_SAMPLES = 2_000_000L

    /**
     * Whether a sound at [rate] Hz with [channels] channels can be played. A header can claim
     * anything, and resampling from a rate far below 44.1 kHz would need far more memory than the
     * file holds.
     */
    @JvmStatic
    fun isSupportedFormat(rate: Int, channels: Int): Boolean =
      rate in MIN_SAMPLE_RATE..MAX_SAMPLE_RATE && channels in 1..MAX_CHANNELS

    /** The most frames a sound at [rate] Hz may have. */
    @JvmStatic
    fun maxInputFrames(rate: Int): Int = MAX_SECONDS * rate

    /** Changes the sample rate of [samples] from [from] to [to] by linear interpolation. */
    @JvmStatic
    fun resample(samples: FloatArray, from: Int, to: Int): FloatArray {
      if (from == to || from <= 0 || samples.isEmpty()) return samples
      val count = (samples.size.toLong() * to / from).toInt().coerceAtLeast(1)
      return FloatArray(count) { i ->
        val position = i.toDouble() * from / to
        val index = position.toInt().coerceAtMost(samples.size - 1)
        val next = (index + 1).coerceAtMost(samples.size - 1)
        val fraction = (position - index).toFloat()
        samples[index] * (1 - fraction) + samples[next] * fraction
      }
    }

    /**
     * Decodes a 16 bit PCM WAV file to mono at 44.1 kHz, or returns null for any other format,
     * which [MediaCodec] then decodes.
     */
    @JvmStatic
    fun decodeWav(bytes: ByteArray): FloatArray? {
      val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
      if (bytes.size < 12 || tag(buffer, 0) != "RIFF" || tag(buffer, 8) != "WAVE") return null
      var position = 12
      var channels = 0
      var rate = 0
      while (position + 8 <= bytes.size) {
        val id = tag(buffer, position)
        val size = buffer.getInt(position + 4)
        val body = position + 8
        if (size < 0 || body + size > bytes.size) return null
        when (id) {
          "fmt " -> {
            if (size < 16) return null
            val format = buffer.getShort(body).toInt()
            val bits = buffer.getShort(body + 14).toInt()
            if (format != 1 || bits != 16) return null
            channels = buffer.getShort(body + 2).toInt()
            rate = buffer.getInt(body + 4)
          }
          "data" -> {
            if (!isSupportedFormat(rate, channels)) return null
            if (size / 2 / channels > maxInputFrames(rate)) return null
            // Mixed down to mono as it is read, so many channels never need memory all at once.
            val mono =
              FloatArray(size / 2 / channels) { frame ->
                var sum = 0f
                for (c in 0 until channels) {
                  sum += buffer.getShort(body + 2 * (frame * channels + c)) / 32768f
                }
                sum / channels
              }
            return resample(mono, rate, Hrtf.SAMPLE_RATE)
          }
        }
        // Chunks are padded to an even length.
        position = body + size + (size and 1)
      }
      return null
    }

    private fun tag(buffer: ByteBuffer, at: Int): String =
      String(ByteArray(4) { buffer.get(at + it) }, Charsets.US_ASCII)
  }
}
