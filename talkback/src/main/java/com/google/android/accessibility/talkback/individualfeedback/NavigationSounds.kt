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

package com.google.android.accessibility.talkback.individualfeedback

import com.google.android.accessibility.talkback.R

/**
 * Sounds for reading controls, edge gestures, the insertion point and grid rows. SandFly has no
 * sounds of its own for them: each plays only the sound a sound theme gives it. The edge gesture
 * start and middle have SandFly vibrations; the others vibrate only with a theme's vibration.
 */
object NavigationSounds {
  /** The ID that stands for each sound, by the name of its theme sound and sound switch. */
  @JvmField
  val SOUNDS: Map<String, Int> =
    linkedMapOf(
      "next_reading_control" to R.id.next_reading_control,
      "previous_reading_control" to R.id.previous_reading_control,
      "edge_gesture_start" to R.id.edge_gesture_start,
      "edge_gesture_middle" to R.id.edge_gesture_middle,
      "edge_gesture_end" to R.id.edge_gesture_end,
      "insertion_point_moved" to R.id.insertion_point_moved,
      "column_wrap" to R.id.column_wrap,
      "column_wrap_back" to R.id.column_wrap_back,
    )

  /** The sounds with a SandFly vibration, by sound name, with the vibration's resource name. */
  val PATTERNS: Map<String, String> =
    mapOf(
      "edge_gesture_start" to "edge_gesture_start_pattern",
      "edge_gesture_middle" to "edge_gesture_middle_pattern",
    )

  /** The edge gesture sounds, which watches, having no edge gestures, leave out. */
  val EDGE_GESTURES: Set<String> =
    setOf("edge_gesture_start", "edge_gesture_middle", "edge_gesture_end")
}
