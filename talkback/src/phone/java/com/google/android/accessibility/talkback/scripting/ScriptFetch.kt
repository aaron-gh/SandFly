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

package com.google.android.accessibility.talkback.scripting

import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.Charset
import java.util.concurrent.Executors
import java.util.concurrent.Future
import org.json.JSONObject

object ScriptFetch {
  private const val MAX_RESPONSE_BYTES = 5 * 1024 * 1024
  private const val MAX_BODY_CHARS = 1024 * 1024
  private const val DEFAULT_TIMEOUT_MS = 30_000
  private const val MAX_TIMEOUT_MS = 60_000
  private val METHODS = setOf("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")

  private val executor =
    Executors.newFixedThreadPool(4) { Thread(it, "BacktalkScriptFetch").apply { isDaemon = true } }

  class Request(
    val url: URL,
    val method: String,
    val headers: Map<String, String>,
    val body: ByteArray?,
    val timeoutMs: Int,
  )

  fun request(args: JSONObject): Request {
    val text = args.optString("url")
    val url =
      try {
        URL(text)
      } catch (e: MalformedURLException) {
        apiError("Not a URL: $text")
      }
    // Android blocks cleartext requests from Backtalk, and what a script sends may be on screen.
    apiCheck(url.protocol == "https") { "fetch only takes https URLs: $text" }
    val method = args.optString("method", "GET").uppercase()
    apiCheck(method in METHODS) { "Unknown method $method. Methods: ${METHODS.joinToString()}" }
    val headers =
      args.optJSONObject("headers")?.let { obj ->
        obj.keys().asSequence().associateWith { obj.get(it).toString() }
      }.orEmpty()
    val body = args.optString("body").takeUnless { args.isNull("body") }
    apiCheck((body?.length ?: 0) <= MAX_BODY_CHARS) { "A request body is at most 1 MB" }
    val timeout = args.optInt("timeout", DEFAULT_TIMEOUT_MS).coerceIn(1, MAX_TIMEOUT_MS)
    return Request(url, method, headers, body?.toByteArray(), timeout)
  }

  fun start(request: Request, done: (Boolean, Any?) -> Unit): Future<*> =
    executor.submit {
      runCatching {
          (request.url.openConnection() as HttpURLConnection).run {
            try {
              send(request)
            } finally {
              disconnect()
            }
          }
        }
        .fold(
          { done(true, it) },
          { done(false, "Network error: ${it.message ?: it.javaClass.simpleName}") },
        )
    }

  private fun HttpURLConnection.send(request: Request): JSONObject {
    requestMethod = request.method
    connectTimeout = request.timeoutMs
    readTimeout = request.timeoutMs
    useCaches = false
    request.headers.forEach { (key, value) -> setRequestProperty(key, value) }
    request.body?.let { body ->
      doOutput = true
      outputStream.use { it.write(body) }
    }
    val status = responseCode
    val stream = if (status >= 400) errorStream else inputStream
    val bytes = stream?.use { it.readAtMost(MAX_RESPONSE_BYTES + 1) } ?: ByteArray(0)
    if (bytes.size > MAX_RESPONSE_BYTES) throw IOException("The response is larger than 5 MB")
    val headers =
      headerFields.entries
        .filter { it.key != null }
        .associate { (key, values) -> key.lowercase() to values.joinToString(", ") }
    return jsonObject(
      "status" to status,
      "statusText" to responseMessage.orEmpty(),
      "url" to url.toString(),
      "headers" to JSONObject(headers),
      "body" to String(bytes, charsetOf(contentType)),
    )
  }

  private fun charsetOf(contentType: String?): Charset =
    contentType
      ?.split(';')
      ?.map { it.trim() }
      ?.firstOrNull { it.startsWith("charset=", ignoreCase = true) }
      ?.substringAfter('=')
      ?.trim('"', ' ')
      ?.let { runCatching { Charset.forName(it) }.getOrNull() }
      ?: Charsets.UTF_8
}
