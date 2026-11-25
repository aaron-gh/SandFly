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

import com.google.android.accessibility.talkback.calls.CallNotices.CALL_TYPE_INCOMING
import com.google.android.accessibility.talkback.calls.CallNotices.CALL_TYPE_ONGOING
import com.google.android.accessibility.talkback.calls.CallNotices.CALL_TYPE_SCREENING
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CallNoticesTest {
  private val own = "io.github.aaron_gh.sandfly"

  private fun incomingCallStyle(key: String = "wa", time: Long = 100) =
    CallNotice(
      key = key,
      packageName = "com.whatsapp",
      postTime = time,
      callType = CALL_TYPE_INCOMING,
      callCategory = true,
      fullScreen = true,
      ongoing = true,
      hasAnswerIntent = true,
      hasDeclineIntent = true,
    )

  private fun ongoingCallStyle(key: String = "wa-ongoing", time: Long = 100) =
    CallNotice(
      key = key,
      packageName = "com.whatsapp",
      postTime = time,
      callType = CALL_TYPE_ONGOING,
      callCategory = true,
      ongoing = true,
      hasHangUpIntent = true,
    )

  @Test
  fun incomingCallStyle_answersWithTheAnswerIntent() {
    assertEquals(
      CallTarget("wa", CallIntent.Answer),
      CallNotices.answerTarget(listOf(incomingCallStyle()), own),
    )
  }

  @Test
  fun incomingCallStyle_endsWithTheDeclineIntent() {
    assertEquals(
      CallTarget("wa", CallIntent.Decline),
      CallNotices.endTarget(listOf(incomingCallStyle()), own),
    )
  }

  @Test
  fun ongoingCallStyle_endsWithTheHangUpIntent_andIsNotAnswered() {
    val notices = listOf(ongoingCallStyle())
    assertEquals(CallTarget("wa-ongoing", CallIntent.HangUp), CallNotices.endTarget(notices, own))
    assertNull(CallNotices.answerTarget(notices, own))
  }

  @Test
  fun screeningCall_answersAndHangsUp() {
    val screening =
      CallNotice(
        key = "s",
        packageName = "com.google.android.dialer",
        postTime = 1,
        callType = CALL_TYPE_SCREENING,
        hasAnswerIntent = true,
        hasHangUpIntent = true,
      )
    assertEquals(CallTarget("s", CallIntent.Answer), CallNotices.answerTarget(listOf(screening), own))
    assertEquals(CallTarget("s", CallIntent.HangUp), CallNotices.endTarget(listOf(screening), own))
  }

  @Test
  fun callCategoryWithoutCallStyle_usesTheActionTitles() {
    val notice =
      CallNotice(
        key = "t",
        packageName = "org.telegram.messenger",
        postTime = 1,
        callCategory = true,
        ongoing = true,
        actionTitles = listOf("Decline", "Answer"),
      )
    assertEquals(CallTarget("t", CallIntent.Action(1)), CallNotices.answerTarget(listOf(notice), own))
    assertEquals(CallTarget("t", CallIntent.Action(0)), CallNotices.endTarget(listOf(notice), own))
  }

  @Test
  fun untitledActionsOfARingingCall_answerLastAndDeclineFirst() {
    val notice =
      CallNotice(
        key = "x",
        packageName = "com.example.voip",
        postTime = 1,
        callCategory = true,
        fullScreen = true,
        actionTitles = listOf("Ablehnen", "Annehmen"),
      )
    assertEquals(CallTarget("x", CallIntent.Action(1)), CallNotices.answerTarget(listOf(notice), own))
    assertEquals(CallTarget("x", CallIntent.Action(0)), CallNotices.endTarget(listOf(notice), own))
  }

  @Test
  fun ongoingCallWithoutCallStyle_hangsUpByTitle() {
    val notice =
      CallNotice(
        key = "o",
        packageName = "com.example.voip",
        postTime = 1,
        callCategory = true,
        ongoing = true,
        actionTitles = listOf("Mute", "Speaker", "Hang up"),
      )
    assertNull(CallNotices.answerTarget(listOf(notice), own))
    assertEquals(CallTarget("o", CallIntent.Action(2)), CallNotices.endTarget(listOf(notice), own))
  }

  @Test
  fun ringingCallWithOneUntitledAction_isNotAnswered() {
    val notice =
      CallNotice(
        key = "one",
        packageName = "com.example.voip",
        postTime = 1,
        callCategory = true,
        fullScreen = true,
        actionTitles = listOf("Ablehnen"),
      )
    assertNull(CallNotices.answerTarget(listOf(notice), own))
    assertNull(CallNotices.endTarget(listOf(notice), own))
  }

  @Test
  fun notificationsThatAreNotCalls_areLeftOut() {
    val message =
      CallNotice(
        key = "m",
        packageName = "com.whatsapp",
        postTime = 1,
        fullScreen = true,
        ongoing = true,
        actionTitles = listOf("Reply", "Mark as read"),
      )
    assertNull(CallNotices.answerTarget(listOf(message), own))
    assertNull(CallNotices.endTarget(listOf(message), own))
  }

  @Test
  fun sandFlysOwnNotifications_areLeftOut() {
    val notice = incomingCallStyle().copy(packageName = own)
    assertNull(CallNotices.answerTarget(listOf(notice), own))
    assertNull(CallNotices.endTarget(listOf(notice), own))
  }

  @Test
  fun newestRingingCall_isChosen() {
    val notices = listOf(incomingCallStyle("old", 100), incomingCallStyle("new", 200))
    assertEquals(CallTarget("new", CallIntent.Answer), CallNotices.answerTarget(notices, own))
  }

  @Test
  fun endCall_declinesARingingCallBeforeHangingUpOne() {
    val notices = listOf(incomingCallStyle("ringing", 100), ongoingCallStyle("ongoing", 200))
    assertEquals(CallTarget("ringing", CallIntent.Decline), CallNotices.endTarget(notices, own))
  }

  @Test
  fun answerCall_skipsACallInProgressForTheRingingOne() {
    val notices = listOf(ongoingCallStyle("ongoing", 200), incomingCallStyle("ringing", 100))
    assertEquals(CallTarget("ringing", CallIntent.Answer), CallNotices.answerTarget(notices, own))
  }
}
