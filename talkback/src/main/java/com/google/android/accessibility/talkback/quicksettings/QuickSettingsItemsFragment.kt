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

package com.google.android.accessibility.talkback.quicksettings

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.notificationlist.NotificationReplyActivity.Companion.keepButtonCase
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment
import com.google.android.accessibility.talkback.preference.base.announce
import com.google.android.accessibility.talkback.preference.base.positionText
import com.google.android.accessibility.talkback.preference.base.setMoveActions
import com.google.android.accessibility.talkback.shizuku.ShizukuCommand
import com.google.android.accessibility.talkback.shizuku.ShizukuShell
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * Chooses what SandFly's quick settings panel shows, and in what order. Each item has Move up, Move
 * down and Remove accessibility actions, which a double tap also offers. Add control, Add app tile
 * and Add system tile list what isn't in the panel yet.
 */
class QuickSettingsItemsFragment : TalkbackBaseFragment() {
  private lateinit var prefs: SharedPreferences
  private lateinit var controls: QuickControls
  private var appTiles: Map<ComponentName, AppTile> = emptyMap()

  /** The panel's items that this device has, in order. */
  private val items = ArrayList<QuickItem>()

  /** Saved items this device doesn't have, kept at the end so that they aren't lost. */
  private var unavailable: List<QuickItem> = emptyList()

  public override fun getTitle(): CharSequence = getText(R.string.title_pref_quick_settings_items)

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    val context = requireContext()
    // Built in code, so it must ask for the storage that SandFly's settings use itself.
    preferenceManager.setStorageDeviceProtected()
    prefs = SharedPreferencesUtils.getSharedPreferences(context)
    controls = QuickControls(context)
    appTiles = SystemTiles.appTiles(context).associateBy { it.component }
    val saved = QuickSettingsItems.saved(prefs)
    items.clear()
    items.addAll(saved.filter { isAvailable(it) })
    unavailable = saved.filterNot { isAvailable(it) }
    val screen = preferenceManager.createPreferenceScreen(context)
    items.forEachIndexed { index, item ->
      screen.addPreference(ItemPreference(context, item).apply { order = index })
    }
    screen.addPreference(
      AddPreference(context, KEY_ADD_CONTROL, R.string.quick_settings_add_control, ORDER_ADD) {
        addControl()
      }
    )
    screen.addPreference(
      AddPreference(context, KEY_ADD_TILE, R.string.quick_settings_add_tile, ORDER_ADD + 1) {
        addTile()
      }
    )
    screen.addPreference(
      AddPreference(context, KEY_ADD_SYSTEM_TILE, R.string.quick_settings_add_system_tile, ORDER_ADD + 2) {
        addSystemTile()
      }
    )
    preferenceScreen = screen
  }

  private fun isAvailable(item: QuickItem): Boolean =
    when (item) {
      is QuickItem.Control -> controls.isAvailable(item.control)
      is QuickItem.Tile -> item.component in appTiles
      is QuickItem.SystemTile -> true
    }

  private fun title(item: QuickItem): CharSequence =
    when (item) {
      is QuickItem.Control -> getString(item.control.title)
      is QuickItem.Tile -> appTiles[item.component]?.label ?: item.component.packageName
      is QuickItem.SystemTile -> item.label
    }

  private fun save() {
    QuickSettingsItems.save(prefs, items + unavailable)
  }

  private fun addControl() {
    val available = QuickControl.entries.filter { control ->
      controls.isAvailable(control) && items.none { it is QuickItem.Control && it.control == control }
    }
    if (available.isEmpty()) {
      return
    }
    AlertDialog.Builder(requireContext())
      .setTitle(R.string.quick_settings_add_control)
      .setItems(available.map { getString(it.title) }.toTypedArray()) { _, which ->
        add(QuickItem.Control(available[which]))
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
      .keepButtonCase()
  }

  private fun addTile() {
    val available = appTiles.values.filter { tile ->
      items.none { it is QuickItem.Tile && it.component == tile.component }
    }
    if (available.isEmpty()) {
      return
    }
    val names =
      available.map { tile ->
        if (tile.label.toString() == tile.appName.toString()) tile.label
        else getString(R.string.quick_settings_tile_name, tile.label, tile.appName)
      }
    AlertDialog.Builder(requireContext())
      .setTitle(R.string.quick_settings_add_tile)
      .setItems(names.toTypedArray()) { _, which ->
        val component = available[which].component
        add(QuickItem.Tile(component))
        // SystemUI runs only the tiles in the system's quick settings, so the tile goes there too.
        if (ShizukuShell.isAllowed()) {
          ShizukuShell.run(requireContext(), ShizukuCommand.ADD_TILE, component.flattenToString()) {}
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
      .keepButtonCase()
  }

  /**
   * Lists the system's own tiles, such as Hotspot, from SystemUI's dump when SandFly can read it,
   * and otherwise by looking through the system's quick settings, which takes a few seconds.
   */
  private fun addSystemTile() {
    val context = requireContext().applicationContext
    if (SystemTiles.canRead(context)) {
      val handler = Handler(Looper.getMainLooper())
      Thread({
          val tiles =
            try {
              SystemTiles.read()
            } catch (e: Throwable) {
              null
            }
          val found =
            tiles?.values?.filter { !it.spec.startsWith("custom(") && it.label != null }?.map {
              QuickItem.SystemTile(it.spec, it.label!!)
            }
          handler.post { if (isAdded) chooseSystemTile(found) }
        }, "QuickSettingsTiles")
        .start()
    } else {
      SystemPanelClicker.scan { labels ->
        if (isAdded) chooseSystemTile(labels?.map { QuickItem.SystemTile("", it) })
      }
    }
  }

  private fun chooseSystemTile(found: List<QuickItem.SystemTile>?) {
    val added = items.filterIsInstance<QuickItem.SystemTile>()
    val available =
      found.orEmpty().filter { tile ->
        added.none {
          (it.spec.isNotEmpty() && it.spec == tile.spec) ||
            SystemPanelClicker.normalize(it.label) == SystemPanelClicker.normalize(tile.label)
        }
      }
    val list = listView ?: return
    if (available.isEmpty()) {
      announce(list, getString(R.string.quick_settings_no_system_tiles))
      return
    }
    AlertDialog.Builder(requireContext())
      .setTitle(R.string.quick_settings_add_system_tile)
      .setItems(available.map { it.label }.toTypedArray()) { _, which -> add(available[which]) }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
      .keepButtonCase()
  }

  private fun add(item: QuickItem) {
    items += item
    save()
    preferenceScreen.addPreference(ItemPreference(requireContext(), item).apply { order = items.size - 1 })
    listView?.let { announce(it, getString(R.string.quick_settings_added, title(item))) }
    // Focus goes to the new item, at the end of the panel, from where the user can move it.
    focusAfterChange(items.size - 1, announcePosition = false)
  }

  private fun remove(item: QuickItem) {
    val index = items.indexOf(item)
    if (index < 0) {
      return
    }
    items.removeAt(index)
    save()
    preferenceScreen.findPreference<Preference>(item.id)?.let { preferenceScreen.removePreference(it) }
    reorder()
    listView?.let { announce(it, getString(R.string.quick_settings_removed, title(item))) }
    focusAfterChange(minOf(index, preferenceScreen.preferenceCount - 1), announcePosition = false)
  }

  private fun move(item: QuickItem, offset: Int): Boolean {
    val from = items.indexOf(item)
    val to = from + offset
    if (from < 0 || to !in items.indices) {
      return false
    }
    items.add(to, items.removeAt(from))
    save()
    reorder()
    focusAfterChange(to, announcePosition = true)
    return true
  }

  private fun reorder() {
    items.forEachIndexed { index, item -> preferenceScreen.findPreference<Preference>(item.id)?.order = index }
  }

  private fun showActions(item: QuickItem) {
    val index = items.indexOf(item)
    val actions = ArrayList<Pair<Int, () -> Unit>>()
    if (index > 0) actions += R.string.action_move_up to { move(item, -1) }
    if (index in 0 until items.size - 1) actions += R.string.action_move_down to { move(item, 1) }
    actions += R.string.quick_settings_remove to { remove(item) }
    AlertDialog.Builder(requireContext())
      .setTitle(title(item))
      .setItems(actions.map { getString(it.first) }.toTypedArray()) { _, which -> actions[which].second() }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
      .keepButtonCase()
  }

  /**
   * Moves accessibility focus to the row at [index] after the list has changed. Changing the list
   * rebuilds it after the adapter syncs and the list lays out again, and until then the focused
   * row shows another item.
   */
  private fun focusAfterChange(index: Int, announcePosition: Boolean) {
    val list = listView ?: return
    if (index < 0) return
    val count = items.size
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
            list.scrollToPosition(index)
            val row = list.findViewHolderForAdapterPosition(index)?.itemView ?: return
            row.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
            if (announcePosition) {
              announce(row, positionText(row, index, count))
            }
          }
        }
      )
    }
  }

  private inner class ItemPreference(context: Context, private val item: QuickItem) : Preference(context) {
    init {
      key = item.id
      isPersistent = false
      title = title(item)
      if (item is QuickItem.Tile) {
        appTiles[item.component]?.let { tile ->
          if (tile.appName.toString() != tile.label.toString()) summary = tile.appName
        }
      }
      setOnPreferenceClickListener {
        showActions(item)
        true
      }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
      super.onBindViewHolder(holder)
      // Rows are reused for other items, so every bind replaces all the actions.
      val row = holder.itemView
      setMoveActions(row, items.indexOf(item), items.size) { move(item, it) }
      val removeText = getString(R.string.quick_settings_remove)
      ViewCompat.replaceAccessibilityAction(
        row,
        AccessibilityActionCompat(R.id.accessibility_custom_action_2, removeText),
        removeText,
      ) { _, _ ->
        remove(item)
        true
      }
    }
  }

  /** Add control or Add app tile. */
  private class AddPreference(context: Context, key: String, title: Int, order: Int, onClick: () -> Unit) :
    Preference(context) {
    init {
      this.key = key
      isPersistent = false
      this.order = order
      setTitle(title)
      setOnPreferenceClickListener {
        onClick()
        true
      }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
      super.onBindViewHolder(holder)
      // The row may have shown an item before, with that item's actions.
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

    const val KEY_ADD_CONTROL = "quick_settings_add_control"
    const val KEY_ADD_TILE = "quick_settings_add_tile"
    const val KEY_ADD_SYSTEM_TILE = "quick_settings_add_system_tile"
    const val ORDER_ADD = 100_000
  }
}
