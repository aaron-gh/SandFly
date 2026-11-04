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

package com.google.android.accessibility.talkback.gesture

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.util.SparseArray
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowAccessibilityService
import org.robolectric.shadows.ShadowAccessibilityWindowInfo
import org.robolectric.util.ReflectionHelpers

/**
 * Finds keys under a finger on Gboard, and counts how often Gboard's window root is fetched, which
 * Android doesn't cache and which waits for Gboard.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
  sdk = [34],
  shadows = [KeyboardExplorationTest.WindowsShadow::class, KeyboardExplorationTest.RootsShadow::class],
)
class KeyboardExplorationTest {

  class TestService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent) {}

    override fun onInterrupt() {}
  }

  /** Lists the windows a test puts on screen, topmost first, in a new list each time. */
  @Implements(AccessibilityService::class)
  class WindowsShadow : ShadowAccessibilityService() {
    var onScreen: List<AccessibilityWindowInfo> = emptyList()

    @Implementation
    override fun getWindowsOnAllDisplays(): SparseArray<List<AccessibilityWindowInfo>> =
      SparseArray<List<AccessibilityWindowInfo>>().apply {
        put(Display.DEFAULT_DISPLAY, ArrayList(onScreen))
      }
  }

  /** Counts how often a window's root is fetched. */
  @Implements(AccessibilityWindowInfo::class)
  class RootsShadow : ShadowAccessibilityWindowInfo() {
    @Implementation
    override fun getRoot(): AccessibilityNodeInfo? {
      rootRequests++
      return super.getRoot()
    }

    companion object {
      var rootRequests = 0
    }
  }

  private lateinit var service: TestService

  @Before
  fun setUp() {
    service = Robolectric.setupService(TestService::class.java)
    val keyboard =
      window(KEYBOARD_ID, AccessibilityWindowInfo.TYPE_INPUT_METHOD, KEYBOARD_BOUNDS, GBOARD)
    keyboard.root.addKey("q", Rect(0, 1600, 108, 1750))
    keyboard.root.addKey("w", Rect(108, 1600, 216, 1750))
    val app = window(APP_ID, AccessibilityWindowInfo.TYPE_APPLICATION, SCREEN_BOUNDS, "com.example")
    Shadow.extract<WindowsShadow>(service).onScreen = listOf(keyboard.window, app.window)
    RootsShadow.rootRequests = 0
  }

  @Test
  fun rootIsFetchedOnceForATouchOnGboard() {
    val root = KeyboardExploration.gboardRootAt(service, Display.DEFAULT_DISPLAY, 50f, 1650f)

    assertNotNull(root)
    assertEquals(1, RootsShadow.rootRequests)
  }

  @Test
  fun touchOnTheAppIsNotOnGboard() {
    assertNull(KeyboardExploration.gboardRootAt(service, Display.DEFAULT_DISPLAY, 50f, 500f))
    assertEquals(0, RootsShadow.rootRequests)
  }

  @Test
  fun keysUnderAMovingFingerAreFoundWithoutFetchingTheRootAgain() {
    val root = KeyboardExploration.gboardRootAt(service, Display.DEFAULT_DISPLAY, 50f, 1650f)!!
    val found = mutableListOf<CharSequence?>()
    for (x in 10..200 step 6) {
      found +=
        KeyboardExploration.nodeAt(service, Display.DEFAULT_DISPLAY, root, x.toFloat(), 1650f)
          ?.contentDescription
    }

    assertEquals(listOf<CharSequence>("q", "w"), found.distinct())
    assertEquals(1, RootsShadow.rootRequests)
  }

  @Test
  fun fingerOffGboardFindsNothing() {
    val root = KeyboardExploration.gboardRootAt(service, Display.DEFAULT_DISPLAY, 50f, 1650f)!!

    assertNull(KeyboardExploration.nodeAt(service, Display.DEFAULT_DISPLAY, root, 50f, 500f))
  }

  private class TestWindow(val window: AccessibilityWindowInfo, val root: AccessibilityNodeInfo)

  private fun window(id: Int, type: Int, bounds: Rect, rootPackage: String): TestWindow {
    val window = AccessibilityWindowInfo()
    val shadow = shadowOf(window)
    shadow.setId(id)
    shadow.setType(type)
    shadow.setBoundsInScreen(bounds)
    shadow.setDisplayId(Display.DEFAULT_DISPLAY)
    val root =
      AccessibilityNodeInfo().apply {
        packageName = rootPackage
        setBoundsInScreen(bounds)
        isVisibleToUser = true
      }
    ReflectionHelpers.setField(root, "mWindowId", id)
    shadow.setRoot(root)
    return TestWindow(window, root)
  }

  private fun AccessibilityNodeInfo.addKey(label: String, bounds: Rect) {
    val key =
      AccessibilityNodeInfo().apply {
        packageName = GBOARD
        className = "com.android.inputmethod.keyboard.Key"
        contentDescription = label
        setBoundsInScreen(bounds)
        isVisibleToUser = true
        isFocusable = true
        isClickable = true
        isEnabled = true
      }
    ReflectionHelpers.setField(key, "mWindowId", KEYBOARD_ID)
    shadowOf(this).addChild(key)
  }

  private companion object {
    const val GBOARD = "com.google.android.inputmethod.latin"
    const val KEYBOARD_ID = 7
    const val APP_ID = 3
    val SCREEN_BOUNDS = Rect(0, 0, 1080, 2400)
    val KEYBOARD_BOUNDS = Rect(0, 1500, 1080, 2400)
  }
}
