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

package com.google.android.accessibility.talkback.gesture

import android.app.Application
import android.content.ClipData
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.net.Uri
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppShortcutActionsTest {
  private val context: Application = RuntimeEnvironment.getApplication()
  private val prefs = SharedPreferencesUtils.getSharedPreferences(context)
  private val macro = ComponentName("com.example.macro", "com.example.macro.RunMacro")

  private fun install(component: ComponentName, exported: Boolean) {
    val activity =
      ActivityInfo().apply {
        packageName = component.packageName
        name = component.className
        this.exported = exported
        applicationInfo = ApplicationInfo().apply { packageName = component.packageName }
      }
    if (component.packageName != context.packageName) {
      shadowOf(context.packageManager)
        .installPackage(
          PackageInfo().apply {
            packageName = component.packageName
            applicationInfo = activity.applicationInfo
          }
        )
    }
    shadowOf(context.packageManager).addOrUpdateActivity(activity)
  }

  private fun add(id: String, name: String, intent: Intent, packageName: String = macro.packageName) {
    AppShortcutActions.writeAdded(
      prefs,
      AppShortcutActions.readAdded(prefs) +
        AppShortcutActions.Added(id, name, packageName, intent.toUri(Intent.URI_INTENT_SCHEME)),
    )
  }

  @Test
  fun staticValueKeepsThePackageAndTheShortcutId() {
    val value = AppShortcutActions.staticValue("com.example.shop", "cart/today")

    assertTrue(AppShortcutActions.isAction(value))
    assertEquals("com.example.shop" to "cart/today", AppShortcutActions.staticRef(value))
    assertNull(AppShortcutActions.addedId(value))
  }

  @Test
  fun addedValueKeepsTheId() {
    val value = AppShortcutActions.addedValue("1234")

    assertTrue(AppShortcutActions.isAction(value))
    assertEquals("1234", AppShortcutActions.addedId(value))
    assertNull(AppShortcutActions.staticRef(value))
  }

  @Test
  fun otherValuesAreNoAction() {
    assertFalse(AppShortcutActions.isAction(null))
    assertFalse(AppShortcutActions.isAction("NEXT"))
    assertFalse(AppShortcutActions.isAction("OPEN_APP:com.example/.Main"))
    assertFalse(AppShortcutActions.isAction("ACCESSIBILITY_SHORTCUT:com.example/.Main"))
    assertFalse(AppShortcutActions.isAction("APP_SHORTCUT:"))
    assertFalse(AppShortcutActions.isAction("APP_SHORTCUT:com.example.shop"))
    assertFalse(AppShortcutActions.isAction("APP_SHORTCUT:com.example.shop/"))
    assertFalse(AppShortcutActions.isAction("APP_SHORTCUT:/cart"))
    assertFalse(AppShortcutActions.isAction("ADDED_APP_SHORTCUT:"))
    assertNull(AppShortcutActions.label(context, "NEXT"))
  }

  @Test
  fun addedShortcutsAreKeptAsJson() {
    val shortcuts =
      listOf(
        AppShortcutActions.Added("a", "Morning", "com.example.macro", "intent:#Intent;action=A;end"),
        AppShortcutActions.Added("b", "", "com.example.dial", "intent:#Intent;action=B;end"),
      )

    assertEquals(shortcuts, AppShortcutActions.fromJson(AppShortcutActions.toJson(shortcuts)))
  }

  @Test
  fun badOrIncompleteJsonGivesNoShortcuts() {
    assertTrue(AppShortcutActions.fromJson(null).isEmpty())
    assertTrue(AppShortcutActions.fromJson("not json").isEmpty())
    assertTrue(AppShortcutActions.fromJson("""[{"name":"No ID","intent":"intent:#Intent;end"}]""").isEmpty())
    assertTrue(AppShortcutActions.fromJson("""[{"id":"a","name":"No intent"}]""").isEmpty())
  }

  @Test
  fun sanitizedLeavesOutTheSelectorClipDataAndUriGrants() {
    val intent =
      Intent(Intent.ACTION_VIEW, Uri.parse("content://com.example/1"))
        .setComponent(macro)
        .addFlags(
          Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
            Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        )
    intent.selector = Intent(Intent.ACTION_MAIN).setPackage(context.packageName)
    intent.clipData = ClipData.newRawUri("", Uri.parse("content://com.example/2"))

    val safe = AppShortcutActions.sanitized(intent, context.packageName)

    assertNotNull(safe)
    assertNull(safe!!.selector)
    assertNull(safe.clipData)
    assertEquals(Intent.FLAG_ACTIVITY_CLEAR_TOP, safe.flags)
    assertEquals(macro, safe.component)
    // The original is left as it was.
    assertNotNull(intent.selector)
  }

  @Test
  fun sanitizedRefusesSandFlysOwnActivities() {
    val own = context.packageName
    assertNull(AppShortcutActions.sanitized(Intent().setClassName(own, "$own.Settings"), own))
    assertNull(AppShortcutActions.sanitized(Intent(Intent.ACTION_VIEW).setPackage(own), own))
  }

  @Test
  fun parseReadsTheSavedIntent() {
    val intent = Intent("com.example.macro.RUN").setComponent(macro).putExtra("macro", "Morning")

    val parsed = AppShortcutActions.parse(intent.toUri(Intent.URI_INTENT_SCHEME))

    assertEquals("com.example.macro.RUN", parsed?.action)
    assertEquals(macro, parsed?.component)
    assertEquals("Morning", parsed?.getStringExtra("macro"))
    assertNull(AppShortcutActions.parse("intent:#Intent;action=A;S.unterminated"))
  }

  @Test
  fun labelOfAnAddedShortcutNamesItsAppAndItself() {
    add("m", "Morning", Intent("com.example.macro.RUN").setComponent(macro))

    // The app isn't installed, so its package names it.
    assertEquals("com.example.macro: Morning", AppShortcutActions.label(context, AppShortcutActions.addedValue("m")))
    assertNull(AppShortcutActions.label(context, AppShortcutActions.addedValue("deleted")))
  }

  @Test
  fun addedShortcutsAreListed() {
    add("m", "Morning", Intent("com.example.macro.RUN").setComponent(macro))

    assertEquals(
      listOf(AppShortcutActions.Shortcut("com.example.macro: Morning", AppShortcutActions.addedValue("m"))),
      AppShortcutActions.shortcuts(context),
    )
  }

  @Test
  fun openStartsAnAddedShortcutInANewTask() {
    install(macro, exported = true)
    add("m", "Morning", Intent("com.example.macro.RUN").setComponent(macro))

    assertTrue(AppShortcutActions.open(context, AppShortcutActions.addedValue("m")))

    val started = shadowOf(context).nextStartedActivity
    assertEquals(macro, started.component)
    assertEquals("com.example.macro.RUN", started.action)
    assertTrue((started.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
  }

  @Test
  fun openRefusesAnActivityThatIsNotExported() {
    install(macro, exported = false)
    add("m", "Morning", Intent("com.example.macro.RUN").setComponent(macro))

    assertFalse(AppShortcutActions.open(context, AppShortcutActions.addedValue("m")))
    assertNull(shadowOf(context).nextStartedActivity)
  }

  @Test
  fun openRefusesSandFlysOwnActivities() {
    val own = ComponentName(context.packageName, "${context.packageName}.Settings")
    install(own, exported = false)
    add("own", "Settings", Intent().setComponent(own))

    assertFalse(AppShortcutActions.open(context, AppShortcutActions.addedValue("own")))
    assertNull(shadowOf(context).nextStartedActivity)
  }

  @Test
  fun openFailsForADeletedShortcut() {
    assertFalse(AppShortcutActions.open(context, AppShortcutActions.addedValue("deleted")))
  }
}
