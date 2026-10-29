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
import android.view.KeyEvent
import com.google.android.accessibility.talkback.keyboard.KeyCombo
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
    val apps =
      mapOf("com.example" to AppGestures.App("Example", mapOf("a" to "B", "c" to "UNASSIGNED"), mapOf("1:true" to "D")))
    assertEquals(apps, AppGestures.fromJson(AppGestures.toJson(apps)))
  }

  @Test
  fun keyboardShortcutsRunTheAppsCommandOnlyInThatApp() {
    val keys = KeyCombo(KeyEvent.META_META_ON, KeyEvent.KEYCODE_K, /* triggerModifierUsed= */ true)
    AppGestures.write(
      prefs,
      mapOf(
        "com.example.player" to
          AppGestures.App("Player", emptyMap(), mapOf(AppGestures.keyName(keys) to "FAST_FORWARD"))
      ),
    )
    AppGestures.load(prefs)
    AppGestures.onWindowShown("com.example.player")
    assertEquals(listOf(keys to "FAST_FORWARD"), AppGestures.keys())
    AppGestures.onWindowShown("com.example.mail")
    assertEquals(emptyList<Pair<KeyCombo, String>>(), AppGestures.keys())
  }

  @Test
  fun keyNamesKeepTheKeys() {
    val keys = KeyCombo(KeyEvent.META_CTRL_ON, KeyEvent.KEYCODE_J, /* triggerModifierUsed= */ false)
    assertEquals(keys, AppGestures.keyCombo(AppGestures.keyName(keys)))
    assertNull(AppGestures.keyCombo("not keys"))
  }

  @Test
  fun brokenSettingIsNoApps() {
    assertEquals(emptyMap<String, AppGestures.App>(), AppGestures.fromJson("{"))
  }
}
