/* Copyright 2026 Backtalk contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.soundthemes

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.google.android.accessibility.talkback.R
import java.io.IOException

/** Shared worker-thread export for installed themes and drafts. */
internal object SoundThemeExport {
  fun message(context: Context, prefs: SharedPreferences, id: String, uri: Uri): String {
    val count = try {
      context.contentResolver.openOutputStream(uri)?.use { SoundThemes.export(context, prefs, id, it) }
    } catch (error: IOException) { null } catch (error: SecurityException) { null }
    return if (count == null) context.getString(R.string.sound_theme_export_failed)
      else context.resources.getQuantityString(R.plurals.sound_theme_exported, count, count)
  }
}
