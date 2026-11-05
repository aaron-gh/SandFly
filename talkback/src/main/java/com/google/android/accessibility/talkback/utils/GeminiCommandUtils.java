/*
 * Copyright (C) 2025 Google Inc.
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

package com.google.android.accessibility.talkback.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.ActorState;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.actor.gemini.AiResultOutput;
import com.google.android.accessibility.talkback.actor.gemini.GeminiFunctionUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.output.FeedbackItem;
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions;

/**
 * Utility class for Gemini functions called by {@link GestureController} and {@link
 * KeyComboMapper}.
 */
public final class GeminiCommandUtils {
  private static final String TAG = "GeminiCommandUtils";

  private static final SpeakOptions SPEAK_OPTIONS =
      SpeakOptions.create()
          .setFlags(
              FeedbackItem.FLAG_NO_HISTORY
                  | FeedbackItem.FLAG_FORCE_FEEDBACK_ALL);

  public static Feedback.Part.Builder feedbackForDescribeImage(
      Context context, AccessibilityNodeInfoCompat node, ActorState actorState) {
    return feedbackForDescribeImage(context, node, actorState, /* speak= */ false);
  }

  /**
   * Returns the feedback that describes the image {@code node}. With {@code speak}, the description
   * is spoken instead of shown in a window.
   */
  public static Feedback.Part.Builder feedbackForDescribeImage(
      Context context, AccessibilityNodeInfoCompat node, ActorState actorState, boolean speak) {
    if (node == null) {
      return Feedback.speech(context.getString(R.string.image_caption_no_result), SPEAK_OPTIONS);
    }

    AiResultOutput.request(speak);
    return GeminiFunctionUtils.getPreferredImageDescriptionFeedback(context, actorState, node);
  }

  public static Feedback.Part.Builder feedbackForScreenOverview(
      Context context, AccessibilityNodeInfoCompat node) {
    return feedbackForScreenOverview(context, node, /* speak= */ false);
  }

  /**
   * Returns the feedback that describes the screen. With {@code speak}, the description is spoken
   * instead of shown in a window.
   */
  public static Feedback.Part.Builder feedbackForScreenOverview(
      Context context, AccessibilityNodeInfoCompat node, boolean speak) {
    if (node == null) {
      return Feedback.speech(context.getString(R.string.screen_overview_no_result), SPEAK_OPTIONS);
    }

    AiResultOutput.request(speak);
    return GeminiFunctionUtils.getPreferredScreenOverviewFeedback(context, node);
  }

  /**
   * Returns the feedback that takes a screenshot of the screen, or of the image {@code node} when
   * {@code wholeScreen} is false, and opens a window for a question about it. Before the user has
   * agreed to send screenshots to the AI service, it describes the screen or image instead, which
   * asks for that agreement.
   */
  public static Feedback.Part.Builder feedbackForAskAbout(
      Context context,
      AccessibilityNodeInfoCompat node,
      ActorState actorState,
      boolean wholeScreen) {
    if (node == null) {
      return Feedback.speech(
          context.getString(
              wholeScreen ? R.string.screen_overview_no_result : R.string.image_caption_no_result),
          SPEAK_OPTIONS);
    }
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    boolean agreed =
        wholeScreen
            ? SharedPreferencesUtils.getBooleanPref(
                prefs,
                context.getResources(),
                R.string.pref_opt_in_screen_overview_key,
                R.bool.pref_opt_in_screen_overview_default)
            : SharedPreferencesUtils.getBooleanPref(
                prefs,
                context.getResources(),
                R.string.pref_detailed_image_description_key,
                R.bool.pref_detailed_image_description_default);
    if (!agreed) {
      return wholeScreen
          ? feedbackForScreenOverview(context, node)
          : feedbackForDescribeImage(context, node, actorState);
    }
    return Feedback.askAboutScreenshot(node, wholeScreen);
  }

  private GeminiCommandUtils() {}
}
