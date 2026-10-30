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

package com.google.android.accessibility.talkback.scripting

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.content.edit
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONArray
import org.json.JSONException

/**
 * Keeps installed scripts up to date from the sources they came from. A newer version is
 * installed on its own only when nothing the user agreed to changes: the same author, no new
 * permissions, made for this SandFly, and from the script's own source. Scripts installed before
 * SandFly kept their source are updated that way only from SandFly's own scripts. Any other newer
 * version is listed in Scripts, to be installed after the usual question.
 */
object ScriptUpdater {
  /** A newer version that waits for the user. */
  data class Available(val id: String, val name: String, val version: String, val origin: String)

  /** What a check found, of [checked] scripts. */
  data class Result(
    val updated: List<String>,
    val available: List<Available>,
    val failed: Int,
    val checked: Int = 0,
  )

  private const val TAG = "ScriptUpdater"
  private const val PREFS = "backtalk_script_updates"
  private const val KEY_AUTO = "auto"
  private const val KEY_LAST_CHECK = "last_check"
  private const val KEY_AVAILABLE = "available"
  private const val CHECK_EVERY_MS = 24 * 60 * 60 * 1000L
  /** How soon a check that reached no source is tried again, as when the phone was offline. */
  private const val RETRY_MS = 60 * 60 * 1000L
  /** The most scripts downloaded in one check, so that a check stays small. */
  private const val MAX_DOWNLOADS = 50
  private const val SEPARATOR = "\n"

  private val executor = Executors.newSingleThreadExecutor()
  private val main by lazy { Handler(Looper.getMainLooper()) }
  private val checking = AtomicBoolean(false)

  /** Where a script from [file] in [source] came from, as kept with the installed script. */
  fun originOf(source: ScriptSource, file: String): String = source.address + SEPARATOR + file

  fun isAutomatic(context: Context): Boolean = prefs(context).getBoolean(KEY_AUTO, true)

  fun setAutomatic(context: Context, on: Boolean) = prefs(context).edit { putBoolean(KEY_AUTO, on) }

  /** The newer versions found that wait for the user, without those since installed. */
  fun available(context: Context): List<Available> {
    val store = ScriptStore.get(context)
    return readAvailable(context).filter { found ->
      val installed = store.find(found.id) ?: return@filter false
      compareVersions(found.version, installed.manifest.version) > 0
    }
  }

  /** Checks when updates are automatic and the last check is a day old. */
  @JvmStatic
  fun checkIfDue(context: Context) {
    val app = context.applicationContext
    if (!isAutomatic(app)) return
    val last = prefs(app).getLong(KEY_LAST_CHECK, 0L)
    val now = System.currentTimeMillis()
    if (now - last in 0 until CHECK_EVERY_MS) return
    check(app, install = true, done = null)
  }

  /**
   * Checks every installed script now, installing what can be installed on its own when
   * [install], and calls [done] on the main thread. Does nothing and returns false when a check is
   * running.
   */
  fun check(context: Context, install: Boolean, done: ((Result) -> Unit)?): Boolean {
    val app = context.applicationContext
    if (!checking.compareAndSet(false, true)) return false
    executor.execute {
      val result =
        try {
          run(app, install)
        } catch (e: Throwable) {
          LogUtils.e(TAG, "Checking for script updates failed: %s", e)
          Result(emptyList(), emptyList(), 1)
        } finally {
          checking.set(false)
        }
      // A check with nothing to check doesn't count, so that a script installed next is checked.
      if (result.checked > 0) {
        val reachedNone = result.failed > 0 && result.updated.isEmpty() && result.available.isEmpty()
        val checkedAt = System.currentTimeMillis() - if (reachedNone) CHECK_EVERY_MS - RETRY_MS else 0L
        prefs(app).edit { putLong(KEY_LAST_CHECK, checkedAt) }
      }
      if (done != null) main.post { done(result) }
    }
    return true
  }

  private fun run(context: Context, install: Boolean): Result {
    val store = ScriptStore.get(context)
    val installed = store.all().filter { it.id != ScriptStore.EXAMPLE_ID }
    if (installed.isEmpty()) return Result(emptyList(), emptyList(), 0)
    val sources = ScriptSources.all(context)
    val official = ScriptSource.parse(ScriptSources.OFFICIAL_SCRIPTS)?.address
    val listings = mutableMapOf<String, List<SourceScript>?>()
    var failed = 0
    fun listing(source: ScriptSource): List<SourceScript>? =
      listings.getOrPut(source.address) {
        try {
          ScriptSources.list(source)
        } catch (e: IOException) {
          LogUtils.w(TAG, "Could not list %s: %s", source.address, e.message)
          failed++
          null
        }
      }
    val updated = mutableListOf<String>()
    val available = mutableListOf<Available>()
    // Scripts this check couldn't reach keep the updates an earlier check found.
    val unchecked = mutableSetOf<String>()
    var downloads = 0
    for (script in installed) {
      if (downloads >= MAX_DOWNLOADS) {
        unchecked += script.id
        continue
      }
      val failedBefore = failed
      // Its own source, or, for a script installed before sources were kept, a file named after
      // its id in any source.
      val candidates =
        if (script.origin.isNotEmpty()) {
          val address = script.origin.substringBefore(SEPARATOR)
          val file = script.origin.substringAfter(SEPARATOR)
          sources.filter { it.address == address }.mapNotNull { source ->
            listing(source)?.firstOrNull { it.file == file }?.let { source to it }
          }
        } else {
          sources.mapNotNull { source ->
            listing(source)
              ?.firstOrNull { it.file.substringBeforeLast('.').equals(script.id, ignoreCase = true) }
              ?.let { source to it }
          }
        }
      for ((source, file) in candidates) {
        if (downloads++ >= MAX_DOWNLOADS) break
        val pkg =
          try {
            ScriptPackage.read(ScriptSources.download(file))
          } catch (e: IOException) {
            failed++
            unchecked += script.id
            continue
          } catch (e: IllegalArgumentException) {
            // Not a script SandFly can read, such as one with a broken manifest.
            continue
          }
        val manifest = pkg.manifest
        if (manifest.id != script.id) continue
        if (compareVersions(manifest.version, script.manifest.version) <= 0) break
        val origin = originOf(source, file.file)
        val ownSource = script.origin == origin || (script.origin.isEmpty() && source.address == official)
        // SandFly's own scripts update on their own only when SandFly signed the new version, so
        // that someone who can change that repository can't change the installed scripts.
        val safe =
          ownSource &&
            (source.address != official || pkg.signature == ScriptSignature.Status.SIGNED) &&
            pkg.signature != ScriptSignature.Status.CHANGED &&
            manifest.author == script.manifest.author &&
            (manifest.permissions - script.manifest.permissions).isEmpty() &&
            manifest.compatibility == ApiCompatibility.COMPATIBLE
        if (install && safe) {
          try {
            // Not when the script was removed during the check.
            if (store.update(pkg, origin)) {
              ScriptLog.add(script.id, ScriptLog.Level.INFO, "Updated to ${manifest.version}")
              updated += manifest.name
            }
          } catch (e: IOException) {
            failed++
          }
        } else {
          available += Available(script.id, manifest.name, manifest.version, origin)
        }
        break
      }
      if (failed > failedBefore) unchecked += script.id
    }
    saveAvailable(
      context,
      available + readAvailable(context).filter { it.id in unchecked && available.none { a -> a.id == it.id } },
    )
    return Result(updated, available, failed, installed.size)
  }

  /**
   * Compares versions such as "3.10" and "3.9" by their numbers, part by part. Versions with no
   * numbers count as equal.
   */
  fun compareVersions(a: String, b: String): Int {
    val x = numbers(a)
    val y = numbers(b)
    for (i in 0 until maxOf(x.size, y.size)) {
      val c = (x.getOrNull(i) ?: 0L).compareTo(y.getOrNull(i) ?: 0L)
      if (c != 0) return c
    }
    return 0
  }

  private fun numbers(version: String): List<Long> =
    NUMBER.findAll(version).take(MAX_PARTS).mapNotNull { it.value.take(18).toLongOrNull() }.toList()

  private val NUMBER = Regex("\\d+")
  private const val MAX_PARTS = 8

  private fun readAvailable(context: Context): List<Available> =
    try {
      JSONArray(prefs(context).getString(KEY_AVAILABLE, null) ?: "[]").let { array ->
        (0 until array.length()).mapNotNull { i ->
          array.optJSONObject(i)?.let {
            Available(it.optString("id"), it.optString("name"), it.optString("version"), it.optString("origin"))
          }
        }
      }
    } catch (e: JSONException) {
      emptyList()
    }

  private fun saveAvailable(context: Context, available: List<Available>) =
    prefs(context).edit {
      putString(
        KEY_AVAILABLE,
        JSONArray(
            available.map {
              jsonObject("id" to it.id, "name" to it.name, "version" to it.version, "origin" to it.origin)
            }
          )
          .toString(),
      )
    }

  /** The name of the source [available] comes from. */
  fun sourceTitle(context: Context, available: Available): String {
    val address = available.origin.substringBefore(SEPARATOR)
    return (ScriptSources.all(context).firstOrNull { it.address == address }
        ?: ScriptSource.parse(address))
      ?.title ?: address
  }

  /** Downloads the version in [available], to be installed after the usual question. */
  @Throws(IOException::class)
  fun download(context: Context, available: Available): ByteArray {
    val address = available.origin.substringBefore(SEPARATOR)
    val file = available.origin.substringAfter(SEPARATOR)
    val source =
      ScriptSources.all(context).firstOrNull { it.address == address }
        ?: ScriptSource.parse(address)
        ?: throw IOException("The source is gone")
    val script =
      ScriptSources.list(source).firstOrNull { it.file == file } ?: throw IOException("$file is gone")
    return ScriptSources.download(script)
  }

  private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
