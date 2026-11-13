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

import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** Reads commands with the English patterns of strings_voice_commands_sandfly.xml. */
class VoiceGrammarTest {

  private val commands =
    mapOf(
      "home" to "HOME",
      "back" to "BACK",
      "select all" to "SELECT_ALL",
      "copy last spoken phrase" to "COPY_LAST_SPOKEN_UTTERANCE",
      "open whatsapp" to "OPEN_APP:com.whatsapp/.Main",
      "morning" to "MACRO:1",
      "next heading" to "NEXT_HEADING",
    )

  private val apps = mapOf("whatsapp" to "OPEN_APP:com.whatsapp/.Main", "chrome" to "OPEN_APP:chrome")

  private val resolver =
    object : VoiceGrammar.Resolver {
      override fun custom(normalized: String): VoiceStep.Command? =
        if (normalized == "good morning") VoiceStep.Command("MACRO:1", "Morning") else null

      override fun command(normalized: String): VoiceStep.Command? =
        commands[normalized]?.let { VoiceStep.Command(it, normalized) }

      override fun app(name: String): VoiceStep.OpenApp? =
        apps[VoiceGrammar.normalize(name, Locale.ENGLISH)]?.let { VoiceStep.OpenApp(name, it) }
    }

  private val grammar = VoiceGrammar(englishPhrases(), resolver, Locale.ENGLISH)

  private fun steps(said: String): List<VoiceStep> {
    val result = grammar.parse(said)
    assertTrue("Not understood: $said (${result.unparsed})", result.complete)
    return result.steps
  }

  @Test
  fun commandNames() {
    assertEquals(listOf(VoiceStep.Command("HOME", "home")), steps("Home."))
    assertEquals(listOf(VoiceStep.Command("NEXT_HEADING", "next heading")), steps("please next heading"))
    assertEquals(listOf(VoiceStep.Command("MACRO:1", "Morning")), steps("Good morning!"))
    assertEquals(listOf(VoiceStep.Command("MACRO:1", "morning")), steps("run the morning macro"))
  }

  @Test
  fun chains() {
    assertEquals(
      listOf(VoiceStep.Command("HOME", "home"), VoiceStep.OpenApp("Chrome", "OPEN_APP:chrome")),
      steps("Home, then open Chrome"),
    )
    assertEquals(
      listOf(VoiceStep.OpenApp("Chrome", "OPEN_APP:chrome"), VoiceStep.Call("Sam")),
      steps("Could you open Chrome and call Sam"),
    )
    assertEquals(
      listOf(VoiceStep.Tap("Settings"), VoiceStep.Wait(2000), VoiceStep.Tap("Display")),
      steps("Tap Settings, wait 2 seconds, then tap Display."),
    )
  }

  @Test
  fun messages() {
    assertEquals(
      listOf(VoiceStep.Message(who = "Sam", text = "I'm on my way!")),
      steps("Send a message to Sam saying I'm on my way!"),
    )
    assertEquals(
      listOf(VoiceStep.Message(who = "Sam Jones", text = "Are you home?", app = "WhatsApp")),
      steps("send a WhatsApp message to Sam Jones saying Are you home?"),
    )
    assertEquals(listOf(VoiceStep.Message(whoAndText = "Sam I'm late")), steps("Text Sam I'm late."))
    // "and" in the text of a message doesn't join commands.
    assertEquals(
      listOf(VoiceStep.Message(who = "Sam", text = "buy salt and pepper")),
      steps("message Sam saying buy salt and pepper"),
    )
  }

  @Test
  fun descriptions() {
    assertEquals(listOf(VoiceStep.Describe(screen = true)), steps("What's on the screen?"))
    assertEquals(
      listOf(VoiceStep.Describe(screen = false), VoiceStep.Message(who = "Sam", usePrevious = true)),
      steps("Send the image description to Sam"),
    )
    assertEquals(
      listOf(VoiceStep.Describe(screen = false), VoiceStep.Message(who = "Sam", usePrevious = true)),
      steps("Describe this image and send it to Sam"),
    )
    assertEquals(listOf(VoiceStep.Describe(screen = true), VoiceStep.Copy), steps("describe the screen then copy it"))
    assertEquals(listOf(VoiceStep.Copy), steps("Copy what you just said"))
  }

  @Test
  fun commandNamesComeBeforePatterns() {
    assertEquals(listOf(VoiceStep.Command("SELECT_ALL", "select all")), steps("Select all"))
    assertEquals(listOf(VoiceStep.Command("OPEN_APP:com.whatsapp/.Main", "open whatsapp")), steps("Open WhatsApp"))
  }

  @Test
  fun typeKeepsCase() {
    assertEquals(listOf(VoiceStep.TypeText("Hello Sam")), steps("Type Hello Sam"))
  }

  @Test
  fun notUnderstood() {
    val result = grammar.parse("label the button cancel")
    assertFalse(result.complete)
    assertEquals(1, result.parts)
    // An app that isn't installed isn't opened or tapped.
    assertFalse(grammar.parse("open the notifications").complete)
    val chain = grammar.parse("open Chrome then fly to the moon")
    assertFalse(chain.complete)
    assertEquals(listOf("fly to the moon"), chain.unparsed)
    assertEquals(2, chain.parts)
  }

  @Test
  fun cancel() {
    assertTrue(grammar.parse("Never mind.").cancel)
  }

  @Test
  fun normalize() {
    assertEquals("whats on the screen", VoiceGrammar.normalize("What's on the  screen?", Locale.ENGLISH))
    assertEquals("Hi there?", VoiceGrammar.trimText(" , Hi there?. "))
  }

  companion object {
    private fun englishPhrases(): VoiceGrammar.Phrases {
      val file =
        listOf("src/main/res/values/strings_voice_commands_sandfly.xml", "talkback/src/main/res/values/strings_voice_commands_sandfly.xml")
          .map(::File)
          .first { it.exists() }
      val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
      val arrays = HashMap<String, List<String>>()
      val nodes = document.getElementsByTagName("string-array")
      for (i in 0 until nodes.length) {
        val array = nodes.item(i) as Element
        val items = array.getElementsByTagName("item")
        arrays[array.getAttribute("name")] =
          (0 until items.length).map { items.item(it).textContent.replace("\\'", "'") }
      }
      fun array(name: String) = arrays.getValue("voice_commands_$name")
      return VoiceGrammar.Phrases(
        chain = array("chain_words"),
        and = array("and_words"),
        fillersStart = array("fillers_start"),
        fillersEnd = array("fillers_end"),
        cancel = array("cancel_words"),
        sendImageDescription = array("send_image_description"),
        sendScreenDescription = array("send_screen_description"),
        sendPrevious = array("send_previous"),
        message = array("message"),
        call = array("call"),
        describeScreen = array("describe_screen"),
        describeImage = array("describe_image"),
        copy = array("copy_result"),
        wait = array("wait"),
        type = array("type_text"),
        run = array("run"),
        open = array("open"),
        tap = array("tap"),
      )
    }
  }
}
