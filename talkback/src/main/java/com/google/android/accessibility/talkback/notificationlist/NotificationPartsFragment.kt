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

package com.google.android.accessibility.talkback.notificationlist

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
 * Chooses which parts of a notification Backtalk's notification list speaks, and their order. Each
 * part has Move up and Move down accessibility actions, which screen reader users reach from the
 * actions menu.
 */
class NotificationPartsFragment : TalkbackBaseFragment() {
  private lateinit var prefs: SharedPreferences

  public override fun getTitle(): CharSequence = getText(R.string.title_pref_notification_parts)

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    preferenceManager.setStorageDeviceProtected()
    val context = requireContext()
    prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val screen = preferenceManager.createPreferenceScreen(context)
    NotificationParts.order(prefs).forEachIndexed { index, part ->
      screen.addPreference(PartPreference(context, part).apply { order = index })
    }
    preferenceScreen = screen
  }

  private fun move(part: NotificationPart, offset: Int): Boolean {
    val newIndex = NotificationParts.move(prefs, part, offset) ?: return false
    NotificationParts.order(prefs).forEachIndexed { index, it ->
      findPreference<PartPreference>(it.name)?.order = index
    }
    focusAfterMove(newIndex)
    return true
  }

  /**
   * Moves accessibility focus to the part at its new place. Changing the order rebuilds the list
   * after the adapter syncs and the list lays out again, and until then the focused row shows
   * another part.
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

  private inner class PartPreference(context: Context, private val part: NotificationPart) :
    CheckBoxPreference(context) {
    init {
      key = part.name
      isPersistent = false
      setTitle(part.title)
      isChecked = NotificationParts.isEnabled(prefs, part)
      setOnPreferenceChangeListener { _, newValue ->
        NotificationParts.setEnabled(prefs, part, newValue as Boolean)
        true
      }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
      super.onBindViewHolder(holder)
      // Rows are reused for other parts, so every bind replaces both actions.
      val index = NotificationParts.order(prefs).indexOf(part)
      setMoveActions(holder.itemView, index, NotificationPart.entries.size) { move(part, it) }
    }
  }
}
