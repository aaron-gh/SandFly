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

package com.google.android.accessibility.talkback.commands

import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceGroup
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.gesture.AppGestures
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping
import com.google.android.accessibility.talkback.keyboard.KeyComboModel
import com.google.android.accessibility.talkback.notifications.NotificationCodeFragment
import com.google.android.accessibility.talkback.notifications.NotificationReader
import com.google.android.accessibility.talkback.preference.base.KeyboardShortcutDialogPreference
import java.util.concurrent.Executors

/**
 * Gestures > Gestures in apps, and Keyboard shortcuts > Keyboard shortcuts in apps: the apps in
 * which gestures and keyboard shortcuts run other commands.
 */
class AppGesturesFragment : NotificationCodeFragment(R.string.title_pref_app_gestures) {

  override fun getTitle(): CharSequence =
    getText(
      if (arguments?.getString(ARG_SHOWN_FROM) == FROM_KEYBOARD_SHORTCUTS) {
        R.string.title_pref_app_keyboard_shortcuts
      } else {
        R.string.title_pref_app_gestures
      }
    )

  override fun build(screen: PreferenceGroup) {
    val apps = AppGestures.read(prefs)
    screen.action(getString(R.string.app_gestures_add_app)) { chooseApp() }
    apps.entries
      .sortedBy { it.value.name.lowercase() }
      .forEach { (packageName, app) ->
        screen.link(app.name, null, AppGestureFragment::class.java) {
          putString(AppGestureFragment.ARG_PACKAGE, packageName)
        }
      }
  }

  private fun chooseApp() {
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
        choose(getString(R.string.notification_choose_app), apps.map { it.second }) {
          val (packageName, name) = apps[it]
          val all = AppGestures.read(prefs)
          if (packageName !in all) {
            AppGestures.write(prefs, all + (packageName to AppGestures.App(name, emptyMap())))
          }
          rebuild()
        }
      }
    }
  }

  companion object {
    /** Which screen the list was opened from, which names it. */
    const val ARG_SHOWN_FROM = "shown_from"
    const val FROM_KEYBOARD_SHORTCUTS = "keyboard_shortcuts"

    private val EXECUTOR = Executors.newSingleThreadExecutor()
  }
}

/** The gestures and keyboard shortcuts that run other commands in one app. */
class AppGestureFragment : NotificationCodeFragment(R.string.title_pref_app_gestures) {

  private val packageName: String by lazy { requireArguments().getString(ARG_PACKAGE).orEmpty() }

  // The gestures that can be assigned, named and in order as on the Gestures screen.
  private var slots: List<CommandBindings.GestureSlot> = emptyList()

  // Records the keys of a new keyboard shortcut, while its dialog is open.
  private var recorder: KeyboardShortcutDialogPreference? = null

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    preferenceManager.setStorageDeviceProtected()
    // Read from the Gestures screen, which the screen built in code then replaces.
    slots = readGestures(this)
    super.onCreatePreferences(savedInstanceState, rootKey)
  }

  private val app: AppGestures.App?
    get() = AppGestures.read(prefs)[packageName]

  override fun getTitle(): CharSequence =
    app?.name ?: NotificationReader.appName(requireContext(), packageName)

  override fun build(screen: PreferenceGroup) {
    val gestures = app?.gestures.orEmpty()
    val gestureCategory = screen.category(R.string.command_gestures)
    gestureCategory.action(getString(R.string.command_add_gesture)) { chooseGesture() }
    for (slot in slots) {
      val value = gestures[baseKey(slot)] ?: continue
      entry(gestureCategory, slot.name, value, { chooseCommand { assignGesture(slot, it) } }) {
        save(gestures = gestures - baseKey(slot))
      }
    }

    val keys = app?.keys.orEmpty()
    val keyCategory = screen.category(R.string.title_pref_manage_keyboard_shortcuts)
    keyCategory.action(getString(R.string.app_keys_add)) { recordKeys() }.apply {
      // Recording keys needs the screen reader's key events.
      isEnabled = TalkBackService.isServiceActive()
    }
    val manager = CommandBindings.keyComboManager(requireContext())
    for ((name, value) in keys) {
      val keyCombo = AppGestures.keyCombo(name) ?: continue
      entry(keyCategory, manager.getKeyComboStringRepresentation(keyCombo), value, { chooseCommand { assignKeys(name, it) } }) {
        save(keys = keys - name)
      }
    }

    screen.action(getString(R.string.app_gestures_remove_app)) {
      AppGestures.write(prefs, AppGestures.read(prefs) - packageName)
      requireActivity().onBackPressedDispatcher.onBackPressed()
    }
  }

  /** Adds a gesture or keyboard shortcut named [name] that runs [value], to change or remove. */
  private fun entry(
    group: PreferenceGroup,
    name: CharSequence,
    value: String,
    change: () -> Unit,
    remove: () -> Unit,
  ) {
    val title = getString(R.string.command_gesture_assigned, name, label(value))
    group.action(title) {
      choose(
        title,
        listOf(getString(R.string.app_gestures_choose_command), getString(R.string.notification_remove)),
      ) {
        if (it == 0) change() else remove()
      }
    }
  }

  private fun label(value: String): String =
    if (value == getString(R.string.shortcut_value_unassigned)) getString(R.string.keycombo_unassigned)
    else Commands.label(requireContext(), value) ?: value

  /** The gesture's key on the Gestures screen, without the gesture set. */
  private fun baseKey(slot: CommandBindings.GestureSlot): String =
    GestureShortcutMapping.getPrefKeyWithGestureSet(slot.key, 0)

  private fun chooseGesture() {
    val context = requireContext()
    val names = slots.map { "${it.name}: ${label(it.value(context, prefs))}" }
    choose(getString(R.string.command_choose_gesture), names) { index ->
      chooseCommand { assignGesture(slots[index], it) }
    }
  }

  /** Records keys, then asks for the command they run in the app. */
  private fun recordKeys() {
    val context = requireContext()
    val dialog = KeyboardShortcutDialogPreference(context, RECORD_KEY, getTitle())
    dialog.setRecordListener { keyCombo ->
      recorder = null
      if (keyCombo.keyComboCode != KeyComboModel.KEY_COMBO_CODE_UNASSIGNED.toLong()) {
        chooseCommand { assignKeys(AppGestures.keyName(keyCombo), it) }
      }
    }
    recorder = dialog
    dialog.showRecordDialog()
  }

  /** Asks for a command, first its group, and gives its value to [then]. */
  private fun chooseCommand(then: (String) -> Unit) {
    val context = requireContext()
    val categories = Commands.categories(context)
    val groups = listOf(getString(R.string.keycombo_unassigned)) + categories.map { getString(it.titleRes) }
    choose(getString(R.string.app_gestures_choose_command), groups) { group ->
      if (group == 0) {
        then(getString(R.string.shortcut_value_unassigned))
        return@choose
      }
      val commands = categories[group - 1].commands
      choose(groups[group], commands.map { it.label }) { then(commands[it].value) }
    }
  }

  private fun assignGesture(slot: CommandBindings.GestureSlot, value: String) {
    save(gestures = app?.gestures.orEmpty() + (baseKey(slot) to value))
  }

  private fun assignKeys(name: String, value: String) {
    save(keys = app?.keys.orEmpty() + (name to value))
  }

  private fun save(
    gestures: Map<String, String> = app?.gestures.orEmpty(),
    keys: Map<String, String> = app?.keys.orEmpty(),
  ) {
    val all = AppGestures.read(prefs)
    val name = all[packageName]?.name ?: NotificationReader.appName(requireContext(), packageName)
    AppGestures.write(prefs, all + (packageName to AppGestures.App(name, gestures, keys)))
    rebuild()
  }

  companion object {
    const val ARG_PACKAGE = "package"

    // The key the recording dialog is made for, which has no shortcut in the keymap.
    private const val RECORD_KEY = "pref_app_keys_record"
  }
}
