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

import java.io.Closeable
import java.io.File
import java.io.IOException

/**
 * A Whisper speech recognition model in whisper.cpp's format, loaded from [file]. Loading and
 * [transcribe] block, so call them off the main thread; [cancel] can be called from any thread.
 */
class WhisperModel private constructor(private var handle: Long) : Closeable {
  // [transcribe] holds this; [close] waits for it. [handle] itself is guarded by [handleLock], so
  // that [cancel] doesn't wait for the transcription it stops.
  private val transcribeLock = Any()
  private val handleLock = Any()

  /**
   * The text spoken in [samples], 16 kHz mono audio from -1 to 1. [language] is a two-letter code,
   * or "auto" to detect it. Returns null when cancelled.
   */
  fun transcribe(samples: FloatArray, language: String, threads: Int): String? =
    synchronized(transcribeLock) {
      val current = synchronized(handleLock) { handle }
      check(current != 0L) { "Closed" }
      val bytes = nativeTranscribe(current, samples, language, threads) ?: return null
      String(bytes, Charsets.UTF_8).trim()
    }

  /** Stops [transcribe], which then returns null. */
  fun cancel() {
    synchronized(handleLock) { if (handle != 0L) nativeCancel(handle) }
  }

  override fun close() {
    cancel()
    synchronized(transcribeLock) {
      synchronized(handleLock) {
        if (handle != 0L) nativeFree(handle)
        handle = 0L
      }
    }
  }

  companion object {
    init {
      System.loadLibrary("sandfly_whisper")
    }

    @Throws(IOException::class)
    fun load(file: File): WhisperModel {
      val handle = nativeLoad(file.absolutePath)
      if (handle == 0L) throw IOException("Could not load ${file.name}")
      return WhisperModel(handle)
    }

    /** Whether Whisper knows the language with the two-letter code [code]. */
    fun isLanguage(code: String): Boolean = code.isNotEmpty() && nativeLanguageId(code) >= 0

    @JvmStatic private external fun nativeLanguageId(code: String): Int

    @JvmStatic private external fun nativeLoad(path: String): Long

    @JvmStatic private external fun nativeFree(handle: Long)

    @JvmStatic private external fun nativeCancel(handle: Long)

    @JvmStatic
    private external fun nativeTranscribe(
      handle: Long,
      samples: FloatArray,
      language: String,
      threads: Int,
    ): ByteArray?
  }
}
