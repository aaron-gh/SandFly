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
import android.view.Menu;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.contextmenu.ContextMenu;
import com.google.android.accessibility.talkback.contextmenu.ContextMenuItem;
import com.google.android.accessibility.utils.TelegramReplyQuote;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Go to quoted message, on a Telegram message that replies to another. The reply quote is read with
 * the message and isn't focused on its own, so this action clicks it, which shows the quoted
 * message.
 */
public class QuotedMessageMenu implements NodeMenu {

  @Override
  public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
    return TelegramReplyQuote.isTelegram(node.getPackageName())
        && TelegramReplyQuote.quoteNodeOf(node) != null;
  }

  @Override
  public List<ContextMenuItem> getMenuItemsForNode(
      Context context, AccessibilityNodeInfoCompat node, boolean includeAncestors) {
    List<ContextMenuItem> items = new ArrayList<>();
    @Nullable AccessibilityNodeInfoCompat quote = TelegramReplyQuote.quoteNodeOf(node);
    if (quote == null) {
      return items;
    }
    ContextMenuItem item =
        ContextMenu.createMenuItem(
            context,
            Menu.NONE,
            R.id.go_to_quoted_message,
            Menu.NONE,
            context.getString(R.string.title_go_to_quoted_message));
    item.setOnMenuItemClickListener(
        menuItem -> {
          // The message list may have been refreshed since the menu opened.
          @Nullable AccessibilityNodeInfoCompat current =
              node.refresh() ? TelegramReplyQuote.quoteNodeOf(node) : null;
          AccessibilityNodeInfoCompat target = current != null ? current : quote;
          return target.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        });
    items.add(item);
    return items;
  }
}
