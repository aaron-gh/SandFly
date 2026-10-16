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

package com.google.android.accessibility.talkback.status

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceViewHolder
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment
import com.google.android.accessibility.talkback.preference.base.announce
import com.google.android.accessibility.talkback.preference.base.positionText
import com.google.android.accessibility.talkback.preference.base.setMoveActions
import com.google.android.accessibility.utils.SharedPreferencesUtils

/**
 * Chooses which items the status gesture speaks, and their order. Each item has Move up and Move
 * down accessibility actions, which screen reader users reach from the actions menu.
 */
class StatusReadoutFragment : TalkbackBaseFragment() {
  private lateinit var prefs: SharedPreferences

  public override fun getTitle(): CharSequence = getText(R.string.title_pref_status_readout)

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    val context = requireContext()
    prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val screen = preferenceManager.createPreferenceScreen(context)
    StatusSettings.order(prefs).forEachIndexed { index, item ->
      screen.addPreference(StatusItemPreference(context, item).apply { order = index })
    }
    preferenceScreen = screen
  }

  private fun move(item: StatusItem, offset: Int): Boolean {
    val newIndex = StatusSettings.move(prefs, item, offset) ?: return false
    StatusSettings.order(prefs).forEachIndexed { index, it ->
      findPreference<StatusItemPreference>(it.name)?.order = index
    }
    focusAfterMove(newIndex)
    return true
  }

  /**
   * Moves accessibility focus to the item at its new place. Changing the order rebuilds the list
   * after the adapter syncs and the list lays out again, and until then the focused row shows
   * another item.
   */
  private fun focusAfterMove(index: Int) {
    val list = listView ?: return
    val count = preferenceScreen.preferenceCount
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
            announce(row, positionText(row, index, count))
          }
        }
      )
    }
  }

  private inner class StatusItemPreference(context: Context, private val item: StatusItem) :
    CheckBoxPreference(context) {
    init {
      key = item.name
      isPersistent = false
      layoutResource = R.layout.listitem_2texts
      setTitle(item.title)
      setSummary(item.summary)
      isChecked = StatusSettings.isEnabled(prefs, item)
      setOnPreferenceChangeListener { _, newValue ->
        StatusSettings.setEnabled(prefs, item, newValue as Boolean)
        true
      }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
      super.onBindViewHolder(holder)
      // Rows are reused for other items, so every bind replaces both actions.
      val index = StatusSettings.order(prefs).indexOf(item)
      setMoveActions(holder.itemView, index, StatusItem.entries.size) { move(item, it) }
    }
  }
}
