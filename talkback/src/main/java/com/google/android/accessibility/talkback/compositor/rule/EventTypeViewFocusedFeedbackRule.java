/*
 * Copyright (C) 2023 Google Inc.
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
package com.google.android.accessibility.talkback.compositor.rule;

import static com.google.android.accessibility.talkback.compositor.Compositor.EVENT_TYPE_VIEW_FOCUSED;

import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.compositor.AccessibilityEventFeedbackUtils;
import com.google.android.accessibility.talkback.compositor.Compositor.HandleEventOptions;
import com.google.android.accessibility.talkback.compositor.EventFeedback;
import com.google.android.accessibility.talkback.compositor.GlobalVariables;
import com.google.android.accessibility.talkback.compositor.TalkBackFeedbackProvider;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.accessibility.utils.Role;
import com.google.android.accessibility.utils.StringBuilderUtils;
import com.google.android.accessibility.utils.WebInterfaceUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Event feedback rules for {@link EVENT_TYPE_VIEW_FOCUSED} event. These rules will provide the
 * event feedback output function by inputting the {@link HandleEventOptions} and outputting {@link
 * EventFeedback}.
 */
public final class EventTypeViewFocusedFeedbackRule {

  private static final String TAG = "EventTypeViewFocusedFeedbackRule";

  /**
   * Adds the feedback rules to the provided event feedback rules map. So {@link
   * TalkBackFeedbackProvider} can provide the event feedback by the rules.
   *
   * @param eventFeedbackRules the event feedback rules
   * @param globalVariables the global compositor variables
   */
  public static void addFeedbackRule(
      Map<Integer, Function<HandleEventOptions, EventFeedback>> eventFeedbackRules,
      GlobalVariables globalVariables) {
    eventFeedbackRules.put(
        EVENT_TYPE_VIEW_FOCUSED,
        (eventOptions) -> {
          CharSequence ttsOutput = getTtsOutput(eventOptions, globalVariables);
          boolean sourceNodeIsNull = (eventOptions.sourceNode == null);
          int earcon = sourceNodeIsNull ? R.raw.focus_actionable : -1;
          int haptic = sourceNodeIsNull ? R.array.view_focused_or_selected_pattern : -1;

          LogUtils.v(
              TAG,
              StringBuilderUtils.joinFields(
                  " ttsOutputRule= eventContentDescriptionOrEventAggregateText, ",
                  StringBuilderUtils.optionalTag("sourceNodeIsNull", sourceNodeIsNull)));

          return EventFeedback.builder()
              .setTtsOutput(Optional.of(ttsOutput))
              .setTtsAddToHistory(true)
              .setEarcon(earcon)
              .setHaptic(haptic)
              .build();
        });
  }

  private static CharSequence getTtsOutput(
      HandleEventOptions eventOptions, GlobalVariables globalVariables) {
    if (FormFactorUtils.isAndroidTv() || FormFactorUtils.isAndroidWear()) {
      // On TV, we will always sync accessibility-focus to input-focus, so it is sufficient to
      // speak on TYPE_VIEW_ACCESSIBILITY_FOCUSED.
      //
      // On wear, input-focus is on the scrollable view because it has the side button to scroll it.
      // We could skip the announcement.
      return "";
    }
    // Accessibility focus follows input focus onto a node it can focus, and that node is spoken
    // then. Speaking it here too started it twice, the second cutting off the first.
    if (willTakeAccessibilityFocus(eventOptions.sourceNode)) {
      return "";
    }
    return AccessibilityEventFeedbackUtils.getEventContentDescriptionOrEventAggregateText(
        eventOptions.eventObject, globalVariables.getUserPreferredLocale());
  }

  /**
   * Whether accessibility focus follows input focus onto {@code node} and is spoken, as {@link
   * com.google.android.accessibility.talkback.interpreters.InputFocusInterpreter} moves it: any
   * node that can take accessibility focus, except a list or grid. A node that already has it isn't
   * focused again, so nothing would be spoken, except in web content, where Chrome moves
   * accessibility focus along with input focus and sends its own event.
   */
  private static boolean willTakeAccessibilityFocus(@Nullable AccessibilityNodeInfoCompat node) {
    if (node == null) {
      return false;
    }
    int role = Role.getRole(node);
    if (role == Role.ROLE_LIST
        || role == Role.ROLE_GRID
        || !AccessibilityNodeInfoUtils.shouldFocusNode(node)) {
      return false;
    }
    return !node.isAccessibilityFocused() || WebInterfaceUtils.supportsWebActions(node);
  }

  private EventTypeViewFocusedFeedbackRule() {}
}
