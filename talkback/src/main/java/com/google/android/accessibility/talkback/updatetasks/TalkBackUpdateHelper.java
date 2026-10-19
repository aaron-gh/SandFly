/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.google.android.accessibility.talkback.updatetasks;

import android.content.SharedPreferences;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.eventprocessor.PhoneticLetterMode;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/** Checks and setting changes that run each time the service starts. */
public class TalkBackUpdateHelper {
  private final TalkBackService service;
  private final SharedPreferences sharedPreferences;

  public TalkBackUpdateHelper(TalkBackService service) {
    this.service = service;
    sharedPreferences = SharedPreferencesUtils.getSharedPreferences(this.service);
  }

  public void checkUpdate() {
    BatteryOptimizationCheck.check(service, sharedPreferences);
    PhoneticLetterMode.migrate(sharedPreferences, service.getResources());
  }
}
