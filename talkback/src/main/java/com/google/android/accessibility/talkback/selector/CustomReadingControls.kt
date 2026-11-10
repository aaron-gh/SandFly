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
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.commands.CommandMenus
import com.google.android.accessibility.talkback.commands.Commands
import com.google.android.accessibility.talkback.scripting.ScriptReadingControl
import com.google.android.accessibility.utils.FeatureSupport
import java.util.function.Consumer

/**
 * Reading controls the user makes from commands. Adjusting one chooses a command, and double-tap
 * runs it. Main thread only.
 */
object CustomReadingControls {
  // The command last chosen in each control, by control ID, kept while the app runs.
  private val chosen = HashMap<String, String>()

  /** Returns the custom reading controls that have commands. [speak] says the command chosen. */
  @JvmStatic
  fun controls(
    context: Context,
    prefs: SharedPreferences,
    speak: Consumer<CharSequence>,
  ): List<ScriptReadingControl> {
    if (!FeatureSupport.doesServiceHandleDoubleTap()) return emptyList()
    return CommandMenus.controls(prefs)
      .filter { it.items.isNotEmpty() }
      .map { control ->
        ScriptReadingControl(
          control.id,
          control.name,
          adjuster = { next ->
            val action = AiActions.step(actions(context, control), chosen[control.id], next)
            if (action != null) {
              chosen[control.id] = action.value
              speak.accept(action.label)
            }
          },
          activator = {
            val action = current(context, control)
            val service = TalkBackService.getInstance()
            action != null && service != null && service.performCommand(action.value)
          },
          choice = { current(context, control)?.label },
        )
      }
  }

  private fun current(context: Context, control: CommandMenus.Menu): AiActions.Action? {
    val actions = actions(context, control)
    return actions.firstOrNull { it.value == chosen[control.id] } ?: actions.firstOrNull()
  }

  // The commands this device has. Looked up only when a control is in use.
  private fun actions(context: Context, control: CommandMenus.Menu): List<AiActions.Action> {
    val values = control.items.filterIsInstance<CommandMenus.Command>().map { it.value }
    return values.zip(Commands.labels(context, values)).mapNotNull { (value, label) ->
      label?.let { AiActions.Action(value, it) }
    }
  }
}
