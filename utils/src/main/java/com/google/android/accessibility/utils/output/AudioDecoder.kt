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
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Decodes short sounds to float samples with Android's decoders: Ogg Vorbis, WAV, MP3 and the rest,
 * and MIDI, which Android's extractor renders to raw audio itself.
 *
 * Sounds come from themes, so from anyone. A sound must have a format [SpatialSoundPlayer] accepts,
 * and last no longer than [SpatialSoundPlayer.MAX_SECONDS]. At most 2 channels are kept, as it
 * decodes, so a sound with many channels never needs memory for all of them.
 */
object AudioDecoder {
  private const val TAG = "AudioDecoder"
  private const val TIMEOUT_US = 10_000L
  // Sounds are short, so a decoder that never finishes gives up rather than holding a thread.
  private const val MAX_STEPS = 2_000

  /** Interleaved float samples, from -1 to 1, with 1 or 2 channels. */
  class Decoded(val samples: FloatArray, val channels: Int, val sampleRate: Int)

  /** Decodes a raw resource, or returns null if it cannot be decoded. */
  @JvmStatic
  fun decode(context: Context, resId: Int): Decoded? {
    val extractor = MediaExtractor()
    return try {
      context.resources.openRawResourceFd(resId).use {
        extractor.setDataSource(it.fileDescriptor, it.startOffset, it.length)
      }
      decode(extractor)
    } catch (e: Throwable) {
      // Running out of memory is not an Exception, and must not take the screen reader down.
      LogUtils.w(TAG, "Cannot decode sound %d: %s", resId, e)
      null
    } finally {
      extractor.release()
    }
  }

  /** Decodes a sound file, or returns null if it cannot be decoded. */
  @JvmStatic
  fun decode(path: String): Decoded? {
    val extractor = MediaExtractor()
    return try {
      extractor.setDataSource(path)
      decode(extractor)
    } catch (e: Throwable) {
      LogUtils.w(TAG, "Cannot decode sound %s: %s", path, e)
      null
    } finally {
      extractor.release()
    }
  }

  /** Whether [toFloats] reads audio in [encoding]. */
  @JvmStatic
  fun isSupportedEncoding(encoding: Int): Boolean =
    encoding == AudioFormat.ENCODING_PCM_16BIT ||
      encoding == AudioFormat.ENCODING_PCM_8BIT ||
      encoding == AudioFormat.ENCODING_PCM_FLOAT

  private fun decode(extractor: MediaExtractor): Decoded? {
    val track =
      (0 until extractor.trackCount).firstOrNull {
        extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
      } ?: return null
    extractor.selectTrack(track)
    val format = extractor.getTrackFormat(track)
    val mime = format.getString(MediaFormat.KEY_MIME)!!
    val rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
    val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
    if (!SpatialSoundPlayer.isSupportedFormat(rate, channels)) return null
    return if (mime == MediaFormat.MIMETYPE_AUDIO_RAW) {
      readRaw(extractor, format, rate, channels)
    } else {
      decodeWithCodec(extractor, format, mime, rate, channels)
    }
  }

  /** Reads audio the extractor already gives as samples, such as rendered MIDI. */
  private fun readRaw(
    extractor: MediaExtractor,
    format: MediaFormat,
    rate: Int,
    channels: Int,
  ): Decoded? {
    val encoding = pcmEncoding(format)
    if (!isSupportedEncoding(encoding)) return null
    val samples = FloatList()
    val buffer = ByteBuffer.allocate(64 * 1024)
    repeat(MAX_STEPS) {
      buffer.clear()
      val size = extractor.readSampleData(buffer, 0)
      if (size < 0) return Decoded(samples.toArray(), minOf(channels, 2), rate)
      buffer.limit(size)
      if (!appendFrames(buffer, encoding, channels, rate, samples)) return null
      extractor.advance()
    }
    return null
  }

  private fun decodeWithCodec(
    extractor: MediaExtractor,
    format: MediaFormat,
    mime: String,
    initialRate: Int,
    initialChannels: Int,
  ): Decoded? {
    var rate = initialRate
    var channels = initialChannels
    var encoding = AudioFormat.ENCODING_PCM_16BIT
    val codec = MediaCodec.createDecoderByType(mime)
    try {
      codec.configure(format, null, null, 0)
      codec.start()
      val samples = FloatList()
      val info = MediaCodec.BufferInfo()
      var inputDone = false
      repeat(MAX_STEPS) {
        if (!inputDone) {
          val index = codec.dequeueInputBuffer(TIMEOUT_US)
          if (index >= 0) {
            val size = extractor.readSampleData(codec.getInputBuffer(index)!!, 0)
            if (size < 0) {
              codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
              inputDone = true
            } else {
              codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
              extractor.advance()
            }
          }
        }
        val index = codec.dequeueOutputBuffer(info, TIMEOUT_US)
        if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
          val output = codec.outputFormat
          val newRate = output.getInteger(MediaFormat.KEY_SAMPLE_RATE)
          val newChannels = output.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
          val newEncoding = pcmEncoding(output)
          val changed = newRate != rate || newChannels != channels || newEncoding != encoding
          if (
            !SpatialSoundPlayer.isSupportedFormat(newRate, newChannels) ||
              !isSupportedEncoding(newEncoding) ||
              // Frames kept already are in the old format, and cannot be mixed with the new.
              (changed && samples.size > 0)
          ) {
            return null
          }
          rate = newRate
          channels = newChannels
          encoding = newEncoding
        } else if (index >= 0) {
          val output = codec.getOutputBuffer(index)!!
          output.position(info.offset)
          output.limit(info.offset + info.size)
          if (!appendFrames(output, encoding, channels, rate, samples)) return null
          codec.releaseOutputBuffer(index, false)
          if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
            return Decoded(samples.toArray(), minOf(channels, 2), rate)
          }
        }
      }
      return null
    } finally {
      try {
        codec.stop()
      } catch (e: IllegalStateException) {
        // Never started.
      }
      codec.release()
    }
  }

  private fun pcmEncoding(format: MediaFormat): Int =
    if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
      format.getInteger(MediaFormat.KEY_PCM_ENCODING)
    } else {
      AudioFormat.ENCODING_PCM_16BIT
    }

  /**
   * Appends the frames in [buffer], with [channels] channels in [encoding], to [out], keeping the
   * first 2 channels. Returns false, appending nothing, if the sound would grow longer than
   * [SpatialSoundPlayer.MAX_SECONDS] at [rate] Hz.
   */
  private fun appendFrames(
    buffer: ByteBuffer,
    encoding: Int,
    channels: Int,
    rate: Int,
    out: FloatList,
  ): Boolean {
    val frame = FloatList()
    toFloats(buffer, encoding, frame)
    val frames = frame.size / channels
    val kept = minOf(channels, 2)
    if (out.size / kept + frames > SpatialSoundPlayer.maxInputFrames(rate)) return false
    val values = frame.values()
    for (i in 0 until frames) {
      for (c in 0 until kept) out.add(values[i * channels + c])
    }
    return true
  }

  /** Appends the samples in [buffer], in [encoding], to [out] as floats. */
  @JvmStatic
  fun toFloats(buffer: ByteBuffer, encoding: Int, out: FloatList) {
    val data = buffer.slice().order(ByteOrder.nativeOrder())
    when (encoding) {
      AudioFormat.ENCODING_PCM_FLOAT -> {
        val floats = data.asFloatBuffer()
        while (floats.hasRemaining()) out.add(floats.get())
      }
      AudioFormat.ENCODING_PCM_8BIT -> {
        while (data.hasRemaining()) out.add(((data.get().toInt() and 0xFF) - 128) / 128f)
      }
      else -> {
        val shorts = data.asShortBuffer()
        while (shorts.hasRemaining()) out.add(shorts.get() / 32768f)
      }
    }
  }

  /** A growing list of floats, without boxing each sample. */
  class FloatList {
    private var values = FloatArray(4096)
    var size = 0
      private set

    fun add(value: Float) {
      if (size == values.size) values = values.copyOf(size * 2)
      values[size++] = value
    }

    /** The list's storage, whose first [size] values are the list. */
    internal fun values(): FloatArray = values

    fun toArray(): FloatArray = values.copyOf(size)
  }
}
