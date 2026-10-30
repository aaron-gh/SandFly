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

package com.google.android.accessibility.braille.translate.japanese

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.CopyOnWriteArrayList
import java.util.zip.ZipInputStream

/**
 * The JTalk extended dictionary that Japanese braille needs to read kanji and split words: the
 * pinned release of nishimotz/libkuraji-jtalk-dic, downloaded once (15 MB, 67 MB unpacked) into
 * device-protected storage, so that it works before the phone is first unlocked.
 */
object JapaneseDictionary {
  enum class State {
    MISSING,
    DOWNLOADING,
    FAILED,
    READY,
  }

  private const val TAG = "JapaneseDictionary"
  private const val TAG_NAME = "v1.2.1"
  private const val URL_STRING =
    "https://github.com/nishimotz/libkuraji-jtalk-dic/releases/download/$TAG_NAME/jtalk-dic-$TAG_NAME.zip"
  private const val SHA256 = "3c13f45a956a14e404dc473052ae68259a3b936143e3127bf4be6f1df5c5b67a"
  /** The files MeCab needs; dicrc and DIC_VERSION aren't read. */
  private val FILES = setOf("sys.dic", "unk.dic", "matrix.bin", "char.bin")
  /** Larger than the release's unpacked files, so a damaged archive can't fill the storage. */
  private const val MAX_FILE_BYTES = 100L * 1024 * 1024
  private const val TIMEOUT_MS = 30_000
  private const val RETRY_MS = 10 * 60_000L

  private val main = Handler(Looper.getMainLooper())
  private val listeners = CopyOnWriteArrayList<Runnable>()
  @Volatile private var state: State? = null
  @Volatile private var analyzer: JapaneseAnalyzer? = null
  @Volatile private var failedAt = 0L

  /** Where the unpacked dictionary is. */
  private fun directory(context: Context): File =
    File(context.createDeviceProtectedStorageContext().noBackupFilesDir, "japanese-braille/$TAG_NAME")

  @JvmStatic
  fun state(context: Context): State =
    state ?: (if (isComplete(directory(context))) State.READY else State.MISSING).also { state = it }

  private fun isComplete(directory: File): Boolean = FILES.all { File(directory, it).isFile }

  /** Runs [listener] on the main thread whenever the state changes. */
  @JvmStatic
  fun addListener(listener: Runnable) {
    listeners.add(listener)
  }

  @JvmStatic
  fun removeListener(listener: Runnable) {
    listeners.remove(listener)
  }

  private fun setState(new: State) {
    state = new
    main.post { listeners.forEach(Runnable::run) }
  }

  /** The analyzer over the downloaded dictionary, or null while it isn't downloaded or can't be read. */
  @Synchronized
  internal fun analyzer(context: Context): JapaneseAnalyzer? {
    analyzer?.let { return it }
    if (state(context) != State.READY) return null
    return try {
      JapaneseAnalyzer(directory(context)).also { analyzer = it }
    } catch (e: Exception) {
      // A truncated file throws IndexOutOfBoundsException from its ByteBuffer rather than
      // IOException, on the main thread.
      LogUtils.e(TAG, "Dictionary unreadable, downloading it again: %s", e)
      directory(context).deleteRecursively()
      setState(State.MISSING)
      null
    }
  }

  /**
   * Downloads the dictionary in the background, unless it is downloaded or downloading. After a
   * failed download, tries again only after [RETRY_MS] unless [retryNow].
   */
  @JvmStatic
  @Synchronized
  fun download(context: Context, retryNow: Boolean) {
    val current = state(context)
    if (current == State.READY || current == State.DOWNLOADING) return
    if (
      current == State.FAILED && !retryNow && SystemClock.elapsedRealtime() - failedAt < RETRY_MS
    ) {
      return
    }
    setState(State.DOWNLOADING)
    val app = context.applicationContext
    Thread(
        {
          val ok =
            try {
              fetch(directory(app))
              true
            } catch (e: IOException) {
              LogUtils.e(TAG, "Download failed: %s", e)
              false
            } catch (e: SecurityException) {
              LogUtils.e(TAG, "Download failed: %s", e)
              false
            }
          if (!ok) failedAt = SystemClock.elapsedRealtime()
          setState(if (ok) State.READY else State.FAILED)
        },
        "japanese-braille-dictionary",
      )
      .apply { isDaemon = true }
      .start()
  }

  /** Downloads and checks the archive, then unpacks the dictionary files into [target]. */
  private fun fetch(target: File) {
    val parent = target.parentFile ?: throw IOException("No storage")
    // An older dictionary or an unfinished download is replaced.
    parent.deleteRecursively()
    if (!parent.mkdirs()) throw IOException("Can't create $parent")
    val archive = File(parent, "download.zip")
    val connection = URL(URL_STRING).openConnection() as HttpURLConnection
    connection.connectTimeout = TIMEOUT_MS
    connection.readTimeout = TIMEOUT_MS
    try {
      if (connection.responseCode != HttpURLConnection.HTTP_OK) {
        throw IOException("HTTP ${connection.responseCode}")
      }
      val digest = MessageDigest.getInstance("SHA-256")
      connection.inputStream.use { input ->
        archive.outputStream().use { output ->
          val buffer = ByteArray(64 * 1024)
          var total = 0L
          while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_FILE_BYTES) throw IOException("Archive too large")
            digest.update(buffer, 0, read)
            output.write(buffer, 0, read)
          }
        }
      }
      val hash = digest.digest().joinToString("") { "%02x".format(it) }
      if (hash != SHA256) throw IOException("Archive checksum $hash")
    } finally {
      connection.disconnect()
    }
    val unpacking = File(parent, "unpacking")
    unpacking.mkdirs()
    ZipInputStream(archive.inputStream().buffered()).use { zip ->
      while (true) {
        val entry = zip.nextEntry ?: break
        // Only the known files, by name, so no entry can write elsewhere.
        val name = entry.name.substringAfterLast('/')
        if (entry.isDirectory || name !in FILES) continue
        File(unpacking, name).outputStream().use { output ->
          val buffer = ByteArray(64 * 1024)
          var total = 0L
          while (true) {
            val read = zip.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_FILE_BYTES) throw IOException("$name too large")
            output.write(buffer, 0, read)
          }
        }
      }
    }
    archive.delete()
    if (!isComplete(unpacking)) throw IOException("Archive lacks dictionary files")
    // Checks that the files can be read before they are used.
    JapaneseAnalyzer(unpacking)
    if (!unpacking.renameTo(target)) throw IOException("Can't move the dictionary into place")
  }
}
