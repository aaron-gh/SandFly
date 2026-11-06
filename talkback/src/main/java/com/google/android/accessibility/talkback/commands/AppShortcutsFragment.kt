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

package com.google.android.accessibility.talkback.commands

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.IntentCompat
import androidx.preference.PreferenceGroup
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.gesture.AppShortcutActions
import com.google.android.accessibility.talkback.notifications.NotificationCodeFragment

/**
 * Gestures > App shortcuts: adds shortcuts from apps' create shortcut activities, as a launcher
 * adds them to the home screen, and renames or deletes them. Each one is a command in the App
 * shortcuts category, with the apps' static shortcuts.
 */
class AppShortcutsFragment : NotificationCodeFragment(R.string.title_pref_app_shortcuts) {

  /** The create shortcut activity started, and its name, until it returns. */
  private var creating: ComponentName? = null
  private var creatingName: String? = null

  private val create =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { onCreated(it) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    creating =
      savedInstanceState?.getString(STATE_COMPONENT)?.let { ComponentName.unflattenFromString(it) }
    creatingName = savedInstanceState?.getString(STATE_NAME)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    creating?.let { outState.putString(STATE_COMPONENT, it.flattenToString()) }
    creatingName?.let { outState.putString(STATE_NAME, it) }
  }

  override fun build(screen: PreferenceGroup) {
    val context = requireContext()
    screen.action(getString(R.string.title_pref_add_app_shortcut)) { add() }
    for (shortcut in AppShortcutActions.readAdded(prefs)) {
      val title =
        AppShortcutActions.label(context, AppShortcutActions.addedValue(shortcut.id)) ?: shortcut.name
      screen.action(title) { edit(shortcut.id, title) }
    }
  }

  /** Asks for an app's create shortcut activity, and starts it. */
  private fun add() {
    val context = requireContext()
    val packageManager = context.packageManager
    val activities =
      packageManager
        .queryIntentActivities(Intent(Intent.ACTION_CREATE_SHORTCUT), /* flags= */ 0)
        .map { it.activityInfo }
        .filter { it.packageName != context.packageName && it.exported }
        .map { info ->
          val app = info.applicationInfo.loadLabel(packageManager).toString()
          val name = info.loadLabel(packageManager).toString()
          val shown =
            if (name.contains(app, ignoreCase = true)) name
            else getString(R.string.app_shortcut_label, app, name)
          Triple(ComponentName(info.packageName, info.name), name, shown)
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.third })
    if (activities.isEmpty()) {
      toast(R.string.app_shortcut_no_apps)
      return
    }
    choose(getString(R.string.title_pref_add_app_shortcut), activities.map { it.third }) { index ->
      val (component, name, _) = activities[index]
      creating = component
      creatingName = name
      try {
        create.launch(Intent(Intent.ACTION_CREATE_SHORTCUT).setComponent(component))
      } catch (e: ActivityNotFoundException) {
        creating = null
        toast(R.string.app_shortcut_not_added)
      } catch (e: SecurityException) {
        creating = null
        toast(R.string.app_shortcut_not_added)
      }
    }
  }

  /** Keeps the shortcut that the create shortcut activity returned, if it returned one. */
  private fun onCreated(result: ActivityResult) {
    val component = creating ?: return
    val fallbackName = creatingName
    creating = null
    creatingName = null
    if (result.resultCode != Activity.RESULT_OK) {
      return
    }
    val context = requireContext()
    val data = result.data
    if (data == null) {
      toast(R.string.app_shortcut_not_added)
      return
    }
    // The extra that launchers read before Android 8. AndroidX ShortcutManagerCompat still adds it.
    @Suppress("DEPRECATION")
    val intent = IntentCompat.getParcelableExtra(data, Intent.EXTRA_SHORTCUT_INTENT, Intent::class.java)
    if (intent == null || !AppShortcutActions.openable(context, intent, component.packageName)) {
      toast(R.string.app_shortcut_not_added)
      return
    }
    @Suppress("DEPRECATION")
    val name =
      data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME)?.trim()?.takeIf { it.isNotEmpty() }
        ?: fallbackName
        ?: appName(component.packageName)
    val added =
      AppShortcutActions.Added(
        AppShortcutActions.newId(),
        name,
        component.packageName,
        intent.toUri(Intent.URI_INTENT_SCHEME),
      )
    AppShortcutActions.writeAdded(prefs, AppShortcutActions.readAdded(prefs) + added)
    rebuild()
    AppShortcutActions.label(context, AppShortcutActions.addedValue(added.id))?.let {
      listView?.announceForAccessibility(getString(R.string.app_shortcut_added, it))
    }
  }

  private fun edit(id: String, title: String) {
    choose(title, listOf(getString(R.string.voice_profile_rename), getString(R.string.voice_profile_delete))) {
      if (it == 0) rename(id) else delete(id, title)
    }
  }

  private fun rename(id: String) {
    val shortcut = AppShortcutActions.readAdded(prefs).firstOrNull { it.id == id } ?: return
    askText(
      getString(R.string.voice_profile_rename),
      shortcut.name,
      hint = getString(R.string.title_pref_voice_profile_name),
    ) { name, _, _ ->
      if (name.isEmpty()) {
        return@askText ""
      }
      AppShortcutActions.writeAdded(
        prefs,
        AppShortcutActions.readAdded(prefs).map { if (it.id == id) it.copy(name = name) else it },
      )
      rebuild()
      null
    }
  }

  private fun delete(id: String, title: String) {
    confirm(getString(R.string.voice_profile_delete_confirm, title), R.string.voice_profile_delete) {
      AppShortcutActions.writeAdded(prefs, AppShortcutActions.readAdded(prefs).filterNot { it.id == id })
      rebuild()
    }
  }

  private fun appName(packageName: String): String {
    val packageManager = requireContext().packageManager
    return try {
      packageManager.getApplicationInfo(packageName, /* flags= */ 0).loadLabel(packageManager).toString()
    } catch (e: PackageManager.NameNotFoundException) {
      packageName
    }
  }

  private companion object {
    const val STATE_COMPONENT = "creating_component"
    const val STATE_NAME = "creating_name"
  }
}
