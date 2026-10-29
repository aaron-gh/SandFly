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

package com.google.android.accessibility.talkback.gesture

import android.media.session.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Checks the positions that rewind and fast forward move playing media to. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaSeekTest {

  @Test
  fun playingMediaHasMovedOnSinceItsLastUpdate() {
    val state = state(PlaybackState.STATE_PLAYING, position = 5_000, updated = 1_000, speed = 1f)
    assertEquals(8_000, MediaSeek.currentPosition(state, now = 4_000))
  }

  @Test
  fun fasterPlaybackMovesFurther() {
    val state = state(PlaybackState.STATE_PLAYING, position = 0, updated = 1_000, speed = 2f)
    assertEquals(6_000, MediaSeek.currentPosition(state, now = 4_000))
  }

  @Test
  fun pausedMediaStaysWhereItIs() {
    val state = state(PlaybackState.STATE_PAUSED, position = 5_000, updated = 1_000, speed = 0f)
    assertEquals(5_000, MediaSeek.currentPosition(state, now = 4_000))
  }

  @Test
  fun stepsStayWithinTheMedia() {
    assertEquals(15_000, MediaSeek.newPosition(5_000, MediaSeek.STEP_MS, duration = 60_000))
    assertEquals(0, MediaSeek.newPosition(4_000, -MediaSeek.STEP_MS, duration = 60_000))
    assertEquals(60_000, MediaSeek.newPosition(55_000, MediaSeek.STEP_MS, duration = 60_000))
    assertEquals(65_000, MediaSeek.newPosition(55_000, MediaSeek.STEP_MS, duration = null))
  }

  private fun state(state: Int, position: Long, updated: Long, speed: Float): PlaybackState =
    PlaybackState.Builder()
      .setState(state, position, speed, updated)
      .setActions(PlaybackState.ACTION_SEEK_TO)
      .build()
}
