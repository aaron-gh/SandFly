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

package com.google.android.accessibility.talkback.preference.base

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import androidx.preference.PreferenceViewHolder
import androidx.preference.TwoStatePreference
import com.google.android.accessibility.material.preference.AccessibilitySuitePreferenceCategory
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.ItemOrder
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * A list the user puts together, such as the reading controls or the SandFly menu. Current lists
 * what is in it, in its order, each with Move up, Move down and Remove, as accessibility actions
 * and on a double tap. Below a "Not in" heading, the screen's groups list what isn't in it; a
 * double tap adds one at the end of Current.
 *
 * Each item is a switch setting in [xmlResId]: the switch says whether the item is in the list, and
 * [orderKey] keeps the order. Other settings of the screen are listed under Options.
 */
abstract class ItemListFragment(xmlResId: Int) : TalkbackBaseFragment(xmlResId) {

  /** The setting that keeps the order of the items. */
  protected abstract val orderKey: String

  /** The heading above the groups of items not in the list. */
  @get:StringRes protected abstract val notInListTitle: Int

  /** Returns the key of every item, in the list's default order. */
  protected abstract fun defaultOrder(context: Context): List<String>

  /** Hides the items and options of [screen], the XML screen, that this device can't use. */
  protected open fun hideUnavailable(screen: PreferenceScreen) {}

  /** Runs after an item is added or removed, with whether [key] is now in the list. */
  protected open fun onMembershipChanged(key: String, inList: Boolean) {}

  private class Item(
    val key: String,
    val title: CharSequence,
    val summary: CharSequence?,
    val group: Int,
    val groupOrder: Int,
  )

  private lateinit var prefs: SharedPreferences
  private val items = LinkedHashMap<String, Item>()
  private val current = ArrayList<String>()
  private var fullOrder: List<String> = emptyList()
  private lateinit var currentCategory: AccessibilitySuitePreferenceCategory
  private val groups = ArrayList<AccessibilitySuitePreferenceCategory>()

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    super.onCreatePreferences(savedInstanceState, rootKey)
    val context = requireContext()
    val xmlScreen = preferenceScreen
    hideUnavailable(xmlScreen)
    if (FormFactorUtils.isAndroidWear()) {
      // A watch keeps the switches, in the default order.
      collapseCategories()
      return
    }
    prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val defaults = defaultOrder(context)
    val known = defaults.toSet()

    // Reads the items and options from the XML screen, then moves the options to the new one.
    val options = ArrayList<Preference>()
    val enabled = HashSet<String>()
    val groupTitles = ArrayList<CharSequence?>()
    fun read(group: PreferenceGroup, groupIndex: Int) {
      for (i in 0 until group.preferenceCount) {
        val preference = group.getPreference(i)
        when {
          preference is PreferenceGroup -> {
            groupTitles += preference.title
            read(preference, groupTitles.size - 1)
          }
          !preference.isVisible -> {}
          preference is TwoStatePreference && preference.key in known -> {
            items[preference.key] =
              Item(preference.key, preference.title ?: preference.key, preference.summary, groupIndex, i)
            if (preference.isChecked) {
              enabled += preference.key
            }
          }
          else -> options += preference
        }
      }
    }
    read(xmlScreen, -1)
    options.forEach { it.parent?.removePreference(it) }

    fullOrder = ItemOrder.ordered(prefs, orderKey, defaults)
    current.clear()
    current.addAll(fullOrder.filter { it in items && it in enabled })

    val screen = preferenceManager.createPreferenceScreen(context)
    preferenceScreen = screen
    currentCategory =
      AccessibilitySuitePreferenceCategory(context).apply {
        key = "${orderKey}_current"
        setTitle(R.string.item_list_current)
        isIconSpaceReserved = false
      }
    screen.addPreference(currentCategory)
    current.forEachIndexed { index, key -> currentCategory.addPreference(CurrentRow(context, key, index)) }

    if (options.isNotEmpty()) {
      val optionsCategory =
        PreferenceCategory(context).apply {
          key = "${orderKey}_options"
          setTitle(R.string.item_list_options)
          isIconSpaceReserved = false
        }
      screen.addPreference(optionsCategory)
      options.forEach { optionsCategory.addPreference(it) }
    }

    screen.addPreference(
      PreferenceCategory(context).apply {
        key = "${orderKey}_not_in_list"
        setTitle(notInListTitle)
        isIconSpaceReserved = false
      }
    )
    groupTitles.forEachIndexed { index, title ->
      val group =
        AccessibilitySuitePreferenceCategory(context).apply {
          key = "${orderKey}_group_$index"
          this.title = title
          isIconSpaceReserved = false
        }
      groups += group
      screen.addPreference(group)
    }
    for (item in items.values) {
      if (item.key !in current && item.group >= 0) {
        groups[item.group].addPreference(AvailableRow(context, item))
      }
    }
    for (group in groups) {
      // Groups with nothing left still head the items removed from the list later.
      group.setCollapsible(getString(R.string.category_collapsed), getString(R.string.category_expanded))
    }
    currentCategory.setCollapsible(
      getString(R.string.category_collapsed),
      getString(R.string.category_expanded),
    )
  }

  private fun title(key: String): CharSequence = items[key]?.title ?: key

  /** Saves the order: the list as it is, then everything else in its old order. */
  private fun save() {
    fullOrder = current + fullOrder.filter { it !in current }
    ItemOrder.save(prefs, orderKey, fullOrder)
  }

  private fun add(item: Item, row: Preference) {
    val group = row.parent
    val index = group?.let { (0 until it.preferenceCount).indexOfFirst { i -> it.getPreference(i) == row } } ?: -1
    group?.removePreference(row)
    current += item.key
    save()
    prefs.edit { putBoolean(item.key, true) }
    onMembershipChanged(item.key, true)
    addRow(currentCategory, CurrentRow(requireContext(), item.key, current.size - 1))
    val announcement = getString(R.string.quick_settings_added, item.title)
    // Focus stays in the group, on the item that took the added one's place.
    val next = group?.takeIf { it.preferenceCount > 0 }?.getPreference(minOf(index, group.preferenceCount - 1))
    focusAfterChange(next ?: group, announcement)
  }

  private fun remove(key: String) {
    val index = current.indexOf(key)
    if (index < 0) {
      return
    }
    current.removeAt(index)
    save()
    prefs.edit { putBoolean(key, false) }
    onMembershipChanged(key, false)
    currentCategory.findPreference<Preference>(rowKey(key))?.let { currentCategory.removePreference(it) }
    renumber()
    items[key]?.let { item ->
      if (item.group >= 0) {
        addRow(groups[item.group], AvailableRow(requireContext(), item))
      }
    }
    val announcement = getString(R.string.quick_settings_removed, title(key))
    val next =
      current.getOrNull(minOf(index, current.size - 1))?.let {
        currentCategory.findPreference<Preference>(rowKey(it))
      }
    focusAfterChange(next ?: currentCategory, announcement)
  }

  private fun move(key: String, offset: Int): Boolean {
    val from = current.indexOf(key)
    val to = from + offset
    if (from < 0 || to !in current.indices) {
      return false
    }
    current.add(to, current.removeAt(from))
    save()
    renumber()
    focusAfterChange(
      currentCategory.findPreference(rowKey(key)),
      positionText(listView ?: return true, to, current.size),
    )
    return true
  }

  /** Adds [row] to [category], hidden if the category is collapsed. */
  private fun addRow(category: AccessibilitySuitePreferenceCategory, row: Preference) {
    val collapsed = category.isCollapsed
    if (collapsed) {
      category.setCollapsed(false)
    }
    category.addPreference(row)
    if (collapsed) {
      category.setCollapsed(true)
    }
  }

  private fun renumber() {
    current.forEachIndexed { index, key ->
      currentCategory.findPreference<Preference>(rowKey(key))?.order = index
    }
  }

  private fun showActions(key: String) {
    val index = current.indexOf(key)
    val actions = ArrayList<Pair<Int, () -> Unit>>()
    if (index > 0) actions += R.string.action_move_up to { move(key, -1) }
    if (index in 0 until current.size - 1) actions += R.string.action_move_down to { move(key, 1) }
    actions += R.string.quick_settings_remove to { remove(key) }
    AlertDialog.Builder(requireContext())
      .setTitle(title(key))
      .setItems(actions.map { getString(it.first) }.toTypedArray()) { _, which ->
        actions[which].second()
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  /**
   * Moves accessibility focus to [preference] once the list has laid out again, and says
   * [announcement].
   */
  private fun focusAfterChange(preference: Preference?, announcement: String?) {
    val list = listView ?: return
    list.post {
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
            val adapter = list.adapter as? PreferenceGroup.PreferencePositionCallback
            val position = preference?.let { adapter?.getPreferenceAdapterPosition(it) } ?: -1
            if (position >= 0) {
              list.scrollToPosition(position)
              list
                .findViewHolderForAdapterPosition(position)
                ?.itemView
                ?.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
            }
            announcement?.let { announce(list, it) }
          }
        }
      )
      list.requestLayout()
    }
  }

  private fun rowKey(key: String) = "${orderKey}_row_$key"

  /** An item in the list. */
  private inner class CurrentRow(context: Context, private val itemKey: String, index: Int) :
    Preference(context) {
    init {
      key = rowKey(itemKey)
      isPersistent = false
      isIconSpaceReserved = false
      order = index
      title = title(itemKey)
      summary = items[itemKey]?.summary
      setOnPreferenceClickListener {
        showActions(itemKey)
        true
      }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
      super.onBindViewHolder(holder)
      // Rows are reused for other items, so every bind replaces all the actions.
      val row = holder.itemView
      setMoveActions(row, current.indexOf(itemKey), current.size) { move(itemKey, it) }
      val removeText = getString(R.string.quick_settings_remove)
      ViewCompat.replaceAccessibilityAction(
        row,
        AccessibilityActionCompat(R.id.accessibility_custom_action_2, removeText),
        removeText,
      ) { _, _ ->
        remove(itemKey)
        true
      }
    }
  }

  /** An item not in the list. */
  private inner class AvailableRow(context: Context, private val item: Item) : Preference(context) {
    init {
      key = "${orderKey}_available_${item.key}"
      isPersistent = false
      isIconSpaceReserved = false
      order = item.groupOrder
      title = item.title
      summary = item.summary
      setOnPreferenceClickListener {
        add(item, this)
        true
      }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
      super.onBindViewHolder(holder)
      // The row may have shown an item of the list before, with that item's actions.
      ITEM_ACTIONS.forEach { ViewCompat.removeAccessibilityAction(holder.itemView, it) }
    }
  }

  private companion object {
    val ITEM_ACTIONS =
      listOf(
        R.id.accessibility_custom_action_0,
        R.id.accessibility_custom_action_1,
        R.id.accessibility_custom_action_2,
      )
  }
}
