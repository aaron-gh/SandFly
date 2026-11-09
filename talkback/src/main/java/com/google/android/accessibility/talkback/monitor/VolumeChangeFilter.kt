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

package com.google.android.accessibility.talkback.monitor

/** Classifies reported volume changes for Volume change feedback. */
class VolumeChangeFilter {
  enum class Kind {
    /** The volume is the same as before. */
    NONE,
    /** SandFly made the change. */
    OWN,
    /** Another report of the change just classified, for a stream sharing the same volume. */
    REPEAT,
    /** A new step made by the user. */
    STEP,
  }

  private var ownUntil = NEVER
  private var lastStream = -1
  private var lastValue = -1
  private var lastAt = NEVER

  /** Records that SandFly is changing a volume at [now], on any stream. */
  fun noteOwnChange(now: Long) {
    ownUntil = now + OWN_CHANGE_MS
  }

  /** Classifies a change of [stream] from [prevValue] to [value] reported at [now]. */
  fun classify(stream: Int, value: Int, prevValue: Int, now: Long): Kind {
    if (value == prevValue) {
      return Kind.NONE
    }
    val repeated =
      stream == lastStream && value == lastValue && lastAt != NEVER && now - lastAt < REPEAT_MS
    lastStream = stream
    lastValue = value
    lastAt = now
    return when {
      now < ownUntil -> Kind.OWN
      repeated -> Kind.REPEAT
      else -> Kind.STEP
    }
  }

  private companion object {
    private const val NEVER = Long.MIN_VALUE

    /** How long after SandFly changes a volume the reported changes are SandFly's. */
    private const val OWN_CHANGE_MS = 500L

    /** How long Android can take to report one change for each stream that shares the volume. */
    private const val REPEAT_MS = 200L
  }
}
