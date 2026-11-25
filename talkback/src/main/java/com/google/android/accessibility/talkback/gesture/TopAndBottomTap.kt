/* Copyright 2026 SandFly contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.gesture

/** Decides whether a two-finger tap had one finger at the top of the screen and one at the bottom. */
object TopAndBottomTap {
  /**
   * Whether one of [firstY] and [secondY] is in the top third of a screen [height] pixels high, as
   * displayed, and the other in the bottom third.
   */
  @JvmStatic
  fun isTopAndBottom(firstY: Float, secondY: Float, height: Int): Boolean {
    if (height <= 0) return false
    val top = height / 3f
    val bottom = height * 2 / 3f
    return (firstY < top && secondY >= bottom) || (secondY < top && firstY >= bottom)
  }
}
