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

package com.google.android.accessibility.talkback.scripting

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.content.Intent
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.macros.MacroFragment
import com.google.android.accessibility.talkback.macros.Macros
import com.google.android.accessibility.talkback.notifications.NotificationCodeFragment
import com.google.android.accessibility.talkback.preference.base.GesturePreferenceFragmentCompat
import com.google.android.accessibility.talkback.scripting.ScriptBuilder.objects
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject

/**
 * A screen of the script builder. It reads the script's draft each time it is built, and saves
 * and installs the script after each change.
 */
abstract class ScriptBuilderBaseFragment : NotificationCodeFragment(R.string.script_builder_title) {
  protected val scriptId: String by lazy { requireArguments().getString(ScriptScreenFragment.ARG_ID).orEmpty() }
  protected val store: ScriptStore by lazy { ScriptStore.get(requireContext()) }

  protected fun draft(): JSONObject? = ScriptBuilder.draft(store, scriptId)

  /** Changes the draft with [change], saves it and builds the screen again. */
  protected open fun edit(change: (JSONObject) -> Unit) {
    val draft = draft() ?: return
    change(draft)
    try {
      ScriptBuilder.save(requireContext(), draft)
    } catch (e: IOException) {
      Toast.makeText(requireContext(), getString(R.string.script_builder_save_failed, e.message), Toast.LENGTH_LONG).show()
    } catch (e: ManifestException) {
      // The script stays as it was: the change makes a manifest SandFly refuses.
      Toast.makeText(requireContext(), getString(R.string.script_builder_save_failed, e.message), Toast.LENGTH_LONG).show()
    }
    rebuild()
  }

  protected fun open(screen: Class<out Fragment>, arguments: Bundle.() -> Unit) {
    onPreferenceTreeClick(
      Preference(requireContext()).apply {
        fragment = screen.name
        extras.putString(ScriptScreenFragment.ARG_ID, scriptId)
        extras.arguments()
      }
    )
  }

  /** Opens the query at [path] of the draft, making an empty one there first if there is none. */
  protected fun openQuery(path: List<Any>) {
    if (Paths.get(draft(), path) !is JSONObject) edit { Paths.put(it, path, JSONObject()) }
    open(ScriptQueryFragment::class.java) { putString(ScriptQueryFragment.ARG_PATH, Paths.encode(path)) }
  }

  /** Asks for a text, with [old] filled in, and gives it to [then]; an empty text is null. */
  protected fun askOptional(title: CharSequence, old: String?, inputType: Int = InputType.TYPE_CLASS_TEXT, then: (String?) -> Unit) {
    askText(title, text = old.orEmpty(), inputType = inputType) { text, _, _ ->
      then(text.ifEmpty { null })
      null
    }
  }

  /** Calls [edit], or, when the value is set, first offers Edit and Remove. */
  protected fun editOrRemove(title: String, isSet: Boolean, edit: () -> Unit, remove: () -> Unit) {
    if (!isSet) return edit()
    choose(title, listOf(getString(R.string.notification_edit), getString(R.string.script_builder_remove))) {
      if (it == 0) edit() else remove()
    }
  }

  protected fun JSONObject.putOrRemove(key: String, value: Any?): JSONObject =
    if (value == null) apply { remove(key) } else put(key, value)

  protected fun notSet(): String = getString(R.string.script_builder_not_set)

  protected fun move(list: JSONArray, index: Int, offset: Int) {
    val target = index + offset
    if (target !in 0 until list.length()) return
    val item = list.get(index)
    list.put(index, list.get(target))
    list.put(target, item)
  }

  protected fun JSONArray.removeAt(index: Int) {
    remove(index)
  }
}

/** The page of a built script: its name, apps, rules and reading controls. */
class ScriptBuilderFragment : ScriptBuilderBaseFragment() {
  override fun getTitle(): CharSequence = draft()?.optString("name") ?: getText(R.string.script_builder_title)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // The command list for the default of a command setting gives the command chosen.
    parentFragmentManager.setFragmentResultListener(defaultCommandResultKey(), this) { _, result ->
      val index = result.getString(GesturePreferenceFragmentCompat.RESULT_TAG)?.toIntOrNull()
      val command = result.getString(GesturePreferenceFragmentCompat.RESULT_COMMAND)
      if (index != null && command != null) {
        edit { draft ->
          draft.optJSONArray("settings")?.optJSONObject(index)
            ?.takeIf { it.optString("type") == "command" }
            ?.putOrRemove("default", command.ifEmpty { null })
        }
      }
    }
  }

  override fun build(screen: PreferenceGroup) {
    val draft = draft() ?: return
    val context = requireContext()
    val pro = com.google.android.accessibility.talkback.sandflyai.SandflyAi.features
    ScriptAi.review(context, scriptId)?.let { summary ->
      // A script SandFly Pro wrote, which the person hasn't accepted yet.
      val review = screen.category(R.string.script_ai_review)
      if (summary.isNotBlank()) review.action(summary) {}.isSelectable = false
      val held = store.find(scriptId)?.manifest?.permissions.orEmpty().filter { it in ScriptAi.HELD }
      if (held.isNotEmpty()) {
        review.action(getString(R.string.script_ai_held, held.joinToString(", ") { it.key })) {}.isSelectable = false
      }
      review.action(getString(R.string.script_ai_accept)) {
        ScriptAi.accept(context, scriptId)
        rebuild()
      }
      pro?.let { review.action(getString(R.string.script_ai_ask_changes)) { it.buildScript(context, scriptId) } }
      review.action(getString(R.string.script_ai_discard)) {
        confirm(getString(R.string.script_ai_discard), R.string.script_builder_delete) {
          ScriptAi.discard(context, scriptId)
          parentFragmentManager.popBackStack()
        }
      }
    }
    screen.action(getString(R.string.script_builder_name), draft.optString("name")) {
      askText(getString(R.string.script_builder_name), draft.optString("name")) { name, _, _ ->
        if (name.isEmpty()) return@askText getString(R.string.script_builder_name_empty)
        edit { it.put("name", name) }
        activity?.title = name
        null
      }
    }
    screen.link(getString(R.string.script_builder_script_page), null, ScriptDetailFragment::class.java) {
      putString(ScriptScreenFragment.ARG_ID, scriptId)
    }
    if (pro != null && ScriptAi.review(context, scriptId) == null) {
      screen.action(getString(R.string.script_ai_change)) { pro.buildScript(context, scriptId) }
    }

    val apps = draft.optJSONArray("apps") ?: JSONArray()
    val appsCategory = screen.category(R.string.script_builder_apps)
    if (apps.length() == 0) appsCategory.action(getString(R.string.script_builder_all_apps)) {}.isSelectable = false
    for (index in 0 until apps.length()) {
      val app = apps.get(index)
      appsCategory.action(appText(context, app)) { editApp(index) }
    }
    appsCategory.action(getString(R.string.script_builder_add_app)) {
      chooseApp { packageName, _ -> if (packageName.isNotEmpty()) edit { it.getOrPutArray("apps").put(packageName) } }
    }

    val rules = draft.optJSONArray("rules").objects()
    val rulesCategory = screen.category(R.string.script_builder_rules)
    rules.forEachIndexed { index, rule ->
      rulesCategory.link(ruleTitle(context, rule), ruleSummary(context, rule), ScriptRuleFragment::class.java) {
        putString(ScriptScreenFragment.ARG_ID, scriptId)
        putInt(ScriptRuleFragment.ARG_INDEX, index)
      }
    }
    rulesCategory.action(getString(R.string.script_builder_add_rule)) {
      edit { it.getOrPutArray("rules").put(JSONObject().put("match", JSONObject())) }
      val index = draft()?.optJSONArray("rules")?.length()?.minus(1) ?: return@action
      open(ScriptRuleFragment::class.java) { putInt(ScriptRuleFragment.ARG_INDEX, index) }
    }

    val navigation = draft.optJSONArray("navigation").objects()
    val navigationCategory = screen.category(R.string.script_builder_navigation)
    navigation.forEachIndexed { index, item ->
      val problem = ScriptBuilder.entryProblemText(context, item, ScriptBuilder.navigationProblem(item))
      val summary =
        if (problem != null) getString(R.string.script_builder_not_used, problem)
        else describeQuery(context, item.optJSONObject("match"))
      navigationCategory.action(item.optString("title"), summary) { editNavigation(index) }
    }
    navigationCategory.action(getString(R.string.script_builder_add_navigation)) {
      askText(getString(R.string.script_builder_add_navigation), hint = getString(R.string.script_builder_title_field)) { title, _, _ ->
        if (title.isEmpty()) return@askText getString(R.string.script_builder_name_empty)
        edit { it.getOrPutArray("navigation").put(JSONObject().put("title", title).put("match", JSONObject())) }
        val index = draft()?.optJSONArray("navigation")?.length()?.minus(1) ?: return@askText null
        openQuery(listOf("navigation", index, "match"))
        null
      }
    }

    BuiltScriptMacros.init(context)
    val commands = draft.optJSONArray(BuiltScriptMacros.COMMANDS).objects()
    val commandsCategory = screen.category(R.string.script_builder_commands)
    commands.forEachIndexed { index, command ->
      commandsCategory.link(command.optString("title"), stepsText(command), ScriptBuiltCommandFragment::class.java) {
        putString(ScriptScreenFragment.ARG_ID, scriptId)
        putInt(ScriptBuiltCommandFragment.ARG_INDEX, index)
      }
    }
    commandsCategory.action(getString(R.string.script_builder_add_command)) {
      askText(getString(R.string.script_builder_add_command), hint = getString(R.string.script_builder_title_field)) { title, _, _ ->
        if (title.isEmpty()) return@askText getString(R.string.script_builder_name_empty)
        val entryId = newEntryId("c")
        edit { it.getOrPutArray(BuiltScriptMacros.COMMANDS).put(JSONObject().put("id", entryId).put("title", title)) }
        openSteps(BuiltScriptMacros.COMMANDS, entryId)
        null
      }
    }

    val triggers = draft.optJSONArray(BuiltScriptMacros.TRIGGERS).objects()
    val triggersCategory = screen.category(R.string.script_builder_triggers)
    triggers.forEachIndexed { index, trigger ->
      val problem = ScriptBuilder.entryProblemText(context, trigger, ScriptBuilder.triggerProblem(trigger))
      val summary = if (problem != null) getString(R.string.script_builder_not_used, problem) else stepsText(trigger)
      val title =
        if (trigger.optString("when") == ScriptBuilder.WHEN_BUTTON) {
          getString(R.string.script_builder_when_button_value, ScriptBuilder.settingTitle(trigger.optString("setting"), draft))
        } else {
          ScriptBuilder.triggerText(context, trigger)
        }
      triggersCategory.action(title, summary) { editTrigger(index) }
    }
    triggersCategory.action(getString(R.string.script_builder_add_trigger)) { addTrigger() }

    val speech = draft.optJSONArray(ScriptBuilder.SPEECH).objects()
    val speechCategory = screen.category(R.string.script_builder_speech_changes)
    speech.forEachIndexed { index, change ->
      val problem = ScriptBuilder.entryProblemText(context, change, ScriptBuilder.speechProblem(change))
      speechCategory.link(
        ScriptBuilder.speechText(context, change),
        problem?.let { getString(R.string.script_builder_not_used, it) } ?: ScriptSpeechChangeFragment.changeText(context, change),
        ScriptSpeechChangeFragment::class.java,
      ) {
        putString(ScriptScreenFragment.ARG_ID, scriptId)
        putInt(ScriptSpeechChangeFragment.ARG_INDEX, index)
      }
    }
    speechCategory.action(getString(R.string.script_builder_add_speech_change)) {
      edit {
        it.getOrPutArray(ScriptBuilder.SPEECH)
          .put(JSONObject().put("when", ScriptBuilder.WHEN_SPEECH).put("change", ScriptBuilder.CHANGE_SAY))
      }
      val index = draft()?.optJSONArray(ScriptBuilder.SPEECH)?.length()?.minus(1) ?: return@action
      open(ScriptSpeechChangeFragment::class.java) { putInt(ScriptSpeechChangeFragment.ARG_INDEX, index) }
    }

    val settings = draft.optJSONArray("settings").objects()
    val settingsCategory = screen.category(R.string.script_builder_settings)
    settings.forEachIndexed { index, setting ->
      settingsCategory.action(setting.optString("title"), settingText(setting)) { editSetting(index) }
    }
    settingsCategory.action(getString(R.string.script_builder_add_setting)) { addSetting() }

    val about = screen.category(R.string.script_builder_about)
    for ((key, title) in
      listOf(
        "description" to R.string.script_builder_description,
        "version" to R.string.script_builder_version,
        "homepage" to R.string.script_builder_homepage,
      )) {
      val old = draft.optString(key).ifEmpty { null }
      about.action(getString(title), old ?: notSet()) {
        askOptional(getString(title), old) { value -> edit { it.putOrRemove(key, value) } }
      }
    }
    about.switch(getString(R.string.script_builder_on_demand), draft.optBoolean("onDemand")) { on ->
      edit { it.putOrRemove("onDemand", if (on) true else null) }
    }
    about.switch(getString(R.string.script_keep_running), draft.optBoolean("keepLoaded")) { on ->
      edit { it.putOrRemove("keepLoaded", if (on) true else null) }
    }

    val codeCategory = screen.category(R.string.script_builder_code_category)
    val code = draft.optString("code")
    codeCategory.action(
      getString(R.string.script_builder_code),
      if (code.isBlank()) notSet() else resources.getQuantityString(R.plurals.script_builder_code_lines, code.lines().size, code.lines().size),
    ) {
      askText(
        getString(R.string.script_builder_code),
        code,
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,
      ) { text, _, _ ->
        LuaEngine.syntaxError(text)?.let { return@askText getString(R.string.script_builder_code_error, it) }
        edit { it.putOrRemove("code", text.ifBlank { null }) }
        null
      }
    }
    val extra = draft.optJSONArray("permissions").let { list -> (0 until (list?.length() ?: 0)).map { list!!.optString(it) } }
    codeCategory.action(
      getString(R.string.script_builder_extra_permissions),
      extra.mapNotNull { ScriptPermission.fromKey(it) }.joinToString(", ") { it.key }.ifEmpty { notSet() },
    ) {
      val permissions = ScriptPermission.entries
      val checked = permissions.map { it.key in extra }.toBooleanArray()
      dialog()
        .setTitle(R.string.script_builder_extra_permissions)
        .setMultiChoiceItems(permissions.map { requireContext().permissionText(it) }.toTypedArray(), checked) { _, which, on ->
          checked[which] = on
        }
        .setPositiveButton(android.R.string.ok) { _, _ ->
          val chosen = permissions.filterIndexed { i, _ -> checked[i] }.map { it.key }
          edit { it.putOrRemove("permissions", if (chosen.isEmpty()) null else JSONArray(chosen)) }
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
    }

    val scriptCategory = screen.category(R.string.script_builder_script)
    scriptCategory.action(getString(R.string.script_builder_export)) {
      exportFile.launch(draft.optString("name").ifBlank { scriptId } + ".zip")
    }
    scriptCategory.action(getString(R.string.script_builder_duplicate_script)) {
      val copy = JSONObject(draft.toString())
      copy.put("name", getString(R.string.script_builder_copy_name, draft.optString("name")))
      val id =
        try {
          ScriptBuilder.createFrom(requireContext(), copy, scriptId)
        } catch (e: IOException) {
          Toast.makeText(requireContext(), getString(R.string.script_builder_save_failed, e.message), Toast.LENGTH_LONG).show()
          return@action
        }
      Toast.makeText(requireContext(), getString(R.string.script_builder_copied, copy.optString("name")), Toast.LENGTH_SHORT).show()
      open(ScriptBuilderFragment::class.java) { putString(ScriptScreenFragment.ARG_ID, id) }
    }
    scriptCategory.action(getString(R.string.script_remove)) {
      confirm(getString(R.string.script_remove_title, draft.optString("name")), R.string.script_remove) {
        store.remove(scriptId)
        ScriptLog.clear(scriptId)
        parentFragmentManager.popBackStack()
      }
    }
  }

  private val exportFile =
    registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/zip")) { uri ->
      if (uri == null) return@registerForActivityResult
      val context = requireContext()
      val saved =
        try {
          context.contentResolver.openOutputStream(uri)?.use { ScriptBuilder.exportZip(store, scriptId, it) } != null
        } catch (e: IOException) {
          false
        }
      Toast.makeText(context, if (saved) R.string.script_builder_exported else R.string.script_builder_export_failed, Toast.LENGTH_SHORT).show()
    }

  private fun newEntryId(prefix: String): String =
    prefix + java.lang.Long.toString(System.currentTimeMillis(), Character.MAX_RADIX)

  private fun stepsText(entry: JSONObject): String {
    val count = BuiltScriptMacros.macroOf(entry)?.steps?.size ?: 0
    return if (count == 0) getString(R.string.macro_no_steps)
    else resources.getQuantityString(R.plurals.script_builder_steps, count, count)
  }

  private fun openSteps(list: String, entryId: String) {
    open(MacroFragment::class.java) { putString(MacroFragment.ARG_MACRO, BuiltScriptMacros.idOf(scriptId, list, entryId)) }
  }

  /** The kinds of trigger, in the order Add trigger lists them, with their names. */
  private fun triggerKinds(): List<Pair<String, String>> =
    listOf(
      ScriptBuilder.WHEN_FOCUS to getString(R.string.script_builder_when_focus),
      ScriptBuilder.WHEN_APP to getString(R.string.script_builder_when_app),
      ScriptBuilder.WHEN_APP_LEAVE to getString(R.string.script_builder_when_app_leave),
      ScriptBuilder.WHEN_WINDOW to getString(R.string.script_builder_when_window),
      ScriptBuilder.WHEN_ANNOUNCEMENT to getString(R.string.script_builder_when_announcement),
      ScriptBuilder.WHEN_NOTIFICATION to getString(R.string.script_builder_when_notification),
    ) +
      ScriptBuilder.NODE_TRIGGER_TITLES.map { (kind, title) -> kind to getString(title) } +
      listOf(
        ScriptBuilder.WHEN_EVERY to getString(R.string.script_builder_when_every),
        ScriptBuilder.WHEN_DAILY to getString(R.string.script_builder_when_daily),
        ScriptBuilder.WHEN_BUTTON to getString(R.string.script_builder_when_button),
      )

  private fun addTrigger() {
    val kinds = triggerKinds()
    choose(getString(R.string.script_builder_add_trigger), kinds.map { it.second }) { which ->
      val (kind, name) = kinds[which]
      val entryId = newEntryId("t")
      val trigger = JSONObject().put("id", entryId).put("when", kind)
      fun add(then: () -> Unit = { openSteps(BuiltScriptMacros.TRIGGERS, entryId) }) {
        edit { it.getOrPutArray(BuiltScriptMacros.TRIGGERS).put(trigger) }
        then()
      }
      when (kind) {
        ScriptBuilder.WHEN_FOCUS -> {
          trigger.put("match", JSONObject())
          add {
            val index = draft()?.optJSONArray(BuiltScriptMacros.TRIGGERS)?.length()?.minus(1) ?: return@add
            openQuery(listOf(BuiltScriptMacros.TRIGGERS, index, "match"))
          }
        }
        ScriptBuilder.WHEN_ANNOUNCEMENT, ScriptBuilder.WHEN_NOTIFICATION ->
          askText(name, hint = getString(R.string.script_builder_has_text_optional), inputType = InputType.TYPE_CLASS_TEXT) { text, _, _ ->
            trigger.put("text", text)
            add()
            null
          }
        ScriptBuilder.WHEN_EVERY -> askMinutes(null) { trigger.put("minutes", it); add() }
        ScriptBuilder.WHEN_DAILY -> askTime(null) { trigger.put("time", it); add() }
        ScriptBuilder.WHEN_BUTTON -> chooseButton(null) { trigger.put("setting", it); add() }
        else -> add()
      }
    }
  }

  private fun askMinutes(old: Int?, then: (Int) -> Unit) =
    askText(getString(R.string.script_builder_minutes), old?.toString().orEmpty(), inputType = InputType.TYPE_CLASS_NUMBER) { text, _, _ ->
      val minutes = text.toIntOrNull()?.takeIf { it in 1..1440 } ?: return@askText getString(R.string.macro_number_range, 1, 1440)
      then(minutes)
      null
    }

  private fun askTime(old: String?, then: (String) -> Unit) =
    askText(getString(R.string.script_builder_time), old.orEmpty(), hint = "08:30", inputType = InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_TIME) { text, _, _ ->
      val time = Regex("^(\\d{1,2}):(\\d{2})$").find(text.trim())?.let { m ->
        val hour = m.groupValues[1].toInt()
        val minute = m.groupValues[2].toInt()
        if (hour in 0..23 && minute in 0..59) "%02d:%02d".format(hour, minute) else null
      } ?: return@askText getString(R.string.script_builder_time_error)
      then(time)
      null
    }

  /** Asks for one of the draft's button settings, making one first when there is none. */
  private fun chooseButton(old: String?, then: (String) -> Unit) {
    val buttons = draft()?.optJSONArray("settings").objects().filter { it.optString("type") == "button" }
    if (buttons.isNullOrEmpty()) {
      askText(getString(R.string.script_builder_add_setting), hint = getString(R.string.script_builder_title_field)) { title, _, _ ->
        if (title.isEmpty()) return@askText getString(R.string.script_builder_name_empty)
        val key = newEntryId("s")
        edit { it.getOrPutArray("settings").put(JSONObject().put("key", key).put("type", "button").put("title", title)) }
        then(key)
        null
      }
      return
    }
    choose(getString(R.string.script_builder_when_button), buttons.map { it.optString("title") }, buttons.indexOfFirst { it.optString("key") == old }) {
      then(buttons[it].optString("key"))
    }
  }

  private fun editTrigger(index: Int) {
    val trigger = draft()?.optJSONArray(BuiltScriptMacros.TRIGGERS)?.optJSONObject(index) ?: return
    val entryId = trigger.optString("id")
    val kind = trigger.optString("when")
    val steps = getString(R.string.script_builder_steps)
    val item = getString(R.string.script_builder_item)
    val anyItem = getString(R.string.script_builder_any_item)
    val text = getString(R.string.script_builder_text)
    val minutes = getString(R.string.script_builder_minutes)
    val time = getString(R.string.script_builder_time)
    val button = getString(R.string.script_builder_when_button)
    val remove = getString(R.string.script_builder_remove)
    val options = mutableListOf(steps)
    when (kind) {
      ScriptBuilder.WHEN_FOCUS -> options += item
      in ScriptBuilder.NODE_TRIGGERS -> {
        options += item
        if ((trigger.optJSONObject("match")?.length() ?: 0) > 0) options += anyItem
      }
      ScriptBuilder.WHEN_ANNOUNCEMENT, ScriptBuilder.WHEN_NOTIFICATION -> options += text
      ScriptBuilder.WHEN_EVERY -> options += minutes
      ScriptBuilder.WHEN_DAILY -> options += time
      ScriptBuilder.WHEN_BUTTON -> options += button
    }
    options += remove
    fun change(block: (JSONObject) -> Unit) = edit { block(it.getJSONArray(BuiltScriptMacros.TRIGGERS).getJSONObject(index)) }
    choose(ScriptBuilder.triggerText(requireContext(), trigger), options) { which ->
      when (options[which]) {
        steps -> openSteps(BuiltScriptMacros.TRIGGERS, entryId)
        item -> openQuery(listOf(BuiltScriptMacros.TRIGGERS, index, "match"))
        anyItem -> change { it.remove("match") }
        text ->
          askText(text, trigger.optString("text"), hint = getString(R.string.script_builder_has_text_optional), inputType = InputType.TYPE_CLASS_TEXT) { value, _, _ ->
            change { it.put("text", value) }
            null
          }
        minutes -> askMinutes(trigger.optInt("minutes")) { value -> change { it.put("minutes", value) } }
        time -> askTime(trigger.optString("time")) { value -> change { it.put("time", value) } }
        button -> chooseButton(trigger.optString("setting")) { value -> change { it.put("setting", value) } }
        else -> edit { it.getJSONArray(BuiltScriptMacros.TRIGGERS).removeAt(index) }
      }
    }
  }

  private fun settingText(setting: JSONObject): String {
    val type =
      getString(
        when (setting.optString("type")) {
          "switch" -> R.string.script_builder_setting_switch
          "list" -> R.string.script_builder_setting_list
          "text" -> R.string.script_builder_setting_text
          "number" -> R.string.script_builder_setting_number
          "command" -> R.string.script_builder_setting_command
          else -> R.string.script_builder_setting_button
        }
      )
    return if (setting.optString("type") == "button") type
    else getString(R.string.script_builder_setting_value, type, "{setting.${setting.optString("key")}}")
  }

  private fun addSetting() {
    val types = listOf("switch", "list", "text", "number", "button", "command")
    val names =
      listOf(
        R.string.script_builder_setting_switch,
        R.string.script_builder_setting_list,
        R.string.script_builder_setting_text,
        R.string.script_builder_setting_number,
        R.string.script_builder_setting_button,
        R.string.script_builder_setting_command,
      ).map { getString(it) }
    choose(getString(R.string.script_builder_add_setting), names) { which ->
      askText(names[which], hint = getString(R.string.script_builder_title_field), inputType = InputType.TYPE_CLASS_TEXT) { title, _, _ ->
        if (title.isEmpty()) return@askText getString(R.string.script_builder_name_empty)
        val setting = JSONObject().put("key", newEntryId("s")).put("type", types[which]).put("title", title)
        when (types[which]) {
          "switch" -> setting.put("default", false)
          "list" -> setting.put("options", JSONArray().put(getString(R.string.script_builder_option_one)))
        }
        edit { it.getOrPutArray("settings").put(setting) }
        if (types[which] == "list") editOptions(draft()?.optJSONArray("settings")?.length()?.minus(1) ?: return@askText null)
        null
      }
    }
  }

  private fun editSetting(index: Int) {
    val setting = draft()?.optJSONArray("settings")?.optJSONObject(index) ?: return
    val rename = getString(R.string.script_builder_rename)
    val summary = getString(R.string.script_builder_setting_summary)
    val default = getString(R.string.script_builder_setting_default)
    val options = getString(R.string.script_builder_setting_options)
    val remove = getString(R.string.script_builder_remove)
    val type = setting.optString("type")
    val choices =
      listOfNotNull(
        rename,
        summary,
        default.takeIf { type != "button" },
        options.takeIf { type == "list" || type == "command" },
        remove,
      )
    fun change(block: (JSONObject) -> Unit) = edit { block(it.getJSONArray("settings").getJSONObject(index)) }
    choose(setting.optString("title"), choices) { which ->
      when (choices[which]) {
        rename ->
          askText(rename, setting.optString("title")) { title, _, _ ->
            if (title.isEmpty()) return@askText getString(R.string.script_builder_name_empty)
            change { it.put("title", title) }
            null
          }
        summary -> askOptional(summary, setting.optString("summary").ifEmpty { null }) { value -> change { it.putOrRemove("summary", value) } }
        default ->
          when (type) {
            "switch" -> {
              val names = listOf(getString(R.string.script_state_off), getString(R.string.script_state_on))
              choose(default, names, if (setting.optBoolean("default")) 1 else 0) { on -> change { it.put("default", on == 1) } }
            }
            "list" -> {
              val values = optionValues(setting)
              choose(default, values, values.indexOf(setting.optString("default"))) { chosen -> change { it.put("default", values[chosen]) } }
            }
            "command" -> chooseDefaultCommand(index, setting)
            "number" ->
              askText(default, setting.opt("default")?.toString().orEmpty(), inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_NUMBER_FLAG_DECIMAL) { text, _, _ ->
                change { it.putOrRemove("default", text.toDoubleOrNull()?.let { n -> if (n % 1.0 == 0.0) n.toLong() else n }) }
                null
              }
            else -> askOptional(default, setting.optString("default").ifEmpty { null }) { value -> change { it.putOrRemove("default", value) } }
          }
        options -> editOptions(index)
        else -> edit { it.getJSONArray("settings").removeAt(index) }
      }
    }
  }

  /**
   * The key of the fragment result that gets the default chosen for a command setting of this
   * script, so that the builder of another script, opened over this one by Duplicate script, can't
   * take it.
   */
  private fun defaultCommandResultKey(): String = "$DEFAULT_COMMAND_RESULT:$scriptId"

  /**
   * Opens the list of commands used for gestures, with the setting's own options first, for the
   * default of the command setting at [index].
   */
  private fun chooseDefaultCommand(index: Int, setting: JSONObject) {
    val options = optionValues(setting).toTypedArray()
    onPreferenceTreeClick(
      Preference(requireContext()).apply {
        fragment = GesturePreferenceFragmentCompat::class.java.name
        extras.putAll(
          GesturePreferenceFragmentCompat.commandSettingArguments(
            getString(R.string.script_builder_setting_default),
            setting.optString("default"),
            defaultCommandResultKey(),
            index.toString(),
            options,
            options,
          )
        )
      }
    )
  }

  private fun optionValues(setting: JSONObject): List<String> =
    setting.optJSONArray("options")?.let { list -> (0 until list.length()).map { list.optString(it) } }.orEmpty()

  /**
   * Asks for the options of the list or command setting at [index], one a line. A list needs one;
   * a command setting may have none.
   */
  private fun editOptions(index: Int) {
    val setting = draft()?.optJSONArray("settings")?.optJSONObject(index) ?: return
    val command = setting.optString("type") == "command"
    val old = optionValues(setting)
    askText(
      getString(R.string.script_builder_setting_options),
      old.joinToString("\n"),
      message = getString(R.string.script_builder_setting_options_help),
    ) { text, _, _ ->
      val values = text.lines().map { it.trim() }.filter { it.isNotEmpty() }.distinct()
      if (values.isEmpty() && !command) return@askText getString(R.string.script_builder_name_empty)
      edit { draft ->
        val edited = draft.getJSONArray("settings").getJSONObject(index)
        if (values.isEmpty()) edited.remove("options") else edited.put("options", JSONArray(values))
        val default = edited.optString("default")
        // A command setting's default may be a command, which stays, rather than an option.
        val removed = if (command) default in old else true
        if (default !in values && removed) edited.remove("default")
      }
      null
    }
  }

  private fun editApp(index: Int) {
    val app = draft()?.optJSONArray("apps")?.opt(index) ?: return
    val record = app as? JSONObject ?: JSONObject().put("package", app.toString())
    val options =
      listOf(
        getString(R.string.script_builder_only_activity),
        getString(R.string.script_builder_only_window),
        getString(R.string.script_builder_remove),
      )
    choose(appText(requireContext(), app), options) { which ->
      when (which) {
        0 ->
          askOptional(getString(R.string.script_builder_only_activity), record.optString("activity").ifEmpty { null }) { value ->
            edit { it.getJSONArray("apps").put(index, appValue(record.putOrRemove("activity", value))) }
          }
        1 ->
          askOptional(getString(R.string.script_builder_only_window), record.optString("window").ifEmpty { null }) { value ->
            edit { it.getJSONArray("apps").put(index, appValue(record.putOrRemove("window", value))) }
          }
        else -> edit { it.getJSONArray("apps").removeAt(index) }
      }
    }
  }

  /** An app as the manifest has it: its package name alone when nothing limits it. */
  private fun appValue(record: JSONObject): Any =
    if (record.length() == 1) record.getString("package") else record

  private fun editNavigation(index: Int) {
    val item = draft()?.optJSONArray("navigation")?.optJSONObject(index) ?: return
    val options =
      listOf(
        getString(R.string.script_builder_rename),
        getString(R.string.script_builder_item),
        getString(R.string.script_builder_remove),
      )
    choose(item.optString("title"), options) { which ->
      when (which) {
        0 ->
          askText(getString(R.string.script_builder_rename), item.optString("title")) { title, _, _ ->
            if (title.isEmpty()) return@askText getString(R.string.script_builder_name_empty)
            edit { it.getJSONArray("navigation").getJSONObject(index).put("title", title) }
            null
          }
        1 -> openQuery(listOf("navigation", index, "match"))
        else -> edit { it.getJSONArray("navigation").removeAt(index) }
      }
    }
  }

  companion object {
    private const val DEFAULT_COMMAND_RESULT = "script_builder_default_command"

    fun appText(context: Context, app: Any): String {
      val record = app as? JSONObject
      val packageName = record?.optString("package") ?: app.toString()
      val name =
        try {
          context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
          packageName
        }
      val limits =
        listOfNotNull(
          record?.optString("activity")?.ifEmpty { null }?.let { context.getString(R.string.script_builder_activity_value, it) },
          record?.optString("window")?.ifEmpty { null }?.let { context.getString(R.string.script_builder_window_value, it) },
        )
      return (listOf(name) + limits).joinToString(", ")
    }

    fun ruleTitle(context: Context, rule: JSONObject): String =
      describeQuery(context, rule.optJSONObject("match"))

    fun ruleSummary(context: Context, rule: JSONObject): String {
      ScriptBuilder.ruleProblemText(context, rule)?.let { return context.getString(R.string.script_builder_not_used, it) }
      return changeTexts(context, rule).joinToString(", ")
    }

    /** What [rule] changes, one text for each change. */
    fun changeTexts(context: Context, rule: JSONObject): List<String> = buildList {
      rule.optString("label").ifEmpty { null }?.let { add(context.getString(R.string.script_builder_label_value, it)) }
      rule.optString("speak").ifEmpty { null }?.let { add(context.getString(R.string.script_builder_speak_value, it)) }
      rule.optString("hint").ifEmpty { null }?.let { add(context.getString(R.string.script_builder_hint_value, it)) }
      rule.optString("state").ifEmpty { null }?.let { add(context.getString(R.string.script_builder_state_value, it)) }
      rule.optString("role").ifEmpty { null }?.let { add(context.getString(R.string.script_builder_role_value, it)) }
      if (rule.has("heading")) {
        add(context.getString(if (rule.getBoolean("heading")) R.string.script_builder_heading_yes else R.string.script_builder_heading_no))
      }
      if (rule.has("hide")) add(context.getString(R.string.script_builder_hide))
      if (rule.has("silence")) add(context.getString(R.string.script_builder_silence))
      if (rule.optBoolean("group")) add(context.getString(R.string.script_builder_group))
      if (rule.has("readBefore") || rule.has("readAfter")) add(context.getString(R.string.script_builder_read_order))
      if (rule.has("activate")) add(context.getString(R.string.script_builder_activate))
      if (rule.optBoolean("liftToActivate")) add(context.getString(R.string.script_builder_lift_to_activate))
      if (rule.optBoolean("touchToActivate")) add(context.getString(R.string.script_builder_touch_to_activate))
      rule.optJSONArray("actions").objects().forEach {
        add(context.getString(R.string.script_builder_action_value, it.optString("title")))
      }
    }

    /** [query] in words, such as: text contains "new post" (Description), Clickable. */
    fun describeQuery(context: Context, query: JSONObject?): String {
      if (query == null || query.length() == 0) return context.getString(R.string.script_builder_no_item)
      val parts = buildList {
        query.optString("id").ifEmpty { null }?.let { add(context.getString(R.string.script_builder_id_value, it)) }
        textCondition(context, query)?.let(::add)
        kindsOf(query).forEach { add(context.getString(it.title)) }
        query.optString("className").ifEmpty { null }?.let { add(context.getString(R.string.script_builder_class_value, it)) }
        query.optString("role").ifEmpty { null }?.let { add(context.getString(R.string.script_builder_role_value, it)) }
        when (val inside = query.opt("inside")) {
          is JSONObject -> add(context.getString(R.string.script_builder_inside_value, describeQuery(context, inside)))
          is JSONArray ->
            inside.objects().forEach { add(context.getString(R.string.script_builder_inside_value, describeQuery(context, it))) }
        }
        listOf("text", "contentDescription", "hint", "stateDescription").forEach { key ->
          query.optString(key).ifEmpty { null }?.let { add(context.getString(R.string.script_builder_exact_value, it)) }
        }
        query.optString("parentId").ifEmpty { null }?.let { add(context.getString(R.string.script_builder_parent_value, it)) }
        query.optJSONObject("has")?.let {
          add(context.getString(R.string.script_builder_has_value, describeQuery(context, it)))
        }
      }
      return parts.joinToString(", ").ifEmpty { context.getString(R.string.script_builder_no_item) }
    }

    /** The text condition of [query], as a macro test shows it, or null. */
    fun textCondition(context: Context, query: JSONObject): String? {
      val regex = query.has("matches")
      val text = (if (regex) query.optString("matches") else query.optString("contains")).ifEmpty { return null }
      val field = fieldOf(query)
      return when {
        field != null && regex ->
          context.getString(R.string.macro_field_regex_value, text, context.getString(field.title))
        field != null -> context.getString(R.string.macro_field_value, text, context.getString(field.title))
        regex -> context.getString(R.string.macro_regex_value, text)
        else -> context.getString(R.string.script_builder_contains_value, text)
      }
    }

    fun fieldOf(query: JSONObject): Macros.Field? =
      when (query.optString("field")) {
        "text" -> Macros.Field.TEXT
        "contentDescription", "description" -> Macros.Field.DESCRIPTION
        "hint" -> Macros.Field.HINT
        "stateDescription", "state" -> Macros.Field.STATE
        "tooltip" -> Macros.Field.TOOLTIP
        "id" -> Macros.Field.RESOURCE_ID
        else -> null
      }

    /** The name of [field] in a query. */
    fun fieldKey(field: Macros.Field): String =
      when (field) {
        Macros.Field.TEXT -> "text"
        Macros.Field.DESCRIPTION -> "contentDescription"
        Macros.Field.HINT -> "hint"
        Macros.Field.STATE -> "stateDescription"
        Macros.Field.TOOLTIP -> "tooltip"
        Macros.Field.RESOURCE_ID -> "id"
      }

    fun kindsOf(query: JSONObject): List<Macros.Kind> {
      val names =
        when (val kind = query.opt("kind")) {
          is String -> listOf(kind)
          is JSONArray -> (0 until kind.length()).map { kind.optString(it) }
          else -> emptyList()
        }
      return names.mapNotNull { NodeQuery.KINDS[it] }
    }
  }
}

/** One rule of a built script: the item it names and what it changes. */
class ScriptRuleFragment : ScriptBuilderBaseFragment() {
  private val index: Int by lazy { requireArguments().getInt(ARG_INDEX) }

  private fun rule(): JSONObject? = draft()?.optJSONArray("rules")?.optJSONObject(index)

  private fun path(vararg keys: Any): List<Any> = listOf("rules", index) + keys.toList()

  private fun change(key: String, value: Any?) = edit { draft ->
    draft.getJSONArray("rules").getJSONObject(index).putOrRemove(key, value)
  }

  override fun getTitle(): CharSequence = getText(R.string.script_builder_rule)

  override fun build(screen: PreferenceGroup) {
    val rule = rule() ?: return
    val context = requireContext()
    ScriptBuilder.ruleProblemText(context, rule)?.let {
      screen.action(getString(R.string.script_builder_not_used, it)) {}.isSelectable = false
    }

    val match = rule.optJSONObject("match")
    // The inspected item, when this rule names it: changes can be worked out from it.
    val sample =
      ItemPattern.samples(ItemInspector.lastSample(context)).firstOrNull { sample ->
        match != null && ItemPattern.item(sample)?.let { ItemPattern.tellsItem(match, it) } == true
      }
    val item = screen.category(R.string.script_builder_applies_to)
    val described = ScriptBuilderFragment.describeQuery(context, match)
    item.action(
      getString(R.string.script_builder_item),
      listOfNotNull(described, likeCountText(context, match, sample)).joinToString("\n"),
    ) {
      openQuery(path("match"))
    }
    textRow(item, "window", R.string.script_builder_only_window)
    textRow(item, "activity", R.string.script_builder_only_activity)

    if (sample != null) inspectedRows(screen, rule, sample)

    val changes = screen.category(R.string.script_builder_changes)
    textRow(changes, "label", R.string.script_builder_label)
    textRow(changes, "speak", R.string.script_builder_speak)
    textRow(changes, "hint", R.string.script_builder_hint)
    textRow(changes, "state", R.string.script_builder_state)
    changes.action(getString(R.string.script_builder_role), rule.optString("role").ifEmpty { notSet() }) {
      val roles = ScriptRoles.names.sorted()
      choose(getString(R.string.script_builder_role), listOf(notSet()) + roles, roles.indexOf(rule.optString("role")) + 1) {
        change("role", roles.getOrNull(it - 1))
      }
    }
    val headingValues = listOf(null, true, false)
    val headingNames =
      listOf(notSet(), getString(R.string.script_builder_heading_yes), getString(R.string.script_builder_heading_no))
    val heading = if (rule.has("heading")) rule.getBoolean("heading") else null
    changes.action(getString(R.string.script_builder_heading), headingNames[headingValues.indexOf(heading)]) {
      choose(getString(R.string.script_builder_heading), headingNames, headingValues.indexOf(heading)) {
        change("heading", headingValues[it])
      }
    }
    hideRow(changes, "hide", R.string.script_builder_hide, R.string.script_builder_hide_item, R.string.script_builder_hide_all)
    hideRow(
      changes,
      "silence",
      R.string.script_builder_silence,
      R.string.script_builder_silence_item,
      R.string.script_builder_silence_all,
    )
    changes.switch(getString(R.string.script_builder_group), rule.optBoolean("group")) {
      change("group", if (it) true else null)
    }
    readOrderRow(changes, rule)
    queryRow(changes, rule, "activate", R.string.script_builder_activate)
    changes.switch(getString(R.string.script_builder_lift_to_activate), rule.optBoolean("liftToActivate")) {
      edit { draft ->
        val edited = draft.getJSONArray("rules").getJSONObject(index)
        edited.putOrRemove("liftToActivate", if (it) true else null)
        if (!it) edited.remove("longPressAfter")
      }
    }
    if (rule.optBoolean("liftToActivate")) {
      val old = if (rule.has("longPressAfter")) rule.optInt("longPressAfter").toString() else null
      changes.action(getString(R.string.script_builder_long_press_after), old ?: notSet()) {
        askText(getString(R.string.script_builder_long_press_after), old.orEmpty(), inputType = InputType.TYPE_CLASS_NUMBER) { text, _, _ ->
          if (text.isEmpty()) {
            change("longPressAfter", null)
            return@askText null
          }
          val ms = text.toIntOrNull()
          if (ms == null || ms !in 100..10_000) return@askText getString(R.string.script_builder_long_press_range)
          change("longPressAfter", ms)
          null
        }
      }
    }
    changes.switch(getString(R.string.script_builder_touch_to_activate), rule.optBoolean("touchToActivate")) {
      change("touchToActivate", if (it) true else null)
    }

    changes.action(getString(R.string.script_builder_change_speech), getString(R.string.script_builder_change_speech_summary)) {
      // A speech change for this rule's item, which edits what SandFly says for it.
      edit { draft ->
        draft.getOrPutArray(ScriptBuilder.SPEECH)
          .put(
            JSONObject()
              .put("when", ScriptBuilder.WHEN_FOCUS)
              .put("match", JSONObject((rule.optJSONObject("match") ?: JSONObject()).toString()))
              .put("edits", JSONArray())
          )
      }
      val speechIndex = draft()?.optJSONArray(ScriptBuilder.SPEECH)?.length()?.minus(1) ?: return@action
      open(ScriptSpeechChangeFragment::class.java) { putInt(ScriptSpeechChangeFragment.ARG_INDEX, speechIndex) }
    }

    val parts = rule.optJSONArray("read").objects()
    val readSummary =
      when {
        parts.isEmpty() -> notSet()
        sample != null -> ItemPattern.item(sample)?.let { ItemPattern.readText(it, rule.optJSONArray("read")) }
          ?: resources.getQuantityString(R.plurals.script_builder_read_parts, parts.size, parts.size)
        else -> resources.getQuantityString(R.plurals.script_builder_read_parts, parts.size, parts.size)
      }
    changes.action(getString(R.string.script_builder_read), readSummary) {
      open(ScriptReadFragment::class.java) { putInt(ScriptReadFragment.ARG_INDEX, index) }
    }

    val actions = screen.category(R.string.script_builder_actions)
    rule.optJSONArray("actions").objects().forEachIndexed { actionIndex, action ->
      actions.action(action.optString("title"), actionText(context, action)) { editAction(actionIndex) }
    }
    actions.switch(getString(R.string.script_builder_button_actions), rule.optBoolean("buttonActions")) {
      edit { draft ->
        val edited = draft.getJSONArray("rules").getJSONObject(index)
        edited.putOrRemove("buttonActions", if (it) true else null)
        if (!it) edited.remove("skipButtons")
      }
    }
    if (rule.optBoolean("buttonActions")) {
      val skipped = rule.optJSONArray("skipButtons").strings()
      actions.action(getString(R.string.script_builder_skip_buttons), skipped.joinToString(", ").ifEmpty { notSet() }) {
        askText(
          getString(R.string.script_builder_skip_buttons),
          skipped.joinToString(", "),
          hint = getString(R.string.script_builder_skip_buttons_hint),
        ) { text, _, _ ->
          val texts = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }
          change("skipButtons", if (texts.isEmpty()) null else JSONArray(texts))
          null
        }
      }
    }
    actions.action(getString(R.string.script_builder_add_action)) { addAction() }
    actions.action(getString(R.string.script_builder_record_action)) {
      askText(getString(R.string.script_builder_record_action), hint = getString(R.string.script_builder_title_field)) { title, _, _ ->
        if (title.isEmpty()) return@askText getString(R.string.script_builder_name_empty)
        startRecording(title)
        null
      }
    }

    val manage = screen.category(R.string.script_builder_rule)
    manage.action(getString(R.string.script_move_up)) { edit { move(it.getJSONArray("rules"), index, -1) }.also { parentFragmentManager.popBackStack() } }
    manage.action(getString(R.string.script_move_down)) { edit { move(it.getJSONArray("rules"), index, 1) }.also { parentFragmentManager.popBackStack() } }
    manage.action(getString(R.string.script_builder_duplicate_rule)) {
      edit { draft ->
        val rules = draft.getJSONArray("rules")
        val copy = JSONObject(rules.getJSONObject(index).toString())
        // The copy goes right after the rule.
        val list = (0 until rules.length()).map { rules.get(it) }.toMutableList().apply { add(index + 1, copy) }
        draft.put("rules", JSONArray(list))
      }
      parentFragmentManager.popBackStack()
    }
    manage.action(getString(R.string.script_builder_copy_rule)) {
      val others = store.all().filter { it.id != scriptId && ScriptBuilder.isBuilt(store, it.id) }
      if (others.isEmpty()) {
        Toast.makeText(requireContext(), R.string.script_builder_no_other_scripts, Toast.LENGTH_SHORT).show()
        return@action
      }
      choose(getString(R.string.script_builder_copy_rule), others.map { it.manifest.name }) { which ->
        val target = others[which]
        val draft = ScriptBuilder.draft(store, target.id) ?: return@choose
        draft.getOrPutArray("rules").put(JSONObject(rule.toString()))
        try {
          ScriptBuilder.save(requireContext(), draft)
          Toast.makeText(requireContext(), getString(R.string.script_builder_copied, target.manifest.name), Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
          Toast.makeText(requireContext(), getString(R.string.script_builder_save_failed, e.message), Toast.LENGTH_LONG).show()
        } catch (e: ManifestException) {
          Toast.makeText(requireContext(), getString(R.string.script_builder_save_failed, e.message), Toast.LENGTH_LONG).show()
        }
      }
    }
    manage.action(getString(R.string.script_builder_delete_rule)) {
      confirm(getString(R.string.script_builder_delete_rule), R.string.script_builder_delete) {
        edit { it.getJSONArray("rules").removeAt(index) }
        parentFragmentManager.popBackStack()
      }
    }
  }

  /**
   * Changes worked out from the inspected item and the items like it: reading it as one item with
   * its buttons as actions, and choosing which of its parts are read.
   */
  private fun inspectedRows(screen: PreferenceGroup, rule: JSONObject, sample: JSONObject) {
    val category = screen.category(R.string.script_builder_inspected_category)
    val buttons = ItemPattern.buttons(sample)
    if (buttons.isNotEmpty()) {
      category.action(
        getString(R.string.script_builder_one_item_actions),
        getString(R.string.script_builder_actions_value, buttons.flatMap { listOfNotNull(it.title, it.longPressTitle) }.joinToString(", ")),
      ) {
        makeOneItem(sample, buttons)
      }
    }
    val grouped = rule.optBoolean("group")
    val text = ItemPattern.text(sample, grouped) ?: return
    category.action(getString(R.string.script_builder_choose_read), spokenText(text, speechEdits(rule))) {
      chooseRead(sample, grouped)
    }
  }

  /** What SandFly says for [text] after [edits]. */
  private fun spokenText(text: String, edits: JSONArray): String =
    when (val result = SpeechEdits.apply(text, edits)) {
      is SpeechEdits.Result.Speak -> result.text
      SpeechEdits.Result.Silent -> getString(R.string.script_builder_change_silence)
      SpeechEdits.Result.Unchanged -> text
    }

  /** Reads the item as one item, adds an action for each button in it, and stops reading the buttons. */
  private fun makeOneItem(sample: JSONObject, buttons: List<ItemPattern.Button>) {
    val item = ItemPattern.item(sample) ?: return
    edit { draft ->
      val rule = draft.getJSONArray("rules").getJSONObject(index)
      rule.put("group", true)
      val actions = rule.getOrPutArray("actions")
      val titles = actions.objects().map { it.optString("title").lowercase() }
      for (button in buttons) {
        if (button.title.lowercase() !in titles) {
          actions.put(JSONObject().put("title", button.title).put("click", JSONObject(button.target.toString())))
        }
        // A touch held on the button, as a double-tap and hold: apps such as Facebook open their
        // reactions only for that, not for the accessibility long click.
        button.longPressTitle?.takeIf { it.lowercase() !in titles }?.let {
          actions.put(JSONObject().put("title", it).put("touchHold", JSONObject(button.target.toString())))
        }
      }
      val parts = ItemPattern.parts(item, grouped = true)
      editSpeech(draft) { edits ->
        // The buttons' texts are found as the actions find the buttons, in every row.
        for (button in buttons) {
          val find = button.target.optString("matches").ifEmpty { null }
            ?: button.target.keys().asSequence().map { button.target.optString(it) }.firstOrNull()?.let { "^" + ItemPattern.quote(it) + "$" }
            ?: continue
          addEdit(edits, JSONObject().put("op", SpeechEdits.REMOVE_PARTS).put("find", find).put("regex", true))
        }
        if (parts.map { it.lowercase() }.distinct().size < parts.size) {
          addEdit(edits, JSONObject().put("op", SpeechEdits.REMOVE_REPEATS))
        }
      }
    }
  }

  /** Lists the parts SandFly reads for the inspected item; the ones unchecked are removed from every item like it. */
  private fun chooseRead(sample: JSONObject, grouped: Boolean) {
    val item = ItemPattern.item(sample) ?: return
    val rule = rule() ?: return
    val parts = ItemPattern.parts(item, grouped)
    val kept = keptParts(parts, speechEdits(rule))
    val checked = kept.copyOf()
    dialog()
      .setTitle(R.string.script_builder_choose_read)
      .setMultiChoiceItems(parts.toTypedArray(), checked) { _, which, isChecked -> checked[which] = isChecked }
      .setPositiveButton(android.R.string.ok) { _, _ ->
        edit { draft ->
          editSpeech(draft) { edits ->
            parts.indices.forEach { i ->
              if (kept[i] && !checked[i]) ItemPattern.removePart(sample, grouped, i)?.let { addEdit(edits, it) }
              if (!kept[i] && checked[i]) {
                val repeated = parts.take(i).any { it.equals(parts[i], ignoreCase = true) }
                val removing =
                  (0 until edits.length()).filter { j ->
                    val edit = edits.optJSONObject(j) ?: return@filter false
                    when (edit.optString("op")) {
                      SpeechEdits.REMOVE_PARTS, SpeechEdits.REMOVE ->
                        SpeechEdits.has(parts[i], edit.optString("find"), edit.optBoolean("regex"))
                      SpeechEdits.REMOVE_REPEATS -> repeated
                      else -> false
                    }
                  }
                removing.reversed().forEach(edits::remove)
              }
            }
          }
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  /** For each of [parts], whether it is still read after [edits]. */
  private fun keptParts(parts: List<String>, edits: JSONArray): BooleanArray {
    val text = parts.joinToString(", ")
    val result =
      when (val r = SpeechEdits.apply(text, edits)) {
        is SpeechEdits.Result.Speak -> r.text
        SpeechEdits.Result.Silent -> ""
        SpeechEdits.Result.Unchanged -> text
      }
    val left = result.split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toMutableList()
    return BooleanArray(parts.size) { i -> left.remove(parts[i].lowercase()) }
  }

  private fun addEdit(edits: JSONArray, edit: JSONObject) {
    val same = edits.objects().any { it.optString("op") == edit.optString("op") && it.optString("find") == edit.optString("find") }
    if (!same) edits.put(edit)
  }

  /** The index of the speech change for this rule's item in [draft], or -1. */
  private fun speechIndex(draft: JSONObject, match: JSONObject?): Int {
    val list = draft.optJSONArray(ScriptBuilder.SPEECH) ?: return -1
    val key = (match ?: JSONObject()).toString()
    return (0 until list.length()).firstOrNull { i ->
      val change = list.optJSONObject(i) ?: return@firstOrNull false
      change.optString("when") == ScriptBuilder.WHEN_FOCUS && change.optJSONObject("match")?.toString() == key
    } ?: -1
  }

  /** The edits of the speech change for this rule's item. */
  private fun speechEdits(rule: JSONObject): JSONArray {
    val draft = draft() ?: return JSONArray()
    val at = speechIndex(draft, rule.optJSONObject("match"))
    return if (at < 0) JSONArray() else ScriptBuilder.editsOf(draft.getJSONArray(ScriptBuilder.SPEECH).getJSONObject(at))
  }

  /** Changes the edits of the speech change for this rule's item, made when there is none. */
  private fun editSpeech(draft: JSONObject, change: (JSONArray) -> Unit) {
    val rule = draft.getJSONArray("rules").getJSONObject(index)
    val list = draft.getOrPutArray(ScriptBuilder.SPEECH)
    var at = speechIndex(draft, rule.optJSONObject("match"))
    val edits = if (at < 0) JSONArray() else ScriptBuilder.editsOf(list.getJSONObject(at))
    change(edits)
    if (at < 0) {
      if (edits.length() == 0) return
      list.put(
        JSONObject()
          .put("when", ScriptBuilder.WHEN_FOCUS)
          .put("match", JSONObject((rule.optJSONObject("match") ?: JSONObject()).toString()))
      )
      at = list.length() - 1
    }
    list.getJSONObject(at).put("edits", edits)
  }

  private fun textRow(group: PreferenceGroup, key: String, title: Int) {
    val old = rule()?.optString(key)?.ifEmpty { null }
    group.action(getString(title), old ?: notSet()) {
      editOrRemove(getString(title), old != null, { askOptional(getString(title), old) { change(key, it) } }) { change(key, null) }
    }
  }

  private fun hideRow(group: PreferenceGroup, key: String, title: Int, item: Int, all: Int) {
    val values = listOf<Any?>(null, true, "all")
    val names = listOf(notSet(), getString(item), getString(all))
    val old = rule()?.opt(key)
    val chosen = values.indexOf(old).coerceAtLeast(0)
    group.action(getString(title), names[chosen]) {
      choose(getString(title), names, chosen) { change(key, values[it]) }
    }
  }

  private fun readOrderRow(group: PreferenceGroup, rule: JSONObject) {
    val key = listOf("readBefore", "readAfter").firstOrNull(rule::has)
    val summary =
      when (key) {
        null -> notSet()
        else ->
          getString(
            if (key == "readBefore") R.string.script_builder_read_before_value else R.string.script_builder_read_after_value,
            ScriptBuilderFragment.describeQuery(requireContext(), rule.optJSONObject(key)),
          )
      }
    group.action(getString(R.string.script_builder_read_order), summary) {
      val names =
        listOf(notSet(), getString(R.string.script_builder_read_before), getString(R.string.script_builder_read_after))
      choose(getString(R.string.script_builder_read_order), names, listOf(null, "readBefore", "readAfter").indexOf(key)) { which ->
        when (which) {
          0 -> edit { it.getJSONArray("rules").getJSONObject(index).apply { remove("readBefore"); remove("readAfter") } }
          else -> {
            val chosen = if (which == 1) "readBefore" else "readAfter"
            val other = if (which == 1) "readAfter" else "readBefore"
            edit { draft ->
              val edited = draft.getJSONArray("rules").getJSONObject(index)
              val target = edited.optJSONObject(key ?: chosen) ?: JSONObject()
              edited.remove(other)
              edited.put(chosen, target)
            }
            openQuery(path(chosen))
          }
        }
      }
    }
  }

  private fun queryRow(group: PreferenceGroup, rule: JSONObject, key: String, title: Int) {
    val query = rule.optJSONObject(key)
    val summary = query?.let { ScriptBuilderFragment.describeQuery(requireContext(), it) } ?: notSet()
    group.action(getString(title), summary) {
      if (query == null) {
        openQuery(path(key))
      } else {
        choose(getString(title), listOf(getString(R.string.script_builder_item), getString(R.string.script_builder_remove))) {
          if (it == 0) openQuery(path(key)) else change(key, null)
        }
      }
    }
  }

  private fun actionKinds(): List<CharSequence> =
    listOf(
      getString(R.string.script_builder_action_click),
      getString(R.string.script_builder_action_long_click),
      getString(R.string.macro_step_touch_hold),
      getString(R.string.script_builder_action_focus),
      getString(R.string.script_builder_action_scroll_forward),
      getString(R.string.script_builder_action_scroll_backward),
      getString(R.string.script_builder_action_speak_text),
      getString(R.string.script_builder_action_speak_item),
    )

  private fun addAction() {
    askText(getString(R.string.script_builder_add_action), hint = getString(R.string.script_builder_title_field)) { title, _, _ ->
      if (title.isEmpty()) return@askText getString(R.string.script_builder_name_empty)
      chooseActionKind(JSONObject().put("title", title), null)
      null
    }
  }

  /** Asks what [action] does, and saves it at [actionIndex], or adds it when that is null. */
  private fun chooseActionKind(action: JSONObject, actionIndex: Int?) {
    choose(action.optString("title"), actionKinds()) { which ->
      ScriptBuilder.ACTION_KINDS.forEach(action::remove)
      fun saveAction(): Int {
        var at = actionIndex ?: -1
        edit { draft ->
          val list = draft.getJSONArray("rules").getJSONObject(index).getOrPutArray("actions")
          if (actionIndex == null) {
            list.put(action)
            at = list.length() - 1
          } else {
            list.put(actionIndex, action)
          }
        }
        return at
      }
      when (which) {
        ScriptBuilder.ACTION_KINDS.indexOf("speak") ->
          askText(getString(R.string.script_builder_action_speak_text)) { text, _, _ ->
            if (text.isEmpty()) return@askText getString(R.string.script_builder_name_empty)
            action.put("speak", text)
            saveAction()
            null
          }
        ScriptBuilder.ACTION_KINDS.indexOf("speak") + 1 -> {
          action.put("speak", JSONObject().put("textOf", JSONObject()))
          val at = saveAction()
          openQuery(path("actions", at, "speak", "textOf"))
        }
        else -> {
          val key = ScriptBuilder.ACTION_KINDS[which]
          action.put(key, JSONObject())
          val at = saveAction()
          openQuery(path("actions", at, key))
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    // Coming back from the app ends a recording of one of this rule's actions.
    val recording = ScriptActionRecorder.recordingFor(scriptId, index) ?: return
    val steps = ScriptActionRecorder.stop(requireContext())
    if (steps.isEmpty()) {
      Toast.makeText(requireContext(), R.string.script_builder_nothing_recorded, Toast.LENGTH_LONG).show()
      return
    }
    val title = recording.title
    dialog()
      .setTitle(title)
      .setMessage(steps.joinToString("\n") { stepText(requireContext(), it) })
      .setPositiveButton(R.string.script_builder_save) { _, _ ->
        edit { draft ->
          draft.getJSONArray("rules").getJSONObject(index).getOrPutArray("actions")
            .put(JSONObject().put("title", title).put("steps", JSONArray(steps)))
        }
      }
      .setNeutralButton(R.string.script_builder_record_again) { _, _ -> startRecording(title) }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  /**
   * Records the action [title] as the user does it in the rule's app, which is opened. The
   * recording ends when this screen is opened again, from its notification or by going back.
   */
  private fun startRecording(title: String) {
    val context = requireContext()
    val match = rule()?.optJSONObject("match") ?: return
    try {
      ScriptActionRecorder.start(context, scriptId, index, title, match)
    } catch (e: ManifestException) {
      Toast.makeText(context, getString(R.string.script_builder_save_failed, e.message), Toast.LENGTH_LONG).show()
      return
    }
    Toast.makeText(context, R.string.script_builder_recording_toast, Toast.LENGTH_LONG).show()
    val apps = draft()?.optJSONArray("apps")
    val app = apps?.opt(0)?.let { (it as? JSONObject)?.optString("package") ?: it.toString() }?.ifEmpty { null } ?: return
    context.packageManager.getLaunchIntentForPackage(app)?.let { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
  }

  private fun editAction(actionIndex: Int) {
    val action = rule()?.optJSONArray("actions")?.optJSONObject(actionIndex) ?: return
    if (action.has("steps")) return editStepsAction(actionIndex, action)
    val target = actionTargetPath(action)
    val options =
      listOfNotNull(
        getString(R.string.script_builder_rename),
        getString(R.string.script_builder_action_kind),
        target?.let { getString(R.string.script_builder_item) },
        getString(if (action.optBoolean("first")) R.string.script_builder_action_after_app else R.string.script_builder_action_first),
        getString(R.string.script_builder_remove),
      )
    choose(action.optString("title"), options) { which ->
      when (options[which]) {
        getString(R.string.script_builder_rename) ->
          askText(getString(R.string.script_builder_rename), action.optString("title")) { title, _, _ ->
            if (title.isEmpty()) return@askText getString(R.string.script_builder_name_empty)
            edit { actionAt(it, actionIndex).put("title", title) }
            null
          }
        getString(R.string.script_builder_action_kind) -> chooseActionKind(action, actionIndex)
        getString(R.string.script_builder_item) -> target?.let { openQuery(path("actions", actionIndex, *it.toTypedArray())) }
        getString(R.string.script_builder_remove) ->
          edit { it.getJSONArray("rules").getJSONObject(index).getJSONArray("actions").removeAt(actionIndex) }
        else -> edit { actionAt(it, actionIndex).putOrRemove("first", if (action.optBoolean("first")) null else true) }
      }
    }
  }

  /** The options of an action made of steps: each step's item, recording it again, and the rest. */
  private fun editStepsAction(actionIndex: Int, action: JSONObject) {
    val steps = action.optJSONArray("steps").objects()
    val options =
      listOf(getString(R.string.script_builder_rename)) +
        steps.map { stepText(requireContext(), it) } +
        listOf(
          getString(R.string.script_builder_record_again),
          getString(if (action.optBoolean("first")) R.string.script_builder_action_after_app else R.string.script_builder_action_first),
          getString(R.string.script_builder_remove),
        )
    choose(action.optString("title"), options) { which ->
      val stepIndex = which - 1
      when {
        which == 0 ->
          askText(getString(R.string.script_builder_rename), action.optString("title")) { title, _, _ ->
            if (title.isEmpty()) return@askText getString(R.string.script_builder_name_empty)
            edit { actionAt(it, actionIndex).put("title", title) }
            null
          }
        stepIndex < steps.size -> editStep(actionIndex, stepIndex, steps[stepIndex])
        stepIndex == steps.size -> {
          edit { it.getJSONArray("rules").getJSONObject(index).getJSONArray("actions").removeAt(actionIndex) }
          startRecording(action.optString("title"))
        }
        stepIndex == steps.size + 1 -> edit { actionAt(it, actionIndex).putOrRemove("first", if (action.optBoolean("first")) null else true) }
        else -> edit { it.getJSONArray("rules").getJSONObject(index).getJSONArray("actions").removeAt(actionIndex) }
      }
    }
  }

  /** A step's item, which can be changed, or the step removed. */
  private fun editStep(actionIndex: Int, stepIndex: Int, step: JSONObject) {
    val verb = ScriptBuilder.ACTION_KINDS.firstOrNull { it != "speak" && step.has(it) } ?: return
    choose(stepText(requireContext(), step), listOf(getString(R.string.script_builder_item), getString(R.string.script_builder_remove))) {
      if (it == 0) {
        // Alternatives are kept as a list; the screen changes the first.
        val target: List<Any> = if (step.opt(verb) is JSONArray) listOf(verb, 0) else listOf(verb)
        openQuery(path("actions", actionIndex, "steps", stepIndex, *target.toTypedArray()))
      } else {
        edit { draft ->
          val steps = actionAt(draft, actionIndex).getJSONArray("steps")
          steps.removeAt(stepIndex)
          if (steps.length() == 0) draft.getJSONArray("rules").getJSONObject(index).getJSONArray("actions").removeAt(actionIndex)
        }
      }
    }
  }

  /** A step of an action, in words: "Long-press the item", "Press Add to queue". */
  private fun stepText(context: Context, step: JSONObject): String {
    val verb = ScriptBuilder.ACTION_KINDS.firstOrNull { it != "speak" && step.has(it) } ?: return notSet()
    val queries =
      when (val target = step.opt(verb)) {
        is JSONArray -> target.objects()
        is JSONObject -> listOf(target)
        else -> emptyList()
      }
    val onScreen = step.optString("where") == "screen"
    val long = verb == "longClick" || verb == "touchHold"
    if (!onScreen && queries.singleOrNull()?.length() == 0) {
      return getString(if (long) R.string.script_builder_step_long_click_item else R.string.script_builder_step_click_item)
    }
    val name = queries.joinToString(" / ") { ScriptBuilderFragment.describeQuery(context, it) }
    return when (verb) {
      "click", "longClick", "touchHold" ->
        getString(
          when {
            onScreen && long -> R.string.script_builder_step_long_click_screen
            onScreen -> R.string.script_builder_step_click_screen
            long -> R.string.script_builder_step_long_click_in_item
            else -> R.string.script_builder_step_click_in_item
          },
          name,
        )
      else -> "${actionKinds()[ScriptBuilder.ACTION_KINDS.indexOf(verb)]}: $name"
    }
  }

  private fun actionAt(draft: JSONObject, actionIndex: Int): JSONObject =
    draft.getJSONArray("rules").getJSONObject(index).getJSONArray("actions").getJSONObject(actionIndex)

  /** Where the item [action] acts on is, inside the action, or null for one that speaks a text. */
  private fun actionTargetPath(action: JSONObject): List<Any>? {
    val key = ScriptBuilder.ACTION_KINDS.firstOrNull(action::has) ?: return null
    return when {
      key != "speak" -> listOf(key)
      action.optJSONObject("speak")?.has("textOf") == true -> listOf("speak", "textOf")
      else -> null
    }
  }

  private fun actionText(context: Context, action: JSONObject): String {
    if (action.has("steps")) {
      val steps = action.optJSONArray("steps").objects().joinToString("; ") { stepText(context, it) }
      return if (action.optBoolean("first")) "$steps, ${getString(R.string.script_builder_action_first)}" else steps
    }
    val key = ScriptBuilder.ACTION_KINDS.firstOrNull(action::has) ?: return notSet()
    val kind = actionKinds()[ScriptBuilder.ACTION_KINDS.indexOf(key) + if (key == "speak" && action.opt("speak") is JSONObject) 1 else 0]
    val target = actionTargetPath(action)?.let { Paths.get(action, it) as? JSONObject }
    val what =
      when {
        target != null -> "$kind: ${ScriptBuilderFragment.describeQuery(context, target)}"
        key == "speak" -> "$kind: ${action.optString("speak")}"
        else -> kind.toString()
      }
    return if (action.optBoolean("first")) "$what, ${getString(R.string.script_builder_action_first)}" else what
  }

  companion object {
    const val ARG_INDEX = "rule_index"
  }
}

/**
 * How many items [query] names of the inspected item and the items like it, or null when it doesn't
 * name the inspected item or none were found like it.
 */
fun likeCountText(context: Context, query: JSONObject?, sample: JSONObject?): String? {
  val item = ItemPattern.item(sample) ?: return null
  if (query == null || ItemPattern.similarCount(sample) == 0 || !ItemPattern.tellsItem(query, item)) return null
  val others = ItemPattern.count(query, sample) - 1
  return context.resources.getQuantityString(R.plurals.script_builder_like_count, others, others)
}

/** The item a rule, action or reading control names: the tests an item must pass. */
/**
 * What is read for a rule's item, in parts taken from it: each from a place in the item, an item
 * found in it, or its own text, with a text before it, and taken from the item above when empty.
 */
class ScriptReadFragment : ScriptBuilderBaseFragment() {
  private val index: Int by lazy { requireArguments().getInt(ARG_INDEX) }

  private fun rule(): JSONObject? = draft()?.optJSONArray("rules")?.optJSONObject(index)

  private fun editParts(change: (JSONArray) -> Unit) = edit { draft ->
    val rule = draft.getJSONArray("rules").getJSONObject(index)
    val parts = rule.getOrPutArray("read")
    change(parts)
    if (parts.length() == 0) rule.remove("read")
  }

  override fun getTitle(): CharSequence = getText(R.string.script_builder_read)

  override fun build(screen: PreferenceGroup) {
    val rule = rule() ?: return
    val context = requireContext()
    val sample =
      ItemPattern.samples(ItemInspector.lastSample(context)).firstOrNull { sample ->
        rule.optJSONObject("match")?.let { match -> ItemPattern.item(sample)?.let { ItemPattern.tellsItem(match, it) } } == true
      }
    val item = ItemPattern.item(sample)
    item?.let { ItemPattern.readText(it, rule.optJSONArray("read")) }?.let {
      screen.action(it) {}.isSelectable = false
    }
    val parts = rule.optJSONArray("read").objects()
    parts.forEachIndexed { i, part ->
      val value = item?.let { ItemPattern.readText(it, JSONArray().put(JSONObject(part.toString()).apply { remove("before") })) }
      screen.action(partTitle(context, part), value) { editPart(i, part) }
    }
    screen.action(getString(R.string.script_builder_read_add)) { addPart(item) }
  }

  private fun partTitle(context: Context, part: JSONObject): String {
    val path = part.optJSONArray("path")
    val from = part.optJSONObject("from")
    val source =
      when {
        path != null -> getString(R.string.script_builder_read_place, (0 until path.length()).joinToString(".") { path.optInt(it).toString() })
        from != null && from.length() == 0 -> getString(R.string.script_builder_read_own)
        else -> ScriptBuilderFragment.describeQuery(context, from)
      }
    return listOfNotNull(part.optString("before").ifEmpty { null }, source).joinToString(" ")
  }

  /** Adds a part: one of the inspected item's texts by its place, its own text, or an item found in it. */
  private fun addPart(item: JSONObject?) {
    val choices = item?.let(ItemPattern::readChoices).orEmpty()
    val names =
      listOf(getString(R.string.script_builder_read_own)) + choices.map { it.second } + getString(R.string.script_builder_item)
    choose(getString(R.string.script_builder_read_add), names) { which ->
      when {
        which == 0 -> editParts { it.put(JSONObject().put("from", JSONObject())) }
        which <= choices.size -> editParts { it.put(JSONObject().put("path", JSONArray(choices[which - 1].first))) }
        else -> {
          editParts { it.put(JSONObject().put("from", JSONObject())) }
          val at = rule()?.optJSONArray("read")?.length()?.minus(1) ?: return@choose
          // An empty item to fill in, in place of the item's own text.
          edit { draft -> Paths.put(draft, listOf("rules", index, "read", at, "from"), JSONObject()) }
          openQuery(listOf("rules", index, "read", at, "from"))
        }
      }
    }
  }

  private fun editPart(i: Int, part: JSONObject) {
    val yes = getString(R.string.script_builder_yes)
    val no = getString(R.string.script_builder_no)
    val options =
      listOf(
        getString(R.string.script_builder_read_text_before),
        getString(R.string.script_builder_state_choice, getString(R.string.script_builder_read_above), if (part.optBoolean("fromAbove")) yes else no),
        getString(R.string.action_move_up),
        getString(R.string.action_move_down),
        getString(R.string.script_builder_remove),
      ) + listOfNotNull(part.optJSONObject("from")?.takeIf { it.length() > 0 }?.let { getString(R.string.script_builder_item) })
    choose(partTitle(requireContext(), part), options) { which ->
      when (which) {
        0 ->
          askOptional(getString(R.string.script_builder_read_text_before), part.optString("before").ifEmpty { null }) { text ->
            editParts { it.getJSONObject(i).putOrRemove("before", text) }
          }
        1 -> editParts { it.getJSONObject(i).putOrRemove("fromAbove", if (part.optBoolean("fromAbove")) null else true) }
        2 -> editParts { move(it, i, -1) }
        3 -> editParts { move(it, i, 1) }
        4 -> editParts { it.removeAt(i) }
        else -> openQuery(listOf("rules", index, "read", i, "from"))
      }
    }
  }

  companion object {
    const val ARG_INDEX = "rule_index"
  }
}

class ScriptQueryFragment : ScriptBuilderBaseFragment() {
  private val path: List<Any> by lazy { Paths.decode(requireArguments().getString(ARG_PATH).orEmpty()) }

  /** inside and has hold a query that can't hold inside or has. */
  private val nested: Boolean
    get() = path.any { it == "inside" || it == "has" }

  private fun query(): JSONObject? = Paths.get(draft(), path) as? JSONObject

  /** The path of the item of the rule this screen is in, or null outside a rule's item. */
  private val ruleMatch: List<Any>? by lazy {
    path.take(3).takeIf { it.size == 3 && it[0] == "rules" && it[2] == "match" }
  }

  /** A rule's speech change for its item keeps naming the item when the item changes. */
  override fun edit(change: (JSONObject) -> Unit) =
    super.edit { draft ->
      val match = ruleMatch
      val old = match?.let { (Paths.get(draft, it) as? JSONObject)?.toString() }
      change(draft)
      val new = match?.let { Paths.get(draft, it) as? JSONObject } ?: return@edit
      if (old == null || old == new.toString()) return@edit
      for (speech in draft.optJSONArray(ScriptBuilder.SPEECH).objects()) {
        if (speech.optString("when") == ScriptBuilder.WHEN_FOCUS && speech.optJSONObject("match")?.toString() == old) {
          speech.put("match", JSONObject(new.toString()))
        }
      }
    }

  private fun change(key: String, value: Any?) = edit { draft ->
    (Paths.get(draft, path) as? JSONObject)?.putOrRemove(key, value)
  }

  override fun getTitle(): CharSequence = getText(R.string.script_builder_item)

  override fun build(screen: PreferenceGroup) {
    val query = query() ?: return
    val context = requireContext()
    screen.action(ScriptBuilderFragment.describeQuery(context, query)) {}.isSelectable = false
    val sample = ItemInspector.lastSample(context)
    val rowSample = ItemPattern.rowSample(sample)
    ItemPattern.likeQuery(rowSample)?.let { like ->
      screen.action(
        getString(R.string.script_builder_rule_for_row),
        listOfNotNull(ScriptBuilderFragment.describeQuery(context, like), likeCountText(context, like, rowSample)).joinToString("\n"),
      ) {
        edit { draft ->
          val target = Paths.get(draft, path) as? JSONObject ?: return@edit
          for (key in QUERY_KEYS + "hasDepth") target.remove(key)
          like.keys().forEach { target.put(it, like.get(it)) }
        }
      }
    }
    ItemPattern.likeQuery(sample)?.let { like ->
      screen.action(
        getString(R.string.script_builder_like_inspected),
        listOfNotNull(ScriptBuilderFragment.describeQuery(context, like), likeCountText(context, like, sample)).joinToString("\n"),
      ) {
        edit { draft ->
          val target = Paths.get(draft, path) as? JSONObject ?: return@edit
          for (key in QUERY_KEYS + "hasDepth") target.remove(key)
          like.keys().forEach { target.put(it, like.get(it)) }
        }
      }
    }
    ItemInspector.lastQuery(context)?.let { inspected ->
      screen.action(
        getString(R.string.script_builder_use_inspected),
        ScriptBuilderFragment.describeQuery(context, inspected),
      ) {
        edit { draft ->
          val target = Paths.get(draft, path) as? JSONObject ?: return@edit
          for (key in QUERY_KEYS + "hasDepth") target.remove(key)
          inspected.keys().forEach { target.put(it, inspected.get(it)) }
        }
      }
    }
    screen.action(getString(R.string.script_builder_text), ScriptBuilderFragment.textCondition(context, query) ?: notSet()) {
      editOrRemove(getString(R.string.script_builder_text), ScriptBuilderFragment.textCondition(context, query) != null, { askTextCondition(query) }) {
        removeKeys("contains", "matches", "field")
      }
    }
    screen.action(getString(R.string.macro_item_kind), ScriptBuilderFragment.kindsOf(query).joinToString(", ") { getString(it.title) }.ifEmpty { notSet() }) {
      editOrRemove(getString(R.string.macro_item_kind), query.has("kind"), { askKinds(query) }) { removeKeys("kind") }
    }
    val exact = EXACT_FIELDS.filter { (key, _) -> query.optString(key).isNotEmpty() }
    screen.action(
      getString(R.string.script_builder_exact_text),
      exact.joinToString(", ") { (key, field) -> getString(R.string.macro_field_value, query.optString(key), getString(field.title)) }.ifEmpty { notSet() },
    ) {
      editOrRemove(getString(R.string.script_builder_exact_text), exact.isNotEmpty(), { askExactText(query) }) {
        removeKeys(*EXACT_FIELDS.map { it.first }.toTypedArray())
      }
    }
    textRow(screen, query, "id", R.string.script_builder_id)
    textRow(screen, query, "parentId", R.string.script_builder_parent_id)
    textRow(screen, query, "className", R.string.script_builder_class)
    val states = STATE_TITLES.filter { (key, _) -> query.has(key) }
    screen.action(
      getString(R.string.script_builder_states),
      states.joinToString(", ") { (key, title) ->
        getString(if (query.optBoolean(key)) R.string.script_builder_state_yes else R.string.script_builder_state_no, getString(title))
      }.ifEmpty { notSet() },
    ) {
      askState(query)
    }
    screen.action(getString(R.string.script_builder_role), query.optString("role").ifEmpty { notSet() }) {
      val roles = ScriptRoles.names.sorted()
      choose(getString(R.string.script_builder_role), listOf(notSet()) + roles, roles.indexOf(query.optString("role")) + 1) {
        change("role", roles.getOrNull(it - 1))
      }
    }
    if (!nested) {
      insideRows(screen, query)
      subQueryRow(screen, query, "has", R.string.script_builder_has)
      if (query.optJSONObject("has") != null) {
        val depth = query.optInt("hasDepth", NodeQuery.MAX_HAS_DEPTH)
        screen.action(getString(R.string.script_builder_has_depth), resources.getQuantityString(R.plurals.script_builder_levels, depth, depth)) {
          val choices = (1..NodeQuery.MAX_HAS_DEPTH).toList()
          choose(getString(R.string.script_builder_has_depth), choices.map { resources.getQuantityString(R.plurals.script_builder_levels, it, it) }, depth - 1) {
            change("hasDepth", choices[it].takeIf { level -> level < NodeQuery.MAX_HAS_DEPTH })
          }
        }
      }
    }
  }

  private fun removeKeys(vararg keys: String) = edit { draft ->
    val target = Paths.get(draft, path) as? JSONObject ?: return@edit
    keys.forEach { target.remove(it) }
  }

  /** A row for each item the item is inside, and one that adds another, up to four. */
  private fun insideRows(group: PreferenceGroup, query: JSONObject) {
    val inside: List<JSONObject> =
      when (val value = query.opt("inside")) {
        is JSONObject -> listOf(value)
        is JSONArray -> value.objects()
        else -> emptyList()
      }
    inside.forEachIndexed { i, sub ->
      group.action(getString(R.string.script_builder_inside), ScriptBuilderFragment.describeQuery(requireContext(), sub)) {
        choose(getString(R.string.script_builder_inside), listOf(getString(R.string.script_builder_item), getString(R.string.script_builder_remove))) {
          if (it == 0) {
            insideAsList()
            openQuery(path + "inside" + i)
          } else {
            edit { draft ->
              val target = Paths.get(draft, path) as? JSONObject ?: return@edit
              val list = (target.opt("inside") as? JSONArray)?.objects() ?: listOfNotNull(target.optJSONObject("inside"))
              val left = list.filterIndexed { j, _ -> j != i }
              if (left.isEmpty()) target.remove("inside") else target.put("inside", JSONArray(left))
            }
          }
        }
      }
    }
    if (inside.isEmpty()) {
      group.action(getString(R.string.script_builder_inside), notSet()) {
        insideAsList()
        openQuery(path + "inside" + 0)
      }
    } else if (inside.size < MAX_INSIDE) {
      group.action(getString(R.string.script_builder_add_inside)) {
        insideAsList()
        openQuery(path + "inside" + inside.size)
      }
    }
  }

  /** Keeps the items the item is inside as a list, so that each has an index in the path. */
  private fun insideAsList() {
    val query = query() ?: return
    if (query.opt("inside") is JSONObject) edit { draft ->
      val target = Paths.get(draft, path) as? JSONObject ?: return@edit
      target.put("inside", JSONArray().put(target.getJSONObject("inside")))
    }
    if (query.opt("inside") == null) edit { draft -> (Paths.get(draft, path) as? JSONObject)?.put("inside", JSONArray()) }
  }

  /** Asks for a text the item has exactly, in one of its texts; an empty text clears that one. */
  private fun askExactText(query: JSONObject) {
    val fields = EXACT_FIELDS.map { it.second }
    val first = EXACT_FIELDS.firstOrNull { (key, _) -> query.optString(key).isNotEmpty() }
    val choice =
      DialogChoice(getString(R.string.macro_match_in), fields.map { getString(it.title) }, EXACT_FIELDS.indexOf(first).coerceAtLeast(0))
    askText(
      getString(R.string.script_builder_exact_text),
      first?.let { query.optString(it.first) }.orEmpty(),
      hint = getString(R.string.script_builder_text),
      choice = choice,
      inputType = InputType.TYPE_CLASS_TEXT,
    ) { text, _, _ ->
      change(EXACT_FIELDS[choice.selected].first, text.ifEmpty { null })
      null
    }
  }

  /** Asks for a state and whether the item must have it. */
  private fun askState(query: JSONObject) {
    val names = STATE_TITLES.map { (key, title) ->
      val value =
        when {
          !query.has(key) -> notSet()
          query.optBoolean(key) -> getString(R.string.script_builder_yes)
          else -> getString(R.string.script_builder_no)
        }
      getString(R.string.script_builder_state_choice, getString(title), value)
    }
    choose(getString(R.string.script_builder_states), names) { which ->
      val (key, title) = STATE_TITLES[which]
      val values = listOf(null, true, false)
      val labels = listOf(notSet(), getString(R.string.script_builder_yes), getString(R.string.script_builder_no))
      val current = if (query.has(key)) query.optBoolean(key) else null
      choose(getString(title), labels, values.indexOf(current)) { change(key, values[it]) }
    }
  }

  private fun textRow(group: PreferenceGroup, query: JSONObject, key: String, title: Int) {
    val old = query.optString(key).ifEmpty { null }
    group.action(getString(title), old ?: notSet()) {
      editOrRemove(getString(title), old != null, { askOptional(getString(title), old) { change(key, it) } }) { change(key, null) }
    }
  }

  private fun subQueryRow(group: PreferenceGroup, query: JSONObject, key: String, title: Int) {
    val sub = query.optJSONObject(key)
    group.action(getString(title), sub?.let { ScriptBuilderFragment.describeQuery(requireContext(), it) } ?: notSet()) {
      if (sub == null) {
        openQuery(path + key)
      } else {
        choose(getString(title), listOf(getString(R.string.script_builder_item), getString(R.string.script_builder_remove))) {
          if (it == 0) openQuery(path + key) else change(key, null)
        }
      }
    }
  }

  /** Asks for the text an item has, as a macro test does: the text, Match in and Regular expression. */
  private fun askTextCondition(query: JSONObject) {
    val regex = query.has("matches")
    val old = if (regex) query.optString("matches") else query.optString("contains")
    val field = ScriptBuilderFragment.fieldOf(query)
    val choice =
      DialogChoice(
        getString(R.string.macro_match_in),
        listOf(getString(R.string.macro_field_any)) + Macros.Field.entries.map { getString(it.title) },
        field?.let { it.ordinal + 1 } ?: 0,
      )
    askText(
      getString(R.string.script_builder_text),
      old,
      hint = getString(R.string.script_builder_text),
      checkLabels = listOf(getString(R.string.notification_regex)),
      checked = listOf(regex),
      choice = choice,
      inputType = InputType.TYPE_CLASS_TEXT,
    ) { text, _, checks ->
      val isRegex = checks.firstOrNull() == true
      if (text.isNotEmpty() && isRegex && Macros.compilePattern(text) == null) {
        return@askText getString(R.string.macro_invalid_regex)
      }
      val chosenField = Macros.Field.entries.getOrNull(choice.selected - 1)
      edit { draft ->
        val target = Paths.get(draft, path) as? JSONObject ?: return@edit
        target.remove("contains")
        target.remove("matches")
        target.remove("field")
        if (text.isNotEmpty()) {
          target.put(if (isRegex) "matches" else "contains", text)
          chosenField?.let { target.put("field", ScriptBuilderFragment.fieldKey(it)) }
        }
      }
      null
    }
  }

  private fun askKinds(query: JSONObject) {
    val kinds = Macros.Kind.entries
    val chosen = ScriptBuilderFragment.kindsOf(query)
    val checked = kinds.map { it in chosen }.toBooleanArray()
    dialog()
      .setTitle(R.string.macro_item_kind)
      .setMultiChoiceItems(kinds.map { getString(it.title) }.toTypedArray(), checked) { _, which, isChecked ->
        checked[which] = isChecked
      }
      .setPositiveButton(android.R.string.ok) { _, _ ->
        val names = kinds.filterIndexed { i, _ -> checked[i] }.map { it.scriptName }
        change(
          "kind",
          when (names.size) {
            0 -> null
            1 -> names[0]
            else -> JSONArray(names)
          },
        )
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  companion object {
    const val ARG_PATH = "query_path"

    /** The fields of a query this screen sets, and Use the inspected item replaces. */
    private val QUERY_KEYS =
      listOf("id", "contains", "matches", "field", "kind", "className", "role", "has", "text", "contentDescription")

    /** The texts an item can have exactly, by their query fields. */
    private val EXACT_FIELDS =
      listOf(
        "text" to Macros.Field.TEXT,
        "contentDescription" to Macros.Field.DESCRIPTION,
        "hint" to Macros.Field.HINT,
        "stateDescription" to Macros.Field.STATE,
      )

    /** The states a query can require, by their query fields. */
    private val STATE_TITLES =
      listOf(
        "clickable" to R.string.macro_kind_clickable,
        "longClickable" to R.string.macro_kind_long_clickable,
        "checkable" to R.string.macro_kind_checkable,
        "checked" to R.string.macro_kind_checked,
        "selected" to R.string.macro_kind_selected,
        "enabled" to R.string.macro_kind_enabled,
        "editable" to R.string.macro_kind_editable,
        "scrollable" to R.string.macro_kind_scrollable,
        "heading" to R.string.macro_kind_heading,
        "focusable" to R.string.script_builder_state_focusable,
        "focused" to R.string.script_builder_state_focused,
        "visible" to R.string.script_builder_state_visible,
        "password" to R.string.script_builder_state_password,
      )

    private const val MAX_INSIDE = 4
  }
}

/** Places in a draft, such as rules, 2, match: keys of objects and indexes of lists. */
object Paths {
  fun encode(path: List<Any>): String = JSONArray(path).toString()

  fun decode(text: String): List<Any> {
    val array = JSONArray(text)
    return (0 until array.length()).map { array.get(it) }
  }

  fun get(root: Any?, path: List<Any>): Any? =
    path.fold(root) { value, key ->
      when {
        value is JSONObject && key is String -> value.opt(key)
        value is JSONArray && key is Int -> value.opt(key)
        else -> null
      }
    }

  /** Puts [value] at [path] in [root], when the place above it is there. */
  fun put(root: JSONObject, path: List<Any>, value: Any) {
    val parent = get(root, path.dropLast(1))
    when (val key = path.last()) {
      is String -> (parent as? JSONObject)?.put(key, value)
      is Int -> (parent as? JSONArray)?.put(key, value)
    }
  }
}

fun JSONObject.getOrPutArray(key: String): JSONArray =
  optJSONArray(key) ?: JSONArray().also { put(key, it) }

/**
 * Adds a rule for the item last inspected to a built script, or to a new one for its app, and
 * opens the rule. Opened from the Item details window.
 */
class ScriptRulePickFragment : NotificationCodeFragment(R.string.script_builder_rule_for_item) {
  private val store: ScriptStore by lazy { ScriptStore.get(requireContext()) }

  /** Whether the rule is for the list row the inspected item is in, when it is in one. */
  private var forRow = true

  override fun build(screen: PreferenceGroup) {
    val context = requireContext()
    val inspected = ItemInspector.lastSample(context)
    val rowSample = ItemPattern.rowSample(inspected)?.takeIf { ItemPattern.likeQuery(it) != null }
    // A rule for the list row the inspected item is in, such as a comment, unless the person chose
    // the item itself.
    val forRow = rowSample != null && forRow
    val sample = if (forRow) rowSample else inspected
    // Every item like the inspected one, such as each row of a list, or else the item itself.
    val query = ItemPattern.likeQuery(sample) ?: ItemInspector.lastQuery(context)
    if (query == null) {
      screen.action(getString(R.string.script_builder_no_inspected)) {}.isSelectable = false
      return
    }
    if (rowSample != null) {
      val choices = listOf(getString(R.string.script_builder_rule_for_row), getString(R.string.script_builder_rule_for_item_choice))
      screen.action(getString(R.string.script_builder_rule_for), choices[if (forRow) 0 else 1]) {
        choose(getString(R.string.script_builder_rule_for), choices, if (forRow) 0 else 1) {
          this.forRow = it == 0
          rebuild()
        }
      }
    }
    screen.action(
      getString(R.string.script_builder_item),
      listOfNotNull(ScriptBuilderFragment.describeQuery(context, query), likeCountText(context, query, sample)).joinToString("\n"),
    ) {}.isSelectable = false
    val app = ItemInspector.lastApp(context)
    if (app != null) {
      screen.action(getString(R.string.script_builder_new_for_app, ScriptBuilderFragment.appText(context, app))) {
        val name = ScriptBuilderFragment.appText(context, app)
        val id =
          try {
            ScriptBuilder.create(context, name)
          } catch (e: IOException) {
            Toast.makeText(context, getString(R.string.script_builder_save_failed, e.message), Toast.LENGTH_LONG).show()
            return@action
          }
        addRule(id, query, app)
      }
    }
    val built = store.all().filter { ScriptBuilder.isBuilt(store, it.id) }
    if (built.isEmpty()) return
    val category = screen.category(R.string.script_builder_existing)
    // Scripts for the item's app first.
    val (forApp, others) = built.partition { script -> app != null && script.manifest.apps.any { it.packageName == app } }
    for (script in forApp + others) {
      category.action(script.manifest.name, requireContext().scriptSummary(script)) { addRule(script.id, query, app) }
    }
  }

  private fun addRule(scriptId: String, query: JSONObject, app: String?) {
    val context = requireContext()
    val draft = ScriptBuilder.draft(store, scriptId) ?: return
    val apps = draft.getOrPutArray("apps")
    val listed = (0 until apps.length()).map { apps.get(it).let { a -> (a as? JSONObject)?.optString("package") ?: a.toString() } }
    // A script for some apps runs in the item's app too; a script for all apps already does.
    val isNew = draft.optJSONArray("rules").objects().isEmpty() && listed.isEmpty() && draft.optJSONArray("commands").objects().isEmpty()
    if (app != null && app !in listed && (listed.isNotEmpty() || isNew)) apps.put(app)
    draft.getOrPutArray("rules").put(JSONObject().put("match", JSONObject(query.toString())))
    try {
      ScriptBuilder.save(context, draft)
    } catch (e: IOException) {
      Toast.makeText(context, getString(R.string.script_builder_save_failed, e.message), Toast.LENGTH_LONG).show()
      return
    }
    val index = draft.getJSONArray("rules").length() - 1
    onPreferenceTreeClick(
      Preference(context).apply {
        fragment = ScriptRuleFragment::class.java.name
        extras.putString(ScriptScreenFragment.ARG_ID, scriptId)
        extras.putInt(ScriptRuleFragment.ARG_INDEX, index)
      }
    )
  }
}

/** A command of a built script: its steps, and how it is offered by default. */
class ScriptBuiltCommandFragment : ScriptBuilderBaseFragment() {
  private val index: Int by lazy { requireArguments().getInt(ARG_INDEX) }

  private fun command(): JSONObject? = draft()?.optJSONArray(BuiltScriptMacros.COMMANDS)?.optJSONObject(index)

  private fun change(key: String, value: Any?) = edit { draft ->
    draft.getJSONArray(BuiltScriptMacros.COMMANDS).getJSONObject(index).putOrRemove(key, value)
  }

  override fun getTitle(): CharSequence = command()?.optString("title") ?: getText(R.string.script_builder_commands)

  override fun build(screen: PreferenceGroup) {
    val command = command() ?: return
    val context = requireContext()
    BuiltScriptMacros.init(context)
    val macroId = BuiltScriptMacros.idOf(scriptId, BuiltScriptMacros.COMMANDS, command.optString("id"))
    screen.link(getString(R.string.script_builder_steps), null, MacroFragment::class.java) {
      putString(MacroFragment.ARG_MACRO, macroId)
    }
    screen.link(getString(R.string.menu_assign), null, ScriptCommandFragment::class.java) {
      putString(ScriptScreenFragment.ARG_ID, scriptId)
      putString(ScriptCommandFragment.ARG_COMMAND, command.optString("id"))
    }
    val defaults = screen.category(R.string.script_builder_command_defaults)
    defaults.switch(getString(R.string.script_builder_command_menu), command.optBoolean("menu")) { change("menu", if (it) true else null) }
    defaults.switch(getString(R.string.script_builder_command_control), command.optBoolean("control")) {
      change("control", if (it) true else null)
    }
    val gesture = command.optString("gesture").ifEmpty { null }
    defaults.action(
      getString(R.string.script_builder_command_gesture),
      gesture?.let { ScriptInput.gestureTitle(context, it) } ?: notSet(),
    ) {
      val names = ScriptInput.gestureNames
      choose(
        getString(R.string.script_builder_command_gesture),
        listOf(notSet()) + names.map { ScriptInput.gestureTitle(context, it) },
        names.indexOf(gesture) + 1,
      ) {
        change("gesture", names.getOrNull(it - 1))
      }
    }
    val keys = command.optString("keys").ifEmpty { null }
    defaults.action(getString(R.string.script_builder_command_keys), keys ?: notSet()) {
      askOptional(getString(R.string.script_builder_command_keys), keys) { change("keys", it) }
    }
    val manage = screen.category(R.string.script_builder_script)
    manage.action(getString(R.string.script_builder_rename)) {
      askText(getString(R.string.script_builder_rename), command.optString("title")) { title, _, _ ->
        if (title.isEmpty()) return@askText getString(R.string.script_builder_name_empty)
        change("title", title)
        activity?.title = title
        null
      }
    }
    manage.action(getString(R.string.menu_delete)) {
      confirm(getString(R.string.macro_delete_confirm, command.optString("title")), R.string.menu_delete) {
        edit { it.getJSONArray(BuiltScriptMacros.COMMANDS).removeAt(index) }
        parentFragmentManager.popBackStack()
      }
    }
  }

  companion object {
    const val ARG_INDEX = "command_index"
  }
}

/** A speech change of a built script: what speech it changes, and how. */
class ScriptSpeechChangeFragment : ScriptBuilderBaseFragment() {
  private val index: Int by lazy { requireArguments().getInt(ARG_INDEX) }

  private fun speech(): JSONObject? = draft()?.optJSONArray(ScriptBuilder.SPEECH)?.optJSONObject(index)

  private fun change(key: String, value: Any?) = edit { draft ->
    draft.getJSONArray(ScriptBuilder.SPEECH).getJSONObject(index).putOrRemove(key, value)
  }

  override fun getTitle(): CharSequence = getText(R.string.script_builder_speech_change)

  override fun build(screen: PreferenceGroup) {
    val speech = speech() ?: return
    val context = requireContext()
    ScriptBuilder.entryProblemText(context, speech, ScriptBuilder.speechProblem(speech))?.let {
      screen.action(getString(R.string.script_builder_not_used, it)) {}.isSelectable = false
    }
    val whenKinds = ScriptBuilder.SPEECH_WHEN
    val whenNames =
      listOf(
        R.string.script_builder_speech_focus,
        R.string.script_builder_speech_notifications,
        R.string.script_builder_speech_announcements,
        R.string.script_builder_speech_all,
      ).map { getString(it) }
    val kind = speech.optString("when")
    screen.action(getString(R.string.script_builder_speech_which), whenNames.getOrNull(whenKinds.indexOf(kind)) ?: notSet()) {
      choose(getString(R.string.script_builder_speech_which), whenNames, whenKinds.indexOf(kind)) { which ->
        edit { draft ->
          val edited = draft.getJSONArray(ScriptBuilder.SPEECH).getJSONObject(index)
          edited.put("when", whenKinds[which])
          if (whenKinds[which] == ScriptBuilder.WHEN_FOCUS) {
            if (edited.optJSONObject("match") == null) edited.put("match", JSONObject())
          } else {
            edited.remove("match")
          }
          if (whenKinds[which] != ScriptBuilder.WHEN_NOTIFICATION && whenKinds[which] != ScriptBuilder.WHEN_ANNOUNCEMENT) {
            edited.remove("app")
          }
        }
      }
    }
    when (kind) {
      ScriptBuilder.WHEN_FOCUS ->
        screen.action(getString(R.string.script_builder_item), ScriptBuilderFragment.describeQuery(context, speech.optJSONObject("match"))) {
          openQuery(listOf(ScriptBuilder.SPEECH, index, "match"))
        }
      ScriptBuilder.WHEN_NOTIFICATION, ScriptBuilder.WHEN_ANNOUNCEMENT -> {
        val app = speech.optString("app").ifEmpty { null }
        screen.action(getString(R.string.script_builder_speech_app), app?.let { ScriptBuilderFragment.appText(context, it) } ?: getString(R.string.script_builder_all_apps)) {
          chooseApp(getString(R.string.script_builder_all_apps)) { packageName, _ -> change("app", packageName.ifEmpty { null }) }
        }
      }
    }
    val has = speech.optString("has").ifEmpty { null }
    screen.action(getString(R.string.script_builder_has_text_optional), has ?: notSet()) {
      askOptional(getString(R.string.script_builder_has_text_optional), has) { change("has", it) }
    }

    val edits = ScriptBuilder.editsOf(speech)
    val editsCategory = screen.category(R.string.script_builder_edits)
    edits.objects().forEachIndexed { i, edit ->
      val problem = SpeechEdits.problem(edit)
      editsCategory.action(editText(context, edit), problem?.let { getString(R.string.script_builder_not_used, it) }) {
        val choices =
          listOf(
            getString(R.string.notification_edit),
            getString(R.string.action_move_up),
            getString(R.string.action_move_down),
            getString(R.string.script_builder_remove),
          )
        choose(editText(context, edit), choices) { which ->
          when (which) {
            0 -> askEdit(edit.optString("op"), edit) { changed -> changeEdits { it.put(i, changed) } }
            1 -> changeEdits { move(it, i, -1) }
            2 -> changeEdits { move(it, i, 1) }
            else -> changeEdits { it.removeAt(i) }
          }
        }
      }
    }
    editsCategory.action(getString(R.string.script_builder_add_edit)) {
      val ops = SpeechEdits.OPS
      choose(getString(R.string.script_builder_add_edit), ops.map { getString(opTitle(it)) }) { which ->
        askEdit(ops[which], null) { added -> changeEdits { it.put(added) } }
      }
    }
    ItemInspector.lastText(context)?.let { sample ->
      val result =
        when (val applied = SpeechEdits.apply(sample, edits)) {
          is SpeechEdits.Result.Speak -> applied.text
          SpeechEdits.Result.Silent -> getString(R.string.script_builder_preview_silent)
          SpeechEdits.Result.Unchanged -> sample
        }
      editsCategory.action(getString(R.string.script_builder_preview), result) {}.isSelectable = false
    }
    screen.category(R.string.script_builder_script).action(getString(R.string.script_builder_remove)) {
      edit { it.getJSONArray(ScriptBuilder.SPEECH).removeAt(index) }
      parentFragmentManager.popBackStack()
    }
  }

  /** Changes the edits of this speech change, which then keep only edits, not the old single change. */
  private fun changeEdits(block: (JSONArray) -> Unit) = edit { draft ->
    val speech = draft.getJSONArray(ScriptBuilder.SPEECH).getJSONObject(index)
    val edits = ScriptBuilder.editsOf(speech)
    block(edits)
    speech.put("edits", edits)
    listOf("change", "text", "find").forEach(speech::remove)
  }

  /** Asks for the values of an edit of [op], with [old] filled in, and gives it to [then]. */
  private fun askEdit(op: String, old: JSONObject?, then: (JSONObject) -> Unit) {
    val title = getString(opTitle(op))
    val edit = JSONObject().put("op", op)
    fun findDialog(withSecond: Boolean, hint: Int) =
      askText(
        title,
        old?.optString("find").orEmpty(),
        hint = getString(hint),
        message = getString(if (op == SpeechEdits.REMOVE_PARTS) R.string.script_builder_parts_help else R.string.script_builder_regex_help),
        checkLabels = listOf(getString(R.string.macro_regex)),
        checked = listOf(old?.optBoolean("regex") == true),
        secondHint = if (withSecond) getString(R.string.script_builder_speech_replace_with) else null,
        secondText = old?.optString("with").orEmpty(),
        inputType = InputType.TYPE_CLASS_TEXT,
      ) { find, with, checks ->
        val regex = checks.firstOrNull() == true
        when {
          find.isEmpty() -> ""
          regex && Macros.compilePattern(find) == null -> getString(R.string.macro_invalid_regex)
          else -> null.also {
            edit.put("find", find)
            if (regex) edit.put("regex", true)
            if (withSecond) edit.put("with", with)
            then(edit)
          }
        }
      }
    when (op) {
      SpeechEdits.REMOVE -> findDialog(false, R.string.script_builder_speech_find)
      SpeechEdits.REPLACE -> findDialog(true, R.string.script_builder_speech_find)
      SpeechEdits.REMOVE_PARTS -> findDialog(false, R.string.script_builder_text)
      SpeechEdits.KEEP_PARTS ->
        askText(title, old?.optInt("count")?.takeIf { it > 0 }?.toString() ?: "1", message = getString(R.string.script_builder_parts_help), inputType = InputType.TYPE_CLASS_NUMBER) { text, _, _ ->
          val count = text.toIntOrNull()?.takeIf { it in 1..100 } ?: return@askText getString(R.string.macro_number_range, 1, 100)
          null.also { then(edit.put("count", count)) }
        }
      SpeechEdits.SAY, SpeechEdits.BEFORE, SpeechEdits.AFTER ->
        askText(title, old?.optString("text").orEmpty(), message = getString(R.string.script_builder_speech_text_help), inputType = InputType.TYPE_CLASS_TEXT) { text, _, _ ->
          if (text.isEmpty()) "" else null.also { then(edit.put("text", text)) }
        }
      else -> then(edit)
    }
  }

  companion object {
    const val ARG_INDEX = "speech_index"

    /** What [change] does to speech, in words: its edits. */
    fun changeText(context: Context, change: JSONObject): String =
      ScriptBuilder.editsOf(change).objects().joinToString("; ") { editText(context, it) }

    fun opTitle(op: String): Int =
      when (op) {
        SpeechEdits.REMOVE -> R.string.script_builder_edit_remove
        SpeechEdits.REPLACE -> R.string.script_builder_edit_replace
        SpeechEdits.REMOVE_REPEATS -> R.string.script_builder_edit_remove_repeats
        SpeechEdits.REMOVE_PARTS -> R.string.script_builder_edit_remove_parts
        SpeechEdits.KEEP_PARTS -> R.string.script_builder_edit_keep_parts
        SpeechEdits.SAY -> R.string.script_builder_change_say
        SpeechEdits.BEFORE -> R.string.script_builder_change_before
        SpeechEdits.AFTER -> R.string.script_builder_change_after
        else -> R.string.script_builder_change_silence
      }

    /** What [edit] does, in words. */
    fun editText(context: Context, edit: JSONObject): String {
      val find = edit.optString("find").let { if (edit.optBoolean("regex")) context.getString(R.string.macro_regex_value, it) else it }
      return when (edit.optString("op")) {
        SpeechEdits.REMOVE -> context.getString(R.string.script_builder_edit_remove_value, find)
        SpeechEdits.REPLACE -> context.getString(R.string.script_builder_change_replace_value, find, edit.optString("with"))
        SpeechEdits.REMOVE_PARTS -> context.getString(R.string.script_builder_edit_remove_parts_value, find)
        SpeechEdits.KEEP_PARTS ->
          context.resources.getQuantityString(R.plurals.script_builder_edit_keep_parts_value, edit.optInt("count"), edit.optInt("count"))
        SpeechEdits.SAY -> context.getString(R.string.script_builder_change_say_value, edit.optString("text"))
        SpeechEdits.BEFORE -> context.getString(R.string.script_builder_change_before_value, edit.optString("text"))
        SpeechEdits.AFTER -> context.getString(R.string.script_builder_change_after_value, edit.optString("text"))
        else -> context.getString(opTitle(edit.optString("op")))
      }
    }
  }
}
