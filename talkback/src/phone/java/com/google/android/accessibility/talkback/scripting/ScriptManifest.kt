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

import android.content.Context
import androidx.annotation.StringRes
import com.google.android.accessibility.talkback.R

enum class ScriptPermission(val key: String, val sensitive: Boolean, @StringRes val text: Int) {
  SCREEN("screen", false, R.string.script_permission_screen),
  ACTIONS("actions", false, R.string.script_permission_actions),
  SPEECH("speech", false, R.string.script_permission_speech),
  PASSWORDS("passwords", true, R.string.script_permission_passwords),
  INPUT("input", false, R.string.script_permission_input),
  NETWORK("network", true, R.string.script_permission_network),
  CLIPBOARD("clipboard", true, R.string.script_permission_clipboard),
  SYSTEM("system", true, R.string.script_permission_system),
  NOTIFICATIONS("notifications", true, R.string.script_permission_notifications),
  EVENTS("events", true, R.string.script_permission_events),
  DIALOGS("dialogs", true, R.string.script_permission_dialogs);

  companion object {
    fun fromKey(key: String): ScriptPermission? = entries.firstOrNull { it.key == key }
  }
}

data class AppMatcher(val packageName: String, val activity: String?, val window: String?) {
  fun matchesFront(packageName: String?, activity: String?, windowTitle: String?): Boolean =
    packageName == this.packageName &&
      (this.activity == null || activityMatches(activity)) &&
      (window == null || windowTitle?.contains(window, ignoreCase = true) == true)

  fun matchesOther(packageName: String?): Boolean =
    packageName == this.packageName && activity == null && window == null

  /**
   * Whether the front activity is the one this names. An activity is only known from the app's
   * window changes, so after Backtalk starts with the app already open it is unknown until the app
   * changes window, and is taken to match.
   */
  private fun activityMatches(className: String?): Boolean {
    val wanted = activity ?: return true
    return className == null ||
      className == wanted ||
      className.endsWith(if (wanted.startsWith(".")) wanted else ".$wanted")
  }
}

data class ScriptSetting(
  val key: String,
  val type: Type,
  val title: String,
  val summary: String?,
  val defaultJson: String,
  val options: List<Pair<String, String>>,
) {
  enum class Type(val key: String, val hasValue: Boolean = true) {
    SWITCH("switch"),
    LIST("list"),
    TEXT("text"),
    NUMBER("number"),
    BUTTON("button", hasValue = false),
  }
}

/** How far hide or silence reaches: not at all, the item, or the item and everything in it. */
enum class Hide {
  NONE,
  SELF,
  ALL,
}

data class ScriptRule(
  val id: String? = null,
  val match: NodeQuery? = null,
  val window: String? = null,
  val activity: String? = null,
  val label: String? = null,
  val speak: String? = null,
  val hide: Hide = Hide.NONE,
  /** The items whose changes, selections and announcements are not spoken. */
  val silence: Hide = Hide.NONE,
  val role: String? = null,
  val state: String? = null,
  val hint: String? = null,
  val heading: Boolean? = null,
  val group: Boolean = false,
  val readBefore: NodeQuery? = null,
  val readAfter: NodeQuery? = null,
  val actions: List<RuleItemAction> = emptyList(),
  /** What a double-tap presses, searched inside the item. */
  val activate: NodeQuery? = null,
) {
  val target: String
    get() = id ?: match.toString()

  val changesItem: Boolean
    get() =
      listOf(label, speak, role, state, hint, heading, readBefore, readAfter, activate)
        .any { it != null } ||
        hide != Hide.NONE ||
        silence != Hide.NONE ||
        group
}

data class ScriptNavigation(val title: String, val query: NodeQuery)

data class RuleItemAction(
  val title: String,
  val verb: Verb,
  val target: NodeQuery? = null,
  val text: String? = null,
) {
  enum class Verb(val key: String) {
    CLICK("click"),
    LONG_CLICK("longClick"),
    FOCUS("focus"),
    SCROLL_FORWARD("scrollForward"),
    SCROLL_BACKWARD("scrollBackward"),
    SPEAK("speak"),
  }

  val permission: ScriptPermission?
    get() =
      when {
        verb != Verb.SPEAK -> ScriptPermission.ACTIONS
        target != null -> ScriptPermission.SCREEN
        else -> null
      }
}

sealed class CommandBinding(val key: String) {
  class Gesture(val name: String) : CommandBinding(ScriptInput.gestureBinding(name))

  class KeyCombo(val keys: ScriptInput.Keys) : CommandBinding(ScriptInput.keysBinding(keys))

  data object MenuItem : CommandBinding("menu")

  data object ReadingControl : CommandBinding("control")

  companion object {
    fun fromKey(key: String): CommandBinding? =
      when {
        key == MenuItem.key -> MenuItem
        key == ReadingControl.key -> ReadingControl
        key.startsWith(ScriptInput.GESTURE_BINDING) ->
          key
            .removePrefix(ScriptInput.GESTURE_BINDING)
            .takeIf(ScriptInput::isGesture)
            ?.let(::Gesture)
        key.startsWith(ScriptInput.KEYS_BINDING) ->
          ScriptInput.keysFromId(key.removePrefix(ScriptInput.KEYS_BINDING))?.let(::KeyCombo)
        else -> null
      }
  }
}

data class ScriptCommand(
  val id: String,
  val title: String,
  val gestures: List<String>,
  val keys: List<ScriptInput.Keys>,
  val menu: Boolean,
  val control: Boolean,
) {
  fun isDefault(binding: CommandBinding): Boolean = bindings.any { it.key == binding.key }

  val bindings: List<CommandBinding>
    get() =
      gestures.map { CommandBinding.Gesture(it) } +
        keys.map { CommandBinding.KeyCombo(it) } +
        listOfNotNull(
          CommandBinding.MenuItem.takeIf { menu },
          CommandBinding.ReadingControl.takeIf { control },
        )
}

enum class ApiCompatibility {
  COMPATIBLE,
  BACKTALK_TOO_OLD,
  UNTESTED,
}

data class ScriptManifest(
  val id: String,
  val name: String,
  val version: String,
  val description: String?,
  val author: String,
  val homepage: String?,
  val minApiVersion: Int,
  val testedApiVersion: Int,
  val apps: List<AppMatcher>,
  val permissions: Set<ScriptPermission>,
  val settings: List<ScriptSetting>,
  val rules: List<ScriptRule>,
  val commands: List<ScriptCommand>,
  val navigation: List<ScriptNavigation>,
  val json: String,
) {
  val isGlobal: Boolean
    get() = apps.isEmpty()

  val compatibility: ApiCompatibility
    get() =
      when {
        API_VERSION < minApiVersion -> ApiCompatibility.BACKTALK_TOO_OLD
        API_VERSION > testedApiVersion -> ApiCompatibility.UNTESTED
        else -> ApiCompatibility.COMPATIBLE
      }

  fun translated(text: (String) -> String): ScriptManifest =
    copy(
      name = text(name),
      description = description?.let(text),
      settings =
        settings.map { setting ->
          setting.copy(
            title = text(setting.title),
            summary = setting.summary?.let(text),
            options = setting.options.map { (value, label) -> value to text(label) },
          )
        },
      rules =
        rules.map {
          it.copy(
            label = it.label?.let(text),
            speak = it.speak?.let(text),
            state = it.state?.let(text),
            hint = it.hint?.let(text),
            actions =
              it.actions.map { action ->
                action.copy(title = text(action.title), text = action.text?.let(text))
              },
          )
        },
      commands = commands.map { it.copy(title = text(it.title)) },
      navigation = navigation.map { it.copy(title = text(it.title)) },
    )

  fun runsIn(packageName: String?): Boolean = isGlobal || apps.any { it.packageName == packageName }

  fun compatibilityWarning(context: Context): String? =
    when (compatibility) {
      ApiCompatibility.COMPATIBLE -> null
      ApiCompatibility.BACKTALK_TOO_OLD ->
        context.getString(R.string.script_api_too_old, minApiVersion, API_VERSION)
      ApiCompatibility.UNTESTED ->
        context.getString(R.string.script_api_untested, testedApiVersion, API_VERSION)
    }

  companion object {
    const val API_VERSION = 4

    fun parse(json: String, strict: Boolean = true): ScriptManifest =
      ManifestParser(strict).parse(json)
  }
}
