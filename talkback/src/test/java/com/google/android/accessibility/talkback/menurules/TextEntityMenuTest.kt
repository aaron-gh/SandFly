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

package com.google.android.accessibility.talkback.menurules

import com.google.android.accessibility.talkback.menurules.TextEntityMenu.Entity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TextEntityMenuTest {
  @Test
  fun aOneTimeCodeIsOffered() {
    val found = TextEntityMenu.find("Your verification code is 482916. Don't share it.")
    assertEquals(listOf<Entity>(Entity.Code("482916")), found)
  }

  @Test
  fun linksAndAddressesAreOfferedOnce() {
    val found =
      TextEntityMenu.find("See https://example.org/a and write to help@example.org or https://example.org/a")
    assertTrue(found.any { it is Entity.Web && it.text == "https://example.org/a" })
    assertTrue(found.any { it is Entity.Email && it.address == "help@example.org" })
    assertEquals(2, found.size)
  }

  @Test
  fun linksTheItemAlreadyHasAreLeftToTheLinkMenu() {
    val found = TextEntityMenu.find("https://example.org/a", setOf("https://example.org/a"))
    assertTrue(found.isEmpty())
  }

  @Test
  fun digitsInsideWordsOrLongNumbersAreNoCodes() {
    assertTrue(TextEntityMenu.find("Order A12345B, card 1234567890123").none { it is Entity.Code })
  }

  @Test
  fun phoneNumbersAreOffered() {
    assertEquals(
      listOf<Entity>(Entity.Phone("020 7946 0018", "02079460018")),
      TextEntityMenu.find("Call 020 7946 0018 today"),
    )
    assertEquals(
      listOf<Entity>(Entity.Phone("+44 (0)20 7946 0018", "+442079460018")),
      TextEntityMenu.find("Ring +44 (0)20 7946 0018."),
    )
    assertEquals(
      listOf<Entity>(Entity.Phone("(555) 123-4567", "5551234567")),
      TextEntityMenu.find("Office: (555) 123-4567"),
    )
    assertEquals(
      listOf<Entity>(Entity.Phone("+33 1 23 45 67 89", "+33123456789")),
      TextEntityMenu.find("Tél +33 1 23 45 67 89"),
    )
    assertEquals(
      listOf<Entity>(Entity.Phone("07700 900123", "07700900123")),
      TextEntityMenu.find("Mobile 07700 900123"),
    )
    assertEquals(
      listOf<Entity>(Entity.Phone("5551234567", "5551234567")),
      TextEntityMenu.find("Text 5551234567"),
    )
  }

  @Test
  fun datesTimesAndYearsAreNoNumbersOrCodes() {
    for (text in
      listOf(
        "Sat, 7 Nov 2026",
        "7 November 2026, 10:45",
        "Updated 2026-11-07 10:45",
        "07 11 2026 10 45",
        "Due 07.11.2026",
        "Due 11/07/2026",
        "2020-2026",
        "Copyright 1999 2026",
      )) {
      assertEquals(text, emptyList<Entity>(), TextEntityMenu.find(text))
    }
  }

  @Test
  fun digitsInUserNamesAreNoNumbersOrCodes() {
    for (text in
      listOf(
        "u/Ok_Fact_2026 replied",
        "u/Some-Name-5551234 replied",
        "u/name5551234567 replied",
        "@friend1234 posted",
        "r/2026 is trending",
        "#1234",
      )) {
      assertEquals(text, emptyList<Entity>(), TextEntityMenu.find(text))
    }
  }

  @Test
  fun amountsAddressesAndDecimalsAreNoNumbers() {
    for (text in
      listOf(
        "Paid \$1234.56",
        "Total 12.345.678",
        "Server 192.168.10.20",
        "1234.567 km",
        "Order A12345B, card 1234567890123",
      )) {
      assertTrue(text, TextEntityMenu.find(text).none { it is Entity.Phone })
    }
  }

  @Test
  fun aCodeNextToOtherNumbersOrAfterAPrefixIsOffered() {
    assertEquals(
      listOf<Entity>(Entity.Code("482916")),
      TextEntityMenu.find("G-482916 is your code. It expires in 10 minutes."),
    )
    assertEquals(listOf<Entity>(Entity.Code("482916")), TextEntityMenu.find("Code:482916 10 min"))
  }
}
