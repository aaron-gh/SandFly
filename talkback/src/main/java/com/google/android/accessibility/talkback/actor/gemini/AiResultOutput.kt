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

/**
 * Whether the result of a Describe screen or Describe image request is spoken instead of shown in
 * a window.
 *
 * A command sets the output with [request] before its request starts, possibly after a consent
 * dialog. The next user-requested description takes it with [claim] under its request ID, and the
 * result display asks [takeSpoken] for that ID.
 */
object AiResultOutput {
  // Request IDs kept at most; a request whose result never arrives is dropped after this many more.
  private const val MAX_SPOKEN = 8

  private var speakNext = false
  private val spoken = ArrayDeque<Int>()

  /** Sets whether the next user-requested description is spoken. */
  @JvmStatic
  @Synchronized
  fun request(speak: Boolean) {
    speakNext = speak
  }

  /** Gives the request [requestId] the output last requested, and resets it to the window. */
  @JvmStatic
  @Synchronized
  fun claim(requestId: Int) {
    if (!speakNext) {
      return
    }
    speakNext = false
    spoken.remove(requestId)
    spoken.addLast(requestId)
    while (spoken.size > MAX_SPOKEN) {
      spoken.removeFirst()
    }
  }

  /** Returns whether the result of [requestId] is spoken, and forgets the request. */
  @JvmStatic
  @Synchronized
  fun takeSpoken(requestId: Int): Boolean = spoken.remove(requestId)

  /** Forgets everything, as when the service stops. */
  @JvmStatic
  @Synchronized
  fun clear() {
    speakNext = false
    spoken.clear()
  }
}
