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

package com.google.android.accessibility.talkback.dictation

import com.google.android.accessibility.talkback.actor.gemini.local.LiteRtLmLocalLlm
import com.google.android.accessibility.talkback.directtouch.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRetentionTest {
  private val prefs = FakeSharedPreferences()

  private fun set(value: String) {
    prefs.edit().putString(ModelRetention.KEY, value).apply()
  }

  @Test
  fun unsetUnloadsWhisperAtOnceAndKeepsTheOnDeviceModelItsUsualTime() {
    assertFalse(ModelRetention.untilStopped(prefs))
    assertEquals(0L, ModelRetention.keepLoadedMs(prefs))
    assertEquals(LiteRtLmLocalLlm.DEFAULT_IDLE_UNLOAD_MS, ModelRetention.onDeviceIdleMs(prefs))
  }

  @Test
  fun minutesKeepBothModelsThatLong() {
    set("30")
    assertFalse(ModelRetention.untilStopped(prefs))
    assertEquals(30 * 60_000L, ModelRetention.keepLoadedMs(prefs))
    assertEquals(30 * 60_000L, ModelRetention.onDeviceIdleMs(prefs))
  }

  @Test
  fun untilStoppedNeverUnloadsEitherModel() {
    set("until_stopped")
    assertTrue(ModelRetention.untilStopped(prefs))
    assertEquals(ModelRetention.UNTIL_STOPPED, ModelRetention.keepLoadedMs(prefs))
    assertEquals(ModelRetention.UNTIL_STOPPED, ModelRetention.onDeviceIdleMs(prefs))
  }

  @Test
  fun unknownAndOutOfRangeValuesCannotOverflow() {
    set("junk")
    assertEquals(0L, ModelRetention.keepLoadedMs(prefs))
    set("-5")
    assertEquals(0L, ModelRetention.keepLoadedMs(prefs))
    set(Long.MAX_VALUE.toString())
    assertTrue(ModelRetention.keepLoadedMs(prefs) in 1 until ModelRetention.UNTIL_STOPPED)
  }
}
