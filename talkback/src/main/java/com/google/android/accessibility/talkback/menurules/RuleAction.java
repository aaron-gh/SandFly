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
  }

  @Override
  public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
    return customActionMenu.accept(context, node)
        || editingAndSelectingMenu.accept(context, node)
        || typoSuggestionMenu.accept(context, node)
        || linkMenu.accept(context, node)
        || quotedMessageMenu.accept(context, node)
        || telegramShareMenu.accept(context, node)
        || sheetsEditMenu.accept(context, node);
  }

  /** Returns whether {@code node} has at least one action without building every rule's items. */
  public boolean hasMenuItemsForNode(
      Context context, AccessibilityNodeInfoCompat node, boolean includeAncestors) {
    // Check editing actions first so edit boxes short-circuit before ancestor and subtree walks.
    if (editingAndSelectingMenu.accept(context, node)
        && !editingAndSelectingMenu.getMenuItemsForNode(context, node, includeAncestors).isEmpty()) {
      return true;
    }
    if (typoSuggestionMenu.accept(context, node)) {
      return true;
    }
    if (customActionMenu.accept(context, node)
        && !customActionMenu.getMenuItemsForNode(context, node, includeAncestors).isEmpty()) {
      return true;
    }
    if (linkMenu.accept(context, node)
        && !linkMenu.getMenuItemsForNode(context, node, includeAncestors).isEmpty()) {
      return true;
    }
    if (quotedMessageMenu.accept(context, node)) {
      return true;
    }
    if (telegramShareMenu.accept(context, node)) {
      return true;
    }
    return sheetsEditMenu.accept(context, node)
        && !sheetsEditMenu.getMenuItemsForNode(context, node, includeAncestors).isEmpty();
  }

  @Override
  public List<ContextMenuItem> getMenuItemsForNode(
      Context context, AccessibilityNodeInfoCompat node, boolean includeAncestors) {
    List<ContextMenuItem> actionItems = new ArrayList<>();

    if (typoSuggestionMenu.accept(context, node)) {
      actionItems.addAll(typoSuggestionMenu.getMenuItemsForNode(context, node, includeAncestors));
    }

    if (customActionMenu.accept(context, node)) {
      actionItems.addAll(customActionMenu.getMenuItemsForNode(context, node, includeAncestors));
    }

    if (editingAndSelectingMenu.accept(context, node)) {
      actionItems.addAll(
          editingAndSelectingMenu.getMenuItemsForNode(context, node, includeAncestors));
    }

    if (linkMenu.accept(context, node)) {
      actionItems.addAll(linkMenu.getMenuItemsForNode(context, node, includeAncestors));
    }

    if (quotedMessageMenu.accept(context, node)) {
      actionItems.addAll(quotedMessageMenu.getMenuItemsForNode(context, node, includeAncestors));
    }

    if (telegramShareMenu.accept(context, node)) {
      actionItems.addAll(telegramShareMenu.getMenuItemsForNode(context, node, includeAncestors));
    }

    if (sheetsEditMenu.accept(context, node)) {
      actionItems.addAll(sheetsEditMenu.getMenuItemsForNode(context, node, includeAncestors));
    }

    return actionItems;
  }

  @Override
  public CharSequence getUserFriendlyMenuName(Context context) {
    return context.getString(R.string.title_custom_action);
  }
}
