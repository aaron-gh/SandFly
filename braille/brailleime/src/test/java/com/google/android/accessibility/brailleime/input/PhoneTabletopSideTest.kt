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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneTabletopSideTest {
  private val rotation0 = 0

  /** Held up facing the user with the port on the left, or from behind with it on the right. */
  private val heldPortLeftFacing = Orientation.LANDSCAPE

  private val side = PhoneTabletopSide()

  /** Lays the phone flat in portrait, as last held at [heldAtMs]. */
  private fun layFlat(held: Orientation, heldAtMs: Long, nowMs: Long) =
    side.decideIfNeeded(held, heldAtMs, /* portrait= */ true, rotation0, nowMs)

  @Test
  fun heldUpFacingTheUser_givesThatSide() {
    assertTrue(layFlat(heldPortLeftFacing, heldAtMs = 100, nowMs = 200))
    assertEquals(PortPosition.LEFT, side.port)
    assertFalse(side.portOnRight)
  }

  @Test
  fun tiltedInTheHands_typingDoesNotCount() {
    // The #73 posture: typed nearly flat in the hands, tipped toward the user past the screen-away
    // angle, which reads as held up, and a key typed while tipped.
    layFlat(heldPortLeftFacing, heldAtMs = 100, nowMs = 200)
    side.onTabletopTouch()
    side.onScreenAwayTouch(portOnRight = true, heldUpright = false)
    assertNull(side.typedPortOnRight)
    assertTrue(layFlat(heldPortLeftFacing, heldAtMs = 300, nowMs = 400))
    assertEquals(PortPosition.LEFT, side.port)
  }

  @Test
  fun typedScreenAway_comesFirst() {
    side.onScreenAwayTouch(portOnRight = true, heldUpright = true)
    assertTrue(layFlat(heldPortLeftFacing, heldAtMs = 100, nowMs = 200))
    assertEquals(PortPosition.RIGHT, side.port)
    assertTrue(side.portOnRight)
  }

  @Test
  fun typedScreenAway_isKeptWhenLiftedInAPauseAndLaidFlatAgain() {
    side.onScreenAwayTouch(portOnRight = true, heldUpright = true)
    layFlat(heldPortLeftFacing, heldAtMs = 100, nowMs = 200)
    // Lifted without typing, so held up again, then dipped flat.
    assertTrue(layFlat(heldPortLeftFacing, heldAtMs = 300, nowMs = 400))
    assertEquals(PortPosition.RIGHT, side.port)
  }

  @Test
  fun typingOnTheTable_forgetsTheTypedSide() {
    side.onScreenAwayTouch(portOnRight = true, heldUpright = true)
    layFlat(heldPortLeftFacing, heldAtMs = 100, nowMs = 200)
    side.onTabletopTouch()
    assertNull(side.typedPortOnRight)
    assertTrue(layFlat(heldPortLeftFacing, heldAtMs = 300, nowMs = 400))
    assertEquals(PortPosition.LEFT, side.port)
  }

  @Test
  fun typedSide_turnsWithThePhoneOnTheTable() {
    side.onScreenAwayTouch(portOnRight = true, heldUpright = true)
    layFlat(heldPortLeftFacing, heldAtMs = 100, nowMs = 200)
    side.turn(1, tabletop = true)
    assertEquals(PortPosition.NEAR, side.port)
    assertTrue(side.portOnRight)
    assertEquals(true, side.typedPortOnRight)
    side.turn(1, tabletop = true)
    assertEquals(PortPosition.LEFT, side.port)
    assertEquals(false, side.typedPortOnRight)
    assertTrue(layFlat(heldPortLeftFacing, heldAtMs = 300, nowMs = 400))
    assertEquals(PortPosition.LEFT, side.port)
  }

  @Test
  fun typedSide_doesNotTurnWhileHeldUp() {
    side.onScreenAwayTouch(portOnRight = true, heldUpright = true)
    layFlat(heldPortLeftFacing, heldAtMs = 100, nowMs = 200)
    side.turn(2, tabletop = false)
    assertEquals(true, side.typedPortOnRight)
  }

  @Test
  fun decidesOnlyWhenSomethingNewShowsTheSide() {
    layFlat(heldPortLeftFacing, heldAtMs = 100, nowMs = 200)
    side.turn(2, tabletop = true)
    // Tilted for a moment while turning, too briefly to read as held up.
    assertFalse(layFlat(heldPortLeftFacing, heldAtMs = 100, nowMs = 400))
    assertEquals(PortPosition.RIGHT, side.port)
    // Typing in screen-away mode is something new.
    side.onScreenAwayTouch(portOnRight = false, heldUpright = true)
    assertTrue(layFlat(heldPortLeftFacing, heldAtMs = 100, nowMs = 500))
    assertEquals(PortPosition.LEFT, side.port)
  }

  @Test
  fun heldInPortrait_keepsTheLayoutsSideUntilTurnedToASide() {
    layFlat(Orientation.PORTRAIT, heldAtMs = 100, nowMs = 200)
    assertEquals(PortPosition.NEAR, side.port)
    // In portrait, the tabletop layout expects the port on the left.
    assertFalse(side.portOnRight)
    side.turn(-1, tabletop = true)
    assertEquals(PortPosition.RIGHT, side.port)
    assertTrue(side.portOnRight)
    side.turn(1, tabletop = true)
    assertEquals(PortPosition.NEAR, side.port)
    assertTrue(side.portOnRight)
  }
}
