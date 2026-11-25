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

package com.google.android.accessibility.talkback.actor

import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** GitHub #210: the step of speech rate changes can be chosen. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpeechRateStepTest {
  private val context = RuntimeEnvironment.getApplication()
  private val prefs = SharedPreferencesUtils.getSharedPreferences(context)

  @Before
  fun setUp() {
    prefs.edit().clear().commit()
  }

  private fun percent(step: Int, rate: Int, isIncrease: Boolean): Int =
    Math.round(SpeechRateAndPitchActor.steppedRate(step, rate / 100f, isIncrease) * 100)

  @Test
  fun tenPercentOfTheRateByDefault() {
    assertEquals(SpeechRateAndPitchActor.RATE_STEP_RELATIVE, SpeechRateAndPitchActor.rateStep(prefs))
    assertEquals(110, percent(SpeechRateAndPitchActor.RATE_STEP_RELATIVE, 100, true))
    assertEquals(121, percent(SpeechRateAndPitchActor.RATE_STEP_RELATIVE, 110, true))
    // Close to the normal rate, it is the normal rate.
    assertEquals(100, percent(SpeechRateAndPitchActor.RATE_STEP_RELATIVE, 110, false))
  }

  @Test
  fun fixedStepsMoveToTheNextMultiple() {
    assertEquals(105, percent(5, 100, true))
    assertEquals(95, percent(5, 100, false))
    assertEquals(135, percent(5, 133, true))
    assertEquals(130, percent(5, 133, false))
    assertEquals(130, percent(5, 135, false))
    // Near the normal rate, a small step isn't pulled back to it.
    assertEquals(101, percent(1, 100, true))
    assertEquals(102, percent(1, 101, true))
  }

  @Test
  fun fixedStepsStayInTheRange() {
    assertEquals(10, percent(20, 20, false))
    assertEquals(10, percent(5, 10, false))
    assertEquals(600, percent(20, 590, true))
    assertEquals(600, percent(1, 600, true))
  }

  @Test
  fun unknownStepIsTheDefault() {
    prefs.edit().putString(SpeechRateAndPitchActor.PREF_RATE_STEP, "7").commit()
    assertEquals(SpeechRateAndPitchActor.RATE_STEP_RELATIVE, SpeechRateAndPitchActor.rateStep(prefs))
    prefs.edit().putString(SpeechRateAndPitchActor.PREF_RATE_STEP, "x").commit()
    assertEquals(SpeechRateAndPitchActor.RATE_STEP_RELATIVE, SpeechRateAndPitchActor.rateStep(prefs))
  }

  @Test
  fun rateCommandsUseTheChosenStep() {
    prefs.edit().putString(SpeechRateAndPitchActor.PREF_RATE_STEP, "5").commit()
    prefs.edit().putString(context.getString(R.string.pref_speech_rate_key), "1.0").commit()
    val actor = SpeechRateAndPitchActor(context)
    actor.changeSpeechRate(/* isIncrease= */ true)
    assertEquals(105, actor.speechRatePercentage)
    actor.changeSpeechRate(/* isIncrease= */ true)
    assertEquals(110, actor.speechRatePercentage)
  }
}
