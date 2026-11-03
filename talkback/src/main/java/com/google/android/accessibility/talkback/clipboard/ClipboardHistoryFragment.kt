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

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.ViewTreeObserver
import android.view.accessibility.AccessibilityNodeInfo
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.notifications.NotificationCodeFragment
import com.google.android.accessibility.talkback.preference.TalkBackPreferencesActivity
import com.google.android.accessibility.talkback.shizuku.ShizukuClipboard
import com.google.android.accessibility.talkback.shizuku.ShizukuShell
import com.google.android.accessibility.utils.ClipboardHistory
import com.google.android.accessibility.utils.preference.PreferencesActivity.FRAGMENT_NAME

/**
 * The texts copied, newest first: each can be pasted into the edit box it was opened from, copied
 * again or removed.
 */
class ClipboardHistoryFragment : NotificationCodeFragment(R.string.title_clipboard_history) {

  private val onChange: () -> Unit = { if (isResumed) rebuild() }

  // The edit box this history was opened for. An older history that opening again closes is
  // destroyed after the new one is opened, and leaves the new edit box alone.
  private val openedTarget = pasteTarget

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

  override fun onDestroy() {
    // Opened again from settings, the history has no edit box to paste into.
    if (activity?.isFinishing == true && pasteTarget === openedTarget) pasteTarget = null
    super.onDestroy()
  }

  override fun build(screen: PreferenceGroup) {
    val context = requireContext()
    if (ShizukuShell.isAllowed()) {
      screen.switch(getString(R.string.title_clipboard_history_other_apps), ShizukuClipboard.isWanted(context)) {
        ShizukuClipboard.setWanted(context, it)
      }
    }
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
        val target = pasteTarget
        val choices =
          listOfNotNull(
            target?.let { getString(android.R.string.paste) },
            getString(android.R.string.copy),
            getString(R.string.notification_remove),
          )
        choose(title, choices) {
          when (if (target == null) it + 1 else it) {
            0 -> paste(context, text, target!!)
            1 -> ClipboardHistory.copy(context, text)
            else -> ClipboardHistory.remove(context, text)
          }
        }
      }
    }
    screen.action(getString(R.string.clipboard_history_clear)) { ClipboardHistory.clear(context) }
  }

  /** Copies [text], closes the history and pastes it into [target] once its app is in front. */
  private fun paste(context: Context, text: String, target: AccessibilityNodeInfo) {
    if (!ClipboardHistory.copy(context, text)) return
    pasteTarget = null
    activity?.finish()
    val handler = Handler(Looper.getMainLooper())
    // The app reads the clipboard only once its window has focus again.
    handler.postDelayed(
      {
        if (!target.refresh() || !target.performAction(AccessibilityNodeInfo.ACTION_PASTE)) {
          handler.postDelayed(
            { if (target.refresh()) target.performAction(AccessibilityNodeInfo.ACTION_PASTE) },
            PASTE_DELAY_MS,
          )
        }
      },
      PASTE_DELAY_MS,
    )
  }

  companion object {
    private const val TITLE_LENGTH = 500
    private const val PASTE_DELAY_MS = 500L

    /** The edit box the history was opened from, which Paste pastes into. */
    private var pasteTarget: AccessibilityNodeInfo? = null

    /** Opens the clipboard history from the service. */
    @JvmStatic
    fun open(context: Context): Boolean {
      pasteTarget = (context as? AccessibilityService)?.let(::editBoxWithFocus)
      context.startActivity(
        Intent(context, TalkBackPreferencesActivity::class.java)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
          .putExtra(FRAGMENT_NAME, ClipboardHistoryFragment::class.java.name)
      )
      return true
    }

    /** The edit box with accessibility focus, or else with input focus. */
    private fun editBoxWithFocus(service: AccessibilityService): AccessibilityNodeInfo? =
      listOf(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY, AccessibilityNodeInfo.FOCUS_INPUT)
        .firstNotNullOfOrNull { focus -> service.findFocus(focus)?.takeIf { it.isEditable } }
  }
}
