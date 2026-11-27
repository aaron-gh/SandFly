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

package com.google.android.accessibility.talkback.actor.voicecommands

import android.content.Context
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotUnderstoodPhrasesTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private val locale = Locale.ENGLISH
  private val day = 24 * 60 * 60 * 1000L

  @Before
  fun setUp() {
    SharedPreferencesUtils.getSharedPreferences(context).edit().clear().commit()
    NotUnderstoodPhrases.clear(context)
  }

  /** [entries] with each of [phrases] counted once at [nowMs], and whether the last was listed. */
  private fun addAll(
    entries: List<NotUnderstoodPhrases.Entry>,
    vararg phrases: String,
    nowMs: Long = 0,
  ): Pair<List<NotUnderstoodPhrases.Entry>, Boolean> {
    var result = entries to false
    for (phrase in phrases) result = NotUnderstoodPhrases.add(result.first, phrase, locale, nowMs)
    return result
  }

  @Test
  fun theThirdTimeListsThePhraseOnce() {
    var entries = emptyList<NotUnderstoodPhrases.Entry>()
    val listed =
      (1..4).map {
        val (next, now) = NotUnderstoodPhrases.add(entries, "read my mail", locale, it * 1000L)
        entries = next
        now
      }
    assertEquals(listOf(false, false, true, false), listed)
    assertEquals(4, entries.single().count)
    assertEquals(listOf("read my mail"), NotUnderstoodPhrases.listed(entries, 5000L).map { it.phrase })
  }

  @Test
  fun phrasesAreCountedAsCustomVoiceCommandsAreMatched() {
    val (entries, listed) = addAll(emptyList(), "Read my mail", "read my mail!", "READ  my, mail")
    assertTrue(listed)
    val entry = entries.single()
    assertEquals("read my mail", entry.key)
    // The phrase said last is the one shown.
    assertEquals("READ  my, mail", entry.phrase)
    assertEquals(VoiceGrammar.normalize("Read my mail", locale), NotUnderstoodPhrases.key("read my mail.", locale))
  }

  @Test
  fun emptyPhrasesAreNotCounted() {
    val (entries, listed) = addAll(emptyList(), "...", "  ", "!?")
    assertFalse(listed)
    assertTrue(entries.isEmpty())
  }

  @Test
  fun countsAreForgottenAfterSomeDays() {
    val (counting, _) = addAll(emptyList(), "open the door", nowMs = 0)
    assertEquals(1, NotUnderstoodPhrases.current(counting, 7 * day).size)
    assertTrue(NotUnderstoodPhrases.current(counting, 7 * day + 1).isEmpty())
    // A count that was forgotten starts again.
    val (again, listed) = addAll(counting, "open the door", "open the door", nowMs = 8 * day)
    assertFalse(listed)
    assertEquals(2, again.single().count)

    val (listedEntries, _) = addAll(emptyList(), "lights", "lights", "lights", nowMs = 0)
    assertEquals(1, NotUnderstoodPhrases.listed(listedEntries, 8 * day).size)
    assertEquals(1, NotUnderstoodPhrases.listed(listedEntries, 30 * day).size)
    assertTrue(NotUnderstoodPhrases.listed(listedEntries, 30 * day + 1).isEmpty())
  }

  @Test
  fun keepsAtMostFiftyPhrasesListedOnesFirst() {
    var entries = addAll(emptyList(), "kept", "kept", "kept", nowMs = 0).first
    for (i in 1..60) entries = NotUnderstoodPhrases.add(entries, "phrase $i", locale, i.toLong()).first
    assertEquals(NotUnderstoodPhrases.MAX, entries.size)
    val keys = entries.map { it.key }.toSet()
    assertTrue("kept" in keys)
    assertTrue("phrase 60" in keys)
    assertFalse("phrase 1" in keys)
    assertFalse("phrase 11" in keys)
    assertTrue("phrase 12" in keys)
  }

  @Test
  fun readsBackWhatItWritesAndDropsBadEntries() {
    val (entries, _) = addAll(emptyList(), "one", "two", "two", nowMs = 42)
    assertEquals(entries, NotUnderstoodPhrases.parse(NotUnderstoodPhrases.serialize(entries)))
    assertTrue(NotUnderstoodPhrases.parse("not json").isEmpty())
    assertTrue(NotUnderstoodPhrases.parse(null).isEmpty())
    val parsed =
      NotUnderstoodPhrases.parse(
        """[{"key":"a","phrase":"A","count":2,"last":1},{"key":"a","phrase":"A again","count":5,"last":2},""" +
          """{"key":"","phrase":"B","count":1,"last":1},{"key":"c","phrase":"C","count":0,"last":1},5]"""
      )
    assertEquals(listOf(NotUnderstoodPhrases.Entry("a", "A", 2, 1)), parsed)
  }

  @Test
  fun recordsListsAndForgetsInItsFile() {
    repeat(2) { assertFalse(NotUnderstoodPhrases.record(context, "read my mail", locale)) }
    assertTrue(NotUnderstoodPhrases.listed(context).isEmpty())
    assertTrue(NotUnderstoodPhrases.record(context, "Read my mail.", locale))
    assertEquals(listOf("Read my mail."), NotUnderstoodPhrases.listed(context).map { it.phrase })
    // Nothing is in the default file, which settings files and profiles hold.
    assertFalse(SharedPreferencesUtils.getSharedPreferences(context).all.keys.any { it.contains("phrase") })

    NotUnderstoodPhrases.remove(context, "read my mail")
    assertTrue(NotUnderstoodPhrases.listed(context).isEmpty())
  }

  @Test
  fun offCountsNothing() {
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putBoolean(context.getString(R.string.pref_voice_phrases_not_understood_key), false)
      .commit()
    assertFalse(NotUnderstoodPhrases.isOn(context))
    repeat(3) { assertFalse(NotUnderstoodPhrases.record(context, "read my mail", locale)) }
    assertTrue(SharedPreferencesUtils.getSharedPreferences(context, NotUnderstoodPhrases.PREFS).all.isEmpty())
  }
}
