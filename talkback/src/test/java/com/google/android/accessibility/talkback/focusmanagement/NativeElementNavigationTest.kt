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

package com.google.android.accessibility.talkback.focusmanagement

import android.util.Pair
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionInfoCompat
import com.google.android.accessibility.talkback.focusmanagement.NavigationTarget.TargetType
import com.google.android.accessibility.talkback.focusmanagement.action.NavigationAction
import com.google.android.accessibility.utils.input.CursorGranularity
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_BACKWARD
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_FORWARD
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

/** The element targets that web pages offer also find nodes outside web content. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NativeElementNavigationTest {
  private fun node(className: String, configure: AccessibilityNodeInfoCompat.() -> Unit = {}) =
    AccessibilityNodeInfoCompat.wrap(AccessibilityNodeInfo.obtain()).apply {
      this.className = className
      packageName = "example.app"
      text = "Item"
      isVisibleToUser = true
      isScreenReaderFocusable = true
      configure()
    }

  private fun accepts(@TargetType target: Int, node: AccessibilityNodeInfoCompat) =
    NavigationTarget.createNodeFilter(target, null).accept(node)

  @Test
  fun buttonTargetFindsButtons() {
    assertTrue(accepts(NavigationTarget.TARGET_HTML_ELEMENT_BUTTON, node("android.widget.Button")))
    assertFalse(
      accepts(NavigationTarget.TARGET_HTML_ELEMENT_BUTTON, node("android.widget.TextView"))
    )
  }

  @Test
  fun editFieldAndCheckboxTargetsFindTheirRoles() {
    assertTrue(
      accepts(NavigationTarget.TARGET_HTML_ELEMENT_EDIT_FIELD, node("android.widget.EditText"))
    )
    assertTrue(
      accepts(NavigationTarget.TARGET_HTML_ELEMENT_CHECKBOX, node("android.widget.CheckBox"))
    )
    assertTrue(accepts(NavigationTarget.TARGET_HTML_ELEMENT_CHECKBOX, node("android.widget.Switch")))
  }

  @Test
  fun everyHeadingLevelFindsHeadings() {
    val heading = node("android.widget.TextView") { isHeading = true }
    for (
      target in
        listOf(
          NavigationTarget.TARGET_HTML_ELEMENT_HEADING_1,
          NavigationTarget.TARGET_HTML_ELEMENT_HEADING_6,
        )
    ) {
      assertTrue(accepts(target, heading))
    }
  }

  @Test
  fun visitedLinkTargetFindsNothing() {
    assertFalse(
      accepts(NavigationTarget.TARGET_HTML_ELEMENT_VISITED_LINK, node("android.widget.TextView"))
    )
  }

  @Test
  fun visitedLinksAreNotScrolledFor() {
    assertTrue(NavigationTarget.isMacroGranularity(NavigationTarget.TARGET_HTML_ELEMENT_BUTTON))
    assertTrue(NavigationTarget.isMacroGranularity(NavigationTarget.TARGET_HTML_ELEMENT_HEADING_2))
    assertFalse(
      NavigationTarget.isMacroGranularity(NavigationTarget.TARGET_HTML_ELEMENT_VISITED_LINK)
    )
    assertFalse(NavigationTarget.isMacroGranularity(NavigationTarget.TARGET_HTML_ELEMENT_ROW_BOUNDS))
  }

  @Test
  fun listAndTableAreDifferentContainers() {
    val list =
      node("android.view.ViewGroup") {
        setCollectionInfo(CollectionInfoCompat.obtain(5, 1, false, 0))
      }
    val table =
      node("android.view.ViewGroup") {
        setCollectionInfo(CollectionInfoCompat.obtain(5, 3, false, 0))
      }
    val listFilter = NavigationTarget.createContainerFilter(NavigationTarget.TARGET_HTML_ELEMENT_LIST)
    val tableFilter =
      NavigationTarget.createContainerFilter(NavigationTarget.TARGET_HTML_ELEMENT_TABLE)
    assertTrue(listFilter.accept(list))
    assertFalse(listFilter.accept(table))
    assertTrue(tableFilter.accept(table))
    assertFalse(tableFilter.accept(list))
  }

  @Test
  fun landmarkContainersHaveATitleOrAreBars() {
    val filter =
      NavigationTarget.createContainerFilter(NavigationTarget.TARGET_HTML_ELEMENT_ARIA_LANDMARK)
    assertTrue(filter.accept(node("android.view.View") { paneTitle = "Search results" }))
    assertTrue(filter.accept(node("androidx.appcompat.widget.Toolbar")))
    assertFalse(filter.accept(node("android.view.View")))
  }

  @Test
  fun webGranularitiesAreSupportedOutsideWebContent() {
    val supported = ArrayList<CursorGranularity>()
    CursorGranularity.extractFromMask(0, false, null, supported)
    assertTrue(supported.contains(CursorGranularity.WEB_BUTTON))
    assertTrue(supported.contains(CursorGranularity.WEB_H3))
    assertTrue(supported.contains(CursorGranularity.WEB_EDITFIELD))
  }

  private fun targetIndex(
    @TargetType target: Int,
    direction: Int,
    pivot: Pair<Int, Int>,
  ): Pair<Int, Int>? {
    val root =
      node("android.widget.GridView") {
        setCollectionInfo(CollectionInfoCompat.obtain(4, 3, false, 0))
      }
    val action =
      NavigationAction.Builder()
        .setAction(NavigationAction.DIRECTIONAL_NAVIGATION)
        .setDirection(direction)
        .setTarget(target)
        .setPivotIndex(pivot)
        .build()
    val navigation = TableNavigation(action, root, null, null)
    return ReflectionHelpers.callInstanceMethod(
      navigation,
      "getTargetIndexIncrements",
      ClassParameter.from(Int::class.javaPrimitiveType, 1),
    )
  }

  @Test
  fun boundaryTargetsGoToTheEdgesOfTheTable() {
    val middle = Pair.create(1, 1)
    assertEquals(
      Pair.create(1, 2),
      targetIndex(NavigationTarget.TARGET_HTML_ELEMENT_ROW_BOUNDS, SEARCH_FOCUS_FORWARD, middle),
    )
    assertEquals(
      Pair.create(1, 0),
      targetIndex(NavigationTarget.TARGET_HTML_ELEMENT_ROW_BOUNDS, SEARCH_FOCUS_BACKWARD, middle),
    )
    assertEquals(
      Pair.create(3, 1),
      targetIndex(NavigationTarget.TARGET_HTML_ELEMENT_COLUMN_BOUNDS, SEARCH_FOCUS_FORWARD, middle),
    )
    assertEquals(
      Pair.create(0, 0),
      targetIndex(NavigationTarget.TARGET_HTML_ELEMENT_TABLE_BOUNDS, SEARCH_FOCUS_BACKWARD, middle),
    )
    assertEquals(
      Pair.create(3, 2),
      targetIndex(NavigationTarget.TARGET_HTML_ELEMENT_TABLE_BOUNDS, SEARCH_FOCUS_FORWARD, middle),
    )
  }

  @Test
  fun boundaryTargetAlreadyAtTheEdgeHasNoTarget() {
    assertNull(
      targetIndex(
        NavigationTarget.TARGET_HTML_ELEMENT_ROW_BOUNDS,
        SEARCH_FOCUS_FORWARD,
        Pair.create(1, 2),
      )
    )
  }
}
