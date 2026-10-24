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

import android.os.SystemClock
import android.system.ErrnoException
import android.system.Os
import com.google.android.accessibility.talkback.acceleration.Accelerators
import com.google.android.accessibility.whisper.WhisperModel
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.File
import java.io.IOException

/** Whisper through whisper.cpp. */
object WhisperEngines {
  private const val TAG = "WhisperEngines"
  private const val MAX_THREADS = 4
  private const val SAMPLE_RATE = 16_000
  private const val KERNEL_CACHE_VARIABLE = "GGML_OPENCL_KERNEL_CACHE_DIR"

  @JvmStatic fun isAvailable(): Boolean = true

  /** Whether Whisper can run on this phone's GPU: an Adreno GPU with an OpenCL driver. */
  fun gpuAvailable(): Boolean = Accelerators.hasAdrenoOpenCl

  /**
   * Transcribes with the model in [file], on the GPU when [gpu] is true and it can. The model is
   * loaded for each transcription and closed after it, so that it takes no memory between
   * dictations. [build] tells this install of SandFly from others, as [load] says.
   */
  fun transcriber(file: File, gpu: Boolean, build: String): SpeechTranscriber? =
    object : SpeechTranscriber {
      @Volatile private var model: WhisperModel? = null
      @Volatile private var cancelled = false

      override fun transcribe(samples: FloatArray, language: String): String? {
        if (cancelled) return null
        val loadStart = SystemClock.elapsedRealtime()
        val loaded =
          try {
            load(file, gpu && gpuAvailable(), build)
          } catch (e: IOException) {
            LogUtils.e(TAG, "Could not load %s: %s", file.name, e)
            return null
          }
        model = loaded
        try {
          if (cancelled) return null
          val code = if (WhisperModel.isLanguage(language)) language else "auto"
          val threads = Runtime.getRuntime().availableProcessors().coerceIn(1, MAX_THREADS)
          val transcribeStart = SystemClock.elapsedRealtime()
          val text = loaded.transcribe(samples, code, threads)
          LogUtils.i(
            TAG,
            "%s on the %s: loaded in %d ms, %d ms of audio transcribed in %d ms",
            file.name,
            if (loaded.onGpu) "GPU" else "CPU",
            transcribeStart - loadStart,
            samples.size * 1000L / SAMPLE_RATE,
            SystemClock.elapsedRealtime() - transcribeStart,
          )
          return text
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

  /**
   * Loads the model in [file], on the GPU if [gpu]. A crash in the GPU driver or the GPU build
   * stops SandFly while it loads, so a marker is kept during the load. If it is still there,
   * SandFly stopped during the last load, and this install, [build], loads on the CPU from then on.
   */
  private fun load(file: File, gpu: Boolean, build: String): WhisperModel {
    if (!gpu) return WhisperModel.load(file, gpu = false)
    // The GPU build compiles its kernels when it loads a model, which takes many seconds. Its cache
    // of compiled kernels is turned off unless it is given a folder it can write to.
    File(file.parentFile, "opencl-cache").let { cache ->
      try {
        Os.setenv(KERNEL_CACHE_VARIABLE, cache.absolutePath, /* overwrite= */ true)
      } catch (e: ErrnoException) {
        LogUtils.w(TAG, "Cannot set the kernel cache: %s", e)
      }
    }
    val marker = File(file.parentFile, ".${file.name}.gpu-loading")
    if (runCatching { marker.readText() }.getOrNull() == build) {
      LogUtils.w(
        TAG, "SandFly stopped while loading %s on the GPU, so it runs on the CPU", file.name)
      return WhisperModel.load(file, gpu = false)
    }
    try {
      marker.writeText(build)
    } catch (e: IOException) {
      LogUtils.w(TAG, "Cannot mark the GPU load: %s", e)
    }
    try {
      return WhisperModel.load(file, gpu = true)
    } finally {
      marker.delete()
    }
  }
}
