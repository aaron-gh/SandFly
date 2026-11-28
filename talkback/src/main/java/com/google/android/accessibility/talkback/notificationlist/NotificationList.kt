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
import com.google.android.accessibility.talkback.notifications.NotificationInfo
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils

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
  /** Whether [cached] leaves out media players. */
  private var cachedLeavesOutMedia = false
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

  /** Whether the list shows every notification on its own, newest first. */
  fun isByTime(context: Context): Boolean =
    SharedPreferencesUtils.getSharedPreferences(context)
      .getBoolean(
        context.getString(R.string.pref_notification_list_by_time_key),
        context.resources.getBoolean(R.bool.pref_notification_list_by_time_default),
      )

  /** Whether the list leaves out media players' notifications. */
  fun leavesOutMedia(context: Context): Boolean =
    SharedPreferencesUtils.getSharedPreferences(context)
      .getBoolean(
        context.getString(R.string.pref_notification_list_skip_media_key),
        context.resources.getBoolean(R.bool.pref_notification_list_skip_media_default),
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
  fun items(context: Context): List<NotificationItem> {
    val leaveOutMedia = leavesOutMedia(context)
    cached?.takeIf { cachedLeavesOutMedia == leaveOutMedia }?.let {
      return it
    }
    return build(context, leaveOutMedia).also {
      cached = it
      cachedLeavesOutMedia = leaveOutMedia
    }
  }

  /**
   * Every notification as an item of its own, newest first by the time it shows, and every message
   * of a messaging notification with several. A group's summary is left out where [items] leaves it
   * out, as it repeats the group's members.
   */
  fun itemsByTime(context: Context): List<NotificationItem> = byTime(items(context))

  internal fun byTime(items: List<NotificationItem>): List<NotificationItem> =
    items
      .flatMap { it.entries }
      .flatMap { it.messageEntries() }
      .sortedWith(
        compareByDescending<NotificationEntry> { it.timeMillis }
          .thenByDescending { it.notification.postTime }
      )
      .map { NotificationItem.Single(it) }

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
    lastLoggedKeys = emptyList()
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
   * without a summary are shown one by one. Media players are left out when [leaveOutMedia], and
   * notifications the shade hides, as [isHiddenInShade] tells, are left out.
   */
  private fun build(context: Context, leaveOutMedia: Boolean): List<NotificationItem> {
    val order = ranking?.orderedKeys?.toList() ?: notifications.keys.toList()
    val orderSet = order.toSet()
    val all =
      order.mapNotNull { notifications[it] } + notifications.values.filter { it.key !in orderSet }
    val ranked =
      all.filter {
        !isPlaceholder(it) &&
          !(leaveOutMedia && NotificationInfo.isMedia(it.notification)) &&
          !isHiddenInShade(it)
      }
    logContents(all, ranked.size)
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
   * list shows one by one, and its Now Bar media notification, which has no text or actions and
   * repeats the media app's own notification.
   */
  private fun isPlaceholder(notification: StatusBarNotification): Boolean =
    (notification.packageName == "com.android.systemui" && notification.tag == "INSIGNIFICANT") ||
      NotificationInfo.isSamsungMediaActivity(notification.packageName, notification.notification)

  /**
   * Whether the system's shade hides [notification]: that of a suspended app, one that Do Not
   * Disturb hides from the notification list, or one on a blocked channel.
   */
  private fun isHiddenInShade(notification: StatusBarNotification): Boolean {
    val map = ranking ?: return false
    val notificationRanking = Ranking()
    if (!map.getRanking(notification.key, notificationRanking)) return false
    val suspended =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && notificationRanking.isSuspended
    return isHiddenInShade(
      notificationRanking.importance,
      suspended,
      notificationRanking.suppressedVisualEffects,
    )
  }

  /** [isHiddenInShade] for a notification with the ranking's [importance], [suspended] and [suppressedEffects]. */
  internal fun isHiddenInShade(importance: Int, suspended: Boolean, suppressedEffects: Int): Boolean =
    suspended ||
      importance == NotificationManager.IMPORTANCE_NONE ||
      (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
        suppressedEffects and NotificationManager.Policy.SUPPRESSED_EFFECT_NOTIFICATION_LIST != 0)

  /**
   * Logs what the list is built from, without the notifications' text: for each notification its
   * app, channel, importance, flags, whether it has a title and text, and the names of extras that
   * aren't Android's own, which some systems use to hide notifications from the shade. Logged
   * only when the notifications differ from those last logged, as updates such as progress
   * rebuild the list often.
   */
  private fun logContents(all: List<StatusBarNotification>, shown: Int) {
    if (LogUtils.getLogLevel() > android.util.Log.DEBUG) return
    val keys = all.map { it.key }
    if (keys == lastLoggedKeys) return
    lastLoggedKeys = keys
    LogUtils.d(TAG, "%d notifications, %d shown before grouping", all.size, shown)
    val notificationRanking = Ranking()
    all.take(MAX_LOGGED).forEach {
      val posted = it.notification
      val extras = posted.extras
      val importance =
        if (ranking?.getRanking(it.key, notificationRanking) == true) notificationRanking.importance else null
      val other = extras?.keySet()?.filterNot { key -> key.startsWith("android.") }?.sorted().orEmpty()
      LogUtils.d(
        TAG,
        "%s",
        "${it.packageName} channel=${posted.channelId} importance=$importance" +
          " flags=0x${Integer.toHexString(posted.flags)} ongoing=${it.isOngoing}" +
          " title=${!extras?.getCharSequence(android.app.Notification.EXTRA_TITLE).isNullOrBlank()}" +
          " text=${!extras?.getCharSequence(android.app.Notification.EXTRA_TEXT).isNullOrBlank()}" +
          " custom=${posted.contentView != null || posted.bigContentView != null}" +
          " hidden=${isHiddenInShade(it)} extras=$other",
      )
    }
  }

  private const val TAG = "NotificationList"
  /** The keys of the notifications [logContents] last logged. */
  private var lastLoggedKeys: List<String> = emptyList()
  private const val MAX_LOGGED = 100

  private fun entry(context: Context, notification: StatusBarNotification): NotificationEntry =
    NotificationEntry.from(notification, appName(context, notification.packageName)) { skip ->
      CustomViewText.of(context, notification, skip)
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
