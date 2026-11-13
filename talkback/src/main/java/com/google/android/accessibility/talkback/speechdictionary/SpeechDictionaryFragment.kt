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

import androidx.annotation.StringRes
import androidx.preference.PreferenceGroup
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.backup.ItemFileLauncher
import com.google.android.accessibility.talkback.backup.ItemFiles
import com.google.android.accessibility.talkback.notifications.NotificationCodeFragment
import com.google.android.accessibility.talkback.notifications.Replacement
import com.google.android.accessibility.talkback.notifications.TextMatch

/** Speech > Speech dictionary: the entries, each with the app it is used in. */
class SpeechDictionaryFragment :
  DictionaryFragment(
    R.string.title_pref_speech_dictionary,
    R.string.speech_dictionary_not_said,
    ItemFiles.Kind.SPEECH_DICTIONARY,
  ) {
  override fun read(): List<DictionaryEntry> = SpeechDictionary.read(prefs)

  override fun write(entries: List<DictionaryEntry>) = SpeechDictionary.write(prefs, entries)
}

/** Braille > Braille dictionary: the entries, each with the app it is used in. */
class BrailleDictionaryFragment :
  DictionaryFragment(
    R.string.title_pref_braille_dictionary,
    R.string.braille_dictionary_not_shown,
    ItemFiles.Kind.BRAILLE_DICTIONARY,
  ) {
  override fun read(): List<DictionaryEntry> = BrailleDictionary.read(prefs)

  override fun write(entries: List<DictionaryEntry>) = BrailleDictionary.write(prefs, entries)
}

/** Symbol dictionary: the entries, each with the app it is used in. */
class SymbolDictionaryFragment :
  DictionaryFragment(
    R.string.title_pref_symbol_dictionary,
    R.string.symbol_dictionary_removed,
    ItemFiles.Kind.SYMBOL_DICTIONARY,
    findHint = R.string.symbol_dictionary_find,
    replaceHint = R.string.symbol_dictionary_replace_with,
    plainText = true,
    maxEntries = SymbolDictionary.MAX_ENTRIES,
  ) {
  override fun read(): List<DictionaryEntry> = SymbolDictionary.read(prefs)

  override fun write(entries: List<DictionaryEntry>) = SymbolDictionary.write(prefs, entries)
}

/**
 * A dictionary's entries, each with the app it is used in. With [plainText], an entry has no
 * Whole words or Regular expression option.
 */
abstract class DictionaryFragment(
  @StringRes private val titleRes: Int,
  @StringRes private val removedText: Int,
  private val kind: ItemFiles.Kind,
  @StringRes private val findHint: Int = R.string.notification_replace_find,
  @StringRes private val replaceHint: Int = R.string.notification_replace_with,
  private val plainText: Boolean = false,
  private val maxEntries: Int = Int.MAX_VALUE,
) : NotificationCodeFragment(titleRes) {

  private val files = ItemFileLauncher(this) { rebuild() }

  protected abstract fun read(): List<DictionaryEntry>

  protected abstract fun write(entries: List<DictionaryEntry>)

  override fun build(screen: PreferenceGroup) {
    val entries = read()
    if (entries.size < maxEntries) {
      screen.action(getString(R.string.notification_add_replacement)) { edit(entries, -1) }
    }
    if (ItemFileLauncher.isAvailable) {
      screen.action(getString(R.string.item_file_import)) { files.importFile() }
      if (entries.isNotEmpty()) {
        screen.action(getString(R.string.item_file_export)) {
          files.export(getString(titleRes)) { ItemFiles.exportDictionary(it, kind) }
        }
      }
    }
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
      replacement.replace.isEmpty() -> getString(removedText, replacement.find)
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
      hint = getString(findHint),
      secondHint = getString(replaceHint),
      secondText = current?.replacement?.replace.orEmpty(),
      checkLabels =
        if (plainText) emptyList()
        else listOf(getString(R.string.notification_whole_words), getString(R.string.notification_regex)),
      checked =
        if (plainText) emptyList()
        else listOf(current?.replacement?.wholeWord ?: true, current?.replacement?.regex == true),
    ) { find, replace, checks ->
      if (find.isEmpty()) return@askText getString(R.string.notification_enter_text)
      if (!plainText && checks[1] && !TextMatch.isValidRegex(find)) {
        return@askText getString(R.string.notification_invalid_regex)
      }
      val replacement =
        if (plainText) Replacement(find, replace, regex = false, wholeWord = false)
        else Replacement(find, replace, regex = checks[1], wholeWord = checks[0])
      save(
        if (current == null) entries + DictionaryEntry(replacement)
        else entries.replace(index, current.copy(replacement = replacement))
      )
      null
    }
  }

  /** Asks for the app an entry is used in, with All apps first, and calls [then] with it. */
  private fun chooseApp(then: (String, String) -> Unit) =
    chooseApp(getString(R.string.speech_dictionary_all_apps), then)

  private fun save(entries: List<DictionaryEntry>) {
    write(entries)
    rebuild()
  }

  private fun <T> List<T>.replace(index: Int, item: T): List<T> =
    mapIndexed { i, old -> if (i == index) item else old }
}
