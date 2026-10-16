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

import android.view.Surface
import com.google.android.accessibility.brailleime.OrientationMonitor.Orientation

/** Where the charging port is, from where the user is. */
enum class PortPosition {
  LEFT,
  RIGHT,
  /** Toward the user, with the device lying flat. */
  NEAR,
  /** Away from the user, with the device lying flat. */
  FAR,
  /** Pointing down, with a tablet held up. */
  DOWN,
  /** Pointing up, with a tablet held up. */
  UP,
}

/**
 * Decides which way the braille dots face, apart from the views and sensors so it can be tested.
 * Rotations are [Surface] rotations, from 0 to 3 quarter turns.
 *
 * A phone only turns the dots by half turns, as its charging port can only be on the user's left
 * or right, but it follows quarter turns on the table to tell which side the port ends up on. A
 * tablet turns them by quarter turns, as if auto-rotate had turned the screen to face the user.
 *
 * Gravity cannot tell whether a device held up faces the user or faces away. Laid flat after being
 * held up, a device takes it that it was held facing the user, as auto-rotate does, unless the user
 * typed on a phone in screen-away mode.
 */
object DotsOrientation {
  /** The orientation lock when unlocked. */
  const val UNLOCKED = -1

  /** A phone's orientation lock with the charging port on the user's left. */
  const val LOCKED_PORT_ON_LEFT = 0

  /** A phone's orientation lock with the charging port on the user's right. */
  const val LOCKED_PORT_ON_RIGHT = 1

  /**
   * The screen rotation auto-rotate gives for a reading from
   * [android.view.OrientationEventListener], which is how far the device is turned clockwise from
   * its natural orientation. The screen turns the other way to stay upright.
   */
  @JvmStatic
  fun rotationForDegrees(degrees: Int): Int = (4 - Math.round(degrees / 90f) % 4) % 4

  /**
   * How many quarter turns clockwise the dots are drawn from the screen as displayed, so they face
   * the user as if the screen were at [wantedRotation].
   */
  @JvmStatic
  fun quarterTurns(wantedRotation: Int, displayedRotation: Int): Int =
    Math.floorMod(wantedRotation - displayedRotation, 4)

  /**
   * The rotation that faces the user after the device turns this many quarter turns clockwise, seen
   * from above, while lying flat. The screen turns the other way to keep facing the user.
   */
  @JvmStatic
  fun turnRotation(rotation: Int, quarters: Int): Int = Math.floorMod(rotation - quarters, 4)

  /** Where the charging port is, from the user, as it goes round a device turned clockwise. */
  private val CLOCKWISE =
    listOf(PortPosition.NEAR, PortPosition.LEFT, PortPosition.FAR, PortPosition.RIGHT)

  /**
   * Where a phone's charging port is after the phone, lying flat, turns this many quarter turns
   * clockwise, seen from above.
   */
  @JvmStatic
  fun turnPortPosition(position: PortPosition, quarters: Int): PortPosition {
    val index = CLOCKWISE.indexOf(position)
    return if (index < 0) position else CLOCKWISE[Math.floorMod(index + quarters, 4)]
  }

  /**
   * Maps the layout of the dots onto a screen of this size, turned this many quarter turns
   * clockwise, as the nine values of an [android.graphics.Matrix]. A sideways layout has the
   * screen's width and height swapped.
   */
  @JvmStatic
  fun layoutToScreen(quarterTurns: Int, width: Float, height: Float): FloatArray =
    when (Math.floorMod(quarterTurns, 4)) {
      1 -> floatArrayOf(0f, -1f, width, 1f, 0f, 0f, 0f, 0f, 1f)
      2 -> floatArrayOf(-1f, 0f, width, 0f, -1f, height, 0f, 0f, 1f)
      3 -> floatArrayOf(0f, 1f, 0f, -1f, 0f, height, 0f, 0f, 1f)
      else -> floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
    }

  /**
   * Which side a phone's layout expects the charging port on in screen-away mode. In portrait, as
   * with auto-rotate off, it expects the port on the right. In landscape, the screen has turned with
   * the phone. It depends only on how the screen is turned, never on how the dots are numbered: the
   * user's settings to swap the dots apply on top of any turn, as [DotNumberOrder] numbers the dots
   * by where they are.
   */
  @JvmStatic
  fun screenAwayLayoutExpectsPortOnRight(portrait: Boolean, rotation: Int): Boolean =
    portrait || rotation == Surface.ROTATION_270

  /**
   * Which side a phone's layout expects the charging port on in tabletop mode. In portrait, as with
   * auto-rotate off, it expects the port on the left.
   */
  @JvmStatic
  fun tabletopLayoutExpectsPortOnRight(portrait: Boolean, rotation: Int): Boolean =
    !portrait && rotation == Surface.ROTATION_90

  /**
   * Which side the charging port is on when a phone is held like this in screen-away mode, or null
   * when it is not held in landscape.
   */
  @JvmStatic
  fun heldPortOnRight(held: Orientation): Boolean? =
    when (held) {
      Orientation.LANDSCAPE -> true
      Orientation.REVERSE_LANDSCAPE -> false
      else -> null
    }

  /**
   * Where a phone's charging port is once it is laid flat after being held up like this, taking it
   * that the screen faced the user, as auto-rotate does, or null if it has not been held up.
   */
  @JvmStatic
  fun laidFlatPortPosition(held: Orientation): PortPosition? =
    when (held) {
      Orientation.LANDSCAPE -> PortPosition.LEFT
      Orientation.REVERSE_LANDSCAPE -> PortPosition.RIGHT
      Orientation.PORTRAIT -> PortPosition.NEAR
      Orientation.REVERSE_PORTRAIT -> PortPosition.FAR
      else -> null
    }

  /**
   * Decides where a phone's charging port is when it is laid flat. A phone lying flat gives no sign
   * of which way round it is, but tipping it flat keeps the port on the same side of the user. So
   * the side is the one the user last typed with in screen-away mode. Otherwise it is where the
   * port was when the phone was last held up, taking it that the screen faced the user, as when
   * picking the phone up to read it. Held in portrait, the port is toward or away from the user
   * until the phone is turned on the table, unless the screen has turned to landscape since.
   * Otherwise the screen rotation gives it, as auto-rotate turns the screen to face the user.
   */
  @JvmStatic
  fun decidePhoneTabletopPort(
    typedScreenAwayPortOnRight: Boolean?,
    lastHeld: Orientation,
    portrait: Boolean,
    rotation: Int,
  ): PortPosition {
    if (typedScreenAwayPortOnRight != null) {
      return phonePortPosition(typedScreenAwayPortOnRight)
    }
    val held = laidFlatPortPosition(lastHeld)
    if (held == PortPosition.LEFT || held == PortPosition.RIGHT || (held != null && portrait)) {
      return held
    }
    return phonePortPosition(tabletopLayoutExpectsPortOnRight(portrait, rotation))
  }

  /**
   * Which side a phone's layout puts the charging port on: the side the port is on, or else, with
   * the port toward or away from the user, [previous], until a turn puts it on a side.
   */
  @JvmStatic
  fun phonePortOnRight(position: PortPosition, previous: Boolean): Boolean =
    when (position) {
      PortPosition.RIGHT -> true
      PortPosition.LEFT -> false
      else -> previous
    }

  /**
   * Whether to decide the tabletop side again, which is only when something new shows it: typing
   * in screen-away mode, or holding the device up, since it was last decided. So a device that
   * tilts for a moment while being turned on the table keeps its side.
   */
  @JvmStatic
  fun shouldDecideTabletopAgain(
    decidedAtMs: Long,
    typedInScreenAway: Boolean,
    lastHeldSeenMs: Long,
  ): Boolean = decidedAtMs < 0 || typedInScreenAway || lastHeldSeenMs > decidedAtMs

  /**
   * Where the charging port is on a tablet facing the user at this rotation, for one whose port is
   * at the bottom of the screen in its natural orientation. Lying flat, that edge is toward the
   * user. Held up in screen-away mode, the user sees the back, so left and right swap.
   */
  @JvmStatic
  fun tabletPortPosition(rotation: Int, tabletop: Boolean): PortPosition =
    when (Math.floorMod(rotation, 4)) {
      Surface.ROTATION_90 -> if (tabletop) PortPosition.RIGHT else PortPosition.LEFT
      Surface.ROTATION_180 -> if (tabletop) PortPosition.FAR else PortPosition.UP
      Surface.ROTATION_270 -> if (tabletop) PortPosition.LEFT else PortPosition.RIGHT
      else -> if (tabletop) PortPosition.NEAR else PortPosition.DOWN
    }

  /**
   * Where the charging port is, from behind the screen, when the device is held like this in
   * screen-away mode, or null when that cannot be said. A phone is only held in landscape, while a
   * tablet can be held any way round. Only call it for a tablet whose port is at the bottom of the
   * screen in its natural orientation.
   */
  @JvmStatic
  fun heldPortPosition(held: Orientation, phone: Boolean): PortPosition? =
    when (held) {
      Orientation.LANDSCAPE -> PortPosition.RIGHT
      Orientation.REVERSE_LANDSCAPE -> PortPosition.LEFT
      Orientation.PORTRAIT -> if (phone) null else PortPosition.DOWN
      Orientation.REVERSE_PORTRAIT -> if (phone) null else PortPosition.UP
      else -> null
    }

  /**
   * The screen rotation that faces the user once a tablet is laid flat, from [heldRotation], the
   * rotation auto-rotate gave for how it was last held up. Someone facing the screen ends up at the
   * edge that was at the bottom, as auto-rotate assumes. But a tablet held up in screen-away mode
   * with the charging port to the left or right was held from behind, and tipping it flat keeps the
   * port on the same side of the user, so the user is at the opposite edge. Held with the port down
   * or up, the user most likely opened the keyboard facing the screen in portrait. When
   * [heldUpFacesAway] is off, a tablet held up is always taken to face the user, as when it stood
   * on a stand. Only call it for a tablet whose port is at the bottom of the screen in its natural
   * orientation.
   */
  @JvmStatic
  fun tabletTabletopRotation(
    heldRotation: Int,
    held: Orientation,
    fromScreenAway: Boolean,
    heldUpFacesAway: Boolean,
  ): Int {
    val port = heldPortPosition(held, phone = false)
    val heldFromBehind =
      heldUpFacesAway &&
        fromScreenAway &&
        (port == PortPosition.LEFT || port == PortPosition.RIGHT)
    return if (heldFromBehind) turnRotation(heldRotation, 2) else heldRotation
  }

  /**
   * Whether a device held up like this is taken to face the user rather than face away: a tablet
   * held up any way round when [heldUpFacesAway] is off. Then it uses the tabletop layout, as the
   * user types on the front of the screen with the thumbs holding the edge nearest the floor.
   */
  @JvmStatic
  fun heldFacingUser(held: Orientation, phone: Boolean, heldUpFacesAway: Boolean): Boolean =
    !phone && !heldUpFacesAway && held != Orientation.UNKNOWN

  /**
   * Where the charging port is on a tablet standing up facing the user, from where it would be with
   * the tablet lying flat at the same rotation: the edge toward the user is the one pointing down.
   */
  @JvmStatic
  fun uprightPortPosition(tabletop: PortPosition): PortPosition =
    when (tabletop) {
      PortPosition.NEAR -> PortPosition.DOWN
      PortPosition.FAR -> PortPosition.UP
      else -> tabletop
    }

  /** A phone's orientation lock for the charging port on this side. */
  @JvmStatic
  fun phoneLock(portOnRight: Boolean): Int =
    if (portOnRight) LOCKED_PORT_ON_RIGHT else LOCKED_PORT_ON_LEFT

  /** The side of the charging port in a phone's orientation lock. */
  @JvmStatic fun phoneLockPortOnRight(lock: Int): Boolean = lock == LOCKED_PORT_ON_RIGHT

  /** Where the charging port is on a phone, which is only ever on the user's left or right. */
  @JvmStatic
  fun phonePortPosition(portOnRight: Boolean): PortPosition =
    if (portOnRight) PortPosition.RIGHT else PortPosition.LEFT
}
