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

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OpenAppActionsTest {
  private val context: Application = RuntimeEnvironment.getApplication()
  private val mail = ComponentName("com.example.mail", "com.example.mail.Inbox")
  private val atlas = ComponentName("com.example.atlas", "com.example.atlas.Map")

  @Before
  fun setUp() {
    installHomeScreenApp(mail, "mail")
    installHomeScreenApp(atlas, "Atlas")
  }

  private fun installHomeScreenApp(component: ComponentName, label: String) {
    val activity =
      ActivityInfo().apply {
        packageName = component.packageName
        name = component.className
        nonLocalizedLabel = label
        applicationInfo =
          ApplicationInfo().apply {
            packageName = component.packageName
            nonLocalizedLabel = label
          }
      }
    val packageManager = shadowOf(context.packageManager)
    packageManager.addOrUpdateActivity(activity)
    packageManager.addResolveInfoForIntent(
      Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
      ResolveInfo().apply { activityInfo = activity },
    )
  }

  @Test
  fun appsAreListedByNameIgnoringCase() {
    assertEquals(
      listOf(
        OpenAppActions.App("Atlas", OpenAppActions.valueOf(atlas)),
        OpenAppActions.App("mail", OpenAppActions.valueOf(mail)),
      ),
      OpenAppActions.apps(context),
    )
  }

  @Test
  fun onlyOpenAppValuesAreActions() {
    assertTrue(OpenAppActions.isAction(OpenAppActions.valueOf(mail)))
    assertFalse(OpenAppActions.isAction("NEXT"))
    assertFalse(OpenAppActions.isAction("OPEN_APP:"))
    assertFalse(OpenAppActions.isAction(null))
    assertNull(OpenAppActions.label(context, "NEXT"))
  }

  @Test
  fun labelNamesTheApp() {
    assertEquals("Open Atlas", OpenAppActions.label(context, OpenAppActions.valueOf(atlas)))
  }

  @Test
  fun labelOfAnAppNoLongerInstalledNamesItsPackage() {
    val gone = ComponentName("com.example.gone", "com.example.gone.Main")
    assertEquals("Open com.example.gone", OpenAppActions.label(context, OpenAppActions.valueOf(gone)))
  }

  @Test
  fun openStartsTheAppInANewTask() {
    assertTrue(OpenAppActions.open(context, OpenAppActions.valueOf(mail)))

    val started = shadowOf(context).nextStartedActivity
    assertEquals(mail, started.component)
    assertTrue((started.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
  }

  @Test
  fun openFailsForAnAppNoLongerInstalled() {
    val gone = ComponentName("com.example.gone", "com.example.gone.Main")
    assertFalse(OpenAppActions.open(context, OpenAppActions.valueOf(gone)))
  }
}
