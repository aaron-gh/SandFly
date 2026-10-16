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

package com.google.android.accessibility.talkback.scripting

import android.content.Context
import androidx.core.os.ConfigurationCompat
import java.util.Locale
import org.json.JSONObject

class ScriptTranslations private constructor(val language: String, val messages: JSONObject) {
  fun text(english: String): String =
    (messages.opt(english) as? String)?.ifEmpty { null } ?: english

  companion object {
    private const val FOLDER = "locales/"

    fun languageOf(context: Context): Locale =
      ConfigurationCompat.getLocales(context.resources.configuration)[0] ?: Locale.getDefault()

    fun load(locale: Locale, read: (String) -> ByteArray?): ScriptTranslations {
      val messages = JSONObject()
      tagsOf(locale)
        .mapNotNull { tag -> read("$FOLDER$tag.json") }
        .mapNotNull { runCatching { JSONObject(it.decodeToString()) }.getOrNull() }
        .forEach { file -> file.keys().forEach { messages.put(it, file.get(it)) } }
      return ScriptTranslations(locale.toLanguageTag(), messages)
    }

    private fun tagsOf(locale: Locale): List<String> {
      val parts = locale.toLanguageTag().split('-')
      val prefixes = parts.indices.map { parts.take(it + 1).joinToString("-") }
      val withRegion = locale.country.takeIf { it.isNotEmpty() }?.let { "${parts.first()}-$it" }
      return (prefixes + listOfNotNull(withRegion)).distinct().sortedBy { tag ->
        tag.count { it == '-' }
      }
    }
  }
}
