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
import android.widget.EditText
import androidx.preference.PreferenceScreen
import com.google.android.accessibility.talkback.R

class ScriptCommandFragment : ScriptScreenFragment() {
  private val commandId: String by lazy { requireArguments().getString(ARG_COMMAND).orEmpty() }

  public override fun getTitle(): CharSequence =
    command()?.title ?: getText(R.string.script_commands_category)

  override fun state(): Any? =
    command()?.let { command ->
      command to store.bindingsOf(scriptId, command).map { it.key to isOn(it) }
    }

  override fun build(screen: PreferenceScreen) {
    val command = orClose(command()) ?: return
    screen.info(getString(R.string.script_commands_summary))
    for (binding in store.bindingsOf(scriptId, command)) {
      val on = isOn(binding)
      val state = getString(if (on) R.string.script_state_on else R.string.script_state_off)
      screen.action(requireContext().bindingText(binding), state) { manage(command, binding, on) }
    }
    screen.action(
      getString(R.string.script_binding_perform),
      getString(R.string.script_binding_perform_summary),
    ) {
      capture(command)
    }
    screen.action(getString(R.string.script_binding_choose_gesture)) { chooseGesture() }
    screen.action(
      getString(R.string.script_binding_type_keys),
      getString(R.string.script_binding_type_keys_summary),
    ) {
      typeKeys()
    }
    if (store.hasChangedDefaults(scriptId, command)) {
      screen.action(getString(R.string.script_binding_restore)) {
        store.restoreDefaults(scriptId, command)
        toast(R.string.script_binding_restored)
      }
    }
  }

  private fun manage(command: ScriptCommand, binding: CommandBinding, on: Boolean) {
    val toggle = if (on) R.string.script_binding_turn_off else R.string.script_binding_turn_on
    val choices = listOf(getString(toggle), getString(R.string.script_remove))
    choose(requireContext().bindingText(binding), choices) {
      if (it == 0) store.setBindingOn(scriptId, commandId, binding, !on)
      else store.setBinding(scriptId, command, binding, present = false)
    }
  }

  private fun capture(command: ScriptCommand) {
    val waiting =
      dialog(getString(R.string.script_binding_perform)) {
        setMessage(R.string.script_binding_perform_message)
        setOnDismissListener { Scripts.captureNext(null) }
      }
    Scripts.captureNext { key ->
      view?.post {
        waiting.dismiss()
        CommandBinding.fromKey(key)?.let { binding ->
          val text = requireContext().bindingText(binding)
          val title = getString(R.string.script_binding_assign_title, text, command.title)
          confirm(title, null, R.string.script_binding_assign) { add(binding) }
        }
      }
    }
  }

  private fun chooseGesture() {
    val names = ScriptInput.gestureNames
    choose(
      getString(R.string.script_binding_choose_gesture),
      names.map { ScriptInput.gestureTitle(requireContext(), it) },
    ) {
      add(CommandBinding.Gesture(names[it]))
    }
  }

  private fun typeKeys() {
    val field =
      EditText(requireContext()).apply {
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        hint = getString(R.string.script_binding_type_keys_summary)
      }
    dialog(getString(R.string.script_binding_type_keys)) {
      setView(field)
      setPositiveButton(R.string.script_binding_assign) { _, _ ->
        runCatching { ScriptInput.parseKeys(field.text.toString()) }
          .onSuccess { add(CommandBinding.KeyCombo(it)) }
          .onFailure { toast(getString(R.string.script_binding_bad_keys, it.message.orEmpty())) }
      }
    }
  }

  private fun add(binding: CommandBinding) {
    command()?.let { store.setBinding(scriptId, it, binding, present = true) }
  }

  private fun isOn(binding: CommandBinding) = store.isBindingOn(scriptId, commandId, binding)

  private fun command(): ScriptCommand? =
    store.find(scriptId)?.manifest?.commands?.firstOrNull { it.id == commandId }

  companion object {
    const val ARG_COMMAND = "command_id"
  }
}
