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
package com.google.android.accessibility.talkback.contextmenu

import android.content.Context
import android.view.Menu
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.braillecommands.BrailleCommands
import com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED
import com.google.android.accessibility.utils.SharedPreferencesUtils

/** Fills the menu that chooses the braille command profile in use. */
object BrailleCommandProfileMenuProcessor {

  /**
   * Adds an item for Default and each profile, in the order of the Braille commands screen, or
   * none when there are no profiles to choose between.
   */
  @JvmStatic
  fun prepareMenu(context: Context, pipeline: Pipeline.FeedbackReturner, menu: ContextMenu) {
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val choices = BrailleCommands.choices(context, prefs)
    if (choices.size < 2) return
    val activeId = BrailleCommands.activeId(prefs)
    for ((id, name) in choices) {
      val item =
        ContextMenu.createMenuItem(
          context,
          R.id.group_braille_command_profile,
          Menu.NONE,
          Menu.NONE,
          if (id == activeId) context.getString(R.string.gesture_profile_menu_item_in_use, name)
          else name,
        )
      item.setOnMenuItemClickListener {
        // Null when the profile was deleted while the menu was open.
        BrailleCommands.select(context, prefs, id)?.let {
          pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(it))
        }
        true
      }
      menu.add(item)
    }
  }
}
