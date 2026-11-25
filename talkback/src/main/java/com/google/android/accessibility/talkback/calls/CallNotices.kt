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

import java.util.Locale

/**
 * A notification that may be an app's call, reduced to what the Answer call and End call commands
 * choose by.
 */
data class CallNotice(
  val key: String,
  val packageName: String,
  /** When the notification was posted or last updated, in milliseconds. */
  val postTime: Long,
  /** The call type of a call style notification, or [CallNotices.CALL_TYPE_UNKNOWN]. */
  val callType: Int = CallNotices.CALL_TYPE_UNKNOWN,
  /** Whether its category is Notification.CATEGORY_CALL. */
  val callCategory: Boolean = false,
  /** Whether it has a full-screen intent, as a ringing call's notification does. */
  val fullScreen: Boolean = false,
  /** Whether it is an ongoing notification. */
  val ongoing: Boolean = false,
  val hasAnswerIntent: Boolean = false,
  val hasDeclineIntent: Boolean = false,
  val hasHangUpIntent: Boolean = false,
  /** The titles of its actions, in order. */
  val actionTitles: List<String> = emptyList(),
)

/** What to send from a call notification. */
sealed interface CallIntent {
  /** Notification.EXTRA_ANSWER_INTENT. */
  data object Answer : CallIntent

  /** Notification.EXTRA_DECLINE_INTENT. */
  data object Decline : CallIntent

  /** Notification.EXTRA_HANG_UP_INTENT. */
  data object HangUp : CallIntent

  /** The notification action at [index]. */
  data class Action(val index: Int) : CallIntent
}

/** The notification with [key], and what to send from it. */
data class CallTarget(val key: String, val intent: CallIntent)

/** Chooses the call notification and its intent for the Answer call and End call commands. */
object CallNotices {
  /** Notification.CallStyle.CALL_TYPE_UNKNOWN and the other call types. */
  const val CALL_TYPE_UNKNOWN = 0
  const val CALL_TYPE_INCOMING = 1
  const val CALL_TYPE_ONGOING = 2
  const val CALL_TYPE_SCREENING = 3

  private val ANSWER_TITLES = listOf("answer", "accept", "pick up")
  private val DECLINE_TITLES = listOf("decline", "reject")
  private val HANG_UP_TITLES = listOf("hang up", "hangup", "end call", "end", "leave")

  /** Whether [notice] is a call: a call style notification, or one in the call category. */
  @JvmStatic
  fun isCall(notice: CallNotice): Boolean =
    notice.callType != CALL_TYPE_UNKNOWN || notice.callCategory

  /** Whether [notice] is a call that rings, or one being screened. */
  @JvmStatic
  fun isIncoming(notice: CallNotice): Boolean =
    when (notice.callType) {
      CALL_TYPE_INCOMING,
      CALL_TYPE_SCREENING -> true
      CALL_TYPE_ONGOING -> false
      else ->
        notice.callCategory &&
          (notice.fullScreen || titleIndex(notice.actionTitles, ANSWER_TITLES) != null)
    }

  /** Whether [notice] is a call in progress. */
  @JvmStatic
  fun isOngoing(notice: CallNotice): Boolean =
    when (notice.callType) {
      CALL_TYPE_ONGOING -> true
      CALL_TYPE_INCOMING,
      CALL_TYPE_SCREENING -> false
      else -> notice.callCategory && notice.ongoing && !isIncoming(notice)
    }

  /**
   * The intent that answers the incoming call of [notice]: the call style's answer intent, else an
   * action titled as an answer, else the last of two or more actions, where call notifications put
   * it.
   */
  @JvmStatic
  fun answerIntent(notice: CallNotice): CallIntent? {
    if (!isIncoming(notice)) return null
    if (notice.hasAnswerIntent) return CallIntent.Answer
    titleIndex(notice.actionTitles, ANSWER_TITLES)?.let {
      return CallIntent.Action(it)
    }
    return if (notice.actionTitles.size >= 2) {
      CallIntent.Action(notice.actionTitles.size - 1)
    } else null
  }

  /**
   * The intent that ends the call of [notice]: for an incoming call, the decline or hang-up intent,
   * else an action titled so, else the first of two or more actions; for a call in progress, the
   * hang-up intent, else an action titled so, else the first action.
   */
  @JvmStatic
  fun endIntent(notice: CallNotice): CallIntent? {
    val titles = notice.actionTitles
    return when {
      isIncoming(notice) ->
        when {
          notice.hasDeclineIntent -> CallIntent.Decline
          notice.hasHangUpIntent -> CallIntent.HangUp
          else ->
            (titleIndex(titles, DECLINE_TITLES) ?: titleIndex(titles, HANG_UP_TITLES))?.let {
              CallIntent.Action(it)
            } ?: if (titles.size >= 2) CallIntent.Action(0) else null
        }
      isOngoing(notice) ->
        when {
          notice.hasHangUpIntent -> CallIntent.HangUp
          else ->
            titleIndex(titles, HANG_UP_TITLES)?.let { CallIntent.Action(it) }
              ?: if (titles.isNotEmpty()) CallIntent.Action(0) else null
        }
      else -> null
    }
  }

  /**
   * The newest incoming call among [notices] that can be answered, leaving out those of
   * [ownPackage].
   */
  @JvmStatic
  fun answerTarget(notices: List<CallNotice>, ownPackage: String): CallTarget? =
    newest(notices, ownPackage) { if (isIncoming(it)) answerIntent(it) else null }

  /**
   * The call among [notices] to end, leaving out those of [ownPackage]: the newest incoming call,
   * as Telecom ends a ringing call first, else the newest call in progress.
   */
  @JvmStatic
  fun endTarget(notices: List<CallNotice>, ownPackage: String): CallTarget? =
    newest(notices, ownPackage) { if (isIncoming(it)) endIntent(it) else null }
      ?: newest(notices, ownPackage) { if (isOngoing(it)) endIntent(it) else null }

  private fun newest(
    notices: List<CallNotice>,
    ownPackage: String,
    intentOf: (CallNotice) -> CallIntent?,
  ): CallTarget? {
    var best: CallNotice? = null
    var bestIntent: CallIntent? = null
    for (notice in notices) {
      if (notice.packageName == ownPackage || !isCall(notice)) continue
      val intent = intentOf(notice) ?: continue
      if (best == null || notice.postTime > best.postTime) {
        best = notice
        bestIntent = intent
      }
    }
    return if (best != null && bestIntent != null) CallTarget(best.key, bestIntent) else null
  }

  /**
   * The index of the first title that is one of [words], or starts with one followed by a space,
   * ignoring case.
   */
  private fun titleIndex(titles: List<String>, words: List<String>): Int? {
    titles.forEachIndexed { index, title ->
      val text = title.trim().lowercase(Locale.ROOT)
      if (words.any { text == it || text.startsWith("$it ") }) return index
    }
    return null
  }
}
