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

package com.google.android.accessibility.talkback.scripting

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.IntentCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.TalkBackPreferencesActivity
import com.google.android.accessibility.talkback.soundthemes.ThemeLinks
import com.google.android.accessibility.utils.preference.BasePreferencesActivity.FRAGMENT_ARGS
import com.google.android.accessibility.utils.preference.BasePreferencesActivity.FRAGMENT_NAME
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors

/**
 * Takes a script that is shared to Backtalk or opened with it, such as from a file manager or a
 * browser's downloads, or a shared https link to one, such as a script's page on GitHub, and opens
 * Scripts with it. Scripts then asks before installing, as it does for Import script, listing what
 * the script can do.
 */
class ScriptImportActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val source = sourceOf(intent)
    if (source == null) {
      // A repository link has already said why.
      if (!isFinishing) failed(getString(R.string.script_import_nothing))
      return
    }
    val context = applicationContext
    executor.execute {
      val file = File(cacheDir, SHARED_FILE)
      val error =
        try {
          when (source) {
            is Source.FromFile -> {
              val bytes =
                contentResolver.openInputStream(source.uri)?.use {
                  it.readAtMost(ScriptPackage.MAX_BYTES + 1)
                } ?: throw IOException(getString(R.string.script_import_cannot_open))
              file.writeBytes(bytes)
            }
            is Source.FromLink -> ThemeLinks.fetch(source.url, file)
          }
          null
        } catch (e: IOException) {
          e.message.orEmpty()
        } catch (e: SecurityException) {
          e.message.orEmpty()
        }
      runOnUiThread {
        if (error != null) {
          failed(getString(R.string.script_import_failed, error))
        } else {
          startActivity(
            Intent(context, TalkBackPreferencesActivity::class.java)
              .putExtra(FRAGMENT_NAME, ScriptsFragment::class.java.name)
              .putExtra(
                FRAGMENT_ARGS,
                Bundle().apply { putString(ScriptsFragment.ARG_IMPORT_FILE, file.path) },
              )
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
          )
          finish()
        }
      }
    }
  }

  private fun failed(message: String) {
    Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    finish()
  }

  /** Where the script comes from: a file, or a link to download. */
  private sealed class Source {
    data class FromFile(val uri: Uri) : Source()

    data class FromLink(val url: String) : Source()
  }

  private fun sourceOf(intent: Intent): Source? =
    when (intent.action) {
      Intent.ACTION_VIEW -> fileSource(intent.data)
      Intent.ACTION_SEND ->
        fileSource(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
          ?: linkSource(intent.getStringExtra(Intent.EXTRA_TEXT))
      else -> null
    }

  /**
   * A file to read, if it is a content URI. Another app could name a file:// path that only
   * Backtalk may read, and Backtalk would read it with its own permissions.
   */
  private fun fileSource(uri: Uri?): Source? =
    uri?.takeIf { it.scheme == ContentResolver.SCHEME_CONTENT }?.let { Source.FromFile(it) }

  /**
   * The script a shared https link points to. A link to a file on GitHub downloads the file
   * rather than its page. A link to a whole repository or branch isn't taken, since it can hold
   * more than one script, and its ZIP file isn't a script.
   */
  private fun linkSource(text: String?): Source? {
    val download = ThemeLinks.findLink(text)?.let(ThemeLinks::download) ?: return null
    if (download.url.startsWith("https://github.com/") && download.url.contains("/archive/")) {
      failed(getString(R.string.script_import_repository))
      return null
    }
    return Source.FromLink(download.url)
  }

  companion object {
    /** The copy of the shared script in Backtalk's cache, which Scripts reads and deletes. */
    const val SHARED_FILE = "shared_script"

    private val executor = Executors.newSingleThreadExecutor()
  }
}
