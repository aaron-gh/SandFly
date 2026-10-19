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

package com.google.android.accessibility.talkback.gesture

import android.view.MotionEvent
import kotlin.math.hypot

/**
 * Recognizes a one-finger tap on the keyboard, which types the key under it with the Tap to type
 * typing method. A touch stops being one when another finger lands or the finger moves further than
 * [touchSlop], and is then explored or recognized as a gesture as usual.
 */
class TapToTypeTracker(private val touchSlop: Float) {
  /** Says whether a point on the screen is on the keyboard. */
  fun interface Keyboard {
    fun contains(x: Float, y: Float): Boolean
  }

  private var possibleTap = false
  private var downX = 0f
  private var downY = 0f

  /**
   * Follows [event]. Returns true for the event that lifts the finger of a tap that went down on
   * [keyboard].
   */
  fun onMotionEvent(event: MotionEvent, keyboard: Keyboard): Boolean {
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        downX = event.x
        downY = event.y
        possibleTap = keyboard.contains(downX, downY)
      }
      MotionEvent.ACTION_MOVE ->
        if (possibleTap && hypot(event.x - downX, event.y - downY) > touchSlop) {
          possibleTap = false
        }
      MotionEvent.ACTION_UP -> {
        val tap = possibleTap
        possibleTap = false
        return tap
      }
      else -> possibleTap = false
    }
    return false
  }

  fun clear() {
    possibleTap = false
  }
}
