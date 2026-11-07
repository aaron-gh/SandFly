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
  /** Receives what to say once color inversion has changed. */
  fun interface Announcer {
    fun announce(text: CharSequence)
  }

  /**
   * Returns whether color inversion changed or a change started, and false when settings opened
   * or nothing changed. [announcer] gets what to say once the setting has changed, which is later
   * when SandFly first grants itself the permission through Shizuku.
   */
  @JvmStatic
  fun toggle(context: Context, announcer: Announcer): Boolean {
    val controls = QuickControls(context)
    val on = controls.isOn(QuickControl.COLOR_INVERSION) != true
    val text =
      context.getString(if (on) R.string.color_inversion_turned_on else R.string.color_inversion_turned_off)
    val change =
      controls.setSwitch(
        QuickControl.COLOR_INVERSION,
        on,
        onDone = { announcer.announce(text) },
        onFailed = {},
      )
    return when (change) {
      QuickControls.Change.DONE -> {
        announcer.announce(text)
        true
      }
      QuickControls.Change.STARTED -> true
      QuickControls.Change.OPENED,
      QuickControls.Change.FAILED -> false
    }
  }
}
