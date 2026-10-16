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

package com.google.android.accessibility.talkback.soundthemes

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

/**
 * Links to sound themes. A link can be to a theme file, or to a GitHub repository that holds a
 * theme, which is downloaded as a ZIP file of the repository. Links must be https.
 */
object ThemeLinks {
  private const val CONNECT_TIMEOUT_MS = 15_000
  private const val READ_TIMEOUT_MS = 30_000
  private const val MAX_BYTES = 60L * 1024 * 1024
  private const val MAX_REDIRECTS = 5

  private val LINK = Regex("https://\\S+")

  /** A link to download, and a name for the theme if the theme file has none. */
  data class Download(val url: String, val name: String)

  /** Returns the first https link in [text], such as text shared from a browser, or null. */
  @JvmStatic fun findLink(text: String?): String? = text?.let { LINK.find(it)?.value }

  /**
   * Returns what to download for a link, or null if it is not an https link. A link to a GitHub
   * repository, or to a branch of one, downloads the repository as a ZIP file, and a link to a
   * file on GitHub downloads the file rather than its page.
   */
  @JvmStatic
  fun download(link: String): Download? {
    val text = link.trim().let { if (it.contains("://")) it else "https://$it" }
    val uri =
      try {
        URI(text)
      } catch (e: URISyntaxException) {
        return null
      }
    if (!"https".equals(uri.scheme, ignoreCase = true) || uri.host.isNullOrEmpty()) return null
    val segments = uri.path.orEmpty().split('/').filter { it.isNotEmpty() }
    val host = uri.host.lowercase()
    if (host == "github.com" || host == "www.github.com") {
      if (segments.size >= 2) {
        val owner = segments[0]
        val repo = segments[1].removeSuffix(".git")
        val base = "https://github.com/$owner/$repo"
        when {
          segments.size == 2 -> return Download("$base/archive/HEAD.zip", repo)
          segments[2] == "tree" && segments.size >= 4 ->
            return Download("$base/archive/${segments[3]}.zip", repo)
          segments[2] == "blob" && segments.size >= 5 ->
            return Download(
              "https://raw.githubusercontent.com/$owner/$repo/${segments.drop(3).joinToString("/")}",
              segments.last().substringBeforeLast('.'),
            )
        }
      }
    }
    val name = segments.lastOrNull()?.substringBeforeLast('.')?.ifEmpty { null } ?: host
    return Download(uri.toASCIIString(), name)
  }

  /**
   * Copies [input] into [file], up to [maxBytes], which is 60 MB, the most a theme can be, unless
   * given. Throws if there is more, so that a stream that never ends cannot fill the storage.
   */
  @JvmStatic
  @JvmOverloads
  @Throws(IOException::class)
  fun copyLimited(input: InputStream, file: File, maxBytes: Long = MAX_BYTES) {
    file.outputStream().use { output ->
      val buffer = ByteArray(64 * 1024)
      var total = 0L
      while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        total += count
        if (total > maxBytes) throw IOException(tooLarge(maxBytes))
        output.write(buffer, 0, count)
      }
    }
  }

  private fun tooLarge(maxBytes: Long) = "larger than ${maxBytes / (1024 * 1024)} MB"

  /**
   * Downloads [url] into [file], following redirects as long as they stay on https, up to
   * [maxBytes].
   */
  @JvmStatic
  @JvmOverloads
  @Throws(IOException::class)
  fun fetch(url: String, file: File, maxBytes: Long = MAX_BYTES) {
    var current = URL(url)
    repeat(MAX_REDIRECTS) {
      if (current.protocol != "https") throw IOException("only https links can be used")
      val connection = current.openConnection() as HttpURLConnection
      try {
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.instanceFollowRedirects = false
        val code = connection.responseCode
        if (code in 300..399) {
          val location = connection.getHeaderField("Location") ?: throw IOException("HTTP $code")
          current = URL(current, location)
          return@repeat
        }
        if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code")
        if (connection.contentLengthLong > maxBytes) throw IOException(tooLarge(maxBytes))
        connection.inputStream.use { copyLimited(it, file, maxBytes) }
        return
      } finally {
        connection.disconnect()
      }
    }
    throw IOException("too many redirects")
  }
}
