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

package com.google.android.accessibility.talkback;

import android.app.Application;
import android.os.Build;
import com.google.android.accessibility.braille.interfaces.ScreenReaderCommands;
import com.google.android.accessibility.talkback.apinames.DeprecatedApiNotification;
import com.google.android.accessibility.talkback.braille.BrailleDisplayCommands;
import com.google.android.accessibility.talkback.report.CrashLog;
import com.google.android.accessibility.talkback.report.ProblemReportSender;
import com.google.android.accessibility.talkback.migration.SettingsImporter;
import com.google.android.accessibility.talkback.network.NetworkForeground;
import com.google.android.accessibility.talkback.training.PageConfigMapperImpl;
import com.google.android.accessibility.talkback.training.TalkBackDisabledWarmingDialogImpl;
import com.google.android.accessibility.talkback.training.TrainingConfigMapperImpl;
import com.google.android.accessibility.talkback.training.TrainingSectionLoggerImpl;
import com.google.android.accessibility.talkback.training.content.TutorialContentSupplierImpl;
import com.google.android.accessibility.talkback.trainingcommon.TrainingActivityInterfaceInjector;
import com.google.android.accessibility.talkback.trainingcommon.content.TutorialContentInterfaceInjector;
import com.google.android.accessibility.talkback.userguide.SettingsGuideSections;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.accessibility.utils.mlkit.MlKitHelper;
import com.google.android.accessibility.utils.preference.SettingsHelp;

/** A top level Application for TalkBack. */
public class TalkBackApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();
    CrashLog.install(this);

    // Before anything loads the preferences, so settings from the old app ID are not overwritten.
    // Only Backtalk's main process imports them, not the notification reply's.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P
        || getPackageName().equals(Application.getProcessName())) {
      SettingsImporter.importIfNeeded(this);
    }
    // Its service runs in the main process, which makes the requests that need it.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        && getPackageName().equals(Application.getProcessName())) {
      NetworkForeground.initialize(this);
    }
    DeprecatedApiNotification.install(this);
    MlKitHelper.initialize(this);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        && Application.getProcessName().endsWith(":mlkit")) {
      // So that a problem report includes the log of ML Kit's process.
      ProblemReportSender.rememberProcess(this, "mlkit");
    }

    FormFactorUtils.initialize(this);
    PackageNameProvider.initialize(this);
    TrainingActivityInterfaceInjector.initialize(
        new TrainingConfigMapperImpl(),
        new PageConfigMapperImpl(),
        TrainingSectionLoggerImpl::new,
        TalkBackDisabledWarmingDialogImpl::new);
    TutorialContentInterfaceInjector.initialize(new TutorialContentSupplierImpl());
    ScreenReaderCommands.Provider.set(BrailleDisplayCommands.INSTANCE);
    // The Help action of settings screens opens their section of the user guide.
    SettingsHelp.setProvider(SettingsGuideSections.INSTANCE);
  }
}
