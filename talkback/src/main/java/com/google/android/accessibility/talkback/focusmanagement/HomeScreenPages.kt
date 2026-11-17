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

package com.google.android.accessibility.talkback.focusmanagement

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Rect
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.Filter

/**
 * Finds the pages of the home screen, such as "Home screen 1 of 2" in the Pixel Launcher, which
 * hold the app icons or lie behind them, and the view behind the lock screen. Touch exploration
 * leaves them out, so touching between icons says nothing; swiping still reaches them.
 */
object HomeScreenPages {
  private const val HOME_CHECK_INTERVAL_MS = 60_000L
  private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
  /** The package of Android's app chooser, which resolves the home intent with no default set. */
  private const val ANDROID_PACKAGE = "android"

  private var homePackage: String? = null
  private var homeCheckedAt = 0L
  private var launchersInUse: Set<String> = emptySet()
  private var launchersCheckedAt = 0L

  /**
   * Whether [node] can't be clicked and either belongs to the home app and holds items that can
   * take focus themselves or covers a third of its window or more, or is a view with no children
   * covering a third of the lock screen. The Pixel Launcher puts a view the size of the screen,
   * with no children, behind the icons, and Android's lock screen puts one called "Lock screen"
   * behind the clock and notifications. Cards in the launcher's recent apps can be clicked.
   */
  @JvmStatic
  fun isPage(context: Context, node: AccessibilityNodeInfoCompat): Boolean {
    val packageName = node.packageName?.toString() ?: return false
    if (AccessibilityNodeInfoUtils.isClickable(node)) {
      return false
    }
    if (packageName == SYSTEM_UI_PACKAGE) {
      return node.childCount == 0 &&
        coversWindow(node) &&
        context.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true
    }
    if (packageName != homePackage(context)) {
      return false
    }
    return coversWindow(node) ||
      AccessibilityNodeInfoUtils.hasMatchingDescendant(
        node,
        Filter.node { it.isVisibleToUser && AccessibilityNodeInfoUtils.isAccessibilityFocusable(it) },
      )
  }

  private fun coversWindow(node: AccessibilityNodeInfoCompat): Boolean {
    val window = node.window ?: return false
    val windowBounds = Rect()
    window.getBoundsInScreen(windowBounds)
    val bounds = Rect()
    node.getBoundsInScreen(bounds)
    val windowArea = windowBounds.width().toLong() * windowBounds.height()
    return windowArea > 0 && bounds.width().toLong() * bounds.height() * 3 >= windowArea
  }

  /** Whether [packageName] is the home app's. */
  @JvmStatic
  fun isHomePackage(context: Context, packageName: CharSequence?): Boolean =
    packageName != null && packageName.toString() == homePackage(context)

  /**
   * Whether [packageName] is the home app in use: the default home app, or any installed home app
   * when there is no default, as Android then resolves the home intent to its app chooser.
   */
  @JvmStatic
  fun isLauncherPackage(context: Context, packageName: CharSequence?): Boolean =
    packageName != null && packageName.toString() in launchersInUse(context)

  private fun launchersInUse(context: Context): Set<String> {
    val now = SystemClock.uptimeMillis()
    if (launchersCheckedAt == 0L || now - launchersCheckedAt > HOME_CHECK_INTERVAL_MS) {
      launchersCheckedAt = now
      val packageManager = context.packageManager
      val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
      launchersInUse =
        launchersInUse(packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)) {
          packageManager.queryIntentActivities(home, PackageManager.MATCH_DEFAULT_ONLY)
        }
    }
    return launchersInUse
  }

  /**
   * Returns the package of [resolved], the activity the home intent resolves to, when it is a home
   * app. Otherwise, such as for Android's app chooser, Settings' FallbackHome or nothing, returns
   * the packages of all home apps among [homeActivities].
   */
  @VisibleForTesting
  @JvmStatic
  fun launchersInUse(
    resolved: ResolveInfo?,
    homeActivities: () -> List<ResolveInfo>,
  ): Set<String> {
    val resolvedPackage = resolved?.takeIf(::isLauncher)?.activityInfo?.packageName
    return if (resolvedPackage != null) setOf(resolvedPackage)
    else homeActivities().filter(::isLauncher).mapNotNull { it.activityInfo?.packageName }.toSet()
  }

  /**
   * Whether [homeActivity] belongs to a home app. Activities with a negative priority, such as the
   * Settings app's FallbackHome, only stand in while no home app runs, and Android's app chooser
   * is not one.
   */
  private fun isLauncher(homeActivity: ResolveInfo): Boolean {
    val packageName = homeActivity.activityInfo?.packageName ?: return false
    return homeActivity.priority >= 0 && packageName != ANDROID_PACKAGE
  }

  private fun homePackage(context: Context): String? {
    val now = SystemClock.uptimeMillis()
    if (homeCheckedAt == 0L || now - homeCheckedAt > HOME_CHECK_INTERVAL_MS) {
      homeCheckedAt = now
      homePackage =
        context.packageManager
          .resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY,
          )
          ?.activityInfo
          ?.packageName
    }
    return homePackage
  }
}
