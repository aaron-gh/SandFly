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

import android.view.Menu;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.contextmenu.ContextMenuItem.DeferredType;

/** Fills the menu of text editing commands. */
public final class EditingMenuProcessor {
  // The title of each item, and the gesture command it runs.
  private static final int[][] ITEMS = {
    {R.string.title_clipboard_history, R.string.shortcut_value_clipboard_history},
    {android.R.string.copy, R.string.shortcut_value_copy},
    {R.string.shortcut_add_to_clipboard, R.string.shortcut_value_add_to_clipboard},
    {android.R.string.cut, R.string.shortcut_value_cut},
    {android.R.string.paste, R.string.shortcut_value_paste},
    {R.string.shortcut_clear_text, R.string.shortcut_value_clear_text},
    {android.R.string.selectAll, R.string.shortcut_value_select_all},
    {
      R.string.title_edittext_breakout_start_selection_mode,
      R.string.shortcut_value_start_selection_mode
    },
  };

  private EditingMenuProcessor() {}

  public static void prepareEditingMenu(TalkBackService service, ContextMenu menu) {
    for (int[] entry : ITEMS) {
      ContextMenuItem item =
          ContextMenu.createMenuItem(
              service, Menu.NONE, Menu.NONE, Menu.NONE, service.getString(entry[0]));
      String command = service.getString(entry[1]);
      item.setOnMenuItemClickListener(
          clicked -> {
            return service.performCommand(command);
          });
      // Runs after the menu closes, when focus is back on the edit field.
      item.setDeferredType(DeferredType.WINDOWS_STABLE);
      item.setCheckable(false);
      menu.add(item);
    }
  }
}
