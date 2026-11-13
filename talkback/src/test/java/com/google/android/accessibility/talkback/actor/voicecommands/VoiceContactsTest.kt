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

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoiceContactsTest {
  private val numbers =
    listOf(
      VoiceContacts.Number(1, "Sam Jones", "01", mobile = false, primary = false),
      VoiceContacts.Number(1, "Sam Jones", "07", mobile = true, primary = false),
      VoiceContacts.Number(2, "Sam Smith", "02", mobile = true, primary = false),
      VoiceContacts.Number(3, "Mum", "03", mobile = false, primary = true),
      VoiceContacts.Number(4, "Mum", "04", mobile = true, primary = false),
      VoiceContacts.Number(5, "Jo", "05", mobile = true, primary = false),
    )

  @Test
  fun wholeNameFirst() {
    assertEquals("07", VoiceContacts.find(numbers, "sam jones", Locale.ENGLISH).number?.number)
    assertEquals("05", VoiceContacts.find(numbers, "Jo", Locale.ENGLISH).number?.number)
  }

  @Test
  fun severalMatches() {
    val found = VoiceContacts.find(numbers, "Sam", Locale.ENGLISH)
    assertNull(found.number)
    assertEquals(listOf("Sam Jones", "Sam Smith"), found.names)
  }

  @Test
  fun sameNameIsOnePerson() {
    // Two contacts named Mum, as from an account and the phone, are one; the default number wins.
    assertEquals("03", VoiceContacts.find(numbers, "mum", Locale.ENGLISH).number?.number)
  }

  @Test
  fun nameAtStart() {
    val (found, rest) = VoiceContacts.findAtStart(numbers, "Sam Smith I'm late", Locale.ENGLISH)!!
    assertEquals("02", found.number?.number)
    assertEquals("I'm late", rest)
    assertEquals("are you up", VoiceContacts.findAtStart(numbers, "Jo are you up", Locale.ENGLISH)!!.second)
    assertNull(VoiceContacts.findAtStart(numbers, "nobody here", Locale.ENGLISH))
  }

  @Test
  fun numbers() {
    assertTrue(VoiceContacts.isNumber("+44 7700 900123"))
    assertTrue(VoiceContacts.isNumber("999"))
    assertFalse(VoiceContacts.isNumber("Sam"))
  }

  @Test
  fun customCommandsRoundTrip() {
    val entries = listOf(CustomVoiceCommands.Entry("a", "Good morning", "MACRO:1"))
    assertEquals(entries, CustomVoiceCommands.parse(CustomVoiceCommands.serialize(entries)))
    assertEquals(emptyList<CustomVoiceCommands.Entry>(), CustomVoiceCommands.parse("not json"))
    assertEquals(entries[0], CustomVoiceCommands.find(entries, "good morning!", Locale.ENGLISH))
  }
}
