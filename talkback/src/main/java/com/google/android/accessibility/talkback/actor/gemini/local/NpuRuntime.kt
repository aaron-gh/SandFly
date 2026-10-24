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
 *   Hexagon NPU. The dispatch library refuses a QNN runtime older than the QAIRT SDK it was built
 *   with, which for LiteRT 2.3.0 is QAIRT 2.50.0.
 */
object NpuRuntime {
  private const val TAG = "NpuRuntime"
  private const val MAX_LIBRARY_BYTES = 40L * 1024 * 1024
  private const val TIMEOUT_MS = 30_000

  private const val LITERT_VERSION = "2.3.0"
  private const val QNN_VERSION = "2.50.0"
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

  /**
   * A library in the 64-bit ARM folder of a package. [preload] marks one that another library
   * opens by its bare name, which Android looks for only in SandFly's own library folder, so
   * [load] loads it from the download folder first.
   */
  private class Library(
    val packageUrl: String,
    val name: String,
    val sha256: String,
    val preload: Boolean = false,
  )

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
      Library(
        QNN_AAR,
        "libQnnHtp.so",
        "30e2981965e687d71626c91397867da81247c639e598f49fa5d139cbb21255d0",
        preload = true,
      ),
      Library(
        QNN_AAR,
        "libQnnSystem.so",
        "ca9e5429c77380e1c3f5a401f00288915ec2f6cc478979bafd57c665391d432f",
        preload = true,
      ),
    )

  /** The libraries for [chip]'s NPU, or empty when SandFly has none for it. */
  private fun librariesFor(chip: String): List<Library> =
    when (chip) {
      Accelerators.TENSOR_G5,
      Accelerators.TENSOR_G6 -> TENSOR
      Accelerators.SNAPDRAGON_8_ELITE ->
        QUALCOMM_COMMON +
          listOf(
            Library(
              QNN_AAR,
              "libQnnHtpV79Stub.so",
              "d9f5eca67297125c572995526fc97b1c04cf6b8eb14c81eb0cef5115f58aa766",
              preload = true,
            ),
            Library(QNN_AAR, "libQnnHtpV79Skel.so", "860c9d2e7c937c9fb8f8f18daa9a79cab6c566066a2d36f235f6c8708fdc75bd"),
          )
      Accelerators.SNAPDRAGON_8_ELITE_GEN_5 ->
        QUALCOMM_COMMON +
          listOf(
            Library(
              QNN_AAR,
              "libQnnHtpV81Stub.so",
              "e672e6047088a4440e3027663805b6de7b0531f3e21c45ddb2e19e3f29583d6c",
              preload = true,
            ),
            Library(QNN_AAR, "libQnnHtpV81Skel.so", "02047c9fef8a22801c0eefaa79188e87b600372c9813dea3f621ba256d1ddce0"),
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
    deleteOtherVersions(context, dir)
    dir.mkdirs()
    libraries
      .filter { !File(dir, it.name).isFile }
      .groupBy { it.packageUrl }
      .forEach { (url, missing) -> extract(url, missing, dir) }
    return dir
  }

  /**
   * Loads the libraries for [chip] that are opened by bare name, from the folder that [ensure]
   * returned, into this process, so that LiteRT-LM's NPU backend finds them. Call it in the process
   * that runs the model, before the model loads.
   */
  @Throws(IOException::class)
  fun load(dir: File, chip: String) {
    for (library in librariesFor(chip).filter { it.preload }) {
      try {
        System.load(File(dir, library.name).path)
      } catch (e: UnsatisfiedLinkError) {
        throw IOException("Could not load ${library.name}: ${e.message}", e)
      }
    }
  }

  /** Deletes the folders of other chips and versions, such as those from before an update. */
  private fun deleteOtherVersions(context: Context, keep: File) {
    context.filesDir
      .listFiles { file -> file.isDirectory && file.name.startsWith("npu-") && file != keep }
      ?.forEach {
        if (it.deleteRecursively()) Log.i(TAG, "Deleted ${it.name}")
      }
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
