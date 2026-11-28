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
import android.content.SharedPreferences
import android.graphics.Rect
import android.graphics.Region
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.WindowInsets
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.FeatureSupport
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Use Android navigation gestures: touches that start in Android's gesture navigation areas at the
 * bottom, left and right edges of the screen pass through to Android, so that its swipe up for
 * Home, swipe up and hold for Recent apps and swipe in from a side for Back work as without a
 * screen reader. Applies only while Android uses gesture navigation. SandFly's edge and corner
 * gestures start in the same places, so they are off and hidden while it applies.
 */
object AndroidGestureNavigation {
  private const val TAG = "AndroidGestureNavigation"

  // Sizes used when Android reports no gesture areas.
  private const val FALLBACK_SIDE_DP = 24f
  private const val FALLBACK_BOTTOM_DP = 32f

  /** Returns whether this device can pass the gesture navigation areas through to Android. */
  @JvmStatic
  fun isSupported(): Boolean =
    FeatureSupport.supportPassthrough() &&
      !FormFactorUtils.isAndroidWear() &&
      !FormFactorUtils.isAndroidTv()

  /** Returns whether the switch is on, whatever navigation Android uses. */
  @JvmStatic
  fun isSwitchedOn(context: Context): Boolean =
    SharedPreferencesUtils.getSharedPreferences(context)
      .getBoolean(
        context.getString(R.string.pref_android_gesture_navigation_key),
        context.resources.getBoolean(R.bool.pref_android_gesture_navigation_default),
      )

  /** Returns whether Android uses gesture navigation, the only kind that the switch applies to. */
  @JvmStatic
  fun isGestureNavigation(context: Context): Boolean =
    FeatureSupport.isGestureNavigateEnabled(context)

  /** Returns whether the gesture navigation areas pass through to Android now. */
  @JvmStatic
  fun isOn(context: Context): Boolean =
    isSupported() && isSwitchedOn(context) && isGestureNavigation(context)

  /** Returns whether the Gestures screen category with [key] is hidden. */
  @JvmStatic
  fun hidesCategory(context: Context, key: String?): Boolean =
    key != null && key in categoryKeys(context) && isOn(context)

  /** Keys of the Gestures screen categories hidden while the switch applies. */
  @JvmStatic
  fun categoryKeys(context: Context): Set<String> =
    setOf(
      context.getString(R.string.pref_category_edge_shortcuts_key),
      context.getString(R.string.pref_category_corner_shortcuts_key),
    )

  /** Returns whether the gesture with the setting [baseKey] is an edge or corner gesture. */
  @JvmStatic
  fun isEdgeOrCornerGesture(baseKey: String): Boolean =
    baseKey.startsWith("pref_shortcut_edge_") || baseKey.startsWith("pref_shortcut_corner_")

  /**
   * Returns the gesture navigation areas of the display that [context] shows on, in screen
   * coordinates: the bottom area for Home and Recent apps, and the side areas for Back.
   */
  @JvmStatic
  @RequiresApi(Build.VERSION_CODES.R)
  fun region(context: Context): Region {
    val windowManager = context.getSystemService(WindowManager::class.java) ?: return Region()
    val metrics = windowManager.currentWindowMetrics
    val bounds = metrics.bounds
    val insets = metrics.windowInsets
    val gestures = insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemGestures())
    val mandatory = insets.getInsetsIgnoringVisibility(WindowInsets.Type.mandatorySystemGestures())
    var left = gestures.left
    var right = gestures.right
    var bottom = maxOf(gestures.bottom, mandatory.bottom)
    if (left == 0 && right == 0 && bottom == 0) {
      // Android reported no gesture areas.
      val metricsDp = context.resources.displayMetrics
      left = dp(FALLBACK_SIDE_DP, metricsDp)
      right = left
      bottom = dp(FALLBACK_BOTTOM_DP, metricsDp)
      LogUtils.w(TAG, "No gesture navigation insets; using %d px sides, %d px bottom", left, bottom)
    }
    return region(bounds, left, right, bottom)
  }

  /** Returns the strips of [bounds] [left], [right] and [bottom] pixels wide at those edges. */
  @JvmStatic
  fun region(bounds: Rect, left: Int, right: Int, bottom: Int): Region {
    val region = Region()
    if (left > 0) {
      region.op(Rect(bounds.left, bounds.top, bounds.left + left, bounds.bottom), Region.Op.UNION)
    }
    if (right > 0) {
      region.op(Rect(bounds.right - right, bounds.top, bounds.right, bounds.bottom), Region.Op.UNION)
    }
    if (bottom > 0) {
      region.op(
        Rect(bounds.left, bounds.bottom - bottom, bounds.right, bounds.bottom),
        Region.Op.UNION,
      )
    }
    return region
  }

  private fun dp(value: Float, metrics: android.util.DisplayMetrics): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, metrics).toInt()

  /**
   * Sends the gesture navigation areas to [send] while the switch applies, and an empty region
   * otherwise. It sends again when the switch changes and on [update], which the service calls on
   * configuration changes, such as rotation, folding and a change of Android's navigation mode.
   */
  class RegionUpdater(
    private val context: Context,
    private val prefs: SharedPreferences,
    private val send: (Region) -> Unit,
  ) {
    private val handler = Handler(Looper.getMainLooper())
    private val key = context.getString(R.string.pref_android_gesture_navigation_key)
    private var sent: Region? = null
    private val listener =
      SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
        if (changed == key) update()
      }
    // Window metrics can still have the old size while the configuration change is delivered.
    private val updateLater = Runnable { update() }

    fun start() {
      prefs.registerOnSharedPreferenceChangeListener(listener)
      update()
    }

    fun shutdown() {
      prefs.unregisterOnSharedPreferenceChangeListener(listener)
      handler.removeCallbacks(updateLater)
      if (sent?.isEmpty == false) send(Region())
      sent = null
    }

    /** Sends the areas that apply now, now and again shortly after. */
    fun onConfigurationChanged() {
      update()
      handler.removeCallbacks(updateLater)
      handler.postDelayed(updateLater, CONFIGURATION_SETTLE_MS)
    }

    /** Sends the areas that apply now, if they changed. */
    fun update() {
      val region =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && isOn(context)) region(context)
        else Region()
      if (region == sent) return
      sent = region
      send(region)
    }

    private companion object {
      const val CONFIGURATION_SETTLE_MS = 500L
    }
  }
}
