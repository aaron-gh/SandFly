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

package com.google.android.accessibility.talkback.actor.voicecommands

import android.content.SharedPreferences
import java.util.Locale
import java.util.UUID
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Voice commands the user made: a phrase that runs a command, such as a macro or a script's. */
object CustomVoiceCommands {
  const val KEY = "pref_custom_voice_commands"

  /** The most custom voice commands kept. */
  const val MAX = 500

  /** The longest phrase kept. */
  const val MAX_PHRASE = 200

  data class Entry(val id: String, val phrase: String, val value: String)

  @JvmStatic fun newId(): String = UUID.randomUUID().toString()

  @JvmStatic
  fun read(prefs: SharedPreferences): List<Entry> = parse(prefs.getString(KEY, null))

  @JvmStatic
  fun write(prefs: SharedPreferences, entries: List<Entry>) {
    prefs.edit().putString(KEY, serialize(entries)).apply()
  }

  /** Returns the entry whose phrase is [phrase] once both are normalized, or null. */
  fun find(entries: List<Entry>, phrase: String, locale: Locale): Entry? {
    val normalized = VoiceGrammar.normalize(phrase, locale)
    if (normalized.isEmpty()) return null
    return entries.firstOrNull { VoiceGrammar.normalize(it.phrase, locale) == normalized }
  }

  internal fun parse(json: String?): List<Entry> {
    if (json.isNullOrEmpty()) return emptyList()
    return try {
      val array = JSONArray(json)
      (0 until minOf(array.length(), MAX)).mapNotNull { index ->
        val item = array.optJSONObject(index) ?: return@mapNotNull null
        val phrase = item.optString("phrase").trim().take(MAX_PHRASE)
        val value = item.optString("value")
        if (phrase.isEmpty() || value.isEmpty()) null
        else Entry(item.optString("id").ifEmpty { newId() }, phrase, value)
      }
    } catch (e: JSONException) {
      emptyList()
    }
  }

  internal fun serialize(entries: List<Entry>): String {
    val array = JSONArray()
    for (entry in entries.take(MAX)) {
      array.put(JSONObject().put("id", entry.id).put("phrase", entry.phrase).put("value", entry.value))
    }
    return array.toString()
  }
}
