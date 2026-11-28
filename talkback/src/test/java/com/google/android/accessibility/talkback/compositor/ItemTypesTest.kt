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

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.utils.monitor.CollectionState
import com.google.android.accessibility.utils.monitor.InputModeTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ItemTypesTest {
  private lateinit var context: Context
  private lateinit var globalVariables: GlobalVariables

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    val service = Robolectric.buildService(TalkBackService::class.java).get()
    globalVariables = GlobalVariables(service, InputModeTracker(), CollectionState(), null)
  }

  private fun node(
    className: String? = null,
    chromeRole: String? = null,
    roleDescription: String? = null,
    heading: Boolean = false,
    clickable: Boolean = false,
  ): AccessibilityNodeInfoCompat {
    val node = AccessibilityNodeInfoCompat.wrap(AccessibilityNodeInfo())
    node.className = className
    chromeRole?.let { node.unwrap().extras.putCharSequence("AccessibilityNodeInfo.chromeRole", it) }
    node.roleDescription = roleDescription
    node.isHeading = heading
    node.isClickable = clickable
    return node
  }

  @Test
  fun settingLists_everyType() {
    val values = context.resources.getStringArray(R.array.pref_item_types_spoken_values).toList()
    assertEquals(ItemTypes.ALL, values)
    assertEquals(
      values.size,
      context.resources.getStringArray(R.array.pref_item_types_spoken_entries).size,
    )
  }

  @Test
  fun unspoken_isEmptyUntilTheSettingIsChanged() {
    val prefs = context.getSharedPreferences("item_types_test", Context.MODE_PRIVATE)
    assertTrue(ItemTypes.unspoken(prefs).isEmpty())
    prefs.edit().putStringSet(ItemTypes.PREF_SPOKEN, ItemTypes.ALL.toSet()).commit()
    assertTrue(ItemTypes.unspoken(prefs).isEmpty())
    prefs.edit().putStringSet(ItemTypes.PREF_SPOKEN, setOf(ItemTypes.LINK)).commit()
    assertEquals(ItemTypes.ALL.toSet() - ItemTypes.LINK, ItemTypes.unspoken(prefs))
    prefs.edit().putString(ItemTypes.PREF_SPOKEN, "button").commit()
    assertTrue(ItemTypes.unspoken(prefs).isEmpty())
  }

  @Test
  fun unspoken_leavesOutUnknownValues() {
    assertEquals(
      ItemTypes.ALL.toSet() - ItemTypes.BUTTON,
      ItemTypes.unspoken(setOf(ItemTypes.BUTTON, "removed_type")),
    )
  }

  @Test
  fun nativeItems_haveTheTypeOfTheirRole() {
    assertEquals(ItemTypes.BUTTON, ItemTypes.of(node("android.widget.Button")))
    assertEquals(
      ItemTypes.BUTTON,
      ItemTypes.of(node("android.widget.ImageButton", clickable = true)),
    )
    assertEquals(ItemTypes.CHECKBOX, ItemTypes.of(node("android.widget.CheckBox")))
    assertEquals(ItemTypes.SWITCH, ItemTypes.of(node("android.widget.Switch")))
    assertEquals(ItemTypes.SWITCH, ItemTypes.of(node("android.widget.ToggleButton")))
    assertEquals(ItemTypes.RADIO_BUTTON, ItemTypes.of(node("android.widget.RadioButton")))
    assertEquals(ItemTypes.EDIT_BOX, ItemTypes.of(node("android.widget.EditText")))
    assertEquals(ItemTypes.DROP_DOWN_LIST, ItemTypes.of(node("android.widget.Spinner")))
    assertEquals(ItemTypes.SLIDER, ItemTypes.of(node("android.widget.SeekBar")))
    assertEquals(ItemTypes.PROGRESS_BAR, ItemTypes.of(node("android.widget.ProgressBar")))
    assertEquals(ItemTypes.IMAGE, ItemTypes.of(node("android.widget.ImageView")))
    assertEquals(ItemTypes.LIST, ItemTypes.of(node("android.widget.ListView")))
    assertEquals(ItemTypes.GRID, ItemTypes.of(node("android.widget.GridView")))
    assertEquals(ItemTypes.TAB_BAR, ItemTypes.of(node("android.widget.TabWidget")))
    assertEquals(ItemTypes.WEB_VIEW, ItemTypes.of(node("android.webkit.WebView")))
  }

  @Test
  fun webItems_haveTheTypeOfTheirChromeRole() {
    assertEquals(ItemTypes.LINK, ItemTypes.of(node("android.view.View", chromeRole = "link")))
    assertEquals(ItemTypes.HEADING, ItemTypes.of(node("android.view.View", chromeRole = "heading")))
    assertEquals(ItemTypes.TAB, ItemTypes.of(node("android.view.View", chromeRole = "tab")))
    assertEquals(
      ItemTypes.CHECKBOX,
      ItemTypes.of(node("android.widget.CheckBox", chromeRole = "checkBox")),
    )
    assertEquals(
      ItemTypes.OTHER,
      ItemTypes.of(node("android.view.View", chromeRole = "navigation", roleDescription = "nav")),
    )
  }

  @Test
  fun tabsHeadingsAndOtherTypesOfApps() {
    assertEquals(ItemTypes.TAB, ItemTypes.of(node("android.view.View", roleDescription = "Tab")))
    assertEquals(
      ItemTypes.TAB,
      ItemTypes.of(node("com.google.android.material.tabs.TabLayout\$TabView")),
    )
    assertEquals(ItemTypes.HEADING, ItemTypes.of(node("android.widget.TextView", heading = true)))
    assertEquals(
      ItemTypes.BUTTON,
      ItemTypes.of(node("android.widget.Button", heading = true)),
    )
    assertEquals(
      ItemTypes.OTHER,
      ItemTypes.of(node("android.view.View", roleDescription = "Chip")),
    )
  }

  @Test
  fun uncheckedType_isLeftOutOfTheDescription() {
    val button = node("android.widget.Button")
    assertEquals("Button", role(button))
    globalVariables.setUnspokenItemTypes(setOf(ItemTypes.CHECKBOX))
    assertEquals("Button", role(button))
    globalVariables.setUnspokenItemTypes(setOf(ItemTypes.BUTTON))
    assertEquals("", role(button))
    assertEquals("Check box", role(node("android.widget.CheckBox")))
  }

  @Test
  fun uncheckedType_leavesOutTheTypeAnAppGivesTheItem() {
    val chip = node("android.view.View", roleDescription = "Chip")
    assertEquals("Chip", role(chip))
    globalVariables.setUnspokenItemTypes(setOf(ItemTypes.OTHER))
    assertEquals("", role(chip))
  }

  @Test
  fun uncheckedPage_leavesOutPage() {
    val page = node("android.view.View")
    assertEquals(
      "Page",
      AccessibilityNodeFeedbackUtils.getPagerPageRoleDescription(page, context, globalVariables)
        .toString(),
    )
    globalVariables.setUnspokenItemTypes(setOf(ItemTypes.PAGE))
    assertEquals(
      "",
      AccessibilityNodeFeedbackUtils.getPagerPageRoleDescription(page, context, globalVariables)
        .toString(),
    )
  }

  @Test
  fun headingType_followsItsCheckBox() {
    assertTrue(globalVariables.isItemTypeSpoken(ItemTypes.HEADING))
    globalVariables.setUnspokenItemTypes(setOf(ItemTypes.HEADING))
    assertFalse(globalVariables.isItemTypeSpoken(ItemTypes.HEADING))
    assertTrue(globalVariables.isItemTypeSpoken(ItemTypes.LINK))
  }

  private fun role(node: AccessibilityNodeInfoCompat): String =
    AccessibilityNodeFeedbackUtils.defaultRoleDescription(node, context, globalVariables).toString()
}
