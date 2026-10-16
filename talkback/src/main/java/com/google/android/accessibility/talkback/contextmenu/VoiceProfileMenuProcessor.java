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

package com.google.android.accessibility.talkback.contextmenu;

import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.Menu;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.speech.VoiceProfileNames;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.output.VoiceProfiles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Fills the menu that chooses the voice profile in use. */
public final class VoiceProfileMenuProcessor {

  private VoiceProfileMenuProcessor() {}

  /**
   * Returns an item for Backtalk default and each voice profile, in the order of the Voice profiles
   * screen, or none when there are no profiles to choose between.
   */
  private static List<ContextMenuItem> getMenuItems(
      Context context, Pipeline.FeedbackReturner pipeline) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    List<ContextMenuItem> menuItems = new ArrayList<>();
    List<String> ids = VoiceProfiles.ids(prefs);
    if (ids.isEmpty()) {
      return menuItems;
    }
    Map<String, String> names = VoiceProfileNames.names(context, prefs);
    String activeId = VoiceProfiles.activeId(prefs);

    List<String> choices = new ArrayList<>();
    choices.add("");
    choices.addAll(ids);
    for (String id : choices) {
      String name =
          id.isEmpty() ? context.getString(R.string.voice_profile_default) : names.get(id);
      if (name == null) {
        continue;
      }
      ContextMenuItem item =
          ContextMenu.createMenuItem(
              context,
              R.id.group_voice_profile,
              Menu.NONE,
              Menu.NONE,
              id.equals(activeId)
                  ? context.getString(R.string.voice_profile_menu_item_in_use, name)
                  : name);
      item.setOnMenuItemClickListener(
          clicked -> {
            // The profile may have been deleted while the menu was open.
            if (!id.isEmpty() && !VoiceProfiles.ids(prefs).contains(id)) {
              return true;
            }
            prefs.edit().putString(VoiceProfiles.PREF_ACTIVE, id).apply();
            pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(name));
            return true;
          });
      menuItems.add(item);
    }
    return menuItems;
  }

  /** Fills the menu opened by a gesture or keyboard shortcut. */
  public static void prepareVoiceProfileMenu(
      Context context, Pipeline.FeedbackReturner pipeline, ContextMenu menu) {
    for (ContextMenuItem item : getMenuItems(context, pipeline)) {
      menu.add(item);
    }
  }

  /**
   * Fills the submenu in the Backtalk menu.
   *
   * @return {@code false} if there are no profiles to choose between.
   */
  public static boolean prepareVoiceProfileSubMenu(
      Context context, Pipeline.FeedbackReturner pipeline, ListSubMenu subMenu) {
    List<ContextMenuItem> menuItems = getMenuItems(context, pipeline);
    for (ContextMenuItem item : menuItems) {
      subMenu.add(item);
    }
    return !menuItems.isEmpty();
  }
}
