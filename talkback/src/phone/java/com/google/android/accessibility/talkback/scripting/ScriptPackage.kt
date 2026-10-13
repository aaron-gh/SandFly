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

import com.google.android.accessibility.scripting.quickjs.QuickJs
import com.google.android.accessibility.scripting.quickjs.QuickJsException
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

class ScriptPackage(val files: Map<String, ByteArray>, val manifest: ScriptManifest) {

  companion object {
    const val MAIN = "main.js"
    /** The most a .zip file can be, and the most its files can be together once unpacked. */
    const val MAX_BYTES = 5 * 1024 * 1024

    /** The most a .js file can be. */
    const val MAX_JS_BYTES = 1024 * 1024
    private const val MAX_FILES = 200

    fun read(bytes: ByteArray): ScriptPackage {
      val zip = isZip(bytes)
      manifestCheck(bytes.size <= if (zip) MAX_BYTES else MAX_JS_BYTES) {
        if (zip) "The zip is larger than 5 MB" else "The script is larger than 1 MB"
      }
      val files = if (zip) unzip(bytes) else mapOf(MAIN to bytes)
      val main = files[MAIN] ?: manifestError("The zip has no main.js")
      return ScriptPackage(files, ScriptManifest.parse(ManifestReader.read(main.decodeToString())))
    }

    /** The most a file can be, by its name: a .zip, or otherwise a .js. */
    fun maxBytesOf(fileName: String): Int =
      if (fileName.endsWith(".zip", ignoreCase = true)) MAX_BYTES else MAX_JS_BYTES

    private fun isZip(bytes: ByteArray): Boolean =
      bytes.size >= 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> {
      val entries = mutableMapOf<String, ByteArray>()
      var total = 0L
      ZipInputStream(bytes.inputStream()).use { zip ->
        for (entry in generateSequence { zip.nextEntry }.filterNot { it.isDirectory }) {
          val name = entry.name.replace('\\', '/')
          manifestCheck(
            !name.startsWith("/") && name.split('/').none { it == ".." || it.isEmpty() }
          ) {
            "The zip has a file outside it: $name"
          }
          if (name.startsWith("__MACOSX/") || name.substringAfterLast('/').startsWith(".")) continue
          // Stops reading as soon as the files would be too large together. A small zip can hold
          // gigabytes, and this runs in the screen reader's own process.
          val content = zip.readAtMost((MAX_BYTES - total + 1).toInt())
          total += content.size
          manifestCheck(entries.size < MAX_FILES && total <= MAX_BYTES) {
            "The zip has too many or too large files"
          }
          entries[name] = content
        }
      }
      if (MAIN in entries) return entries
      val folder = entries.keys.map { it.substringBefore('/', "") }.toSet().singleOrNull()
      manifestCheck(!folder.isNullOrEmpty() && "$folder/$MAIN" in entries) {
        "The zip has no main.js"
      }
      return entries.mapKeys { it.key.removePrefix("$folder/") }
    }
  }
}

fun InputStream.readAtMost(limit: Int): ByteArray {
  val out = ByteArrayOutputStream()
  val buffer = ByteArray(16 * 1024)
  while (out.size() < limit) {
    val read = read(buffer, 0, minOf(buffer.size, limit - out.size()))
    if (read < 0) break
    out.write(buffer, 0, read)
  }
  return out.toByteArray()
}

object ManifestReader {
  private val START = Regex("""export\s+const\s+manifest\s*=\s*\{""")
  private const val MEMORY_LIMIT = 4L * 1024 * 1024
  private const val STACK_LIMIT = 256L * 1024
  private const val TIME_LIMIT_MS = 500L

  fun read(source: String): String {
    val start =
      START.find(source)?.range?.last
        ?: manifestError("The script must start with export const manifest = { ... }")
    val literal = source.substring(start, closingBrace(source, start) + 1)
    val runtime =
      try {
        QuickJs(NoHost, MEMORY_LIMIT, STACK_LIMIT)
      } catch (e: QuickJsException) {
        manifestError(e.message.orEmpty())
      }
    return try {
      runtime.evalScript(
        "globalThis.__manifest = function () { return JSON.stringify(($literal)); };",
        "manifest.js",
        TIME_LIMIT_MS,
      )
      runtime.call("__manifest", null, TIME_LIMIT_MS) ?: manifestError("The manifest is empty")
    } catch (e: QuickJsException) {
      manifestError("The manifest must be plain values: ${e.message}")
    } finally {
      runtime.close()
    }
  }

  private fun closingBrace(source: String, open: Int): Int {
    var depth = 0
    var i = open
    while (i < source.length) {
      when (val c = source[i]) {
        '{' -> depth++
        '}' -> if (--depth == 0) return i
        '"',
        '\'',
        '`' -> i = stringEnd(source, i, c)
        '/' ->
          when (source.getOrNull(i + 1)) {
            '/' -> i = source.indexOf('\n', i).takeIf { it >= 0 } ?: source.length
            '*' -> i = source.indexOf("*/", i + 2).takeIf { it >= 0 }?.plus(1) ?: source.length
          }
      }
      i++
    }
    manifestError("The manifest object is not closed")
  }

  private fun stringEnd(source: String, start: Int, quote: Char): Int {
    var i = start + 1
    while (i < source.length && source[i] != quote) {
      i += if (source[i] == '\\') 2 else 1
    }
    return i
  }

  private object NoHost : QuickJs.Host {
    override fun call(method: String, json: String?): String? =
      error("The manifest cannot call Backtalk")

    override fun loadModule(name: String): ByteArray? = null

    override fun onUnhandledError(error: QuickJsException) {}
  }
}
