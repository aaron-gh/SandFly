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

/**
 * Sounds for SandFly Pro's agent: a task starting, finishing, asking the person something,
 * stopping at an action the safety check blocks, and failing. They are SandFly's own raw sounds,
 * made by tools/sounds.py, which a sound theme can replace. They have no SandFly vibrations:
 * they vibrate only with a theme's.
 */
object AgentSounds {
  /** The resource names of the sounds, which are also their theme sound and switch names. */
  @JvmField
  val NAMES: Set<String> =
    linkedSetOf(
      "agentic_interaction_start",
      "agentic_interaction_complete",
      "agentic_interaction_ask",
      "agentic_interaction_sensitive_stop",
      "agentic_interaction_fail",
    )
}
