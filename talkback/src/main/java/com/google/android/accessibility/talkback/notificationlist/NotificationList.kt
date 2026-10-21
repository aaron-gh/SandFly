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
import android.os.Build
import android.service.notification.NotificationListenerService.Ranking
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
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
  private var cached: List<NotificationItem>? = null
  private val appNames = HashMap<String, CharSequence>()

  /** Whether the listener is connected, so that the list is current. */
  var isConnected = false
    private set

  /** Whether the Notifications action opens this list instead of the system's shade. */
  fun isEnabled(context: Context): Boolean =
    SharedPreferencesUtils.getSharedPreferences(context)
      .getBoolean(
        context.getString(R.string.pref_notification_list_key),
        context.resources.getBoolean(R.bool.pref_notification_list_default),
      )

  /** Whether the user has given Backtalk notification access. */
  fun isAccessGranted(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      context
        .getSystemService(NotificationManager::class.java)
        .isNotificationListenerAccessGranted(
          ComponentName(context, NotificationListService::class.java)
        )
    } else {
      NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }

  fun addListener(listener: Listener) {
    listeners.add(listener)
  }

  fun removeListener(listener: Listener) {
    listeners.remove(listener)
  }

  /** The notifications to show, in order. */
  fun items(context: Context): List<NotificationItem> =
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

  /**
   * Android groups an app's notifications itself once it has several, and reports that only as a
   * new ranking, so each notification takes the group the ranking gives it.
   */
  internal fun onRankingUpdate(ranking: RankingMap?) {
    if (ranking != null) {
      val notificationRanking = Ranking()
      notifications.values.forEach {
        if (ranking.getRanking(it.key, notificationRanking)) {
          it.overrideGroupKey = notificationRanking.overrideGroupKey
        }
      }
    }
    this.ranking = ranking
    changed()
  }

  private fun changed() {
    cached = null
    listeners.toList().forEach { it.onNotificationsChanged() }
  }

  /**
   * Orders the notifications as the system ranks them. A group is shown where its first member
   * ranks, as one item when it has two or more members, and its summary only when it has no other
   * members, as the summary repeats them. As in the system's shade, the members of an app's group
   * without a summary are shown one by one.
   */
  private fun build(context: Context): List<NotificationItem> {
    val order = ranking?.orderedKeys?.toList() ?: notifications.keys.toList()
    val orderSet = order.toSet()
    val ranked =
      (order.mapNotNull { notifications[it] } +
          notifications.values.filter { it.key !in orderSet })
        .filter { !isPlaceholder(it) }
    val groups = ranked.groupBy { it.groupKey }
    val shownGroups = HashSet<String>()
    val result = ArrayList<NotificationItem>()
    for (notification in ranked) {
      val groupKey = notification.groupKey
      if (!shownGroups.add(groupKey)) {
        continue
      }
      val members = groups[groupKey].orEmpty()
      val (summaries, children) = members.partition { it.notification.isGroupSummary() }
      if (summaries.isEmpty() && members.none { it.overrideGroupKey != null }) {
        members.forEach { result.add(NotificationItem.Single(entry(context, it))) }
        continue
      }
      val entries = children.ifEmpty { members }.map { entry(context, it) }
      result.add(
        if (entries.size >= 2) {
          NotificationItem.Group(
            groupKey,
            entries.first().appName,
            summaries.firstOrNull()?.let { entry(context, it) },
            entries,
          )
        } else {
          NotificationItem.Single(entries.first())
        }
      )
    }
    return result
  }

  /**
   * One UI's "N more notifications", which stands in the shade for silent notifications that the
   * list shows one by one.
   */
  private fun isPlaceholder(notification: StatusBarNotification): Boolean =
    notification.packageName == "com.android.systemui" && notification.tag == "INSIGNIFICANT"

  private fun entry(context: Context, notification: StatusBarNotification): NotificationEntry =
    NotificationEntry.from(notification, appName(context, notification.packageName))

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

/** A notification, or a group of two or more, as the list shows it. */
sealed class NotificationItem {
  abstract val entries: List<NotificationEntry>

  class Single(val entry: NotificationEntry) : NotificationItem() {
    override val entries: List<NotificationEntry>
      get() = listOf(entry)
  }

  class Group(
    val key: String,
    val appName: CharSequence,
    val summary: NotificationEntry?,
    override val entries: List<NotificationEntry>,
  ) : NotificationItem()
}
