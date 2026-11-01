package com.google.android.accessibility.talkback.gesture

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.MagnificationConfig
import android.os.Build
import android.provider.Settings
import com.google.android.libraries.accessibility.utils.log.LogUtils

/**
 * Turns Android's screen magnification on or off, at the scale set in Android's magnification
 * settings. SandFly's magnification feedback says the change.
 */
object MagnificationToggle {

  private const val TAG = "MagnificationToggle"

  // Android's magnification scale, which its own magnification shortcuts use.
  private const val SCALE_SETTING = "accessibility_display_magnification_scale"
  private const val DEFAULT_SCALE = 2f
  private const val MIN_SCALE = 1.1f
  private const val MAX_SCALE = 8f

  @JvmStatic
  fun toggle(service: AccessibilityService): Boolean {
    val controller = service.magnificationController
    return try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val config = controller.magnificationConfig
        val on =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            config?.isActivated == true
          } else {
            (config?.scale ?: 1f) > 1f
          }
        if (on) {
          controller.resetCurrentMagnification(/* animate= */ true)
        } else {
          // The default mode is the one chosen in Android's magnification settings.
          controller.setMagnificationConfig(
            MagnificationConfig.Builder().setScale(scale(service)).build(),
            /* animate= */ true,
          )
        }
      } else if (controller.scale > 1f) {
        controller.reset(/* animate= */ true)
      } else {
        controller.setScale(scale(service), /* animate= */ true)
      }
    } catch (e: RuntimeException) {
      LogUtils.w(TAG, "Cannot turn magnification on or off: %s", e)
      false
    }
  }

  private fun scale(service: AccessibilityService): Float =
    Settings.Secure.getFloat(service.contentResolver, SCALE_SETTING, DEFAULT_SCALE)
      .coerceIn(MIN_SCALE, MAX_SCALE)
}
