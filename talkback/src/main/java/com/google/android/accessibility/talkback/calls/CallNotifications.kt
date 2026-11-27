/*
 * Copyright 2026 SandFly contributors
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

package com.google.android.accessibility.talkback.calls

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.os.SystemClock
import android.util.Log
import androidx.annotation.MainThread
import com.google.android.accessibility.talkback.notificationlist.NotificationListService
import com.google.android.accessibility.talkback.notificationlist.NotificationReplyActivity
import java.util.function.Consumer

/**
 * Answers and ends the calls of apps, such as WhatsApp, through their call notifications: the
 * notifications in the shade while the notification listener is connected, or else those of
 * ringing calls from recent accessibility events. With notification access, a command that finds
 * no call connects the listener for a short time and looks again.
 */
object CallNotifications {
  private const val TAG = "CallNotifications"

  // Notification.EXTRA_CALL_TYPE and the call intents, also set by NotificationCompat.CallStyle
  // before Android 12.
  private const val EXTRA_CALL_TYPE = "android.callType"
  private const val EXTRA_ANSWER_INTENT = "android.answerIntent"
  private const val EXTRA_DECLINE_INTENT = "android.declineIntent"
  private const val EXTRA_HANG_UP_INTENT = "android.hangUpIntent"

  /** How long a ringing call's notification from an accessibility event is used for. */
  private const val EVENT_RINGING_MS = 60_000L
  private const val MAX_EVENTS = 8
  /**
   * How long a command keeps the notification listener connected. Short: while it is connected and
   * the device is locked, notifications are read from it rather than from their events.
   */
  private const val KEEP_CONNECTED_MS = 15_000L
  /** How long a command waits for the notification listener to connect. */
  private const val CONNECT_WAIT_MS = 2_000L
  private const val CONNECT_POLL_MS = 200L

  enum class Sent {
    SENT,
    /** No call notification has the intent. */
    NONE,
    /** The app cancelled the intent. */
    FAILED,
  }

  private class Seen(val packageName: String, val notification: Notification, val uptime: Long)

  /** Call notifications from accessibility events, by package, oldest first. Main thread only. */
  private val seen = LinkedHashMap<String, Seen>()

  private val handler = Handler(Looper.getMainLooper())

  private class Candidate(val notice: CallNotice, val notification: Notification)

  /** Notes a notification from an accessibility event, and keeps it for the commands if a call. */
  @JvmStatic
  @MainThread
  fun onNotificationEvent(context: Context, packageName: CharSequence?, notification: Notification) {
    val name = packageName?.toString() ?: return
    if (name == context.packageName) return
    noticeOf(name, name, System.currentTimeMillis(), notification) ?: return
    seen.remove(name)
    seen[name] = Seen(name, notification, SystemClock.uptimeMillis())
    while (seen.size > MAX_EVENTS) seen.remove(seen.keys.first())
  }

  /** Answers the newest ringing call of an app, and passes the result to [done]. */
  @JvmStatic
  @MainThread
  fun answer(context: Context, done: Consumer<Sent>) {
    attempt(context, done) {
      CallNotices.answerTarget(it.map { candidate -> candidate.notice }, context.packageName)
    }
  }

  /**
   * Declines the newest ringing call of an app, or else hangs up its newest call in progress, and
   * passes the result to [done].
   */
  @JvmStatic
  @MainThread
  fun end(context: Context, done: Consumer<Sent>) {
    attempt(context, done) {
      CallNotices.endTarget(it.map { candidate -> candidate.notice }, context.packageName)
    }
  }

  /**
   * Sends the intent [choose] picks, and passes the result to [done]. When there is none, the
   * listener isn't connected and SandFly has notification access, connects the listener and tries
   * again once it has connected.
   */
  private fun attempt(
    context: Context,
    done: Consumer<Sent>,
    choose: (List<Candidate>) -> CallTarget?,
  ) {
    val sent = sendChosen(context, choose)
    if (
      sent != Sent.NONE ||
        NotificationListService.instance != null ||
        !NotificationListService.isAccessGranted(context)
    ) {
      done.accept(sent)
      return
    }
    whenConnected(context) { done.accept(sendChosen(context, choose)) }
  }

  private fun sendChosen(context: Context, choose: (List<Candidate>) -> CallTarget?): Sent {
    val candidates = candidates()
    val target = choose(candidates) ?: return Sent.NONE
    return send(context, candidates, target)
  }

  /**
   * Connects the notification listener and runs [then] once it is connected, or after
   * [CONNECT_WAIT_MS] at most.
   */
  private fun whenConnected(context: Context, then: Runnable) {
    NotificationListService.keepConnected(context, KEEP_CONNECTED_MS)
    val deadline = SystemClock.uptimeMillis() + CONNECT_WAIT_MS
    handler.postDelayed(
      object : Runnable {
        override fun run() {
          if (
            NotificationListService.instance != null || SystemClock.uptimeMillis() >= deadline
          ) {
            then.run()
          } else {
            handler.postDelayed(this, CONNECT_POLL_MS)
          }
        }
      },
      CONNECT_POLL_MS,
    )
  }

  /**
   * The newest notification of an incoming call of another app in the shade, or null when there is
   * none or the notification listener isn't connected.
   */
  @JvmStatic
  @MainThread
  fun incomingCallInShade(context: Context): Notification? =
    shadeCandidates()
      ?.filter { it.notice.packageName != context.packageName && CallNotices.isIncoming(it.notice) }
      ?.maxByOrNull { it.notice.postTime }
      ?.notification

  /** Whether [notification], from an accessibility event of [packageName], is an incoming call. */
  @JvmStatic
  fun isIncomingCall(packageName: CharSequence?, notification: Notification): Boolean {
    val name = packageName?.toString() ?: return false
    val notice = noticeOf(name, name, System.currentTimeMillis(), notification) ?: return false
    return CallNotices.isIncoming(notice)
  }

  /**
   * The call notifications in the shade, or without the notification listener, those of calls that
   * rang in the last [EVENT_RINGING_MS].
   */
  private fun candidates(): List<Candidate> {
    shadeCandidates()?.let {
      return it
    }
    val now = SystemClock.uptimeMillis()
    seen.values.removeAll { now - it.uptime >= EVENT_RINGING_MS }
    return seen.values.mapNotNull { event ->
      noticeOf(
          event.packageName,
          event.packageName,
          System.currentTimeMillis() - (now - event.uptime),
          event.notification,
        )
        ?.takeIf { CallNotices.isIncoming(it) }
        ?.let { Candidate(it, event.notification) }
    }
  }

  /**
   * The call notifications in the shade, or null when the notification listener isn't connected or
   * can't read them.
   */
  private fun shadeCandidates(): List<Candidate>? {
    val listener = NotificationListService.instance ?: return null
    val active =
      try {
        listener.activeNotifications
      } catch (e: RuntimeException) {
        // SecurityException while the listener disconnects, or a notification that can't be read.
        Log.w(TAG, "Can't read the notifications", e)
        null
      } ?: return null
    return active.mapNotNull { posted ->
      val notification = posted.notification ?: return@mapNotNull null
      noticeOf(posted.key, posted.packageName, posted.postTime, notification)?.let {
        Candidate(it, notification)
      }
    }
  }

  private fun send(context: Context, candidates: List<Candidate>, target: CallTarget): Sent {
    val notification =
      candidates.firstOrNull { it.notice.key == target.key }?.notification ?: return Sent.NONE
    val found: PendingIntent? =
      try {
        when (val chosen = target.intent) {
          CallIntent.Answer -> notification.extras?.let { pendingIntent(it, EXTRA_ANSWER_INTENT) }
          CallIntent.Decline -> notification.extras?.let { pendingIntent(it, EXTRA_DECLINE_INTENT) }
          CallIntent.HangUp -> notification.extras?.let { pendingIntent(it, EXTRA_HANG_UP_INTENT) }
          is CallIntent.Action -> notification.actions?.getOrNull(chosen.index)?.actionIntent
        }
      } catch (e: RuntimeException) {
        Log.w(TAG, "Can't read the call intent", e)
        null
      }
    val intent = found ?: return Sent.NONE
    // A call that rang is answered or declined once.
    seen.remove(target.key)
    return try {
      intent.send(context, 0, null, null, null, null, NotificationReplyActivity.startOptions())
      Log.i(TAG, "Sent ${target.intent} of a call of ${intent.creatorPackage}")
      Sent.SENT
    } catch (e: PendingIntent.CanceledException) {
      Log.w(TAG, "Call intent cancelled", e)
      Sent.FAILED
    }
  }

  /** The [CallNotice] of [notification], or null when it isn't a call or can't be read. */
  private fun noticeOf(
    key: String,
    packageName: String,
    postTime: Long,
    notification: Notification,
  ): CallNotice? {
    val callCategory = notification.category == Notification.CATEGORY_CALL
    return try {
      val extras: Bundle? = notification.extras
      val callType =
        extras?.getInt(EXTRA_CALL_TYPE, CallNotices.CALL_TYPE_UNKNOWN)
          ?: CallNotices.CALL_TYPE_UNKNOWN
      if (callType == CallNotices.CALL_TYPE_UNKNOWN && !callCategory) {
        null
      } else CallNotice(
        key = key,
        packageName = packageName,
        postTime = postTime,
        callType = callType,
        callCategory = callCategory,
        fullScreen = notification.fullScreenIntent != null,
        ongoing = notification.flags and Notification.FLAG_ONGOING_EVENT != 0,
        hasAnswerIntent = extras != null && pendingIntent(extras, EXTRA_ANSWER_INTENT) != null,
        hasDeclineIntent = extras != null && pendingIntent(extras, EXTRA_DECLINE_INTENT) != null,
        hasHangUpIntent = extras != null && pendingIntent(extras, EXTRA_HANG_UP_INTENT) != null,
        actionTitles = notification.actions?.map { it.title?.toString().orEmpty() }.orEmpty(),
      )
    } catch (e: RuntimeException) {
      // BadParcelableException from an app's extras.
      Log.w(TAG, "Can't read a notification of $packageName", e)
      null
    }
  }

  @Suppress("DEPRECATION") // getParcelable(String, Class) needs Android 13.
  private fun pendingIntent(extras: Bundle, key: String): PendingIntent? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      extras.getParcelable(key, PendingIntent::class.java)
    } else {
      extras.getParcelable<Parcelable>(key) as? PendingIntent
    }
}
