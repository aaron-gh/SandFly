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

import com.google.android.accessibility.talkback.actor.gemini.local.ModelStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecognitionTonesTest {
  private val calls = mutableListOf<String>()
  private var loaded = false

  private val tones =
    RecognitionTones(
      object : RecognitionTones.Tones {
        override fun play(modelLoading: Boolean) {
          calls.add(if (modelLoading) "play loading" else "play working")
        }

        override fun setModelLoading(modelLoading: Boolean) {
          calls.add(if (modelLoading) "switch loading" else "switch working")
        }

        override fun stop() {
          calls.add("stop")
        }
      }
    ) {
      loaded
    }

  @Test
  fun theFirstFrameLoadsTheModelThenIsDescribed() {
    tones.onRequest()
    tones.onStage(ModelStage.LOADING)
    tones.onStage(ModelStage.PROCESSING)
    tones.onRequestEnd(gotAnswer = true)
    assertEquals(
      listOf("play loading", "switch loading", "switch working", "stop"),
      calls,
    )
  }

  @Test
  fun aLoadedModelPlaysTheWorkingTonesForTheFirstFrame() {
    loaded = true
    tones.onRequest()
    tones.onStage(ModelStage.PROCESSING)
    tones.onRequestEnd(gotAnswer = true)
    assertEquals(listOf("play working", "switch working", "stop"), calls)
  }

  @Test
  fun framesAfterTheFirstAnswerAreQuiet() {
    loaded = true
    tones.onRequest()
    tones.onRequestEnd(gotAnswer = true)
    calls.clear()
    tones.onRequest()
    tones.onStage(ModelStage.PROCESSING)
    tones.onRequestEnd(gotAnswer = true)
    assertTrue(calls.isEmpty())
  }

  @Test
  fun aReloadAfterTheFirstAnswerPlaysTheLoadingTonesOnly() {
    loaded = true
    tones.onRequest()
    tones.onRequestEnd(gotAnswer = true)
    calls.clear()
    tones.onRequest()
    tones.onStage(ModelStage.LOADING)
    tones.onStage(ModelStage.PROCESSING)
    tones.onRequestEnd(gotAnswer = true)
    assertEquals(listOf("play loading", "stop"), calls)
  }

  @Test
  fun aFailedRequestKeepsTheWorkingTonesForTheNext() {
    loaded = true
    tones.onRequest()
    tones.onRequestEnd(gotAnswer = false)
    tones.onRequest()
    assertEquals(listOf("play working", "stop", "play working"), calls)
  }

  @Test
  fun closeStopsTheTonesForGood() {
    tones.onRequest()
    tones.close()
    tones.onRequest()
    tones.onStage(ModelStage.LOADING)
    tones.onRequestEnd(gotAnswer = false)
    assertEquals(listOf("play loading", "stop"), calls)
  }
}
