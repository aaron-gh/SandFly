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

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * The notifications that [NotificationListService] reports, in the order the system ranks them.
 * Changes only when a notification is posted, updated or removed, or the ranking changes. Used on
 * the main thread, where the listener's callbacks arrive.
 */
object NotificationList {
  fun interface Listener {
    fun onNotificationsChanged()
  }

  private val listeners = LinkedHashSet<Listener>()
  private val notifications = LinkedHashMap<String, StatusBarNotification>()
  private var ranking: RankingMap? = null
  private var cached: List<NotificationEntry>? = null
  private val appNames = HashMap<String, CharSequence>()

  /** Whether the listener is connected, so that the list is current. */
  var isConnected = false
    private set

  /** Whether the Notifications action opens this list instead of the system's shade. */
  @JvmStatic
  fun isEnabled(context: Context): Boolean =
    SharedPreferencesUtils.getSharedPreferences(context)
      .getBoolean(
        context.getString(R.string.pref_notification_list_key),
        context.resources.getBoolean(R.bool.pref_notification_list_default),
      )

  /** Whether the user has given Backtalk notification access. */
  @JvmStatic
  fun isAccessGranted(context: Context): Boolean =
    context
      .getSystemService(NotificationManager::class.java)
      .isNotificationListenerAccessGranted(
        ComponentName(context, NotificationListService::class.java)
      )

  fun addListener(listener: Listener) {
    listeners.add(listener)
  }

  fun removeListener(listener: Listener) {
    listeners.remove(listener)
  }

  /** The notifications to show, in order. */
  fun entries(context: Context): List<NotificationEntry> =
    cached ?: build(context).also { cached = it }

  internal fun onConnected(active: Array<StatusBarNotification>?, ranking: RankingMap?) {
    isConnected = true
    notifications.clear()
    active?.forEach { notifications[it.key] = it }
    this.ranking = ranking
    changed()
  }

  internal fun onDisconnected() {
    isConnected = false
    notifications.clear()
    ranking = null
    changed()
  }

  internal fun onPosted(notification: StatusBarNotification, ranking: RankingMap?) {
    notifications[notification.key] = notification
    ranking?.let { this.ranking = it }
    changed()
  }

  internal fun onRemoved(notification: StatusBarNotification, ranking: RankingMap?) {
    notifications.remove(notification.key)
    ranking?.let { this.ranking = it }
    changed()
  }

  internal fun onRankingUpdate(ranking: RankingMap?) {
    this.ranking = ranking
    changed()
  }

  private fun changed() {
    cached = null
    listeners.toList().forEach { it.onNotificationsChanged() }
  }

  /**
   * Orders the notifications as the system ranks them. A group is shown where its first member
   * ranks, members together, and its summary only when it has no other members, as the summary
   * repeats them.
   */
  private fun build(context: Context): List<NotificationEntry> {
    val order = ranking?.orderedKeys?.toList() ?: notifications.keys.toList()
    val ranked = order.mapNotNull { notifications[it] } + notifications.values.filter { it.key !in order }
    val groups = ranked.groupBy { it.groupKey }
    val shownGroups = HashSet<String>()
    val result = ArrayList<NotificationEntry>()
    for (notification in ranked) {
      val groupKey = notification.groupKey
      if (!shownGroups.add(groupKey)) {
        continue
      }
      val members = groups[groupKey].orEmpty()
      val children = members.filter { !it.notification.isGroupSummary() }
      for (member in children.ifEmpty { members }) {
        result.add(NotificationEntry.from(member, appName(context, member.packageName)))
      }
    }
    return result
  }

  private fun appName(context: Context, packageName: String): CharSequence =
    appNames.getOrPut(packageName) {
      val packageManager = context.packageManager
      runCatching {
          packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0))
        }
        .getOrDefault(packageName)
    }

  private fun android.app.Notification.isGroupSummary(): Boolean =
    flags and android.app.Notification.FLAG_GROUP_SUMMARY != 0
}
