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

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.google.android.accessibility.talkback.R

/**
 * Gesture actions that open an app, as its icon on the home screen does. The gesture preference
 * saves [PREFIX] and the component name of the app's home screen activity.
 */
object OpenAppActions {
  private const val TAG = "OpenAppActions"

  /** Starts every saved value of these actions. It must never change once released. */
  private const val PREFIX = "OPEN_APP:"

  /** An app with an icon on the home screen, and the action that opens it. */
  data class App(val label: String, val value: String)

  /**
   * The labels of home screen activities by component, with the APK path and the locale they were
   * loaded for. Loading every label takes seconds on some phones; an update or a new language
   * loads it again. Holds only the apps of the last list.
   */
  private val labels = HashMap<String, Pair<String, String>>()

  /** Returns the action that opens [component]. */
  @JvmStatic fun valueOf(component: ComponentName): String = PREFIX + component.flattenToString()

  /** Returns the package of the app [value] opens, or null when it isn't one of these actions. */
  @JvmStatic fun packageName(value: String?): String? = component(value)?.packageName

  /** Returns whether [value] is an action that opens an app. */
  @JvmStatic fun isAction(value: String?): Boolean = component(value) != null

  private fun component(value: String?): ComponentName? =
    if (value != null && value.startsWith(PREFIX)) {
      ComponentName.unflattenFromString(value.substring(PREFIX.length))
    } else {
      null
    }

  /** Returns the apps with an icon on the home screen, by name. */
  @JvmStatic
  fun apps(context: Context): List<App> {
    val packageManager = context.packageManager
    val locale = context.resources.configuration.locales.toLanguageTags()
    val found =
      packageManager
        .queryIntentActivities(homeScreenIntent(), /* flags= */ 0)
        // SandFly has a launcher icon of its own, which opens its settings; leave it out of the list.
        .filterNot { it.activityInfo.packageName == context.packageName }
    return synchronized(labels) {
        val apps =
          found.map {
            val value = valueOf(ComponentName(it.activityInfo.packageName, it.activityInfo.name))
            val key = it.activityInfo.applicationInfo?.sourceDir + "|" + locale
            val label =
              labels[value]?.takeIf { (loadedFor, _) -> loadedFor == key }?.second
                ?: it.loadLabel(packageManager).toString().also { label -> labels[value] = key to label }
            App(label, value)
          }
        labels.keys.retainAll(apps.mapTo(HashSet()) { it.value })
        apps
      }
      .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, App::label).thenBy(App::value))
  }

  /** Returns "Open" and the app's name, or null when [value] isn't one of these actions. */
  @JvmStatic
  fun label(context: Context, value: String?): String? {
    val component = component(value) ?: return null
    return context.getString(R.string.shortcut_open_app, appName(context.packageManager, component))
  }

  private fun appName(packageManager: PackageManager, component: ComponentName): CharSequence =
    try {
      packageManager.getActivityInfo(component, /* flags= */ 0).loadLabel(packageManager)
    } catch (e: PackageManager.NameNotFoundException) {
      try {
        packageManager
          .getApplicationInfo(component.packageName, /* flags= */ 0)
          .loadLabel(packageManager)
      } catch (e: PackageManager.NameNotFoundException) {
        component.packageName
      }
    }

  /**
   * Opens the app of the action saved as [value]. If an update moved its home screen activity, opens
   * the app's new one. Returns false when the app isn't installed.
   */
  @JvmStatic
  fun open(context: Context, value: String): Boolean {
    val component = component(value) ?: return false
    val packageManager = context.packageManager
    val intent =
      if (isInstalled(packageManager, component)) {
        homeScreenIntent().setComponent(component)
      } else {
        packageManager.getLaunchIntentForPackage(component.packageName) ?: return false
      }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    return try {
      context.startActivity(intent)
      true
    } catch (e: ActivityNotFoundException) {
      Log.w(TAG, "Could not open $component", e)
      false
    } catch (e: SecurityException) {
      Log.w(TAG, "Could not open $component", e)
      false
    }
  }

  private fun isInstalled(packageManager: PackageManager, component: ComponentName): Boolean =
    try {
      packageManager.getActivityInfo(component, /* flags= */ 0)
      true
    } catch (e: PackageManager.NameNotFoundException) {
      false
    }

  private fun homeScreenIntent(): Intent =
    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
}
