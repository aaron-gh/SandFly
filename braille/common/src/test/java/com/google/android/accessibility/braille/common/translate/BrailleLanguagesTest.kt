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
import androidx.test.core.app.ApplicationProvider
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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BrailleLanguagesTest {
  private val context: Context = ApplicationProvider.getApplicationContext()

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
}
