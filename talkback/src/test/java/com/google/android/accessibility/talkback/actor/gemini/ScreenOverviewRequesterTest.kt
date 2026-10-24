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

package com.google.android.accessibility.talkback.actor.gemini

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.gemineye.api.AccessibilityTree
import com.google.android.accessibility.gemineye.api.NodeId
import com.google.android.accessibility.talkback.actor.gemini.DataFieldUtils.GeminiResponse
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.ErrorReason
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.FinishReason
import com.google.android.accessibility.talkback.actor.gemini.local.LocalModel
import com.google.android.accessibility.talkback.actor.gemini.screenqa.OverviewResponse
import com.google.protobuf.ByteString
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScreenOverviewRequesterTest {
  private val tree =
    object : AccessibilityTree {
      override fun findNodeById(nodeId: NodeId): AccessibilityNodeInfoCompat? = null

      override fun serialize(): ByteString = ByteString.EMPTY
    }

  /** Asks a question in [style], answers it with [answer], and returns the request and result. */
  private fun ask(style: PromptStyle, answer: String): Pair<JSONObject, OverviewResponse> {
    var request: JSONObject? = null
    var result: OverviewResponse? = null
    val requester =
      ScreenOverviewRequester(
        style = { style },
        performRequest = { postData, callback ->
          request = postData
          callback.onResponse(
            GeminiResponse.builder()
              .setText(answer)
              .setFinishReason(DataFieldUtils.FINISH_REASON_STOP)
              .build()
          )
        },
      )
    requester.requestQuery(
      GeminiCommand.ScreenQuery(
        screenshot = byteArrayOf(1),
        a11yTree = tree,
        query = "Which devices are on the list?",
        chatHistory = null,
        listener =
          object : GeminiResponseCallback<OverviewResponse> {
            override fun onResponse(finishReason: FinishReason, response: OverviewResponse) {
              result = response
            }

            override fun onError(errorReason: ErrorReason) {}
          },
      ),
      JSONArray(),
    )
    return request!! to result!!
  }

  private fun prompt(request: JSONObject): String =
    request
      .getJSONArray("contents")
      .getJSONObject(0)
      .getJSONArray("parts")
      .let { parts -> (0 until parts.length()).map { parts.getJSONObject(it) } }
      .first { it.has("text") }
      .getString("text")

  @Test
  fun plainStyleAsksForSentencesAndKeepsTheAnswerAsWritten() {
    val (request, result) =
      ask(PromptStyle.ON_DEVICE_PLAIN, " The devices are \"fold7\" and {macbook}. ")

    assertFalse(prompt(request).contains("Example"))
    assertFalse(request.getJSONObject("generationConfig").has("responseMimeType"))
    assertEquals(
      "The devices are \"fold7\" and {macbook}.",
      (result as OverviewResponse.QueryAnswer).answer.answer,
    )
  }

  @Test
  fun plainOverviewNamesTheAppAndListsTheTextOnlyToCopy() {
    var request: JSONObject? = null
    var result: OverviewResponse? = null
    val node = AccessibilityNodeInfoCompat.obtain().apply {
      packageName = "com.tailscale.ipn"
      text = "Connected"
      isVisibleToUser = true
    }
    ScreenOverviewRequester(
        style = { PromptStyle.ON_DEVICE_PLAIN },
        performRequest = { postData, callback ->
          request = postData
          callback.onResponse(
            GeminiResponse.builder()
              .setText("The Tailscale app shows two connected devices.")
              .setFinishReason(DataFieldUtils.FINISH_REASON_STOP)
              .build()
          )
        },
        appName = { if (it == "com.tailscale.ipn") "Tailscale" else null },
      )
      .requestOverview(
        GeminiCommand.ScreenOverview(
          screenshot = byteArrayOf(1),
          focusedNode = node,
          listener =
            object : GeminiResponseCallback<OverviewResponse> {
              override fun onResponse(finishReason: FinishReason, response: OverviewResponse) {
                result = response
              }

              override fun onError(errorReason: ErrorReason) {}
            },
        ),
        JSONArray(),
      )

    val prompt = prompt(request!!)
    assertTrue(prompt.contains("The app is Tailscale."))
    assertFalse(prompt.contains("Nodes:"))
    assertTrue(prompt.endsWith("copy its text exactly from this list: Connected"))
    assertEquals(
      "The Tailscale app shows two connected devices.",
      (result as OverviewResponse.Success).overview.summary,
    )
  }

  @Test
  fun onDeviceStyleStillReadsJson() {
    val (request, result) = ask(PromptStyle.ON_DEVICE, """{"answer": "Two devices."}""")

    assertEquals(
      "application/json",
      request.getJSONObject("generationConfig").getString("responseMimeType"),
    )
    assertEquals("Two devices.", (result as OverviewResponse.QueryAnswer).answer.answer)
  }

  @Test
  fun onlyGemmaModelsGetTheJsonFormat() {
    val json = LocalModel.entries.filter { it.writesJson }.map { it.id }
    assertEquals(listOf("e2b", "e4b", "gemma4-12b", "e2b-tensor-g5", "e2b-tensor-g6"), json)
    assertTrue(LocalModel.entries.filter { it.id.startsWith("fastvlm") }.none { it.writesJson })
  }
}
