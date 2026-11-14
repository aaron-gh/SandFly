/*
 * Copyright (C) 2020 Google Inc.
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

package com.google.android.accessibility.talkback;

import static com.google.android.accessibility.utils.WebInterfaceUtils.DIRECTION_FORWARD;
import static com.google.android.accessibility.utils.monitor.InputModeTracker.INPUT_MODE_KEYBOARD;

import android.accessibilityservice.AccessibilityService;
import android.os.Bundle;
import android.os.SystemClock;
import com.google.android.accessibility.utils.input.ScrollEventInterpreter;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.Feedback.WebAction;
import com.google.android.accessibility.talkback.focusmanagement.NavigationTarget;
import com.google.android.accessibility.talkback.focusmanagement.action.NavigationAction;
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo;
import com.google.android.accessibility.utils.PerformActionUtils;
import com.google.android.accessibility.utils.Performance;
import com.google.android.accessibility.utils.WebInterfaceUtils;
import com.google.android.accessibility.utils.output.FeedbackItem;
import com.google.android.accessibility.utils.output.SpeechController;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import org.checkerframework.checker.nullness.qual.NonNull;

/** WebActor executes WebAction-feedback. */
public class WebActor {

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // UpdateFocusHistory interface

  /** Interface for a update history method, used to update focus history. */
  public interface UpdateFocusHistory {
    void updateHistory(AccessibilityNodeInfoCompat start, FocusActionInfo focusActionInfo, long actionTime);

    default void beforeNavigation(AccessibilityNodeInfoCompat start) {}
  }

  private final UpdateFocusHistory focusHistory;

  //////////////////////////////////////////////////////////////////////////////////////////////////

  private static final String LOG_TAG = "WebActor";

  private Pipeline.FeedbackReturner pipeline;
  private final AccessibilityService service;

  public WebActor(AccessibilityService service, UpdateFocusHistory focusHistory) {
    this.service = service;
    this.focusHistory = focusHistory;
  }

  public void setPipeline(Pipeline.FeedbackReturner pipeline) {
    this.pipeline = pipeline;
  }

  ///////////////////////////////////////////////////////////////////////////////////////
  // Methods

  /**
   * Perform web navigation action and update focus history with source action, {@code
   * FocusActionInfo.LOGICAL_NAVIGATION}
   */
  public boolean performAction(@NonNull WebAction webAction, Performance.EventId eventId) {
    if (webAction.updateFocusHistory() && focusHistory != null && webAction.target() != null) {
      focusHistory.beforeNavigation(webAction.target());
    }
    long actionTime = SystemClock.uptimeMillis();
    boolean result = performActionWithScrollAttribution(webAction.target(), webAction.nodeAction(),
        webAction.nodeActionArgs(), eventId, actionTime);
    if (result && webAction.updateFocusHistory() && (focusHistory != null)) {
      // The source action of FocusActionInfo only has FocusActionInfo.LOGICAL_NAVIGATION since web
      // navigation only has navigation action.
      focusHistory.updateHistory(
          webAction.target(),
          FocusActionInfo.builder()
              .setSourceAction(FocusActionInfo.LOGICAL_NAVIGATION)
              .setNavigationAction(webAction.navigationAction())
              .build(), actionTime);
    }
    return result;
  }

  public boolean navigateToHtmlElement(
      AccessibilityNodeInfoCompat start,
      NavigationAction navigationAction,
      Performance.EventId eventId) {

    String htmlElementType = NavigationTarget.targetTypeToHtmlElement(navigationAction.targetType);
    if (htmlElementType == null) {
      LogUtils.w(
          LOG_TAG,
          "Cannot navigate to HTML target: invalid targetType=%d.",
          navigationAction.targetType);
      return false;
    }

    Bundle args = new Bundle();
    args.putString(
        AccessibilityNodeInfoCompat.ACTION_ARGUMENT_HTML_ELEMENT_STRING, htmlElementType);

    if (!WebInterfaceUtils.supportsWebActions(start)) {
      LogUtils.w(LOG_TAG, "Cannot navigate to HTML target: current pivot is not a web element.");
      if (navigationAction.inputMode == INPUT_MODE_KEYBOARD) {
        speakTTSText(service.getString(R.string.keycombo_announce_shortcut_not_supported), eventId);
      }
      return false;
    }

    int webNavigationDirection =
        WebInterfaceUtils.searchDirectionToWebNavigationDirection(
            /* context= */ service, navigationAction.searchDirection);
    if (webNavigationDirection == 0) {
      LogUtils.w(LOG_TAG, "Cannot navigate to HTML target: invalid direction.");
      return false;
    }
    int action =
        (webNavigationDirection == DIRECTION_FORWARD)
            ? AccessibilityNodeInfoCompat.ACTION_NEXT_HTML_ELEMENT
            : AccessibilityNodeInfoCompat.ACTION_PREVIOUS_HTML_ELEMENT;

    if (focusHistory != null) {
      focusHistory.beforeNavigation(start);
    }
    long actionTime = SystemClock.uptimeMillis();
    if (performActionWithScrollAttribution(start, action, args, eventId, actionTime)) {
      if (focusHistory != null) {
        focusHistory.updateHistory(
            start,
            FocusActionInfo.builder()
                .setSourceAction(FocusActionInfo.LOGICAL_NAVIGATION)
                .setNavigationAction(navigationAction)
                .build(), actionTime);
      }
      return true;
    } else {
      int resId =
          webNavigationDirection == WebInterfaceUtils.DIRECTION_FORWARD
              ? R.string.end_of_page
              : R.string.start_of_page;
      String ttsText =
          service.getString(
              resId,
              NavigationTarget.htmlTargetToDisplayName(
                  /* context= */ service, navigationAction.targetType));

      speakTTSText(ttsText, eventId);
      return false;
    }
  }

  private static boolean performActionWithScrollAttribution(AccessibilityNodeInfoCompat target,
      int action, Bundle args, Performance.EventId eventId, long actionTime) {
    boolean html = target != null && (action == AccessibilityNodeInfoCompat.ACTION_NEXT_HTML_ELEMENT
        || action == AccessibilityNodeInfoCompat.ACTION_PREVIOUS_HTML_ELEMENT);
    if (html) ScrollEventInterpreter.startWebNavigation(actionTime);
    boolean succeeded = false;
    try {
      succeeded = PerformActionUtils.performAction(target, action, args, eventId);
      return succeeded;
    } finally {
      if (html) ScrollEventInterpreter.finishWebNavigationAction(target.getWindowId(), actionTime, succeeded);
    }
  }

  private void speakTTSText(CharSequence ttsText, Performance.EventId eventId) {
    SpeechController.SpeakOptions speakOptions =
        SpeechController.SpeakOptions.create()
            .setQueueMode(SpeechController.QUEUE_MODE_INTERRUPT)
            .setFlags(FeedbackItem.FLAG_FORCE_FEEDBACK);
    pipeline.returnFeedback(eventId, Feedback.speech(ttsText, speakOptions));
  }
}
