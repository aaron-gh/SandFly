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

package com.google.android.accessibility.talkback.speechdictionary

import android.content.Intent
import androidx.preference.PreferenceGroup
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.notifications.NotificationCodeFragment
import com.google.android.accessibility.talkback.notifications.NotificationReader
import com.google.android.accessibility.talkback.notifications.Replacement
import com.google.android.accessibility.talkback.notifications.TextMatch
import java.util.concurrent.Executors

/** Speech > Speech dictionary: the entries, each with the app it is used in. */
class SpeechDictionaryFragment : NotificationCodeFragment(R.string.title_pref_speech_dictionary) {

  override fun build(screen: PreferenceGroup) {
    val entries = SpeechDictionary.read(prefs)
    screen.action(getString(R.string.notification_add_replacement)) { edit(entries, -1) }
    entries.forEachIndexed { index, entry ->
      val title = describe(entry.replacement)
      val summary =
        listOfNotNull(
            entry.appName.takeIf { entry.app.isNotEmpty() },
            getString(R.string.notification_regex).takeIf { entry.replacement.regex },
          )
          .joinToString(", ")
          .ifEmpty { null }
      screen.action(title, summary) {
        choose(
          title,
          listOf(
            getString(R.string.notification_edit),
            getString(R.string.notification_choose_app),
            getString(R.string.notification_remove),
          ),
        ) {
          when (it) {
            0 -> edit(entries, index)
            1 -> chooseApp { app, name -> save(entries.replace(index, entry.copy(app = app, appName = name))) }
            else -> save(entries.filterIndexed { i, _ -> i != index })
          }
        }
      }
    }
  }

  private fun describe(replacement: Replacement): String =
    when {
      replacement.replace.isEmpty() -> getString(R.string.speech_dictionary_not_said, replacement.find)
      else ->
        getString(
          R.string.notification_replacement_item,
          replacement.find,
          if (replacement.replace.isBlank()) getString(R.string.granular_editing_space) else replacement.replace,
        )
    }

  private fun edit(entries: List<DictionaryEntry>, index: Int) {
    val current = entries.getOrNull(index)
    askText(
      getString(if (current == null) R.string.notification_add_replacement else R.string.notification_edit),
      current?.replacement?.find.orEmpty(),
      hint = getString(R.string.notification_replace_find),
      secondHint = getString(R.string.notification_replace_with),
      secondText = current?.replacement?.replace.orEmpty(),
      checkLabels = listOf(getString(R.string.notification_whole_words), getString(R.string.notification_regex)),
      checked = listOf(current?.replacement?.wholeWord ?: true, current?.replacement?.regex == true),
    ) { find, replace, checks ->
      if (find.isEmpty()) return@askText getString(R.string.notification_enter_text)
      if (checks[1] && !TextMatch.isValidRegex(find)) return@askText getString(R.string.notification_invalid_regex)
      val replacement = Replacement(find, replace, regex = checks[1], wholeWord = checks[0])
      save(
        if (current == null) entries + DictionaryEntry(replacement)
        else entries.replace(index, current.copy(replacement = replacement))
      )
      null
    }
  }

  /** Asks for the app an entry is used in, with All apps first, and calls [then] with it. */
  private fun chooseApp(then: (String, String) -> Unit) {
    val context = requireContext().applicationContext
    EXECUTOR.execute {
      val apps =
        context.packageManager
          .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
          .map { it.activityInfo.packageName }
          .distinct()
          .map { it to NotificationReader.appName(context, it) }
          .sortedBy { it.second.lowercase() }
      activity?.runOnUiThread {
        if (!isAdded) return@runOnUiThread
        val names = listOf(getString(R.string.speech_dictionary_all_apps)) + apps.map { it.second }
        choose(getString(R.string.notification_choose_app), names) {
          if (it == 0) then("", "") else then(apps[it - 1].first, apps[it - 1].second)
        }
      }
    }
  }

  private fun save(entries: List<DictionaryEntry>) {
    SpeechDictionary.write(prefs, entries)
    rebuild()
  }

  private fun <T> List<T>.replace(index: Int, item: T): List<T> =
    mapIndexed { i, old -> if (i == index) item else old }

  private companion object {
    val EXECUTOR = Executors.newSingleThreadExecutor()
  }
}
