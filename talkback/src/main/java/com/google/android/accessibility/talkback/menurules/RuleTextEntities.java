/*
 * Copyright 2026 Backtalk contributors
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
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.contextmenu.ContextMenuItem;
import java.util.List;

/**
 * Rule for the Addresses and numbers item of the SandFly menu: the web and email addresses, phone
 * numbers and codes written in the focused item's text.
 */
public class RuleTextEntities extends NodeMenuRule {
  private final TextEntityMenu textEntityMenu;

  public RuleTextEntities(Pipeline.FeedbackReturner pipeline) {
    super(
        R.string.pref_show_context_menu_text_entities_setting_key,
        R.bool.pref_show_context_menu_text_entities_default);
    textEntityMenu = new TextEntityMenu(pipeline);
  }

  @Override
  public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
    return textEntityMenu.accept(context, node);
  }

  @Override
  public List<ContextMenuItem> getMenuItemsForNode(
      Context context, AccessibilityNodeInfoCompat node, boolean includeAncestors) {
    return textEntityMenu.getMenuItemsForNode(context, node, includeAncestors);
  }

  @Override
  public CharSequence getUserFriendlyMenuName(Context context) {
    return context.getString(R.string.title_text_entities);
  }
}
