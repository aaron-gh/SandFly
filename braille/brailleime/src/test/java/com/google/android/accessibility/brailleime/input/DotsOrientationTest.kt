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

class DotsOrientationTest {
  private val rotation0 = 0
  private val rotation90 = 1
  private val rotation180 = 2
  private val rotation270 = 3

  @Test
  fun rotationForDegrees_matchesWhatAutoRotateChoseOnAFold() {
    // Readings and rotations logged on the inner screen of a Galaxy Fold with auto-rotate on.
    assertEquals(rotation0, DotsOrientation.rotationForDegrees(16)) // Port down.
    assertEquals(rotation90, DotsOrientation.rotationForDegrees(297)) // Port on the right.
    assertEquals(rotation270, DotsOrientation.rotationForDegrees(110)) // Port on the left.
    assertEquals(rotation180, DotsOrientation.rotationForDegrees(223)) // Port up.
  }

  @Test
  fun rotationForDegrees_roundsNearlyUprightToUpright() {
    assertEquals(rotation0, DotsOrientation.rotationForDegrees(344))
    assertEquals(rotation0, DotsOrientation.rotationForDegrees(359))
    assertEquals(rotation0, DotsOrientation.rotationForDegrees(0))
  }

  @Test
  fun quarterTurns_isTheDifferenceFromTheScreenAsDisplayed() {
    assertEquals(0, DotsOrientation.quarterTurns(rotation90, rotation90))
    assertEquals(1, DotsOrientation.quarterTurns(rotation90, rotation0))
    assertEquals(3, DotsOrientation.quarterTurns(rotation0, rotation90))
    assertEquals(2, DotsOrientation.quarterTurns(rotation270, rotation90))
  }

  @Test
  fun turnRotation_turnsTheScreenAgainstTheDevice() {
    assertEquals(rotation270, DotsOrientation.turnRotation(rotation0, 1))
    assertEquals(rotation90, DotsOrientation.turnRotation(rotation0, -1))
    assertEquals(rotation180, DotsOrientation.turnRotation(rotation0, 2))
    assertEquals(rotation90, DotsOrientation.turnRotation(rotation90, 4))
  }

  @Test
  fun turnPortPosition_goesRoundClockwiseSeenFromAbove() {
    assertEquals(PortPosition.LEFT, DotsOrientation.turnPortPosition(PortPosition.NEAR, 1))
    assertEquals(PortPosition.RIGHT, DotsOrientation.turnPortPosition(PortPosition.NEAR, -1))
    assertEquals(PortPosition.FAR, DotsOrientation.turnPortPosition(PortPosition.LEFT, 1))
    assertEquals(PortPosition.RIGHT, DotsOrientation.turnPortPosition(PortPosition.LEFT, 2))
    assertEquals(PortPosition.LEFT, DotsOrientation.turnPortPosition(PortPosition.RIGHT, -2))
    assertEquals(PortPosition.NEAR, DotsOrientation.turnPortPosition(PortPosition.RIGHT, 1))
    assertEquals(PortPosition.FAR, DotsOrientation.turnPortPosition(PortPosition.FAR, 4))
  }

  @Test
  fun turnPortPosition_matchesHowATabletTurns() {
    for (rotation in 0..3) {
      for (quarters in -2..2) {
        assertEquals(
          DotsOrientation.tabletPortPosition(DotsOrientation.turnRotation(rotation, quarters), true),
          DotsOrientation.turnPortPosition(
            DotsOrientation.tabletPortPosition(rotation, true),
            quarters,
          ),
        )
      }
    }
  }

  @Test
  fun layoutToScreen_withoutTurnsLeavesTheLayoutAlone() {
    assertPoint(3f, 4f, map(0, 100f, 200f, 3f, 4f))
  }

  @Test
  fun layoutToScreen_aQuarterTurnPointsTheLayoutsTopToTheRight() {
    val width = 100f
    val height = 200f
    // The sideways layout is height wide and width tall.
    assertPoint(width, 0f, map(1, width, height, 0f, 0f))
    assertPoint(0f, height, map(1, width, height, height, width))
    val top = map(1, width, height, 0f, -1f)
    val origin = map(1, width, height, 0f, 0f)
    assertPoint(1f, 0f, floatArrayOf(top[0] - origin[0], top[1] - origin[1]))
  }

  @Test
  fun layoutToScreen_aHalfTurnSwapsTheCorners() {
    assertPoint(100f, 200f, map(2, 100f, 200f, 0f, 0f))
    assertPoint(0f, 0f, map(2, 100f, 200f, 100f, 200f))
  }

  @Test
  fun layoutToScreen_threeQuarterTurnsPointTheLayoutsTopToTheLeft() {
    val width = 100f
    val height = 200f
    assertPoint(0f, height, map(3, width, height, 0f, 0f))
    assertPoint(width, 0f, map(3, width, height, height, width))
  }

  @Test
  fun layoutToScreen_keepsEveryCornerOnTheScreen() {
    val width = 100f
    val height = 200f
    for (turns in 0..3) {
      val layoutWidth = if (turns % 2 == 1) height else width
      val layoutHeight = if (turns % 2 == 1) width else height
      for (x in listOf(0f, layoutWidth)) {
        for (y in listOf(0f, layoutHeight)) {
          val point = map(turns, width, height, x, y)
          assertTrue("turns $turns", point[0] in 0f..width && point[1] in 0f..height)
        }
      }
    }
  }

  @Test
  fun screenAwayLayout_expectsThePortOnTheRightInPortrait() {
    assertTrue(DotsOrientation.screenAwayLayoutExpectsPortOnRight(true, rotation0))
    assertTrue(DotsOrientation.screenAwayLayoutExpectsPortOnRight(false, rotation270))
    assertFalse(DotsOrientation.screenAwayLayoutExpectsPortOnRight(false, rotation90))
  }

  @Test
  fun tabletopLayout_expectsThePortOnTheLeftInPortrait() {
    assertFalse(DotsOrientation.tabletopLayoutExpectsPortOnRight(true, rotation0))
    assertTrue(DotsOrientation.tabletopLayoutExpectsPortOnRight(false, rotation90))
    assertFalse(DotsOrientation.tabletopLayoutExpectsPortOnRight(false, rotation270))
  }

  @Test
  fun heldPortOnRight_onlyInLandscape() {
    assertEquals(true, DotsOrientation.heldPortOnRight(Orientation.LANDSCAPE))
    assertEquals(false, DotsOrientation.heldPortOnRight(Orientation.REVERSE_LANDSCAPE))
    assertNull(DotsOrientation.heldPortOnRight(Orientation.PORTRAIT))
    assertNull(DotsOrientation.heldPortOnRight(Orientation.UNKNOWN))
  }

  @Test
  fun laidFlatPortPosition_takesItThatTheScreenFacedTheUser() {
    // Held up facing the user with the right edge down, the port is on the left.
    assertEquals(PortPosition.LEFT, DotsOrientation.laidFlatPortPosition(Orientation.LANDSCAPE))
    assertEquals(
      PortPosition.RIGHT,
      DotsOrientation.laidFlatPortPosition(Orientation.REVERSE_LANDSCAPE),
    )
    assertEquals(PortPosition.NEAR, DotsOrientation.laidFlatPortPosition(Orientation.PORTRAIT))
    assertEquals(
      PortPosition.FAR,
      DotsOrientation.laidFlatPortPosition(Orientation.REVERSE_PORTRAIT),
    )
    assertNull(DotsOrientation.laidFlatPortPosition(Orientation.UNKNOWN))
  }

  @Test
  fun laidFlatPortPosition_matchesATabletFacingTheUser() {
    // Auto-rotate turns the screen to face the user, so the tablet rule gives the same sides.
    for ((held, degrees) in
      listOf(
        Orientation.PORTRAIT to 0,
        Orientation.LANDSCAPE to 90,
        Orientation.REVERSE_PORTRAIT to 180,
        Orientation.REVERSE_LANDSCAPE to 270,
      )) {
      assertEquals(
        DotsOrientation.tabletPortPosition(DotsOrientation.rotationForDegrees(degrees), true),
        DotsOrientation.laidFlatPortPosition(held),
      )
    }
  }

  @Test
  fun decidePhoneTabletop_typingInScreenAwayModeComesFirst() {
    assertEquals(
      PortPosition.LEFT,
      DotsOrientation.decidePhoneTabletopPort(false, Orientation.REVERSE_LANDSCAPE, false, rotation90),
    )
    assertEquals(
      PortPosition.RIGHT,
      DotsOrientation.decidePhoneTabletopPort(true, Orientation.LANDSCAPE, true, rotation0),
    )
  }

  @Test
  fun decidePhoneTabletop_thenHowThePhoneWasLastHeldUpFacingTheUser() {
    assertEquals(
      PortPosition.LEFT,
      DotsOrientation.decidePhoneTabletopPort(null, Orientation.LANDSCAPE, true, rotation0),
    )
    assertEquals(
      PortPosition.RIGHT,
      DotsOrientation.decidePhoneTabletopPort(null, Orientation.REVERSE_LANDSCAPE, false, rotation270),
    )
    assertEquals(
      PortPosition.NEAR,
      DotsOrientation.decidePhoneTabletopPort(null, Orientation.PORTRAIT, true, rotation0),
    )
    assertEquals(
      PortPosition.FAR,
      DotsOrientation.decidePhoneTabletopPort(null, Orientation.REVERSE_PORTRAIT, true, rotation0),
    )
  }

  @Test
  fun decidePhoneTabletop_heldInPortraitButTheScreenTurnedSince() {
    assertEquals(
      PortPosition.LEFT,
      DotsOrientation.decidePhoneTabletopPort(null, Orientation.PORTRAIT, false, rotation270),
    )
    assertEquals(
      PortPosition.RIGHT,
      DotsOrientation.decidePhoneTabletopPort(null, Orientation.PORTRAIT, false, rotation90),
    )
  }

  @Test
  fun decidePhoneTabletop_otherwiseTheScreenRotation() {
    // Never held up, with auto-rotate off: the port on the left, as the layout expects.
    assertEquals(
      PortPosition.LEFT,
      DotsOrientation.decidePhoneTabletopPort(null, Orientation.UNKNOWN, true, rotation0),
    )
    assertEquals(
      PortPosition.RIGHT,
      DotsOrientation.decidePhoneTabletopPort(null, Orientation.UNKNOWN, false, rotation90),
    )
    assertEquals(
      PortPosition.LEFT,
      DotsOrientation.decidePhoneTabletopPort(null, Orientation.UNKNOWN, false, rotation270),
    )
  }

  @Test
  fun phonePortOnRight_keepsTheLastSideWhileThePortIsTowardOrAwayFromTheUser() {
    assertTrue(DotsOrientation.phonePortOnRight(PortPosition.RIGHT, false))
    assertFalse(DotsOrientation.phonePortOnRight(PortPosition.LEFT, true))
    assertTrue(DotsOrientation.phonePortOnRight(PortPosition.NEAR, true))
    assertFalse(DotsOrientation.phonePortOnRight(PortPosition.FAR, false))
  }

  @Test
  fun shouldDecideTabletopAgain_whenNeverDecided() {
    assertTrue(DotsOrientation.shouldDecideTabletopAgain(-1, false, 0))
  }

  @Test
  fun shouldDecideTabletopAgain_afterTypingInScreenAwayMode() {
    assertTrue(DotsOrientation.shouldDecideTabletopAgain(1000, true, 0))
  }

  @Test
  fun shouldDecideTabletopAgain_afterBeingHeldUp() {
    assertTrue(DotsOrientation.shouldDecideTabletopAgain(1000, false, 1500))
  }

  @Test
  fun shouldDecideTabletopAgain_notAfterATiltWhileTurning() {
    assertFalse(DotsOrientation.shouldDecideTabletopAgain(1000, false, 500))
    assertFalse(DotsOrientation.shouldDecideTabletopAgain(1000, false, 1000))
  }

  @Test
  fun tabletPortPosition_lyingFlat() {
    assertEquals(PortPosition.NEAR, DotsOrientation.tabletPortPosition(rotation0, true))
    assertEquals(PortPosition.RIGHT, DotsOrientation.tabletPortPosition(rotation90, true))
    assertEquals(PortPosition.FAR, DotsOrientation.tabletPortPosition(rotation180, true))
    assertEquals(PortPosition.LEFT, DotsOrientation.tabletPortPosition(rotation270, true))
  }

  @Test
  fun tabletPortPosition_screenAway() {
    assertEquals(PortPosition.DOWN, DotsOrientation.tabletPortPosition(rotation0, false))
    assertEquals(PortPosition.LEFT, DotsOrientation.tabletPortPosition(rotation90, false))
    assertEquals(PortPosition.UP, DotsOrientation.tabletPortPosition(rotation180, false))
    assertEquals(PortPosition.RIGHT, DotsOrientation.tabletPortPosition(rotation270, false))
  }

  @Test
  fun heldPortPosition_aPhoneIsOnlyHeldInLandscape() {
    assertEquals(PortPosition.RIGHT, DotsOrientation.heldPortPosition(Orientation.LANDSCAPE, true))
    assertEquals(
      PortPosition.LEFT,
      DotsOrientation.heldPortPosition(Orientation.REVERSE_LANDSCAPE, true),
    )
    assertNull(DotsOrientation.heldPortPosition(Orientation.PORTRAIT, true))
    assertNull(DotsOrientation.heldPortPosition(Orientation.REVERSE_PORTRAIT, true))
    assertNull(DotsOrientation.heldPortPosition(Orientation.UNKNOWN, true))
  }

  @Test
  fun heldPortPosition_aTabletCanBeHeldAnyWayRound() {
    assertEquals(PortPosition.DOWN, DotsOrientation.heldPortPosition(Orientation.PORTRAIT, false))
    assertEquals(
      PortPosition.UP,
      DotsOrientation.heldPortPosition(Orientation.REVERSE_PORTRAIT, false),
    )
    assertNull(DotsOrientation.heldPortPosition(Orientation.UNKNOWN, false))
  }

  @Test
  fun heldPortPosition_agreesWithTheRotationTheTabletTurnsTo() {
    // How a tablet is held picks a rotation, which then says the same port position.
    for (held in
      listOf(
        Orientation.PORTRAIT,
        Orientation.LANDSCAPE,
        Orientation.REVERSE_PORTRAIT,
        Orientation.REVERSE_LANDSCAPE,
      )) {
      val rotation = DotsOrientation.rotationForDegrees(held.degree)
      assertEquals(
        held.name,
        DotsOrientation.heldPortPosition(held, false),
        DotsOrientation.tabletPortPosition(rotation, false),
      )
    }
  }

  @Test
  fun tabletTabletopRotation_heldFromBehindWithThePortAtTheSide_facesTheOtherEdge() {
    // Held screen-away with the port on the right, as on the Fold7 unfolded at rotation 270.
    assertEquals(
      rotation90,
      DotsOrientation.tabletTabletopRotation(rotation270, Orientation.LANDSCAPE, true, true),
    )
    assertEquals(
      rotation270,
      DotsOrientation.tabletTabletopRotation(rotation90, Orientation.REVERSE_LANDSCAPE, true, true),
    )
  }

  @Test
  fun tabletTabletopRotation_keepsThePortOnTheSameSideOfTheUser() {
    // Tipping a tablet flat keeps the port on the user's left or right.
    for (held in listOf(Orientation.LANDSCAPE, Orientation.REVERSE_LANDSCAPE)) {
      val heldRotation = DotsOrientation.rotationForDegrees(held.degree)
      assertEquals(
        held.name,
        DotsOrientation.heldPortPosition(held, false),
        DotsOrientation.tabletPortPosition(
          DotsOrientation.tabletTabletopRotation(heldRotation, held, true, true),
          true,
        ),
      )
    }
  }

  @Test
  fun tabletTabletopRotation_heldWithThePortDownOrUp_facesTheUserAsAutoRotateDoes() {
    assertEquals(
      rotation0,
      DotsOrientation.tabletTabletopRotation(rotation0, Orientation.PORTRAIT, true, true),
    )
    assertEquals(
      rotation180,
      DotsOrientation.tabletTabletopRotation(rotation180, Orientation.REVERSE_PORTRAIT, true, true),
    )
  }

  @Test
  fun tabletTabletopRotation_notFromScreenAway_facesTheUserAsAutoRotateDoes() {
    assertEquals(
      rotation270,
      DotsOrientation.tabletTabletopRotation(rotation270, Orientation.LANDSCAPE, false, true),
    )
  }

  @Test
  fun tabletTabletopRotation_heldUpFacesAwayOff_facesTheUserAsAutoRotateDoes() {
    // Stood on a stand facing the user, then laid flat.
    assertEquals(
      rotation270,
      DotsOrientation.tabletTabletopRotation(rotation270, Orientation.LANDSCAPE, true, false),
    )
    assertEquals(
      rotation90,
      DotsOrientation.tabletTabletopRotation(
        rotation90,
        Orientation.REVERSE_LANDSCAPE,
        true,
        false,
      ),
    )
  }

  @Test
  fun tabletTabletopRotation_heldUpFacesAwayOff_portraitFacesTheUserAsAutoRotateDoes() {
    assertEquals(
      rotation0,
      DotsOrientation.tabletTabletopRotation(rotation0, Orientation.PORTRAIT, true, false),
    )
    assertEquals(
      rotation180,
      DotsOrientation.tabletTabletopRotation(rotation180, Orientation.REVERSE_PORTRAIT, true, false),
    )
  }

  @Test
  fun heldFacingUser_aTabletHeldAnyWayRoundWithTheSwitchOff() {
    for (held in
      listOf(
        Orientation.PORTRAIT,
        Orientation.LANDSCAPE,
        Orientation.REVERSE_PORTRAIT,
        Orientation.REVERSE_LANDSCAPE,
      )) {
      assertTrue(held.name, DotsOrientation.heldFacingUser(held, false, false))
      assertFalse(held.name, DotsOrientation.heldFacingUser(held, false, true))
      assertFalse(held.name, DotsOrientation.heldFacingUser(held, true, false))
    }
    assertFalse(DotsOrientation.heldFacingUser(Orientation.UNKNOWN, false, false))
  }

  @Test
  fun uprightPortPosition_theEdgeTowardTheUserPointsDown() {
    assertEquals(PortPosition.DOWN, DotsOrientation.uprightPortPosition(PortPosition.NEAR))
    assertEquals(PortPosition.UP, DotsOrientation.uprightPortPosition(PortPosition.FAR))
    assertEquals(PortPosition.LEFT, DotsOrientation.uprightPortPosition(PortPosition.LEFT))
    assertEquals(PortPosition.RIGHT, DotsOrientation.uprightPortPosition(PortPosition.RIGHT))
  }

  @Test
  fun facingUser_portraitSaysThePortPointingDownOrUp() {
    assertEquals(
      PortPosition.DOWN,
      DotsOrientation.uprightPortPosition(DotsOrientation.tabletPortPosition(rotation0, true)),
    )
    assertEquals(
      PortPosition.UP,
      DotsOrientation.uprightPortPosition(DotsOrientation.tabletPortPosition(rotation180, true)),
    )
  }

  @Test
  fun heldFacingUser_portIsOnTheOtherSideFromWhenHeldFromBehind() {
    // Standing up facing the user at the held rotation, the user sees the port on the other side.
    assertEquals(
      PortPosition.LEFT,
      DotsOrientation.tabletPortPosition(
        DotsOrientation.rotationForDegrees(Orientation.LANDSCAPE.degree),
        true,
      ),
    )
    assertEquals(PortPosition.RIGHT, DotsOrientation.heldPortPosition(Orientation.LANDSCAPE, false))
  }

  @Test
  fun phoneLock_roundTrips() {
    assertTrue(DotsOrientation.phoneLockPortOnRight(DotsOrientation.phoneLock(true)))
    assertFalse(DotsOrientation.phoneLockPortOnRight(DotsOrientation.phoneLock(false)))
    assertTrue(DotsOrientation.phoneLock(true) != DotsOrientation.UNLOCKED)
    assertTrue(DotsOrientation.phoneLock(false) != DotsOrientation.UNLOCKED)
  }

  @Test
  fun tabletLocks_areNeverUnlocked() {
    for (rotation in 0..3) {
      assertTrue(rotation != DotsOrientation.UNLOCKED)
    }
  }

  private fun map(turns: Int, width: Float, height: Float, x: Float, y: Float): FloatArray {
    val m = DotsOrientation.layoutToScreen(turns, width, height)
    return floatArrayOf(m[0] * x + m[1] * y + m[2], m[3] * x + m[4] * y + m[5])
  }

  private fun assertPoint(x: Float, y: Float, point: FloatArray) {
    assertEquals(x, point[0], 0.001f)
    assertEquals(y, point[1], 0.001f)
  }
}
