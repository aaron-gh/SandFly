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

import android.content.Context
import com.google.android.accessibility.talkback.notifications.Replacement
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BrailleDictionaryTest {
  private val context = RuntimeEnvironment.getApplication()
  private val prefs = context.getSharedPreferences("braille-dictionary", Context.MODE_PRIVATE)

  @Test
  fun brailleEntriesAreKeptApartFromSpeechEntries() {
    BrailleDictionary.write(prefs, listOf(DictionaryEntry(Replacement("&", "and"))))
    SpeechDictionary.write(prefs, listOf(DictionaryEntry(Replacement("SF", "SandFly"))))
    prefs.edit().commit()

    assertEquals(listOf("&"), BrailleDictionary.read(prefs).map { it.replacement.find })
    assertEquals(listOf("SF"), SpeechDictionary.read(prefs).map { it.replacement.find })
    SpeechDictionary.onWindowShown("com.example")
    assertEquals(
      "cats and dogs",
      SpeechDictionary.applyInCurrentApp("cats & dogs", BrailleDictionary.read(prefs)).toString(),
    )
  }
}
