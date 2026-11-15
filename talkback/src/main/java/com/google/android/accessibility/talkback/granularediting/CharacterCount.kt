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

package com.google.android.accessibility.talkback.granularediting

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import java.text.BreakIterator

/** Character count: the length of the text in an edit box, in characters and words. */
object CharacterCount {

  /**
   * What the command says for [node]: "12 characters, 3 words", or only the characters in a
   * password field, whose words can't be told apart. A hint shown in an empty box counts as none.
   */
  @JvmStatic
  fun describe(context: Context, node: AccessibilityNodeInfoCompat): String {
    val text =
      if (node.isShowingHintText) "" else AccessibilityNodeInfoUtils.getText(node)?.toString() ?: ""
    val characters = characters(text)
    val res = context.resources
    val said = res.getQuantityString(R.plurals.character_count_characters, characters, characters)
    if (node.isPassword) {
      return said
    }
    val words = words(text)
    return context.getString(
      R.string.character_count_template,
      said,
      res.getQuantityString(R.plurals.character_count_words, words, words),
    )
  }

  /** The characters of [text] as they are seen: an emoji or a letter with its accent is one. */
  @VisibleForTesting
  @JvmStatic
  fun characters(text: String): Int {
    val boundary = BreakIterator.getCharacterInstance()
    boundary.setText(text)
    var count = 0
    while (boundary.next() != BreakIterator.DONE) {
      count++
    }
    return count
  }

  /**
   * The words of [text]: the pieces between word boundaries that have a letter or digit. Android's
   * word boundaries split Chinese and Japanese text into words too.
   */
  @VisibleForTesting
  @JvmStatic
  fun words(text: String): Int {
    val boundary = BreakIterator.getWordInstance()
    boundary.setText(text)
    var count = 0
    var start = boundary.first()
    var end = boundary.next()
    while (end != BreakIterator.DONE) {
      if (hasLetterOrDigit(text, start, end)) {
        count++
      }
      start = end
      end = boundary.next()
    }
    return count
  }

  private fun hasLetterOrDigit(text: String, start: Int, end: Int): Boolean {
    var i = start
    while (i < end) {
      val codePoint = text.codePointAt(i)
      if (Character.isLetterOrDigit(codePoint)) {
        return true
      }
      i += Character.charCount(codePoint)
    }
    return false
  }
}
