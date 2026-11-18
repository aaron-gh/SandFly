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

package com.google.android.accessibility.talkback.braille

import android.content.Context
import android.content.SharedPreferences
import android.text.InputType
import androidx.preference.PreferenceGroup
import com.google.android.accessibility.braille.common.BrailleUserPreferences
import com.google.android.accessibility.braille.common.translate.BrailleLanguages
import com.google.android.accessibility.braille.interfaces.CustomBrailleCharacters
import com.google.android.accessibility.braille.interfaces.CustomBrailleCharacters.Entry
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.backup.ItemFileLauncher
import com.google.android.accessibility.talkback.backup.ItemFiles
import com.google.android.accessibility.talkback.notifications.NotificationCodeFragment

/**
 * Braille keyboard settings > Custom braille characters: text typed with braille cells of the
 * user's choice, each in one braille table, ahead of the table's own rules.
 */
class CustomBrailleCharactersFragment : NotificationCodeFragment(R.string.title_pref_custom_braille_characters) {

  private val files = ItemFileLauncher(this) { rebuild() }

  private val braillePrefs: SharedPreferences
    get() = CustomBrailleCharacters.prefs(requireContext())

  override fun build(screen: PreferenceGroup) {
    val entries = CustomBrailleCharacters.read(braillePrefs)
    if (entries.size < CustomBrailleCharacters.MAX_ENTRIES) {
      screen.action(getString(R.string.custom_braille_characters_add)) { edit(entries, -1) }
    }
    if (ItemFileLauncher.isAvailable) {
      screen.action(getString(R.string.item_file_import)) { files.importFile() }
      if (entries.isNotEmpty()) {
        screen.action(getString(R.string.item_file_export)) {
          files.export(getString(R.string.title_pref_custom_braille_characters)) {
            ItemFiles.exportBrailleCharacters(it)
          }
        }
      }
    }
    val context = requireContext()
    entries.forEachIndexed { index, entry ->
      val summary = "${entry.cells}, ${tableName(context, entry.table)}"
      screen.action(entry.text, summary) {
        choose(entry.text, listOf(getString(R.string.notification_edit), getString(R.string.notification_remove))) {
          if (it == 0) edit(entries, index) else save(entries.filterIndexed { i, _ -> i != index })
        }
      }
    }
  }

  private fun edit(entries: List<Entry>, index: Int) {
    val context = requireContext()
    val current = entries.getOrNull(index)
    val tables = tableChoices(context, current?.table)
    val choice =
      DialogChoice(
        getString(R.string.custom_braille_characters_table),
        tables.map { tableName(context, it) },
        tables.indexOf(current?.table ?: currentTable(context)).coerceAtLeast(0),
      )
    askText(
      getString(if (current == null) R.string.custom_braille_characters_add else R.string.notification_edit),
      current?.text.orEmpty(),
      hint = getString(R.string.custom_braille_characters_text),
      message = getString(R.string.custom_braille_characters_cells_help),
      secondHint = getString(R.string.custom_braille_characters_cells),
      secondText = current?.cells.orEmpty(),
      inputType = InputType.TYPE_CLASS_TEXT,
      choice = choice,
    ) { text, cells, _ ->
      if (!CustomBrailleCharacters.isValidText(text)) {
        return@askText getString(R.string.custom_braille_characters_invalid_text, CustomBrailleCharacters.MAX_TEXT)
      }
      val entry =
        CustomBrailleCharacters.entry(text, cells, tables[choice.selected])
          ?: return@askText getString(R.string.custom_braille_characters_invalid_cells, CustomBrailleCharacters.MAX_CELLS)
      val position = if (index >= 0) index else entries.size
      val updated = entries.toMutableList().apply { if (index >= 0) set(index, entry) else add(entry) }
      // An entry replaces the one with the same text in the same table.
      save(updated.filterIndexed { i, other -> i == position || other.text != entry.text || other.table != entry.table })
      null
    }
  }

  private fun save(entries: List<Entry>) {
    CustomBrailleCharacters.write(braillePrefs, entries)
    rebuild()
  }

  private companion object {
    /** The braille keyboard's current language, which a new entry is for. */
    fun currentTable(context: Context): String =
      BrailleUserPreferences.readCurrentActiveInputCodeAndCorrect(context).name

    /**
     * The tables an entry can be for: the braille keyboard's languages, the braille display's, and
     * [table], the entry's own.
     */
    fun tableChoices(context: Context, table: String?): List<String> =
      (BrailleUserPreferences.readAvailablePreferredCodes(context).map { it.name } +
          listOfNotNull(
            currentTable(context),
            BrailleUserPreferences.readCurrentActiveOutputCodeAndCorrect(context).name,
            table,
          ))
        .distinct()

    fun tableName(context: Context, table: String): String =
      try {
        BrailleLanguages.Code.valueOf(table).getUserFacingName(context)
      } catch (e: IllegalArgumentException) {
        table
      }
  }
}
