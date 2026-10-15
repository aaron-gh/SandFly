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

package com.google.android.accessibility.talkback.preference.base

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.view.accessibility.AccessibilityViewCommand
import androidx.preference.PreferenceViewHolder
import com.google.android.accessibility.material.preference.AccessibilitySuiteSwitchPreference
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.utils.ClipboardUtils

/**
 * A switch for a setting that needs a permission granted with adb. While [command] is set, the
 * switch has a "Copy command" action that copies it to the clipboard.
 */
class CommandSwitchPreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) :
  AccessibilitySuiteSwitchPreference(context, attrs) {

  /** The adb command that grants the permission, or null once it is granted. */
  var command: String? = null
    set(value) {
      if (field != value) {
        field = value
        notifyChanged()
      }
    }

  override fun onBindViewHolder(holder: PreferenceViewHolder) {
    super.onBindViewHolder(holder)
    // Rows are reused for other preferences, so every bind removes or replaces the action.
    ViewCompat.removeAccessibilityAction(holder.itemView, ACTION_COPY_COMMAND)
    val text = command ?: return
    val label = context.getString(R.string.action_copy_command)
    ViewCompat.replaceAccessibilityAction(
      holder.itemView,
      AccessibilityActionCompat(ACTION_COPY_COMMAND, label),
      label,
      AccessibilityViewCommand { _, _ ->
        copy(text)
        true
      },
    )
  }

  private fun copy(text: String) {
    if (!ClipboardUtils.copyToClipboard(context, text)) return
    // From Android 13 the system confirms copies itself.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      Toast.makeText(context, R.string.command_copied, Toast.LENGTH_SHORT).show()
    }
  }

  private companion object {
    val ACTION_COPY_COMMAND = R.id.accessibility_custom_action_0
  }
}
