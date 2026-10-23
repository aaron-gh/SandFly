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

package com.google.android.accessibility.talkback.dictation

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.Locale
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DictationTest {
  @Test
  fun transcriptionIsTheTextOfTheModelOutputSteps() {
    val answer =
      JSONObject(
        """{"id":"interactions/1","status":"completed","steps":[
          {"type":"user_input","content":[{"type":"text","text":"ignored"}]},
          {"type":"model_output","content":[{"type":"text","text":"Let's meet at 3:00 PM.","annotations":[]}]}
        ]}"""
      )
    assertEquals("Let's meet at 3:00 PM.", DictationRequests.interactionText(answer))
  }

  @Test
  fun transcriptionIsReadFromOutputsToo() {
    val answer = JSONObject("""{"outputs":[{"type":"text","text":" Hello "}]}""")
    assertEquals("Hello", DictationRequests.interactionText(answer))
  }

  @Test
  fun wavHasAHeaderBeforeTheSamples() {
    val pcm = byteArrayOf(1, 0, -1, -1)
    val wav = AudioCapture.toWav(pcm)
    assertEquals(48, wav.size)
    assertEquals("RIFF", String(wav, 0, 4, Charsets.US_ASCII))
    assertEquals("WAVE", String(wav, 8, 4, Charsets.US_ASCII))
    assertEquals("data", String(wav, 36, 4, Charsets.US_ASCII))
    assertArrayEquals(pcm, wav.copyOfRange(44, 48))
  }

  @Test
  fun samplesRunFromMinusOneToOne() {
    val floats = AudioCapture.toFloats(byteArrayOf(0, 0x40, 0, -128))
    assertEquals(0.5f, floats[0], 0.0001f)
    assertEquals(-1f, floats[1], 0.0001f)
  }

  @Test
  fun ramblerLanguages() {
    assertTrue(DictationSettings.ramblerSupports(Locale.US))
    assertTrue(DictationSettings.ramblerSupports(Locale.forLanguageTag("vi-VN")))
    assertFalse(DictationSettings.ramblerSupports(Locale.forLanguageTag("cy-GB")))
  }

  /** Answers each request with [answer] and keeps the request's headers and body. */
  private class FakeService(private val answer: String) : AutoCloseable {
    var body = ""
    var headers = mapOf<String, String>()
    var path = ""
    private val server =
      HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/") { exchange ->
          path = exchange.requestURI.path
          body = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
          headers = exchange.requestHeaders.mapValues { it.value.first() }
          val bytes = answer.toByteArray()
          exchange.sendResponseHeaders(200, bytes.size.toLong())
          exchange.responseBody.use { it.write(bytes) }
        }
        start()
      }
    val address = "http://127.0.0.1:${server.address.port}"

    override fun close() = server.stop(0)
  }

  @Test
  fun ramblerRequestAsksForSmartTranscriptionOfInlineAudio() {
    FakeService("""{"steps":[{"type":"model_output","content":[{"type":"text","text":"Hi."}]}]}""").use {
      val text =
        DictationRequests.transcribe("key", byteArrayOf(1, 2, 3), url = "${it.address}/v1beta/interactions")
      assertEquals("Hi.", text)
      val body = JSONObject(it.body)
      assertEquals("gemini-3.5-transcribe", body.getString("model"))
      assertFalse(body.getBoolean("store"))
      val audio = body.getJSONArray("input").getJSONObject(0)
      assertEquals("audio", audio.getString("type"))
      assertEquals("audio/wav", audio.getString("mime_type"))
      assertEquals("AQID", audio.getString("data"))
      assertEquals(
        "smart",
        body.getJSONObject("generation_config").getJSONObject("transcription_config").getString("mode"),
      )
      assertEquals("key", it.headers["X-goog-api-key"])
    }
  }

  @Test
  fun chatCorrectionSendsThePromptAndReadsTheAnswer() {
    FakeService("""{"choices":[{"message":{"content":"Hello, Sam."},"finish_reason":"stop"}]}""").use {
      val answer = DictationRequests.askChat("${it.address}/v1/chat/completions", "secret", "model-1", "fix it")
      assertEquals("Hello, Sam.", answer)
      val body = JSONObject(it.body)
      assertEquals("model-1", body.getString("model"))
      assertEquals("fix it", body.getJSONArray("messages").getJSONObject(0).getString("content"))
      assertEquals("Bearer secret", it.headers["Authorization"])
    }
  }

  @Test
  fun geminiCorrectionReadsTheCandidateText() {
    FakeService("""{"candidates":[{"content":{"parts":[{"text":"Hello, "},{"text":"Sam."}]}}]}""").use {
      val answer = DictationRequests.askGemini("key", "gemini-x", "fix it", base = "${it.address}/models/")
      assertEquals("Hello, Sam.", answer)
      assertEquals("/models/gemini-x:generateContent", it.path)
    }
  }
}
