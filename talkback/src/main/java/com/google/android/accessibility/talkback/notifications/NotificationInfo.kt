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

package com.google.android.accessibility.talkback.notifications

import android.app.Notification
import android.os.Build
import android.os.Bundle
import android.os.Parcelable

/**
 * What SandFly reads from a notification: its app and texts, and the facts the filters look at.
 * Texts are plain strings, cut to [MAX_TEXT], since apps can post far more than anyone hears.
 */
data class NotificationInfo(
  val packageName: String,
  val appName: String,
  val category: String? = null,
  val title: String = "",
  val text: String = "",
  val bigText: String = "",
  val subText: String = "",
  val ticker: String = "",
  /** The lines of an inbox notification, or the messages of a conversation, oldest first. */
  val lines: List<String> = emptyList(),
  val postedAtMillis: Long = 0,
  val ongoing: Boolean = false,
  val foregroundService: Boolean = false,
  val groupSummary: Boolean = false,
  val lowPriority: Boolean = false,
  val media: Boolean = false,
  val progress: Boolean = false,
  val call: Boolean = false,
) {
  /** The fullest text the notification has: its big text, else its lines, else its text. */
  val content: String
    get() =
      when {
        bigText.isNotBlank() -> bigText
        lines.isNotEmpty() -> lines.joinToString(". ")
        text.isNotBlank() -> text
        else -> ticker
      }

  /** Every text of the notification, for matching words. */
  val allText: String
    get() = listOf(title, text, bigText, subText, ticker).plus(lines).filter { it.isNotBlank() }
      .distinct()
      .joinToString(" ")

  /** Whether the system or one of its parts posted it. */
  val system: Boolean
    get() =
      category == Notification.CATEGORY_SYSTEM ||
        packageName == "android" ||
        packageName == "com.android.systemui" ||
        packageName == "com.android.settings"

  companion object {
    /** The most of any one text that is kept. Android cuts texts at 5,120 characters itself. */
    const val MAX_TEXT = 5120

    /** Reads [notification], posted by [packageName], whose app is called [appName]. */
    @JvmStatic
    fun from(packageName: String, appName: String, notification: Notification): NotificationInfo {
      val extras = notification.extras ?: Bundle.EMPTY
      fun text(key: String) = extras.getCharSequence(key)?.toString()?.trim()?.take(MAX_TEXT).orEmpty()
      val flags = notification.flags
      @Suppress("DEPRECATION") val priority = notification.priority
      return NotificationInfo(
        packageName = packageName,
        appName = appName,
        category = notification.category,
        title = text(Notification.EXTRA_TITLE).ifEmpty { text(Notification.EXTRA_TITLE_BIG) },
        text = text(Notification.EXTRA_TEXT),
        bigText = text(Notification.EXTRA_BIG_TEXT),
        subText = text(Notification.EXTRA_SUB_TEXT),
        ticker = notification.tickerText?.toString()?.trim()?.take(MAX_TEXT).orEmpty(),
        lines = lines(extras),
        postedAtMillis = notification.`when`,
        ongoing = flags and Notification.FLAG_ONGOING_EVENT != 0 ||
          flags and Notification.FLAG_NO_CLEAR != 0,
        foregroundService = flags and Notification.FLAG_FOREGROUND_SERVICE != 0,
        groupSummary = flags and Notification.FLAG_GROUP_SUMMARY != 0,
        lowPriority = priority < Notification.PRIORITY_DEFAULT,
        media = extras.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
          notification.category == Notification.CATEGORY_TRANSPORT ||
          isSamsungMediaActivity(extras, notification.channelId),
        progress = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) > 0 ||
          extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) ||
          notification.category == Notification.CATEGORY_PROGRESS,
        call = notification.category == Notification.CATEGORY_CALL,
      )
    }

    /**
     * Whether it is the notification One UI's System UI posts while media plays, titled
     * "MediaOngoingActivity", which stands for the media player in the Now Bar.
     */
    private fun isSamsungMediaActivity(extras: Bundle, channelId: String?): Boolean =
      extras.containsKey(SAMSUNG_ONGOING_ACTIVITY_STYLE) &&
        channelId?.contains("media", ignoreCase = true) == true

    /** The extra One UI puts on its ongoing activity notifications, shown in the Now Bar. */
    private const val SAMSUNG_ONGOING_ACTIVITY_STYLE = "android.ongoingActivityNoti.style"

    /** Inbox lines, or the messages of a conversation as "sender: text". */
    private fun lines(extras: Bundle): List<String> {
      val messages = messages(extras)
      if (messages.isNotEmpty()) return messages
      return extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        ?.mapNotNull { it?.toString()?.trim()?.takeIf { line -> line.isNotEmpty() }?.take(MAX_TEXT) }
        .orEmpty()
    }

    @Suppress("DEPRECATION")
    private fun messages(extras: Bundle): List<String> {
      val parcels: Array<Parcelable>? =
        try {
          extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        } catch (e: RuntimeException) {
          null
        }
      if (parcels == null) return emptyList()
      return parcels.mapNotNull { parcel ->
        val bundle = parcel as? Bundle ?: return@mapNotNull null
        val text = bundle.getCharSequence("text")?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return@mapNotNull null
        val sender =
          bundle.getCharSequence("sender")?.toString()?.trim()
            ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
              (bundle.get("sender_person") as? android.app.Person)?.name?.toString()?.trim()
            } else {
              null
            }
        (if (sender.isNullOrEmpty()) text else "$sender: $text").take(MAX_TEXT)
      }.takeLast(MAX_MESSAGES)
    }

    private const val MAX_MESSAGES = 10
  }
}
