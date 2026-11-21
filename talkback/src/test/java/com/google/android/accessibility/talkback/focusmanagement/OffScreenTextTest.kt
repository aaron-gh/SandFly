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

import android.graphics.Rect
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Checks which partly visible swipe targets are scrolled on screen before they are focused, as
 * their text is off screen, instead of being spoken first.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OffScreenTextTest {

  @Test
  fun rowWithItsTitleCutOffAtTheListEdgeHasTextOffScreen() {
    // Xiaomi Settings (SandFly#155): a clickable row with no text of its own, its title cut off
    // by the list's bottom padding behind the navigation bar.
    val row = node()
    node(row)
    node(row, text = "Home screen", visible = false)
    assertTrue(FocusProcessorForLogicalNavigation.hasTextOffScreen(row))
  }

  @Test
  fun textInsideAContainerOffScreenCounts() {
    val row = node()
    node(row)
    val texts = node(row, visible = false)
    node(texts, text = "Display and brightness", visible = false)
    assertTrue(FocusProcessorForLogicalNavigation.hasTextOffScreen(row))
  }

  @Test
  fun contentDescriptionOffScreenCounts() {
    val row = node()
    node(row, description = "Wi-Fi", visible = false)
    assertTrue(FocusProcessorForLogicalNavigation.hasTextOffScreen(row))
  }

  @Test
  fun rowWithAllItsTextOnScreenIsSpokenFirst() {
    val row = node()
    node(row, text = "Home screen")
    node(row, text = "Wallpaper, icons")
    assertFalse(FocusProcessorForLogicalNavigation.hasTextOffScreen(row))
  }

  @Test
  fun childrenOffScreenWithoutTextDoNotCount() {
    val row = node()
    node(row, text = "Home screen")
    node(row, visible = false)
    node(row, text = "   ", visible = false)
    assertFalse(FocusProcessorForLogicalNavigation.hasTextOffScreen(row))
  }

  @Test
  fun itemReadByItsOwnDescriptionIsSpokenFirst() {
    val row = node(description = "Home screen")
    node(row, text = "Home screen", visible = false)
    assertFalse(FocusProcessorForLogicalNavigation.hasTextOffScreen(row))
  }

  @Test
  fun lookingStopsAfterTheLimit() {
    val row = node()
    repeat(40) { node(row) }
    node(row, text = "Late", visible = false)
    assertFalse(FocusProcessorForLogicalNavigation.hasTextOffScreen(row))
  }

  @Test
  fun textInsideTheListUnderASystemBarIsNotOffScreen() {
    val row = node()
    node(row, text = "Home screen", visible = false, bounds = Rect(216, 2487, 707, 2555))
    val list = Rect(0, 647, 1200, 2608)
    val bar = listOf(Rect(0, 2466, 1200, 2608))
    assertFalse(FocusProcessorForLogicalNavigation.hasTextOffScreen(row, list, bar))
  }

  @Test
  fun textInsideTheListNotUnderASystemBarIsOffScreen() {
    val row = node()
    node(row, text = "Home screen", visible = false, bounds = Rect(216, 1000, 707, 1100))
    val list = Rect(0, 647, 1200, 2608)
    val bar = listOf(Rect(0, 2466, 1200, 2608))
    assertTrue(FocusProcessorForLogicalNavigation.hasTextOffScreen(row, list, bar))
  }

  @Test
  fun textOutsideTheListIsOffScreenEvenUnderASystemBar() {
    val row = node()
    node(row, text = "Home screen", visible = false, bounds = Rect(216, 2487, 707, 2700))
    val list = Rect(0, 647, 1200, 2608)
    val bar = listOf(Rect(0, 2466, 1200, 2800))
    assertTrue(FocusProcessorForLogicalNavigation.hasTextOffScreen(row, list, bar))
  }

  private fun node(
    parent: AccessibilityNodeInfoCompat? = null,
    text: String? = null,
    description: String? = null,
    visible: Boolean = true,
    bounds: Rect = Rect(0, 0, 100, 100),
  ): AccessibilityNodeInfoCompat {
    val raw = AccessibilityNodeInfo.obtain()
    // Distinct sources keep the nodes unequal.
    raw.setSource(View(RuntimeEnvironment.getApplication()))
    raw.className = if (text != null) "android.widget.TextView" else "android.widget.LinearLayout"
    raw.text = text
    raw.contentDescription = description
    raw.isVisibleToUser = visible
    raw.setBoundsInScreen(bounds)
    val node = AccessibilityNodeInfoCompat.wrap(raw)
    if (parent != null) {
      shadowOf(parent.unwrap()).addChild(raw)
    }
    return node
  }
}
