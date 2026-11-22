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

package com.google.android.accessibility.talkback.labeling

import android.content.Context
import android.content.Intent
import androidx.annotation.WorkerThread
import com.google.android.accessibility.utils.labeling.Label
import com.google.android.accessibility.utils.labeling.LabelProviderClient
import java.io.IOException
import org.json.JSONException

/**
 * Custom labels in Export settings and Import settings, in the format of the labels file that
 * Custom labels > Export writes.
 */
object LabelBackup {
  /** The most labels read from a settings file. */
  const val MAX_LABELS = 50_000

  /** Returns every custom label as a labels file, or null when there are none. */
  @WorkerThread
  @Throws(IOException::class)
  fun export(context: Context): String? {
    val client = client(context)
    try {
      if (!client.isInitialized) throw IOException("The label database can't be opened")
      val labels = client.currentLabels ?: throw IOException("The labels can't be read")
      if (labels.isEmpty()) return null
      return try {
        CustomLabelMigrationManager.toJson(labels)
      } catch (e: JSONException) {
        throw IOException("The labels can't be written", e)
      }
    } finally {
      client.shutdown()
    }
  }

  /** Reads the labels of the labels file [json]. */
  @Throws(JSONException::class)
  fun parse(json: String): List<Label> {
    val labels = CustomLabelMigrationManager.fromJson(json)
    if (labels.size > MAX_LABELS) throw JSONException("More than $MAX_LABELS labels")
    return labels
  }

  /**
   * Adds [labels], replacing labels for the same items, as importing a labels file with Override
   * does, and has the label cache read again. Returns false if the database can't be opened.
   */
  @WorkerThread
  fun import(context: Context, labels: List<Label>): Boolean {
    if (labels.isEmpty()) return true
    val client = client(context)
    try {
      if (!client.isInitialized) return false
      ImportLabelRequest(client, labels, /* overrideExistingLabels= */ true, null).doInBackground()
    } finally {
      client.shutdown()
    }
    context.sendBroadcast(
      Intent(CustomLabelManager.ACTION_REFRESH_LABEL_CACHE).setPackage(context.packageName)
    )
    return true
  }

  private fun client(context: Context) = LabelProviderClient(context, CustomLabelManager.AUTHORITY)
}
