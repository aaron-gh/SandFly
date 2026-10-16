/*
 * Copyright 2026 Backtalk contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.talkback.scripting

import android.os.SystemClock

/** All listeners share the caller's deadline, including time already spent in the queue. */
internal fun <T> rewriteSpeechChain(
  input: String,
  listeners: List<T>,
  deadline: Long,
  now: () -> Long = SystemClock::uptimeMillis,
  dispatch: (T, String, Long) -> String?,
): String? {
  var text = input
  for (listener in listeners) {
    val remaining = deadline - now()
    if (remaining <= 0) return null
    val result = dispatch(listener, text, remaining)
    // The caller has already fallen back. Do not start more hooks or return a late rewrite.
    if (now() >= deadline) return null
    if (result != null) text = result
  }
  return text.takeIf { it != input }
}
