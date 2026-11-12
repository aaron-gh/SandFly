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

package com.google.android.accessibility.talkback.selector

import android.content.Context
import android.content.SharedPreferences
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.actor.ImageCaptioner
import com.google.android.accessibility.talkback.actor.gemini.GeminiConfiguration
import com.google.android.accessibility.talkback.sandflyai.SandflyAi
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.ValueSteps

/**
 * The commands of the AI actions reading control: Describe screen, Describe image, their spoken and
 * question versions, and SandFly Pro's AI agent command in builds that include it. The user chooses
 * which of them the reading control offers.
 */
object AiActions {
  /** A command value and the label spoken for it. */
  data class Action(val value: String, val label: String)

  // The command value last chosen, kept while the app runs.
  private var selected: String? = null

  /** Returns the actions this device supports, in reading control order, chosen or not. */
  @JvmStatic
  fun all(context: Context): List<Action> {
    val actions = mutableListOf<Action>()
    val captions = ImageCaptioner.supportsImageCaption(context)
    fun add(value: Int, label: Int) {
      actions.add(Action(context.getString(value), context.getString(label)))
    }
    if (captions && GeminiConfiguration.screenOverviewEnabled(context)) {
      add(R.string.shortcut_value_screen_overview, R.string.title_summarize_view)
      add(R.string.shortcut_value_speak_screen_description, R.string.title_speak_screen_description)
      add(R.string.shortcut_value_ask_about_screen, R.string.title_ask_about_screen)
    }
    if (captions) {
      add(R.string.shortcut_value_describe_image, R.string.title_image_caption)
      add(R.string.shortcut_value_speak_image_description, R.string.title_speak_image_description)
      if (GeminiConfiguration.imageQnaEnabled(context)) {
        add(R.string.shortcut_value_ask_about_image, R.string.title_ask_about_image)
      }
    }
    SandflyAi.features?.let {
      actions.add(
        Action(context.getString(R.string.shortcut_value_ai_agent), it.agentCommandLabel(context))
      )
      it.commands(context)[SandflyAi.COMMAND_TYPE_AGENT_TASK]?.let { label ->
        actions.add(Action(SandflyAi.COMMAND_TYPE_AGENT_TASK, label))
      }
    }
    return actions
  }

  /** Returns the actions the reading control offers now: the supported ones the user included. */
  @JvmStatic
  fun available(context: Context): List<Action> {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    return included(all(context), includedValues(prefs, context))
  }

  /**
   * Returns the values of the actions the user included, or null when the user hasn't chosen, in
   * which case all are included.
   */
  @JvmStatic
  fun includedValues(prefs: SharedPreferences, context: Context): Set<String>? =
    prefs.getStringSet(context.getString(R.string.pref_ai_actions_included_key), null)

  /** Returns [actions] whose values are in [values], or all of them when [values] is null. */
  internal fun included(actions: List<Action>, values: Set<String>?): List<Action> =
    if (values == null) actions else actions.filter { it.value in values }

  /** Returns the chosen action, or the first available one, or null when none is available. */
  @JvmStatic
  fun current(context: Context): Action? {
    val actions = available(context)
    return actions.firstOrNull { it.value == selected } ?: actions.firstOrNull()
  }

  /** Chooses the next or previous action, as [ValueSteps] goes, and returns it. */
  @JvmStatic
  fun step(context: Context, forward: Boolean): Action? {
    val action = step(available(context), selected, forward) ?: return null
    selected = action.value
    return action
  }

  /**
   * Returns the action after or before the one with the value [selected] in [actions], as
   * [ValueSteps] goes. With no choice yet, or one no longer available, the first action is the
   * current one.
   */
  internal fun step(actions: List<Action>, selected: String?, forward: Boolean): Action? {
    if (actions.isEmpty()) {
      return null
    }
    val index = actions.indexOfFirst { it.value == selected }.coerceAtLeast(0)
    return actions[ValueSteps.step(index, actions.size, forward)]
  }
}
