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

package com.google.android.accessibility.talkback.actor.gemini

import com.google.android.accessibility.talkback.directtouch.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GeminiApiKeyTest {
  @Test
  fun noKeyByDefault() {
    val prefs = FakeSharedPreferences()
    assertEquals("", GeminiApiKey.userKey(prefs))
    assertEquals("", GeminiApiKey.effectiveKey(prefs, builtInKey = ""))
  }

  @Test
  fun builtInKeyIsUsedWithoutAUserKey() {
    assertEquals("built-in", GeminiApiKey.effectiveKey(FakeSharedPreferences(), "built-in"))
  }

  @Test
  fun userKeyWinsOverBuiltInKey() {
    val prefs = FakeSharedPreferences()
    GeminiApiKey.setUserKey(prefs, "mine")
    assertEquals("mine", GeminiApiKey.effectiveKey(prefs, "built-in"))
  }

  @Test
  fun pastedKeyIsTrimmed() {
    val prefs = FakeSharedPreferences()
    GeminiApiKey.setUserKey(prefs, "  mine\n")
    assertEquals("mine", GeminiApiKey.userKey(prefs))
  }

  @Test
  fun blankKeyRemovesTheUserKey() {
    val prefs = FakeSharedPreferences()
    GeminiApiKey.setUserKey(prefs, "mine")
    GeminiApiKey.setUserKey(prefs, "   ")
    assertFalse(prefs.contains(GeminiApiKey.PREF_KEY))
    assertEquals("built-in", GeminiApiKey.effectiveKey(prefs, "built-in"))
  }
}
