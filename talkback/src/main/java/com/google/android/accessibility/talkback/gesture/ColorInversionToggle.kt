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

package com.google.android.accessibility.talkback.gesture

import android.content.Context
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.quicksettings.QuickControl
import com.google.android.accessibility.talkback.quicksettings.QuickControls

/**
 * Turns Android's color inversion on or off. It is a secure setting, written with the permission
 * SandFly grants itself through Shizuku; without Shizuku, Android's accessibility settings open.
 */
object ColorInversionToggle {
  /** Returns what to say about the change, or null when settings opened or nothing changed. */
  @JvmStatic
  fun toggle(context: Context): CharSequence? {
    val controls = QuickControls(context)
    val on = controls.isOn(QuickControl.COLOR_INVERSION) != true
    val change = controls.setSwitch(QuickControl.COLOR_INVERSION, on) {}
    return when (change) {
      QuickControls.Change.DONE,
      QuickControls.Change.STARTED ->
        context.getString(if (on) R.string.color_inversion_turned_on else R.string.color_inversion_turned_off)
      QuickControls.Change.OPENED,
      QuickControls.Change.FAILED -> null
    }
  }
}
