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

package com.google.android.accessibility.talkback.contextmenu;

import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;

import android.content.Context;
import android.view.Menu;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.backup.SettingsProfileStore;
import com.google.android.accessibility.talkback.backup.SettingsProfiles;
import java.util.List;
import kotlin.Pair;

/** Fills the menu that chooses the settings profile in use. */
public final class SettingsProfileMenuProcessor {

  private SettingsProfileMenuProcessor() {}

  /**
   * Adds an item for each settings profile, in the order of the Settings profiles screen. Before
   * profiles are used, Default is the only item.
   */
  public static void prepareSettingsProfileMenu(
      Context context, Pipeline.FeedbackReturner pipeline, ContextMenu menu) {
    Pair<List<SettingsProfileStore.Profile>, String> choices = SettingsProfiles.choices(context);
    String activeId = choices.getSecond();
    for (SettingsProfileStore.Profile profile : choices.getFirst()) {
      String id = profile.getId();
      String name = profile.getName();
      ContextMenuItem item =
          ContextMenu.createMenuItem(
              context,
              R.id.group_settings_profile,
              Menu.NONE,
              Menu.NONE,
              id.equals(activeId)
                  ? context.getString(R.string.settings_profile_menu_item_in_use, name)
                  : name);
      item.setOnMenuItemClickListener(
          clicked -> {
            SettingsProfiles.switchTo(
                context,
                id,
                said -> pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(said)));
            return true;
          });
      menu.add(item);
    }
  }
}
