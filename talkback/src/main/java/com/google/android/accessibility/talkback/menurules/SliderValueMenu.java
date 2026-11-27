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

import static android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS;

import android.content.Context;
import android.view.Menu;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.actor.SliderSteps;
import com.google.android.accessibility.talkback.contextmenu.ContextMenu;
import com.google.android.accessibility.talkback.contextmenu.ContextMenuItem;
import com.google.android.accessibility.talkback.dialog.SliderValueDialog;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.Role;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Enter value, on a slider that takes ACTION_SET_PROGRESS (GitHub #281): a window with a field for
 * a value in the slider's range, which the slider is then set to.
 */
public class SliderValueMenu implements NodeMenu {

  private final Pipeline.FeedbackReturner pipeline;

  public SliderValueMenu(Pipeline.FeedbackReturner pipeline) {
    this.pipeline = pipeline;
  }

  @Override
  public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
    if (Role.getRole(node) != Role.ROLE_SEEK_CONTROL
        || !AccessibilityNodeInfoUtils.supportsAction(node, ACTION_SET_PROGRESS.getId())) {
      return false;
    }
    AccessibilityNodeInfoCompat.@Nullable RangeInfoCompat range = node.getRangeInfo();
    return range != null
        && SliderSteps.isValidRange(
            range.getType(), range.getMin(), range.getMax(), range.getCurrent());
  }

  /** Whether {@code item} is Enter value. */
  static boolean isItem(ContextMenuItem item) {
    return item.getItemId() == R.id.slider_enter_value;
  }

  @Override
  public List<ContextMenuItem> getMenuItemsForNode(
      Context context, AccessibilityNodeInfoCompat node, boolean includeAncestors) {
    List<ContextMenuItem> items = new ArrayList<>();
    ContextMenuItem item =
        ContextMenu.createMenuItem(
            context,
            Menu.NONE,
            R.id.slider_enter_value,
            Menu.NONE,
            context.getString(R.string.title_slider_enter_value));
    item.setOnMenuItemClickListener(
        menuItem -> {
          // The window is an accessibility overlay only when it is made from the service.
          @Nullable TalkBackService service = TalkBackService.getInstance();
          Context dialogContext = (context instanceof TalkBackService || service == null) ? context : service;
          // The range may have changed since the menu was made.
          if (node.refresh() && accept(dialogContext, node)) {
            new SliderValueDialog(dialogContext, pipeline, node).show();
          }
          return true;
        });
    items.add(item);
    return items;
  }
}
