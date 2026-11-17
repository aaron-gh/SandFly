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

package com.google.android.accessibility.talkback.focusmanagement

import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeScreenPagesTest {
  private fun home(packageName: String, priority: Int = 0): ResolveInfo =
    ResolveInfo().apply {
      activityInfo =
        ActivityInfo().apply {
          this.packageName = packageName
          name = "$packageName.Home"
        }
      this.priority = priority
    }

  private val samsung = home("com.sec.android.app.launcher")
  private val other = home("com.dialect.launcher")

  @Test
  fun defaultHomeApp_isTheOnlyOneInUse() {
    assertEquals(
      setOf("com.sec.android.app.launcher"),
      HomeScreenPages.launchersInUse(samsung) { throw AssertionError("not needed") },
    )
  }

  @Test
  fun appChooser_meansEveryInstalledHomeAppCounts() {
    assertEquals(
      setOf("com.sec.android.app.launcher", "com.dialect.launcher"),
      HomeScreenPages.launchersInUse(home("android")) { listOf(samsung, other) },
    )
  }

  @Test
  fun noResolvedActivity_meansEveryInstalledHomeAppCounts() {
    assertEquals(
      setOf("com.sec.android.app.launcher", "com.dialect.launcher"),
      HomeScreenPages.launchersInUse(null) { listOf(samsung, other) },
    )
  }

  @Test
  fun fallbackHome_meansEveryInstalledHomeAppCounts_withoutFallbackHomeOrTheAppChooser() {
    val fallbackHome = home("com.android.settings", priority = -1000)
    assertEquals(
      setOf("com.sec.android.app.launcher"),
      HomeScreenPages.launchersInUse(fallbackHome) {
        listOf(samsung, fallbackHome, home("android"))
      },
    )
  }
}
