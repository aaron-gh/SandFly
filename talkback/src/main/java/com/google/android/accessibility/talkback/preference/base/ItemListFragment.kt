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
import com.google.android.accessibility.talkback.preference.search.SettingsSearch
import com.google.android.accessibility.utils.FormFactorUtils
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * A list the user puts together, such as the reading controls or the SandFly menu. Current lists
 * what is in it, in its order, each with Move up, Move down and Remove, as accessibility actions
 * and on a double tap. Below a "Not in" heading, the screen's groups list what isn't in it; a
 * double tap adds one at the end of Current.
 *
 * Each item is a switch setting in [xmlResId]: the switch says whether the item is in the list, and
 * [orderKey] keeps the order. A setting of the screen with an [OPTION_ITEMS] extra, the keys of
 * items separated by commas, is an option of those items: it follows the first of them in Current,
 * and is hidden while none of them is in the list. The other settings are listed under Options.
 */
abstract class ItemListFragment(xmlResId: Int) :
  TalkbackBaseFragment(xmlResId), SettingsSearch.FoundKey {

  /** The setting that keeps the order of the items. */
  protected abstract val orderKey: String

  /** The heading above the groups of items not in the list. */
  @get:StringRes protected abstract val notInListTitle: Int

  /** Returns the key of every item, in the list's default order. */
  protected abstract fun defaultOrder(context: Context): List<String>

  /** The title of the group of items that aren't in the XML screen, 0 without such items. */
  @StringRes protected open fun extraGroupTitle(): Int = 0

  /** The key and title of each item that isn't in the XML screen, in the list by default. */
  protected open fun extraItems(context: Context): List<Pair<String, CharSequence>> = emptyList()

  /** Hides the items and options of [screen], the XML screen, that this device can't use. */
  protected open fun hideUnavailable(screen: PreferenceScreen) {}

  /**
   * Runs after an item is added or removed and the screen shows the change, with whether [key] is
   * now in the list.
   */
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
  // The options of items, each with the keys of its items.
  private val itemOptions = LinkedHashMap<Preference, List<String>>()
  // The items that aren't in the XML screen, and the index of their group.
  private val extraKeys = LinkedHashSet<String>()
  private var extraGroup = -1

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
          optionItems(preference) != null -> itemOptions[preference] = optionItems(preference)!!
          else -> options += preference
        }
      }
    }
    read(xmlScreen, -1)
    if (extraGroupTitle() != 0) {
      extraGroup = groupTitles.size
      groupTitles += getString(extraGroupTitle())
      for ((index, extra) in extraItems(context).withIndex()) {
        items[extra.first] = Item(extra.first, extra.second, null, extraGroup, index)
        extraKeys += extra.first
        if (prefs.getBoolean(extra.first, true)) {
          enabled += extra.first
        }
      }
    }
    options.forEach { it.parent?.removePreference(it) }
    itemOptions.keys.forEach { it.parent?.removePreference(it) }
    // Options of items that this device hides go with them.
    itemOptions.entries.removeAll { (_, keys) -> keys.none { it in items } }

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
    placeOptions()

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

  override fun onResume() {
    super.onResume()
    if (!FormFactorUtils.isAndroidWear() && extraGroupTitle() != 0) {
      syncExtraItems()
    }
  }

  /** Adds, renames and removes the items that aren't in the XML screen, which change elsewhere. */
  private fun syncExtraItems() {
    val context = requireContext()
    val extras = extraItems(context).toMap()
    for (key in extraKeys.filter { it !in extras }) {
      extraKeys -= key
      items.remove(key)
      current.remove(key)
      for (row in listOf(rowKey(key), availableKey(key))) {
        findPreference<Preference>(row)?.let { it.parent?.removePreference(it) }
      }
    }
    var added = false
    extras.entries.forEachIndexed { index, (key, title) ->
      if (key in extraKeys) {
        items[key] = Item(key, title, null, extraGroup, index)
        findPreference<Preference>(rowKey(key))?.title = title
        findPreference<Preference>(availableKey(key))?.title = title
      } else {
        extraKeys += key
        val item = Item(key, title, null, extraGroup, index)
        items[key] = item
        if (prefs.getBoolean(key, true)) {
          current += key
          addRow(currentCategory, CurrentRow(context, key, current.size - 1))
          added = true
        } else {
          addRow(groups[extraGroup], AvailableRow(context, item))
        }
      }
    }
    if (added) {
      fullOrder = ItemOrder.ordered(prefs, orderKey, defaultOrder(context))
      save()
    }
    renumber()
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
    addRow(currentCategory, CurrentRow(requireContext(), item.key, current.size - 1))
    placeOptions()
    onMembershipChanged(item.key, true)
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
    currentCategory.findPreference<Preference>(rowKey(key))?.let { currentCategory.removePreference(it) }
    renumber()
    onMembershipChanged(key, false)
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
      currentCategory.findPreference<Preference>(rowKey(key))?.order = index * ORDER_STEP
    }
    placeOptions()
  }

  /** Puts each option after the first of its items in Current, or hides it when none is there. */
  private fun placeOptions() {
    itemOptions.entries.forEachIndexed { optionIndex, (option, keys) ->
      val index = current.indexOfFirst { it in keys }
      val shown = option.parent == currentCategory
      if (index < 0) {
        if (shown) {
          currentCategory.removePreference(option)
        }
        return@forEachIndexed
      }
      option.order = index * ORDER_STEP + 1 + optionIndex
      if (!shown) {
        addRow(currentCategory, option)
      }
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
    listView?.let { focusAfterLayout(it, preference, announcement) }
  }

  private fun rowKey(key: String) = "${orderKey}_row_$key"

  private fun availableKey(key: String) = "${orderKey}_available_$key"

  override fun foundKey(key: String): String =
    when (key) {
      in current -> rowKey(key)
      in items -> availableKey(key)
      else -> key
    }

  /** An item in the list. */
  private inner class CurrentRow(context: Context, private val itemKey: String, index: Int) :
    Preference(context) {
    init {
      key = rowKey(itemKey)
      isPersistent = false
      isIconSpaceReserved = false
      order = index * ORDER_STEP
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
      key = availableKey(item.key)
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

  companion object {
    /** The extra of an option of items: the keys of its items, separated by commas. */
    const val OPTION_ITEMS = "items"

    // Leaves room after each row for its options.
    private const val ORDER_STEP = 100

    /** The keys of the items that [preference] is an option of, or null for other settings. */
    @JvmStatic
    fun optionItems(preference: Preference): List<String>? =
      preference
        .peekExtras()
        ?.getCharSequence(OPTION_ITEMS)
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.takeIf { it.isNotEmpty() }

    private val ITEM_ACTIONS =
      listOf(
        R.id.accessibility_custom_action_0,
        R.id.accessibility_custom_action_1,
        R.id.accessibility_custom_action_2,
      )
  }
}
