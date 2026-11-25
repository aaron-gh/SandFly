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
import com.google.android.accessibility.braille.common.translate.BrailleLanguages
import com.google.android.accessibility.braille.common.translate.BrailleLanguages.Code
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Unicode braille is a typing table only: braille displays never show text in it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BrailleUserPreferencesOutputCodesTest {
  private val context: Context = RuntimeEnvironment.getApplication()

  @Test
  fun outputCodesLeaveOutUnicodeBraille() {
    BrailleUserPreferences.writePreferredCodes(
      context,
      listOf(Code.UNICODE_BRAILLE, Code.UEB, Code.GERMAN, Code.UNICODE_BRAILLE_8),
    )
    assertEquals(
      setOf(Code.UEB, Code.GERMAN),
      BrailleUserPreferences.readAvailablePreferredOutputCodes(context).toSet(),
    )
    assertEquals(4, BrailleUserPreferences.readAvailablePreferredCodes(context).size)
  }

  @Test
  fun nextOutputCodeSkipsUnicodeBraille() {
    BrailleUserPreferences.writePreferredCodes(
      context,
      listOf(Code.UNICODE_BRAILLE, Code.UEB, Code.GERMAN),
    )
    BrailleUserPreferences.writeCurrentActiveOutputCode(context, Code.UEB)
    repeat(4) {
      val next = BrailleUserPreferences.getNextOutputCode(context)
      assertFalse(next.isInputOnly)
      BrailleUserPreferences.writeCurrentActiveOutputCode(context, next)
    }
  }

  @Test
  fun unicodeBrailleAsOutputCodeIsCorrected() {
    BrailleUserPreferences.writePreferredCodes(context, listOf(Code.UNICODE_BRAILLE, Code.GERMAN))
    BrailleUserPreferences.writeCurrentActiveOutputCode(context, Code.UNICODE_BRAILLE)
    assertEquals(Code.GERMAN, BrailleUserPreferences.readCurrentActiveOutputCodeAndCorrect(context))
  }

  @Test
  fun onlyUnicodeBrailleReadsInTheDefaultCode() {
    BrailleUserPreferences.writePreferredCodes(context, listOf(Code.UNICODE_BRAILLE))
    assertEquals(
      BrailleLanguages.getDefaultCode(context),
      BrailleUserPreferences.readCurrentActiveOutputCodeAndCorrect(context),
    )
    assertEquals(
      Code.UNICODE_BRAILLE,
      BrailleUserPreferences.readCurrentActiveInputCodeAndCorrect(context),
    )
  }

  @Test
  fun nextInputCodeIncludesUnicodeBraille() {
    BrailleUserPreferences.writePreferredCodes(context, listOf(Code.UNICODE_BRAILLE, Code.GERMAN))
    BrailleUserPreferences.writeCurrentActiveInputCode(context, Code.GERMAN)
    assertEquals(Code.UNICODE_BRAILLE, BrailleUserPreferences.getNextInputCode(context))
  }
}
