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
) {
  val key: String
    get() = notification.key

  companion object {
    fun from(notification: StatusBarNotification, appName: CharSequence): NotificationEntry {
      val extras = notification.notification.extras ?: Bundle.EMPTY
      val title =
        extras.getCharSequence(Notification.EXTRA_TITLE_BIG)
          ?: extras.getCharSequence(Notification.EXTRA_TITLE)
      val text =
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
          ?: extras.getCharSequence(Notification.EXTRA_TEXT)
          ?: lastMessage(extras)
          ?: extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString("\n")
          ?: notification.notification.tickerText
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
