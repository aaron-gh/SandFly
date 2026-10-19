/* Copyright 2026 Backtalk contributors. Licensed under the Apache License, Version 2.0. */
package com.google.android.accessibility.talkback.soundthemes

import android.annotation.SuppressLint
import android.view.MenuItem
import com.google.android.accessibility.talkback.preference.TalkBackPreferencesActivity

/** Keeps legacy Back keys and Navigate up on the draft fragment's confirmation path. */
class ThemeDraftActivity : TalkBackPreferencesActivity.TalkBackSubSettings() {
  // BasePreferencesActivity finishes directly for legacy Back. Delegate to AndroidX instead;
  // modern system gestures already use this dispatcher. Calling super would discard
  // the draft immediately, before its confirmation callback could run.
  @SuppressLint("GestureBackNavigation", "MissingSuperCall")
  @Suppress("DEPRECATION")
  override fun onBackPressed() { onBackPressedDispatcher.onBackPressed() }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      onBackPressedDispatcher.onBackPressed()
      return true
    }
    return super.onOptionsItemSelected(item)
  }
}
