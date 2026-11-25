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

package com.google.android.accessibility.talkback.soundthemes

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import java.text.Collator

/**
 * The folder that sound files for theme sounds are chosen from, picked once with the system folder
 * picker and kept with a persisted read permission, and the listing of its sound files and
 * subfolders.
 */
object SoundFolder {
  /** The tree URI of the folder. Kept out of settings profiles, since it is only valid here. */
  const val PREF_FOLDER = "pref_theme_sound_folder"

  /** The most sound files and subfolders listed in one folder. */
  const val MAX_ENTRIES = 2000

  /** The most rows read from one folder, sound files or not. */
  private const val MAX_ROWS = 20000

  /** A sound file or a subfolder. */
  data class Entry(val documentId: String, val name: String, val isFolder: Boolean)

  /** One folder: its name, if the provider gives one, and its entries in the order shown. */
  class Listing(val name: String?, val entries: List<Entry>)

  /** The folder, if one was chosen. Whether it can still be read is [hasAccess]. */
  fun folder(prefs: SharedPreferences): Uri? = prefs.getString(PREF_FOLDER, null)?.let(Uri::parse)

  /** Whether the read permission for [tree] is still held. */
  fun hasAccess(context: Context, tree: Uri): Boolean =
    context.contentResolver.persistedUriPermissions.any { it.uri == tree && it.isReadPermission }

  /**
   * Keeps read access to [tree] and uses it from now on, releasing the folder used before. Returns
   * false if the permission can't be kept.
   */
  fun remember(context: Context, prefs: SharedPreferences, tree: Uri): Boolean {
    val resolver = context.contentResolver
    try {
      resolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (e: SecurityException) {
      return false
    }
    val old = folder(prefs)
    if (old != null && old != tree) release(context, old)
    prefs.edit().putString(PREF_FOLDER, tree.toString()).apply()
    return true
  }

  /** Stops using the folder, such as when its permission was revoked or it was removed. */
  fun forget(context: Context, prefs: SharedPreferences) {
    folder(prefs)?.let { release(context, it) }
    prefs.edit().remove(PREF_FOLDER).apply()
  }

  private fun release(context: Context, tree: Uri) {
    try {
      context.contentResolver.releasePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (e: SecurityException) {
      // Not held any more.
    }
  }

  /** The document ID of the top of [tree]. */
  fun rootId(tree: Uri): String = DocumentsContract.getTreeDocumentId(tree)

  /** The URI of a file or folder in [tree], to read it with. */
  fun documentUri(tree: Uri, documentId: String): Uri =
    DocumentsContract.buildDocumentUriUsingTree(tree, documentId)

  /**
   * Lists the sound files and subfolders of the folder [documentId] in [tree]. Returns null if the
   * folder can't be read, such as when its permission was revoked or it was removed. Reads from
   * the provider, so not to be called on the main thread.
   */
  fun list(context: Context, tree: Uri, documentId: String): Listing? {
    val resolver = context.contentResolver
    val found = ArrayList<Entry>()
    try {
      val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, documentId)
      val columns = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE)
      val cursor = resolver.query(children, columns, null, null, null) ?: return null
      cursor.use {
        var rows = 0
        while (rows < MAX_ROWS && found.size < MAX_ENTRIES && it.moveToNext()) {
          rows++
          val id = it.getString(0) ?: continue
          val name = it.getString(1) ?: continue
          entryOf(id, name, it.getString(2))?.let(found::add)
        }
      }
    } catch (e: RuntimeException) {
      // Another app's provider can throw anything, such as SecurityException.
      return null
    }
    return Listing(nameOf(context, tree, documentId), sorted(found, Collator.getInstance()))
  }

  private fun nameOf(context: Context, tree: Uri, documentId: String): String? =
    try {
      context.contentResolver
        .query(documentUri(tree, documentId), arrayOf(Document.COLUMN_DISPLAY_NAME), null, null, null)
        ?.use { if (it.moveToFirst()) it.getString(0) else null }
    } catch (e: RuntimeException) {
      null
    }

  /** The entry for a row of a folder, or null if it is neither a sound file nor a folder. */
  fun entryOf(documentId: String, name: String, mimeType: String?): Entry? =
    when {
      name.startsWith(".") -> null
      mimeType == Document.MIME_TYPE_DIR -> Entry(documentId, name, isFolder = true)
      isSound(name, mimeType) -> Entry(documentId, name, isFolder = false)
      else -> null
    }

  /** Whether a file can be chosen as a sound, from its name or else from its type. */
  fun isSound(name: String, mimeType: String?): Boolean =
    SoundThemes.extensionOf(name) != null ||
      mimeType?.startsWith("audio/") == true ||
      mimeType == "application/ogg"

  /** Folders first, then files, each by name. */
  fun sorted(entries: List<Entry>, collator: Comparator<in String>): List<Entry> =
    entries.sortedWith(compareBy<Entry> { !it.isFolder }.thenBy(collator) { it.name })
}
