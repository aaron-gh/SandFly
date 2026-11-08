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

package com.google.android.accessibility.talkback.applauncher

import com.google.android.accessibility.talkback.gesture.OpenAppActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeScreenAppsTest {
  private val maps = OpenAppActions.App("Google Maps", "OPEN_APP:maps")
  private val clock = OpenAppActions.App("Clock", "OPEN_APP:clock")
  private val secondClock = OpenAppActions.App("Clock", "OPEN_APP:second")
  private val apps = HomeScreenApps.index(listOf(maps, clock, secondClock))

  @Test
  fun findsByText() {
    assertEquals(clock, HomeScreenApps.find(apps, "Clock", null))
  }

  @Test
  fun comparesWithoutCaseOrSpaces() {
    assertEquals(maps, HomeScreenApps.find(apps, "google  maps", null))
  }

  @Test
  fun findsByDescriptionWithNotificationText() {
    assertEquals(maps, HomeScreenApps.find(apps, null, "Google Maps, 2 notifications"))
  }

  @Test
  fun findsPredictedApp() {
    assertEquals(maps, HomeScreenApps.find(apps, null, "Predicted app: Google Maps"))
  }

  @Test
  fun keepsTheFirstOfAppsWithOneName() {
    assertEquals("OPEN_APP:clock", HomeScreenApps.find(apps, "Clock", null)?.value)
  }

  @Test
  fun otherTextFindsNothing() {
    assertNull(HomeScreenApps.find(apps, "Weather", "Weather widget"))
    assertNull(HomeScreenApps.find(apps, null, null))
  }
}
