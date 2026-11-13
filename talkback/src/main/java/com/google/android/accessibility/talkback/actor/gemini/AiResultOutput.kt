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

  // Who gets the text of the next spoken description, and of the requests that claimed one.
  private var nextReceiver: ((String?) -> Unit)? = null
  private val receivers = LinkedHashMap<Int, (String?) -> Unit>()

  /**
   * Gives the text of the next spoken description to [receiver], or null when it fails, as a voice
   * command that sends or copies it needs. A description shown in a window gives nothing.
   */
  @JvmStatic
  @Synchronized
  fun expect(receiver: (String?) -> Unit) {
    nextReceiver = receiver
  }

  /** Stops giving a description to [receiver], as when it waited too long. */
  @JvmStatic
  @Synchronized
  fun forget(receiver: (String?) -> Unit) {
    if (nextReceiver === receiver) nextReceiver = null
    receivers.values.removeAll { it === receiver }
  }

  /**
   * Whether a spoken description will be given to [receiver]: one was requested since [expect], or a
   * request took it.
   */
  @JvmStatic
  @Synchronized
  fun isWaiting(receiver: (String?) -> Unit): Boolean =
    (speakNext && nextReceiver === receiver) || receivers.values.any { it === receiver }

  /** Gives [text], the spoken result of [requestId], or null when it failed, to its receiver. */
  @JvmStatic
  fun deliver(requestId: Int, text: String?) {
    val receiver = synchronized(this) { receivers.remove(requestId) } ?: return
    receiver(text)
  }

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
    nextReceiver?.let {
      nextReceiver = null
      receivers[requestId] = it
      while (receivers.size > MAX_SPOKEN) {
        receivers.remove(receivers.keys.first())
      }
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
    nextReceiver = null
    receivers.clear()
  }
}
