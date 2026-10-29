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

package com.google.android.accessibility.talkback.speechdictionary

import android.text.SpannableString
import android.text.Spanned
import android.text.style.LocaleSpan
import com.google.android.accessibility.talkback.notifications.Replacement
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Checks how the speech dictionary changes speech. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpeechDictionaryTest {

  private fun entry(
    find: String,
    replace: String,
    regex: Boolean = false,
    wholeWord: Boolean = true,
    app: String = "",
  ) = DictionaryEntry(Replacement(find, replace, regex, wholeWord), app)

  private fun say(text: CharSequence, vararg entries: DictionaryEntry, app: String = "") =
    SpeechDictionary.apply(text, entries.toList(), app)?.toString()

  @Test
  fun wordIsSaidAsAnotherWord() {
    assertEquals("laughing out loud, ok", say("LOL, ok", entry("lol", "laughing out loud")))
  }

  @Test
  fun partOfAWordIsLeftAloneForWholeWords() {
    assertNull(say("lollipop", entry("lol", "laughing")))
    assertEquals("laughinglipop", say("lollipop", entry("lol", "laughing", wholeWord = false)))
  }

  @Test
  fun symbolIsSaidAsASpaceOrLeftOut() {
    assertEquals("snake case name", say("snake_case_name", entry("_", " ", wholeWord = false)))
    assertEquals("Heading", say("## Heading", entry("#", "", wholeWord = false), entry(" ", "", wholeWord = false)))
  }

  @Test
  fun regularExpressionFillsInGroups() {
    assertEquals("3 minutes", say("3m", entry("(\\d+)m", "$1 minutes", regex = true)))
  }

  @Test
  fun replacementTextIsSaidAsWritten() {
    assertEquals("cost \$5", say("cost five", entry("five", "\$5")))
  }

  @Test
  fun entriesApplyInOrder() {
    assertEquals("c", say("a", entry("a", "b"), entry("b", "c")))
  }

  @Test
  fun appEntriesApplyOnlyInTheirApp() {
    val entry = entry("msg", "message", app = "com.example.chat")
    assertNull(say("msg", entry, app = "com.example.mail"))
    assertEquals("message", say("msg", entry, app = "com.example.chat"))
  }

  @Test
  fun unchangedSpeechIsLeftAsItIs() {
    assertNull(say("hello", entry("bye", "goodbye")))
  }

  @Test
  fun spansOfTheTextAroundAreKept() {
    val text = SpannableString("one lol two")
    text.setSpan(LocaleSpan(Locale.FRENCH), 8, 11, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    val said = SpeechDictionary.apply(text, listOf(entry("lol", "laughing")), "") as Spanned
    assertEquals("one laughing two", said.toString())
    val span = said.getSpans(0, said.length, LocaleSpan::class.java).single()
    assertEquals("two", said.subSequence(said.getSpanStart(span), said.getSpanEnd(span)).toString())
  }

  @Test
  fun entriesSurviveSaving() {
    val entries = listOf(entry("a", "b", regex = true, wholeWord = false, app = "com.example"))
    assertEquals(entries, SpeechDictionary.fromJson(SpeechDictionary.toJson(entries)))
  }
}
