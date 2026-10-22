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

import com.google.android.accessibility.talkback.Pipeline

/**
 * What the handwriting keyboard, which runs in SandFly's process, uses of SandFly: its speech and
 * feedback, while SandFly is on. The keyboard tells SandFly when it is open, since it echoes what
 * it types itself: the app's field is hidden behind it, and SandFly doesn't read hidden fields.
 */
object HandwritingKeyboardHost {
  /** SandFly's feedback, or null while SandFly is off. */
  @JvmStatic
  @Volatile
  var feedback: Pipeline.FeedbackReturner? = null

  /** Whether the keyboard is open, so that text changes come from it. */
  @JvmStatic
  @Volatile
  var isTyping: Boolean = false
}
