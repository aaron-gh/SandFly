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
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.accessibility.talkback.actor.gemini

import com.google.android.accessibility.talkback.actor.gemini.DataFieldUtils.GeminiResponse
import com.google.android.accessibility.talkback.actor.gemini.GeminiRestRequestPerformer.GeminiRestResponseCallback
import com.google.android.accessibility.talkback.actor.gemini.local.LocalLlm
import com.google.android.accessibility.talkback.actor.gemini.local.LocalLlmCancelledException
import com.google.android.accessibility.talkback.actor.gemini.local.LocalLlmException
import com.google.android.accessibility.talkback.actor.gemini.local.LocalLlmMemoryException
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalGemmaRunnerTest {
  private val worker = Executors.newSingleThreadExecutor()
  private val sameThread = java.util.concurrent.Executor { it.run() }

  @After
  fun tearDown() {
    worker.shutdownNow()
  }

  private class FakeLlm(var answer: () -> String = { "hello" }) : LocalLlm {
    var prompt: String? = null
    var jpeg: ByteArray? = null
    @Volatile var cancelled = false
    val started = CountDownLatch(1)
    val release = CountDownLatch(1)
    var block = false

    override fun generate(prompt: String, jpeg: ByteArray?): String {
      this.prompt = prompt
      this.jpeg = jpeg
      started.countDown()
      if (block) {
        release.await(5, TimeUnit.SECONDS)
        if (cancelled) throw LocalLlmCancelledException()
      }
      return answer()
    }

    override fun cancel() {
      cancelled = true
      release.countDown()
    }
  }

  private class Recorder : GeminiRestResponseCallback {
    val done = CountDownLatch(1)
    var response: GeminiResponse? = null
    var failure: String? = null
    var cancelled = false

    override fun onResponse(response: GeminiResponse) {
      this.response = response
      done.countDown()
    }

    override fun onFailure(failure: GeminiFailure) {
      this.failure = failure.message
      done.countDown()
    }

    override fun onCancelled() {
      cancelled = true
      done.countDown()
    }

    fun await() = assertTrue(done.await(5, TimeUnit.SECONDS))
  }

  private fun request(prompt: String, image: ByteArray? = null, json: Boolean = false): JSONObject {
    val parts = JSONArray().put(JSONObject().put("text", prompt))
    if (image != null) {
      parts.put(
        JSONObject()
          .put(
            "inlineData",
            JSONObject()
              .put("mimeType", "image/jpeg")
              .put("data", Base64.getEncoder().encodeToString(image)),
          )
      )
    }
    val body =
      JSONObject()
        .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", parts)))
        .put("generationConfig", JSONObject().put("temperature", 0.0))
    if (json) body.getJSONObject("generationConfig").put("responseMimeType", "application/json")
    return body
  }

  private val announced = mutableListOf<String>()

  private fun runner(llm: LocalLlm?) =
    LocalGemmaRunner({ llm }, { llm }, worker, sameThread, sameThread, { announced.add(it) })

  @Test
  fun aMemoryFailureIsSpokenAndEndsQuietly() {
    val llm = FakeLlm { throw LocalLlmMemoryException("Not enough free memory for Gemma") }
    val recorder = Recorder()
    runner(llm).run(request("Describe"), recorder)
    recorder.await()
    assertEquals(listOf("Not enough free memory for Gemma"), announced)
    assertTrue(recorder.cancelled)
    assertNull(recorder.failure)
  }

  @Test
  fun otherFailuresAreNotSpokenByTheRunner() {
    val llm = FakeLlm { throw LocalLlmException("broken") }
    val recorder = Recorder()
    runner(llm).run(request("Describe"), recorder)
    recorder.await()
    assertTrue(announced.isEmpty())
    assertTrue(recorder.failure!!.contains("broken"))
  }

  @Test
  fun passesThePromptAndImageToTheModelAndReturnsItsAnswer() {
    val llm = FakeLlm()
    val recorder = Recorder()
    runner(llm).run(request("Describe", byteArrayOf(1, 2, 3)), recorder)
    recorder.await()
    assertEquals("Describe", llm.prompt)
    assertTrue(byteArrayOf(1, 2, 3).contentEquals(llm.jpeg))
    assertEquals("hello", recorder.response!!.text())
    assertEquals("STOP", recorder.response!!.finishReason())
  }

  @Test
  fun jsonRequestsGetTheJsonOutOfAFenceAndSurroundingText() {
    val llm = FakeLlm { "Sure!\n```json\n{\"summary\": \"x\"}\n```\nHope it helps." }
    val recorder = Recorder()
    runner(llm).run(request("p", json = true), recorder)
    recorder.await()
    assertEquals("{\"summary\": \"x\"}", recorder.response!!.text())
  }

  @Test
  fun plainRequestsKeepTheirText() {
    val llm = FakeLlm { "  A dog {sitting}.  " }
    val recorder = Recorder()
    runner(llm).run(request("p"), recorder)
    recorder.await()
    assertEquals("A dog {sitting}.", recorder.response!!.text())
  }

  @Test
  fun noModelIsAFailure() {
    val recorder = Recorder()
    runner(null).run(request("p"), recorder)
    recorder.await()
    assertTrue(recorder.failure!!.contains("No on-device model"))
  }

  @Test
  fun aModelErrorIsAFailure() {
    val recorder = Recorder()
    runner(FakeLlm { error("out of memory") }).run(request("p"), recorder)
    recorder.await()
    assertTrue(recorder.failure!!.contains("out of memory"))
  }

  @Test
  fun anEmptyAnswerIsAFailure() {
    val recorder = Recorder()
    runner(FakeLlm { "  " }).run(request("p"), recorder)
    recorder.await()
    assertTrue(recorder.failure!!.contains("empty"))
  }

  @Test
  fun aBadRequestIsAFailureWithoutRunningTheModel() {
    val llm = FakeLlm()
    val recorder = Recorder()
    runner(llm).run(JSONObject(), recorder)
    recorder.await()
    assertTrue(recorder.failure!!.contains("Bad request"))
    assertNull(llm.prompt)
  }

  @Test
  fun cancelStopsTheRunningRequest() {
    val llm = FakeLlm().apply { block = true }
    val runner = runner(llm)
    val recorder = Recorder()
    runner.run(request("p"), recorder)
    assertTrue(llm.started.await(5, TimeUnit.SECONDS))
    assertTrue(runner.hasPending)
    runner.cancel()
    recorder.await()
    assertTrue(recorder.cancelled)
    assertFalse(runner.hasPending)
  }

  @Test
  fun aNewRequestCancelsTheOneThatIsRunning() {
    val llm = FakeLlm().apply { block = true }
    val runner = runner(llm)
    val first = Recorder()
    runner.run(request("one"), first)
    assertTrue(llm.started.await(5, TimeUnit.SECONDS))
    val second = Recorder()
    llm.block = false
    runner.run(request("two"), second)
    first.await()
    second.await()
    assertTrue(first.cancelled)
    assertEquals("hello", second.response!!.text())
  }

  @Test
  fun extractJsonLeavesNonJsonAlone() {
    assertEquals("no json here", LocalGemmaRunner.extractJson(" no json here "))
  }

  @Test
  fun cancelReturnsAtOnceEvenWhenStoppingTheModelBlocks() {
    // Cancel runs on the main thread, and a blocked cancel once froze the whole app.
    val llm = FakeLlm().apply { block = true }
    val stopping = CountDownLatch(1)
    val blockedCanceller = Executors.newSingleThreadExecutor()
    val runner =
      LocalGemmaRunner(
        { llm },
        {
          stopping.await(5, TimeUnit.SECONDS)
          llm
        },
        worker,
        blockedCanceller,
        sameThread,
        { announced.add(it) },
      )
    val recorder = Recorder()
    runner.run(request("p"), recorder)
    assertTrue(llm.started.await(5, TimeUnit.SECONDS))

    val started = System.nanoTime()
    runner.cancel()
    val tookMs = (System.nanoTime() - started) / 1_000_000
    assertTrue("cancel took $tookMs ms", tookMs < 1000)

    stopping.countDown()
    llm.cancel()
    recorder.await()
    blockedCanceller.shutdownNow()
  }
}
