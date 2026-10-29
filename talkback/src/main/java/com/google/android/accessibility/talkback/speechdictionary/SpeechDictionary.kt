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

package com.google.android.accessibility.talkback.speechdictionary

import android.content.Context
import android.content.SharedPreferences
import android.text.SpannableStringBuilder
import com.google.android.accessibility.talkback.notifications.Replacement
import com.google.android.accessibility.utils.output.SpeechControllerImpl
import java.util.regex.Matcher
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** A [Replacement] in the speech dictionary, used in every app, or only in [app]. */
data class DictionaryEntry(val replacement: Replacement, val app: String = "", val appName: String = "") {
  fun toJson(): JSONObject = replacement.toJson().put("app", app).put("appName", appName)

  companion object {
    fun fromJson(json: JSONObject) =
      DictionaryEntry(Replacement.fromJson(json), json.optString("app"), json.optString("appName"))
  }
}

/**
 * Settings > Speech > Speech dictionary: text that all speech says differently, such as a word as
 * another word, or a symbol as a space. Entries apply in order, each to the text the one before it
 * left, and those of one app only while that app's window is the last one shown. SandFly's own
 * windows are left as they are, so that the entries are read as written.
 *
 * The replaced text keeps the spans of the text around it, such as its language.
 */
object SpeechDictionary {

  const val KEY = "pref_speech_dictionary"

  // Longer speech, such as a whole page being read, is not searched with regular expressions, so
  // that one that takes long on it can't hold up speech.
  private const val MAX_REGEX_SUBJECT = 5000

  @Volatile private var entries: List<DictionaryEntry> = emptyList()

  // The package of the window shown last.
  @Volatile private var app: String = ""

  private var ownPackage: String = ""

  /** Reads the entries from [prefs], and applies them to speech while there are any. */
  @JvmStatic
  fun load(context: Context, prefs: SharedPreferences) {
    ownPackage = context.packageName
    entries = read(prefs)
    SpeechControllerImpl.setDictionaryFilter(
      if (entries.isEmpty()) null else SpeechControllerImpl.SpeechFilter(::apply)
    )
  }

  @JvmStatic
  fun read(prefs: SharedPreferences): List<DictionaryEntry> = fromJson(prefs.getString(KEY, null))

  fun write(prefs: SharedPreferences, list: List<DictionaryEntry>) {
    prefs.edit().putString(KEY, toJson(list)).apply()
  }

  /** Notes the app whose window is shown, for the entries of one app. */
  @JvmStatic
  fun onWindowShown(packageName: CharSequence?) {
    app = packageName?.toString().orEmpty()
  }

  /** Returns [text] with the entries applied, or null when none changed it. */
  @JvmStatic
  fun apply(text: CharSequence): CharSequence? {
    val app = app
    return if (app == ownPackage) null else apply(text, entries, app)
  }

  internal fun apply(text: CharSequence, entries: List<DictionaryEntry>, app: String): CharSequence? {
    var builder: SpannableStringBuilder? = null
    for (entry in entries) {
      if (entry.app.isNotEmpty() && entry.app != app) continue
      val replacement = entry.replacement
      if (replacement.find.isEmpty()) continue
      val current = builder ?: text
      if (replacement.regex && current.length > MAX_REGEX_SUBJECT) continue
      val changes = matches(current.toString(), replacement)
      if (changes.isEmpty()) continue
      val target = builder ?: SpannableStringBuilder(text).also { builder = it }
      // From the end, so that earlier positions stay where they were.
      for (change in changes.asReversed()) {
        target.replace(change.start, change.end, change.text)
      }
    }
    return builder
  }

  private class Change(val start: Int, val end: Int, val text: String)

  /** Where [replacement] matches [subject], and the text that each match is said as. */
  private fun matches(subject: String, replacement: Replacement): List<Change> {
    val pattern = replacement.pattern ?: return emptyList()
    val with = if (replacement.regex) replacement.replace else Matcher.quoteReplacement(replacement.replace)
    val matcher = pattern.toPattern().matcher(subject)
    val changes = ArrayList<Change>()
    var appended = 0
    try {
      while (matcher.find()) {
        // appendReplacement adds the text since the last match, then this match's replacement,
        // with any groups it names filled in.
        val buffer = StringBuffer()
        matcher.appendReplacement(buffer, with)
        changes += Change(matcher.start(), matcher.end(), buffer.substring(matcher.start() - appended))
        appended = matcher.end()
      }
    } catch (e: RuntimeException) {
      // A replacement that names a group the expression doesn't have.
      return emptyList()
    }
    return changes
  }

  fun fromJson(json: String?): List<DictionaryEntry> {
    if (json.isNullOrEmpty()) return emptyList()
    return try {
      val array = JSONArray(json)
      (0 until array.length()).mapNotNull { array.optJSONObject(it)?.let(DictionaryEntry::fromJson) }
    } catch (e: JSONException) {
      emptyList()
    }
  }

  fun toJson(list: List<DictionaryEntry>): String =
    JSONArray().also { array -> list.forEach { array.put(it.toJson()) } }.toString()
}
