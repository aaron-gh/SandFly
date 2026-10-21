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

import android.text.InputType
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceScreen
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.widget.KeyboardSafeEditText

/** The places to find scripts that the user has added, and a way to add another. */
open class ScriptSourcesFragment : ScriptScreenFragment() {
  protected open val kind = SourceKind.SCRIPTS
  protected open val sourceScreen: Class<out ScriptSourceFragment> = ScriptSourceFragment::class.java

  public override fun getTitle(): CharSequence = getText(R.string.script_sources)

  override fun state(): Any = ScriptSources.all(requireContext(), kind)

  override fun build(screen: PreferenceScreen) {
    screen.action(getString(R.string.script_source_add)) { askForAddress() }
    val sources = ScriptSources.all(requireContext(), kind)
    if (sources.isEmpty()) screen.info(getString(R.string.script_sources_none))
    sources.forEach { screen.link(it.title, it.where, sourceScreen, it.address) }
  }

  private fun askForAddress() {
    val context = requireContext()
    lateinit var shown: AlertDialog
    val field =
      KeyboardSafeEditText(context).apply {
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        setHint(R.string.script_source_hint)
        isSingleLine = true
        imeOptions = EditorInfo.IME_ACTION_DONE
        setOnEditorActionListener { _, action, _ ->
          (action == EditorInfo.IME_ACTION_DONE).also {
            if (it) {
              shown.dismiss()
              add(text.toString())
            }
          }
        }
      }
    // Lines the field up with the dialog's title.
    val padding = (24 * context.resources.displayMetrics.density).toInt()
    val container =
      FrameLayout(context).apply {
        setPadding(padding, 0, padding, 0)
        addView(field)
      }
    shown =
      dialog(getString(R.string.script_source_add)) {
        setView(container)
        setPositiveButton(R.string.script_source_add_button) { _, _ -> add(field.text.toString()) }
      }
    shown.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    field.requestFocus()
  }

  private fun add(text: String) {
    val source = ScriptSource.parse(text)
    when {
      source == null -> toast(R.string.script_source_invalid)
      !ScriptSources.add(requireContext(), source, kind) -> toast(R.string.script_source_exists)
      else -> rebuild()
    }
  }
}
