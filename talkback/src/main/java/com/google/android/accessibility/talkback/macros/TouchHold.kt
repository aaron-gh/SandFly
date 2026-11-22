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

package com.google.android.accessibility.talkback.macros

import android.graphics.Rect
import com.google.android.accessibility.talkback.TalkBackService

/**
 * A touch held on an item, passed through to the app as a double-tap and hold is: for buttons that
 * act on a touch held down, such as a Like button that shows reactions, which the accessibility
 * long click doesn't open.
 */
object TouchHold {
  const val DEFAULT_HOLD_MS = 1000L
  private const val MAX_HOLD_MS = 10_000L

  /**
   * Touches the middle of the part of [bounds] on screen for [holdMs], then calls [done] on the
   * main thread with whether the touch was performed. Needs Android 11. Called on the main thread.
   */
  @JvmStatic
  fun perform(service: TalkBackService, bounds: Rect, holdMs: Long, done: (Boolean) -> Unit) {
    val centre = PassThroughTouch.visibleCentre(service, bounds) ?: return done(false)
    val hold =
      TouchSegment(centre.x, centre.y, centre.x, centre.y, holdMs.coerceIn(1L, MAX_HOLD_MS), false)
    if (!PassThroughTouch.perform(service, listOf(hold), stopped = { false }, done)) done(false)
  }
}
