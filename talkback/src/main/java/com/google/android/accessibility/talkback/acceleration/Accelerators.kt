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

package com.google.android.accessibility.talkback.acceleration

import android.os.Build
import java.io.File

/**
 * Which processors besides the CPU this phone can run models on. Found from the lists of vendor
 * libraries that apps may load and from the chip's name, without loading any library, so that
 * nothing for a GPU or NPU is loaded on a phone that has none.
 */
object Accelerators {
  /** The vendor libraries apps may load, from Android's public.libraries files. */
  private val publicLibraries: Set<String> by lazy {
    val dirs = listOf("/system/etc", "/vendor/etc", "/odm/etc", "/product/etc", "/system_ext/etc")
    dirs
      .flatMap { dir ->
        File(dir).listFiles { file -> file.name.startsWith("public.libraries") && file.name.endsWith(".txt") }
          .orEmpty()
          .toList()
      }
      .flatMap { file -> runCatching { file.readLines() }.getOrDefault(emptyList()) }
      .map { it.trim().substringBefore(' ') }
      .filter { it.endsWith(".so") }
      .toSet()
  }

  // Null outside Android, as in unit tests.
  private val is64BitArm: Boolean
    get() = (Build.SUPPORTED_ABIS as Array<String>?)?.contains("arm64-v8a") == true

  /** Whether the phone has an OpenCL driver that apps may use, as GPU models need. */
  val hasOpenCl: Boolean by lazy { is64BitArm && "libOpenCL.so" in publicLibraries }

  /** Whether the GPU is a Qualcomm Adreno, whose OpenCL driver Whisper's GPU code is made for. */
  val hasAdrenoOpenCl: Boolean by lazy {
    hasOpenCl &&
      (socManufacturer().let { it == "qti" || it == "qualcomm" } ||
        File("/vendor/lib64/egl/libGLESv2_adreno.so").exists())
  }

  /**
   * The chip whose NPU on-device AI can use, such as [TENSOR_G5] or [SNAPDRAGON_8_ELITE], or null.
   * Only chips that have a model built for their NPU count, and only when Android lets apps use the
   * NPU's driver.
   */
  val npuChip: String? by lazy {
    if (!is64BitArm) return@lazy null
    val soc = socModel().uppercase().filter { it.isLetterOrDigit() }
    when {
      soc == "TENSORG5" && "libedgetpu_litert.so" in publicLibraries -> TENSOR_G5
      soc == "TENSORG6" && "libedgetpu_litert.so" in publicLibraries -> TENSOR_G6
      soc.startsWith("SM8750") && "libcdsprpc.so" in publicLibraries -> SNAPDRAGON_8_ELITE
      soc.startsWith("SM8850") && "libcdsprpc.so" in publicLibraries -> SNAPDRAGON_8_ELITE_GEN_5
      else -> null
    }
  }

  const val TENSOR_G5 = "Tensor_G5"
  const val TENSOR_G6 = "Tensor_G6"
  /** Snapdragon 8 Elite, with a Hexagon v79 NPU, as in the Galaxy S25 and Z Fold7. */
  const val SNAPDRAGON_8_ELITE = "SM8750"
  /** Snapdragon 8 Elite Gen 5, with a Hexagon v81 NPU. */
  const val SNAPDRAGON_8_ELITE_GEN_5 = "SM8850"

  private fun socModel(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) (Build.SOC_MODEL as String?).orEmpty() else ""

  private fun socManufacturer(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) (Build.SOC_MANUFACTURER as String?).orEmpty().lowercase()
    else ""
}
