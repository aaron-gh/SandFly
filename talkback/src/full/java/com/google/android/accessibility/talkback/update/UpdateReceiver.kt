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

package com.google.android.accessibility.talkback.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Receives taps on the update notification and status updates from the install session. */
class UpdateReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      ACTION_INSTALL ->
        intent.getStringExtra(EXTRA_DOWNLOAD_URL)?.let { Updater.install(context, it) }
      ACTION_INSTALL_STATUS -> Updater.onInstallStatus(context, intent)
    }
  }

  companion object {
    const val ACTION_INSTALL = "com.google.android.accessibility.talkback.update.INSTALL"
    const val ACTION_INSTALL_STATUS =
      "com.google.android.accessibility.talkback.update.INSTALL_STATUS"
    const val EXTRA_DOWNLOAD_URL = "download_url"
    /** The app ID of the APK that the install session installs. */
    const val EXTRA_INSTALLED_PACKAGE = "installed_package"
  }
}
