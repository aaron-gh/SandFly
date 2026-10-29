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

package com.google.android.accessibility.talkback.individualfeedback

import com.google.android.accessibility.talkback.R

/**
 * Sounds for SandFly AI's agent: a task starting, finishing, asking the person something, stopping
 * at an action the safety check blocks, and failing. SandFly has no sounds or vibrations of its own
 * for them: each plays only what a sound theme gives it.
 */
object AgentSounds {
  /** The ID that stands for each sound, by the name of its theme sound and sound switch. */
  @JvmField
  val SOUNDS: Map<String, Int> =
    linkedMapOf(
      "agentic_interaction_start" to R.id.agentic_interaction_start,
      "agentic_interaction_complete" to R.id.agentic_interaction_complete,
      "agentic_interaction_ask" to R.id.agentic_interaction_ask,
      "agentic_interaction_sensitive_stop" to R.id.agentic_interaction_sensitive_stop,
      "agentic_interaction_fail" to R.id.agentic_interaction_fail,
    )
}
