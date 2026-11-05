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
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.actor.ImageCaptioner
import com.google.android.accessibility.talkback.actor.gemini.GeminiConfiguration
import com.google.android.accessibility.talkback.sandflyai.SandflyAi

/**
 * The commands of the AI actions reading control: Describe screen, Describe image, and SandFly
 * Pro's AI agent command in builds that include it.
 */
object AiActions {
  /** A command value and the label spoken for it. */
  data class Action(val value: String, val label: String)

  // The command value last chosen, kept while the app runs.
  private var selected: String? = null

  /** Returns the actions available now, in reading control order. */
  @JvmStatic
  fun available(context: Context): List<Action> {
    val actions = mutableListOf<Action>()
    val captions = ImageCaptioner.supportsImageCaption(context)
    if (captions && GeminiConfiguration.screenOverviewEnabled(context)) {
      actions.add(
        Action(
          context.getString(R.string.shortcut_value_screen_overview),
          context.getString(R.string.title_summarize_view),
        )
      )
    }
    if (captions) {
      actions.add(
        Action(
          context.getString(R.string.shortcut_value_describe_image),
          context.getString(R.string.title_image_caption),
        )
      )
    }
    SandflyAi.features?.let {
      actions.add(
        Action(context.getString(R.string.shortcut_value_ai_agent), it.agentCommandLabel(context))
      )
    }
    return actions
  }

  /** Returns the chosen action, or the first available one, or null when none is available. */
  @JvmStatic
  fun current(context: Context): Action? {
    val actions = available(context)
    return actions.firstOrNull { it.value == selected } ?: actions.firstOrNull()
  }

  /** Chooses the next or previous action, wrapping around, and returns it. */
  @JvmStatic
  fun step(context: Context, forward: Boolean): Action? {
    val action = step(available(context), selected, forward) ?: return null
    selected = action.value
    return action
  }

  /**
   * Returns the action after or before the one with the value [selected] in [actions], wrapping
   * around. With no choice yet, or one no longer available, the first action is the current one.
   */
  internal fun step(actions: List<Action>, selected: String?, forward: Boolean): Action? {
    if (actions.isEmpty()) {
      return null
    }
    val index = actions.indexOfFirst { it.value == selected }.coerceAtLeast(0)
    return actions[Math.floorMod(index + if (forward) 1 else -1, actions.size)]
  }
}
