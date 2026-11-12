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
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.annotation.StringRes
import androidx.core.content.edit
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.commands.CommandAssignments
import com.google.android.accessibility.talkback.commands.Commands
import com.google.android.accessibility.talkback.gesture.OpenAppActions
import java.util.UUID
import kotlin.math.roundToLong
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Macros: named lists of steps that run one after another, each macro a command. Conditions choose
 * between steps (If, Else if, Else) and repeat them (Repeat while), and a macro can have a condition
 * of its own that has to be met for it to run.
 *
 * They are kept as JSON in the default preferences, in device-protected storage, so that settings
 * export and import include them.
 */
object Macros {
  /** One step of a macro. */
  sealed class Step

  /** Runs the command saved as [value]. */
  data class RunCommand(val value: String) : Step()

  /** Waits [millis] before the next step. */
  data class Wait(val millis: Long) : Step()

  /**
   * Clicks the first item on screen whose text or description contains [label], ignoring case,
   * waiting up to [timeoutMillis] for it to appear.
   */
  data class Tap(val label: String, val timeoutMillis: Long = DEFAULT_TAP_TIMEOUT_MS) : Step()

  /** Opens the app of the [OpenAppActions] value [value]. */
  data class OpenApp(val value: String) : Step()

  /** Types [text] in the focused edit field, at the cursor, or in place of its text for [replace]. */
  data class TypeText(val text: String, val replace: Boolean = false) : Step()

  /** Runs the steps up to the matching [Else], [ElseIf] or [EndIf] when [condition] is met. */
  data class If(val condition: Condition) : Step()

  /** Starts the steps that run when no branch before it ran and [condition] is met. */
  data class ElseIf(val condition: Condition) : Step()

  /** Starts the steps that run when no branch before it ran. */
  data object Else : Step()

  /** Ends an [If] block. */
  data object EndIf : Step()

  /** Runs the steps up to the matching [EndRepeat] [count] times. */
  data class RepeatTimes(val count: Int) : Step()

  /** Runs the steps up to the matching [EndRepeat] while [condition] is met. */
  data class RepeatWhile(val condition: Condition) : Step()

  /** Ends a [RepeatTimes] or [RepeatWhile] block. */
  data object EndRepeat : Step()

  /** Goes on after the [EndRepeat] of the innermost repeat. */
  data object ExitLoop : Step()

  /** Stops the macro. */
  data object StopMacro : Step()

  /** Waits until [condition] is met, up to [timeoutMillis]; the macro stops after that. */
  data class WaitUntil(val condition: Condition, val timeoutMillis: Long = DEFAULT_WAIT_UNTIL_MS) :
    Step()

  /** A kind of item a test can look for. [key] is saved and must never change once released. */
  enum class Kind(val key: String, @StringRes val title: Int) {
    CLICKABLE("clickable", R.string.macro_kind_clickable),
    LONG_CLICKABLE("long_clickable", R.string.macro_kind_long_clickable),
    EDITABLE("editable", R.string.macro_kind_editable),
    SCROLLABLE("scrollable", R.string.macro_kind_scrollable),
    ADJUSTABLE("adjustable", R.string.macro_kind_adjustable),
    CHECKABLE("checkable", R.string.macro_kind_checkable),
    CHECKED("checked", R.string.macro_kind_checked),
    SELECTED("selected", R.string.macro_kind_selected),
    HEADING("heading", R.string.macro_kind_heading),
    ENABLED("enabled", R.string.macro_kind_enabled);

    companion object {
      fun of(key: String?): Kind? = entries.firstOrNull { it.key == key }
    }
  }

  /** What a condition checks. */
  sealed class Test

  /**
   * An item on screen whose text, description or hint contains [label], ignoring case, and that is
   * of [kind] when it isn't null.
   */
  data class ItemOnScreen(val label: String, val kind: Kind? = null) : Test()

  /**
   * The item with accessibility focus: its text, or the text of the items in it, contains [label]
   * when it isn't empty, and it is of [kind] when it isn't null.
   */
  data class FocusedItem(val label: String, val kind: Kind? = null) : Test()

  /** The app of the active window is [packageName]. */
  data class AppInFront(val packageName: String) : Test()

  /** The on-screen keyboard is shown. */
  data object KeyboardShown : Test()

  /** The lock screen is shown. */
  data object ScreenLocked : Test()

  /**
   * One test of a condition. [not] reverses it; [or] joins it to the tests before it with "or"
   * instead of "and".
   */
  data class Clause(val test: Test, val not: Boolean = false, val or: Boolean = false)

  /**
   * Tests joined with "and" and "or", where "and" goes first: the condition is met when all the tests
   * of one of the groups that "or" separates are met. A condition without tests is always met.
   */
  data class Condition(val clauses: List<Clause> = emptyList()) {
    /** Returns whether the condition is met, checking each test with [check] only as needed. */
    fun isMet(check: (Test) -> Boolean): Boolean {
      if (clauses.isEmpty()) return true
      var group = true
      clauses.forEachIndexed { index, clause ->
        if (index > 0 && clause.or) {
          if (group) return true
          group = true
        }
        if (group) group = check(clause.test) != clause.not
      }
      return group
    }
  }

  /** A macro. [id] is unique among the macros. It only runs when [condition] is met. */
  data class Macro(
    val id: String,
    val name: String,
    val steps: List<Step>,
    val condition: Condition = Condition(),
  )

  /** Why the steps of a macro can't run, at the step [index]. */
  data class Problem(val index: Int, @StringRes val message: Int)

  /**
   * Where each block step of [steps] leads, from [analyze]. For an [If] or [ElseIf], [next] is the
   * next [ElseIf], [Else] or [EndIf] of its block, and [end] its [EndIf]; for an [Else], [end] is its
   * [EndIf]. For a repeat, [end] is its [EndRepeat]; for an [EndRepeat] or [ExitLoop], [start] is
   * the repeat. [level] is how many blocks each step is in. [problem] is the first mismatched block
   * step, and when it isn't null the other values are incomplete.
   */
  class Flow(
    val next: IntArray,
    val end: IntArray,
    val start: IntArray,
    val level: IntArray,
    val problem: Problem?,
  )

  /** The value of the command that stops the macro running. It must never change once released. */
  const val STOP_COMMAND = "STOP_MACRO"

  const val KEY = "pref_macros"

  /** The most steps a macro keeps. */
  const val MAX_STEPS = 200

  const val DEFAULT_TAP_TIMEOUT_MS = 3_000L
  const val MIN_TAP_TIMEOUT_MS = 500L
  const val MAX_TAP_TIMEOUT_MS = 60_000L

  /** The longest wait, 10 minutes. */
  const val MAX_WAIT_MS = 600_000L

  const val DEFAULT_WAIT_UNTIL_MS = 10_000L

  /** The most times a repeat runs its steps in one run of a macro. */
  const val MAX_REPEATS = 1_000

  /** The most tests a condition keeps. */
  const val MAX_CLAUSES = 20

  /** The most characters of the text a test looks for. */
  const val MAX_LABEL = 500

  /** The most characters of text a step types. */
  const val MAX_TEXT = 10_000

  /** Starts the value of a macro's command. It must never change once released. */
  private const val COMMAND_PREFIX = "MACRO:"

  private const val TYPE_COMMAND = "command"
  private const val TYPE_WAIT = "wait"
  private const val TYPE_TAP = "tap"
  private const val TYPE_OPEN_APP = "open_app"
  private const val TYPE_TYPE_TEXT = "type_text"
  private const val TYPE_IF = "if"
  private const val TYPE_ELSE_IF = "else_if"
  private const val TYPE_ELSE = "else"
  private const val TYPE_END_IF = "end_if"
  private const val TYPE_REPEAT = "repeat"
  private const val TYPE_REPEAT_WHILE = "repeat_while"
  private const val TYPE_END_REPEAT = "end_repeat"
  private const val TYPE_EXIT_LOOP = "exit_loop"
  private const val TYPE_STOP = "stop"
  private const val TYPE_WAIT_UNTIL = "wait_until"

  private const val TEST_ITEM = "item"
  private const val TEST_FOCUSED = "focused"
  private const val TEST_APP = "app"
  private const val TEST_KEYBOARD = "keyboard"
  private const val TEST_LOCKED = "locked"

  @JvmStatic fun newId(): String = UUID.randomUUID().toString()

  /** Returns the value of the command that runs the macro [id]. */
  @JvmStatic fun commandOf(id: String): String = COMMAND_PREFIX + id

  /** Returns whether [value] is the command of a macro or [STOP_COMMAND]. */
  @JvmStatic
  fun isCommand(value: String?): Boolean =
    value != null && (value.startsWith(COMMAND_PREFIX) || value == STOP_COMMAND)

  /** Returns the ID of the macro whose command is [value], or null for other commands. */
  @JvmStatic
  fun idOf(value: String?): String? =
    value?.takeIf { it.startsWith(COMMAND_PREFIX) }?.substring(COMMAND_PREFIX.length)

  /** Returns the name of the command [value], a macro's or [STOP_COMMAND], or null for others. */
  @JvmStatic
  fun label(context: Context, prefs: SharedPreferences, value: String?): String? {
    if (value == STOP_COMMAND) return context.getString(R.string.shortcut_stop_macro)
    val id = idOf(value) ?: return null
    return read(prefs).firstOrNull { it.id == id }?.name
  }

  @Volatile private var parsed: Pair<String?, List<Macro>>? = null

  /** Returns the macros. */
  @JvmStatic
  fun read(prefs: SharedPreferences): List<Macro> {
    val json = prefs.getString(KEY, null)
    parsed?.takeIf { it.first == json }?.let {
      return it.second
    }
    return fromJson(json).also { parsed = json to it }
  }

  @JvmStatic
  fun write(prefs: SharedPreferences, macros: List<Macro>) {
    prefs.edit { putString(KEY, toJson(macros)) }
  }

  /** Returns the macro [id], or null. */
  @JvmStatic fun find(prefs: SharedPreferences, id: String): Macro? = read(prefs).firstOrNull { it.id == id }

  /** Adds a macro named [name] with no steps, and returns it. */
  fun add(prefs: SharedPreferences, name: String): Macro {
    val macro = Macro(newId(), name, emptyList())
    write(prefs, read(prefs) + macro)
    return macro
  }

  /** Saves [macro] in place of the macro with its ID. */
  fun update(prefs: SharedPreferences, macro: Macro) {
    write(prefs, read(prefs).map { if (it.id == macro.id) macro else it })
  }

  /**
   * Deletes the macro [id], and takes its command off the gestures, keys, menus and other settings
   * that run it.
   */
  fun delete(context: Context, prefs: SharedPreferences, id: String) {
    write(prefs, read(prefs).filter { it.id != id })
    CommandAssignments.unassign(context, prefs, commandOf(id))
  }

  /** Returns [steps] with the step at [index] moved by [offset], or null when it can't move. */
  @JvmStatic
  fun moved(steps: List<Step>, index: Int, offset: Int): List<Step>? {
    val target = index + offset
    if (index !in steps.indices || target !in steps.indices) return null
    return steps.toMutableList().apply { add(target, removeAt(index)) }
  }

  /**
   * Returns the milliseconds in [seconds], a number of seconds that can have a decimal point or
   * comma, or null when it isn't one.
   */
  @JvmStatic
  fun parseSeconds(seconds: String): Long? {
    val number = seconds.trim().replace(',', '.').toDoubleOrNull() ?: return null
    if (number.isNaN() || number < 0 || number > MAX_WAIT_MS / 1000.0) return null
    return (number * 1000).roundToLong()
  }

  /** Returns [millis] as seconds for a text field: whole seconds without a decimal point. */
  @JvmStatic
  fun secondsText(millis: Long): String =
    if (millis % 1000 == 0L) (millis / 1000).toString()
    else (millis / 1000.0).toBigDecimal().stripTrailingZeros().toPlainString()

  /** Returns what [step] does, as the editor and the message of a failed step say it. */
  @JvmStatic
  fun describe(context: Context, step: Step): String =
    when (step) {
      is RunCommand ->
        Commands.label(context, step.value) ?: context.getString(R.string.macro_step_unavailable)
      is Wait -> waitText(context, step.millis)
      is Tap -> context.getString(R.string.macro_step_tap_value, step.label)
      is OpenApp ->
        OpenAppActions.label(context, step.value) ?: context.getString(R.string.macro_step_unavailable)
      is TypeText ->
        context.getString(
          if (step.replace) R.string.macro_step_replace_value else R.string.macro_step_type_value,
          step.text,
        )
      is If -> context.getString(R.string.macro_step_if_value, describe(context, step.condition))
      is ElseIf -> context.getString(R.string.macro_step_else_if_value, describe(context, step.condition))
      Else -> context.getString(R.string.macro_step_else)
      EndIf -> context.getString(R.string.macro_step_end_if)
      is RepeatTimes ->
        context.resources.getQuantityString(R.plurals.macro_step_repeat_value, step.count, step.count)
      is RepeatWhile ->
        context.getString(R.string.macro_step_repeat_while_value, describe(context, step.condition))
      EndRepeat -> context.getString(R.string.macro_step_end_repeat)
      ExitLoop -> context.getString(R.string.macro_step_exit_loop)
      StopMacro -> context.getString(R.string.shortcut_stop_macro)
      is WaitUntil ->
        context.getString(R.string.macro_step_wait_until_value, describe(context, step.condition))
    }

  /** Returns [condition] as its tests joined with "and" and "or", or "No tests". */
  @JvmStatic
  fun describe(context: Context, condition: Condition): String {
    if (condition.clauses.isEmpty()) return context.getString(R.string.macro_no_tests)
    var text = describe(context, condition.clauses[0])
    for (clause in condition.clauses.drop(1)) {
      text =
        context.getString(
          if (clause.or) R.string.macro_join_or else R.string.macro_join_and,
          text,
          describe(context, clause),
        )
    }
    return text
  }

  /** Returns what [clause] checks, its test said the other way round when it is reversed. */
  @JvmStatic
  fun describe(context: Context, clause: Clause): String {
    val not = clause.not
    return when (val test = clause.test) {
      is ItemOnScreen ->
        context.getString(
          if (not) R.string.macro_test_no_item_value else R.string.macro_test_item_value,
          itemText(context, test.label, test.kind),
        )
      is FocusedItem ->
        context.getString(
          if (not) R.string.macro_test_not_focused_value else R.string.macro_test_focused_value,
          itemText(context, test.label, test.kind),
        )
      is AppInFront ->
        context.getString(
          if (not) R.string.macro_test_not_app_value else R.string.macro_test_app_value,
          appName(context, test.packageName),
        )
      KeyboardShown ->
        context.getString(if (not) R.string.macro_test_keyboard_hidden else R.string.macro_test_keyboard)
      ScreenLocked ->
        context.getString(if (not) R.string.macro_test_unlocked else R.string.macro_test_locked)
    }
  }

  private fun itemText(context: Context, label: String, kind: Kind?): String =
    when {
      kind == null -> label.ifEmpty { context.getString(R.string.macro_kind_any) }
      label.isEmpty() -> context.getString(kind.title)
      else -> context.getString(R.string.macro_test_label_kind, label, context.getString(kind.title))
    }

  /** Returns the name of the app [packageName], or the package name when it isn't installed. */
  @JvmStatic
  fun appName(context: Context, packageName: String): String =
    try {
      val packageManager = context.packageManager
      packageManager.getApplicationInfo(packageName, 0).loadLabel(packageManager).toString()
    } catch (e: PackageManager.NameNotFoundException) {
      packageName
    }

  /** Returns the condition of [step], or null for a step without one. */
  @JvmStatic
  fun conditionOf(step: Step?): Condition? =
    when (step) {
      is If -> step.condition
      is ElseIf -> step.condition
      is RepeatWhile -> step.condition
      is WaitUntil -> step.condition
      else -> null
    }

  /** Returns [step] with [condition] in place of its own, or null for a step without one. */
  @JvmStatic
  fun withCondition(step: Step, condition: Condition): Step? =
    when (step) {
      is If -> step.copy(condition = condition)
      is ElseIf -> step.copy(condition = condition)
      is RepeatWhile -> step.copy(condition = condition)
      is WaitUntil -> step.copy(condition = condition)
      else -> null
    }

  private fun isRepeat(step: Step): Boolean = step is RepeatTimes || step is RepeatWhile

  /** Works out where the block steps of [steps] lead, and the first one that doesn't match. */
  @JvmStatic
  fun analyze(steps: List<Step>): Flow {
    val size = steps.size
    val next = IntArray(size) { -1 }
    val end = IntArray(size) { -1 }
    val start = IntArray(size) { -1 }
    val level = IntArray(size)
    // Open blocks: the If or repeat, and for an If its branch steps so far.
    val open = ArrayList<Int>()
    val branches = HashMap<Int, MutableList<Int>>()
    var problem: Problem? = null
    fun problem(index: Int, message: Int) {
      if (problem == null) problem = Problem(index, message)
    }
    // Closes blocks down to the innermost one [wanted] accepts, reporting the blocks left open.
    fun innermost(index: Int, wanted: (Step) -> Boolean, missing: Int): Int? {
      val found = open.indexOfLast { wanted(steps[it]) }
      if (found < 0) {
        problem(index, missing)
        return null
      }
      if (found < open.size - 1) {
        val inner = open.last()
        problem(inner, if (isRepeat(steps[inner])) R.string.macro_problem_no_end_repeat else R.string.macro_problem_no_end_if)
        while (open.size > found + 1) open.removeAt(open.size - 1)
      }
      return open.last()
    }
    steps.forEachIndexed { index, step ->
      level[index] = open.size
      when {
        step is If -> {
          open.add(index)
          branches[index] = mutableListOf(index)
        }
        isRepeat(step) -> open.add(index)
        step is ElseIf || step === Else -> {
          val block = innermost(index, { it is If }, R.string.macro_problem_no_if) ?: return@forEachIndexed
          level[index] = open.size - 1
          val list = branches.getValue(block)
          if (steps[list.last()] === Else) problem(index, R.string.macro_problem_after_else)
          next[list.last()] = index
          list.add(index)
        }
        step === EndIf -> {
          val block = innermost(index, { it is If }, R.string.macro_problem_no_if) ?: return@forEachIndexed
          level[index] = open.size - 1
          val list = branches.getValue(block)
          next[list.last()] = index
          for (branch in list) end[branch] = index
          open.removeAt(open.size - 1)
        }
        step === EndRepeat -> {
          val block = innermost(index, ::isRepeat, R.string.macro_problem_no_repeat) ?: return@forEachIndexed
          level[index] = open.size - 1
          start[index] = block
          end[block] = index
          open.removeAt(open.size - 1)
        }
        step === ExitLoop -> {
          val loop = open.lastOrNull { isRepeat(steps[it]) }
          if (loop == null) problem(index, R.string.macro_problem_exit) else start[index] = loop
        }
      }
    }
    open.firstOrNull()?.let {
      problem(it, if (isRepeat(steps[it])) R.string.macro_problem_no_end_repeat else R.string.macro_problem_no_end_if)
    }
    return Flow(next, end, start, level, problem)
  }

  /**
   * Returns [steps] without the step at [index], and when it starts a block, without the Else if,
   * Else and End steps of that block either; the steps in the block stay. Returns null when [index]
   * isn't a step.
   */
  @JvmStatic
  fun removed(steps: List<Step>, index: Int): List<Step>? {
    val drop = removedIndices(steps, index) ?: return null
    return steps.filterIndexed { i, _ -> i !in drop }
  }

  /** Returns the indices of the steps [removed] takes out, or null when [index] isn't a step. */
  @JvmStatic
  fun removedIndices(steps: List<Step>, index: Int): Set<Int>? {
    if (index !in steps.indices) return null
    val drop = hashSetOf(index)
    val step = steps[index]
    if (step is If || isRepeat(step)) {
      // The block's own steps, found by counting the blocks opened and closed after it.
      var depth = 0
      for (i in index + 1 until steps.size) {
        val other = steps[i]
        when {
          other is If || isRepeat(other) -> depth++
          other === EndIf || other === EndRepeat -> {
            if (depth == 0) {
              if ((other === EndIf) == (step is If)) drop.add(i)
              break
            }
            depth--
          }
          (other is ElseIf || other === Else) && depth == 0 && step is If -> drop.add(i)
        }
      }
    }
    return drop
  }

  /**
   * Returns where [step] goes when it is added at [position] of [steps]: an [ElseIf] or [Else] right
   * after an [EndIf] goes before it, into that If block, since it can't follow End if.
   */
  @JvmStatic
  fun insertPosition(steps: List<Step>, position: Int, step: Step): Int {
    val at = position.coerceIn(0, steps.size)
    return if ((step is ElseIf || step === Else) && at > 0 && steps[at - 1] === EndIf) at - 1 else at
  }

  /**
   * Returns the position after the step that was before [position] once the step at [from] has moved
   * to [to], so that the next step added still follows the same step.
   */
  @JvmStatic
  fun positionAfterMove(position: Int, from: Int, to: Int): Int {
    val anchor = position - 1
    if (anchor < 0) return 0
    val moved =
      when {
        anchor == from -> to
        from < anchor && anchor <= to -> anchor - 1
        to <= anchor && anchor < from -> anchor + 1
        else -> anchor
      }
    return moved + 1
  }

  /** Returns [position] once the steps at [dropped] are deleted. */
  @JvmStatic
  fun positionAfterRemoval(position: Int, dropped: Set<Int>): Int = position - dropped.count { it < position }

  private fun waitText(context: Context, millis: Long): String =
    if (millis % 1000 == 0L) {
      val seconds = (millis / 1000).toInt()
      context.resources.getQuantityString(R.plurals.macro_step_wait_seconds, seconds, seconds)
    } else {
      val ms = millis.toInt()
      context.resources.getQuantityString(R.plurals.macro_step_wait_milliseconds, ms, ms)
    }

  internal fun toJson(macros: List<Macro>): String =
    JSONArray()
      .apply {
        for (macro in macros) {
          put(
            JSONObject()
              .put("id", macro.id)
              .put("name", macro.name)
              .put("steps", JSONArray().apply { macro.steps.forEach { put(stepToJson(it)) } })
              .apply { if (macro.condition.clauses.isNotEmpty()) put("condition", conditionToJson(macro.condition)) }
          )
        }
      }
      .toString()

  private fun stepToJson(step: Step): JSONObject =
    when (step) {
      is RunCommand -> JSONObject().put("type", TYPE_COMMAND).put("command", step.value)
      is Wait -> JSONObject().put("type", TYPE_WAIT).put("ms", step.millis)
      is Tap ->
        JSONObject().put("type", TYPE_TAP).put("label", step.label).put("timeout", step.timeoutMillis)
      is OpenApp -> JSONObject().put("type", TYPE_OPEN_APP).put("app", step.value)
      is TypeText ->
        JSONObject().put("type", TYPE_TYPE_TEXT).put("text", step.text).put("replace", step.replace)
      is If -> JSONObject().put("type", TYPE_IF).put("condition", conditionToJson(step.condition))
      is ElseIf -> JSONObject().put("type", TYPE_ELSE_IF).put("condition", conditionToJson(step.condition))
      Else -> JSONObject().put("type", TYPE_ELSE)
      EndIf -> JSONObject().put("type", TYPE_END_IF)
      is RepeatTimes -> JSONObject().put("type", TYPE_REPEAT).put("count", step.count)
      is RepeatWhile ->
        JSONObject().put("type", TYPE_REPEAT_WHILE).put("condition", conditionToJson(step.condition))
      EndRepeat -> JSONObject().put("type", TYPE_END_REPEAT)
      ExitLoop -> JSONObject().put("type", TYPE_EXIT_LOOP)
      StopMacro -> JSONObject().put("type", TYPE_STOP)
      is WaitUntil ->
        JSONObject()
          .put("type", TYPE_WAIT_UNTIL)
          .put("condition", conditionToJson(step.condition))
          .put("timeout", step.timeoutMillis)
    }

  private fun conditionToJson(condition: Condition): JSONArray =
    JSONArray().apply {
      for (clause in condition.clauses) {
        val item =
          when (val test = clause.test) {
            is ItemOnScreen ->
              JSONObject().put("test", TEST_ITEM).put("label", test.label).put("kind", test.kind?.key)
            is FocusedItem ->
              JSONObject().put("test", TEST_FOCUSED).put("label", test.label).put("kind", test.kind?.key)
            is AppInFront -> JSONObject().put("test", TEST_APP).put("package", test.packageName)
            KeyboardShown -> JSONObject().put("test", TEST_KEYBOARD)
            ScreenLocked -> JSONObject().put("test", TEST_LOCKED)
          }
        if (clause.not) item.put("not", true)
        if (clause.or) item.put("or", true)
        put(item)
      }
    }

  /** Reads a condition saved by [conditionToJson], leaving out tests it can't read. */
  private fun conditionFromJson(array: JSONArray?): Condition {
    if (array == null) return Condition()
    val clauses =
      (0 until array.length())
        .mapNotNull { index ->
          val item = array.optJSONObject(index) ?: return@mapNotNull null
          val test =
            when (item.optString("test")) {
              TEST_ITEM ->
                item.optString("label").take(MAX_LABEL).takeIf { it.isNotBlank() }?.let {
                  ItemOnScreen(it, Kind.of(item.optString("kind")))
                }
              TEST_FOCUSED -> {
                val label = item.optString("label").take(MAX_LABEL)
                val kind = Kind.of(item.optString("kind"))
                if (label.isBlank() && kind == null) null else FocusedItem(label.trim(), kind)
              }
              TEST_APP -> item.optString("package").takeIf { it.isNotEmpty() }?.let { AppInFront(it) }
              TEST_KEYBOARD -> KeyboardShown
              TEST_LOCKED -> ScreenLocked
              else -> null
            } ?: return@mapNotNull null
          Clause(test, item.optBoolean("not", false), item.optBoolean("or", false))
        }
        .take(MAX_CLAUSES)
    return Condition(clauses)
  }

  /**
   * Reads macros saved by [toJson]. Settings can be imported from a file, so values are kept
   * within their limits, steps of unknown types are left out, and a macro that can't be read is
   * skipped.
   */
  internal fun fromJson(json: String?): List<Macro> {
    if (json.isNullOrEmpty()) return emptyList()
    val array =
      try {
        JSONArray(json)
      } catch (e: JSONException) {
        return emptyList()
      }
    val ids = HashSet<String>()
    return (0 until array.length()).mapNotNull { index ->
      try {
        val item = array.getJSONObject(index)
        val id = item.getString("id")
        if (id.isEmpty() || !ids.add(id)) return@mapNotNull null
        val steps = item.optJSONArray("steps") ?: JSONArray()
        Macro(
          id,
          item.getString("name"),
          (0 until minOf(steps.length(), MAX_STEPS)).mapNotNull { stepFromJson(steps.optJSONObject(it)) },
          conditionFromJson(item.optJSONArray("condition")),
        )
      } catch (e: JSONException) {
        null
      }
    }
  }

  private fun stepFromJson(item: JSONObject?): Step? {
    if (item == null) return null
    return when (item.optString("type")) {
      TYPE_COMMAND -> item.optString("command").takeIf { it.isNotEmpty() }?.let { RunCommand(it) }
      TYPE_WAIT -> Wait(item.optLong("ms", 0L).coerceIn(0L, MAX_WAIT_MS))
      TYPE_TAP ->
        item.optString("label").takeIf { it.isNotBlank() }?.let {
          Tap(
            it,
            item.optLong("timeout", DEFAULT_TAP_TIMEOUT_MS).coerceIn(MIN_TAP_TIMEOUT_MS, MAX_TAP_TIMEOUT_MS),
          )
        }
      TYPE_OPEN_APP -> item.optString("app").takeIf { OpenAppActions.isAction(it) }?.let { OpenApp(it) }
      TYPE_TYPE_TEXT ->
        item.optString("text").takeIf { it.isNotEmpty() }?.let {
          TypeText(it.take(MAX_TEXT), item.optBoolean("replace", false))
        }
      TYPE_IF -> If(conditionFromJson(item.optJSONArray("condition")))
      TYPE_ELSE_IF -> ElseIf(conditionFromJson(item.optJSONArray("condition")))
      TYPE_ELSE -> Else
      TYPE_END_IF -> EndIf
      TYPE_REPEAT -> RepeatTimes(item.optInt("count", 1).coerceIn(1, MAX_REPEATS))
      TYPE_REPEAT_WHILE -> RepeatWhile(conditionFromJson(item.optJSONArray("condition")))
      TYPE_END_REPEAT -> EndRepeat
      TYPE_EXIT_LOOP -> ExitLoop
      TYPE_STOP -> StopMacro
      TYPE_WAIT_UNTIL ->
        WaitUntil(
          conditionFromJson(item.optJSONArray("condition")),
          item.optLong("timeout", DEFAULT_WAIT_UNTIL_MS).coerceIn(MIN_TAP_TIMEOUT_MS, MAX_WAIT_MS),
        )
      else -> null
    }
  }
}
