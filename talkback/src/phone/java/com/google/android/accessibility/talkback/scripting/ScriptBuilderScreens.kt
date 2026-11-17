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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.macros.MacroFragment
import com.google.android.accessibility.talkback.macros.Macros
import com.google.android.accessibility.talkback.notifications.NotificationCodeFragment
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
  protected fun edit(change: (JSONObject) -> Unit) {
    val draft = draft() ?: return
    change(draft)
    try {
      ScriptBuilder.save(requireContext(), draft)
    } catch (e: IOException) {
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

  override fun build(screen: PreferenceGroup) {
    val draft = draft() ?: return
    val context = requireContext()
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
      val problem = ScriptBuilder.navigationProblem(item)
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
    for (command in commands) {
      commandsCategory.link(command.optString("title"), stepsText(command), MacroFragment::class.java) {
        putString(MacroFragment.ARG_MACRO, BuiltScriptMacros.idOf(scriptId, BuiltScriptMacros.COMMANDS, command.getString("id")))
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
      val problem = ScriptBuilder.triggerProblem(trigger)
      val summary = if (problem != null) getString(R.string.script_builder_not_used, problem) else stepsText(trigger)
      triggersCategory.action(ScriptBuilder.triggerText(context, trigger), summary) { editTrigger(index) }
    }
    triggersCategory.action(getString(R.string.script_builder_add_trigger)) { addTrigger() }

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

  private fun addTrigger() {
    val kinds = listOf(ScriptBuilder.WHEN_FOCUS, ScriptBuilder.WHEN_APP, ScriptBuilder.WHEN_WINDOW, ScriptBuilder.WHEN_ANNOUNCEMENT)
    val names =
      listOf(
        getString(R.string.script_builder_when_focus),
        getString(R.string.script_builder_when_app),
        getString(R.string.script_builder_when_window),
        getString(R.string.script_builder_when_announcement),
      )
    choose(getString(R.string.script_builder_add_trigger), names) { which ->
      val entryId = newEntryId("t")
      val trigger = JSONObject().put("id", entryId).put("when", kinds[which])
      when (kinds[which]) {
        ScriptBuilder.WHEN_FOCUS -> {
          edit { it.getOrPutArray(BuiltScriptMacros.TRIGGERS).put(trigger.put("match", JSONObject())) }
          val index = draft()?.optJSONArray(BuiltScriptMacros.TRIGGERS)?.length()?.minus(1) ?: return@choose
          openQuery(listOf(BuiltScriptMacros.TRIGGERS, index, "match"))
        }
        ScriptBuilder.WHEN_ANNOUNCEMENT ->
          askText(getString(R.string.script_builder_when_announcement), hint = getString(R.string.script_builder_text)) { text, _, _ ->
            if (text.isEmpty()) return@askText getString(R.string.script_builder_name_empty)
            edit { it.getOrPutArray(BuiltScriptMacros.TRIGGERS).put(trigger.put("text", text)) }
            openSteps(BuiltScriptMacros.TRIGGERS, entryId)
            null
          }
        else -> {
          edit { it.getOrPutArray(BuiltScriptMacros.TRIGGERS).put(trigger) }
          openSteps(BuiltScriptMacros.TRIGGERS, entryId)
        }
      }
    }
  }

  private fun editTrigger(index: Int) {
    val trigger = draft()?.optJSONArray(BuiltScriptMacros.TRIGGERS)?.optJSONObject(index) ?: return
    val entryId = trigger.optString("id")
    val options = mutableListOf(getString(R.string.script_builder_steps))
    when (trigger.optString("when")) {
      ScriptBuilder.WHEN_FOCUS -> options += getString(R.string.script_builder_item)
      ScriptBuilder.WHEN_ANNOUNCEMENT -> options += getString(R.string.script_builder_text)
    }
    options += getString(R.string.script_builder_remove)
    choose(ScriptBuilder.triggerText(requireContext(), trigger), options) { which ->
      when (options[which]) {
        getString(R.string.script_builder_steps) -> openSteps(BuiltScriptMacros.TRIGGERS, entryId)
        getString(R.string.script_builder_item) -> openQuery(listOf(BuiltScriptMacros.TRIGGERS, index, "match"))
        getString(R.string.script_builder_text) ->
          askText(getString(R.string.script_builder_text), trigger.optString("text")) { text, _, _ ->
            if (text.isEmpty()) return@askText getString(R.string.script_builder_name_empty)
            edit { it.getJSONArray(BuiltScriptMacros.TRIGGERS).getJSONObject(index).put("text", text) }
            null
          }
        else -> edit { it.getJSONArray(BuiltScriptMacros.TRIGGERS).removeAt(index) }
      }
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
      ScriptBuilder.ruleProblem(rule)?.let { return context.getString(R.string.script_builder_not_used, it) }
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
        query.optJSONObject("inside")?.let {
          add(context.getString(R.string.script_builder_inside_value, describeQuery(context, it)))
        }
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
    ScriptBuilder.ruleProblem(rule)?.let {
      screen.action(getString(R.string.script_builder_not_used, it)) {}.isSelectable = false
    }

    val item = screen.category(R.string.script_builder_applies_to)
    item.action(getString(R.string.script_builder_item), ScriptBuilderFragment.describeQuery(context, rule.optJSONObject("match"))) {
      openQuery(path("match"))
    }
    textRow(item, "window", R.string.script_builder_only_window)
    textRow(item, "activity", R.string.script_builder_only_activity)

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

    val actions = screen.category(R.string.script_builder_actions)
    rule.optJSONArray("actions").objects().forEachIndexed { actionIndex, action ->
      actions.action(action.optString("title"), actionText(context, action)) { editAction(actionIndex) }
    }
    actions.action(getString(R.string.script_builder_add_action)) { addAction() }

    val manage = screen.category(R.string.script_builder_rule)
    manage.action(getString(R.string.script_move_up)) { edit { move(it.getJSONArray("rules"), index, -1) }.also { parentFragmentManager.popBackStack() } }
    manage.action(getString(R.string.script_move_down)) { edit { move(it.getJSONArray("rules"), index, 1) }.also { parentFragmentManager.popBackStack() } }
    manage.action(getString(R.string.script_builder_delete_rule)) {
      confirm(getString(R.string.script_builder_delete_rule), R.string.script_builder_delete) {
        edit { it.getJSONArray("rules").removeAt(index) }
        parentFragmentManager.popBackStack()
      }
    }
  }

  private fun textRow(group: PreferenceGroup, key: String, title: Int) {
    val old = rule()?.optString(key)?.ifEmpty { null }
    group.action(getString(title), old ?: notSet()) {
      askOptional(getString(title), old) { change(key, it) }
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
        5 ->
          askText(getString(R.string.script_builder_action_speak_text)) { text, _, _ ->
            if (text.isEmpty()) return@askText getString(R.string.script_builder_name_empty)
            action.put("speak", text)
            saveAction()
            null
          }
        6 -> {
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

  private fun editAction(actionIndex: Int) {
    val action = rule()?.optJSONArray("actions")?.optJSONObject(actionIndex) ?: return
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

/** The item a rule, action or reading control names: the tests an item must pass. */
class ScriptQueryFragment : ScriptBuilderBaseFragment() {
  private val path: List<Any> by lazy { Paths.decode(requireArguments().getString(ARG_PATH).orEmpty()) }

  /** inside and has hold a query that can't hold inside or has. */
  private val nested: Boolean
    get() = path.lastOrNull() == "inside" || path.lastOrNull() == "has"

  private fun query(): JSONObject? = Paths.get(draft(), path) as? JSONObject

  private fun change(key: String, value: Any?) = edit { draft ->
    (Paths.get(draft, path) as? JSONObject)?.putOrRemove(key, value)
  }

  override fun getTitle(): CharSequence = getText(R.string.script_builder_item)

  override fun build(screen: PreferenceGroup) {
    val query = query() ?: return
    val context = requireContext()
    screen.action(ScriptBuilderFragment.describeQuery(context, query)) {}.isSelectable = false
    ItemInspector.lastQuery(context)?.let { inspected ->
      screen.action(
        getString(R.string.script_builder_use_inspected),
        ScriptBuilderFragment.describeQuery(context, inspected),
      ) {
        edit { draft ->
          val target = Paths.get(draft, path) as? JSONObject ?: return@edit
          for (key in QUERY_KEYS) target.remove(key)
          inspected.keys().forEach { target.put(it, inspected.get(it)) }
        }
      }
    }
    screen.action(getString(R.string.script_builder_text), ScriptBuilderFragment.textCondition(context, query) ?: notSet()) {
      askTextCondition(query)
    }
    screen.action(getString(R.string.macro_item_kind), ScriptBuilderFragment.kindsOf(query).joinToString(", ") { getString(it.title) }.ifEmpty { notSet() }) {
      askKinds(query)
    }
    textRow(screen, query, "id", R.string.script_builder_id)
    textRow(screen, query, "className", R.string.script_builder_class)
    screen.action(getString(R.string.script_builder_role), query.optString("role").ifEmpty { notSet() }) {
      val roles = ScriptRoles.names.sorted()
      choose(getString(R.string.script_builder_role), listOf(notSet()) + roles, roles.indexOf(query.optString("role")) + 1) {
        change("role", roles.getOrNull(it - 1))
      }
    }
    if (!nested) {
      subQueryRow(screen, query, "inside", R.string.script_builder_inside)
      subQueryRow(screen, query, "has", R.string.script_builder_has)
    }
  }

  private fun textRow(group: PreferenceGroup, query: JSONObject, key: String, title: Int) {
    val old = query.optString(key).ifEmpty { null }
    group.action(getString(title), old ?: notSet()) {
      askOptional(getString(title), old) { change(key, it) }
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
    private val QUERY_KEYS = listOf("id", "contains", "matches", "field", "kind", "className", "role")
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
