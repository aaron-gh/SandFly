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
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
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
        key == NotificationSettings.Companion.Keys.APP_CANCEL ||
        key == NotificationSettings.Companion.Keys.DND ||
        key == NotificationSettings.Companion.Keys.ENABLED) {
        if (!isNeeded(this)) {
          unbind()
        } else if (key == getString(R.string.pref_notification_list_key) && listEnabled()) {
          connectList()
        }
      }
    }

  private fun listEnabled() = NotificationList.isEnabled(this)

  private val handler = Handler(Looper.getMainLooper())
  /** Unbinds once a time it was kept connected for is over and nothing else needs it. */
  private val unbindWhenUnneeded = Runnable { if (!isNeeded(this)) unbind() else checkLater() }

  private fun checkLater() {
    handler.removeCallbacks(unbindWhenUnneeded)
    val left = keepConnectedUntil - SystemClock.uptimeMillis()
    if (left > 0) handler.postDelayed(unbindWhenUnneeded, left + 1_000L)
  }

  override fun onListenerConnected() {
    if (!isNeeded(this)) {
      unbind()
      return
    }
    instance = this
    // Notifications already in the shade aren't new.
    runCatching { activeNotifications }.getOrNull()?.forEach {
      postedTexts[it.key] = textOf(it.notification)
    }
    SharedPreferencesUtils.getSharedPreferences(this)
      .registerOnSharedPreferenceChangeListener(switchListener)
    if (listEnabled()) connectList()
    checkLater()
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
    handler.removeCallbacks(unbindWhenUnneeded)
    SharedPreferencesUtils.getSharedPreferences(this)
      .unregisterOnSharedPreferenceChangeListener(switchListener)
    if (instance === this) {
      instance = null
    }
    NotificationList.onDisconnected()
  }

  /**
   * The text of each notification posted while connected, by key, so that an update that only
   * changes its time or progress isn't taken for a new notification.
   */
  private val postedTexts = LinkedHashMap<String, String>()

  override fun onNotificationPosted(notification: StatusBarNotification, ranking: RankingMap?) {
    if (listEnabled()) NotificationList.onPosted(notification, ranking)
    val text = textOf(notification.notification)
    if (postedTexts.put(notification.key, text) != text) {
      while (postedTexts.size > MAX_POSTED_KEYS) postedTexts.remove(postedTexts.keys.first())
      val importance =
        Ranking().takeIf { ranking?.getRanking(notification.key, it) == true }?.importance
          ?: NotificationManager.IMPORTANCE_DEFAULT
      NotificationReader.onListenerPosted(notification.packageName, notification.notification, importance)
    }
  }

  private fun textOf(notification: Notification): String {
    val extras = notification.extras ?: return ""
    return "${extras.getCharSequence(Notification.EXTRA_TITLE)}|${extras.getCharSequence(Notification.EXTRA_TEXT)}"
  }

  override fun onNotificationRemoved(notification: StatusBarNotification, ranking: RankingMap?) {
    postedTexts.remove(notification.key)
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

  /**
   * Whether Do Not Disturb lets [notification], posted by [packageName], through, or null when it
   * isn't in the listener's ranking yet.
   */
  fun matchesInterruptionFilter(packageName: String, notification: Notification): Boolean? {
    val posted =
      runCatching { activeNotifications }.getOrNull()?.lastOrNull {
        it.packageName == packageName &&
          it.notification.`when` == notification.`when` &&
          it.notification.channelId == notification.channelId
      } ?: return null
    val ranking = Ranking()
    val map = currentRanking ?: return null
    if (!map.getRanking(posted.key, ranking)) return null
    return ranking.matchesInterruptionFilter()
  }

  override fun onNotificationRankingUpdate(ranking: RankingMap?) {
    if (listEnabled()) NotificationList.onRankingUpdate(ranking)
  }

  override fun onDestroy() {
    handler.removeCallbacks(unbindWhenUnneeded)
    SharedPreferencesUtils.getSharedPreferences(this)
      .unregisterOnSharedPreferenceChangeListener(switchListener)
    if (instance === this) {
      instance = null
    }
    super.onDestroy()
  }

  companion object {
    private const val TAG = "NotificationListService"
    private const val MAX_POSTED_KEYS = 500

    /**
     * Whether the listener is needed: for SandFly's notification list, or to stop reading a
     * notification that goes away. Only while SandFly is on: a bound listener keeps SandFly's
     * process running, and with it, the audio and speech engine it held, after SandFly is turned
     * off, which can leave another screen reader turned on then silent.
     */
    fun isNeeded(context: Context): Boolean =
      TalkBackService.getInstance() != null &&
        (NotificationList.isEnabled(context) ||
          SystemClock.uptimeMillis() < keepConnectedUntil ||
          SharedPreferencesUtils.getSharedPreferences(context).let {
            // With notification speech on, it reads notifications that Android sends no
            // accessibility event for, such as those on silent channels.
            NotificationSettings.read(it).let { s -> s.needsListener || s.enabled } ||
              it.getBoolean(SpokenSchedules.KEY_SUMMARY_DAILY, false)
          })

    /** Connects the listener when SandFly starts, if it is needed. */
    @JvmStatic
    fun onSandFlyStarted(context: Context) {
      if (instance == null && isNeeded(context)) rebind(context)
    }

    /** Disconnects the listener when SandFly stops. */
    @JvmStatic
    fun onSandFlyStopped() {
      instance?.unbind()
    }

    private var keepConnectedUntil = 0L

    /** Keeps the listener connected for [millis], such as to read a summary. */
    fun keepConnected(context: Context, millis: Long) {
      keepConnectedUntil = SystemClock.uptimeMillis() + millis
      instance?.checkLater() ?: rebind(context)
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
