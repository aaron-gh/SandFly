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

  @Test fun waitsForTurnCompleteAndDoesNotSpeakAudioOrThoughts() {
    val response = GeminiLiveResponse()
    assertNull(response.accept(JSONObject("""{"modelTurn":{"parts":[{"inlineData":{"mimeType":"audio/pcm","data":"AAAA"}},{"text":"private reasoning","thought":true}]},"outputTranscription":{"text":"A door "}}""")))
    assertNull(response.accept(JSONObject("""{"outputTranscription":{"text":"on the left."},"generationComplete":true}""")))
    assertEquals("A door on the left.", response.accept(JSONObject("""{"turnComplete":true}""")))
    assertEquals("", response.accept(JSONObject("""{"turnComplete":true}""")))
  }

  @Test fun interruptionDiscardsPartialTranscript() {
    val response = GeminiLiveResponse()
    response.accept(JSONObject("""{"outputTranscription":{"text":"Old scene"}}"""))
    response.accept(JSONObject("""{"interrupted":true}"""))
    assertEquals("New scene", response.accept(JSONObject("""{"outputTranscription":{"text":"New scene"},"turnComplete":true}""")))
  }

  @Test fun transcriptTakesPriorityOverDuplicateTextParts() {
    assertEquals("A person", GeminiLiveResponse().accept(JSONObject("""{"modelTurn":{"parts":[{"text":"A person"}]},"outputTranscription":{"text":"A person"},"turnComplete":true}""")))
  }

  @Test(expected = IllegalStateException::class) fun unboundedResponseIsRejected() {
    GeminiLiveResponse().accept(JSONObject().put("outputTranscription", JSONObject().put("text", "x".repeat(16001))))
  }
}
