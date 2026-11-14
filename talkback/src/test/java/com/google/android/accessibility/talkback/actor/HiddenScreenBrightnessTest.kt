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

import android.content.Context
import android.provider.Settings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HiddenScreenBrightnessTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private val resolver = context.contentResolver
  private var canWrite = true

  @Before
  fun setUp() {
    HiddenScreenBrightness.writePermission = { canWrite }
    Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 120)
    Settings.System.putInt(
      resolver,
      Settings.System.SCREEN_BRIGHTNESS_MODE,
      Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC,
    )
  }

  @After
  fun tearDown() {
    HiddenScreenBrightness.writePermission = { Settings.System.canWrite(it) }
  }

  private fun brightness() = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS)

  private fun mode() = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE)

  @Test
  fun restore_bringsBackTheBrightnessAndMode() {
    HiddenScreenBrightness.lower(context)
    assertEquals(1, brightness())
    assertEquals(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL, mode())

    HiddenScreenBrightness.restore(context)
    assertEquals(120, brightness())
    assertEquals(Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC, mode())
  }

  @Test
  fun restore_withoutPermission_keepsTheSavedValuesForLater() {
    HiddenScreenBrightness.lower(context)
    canWrite = false
    HiddenScreenBrightness.restore(context)
    assertEquals(1, brightness())

    canWrite = true
    HiddenScreenBrightness.restore(context)
    assertEquals(120, brightness())
    assertEquals(Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC, mode())

    // Once restored, the values are gone, so a later restore leaves the brightness alone.
    Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 200)
    HiddenScreenBrightness.restore(context)
    assertEquals(200, brightness())
  }
}
