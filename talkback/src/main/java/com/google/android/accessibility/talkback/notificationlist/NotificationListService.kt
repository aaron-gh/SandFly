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

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.notifications.NotificationReader
import com.google.android.accessibility.talkback.notifications.NotificationSettings
import com.google.android.accessibility.talkback.notifications.SpokenSchedules
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * Receives the notifications for [NotificationList]. Android binds it while the user has given
 * Backtalk notification access. It unbinds itself while the list is turned off, and the list binds
 * it again when it opens.
 */
class NotificationListService : NotificationListenerService() {

  private val switchListener =
    SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      if (key == getString(R.string.pref_notification_list_key) ||
        key == NotificationSettings.Companion.Keys.DISMISS ||
        key == NotificationSettings.Companion.Keys.APP_CANCEL) {
        if (!isNeeded(this)) {
          unbind()
        } else if (key == getString(R.string.pref_notification_list_key) && listEnabled()) {
          connectList()
        }
      }
    }

  private fun listEnabled() = NotificationList.isEnabled(this)

  override fun onListenerConnected() {
    if (!isNeeded(this)) {
      unbind()
      return
    }
    instance = this
    SharedPreferencesUtils.getSharedPreferences(this)
      .registerOnSharedPreferenceChangeListener(switchListener)
    if (listEnabled()) connectList()
  }

  private fun connectList() {
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
    if (listEnabled()) NotificationList.onPosted(notification, ranking)
  }

  override fun onNotificationRemoved(notification: StatusBarNotification, ranking: RankingMap?) {
    if (listEnabled()) NotificationList.onRemoved(notification, ranking)
  }

  override fun onNotificationRemoved(
    notification: StatusBarNotification,
    ranking: RankingMap?,
    reason: Int,
  ) {
    onNotificationRemoved(notification, ranking)
    val title =
      notification.notification.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
    NotificationReader.onNotificationRemoved(
      notification.packageName,
      notification.notification.`when`,
      title.orEmpty(),
      reason,
    )
  }

  override fun onNotificationRankingUpdate(ranking: RankingMap?) {
    if (listEnabled()) NotificationList.onRankingUpdate(ranking)
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

    /**
     * Whether the listener is needed: for SandFly's notification list, or to stop reading a
     * notification that goes away.
     */
    fun isNeeded(context: Context): Boolean =
      NotificationList.isEnabled(context) ||
        SystemClock.uptimeMillis() < keepConnectedUntil ||
        SharedPreferencesUtils.getSharedPreferences(context).let {
          NotificationSettings.read(it).needsListener ||
            it.getBoolean(SpokenSchedules.KEY_SUMMARY_DAILY, false)
        }

    private var keepConnectedUntil = 0L

    /** Keeps the listener connected for [millis], such as to read a summary. */
    fun keepConnected(context: Context, millis: Long) {
      keepConnectedUntil = SystemClock.uptimeMillis() + millis
      rebind(context)
    }

    /** Whether the user gave SandFly notification access. */
    fun isAccessGranted(context: Context): Boolean = NotificationList.isAccessGranted(context)

    /** Asks Android to connect the listener again, after a setting that needs it is turned on. */
    fun rebind(context: Context) {
      runCatching {
        requestRebind(ComponentName(context, NotificationListService::class.java))
      }.onFailure { Log.w(TAG, "Can't rebind", it) }
    }

    /** The connected listener, through which notifications are dismissed. */
    var instance: NotificationListService? = null
      private set
  }
}
