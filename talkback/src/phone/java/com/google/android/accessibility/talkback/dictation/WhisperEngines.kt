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

import com.google.android.accessibility.talkback.acceleration.Accelerators
import com.google.android.accessibility.whisper.WhisperModel
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.File
import java.io.IOException

/** Whisper through whisper.cpp. */
object WhisperEngines {
  private const val TAG = "WhisperEngines"
  private const val MAX_THREADS = 4

  @JvmStatic fun isAvailable(): Boolean = true

  /** Whether Whisper can run on this phone's GPU: an Adreno GPU with an OpenCL driver. */
  fun gpuAvailable(): Boolean = Accelerators.hasAdrenoOpenCl

  /**
   * Transcribes with the model in [file], on the GPU when [gpu] is true and it can. The model is
   * loaded for each transcription and closed after it, so that it takes no memory between
   * dictations.
   */
  fun transcriber(file: File, gpu: Boolean): SpeechTranscriber? =
    object : SpeechTranscriber {
      @Volatile private var model: WhisperModel? = null
      @Volatile private var cancelled = false

      override fun transcribe(samples: FloatArray, language: String): String? {
        if (cancelled) return null
        val loaded =
          try {
            WhisperModel.load(file, gpu && gpuAvailable())
          } catch (e: IOException) {
            LogUtils.e(TAG, "Could not load %s: %s", file.name, e)
            return null
          }
        model = loaded
        try {
          if (cancelled) return null
          val code = if (WhisperModel.isLanguage(language)) language else "auto"
          val threads = Runtime.getRuntime().availableProcessors().coerceIn(1, MAX_THREADS)
          return loaded.transcribe(samples, code, threads)
        } finally {
          model = null
          loaded.close()
        }
      }

      override fun cancel() {
        cancelled = true
        model?.cancel()
      }
    }
}
