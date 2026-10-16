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
 * License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.talkback.actor.gemini

import android.util.Base64
import android.util.Log
import com.google.android.accessibility.gemineye.api.AccessibilityTree
import com.google.android.accessibility.gemineye.screenoverview.json.MessageType
import com.google.android.accessibility.gemineye.screenoverview.json.ParseException
import com.google.android.accessibility.gemineye.screenoverview.json.Parser
import com.google.android.accessibility.gemineye.screenoverview.json.ScreenOverview
import com.google.android.accessibility.gemineye.screenoverview.json.ScreenQueryAnswer
import com.google.android.accessibility.talkback.BuildConfig
import com.google.android.accessibility.talkback.actor.gemini.DataFieldUtils.GeminiResponse
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.ErrorReason
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.FinishReason
import com.google.android.accessibility.talkback.actor.gemini.GeminiRestRequestPerformer.GeminiRestResponseCallback
import com.google.android.accessibility.talkback.actor.gemini.screenqa.OverviewResponse
import com.google.android.accessibility.talkback.actor.gemini.screenqa.ScreenTree
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/** How a screen request is worded, which depends on who answers it. */
internal enum class PromptStyle {
  /** The Gemini API, which handles the full format. */
  CLOUD,

  /**
   * A model on the phone. It is asked only for what the result screen shows. The controls list is
   * left out because this build does not show it, and writing it was most of the answer.
   */
  ON_DEVICE,

  /** [ON_DEVICE], with less detail and a shorter list of the nodes, to answer sooner. */
  ON_DEVICE_SHORT,
}

/**
 * Sends screen overview and screen question requests to the Gemini REST API. Google's release
 * only supports these through its own server, so this builds the prompts and parses the JSON reply
 * into the types that the screen Q&A UI already uses.
 */
internal class ScreenOverviewRequester(
  private val style: () -> PromptStyle = { PromptStyle.CLOUD },
  private val performRequest: (JSONObject, GeminiRestResponseCallback) -> Unit,
  /** What to tell the user about a failed request, or null for the general error message. */
  private val describeFailure: (GeminiFailure) -> String? = { null },
) {
  private val parser = Parser()

  fun requestOverview(command: GeminiCommand.ScreenOverview, safetySettings: JSONArray) {
    val style = style()
    val tree = ScreenTree.fromNode(command.focusedNode)
    val instructions =
      when (style) {
        PromptStyle.CLOUD -> OVERVIEW_INSTRUCTIONS
        PromptStyle.ON_DEVICE -> ON_DEVICE_OVERVIEW_INSTRUCTIONS
        PromptStyle.ON_DEVICE_SHORT -> SHORT_OVERVIEW_INSTRUCTIONS
      }
    val prompt = "$instructions\n${languageInstruction()}\n\nNodes:\n${nodes(tree, style)}"
    send(
      prompt,
      style,
      command.screenshot,
      safetySettings,
      command.listener,
      parse = { json ->
        OverviewResponse.Success(parser.parseScreenOverview(json), tree, command.screenshot)
      },
      fallback = { text ->
        OverviewResponse.Success(
          ScreenOverview(summary = ScreenJsonRepair.fallbackText(text, "summary")),
          tree,
          command.screenshot,
        )
      },
    )
  }

  fun requestQuery(command: GeminiCommand.ScreenQuery, safetySettings: JSONArray) {
    val style = style()
    val history =
      command.chatHistory.orEmpty().joinToString(separator = "\n") {
        val speaker = if (it.messageType == MessageType.USER) "USER" else "MODEL"
        "$speaker: ${it.message}"
      }
    val instructions =
      when (style) {
        PromptStyle.CLOUD -> QUERY_INSTRUCTIONS
        PromptStyle.ON_DEVICE -> ON_DEVICE_QUERY_INSTRUCTIONS
        PromptStyle.ON_DEVICE_SHORT -> SHORT_QUERY_INSTRUCTIONS
      }
    val prompt =
      "$instructions\n${languageInstruction()}\n\nNodes:\n${nodeList(command.a11yTree, style)}" +
        "\n\nConversation so far:\n$history\n\nQuestion: ${command.query}"
    send(
      prompt,
      style,
      command.screenshot,
      safetySettings,
      command.listener,
      parse = { json ->
        OverviewResponse.QueryAnswer(
          parser.parseScreenQueryAnswer(json),
          command.a11yTree,
          command.screenshot,
        )
      },
      fallback = { text ->
        OverviewResponse.QueryAnswer(
          ScreenQueryAnswer(answer = ScreenJsonRepair.fallbackText(text, "answer")),
          command.a11yTree,
          command.screenshot,
        )
      },
    )
  }

  private fun send(
    prompt: String,
    style: PromptStyle,
    screenshot: ByteArray,
    safetySettings: JSONArray,
    listener: GeminiResponseCallback<OverviewResponse>,
    parse: (String) -> OverviewResponse,
    fallback: (String) -> OverviewResponse,
  ) {
    if (screenshot.isEmpty()) {
      listener.onError(ErrorReason.NO_IMAGE)
      return
    }
    Log.d(TAG, "Prompt is ${prompt.length} characters, style=$style")
    val postData =
      DataFieldUtils.createPostDataJson(
        prompt,
        Base64.encodeToString(screenshot, Base64.NO_WRAP),
        safetySettings,
      )
    postData.getJSONObject("generationConfig").put("responseMimeType", "application/json")
    performRequest(
      postData,
      object : GeminiRestResponseCallback {
        override fun onResponse(response: GeminiResponse) {
          val text = response.text()
          if (response.finishReason() != DataFieldUtils.FINISH_REASON_STOP || text == null) {
            Log.e(
              TAG,
              "Gemini gave no answer, finish reason ${response.finishReason()}, " +
                "block reason ${response.blockReason()}",
            )
            listener.onResponse(FinishReason.ERROR_BLOCKED, error(FinishReason.ERROR_BLOCKED))
            return
          }
          if (BuildConfig.DEBUG) {
            Log.d(TAG, "Model answer (${text.length} characters): ${text.take(1500)}")
          }
          try {
            listener.onResponse(FinishReason.STOP, parse(ScreenJsonRepair.repair(text)))
          } catch (e: ParseException) {
            Log.w(TAG, "Could not parse the screen answer (${text.length} characters)", e)
            if (style == PromptStyle.CLOUD || text.isBlank()) {
              listener.onResponse(
                FinishReason.ERROR_PARSING_RESULT,
                error(FinishReason.ERROR_PARSING_RESULT),
              )
            } else {
              // A small model that cannot keep to the format still wrote something useful, so show
              // that rather than an error.
              listener.onResponse(FinishReason.STOP, fallback(text))
            }
          }
        }

        override fun onFailure(failure: GeminiFailure) {
          // The request performer has logged the failure.
          listener.onResponse(
            FinishReason.ERROR_RESPONSE,
            OverviewResponse.Error(
              errorReason = null,
              finishReason = FinishReason.ERROR_RESPONSE,
              message = describeFailure(failure),
            ),
          )
        }

        override fun onCancelled() {
          listener.onError(ErrorReason.JOB_CANCELLED)
        }
      },
    )
  }

  private fun error(finishReason: FinishReason) =
    OverviewResponse.Error(errorReason = null, finishReason = finishReason)

  private fun nodes(tree: ScreenTree, style: PromptStyle): String =
    if (style == PromptStyle.ON_DEVICE_SHORT) tree.compactDescription else tree.description

  private fun nodeList(tree: AccessibilityTree, style: PromptStyle): String =
    (tree as? ScreenTree)?.let { nodes(it, style) }.orEmpty()

  private fun languageInstruction(): String =
    "Write all text in ${Locale.getDefault().getDisplayLanguage(Locale.ENGLISH)}. " +
      "Do not use markdown."

  companion object {
    private const val TAG = "ScreenOverviewRequester"
    private const val UI_ELEMENT_FORMAT =
      """Each UI element is an object with these fields:
- "label": the name of the control as shown on screen.
- "description": what the control does, in one short sentence.
- "type": one of button, floating_action_button, text_field, header, checkbox, menu, slider, tab, link.
- "parent_container": one of tab_bar, top_navigation, bottom_navigation, side_menu, filters, post, list_item, main_content.
- "potentiallyMatchingNodes": a list of objects like {"uniqueId": 12}, with the numbers of the nodes that match the control, most likely first."""

    private const val OVERVIEW_INSTRUCTIONS =
      """You help a blind person who uses the TalkBack screen reader understand their phone screen. You get a screenshot and a numbered list of the accessibility nodes on the screen.

Reply with only a JSON object with these fields:
- "summary": 2 to 4 sentences about what the screen is for and what is on it. Mention important content that the node list does not describe, such as images, charts, or text in images. Do not list every element.
- "top_images": up to 3 short descriptions of the most important images on the screen. Use an empty list if there are none.
- "top_ui_elements": up to 6 of the most useful controls on the screen.

$UI_ELEMENT_FORMAT"""

    private const val QUERY_INSTRUCTIONS =
      """You help a blind person who uses the TalkBack screen reader understand their phone screen. You get a screenshot, a numbered list of the accessibility nodes on the screen, the conversation so far, and a question.

Reply with only a JSON object with these fields:
- "answer": the answer to the question, in plain text.
- "ui_elements": the controls that the answer refers to, or an empty list.

$UI_ELEMENT_FORMAT"""

    // For a model on the phone. The result screen shows only the summary, the images and the
    // answer, so nothing else is asked for. The example helps small models keep to the format.
    private const val ON_DEVICE_OVERVIEW_INSTRUCTIONS =
      """You help a blind person who uses the TalkBack screen reader understand their phone screen. You get a screenshot and a numbered list of the accessibility nodes on the screen.

Reply with only a JSON object with these fields:
- "summary": 2 to 4 sentences about what the screen is for and what is on it. Mention important content that the node list does not describe, such as images, charts, or text in images. Do not list every element.
- "top_images": up to 3 short descriptions of the most important images on the screen. Use an empty list if there are none.

Example: {"summary": "A chat with Sam. The last message asks about dinner.", "top_images": ["A photo of a dog on a beach"]}"""

    private const val ON_DEVICE_QUERY_INSTRUCTIONS =
      """You help a blind person who uses the TalkBack screen reader understand their phone screen. You get a screenshot, a numbered list of the accessibility nodes on the screen, the conversation so far, and a question.

Reply with only a JSON object with one field, "answer": the answer to the question, in plain text.

Example: {"answer": "The Send button is at the bottom right."}"""

    private const val SHORT_OVERVIEW_INSTRUCTIONS =
      """You help a blind person understand their phone screen. You get a screenshot and a numbered list of the nodes on the screen.

Reply with only a JSON object with these fields:
- "summary": 2 or 3 short sentences about what the screen is for and what is on it, including images or text in images that the list does not show.
- "top_images": up to 2 short descriptions of real pictures on the screen, or an empty list.

Example: {"summary": "A chat with Sam. The last message asks about dinner.", "top_images": []}"""

    private const val SHORT_QUERY_INSTRUCTIONS =
      """You help a blind person understand their phone screen. You get a screenshot, a numbered list of the nodes on the screen, the conversation so far, and a question.

Reply with only a JSON object with one field, "answer": a short plain answer to the question.

Example: {"answer": "The Send button is at the bottom right."}"""
  }
}
