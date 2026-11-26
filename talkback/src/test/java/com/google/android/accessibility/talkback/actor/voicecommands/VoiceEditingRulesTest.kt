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

import com.google.android.accessibility.talkback.actor.voicecommands.VoiceEditingRules.AfterNoText
import com.google.android.accessibility.talkback.commands.Commands
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** Voice editing's rules, with the English phrases of strings_voice_commands_sandfly.xml. */
class VoiceEditingRulesTest {

  @Test
  fun timeLimitIsOneOfTheChoices() {
    assertEquals(5, VoiceEditingRules.timeLimitMinutes("5"))
    assertEquals(30, VoiceEditingRules.timeLimitMinutes("30"))
    assertEquals(10, VoiceEditingRules.timeLimitMinutes(null))
    assertEquals(10, VoiceEditingRules.timeLimitMinutes("7"))
    assertEquals(10, VoiceEditingRules.timeLimitMinutes("ten"))
  }

  @Test
  fun sessionExpiresAtTheLimit() {
    val limit = 10 * 60_000L
    assertFalse(VoiceEditingRules.expired(now = 1_000L + limit - 1, startedAt = 1_000L, limitMs = limit))
    assertTrue(VoiceEditingRules.expired(now = 1_000L + limit, startedAt = 1_000L, limitMs = limit))
  }

  @Test
  fun nothingSaidRecordsAgain() {
    assertEquals(AfterNoText.LISTEN, VoiceEditingRules.afterNoText(0, failures = 0, recordedMs = 8_000))
    // ERROR_SPEECH_TIMEOUT and ERROR_NO_MATCH.
    assertEquals(AfterNoText.LISTEN, VoiceEditingRules.afterNoText(6, failures = 2, recordedMs = 5_000))
    assertEquals(AfterNoText.LISTEN, VoiceEditingRules.afterNoText(7, failures = 0, recordedMs = 5_000))
  }

  @Test
  fun recordingThatEndsAtOnceCountsAsFailure() {
    assertEquals(AfterNoText.RETRY, VoiceEditingRules.afterNoText(0, failures = 0, recordedMs = 200))
    assertEquals(AfterNoText.STOP, VoiceEditingRules.afterNoText(7, failures = 2, recordedMs = 200))
  }

  @Test
  fun failuresRetryThenStop() {
    // ERROR_SERVER.
    assertEquals(AfterNoText.RETRY, VoiceEditingRules.afterNoText(4, failures = 0, recordedMs = 3_000))
    assertEquals(AfterNoText.RETRY, VoiceEditingRules.afterNoText(4, failures = 1, recordedMs = 3_000))
    assertEquals(AfterNoText.STOP, VoiceEditingRules.afterNoText(4, failures = 2, recordedMs = 3_000))
    // A transcription that failed.
    assertEquals(AfterNoText.RETRY, VoiceEditingRules.afterNoText(-1, failures = 0, recordedMs = 3_000))
  }

  @Test
  fun missingPermissionStopsAtOnce() {
    // ERROR_INSUFFICIENT_PERMISSIONS.
    assertEquals(AfterNoText.STOP, VoiceEditingRules.afterNoText(9, failures = 0, recordedMs = 3_000))
  }

  @Test
  fun retryDelaysDouble() {
    assertEquals(1_000L, VoiceEditingRules.retryDelayMs(1))
    assertEquals(2_000L, VoiceEditingRules.retryDelayMs(2))
    assertEquals(4_000L, VoiceEditingRules.retryDelayMs(3))
    assertEquals(8_000L, VoiceEditingRules.retryDelayMs(10))
    assertEquals(1_000L, VoiceEditingRules.retryDelayMs(0))
  }

  @Test
  fun recordingWaitsForSpeech() {
    // Speech of the command hasn't started yet.
    assertTrue(VoiceEditingRules.keepWaiting(speaking = false, started = false, elapsed = 300, graceMs = 800, maxMs = 60_000))
    assertFalse(VoiceEditingRules.keepWaiting(speaking = false, started = false, elapsed = 800, graceMs = 800, maxMs = 60_000))
    // Speaking, then done.
    assertTrue(VoiceEditingRules.keepWaiting(speaking = true, started = true, elapsed = 5_000, graceMs = 800, maxMs = 60_000))
    assertFalse(VoiceEditingRules.keepWaiting(speaking = false, started = true, elapsed = 400, graceMs = 800, maxMs = 60_000))
    // Speech that goes on too long.
    assertFalse(VoiceEditingRules.keepWaiting(speaking = true, started = true, elapsed = 60_000, graceMs = 800, maxMs = 60_000))
  }

  @Test
  fun focusInField() {
    assertTrue(VoiceEditingRules.stillInField(true, false, false, inputFocusInField = false))
    // Accessibility focus on the keyboard, or nowhere, with input focus in the field.
    assertTrue(VoiceEditingRules.stillInField(false, false, true, inputFocusInField = true))
    assertTrue(VoiceEditingRules.stillInField(false, true, false, inputFocusInField = true))
    assertFalse(VoiceEditingRules.stillInField(false, false, true, inputFocusInField = false))
    // Accessibility focus moved to another item.
    assertFalse(VoiceEditingRules.stillInField(false, false, false, inputFocusInField = true))
  }

  @Test
  fun stopPhrases() {
    val phrases = array("voice_editing_stop_words") + array("voice_commands_cancel_words") + "Start voice editing"
    assertTrue(VoiceEditingRules.isStopPhrase("Stop editing.", phrases, Locale.ENGLISH))
    assertTrue(VoiceEditingRules.isStopPhrase("voice editing off", phrases, Locale.ENGLISH))
    assertTrue(VoiceEditingRules.isStopPhrase("Cancel", phrases, Locale.ENGLISH))
    assertTrue(VoiceEditingRules.isStopPhrase("start voice editing", phrases, Locale.ENGLISH))
    assertFalse(VoiceEditingRules.isStopPhrase("stop editing the title", phrases, Locale.ENGLISH))
    assertFalse(VoiceEditingRules.isStopPhrase("", phrases, Locale.ENGLISH))
  }

  @Test
  fun onlyEditingCategoriesWithoutExcludedCommands() {
    val commands =
      VoiceEditingRules.editingCommands(categories, setOf(TEXT_EDITING, NAVIGATE_BY_TEXT), setOf("DICTATE", "VOICE_EDITING"))
    assertEquals(
      listOf("SELECT_ALL", "COPY", "PASTE", "CLEAR_TEXT", "NEW_LINE", "NEXT_WORD", "PREVIOUS_CHARACTER"),
      commands.map { it.value },
    )
  }

  @Test
  fun grammarRunsEditingCommandsAndTyping() {
    assertEquals(listOf(VoiceStep.Command("SELECT_ALL", "Select all")), steps("Select all."))
    assertEquals(listOf(VoiceStep.Command("NEXT_WORD", "Next word")), steps("please next word"))
    assertEquals(
      listOf(VoiceStep.Command("SELECT_ALL", "Select all"), VoiceStep.Command("COPY", "Copy")),
      steps("select all then copy"),
    )
    assertEquals(listOf(VoiceStep.TypeText("Hello there")), steps("type Hello there"))
    assertEquals(listOf(VoiceStep.Command("CLEAR_TEXT", "Clear text")), steps("run clear text"))
  }

  @Test
  fun grammarLeavesOtherCommandsToTheEditingLegacyCommands() {
    // Not editing commands: open, call, tap, describe and commands of other categories.
    for (said in listOf("open Chrome", "call Sam", "tap Settings", "press OK", "describe the screen", "go home", "dictate")) {
      val result = grammar.parse(said)
      assertFalse(said, result.complete)
      assertEquals(said, listOf(VoiceStep.Legacy(said)), result.steps)
    }
  }

  @Test
  fun newLineByItsNameAndOtherPhrases() {
    for (said in listOf("New line.", "new paragraph", "press enter", "Press Enter please")) {
      assertEquals(said, listOf(VoiceStep.Command("NEW_LINE", "New line")), steps(said))
    }
    assertEquals(
      listOf(VoiceStep.TypeText("Dear Sam"), VoiceStep.Command("NEW_LINE", "New line")),
      steps("type Dear Sam then new line"),
    )
  }

  @Test
  fun textEditingPhrasesActOnTheTextOnly() {
    assertEquals(
      listOf(
        VoiceStep.TextEdit(TextOperation.DELETE, TextTarget.Units(TextUnit.WORD, 5, forward = true), "delete next five words")
      ),
      steps("delete next five words"),
    )
    // No item is activated in voice editing: "select" and a text selects it in the field.
    assertEquals(
      listOf(VoiceStep.TextEdit(TextOperation.SELECT, TextTarget.Phrase("crocodile"), "select crocodile")),
      steps("select crocodile"),
    )
    assertEquals(
      listOf(VoiceStep.TextEdit(TextOperation.MOVE, TextTarget.Boundary(TextUnit.SENTENCE, end = true), "end of sentence")),
      steps("end of sentence"),
    )
    assertEquals(
      listOf(
        VoiceStep.TextEdit(TextOperation.SELECT, TextTarget.Units(TextUnit.WORD, 2, forward = false), "select previous 2 words"),
        VoiceStep.Command("COPY", "Copy"),
      ),
      steps("select previous 2 words then copy"),
    )
    // Command names still come first.
    assertEquals(listOf(VoiceStep.Command("NEXT_WORD", "Next word")), steps("next word"))
    assertEquals(listOf(VoiceStep.Command("SELECT_ALL", "Select all")), steps("select all"))
  }

  @Test
  fun aliasesOfCommandsNotListedAreLeftOut() {
    val names =
      VoiceEditingRules.commandNames(
        listOf(command("COPY", "Copy")),
        Locale.ENGLISH,
        mapOf("NEW_LINE" to listOf("new line")),
      )
    assertNull(names["new line"])
    assertEquals("COPY", names["copy"]?.value)
  }

  @Test
  fun customVoiceCommandsForEditingCommandsAndMacros() {
    assertEquals(listOf(VoiceStep.Command("COPY", "Copy")), steps("grab it"))
    assertEquals(listOf(VoiceStep.Command("MACRO:1", "undo that")), steps("undo that"))
    assertNull(resolver.custom("go home now"))
  }

  private val categories =
    listOf(
      Commands.Category(
        TEXT_EDITING,
        listOf(
          command("SELECT_ALL", "Select all"),
          command("COPY", "Copy"),
          command("PASTE", "Paste"),
          command("CLEAR_TEXT", "Clear text"),
          command("NEW_LINE", "New line"),
          command("DICTATE", "Dictate"),
          command("VOICE_EDITING", "Start voice editing"),
        ),
      ),
      Commands.Category(
        NAVIGATE_BY_TEXT,
        listOf(command("NEXT_WORD", "Next word"), command("PREVIOUS_CHARACTER", "Previous character")),
      ),
      Commands.Category(SYSTEM, listOf(command("HOME", "Go home"), command("BACK", "Back"))),
    )

  private val custom =
    listOf(
      CustomVoiceCommands.Entry("1", "grab it", "COPY"),
      CustomVoiceCommands.Entry("2", "undo that", "MACRO:1"),
      CustomVoiceCommands.Entry("3", "go home now", "HOME"),
    )

  private val editing =
    VoiceEditingRules.editingCommands(categories, setOf(TEXT_EDITING, NAVIGATE_BY_TEXT), setOf("DICTATE", "VOICE_EDITING"))

  private val resolver =
    VoiceEditingRules.Resolver(
      VoiceEditingRules.commandNames(
        editing,
        Locale.ENGLISH,
        mapOf("NEW_LINE" to array("voice_commands_new_line")),
      ),
      custom,
      { value -> value in editing.map { it.value } || value.startsWith("MACRO:") },
      Locale.ENGLISH,
    )

  private val grammar =
    VoiceGrammar(
      VoiceEditingRules.phrases(
        chain = array("voice_commands_chain_words"),
        and = array("voice_commands_and_words"),
        fillersStart = array("voice_commands_fillers_start"),
        fillersEnd = array("voice_commands_fillers_end"),
        cancel = array("voice_commands_cancel_words"),
        type = array("voice_commands_type_text"),
        run = array("voice_commands_run"),
        text = textPhrasesForTests { array("voice_commands_$it") },
      ),
      resolver,
      Locale.ENGLISH,
    )

  private fun steps(said: String): List<VoiceStep> {
    val result = grammar.parse(said)
    assertTrue("Not understood: $said (${result.unparsed})", result.complete)
    return result.steps
  }

  private companion object {
    const val TEXT_EDITING = 1
    const val NAVIGATE_BY_TEXT = 2
    const val SYSTEM = 3

    fun command(value: String, label: String) = Commands.Command(value, label, keyboardAction = null)

    private val arrays: Map<String, List<String>> by lazy {
      val file =
        listOf(
            "src/main/res/values/strings_voice_commands_sandfly.xml",
            "talkback/src/main/res/values/strings_voice_commands_sandfly.xml",
          )
          .map(::File)
          .first { it.exists() }
      val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
      val result = HashMap<String, List<String>>()
      val nodes = document.getElementsByTagName("string-array")
      for (i in 0 until nodes.length) {
        val array = nodes.item(i) as Element
        val items = array.getElementsByTagName("item")
        result[array.getAttribute("name")] =
          (0 until items.length).map { items.item(it).textContent.replace("\\'", "'") }
      }
      result
    }

    fun array(name: String): List<String> = arrays.getValue(name)
  }
}
