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

package com.google.android.accessibility.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONException

/**
 * The texts SandFly put on the clipboard, newest first, and those it found there while one of its
 * own windows had focus: since Android 10, apps can't read the clipboard from the background, so
 * texts other apps copy are only seen that way, unless Shizuku reads them (ShizukuClipboard).
 *
 * Kept in a file of its own, apart from the settings, so that it is not exported with them.
 */
object ClipboardHistory {

  const val MAX_ENTRIES = 50

  // Longer texts are kept cut to this length.
  const val MAX_LENGTH = 10_000

  private const val FILE = "clipboard_history"
  private const val KEY = "entries"
  /** ClipDescription.EXTRA_IS_SENSITIVE, which apps set before Android 13 too. */
  private const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"

  private val listeners = mutableListOf<() -> Unit>()

  // When the clip read last was copied, so that the same clip isn't read again: Android announces
  // each read.
  @Volatile private var readClipTime = 0L

  /** Puts [text] on the clipboard and at the top of the history. Returns false if it is empty. */
  @JvmStatic
  fun copy(context: Context, text: CharSequence?): Boolean {
    if (text.isNullOrEmpty()) return false
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
    clipboard.setPrimaryClip(ClipData.newPlainText(null, text))
    record(context, text)
    // The clip is SandFly's own, and in the history already.
    readClipTime = clipboard.primaryClipDescription?.timestamp ?: readClipTime
    return true
  }

  /** Puts [text] at the top of the history, once. */
  @JvmStatic
  fun record(context: Context, text: CharSequence?) {
    if (text.isNullOrBlank()) return
    val entry = text.toString().take(MAX_LENGTH)
    synchronized(this) {
      val entries = listOf(entry) + read(context).filter { it != entry }
      write(context, entries.take(MAX_ENTRIES))
    }
  }

  /**
   * The text on the clipboard, which only SandFly windows with focus may read, or else the newest
   * text in the history.
   */
  @JvmStatic
  fun current(context: Context): String? {
    readClipboard(context)?.let {
      record(context, it)
      return it
    }
    return entries(context).firstOrNull()
  }

  /**
   * Puts the [current] text with [added] after it, on a new line, on the clipboard, in place of
   * the current text at the top of the history. Returns the text put there.
   */
  @JvmStatic
  fun append(context: Context, added: CharSequence): String {
    val before = current(context)
    val combined = if (before.isNullOrEmpty()) added.toString() else before + "\n" + added
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return combined
    clipboard.setPrimaryClip(ClipData.newPlainText(null, combined))
    readClipTime = clipboard.primaryClipDescription?.timestamp ?: readClipTime
    val entry = combined.take(MAX_LENGTH)
    synchronized(this) {
      write(context, (listOf(entry) + read(context).filter { it != entry && it != before }).take(MAX_ENTRIES))
    }
    return combined
  }

  /** Adds the clipboard's text to the history, if SandFly may read it. */
  @JvmStatic
  fun recordClipboard(context: Context) {
    readClipboard(context)?.let { record(context, it) }
  }

  private fun readClipboard(context: Context): String? =
    try {
      val clipboard = context.getSystemService(ClipboardManager::class.java)
      val time = clipboard?.primaryClipDescription?.timestamp
      if (clipboard == null || time == null || time == readClipTime) {
        null
      } else {
        readClipTime = time
        // Password managers and keyboards mark passwords as sensitive, which the history doesn't
        // keep.
        val sensitive =
          clipboard.primaryClipDescription?.extras?.getBoolean(EXTRA_IS_SENSITIVE) == true
        clipboard.primaryClip
          ?.takeIf { !sensitive && it.itemCount > 0 }
          ?.getItemAt(0)
          ?.coerceToText(context)
          ?.toString()
          ?.takeIf { it.isNotBlank() }
      }
    } catch (e: RuntimeException) {
      // Clipboards some apps put content URIs on.
      null
    }

  @JvmStatic
  fun entries(context: Context): List<String> = synchronized(this) { read(context) }

  @JvmStatic
  fun remove(context: Context, text: String) {
    synchronized(this) { write(context, read(context).filter { it != text }) }
  }

  @JvmStatic
  fun clear(context: Context) {
    synchronized(this) { write(context, emptyList()) }
  }

  /** Calls [listener] on the main thread each time the history changes. */
  fun addListener(listener: () -> Unit) {
    synchronized(listeners) { listeners += listener }
  }

  fun removeListener(listener: () -> Unit) {
    synchronized(listeners) { listeners -= listener }
  }

  private fun prefs(context: Context): SharedPreferences =
    context.createDeviceProtectedStorageContext().getSharedPreferences(FILE, Context.MODE_PRIVATE)

  private fun read(context: Context): List<String> {
    val json = prefs(context).getString(KEY, null) ?: return emptyList()
    return try {
      val array = JSONArray(json)
      (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotEmpty) }
    } catch (e: JSONException) {
      emptyList()
    }
  }

  private fun write(context: Context, entries: List<String>) {
    prefs(context).edit().putString(KEY, JSONArray(entries).toString()).apply()
    val copy = synchronized(listeners) { listeners.toList() }
    android.os.Handler(android.os.Looper.getMainLooper()).post { copy.forEach { it() } }
  }
}
