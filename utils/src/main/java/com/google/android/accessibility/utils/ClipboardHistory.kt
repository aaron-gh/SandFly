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
import android.os.UserManager
import org.json.JSONArray
import org.json.JSONException

/**
 * The texts SandFly put on the clipboard, newest first, and those it found there while one of its
 * own windows had focus: since Android 10, apps can't read the clipboard from the background, so
 * texts other apps copy are only seen that way, unless Shizuku reads them (ShizukuClipboard).
 *
 * Kept in a file of its own, apart from the settings, so that it is not exported with them. The
 * history keeps at most [maxEntries] texts, and, when [days] is set, deletes texts copied longer
 * ago than that when it is next read or changed.
 */
object ClipboardHistory {

  /** How many texts the history keeps unless set otherwise. */
  const val DEFAULT_MAX_ENTRIES = 50

  /** The most texts the history can be set to keep. */
  const val MAX_ENTRIES = 5000

  /** The setting of how many texts the history keeps, an Int. */
  const val KEY_MAX_ENTRIES = "pref_clipboard_history_max_entries"

  /** The setting of how many days the history keeps a text, an Int; 0 keeps texts for ever. */
  const val KEY_DAYS = "pref_clipboard_history_days"

  /**
   * Longer texts are kept, and appended texts put on the clipboard, cut to this length: as many
   * characters as continuous copy collects. The clip goes to the clipboard service in one Binder
   * transaction, of about 2 bytes a character.
   */
  const val MAX_LENGTH = 200_000

  /** The most characters of all entries together: older entries past it are dropped. */
  const val MAX_TOTAL_LENGTH = 1_000_000

  private const val FILE = "clipboard_history"
  private const val KEY = "entries"
  /** When each entry was copied, in milliseconds, in the order of the entries. */
  private const val KEY_TIMES = "times"
  private const val DAY_MS = 24L * 60 * 60 * 1000
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
      write(context, listOf(Entry(entry, now())) + read(context).filter { it.text != entry })
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
    val combined =
      (if (before.isNullOrEmpty()) added.toString() else before + "\n" + added).take(MAX_LENGTH)
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return combined
    clipboard.setPrimaryClip(ClipData.newPlainText(null, combined))
    readClipTime = clipboard.primaryClipDescription?.timestamp ?: readClipTime
    synchronized(this) {
      write(
        context,
        listOf(Entry(combined, now())) +
          read(context).filter { it.text != combined && it.text != before },
      )
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
  fun entries(context: Context): List<String> = synchronized(this) { read(context).map { it.text } }

  @JvmStatic
  fun remove(context: Context, text: String) {
    synchronized(this) { write(context, read(context).filter { it.text != text }) }
  }

  /** Removes the entries [texts]. */
  @JvmStatic
  fun removeAll(context: Context, texts: Collection<String>) {
    if (texts.isEmpty()) return
    val removed = texts.toHashSet()
    synchronized(this) { write(context, read(context).filter { it.text !in removed }) }
  }

  @JvmStatic
  fun clear(context: Context) {
    synchronized(this) { write(context, emptyList()) }
  }

  /**
   * Puts [edited] in the place of the entry [original]; another entry with the same text as
   * [edited] is removed. When [original] is no longer in the history, [edited] goes at the top. The
   * clipboard is left as it is. Returns false, changing nothing, when [edited] is blank.
   */
  @JvmStatic
  fun replace(context: Context, original: String, edited: CharSequence): Boolean {
    if (edited.isBlank()) return false
    val entry = edited.toString().take(MAX_LENGTH)
    synchronized(this) {
      val entries = read(context)
      if (entries.none { it.text == original }) {
        write(context, listOf(Entry(entry, now())) + entries.filter { it.text != entry })
      } else if (entry != original) {
        // The edited text keeps the time the original was copied.
        write(
          context,
          entries
            .filter { it.text != entry }
            .map { if (it.text == original) Entry(entry, it.time) else it },
        )
      }
    }
    return true
  }

  /** The name the exported history's, or the exported entries', file is offered with. */
  const val EXPORT_FILE_NAME = "sandfly-clipboard-history.txt"

  /** The line between two entries in the exported file. */
  const val EXPORT_SEPARATOR = "---"

  /**
   * The text of the exported history or entries: [entries] newest first, each as it is, with a
   * blank line, a line of [EXPORT_SEPARATOR] and a blank line between two entries, and a line
   * break at the end. The history holds at most [MAX_ENTRIES] texts of [MAX_LENGTH] characters,
   * [MAX_TOTAL_LENGTH] in all.
   */
  @JvmStatic
  fun exportText(entries: List<String>): String =
    if (entries.isEmpty()) "" else entries.joinToString("\n\n$EXPORT_SEPARATOR\n\n", postfix = "\n")

  /** How many texts the history keeps, from 1 to [MAX_ENTRIES]. */
  @JvmStatic
  fun maxEntries(context: Context): Int =
    intSetting(context, KEY_MAX_ENTRIES, DEFAULT_MAX_ENTRIES).coerceIn(1, MAX_ENTRIES)

  /** How many days the history keeps a text after it is copied; 0 keeps texts for ever. */
  @JvmStatic
  fun days(context: Context): Int = intSetting(context, KEY_DAYS, 0).coerceAtLeast(0)

  /**
   * Keeps at most [maxEntries] texts, from 1 to [MAX_ENTRIES], each for [days] after it is copied,
   * or for ever when [days] is 0, and deletes the texts past them now.
   */
  @JvmStatic
  fun setLimits(context: Context, maxEntries: Int, days: Int) {
    SharedPreferencesUtils.getSharedPreferences(context)
      .edit()
      .putInt(KEY_MAX_ENTRIES, maxEntries.coerceIn(1, MAX_ENTRIES))
      .putInt(KEY_DAYS, days.coerceAtLeast(0))
      .apply()
    // Reading deletes what is past the limits.
    synchronized(this) { read(context) }
  }

  private fun intSetting(context: Context, key: String, default: Int): Int =
    try {
      SharedPreferencesUtils.getSharedPreferences(context).getInt(key, default)
    } catch (e: ClassCastException) {
      default
    }

  /** Calls [listener] on the main thread each time the history changes. */
  fun addListener(listener: () -> Unit) {
    synchronized(listeners) { listeners += listener }
  }

  fun removeListener(listener: () -> Unit) {
    synchronized(listeners) { listeners -= listener }
  }

  @Volatile private var moved = false

  /**
   * The history's file, in storage that is encrypted until the person unlocks the device, as the
   * history holds what other apps copied. Null while it is locked: nothing is kept until then.
   */
  private fun prefs(context: Context): SharedPreferences? {
    val user = context.getSystemService(UserManager::class.java)
    if (user != null && !user.isUserUnlocked) return null
    val app = context.applicationContext ?: context
    if (!moved) {
      moved = true
      // Kept in device-protected storage before 0.3.0.
      app.moveSharedPreferencesFrom(app.createDeviceProtectedStorageContext(), FILE)
    }
    return app.getSharedPreferences(FILE, Context.MODE_PRIVATE)
  }

  /** A text of the history, and when it was copied, in milliseconds. */
  private class Entry(val text: String, val time: Long)

  private fun now() = System.currentTimeMillis()

  /**
   * [entries] within the limits: the first [maxEntries], copied within [days], whose lengths add up
   * to [MAX_TOTAL_LENGTH] at most, and else the first of them.
   */
  private fun withinLimits(context: Context, entries: List<Entry>): List<Entry> {
    val days = days(context)
    val oldest = if (days == 0) Long.MIN_VALUE else now() - days * DAY_MS
    val recent = entries.filter { it.time >= oldest }.take(maxEntries(context))
    var total = 0
    val kept = recent.takeWhile {
      total += it.text.length
      total <= MAX_TOTAL_LENGTH
    }
    return if (kept.isEmpty()) recent.take(1) else kept
  }

  /**
   * The entries within the limits. Deletes the others from the file, and gives the time now to
   * entries stored without a time, by versions that kept no times.
   */
  private fun read(context: Context): List<Entry> {
    val prefs = prefs(context) ?: return emptyList()
    val json = prefs.getString(KEY, null) ?: return emptyList()
    val stored =
      try {
        val array = JSONArray(json)
        val times = prefs.getString(KEY_TIMES, null)?.let(::JSONArray)
        val missingTime = now()
        (0 until array.length()).mapNotNull { index ->
          array.optString(index).takeIf(String::isNotEmpty)?.let {
            Entry(it, times?.optLong(index, missingTime) ?: missingTime)
          }
        }
      } catch (e: JSONException) {
        emptyList()
      }
    val kept = withinLimits(context, stored)
    if (kept.size != stored.size || !prefs.contains(KEY_TIMES)) write(context, kept)
    return kept
  }

  private fun write(context: Context, entries: List<Entry>) {
    val prefs = prefs(context) ?: return
    val kept = withinLimits(context, entries)
    prefs
      .edit()
      .putString(KEY, JSONArray(kept.map { it.text }).toString())
      .putString(KEY_TIMES, JSONArray(kept.map { it.time }).toString())
      .apply()
    val copy = synchronized(listeners) { listeners.toList() }
    android.os.Handler(android.os.Looper.getMainLooper()).post { copy.forEach { it() } }
  }
}
