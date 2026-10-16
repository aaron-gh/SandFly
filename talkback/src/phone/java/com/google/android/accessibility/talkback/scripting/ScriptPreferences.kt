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

import android.R as AndroidR
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment

abstract class ScriptScreenFragment : TalkbackBaseFragment(), ScriptStore.Listener {
  protected val store: ScriptStore by lazy { ScriptStore.get(requireContext()) }
  protected val scriptId: String by lazy { requireArguments().getString(ARG_ID).orEmpty() }
  private var shown: Any? = null

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) = rebuild()

  override fun onStart() {
    super.onStart()
    store.addListener(this)
    rebuild()
  }

  override fun onStop() {
    store.removeListener(this)
    super.onStop()
  }

  override fun onScriptsChanged() {
    if (isAdded && state() != shown) rebuild()
  }

  protected abstract fun state(): Any?

  protected abstract fun build(screen: PreferenceScreen)

  protected fun rebuild() {
    shown = state()
    preferenceScreen = preferenceManager.createPreferenceScreen(requireContext()).also(::build)
  }

  protected fun <T : Any> orClose(value: T?): T? =
    value.also { if (it == null) parentFragmentManager.popBackStack() }

  protected fun toast(text: Int) = toast(getString(text))

  protected fun toast(text: CharSequence) {
    Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show()
  }

  protected fun dialog(title: CharSequence, build: AlertDialog.Builder.() -> Unit): AlertDialog =
    AlertDialog.Builder(requireContext())
      .setTitle(title)
      .apply(build)
      .setNegativeButton(AndroidR.string.cancel, null)
      .show()

  protected fun confirm(
    title: CharSequence,
    message: CharSequence?,
    button: Int,
    onOk: () -> Unit,
  ) =
    dialog(title) {
      setMessage(message)
      setPositiveButton(button) { _, _ -> onOk() }
    }

  protected fun choose(title: CharSequence, items: List<CharSequence>, onPick: (Int) -> Unit) =
    dialog(title) { setItems(items.toTypedArray()) { _, which -> onPick(which) } }

  companion object {
    const val ARG_ID = "script_id"
  }
}

fun <P : Preference> PreferenceGroup.item(
  preference: P,
  title: CharSequence,
  summary: CharSequence? = null,
  setup: P.() -> Unit = {},
): P =
  preference
    .apply {
      isPersistent = false
      layoutResource = R.layout.listitem_2texts
      isSingleLineTitle = false
      this.title = title
      this.summary = summary
      setup()
    }
    .also { addPreference(it) }

fun PreferenceGroup.info(title: CharSequence, summary: CharSequence? = null): Preference =
  item(Preference(context), title, summary) { isSelectable = false }

fun PreferenceGroup.action(
  title: CharSequence,
  summary: CharSequence? = null,
  onClick: () -> Unit,
): Preference =
  item(Preference(context), title, summary) {
    setOnPreferenceClickListener {
      onClick()
      true
    }
  }

fun PreferenceGroup.link(
  title: CharSequence,
  summary: CharSequence?,
  screen: Class<out Fragment>,
  scriptId: String,
  arguments: Bundle.() -> Unit = {},
): Preference =
  item(Preference(context), title, summary) {
    fragment = screen.name
    extras.putString(ScriptScreenFragment.ARG_ID, scriptId)
    extras.arguments()
  }

fun PreferenceGroup.switch(
  title: CharSequence,
  summary: CharSequence?,
  checked: Boolean,
  onChange: (Boolean) -> Unit,
): CheckBoxPreference =
  item(CheckBoxPreference(context), title, summary) {
    isChecked = checked
    setOnPreferenceChangeListener { _, value ->
      onChange(value as Boolean)
      true
    }
  }

fun PreferenceGroup.category(title: CharSequence): PreferenceCategory =
  PreferenceCategory(context)
    .apply {
      this.title = title
      isIconSpaceReserved = false
    }
    .also { addPreference(it) }
