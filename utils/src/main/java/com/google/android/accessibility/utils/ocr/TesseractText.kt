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

package com.google.android.accessibility.utils.ocr

import android.graphics.Point
import android.graphics.Rect
import com.google.mlkit.vision.text.Text

/**
 * Turns the words Tesseract found into ML Kit's [Text], with blocks, lines and elements, so that
 * the callers of [LanguageTextRecognizer] read both recognizers' results the same way.
 */
class TesseractText(
  /** The BCP 47 tag of the recognized language, such as "ru". */
  private val language: String,
  /** Boxes are multiplied by this, for an image that was made smaller to be recognized. */
  private val scale: Float = 1f,
) {
  /** A word as Tesseract's result iterator gives it, with its confidence from 0 to 100. */
  class Word(
    val text: String,
    val box: Rect,
    val confidence: Float,
    /** Whether the word starts a block, and so a line. */
    val startsBlock: Boolean,
    /** Whether the word starts a line. */
    val startsLine: Boolean,
  )

  private class LineBuilder {
    val elements = ArrayList<Text.Element>()
    val confidences = ArrayList<Float>()
  }

  private val blocks = ArrayList<ArrayList<LineBuilder>>()
  private var words = 0

  /**
   * Adds [word]. Words with no letters or digits, and words Tesseract is less sure of than
   * [MIN_CONFIDENCE], such as those it finds in icons and lines, are left out.
   */
  fun add(word: Word) {
    if (words >= MAX_WORDS) return
    val text = word.text.trim()
    if (text.isEmpty() || text.none(Char::isLetterOrDigit) || word.confidence < MIN_CONFIDENCE) {
      // A left-out word that starts a block or line still starts one for the words after it.
      if (word.startsBlock) blocks.add(ArrayList())
      if (word.startsLine) blocks.lastOrNull()?.add(LineBuilder())
      return
    }
    if (word.startsBlock || blocks.isEmpty()) blocks.add(ArrayList())
    val lines = blocks.last()
    if (word.startsLine || word.startsBlock || lines.isEmpty()) lines.add(LineBuilder())
    val box = scaled(word.box)
    val line = lines.last()
    line.elements.add(
      Text.Element(
        text,
        box,
        corners(box),
        language,
        null,
        word.confidence / 100f,
        0f,
        emptyList<Text.Symbol>(),
      )
    )
    line.confidences.add(word.confidence / 100f)
    words++
  }

  fun build(): Text {
    val textBlocks =
      blocks.mapNotNull { lineBuilders ->
        val lines =
          lineBuilders
            .filter { it.elements.isNotEmpty() }
            .map { builder ->
              val box = union(builder.elements.map { it.boundingBox ?: Rect() })
              Text.Line(
                builder.elements.joinToString(" ") { it.text },
                box,
                corners(box),
                language,
                null,
                builder.elements,
                builder.confidences.average().toFloat(),
                0f,
              )
            }
        if (lines.isEmpty()) return@mapNotNull null
        val box = union(lines.map { it.boundingBox ?: Rect() })
        Text.TextBlock(lines.joinToString("\n") { it.text }, box, corners(box), language, null, lines)
      }
    return Text(textBlocks.joinToString("\n") { it.text }, textBlocks)
  }

  private fun scaled(box: Rect): Rect =
    if (scale == 1f) Rect(box)
    else
      Rect(
        (box.left * scale).toInt(),
        (box.top * scale).toInt(),
        (box.right * scale).toInt(),
        (box.bottom * scale).toInt(),
      )

  private fun union(boxes: List<Rect>): Rect {
    val union = Rect(boxes.first())
    for (box in boxes.drop(1)) union.union(box)
    return union
  }

  private fun corners(box: Rect): List<Point> =
    listOf(
      Point(box.left, box.top),
      Point(box.right, box.top),
      Point(box.right, box.bottom),
      Point(box.left, box.bottom),
    )

  companion object {
    /**
     * Tesseract's confidence below which a word is left out. Text on screens is mostly read with
     * 80 to 95; shapes it takes for letters mostly have less than 30.
     */
    const val MIN_CONFIDENCE = 30f

    /** The most words kept, against an image of noise. */
    const val MAX_WORDS = 20_000
  }
}
