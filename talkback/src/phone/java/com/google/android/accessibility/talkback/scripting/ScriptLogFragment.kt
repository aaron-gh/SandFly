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

package com.google.android.accessibility.talkback.scripting

import android.text.format.DateFormat
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.google.android.accessibility.talkback.R
import java.util.Date

class ScriptLogFragment : ScriptScreenFragment() {

  public override fun getTitle(): CharSequence = getText(R.string.script_log)

  override fun state(): Any = Unit

  override fun build(screen: PreferenceScreen) {
    val entries = ScriptLog.entries(scriptId)
    screen.action(getString(R.string.script_log_copy)) {
      val text = entries.asReversed().joinToString("\n") { "${describe(it)}: ${it.message}" }
      if (requireContext().copyToClipboard(getString(R.string.script_log), text)) {
        toast(R.string.script_log_copied)
      }
    }
    screen.action(getString(R.string.script_log_clear)) {
      ScriptLog.clear(scriptId)
      rebuild()
    }
    if (entries.isEmpty()) {
      screen.info(getString(R.string.script_log_empty))
    }
    entries.forEach { screen.item(Preference(screen.context), it.message, describe(it)) }
  }

  private fun describe(entry: ScriptLog.Entry): String =
    getString(
      R.string.script_log_entry,
      DateFormat.getTimeFormat(requireContext()).format(Date(entry.timeMillis)),
      entry.level.name.lowercase(),
    )
}
