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

import android.accessibilityservice.AccessibilityService;
import android.view.Menu;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.contextmenu.ContextMenuItem.DeferredType;
import com.google.android.accessibility.talkback.overview.SplitScreen;
import java.util.ArrayList;
import java.util.List;

/** Fills the menu that moves to the split screen divider and runs the divider's actions. */
public final class SplitScreenMenuProcessor {

  private SplitScreenMenuProcessor() {}

  /** Returns no items unless the split screen divider is on screen. */
  private static List<ContextMenuItem> getMenuItems(
      AccessibilityService service, Pipeline.FeedbackReturner pipeline) {
    List<ContextMenuItem> items = new ArrayList<>();
    AccessibilityNodeInfoCompat handle = SplitScreen.dividerHandle(service);
    if (handle == null) {
      return items;
    }

    ContextMenuItem goToDivider =
        ContextMenu.createMenuItem(
            service,
            Menu.NONE,
            Menu.NONE,
            Menu.NONE,
            service.getString(R.string.split_screen_go_to_divider));
    goToDivider.setOnMenuItemClickListener(
        clicked -> {
          AccessibilityNodeInfoCompat current = SplitScreen.dividerHandle(service);
          return current != null
              && pipeline.returnFeedback(
                  EVENT_ID_UNTRACKED,
                  Feedback.nodeAction(
                      current, AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS));
        });
    configure(goToDivider);
    items.add(goToDivider);

    for (AccessibilityActionCompat action : SplitScreen.dividerActions(handle)) {
      int actionId = action.getId();
      ContextMenuItem item =
          ContextMenu.createMenuItem(
              service, Menu.NONE, Menu.NONE, Menu.NONE, action.getLabel());
      item.setOnMenuItemClickListener(
          clicked -> {
            AccessibilityNodeInfoCompat current = SplitScreen.dividerHandle(service);
            return current != null
                && pipeline.returnFeedback(
                    EVENT_ID_UNTRACKED, Feedback.nodeAction(current, actionId));
          });
      configure(item);
      items.add(item);
    }
    return items;
  }

  /** Runs the item after the menu closes, and leaves focus where the item puts it. */
  private static void configure(ContextMenuItem item) {
    item.setDeferredType(DeferredType.WINDOWS_STABLE);
    item.setSkipRefocusEvents(true);
    item.setCheckable(false);
  }

  /** Fills the menu opened by a gesture or keyboard shortcut. */
  public static void prepareSplitScreenMenu(
      AccessibilityService service, Pipeline.FeedbackReturner pipeline, ContextMenu menu) {
    for (ContextMenuItem item : getMenuItems(service, pipeline)) {
      menu.add(item);
    }
  }

  /**
   * Fills the submenu in the SandFly menu.
   *
   * @return {@code false} if the split screen divider isn't on screen.
   */
  public static boolean prepareSplitScreenSubMenu(
      AccessibilityService service, Pipeline.FeedbackReturner pipeline, ListSubMenu subMenu) {
    List<ContextMenuItem> items = getMenuItems(service, pipeline);
    for (ContextMenuItem item : items) {
      subMenu.add(item);
    }
    return !items.isEmpty();
  }
}
