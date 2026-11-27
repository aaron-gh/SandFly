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

import android.content.Context
import android.content.SharedPreferences
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.util.Locale
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Voice commands that ended in "not understood", counted by their phrase as custom voice commands
 * are matched. A phrase not understood [TIMES] times is listed in Voice commands settings, where it
 * can become a custom voice command. Kept in a file of its own, [PREFS], which settings files and
 * settings profiles leave out.
 */
object NotUnderstoodPhrases {
  /** The preference file of the counts. */
  const val PREFS = "voice_phrases_not_understood"

  private const val KEY = "phrases"

  /** How many times a phrase is not understood before it is listed. */
  const val TIMES = 3

  /** The most phrases kept; the ones not understood longest ago go first. */
  const val MAX = 50

  /** How long a phrase not yet listed is kept after it was last not understood. */
  @JvmField val COUNTING_MS = TimeUnit.DAYS.toMillis(7)

  /** How long a listed phrase is kept after it was last not understood. */
  @JvmField val LISTED_MS = TimeUnit.DAYS.toMillis(30)

  private const val MAX_COUNT = 1_000

  /** [phrase] as said, [key] as matched, [count] times not understood, the last at [lastMs]. */
  data class Entry(val key: String, val phrase: String, val count: Int, val lastMs: Long) {
    val listed: Boolean
      get() = count >= TIMES
  }

  /** Whether Save phrases not understood is on. */
  @JvmStatic
  fun isOn(context: Context): Boolean =
    SharedPreferencesUtils.getSharedPreferences(context)
      .getBoolean(context.getString(R.string.pref_voice_phrases_not_understood_key), true)

  /**
   * Counts [phrase] as not understood once more, and returns whether it is listed from now on. Does
   * nothing when Save phrases not understood is off.
   */
  @JvmStatic
  fun record(context: Context, phrase: String, locale: Locale): Boolean {
    if (!isOn(context)) return false
    val prefs = prefs(context)
    val (entries, listed) = add(parse(prefs.getString(KEY, null)), phrase, locale, System.currentTimeMillis())
    write(prefs, entries)
    return listed
  }

  /** The listed phrases, the one not understood most recently first. */
  @JvmStatic
  fun listed(context: Context): List<Entry> =
    listed(parse(prefs(context).getString(KEY, null)), System.currentTimeMillis())

  /** Forgets the phrase whose key is [key]. */
  @JvmStatic
  fun remove(context: Context, key: String) {
    val prefs = prefs(context)
    write(prefs, parse(prefs.getString(KEY, null)).filter { it.key != key })
  }

  /** Forgets every phrase, as when Save phrases not understood is turned off. */
  @JvmStatic
  fun clear(context: Context) {
    prefs(context).edit().clear().apply()
  }

  /** The key [phrase] is counted under: its words as custom voice commands are matched. */
  fun key(phrase: String, locale: Locale): String = VoiceGrammar.normalize(phrase, locale)

  /**
   * [entries] with [phrase] counted once more at [nowMs], without the phrases kept too long and
   * those over [MAX], and whether [phrase] has just been listed.
   */
  internal fun add(entries: List<Entry>, phrase: String, locale: Locale, nowMs: Long): Pair<List<Entry>, Boolean> {
    val text = phrase.trim().take(CustomVoiceCommands.MAX_PHRASE)
    val key = key(text, locale)
    val kept = current(entries, nowMs)
    if (key.isEmpty()) return kept to false
    val old = kept.firstOrNull { it.key == key }
    val count = minOf((old?.count ?: 0) + 1, MAX_COUNT)
    val entry = Entry(key, text, count, nowMs)
    var result = listOf(entry) + kept.filter { it.key != key }
    if (result.size > MAX) {
      // The phrase just counted stays, then the listed phrases, then the most recent.
      val chosen =
        result
          .sortedWith(
            compareByDescending<Entry> { it.key == key }.thenByDescending { it.listed }.thenByDescending { it.lastMs }
          )
          .take(MAX)
          .map { it.key }
          .toSet()
      result = result.filter { it.key in chosen }
    }
    return result to (count == TIMES)
  }

  /** [entries] without the ones kept too long at [nowMs]. */
  internal fun current(entries: List<Entry>, nowMs: Long): List<Entry> =
    entries.filter { nowMs - it.lastMs <= if (it.listed) LISTED_MS else COUNTING_MS }

  /** The listed phrases of [entries] at [nowMs], the one not understood most recently first. */
  internal fun listed(entries: List<Entry>, nowMs: Long): List<Entry> =
    current(entries, nowMs).filter { it.listed }.sortedByDescending { it.lastMs }

  internal fun parse(json: String?): List<Entry> {
    if (json.isNullOrEmpty()) return emptyList()
    return try {
      val array = JSONArray(json)
      val seen = HashSet<String>()
      (0 until minOf(array.length(), MAX)).mapNotNull { index ->
        val item = array.optJSONObject(index) ?: return@mapNotNull null
        val key = item.optString("key").take(CustomVoiceCommands.MAX_PHRASE)
        val phrase = item.optString("phrase").trim().take(CustomVoiceCommands.MAX_PHRASE)
        val count = item.optInt("count").coerceAtMost(MAX_COUNT)
        if (key.isEmpty() || phrase.isEmpty() || count < 1 || !seen.add(key)) null
        else Entry(key, phrase, count, item.optLong("last"))
      }
    } catch (e: JSONException) {
      emptyList()
    }
  }

  internal fun serialize(entries: List<Entry>): String {
    val array = JSONArray()
    for (entry in entries.take(MAX)) {
      array.put(
        JSONObject()
          .put("key", entry.key)
          .put("phrase", entry.phrase)
          .put("count", entry.count)
          .put("last", entry.lastMs)
      )
    }
    return array.toString()
  }

  private fun write(prefs: SharedPreferences, entries: List<Entry>) {
    if (entries.isEmpty()) prefs.edit().remove(KEY).apply()
    else prefs.edit().putString(KEY, serialize(entries)).apply()
  }

  private fun prefs(context: Context): SharedPreferences = SharedPreferencesUtils.getSharedPreferences(context, PREFS)
}
