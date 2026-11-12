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

package com.google.android.accessibility.talkback.actor.gemini.local

import com.google.android.accessibility.talkback.dictation.WhisperModels
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelServerTest {
  private val model = LocalModel.entries.first()

  @Test
  fun huggingFaceKeepsTheUrl() {
    assertEquals(model.url, ModelServer.urlOn(ModelServer.HUGGING_FACE, model.url))
    assertEquals(model.url, ModelServer.urlOn(null, model.url))
  }

  @Test
  fun theMirrorServesTheSamePath() {
    assertEquals(
      "https://hf-mirror.com/${model.repo}/resolve/${model.revision}/${model.fileName}",
      ModelServer.urlOn(ModelServer.HF_MIRROR, model.url),
    )
    val whisper = WhisperModels.Model.TINY
    assertEquals(
      "https://hf-mirror.com/ggerganov/whisper.cpp/resolve/main/ggml-tiny-q5_1.bin",
      ModelServer.urlOn(ModelServer.HF_MIRROR, whisper.url),
    )
  }

  @Test
  fun otherServersAreLeftAlone() {
    val futo = WhisperModels.Model.FUTO_TINY.url
    assertEquals(futo, ModelServer.urlOn(ModelServer.HF_MIRROR, futo))
  }
}
