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

package com.google.android.accessibility.talkback.soundthemes

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors

/**
 * Installs a sound theme from a theme file or a link. Other apps open theme files with it, such as
 * a file manager or a browser's downloads, and share theme files or links to it. The settings open
 * it with a file or link the person chose. It says what the theme is and asks before installing.
 */
class SoundThemeInstallActivity : AppCompatActivity() {
  private val executor = Executors.newSingleThreadExecutor()
  private var staged: StagedTheme? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val source = sourceOf(intent)
    if (source == null) {
      fail(getString(R.string.sound_theme_install_nothing))
      return
    }
    Toast.makeText(this, R.string.sound_theme_install_reading, Toast.LENGTH_SHORT).show()
    val context = applicationContext
    executor.execute {
      val download = File(cacheDir, "sound_theme.zip")
      val result: Any =
        try {
          val name = fetch(context, source, download)
          SoundThemes.stage(context, download, name)
        } catch (e: SoundThemes.NotAThemeException) {
          getString(R.string.sound_theme_install_not_a_theme, e.message)
        } catch (e: IOException) {
          LogUtils.w(TAG, "Cannot read sound theme: %s", e)
          getString(R.string.sound_theme_install_failed, e.message ?: "")
        } catch (e: SecurityException) {
          getString(R.string.sound_theme_install_failed, e.message ?: "")
        } finally {
          download.delete()
        }
      runOnUiThread {
        if (isFinishing || isDestroyed) {
          (result as? StagedTheme)?.let { SoundThemes.discard(it) }
        } else if (result is StagedTheme) {
          staged = result
          confirm(result)
        } else {
          fail(result as String)
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    // Dismissed without installing.
    staged?.let { SoundThemes.discard(it) }
    staged = null
    executor.shutdown()
  }

  private fun confirm(theme: StagedTheme) {
    var message =
      SoundThemesFragment.describe(this, theme.manifest, theme.sounds.size)
    if (SoundThemes.replacesInstalled(this, theme)) {
      message += "\n\n" + getString(R.string.sound_theme_install_replaces)
    }
    if (theme.skipped.isNotEmpty()) {
      message +=
        "\n\n" + getString(R.string.sound_theme_install_skipped, theme.skipped.joinToString("\n"))
    }
    AlertDialog.Builder(this)
      .setTitle(getString(R.string.sound_theme_install_title, theme.manifest.name))
      .setMessage(message)
      .setPositiveButton(R.string.sound_theme_install_and_use) { _, _ -> install(theme, true) }
      .setNeutralButton(R.string.sound_theme_install) { _, _ -> install(theme, false) }
      .setNegativeButton(android.R.string.cancel, null)
      .setOnDismissListener { finish() }
      .show()
  }

  private fun install(theme: StagedTheme, use: Boolean) {
    staged = null
    val prefs = SharedPreferencesUtils.getSharedPreferences(this)
    try {
      SoundThemes.install(this, prefs, theme, use)
      val done = if (use) R.string.sound_theme_installed_in_use else R.string.sound_theme_installed
      Toast.makeText(applicationContext, getString(done, theme.manifest.name), Toast.LENGTH_LONG)
        .show()
    } catch (e: IOException) {
      Toast.makeText(
          applicationContext,
          getString(R.string.sound_theme_install_failed, e.message ?: ""),
          Toast.LENGTH_LONG,
        )
        .show()
    }
  }

  private fun fail(message: String) {
    AlertDialog.Builder(this)
      .setMessage(message)
      .setPositiveButton(android.R.string.ok, null)
      .setOnDismissListener { finish() }
      .show()
  }

  /** Where the theme comes from: a file or a link. */
  private sealed class Source {
    data class FromFile(val uri: Uri) : Source()

    data class FromLink(val link: String) : Source()
  }

  private fun sourceOf(intent: Intent): Source? {
    // A link to download is only taken from Backtalk itself, through the activity alias that other
    // apps cannot start, so that no app can make Backtalk download without the user sharing a link.
    if (intent.component?.className == LINK_ALIAS) {
      return intent.getStringExtra(EXTRA_LINK)?.let { Source.FromLink(it) }
    }
    return when (intent.action) {
      Intent.ACTION_VIEW -> fileSource(intent.data)
      Intent.ACTION_SEND ->
        IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let {
          fileSource(it)
        } ?: ThemeLinks.findLink(intent.getStringExtra(Intent.EXTRA_TEXT))?.let {
          Source.FromLink(it)
        }
      else -> null
    }
  }

  /**
   * A file to read, if it is a content URI. Another app could name a file:// path that only
   * Backtalk may read, and Backtalk would read it with its own permissions.
   */
  private fun fileSource(uri: Uri?): Source? =
    uri?.takeIf { it.scheme == ContentResolver.SCHEME_CONTENT }?.let { Source.FromFile(it) }

  /** Copies or downloads the theme file into [file], and returns a name for the theme. */
  @Throws(IOException::class)
  private fun fetch(context: Context, source: Source, file: File): String =
    when (source) {
      is Source.FromLink -> {
        val download =
          ThemeLinks.download(source.link)
            ?: throw IOException(context.getString(R.string.sound_theme_install_https_only))
        ThemeLinks.fetch(download.url, file)
        download.name
      }
      is Source.FromFile -> {
        val input =
          context.contentResolver.openInputStream(source.uri)
            ?: throw IOException("cannot open ${source.uri}")
        input.use { ThemeLinks.copyLimited(it, file) }
        displayName(context, source.uri)?.substringBeforeLast('.') ?: "Sound theme"
      }
    }

  private fun displayName(context: Context, uri: Uri): String? =
    try {
      context.contentResolver
        .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { if (it.moveToFirst()) it.getString(0) else null }
    } catch (e: RuntimeException) {
      uri.lastPathSegment
    }

  companion object {
    private const val TAG = "SoundThemeInstall"
    private const val EXTRA_LINK = "sound_theme_link"

    /** The activity alias, not exported, that starts a download of a link. */
    private const val LINK_ALIAS =
      "com.google.android.accessibility.talkback.soundthemes.SoundThemeLinkInstall"

    @JvmStatic
    fun fileIntent(context: Context, uri: Uri): Intent =
      Intent(context, SoundThemeInstallActivity::class.java)
        .setAction(Intent.ACTION_VIEW)
        .setData(uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    @JvmStatic
    fun linkIntent(context: Context, link: String): Intent =
      Intent().setClassName(context, LINK_ALIAS).putExtra(EXTRA_LINK, link)
  }
}
