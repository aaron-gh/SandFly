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
 * Sounds for reading controls, edge gestures, the insertion point and grid rows, each with a
 * SandFly vibration.
 */
object NavigationSounds {
  /** The sound of each, by the name of its theme sound and sound switch. */
  @JvmField
  val SOUNDS: Map<String, Int> =
    linkedMapOf(
      "next_reading_control" to R.raw.next_reading_control,
      "previous_reading_control" to R.raw.previous_reading_control,
      "edge_gesture_bottom" to R.raw.edge_gesture_bottom,
      "edge_gesture_middle" to R.raw.edge_gesture_middle,
      "edge_gesture_top" to R.raw.edge_gesture_top,
      "insertion_point_moved" to R.raw.insertion_point_moved,
      "column_wrap" to R.raw.column_wrap,
      "column_wrap_back" to R.raw.column_wrap_back,
    )

  /** The vibration of each sound, by sound name, with the vibration's resource name. */
  val PATTERNS: Map<String, String> = SOUNDS.keys.associateWith { "${it}_pattern" }

  /** The edge gesture sounds, which watches, having no edge gestures, leave out. */
  val EDGE_GESTURES: Set<String> =
    setOf("edge_gesture_bottom", "edge_gesture_middle", "edge_gesture_top")
}
