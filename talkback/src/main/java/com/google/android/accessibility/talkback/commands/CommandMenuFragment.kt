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
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceViewHolder
import com.google.android.accessibility.material.preference.AccessibilitySuitePreferenceCategory
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.commands.CommandMenus.Place
import com.google.android.accessibility.talkback.notifications.NotificationCodeFragment
import com.google.android.accessibility.talkback.preference.PreferencesActivityUtils
import com.google.android.accessibility.talkback.preference.base.ItemListFragment
import com.google.android.accessibility.talkback.preference.base.announce
import com.google.android.accessibility.talkback.preference.base.focusAfterLayout
import com.google.android.accessibility.talkback.preference.base.positionText
import com.google.android.accessibility.talkback.preference.base.setMoveActions
import com.google.android.accessibility.talkback.preference.search.SettingsSearch

/**
 * The SandFly menu, a custom menu or one of their submenus: its commands and submenus in order,
 * each of which can be moved, moved to another menu, renamed or removed.
 */
class CommandMenuFragment :
  NotificationCodeFragment(R.string.title_pref_category_context_menu), SettingsSearch.FoundKey {

  private val place: Place by lazy {
    Place(
      arguments?.getString(ARG_MENU) ?: CommandMenus.SANDFLY_MENU,
      arguments?.getString(ARG_PARENT),
    )
  }

  private val isSandflyTop: Boolean
    get() = place.menuId == CommandMenus.SANDFLY_MENU && place.parent == null

  // The settings of the SandFly menu screen: those of one command, and the rest.
  private val commandOptions = HashMap<String, MutableList<Preference>>()
  private val generalOptions = ArrayList<Preference>()

  override fun onCreate(savedInstanceState: Bundle?) {
    PreferencesActivityUtils.removeEditingKey(requireContext())
    super.onCreate(savedInstanceState)
  }

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    preferenceManager.setStorageDeviceProtected()
    addPreferencesFromResource(R.xml.context_menu_preferences)
    readOptions(preferenceScreen)
    (commandOptions.values.flatten() + generalOptions).forEach { it.parent?.removePreference(it) }
    super.onCreatePreferences(savedInstanceState, rootKey)
  }

  private fun readOptions(group: PreferenceGroup) {
    val context = requireContext()
    for (i in 0 until group.preferenceCount) {
      val preference = group.getPreference(i)
      val items = ItemListFragment.optionItems(preference)
      when {
        preference is PreferenceGroup -> readOptions(preference)
        items != null ->
          items
            .firstNotNullOfOrNull { MenuCommands.valueOfShowKey(context, it) }
            ?.let { commandOptions.getOrPut(it) { ArrayList() } += preference }
        MenuCommands.valueOfShowKey(context, preference.key.orEmpty()) != null -> {}
        preference.key == getString(R.string.pref_radial_menu_key) -> generalOptions += preference
      }
    }
  }

  override fun getTitle(): CharSequence {
    val context = requireContext()
    val parentId = place.parent
    if (parentId != null) {
      return findSubmenu(CommandMenus.items(context, prefs, place.menuId), parentId)?.name.orEmpty()
    }
    return if (place.menuId == CommandMenus.SANDFLY_MENU) {
      getText(R.string.title_pref_category_context_menu)
    } else {
      customMenu()?.name.orEmpty()
    }
  }

  private fun customMenu(): CommandMenus.Menu? =
    CommandMenus.custom(prefs).firstOrNull { it.id == place.menuId }

  private fun findSubmenu(nodes: List<CommandMenus.Node>, id: String): CommandMenus.Submenu? {
    for (node in nodes) {
      if (node !is CommandMenus.Submenu) continue
      if (node.id == id) return node
      findSubmenu(node.children, id)?.let {
        return it
      }
    }
    return null
  }

  private fun items(): List<CommandMenus.Node> = CommandMenus.children(requireContext(), prefs, place)

  private fun save(children: List<CommandMenus.Node>) {
    CommandMenus.setChildren(requireContext(), prefs, place, children)
  }

  override fun build(screen: PreferenceGroup) {
    val context = requireContext()
    val nodes = items()
    screen.link(getString(R.string.edge_menu_add_command), null, MenuAddCommandFragment::class.java) {
      putString(ARG_MENU, place.menuId)
      putString(ARG_PARENT, place.parent)
    }
    screen.action(getString(R.string.menu_add_submenu)) { addSubmenu() }

    val current = screen.category(R.string.item_list_current)
    val labels =
      Commands.labels(context, nodes.map { (it as? CommandMenus.Command)?.value.orEmpty() })
    nodes.forEachIndexed { index, node ->
      val name =
        when (node) {
          is CommandMenus.Submenu -> node.name
          // A command this device or build doesn't have stays saved, and isn't listed.
          is CommandMenus.Command -> labels[index] ?: return@forEachIndexed
        }
      current.addPreference(ItemRow(context, nodes, index, node, name))
      if (node is CommandMenus.Command) {
        commandOptions[node.value]?.forEach { option ->
          option.parent?.removePreference(option)
          option.order = Preference.DEFAULT_ORDER
          current.addPreference(option)
        }
      }
    }

    if (isSandflyTop) {
      generalOptions.forEach { option ->
        option.parent?.removePreference(option)
        option.order = Preference.DEFAULT_ORDER
        screen.addPreference(option)
      }
      screen.action(getString(R.string.menu_reset)) { confirmReset() }
    } else if (place.parent == null) {
      screen.link(getString(R.string.menu_assign), null, CommandFragment::class.java) {
        putString(CommandFragment.ARG_COMMAND, CommandMenus.commandOf(place.menuId))
      }
      screen.action(getString(R.string.menu_rename)) { renameMenu() }
      screen.action(getString(R.string.menu_delete)) { confirmDeleteMenu() }
    }
  }

  private fun rowKey(node: CommandMenus.Node): String =
    when (node) {
      is CommandMenus.Command -> "menu_row_${node.value}"
      is CommandMenus.Submenu -> "menu_row_${node.id}"
    }

  override fun foundKey(key: String): String {
    val value = MenuCommands.valueOfShowKey(requireContext(), key) ?: return key
    val node = CommandMenus.Command(value)
    return if (node in items()) rowKey(node) else key
  }

  private fun askName(title: CharSequence, text: String = "", onName: (String) -> Unit) {
    askText(title, text, hint = getString(R.string.menu_name)) { name, _, _ ->
      val trimmed = name.trim()
      if (trimmed.isEmpty()) {
        ""
      } else {
        onName(trimmed)
        null
      }
    }
  }

  private fun addSubmenu() {
    askName(getString(R.string.menu_add_submenu)) { name ->
      save(items() + CommandMenus.Submenu(CommandMenus.newId(), name, emptyList()))
      rebuild()
    }
  }

  private fun open(submenu: CommandMenus.Submenu) {
    onPreferenceTreeClick(
      Preference(requireContext()).apply {
        fragment = CommandMenuFragment::class.java.name
        extras.putString(ARG_MENU, place.menuId)
        extras.putString(ARG_PARENT, submenu.id)
      }
    )
  }

  private fun showActions(nodes: List<CommandMenus.Node>, index: Int, node: CommandMenus.Node, name: CharSequence) {
    val actions = ArrayList<Pair<Int, () -> Unit>>()
    if (node is CommandMenus.Submenu) actions += R.string.menu_open to { open(node) }
    if (index > 0) actions += R.string.action_move_up to { move(nodes, index, -1) }
    if (index < nodes.size - 1) actions += R.string.action_move_down to { move(nodes, index, 1) }
    actions += R.string.menu_move_to to { moveTo(index, node) }
    if (node is CommandMenus.Submenu) {
      actions += R.string.menu_rename to { renameSubmenu(index, node) }
      actions += R.string.menu_delete to { confirmDeleteSubmenu(index, node) }
    } else {
      actions += R.string.quick_settings_remove to { remove(index, name) }
    }
    choose(name, actions.map { getString(it.first) }) { actions[it].second() }
  }

  private fun move(nodes: List<CommandMenus.Node>, index: Int, offset: Int): Boolean {
    val target = index + offset
    if (index !in nodes.indices || target !in nodes.indices) {
      return false
    }
    val moved = nodes.toMutableList()
    val node = moved.removeAt(index)
    moved.add(target, node)
    save(moved)
    rebuild()
    listView?.let {
      focusAfterLayout(it, findPreference(rowKey(node)), positionText(it, target, moved.size))
    }
    return true
  }

  private fun moveTo(index: Int, node: CommandMenus.Node) {
    val context = requireContext()
    val destinations = CommandMenus.destinations(context, prefs, place, node)
    choose(getString(R.string.menu_move_to), destinations.map { it.path }) { chosen ->
      val destination = destinations[chosen]
      if (CommandMenus.moveTo(context, prefs, place, index, destination.place)) {
        rebuild()
        listView?.let { announce(it, getString(R.string.menu_moved, destination.path)) }
      }
    }
  }

  private fun remove(index: Int, name: CharSequence): Boolean {
    val nodes = items()
    if (index !in nodes.indices) {
      return false
    }
    save(nodes.filterIndexed { i, _ -> i != index })
    rebuild()
    listView?.let { announce(it, getString(R.string.quick_settings_removed, name)) }
    return true
  }

  private fun renameSubmenu(index: Int, submenu: CommandMenus.Submenu) {
    askName(getString(R.string.menu_rename), submenu.name) { name ->
      save(items().mapIndexed { i, node -> if (i == index) submenu.copy(name = name) else node })
      rebuild()
    }
  }

  private fun confirmDeleteSubmenu(index: Int, submenu: CommandMenus.Submenu) {
    dialog()
      .setMessage(getString(R.string.menu_delete_confirm, submenu.name))
      .setPositiveButton(R.string.menu_delete) { _, _ -> remove(index, submenu.name) }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun confirmReset() {
    dialog()
      .setMessage(R.string.menu_reset_confirm)
      .setPositiveButton(R.string.menu_reset) { _, _ ->
        CommandMenus.resetSandfly(requireContext(), prefs)
        rebuild()
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun renameMenu() {
    val menu = customMenu() ?: return
    askName(getString(R.string.menu_rename), menu.name) { name ->
      CommandMenus.writeCustom(
        prefs,
        CommandMenus.custom(prefs).map { if (it.id == menu.id) it.copy(name = name) else it },
      )
      activity?.title = name
      rebuild()
    }
  }

  private fun confirmDeleteMenu() {
    val menu = customMenu() ?: return
    dialog()
      .setMessage(getString(R.string.menu_delete_confirm, menu.name))
      .setPositiveButton(R.string.menu_delete) { _, _ ->
        CommandMenus.writeCustom(prefs, CommandMenus.custom(prefs).filter { it.id != menu.id })
        requireActivity().onBackPressedDispatcher.onBackPressed()
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  /** A command or submenu, at [index] of [nodes]. */
  private inner class ItemRow(
    context: Context,
    private val nodes: List<CommandMenus.Node>,
    private val index: Int,
    private val node: CommandMenus.Node,
    private val name: CharSequence,
  ) : Preference(context) {
    init {
      key = rowKey(node)
      isPersistent = false
      isIconSpaceReserved = false
      title = name
      setOnPreferenceClickListener {
        showActions(nodes, index, node, name)
        true
      }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
      super.onBindViewHolder(holder)
      // Rows are reused for other preferences, so every bind replaces all the actions.
      val row = holder.itemView
      setMoveActions(row, index, nodes.size) { move(nodes, index, it) }
      val moveToText = getString(R.string.menu_move_to)
      ViewCompat.replaceAccessibilityAction(
        row,
        AccessibilityActionCompat(R.id.accessibility_custom_action_3, moveToText),
        moveToText,
      ) { _, _ ->
        moveTo(index, node)
        true
      }
      val removeText =
        getString(if (node is CommandMenus.Submenu) R.string.menu_delete else R.string.quick_settings_remove)
      ViewCompat.replaceAccessibilityAction(
        row,
        AccessibilityActionCompat(R.id.accessibility_custom_action_2, removeText),
        removeText,
      ) { _, _ ->
        if (node is CommandMenus.Submenu) {
          confirmDeleteSubmenu(index, node)
          true
        } else {
          remove(index, name)
        }
      }
    }
  }

  companion object {
    /** The argument with the ID of the menu. */
    const val ARG_MENU = "menu"

    /** The argument with the ID of the submenu, absent for the top of the menu. */
    const val ARG_PARENT = "parent"
  }
}

/** Custom menus: the menus the user made, and the button that makes another. */
class CustomMenusFragment : NotificationCodeFragment(R.string.title_pref_custom_menus) {

  override fun build(screen: PreferenceGroup) {
    screen.action(getString(R.string.menu_add_menu)) { addMenu() }
    for (menu in CommandMenus.custom(prefs)) {
      screen.link(menu.name, null, CommandMenuFragment::class.java) {
        putString(CommandMenuFragment.ARG_MENU, menu.id)
      }
    }
  }

  private fun addMenu() {
    askText(getString(R.string.menu_add_menu), hint = getString(R.string.menu_name)) { name, _, _ ->
      val trimmed = name.trim()
      if (trimmed.isEmpty()) {
        ""
      } else {
        CommandMenus.writeCustom(
          prefs,
          CommandMenus.custom(prefs) + CommandMenus.Menu(CommandMenus.newId(), trimmed, emptyList()),
        )
        rebuild()
        null
      }
    }
  }
}

/** The commands that can be added to a menu, by category. A double tap adds one. */
class MenuAddCommandFragment : NotificationCodeFragment(R.string.edge_menu_add_command) {

  private val place: Place by lazy {
    Place(
      arguments?.getString(CommandMenuFragment.ARG_MENU) ?: CommandMenus.SANDFLY_MENU,
      arguments?.getString(CommandMenuFragment.ARG_PARENT),
    )
  }

  override fun build(screen: PreferenceGroup) {
    val context = requireContext()
    val present = CommandMenus.children(context, prefs, place).toSet()
    val own = CommandMenus.commandOf(place.menuId)
    for (category in Commands.categories(context)) {
      val group =
        AccessibilitySuitePreferenceCategory(context).apply {
          key = "pref_menu_add_${category.titleRes}"
          setTitle(category.titleRes)
          isIconSpaceReserved = false
        }
      screen.addPreference(group)
      for (command in category.commands) {
        if (command.value == own || CommandMenus.Command(command.value) in present) continue
        group.addPreference(
          Preference(context).apply {
            key = "pref_menu_add_row_${command.value}"
            isPersistent = false
            isIconSpaceReserved = false
            title = command.label
            setOnPreferenceClickListener {
              add(group, this, command)
              true
            }
          }
        )
      }
      group.setCollapsible(getString(R.string.category_collapsed), getString(R.string.category_expanded))
    }
  }

  private fun add(group: PreferenceGroup, row: Preference, command: Commands.Command) {
    val context = requireContext()
    CommandMenus.setChildren(
      context,
      prefs,
      place,
      CommandMenus.children(context, prefs, place) + CommandMenus.Command(command.value),
    )
    val index = (0 until group.preferenceCount).indexOfFirst { group.getPreference(it) == row }
    group.removePreference(row)
    val next = group.takeIf { it.preferenceCount > 0 }?.getPreference(minOf(index, group.preferenceCount - 1))
    listView?.let {
      focusAfterLayout(it, next ?: group, getString(R.string.quick_settings_added, command.label))
    }
  }
}
