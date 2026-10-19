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

package com.google.android.accessibility.talkback.audio

import android.content.Context
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpeakerDevicesTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private val prefs = SharedPreferencesUtils.getSharedPreferences(context)

  @Test
  fun connectedChosenOutputMatchesWhateverTheCase() {
    assertTrue(SpeakerDevices.anyChosen(listOf("Pixel Buds", "JBL Flip 6"), setOf("jbl flip 6")))
    assertFalse(SpeakerDevices.anyChosen(listOf("Pixel Buds"), setOf("JBL Flip 6")))
    assertFalse(SpeakerDevices.anyChosen(emptyList(), setOf("JBL Flip 6")))
  }

  @Test
  fun rememberedOutputsAreNewestFirstWithoutRepeats() {
    SpeakerDevices.remember(prefs, context, listOf("JBL Flip 6"))
    SpeakerDevices.remember(prefs, context, listOf("Pixel Buds", "Pixel Buds"))
    SpeakerDevices.remember(prefs, context, listOf("jbl flip 6"))

    assertEquals(listOf("jbl flip 6", "Pixel Buds"), SpeakerDevices.seen(prefs, context))
  }

  @Test
  fun rememberedOutputsAreCapped() {
    for (i in 1..30) {
      SpeakerDevices.remember(prefs, context, listOf("Device $i"))
    }

    val seen = SpeakerDevices.seen(prefs, context)
    assertEquals(20, seen.size)
    assertEquals("Device 30", seen.first())
  }

  @Test
  fun choicesListConnectedThenSeenThenOtherChosen() {
    assertEquals(
      listOf("Pixel Buds", "JBL Flip 6", "Car", "Old earbuds"),
      SpeakerDevices.choices(
        connected = listOf("Pixel Buds"),
        seen = listOf("JBL Flip 6", "pixel buds", "Car"),
        chosen = setOf("Old earbuds", "Pixel Buds"),
      ),
    )
  }
}
