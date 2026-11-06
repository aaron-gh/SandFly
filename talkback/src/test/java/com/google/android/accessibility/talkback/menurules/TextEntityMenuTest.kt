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
}
