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
import android.os.Bundle
import android.text.InputType
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.fragment.app.Fragment
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

/** A screen that edits a macro, with the dialogs that ask for the tests of its conditions. */
abstract class MacroEditorFragment : NotificationCodeFragment(R.string.title_pref_macros) {

  protected val macroId: String by lazy { arguments?.getString(MacroFragment.ARG_MACRO).orEmpty() }

  // The title is asked for before the screen is made, when prefs isn't set yet.
  protected fun macro(): Macros.Macro? =
    Macros.find(SharedPreferencesUtils.getSharedPreferences(requireContext()), macroId)

  /** Opens [screen] with the arguments [arguments]. */
  protected fun open(screen: Class<out Fragment>, arguments: Bundle.() -> Unit) {
    onPreferenceTreeClick(
      Preference(requireContext()).apply {
        fragment = screen.name
        extras.arguments()
      }
    )
  }

  /**
   * Asks for a test, with [old] chosen, then for what it looks for, and gives it to [then].
   */
  protected fun askTest(old: Macros.Test?, then: (Macros.Test) -> Unit) {
    val kinds =
      listOf(
        R.string.macro_test_item,
        R.string.macro_test_focused,
        R.string.macro_test_app,
        R.string.macro_test_keyboard,
        R.string.macro_test_locked,
      )
    val checked =
      when (old) {
        is Macros.ItemOnScreen -> 0
        is Macros.FocusedItem -> 1
        is Macros.AppInFront -> 2
        Macros.KeyboardShown -> 3
        Macros.ScreenLocked -> 4
        null -> -1
      }
    choose(getString(R.string.macro_choose_test), kinds.map { getString(it) }, checked) { chosen ->
      when (kinds[chosen]) {
        R.string.macro_test_item -> {
          val item = old as? Macros.ItemOnScreen
          askText(
            getString(R.string.macro_test_item),
            text = item?.label.orEmpty(),
            hint = getString(R.string.macro_tap_label),
            inputType = InputType.TYPE_CLASS_TEXT,
          ) { label, _, _ ->
            if (label.isEmpty()) {
              ""
            } else {
              askKind(item?.kind, anyType = true) { then(Macros.ItemOnScreen(label.take(Macros.MAX_LABEL), it)) }
              null
            }
          }
        }
        R.string.macro_test_focused -> {
          val item = old as? Macros.FocusedItem
          askText(
            getString(R.string.macro_test_focused),
            text = item?.label.orEmpty(),
            hint = getString(R.string.macro_test_label_optional),
            inputType = InputType.TYPE_CLASS_TEXT,
          ) { label, _, _ ->
            // A test of the focused item looks for a text, a type or both.
            askKind(item?.kind, anyType = label.isNotEmpty()) {
              then(Macros.FocusedItem(label.take(Macros.MAX_LABEL), it))
            }
            null
          }
        }
        R.string.macro_test_app ->
          chooseApp(null) { packageName, _ ->
            if (packageName.isNotEmpty()) then(Macros.AppInFront(packageName))
          }
        R.string.macro_test_keyboard -> then(Macros.KeyboardShown)
        R.string.macro_test_locked -> then(Macros.ScreenLocked)
      }
    }
  }

  /** Asks for an item type, with [old] chosen, and Any type first for [anyType]. */
  private fun askKind(old: Macros.Kind?, anyType: Boolean, then: (Macros.Kind?) -> Unit) {
    val kinds: List<Macros.Kind?> = (if (anyType) listOf(null) else emptyList()) + Macros.Kind.entries
    val names = kinds.map { getString(it?.title ?: R.string.macro_kind_any) }
    choose(getString(R.string.macro_item_kind), names, kinds.indexOf(old)) { then(kinds[it]) }
  }

  /** A row of a list that can be moved, edited and deleted from its accessibility actions. */
  protected open inner class ListRow(
    context: Context,
    rowKey: String,
    protected val index: Int,
    private val count: Int,
    name: CharSequence,
    private val level: Int = 0,
  ) : Preference(context) {
    init {
      key = rowKey
      isPersistent = false
      isIconSpaceReserved = false
      title = name
    }

    open fun move(offset: Int): Boolean = false

    open fun edit(): Boolean = false

    open fun delete(): Boolean = false

    open val editable: Boolean = true

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
      super.onBindViewHolder(holder)
      // Rows are reused for other rows of the class, so every bind sets the indent and replaces all
      // the actions.
      val row = holder.itemView
      val base =
        row.getTag(R.id.macro_step_base_padding) as? Int
          ?: row.paddingStart.also { row.setTag(R.id.macro_step_base_padding, it) }
      val indent = (INDENT_DP * row.resources.displayMetrics.density).toInt()
      row.setPaddingRelative(base + level * indent, row.paddingTop, row.paddingEnd, row.paddingBottom)
      setMoveActions(row, index, count) { move(it) }
      val editText = getString(R.string.notification_edit)
      if (editable) {
        ViewCompat.replaceAccessibilityAction(
          row,
          AccessibilityActionCompat(R.id.accessibility_custom_action_3, editText),
          editText,
        ) { _, _ ->
          edit()
        }
      } else {
        ViewCompat.removeAccessibilityAction(row, R.id.accessibility_custom_action_3)
      }
      val deleteText = getString(R.string.menu_delete)
      ViewCompat.replaceAccessibilityAction(
        row,
        AccessibilityActionCompat(R.id.accessibility_custom_action_2, deleteText),
        deleteText,
      ) { _, _ ->
        delete()
      }
    }
  }

  companion object {
    private const val INDENT_DP = 24
  }
}

/**
 * One macro: Run now, the condition it runs on, its steps in order, each of which can be edited,
 * moved or deleted, and the gestures that run it.
 */
class MacroFragment : MacroEditorFragment() {

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
      screen.action(getString(R.string.macro_add_step)) { addStep(null) }
    }
    screen.link(
      getString(R.string.macro_run_only_when),
      if (macro.condition.clauses.isEmpty()) getString(R.string.macro_always)
      else Macros.describe(context, macro.condition),
      MacroConditionFragment::class.java,
    ) {
      putString(ARG_MACRO, macro.id)
    }
    val list = screen.category(R.string.macro_steps)
    if (macro.steps.isEmpty()) list.addPreference(infoRow(context, "macro_no_steps", getString(R.string.macro_no_steps)))
    val flow = Macros.analyze(macro.steps)
    flow.problem?.let {
      list.addPreference(
        infoRow(context, "macro_problem", getString(R.string.macro_problem, it.index + 1, getString(it.message)))
      )
    }
    // The names of the commands, looked up together.
    val commands = Commands.labels(context, macro.steps.map { (it as? Macros.RunCommand)?.value.orEmpty() })
    macro.steps.forEachIndexed { index, step ->
      val name =
        if (step is Macros.RunCommand) commands[index] ?: getString(R.string.macro_step_unavailable)
        else Macros.describe(context, step)
      list.addPreference(StepRow(context, macro.steps, index, name, flow.level[index]))
    }
    screen.link(getString(R.string.menu_assign), null, CommandFragment::class.java) {
      putString(CommandFragment.ARG_COMMAND, Macros.commandOf(macro.id))
    }
    screen.action(getString(R.string.menu_rename)) { rename() }
    screen.action(getString(R.string.macro_delete)) { confirmDelete() }
  }

  private fun infoRow(context: Context, rowKey: String, text: CharSequence): Preference =
    Preference(context).apply {
      key = rowKey
      isPersistent = false
      isIconSpaceReserved = false
      isSelectable = false
      title = text
    }

  private fun rowKey(index: Int): String = "macro_step_$index"

  private fun runNow() {
    if (TalkBackService.getInstance() == null) {
      toast(R.string.talkback_inactive_title)
      return
    }
    MacroRunner.run(macroId, sayNotMet = true)
  }

  private fun isEditable(step: Macros.Step): Boolean =
    step !== Macros.Else &&
      step !== Macros.EndIf &&
      step !== Macros.EndRepeat &&
      step !== Macros.ExitLoop &&
      step !== Macros.StopMacro

  private fun showActions(steps: List<Macros.Step>, index: Int, name: CharSequence) {
    val actions = ArrayList<Pair<Int, () -> Unit>>()
    if (isEditable(steps[index])) actions += R.string.notification_edit to { editStep(index) }
    if (steps.size < Macros.MAX_STEPS) actions += R.string.macro_add_step_after to { addStep(index + 1) }
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

  /** Deletes the step at [index], and the Else and End steps of the block it starts. */
  private fun delete(index: Int, name: CharSequence): Boolean {
    val removed = Macros.removed(steps(), index) ?: return false
    save(removed)
    rebuild()
    listView?.let { announce(it, getString(R.string.quick_settings_removed, name)) }
    return true
  }

  /**
   * Asks for the kind of step, then for what it does, and adds it at [at], or at the end for null.
   * If and the repeats are added with their End step.
   */
  private fun addStep(at: Int?) {
    val kinds =
      listOf(
        R.string.macro_step_command,
        R.string.macro_step_tap,
        R.string.macro_step_type,
        R.string.shortcut_title_open_app,
        R.string.macro_step_wait,
        R.string.macro_step_wait_until,
        R.string.macro_step_if,
        R.string.macro_step_else_if,
        R.string.macro_step_else,
        R.string.macro_step_end_if,
        R.string.macro_step_repeat,
        R.string.macro_step_repeat_while,
        R.string.macro_step_end_repeat,
        R.string.macro_step_exit_loop,
        R.string.shortcut_stop_macro,
      )
    choose(getString(R.string.macro_add_step), kinds.map { getString(it) }) { chosen ->
      val add: (Macros.Step) -> Unit = { step -> insertSteps(at, listOf(step)) }
      fun condition(test: Macros.Test) = Macros.Condition(listOf(Macros.Clause(test)))
      when (kinds[chosen]) {
        R.string.macro_step_command -> chooseCommand(add)
        R.string.macro_step_tap -> askTap(null, add)
        R.string.macro_step_type -> askTypeText(null, add)
        R.string.shortcut_title_open_app -> chooseOpenApp(add)
        R.string.macro_step_wait -> askWait(null, add)
        R.string.macro_step_wait_until -> askTest(null) { add(Macros.WaitUntil(condition(it))) }
        R.string.macro_step_if ->
          askTest(null) { insertSteps(at, listOf(Macros.If(condition(it)), Macros.EndIf)) }
        R.string.macro_step_else_if -> askTest(null) { add(Macros.ElseIf(condition(it))) }
        R.string.macro_step_else -> add(Macros.Else)
        R.string.macro_step_end_if -> add(Macros.EndIf)
        R.string.macro_step_repeat ->
          askTimes(null) { insertSteps(at, listOf(Macros.RepeatTimes(it), Macros.EndRepeat)) }
        R.string.macro_step_repeat_while ->
          askTest(null) { insertSteps(at, listOf(Macros.RepeatWhile(condition(it)), Macros.EndRepeat)) }
        R.string.macro_step_end_repeat -> add(Macros.EndRepeat)
        R.string.macro_step_exit_loop -> add(Macros.ExitLoop)
        R.string.shortcut_stop_macro -> add(Macros.StopMacro)
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
      is Macros.OpenApp -> chooseOpenApp(replace)
      is Macros.TypeText -> askTypeText(step, replace)
      is Macros.RepeatTimes -> askTimes(step.count) { placeStep(index, Macros.RepeatTimes(it)) }
      is Macros.If, is Macros.ElseIf, is Macros.RepeatWhile, is Macros.WaitUntil ->
        open(MacroConditionFragment::class.java) {
          putString(ARG_MACRO, macroId)
          putInt(MacroConditionFragment.ARG_STEP, index)
        }
      Macros.Else, Macros.EndIf, Macros.EndRepeat, Macros.ExitLoop, Macros.StopMacro -> {}
    }
  }

  /** Saves [step] in place of the step at [index], and moves focus to it. */
  private fun placeStep(index: Int, step: Macros.Step) {
    if (!isAdded) return
    val steps = steps()
    if (index !in steps.indices) return
    save(steps.mapIndexed { i, old -> if (i == index) step else old })
    rebuild()
    listView?.let { focusAfterLayout(it, findPreference(rowKey(index)), null) }
  }

  /** Adds [added] at [at], or at the end for null, and moves focus to the first of them. */
  private fun insertSteps(at: Int?, added: List<Macros.Step>) {
    if (!isAdded) return
    val steps = steps()
    if (steps.size + added.size > Macros.MAX_STEPS) return
    val position = at?.coerceIn(0, steps.size) ?: steps.size
    save(steps.subList(0, position) + added + steps.subList(position, steps.size))
    rebuild()
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

  private fun askTimes(count: Int?, then: (Int) -> Unit) {
    askText(
      getString(R.string.macro_step_repeat),
      text = (count ?: 2).toString(),
      hint = getString(R.string.macro_times),
      inputType = InputType.TYPE_CLASS_NUMBER,
    ) { text, _, _ ->
      val times = text.toIntOrNull()?.takeIf { it in 1..Macros.MAX_REPEATS }
      if (times == null) {
        getString(R.string.macro_invalid_times, Macros.MAX_REPEATS)
      } else {
        then(times)
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
  private fun chooseOpenApp(then: (Macros.Step) -> Unit) {
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
          askText(search, inputType = InputType.TYPE_CLASS_TEXT) { text, _, _ ->
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

  /** A step, at [index] of [steps], indented by the [level] of blocks it is in. */
  private inner class StepRow(
    context: Context,
    private val steps: List<Macros.Step>,
    index: Int,
    private val name: CharSequence,
    level: Int,
  ) : ListRow(context, rowKey(index), index, steps.size, name, level) {
    init {
      setOnPreferenceClickListener {
        showActions(steps, index, name)
        true
      }
    }

    override val editable: Boolean = isEditable(steps[index])

    override fun move(offset: Int): Boolean = this@MacroFragment.move(steps, index, offset)

    override fun edit(): Boolean {
      editStep(index)
      return true
    }

    override fun delete(): Boolean = this@MacroFragment.delete(index, name)
  }

  companion object {
    /** The argument with the ID of the macro, for each screen of a macro. */
    const val ARG_MACRO = "macro"

    private const val SECONDS_INPUT = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

    /** Loads the apps for Open app, on a thread that ends once it has been idle for a while. */
    private val APP_LOADER = ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>())
  }
}

/**
 * The condition of a macro step, or for no step, the condition the macro runs on: its tests in
 * order, each of which can be edited, reversed, joined with and or or, moved or deleted.
 */
class MacroConditionFragment : MacroEditorFragment() {

  private val stepIndex: Int by lazy { arguments?.getInt(ARG_STEP, -1) ?: -1 }

  private fun step(): Macros.Step? = macro()?.steps?.getOrNull(stepIndex)

  override fun getTitle(): CharSequence =
    getText(
      when (step()) {
        is Macros.If -> R.string.macro_step_if
        is Macros.ElseIf -> R.string.macro_step_else_if
        is Macros.RepeatWhile -> R.string.macro_step_repeat_while
        is Macros.WaitUntil -> R.string.macro_step_wait_until
        else -> R.string.macro_run_only_when
      }
    )

  /** The condition, or null when the step isn't there any more. */
  private fun condition(): Macros.Condition? =
    if (stepIndex < 0) macro()?.condition else Macros.conditionOf(step())

  private fun save(condition: Macros.Condition) {
    val macro = macro() ?: return
    val clauses = condition.clauses.take(Macros.MAX_CLAUSES)
    // The first test has nothing before it to join.
    val saved = Macros.Condition(clauses.mapIndexed { i, clause -> if (i == 0) clause.copy(or = false) else clause })
    if (stepIndex < 0) {
      Macros.update(prefs, macro.copy(condition = saved))
      return
    }
    val step = macro.steps.getOrNull(stepIndex) ?: return
    val changed = Macros.withCondition(step, saved) ?: return
    Macros.update(prefs, macro.copy(steps = macro.steps.mapIndexed { i, old -> if (i == stepIndex) changed else old }))
  }

  override fun build(screen: PreferenceGroup) {
    val context = requireContext()
    val condition = condition() ?: return
    val clauses = condition.clauses
    if (clauses.size < Macros.MAX_CLAUSES) {
      screen.action(getString(R.string.macro_add_test)) {
        askTest(null) { test ->
          val current = condition() ?: return@askTest
          save(Macros.Condition(current.clauses + Macros.Clause(test)))
          rebuild()
          listView?.let { focusAfterLayout(it, findPreference(rowKey(current.clauses.size)), null) }
        }
      }
    }
    (step() as? Macros.WaitUntil)?.let { wait ->
      screen.action(getString(R.string.macro_wait_timeout), Macros.secondsText(wait.timeoutMillis)) {
        askTimeout(wait)
      }
    }
    val list = screen.category(R.string.macro_tests)
    if (clauses.isEmpty()) {
      list.addPreference(
        Preference(context).apply {
          key = "macro_no_tests"
          isPersistent = false
          isIconSpaceReserved = false
          isSelectable = false
          setTitle(R.string.macro_no_tests)
        }
      )
    }
    clauses.forEachIndexed { index, clause ->
      list.addPreference(ClauseRow(context, clauses, index, clauseText(index, clause)))
    }
  }

  private fun clauseText(index: Int, clause: Macros.Clause): String {
    val text = Macros.describe(requireContext(), clause)
    return when {
      index == 0 -> text
      clause.or -> getString(R.string.macro_clause_or, text)
      else -> getString(R.string.macro_clause_and, text)
    }
  }

  private fun rowKey(index: Int): String = "macro_test_$index"

  private fun askTimeout(wait: Macros.WaitUntil) {
    askText(
      getString(R.string.macro_wait_timeout),
      text = Macros.secondsText(wait.timeoutMillis),
      hint = getString(R.string.macro_seconds),
      inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
    ) { text, _, _ ->
      val millis = Macros.parseSeconds(text)?.takeIf { it >= Macros.MIN_TAP_TIMEOUT_MS }
      val macro = macro()
      if (millis == null) {
        getString(R.string.macro_invalid_seconds, (Macros.MAX_WAIT_MS / 1000).toInt())
      } else {
        val step = step() as? Macros.WaitUntil
        if (macro != null && step != null) {
          val changed = step.copy(timeoutMillis = millis)
          Macros.update(prefs, macro.copy(steps = macro.steps.mapIndexed { i, old -> if (i == stepIndex) changed else old }))
          rebuild()
        }
        null
      }
    }
  }

  /** Saves [clauses] with [change] made to the test at [index], and moves focus to it. */
  private fun change(clauses: List<Macros.Clause>, index: Int, change: (Macros.Clause) -> Macros.Clause) {
    if (!isAdded || index !in clauses.indices) return
    save(Macros.Condition(clauses.mapIndexed { i, clause -> if (i == index) change(clause) else clause }))
    rebuild()
    listView?.let { focusAfterLayout(it, findPreference(rowKey(index)), null) }
  }

  private fun edit(clauses: List<Macros.Clause>, index: Int) {
    val clause = clauses.getOrNull(index) ?: return
    askTest(clause.test) { test -> change(clauses, index) { it.copy(test = test) } }
  }

  private fun move(clauses: List<Macros.Clause>, index: Int, offset: Int): Boolean {
    val target = index + offset
    if (index !in clauses.indices || target !in clauses.indices) return false
    val moved = clauses.toMutableList().apply { add(target, removeAt(index)) }
    save(Macros.Condition(moved))
    rebuild()
    listView?.let { focusAfterLayout(it, findPreference(rowKey(target)), positionText(it, target, moved.size)) }
    return true
  }

  private fun delete(clauses: List<Macros.Clause>, index: Int, name: CharSequence): Boolean {
    if (index !in clauses.indices) return false
    save(Macros.Condition(clauses.filterIndexed { i, _ -> i != index }))
    rebuild()
    listView?.let { announce(it, getString(R.string.quick_settings_removed, name)) }
    return true
  }

  private fun showActions(clauses: List<Macros.Clause>, index: Int, name: CharSequence) {
    val clause = clauses[index]
    val reversed = Macros.describe(requireContext(), clause.copy(not = !clause.not))
    val actions = ArrayList<Pair<String, () -> Unit>>()
    actions += getString(R.string.notification_edit) to { edit(clauses, index) }
    actions += getString(R.string.macro_change_to, reversed) to { change(clauses, index) { it.copy(not = !it.not) } }
    if (index > 0) {
      actions +=
        getString(if (clause.or) R.string.macro_join_with_and else R.string.macro_join_with_or) to
          { change(clauses, index) { it.copy(or = !it.or) } }
    }
    if (index > 0) actions += getString(R.string.action_move_up) to { move(clauses, index, -1) }
    if (index < clauses.size - 1) actions += getString(R.string.action_move_down) to { move(clauses, index, 1) }
    actions += getString(R.string.menu_delete) to { delete(clauses, index, name) }
    choose(name, actions.map { it.first }) { actions[it].second() }
  }

  /** A test, at [index] of [clauses]. */
  private inner class ClauseRow(
    context: Context,
    private val clauses: List<Macros.Clause>,
    index: Int,
    private val name: CharSequence,
  ) : ListRow(context, rowKey(index), index, clauses.size, name) {
    init {
      setOnPreferenceClickListener {
        showActions(clauses, index, name)
        true
      }
    }

    override fun move(offset: Int): Boolean = this@MacroConditionFragment.move(clauses, index, offset)

    override fun edit(): Boolean {
      this@MacroConditionFragment.edit(clauses, index)
      return true
    }

    override fun delete(): Boolean = this@MacroConditionFragment.delete(clauses, index, name)
  }

  companion object {
    /** The argument with the index of the step, or -1 for the condition the macro runs on. */
    const val ARG_STEP = "step"
  }
}
