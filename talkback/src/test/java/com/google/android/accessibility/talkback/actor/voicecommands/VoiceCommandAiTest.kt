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

package com.google.android.accessibility.talkback.actor.voicecommands

import com.google.android.accessibility.talkback.commands.Commands
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoiceCommandAiTest {
  private val commands = mapOf("HOME" to Commands.Command("HOME", "Home", keyboardAction = null))

  @Test
  fun readsSteps() {
    val answer =
      """
      Here you go:
      {"steps":[
        {"type":"command","value":"HOME"},
        {"type":"command","value":"EXIT"},
        {"type":"open_app","name":"WhatsApp"},
        {"type":"tap","label":"Sam"},
        {"type":"describe","target":"image"},
        {"type":"message","contact":"Sam"},
        {"type":"message","contact":"Jo","text":"Hi","app":"WhatsApp"},
        {"type":"wait","seconds":500},
        {"type":"copy"},
        {"type":"unknown"}
      ]}
      """
    assertEquals(
      listOf(
        VoiceStep.Command("HOME", "Home"),
        VoiceStep.OpenApp("WhatsApp"),
        VoiceStep.Tap("Sam"),
        VoiceStep.Describe(screen = false),
        VoiceStep.Message(who = "Sam", usePrevious = true),
        VoiceStep.Message(who = "Jo", text = "Hi", app = "WhatsApp"),
        VoiceStep.Wait(60_000),
        VoiceStep.Copy,
      ),
      VoiceCommandAi.parse(answer, commands),
    )
  }

  @Test
  fun notJson() {
    assertNull(VoiceCommandAi.parse("I can't do that.", commands))
    assertNull(VoiceCommandAi.parse("{\"steps\": [", commands))
  }

  @Test
  fun promptLeavesOutOpenApp() {
    val categories =
      listOf(
        Commands.Category(1, listOf(Commands.Command("HOME", "Home", null))),
      )
    val prompt = VoiceCommandAi.prompt("go \"home\"", categories, listOf("Settings"), Locale.ENGLISH.displayLanguage)
    assertTrue(prompt.contains("HOME: Home"))
    assertTrue(prompt.contains("Settings"))
    assertTrue(prompt.contains("Spoken command: \"go \\\"home\\\"\""))
  }
}
