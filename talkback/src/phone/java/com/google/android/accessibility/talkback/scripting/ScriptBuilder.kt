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

  /** Writes [draft] and installs the script made from it. */
  @Throws(IOException::class)
  fun save(context: Context, draft: JSONObject) {
    val manifestJson = manifestOf(context, draft)
    val main = "local manifest = ${lua(manifestJson)}\n" + code(draft)
    val files =
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
      rule.keys().asSequence().none { it in CHANGES } && rule.optJSONArray("actions").objects().isEmpty() ->
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
    val commands = draft.optJSONArray(BuiltScriptMacros.COMMANDS).objects()
    if (commands.isNotEmpty()) {
      manifest.put(
        "commands",
        JSONArray(commands.map { JSONObject().put("id", it.getString("id")).put("title", it.optString("title").ifBlank { it.getString("id") }) }),
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
    if (actions.any { action -> ACTION_KINDS.any { it != "speak" && action.has(it) } }) {
      needed += ScriptPermission.ACTIONS
    }
    if (actions.any { it.optJSONObject("speak")?.has("textOf") == true }) {
      needed += ScriptPermission.SCREEN
    }
    return needed
  }

  /** The permissions the commands and triggers of [draft] need to run their steps. */
  fun macroPermissionsOf(draft: JSONObject): Set<ScriptPermission> {
    val needed = mutableSetOf<ScriptPermission>()
    if (draft.optJSONArray(BuiltScriptMacros.COMMANDS).objects().isNotEmpty()) needed += ScriptPermission.INPUT
    for (entry in runnable(draft)) {
      BuiltScriptMacros.macroOf(entry)?.let { needed += ScriptRuntime.macroPermissions(it) }
    }
    return needed
  }

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

  /** What is wrong with the trigger [trigger], or null when it is finished. */
  fun triggerProblem(trigger: JSONObject): String? =
    when (trigger.optString("when")) {
      WHEN_FOCUS -> {
        val match = trigger.optJSONObject("match")
        when {
          match == null || match.length() == 0 -> "A match needs at least one field"
          else ->
            try {
              NodeQuery.problem(match) ?: NodeQuery.of(match).let { null }
            } catch (e: ScriptApiException) {
              e.message
            }
        }
      }
      WHEN_ANNOUNCEMENT -> if (trigger.optString("text").isBlank()) "The text is empty" else null
      WHEN_APP, WHEN_WINDOW -> null
      else -> "Unknown trigger"
    }

  /**
   * The code of [draft]: each command and trigger runs its macro file, then the Lua the user wrote
   * in the builder's Code, if any.
   */
  fun code(draft: JSONObject): String = generatedCode(draft) + draft.optString("code").let { if (it.isBlank()) "" else "\n$it\n" }

  private fun generatedCode(draft: JSONObject): String = buildString {
    val runnable = runnable(draft).toSet()
    for (command in draft.optJSONArray(BuiltScriptMacros.COMMANDS).objects()) {
      if (command !in runnable) continue
      append("sf.onCommand(${lua(command.getString("id"))}, function() sf.macros.run(${lua(macroFile(command))}) end)\n")
    }
    for (trigger in draft.optJSONArray(BuiltScriptMacros.TRIGGERS).objects()) {
      if (trigger !in runnable) continue
      val run = "sf.macros.run(${lua(macroFile(trigger))})"
      when (trigger.optString("when")) {
        WHEN_FOCUS -> {
          val query = lua(trigger.getJSONObject("match"), "  ")
          append("do\n  local query = $query\n")
          append("  sf.onFocus(function(item) if item:find(query) then $run end end)\nend\n")
        }
        WHEN_APP -> append("sf.onAppEnter(function() $run end)\n")
        WHEN_WINDOW -> append("sf.onWindowChange(function() $run end)\n")
        WHEN_ANNOUNCEMENT -> {
          val text = lua(trigger.getString("text").lowercase())
          append("sf.onAnnouncement(function(a) if a.text and string.find(string.lower(a.text), $text, 1, true) then $run end end)\n")
        }
      }
    }
  }

  /** What makes [trigger] run, in words. */
  fun triggerText(context: Context, trigger: JSONObject): String =
    when (trigger.optString("when")) {
      WHEN_FOCUS ->
        context.getString(
          R.string.script_builder_when_focus_value,
          ScriptBuilderFragment.describeQuery(context, trigger.optJSONObject("match")),
        )
      WHEN_APP -> context.getString(R.string.script_builder_when_app)
      WHEN_WINDOW -> context.getString(R.string.script_builder_when_window)
      WHEN_ANNOUNCEMENT -> context.getString(R.string.script_builder_when_announcement_value, trigger.optString("text"))
      else -> trigger.optString("when")
    }

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

  const val WHEN_FOCUS = "focus"
  const val WHEN_APP = "app"
  const val WHEN_WINDOW = "window"
  const val WHEN_ANNOUNCEMENT = "announcement"

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
