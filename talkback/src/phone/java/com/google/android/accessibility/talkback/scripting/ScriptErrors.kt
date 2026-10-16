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

package com.google.android.accessibility.talkback.scripting

class ScriptApiException(message: String) : RuntimeException(message)

class ManifestException(message: String) : IllegalArgumentException(message)

fun apiError(message: String): Nothing = throw ScriptApiException(message)

inline fun apiCheck(condition: Boolean, message: () -> String) {
  if (!condition) apiError(message())
}

inline fun <T> asApiError(block: () -> T): T =
  try {
    block()
  } catch (e: IllegalArgumentException) {
    apiError(e.message.orEmpty())
  }

fun manifestError(message: String): Nothing = throw ManifestException(message)

inline fun manifestCheck(condition: Boolean, message: () -> String) {
  if (!condition) manifestError(message())
}

inline fun <T> asManifestError(block: () -> T): T =
  try {
    block()
  } catch (e: IllegalArgumentException) {
    manifestError(e.message.orEmpty())
  }
