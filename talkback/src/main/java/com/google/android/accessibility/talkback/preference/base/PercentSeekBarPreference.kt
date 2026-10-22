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
import android.os.Bundle
import android.util.AttributeSet
import android.widget.SeekBar
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.preference.PreferenceViewHolder
import com.google.android.accessibility.material.preference.AccessibilitySeekBarPreference
import com.google.android.accessibility.talkback.R

/**
 * A slider of a percentage. It speaks its value as a percentage, rather than its position between
 * its minimum and maximum, and swipes move it by its increment.
 */
class PercentSeekBarPreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) :
  AccessibilitySeekBarPreference(context, attrs) {

  override fun onBindViewHolder(holder: PreferenceViewHolder) {
    super.onBindViewHolder(holder)
    val seekBar = holder.findViewById(androidx.preference.R.id.seekbar) as? SeekBar ?: return
    describe(seekBar)
    ViewCompat.replaceAccessibilityAction(
      seekBar,
      AccessibilityActionCompat.ACTION_SCROLL_BACKWARD,
      null,
    ) { _, _ ->
      moveTo(seekBar, value - seekBarIncrement)
    }
    ViewCompat.replaceAccessibilityAction(
      seekBar,
      AccessibilityActionCompat.ACTION_SCROLL_FORWARD,
      null,
    ) { _, _ ->
      moveTo(seekBar, value + seekBarIncrement)
    }
  }

  private fun moveTo(seekBar: SeekBar, newValue: Int): Boolean {
    // Set as the user's own change, so that the preference saves it and calls its listener.
    val arguments = Bundle()
    arguments.putFloat(
      AccessibilityNodeInfoCompat.ACTION_ARGUMENT_PROGRESS_VALUE,
      (newValue.coerceIn(min, max) - min).toFloat(),
    )
    val moved =
      seekBar.performAccessibilityAction(android.R.id.accessibilityActionSetProgress, arguments)
    describe(seekBar)
    return moved
  }

  private fun describe(seekBar: SeekBar) {
    ViewCompat.setStateDescription(
      seekBar,
      context.getString(R.string.tb_template_percent, value.toString()),
    )
  }
}
