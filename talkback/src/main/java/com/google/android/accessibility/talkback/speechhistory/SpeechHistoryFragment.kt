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

package com.google.android.accessibility.talkback.speechhistory

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.notifications.NotificationCodeFragment
import com.google.android.accessibility.talkback.preference.TalkBackPreferencesActivity
import com.google.android.accessibility.utils.ClipboardHistory
import com.google.android.accessibility.utils.output.SpeechHistory
import com.google.android.accessibility.utils.preference.BasePreferencesActivity.FRAGMENT_ARGS
import com.google.android.accessibility.utils.preference.PreferencesActivity.FRAGMENT_NAME
import java.util.UUID

/**
 * The texts SandFly spoke lately, newest first, as they were when the screen showed. Each can be
 * copied, and shown whole when the list shortens it. Speech isn't recorded while the screen shows.
 * The settings activity is exported, so the list is empty unless the Speech history command opened
 * it.
 */
class SpeechHistoryFragment : NotificationCodeFragment(R.string.title_speech_history) {

  override fun onResume() {
    SpeechHistory.setPaused(true)
    super.onResume()
  }

  override fun onPause() {
    SpeechHistory.setPaused(false)
    super.onPause()
  }

  override fun build(screen: PreferenceGroup) {
    val context = requireContext()
    val token = arguments?.getString(ARG_TOKEN)
    val entries = if (token != null && token == openToken) SpeechHistory.entries() else emptyList()
    if (entries.isEmpty()) {
      screen.addPreference(
        Preference(context).apply {
          setTitle(R.string.speech_history_empty)
          isPersistent = false
          isSelectable = false
          isIconSpaceReserved = false
        }
      )
      return
    }
    entries.forEach { text ->
      val shortened = text.length > TITLE_LENGTH
      val title = if (shortened) text.take(TITLE_LENGTH) + "…" else text
      screen.action(title) {
        val choices =
          listOfNotNull(
            getString(R.string.notification_history_copy),
            if (shortened) getString(R.string.notification_list_show_all) else null,
          )
        choose(title, choices) {
          when (it) {
            0 -> copy(context, text)
            else -> dialog().setMessage(text).setPositiveButton(android.R.string.ok, null).show()
          }
        }
      }
    }
    screen.action(getString(R.string.speech_history_clear)) {
      SpeechHistory.clear()
      rebuild()
    }
  }

  private fun copy(context: Context, text: String) {
    if (ClipboardHistory.copy(context, text)) {
      Toast.makeText(context, R.string.notification_copied, Toast.LENGTH_SHORT).show()
    }
  }

  companion object {
    private const val TITLE_LENGTH = 500
    private const val ARG_TOKEN = "speech_history_token"

    /** Given to the screen the command opened last, which alone shows the history. */
    @Volatile private var openToken: String? = null

    /** Opens the speech history from the service. */
    @JvmStatic
    fun open(context: Context): Boolean {
      val token = UUID.randomUUID().toString()
      openToken = token
      context.startActivity(
        Intent(context, TalkBackPreferencesActivity::class.java)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
          .putExtra(FRAGMENT_NAME, SpeechHistoryFragment::class.java.name)
          .putExtra(FRAGMENT_ARGS, Bundle().apply { putString(ARG_TOKEN, token) })
      )
      return true
    }
  }
}
