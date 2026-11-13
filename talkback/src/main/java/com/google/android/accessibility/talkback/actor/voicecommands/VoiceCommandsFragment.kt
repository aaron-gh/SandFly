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

package com.google.android.accessibility.talkback.actor.voicecommands

import android.text.InputType
import androidx.preference.PreferenceGroup
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.commands.Commands
import com.google.android.accessibility.talkback.notifications.NotificationCodeFragment

/**
 * Voice commands: whether the AI service turns commands into steps, and the voice commands the user
 * made, each a phrase that runs a command.
 */
class VoiceCommandsFragment : NotificationCodeFragment(R.string.title_pref_voice_commands) {

  override fun build(screen: PreferenceGroup) {
    val aiKey = getString(R.string.pref_voice_commands_ai_key)
    screen.switch(getString(R.string.title_pref_voice_commands_ai), prefs.getBoolean(aiKey, false)) {
      prefs.edit().putBoolean(aiKey, it).apply()
    }
    val custom = screen.category(R.string.voice_commands_custom)
    custom.action(getString(R.string.voice_command_add)) { add() }
    val context = requireContext()
    for (entry in CustomVoiceCommands.read(prefs)) {
      val label = Commands.find(context, entry.value)?.label ?: entry.value
      custom.action(entry.phrase, label) { showActions(entry) }
    }
  }

  private fun add() {
    askPhrase(null) { phrase ->
      chooseCommand { value ->
        save(CustomVoiceCommands.read(prefs) + CustomVoiceCommands.Entry(CustomVoiceCommands.newId(), phrase, value))
      }
    }
  }

  private fun showActions(entry: CustomVoiceCommands.Entry) {
    val actions =
      listOf<CharSequence>(
        getString(R.string.voice_command_change_phrase),
        getString(R.string.voice_command_change_command),
        getString(R.string.menu_delete),
      )
    choose(entry.phrase, actions) { which ->
      when (which) {
        0 -> askPhrase(entry) { phrase -> replace(entry.copy(phrase = phrase)) }
        1 -> chooseCommand { value -> replace(entry.copy(value = value)) }
        else ->
          confirm(getString(R.string.voice_command_delete_confirm, entry.phrase), R.string.menu_delete) {
            save(CustomVoiceCommands.read(prefs).filter { it.id != entry.id })
          }
      }
    }
  }

  /** Asks for the phrase of [old], or of a new command, and gives it to [then]. */
  private fun askPhrase(old: CustomVoiceCommands.Entry?, then: (String) -> Unit) {
    val title = if (old == null) getString(R.string.voice_command_add) else getString(R.string.voice_command_change_phrase)
    askText(
      title,
      text = old?.phrase.orEmpty(),
      hint = getString(R.string.voice_command_phrase),
      inputType = InputType.TYPE_CLASS_TEXT,
    ) { text, _, _ ->
      val phrase = text.trim().take(CustomVoiceCommands.MAX_PHRASE)
      val others = CustomVoiceCommands.read(prefs).filter { it.id != old?.id }
      val locale = resources.configuration.locales[0]
      when {
        VoiceGrammar.normalize(phrase, locale).isEmpty() -> ""
        CustomVoiceCommands.find(others, phrase, locale) != null ->
          getString(R.string.voice_command_phrase_exists, phrase)
        else -> {
          then(phrase)
          null
        }
      }
    }
  }

  /** Asks for a command, first its category, and gives its value to [then]. */
  private fun chooseCommand(then: (String) -> Unit) {
    val context = requireContext()
    val own = getString(R.string.shortcut_value_voice_commands)
    val categories =
      Commands.categories(context)
        .map { category -> Commands.Category(category.titleRes, category.commands.filter { it.value != own }) }
        .filter { it.commands.isNotEmpty() }
    choose(getString(R.string.app_gestures_choose_command), categories.map { getString(it.titleRes) }) { group ->
      val commands = categories[group].commands
      choose(getString(categories[group].titleRes), commands.map { it.label }) { then(commands[it].value) }
    }
  }

  private fun replace(entry: CustomVoiceCommands.Entry) {
    save(CustomVoiceCommands.read(prefs).map { if (it.id == entry.id) entry else it })
  }

  private fun save(entries: List<CustomVoiceCommands.Entry>) {
    CustomVoiceCommands.write(prefs, entries)
    rebuild()
  }
}
