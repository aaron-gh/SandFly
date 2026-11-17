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

import android.view.accessibility.AccessibilityWindowInfo
import com.google.android.accessibility.talkback.TalkBackService
import com.google.android.accessibility.talkback.compositor.Compositor
import com.google.android.accessibility.utils.Filter
import com.google.android.accessibility.utils.FormFactorUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * Swipes past the first or last item of the navigation bar or the status bar go on into the app,
 * stopping at the end of the screen as Wrap around and Stop at the last item before wrapping say.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SystemBarExitTest {
  private lateinit var service: TalkBackService
  private lateinit var processor: FocusProcessorForLogicalNavigation

  @Before
  fun setUp() {
    service = Robolectric.buildService(TalkBackService::class.java).get()
    FormFactorUtils.initialize(service)
    processor =
      FocusProcessorForLogicalNavigation(
        service,
        null,
        AccessibilityFocusMonitor(service, null, null),
        null,
        null,
        Compositor.TextComposer { _, _, _ -> null },
        null,
      )
    FocusProcessorForLogicalNavigation.setWrapAround(true)
    FocusProcessorForLogicalNavigation.setStopBeforeWrap(true)
  }

  @After
  fun tearDown() {
    FocusProcessorForLogicalNavigation.setWrapAround(true)
    FocusProcessorForLogicalNavigation.setStopBeforeWrap(true)
  }

  private fun stoppedAtEdge(): Boolean = ReflectionHelpers.getField(processor, "stoppedAtEdge")

  @Test
  fun intoTheApp_neverStops() {
    assertFalse(processor.stopsLeavingSystemBar(/* pastScreenEnd= */ false, /* shouldWrap= */ true))
    FocusProcessorForLogicalNavigation.setWrapAround(false)
    assertFalse(processor.stopsLeavingSystemBar(false, true))
  }

  @Test
  fun pastTheScreenEnd_stopsOnceThenGoesOn() {
    assertTrue(processor.stopsLeavingSystemBar(true, true))
    assertFalse(stoppedAtEdge())
    assertFalse(processor.stopsLeavingSystemBar(true, true))
  }

  @Test
  fun pastTheScreenEnd_stopBeforeWrapOff_goesOnAtOnce() {
    FocusProcessorForLogicalNavigation.setStopBeforeWrap(false)
    assertFalse(processor.stopsLeavingSystemBar(true, true))
  }

  @Test
  fun pastTheScreenEnd_wrapAroundOff_alwaysStops() {
    FocusProcessorForLogicalNavigation.setWrapAround(false)
    FocusProcessorForLogicalNavigation.setStopBeforeWrap(false)
    assertTrue(processor.stopsLeavingSystemBar(true, true))
    assertTrue(stoppedAtEdge())
    assertTrue(processor.stopsLeavingSystemBar(true, true))
  }

  @Test
  fun pastTheScreenEnd_navigationThatDoesNotWrap_alwaysStops() {
    assertTrue(processor.stopsLeavingSystemBar(true, false))
    assertTrue(processor.stopsLeavingSystemBar(true, false))
  }

  @Test
  fun appWindows_focusedAppWindowComesFirst() {
    val top = window(1, AccessibilityWindowInfo.TYPE_APPLICATION)
    val focused = window(2, AccessibilityWindowInfo.TYPE_APPLICATION, focused = true)
    val keyboard = window(3, AccessibilityWindowInfo.TYPE_INPUT_METHOD)
    val navBar = window(4, AccessibilityWindowInfo.TYPE_SYSTEM)
    val order = listOf(top, focused, keyboard, navBar)
    val traversal = Ordered(order)

    assertEquals(
      listOf(focused, keyboard),
      FocusProcessorForLogicalNavigation.appWindowsNextToSystemBar(
        order,
        traversal,
        notBar(navBar),
        navBar,
        /* forward= */ false,
      ),
    )
    assertEquals(
      listOf(focused, top),
      FocusProcessorForLogicalNavigation.appWindowsNextToSystemBar(
        order,
        traversal,
        notBar(navBar),
        navBar,
        /* forward= */ true,
      ),
    )
  }

  @Test
  fun appWindows_withoutFocusedWindow_takesTheNextWindowInOrder() {
    val statusBar = window(1, AccessibilityWindowInfo.TYPE_SYSTEM)
    val app = window(2, AccessibilityWindowInfo.TYPE_APPLICATION)
    val order = listOf(app, statusBar)

    assertEquals(
      listOf(app),
      FocusProcessorForLogicalNavigation.appWindowsNextToSystemBar(
        order,
        Ordered(order),
        notBar(statusBar),
        statusBar,
        /* forward= */ true,
      ),
    )
  }

  /** Goes through [order] and around, as Android's window order does. */
  private inner class Ordered(private val order: List<AccessibilityWindowInfo>) :
    WindowTraversal(service) {
    override fun getNextWindow(currentWindow: AccessibilityWindowInfo): AccessibilityWindowInfo? =
      order[(order.indexOf(currentWindow) + 1) % order.size]

    override fun getPreviousWindow(
      currentWindow: AccessibilityWindowInfo
    ): AccessibilityWindowInfo? = order[(order.indexOf(currentWindow) - 1 + order.size) % order.size]
  }

  private fun notBar(bar: AccessibilityWindowInfo): Filter<AccessibilityWindowInfo> =
    object : Filter<AccessibilityWindowInfo>() {
      override fun accept(obj: AccessibilityWindowInfo?) = obj != null && obj != bar
    }

  private fun window(id: Int, type: Int, focused: Boolean = false): AccessibilityWindowInfo {
    val window = AccessibilityWindowInfo.obtain()
    val c = AccessibilityWindowInfo::class.java
    c.getDeclaredMethod("setId", Int::class.javaPrimitiveType).invoke(window, id)
    c.getDeclaredMethod("setType", Int::class.javaPrimitiveType).invoke(window, type)
    c.getDeclaredMethod("setFocused", Boolean::class.javaPrimitiveType).invoke(window, focused)
    return window
  }
}
