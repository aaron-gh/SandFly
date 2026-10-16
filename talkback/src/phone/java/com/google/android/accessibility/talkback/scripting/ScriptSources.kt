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

import android.content.Context
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * A script file in a source. [file] is its file name, [name] what the list shows: the file name
 * without its extension, unless that is empty or names two files.
 */
data class SourceScript(val file: String, val url: String, val name: String = file)

/** One page of a source's files. [count] is how many entries the page held. */
data class SourcePage(val scripts: List<SourceScript>, val count: Int, val next: String?)

object ScriptSources {
  private const val PREFS = "script_sources"
  private const val KEY_SOURCES = "sources"
  private const val CONNECT_TIMEOUT_MS = 15_000
  private const val READ_TIMEOUT_MS = 30_000
  private const val MAX_REDIRECTS = 5
  private const val MAX_LISTING_BYTES = 8 * 1024 * 1024
  private const val MAX_PAGES = 5
  private const val MAX_SCRIPTS = 200
  private val EXTENSIONS = listOf(".js", ".zip")

  @Synchronized
  fun all(context: Context): List<ScriptSource> {
    val saved =
      context
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY_SOURCES, null) ?: return emptyList()
    return try {
      val array = JSONArray(saved)
      (0 until array.length()).mapNotNull { ScriptSource.parse(array.optString(it)) }
    } catch (e: JSONException) {
      emptyList()
    }
  }

  /** Adds [source] to the end of the list. Returns false if it is already there. */
  @Synchronized
  fun add(context: Context, source: ScriptSource): Boolean {
    val sources = all(context)
    if (sources.any { it.address.equals(source.address, ignoreCase = true) }) return false
    save(context, sources + source)
    return true
  }

  @Synchronized
  fun remove(context: Context, source: ScriptSource) {
    save(context, all(context).filterNot { it.address == source.address })
  }

  private fun save(context: Context, sources: List<ScriptSource>) {
    context
      .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
      .edit()
      .putString(KEY_SOURCES, JSONArray(sources.map { it.address }).toString())
      .apply()
  }

  /** The scripts in [source], by file name. Throws [IOException] with the reason it failed. */
  @Throws(IOException::class)
  fun list(source: ScriptSource): List<SourceScript> {
    val scripts = mutableListOf<SourceScript>()
    var url: String? = source.listUrl
    var pages = 0
    while (url != null && pages < MAX_PAGES && scripts.size < MAX_SCRIPTS) {
      val body =
        fetch(url, MAX_LISTING_BYTES, "application/json") { connection, code ->
          listingError(source.host, connection, code)
        }
      val page =
        try {
          parsePage(source, body.decodeToString())
        } catch (e: JSONException) {
          throw IOException("The answer of ${source.host.label} could not be read")
        }
      scripts += page.scripts
      pages++
      url =
        page.next
          ?: if (source.host.api == SourceApi.GITLAB && page.count >= ScriptSource.GITLAB_PAGE) {
            source.listUrl + "&page=${pages + 1}"
          } else {
            null
          }
    }
    return named(scripts)
  }

  /** Throws [JSONException] if [json] is not a list of files in a folder of [source]. */
  fun parsePage(source: ScriptSource, json: String): SourcePage {
    val host = source.host
    if (host.api == SourceApi.BITBUCKET) {
      val answer = JSONObject(json)
      val values = answer.getJSONArray("values")
      val next = answer.optString("next").takeIf { it.startsWith(host.filesPrefix) }
      return SourcePage(scriptsIn(values) { bitbucketScript(host, it) }, values.length(), next)
    }
    val entries = JSONArray(json)
    val scripts =
      scriptsIn(entries) {
        if (host.api == SourceApi.GITLAB) gitlabScript(source, it) else gitScript(host, it)
      }
    return SourcePage(scripts, entries.length(), null)
  }

  private fun scriptsIn(entries: JSONArray, read: (JSONObject) -> SourceScript?) =
    (0 until entries.length()).mapNotNull { entries.optJSONObject(it)?.let(read) }

  // GitHub, and Gitea or Forgejo, which answer alike.
  private fun gitScript(host: SourceHost, entry: JSONObject): SourceScript? {
    if (entry.optString("type") != "file") return null
    val file = entry.optString("name")
    return scriptOf(host, file, entry.optString("download_url"), entry.optLong("size"))
  }

  // GitLab lists no sizes or addresses of files, so a file's address is made, and its size is
  // limited as it is downloaded.
  private fun gitlabScript(source: ScriptSource, entry: JSONObject): SourceScript? {
    if (entry.optString("type") != "blob") return null
    val file = entry.optString("name")
    val path = entry.optString("path")
    val ref = source.ref?.let { "?ref=" + ScriptSource.encode(it) }.orEmpty()
    val url =
      "${source.host.filesPrefix}${source.projectId}/repository/files/" +
        ScriptSource.encode(path) +
        "/raw" +
        ref
    return scriptOf(source.host, file, url, 0)
  }

  private fun bitbucketScript(host: SourceHost, entry: JSONObject): SourceScript? {
    if (entry.optString("type") != "commit_file") return null
    val file = entry.optString("path").substringAfterLast('/')
    val url = entry.optJSONObject("links")?.optJSONObject("self")?.optString("href").orEmpty()
    return scriptOf(host, file, url, entry.optLong("size"))
  }

  private fun scriptOf(host: SourceHost, file: String, url: String, size: Long): SourceScript? {
    if (EXTENSIONS.none { file.endsWith(it, ignoreCase = true) }) return null
    // Only from where the service serves its files, and no more than a script can be.
    if (!url.startsWith(host.filesPrefix) || size > ScriptPackage.maxBytesOf(file)) return null
    return SourceScript(file, url)
  }

  /** Sorts [scripts], keeps the first 200, and gives each its name for the list. */
  fun named(scripts: List<SourceScript>): List<SourceScript> {
    val files = scripts.sortedBy { it.file.lowercase() }.take(MAX_SCRIPTS)
    val shortNames = files.groupingBy { it.file.substringBeforeLast('.') }.eachCount()
    return files.map {
      val short = it.file.substringBeforeLast('.')
      if (short.isEmpty() || shortNames.getValue(short) > 1) it else it.copy(name = short)
    }
  }

  /** The bytes of a script from a source, at most [ScriptPackage.maxBytesOf] its file name. */
  @Throws(IOException::class)
  fun download(script: SourceScript): ByteArray =
    fetch(script.url, ScriptPackage.maxBytesOf(script.file), "*/*")

  private fun listingError(
    host: SourceHost,
    connection: HttpURLConnection,
    code: Int,
  ): IOException =
    when {
      code == HttpURLConnection.HTTP_NOT_FOUND ->
        IOException("The repository or folder was not found")
      code == 429 ||
        code == HttpURLConnection.HTTP_FORBIDDEN &&
          connection.getHeaderField("X-RateLimit-Remaining") == "0" ->
        IOException("${host.label}'s limit on requests was reached. Try again later")
      else -> IOException("HTTP $code")
    }

  /** Reads [url], following redirects as long as they stay on https. */
  private fun fetch(
    url: String,
    limit: Int,
    accept: String,
    error: (HttpURLConnection, Int) -> IOException = { _, code -> IOException("HTTP $code") },
  ): ByteArray {
    var current = URL(url)
    repeat(MAX_REDIRECTS) {
      if (current.protocol != "https") throw IOException("Only https addresses can be used")
      val connection = current.openConnection() as HttpURLConnection
      try {
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", accept)
        connection.setRequestProperty("User-Agent", "Backtalk")
        val code = connection.responseCode
        if (code in 300..399) {
          val location = connection.getHeaderField("Location") ?: throw IOException("HTTP $code")
          current = URL(current, location)
          return@repeat
        }
        if (code != HttpURLConnection.HTTP_OK) throw error(connection, code)
        val bytes = connection.inputStream.use { it.readAtMost(limit + 1) }
        if (bytes.size > limit) throw IOException("The response is too large")
        return bytes
      } finally {
        connection.disconnect()
      }
    }
    throw IOException("Too many redirects")
  }
}
