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

package com.google.android.accessibility.talkback.menurules;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Menu;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.contextmenu.ContextMenu;
import com.google.android.accessibility.talkback.contextmenu.ContextMenuItem;
import com.google.android.accessibility.talkback.focusmanagement.SheetsGrid;
import com.google.android.accessibility.utils.AccessibilityServiceCompatUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Edit cell and Insert formula, on a Google Sheets cell. Typing into a cell takes Sheets' "Enter
 * text or formula" field above the grid, so these actions select the cell, open that field (Insert
 * formula turns on formula mode first) and, when editing ends, focus the cell again.
 *
 * <p>Editing has ended when, after the field had input focus or the keyboard was shown, neither
 * is so any more. Sheets lists only the cells around the selected one; when it no longer lists the
 * edited cell (Enter moved the selection), the nearest cell it lists is focused.
 */
public class SheetsEditMenu implements NodeMenu {

  private static final String TAG = "SheetsEditMenu";

  static final String EDITOR_ID = SheetsGrid.PACKAGE + ":id/cell_content_editor";

  static final String FORMULA_MODE_ID = SheetsGrid.PACKAGE + ":id/toggle_formula_mode";

  /** How often, and how many times, the field is looked for after the cell is selected. */
  private static final long EDITOR_RETRY_MS = 150;

  private static final int EDITOR_ATTEMPTS = 14;

  /** How long Sheets takes to list the cells again after editing ends. */
  private static final long RETURN_DELAY_MS = 400;

  /** Editing still going on after this long is no longer watched. */
  private static final long MAX_EDIT_MS = 30 * 60_000;

  private static final Handler handler = new Handler(Looper.getMainLooper());

  /** The address of the cell to focus when editing ends, or null. Used on the main thread only. */
  private static @Nullable String returnAddress = null;

  /** Whether the field had input focus or the keyboard was shown since editing started. */
  private static boolean editing = false;

  private static long watchUntil = 0;

  @Override
  public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
    return SheetsGrid.isCell(node);
  }

  @Override
  public List<ContextMenuItem> getMenuItemsForNode(
      Context context, AccessibilityNodeInfoCompat node, boolean includeAncestors) {
    List<ContextMenuItem> items = new ArrayList<>();
    SheetsGrid.@Nullable Cell cell = SheetsGrid.cellOf(node);
    if (cell == null) {
      return items;
    }
    items.add(
        item(
            context,
            node,
            cell.address,
            R.id.sheets_edit_cell,
            R.string.title_sheets_edit_cell,
            false));
    items.add(
        item(
            context,
            node,
            cell.address,
            R.id.sheets_insert_formula,
            R.string.title_sheets_insert_formula,
            true));
    return items;
  }

  private static ContextMenuItem item(
      Context context,
      AccessibilityNodeInfoCompat node,
      String address,
      int id,
      int title,
      boolean formula) {
    ContextMenuItem item =
        ContextMenu.createMenuItem(context, Menu.NONE, id, Menu.NONE, context.getString(title));
    item.setOnMenuItemClickListener(
        menuItem -> {
          start(node, address, formula);
          return true;
        });
    return item;
  }

  private static void start(AccessibilityNodeInfoCompat cell, String address, boolean formula) {
    stopWatching();
    boolean selected = cell.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
    LogUtils.d(TAG, "Selected %s: %s", address, selected);
    handler.postDelayed(() -> openEditor(address, formula, EDITOR_ATTEMPTS), EDITOR_RETRY_MS);
  }

  private static void openEditor(String address, boolean formula, int attempts) {
    @Nullable AccessibilityNodeInfo root = sheetsRoot();
    @Nullable AccessibilityNodeInfo editor = root == null ? null : visibleById(root, EDITOR_ID);
    if (editor == null) {
      if (attempts > 1) {
        handler.postDelayed(() -> openEditor(address, formula, attempts - 1), EDITOR_RETRY_MS);
      } else {
        LogUtils.w(TAG, "The Enter text or formula field isn't on the screen");
      }
      return;
    }
    if (formula) {
      @Nullable AccessibilityNodeInfo toggle = visibleById(root, FORMULA_MODE_ID);
      if (toggle != null && !toggle.isChecked()) {
        toggle.performAction(AccessibilityNodeInfo.ACTION_CLICK);
      }
    }
    editor.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
    editor.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    editor.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
    returnAddress = address;
    editing = false;
    watchUntil = SystemClock.uptimeMillis() + MAX_EDIT_MS;
    LogUtils.d(TAG, "Editing %s", address);
  }

  /**
   * Watches for the end of editing started by Edit cell or Insert formula, and then focuses the
   * edited cell. Called for every event; does nothing unless such editing is going on.
   */
  public static void onEvent(AccessibilityEvent event) {
    if (returnAddress == null) {
      return;
    }
    int type = event.getEventType();
    if (type != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        && type != AccessibilityEvent.TYPE_VIEW_FOCUSED
        && type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
      return;
    }
    @Nullable AccessibilityService service = TalkBackService.getInstance();
    @Nullable AccessibilityNodeInfo root = service == null ? null : service.getRootInActiveWindow();
    if (service == null
        || SystemClock.uptimeMillis() > watchUntil
        || (root != null && !TextUtils.equals(root.getPackageName(), SheetsGrid.PACKAGE))) {
      // Another app is in front, or editing has gone on too long to be the one started here.
      stopWatching();
      return;
    }
    @Nullable AccessibilityNodeInfo editor = root == null ? null : visibleById(root, EDITOR_ID);
    boolean inEditor = editor != null && editor.isFocused();
    if (inEditor || AccessibilityServiceCompatUtils.isInputWindowOnScreen(service)) {
      editing = true;
      return;
    }
    if (!editing) {
      return;
    }
    String address = returnAddress;
    stopWatching();
    handler.postDelayed(() -> focusCell(address), RETURN_DELAY_MS);
  }

  private static void focusCell(String address) {
    @Nullable AccessibilityNodeInfoCompat cell = SheetsGrid.listedCellOrNearest(address);
    LogUtils.d(
        TAG, "Editing ended; focusing %s", cell == null ? "nothing" : cell.getContentDescription());
    if (cell != null) {
      cell.performAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS);
    }
  }

  private static void stopWatching() {
    returnAddress = null;
    editing = false;
  }

  private static @Nullable AccessibilityNodeInfo sheetsRoot() {
    @Nullable AccessibilityService service = TalkBackService.getInstance();
    @Nullable AccessibilityNodeInfo root = service == null ? null : service.getRootInActiveWindow();
    return root != null && TextUtils.equals(root.getPackageName(), SheetsGrid.PACKAGE) ? root : null;
  }

  private static @Nullable AccessibilityNodeInfo visibleById(AccessibilityNodeInfo root, String id) {
    List<AccessibilityNodeInfo> found = root.findAccessibilityNodeInfosByViewId(id);
    if (found == null) {
      return null;
    }
    for (AccessibilityNodeInfo node : found) {
      if (node != null && node.isVisibleToUser()) {
        return node;
      }
    }
    return null;
  }
}
