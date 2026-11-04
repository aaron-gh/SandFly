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
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceViewHolder
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.gesture.EdgeMenus
import com.google.android.accessibility.talkback.gesture.EdgeSwipeTracker.MenuEdge
import com.google.android.accessibility.talkback.notifications.NotificationCodeFragment
import com.google.android.accessibility.talkback.preference.base.GestureListPreference
import com.google.android.accessibility.talkback.preference.base.announce
import com.google.android.accessibility.talkback.preference.base.positionText
import com.google.android.accessibility.talkback.preference.base.setMoveActions

/**
 * Gestures > Edges > Left edge menu or Right edge menu: the switch that makes the edge a menu, and
 * the menu's commands in order, each with Change command, Move up, Move down and Remove.
 */
class EdgeMenuFragment : NotificationCodeFragment(R.string.title_pref_edge_menu_left) {

  private val edge: MenuEdge by lazy {
    if (arguments?.getString(ARG_EDGE) == MenuEdge.RIGHT.name) MenuEdge.RIGHT else MenuEdge.LEFT
  }

  private val unassigned: String
    get() = getString(R.string.shortcut_value_unassigned)

  override fun getTitle(): CharSequence =
    getText(
      if (edge == MenuEdge.LEFT) R.string.title_pref_edge_menu_left
      else R.string.title_pref_edge_menu_right
    )

  override fun build(screen: PreferenceGroup) {
    val context = requireContext()
    val items = EdgeMenus.items(prefs, edge, unassigned)
    // Closes the gap left by a command changed to none.
    EdgeMenus.setItems(prefs, edge, items)
    val switchKey = EdgeMenus.switchKey(edge)
    screen.switch(
      getString(
        if (edge == MenuEdge.LEFT) R.string.title_pref_edge_menu_left_switch
        else R.string.title_pref_edge_menu_right_switch
      ),
      prefs.getBoolean(switchKey, false),
    ) {
      prefs.edit { putBoolean(switchKey, it) }
    }
    val category = screen.category(R.string.item_list_current)
    items.forEachIndexed { index, command ->
      category.addPreference(ItemRow(context, items, index, Commands.label(context, command) ?: command))
    }
    if (items.size < EdgeMenus.MAX_ITEMS) {
      screen.action(getString(R.string.edge_menu_add_command)) {
        pickCommand(items.size + 1, getString(R.string.edge_menu_add_command))
      }
    }
  }

  /** Opens the list of commands for [position], from 1, which saves the one chosen there. */
  private fun pickCommand(position: Int, title: CharSequence) {
    val preference =
      GestureListPreference(requireContext(), EdgeMenus.itemKey(edge, position), title, unassigned)
    GestureListPreference.showCommandList(this, preference)
  }

  private fun showActions(items: List<String>, index: Int, name: CharSequence) {
    val actions = ArrayList<Pair<Int, () -> Unit>>()
    actions += R.string.edge_menu_change_command to { pickCommand(index + 1, name) }
    if (index > 0) actions += R.string.action_move_up to { move(items, index, -1) }
    if (index < items.size - 1) actions += R.string.action_move_down to { move(items, index, 1) }
    actions += R.string.quick_settings_remove to { remove(items, index, name) }
    choose(name, actions.map { getString(it.first) }) { actions[it].second() }
  }

  private fun move(items: List<String>, index: Int, offset: Int): Boolean {
    val target = index + offset
    if (index !in items.indices || target !in items.indices) {
      return false
    }
    val moved = items.toMutableList()
    moved.add(target, moved.removeAt(index))
    EdgeMenus.setItems(prefs, edge, moved)
    rebuild()
    listView?.let { announce(it, positionText(it, target, moved.size)) }
    return true
  }

  private fun remove(items: List<String>, index: Int, name: CharSequence): Boolean {
    if (index !in items.indices) {
      return false
    }
    EdgeMenus.setItems(prefs, edge, items.filterIndexed { i, _ -> i != index })
    rebuild()
    listView?.let { announce(it, getString(R.string.quick_settings_removed, name)) }
    return true
  }

  /** A command of the menu, at [index] of [items]. */
  private inner class ItemRow(
    context: Context,
    private val items: List<String>,
    private val index: Int,
    private val name: CharSequence,
  ) : Preference(context) {
    init {
      isPersistent = false
      isIconSpaceReserved = false
      title = name
      setOnPreferenceClickListener {
        showActions(items, index, name)
        true
      }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
      super.onBindViewHolder(holder)
      // Rows are reused for other preferences, so every bind replaces all the actions.
      val row = holder.itemView
      setMoveActions(row, index, items.size) { move(items, index, it) }
      val removeText = getString(R.string.quick_settings_remove)
      ViewCompat.replaceAccessibilityAction(
        row,
        AccessibilityActionCompat(R.id.accessibility_custom_action_2, removeText),
        removeText,
      ) { _, _ ->
        remove(items, index, name)
      }
    }
  }

  companion object {
    /** The argument naming the edge, LEFT or RIGHT. */
    const val ARG_EDGE = "edge"
  }
}
