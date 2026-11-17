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
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.io.IOException
import com.google.android.accessibility.talkback.scripting.ScriptBuilder.objects
import org.json.JSONException
import org.json.JSONObject

/**
 * Scripts that SandFly Pro's agent writes: a whole main.lua, saved as a built script so that the
 * person can review and edit it in the script builder. Its manifest becomes builder parts, the rest
 * the builder's Lua code. Until the person accepts it, the script runs without the network and
 * java permissions, so that code written from what was on screen can't send anything away or reach
 * past the script API.
 */
object ScriptAi {
  /** What saving gave: the script's id, or null when nothing was saved, and what is wrong. */
  data class Saved(val id: String?, val problems: List<String>)

  /** Permissions held back until the person accepts the script. */
  val HELD = setOf(ScriptPermission.NETWORK, ScriptPermission.JAVA)

  private const val PREF_REVIEW = "script_ai_review"
  private const val MAX_LOG = 20

  /**
   * Saves [source], a whole main.lua, as the built script [id], or as a new one when [id] is null
   * or not a built script. Problems that stop it being saved come back without an id.
   */
  fun save(context: Context, id: String?, source: String): Saved {
    val manifest =
      try {
        JSONObject(ManifestReader.read(source))
      } catch (e: ManifestException) {
        return Saved(null, listOf("Manifest: ${e.message}"))
      } catch (e: JSONException) {
        return Saved(null, listOf("Manifest: ${e.message}"))
      }
    val code = ScriptBuilder.codeOf(source)
    LuaEngine.syntaxError(code)?.let { return Saved(null, listOf("Lua: $it")) }
    val draft = ScriptBuilder.draftOf(manifest, code)
    if (draft.optString("name").isBlank()) draft.put("name", "Script")
    val store = ScriptStore.get(context)
    val saved =
      try {
        if (id != null && ScriptBuilder.isBuilt(store, id)) {
          draft.put("id", id)
          ScriptBuilder.save(context, draft)
          id
        } else {
          ScriptBuilder.createFrom(context, draft, null)
        }
      } catch (e: IOException) {
        return Saved(null, listOf("Not saved: ${e.message}"))
      } catch (e: ManifestException) {
        return Saved(null, listOf("Manifest: ${e.message}"))
      } catch (e: ScriptApiException) {
        return Saved(null, listOf("Manifest: ${e.message}"))
      }
    // Each version the agent saves is reviewed again, even of a script the person accepted.
    for (permission in HELD) store.setGranted(saved, permission, false)
    setReview(context, saved, reviews(context).optJSONObject(saved)?.optString("summary").orEmpty())
    val problems = mutableListOf<String>()
    draft.optJSONArray("rules").objects().forEachIndexed { i, rule ->
      ScriptBuilder.ruleProblem(rule)?.let { problems += "Rule ${i + 1} is left out: $it" }
    }
    return Saved(saved, problems)
  }

  /** The script's main.lua, for the agent to change, or null. */
  fun source(context: Context, id: String): String? =
    ScriptStore.get(context).readFile(id, ScriptPackage.MAIN)?.decodeToString()

  /** The warnings and errors the script [id] logged, newest first. */
  fun log(id: String): List<String> =
    ScriptLog.entries(id)
      .filter { it.level == ScriptLog.Level.WARN || it.level == ScriptLog.Level.ERROR }
      .take(MAX_LOG)
      .map { "${it.level}: ${it.message}" }

  /** The agent's summary of the script [id] while the person hasn't accepted it, or null. */
  fun review(context: Context, id: String): String? =
    reviews(context).optJSONObject(id)?.takeUnless { it.optBoolean("accepted") }?.optString("summary")

  /** Marks the script [id] as waiting for the person's review, with the agent's [summary]. */
  fun setReview(context: Context, id: String, summary: String) {
    val all = reviews(context)
    all.put(id, JSONObject().put("summary", summary))
    write(context, all)
  }

  /** Keeps the script [id] and gives it the permissions held back while it was reviewed. */
  fun accept(context: Context, id: String) {
    val store = ScriptStore.get(context)
    store.find(id)?.let { script ->
      for (permission in HELD) if (permission in script.manifest.permissions) store.setGranted(id, permission, true)
    }
    val all = reviews(context)
    all.put(id, JSONObject().put("accepted", true))
    write(context, all)
  }

  /** Removes the script [id] the person didn't want. */
  fun discard(context: Context, id: String) {
    ScriptStore.get(context).remove(id)
    val all = reviews(context)
    all.remove(id)
    write(context, all)
  }

  private fun reviews(context: Context): JSONObject =
    SharedPreferencesUtils.getSharedPreferences(context).getString(PREF_REVIEW, null)?.let {
      try {
        JSONObject(it)
      } catch (e: JSONException) {
        null
      }
    } ?: JSONObject()

  private fun write(context: Context, all: JSONObject) {
    SharedPreferencesUtils.getSharedPreferences(context).edit().putString(PREF_REVIEW, all.toString()).apply()
  }
}
