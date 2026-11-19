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

package com.google.android.accessibility.brailleime.input

import com.google.android.accessibility.brailleime.OrientationMonitor.Orientation

/**
 * Where a phone's charging port is in tabletop mode, apart from the view so it can be tested. It is
 * decided when the phone is laid flat, by [DotsOrientation.decidePhoneTabletopPort], and turns with
 * the phone on the table.
 *
 * The side the user typed with in screen-away mode comes first when it is decided, as tipping the
 * phone flat keeps the port on the same side of the user. It only counts when the phone was held
 * upright in landscape, so that tipping a phone held nearly flat in the hands past the screen-away
 * angle, or holding it up in portrait to read it, and touching the screen does not count. It is
 * kept, and turns with the phone, until the user types on the table or holds the phone up in
 * portrait, so that lifting the phone in a pause and laying it flat again keeps it.
 */
class PhoneTabletopSide {
  /**
   * The side of the charging port the user last typed with in screen-away mode, turned with the
   * phone on the table since, or null if they have typed on the table since.
   */
  var typedPortOnRight: Boolean? = null
    private set

  /** Whether the user typed in screen-away mode since the side was last decided. */
  private var typedSinceDecided = false

  /** When the user last typed in screen-away mode, in uptime milliseconds. */
  private var typedAtMs = -1L

  /** When the side was last decided, in uptime milliseconds, or -1 if not yet. */
  private var decidedAtMs = -1L

  /** Where the charging port is. */
  var port = PortPosition.LEFT
    private set

  /**
   * The side of the charging port that the layout uses: the side of [port], or the last side while
   * the port is toward or away from the user.
   */
  var portOnRight = false
    private set

  /**
   * The user touched the screen in screen-away mode at [nowMs], with the layout expecting the
   * charging port on this side. It counts as screen-away typing only if the phone was held upright
   * in screen-away mode and is held in landscape, as [heldUprightInLandscape] says.
   */
  fun onScreenAwayTouch(portOnRight: Boolean, heldUprightInLandscape: Boolean, nowMs: Long) {
    if (!heldUprightInLandscape) {
      return
    }
    typedPortOnRight = portOnRight
    typedSinceDecided = true
    typedAtMs = nowMs
  }

  /** The user touched the screen in tabletop mode, so screen-away typing is no longer the last. */
  fun onTabletopTouch() {
    typedPortOnRight = null
  }

  /**
   * Decides the side again when something new shows it: see
   * [DotsOrientation.shouldDecideTabletopAgain]. Returns whether it decided.
   */
  fun decideIfNeeded(
    lastHeld: Orientation,
    lastHeldSeenMs: Long,
    portrait: Boolean,
    rotation: Int,
    nowMs: Long,
  ): Boolean {
    if (
      !DotsOrientation.shouldDecideTabletopAgain(decidedAtMs, typedSinceDecided, lastHeldSeenMs)
    ) {
      return false
    }
    if (
      typedPortOnRight != null &&
        lastHeldSeenMs > typedAtMs &&
        DotsOrientation.heldPortOnRight(lastHeld) == null
    ) {
      // Held up in portrait since typing, so the port is toward or away from the user.
      typedPortOnRight = null
    }
    port = DotsOrientation.decidePhoneTabletopPort(typedPortOnRight, lastHeld, portrait, rotation)
    portOnRight =
      DotsOrientation.phonePortOnRight(
        port,
        DotsOrientation.tabletopLayoutExpectsPortOnRight(portrait, rotation),
      )
    typedSinceDecided = false
    decidedAtMs = nowMs
    return true
  }

  /**
   * The phone turned this many quarter turns clockwise, seen from above. The typed side only turns
   * with it in [tabletop] mode, as a phone held up gives no reliable heading, and laying it flat
   * decides the side afresh.
   */
  fun turn(quarters: Int, tabletop: Boolean) {
    port = DotsOrientation.turnPortPosition(port, quarters)
    portOnRight = DotsOrientation.phonePortOnRight(port, portOnRight)
    if (
      tabletop &&
        typedPortOnRight != null &&
        (port == PortPosition.LEFT || port == PortPosition.RIGHT)
    ) {
      typedPortOnRight = portOnRight
    }
  }
}
