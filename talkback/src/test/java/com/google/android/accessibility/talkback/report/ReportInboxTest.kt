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

package com.google.android.accessibility.talkback.report

import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReportInboxTest {
  private val report =
    SentReport(
      id = "r1",
      token = "t".repeat(43),
      type = "Crash",
      title = "Crash on open",
      description = "It crashes",
      sentAt = 1000,
    )
  private val reply = ReportMessage(5, fromDeveloper = true, body = "Try build 150", at = "x")
  private val own = ReportMessage(6, fromDeveloper = false, body = "Still crashes", at = "y")

  private fun thread(status: String, vararg messages: ReportMessage) =
    ProblemReportSender.ReportThread(report.token, status, messages.toList())

  @Test
  fun aNewReplyIsNewsAndUnread() {
    val merged = ReportInbox.merge(report, thread("open", reply, own), now = 2000)
    assertEquals("Try build 150", ReportInbox.newsOf(merged)?.reply)
    assertNull(ReportInbox.newsOf(merged)?.status)
    assertEquals(1, merged.unreadMessages)
    assertFalse(merged.statusUnread)
  }

  @Test
  fun theReportersOwnMessageIsNotNews() {
    val merged = ReportInbox.merge(report, thread("open", own), now = 2000)
    assertNull(ReportInbox.newsOf(merged))
  }

  @Test
  fun aStatusChangeIsNewsOnce() {
    val merged = ReportInbox.merge(report, thread("completed"), now = 2000)
    assertEquals("completed", ReportInbox.newsOf(merged)?.status)
    assertEquals(2000, merged.statusChangedAt)
    assertTrue(merged.statusUnread)
    val notified = merged.copy(notifiedStatus = "completed")
    assertNull(ReportInbox.newsOf(notified))
  }

  @Test
  fun aClosedReportIsFollowedForThirtyDays() {
    val closed = report.copy(status = "completed", statusChangedAt = 0)
    assertTrue(closed.isFollowed(SentReport.FOLLOW_CLOSED_MS - 1))
    assertFalse(closed.isFollowed(SentReport.FOLLOW_CLOSED_MS))
    assertTrue(report.isFollowed(Long.MAX_VALUE))
    assertFalse(report.copy(status = "unknown").isFollowed(0))
  }

  @Test
  fun threadsAreRead() {
    val threads =
      ProblemReportSender.parseThreads(
        JSONObject(
          """{"threads":[{"token":"abc","status":"not_planned","messages":[""" +
            """{"id":7,"from":"developer","body":"Works as intended","at":"2026-10-22T10:00:00Z"},""" +
            """{"id":8,"from":"reporter","body":"OK","at":"2026-10-22T11:00:00Z"}]}]}"""
        )
      )
    assertEquals("not_planned", threads[0].status)
    assertTrue(threads[0].messages[0].fromDeveloper)
    assertFalse(threads[0].messages[1].fromDeveloper)
  }

  @Test
  fun theStoreKeepsDraftsAndReports() {
    val file = File.createTempFile("reports", ".json").apply { delete() }
    val store = ReportStore.forFile(file)
    store.putDraft(ReportDraft(id = "d1", title = "Half written"))
    store.putDraft(ReportDraft(id = "d2", title = "Sent soon"))
    store.markSent("d2", report.copy(id = "d2", messages = listOf(reply)))

    val again = ReportStore.forFile(file)
    assertEquals(listOf("d1"), again.drafts().map { it.id })
    assertEquals("Half written", again.draft("d1")?.title)
    assertEquals(listOf(reply), again.sentReport("d2")?.messages)
    again.updateSent("d2") { it.copy(readMessageId = 5) }
    assertEquals(0, ReportStore.forFile(file).sentReport("d2")?.unreadMessages)
    file.delete()
  }
}
