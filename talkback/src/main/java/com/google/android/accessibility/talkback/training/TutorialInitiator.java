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

package com.google.android.accessibility.talkback.training;

import static com.google.android.accessibility.talkback.trainingcommon.TrainingConfig.TrainingId.TRAINING_ID_TUTORIAL_FOR_TV;
import static com.google.android.accessibility.talkback.trainingcommon.TrainingConfig.TrainingId.TRAINING_ID_TUTORIAL_FOR_WATCH;
import static com.google.android.accessibility.talkback.trainingcommon.TrainingConfig.TrainingId.TRAINING_ID_TUTORIAL_PRACTICE_GESTURE;
import static com.google.android.accessibility.talkback.trainingcommon.TrainingConfig.TrainingId.TRAINING_ID_TUTORIAL_PRACTICE_GESTURE_PRE_R;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.trainingcommon.TrainingActivity;
import com.google.android.accessibility.talkback.trainingcommon.TutorialChooserActivity;
import com.google.android.accessibility.talkback.tutorial.TutorialActivity;
import com.google.android.accessibility.talkback.tutorial.TutorialLessons;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.accessibility.utils.monitor.InputDeviceMonitor;

/** Starts a {@link TrainingActivity} to show tutorial. */
public class TutorialInitiator {

  public static Intent createFirstRunTutorialIntent(Context context) {
    return createFirstRunTutorialIntent(context, new InputDeviceMonitor(context));
  }

  /** Returns an intent to start tutorial for the first run users. */
  @VisibleForTesting
  public static Intent createFirstRunTutorialIntent(
      Context context, InputDeviceMonitor inputDeviceMonitor) {
    if (FormFactorUtils.isAndroidWear()) {
      return TrainingActivity.createTrainingIntent(
          context, TRAINING_ID_TUTORIAL_FOR_WATCH, /* showExitBanner= */ true);
    } else if (FormFactorUtils.isAndroidTv()) {
      return TrainingActivity.createTrainingIntent(context, TRAINING_ID_TUTORIAL_FOR_TV);
    } else if (FeatureFlagReader.enableShowTalkbackKeyboardTutorial(context)
        && inputDeviceMonitor.hasPhysicalKeyboard()) {
      return TutorialActivity.keyboardIntent(context);
    } else {
      return TutorialActivity.intent(context);
    }
  }

  /** Returns an intent to start tutorial. */
  public static Intent createTutorialIntent(Context context) {
    return createTutorialIntent(context, new InputDeviceMonitor(context));
  }

  @VisibleForTesting
  static Intent createTutorialIntent(Context context, InputDeviceMonitor inputDeviceMonitor) {
    if (FormFactorUtils.isAndroidWear()) {
      return TrainingActivity.createTrainingIntent(context, TRAINING_ID_TUTORIAL_FOR_WATCH);
    } else if (FormFactorUtils.isAndroidTv()) {
      return TrainingActivity.createTrainingIntent(context, TRAINING_ID_TUTORIAL_FOR_TV);
    } else if (FeatureFlagReader.enableShowTalkbackKeyboardTutorial(context)
        && inputDeviceMonitor.hasPhysicalKeyboard()
        && inputDeviceMonitor.hasTouchScreen()) {
      return new Intent(context, TutorialChooserActivity.class);
    } else if (FeatureFlagReader.enableShowTalkbackKeyboardTutorial(context)
        && inputDeviceMonitor.hasPhysicalKeyboard()) {
      return TutorialActivity.keyboardIntent(context);
    } else {
      return TutorialActivity.intent(context);
    }
  }

  public static Intent createPracticeGesturesIntent(Context context) {
    if (!FormFactorUtils.isAndroidWear()) {
      return TutorialActivity.intent(context, TutorialLessons.PRACTICE_LESSON);
    }
    return TrainingActivity.createTrainingIntent(
        context,
        FeatureSupport.isMultiFingerGestureSupported()
            ? TRAINING_ID_TUTORIAL_PRACTICE_GESTURE
            : TRAINING_ID_TUTORIAL_PRACTICE_GESTURE_PRE_R);
  }

  public static Intent createKeyboardTutorialIntent(Context context) {
    return TutorialActivity.keyboardIntent(context);
  }

  public static Intent createPracticeKeyboardShortcutsIntent(Context context) {
    return TutorialActivity.keyboardIntent(context, TutorialLessons.PRACTICE_LESSON);
  }

}
