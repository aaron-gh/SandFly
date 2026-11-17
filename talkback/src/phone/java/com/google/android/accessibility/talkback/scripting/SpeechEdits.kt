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

import com.google.android.accessibility.talkback.macros.Macros
import java.util.regex.Matcher
import org.json.JSONArray
import org.json.JSONObject

/**
 * Edits of a text SandFly says, in order: removing and replacing text, removing repeated or
 * unwanted parts, keeping the first parts, and saying something instead, before or after. Parts
 * are the pieces between commas, as SandFly joins an item's texts. Regular expressions are found
 * ignoring case, as in macros.
 */
object SpeechEdits {
  const val REMOVE = "remove"
  const val REPLACE = "replace"
  const val REMOVE_REPEATS = "removeRepeats"
  const val REMOVE_PARTS = "removeParts"
  const val KEEP_PARTS = "keepParts"
  const val SAY = "say"
  const val BEFORE = "before"
  const val AFTER = "after"
  const val SILENCE = "silence"

  val OPS = listOf(REMOVE, REPLACE, REMOVE_REPEATS, REMOVE_PARTS, KEEP_PARTS, SAY, BEFORE, AFTER, SILENCE)

  /** The most characters edited. */
  private const val MAX_TEXT = 10_000

  /** What the edits make of a text. */
  sealed class Result {
    /** Say [text] instead. */
    data class Speak(val text: String) : Result()

    /** Say nothing. */
    data object Silent : Result()

    /** Say the text as it was. */
    data object Unchanged : Result()
  }

  /** What is wrong with [edit], or null. */
  fun problem(edit: JSONObject): String? {
    val op = edit.optString("op")
    val find = edit.optString("find")
    val regex = edit.optBoolean("regex")
    return when {
      op !in OPS -> "Unknown edit $op"
      op in listOf(REMOVE, REPLACE, REMOVE_PARTS) && find.isEmpty() -> "Set the text to find"
      regex && op in listOf(REMOVE, REPLACE, REMOVE_PARTS) && Macros.compilePattern(find) == null ->
        "Not a usable regular expression: $find"
      op == KEEP_PARTS && edit.optInt("count") < 1 -> "Keep at least 1 part"
      op in listOf(SAY, BEFORE, AFTER) && edit.optString("text").isEmpty() -> "Set the text to say"
      else -> null
    }
  }

  fun apply(text: String, edits: JSONArray): Result {
    var said = text.take(MAX_TEXT)
    var before: String? = null
    var after: String? = null
    for (i in 0 until edits.length()) {
      val edit = edits.optJSONObject(i) ?: continue
      if (problem(edit) != null) continue
      val find = edit.optString("find")
      val regex = edit.optBoolean("regex")
      when (edit.optString("op")) {
        REMOVE -> said = replace(said, find, "", regex)
        REPLACE -> said = replace(said, find, edit.optString("with"), regex)
        REMOVE_REPEATS -> {
          val seen = HashSet<String>()
          said = join(parts(said).filter { seen.add(it.lowercase()) })
        }
        REMOVE_PARTS -> said = join(parts(said).filterNot { has(it, find, regex) })
        KEEP_PARTS -> said = join(parts(said).take(edit.optInt("count")))
        SAY -> said = edit.optString("text").replace("{text}", said)
        BEFORE -> before = edit.optString("text").replace("{text}", said)
        AFTER -> after = edit.optString("text").replace("{text}", said)
        SILENCE -> return Result.Silent
      }
      said = tidy(said)
    }
    val result = listOfNotNull(before, said.ifBlank { null }, after).joinToString(", ")
    return when {
      result.isBlank() -> Result.Silent
      result == text -> Result.Unchanged
      else -> Result.Speak(result)
    }
  }

  /** [text] with [find] replaced by [with]: plainly, ignoring case, or as a regular expression. */
  fun replace(text: String, find: String, with: String, regex: Boolean): String {
    if (find.isEmpty()) return text
    if (regex) {
      val pattern = Macros.compilePattern(find) ?: return text
      return try {
        pattern.matcher(text).replaceAll(with)
      } catch (e: RuntimeException) {
        // A replacement with a group the expression doesn't have: replace with the plain text.
        pattern.matcher(text).replaceAll(Matcher.quoteReplacement(with))
      }
    }
    return text.replace(find, with, ignoreCase = true)
  }

  /** Whether [text] has [find]: plainly, ignoring case, or as a regular expression. */
  fun has(text: String, find: String, regex: Boolean): Boolean =
    if (regex) Macros.compilePattern(find)?.matcher(text)?.find() == true else text.contains(find, ignoreCase = true)

  private fun parts(text: String): List<String> = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }

  private fun join(parts: List<String>): String = parts.joinToString(", ")

  /** [text] with no empty parts or doubled spaces, which removing text leaves. */
  private fun tidy(text: String): String = join(parts(text.replace(Regex("[ \\t]{2,}"), " ")))

  /** The result as a speech hook gives it: a text, false for nothing, or null to leave it. */
  fun hookResult(result: Result): Any =
    when (result) {
      is Result.Speak -> JSONObject().put("text", result.text)
      Result.Silent -> JSONObject().put("silent", true)
      Result.Unchanged -> JSONObject.NULL
    }
}
