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

package com.google.android.accessibility.talkback.macros

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * Runs the macro of a home screen shortcut from [MacroShortcuts] and finishes at once, with no
 * window. The macro starts after a short delay, on the screen the shortcut was opened from.
 */
class MacroShortcutActivity : Activity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    finish()
    if (savedInstanceState != null || intent?.action != MacroShortcuts.ACTION_RUN) return
    val (macroId, token) =
      try {
        intent.getStringExtra(MacroShortcuts.EXTRA_MACRO) to intent.getStringExtra(MacroShortcuts.EXTRA_TOKEN)
      } catch (e: RuntimeException) {
        // Extras that can't be read.
        return
      }
    val context = applicationContext
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val macro = MacroShortcuts.verified(Macros.read(prefs), macroId, token)
    if (macro == null) {
      // A macro deleted in a way that didn't disable its shortcut, such as a settings import.
      val deleted = macroId != null && MacroShortcuts.isMacroId(macroId) && Macros.find(prefs, macroId) == null
      if (deleted) MacroShortcuts.disable(context, macroId)
      val message = if (deleted) R.string.macro_shortcut_deleted else R.string.macro_shortcut_invalid
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
      return
    }
    if (TalkBackService.getInstance() == null) {
      Toast.makeText(context, R.string.talkback_inactive_title, Toast.LENGTH_SHORT).show()
      return
    }
    val id = macro.id
    Handler(Looper.getMainLooper()).postDelayed({ MacroRunner.run(id, sayNotMet = true) }, RUN_DELAY_MS)
  }

  companion object {
    /** The time for the launcher to be in front again before the macro starts. */
    private const val RUN_DELAY_MS = 500L
  }
}
