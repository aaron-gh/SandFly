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

package com.google.android.accessibility.whisper

import android.util.Log
import java.io.Closeable
import java.io.File
import java.io.IOException

/**
 * A Whisper speech recognition model in whisper.cpp's format, loaded from [file]. Loading and
 * [transcribe] block, so call them off the main thread; [cancel] can be called from any thread.
 */
class WhisperModel
private constructor(
  private val native: NativeWhisper,
  private var handle: Long,
  /** Whether the model runs on the GPU. */
  val onGpu: Boolean,
) : Closeable {
  // [transcribe] holds this; [close] waits for it. [handle] itself is guarded by [handleLock], so
  // that [cancel] doesn't wait for the transcription it stops.
  private val transcribeLock = Any()
  private val handleLock = Any()

  /**
   * The text spoken in [samples], 16 kHz mono audio from -1 to 1. [language] is a two-letter code,
   * or "auto" to detect it. With [shortContext], for models fine-tuned for it, the encoder takes
   * only as much audio as was spoken rather than 30 seconds. Returns null when cancelled.
   */
  fun transcribe(
    samples: FloatArray,
    language: String,
    threads: Int,
    shortContext: Boolean = false,
  ): String? =
    synchronized(transcribeLock) {
      val current = synchronized(handleLock) { handle }
      check(current != 0L) { "Closed" }
      val context = if (shortContext) audioContext(samples.size) else 0
      val bytes =
        native.nativeTranscribe(current, samples, language, threads, context) ?: return null
      String(bytes, Charsets.UTF_8).trim()
    }

  /** Stops [transcribe], which then returns null. */
  fun cancel() {
    synchronized(handleLock) { if (handle != 0L) native.nativeCancel(handle) }
  }

  override fun close() {
    cancel()
    synchronized(transcribeLock) {
      synchronized(handleLock) {
        if (handle != 0L) native.nativeFree(handle)
        handle = 0L
      }
    }
  }

  companion object {
    private const val TAG = "WhisperModel"

    /**
     * The encoder's audio context for [samples] samples: 50 for each second of audio, as the full
     * 1500 is 30 seconds, with a margin, as FUTO's voice input sets it for these models.
     */
    fun audioContext(samples: Int): Int = ((samples + 319) / 320 + 32).coerceIn(160, 1500)

    /** Whether Whisper knows the language with the two-letter code [code]. */
    fun isLanguage(code: String): Boolean =
      code.isNotEmpty() && WhisperNative.nativeLanguageId(code) >= 0

    /**
     * Loads the model in [file], on the GPU when [gpu] is true and the GPU build can load it, and
     * otherwise on the CPU.
     */
    @Throws(IOException::class)
    fun load(file: File, gpu: Boolean = false): WhisperModel {
      if (gpu) {
        try {
          val handle = WhisperOpenClNative.nativeLoad(file.absolutePath, true)
          if (handle != 0L) return WhisperModel(WhisperOpenClNative, handle, onGpu = true)
          Log.w(TAG, "The GPU build could not load ${file.name}, using the CPU")
        } catch (e: LinkageError) {
          // No OpenCL driver, or one too old for the GPU build.
          Log.w(TAG, "No GPU build: $e")
        }
      }
      val handle = WhisperNative.nativeLoad(file.absolutePath, false)
      if (handle == 0L) throw IOException("Could not load ${file.name}")
      return WhisperModel(WhisperNative, handle, onGpu = false)
    }
  }
}
