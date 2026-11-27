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
  fun andJoinsAsManyCommandsAsUnderstood() {
    assertEquals(
      listOf(
        VoiceStep.Command("HOME", "home"),
        VoiceStep.OpenApp("Chrome", "OPEN_APP:chrome"),
        VoiceStep.Tap("Network and internet"),
      ),
      steps("Home then open Chrome and tap Network and internet"),
    )
    assertEquals(listOf(VoiceStep.Tap("Salt and pepper")), steps("tap Salt and pepper"))
    assertEquals(listOf(VoiceStep.Tap("A"), VoiceStep.Call("Sam")), steps("tap A and call Sam"))
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

  @Test
  fun languagesWithoutSpacesMatchWithoutSpaces() {
    val chinese =
      VoiceGrammar.Phrases(
        chain = listOf("然后"),
        and = listOf("和"),
        fillersStart = listOf("请"),
        fillersEnd = emptyList(),
        cancel = listOf("取消"),
        sendImageDescription = emptyList(),
        sendScreenDescription = emptyList(),
        sendPrevious = emptyList(),
        message = emptyList(),
        call = listOf("给 {who} 打电话", "打电话给 {who}"),
        describeScreen = emptyList(),
        describeImage = emptyList(),
        copy = emptyList(),
        wait = emptyList(),
        type = emptyList(),
        run = emptyList(),
        open = listOf("打开 {app}"),
        tap = listOf("点击 {label}"),
      )
    val grammar = VoiceGrammar(chinese, resolver, Locale.SIMPLIFIED_CHINESE)
    val result = grammar.parse("请打开Chrome然后给小明打电话")
    assertTrue(result.unparsed.toString(), result.complete)
    assertEquals(
      listOf(VoiceStep.OpenApp("Chrome", "OPEN_APP:chrome"), VoiceStep.Call("小明")),
      result.steps,
    )
    assertEquals(listOf(VoiceStep.Call("小明")), grammar.parse("打电话给 小明").steps)
  }

  @Test
  fun newLinePhrasesRunNewLineInsteadOfTapping() {
    val names =
      VoiceTargets.commandNames(
        listOf(Commands.Category(1, listOf(Commands.Command("NEW_LINE", "New line", keyboardAction = null)))),
        emptyList(),
        Locale.ENGLISH,
        mapOf("NEW_LINE" to englishArray("new_line")),
      )
    val newLine =
      VoiceGrammar(
        englishPhrases(),
        object : VoiceGrammar.Resolver {
          override fun custom(normalized: String): VoiceStep.Command? = null

          override fun command(normalized: String): VoiceStep.Command? =
            names[normalized]?.let { VoiceStep.Command(it.value, it.label) }

          override fun app(name: String): VoiceStep.OpenApp? = null
        },
        Locale.ENGLISH,
      )
    for (said in listOf("New line.", "new paragraph", "Press Enter", "please press enter")) {
      val result = newLine.parse(said)
      assertTrue(said, result.complete)
      assertEquals(said, listOf(VoiceStep.Command("NEW_LINE", "New line")), result.steps)
    }
    // Other items are still pressed by their labels.
    assertEquals(listOf(VoiceStep.Tap("OK")), newLine.parse("press OK").steps)
    // Typing the words keeps them as text.
    assertEquals(listOf(VoiceStep.TypeText("new line")), newLine.parse("type new line").steps)
  }

  @Test
  fun countedTextOperations() {
    val words = TextTarget.Units(TextUnit.WORD, 5, forward = true)
    assertEquals(
      listOf(VoiceStep.TextEdit(TextOperation.DELETE, words, "delete next five words", VoiceStep.Legacy("delete next five words"))),
      steps("delete next five words"),
    )
    assertEquals(
      listOf(
        VoiceStep.TextEdit(
          TextOperation.COPY,
          TextTarget.Units(TextUnit.SENTENCE, 5, forward = false),
          "Copy previous 5 sentences",
          VoiceStep.Legacy("Copy previous 5 sentences"),
        )
      ),
      steps("Copy previous 5 sentences."),
    )
    assertEquals(TextTarget.Units(TextUnit.LINE, 1, forward = true), textTarget("select the next line"))
    assertEquals(TextTarget.Units(TextUnit.PARAGRAPH, 2, forward = false), textTarget("cut last two paragraphs"))
    assertEquals(TextTarget.Units(TextUnit.CHARACTER, 3, forward = false), textTarget("delete previous 3 characters"))
    assertEquals(TextOperation.SELECT, textEdit("highlight next word").operation)
    assertEquals(TextOperation.DELETE, textEdit("remove the previous word").operation)
  }

  @Test
  fun cursorMoves() {
    for ((said, target) in
      listOf(
        "move left 5 words" to TextTarget.Units(TextUnit.WORD, 5, forward = false),
        "move cursor right by three characters" to TextTarget.Units(TextUnit.CHARACTER, 3, forward = true),
        "next 3 words" to TextTarget.Units(TextUnit.WORD, 3, forward = true),
        "go back two lines" to TextTarget.Units(TextUnit.LINE, 2, forward = false),
        "forward 10 words" to TextTarget.Units(TextUnit.WORD, 10, forward = true),
      )) {
      val step = textEdit(said)
      assertEquals(said, TextOperation.MOVE, step.operation)
      assertEquals(said, target, step.target)
    }
  }

  @Test
  fun startAndEndOfUnits() {
    for ((said, target) in
      listOf(
        "Start of sentence." to TextTarget.Boundary(TextUnit.SENTENCE, end = false),
        "end of line" to TextTarget.Boundary(TextUnit.LINE, end = true),
        "start of word" to TextTarget.Boundary(TextUnit.WORD, end = false),
        "go to the end of the paragraph" to TextTarget.Boundary(TextUnit.PARAGRAPH, end = true),
        "move to the beginning of the line" to TextTarget.Boundary(TextUnit.LINE, end = false),
        "to start" to TextTarget.Boundary(TextUnit.TEXT, end = false),
        "go to the end" to TextTarget.Boundary(TextUnit.TEXT, end = true),
        "end of text" to TextTarget.Boundary(TextUnit.TEXT, end = true),
      )) {
      val step = textEdit(said)
      assertEquals(said, TextOperation.MOVE, step.operation)
      assertEquals(said, target, step.target)
    }
    assertEquals(
      VoiceStep.TextEdit(
        TextOperation.DELETE,
        TextTarget.Boundary(TextUnit.LINE, end = true),
        "delete to end of line",
        VoiceStep.Legacy("delete to end of line"),
      ),
      textEdit("delete to end of line"),
    )
  }

  @Test
  fun textPhrases() {
    // Outside text fields, "select" and a text activates the item, as before.
    assertEquals(
      listOf(
        VoiceStep.TextEdit(TextOperation.SELECT, TextTarget.Phrase("crocodile"), "select crocodile", VoiceStep.Tap("crocodile"))
      ),
      steps("select crocodile"),
    )
    assertEquals(
      listOf(
        VoiceStep.TextEdit(TextOperation.DELETE, TextTarget.Phrase("New York"), "Delete New York", VoiceStep.Legacy("Delete New York"))
      ),
      steps("Delete New York."),
    )
    assertEquals(TextTarget.Phrase("the cat"), textTarget("copy the cat"))
    assertEquals(TextTarget.Phrase("crocodile"), textTarget("cut crocodile"))
  }

  @Test
  fun textPhrasesLeaveOtherCommands() {
    // Command names, and other patterns, come first.
    assertEquals(listOf(VoiceStep.Command("SELECT_ALL", "select all")), steps("select all"))
    assertEquals(listOf(VoiceStep.Command("BACK", "back")), steps("back"))
    assertEquals(listOf(VoiceStep.Copy), steps("copy that"))
    assertEquals(listOf(VoiceStep.OpenApp("Chrome", "OPEN_APP:chrome")), steps("start Chrome"))
    assertEquals(listOf(VoiceStep.TypeText("next word")), steps("type next word"))
    assertEquals(listOf(VoiceStep.Tap("Settings")), steps("tap Settings"))
    // Not a unit, or no number.
    assertFalse(grammar.parse("next 3 headings").complete)
    assertFalse(grammar.parse("go home").complete)
    assertFalse(grammar.parse("next lots of words").complete)
  }

  @Test
  fun textEditsInChains() {
    assertEquals(
      listOf(
        TextTarget.Units(TextUnit.WORD, 2, forward = false),
        TextTarget.Boundary(TextUnit.LINE, end = true),
      ),
      steps("select previous two words then end of line").map { (it as VoiceStep.TextEdit).target },
    )
  }

  @Test
  fun customPhraseLeavesOutFillersAndPunctuation() {
    assertEquals("read my mail", grammar.customPhrase("Please read my mail."))
    assertEquals("Read my mail", grammar.customPhrase("  Read my mail for me?"))
    // Counted under the phrase that a custom voice command is matched by.
    assertEquals("read my mail", grammar.customPhrase("could you read my mail now"))
  }

  @Test
  fun customPhraseIsNullForTextToTypeAndEmptyOrLongText() {
    assertEquals(null, grammar.customPhrase("type my secret"))
    assertEquals(null, grammar.customPhrase("Write"))
    assertEquals(null, grammar.customPhrase("enter, the code"))
    assertEquals(null, grammar.customPhrase(" ... "))
    assertEquals(null, grammar.customPhrase("a".repeat(CustomVoiceCommands.MAX_PHRASE + 1)))
    // A word that only starts like a type word isn't text to type.
    assertEquals("typewriter sounds", grammar.customPhrase("typewriter sounds"))
  }

  private fun textEdit(said: String): VoiceStep.TextEdit {
    val steps = steps(said)
    assertEquals(said, 1, steps.size)
    return steps[0] as? VoiceStep.TextEdit ?: throw AssertionError("Not a text edit: $said: $steps")
  }

  private fun textTarget(said: String): TextTarget = textEdit(said).target

  companion object {
    private fun englishArray(name: String): List<String> = englishArrays.getValue("voice_commands_$name")

    private val englishArrays: Map<String, List<String>> by lazy {
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
      arrays
    }

    private fun englishPhrases(): VoiceGrammar.Phrases {
      fun array(name: String) = englishArray(name)
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
        text = textPhrasesForTests(::englishArray),
      )
    }
  }
}
