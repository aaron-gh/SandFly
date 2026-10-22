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

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.File
import java.io.IOException
import java.util.UUID
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** A report being written, kept until it is sent or deleted. */
data class ReportDraft(
  val id: String = UUID.randomUUID().toString(),
  val type: String = ProblemReport.DEFAULT_TYPE,
  val area: String? = null,
  val title: String = "",
  val app: String = "",
  val description: String = "",
  val steps: String = "",
  val updatedAt: Long = System.currentTimeMillis(),
) {
  val isEmpty: Boolean
    get() = title.isBlank() && app.isBlank() && description.isBlank() && steps.isBlank()
}

/** A message about a sent report, from the developer or from the reporter. */
data class ReportMessage(val id: Long, val fromDeveloper: Boolean, val body: String, val at: String)

/** A sent report, with the conversation about it. */
data class SentReport(
  val id: String,
  /** The secret the report server knows the report by. */
  val token: String,
  val type: String,
  val title: String,
  val description: String,
  val sentAt: Long,
  val status: String = STATUS_OPEN,
  val messages: List<ReportMessage> = emptyList(),
  /** When the status last changed, or the report was sent. */
  val statusChangedAt: Long = sentAt,
  /** The newest developer message that the reporter has opened. */
  val readMessageId: Long = 0,
  /** The status that the reporter has opened. */
  val readStatus: String = STATUS_OPEN,
  /** The newest developer message and the status that a notification was posted for. */
  val notifiedMessageId: Long = 0,
  val notifiedStatus: String = STATUS_OPEN,
  /** A reply being written. */
  val replyDraft: String = "",
) {
  val unreadMessages: Int
    get() = messages.count { it.fromDeveloper && it.id > readMessageId }

  val statusUnread: Boolean
    get() = status != readStatus

  val isClosed: Boolean
    get() = status != STATUS_OPEN && status != STATUS_UNKNOWN

  /** Whether replies and status changes are still checked for. */
  fun isFollowed(now: Long): Boolean =
    status != STATUS_UNKNOWN && (!isClosed || now - statusChangedAt < FOLLOW_CLOSED_MS)

  companion object {
    const val STATUS_OPEN = "open"
    const val STATUS_COMPLETED = "completed"
    const val STATUS_NOT_PLANNED = "not_planned"
    const val STATUS_DUPLICATE = "duplicate"
    /** The report server no longer knows the report. */
    const val STATUS_UNKNOWN = "unknown"

    /** How long a closed report is still checked, in case it is reopened or answered. */
    const val FOLLOW_CLOSED_MS = 30L * 24 * 60 * 60 * 1000
  }
}

/**
 * Keeps drafts and sent reports in a file in device-protected storage, so that the service can
 * check for replies before the phone is first unlocked.
 */
class ReportStore private constructor(private val file: File) {
  private var drafts = mutableListOf<ReportDraft>()
  private var sent = mutableListOf<SentReport>()
  private val listeners = mutableListOf<() -> Unit>()

  init {
    load()
  }

  /** An ID for a new draft, which is saved once something is typed in it. */
  fun newDraftId(): String = UUID.randomUUID().toString()

  @Synchronized fun drafts(): List<ReportDraft> = drafts.sortedByDescending { it.updatedAt }

  @Synchronized fun draft(id: String): ReportDraft? = drafts.firstOrNull { it.id == id }

  @Synchronized fun sentReports(): List<SentReport> = sent.sortedByDescending { it.sentAt }

  @Synchronized fun sentReport(id: String): SentReport? = sent.firstOrNull { it.id == id }

  fun putDraft(draft: ReportDraft) = change {
    drafts.removeAll { it.id == draft.id }
    drafts += draft.copy(updatedAt = System.currentTimeMillis())
  }

  fun deleteDraft(id: String) = change { drafts.removeAll { it.id == id } }

  /** Moves the draft with [draftId] to the sent reports as [report]. */
  fun markSent(draftId: String, report: SentReport) = change {
    drafts.removeAll { it.id == draftId }
    sent += report
  }

  /** Changes the sent report with [id] with [update], if it is there. */
  fun updateSent(id: String, update: (SentReport) -> SentReport) = change {
    val index = sent.indexOfFirst { it.id == id }
    if (index >= 0) sent[index] = update(sent[index])
  }

  fun deleteSent(id: String) = change { sent.removeAll { it.id == id } }

  /** Calls [listener] on the thread that changed the store, after each change. */
  @Synchronized fun addListener(listener: () -> Unit) {
    listeners += listener
  }

  @Synchronized fun removeListener(listener: () -> Unit) {
    listeners -= listener
  }

  private fun change(block: () -> Unit) {
    val toCall: List<() -> Unit>
    synchronized(this) {
      block()
      save()
      toCall = listeners.toList()
    }
    toCall.forEach { it() }
  }

  private fun load() {
    if (!file.exists()) return
    try {
      val root = JSONObject(file.readText())
      drafts = parseDrafts(root.optJSONArray("drafts")).toMutableList()
      sent = parseSent(root.optJSONArray("sent")).toMutableList()
    } catch (e: IOException) {
      LogUtils.e(TAG, "Cannot read the reports: %s", e)
    } catch (e: JSONException) {
      LogUtils.e(TAG, "Cannot read the reports: %s", e)
    }
  }

  private fun save() {
    val root = JSONObject().put("drafts", draftsJson(drafts)).put("sent", sentJson(sent))
    val temporary = File(file.parentFile, "${file.name}.tmp")
    try {
      temporary.writeText(root.toString())
      if (!temporary.renameTo(file)) throw IOException("Cannot replace ${file.name}")
    } catch (e: IOException) {
      LogUtils.e(TAG, "Cannot save the reports: %s", e)
    }
  }

  companion object {
    private const val TAG = "ReportStore"
    private const val FILE_NAME = "problem_reports.json"

    @Volatile private var instance: ReportStore? = null

    @JvmStatic
    fun get(context: Context): ReportStore =
      instance
        ?: synchronized(this) {
          instance
            ?: ReportStore(
                File(context.applicationContext.createDeviceProtectedStorageContext().filesDir, FILE_NAME)
              )
              .also { instance = it }
        }

    @VisibleForTesting fun forFile(file: File): ReportStore = ReportStore(file)

    private fun draftsJson(drafts: List<ReportDraft>) =
      JSONArray(
        drafts.map {
          JSONObject()
            .put("id", it.id)
            .put("type", it.type)
            .put("area", it.area ?: JSONObject.NULL)
            .put("title", it.title)
            .put("app", it.app)
            .put("description", it.description)
            .put("steps", it.steps)
            .put("updatedAt", it.updatedAt)
        }
      )

    private fun parseDrafts(array: JSONArray?): List<ReportDraft> =
      objects(array).map {
        ReportDraft(
          id = it.getString("id"),
          type = it.optString("type", ProblemReport.DEFAULT_TYPE),
          area = if (it.isNull("area")) null else it.optString("area"),
          title = it.optString("title"),
          app = it.optString("app"),
          description = it.optString("description"),
          steps = it.optString("steps"),
          updatedAt = it.optLong("updatedAt"),
        )
      }

    private fun sentJson(reports: List<SentReport>) =
      JSONArray(
        reports.map { report ->
          JSONObject()
            .put("id", report.id)
            .put("token", report.token)
            .put("type", report.type)
            .put("title", report.title)
            .put("description", report.description)
            .put("sentAt", report.sentAt)
            .put("status", report.status)
            .put("statusChangedAt", report.statusChangedAt)
            .put("readMessageId", report.readMessageId)
            .put("readStatus", report.readStatus)
            .put("notifiedMessageId", report.notifiedMessageId)
            .put("notifiedStatus", report.notifiedStatus)
            .put("replyDraft", report.replyDraft)
            .put(
              "messages",
              JSONArray(
                report.messages.map {
                  JSONObject()
                    .put("id", it.id)
                    .put("fromDeveloper", it.fromDeveloper)
                    .put("body", it.body)
                    .put("at", it.at)
                }
              ),
            )
        }
      )

    private fun parseSent(array: JSONArray?): List<SentReport> =
      objects(array).map {
        SentReport(
          id = it.getString("id"),
          token = it.getString("token"),
          type = it.optString("type"),
          title = it.optString("title"),
          description = it.optString("description"),
          sentAt = it.optLong("sentAt"),
          status = it.optString("status", SentReport.STATUS_OPEN),
          statusChangedAt = it.optLong("statusChangedAt"),
          readMessageId = it.optLong("readMessageId"),
          readStatus = it.optString("readStatus", SentReport.STATUS_OPEN),
          notifiedMessageId = it.optLong("notifiedMessageId"),
          notifiedStatus = it.optString("notifiedStatus", SentReport.STATUS_OPEN),
          replyDraft = it.optString("replyDraft"),
          messages =
            objects(it.optJSONArray("messages")).map { message ->
              ReportMessage(
                id = message.getLong("id"),
                fromDeveloper = message.getBoolean("fromDeveloper"),
                body = message.optString("body"),
                at = message.optString("at"),
              )
            },
        )
      }

    private fun objects(array: JSONArray?): List<JSONObject> =
      if (array == null) emptyList()
      else (0 until array.length()).mapNotNull { array.optJSONObject(it) }
  }
}
