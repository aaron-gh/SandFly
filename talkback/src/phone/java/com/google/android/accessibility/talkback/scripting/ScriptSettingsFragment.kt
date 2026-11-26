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

import android.os.Bundle
import android.text.InputType
import androidx.appcompat.app.AlertDialog
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.commands.Commands
import com.google.android.accessibility.talkback.preference.base.GesturePreferenceFragmentCompat
import org.json.JSONObject

class ScriptSettingsFragment : ScriptScreenFragment() {

  public override fun getTitle(): CharSequence = getText(R.string.script_settings_category)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // The command list of a command setting gives the command chosen when it closes.
    parentFragmentManager.setFragmentResultListener(commandResultKey(), this) { _, result ->
      val key = result.getString(GesturePreferenceFragmentCompat.RESULT_TAG)
      val command = result.getString(GesturePreferenceFragmentCompat.RESULT_COMMAND)
      val declared =
        store.find(scriptId)?.manifest?.settings.orEmpty().any {
          it.key == key && it.type == ScriptSetting.Type.COMMAND
        }
      if (key != null && command != null && declared) {
        store.setSetting(scriptId, key, jsonOf(command))
        if (isAdded) rebuild()
      }
    }
  }

  override fun state(): Any? =
    store.find(scriptId)?.let { script ->
      listOf(
        script.manifest,
        script.enabled,
        script.manifest.settings.map { store.settingOptions(script, it) },
        script.manifest.settings.map { store.settingTitle(script, it) },
        script.manifest.settings.map { store.settingSummary(script, it) },
      )
    }

  override fun build(screen: PreferenceScreen) {
    val script = orClose(store.find(scriptId)) ?: return
    script.manifest.settings.forEach { screen.addSetting(script, it) }
  }

  private fun PreferenceGroup.addSetting(script: InstalledScript, setting: ScriptSetting) {
    val current = parseJson(store.settingJson(script, setting.key))
    val save = { json: String -> store.setSetting(scriptId, setting.key, json) }
    val title = store.settingTitle(script, setting)
    val summary = store.settingSummary(script, setting)
    val preference =
      when (setting.type) {
        ScriptSetting.Type.SWITCH ->
          switch(title, summary, current == true) { save(it.toString()) }
        ScriptSetting.Type.BUTTON ->
          action(title, summary) {
            if (script.enabled) store.pressButton(scriptId, setting.key)
            else toast(R.string.script_button_off)
          }
        ScriptSetting.Type.LIST ->
          listSetting(title, store.settingOptions(script, setting), current.toString(), save)
        ScriptSetting.Type.TEXT,
        ScriptSetting.Type.NUMBER -> textSetting(setting, title, current, save)
        ScriptSetting.Type.COMMAND ->
          commandSetting(
            setting.key,
            title,
            store.settingOptions(script, setting),
            if (current == JSONObject.NULL) "" else current.toString(),
          )
      }
    preference.key = "$KEY_PREFIX${setting.key}"
  }

  /**
   * A row that shows the command of a command setting and opens the list of commands used for
   * gestures, with the script's own [options] first.
   */
  private fun PreferenceGroup.commandSetting(
    key: String,
    title: String,
    options: List<Pair<String, String>>,
    current: String,
  ): Preference =
    item(Preference(context), title, commandLabel(options, current)) {
      fragment = GesturePreferenceFragmentCompat::class.java.name
      extras.putAll(
        GesturePreferenceFragmentCompat.commandSettingArguments(
          title,
          current,
          commandResultKey(),
          key,
          options.map { it.second }.toTypedArray(),
          options.map { it.first }.toTypedArray(),
        )
      )
    }

  /**
   * The key of the fragment result that gets the command chosen for a setting of this script, so
   * that the settings of another script, opened over these, can't take it.
   */
  private fun commandResultKey(): String = "$COMMAND_RESULT:$scriptId"

  /** The name of [value] in a command setting: an option's label, Unassigned, or a command's. */
  private fun commandLabel(options: List<Pair<String, String>>, value: String): String =
    options.firstOrNull { it.first == value }?.second
      ?: if (value.isEmpty()) getString(R.string.shortcut_unassigned)
      else Commands.label(requireContext(), value) ?: value

  private fun PreferenceGroup.textSetting(
    setting: ScriptSetting,
    title: String,
    current: Any,
    save: (String) -> Unit,
  ): Preference {
    val number = setting.type == ScriptSetting.Type.NUMBER
    return item(EditTextPreference(context), title) {
      text = if (current == JSONObject.NULL) "" else current.toString()
      dialogTitle = title
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
    title: String,
    options: List<Pair<String, String>>,
    current: String,
    save: (String) -> Unit,
  ): Preference {
    val labelOf = { value: String -> options.firstOrNull { it.first == value }?.second ?: value }
    lateinit var preference: Preference
    preference =
      action(title, labelOf(current)) {
        chooseOption(title, options, current) { value ->
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
    const val COMMAND_RESULT = "script_command_setting"
    const val NUMBER_INPUT =
      InputType.TYPE_CLASS_NUMBER or
        InputType.TYPE_NUMBER_FLAG_DECIMAL or
        InputType.TYPE_NUMBER_FLAG_SIGNED
  }
}
