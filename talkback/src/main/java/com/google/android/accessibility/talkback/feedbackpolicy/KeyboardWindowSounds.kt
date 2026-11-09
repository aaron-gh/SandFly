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

package com.google.android.accessibility.talkback.feedbackpolicy

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.AccessibilityWindowInfoUtils
import com.google.android.accessibility.utils.Performance.EventId
import com.google.android.accessibility.utils.input.WindowEventInterpreter
import com.google.android.accessibility.utils.input.WindowEventInterpreter.EventInterpretation

/**
 * Plays a sound when the input method window of an on-screen keyboard appears or disappears. The
 * braille keyboard draws in an overlay and plays these sounds itself, so it is skipped here.
 */
class KeyboardWindowSounds(
  private val context: Context,
  private val play: (Int) -> Unit,
  private val active: () -> Boolean,
) : WindowEventInterpreter.WindowEventHandler {
  private val handler = Handler(Looper.getMainLooper())
  private var windowOpen = false
  private var announcedOpen: Boolean? = null
  private val settled = Runnable { onSettled() }

  override fun handle(interpretation: EventInterpretation, eventId: EventId?) {
    windowOpen = interpretation.inputMethod.id != AccessibilityWindowInfoUtils.WINDOW_ID_NONE
    handler.removeCallbacks(settled)
    if (windowOpen != announcedOpen) {
      handler.postDelayed(settled, SETTLE_MS)
    }
  }

  fun shutdown() {
    handler.removeCallbacks(settled)
  }

  private fun onSettled() {
    val first = announcedOpen == null
    announcedOpen = windowOpen
    if (first || !active() || !isInteractive() || ownKeyboardCurrent()) {
      return
    }
    play(if (windowOpen) R.raw.keyboard_opened else R.raw.keyboard_closed)
  }

  private fun isInteractive(): Boolean =
    context.getSystemService(PowerManager::class.java)?.isInteractive != false

  private fun ownKeyboardCurrent(): Boolean =
    Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
      ?.startsWith("${context.packageName}/") == true

  private companion object {
    // Ignores a window that disappears and reappears at once.
    const val SETTLE_MS = 100L
  }
}
