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

package com.google.android.accessibility.talkback.notificationlist

import android.content.SharedPreferences
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * Receives the notifications for [NotificationList]. Android binds it while the user has given
 * Backtalk notification access. It unbinds itself while the list is turned off, and the list binds
 * it again when it opens.
 */
class NotificationListService : NotificationListenerService() {

  private val switchListener =
    SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      if (key == getString(R.string.pref_notification_list_key) && !NotificationList.isEnabled(this)) {
        unbind()
      }
    }

  override fun onListenerConnected() {
    if (!NotificationList.isEnabled(this)) {
      unbind()
      return
    }
    instance = this
    SharedPreferencesUtils.getSharedPreferences(this)
      .registerOnSharedPreferenceChangeListener(switchListener)
    val active = runCatching { activeNotifications }.onFailure { Log.w(TAG, "No notifications", it) }
    NotificationList.onConnected(active.getOrNull(), currentRanking)
  }

  override fun onListenerDisconnected() {
    disconnected()
  }

  private fun unbind() {
    runCatching { requestUnbind() }.onFailure { Log.w(TAG, "Can't unbind", it) }
    disconnected()
  }

  private fun disconnected() {
    SharedPreferencesUtils.getSharedPreferences(this)
      .unregisterOnSharedPreferenceChangeListener(switchListener)
    if (instance === this) {
      instance = null
    }
    NotificationList.onDisconnected()
  }

  override fun onNotificationPosted(notification: StatusBarNotification, ranking: RankingMap?) {
    NotificationList.onPosted(notification, ranking)
  }

  override fun onNotificationRemoved(notification: StatusBarNotification, ranking: RankingMap?) {
    NotificationList.onRemoved(notification, ranking)
  }

  override fun onNotificationRankingUpdate(ranking: RankingMap?) {
    NotificationList.onRankingUpdate(ranking)
  }

  override fun onDestroy() {
    SharedPreferencesUtils.getSharedPreferences(this)
      .unregisterOnSharedPreferenceChangeListener(switchListener)
    if (instance === this) {
      instance = null
    }
    super.onDestroy()
  }

  companion object {
    private const val TAG = "NotificationListService"

    /** The connected listener, through which notifications are dismissed. */
    var instance: NotificationListService? = null
      private set
  }
}
