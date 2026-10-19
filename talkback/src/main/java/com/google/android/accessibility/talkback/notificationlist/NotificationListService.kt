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

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Receives the notifications for [NotificationList]. Android binds it while the user has given
 * Backtalk notification access.
 */
class NotificationListService : NotificationListenerService() {

  override fun onListenerConnected() {
    instance = this
    val active = runCatching { activeNotifications }.onFailure { Log.w(TAG, "No notifications", it) }
    NotificationList.onConnected(active.getOrNull(), currentRanking)
  }

  override fun onListenerDisconnected() {
    instance = null
    NotificationList.onDisconnected()
  }

  override fun onNotificationPosted(notification: StatusBarNotification, ranking: RankingMap?) {
    NotificationList.onPosted(notification, ranking)
  }

  override fun onNotificationRemoved(notification: StatusBarNotification, ranking: RankingMap?) {
    NotificationList.onRemoved(notification, ranking)
  }

  /**
   * Android groups an app's notifications itself once it has several, and reports that only as a
   * new ranking, so the notifications are read again with their new groups.
   */
  override fun onNotificationRankingUpdate(ranking: RankingMap?) {
    val active = runCatching { activeNotifications }.onFailure { Log.w(TAG, "No notifications", it) }
    NotificationList.onRankingUpdate(active.getOrNull(), ranking)
  }

  override fun onDestroy() {
    if (instance === this) {
      instance = null
    }
    super.onDestroy()
  }

  companion object {
    private const val TAG = "NotificationListService"

    /** The connected listener, through which notifications are dismissed. */
    @JvmStatic
    var instance: NotificationListService? = null
      private set
  }
}
