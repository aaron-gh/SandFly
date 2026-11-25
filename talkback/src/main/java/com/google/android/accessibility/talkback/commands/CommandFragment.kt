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

package com.google.android.accessibility.talkback.commands

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.text.InputType
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.preference.EditTextPreference
import com.google.android.accessibility.material.preference.AccessibilitySuitePreference
import com.google.android.accessibility.material.preference.AccessibilitySuitePreferenceCategory
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.braillecommands.BrailleCellAssigner
import com.google.android.accessibility.talkback.braillecommands.BrailleCommands
import com.google.android.accessibility.talkback.keyboard.ArrowKeyNav
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.preference.PreferencesActivityUtils
import com.google.android.accessibility.talkback.preference.base.KeyboardShortcutDialogPreference
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment
import com.google.android.accessibility.talkback.scripting.Scripts
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * One command: Run command, its item in the SandFly menu, and the gestures, arrow keys and keyboard
 * shortcut that run it, to change or add to.
 */
class CommandFragment : TalkbackBaseFragment() {

  private val prefs by lazy { SharedPreferencesUtils.getSharedPreferences(requireContext()) }

  private val command: Commands.Command? by lazy {
    Commands.find(requireContext(), arguments?.getString(ARG_COMMAND))
  }

  override fun getTitle(): CharSequence = command?.label ?: getString(R.string.title_pref_commands)

  /** The gestures, then the arrow keys of arrow key navigation: each can run one command. */
  private class Section(
    val name: String,
    @StringRes val title: Int,
    @StringRes val none: Int,
    @StringRes val add: Int,
    @StringRes val choose: Int,
    @StringRes val remove: Int,
    val slots: List<CommandBindings.GestureSlot>,
  )

  private var sections: List<Section> = emptyList()

  private val brailleCells by lazy { BrailleCellAssigner(this) { fill() } }

  /** The dialog shown while Perform gesture waits for the gesture. */
  private var waiting: AlertDialog? = null

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    preferenceManager.setStorageDeviceProtected()
    val gestures = readGestures(this)
    sections =
      listOfNotNull(
        Section(
          GESTURES,
          R.string.command_gestures,
          R.string.command_no_gestures,
          R.string.command_add_gesture,
          R.string.command_choose_gesture,
          R.string.command_remove_gesture,
          gestures,
        ),
        // Keymaps whose arrow keys move SandFly focus, such as NVDA's, have no arrow key navigation.
        Section(
          "arrow_keys",
          R.string.keycombo_menu_category_arrow_key_nav,
          R.string.command_no_arrow_keys,
          R.string.command_add_arrow_keys,
          R.string.command_choose_arrow_keys,
          R.string.command_remove_arrow_keys,
          CommandBindings.arrowKeys(requireContext()),
        )
          .takeIf { ArrowKeyNav.isAvailable(requireContext()) },
      )
    preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
  }

  override fun onResume() {
    super.onResume()
    fill()
  }

  override fun onPause() {
    // Stops waiting for a gesture, which would otherwise be taken from whatever is shown next.
    waiting?.dismiss()
    super.onPause()
  }

  private fun fill() {
    val context = requireContext()
    val screen = preferenceScreen
    screen.removeAll()
    val command = command ?: return

    addRun(command)
    addSandflyMenu(command)
    ButtonCommands.kindOf(context, command.value)?.let { addButtonWords(it) }
    for (section in sections) {
      addSection(section, command)
    }
    addBrailleCells(command)

    val manager = CommandBindings.keyComboManager(context)
    val model = manager.keyComboModel ?: return
    val keyboardCategory =
      AccessibilitySuitePreferenceCategory(context).apply {
        key = "pref_command_keyboard"
        title = getString(R.string.command_keyboard_shortcut)
        isIconSpaceReserved = false
      }
    screen.addPreference(keyboardCategory)
    val key = CommandBindings.keyboardKey(context, model, command)
    keyboardCategory.addPreference(
      KeyboardShortcutDialogPreference(context, key, command.label).apply {
        isIconSpaceReserved = false
        // Recording a shortcut needs the screen reader's key events.
        isEnabled = TalkBackService.isServiceActive()
      }
    )
  }

  /** Adds [section]: what of it runs [command], or a line saying nothing does, then Add. */
  private fun addSection(section: Section, command: Commands.Command) {
    val context = requireContext()
    val category =
      AccessibilitySuitePreferenceCategory(context).apply {
        key = "pref_command_${section.name}"
        title = getString(section.title)
        isIconSpaceReserved = false
      }
    preferenceScreen.addPreference(category)
    val assigned = section.slots.filter { it.value(context, prefs) == command.value }
    if (assigned.isEmpty()) {
      category.addPreference(
        AccessibilitySuitePreference(context).apply {
          key = "pref_command_no_${section.name}"
          isPersistent = false
          isIconSpaceReserved = false
          isSelectable = false
          title = getString(section.none)
        }
      )
    }
    for (slot in assigned) {
      category.addPreference(
        AccessibilitySuitePreference(context).apply {
          key = "pref_command_slot_${slot.key}"
          isPersistent = false
          isIconSpaceReserved = false
          title = slot.name
          setOnPreferenceClickListener {
            confirmRemove(slot, section.remove)
            true
          }
        }
      )
    }
    category.addPreference(
      AccessibilitySuitePreference(context).apply {
        key = "pref_command_add_${section.name}"
        isPersistent = false
        isIconSpaceReserved = false
        title = getString(section.add)
        setOnPreferenceClickListener {
          chooseGesture(command, section.slots, section.choose)
          true
        }
      }
    )
    if (section.name == GESTURES) {
      category.addPreference(
        AccessibilitySuitePreference(context).apply {
          key = "pref_command_perform_gesture"
          isPersistent = false
          isIconSpaceReserved = false
          title = getString(R.string.command_perform_gesture)
          // The screen reader recognizes the gesture.
          isEnabled = TalkBackService.isServiceActive()
          setOnPreferenceClickListener {
            performGesture(command, section.slots)
            true
          }
        }
      )
    }
  }

  /** Adds Run command, which leaves SandFly settings and runs [command] in the app shown before. */
  private fun addRun(command: Commands.Command) {
    preferenceScreen.addPreference(
      AccessibilitySuitePreference(requireContext()).apply {
        key = "pref_command_run"
        isPersistent = false
        isIconSpaceReserved = false
        title = getString(R.string.command_run)
        isEnabled = TalkBackService.isServiceActive()
        setOnPreferenceClickListener {
          runCommand(command)
          true
        }
      }
    )
  }

  /**
   * Leaves SandFly settings, bringing back the app shown before, and runs [command] there once that
   * app is in front.
   */
  private fun runCommand(command: Commands.Command) {
    val value = command.value
    requireActivity().moveTaskToBack(true)
    Handler(Looper.getMainLooper())
      .postDelayed({ TalkBackService.getInstance()?.performCommand(value) }, RUN_DELAY_MS)
  }

  /** Adds Add to SandFly menu, or Remove from SandFly menu while [command] is in it. */
  private fun addSandflyMenu(command: Commands.Command) {
    val context = requireContext()
    preferenceScreen.addPreference(
      AccessibilitySuitePreference(context).apply {
        key = "pref_command_sandfly_menu"
        isPersistent = false
        isIconSpaceReserved = false
        title = sandflyMenuTitle(CommandMenus.isInSandfly(context, prefs, command.value))
        setOnPreferenceClickListener { row ->
          val add = !CommandMenus.isInSandfly(context, prefs, command.value)
          if (add) {
            CommandMenus.addToSandfly(context, prefs, command.value)
          } else {
            CommandMenus.removeFromSandfly(context, prefs, command.value)
          }
          row.title = sandflyMenuTitle(add)
          PreferencesActivityUtils.announceText(
            getString(
              if (add) R.string.command_added_to_sandfly_menu
              else R.string.command_removed_from_sandfly_menu,
              command.label,
            ),
            context,
          )
          true
        }
      }
    )
  }

  private fun sandflyMenuTitle(inMenu: Boolean): String =
    getString(
      if (inMenu) R.string.command_remove_from_sandfly_menu
      else R.string.command_add_to_sandfly_menu
    )

  /** Waits for a gesture, then asks to assign it to [command]. */
  private fun performGesture(
    command: Commands.Command,
    gestures: List<CommandBindings.GestureSlot>,
  ) {
    waiting?.dismiss()
    val dialog =
      AlertDialog.Builder(requireContext())
        .setTitle(R.string.command_perform_gesture)
        .setMessage(R.string.command_perform_gesture_message)
        .setNegativeButton(android.R.string.cancel, null)
        .create()
    dialog.setOnDismissListener {
      Scripts.captureNext(null)
      if (waiting === dialog) {
        waiting = null
      }
    }
    waiting = dialog
    dialog.show()
    Scripts.captureNext { binding ->
      view?.post { onGesturePerformed(dialog, binding, command, gestures) }
    }
  }

  private fun onGesturePerformed(
    dialog: AlertDialog,
    binding: String,
    command: Commands.Command,
    gestures: List<CommandBindings.GestureSlot>,
  ) {
    // Cancelled, or the screen closed, before the gesture.
    if (!dialog.isShowing || !isAdded) {
      return
    }
    dialog.dismiss()
    val context = requireContext()
    val gesture = CommandBindings.performedGesture(context, gestures, binding)
    if (gesture == null) {
      PreferencesActivityUtils.announceText(
        getString(
          R.string.command_gesture_not_assignable,
          CommandBindings.performedTitle(context, binding),
        ),
        context,
      )
      return
    }
    val current = gesture.value(context, prefs)
    when (current) {
      command.value ->
        PreferencesActivityUtils.announceText(
          getString(R.string.command_gesture_assigned, gesture.name, command.label),
          context,
        )
      getString(R.string.shortcut_value_unassigned) ->
        AlertDialog.Builder(context)
          .setTitle(getString(R.string.script_binding_assign_title, gesture.name, command.label))
          .setPositiveButton(R.string.script_binding_assign) { _, _ ->
            assign(gesture, command.value)
          }
          .setNegativeButton(android.R.string.cancel, null)
          .show()
      else -> confirmReplace(gesture, current, command)
    }
  }

  /** Adds the labels of the buttons that Activate confirm button or cancel button looks for. */
  private fun addButtonWords(kind: ButtonCommands.Kind) {
    val context = requireContext()
    preferenceScreen.addPreference(
      EditTextPreference(context).apply {
        key = "pref_command_button_words"
        isPersistent = false
        isIconSpaceReserved = false
        title = getString(R.string.title_button_command_words)
        dialogTitle = title
        showButtonWords(this, kind)
        setOnBindEditTextListener { field ->
          field.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
          field.filters = arrayOf(InputFilter.LengthFilter(ButtonCommands.MAX_TEXT))
          field.setSelection(field.length())
        }
        setOnPreferenceChangeListener { _, value ->
          ButtonCommands.saveWords(context, prefs, kind, value as String)
          showButtonWords(this, kind)
          false
        }
      }
    )
  }

  /** Shows the saved labels of [kind] in [preference], the built-in ones while none are saved. */
  private fun showButtonWords(preference: EditTextPreference, kind: ButtonCommands.Kind) {
    val text = ButtonCommands.wordsText(requireContext(), prefs, kind)
    preference.text = text
    preference.summary = text.lines().filter { it.isNotBlank() }.joinToString(", ")
  }

  /** Adds the braille cells that run [command] in Default's braille commands, then Add. */
  private fun addBrailleCells(command: Commands.Command) {
    val context = requireContext()
    val category =
      AccessibilitySuitePreferenceCategory(context).apply {
        key = "pref_command_braille_cells"
        title = getString(R.string.command_braille_cells)
        isIconSpaceReserved = false
      }
    preferenceScreen.addPreference(category)
    val cells = BrailleCommands.cellsOf(context, prefs, command.value)
    if (cells.isEmpty()) {
      category.addPreference(
        AccessibilitySuitePreference(context).apply {
          key = "pref_command_no_braille_cells"
          isPersistent = false
          isIconSpaceReserved = false
          isSelectable = false
          title = getString(R.string.command_no_braille_cells)
        }
      )
    }
    for (cell in cells) {
      category.addPreference(
        AccessibilitySuitePreference(context).apply {
          key = "pref_command_braille_cell_$cell"
          isPersistent = false
          isIconSpaceReserved = false
          title = BrailleCellAssigner.cellName(context, cell)
          setOnPreferenceClickListener {
            AlertDialog.Builder(context)
              .setTitle(title)
              .setPositiveButton(R.string.command_remove_braille_cell) { _, _ ->
                BrailleCommands.remove(context, prefs, BrailleCommands.DEFAULT_SET, cell)
                fill()
              }
              .setNegativeButton(android.R.string.cancel, null)
              .show()
            true
          }
        }
      )
    }
    category.addPreference(
      AccessibilitySuitePreference(context).apply {
        key = "pref_command_add_braille_cell"
        isPersistent = false
        isIconSpaceReserved = false
        title = getString(R.string.command_add_braille_cell)
        // The cell is typed on SandFly's braille surface.
        isEnabled = brailleCells.canRecord
        setOnPreferenceClickListener {
          brailleCells.record { cell ->
            brailleCells.assign(BrailleCommands.DEFAULT_SET, cell, command.value)
          }
          true
        }
      }
    )
  }

  private fun confirmRemove(gesture: CommandBindings.GestureSlot, @StringRes remove: Int) {
    AlertDialog.Builder(requireContext())
      .setTitle(gesture.name)
      .setPositiveButton(remove) { _, _ ->
        assign(gesture, getString(R.string.shortcut_value_unassigned))
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun chooseGesture(
    command: Commands.Command,
    gestures: List<CommandBindings.GestureSlot>,
    @StringRes chooseTitle: Int,
  ) {
    val context = requireContext()
    val unassigned = getString(R.string.shortcut_value_unassigned)
    val choices = gestures.filter { it.value(context, prefs) != command.value }
    val names =
      choices.map { gesture ->
        val current = gesture.value(context, prefs)
        if (current == unassigned) {
          gesture.name
        } else {
          "${gesture.name}: ${Commands.label(context, current) ?: current}"
        }
      }
    AlertDialog.Builder(context)
      .setTitle(chooseTitle)
      .setItems(names.toTypedArray()) { _, which ->
        val gesture = choices[which]
        val current = gesture.value(context, prefs)
        if (current == unassigned) {
          assign(gesture, command.value)
        } else {
          confirmReplace(gesture, current, command)
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun confirmReplace(
    gesture: CommandBindings.GestureSlot,
    current: String,
    command: Commands.Command,
  ) {
    val context = requireContext()
    AlertDialog.Builder(context)
      .setMessage(
        getString(
          R.string.command_replace_gesture,
          gesture.name,
          Commands.label(context, current) ?: current,
          command.label,
        )
      )
      .setPositiveButton(R.string.command_replace) { _, _ -> assign(gesture, command.value) }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun assign(gesture: CommandBindings.GestureSlot, value: String) {
    prefs.edit().putString(gesture.key, value).apply()
    fill()
    val context = requireContext()
    PreferencesActivityUtils.announceText(
      getString(
        R.string.command_gesture_assigned,
        gesture.name,
        Commands.label(context, value) ?: getString(R.string.keycombo_unassigned),
      ),
      context,
    )
  }

  companion object {
    const val ARG_COMMAND = "command"

    private const val GESTURES = "gestures"

    /** How long Run command waits for the app shown before to come to the front. */
    private const val RUN_DELAY_MS = 1000L
  }
}
