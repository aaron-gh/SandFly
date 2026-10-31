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
 * Sounds for reading controls, edge gestures, the insertion point and grid rows. Only the edge
 * gestures have sounds of SandFly's own; the others play only the sound a sound theme gives them.
 * The edge gesture middle and top have SandFly vibrations; the others vibrate only with a theme's
 * vibration.
 */
object NavigationSounds {
  /**
   * The resource ID of each sound, by the name of its theme sound and sound switch: the raw sound,
   * or, for sounds without one, an ID that stands for it.
   */
  @JvmField
  val SOUNDS: Map<String, Int> =
    linkedMapOf(
      "next_reading_control" to R.id.next_reading_control,
      "previous_reading_control" to R.id.previous_reading_control,
      "edge_gesture_bottom" to R.raw.edge_gesture_bottom,
      "edge_gesture_middle" to R.raw.edge_gesture_middle,
      "edge_gesture_top" to R.raw.edge_gesture_top,
      "insertion_point_moved" to R.id.insertion_point_moved,
      "column_wrap" to R.id.column_wrap,
      "column_wrap_back" to R.id.column_wrap_back,
    )

  /** The sounds with a SandFly vibration, by sound name, with the vibration's resource name. */
  val PATTERNS: Map<String, String> =
    mapOf(
      "edge_gesture_middle" to "edge_gesture_middle_pattern",
      "edge_gesture_top" to "edge_gesture_top_pattern",
    )

  /** The edge gesture sounds, which watches, having no edge gestures, leave out. */
  val EDGE_GESTURES: Set<String> =
    setOf("edge_gesture_bottom", "edge_gesture_middle", "edge_gesture_top")

  /** The sounds that play only a theme's sound. */
  val WITHOUT_OWN_SOUND: Set<String> = SOUNDS.keys - EDGE_GESTURES
}
