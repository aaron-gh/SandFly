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
import androidx.appcompat.app.AlertDialog
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import com.google.android.accessibility.talkback.R
import org.json.JSONObject

class ScriptSettingsFragment : ScriptScreenFragment() {

  public override fun getTitle(): CharSequence = getText(R.string.script_settings_category)

  override fun state(): Any? =
    store.find(scriptId)?.let { script ->
      listOf(
        script.manifest,
        script.enabled,
        script.manifest.settings.map { store.settingOptions(script, it) },
      )
    }

  override fun build(screen: PreferenceScreen) {
    val script = orClose(store.find(scriptId)) ?: return
    script.manifest.settings.forEach { screen.addSetting(script, it) }
  }

  private fun PreferenceGroup.addSetting(script: InstalledScript, setting: ScriptSetting) {
    val current = parseJson(store.settingJson(script, setting.key))
    val save = { json: String -> store.setSetting(scriptId, setting.key, json) }
    val preference =
      when (setting.type) {
        ScriptSetting.Type.SWITCH ->
          switch(setting.title, setting.summary, current == true) { save(it.toString()) }
        ScriptSetting.Type.BUTTON ->
          action(setting.title, setting.summary) {
            if (script.enabled) store.pressButton(scriptId, setting.key)
            else toast(R.string.script_button_off)
          }
        ScriptSetting.Type.LIST ->
          listSetting(setting, store.settingOptions(script, setting), current.toString(), save)
        ScriptSetting.Type.TEXT,
        ScriptSetting.Type.NUMBER -> textSetting(setting, current, save)
      }
    preference.key = "$KEY_PREFIX${setting.key}"
  }

  private fun PreferenceGroup.textSetting(
    setting: ScriptSetting,
    current: Any,
    save: (String) -> Unit,
  ): Preference {
    val number = setting.type == ScriptSetting.Type.NUMBER
    return item(EditTextPreference(context), setting.title) {
      text = if (current == JSONObject.NULL) "" else current.toString()
      dialogTitle = setting.title
      summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
      if (number) setOnBindEditTextListener { it.inputType = NUMBER_INPUT }
      setOnPreferenceChangeListener { _, value ->
        val text = value as String
        val json =
          if (number) text.toDoubleOrNull()?.takeIf { it.isFinite() }?.let(::jsonOf)
          else jsonOf(text)
        json?.let(save)
        json != null
      }
    }
  }

  private fun PreferenceGroup.listSetting(
    setting: ScriptSetting,
    options: List<Pair<String, String>>,
    current: String,
    save: (String) -> Unit,
  ): Preference {
    val labelOf = { value: String -> options.firstOrNull { it.first == value }?.second ?: value }
    lateinit var preference: Preference
    preference =
      action(setting.title, labelOf(current)) {
        chooseOption(setting.title, options, current) { value ->
          save(jsonOf(value))
          preference.summary = labelOf(value)
        }
      }
    return preference
  }

  private fun chooseOption(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    choose: (String) -> Unit,
  ) {
    lateinit var shown: AlertDialog
    val choices =
      ChoiceList(requireContext(), options, selected) {
        choose(it)
        shown.dismiss()
      }
    shown = dialog(title) { setView(choices.view) }
  }

  private companion object {
    const val KEY_PREFIX = "setting_"
    const val NUMBER_INPUT =
      InputType.TYPE_CLASS_NUMBER or
        InputType.TYPE_NUMBER_FLAG_DECIMAL or
        InputType.TYPE_NUMBER_FLAG_SIGNED
  }
}
