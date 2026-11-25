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

package com.google.android.accessibility.talkback.directtouch

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import com.google.android.accessibility.utils.DeprecatedApiNames

/**
 * What an app's manifest says about direct touch.
 *
 * @property asksForDirectTouch whether direct touch is turned on for the app the first time SandFly
 *   sees it: it sets [DirectTouchSettings.CAPABILITY_KEY], declares where it wants direct touch, or
 *   holds the direct touch permission.
 * @property declaration where the app wants direct touch, or null when it doesn't say
 * @property managesDirectTouch whether the app says where it wants direct touch, in its manifest or
 *   while it runs, so that it gets none until it does. Other apps get the whole screen.
 */
data class DirectTouchApp(
  val asksForDirectTouch: Boolean,
  val declaration: DirectTouchDeclaration?,
  val managesDirectTouch: Boolean,
) {
  companion object {
    val UNKNOWN = DirectTouchApp(false, null, false)
  }
}

object DirectTouchCapability {
  /**
   * Reads the app's manifest once: its meta-data, from its application or else its launch activity,
   * and whether it holds [permission].
   */
  fun read(pm: PackageManager, packageName: String, permission: String): DirectTouchApp {
    try {
      @Suppress("DEPRECATION")
      val info =
        pm.getPackageInfo(packageName, PackageManager.GET_META_DATA or PackageManager.GET_PERMISSIONS)
      val appInfo = info.applicationInfo ?: return DirectTouchApp.UNKNOWN
      val holdsPermission = info.requestedPermissions?.contains(permission) == true
      val declaration = readDeclaration(pm, appInfo, packageName)
      val flag = declares(appInfo.metaData) || launchActivityDeclares(pm, packageName)
      return DirectTouchApp(
        asksForDirectTouch = flag || declaration != null || holdsPermission,
        declaration = declaration,
        managesDirectTouch = declaration != null || holdsPermission,
      )
    } catch (_: Exception) {
      return DirectTouchApp.UNKNOWN
    }
  }

  /**
   * The declaration in the XML resource that [DirectTouchDeclaration.META_DATA_KEY] names, or else
   * the deprecated [DirectTouchDeclaration.LEGACY_META_DATA_KEY].
   */
  private fun readDeclaration(
    pm: PackageManager,
    appInfo: ApplicationInfo,
    packageName: String,
  ): DirectTouchDeclaration? {
    val metaData = appInfo.metaData ?: return null
    val key =
      when {
        metaData.containsKey(DirectTouchDeclaration.META_DATA_KEY) ->
          DirectTouchDeclaration.META_DATA_KEY
        metaData.containsKey(DirectTouchDeclaration.LEGACY_META_DATA_KEY) -> {
          DeprecatedApiNames.warn(
            packageName,
            DirectTouchDeclaration.LEGACY_META_DATA_KEY,
            DirectTouchDeclaration.META_DATA_KEY,
          )
          DirectTouchDeclaration.LEGACY_META_DATA_KEY
        }
        else -> return null
      }
    val parser = appInfo.loadXmlMetaData(pm, key)
    if (parser == null) {
      // The key names no XML resource, so the declaration is empty: no direct touch until asked.
      return DirectTouchDeclaration(emptyList())
    }
    try {
      val resources = pm.getResourcesForApplication(appInfo)
      return DirectTouchDeclaration.parse(parser, packageName) { id ->
        try {
          resources.getResourceName(id)
        } catch (_: Exception) {
          null
        }
      }
    } finally {
      parser.close()
    }
  }

  private fun launchActivityDeclares(pm: PackageManager, packageName: String): Boolean {
    val component = pm.getLaunchIntentForPackage(packageName)?.component ?: return false
    @Suppress("DEPRECATION")
    return declares(pm.getActivityInfo(component, PackageManager.GET_META_DATA).metaData)
  }

  private fun declares(metaData: Bundle?): Boolean =
    metaData?.getBoolean(DirectTouchSettings.CAPABILITY_KEY, false) == true
}
