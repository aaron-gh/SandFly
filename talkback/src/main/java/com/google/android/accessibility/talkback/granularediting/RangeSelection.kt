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

package com.google.android.accessibility.talkback.granularediting

/**
 * The item of a list that Start selection marked. End selection on another item selects every
 * item from the marked one to that one, both included.
 */
class RangeSelection {
  /** The marked item, or [NONE]. */
  var marked: Int = NONE
    private set

  val isStarted: Boolean
    get() = marked != NONE

  fun start(index: Int) {
    marked = index
  }

  /**
   * The items from the marked one to [index], both included, in list order, or [index] alone when
   * no item is marked. Clears the mark.
   */
  fun end(index: Int): IntRange {
    val from = if (isStarted) marked else index
    clear()
    return minOf(from, index)..maxOf(from, index)
  }

  fun clear() {
    marked = NONE
  }

  companion object {
    const val NONE = -1
  }
}
