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

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.SystemClock
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.Filter

/**
 * Finds the pages of the home screen, such as "Home screen 1 of 2" in the Pixel Launcher, which
 * hold the app icons or lie behind them. Touch exploration leaves them out, so touching between
 * icons says nothing; swiping still reaches them.
 */
object HomeScreenPages {
  private const val HOME_CHECK_INTERVAL_MS = 60_000L

  private var homePackage: String? = null
  private var homeCheckedAt = 0L

  /**
   * Whether [node] belongs to the home app and holds items that can take focus themselves, or
   * covers a third of its window or more. The Pixel Launcher puts a view the size of the screen,
   * with no children, behind the icons.
   */
  @JvmStatic
  fun isPage(context: Context, node: AccessibilityNodeInfoCompat): Boolean {
    val packageName = node.packageName?.toString() ?: return false
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
