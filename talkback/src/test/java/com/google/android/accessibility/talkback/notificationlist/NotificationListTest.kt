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
import android.os.Process
import android.service.notification.StatusBarNotification
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationListTest {
  private val context = RuntimeEnvironment.getApplication()

  private fun posted(packageName: String, id: Int, notification: Notification) =
    @Suppress("DEPRECATION")
    StatusBarNotification(packageName, packageName, id, null, 0, 0, 0, notification, Process.myUserHandle(), 0L)

  private fun mediaActivity(): Notification =
    Notification.Builder(context, "MediaOngoingActivity").setContentTitle("MediaOngoingActivity").build().also {
      it.extras.putInt("android.ongoingActivityNoti.style", 1)
    }

  @After
  fun disconnect() {
    NotificationList.onDisconnected()
    setLeaveOutMedia(false)
  }

  private fun setLeaveOutMedia(on: Boolean) {
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putBoolean(context.getString(R.string.pref_notification_list_skip_media_key), on)
      .commit()
  }

  @Test
  fun oneUiMediaActivityIsLeftOut() {
    val player = posted("com.example.player", 2, Notification.Builder(context, "playing").setContentTitle("Episode").build())
    NotificationList.onConnected(arrayOf(posted("com.android.systemui", 1, mediaActivity()), player), null)

    val keys = NotificationList.items(context).flatMap { it.entries }.map { it.key }

    assertEquals(listOf(player.key), keys)
  }

  private fun message(title: String, time: Long, group: String? = null, summary: Boolean = false): Notification =
    Notification.Builder(context, "messages")
      .setContentTitle(title)
      .setWhen(time)
      .setShowWhen(true)
      .apply {
        group?.let { setGroup(it) }
        setGroupSummary(summary)
      }
      .build()

  @Test
  fun byTime_listsEveryNotificationNewestFirstWithoutSummaries() {
    val adam = posted("com.example.chat", 1, message("Adam", 1_000L, group = "chats"))
    val ben = posted("com.example.chat", 2, message("Ben", 3_000L, group = "chats"))
    val summary = posted("com.example.chat", 3, message("2 chats", 3_000L, group = "chats", summary = true))
    val dad = posted("com.example.sms", 4, message("Dad", 2_000L))
    NotificationList.onConnected(arrayOf(adam, ben, summary, dad), null)

    val byTime = NotificationList.itemsByTime(context)

    assertEquals(listOf(ben.key, dad.key, adam.key), byTime.flatMap { it.entries }.map { it.key })
    assertEquals(3, byTime.filterIsInstance<NotificationItem.Single>().size)
  }

  @Test
  fun grouped_keepsTheGroup() {
    val adam = posted("com.example.chat", 1, message("Adam", 1_000L, group = "chats"))
    val ben = posted("com.example.chat", 2, message("Ben", 3_000L, group = "chats"))
    val summary = posted("com.example.chat", 3, message("2 chats", 3_000L, group = "chats", summary = true))
    NotificationList.onConnected(arrayOf(adam, ben, summary), null)

    val items = NotificationList.items(context)

    assertEquals(1, items.size)
    assertEquals(2, (items.single() as NotificationItem.Group).entries.size)
  }

  @Test
  fun mediaPlayers_leftOutOnlyWhenTheSwitchIsOn() {
    val player =
      posted(
        "com.example.player",
        1,
        Notification.Builder(context, "playing")
          .setContentTitle("Episode")
          .setCategory(Notification.CATEGORY_TRANSPORT)
          .build(),
      )
    val session =
      posted(
        "com.example.radio",
        2,
        Notification.Builder(context, "radio").setContentTitle("Station").build().also {
          it.extras.putString(Notification.EXTRA_MEDIA_SESSION, "token")
        },
      )
    val dad = posted("com.example.sms", 3, message("Dad", 2_000L))
    NotificationList.onConnected(arrayOf(player, session, dad), null)

    fun keys() = NotificationList.items(context).flatMap { it.entries }.map { it.key }

    assertEquals(listOf(player.key, session.key, dad.key), keys())
    setLeaveOutMedia(true)
    assertEquals(listOf(dad.key), keys())
    assertEquals(listOf(dad.key), NotificationList.itemsByTime(context).flatMap { it.entries }.map { it.key })
    setLeaveOutMedia(false)
    assertEquals(listOf(player.key, session.key, dad.key), keys())
  }
}
