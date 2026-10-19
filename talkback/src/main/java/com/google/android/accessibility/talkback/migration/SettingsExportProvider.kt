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

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.os.UserManagerCompat
import com.google.android.libraries.accessibility.utils.log.LogUtils
import java.io.File
import java.io.FileNotFoundException

/**
 * Shares Backtalk Aaron's settings and custom labels, read-only, with SandFly.
 *
 * Only SandFly, signed with its own key, can use it, and only from Backtalk Aaron. It
 * lists the files at content://<authority>/files and opens one at
 * content://<authority>/file/<directory>/<name>. Only the files that [MigrationFiles] allows are
 * served, and only from their own directories.
 */
class SettingsExportProvider : ContentProvider() {
  override fun onCreate(): Boolean = true

  override fun query(
    uri: Uri,
    projection: Array<out String>?,
    selection: String?,
    selectionArgs: Array<out String>?,
    sortOrder: String?,
  ): Cursor? {
    if (uri.pathSegments != listOf(AppIdMove.PATH_FILES)) {
      return null
    }
    val context = context ?: return null
    checkCaller(context)
    val cursor = MatrixCursor(arrayOf(AppIdMove.COLUMN_PATH, AppIdMove.COLUMN_SIZE))
    for ((path, file) in exportedFiles(context)) {
      cursor.addRow(arrayOf<Any>(path, file.length()))
    }
    return cursor
  }

  override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
    if (mode != "r") {
      throw SecurityException("Settings are shared read-only")
    }
    val segments = uri.pathSegments
    if (segments.size != 3 || segments[0] != AppIdMove.PATH_FILE) {
      throw FileNotFoundException(uri.toString())
    }
    val path = MigrationFiles.path(segments[1], segments[2])
    val context = context ?: throw FileNotFoundException(path)
    checkCaller(context)
    val file =
      exportedFiles(context, checkpoint = false)[path] ?: throw FileNotFoundException(path)
    return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
  }

  /** Refuses every app but SandFly, and refuses everyone when this app is not Backtalk Aaron. */
  private fun checkCaller(context: Context) {
    val caller = callingPackage
    if (
      context.packageName != AppIdMove.OLD_PACKAGE ||
        caller != AppIdMove.NEW_PACKAGE ||
        !AppIdMove.isSignedBy(context.packageManager, caller, AppIdMove.NEW_CERT_SHA256)
    ) {
      throw SecurityException("Settings are shared only with SandFly, not with $caller")
    }
  }

  override fun getType(uri: Uri): String? = null

  override fun insert(uri: Uri, values: ContentValues?): Uri? =
    throw UnsupportedOperationException("Read-only")

  override fun update(
    uri: Uri,
    values: ContentValues?,
    selection: String?,
    selectionArgs: Array<out String>?,
  ): Int = throw UnsupportedOperationException("Read-only")

  override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
    throw UnsupportedOperationException("Read-only")

  companion object {
    private const val TAG = "SettingsExportProvider"

    /** The files that may be exported now, by their exported path. */
    private fun exportedFiles(context: Context, checkpoint: Boolean = true): Map<String, File> {
      val files = LinkedHashMap<String, File>()
      val deContext = context.createDeviceProtectedStorageContext()
      val prefsDir = File(deContext.dataDir, MigrationFiles.PREFS_DIR)
      prefsDir.listFiles()?.sortedBy { it.name }?.forEach { addIfAllowed(files, prefsDir, it) }

      // Custom labels are in credential-protected storage, which is readable once unlocked.
      if (UserManagerCompat.isUserUnlocked(context)) {
        val database = context.getDatabasePath(MigrationFiles.LABELS_DATABASE)
        if (checkpoint && database.isFile) {
          checkpoint(database)
        }
        val databasesDir = database.parentFile
        if (databasesDir != null) {
          for (name in MigrationFiles.DATABASE_FILES) {
            val file = File(databasesDir, name)
            if (file.length() > 0) {
              addIfAllowed(files, databasesDir, file)
            }
          }
        }
      }
      return files
    }

    private fun addIfAllowed(files: MutableMap<String, File>, directory: File, file: File) {
      val path = MigrationFiles.path(directory.name, file.name)
      if (!file.isFile || MigrationFiles.parse(path) == null) {
        return
      }
      // Rejects links that lead out of the directory.
      if (file.canonicalFile.parentFile != directory.canonicalFile) {
        return
      }
      files[path] = file
    }

    /** Moves the write-ahead log into the database file, so the copy is complete on its own. */
    private fun checkpoint(database: File) {
      try {
        SQLiteDatabase.openDatabase(database.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
          db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() }
        }
      } catch (e: Exception) {
        // The log is served too, so the copy is still complete.
        LogUtils.w(TAG, "Cannot checkpoint the labels database: %s", e)
      }
    }
  }
}
