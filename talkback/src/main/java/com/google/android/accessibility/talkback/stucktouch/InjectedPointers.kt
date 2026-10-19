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

package com.google.android.accessibility.talkback.stucktouch

/**
 * Reads the injected pointers that the input dispatcher holds as down, from `dumpsys input`.
 *
 * Samsung's builds print the verifier for injected events:
 * ```
 *   Filter verifier on display 0:
 *     Touching pointer IDs by device: {DeviceId(-1): {4, 1}}
 * ```
 * AOSP doesn't, so on other phones the pointers can't be read.
 */
object InjectedPointers {

  private val HEADER = Regex("""Filter verifier on display (\d+):""")
  private const val TOUCHING = "Touching pointer IDs by device:"
  private val ACCESSIBILITY_DEVICE = Regex("""DeviceId\(-1\): \{([\d, ]*)\}""")

  /**
   * Returns the pointer ids held down for device -1, by display, leaving out displays with none.
   * Returns null if the output has no injected event verifier.
   */
  fun parse(lines: Sequence<String>): Map<Int, Set<Int>>? {
    var found = false
    var display: Int? = null
    val stuck = mutableMapOf<Int, Set<Int>>()
    for (line in lines) {
      val text = line.trim()
      val header = HEADER.matchEntire(text)
      if (header != null) {
        found = true
        display = header.groupValues[1].toIntOrNull()
        continue
      }
      val current = display ?: continue
      if (text.startsWith(TOUCHING)) {
        val ids =
          ACCESSIBILITY_DEVICE.find(text)
            ?.groupValues
            ?.get(1)
            ?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.toSet()
            .orEmpty()
        if (ids.isNotEmpty()) {
          stuck[current] = ids
        }
        display = null
      }
    }
    return if (found) stuck else null
  }
}
