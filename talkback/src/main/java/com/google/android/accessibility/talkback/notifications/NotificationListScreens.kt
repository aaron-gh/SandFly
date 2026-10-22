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

package com.google.android.accessibility.talkback.notifications

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.text.format.DateFormat
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Date
import java.util.concurrent.Executors

/**
 * A notification settings screen built in code, which builds itself again each time it shows, so
 * that it shows what a screen opened from it changed.
 */
abstract class NotificationCodeFragment(@StringRes private val title: Int) : TalkbackBaseFragment() {
  protected lateinit var prefs: SharedPreferences
  protected val settings: NotificationSettings
    get() = NotificationSettings.read(prefs)

  override fun getTitle(): CharSequence = getText(title)

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    preferenceManager.setStorageDeviceProtected()
    prefs = SharedPreferencesUtils.getSharedPreferences(requireContext())
    preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
  }

  override fun onResume() {
    super.onResume()
    rebuild()
  }

  /** Builds the screen again, keeping the list where it was. */
  protected fun rebuild() {
    val screen = preferenceScreen
    screen.removeAll()
    build(screen)
  }

  protected abstract fun build(screen: PreferenceGroup)

  protected fun PreferenceGroup.category(@StringRes title: Int): PreferenceCategory =
    PreferenceCategory(context).also {
      it.setTitle(title)
      it.isIconSpaceReserved = false
      addPreference(it)
    }

  protected fun PreferenceGroup.action(
    title: CharSequence,
    summary: CharSequence? = null,
    onClick: () -> Unit,
  ): Preference =
    Preference(context).also {
      it.title = title
      it.summary = summary
      it.isPersistent = false
      it.isIconSpaceReserved = false
      it.layoutResource = R.layout.listitem_2texts
      it.setOnPreferenceClickListener {
        onClick()
        true
      }
      addPreference(it)
    }

  protected fun PreferenceGroup.link(
    title: CharSequence,
    summary: CharSequence?,
    screen: Class<out Fragment>,
    arguments: Bundle.() -> Unit,
  ): Preference =
    Preference(context).also {
      it.title = title
      it.summary = summary
      it.isPersistent = false
      it.isIconSpaceReserved = false
      it.layoutResource = R.layout.listitem_2texts
      it.fragment = screen.name
      it.extras.arguments()
      addPreference(it)
    }

  protected fun PreferenceGroup.switch(
    title: CharSequence,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
  ): CheckBoxPreference =
    CheckBoxPreference(context).also {
      it.title = title
      it.isChecked = checked
      it.isPersistent = false
      it.isIconSpaceReserved = false
      it.layoutResource = R.layout.listitem_2texts
      it.setOnPreferenceChangeListener { _, value ->
        onChange(value as Boolean)
        true
      }
      addPreference(it)
    }

  protected fun dialog(): MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(requireActivity())

  /** Asks for a choice of [items], with [checked] chosen, and calls [onChoose] with its index. */
  protected fun choose(title: CharSequence, items: List<CharSequence>, checked: Int = -1, onChoose: (Int) -> Unit) {
    val builder = dialog().setTitle(title)
    if (checked >= 0) {
      builder.setSingleChoiceItems(items.toTypedArray(), checked) { dialog, which ->
        dialog.dismiss()
        onChoose(which)
      }
    } else {
      builder.setItems(items.toTypedArray()) { _, which -> onChoose(which) }
    }
    builder.setNegativeButton(android.R.string.cancel, null).show()
  }

  /**
   * Asks for a text, with [text] filled in, and an optional switch such as "Regular expression".
   * [onDone] gets the text and the switch, and returns an error to show, or null to close.
   */
  protected fun askText(
    title: CharSequence,
    text: String = "",
    hint: CharSequence? = null,
    message: CharSequence? = null,
    checkLabels: List<CharSequence> = emptyList(),
    checked: List<Boolean> = emptyList(),
    secondHint: CharSequence? = null,
    secondText: String = "",
    onDone: (String, String, List<Boolean>) -> String?,
  ) {
    val context = requireContext()
    val padding = (20 * resources.displayMetrics.density).toInt()
    val layout =
      LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(padding, padding / 2, padding, 0)
      }
    val edit =
      EditText(context).apply {
        setText(text)
        // The title labels the field when there is no hint.
        this.hint = hint ?: title
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        setSelection(text.length)
      }
    layout.addView(edit, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    val second =
      secondHint?.let {
        EditText(context).apply {
          setText(secondText)
          this.hint = it
          inputType = InputType.TYPE_CLASS_TEXT
        }.also { layout.addView(it, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) }
      }
    val boxes =
      checkLabels.mapIndexed { index, label ->
        CheckBox(context).apply {
          this.text = label
          isChecked = checked.getOrElse(index) { false }
        }.also { layout.addView(it) }
      }
    val dialog =
      dialog()
        .setTitle(title)
        .apply { if (message != null) setMessage(message) }
        .setView(layout)
        .setPositiveButton(android.R.string.ok, null)
        .setNegativeButton(android.R.string.cancel, null)
        .create()
    dialog.setOnShowListener {
      dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
        val error = onDone(edit.text.toString().trim(), second?.text?.toString().orEmpty(), boxes.map { it.isChecked })
        if (error == null) dialog.dismiss() else edit.error = error
      }
    }
    dialog.show()
    edit.requestFocus()
  }

  protected fun confirm(message: CharSequence, @StringRes button: Int, onYes: () -> Unit) {
    dialog()
      .setMessage(message)
      .setPositiveButton(button) { _, _ -> onYes() }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  protected fun toast(@StringRes text: Int) {
    Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
  }

  protected fun saveApps(apps: Map<String, AppSettings>) {
    prefs.edit().putString(NotificationSettings.Companion.Keys.APPS, NotificationSettings.appsToJson(apps)).apply()
  }

  protected fun setApp(packageName: String, change: (AppSettings) -> AppSettings) {
    val apps = settings.apps.toMutableMap()
    apps[packageName] = change(apps[packageName] ?: AppSettings())
    saveApps(apps)
  }

  companion object {
    const val ARG_PACKAGE = "package"

    fun modeText(context: Context, mode: AppMode): String =
      context.getString(
        when (mode) {
          AppMode.DEFAULT -> R.string.notification_app_mode_default
          AppMode.READ -> R.string.notification_app_mode_read
          AppMode.PRIVATE -> R.string.notification_app_mode_private
          AppMode.PRIORITY -> R.string.notification_app_mode_priority
          AppMode.IGNORE -> R.string.notification_app_mode_ignore
        }
      )

    fun outcomeText(context: Context, outcome: Outcome): String =
      context.getString(
        when (outcome) {
          Outcome.READ -> R.string.notification_outcome_read
          Outcome.STOPPED -> R.string.notification_outcome_stopped
          Outcome.SPEECH_OFF -> R.string.notification_outcome_speech_off
          Outcome.DO_NOT_DISTURB -> R.string.notification_outcome_dnd
          Outcome.TOUCHING -> R.string.notification_outcome_touching
          Outcome.APP_IGNORED -> R.string.notification_outcome_app
          Outcome.ONGOING -> R.string.notification_outcome_ongoing
          Outcome.FOREGROUND_SERVICE -> R.string.notification_outcome_foreground
          Outcome.LOW_PRIORITY -> R.string.notification_outcome_low_priority
          Outcome.SYSTEM -> R.string.notification_outcome_system
          Outcome.MEDIA -> R.string.notification_outcome_media
          Outcome.GROUP_SUMMARY -> R.string.notification_outcome_group_summary
          Outcome.PROGRESS -> R.string.notification_outcome_progress
          Outcome.REQUIRED_WORDS_MISSING -> R.string.notification_outcome_required
          Outcome.BLOCKED_WORD -> R.string.notification_outcome_blocked
          Outcome.EMPTY -> R.string.notification_outcome_empty
          Outcome.REPEAT -> R.string.notification_outcome_repeat
          Outcome.COOLDOWN -> R.string.notification_outcome_cooldown
          Outcome.QUIET_HOURS -> R.string.notification_outcome_quiet_hours
          Outcome.SCREEN_ON -> R.string.notification_outcome_screen_on
          Outcome.SCREEN_OFF -> R.string.notification_outcome_screen_off
          Outcome.LOCKED -> R.string.notification_outcome_locked
          Outcome.HEADPHONES_CONNECTED -> R.string.notification_outcome_headphones
          Outcome.HEADPHONES_NOT_CONNECTED -> R.string.notification_outcome_no_headphones
          Outcome.RINGER_SILENT -> R.string.notification_outcome_silent
          Outcome.RINGER_VIBRATE -> R.string.notification_outcome_vibrate
          Outcome.MEDIA_PLAYING -> R.string.notification_outcome_media_playing
          Outcome.CAR -> R.string.notification_outcome_car
          Outcome.RULE -> R.string.notification_outcome_rule
          Outcome.BUSY -> R.string.notification_outcome_busy
          Outcome.CALL -> R.string.notification_outcome_call
        }
      )
  }
}

/** Notifications > Apps: how each app's notifications are read, and other apps'. */
class NotificationAppsFragment : NotificationCodeFragment(R.string.notification_apps_title) {
  private var installed: List<Pair<String, String>>? = null

  override fun build(screen: PreferenceGroup) {
    val context = requireContext()
    val settings = settings
    val others = listOf(AppMode.READ, AppMode.PRIVATE, AppMode.IGNORE)
    screen.action(getString(R.string.notification_other_apps_title), modeText(context, settings.otherApps)) {
      choose(getString(R.string.notification_other_apps_title), others.map { modeText(context, it) }, others.indexOf(settings.otherApps)) {
        prefs.edit().putString(NotificationSettings.Companion.Keys.OTHER_APPS, others[it].name).apply()
        rebuild()
      }
    }
    val apps = screen.category(R.string.notification_apps_category)
    val list = installed
    if (list == null) {
      apps.action(getString(R.string.notification_apps_loading)) {}
      loadApps(context)
      return
    }
    for ((packageName, label) in list) {
      val app = settings.app(packageName)
      val summary =
        listOfNotNull(
            modeText(context, app.mode),
            app.spokenName.takeIf { it.isNotEmpty() }?.let { getString(R.string.notification_app_said_as, it) },
          )
          .joinToString(", ")
      apps.link(label, summary, NotificationAppFragment::class.java) { putString(ARG_PACKAGE, packageName) }
    }
  }

  /** Lists the launchable apps, the apps with settings, and those in the history, by name. */
  private fun loadApps(context: Context) {
    val appContext = context.applicationContext
    val known = settings.apps.keys + NotificationHistory.all().map { it.info.packageName }
    EXECUTOR.execute {
      val pm = appContext.packageManager
      val launchable =
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
          .map { it.activityInfo.packageName }
      val list =
        (launchable + known)
          .distinct()
          .filter { it != appContext.packageName }
          .map { it to NotificationReader.appName(appContext, it) }
          .sortedBy { it.second.lowercase() }
      activity?.runOnUiThread {
        if (!isAdded) return@runOnUiThread
        installed = list
        rebuild()
      }
    }
  }

  private companion object {
    val EXECUTOR = Executors.newSingleThreadExecutor()
  }
}

/** The notification settings of one app. */
class NotificationAppFragment : NotificationCodeFragment(R.string.notification_apps_title) {
  private val packageName: String by lazy { requireArguments().getString(ARG_PACKAGE).orEmpty() }

  override fun getTitle(): CharSequence = NotificationReader.appName(requireContext(), packageName)

  override fun build(screen: PreferenceGroup) {
    val context = requireContext()
    val app = settings.app(packageName)
    val modes = AppMode.entries
    screen.action(getString(R.string.notification_app_mode_title), modeText(context, app.mode)) {
      choose(getString(R.string.notification_app_mode_title), modes.map { modeText(context, it) }, modes.indexOf(app.mode)) { index ->
        setApp(packageName) { it.copy(mode = modes[index]) }
        rebuild()
      }
    }
    screen.action(
      getString(R.string.notification_app_name_title),
      app.spokenName.ifEmpty { NotificationReader.appName(context, packageName) },
    ) {
      askText(getString(R.string.notification_app_name_title), app.spokenName, NotificationReader.appName(context, packageName)) { text, _, _ ->
        setApp(packageName) { it.copy(spokenName = text) }
        rebuild()
        null
      }
    }
    val cooldowns = listOf<Int?>(null) + (NotificationChoices.of(context, NotificationSettings.Companion.Keys.COOLDOWN)?.map { it.first } ?: emptyList())
    fun cooldownText(value: Int?) =
      if (value == null) getString(R.string.notification_app_mode_default) else NotificationChoices.duration(context, value)
    screen.action(getString(R.string.notification_cooldown_title), cooldownText(app.cooldownSeconds)) {
      choose(getString(R.string.notification_cooldown_title), cooldowns.map { cooldownText(it) }, cooldowns.indexOf(app.cooldownSeconds)) { index ->
        setApp(packageName) { it.copy(cooldownSeconds = cooldowns[index]) }
        rebuild()
      }
    }
    screen.action(
      getString(R.string.notification_format_title),
      app.format.ifEmpty { getString(R.string.notification_app_mode_default) },
    ) {
      askText(
        getString(R.string.notification_format_title),
        app.format.ifEmpty { settings.format },
        message = getString(R.string.notification_format_message),
      ) { text, _, _ ->
        setApp(packageName) { it.copy(format = if (text == settings.format) "" else text) }
        rebuild()
        null
      }
    }
    if (!app.isEmpty) {
      screen.action(getString(R.string.notification_app_reset)) {
        val apps = settings.apps.toMutableMap()
        apps.remove(packageName)
        saveApps(apps)
        rebuild()
      }
    }
  }
}

/** Notifications > Words: words that are needed, that block, that keep private, and replacements. */
class NotificationWordsFragment : NotificationCodeFragment(R.string.notification_words_title) {
  override fun build(screen: PreferenceGroup) {
    val settings = settings
    matchList(screen, R.string.notification_required_title, NotificationSettings.Companion.Keys.REQUIRED, settings.requiredWords)
    matchList(screen, R.string.notification_blocked_title, NotificationSettings.Companion.Keys.BLOCKED, settings.blockedWords)
    matchList(screen, R.string.notification_private_title, NotificationSettings.Companion.Keys.PRIVATE, settings.privateWords)
    val replacements = screen.category(R.string.notification_replacements_title)
    replacements.action(getString(R.string.notification_add_replacement)) { editReplacement(settings.replacements, -1) }
    settings.replacements.forEachIndexed { index, replacement ->
      val title = getString(R.string.notification_replacement_item, replacement.find, replacement.replace)
      replacements.action(title, if (replacement.regex) getString(R.string.notification_regex) else null) {
        choose(title, listOf(getString(R.string.notification_edit), getString(R.string.notification_remove))) {
          if (it == 0) editReplacement(settings.replacements, index)
          else saveReplacements(settings.replacements.filterIndexed { i, _ -> i != index })
        }
      }
    }
  }

  private fun matchList(screen: PreferenceGroup, @StringRes title: Int, key: String, list: List<TextMatch>) {
    val category = screen.category(title)
    category.action(getString(R.string.notification_add_word)) { editMatch(key, list, -1) }
    list.forEachIndexed { index, match ->
      category.action(match.text, if (match.regex) getString(R.string.notification_regex) else null) {
        choose(match.text, listOf(getString(R.string.notification_edit), getString(R.string.notification_remove))) {
          if (it == 0) editMatch(key, list, index)
          else saveMatches(key, list.filterIndexed { i, _ -> i != index })
        }
      }
    }
  }

  private fun editMatch(key: String, list: List<TextMatch>, index: Int) {
    val current = list.getOrNull(index)
    askText(
      getString(if (current == null) R.string.notification_add_word else R.string.notification_edit),
      current?.text.orEmpty(),
      checkLabels = listOf(getString(R.string.notification_regex)),
      checked = listOf(current?.regex == true),
    ) { text, _, checks ->
      if (text.isEmpty()) return@askText getString(R.string.notification_enter_text)
      if (checks[0] && !TextMatch.isValidRegex(text)) return@askText getString(R.string.notification_invalid_regex)
      val match = TextMatch(text, checks[0])
      saveMatches(key, if (index < 0) list + match else list.mapIndexed { i, m -> if (i == index) match else m })
      null
    }
  }

  private fun editReplacement(list: List<Replacement>, index: Int) {
    val current = list.getOrNull(index)
    askText(
      getString(if (current == null) R.string.notification_add_replacement else R.string.notification_edit),
      current?.find.orEmpty(),
      hint = getString(R.string.notification_replace_find),
      secondHint = getString(R.string.notification_replace_with),
      secondText = current?.replace.orEmpty(),
      checkLabels = listOf(getString(R.string.notification_whole_words), getString(R.string.notification_regex)),
      checked = listOf(current?.wholeWord ?: true, current?.regex == true),
    ) { find, replace, checks ->
      if (find.isEmpty()) return@askText getString(R.string.notification_enter_text)
      if (checks[1] && !TextMatch.isValidRegex(find)) return@askText getString(R.string.notification_invalid_regex)
      val replacement = Replacement(find, replace, regex = checks[1], wholeWord = checks[0])
      saveReplacements(if (index < 0) list + replacement else list.mapIndexed { i, r -> if (i == index) replacement else r })
      null
    }
  }

  private fun saveMatches(key: String, list: List<TextMatch>) {
    prefs.edit().putString(key, NotificationSettings.matchesToJson(list)).apply()
    rebuild()
  }

  private fun saveReplacements(list: List<Replacement>) {
    prefs.edit().putString(NotificationSettings.Companion.Keys.REPLACEMENTS, NotificationSettings.replacementsToJson(list)).apply()
    rebuild()
  }
}

/**
 * Notifications > History: the notifications that arrived since SandFly started, whether each
 * was read, and settings made from one.
 */
class NotificationHistoryFragment : NotificationCodeFragment(R.string.notification_history_title) {
  private val onChange: () -> Unit = { if (isResumed) rebuild() }

  override fun onResume() {
    super.onResume()
    NotificationHistory.addListener(onChange)
  }

  override fun onPause() {
    NotificationHistory.removeListener(onChange)
    super.onPause()
  }

  override fun build(screen: PreferenceGroup) {
    val context = requireContext()
    screen.switch(getString(R.string.notification_history_keep), settings.history) {
      prefs.edit().putBoolean(NotificationSettings.Companion.Keys.HISTORY, it).apply()
      if (!it) NotificationHistory.clear()
    }
    val entries = NotificationHistory.all()
    if (entries.isNotEmpty()) {
      screen.action(getString(R.string.notification_history_clear)) {
        NotificationHistory.clear()
      }
    }
    val list = screen.category(R.string.notification_history_category)
    if (entries.isEmpty()) {
      list.action(getString(R.string.notification_history_empty)) {}
      return
    }
    val time = DateFormat.getTimeFormat(context)
    for (entry in entries) {
      val title = listOf(entry.info.appName, entry.info.title).filter { it.isNotBlank() }.joinToString(": ")
      val summary =
        listOf(time.format(Date(entry.arrivedAtMillis)), outcomeText(context, entry.outcome), entry.spoken)
          .filter { it.isNotBlank() }
          .joinToString(". ")
      list.action(title, summary) { entryActions(entry) }
    }
  }

  private fun entryActions(entry: NotificationHistory.Entry) {
    val context = requireContext()
    val app = entry.info.appName
    val packageName = entry.info.packageName
    val similar = entry.info.text.ifBlank { entry.info.title }
    val actions =
      listOf<Pair<CharSequence, () -> Unit>>(
        getString(R.string.notification_history_ignore_app, app) to {
          setApp(packageName) { it.copy(mode = AppMode.IGNORE) }
          toast(R.string.notification_saved)
        },
        getString(R.string.notification_history_private_app, app) to {
          setApp(packageName) { it.copy(mode = AppMode.PRIVATE) }
          toast(R.string.notification_saved)
        },
        getString(R.string.notification_history_priority_app, app) to {
          setApp(packageName) { it.copy(mode = AppMode.PRIORITY) }
          toast(R.string.notification_saved)
        },
        getString(R.string.notification_history_block_similar) to {
          val pattern = NotificationText.similarPattern(similar)
          val blocked = settings.blockedWords + TextMatch(pattern, regex = true)
          prefs.edit().putString(NotificationSettings.Companion.Keys.BLOCKED, NotificationSettings.matchesToJson(blocked)).apply()
          toast(R.string.notification_saved)
        },
        getString(R.string.notification_history_copy) to {
          context.getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText(app, listOf(entry.info.title, entry.info.content).filter { it.isNotBlank() }.joinToString("\n")))
          toast(R.string.notification_copied)
        },
      )
    choose(listOf(app, entry.info.title).filter { it.isNotBlank() }.joinToString(": "), actions.map { it.first }) {
      actions[it].second()
    }
  }
}
