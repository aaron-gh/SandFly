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

package com.google.android.accessibility.talkback.clipboard

import android.content.Context
import android.content.Intent
import android.view.ViewTreeObserver
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.notifications.NotificationCodeFragment
import com.google.android.accessibility.talkback.preference.TalkBackPreferencesActivity
import com.google.android.accessibility.talkback.shizuku.ShizukuClipboard
import com.google.android.accessibility.utils.ClipboardHistory
import com.google.android.accessibility.utils.preference.PreferencesActivity.FRAGMENT_NAME

/** The texts copied, newest first: each can be copied again or removed. */
class ClipboardHistoryFragment : NotificationCodeFragment(R.string.title_clipboard_history) {

  private val onChange: () -> Unit = { if (isResumed) rebuild() }

  // The clipboard can be read once the screen has focus, which it doesn't have yet on resume.
  // While Shizuku adds each copy, the history has the clipboard's text already.
  private val onFocus =
    ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
      if (hasFocus && !ShizukuClipboard.isWatching()) context?.let(ClipboardHistory::recordClipboard)
    }

  override fun onResume() {
    super.onResume()
    ClipboardHistory.addListener(onChange)
    view?.viewTreeObserver?.addOnWindowFocusChangeListener(onFocus)
    if (view?.hasWindowFocus() == true && !ShizukuClipboard.isWatching()) {
      ClipboardHistory.recordClipboard(requireContext())
    }
  }

  override fun onPause() {
    ClipboardHistory.removeListener(onChange)
    view?.viewTreeObserver?.removeOnWindowFocusChangeListener(onFocus)
    super.onPause()
  }

  override fun build(screen: PreferenceGroup) {
    val context = requireContext()
    val entries = ClipboardHistory.entries(context)
    if (entries.isEmpty()) {
      screen.addPreference(
        Preference(context).apply {
          setTitle(R.string.clipboard_history_empty)
          isPersistent = false
          isSelectable = false
          isIconSpaceReserved = false
        }
      )
      return
    }
    entries.forEach { text ->
      val title = if (text.length > TITLE_LENGTH) text.take(TITLE_LENGTH) + "…" else text
      screen.action(title) {
        choose(title, listOf(getString(android.R.string.copy), getString(R.string.notification_remove))) {
          if (it == 0) ClipboardHistory.copy(context, text) else ClipboardHistory.remove(context, text)
        }
      }
    }
    screen.action(getString(R.string.clipboard_history_clear)) { ClipboardHistory.clear(context) }
  }

  companion object {
    private const val TITLE_LENGTH = 500

    /** Opens the clipboard history from the service. */
    @JvmStatic
    fun open(context: Context): Boolean {
      context.startActivity(
        Intent(context, TalkBackPreferencesActivity::class.java)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
          .putExtra(FRAGMENT_NAME, ClipboardHistoryFragment::class.java.name)
      )
      return true
    }
  }
}
