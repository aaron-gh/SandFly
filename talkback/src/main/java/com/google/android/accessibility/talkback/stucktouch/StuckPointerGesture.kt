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

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.Display

/**
 * Builds a gesture that lifts injected pointers which Android's input dispatcher holds as down.
 *
 * The dispatcher checks every injected event against the pointers it holds as down for the
 * event's device, and rejects events that don't match. Touch exploration injects its events as
 * device -1. When it leaves pointers down, every hover event it injects afterwards is rejected, and
 * touch exploration stops until the phone restarts. Gesture events from `dispatchGesture` are
 * also device -1, so a gesture can lift those pointers.
 *
 * The injector gives each new stroke the lowest unused pointer id, and keeps the ids of lifted
 * strokes reserved until all strokes are up. The gesture opens strokes for ids 0 to the highest
 * stuck id one at a time, keeps open each id the dispatcher holds, lifts the others as soon as
 * another stroke is open, and then lifts the held ones. The dispatcher rejects every event until
 * the open pointers match what it holds, and then accepts the lifts. Strokes are outside the
 * display, so an event the dispatcher accepts touches no window.
 *
 * Touch exploration must be off while the gesture runs, or it turns the strokes into other events.
 */
object StuckPointerGesture {

  /** Start and end time of the stroke that gets the pointer id at its index. */
  data class Stroke(val start: Long, val end: Long)

  /** Time between successive stroke starts and ends. */
  private const val STEP_MS = 20L

  /**
   * The injector gives every stroke past the tenth id 10 while ids 0 to 9 are taken, so a gesture
   * can reach ids 0 to 10 only, one stroke each.
   */
  const val MAX_STROKES = 11

  /** Outside any display, in any rotation. Gesture coordinates can't be negative. */
  private const val OFF_SCREEN = 20000f

  /**
   * Returns one stroke per pointer id from 0 to the highest id in [stuck], or an empty list if
   * [stuck] is empty or has an id that a gesture of at most [maxStrokes] strokes can't reach.
   */
  fun plan(stuck: Set<Int>, maxStrokes: Int = MAX_STROKES): List<Stroke> {
    val max = stuck.maxOrNull() ?: return emptyList()
    if (stuck.min() < 0 || max >= minOf(maxStrokes, MAX_STROKES)) {
      return emptyList()
    }
    // What the dispatcher holds, as it changes. A POINTER_DOWN it accepts adds that pointer.
    val held = stuck.toMutableSet()
    val open = mutableListOf<Int>()
    val starts = LongArray(max + 1)
    val ends = LongArray(max + 1)
    var time = 0L
    for (id in 0..max) {
      starts[id] = time
      time += STEP_MS
      if (open.isNotEmpty() && held.size == open.size && id !in held) {
        held.add(id)
      }
      open.add(id)
      // Lift every open pointer the dispatcher doesn't hold, keeping one open so that ids stay
      // reserved. The dispatcher rejects those lifts.
      for (i in open.indices.reversed()) {
        if (open.size > 1 && open[i] !in held) {
          ends[open[i]] = time
          time += STEP_MS
          open.removeAt(i)
        }
      }
    }
    if (held != open.toSet()) {
      return emptyList()
    }
    // Lift the held pointers one at a time. Each lift matches the dispatcher exactly.
    for (id in open) {
      ends[id] = time
      time += STEP_MS
    }
    return (0..max).map { Stroke(starts[it], ends[it]) }
  }

  /** Returns the gesture for [stuck] on [displayId], or null if there is none. */
  fun build(stuck: Set<Int>, displayId: Int): GestureDescription? {
    // Android 8 to 10 allow 10 strokes, later versions 20.
    val strokes = plan(stuck, GestureDescription.getMaxStrokeCount())
    val canSetDisplay = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    if (strokes.isEmpty() || (!canSetDisplay && displayId != Display.DEFAULT_DISPLAY)) {
      return null
    }
    val builder = GestureDescription.Builder()
    strokes.forEachIndexed { i, stroke ->
      val path = Path().apply { moveTo(OFF_SCREEN + i, OFF_SCREEN) }
      builder.addStroke(
        GestureDescription.StrokeDescription(path, stroke.start, stroke.end - stroke.start)
      )
    }
    if (canSetDisplay) {
      builder.setDisplayId(displayId)
    }
    return builder.build()
  }
}
