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

package com.google.android.accessibility.talkback.commands

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.core.widget.doAfterTextChanged
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.google.android.accessibility.material.preference.AccessibilitySuitePreference
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.gesture.AppShortcutActions
import com.google.android.accessibility.talkback.gesture.SwipesBeforeLift
import com.google.android.accessibility.talkback.preference.PreferencesActivityUtils
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment
import com.google.android.accessibility.talkback.preference.search.SettingsSearch
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.widget.KeyboardSafeEditText

/**
 * Commands: a search field, which stays at the top, then one entry per category of commands, or
 * the commands whose names have the words typed in the field.
 */
class CommandsFragment : TalkbackBaseFragment() {

  override fun getTitle(): CharSequence = getString(R.string.title_pref_commands)

  private var categories: List<Commands.Category> = emptyList()

  /** The text of the search field. */
  private var query = ""

  /** The query whose rows are shown, or null before any are. */
  private var shownQuery: String? = null

  private val handler = Handler(Looper.getMainLooper())
  private val update = Runnable { show(announce = true) }

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    preferenceManager.setStorageDeviceProtected()
    val context = requireContext()
    query = savedInstanceState?.getString(STATE_QUERY).orEmpty()
    categories = Commands.categories(context)
    preferenceScreen = preferenceManager.createPreferenceScreen(context)
    show(announce = false)
    AppShortcutActions.refresh(context) {
      if (!isAdded) return@refresh
      val shown = categories.map { it.titleRes }
      categories = Commands.categories(context)
      // The categories, unless one came or went, stay as they are.
      if (SettingsSearch.words(query.trim()).isNotEmpty() || categories.map { it.titleRes } != shown) {
        shownQuery = null
        show(announce = false)
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    val list = super.onCreateView(inflater, container, savedInstanceState)
    val context = requireContext()
    val padding = (16 * resources.displayMetrics.density).toInt()
    val field =
      KeyboardSafeEditText(context).apply {
        hint = getString(R.string.commands_search_hint)
        inputType = InputType.TYPE_CLASS_TEXT
        imeOptions = EditorInfo.IME_ACTION_SEARCH
        isSingleLine = true
        // The fragment keeps the query, and shows its rows.
        isSaveEnabled = false
        setText(query)
        doAfterTextChanged { text ->
          query = text?.toString().orEmpty()
          handler.removeCallbacks(update)
          handler.postDelayed(update, UPDATE_DELAY_MS)
        }
        setOnEditorActionListener { view, actionId, event ->
          when (SettingsSearch.editorAction(actionId, event?.action, event?.repeatCount ?: 0)) {
            SettingsSearch.EditorAction.SEARCH -> {
              handler.removeCallbacks(update)
              show(announce = true)
              (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.hideSoftInputFromWindow(view.windowToken, 0)
              true
            }
            SettingsSearch.EditorAction.CONSUME -> true
            SettingsSearch.EditorAction.IGNORE -> false
          }
        }
      }
    // Outside the list, so that it stays at the top while the list scrolls.
    return LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      addView(
        field,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
          )
          .apply { setMargins(padding, padding / 2, padding, 0) },
      )
      addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(STATE_QUERY, query)
  }

  override fun onDestroyView() {
    handler.removeCallbacks(update)
    super.onDestroyView()
  }

  /**
   * Shows the categories while the search field has no words, otherwise the commands found, unless
   * they are shown already. With [announce], says how many commands are found.
   */
  private fun show(announce: Boolean) {
    val context = context ?: return
    val screen = preferenceScreen ?: return
    val trimmed = query.trim()
    if (trimmed == shownQuery) {
      return
    }
    shownQuery = trimmed
    screen.removeAll()
    if (SettingsSearch.words(trimmed).isEmpty()) {
      addCategories(context, screen)
    } else {
      val found = CommandSearch.find(categories, trimmed)
      for (result in found) {
        screen.addPreference(
          AccessibilitySuitePreference(context).apply {
            key = "pref_commands_found_${result.command.value}"
            isPersistent = false
            isIconSpaceReserved = false
            title = result.command.label
            summary = getString(result.category.titleRes)
            fragment = CommandFragment::class.java.name
            extras.putString(CommandFragment.ARG_COMMAND, result.command.value)
          }
        )
      }
      if (announce) {
        PreferencesActivityUtils.announceText(
          if (found.isEmpty()) getString(R.string.keyboard_shortcuts_no_results)
          else resources.getQuantityString(R.plurals.commands_search_results, found.size, found.size),
          context,
        )
      }
    }
    listView?.scrollToPosition(0)
  }

  private fun addCategories(context: Context, screen: PreferenceScreen) {
    for (category in categories) {
      screen.addPreference(
        AccessibilitySuitePreference(context).apply {
          key = "pref_commands_${category.titleRes}"
          isPersistent = false
          isIconSpaceReserved = false
          title = getString(category.titleRes)
          fragment = CommandCategoryFragment::class.java.name
          extras.putInt(CommandCategoryFragment.ARG_CATEGORY, category.titleRes)
        }
      )
    }
  }

  private companion object {
    const val STATE_QUERY = "commands_search_query"

    /** How long after the last change to the search field its results are shown. */
    const val UPDATE_DELAY_MS = 400L
  }
}

/** The commands of one category, each with the gestures and keyboard shortcut that run it. */
class CommandCategoryFragment : TalkbackBaseFragment() {

  private val categoryTitle: Int
    get() = arguments?.getInt(ARG_CATEGORY) ?: 0

  override fun getTitle(): CharSequence =
    if (categoryTitle != 0) getString(categoryTitle) else getString(R.string.title_pref_commands)

  private var gestures: List<CommandBindings.GestureSlot> = emptyList()

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    preferenceManager.setStorageDeviceProtected()
    gestures = readGestures(this) + CommandBindings.arrowKeys(requireContext())
    preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
    if (categoryTitle == R.string.title_pref_app_shortcuts) {
      AppShortcutActions.refresh(requireContext()) { if (isAdded) fill() }
    }
  }

  override fun onResume() {
    super.onResume()
    // Gestures and shortcuts change on the command screens opened from here.
    fill()
  }

  /**
   * Adds a row for each command the first time, and updates what runs each command after. Rows of
   * commands no longer in the category, such as a removed app shortcut, are removed.
   */
  private fun fill() {
    val context = requireContext()
    val screen = preferenceScreen
    val commands =
      Commands.categories(context).firstOrNull { it.titleRes == categoryTitle }?.commands.orEmpty()
    val prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val manager = CommandBindings.keyComboManager(context)
    val model = manager.keyComboModel
    val keys = HashSet<String>()
    for ((index, command) in commands.withIndex()) {
      val names = gestures.filter { it.value(context, prefs) == command.value }.map { it.name }
      val shortcut =
        model?.let {
          CommandBindings.keyboardShortcutText(
            manager,
            CommandBindings.keyboardKey(context, it, command),
          )
        }
      val summary = (names + listOfNotNull(shortcut)).joinToString(", ").ifEmpty { null }
      val key = "pref_command_${command.value}"
      val row: Preference =
        screen.findPreference(key)
          ?: AccessibilitySuitePreference(context).apply {
            this.key = key
            isPersistent = false
            isIconSpaceReserved = false
            title = command.label
            fragment = CommandFragment::class.java.name
            extras.putString(CommandFragment.ARG_COMMAND, command.value)
            screen.addPreference(this)
          }
      row.summary = summary
      row.order = index
      keys += key
    }
    for (i in screen.preferenceCount - 1 downTo 0) {
      val row = screen.getPreference(i)
      if (row.key !in keys) screen.removePreference(row)
    }
  }

  companion object {
    const val ARG_CATEGORY = "category"
  }
}

/**
 * Returns the gestures that can be assigned, read from the Gestures screen, which [fragment] shows
 * only until it sets its own screen.
 */
internal fun readGestures(fragment: TalkbackBaseFragment): List<CommandBindings.GestureSlot> {
  fragment.addPreferencesFromResource(R.xml.gesture_preferences)
  return CommandBindings.gestures(fragment.requireContext(), fragment.preferenceScreen)
}

/** Returns the categories of the Gestures screen, with their gestures, as [readGestures] reads them. */
internal fun readGestureCategories(
  fragment: TalkbackBaseFragment,
  swipesBeforeLift: Boolean = SwipesBeforeLift.isOnByDefault(fragment.requireContext()),
): List<CommandBindings.GestureCategory> {
  fragment.addPreferencesFromResource(R.xml.gesture_preferences)
  return CommandBindings.gestureCategories(
    fragment.requireContext(),
    fragment.preferenceScreen,
    swipesBeforeLift,
  )
}
