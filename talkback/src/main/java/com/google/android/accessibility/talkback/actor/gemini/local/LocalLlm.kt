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

package com.google.android.accessibility.talkback.actor.gemini.local

/** A language model that runs on the phone and can look at one image. */
interface LocalLlm {
  /**
   * Answers [prompt], looking at [jpeg] when it is not null. Blocks until done. Throws
   * [LocalLlmCancelledException] after [cancel], and [LocalLlmException] for any other failure.
   */
  fun generate(prompt: String, jpeg: ByteArray?): String

  /**
   * Like [generate], and tells [onStage] when the model starts loading and when it starts on the
   * prompt. The calls can come on any thread. By default it reports nothing.
   */
  fun generate(prompt: String, jpeg: ByteArray?, onStage: ModelStageListener): String =
    generate(prompt, jpeg)

  /** Stops the answer that [generate] is working on, if any. Safe to call from any thread. */
  fun cancel()

  /**
   * Whether the model is most likely loaded, so that the next answer does not wait for a load. Never
   * blocks.
   */
  val isLoaded: Boolean
    get() = true
}

/** What the on-device model does for a request. */
enum class ModelStage {
  /** Loading the model, before it can start on the prompt. */
  LOADING,
  /** Working on the prompt. */
  PROCESSING,
}

fun interface ModelStageListener {
  fun onStage(stage: ModelStage)
}

open class LocalLlmException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * The model could not run for lack of memory: the phone had too little free to load it, or the
 * model's process stopped while it was working. [spokenMessage] tells the user what happened.
 */
class LocalLlmMemoryException(val spokenMessage: String) : LocalLlmException(spokenMessage)

class LocalLlmCancelledException : Exception("Cancelled")
