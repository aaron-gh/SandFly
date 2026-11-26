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

package com.google.android.accessibility.talkback.recognition

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.media.AudioFormat
import android.media.AudioTrack
import com.google.android.accessibility.utils.output.FeedbackController
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/**
 * The light level of camera frames for the Light level tone mode, from 0 (dark) to 1 (bright).
 * With the exposure of the frame, the level is the brightness of the scene, as automatic exposure
 * makes most frames about as bright as each other; without it, the brightness of the frame.
 */
internal object LightToneLevel {
  /** The lowest and highest pitch of the tone, three octaves apart. */
  const val MIN_HZ = 200.0
  const val MAX_HZ = 1600.0

  /** The exposure values (EV at ISO 100) at level 0 and level 1: a dark room and sunlight. */
  private const val MIN_EV = -2.0
  private const val MAX_EV = 16.0

  /** The linear brightness that automatic exposure aims for: 18% grey. */
  private const val MID_GREY = 0.18

  /** The aperture when the camera doesn't report it, the f-number of most phone main cameras. */
  private const val DEFAULT_APERTURE = 1.8f

  /** Pixels read in each direction: one in [STEP] columns of one in [STEP] rows. */
  private const val STEP = 8

  /** Linear light of each 8-bit luma value, with the sRGB transfer function. */
  private val LINEAR =
    FloatArray(256) {
      val v = it / 255.0
      (if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)).toFloat()
    }

  /**
   * The mean linear light, 0 to 1, of the luma plane in [buffer], [width] by [height] pixels with
   * [rowStride] bytes between rows and [pixelStride] between pixels. Reads with absolute gets, so
   * the buffer's position is left as it was. Returns -1 when the plane is empty or too small for
   * its size.
   */
  fun meanLinear(buffer: ByteBuffer, width: Int, height: Int, rowStride: Int, pixelStride: Int): Float {
    if (width <= 0 || height <= 0 || pixelStride <= 0 || rowStride <= 0) return -1f
    val last = (height - 1).toLong() * rowStride + (width - 1).toLong() * pixelStride
    if (last >= buffer.limit()) return -1f
    var sum = 0.0
    var count = 0
    var y = STEP / 2
    while (y < height) {
      val row = y * rowStride
      var x = STEP / 2
      while (x < width) {
        sum += LINEAR[buffer.get(row + x * pixelStride).toInt() and 0xFF]
        count++
        x += STEP
      }
      y += STEP
    }
    if (count == 0) return -1f
    return (sum / count).toFloat()
  }

  /** The level of a frame of brightness [meanLinear] with no exposure known. */
  fun level(meanLinear: Float): Float = encode(meanLinear.coerceIn(0f, 1f))

  /**
   * The level of the scene in a frame of brightness [meanLinear], taken with [exposureNs] of
   * exposure at [iso] through [aperture] (0 when not known).
   */
  fun level(meanLinear: Float, exposureNs: Long, iso: Int, aperture: Float): Float {
    if (exposureNs <= 0 || iso <= 0) return level(meanLinear)
    val n = if (aperture > 0f) aperture.toDouble() else DEFAULT_APERTURE.toDouble()
    val seconds = exposureNs / 1e9
    val light = max(meanLinear.toDouble(), 1e-4) / MID_GREY
    val ev = log2(n * n / seconds) - log2(iso / 100.0) + log2(light)
    return ((ev - MIN_EV) / (MAX_EV - MIN_EV)).coerceIn(0.0, 1.0).toFloat()
  }

  /** The pitch of [level], on a log scale from [MIN_HZ] to [MAX_HZ]. */
  fun frequency(level: Float): Double = MIN_HZ * (MAX_HZ / MIN_HZ).pow(level.coerceIn(0f, 1f).toDouble())

  /** sRGB-encodes linear light, so that the level follows how bright the frame looks. */
  private fun encode(linear: Float): Float =
    if (linear <= 0.0031308f) linear * 12.92f
    else (1.055 * linear.toDouble().pow(1 / 2.4) - 0.055).toFloat()
}

/**
 * The exposure of the latest frames, from the camera's capture results. Written on the camera's
 * thread, read on the analysis thread.
 */
internal class CameraExposure : CameraCaptureSession.CaptureCallback() {
  @Volatile var exposureNs = 0L
    private set
  @Volatile var iso = 0
    private set
  @Volatile var aperture = 0f
    private set

  override fun onCaptureCompleted(
    session: CameraCaptureSession,
    request: CaptureRequest,
    result: TotalCaptureResult,
  ) {
    exposureNs = result.get(CaptureResult.SENSOR_EXPOSURE_TIME) ?: 0L
    iso = result.get(CaptureResult.SENSOR_SENSITIVITY) ?: 0
    aperture = result.get(CaptureResult.LENS_APERTURE) ?: 0f
  }
}

/**
 * A sine tone that glides to each new pitch and fades in and out, so that changes never click.
 * One thread only.
 */
internal class ToneSynth(private val sampleRate: Int) {
  private val glide = 1 - exp(-1.0 / (sampleRate * GLIDE_SECONDS))
  private val fade = 1 - exp(-1.0 / (sampleRate * FADE_SECONDS))
  private var phase = 0.0

  /** The pitch now, or NaN before the first one. */
  var hz = Double.NaN
    private set

  /** The amplitude now, 0 to 1. */
  var amplitude = 0.0
    private set

  /**
   * Writes [count] samples to [out], moving towards [targetHz] (NaN keeps the pitch) and
   * [targetAmplitude], 0 to 1.
   */
  fun fill(out: ShortArray, count: Int, targetHz: Double, targetAmplitude: Double) {
    if (hz.isNaN()) hz = targetHz
    if (hz.isNaN()) {
      out.fill(0, 0, count)
      return
    }
    val toHz = if (targetHz.isNaN()) hz else targetHz
    for (i in 0 until count) {
      hz += (toHz - hz) * glide
      amplitude += (targetAmplitude - amplitude) * fade
      phase += 2 * PI * hz / sampleRate
      if (phase >= 2 * PI) phase -= 2 * PI
      out[i] = (sin(phase) * amplitude * Short.MAX_VALUE).toInt().toShort()
    }
  }

  private companion object {
    /** How fast the pitch follows: about 63% of the way in this time. */
    const val GLIDE_SECONDS = 0.08
    /** How fast the tone fades in and out. */
    const val FADE_SECONDS = 0.1
  }
}

/**
 * Plays the Light level tone: a quiet continuous sine whose pitch follows [setLevel], with the
 * attributes of SandFly's sounds. Plays on a thread of its own that ends when [stop] is called.
 * [start] and [stop] on the main thread; [setLevel] on any thread.
 */
internal class LightTonePlayer {
  @Volatile private var level = Float.NaN
  private var session: Session? = null

  /** Sets the light level, 0 to 1. The tone starts at the first level. */
  fun setLevel(value: Float) {
    level = value
  }

  fun start() {
    if (session != null) return
    level = Float.NaN
    val created = Session()
    session = created
    Thread(created, "LightTone").start()
  }

  /** Fades the tone out and releases its track. */
  fun stop() {
    session?.running = false
    session = null
  }

  private inner class Session : Runnable {
    @Volatile var running = true

    override fun run() {
      var track: AudioTrack? = null
      try {
        val minBytes =
          AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val created =
          AudioTrack.Builder()
            .setAudioAttributes(FeedbackController.feedbackAttributes())
            .setAudioFormat(
              AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            )
            .setBufferSizeInBytes(max(minBytes, CHUNK * Short.SIZE_BYTES * BUFFER_CHUNKS))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track = created
        if (created.state != AudioTrack.STATE_INITIALIZED) {
          LogUtils.w(TAG, "Light tone track not initialized")
          return
        }
        val synth = ToneSynth(SAMPLE_RATE)
        val samples = ShortArray(CHUNK)
        created.play()
        while (running) {
          val value = level
          if (value.isNaN()) synth.fill(samples, CHUNK, Double.NaN, 0.0)
          else synth.fill(samples, CHUNK, LightToneLevel.frequency(value), VOLUME)
          // Blocks until the track has room, so the loop runs at the pace of playback.
          if (created.write(samples, 0, CHUNK) < 0) return
        }
        // Fades out, then adds silence, so that releasing the track cuts nothing audible.
        var left = FADE_OUT_CHUNKS
        while (left-- > 0 && synth.amplitude > SILENT) {
          synth.fill(samples, CHUNK, Double.NaN, 0.0)
          if (created.write(samples, 0, CHUNK) < 0) return
        }
        samples.fill(0)
        repeat(BUFFER_CHUNKS) { if (created.write(samples, 0, CHUNK) < 0) return }
        created.stop()
      } catch (e: Throwable) {
        // Includes the RuntimeExceptions of a track that cannot be created or started.
        LogUtils.w(TAG, "Light tone stopped: %s", e.javaClass.simpleName)
      } finally {
        track?.release()
      }
    }
  }

  private companion object {
    const val TAG = "LightTonePlayer"
    const val SAMPLE_RATE = 48_000
    /** 20 ms of samples, written at a time. */
    const val CHUNK = SAMPLE_RATE / 50
    const val BUFFER_CHUNKS = 4
    const val FADE_OUT_CHUNKS = 25
    /** The amplitude of the tone, to stay below speech. */
    const val VOLUME = 0.12
    const val SILENT = 0.0005
  }
}
