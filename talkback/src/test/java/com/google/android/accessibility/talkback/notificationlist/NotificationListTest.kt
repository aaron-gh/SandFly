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
import android.app.Person
import android.os.Process
import android.service.notification.StatusBarNotification
import android.widget.RemoteViews
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

  private fun chat(title: String, vararg messages: Pair<String, Long>): Notification {
    val style = Notification.MessagingStyle(Person.Builder().setName("Me").build()).setConversationTitle(title).setGroupConversation(true)
    messages.forEach { (text, time) ->
      val sender = text.substringBefore(':')
      style.addMessage(text.substringAfter(": "), time, Person.Builder().setName(sender).build())
    }
    return Notification.Builder(context, "messages")
      .setContentTitle(title)
      .setContentText(messages.last().first)
      .setWhen(messages.last().second)
      .setShowWhen(true)
      .setStyle(style)
      .build()
  }

  @Test
  fun byTime_listsEachMessageOfAChatNotification() {
    val family =
      posted("org.telegram.messenger", 1, chat("Family", "Ann: one" to 1_000L, "Bob: two" to 3_000L, "Ann: three" to 5_000L))
    val dad = posted("com.example.sms", 2, message("Dad", 4_000L))
    NotificationList.onConnected(arrayOf(family, dad), null)

    val entries = NotificationList.itemsByTime(context).flatMap { it.entries }

    assertEquals(listOf("Ann: three", null, "Bob: two", "Ann: one"), entries.map { it.text?.toString() })
    assertEquals(listOf(family.key, dad.key, family.key, family.key), entries.map { it.key })
    assertEquals(4, entries.map { it.rowKey }.toSet().size)
    assertEquals(listOf("Family", "Dad", "Family", "Family"), entries.map { it.title?.toString() })
  }

  @Test
  fun grouped_keepsAChatNotificationAsOneItem() {
    val family = posted("org.telegram.messenger", 1, chat("Family", "Ann: one" to 1_000L, "Bob: two" to 3_000L))
    NotificationList.onConnected(arrayOf(family), null)

    val entries = NotificationList.items(context).flatMap { it.entries }

    assertEquals(listOf(family.key), entries.map { it.rowKey })
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

  @Test
  fun customLayoutText_standsInForMissingText() {
    // Samsung Weather puts its text only in a custom layout (#583).
    val layout = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
    layout.setTextViewText(android.R.id.text1, "12°, rain 30%")
    val titleOnly = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
    titleOnly.setTextViewText(android.R.id.text1, "Weather")
    val weather = Notification.Builder(context, "weather").setContentTitle("Weather").build()
    @Suppress("DEPRECATION")
    weather.contentView = layout
    val withText =
      Notification.Builder(context, "weather").setContentTitle("Weather").setContentText("Sunny").build()
    @Suppress("DEPRECATION")
    withText.contentView = layout
    val repeatsTitle = Notification.Builder(context, "weather").setContentTitle("Weather").build()
    @Suppress("DEPRECATION")
    repeatsTitle.contentView = titleOnly
    NotificationList.onConnected(
      arrayOf(
        posted("com.example.weather", 1, weather),
        posted("com.example.weather2", 2, withText),
        posted("com.example.weather3", 3, repeatsTitle),
      ),
      null,
    )

    val texts = NotificationList.items(context).flatMap { it.entries }.map { it.text?.toString() }

    assertEquals(listOf("12°, rain 30%", "Sunny", null), texts)
  }

  @Test
  fun customLayoutText_standsInForBlankText() {
    val layout = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
    layout.setTextViewText(android.R.id.text1, "12°, rain 30%")
    val weather = Notification.Builder(context, "weather").setContentTitle("Weather").build()
    weather.extras.putCharSequence(Notification.EXTRA_TEXT, " ")
    @Suppress("DEPRECATION")
    weather.contentView = layout
    NotificationList.onConnected(arrayOf(posted("com.example.weather", 1, weather)), null)

    val texts = NotificationList.items(context).flatMap { it.entries }.map { it.text?.toString() }

    assertEquals(listOf("12°, rain 30%"), texts)
  }

  @Test
  fun subInfoAndSummaryText_onlyWhenNothingElse() {
    val layout = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
    layout.setTextViewText(android.R.id.text1, "12°, rain 30%")
    fun withExtra(title: String, key: String, value: String) =
      Notification.Builder(context, "texts").setContentTitle(title).build().also {
        it.extras.putCharSequence(key, value)
      }
    val sub = withExtra("Sub", Notification.EXTRA_SUB_TEXT, "Work account")
    val info = withExtra("Info", Notification.EXTRA_INFO_TEXT, "3 new")
    val summary = withExtra("Summary", Notification.EXTRA_SUMMARY_TEXT, "2 more")
    val subAndText = withExtra("SubAndText", Notification.EXTRA_SUB_TEXT, "Work account")
    subAndText.extras.putCharSequence(Notification.EXTRA_TEXT, "Meeting at 10")
    val subAndTicker = withExtra("SubAndTicker", Notification.EXTRA_SUB_TEXT, "Work account")
    subAndTicker.tickerText = "New mail"
    val subAndLayout = withExtra("SubAndLayout", Notification.EXTRA_SUB_TEXT, "Work account")
    @Suppress("DEPRECATION")
    subAndLayout.contentView = layout
    val notifications =
      listOf(sub, info, summary, subAndText, subAndTicker, subAndLayout).mapIndexed { index, notification ->
        posted("com.example.app$index", index + 1, notification)
      }
    NotificationList.onConnected(notifications.toTypedArray(), null)

    val texts =
      NotificationList.items(context).flatMap { it.entries }.associate { it.title?.toString() to it.text?.toString() }

    assertEquals(
      mapOf(
        "Sub" to "Work account",
        "Info" to "3 new",
        "Summary" to "2 more",
        "SubAndText" to "Meeting at 10",
        "SubAndTicker" to "New mail",
        "SubAndLayout" to "12°, rain 30%",
      ),
      texts,
    )
  }
}
