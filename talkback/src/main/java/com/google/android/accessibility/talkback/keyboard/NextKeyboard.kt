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

package com.google.android.accessibility.talkback.keyboard

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.os.Build
import android.view.inputmethod.InputMethodManager
import androidx.annotation.VisibleForTesting
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.KeyboardUtils

/**
 * Switch keyboard: switches to the enabled input method after the current one, with the soft
 * keyboard controller of Android 11 and later. Android shows its input method picker only to the
 * app in focus and the current input method, so there is no fallback for earlier versions.
 */
object NextKeyboard {
  /** Receives what to say. */
  fun interface Announcer {
    fun announce(text: CharSequence)
  }

  /**
   * Switches to the next enabled input method and announces its name, or announces why it didn't.
   * Returns whether the input method changed.
   */
  @JvmStatic
  fun switchKeyboard(service: AccessibilityService, announcer: Announcer): Boolean {
    val enabled =
      service.getSystemService(InputMethodManager::class.java)?.enabledInputMethodList.orEmpty()
    val current =
      KeyboardUtils.getCurrentInputMethod(service)?.let { ComponentName.unflattenFromString(it) }
    val index = nextIndex(enabled.map { it.component }, current)
    if (index < 0) {
      announcer.announce(service.getString(R.string.switch_keyboard_none))
      return false
    }
    val next = enabled[index]
    val switched =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
        service.softKeyboardController.switchToInputMethod(next.id)
    if (!switched) {
      announcer.announce(service.getString(R.string.switch_keyboard_failed))
      return false
    }
    announcer.announce(next.loadLabel(service.packageManager))
    return true
  }

  /**
   * The index in [enabled] of the input method after [current], going round to the first, or the
   * first when [current] isn't in it. -1 when there is no other input method.
   */
  @VisibleForTesting
  @JvmStatic
  fun <T> nextIndex(enabled: List<T>, current: T?): Int {
    val at = enabled.indexOf(current)
    return when {
      enabled.isEmpty() -> -1
      at < 0 -> 0
      enabled.size == 1 -> -1
      else -> (at + 1) % enabled.size
    }
  }
}
