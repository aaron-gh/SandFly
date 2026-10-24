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

package com.google.android.accessibility.talkback.recognition

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class GeminiLiveProtocolTest {
  @Test fun setupRequestsAudioTranscriptionForSandFlySpeech() {
    val setup = JSONObject(GeminiLiveProtocol.setup("models/test-live", "fr-FR")).getJSONObject("setup")
    assertEquals("models/test-live", setup.getString("model"))
    assertEquals("AUDIO", setup.getJSONObject("generationConfig").getJSONArray("responseModalities").getString(0))
    assertTrue(setup.has("outputAudioTranscription"))
    assertFalse(setup.has("inputAudioTranscription"))
    assertTrue(setup.getJSONObject("systemInstruction").toString().contains("fr-FR"))
  }

  @Test fun frameUsesTypedRealtimeVideoBlob() {
    val message = JSONObject(GeminiLiveProtocol.video("abc"))
    val input = message.getJSONObject("realtimeInput")
    assertEquals(1, input.length())
    assertEquals("image/jpeg", input.getJSONObject("video").getString("mimeType"))
    assertEquals("abc", input.getJSONObject("video").getString("data"))
  }

  @Test fun onlyRequestedCloudModesAreDescribed() {
    val message = JSONObject(GeminiLiveProtocol.request(setOf(RecognitionMode.DOORS, RecognitionMode.TEXT), true))
    val prompt = message.getJSONObject("realtimeInput").getString("text")
    assertTrue(prompt.contains("Doors:"))
    assertFalse(prompt.contains("Scenes:"))
    assertFalse(prompt.contains("People:"))
    assertFalse(prompt.contains("Text:"))
    assertFalse(message.has("clientContent"))
  }

  @Test fun imagePromptHasTheInstructionsAndOnlyTheRequestedModes() {
    val prompt = GeminiLiveProtocol.imagePrompt(setOf(RecognitionMode.PEOPLE), false, "de-DE")
    assertTrue(prompt.contains("Respond in de-DE"))
    assertTrue(prompt.contains("this image"))
    assertTrue(prompt.contains("People:"))
    assertFalse(prompt.contains("Doors:"))
  }

  @Test fun plainPromptAsksShortQuestionsForEachMode() {
    val prompt = GeminiLiveProtocol.plainPrompt(
      setOf(RecognitionMode.DOORS, RecognitionMode.POINT_AND_SPEAK, RecognitionMode.TEXT), false, "en-GB")
    assertEquals("What text is below the pointing finger? Is there a door in this image? Where is it, and is it open? " +
      "Answer in one short sentence, in en-GB.", prompt)
  }

  @Test(expected = IllegalArgumentException::class) fun textAloneNeverCreatesCloudRequest() {
    GeminiLiveProtocol.request(setOf(RecognitionMode.TEXT), true)
  }

  @Test fun pointingRequiresPhysicalFingerAndUsesChosenSide() {
    val above = GeminiLiveProtocol.request(setOf(RecognitionMode.POINT_AND_SPEAK), true)
    val below = GeminiLiveProtocol.request(setOf(RecognitionMode.POINT_AND_SPEAK), false)
    assertTrue(above.contains("actual pointing index finger"))
    assertTrue(above.contains("above"))
    assertTrue(below.contains("below"))
    assertTrue(below.contains("ambiguous or unreadable"))
  }

  @Test fun answerIsCompleteAtGenerationCompleteAndAudioAndThoughtsAreNotSpoken() {
    val response = GeminiLiveResponse()
    assertFalse(response.accept(JSONObject("""{"modelTurn":{"parts":[{"inlineData":{"mimeType":"audio/pcm","data":"AAAA"}},{"text":"private reasoning","thought":true}]},"outputTranscription":{"text":"A door "}}""")))
    assertFalse(response.accept(JSONObject("""{"outputTranscription":{"text":"on the left."}}""")))
    assertTrue(response.accept(JSONObject("""{"generationComplete":true}""")))
    assertEquals("A door on the left.", response.take())
    assertEquals("", response.take())
  }

  @Test fun transcriptAfterTurnCompleteIsPartOfTheAnswer() {
    val response = GeminiLiveResponse()
    assertTrue(response.accept(JSONObject("""{"outputTranscription":{"text":"A table "},"turnComplete":true}""")))
    response.accept(JSONObject("""{"outputTranscription":{"text":"in the middle."}}"""))
    assertEquals("A table in the middle.", response.take())
  }

  @Test fun interruptionDiscardsPartialTranscript() {
    val response = GeminiLiveResponse()
    response.accept(JSONObject("""{"outputTranscription":{"text":"Old scene"}}"""))
    response.accept(JSONObject("""{"interrupted":true}"""))
    response.accept(JSONObject("""{"outputTranscription":{"text":"New scene"},"turnComplete":true}"""))
    assertEquals("New scene", response.take())
  }

  @Test fun transcriptTakesPriorityOverDuplicateTextParts() {
    val response = GeminiLiveResponse()
    response.accept(JSONObject("""{"modelTurn":{"parts":[{"text":"A person"}]},"outputTranscription":{"text":"A person"},"turnComplete":true}"""))
    assertEquals("A person", response.take())
  }

  @Test(expected = IllegalStateException::class) fun unboundedResponseIsRejected() {
    GeminiLiveResponse().accept(JSONObject().put("outputTranscription", JSONObject().put("text", "x".repeat(16001))))
  }
}
