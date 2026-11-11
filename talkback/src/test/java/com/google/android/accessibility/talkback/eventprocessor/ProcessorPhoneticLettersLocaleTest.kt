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

package com.google.android.accessibility.talkback.eventprocessor

import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests for [ProcessorPhoneticLetters.normalizePhoneticLocale]. */
class ProcessorPhoneticLettersLocaleTest {

  @Test
  fun simplifiedVariantsResolveToZhCn() {
    assertEquals("zh-CN", ProcessorPhoneticLetters.normalizePhoneticLocale("zh-Hans-CN"))
    assertEquals("zh-CN", ProcessorPhoneticLetters.normalizePhoneticLocale("zh-Hans-SG"))
    assertEquals("zh-CN", ProcessorPhoneticLetters.normalizePhoneticLocale("zh-Hans"))
    assertEquals("zh-CN", ProcessorPhoneticLetters.normalizePhoneticLocale("zh-CN"))
    assertEquals("zh-CN", ProcessorPhoneticLetters.normalizePhoneticLocale("zh-SG"))
  }

  @Test
  fun traditionalVariantsResolveToZhTw() {
    assertEquals("zh-TW", ProcessorPhoneticLetters.normalizePhoneticLocale("zh-Hant-TW"))
    assertEquals("zh-TW", ProcessorPhoneticLetters.normalizePhoneticLocale("zh-Hant-HK"))
    assertEquals("zh-TW", ProcessorPhoneticLetters.normalizePhoneticLocale("zh-Hant-MO"))
    assertEquals("zh-TW", ProcessorPhoneticLetters.normalizePhoneticLocale("zh-Hant"))
    assertEquals("zh-TW", ProcessorPhoneticLetters.normalizePhoneticLocale("zh-TW"))
    assertEquals("zh-TW", ProcessorPhoneticLetters.normalizePhoneticLocale("zh-HK"))
    assertEquals("zh-TW", ProcessorPhoneticLetters.normalizePhoneticLocale("zh-MO"))
  }

  @Test
  fun scriptTakesPriorityOverRegion() {
    // A user reading simplified characters in a traditionally traditional region should get
    // simplified descriptions, and vice versa.
    assertEquals("zh-CN", ProcessorPhoneticLetters.normalizePhoneticLocale("zh-Hans-MO"))
    assertEquals("zh-CN", ProcessorPhoneticLetters.normalizePhoneticLocale("zh-Hans-TW"))
    assertEquals("zh-TW", ProcessorPhoneticLetters.normalizePhoneticLocale("zh-Hant-CN"))
    assertEquals("zh-TW", ProcessorPhoneticLetters.normalizePhoneticLocale("zh-Hant-SG"))
  }

  @Test
  fun bareAndUnrecognizedChineseFallBackToZh() {
    assertEquals("zh", ProcessorPhoneticLetters.normalizePhoneticLocale("zh"))
    assertEquals("zh", ProcessorPhoneticLetters.normalizePhoneticLocale("zh-min-nan"))
  }

  @Test
  fun caseInsensitiveSubtagsResolveToCanonicalNodes() {
    assertEquals("zh-CN", ProcessorPhoneticLetters.normalizePhoneticLocale("zh-hans-cn"))
    assertEquals("zh-TW", ProcessorPhoneticLetters.normalizePhoneticLocale("ZH-HANT-HK"))
    assertEquals("zh-CN", ProcessorPhoneticLetters.normalizePhoneticLocale("zh-cn"))
  }

  @Test
  fun extensionAndPrivateUseSubtagsDoNotAffectResolution() {
    // Everything after a singleton subtag (x-, u-) is extension or private-use data and must not
    // be mistaken for a script or region.
    assertEquals("zh-CN", ProcessorPhoneticLetters.normalizePhoneticLocale("zh-CN-x-foo"))
    assertEquals("zh", ProcessorPhoneticLetters.normalizePhoneticLocale("zh-x-Hans"))
    assertEquals("zh", ProcessorPhoneticLetters.normalizePhoneticLocale("zh-u-ca-chinese"))
    assertEquals("zh-CN", ProcessorPhoneticLetters.normalizePhoneticLocale("zh-Hans-x-foo"))
  }

  @Test
  fun nonChineseLocalesAreUnchanged() {
    assertEquals("en-US", ProcessorPhoneticLetters.normalizePhoneticLocale("en-US"))
    assertEquals("ja-JP", ProcessorPhoneticLetters.normalizePhoneticLocale("ja-JP"))
    assertEquals("fr", ProcessorPhoneticLetters.normalizePhoneticLocale("fr"))
    assertEquals("yue-Hant-HK", ProcessorPhoneticLetters.normalizePhoneticLocale("yue-Hant-HK"))
  }
}
