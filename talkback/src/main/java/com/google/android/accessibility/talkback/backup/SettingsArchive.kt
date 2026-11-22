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

package com.google.android.accessibility.talkback.backup

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FilterOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * The .zip file of Export settings: the settings file, and the user content kept in files of their
 * own, each in the format of its own export.
 *
 * - settings.json: the settings, as [SettingsBackup.export] writes them.
 * - labels.json: custom labels, as Custom labels > Export writes them.
 * - settings-profiles.json: settings profiles, as an item file.
 * - wallpaper.png: the image of SandFly's wallpaper.
 * - scripts/ID/PATH: the files of each installed script.
 * - sound-themes/ID.zip: each installed sound theme, as Export theme writes it.
 *
 * Reading accepts only these names, within limits of size and number, and nothing outside them.
 */
object SettingsArchive {
  const val SETTINGS = "settings.json"
  const val LABELS = "labels.json"
  const val PROFILES = "settings-profiles.json"
  const val WALLPAPER = "wallpaper.png"
  private const val SCRIPTS = "scripts/"
  private const val THEMES = "sound-themes/"
  private const val THEME_SUFFIX = ".zip"

  const val MAX_ENTRIES = 5_000
  const val MAX_TOTAL_BYTES = 300L * 1024 * 1024
  const val MAX_SETTINGS_BYTES = 5L * 1024 * 1024
  const val MAX_LABELS_BYTES = 10L * 1024 * 1024
  const val MAX_PROFILES_BYTES = ItemFiles.MAX_BYTES.toLong()
  const val MAX_WALLPAPER_BYTES = 32L * 1024 * 1024
  const val MAX_SCRIPT_FILE_BYTES = 5L * 1024 * 1024
  const val MAX_SCRIPTS_BYTES = 32L * 1024 * 1024
  const val MAX_SCRIPTS = 200
  const val MAX_THEME_BYTES = 64L * 1024 * 1024
  const val MAX_THEMES = 100

  /** A script or theme ID: lowercase letters, digits, ".", "_" and "-", not starting with "." */
  private val ID = Regex("[a-z0-9][a-z0-9._-]{0,63}")

  private val PNG = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte())

  /** What Export settings writes. [themes] write each theme file, by theme ID. */
  class Contents(
    val settings: String,
    val labels: String? = null,
    val profiles: String? = null,
    val wallpaper: File? = null,
    val scripts: Map<String, Map<String, ByteArray>> = emptyMap(),
    val themes: Map<String, (OutputStream) -> Unit> = emptyMap(),
  )

  /**
   * What a settings .zip file holds. The wallpaper and the theme files are in the directory that
   * [read] was given, which the caller deletes.
   */
  class Read(
    val settings: String,
    val labels: String?,
    val profiles: String?,
    val wallpaper: File?,
    val scripts: Map<String, Map<String, ByteArray>>,
    val themes: Map<String, File>,
  )

  /** A settings .zip file that can't be read: too large, damaged, or with a name outside it. */
  class ArchiveException(message: String) : IOException(message)

  /** Whether [header], the first bytes of a file, are those of a .zip file. */
  fun isZip(header: ByteArray, length: Int = header.size): Boolean =
    length >= 4 &&
      header[0] == 'P'.code.toByte() &&
      header[1] == 'K'.code.toByte() &&
      header[2] == 3.toByte() &&
      header[3] == 4.toByte()

  @Throws(IOException::class)
  fun write(output: OutputStream, contents: Contents) {
    ZipOutputStream(output).use { zip ->
      fun put(name: String, write: (OutputStream) -> Unit) {
        zip.putNextEntry(ZipEntry(name))
        write(Unclosed(zip))
        zip.closeEntry()
      }
      put(SETTINGS) { it.write(contents.settings.toByteArray()) }
      contents.labels?.let { labels -> put(LABELS) { it.write(labels.toByteArray()) } }
      contents.profiles?.let { profiles -> put(PROFILES) { it.write(profiles.toByteArray()) } }
      contents.wallpaper?.takeIf { it.isFile }?.let { file ->
        put(WALLPAPER) { out -> file.inputStream().use { it.copyTo(out) } }
      }
      for ((id, files) in contents.scripts) {
        if (!ID.matches(id)) continue
        for ((path, bytes) in files) {
          if (!isSafePath(path)) continue
          put("$SCRIPTS$id/$path") { it.write(bytes) }
        }
      }
      for ((id, write) in contents.themes) {
        if (ID.matches(id)) put("$THEMES$id$THEME_SUFFIX", write)
      }
    }
  }

  /**
   * Reads a settings .zip file from [input], keeping the wallpaper and theme files in [directory].
   * Names outside the file, such as with "..", and files over the limits make the whole file
   * unreadable; unknown names are skipped.
   */
  @Throws(IOException::class)
  fun read(input: InputStream, directory: File): Read {
    var settings: String? = null
    var labels: String? = null
    var profiles: String? = null
    var wallpaper: File? = null
    val scripts = LinkedHashMap<String, LinkedHashMap<String, ByteArray>>()
    val themes = LinkedHashMap<String, File>()
    val names = HashSet<String>()
    var entries = 0
    var total = 0L
    var scriptBytes = 0L
    directory.mkdirs()
    try {
      ZipInputStream(input).use { zip ->
        while (true) {
          val entry = zip.nextEntry ?: break
          if (++entries > MAX_ENTRIES) throw ArchiveException("More than $MAX_ENTRIES files")
          val name = entry.name
          if (!isSafePath(name.removeSuffix("/"))) throw ArchiveException("A name outside the file: $name")
          if (entry.isDirectory) continue
          if (!names.add(name)) throw ArchiveException("Two files named $name")
          fun bytes(max: Long): ByteArray {
            val read = readAtMost(zip, max)
            total += read.size
            if (total > MAX_TOTAL_BYTES) throw ArchiveException("Larger than $MAX_TOTAL_BYTES bytes")
            return read
          }
          fun file(max: Long, target: File) {
            target.outputStream().use { out ->
              val buffer = ByteArray(64 * 1024)
              var size = 0L
              while (true) {
                val count = zip.read(buffer)
                if (count < 0) break
                size += count
                total += count
                if (size > max) throw ArchiveException("$name is larger than $max bytes")
                if (total > MAX_TOTAL_BYTES) throw ArchiveException("Larger than $MAX_TOTAL_BYTES bytes")
                out.write(buffer, 0, count)
              }
            }
          }
          when {
            name == SETTINGS -> settings = bytes(MAX_SETTINGS_BYTES).decodeToString()
            name == LABELS -> labels = bytes(MAX_LABELS_BYTES).decodeToString()
            name == PROFILES -> profiles = bytes(MAX_PROFILES_BYTES).decodeToString()
            name == WALLPAPER -> {
              val target = File(directory, WALLPAPER)
              file(MAX_WALLPAPER_BYTES, target)
              if (!startsWith(target, PNG)) throw ArchiveException("$name is not a PNG image")
              wallpaper = target
            }
            name.startsWith(SCRIPTS) -> {
              val id = name.removePrefix(SCRIPTS).substringBefore('/')
              val path = name.removePrefix("$SCRIPTS$id/")
              if (!ID.matches(id) || path == name || path.isEmpty()) {
                throw ArchiveException("Not a script file: $name")
              }
              val files = scripts.getOrPut(id) {
                if (scripts.size >= MAX_SCRIPTS) throw ArchiveException("More than $MAX_SCRIPTS scripts")
                LinkedHashMap()
              }
              val content = bytes(MAX_SCRIPT_FILE_BYTES)
              scriptBytes += content.size
              if (scriptBytes > MAX_SCRIPTS_BYTES) throw ArchiveException("Scripts larger than $MAX_SCRIPTS_BYTES bytes")
              files[path] = content
            }
            name.startsWith(THEMES) && name.endsWith(THEME_SUFFIX) -> {
              val id = name.removePrefix(THEMES).removeSuffix(THEME_SUFFIX)
              if (!ID.matches(id)) throw ArchiveException("Not a theme file: $name")
              if (themes.size >= MAX_THEMES) throw ArchiveException("More than $MAX_THEMES themes")
              val target = File(directory, "theme-${themes.size}$THEME_SUFFIX")
              file(MAX_THEME_BYTES, target)
              themes[id] = target
            }
            // Written by a later version: skipped.
            else -> {}
          }
        }
      }
    } catch (e: ZipException) {
      throw ArchiveException("Damaged: ${e.message}")
    } catch (e: IllegalArgumentException) {
      // ZipInputStream throws it for a name that isn't UTF-8.
      throw ArchiveException("Damaged: ${e.message}")
    }
    return Read(
      settings ?: throw ArchiveException("No $SETTINGS"),
      labels,
      profiles,
      wallpaper,
      scripts,
      themes,
    )
  }

  /** Whether [path] is a relative path of names, without ".", ".." or "\". */
  fun isSafePath(path: String): Boolean =
    path.isNotEmpty() &&
      '\\' !in path &&
      ':' !in path &&
      '\u0000' !in path &&
      !path.startsWith("/") &&
      path.split('/').none { it.isEmpty() || it == "." || it == ".." }

  /** Reads at most [max] bytes of the entry, and fails when it has more. */
  private fun readAtMost(input: InputStream, max: Long): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    while (true) {
      val count = input.read(buffer)
      if (count < 0) break
      if (out.size() + count > max) throw ArchiveException("A file is larger than $max bytes")
      out.write(buffer, 0, count)
    }
    return out.toByteArray()
  }

  private fun startsWith(file: File, header: ByteArray): Boolean {
    val start = ByteArray(header.size)
    val read = file.inputStream().use { it.read(start) }
    return read == header.size && start.contentEquals(header)
  }

  /** Writes to an entry without closing the .zip file when the writer closes its stream. */
  private class Unclosed(output: OutputStream) : FilterOutputStream(output) {
    override fun write(buffer: ByteArray, offset: Int, length: Int) = out.write(buffer, offset, length)

    override fun close() = flush()
  }
}
