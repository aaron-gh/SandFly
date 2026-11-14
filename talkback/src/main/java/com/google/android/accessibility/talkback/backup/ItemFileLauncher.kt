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

package com.google.android.accessibility.talkback.backup

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.utils.FormFactorUtils
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Export to file and Import from file on a screen of items: writes a file that [ItemFiles] made,
 * and imports one, then calls [onImported] on the main thread. Make it while [fragment] is created,
 * as its file pickers are registered then.
 */
class ItemFileLauncher(private val fragment: Fragment, private val onImported: () -> Unit) {

  private val create =
    fragment.registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
      val build = pending
      pending = null
      if (uri == null) return@registerForActivityResult
      if (build != null) {
        exportText(fragment.requireContext(), uri, build)
      } else {
        discard(fragment.requireContext(), uri)
      }
    }

  private val open =
    fragment.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      uri?.let { read(fragment.requireContext().applicationContext, it) }
    }

  /** Asks where to save a file named [name] and writes it with [build], off the main thread. */
  fun export(name: String, build: (Context) -> String) {
    pending = build
    create.launch(fileName(name))
  }

  /** Asks for a file and imports its items. */
  fun importFile() {
    open.launch(MIME_TYPES)
  }

  private fun read(context: Context, uri: Uri) {
    executor.execute {
      val result =
        try {
          readText(context, uri, ItemFiles.MAX_BYTES)?.let { ItemFiles.import(context, it) }
            ?: ItemFiles.Result.NotRead
        } catch (e: Throwable) {
          ItemFiles.Result.NotRead
        }
      val text = ItemFiles.describe(context, result)
      mainHandler.post {
        if (result is ItemFiles.Result.Imported && result.keyboardChanged) {
          // The keyboard shortcuts are read once, when the key combo model is built.
          TalkBackService.getInstance()?.keyComboManager?.refreshKeyComboModel()
        }
        val activity = fragment.activity
        if (result is ItemFiles.Result.Imported && result.problems.isNotEmpty() && fragment.isAdded && activity != null) {
          AlertDialog.Builder(activity)
            .setTitle(R.string.item_file_import)
            .setMessage(text)
            .setPositiveButton(android.R.string.ok, null)
            .show()
        } else {
          Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        }
        if (fragment.isAdded) onImported()
      }
    }
  }

  companion object {
    /**
     * What the file being created is written with, from the last export. Kept here rather than on
     * the screen, which is made again while the file picker is open when the device is rotated or
     * folded; only one file picker is open at a time. Gone if SandFly's process was stopped.
     */
    @Volatile private var pending: ((Context) -> String)? = null

    /** Watches have no file picker. */
    @JvmStatic
    val isAvailable: Boolean
      get() = !FormFactorUtils.isAndroidWear()

    private val MIME_TYPES = arrayOf("application/json", "text/*", "application/octet-stream")

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    /** Files can be on a cloud provider, which downloads or uploads before the stream returns. */
    private val executor = ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>())

    /** [name] as a file name, without characters that file systems refuse. */
    private fun fileName(name: String): String {
      val safe = name.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), " ").trim().take(80).ifEmpty { "SandFly" }
      return "$safe.json"
    }

    /** Reads the text of [uri], or null when it is larger than [max] bytes or can't be opened. */
    private fun readText(context: Context, uri: Uri, max: Int): String? =
      try {
        context.contentResolver.openInputStream(uri)?.use { input ->
          val buffer = ByteArray(max + 1)
          var length = 0
          while (length < buffer.size) {
            val count = input.read(buffer, length, buffer.size - length)
            if (count < 0) break
            length += count
          }
          if (length > max) null else buffer.decodeToString(0, length)
        }
      } catch (e: IOException) {
        null
      } catch (e: SecurityException) {
        null
      }

    /** Writes [text] to [uri] off the main thread, and says whether it was written. */
    @JvmStatic
    fun exportText(context: Context, uri: Uri, build: (Context) -> String) {
      val app = context.applicationContext
      executor.execute {
        val written =
          try {
            val text = build(app)
            app.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) } != null
          } catch (e: Throwable) {
            false
          }
        toast(app, app.getString(if (written) R.string.item_file_exported else R.string.item_file_export_failed))
      }
    }

    /**
     * Deletes the empty file created for an export whose contents were lost, and says that the
     * export failed.
     */
    private fun discard(context: Context, uri: Uri) {
      val app = context.applicationContext
      executor.execute {
        try {
          DocumentsContract.deleteDocument(app.contentResolver, uri)
        } catch (e: Exception) {
          // Some providers can't delete; the file stays empty.
        }
        toast(app, app.getString(R.string.item_file_export_failed))
      }
    }

    private fun toast(context: Context, text: String) {
      mainHandler.post { Toast.makeText(context, text, Toast.LENGTH_LONG).show() }
    }
  }
}
