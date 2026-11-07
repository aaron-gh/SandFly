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

import android.view.textclassifier.TextClassifier
import com.google.android.accessibility.talkback.menurules.TextEntityMenu.Classified
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
  /** What the classifier would find: [parts] of [text], each with its type. */
  private fun classified(text: String, vararg parts: Pair<String, String>): List<Classified> =
    parts.map { (part, type) ->
      val start = text.indexOf(part)
      Classified(start, start + part.length, type)
    }

  @Test
  fun aOneTimeCodeIsOffered() {
    val text = "Your verification code is 482916. Don't share it."
    assertEquals(listOf<Entity>(Entity.Code("482916")), TextEntityMenu.find(text, classified = listOf()))
  }

  @Test
  fun aCodeThatLooksLikeAYearIsOffered() {
    assertEquals(
      listOf<Entity>(Entity.Code("2026")),
      TextEntityMenu.find("Your code is 2026. Don't share it.", classified = listOf()),
    )
  }

  @Test
  fun digitsTheClassifierFoundAreNoCodes() {
    val date = "Sat, 7 Nov 2026"
    assertEquals(
      emptyList<Entity>(),
      TextEntityMenu.find(date, classified = classified(date, date to TextClassifier.TYPE_DATE_TIME)),
    )
    val text = "Meet at 1600 Main Street on 12/11/2026, code 4821"
    assertEquals(
      listOf<Entity>(Entity.Code("4821")),
      TextEntityMenu.find(
        text,
        classified =
          classified(
            text,
            "1600 Main Street" to TextClassifier.TYPE_ADDRESS,
            "12/11/2026" to TextClassifier.TYPE_DATE,
          ),
      ),
    )
  }

  @Test
  fun phoneNumbersComeFromTheClassifier() {
    val text = "Ring +44 (0)20 7946 0018."
    assertEquals(
      listOf<Entity>(Entity.Phone("+44 (0)20 7946 0018", "+442079460018")),
      TextEntityMenu.find(
        text,
        classified = classified(text, "+44 (0)20 7946 0018" to TextClassifier.TYPE_PHONE),
      ),
    )
  }

  @Test
  fun withoutTheClassifierOnlyAddressesAreOffered() {
    val found = TextEntityMenu.find("Code 482916, see https://example.org/a", classified = null)
    assertEquals(listOf<Entity>(Entity.Web("https://example.org/a", "https://example.org/a")), found)
  }

  @Test
  fun onlyIncludedTypesAreOffered() {
    val text = "Call 020 7946 0018 or write to help@example.org, code 482916"
    val classified = classified(text, "020 7946 0018" to TextClassifier.TYPE_PHONE)
    assertEquals(
      listOf<Entity>(Entity.Code("482916")),
      TextEntityMenu.find(text, classified = classified, included = setOf(TextEntityMenu.TYPE_CODE)),
    )
    assertEquals(
      listOf<Entity>(Entity.Email("help@example.org", "help@example.org")),
      TextEntityMenu.find(text, classified = classified, included = setOf(TextEntityMenu.TYPE_EMAIL)),
    )
    assertTrue(TextEntityMenu.find(text, classified = classified, included = emptySet()).isEmpty())
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
  fun digitsJoinedToOtherTextAreNoCodes() {
    for (text in
      listOf(
        "u/Ok_Fact_2026 replied",
        "u/Some-Name-5551234 replied",
        "@friend1234 posted",
        "r/2026 is trending",
        "#1234",
        "Order A12345B, card 1234567890123",
        "Paid \$1234.56",
        "At 10:45",
        "© 2026",
        "©1999–2026",
      )) {
      assertEquals(text, emptyList<Entity>(), TextEntityMenu.find(text, classified = listOf()))
    }
  }

  @Test
  fun aCodeAfterAPrefixOrNextToOtherNumbersIsOffered() {
    assertEquals(
      listOf<Entity>(Entity.Code("482916")),
      TextEntityMenu.find("G-482916 is your code. It expires in 10 minutes.", classified = listOf()),
    )
    assertEquals(
      listOf<Entity>(Entity.Code("482916")),
      TextEntityMenu.find("Code:482916 10 min", classified = listOf()),
    )
  }
}
