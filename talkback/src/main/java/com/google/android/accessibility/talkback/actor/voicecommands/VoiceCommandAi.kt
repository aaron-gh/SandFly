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

import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.commands.Commands
import org.json.JSONException
import org.json.JSONObject

/**
 * Asks the AI service in use to turn a voice command that the phrase patterns don't understand into
 * [VoiceStep]s. The prompt has the user's commands, macros and scripts by name, and the texts of the
 * items in the active window. Contacts and apps are found on the phone from the names it gives.
 */
object VoiceCommandAi {
  /** The most steps taken from an answer. */
  private const val MAX_STEPS = 20

  private const val MAX_SCREEN_ITEMS = 80
  private const val MAX_ITEM_TEXT = 100
  private const val MAX_NODES = 2_000
  private const val MAX_TEXT = 2_000
  private const val MAX_WAIT_SECONDS = 60

  /** Categories left out of the prompt: apps are opened by name. */
  private val LEFT_OUT = setOf(R.string.shortcut_title_open_app)

  /**
   * The prompt for [said], with [categories] of commands, [screen] item texts, and [language], the
   * name of the language spoken answers are in.
   */
  @JvmStatic
  fun prompt(said: String, categories: List<Commands.Category>, screen: List<String>, language: String): String {
    val commands = StringBuilder()
    for (category in categories) {
      if (category.titleRes in LEFT_OUT) continue
      for (command in category.commands) {
        commands.append(command.value).append(": ").append(command.label).append('\n')
      }
    }
    val items = screen.joinToString("\n")
    return """
      |You turn a spoken command for SandFly, an Android screen reader, into steps that it runs. The
      |person speaking uses the screen reader to use the phone.
      |
      |Answer with JSON only, in the form {"steps":[...]}. Each step is one of these:
      |{"type":"command","value":"<a value from COMMANDS>"} runs a screen reader command.
      |{"type":"tap","label":"<text of an item in SCREEN>"} activates that item.
      |{"type":"open_app","name":"<app name>"} opens an app.
      |{"type":"type","text":"<text>"} types into the edit field that has focus.
      |{"type":"wait","seconds":<1 to 60>}
      |{"type":"call","contact":"<contact name or phone number, as said>"}
      |{"type":"message","contact":"<contact name or number, as said>","text":"<the message>","app":"<messaging app, such as WhatsApp; leave out for SMS>"}
      |  Leave out "text" to send the result of the step before it.
      |{"type":"describe","target":"screen" or "image"} speaks an AI description of the screen or of the focused image. Its text is the result for the next step.
      |{"type":"copy"} copies the result of the step before, or else the last phrase the screen reader spoke.
      |{"type":"say","text":"<short answer>"} only for a question that needs no action, or to say in a few words why the command can't be done.
      |
      |Rules:
      |- Use as few steps as the command needs, in the order said.
      |- Use only values listed in COMMANDS, and labels listed in SCREEN.
      |- Keep message texts as said, in their language, adding nothing.
      |- Never call or message anyone the command doesn't name.
      |- Write "say" texts in $language, plainly, without greetings or offers of help.
      |
      |COMMANDS (value: name):
      |$commands
      |SCREEN (items in the active window, from the top):
      |$items
      |
      |Spoken command: ${JSONObject.quote(said.take(MAX_TEXT))}
      """
      .trimMargin()
  }

  /**
   * Reads the steps in [answer]. Commands not in [commands] are left out. Returns null when the
   * answer isn't the JSON asked for.
   */
  @JvmStatic
  fun parse(answer: String, commands: Map<String, Commands.Command>): List<VoiceStep>? {
    val start = answer.indexOf('{')
    val end = answer.lastIndexOf('}')
    if (start < 0 || end <= start) return null
    val array =
      try {
        JSONObject(answer.substring(start, end + 1)).optJSONArray("steps")
      } catch (e: JSONException) {
        null
      } ?: return null
    val steps = mutableListOf<VoiceStep>()
    for (index in 0 until minOf(array.length(), MAX_STEPS)) {
      val item = array.optJSONObject(index) ?: continue
      fun text(name: String): String? = item.optString(name).trim().take(MAX_TEXT).ifEmpty { null }
      when (item.optString("type")) {
        "command" -> text("value")?.let { commands[it] }?.let { steps.add(VoiceStep.Command(it.value, it.label)) }
        "tap" -> text("label")?.let { steps.add(VoiceStep.Tap(it)) }
        "open_app" -> text("name")?.let { steps.add(VoiceStep.OpenApp(it)) }
        "type" -> text("text")?.let { steps.add(VoiceStep.TypeText(it)) }
        "wait" -> steps.add(VoiceStep.Wait(item.optInt("seconds", 1).coerceIn(1, MAX_WAIT_SECONDS) * 1000L))
        "call" -> text("contact")?.let { steps.add(VoiceStep.Call(it)) }
        "message" ->
          text("contact")?.let {
            val message = text("text")
            steps.add(VoiceStep.Message(who = it, text = message, app = text("app"), usePrevious = message == null))
          }
        "describe" -> steps.add(VoiceStep.Describe(screen = item.optString("target") != "image"))
        "copy" -> steps.add(VoiceStep.Copy)
        "say" -> text("text")?.let { steps.add(VoiceStep.Say(it)) }
      }
    }
    return steps
  }

  /** The texts of the visible items in the active window, from the top. Off the main thread. */
  @JvmStatic
  fun screenItems(service: TalkBackService): List<String> {
    val window =
      service.windows.firstOrNull { it.isActive && it.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY }
        ?: return emptyList()
    val root = window.root ?: return emptyList()
    val items = LinkedHashSet<String>()
    val queue = ArrayDeque<AccessibilityNodeInfo>()
    queue.add(root)
    var count = 0
    while (queue.isNotEmpty() && count < MAX_NODES && items.size < MAX_SCREEN_ITEMS) {
      val node = queue.removeFirst()
      count++
      if (node.isVisibleToUser) {
        val text = (node.contentDescription ?: node.text)?.toString()?.trim()?.replace(Regex("\\s+"), " ")
        if (!text.isNullOrEmpty()) items.add(text.take(MAX_ITEM_TEXT))
      }
      for (i in 0 until node.childCount) {
        node.getChild(i)?.let { queue.add(it) }
      }
    }
    return items.toList()
  }
}
