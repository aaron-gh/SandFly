/*
 * Copyright 2026 Backtalk contributors
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

package com.google.android.accessibility.talkback.update

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection

/**
 * Downloads an update APK into [part], resuming what an earlier attempt left there.
 *
 * Next to the part, an info file holds what the part is a download of (the address and the
 * release's asset size) and the server's validator (ETag or Last-Modified). A part of another
 * download is deleted before the first attempt. A resumed request sends the validator in
 * `If-Range`, so a server whose file changed answers with all of it.
 */
internal class ResumableDownload(
  private val part: File,
  /** The address and release asset size; a part of a different download is not resumed. */
  private val identity: String,
  /** The release's asset size, or 0 or less when unknown. */
  private val expectedSize: Long,
  private val connect: (String) -> HttpURLConnection,
  /** Gets the bytes in the part so far and the whole size, or -1 when unknown. */
  private val onProgress: (Long, Long) -> Unit = { _, _ -> },
) {
  private val info = File(part.path + INFO_SUFFIX)

  init {
    if (readInfo()?.getOrNull(0) != identity) {
      discard()
    }
  }

  /** Deletes the part and its info. */
  fun discard() {
    part.delete()
    info.delete()
  }

  /**
   * Requests the rest of the file from [url] and appends it to the part. Returns when the part has
   * the whole file.
   *
   * @throws PermanentDownloadException for an answer that another attempt would not change.
   * @throws IOException when the connection fails or the file is incomplete; the part keeps what
   *   arrived, for the next attempt.
   */
  @Throws(IOException::class)
  fun attempt(url: String, isApi: Boolean) {
    var have = if (part.isFile) part.length() else 0L
    if (expectedSize > 0 && have > expectedSize) {
      discard()
      have = 0L
    }
    if (expectedSize > 0 && have == expectedSize) {
      return
    }
    val connection = connect(url)
    try {
      // Without this type, the GitHub API returns the asset's JSON description instead of the APK.
      if (isApi) connection.setRequestProperty("Accept", "application/octet-stream")
      // A range of a compressed response would count compressed bytes.
      connection.setRequestProperty("Accept-Encoding", "identity")
      if (have > 0) {
        connection.setRequestProperty("Range", "bytes=$have-")
        readInfo()?.getOrNull(1)?.takeIf { it.isNotEmpty() }?.let {
          connection.setRequestProperty("If-Range", it)
        }
      }
      val code = connection.responseCode
      val total: Long
      val append: Boolean
      when (code) {
        HttpURLConnection.HTTP_PARTIAL -> {
          val range = parseContentRange(connection.getHeaderField("Content-Range"))
          if (range == null || range.first != have) {
            discard()
            throw IOException("Range answer ${connection.getHeaderField("Content-Range")} for $have")
          }
          total = if (range.second > 0) range.second else expectedSize
          if (expectedSize > 0 && total != expectedSize) {
            // The file at the address was replaced by another build.
            discard()
            throw IOException("Range answer has $total bytes, not $expectedSize")
          }
          append = true
        }
        HttpURLConnection.HTTP_OK -> {
          have = 0L
          val length = connection.contentLengthLong
          total = if (length > 0) length else expectedSize
          writeInfo(validator(connection))
          append = false
        }
        HTTP_RANGE_NOT_SATISFIABLE -> {
          discard()
          throw IOException("HTTP 416 for bytes $have-")
        }
        else -> {
          val message = "Download returned HTTP $code"
          // An API failure such as the rate limit leaves the direct address to try.
          throw if (isApi || DownloadRetry.isRetryableStatus(code)) {
            IOException(message)
          } else {
            PermanentDownloadException(message)
          }
        }
      }
      if (!append) {
        part.parentFile?.mkdirs()
      }
      onProgress(have, total)
      connection.inputStream.use { input ->
        FileOutputStream(part, append).use { output ->
          val buffer = ByteArray(BUFFER_SIZE)
          while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            output.write(buffer, 0, count)
            have += count
            onProgress(have, total)
          }
        }
      }
      if (total > 0 && have > total) {
        discard()
      }
      DownloadRetry.checkSize(total, have)
    } finally {
      connection.disconnect()
    }
  }

  /** Moves the complete part to [target], and deletes the info. */
  @Throws(IOException::class)
  fun finish(target: File) {
    target.delete()
    if (!part.renameTo(target)) {
      throw IOException("Cannot rename ${part.name}")
    }
    info.delete()
  }

  private fun readInfo(): List<String>? =
    try {
      if (info.isFile && info.length() <= MAX_INFO_BYTES) info.readText().split('\n') else null
    } catch (e: IOException) {
      null
    }

  private fun writeInfo(validator: String) {
    try {
      info.parentFile?.mkdirs()
      info.writeText(identity + "\n" + validator)
    } catch (e: IOException) {
      // Without the info, the next download starts again from the beginning.
    }
  }

  companion object {
    const val INFO_SUFFIX = ".info"
    private const val HTTP_RANGE_NOT_SATISFIABLE = 416
    private const val BUFFER_SIZE = 64 * 1024
    private const val MAX_INFO_BYTES = 8 * 1024L
    private val CONTENT_RANGE = Regex("""bytes\s+(\d+)-(\d+)/(\d+|\*)""")

    /** What a part is a download of: the address and the asset size. */
    fun identity(url: String, size: Long): String = "$url $size".replace('\n', ' ')

    /**
     * Returns the first byte and the whole size, or -1 when the server gives none, of a
     * `Content-Range` header such as "bytes 100-199/200", or null if it is not one.
     */
    fun parseContentRange(header: String?): Pair<Long, Long>? {
      val match = header?.trim()?.let { CONTENT_RANGE.matchEntire(it) } ?: return null
      val first = match.groupValues[1].toLongOrNull() ?: return null
      val total = match.groupValues[3].let { if (it == "*") -1L else it.toLongOrNull() ?: return null }
      return first to total
    }

    /** The strong ETag or the Last-Modified date of the answer, which `If-Range` can use. */
    private fun validator(connection: HttpURLConnection): String {
      val etag = connection.getHeaderField("ETag")?.trim()
      if (etag != null && etag.startsWith("\"")) return etag
      return connection.getHeaderField("Last-Modified")?.trim().orEmpty()
    }
  }
}
