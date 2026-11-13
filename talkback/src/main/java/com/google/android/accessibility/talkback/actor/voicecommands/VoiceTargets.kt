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
    val names = HashMap<String, Commands.Command>()
    for (command in categories.flatMap { it.commands }) {
      names.putIfAbsent(VoiceGrammar.normalize(command.label, locale), command)
    }
    names
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
