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

package com.google.android.accessibility.talkback.compositor

import android.content.SharedPreferences
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.controlsounds.ControlSounds
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.Role

/**
 * The item types of Verbosity > Item types to speak. Speak item type leaves out the type of an
 * item whose type is unchecked; its name, state and hints are still spoken.
 *
 * The setting stores the checked types, so a type added here later is unchecked for anyone who
 * changed the setting before, unless a migration adds it to their set.
 */
object ItemTypes {
  /** The setting, a set of the checked types. Unset means all of them. */
  const val PREF_SPOKEN = "pref_item_types_spoken"

  const val BUTTON = "button"
  const val CHECKBOX = "checkbox"
  const val SWITCH = "switch"
  const val RADIO_BUTTON = "radio_button"
  const val EDIT_BOX = "edit_box"
  const val DROP_DOWN_LIST = "drop_down_list"
  const val SLIDER = "slider"
  const val PROGRESS_BAR = "progress_bar"
  const val LINK = "link"
  const val HEADING = "heading"
  const val IMAGE = "image"
  const val TAB = "tab"
  const val TAB_BAR = "tab_bar"
  const val LIST = "list"
  const val GRID = "grid"
  const val PAGE = "page"
  const val WEB_VIEW = "web_view"

  /** Types that apps name themselves and that are none of the others, such as "Chip". */
  const val OTHER = "other"

  /** All types, in the order of the setting's list. Matches pref_item_types_spoken_values. */
  @JvmField
  val ALL: List<String> =
    listOf(
      BUTTON,
      CHECKBOX,
      SWITCH,
      RADIO_BUTTON,
      EDIT_BOX,
      DROP_DOWN_LIST,
      SLIDER,
      PROGRESS_BAR,
      LINK,
      HEADING,
      IMAGE,
      TAB,
      TAB_BAR,
      LIST,
      GRID,
      PAGE,
      WEB_VIEW,
      OTHER,
    )

  /** The types of the roles Chrome gives web elements. */
  private val CHROME_TYPES =
    mapOf(
      "button" to BUTTON,
      "popUpButton" to BUTTON,
      "disclosureTriangle" to BUTTON,
      "checkBox" to CHECKBOX,
      "menuItemCheckBox" to CHECKBOX,
      "switch" to SWITCH,
      "toggleButton" to SWITCH,
      "radioButton" to RADIO_BUTTON,
      "menuItemRadio" to RADIO_BUTTON,
      "textField" to EDIT_BOX,
      "searchBox" to EDIT_BOX,
      "textFieldWithComboBox" to EDIT_BOX,
      "comboBoxSelect" to DROP_DOWN_LIST,
      "comboBoxMenuButton" to DROP_DOWN_LIST,
      "comboBoxGrouping" to DROP_DOWN_LIST,
      "slider" to SLIDER,
      "spinButton" to SLIDER,
      "progressIndicator" to PROGRESS_BAR,
      "meter" to PROGRESS_BAR,
      "link" to LINK,
      "heading" to HEADING,
      "image" to IMAGE,
      "svgRoot" to IMAGE,
      "canvas" to IMAGE,
      "tab" to TAB,
      "tabList" to TAB_BAR,
      "list" to LIST,
      "listBox" to LIST,
      "grid" to GRID,
      "table" to GRID,
      "treeGrid" to GRID,
    )

  /**
   * Returns the types that are left out: the ones unchecked in [prefs]. Empty while the setting is
   * unset or not a set.
   */
  @JvmStatic
  fun unspoken(prefs: SharedPreferences): Set<String> {
    val spoken =
      try {
        prefs.getStringSet(PREF_SPOKEN, null)
      } catch (e: ClassCastException) {
        null
      } ?: return emptySet()
    return unspoken(spoken)
  }

  /** Returns the types that are not in [spoken]. */
  @JvmStatic
  fun unspoken(spoken: Set<String>): Set<String> = ALL.filterNotTo(HashSet()) { it in spoken }

  /** Returns the type of [node], as spoken by Speak item type. */
  @JvmStatic
  fun of(node: AccessibilityNodeInfoCompat): String {
    CHROME_TYPES[AccessibilityNodeInfoUtils.getChromeRole(node)?.toString()]?.let {
      return it
    }
    ofRole(Role.getRole(node))?.let {
      return it
    }
    return when {
      ControlSounds.isTab(node, node.parent) -> TAB
      AccessibilityNodeInfoUtils.isHeading(node) -> HEADING
      else -> OTHER
    }
  }

  /** Returns the type of the [role] of an item, or null for roles with no type of their own. */
  @JvmStatic
  fun ofRole(role: Int): String? =
    when (role) {
      Role.ROLE_BUTTON,
      Role.ROLE_IMAGE_BUTTON,
      Role.ROLE_FLOATING_ACTION_BUTTON,
      Role.ROLE_VOICE_DICTATION_BUTTON -> BUTTON
      Role.ROLE_CHECK_BOX -> CHECKBOX
      Role.ROLE_SWITCH,
      Role.ROLE_TOGGLE_BUTTON -> SWITCH
      Role.ROLE_RADIO_BUTTON,
      Role.ROLE_CHECKED_TEXT_VIEW -> RADIO_BUTTON
      Role.ROLE_EDIT_TEXT -> EDIT_BOX
      Role.ROLE_DROP_DOWN_LIST -> DROP_DOWN_LIST
      Role.ROLE_SEEK_CONTROL -> SLIDER
      Role.ROLE_PROGRESS_BAR -> PROGRESS_BAR
      Role.ROLE_IMAGE -> IMAGE
      Role.ROLE_TAB_BAR -> TAB_BAR
      Role.ROLE_LIST -> LIST
      Role.ROLE_GRID,
      Role.ROLE_STAGGERED_GRID -> GRID
      Role.ROLE_PAGER -> PAGE
      Role.ROLE_WEB_VIEW -> WEB_VIEW
      else -> null
    }
}
