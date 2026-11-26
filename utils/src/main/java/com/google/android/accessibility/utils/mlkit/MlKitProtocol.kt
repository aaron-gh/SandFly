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

package com.google.android.accessibility.utils.mlkit

import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import com.google.mlkit.nl.languageid.IdentifiedLanguage
import com.google.mlkit.vision.text.Text
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/**
 * The messages between [MlKitHelper] in SandFly's process and [MlKitHelperService] in the
 * `:mlkit` process, and how their results are written. Each request carries its ID in arg1, and
 * its answer has the same ID.
 */
object MlKitProtocol {
  /** [KEY_TEXT], with [KEY_THRESHOLD]: answers [KEY_LANGUAGE]. */
  const val MSG_IDENTIFY_LANGUAGE = 1

  /** [KEY_TEXT], with [KEY_THRESHOLD]: answers [KEY_LANGUAGES] and [KEY_CONFIDENCES]. */
  const val MSG_IDENTIFY_POSSIBLE_LANGUAGES = 2

  /**
   * [KEY_IMAGE] and [KEY_ROTATION] in [KEY_SCRIPT], with [KEY_TESSERACT_MODELS] for Cyrillic:
   * answers [KEY_TEXT_DATA] or [KEY_TEXT_FILE].
   */
  const val MSG_RECOGNIZE_TEXT = 3

  /** Asks Google Play services to download the recognizer for [KEY_SCRIPT]. Answers nothing. */
  const val MSG_INSTALL_TEXT_RECOGNIZER = 4

  /** The answer to a request. */
  const val MSG_RESULT = 100

  /** A request failed: the ML Kit error code in arg2, the message in [KEY_ERROR]. */
  const val MSG_ERROR = 101

  const val KEY_TEXT = "text"
  const val KEY_THRESHOLD = "threshold"
  const val KEY_LANGUAGE = "language"
  const val KEY_LANGUAGES = "languages"
  const val KEY_CONFIDENCES = "confidences"
  const val KEY_IMAGE = "image"
  const val KEY_ROTATION = "rotation"
  const val KEY_SCRIPT = "script"
  const val KEY_TESSERACT_MODELS = "tesseract_models"
  const val KEY_TEXT_DATA = "text_data"
  const val KEY_TEXT_FILE = "text_file"
  const val KEY_ERROR = "error"

  /** A threshold of ML Kit's default. */
  const val DEFAULT_THRESHOLD = -1f

  /**
   * The longest text sent for language identification. ML Kit decides from the start of a text,
   * and a message holds 1 MB at most.
   */
  const val MAX_IDENTIFY_CHARS = 20_000

  /**
   * Recognized text larger than this goes through a file in the cache directory, since a message
   * holds 1 MB at most, shared with every other message on the way.
   */
  const val MAX_INLINE_TEXT_BYTES = 128 * 1024

  /** The cache directory of [KEY_TEXT_FILE]s. */
  const val TEXT_FILE_DIR = "mlkit-helper"

  /** The most items of any one kind read from an answer, against a damaged file. */
  private const val MAX_ITEMS = 100_000

  fun putLanguages(data: Bundle, languages: List<IdentifiedLanguage>) {
    data.putStringArray(KEY_LANGUAGES, languages.map { it.languageTag }.toTypedArray())
    data.putFloatArray(KEY_CONFIDENCES, languages.map { it.confidence }.toFloatArray())
  }

  fun languages(data: Bundle): List<IdentifiedLanguage> {
    val tags = data.getStringArray(KEY_LANGUAGES) ?: return emptyList()
    val confidences = data.getFloatArray(KEY_CONFIDENCES) ?: return emptyList()
    return tags.indices
      .filter { it < confidences.size }
      .map { IdentifiedLanguage(tags[it], confidences[it]) }
  }

  /** Writes [text] with its blocks, lines and elements. Symbols are left out. */
  fun encodeText(text: Text): ByteArray {
    val bytes = ByteArrayOutputStream()
    DataOutputStream(bytes).use { out ->
      out.writeString(text.text)
      out.writeInt(text.textBlocks.size)
      for (block in text.textBlocks) {
        out.writeBase(block.text, block.boundingBox, block.cornerPoints, block.recognizedLanguage)
        out.writeInt(block.lines.size)
        for (line in block.lines) {
          out.writeBase(line.text, line.boundingBox, line.cornerPoints, line.recognizedLanguage)
          out.writeFloat(line.confidence)
          out.writeFloat(line.angle)
          out.writeInt(line.elements.size)
          for (element in line.elements) {
            out.writeBase(
              element.text,
              element.boundingBox,
              element.cornerPoints,
              element.recognizedLanguage,
            )
            out.writeFloat(element.confidence)
            out.writeFloat(element.angle)
          }
        }
      }
    }
    return bytes.toByteArray()
  }

  /** Reads what [encodeText] wrote. Throws [IOException] when it is damaged. */
  @Throws(IOException::class)
  fun decodeText(bytes: ByteArray): Text {
    val input = DataInputStream(ByteArrayInputStream(bytes))
    val text = input.readString()
    val blocks =
      List(input.readCount()) {
        val base = input.readBase()
        val lines =
          List(input.readCount()) {
            val lineBase = input.readBase()
            val confidence = input.readFloat()
            val angle = input.readFloat()
            val elements =
              List(input.readCount()) {
                val elementBase = input.readBase()
                Text.Element(
                  elementBase.text,
                  elementBase.box,
                  elementBase.corners,
                  elementBase.language,
                  null,
                  input.readFloat(),
                  input.readFloat(),
                  emptyList<Text.Symbol>(),
                )
              }
            Text.Line(
              lineBase.text,
              lineBase.box,
              lineBase.corners,
              lineBase.language,
              null,
              elements,
              confidence,
              angle,
            )
          }
        Text.TextBlock(base.text, base.box, base.corners, base.language, null, lines)
      }
    return Text(text, blocks)
  }

  private class Base(val text: String, val box: Rect, val corners: List<Point>, val language: String)

  private fun DataOutputStream.writeBase(
    text: String,
    box: Rect?,
    corners: Array<Point>?,
    language: String?,
  ) {
    writeString(text)
    writeBoolean(box != null)
    if (box != null) {
      writeInt(box.left)
      writeInt(box.top)
      writeInt(box.right)
      writeInt(box.bottom)
    }
    val points = corners.orEmpty()
    writeInt(points.size)
    for (point in points) {
      writeInt(point.x)
      writeInt(point.y)
    }
    writeString(language.orEmpty())
  }

  private fun DataInputStream.readBase(): Base {
    val text = readString()
    // ML Kit's text always has a box; the constructors take no null.
    val box = if (readBoolean()) Rect(readInt(), readInt(), readInt(), readInt()) else Rect()
    val corners = List(readCount()) { Point(readInt(), readInt()) }
    return Base(text, box, corners, readString())
  }

  // Not writeUTF, which takes 64 KB at most.
  private fun DataOutputStream.writeString(value: String) {
    val bytes = value.toByteArray(Charsets.UTF_8)
    writeInt(bytes.size)
    write(bytes)
  }

  private fun DataInputStream.readString(): String {
    val size = readInt()
    if (size < 0 || size > available()) throw IOException("Bad string length $size")
    val bytes = ByteArray(size)
    readFully(bytes)
    return String(bytes, Charsets.UTF_8)
  }

  private fun DataInputStream.readCount(): Int {
    val count = readInt()
    if (count < 0 || count > MAX_ITEMS) throw IOException("Bad count $count")
    return count
  }
}
