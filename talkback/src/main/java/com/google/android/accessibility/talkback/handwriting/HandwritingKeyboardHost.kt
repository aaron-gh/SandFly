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
package com.google.android.accessibility.talkback.handwriting

import android.os.SystemClock
import com.google.android.accessibility.talkback.Pipeline

/**
 * What the handwriting keyboard, which runs in SandFly's process, uses of SandFly: its speech and
 * feedback, while SandFly is on. The keyboard tells SandFly when it types, so that SandFly doesn't
 * also echo what the keyboard has already said.
 */
object HandwritingKeyboardHost {
  /** SandFly's feedback, or null while SandFly is off. */
  @JvmStatic
  @Volatile
  var feedback: Pipeline.FeedbackReturner? = null

  @Volatile private var typedAt = 0L

  /** Notes that the keyboard is changing the text now. */
  @JvmStatic
  fun noteTyping() {
    typedAt = SystemClock.uptimeMillis()
  }

  /** Whether a text change at [eventTime], in uptime milliseconds, came from the keyboard. */
  @JvmStatic
  fun typedAround(eventTime: Long): Boolean = eventTime - typedAt in -ECHO_WINDOW_MS..ECHO_WINDOW_MS

  /** How far apart the keyboard's change and the app's text change event can be. */
  private const val ECHO_WINDOW_MS = 1_000L
}
