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

package com.google.android.accessibility.talkback.userguide

import android.content.Context
import android.content.res.Configuration
import android.text.TextUtils
import android.view.View
import androidx.annotation.WorkerThread
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.commands.Commands
import com.google.android.accessibility.talkback.gesture.GestureLayout
import com.google.android.accessibility.talkback.keyboard.KeyComboManager
import com.google.android.accessibility.talkback.keyboard.KeyComboModel
import com.google.android.accessibility.talkback.keyboard.Keymap
import com.google.android.accessibility.talkback.keyboard.NewKeyComboModel
import java.io.FileNotFoundException
import java.util.Locale
import org.xmlpull.v1.XmlPullParser

/**
 * The user guide: the chapter files in the assets under user-guide/, in the language of the
 * device where translated, with the lists that SandFly makes from its defaults.
 */
object UserGuide {
  private const val ROOT = "user-guide"
  private const val ENGLISH = "en"
  private const val API = "api"

  /**
   * Returns the page of the guide, or with [section], the page of that heading. [keyComboManager]
   * names keys as the device does.
   */
  @WorkerThread
  fun build(context: Context, keyComboManager: KeyComboManager, section: String? = null): String {
    val folders = languageFolders(context, context.resources.configuration.locales[0])
    val names = context.assets.list("$ROOT/$ENGLISH").orEmpty().filter { it.endsWith(".md") }.sorted()
    val chapters =
      names.map { name ->
        val (folder, text) = read(context, folders, name) ?: (ENGLISH to "")
        GuideMarkdown.Chapter(name, text, langOf(folder))
      }
    val sources =
      object : GuideMarkdown.Sources {
        override fun include(name: String): String? =
          read(context, folders.filter { it != ENGLISH }, name)?.second ?: asset(context, "$ROOT/$API/$name")

        override fun generate(name: String): String? =
          when (name) {
            "gestures" -> gestures(context)
            "keyboard-shortcuts" -> keyboardShortcuts(context, keyComboManager)
            else -> null
          }
      }
    val strings =
      GuideMarkdown.Strings(
        title = context.getString(R.string.user_guide_title),
        contents = context.getString(R.string.user_guide_contents),
        appendix = { letter, title -> context.getString(R.string.user_guide_appendix, letter, title) },
        backToContents = context.getString(R.string.user_guide_back_to_contents),
      )
    val locale = context.resources.configuration.locales[0]
    val rtl = TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL
    val dark =
      context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
        Configuration.UI_MODE_NIGHT_YES
    return GuideMarkdown.render(chapters, sources, strings, langOf(folders.first()), rtl, dark, section)
  }

  /**
   * Returns the folders to read a file from, in order: the language and region of [locale], its
   * language, then English. Folders are named like resource folders, such as pt-rBR.
   */
  private fun languageFolders(context: Context, locale: Locale): List<String> {
    val available = context.assets.list(ROOT).orEmpty().map { it.substringBefore('/') }.toSet()
    val languages = listOf(locale.language) + LEGACY[locale.language].orEmpty()
    val folders = mutableListOf<String>()
    for (language in languages) {
      if (locale.country.isNotEmpty()) folders += language + "-r" + locale.country
      folders += language
    }
    return (folders.filter { it in available && it != ENGLISH } + ENGLISH).distinct()
  }

  // Android and Java name some languages by older codes.
  private val LEGACY =
    mapOf("in" to listOf("id"), "id" to listOf("in"), "iw" to listOf("he"), "he" to listOf("iw"), "ji" to listOf("yi"))

  private fun langOf(folder: String): String = folder.replace("-r", "-")

  /** Returns the first of [folders] that has [name], with its text. */
  private fun read(context: Context, folders: List<String>, name: String): Pair<String, String>? {
    for (folder in folders) {
      asset(context, "$ROOT/$folder/$name")?.let {
        return folder to it
      }
    }
    return null
  }

  private fun asset(context: Context, path: String): String? =
    try {
      context.assets.open(path).use { it.readBytes().decodeToString() }
    } catch (e: FileNotFoundException) {
      null
    }

  // -----------------------------------------------------------------------------------------------
  // Generated lists.

  /** The default gestures of each gesture layout, by the groups of the Gestures screen. */
  private fun gestures(context: Context): String {
    val android = "http://schemas.android.com/apk/res/android"
    class Gesture(val key: String, val title: String, val sandFlyDefault: String?)
    val groups = mutableListOf<Pair<String, MutableList<Gesture>>>()
    context.resources.getXml(R.xml.gesture_preferences).use { parser ->
      while (parser.next() != XmlPullParser.END_DOCUMENT) {
        if (parser.eventType != XmlPullParser.START_TAG) continue
        val title = parser.getAttributeResourceValue(android, "title", 0)
        if (parser.name.endsWith("PreferenceCategory")) {
          groups += (if (title == 0) "" else context.getString(title)) to mutableListOf()
          continue
        }
        if (!parser.name.endsWith(".GestureListPreference")) continue
        val key = parser.getAttributeResourceValue(android, "key", 0)
        if (key == 0 || title == 0 || groups.isEmpty()) continue
        val default = parser.getAttributeResourceValue(android, "defaultValue", 0)
        groups.last().second +=
          Gesture(context.getString(key), context.getString(title), if (default == 0) null else context.getString(default))
      }
    }
    val unassigned = context.getString(R.string.shortcut_value_unassigned)
    val text = StringBuilder()
    for ((layout, nameRes) in
      listOf(
        GestureLayout.SANDFLY to R.string.gesture_layout_sandfly,
        GestureLayout.VOICEOVER to R.string.gesture_layout_voiceover,
        GestureLayout.BLACKBERRY to R.string.gesture_layout_blackberry,
      )) {
      text.append("### ").append(cell(context.getString(nameRes))).append(" {#appendix-gestures-").append(layout).append("}\n\n")
      for ((index, group) in groups.withIndex()) {
        val rows =
          group.second.mapNotNull { gesture ->
            val value =
              if (layout == GestureLayout.SANDFLY) gesture.sandFlyDefault
              else GestureLayout.layoutDefault(context, layout, gesture.key)
            if (value == null || value == unassigned) return@mapNotNull null
            val command = Commands.label(context, value) ?: return@mapNotNull null
            gesture.title to command
          }
        if (rows.isEmpty()) continue
        text.append("#### ").append(cell(group.first)).append(" {#appendix-gestures-").append(layout).append('-').append(index).append("}\n\n")
        table(text, context.getString(R.string.user_guide_gesture), context.getString(R.string.user_guide_command), rows)
      }
    }
    return text.toString()
  }

  /** The default keyboard shortcuts of each built-in keymap, by the categories of commands. */
  private fun keyboardShortcuts(context: Context, keyComboManager: KeyComboManager): String {
    val categories = Commands.categories(context)
    val text = StringBuilder()
    for (keymap in Keymap.entries) {
      val model = NewKeyComboModel(context, keymap)
      val id = keymap.name.lowercase(Locale.ROOT).replace('_', '-')
      text.append("### ").append(cell(context.getString(keymap.nameRes))).append(" {#appendix-keyboard-shortcuts-").append(id).append("}\n\n")
      text.append(context.getString(R.string.user_guide_sandfly_key, context.getString(model.sandFlyKey.nameRes))).append("\n\n")
      val seen = HashSet<String>()
      for ((index, category) in categories.withIndex()) {
        val rows =
          category.commands.flatMap { command ->
            // A command's second key, such as P for Next paragraph in the NVDA keymap, follows it.
            val second = command.secondKeyboardAction
            listOfNotNull(
                command.keyboardKey(context) to command.label,
                second?.let { it.getKey(context.resources) to it.getDescription(context.resources) },
              )
              .mapNotNull { (key, label) ->
                if (!seen.add(key)) return@mapNotNull null
                val keyCombo = model.getDefaultKeyCombo(key)
                if (keyCombo.keyComboCode == KeyComboModel.KEY_COMBO_CODE_UNASSIGNED.toLong()) return@mapNotNull null
                label to keyComboManager.getKeyComboStringRepresentation(keyCombo, model)
              }
          }
        if (rows.isEmpty()) continue
        text.append("#### ").append(cell(context.getString(category.titleRes))).append(" {#appendix-keyboard-shortcuts-").append(id).append('-').append(index).append("}\n\n")
        table(text, context.getString(R.string.user_guide_command), context.getString(R.string.user_guide_keyboard_shortcut), rows)
      }
    }
    return text.toString()
  }

  private fun table(text: StringBuilder, first: String, second: String, rows: List<Pair<String, String>>) {
    text.append("| ").append(cell(first)).append(" | ").append(cell(second)).append(" |\n| --- | --- |\n")
    for ((a, b) in rows) {
      text.append("| ").append(cell(a)).append(" | ").append(cell(b)).append(" |\n")
    }
    text.append('\n')
  }

  /** Returns [text] as the text of a table cell or heading, with Markdown marks shown as text. */
  private fun cell(text: String): String {
    val out = StringBuilder()
    for (c in text) {
      if (c == '\n') {
        out.append(' ')
        continue
      }
      if (c in "\\`*_[]|#<") out.append('\\')
      out.append(c)
    }
    return out.toString().trim()
  }
}
