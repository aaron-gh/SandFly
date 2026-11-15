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

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.contextmenu.ContextMenu;
import com.google.android.accessibility.talkback.contextmenu.ContextMenuItem;
import com.google.android.accessibility.utils.TelegramShareButton;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The share buttons of a Telegram message, which aren't focused on their own, as actions of the
 * message, each under the label Telegram gives it. When Telegram refuses the click, the button is
 * tapped as a finger would.
 */
public class TelegramShareMenu implements NodeMenu {

  private static final String TAG = "TelegramShareMenu";

  private final PassThroughTap passThroughTap;

  public TelegramShareMenu(Pipeline.FeedbackReturner pipeline) {
    passThroughTap = new PassThroughTap(pipeline, new Handler(Looper.getMainLooper()));
  }

  @Override
  public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
    return !TelegramShareButton.shareButtonsOf(node).isEmpty();
  }

  @Override
  public List<ContextMenuItem> getMenuItemsForNode(
      Context context, AccessibilityNodeInfoCompat node, boolean includeAncestors) {
    List<ContextMenuItem> items = new ArrayList<>();
    List<AccessibilityNodeInfoCompat> buttons = TelegramShareButton.shareButtonsOf(node);
    for (int i = 0; i < buttons.size(); i++) {
      AccessibilityNodeInfoCompat button = buttons.get(i);
      String label = button.getContentDescription().toString().trim();
      int index = i;
      ContextMenuItem item =
          ContextMenu.createMenuItem(
              context, Menu.NONE, R.id.telegram_share_button, Menu.NONE, label);
      item.setOnMenuItemClickListener(
          menuItem -> {
            press(current(node, index, label, button));
            return true;
          });
      items.add(item);
    }
    return items;
  }

  /**
   * The button now at [index] among [message]'s share buttons, if it still has [label], since the
   * message list may have been refreshed since the menu opened; otherwise [button].
   */
  private static AccessibilityNodeInfoCompat current(
      AccessibilityNodeInfoCompat message,
      int index,
      String label,
      AccessibilityNodeInfoCompat button) {
    if (!message.refresh()) {
      return button;
    }
    List<AccessibilityNodeInfoCompat> buttons = TelegramShareButton.shareButtonsOf(message);
    if (index < buttons.size()) {
      @Nullable CharSequence description = buttons.get(index).getContentDescription();
      if (description != null && label.equals(description.toString().trim())) {
        return buttons.get(index);
      }
    }
    return button;
  }

  private void press(AccessibilityNodeInfoCompat button) {
    if (button.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK)) {
      return;
    }
    Rect bounds = new Rect();
    button.getBoundsInScreen(bounds);
    LogUtils.d(TAG, "Telegram refused the click on the share button; tapping it at %s", bounds);
    if (!bounds.isEmpty()) {
      passThroughTap.tap(bounds.centerX(), bounds.centerY());
    }
  }
}
