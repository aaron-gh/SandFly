/*
 * Copyright (C) 2014 Google Inc.
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
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.ActorState;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics;
import com.google.android.accessibility.talkback.contextmenu.ContextMenuItem;
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Rule for generating the action menu for a {@link AccessibilityNodeInfoCompat}.
 *
 * <p>The action menu contains custom actions, edit options, spannable links and spelling
 * suggestions.
 */
public class RuleAction extends NodeMenuRule {
  private final CustomActionMenu customActionMenu;
  private final EditingAndSelectingMenu editingAndSelectingMenu;
  private final TypoSuggestionMenu typoSuggestionMenu;
  private final LinkMenu linkMenu;
  private final QuotedMessageMenu quotedMessageMenu;
  private final TelegramShareMenu telegramShareMenu;
  private final SheetsEditMenu sheetsEditMenu = new SheetsEditMenu();
  private final SliderValueMenu sliderValueMenu;
  // The menus in the order that getMenuItemsForNode lists their items.
  private final NodeMenu[] menusInOrder;

  public RuleAction(
      Pipeline.FeedbackReturner pipeline,
      ActorState actorState,
      AccessibilityFocusMonitor accessibilityFocusMonitor,
      TalkBackAnalytics analytics) {
    super(
        R.string.pref_show_context_menu_custom_action_setting_key,
        R.bool.pref_show_context_menu_custom_action_default);
    customActionMenu = new CustomActionMenu(pipeline, analytics);
    editingAndSelectingMenu =
        new EditingAndSelectingMenu(pipeline, actorState, accessibilityFocusMonitor, analytics);
    typoSuggestionMenu = new TypoSuggestionMenu(pipeline, actorState, accessibilityFocusMonitor);
    linkMenu = new LinkMenu(analytics);
    quotedMessageMenu = new QuotedMessageMenu(pipeline);
    telegramShareMenu = new TelegramShareMenu(pipeline);
    sliderValueMenu = new SliderValueMenu(pipeline);
    menusInOrder =
        new NodeMenu[] {
          typoSuggestionMenu,
          customActionMenu,
          editingAndSelectingMenu,
          linkMenu,
          quotedMessageMenu,
          telegramShareMenu,
          sheetsEditMenu,
          sliderValueMenu
        };
  }

  @Override
  public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
    return customActionMenu.accept(context, node)
        || editingAndSelectingMenu.accept(context, node)
        || typoSuggestionMenu.accept(context, node)
        || linkMenu.accept(context, node)
        || quotedMessageMenu.accept(context, node)
        || telegramShareMenu.accept(context, node)
        || sheetsEditMenu.accept(context, node)
        || sliderValueMenu.accept(context, node);
  }

  /**
   * Enter value, which every slider that can be set has, isn't announced as an action: the hint is
   * given only for the other actions.
   */
  @Override
  boolean hasHint(Context context, AccessibilityNodeInfoCompat node) {
    if (!sliderValueMenu.accept(context, node)) {
      return true;
    }
    for (ContextMenuItem item :
        getMenuItemsForNode(context, node, /* includeAncestors= */ false)) {
      if (!SliderValueMenu.isItem(item)) {
        return true;
      }
    }
    return false;
  }

  /** Whether {@code item} is Enter value of a slider. */
  public static boolean isSliderValueItem(ContextMenuItem item) {
    return SliderValueMenu.isItem(item);
  }

  /**
   * Returns whether {@link #getMenuItemsForNode} would list an item matching {@code wanted}. The
   * menus are read in the same order, and the menus after the first match aren't read.
   */
  public boolean hasMenuItem(
      Context context,
      AccessibilityNodeInfoCompat node,
      boolean includeAncestors,
      Predicate<ContextMenuItem> wanted) {
    for (NodeMenu menu : menusInOrder) {
      if (menu.accept(context, node)) {
        for (ContextMenuItem item : menu.getMenuItemsForNode(context, node, includeAncestors)) {
          if (wanted.test(item)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  @Override
  public List<ContextMenuItem> getMenuItemsForNode(
      Context context, AccessibilityNodeInfoCompat node, boolean includeAncestors) {
    List<ContextMenuItem> actionItems = new ArrayList<>();
    for (NodeMenu menu : menusInOrder) {
      if (menu.accept(context, node)) {
        actionItems.addAll(menu.getMenuItemsForNode(context, node, includeAncestors));
      }
    }
    return actionItems;
  }

  @Override
  public CharSequence getUserFriendlyMenuName(Context context) {
    return context.getString(R.string.title_custom_action);
  }
}
