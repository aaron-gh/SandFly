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

import android.app.Application
import android.content.Context
import com.google.android.accessibility.talkback.macros.Macros
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject

/**
 * The steps of the commands and triggers of scripts made in the script builder, as macros, so
 * that the macro editor edits them. Their ids are script:, the script's id, the list they are in
 * (commands or triggers) and their own id, separated by colons.
 */
object BuiltScriptMacros : Macros.ExternalMacros {
  private data class Place(val scriptId: String, val list: String, val entryId: String)

  fun idOf(scriptId: String, list: String, entryId: String): String =
    "${Macros.EXTERNAL_PREFIX}$scriptId:$list:$entryId"

  private fun placeOf(id: String): Place? {
    val parts = id.removePrefix(Macros.EXTERNAL_PREFIX).split(':')
    if (parts.size != 3 || parts[1] !in LISTS) return null
    return Place(parts[0], parts[1], parts[2])
  }

  private val context: Context?
    get() = appContext ?: currentApplication()

  @Volatile private var appContext: Context? = null

  fun init(context: Context) {
    appContext = context.applicationContext
  }

  @Suppress("PrivateApi")
  private fun currentApplication(): Context? =
    try {
      Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null) as? Application
    } catch (e: ReflectiveOperationException) {
      null
    }

  private fun entry(draft: JSONObject, place: Place): JSONObject? =
    draft.optJSONArray(place.list)?.let { list ->
      (0 until list.length()).mapNotNull(list::optJSONObject).firstOrNull { it.optString("id") == place.entryId }
    }

  override fun find(id: String): Macros.Macro? {
    val place = placeOf(id) ?: return null
    val context = context ?: return null
    val draft = ScriptBuilder.draft(ScriptStore.get(context), place.scriptId) ?: return null
    val entry = entry(draft, place) ?: return null
    val macro = macroOf(entry) ?: Macros.Macro(id, "", emptyList())
    val name = if (place.list == COMMANDS) entry.optString("title") else ScriptBuilder.triggerText(context, entry)
    return macro.copy(id = id, name = name)
  }

  override fun update(macro: Macros.Macro) {
    val place = placeOf(macro.id) ?: return
    val context = context ?: return
    val draft = ScriptBuilder.draft(ScriptStore.get(context), place.scriptId) ?: return
    val entry = entry(draft, place) ?: return
    entry.put("macro", JSONObject(macroJson(macro.copy(id = place.entryId))))
    if (place.list == COMMANDS && macro.name.isNotBlank()) entry.put("title", macro.name)
    save(context, draft)
  }

  override fun canRename(id: String): Boolean = placeOf(id)?.list == COMMANDS

  override fun delete(id: String) {
    val place = placeOf(id) ?: return
    val context = context ?: return
    val draft = ScriptBuilder.draft(ScriptStore.get(context), place.scriptId) ?: return
    val list = draft.optJSONArray(place.list) ?: return
    val index = (0 until list.length()).firstOrNull { list.optJSONObject(it)?.optString("id") == place.entryId } ?: return
    list.remove(index)
    save(context, draft)
  }

  private fun save(context: Context, draft: JSONObject) {
    try {
      ScriptBuilder.save(context, draft)
    } catch (e: IOException) {
      // The draft stays as it was; the editor shows the macro as saved before.
    }
  }

  /** The macro of a command or trigger of a draft, or null when it has no steps yet. */
  fun macroOf(entry: JSONObject): Macros.Macro? =
    entry.optJSONObject("macro")?.let { Macros.macroFromJson(JSONArray().put(it).toString()) }

  /** [macro] as the JSON object a draft keeps, and a macro file holds in a list. */
  fun macroJson(macro: Macros.Macro): String = JSONArray(Macros.macroToJson(macro)).getJSONObject(0).toString()

  const val COMMANDS = "commands"
  const val TRIGGERS = "triggers"
  private val LISTS = setOf(COMMANDS, TRIGGERS)
}
