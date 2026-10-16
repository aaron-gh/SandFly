/*
 * Copyright 2026 Backtalk contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.braille.brailledisplay.platform.external;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * Asks the user, with a warning, whether an app may be a braille display. The notification that
 * Backtalk shows when it finds a new display app opens this, and a display app can open it too, with
 * {@link #ACTION_ASK} and its package name in {@link #EXTRA_PACKAGE}.
 */
public final class ExternalDisplayApprovalActivity extends Activity {
  /** Opens the question about the app in {@link #EXTRA_PACKAGE}. */
  public static final String ACTION_ASK = "fyi.quin.backtalk.action.ASK_BRAILLE_DISPLAY";

  public static final String EXTRA_PACKAGE = "fyi.quin.backtalk.extra.PACKAGE";

  static Intent intent(Context context, String app) {
    return new Intent(ACTION_ASK)
        .setClass(context, ExternalDisplayApprovalActivity.class)
        .putExtra(EXTRA_PACKAGE, app)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState != null) {
      return; // the warning is still showing
    }
    String app = getIntent().getStringExtra(EXTRA_PACKAGE);
    if (app == null) {
      app = getCallingPackage();
    }
    // Only an app that can be a display is asked about, and only once it's allowed is there nothing
    // to ask.
    if (app == null
        || !ExternalDisplayApps.displayServices(this, 0).containsKey(app)
        || ExternalDisplayApps.state(ExternalDisplayApps.prefs(this), app)
            == ExternalDisplayApps.State.ALLOWED) {
      finish();
      return;
    }
    String asked = app;
    ExternalDisplayApproval.confirm(
        this,
        asked,
        () -> {
          ExternalDisplayApproval.setAllowed(this, asked, true);
          finish();
        },
        () -> {
          ExternalDisplayApproval.setAllowed(this, asked, false);
          finish();
        },
        this::finish);
  }
}
