/*
 * Copyright 2026 SandFly contributors
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

package io.github.aaron_gh.sandfly.addon

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import android.widget.Toast

/** Receives the status of an update's install session. */
class InstallReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
      PackageInstaller.STATUS_PENDING_USER_ACTION -> {
        val confirm =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
          } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_INTENT)
          }
        try {
          // The add-on's screen is in front, which lets it open Android's confirmation.
          context.startActivity(confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ?: return failed(context, status))
        } catch (e: ActivityNotFoundException) {
          failed(context, status)
        }
      }
      // Android stops the add-on to replace it.
      PackageInstaller.STATUS_SUCCESS -> {}
      PackageInstaller.STATUS_FAILURE_ABORTED -> {}
      else -> {
        Log.w("SandFlyAddonUpdate", "Install status $status: ${intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)}")
        failed(context, status)
      }
    }
  }

  private fun failed(context: Context, @Suppress("UNUSED_PARAMETER") status: Int) {
    Toast.makeText(context, R.string.addon_update_failed, Toast.LENGTH_LONG).show()
  }
}
