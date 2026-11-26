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
package com.google.android.accessibility.talkback.keyboard.words

/**
 * The words of one language that the user typed: how often each word of the list was used, so
 * that it ranks higher, and the words added by undoing their correction, which are never corrected
 * again. Saved as text, a word, a tab, its uses, a tab and 1 for an added word per line, at most
 * [MAX_WORDS] words: the least used of the words not added go first.
 */
class LearnedWords {
  private val uses = HashMap<String, Int>()
  private val added = HashSet<String>()

  /** Whether there are changes since the text was last taken with [text]. */
  var changed = false
    private set

  fun uses(word: String): Int = uses[word] ?: 0

  fun isAdded(word: String): Boolean = word in added

  fun addedWords(): Set<String> = added.toSet()

  fun recordUse(word: String) {
    val count = uses(word)
    if (count >= MAX_USES) return
    uses[word] = count + 1
    changed = true
    trim()
  }

  fun add(word: String) {
    if (added.size >= MAX_WORDS) return
    if (added.add(word)) {
      changed = true
      trim()
    }
  }

  fun clear() {
    if (uses.isEmpty() && added.isEmpty()) return
    uses.clear()
    added.clear()
    changed = true
  }

  /** The text to save, after which [changed] is false. */
  fun text(): String {
    changed = false
    val all = uses.keys + added
    return all.joinToString("\n") { word -> "$word\t${uses(word)}\t${if (word in added) 1 else 0}" }
  }

  private fun trim() {
    val over = uses.keys.size + added.count { it !in uses } - MAX_WORDS
    if (over <= 0) return
    uses.entries
      .filter { it.key !in added }
      .sortedBy { it.value }
      .take(over)
      .forEach { uses.remove(it.key) }
  }

  companion object {
    const val MAX_WORDS = 5_000
    const val MAX_USES = 100

    /** The longest saved text read, in characters. */
    private const val MAX_TEXT = 400_000

    /** Reads saved [text]; lines that don't parse are left out. */
    fun parse(text: String?): LearnedWords {
      val words = LearnedWords()
      if (text == null) return words
      for (line in text.take(MAX_TEXT).lineSequence()) {
        val parts = line.split('\t')
        if (parts.size != 3) continue
        val word = parts[0]
        if (!WordList.isListWord(word) || word.length > WordList.MAX_WORD_LENGTH) continue
        val count = parts[1].toIntOrNull()?.coerceIn(0, MAX_USES) ?: continue
        if (count > 0) words.uses[word] = count
        if (parts[2] == "1" && words.added.size < MAX_WORDS) words.added += word
        if (words.uses.size + words.added.size >= MAX_WORDS * 2) break
      }
      words.trim()
      return words
    }
  }
}
