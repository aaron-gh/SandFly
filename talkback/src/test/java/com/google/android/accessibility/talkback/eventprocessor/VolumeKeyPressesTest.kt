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

package com.google.android.accessibility.talkback.eventprocessor

import android.view.KeyEvent.KEYCODE_VOLUME_DOWN
import android.view.KeyEvent.KEYCODE_VOLUME_UP
import com.google.android.accessibility.talkback.eventprocessor.VolumeKeyPresses.KeyCommands
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VolumeKeyPressesTest {
  private val timeouts = VolumeKeyPresses.Timeouts(multiPress = 300, longPress = 400, repeat = 50)

  /** What happened, in order: "run X", "up", "down", "up repeated". */
  private val log = mutableListOf<String>()
  private var now = 0L
  private var taskAt = -1L
  private var task: Runnable? = null
  private var canUse = true

  private val timer =
    object : VolumeKeyPresses.Timer {
      override fun schedule(delayMs: Long, task: Runnable) {
        taskAt = now + delayMs
        this@VolumeKeyPressesTest.task = task
      }

      override fun cancel() {
        task = null
      }
    }

  private lateinit var presses: VolumeKeyPresses

  @Before
  fun setUp() {
    presses =
      VolumeKeyPresses(
        timer,
        object : VolumeKeyPresses.Callbacks {
          override fun perform(command: String) {
            log += "run $command"
          }

          override fun adjustVolume(keyCode: Int, repeated: Boolean) {
            val name = if (keyCode == KEYCODE_VOLUME_UP) "up" else "down"
            log += if (repeated) "$name repeated" else name
          }
        },
      )
  }

  private fun assign(up: KeyCommands = KeyCommands.NONE, down: KeyCommands = KeyCommands.NONE) {
    presses.setCommands(up, down, timeouts)
  }

  /** Moves the clock on by [ms], running the timer's task when it comes due. */
  private fun advance(ms: Long) {
    val end = now + ms
    while (true) {
      val due = task
      if (due == null || taskAt > end) {
        break
      }
      now = taskAt
      task = null
      due.run()
    }
    now = end
  }

  private fun down(key: Int, repeated: Boolean = false): Boolean =
    presses.onKeyDown(key, repeated) { canUse }

  private fun up(key: Int): Boolean = presses.onKeyUp(key)

  /** Presses and releases [key] after [heldMs], and returns whether both were kept. */
  private fun press(key: Int, heldMs: Long = 80): Boolean {
    val keptDown = down(key)
    advance(heldMs)
    val keptUp = up(key)
    assertEquals(keptDown, keptUp)
    return keptDown
  }

  @Test
  fun nothingAssigned_keysPassThroughWithoutTimers() {
    assertFalse(press(KEYCODE_VOLUME_UP))
    assertFalse(press(KEYCODE_VOLUME_DOWN, heldMs = 2000))
    assertFalse(presses.isUsed)
    assertEquals(null, task)
    assertTrue(log.isEmpty())
  }

  @Test
  fun otherKeyAssigned_keyPassesThrough() {
    assign(down = KeyCommands("A", null, null))
    assertFalse(press(KEYCODE_VOLUME_UP))
    assertEquals(null, task)
  }

  @Test
  fun notUsable_keyPassesThrough() {
    assign(up = KeyCommands("A", null, null))
    canUse = false
    assertFalse(press(KEYCODE_VOLUME_UP))
    assertEquals(null, task)
  }

  @Test
  fun doubleAssigned_singlePressChangesVolumeAfterTimeout() {
    assign(up = KeyCommands("A", null, null))
    assertTrue(press(KEYCODE_VOLUME_UP))
    advance(299)
    assertTrue(log.isEmpty())
    advance(1)
    assertEquals(listOf("up"), log)
    assertEquals(null, task)
  }

  @Test
  fun doublePress_runsCommandAtSecondRelease() {
    assign(up = KeyCommands("A", null, null))
    assertTrue(press(KEYCODE_VOLUME_UP))
    advance(150)
    assertTrue(press(KEYCODE_VOLUME_UP))
    assertEquals(listOf("run A"), log)
    assertEquals(null, task)
  }

  @Test
  fun pressesTooFarApart_eachChangesVolume() {
    assign(up = KeyCommands("A", null, null))
    press(KEYCODE_VOLUME_UP)
    advance(400)
    press(KEYCODE_VOLUME_UP)
    advance(400)
    assertEquals(listOf("up", "up"), log)
  }

  @Test
  fun tripleAssigned_doublePressWaitsForThird() {
    assign(down = KeyCommands("A", "B", null))
    press(KEYCODE_VOLUME_DOWN)
    advance(100)
    press(KEYCODE_VOLUME_DOWN)
    assertTrue(log.isEmpty())
    advance(300)
    assertEquals(listOf("run A"), log)
  }

  @Test
  fun triplePress_runsCommandAtThirdRelease() {
    assign(down = KeyCommands("A", "B", null))
    press(KEYCODE_VOLUME_DOWN)
    advance(100)
    press(KEYCODE_VOLUME_DOWN)
    advance(100)
    press(KEYCODE_VOLUME_DOWN)
    assertEquals(listOf("run B"), log)
    assertEquals(null, task)
  }

  @Test
  fun onlyTripleAssigned_doublePressChangesVolumeTwice() {
    assign(down = KeyCommands(null, "B", null))
    press(KEYCODE_VOLUME_DOWN)
    advance(100)
    press(KEYCODE_VOLUME_DOWN)
    advance(300)
    assertEquals(listOf("down", "down"), log)
  }

  @Test
  fun onlyLongAssigned_shortPressChangesVolumeAtRelease() {
    assign(up = KeyCommands(null, null, "L"))
    assertTrue(press(KEYCODE_VOLUME_UP))
    assertEquals(listOf("up"), log)
    assertEquals(null, task)
  }

  @Test
  fun longPress_runsCommandOnceAndKeepsRelease() {
    assign(up = KeyCommands("A", null, "L"))
    assertTrue(down(KEYCODE_VOLUME_UP))
    advance(400)
    assertEquals(listOf("run L"), log)
    // Repeats while held change nothing.
    assertTrue(down(KEYCODE_VOLUME_UP, repeated = true))
    advance(2000)
    assertTrue(up(KEYCODE_VOLUME_UP))
    assertEquals(listOf("run L"), log)
    assertEquals(null, task)
  }

  @Test
  fun heldWithoutLongCommand_changesVolumeRepeatedlyUntilRelease() {
    assign(up = KeyCommands("A", null, null))
    down(KEYCODE_VOLUME_UP)
    advance(400)
    assertEquals(listOf("up"), log)
    advance(100)
    assertEquals(listOf("up", "up repeated", "up repeated"), log)
    assertTrue(up(KEYCODE_VOLUME_UP))
    advance(1000)
    assertEquals(3, log.size)
    assertEquals(null, task)
  }

  @Test
  fun heldAfterAPress_changesVolumeForBothThenRepeats() {
    assign(up = KeyCommands("A", null, "L"))
    press(KEYCODE_VOLUME_UP)
    advance(100)
    down(KEYCODE_VOLUME_UP)
    advance(400)
    // Long press is only the first press of the key.
    assertEquals(listOf("up", "up"), log)
    advance(50)
    assertEquals(listOf("up", "up", "up repeated"), log)
    up(KEYCODE_VOLUME_UP)
  }

  @Test
  fun repeatsStopAtLimitWithoutRelease() {
    assign(up = KeyCommands("A", null, null))
    down(KEYCODE_VOLUME_UP)
    advance(400 + 50L * (VolumeKeyPresses.MAX_REPEATS + 20))
    assertEquals(1 + VolumeKeyPresses.MAX_REPEATS, log.size)
    assertEquals(null, task)
    assertTrue(up(KEYCODE_VOLUME_UP))
  }

  @Test
  fun bothKeysHeld_nothingRunsAndOtherKeyPasses() {
    assign(up = KeyCommands("A", null, "L"), down = KeyCommands("B", null, null))
    assertTrue(down(KEYCODE_VOLUME_UP))
    advance(100)
    assertFalse(down(KEYCODE_VOLUME_DOWN))
    advance(3000)
    assertFalse(up(KEYCODE_VOLUME_DOWN))
    assertTrue(up(KEYCODE_VOLUME_UP))
    advance(1000)
    assertTrue(log.isEmpty())
    assertEquals(null, task)
  }

  @Test
  fun otherKeyDuringWait_finishesFirstKeyAndStartsOther() {
    assign(up = KeyCommands("A", null, null), down = KeyCommands("B", null, null))
    press(KEYCODE_VOLUME_UP)
    advance(100)
    assertTrue(down(KEYCODE_VOLUME_DOWN))
    assertEquals(listOf("up"), log)
    advance(50)
    up(KEYCODE_VOLUME_DOWN)
    advance(100)
    press(KEYCODE_VOLUME_DOWN)
    assertEquals(listOf("up", "run B"), log)
  }

  @Test
  fun otherUnassignedKeyDuringWait_finishesFirstKeyAndPasses() {
    assign(up = KeyCommands("A", null, null))
    press(KEYCODE_VOLUME_UP)
    advance(100)
    assertFalse(press(KEYCODE_VOLUME_DOWN))
    assertEquals(listOf("up"), log)
    assertEquals(null, task)
  }

  @Test
  fun notUsableAtSecondPress_finishesAndPasses() {
    assign(up = KeyCommands("A", null, null))
    press(KEYCODE_VOLUME_UP)
    advance(100)
    canUse = false
    assertFalse(press(KEYCODE_VOLUME_UP))
    assertEquals(listOf("up"), log)
    assertEquals(null, task)
  }

  @Test
  fun cancel_forgetsPresses() {
    assign(up = KeyCommands("A", null, null))
    press(KEYCODE_VOLUME_UP)
    presses.cancel()
    advance(1000)
    assertTrue(log.isEmpty())
    assertEquals(null, task)
  }

  @Test
  fun unassigningAll_dropsPressesInProgress() {
    assign(up = KeyCommands("A", null, null))
    down(KEYCODE_VOLUME_UP)
    assign()
    assertFalse(up(KEYCODE_VOLUME_UP))
    advance(1000)
    assertTrue(log.isEmpty())
    assertEquals(null, task)
  }

  @Test
  fun repeatOfPassedPress_passes() {
    assign(up = KeyCommands("A", null, null))
    canUse = false
    assertFalse(down(KEYCODE_VOLUME_UP))
    canUse = true
    assertFalse(down(KEYCODE_VOLUME_UP, repeated = true))
    assertFalse(up(KEYCODE_VOLUME_UP))
    assertEquals(null, task)
  }

  @Test
  fun missedRelease_nextPressStartsAgain() {
    assign(up = KeyCommands("A", null, "L"))
    down(KEYCODE_VOLUME_UP)
    advance(400)
    assertEquals(listOf("run L"), log)
    // The release went to another feature; the next press is a new one.
    assertTrue(down(KEYCODE_VOLUME_UP))
    advance(80)
    assertTrue(up(KEYCODE_VOLUME_UP))
    advance(300)
    assertEquals(listOf("run L", "up"), log)
  }
}
