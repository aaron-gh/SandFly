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

package com.google.android.accessibility.talkback.speechdictionary

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.LocaleSpan
import com.google.android.accessibility.talkback.notifications.Replacement
import com.google.android.accessibility.utils.output.SpeechCleanupUtils
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Checks how the symbol dictionary changes speech and braille text. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SymbolDictionaryTest {
  private val prefs =
    RuntimeEnvironment.getApplication().getSharedPreferences("symbol-dictionary", Context.MODE_PRIVATE)

  private fun entry(find: String, replace: String, app: String = "") =
    DictionaryEntry(Replacement(find, replace, regex = false, wholeWord = false), app)

  private fun replace(text: CharSequence, vararg entries: DictionaryEntry, app: String = "") =
    SymbolDictionary.apply(text, entries.toList(), app)?.toString()

  @After
  fun clearEntries() {
    prefs.edit().clear().commit()
    SymbolDictionary.load(prefs)
    SpeechDictionary.load(RuntimeEnvironment.getApplication(), prefs)
  }

  @Test
  fun periodAfterAWordIsReplacedByASingleSpace() {
    assertEquals("end of line ", replace("end of line.", entry(".", " ")))
  }

  @Test
  fun multipleCharacterSymbolIsReplaced() {
    assertEquals("a to b", replace("a->b", entry("->", " to ")))
  }

  @Test
  fun emptyReplacementRemovesTheSymbol() {
    assertEquals("ab", replace("a#b", entry("#", "")))
  }

  @Test
  fun entriesApplyInOrder() {
    assertEquals("axby", replace("a..b.", entry("..", "x"), entry(".", "y")))
    assertEquals("ayyby", replace("a..b.", entry(".", "y"), entry("..", "x")))
  }

  @Test
  fun matchesDoNotOverlap() {
    assertEquals("--", replace("....", entry("..", "-")))
  }

  @Test
  fun anEmptyListChangesNothing() {
    assertNull(SymbolDictionary.apply("text.", emptyList(), ""))
    assertNull(replace("no symbols", entry(".", " ")))
  }

  @Test
  fun entryForOneAppAppliesOnlyInThatApp() {
    val entry = entry(".", " ", app = "com.example")
    assertEquals("a b", replace("a.b", entry, app = "com.example"))
    assertNull(replace("a.b", entry, app = "com.other"))
  }

  @Test
  fun replacementTextIsWrittenLiterally() {
    assertEquals("a\$1b", replace("a.b", entry(".", "\$1")))
  }

  @Test
  fun spansAroundTheSymbolAreKept() {
    val text = SpannableString("one. two")
    val span = LocaleSpan(Locale.FRENCH)
    text.setSpan(span, 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

    val result = SymbolDictionary.apply(text, listOf(entry(".", " ")), "") as Spanned

    assertEquals("one  two", result.toString())
    assertEquals(span, result.getSpans(0, result.length, LocaleSpan::class.java).single())
  }

  @Test
  fun aSingleSpaceReplacementIsKeptWhenSavedAndRead() {
    SymbolDictionary.write(prefs, listOf(entry(".", " ")))
    prefs.edit().commit()

    assertEquals(" ", SymbolDictionary.read(prefs).single().replacement.replace)
  }

  @Test
  fun entriesBeyondTheLimitAreNotRead() {
    SymbolDictionary.write(prefs, List(SymbolDictionary.MAX_ENTRIES + 10) { entry("$it", "x") })
    prefs.edit().commit()

    assertEquals(SymbolDictionary.MAX_ENTRIES, SymbolDictionary.read(prefs).size)
  }

  @Test
  fun entriesThatGrowTheTextPastTheLimitAreSkipped() {
    val grow = Array(30) { entry("a", "aaaa") }

    val result = SymbolDictionary.apply("a".repeat(1000), grow.toList(), "")!!

    assertTrue(result.length <= SymbolDictionary.MAX_RESULT_LENGTH)
  }

  @Test
  fun symbolEntriesApplyAfterTheSpeechDictionary() {
    SymbolDictionary.write(prefs, listOf(entry(".", " ")))
    prefs.edit().commit()
    SymbolDictionary.load(prefs)
    SpeechDictionary.onWindowShown("com.example")
    val speech = listOf(DictionaryEntry(Replacement("e.g.", "for example", wholeWord = false)))

    assertEquals(
      "see for example this ",
      SpeechDictionary.applyInCurrentApp("see e.g. this.", speech).toString(),
    )
  }

  @Test
  fun symbolEntriesApplyWithoutSpeechDictionaryEntries() {
    SymbolDictionary.write(prefs, listOf(entry(".", " ")))
    prefs.edit().commit()
    SymbolDictionary.load(prefs)
    SpeechDictionary.onWindowShown("com.example")

    assertEquals("a b", SpeechDictionary.applyInCurrentApp("a.b", emptyList()).toString())
  }

  @Test
  fun characterOnItsOwnKeepsItsTextForItsEntry() {
    val context = RuntimeEnvironment.getApplication()
    SymbolDictionary.write(prefs, listOf(entry("#", "hashmark"), entry(".", " ")))
    prefs.edit().commit()
    SymbolDictionary.load(prefs)
    SpeechDictionary.load(context, prefs)
    SpeechDictionary.onWindowShown("com.example")

    assertEquals("#", SpeechCleanupUtils.getCleanValueFor(context, '#'))
    assertNull(SpeechCleanupUtils.characterToName(context, '#'))
    assertEquals("hashmark", SpeechDictionary.apply("#").toString())
    // An entry that removes the character from speech leaves its name.
    assertEquals(SpeechCleanupUtils.characterToName(context, '.'), SpeechCleanupUtils.getCleanValueFor(context, '.'))
    assertTrue(SpeechCleanupUtils.getCleanValueFor(context, '.') != ".")
  }

  @Test
  fun characterInSandFlyWindowsKeepsItsName() {
    val context = RuntimeEnvironment.getApplication()
    SymbolDictionary.write(prefs, listOf(entry("#", "hashmark")))
    prefs.edit().commit()
    SymbolDictionary.load(prefs)
    SpeechDictionary.load(context, prefs)
    SpeechDictionary.onWindowShown(context.packageName)

    assertTrue(SpeechCleanupUtils.getCleanValueFor(context, '#') != "#")
  }
}
