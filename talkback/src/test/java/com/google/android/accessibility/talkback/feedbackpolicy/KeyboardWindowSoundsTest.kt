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

package com.google.android.accessibility.talkback.feedbackpolicy

import android.content.Context
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.input.WindowEventInterpreter.EventInterpretation
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class KeyboardWindowSoundsTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private val played = mutableListOf<Int>()
  private var active = true
  private val sounds = KeyboardWindowSounds(context, { played += it }, { active })

  @Before
  fun baseline() {
    window(open = false)
    settle()
  }

  @Test
  fun playsOpenedThenClosed() {
    window(open = true)
    settle()
    window(open = false)
    settle()
    assertEquals(listOf(R.raw.keyboard_opened, R.raw.keyboard_closed), played)
  }

  @Test
  fun aWindowOpenAtStartPlaysNothing() {
    val started = KeyboardWindowSounds(context, { played += it }, { active })
    started.handle(interpretation(open = true), null)
    settle()
    assertEquals(emptyList<Int>(), played)
  }

  @Test
  fun aWindowThatReappearsAtOncePlaysNothing() {
    window(open = true)
    settle()
    played.clear()
    window(open = false)
    window(open = true)
    settle()
    assertEquals(emptyList<Int>(), played)
  }

  @Test
  fun aRepeatedStatePlaysOnce() {
    window(open = true)
    settle()
    window(open = true)
    settle()
    assertEquals(listOf(R.raw.keyboard_opened), played)
  }

  @Test
  fun playsNothingWhenInactive() {
    active = false
    window(open = true)
    settle()
    assertEquals(emptyList<Int>(), played)
  }

  @Test
  fun playsNothingWithTheScreenOff() {
    shadowOf(context.getSystemService(PowerManager::class.java)).setIsInteractive(false)
    window(open = true)
    settle()
    assertEquals(emptyList<Int>(), played)
  }

  @Test
  fun playsNothingForTheBrailleKeyboard() {
    Settings.Secure.putString(
      context.contentResolver,
      Settings.Secure.DEFAULT_INPUT_METHOD,
      "${context.packageName}/com.google.android.accessibility.brailleime.BrailleIme",
    )
    window(open = true)
    settle()
    assertEquals(emptyList<Int>(), played)
  }

  @Test
  fun playsForAnotherKeyboard() {
    Settings.Secure.putString(
      context.contentResolver,
      Settings.Secure.DEFAULT_INPUT_METHOD,
      "com.google.android.inputmethod.latin/.LatinIME",
    )
    window(open = true)
    settle()
    assertEquals(listOf(R.raw.keyboard_opened), played)
  }

  private fun window(open: Boolean) {
    sounds.handle(interpretation(open), null)
  }

  private fun interpretation(open: Boolean) =
    EventInterpretation().apply { inputMethod.id = if (open) 7 else -1 }

  private fun settle() {
    shadowOf(Looper.getMainLooper()).idleFor(200, TimeUnit.MILLISECONDS)
  }
}
