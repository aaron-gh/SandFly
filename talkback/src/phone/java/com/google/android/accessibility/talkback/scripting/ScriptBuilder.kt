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
import com.google.android.accessibility.talkback.R
import java.io.IOException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Scripts made in the script builder. A built script keeps what the builder shows in
 * [DRAFT_FILE], in the form of a manifest: its name, apps, rules and reading controls, including
 * ones that aren't finished. Each change writes main.lua again, with a manifest of the finished
 * ones and no code, and installs it, so that it applies at once.
 */
object ScriptBuilder {
  const val DRAFT_FILE = "builder.json"
  private const val ID_PREFIX = "built-"

  fun isBuilt(store: ScriptStore, id: String): Boolean = store.file(id, DRAFT_FILE)?.isFile == true

  /** The draft of the built script [id], or null when it isn't one. */
  fun draft(store: ScriptStore, id: String): JSONObject? =
    store.readFile(id, DRAFT_FILE)?.let {
      try {
        JSONObject(it.decodeToString())
      } catch (e: JSONException) {
        null
      }
    }

  /** Makes and installs an empty script named [name], and returns its id. */
  @Throws(IOException::class)
  fun create(context: Context, name: String): String {
    val id = ID_PREFIX + java.lang.Long.toString(System.currentTimeMillis(), Character.MAX_RADIX)
    val draft =
      JSONObject()
        .put("id", id)
        .put("name", name)
        .put("apps", JSONArray())
        .put("rules", JSONArray())
        .put("navigation", JSONArray())
    save(context, draft)
    return id
  }

  /**
   * Installs [draft] as a new built script, with a new id, and with the files of the script
   * [filesFrom] that the builder doesn't write. Returns the new id.
   */
  @Throws(IOException::class)
  fun createFrom(context: Context, draft: JSONObject, filesFrom: String?): String {
    val id = ID_PREFIX + java.lang.Long.toString(System.currentTimeMillis(), Character.MAX_RADIX)
    draft.put("id", id)
    save(context, draft, filesFrom?.let { otherFiles(ScriptStore.get(context), it) }.orEmpty())
    return id
  }

  /**
   * Makes a built script from the installed script [script]: its manifest becomes the builder's
   * parts, and its code the builder's Lua code. Its other files are kept. Returns the new id.
   */
  @Throws(IOException::class)
  fun copyToBuilder(context: Context, script: InstalledScript): String {
    val store = ScriptStore.get(context)
    val source = store.readFile(script.id, ScriptPackage.MAIN)?.decodeToString() ?: throw IOException("No main.lua")
    val manifest = JSONObject(ManifestReader.read(source))
    val code =
      ManifestReader.codeAfter(source).lines().filterNot { it.startsWith(ScriptSignature.PREFIX) }.joinToString("\n").trim()
    val draft =
      JSONObject()
        .put("name", context.getString(R.string.script_builder_copy_name, manifest.optString("name", script.manifest.name)))
        .put("apps", manifest.optJSONArray("apps") ?: manifest.optString("apps").takeIf { it.isNotEmpty() }?.let { JSONArray().put(it) } ?: JSONArray())
        .put("rules", manifest.optJSONArray("rules") ?: JSONArray())
        .put("navigation", manifest.optJSONArray("navigation") ?: JSONArray())
    manifest.optJSONArray("settings")?.let { draft.put("settings", it) }
    manifest.optJSONArray("permissions")?.let { draft.put("permissions", it) }
    (manifest.opt("permissions") as? String)?.let { draft.put("permissions", JSONArray().put(it)) }
    for (key in MANIFEST_FIELDS) manifest.opt(key)?.let { draft.put(key, it) }
    manifest.optJSONArray("commands")?.let { draft.put(BuiltScriptMacros.COMMANDS, it) }
    if (code.isNotEmpty()) draft.put("code", code)
    return createFrom(context, draft, script.id)
  }

  /** Writes [draft] and installs the script made from it. */
  @Throws(IOException::class)
  fun save(context: Context, draft: JSONObject, extraFiles: Map<String, ByteArray>? = null) {
    val manifestJson = manifestOf(context, draft)
    val main = "local manifest = ${lua(manifestJson)}\n" + code(draft)
    val files =
      (extraFiles ?: otherFiles(ScriptStore.get(context), draft.getString("id"))) +
        mapOf(
          ScriptPackage.MAIN to main.encodeToByteArray(),
          DRAFT_FILE to draft.toString(1).encodeToByteArray(),
        ) + macroFiles(draft)
    val manifest = ScriptManifest.parse(ManifestReader.read(main))
    val store = ScriptStore.get(context)
    // Permissions the script already asked for keep the user's choice on its page; ones its new
    // parts need are allowed, as the user added those parts.
    val existing = store.find(manifest.id)
    val granted =
      if (existing == null) manifest.permissions
      else manifest.permissions.filter { it in existing.granted || it !in existing.manifest.permissions }.toSet()
    store.install(ScriptPackage(files, manifest), granted)
  }

  /**
   * The files of the installed script [id] that the builder doesn't write, such as sounds and Lua
   * modules of a script copied into the builder, so that they stay.
   */
  private fun otherFiles(store: ScriptStore, id: String): Map<String, ByteArray> {
    val root = store.file(id, ScriptPackage.MAIN)?.parentFile ?: return emptyMap()
    if (!root.isDirectory) return emptyMap()
    return root.walkTopDown()
      .filter { it.isFile }
      .map { it.relativeTo(root).invariantSeparatorsPath to it }
      .filter { (path, _) -> path != ScriptPackage.MAIN && path != DRAFT_FILE && !path.startsWith("macros/") }
      .associate { (path, file) -> path to file.readBytes() }
  }

  /** What is wrong with the rule [rule], or null when it is finished. */
  fun ruleProblem(rule: JSONObject): String? =
    try {
      ManifestParser(strict = true).rule(rule)
      null
    } catch (e: ManifestException) {
      e.message
    } catch (e: ScriptApiException) {
      e.message
    } catch (e: JSONException) {
      e.message
    }

  /**
   * What the user still has to do for [rule], in words, or null when it is finished. Problems the
   * builder knows are named plainly; others are the manifest's own message.
   */
  fun ruleProblemText(context: Context, rule: JSONObject): String? {
    val problem = ruleProblem(rule) ?: return null
    val action = rule.optJSONArray("actions").objects().firstOrNull { action ->
      if (action.has("steps")) return@firstOrNull action.optJSONArray("steps").objects().isEmpty()
      val key = ACTION_KINDS.firstOrNull(action::has)
      val target = when {
        key == null -> null
        key == "speak" -> (action.opt("speak") as? JSONObject)?.optJSONObject("textOf")
        else -> action.optJSONObject(key)
      }
      key == null || (target != null && target.length() == 0)
    }
    return when {
      isEmptyQuery(rule.optJSONObject("match")) -> context.getString(R.string.script_builder_problem_item)
      listOf("readBefore", "readAfter", "activate").any { rule.has(it) && isEmptyQuery(rule.optJSONObject(it)) } ->
        context.getString(R.string.script_builder_problem_other_item)
      action != null -> context.getString(R.string.script_builder_problem_action, action.optString("title"))
      rule.keys().asSequence().none { it in CHANGES } && rule.optJSONArray("actions").objects().isEmpty() &&
        !rule.optBoolean("buttonActions") ->
        context.getString(R.string.script_builder_problem_change)
      else -> problem
    }
  }

  /** What the user still has to do for the reading control or trigger [entry], or null. */
  fun entryProblemText(context: Context, entry: JSONObject, problem: String?): String? =
    when {
      problem == null -> null
      entry.has("match") && isEmptyQuery(entry.optJSONObject("match")) -> context.getString(R.string.script_builder_problem_item)
      else -> problem
    }

  private fun isEmptyQuery(query: JSONObject?): Boolean = query == null || query.length() == 0

  /** What is wrong with the reading control [item], or null when it is finished. */
  fun navigationProblem(item: JSONObject): String? {
    val manifest =
      baseManifest("check", "check")
        .put("permissions", JSONArray().put(ScriptPermission.INPUT.key))
        .put("navigation", JSONArray().put(item))
    return try {
      ScriptManifest.parse(manifest.toString())
      null
    } catch (e: ManifestException) {
      e.message
    } catch (e: ScriptApiException) {
      e.message
    } catch (e: JSONException) {
      e.message
    }
  }

  /** The manifest of [draft]: its finished rules and reading controls, and what they need. */
  fun manifestOf(context: Context, draft: JSONObject): JSONObject {
    val rules = draft.optJSONArray("rules").objects().filter { ruleProblem(it) == null }
    val navigation =
      draft.optJSONArray("navigation").objects().filter { navigationProblem(it) == null }
    val manifest =
      baseManifest(draft.getString("id"), draft.optString("name").ifBlank { draft.getString("id") })
        .put("author", context.getString(R.string.script_builder_author))
    val apps = draft.optJSONArray("apps")
    if (apps != null && apps.length() > 0) manifest.put("apps", apps)
    val permissions =
      permissionsOf(rules, navigation) + macroPermissionsOf(draft) +
        draft.optJSONArray("permissions").let { list ->
          (0 until (list?.length() ?: 0)).mapNotNull { ScriptPermission.fromKey(list!!.optString(it)) }
        }
    if (permissions.isNotEmpty()) manifest.put("permissions", JSONArray(permissions.map { it.key }))
    for (key in MANIFEST_FIELDS) {
      when (val value = draft.opt(key)) {
        null, JSONObject.NULL -> {}
        is String -> if (value.isNotBlank()) manifest.put(key, value)
        is Boolean -> if (value) manifest.put(key, true)
      }
    }
    val settings = draft.optJSONArray("settings").objects()
    if (settings.isNotEmpty()) manifest.put("settings", JSONArray(settings))
    val commands = draft.optJSONArray(BuiltScriptMacros.COMMANDS).objects()
    if (commands.isNotEmpty()) {
      manifest.put(
        "commands",
        JSONArray(
          commands.map { command ->
            JSONObject().put("id", command.getString("id")).put("title", command.optString("title").ifBlank { command.getString("id") }).apply {
              for (key in COMMAND_FIELDS) {
                when (val value = command.opt(key)) {
                  is String -> if (value.isNotBlank()) put(key, value)
                  is Boolean -> if (value) put(key, true)
                  is JSONArray -> if (value.length() > 0) put(key, value)
                }
              }
            }
          }
        ),
      )
    }
    if (rules.isNotEmpty()) manifest.put("rules", JSONArray(rules))
    if (navigation.isNotEmpty()) manifest.put("navigation", JSONArray(navigation))
    return manifest
  }

  /** The permissions [rules] and [navigation] need, as the scripting guide lists them. */
  fun permissionsOf(rules: List<JSONObject>, navigation: List<JSONObject>): Set<ScriptPermission> {
    val needed = mutableSetOf<ScriptPermission>()
    if (rules.any { rule -> rule.keys().asSequence().any { it in CHANGES } }) {
      needed += ScriptPermission.SPEECH
    }
    val actions = rules.flatMap { it.optJSONArray("actions").objects() }
    if (actions.isNotEmpty() || navigation.isNotEmpty()) needed += ScriptPermission.INPUT
    if (rules.any { it.optBoolean("buttonActions") }) needed += setOf(ScriptPermission.INPUT, ScriptPermission.ACTIONS)
    if (actions.any { action -> action.has("steps") || ACTION_KINDS.any { it != "speak" && action.has(it) } }) {
      needed += ScriptPermission.ACTIONS
    }
    // Steps find menu entries on screen.
    if (actions.any { action -> action.optJSONArray("steps").objects().any { it.optString("where") == "screen" } }) {
      needed += ScriptPermission.SCREEN
    }
    if (actions.any { it.optJSONObject("speak")?.has("textOf") == true }) {
      needed += ScriptPermission.SCREEN
    }
    return needed
  }

  /** The permissions the commands, triggers and speech changes of [draft] need. */
  fun macroPermissionsOf(draft: JSONObject): Set<ScriptPermission> {
    val needed = mutableSetOf<ScriptPermission>()
    // Commands the user assigns need nothing; their own gesture, keys, menu or control need input.
    val binds = draft.optJSONArray(BuiltScriptMacros.COMMANDS).objects().any { command ->
      COMMAND_FIELDS.any { key ->
        when (val value = command.opt(key)) {
          is String -> value.isNotBlank()
          is Boolean -> value
          else -> false
        }
      }
    }
    if (binds) needed += ScriptPermission.INPUT
    for (entry in runnable(draft)) {
      BuiltScriptMacros.macroOf(entry)?.let { needed += ScriptRuntime.macroPermissions(it) }
      when (entry.optString("when")) {
        "" -> {}
        WHEN_NOTIFICATION -> needed += ScriptPermission.NOTIFICATIONS
        // Timers and buttons read nothing on screen; the other triggers' hooks do.
        WHEN_EVERY, WHEN_DAILY, WHEN_BUTTON -> {}
        else -> needed += ScriptPermission.SCREEN
      }
    }
    for (change in draft.optJSONArray(SPEECH).objects().filter { speechProblem(it) == null }) {
      needed += ScriptPermission.SPEECH
      when (change.optString("when")) {
        WHEN_FOCUS, WHEN_ANNOUNCEMENT -> needed += ScriptPermission.SCREEN
        WHEN_NOTIFICATION -> needed += ScriptPermission.NOTIFICATIONS
      }
    }
    return needed
  }

  /** Fields of the manifest the builder's About part sets. */
  val MANIFEST_FIELDS = listOf("description", "version", "homepage", "onDemand", "keepLoaded")

  /** Fields of a command besides its id and title. */
  val COMMAND_FIELDS = listOf("gesture", "keys", "menu", "control")

  /** The commands, and the finished triggers, of [draft] that have steps. */
  private fun runnable(draft: JSONObject): List<JSONObject> =
    draft.optJSONArray(BuiltScriptMacros.COMMANDS).objects().filter { hasSteps(it) } +
      draft.optJSONArray(BuiltScriptMacros.TRIGGERS).objects().filter { hasSteps(it) && triggerProblem(it) == null }

  private fun hasSteps(entry: JSONObject): Boolean = BuiltScriptMacros.macroOf(entry)?.steps?.isNotEmpty() == true

  private fun macroFile(entry: JSONObject): String = "macros/${entry.getString("id")}.json"

  private fun macroFiles(draft: JSONObject): Map<String, ByteArray> =
    runnable(draft).associate { entry ->
      val macro = BuiltScriptMacros.macroOf(entry)!!.copy(id = entry.getString("id"))
      macroFile(entry) to com.google.android.accessibility.talkback.macros.Macros.macroToJson(macro).encodeToByteArray()
    }

  /** What is wrong with the query [match], which must name an item when [required]. */
  private fun queryProblem(match: JSONObject?, required: Boolean): String? =
    when {
      match == null || match.length() == 0 -> if (required) "A match needs at least one field" else null
      else ->
        try {
          NodeQuery.problem(match) ?: NodeQuery.of(match).let { null }
        } catch (e: ScriptApiException) {
          e.message
        }
    }

  /** What is wrong with the trigger [trigger], or null when it is finished. */
  fun triggerProblem(trigger: JSONObject): String? =
    when (val kind = trigger.optString("when")) {
      WHEN_FOCUS -> queryProblem(trigger.optJSONObject("match"), required = true)
      in NODE_TRIGGERS -> queryProblem(trigger.optJSONObject("match"), required = false)
      WHEN_ANNOUNCEMENT, WHEN_NOTIFICATION, WHEN_APP, WHEN_APP_LEAVE, WHEN_WINDOW -> null
      WHEN_EVERY -> if (trigger.optInt("minutes") in 1..1440) null else "Every needs 1 to 1440 minutes"
      WHEN_DAILY -> if (TIME.matches(trigger.optString("time"))) null else "The time is HH:MM"
      WHEN_BUTTON -> if (trigger.optString("setting").isEmpty()) "Choose a button setting" else null
      else -> "Unknown trigger $kind"
    }

  /** What is wrong with the speech change [change], or null when it is finished. */
  fun speechProblem(change: JSONObject): String? {
    val edits = editsOf(change)
    return when {
      change.optString("when") !in SPEECH_WHEN -> "Unknown speech change"
      change.optString("when") == WHEN_FOCUS && queryProblem(change.optJSONObject("match"), required = true) != null ->
        queryProblem(change.optJSONObject("match"), required = true)
      edits.length() == 0 -> "Add an edit"
      else -> edits.objects().firstNotNullOfOrNull { SpeechEdits.problem(it) }
    }
  }

  /**
   * The edits of [change]. A speech change made before edits had one change, kept as
   * change, text and find; it reads as the same edit.
   */
  fun editsOf(change: JSONObject): JSONArray {
    change.optJSONArray("edits")?.let { return it }
    val text = change.optString("text")
    val edit =
      when (change.optString("change")) {
        CHANGE_SAY -> JSONObject().put("op", SpeechEdits.SAY).put("text", text)
        CHANGE_BEFORE -> JSONObject().put("op", SpeechEdits.BEFORE).put("text", text)
        CHANGE_AFTER -> JSONObject().put("op", SpeechEdits.AFTER).put("text", text)
        CHANGE_SILENCE -> JSONObject().put("op", SpeechEdits.SILENCE)
        CHANGE_REPLACE_PART -> JSONObject().put("op", SpeechEdits.REPLACE).put("find", change.optString("find")).put("with", text)
        else -> return JSONArray()
      }
    return JSONArray().put(edit)
  }

  /**
   * The code of [draft]: each command and trigger runs its macro file, each speech change changes
   * speech, then the Lua the user wrote in the builder's Code, if any.
   */
  fun code(draft: JSONObject): String = generatedCode(draft) + draft.optString("code").let { if (it.isBlank()) "" else "\n$it\n" }

  /** Functions the generated code shares. Their names start with __sf_b so that Lua code can't clash. */
  private const val HELPERS =
    """
local function __sf_bvalues(extra)
  local v = {}
  local ok, all = pcall(sf.settings.all)
  if ok and type(all) == 'table' then
    for k, s in pairs(all) do v['setting.' .. k] = tostring(s) end
  end
  for k, s in pairs(extra or {}) do v[k] = tostring(s) end
  return v
end
local function __sf_brun(file, extra) sf.macros.run(file, __sf_bvalues(extra)) end
local function __sf_bhas(text, part)
  return part == '' or (text ~= nil and string.find(string.lower(tostring(text)), part, 1, true) ~= nil)
end
local function __sf_bnode(node, query)
  return query == nil or (node ~= nil and node:find(query) ~= nil)
end
local function __sf_breplace(text, find, with)
  local out, i = {}, 1
  while true do
    local s, e = string.find(text, find, i, true)
    if not s then break end
    out[#out + 1] = string.sub(text, i, s - 1) .. with
    i = e + 1
  end
  out[#out + 1] = string.sub(text, i)
  return table.concat(out)
end
local function __sf_btext(node) return node ~= nil and tostring(node) or nil end
local function __sf_bedits(said, edits)
  local result = sf.text.edit(said, edits)
  if result == nil then return nil end
  if result.silent then return false end
  return result.text
end
"""

  private fun generatedCode(draft: JSONObject): String = buildString {
    val runnable = runnable(draft).toSet()
    val speech = draft.optJSONArray(SPEECH).objects().filter { speechProblem(it) == null }
    if (runnable.isEmpty() && speech.isEmpty()) return@buildString
    append(HELPERS)
    for (command in draft.optJSONArray(BuiltScriptMacros.COMMANDS).objects()) {
      if (command !in runnable) continue
      append("sf.onCommand(${lua(command.getString("id"))}, function(e) ")
      append("__sf_brun(${lua(macroFile(command))}, { direction = e.direction, text = __sf_btext(e.node) }) end)\n")
    }
    for (trigger in draft.optJSONArray(BuiltScriptMacros.TRIGGERS).objects()) {
      if (trigger !in runnable) continue
      val file = lua(macroFile(trigger))
      val match = trigger.optJSONObject("match")?.takeIf { it.length() > 0 }
      val query = match?.let { lua(it, "  ") } ?: "nil"
      when (val kind = trigger.optString("when")) {
        WHEN_FOCUS -> {
          append("do\n  local query = $query\n")
          append("  sf.onFocus(function(item) if item:find(query) then __sf_brun($file, { text = tostring(item) }) end end)\nend\n")
        }
        WHEN_APP -> append("sf.onAppEnter(function(a) __sf_brun($file, { app = a.package }) end)\n")
        WHEN_APP_LEAVE -> append("sf.onAppLeave(function(a) __sf_brun($file, { app = a.package }) end)\n")
        WHEN_WINDOW -> append("sf.onWindowChange(function(a) __sf_brun($file, { app = a.package, window = a.window }) end)\n")
        WHEN_NOTIFICATION -> {
          val text = lua(trigger.optString("text").lowercase())
          append("sf.onNotification(function(n) local t = (n.title or '') .. ' ' .. (n.text or '') ")
          append("if __sf_bhas(t, $text) then __sf_brun($file, { text = t, title = n.title, body = n.text, app = n.package }) end end)\n")
        }
        WHEN_ANNOUNCEMENT -> {
          val text = lua(trigger.optString("text").lowercase())
          append("sf.onAnnouncement(function(a) if __sf_bhas(a.text, $text) then __sf_brun($file, { text = a.text, app = a.package }) end end)\n")
        }
        WHEN_TEXT_CHANGE -> {
          append("do\n  local query = $query\n")
          append("  sf.onTextChange(function(e) if __sf_bnode(e.node, query) then __sf_brun($file, { text = e.text, added = e.added, app = e.package }) end end)\nend\n")
        }
        in NODE_TRIGGERS -> {
          val hook = NODE_HOOKS.getValue(kind)
          append("do\n  local query = $query\n")
          append("  sf.$hook(function(e) if __sf_bnode(e.node, query) then __sf_brun($file, { text = __sf_btext(e.node), app = e.package }) end end)\nend\n")
        }
        WHEN_EVERY -> append("sf.setInterval(function() __sf_brun($file) end, ${trigger.optInt("minutes") * 60_000})\n")
        WHEN_DAILY -> {
          val time = lua(trigger.optString("time"))
          append("do\n  local last\n  sf.setInterval(function()\n")
          append("    local day = os.date('%Y-%m-%d')\n")
          append("    if os.date('%H:%M') == $time and last ~= day then last = day; __sf_brun($file) end\n")
          append("  end, 20000)\nend\n")
        }
        WHEN_BUTTON -> append("sf.onButton(function(key) if key == ${lua(trigger.optString("setting"))} then __sf_brun($file) end end)\n")
      }
    }
    for (change in speech) {
      val result = "__sf_bedits(said, ${lua(editsOf(change), "  ")})"
      val text = lua(change.optString("has").lowercase())
      val app = change.optString("app").ifEmpty { null }?.let(::lua)
      when (change.optString("when")) {
        WHEN_FOCUS -> {
          append("do\n  local query = ${lua(change.getJSONObject("match"), "  ")}\n")
          append("  sf.onFocus(function(item, said) if item:find(query) and __sf_bhas(said, $text) then return $result end end)\nend\n")
        }
        WHEN_NOTIFICATION ->
          append(
            "sf.onNotification(function(n, said) if ${app?.let { "n.package == $it and " }.orEmpty()}__sf_bhas(said, $text) then return $result end end)\n"
          )
        WHEN_ANNOUNCEMENT ->
          append(
            "sf.onAnnouncement(function(a, said) if ${app?.let { "a.package == $it and " }.orEmpty()}__sf_bhas(said, $text) then return $result end end)\n"
          )
        WHEN_SPEECH -> append("sf.onSpeech(function(said) if __sf_bhas(said, $text) then return $result end end)\n")
      }
    }
  }

  /** What makes [trigger] run, in words. */
  fun triggerText(context: Context, trigger: JSONObject): String {
    val match = trigger.optJSONObject("match")?.takeIf { it.length() > 0 }
    val item = match?.let { ScriptBuilderFragment.describeQuery(context, it) }
    return when (val kind = trigger.optString("when")) {
      WHEN_FOCUS -> context.getString(R.string.script_builder_when_focus_value, item.orEmpty())
      WHEN_APP -> context.getString(R.string.script_builder_when_app)
      WHEN_APP_LEAVE -> context.getString(R.string.script_builder_when_app_leave)
      WHEN_WINDOW -> context.getString(R.string.script_builder_when_window)
      WHEN_ANNOUNCEMENT -> context.getString(R.string.script_builder_when_announcement_value, trigger.optString("text"))
      WHEN_NOTIFICATION -> context.getString(R.string.script_builder_when_notification_value, trigger.optString("text"))
      WHEN_EVERY -> context.resources.getQuantityString(R.plurals.script_builder_when_every_value, trigger.optInt("minutes"), trigger.optInt("minutes"))
      WHEN_DAILY -> context.getString(R.string.script_builder_when_daily_value, trigger.optString("time"))
      WHEN_BUTTON -> context.getString(R.string.script_builder_when_button_value, settingTitle(trigger.optString("setting"), null))
      in NODE_TRIGGERS -> {
        val name = context.getString(NODE_TRIGGER_TITLES.getValue(kind))
        if (item == null) name else context.getString(R.string.script_builder_trigger_item_value, name, item)
      }
      else -> kind
    }
  }

  /** The title of the setting [key] of [draft], or the key when there is none. */
  fun settingTitle(key: String, draft: JSONObject?): String =
    draft?.optJSONArray("settings").objects().firstOrNull { it.optString("key") == key }?.optString("title")?.ifEmpty { null } ?: key

  /** What [change] does, in words. */
  fun speechText(context: Context, change: JSONObject): String {
    val whenText =
      when (change.optString("when")) {
        WHEN_FOCUS ->
          context.getString(R.string.script_builder_when_focus_value, ScriptBuilderFragment.describeQuery(context, change.optJSONObject("match")))
        WHEN_NOTIFICATION -> context.getString(R.string.script_builder_speech_notifications)
        WHEN_ANNOUNCEMENT -> context.getString(R.string.script_builder_speech_announcements)
        else -> context.getString(R.string.script_builder_speech_all)
      }
    val has = change.optString("has").ifEmpty { null }?.let { context.getString(R.string.script_builder_speech_has_value, it) }
    return listOfNotNull(whenText, has).joinToString(", ")
  }

  const val WHEN_FOCUS = "focus"
  const val WHEN_APP = "app"
  const val WHEN_APP_LEAVE = "appLeave"
  const val WHEN_WINDOW = "window"
  const val WHEN_ANNOUNCEMENT = "announcement"
  const val WHEN_NOTIFICATION = "notification"
  const val WHEN_TEXT_CHANGE = "textChange"
  const val WHEN_CONTENT_CHANGE = "contentChange"
  const val WHEN_SCROLL = "scroll"
  const val WHEN_SELECTION = "selection"
  const val WHEN_CLICK = "click"
  const val WHEN_LONG_CLICK = "longClick"
  const val WHEN_INPUT_FOCUS = "inputFocus"
  const val WHEN_EVERY = "every"
  const val WHEN_DAILY = "daily"
  const val WHEN_BUTTON = "button"
  const val WHEN_SPEECH = "speech"

  /** Triggers on an item, which may name the item, by the hooks that run them. */
  val NODE_HOOKS =
    mapOf(
      WHEN_TEXT_CHANGE to "onTextChange",
      WHEN_CONTENT_CHANGE to "onContentChange",
      WHEN_SCROLL to "onScroll",
      WHEN_SELECTION to "onSelectionChange",
      WHEN_CLICK to "onClick",
      WHEN_LONG_CLICK to "onLongClick",
      WHEN_INPUT_FOCUS to "onInputFocus",
    )
  val NODE_TRIGGERS: Set<String> = NODE_HOOKS.keys
  val NODE_TRIGGER_TITLES =
    mapOf(
      WHEN_TEXT_CHANGE to R.string.script_builder_when_text_change,
      WHEN_CONTENT_CHANGE to R.string.script_builder_when_content_change,
      WHEN_SCROLL to R.string.script_builder_when_scroll,
      WHEN_SELECTION to R.string.script_builder_when_selection,
      WHEN_CLICK to R.string.script_builder_when_click,
      WHEN_LONG_CLICK to R.string.script_builder_when_long_click,
      WHEN_INPUT_FOCUS to R.string.script_builder_when_input_focus,
    )

  const val SPEECH = "speech"
  const val CHANGE_SAY = "say"
  const val CHANGE_BEFORE = "before"
  const val CHANGE_AFTER = "after"
  const val CHANGE_SILENCE = "silence"
  const val CHANGE_REPLACE_PART = "replacePart"
  val SPEECH_WHEN = listOf(WHEN_FOCUS, WHEN_NOTIFICATION, WHEN_ANNOUNCEMENT, WHEN_SPEECH)
  val SPEECH_CHANGES = listOf(CHANGE_SAY, CHANGE_BEFORE, CHANGE_AFTER, CHANGE_SILENCE, CHANGE_REPLACE_PART)
  private val TIME = Regex("^([01][0-9]|2[0-3]):[0-5][0-9]$")

  /** Writes the files of the installed script [id] as a zip to [out]. */
  @Throws(IOException::class)
  fun exportZip(store: ScriptStore, id: String, out: java.io.OutputStream) {
    val root = store.file(id, ScriptPackage.MAIN)?.parentFile ?: throw IOException("No script $id")
    java.util.zip.ZipOutputStream(out).use { zip ->
      root.walkTopDown().filter { it.isFile }.forEach { file ->
        zip.putNextEntry(java.util.zip.ZipEntry(file.relativeTo(root).invariantSeparatorsPath))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
      }
    }
  }

  private fun baseManifest(id: String, name: String): JSONObject =
    JSONObject()
      .put("id", id)
      .put("name", name)
      .put("author", "-")
      .put("minApiVersion", ScriptManifest.API_VERSION)
      .put("testedApiVersion", ScriptManifest.API_VERSION)

  /** The JSON value [value] as a Lua value that [ManifestReader] reads back the same. */
  fun lua(value: Any?, indent: String = ""): String =
    when (value) {
      null,
      JSONObject.NULL -> "nil"
      is JSONObject -> {
        val keys = value.keys().asSequence().toList()
        if (keys.isEmpty()) {
          "{}"
        } else {
          val inner = "$indent  "
          keys.joinToString(",\n", "{\n", "\n$indent}") { key ->
            val name = if (IDENTIFIER.matches(key) && key !in LUA_KEYWORDS) key else "[${luaString(key)}]"
            "$inner$name = ${lua(value.get(key), inner)}"
          }
        }
      }
      is JSONArray -> {
        if (value.length() == 0) {
          "{}"
        } else {
          val inner = "$indent  "
          (0 until value.length()).joinToString(",\n", "{\n", "\n$indent}") {
            "$inner${lua(value.get(it), inner)}"
          }
        }
      }
      is String -> luaString(value)
      is Boolean -> value.toString()
      is Number ->
        if (value.toDouble() == value.toLong().toDouble()) value.toLong().toString() else value.toString()
      else -> luaString(value.toString())
    }

  private fun luaString(text: String): String =
    buildString {
      append('"')
      for (c in text) {
        when {
          c == '\\' -> append("\\\\")
          c == '"' -> append("\\\"")
          c == '\n' -> append("\\n")
          c == '\r' -> append("\\r")
          c == '\t' -> append("\\t")
          c < ' ' -> append("\\").append(c.code.toString().padStart(3, '0'))
          else -> append(c)
        }
      }
      append('"')
    }

  fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

  /** The keys of a rule that change its item, and so need the speech permission. */
  private val CHANGES =
    setOf(
      "label", "speak", "hide", "silence", "role", "state", "hint", "heading", "group",
      "readBefore", "readAfter", "activate", "liftToActivate", "longPressAfter", "touchToActivate",
    )

  /** The kinds of rule action, each the key that holds what it acts on. */
  val ACTION_KINDS = listOf("click", "longClick", "focus", "scrollForward", "scrollBackward", "speak")

  private val IDENTIFIER = Regex("^[A-Za-z_][A-Za-z0-9_]*$")
  private val LUA_KEYWORDS =
    setOf(
      "and", "break", "do", "else", "elseif", "end", "false", "for", "function", "goto", "if", "in",
      "local", "nil", "not", "or", "repeat", "return", "then", "true", "until", "while",
    )
}
