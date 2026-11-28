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

package com.google.android.accessibility.braille.common

import android.content.Context
import com.google.android.accessibility.braille.common.translate.BrailleLanguages.Code
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BrailleUserPreferencesSecondaryOutputCodeTest {
  private val context: Context = RuntimeEnvironment.getApplication()

  @Test
  fun noneByDefault() {
    BrailleUserPreferences.writePreferredCodes(context, listOf(Code.UEB, Code.RUSSIAN))
    assertNull(BrailleUserPreferences.readSecondaryOutputCode(context))
  }

  @Test
  fun readsAPreferredTable() {
    BrailleUserPreferences.writePreferredCodes(context, listOf(Code.UEB, Code.RUSSIAN))
    BrailleUserPreferences.writeCurrentActiveOutputCode(context, Code.UEB)
    BrailleUserPreferences.writeSecondaryOutputCode(context, Code.RUSSIAN)
    assertEquals(Code.RUSSIAN, BrailleUserPreferences.readSecondaryOutputCode(context))
    BrailleUserPreferences.writeSecondaryOutputCode(context, null)
    assertNull(BrailleUserPreferences.readSecondaryOutputCode(context))
  }

  @Test
  fun noneWhileItIsTheOutputTable() {
    BrailleUserPreferences.writePreferredCodes(context, listOf(Code.UEB, Code.RUSSIAN))
    BrailleUserPreferences.writeSecondaryOutputCode(context, Code.RUSSIAN)
    BrailleUserPreferences.writeCurrentActiveOutputCode(context, Code.RUSSIAN)
    assertNull(BrailleUserPreferences.readSecondaryOutputCode(context))
    // Kept for when the output table changes back.
    BrailleUserPreferences.writeCurrentActiveOutputCode(context, Code.UEB)
    assertEquals(Code.RUSSIAN, BrailleUserPreferences.readSecondaryOutputCode(context))
  }

  @Test
  fun noneWhenNoLongerPreferred() {
    BrailleUserPreferences.writePreferredCodes(context, listOf(Code.UEB, Code.RUSSIAN))
    BrailleUserPreferences.writeCurrentActiveOutputCode(context, Code.UEB)
    BrailleUserPreferences.writeSecondaryOutputCode(context, Code.RUSSIAN)
    BrailleUserPreferences.writePreferredCodes(context, listOf(Code.UEB))
    assertNull(BrailleUserPreferences.readSecondaryOutputCode(context))
  }

  @Test
  fun typingOnlyTableIsNotRead() {
    BrailleUserPreferences.writePreferredCodes(context, listOf(Code.UEB, Code.UNICODE_BRAILLE))
    BrailleUserPreferences.writeCurrentActiveOutputCode(context, Code.UEB)
    BrailleUserPreferences.writeSecondaryOutputCode(context, Code.UNICODE_BRAILLE)
    assertNull(BrailleUserPreferences.readSecondaryOutputCode(context))
  }
}
