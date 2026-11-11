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

import android.content.Context
import android.text.InputType
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceViewHolder
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.commands.CommandFragment
import com.google.android.accessibility.talkback.commands.Commands
import com.google.android.accessibility.talkback.gesture.OpenAppActions
import com.google.android.accessibility.talkback.notifications.NotificationCodeFragment
import com.google.android.accessibility.talkback.preference.base.announce
import com.google.android.accessibility.talkback.preference.base.focusAfterLayout
import com.google.android.accessibility.talkback.preference.base.positionText
import com.google.android.accessibility.talkback.preference.base.setMoveActions
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Macros: the macros the user made, and the button that makes another. */
class MacrosFragment : NotificationCodeFragment(R.string.title_pref_macros) {

  override fun build(screen: PreferenceGroup) {
    screen.action(getString(R.string.macro_add)) { add() }
    for (macro in Macros.read(prefs)) {
      screen.link(macro.name, null, MacroFragment::class.java) {
        putString(MacroFragment.ARG_MACRO, macro.id)
      }
    }
  }

  private fun add() {
    askText(getString(R.string.macro_add), hint = getString(R.string.menu_name)) { name, _, _ ->
      val trimmed = name.trim()
      if (trimmed.isEmpty()) {
        ""
      } else {
        val macro = Macros.add(prefs, trimmed)
        rebuild()
        onPreferenceTreeClick(
          Preference(requireContext()).apply {
            fragment = MacroFragment::class.java.name
            extras.putString(MacroFragment.ARG_MACRO, macro.id)
          }
        )
        null
      }
    }
  }
}

/**
 * One macro: Run now, its steps in order, each of which can be edited, moved or deleted, and the
 * gestures that run it.
 */
class MacroFragment : NotificationCodeFragment(R.string.title_pref_macros) {

  private val macroId: String by lazy { arguments?.getString(ARG_MACRO).orEmpty() }

  // The title is asked for before the screen is made, when prefs isn't set yet.
  private fun macro(): Macros.Macro? =
    Macros.find(SharedPreferencesUtils.getSharedPreferences(requireContext()), macroId)

  override fun getTitle(): CharSequence = macro()?.name ?: getText(R.string.title_pref_macros)

  private fun steps(): List<Macros.Step> = macro()?.steps.orEmpty()

  private fun save(steps: List<Macros.Step>) {
    val macro = macro() ?: return
    Macros.update(prefs, macro.copy(steps = steps.take(Macros.MAX_STEPS)))
  }

  override fun build(screen: PreferenceGroup) {
    val context = requireContext()
    val macro = macro() ?: return
    screen.action(getString(R.string.macro_run_now)) { runNow() }
    if (macro.steps.size < Macros.MAX_STEPS) {
      screen.action(getString(R.string.macro_add_step)) { addStep() }
    }
    val list = screen.category(R.string.macro_steps)
    if (macro.steps.isEmpty()) {
      list.addPreference(
        Preference(context).apply {
          key = "macro_no_steps"
          isPersistent = false
          isIconSpaceReserved = false
          isSelectable = false
          setTitle(R.string.macro_no_steps)
        }
      )
    }
    // The names of the commands, looked up together.
    val commands = Commands.labels(context, macro.steps.map { (it as? Macros.RunCommand)?.value.orEmpty() })
    macro.steps.forEachIndexed { index, step ->
      val name =
        if (step is Macros.RunCommand) commands[index] ?: getString(R.string.macro_step_unavailable)
        else Macros.describe(context, step)
      list.addPreference(StepRow(context, macro.steps, index, name))
    }
    screen.link(getString(R.string.menu_assign), null, CommandFragment::class.java) {
      putString(CommandFragment.ARG_COMMAND, Macros.commandOf(macro.id))
    }
    screen.action(getString(R.string.menu_rename)) { rename() }
    screen.action(getString(R.string.macro_delete)) { confirmDelete() }
  }

  private fun rowKey(index: Int): String = "macro_step_$index"

  private fun runNow() {
    if (TalkBackService.getInstance() == null) {
      toast(R.string.talkback_inactive_title)
      return
    }
    MacroRunner.run(macroId)
  }

  private fun showActions(steps: List<Macros.Step>, index: Int, name: CharSequence) {
    val actions = ArrayList<Pair<Int, () -> Unit>>()
    actions += R.string.notification_edit to { editStep(index) }
    if (index > 0) actions += R.string.action_move_up to { move(steps, index, -1) }
    if (index < steps.size - 1) actions += R.string.action_move_down to { move(steps, index, 1) }
    actions += R.string.menu_delete to { delete(index, name) }
    choose(name, actions.map { getString(it.first) }) { actions[it].second() }
  }

  private fun move(steps: List<Macros.Step>, index: Int, offset: Int): Boolean {
    val moved = Macros.moved(steps, index, offset) ?: return false
    save(moved)
    rebuild()
    listView?.let {
      focusAfterLayout(it, findPreference(rowKey(index + offset)), positionText(it, index + offset, moved.size))
    }
    return true
  }

  private fun delete(index: Int, name: CharSequence): Boolean {
    val steps = steps()
    if (index !in steps.indices) return false
    save(steps.filterIndexed { i, _ -> i != index })
    rebuild()
    listView?.let { announce(it, getString(R.string.quick_settings_removed, name)) }
    return true
  }

  /** Asks for the kind of step, then for what it does, and adds it at the end. */
  private fun addStep() {
    val kinds =
      listOf(
        R.string.macro_step_command,
        R.string.macro_step_wait,
        R.string.macro_step_tap,
        R.string.shortcut_title_open_app,
        R.string.macro_step_type,
      )
    choose(getString(R.string.macro_add_step), kinds.map { getString(it) }) { chosen ->
      val add: (Macros.Step) -> Unit = { step -> placeStep(null, step) }
      when (kinds[chosen]) {
        R.string.macro_step_command -> chooseCommand(add)
        R.string.macro_step_wait -> askWait(null, add)
        R.string.macro_step_tap -> askTap(null, add)
        R.string.shortcut_title_open_app -> chooseApp(add)
        R.string.macro_step_type -> askTypeText(null, add)
      }
    }
  }

  private fun editStep(index: Int) {
    val step = steps().getOrNull(index) ?: return
    val replace: (Macros.Step) -> Unit = { placeStep(index, it) }
    when (step) {
      is Macros.RunCommand -> chooseCommand(replace)
      is Macros.Wait -> askWait(step, replace)
      is Macros.Tap -> askTap(step, replace)
      is Macros.OpenApp -> chooseApp(replace)
      is Macros.TypeText -> askTypeText(step, replace)
    }
  }

  /** Saves [step] in place of the step at [index], or at the end for null, and moves focus to it. */
  private fun placeStep(index: Int?, step: Macros.Step) {
    if (!isAdded) return
    val steps = steps()
    val placed =
      if (index != null && index in steps.indices) {
        steps.mapIndexed { i, old -> if (i == index) step else old }
      } else {
        steps + step
      }
    save(placed)
    rebuild()
    val position = index?.takeIf { it in steps.indices } ?: (placed.size - 1)
    listView?.let { focusAfterLayout(it, findPreference(rowKey(position)), null) }
  }

  /** Asks for a command, first its category, leaving out macros, and gives it to [then]. */
  private fun chooseCommand(then: (Macros.Step) -> Unit) {
    val context = requireContext()
    val categories = Commands.categories(context).filter { it.titleRes != R.string.title_pref_macros }
    choose(getString(R.string.app_gestures_choose_command), categories.map { getString(it.titleRes) }) { group ->
      val commands = categories[group].commands
      choose(getString(categories[group].titleRes), commands.map { it.label }) {
        then(Macros.RunCommand(commands[it].value))
      }
    }
  }

  private fun askWait(step: Macros.Wait?, then: (Macros.Step) -> Unit) {
    askText(
      getString(R.string.macro_step_wait),
      text = step?.let { Macros.secondsText(it.millis) } ?: "1",
      hint = getString(R.string.macro_seconds),
      inputType = SECONDS_INPUT,
    ) { text, _, _ ->
      val millis = Macros.parseSeconds(text)
      if (millis == null) {
        getString(R.string.macro_invalid_seconds, (Macros.MAX_WAIT_MS / 1000).toInt())
      } else {
        then(Macros.Wait(millis))
        null
      }
    }
  }

  private fun askTap(step: Macros.Tap?, then: (Macros.Step) -> Unit) {
    askText(
      getString(R.string.macro_step_tap),
      text = step?.label.orEmpty(),
      hint = getString(R.string.macro_tap_label),
      secondHint = getString(R.string.macro_tap_timeout),
      secondText = Macros.secondsText(step?.timeoutMillis ?: Macros.DEFAULT_TAP_TIMEOUT_MS),
      inputType = InputType.TYPE_CLASS_TEXT,
      secondInputType = SECONDS_INPUT,
    ) { label, timeout, _ ->
      if (label.isEmpty()) {
        ""
      } else {
        val millis =
          Macros.parseSeconds(timeout)?.coerceIn(Macros.MIN_TAP_TIMEOUT_MS, Macros.MAX_TAP_TIMEOUT_MS)
            ?: Macros.DEFAULT_TAP_TIMEOUT_MS
        then(Macros.Tap(label, millis))
        null
      }
    }
  }

  private fun askTypeText(step: Macros.TypeText?, then: (Macros.Step) -> Unit) {
    askText(
      getString(R.string.macro_step_type),
      text = step?.text.orEmpty(),
      hint = getString(R.string.macro_text),
      checkLabels = listOf(getString(R.string.macro_replace_text)),
      checked = listOf(step?.replace == true),
    ) { text, _, checked ->
      if (text.isEmpty()) {
        ""
      } else {
        then(Macros.TypeText(text.take(Macros.MAX_TEXT), checked.firstOrNull() == true))
        null
      }
    }
  }

  /**
   * Asks for an app with an icon on the home screen, and gives the step that opens it to [then].
   * The first choice, Search, lists the apps whose names contain a text.
   */
  private fun chooseApp(then: (Macros.Step) -> Unit) {
    val context = requireContext().applicationContext
    APP_LOADER.execute {
      val apps = OpenAppActions.apps(context)
      activity?.runOnUiThread {
        if (!isAdded) return@runOnUiThread
        val title = getString(R.string.notification_choose_app)
        val search = getString(android.R.string.search_go)
        choose(title, listOf(search) + apps.map { it.label }) { index ->
          if (index > 0) {
            then(Macros.OpenApp(apps[index - 1].value))
            return@choose
          }
          askText(search) { text, _, _ ->
            val found = apps.filter { it.label.contains(text, ignoreCase = true) }
            if (found.isEmpty()) {
              getString(R.string.keyboard_shortcuts_no_results)
            } else {
              choose(title, found.map { it.label }) { then(Macros.OpenApp(found[it].value)) }
              null
            }
          }
        }
      }
    }
  }

  private fun rename() {
    val macro = macro() ?: return
    askText(getString(R.string.menu_rename), macro.name, hint = getString(R.string.menu_name)) { name, _, _ ->
      val trimmed = name.trim()
      if (trimmed.isEmpty()) {
        ""
      } else {
        Macros.update(prefs, macro.copy(name = trimmed))
        activity?.title = trimmed
        rebuild()
        null
      }
    }
  }

  private fun confirmDelete() {
    val macro = macro() ?: return
    confirm(getString(R.string.macro_delete_confirm, macro.name), R.string.menu_delete) {
      if (MacroRunner.runningId() == macro.id) MacroRunner.stop()
      Macros.delete(requireContext(), prefs, macro.id)
      requireActivity().onBackPressedDispatcher.onBackPressed()
    }
  }

  /** A step, at [index] of [steps]. */
  private inner class StepRow(
    context: Context,
    private val steps: List<Macros.Step>,
    private val index: Int,
    private val name: CharSequence,
  ) : Preference(context) {
    init {
      key = rowKey(index)
      isPersistent = false
      isIconSpaceReserved = false
      title = name
      setOnPreferenceClickListener {
        showActions(steps, index, name)
        true
      }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
      super.onBindViewHolder(holder)
      // Rows are reused for other preferences, so every bind replaces all the actions.
      val row = holder.itemView
      setMoveActions(row, index, steps.size) { move(steps, index, it) }
      val editText = getString(R.string.notification_edit)
      ViewCompat.replaceAccessibilityAction(
        row,
        AccessibilityActionCompat(R.id.accessibility_custom_action_3, editText),
        editText,
      ) { _, _ ->
        editStep(index)
        true
      }
      val deleteText = getString(R.string.menu_delete)
      ViewCompat.replaceAccessibilityAction(
        row,
        AccessibilityActionCompat(R.id.accessibility_custom_action_2, deleteText),
        deleteText,
      ) { _, _ ->
        delete(index, name)
      }
    }
  }

  companion object {
    /** The argument with the ID of the macro. */
    const val ARG_MACRO = "macro"

    private const val SECONDS_INPUT = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

    /** Loads the apps for Open app, on a thread that ends once it has been idle for a while. */
    private val APP_LOADER = ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>())
  }
}
