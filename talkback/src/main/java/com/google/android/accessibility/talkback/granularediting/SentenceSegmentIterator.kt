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

import com.google.android.accessibility.utils.input.GranularityIterator.TextSegmentIterator
import java.util.Locale

/**
 * Moves through the sentences of [text], split as [TextPieces] splits them, by character offset.
 * Like the other text segment iterators, an offset inside a sentence moves over the rest of it in
 * the direction of the move, and an offset past either end of the text is treated as that end.
 */
class SentenceSegmentIterator
@JvmOverloads
constructor(val text: String, locale: Locale = Locale.getDefault()) : TextSegmentIterator {
  private val pieces = TextPieces.split(text, Granularity.SENTENCES, locale)

  override fun following(current: Int): IntArray? {
    val offset = current.coerceAtLeast(0)
    val piece = pieces.firstOrNull { it.coreEnd > offset } ?: return null
    return intArrayOf(maxOf(piece.coreStart, offset), piece.coreEnd)
  }

  override fun preceding(current: Int): IntArray? {
    val offset = current.coerceAtMost(text.length)
    val piece = pieces.lastOrNull { it.coreStart < offset } ?: return null
    return intArrayOf(piece.coreStart, minOf(piece.coreEnd, offset))
  }
}
