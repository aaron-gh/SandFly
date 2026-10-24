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

package com.google.android.accessibility.talkback.recognition

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.gestures.GestureManifold
import java.lang.ref.WeakReference

/** Connects the Live recognition reading control to [LiveRecognitionActivity]. Main thread only. */
object LiveRecognition {
  private var activity = WeakReference<LiveRecognitionActivity>(null)
  private var selected = RecognitionMode.SCENES

  // Uses the rear camera only.
  @SuppressLint("UnsupportedChromeOsCameraSystemFeature")
  @JvmStatic fun isSupported(context: Context): Boolean =
    !FormFactorUtils.isAndroidWear() &&
      !FormFactorUtils.isAndroidTv() &&
      context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)

  @JvmStatic fun selection(context: Context): String = context.getString(
    R.string.live_recognition_mode_state,
    context.getString(selected.label),
    context.getString(if (activity.get()?.isEnabled(selected) == true) R.string.value_on else R.string.value_off)
  )

  @JvmStatic fun step(context: Context, forward: Boolean): String {
    selected = selected.step(forward)
    return selection(context)
  }

  @JvmStatic fun toggle(context: Context) {
    val current = activity.get()
    if (current != null) {
      if (current.canUseRotor()) current.toggle(selected)
    } else {
      context.startActivity(Intent(context, LiveRecognitionActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .putExtra(LiveRecognitionActivity.EXTRA_MODE, selected.name))
    }
  }

  @JvmStatic fun stop() { activity.get()?.stopAndFinish() }

  @JvmStatic fun onGesture(gestureId: Int): Boolean {
    val current = activity.get() ?: return false
    if (!current.canUseRotor()) return false
    return when (gestureId) {
      AccessibilityService.GESTURE_2_FINGER_SINGLE_TAP -> { current.skipResult(); true }
      GestureManifold.GESTURE_2_FINGER_SCRUB -> { current.stopAndFinish(); true }
      else -> false
    }
  }

  internal fun attach(value: LiveRecognitionActivity) { activity = WeakReference(value) }
  internal fun detach(value: LiveRecognitionActivity) {
    if (activity.get() === value) activity.clear()
  }
}
