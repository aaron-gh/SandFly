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

package com.google.android.accessibility.talkback.migration

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.os.UserManagerCompat
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.File
import java.io.IOException

/**
 * Copies the settings and custom labels of Backtalk Aaron on SandFly's first start.
 *
 * It runs from the Application, before anything loads the preferences or the labels database, so
 * no stale copy of them is cached. The files come from [SettingsExportProvider] in Backtalk
 * Aaron, which must be signed with Backtalk Aaron's key.
 */
object SettingsImporter {
  private const val TAG = "SettingsImporter"
  private const val KEY_DONE = "migration_done"
  private const val KEY_ATTEMPTS = "migration_attempts"
  // A failed copy is tried again on the next starts, but not forever.
  private const val MAX_ATTEMPTS = 3
  private const val TEMP_SUFFIX = ".import"

  /** Imports the old app's settings, once. Call it first thing in Application.onCreate. */
  @JvmStatic
  fun importIfNeeded(application: Application) {
    val context: Context = application
    if (context.packageName == AppIdMove.OLD_PACKAGE || !isMainProcess(context)) {
      return
    }
    val state = SharedPreferencesUtils.getSharedPreferences(context, MigrationFiles.STATE_PREFS)
    if (state.getBoolean(KEY_DONE, false)) {
      return
    }
    // Custom labels are in credential-protected storage, so wait until the user unlocks.
    if (!UserManagerCompat.isUserUnlocked(context)) {
      LogUtils.i(TAG, "The user is locked, so settings are imported later")
      return
    }
    val attempts = state.getInt(KEY_ATTEMPTS, 0) + 1
    val done =
      try {
        importFromOldApp(context)
        true
      } catch (e: Exception) {
        LogUtils.e(TAG, "Cannot import settings from %s: %s", AppIdMove.OLD_PACKAGE, e)
        attempts >= MAX_ATTEMPTS
      }
    state.edit().putInt(KEY_ATTEMPTS, attempts).putBoolean(KEY_DONE, done).commit()
  }

  /** Returns true if Backtalk Aaron is installed, signed with its own key. */
  @JvmStatic
  fun isOldAppInstalled(context: Context): Boolean =
    AppIdMove.isSignedBy(context.packageManager, AppIdMove.OLD_PACKAGE, AppIdMove.OLD_CERT_SHA256)

  private fun importFromOldApp(context: Context) {
    if (!isOldAppInstalled(context)) {
      LogUtils.i(TAG, "Backtalk Aaron is not installed, so there are no settings to import")
      return
    }
    val authority = AppIdMove.authority(AppIdMove.OLD_PACKAGE)
    val provider = context.packageManager.resolveContentProvider(authority, 0)
    if (provider == null || provider.packageName != AppIdMove.OLD_PACKAGE) {
      LogUtils.w(TAG, "The old app cannot share its settings, so none are imported")
      return
    }
    val base = Uri.Builder().scheme("content").authority(authority).build()
    val paths = listFiles(context, base)

    val prefsDir =
      File(context.createDeviceProtectedStorageContext().dataDir, MigrationFiles.PREFS_DIR)
    val database = context.getDatabasePath(MigrationFiles.LABELS_DATABASE)
    if (paths.any { it.startsWith(MigrationFiles.DATABASES_DIR + "/") }) {
      // Nothing has opened the database yet, so its files can be replaced.
      for (suffix in listOf("", "-wal", "-shm", "-journal")) {
        File(database.path + suffix).delete()
      }
    }

    val imported = mutableListOf<String>()
    for (path in paths) {
      val (directory, name) = MigrationFiles.parse(path) ?: continue
      val target =
        when (directory) {
          MigrationFiles.PREFS_DIR -> {
            val targetName =
              MigrationFiles.importedPrefsName(name, AppIdMove.OLD_PACKAGE, context.packageName)
                ?: continue
            // A backup file would win over the copy when the preferences load.
            File(prefsDir, "$targetName.bak").delete()
            File(prefsDir, targetName)
          }
          else -> File(database.parentFile, name)
        }
      val uri =
        base
          .buildUpon()
          .appendPath(AppIdMove.PATH_FILE)
          .appendPath(directory)
          .appendPath(name)
          .build()
      copy(context, uri, target)
      imported.add("$path (${target.length()} bytes)")
    }

    // Loads the default preferences from the copy, which is the first load in this process.
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val editor = prefs.edit()
    MigrationFiles.SKIPPED_PREF_KEYS.forEach { editor.remove(it) }
    editor.commit()

    LogUtils.i(
      TAG,
      "Imported %d files from %s: %s",
      imported.size,
      AppIdMove.OLD_PACKAGE,
      imported.joinToString(", "),
    )
  }

  private fun listFiles(context: Context, base: Uri): List<String> {
    val uri = base.buildUpon().appendPath(AppIdMove.PATH_FILES).build()
    val cursor =
      context.contentResolver.query(uri, null, null, null, null)
        ?: throw IOException("The old app did not list its files")
    return cursor.use {
      val column = it.getColumnIndexOrThrow(AppIdMove.COLUMN_PATH)
      buildList {
        while (it.moveToNext()) {
          add(it.getString(column))
        }
      }
    }
  }

  /** Copies through a temporary file, so a failed copy leaves no partial file behind. */
  private fun copy(context: Context, uri: Uri, target: File) {
    target.parentFile?.mkdirs()
    val temp = File(target.path + TEMP_SUFFIX)
    try {
      val input =
        context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")
      input.use { source -> temp.outputStream().use { source.copyTo(it) } }
      if (!temp.renameTo(target)) {
        throw IOException("Cannot move $temp to $target")
      }
    } finally {
      temp.delete()
    }
  }

  private fun isMainProcess(context: Context): Boolean {
    val processName =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        Application.getProcessName()
      } else {
        try {
          File("/proc/self/cmdline").readText().substringBefore('\u0000').trim()
        } catch (e: IOException) {
          return false
        }
      }
    return processName == context.packageName
  }
}
