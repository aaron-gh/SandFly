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

package com.google.android.accessibility.talkback.keyboard

import android.text.InputType
import androidx.preference.PreferenceGroup
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.keyboard.DoublePressCharacters.Entry
import com.google.android.accessibility.talkback.notifications.NotificationCodeFragment
import com.google.android.accessibility.talkback.notifications.RowAction

/** Typing > Physical keyboard > Double-press characters: each key with what its double press types. */
class DoublePressCharactersFragment :
  NotificationCodeFragment(R.string.title_pref_sandfly_keyboard_double_press) {

  override fun build(screen: PreferenceGroup) {
    val entries = DoublePressCharacters.read(prefs)
    if (entries.size < DoublePressCharacters.MAX_ENTRIES) {
      screen.action(getString(R.string.double_press_add)) { edit(entries, -1) }
    }
    entries.forEachIndexed { index, entry ->
      val title = getString(R.string.double_press_item, entry.key.toString(), entry.characters)
      val actions =
        listOf(
          RowAction(getString(R.string.notification_edit)) { edit(entries, index) },
          RowAction(getString(R.string.notification_remove)) {
            save(entries.filterIndexed { i, _ -> i != index })
          },
        )
      screen.action(title, null, actions) { choose(title, actions.map { it.label }) { actions[it].run() } }
    }
  }

  private fun edit(entries: List<Entry>, index: Int) {
    val current = entries.getOrNull(index)
    askText(
      getString(if (current == null) R.string.double_press_add else R.string.notification_edit),
      current?.key?.toString().orEmpty(),
      hint = getString(R.string.double_press_key),
      secondHint = getString(R.string.double_press_characters),
      secondText = current?.characters.orEmpty(),
      inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,
      secondInputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,
    ) { keyText, characters, _ ->
      val key =
        DoublePressCharacters.normalizeKey(keyText)
          ?: return@askText getString(R.string.double_press_enter_key)
      if (!DoublePressCharacters.isValidCharacters(characters)) {
        return@askText getString(R.string.double_press_enter_characters)
      }
      val entry = Entry(key, characters)
      // A key has one entry: another entry of the same key is removed.
      save(
        if (current == null) entries.filter { it.key != key } + entry
        else
          entries.mapIndexedNotNull { i, old ->
            when {
              i == index -> entry
              old.key == key -> null
              else -> old
            }
          }
      )
      null
    }
  }

  private fun save(entries: List<Entry>) {
    DoublePressCharacters.write(prefs, entries)
    rebuild()
  }
}
