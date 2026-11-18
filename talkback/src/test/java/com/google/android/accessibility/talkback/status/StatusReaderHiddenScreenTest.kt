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


package com.google.android.accessibility.talkback.status

import com.google.android.accessibility.talkback.directtouch.FakeSharedPreferences
import com.google.android.accessibility.talkback.monitor.BatteryMonitor
import java.util.function.BooleanSupplier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StatusReaderHiddenScreenTest {
  private val context = RuntimeEnvironment.getApplication()
  private val reader = StatusReader(context, BatteryMonitor(context))

  @Test
  fun aHiddenScreenIsSaid() {
    reader.screenHidden = BooleanSupplier { true }
    assertEquals("Screen hidden", reader.describe(listOf(StatusItem.HIDDEN_SCREEN)))
  }

  @Test
  fun aShownScreenIsLeftOut() {
    reader.screenHidden = BooleanSupplier { false }
    assertEquals("", reader.describe(listOf(StatusItem.HIDDEN_SCREEN)))
  }

  @Test
  fun theScreenIsShownUntilSandFlySaysOtherwise() {
    assertEquals("", reader.describe(listOf(StatusItem.HIDDEN_SCREEN)))
  }

  @Test
  fun theHiddenScreenIsReadWithTheOtherItems() {
    reader.screenHidden = BooleanSupplier { true }
    val status = reader.describe(listOf(StatusItem.TIME, StatusItem.HIDDEN_SCREEN))
    assertTrue(status, status.endsWith(", Screen hidden"))
  }

  @Test
  fun theHiddenScreenIsOnAndLastForASavedOrderFromBefore() {
    val prefs = FakeSharedPreferences()
    prefs
      .edit()
      .putString("pref_status_item_order", "BATTERY,TIME,WIFI,MOBILE,RINGER,AIRPLANE_MODE")
      .apply()
    assertEquals(StatusItem.HIDDEN_SCREEN, StatusSettings.enabledItems(prefs).last())
    assertTrue(StatusSettings.isEnabled(prefs, StatusItem.HIDDEN_SCREEN))
  }
}
