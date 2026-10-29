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

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Checks that gestures run an app's own commands only in that app. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppGesturesTest {

  private val prefs =
    RuntimeEnvironment.getApplication().getSharedPreferences("app_gestures_test", Context.MODE_PRIVATE)

  @After
  fun forget() {
    prefs.edit().clear().apply()
    AppGestures.load(prefs)
    AppGestures.onWindowShown(null)
  }

  @Test
  fun gestureRunsTheAppsCommandOnlyInThatApp() {
    AppGestures.write(
      prefs,
      mapOf("com.example.player" to AppGestures.App("Player", mapOf("pref_shortcut_up" to "FAST_FORWARD"))),
    )
    AppGestures.load(prefs)
    AppGestures.onWindowShown("com.example.player")
    assertEquals("FAST_FORWARD", AppGestures.action("pref_shortcut_up"))
    assertNull(AppGestures.action("pref_shortcut_down"))
    AppGestures.onWindowShown("com.example.mail")
    assertNull(AppGestures.action("pref_shortcut_up"))
  }

  @Test
  fun appsSurviveSaving() {
    val apps = mapOf("com.example" to AppGestures.App("Example", mapOf("a" to "B", "c" to "UNASSIGNED")))
    assertEquals(apps, AppGestures.fromJson(AppGestures.toJson(apps)))
  }

  @Test
  fun brokenSettingIsNoApps() {
    assertEquals(emptyMap<String, AppGestures.App>(), AppGestures.fromJson("{"))
  }
}
