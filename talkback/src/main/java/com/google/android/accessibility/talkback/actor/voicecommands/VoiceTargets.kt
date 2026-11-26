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
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.commands.Commands
import com.google.android.accessibility.talkback.gesture.OpenAppActions
import com.google.android.accessibility.utils.SharedPreferencesUtils
import java.util.Locale

/**
 * The commands, apps and custom voice commands a voice command can name, read once for one
 * command. Called off the main thread.
 */
class VoiceTargets(private val context: Context, private val locale: Locale) : VoiceGrammar.Resolver {

  /** Every command but voice commands itself, by category. */
  val categories: List<Commands.Category> by lazy {
    val own = context.getString(R.string.shortcut_value_voice_commands)
    Commands.categories(context).map { category ->
      Commands.Category(category.titleRes, category.commands.filter { it.value != own })
    }
  }

  /** Every command by its value. */
  val commandsByValue: Map<String, Commands.Command> by lazy {
    categories.flatMap { it.commands }.associateBy { it.value }
  }

  private val commandsByName: Map<String, Commands.Command> by lazy {
    commandNames(categories, apps, locale, aliases(context))
  }

  private val custom: List<CustomVoiceCommands.Entry> by lazy {
    CustomVoiceCommands.read(SharedPreferencesUtils.getSharedPreferences(context))
  }

  val apps: List<OpenAppActions.App> by lazy { OpenAppActions.apps(context) }

  override fun custom(normalized: String): VoiceStep.Command? {
    val entry = custom.firstOrNull { VoiceGrammar.normalize(it.phrase, locale) == normalized } ?: return null
    val label = commandsByValue[entry.value]?.label ?: Commands.find(context, entry.value)?.label ?: entry.phrase
    return VoiceStep.Command(entry.value, label)
  }

  override fun command(normalized: String): VoiceStep.Command? =
    commandsByName[normalized]?.let { VoiceStep.Command(it.value, it.label) }

  override fun app(name: String): VoiceStep.OpenApp? = findApp(apps, name, locale)

  companion object {
    /**
     * Commands by their normalized names. SandFly's own commands come first, so a command wins over
     * an app of the same name. Apps go by their bare names, as in "Gmail" or "run Gmail", and not by
     * their command list labels ("Open Gmail"), which the open phrase reads as an app.
     */
    @JvmStatic
    @JvmOverloads
    fun commandNames(
      categories: List<Commands.Category>,
      apps: List<OpenAppActions.App>,
      locale: Locale,
      aliases: Map<String, List<String>> = emptyMap(),
    ): Map<String, Commands.Command> {
      val names = HashMap<String, Commands.Command>()
      val (appCategories, own) = categories.partition { it.titleRes == R.string.shortcut_title_open_app }
      val ownCommands = own.flatMap { it.commands }
      for (command in ownCommands) {
        names.putIfAbsent(VoiceGrammar.normalize(command.label, locale), command)
      }
      VoiceEditingRules.addAliases(names, ownCommands, aliases, locale)
      val appCommands = appCategories.flatMap { it.commands }.associateBy { it.value }
      for (app in apps) {
        val command = appCommands[app.value] ?: continue
        names.putIfAbsent(VoiceGrammar.normalize(app.label, locale), command)
      }
      return names
    }

    /**
     * Other names of commands, by command value: "new line", "new paragraph" and "press enter" for
     * New line. They are names, so "press enter" runs New line instead of tapping an item.
     */
    @JvmStatic
    fun aliases(context: Context): Map<String, List<String>> =
      mapOf(
        context.getString(R.string.shortcut_value_new_line) to
          context.resources.getStringArray(R.array.voice_commands_new_line).toList()
      )

    /** The text editing phrases, in the language of [context]. */
    @JvmStatic
    fun textPhrases(context: Context): VoiceGrammar.TextPhrases {
      val resources = context.resources
      fun array(id: Int): List<String> = resources.getStringArray(id).toList()
      return VoiceGrammar.TextPhrases(
        move = array(R.array.voice_commands_text_move),
        select = array(R.array.voice_commands_text_select),
        copy = array(R.array.voice_commands_text_copy),
        cut = array(R.array.voice_commands_text_cut),
        delete = array(R.array.voice_commands_text_delete),
        next = array(R.array.voice_commands_text_next),
        previous = array(R.array.voice_commands_text_previous),
        start = array(R.array.voice_commands_text_start),
        end = array(R.array.voice_commands_text_end),
        units =
          mapOf(
            TextUnit.CHARACTER to array(R.array.voice_commands_text_characters),
            TextUnit.WORD to array(R.array.voice_commands_text_words),
            TextUnit.SENTENCE to array(R.array.voice_commands_text_sentences),
            TextUnit.LINE to array(R.array.voice_commands_text_lines),
            TextUnit.PARAGRAPH to array(R.array.voice_commands_text_paragraphs),
            TextUnit.TEXT to array(R.array.voice_commands_text_whole),
          ),
        numbers = array(R.array.voice_commands_text_numbers),
      )
    }

    /**
     * The app named [name]: its whole name, else the one whose name starts with it, else the one
     * with a word that is it. Null when none is, or when several are equally good and none is the
     * whole name.
     */
    @JvmStatic
    fun findApp(apps: List<OpenAppActions.App>, name: String, locale: Locale): VoiceStep.OpenApp? {
      val query = VoiceGrammar.normalize(name, locale)
      if (query.isEmpty()) return null
      val scored =
        apps.mapNotNull { app ->
          val score = VoiceContacts.score(VoiceGrammar.normalize(app.label, locale), query)
          if (score > 0) score to app else null
        }
      val best = scored.maxOfOrNull { it.first } ?: return null
      val found = scored.filter { it.first == best }.map { it.second }
      // One app can have several home screen activities with one name.
      if (found.distinctBy { it.label }.size > 1 && best < 3) return null
      val app = found.first()
      return VoiceStep.OpenApp(app.label, app.value)
    }
  }
}
