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
import android.content.SharedPreferences;
import android.view.Menu;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.gesture.GestureProfiles;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.ArrayList;
import java.util.List;
import kotlin.Pair;

/** Fills the menu that chooses the gesture profile in use. */
public final class GestureProfileMenuProcessor {

  private GestureProfileMenuProcessor() {}

  /**
   * Returns an item for Default and each gesture profile, in the order of the Gesture profiles
   * screen, or none when there are no profiles to choose between.
   */
  private static List<ContextMenuItem> getMenuItems(
      Context context, Pipeline.FeedbackReturner pipeline) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    List<ContextMenuItem> menuItems = new ArrayList<>();
    List<Pair<String, String>> choices = GestureProfiles.choices(context, prefs);
    if (choices.size() < 2) {
      return menuItems;
    }
    String activeId = GestureProfiles.activeId(prefs);
    for (Pair<String, String> choice : choices) {
      String id = choice.getFirst();
      String name = choice.getSecond();
      ContextMenuItem item =
          ContextMenu.createMenuItem(
              context,
              R.id.group_gesture_profile,
              Menu.NONE,
              Menu.NONE,
              id.equals(activeId)
                  ? context.getString(R.string.gesture_profile_menu_item_in_use, name)
                  : name);
      item.setOnMenuItemClickListener(
          clicked -> {
            // Null when the profile was deleted while the menu was open.
            String selected = GestureProfiles.select(context, prefs, id);
            if (selected != null) {
              pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(selected));
            }
            return true;
          });
      menuItems.add(item);
    }
    return menuItems;
  }

  /** Fills the menu opened by a gesture or keyboard shortcut. */
  public static void prepareGestureProfileMenu(
      Context context, Pipeline.FeedbackReturner pipeline, ContextMenu menu) {
    for (ContextMenuItem item : getMenuItems(context, pipeline)) {
      menu.add(item);
    }
  }

  /**
   * Fills the submenu in the SandFly menu.
   *
   * @return {@code false} if there are no profiles to choose between.
   */
  public static boolean prepareGestureProfileSubMenu(
      Context context, Pipeline.FeedbackReturner pipeline, ListSubMenu subMenu) {
    List<ContextMenuItem> menuItems = getMenuItems(context, pipeline);
    for (ContextMenuItem item : menuItems) {
      subMenu.add(item);
    }
    return !menuItems.isEmpty();
  }
}
