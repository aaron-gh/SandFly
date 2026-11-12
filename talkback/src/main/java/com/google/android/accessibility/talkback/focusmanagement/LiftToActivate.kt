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

import com.google.android.accessibility.utils.ValueSteps

/** Where lifting a finger after touch exploring activates the item under it. */
enum class LiftToActivateMode(val prefValue: String) {
  DISABLED("0"),
  NAVIGATION_BAR("1"),
  ENTIRE_SCREEN("2");

  /** The mode after this one, as [ValueSteps] goes. */
  fun next(): LiftToActivateMode = entries[ValueSteps.step(ordinal, entries.size, true)]

  /** The mode before this one, as [ValueSteps] goes. */
  fun previous(): LiftToActivateMode = entries[ValueSteps.step(ordinal, entries.size, false)]

  companion object {
    /** Returns the mode stored as [prefValue], or [DISABLED] if it is missing or unknown. */
    @JvmStatic
    fun fromPrefValue(prefValue: String?): LiftToActivateMode =
      entries.firstOrNull { it.prefValue == prefValue } ?: DISABLED
  }
}

/** Decides whether lifting the finger on a node should activate it. */
object LiftToActivate {
  private const val SYSTEM_UI_PACKAGE = "com.android.systemui"

  // The buttons of the system navigation bar, as named by their view ids.
  private val NAVIGATION_BAR_BUTTON_IDS =
    setOf("back", "home", "recent_apps", "menu", "ime_switcher", "accessibility_button")

  /** Returns whether a lift on a node should activate it under [mode]. */
  @JvmStatic
  fun shouldActivate(mode: LiftToActivateMode, isNavigationBarButton: Boolean): Boolean =
    when (mode) {
      LiftToActivateMode.DISABLED -> false
      LiftToActivateMode.NAVIGATION_BAR -> isNavigationBarButton
      LiftToActivateMode.ENTIRE_SCREEN -> true
    }

  /** Returns whether a node of [packageName] with [viewId] is a system navigation bar button. */
  @JvmStatic
  fun isNavigationBarButton(packageName: CharSequence?, viewId: String?): Boolean {
    if (packageName?.toString() != SYSTEM_UI_PACKAGE || viewId == null) {
      return false
    }
    return viewId.substringAfter(":id/", missingDelimiterValue = "") in NAVIGATION_BAR_BUTTON_IDS
  }
}
