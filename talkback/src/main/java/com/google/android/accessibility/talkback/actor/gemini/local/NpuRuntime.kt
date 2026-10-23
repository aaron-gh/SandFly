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
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * The library through which LiteRT-LM reaches the NPU of a Google Tensor chip. It is downloaded
 * from Google's Maven repository on the phones that use it, so that it is neither in SandFly nor
 * loaded on other phones, and checked against the hash recorded here.
 */
object NpuRuntime {
  private const val TAG = "NpuRuntime"
  private const val VERSION = "2.3.0"
  private const val AAR_URL =
    "https://dl.google.com/android/maven2/com/google/ai/edge/litert/litert-npu-runtime-google-tensor/" +
      "$VERSION/litert-npu-runtime-google-tensor-$VERSION.aar"
  private const val ENTRY = "jni/arm64-v8a/libLiteRtDispatch_GoogleTensor.so"
  private const val LIBRARY = "libLiteRtDispatch_GoogleTensor.so"
  private const val SHA256 = "e38144002de11a15f653bb34f1c7efa4cb5af7acdddca424eb32297c5b5f9391"
  private const val MAX_BYTES = 8L * 1024 * 1024
  private const val TIMEOUT_MS = 30_000

  /** The folder that holds the library, for LiteRT-LM's NPU backend. */
  fun directory(context: Context) = File(context.filesDir, "npu-$VERSION")

  fun isInstalled(context: Context) = File(directory(context), LIBRARY).isFile

  /**
   * Downloads the library if it isn't on the phone yet, and returns its folder. Blocks, so call
   * it off the main thread.
   */
  @Synchronized
  @Throws(IOException::class)
  fun ensure(context: Context): File {
    val dir = directory(context)
    val target = File(dir, LIBRARY)
    if (target.isFile) return dir
    dir.mkdirs()
    val part = File(dir, "$LIBRARY.part")
    val connection = URL(AAR_URL).openConnection() as HttpURLConnection
    try {
      connection.connectTimeout = TIMEOUT_MS
      connection.readTimeout = TIMEOUT_MS
      if (connection.responseCode != HttpURLConnection.HTTP_OK) {
        throw IOException("HTTP ${connection.responseCode}")
      }
      ZipInputStream(connection.inputStream).use { zip ->
        while (true) {
          val entry = zip.nextEntry ?: throw IOException("No $ENTRY in the package")
          if (entry.name != ENTRY) continue
          val digest = MessageDigest.getInstance("SHA-256")
          var total = 0L
          part.outputStream().use { output ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
              val read = zip.read(buffer)
              if (read < 0) break
              total += read
              if (total > MAX_BYTES) throw IOException("The library is too large")
              digest.update(buffer, 0, read)
              output.write(buffer, 0, read)
            }
          }
          val hash = digest.digest().joinToString("") { "%02x".format(it) }
          if (hash != SHA256) throw IOException("The library doesn't match its hash")
          break
        }
      }
    } catch (e: IOException) {
      part.delete()
      throw e
    } finally {
      connection.disconnect()
    }
    if (ModelFiles.moveIntoPlace(part, target) != null) throw IOException("Could not save $LIBRARY")
    Log.i(TAG, "Installed $LIBRARY $VERSION")
    return dir
  }
}
