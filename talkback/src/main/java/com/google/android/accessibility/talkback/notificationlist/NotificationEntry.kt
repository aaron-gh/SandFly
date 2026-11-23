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
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.StatusBarNotification
import android.text.format.DateUtils
import java.text.DateFormat

/** What the list shows of one notification. */
class NotificationEntry(
  val notification: StatusBarNotification,
  val appName: CharSequence,
  val title: CharSequence?,
  val text: CharSequence?,
  val time: CharSequence,
  /** When the notification says it happened, such as when a message arrived. */
  val timeMillis: Long,
  /** Tells apart the rows of a messaging notification's messages, which share [key]. */
  val rowKey: String = notification.key,
) {
  val key: String
    get() = notification.key

  /**
   * One entry for each message of a messaging notification with two or more, which apps such as
   * Telegram post once per chat and update as messages arrive. This entry for any other.
   */
  fun messageEntries(): List<NotificationEntry> {
    val extras = notification.notification.extras ?: Bundle.EMPTY
    val messages = messages(extras)
    if (messages.size < 2) {
      return listOf(this)
    }
    // The big title names the newest message's sender as well, as in "Group: Sender".
    val chat =
      (extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
          ?: extras.getCharSequence(Notification.EXTRA_TITLE)
          ?: title)
        ?.limited()
    return messages.mapIndexed { index, message ->
      val sender = message.sender?.toString()?.trim()
      val text =
        if (sender.isNullOrEmpty() || sender == chat?.toString()?.trim()) message.text
        else "$sender: ${message.text}"
      val millis = if (message.timeMillis > 0) message.timeMillis else timeMillis
      NotificationEntry(
        notification,
        appName,
        chat,
        text.limited(),
        DateUtils.formatSameDayTime(millis, System.currentTimeMillis(), DateFormat.SHORT, DateFormat.SHORT),
        millis,
        "$key#$index#${message.timeMillis}",
      )
    }
  }

  private class Message(val text: CharSequence, val sender: CharSequence?, val timeMillis: Long)

  companion object {
    fun from(notification: StatusBarNotification, appName: CharSequence): NotificationEntry {
      val extras = notification.notification.extras ?: Bundle.EMPTY
      val title =
        extras.getCharSequence(Notification.EXTRA_TITLE_BIG)
          ?: extras.getCharSequence(Notification.EXTRA_TITLE)
      val text =
        extras.nonBlank(Notification.EXTRA_BIG_TEXT)
          ?: extras.nonBlank(Notification.EXTRA_TEXT)
          ?: lastMessage(extras)
          ?: extras
            .getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.filterNotNull()
            ?.filter { it.isNotBlank() }
            ?.joinToString("\n")
            ?.takeIf { it.isNotEmpty() }
          ?: extras.nonBlank(Notification.EXTRA_SUB_TEXT)
          ?: extras.nonBlank(Notification.EXTRA_INFO_TEXT)
          ?: extras.nonBlank(Notification.EXTRA_SUMMARY_TEXT)
          ?: notification.notification.tickerText?.takeIf { it.isNotBlank() }
      val timeMillis = timeMillis(notification)
      return NotificationEntry(
        notification,
        appName,
        title?.limited(),
        text?.limited(),
        DateUtils.formatSameDayTime(
          timeMillis,
          System.currentTimeMillis(),
          DateFormat.SHORT,
          DateFormat.SHORT,
        ),
        timeMillis,
      )
    }

    private fun Bundle.nonBlank(key: String): CharSequence? =
      getCharSequence(key)?.takeIf { it.isNotBlank() }

    /** Android's own limit for a notification's texts, which apps can get around. */
    private const val MAX_LENGTH = 5 * 1024

    private fun CharSequence.limited(): CharSequence =
      if (length > MAX_LENGTH) subSequence(0, MAX_LENGTH) else this

    /** The newest message of a messaging notification, as "sender: text". */
    @Suppress("DEPRECATION") // getParcelableArray(String, Class) needs Android 13.
    private fun lastMessage(extras: Bundle): CharSequence? {
      val messages: Array<Parcelable> = extras.getParcelableArray(Notification.EXTRA_MESSAGES) ?: return null
      val last = messages.lastOrNull() as? Bundle ?: return null
      val text = last.getCharSequence("text") ?: return null
      val sender = last.getCharSequence("sender")
      return if (sender.isNullOrBlank()) text else "$sender: $text"
    }

    /** A messaging notification's messages with text, oldest first, as MessagingStyle keeps them. */
    @Suppress("DEPRECATION") // getParcelableArray(String, Class) needs Android 13.
    private fun messages(extras: Bundle): List<Message> {
      val messages: Array<Parcelable> = extras.getParcelableArray(Notification.EXTRA_MESSAGES) ?: return emptyList()
      return messages.mapNotNull { parcelable ->
        val bundle = parcelable as? Bundle ?: return@mapNotNull null
        val text = bundle.getCharSequence("text")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        Message(text, sender(bundle), bundle.getLong("time"))
      }
    }

    /** The sender's name, which Android 9 and later keep as a Person. */
    @Suppress("DEPRECATION") // getParcelable(String, Class) needs Android 13.
    private fun sender(message: Bundle): CharSequence? =
      message.getCharSequence("sender")
        ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          (message.getParcelable<Parcelable>("sender_person") as? Person)?.name
        } else {
          null
        }

    private fun timeMillis(notification: StatusBarNotification): Long {
      val posted = notification.notification
      return if (posted.extras?.getBoolean(Notification.EXTRA_SHOW_WHEN, true) != false && posted.`when` != 0L) {
        posted.`when`
      } else {
        notification.postTime
      }
    }
  }
}
