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

/** SandFly: Read line breaks as spaces, which speaks text without the pauses at its line breaks. */
object LineBreaks {

  @JvmStatic
  fun isLineBreak(c: Char): Boolean = c == '\n' || c == '\r' || c == ' ' || c == ' '

  /**
   * Returns [text] with each run of line breaks replaced by one space, or null when it has no line
   * break, or nothing but line breaks and spaces, so that a line break on its own keeps its name.
   * Spans keep their place in the text around them.
   */
  @JvmStatic
  fun toSpaces(text: CharSequence): CharSequence? {
    if (text.none(::isLineBreak) || text.all { isLineBreak(it) || it.isWhitespace() }) return null
    // The start and end of each run of line breaks.
    val runs = ArrayList<IntArray>()
    var i = 0
    while (i < text.length) {
      if (!isLineBreak(text[i])) {
        i++
        continue
      }
      val start = i
      while (i < text.length && isLineBreak(text[i])) i++
      runs += intArrayOf(start, i)
    }
    if (text is Spanned) {
      val builder = SpannableStringBuilder(text)
      // From the end, so that earlier positions stay where they were.
      for (run in runs.asReversed()) builder.replace(run[0], run[1], " ")
      return builder
    }
    val builder = StringBuilder(text.length)
    var from = 0
    for (run in runs) {
      builder.append(text, from, run[0]).append(' ')
      from = run[1]
    }
    return builder.append(text, from, text.length).toString()
  }
}
