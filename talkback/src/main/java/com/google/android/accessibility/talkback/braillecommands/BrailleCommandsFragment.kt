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
package com.google.android.accessibility.talkback.braillecommands

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceGroup
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.commands.Commands
import com.google.android.accessibility.talkback.notifications.NotificationCodeFragment
import com.google.android.accessibility.talkback.notifications.NotificationReader
import com.google.android.accessibility.talkback.preference.PreferencesActivityUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Records a braille cell on the screen and gives it a command in a set of braille commands, after
 * asking when the cell already runs another command there.
 */
class BrailleCellAssigner(private val fragment: Fragment, private val onChanged: () -> Unit) {

  private val context: Context
    get() = fragment.requireContext()

  private val prefs
    get() = SharedPreferencesUtils.getSharedPreferences(context)

  /** Whether cells can be recorded: SandFly shows the braille surface they are typed on. */
  val canRecord: Boolean
    get() = TalkBackService.isServiceActive()

  /** Shows the braille surface, and gives the cell typed on it to [then]. */
  fun record(then: (String) -> Unit) {
    val input = TalkBackService.getInstance()?.brailleScreenInput
    val shown =
      input != null &&
        input.recordCell { cell -> if (cell != null && fragment.isAdded) then(cell) }
    if (!shown) {
      PreferencesActivityUtils.announceText(
        context.getString(R.string.braille_app_not_available),
        context,
      )
    }
  }

  /**
   * Makes [cell] run [value] in [set], first asking whether to replace the command it runs there.
   * [replacing] is a cell to take out of the set, when the command moves from it to [cell].
   */
  fun assign(set: String, cell: String, value: String, replacing: String? = null) {
    val current = BrailleCommands.collision(context, prefs, set, cell)
    if (current == null || current == value || cell == replacing) {
      save(set, cell, value, replacing)
      return
    }
    MaterialAlertDialogBuilder(fragment.requireActivity())
      .setMessage(
        context.getString(
          R.string.command_replace_gesture,
          cellName(context, cell),
          label(context, current),
          label(context, value),
        )
      )
      .setPositiveButton(R.string.command_replace) { _, _ -> save(set, cell, value, replacing) }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun save(set: String, cell: String, value: String, replacing: String?) {
    if (replacing != null && replacing != cell) BrailleCommands.remove(context, prefs, set, replacing)
    BrailleCommands.assign(context, prefs, set, cell, value)
    onChanged()
    PreferencesActivityUtils.announceText(
      context.getString(R.string.command_gesture_assigned, cellName(context, cell), label(context, value)),
      context,
    )
  }

  companion object {
    /** The name of [cell], such as "Dots 1 2 5". */
    @JvmStatic
    fun cellName(context: Context, cell: String): String =
      context.resources.getQuantityString(
        R.plurals.braille_command_cell,
        cell.length,
        cell.toList().joinToString(" "),
      )

    /** The name of the command [value], or Unassigned. */
    @JvmStatic
    fun label(context: Context, value: String): String =
      if (value == context.getString(R.string.shortcut_value_unassigned)) {
        context.getString(R.string.keycombo_unassigned)
      } else {
        Commands.label(context, value) ?: value
      }
  }
}

/** The cells of one set of braille commands, to add to, change and remove. */
abstract class BrailleCellsFragment(title: Int) : NotificationCodeFragment(title) {

  protected val assigner by lazy { BrailleCellAssigner(this) { rebuild() } }

  /** The set whose cells the screen shows. */
  protected abstract val set: String

  /** Adds Add braille command and a row for each cell of the set. */
  protected fun addCells(screen: PreferenceGroup) {
    val context = requireContext()
    screen.action(getString(R.string.braille_commands_add)) {
      chooseCommand { value -> assigner.record { cell -> assigner.assign(set, cell, value) } }
    }.apply { isEnabled = assigner.canRecord }
    val cells = BrailleCommands.cells(context, BrailleCommands.read(prefs), set)
    for (cell in cells.keys.sorted()) {
      val value = cells.getValue(cell)
      val title =
        getString(
          R.string.command_gesture_assigned,
          BrailleCellAssigner.cellName(context, cell),
          BrailleCellAssigner.label(context, value),
        )
      screen.action(title) {
        val choices =
          listOfNotNull(
            getString(R.string.app_gestures_choose_command) to { ->
              chooseCommand { assigner.assign(set, cell, it) }
            },
            (getString(R.string.braille_command_change_cell) to { ->
                assigner.record { newCell -> assigner.assign(set, newCell, value, replacing = cell) }
              })
              .takeIf { assigner.canRecord },
            getString(R.string.notification_remove) to { ->
              BrailleCommands.remove(context, prefs, set, cell)
              rebuild()
            },
          )
        choose(title, choices.map { it.first }) { choices[it].second() }
      }
    }
  }

  /**
   * Asks for a command, first its group, and gives its value to [then]. Sets other than Default
   * offer Unassigned, which stops a cell running Default's command.
   */
  private fun chooseCommand(then: (String) -> Unit) {
    val categories = Commands.categories(requireContext())
    val unassigned = set != BrailleCommands.DEFAULT_SET
    val groups =
      (if (unassigned) listOf(getString(R.string.keycombo_unassigned)) else emptyList()) +
        categories.map { getString(it.titleRes) }
    choose(getString(R.string.app_gestures_choose_command), groups) { group ->
      val index = if (unassigned) group - 1 else group
      if (index < 0) {
        then(getString(R.string.shortcut_value_unassigned))
        return@choose
      }
      val commands = categories[index].commands
      choose(groups[group], commands.map { it.label }) { then(commands[it].value) }
    }
  }
}

/**
 * Braille > Braille commands: the profile in use, Default's cells, and links to the cells of apps
 * and profiles.
 */
class BrailleCommandsFragment : BrailleCellsFragment(R.string.title_braille_commands) {

  override val set: String = BrailleCommands.DEFAULT_SET

  override fun build(screen: PreferenceGroup) {
    val context = requireContext()
    val choices = BrailleCommands.choices(context, prefs)
    if (choices.size > 1) {
      val activeId = BrailleCommands.activeId(prefs)
      val active = choices.indexOfFirst { it.first == activeId }.coerceAtLeast(0)
      screen.action(getString(R.string.title_pref_braille_command_profile_in_use), choices[active].second) {
        choose(getString(R.string.title_pref_braille_command_profile_in_use), choices.map { it.second }, active) {
          BrailleCommands.select(context, prefs, choices[it].first)
          rebuild()
        }
      }
    }
    screen.link(getString(R.string.title_braille_commands_in_apps), null, BrailleCommandAppsFragment::class.java) {}
    screen.link(getString(R.string.title_braille_command_profiles), null, BrailleCommandProfilesFragment::class.java) {}
    if (BrailleCommands.read(prefs).defaults != null) {
      screen.action(getString(R.string.menu_reset)) {
        confirm(getString(R.string.braille_commands_reset_confirm), R.string.menu_reset) {
          BrailleCommands.resetDefaults(prefs)
          rebuild()
        }
      }
    }
    addCells(screen)
  }
}

/** Braille commands > Braille commands in apps: the apps in which cells run other commands. */
class BrailleCommandAppsFragment : NotificationCodeFragment(R.string.title_braille_commands_in_apps) {

  override fun build(screen: PreferenceGroup) {
    screen.action(getString(R.string.app_gestures_add_app)) {
      chooseApp { packageName, name ->
        val config = BrailleCommands.read(prefs)
        if (packageName !in config.apps) {
          BrailleCommands.write(
            prefs,
            config.copy(apps = config.apps + (packageName to BrailleCommands.App(name, emptyMap()))),
          )
        }
        rebuild()
      }
    }
    BrailleCommands.read(prefs)
      .apps
      .entries
      .sortedBy { it.value.name.lowercase() }
      .forEach { (packageName, app) ->
        screen.link(app.name, null, BrailleCommandSetFragment::class.java) {
          putString(BrailleCommandSetFragment.ARG_SET, BrailleCommands.appSet(packageName))
        }
      }
  }
}

/** Braille commands > Braille command profiles: the profiles to add, change or delete. */
class BrailleCommandProfilesFragment :
  NotificationCodeFragment(R.string.title_braille_command_profiles) {

  override fun build(screen: PreferenceGroup) {
    screen.action(getString(R.string.braille_command_profile_add)) { add() }
    for (profile in BrailleCommands.read(prefs).profiles) {
      screen.link(profile.name, null, BrailleCommandSetFragment::class.java) {
        putString(BrailleCommandSetFragment.ARG_SET, BrailleCommands.profileSet(profile.id))
      }
    }
  }

  private fun add() {
    val profiles = BrailleCommands.read(prefs).profiles
    var number = profiles.size + 1
    while (nameTaken(profiles, getString(R.string.braille_command_profile_new_name, number), null)) {
      number++
    }
    val suggested = getString(R.string.braille_command_profile_new_name, number)
    askText(getString(R.string.braille_command_profile_add), hint = suggested) { entered, _, _ ->
      val name = entered.ifEmpty { suggested }
      val config = BrailleCommands.read(prefs)
      if (nameTaken(config.profiles, name, null)) {
        getString(R.string.braille_command_profile_name_taken, name)
      } else {
        BrailleCommands.write(
          prefs,
          config.copy(
            profiles = config.profiles + BrailleCommands.Profile(BrailleCommands.newId(), name, emptyMap())
          ),
        )
        rebuild()
        null
      }
    }
  }
}

/** The cells of one app or profile, and the app's or profile's own buttons. */
class BrailleCommandSetFragment : BrailleCellsFragment(R.string.title_braille_commands) {

  override val set: String by lazy { requireArguments().getString(ARG_SET).orEmpty() }

  private val packageName: String?
    get() = BrailleCommands.packageOf(set)

  private val profile: BrailleCommands.Profile?
    get() = BrailleCommands.profileOf(set)?.let { id -> BrailleCommands.read(prefs).profiles.firstOrNull { it.id == id } }

  override fun getTitle(): CharSequence {
    packageName?.let { packageName ->
      return BrailleCommands.read(prefs).apps[packageName]?.name
        ?: NotificationReader.appName(requireContext(), packageName)
    }
    return profile?.name ?: getText(R.string.title_braille_commands)
  }

  override fun build(screen: PreferenceGroup) {
    val packageName = packageName
    if (packageName != null) {
      screen.action(getString(R.string.app_gestures_remove_app)) {
        val config = BrailleCommands.read(prefs)
        BrailleCommands.write(prefs, config.copy(apps = config.apps - packageName))
        activity?.onBackPressedDispatcher?.onBackPressed()
      }
    } else {
      val profile = profile
      if (profile == null) {
        // Deleted, or restored settings without it.
        activity?.onBackPressedDispatcher?.onBackPressed()
        return
      }
      screen.action(getString(R.string.voice_profile_rename)) { rename() }
      screen.action(getString(R.string.braille_command_profile_delete)) {
        confirm(getString(R.string.voice_profile_delete_confirm, profile.name), R.string.voice_profile_delete) {
          val config = BrailleCommands.read(prefs)
          BrailleCommands.write(prefs, config.copy(profiles = config.profiles.filterNot { it.id == profile.id }))
          if (prefs.getString(BrailleCommands.ACTIVE_KEY, "") == profile.id) {
            BrailleCommands.select(requireContext(), prefs, "")
          }
          activity?.onBackPressedDispatcher?.onBackPressed()
        }
      }
    }
    addCells(screen)
  }

  private fun rename() {
    val profile = profile ?: return
    askText(getString(R.string.voice_profile_rename), profile.name, hint = getString(R.string.title_pref_voice_profile_name)) {
      name, _, _ ->
      val config = BrailleCommands.read(prefs)
      when {
        name.isEmpty() -> ""
        nameTaken(config.profiles, name, profile.id) -> getString(R.string.braille_command_profile_name_taken, name)
        else -> {
          BrailleCommands.write(
            prefs,
            config.copy(profiles = config.profiles.map { if (it.id == profile.id) it.copy(name = name) else it }),
          )
          requireActivity().title = name
          rebuild()
          null
        }
      }
    }
  }

  companion object {
    const val ARG_SET = "set"
  }
}

/** Whether a profile other than [exceptId], or Default, is called [name]. */
private fun NotificationCodeFragment.nameTaken(
  profiles: List<BrailleCommands.Profile>,
  name: String,
  exceptId: String?,
): Boolean =
  name.equals(getString(R.string.gesture_profile_default), ignoreCase = true) ||
    profiles.any { it.id != exceptId && it.name.equals(name, ignoreCase = true) }
