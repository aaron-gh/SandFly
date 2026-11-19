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

package com.google.android.accessibility.talkback.actor

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.google.android.accessibility.utils.ScreenOnKeeper
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * A transparent one-pixel window with FLAG_KEEP_SCREEN_ON, shown while
 * [com.google.android.accessibility.utils.KeepScreenOn] keeps the screen on.
 *
 * It takes no touches and no focus. Android leaves windows that take no touches out of the
 * accessibility window list, so no screen reader moves into it; its view is also hidden from
 * accessibility and sends no events. FLAG_KEEP_SCREEN_ON doesn't stop the power key from turning
 * the screen off.
 */
class KeepScreenOnWindow(private val context: Context) : ScreenOnKeeper.Window {
  private val windowManager = context.getSystemService(WindowManager::class.java)
  private var view: View? = null

  override fun setShown(shown: Boolean) {
    if (shown == (view != null)) {
      return
    }
    if (shown) show() else hide()
  }

  private fun show() {
    val manager = windowManager ?: return
    val pixel =
      object : View(context) {
        override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent): Boolean = true

        override fun sendAccessibilityEvent(eventType: Int) {}

        override fun sendAccessibilityEventUnchecked(event: AccessibilityEvent) {}
      }
    pixel.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    val params =
      WindowManager.LayoutParams(
        1,
        1,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
          WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
          WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
          WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
      )
    params.gravity = Gravity.TOP or Gravity.START
    params.title = WINDOW_TITLE
    try {
      manager.addView(pixel, params)
      view = pixel
      LogUtils.d(TAG, "Keeping the screen on")
    } catch (e: RuntimeException) {
      // BadTokenException or IllegalStateException, when the service has no window token.
      LogUtils.w(TAG, "Couldn't add the keep screen on window: %s", e)
    }
  }

  private fun hide() {
    val pixel = view ?: return
    view = null
    try {
      windowManager?.removeViewImmediate(pixel)
      LogUtils.d(TAG, "No longer keeping the screen on")
    } catch (e: IllegalArgumentException) {
      LogUtils.w(TAG, "Keep screen on window was not shown: %s", e)
    }
  }

  private companion object {
    const val TAG = "KeepScreenOnWindow"
    const val WINDOW_TITLE = "SandFly keep screen on"
  }
}
