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

package com.google.android.accessibility.talkback.doubletaptaps

import com.google.android.accessibility.talkback.directtouch.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DoubleTapTapsAppsTest {
  private val prefs = FakeSharedPreferences()

  @Test
  fun offByDefault() {
    assertFalse(DoubleTapTapsApps.isOn(prefs, "com.yum.pizzahut"))
    assertFalse(DoubleTapTapsApps.isOn(prefs, null))
  }

  @Test
  fun toggleAddsAndRemovesTheApp() {
    assertTrue(DoubleTapTapsApps.toggle(prefs, "com.yum.pizzahut"))
    assertTrue(DoubleTapTapsApps.toggle(prefs, "com.zhiliaoapp.musically"))
    assertEquals(
      setOf("com.yum.pizzahut", "com.zhiliaoapp.musically"),
      prefs.getStringSet(DoubleTapTapsApps.PREF_APPS, null),
    )
    assertFalse(DoubleTapTapsApps.toggle(prefs, "com.yum.pizzahut"))
    assertFalse(DoubleTapTapsApps.isOn(prefs, "com.yum.pizzahut"))
    assertTrue(DoubleTapTapsApps.isOn(prefs, "com.zhiliaoapp.musically"))
  }

  @Test
  fun setOnKeepsOtherApps() {
    DoubleTapTapsApps.setOn(prefs, "a.b", true)
    DoubleTapTapsApps.setOn(prefs, "c.d", true)
    DoubleTapTapsApps.setOn(prefs, "a.b", false)
    assertEquals(setOf("c.d"), prefs.getStringSet(DoubleTapTapsApps.PREF_APPS, null))
  }

  @Test
  fun tapsInChosenAppsOrForScriptRules() {
    DoubleTapTapsApps.setOn(prefs, "com.yum.pizzahut", true)
    assertTrue(DoubleTapTapsApps.tapsToActivate(prefs, scriptTouches = false, pkg = "com.yum.pizzahut"))
    assertFalse(DoubleTapTapsApps.tapsToActivate(prefs, scriptTouches = false, pkg = "other.app"))
    assertFalse(DoubleTapTapsApps.tapsToActivate(prefs, scriptTouches = false, pkg = null))
    assertTrue(DoubleTapTapsApps.tapsToActivate(prefs, scriptTouches = true, pkg = "other.app"))
  }
}
