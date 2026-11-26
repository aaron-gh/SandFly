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

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Corrections and suggestions from the real English (US) and German word lists. */
class WordSuggesterTest {

  private fun suggester(learned: LearnedWords = LearnedWords()) =
    WordSuggester(WordLanguage.EN_US, englishList(), learned)

  @Test
  fun correctsCommonTypos() {
    val suggester = suggester()
    val typos =
      mapOf(
        "teh" to "the",
        "hte" to "the",
        "recieve" to "receive",
        "thier" to "their",
        "wierd" to "weird",
        "becuase" to "because",
        "occured" to "occurred",
        "definately" to "definitely",
        "seperate" to "separate",
        "untill" to "until",
        "tommorow" to "tomorrow",
        "beleive" to "believe",
        "freind" to "friend",
        "wiht" to "with",
        "jsut" to "just",
        "abotu" to "about",
        "thw" to "the",
        "yhe" to "the",
        "helllo" to "hello",
        "probaly" to "probably",
        "dont" to "don't",
        "doesnt" to "doesn't",
        "im" to "I'm",
        "ive" to "I've",
        "i" to "I",
      )
    val wrong =
      typos.mapNotNull { (typo, expected) ->
        val got = suggester.correction(typo, sentenceStart = false)
        if (got == expected) null else "$typo -> $got (expected $expected)"
      }
    assertTrue(wrong.joinToString("\n"), wrong.isEmpty())
  }

  @Test
  fun leavesWordsAsTyped() {
    val suggester = suggester()
    val words =
      listOf(
        "form", "from", "its", "it's", "were", "well", "hell", "the", "then", "than", "though",
        "through", "quiet", "quite", "lose", "loose", "affect", "effect", "trail", "trial",
        "angel", "angle", "dessert", "desert", "casual", "causal", "united", "keyboard", "lets", "wed",
        "Zinwa", "sandfly", "talkback",
      )
    val changed =
      words.mapNotNull { word ->
        suggester.correction(word, sentenceStart = false)?.let { "$word -> $it" }
      }
    assertTrue(changed.joinToString("\n"), changed.isEmpty())
  }

  @Test
  fun keepsCapitals() {
    val suggester = suggester()
    assertEquals("The", suggester.correction("Teh", sentenceStart = true))
    // Words typed in capitals are as meant, as acronyms are.
    assertNull(suggester.correction("TEH", sentenceStart = false))
    assertEquals("THE", suggester.suggestions("TEH").first())
    // A capital within a sentence is a name.
    assertNull(suggester.correction("Teh", sentenceStart = false))
  }

  @Test
  fun suggestsCompletionsAndCorrections() {
    val suggester = suggester()
    val forRec = suggester.suggestions("recieve")
    assertEquals("receive", forRec.first())
    val forTh = suggester.suggestions("th")
    assertTrue(forTh.toString(), "the" in forTh.take(3))
    val forKeyb = suggester.suggestions("keyb")
    assertTrue(forKeyb.toString(), "keyboard" in forKeyb.take(3))
    assertFalse("the" in suggester.suggestions("the"))
    assertTrue(suggester.suggestions("Keyb").first().startsWith("K"))
  }

  @Test
  fun addedWordIsNeverCorrected() {
    val learned = LearnedWords()
    val suggester = suggester(learned)
    assertEquals("the", suggester.correction("teh", sentenceStart = false))
    suggester.addWord("teh")
    assertNull(suggester.correction("teh", sentenceStart = false))
    val again = WordSuggester(WordLanguage.EN_US, englishList(), LearnedWords.parse(learned.text()))
    assertNull(again.correction("teh", sentenceStart = false))
  }

  @Test
  fun usesRankWordsHigher() {
    val learned = LearnedWords()
    val suggester = suggester(learned)
    val before = suggester.completions("keyb", 8).map { it.word }
    val third = before[2]
    repeat(5) { suggester.recordUse(third) }
    val after = suggester.completions("keyb", 8).map { it.word }
    assertTrue("$before $after", after.indexOf(third) < 2)
  }

  @Test
  fun correctsGermanWithoutUmlauts() {
    val list = javaClass.listFile("de.txt")
    val suggester = WordSuggester(WordLanguage.DE, list, LearnedWords())
    assertEquals("für", suggester.correction("fur", sentenceStart = false))
    assertTrue(suggester.suggestions("mochte").contains("möchte"))
  }

  @Test
  fun neighbouringKeys() {
    assertTrue(KeyLayout.QWERTY.areNeighbours('e', 'r'))
    assertTrue(KeyLayout.QWERTY.areNeighbours('a', 'q'))
    assertTrue(KeyLayout.QWERTY.areNeighbours('a', 'w'))
    assertTrue(KeyLayout.QWERTY.areNeighbours('n', 'h'))
    assertTrue(KeyLayout.QWERTY.areNeighbours('n', 'j'))
    assertFalse(KeyLayout.QWERTY.areNeighbours('a', 'e'))
    assertFalse(KeyLayout.QWERTY.areNeighbours('q', 'p'))
  }

  @Test
  fun learnedWordsParseLeavesOutBadLines() {
    val learned = LearnedWords.parse("good\t3\t0\nbad line\n1234\t2\t0\nadded\t0\t1\n")
    assertEquals(3, learned.uses("good"))
    assertTrue(learned.isAdded("added"))
    assertEquals(0, learned.uses("1234"))
  }

  @Test
  fun listIsCompact() {
    val list = englishList()
    // About 20 bytes a node.
    assertTrue("${list.nodeCount} nodes", list.nodeCount < 400_000)
  }

  companion object {
    /** A new list each time, as suggesters add words to theirs. */
    private fun englishList(): WordList = WordSuggesterTest::class.java.listFile("en_us.txt")

    private fun Class<*>.listFile(name: String): WordList {
      val language = WordLanguage.entries.first { it.fileName == name }
      val file =
        listOf("src/phone/assets/word_lists/$name", "talkback/src/phone/assets/word_lists/$name")
          .map(::File)
          .first { it.exists() }
      return file.bufferedReader().use { WordList.read(it, language) }
    }
  }
}
