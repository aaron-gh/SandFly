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

package com.google.android.accessibility.talkback.handwriting

import com.google.android.accessibility.talkback.gesture.OpenAppActions
import org.junit.Assert.assertEquals
import org.junit.Test

class HandwritingAppLauncherTest {
  private val apps =
    listOf("Calculator", "Camera", "Google Maps", "Música", "WhatsApp", "WhatsApp Business", "Wallet")
      .map { OpenAppActions.App(it, "OPEN_APP:$it") }

  private fun matching(letters: String) = HandwritingAppLauncher.matching(apps, letters).map { it.label }

  @Test
  fun matchesTheStartOfTheName_ignoringCase() {
    assertEquals(listOf("WhatsApp", "WhatsApp Business"), matching("wha"))
    assertEquals(listOf("WhatsApp", "WhatsApp Business"), matching("WHA"))
  }

  @Test
  fun namesThatStartWithTheLetters_comeBeforeLaterWords() {
    assertEquals(listOf("Google Maps"), matching("ma"))
    assertEquals(listOf("Calculator", "Camera"), matching("ca"))
  }

  @Test
  fun spacesAndAccentsDontCount() {
    assertEquals(listOf("WhatsApp Business"), matching("whatsappb"))
    assertEquals(listOf("Música"), matching("musi"))
  }

  @Test
  fun noLetters_matchNothing() {
    assertEquals(emptyList<String>(), matching(""))
    assertEquals(emptyList<String>(), matching("xyz"))
  }
}
