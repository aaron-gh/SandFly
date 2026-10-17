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

package com.google.android.accessibility.talkback.scripting

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppMatcherTest {
  private val chat = AppMatcher("com.example.chat", "Conversation", null)

  @Test
  fun theNamedActivityMatches() {
    assertTrue(chat.matchesFront("com.example.chat", "com.example.chat.Conversation", null))
  }

  @Test
  fun anotherActivityDoesNotMatch() {
    assertFalse(chat.matchesFront("com.example.chat", "com.example.chat.Settings", null))
  }

  @Test
  fun anActivityNotKnownYetMatches() {
    assertTrue(chat.matchesFront("com.example.chat", null, null))
  }

  @Test
  fun anotherAppDoesNotMatch() {
    assertFalse(chat.matchesFront("com.example.other", null, null))
  }
}
