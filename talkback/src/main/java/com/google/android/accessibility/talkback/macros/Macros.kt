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
 * Macros: named lists of steps that run one after another, each macro a command.
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

  /** A macro. [id] is unique among the macros. */
  data class Macro(val id: String, val name: String, val steps: List<Step>)

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

  /** The most characters of text a step types. */
  const val MAX_TEXT = 10_000

  /** Starts the value of a macro's command. It must never change once released. */
  private const val COMMAND_PREFIX = "MACRO:"

  private const val TYPE_COMMAND = "command"
  private const val TYPE_WAIT = "wait"
  private const val TYPE_TAP = "tap"
  private const val TYPE_OPEN_APP = "open_app"
  private const val TYPE_TYPE_TEXT = "type_text"

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
    }

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
      else -> null
    }
  }
}
