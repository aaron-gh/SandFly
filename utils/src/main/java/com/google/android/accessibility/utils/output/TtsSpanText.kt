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

package com.google.android.accessibility.utils.output

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.TtsSpan

/**
 * Text whose [TtsSpan.TYPE_TEXT] spans are written into it as words, such as punctuation names, for
 * engines that read only the plain text and ignore the spans. [toOriginal] turns a position in
 * [text], as an engine reports it, back into a position in the text before.
 */
class TtsSpanText
private constructor(
  val text: CharSequence,
  /** Where each replacement starts and ends in [text], in order. */
  private val starts: IntArray,
  private val ends: IntArray,
  /** Where each replaced span started and ended in the text before. */
  private val originalStarts: IntArray,
  private val originalEnds: IntArray,
) {
  /** The position in the text before of [offset] in [text]. */
  fun toOriginal(offset: Int): Int {
    var shift = 0
    for (i in starts.indices) {
      if (offset < starts[i]) break
      if (offset < ends[i]) return originalStarts[i]
      shift = originalEnds[i] - ends[i]
    }
    return offset + shift
  }

  companion object {
    /** [text] with its text spans written in, or null if it has none. */
    @JvmStatic
    fun flatten(text: CharSequence): TtsSpanText? {
      if (text !is Spanned) return null
      val spans =
        text
          .getSpans(0, text.length, TtsSpan::class.java)
          .filter {
            it.type == TtsSpan.TYPE_TEXT &&
              !it.args.getString(TtsSpan.ARG_TEXT).isNullOrBlank() &&
              text.getSpanStart(it) < text.getSpanEnd(it)
          }
          .sortedBy { text.getSpanStart(it) }
      if (spans.isEmpty()) return null
      // Overlapping spans keep the first.
      val kept = ArrayList<TtsSpan>(spans.size)
      var lastEnd = -1
      for (span in spans) {
        if (text.getSpanStart(span) >= lastEnd) {
          kept += span
          lastEnd = text.getSpanEnd(span)
        }
      }
      val builder = SpannableStringBuilder(text)
      val starts = IntArray(kept.size)
      val ends = IntArray(kept.size)
      val originalStarts = IntArray(kept.size)
      val originalEnds = IntArray(kept.size)
      var shift = 0
      for ((i, span) in kept.withIndex()) {
        val start = text.getSpanStart(span)
        val end = text.getSpanEnd(span)
        val words = words(span)
        originalStarts[i] = start
        originalEnds[i] = end
        starts[i] = start + shift
        ends[i] = starts[i] + words.length
        shift += words.length - (end - start)
      }
      // From the end, so that the earlier positions stay where they are.
      for (i in kept.indices.reversed()) {
        builder.removeSpan(kept[i])
        builder.replace(originalStarts[i], originalEnds[i], words(kept[i]))
      }
      return TtsSpanText(builder, starts, ends, originalStarts, originalEnds)
    }

    /** The span's text, with spaces to keep it apart from the text around it. */
    private fun words(span: TtsSpan): String =
      " " + span.args.getString(TtsSpan.ARG_TEXT)!!.trim() + " "
  }
}
