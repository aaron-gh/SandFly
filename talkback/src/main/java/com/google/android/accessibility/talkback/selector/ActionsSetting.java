/*
 * Copyright (C) 2022 Google Inc.
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

package com.google.android.accessibility.talkback.selector;

import static com.google.android.accessibility.talkback.selector.SelectorController.Setting.ACTIONS;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.contextmenu.ContextMenuItem;
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor;
import com.google.android.accessibility.talkback.menurules.CustomActionMenu;
import com.google.android.accessibility.talkback.menurules.NodeMenuRuleCreator;
import com.google.android.accessibility.talkback.menurules.NodeMenuRuleCreator.MenuRules;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.Role;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.WebInterfaceUtils;
import java.util.List;

/**
 * Contextual setting for node with custom actions.
 *
 * <p><b>Note:</b> Custom actions are reported and handled by custom widgets. For example, an
 * application may define a custom action for clearing the user history.
 */
public class ActionsSetting implements SelectorController.ContextualSetting {
  private final NodeMenuRuleCreator nodeMenuCreator;
  private final AccessibilityFocusMonitor accessibilityFocusMonitor;

  public ActionsSetting(
      NodeMenuRuleCreator nodeMenuCreator, AccessibilityFocusMonitor accessibilityFocusMonitor) {
    this.nodeMenuCreator = nodeMenuCreator;
    this.accessibilityFocusMonitor = accessibilityFocusMonitor;
  }

  @Override
  public SelectorController.Setting getSetting() {
    return ACTIONS;
  }

  @Override
  public boolean isNodeSupportSetting(Context context, AccessibilityNodeInfoCompat node) {
    return node != null && isSettingEnabled(context) && !actionsOf(context, node).isEmpty();
  }

  @Override
  public boolean shouldActivateSetting(Context context, AccessibilityNodeInfoCompat node) {
    // For better editing experience, keeps the setting of Reading Controls when navigating to an
    // edit text.
    if (node == null
        || Role.getRole(node) == Role.ROLE_EDIT_TEXT
        || node.isEditable()
        || accessibilityFocusMonitor.getEditingNodeFromFocusedKeyboard(node) != null
        || !isSettingEnabled(context)) {
      return false;
    }

    boolean selectableText = AccessibilityNodeInfoUtils.isNonEditableSelectableText(node);
    if (WebInterfaceUtils.isWebContainer(node)) {
      // Text on web pages keeps it, as in TalkBack. Other web items switch for their own actions
      // only: not for actions of the page or of the browser's views around it.
      return !selectableText
          && hasOtherThanEditingOptions(
              nodeMenuCreator.getNodeMenuByRule(
                  MenuRules.RULE_CUSTOM_ACTION, context, node, /* includeAncestors= */ false));
    }
    List<ContextMenuItem> actions = actionsOf(context, node);
    // Other selectable text keeps it where its only actions are editing options.
    if (selectableText) {
      return hasOtherThanEditingOptions(actions);
    }
    return !actions.isEmpty();
  }

  private static boolean hasOtherThanEditingOptions(List<ContextMenuItem> actions) {
    for (ContextMenuItem action : actions) {
      if (!isEditingOption(action)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isSettingEnabled(Context context) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    return prefs.getBoolean(
        context.getString(ACTIONS.prefKeyResId),
        context.getResources().getBoolean(ACTIONS.defaultValueResId));
  }

  private List<ContextMenuItem> actionsOf(Context context, AccessibilityNodeInfoCompat node) {
    return nodeMenuCreator.getNodeMenuByRule(MenuRules.RULE_CUSTOM_ACTION, context, node, true);
  }

  /**
   * Returns whether {@code action} edits or selects text, rather than being an app's or script's
   * action, a link or Go to quoted message.
   */
  private static boolean isEditingOption(ContextMenuItem action) {
    return action.getGroupId() != CustomActionMenu.CUSTOM_ACTION_GROUP_ID
        && action.getGroupId() != R.id.group_links
        && action.getItemId() != R.id.go_to_quoted_message;
  }
}
