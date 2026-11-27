/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.gesture

import android.view.Surface

/** Decides whether a two-finger tap had one finger at the top of the screen and one at the bottom. */
object TopAndBottomTap {
  /**
   * Where a touch at [x], [y] on the screen as displayed lies from the top of the device held in
   * its natural orientation, with the display turned by [rotation], a [Surface] rotation. The
   * thirds are the same from either end, so only the axis matters, not the direction of the turn.
   */
  @JvmStatic
  fun alongDevice(x: Float, y: Float, rotation: Int): Float = if (isTurnedSideways(rotation)) x else y

  /** The length of the device from top to bottom, on a screen [width] by [height] as displayed. */
  @JvmStatic
  fun deviceLength(width: Int, height: Int, rotation: Int): Int =
    if (isTurnedSideways(rotation)) width else height

  private fun isTurnedSideways(rotation: Int) =
    rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270

  /**
   * Whether one of [firstY] and [secondY], measured from the top of a length [height] pixels long,
   * such as from [alongDevice] and [deviceLength], is in the top third and the other in the bottom
   * third.
   */
  @JvmStatic
  fun isTopAndBottom(firstY: Float, secondY: Float, height: Int): Boolean {
    if (height <= 0) return false
    val top = height / 3f
    val bottom = height * 2 / 3f
    return (firstY < top && secondY >= bottom) || (secondY < top && firstY >= bottom)
  }
}
