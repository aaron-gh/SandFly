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

package com.google.android.accessibility.talkback.backup

import android.content.Context
import androidx.annotation.WorkerThread
import com.google.android.accessibility.talkback.BuildConfig
import com.google.android.accessibility.talkback.commands.Commands
import com.google.android.accessibility.talkback.keyboard.KeyText
import com.google.android.accessibility.talkback.preference.search.SettingsIndex
import org.json.JSONArray
import org.json.JSONObject

/**
 * The file format reference: the description of item files, then the gestures, commands and
 * settings of this phone, for writing item files by hand or with an AI chat.
 */
object ItemFileReference {
  private const val ASSET = "item-files.md"

  /** Returns the reference as Markdown. [context] is a settings screen's, with its theme. */
  @WorkerThread
  fun build(context: Context): String {
    val text = StringBuilder()
    text.append(context.assets.open(ASSET).use { it.readBytes().decodeToString() }.trimEnd())
    text.append("\n\n# This phone\n\n")
    text.append("SandFly ").append(BuildConfig.SANDFLY_VERSION).append(". The lists below are of this phone.\n")

    text.append("\n## Gestures\n\n| Key | Gesture |\n| --- | --- |\n")
    for ((key, name) in ItemFiles.gestures(context)) {
      text.append("| `").append(key).append("` | ").append(cell(name)).append(" |\n")
    }

    text.append("\n## Commands\n")
    for (category in Commands.categories(context)) {
      text.append("\n### ").append(context.getString(category.titleRes)).append("\n\n")
      text.append("| Command | Name |\n| --- | --- |\n")
      for (command in category.commands) {
        text.append("| `").append(cell(command.value)).append("` | ").append(cell(command.label)).append(" |\n")
      }
    }

    text.append("\n## Keys\n\nModifiers: ").append(KeyText.SANDFLY).append(", Shift, Ctrl, Alt, Meta.\n\n")
    text.append("Named keys: ").append(KeyText.namedKeys().joinToString(", ")).append(".\n\n")
    text.append("SandFly keys of keymaps: ").append(ItemFiles.sandFlyKeyNames().joinToString(", ")).append(".\n")

    text.append("\n## Settings\n\n")
    text.append("Settings that a settings profile can set, with the value each has now.\n\n")
    text.append("| Key | Setting | Value now | Choices |\n| --- | --- | --- | --- |\n")
    val seen = HashSet<String>()
    for (entry in SettingsIndex.build(context)) {
      val key = entry.key ?: continue
      val profileKey = SettingsBackup.profileKeyOf(context, key)
      if (!seen.add(profileKey)) continue
      val value = SettingsBackup.currentOrDefault(context, profileKey) ?: continue
      val (file, _) = SettingsBackup.splitProfileKey(profileKey) ?: continue
      val written = if (file == SettingsBackup.DEFAULT_PREFS) key else profileKey
      val name = (entry.path + entry.title).joinToString(" > ")
      val choices =
        entry.choices?.joinToString(", ") { (choice, label) -> "`${cell(choice)}` ${cell(label)}" }.orEmpty()
      text.append("| `").append(cell(written)).append("` | ").append(cell(name)).append(" | `")
        .append(cell(json(value))).append("` | ").append(choices).append(" |\n")
    }
    return text.toString()
  }

  /** [value] as it is written in a file. */
  private fun json(value: Any): String =
    when (value) {
      is String -> JSONObject.quote(value)
      is Set<*> -> JSONArray(value.map { it.toString() }.sorted()).toString()
      else -> value.toString()
    }

  /** [text] in a table cell, where "|" and line breaks would end it. */
  private fun cell(text: CharSequence): String =
    text.toString().replace("|", "\\|").replace(Regex("\\s*[\\r\\n]+\\s*"), " ")
}
