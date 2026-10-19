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

package com.google.android.accessibility.talkback.scripting

import android.accessibilityservice.AccessibilityService
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject

class ScriptActivation(private val service: AccessibilityService) {
  private val ownPackage = service.packageName
  private var activePackage: String? = null
  private var focusedPackage: String? = null
  private var metaDataPackage: String? = null
  private var metaData = JSONObject()

  var windowTitle: String? = null
    private set

  @Volatile
  var appInfo: JSONObject = JSONObject()
    private set

  val activity: String?
    get() = activePackage?.let(activityByPackage::get)

  fun onWindowStateChanged(event: AccessibilityEvent) {
    val packageName = event.packageName?.toString() ?: return
    val className = event.className?.toString() ?: return
    if (!className.startsWith("android.")) {
      activityByPackage[packageName] = className
    }
  }

  fun onFocusMoved(packageName: String?): Boolean =
    (packageName != focusedPackage).also { focusedPackage = packageName }

  fun refresh() {
    readActiveWindow()
    appInfo =
      jsonObject(
        "package" to activePackage,
        "activity" to activity,
        "window" to windowTitle,
        "meta" to frontAppMetaData(),
      )
  }

  /** The front app's manifest meta-data, read again only when another app comes to the front. */
  private fun frontAppMetaData(): JSONObject {
    val pkg = activePackage
    if (pkg != metaDataPackage) {
      metaDataPackage = pkg
      metaData = pkg?.let(::readMetaData) ?: JSONObject()
    }
    return metaData
  }

  private fun readMetaData(pkg: String): JSONObject {
    val meta = JSONObject()
    val bundle =
      try {
        service.packageManager.getApplicationInfo(pkg, PackageManager.GET_META_DATA).metaData
      } catch (e: PackageManager.NameNotFoundException) {
        null
      } ?: return meta
    for (key in bundle.keySet()) {
      @Suppress("DEPRECATION")
      when (val value = bundle.get(key)) {
        is Boolean,
        is Int,
        is Long,
        is Float,
        is Double,
        is String -> meta.put(key, value)
      }
    }
    return meta
  }

  /**
   * Whether a Backtalk screen is in front. Scripts change nothing there: one that silences speech,
   * hides items or takes over gestures would otherwise leave no way to reach Scripts in Backtalk's
   * settings and turn it off. Scripts for all apps stay loaded, so that their settings still reach
   * them.
   */
  @Volatile
  var inBacktalk = false
    private set

  fun wants(script: InstalledScript): Boolean =
    script.enabled &&
      (script.manifest.isGlobal ||
        script.manifest.apps.any {
          it.packageName != ownPackage &&
            (it.matchesFront(activePackage, activity, windowTitle) ||
              (focusedPackage != activePackage && it.matchesOther(focusedPackage)))
        })

  private fun readActiveWindow() {
    val windows = runCatching { service.windows.orEmpty() }.getOrDefault(emptyList())
    val active =
      windows.firstOrNull {
        it.isActive && it.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY
      } ?: windows.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_APPLICATION } ?: return
    activePackage = active.root?.packageName?.toString() ?: return
    inBacktalk = activePackage == ownPackage
    windowTitle = active.title?.toString()
  }

  private companion object {
    // An activity is only known from the app's window changes. Turning Backtalk off and on starts a
    // new service in the same process, with the app often still open and sending no window change,
    // so what is known outlives the service. It can be out of date if the app changed activity while
    // Backtalk was off, until the app's next window change.
    val activityByPackage = ConcurrentHashMap<String, String>()
  }
}
