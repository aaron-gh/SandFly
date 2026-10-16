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

package com.google.android.accessibility.talkback.controlsounds

import android.content.Context
import android.graphics.Rect
import android.text.TextUtils
import androidx.annotation.IdRes
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityWindowInfoCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.Role
import com.google.android.accessibility.utils.traversal.ReorderedChildrenIterator
import com.google.android.libraries.accessibility.utils.device.ScreenUtils

/**
 * Control sounds, as in the Unspoken add-on for NVDA: each kind of control has its own sound,
 * played from where the control is on the screen, in place of the focus sound. The kinds of
 * control are Unspoken's. Backtalk has no sounds of its own for them: each plays the custom sound
 * the user chose for it, and a control without one plays the usual focus sound.
 */
object ControlSounds {
  /**
   * The ID that stands for each kind of control, by the name of its custom sound and individual
   * sound switch.
   */
  @JvmField
  val SOUNDS: Map<String, Int> =
    linkedMapOf(
      "control_button" to R.id.control_button,
      "control_checkbox" to R.id.control_checkbox,
      "control_radio_button" to R.id.control_radio_button,
      "control_edit_text" to R.id.control_edit_text,
      "control_combo_box" to R.id.control_combo_box,
      "control_slider" to R.id.control_slider,
      "control_link" to R.id.control_link,
      "control_image" to R.id.control_image,
      "control_clock" to R.id.control_clock,
      "control_tab" to R.id.control_tab,
      "control_menu_item" to R.id.control_menu_item,
      "control_list_item" to R.id.control_list_item,
      "control_tree_item" to R.id.control_tree_item,
    )

  /** Children looked at when a focused item stands for the controls inside it. */
  private const val MAX_CHILDREN_SEARCHED = 40

  /** Sounds for the roles Chrome gives web elements, as Unspoken maps NVDA's roles. */
  private val CHROME_CONTROLS =
    mapOf(
      "button" to R.id.control_button,
      "popUpButton" to R.id.control_button,
      "disclosureTriangle" to R.id.control_button,
      "checkBox" to R.id.control_checkbox,
      "switch" to R.id.control_checkbox,
      "toggleButton" to R.id.control_checkbox,
      "menuItemCheckBox" to R.id.control_checkbox,
      "radioButton" to R.id.control_radio_button,
      "menuItemRadio" to R.id.control_radio_button,
      "textField" to R.id.control_edit_text,
      "searchBox" to R.id.control_edit_text,
      "comboBoxSelect" to R.id.control_combo_box,
      "comboBoxMenuButton" to R.id.control_combo_box,
      "comboBoxGrouping" to R.id.control_combo_box,
      "textFieldWithComboBox" to R.id.control_combo_box,
      "slider" to R.id.control_slider,
      "spinButton" to R.id.control_slider,
      "link" to R.id.control_link,
      "image" to R.id.control_image,
      "svgRoot" to R.id.control_image,
      "canvas" to R.id.control_image,
    )

  private val CHROME_ITEMS =
    mapOf(
      "tab" to R.id.control_tab,
      "menuItem" to R.id.control_menu_item,
      "menuListOption" to R.id.control_menu_item,
      "listItem" to R.id.control_list_item,
      "listBoxOption" to R.id.control_list_item,
      "treeItem" to R.id.control_tree_item,
    )

  /**
   * Returns the sound for the kind of control [node] is, or 0 if it has none. Controls come before
   * the items they can sit in, so a button in a list sounds like a button.
   */
  @JvmStatic
  @IdRes
  fun soundForRole(node: AccessibilityNodeInfoCompat?): Int {
    if (node == null) return 0
    return controlSound(node).takeIf { it != 0 } ?: itemSound(node)
  }

  /**
   * Returns the sound for focusing [node], or 0 if it has none. An item that is not a control
   * itself, like a row in Settings, gets the sound of the first control inside it that is read as
   * part of it, like the row's switch, and otherwise the sound of the kind of item it is, like a
   * list item. Keys on the keyboard keep the usual focus sound.
   */
  @JvmStatic
  @IdRes
  fun soundForFocus(node: AccessibilityNodeInfoCompat?): Int {
    if (node == null) return 0
    if (AccessibilityNodeInfoUtils.getWindowType(node) ==
      AccessibilityWindowInfoCompat.TYPE_INPUT_METHOD
    ) {
      return 0
    }
    controlSound(node).let { if (it != 0) return it }
    soundOfChildren(node, intArrayOf(MAX_CHILDREN_SEARCHED)).let { if (it != 0) return it }
    return itemSound(node)
  }

  /** Controls: things that do something when activated, and images. */
  @IdRes
  private fun controlSound(node: AccessibilityNodeInfoCompat): Int {
    CHROME_CONTROLS[chromeRole(node)]?.let { return it }
    val sound =
      when (Role.getRole(node)) {
        Role.ROLE_BUTTON,
        Role.ROLE_IMAGE_BUTTON,
        Role.ROLE_FLOATING_ACTION_BUTTON,
        Role.ROLE_VOICE_DICTATION_BUTTON -> R.id.control_button
        // Unspoken plays the checkbox sound for toggle buttons, and switches are the same thing.
        Role.ROLE_CHECK_BOX,
        Role.ROLE_SWITCH,
        Role.ROLE_TOGGLE_BUTTON -> R.id.control_checkbox
        Role.ROLE_RADIO_BUTTON,
        Role.ROLE_CHECKED_TEXT_VIEW -> R.id.control_radio_button
        Role.ROLE_EDIT_TEXT -> R.id.control_edit_text
        Role.ROLE_DROP_DOWN_LIST -> R.id.control_combo_box
        Role.ROLE_SEEK_CONTROL,
        Role.ROLE_NUMBER_PICKER -> R.id.control_slider
        Role.ROLE_IMAGE -> R.id.control_image
        else -> 0
      }
    if (sound != 0) return sound
    return if (className(node).contains("TextClock")) R.id.control_clock else 0
  }

  /** Items: the parts of tab bars, menus and lists, which are often not controls of their own. */
  @IdRes
  private fun itemSound(node: AccessibilityNodeInfoCompat): Int {
    CHROME_ITEMS[chromeRole(node)]?.let { return it }
    val className = className(node)
    val parent = node.parent
    val parentClassName = parent?.let { className(it) } ?: ""
    val parentRole = Role.getRole(parent)
    return when {
      Role.getRole(node) == Role.ROLE_ACTION_BAR_TAB ||
        className.contains("TabView") ||
        parentRole == Role.ROLE_TAB_BAR ||
        parentClassName.contains("TabLayout") ||
        // Compose describes its tabs only by this role description.
        node.roleDescription?.toString().equals("tab", ignoreCase = true) -> R.id.control_tab
      className.contains("ListMenuItemView") ||
        parentClassName.contains("MenuDropDownListView") -> R.id.control_menu_item
      node.collectionItemInfo != null ||
        parentRole == Role.ROLE_LIST ||
        parentRole == Role.ROLE_GRID ||
        parentRole == Role.ROLE_STAGGERED_GRID -> R.id.control_list_item
      else -> 0
    }
  }

  private fun chromeRole(node: AccessibilityNodeInfoCompat): String? =
    AccessibilityNodeInfoUtils.getChromeRole(node)?.toString()

  private fun className(node: AccessibilityNodeInfoCompat): String =
    node.className?.toString() ?: ""

  /**
   * Searches the children that are read as part of [node], in reading order, for a control. The
   * descriptions only include children when the node has no content description of its own, and
   * never inside a web view. Images are skipped, because rows often start with an icon before
   * their control.
   */
  private fun soundOfChildren(node: AccessibilityNodeInfoCompat, budget: IntArray): Int {
    if (Role.getRole(node) == Role.ROLE_WEB_VIEW ||
      !TextUtils.isEmpty(node.contentDescription)
    ) {
      return 0
    }
    val children = ReorderedChildrenIterator.createAscendingIterator(node)
    while (children.hasNext() && budget[0] > 0) {
      val child = children.next() ?: continue
      budget[0]--
      if (!AccessibilityNodeInfoUtils.isVisible(child) ||
        AccessibilityNodeInfoUtils.isAccessibilityFocusable(child)
      ) {
        continue
      }
      val own = controlSound(child)
      if (own != 0 && own != R.id.control_image) return own
      val sound = soundOfChildren(child, budget)
      if (sound != 0) return sound
    }
    return 0
  }

  /**
   * Returns where the middle of [node] is, as fractions of the screen from its left and top
   * edges, or null if the node has no place on the screen.
   */
  @JvmStatic
  fun screenPosition(node: AccessibilityNodeInfoCompat, context: Context): FloatArray? {
    val bounds = Rect()
    node.getBoundsInScreen(bounds)
    val screen = ScreenUtils.getRealScreenSize(context)
    if (bounds.isEmpty || screen.x <= 0 || screen.y <= 0) return null
    return floatArrayOf(
      (bounds.exactCenterX() / screen.x).coerceIn(0f, 1f),
      (bounds.exactCenterY() / screen.y).coerceIn(0f, 1f),
    )
  }
}
