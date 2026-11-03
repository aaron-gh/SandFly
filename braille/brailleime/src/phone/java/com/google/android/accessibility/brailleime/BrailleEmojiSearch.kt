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
package com.google.android.accessibility.brailleime

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.accessibility.braille.common.BrailleCommonTalkBackSpeaker
import com.google.android.accessibility.braille.common.BrailleUserPreferences
import com.google.android.accessibility.braille.common.TalkBackSpeaker.AnnounceType
import com.google.android.accessibility.braille.common.translate.BrailleTranslateUtils
import com.google.android.accessibility.braille.interfaces.BrailleCharacter
import com.google.android.accessibility.braille.interfaces.BrailleWord
import com.google.android.accessibility.braille.translate.BrailleTranslator
import com.google.android.accessibility.utils.output.EmojiSearch
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Emoji search on the braille keyboard: the user types words of an emoji's name in uncontracted
 * braille of the keyboard's language, moves through the emoji whose names hold them, and inserts
 * one into the text field. The names are CLDR's, in the language of the braille table.
 */
class BrailleEmojiSearch(private val context: Context, private val callback: Callback) {
  /** Puts an emoji into the text field. */
  fun interface Callback {
    /** Inserts [text] at the cursor and returns whether it was inserted. */
    fun insert(text: String): Boolean
  }

  private val handler = Handler(Looper.getMainLooper())
  private var executor: ExecutorService? = null
  private var translator: BrailleTranslator? = null
  private var search: EmojiSearch? = null

  /** Counts searches started, so that emoji read for an earlier one are dropped. */
  private var generation = 0

  /** The words typed, each as its cells; the last is the word being typed, which may be empty. */
  private val words = mutableListOf(BrailleWord())
  private var matches: List<EmojiSearch.Emoji> = emptyList()
  private var index = 0

  var isActive = false
    private set

  /** Starts emoji search, or says why it can't. */
  fun start() {
    if (isActive) return
    val code = BrailleUserPreferences.readCurrentActiveInputCodeAndCorrect(context)
    translator =
      try {
        BrailleUserPreferences.readTranslatorFactory(context)
          .create(context, code.name, /* contractedMode= */ false)
      } catch (e: RuntimeException) {
        BrailleImeLog.e(TAG, "No braille translator for $code", e)
        null
      }
    if (translator == null) {
      speak(context.getString(R.string.emoji_search_not_available_announcement))
      return
    }
    isActive = true
    clearWords()
    val language = code.locale ?: Locale.getDefault()
    val current = ++generation
    val executor =
      Executors.newSingleThreadExecutor { runnable ->
          Thread(runnable, "BrailleEmojiSearch").apply { isDaemon = true }
        }
        .also { executor = it }
    executor.execute {
      val read =
        try {
          EmojiSearch.read(context.applicationContext, language)
        } catch (e: Throwable) {
          BrailleImeLog.e(TAG, "Can't read emoji", e)
          null
        }
      handler.post { onRead(current, read) }
    }
    executor.shutdown()
    speak(context.getString(R.string.emoji_search_announcement))
  }

  /** Ends emoji search, saying so when [announce] is true. */
  fun stop(announce: Boolean) {
    if (!isActive) return
    isActive = false
    generation++
    executor?.shutdownNow()
    executor = null
    handler.removeCallbacksAndMessages(null)
    translator = null
    search = null
    clearWords()
    if (announce) speak(context.getString(R.string.emoji_search_closed_announcement))
  }

  private fun onRead(current: Int, read: EmojiSearch?) {
    if (current != generation || !isActive) return
    search = read
    if (query().isNotBlank()) {
      findMatches()
      sayMatch()
    }
  }

  /**
   * Adds [cell] to the word being typed and says the character and the first emoji. A cell that
   * leaves no emoji is dropped; one that adds no letter, such as a capital sign, is kept.
   */
  fun addCell(cell: BrailleCharacter) {
    if (!isActive) return
    val word = words.last()
    if (cellCount() >= MAX_CELLS) return
    val before = print(word)
    word.append(cell)
    val after = print(word)
    val added = if (after.startsWith(before)) after.substring(before.length) else after
    val echo = added.ifBlank { BrailleTranslateUtils.getDotsText(context.resources, cell) }
    speak(echo)
    val search = search
    if (search == null || search.normalize(after) == search.normalize(before)) return
    val found = search.find(query(), MAX_MATCHES)
    if (found.isEmpty()) {
      word.remove(word.size() - 1)
      speak(context.getString(R.string.emoji_search_no_emoji_announcement), queue = true)
      return
    }
    matches = found
    index = 0
    speak(describeMatch(), queue = true)
  }

  /**
   * Ends the word being typed, so that the next cells start another word of the name. Spaces after
   * a space or at the start count as one.
   */
  fun addSpace() {
    if (!isActive) return
    if (!words.last().isEmpty && words.size < MAX_WORDS) words.add(BrailleWord())
    speak(context.getString(R.string.perform_add_space_announcement))
  }

  /** Deletes the last cell, or the space after the last word, and returns false if there is none. */
  fun deleteCell(): Boolean {
    if (!isActive) return false
    val word = words.last()
    val deleted: String
    if (word.isEmpty) {
      if (words.size == 1) return false
      words.removeAt(words.size - 1)
      deleted = context.getString(R.string.perform_add_space_announcement)
    } else {
      val before = print(word)
      val cell = word.remove(word.size() - 1)
      val after = print(word)
      deleted =
        (if (before.startsWith(after)) before.substring(after.length) else before).ifBlank {
          BrailleTranslateUtils.getDotsText(context.resources, cell)
        }
    }
    findMatches()
    speak(context.getString(R.string.read_out_deleted, deleted))
    return true
  }

  /** Says the next emoji, or the previous one if [forward] is false, going round at the ends. */
  fun move(forward: Boolean) {
    if (!isActive) return
    if (matches.isEmpty()) {
      speak(noEmoji())
      return
    }
    index = (index + if (forward) 1 else matches.size - 1) % matches.size
    sayMatch()
  }

  /** Inserts the emoji that was last said into the text field. */
  fun insert() {
    if (!isActive) return
    val match = matches.getOrNull(index)
    if (match == null) {
      speak(noEmoji())
      return
    }
    if (callback.insert(match.emoji)) {
      speak(context.getString(R.string.emoji_search_inserted_announcement, match.name))
    }
  }

  private fun findMatches() {
    matches = search?.find(query(), MAX_MATCHES).orEmpty()
    index = 0
  }

  private fun sayMatch() {
    speak(if (matches.isEmpty()) noEmoji() else describeMatch())
  }

  private fun describeMatch(): String {
    val match = matches[index]
    return context.getString(
      R.string.emoji_search_item_announcement,
      match.name,
      index + 1,
      matches.size,
    )
  }

  private fun noEmoji(): String =
    context.getString(
      if (query().isBlank()) R.string.emoji_search_announcement
      else R.string.emoji_search_no_emoji_announcement
    )

  /** The words typed, as print, with a space between each. */
  private fun query(): String = words.joinToString(" ") { print(it) }.trim()

  private fun print(word: BrailleWord): String =
    if (word.isEmpty) "" else translator?.translateToPrintPartial(word).orEmpty()

  private fun cellCount(): Int = words.sumOf { it.size() }

  private fun clearWords() {
    words.clear()
    words.add(BrailleWord())
    matches = emptyList()
    index = 0
  }

  private fun speak(text: String, queue: Boolean = false) {
    BrailleCommonTalkBackSpeaker.getInstance()
      .speak(text, if (queue) AnnounceType.ENQUEUE else AnnounceType.INTERRUPT)
  }

  private companion object {
    const val TAG = "BrailleEmojiSearch"
    /** The most emoji offered for a search. */
    const val MAX_MATCHES = 100
    const val MAX_CELLS = 50
    const val MAX_WORDS = 10
  }
}
