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

import android.content.Context
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.FeatureSupport
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * Recognize swipes before lifting: a one-finger swipe runs once it has moved the gesture confirm
 * distance, with the finger still down. Back-and-forth and angle gestures start the same way, so
 * they are off and hidden while it is on. Only SandFly's own gesture detection does this.
 */
object SwipesBeforeLift {

  /** Returns whether swipes run before the finger lifts. */
  @JvmStatic
  fun isOn(context: Context): Boolean {
    if (!FeatureSupport.supportGestureDetection() || FormFactorUtils.isAndroidWear()) {
      return false
    }
    return SharedPreferencesUtils.getSharedPreferences(context)
      .getBoolean(
        context.getString(R.string.pref_swipes_before_lift_key),
        context.resources.getBoolean(R.bool.pref_swipes_before_lift_default),
      )
  }

  /** Returns whether the Gestures screen category with [key] is hidden. */
  @JvmStatic
  fun hidesCategory(context: Context, key: String?): Boolean =
    key != null && key in categoryKeys(context) && isOn(context)

  /** Keys of the back-and-forth and angle categories of the Gestures screen. */
  @JvmStatic
  fun categoryKeys(context: Context): Set<String> =
    setOf(
      context.getString(R.string.pref_category_1finger_back_forth_shortcuts_key),
      context.getString(R.string.pref_category_1finger_angle_shortcuts_key),
    )
}
