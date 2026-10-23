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

package com.google.android.accessibility.talkback.actor.gemini.local

import android.content.Context
import android.util.Log
import com.google.android.accessibility.talkback.acceleration.Accelerators
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * The libraries through which LiteRT-LM reaches a phone's NPU. They are downloaded on the phones
 * that use them, from the packages that Google and Qualcomm publish on Maven, so that they are
 * neither in SandFly nor loaded on other phones, and checked against the hashes recorded here.
 *
 * - Google Tensor: LiteRT's dispatch library for Tensor.
 * - Snapdragon: LiteRT's dispatch library for Qualcomm, and Qualcomm's QNN runtime for the chip's
 *   Hexagon NPU, the versions Google's FastVLM sample for Qualcomm NPUs uses.
 */
object NpuRuntime {
  private const val TAG = "NpuRuntime"
  private const val MAX_LIBRARY_BYTES = 40L * 1024 * 1024
  private const val TIMEOUT_MS = 30_000

  private const val LITERT_VERSION = "2.3.0"
  private const val QNN_VERSION = "2.45.0"
  private const val GOOGLE_MAVEN = "https://dl.google.com/android/maven2/com/google/ai/edge/litert"
  private const val TENSOR_AAR =
    "$GOOGLE_MAVEN/litert-npu-runtime-google-tensor/$LITERT_VERSION/" +
      "litert-npu-runtime-google-tensor-$LITERT_VERSION.aar"
  private const val QUALCOMM_AAR =
    "$GOOGLE_MAVEN/litert-npu-runtime-qualcomm/$LITERT_VERSION/" +
      "litert-npu-runtime-qualcomm-$LITERT_VERSION.aar"
  private const val QNN_AAR =
    "https://repo1.maven.org/maven2/com/qualcomm/qti/qnn-runtime/$QNN_VERSION/" +
      "qnn-runtime-$QNN_VERSION.aar"

  /** A library in the 64-bit ARM folder of a package. */
  private class Library(val packageUrl: String, val name: String, val sha256: String)

  private val TENSOR =
    listOf(
      Library(
        TENSOR_AAR,
        "libLiteRtDispatch_GoogleTensor.so",
        "e38144002de11a15f653bb34f1c7efa4cb5af7acdddca424eb32297c5b5f9391",
      )
    )

  private val QUALCOMM_COMMON =
    listOf(
      Library(
        QUALCOMM_AAR,
        "libLiteRtDispatch_Qualcomm.so",
        "22e2d581555617c4a4dabc46ed74833829816e9cd13a0eea27dd4d2178049751",
      ),
      Library(QNN_AAR, "libQnnHtp.so", "0d571eca017fa0c36baef5182e0c137d7959db2bff5aea9b0a8cf49377fb1b14"),
      Library(QNN_AAR, "libQnnSystem.so", "2c3b913e54ec36ad2a75ff8ba392c4689fe44d45a0d06653bbd49cb56eb53971"),
    )

  /** The libraries for [chip]'s NPU, or empty when SandFly has none for it. */
  private fun librariesFor(chip: String): List<Library> =
    when (chip) {
      Accelerators.TENSOR_G5,
      Accelerators.TENSOR_G6 -> TENSOR
      Accelerators.SNAPDRAGON_8_ELITE ->
        QUALCOMM_COMMON +
          listOf(
            Library(QNN_AAR, "libQnnHtpV79Stub.so", "d866680209a48b519f982c77242b0c95ebe7ef7c8e992d91861c198c9cb0e4cc"),
            Library(QNN_AAR, "libQnnHtpV79Skel.so", "9e2a40fbd89f4112b60701fe4805067bae8e7aa96673b6482f5d39bdbd9c77be"),
          )
      Accelerators.SNAPDRAGON_8_ELITE_GEN_5 ->
        QUALCOMM_COMMON +
          listOf(
            Library(QNN_AAR, "libQnnHtpV81Stub.so", "ef0cc96ab5b4cccce59f1c02d59bb3f288d00f0045d763651ccb35591695ed0b"),
            Library(QNN_AAR, "libQnnHtpV81Skel.so", "4f061ffb402588ea619d648733b26c7d5ad3cbe9712715586b76ea94187b3b01"),
          )
      else -> emptyList()
    }

  /** The folder that holds [chip]'s libraries, for LiteRT-LM's NPU backend. */
  fun directory(context: Context, chip: String) =
    File(context.filesDir, "npu-$chip-$LITERT_VERSION-$QNN_VERSION")

  /**
   * Downloads the libraries for [chip] that aren't on the phone yet, and returns their folder.
   * Blocks, so call it off the main thread.
   */
  @Synchronized
  @Throws(IOException::class)
  fun ensure(context: Context, chip: String): File {
    val libraries = librariesFor(chip)
    if (libraries.isEmpty()) throw IOException("No NPU libraries for $chip")
    val dir = directory(context, chip)
    dir.mkdirs()
    libraries
      .filter { !File(dir, it.name).isFile }
      .groupBy { it.packageUrl }
      .forEach { (url, missing) -> extract(url, missing, dir) }
    return dir
  }

  /** Reads the package at [url] once, saving [libraries] from it into [dir]. */
  private fun extract(url: String, libraries: List<Library>, dir: File) {
    val wanted = libraries.associateBy { "jni/arm64-v8a/${it.name}" }.toMutableMap()
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
      connection.connectTimeout = TIMEOUT_MS
      connection.readTimeout = TIMEOUT_MS
      if (connection.responseCode != HttpURLConnection.HTTP_OK) {
        throw IOException("HTTP ${connection.responseCode} for $url")
      }
      ZipInputStream(connection.inputStream).use { zip ->
        while (wanted.isNotEmpty()) {
          val entry = zip.nextEntry ?: break
          val library = wanted.remove(entry.name) ?: continue
          save(zip, library, dir)
        }
      }
    } finally {
      connection.disconnect()
    }
    if (wanted.isNotEmpty()) throw IOException("Missing from $url: ${wanted.keys}")
  }

  private fun save(zip: ZipInputStream, library: Library, dir: File) {
    val part = File(dir, "${library.name}.part")
    try {
      val digest = MessageDigest.getInstance("SHA-256")
      var total = 0L
      part.outputStream().use { output ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
          val read = zip.read(buffer)
          if (read < 0) break
          total += read
          if (total > MAX_LIBRARY_BYTES) throw IOException("${library.name} is too large")
          digest.update(buffer, 0, read)
          output.write(buffer, 0, read)
        }
      }
      val hash = digest.digest().joinToString("") { "%02x".format(it) }
      if (hash != library.sha256) throw IOException("${library.name} doesn't match its hash")
      if (ModelFiles.moveIntoPlace(part, File(dir, library.name)) != null) {
        throw IOException("Could not save ${library.name}")
      }
      Log.i(TAG, "Installed ${library.name}")
    } finally {
      part.delete()
    }
  }
}
