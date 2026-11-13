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

package com.google.android.accessibility.talkback.preference.search

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.accessibility.material.preference.AccessibilitySuitePreference
import com.google.android.accessibility.material.preference.AccessibilitySuitePreferenceCategory
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.TalkBackPreferencesActivity.TalkBackSubSettings
import com.google.android.accessibility.utils.preference.BasePreferencesActivity.FRAGMENT_ARGS
import com.google.android.accessibility.utils.preference.BasePreferencesActivity.FRAGMENT_NAME
import com.google.android.accessibility.utils.widget.KeyboardSafeEditText
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.Normalizer
import java.util.Locale
import java.util.function.Consumer
import java.util.function.Predicate

/**
 * Search in settings: Search on the main settings screen finds settings and commands on every
 * screen, and Search on a long screen finds its rows. Search asks for a text in a dialog, and lists
 * what has every word of it in another, to choose from.
 */
object SettingsSearch {

  /** Argument of a settings screen: the key of the preference to scroll to and focus. */
  const val ARG_FOUND_KEY = "settings_search_found_key"

  /** Key of the Search item, which search doesn't list. */
  const val SEARCH_KEY = "pref_settings_search"

  private const val PATH_SEPARATOR = " > "
  private const val DETAILS_LENGTH = 120

  private val MARKS = Regex("\\p{Mn}+")
  private val SPACES = Regex("\\s+")

  /** Something search can find. */
  class Candidate
  @JvmOverloads
  constructor(
    /** Matched first: candidates that have every word in their name are listed first. */
    val name: CharSequence,
    /** Also matched, and listed when the name alone doesn't match, or [alwaysShowDetails]. */
    val details: List<CharSequence?> = emptyList(),
    /** Where it is, listed on its own line. */
    val path: CharSequence? = null,
    val alwaysShowDetails: Boolean = false,
  )

  /** A settings screen that shows the preferences of its XML as rows with other keys. */
  fun interface FoundKey {
    /** Returns the key of the row that shows the XML preference [key]. */
    fun foundKey(key: String): String
  }

  /** Receives the index of the chosen candidate. */
  fun interface OnChoose {
    fun choose(index: Int)
  }

  /** Returns [text] in lower case, without accents, with single spaces, for matching. */
  @JvmStatic
  fun normalize(text: CharSequence?): String {
    if (text.isNullOrEmpty()) {
      return ""
    }
    val decomposed = Normalizer.normalize(text.toString().lowercase(Locale.ROOT), Normalizer.Form.NFKD)
    return SPACES.replace(MARKS.replace(decomposed, ""), " ").trim()
  }

  /** Returns the words of [query], normalized. */
  @JvmStatic
  fun words(query: CharSequence?): List<String> =
    normalize(query).split(' ').filter { it.isNotEmpty() }

  /** Returns whether each of [words] is somewhere in [texts], also inside a word. */
  @JvmStatic
  fun matches(words: List<String>, texts: List<CharSequence?>): Boolean {
    if (words.isEmpty()) {
      return false
    }
    val haystack = texts.joinToString(" ") { normalize(it) }
    return words.all { haystack.contains(it) }
  }

  /**
   * Returns the indexes of the [candidates] that have every one of [words], those with all of them
   * in their names first, each group in the order of [candidates].
   */
  @JvmStatic
  fun find(candidates: List<Candidate>, words: List<String>): List<Int> {
    val inName = mutableListOf<Int>()
    val elsewhere = mutableListOf<Int>()
    candidates.forEachIndexed { index, candidate ->
      when {
        matches(words, listOf(candidate.name)) -> inName += index
        matches(words, listOf(candidate.name) + candidate.details) -> elsewhere += index
      }
    }
    return inName + elsewhere
  }

  /** Returns the text that lists [candidate] among the results for [words]. */
  @JvmStatic
  fun label(candidate: Candidate, words: List<String>): CharSequence {
    val lines = mutableListOf(candidate.name.toString())
    if (candidate.alwaysShowDetails || !matches(words, listOf(candidate.name))) {
      val details = candidate.details.filterNot { it.isNullOrBlank() }.joinToString(", ")
      if (details.isNotEmpty()) {
        lines +=
          if (details.length > DETAILS_LENGTH) details.take(DETAILS_LENGTH).trimEnd() + "…"
          else details
      }
    }
    candidate.path?.takeIf { it.isNotEmpty() }?.let { lines += it.toString() }
    return lines.joinToString("\n")
  }

  /** What the search field does with an editor action. */
  internal enum class EditorAction {
    SEARCH,
    /** Takes the action without searching. */
    CONSUME,
    /** Leaves the action to the field. */
    IGNORE,
  }

  /**
   * Returns what the search field does with the editor action [actionId], which comes with a key
   * event of [keyAction] and [repeatCount] for Enter on a physical keyboard: such an Enter
   * searches when pressed, and is consumed when released, so that it doesn't move focus.
   */
  internal fun editorAction(actionId: Int, keyAction: Int?, repeatCount: Int): EditorAction =
    when (actionId) {
      EditorInfo.IME_ACTION_SEARCH,
      EditorInfo.IME_ACTION_DONE,
      EditorInfo.IME_ACTION_GO -> EditorAction.SEARCH
      EditorInfo.IME_NULL ->
        when {
          keyAction == KeyEvent.ACTION_DOWN && repeatCount == 0 -> EditorAction.SEARCH
          keyAction == KeyEvent.ACTION_DOWN || keyAction == KeyEvent.ACTION_UP ->
            EditorAction.CONSUME
          else -> EditorAction.IGNORE
        }
      else -> EditorAction.IGNORE
    }

  /** Returns [titles] as a path of screens and categories. */
  @JvmStatic fun path(titles: List<CharSequence>): String = titles.joinToString(PATH_SEPARATOR)

  /** Adds Search at the top of [screen], which runs [onClick]. */
  @JvmStatic
  fun addSearchItem(screen: PreferenceGroup, onClick: Runnable) {
    val item = AccessibilitySuitePreference(screen.context)
    item.key = SEARCH_KEY
    item.isPersistent = false
    item.order = -1
    item.setTitle(android.R.string.search_go)
    item.setOnPreferenceClickListener {
      onClick.run()
      true
    }
    screen.addPreference(item)
  }

  /**
   * Asks for a text, and lists the [candidates] that have its words, or says there are no results
   * and keeps asking. [candidates] is read once, when the dialog opens.
   */
  @JvmStatic
  fun search(fragment: Fragment, candidates: () -> List<Candidate>, onChoose: OnChoose) =
    searchLoaded(
      fragment,
      { deliver ->
        deliver(candidates())
        null
      },
      onChoose,
    )

  /**
   * Like [search], with candidates that [load] starts finding when the dialog opens, and passes to
   * its argument on the main thread. [load] returns what stops it when the dialog closes first, or
   * null. A search made before the candidates arrive runs when they do.
   */
  private fun searchLoaded(
    fragment: Fragment,
    load: (deliver: (List<Candidate>) -> Unit) -> Runnable?,
    onChoose: OnChoose,
  ) {
    val activity = fragment.requireActivity()
    val title = activity.getString(android.R.string.search_go)
    val padding = (20 * activity.resources.displayMetrics.density).toInt()
    val edit =
      KeyboardSafeEditText(activity).apply {
        hint = title
        inputType = InputType.TYPE_CLASS_TEXT
        imeOptions = EditorInfo.IME_ACTION_SEARCH
        isSingleLine = true
      }
    val layout =
      FrameLayout(activity).apply {
        setPadding(padding, padding / 2, padding, 0)
        addView(edit, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
      }
    val dialog =
      MaterialAlertDialogBuilder(activity)
        .setTitle(title)
        .setView(layout)
        .setPositiveButton(android.R.string.ok, null)
        .setNegativeButton(android.R.string.cancel, null)
        .create()
    var all: List<Candidate>? = null
    var pending = false
    val run = search@{
      val query = edit.text?.toString()?.trim().orEmpty()
      val words = words(query)
      val loaded = all
      if (words.isNotEmpty() && loaded == null) {
        pending = true
        return@search
      }
      val candidates = loaded.orEmpty()
      // Nothing is found without words, which also says so.
      val found = find(candidates, words)
      if (found.isEmpty()) {
        edit.error = activity.getString(R.string.keyboard_shortcuts_no_results)
      } else {
        dialog.dismiss()
        val labels = found.map { label(candidates[it], words) }.toTypedArray()
        MaterialAlertDialogBuilder(activity)
          .setTitle(query)
          .setItems(labels) { _, which -> onChoose.choose(found[which]) }
          .setNegativeButton(android.R.string.cancel, null)
          .show()
      }
    }
    dialog.setOnShowListener {
      dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener { run() }
    }
    edit.setOnEditorActionListener { _, actionId, event ->
      when (editorAction(actionId, event?.action, event?.repeatCount ?: 0)) {
        EditorAction.SEARCH -> {
          run()
          true
        }
        EditorAction.CONSUME -> true
        EditorAction.IGNORE -> false
      }
    }
    var stop: Runnable? = null
    val closeWithScreen = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_DESTROY) {
        dialog.dismiss()
      }
    }
    dialog.setOnDismissListener {
      stop?.run()
      stop = null
      fragment.lifecycle.removeObserver(closeWithScreen)
    }
    fragment.lifecycle.addObserver(closeWithScreen)
    dialog.show()
    edit.requestFocus()
    stop =
      load { candidates ->
        if (dialog.isShowing && fragment.isAdded) {
          all = candidates
          if (pending) {
            pending = false
            run()
          }
        }
      }
  }

  /**
   * Searches the rows of [fragment]'s screen that [include] accepts, by title and summary, and
   * calls [open] with the row chosen.
   */
  @JvmStatic
  fun searchRows(
    fragment: PreferenceFragmentCompat,
    include: Predicate<Preference>,
    open: Consumer<Preference>,
  ) {
    val rows = mutableListOf<Preference>()
    collectRows(fragment.preferenceScreen ?: return, include, rows)
    search(
      fragment,
      {
        rows.map { row ->
          Candidate(
            row.title ?: "",
            listOf(row.summary),
            alwaysShowDetails = true,
          )
        }
      },
    ) { index ->
      if (fragment.isAdded) {
        open.accept(rows[index])
      }
    }
  }

  private fun collectRows(
    group: PreferenceGroup,
    include: Predicate<Preference>,
    rows: MutableList<Preference>,
  ) {
    for (i in 0 until group.preferenceCount) {
      val preference = group.getPreference(i)
      if (!preference.isVisible) {
        continue
      }
      if (preference is PreferenceGroup) {
        collectRows(preference, include, rows)
      } else if (preference.isEnabled && !preference.title.isNullOrEmpty() && include.test(preference)) {
        rows += preference
      }
    }
  }

  /** Searches every settings screen and command from [fragment], the main settings screen. */
  @JvmStatic
  fun searchSettings(fragment: PreferenceFragmentCompat) {
    val context = fragment.requireContext()
    var entries: List<SettingsIndex.Entry> = emptyList()
    searchLoaded(
      fragment,
      { deliver ->
        SettingsIndex.buildInBackground(context) { built ->
          entries = built
          deliver(built.map { Candidate(it.title, listOfNotNull(it.summary), path(it.path)) })
        }
      },
    ) { index ->
      if (fragment.isAdded) {
        open(fragment, entries[index])
      }
    }
  }

  /** Opens the screen of [entry], with its preference focused. */
  private fun open(fragment: PreferenceFragmentCompat, entry: SettingsIndex.Entry) {
    if (entry.screen == fragment.javaClass.name && entry.args.isEmpty && entry.key != null) {
      show(fragment, entry.key)
      return
    }
    val args = Bundle(entry.args)
    entry.key?.let { args.putString(ARG_FOUND_KEY, it) }
    fragment.startActivity(
      Intent(fragment.requireContext(), TalkBackSubSettings::class.java)
        .putExtra(FRAGMENT_NAME, entry.screen)
        .putExtra(FRAGMENT_ARGS, args)
    )
  }

  /**
   * Scrolls to and focuses the preference that search opened [fragment]'s screen for, if it did.
   * Call once the fragment's view is created.
   */
  @JvmStatic
  fun showFound(fragment: PreferenceFragmentCompat) {
    val args = fragment.arguments ?: return
    val key = args.getString(ARG_FOUND_KEY) ?: return
    // Only when the screen first opens, not when it is created again.
    args.remove(ARG_FOUND_KEY)
    show(fragment, key)
  }

  private fun show(fragment: PreferenceFragmentCompat, key: String) {
    val preference =
      fragment.findPreference<Preference>((fragment as? FoundKey)?.foundKey(key) ?: key) ?: return
    var parent = preference.parent
    while (parent != null) {
      if (parent is AccessibilitySuitePreferenceCategory) {
        parent.setCollapsed(false)
      }
      parent = parent.parent
    }
    val list = fragment.listView ?: return
    // After the list shows the rows that expanding the categories showed.
    list.post {
      val adapter = list.adapter as? PreferenceGroup.PreferencePositionCallback ?: return@post
      val position = adapter.getPreferenceAdapterPosition(preference)
      if (position < 0) {
        return@post
      }
      list.addOnLayoutChangeListener(
        object : View.OnLayoutChangeListener {
          override fun onLayoutChange(
            v: View,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int,
          ) {
            list.removeOnLayoutChangeListener(this)
            list
              .findViewHolderForAdapterPosition(position)
              ?.itemView
              ?.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
          }
        }
      )
      val layoutManager = list.layoutManager
      if (layoutManager is LinearLayoutManager) {
        layoutManager.scrollToPositionWithOffset(position, 0)
      } else {
        list.scrollToPosition(position)
      }
      list.requestLayout()
    }
  }
}
