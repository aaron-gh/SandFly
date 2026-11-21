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
import java.util.regex.Pattern
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
   * Clicks the first item on screen whose text or description, or only its [field] when that isn't
   * null, contains [label], ignoring case, or matches it as a regular expression for [regex],
   * waiting up to [timeoutMillis] for it to appear.
   */
  data class Tap(
    val label: String,
    val timeoutMillis: Long = DEFAULT_TAP_TIMEOUT_MS,
    val regex: Boolean = false,
    val field: Field? = null,
  ) : Step()

  /**
   * Taps the screen at [x] and [y], in pixels from the top left of the screen. The step fails when
   * the position is outside the screen.
   */
  data class TapPosition(val x: Int, val y: Int) : Step()

  /**
   * Touches the item a [Tap] with the same values would click, at the centre of its bounds, for
   * [holdMillis], then slides [slideDp] towards [slide] when that isn't null, and lifts the touch.
   * Waits up to [timeoutMillis] for the item to appear. For buttons that act while they are held,
   * such as the voice message button of a chat app.
   */
  data class TouchHold(
    val label: String,
    val holdMillis: Long = DEFAULT_HOLD_MS,
    val slide: Direction? = null,
    val slideDp: Int = DEFAULT_SLIDE_DP,
    val timeoutMillis: Long = DEFAULT_TAP_TIMEOUT_MS,
    val regex: Boolean = false,
    val field: Field? = null,
  ) : Step()

  /**
   * Moves accessibility focus to the item a [Tap] with the same values would click, which is then
   * spoken, waiting up to [timeoutMillis] for it to appear.
   */
  data class FocusItem(
    val label: String,
    val timeoutMillis: Long = DEFAULT_TAP_TIMEOUT_MS,
    val regex: Boolean = false,
    val field: Field? = null,
  ) : Step()

  /**
   * Speaks the name of the item a [Tap] with the same values would click, waiting up to
   * [timeoutMillis] for it to appear, without moving accessibility focus.
   */
  data class SpeakItem(
    val label: String,
    val timeoutMillis: Long = DEFAULT_TAP_TIMEOUT_MS,
    val regex: Boolean = false,
    val field: Field? = null,
  ) : Step()

  /** Speaks [text]. */
  data class SpeakText(val text: String) : Step()

  /** What an [ActOnItem] step does to its item. [key] is saved and must never change. */
  enum class ItemAct(val key: String, @StringRes val title: Int, val action: Int) {
    LONG_PRESS("long_press", R.string.macro_act_long_press, android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK),
    SCROLL_FORWARD("scroll_forward", R.string.macro_act_scroll_forward, android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD),
    SCROLL_BACKWARD("scroll_backward", R.string.macro_act_scroll_backward, android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);

    companion object {
      fun of(key: String?): ItemAct? = entries.firstOrNull { it.key == key }
    }
  }

  /**
   * Long-presses or scrolls, [times] times, the item a [Tap] with the same values would click, or
   * the nearest item around it that can be, such as a seek bar holding a time. A scroll with an
   * empty [label] scrolls the largest list on screen.
   */
  data class ActOnItem(
    val label: String,
    val act: ItemAct,
    val times: Int = 1,
    val timeoutMillis: Long = DEFAULT_TAP_TIMEOUT_MS,
    val regex: Boolean = false,
    val field: Field? = null,
  ) : Step()

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

  /**
   * A step that acts outside the items on screen, such as playing a sound, copying text or
   * opening a link, or that keeps a value. What [text], [name] and [number] hold depends on
   * [kind]. Texts can hold values, as {name}.
   */
  data class Do(val kind: DoKind, val text: String = "", val name: String = "", val number: Int = 0) : Step()

  /** What a [Do] step does. [key] is saved and must never change once released. */
  enum class DoKind(val key: String, @StringRes val title: Int) {
    PLAY_SOUND("play_sound", R.string.macro_do_play_sound),
    VIBRATE("vibrate", R.string.macro_do_vibrate),
    PLAY_TONE("play_tone", R.string.macro_do_play_tone),
    COPY_TEXT("copy_text", R.string.macro_do_copy_text),
    OPEN_LINK("open_link", R.string.macro_do_open_link),
    OPEN_SETTINGS("open_settings", R.string.macro_do_open_settings),
    SHARE_TEXT("share_text", R.string.macro_do_share_text),
    SET_VOLUME("set_volume", R.string.macro_do_set_volume),
    SET_RINGER("set_ringer", R.string.macro_do_set_ringer),
    PRESS_KEYS("press_keys", R.string.macro_do_press_keys),
    SHOW_MESSAGE("show_message", R.string.macro_do_show_message),
    SET_VALUE("set_value", R.string.macro_do_set_value),
    FETCH("fetch", R.string.macro_do_fetch),
    TRANSLATE("translate", R.string.macro_do_translate),
    /** [Do.name] is a control of [DeviceActions.CONTROLS], [Do.text] its new value. */
    DEVICE_CONTROL("device_control", R.string.macro_do_device_control),
    /** [Do.text] is a key of [DeviceActions.MEDIA_KEYS]. */
    MEDIA("media", R.string.macro_do_media),
    /** [Do.number] is seconds, [Do.text] the timer's label. */
    SET_TIMER("set_timer", R.string.macro_do_set_timer),
    /** [Do.number] is minutes after midnight, [Do.text] the alarm's label. */
    SET_ALARM("set_alarm", R.string.macro_do_set_alarm),
    /** [Do.text] is the number. */
    CALL("call", R.string.macro_do_call),
    /** [Do.name] is the number, [Do.text] the message. */
    SEND_MESSAGE("send_message", R.string.macro_do_send_message),
    /** [Do.text] is photo or video. */
    OPEN_CAMERA("open_camera", R.string.macro_do_open_camera);

    /** Whether the step keeps its result as the value [Do.name]. */
    val setsValue: Boolean
      get() = this == SET_VALUE || this == FETCH || this == TRANSLATE

    companion object {
      fun of(key: String?): DoKind? = entries.firstOrNull { it.key == key }
    }
  }

  /** SandFly's sounds a Play sound step plays, by the names scripts give them. */
  val SOUNDS = listOf("focus", "actionable", "click", "longClick", "scroll", "listEnter", "listExit", "end", "enter", "typo", "beep")

  /** The streams a Set volume step changes, by the names scripts give them. */
  val VOLUME_STREAMS = listOf("media", "ring", "notification", "alarm", "call", "system", "accessibility")

  /** The ringer modes of a Set ringer mode step. */
  val RINGER_MODES = listOf("normal", "vibrate", "silent")

  /** The pages of Android's settings an Open settings page step opens, by the names scripts give them. */
  val SETTINGS_PAGES =
    listOf(
      "settings", "wifi", "bluetooth", "mobileData", "airplaneMode", "nfc", "location", "sound", "display",
      "battery", "batterySaver", "accessibility", "apps", "defaultApps", "notifications", "doNotDisturb",
      "dateTime", "language", "keyboard", "storage", "security", "privacy", "developer", "about", "castScreen",
      "hotspot",
    )

  /**
   * Fills the values in [text]: each {name} becomes the value [values] has for it, and names
   * without a value are left as they are.
   */
  @JvmStatic
  fun fill(text: String, values: Map<String, String>): String =
    if ('{' !in text) text
    else VALUE_PATTERN.matcher(text).let { matcher ->
      val out = StringBuffer()
      while (matcher.find()) {
        val value = values[matcher.group(1)] ?: matcher.group(0)
        matcher.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(value))
      }
      matcher.appendTail(out)
      out.toString().take(MAX_TEXT)
    }

  private val VALUE_PATTERN: Pattern = Pattern.compile("\\{([A-Za-z0-9_.\\-]{1,64})\\}")

  /** The most values a set of macros keeps, and characters in each. */
  const val MAX_VALUES = 200
  const val MAX_VALUE_CHARS = 10_000

  private const val KEY_VALUES = "pref_macro_values"

  /** The values kept for the macros of [scope]: the user's own macros, or one script's. */
  @JvmStatic
  fun values(prefs: SharedPreferences, scope: String): Map<String, String> {
    val all = prefs.getString(KEY_VALUES, null)?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return emptyMap()
    val obj = all.optJSONObject(scope) ?: return emptyMap()
    return obj.keys().asSequence().associateWith { obj.optString(it) }
  }

  /** Keeps [value] as [name] for the macros of [scope], or forgets it when it is null. */
  @JvmStatic
  fun setValue(prefs: SharedPreferences, scope: String, name: String, value: String?) {
    val all = prefs.getString(KEY_VALUES, null)?.let { runCatching { JSONObject(it) }.getOrNull() } ?: JSONObject()
    val obj = all.optJSONObject(scope) ?: JSONObject().also { all.put(scope, it) }
    if (value == null) obj.remove(name)
    else if (obj.has(name) || obj.length() < MAX_VALUES) obj.put(name, value.take(MAX_VALUE_CHARS))
    prefs.edit { putString(KEY_VALUES, all.toString()) }
  }

  /** Forgets the values kept for the macros of [scope]. */
  @JvmStatic
  fun clearValues(prefs: SharedPreferences, scope: String) {
    val all = prefs.getString(KEY_VALUES, null)?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return
    all.remove(scope)
    prefs.edit { putString(KEY_VALUES, all.toString()) }
  }

  /** The scope of the values of the macro [id]: its script, or the user's own macros. */
  @JvmStatic
  fun valueScope(id: String): String =
    if (isExternal(id)) id.removePrefix(EXTERNAL_PREFIX).substringBefore(':').let { EXTERNAL_PREFIX + it } else USER_SCOPE

  const val USER_SCOPE = "user"

  /**
   * Steps that need more than SandFly's own screen code: Android's settings pages, volume, keys
   * through Shizuku, messages, fetching and translating. The phone build sets them up when they
   * are first needed. [done] gets whether the step worked, and the value it gives, if any.
   */
  interface DeviceSteps {
    fun run(service: android.accessibilityservice.AccessibilityService, step: Do, text: String, done: (Boolean, String?) -> Unit)
  }

  @Volatile private var deviceStepsHolder: DeviceSteps? = null

  @JvmStatic
  fun deviceSteps(): DeviceSteps? {
    deviceStepsHolder?.let { return it }
    return try {
      (Class.forName(DEVICE_STEPS_CLASS).getField("INSTANCE").get(null) as? DeviceSteps)?.also { deviceStepsHolder = it }
    } catch (e: ReflectiveOperationException) {
      null
    }
  }

  private const val DEVICE_STEPS_CLASS = "com.google.android.accessibility.talkback.scripting.MacroDeviceSteps"

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

    /** The name scripts give it, such as longClickable. */
    val scriptName: String
      get() = key.split('_').mapIndexed { i, part -> if (i == 0) part else part.replaceFirstChar(Char::uppercase) }.joinToString("")

    companion object {
      fun of(key: String?): Kind? = entries.firstOrNull { it.key == key }
    }
  }

  /**
   * A direction a [TouchHold] slides in, by [dx] and [dy] on the screen. [key] is saved and must
   * never change once released.
   */
  enum class Direction(val key: String, @StringRes val title: Int, val dx: Int, val dy: Int) {
    UP("up", R.string.macro_slide_up, 0, -1),
    DOWN("down", R.string.macro_slide_down, 0, 1),
    LEFT("left", R.string.macro_slide_left, -1, 0),
    RIGHT("right", R.string.macro_slide_right, 1, 0);

    companion object {
      fun of(key: String?): Direction? = entries.firstOrNull { it.key == key }
    }
  }

  /** The one text of an item that a macro step or test looks in, instead of all of them. */
  enum class Field(val key: String, @StringRes val title: Int) {
    TEXT("text", R.string.macro_field_text),
    DESCRIPTION("description", R.string.macro_field_description),
    HINT("hint", R.string.macro_field_hint),
    STATE("state", R.string.macro_field_state),
    TOOLTIP("tooltip", R.string.macro_field_tooltip),
    RESOURCE_ID("id", R.string.macro_field_id);

    companion object {
      fun of(key: String?): Field? = entries.firstOrNull { it.key == key }
    }
  }

  /** What a condition checks. */
  sealed class Test

  /**
   * An item on screen whose text, description, hint, state or tooltip, or only its [field] when that isn't
   * null, contains [label], ignoring case, or matches it as a regular expression for [regex], and
   * that is of [kind] when it isn't null.
   */
  data class ItemOnScreen(
    val label: String,
    val kind: Kind? = null,
    val regex: Boolean = false,
    val field: Field? = null,
  ) : Test()

  /**
   * The item with accessibility focus: its texts, or those of the items in it, or only their
   * [field] when that isn't null, contain [label] when it isn't empty, or match it as a regular
   * expression for [regex], and it is of [kind] when it isn't null.
   */
  data class FocusedItem(
    val label: String,
    val kind: Kind? = null,
    val regex: Boolean = false,
    val field: Field? = null,
  ) : Test()

  /**
   * The value [name] contains [label], ignoring case, or matches it as a regular expression for
   * [regex]; or, when [label] is empty, the value isn't empty.
   */
  data class ValueIs(val name: String, val label: String, val regex: Boolean = false) : Test()

  /** The app of the active window is [packageName]. */
  data class AppInFront(val packageName: String) : Test()

  /**
   * The item with accessibility focus is inside an item, or is one, whose own text, description,
   * hint or state, or only its [field], contains [label] or matches it for [regex]: such as a video
   * player, found by its resource ID.
   */
  data class FocusInside(val label: String, val regex: Boolean = false, val field: Field? = null) : Test()

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

  /**
   * A macro. [id] is unique among the macros. It only runs when [condition] is met, and when [app]
   * isn't null, only while that app is in front. [inActions] lists it in the Actions menu of the
   * items of [app], or of every app when [app] is null, while [actionsCondition] is met for the
   * focused item. [shortcutToken] is the token of its home screen shortcut, which [MacroShortcuts]
   * checks, or null when it has none.
   */
  data class Macro(
    val id: String,
    val name: String,
    val steps: List<Step>,
    val condition: Condition = Condition(),
    val app: String? = null,
    val inActions: Boolean = false,
    val shortcutToken: String? = null,
    val actionsCondition: Condition = Condition(),
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

  /**
   * The value of the command that records a new macro, or ends the recording. It must never change
   * once released.
   */
  const val RECORD_COMMAND = "RECORD_MACRO"

  const val KEY = "pref_macros"

  /** The most steps a macro keeps. */
  const val MAX_STEPS = 200

  const val DEFAULT_TAP_TIMEOUT_MS = 3_000L
  const val MIN_TAP_TIMEOUT_MS = 500L
  const val MAX_TAP_TIMEOUT_MS = 60_000L

  /** The longest wait, 10 minutes. */
  const val MAX_WAIT_MS = 600_000L

  /** The largest number a [Do] step keeps, such as milliseconds of vibration or a tone's hertz. */
  const val MAX_DO_NUMBER = 20_000

  const val DEFAULT_WAIT_UNTIL_MS = 10_000L

  /** The most times a repeat runs its steps in one run of a macro. */
  const val MAX_REPEATS = 1_000

  /** The most tests a condition keeps. */
  const val MAX_CLAUSES = 20

  /** The most characters of the text a test looks for. */
  const val MAX_LABEL = 500

  /** The most characters of an item's text a regular expression is matched against. */
  const val MAX_PATTERN_INPUT = 1_000

  /** The most characters of text a step types or speaks. */
  const val MAX_TEXT = 10_000

  /** How long a Touch and hold step holds by default, and at least and at most. */
  const val DEFAULT_HOLD_MS = 1_000L
  const val MIN_HOLD_MS = 100L
  const val MAX_HOLD_MS = MAX_WAIT_MS

  /** How far a Touch and hold step slides by default, and at least and at most, in dp. */
  const val DEFAULT_SLIDE_DP = 100
  const val MIN_SLIDE_DP = 1
  const val MAX_SLIDE_DP = 1_000

  /** The largest X or Y of a Tap at position step, in pixels. */
  const val MAX_COORDINATE = 10_000

  /** Starts the value of a macro's command. It must never change once released. */
  private const val COMMAND_PREFIX = "MACRO:"

  private const val TYPE_COMMAND = "command"
  private const val TYPE_WAIT = "wait"
  private const val TYPE_TAP = "tap"
  private const val TYPE_TAP_POSITION = "tap_position"
  private const val TYPE_TOUCH_HOLD = "touch_hold"
  private const val TYPE_FOCUS_ITEM = "focus_item"
  private const val TYPE_ACT_ITEM = "act_item"
  private const val TYPE_SPEAK_ITEM = "speak_item"
  private const val TYPE_SPEAK_TEXT = "speak_text"
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
  private const val TEST_FOCUS_INSIDE = "focus_inside"
  private const val TEST_APP = "app"
  private const val TEST_KEYBOARD = "keyboard"
  private const val TEST_LOCKED = "locked"
  private const val TEST_VALUE = "value"
  private const val TYPE_DO = "do"

  /** What a value can be named: letters, digits, dots, dashes and underscores. */
  val VALUE_NAME = Regex("^[A-Za-z0-9_.\\-]{1,64}$")

  @JvmStatic fun newId(): String = UUID.randomUUID().toString()

  /** Returns the value of the command that runs the macro [id]. */
  @JvmStatic fun commandOf(id: String): String = COMMAND_PREFIX + id

  /** Returns whether [value] is the command of a macro, [STOP_COMMAND] or [RECORD_COMMAND]. */
  @JvmStatic
  fun isCommand(value: String?): Boolean =
    value != null && (value.startsWith(COMMAND_PREFIX) || value == STOP_COMMAND || value == RECORD_COMMAND)

  /** Returns the ID of the macro whose command is [value], or null for other commands. */
  @JvmStatic
  fun idOf(value: String?): String? =
    value?.takeIf { it.startsWith(COMMAND_PREFIX) }?.substring(COMMAND_PREFIX.length)

  /** Returns the name of the command [value], a macro's or [STOP_COMMAND], or null for others. */
  @JvmStatic
  fun label(context: Context, prefs: SharedPreferences, value: String?): String? {
    if (value == STOP_COMMAND) return context.getString(R.string.shortcut_stop_macro)
    if (value == RECORD_COMMAND) return context.getString(R.string.macro_record_new)
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
    return fromJson(json, moveAppTests = true).also { parsed = json to it }
  }

  /**
   * Returns the macros listed in the Actions menu of an item of the app [packageName]: those of the
   * app, and those of all apps, in their order.
   */
  @JvmStatic
  fun inActionsOf(prefs: SharedPreferences, packageName: CharSequence?): List<Macro> {
    val macros = read(prefs)
    if (macros.none { it.inActions }) return emptyList()
    val app = packageName?.toString()
    return macros.filter { it.inActions && (it.app == null || it.app == app) }
  }

  @JvmStatic
  fun write(prefs: SharedPreferences, macros: List<Macro>) {
    prefs.edit { putString(KEY, toJson(macros)) }
  }

  /**
   * Macros kept somewhere other than the settings, such as the commands of a script made in the
   * script builder, by ids that start with [EXTERNAL_PREFIX]. The macro editor edits them as it
   * edits the user's own macros.
   */
  interface ExternalMacros {
    fun find(id: String): Macro?

    fun update(macro: Macro)

    fun delete(id: String)

    /** Whether the macro [id] has a name the user can change, such as a command's title. */
    fun canRename(id: String): Boolean = true

    /**
     * The screen that assigns the macro [id] to gestures and keys, as its fragment's class name
     * and arguments, or null when it has none.
     */
    fun assignScreen(id: String): Pair<String, android.os.Bundle>? = null
  }

  /** The screen that assigns the external macro [id], or null. */
  @JvmStatic fun assignScreen(id: String): Pair<String, android.os.Bundle>? = if (isExternal(id)) external()?.assignScreen(id) else null

  /** Whether the macro [id] has a name the user can change. */
  @JvmStatic fun canRename(id: String): Boolean = !isExternal(id) || external()?.canRename(id) == true

  const val EXTERNAL_PREFIX = "script:"

  @JvmStatic fun isExternal(id: String): Boolean = id.startsWith(EXTERNAL_PREFIX)

  /** The holder of external macros, which the phone build sets up when it is first needed. */
  @Volatile private var externalMacros: ExternalMacros? = null

  private fun external(): ExternalMacros? {
    externalMacros?.let { return it }
    return try {
      (Class.forName(EXTERNAL_CLASS).getField("INSTANCE").get(null) as? ExternalMacros)?.also { externalMacros = it }
    } catch (e: ReflectiveOperationException) {
      null
    }
  }

  private const val EXTERNAL_CLASS = "com.google.android.accessibility.talkback.scripting.BuiltScriptMacros"

  /** Returns the macro [id], or null. */
  @JvmStatic
  fun find(prefs: SharedPreferences, id: String): Macro? =
    if (isExternal(id)) external()?.find(id) else read(prefs).firstOrNull { it.id == id }

  /** Adds a macro named [name] with no steps, for the app [app] or all apps, and returns it. */
  fun add(prefs: SharedPreferences, name: String, app: String? = null): Macro {
    val macro = Macro(newId(), name, emptyList(), app = app)
    write(prefs, read(prefs) + macro)
    return macro
  }

  /** Saves [macro] in place of the macro with its ID. */
  fun update(prefs: SharedPreferences, macro: Macro) {
    if (isExternal(macro.id)) {
      external()?.update(macro)
      return
    }
    write(prefs, read(prefs).map { if (it.id == macro.id) macro else it })
  }

  /**
   * Deletes the macro [id], and takes its command off the gestures, keys, menus and other settings
   * that run it.
   */
  fun delete(context: Context, prefs: SharedPreferences, id: String) {
    if (isExternal(id)) {
      external()?.delete(id)
      return
    }
    val deleted = read(prefs).firstOrNull { it.id == id }
    write(prefs, read(prefs).filter { it.id != id })
    CommandAssignments.unassign(context, prefs, commandOf(id))
    if (deleted?.shortcutToken != null) MacroShortcuts.disable(context, id)
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

  /**
   * Returns [text] as the X or Y of a Tap at position step, or null when it isn't a whole number
   * from 0 to [MAX_COORDINATE].
   */
  @JvmStatic
  fun parseCoordinate(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in 0..MAX_COORDINATE }

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
      is Tap -> context.getString(R.string.macro_step_tap_value, labelText(context, step.label, step.regex, step.field))
      is TapPosition -> context.getString(R.string.macro_step_tap_position_value, step.x, step.y)
      is TouchHold -> {
        val label = labelText(context, step.label, step.regex, step.field)
        val seconds = secondsText(step.holdMillis)
        step.slide?.let {
          context.getString(
            R.string.macro_step_touch_hold_slide_value,
            label,
            seconds,
            context.getString(it.title),
            step.slideDp,
          )
        } ?: context.getString(R.string.macro_step_touch_hold_value, label, seconds)
      }
      is FocusItem ->
        context.getString(R.string.macro_step_focus_value, labelText(context, step.label, step.regex, step.field))
      is ActOnItem -> {
        val item =
          if (step.label.isEmpty()) context.getString(R.string.macro_list_on_screen)
          else labelText(context, step.label, step.regex, step.field)
        if (step.times > 1) {
          context.getString(R.string.macro_step_act_times_value, context.getString(step.act.title), item, step.times)
        } else {
          context.getString(R.string.macro_step_act_value, context.getString(step.act.title), item)
        }
      }
      is SpeakItem ->
        context.getString(R.string.macro_step_speak_item_value, labelText(context, step.label, step.regex, step.field))
      is SpeakText -> context.getString(R.string.macro_step_speak_text_value, step.text)
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
      is Do -> doText(context, step)
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
          itemText(context, labelText(context, test.label, test.regex, test.field), test.kind),
        )
      is FocusedItem ->
        context.getString(
          if (not) R.string.macro_test_not_focused_value else R.string.macro_test_focused_value,
          itemText(context, labelText(context, test.label, test.regex, test.field), test.kind),
        )
      is FocusInside ->
        context.getString(
          if (not) R.string.macro_test_not_focus_inside_value else R.string.macro_test_focus_inside_value,
          labelText(context, test.label, test.regex, test.field),
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
      is ValueIs -> {
        val what =
          if (test.label.isEmpty()) context.getString(R.string.macro_test_value_set, test.name)
          else context.getString(R.string.macro_test_value_value, test.name, labelText(context, test.label, test.regex, null))
        if (not) context.getString(R.string.macro_test_not, what) else what
      }
    }
  }

  /** The texts [step] holds, joined, as saved: for finding the values it uses. */
  @JvmStatic
  fun describeRaw(step: Step): String =
    when (step) {
      is SpeakText -> step.text
      is TypeText -> step.text
      is Do -> step.text + " " + step.name
      else -> ""
    }

  /** Returns what the [Do] step [step] does, with its values. */
  private fun doText(context: Context, step: Do): String =
    when (step.kind) {
      DoKind.VIBRATE -> context.getString(R.string.macro_do_vibrate_value, step.number)
      DoKind.PLAY_TONE -> context.getString(R.string.macro_do_play_tone_value, step.number)
      DoKind.SET_VOLUME -> context.getString(R.string.macro_do_set_volume_value, step.name, step.number)
      DoKind.DEVICE_CONTROL -> {
        val control = DeviceActions.CONTROLS[step.name]
        val value =
          when (step.text) {
            "on" -> context.getString(R.string.macro_do_turn_on)
            "off" -> context.getString(R.string.macro_do_turn_off)
            "toggle" -> context.getString(R.string.macro_do_toggle)
            else -> step.text
          }
        context.getString(R.string.macro_do_device_control_value, control?.let { context.getString(it.title) } ?: step.name, value)
      }
      DoKind.SET_TIMER -> {
        val seconds = secondsText(step.number * 1000L)
        if (step.text.isBlank()) context.getString(R.string.macro_do_set_timer_short, seconds)
        else context.getString(R.string.macro_do_set_timer_value, seconds, step.text)
      }
      DoKind.SET_ALARM -> {
        val time = "%02d:%02d".format(step.number / 60, step.number % 60)
        if (step.text.isBlank()) context.getString(R.string.macro_do_set_alarm_short, time)
        else context.getString(R.string.macro_do_set_alarm_value, time, step.text)
      }
      DoKind.SEND_MESSAGE -> context.getString(R.string.macro_do_send_message_value, step.name, step.text)
      DoKind.SET_VALUE, DoKind.FETCH, DoKind.TRANSLATE ->
        context.getString(R.string.macro_do_into_value, context.getString(step.kind.title), step.text, step.name)
      else -> context.getString(R.string.macro_do_value, context.getString(step.kind.title), step.text)
    }

  /** Returns [label], with the [field] it is looked in, and said to be a regular expression for [regex]. */
  private fun labelText(context: Context, label: String, regex: Boolean, field: Field?): String =
    when {
      label.isEmpty() -> label
      field != null && regex ->
        context.getString(R.string.macro_field_regex_value, label, context.getString(field.title))
      field != null -> context.getString(R.string.macro_field_value, label, context.getString(field.title))
      regex -> context.getString(R.string.macro_regex_value, label)
      else -> label
    }

  /**
   * Returns [pattern] compiled to be found in a text ignoring case, or null when it isn't a regular
   * expression or repeats a group with repetition or alternatives in it. Java's regular expressions
   * can take exponential time to match such a group, and a match can't be stopped once it starts.
   */
  @JvmStatic
  fun compilePattern(pattern: String): Pattern? {
    if (pattern.isEmpty() || repeatsComplexGroup(pattern)) return null
    return try {
      Pattern.compile(pattern, Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE)
    } catch (e: IllegalArgumentException) {
      null
    }
  }

  /** Returns whether [pattern] repeats a group that has repetition or alternatives in it. */
  internal fun repeatsComplexGroup(pattern: String): Boolean {
    // For each open group, whether it has repetition or alternatives in it.
    val groups = ArrayList<Boolean>()
    fun markComplex() {
      if (groups.isNotEmpty()) groups[groups.size - 1] = true
    }
    var i = 0
    while (i < pattern.length) {
      when (pattern[i]) {
        '\\' -> {
          if (pattern.startsWith("Q", i + 1)) {
            val end = pattern.indexOf("\\E", i + 2)
            i = if (end < 0) pattern.length else end + 2
          } else {
            i += 2
          }
        }
        '[' -> i = endOfClass(pattern, i)
        '(' -> {
          groups.add(false)
          i = afterGroupStart(pattern, i + 1)
        }
        ')' -> {
          val complex = if (groups.isEmpty()) false else groups.removeAt(groups.size - 1)
          i++
          val repeated = repeatEnd(pattern, i) > i
          if (repeated && complex) return true
          if (complex || repeated) markComplex()
        }
        '|' -> {
          markComplex()
          i++
        }
        else -> {
          val end = repeatEnd(pattern, i)
          if (end > i) {
            markComplex()
            i = end
          } else {
            i++
          }
        }
      }
    }
    return false
  }

  /**
   * Returns the end of a quantifier that repeats more than once at [start] of [pattern]: *, + or
   * {n,} or {n,m} with m above 1. Returns [start] when there isn't one.
   */
  private fun repeatEnd(pattern: String, start: Int): Int {
    if (start >= pattern.length) return start
    return when (pattern[start]) {
      '*', '+' -> start + 1
      '{' -> {
        val close = pattern.indexOf('}', start)
        if (close < 0) return start
        val bounds = pattern.substring(start + 1, close).split(',')
        val repeats =
          when (bounds.size) {
            1 -> (bounds[0].trim().toIntOrNull() ?: 0) > 1
            2 -> bounds[1].isBlank() || (bounds[1].trim().toIntOrNull() ?: 0) > 1
            else -> false
          }
        if (repeats) close + 1 else start
      }
      else -> start
    }
  }

  /** Returns the index after the character class that opens at [start] of [pattern]. */
  private fun endOfClass(pattern: String, start: Int): Int {
    var i = start + 1
    // A ] first in the class, or after ^, is a character in it.
    if (pattern.startsWith("^", i)) i++
    if (pattern.startsWith("]", i)) i++
    var depth = 1
    while (i < pattern.length) {
      when (pattern[i]) {
        '\\' -> i++
        '[' -> depth++
        ']' -> if (--depth == 0) return i + 1
      }
      i++
    }
    return pattern.length
  }

  /** Returns the index after the (?:, (?=, (?<name> or flags that can start a group at [start]. */
  private fun afterGroupStart(pattern: String, start: Int): Int {
    if (!pattern.startsWith("?", start)) return start
    var i = start + 1
    if (i >= pattern.length) return i
    when (pattern[i]) {
      ':', '=', '!', '>' -> return i + 1
      '<' -> {
        if (pattern.startsWith("=", i + 1) || pattern.startsWith("!", i + 1)) return i + 2
        val close = pattern.indexOf('>', i)
        return if (close < 0) pattern.length else close + 1
      }
    }
    // Flags, such as (?i) or (?i:.
    while (i < pattern.length && (pattern[i].isLetter() || pattern[i] == '-')) i++
    return if (pattern.startsWith(":", i)) i + 1 else i
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

  /** [macro] as JSON, as a file of macros holds it. */
  fun macroToJson(macro: Macro): String = toJson(listOf(macro), shortcuts = false)

  /** The first macro in [json], made by [macroToJson], or null. */
  fun macroFromJson(json: String?): Macro? = fromJson(json).firstOrNull()

  /** [macros] as JSON; without [shortcuts], without the tokens of their home screen shortcuts. */
  internal fun toJson(macros: List<Macro>, shortcuts: Boolean = true): String =
    JSONArray()
      .apply {
        for (macro in macros) {
          put(
            JSONObject()
              .put("id", macro.id)
              .put("name", macro.name)
              .put("steps", JSONArray().apply { macro.steps.forEach { put(stepToJson(it)) } })
              .apply { if (macro.condition.clauses.isNotEmpty()) put("condition", conditionToJson(macro.condition)) }
              .apply { macro.app?.let { put("app", it) } }
              .apply { if (macro.inActions) put("actions", true) }
              .apply {
                if (macro.actionsCondition.clauses.isNotEmpty()) {
                  put("actionsCondition", conditionToJson(macro.actionsCondition))
                }
              }
              .apply { if (shortcuts) macro.shortcutToken?.let { put("shortcut", it) } }
          )
        }
      }
      .toString()

  private fun stepToJson(step: Step): JSONObject =
    when (step) {
      is RunCommand -> JSONObject().put("type", TYPE_COMMAND).put("command", step.value)
      is Wait -> JSONObject().put("type", TYPE_WAIT).put("ms", step.millis)
      is Tap ->
        JSONObject()
          .put("type", TYPE_TAP)
          .put("label", step.label)
          .put("timeout", step.timeoutMillis)
          .apply { if (step.regex) put("regex", true) }
          .apply { step.field?.let { put("field", it.key) } }
      is TapPosition -> JSONObject().put("type", TYPE_TAP_POSITION).put("x", step.x).put("y", step.y)
      is TouchHold ->
        JSONObject()
          .put("type", TYPE_TOUCH_HOLD)
          .put("label", step.label)
          .put("hold", step.holdMillis)
          .apply { step.slide?.let { put("slide", it.key).put("distance", step.slideDp) } }
          .put("timeout", step.timeoutMillis)
          .apply { if (step.regex) put("regex", true) }
          .apply { step.field?.let { put("field", it.key) } }
      is ActOnItem ->
        JSONObject()
          .put("type", TYPE_ACT_ITEM)
          .put("label", step.label)
          .put("act", step.act.key)
          .put("times", step.times)
          .put("timeout", step.timeoutMillis)
          .apply { if (step.regex) put("regex", true) }
          .apply { step.field?.let { put("field", it.key) } }
      is FocusItem ->
        JSONObject()
          .put("type", TYPE_FOCUS_ITEM)
          .put("label", step.label)
          .put("timeout", step.timeoutMillis)
          .apply { if (step.regex) put("regex", true) }
          .apply { step.field?.let { put("field", it.key) } }
      is SpeakItem ->
        JSONObject()
          .put("type", TYPE_SPEAK_ITEM)
          .put("label", step.label)
          .put("timeout", step.timeoutMillis)
          .apply { if (step.regex) put("regex", true) }
          .apply { step.field?.let { put("field", it.key) } }
      is SpeakText -> JSONObject().put("type", TYPE_SPEAK_TEXT).put("text", step.text)
      is Do ->
        JSONObject()
          .put("type", TYPE_DO)
          .put("do", step.kind.key)
          .apply { if (step.text.isNotEmpty()) put("text", step.text) }
          .apply { if (step.name.isNotEmpty()) put("name", step.name) }
          .apply { if (step.number != 0) put("number", step.number) }
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
              JSONObject()
                .put("test", TEST_ITEM)
                .put("label", test.label)
                .put("kind", test.kind?.key)
                .apply { if (test.regex) put("regex", true) }
                .apply { test.field?.let { put("field", it.key) } }
            is FocusedItem ->
              JSONObject()
                .put("test", TEST_FOCUSED)
                .put("label", test.label)
                .put("kind", test.kind?.key)
                .apply { if (test.regex) put("regex", true) }
                .apply { test.field?.let { put("field", it.key) } }
            is FocusInside ->
              JSONObject()
                .put("test", TEST_FOCUS_INSIDE)
                .put("label", test.label)
                .apply { if (test.regex) put("regex", true) }
                .apply { test.field?.let { put("field", it.key) } }
            is AppInFront -> JSONObject().put("test", TEST_APP).put("package", test.packageName)
            KeyboardShown -> JSONObject().put("test", TEST_KEYBOARD)
            ScreenLocked -> JSONObject().put("test", TEST_LOCKED)
            is ValueIs ->
              JSONObject()
                .put("test", TEST_VALUE)
                .put("name", test.name)
                .put("label", test.label)
                .apply { if (test.regex) put("regex", true) }
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
                  ItemOnScreen(
                    it,
                    Kind.of(item.optString("kind")),
                    item.optBoolean("regex", false),
                    Field.of(item.optString("field")),
                  )
                }
              TEST_FOCUSED -> {
                val label = item.optString("label").take(MAX_LABEL)
                val kind = Kind.of(item.optString("kind"))
                if (label.isBlank() && kind == null) null
                else
                  FocusedItem(
                    label.trim(),
                    kind,
                    label.isNotBlank() && item.optBoolean("regex", false),
                    Field.of(item.optString("field")).takeIf { label.isNotBlank() },
                  )
              }
              TEST_FOCUS_INSIDE ->
                item.optString("label").take(MAX_LABEL).takeIf { it.isNotBlank() }?.let {
                  FocusInside(it, item.optBoolean("regex", false), Field.of(item.optString("field")))
                }
              TEST_APP -> item.optString("package").takeIf { it.isNotEmpty() }?.let { AppInFront(it) }
              TEST_KEYBOARD -> KeyboardShown
              TEST_LOCKED -> ScreenLocked
              TEST_VALUE ->
                item.optString("name").takeIf { VALUE_NAME.matches(it) }?.let {
                  val label = item.optString("label").take(MAX_LABEL).trim()
                  ValueIs(it, label, label.isNotEmpty() && item.optBoolean("regex", false))
                }
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
   * skipped. With [moveAppTests], a macro saved without an app, before macros had one, gets the app
   * of its condition's App in front test, as [withAppOfCondition] does.
   */
  internal fun fromJson(json: String?, moveAppTests: Boolean = false): List<Macro> {
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
        val macro =
          Macro(
            id,
            item.getString("name"),
            (0 until minOf(steps.length(), MAX_STEPS)).mapNotNull { stepFromJson(steps.optJSONObject(it)) },
            conditionFromJson(item.optJSONArray("condition")),
            item.optString("app").takeIf { isPackageName(it) },
            item.optBoolean("actions", false),
            item.optString("shortcut").takeIf { MacroShortcuts.isToken(it) },
            conditionFromJson(item.optJSONArray("actionsCondition")),
          )
        if (moveAppTests && !item.has("app")) withAppOfCondition(macro) else macro
      } catch (e: JSONException) {
        null
      }
    }
  }

  /**
   * Returns [macro] for the app of its condition's App in front test, without that test, when it
   * has no app and its condition needs that app: a test that isn't reversed, in a condition without
   * "or". Otherwise returns [macro] as it is. The macro runs when it did before.
   */
  @JvmStatic
  fun withAppOfCondition(macro: Macro): Macro {
    val clauses = macro.condition.clauses
    if (macro.app != null || clauses.any { it.or }) return macro
    val index = clauses.indexOfFirst { !it.not && it.test is AppInFront }
    if (index < 0) return macro
    val app = (clauses[index].test as AppInFront).packageName
    return macro.copy(condition = Condition(clauses.filterIndexed { i, _ -> i != index }), app = app)
  }

  /** Whether [text] can be the package name of an app. */
  @JvmStatic
  fun isPackageName(text: String): Boolean = text.length <= MAX_PACKAGE_NAME && PACKAGE_NAME.matches(text)

  private const val MAX_PACKAGE_NAME = 255
  private val PACKAGE_NAME = Regex("^[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)*$")

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
            item.optBoolean("regex", false),
            Field.of(item.optString("field")),
          )
        }
      TYPE_TAP_POSITION -> {
        val x = coordinateOf(item, "x")
        val y = coordinateOf(item, "y")
        if (x != null && y != null) TapPosition(x, y) else null
      }
      TYPE_TOUCH_HOLD ->
        item.optString("label").takeIf { it.isNotBlank() }?.let {
          TouchHold(
            it,
            item.optLong("hold", DEFAULT_HOLD_MS).coerceIn(MIN_HOLD_MS, MAX_HOLD_MS),
            Direction.of(item.optString("slide")),
            item.optInt("distance", DEFAULT_SLIDE_DP).coerceIn(MIN_SLIDE_DP, MAX_SLIDE_DP),
            item.optLong("timeout", DEFAULT_TAP_TIMEOUT_MS).coerceIn(MIN_TAP_TIMEOUT_MS, MAX_TAP_TIMEOUT_MS),
            item.optBoolean("regex", false),
            Field.of(item.optString("field")),
          )
        }
      TYPE_ACT_ITEM ->
        ItemAct.of(item.optString("act"))?.let { act ->
          // Without a label, a scroll step scrolls the largest list on screen.
          item.optString("label").trim().takeIf { it.isNotEmpty() || act != ItemAct.LONG_PRESS }?.let { label ->
            ActOnItem(
              label,
              act,
              item.optInt("times", 1).coerceIn(1, MAX_REPEATS),
              item.optLong("timeout", DEFAULT_TAP_TIMEOUT_MS).coerceIn(MIN_TAP_TIMEOUT_MS, MAX_TAP_TIMEOUT_MS),
              item.optBoolean("regex", false),
              Field.of(item.optString("field")),
            )
          }
        }
      TYPE_FOCUS_ITEM ->
        item.optString("label").takeIf { it.isNotBlank() }?.let {
          FocusItem(
            it,
            item.optLong("timeout", DEFAULT_TAP_TIMEOUT_MS).coerceIn(MIN_TAP_TIMEOUT_MS, MAX_TAP_TIMEOUT_MS),
            item.optBoolean("regex", false),
            Field.of(item.optString("field")),
          )
        }
      TYPE_SPEAK_ITEM ->
        item.optString("label").takeIf { it.isNotBlank() }?.let {
          SpeakItem(
            it,
            item.optLong("timeout", DEFAULT_TAP_TIMEOUT_MS).coerceIn(MIN_TAP_TIMEOUT_MS, MAX_TAP_TIMEOUT_MS),
            item.optBoolean("regex", false),
            Field.of(item.optString("field")),
          )
        }
      TYPE_SPEAK_TEXT -> item.optString("text").takeIf { it.isNotBlank() }?.let { SpeakText(it.take(MAX_TEXT)) }
      TYPE_DO ->
        DoKind.of(item.optString("do"))?.let { kind ->
          val name = item.optString("name")
          if (kind.setsValue && !VALUE_NAME.matches(name)) null
          else Do(kind, item.optString("text").take(MAX_TEXT), name.take(64), item.optInt("number").coerceIn(0, MAX_DO_NUMBER))
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

  /** Returns the X or Y [name] of [item], or null when it is missing or out of range. */
  private fun coordinateOf(item: JSONObject, name: String): Int? {
    if (!item.has(name)) return null
    val value = item.optDouble(name, Double.NaN)
    if (value.isNaN() || value < 0 || value > MAX_COORDINATE) return null
    return value.roundToLong().toInt()
  }
}
