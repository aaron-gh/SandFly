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

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenOnRecorderTest {
  @Test
  fun startsAtZero() {
    assertEquals(0L, ScreenOnRecorder().screenOnUptime)
  }

  @Test
  fun broadcastAfterDisplayOn_keepsDisplayOnTime() {
    val recorder = ScreenOnRecorder()
    recorder.onScreenOnSignal(1_000)
    recorder.onScreenOnSignal(1_140)
    assertEquals(1_000L, recorder.screenOnUptime)
  }

  @Test
  fun broadcastAlone_isRecorded() {
    val recorder = ScreenOnRecorder()
    recorder.onScreenOnSignal(1_140)
    assertEquals(1_140L, recorder.screenOnUptime)
  }

  @Test
  fun nextWake_isRecordedAfterScreenOff() {
    val recorder = ScreenOnRecorder()
    recorder.onScreenOnSignal(1_000)
    recorder.onScreenOff()
    recorder.onScreenOnSignal(60_000)
    assertEquals(60_000L, recorder.screenOnUptime)
  }

  @Test
  fun displayOnWithoutScreenOff_keepsFirstTime() {
    val recorder = ScreenOnRecorder()
    recorder.onScreenOnSignal(1_000)
    recorder.onScreenOnSignal(60_000)
    assertEquals(1_000L, recorder.screenOnUptime)
  }
}
