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

package com.google.android.accessibility.talkback.notifications

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.utils.NotificationUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** The test notification and automation entries of the Notifications settings screen. */
@SuppressLint("MissingPermission")
object NotificationSettingsEntries {
  private const val TEST_DELAY_MS = 5_000L
  private const val TEST_NOTIFICATION_ID = 0x0BAC7A20

  @JvmStatic
  fun setUpPreferences(fragment: PreferenceFragmentCompat) {
    fragment.findPreference<Preference>("pref_notification_test")?.setOnPreferenceClickListener {
      postTest(fragment.requireContext())
      true
    }
    fragment.findPreference<Preference>("pref_notification_automation")?.setOnPreferenceClickListener {
      showAutomation(fragment)
      true
    }
  }

  /** Posts a notification after a few seconds, so the settings can be heard with the screen off. */
  private fun postTest(context: Context) {
    val app = context.applicationContext
    if (!NotificationManagerCompat.from(app).areNotificationsEnabled()) {
      Toast.makeText(app, R.string.notification_test_not_allowed, Toast.LENGTH_LONG).show()
      return
    }
    Toast.makeText(app, R.string.notification_test_posting, Toast.LENGTH_SHORT).show()
    Handler(Looper.getMainLooper()).postDelayed({
      val notification =
        NotificationUtils.createDefaultNotificationBuilder(app)
          .setContentTitle(app.getString(R.string.notification_test_content_title))
          .setContentText(app.getString(R.string.notification_test_content_text))
          .setAutoCancel(true)
          .build()
      // Checked when the test was asked for; a refusal since then is caught.
      try {
        NotificationManagerCompat.from(app).notify(TEST_NOTIFICATION_ID, notification)
      } catch (e: SecurityException) {
        // Notifications were turned off meanwhile.
      }
    }, TEST_DELAY_MS)
  }

  /** Lists the broadcasts that automation apps can send, and copies the one chosen. */
  private fun showAutomation(fragment: PreferenceFragmentCompat) {
    val context = fragment.requireContext()
    val actions =
      listOf(
        NotificationAutomationReceiver.ON to R.string.notification_automation_on,
        NotificationAutomationReceiver.OFF to R.string.notification_automation_off,
        NotificationAutomationReceiver.TOGGLE to R.string.notification_automation_toggle,
        NotificationAutomationReceiver.STOP to R.string.notification_automation_stop,
        NotificationAutomationReceiver.READ_ALL to R.string.notification_automation_read_all,
      )
    val items = actions.map { (action, label) -> context.getString(label) + "\n" + context.packageName + action }
    MaterialAlertDialogBuilder(fragment.requireActivity())
      .setTitle(R.string.notification_automation_title)
      .setItems(items.toTypedArray()) { _, which ->
        val action = context.packageName + actions[which].first
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(action, action))
        Toast.makeText(context, R.string.notification_copied, Toast.LENGTH_SHORT).show()
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }
}
