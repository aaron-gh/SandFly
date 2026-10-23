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

/** The calls into one build of whisper.cpp. */
internal interface NativeWhisper {
  fun nativeLoad(path: String, gpu: Boolean): Long

  fun nativeLanguageId(code: String): Int

  fun nativeFree(handle: Long)

  fun nativeCancel(handle: Long)

  fun nativeTranscribe(handle: Long, samples: FloatArray, language: String, threads: Int): ByteArray?
}

/** whisper.cpp for the CPU, in libsandfly_whisper.so. */
internal object WhisperNative : NativeWhisper {
  init {
    System.loadLibrary("sandfly_whisper")
  }

  external override fun nativeLoad(path: String, gpu: Boolean): Long

  external override fun nativeLanguageId(code: String): Int

  external override fun nativeFree(handle: Long)

  external override fun nativeCancel(handle: Long)

  external override fun nativeTranscribe(
    handle: Long,
    samples: FloatArray,
    language: String,
    threads: Int,
  ): ByteArray?
}

/**
 * whisper.cpp with its OpenCL backend for Adreno GPUs, in libsandfly_whisper_opencl.so. That
 * library needs the GPU vendor's libOpenCL.so, so it is loaded only when the GPU is used.
 */
internal object WhisperOpenClNative : NativeWhisper {
  init {
    System.loadLibrary("sandfly_whisper_opencl")
  }

  external override fun nativeLoad(path: String, gpu: Boolean): Long

  external override fun nativeLanguageId(code: String): Int

  external override fun nativeFree(handle: Long)

  external override fun nativeCancel(handle: Long)

  external override fun nativeTranscribe(
    handle: Long,
    samples: FloatArray,
    language: String,
    threads: Int,
  ): ByteArray?
}
