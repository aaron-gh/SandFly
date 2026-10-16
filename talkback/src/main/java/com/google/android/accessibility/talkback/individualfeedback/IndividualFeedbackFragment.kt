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

package com.google.android.accessibility.talkback.individualfeedback

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Vibrator
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.view.accessibility.AccessibilityViewCommand
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import com.google.android.accessibility.material.preference.AccessibilitySuiteSwitchPreference
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.preference.base.TalkbackBaseFragment
import com.google.android.accessibility.talkback.soundthemes.SoundThemes
import com.google.android.accessibility.utils.FeatureSupport
import com.google.android.accessibility.utils.SharedPreferencesUtils
import com.google.android.accessibility.utils.output.HapticPatternParser

/**
 * A switch for each sound and each vibration. Each switch has a Preview action, which screen reader
 * users reach from the actions menu, that plays the sound or vibration so the user can tell which
 * one it is. Changing a switch plays nothing.
 */
class IndividualFeedbackFragment : TalkbackBaseFragment() {
  private lateinit var prefs: SharedPreferences
  private val soundPreview = SoundPreview()
  // The vibrator playing a preview, so that leaving the screen stops it like a sound preview.
  private var previewVibrator: Vibrator? = null

  public override fun getTitle(): CharSequence = getText(R.string.title_pref_individual_feedback)

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    val context = requireContext()
    prefs = SharedPreferencesUtils.getSharedPreferences(context)
    val screen = preferenceManager.createPreferenceScreen(context)
    preferenceScreen = screen

    val sounds = category(context, R.string.individual_feedback_sounds_category)
    screen.addPreference(sounds)
    for (item in IndividualFeedbackSettings.SOUNDS) {
      sounds.addPreference(
        FeedbackSwitch(
          context,
          "sound",
          item,
          IndividualFeedbackSettings.isSoundOn(prefs, item),
          onChange = { on -> IndividualFeedbackSettings.setSoundOn(prefs, item, on) },
          preview = { soundPreview.play(context, prefs, item) },
        )
      )
    }

    if (FeatureSupport.isVibratorSupported(context)) {
      val vibrations = category(context, R.string.individual_feedback_vibrations_category)
      screen.addPreference(vibrations)
      for (item in IndividualFeedbackSettings.VIBRATIONS) {
        if (
          !BrailleKeyboardVibrations.isAvailable() &&
            item in IndividualFeedbackSettings.BRAILLE_KEYBOARD_VIBRATIONS
        ) {
          continue
        }
        vibrations.addPreference(
          FeedbackSwitch(
            context,
            "vibration",
            item,
            IndividualFeedbackSettings.isVibrationOn(prefs, item),
            onChange = { on -> IndividualFeedbackSettings.setVibrationOn(prefs, item, on) },
            preview = { playVibration(context, item) },
          )
        )
      }
    }
  }

  override fun onPause() {
    super.onPause()
    soundPreview.stop()
    previewVibrator?.cancel()
    previewVibrator = null
  }

  private fun category(context: Context, title: Int) =
    PreferenceCategory(context).apply {
      setTitle(title)
      isIconSpaceReserved = false
    }

  /** A switch for one sound or vibration, with a Preview action that plays it. */
  private class FeedbackSwitch(
    context: Context,
    kind: String,
    item: FeedbackItem,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    private val preview: () -> Unit,
  ) : AccessibilitySuiteSwitchPreference(context) {
    init {
      key = "pref_individual_${kind}_${item.key}"
      isPersistent = false
      isIconSpaceReserved = false
      setTitle(item.title)
      isChecked = checked
      setOnPreferenceChangeListener { _, newValue ->
        onChange(newValue as Boolean)
        true
      }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
      super.onBindViewHolder(holder)
      // Rows are reused for other switches, so every bind replaces the action.
      val label = context.getString(R.string.individual_feedback_action_preview)
      ViewCompat.replaceAccessibilityAction(
        holder.itemView,
        AccessibilityActionCompat(ACTION_PREVIEW, label),
        label,
        AccessibilityViewCommand { _, _ ->
          preview()
          true
        },
      )
    }
  }

  private fun playVibration(context: Context, item: FeedbackItem) {
    val vibrator = context.getSystemService(Vibrator::class.java) ?: return
    // The theme in use may replace the vibration, as it can give control sounds theirs.
    val switch = item.resourceNames.first()
    val themeVibrations = SoundThemes.feedback(context, prefs).vibrations
    val themePattern =
      themeVibrations[switch]
        ?: themeVibrations.entries.firstOrNull { SoundVibrations.switchOf(it.key) == switch }?.value
    if (themePattern == null && switch in SoundVibrations.BRAILLE_KEYBOARD) {
      // The braille keyboard plays its own vibrations, which aren't pattern resources.
      if (BrailleKeyboardVibrations.preview(context, switch)) {
        previewVibrator = vibrator
      }
      return
    }
    val pattern =
      themePattern
        ?: resourceId(context, switch, "array").takeIf { it != 0 }?.let {
          context.resources.getIntArray(it)
        }
    if (pattern == null || pattern.isEmpty()) {
      return
    }
    vibrator.vibrate(HapticPatternParser(vibrator).parse(pattern))
    previewVibrator = vibrator
  }

  // By name, because the braille sounds are in a module whose R class talkback cannot see.
  private fun resourceId(context: Context, name: String, type: String): Int =
    context.resources.getIdentifier(name, type, context.packageName)

  private companion object {
    val ACTION_PREVIEW = R.id.accessibility_custom_action_0
  }
}
