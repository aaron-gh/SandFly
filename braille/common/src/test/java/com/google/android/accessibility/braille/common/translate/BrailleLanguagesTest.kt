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

package com.google.android.accessibility.braille.common.translate

import android.content.Context
import com.google.android.accessibility.braille.common.R
import com.google.android.accessibility.braille.common.translate.BrailleLanguages.Code
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BrailleLanguagesTest {
  private val context: Context = RuntimeEnvironment.getApplication()

  @Test
  fun persianCodeConfiguration() {
    val persian = Code.valueOf("PERSIAN")
    assertNotNull(persian)
    assertEquals("fa", persian.locale.language)
    assertFalse(persian.isSupportsContracted(context))
    assertFalse(persian.isEightDot)
    assertTrue(persian.isAvailable(context))
    assertEquals("Persian", persian.getUserFacingName(context))
  }

  @Test
  fun persianComp8CodeConfiguration() {
    val persianComp8 = Code.valueOf("PERSIAN_COMP8")
    assertNotNull(persianComp8)
    assertEquals("fa", persianComp8.locale.language)
    assertFalse(persianComp8.isSupportsContracted(context))
    assertTrue(persianComp8.isEightDot)
    assertTrue(persianComp8.isAvailable(context))
    assertEquals("Persian 8-dot Computer Braille", persianComp8.getUserFacingName(context))
  }

  @Test
  fun defaultCodeForPersianLocale() {
    val originalLocale = Locale.getDefault()
    try {
      Locale.setDefault(Locale.forLanguageTag("fa"))
      assertEquals(Code.PERSIAN, BrailleLanguages.getDefaultCode(context))

      Locale.setDefault(Locale.forLanguageTag("fa-IR"))
      assertEquals(Code.PERSIAN, BrailleLanguages.getDefaultCode(context))
    } finally {
      Locale.setDefault(originalLocale)
    }
  }

  @Test
  fun romanianCodeConfiguration() {
    val romanian = Code.valueOf("ROMANIAN")
    assertEquals("ro", romanian.locale.language)
    assertFalse(romanian.isSupportsContracted(context))
    assertFalse(romanian.isEightDot)
    assertTrue(romanian.isAvailable(context))
    assertEquals("Romanian", romanian.getUserFacingName(context))
  }

  @Test
  fun defaultCodeForRomanianLocale() {
    val originalLocale = Locale.getDefault()
    try {
      Locale.setDefault(Locale.forLanguageTag("ro"))
      assertEquals(Code.ROMANIAN, BrailleLanguages.getDefaultCode(context))

      Locale.setDefault(Locale.forLanguageTag("ro-RO"))
      assertEquals(Code.ROMANIAN, BrailleLanguages.getDefaultCode(context))
    } finally {
      Locale.setDefault(originalLocale)
    }
  }

  @Test
  fun indonesianCodeConfiguration() {
    val indonesian = Code.valueOf("INDONESIAN")
    assertEquals(Locale.forLanguageTag("id").language, indonesian.locale.language)
    assertTrue(indonesian.isSupportsContracted(context))
    assertFalse(indonesian.isEightDot)
    assertTrue(indonesian.isAvailable(context))
    assertEquals("Indonesian", indonesian.getUserFacingName(context))
  }

  @Test
  fun defaultCodeForIndonesianLocale() {
    val originalLocale = Locale.getDefault()
    try {
      Locale.setDefault(Locale.forLanguageTag("id"))
      assertEquals(Code.INDONESIAN, BrailleLanguages.getDefaultCode(context))

      Locale.setDefault(Locale.forLanguageTag("id-ID"))
      assertEquals(Code.INDONESIAN, BrailleLanguages.getDefaultCode(context))
    } finally {
      Locale.setDefault(originalLocale)
    }
  }

  @Test
  fun unicodeBrailleCodeConfiguration() {
    val unicode = Code.UNICODE_BRAILLE
    assertTrue(unicode.hasNoLanguage())
    assertTrue(unicode.isInputOnly)
    assertFalse(unicode.isSupportsContracted(context))
    assertFalse(unicode.isEightDot)
    assertTrue(unicode.isAvailable(context))
    assertEquals("Unicode Braille", unicode.getUserFacingName(context))

    val unicode8 = Code.UNICODE_BRAILLE_8
    assertTrue(unicode8.hasNoLanguage())
    assertTrue(unicode8.isInputOnly)
    assertFalse(unicode8.isSupportsContracted(context))
    assertTrue(unicode8.isEightDot)
    assertEquals("Unicode 8-dot Braille", unicode8.getUserFacingName(context))
  }

  @Test
  fun onlyUnicodeBrailleIsInputOnlyOrOfNoLanguage() {
    val unicode = listOf(Code.UNICODE_BRAILLE, Code.UNICODE_BRAILLE_8)
    assertEquals(unicode, Code.values().filter { it.isInputOnly })
    assertEquals(unicode, Code.values().filter { it.hasNoLanguage() })
  }

  @Test
  fun defaultCodeIsNeverInputOnly() {
    val originalLocale = Locale.getDefault()
    try {
      Locale.setDefault(Locale.ROOT)
      assertFalse(BrailleLanguages.getDefaultCode(context).isInputOnly)
    } finally {
      Locale.setDefault(originalLocale)
    }
  }

  @Test
  fun afrikaansCodeConfiguration() {
    val afrikaans = Code.valueOf("AFRIKAANS")
    assertEquals("af", afrikaans.locale.language)
    assertTrue(afrikaans.isSupportsContracted(context))
    assertFalse(afrikaans.isEightDot)
    assertTrue(afrikaans.isAvailable(context))
    assertEquals("Afrikaans", afrikaans.getUserFacingName(context))
  }

  @Test
  fun defaultCodeForAfrikaansLocale() {
    val originalLocale = Locale.getDefault()
    try {
      Locale.setDefault(Locale.forLanguageTag("af-ZA"))
      assertEquals(Code.AFRIKAANS, BrailleLanguages.getDefaultCode(context))
    } finally {
      Locale.setDefault(originalLocale)
    }
  }
}
