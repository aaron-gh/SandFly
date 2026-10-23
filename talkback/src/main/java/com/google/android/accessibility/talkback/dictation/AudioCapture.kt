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

package com.google.android.accessibility.talkback.dictation

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Records the microphone as 16 kHz mono 16-bit audio, the format Whisper and Gemini 3.5 Transcribe
 * take, from [start] until [stop]. Recording ends by itself after [MAX_SECONDS], and then
 * [onLimit] runs on the recording thread, as does [onChunk], with each tenth of a second recorded.
 */
class AudioCapture(
  private val onLimit: () -> Unit,
  private val onChunk: ((ByteArray) -> Unit)? = null,
) {
  private val audio = ByteArrayOutputStream()
  @Volatile private var recording = false
  private var thread: Thread? = null

  /** Starts recording. Returns false when the microphone can't be used. */
  @SuppressLint("MissingPermission") // Dictation checks the permission before it starts.
  fun start(): Boolean {
    val minBuffer =
      AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
    if (minBuffer <= 0) return false
    val record =
      try {
        AudioRecord(
          MediaRecorder.AudioSource.VOICE_RECOGNITION,
          SAMPLE_RATE,
          AudioFormat.CHANNEL_IN_MONO,
          AudioFormat.ENCODING_PCM_16BIT,
          maxOf(minBuffer, SAMPLE_RATE * 2 / 5),
        )
      } catch (e: RuntimeException) {
        LogUtils.e(TAG, "No microphone: %s", e)
        return false
      }
    if (record.state != AudioRecord.STATE_INITIALIZED) {
      record.release()
      return false
    }
    try {
      record.startRecording()
    } catch (e: IllegalStateException) {
      record.release()
      return false
    }
    if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
      record.release()
      return false
    }
    audio.reset()
    recording = true
    thread =
      Thread({ readUntilStopped(record) }, "dictation-recording").apply {
        isDaemon = true
        start()
      }
    return true
  }

  private fun readUntilStopped(record: AudioRecord) {
    val buffer = ByteArray(SAMPLE_RATE / 10 * 2)
    try {
      while (recording) {
        val read = record.read(buffer, 0, buffer.size)
        if (read < 0) {
          LogUtils.e(TAG, "Recording failed: %d", read)
          break
        }
        synchronized(audio) { audio.write(buffer, 0, read) }
        onChunk?.invoke(buffer.copyOf(read))
        if (audio.size() >= MAX_BYTES) {
          recording = false
          onLimit()
        }
      }
    } finally {
      record.stop()
      record.release()
    }
  }

  /** Stops recording and returns the 16-bit samples recorded, little-endian. */
  fun stop(): ByteArray {
    recording = false
    thread?.join(STOP_WAIT_MS)
    thread = null
    return synchronized(audio) { audio.toByteArray().also { audio.reset() } }
  }

  companion object {
    private const val TAG = "AudioCapture"
    const val SAMPLE_RATE = 16_000
    /** Fifteen minutes. Gemini 3.5 Transcribe takes up to an hour; longer would be slow to send. */
    const val MAX_SECONDS = 15 * 60
    private const val MAX_BYTES = MAX_SECONDS * SAMPLE_RATE * 2
    private const val STOP_WAIT_MS = 1_000L

    /** Whether [pcm] is long enough to hold a word: a quarter of a second. */
    fun hasSpeechLength(pcm: ByteArray) = pcm.size >= SAMPLE_RATE / 2

    /** [pcm] as samples from -1 to 1, as Whisper takes them. */
    fun toFloats(pcm: ByteArray): FloatArray {
      val shorts = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
      return FloatArray(shorts.remaining()) { shorts.get(it) / 32768f }
    }

    /** [pcm] in a WAV file. */
    fun toWav(pcm: ByteArray): ByteArray {
      val header = ByteBuffer.allocate(WAV_HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN)
      header.put("RIFF".toByteArray(Charsets.US_ASCII))
      header.putInt(36 + pcm.size)
      header.put("WAVEfmt ".toByteArray(Charsets.US_ASCII))
      header.putInt(16)
      header.putShort(1) // PCM
      header.putShort(1) // Mono
      header.putInt(SAMPLE_RATE)
      header.putInt(SAMPLE_RATE * 2)
      header.putShort(2)
      header.putShort(16)
      header.put("data".toByteArray(Charsets.US_ASCII))
      header.putInt(pcm.size)
      return header.array() + pcm
    }

    private const val WAV_HEADER_BYTES = 44
  }
}
